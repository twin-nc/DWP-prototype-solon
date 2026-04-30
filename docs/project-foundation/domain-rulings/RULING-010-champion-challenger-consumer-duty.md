# RULING-010: Champion/Challenger — Consumer Duty Implications

**Ruling ID:** RULING-010
**Linked issue:** TBD
**Status:** `awaiting-client-sign-off`
**Date issued:** 23 April 2026
**Domain expert:** DWP Debt Domain Expert

---

## Requirement IDs Covered

- STR.CC-01 (Champion/challenger A/B testing)
- STR.CC-02 (Challenger assignment rules)
- STR.CC-03 (Challenger promotion criteria)
- DIC.11 (FCA Consumer Duty PS22/9)

---

## Regulatory Basis

- **FCA Consumer Duty (PS22/9, effective July 2023)** — applied via DWP's Consumer Duty-aligned debt management framework
- **FCA CONC 7** — treatment of customers in default
- **FCA Finalised Guidance FG21/1** — vulnerable customers

**Scope note:** FCA Consumer Duty applies to FCA-regulated firms. DWP is not an FCA-regulated firm for statutory debt recovery. However, DWP has adopted Consumer Duty-aligned principles in its debt management framework. This ruling applies DWP's policy intent, not FCA enforcement.

---

## Rule Statement

### 1. Challenger treatment must not disadvantage customers

A/B testing of collection strategies is permissible, but the CHALLENGER variant must not result in worse customer outcomes — e.g., higher rates of enforcement, less forbearance offered, or reduced access to vulnerability support.

The analytics framework must track **harm indicators** (complaints, enforcement actions, arrangement failures, vulnerability escalations) not just recovery rates. Recovery rate alone is not a sufficient measure of a compliant strategy.

### 2. Current approved policy: vulnerable customers receive CHAMPION treatment

FCA FG21/1 requires that vulnerable customers receive treatment appropriate to their needs. Assigning a vulnerable customer to an experimental CHALLENGER strategy that has not been validated for vulnerable customer outcomes is not defensible.

The current approved champion/challenger eligibility policy therefore requires:

> If `vulnerability_flag = true` OR `vulnerability_status IN [IDENTIFIED, ASSESSED, MANAGING]`, always assign `CHAMPION` regardless of split ratio.

This rule must have the highest priority in the active assignment policy and must not be overrideable by per-experiment configuration.

This is a governed policy rule, not an ordinary runtime toggle. A future change that permits vulnerable customers to receive CHALLENGER treatment must be made as a Class A champion/challenger policy change with effective dating, legal/policy rationale, approval evidence, and DWP sign-off.

### 3. Minimum test duration and sample size before promotion

A/B tests must run for a statistically meaningful period before conclusions are drawn. Promoting a winner based on insufficient data is a Consumer Duty risk — an apparently better strategy on small data may harm customers at scale.

The system must enforce:
- A minimum test duration (configurable in `SYSTEM_CONFIG.cc_min_test_duration_days`)
- A minimum sample size per variant (configurable in `SYSTEM_CONFIG.cc_min_sample_size`)

The "promote winner" action in the UI must be disabled until both thresholds are met. The values must be confirmed by DWP before the feature is built.

### 4. Harm indicator tracking is mandatory

The analytics schema must include harm indicator fields per champion/challenger assignment:
- `enforcement_actions_taken` (count)
- `arrangement_failures` (count)
- `complaints_received` (count)
- `vulnerability_escalations` (count)
- `recovery_rate` (percentage)

A CHALLENGER strategy may not be promoted if its harm indicator rates exceed the CHAMPION's rates, even if its recovery rate is higher.

---

## Edge Cases

### Customer becomes vulnerable during a CHALLENGER test

If the active champion/challenger policy excludes vulnerable customers from CHALLENGER treatment and a customer's vulnerability status changes to `IDENTIFIED` or above while assigned to a CHALLENGER variant, the system must immediately re-assign them to CHAMPION. The BPMN must check the current vulnerability status and active champion/challenger policy at each relevant service task, not only at assignment time.

### CHALLENGER strategy performs better on harm indicators but worse on recovery

DWP may choose to promote a strategy that has lower recovery rates if it has better harm indicator outcomes. The promotion logic must allow this — the system must not auto-promote on recovery rate alone. The `OPS_MANAGER` must review all indicators before promoting.

### Test result is inconclusive

If a test ends (duration or sample size met) with no statistically significant difference between CHAMPION and CHALLENGER, the system should default to retaining CHAMPION. The CHALLENGER must be marked `INCONCLUSIVE` and archived, not auto-promoted.

---

## Open Questions Requiring DWP Client Sign-Off

### DDE-OQ-12: Minimum test duration and sample size thresholds

**Question:** What are DWP's acceptable minimum thresholds for champion/challenger test duration (days) and sample size (number of accounts per variant) before promotion is permitted?

**Status:** ⚠ **AWAITING DWP SIGN-OFF**

### DDE-OQ-13: Vulnerable-customer challenger eligibility policy

**Question:** Is DWP's intended long-term policy that vulnerable customers are always excluded from CHALLENGER treatment, or should the system support a governed policy path for carefully scoped vulnerable-customer challenger inclusion?

**Status:** ⚠ **AWAITING DWP SIGN-OFF**

---

## Data Classification Flags

- `CHAMPION_CHALLENGER_ASSIGNMENT.variant` — Operational. Not Restricted.
- Harm indicator metrics — Operational (aggregate). Underlying individual account data remains subject to standard account data classification.

---

## Guardrails — Builders Must Not Violate

1. The active champion/challenger assignment policy must have the current vulnerable-customer exclusion rule as the first (highest priority) rule, before any split ratio rules, unless a later approved policy version with DWP sign-off explicitly changes this.
2. The `promote winner` API endpoint must validate that `test_duration_days >= SYSTEM_CONFIG.cc_min_test_duration_days` AND `sample_size >= SYSTEM_CONFIG.cc_min_sample_size` before executing promotion. It must return a 422 with an explanatory message if either threshold is not met.
3. The analytics schema must track harm indicators at the variant level from day one. Retrofitting harm indicator tracking after promotion is insufficient.
4. Customer re-assignment from CHALLENGER to CHAMPION under the current vulnerable-exclusion policy must write an `AUDIT_EVENT` with `event_type = CC_ASSIGNMENT_OVERRIDDEN`, `reason = VULNERABILITY_POLICY`, the active policy version, and the `variant` the customer was moved from.
5. Per-experiment configuration must not override protected-population eligibility. Protected-population eligibility changes must be made through the champion/challenger policy process.

---

## Version History

| Version | Date | Author | Change |
|---|---|---|---|
| 1.0 | 23 Apr 2026 | DWP Debt Domain Expert | Initial ruling |
