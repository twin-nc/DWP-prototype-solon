# Domain Expert Open Questions — Statute-Barred Clock Reset

**Related ruling:** RULING-013  
**Status:** Awaiting DWP client confirmation  
**Owner:** Delivery Lead (to escalate to DWP)  
**Decision deadline:** Sprint 3 kickoff (5 May 2026)  

---

## Summary for Delivery Lead

RULING-013 establishes that the system may delay flag clearance by up to 24 hours (nightly batch cycle) without violating the Limitation Act 1980 or FCA guidance. This unblocks the baseline implementation.

**However, three client choices remain:**

| Question | Option A | Option B | Option C | Recommend | Blocker |
|---|---|---|---|---|---|
| **DDE-OQ-11:** Evaluator timing | Synchronous (real-time flag update) | Asynchronous post-commit (non-blocking) | Nightly only (no event hook) | B | Affects thread model, error handling |
| **DDE-OQ-12:** Suppression log integration | Yes (new suppression reason `STATUTE_BARRED_STATUTORY`) | No (implicit in BPMN logic) | — | Yes | Audit trail clarity; affects schema |
| **DDE-OQ-13:** Grace period post-reset | Mandatory waiting period (e.g., 7 days) | Resume collections immediately | — | Clarify | Affects forbearance logic post-clock-reset |

---

## DDE-OQ-11: Evaluator Timing — Three Approaches

### Option A: Synchronous (Real-Time)

**When:** Immediately within the same transaction as the RULING-012 event is recorded.

**How:**
```
Agent logs ACKNOWLEDGEMENT case note
  → JPA flush
  → Event listener fires
  → StatuteBarredEvaluator.recalculate() called **synchronously**
  → is_statute_barred updated **before response sent to client**
```

**Pros:**
- Client sees updated flag on next screen load (same session).
- Eliminates any window for accidental collection activity.
- Simplest to reason about.

**Cons:**
- Blocking call; adds latency to transaction.
- If evaluator is slow, degrades user experience.
- Not suitable for high-throughput batch operations.

**Regulatory risk:** None — this exceeds the Limitation Act's requirements.

**Recommended if:** Low-volume, low-latency-sensitive system OR client demands real-time visibility.

---

### Option B: Asynchronous Post-Commit (Recommended)

**When:** Event listener triggers after the transaction commits, on a separate thread or via message queue.

**How:**
```
Agent logs ACKNOWLEDGEMENT case note
  → Transaction commits (RULING-012 event written)
  → Event listener **enqueues** async task
  → Response sent to client **immediately**
  → Async worker calls StatuteBarredEvaluator.recalculate() in background
  → is_statute_barred updated within seconds/minutes
```

**Pros:**
- Non-blocking; client sees response immediately.
- Scales for high-volume accounts.
- Allows job retry logic if evaluator fails.
- Follows Spring event pattern best practices.

**Cons:**
- Small window (seconds to minutes) where flag may still be `true` in DB.
- If async task fails, nightly job is the fallback.

**Mitigation:**
- Implement `SUPPRESSION_LOG` suppression reason `STATUTE_BARRED_STATUTORY` (DDE-OQ-12 = Yes).
- All outbound dispatch routes through `CommunicationSuppressionService.isPermitted()`, which reads the flag at the moment of dispatch.
- Even if async evaluator hasn't run yet, dispatch is blocked until suppression is lifted (which happens when flag clears).

**Regulatory risk:** None — flag is read at dispatch time, not at event time.

**Recommended if:** System is expected to handle 100k+ accounts; client prioritizes performance.

---

### Option C: Nightly Only

**When:** `StatuteBarredCalculationJob` runs nightly (e.g., 02:00 UTC); no event hook.

**How:**
```
Agent logs ACKNOWLEDGEMENT case note
  → RULING-012 event written; no evaluator call
  → At 02:00 UTC: nightly job
    → Queries all accounts
    → StatuteBarredEvaluator.recalculate() for each
    → Flag updated
```

**Pros:**
- Simplest to implement; no event listener code.
- Handles missed events via batch reconciliation.
- Single code path (no async complexity).

**Cons:**
- Up to 24-hour delay between event and flag clearance.
- Highest risk of accidental collection activity during the window.
- Requires `SUPPRESSION_LOG` mitigation (DDE-OQ-12 = Yes).

**Regulatory risk:** Low but non-zero. If collection activity somehow proceeds between event and nightly job (e.g., manual escalation, queued process, or system error), the creditor cannot claim real-time action on the clock-reset event.

**Recommended if:** System is low-volume and real-time performance is not a concern. NOT recommended for production.

---

## DDE-OQ-12: Suppression Log Integration

### Question

Should the system add a new suppression reason, `STATUTE_BARRED_STATUTORY`, to `SUPPRESSION_LOG`? When `is_statute_barred` transitions `true → false`, activate/lift this suppression reason.

