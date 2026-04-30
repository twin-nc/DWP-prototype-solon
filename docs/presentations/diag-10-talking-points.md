# DIAG-10 Talking Points — Breathing Space

## Opening — set the scene (30 seconds)

"This diagram shows how the system handles Breathing Space — a legal moratorium where DWP must stop all debt collection activity for a period. There are two variants: a standard 60-day moratorium, and a Mental Health Crisis path where there's no end date. The key architectural question is: how do you guarantee protection is active even if the software has a partial failure?"

---

## Where does this sit in the overall system?

"Before we walk the flows — this is not a standalone service. It runs inside the main debt lifecycle BPMN process in Flowable, our process engine. When a breathing space notification arrives, Flowable fires a **non-interrupting event subprocess**. Non-interrupting means the main process token — wherever that debt account is in its lifecycle — keeps moving. The breathing space logic runs in parallel. That's the BPMN mechanism that makes this work."

> If asked: Flowable is an open-source BPMN engine embedded in the Spring Boot backend. It manages all long-running debt lifecycle processes — timers, user tasks, escalations.

---

## The two-phase commit pattern (the most important concept)

"Walk to Phase 1 on either panel. The single most important design decision here is the split between Phase 1 and Phase 2."

**Phase 1 — `@Transactional` (the legal guarantee):**
- Sets `breathing_space_flag = true` on the account
- Creates a `LEGAL_HOLD` record in the database
- Activates the communication suppression rule — nothing will be sent to this customer
- Writes an audit event
- **This is a database commit. Once this commits, the account is legally protected. Full stop.**

**Phase 2 — non-transactional (Flowable subprocess start):**
- Starts the Flowable subprocess that will manage the timer and expiry
- If Flowable fails here, we log a `MANUAL_REVIEW_REQUIRED` audit event
- **Crucially: a Flowable failure here does NOT roll back Phase 1.** The moratorium is still active. A human will pick it up, but the customer is not exposed in the meantime.

"The reason this matters: under the Debt Respite Scheme Regulations 2020, sending a debt collection communication to someone in breathing space is a **criminal offence**. The two-phase design is the architectural guarantee that we cannot accidentally breach that — even in a failure scenario."

---

## Left panel — Standard 60-day path

Walk top to bottom:

1. **Trigger** — `BREATHING_SPACE_START` message arrives (from an external notification, e.g. a Debt Advice provider registering the moratorium via the Insolvency Service)
2. **Phase 1** — legal hold committed to DB as above
3. **Phase 2** — Flowable subprocess starts
4. **Intermediate Timer** — Flowable parks here, waiting for `${breathingSpaceEndDateISO}` — a process variable set to today + 60 days. Nothing happens during this wait; suppression is already active from Phase 1.
5. **Timer fires at day 60:**
   - `DiscardQueuedCollectionCommunicationsDelegate` — purges any queued comms that built up during the moratorium
   - `LiftBreathingSpaceSuppressionDelegate` — removes the suppression rule
   - `ResumeArrangementMonitoringDelegate` — extends any repayment plan end dates by the number of moratorium days (so the customer isn't penalised for time lost)
6. **Post-review User Task** — assigned to a `SPECIALIST_AGENT` to review the account and decide next steps

---

## Right panel — Mental Health Crisis path

"This is different in one critical respect: **there is no end date**. A mental health crisis moratorium under the regulations has no fixed duration — it continues until a mental health professional confirms it should end."

1. **Same trigger**, but flagged as MH crisis variant
2. **Phase 1** — `LEGAL_HOLD.end_date = NULL`. A non-null value here would be a data integrity violation — we're explicitly recording that no end date exists yet.
3. **User Task — Awaiting MH Confirmation** — assigned to `SPECIALIST_AGENT`. There is no automatic timer countdown. A human must act.
4. **Boundary escalation timer** — if the user task sits unactioned for `${mhcEscalationDays}` days, it escalates to an `OPS_MANAGER`. This is a safety valve, not an expiry.
5. **On confirmation** — same discard / lift / resume sequence as the standard path runs, triggered by the agent completing the task.

> If asked why no automatic timer: the regulations don't allow one. The moratorium for MH crisis continues for as long as the person is receiving treatment. Setting a date would create legal risk.

---

## Transaction boundary rule (bottom panel)

"The bottom panel summarises the failure contract in one place."

- If Flowable fails to start the subprocess: Phase 1 is already committed. The account is protected. We log `MANUAL_REVIEW_REQUIRED` so ops can intervene and manually restart the subprocess.
- We never retry into a second transaction for Phase 1 — idempotency is handled at the `LEGAL_HOLD` level (unique constraint on account + start_date prevents duplicate holds).

---

## Likely questions

**"What sends the BREATHING_SPACE_START message?"**
The `integration` package — specifically the anti-corruption layer that handles inbound notifications from the Insolvency Service API or DWP Place. That's out of scope for this diagram but it's a defined integration boundary.

**"What is a suppression rule?"**
A record in the `communications` domain that blocks outbound messages for a given account + channel combination. The `BREATHING_SPACE_STATUTORY` suppression type blocks all collection comms; it's checked before any communication is dispatched.

**"What if the account has an active repayment plan?"**
`ResumeArrangementMonitoringDelegate` handles this — it adds the moratorium duration to any plan timers that were paused, so the arrangement doesn't breach just because the customer was in breathing space.

**"Is the 60 days calendar days or working days?"**
Calendar days, per the Debt Respite Scheme Regulations 2020.

**"Could the moratorium be extended beyond 60 days?"**
Yes — the standard moratorium can be extended by the debt advice provider, which would fire a new `BREATHING_SPACE_EXTENDED` message and update the timer variable. That extension path isn't shown here but follows the same Phase 1 / Phase 2 pattern.

---

*Sources: RULING-001, ADR-007, ADR-003, Debt Respite Scheme Regulations 2020*
