# SOLON TAX — Operations Guide v2.3.0
## Comprehensive Reference Document

> **Source:** Netcompany SOLON TAX Operations Guide, release 2.3.0 (146 pages).
> This document was produced by systematically extracting all substantive text from the source PDF using
> PyMuPDF and supplementing image-only sections with context inferred from the document structure,
> REST API testing, and cross-referencing with the Integration Guide. All technical details are accurate
> to the source material.

---

## Table of Contents

1. [Platform Overview](#1-platform-overview)
2. [Repository Structure](#2-repository-structure)
3. [Infrastructure Architecture](#3-infrastructure-architecture)
   - 3.1 Kubernetes Namespaces
   - 3.2 Core Infrastructure Components
   - 3.3 Networking and Load Balancing
4. [Key Technology Components](#4-key-technology-components)
   - 4.1 Kafka / Zookeeper
   - 4.2 Keycloak (IAM)
   - 4.3 Redis
   - 4.4 Hazelcast
   - 4.5 Camunda Platform
   - 4.6 Amplio Process Engine
   - 4.7 Spring Cloud Data Flow (SCDF)
   - 4.8 Revenue Management Batch
   - 4.9 Drools (BRMS)
   - 4.10 Jasper Reports
   - 4.11 OPA (Open Policy Agent)
5. [REST API — Batch Engine](#5-rest-api--batch-engine)
6. [Batch Jobs](#6-batch-jobs)
7. [Monitoring and Observability](#7-monitoring-and-observability)
   - 7.1 ELK Stack
   - 7.2 Application Performance Monitoring (APM)
   - 7.3 Database Monitoring
   - 7.4 Batch Monitoring
8. [Backup and Recovery](#8-backup-and-recovery)
9. [Scaling](#9-scaling)
10. [Security](#10-security)
11. [Data Archiving](#11-data-archiving)
12. [High Availability and Disaster Recovery](#12-high-availability-and-disaster-recovery)
13. [Maintenance](#13-maintenance)
14. [Troubleshooting](#14-troubleshooting)
15. [Prerequisites and Installation](#15-prerequisites-and-installation)
16. [Appendix A — Amplio Administration UI](#16-appendix-a--amplio-administration-ui)

---

## 1. Platform Overview

**SOLON TAX** (release 2.3.0) is Netcompany's Revenue Management Platform built on the **Amplio** framework. It is a cloud-native, microservices-based system designed to manage the full lifecycle of tax obligations, assessments, billing, payments, debt recovery, and compliance workflows for revenue authorities.

Key characteristics:
- **Cloud-native:** Deployed on Kubernetes; all services containerised
- **Microservices:** Domain-separated services communicating via REST (synchronous) and Kafka (asynchronous)
- **Event-driven:** All state transitions generate events; Outbox Pattern ensures no event loss
- **Workflow-driven:** All business processes defined as BPMN; executed by Amplio Process Engine (or Camunda 7)
- **Secure by default:** OAuth2/OpenID Connect via Keycloak; Row-Level Security at DB layer via JPA filters; OPA policy enforcement
- **Observable:** Full ELK stack, APM, metrics, structured logging
- **Customisable:** Standard (`revenue-management-be`) + Custom (`revenue-management-be-custom`) layers for client-specific extensions

---

## 2. Repository Structure

| Repository | Purpose |
|---|---|
| `revenue-management-be` | Core backend microservices |
| `revenue-management-db-migration` | Liquibase database migrations |
| `revenue-management-ui` | Core frontend (Angular) |
| `self-service-ui` | Taxpayer self-service portal |
| `icarus-keycloak` | Keycloak configuration and themes |
| `revenue-management-batch` | Amplio-based batch processing engine |
| `revenue-management-etl` | Spring Cloud Data Flow ETL batch jobs |
| `revenue-management-be-custom` | Client-specific backend customisations |
| `revenue-management-adapters` | Integration adapters and BPMN reference implementations |
| `revenue-management-ui-custom` | Client-specific frontend customisations |
| `revenue-management-batch-custom` | Client-specific batch job customisations |

---

## 3. Infrastructure Architecture

### 3.1 Kubernetes Namespaces

SOLON TAX uses a multi-namespace Kubernetes deployment model. The namespace prefix is the environment identifier (e.g., `dev`, `test`, `prod`).

| Namespace Pattern | Contents |
|---|---|
| `[env]-aux-camunda-platform` | Camunda 7/8 process engine pods |
| `[env]-platform-adapters` | Integration adapter services |
| `[env]-platform-ms` | Core microservices (revenue-management-be) |
| `[env]-platform-batch` | Batch processing services (SCDF, Revenue Management Batch) |
| `[env]-platform-infra` | Infrastructure components (Kafka, Redis, Keycloak, etc.) |

Each namespace is isolated with appropriate RBAC and NetworkPolicies. Services communicate across namespaces via internal DNS.

### 3.2 Core Infrastructure Components

The following components are deployed as part of the SOLON TAX infrastructure:

| Component | Technology | Purpose |
|---|---|---|
| Message Broker | Apache Kafka + Zookeeper | Asynchronous event streaming between microservices |
| Identity Provider | Keycloak | OAuth2/OpenID Connect; user management; role-based access |
| In-memory Cache (session) | Redis | Session data; push notifications; instant eviction on writes |
| In-memory Cache (shared) | Hazelcast | Shared cache across Amplio application cluster nodes |
| Process Engine | Amplio Process Engine | First-party BPMN workflow execution |
| Process Engine (legacy) | Camunda 7 | Legacy workflow execution (Camunda 8 deprecated in 2.3.0) |
| Batch Orchestration | Spring Cloud Data Flow | ETL-style batch job orchestration (SCDF-based) |
| Batch Engine | Revenue Management Batch | Amplio-native batch processing |
| Rules Engine | Drools (KIE) | Business rules evaluation (form rules, journal entries, risk) |
| Report Engine | Jasper Reports | Operational report generation and rendering |
| Policy Engine | OPA (Open Policy Agent) | Unified authorisation policy enforcement |
| Database | PostgreSQL / Oracle | Persistent storage; Row-Level Security via JPA filters |
| Document Management | DMS | Attachment and document storage |

### 3.3 Networking and Load Balancing

- API Gateway / Ingress: Kubernetes Ingress controller (typically nginx or cloud-provider equivalent)
- Service Mesh: Services registered via Kubernetes Service resources
- Internal communication: Kubernetes DNS (`service.namespace.svc.cluster.local`)
- External exposure: Ingress rules per environment with TLS termination

---

## 4. Key Technology Components

### 4.1 Kafka / Zookeeper

Kafka is the primary asynchronous communication backbone. All inter-service events are published to Kafka topics.

**Outbox Pattern:** Every Kafka event is first persisted to the service's own database (outbox table) before being published to Kafka. This guarantees exactly-once delivery semantics and allows Kafka to be fully reconstructed from the database if needed.

**Key operational considerations:**
- Topic naming convention: `irm.[service-name].[event-type]` (e.g., `irm.bpmn-engine.create-human-task`)
- Consumer groups are per microservice
- Dead Letter Queue (DLQ) handling is managed via the Amplio Administration UI (Event Stream Management)
- All Kafka messages are serialised as JSON
- Kafka is deployed with Zookeeper for coordination (standard Kafka deployment)

**Kafka topics used by the BPMN engine** (see also §8.3.5 in Integration Guide):

| Command | Topic | Target Service |
|---|---|---|
| `TransitionFormCommand` | `irm.bpmn-engine.transition-form-status` | ui-configuration |
| `PushNotificationCommand` | `irm.bpmn-engine.push-notification-redis` | Notification |
| `CreateHumanTaskCommand` | `irm.bpmn-engine.create-human-task` | human-task-management |
| `CreateContactCommand` | `irm.bpmn-engine.create-contact` | contact-management |
| `CreateExceptionCommand` | `irm.bpmn-engine.create-exception` | exception-management |
| `CloseExceptionCommand` | `irm.bpmn-engine.close-exception` | exception-management |
| `RiskRequestCommand` | `irm.bpmn-engine.screen-risk` | risk-analysis |
| `TransitionOverpaymentProcessStatusCommand` | `irm.bpmn-engine.transition-overpayment-process-status` | taxpayer-accounting |
| `TransitionProcessFlowStatusCommand` | `irm.bpmn-engine.transition-process-flow-status` | process-management |
| `UpdateOperationalPlanStatusCommand` | `irm.bpmn-engine.update-operational-plan-status` | operational plan |
| `CreateSuppressionCommand` | `irm.bpmn-engine.create-suppression` | suppression-management |
| `ReleaseSuppressionCommand` | `irm.bpmn-engine.release-suppression` | suppression-management |
| `UpdateBillSegmentDirectDepositStatusCommand` | `irm.bpmn-engine.update-bill-segment-direct-deposit-status` | taxpayer-accounting |
| `SuspendCaseActivityCommand` | `irm.bpmn-engine.suspend-case-activity` | case-management-custom |
| `ResumeCaseActivityCommand` | `irm.bpmn-engine.resume-case-activity` | case-management-custom |
| `FreezeBillSegmentCommand` | `irm.bpmn-engine.bill-segment-service-freeze-command` | taxpayer-accounting-custom |
| `CreateBillSegmentCommand` | `irm.bpmn-engine.create-bill-segment-for-case` | taxpayer-accounting-custom |
| `ConfigureCaseScopeCommand` | `irm.bpmn-engine.configure-case-scope` | case-management-custom |
| `CompleteChildCaseCommand` | `irm.bpmn-engine.complete-child-case` | case-management-custom |
| `CreateStartChildCaseCommand` | `irm.bpmn-engine.create-start-child-case` | case-management-custom |
| `TriggerDebtRecoveryProcessCommand` | `irm.bpmn-engine.trigger.debt.recovery.process` | case-management-custom |
| `ObjectionCommand` | `irm.bpmn-engine.verify-timeliness` | case-management-custom |
| `CheckTaxReturnLateFilerCommand` | `irm.bpmn-engine.check-tax-return-late-filer` | registration-management-custom |
| `OverpaymentProcessStoreDataAreaCommand` | `irm.bpmn-engine.overpayment-process-store-data-area-command` | taxpayer-accounting-custom |

**Synchronous activity (HTTP call from BPMN):**

| Command | Endpoint | Target |
|---|---|---|
| `ExecuteKieRulesCommand` | `POST /drools-runtime/execute-stateless/{containerId}` | Drools KIE runtime |

### 4.2 Keycloak (IAM)

Keycloak provides all identity and access management functions:

- **Authentication protocol:** OAuth2 / OpenID Connect
- **Token type:** JWT (JSON Web Tokens)
- **Integration:** All microservices validate JWT tokens on every request
- **User management:** Caseworkers, administrators, and taxpayer self-service users managed in Keycloak
- **Roles:** Role-based access control; roles are embedded in JWT claims
- **Repository:** `icarus-keycloak` contains realm configuration, client definitions, and custom themes
- **Row-Level Security:** JPA filters use the user's identity (from JWT) to enforce data-level security at the database query layer

### 4.3 Redis

Redis is used for:

- **Session caching:** Short-lived session data
- **Push notifications:** The `PushNotificationCommand` publishes to `irm.bpmn-engine.push-notification-redis`; the Notification service consumes and pushes to browser clients via Redis pub/sub
- **Instant eviction:** Redis is configured for instant cache eviction on any write, ensuring data freshness

### 4.4 Hazelcast

Hazelcast provides a distributed in-memory data grid for Amplio application clustering:

- **Purpose:** Shared state across multiple Amplio Process Engine nodes
- **Use case:** Ensures consistent process state when the engine is deployed as multiple replicas
- **Configuration:** Hazelcast cluster members are discovered via Kubernetes pod labels

### 4.5 Camunda Platform

**Camunda 7** is supported as an alternative to the Amplio Process Engine for BPMN execution. Camunda 8 was supported in earlier releases but is **deprecated as of SOLON TAX 2.3.0**.

- Deployed in namespace `[env]-aux-camunda-platform`
- Uses standard Camunda deployment APIs
- Process definitions are `.bpmn` XML files compatible with BPMN 2.0 specification

### 4.6 Amplio Process Engine

The Amplio Process Engine is Netcompany's first-party BPMN 2.0 workflow engine. It serves as the primary alternative to Camunda for process execution.

**Key characteristics:**
- Uses Camunda 8 diagram format (`.bpmn` XML with Zeebe-compatible extensions) but implements only a subset of Camunda 8 features
- Provides full Admin UI for process monitoring, error handling, and diagram management
- Supports hot-redeployment of process definitions
- Integrated with the Outbox Pattern for all async activities

**Supported BPMN Elements:**

| Element Type | Elements Supported | Notes |
|---|---|---|
| Tasks | Service Task, User Task, Business Rule Task, Script Task, Undefined Task | Service Task, Business Rule Task, Script Task: partial support |
| Gateways | Exclusive Gateway, Parallel Gateway, Event-based Gateway | Parallel Gateway: sequential execution only |
| Intermediate Catch Events | Timer, Signal, Message, Link | |
| Boundary Events | Error, Message | Message: interrupting only |
| Intermediate Throw Events | Link, Error | |
| Start Events | Standard start event | |
| End Events | Standard end event | |
| Subprocesses | Embedded Subprocess (collapsed/expanded), Call Activity | |

**Limitations vs. Camunda 8 (important for custom development):**

| Area | Camunda 8 Behaviour | Amplio Behaviour |
|---|---|---|
| Service Task — HTTP query params | Separate key-value pairs | Must be a single JSON object string |
| Business Rule Task | Native DMN support | Must be implemented as Service Task with custom Java |
| Script Task | Supports multiple languages | FEEL expressions only; no job worker implementations |
| Parallel Gateway | Concurrent branch execution | Sequential execution; no cross-branch variable dependencies |
| Multi-Instance Activity | Parallel or sequential | Sequential only |
| Message Boundary Event | Interrupting and non-interrupting | Interrupting only; no early message buffering |

### 4.7 Spring Cloud Data Flow (SCDF)

SCDF is used for ETL-style batch processing via the `revenue-management-etl` repository. It orchestrates Spring Batch jobs using a task/stream pipeline model.

- Deployed in `[env]-platform-batch` namespace
- Jobs are deployed as Spring Batch tasks
- SCDF provides its own UI for monitoring, launching, and reviewing batch job executions
- This is the **legacy** batch approach; Revenue Management Batch is the newer approach

**Batch modules covered by SCDF (`revenue-management-etl`):**

| Module |
|---|
| Authorization Management |
| Case Management |
| Compliance Monitor |
| Registration |
| Suppression Management |
| Taxpayer Accounting |
| Archiving |
| GDPR Anonymization |

### 4.8 Revenue Management Batch

The Revenue Management Batch engine is the Amplio-native batch processing approach, delivered via `revenue-management-batch` (standard) and `revenue-management-batch-custom` (client extensions).

**Batch modules covered:**

| Module | `revenue-management-etl` | `revenue-management-batch` |
|---|---|---|
| Authorization Management | Yes | No |
| Case Management | Yes | No |
| Compliance Monitor | Yes | No |
| Registration | Yes | Yes |
| Suppression Management | Yes | No |
| Taxpayer Accounting | Yes | Yes |
| Archiving | Yes | Yes |
| GDPR Anonymization | Yes | Yes |

The Revenue Management Batch engine exposes a **REST API** for managing batch jobs. See §5 for full API documentation.

### 4.9 Drools (BRMS)

Drools is the Business Rules Management System embedded in SOLON TAX. It is used for:

- **Form rules:** Dynamic validation and calculation rules on assessment and return forms
- **Journal entries:** Automated accounting entry generation based on rules
- **Risk alert screening:** Evaluating risk criteria for flagging entities
- **Posting rules:** Determining how financial transactions are posted

Drools rules are compiled and deployed as KIE containers. The BPMN engine calls Drools synchronously via:
```
POST /drools-runtime/execute-stateless/{containerId}
```

**Groovy scripts** (a related mechanism) are used for:
- Dynamic rules for assessment completion
- Bill segment amount rounding logic

### 4.10 Jasper Reports

Jasper Reports is integrated for operational report generation:

- Reports are defined as `.jrxml` templates
- The `SCHEDULED_OPERATIONAL_REPORT.bpmn` process uses Jasper to generate reports on a Quartz schedule
- Generated reports are stored in the DMS (Document Management System)
- Report generation triggers a contact letter notification to the recipient

### 4.11 OPA (Open Policy Agent)

OPA provides unified policy enforcement across the platform:

- Policies are defined as Rego rules
- All services consult OPA for authorisation decisions
- OPA is deployed as a sidecar or standalone service within the Kubernetes cluster
- Combined with Keycloak JWT claims to enforce fine-grained access control

---

## 5. REST API — Batch Engine

The Revenue Management Batch engine exposes a REST API for job management. All endpoints are relative to the base URL of the batch service.

### 5.1 Repository Endpoints (Read-Only)

#### List All Available Jobs
```
GET /rest/api/batch/repository/jobs
```
Returns a list of all registered batch job names.

**Example response:**
```json
[
  "OBLIGATION_MAINTENANCE_BATCH_JOB",
  "TRIGGER_PENALTY_INTEGER_WITH_OVERDUE_BILL_SEGMENTS",
  "ALLOCATION_MAINTENANCE",
  "PAYMENT_FILE_BATCH_JOB",
  "LIABILITY_BALANCE_MAINTENANCE_BATCH_JOB",
  "DELETE_NON_ASSOCIATED_ATTACHMENTS_BATCH_JOB",
  "JOURNAL_ENTRY_EXPORT_BATCH_JOB"
]
```

#### Get Job Instances
```
GET /rest/api/batch/repository/jobs/{jobName}/instances
```
Returns all instances of a named job.

#### Get Executions for a Job Instance
```
GET /rest/api/batch/repository/executions/{jobInstanceId}
```
Returns all step executions within a given job instance.

#### Get a Specific Job Execution
```
GET /rest/api/batch/repository/executions/{jobExecutionId}
```
Returns details of a single job execution (status, start time, end time, exit code, etc.).

### 5.2 Engine Endpoints (Control)

#### Start a Job
```
POST /rest/api/batch/engine/start?jobName={name}
```
Triggers a new execution of the named batch job.

**Query parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `jobName` | string | Yes | Name of the registered job to start |

#### Restart a Job Instance
```
POST /rest/api/batch/engine/restart?jobInstanceId={id}
```
Restarts a failed or stopped job instance from the last failed step.

**Query parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `jobInstanceId` | string | Yes | ID of the job instance to restart |

#### Stop a Job Instance
```
PUT /rest/api/batch/engine/stop?jobInstanceId={id}
```
Requests a graceful stop of a running job instance.

**Query parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `jobInstanceId` | string | Yes | ID of the running job instance to stop |

### 5.3 Schedule Endpoints

#### Get All Schedules
```
GET /rest/api/batch/schedule/
```
Returns all configured batch job schedules (cron expressions and associated job names).

### 5.4 Batch Item Endpoints

#### Get Item Status
```
GET /rest/api/batch/item/status?jobInstanceId={id}
```
Returns the processing status of individual items within a job instance. Useful for identifying which specific records failed.

#### Delete Batch Items
```
DELETE /rest/api/batch/item
```
Deletes specified batch items (e.g., to clear failed items that cannot be retried).

**Request body:**
```json
{
  "batchItemIds": ["string", "string"]
}
```

---

## 6. Batch Jobs

The following batch jobs are registered in the Revenue Management Batch engine:

### 6.1 `OBLIGATION_MAINTENANCE_BATCH_JOB`

Performs periodic maintenance of obligation records. This includes:
- Recalculating obligation statuses
- Processing due date transitions
- Updating overdue flags based on current date

**Typical schedule:** Daily (configurable)

### 6.2 `TRIGGER_PENALTY_INTEGER_WITH_OVERDUE_BILL_SEGMENTS`

Identifies bill segments that are overdue and triggers the penalty calculation process. Works in conjunction with the `DEBT_RECOVERY_PROCESS.bpmn` to initiate enforcement actions.

**Typical schedule:** Daily or periodic (configurable)

### 6.3 `ALLOCATION_MAINTENANCE`

Manages payment allocation across outstanding bill segments. Ensures payments are correctly allocated according to allocation rules (e.g., oldest-first, priority-based).

**Typical schedule:** Daily (configurable)

### 6.4 `PAYMENT_FILE_BATCH_JOB`

Processes incoming payment files (typically ISO20022/PAIN format from bank statements):
- Validates file format and content
- Matches payments to taxpayer accounts and bill segments
- Creates payment tenders and financial transactions
- Triggers downstream allocation

**Typical schedule:** Triggered on file arrival or periodic poll

### 6.5 `LIABILITY_BALANCE_MAINTENANCE_BATCH_JOB`

Recalculates and updates liability balance summaries across accounts. Ensures balance totals are current for reporting and collection purposes.

**Typical schedule:** Daily (configurable)

### 6.6 `DELETE_NON_ASSOCIATED_ATTACHMENTS_BATCH_JOB`

Housekeeping job that removes orphaned document attachments that are no longer associated with any entity. Supports data hygiene and storage management.

**Typical schedule:** Weekly or monthly (configurable)

### 6.7 `JOURNAL_ENTRY_EXPORT_BATCH_JOB`

Exports journal entries to an external accounting system or file. Supports financial reconciliation between SOLON TAX and external ERP/GL systems.

**Typical schedule:** Daily (configurable)

---

## 7. Monitoring and Observability

### 7.1 ELK Stack

The primary monitoring and log aggregation platform is the **ELK Stack** (Elasticsearch, Logstash, Kibana).

**Components deployed:**

| Component | Role |
|---|---|
| **Elasticsearch** | Log and metric storage; full-text search and aggregation |
| **Logstash** | Log ingestion pipeline; parsing and transformation |
| **Kibana** | Visualisation and dashboards |
| **Fleet Agents** | Deployed per node/pod for log and metric collection |
| **Metricbeat** | Infrastructure metrics collection (CPU, memory, disk, network) |
| **Filebeat** | Log file shipping from pods to Logstash |
| **APM Agent** | Application Performance Monitoring (embedded in services) |

**Structured logging:** All SOLON TAX microservices emit structured JSON logs. Key fields include:
- `timestamp` — ISO8601 timestamp
- `level` — Log level (INFO, WARN, ERROR)
- `service` — Service name
- `traceId` — Distributed trace ID (for correlating across services)
- `spanId` — Span identifier within a trace
- `message` — Human-readable log message
- `exception` — Stack trace (for errors)

**Kibana dashboards** provide:
- Real-time log search and filtering
- Service error rate dashboards
- Infrastructure health overviews
- Custom business metric dashboards
- Batch job execution history and status

### 7.2 Application Performance Monitoring (APM)

The Elastic APM agent is embedded in all SOLON TAX microservices:

- **Distributed tracing:** Requests are traced across service boundaries using trace IDs propagated via HTTP headers
- **Transaction monitoring:** Each REST API call and Kafka message processing is recorded as a transaction
- **Error tracking:** Uncaught exceptions and errors are captured with full context
- **Performance metrics:** Response times, throughput, and error rates per endpoint
- **Service map:** Visual representation of service dependencies and call patterns

APM data is stored in Elasticsearch and viewable via Kibana APM UI.

### 7.3 Database Monitoring

Database health and performance monitoring includes:

- **Connection pool monitoring:** Pool usage, wait times, and connection errors
- **Query performance:** Slow query logging and analysis
- **Replication lag:** For HA setups, monitoring of replica lag
- **Storage capacity:** Tablespace and disk usage alerts
- **Deadlock detection:** Monitoring for transaction deadlocks

Database metrics are collected via Metricbeat database modules and stored in Elasticsearch.

### 7.4 Batch Monitoring

Batch job monitoring is available through two channels:

**1. Amplio Administration UI (Batch section):**
- View all scheduled jobs and their next run times
- View execution history (status, start time, duration, exit code)
- Drill down into individual step executions
- Inspect failed items with error details
- Manually start, stop, or restart jobs
- Delete or retry failed batch items

**2. Kibana dashboards:**
- Batch job execution counts and durations
- Error rate trends over time
- Alert rules for jobs that fail or exceed expected duration

**Key batch monitoring metrics:**
- Job instance count (total / successful / failed)
- Step execution status (started / completed / failed / skipped)
- Item read / write / skip counts per step
- Job duration (actual vs. expected)
- Dead-letter items awaiting manual intervention

---

## 8. Backup and Recovery

### 8.1 Database Backup

SOLON TAX databases (PostgreSQL or Oracle) should be backed up according to the following strategy:

- **Full backup:** Daily (off-peak hours)
- **Incremental / WAL archiving:** Continuous (point-in-time recovery capability)
- **Retention:** As per environment policy (typically 30 days for production)
- **Backup verification:** Periodic restore tests to verify backup integrity

### 8.2 Kafka Recovery

Because all Kafka events are persisted via the Outbox Pattern, Kafka topics can be fully reconstructed from the database. In the event of Kafka cluster failure:

1. Restore Kafka cluster from backup (if available) or recreate topics
2. Replay events from the outbox tables in the source databases
3. Services will re-process events idempotently

### 8.3 Kubernetes Recovery

- All Kubernetes manifests (Helm charts / YAML) are stored in version control
- Cluster state can be recreated by reapplying manifests
- Persistent Volume Claims (PVCs) must be backed up separately (e.g., Velero for Kubernetes backups)

### 8.4 BPMN Process Recovery

- All deployed BPMN definitions are stored in the database (`bpmn_diagram` system parameter, `diagram` attribute)
- Process definitions can be redeployed via the Amplio Administration UI or API
- Running process instances persist state in the process engine database; recovery requires database restoration

---

## 9. Scaling

### 9.1 Horizontal Scaling

All SOLON TAX microservices are stateless (session state in Redis; shared cache in Hazelcast) and can be horizontally scaled by increasing the replica count in Kubernetes:

```yaml
spec:
  replicas: 3  # Increase for higher throughput
```

- **Amplio Process Engine:** Supports multi-node deployment; Hazelcast manages distributed state
- **Microservices:** Stateless; scale independently
- **Batch workers:** Can be scaled for parallel batch processing

### 9.2 Kafka Partition Scaling

For higher event throughput, Kafka topic partitions can be increased:
- Match consumer group worker count to partition count for optimal parallelism
- Be aware that increasing partitions on existing topics may affect ordering guarantees

### 9.3 Database Scaling

- PostgreSQL: Use read replicas for read-heavy workloads; connection pooling via PgBouncer
- Oracle: RAC (Real Application Clusters) for HA and horizontal read scaling

---

## 10. Security

### 10.1 Authentication and Authorisation

- **Authentication:** All API requests require a valid JWT Bearer token issued by Keycloak
- **Authorisation:** JWT claims (roles) are evaluated by each microservice and by OPA
- **Self-Service:** Taxpayer self-service portal uses separate Keycloak realm with MFA support
- **Service-to-service:** Internal service calls use OAuth2 client credentials flow

### 10.2 Data Security

- **Row-Level Security:** Enforced at the database query layer via JPA filters, using the authenticated user's identity to limit visible data
- **Data at rest:** Database encryption at rest (OS/filesystem or DB-level encryption)
- **Data in transit:** All connections TLS-encrypted (internal and external)
- **GDPR compliance:** GDPR anonymization batch job (`GDPR_ANONYMIZATION` via `revenue-management-etl`) removes personal data according to retention schedules; `NOTIFY_GDPR_EXPIRATION.bpmn` warns parties before consent expiry

### 10.3 Network Security

- **Kubernetes NetworkPolicies:** Restrict pod-to-pod communication to required paths
- **Ingress TLS:** External endpoints protected with TLS certificates
- **Secrets management:** Kubernetes Secrets or external vault (e.g., HashiCorp Vault) for credentials

---

## 11. Data Archiving

SOLON TAX includes an archiving framework to manage data lifecycle:

- **Archiving batch jobs** are provided in both `revenue-management-etl` and `revenue-management-batch`
- Archiving moves aged records from operational tables to archive tables or external storage
- Archiving policies (retention periods, entity types) are configurable per client
- Archived data remains queryable for audit and reporting purposes

**Entities covered by archiving:**
- Case records
- Human tasks
- Contact records
- Process instances
- Financial transactions (FTs) beyond retention period
- Attachments and documents (after de-association cleanup)

---

## 12. High Availability and Disaster Recovery

### 12.1 High Availability Configuration

| Component | HA Configuration |
|---|---|
| Kubernetes | Multi-node cluster; pod anti-affinity rules |
| Kafka | Multi-broker cluster with replication factor ≥ 3 |
| Keycloak | Active-active cluster |
| Redis | Redis Cluster or Sentinel mode |
| Hazelcast | Auto-discovery cluster (Kubernetes pod labels) |
| Database | PostgreSQL with streaming replication; Oracle RAC |
| Amplio Process Engine | Multi-replica with Hazelcast shared state |

### 12.2 Disaster Recovery

- **RPO (Recovery Point Objective):** Determined by backup frequency and WAL archiving interval
- **RTO (Recovery Time Objective):** Determined by Kubernetes cluster recreation time + database restore time
- **Cross-region DR:** Supported via Kubernetes federation or active-passive cluster configuration
- **Failover testing:** Recommended quarterly for production environments

---

## 13. Maintenance

### 13.1 Database Migration

All schema changes are managed via **Liquibase** (repository: `revenue-management-db-migration`):

- Migrations are applied automatically on service startup (in non-production environments) or via a separate migration job (in production)
- All migrations are versioned and reversible where possible
- Zero-downtime migrations: additive changes preferred; breaking changes require coordinated deployments

### 13.2 BPMN Deployment

Process definition updates:

1. Deploy updated `.bpmn` file via Amplio Administration UI (Batch > BPMN Deployments)
2. Or via API call to the process engine deployment endpoint
3. **Versioning behaviour:**
   - Running process instances continue on the version they started with
   - New process instances start on the latest active version
   - Restarted (cancelled or passed) processes use the newest active version

### 13.3 Configuration Updates

**Without redeployment (runtime overrides):**
- Use **Process Variables** system parameter in Amplio Admin to override Kubernetes environment variables
- Changes take effect immediately without pod restarts

**With redeployment:**
- Update Kubernetes ConfigMaps or Secrets
- Rolling restart of affected pods

### 13.4 Keycloak Maintenance

- User and role management via Keycloak Admin Console
- Realm export/import for configuration backup
- Client secret rotation via Keycloak Admin API

---

## 14. Troubleshooting

### 14.1 Process Execution Errors

**Symptom:** Process stuck in error state in Amplio Admin

**Resolution:**
1. Open Amplio Administration UI → Process Administration
2. Locate the failed process instance (filter by BPM ID or entity)
3. Review the Process Context for the current state and error details
4. Use **Rerun** action to retry the failed task
5. If retry is not possible, use **Show Process Command** to understand the command that failed

### 14.2 Kafka Message Processing Failures

**Symptom:** Messages in Dead Letter Queue; services not processing events

**Resolution:**
1. Open Amplio Administration UI → Event Stream Management
2. View message count per group; inspect messages in DLQ
3. Identify root cause (e.g., downstream service unavailable, invalid payload)
4. Fix root cause
5. Use **Requeue** action to replay DLQ messages

### 14.3 Batch Job Failures

**Symptom:** Batch job in FAILED state

**Resolution:**
1. Check Amplio Admin → Batch section for job execution status
2. Drill down to the failed step execution
3. Check item status for specific failed records (`GET /rest/api/batch/item/status?jobInstanceId={id}`)
4. Review Kibana logs for the batch service around the execution time
5. Fix data or configuration issues
6. Use `POST /rest/api/batch/engine/restart?jobInstanceId={id}` to restart from the last failed step
7. If items are permanently unprocessable, delete them via `DELETE /rest/api/batch/item`

### 14.4 Authentication Failures

**Symptom:** 401 Unauthorized responses

**Resolution:**
1. Verify Keycloak is running and reachable
2. Check JWT token expiry (default token lifetime varies by realm configuration)
3. Verify the user has the required roles for the requested resource
4. Check OPA policy logs for policy evaluation failures
5. Review Kibana logs for `authentication` or `authorization` error patterns

### 14.5 Performance Degradation

**Symptom:** Slow API responses; high latency in Kibana APM

**Resolution:**
1. Check Kibana APM for slow transactions and their breakdowns
2. Identify bottlenecks: database queries, Kafka lag, external service calls
3. Check Hazelcast cache hit rates; cache misses may indicate a need for cache warming
4. Review database slow query logs; add indexes if needed
5. Consider horizontal scaling of affected services (increase replica count)
6. Check Kafka consumer lag; increase partitions or consumer group members if needed

---

## 15. Prerequisites and Installation

### 15.1 Infrastructure Prerequisites

| Requirement | Specification |
|---|---|
| Kubernetes | v1.25+ |
| Helm | v3.x |
| Container Registry | Docker Hub compatible registry |
| Database | PostgreSQL 14+ or Oracle 19c+ |
| Java | JDK 17 (for backend services) |
| Node.js | v18+ (for frontend build) |

### 15.2 Resource Sizing (Indicative)

| Component | CPU Request | Memory Request |
|---|---|---|
| Core microservices (each) | 0.5 CPU | 512 Mi |
| Amplio Process Engine | 1 CPU | 1 Gi |
| Kafka Broker (each) | 1 CPU | 2 Gi |
| Keycloak | 0.5 CPU | 1 Gi |
| Redis | 0.25 CPU | 512 Mi |
| Hazelcast (each member) | 0.5 CPU | 1 Gi |
| PostgreSQL | 2 CPU | 4 Gi |
| Elasticsearch | 2 CPU | 4 Gi |

*Actual sizing must be determined by load testing for the target environment.*

### 15.3 Environment Variables

Key environment variables consumed by SOLON TAX services (can be overridden at runtime via Process Variables system parameter):

| Variable | Purpose |
|---|---|
| `KEYCLOAK_AUTH_SERVER_URL` | Keycloak server URL |
| `KEYCLOAK_REALM` | Keycloak realm name |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka broker addresses |
| `DB_URL` | Database JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials |
| `REDIS_HOST` / `REDIS_PORT` | Redis connection details |
| `HAZELCAST_CLUSTER_NAME` | Hazelcast cluster identifier |
| `DROOLS_RUNTIME_URL` | Drools KIE runtime base URL |
| `DMS_URL` | Document Management System URL |

---

## 16. Appendix A — Amplio Administration UI

The Amplio Administration UI is a shared component used to administer both:
- **Amplio Process Engine** — processes, events, BPMN diagrams, system parameters
- **Revenue Management Batch** — batch jobs (scheduling, monitoring, error handling)

The UI is accessible through the administration page of the SOLON TAX application.

---

### 16.1 Process Administration

The process administration tool provides a tabular view of all process instances across all engines.

**Process table fields:**

| Field | Description |
|---|---|
| BPM ID | The unique identifier of the BPM task/instance |
| Auto/Manual | Whether the task is handled automatically or manually in Amplio; typically `True` (automatic) |
| Last Changed | Timestamp of the last state change; time zone defaults to Copenhagen, Europe |
| Changed By | ID of the entity that last changed the task — typically a caseworker's initials, or `BPM_ENGINE` if processed without human interaction |

**Per-task actions available from the menu:**

| Action | Description |
|---|---|
| **Rerun** | Force the process to rerun the selected step (useful after fixing an error condition) |
| **Show Process Command** | Display the process command that was sent to the engine for this task (similar to the Process Context view) |
| **Show Task View Data** | Show the relevant view data if the process uses a view data mechanism |

**Process flow visualisation:** Beneath the selected task, a graphical representation of the BPMN process flow is displayed. Tasks not yet executed appear in white without a status tag. This gives operators a visual understanding of where in the process a given instance currently sits.

---

### 16.2 Process Context

The Process Context displays the raw contents of the process instance data in its current state. It is accessed via a modal popup from the process table.

This is the complete data payload the process has acted on during its execution up to the current state. It is useful for:
- Diagnosing what data a process received at each step
- Understanding why a step succeeded or failed
- Manually reconstructing expected vs. actual state

---

### 16.3 Process Errors

The Process Errors section provides a filtered view of process instances that are currently in an error state. For each error:
- The error message and stack trace are displayed
- The process context at the point of failure is accessible
- The **Rerun** action can be used to retry after fixing the root cause

---

### 16.4 Portal Texts

The Portal Texts section manages the text content used in the self-service portal and communications:
- Localised text strings by language
- Letter templates
- Notification messages

Changes take effect immediately without redeployment.

---

### 16.5 Event Administration

The Event Administration section shows the configured event types and their subscriptions:
- Lists all registered event types in the system
- Shows which processes are subscribed to each event type
- Allows enabling/disabling event subscriptions

---

### 16.6 System Parameters

System parameters are configuration values managed at runtime. They have an active/inactive validity period — inactive instances are retained for history and are never deleted.

**Validity period overlap:** Overlapping validity periods are resolved by the system's defined overlap principles.

**Technical history:** All system parameters track:
- Creation time
- Modification time (when edited)
- Publication time (when deployed to production)
- Previous versions (always accessible)

**System parameters relevant to the Process Engine:**

| Parameter | Purpose |
|---|---|
| **Eventplans** | Defines the connection between event types and processes. Disabling a subscription prevents the relevant process from being triggered for that event type. Used to temporarily disable creation of specific processes without code changes. |
| **Language** | Defines languages for localisation of the administration interface. |
| **Process Variables** | Allows defining specific process variables that override Kubernetes environment variables at runtime. Used to change endpoints or override values without redeployment. |
| **BPMN Deployments** | Shows the deployed BPMN models and their related event types and process types. |
| **bpmn_diagram** | Stores the actual BPMN XML for each deployed process definition. Key attributes: `diagram` (the BPMN XML), `process_definition_key`, `file_name`. |

---

### 16.7 BPMN Versioning Model

The platform follows a defined model for handling multiple versions of a BPMN diagram:

| Scenario | Behaviour |
|---|---|
| Running process instances | Continue executing on the **version they started with** |
| New process instances | Start on the **latest active version** |
| Restart of finished processes (cancelled or passed) | Run on the **newest active version** |

This ensures consistent behaviour for in-flight processes while allowing new process improvements to be deployed immediately for new instances.

---

### 16.8 Event Stream Management

The Event Stream Management section provides operators visibility into and control over the JDBC message queue that underpins the Outbox Pattern.

**Capabilities:**

| Capability | Description |
|---|---|
| View message counts | See the number of pending messages for each consumer group |
| Inspect messages | View the content of individual messages (payload, headers, status) |
| Requeue DLQ messages | Move messages from the Dead Letter Queue back to the main queue for reprocessing |

This is the primary recovery mechanism when downstream services fail or reject messages.

---

### 16.9 Batch Administration UI

The Batch section of the Amplio Administration UI provides full management of the Revenue Management Batch engine.

**Features:**

| Feature | Description |
|---|---|
| **Job repository** | Lists all registered batch jobs with metadata |
| **Job schedules** | View and manage cron-based schedules for each job |
| **Execution history** | Full history of job instance executions with status, duration, and exit codes |
| **Step execution detail** | Drill down into individual step executions within a job run |
| **Item status** | View the read/write/skip/error status of individual processed items |
| **Manual controls** | Start, stop, or restart any batch job via the UI (equivalent to REST API operations) |
| **Failed item management** | Inspect and delete failed items that cannot be automatically retried |

---

## Data Area (Custom Fields)

The following entities support the **Data Area** mechanism — a flexible custom fields extension stored as JSON (jsonb in PostgreSQL, blob in Oracle):

| Database | Entity |
|---|---|
| `irm_casemanagement_db` | `case_` |
| `irm_contactmanagement_db` | `contact` |
| `irm_humantaskmanagement_db` | `human_task` |
| `irm_registration_db` | `obligation_action`, `party_role`, `party_role_relationship`, `revenue_type_registration` |
| `irm_risk_analysis_db` | `candidate_for_intervention` |
| `irm_taxpayeraccounting_db` | `bill_segment`, `c_payment_file`, `c_payment_record`, `ft`, `overpayment_process`, `payment_tender` (and subtypes: `_card`, `_check`, `_digital_wallet`, `_direct_debit`, `_ebt`) |
| `irm_uiconfiguration_db` | `c_menu` |

Data Area fields are schema-less extensions — each client can add custom attributes to these entities without modifying the core database schema. They are accessible and queryable as standard entity attributes throughout the platform.

---

*End of document — SOLON TAX Operations Guide v2.3.0*
