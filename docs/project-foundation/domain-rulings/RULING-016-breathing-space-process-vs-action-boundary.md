# RULING-016: Breathing Space — Process Execution vs. Prohibited Collection Action Boundary

**Ruling ID:** RULING-016
**Linked issue:** TBD
**Status:** `final` (subject to DWP policy overlay — see open question DDE-OQ-BS-PROCESS-01)
**Date issued:** 30 April 2026
**Domain expert:** DWP Debt Domain Expert

---

## Requirement IDs Covered

- CAS.BS-01 (Breathing space moratorium trigger)
- CAS.BS-02 (Communication suppression during moratorium)
- CAS.BS-03 (Enforcement suspension during moratorium)
- CAS.BS-04 (Interest and fees freeze during moratorium)
- CAS.BS-05 (Arrangement monitoring during moratorium — see also RULING-005)
- DIC.14 (Debt Respite Scheme compliance)
- DW.45 (Regulatory hold account treatment)
- DW.51 (Breathing space moratorium rules)
- DW.31 (Legal hold — enforcement suspension)

---

## Regulatory Basis

- **Debt Respite Scheme (Breathing Space Moratorium and Mental Health Crisis Moratorium) (England and Wales) Regulations 2020, SI 2020/1311** — primary source throughout this ruling
- **Regulation 7** — prohibited creditor actions during moratorium
- **Regulation 8** — permitted creditor actions / exceptions
- **Regulation 9** — moratorium start and notification duties
- **Regulation 10** — moratorium end
- **Regulation 14** — enforcement action prohibition
- **Regulation 21** — Mental Health Crisis Moratorium — specific rules
- **FCA CONC 7** — treatment of customers in financial difficulty
- **FCA Consumer Duty (PS22/9)** — fair outcomes obligation

---

## Direct Answer

The answer is **B, with a precisely bounded set of internal actions that remain permitted**.

The 2020 Regulations do not require that a debt management system stop executing internally. They require that specific, named categories of **action taken against or in relation to the debtor** be suspended. The statutory text of Regulation 7 enumerates the prohibited actions — it does not prohibit internal workflow state transitions, internal recalculations, internal case management, or internal preparation for post-moratorium resumption. A workflow engine continuing to hold a case in a moratorium state, recalculating balances in accordance with Regulation 7(2) (interest and fees freeze), maintaining internal records, processing voluntary debtor payments, and preparing the case for resumption is not in breach of the Regulations, provided none of the enumerated prohibited actions is taken. The boundary is between **actions directed at or affecting the debtor** (prohibited) and **internal processing that produces no prohibited effect on the debtor** (permitted).

---

## 1. Specifically Prohibited Creditor Actions Under the Moratorium

Regulation 7(1) of SI 2020/1311 provides that a creditor must not, during a moratorium, take any of the following actions in relation to a moratorium debt:

| Prohibited action | Regulation citation |
|---|---|
| Contact the debtor to collect or recover the debt | Reg. 7(1)(a) |
| Take any enforcement action in relation to the debt | Reg. 7(1)(b) and Reg. 14 |
| Instruct a debt collector, enforcement agent, or third party to contact the debtor to collect or recover the debt | Reg. 7(1)(c) — by implication from the scheme; DCA placement is halted |
| Charge, apply, or cause to accrue interest, fees, charges, or penalties on the debt | Reg. 7(2) — interest and fees freeze |
| Apply to court for a charging order, attachment of earnings, or other enforcement remedy | Reg. 14(1) |
| Send a default notice or take steps to terminate or accelerate the agreement | Reg. 7(1)(b) read with FCA CONC 7 |
| Report the moratorium period itself as a missed payment or adverse credit event | Reg. 7 read with FCA Guidance — adverse credit reporting during moratorium is not mandated and the moratorium registration itself is recorded on a central register, not on the debtor's credit file as a missed payment |
| Take any step whose effect is to apply additional financial detriment to the debtor in respect of the moratorium debt during the moratorium period | Reg. 7(2) (broad effect provision) |

**DWP-specific additional prohibition:** Deduction-from-benefit recovery is an enforcement mechanism for DWP debts. A breathing space moratorium therefore suspends any ongoing deduction-from-benefit instruction in respect of a moratorium debt. This is confirmed by DWP Debt Management guidance and is consistent with Regulation 7(1)(b). This is a significant DWP-specific point — commercial creditors cannot make benefit deductions, so this obligation has no direct commercial analogue and must not be assumed by inference from commercial debt practice alone.

