> ## STATUS: PRIMARY DESIGN INPUT — PLATFORM PIVOT IN EFFECT
>
> Following ADR-018 (2026-04-30), Solon Tax is now the confirmed base platform for DCMS. This document is a **primary design input** for the new design process — it describes what the base platform provides. Read it to understand what Solon Tax offers; use it to determine where Solon Tax satisfies requirements natively and where new code must be written on top.

---

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
| Frontend | Angular per vendor docs — **believed to actually be React, needs confirmation** (revenue-management-ui + self-service-ui) |
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

## BPMN Reference Processes

> **CORRECTED 2026-05-01.** Earlier versions of this document listed 28 invented BPMN filenames (e.g. `AUDIT_PROCESS.bpmn`, `COMPLIANCE_CHECK_PROCESS.bpmn`, `RISK_ASSESSMENT_PROCESS.bpmn`, `BATCH_MONITORING_PROCESS.bpmn`) that do not exist in the Solon Tax distribution. The list below is the actual file inventory from `external_sys_docs/solon/bpmn/irm/` (Amplio process definitions). The authoritative source is the BPMN files themselves and the process descriptions in the Solon Tax integration guide.

All processes ship as `*_AMPLIO.bpmn` files in `revenue-management-bpmn` and are triggerable via Kafka command or API.

| Process File | Purpose |
|---|---|
| BILL_SEGMENT_APPROVAL_AMPLIO.bpmn | Bill segment approval workflow |
| BILL_SEGMENT_CANCELLATION_AMPLIO.bpmn | Bill segment cancellation workflow |
| BUS_REG_AMPLIO.bpmn | Business registration |
| CASE_DEMAND_LETTER_AMPLIO.bpmn | Demand letter step within debt recovery |
| CASE_HANDOVER_COLLECTION_AGENCY_PROCESS_AMPLIO.bpmn | Hand debt to external DCA |
| CASE_WARRANT_LETTER_AMPLIO.bpmn | Warrant letter step within debt recovery |
| CIT_RETURN_AMPLIO.bpmn | Corporate income tax return |
| DEBT_RECOVERY_PROCESS_AMPLIO.bpmn | Top-level debt recovery: demand → suppression check → warrant |
| DIRECT_DEPOSIT_AMPLIO.bpmn | Direct deposit handling |
| HUMAN_TASK_BEHAVIOUR_TEST_AMPLIO.bpmn | Test/reference process for human task behaviour |
| MUNICIPAL_TAX_EXAMPLE_AMPLIO.bpmn | Municipal tax example process |
| NOTIFY_GDPR_EXPIRATION_AMPLIO.bpmn | GDPR data expiration notification |
| NOTIFY_OVERDUE_DEBTORS_AMPLIO.bpmn | Overdue debtor notification |
| OBJECTION_PROCESS_AMPLIO.bpmn | Taxpayer objection / dispute handling |
| OSS_REG_AMPLIO.bpmn | OSS (One-Stop-Shop) registration |
| OSS_VAT_AMPLIO.bpmn | OSS VAT process |
| O_PLN_PRVL_AMPLIO.bpmn | Payment plan privilege / approval |
| PAYMENT_DEFERRAL_AMPLIO.bpmn | Payment deferral handling |
| P_ROLE_LIN_AMPLIO.bpmn | Party role linkage |
| SCHEDULED_OPERATIONAL_REPORT_AMPLIO.bpmn | Scheduled operational report generation |
| SUSPEND_PROCESS_CHECK_AMPLIO.bpmn | Suppression check invoked from debt recovery |
| TAX_RETURN_LATE_FILLER_CASE_ACTIVITY_PROCESS_AMPLIO.bpmn | Late tax return filer case activity |
| VAT_EU_AMPLIO.bpmn | EU VAT process |
| VAT_OVERPAYMENT_PROCESS_AMPLIO.bpmn | VAT overpayment / refund |
| VAT_RETURN_AMPLIO.bpmn | VAT return |

**Self-service (ISS) processes** also ship under `bpmn/iss/` (e.g. `AGREEMENT_AMPLIO_ISS.bpmn`, `BUS_REG_AMPLIO_ISS.bpmn`).

