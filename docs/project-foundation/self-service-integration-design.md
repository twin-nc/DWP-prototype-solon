# Self-Service Integration Capability Design

**Date:** 2026-04-27
**Status:** Draft design baseline for self-service capability gaps
**Primary owner:** `domain/integration`
**Supporting owners:** `domain/customer`, `domain/repaymentplan`, `domain/payment`, `domain/communications`, `domain/audit`, `infrastructure/process`, `frontend/src`

## 1. Scope Position

The tender requirements include customer self-service capabilities, but the current DCMS baseline should not build a standalone public customer portal unless that scope is explicitly reopened.

The design baseline is:

- The DWP strategic self-service portal/app remains the customer-facing channel.
- DCMS provides the secured backend integration boundary, validation, workflow triggers, domain updates, audit evidence, and staff visibility for customer actions received through that channel.
- Staff-facing DCMS screens show self-service actions in customer timelines, I&E history, arrangement/payment activity, and contact history.
- Customer-facing authentication, device enrolment, app-store distribution, and public portal UX are owned outside DCMS unless a future change adds a DCMS-hosted portal.

## 2. Requirements Covered

| Requirement | Self-service interpretation in DCMS design |
|---|---|
| `CC.22` | DCMS integrates with the DWP strategic portal/app for customer I&E, arrangement, payment, and contact-detail actions. |
| `IEC.2` | DCMS accepts online/app I&E submissions from the portal/app and normalises them into the I&E engine. |
| `IEC.8` | DCMS links I&E submission to automated affordability and arrangement suggestion logic. |
| `RPF.3` | DCMS supports repayment-plan setup through interfaces, including the existing DWP self-service portal. |
| `I3PS.2` | DCMS provides real-time interaction APIs for web portal and IVR style channels where required. |
| `DIC.2` | DCMS accepts contact detail updates from self-service and routes them through customer/contact validation. |
| `DIC.8` | Self-service-derived data is available to UI, reporting, and communication channels. |
| `DIC.21` | Every self-service customer action is recorded automatically and visible to staff near real time. |
| `DW.22` | Self-service I&E and DCA repayment-to-offer submissions can trigger automated workflow. |
| `DW.65` | In-app messages are represented as a communication channel, with delivery performed through the portal/app adapter. |
| `DW.81` | Portal/app engagement events update the last-login/activity signal used by communication strategy. |

Related NFRs: `INT02`, `INT03`, `INT08`, `INT10`, `INT11`, `MOB01`-`MOB13`, `SEC`, `COM06`, and `COM07`.

## 3. Functionality Slices

| Functionality | Name | Primary owner | Supporting owners | Requirement links |
|---|---|---|---|---|
| `FNC-SS-001` | Self-service integration boundary | `domain/integration` | user, audit, platform | `CC.22`, `I3PS.2`, NFR `INT`, `SEC` |
| `FNC-SS-002` | Self-service contact-detail update intake | `domain/integration` | customer, communications, audit | `DIC.2`, `DIC.21`, `CC.22` |
| `FNC-SS-003` | Self-service I&E submission intake | `domain/integration` | repaymentplan, strategy, audit | `IEC.2`, `IEC.8`, `CC.22`, `DW.22` |
| `FNC-SS-004` | Self-service arrangement and payment action intake | `domain/integration` | repaymentplan, payment, process, audit | `RPF.3`, `CC.22`, `DW.22` |
| `FNC-SS-005` | Self-service action audit and timeline visibility | `domain/audit` | customer, frontend, reporting | `DIC.21`, `DIC.8`, `AAD`, `SoR` |
| `FNC-SS-006` | Portal/app engagement signal for communication strategy | `domain/integration` | communications, strategy, audit | `DW.81`, `DW.65`, `CC.22` |

## 4. Integration Contract Shape

The exact URI versioning can be finalised in the API contract phase. The design assumes service-to-service APIs exposed by `domain/integration`:

| Operation | Purpose | Domain handoff |
|---|---|---|
| `POST /api/v1/self-service/contact-details-updated` | Receive portal/app contact detail update. | Validate and hand off to `domain/customer`; create audit and contact-history event. |
| `POST /api/v1/self-service/ie-submissions` | Receive customer-completed I&E form. | Create `ie_trigger = SELF_SERVICE_SUBMISSION`; create/complete `ie_record` with `channel = SELF_SERVICE`; run affordability calculation. |
| `POST /api/v1/self-service/arrangement-offers` | Receive repayment offer or arrangement request. | Hand off to repayment plan domain; trigger workflow if review/agent approval is required. |
| `POST /api/v1/self-service/payment-events` | Receive payment intent, successful payment, failed payment, or cancelled payment callback. | Hand off to payment domain; update arrangement/payment state and audit evidence. |
| `POST /api/v1/self-service/engagement-events` | Receive login, last-active, message-viewed, or stale-engagement signals. | Update engagement facts used by communications and strategy. |
| `GET /api/v1/self-service/customer-summary/{externalRef}` | Optional portal read API for a minimum safe summary. | Must expose only fields approved for self-service display and must enforce data-minimisation rules. |

## 5. Security and Reliability Rules

- All inbound self-service calls must use service-to-service authentication, expected to be OAuth2 client credentials unless DWP mandates another approved trust model.
- DCMS validates the calling service identity; customer-facing identity proofing remains the responsibility of the DWP strategic portal/app.
- Every create/update command must carry an idempotency key, external action reference, correlation ID, and source channel.
- Duplicate delivery must return a deterministic no-op or previously accepted result.
- Invalid customer/account/debt references must produce stable non-retryable errors and must be recorded as rejected integration attempts.
- Retryable dependency failures must use the existing integration retry taxonomy and reconciliation evidence model.
- Sensitive payloads must be redacted from logs. Durable records store only the domain facts needed for operation, audit, and reporting.

