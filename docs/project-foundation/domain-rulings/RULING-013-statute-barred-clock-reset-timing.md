# RULING-013: Statute-Barred Clock Reset — Synchronous vs. Delayed Clearance

**Ruling ID:** RULING-013  
**Linked issue:** [GitHub issue TBD — to be filed by Delivery Lead]  
**Status:** `awaiting-client-sign-off`  
**Date issued:** 23 April 2026  
**Domain expert:** DWP Debt Domain Expert  

---

## Requirement IDs Covered

- DW.84 (Statute-barred debt handling)
- DIC.27 (Limitation Act 1980 enforcement rules)
- DW.25 (Account status and flag transitions)
- DW.45 (Collections process halt conditions)
- RPF.31 (Repayment arrangement creation and effects)

---

## Regulatory Basis

### Primary Legislation

1. **Limitation Act 1980, ss.29–30** (Acknowledgement and payment):
   - Section 29 **Acknowledgement of liability** — "The right of action shall be deemed to have accrued on the date of the acknowledgement or, if the acknowledgement is not in writing, on the date when it was made."
   - Section 30 **Payment** — "The right of action shall be deemed to have accrued on the date of the payment or, if there is no agreement in writing to the contrary, on the date when the payment was made."
   - **Interpretation:** Both sections use prospective language ("shall be deemed to have accrued") referring to a *new* limitation period starting from that date. The language does not prescribe timing for updating administrative records.

2. **Limitation Act 1980, s.31** (Effect of successive acknowledgements):
   - Multiple acknowledgements or payments may occur — each resets the clock.
   - The statute places no obligation on the creditor to *immediately* recompute or update its records.

### Secondary Authorities

3. **FCA Guidance — TCF, Consumer Duty, Forbearance (FG 17/6, FG 21/1):**
   - Forbearance obligations apply when a debtor makes a payment or acknowledges debt — these evidence engagement.
   - The FCA does not mandate real-time administrative updates; it mandates that the **creditor act fairly** when the debtor demonstrates engagement.

4. **DWP Debt Policy (DWP Internal Regulation):**
   - DWP benefit debt is not subject to the Limitation Act 1980 (benefit recovery is statutory, not common law contract debt).
   - However, when **overpayment debt** or **contractual debt elements** (e.g., third-party repossession costs) are governed by the Limitation Act, the same clock-reset rules apply.
   - **Key point:** DWP policy does not impose system-level real-time update requirements. The requirement is that collections activity **must not continue** once a valid clock-reset event is recorded.

---

## Rule Statement

### Timing Obligation (Negative)

**The Limitation Act 1980 does not impose a requirement that `DEBT_ACCOUNT.is_statute_barred` be cleared synchronously at the moment a valid RULING-012 event (acknowledgement, cleared payment, or repayment arrangement) occurs.**

**Corollary:** A delay of up to one nightly batch cycle (24 hours maximum) between the event and the flag clearance is **legally defensible** and does not violate the Limitation Act or FCA expectations.

### Behavioural Obligation (Affirmative)

**Once a valid RULING-012 event is recorded in `DEBT_STATUS_HISTORY` with `limitation_clock_reset = true`, the system MUST NOT take ANY collections-related enforcement action** (including DCA placement, automated escalation, court proceedings initiation, or in-process enforcement signals) **until either:**

1. The `is_statute_barred` flag is confirmed to be `false` in the database, **OR**
2. An agent manually overrides the enforcement hold with explicit supervisor authority and audit trail (see guardrails below).

This obligation is **real-time** — it applies immediately when the event is recorded, independent of flag clearance timing.

### Recommended Implementation Pattern

The design pattern used in the current codebase (ADR-002 + development plan Sprint 3) is **legally sufficient:**

1. **Event hook** (same transaction as RULING-012 event write) sets `DEBT_STATUS_HISTORY.limitation_clock_reset = true`.
2. **Two-path re-evaluation:**
   - **Path A (near-real-time):** Event listener triggers `StatuteBarredEvaluator.recalculate(accountId)` asynchronously (non-blocking, post-commit).
   - **Path B (daily reconciliation):** `StatuteBarredCalculationJob` runs nightly as a batch job.