**Clarification on "contact to collect":** Regulation 7(1)(a) prohibits contact for the purpose of collecting or recovering the debt. It does not prohibit all contact. Administrative contact on non-collection matters (benefit entitlement, general account enquiries initiated by the debtor, responding to a debt advice provider query about the account) is not prohibited. See permitted actions at section 2 below. This is also the foundation of RULING-001 section 3.

---

## 2. Actions Creditors Are Permitted to Continue During the Moratorium

The following actions are either explicitly preserved by the Regulations or are not prohibited by them:

| Permitted action | Basis |
|---|---|
| Accepting and processing voluntary payments made by the debtor | Reg. 7 does not prohibit debtor-initiated payments — only creditor-initiated collection. RULING-005 section 1 confirmed this. |
| Maintaining and updating internal account records (balances, payment history, case notes) | Not an action against the debtor. Internal record-keeping is required for legal compliance and audit. |
| Recalculating account balances in compliance with the interest/fees freeze | Required by Reg. 7(2) — the creditor must ensure interest is not accruing; the system must calculate what the frozen balance is. |
| Holding the case in an internal moratorium state and performing internal case management | Not prohibited. The process engine holding a case in a "breathing space hold" state and running internal checks is not an action against the debtor. |
| Preparing for post-moratorium resumption (re-evaluating strategy, pre-computing treatment path) | Not prohibited, provided no prohibited action is taken or triggered during the moratorium period itself. Any output of pre-computation must not reach the debtor until after the moratorium ends. |
| Responding to queries from the debt advice provider (DAP) or approved intermediary | Reg. 9 requires the creditor to notify the DAP; ongoing liaison with the DAP is part of the scheme's operation. |
| Updating vulnerability flags, customer records, or case notes | Internal record-keeping. Not an action against the debtor. |
| Processing proof-of-debt or other legal formalities (where court-required, not creditor-initiated enforcement) | Permitted where legally required — but creditor-initiated court applications for enforcement are prohibited by Reg. 14. |
| Communicating with the debtor on non-collection matters (benefit entitlement, general queries) | Reg. 7(1)(a) prohibits collection contact; non-collection administrative contact is not prohibited. See RULING-001. |
| Recording the moratorium in the audit trail and compliance log | Required for regulatory compliance. |
| Internal strategy evaluation that produces no debtor-facing output | Not prohibited. A decision engine evaluating which treatment path applies post-moratorium does not constitute an action against the debtor during the moratorium. |

---

## 3. The Statutory Boundary: "Process" vs. "Action Against the Debtor"

The boundary set by the 2020 Regulations is **outcome-focused**, not process-focused. The question the Regulations ask is: **does the action produce a prohibited effect on or in relation to the debtor?**

The Regulations do not prescribe internal system architecture. They do not require a workflow engine to halt. They enumerate specific prohibited outcomes. Consequently:

**Internal processing is permitted if and only if:**

1. It produces no communication directed at the debtor for the purpose of collecting or recovering the debt (Reg. 7(1)(a)).
2. It triggers no enforcement action — court applications, deduction-from-benefit instructions, enforcement agent instructions, warrant execution (Reg. 7(1)(b), Reg. 14).
3. It causes no additional interest, fees, charges, or penalties to accrue on the debt (Reg. 7(2)).
4. It produces no instruction to a third party (DCA, enforcement agent) to contact or pursue the debtor (Reg. 7(1)(c) by implication).
5. Any automated process step that would, if executed, produce one of the above effects must be **gated** at the point of effect — not necessarily at the point of internal processing.

**Practical implication for the Amplio process engine:** Amplio can continue advancing a breathing-space-flagged case through internal state transitions (e.g., MORATORIUM_ACTIVE → internal check steps → MORATORIUM_ACTIVE) provided each state transition is guarded at its output boundary. The guard is: "does this transition produce a prohibited action?" If yes, the transition must be suppressed or deferred until after moratorium end. If no, the transition may proceed.

This is a **gate-at-effect** design, not a **freeze-at-engine** design. The Regulations require the latter interpretation because they enumerate effects, not processes.

---

## 4. DWP-Specific Policy Overlays

The statutory baseline above applies to all creditors. DWP operates under the following additional constraints that are more restrictive than the statutory minimum:

### 4.1 Deduction-from-benefit recovery must be suspended

As stated in section 1, benefit deduction is an enforcement mechanism unique to DWP. During a breathing space moratorium, any active deduction-from-benefit instruction for a moratorium debt must be suspended. This is not merely good practice — it is the only interpretation consistent with Regulation 7(1)(b) (no enforcement action). **This is a mandatory system action: the DCMS must issue a suspension instruction to the benefit system on moratorium start.**

