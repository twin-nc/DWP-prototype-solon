# Solon Tax API — Usage Guide for Additional Layer
**Date:** 2026-04-24  
**Applies to:** Additional layer services integrating with Solon Tax microservices

> This guide documents HOW each additional layer component calls the confirmed Solon Tax REST APIs. All integration is REST-only. The Solon internal Kafka bus (`irm.bpmn-engine.*`) is the BPMN engine's private command channel — the additional layer does NOT publish to those topics.

---

## Authentication

All Solon Tax API calls require a Bearer JWT obtained from Keycloak.

```
Authorization: Bearer {jwt}
```

For service-to-service calls, omit `irm-origin-client` (or use a non-UI value). This bypasses UI-only validation rules that restrict operations like task status transitions or suppression creation. Only include `irm-origin-client: UI-CLIENT` when the call originates from a browser/agent UI.

Standard headers for all service-to-service calls:
```
Authorization: Bearer {jwt}
irm-include-labels: FALSE
Accept-Language: en-GB
```

---

## Starting a Debt Collection Strategy (Process Orchestration)

### Who calls this: Champion/Challenger Framework, Debt Strategy Launcher

When a debtor account enters collection, the additional layer starts a BPMN-backed collection strategy via the Process Management API.

**Step 1 — Ensure the process type exists (one-time setup):**
```
GET /process-management/process-types/{code}
```

**Step 2 — Start a process instance against the debtor:**
```json
POST /process-management/process-instances
{
  "processTypeCode": "DEBT_COLLECTION_CHAMPION",
  "entityCL": "PARTY_ROLE",
  "entityId": "debtor-party-role-uuid"
}
```

**Step 3 — Record the assignment in the additional layer's Assignment DB and create a ProcessFlow join record:**
```json
POST /process-management/process-flows
{
  "sourceEntityId": "debtor-party-role-uuid",
  "entityCL": "PARTY_ROLE",
  "processInstanceId": "returned-process-instance-id",
  "entityStatusCL": "ACTIVE"
}
```

**Querying live strategy state:**
```
GET /process-management/process-flows?sourceEntityId={debtor-party-role-uuid}
GET /process-management/node-instances?processInstanceId={id}
```

---

## Creating a Case (Case-Backed Collection)

### Who calls this: Debt Case Creator, Agent Desktop

Cases in Solon are backed by a CaseType which links to a ProcessType — creating the case automatically starts the associated BPMN process.

**Create a case:**
```json
POST /case-management/cases
{
  "caseTypeCode": "DWP_DEBT_RECOVERY",
  "partyRole": { "id": "debtor-party-role-uuid" },
  "casePriorityCL": "MEDIUM",
  "relatedEntities": [
    {
      "entityCL": "LIABILITY",
      "entityId": "liability-uuid",
      "caseEntityRelationshipTypeCL": "PRIMARY"
    }
  ],
  "dataArea": "{\"riskScore\": 72, \"incomeConfirmed\": false}",
  "startDate": "2026-04-24"
}
```

The `dataArea` field is a free JSON blob — use it to carry vulnerability flags, risk scores, I&E outcomes, or any additional metadata without schema changes.

**Fetch debt balance for a case:**
```
GET /case-management/cases/{id}/balance
```
Returns `totalCaseBalance`, per-liability breakdown, per-bill-segment detail including `outstandingBalance`, `originalLiabilityAmount`, and `billSegmentTypeCd` (e.g. `TAX_FTP` for fees/penalties).

**Get scored list of eligible workers:**
```
GET /case-management/cases/{id}/eligible-workers
```
Returns workers scored against the case type's worker profiles — use this to pre-populate the assignment dropdown in the agent UI.

---

## Deploying a BPMN Strategy

### Who calls this: Strategy Deployment component, Admin API

New or updated collection strategies are deployed as BPMN XML files via the Process Management API.

**Deploy a BPMN definition:**
```json
POST /process-management/process-definitions
{
  "processId": "dwp-debt-collection-v3",
  "label": "DWP Debt Collection Strategy v3",
  "resource": "<base64-encoded BPMN XML>"
}
```

