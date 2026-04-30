# RULING-014: Repayment Arrangement Concurrency — One or Many Active Arrangements per Account

**Ruling ID:** RULING-014
**Linked issue:** TBD — must be linked before BA writes acceptance criteria for RPF.5
**Status:** `awaiting-client-sign-off` (partial — see open questions below)
**Date issued:** 29 April 2026
**Domain expert:** DWP Debt Domain Expert

---

## Requirement IDs Covered

- RPF.5 (Multiple concurrent arrangements per customer)
- RPF.1 (Repayment arrangement creation)
- RPF.6 (Direct debit setup and management within arrangements)
- RPF.9 (Arrangement renegotiation following breach)
- RPF.21 (Arrangement cancellation with reason codes)
- RPF.11 (Payment tracking against each arrangement)
- DW.87 (Payment Allocation System — allocation instruction ownership)
- DW.23 (Payment processing — payment channel interaction)

---

## Regulatory Basis

- **DWP Benefit Overpayment Recovery Staff Guide (May 2025)** — recovery channel hierarchy; deduction and voluntary payment coexistence
- **Universal Credit Regulations 2013 (SI 2013/376), reg 66–71** — deduction rates and caps; cap applies to all deductions in aggregate, not per channel
- **FCA Consumer Duty / Treating Customers Fairly** — affordability obligations; a new arrangement must not create unaffordable cumulative commitments
- **FCA CONC 7** — prohibition on coercive or duplicative demand for the same sum
- **ADR-001** (Process Instance per Account) — architectural authority for what "per account" means; confirms one process instance per account
- **RULING-001-benefit-deduction-mechanics.md** — deduction-from-benefit mechanics; cap rule; allocation order; DCMS role boundary
- **DWP Payment Allocation System policy (DW.87)** — DWP system provides the allocation instruction; DCMS follows it

---

## Definitions Used in This Ruling

| Term | Meaning |
|---|---|
| **Customer** | A Person record in DCMS — one person, potentially linked to many accounts |
| **Account** | A single debt account (one overpayment, advance, or Social Fund loan); one process instance per ADR-001 |
| **Arrangement** | A `REPAYMENT_ARRANGEMENT` record linked to exactly one Account |
| **Active arrangement** | `REPAYMENT_ARRANGEMENT.status = ACTIVE` |
| **Recovery channel** | The payment mechanism: benefit deduction instruction, direct debit mandate, voluntary standing order/bank transfer |

---

## Rule Statements

### Rule 1: Multiple active arrangements across different accounts — PERMITTED

A single customer (Person) may have multiple active `REPAYMENT_ARRANGEMENT` records simultaneously, provided each arrangement is linked to a **different Account**. This is the normal operating state for customers with more than one debt.

**Basis:** ADR-001 establishes one process instance per Account. Accounts progress independently. There is no regulatory rule preventing a person from having an active arrangement on each of their debts simultaneously. FCA Consumer Duty requires that the **aggregate** financial commitment across all arrangements is assessed for affordability (see Rule 5), but it does not prohibit concurrent arrangements across accounts.

**Example:** Customer has Account A (UC overpayment) with an active direct debit arrangement, and Account B (Social Fund loan) with an active benefit deduction instruction. Both are permitted simultaneously.

---

### Rule 2: Multiple active arrangements on the same account — NOT PERMITTED

A single Account must have at most **one** `REPAYMENT_ARRANGEMENT` with `status = ACTIVE` at any point in time.

**Basis:** A repayment arrangement is a commitment to recover a specific debt at a specific rate via a specific channel. Permitting two active arrangements on the same account would mean:

1. Two separate recovery demands for the same debt principal — a breach of FCA CONC 7 (prohibition on duplicate demand for the same liability).
2. Irreconcilable allocation logic — the DWP Payment Allocation System provides a single allocation instruction per account; two active arrangements would conflict over which channel receives the payment and how the balance is reduced.
3. Breach of the benefit deduction cap rule (RULING-001 Q4) — a benefit deduction instruction and a direct debit mandate running simultaneously on the same account would each reduce the same balance independently, creating a risk of double-recovery.