### 4.2 DWP Debt Management guidance may impose a stricter communications standard

DWP published operational guidance (DWP Debt Management's internal policy, last confirmed version 2023) indicating that DWP staff should not proactively contact customers during breathing space for any debt-related purpose, including to discuss repayment options "voluntarily." This is a DWP policy overlay that is stricter than the statutory floor (which only prohibits collection contact, not all debt-related contact). Until DWP client sign-off confirms the exact scope of this policy overlay as it applies to the DCMS, this ruling treats the statutory floor as the minimum and flags the DWP policy overlay as a potential additional constraint.

**Status: DDE-OQ-BS-PROCESS-01 — see open questions below.**

### 4.3 Benefit debt is not commercial debt — do not assume regulatory guidance issued for commercial creditors applies without verification

HMRC and FCA guidance documents about creditor obligations during breathing space are written for commercial creditors and financial services firms. DWP is a statutory creditor with specific powers (benefit deduction, direct earning attachment) not available to commercial creditors. Conversely, DWP lacks some commercial creditor tools (e.g., it is not FCA-regulated in the same way). Every regulatory interpretation must be checked against DWP's specific statutory position before being adopted.

---

## 5. Mental Health Crisis Moratorium Differences

The Mental Health Crisis Moratorium (MHCM) operates under Regulation 21 and the same Regulation 7 prohibition framework. The prohibited and permitted action lists in sections 1 and 2 apply equally to MHCM.

The material difference is duration and termination:

- Standard Breathing Space: 60 days from registration (Reg. 10(1)), extendable in limited circumstances.
- MHCM: No fixed end date. Continues until a registered mental health professional confirms the crisis has ended (Reg. 21(5)). There is no timer expiry event. The process engine must not apply a 60-day timer to MHCM cases.

**This ruling does not change the MHCM vs. standard distinction already established in RULING-001. It confirms that the process/action boundary analysis applies equally to both moratorium types.**

One additional MHCM-specific point: the mere fact that a customer is subject to an MHCM is itself sensitive data (it indicates a mental health crisis). The `hold_type = BREATHING_SPACE_MENTAL_HEALTH_CRISIS` field carries that inference. RULING-002 classified vulnerability data as Restricted / potential GDPR Article 9 special category. MHCM status should be treated at least as Restricted for the same reasons, and potentially as Article 9 health data. The `⚠ confirm with DWP` flag on vulnerability data classification in STD-SEC-003 applies with equal or greater force to MHCM status.

**Statutory Debt Repayment Plan (SDRP):** The SDRP (when commenced under the Debt Respite Scheme) operates a different moratorium structure and is not yet in force in England and Wales as at the date of this ruling (30 April 2026). Its commencement has been repeatedly deferred by HM Treasury. This ruling does not address SDRP. When SDRP is commenced, a separate ruling will be required.

**Scotland:** The Debt Arrangement Scheme (DAS) operates in Scotland under the Debt Arrangement Scheme (Scotland) Regulations 2011 (as amended). It has analogous effect to breathing space but different procedural rules. This ruling covers England and Wales only (SI 2020/1311). DWP debt in Scotland is subject to DAS where applicable, but that is a separate regulatory regime and a separate ruling will be required if DWP's geographic scope requires it.

---

## 6. Interaction with Existing Rulings

This ruling sits above and is consistent with prior breathing space rulings. Precedence order:

| Prior ruling | Relationship to this ruling |
|---|---|
| RULING-001 (communications suppression) | Confirmed and extended. This ruling explains the legal basis for the distinction between collection and non-collection contact. |
| RULING-005 (arrangement monitoring suspension) | Confirmed. Arrangement monitoring being "suspended" (not cancelled) is a correct implementation of the gate-at-effect model: internal monitoring can continue but must not produce breach records, warnings, or escalations during the moratorium. |
| RULING-011 (queued communication disposition) | Confirmed. The discard-on-lift rule for `BREATHING_SPACE_STATUTORY` communications is consistent with the gate-at-effect model — the communication was generated internally (permitted) but must not reach the debtor (the gate prevents the prohibited effect). |

---

## Open Questions Requiring DWP Client Sign-Off

### DDE-OQ-BS-PROCESS-01: Scope of DWP policy overlay on non-collection debt-related contact

**Question:** Does DWP's operational policy (2023 guidance) prohibit all debt-related contact during breathing space (stricter than Reg. 7(1)(a)), or does it align with the statutory floor (prohibiting only collection contact)? Specifically: may a DCMS agent contact a breathing-space customer to discuss a voluntary repayment offer, or to explain the moratorium process?

**Impact on design:** If DWP policy prohibits all debt-related contact (not just collection contact), the `CommunicationSuppressionService` must suppress `DEBT_ADMINISTRATION` category communications in addition to `DEBT_COLLECTION` during `BREATHING_SPACE_STATUTORY`. The communication category model in RULING-001 may need a third category.

**Do-not-proceed gate:** The communication category model must not be finalised until this is confirmed. RULING-001 is currently `awaiting-client-sign-off` for a related reason.

**Named owner for escalation:** Delivery Lead → DWP client policy team.

**Status:** `⚠ AWAITING DWP SIGN-OFF`

### DDE-OQ-BS-PROCESS-02: Deduction-from-benefit suspension — which system owns the suspension instruction?

**Question:** When a breathing space moratorium begins, what is the correct mechanism for suspending an active deduction-from-benefit instruction? Does DCMS issue a suspension instruction directly to the benefit payment system, or does DWP Debt Management Operations issue it manually, or is it triggered via DWP Place?

**Impact on design:** Determines whether the BPMN moratorium-start event must include an outbound integration call to a benefit system API, a manual task for a DWP operations agent, or a notification-only event.

**Do-not-proceed gate:** The integration design for moratorium start cannot be finalised until this is confirmed.

**Named owner for escalation:** Delivery Lead → DWP integration team.

**Status:** `⚠ AWAITING DWP SIGN-OFF`

---

## Data Classification Flags

| Data element | Classification | Note |
|---|---|---|
| `LEGAL_HOLD.hold_type = BREATHING_SPACE_MENTAL_HEALTH_CRISIS` | **Restricted / potential GDPR Article 9 special category** | Implies mental health crisis. Treat as Restricted minimum. STD-SEC-003 `⚠ confirm with DWP legal` flag applies. |
| Internal process state variables during moratorium (e.g., BPMN variables tracking moratorium state) | Operational | Not Restricted — they are process control data, not personal data in themselves. |
| Record of deduction-from-benefit suspension instruction | Operational | Part of enforcement action audit trail. |

---

## STD-SEC-003 Confirm-With-DWP Items Touched

- Vulnerability / MHCM data classification (item 2 in STD-SEC-003 open items) — this ruling reinforces that MHCM status carries at minimum the same sensitivity as vulnerability flags, and potentially higher (explicit health data). No change to classification status; existing `⚠ awaiting DWP legal confirmation of lawful basis` flag is confirmed as still open and applicable.

---

## Guardrails — Builders Must Not Violate

1. The Amplio process engine must not be configured to halt entirely when a breathing space moratorium is active. A complete halt would prevent legally required actions (balance freeze recalculation, audit trail maintenance, voluntary payment processing) that the Regulations either require or permit. The correct model is gate-at-effect, not freeze-at-engine.

2. Every process step that produces a debtor-facing output (communication dispatch, enforcement instruction, deduction-from-benefit instruction, DCA instruction, court application trigger) must check `breathing_space_active` from the suppression service before executing. This check must be at the point of effect, not only at case intake.

3. The Amplio process engine must not generate `ARRANGEMENT_BREACH` records, payment demand communications, escalation tasks, enforcement triggers, or interest-accrual postings while `LEGAL_HOLD.hold_type` is `BREATHING_SPACE_STANDARD` or `BREATHING_SPACE_MENTAL_HEALTH_CRISIS`. Confirmed by RULING-005 and now grounded in Regulation 7(1) and 7(2) of SI 2020/1311.

4. Voluntary payment processing must remain active during a moratorium. The payment pipeline must not check `breathing_space_active` as a gate on payment receipt — only as a gate on payment demand generation.

5. A moratorium-start event must write an `AUDIT_EVENT` with `event_type = MORATORIUM_STARTED`, `hold_type`, `start_date`, `registration_reference` (from the Insolvency Service's register), and `notified_by` (the debt advice provider). This is a legally required record.

6. Any strategy engine evaluation that runs internally during a moratorium must produce outputs flagged as `DEFERRED_PENDING_MORATORIUM_END`. These outputs must not be actioned until after the moratorium ends and must not be treated as current instructions.

7. MHCM cases must not have a timer-based moratorium end event. Only a manual professional sign-off event may end an MHCM moratorium. The process model must enforce this.

---

## Version History

| Version | Date | Author | Change |
|---|---|---|---|
| 1.0 | 30 Apr 2026 | DWP Debt Domain Expert | Initial ruling — answers process-vs-action boundary question for Amplio design |