**List deployed definitions:**
```
GET /process-management/process-definitions/dropdown
```

**Create or update a process type linking to the deployed definition:**
```json
POST /process-management/process-types
{
  "code": "DEBT_COLL_V3",
  "processDefinition": "dwp-debt-collection-v3",
  "entityCL": "PARTY_ROLE",
  "categoryCL": "DEBT_COLLECTION",
  "active": true,
  "ownerCL": "DWP",
  "labels": [{ "languageCL": "EN", "description": "DWP Debt Collection v3" }]
}
```

---

## Champion/Challenger Strategy Assignment

### Who calls this: Champion/Challenger Framework

The additional layer maintains its own Assignment DB recording which variant (champion vs. challenger) each account is assigned to. Solon's ProcessFlow records provide the live process instance linkage.

**On account entry:**
1. Determine champion/challenger split (additional layer logic).
2. `POST /process-management/process-instances` with the appropriate `processTypeCode` for the assigned variant.
3. `POST /process-management/process-flows` to create the Solon join record.
4. Write assignment to local Assignment DB (`accountId`, `variant`, `processInstanceId`, `assignedAt`).

**Querying current strategy state:**
```
GET /process-management/process-flows?sourceEntityId={accountId}&entityStatusCL=ACTIVE
GET /process-management/node-instances?processInstanceId={id}
```

---

## Creating Human Tasks

### Who calls this: Work Item Assignment Engine, BPMN processes (internally)

Human tasks can be created directly via REST — no BPMN process is required.

**One-time: configure a task type:**
```json
POST /human-task-management/human-task-types
{
  "code": "DEBT_REVIEW",
  "active": true,
  "humanTaskPriorityCL": "MEDIUM",
  "manualCreationAllowed": false,
  "humanTaskAssignBehaviorCL": "AUTO_ASSIGN",
  "humanTaskWorkerProfile": "DEBT_CASEWORKER_PROFILE",
  "entities": [
    { "entityCL": "CASE", "humanTaskEntityRelationshipTypeCL": "PRIMARY" }
  ],
  "ownerCL": "DWP",
  "labels": [{ "languageCL": "EN", "description": "Debt Review Task" }]
}
```

**Create a task instance:**
```json
POST /human-task-management/human-tasks
{
  "humanTaskTypeCode": "DEBT_REVIEW",
  "humanTaskPriorityCL": "HIGH",
  "relatedEntities": [
    {
      "entityCL": "CASE",
      "entityId": "case-uuid",
      "humanTaskEntityRelationshipTypeCL": "PRIMARY"
    }
  ],
  "dataArea": "{\"reviewType\": \"vulnerability\", \"flaggedReason\": \"income_change\"}",
  "startDate": "2026-04-24",
  "endDate": "2026-05-01"
}
```

Note: `processInstanceId` on the created task will be null (REST-created). If a BPMN process creates the task, Solon sets `processInstanceId` automatically — completing that task will then fire the BPMN signal-back.

**Get eligible workers for assignment:**
```
GET /human-task-management/human-task-types/{code}/eligible-users
```

---

## Completing a Human Task (BPMN Signal-Back)

### Who calls this: Agent Desktop, Work Item Assignment Engine

```json
POST /human-task-management/human-tasks/complete
{
  "humanTaskId": "task-uuid",
  "entityStatusCL": "COMPLETED",
  "changeReasonCL": "REVIEW_DONE",
  "changeComment": "I&E assessment completed, income confirmed"
}
```

When `processInstanceId` on the task is **not null** (task was created by a BPMN process), `entityStatusCL` is mandatory and Solon automatically fires the signal back to the waiting BPMN process instance, advancing the collection strategy. No additional Kafka or REST call is needed.

---

## Vulnerability Suppression

### Who calls this: Vulnerability Protocol Enforcement

