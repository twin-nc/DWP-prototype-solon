# Suppression Management API — Reference
**Version:** 2.1.0  
**Base path:** `/suppression-management` (assumed; routed via Kubernetes service DNS)  
**Auth:** Bearer JWT from Keycloak on every request

> **Important architectural note:** The Solon integration guide documents suppression via Kafka (`irm.bpmn-engine.create-suppression` / `irm.bpmn-engine.release-suppression`). Those are the commands the *BPMN engine* uses internally to signal the suppression-management service. The REST API documented here is the external-facing surface, accessible directly from the additional layer without going through a BPMN process.

---

## Endpoints

### Suppression Types (Configuration)

| Method | Path | Summary |
|---|---|---|
| GET | `/suppression-types` | List/search suppression types |
| POST | `/suppression-types` | Create a new suppression type |
| GET | `/suppression-types/{code}` | Get by code |
| PUT | `/suppression-types/{code}` | Update |
| DELETE | `/suppression-types/{code}` | Delete |
| GET | `/suppression-types/{code}/exists` | Check existence (returns boolean) |

**GET query filters:** `suppressionTypeCode`, `description`, `manualCreationAllowedSW`, `maintenanceOfAutocreatedAllowedSW`, `maximumNumberDaysTypeCL`, `maximumNumberDays`, `overrideEndDateSW`, `suppressionEntityType`, `suppressionActionTypeCL`, `ownerCL`, `activeSW`

**`SuppressionType` schema:**

| Field | Type | Notes |
|---|---|---|
| `suppressionTypeCode` | string | Unique code |
| `activeSW` | boolean | Whether this type is active |
| `manualCreationAllowedSW` | boolean | **Whether agents can manually create suppressions of this type** |
| `maintenanceOfAutocreatedAllowedSW` | boolean | Whether automatically-created suppressions can be manually maintained |
| `maximumNumberDays` | integer (min 0) | **Maximum duration — used for Breathing Space (set to 60)** |
| `maximumNumberDaysTypeCL` | string | From `MAXIMUM_NUMBER_DAYS_TYPES` (e.g. CALENDAR_DAYS) |
| `overrideEndDateSW` | boolean | Whether the end date can be extended beyond `maximumNumberDays` |
| `suppressionEntityTypeCL` | string | What entity type is suppressed (from `SUPPRESSION_ENTITY_TYPES`) |
| `actionTypes` | array of SuppressionTypeActionType | What actions are suppressed |
| `ownerCL` | string | From `OWNERS` |
| `labels` | array of Label | Multilingual labels (min 1) |

**`SuppressionTypeActionType` schema:**

| Field | Notes |
|---|---|
| `suppressionActionTypeCL` | Which action to suppress (from `SUPPRESSION_ACTION_TYPES`, e.g. `DEBT_REC_C`) |
| `suspendActiveInstancesSW` | **When true: creating a suppression of this type automatically pauses active BPMN process instances on the scoped entities** |

---

### Suppressions (Instances)

| Method | Path | Summary |
|---|---|---|
| GET | `/suppressions` | List/search suppressions |
| POST | `/suppressions` | **Create a suppression directly via REST** |
| GET | `/suppressions/{id}` | Get by ID |
| DELETE | `/suppressions/{id}` | **Release/remove a suppression** |

**GET query filters:** `id`, `suppressionTypeCd`, `suppressionReasonCL`, `suppressionStatusCL`, `sourceEntityTypeCL`, `sourceEntityId`, `startDate`, `endDate`, `releaseDate`, `dataLevelOfInformation` (COMPACT or FULL)

**`Suppression` schema:**

| Field | Type | Notes |
|---|---|---|
| `id` | string | Null on CREATE |
| `suppressionTypeCode` | string | References a configured SuppressionType |
| `suppressionReasonCL` | string | From `SUPPRESSION_REASONS` (e.g. VULNERABILITY, BREATHING_SPACE, DISPUTE) |
| `suppressionStatusCL` | string | From `SUPPRESSION_STATUSES` |
| `suppressionActionTypes` | array of SuppressionActionType | Which actions are suppressed |
| `suppressionScopes` | array of SuppressionScope | **What entities are scoped — can be specific liabilities or revenue types** |
| `sourceEntityId` | string | ID of the entity that triggered the suppression (e.g. a Case ID) |
| `sourceEntityTypeCL` | string | Entity type of the source |
| `startDate` | date | When the suppression becomes effective |
| `endDate` | date | Scheduled end date (enforced by `maximumNumberDays` on the type) |
| `releaseDate` | date | Actual date the suppression was released |
| `activationDateTime` | datetime | When suppression was activated |

**`SuppressionScope` schema:**

| Field | Notes |
|---|---|
| `entityCL` | Entity type being suppressed |
| `entityId` | ID of the specific entity |
| `revenueTypeCode` | Optional — scope to a specific revenue type within the entity |

**`SuppressionActionType` schema (on instance):**

| Field | Notes |
|---|---|
| `suppressionActionTypeCL` | The action being suppressed (from `SUPPRESSION_ACTION_TYPES`) |

---

## Breathing Space — pre-configured type

For DWP statutory Breathing Space (Debt Respite Scheme), configure a suppression type once:

```json
POST /suppression-types
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

Then to activate Breathing Space for a debtor:

```json
POST /suppressions
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

Solon enforces the 60-day maximum at the type level. The `suspendActiveInstancesSW` flag automatically pauses any active debt recovery BPMN processes on the scoped liability.

To release: `DELETE /suppressions/{id}`

---

## Vulnerability suppression — pattern

```json
POST /suppression-types  (one-time setup)
{
  "suppressionTypeCode": "VULNERABILITY",
  "maximumNumberDays": 0,
  "manualCreationAllowedSW": true,
  "suppressionEntityTypeCL": "LIABILITY",
  "actionTypes": [{ "suppressionActionTypeCL": "DEBT_REC_C", "suspendActiveInstancesSW": true }],
  ...
}

POST /suppressions  (per debtor event)
{
  "suppressionTypeCode": "VULNERABILITY",
  "suppressionReasonCL": "VULNERABILITY",
  "suppressionScopes": [{ "entityCL": "LIABILITY", "entityId": "..." }],
  "sourceEntityId": "case-uuid",
  "startDate": "today"
}
```

Release when vulnerability condition is resolved: `DELETE /suppressions/{id}`

---

## Common headers

| Header | Values | Purpose |
|---|---|---|
| `Authorization` | `Bearer {jwt}` | Required on all calls |
| `irm-origin-client` | `UI-CLIENT` or `FORMS` | Controls UI-only validation |
| `irm-include-labels` | `TRUE` or `FALSE` | Whether to include description labels |
| `Accept-Language` | e.g. `en-GB` | Language for returned descriptions |
