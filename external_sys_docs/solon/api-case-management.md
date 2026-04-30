# Case Management API — Reference
**Version:** 2.1.0  
**Base path:** `/case-management` (assumed; routed via Kubernetes service DNS)  
**Auth:** Bearer JWT from Keycloak on every request

---

## Endpoints

### Cases

| Method | Path | Summary |
|---|---|---|
| GET | `/cases` | List/search cases |
| POST | `/cases` | Create a new case |
| GET | `/cases/{id}` | Get case by ID |
| PUT | `/cases/{id}` | Update case |
| DELETE | `/cases/{id}` | Delete case (PENDING status only) |
| GET | `/cases/{id}/balance` | Get full debt balance breakdown |
| GET | `/cases/{id}/eligible-workers` | Get scored list of eligible workers |
| POST | `/cases/{id}/debt-handover` | Initiate third-party collection handover (inferred from `CaseDebtHandover` schema) |
| POST | `/cases/{id}/related-entities` | Add related entity to case |
| DELETE | `/cases/{id}/related-entities/{entityId}` | Remove related entity |

**`Case` schema:**

| Field | Type | Notes |
|---|---|---|
| `id` | string | Null on CREATE |
| `caseTypeCode` | string | Links to a configured CaseType |
| `partyRole` | Identification | The debtor this case belongs to |
| `caseStatusCL` | string | From `CASE_STATUSES`; set to PENDING on create; read-only thereafter |
| `casePriorityCL` | string | From `CASE_PRIORITIES` |
| `caseDifficultyCL` | string | From `CASE_DIFFICULTIES` |
| `caseReferenceNumber` | string (max 255) | Optional reference |
| `activities` | array of CaseActivity | Hierarchical activity/task list |
| `assignedWorkers` | array of CaseAssignedWorker | Workers assigned to this case |
| `relatedEntities` | array of CaseRelatedEntityDto | Linked entities (liabilities, pay plans, etc.) |
| `attachmentDetails` | array | Document attachments |
| `dataArea` | string (JSON) | **Custom JSON blob — stores vulnerability flags, risk scores, I&E data without schema changes** |
| `startDate` / `endDate` | date | Case active period |
| `sourceEntityId` + `sourceEntityTypeCL` | string | Originating entity |
| `supervisorUser` | Identification | Supervising caseworker |
| `changeReasonCL` + `changeComment` | string | Required on updates |

**Edit restrictions:**
- `caseStatusCL`, `createdDateTime`, `activities` are read-only
- Editing is not allowed when `caseStatusCL` is APPROVED, COMPLETED, or CANCELLED
- Deletion only allowed in PENDING status

---

**`CaseActivity` schema:**

| Field | Notes |
|---|---|
| `id` | Null on CREATE |
| `caseTypeActivityInfo.id` | References the CaseTypeActivity definition |
| `caseTypeActivityInfo.humanTaskTypeCode` | If this activity is a Human Task |
| `caseTypeActivityInfo.processTaskTypeCode` | If this activity triggers a BPMN process |
| `caseActivityStatusCL` | Read-only; updated via lifecycle endpoints only |
| `relatedEntityId` | ID of the actual Human Task, Case, or Process behind this activity |
| `relevant` | Boolean; whether this activity is marked as relevant |
| `milestoneDueDate` | Only for MILESTONE type activities |
| `assignedWorkers` | Workers assigned to this activity |
| `childrenActivities` | Nested sub-activities |

---

**`CaseAssignedWorker` schema:**

| Field | Notes |
|---|---|
| `caseWorkerProfileCode` | Which worker profile this worker is assigned under |
| `user` | Identification of the assigned user |
| `allocationPercentage` | 0–999.99% |

---

**`CaseRelatedEntityDto` schema:**

| Field | Notes |
|---|---|
| `entityCL` | Entity type from `ENTITIES` codelist |
| `entityId` | UUID of the related entity |
| `caseEntityRelationshipTypeCL` | Relationship type from `CASE_RELATED_ENTITIES_RELATIONSHIPS` |

**Validation:** If adding a Liability or PayPlan, its party role must match the case's party role.

---

**`CaseBalance` schema (from `GET /cases/{id}/balance`):**

| Field | Notes |
|---|---|
| `caseId` | The case ID |
| `totalCaseBalance` | Aggregate Balance object |
| `liabilities[]` | Per-liability balance breakdown |
| `liabilities[].liabilityId` | Liability ID |
| `liabilities[].totalCaseLiabilityBalance` | Balance this liability contributes |
| `liabilities[].billSegments[]` | Per-bill-segment detail |
| `liabilities[].billSegments[].outstandingBalance` | Current outstanding (decreases with payments) |
| `liabilities[].billSegments[].originalLiabilityAmount` | Original charge amount (fixed) |
| `liabilities[].billSegments[].billSegmentTypeCd` | Type code (e.g. TAX_FTP for fees) |
| `liabilities[].billSegments[].effectiveDate` | Date the charge became effective |
| `liabilities[].unallocatedFTs` | Payments received but not yet allocated |

