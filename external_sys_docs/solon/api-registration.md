# Registration API — Reference
**Version:** 2.1.0  
**Base path:** `/registration` (assumed; routed via Solon API Gateway)  
**Auth:** Bearer JWT from Keycloak on every request

> Solon's registration service is the master record for all parties (debtors, organisations), party roles (the capacity in which a party participates — e.g. TAXPAYER, DEBTOR, CASEWORKER), IRM users (caseworkers and agents), organisational units, obligations, revenue type registrations, and assets. Every `partyRoleId` referenced across all other Solon APIs resolves to a record here.

---

## Core Concepts

| Concept | Description |
|---|---|
| **Party** | The person or organisation — holds name, address, identifiers, bank accounts, GDPR consent dates |
| **PartyRole** | The capacity in which a Party participates in the system (e.g. TAXPAYER, DEBTOR). A single Party may have multiple PartyRoles |
| **IRM User** | A special PartyRole for system users — caseworkers, supervisors, agents. Links to a Party record |
| **OrganisationalUnit** | A team or branch — groups IRM Users for queue and routing purposes |
| **Obligation / ObligationAction** | A tax obligation record and its associated actions (filing periods, due dates) |
| **RevenueTypeRegistration** | Links a PartyRole to a revenue type (e.g. INCOME_TAX, VAT) with lifecycle status |

---

## Endpoints

### Parties

| Method | Path | Summary |
|---|---|---|
| GET | `/parties` | Search parties (filters: `ids`, `partyRoleId`, `identifierType`, `identifierValue`, name, address fields) |
| POST | `/parties` | Create a new party |
| GET | `/parties/{id}` | Get party by ID |
| PUT | `/parties/{id}` | Update party |
| DELETE | `/parties/{id}` | Delete party |
| GET | `/parties/{id}/exists` | Check existence |
| GET | `/parties/single-party-role-view` | Compact party + party role summary in one call |
| POST | `/parties/quickcreate` | Quick-create a party |
| GET | `/parties/{id}/addresses/dropdown` | Addresses for this party |
| GET | `/parties/{id}/bankAccounts/dropdown` | Bank accounts for this party |
| GET | `/parties/{id}/contacts/dropdown` | Contacts for this party |
| GET | `/parties/{id}/identifiers/dropdown/{partyRoleTypeCode}` | Identifiers for a specific party role type |
| GET | `/parties/{id}/gdpr-consent-dates` | GDPR consent start/end dates |

**`Party` schema:**

| Field | Type | Notes |
|---|---|---|
| `id` | string | **Null on CREATE** |
| `partyTypeCode` | string (max 10) | Cannot change on UPDATE |
| `legalEntity` | boolean | `true` = organisation; `false` = individual person |
| `individual` | Individual | **Required if `legalEntity=false`** — birthDate, birthPlace, genderCL, countryCL, deathDate |
| `names` | array (min 1) | Party name records |
| `identifiers` | array (min 1) | Party identifiers (e.g. NI number, UTR) |
| `partyAddresses` | array | Address records |
| `contacts` | array | Contact details (phone, email) |
| `bankAccounts` | array | Bank account records |
| `classifications` | array | Party classifications |
| `partyDataArea` | string | Free JSON blob |
| `languageCL` | string | From `LANGUAGES` |
| `group` | boolean | Whether this is a group party |
| `gdprConsentStartDate` | date | **Read-only** |
| `gdprConsentEndDate` | date | **Read-only** |
| `identificationLabel` | string | **Read-only** — system-generated display label |

---

### Party Roles

| Method | Path | Summary |
|---|---|---|
| GET | `/party-roles` | Search party roles (filters: `ids`, `partyId`, `partyRoleTypeCode`) |
| POST | `/party-roles` | Create a party role |
| GET | `/party-roles/{id}` | Get by ID |
| PUT | `/party-roles/{id}` | Update |
| DELETE | `/party-roles/{id}` | Delete |
| GET | `/party-roles/{id}/exists` | Check existence |
| GET | `/party-roles/dropdown` | Dropdown list |
| GET | `/party-roles/{id}/eligible-addresses/dropdown` | Eligible addresses for this role |
| GET | `/party-roles/{id}/eligible-bank-accounts` | Eligible bank accounts |
| GET | `/party-roles/{id}/eligible-bank-accounts/dropdown` | Dropdown of bank accounts |
| GET | `/party-roles/{id}/eligible-contacts` | Eligible contacts |
| GET | `/party-roles/{id}/party-role-relations` | Relationships to other party roles |
| GET | `/party-roles/{id}/registered-revenue-types/dropdown` | Revenue types this party role is registered for |