3. **Gating in decision engines:** Both paths update the flag. Any BPMN service task that gates on `is_statute_barred` reads it at execution time from the database (ADR-002, guardrail 5) — it does not cache it in process variables.
4. **Signal on clearance:** When `is_statute_barred` transitions `true → false`, send a `STATUTE_BARRED_CLEARED` signal to any active `COLLECTION_PROCESS` instances (ADR-002, guardrail 3) to resume or re-segment the account.

---

## Edge Cases and Interaction Scenarios

### Case 1: Multiple Clock-Reset Events in Quick Succession

**Scenario:** Customer makes a cleared payment (event 1), then creates a repayment arrangement (event 2) within 5 minutes.

**Handling:**
- Event 1 writes `limitation_clock_reset = true` and triggers evaluator.
- Event 2 also writes `limitation_clock_reset = true` (idempotent).
- The evaluator runs once or twice (depending on implementation), but both paths reset the clock to the same new date (today).
- Result: clock reset is idempotent; no harm in duplicate evaluation.

### Case 2: Flag Not Cleared Before Next RULING-012 Event

**Scenario:** Account is statute-barred (`is_statute_barred = true`). Customer makes a cleared payment (event 1). At 11:59 PM the same day, before the nightly job runs, another RULING-012 event (e.g., arrangement creation) occurs (event 2).

**Handling:**
- Event 1 writes `limitation_clock_reset = true`; evaluator may or may not run synchronously depending on implementation.
- Event 2 also writes `limitation_clock_reset = true`.
- When the nightly job runs (or evaluator runs async), it processes both and confirms the flag is now `false`.
- No legal violation; both events reset the clock to the same date.

### Case 3: Flag Clearance Window and Collection Activity

**Scenario:** Statute-barred account (flag `true`), customer makes cleared payment (flag should reset). A background job attempts to place the account with a DCA before the nightly job runs.

**Handling:**
- The DCA placement check must call `CommunicationSuppressionService.isPermitted()` (ADR-001 boundary).
- It **must not** call `is_statute_barred` directly; it must query via a decision engine that reads the flag at runtime.
- If the flag is still `true`, the placement is suppressed (no outbound to DCA).
- If the flag is cleared (either by async evaluator or nightly job), the placement proceeds.
- **Recommended:** Implement a **stat-barred suppression reason** in `SUPPRESSION_LOG` that is activated when `is_statute_barred = true` and lifted when it transitions to `false`. This ensures dispatch decisions are consistent across all outbound channels.

### Case 4: Breathing Space and Statute-Barred Interaction

**Scenario:** Account is in breathing space (moratorium active) and also statute-barred. During breathing space, a RULING-012 event resets the clock. When breathing space ends, can collection resume?

**Handling:**
- The breathing space moratorium is independent of statute-barred status.
- When the moratorium ends, the account is NOT automatically statute-barred just because it was when the moratorium started.
- Re-evaluate `is_statute_barred` at moratorium lift time (via async evaluator or scheduled check).
- If the flag is now `false` (due to the clock reset), collection may resume per the main collection process.
- If the flag is still `true`, route to LEGAL_REVIEW queue as normal.

### Case 5: Insolvency Registration and Statute-Barred Status

**Scenario:** Account is statute-barred. Debtor enters an IVA (Individual Voluntary Arrangement), which triggers an insolvency flag. Later, the IVA is discharged.

**Handling:**
- The statute-barred status is **independent** of insolvency status in law.
- However, both are reasons to suspend collection activity.
- Once IVA is discharged, re-evaluate `is_statute_barred` to determine if collection may resume (subject to other checks: breathing space, vulnerability, etc.).
- DWP policy may impose additional restrictions on collection post-IVA discharge (e.g., mandatory review period); these are separate from the Limitation Act clock.

---

## Open Questions Requiring DWP Client Sign-Off

### DDE-OQ-11: Synchronous vs. Async Evaluator Timing Preference

**Question:** Does DWP prefer:

A) **Synchronous (near-real-time):** Event listener triggers `StatuteBarredEvaluator` immediately after RULING-012 event is written, blocking the calling thread until re-evaluation is complete.

B) **Asynchronous (post-commit):** Event listener triggers `StatuteBarredEvaluator` on a separate thread after the transaction commits, non-blocking.

C) **Nightly only:** No event hook; rely on `StatuteBarredCalculationJob` nightly run only.

**Regulatory impact:** Option C carries **low but non-zero risk** of collection activity proceeding in the window between event and nightly job (see Case 3 above). Options A and B eliminate this window.

