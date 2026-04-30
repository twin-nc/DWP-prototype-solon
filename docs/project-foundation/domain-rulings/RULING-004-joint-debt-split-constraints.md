# RULING-004: Joint Debt — Regulatory Constraints on Account Split

**Ruling ID:** RULING-004
**Linked issue:** TBD
**Status:** `awaiting-client-sign-off`
**Date issued:** 23 April 2026
**Domain expert:** DWP Debt Domain Expert

---

## Requirement IDs Covered

- DW.72 (Joint account split workflow)
- DW.73 (Joint and several liability)
- DIC.19 (Limitation Act interaction with joint debt)
- DIC.20 (Insolvency hold on joint accounts)

---

## Regulatory Basis

- **DWP benefit debt policy** — joint overpayment liability
- **Limitation Act 1980** — limitation period runs from cause of action
- **Insolvency Act 1986** — LEGAL_HOLD survives account restructuring

---

## Rule Statement

### 1. Joint and several liability requires legal sign-off before split

Joint DWP debt (e.g., joint Universal Credit overpayment) typically carries joint and several liability — both parties are liable for the full amount. Splitting the debt into two independent accounts changes the legal liability structure. This is a **policy decision requiring DWP legal sign-off**, not a system configuration choice.

### 2. Active arrangement blocks split

If a `REPAYMENT_ARRANGEMENT` with `status = ACTIVE` exists on the joint account, the split must be blocked until the arrangement is formally cancelled with appropriate notifications to both parties. This is not merely a system integrity question — cancelling an arrangement has customer notification obligations.

**Invariant:** `split` action is blocked if `REPAYMENT_ARRANGEMENT.status = ACTIVE` for any arrangement linked to the account.

### 3. LEGAL_HOLD blocks split

A court order or insolvency hold on a joint account cannot be unilaterally dissolved by splitting the account. The hold travels with the debt obligation, not the account record.

**Invariant:** `split` action is blocked if any `LEGAL_HOLD` is active on the account.

### 4. Limitation clock preserved on child accounts

If the joint debt is approaching statute-barred status, splitting the account does not reset the limitation clock independently for each party. The limitation period runs from the original cause of action. Child accounts created by a split must inherit `cause_of_action_date` from the parent account — they must not use the split date as the start of the limitation period.

### 5. Odd-penny residual write-off requires approval

The automatic write-off of a residual penny via `reason_code = JOINT_SPLIT_RESIDUAL` technically constitutes a write-off of public funds. Under Managing Public Money, even a 1p write-off requires an authorised approver.

**Recommended rule:** Auto-approve residuals at or below a confirmed de minimis threshold (e.g., ≤ £0.05) with the system acting as approver, logged with `approved_by = SYSTEM_AUTO` and `reason_code = DE_MINIMIS_SPLIT_RESIDUAL`. This threshold must be confirmed in DWP's delegated authority framework.

---

## Edge Cases

### One party enters insolvency after split has been requested

If a split is in progress (e.g., pending legal sign-off) and one party enters insolvency before the split completes, the split must be abandoned and the account must revert to full joint hold status. The insolvency flag takes precedence over any in-flight administrative action.

### Deceased party on joint account

If one party is deceased (see RULING-006), the split may be required as part of estate administration. However, the standard split workflow must not be used — the estate administration track is a separate workflow. The standard split invariants (active arrangement, LEGAL_HOLD) still apply.

---

## Open Questions Requiring DWP Client Sign-Off

### DDE-OQ-05: Legal sign-off on liability change from split

**Question:** Does DWP legal confirm that splitting a joint and severally liable account into two independent accounts is permissible under DWP benefit debt policy, and under what conditions?

**Status:** ⚠ **AWAITING DWP LEGAL SIGN-OFF**

### DDE-OQ-06: De minimis residual write-off threshold

**Question:** What is DWP's confirmed de minimis threshold for auto-approved split residuals under the delegated authority framework?

**Status:** ⚠ **AWAITING DWP SIGN-OFF**

---

## Data Classification Flags

- `DEBT_ACCOUNT.cause_of_action_date` — Operational. Must be preserved on split. Not Restricted.
- `JOINT_ACCOUNT_LINK` records — Operational.

---

## Guardrails — Builders Must Not Violate

1. The `splitAccount` service method must validate both invariants (no active arrangement, no active LEGAL_HOLD) in a single pre-condition check before any account mutation begins.
2. Child account creation must copy `cause_of_action_date` from the parent account. This field must not default to the split date.
3. Residual write-offs generated during split must be written to the `WRITE_OFF` table with `reason_code = DE_MINIMIS_SPLIT_RESIDUAL` and `approved_by = SYSTEM_AUTO`, and must generate an `AUDIT_EVENT`.
4. Cancellation of an active `REPAYMENT_ARRANGEMENT` as a precondition to split must trigger customer notification (both parties) before the cancellation is committed. This is a notification obligation, not an optional courtesy.

---

## Version History

| Version | Date | Author | Change |
|---|---|---|---|
| 1.0 | 23 Apr 2026 | DWP Debt Domain Expert | Initial ruling |
