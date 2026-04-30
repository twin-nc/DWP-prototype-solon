# SOLON Context

Quick-reference for working in SOLON environments during the discovery phase.

For AI capability boundaries and what has been tried, see [ai-solon-capabilities-limitations.md](ai-solon-capabilities-limitations.md).

---

## What SOLON Is

SOLON is a tax management platform. Work on it involves three layers that must all align:

- **Configuration layer** — form types, UI schema, JSON schema, labels, endpoint wiring. Accessible via API.
- **Rule execution layer** — registered Drools rules and rule-group linkage that drive check/submit calculations. Requires admin permissions and runtime deployment.
- **Platform rendering layer** — built-in UI component behaviour that may override form-level schema settings.

Most configuration work happens at layer 1. Calculation and validation behaviour depends on layer 2. Display behaviour can be constrained by layer 3 in ways not fully exposed through config.

---

## Environment & Auth

Base URL example: `https://solontax.dev03.pit`
Frontend config path: `/revenue-management/config.js`
API base (from config): `https://<host>/api-gateway`

### Token flow (Keycloak password grant)

```http
POST /auth/realms/solon-pit/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=password&client_id=revenue-management-be&username=<USERNAME>&password=<PASSWORD>
```

Use token on API calls:
- `Authorization: Bearer <access_token>`
- `accept: application/json`
- `Content-Type: application/json` for POST/PUT

### PowerShell template

```powershell
$base = "https://<host>"
$tokenResp = Invoke-RestMethod -Method Post `
  -Uri "$base/auth/realms/solon-pit/protocol/openid-connect/token" `
  -ContentType "application/x-www-form-urlencoded" `
  -Body "grant_type=password&client_id=revenue-management-be&username=$env:SOLON_USER&password=$env:SOLON_PASS"

$token = $tokenResp.access_token
$headers = @{
  Authorization = "Bearer $token"
  accept        = "application/json"
  "Content-Type"= "application/json"
}
```

---

## Configuration Model

How the key objects relate:

- **Form Type** (`form-types/{code}`) — defines the schema template and links `ruleGroups`. Without rule groups, calculations will not run on `checkform`/`submit`.
- **Form** (`forms/{id}`) — an instance of a form type. Contains `formData` as a JSON string (not a nested object).
- **UI Entity** (`ui-entities`) — defines the UI/JSON schema structure for the form.
- **Form Rule** — a registered Drools rule (e.g. category `CALCULATION`, use `VALIDATES_INPUT_FORM`). Must be deployed in the runtime environment.
- **Rule Group** — links a form rule to a form type with a priority and optional step. The form type's `ruleGroups` array must reference this for rules to execute.
- **Translation** (`translation-management/translatable-items`) — human-readable labels for field keys.

For calculations to work end-to-end, all four must exist and be linked: Form Type → Rule Group → Form Rule → deployed Drools rule.

Reference: SOLON Tax Integration Guide (in `input docs/`), pages 430, 453, 525–530.

---

## Key API Areas

| Area | Endpoint |
|------|----------|
| UI entities | `/api-gateway/ui-configuration/ui-entities` |
| Form types | `/api-gateway/ui-configuration/form-types` |
| Forms | `/api-gateway/ui-configuration/forms` |
| Form rules | `/api-gateway/ui-configuration/form-rules` |
| Translations | `/api-gateway/translation-management/translatable-items` |
| Revenue type linkages | `/api-gateway/registration/revenue-types/{code}` |
| Currency codelists | `/api-gateway/reference-data/codelists/CURRENCIES` |

Practical notes:
- Most list endpoints enforce `size <= 100`.
- `formData` in forms is a JSON string, not a nested object.
- `checkform` returns unchanged calculated fields when rule-groups are missing or rules are not deployed.
- Some endpoints are permission-gated per user role even if the token is valid.

See `input docs/api-list.md` for a full changelog of added, modified, and deprecated endpoints across all services.

---

## Verification Checklist

After any config update:

- [ ] `GET form-type` has expected `ruleGroups`
- [ ] `GET ui-entity` has expected UI/JSON schema structure
- [ ] `GET form` returns expected `formData` keys
- [ ] `POST checkform` produces expected computed outputs
- [ ] UI rendering matches expected widget/format behaviour

---

## Reference Case: ExcWgtTax

The Excise Weight Tax form (`FormDefinition?subType=ExcWgtTax`) is the primary reference case for this discovery phase.

Key findings:
- UI/JSON schema and revenue type linkage exist but form-rule registration and rule-group linking are missing.
- All line cells are currently modelled as monetary table values, causing quantity fields (pcs/kg) to render with currency formatting.
- Backend calculations are blocked until rule groups are linked and a Drools rule is deployed.

### FormRule Investigation (2026-04-17)

Verified through live API checks:
- `GET /api-gateway/ui-configuration/form-types/ExcWgtTax` returns `ruleGroups: []`.
- `GET /api-gateway/ui-configuration/form-types/VatReturn` returns populated `ruleGroups` (9 groups), including a `VALIDATES_INPUT_FORM` calculation group.
- `GET /api-gateway/ui-configuration/form-rules` returns 81 registered rules in this environment.

Relevant calculation catalog findings:
- Existing `CALCULATION` rules include `ADDNUMS`, `VATCALC`, and several tax-domain-specific rules.
- No ExcWgt-specific rule code exists.
- No clearly generic multiplication rule was found in the accessible rule catalog; this matters because ExcWgtTax requires multiplication (`line1 * line2`, `line6 * 73.46`) and subtraction (`line3 - line4 - line5`).

Endpoint access findings for this account:
- Read access works for:
  - `/api-gateway/ui-configuration/form-rules`
  - `/api-gateway/ui-configuration/form-rules/{code}`
  - `/api-gateway/ui-configuration/form-types/{code}`
- Direct rule-group listing endpoints returned `403`:
  - `/api-gateway/ui-configuration/form-rule-groups`
  - `/api-gateway/ui-configuration/rule-groups`
  - `/api-gateway/ui-configuration/form-types/{code}/rule-groups`
  - Response header confirms `WWW-Authenticate: Bearer error=\"insufficient_scope\"` (token lacks required privileges/scopes).

