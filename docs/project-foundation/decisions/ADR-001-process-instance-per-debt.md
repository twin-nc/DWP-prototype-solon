# ADR-001: Process Instance per Person + Account Pair

**Status:** Accepted

**Date:** 2026-04-22

**Last updated:** 2026-04-23 — terminology aligned to RULING-002 (Person / Account model)

**Decision Maker:** Architecture Council

---

## Context

The DCMS receives a debt referral (person + account details) via user action in the frontend. Case initiation is user-triggered, not pushed from an external system.

The process engine (Flowable BPMN/DMN) is the authoritative record of where a debt is in its lifecycle. A single Person may have multiple Accounts simultaneously, each at different lifecycle stages (e.g., one in early contact phase, another breached and escalated for enforcement).

Person-level regulatory events (breathing space, insolvency, death) must affect all active Accounts for that Person, requiring cross-process coordination. See RULING-002 for the authoritative definition of Person, Account, and Account-Person Link.

---

## Decision

**One process instance will be created per Person + Account pairing** — i.e., one process instance per `Account`.

**Case initiation** is implemented as an atomic operation:
1. Create `Person` record (or verify existing)
2. Create `Account` record
3. Start process instance via `startProcessInstanceByKey`, all in a single database transaction

**Case view** is a read-model aggregation at the Person level — not a separate domain object. It queries and presents all active process instances and accounts for a Person.

**Person-level events** (breathing space commencement, insolvency date, death notification) are modelled as BPMN signal or message events that correlate across all active process instances for that Person using person ID as the correlation key.

---

## Consequences

**Module responsibilities** are clearly separated:

- **`account` module** owns the financial ledger: balances, payment history, write-off date, overpayments, refunds. It does **not** own lifecycle position.
- **`strategy` module** fires a process-start decision (which account to activate, which treatment path to assign) but does **not** own running state.
- **`workallocation` module** surfaces process tasks and work items by querying active process instances. It does **not** create work independently.
- **`payment` module** allocates payments to accounts following DWP mandatory order; allocation is per-account and aligns with process instance state (current phase determines applicable bands/rules).

**BPMN models must include person-level signal handling from day one:**
- Breathing space, insolvency, and death are **not** account flags that trigger exception logic.
- They are **person-scoped signals** (via `signalEventReceived(personId, ...)`) that cascade to all active process instances for that Person.
- Process models must catch these signals and transition to a "suspended" or "halted" state, with clear re-entry conditions.
- For joint accounts (two Persons linked to one Account), the process model must catch signals from **either** Person — a regulatory event on any linked Person protects the account.

**Multi-account coordination:**
- Accounts can progress independently within their lifecycle states.
- Regulatory holds (breathing space, insolvency) are applied uniformly across all accounts linked to the affected Person.
- Payment collection is per-account; overpayments may be applied to other accounts for the same Person via a manual or automated reallocation workflow.

---

## Alternatives Considered

### One Process Instance per Person (Rejected)

**Alternative:** Create one process instance per Person that governs all accounts for that Person.

**Rationale for rejection:**
- Accounts typically exist at radically different lifecycle stages simultaneously (one early contact, one in court, one being recovered). A single process would require parallel subprocesses to model per-account state.
- That re-invents the per-account process model inside a parent process — adding complexity without benefit.
- Person-level signals (breathing space) apply uniformly to all accounts anyway; there is no semantic reason to nest per-account state in a parent process.
- Query and reporting become harder: each query must navigate parent + child instances and reconstruct the per-account view.

### Lift Case Lifecycle into Domain Object (Rejected)

**Alternative:** Create a separate `Case` domain object that owns lifecycle state, independent of Flowable.

**Rationale for rejection:**
- Duplicates state between the process engine and the domain model, creating eventual consistency risk.
- The process engine is chosen to be the authoritative lifecycle record; storing parallel state in an ORM entity undermines that choice.
- Case view is a **read-model** concern, not a domain entity — it aggregates data across domains (person, account, process) to answer "what is the status of all accounts for this Person?"

---

## Implementation Notes

1. **Atomicity:** Case initiation (person + account + process start) must be wrapped in a single `@Transactional` boundary. If process start fails, the entire transaction rolls back.

2. **Person-Level Signals:** Use Flowable's `RuntimeService.signalEventReceived(signalName, variables)` with a custom message or signal event that includes the person ID. Process models must define catch signal events on their root scope.

3. **Query Patterns:** The case view service queries `HistoricProcessInstance` and `ProcessInstance` filtered by person ID to reconstruct the person-centric view. No separate query table is needed.

4. **Regulatory Event Handling:** Breathing space, insolvency, and death events from external systems (DWP Place, bureau feeds) trigger a signal broadcast to all active instances for that Person. Document the entry point (e.g., in the `integration` or `account` module) clearly.

5. **Joint Account Signal Handling:** For accounts linked to two Persons (UC couple joint accounts), the process model must subscribe to signals from both Person IDs. A regulatory event on either Person halts the process instance for that account.

---

## References

- RULING-002: Account-Person Model and Debt Recovery Mechanics
- BPMN 2.0 specification (OMG)
- Flowable User Guide: Signal Events
