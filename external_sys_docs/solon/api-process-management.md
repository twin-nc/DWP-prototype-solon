# Process Management API — Reference
**Version:** 2.1.0  
**Base path:** `/process-management` (assumed; routed via Kubernetes service DNS)  
**Auth:** Bearer JWT from Keycloak on every request

---

## Endpoints

### Process Definitions

| Method | Path | Summary |
|---|---|---|
| GET | `/process-definitions/dropdown` | List all deployed BPMN process definitions (code + label) |

**`ProcessDefinitionDetails` schema:**

| Field | Type | Notes |
|---|---|---|
| `processId` | string | BPMN process definition ID |
| `label` | string | Human-readable label |
| `version` | integer | Version number |
| `resource` | byte (base64) | **The BPMN XML file itself** — used for upload/deploy |
| `createdDateTime` | datetime | When this version was deployed |

A POST endpoint for deploying new BPMN definitions must exist (the Admin API calls it); not fully documented in available specs but confirmed via the `resource` field on the schema.

---

### Process Types

| Method | Path | Summary |
|---|---|---|
| GET | `/process-types` | List/search process types |
| POST | `/process-types` | Create a new process type |
| GET | `/process-types/{code}` | Get by code |
| PUT | `/process-types/{code}` | Update |
| DELETE | `/process-types/{code}` | Delete |

**`ProcessType` schema:**

| Field | Type | Notes |
|---|---|---|
| `code` | string (max 10) | Unique identifier |
| `processDefinition` | string | Links to a deployed BPMN definition |
| `entityCL` | string | Entity type this process applies to (from `ENTITIES` codelist) |
| `categoryCL` | string | From `PROCESS_CATEGORIES` codelist |
| `active` | boolean | Defaults to false if omitted |
| `ownerCL` | string | From `OWNERS` codelist |
| `labels` | array of Label | Multilingual labels |
| `validStatusTransitions` | array | Allowed from→to entity status transitions |
| `scheduledProcessCategoryCL` | string | From `SCHEDULED_PROCESS_CATEGORIES` codelist |
| `tenantId` | string (max 50) | Tenant identifier |

**`ValidProcessTypeStatusTransition` schema:**

| Field | Notes |
|---|---|
| `fromEntityStatusCL` | From status (from `ENTITY_STATUSES`) |
| `toEntityStatusCL` | To status |
| `manualTransitionAllowed` | Whether users can manually trigger this transition |
| `formRuleUseCL` | Optional form rule use |

---

### Process Instances

| Method | Path | Summary |
|---|---|---|
| GET | `/process-instances` | List/search process instances |
| POST | `/process-instances` | Start a new process instance |
| GET | `/process-instances/{id}` | Get by ID |
| GET | `/process-instances/{id}/relations` | Get parent/child process relations |

**`ProcessInstance` schema:**

| Field | Type | Notes |
|---|---|---|
| `id` | string | Null on CREATE |
| `processTypeCode` | string | Which process type to run |
| `entityCL` | string | Entity type the process runs against |
| `entityId` | string | ID of the specific entity (e.g. debtor party role ID) |
| `processInstanceStatusCL` | string | From `PROCESS_INSTANCE_STATUSES` |
| `startDateTime` | datetime | Set by system |
| `startedBy` | Identification | Who started it |
| `tenantId` | string | Tenant context |

**`ProcessInstanceRelations` schema:**

| Field | Notes |
|---|---|
| `parent` | Parent process instance (if this is a sub-process) |
| `children` | Child process instances started by this process |

---

### Process Flows

A ProcessFlow is a join record linking a domain entity to a process instance. The additional layer uses this to track which strategy (process instance) is running against which account.

| Method | Path | Summary |
|---|---|---|
| GET | `/process-flows` | List/search process flows |
| POST | `/process-flows` | Create a new process flow |
| GET | `/process-flows/{id}` | Get by ID |
| DELETE | `/process-flows/{id}` | Remove |

**GET query parameters:** `id[]`, `entityStatusCL`, `entityCL`, `sourceEntityId`, `processInstanceId`

**`ProcessFlow` schema:**

| Field | Type | Notes |
|---|---|---|
| `id` | string (UUID) | Null on CREATE |
| `sourceEntityId` | string (UUID) | The entity being tracked |
| `entityCL` | string | Entity type (from `ENTITIES` codelist) |
| `processInstanceId` | string | The running process instance |
| `entityStatusCL` | string | Current status of the flow |

---

### Scheduled Processes

| Method | Path | Summary |
|---|---|---|
| GET | `/scheduled-processes` | List/search scheduled processes |
| POST | `/scheduled-processes` | Create a new scheduled process |
| GET | `/scheduled-processes/{id}` | Get by ID |
| PUT | `/scheduled-processes/{id}` | Update |
| DELETE | `/scheduled-processes/{id}` | Delete |
| GET | `/scheduled-processes/{id}/instances` | List instances of a scheduled process |

**`ScheduledProcess` schema:**

| Field | Type | Notes |
|---|---|---|
| `id` | string | Auto-generated; null on CREATE |
| `processTypeCode` | string | Which process type to trigger |
| `cronExpression` | string (max 250) | Standard cron expression |
| `relatedEntities` | array of ScheduledProcessRelatedEntityDto | Entities scoped to this schedule |
| `input` | string | Optional input data (e.g. email address for contact) |
| `active` | boolean | Only runs when true |
| `labels` | array of Label | Multilingual labels (min 1) |
| `scheduledProcessCategoryCL` | string | Read-only; derived from process type |
| `scheduledProcessInstances` | array | Read-only; history of fired instances |

**`ScheduledProcessRelatedEntityDto`:**

| Field | Notes |
|---|---|
| `entityCL` | Entity type |
| `entityId` | Entity ID (UUID) |

---

### Node Instances

| Method | Path | Summary |
|---|---|---|
| GET | `/node-instances` | List node instances (filter by processInstanceId) |
| GET | `/node-instances/{id}` | Get specific node instance |

**`NodeInstance` schema:**

| Field | Notes |
|---|---|
| `id` | Node instance ID |
| `name` | BPMN node name |
| `processInstanceId` | Parent process instance |
| `nodeInstanceStatusCL` | From `NODE_INSTANCE_STATUSES` |
| `nodeInstanceTypeCL` | From `NODE_INSTANCE_TYPES` |
| `startDateTime` | When this node was entered |
| `tenantId` | Tenant context |

---

## Common headers

| Header | Values | Purpose |
|---|---|---|
| `Authorization` | `Bearer {jwt}` | Keycloak JWT — required on all calls |
| `irm-origin-client` | `UI-CLIENT` or `FORMS` | Controls which validation path applies |
| `irm-include-labels` | `TRUE` or `FALSE` | Whether to include description labels in responses |
| `Accept-Language` | e.g. `en-GB` | Language for returned descriptions |