**`PartyRole` schema:**

| Field | Type | Notes |
|---|---|---|
| `id` | string | **Null on CREATE** |
| `party` | Identification | **Required** — links to the parent Party |
| `partyRoleTypeCode` | string (max 10) | **Required** — role type (e.g. TAXPAYER, DEBTOR) |
| `partyRoleTypeCategoryCL` | string | From `PARTY_ROLE_TYPE_CATEGORIES` |
| `eligibleIdentifiers` | array | **Required** — must include a primary identifier |
| `eligibleAddresses` | array | **Required** |
| `eligibleBankAccounts` | array | Bank accounts visible for this role |
| `eligibleContacts` | array | **Required** — contacts visible for this role |
| `states` | array | Lifecycle states of this party role |
| `dataArea` | string | Free JSON blob |
| `sourceCL` | string | Data source (e.g. `NATIONAL_TAX_REGISTRY`) |
| `identificationLabel` | string | **Read-only** — system-generated display label |

> Every `partyRoleId` used in Case Management, Human Task Management, Suppression Management, Risk Analysis, and Taxpayer Accounting resolves to a record here.

---

### Users (IRM Users — Caseworkers and Agents)

| Method | Path | Summary |
|---|---|---|
| GET | `/users` | Search IRM users (filters: `ids`, `partyId`, `partyName`, organisational unit, state) |
| POST | `/users/quick-create` | Quick-create a user |
| GET | `/users/{id}` | Get by ID |
| PUT | `/users/{id}` | Update |
| GET | `/users/{id}/exists` | Check existence |
| GET | `/users/list` | List users |
| GET | `/users/dropdown` | Dropdown list |
| GET | `/users/{id}/eligible-contacts` | Eligible contacts for this user |
| GET | `/users/party-role-id` | Find user by party role ID |

**`User` schema:**

| Field | Type | Notes |
|---|---|---|
| `id` | string | **Null on CREATE** |
| `party` | Identification | Links to the User's Party record |
| `partyRoleTypeCode` | string (max 10) | **Required** |
| `partyRoleTypeCategoryCL` | string | From `PARTY_ROLE_TYPE_CATEGORIES` |
| `eligibleAddresses` | array | **Required** |
| `eligibleContacts` | array | **Required** |
| `eligibleIdentifiers` | array | **Required** |
| `states` | array (min 1) | **Required** — user availability and lifecycle states |
| `dataArea` | string | Free JSON blob |
| `identificationLabel` | string | **Read-only** |

**`UserCreation` schema (for quick-create):**

| Field | Notes |
|---|---|
| `firstName`, `lastName` | User's name |
| `email` | Email address |
| `phoneNumber` | Phone number |
| `countryCL` | From `COUNTRIES` (min 1) |

---

### Organisational Units

| Method | Path | Summary |
|---|---|---|
| GET | `/organizational-unit/v2` | Search organisational units (paginated) |
| GET | `/organizational-unit/v2/{id}` | Get by ID |
| GET | `/organizational-unit/{id}` | Get by ID (v1) |
| GET | `/organizational-unit/first-level-parent` | Top-level parent units |
| GET | `/organizational-unit/{id}/exists` | Check existence |
| GET | `/organizational-unit/{id}/addresses/dropdown` | Addresses for this unit |
| GET | `/organizational-unit-member` | List members of organisational units |
| GET | `/organizational-unit-taxpayer` | Taxpayers linked to an organisational unit |

**`OrganizationalUnit` schema** shares the Party structure — validation requires `partyTypeCode = ORGANIZATIONAL_UNIT`, non-empty names, states, and identifiers.

---

### Obligations and Obligation Actions

| Method | Path | Summary |
|---|---|---|
| GET | `/obligations` | Search obligations |
| POST | `/obligations` | Create an obligation |
| GET | `/obligations/{id}` | Get by ID |
| PUT | `/obligations/{id}` | Update |
| POST | `/obligations/quick-create` | Quick-create an obligation |
| PUT | `/obligations/{id}/cancel` | Cancel |
| GET | `/obligation-actions` | Search obligation actions |
| GET | `/obligation-actions/details` | With related details |
| GET | `/obligation-actions/primary-filing` | Primary filing period actions |
| GET | `/obligation-actions/{id}/bill-segment-types/dropdown` | Bill segment types for an action |
| PUT | `/obligations/{id}/actions/{actionId}/cancel` | Cancel a specific obligation action |

Obligations define the tax duty — each obligation action represents a filing period with a due date. Bill segment types are derived from obligation actions, linking tax obligations to the financial records in Taxpayer Accounting.

