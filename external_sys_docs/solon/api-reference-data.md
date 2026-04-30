# Reference Data Microservice API — Reference
**Version:** 2.1.0  
**Base path:** `/reference-data` (assumed; routed via Solon API Gateway)  
**Auth:** Bearer JWT from Keycloak on every request

> This service manages all code lists used across every Solon Tax microservice. Every field suffixed `CL` in the Case, Human Task, Suppression, and Process Management APIs references a code maintained here. The Foundations Configurator in the additional layer reads and manages these code lists.

---

## Endpoints

### Code Lists

| Method | Path | Summary |
|---|---|---|
| GET | `/categories/{categoryCode}/codelists` | Get all codelists for a given category (with all items, regardless of expiration) |
| GET | `/codelists` | Search / list code lists (paginated) |
| POST | `/codelists` | Create a new code list |
| GET | `/codelists/{id}` | Get by ID |
| PUT | `/codelists/{id}` | Update |
| DELETE | `/codelists/{id}` | Delete |

### Code List Items

| Method | Path | Summary |
|---|---|---|
| GET | `/codelist-items` | Search items (paginated, with filters) |
| POST | `/codelist-items` | Create a new code list item |
| GET | `/codelist-items/{id}` | Get by ID |
| PUT | `/codelist-items/{id}` | Update |
| DELETE | `/codelist-items/{id}` | Delete |

**GET `/codelist-items` query filters:** `codelistKey`, `description`, `itemCode`, `referenceDate`, `attributeKey`, `attributeValue` (wildcards supported with `*`)

### Code List Item Relationships

| Method | Path | Summary |
|---|---|---|
| GET | `/codelist-item-relationships` | Search relationships (paginated) |
| POST | `/codelist-item-relationships` | Add a relationship |
| PUT | `/codelist-item-relationships/{id}` | Update a relationship |
| DELETE | `/codelist-item-relationships/{id}` | Delete a relationship |

**GET filters:** `ids[]`, `relatedCodeListItemId`, `codeListItemId`, `relationshipTypeCL`, `codeListItemCode`, `codeListItemDescription`, `attributeKey`, `attributeValue`, `levelOfInformationEnum` (COMPACT or FULL)

### Reference Data Metadata

| Method | Path | Summary |
|---|---|---|
| GET | `/refdata-metadata` | Search metadata definitions |
| POST | `/refdata-metadata` | Create metadata definition |
| GET | `/refdata-metadata/{id}` | Get by ID |
| PUT | `/refdata-metadata/{id}` | Update |
| DELETE | `/refdata-metadata/{id}` | Delete |

### Cache Management

| Method | Path | Summary |
|---|---|---|
| DELETE | `/caches/clear-all` | Invalidate all reference data caches |

---

## Key schemas

**`CodeListBase`:**

| Field | Type | Notes |
|---|---|---|
| `id` | integer | System-generated; read-only |
| `key` | string | Unique codelist key (e.g. `SUPPRESSION_REASONS`, `OWNERS`) |
| `ownerCL` | string | From `OWNERS` codelist |
| `labels` | array of RefDataLabel | Multilingual descriptions |
| `categoryCodes` | array of string | Which categories this list belongs to |
| `items` | array of CodeListItemBase | Items in this codelist; read-only on the list object |
| `refDataMetadata` | array | Metadata schema definitions for item attributes |

**`CodeListItemBase`:**

| Field | Type | Notes |
|---|---|---|
| `id` | integer | System-generated; read-only |
| `code` | string | The item code (e.g. `BREATHING_SPACE`, `DEBT_REC_C`) |
| `codelistId` | integer | Parent codelist |
| `ownerCL` | string | From `OWNERS` |
| `labels` | array of RefDataLabel | Multilingual descriptions |
| `validFrom` | date | When this item becomes effective |
| `validTo` | date | When this item expires (optional) |
| `sorting` | integer (1–max) | Display order |
| `sourceCL` | string | Source of the data (e.g. `NATIONAL_TAX_REGISTRY`) |
| `attributes` | array of RefDataAttributeBase | Key-value metadata on the item |

**`CodeListItemRelationship`:**

| Field | Notes |
|---|---|
| `id` | Relationship ID |
| `ownerCL` | From `OWNERS` |
| `relationshipTypeCL` | `WITHIN` (within same codelist) or `CROSS` (across codelists) |
| `codeListItemDetails` | The child item in the hierarchical relationship |
| `relatedCodeListItemDetails` | The parent item |

**`RefDataLabel`:**

| Field | Notes |
|---|---|
| `languageCL` | From `LANGUAGES` codelist |
| `description` | Text label (max 1000 chars) |

**`RefdataMetadataBase`:**

| Field | Notes |
|---|---|
| `attributeKey` | The key name for a custom attribute |
| `attributeValueType` | Expected value type |
| `description` | Human-readable explanation |
| `ownerCL` | From `OWNERS` |

---

## Architectural significance

All `*CL` fields across Solon Tax APIs are validated against codes in this service. Examples:

| CL field (in other APIs) | Codelist key |
|---|---|
| `suppressionReasonCL` | `SUPPRESSION_REASONS` |
| `suppressionActionTypeCL` | `SUPPRESSION_ACTION_TYPES` |
| `ownerCL` | `OWNERS` |
| `humanTaskPriorityCL` | `HUMAN_TASK_PRIORITIES` |
| `humanTaskAssignBehaviorCL` | `HUMAN_TASK_ASSIGN_BEHAVIORS` |
| `caseStatusCL` | `CASE_STATUSES` |
| `maximumNumberDaysTypeCL` | `MAXIMUM_NUMBER_DAYS_TYPES` |
| `languageCL` | `LANGUAGES` |

**Time-bounded items:** `validFrom`/`validTo` on each item means code lists can change over time — items expire and new ones become effective. The `referenceDate` query filter on `GET /codelist-items` retrieves the valid set at any point in time.

**Hierarchy via relationships:** `CROSS`-type relationships link items across codelists (e.g. linking a revenue type to an allowed suppression action). `WITHIN` relationships express ordering or grouping within a single list.

---

## Usage in the additional layer

The **Foundations Configurator** uses this API to:
- Populate dropdowns for all `*CL` fields in configuration UIs — `GET /codelist-items?codelistKey=SUPPRESSION_REASONS`
- Add DWP-specific codes to existing codelists — `POST /codelist-items`
- Browse codelists by category — `GET /categories/{categoryCode}/codelists`
- Manage cross-codelist relationships (e.g. revenue type → suppression action mapping)

All other additional layer services read codelists indirectly (the values are validated by Solon when submitted in API requests). Only the Foundations Configurator needs to call this API directly.

---

## Common headers

| Header | Values | Purpose |
|---|---|---|
| `Authorization` | `Bearer {jwt}` | Keycloak JWT — required |
| `irm-origin-client` | `UI-CLIENT` or `FORMS` | Origin indicator |
| `irm-include-labels` | `TRUE` or `FALSE` | Whether to include labels in responses |
| `Accept-Language` | e.g. `en-GB` | Language for returned descriptions |