## 6. Domain Invariants

- Self-service intake records are append-only; corrections are modelled as new actions or domain amendments, not silent overwrites.
- A self-service action must be visible on the staff customer/account timeline near real time after acceptance.
- A portal/app contact-detail update cannot bypass customer data validation, contact preference rules, or returned-mail resolution state.
- A self-service I&E submission cannot bypass affordability calculation, staleness rules, or policy gates.
- A self-service arrangement request cannot create an unauthorised arrangement if policy, affordability, vulnerability, breathing-space, or authority rules require review.
- A payment event from the portal/app is not the financial source of truth until accepted by the payment domain and reconciled against the payment provider.
- In-app communications must still pass suppression and category checks before dispatch instructions are sent to the portal/app adapter.
- Portal/app engagement events can influence contact strategy, but they must not delete or rewrite historical communication events.

## 7. Data Model Additions

Minimum data additions:

| Data object | Owner | Purpose |
|---|---|---|
| `self_service_action` | `domain/integration` | Append-only intake record for every accepted, rejected, or duplicate self-service action. |
| `self_service_action_payload_ref` | `domain/integration` or audit store | Optional reference to retained payload evidence where retention is approved; payload retention must follow data-minimisation rules. |
| `customer_channel_engagement` | `domain/communications` or `domain/customer` by final ownership decision | Last login/activity/message-viewed facts used by `DW.81` strategy rules. |
| `ie_record.channel = SELF_SERVICE` | `domain/repaymentplan` | Distinguishes portal/app I&E from agent, telephony, system, and third-party adviser channels. |
| `ie_trigger.trigger_type = SELF_SERVICE_SUBMISSION` | `domain/repaymentplan` | Captures the cause of an I&E record received from the self-service boundary. |

## 8. Workflow Impacts

- Contact-detail update creates a customer data update event and may trigger validation, returned-mail resolution, contact strategy recalculation, or staff review.
- Self-service I&E submission triggers affordability calculation and may trigger arrangement suggestion, agent review, or updated treatment route.
- Self-service repayment offer triggers the same policy checks as agent-led arrangement creation.
- Payment events trigger payment posting, reconciliation, arrangement monitoring, and breach-prevention logic.
- Engagement events update the communication strategy facts used to decide whether app/portal-first contact is still appropriate.

## 9. Acceptance Criteria

| ID | Acceptance criterion |
|---|---|
| `AC-SS-001` | Given a valid portal/app contact-detail update, when DCMS accepts it, then the customer record is updated through `domain/customer`, a self-service action is recorded, and the update is visible in the staff timeline without manual re-keying. |
| `AC-SS-002` | Given a valid portal/app I&E submission, when DCMS accepts it, then an `ie_record` with `channel = SELF_SERVICE` and an `ie_trigger` with `trigger_type = SELF_SERVICE_SUBMISSION` are created and affordability calculation runs automatically. |
| `AC-SS-003` | Given a self-service arrangement offer, when affordability and policy checks pass, then the repayment-plan domain can create the arrangement or route it to workflow review according to the same rules used by agent-led setup. |
| `AC-SS-004` | Given a payment event from the portal/app, when the payment domain accepts it, then payment state, arrangement monitoring, audit evidence, and reconciliation records are updated consistently. |
| `AC-SS-005` | Given a duplicate self-service action with the same idempotency key, when DCMS receives it again, then no duplicate domain action is created and the response is deterministic. |
| `AC-SS-006` | Given a portal/app engagement event, when DCMS accepts it, then communication strategy can use the updated last-active signal for `DW.81` without rewriting prior communication history. |

## 10. Open Questions

| ID | Question | Owner | Impact |
|---|---|---|---|
| `DDE-OQ-SS-01` | What is the authoritative DWP strategic portal/app API ownership and onboarding model? | DWP Client + Integration Lead | Confirms auth, routing, certificates, and environment setup. |
| `DDE-OQ-SS-02` | Which customer, account, and debt references will the portal/app send, and which are safe to expose back? | DWP Client + Data Architect | Controls matching, privacy, and read API design. |
| `DDE-OQ-SS-03` | Should portal/app I&E submission be synchronous validation or asynchronous accept-and-review? | Product Owner + Debt Domain Expert | Affects API response semantics and customer-facing error design. |
| `DDE-OQ-SS-04` | Are in-app messages rendered and stored by DCMS, the portal/app, or a shared template service? | Communications Owner + Integration Lead | Affects `DW.65`, template ownership, and evidence retention. |
| `DDE-OQ-SS-05` | Which payment actions are portal/app-owned versus payment-provider-owned? | Payment Owner + Integration Lead | Affects payment source-of-truth and reconciliation design. |
| `DDE-OQ-SS-06` | What customer-facing error codes and support journeys must be returned to the portal/app? | Product Owner + Service Design | Affects API contract, support handoff, and audit wording. |

## 11. Traceability Actions

1. Add `FNC-SS-001` to `FNC-SS-006` to the canonical functionality catalogue and `trace-map.yaml`.
2. Link `CC.22`, `IEC.2`, `IEC.8`, `RPF.3`, `I3PS.2`, `DIC.2`, `DIC.8`, `DIC.21`, `DW.22`, `DW.65`, and `DW.81` to the relevant `FNC-SS-*` slices.
3. Add API contract drafts for the five inbound self-service endpoints before implementation.
4. Add integration tests for idempotency, invalid references, duplicate delivery, audit creation, and process-event firing.
