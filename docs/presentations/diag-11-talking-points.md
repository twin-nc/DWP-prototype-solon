# DIAG-11 Talking Points — Deceased Party Handling

## Opening — set the scene (30 seconds)

"This diagram shows what happens the moment an agent records a death notification against a customer record. It's a short, fast operation — but it has to be legally watertight. The core design question is: how do you guarantee the deceased flag and communication suppression are active even if the process engine has a failure partway through?"

---

## Where does this sit in the overall system?

"This is not a standalone service or a BPMN process. It's a synchronous handler — a Spring service method — called directly from the agent's UI action. The agent clicks 'confirm deceased', the handler runs, and by the time the screen responds the legal protection is already committed to the database.

Flowable — our BPMN process engine — is involved in Phase 2, but only as a side effect. The existing debt collection process instances for this customer need to be suspended or flagged for human review. Flowable owns those process instances. But critically, Flowable is touched *after* the database commit — not before, not during."

> If asked: Flowable is an open-source BPMN engine embedded in the Spring Boot backend. It manages all long-running debt lifecycle processes — timers, user tasks, escalations. Everything the system is currently doing to collect a debt from this customer lives in Flowable as an active process instance.

---

## The two-phase pattern (the most important concept)

"The diagram is split into two swimlanes — Phase 1 on the left, Phase 2 on the right. This split is the entire design."

**Phase 1 — `@Transactional` (the legal guarantee):**
- Sets `party.deceased_flag = true` — plus `deceased_flagged_at` and `deceased_flagged_by` for the audit trail
- Activates `DECEASED_MANDATORY` suppression — this immediately blocks all debt collection and general communications. Only estate administration correspondence is permitted from this point.
- Writes an `AUDIT_EVENT(DECEASED_FLAG_SET)` record
- **The transaction commits. The customer is legally protected. Full stop.**

**Phase 2 — non-transactional (Flowable side effects):**
- Runs after the commit, outside any transaction
- Branches on whether there is an active joint debt link
- If it fails for any reason — including a Flowable crash, a race condition, a completing process instance — **Phase 1 is not retried and is not rolled back**
- Failures are logged as `MANUAL_REVIEW_REQUIRED` in the audit trail for ops to pick up

"The reason this split matters: sending a debt collection communication to a deceased person — or to their estate without proper authority — creates regulatory and reputational risk for DWP. The two-phase design guarantees that suppression is active before we touch anything else."

---

## Phase 2 — the joint debt decision

"Once Phase 1 commits, Phase 2 iterates over the customer's accounts and hits a decision point: is this a joint debt?"

**Joint debt (YES path):**
- The surviving party still owes the debt — we cannot suspend the collection process
- Instead: create a `SPECIALIST_AGENT` work queue task — "Joint account holder deceased — review required"
- Add a system case note to the account
- Process instances are left running; only the deceased party's contact details and communications are suppressed

**Sole holder (NO path):**
- No surviving party — the debt now sits with the estate
- Suspend all active Flowable process instances for this customer's accounts
- Set all active `REPAYMENT_ARRANGEMENT` records to `SUSPENDED` — not cancelled, because the debt may still be pursued against the estate once an executor is confirmed

> If asked why SUSPENDED not CANCELLED: cancellation would imply the debt is written off. It isn't. It's paused pending estate review. The distinction matters for the account ledger and for any future estate administration track.

---

## The atomicity guarantee (bottom panel)

"The green panel at the bottom summarises the failure contract in plain terms."

- If something goes wrong in Phase 1 — database error, constraint violation, anything — the entire transaction rolls back. Nothing is saved. The agent sees an error and can retry cleanly.
- If something goes wrong in Phase 2 — the deceased flag is already in the database. Flowable failing, crashing, or throwing because a process instance completed a millisecond before we tried to suspend it — none of that can reach back and un-set the flag.
- Flowable manages collection workflows, but it has no power to undo a database commit. That's the guarantee.

---

## Likely questions

**"What triggers this handler?"**
A direct agent action in the UI — a 'confirm deceased' form submission. It's synchronous, not event-driven. The agent confirms the death notification (typically from a death certificate or Tell Us Once notification) and submits.

**"What is DECEASED_MANDATORY suppression?"**
A suppression record in the `communications` domain that blocks outbound messages for the account. `DECEASED_MANDATORY` is a special type — it cannot be overridden by an agent, unlike standard suppression rules. Only estate administration category communications are permitted through.

**"What happens to the estate — can DWP still pursue the debt?"**
Yes, potentially — but that's an open question (DDE-OQ-07, bottom right of the diagram). Estate pursuit policy must be confirmed by DWP before any account management stories past this design point proceed. It's an active DO NOT PROCEED gate.

**"What if the death notification turns out to be wrong?"**
The `deceased_flagged_at` and `deceased_flagged_by` fields recorded in Phase 1 exist precisely to support a future supervised unflag operation. Reversing the flag requires `TEAM_LEADER` authority or above and generates its own audit event. That flow isn't built yet.

**"What about joint accounts where both parties die?"**
Not handled in this diagram — it would need to be treated as a second death notification on the surviving party's record, which would then route through the sole-holder path. Edge case to be confirmed with DWP.

**"What does 'process instance suspended' mean in Flowable?"**
Flowable lets you pause a running process instance without terminating it. Timers stop firing, user tasks stop being assigned. The instance sits in a suspended state and can be resumed later — for example, when an executor is confirmed and estate administration begins.

---

*Sources: RULING-006, ADR-005, MASTER-DESIGN-DOCUMENT*