**Current design:** Flag is read directly by decision engines. Suppression is implicit in BPMN logic.

**Enhanced design:** Flag changes activate/lift suppression reason. All dispatch decisions route through `CommunicationSuppressionService.isPermitted()`.

### Impact

**If Yes:**

```sql
-- Add to SUPPRESSION_LOG enum
ALTER TYPE suppression_reason ADD VALUE 'STATUTE_BARRED_STATUTORY';

-- When flag transitions true → false
INSERT INTO suppression_log (..., suppression_reason = 'STATUTE_BARRED_STATUTORY', activated_at = NOW(), ...)

-- When flag transitions false → true
UPDATE suppression_log SET lifted_at = NOW() WHERE account_id = ? AND suppression_reason = 'STATUTE_BARRED_STATUTORY'
```

**Benefits:**
- Single unified decision point: `CommunicationSuppressionService.isPermitted()`.
- Audit trail is explicit; queries on `SUPPRESSION_LOG` show all halts.
- Follows existing ADR-001 pattern (communication suppression).
- Covers all outbound channels (letter, SMS, email, in-app).
- Required for Option C (nightly only) to be safe.

**If No:**

- Decision engines read `is_statute_barred` directly from `DEBT_ACCOUNT`.
- `CommunicationSuppressionService` is not involved.
- Audit trail is implicit (must query `AUDIT_EVENT` records).
- Slightly lower complexity; no schema change.

### Recommendation

**Answer Yes.** This follows the existing CommunicationSuppressionService pattern and provides an auditable, unified decision point. Required for Options B and C to be defensible in court if a collection action inadvertently proceeds during the flag-clearance window.

---

## DDE-OQ-13: Grace Period Post-Clock-Reset

### Question

When a RULING-012 event resets the clock, may the system resume automated collection activity (escalation, DCA placement, enforcement) immediately, or must there be a mandatory waiting period?

### Regulatory Position

**Limitation Act 1980:** The new limitation period begins the day after the clock-reset event. No waiting period is mandated.

**FCA Consumer Duty / TCF:** A debtor making a payment or acknowledging debt demonstrates engagement. Fairness requires consideration of forbearance, not immediate escalation. However, this is a behavioral expectation, not a hard rule with a mandatory number of days.

**DWP Policy:** Unknown. DWP benefit debt recovery may have specific internal policies (e.g., "after a payment, must review I&E before escalation" or "after an arrangement breach, 10-day review period before legal referral"). These differ from general UK debt law.

### Implementation Scenarios

**Scenario A: No grace period (resume immediately)**
- As soon as flag clears, automated processes may escalate, place with DCA, refer to legal, etc.
- Fastest recovery; may breach FCA fairness expectations if debtor shows vulnerability signals.

**Scenario B: Configurable grace period (e.g., 7 days)**
- Add `DEBT_ACCOUNT.clock_reset_at TIMESTAMP`.
- In decision engines, gate on `is_statute_barred = false` **AND** `DATEDIFF(NOW(), clock_reset_at) >= grace_period_days`.
- Default grace period in `SYSTEM_CONFIG` (e.g., 7 days); configurable per debt type.
- After period expires, standard collection processes resume.

**Scenario C: Manual review required**
- Add `DEBT_ACCOUNT.clock_reset_at` and a flag `requires_post_reset_review = true`.
- Route account to a specialist queue for forbearance assessment before resuming escalation.
- Agent must close the review and clear the flag before automated processes resume.

### Recommendation

**Ask DWP:** Does benefit debt recovery policy require any waiting or review period after a clock-reset event before automated escalation resumes?

- If **Yes, and specific days required:** Implement Scenario B with the specified period.
- If **Yes, but no specific period:** Implement Scenario B with a default 7-day grace period (aligned with FCA practice in consumer credit).
- If **No:** Implement Scenario A (no grace period; resume immediately).

---

## Next Steps

1. **Delivery Lead:** Send three questions (DDE-OQ-11, DDE-OQ-12, DDE-OQ-13) to DWP client contact.
2. **DWP Response:** Expect answers by 30 April 2026 (Sprint 3 planning).
3. **Developers:** Implement to agreed options in Sprint 3 (5 May – 4 June).
4. **RULING-013 Status Update:** Once answers confirmed, file amendment to RULING-013 status → `final`.

---

## Escalation Path

If DWP cannot confirm by 30 April 2026:

1. **Default to Option B + Yes + No** (async evaluator, suppression log integration, no grace period).
2. **Document decision** in RULING-013 amendment as "client default choice due to no response by decision deadline."
3. **Post-award:** Refine to match client's actual policy in a maintenance release.
4. **Demo (8 July):** Demonstrate baseline approach; explain alternate approaches to tender panel.
