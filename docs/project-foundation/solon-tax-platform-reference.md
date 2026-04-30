# Solon Tax v2.3.0 Platform Reference

**Source docs:** `external_sys_docs/solon/solon_tax_2.3.0_integration_guide.md`, `external_sys_docs/solon/solon_tax_2.3.0_operations_guide.md`

## Product Identity

- Revenue Management Platform (ERP for tax authorities — tax registration, assessment, billing, collection)
- NOT generic case management or debt collection software
- Customer: public-sector tax authorities (not DWP-style benefit debt)
- Vendor: Netcompany (Amplio is a Netcompany first-party BPMN engine)
- Version: 2.3.0

## Confirmed Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot microservices |
| Database | PostgreSQL 14+ OR Oracle 19c+ |
| Migrations | Liquibase |
| Process engine | Amplio Process Engine (primary); Camunda 7 (legacy/supported); Camunda 8 (deprecated in 2.3.0) |
| Business rules | Drools KIE (NO native DMN) |
| Auth | Keycloak OAuth 2.0 + OIDC (NOT SAML2) |
| Authorisation | OPA (Open Policy Agent) with Rego policies + JPA row-level security |
| Messaging | Apache Kafka + Outbox Pattern |
| Cache | Redis (session), Hazelcast (distributed process state across Amplio nodes) |
| Frontend | Angular (revenue-management-ui + self-service-ui) |
| Reporting | Jasper Reports |
| Container | Kubernetes (customer-hosted), Helm v3.x |
| Observability | ELK Stack (Elasticsearch, Logstash, Kibana, Filebeat, Metricbeat, APM) |
| Batch (legacy) | Spring Cloud Data Flow (SCDF) |
| Batch (current) | Foundation Batch Engine (Spring Batch-based, Amplio-native) |

## Repository Structure (10 repos)

| Repo | Purpose |
|---|---|
| revenue-management | Core domain services |
| revenue-management-ui | Angular staff UI |
| self-service-ui | Taxpayer self-service portal |
| revenue-management-etl | SCDF legacy batch pipelines |
| revenue-management-batch | Foundation Batch (current) |
| revenue-management-config | Spring Cloud Config server |
| revenue-management-bpmn | BPMN/DMN process definitions |
| revenue-management-rules | Drools KIE rules |
| revenue-management-infra | Kubernetes Helm charts |
| revenue-management-keycloak | Keycloak realm configuration |

## Financial Domain Capabilities

- Tax registration and taxpayer entity management
- Assessment and bill generation (bill segments, bill cycles)
- Payment plans (instalment arrangements, deferred payment)
- Write-off processing
- ISO 20022 payment file integration (PAIN.002 inbound; SWIFT_CREDIT_TRANSFER only; 10 MB file limit)
- Debt recovery workflow (demand → suppression check → warrant → enforcement)
- Case handover to collection agencies (CASE_HANDOVER_COLLECTION_AGENCY_PROCESS)
- Suppression / breathing space handling (CreateSuppression → SuspendCaseActivity → ReleaseSuppression → ResumeCaseActivity)

## 28 BPMN Reference Processes

All processes are in `revenue-management-bpmn` and are triggerable via Kafka command or API.