**One-time setup — configure suppression type:**
```json
POST /suppression-management/suppression-types
{
  "suppressionTypeCode": "VULNERABILITY",
  "maximumNumberDays": 0,
  "manualCreationAllowedSW": true,
  "maintenanceOfAutocreatedAllowedSW": false,
  "overrideEndDateSW": true,
  "suppressionEntityTypeCL": "LIABILITY",
  "actionTypes": [
    {
      "suppressionActionTypeCL": "DEBT_REC_C",
      "suspendActiveInstancesSW": true
    }
  ],
  "activeSW": true,
  "ownerCL": "DWP",
  "labels": [{ "languageCL": "EN", "description": "Vulnerability" }]
}
```

**Activate suppression for a debtor (per vulnerability event):**
```json
POST /suppression-management/suppressions
{
  "suppressionTypeCode": "VULNERABILITY",
  "suppressionReasonCL": "VULNERABILITY",
  "suppressionActionTypes": [{ "suppressionActionTypeCL": "DEBT_REC_C" }],
  "suppressionScopes": [
    { "entityCL": "LIABILITY", "entityId": "liability-uuid" }
  ],
  "sourceEntityId": "case-uuid",
  "sourceEntityTypeCL": "CASE",
  "startDate": "2026-04-24"
}
```

`suspendActiveInstancesSW: true` means Solon automatically pauses any active debt recovery BPMN processes on the scoped liability when this suppression is created. Store the returned suppression `id` in the additional layer's Vulnerability Registry.

**Release suppression when condition resolves:**
```
DELETE /suppression-management/suppressions/{id}
```

The suppression pauses the BPMN debt recovery process automatically. If the debtor also has an active payment plan, pause it separately:
```
PUT /taxpayer-accounting/pay-plans/{id}/pause
```

---

## Breathing Space (Debt Respite Scheme)

### Who calls this: Breathing Space Handler (triggered by DRS notification)

**One-time setup — configure the Breathing Space suppression type:**
```json
POST /suppression-management/suppression-types
{
  "suppressionTypeCode": "BREATHING_SPACE",
  "maximumNumberDays": 60,
  "maximumNumberDaysTypeCL": "CALENDAR_DAYS",
  "manualCreationAllowedSW": false,
  "maintenanceOfAutocreatedAllowedSW": false,
  "overrideEndDateSW": false,
  "suppressionEntityTypeCL": "LIABILITY",
  "actionTypes": [
    {
      "suppressionActionTypeCL": "DEBT_REC_C",
      "suspendActiveInstancesSW": true
    }
  ],
  "activeSW": true,
  "ownerCL": "DWP",
  "labels": [{ "languageCL": "EN", "description": "Breathing Space" }]
}
```

**Activate Breathing Space for a debtor:**
```json
POST /suppression-management/suppressions
{
  "suppressionTypeCode": "BREATHING_SPACE",
  "suppressionReasonCL": "BREATHING_SPACE",
  "suppressionActionTypes": [{ "suppressionActionTypeCL": "DEBT_REC_C" }],
  "suppressionScopes": [
    { "entityCL": "LIABILITY", "entityId": "liability-uuid" }
  ],
  "sourceEntityId": "case-uuid",
  "sourceEntityTypeCL": "CASE",
  "startDate": "2026-04-24",
  "endDate": "2026-06-23"
}
```

Solon enforces the 60-day maximum at the type level (`overrideEndDateSW: false`). Active debt recovery BPMN processes on the scoped liability are automatically paused. Store the returned suppression `id`.

If the debtor has an active payment plan, pause it separately:
```
PUT /taxpayer-accounting/pay-plans/{id}/pause
```

**Release (end of Breathing Space period or early exit):**
```
DELETE /suppression-management/suppressions/{id}
PUT /taxpayer-accounting/pay-plans/{id}/activate   (if plan was paused)
```

---

## Payment Plan Management (I&E / Repayment Arrangements)

### Who calls this: Repay Plan & I&E UI, Agent Desktop

**Search existing payment plans for a debtor:**
```
GET /taxpayer-accounting/pay-plans?partyRoleId={id}
```