**Invariant for Builders:** The `repaymentplan` module must enforce: at most one `REPAYMENT_ARRANGEMENT` with `status = ACTIVE` per Account at any insert or update operation. This check must be a pre-condition enforced at the service layer, not only at the database level.

---

### Rule 3: Benefit deduction and voluntary payment — DIFFERENT THINGS

A benefit deduction instruction is **not** a `REPAYMENT_ARRANGEMENT` in the DCMS data model. The deduction instruction is sent to the external DWP Benefits Payment System (RULING-001 Q8); it is an operational instruction, not a voluntary plan.

**A benefit deduction instruction and a voluntary repayment arrangement may coexist on the same account only if:**

(a) The DWP Payment Allocation System instruction confirms that both recovery channels are active simultaneously; AND

(b) The combined recovery amount does not cause double-recovery of the same principal — i.e., payments received from both channels are allocated to the same account balance and the balance is not debited twice for the same period's collection.

**This is a DWP policy question.** Whether DWP operationally runs deductions and a concurrent voluntary arrangement on the same debt is not defined in available public guidance. The provisional rule is:

**Provisional (pending DWP sign-off):** When a benefit deduction instruction is active for an account, no additional `REPAYMENT_ARRANGEMENT` with `payment_method = DIRECT_DEBIT` or `payment_method = VOLUNTARY_TRANSFER` may be created for the same account without explicit DWP agent authorisation recorded in the audit trail. The deduction instruction is treated as the sole active recovery mechanism for that account.

**Status of this sub-rule:** ⚠ `awaiting-client-sign-off` — see DDE-OQ-14 below.

---

### Rule 4: What happens when a new arrangement is created on an account that already has one — supersession, not silent replacement

When a new `REPAYMENT_ARRANGEMENT` is created for an Account that already has an arrangement with `status = ACTIVE`, the following sequence is mandatory:

1. **The existing arrangement must be formally ended** before the new one can become `ACTIVE`. The existing arrangement's `status` must transition to `CANCELLED` or `SUPERSEDED` (with an appropriate `cancellation_reason_code`) in the same transaction that creates the new arrangement.
2. **Customer notification is mandatory** before the existing arrangement is cancelled, unless the new arrangement is created at the customer's own request in the same interaction. The notification obligation derives from the same principle as RULING-004 Rule 2 — cancelling an arrangement has customer notification obligations.
3. **An `AUDIT_EVENT` must be written** for the cancellation of the old arrangement and the creation of the new one, with the relationship (`superseded_by_arrangement_id`) recorded.
4. **The Limitation Act clock is not reset** solely by cancelling an old arrangement and creating a new one. The clock reset event is the creation of the new arrangement (a new written acknowledgement) — see RULING-012. The cancellation of the old arrangement does not independently reset the clock.

**Invariant for Builders:** The `createArrangement` service method must check for an existing active arrangement on the account. If one exists, the method must either (a) reject the request with an explicit error requiring the caller to cancel the existing arrangement first, or (b) perform the atomic supersession sequence within a single `@Transactional` boundary.

Option (b) is preferred for system-initiated renegotiation (e.g., post-breach renegotiation per RPF.9). Option (a) is preferred for agent-initiated new arrangements to force explicit agent confirmation that the prior arrangement is being cancelled.

---

### Rule 5: Aggregate affordability check across all active arrangements

When creating a new arrangement on any Account for a customer, DCMS must consider the customer's **aggregate** repayment commitment across all their active arrangements (all accounts). This is an FCA Consumer Duty obligation (RULING-007).

If the proposed new arrangement, when added to the customer's existing active arrangements, would produce a total monthly commitment that exceeds what the most recent I&E assessment indicates the customer can afford, the arrangement creation must:

- Require explicit agent override with a recorded reason; OR
- Be blocked pending a new I&E assessment.