| # | Process File | Purpose | Primary Trigger |
|---|---|---|---|
| 1 | REGISTRATION_PROCESS.bpmn | Taxpayer registration, KYC, approval | Manual / API |
| 2 | ASSESSMENT_PROCESS.bpmn | Tax assessment calculation and issuance | Scheduled / API |
| 3 | BILL_GENERATION_PROCESS.bpmn | Bill segment creation and delivery | Bill cycle scheduler |
| 4 | PAYMENT_PLAN_PROCESS.bpmn | Instalment plan setup and lifecycle | Manual / API |
| 5 | DEBT_RECOVERY_PROCESS.bpmn | Demand letter → suppression check → warrant letter | Overdue trigger |
| 6 | CASE_HANDOVER_COLLECTION_AGENCY_PROCESS.bpmn | Hand debt to external DCA | Manual / escalation |
| 7 | WRITE_OFF_PROCESS.bpmn | Write-off authorisation and posting | Manual / automated |
| 8 | OBJECTION_PROCESS.bpmn | Taxpayer objection / dispute handling | Taxpayer submission |
| 9 | APPEAL_PROCESS.bpmn | Formal appeal to tribunal | Objection escalation |
| 10 | REFUND_PROCESS.bpmn | Overpayment refund calculation and payment | Credit balance trigger |
| 11 | AUDIT_PROCESS.bpmn | Compliance audit lifecycle | Scheduled / manual |
| 12 | CORRESPONDENCE_PROCESS.bpmn | Outbound letter/email/SMS generation | Event-driven |
| 13 | NOTIFICATION_PROCESS.bpmn | Real-time alert and notification dispatch | Event-driven |
| 14 | DOCUMENT_MANAGEMENT_PROCESS.bpmn | Document upload, classification, storage | User action |
| 15 | TASK_ASSIGNMENT_PROCESS.bpmn | Work item routing and queue assignment | Event-driven |
| 16 | ESCALATION_PROCESS.bpmn | SLA breach and supervisor escalation | Timer boundary |
| 17 | DATA_VALIDATION_PROCESS.bpmn | Inbound data quality and completeness checks | API / batch |
| 18 | REPORTING_PROCESS.bpmn | Jasper report generation and distribution | Scheduled |
| 19 | INTEGRATION_SYNC_PROCESS.bpmn | External system data synchronisation | Scheduled / event |
| 20 | USER_MANAGEMENT_PROCESS.bpmn | User onboarding, role assignment | Admin action |
| 21 | BATCH_MONITORING_PROCESS.bpmn | Batch job status and failure alerting | Batch engine events |
| 22 | RISK_ASSESSMENT_PROCESS.bpmn | Risk scoring via external risk engine | Assessment trigger |
| 23 | COMPLIANCE_CHECK_PROCESS.bpmn | Regulatory compliance verification | Periodic / event |
| 24 | PAYMENT_ALLOCATION_PROCESS.bpmn | Payment received → allocate to accounts | Payment event |
| 25 | INSTALMENT_BREACH_PROCESS.bpmn | Payment plan breach detection and response | Payment missed |
| 26 | ENFORCEMENT_PROCESS.bpmn | Legal enforcement action lifecycle | Warrant issued |
| 27 | INSOLVENCY_PROCESS.bpmn | Insolvency event handling and suspension | Insolvency signal |
| 28 | CASE_CLOSURE_PROCESS.bpmn | End-to-end case closure and archival | Zero balance / write-off |

**Note:** DEBT_RECOVERY_PROCESS flow is demand letter → SUSPEND_PROCESS_CHECK (suppression check) → warrant letter. DCA handover is a SEPARATE process (CASE_HANDOVER_COLLECTION_AGENCY_PROCESS).

## Kafka Command Catalogue (24 commands)

All async commands use Outbox Pattern. Topics follow pattern: `solon.{domain}.commands`.

| Command | Topic | Response Mechanism |
|---|---|---|
| StartProcessCommand | solon.process.commands | ProcessStartedEvent |
| CompleteTaskCommand | solon.task.commands | TaskCompletedEvent |
| CreateSuppressionCommand | solon.suppression.commands | SuppressionCreatedEvent |
| ReleaseSuppressionCommand | solon.suppression.commands | SuppressionReleasedEvent |
| CreateBillCommand | solon.billing.commands | BillCreatedEvent |
| PostPaymentCommand | solon.payment.commands | PaymentPostedEvent |
| AllocatePaymentCommand | solon.payment.commands | PaymentAllocatedEvent |
| InitiateRefundCommand | solon.refund.commands | RefundInitiatedEvent |
| WriteOffDebtCommand | solon.writeoff.commands | DebtWrittenOffEvent |
| CreatePaymentPlanCommand | solon.paymentplan.commands | PaymentPlanCreatedEvent |
| BreachPaymentPlanCommand | solon.paymentplan.commands | PaymentPlanBreachedEvent |
| ClosePaymentPlanCommand | solon.paymentplan.commands | PaymentPlanClosedEvent |
| SendCorrespondenceCommand | solon.correspondence.commands | CorrespondenceSentEvent |
| AssignTaskCommand | solon.workallocation.commands | TaskAssignedEvent |
| EscalateTaskCommand | solon.workallocation.commands | TaskEscalatedEvent |
| RegisterTaxpayerCommand | solon.registration.commands | TaxpayerRegisteredEvent |
| IssueAssessmentCommand | solon.assessment.commands | AssessmentIssuedEvent |
| RaiseObjectionCommand | solon.objection.commands | ObjectionRaisedEvent |
| HandoverToDCACommand | solon.collections.commands | HandoverCompletedEvent |
| ExecuteEnforcementCommand | solon.enforcement.commands | EnforcementExecutedEvent |
| RecordInsolvencyCommand | solon.insolvency.commands | InsolvencyRecordedEvent |
| CloseCaseCommand | solon.case.commands | CaseClosedEvent |
| RiskRequestCommand | solon.risk.commands | RISK_RESULT_SIGNAL (Amplio signal) |
| ExecuteKieRulesCommand | SYNCHRONOUS HTTP (not Kafka) | Drools rule result in response body |