---

### Revenue Type Registrations

| Method | Path | Summary |
|---|---|---|
| GET | `/revenue-type-registrations` | Search registrations |
| POST | `/revenue-type-registrations` | Create |
| GET | `/revenue-type-registrations/{id}` | Get by ID |
| PUT | `/revenue-type-registrations/{id}` | Update |
| PUT | `/revenue-type-registrations/{id}/activate` | Activate |
| PUT | `/revenue-type-registrations/{id}/cancel` | Cancel |
| PUT | `/revenue-type-registrations/{id}/deactivate` | Deactivate |
| GET | `/revenue-types` | List revenue types |
| GET | `/revenue-types/{code}` | Get revenue type by code |

Revenue type registrations link a PartyRole to a specific revenue type (e.g. INCOME_TAX, VAT) with a status lifecycle. Active registrations determine which tax obligations a debtor currently has.

---

### Other Resource Groups

| Group | Key endpoints | Purpose |
|---|---|---|
| Assets | `GET/POST /assets`, `GET /assets/{id}` | Property and asset records linked to parties |
| Claimants | `GET /claimants`, `GET /claimants/related-to-party-role/{partyRoleId}` | Claimants linked to a party role |
| Bank Accounts | `GET /bank-accounts`, `GET /bank-accounts/{id}` | Bank account records |
| Bank Account Formats | `GET /bank-account-formats`, `GET /bank-account-formats/{code}` | Format configuration (e.g. IBAN, BACS) |
| Banks | `GET /banks`, `GET /banks/available-house-banks` | Bank master data |
| Party Types | `GET /party-types`, `GET /party-types/{code}` | Type configuration (INDIVIDUAL, ORGANISATION, ORGANIZATIONAL_UNIT) |
| Rights | `GET /rights`, `GET /user-rights` | Access rights configuration |
| Resource Calendars | `GET /resource-calendars`, with exception calendars | Business day / working day calendars |
| Reconciliation Periods | `GET /reconciliation-periods` | Tax period configuration |
| Portal Users | `POST /portal-users/quick-create` | Create self-service portal user accounts |
| Addresses | `GET/POST /addresses`, `GET /address-standards` | Address records and format standards |

---

## Architectural significance for the additional layer

### Agent Desktop — debtor lookup
Every case in Solon carries a `partyRoleId`. The Agent Desktop resolves this to displayable identity by calling:
```
GET /registration/parties/single-party-role-view?partyRoleId={id}
```
This returns the Party (name, address, identifiers) and PartyRole (type, states) in one call. Alternatively:
```
GET /registration/party-roles/{id}          — role details, eligible identifiers, addresses
GET /registration/parties/{partyId}         — full party record including individual/org details
```

### Queue Distribution — agent availability
The Queue Distribution Service reads IRM User records to populate routing decisions:
```
GET /registration/users?states=AVAILABLE&organizationalUnitId={id}
GET /registration/organizational-unit/v2?{team-filter}
```
Worker state (AVAILABLE, BUSY, OFFLINE) is held in the `states` array on the User record.

### Revenue type context for debt collection
Before creating a debt collection case, the system can confirm which revenue types a debtor has active registrations for:
```
GET /registration/revenue-type-registrations?partyRoleId={id}&status=ACTIVE
GET /registration/party-roles/{id}/registered-revenue-types/dropdown
```

### Obligation actions linking to bill segments
The `GET /obligation-actions/{id}/bill-segment-types/dropdown` endpoint bridges the Registration API to Taxpayer Accounting — it shows which bill segment types apply for a given obligation action period, allowing the additional layer to understand the tax debt's structural origin.

### Foundations Configurator — user management
The Foundations Configurator manages IRM user accounts, organisational unit membership, and party type configuration via:
```
POST /registration/users/quick-create
GET /registration/organizational-unit/v2
GET /registration/party-types
```

### Third-Party Collection Handover — claimant and revenue type context
When handing a case to a third-party collection provider, the system reads:
```
GET /registration/revenue-type-registrations/{id}   — to confirm the revenue type registration ID
GET /registration/claimants/related-to-party-role/{partyRoleId}
```

---

## Common headers

| Header | Values | Purpose |
|---|---|---|
| `Authorization` | `Bearer {jwt}` | Keycloak JWT — required |
| `irm-origin-client` | `UI-CLIENT` or `FORMS` | Origin indicator |
| `irm-include-labels` | `TRUE` or `FALSE` | Whether to include labels |
| `Accept-Language` | e.g. `en-GB` | Language for returned descriptions |
