# RULING-015: Consolidated (Multi-Debt) Repayment Arrangement — Permissibility and Data Model

**Ruling ID:** RULING-015
**Linked issue:** TBD — must be linked before BA writes acceptance criteria for RPF.5 multi-debt variant
**Status:** `awaiting-client-sign-off` (see DDE-OQ-16 and DDE-OQ-17 below)
**Date issued:** 29 April 2026
**Domain expert:** DWP Debt Domain Expert
**Builds on:** RULING-014 (Repayment Arrangement Concurrency)

---

## Requirement IDs Covered

- RPF.5 (Multiple concurrent arrangements per customer — multi-debt variant)
- RPF.1 (Repayment arrangement creation)
- RPF.11 (Payment tracking against each arrangement)
- DW.87 (Payment Allocation System — allocation instruction ownership)
- DW.23 (Payment processing — payment channel interaction)
- RPF.31 (Payment allocation across debts)

---

## Question Posed

Can a customer with two debts have a single "consolidated" or "umbrella" repayment arrangement that covers both debts and makes a single combined payment, rather than two separate arrangements? If yes: how does payment allocation work, and how should the data model represent it?

---

## Regulatory Basis

- **DWP Benefit Overpayment Recovery Staff Guide (May 2025)** — voluntary repayment plans; no provision described for single consolidated plan spanning multiple separate debt accounts.
- **DWP Payment Allocation System policy (DW.87)** — the DWP Payment Allocation System provides the allocation instruction per account. DCMS follows this instruction; it does not override or substitute for it. The allocation instruction is per-account, not per-customer.
- **FCA Consumer Duty / TCF** — affordability assessment must reflect the customer's aggregate commitment. The Consumer Duty does not require a single plan; it requires the aggregate to be affordable.
- **FCA CONC 7** — prohibition on coercive or duplicative demand. Two separate plans (one per debt) are not a duplicative demand for the same sum; they are separate demands for separate liabilities.
- **ADR-001** (Process Instance per Account) — one Flowable process instance per Account. The process engine owns the lifecycle of each debt independently. There is no provision in ADR-001 for a process instance that spans multiple accounts.
- **RULING-014** — `REPAYMENT_ARRANGEMENT` is linked to exactly one Account. One active arrangement per Account at any time. These constraints originate from ADR-001 and FCA CONC 7.
- **Insolvency Rules 2016 / Debt Respite Scheme** — regulatory suspension and moratorium events apply per-account (insolvency) or per-person (breathing space). A consolidated plan could complicate selective suspension of one debt while the other remains active.

---

## Rule Statements

### Rule 1: A single REPAYMENT_ARRANGEMENT record spanning multiple Accounts is NOT PERMITTED in DCMS

A `REPAYMENT_ARRANGEMENT` record must be linked to exactly one Account. This is an existing constraint from RULING-014, which derives from:

1. **ADR-001** — one process instance per Account. The process engine cannot govern a plan that lives outside a single account's process.
2. **DW.87** — the DWP Payment Allocation System issues allocation instructions per Account, not per customer or per consolidated plan. A single plan receiving a combined payment creates an immediate conflict: DCMS cannot route the payment to two different accounts using the DWP Payment Allocation System without first splitting it into per-account amounts.
3. **Regulatory event independence** — if Account A enters breathing space (person-level) while Account B does not (e.g., the second debt was not included in the moratorium), a single consolidated plan would be impossible to partially suspend without architectural workarounds. The Debt Respite Scheme applies to the person but moratoriums can exclude specific debts if the creditor successfully challenges inclusion. Insolvency events are per-debt and per-creditor.

**This constraint is not provisional — it is structural.** The data model will not support a single `REPAYMENT_ARRANGEMENT` record linked to multiple Accounts. Builders must not implement this.

---

### Rule 2: The customer experience of a "single plan" is achievable through TWO LINKED arrangements — one per Account

Nothing in DWP policy or regulation prevents DCMS from presenting two separate arrangements to the customer as a coherent combined commitment, provided:

1. Each `REPAYMENT_ARRANGEMENT` is linked to its own Account (satisfying RULING-014 Rule 2).
2. The two arrangements are created in the same agent interaction and share a `linked_arrangement_group_id` field (see Rule 4 below) — making the relationship visible in the data model and audit trail.
3. The payment amounts for the two arrangements are calculated together during the affordability assessment (I&E) and are individually nominated for each account, so the customer's total commitment is transparent.
4. The customer receives a single combined arrangement letter (a single communications event) confirming both arrangements and the total combined monthly commitment, with each debt's individual amount itemised.

A linked pair of arrangements is not a "consolidated plan" in the data model. It is two separate arrangements that were negotiated in the same interaction and are surfaced to the customer as a combined commitment.

---

### Rule 3: Payment allocation for linked arrangements — each payment is made per Account, not as a pooled single payment

A customer with two linked arrangements does NOT make a single payment that DCMS then splits. Each arrangement has its own payment reference and own payment method. Payment is made:

- **Direct debit:** Two separate DD mandates, each with a different reference, collecting from the customer's bank account on the same or different dates.
- **Standing order / voluntary transfer:** The customer makes two separate transfers with two separate payment references, one per account. DCMS may give the customer a combined reference schedule document, but the actual transfers remain separate.
- **Benefit deduction:** RULING-001 Q3 establishes that the DWP Benefits Payment System applies deductions per-account per-priority-rank. Two deductions for two accounts are two separate instructions at two separate priority positions. The customer has no option to make a "single payment" for benefit deductions.

**Why a single pooled payment is not permitted:**

The DWP Payment Allocation System (DW.87) issues allocation instructions per Account. If DCMS received a single pooled payment for two accounts, it would need to split the payment before passing it to the Payment Allocation System — and the split rule would have to be defined by DCMS. DWP policy is explicit: DCMS does not own the allocation decision. DCMS cannot define its own split rule for a pooled payment without substituting its own logic for the DWP Payment Allocation System's instruction, which is prohibited by DW.87.

**This is an open question requiring DWP client confirmation.** See DDE-OQ-16.

---

### Rule 4: Data model — linked_arrangement_group_id

To represent a set of arrangements negotiated together across accounts, the `REPAYMENT_ARRANGEMENT` table must include a nullable `linked_arrangement_group_id` (UUID). When two or more arrangements are created in the same agent interaction to cover a customer's multiple debts together:

- Both records are assigned the same `linked_arrangement_group_id`.
- Each record remains independently linked to its own Account.
- Each record has its own `status`, `payment_method`, `payment_amount`, and lifecycle.
- The group ID is informational — it enables the agent UI and communications module to display or describe the arrangements as a related set. It does not create any shared lifecycle constraint.

**Lifecycle independence is mandatory.** If one arrangement in a linked group is breached, cancelled, or suspended (e.g., due to breathing space on one account), the other arrangement continues under its own lifecycle. The group ID does not cause them to share a lifecycle. A breach on Account A's arrangement must NOT automatically breach Account B's arrangement.

**Audit requirement:** When two arrangements are created with the same `linked_arrangement_group_id`, an `AUDIT_EVENT` must record that the group was created, listing both arrangement IDs, the agent who created them, and the session/interaction ID. This satisfies COM06/COM07 audit trail requirements for a multi-account action.

---

### Rule 5: Affordability assessment still applies in aggregate