**Activate a payment plan:**
```
PUT /taxpayer-accounting/pay-plans/{id}/activate
```

**Pause a payment plan (vulnerability or breathing space):**
```
PUT /taxpayer-accounting/pay-plans/{id}/pause
```

**Cancel a payment plan:**
```
PUT /taxpayer-accounting/pay-plans/{id}/cancel
```

**Complete a payment plan (fully paid):**
```
PUT /taxpayer-accounting/pay-plans/{id}/complete
```

**Read bill segments for a liability (to show arrears breakdown):**
```
GET /taxpayer-accounting/liabilities/{id}/bill-segments
```

**Read payment events (transaction history):**
```
GET /taxpayer-accounting/payment-events?liabilityId={id}
```

---

## Write-offs and Overpayment Processes

### Who calls this: Agent Desktop, Fraud & Dispute UI, BPMN debt resolution processes

**Write off an overpayment:**
```json
POST /taxpayer-accounting/overpayment-processes/write-off
{
  "overpaymentProcessId": "overpayment-uuid",
  "remainingAmount": 450.00
}
```

**Process a refund:**
```json
POST /taxpayer-accounting/overpayment-processes/refund
{
  "overpaymentProcessId": "overpayment-uuid",
  "remainingAmount": 120.00
}
```

**Offset an overpayment against an outstanding liability:**
```json
POST /taxpayer-accounting/overpayment-processes/offset
{
  "overpaymentProcessId": "overpayment-uuid"
}
```

**Transition an overpayment process status:**
```
PUT /taxpayer-accounting/overpayment-processes/{id}/actions/{entityStatusCL}
```

---

## Foundations Configurator — Codelist Management

### Who calls this: Foundations Configurator UI

All `*CL` fields across Solon APIs reference codes managed in the Reference Data service. The Foundations Configurator reads and extends these for DWP-specific configuration.

**Populate a dropdown (e.g. suppression reasons):**
```
GET /reference-data/codelist-items?codelistKey=SUPPRESSION_REASONS
```

**Get all codelists for a configuration category:**
```
GET /reference-data/categories/{categoryCode}/codelists
```

**Add a DWP-specific code to an existing codelist:**
```json
POST /reference-data/codelist-items
{
  "code": "DWP_VULNERABILITY_SEVERE",
  "codelistId": 42,
  "ownerCL": "DWP",
  "validFrom": "2026-04-24",
  "labels": [{ "languageCL": "EN", "description": "Severe Vulnerability (DWP)" }]
}
```

**Invalidate reference data cache after changes:**
```
DELETE /reference-data/caches/clear-all
```

---

## Scheduling Time-Triggered Processes

### Who calls this: Vulnerability Review Scheduler, any time-based additional layer component

Use Solon's ScheduledProcess to trigger BPMN processes on a cron schedule — this replaces any need for the additional layer to maintain its own scheduler for Solon-backed processes.

**Create a scheduled process (e.g., vulnerability review checker):**
```json
POST /process-management/scheduled-processes
{
  "processTypeCode": "VULNERABILITY_REVIEW",
  "cronExpression": "0 6 * * 1",
  "relatedEntities": [
    { "entityCL": "LIABILITY", "entityId": "liability-uuid" }
  ],
  "active": true,
  "labels": [{ "languageCL": "EN", "description": "Weekly Vulnerability Review" }]
}
```

**List instances (execution history):**
```
GET /process-management/scheduled-processes/{id}/instances
```

---

## Operational Plans (Supervisor Campaign Tracking)

### Who calls this: Supervisor Dashboard, MI & Analytics, Strategy Engine

Operational Plans are supervisor-level objects for structuring debt recovery campaigns with measurable objectives and targets. They are read-only from the agent perspective; supervisors and the strategy layer create and manage them.

**Read active operational plans (Supervisor Dashboard):**
```
GET /operational-plan-management/operational-plans?operationalPlanStatusCL=ACTIVE
```

**Read a specific plan with completion metrics:**
```
GET /operational-plan-management/operational-plans/{id}
```
Returns `linkedCases`, `completedCases`, `percentage`, `totalAuditAssessedAmount`, and per-objective/target actual vs. planned values.

