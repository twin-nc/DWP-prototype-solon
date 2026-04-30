# RULING-005: Breathing Space — REPAYMENT_ARRANGEMENT Interaction

**Ruling ID:** RULING-005
**Linked issue:** TBD
**Status:** `final`
**Date issued:** 23 April 2026
**Domain expert:** DWP Debt Domain Expert

---

## Requirement IDs Covered

- CAS.BS-05 (Arrangement monitoring during moratorium)
- CAS.BS-06 (Breach suppression during moratorium)
- DIC.14 (Debt Respite Scheme Regulations 2020)
- RPF.15 (Repayment arrangement breach handling)

---

## Regulatory Basis

- **Debt Respite Scheme (Breathing Space Moratorium and Mental Health Crisis Moratorium) (England and Wales) Regulations 2020, Regulation 7(3)**
- **FCA CONC 7** — Treatment of customers in default or arrears

---

## Rule Statement

### 1. Voluntary payments during moratorium are not prohibited

During a breathing space moratorium, DWP must not take enforcement action or demand payment. However, a voluntary repayment arrangement that the debtor themselves continues to honour is not prohibited — the debtor may choose to keep paying.

### 2. DWP must not demand continuation of the arrangement

DWP must not contact the debtor to demand continuation of the arrangement during the moratorium. The arrangement is effectively suspended from DWP's enforcement perspective, even if the debtor is making payments voluntarily.

### 3. Arrangement monitoring must be suspended, not cancelled

The `ARRANGEMENT_MONITORING_PROCESS` must be **suspended** (not cancelled) during breathing space. The process instance must be placed in a wait state linked to the `LEGAL_HOLD` record for the moratorium.

### 4. Missed payments during moratorium must not trigger breach

If the debtor misses a payment during breathing space, the system must not:
- Create an `ARRANGEMENT_BREACH` record
- Send a payment warning letter
- Escalate the case
- Count the missed payment towards a breach threshold

### 5. Monitoring resumes from last valid state

When breathing space ends, the monitoring process must resume from its last valid state — not from a fresh start, and not treating the moratorium period as a breach period. The breach counter must not include any missed payments that occurred during the moratorium.

---

## Edge Cases

### Debtor makes a payment during moratorium

A voluntary payment during breathing space constitutes a clock-reset event under the Limitation Act 1980 (RULING-012), provided the payment is debtor-initiated. The payment should be recorded and allocated normally. The monitoring process remains suspended.

### Arrangement expiry date falls within moratorium

If the arrangement's scheduled end date falls within the moratorium period, the arrangement must not be automatically closed or expired. The monitoring process is suspended; the end date calculation must account for the suspension period. On moratorium lift, the arrangement should be reviewed and extended if appropriate.

### Mental Health Crisis Moratorium (no fixed end date)

Where the moratorium is `BREATHING_SPACE_MENTAL_HEALTH_CRISIS`, there is no timer to trigger monitoring resumption. The monitoring process remains suspended until a manual event (professional sign-off) ends the moratorium. See RULING-001.

---

## Data Classification Flags

No new Restricted or special-category-adjacent elements introduced by this ruling.

---

## Guardrails — Builders Must Not Violate

1. `ARRANGEMENT_BREACH` records must never be created during a period where `SUPPRESSION_LOG` contains an active entry with `suppression_reason = BREATHING_SPACE_STATUTORY` or `MENTAL_HEALTH_CRISIS_STATUTORY`.
2. The BPMN timer boundary event that evaluates payment receipt must check `breathing_space_flag = true` (resolved from the suppression service, not a cached process variable) before evaluating payment receipt.
3. On moratorium lift, the monitoring process must re-read the arrangement's last confirmed payment date and continue from that point. It must not re-evaluate the full arrangement history as if it were a new breach check.
4. The suspension and resumption of the monitoring process must each write an `AUDIT_EVENT` with `event_type = ARRANGEMENT_MONITORING_SUSPENDED` / `ARRANGEMENT_MONITORING_RESUMED`, with the linked `LEGAL_HOLD.id` as the reason.

---

## Version History

| Version | Date | Author | Change |
|---|---|---|---|
| 1.0 | 23 Apr 2026 | DWP Debt Domain Expert | Initial ruling |