This check applies regardless of which account the new arrangement is being created for.

---

### Rule 6: Direct debit and breathing space — arrangement creation is blocked

If the customer's Person record has an active breathing space moratorium, no new `REPAYMENT_ARRANGEMENT` may be created for any Account linked to that Person during the moratorium period. RULING-005 governs the suspension of monitoring for existing arrangements. RULING-001-breathing-space-comms-suppression governs communication suppression. This ruling adds:

**No new arrangement may be created during breathing space.** Creating a new arrangement during a moratorium would constitute a demand for payment, which is prohibited under the Debt Respite Scheme (Breathing Space Moratorium and Mental Health Crisis Moratorium) (England and Wales) Regulations 2020.

**Invariant for Builders:** The `createArrangement` service method must check the active suppression log for `suppression_reason = BREATHING_SPACE_STATUTORY` or `MENTAL_HEALTH_CRISIS_STATUTORY` on any Account linked to the Person before permitting arrangement creation. If an active entry exists, arrangement creation must be rejected with reason code `ARRANGEMENT_CREATION_BLOCKED_BREATHING_SPACE`.

---

## Interaction Precedence Table

When multiple regulatory regimes apply simultaneously, this table governs:

| Regime | Effect on arrangement creation | Effect on existing active arrangement |
|---|---|---|
| Breathing space (standard) | Blocked — no new arrangements | Suspended per RULING-005; monitoring paused |
| Breathing space (mental health crisis) | Blocked — no new arrangements | Suspended indefinitely per RULING-005 |
| Insolvency (IVA, bankruptcy, DRO) | Blocked — no enforcement or new recovery commitments | Must be suspended and reviewed; no breach processing |
| Deceased customer | Blocked — no new arrangements | Cancelled via deceased handling workflow (RULING-006) |
| Active existing arrangement on same account | Blocked until prior arrangement cancelled/superseded | N/A (this is the prior arrangement) |
| Fraud marker active | Not blocked by fraud marker alone; agent authorisation required | Not suspended by fraud marker alone; monitoring continues |

---

## Edge Cases

### Arrangement created on an account already recovering via benefit deduction

See Rule 3 above. This is the primary open question (DDE-OQ-14). Until DWP confirms, the provisional rule is: benefit deduction instruction takes precedence; no concurrent voluntary arrangement on the same account without explicit authorisation.

### Customer renegotiates terms mid-arrangement

When a customer requests revised terms (e.g., lower monthly payment due to changed income), this is a renegotiation under RPF.9, not a new arrangement. The existing arrangement's terms may be updated in-place with version history recorded, without requiring formal cancellation and recreation, provided the resulting arrangement is still a single active arrangement on the account. The Limitation Act clock reset implications of a renegotiation must be assessed per RULING-012.

Whether an in-place term change constitutes a new written acknowledgement of the debt (resetting the Limitation Act clock) is an open question — see RULING-012 DDE-OQ-13.

### Two accounts, same debt type, same customer — independent arrangements

If a customer has two separate UC overpayment accounts (e.g., one from 2024 and one from 2025 — separate assessment periods), each account is independent under ADR-001. Each may have its own active arrangement independently. These are not "concurrent arrangements on the same account" — they are arrangements on different accounts.

### Arrangement breach followed immediately by new arrangement request

If an arrangement on Account A is in `status = BREACHED` and the customer proposes a new arrangement, the breach status does not automatically block creation. However:

1. The BREACHED arrangement must be formally cancelled (with reason code `RENEGOTIATED`) before the new arrangement is made ACTIVE.
2. The audit trail must show the continuity of recovery effort for that account.
3. If the customer has repeatedly breached arrangements (see DW.84 — broken arrangement history in treatment rules), the strategy module may decline to create a new arrangement below supervisor level.

---

## Data Classification Flags

