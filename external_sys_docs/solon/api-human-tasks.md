# Human Tasks API — Reference
**Version:** 2.1.0  
**Base path:** `/human-task-management` (assumed; routed via Kubernetes service DNS)  
**Auth:** Bearer JWT from Keycloak on every request

---

## Endpoints

### Human Task Types (Configuration)

| Method | Path | Summary |
|---|---|---|
| GET | `/human-task-types` | List/search task types |
| POST | `/human-task-types` | **Create a new human task type** |
| GET | `/human-task-types/{code}` | Get by code |
| PUT | `/human-task-types/{code}` | Update |
| DELETE | `/human-task-types/{code}` | Delete (not allowed if used by existing tasks) |
| GET | `/human-task-types/dropdown` | Dropdown list (code + label + activeSw) |
| GET | `/human-task-types/{code}/eligible-users` | Get scored list of eligible users for this task type |

**GET query filters:** `code`, `description`, `comment`, `active`, `humanTaskPriorityCL`, `manualCreationAllowed`, `humanTaskEntityRelationshipTypeCL`, `entityCL`

**`HumanTaskType` schema:**

| Field | Type | Notes |
|---|---|---|
| `code` | string (max 10) | Unique identifier |
| `active` | boolean | Whether this task type is available |
| `humanTaskPriorityCL` | string | Default priority from `HUMAN_TASK_PRIORITIES` |
| `manualCreationAllowed` | boolean | **Whether agents can manually create tasks of this type from the UI** |
| `multipleWorkersSw` | boolean | Whether multiple workers can be assigned simultaneously |
| `humanTaskAssignBehaviorCL` | string | From `HUMAN_TASK_ASSIGN_BEHAVIORS` (e.g. AUTO_ASSIGN) |
| `humanTaskWorkerProfile` | string | Which worker profile is used for eligibility scoring |
| `entities` | array of HumanTaskTypeEntityType | Which entity types can be related to tasks of this type |
| `ownerCL` | string | From `OWNERS` |
| `labels` | array of Label | Multilingual labels |
| `comment` | string (max 255) | Optional notes |

**`HumanTaskTypeEntityType` schema:**

| Field | Notes |
|---|---|
| `entityCL` | Entity type from `ENTITIES` codelist |
| `humanTaskEntityRelationshipTypeCL` | Relationship type from `HUMAN_TASK_ENTITY_RELATIONSHIP_TYPES` |

**`HumanTaskTypeEligibleUser` schema (from `GET /human-task-types/{code}/eligible-users`):**

| Field | Notes |
|---|---|
| `id` | User code |
| `identificationLabel` | Display name |
| `eligibilityScore` | Computed score (higher = better match) |

---

### Human Tasks (Instances)

| Method | Path | Summary |
|---|---|---|
| GET | `/human-tasks` | List/search human tasks |
| POST | `/human-tasks` | **Create a human task directly via REST** |
| GET | `/human-tasks/{id}` | Get by ID |
| PUT | `/human-tasks/{id}` | Update (limited fields in IN_PROGRESS status) |
| POST | `/human-tasks/complete` | Complete a task and signal back to BPMN if linked |

**`HumanTask` schema:**

| Field | Type | Notes |
|---|---|---|
| `id` | string (UUID) | Null on CREATE |
| `humanTaskTypeCode` | string (max 10) | Must reference an active task type |
| `entityStatusCL` | string | PENDING → IN_PROGRESS → COMPLETED / CANCELED; updated via lifecycle actions only |
| `humanTaskPriorityCL` | string | From `HUMAN_TASK_PRIORITIES` |
| `assignedWorkers` | array of HumanTaskAssignedWorker | Assigned workers with allocation % |
| `relatedEntities` | array of HumanTaskRelatedEntityDto | Linked entities |
| `processInstanceId` | string | **Read-only.** Populated if task was created by a BPMN process. Null if created directly via REST. |
| `dataArea` | string (JSON) | **Custom JSON blob — attach I&E data, vulnerability details, form outputs** |
| `startDate` / `endDate` | date | Task scheduling window |
| `comment` | string (max 255) | Task notes |
| `createdUser` | Identification | Who created the task |
| `createDateTime` | datetime | System-set on creation |

**`HumanTaskAssignedWorker` schema:**

| Field | Notes |
|---|---|
| `assignedUser` | Identification of the worker |
| `allocationPercentage` | 0–100% |

**`HumanTaskRelatedEntityDto` schema:**

| Field | Notes |
|---|---|
| `entityCL` | Entity type from `ENTITIES` codelist |
| `entityId` | UUID of the related entity |
| `humanTaskEntityRelationshipTypeCL` | Relationship type from `HUMAN_TASK_ENTITY_RELATIONSHIP_TYPES` |

---

### Key validation rules on HumanTask

- Task cannot be created from the UI (`irm-origin-client: UI-CLIENT`) unless `manualCreationAllowed = true` on the task type. Service-to-service calls (omitting or setting a non-UI origin client) bypass this restriction.
- Status can only change from PENDING → IN_PROGRESS if at least one worker is assigned.
- If the task is linked to a Case Activity (`relatedEntityId` set), status change from PENDING → IN_PROGRESS can only be triggered via the Case Activity endpoint when called from UI. Service calls bypass this.
- In IN_PROGRESS status, only `assignedWorkers`, `humanTaskPriorityCL`, `comment`, `startDate`, `endDate` can be updated.
- Nothing can be updated once status is COMPLETED or CANCELED.

---

### HumanTaskCompleteRequest

```json
POST /human-tasks/complete
{
  "humanTaskId": "uuid-of-task",
  "entityStatusCL": "COMPLETED",
  "changeReasonCL": "REASON_CODE",
  "changeComment": "Optional comment"
}
```

**Critical:** When `processInstanceId` is not null (task was BPMN-created), `entityStatusCL` is **mandatory**. Completing the task with this request **automatically fires the signal back to the waiting BPMN process instance**, advancing the collection strategy.

---

### Human Task Worker Profiles

| Method | Path | Summary |
|---|---|---|
| GET | `/human-task-worker-profiles` | List/search |
| POST | `/human-task-worker-profiles` | Create |
| GET | `/human-task-worker-profiles/{code}` | Get by code |
| PUT | `/human-task-worker-profiles/{code}` | Update |
| DELETE | `/human-task-worker-profiles/{code}` | Delete |

**`HumanTaskWorkerProfile` schema:**

| Field | Notes |
|---|---|
| `code` | Unique code (max 10) |
| `criteria` | Scoring criteria with weights and required flags |
| `criterionUnits` | Individual workers/roles by criterion type |
| `ownerCL` | From `OWNERS` |
| `labels` | Multilingual labels |

**`HumanTaskWorkerProfileCriterionUnit`:**

| `workerProfileCriterionTypeCL` | Description |
|---|---|
| `WORKER` | Match against a specific user |
| `ROLE` | Match against a role |
| `ROLE_SENIORITY_LEVEL` | Match against seniority level from `HUMAN_TASK_SENIORITY_LEVELS` |

---

## Common headers

| Header | Values | Purpose |
|---|---|---|
| `Authorization` | `Bearer {jwt}` | Required on all calls |
| `irm-origin-client` | `UI-CLIENT` or `FORMS` | Controls UI-only validation paths — omit for service-to-service |
| `irm-include-labels` | `TRUE` or `FALSE` | Whether to include description labels |
| `Accept-Language` | e.g. `en-GB` | Language for returned descriptions |