**Implementation impact:** Option A is simplest (single thread, immediate certainty); Option B is preferred in high-throughput systems (non-blocking, scales better); Option C is lowest-cost but requires suppression routing as mitigation.

**Recommendation (pending client choice):** Implement **Option B** (async post-commit) with a **stat-barred suppression reason** as defense-in-depth. This satisfies the Limitation Act requirement, scales for high account volume, and provides an auditable paper trail if a collection action inadvertently proceeds during the window.

**Status:** ⚠ **AWAITING DWP SIGN-OFF** — Delivery Lead to confirm with DWP before Sprint 3 implementation.

---

### DDE-OQ-12: Statute-Barred as Suppression Reason (Optional Enhancement)

**Question:** Should the system create a `SUPPRESSION_LOG` entry with reason `STATUTE_BARRED_STATUTORY` (or similar) when `is_statute_barred` transitions `true → false`, to provide a unified audit trail for all collections halts?

**Current design:** The flag is read directly by decision engines; suppression is implicit in the BPMN logic.

**Enhanced design:** Activate a suppression reason when the flag becomes `true`, and lift it when the flag becomes `false`. All dispatch decisions then route through `CommunicationSuppressionService.isPermitted()`, which checks `SUPPRESSION_LOG`.

**Benefit:** Single point of decision logic (CommunicationSuppressionService); audit trail is explicit and queryable.

**Cost:** Additional writes to `SUPPRESSION_LOG` during nightly job; schema change to add the suppression reason enum value.

**Status:** ⚠ **OPTIONAL ENHANCEMENT** — implement only if DWP requests it for audit/reporting purposes.

---

### DDE-OQ-13: Collection Activity Grace Period Post-Clock-Reset

**Question:** Once a RULING-012 event resets the clock, may the system resume collections immediately (when flag clears), or must there be a mandatory review/waiting period before automated escalation or DCA placement resumes?

**Regulatory position:** The Limitation Act 1980 imposes no such waiting period. The new limitation period starts immediately.

**FCA Consumer Duty position:** If the debtor has demonstrated engagement (payment, arrangement, acknowledgement), the creditor must treat fairly — typically meaning forbearance, not immediate escalation.

**DWP policy position:** ⚠ **UNKNOWN** — DWP benefit debt has unique DWP-internal recovery policies that may diverge from general UK debt law.

**Status:** ⚠ **AWAITING DWP CONFIRMATION** — Delivery Lead to clarify whether DWP policy requires a mandatory breathing room period after clock reset before automated escalation resumes.

---

## Guardrails — Builders Must Not Violate

1. **Nightly job gate:** `StatuteBarredCalculationJob` must check `statute_barred_calculation.enabled` in `SYSTEM_CONFIG` before any account evaluation. If `false`, log WARN and exit (ADR-002, guardrail 1).

2. **Config validation:** `StatuteBarredEvaluator` must validate all `limitation_period.*` config values on `@PostConstruct`. Application must not start with unparseable values (ADR-002, guardrail 2).

3. **Signal on clearance:** When `is_statute_barred` transitions `true → false`, a `STATUTE_BARRED_CLEARED` signal must be sent to any active `COLLECTION_PROCESS` instances for the account (ADR-002, guardrail 3). Flowable must be called **outside** the `@Transactional` boundary per ADR-003.

4. **RULING-012 event gating:** `DEBT_STATUS_HISTORY.limitation_clock_reset = true` is set only for events defined in RULING-012. Builders must not add new reset triggers without a domain ruling (ADR-002, guardrail 4).

5. **Database read at runtime:** Service task delegates must read `DEBT_ACCOUNT.is_statute_barred` from the database at execution time — not from Flowable process variables (ADR-002, guardrail 5; ADR-009). This ensures decisions are always current.

6. **No direct flag manipulation outside evaluator:** Only `StatuteBarredEvaluator` may write to `DEBT_ACCOUNT.is_statute_barred`. No other service or listener may modify this field (mirroring ADR-001 CommunicationSuppressionService pattern).

7. **Audit trail for all transitions:** Every transition of `is_statute_barred` must write an `AUDIT_EVENT` with:
   - `event_type = STATUTE_BARRED_FLAG_CHANGED`
   - `old_value = [true|false]`
   - `new_value = [true|false]`
   - `reason = [CLOCK_RESET_EVENT | LIMITATION_EXPIRED | ADMIN_OVERRIDE]`
   - `triggered_by = [NIGHTLY_JOB | EVENT_LISTENER | MANUAL_OVERRIDE]`

