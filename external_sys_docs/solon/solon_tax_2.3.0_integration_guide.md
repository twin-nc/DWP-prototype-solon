# SOLON TAX — Integration Guide v2.3.0
## Comprehensive Reference Document

> **Source:** Netcompany SOLON TAX Integration Guide, release 2.3.0 (539 pages).
> This document was produced by systematically extracting all substantive text from the source PDF using
> PyMuPDF, supplemented by targeted reading of all key sections. The integration guide is heavily
> diagram-based; all readable text and table content has been captured in full, and context from
> image-based sections has been inferred from diagram structures visible at 150 DPI.

---

## Table of Contents

1. [Document Scope and Purpose](#1-document-scope-and-purpose)
2. [Architecture Overview](#2-architecture-overview)
3. [Amplio Process Engine — Architecture](#3-amplio-process-engine--architecture)
   - 3.1 Engine Components
   - 3.2 Supported BPMN Elements
   - 3.3 Amplio vs. Camunda 8 Differences
4. [BPMN Process Architecture](#4-bpmn-process-architecture)
   - 4.1 Process Triggering Model
   - 4.2 Human Task Model
   - 4.3 Suppression Model
   - 4.4 SAGA Pattern
   - 4.5 Outbox Pattern
5. [Asynchronous Activities — Kafka Commands](#5-asynchronous-activities--kafka-commands)
6. [Synchronous Activities — HTTP Commands](#6-synchronous-activities--http-commands)
7. [Risk Analysis Integration](#7-risk-analysis-integration)
8. [Payment File Integration (ISO20022)](#8-payment-file-integration-iso20022)
9. [Batch Integration Architecture](#9-batch-integration-architecture)
10. [Data Area (Custom Fields)](#10-data-area-custom-fields)
11. [Archiving Process](#11-archiving-process)
12. [GDPR and Data Retention](#12-gdpr-and-data-retention)
13. [BPMN Reference Implementations — All 28 Processes](#13-bpmn-reference-implementations--all-28-processes)
    - AGREEMENT.bpmn
    - BILL_SEGMENT_APPROVAL.bpmn
    - BILL_SEGMENT_CANCELLATION.bpmn
    - BUS_REG.bpmn
    - CASE_DEMAND_LETTER.bpmn
    - CASE_HANDOVER_COLLECTION_AGENCY_PROCESS.bpmn
    - CASE_WARRANT_LETTER.bpmn
    - CIT_RETURN.bpmn
    - DEBT_RECOVERY_PROCESS.bpmn
    - DEMO_PROCESS_TASK.bpmn
    - DIRECT_DEPOSIT.bpmn
    - HUMAN_TASK_BEHAVIOUR_TEST
    - INCOME_TAX.bpmn
    - MUNICIPAL_TAX_EXAMPLE.bpmn
    - NOTIFY_GDPR_EXPIRATION.bpmn
    - NOTIFY_OVERDUE_DEBTORS.bpmn
    - O_PLN_PRVL.bpmn
    - OBJECTION_PROCESS.bpmn
    - OSS_REG.bpmn
    - OSS_VAT.bpmn
    - P_ROLE_LIN.bpmn
    - PAYMENT_DEFERRAL.bpmn
    - SCHEDULED_OPERATIONAL_REPORT.bpmn
    - SUSPEND_PROCESS_CHECK.bpmn
    - TAX_RETURN_LATE_FILLER_CASE_ACTIVITY_PROCESS.bpmn
    - VAT_EU.bpmn
    - VAT_OVERPAYMENT_PROCESS.bpmn
    - VAT_RETURN.bpmn
14. [Appendix: Human Task Type Codes](#14-appendix-human-task-type-codes)
15. [Appendix: Event and Signal Reference](#15-appendix-event-and-signal-reference)

---

## 1. Document Scope and Purpose

The SOLON TAX Integration Guide describes:
- How SOLON TAX integrates with external systems (banks, collection agencies, risk systems, document management, etc.)
- The BPMN process architecture and all reference process implementations
- The Amplio Process Engine's capabilities and constraints
- Kafka-based asynchronous command flows
- Batch integration architecture
- Data area (custom fields) extension model

This document is the primary technical reference for:
- Implementation teams extending or customising SOLON TAX
- Integration architects connecting SOLON TAX to third-party systems
- Business analysts mapping SOLON TAX processes to functional requirements

---

## 2. Architecture Overview

SOLON TAX is a cloud-native, microservices-based Revenue Management Platform. Integration follows three primary patterns:

### 2.1 Synchronous REST Integration

Services communicate synchronously via REST/HTTP. All endpoints are secured with OAuth2 Bearer tokens issued by Keycloak. Key synchronous integration points:
- Drools KIE rules execution: `POST /drools-runtime/execute-stateless/{containerId}`
- Inter-service REST calls (internal): via Kubernetes service DNS
- External system calls: via integration adapter services (`revenue-management-adapters`)

### 2.2 Asynchronous Kafka Integration

The primary asynchronous integration mechanism. The BPMN engine issues **commands** onto Kafka topics; target services consume and execute, then signal back via Kafka if a response is needed.

All Kafka events follow the **Outbox Pattern**: events are written to an outbox table in the source service's database before being published to Kafka. This guarantees no event loss even under Kafka outages.

### 2.3 BPMN Process Orchestration

All multi-step business processes are orchestrated by the Amplio Process Engine (or Camunda 7) using BPMN 2.0 process definitions. Process instances are triggered by:
- Domain events (e.g., a bill segment created, a case status changed)
- Batch job tasks (Spring Batch step delegates)
- Quartz scheduler (for time-based processes)
- Manual triggers from the UI (via REST API call)

---

## 3. Amplio Process Engine — Architecture

### 3.1 Engine Components

The Amplio Process Engine is Netcompany's first-party BPMN execution engine. It is designed for cloud-native deployment and integrates natively with the Amplio platform's microservices.

**Core components:**
- **BPMN Interpreter:** Parses and executes BPMN 2.0 XML process definitions
- **Process Instance Store:** Persists process instance state in the configured database
- **Command Router:** Routes service task commands to the appropriate handler (sync HTTP or async Kafka)
- **Human Task Manager:** Creates and manages human tasks in the human-task-management microservice
- **Hazelcast Integration:** Distributed state for multi-node deployments
- **Admin API:** REST API for process administration (used by the Amplio Admin UI)
- **Outbox Publisher:** All async commands are written to outbox before Kafka publication
- **JDBC Message Queue:** Underlying mechanism for the Event Stream Management feature

**Camunda 7 vs. Amplio Process Engine:**
As of SOLON TAX 2.3.0, both engines are supported. Camunda 8 is deprecated. The choice of engine is a deployment configuration decision; the BPMN diagrams are compatible with both (with constraints noted in §3.3).

### 3.2 Supported BPMN Elements

The Amplio Process Engine supports a defined subset of BPMN 2.0 elements. The following table summarises support:

#### Tasks

| Element | Support Level | Notes |
|---|---|---|
| Service Task | Partial | HTTP-JSON connector supported; see §3.3 for differences from Camunda 8 |
| User Task | Full | Maps to human task creation in human-task-management service |
| Business Rule Task | Partial | No native DMN; must be implemented as Service Task with custom Java delegate |
| Script Task | Partial | FEEL expressions supported; no job worker implementations |
| Undefined Task | Full | Treated as an empty task; used as placeholder in diagrams |

#### Gateways

| Element | Support Level | Notes |
|---|---|---|
| Exclusive Gateway (XOR) | Full | Standard conditional branching |
| Parallel Gateway | Partial | Branches execute sequentially (not concurrently); no cross-branch variable dependencies allowed |
| Event-based Gateway | Full | Waits for the first of multiple events to occur |

#### Intermediate Catch Events

| Element | Support Level | Notes |
|---|---|---|
| Timer Intermediate Catch | Full | ISO 8601 duration or date expressions |
| Signal Intermediate Catch | Full | Receives named signals; used for async response pattern |
| Message Intermediate Catch | Full | Receives named messages |
| Link Catch | Full | Connects to corresponding Link Throw within same process |

#### Boundary Events

| Element | Support Level | Notes |
|---|---|---|
| Error Boundary | Full | Catches errors thrown by tasks |
| Message Boundary | Partial | Interrupting only; no early-message buffering (message arriving before the event is reached will be lost) |

#### Intermediate Throw Events

| Element | Support Level | Notes |
|---|---|---|
| Link Throw | Full | Jumps to corresponding Link Catch |
| Error Throw | Full | Propagates error to error boundary handler |

#### Subprocesses and Call Activities

| Element | Support Level | Notes |
|---|---|---|
| Embedded Subprocess (collapsed) | Full | |
| Embedded Subprocess (expanded) | Full | |
| Call Activity | Full | Calls a separate named process definition; used for sub-process composition |

#### Start and End Events

| Element | Support Level |
|---|---|
| None Start Event | Full |
| None End Event | Full |
| Error End Event | Full |

### 3.3 Amplio vs. Camunda 8 Differences

The following differences are critical for teams developing or porting BPMN processes:

| Feature Area | Camunda 8 Behaviour | Amplio Behaviour | Impact |
|---|---|---|---|
| **Service Task — HTTP query parameters** | Separate key-value pair configuration in Zeebe connector | Must be expressed as a single JSON object string | Any existing Camunda 8 processes using HTTP connectors with query params need adjustment |
| **Business Rule Task** | Native DMN (Decision Model and Notation) support | No native DMN; must implement as Service Task with custom Java | DMN-based decision tables cannot be used directly |
| **Script Task execution** | Supports multiple expression languages; job worker pattern | FEEL expressions only; no job worker pattern | Complex scripts must use FEEL; imperative scripts need refactoring to service tasks |
| **Parallel Gateway execution** | Branches execute in parallel (concurrent threads) | Branches execute sequentially in definition order | Cannot rely on parallel execution for performance; cross-branch dependencies forbidden |
| **Multi-Instance Activity** | Parallel (concurrent) or sequential | Sequential only | Bulk parallel processing requires alternative design |
| **Message Boundary Event** | Interrupting and non-interrupting; messages buffered if arriving early | Interrupting only; early messages lost | Non-interrupting message boundaries not supported; process design must avoid reliance on early message buffering |

---

## 4. BPMN Process Architecture

### 4.1 Process Triggering Model

BPMN processes in SOLON TAX are triggered by **event plans** — configured mappings between **event types** and **process types**.

**Triggering mechanisms:**

| Trigger Type | Mechanism | Example |
|---|---|---|
| Domain event | A microservice emits an event (via Outbox/Kafka); the process engine consumes and starts a process instance | Bill segment created → `BILL_SEGMENT_APPROVAL.bpmn` starts |
| Batch task | A Spring Batch step delegate calls the process engine to start a process instance | `debt-recovery-task` starts `DEBT_RECOVERY_PROCESS.bpmn` |
| Quartz schedule | A Quartz trigger fires and initiates a process | `SCHEDULED_OPERATIONAL_REPORT.bpmn` on cron schedule |
| Manual/UI | User action triggers a REST API call to start a process | Caseworker opens objection → `OBJECTION_PROCESS.bpmn` starts |
| Parent process | Call Activity in a parent process starts a child process | `DEBT_RECOVERY_PROCESS.bpmn` starts `CASE_DEMAND_LETTER.bpmn` |

**Eventplans system parameter:** Each event type-to-process mapping is managed via the Eventplans system parameter in Amplio Admin. Subscriptions can be disabled to temporarily prevent a process from being triggered for a given event type, without code changes.

### 4.2 Human Task Model

User Tasks in BPMN correspond to **human tasks** created in the `human-task-management` microservice.

**Human task creation flow:**
1. BPMN engine reaches a User Task node
2. Engine issues a `CreateHumanTaskCommand` on Kafka topic `irm.bpmn-engine.create-human-task`
3. `human-task-management` service creates the task record
4. Task appears in the caseworker's task list in the UI
5. Caseworker completes the task; this sends a signal back to the BPMN engine
6. Engine receives the signal and proceeds past the User Task

**Human task behaviours (configurable per task definition):**
- **Auto-start:** Task starts automatically without caseworker explicitly picking it up
- **Auto-assign:** Task is automatically assigned to a specific caseworker or role

**Human task type codes** (referenced in process definitions):

| Code | Description |
|---|---|
| `DBTDL1APPR` | Bill segment debit level-1 approval |
| `DBTDL2APPR` | Bill segment debit level-2 approval |
| `DBTDL3APPR` | Bill segment debit level-3 approval |
| `DBTLV1CANC` | Bill segment debit level-1 cancellation approval |
| `CRDLV1CANC` | Bill segment credit level-1 cancellation approval |
| `RevForExc1` | Review for exception (level 1) |
| `ReqAddInf1` | Request additional information (level 1) |
| `RevForRis1` | Review for risk (level 1) |
| `RevPostErr` | Review posting error |
| `RevRevErr` | Review reversion error |
| `DirDepErr` | Direct deposit error review |
| `VerCoplObj` | Verify compliance of objection |
| `HearRequir` | Hearing requirements |
| `PerfoHear` | Perform hearing |
| `ObjDecsion` | Objection decision |
| `ImplObjDec` | Implement objection decision |
| `EdtSndLtr` | Edit and send letter |
| `OPPLNAPPTK` | Operational plan approval task |

### 4.3 Suppression Model

A **suppression** is a mechanism to pause (suspend) case activities while a condition exists (e.g., an active objection, a payment deferral in progress).

**Suppression lifecycle:**
1. Process issues `CreateSuppressionCommand` → `irm.bpmn-engine.create-suppression` → suppression-management
2. Suppression record is created with an action type (e.g., `DEBT_REC_C`)
3. `SuspendCaseActivityCommand` → `irm.bpmn-engine.suspend-case-activity` → case-management-custom
4. When the condition resolves: `ReleaseSuppressionCommand` → `irm.bpmn-engine.release-suppression` → suppression-management
5. `ResumeCaseActivityCommand` → `irm.bpmn-engine.resume-case-activity` → case-management-custom

The `SUSPEND_PROCESS_CHECK.bpmn` process implements the check loop: it queries for active suppressions on a liability with `DEBT_REC_C` action type and suspends case activity while a suppression is active.

### 4.4 SAGA Pattern

The SAGA Pattern is used for distributed transactions spanning multiple microservices. In SOLON TAX, the BPMN process itself acts as the SAGA coordinator:

- Each step in the process is a compensatable transaction
- If a step fails, compensating actions are triggered (e.g., reversing a created bill segment)
- Error boundary events on tasks catch failures and route to compensation logic
- The process context preserves the state needed to execute compensations

### 4.5 Outbox Pattern

Every Kafka event published by a SOLON TAX service is first written to an outbox table in the service's own database. A background process (typically using Debezium CDC or a polling mechanism) reads the outbox and publishes to Kafka.

**Benefits:**
- Atomicity: the business operation and the event publication are in the same database transaction
- Resilience: if Kafka is down, events accumulate in the outbox and are published when Kafka recovers
- Recoverability: Kafka topics can be fully reconstructed by replaying the outbox
- Observability: the JDBC message queue is inspectable via Amplio Admin → Event Stream Management

---

## 5. Asynchronous Activities — Kafka Commands

The BPMN engine issues the following **commands** onto Kafka topics as part of process execution. Each command is a Service Task in the BPMN that writes to the outbox and publishes asynchronously.

Where a response is needed, the target service sends back a Kafka event which is consumed by the process engine as a **Signal** (Signal Intermediate Catch Event in the BPMN).

**Complete command catalogue (Table 24 equivalent):**

| Command Class | Kafka Topic | Target Service | Response Mechanism |
|---|---|---|---|
| `TransitionFormCommand` | `irm.bpmn-engine.transition-form-status` | ui-configuration | None (fire-and-forget) |
| `PushNotificationCommand` | `irm.bpmn-engine.push-notification-redis` | Notification | None (fire-and-forget) |
| `CreateHumanTaskCommand` | `irm.bpmn-engine.create-human-task` | human-task-management | Signal when task completed |
| `CreateContactCommand` | `irm.bpmn-engine.create-contact` | contact-management | None (fire-and-forget) |
| `CreateExceptionCommand` | `irm.bpmn-engine.create-exception` | exception-management | None (fire-and-forget) |
| `CloseExceptionCommand` | `irm.bpmn-engine.close-exception` | exception-management | None (fire-and-forget) |
| `RiskRequestCommand` | `irm.bpmn-engine.screen-risk` | risk-analysis | `RISK_RESULT_SIGNAL` |
| `TransitionOverpaymentProcessStatusCommand` | `irm.bpmn-engine.transition-overpayment-process-status` | taxpayer-accounting | None (fire-and-forget) |
| `TransitionProcessFlowStatusCommand` | `irm.bpmn-engine.transition-process-flow-status` | process-management | None (fire-and-forget) |
| `UpdateOperationalPlanStatusCommand` | `irm.bpmn-engine.update-operational-plan-status` | operational plan | None (fire-and-forget) |
| `CreateSuppressionCommand` | `irm.bpmn-engine.create-suppression` | suppression-management | None (fire-and-forget) |
| `ReleaseSuppressionCommand` | `irm.bpmn-engine.release-suppression` | suppression-management | None (fire-and-forget) |
| `UpdateBillSegmentDirectDepositStatusCommand` | `irm.bpmn-engine.update-bill-segment-direct-deposit-status` | taxpayer-accounting | None (fire-and-forget) |
| `SuspendCaseActivityCommand` | `irm.bpmn-engine.suspend-case-activity` | case-management-custom | None (fire-and-forget) |
| `ResumeCaseActivityCommand` | `irm.bpmn-engine.resume-case-activity` | case-management-custom | None (fire-and-forget) |
| `FreezeBillSegmentCommand` | `irm.bpmn-engine.bill-segment-service-freeze-command` | taxpayer-accounting-custom | None (fire-and-forget) |
| `CreateBillSegmentCommand` | `irm.bpmn-engine.create-bill-segment-for-case` | taxpayer-accounting-custom | None (fire-and-forget) |
| `ConfigureCaseScopeCommand` | `irm.bpmn-engine.configure-case-scope` | case-management-custom | None (fire-and-forget) |
| `CompleteChildCaseCommand` | `irm.bpmn-engine.complete-child-case` | case-management-custom | None (fire-and-forget) |
| `CreateStartChildCaseCommand` | `irm.bpmn-engine.create-start-child-case` | case-management-custom | None (fire-and-forget) |
| `TriggerDebtRecoveryProcessCommand` | `irm.bpmn-engine.trigger.debt.recovery.process` | case-management-custom | None (fire-and-forget) |
| `ObjectionCommand` | `irm.bpmn-engine.verify-timeliness` | case-management-custom | None (fire-and-forget) |
| `CheckTaxReturnLateFilerCommand` | `irm.bpmn-engine.check-tax-return-late-filer` | registration-management-custom | None (fire-and-forget) |
| `OverpaymentProcessStoreDataAreaCommand` | `irm.bpmn-engine.overpayment-process-store-data-area-command` | taxpayer-accounting-custom | None (fire-and-forget) |

---

## 6. Synchronous Activities — HTTP Commands

The BPMN engine also issues synchronous HTTP calls from Service Tasks:

| Command Class | HTTP Method + Endpoint | Target Service | Notes |
|---|---|---|---|
| `ExecuteKieRulesCommand` | `POST /drools-runtime/execute-stateless/{containerId}` | Drools KIE Runtime | Synchronous; response contains rule evaluation results |

The `ExecuteKieRulesCommand` is used for Drools rule execution within BPMN processes (e.g., VAT return form validation, risk alert evaluation, journal entry rule execution). The `containerId` identifies the KIE container (rule set version) to use.

---

## 7. Risk Analysis Integration

SOLON TAX integrates with an external or internal Risk Analysis microservice for risk screening of taxpayer submissions.

### 7.1 Risk Request

When a BPMN process reaches a risk screening step, it issues a `RiskRequestCommand` onto `irm.bpmn-engine.screen-risk`.

**`RiskRequest` payload:**

| Field | Type | Description |
|---|---|---|
| `ID` | string | Unique identifier for this risk request |
| `processInstanceId` | string | ID of the BPMN process instance requesting the screening |
| `signalName` | string | Name of the signal to send back when screening is complete (e.g., `RISK_RESULT_SIGNAL`) |
| `relatedEntities` | array | List of entities to screen |
| `relatedEntities[].entityId` | string | Identifier of the entity |
| `relatedEntities[].entityCL` | string | Entity class/type |
| `relatedEntities[].payload` | object | Entity-specific data for risk evaluation |

### 7.2 Risk Response

The Risk Analysis service evaluates the request and sends back a `RiskResponse` (via Kafka or direct signal), which the BPMN engine receives as the configured signal:

**`RiskResponse` payload:**

| Field | Type | Description |
|---|---|---|
| `ID` | string | Correlates with the original `RiskRequest.ID` |
| `processInstanceId` | string | The process instance to signal |
| `signalName` | string | The signal name to fire on the process instance |
| `riskObservations` | array | List of risk findings |
| `riskObservations[].riskId` | string | Identifier of the risk rule triggered |
| `riskObservations[].entityCL` | string | Entity class that triggered the risk |

### 7.3 Risk Threshold

In `VAT_RETURN.bpmn`, the risk screening uses a **risk threshold of 40**. If the risk score returned exceeds this threshold, the process routes to a human task for manual review (`RevForRis1`).

### 7.4 IBAN Change Risk Check

In `VAT_OVERPAYMENT_PROCESS.bpmn`, a specific risk check is performed: if a taxpayer's IBAN has been changed **more than 5 times in the previous month**, the refund is flagged for risk review. This check is implemented as part of the risk screening path within the overpayment process.

---

## 8. Payment File Integration (ISO20022)

### 8.1 Overview

SOLON TAX supports ingestion of bank statement payment files in ISO20022 format (PAIN.002 — Payment Status Report and CAMT formats for bank transaction data).

The `PAYMENT_FILE_BATCH_JOB` and the `DIRECT_DEPOSIT.bpmn` process work together to process payment files.

### 8.2 Validation Rules

The following validation rules apply to payment file ingestion:

| Rule | Description |
|---|---|
| File size | Must be less than the maximum configured file size. Default recommendation: **10 MB** (for performance reasons) |
| ISO20022 compliance | File must be valid according to ISO20022 schema; must support the specific format of bank transaction data used |
| Payment Segment Type | The Payment Segment Type used for payment segment distribution must be configured to support the payment upload process from bank statements |
| Tender Source | The tender source matching the bank account reference must have a valid tender type; currently only **`SWIFT_CREDIT_TRANSFER`** is supported |

### 8.3 DIRECT_DEPOSIT Process Integration

The `DIRECT_DEPOSIT.bpmn` process handles outgoing direct deposit payments (e.g., refunds):

1. Triggered by the `PAYMENT_FILE_BATCH_JOB` batch or the `VAT_OVERPAYMENT_PROCESS.bpmn`
2. Processes the PAIN.002 response file via Kafka
3. Bill segment direct deposit status progression: `READY_TO_SEND` → `COMPLETED_OK` or `COMPLETED_NOT_OK`
4. Human task `DirDepErr` is created if processing fails
5. Updates are sent to `taxpayer-accounting` via `UpdateBillSegmentDirectDepositStatusCommand`

In the `VAT_OVERPAYMENT_PROCESS.bpmn`, two `DIRECT_DEPOSIT` sub-processes run **in parallel** (as modelled in the BPMN with a parallel gateway — though Amplio executes them sequentially).

---

## 9. Batch Integration Architecture

### 9.1 Batch Modules

SOLON TAX includes two batch frameworks:

**`revenue-management-etl`** (Spring Cloud Data Flow / Spring Batch):
- Older, SCDF-orchestrated approach
- Modules: Authorization Management, Case Management, Compliance Monitor, Registration, Suppression Management, Taxpayer Accounting, Archiving, GDPR Anonymization

**`revenue-management-batch`** (Amplio-native Batch Engine):
- Newer approach; uses the same Admin UI as the Process Engine
- Modules: Registration, Taxpayer Accounting, Archiving, GDPR Anonymization

### 9.2 Batch Tasks That Trigger BPMN Processes

Many BPMN processes are triggered by Spring Batch step delegates (tasks). These batch tasks use the Kafka command mechanism or direct API calls to the process engine:

| Batch Task / Job | Triggers BPMN Process |
|---|---|
| `debt-recovery-task` | `DEBT_RECOVERY_PROCESS.bpmn` |
| `handover-debt` task | `CASE_HANDOVER_COLLECTION_AGENCY_PROCESS.bpmn` |
| `periodic-billing-municipal-tax-example-task` | `MUNICIPAL_TAX_EXAMPLE.bpmn` |
| `compliance-monitor-GDPR-expiration-task` | `NOTIFY_GDPR_EXPIRATION.bpmn` |
| `compliance-monitor-notify-overdue-debtors-task` | `NOTIFY_OVERDUE_DEBTORS.bpmn` |
| `late-filer-rule` batch | `TAX_RETURN_LATE_FILLER_CASE_ACTIVITY_PROCESS.bpmn` |
| `overpayment-process-task` | `VAT_OVERPAYMENT_PROCESS.bpmn` |

### 9.3 Batch Jobs (Revenue Management Batch Engine)

| Job Name | Description |
|---|---|
| `OBLIGATION_MAINTENANCE_BATCH_JOB` | Periodic obligation status recalculation and overdue flag updates |
| `TRIGGER_PENALTY_INTEGER_WITH_OVERDUE_BILL_SEGMENTS` | Identifies overdue bill segments and triggers penalty process |
| `ALLOCATION_MAINTENANCE` | Payment allocation maintenance across outstanding obligations |
| `PAYMENT_FILE_BATCH_JOB` | Processes incoming ISO20022 bank statement payment files |
| `LIABILITY_BALANCE_MAINTENANCE_BATCH_JOB` | Recalculates and updates liability balance summaries |
| `DELETE_NON_ASSOCIATED_ATTACHMENTS_BATCH_JOB` | Removes orphaned document attachments |
| `JOURNAL_ENTRY_EXPORT_BATCH_JOB` | Exports journal entries to external accounting systems |

### 9.4 Batch REST API

All batch jobs are manageable via the REST API exposed by the Revenue Management Batch service:

```
GET    /rest/api/batch/repository/jobs                              — List all jobs
POST   /rest/api/batch/engine/start?jobName={name}                 — Start a job
POST   /rest/api/batch/engine/restart?jobInstanceId={id}           — Restart a failed job
PUT    /rest/api/batch/engine/stop?jobInstanceId={id}              — Stop a running job
GET    /rest/api/batch/schedule/                                    — View all schedules
GET    /rest/api/batch/repository/jobs/{jobName}/instances         — Get job instances
GET    /rest/api/batch/repository/executions/{jobInstanceId}       — Get step executions
GET    /rest/api/batch/repository/executions/{jobExecutionId}      — Get single execution
GET    /rest/api/batch/item/status?jobInstanceId={id}              — Get item statuses
DELETE /rest/api/batch/item   (body: {"batchItemIds": ["string"]}) — Delete items
```

---

## 10. Data Area (Custom Fields)

The **Data Area** is a flexible custom fields mechanism available on key entities. Custom attributes are stored as JSON (jsonb in PostgreSQL, blob in Oracle) alongside the standard entity fields.

Custom fields can be:
- Defined per client without schema migrations
- Accessed and queried via standard entity APIs
- Used in BPMN process variables
- Managed through Drools rules and UI form configurations

**Entities supporting Data Area:**

| Database | Entity |
|---|---|
| `irm_casemanagement_db` | `case_` |
| `irm_contactmanagement_db` | `contact` |
| `irm_humantaskmanagement_db` | `human_task` |
| `irm_registration_db` | `obligation_action` |
| `irm_registration_db` | `party_role` |
| `irm_registration_db` | `party_role_relationship` |
| `irm_registration_db` | `revenue_type_registration` |
| `irm_risk_analysis_db` | `candidate_for_intervention` |
| `irm_taxpayeraccounting_db` | `bill_segment` |
| `irm_taxpayeraccounting_db` | `c_payment_file` |
| `irm_taxpayeraccounting_db` | `c_payment_record` |
| `irm_taxpayeraccounting_db` | `ft` (financial transaction) |
| `irm_taxpayeraccounting_db` | `overpayment_process` |
| `irm_taxpayeraccounting_db` | `payment_tender` |
| `irm_taxpayeraccounting_db` | `payment_tender_card` |
| `irm_taxpayeraccounting_db` | `payment_tender_check` |
| `irm_taxpayeraccounting_db` | `payment_tender_digital_wallet` |
| `irm_taxpayeraccounting_db` | `payment_tender_direct_debit` |
| `irm_taxpayeraccounting_db` | `payment_tender_ebt` |
| `irm_uiconfiguration_db` | `c_menu` |

---

## 11. Archiving Process

SOLON TAX includes an archiving framework for data lifecycle management.

**Archiving batch jobs** are provided in both `revenue-management-etl` and `revenue-management-batch`.

**Process:** Records exceeding their configured retention period are moved from operational tables to archive tables (or external cold storage). Archived data remains queryable for audit and legal purposes.

**Entities covered:**
- Case records (case management)
- Human tasks (human task management)
- Contact records
- Process instances (process engine)
- Financial transactions (taxpayer accounting)
- Documents and attachments (after `DELETE_NON_ASSOCIATED_ATTACHMENTS_BATCH_JOB` cleanup)

**Configuration:** Retention periods and archiving policies are configurable per entity type and per client deployment.

---

## 12. GDPR and Data Retention

### 12.1 GDPR Anonymisation Batch

The `GDPR_ANONYMIZATION` batch job (in both `revenue-management-etl` and `revenue-management-batch`) removes or anonymises personal data for data subjects whose retention period has expired.

### 12.2 GDPR Expiration Notification Process

The `NOTIFY_GDPR_EXPIRATION.bpmn` process:
- Triggered by the `compliance-monitor-GDPR-expiration-task` batch task
- Identifies all party roles whose GDPR consent expires within the **next 3 months**
- Sends a letter to each affected party role notifying them of the upcoming consent expiry
- Allows the data subject to renew consent or request data deletion

---

## 13. BPMN Reference Implementations — All 28 Processes

All reference BPMN processes are shipped in the `revenue-management-adapters` repository. They are deployable `.bpmn` files compatible with the Amplio Process Engine and Camunda 7.

---

### 13.1 `AGREEMENT.bpmn`
**PDF page:** 165

**Purpose:** Self-service delegation / consent agreement workflow.

**Trigger:** Taxpayer or representative initiates a delegation agreement request via the self-service portal.

**Process flow:**
1. Process starts with the agreement request data
2. A human task is created and assigned to the **recipient** of the delegation (the party being delegated to)
3. The recipient reviews and accepts or rejects the delegation
4. Push notifications are sent to relevant parties (via `PushNotificationCommand`)
5. Process completes when the delegation decision is recorded

**Key integrations:**
- `CreateHumanTaskCommand` → human-task-management (task assigned to recipient)
- `PushNotificationCommand` → Notification service (push notifications to browser/mobile)

**DWP relevance:** Third-party representative authorisation; agent acting on behalf of debtor.

---

### 13.2 `BILL_SEGMENT_APPROVAL.bpmn`
**PDF page:** 166

**Purpose:** Multi-level approval workflow for bill segment creation.

**Trigger:** A bill segment is created and requires approval before being finalised.

**Process flow — Debit bill segments:**

| Amount Threshold | Approval Level | Human Task Code |
|---|---|---|
| Up to 100 | No approval required (auto-approved) | — |
| 100 – 10,000 | Level 1 approval | `DBTDL1APPR` |
| 10,000 – 30,000 | Level 2 approval | `DBTDL2APPR` |
| Over 30,000 | Level 3 approval | `DBTDL3APPR` |

**Process flow — Credit bill segments:**

| Amount Threshold | Approval Level | Human Task Code |
|---|---|---|
| Up to -10,000 | Level 1 approval | `DBTLV1CANC` (credit variant) |
| Over -30,000 | Level 2 approval | `DBTDL2APPR` (or equivalent) |

**Key integrations:**
- `CreateHumanTaskCommand` → human-task-management (approval tasks)
- `FreezeBillSegmentCommand` → taxpayer-accounting-custom (freeze during approval)

**DWP relevance:** Debt write-off approval, penalty creation approval, credit adjustment approval.

---

### 13.3 `BILL_SEGMENT_CANCELLATION.bpmn`
**PDF page:** 167

**Purpose:** Single-level approval workflow for bill segment cancellation.

**Trigger:** A request to cancel an existing bill segment.

**Process flow:**
1. Cancellation request received
2. Human task created for approval:
   - Debit bill segment: `DBTLV1CANC`
   - Credit bill segment: `CRDLV1CANC`
3. Approver reviews and approves or rejects
4. If approved: bill segment is cancelled
5. If rejected: cancellation request is closed without action

**DWP relevance:** Reversal of erroneous debt charges; cancellation of penalties.

---

### 13.4 `BUS_REG.bpmn`
**PDF page:** 168

**Purpose:** Business registration workflow for new business entities.

**Trigger:** New business registration submitted (via self-service or caseworker).

**Process flow:**
1. Registration data received
2. **Drools validation** — form rules executed via `ExecuteKieRulesCommand`
3. **Duplicate check** — verifies no existing registration for the same entity
4. **Risk screening** — `RiskRequestCommand` sent for entity risk assessment
5. Routing based on results:
   - If exception: human task `RevForExc1` (Review for Exception)
   - If additional info needed: human task `ReqAddInf1` (Request Additional Information)
   - If risk flagged: human task `RevForRis1` (Review for Risk)
6. On successful completion: business registration confirmed

**Key integrations:**
- `ExecuteKieRulesCommand` → Drools KIE
- `RiskRequestCommand` → risk-analysis (signal: `RISK_RESULT_SIGNAL`)
- `CreateHumanTaskCommand` → human-task-management
- `CreateExceptionCommand` → exception-management

---

### 13.5 `CASE_DEMAND_LETTER.bpmn`
**PDF page:** 169

**Purpose:** Generates a demand letter for an unpaid debt case.

**Trigger:** Called as a sub-process (Call Activity) from `DEBT_RECOVERY_PROCESS.bpmn`.

**Process flow:**
1. Creates a **TAX_FTP bill segment** (fee for unpaid debt) on the case
2. Sends a **contact letter** to the debtor (demand letter)
3. Signals back to the parent process (`DEBT_RECOVERY_PROCESS`) that the demand letter step is complete

**Key integrations:**
- `CreateBillSegmentCommand` → taxpayer-accounting-custom (creates TAX_FTP fee bill segment)
- `CreateContactCommand` → contact-management (sends demand letter)

**DWP relevance:** Core debt recovery notification — first formal letter before enforcement.

---

### 13.6 `CASE_HANDOVER_COLLECTION_AGENCY_PROCESS.bpmn`
**PDF page:** 169

**Purpose:** Hands over a debt case to an external collection agency.

**Trigger:** Batch task `handover-debt` — identifies cases that meet handover criteria.

**Process flow:**
1. **Remaining debt check** — verifies there is still outstanding debt worth handing over
2. **Mark as handed over** — updates the revenue type registration to `HANDED_OVER` status
3. **Inform Collection Agency** — sends notification to the collection agency asynchronously (e.g., Swedish Enforcement Authority)
4. Process completes

**Key integrations:**
- REST or Kafka call to external collection agency system (asynchronous)
- Registration update via internal API

**DWP relevance:** Direct analogue to passing a debt to a debt collection agency or enforcement body (e.g., HMRC Debt Management, County Court, bailiffs).

---

### 13.7 `CASE_WARRANT_LETTER.bpmn`
**PDF page:** 170

**Purpose:** Issues a warrant letter as an escalated enforcement action.

**Trigger:** Called as a sub-process (Call Activity) from `DEBT_RECOVERY_PROCESS.bpmn` — after the demand letter step and any applicable suspension check.

**Fee calculation formula:**
```
Fee = min(max(total_balance / 8, 40), 12000)
```
i.e., fee is 1/8th of total balance, with a floor of 40 and a ceiling of 12,000 (currency units).

**Process flow:**
1. Calculate warrant fee using the formula above
2. Create a **TAX_FTP bill segment** for the warrant fee
3. Send **warrant contact letter** to the debtor
4. Signal back to the parent `DEBT_RECOVERY_PROCESS`

**Key integrations:**
- `CreateBillSegmentCommand` → taxpayer-accounting-custom
- `CreateContactCommand` → contact-management

**DWP relevance:** Enforcement escalation — formal warrant/court action letter prior to enforcement.

---

### 13.8 `CIT_RETURN.bpmn`
**PDF page:** 171

**Purpose:** Corporate Income Tax return submission and processing.

**Trigger:** Corporate income tax return submitted.

**Process flow:**
1. **Drools validation** — form rules validated
2. **Gross profit calculation** — computed as part of form processing
3. **Posting rules** — journal entries created based on Drools posting rules
4. If posting fails: human task `RevPostErr` (Review Posting Error) created
5. On success: return confirmed and financial transactions created

**Key integrations:**
- `ExecuteKieRulesCommand` → Drools KIE (validation and posting rules)
- `CreateHumanTaskCommand` → human-task-management (for errors)

---

### 13.9 `DEBT_RECOVERY_PROCESS.bpmn`
**PDF page:** 172

**Purpose:** The main debt recovery orchestration process. Coordinates the full debt enforcement lifecycle for a case.

**Trigger:** Batch task `debt-recovery-task` (Spring Batch) — identifies cases meeting debt recovery criteria.

**Process flow (orchestration sequence):**
1. **Start** — case identified as requiring debt recovery action
2. **`CASE_DEMAND_LETTER`** (Call Activity) — issues demand letter and TAX_FTP fee
3. **`SUSPEND_PROCESS_CHECK`** (Call Activity) — checks for active suppressions; waits if suppression exists
4. **`CASE_WARRANT_LETTER`** (Call Activity) — issues warrant letter and warrant fee
5. **Completion** — case debt recovery process concludes; case status updated

**Key design notes:**
- This process is the orchestrator for debt recovery; the actual work is delegated to child processes via Call Activities
- Each Call Activity returns a signal when complete, allowing the parent to proceed
- Suppressions (e.g., active objection, payment deferral) pause the process at the `SUSPEND_PROCESS_CHECK` step

**DWP relevance:** The central debt recovery workflow — analogous to DWP's structured debt recovery escalation process.

---

### 13.10 `DEMO_PROCESS_TASK.bpmn`
**PDF page:** 172

**Purpose:** Reference/demonstration process showing suppression lifecycle.

**Usage:** Used to demonstrate and test the `CreateSuppressionCommand` → `ReleaseSuppressionCommand` cycle. Not a production process.

**Key features demonstrated:**
- Suppression check
- Suppression activation
- Suppression release

---

### 13.11 `DIRECT_DEPOSIT.bpmn`
**PDF page:** 172

**Purpose:** Processes outgoing direct deposit payments (e.g., tax refunds via bank transfer).

**Trigger:** Triggered by `PAYMENT_FILE_BATCH_JOB` or as a sub-process within `VAT_OVERPAYMENT_PROCESS.bpmn`.

**Process flow:**
1. Processes PAIN.002 response file received via Kafka
2. Bill segment direct deposit status: `READY_TO_SEND`
3. Sends payment file to bank
4. Receives PAIN.002 response (payment status report):
   - Success → status: `COMPLETED_OK`
   - Failure → human task `DirDepErr` created → status: `COMPLETED_NOT_OK` (after resolution)
5. Sends status update via `UpdateBillSegmentDirectDepositStatusCommand`

**Key integrations:**
- Kafka: PAIN.002 file processing
- `UpdateBillSegmentDirectDepositStatusCommand` → taxpayer-accounting
- `CreateHumanTaskCommand` → human-task-management (for `DirDepErr` on failure)

**DWP relevance:** Direct payment/refund to debtor's bank account; payment confirmation lifecycle.

---

### 13.12 `HUMAN_TASK_BEHAVIOUR_TEST`
**PDF page:** 173

**Purpose:** Test/reference process demonstrating human task creation with auto-start and auto-assign behaviour.

**Usage:** Not a production process; used for testing human task configuration options.

---

### 13.13 `INCOME_TAX.bpmn`
**PDF page:** 175

**Purpose:** Income tax return processing (simplified).

**Process flow:**
1. Process starts
2. Waits for a `SUBMITTED_SIGNAL` (signal intermediate catch event)
3. When signal received: process exits

**Note:** This is a minimal/example implementation. Full income tax processing would involve Drools validation and posting, similar to `CIT_RETURN.bpmn`.

---

### 13.14 `MUNICIPAL_TAX_EXAMPLE.bpmn`
**PDF page:** 175

**Purpose:** Example process for periodic billing of municipal tax obligations.

**Trigger:** Batch task `periodic-billing-municipal-tax-example-task`.

**Process flow:**
1. **Create assessment** — new bill segment (assessment) created for the period
2. **Cancel assessment** — (conditional) existing assessment cancelled if one exists
3. **Recreate assessment** — new assessment created with corrected values
4. **Send instalment letter** — contact letter sent with instalment schedule

**DWP relevance:** Periodic billing example; demonstrates assessment creation/cancellation/recreation lifecycle that applies to debt charging cycles.

---

### 13.15 `NOTIFY_GDPR_EXPIRATION.bpmn`
**PDF page:** 176

**Purpose:** Notifies party roles whose GDPR consent is approaching expiry.

**Trigger:** Batch task `compliance-monitor-GDPR-expiration-task`.

**Process flow:**
1. Query identifies party roles with GDPR consent expiring in the **next 3 months**
2. For each identified party role:
   - A letter is sent via `CreateContactCommand`
3. Process completes

**DWP relevance:** GDPR compliance — data subject notification before consent expiry.

---

### 13.16 `NOTIFY_OVERDUE_DEBTORS.bpmn`
**PDF page:** 176

**Purpose:** Sends reminder letters to debtors with overdue obligation actions.

**Trigger:** Batch task `compliance-monitor-notify-overdue-debtors-task`.

**Process flow:**
1. Query identifies party roles with overdue obligation actions
2. For each identified party role:
   - A letter is sent via `CreateContactCommand`
3. Process completes

**DWP relevance:** Automated overdue payment reminder — first-level debt notification.

---

### 13.17 `O_PLN_PRVL.bpmn`
**PDF page:** 176

**Purpose:** Operational plan approval workflow.

**Trigger:** An operational plan is submitted for approval.

**Process flow:**
1. Human task `OPPLNAPPTK` created for the operational plan approver
2. Approver reviews:
   - **Approve** → plan status transitions to `IN_PROGRESS`
   - **Reject** → plan status transitions to `PENDING` (back for revision)
3. `UpdateOperationalPlanStatusCommand` sent to update the operational plan service

**Key integrations:**
- `CreateHumanTaskCommand` → human-task-management (`OPPLNAPPTK`)
- `UpdateOperationalPlanStatusCommand` → operational plan service

---

### 13.18 `OBJECTION_PROCESS.bpmn`
**PDF page:** 177

**Purpose:** Manages the formal objection (dispute) lifecycle for a tax case.

**Trigger:** A "Debtor Objection" case is set to `IN_PROGRESS` status.

**Process flow:**
1. **Timeliness check** — `ObjectionCommand` sent via `irm.bpmn-engine.verify-timeliness` to verify the objection was filed within the allowed period (30 days)
2. **Suppression created** — `CreateSuppressionCommand` to pause debt recovery during objection
3. **Case activity suspended** — `SuspendCaseActivityCommand`
4. Human task sequence (multiple steps):
   - `VerCoplObj` — Verify compliance of objection
   - `HearRequir` — Determine hearing requirements
   - `PerfoHear` — Perform hearing (if required)
   - `ObjDecsion` — Objection decision
   - `ImplObjDec` — Implement objection decision
   - `EdtSndLtr` — Edit and send letter (outcome letter to debtor)
5. **Suppression released** — `ReleaseSuppressionCommand`
6. **Case activity resumed** — `ResumeCaseActivityCommand`
7. Multiple letters sent at relevant stages via `CreateContactCommand`
8. Process completes

**Key integrations:**
- `ObjectionCommand` → `irm.bpmn-engine.verify-timeliness` → case-management-custom
- `CreateSuppressionCommand` / `ReleaseSuppressionCommand` → suppression-management
- `SuspendCaseActivityCommand` / `ResumeCaseActivityCommand` → case-management-custom
- `CreateHumanTaskCommand` → human-task-management (6 distinct task types)
- `CreateContactCommand` → contact-management (multiple letters)

**DWP relevance:** Full dispute/mandatory reconsideration workflow — directly analogous to DWP's statutory appeal/reconsideration process.

---

### 13.19 `OSS_REG.bpmn`
**PDF page:** 178

**Purpose:** One Stop Shop (OSS) VAT registration processing.

**Trigger:** OSS registration submitted.

**Process flow:**
1. **Drools validation** — registration data validated
2. **Posting** — Drools posting rules create necessary financial entries
3. **OSS revenue type registration created** — party registered for OSS VAT scheme
4. Process completes

**Key integrations:**
- `ExecuteKieRulesCommand` → Drools KIE (validation and posting)

---

### 13.20 `OSS_VAT.bpmn`
**PDF page:** 179

**Purpose:** OSS VAT return submission and processing.

**Trigger:** OSS VAT return submitted.

**Process flow:**
1. **Drools validation** — return data validated
2. **Posting** — journal entries created
3. **Bill segment created** — via `CreateBillSegmentCommand`
4. **Bill created** — from the bill segment
5. Process completes

**Key integrations:**
- `ExecuteKieRulesCommand` → Drools KIE
- `CreateBillSegmentCommand` → taxpayer-accounting-custom

---

### 13.21 `P_ROLE_LIN.bpmn`
**PDF page:** 179

**Purpose:** Party role linkage — links a self-service user to a party role in the system.

**Trigger:** Self-service user requests linkage to a party (taxpayer entity).

**Process flow:**
1. **Drools validation** — linkage request validated
2. Linkage created in registration management
3. **Push notification** sent to relevant parties (via `PushNotificationCommand`)
4. Process completes

**Key integrations:**
- `ExecuteKieRulesCommand` → Drools KIE
- `PushNotificationCommand` → Notification service

---

### 13.22 `PAYMENT_DEFERRAL.bpmn`
**PDF page:** 180

**Purpose:** Processes a taxpayer's request to defer payment of an outstanding debt.

**Trigger:** Payment deferral form submitted (via self-service or caseworker).

**Process flow:**
1. **Drools validation** — deferral eligibility rules evaluated
2. If validation fails: human task `RevForExc1` (Review for Exception)
3. If validation passes: deferral decision:
   - **Approved** → approval letter sent; deferral applied to obligation
   - **Rejected** → rejection letter sent
4. Letters sent via `CreateContactCommand`
5. Process completes

**Key integrations:**
- `ExecuteKieRulesCommand` → Drools KIE (eligibility rules)
- `CreateHumanTaskCommand` → human-task-management (`RevForExc1`)
- `CreateContactCommand` → contact-management (approval/rejection letters)

**DWP relevance:** Payment arrangement / deferral request — directly relevant to debt management workflows.

---

### 13.23 `SCHEDULED_OPERATIONAL_REPORT.bpmn`
**PDF page:** 181

**Purpose:** Generates and distributes operational reports on a schedule.

**Trigger:** Quartz scheduler (cron-based trigger).

**Process flow:**
1. Quartz trigger fires according to the configured schedule
2. **Jasper report generated** — report template rendered with current data
3. **Report stored in DMS** — stored in the Document Management System
4. **Notification sent** — via push notification or email
5. **Contact letter sent** — formal report distribution letter via `CreateContactCommand`
6. Process completes

**Key integrations:**
- Jasper Reports engine (embedded call)
- DMS API (document storage)
- `PushNotificationCommand` → Notification service
- `CreateContactCommand` → contact-management

---

### 13.24 `SUSPEND_PROCESS_CHECK.bpmn`
**PDF page:** 181

**Purpose:** Checks for active suppressions on a liability and suspends process execution while a suppression is active.

**Trigger:** Called as a sub-process (Call Activity) from `DEBT_RECOVERY_PROCESS.bpmn`.

**Process flow:**
1. Query: check for active suppressions on the liability with action type `DEBT_REC_C`
2. If suppression found:
   - `SuspendCaseActivityCommand` → case-management-custom
   - **Loop** — process waits (timer-based check) and re-evaluates
3. Once no active suppression:
   - `ResumeCaseActivityCommand` → case-management-custom
   - Signal back to parent process to continue
4. Process completes

**Key integrations:**
- `SuspendCaseActivityCommand` → case-management-custom
- `ResumeCaseActivityCommand` → case-management-custom

**Design note:** This implements the "wait for suppression to be lifted" pattern. Combined with `OBJECTION_PROCESS.bpmn` (which creates and releases the suppression), it ensures debt recovery is paused for the full duration of a formal objection.

---

### 13.25 `TAX_RETURN_LATE_FILLER_CASE_ACTIVITY_PROCESS.bpmn`
**PDF page:** 182

**Purpose:** Handles enforcement actions against taxpayers who have not filed a required tax return by the due date.

**Trigger:** Batch task `late-filer-rule`.

**Process flow:**
1. **Warning letter** sent to the late filer via `CreateContactCommand`
2. **Wait** — process waits a configurable period for the return to be filed
3. **Overdue check** — after the wait period, checks if the return has now been filed
4. If still overdue:
   - **Final warning letter** sent
   - **Penalty bill segment created** — via `CreateBillSegmentCommand`
5. Process completes

**Key integrations:**
- `CreateContactCommand` → contact-management (warning and final warning letters)
- `CreateBillSegmentCommand` → taxpayer-accounting-custom (penalty)
- Timer intermediate catch event (configurable wait duration)

**DWP relevance:** Late filing penalty workflow — applies to mandatory reporting obligations.

---

### 13.26 `VAT_EU.bpmn`
**PDF page:** 182

**Purpose:** EU VAT sales list submission processing.

**Trigger:** EU VAT sales list submitted.

**Process flow:**
1. **Drools validation** — sales list data validated
2. **Posting** — journal entries created via Drools posting rules
3. If any step fails: human task created for error review
4. Process completes

**Key integrations:**
- `ExecuteKieRulesCommand` → Drools KIE

---

### 13.27 `VAT_OVERPAYMENT_PROCESS.bpmn`
**PDF page:** 183

**Purpose:** Processes VAT overpayments — orchestrates offsetting, risk assessment, and refund or write-off.

**Trigger:** Batch task `overpayment-process-task`.

**Process flow:**

1. **Offsetting phase** — overpayment is first applied to any outstanding debts (offsetting)
2. **Remaining amount check** — if residual overpayment exists after offsetting:
3. **Risk check (IBAN)** — checks if the taxpayer's IBAN has changed more than **5 times in the last month**
   - If yes: risk flag raised; human review may be required
4. **Refund path** (if risk check passes):
   - `BILL_SEGMENT_APPROVAL.bpmn` called (Call Activity) — refund bill segment approval
   - Two `DIRECT_DEPOSIT.bpmn` processes called in parallel:
     - First direct deposit attempt
     - Second direct deposit attempt (as backup or split)
5. **Write-off path** (if refund not possible):
   - Write-off bill segment created
6. **Letters at completion** — appropriate letters sent based on outcome (refund confirmation or write-off notification)
7. `OverpaymentProcessStoreDataAreaCommand` sent to store process outcome data
8. Process completes

**Key integrations:**
- `RiskRequestCommand` → risk-analysis (IBAN change check)
- `CreateBillSegmentCommand` → taxpayer-accounting-custom (refund/write-off bill segments)
- Call Activity → `BILL_SEGMENT_APPROVAL.bpmn`
- Call Activity × 2 → `DIRECT_DEPOSIT.bpmn`
- `OverpaymentProcessStoreDataAreaCommand` → taxpayer-accounting-custom
- `CreateContactCommand` → contact-management (outcome letters)

**DWP relevance:** Benefit overpayment recovery and write-off workflow — directly analogous to DWP overpayment processes.

---

### 13.28 `VAT_RETURN.bpmn`
**PDF page:** 185

**Purpose:** VAT return submission and processing — the most complex standard return process.

**Trigger:** VAT return submitted.

**Process flow:**

1. **Drools validation** — VAT return form data validated against configurable rules
2. **Risk screening** — `RiskRequestCommand` sent to risk-analysis microservice
   - Risk threshold: **40** (if risk score > 40, route to human review)
   - Signal received: `RISK_RESULT_SIGNAL`
3. **Routing based on outcomes:**
   - Risk flagged (score > 40) → human task `RevForRis1` (Review for Risk)
   - Exception detected → human task `RevForExc1` (Review for Exception)
   - Additional info required → human task `ReqAddInf1` (Request Additional Information)
   - Posting error → human task `RevPostErr` (Review Posting Error)
   - Reversion error → human task `RevRevErr` (Review Reversion Error)
4. **Posting** — on successful validation and risk clearance: Drools posting rules create journal entries and bill segments
5. Process completes

**Key integrations:**
- `ExecuteKieRulesCommand` → Drools KIE (validation and posting rules)
- `RiskRequestCommand` → risk-analysis (signal: `RISK_RESULT_SIGNAL`)
- `CreateHumanTaskCommand` → human-task-management (5 distinct task types)
- `CreateExceptionCommand` → exception-management

---

## 14. Appendix: Human Task Type Codes

Full catalogue of human task type codes used across all BPMN processes:

| Code | Process(es) | Description |
|---|---|---|
| `DBTDL1APPR` | `BILL_SEGMENT_APPROVAL.bpmn` | Bill segment debit Level-1 approval (100–10,000) |
| `DBTDL2APPR` | `BILL_SEGMENT_APPROVAL.bpmn` | Bill segment debit Level-2 approval (10,000–30,000) |
| `DBTDL3APPR` | `BILL_SEGMENT_APPROVAL.bpmn` | Bill segment debit Level-3 approval (>30,000) |
| `DBTLV1CANC` | `BILL_SEGMENT_CANCELLATION.bpmn` | Bill segment debit Level-1 cancellation approval |
| `CRDLV1CANC` | `BILL_SEGMENT_CANCELLATION.bpmn` | Bill segment credit Level-1 cancellation approval |
| `RevForExc1` | `BUS_REG.bpmn`, `PAYMENT_DEFERRAL.bpmn`, `VAT_RETURN.bpmn` | Review for exception (Level 1) |
| `ReqAddInf1` | `BUS_REG.bpmn`, `VAT_RETURN.bpmn` | Request additional information |
| `RevForRis1` | `BUS_REG.bpmn`, `VAT_RETURN.bpmn` | Review for risk |
| `RevPostErr` | `CIT_RETURN.bpmn`, `VAT_RETURN.bpmn` | Review posting error |
| `RevRevErr` | `VAT_RETURN.bpmn` | Review reversion error |
| `DirDepErr` | `DIRECT_DEPOSIT.bpmn` | Direct deposit error review |
| `VerCoplObj` | `OBJECTION_PROCESS.bpmn` | Verify compliance of objection |
| `HearRequir` | `OBJECTION_PROCESS.bpmn` | Determine hearing requirements |
| `PerfoHear` | `OBJECTION_PROCESS.bpmn` | Perform hearing |
| `ObjDecsion` | `OBJECTION_PROCESS.bpmn` | Record objection decision |
| `ImplObjDec` | `OBJECTION_PROCESS.bpmn` | Implement objection decision |
| `EdtSndLtr` | `OBJECTION_PROCESS.bpmn` | Edit and send outcome letter |
| `OPPLNAPPTK` | `O_PLN_PRVL.bpmn` | Operational plan approval task |

---

## 15. Appendix: Event and Signal Reference

### 15.1 Signals Used in BPMN Processes

| Signal Name | Produced By | Consumed By | Description |
|---|---|---|---|
| `RISK_RESULT_SIGNAL` | risk-analysis service | Process engine (Signal Intermediate Catch) | Risk screening result returned |
| `SUBMITTED_SIGNAL` | External (form submission) | `INCOME_TAX.bpmn` | Signals that an income tax return has been submitted |
| Sub-process completion signals | Child process (via Call Activity) | Parent process (e.g., `DEBT_RECOVERY_PROCESS`) | Child processes signal completion to parent |

### 15.2 Event Types and Eventplans

Event types are the primary trigger mechanism for BPMN processes. Each event type is mapped to a process type via the **Eventplans** system parameter. Key event types include:

| Domain | Event Type Category | Typical Process Triggered |
|---|---|---|
| Taxpayer Accounting | Bill segment created | `BILL_SEGMENT_APPROVAL.bpmn` |
| Taxpayer Accounting | Overpayment identified | `VAT_OVERPAYMENT_PROCESS.bpmn` |
| Registration | Business registration submitted | `BUS_REG.bpmn` |
| Registration | OSS registration submitted | `OSS_REG.bpmn` |
| Tax Returns | VAT return submitted | `VAT_RETURN.bpmn` |
| Tax Returns | OSS VAT return submitted | `OSS_VAT.bpmn` |
| Tax Returns | Corporate income tax return submitted | `CIT_RETURN.bpmn` |
| Case Management | Case set to IN_PROGRESS (Debtor Objection type) | `OBJECTION_PROCESS.bpmn` |
| Self Service | Agreement/delegation requested | `AGREEMENT.bpmn` |
| Self Service | Party role link requested | `P_ROLE_LIN.bpmn` |

### 15.3 Business Components / Task Delegate Catalogue

The full catalogue of available BPMN task delegates (business components / executors) is maintained on the Netcompany Confluence:

```
https://confluence.intrasoft-intl.com/spaces/IC/pages/36412664/Business+Components
```

These delegates are Java classes implementing the Amplio task executor interface and are available as service task implementations within BPMN processes. The integration guide references this catalogue but does not reproduce it inline.

---

*End of document — SOLON TAX Integration Guide v2.3.0*

---

## 16. Business Components — Confirmed Capability Index

> **Source:** Confluence page IC/pages/36412664, provided 2026-04-16.
> This is the official index of all business components included in SOLON TAX.

### Core Revenue Management Operations

- Registration: Taxpayers, Assets, Registration to Taxes
- Accounting, Cashiering
- Payments management
- Taxation, Interest and Fees, Waivers
- Cases management: Audits, Disputes, Collections, Enforcements, Claims, Suppressions
- Business processes (BPMN)
- Billing
- Input forms
- Work management, Operational Plans
- Overpayment management
- Contact Management

### Platform-Wide Capabilities

- Overview pages and Operational reports
- Configuration and Administration Pages
- Custom UI schemas
- Multicurrency / Primary currency modes
- Security and Permissions: authentication, user roles, securable objects, row level security
- Batch Jobs / ETLs (internal processes and integration exports)
- Archiving
- Message store
- Customization: adapters, custom microservices, flows, reference data, Drools rules, Groovy scripts, OPA, user defined characteristics, plugin spots, custom codelist values, user preferences
- Localization: multilanguage support, localised currencies and record formats, timezone support, custom business rules and validations
- Solon Self Service portal

### Taxpayer Accounting (TXPACC) — Confirmed Sub-Capabilities

| Capability | Component |
|---|---|
| Bill Segments and Bills Management | Bill Types → Bill Segment Types relations; Bills Management; Bill Segment Outstanding Balance |
| Financial Transactions and Journal Entries | Creation via segment status transition; Effective Date / Accounting Date management |
| Waivers / Write-offs | TXPACC - Waivers/Write-offs |
| Amendments | TXPACC - Amendments |
| Bill Segment Approvals (multi-level) | TXPACC - Bill segment approvals |
| Credit Allocation | Allocation Links; Allocation Segment; Credit Allocation Rules |
| Penalty & Interest Calculation | Full calculation detail including Statute of Limitation |
| Liabilities | TXPACC - Liabilities; Statute of Limitation |
| Cashiering | TXPACC - Cashiering |
| Pay Plans | TXPACC - Pay Plans |
| Transfer Amount | TXPACC - Transfer Amount |
| Assessment Calculations | Amend Assessments; Assessment Calculation Completion Logic; Periodic Billing — Assessments |
| Overpayment Management | Overpayment Process; Direct Deposit; Refunding; ISO 20022 / PAIN support |
| Payment Distribution / TAXPACC | Banks and Bank Accounts in Tender and Deposit Sources; Managing Payments in Office (Cashiering) |
| Payment Hints | Payment Hints |
| Single Taxpayer View | Unified debtor view across all obligations |

### Case Management — Confirmed Sub-Capabilities

| Capability | Component |
|---|---|
| Case activity | Case Activities; Start button for Process and Case |
| Case Scope | Case Scope configuration |
| Entry / Exit Criteria | Configurable criteria for case lifecycle |
| Related Entities | Entity relationships on a case |
| Candidate Selection | Support for candidate selection lists |
| Forms | Business Registration, EU VAT, OSS VAT, input form setup |
| Exceptions | Exceptions from forms; from bill segments; from journal entry failures |

### Suppression Management — Confirmed Sub-Capabilities

- Create, Activate, Release suppression by a process
- Suppression of Cases
- Suppression of Penalty & Interest
- Suppression of Processes

### Work Management — Confirmed Sub-Capabilities

- Operational Plans
- Human Tasks (admin configuration, user tasks, worker profiles)
- Queries in IRM (custom worklist queries)
- Note Management
- Operational Reporting

### Authorization and Security — Confirmed Sub-Capabilities

- Authorization on IRM
- Securable Objects
- Row Level Access Management
- LDAP-AD Integration
- Keycloak integration
- Self Service Security

### Contact Management — Confirmed Sub-Capabilities

- Contacts (letters, email)
- Electronic Signatures in Contact Management
- Notifications
- Inbound and Outbound Messages
- Reconciliation of Outgoing Payments with Pain.001 messages

### Self Service Portal — Confirmed Sub-Capabilities

- Agreements
- Queries in Self Service
- Self Service Security
- Services mechanism
- Sub-Users
- Support drafts of Forms in Self Service
- Tasks
- User management (portal user creation; link to party role)