**Note:** DEBT_RECOVERY_PROCESS_AMPLIO flow is demand letter → SUSPEND_PROCESS_CHECK_AMPLIO (suppression check) → warrant letter. DCA handover is a SEPARATE process (CASE_HANDOVER_COLLECTION_AGENCY_PROCESS_AMPLIO).

## Kafka Command Catalogue

> **CORRECTED 2026-05-01.** Earlier versions of this document used an invented topic naming pattern `solon.{domain}.commands`. The authoritative pattern is `irm.{service}.{event-type}` per `external_sys_docs/solon/KAFKA_EVENT_CATALOG.md` and `external_sys_docs/solon/solon_tax_2.3.0_integration_guide.md` §5. Inbound BPMN engine commands use the `irm.bpmn-engine.*` namespace. The full topic catalogue is in `KAFKA_EVENT_CATALOG.md` — that file is authoritative; the table below is a representative sample.

All async commands use Outbox Pattern. Topic naming convention: `irm.{service-name}.{event-type}`.

> **Important architectural note:** The `irm.bpmn-engine.*` topics are the BPMN engine's *private* internal command channel between the Amplio engine and Solon services. The DCMS additional layer does NOT publish to those topics. The additional layer integrates with Solon Tax via REST APIs (see `external_sys_docs/solon/api-usage-guide.md`). DCMS may *consume* selected outbound `irm.{service}.*` event topics for reactive flows, but command issuance to Solon is REST-only.

Representative topic samples (full list in `KAFKA_EVENT_CATALOG.md`):

| Topic | Direction | Purpose |
|---|---|---|
| `irm.bpmn-engine.create-suppression` | Inbound (engine → suppression-mgmt) | Create a suppression |
| `irm.bpmn-engine.release-suppression` | Inbound | Release a suppression |
| `irm.bpmn-engine.suspend-case-activity` | Inbound | Suspend case activity (`SuspendCaseActivityCommand`) |
| `irm.bpmn-engine.resume-case-activity` | Inbound | Resume case activity (`ResumeCaseActivityCommand`) |
| `irm.bpmn-engine.create-human-task` | Inbound | Create a human task |
| `irm.bpmn-engine.create-contact` | Inbound | Trigger contact via correspondence |
| `irm.bpmn-engine.case-creation` | Inbound | Start a new case |
| `irm.bpmn-engine.send-data-to-collection-agency` | Inbound | DCA handover data |
| `irm.bpmn-engine.screen-risk` | Inbound | Risk screening request |
| `irm.process-management.process-flow-status-changed` | Outbound | Process flow status change event |
| `irm.suppression-management.suppression-status-changed` | Outbound | Suppression status change (consumed by `taxpayer-accounting-custom`, `case-management-custom`, `process-management-custom`) |
| `irm.case-management.entity-version` | Outbound | Entity version change event |
| `irm.taxpayer-accounting.balance-updated` | Outbound | Account balance updated |
| `irm.taxpayer-accounting.signal-process-flow` | — | Signal a process flow node |
| `irm.taxpayer-accounting.trigger-allocation` | Outbound | Trigger payment allocation |

**Synchronous (non-Kafka) integration:** Drools KIE rule execution — `POST /drools-runtime/execute-stateless/{containerId}` (HTTP), result returned in response body. Latency and availability of this endpoint are part of any DCMS request path that depends on it.

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

> **CORRECTED 2026-05-01.** Earlier versions of this document used invented namespace names (`solon-core`, `solon-process`, `solon-infra`, `solon-batch`, `solon-monitoring`). The actual namespace pattern, per `external_sys_docs/solon/solon_tax_2.3.0_operations_guide.md` §3.1, uses an `[env]-` prefix and is shown below.

**Namespace model:**
- `[env]-platform-ms` — Core microservices (`revenue-management-be`)
- `[env]-aux-camunda-platform` — Camunda 7/8 / Amplio process engine pods
- `[env]-platform-adapters` — Integration adapter services
- `[env]-platform-batch` — Batch processing services (SCDF, Revenue Management Batch)
- `[env]-platform-infra` — Infrastructure components (Kafka, Redis, Keycloak, Hazelcast, Config server, ELK)

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
