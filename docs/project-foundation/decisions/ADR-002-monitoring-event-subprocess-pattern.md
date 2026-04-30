# ADR-002: Monitoring and Event Handling via BPMN Event Subprocesses

## Status

Accepted

## Date

2026-04-22

## Context

The workflow diagram shows a "Monitoring / Event Handling" pool that runs across all treatment paths. It must:

- Continuously monitor repayment behaviour
- Detect change of circumstances and re-segment the debtor
- Terminate the current treatment process and start a new one when segment changes (e.g. benefit cessation → off-benefit recovery)
- Handle customer-level regulatory events (breathing space, insolvency, death) that suspend or halt all active process instances for a customer

Three architectural options were evaluated:

1. **Separate parallel Flowable process instance per debt** — one treatment instance + one monitoring instance running concurrently
2. **BPMN event subprocesses within each treatment process** — interrupting and non-interrupting event subprocesses at root scope
3. **Application-layer Spring service manipulating Flowable externally** — scheduled jobs and event listeners outside BPMN

Requirements driving this decision:

- DW.10 — automatically re-segment, classify accounts, trigger channels
- DW.18 — eliminate blackholes (accounts lost in workflow)
- DW.35 — remove account from workflow when scenarios recorded, include in appropriate workflow
- DW.37 — manage multiple accounts in multiple statuses simultaneously
- DW.38 — visual strategy in easy-to-follow format for non-technical users
- DW.40 — reuse sets of rules and components
- DW.42 — trace outcome back through decisioning logic
- DW.45 — flag deceased, vulnerable, insolvency, fraud, breathing space
- DW.48 — move account back to previous position when customer situation changes
- DW.49 — account can enter, exit, pause within a workflow/treatment
- DW.51 — pause communications based on account or customer circumstances

## Decision

**Option 2 chosen: BPMN event subprocesses within each treatment process, with application-layer event sources.**

Each treatment process definition includes the following event subprocesses at root scope:

| Event subprocess | Trigger | Interrupting? | Correlation key | Action |
|---|---|---|---|---|
| `breathing-space-received` | Message: `BREATHING_SPACE_START` | Yes | `customerId` | Suspends all comms, moves to suspended state, waits for `BREATHING_SPACE_END` |
| `benefit-ceased` | Message: `BENEFIT_CEASED` | Yes | `debtAccountId` | Terminates current treatment, fires `RESEGMENT` signal |
| `change-of-circumstances` | Message: `CIRCUMSTANCE_CHANGE` | No | `debtAccountId` | Triggers re-evaluation task; if segment changes fires `RESEGMENT` signal |
| `insolvency-registered` | Message: `INSOLVENCY_REGISTERED` | Yes | `customerId` | Halts treatment, moves to insolvency-handling state |
| `death-registered` | Message: `DEATH_REGISTERED` | Yes | `customerId` | Halts all activity, creates estate-handling task |
| `payment-missed` | Timer: configurable N days after expected payment | No | `debtAccountId` | Creates breach task, evaluates breach tolerance rules |
| `supervisor-override` | Message: `SUPERVISOR_OVERRIDE` | No | `debtAccountId` | Moves process to specified step per DW.11 |

The `RESEGMENT` signal is handled by a signal catch event that calls the `strategy` module segmentation service, gets a new segment, and starts a new treatment process instance with the correct definition key. The current instance terminates cleanly.

**Event sources are application-layer.** Inbound API calls (benefit cessation, breathing space registration) or scheduled checks (payment missed N days) fire messages/signals via `RuntimeService`. Detection is application-layer; response is entirely within the BPMN model.

**Two port interfaces are defined as the only permitted entry points into the process engine from the rest of the application:**

- **`ProcessEventPort`** — the only way any module fires an event into a running process instance. Wraps `RuntimeService.messageEventReceived()` and `RuntimeService.signalEventReceived()`. Direct `RuntimeService` calls outside the `process` module are prohibited.

- **`ProcessStartPort`** — the only way a treatment process is started or resegmentation triggers a new process. Called by case initiation flow and by the resegment event subprocess handler.

Event subprocesses are defined once as reusable BPMN fragments and referenced across all treatment process definitions (satisfies DW.40).

## Consequences