**Create an operational plan for a new campaign:**
```json
POST /operational-plan-management/operational-plans
{
  "operationalPlanTypeCode": "DWP_DEBT_CAMPAIGN",
  "operationalPlanStatusCL": "DRAFT",
  "description": "Q2 2026 Debt Recovery",
  "startDt": "2026-04-01",
  "endDt": "2026-06-30",
  "operationalPlanObjectives": [
    {
      "description": "Case resolution rate",
      "operationalPlanTargets": [
        {
          "description": "Cases resolved",
          "operationalPlanMetricCL": "CASES_RESOLVED",
          "weight": 100,
          "targetValue": 500,
          "operationalPlanTargetValues": [
            {
              "operationalPlanTargetDimensionValues": [
                {
                  "operationalPlanDimensionTypeCL": "REVENUE_TYPE",
                  "operationalPlanTargetDimension": "REVENUE_TYPE",
                  "operationalPlanTargetDimensionValue": "INCOME_TAX"
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

**Update plan status (e.g. activate after approval):**
```json
PUT /operational-plan-management/operational-plans/{id}
{
  ...plan body with operationalPlanStatusCL: "ACTIVE"
}
```

> Note: `approvalProcessTypeCD` on an OperationalPlanType links it to an approval workflow — plans of that type must go through approval before activation.

---

## Registration — Debtor Identity and Worker Lookup

### Who calls this: Agent Desktop, Queue Distribution Service, Foundations Configurator, Third-Party Collection Handover Orchestrator

The Registration API is the master record for all parties (debtors, organisations), party roles, IRM users (caseworkers/agents), and organisational units. Every `partyRoleId` referenced across all other Solon APIs resolves to a record here.

**Resolve a debtor's identity from a case's partyRoleId (Agent Desktop):**
```
GET /registration/parties/single-party-role-view?partyRoleId={id}
```
Returns Party (name, address, identifiers, bank accounts) and PartyRole (type, states) in one call.

**Get full party record:**
```
GET /registration/parties/{partyId}
```

**Get party role details (identifiers, addresses, eligible contacts):**
```
GET /registration/party-roles/{id}
GET /registration/party-roles/{id}/eligible-contacts
GET /registration/party-roles/{id}/eligible-addresses/dropdown
```

**Get active revenue type registrations for a debtor (debt context):**
```
GET /registration/revenue-type-registrations?partyRoleId={id}&status=ACTIVE
GET /registration/party-roles/{id}/registered-revenue-types/dropdown
```

**List available agents for queue routing:**
```
GET /registration/users?states=AVAILABLE&organizationalUnitId={id}
GET /registration/organizational-unit/v2?{team-filter}
```

**Create a user account (Foundations Configurator):**
```json
POST /registration/users/quick-create
{
  "firstName": "Jane",
  "lastName": "Smith",
  "email": "jane.smith@dwp.gov.uk",
  "phoneNumber": "+441234567890",
  "countryCL": "GB"
}
```

---

## Contact Management (Creating Correspondence)

### Who calls this: Contact Orchestration Service, Agent Desktop, BPMN-triggered flows

Solon's Contact Management API owns the full contact record lifecycle. The additional layer creates contacts for non-BPMN flows (agent-initiated letters, dialler campaign contacts). BPMN-triggered contacts are created internally by the engine via its private command bus.

**Create a contact (e.g. a debt demand letter):**
```json
POST /contact-management/contacts
{
  "communicationChannelCode": "LETTER_POST",
  "recipientPartyRole": { "id": "debtor-party-role-uuid" },
  "contactTypeDetails": { "contactTypeCode": "DWP_DEMAND_LETTER" },
  "relatedEntities": [
    { "entityCL": "CASE", "entityId": "case-uuid" },
    { "entityCL": "LIABILITY", "entityId": "liability-uuid" }
  ],
  "parameters": [
    { "key": "DEBT_AMOUNT", "value": "1250.00" },
    { "key": "DUE_DATE", "value": "2026-05-24" }
  ],
  "dataArea": "{\"agentId\": \"agent-123\", \"campaignRef\": \"Q2-2026\"}"
}
```

Contact `statusCL` is set to PENDING by Solon on creation — it is system-managed. Store the returned `id`.

**Generate and dispatch the contact:**
```json
POST /contact-management/contacts/{id}/generate
{
  "changeReasonCL": "INITIAL_SEND",
  "contactFileOutputCL": "PDF"
}
```

**Read available channels for a contact type:**
```
GET /contact-management/communication-channels?typeCL=LETTER
```

**Read contact types for a medium (e.g. letter):**
```
GET /contact-management/contact-types?contactMediumCL=LETTER&activeSw=true
```

**Read contact history for a case:**
```
GET /contact-management/contacts?relatedEntityId={caseId}&relatedEntityCL=CASE
```

> Note: `Contact.noticeDate` is computed automatically from the ContactType's `noticeDateCalculationNumberOfDays`. Use `overrideNoticeDate` only when a manual date is required.

---

## Risk Analysis — Candidate Lists and Intervention Initiation

### Who calls this: Champion/Challenger Framework, Strategy Engine, Supervisor Dashboard

The Risk Analysis API manages candidate lists for debt collection targeting. Accounts are grouped into candidate lists, scored, and then interventions (case creation) are initiated for selected candidates.

**Create a candidate list for a debt collection campaign:**
```json
POST /risk-analysis/candidate-lists
{
  "candidateListTypeCode": "DWP_DEBT_TARGETING",
  "description": "Q2 2026 High-Risk Accounts",
  "operationalPlan": { "id": "operational-plan-uuid" },
  "candidates": [
    {
      "candidateSourceCL": "ANALYTICS_ENGINE",
      "partyRole": { "id": "debtor-party-role-uuid-1" },
      "caseTypeCode": "DWP_DEBT_RECOVERY",
      "riskScore": 78.5
    },
    {
      "candidateSourceCL": "ANALYTICS_ENGINE",
      "partyRole": { "id": "debtor-party-role-uuid-2" },
      "caseTypeCode": "DWP_DEBT_RECOVERY",
      "riskScore": 65.2
    }
  ]
}
```

**Initiate debt collection cases for selected candidates:**
```json
POST /risk-analysis/candidates/intervention-initiation
{
  "candidatesIds": ["candidate-uuid-1", "candidate-uuid-2"],
  "caseTypeCode": "DWP_DEBT_RECOVERY",
  "operationalPlanId": "operational-plan-uuid"
}
```

This triggers case creation in Solon's case management service for each candidate.

**Read candidate lists for a campaign:**
```
GET /risk-analysis/candidate-lists?operationalPlanId={id}
```

**Read available candidate list types:**
```
GET /risk-analysis/candidate-list-types?active=true
```

> Risk screening (`riskScore` population) is triggered by the BPMN engine internally via `irm.bpmn-engine.screen-risk` — the additional layer reads the scores as part of candidate data. The additional layer does not trigger screening directly.

---

## Third-Party Collection Handover

### Who calls this: Third-Party Collection Handover Orchestrator

```json
POST /case-management/cases/{id}/debt-handover
{
  "revenueTypeRegistrationId": "rev-type-reg-uuid",
  "handoverAmount": 1250.00,
  "revenueTypeRegistrationReconciliationPeriodId": "period-uuid"
}
```

---

## Agent Desktop — Reading State

### Who calls this: Agent Desktop API (read operations)

| Data needed | Call |
|---|---|
| Debtor identity (name, address, IDs) | `GET /registration/parties/single-party-role-view?partyRoleId={id}` |
| Full party record | `GET /registration/parties/{partyId}` |
| Party role details (contacts, identifiers) | `GET /registration/party-roles/{id}` |
| Debtor's active revenue registrations | `GET /registration/revenue-type-registrations?partyRoleId={id}&status=ACTIVE` |
| Case details + activities | `GET /case-management/cases/{id}` |
| Full debt balance breakdown | `GET /case-management/cases/{id}/balance` |
| Liability detail | `GET /taxpayer-accounting/liabilities/{id}` |
| Bill segments for a liability | `GET /taxpayer-accounting/liabilities/{id}/bill-segments` |
| Active payment plans for a debtor | `GET /taxpayer-accounting/pay-plans?partyRoleId={id}` |
| Payment history (events) | `GET /taxpayer-accounting/payment-events?liabilityId={id}` |
| Active suppressions on a liability | `GET /suppression-management/suppressions?sourceEntityId={id}` |
| Open human tasks for a case | `GET /human-task-management/human-tasks?relatedEntityId={caseId}` |
| Current BPMN step | `GET /process-management/node-instances?processInstanceId={id}` |
| Active process flow for an account | `GET /process-management/process-flows?sourceEntityId={id}&entityStatusCL=ACTIVE` |
| Eligible workers for a case | `GET /case-management/cases/{id}/eligible-workers` |
| Suppression types (configuration) | `GET /suppression-management/suppression-types` |
| Human task types (configuration) | `GET /human-task-management/human-task-types` |
| Contact history for a case | `GET /contact-management/contacts?relatedEntityId={caseId}&relatedEntityCL=CASE` |
| Available contact types (letter/email/SMS) | `GET /contact-management/contact-types?contactMediumCL={medium}&activeSw=true` |
| Candidate lists for a campaign | `GET /risk-analysis/candidate-lists?operationalPlanId={id}` |
| Codelist values for a field | `GET /reference-data/codelist-items?codelistKey={key}` |

---

## Query Filters Reference

### Suppression searches
```
GET /suppression-management/suppressions?suppressionTypeCd=BREATHING_SPACE&sourceEntityId={id}&suppressionStatusCL=ACTIVE
```

### Human task searches
```
GET /human-task-management/human-tasks?humanTaskTypeCode=DEBT_REVIEW&entityStatusCL=PENDING
```

### Process flow searches
```
GET /process-management/process-flows?sourceEntityId={id}&entityCL=PARTY_ROLE&entityStatusCL=ACTIVE
```

### Case searches
```
GET /case-management/cases?partyRoleId={id}&caseStatusCL=IN_PROGRESS
```

### Taxpayer accounting searches
```
GET /taxpayer-accounting/pay-plans?partyRoleId={id}
GET /taxpayer-accounting/liabilities?partyRoleId={id}
GET /taxpayer-accounting/payment-events?liabilityId={id}
GET /taxpayer-accounting/overpayment-processes?partyRoleId={id}
GET /taxpayer-accounting/financial-transactions?liabilityId={id}
```

### Registration searches
```
GET /registration/parties/single-party-role-view?partyRoleId={id}
GET /registration/party-roles?partyId={id}
GET /registration/users?states=AVAILABLE&organizationalUnitId={id}
GET /registration/revenue-type-registrations?partyRoleId={id}&status=ACTIVE
GET /registration/organizational-unit/v2?{team-filter}
```

### Contact management searches
```
GET /contact-management/contacts?relatedEntityId={caseId}&relatedEntityCL=CASE
GET /contact-management/contacts?recipientPartyRoleId={id}
GET /contact-management/contact-types?contactMediumCL=LETTER&activeSw=true
GET /contact-management/communication-channels?typeCL=EMAIL
```

### Risk analysis searches
```
GET /risk-analysis/candidate-lists?operationalPlanId={id}
GET /risk-analysis/candidate-lists?candidateListTypeCode={code}
GET /risk-analysis/candidate-list-types?active=true
```

### Reference data lookups
```
GET /reference-data/codelist-items?codelistKey=SUPPRESSION_REASONS
GET /reference-data/codelist-items?codelistKey=HUMAN_TASK_PRIORITIES
GET /reference-data/codelist-items?codelistKey=CASE_STATUSES&referenceDate=2026-04-24
GET /reference-data/categories/{categoryCode}/codelists
```