When creating two linked arrangements (one per account) in the same interaction, the I&E affordability assessment must evaluate the **combined** proposed monthly commitment (sum of both arrangements' payment amounts) against the customer's disposable income, as required by RULING-007 and FCA Consumer Duty.

The fact that the two arrangements are technically separate records does not reduce the affordability obligation. DCMS must treat the combined commitment as a single affordability question during the creation interaction.

If the combined commitment is affordable, both arrangements may proceed. If not, the agent must either:
- Reduce the payment amount on one or both arrangements to bring the total within affordability, or
- Obtain a supervisor override with a recorded reason before proceeding.

This is the same aggregate check required by RULING-014 Rule 5 for concurrent independent arrangements, applied to the specific case of same-session linked arrangements.

---

### Rule 6: Regulatory event interaction — linked arrangements do NOT share suppression or suspension

Each arrangement in a linked group is governed by the regulatory status of its own Account and the Person. Regulatory events apply as follows:

| Event | Scope | Effect on Linked Arrangements |
|---|---|---|
| Breathing space moratorium | Person-level | ALL arrangements for the Person are suspended, regardless of group. RULING-005 applies. |
| Insolvency (IVA, bankruptcy, DRO) | Per-debt / per-account (insolvency event applies to a specific liability) | Only the arrangement for the Account that is subject to the insolvency event is suspended. The other account's arrangement continues. |
| Deceased customer | Person-level | ALL arrangements for the Person are cancelled per RULING-006. |
| Breach on one account | Per-Account | Only the breached account's arrangement enters BREACHED status. The other account's arrangement is unaffected unless the agent takes action. |
| I&E reassessment revealing unaffordability | Customer-level | Both arrangements must be reviewed for affordability. An agent must action both, but each arrangement is renegotiated independently under its own account's process. |

**Builder note:** The `linked_arrangement_group_id` field must not be used as a trigger or filter for any regulatory event propagation. Regulatory events use the Person ID (breathing space, deceased) or Account ID (insolvency, breach) as their scope key, not the group ID.

---

## Edge Cases

### Customer requests a single bank transfer for both debts

The customer may want to make one bank transfer that DCMS applies across both accounts. DCMS must not accept a pooled payment without a defined per-account split. The pending DWP confirmation (DDE-OQ-16) must resolve whether DCMS is ever permitted to define a split rule for pooled receipts. Until confirmed, **DCMS must require two separate payment references** and must not accept a single unallocated payment for two accounts.

### Customer has three or more debts — can all be linked?

Yes — a `linked_arrangement_group_id` can link more than two arrangements. The rules in RULING-015 apply equally to a group of any size. Each member of the group remains per-Account. Affordability is assessed on the full aggregate (all arrangements in the group, plus any other active arrangements outside the group).

### One arrangement in the group is cancelled — does the group dissolve?

The group ID is informational. Cancelling one arrangement does not require cancelling the others. The remaining arrangement(s) continue under their own lifecycle. The cancelled arrangement's record retains the `linked_arrangement_group_id` for audit traceability. The group is dissolved only if all members reach a terminal status (COMPLETED, CANCELLED, WRITTEN_OFF). No automated dissolution logic is required — the group ID is a label, not a shared state machine.

### Customer proposes different payment amounts for each debt

Yes — each arrangement in a linked group has its own `payment_amount`. The two amounts need not be equal. The affordability assessment covers the sum. An agent may, in the same interaction, set £100/month on Account A and £50/month on Account B, producing a total commitment of £150/month for the customer.

### One account has an active benefit deduction instruction

If Account A is recovering via benefit deduction (which is not modelled as a `REPAYMENT_ARRANGEMENT` — see RULING-014 Rule 3 and RULING-001 Q8), and the agent wants to create a voluntary repayment arrangement for Account B only, that is a single arrangement for Account B. The benefit deduction on Account A and the voluntary arrangement on Account B coexist by virtue of being on different accounts. This is the same scenario as RULING-014 Rule 1 (concurrent arrangements across different accounts) and is PERMITTED. No linking is required between the deduction instruction and the arrangement; they are on separate accounts.

---

## Open Questions Requiring DWP Client Sign-Off

### DDE-OQ-16: Does DWP policy permit a single pooled customer payment to be received by DCMS and split across two accounts?

**Question:** If a customer makes a single bank transfer (or direct debit) with an aggregate amount intended to cover two debts, may DCMS split that payment across two accounts and route each portion to the relevant Account's balance?

**Why this matters:** If yes, DCMS must define a split rule (e.g., pro-rata by outstanding balance, or fixed amounts specified at arrangement creation). That split rule would substitute for the DWP Payment Allocation System instruction on a per-account basis — which is only permissible if DWP explicitly authorises DCMS to define split logic for this scenario.

**If yes:** What is the permitted split rule? Is it pro-rata by outstanding balance, or must the customer nominate specific amounts per account at arrangement creation?

**If no:** DCMS must enforce two separate payment references (one per account) and reject or suspend any unallocated pooled receipt pending agent intervention.

**Status:** AWAITING DWP CLIENT SIGN-OFF
**Owner:** Delivery Lead to escalate to DWP policy and DWP Payment Allocation System team
**Blocking:** RPF.31, DW.87 — builders must not implement pooled payment split logic until confirmed. Two-reference model may be implemented as the safe default.

---

### DDE-OQ-17: Is there a DWP operational concept of a "consolidated arrangement" that DCMS must support?

**Question:** DWP caseworkers may in practice negotiate a single payment commitment with a customer that covers multiple debts. Does DWP's operational policy have a formal concept of a consolidated arrangement, and if so, does it require specific data fields, reporting formats, or customer-facing wording beyond what RULING-015 Rule 2 describes?

**Why this matters:** If DWP has an existing concept of a consolidated plan (e.g., used in DWP customer service workflows), DCMS must align its data model and communications with that concept. If there is no such concept, the linked-arrangement model described in this ruling is the correct approach.

**Status:** AWAITING DWP CLIENT SIGN-OFF
**Owner:** Delivery Lead to raise with DWP operational policy team during discovery
**Blocking:** RPF.5 acceptance criteria for multi-debt arrangements — BA should not finalise wording until confirmed. Design and data model work may proceed on the basis of this ruling's linked-arrangement model as the safe default.

---

## Summary: Answers to the Developer's Questions

| Question | Answer |
|---|---|
| Is a single "umbrella" plan across multiple debts permitted under DWP policy? | No — a single `REPAYMENT_ARRANGEMENT` record spanning multiple Accounts is not permitted. DW.87 (Payment Allocation System) issues per-Account instructions; ADR-001 requires per-Account process instances. |
| How does payment allocation work across two debts within a single plan? | There is no single plan. Two separate arrangements are created, each with its own payment reference. Allocation is per-Account, as instructed by the DWP Payment Allocation System per DW.87. A pooled payment is not supported pending DDE-OQ-16. |
| One REPAYMENT_ARRANGEMENT linked to multiple accounts, or two linked arrangements? | Two separate `REPAYMENT_ARRANGEMENT` records, each linked to its own Account, optionally sharing a `linked_arrangement_group_id` (UUID) for display and audit purposes. The group ID is informational — it does not create a shared lifecycle. |
| Are there regulatory constraints? | Yes. DW.87 prohibits DCMS from overriding the Payment Allocation System's per-Account allocation instruction. ADR-001 requires process-instance-per-Account. FCA Consumer Duty requires aggregate affordability assessment across both arrangements. Regulatory events (breathing space, insolvency) apply per their natural scope (person or account), not per the group. |

---

## Data Classification Flags

- `linked_arrangement_group_id` — Operational. Not Restricted in isolation. Restricted when returned as part of an arrangement record containing financial terms.
- No new special-category-adjacent (GDPR Art. 9) elements introduced by this ruling.

---

## Guardrails — Builders Must Not Violate

1. A `REPAYMENT_ARRANGEMENT` record must reference exactly one Account. A foreign key to a second account on the same arrangement record must not be added.

2. `linked_arrangement_group_id` must be nullable. Arrangements created outside of a linked-pair interaction must have `NULL` in this field.

3. The `linked_arrangement_group_id` must not be used as a scope key for any regulatory event (breathing space suspension, insolvency suspension, breach propagation). Regulatory event scope is always Person ID or Account ID.

4. DCMS must not accept a pooled payment for two accounts without DWP confirmation that pooled payment splitting is permitted (DDE-OQ-16). Until confirmation is received, each arrangement must have a distinct payment reference, and any unallocated pooled receipt must be routed to a suspense queue for agent resolution.

5. When two arrangements are created in the same interaction with the same `linked_arrangement_group_id`, both arrangements and their group association must be written within a single `@Transactional` boundary. Partial state — one arrangement written but not the other — must never be committed.

6. Aggregate affordability check (RULING-014 Rule 5; RULING-007) must be performed over the combined proposed payment amounts of all arrangements being created in the interaction, not per arrangement individually.

---

## Version History

| Version | Date | Author | Change |
|---|---|---|---|
| 1.0 | 29 Apr 2026 | DWP Debt Domain Expert | Initial ruling |
