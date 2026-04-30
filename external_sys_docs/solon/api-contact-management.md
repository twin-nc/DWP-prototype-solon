# Contact Management API — Reference
**Version:** 2.1.0  
**Base path:** `/contact-management` (assumed; routed via Solon API Gateway)  
**Auth:** Bearer JWT from Keycloak on every request

> Solon's contact management service owns the full contact lifecycle — creating, generating, tracking, and managing correspondence records across all channels (letter, email, SMS, dialler). The additional layer's Contact Orchestration Service creates contacts here; Solon manages the status transitions and document generation pipeline.

---

## Endpoints

### Communication Channels (Configuration)

| Method | Path | Summary |
|---|---|---|
| GET | `/communication-channels` | Search channels |
| POST | `/communication-channels` | Create a channel |
| GET | `/communication-channels/{code}` | Get by code |
| PUT | `/communication-channels/{code}` | Update |
| DELETE | `/communication-channels/{code}` | Delete |
| GET | `/communication-channels/dropdown` | Dropdown list (code + label) |

**`CommunicationChannel` schema:**

| Field | Type | Notes |
|---|---|---|
| `code` | string | Unique identifier |
| `directionCL` | string | From `COMMUNICATION_CHANNEL_DIRECTIONS` (e.g. OUTBOUND, INBOUND) |
| `typeCL` | string | From `COMMUNICATION_CHANNEL_TYPES` (e.g. EMAIL, SMS, LETTER, DIALLER) |
| `serverParameters` | object | Channel-specific connection config (SMTP host, gateway URL, etc.) |
| `labels` | array | Localised descriptions |

---

### Contact Types (Configuration)

| Method | Path | Summary |
|---|---|---|
| GET | `/contact-types` | Search contact types |
| POST | `/contact-types` | Create a contact type |
| GET | `/contact-types/{code}` | Get by code |
| PUT | `/contact-types/{code}` | Update |
| DELETE | `/contact-types/{code}` | Delete |
| GET | `/contact-types/dropdown` | Dropdown list |

**`ContactType` schema:**

| Field | Type | Notes |
|---|---|---|
| `code` | string | Unique identifier |
| `activeSw` | boolean | Whether the type is active |
| `contactCategoryCL` | string | Category codelist |
| `contactDirectionCL` | string | OUTBOUND / INBOUND |
| `contactMediumCL` | string | LETTER / EMAIL / SMS / DIALLER |
| `contactCanBeDisputedSw` | boolean | Whether a contact of this type can be disputed |
| `contactEditingAllowedSw` | boolean | Whether the contact can be edited after creation |
| `requiresSigning` | boolean | Whether document requires a digital signature |
| `noticeDateCalculationMethodCL` | string | Method used to calculate the notice date |
| `noticeDateCalculationNumberOfDays` | integer | Number of days for notice date calculation |
| `ownerCL` | string | From `OWNERS` |
| `supportedCommunicationChannels` | array | Channels this type can be sent via |
| `parameters` | array | Template parameters required for this contact type |

**`ContactTypeSupportedCommunicationChannel` sub-schema:**

| Field | Notes |
|---|---|
| `communicationChannelCode` | References a configured CommunicationChannel |
| `defaultSw` | Whether this is the default channel for the contact type |
| `contentParts` | Array of ContentPart — the document/attachment components for this channel |

**`ContentPart` sub-schema:**

| Field | Notes |
|---|---|
| `nameCl` | From `CONTENT_PART_NAMES` — identifies this part (e.g. MAIN_LETTER, ATTACHMENT_1) |
| `template` | Jasper Reports template code — used to render the document |
| `contentTypeCl` | MIME type / content type codelist |
| `applyUserSelectionSW` | Whether the caseworker selects this part at runtime |
| `filename` | Filename for attachment delivery |
| `includeInEmbeddedZipFolderSW` | Whether to bundle in a ZIP for bulk delivery |

> `ContentPart.template` references Jasper Reports templates — this confirms the Jasper Reports component in the Solon runtime.

---

### Contacts (Instances)

| Method | Path | Summary |
|---|---|---|
| GET | `/contacts` | Search contacts (paginated) |
| POST | `/contacts` | Create a new contact |
| GET | `/contacts/{id}` | Get by ID |
| PUT | `/contacts/{id}` | Update a contact |
| DELETE | `/contacts/{id}` | Delete a contact |
| POST | `/contacts/{id}/generate` | Generate/send the contact (triggers document rendering and dispatch) |

