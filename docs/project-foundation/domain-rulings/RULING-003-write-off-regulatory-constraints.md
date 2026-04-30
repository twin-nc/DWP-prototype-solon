# RULING-003: Write-Off — Regulatory and Policy Constraints

**Ruling ID:** RULING-003
**Linked issue:** TBD
**Status:** `awaiting-client-sign-off`
**Date issued:** 23 April 2026
**Domain expert:** DWP Debt Domain Expert

---

## Requirement IDs Covered

- DW.60 (Write-off approval workflow)
- DW.61 (Delegated authority thresholds)
- DIC.23 (Managing Public Money compliance)
- DIC.24 (Audit trail for write-off of public funds)

---

## Regulatory Basis

- **DWP Debt Management Policy**
- **Managing Public Money (HM Treasury)** — write-off of public funds, delegated authority, separation of duties
- **Government Internal Audit Agency standards** — write-off approval audit requirements
- **Note:** DWP debt write-off is **not** governed by the Consumer Credit Act 1974. DWP debts are statutory debts, not regulated credit agreements.

---

## Rule Statement

### 1. MPM write-off requirements

Write-off of public funds under Managing Public Money requires:

1. Proper authorisation at the appropriate delegated authority level.
2. A sufficient audit trail to satisfy National Audit Office and Public Accounts Committee scrutiny.
3. Accounting Officer sign-off for write-offs above threshold (DWP-specific thresholds to be confirmed — see open question).

### 2. Delegated authority is role-based, not transaction-routing

Write-off authority is a role-based delegated authority. A `TEAM_LEADER` with authority for amounts up to £2,000 does not need to route a £300 write-off through an `AGENT` — they have authority to approve it directly.

### 3. Self-approval prohibition

A `TEAM_LEADER` or any role approving their own write-off request (self-approval) is prohibited under separation of duties principles in MPM.

**DMN rule:** The approval is granted if and only if:
1. The requestor's role limit covers the amount, AND
2. The approver is **not** the same user as the requestor.

This must be enforced in the DMN and at the service layer — not only in the UI.

### 4. Threshold values are assumed — not confirmed

The current DMN values (£500 / £2,000 / £10,000) are assumed. They must be confirmed against DWP's actual delegated authority framework before the write-off feature is built.

### 5. COMPLIANCE is not an approval authority

The current DMN routes amounts above £10,000 to `COMPLIANCE`. This is **incorrect**. `COMPLIANCE` is a review and oversight function, not an approval authority. Amounts above `OPS_MANAGER` limit must route to a Senior Responsible Owner or Accounting Officer role. The correct escalation role must be confirmed by DWP.

---

## Edge Cases

### Write-off of residual balance after partial payment

A small residual balance (e.g., £0.01) remaining after a final payment is still a write-off of public funds. A de minimis threshold may be set to allow system auto-approval below that amount, but this threshold must be confirmed in DWP's delegated authority framework.

### Write-off of jointly held debt

Write-off on a joint account requires the same delegated authority rules. If the account is being split (RULING-004), the write-off of any residual must be approved before the split completes.

### Write-off during breathing space

Writing off a debt during a breathing space moratorium is not prohibited by the Debt Respite Scheme — the moratorium prohibits collection action, not debt forgiveness. However, DWP policy may restrict write-off during moratorium. AWAITING-CLIENT-SIGN-OFF.

---

## Open Questions Requiring DWP Client Sign-Off

### DDE-OQ-03: Confirmed delegated authority thresholds

**Question:** What are DWP's confirmed delegated authority thresholds for write-off: AGENT / TEAM_LEADER / OPS_MANAGER / Accounting Officer?

**Status:** CLOSED — 2026-04-24

**Resolution:** SRO = Senior Responsible Officer. In-system actor provisioned as Keycloak realm role `SRO`. Provisional thresholds set as seed values in `SYSTEM_CONFIG`: Tier 1 £500 (AGENT and above), Tier 2 £2,000 (TEAM_LEADER and above), Tier 3 £10,000 (OPS_MANAGER and above), Tier 4 £50,000 (SRO only). All values remain provisional pending DWP client confirmation (DDE-OQ-02 remains open). Thresholds are in `SYSTEM_CONFIG`, not hardcoded — can be updated without a code change on confirmation.

### DDE-OQ-04: Correct escalation role above OPS_MANAGER

**Question:** What is the correct role for write-offs above OPS_MANAGER limit — Senior Responsible Owner, Accounting Officer, or other?

**Status:** CLOSED — 2026-04-24

**Resolution:** Above-ceiling amounts (above Tier 4 / SRO ceiling) do not produce a service-layer rejection. They produce: (1) `AUDIT_EVENT` of type `MANUAL_REVIEW_REQUIRED`, (2) routing to COMPLIANCE as a notification, (3) empty candidate group guard prevents silent process continuation. No tender requirement mandates a hard block. This is the same pattern as write-off self-approval prevention. See `RBAC-IMPLEMENTATION-DECISIONS.md` section 3.4 and `ADR-004`.

---

## Data Classification Flags

- `WRITE_OFF.approved_by` — Operational. Audit trail field. Required for NAO/PAC scrutiny.
- `WRITE_OFF.reason_code` — Operational.
- `WRITE_OFF.amount` — Financial. Subject to internal financial controls.

---

## Guardrails — Builders Must Not Violate

1. The DMN must enforce self-approval prohibition at the rule engine level. The service layer must also validate that `approver_id != requestor_id` before committing a write-off.
2. Every write-off must write an `AUDIT_EVENT` with `event_type = WRITE_OFF_APPROVED`, including `amount`, `approver_id`, `requestor_id`, `reason_code`, and `delegated_authority_tier`.
3. The £500 / £2,000 / £10,000 thresholds in the DMN must be sourced from `SYSTEM_CONFIG`, not hardcoded, so they can be updated when DWP confirms the correct values without a code change.
4. No write-off may be committed without an `approved_by` user ID — the system must not allow system-generated auto-approvals above the confirmed de minimis threshold.

---

## Version History

| Version | Date | Author | Change |
|---|---|---|---|
| 1.0 | 23 Apr 2026 | DWP Debt Domain Expert | Initial ruling |
