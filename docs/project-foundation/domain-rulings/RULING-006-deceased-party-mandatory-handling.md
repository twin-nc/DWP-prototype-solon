# RULING-006: Deceased Party — Mandatory Handling

**Ruling ID:** RULING-006
**Linked issue:** TBD
**Status:** `awaiting-client-sign-off`
**Date issued:** 23 April 2026
**Domain expert:** DWP Debt Domain Expert

---

## Requirement IDs Covered

- DW.80 (Deceased party flag and workflow)
- DW.81 (Estate administration track)
- DIC.25 (FCA CONC 7.14–7.17 deceased customer treatment)
- DIC.26 (DWP bereavement policy)

---

## Regulatory Basis

- **FCA CONC 7.14–7.17** — Treatment of deceased customers
- **DWP bereavement policy**
- **UK GDPR** — Data of deceased persons is outside GDPR scope, but DWP policy applies internal standards equivalent to GDPR for a defined period post-death

---

## Rule Statement

This ruling establishes a **DO NOT PROCEED gate** on Sprint 1 account management stories until a deceased handling workflow is defined by the BA and confirmed by DWP.

### 1. Immediate communication suppression on deceased flag

Once `PARTY.deceased_flag = true` is set, all automated collections contact must immediately cease. This is equivalent to statutory suppression (`DECEASED_MANDATORY`). There is no grace period or agent override for this suppression.

### 2. Debt may be pursued against the estate

DWP may continue to pursue the debt against the deceased's estate. This is a different legal relationship and requires a distinct workflow: the **estate administration track**. This track is separate from the standard collection process and must not share state with it.

### 3. Communications must not be addressed to the deceased

The system must not send communications addressed to the deceased person. Communications may be sent to the executor or administrator of the estate, but only after their authority has been verified. Estate communications use a new category: `ESTATE_ADMINISTRATION` (see RULING-011).

### 4. Joint accounts require immediate review

Joint accounts where one party is deceased require immediate review. The surviving party retains liability, but the account treatment must change:
- The deceased party's share of the liability must be separated from the surviving party's share.
- The surviving party's account must not be placed in `DECEASED_MANDATORY` suppression — only the deceased party's contact details are suppressed.

### 5. Active arrangements must be suspended pending estate review

Existing `REPAYMENT_ARRANGEMENT` records on the account must be suspended pending estate administration review. They must not be cancelled — the debt obligation may survive against the estate and the arrangement may need to be re-established with the executor.

### 6. Automated collection must not resume without estate review

The collection process must not automatically resume after a deceased flag is set. The account must be routed to an agent queue for manual review and estate administration workflow initiation.

---

## Edge Cases

### Deceased flag set in error

If `PARTY.deceased_flag` is set incorrectly and the party is alive, the flag must be reversible by an authorised agent with TEAM_LEADER or above. The reversal must write an `AUDIT_EVENT` explaining the correction. All suppressed communications during the error period must be reviewed — they may need to be regenerated and sent.

### Multiple parties, one deceased

Where an account has multiple linked parties and one is deceased, only communications addressed to the deceased party are suppressed. Other parties' communications continue normally, subject to any other suppression flags.

---

## Open Questions Requiring DWP Client Sign-Off

### DDE-OQ-07: Estate pursuit policy

**Question:** What is DWP's confirmed policy on pursuing benefit debt against a deceased debtor's estate? Under what conditions will DWP initiate estate administration proceedings?

**Impact:** Determines whether the estate administration track must be built in scope for the initial release.

**Status:** ⚠ **AWAITING DWP SIGN-OFF — DO NOT PROCEED gate on Sprint 1 account management stories**

---

## Data Classification Flags

- `PARTY.deceased_flag` — Operational. Sensitive but not Restricted.
- `PARTY.death_notification_date` — Operational.
- Estate administration case notes — may contain health or bereavement data. Handle as **Restricted**.

---

## Guardrails — Builders Must Not Violate

1. Setting `PARTY.deceased_flag = true` must atomically activate a `SUPPRESSION_LOG` entry with `suppression_reason = DECEASED_MANDATORY`. These two writes must occur in the same database transaction.
2. `CommunicationSuppressionService.isPermitted()` must return `false` for all `communication_category` values (including `NON_COLLECTION`) when `suppression_reason = DECEASED_MANDATORY` is active. Exception: `ESTATE_ADMINISTRATION` category is permitted once executor authority is verified.
3. The collection BPMN process must check `deceased_flag` at every service task gateway before sending any communication or taking any collection action. It must not rely on the flag value read at process start.
4. No automated process may clear `DECEASED_MANDATORY` suppression. It may only be cleared by an explicit agent action with supervisor-level authority.

---

## Version History

| Version | Date | Author | Change |
|---|---|---|---|
| 1.0 | 23 Apr 2026 | DWP Debt Domain Expert | Initial ruling |