**`Contact` schema:**

| Field | Type | Notes |
|---|---|---|
| `id` | string | **Null on CREATE** |
| `statusCL` | string | **System-managed** — set to PENDING on creation; transitions are automatic |
| `communicationChannelCode` | string | The channel to send via |
| `recipientPartyRole` | object | **Mandatory** — PartyRole identification (`id`, UUID 36 chars) |
| `contactTypeDetails` | object | **Mandatory** — must include `contactTypeCode` |
| `parameters` | array | Runtime parameter values (e.g. debtor name, amount owed) |
| `relatedEntities` | array | Entities linked to this contact (case, liability, etc.) |
| `dataArea` | string | Free JSON blob — custom metadata |
| `noticeDate` | date | **Read-only** — computed by Solon from `noticeDateCalculationNumberOfDays` |
| `overrideNoticeDate` | date | Optional manual override of the computed notice date |
| `sentDate` | date | **Read-only** — set when contact is dispatched |
| `contactReferenceNumber` | string | Human-readable reference number |

**Validation rules:**
- `id` must be null on CREATE
- `recipientPartyRole` is mandatory — contact must be linked to a party role
- `statusCL` is system-managed — the additional layer cannot set it directly (it will be PENDING on creation)

**`GenerateContactDetails` schema (for `POST /contacts/{id}/generate`):**

| Field | Notes |
|---|---|
| `changeReasonCL` | Reason for generating/regenerating |
| `changeComment` | Free-text comment |
| `contactFileOutputCL` | From `CONTACT_FILE_OUTPUTS` — output format (PDF, PRINT, EMAIL, etc.) |

**`AttachmentDetails` schema:**

| Field | Notes |
|---|---|
| `dmsId` | Document Management System UUID — reference to stored document |
| `entityCL` | Entity type codelist |
| `entityId` | Entity ID |
| `fileName` | Document filename |
| `sizeInBytes` | File size |

> `AttachmentDetails.dmsId` references a separate Document Management System (DMS) — confirms the Document Management API is part of the Solon ecosystem and relevant for the additional layer.

---

### Contact Templates (Configuration)

| Method | Path | Summary |
|---|---|---|
| GET | `/contact-templates` | Search templates |
| POST | `/contact-templates` | Create a template |
| GET | `/contact-templates/{code}` | Get by code |
| PUT | `/contact-templates/{code}` | Update |
| DELETE | `/contact-templates/{code}` | Delete |

Contact templates are the Jasper template configurations linked to ContactType content parts. They define the document structure and variable mappings for each communication type.

---

## Architectural significance for the additional layer

### Creating a contact (confirmed)

The additional layer's **Contact Orchestration Service** creates contacts via:
```
POST /contact-management/contacts
```

The `statusCL` is set to PENDING by Solon on creation. The additional layer stores the returned contact ID for tracking. Generation/dispatch is triggered separately via `POST /contacts/{id}/generate`.

### Channel configuration
`GET /communication-channels` is used by the Foundations Configurator to configure available communication channels and their server parameters. This drives which channels appear in the contact type setup UI.

### Template management
`GET/PUT /contact-templates` is used by the Template Management Service in the additional layer to configure Jasper template mappings. The `ContentPart.template` field links contact types to specific Jasper templates.

### Contact suppression vs contact management
These are separate concerns:
- **Contact Suppression** = `POST /suppression-management/suppressions` — prevents contacts from being created (vulnerability/breathing space)
- **Contact Management** = `POST /contact-management/contacts` — creates and sends the actual contact record when suppression is not active

### BPMN-initiated contacts vs additional layer contacts
The BPMN engine can trigger contacts internally via `irm.bpmn-engine.create-contact` (its private Kafka command). The additional layer creates contacts directly via REST — `POST /contact-management/contacts` — for contacts triggered by non-BPMN flows (e.g. agent-initiated letters, dialler campaigns).

---

## Common headers

| Header | Values | Purpose |
|---|---|---|
| `Authorization` | `Bearer {jwt}` | Keycloak JWT — required |
| `irm-origin-client` | `UI-CLIENT` or `FORMS` | Origin indicator |
| `irm-include-labels` | `TRUE` or `FALSE` | Whether to include labels |
| `Accept-Language` | e.g. `en-GB` | Language for returned descriptions |