- `REPAYMENT_ARRANGEMENT` records — Restricted (financial account data per STD-SEC-003 § 5). Access limited to DCMS caseworkers, supervisors, and compliance. Not to be returned in API responses beyond the account's assigned agents without RBAC check.
- `superseded_by_arrangement_id` link field — Operational. Not Restricted in isolation; Restricted when returned with arrangement record containing financial terms.
- `cancellation_reason_code` — Operational. Not Restricted.

No new special-category-adjacent (GDPR Art. 9) elements introduced by this ruling.

---

## Open Questions Requiring DWP Client Sign-Off

### DDE-OQ-14: Concurrent benefit deduction and voluntary arrangement on the same account

**Question:** Does DWP policy permit an account to simultaneously have an active benefit deduction instruction to DWP Benefits Payment System AND an active voluntary repayment arrangement (direct debit or standing order) for the same debt?

**If yes:** What is the allocation rule when payments arrive from both channels in the same period? Who instructs the Payment Allocation System?

**If no:** Which channel takes precedence, and what workflow suspends or cancels the other channel when a new recovery mechanism starts?

**Status:** ⚠ **AWAITING DWP CLIENT SIGN-OFF**
**Owner:** Delivery Lead to escalate to DWP policy team
**Blocking:** RPF.5, RPF.6, DW.23 — builders must not implement concurrent dual-channel recovery logic until this is answered

### DDE-OQ-15: Notification obligation timing when superseding an arrangement

**Question:** DWP policy and FCA Consumer Duty require customer notification when an arrangement is cancelled. What is the required notification lead time when an existing arrangement is cancelled in order to create a superseding arrangement — specifically, where the customer has requested the change themselves in the same interaction?

**Provisional position:** Where the customer initiates the renegotiation in the same agent interaction, notification at the point of the new arrangement confirmation letter (sent immediately) satisfies the obligation. Where the system initiates cancellation (e.g., automated post-breach renegotiation), a separate advance notification is required.

**Status:** ⚠ **AWAITING DWP CLIENT SIGN-OFF**
**Owner:** Delivery Lead to confirm with DWP operational policy
**Blocking:** RPF.9, RPF.21 — acceptance criteria for breach renegotiation must not be finalised until this is confirmed

---

## Guardrails — Builders Must Not Violate

1. At most one `REPAYMENT_ARRANGEMENT` with `status = ACTIVE` per Account at any time. This invariant must be enforced at the service layer in the `repaymentplan` module, not only as a database constraint.

2. `createArrangement` must check for an active breathing space or insolvency suppression entry on the Person before permitting creation. If found, reject with the appropriate reason code. This check must query the live suppression log, not a cached process variable.

3. When a new arrangement supersedes an existing one, the cancellation of the old arrangement and the creation of the new arrangement must occur within a single `@Transactional` boundary. Partial state — old arrangement cancelled but new arrangement not yet created, or vice versa — must never be committed.

4. The `superseded_by_arrangement_id` field on the cancelled arrangement must be populated in the same transaction. It must not be populated in a subsequent update.

5. An `AUDIT_EVENT` must be written for every arrangement status transition (ACTIVE to CANCELLED, ACTIVE to SUPERSEDED, ACTIVE to SUSPENDED, ACTIVE to BREACHED). The event must include the arrangement ID, the triggering agent or system, and the reason code. COM06/COM07 required fields apply.

6. Aggregate affordability check (Rule 5) must be performed before any arrangement is confirmed `ACTIVE`, not after. If the check fails and the agent proceeds via override, the override and its reason must be recorded as an `AUDIT_EVENT` before the arrangement is committed.

7. Benefit deduction instructions are managed by the `integration` module (DWP Benefits Payment System interface), not by the `repaymentplan` module. The `repaymentplan` module must not create, modify, or cancel benefit deduction instructions directly. If a new arrangement is created that should supersede a deduction, the `repaymentplan` module must raise a process event to the `integration` module to initiate deduction cancellation.

---

## Version History

| Version | Date | Author | Change |
|---|---|---|---|
| 1.0 | 29 Apr 2026 | DWP Debt Domain Expert | Initial ruling |
