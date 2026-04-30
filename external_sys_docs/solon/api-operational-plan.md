# Operational Plan API — Reference
**Version:** 2.1.0  
**Base path:** `/operational-plan-management` (assumed; routed via Solon API Gateway)  
**Auth:** Bearer JWT from Keycloak on every request

> Operational Plans are supervisor-level planning and target-tracking objects. They group cases into a structured plan with measurable objectives and weighted targets — used to manage debt recovery campaigns, set collection targets, and track actual vs. planned performance. This is NOT a process control API; it has no direct relationship to BPMN process execution.

---

## Endpoints

### Operational Plan Types (Configuration)

| Method | Path | Summary |
|---|---|---|
| GET | `/operational-plan-types` | Search types (filters: `code`, `description`, `operationalPlanTypeCategoryCL`) |
| POST | `/operational-plan-types` | Create a new operational plan type |
| GET | `/operational-plan-types/{code}` | Get by code |
| PUT | `/operational-plan-types/{code}` | Update |
| DELETE | `/operational-plan-types/{code}` | Delete |
| GET | `/operational-plan-types/{code}/exists` | Check existence (returns boolean) |
| GET | `/operational-plan-types/dropdown` | Dropdown list (code + label) |

**`OperationalPlanType` schema:**

| Field | Type | Notes |
|---|---|---|
| `code` | string (max 10) | Unique identifier |
| `description` | string (max 50) | Human-readable label |
| `ownerCL` | string | From `OWNERS` codelist |
| `operationalPlanTypeCategoryCL` | string | From `OPERATIONAL_PLAN_CATEGORIES` |
| `approvalProcessTypeCD` | string (max 10) | Links to an approval process type — operational plans of this type require approval workflow |

---

### Operational Plans (Instances)

| Method | Path | Summary |
|---|---|---|
| GET | `/operational-plans` | Search plans (paginated) |
| POST | `/operational-plans` | Create a new operational plan |
| GET | `/operational-plans/{id}` | Get by ID |
| PUT | `/operational-plans/{id}` | Update |
| DELETE | `/operational-plans/{id}` | Delete |

**GET query filters:** `id`, `description`, `operationalPlanType`, `operationalPlanStatusCL`, `fromStartDt`, `toStartDt`

**`OperationalPlan` schema:**

| Field | Type | Notes |
|---|---|---|
| `id` | string | Null on CREATE |
| `operationalPlanTypeCode` | string | References a configured OperationalPlanType |
| `operationalPlanStatusCL` | string | From `OPERATIONAL_PLAN_STATUSES` |
| `description` | string (max 50) | Plan description |
| `startDt` / `endDt` | date | Plan active period |
| `scope` | string | Textual scope description |
| `operationalPlanObjectives` | array (min 1) | Objectives with targets |
| `linkedCases` | integer | **Read-only.** Count of cases currently linked to this plan |
| `completedCases` | integer | **Read-only.** Count of completed linked cases |
| `percentage` | integer | **Read-only.** Overall completion percentage |
| `totalAuditAssessedAmount` | number | **Read-only.** Total assessed amount from related cases |

**Validation rules:**
- `id` must be null on CREATE, non-null on UPDATE
- `operationalPlanTypeCode` must reference an existing type

---

### Operational Plan Objectives

Objectives are nested within an `OperationalPlan` — they are not a separate endpoint.

**`OperationalPlanObjective` schema:**

| Field | Type | Notes |
|---|---|---|
| `id` | string | Null on CREATE |
| `description` | string (max 50) | Objective description |
| `operationalPlanTargets` | array (min 1) | Weighted targets for this objective |
| `percentage` | integer | Read-only completion % |

**Validation:** Each objective must have at least one target. The sum of all target `weight` values within an objective must equal 100.

---

### Operational Plan Targets

Targets are nested within an `OperationalPlanObjective`.

**`OperationalPlanTarget` schema:**

| Field | Type | Notes |
|---|---|---|
| `id` | string | Null on CREATE |
| `description` | string (max 50) | Target description |
| `operationalPlanMetricCL` | string | From `TARGET_METRICS` — what is being measured (e.g. cases resolved, amount collected) |
| `weight` | number (1–100) | Weight of this target within its objective (all weights in an objective must sum to 100) |
| `targetValue` | integer (min 1) | The planned target value |
| `actualValue` | integer | Read-only — the actual measured value |
| `percentage` | integer | Read-only completion % |
| `operationalPlanTargetValues` | array (min 1) | Dimension breakdowns of the target |

---

### Target Dimension Values

Break down a target by dimension (e.g. by revenue type, by region, by caseworker).

**`OperationalPlanTargetDimensionValue` schema:**

| Field | Notes |
|---|---|
| `operationalPlanDimensionTypeCL` | From `OPERATIONAL_PLAN_DIMENSION_TYPES` — the type of dimension being sliced by |
| `operationalPlanTargetDimension` | The dimension name (e.g. `REVENUE_TYPE`) |
| `operationalPlanTargetDimensionValue` | The specific value for this dimension slice (max 36 chars) |

---

## Architectural significance

### What Operational Plan IS

A supervisor-level planning tool for structuring and tracking debt recovery campaigns. A plan contains:
- Multiple **objectives** (e.g. "Increase resolved cases by 20%")
- Each objective has **weighted targets** measured against specific metrics from the `TARGET_METRICS` codelist
- Targets can be sliced by **dimensions** (e.g. broken down by revenue type or agent team)
- The plan tracks `linkedCases`, `completedCases`, and `percentage` automatically from related case data

### What Operational Plan is NOT

- It is not a process orchestration mechanism
- It has no relationship to BPMN process instances or Kafka commands
- Updating an operational plan's status (`operationalPlanStatusCL`) is done via `PUT /operational-plans/{id}` — there is no Kafka command for this

### Who uses it in the additional layer

The **Supervisor Dashboard** and **MI & Analytics** components read operational plan data to display:
- `GET /operational-plans?operationalPlanStatusCL=ACTIVE` — active campaigns
- Plan completion % and linked case counts
- Actual vs. target values per objective

The additional layer may also **create** operational plans when a new debt recovery campaign or champion/challenger test is initiated — linking cases to the plan as they are assigned.

### Approval workflow

`approvalProcessTypeCD` on the OperationalPlanType links to an approval process — plans of certain types require an approval workflow before activation. This is consistent with the Deployment Pipeline (draft → review → active) in the Strategy Engine.

---

## Common headers

| Header | Values | Purpose |
|---|---|---|
| `Authorization` | `Bearer {jwt}` | Keycloak JWT — required |
| `irm-origin-client` | `UI-CLIENT` or `FORMS` | Origin indicator |
| `irm-include-labels` | `TRUE` or `FALSE` | Whether to include labels |
| `Accept-Language` | e.g. `en-GB` | Language for returned descriptions |
