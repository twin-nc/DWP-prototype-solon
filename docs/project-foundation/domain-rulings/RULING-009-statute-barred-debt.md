# RULING-009: Statute-Barred Debt

**Ruling ID:** RULING-009
**Linked issue:** TBD
**Status:** `awaiting-client-sign-off`
**Date issued:** 23 April 2026
**Domain expert:** DWP Debt Domain Expert

---

## Requirement IDs Covered

- DW.84 (Statute-barred debt handling)
- DIC.27 (Limitation Act 1980 enforcement rules)
- DW.25 (Account status transitions)

---

## Regulatory Basis

- **Limitation Act 1980, Section 5** — Simple contract debt: 6 years from cause of action
- **Limitation Act 1980, Section 9** — Specialty debt: 12 years
- **Social Security Administration Act 1992** — DWP benefit overpayment recovery
- **DWP Debt Management Policy**

---

## Rule Statement

This ruling establishes a **DO NOT PROCEED gate** on any enforcement or collection workflow until the limitation period applicable to each DWP debt type is confirmed by DWP legal.

### 1. Applicable limitation periods by debt type

| Debt type | Likely limitation period | Basis | Status |
|---|---|---|---|
| Benefit overpayment | 6 years (may be longer) | Social Security Administration Act 1992 / Limitation Act 1980 | ⚠ AWAITING DWP LEGAL CONFIRMATION |
| Contractual debt elements | 6 years | Limitation Act 1980 s.5 | Provisional |
| Court-ordered debt | 12 years | Limitation Act 1980 s.24 | Provisional |

DWP benefit overpayments may be recoverable under statute for up to 6 years under the Social Security Administration Act 1992, but this interacts with the Limitation Act. DWP legal must confirm the applicable limitation period per debt type before the statute-barred calculation is implemented.

### 2. Required domain model fields

`DEBT_ACCOUNT` currently has no `cause_of_action_date` or `last_acknowledgement_date` fields. These are required to calculate statute-barred status.

**Required additions:**
- `DEBT_ACCOUNT.cause_of_action_date DATE NOT NULL` — the date the debt became legally enforceable (typically the date of overpayment decision or last acknowledgement / payment, whichever is later)
- `DEBT_ACCOUNT.is_statute_barred BOOLEAN NOT NULL DEFAULT false` — system-calculated flag, updated by `StatuteBarredEvaluator`

`DEBT_STATUS_HISTORY` partially covers acknowledgement history but is not sufficient for a formal limitation calculation. A dedicated `limitation_clock_reset` flag must be added to `DEBT_STATUS_HISTORY` per RULING-012.

### 3. Segmentation DMN must include statute-barred route

The `segmentation.dmn` has no rule for statute-barred debt. A statute-barred debt must route to a **legal review track**, not standard collection. Pursuing a statute-barred debt is a regulatory violation and must be blocked at the segmentation stage.

**Required DMN rule:** If `is_statute_barred = true`, route to `LEGAL_REVIEW` queue regardless of any other segmentation criteria.

### 4. Nightly recalculation

The `is_statute_barred` flag must be recalculated nightly by `StatuteBarredCalculationJob`. The calculation must use `cause_of_action_date` (or the most recent `limitation_clock_reset` event date, whichever is later) plus the applicable limitation period for the debt type.

See RULING-013 for detailed ruling on synchronous vs. delayed flag clearance.

---

## Edge Cases

### Statute-barred debt where debtor makes a payment

A payment on a statute-barred debt resets the limitation clock (RULING-012). The `is_statute_barred` flag must be cleared and the clock restarted from the payment date. See RULING-013 for timing of flag clearance.

### Debt approaching statute-barred status

The system should alert agents when a debt is within 90 days of becoming statute-barred, so that DWP can consider whether to initiate legal proceedings before the limitation period expires. This is an operational decision, not a mandatory system action.

### Statute-barred debt during breathing space

A breathing space moratorium does **not** stop the limitation clock. The Limitation Act and the Debt Respite Scheme operate independently. However, DWP cannot take enforcement action during breathing space to prevent a debt becoming statute-barred. This is a known legal gap — DWP legal must confirm the policy response.

---

## Open Questions Requiring DWP Client Sign-Off

### DDE-OQ-11: Confirmed limitation period per debt type

**Question:** What are the confirmed applicable limitation periods for each DWP debt type (benefit overpayment, contractual debt, court-ordered debt, other)?

**Status:** ⚠ **AWAITING DWP LEGAL SIGN-OFF — DO NOT PROCEED gate on statute-barred calculation implementation**

---

## Data Classification Flags

- `DEBT_ACCOUNT.cause_of_action_date` — Operational. Required for limitation calculation. Not Restricted.
- `DEBT_ACCOUNT.is_statute_barred` — Operational. Decision engine input. Not Restricted.
- `DEBT_STATUS_HISTORY.limitation_clock_reset` — Operational. Not Restricted.

---

## Guardrails — Builders Must Not Violate

1. No enforcement action (DCA placement, court proceedings, automated escalation) may be initiated for an account where `is_statute_barred = true`. The enforcement service must check this flag before any enforcement action, reading it at runtime from the database (not from a process variable cache).
2. `cause_of_action_date` must not be null for any account that has a collection process active. A migration or seed data validation must enforce this.
3. The `segmentation.dmn` must be updated to include a rule for `is_statute_barred = true` that routes to `LEGAL_REVIEW` **before** any other segmentation rule fires. This rule must have the highest priority in the DMN hit policy.
4. The limitation period values (e.g., 6 years) must be stored in `SYSTEM_CONFIG` and sourced from there in `StatuteBarredEvaluator` — not hardcoded. This allows DWP to update them when confirmed without a code change.

---

## Version History

| Version | Date | Author | Change |
|---|---|---|---|
| 1.0 | 23 Apr 2026 | DWP Debt Domain Expert | Initial ruling |
