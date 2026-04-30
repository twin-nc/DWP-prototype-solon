# RULING-007: I&E Assessment — FCA Affordability Obligations

**Ruling ID:** RULING-007
**Linked issue:** TBD
**Status:** `awaiting-client-sign-off`
**Date issued:** 23 April 2026
**Domain expert:** DWP Debt Domain Expert

---

## Requirement IDs Covered

- RPF.8 (Income and expenditure assessment threshold)
- RPF.9 (I&E staleness and refresh)
- RPF.10 (Minimum instalment floor)
- DIC.9 (FCA CONC 7.3 affordability)
- DIC.10 (FCA Consumer Duty PS22/9)

---

## Regulatory Basis

- **FCA CONC 7.3** — Treatment of customers in default or arrears difficulties; taking account of current financial circumstances
- **FCA Consumer Duty (PS22/9, effective July 2023)** — Firms must act to deliver good customer outcomes
- **FCA Finalised Guidance FG21/1** — Vulnerable customers
- **Social Security Administration Act 1992** — Statutory deduction from benefit

---

## Rule Statement

### 1. I&E threshold applies to outstanding balance, not original amount

FCA affordability obligations are triggered by the customer's current financial circumstances, not the original debt amount. The I&E threshold in RPF.8 (£500) must apply to the **outstanding balance**, not the original amount. A debt starting at £600 with £480 outstanding still warrants an I&E if the customer is in difficulty — the threshold is a minimum floor, not an exemption ceiling.

### 2. I&E staleness and refresh

FCA CONC 7.3.4G requires that firms take account of customers' circumstances at the time of the arrangement.

**Rule:**
- An I&E assessment older than **12 months** triggers a soft warning; agents must confirm or refresh before creating a new arrangement.
- If the customer's circumstances are known to have changed (e.g., benefit status change, new vulnerability flag), the I&E must be refreshed regardless of age.
- The 12-month threshold is subject to DWP policy confirmation — DWP may set a shorter staleness period.

### 3. Deduction from benefit is exempt from FCA affordability rules

Deduction from benefit is a statutory recovery mechanism under the Social Security Administration Act 1992 — it is not a voluntary arrangement in the FCA sense. FCA CONC affordability rules do not apply to statutory deductions. However, DWP policy (not FCA) may still require an I&E to confirm the deduction rate does not cause hardship.

### 4. Minimum instalment floor must permit £0 when necessary

The `plan_suitability.dmn` currently sets a minimum instalment of £5 for disposable income < £50. FCA Consumer Duty requires that the arrangement be genuinely affordable.

**Ruling:** A £5 minimum on zero or near-zero disposable income is not defensible under Consumer Duty. The system must permit a **£0 instalment** (payment holiday / forbearance period) when disposable income is genuinely insufficient, rather than enforcing a nominal floor that the customer cannot sustain.

The DMN must add a rule: if `disposable_income <= 0`, then `minimum_instalment = 0` and `arrangement_type = FORBEARANCE`.

---

## Edge Cases

### Customer in breathing space requesting arrangement

If a customer in breathing space requests a repayment arrangement, the I&E must still be completed for the arrangement to be valid. The breathing space moratorium does not exempt DWP from conducting an affordability assessment — it only prohibits enforcement action.

### I&E completed by third-party debt adviser

An I&E completed by a FCA-regulated debt advice charity (e.g., StepChange, National Debtline) should be treated as equivalent to a DWP-conducted I&E, provided the document is dated and signed. The staleness clock applies from the date of the third-party I&E.

### Customer refuses I&E

If a customer refuses to provide income and expenditure information, DWP may set a default arrangement based on a standard national minimum. This is an internal policy decision. AWAITING-CLIENT-SIGN-OFF on the default rate.

---

## Open Questions Requiring DWP Client Sign-Off

### DDE-OQ-08: DWP staleness period for I&E

**Question:** Does DWP policy set a staleness period shorter than 12 months for I&E assessments? What is the confirmed DWP policy?

**Status:** ⚠ **AWAITING DWP SIGN-OFF**

### DDE-OQ-09: I&E requirement for benefit deduction arrangements

**Question:** Does DWP require an I&E assessment before setting a deduction from benefit arrangement, for hardship-prevention purposes?

**Status:** ⚠ **AWAITING DWP SIGN-OFF**

---

## Data Classification Flags

- `IE_ASSESSMENT.disposable_income` — **Restricted** (financial data)
- `IE_ASSESSMENT.income_details` / `IE_ASSESSMENT.expenditure_details` — **Restricted** (detailed financial data)
- `IE_ASSESSMENT.completed_by` — Operational

---

## Guardrails — Builders Must Not Violate

1. The I&E staleness check must compare `IE_ASSESSMENT.assessment_date` against both: (a) 12 months ago, and (b) the date of the last known circumstances change on the account. The shorter of the two determines staleness.
2. The DMN `plan_suitability.dmn` must have a rule for `disposable_income <= 0` that routes to `FORBEARANCE` before any minimum instalment rule fires.
3. The `arrangement_type = FORBEARANCE` must set a review date (e.g., 3 months) when the customer's circumstances must be reassessed. A forbearance plan must not be open-ended.
4. The I&E threshold check must use `DEBT_ACCOUNT.outstanding_balance` at the time of the check, not `DEBT_ACCOUNT.original_amount`.

---

## Version History

| Version | Date | Author | Change |
|---|---|---|---|
| 1.0 | 23 Apr 2026 | DWP Debt Domain Expert | Initial ruling |