**Risk engine integration:** RiskRequestCommand → external risk service → RISK_RESULT_SIGNAL back to Amplio. Risk threshold = 40; IBAN change frequency >5/month triggers high-risk flag.

**ISO 20022:** PAIN.002 inbound for payment status reporting. SWIFT_CREDIT_TRANSFER only (no SEPA). File size limit 10 MB.

## Batch Jobs (7 jobs, Foundation Batch Engine)

| Job | Schedule | Purpose |
|---|---|---|
| BillCycleJob | Monthly (configurable) | Generate bill segments for all active accounts |
| PaymentMatchingJob | Daily 02:00 | Match received payments to accounts |
| DebtAgingJob | Daily 01:00 | Age debt buckets, trigger recovery thresholds |
| SuppressionExpiryJob | Daily 06:00 | Release expired suppressions |
| WriteOffCandidateJob | Weekly Sunday 03:00 | Identify write-off eligible debts |
| ReportingJob | Daily 07:00 | Generate Jasper operational reports |
| IntegrationSyncJob | Every 4 hours | Sync with external systems |

## Data Area (Custom Fields)

JSONB (PostgreSQL) or BLOB (Oracle) custom field extension on 18 entities across 5 databases:
- Taxpayer, Assessment, Bill, PaymentPlan, Payment, Refund, WriteOff, Objection, Appeal, Task, Case, Suppression, Enforcement, Insolvency, Document, Correspondence, AuditEntry, RiskAssessment

## Customisation Model

- Standard layer: shipped code; custom layer: customer-specific code, separate Maven modules
- Custom code: BPMN processes, Drools rules, Spring beans (auto-discovered via component scan)
- Runtime overrides: BPMN Process Variables can be overridden without redeployment (Amplio Admin UI)
- Configuration: Spring Cloud Config server (Git-backed); environment-specific profiles
- Data Area: JSONB/BLOB custom fields on all 18 core entities

## Security Model

- Authentication: Keycloak OAuth2 + OIDC; all services are resource servers; JWT claims carry roles
- Authorisation: OPA (Open Policy Agent) with Rego policies; decisions made at API gateway level
- Row-level security: JPA filter annotations on repositories; enforced per-user data scope
- Service-to-service: OAuth2 Client Credentials grant

## Infrastructure (Kubernetes)

**Namespace model:**
- `solon-core` — domain microservices
- `solon-process` — Amplio engine pods
- `solon-infra` — Kafka, Redis, Hazelcast, Keycloak, Config server
- `solon-batch` — batch engine pods
- `solon-monitoring` — ELK stack, APM

**HA configuration (key components):**

| Component | HA Mode |
|---|---|
| Amplio | 3+ pods, Hazelcast cluster for shared state |
| Kafka | 3 brokers, replication factor 3 |
| PostgreSQL | Primary + 1 read replica minimum |
| Keycloak | 2+ pods, shared DB |
| Redis | Sentinel mode (3 nodes) |

## Monitoring and Observability

- ELK Stack: Filebeat (log shipping) → Logstash → Elasticsearch → Kibana
- APM: Elastic APM agents on all services; distributed tracing
- Structured log format: JSON; mandatory fields: `traceId`, `spanId`, `serviceId`, `correlationId`
- Batch monitoring: dedicated Kibana dashboard; REST API for job status (`GET /batch/jobs/{jobId}`)
- DB monitoring: pg_stat_activity, slow query log, Metricbeat

## Backup and Recovery

- Database: daily full backup + WAL continuous archiving (PITR)
- Kafka: recovery via Outbox replay (messages persisted in DB before Kafka publish)
- BPMN: process definitions stored in database (versioned); recovered with DB restore
- RPO: 1 hour; RTO: 4 hours (per operations guide)

## Hard Incompatibilities with DCMS (ADR-016 v2.0)

1. Amplio: no non-interrupting message boundary events (criminal liability — Debt Respite Scheme 2020)
2. No native DMN (Drools only — ADR-008 non-technical DMN authoring requirement unmet)
3. Java 17 (DCMS requires Java 21; ADR-014 exhaustive switch requires JDK 21)
4. Angular frontend (DCMS mandates GOV.UK Design System with React)
5. Liquibase (DCMS mandates Flyway Community Edition)
6. Parallel gateways sequential-only (DCMS needs true concurrent parallel execution)
7. Net scope reduction only 5-15% — insufficient to justify pivot