- Every treatment process BPMN definition must include the standard event subprocesses at root scope before deployment
- No domain module (`account`, `communications`, `integration`) may call Flowable APIs directly — all process interaction goes through `ProcessEventPort` or `ProcessStartPort`
- A message/signal name catalogue must be maintained as a design artefact and treated as a versioned contract (additions are non-breaking; renames and removals are breaking changes)
- Breathing space and insolvency are NOT account flags — they are process-engine states modelled entirely in BPMN. The `account` module records the *fact* (date registered, regulatory reference) for audit; the *behavioural effect* is owned by the process engine
- Re-segmentation is a process engine concern: the monitoring event subprocess calls `strategy` for the new segment and calls `ProcessStartPort` for the new process. It does not write a new status to `account`
- BPMN model changes to event subprocesses are Class A changes — they alter debt collection behaviour for live debtors and require Domain Expert + Solution Architect sign-off per WAYS-OF-WORKING.md

## Alternatives Rejected

### Option 1 — Separate parallel process instance per debt

Two process instances (treatment + monitoring) running concurrently per debt.

**Rejected because:**

- Coordination between instances is fragile: if treatment terminates unexpectedly, monitoring instance is orphaned (violates DW.18)
- Termination must be explicitly coordinated in both directions, adding complexity without benefit
- Harder to audit: two process histories per debt instead of one

### Option 3 — Application-layer Spring service manipulating Flowable

Scheduled jobs and event listeners outside BPMN that call `RuntimeService` directly.

**Rejected because:**

- Lifecycle state split between Flowable and application logic defeats the process engine as authoritative record (violates ADR-001)
- Event handling logic is invisible to non-technical users (violates DW.38)
- Harder to trace outcomes through decisioning (violates DW.42)
- Cannot be version-controlled or champion/challenger tested as BPMN (violates DW.9, DW.39)

## Implementation Notes

1. **Before any treatment BPMN is authored:** the message/signal catalogue below must be finalised and published as a versioned design artefact.

2. **Message/signal catalogue (v1):**
   - **Messages (correlation required):** `BREATHING_SPACE_START`, `BREATHING_SPACE_END`, `BENEFIT_CEASED`, `CIRCUMSTANCE_CHANGE`, `INSOLVENCY_REGISTERED`, `DEATH_REGISTERED`, `SUPERVISOR_OVERRIDE`
   - **Signals (broadcast to all matching instances):** `RESEGMENT`
   - All message names are stable identifiers — treated as a contract. Changes follow contract versioning standard (STD-GOV-004).

3. **`ProcessEventPort` interface location:** `backend/src/main/java/com/netcompany/dcms/shared/process/port/ProcessEventPort.java` (per ADR-003 — interfaces live in `shared/process/port/`, not `domain/`)

4. **`ProcessStartPort` interface location:** `backend/src/main/java/com/netcompany/dcms/shared/process/port/ProcessStartPort.java` (per ADR-003)

5. **Shared event subprocess — call activity pattern (amended by ADR-004):** The standard event subprocesses are defined once as a Flowable call activity subprocess with definition key `standard-event-subprocesses`, deployed from `backend/src/main/resources/processes/fragments/standard-event-subprocesses.bpmn` at application startup. Each treatment process definition invokes it via a BPMN `callActivity` element at root scope — not via copy-on-use embedding. This means updating the shared definition (e.g. adding a new regulatory signal) propagates to all treatment processes automatically on next instance start, without modifying individual process definitions. In-flight instances remain on the version active when they started.

6. **Regulatory event recording:** when `BREATHING_SPACE_START` or `INSOLVENCY_REGISTERED` fires, the `account` module records the fact (date, regulatory reference, source) via a domain event. The process engine independently handles the behavioural response. These are two separate concerns and must not be coupled.

## References

- ADR-001: Process instance per debtor+debt pair
- WAYS-OF-WORKING.md §5a (Class A change triggers)
- STD-GOV-004 (contract versioning)
- Flowable User Guide: Event Subprocesses, Message Events, Signal Events
- DW requirements: DW.9, DW.10, DW.11, DW.18, DW.35, DW.37, DW.38, DW.40, DW.42, DW.45, DW.48, DW.49, DW.51