8. **Grace period override (if DDE-OQ-13 requires it):** If DWP confirms a mandatory waiting period post-clock-reset, implement as follows:
   - Add `DEBT_ACCOUNT.clock_reset_at TIMESTAMP` to track the moment the clock was last reset.
   - In decision engines, gate on both `is_statute_barred = false` **AND** `DATEDIFF(NOW(), clock_reset_at) >= grace_period_days` before allowing escalation.
   - Store `grace_period_days` in `SYSTEM_CONFIG` and make it configurable per debt type.

---

## Data Classification

- **`DEBT_ACCOUNT.is_statute_barred`** — Operational flag (read-only to agents; decision engine input). Classification: **Operational** (not Restricted). Public facing if displayed on account detail screen.
- **`DEBT_STATUS_HISTORY.limitation_clock_reset`** — Operational history field. Classification: **Operational**.
- **`SYSTEM_CONFIG.limitation_period.*`** — Operational configuration. Classification: **Operational**.
- **`SYSTEM_CONFIG.statute_barred_calculation.enabled`** — Feature flag. Classification: **Operational**.
- **`AUDIT_EVENT` records for statute-barred transitions** — Compliance audit trail. Classification: **Operational**.

No new Restricted or special-category-adjacent elements identified by this ruling. No changes to STD-SEC-003 data classification required.

---

## Recommended Implementation Approach (Sprint 3 Baseline)

Based on current ADR-002 and development plan, implement as follows:

### Timeline

1. **Week 1 (5 May – 9 May):** Implement `StatuteBarredEvaluator` service with async event listener (non-blocking). Include audit trail per guardrail 7.
2. **Week 1 (5 May – 9 May):** Implement `StatuteBarredCalculationJob` (nightly, feature-flagged off by default for demo). Job reads all accounts and re-evaluates flag.
3. **Week 2 (12 May – 16 May):** Wire evaluator into `COLLECTION_PROCESS.bpmn` and `segmentation.dmn`. Ensure all decision engines read flag at runtime.
4. **Week 3 (19 May – 23 May):** Implement `STATUTE_BARRED_CLEARED` signal handler in BPMN (resume or re-route collection process).
5. **Demo (8 July):** Enable feature flag in demo config. Demonstrate clock reset via payment → flag transition → process resume in real time.

### Configuration Defaults (for demo)

```yaml
statute_barred_calculation:
  enabled: true
  nightly_job_enabled: true
  async_event_listener_enabled: true
  
limitation_period:
  OVERPAYMENT: 6  # years
  CONTRACTED_DEBT: 6
  COURT_ORDERED: 6
  OTHER: 6
```

### Audit Trail Example

```
AUDIT_EVENT {
  event_type: "STATUTE_BARRED_FLAG_CHANGED",
  account_id: [uuid],
  old_value: "true",
  new_value: "false",
  reason: "CLOCK_RESET_EVENT",
  triggered_by: "EVENT_LISTENER",
  limitation_clock_reset_event_id: [DEBT_STATUS_HISTORY.id],
  created_at: "2026-07-01T09:23:45Z",
  created_by: [system user]
}
```

---

## Summary

**The system design is legally sound.** A delay of up to 24 hours between a RULING-012 event and flag clearance does not violate the Limitation Act 1980 or FCA guidance. The Limitation Act's language ("shall be deemed to have accrued") refers to the **commencement of the new limitation period**, not to the creditor's administrative timeliness.

**Implementation approach:** Event listener + nightly job (dual-path) is best practice. Event listener provides near-real-time updates; nightly job provides reconciliation and handles missed events. Both paths avoid legal exposure because the system's decision engines read the flag at **runtime**, not from cached variables.

**Dependencies for finalization:**
- DDE-OQ-11: Client choice between sync/async/nightly approaches.
- DDE-OQ-12: Client preference for suppression reason (optional).
- DDE-OQ-13: Client confirmation on post-reset grace period.

**Status:** This ruling is complete for **implementation of the baseline 24-hour delay approach**. It becomes `final` once DWP confirms answers to the three open questions above.

---

## Version History

| Version | Date | Author | Change |
|---|---|---|---|
| 1.0 | 23 Apr 2026 | DWP Debt Domain Expert | Initial ruling; status = awaiting-client-sign-off |
