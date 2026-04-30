# RULING-012: Limitation Clock Reset Events

**Ruling ID:** RULING-012
**Linked issue:** TBD
**Status:** `awaiting-client-sign-off`
**Date issued:** 23 April 2026
**Domain expert:** DWP Debt Domain Expert

---

## Requirement IDs Covered

- DW.84 (Statute-barred debt handling)
- DIC.27 (Limitation Act 1980 enforcement rules)
- DIC.28 (Acknowledgement and payment as clock-reset events)
- RPF.31 (Repayment arrangement creation and effects)

---

## Regulatory Basis

- **Limitation Act 1980, Sections 29–30** — Acknowledgement and part payment as clock-reset events
- **Limitation Act 1980, Section 31** — Effect of successive acknowledgements
- **DWP Debt Management Policy** — treatment of payment and acknowledgement events

---

## Rule Statement

### 1. Events that reset the limitation clock

Under the Limitation Act 1980, Sections 29–30, the limitation period is reset by the following events:

#### Part payment

Any payment, however small, made by or on behalf of the debtor in acknowledgement of the debt. This means `PAYMENT.status = CLEARED` where `payment_method` is any method **initiated by the debtor** — not a DWP-initiated deduction.

| Payment method | Resets clock? |
|---|---|
| Bank transfer (debtor-initiated) | Yes |
| Direct debit (debtor-authorised) | Yes |
| Cash / postal order | Yes |
| `DEDUCTION_FROM_BENEFIT` (DWP-initiated statutory deduction) | **No** |
| DWP-initiated overpayment recovery | **No** |

#### Written acknowledgement

A written acknowledgement of the debt signed by the debtor or their authorised agent. In a digital system, this means a digitally signed or agent-confirmed statement. Requirements:

- A `CASE_NOTE` of type `ACTION` where the agent records verbal acknowledgement is **not sufficient** — it is hearsay and does not satisfy the Limitation Act's written acknowledgement requirement.
- A formal `PROMISE_TO_PAY` record with debtor confirmation **may** constitute acknowledgement depending on how it is worded — subject to DWP legal confirmation.

#### Arrangement agreement

Entering into a `REPAYMENT_ARRANGEMENT` where the debtor actively agrees to the terms constitutes implicit acknowledgement and resets the clock.

### 2. Events that do NOT reset the clock

The following events do **not** reset the limitation clock, regardless of their impact on account status:

- DWP-generated communications (letters, SMS, email)
- System-generated payment allocations (benefit deductions initiated by DWP)
- Agent-only case notes (no debtor involvement)
- Account status changes made by DWP unilaterally
- Adding or removing flags on the account (DWP action only)

### 3. Implementation: DEBT_STATUS_HISTORY flag

`DEBT_STATUS_HISTORY.limitation_clock_reset = true` must be set on the following events:

1. `PAYMENT` cleared where `payment_method != DEDUCTION_FROM_BENEFIT` (and `payment_method != DWP_INITIATED`)
2. `REPAYMENT_ARRANGEMENT` created with `status = ACTIVE`
3. A new `ACKNOWLEDGEMENT` event type added to `CASE_NOTE.note_type` for formally recorded written acknowledgements (see section 4)

### 4. ACKNOWLEDGEMENT note type

A new `note_type = ACKNOWLEDGEMENT` must be added to `CASE_NOTE`. This type has additional requirements:

- It must capture the debtor's confirmation method (e.g., digital signature, written letter scanned, verbal with contemporaneous record)
- It must only be creatable by `SPECIALIST_AGENT`, `TEAM_LEADER`, or above
- It must generate a `DEBT_STATUS_HISTORY` record with `limitation_clock_reset = true`
- It must not be created based solely on a verbal statement by the debtor — there must be a contemporaneous documentary basis

---

## Edge Cases

### Payment made during breathing space

A payment made by the debtor during a breathing space moratorium still resets the limitation clock, provided it is debtor-initiated. Breathing space suppresses DWP's enforcement activity, not the legal effect of the debtor's own actions. The `DEBT_STATUS_HISTORY` record must be written even during moratorium.

### Payment reversal after clock reset

If a payment that reset the clock is subsequently reversed (e.g., bounced direct debit), the clock reset is void. The `DEBT_STATUS_HISTORY.limitation_clock_reset` record must be updated to `limitation_clock_reset = false` and the original event ID must be referenced. The `is_statute_barred` flag must be recalculated.

### Promise to Pay (PTP) record as acknowledgement

Whether a `PROMISE_TO_PAY` record constitutes a written acknowledgement under the Limitation Act depends on its wording. A PTP that says "I acknowledge I owe £X and promise to pay by [date]" likely satisfies Section 29. A PTP that says "I will try to pay" likely does not. The system must not automatically treat all PTPs as clock-reset events — only those that DWP legal confirms as meeting the acknowledgement standard.

---

## Open Questions Requiring DWP Client Sign-Off

### DDE-OQ-13: PROMISE_TO_PAY as written acknowledgement

**Question:** Does DWP legal confirm that `PROMISE_TO_PAY` records (as currently worded in the system's letter templates) constitute written acknowledgement under Limitation Act 1980 Section 29?

**Impact:** Determines whether PTP creation triggers `limitation_clock_reset = true` in `DEBT_STATUS_HISTORY`.

**Status:** ⚠ **AWAITING DWP LEGAL SIGN-OFF**

---

## Data Classification Flags

- `DEBT_STATUS_HISTORY.limitation_clock_reset` — Operational. Not Restricted.
- `CASE_NOTE` with `note_type = ACKNOWLEDGEMENT` — Operational audit record. Not Restricted.

---

## Guardrails — Builders Must Not Violate

1. Only events explicitly listed in this ruling may set `limitation_clock_reset = true`. No service may set this flag for an unlisted event type without a new domain ruling.
2. `DEDUCTION_FROM_BENEFIT` payments must never set `limitation_clock_reset = true`, regardless of the payment amount.
3. The `ACKNOWLEDGEMENT` note type must be unavailable to the `AGENT` role in the UI and API. The create endpoint must validate the caller's role before accepting this note type.
4. If a payment that triggered a clock reset is reversed, the corresponding `DEBT_STATUS_HISTORY.limitation_clock_reset` must be set to `false` in the same transaction as the reversal. `StatuteBarredEvaluator` must be triggered after the reversal transaction commits.

---

## Version History

| Version | Date | Author | Change |
|---|---|---|---|
| 1.0 | 23 Apr 2026 | DWP Debt Domain Expert | Initial ruling |