---

**`CaseEligibleWorker` schema (from `GET /cases/{id}/eligible-workers`):**

| Field | Notes |
|---|---|
| `id` | User ID |
| `identificationLabel` | Display name |
| `score` | Eligibility score (higher = better match) |
| `caseWorkerProfileCode` | Which profile the score was calculated against |

---

**`CaseDebtHandover` schema:**

| Field | Notes |
|---|---|
| `revenueTypeRegistrationId` | The liability/revenue type registration being handed over |
| `handoverAmount` | Amount to hand over |
| `revenueTypeRegistrationReconciliationPeriodId` | Optional reconciliation period |

---

### Case Types

| Method | Path | Summary |
|---|---|---|
| GET | `/case-types` | List/search case types |
| POST | `/case-types` | Create a new case type |
| GET | `/case-types/{code}` | Get by code |
| PUT | `/case-types/{code}` | Update |
| DELETE | `/case-types/{code}` | Delete (not allowed if used by existing cases) |

**`CaseType` schema:**

| Field | Type | Notes |
|---|---|---|
| `code` | string (max 10) | Unique code |
| `processTypeCode` | string | **Links to a Process Type — creating a case of this type starts the linked BPMN process** |
| `caseTypeCategoryCL` | string | From `CASE_TYPE_CATEGORIES` |
| `caseTypeStatusCL` | string | From `CASE_TYPE_STATUSES` |
| `activities` | array of CaseTypeActivity | Template activity structure for new cases |
| `workerProfiles` | array (min 1) | Eligible worker profiles for this case type |
| `relatedEntityTypes` | array | Which entity types can be related to this case type |
| `caseTypeRules` | array of CaseTypeRule | Business rules applied to this case type |
| `ownerCL` | string | From `OWNERS` |
| `labels` | array of Label | Multilingual labels |

**`CaseTypeActivity` schema:**

| Field | Notes |
|---|---|
| `caseTypeActivityTypeCL` | Type: PHASE, SUBPHASE, HUMAN_TASK_TYPE, PROCESS_TASK_TYPE, CASE_TASK_TYPE |
| `humanTaskTypeCode` | If type is HUMAN_TASK_TYPE |
| `processTaskTypeCode` | If type is PROCESS_TASK_TYPE |
| `activityCaseTypeCode` | If type is CASE_TASK_TYPE |
| `phaseCode` | For PHASE and SUBPHASE types |
| `order` | Display/execution order |
| `cancelAllowed` / `cancelReasonRequired` | Whether activity can be cancelled |
| `repeatAllowed` | Whether activity can be repeated |
| `required` | Whether activity is mandatory |
| `childrenActivities` | Nested activities (phases contain subphases contain tasks) |

---

### Case Worker Profiles

| Method | Path | Summary |
|---|---|---|
| GET | `/case-worker-profiles` | List/search |
| POST | `/case-worker-profiles` | Create |
| GET | `/case-worker-profiles/{code}` | Get by code |
| PUT | `/case-worker-profiles/{code}` | Update |
| DELETE | `/case-worker-profiles/{code}` | Delete (not allowed if used by a case type) |

**`CaseWorkerProfile` schema:**

| Field | Notes |
|---|---|
| `code` | Unique code (max 10) |
| `criteria` | Scoring criteria with weights |
| `criterionUnits` | Individual workers/roles/seniority levels with criterion type |
| `ownerCL` | From `OWNERS` |
| `labels` | Multilingual labels |

**`CaseWorkerProfileCriterionUnit`:**

| `workerProfileCriterionTypeCL` | Description |
|---|---|
| `WORKER` | Match against a specific user |
| `ROLE` | Match against a Keycloak role |
| `ROLE_SENIORITY_LEVEL` | Match against seniority level from `HUMAN_TASK_SENIORITY_LEVELS` |

---

## Case Scope

| Method | Path | Summary |
|---|---|---|
| GET/PUT | `/cases/{id}/scope` | Get or update the liability/bill segment scope of a case |

**`InputScope` schema (for scope configuration):**

| Field | Notes |
|---|---|
| `liabilityId` | Which liability is in scope |
| `billSegments` | Specific bill segments included/excluded |
| `caseTypes` | Case types in scope |

---

## Common headers

| Header | Values | Purpose |
|---|---|---|
| `Authorization` | `Bearer {jwt}` | Required on all calls |
| `irm-origin-client` | `UI-CLIENT` or `FORMS` | Controls which validation path applies |
| `irm-include-labels` | `TRUE` or `FALSE` | Whether to include description labels |
| `Accept-Language` | e.g. `en-GB` | Language for returned descriptions |