Implication:
- Form rules remain the correct technical solution path for calculations, but two conditions must be met:
  - `ExcWgtTax` must be linked to at least one active `VALIDATES_INPUT_FORM` calculation rule group.
  - The linked calculation rule must support the required arithmetic (likely via custom deployed Drools rule if existing catalog is insufficient).

Latest status update (2026-04-17):
- Rule metadata wiring has now been applied:
  - custom rule `EXWGCALC` exists in `form-rules`,
  - `ExcWgtTax` has active rule group `exc_bags` with mapped IN/OUT parameters.
- Runtime still does not execute calculation behavior:
  - `checkform` with controlled inputs returns `null` for calculated outputs.
- Interpretation: metadata/configuration layer is now wired, but runtime executable rule behavior is still missing/not active in the Drools/KIE runtime.

Full fix design: [pre-existing-scripts/Reference-Case/new_forms/excise_weight_tax_fix_design.md](../pre-existing-scripts/Reference-Case/new_forms/excise_weight_tax_fix_design.md)

Active blockers for this case are tracked in [ai-solon-capabilities-limitations.md](ai-solon-capabilities-limitations.md).

---

## Reference Case: Bank List Danish Clone (BankDK)

This case documents how a separate Danish page was implemented for Bank list without replacing the English page.

### Objective

- Keep existing English page (`/page/Bank/list`) unchanged.
- Create a separate Danish-target page (`/page/BankDK/list`).
- Translate visible list labels using API-supported configuration only.

### What worked

1. **Cloned BASE UI entity into DEMO-owned page**
- Source: `code=Bank`, `screenTypeCL=SEARCH_FORM`, `ownerCL=BASE`
- Clone created through `POST /api-gateway/ui-configuration/ui-entities` with:
  - `code=BankDK`
  - `ownerCL=DEMO`
- Result: new route resolved by page code: `/revenue-management/page/BankDK/list`

2. **Remapped list keys to custom DEMO translatable keys**
- BASE keys used by the list (e.g. `code`, `labels`, `countryCL`, `bic`, `houseBank`) were not writable.
- LIST schema on `BankDK` was updated to custom keys (for example `code_dk_custom`, `labels_dk_custom`).
- Matching translatable items were created in `bank` namespace with EN+DK.

3. **Added explicit page header keys**
- LIST schema updated with:
  - `title: "title_dk_custom"`
  - `category: "category_dk_custom"`
  - `showTitle: true`
- Custom keys were inserted with DK values (for example `Banker`, `Registrering`).

4. **Added a distinct `BankDK` navigation entry**
- Created `BANKS-DK` under `ADMIN-REGISTRATION` with target `page/BankDK/list`.
- Direct binding to a newly-created custom menu key was rejected by menu validation.
- Working approach: rebind to an existing valid-but-unused `menu` key (`items.archivePaymentEvent`, DEMO-owned), then set EN/DK descriptions to a unique caption (`Banks (DK)` / `Banker (DK)`).

### Constraints discovered

1. **BASE-owned UI entities are immutable in this environment**
- Updating `ui-entities/{id}` for `ownerCL=BASE` can fail with validation (`ST-UIC-V0095`).

2. **BASE-owned translatable items are not practically overrideable via current API payload path**
- Updating BASE labels returns owner/override validation errors (`ST-COM-V0061`) and language completeness checks (`ST-COM-V0002`).
- Safe pattern: create and use DEMO-owned custom keys instead of mutating BASE keys.

3. **New translation namespace cannot be invented ad-hoc**
- Creating items with a new namespace (e.g. `bankdk`) fails unless namespace exists in `TRANSLATION_NAMESPACES` codelist.
- Reuse existing valid namespace (here: `bank`) with unique custom keys.

4. **Menu translatable key governance is stricter than translation item existence**
- A key can exist in `translation-management` (`namespace=menu`) but still fail `ui-configuration/menus` validation when used as `translatableItemKey`.
- Use `menus/key/{translatableItemKey}/exists` as the practical validity signal for menu binding.

### Language selector integration implication

The language switcher (DK/EN) does not automatically remap route `Bank -> BankDK`.

To use `BankDK` when DK is selected, frontend routing logic is required:
- DK selected + route `/page/Bank/list` -> redirect to `/page/BankDK/list`
- EN selected + route `/page/BankDK/list` -> redirect to `/page/Bank/list`

Also note:
- Row action URLs cloned from Bank can still point to `/page/Bank/{code}` unless explicitly changed.
- Full Danish flow likely needs a paired DK detail page route strategy.
