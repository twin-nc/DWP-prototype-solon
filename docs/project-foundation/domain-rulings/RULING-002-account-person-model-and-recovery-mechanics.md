# RULING-002: Account-Person Model and Debt Recovery Mechanics

**Ruling ID:** RULING-002
**Status:** final
**Date:** 2026-04-23
**Created by:** DWP Debt Domain Expert Agent + Business Analyst Agent + Traceability Steward Agent
**Requirement IDs:** CAS.1, CAS.2, CAS.3, CAS.4, CAS.8, CAS.11, CAS.15, CAS.17, DIC.5, DIC.6, DIC.11, DIC.16, DIC.20, DIC.26, DW.22, DW.23, DW.32, DW.36, DW.37, DW.44, DW.74, DW.87, IEC.1–11, RPF.1–9
**Regulatory basis:**
- Social Security Administration Act 1992
- Universal Credit Regulations 2013 (SI 2013/376)
- Welfare Reform Act 2012
- Debt Respite Scheme (Breathing Space Moratorium and Mental Health Crisis Moratorium) (England and Wales) Regulations 2020
- DWP Benefit Overpayment Recovery Staff Guide (GOV.UK)
- DWP ADM Chapter D2: Third-Party Deductions (UC, JSA, ESA)
- Universal Credit Third-Party Payments Creditor and Supplier Handbook (GOV.UK)
- FCA Vulnerability Guidance FG21/1

---

## 1. Core Terminology

| Term | Definition |
|---|---|
| **UC** | Universal Credit — the UK government benefit that replaced six legacy benefits (Income Support, Jobseeker's Allowance, Employment and Support Allowance, Housing Benefit, Working Tax Credit, Child Tax Credit). Paid monthly to individuals or couples jointly. |
| **DCMS** | Debt Collection Management System — the system being built for DWP in this project. |
| **Person** | An individual claimant or co-claimant. The individual human being who holds legal liability. |
| **Account** | A UC claim or legacy benefit claim that carries a debt. The financial ledger for a single debt event. Holds the debt, claim status, deduction instructions, I&E assessment, and treatment stream. |
| **Account-Person Link** | The explicit relationship between a Person and an Account. One account has one or two links. One person may have many links (one per debt event). |
| **On benefit** | The account's UC claim is active; deductions from the award are the primary recovery method. |
| **Off benefit** | The UC claim has ended; direct billing is the primary recovery method. |

---

## 2. The Three-Entity Model

The data model has three core entities:

```
Person  ──<  Account-Person Link  >──  Account
```

### Person
Holds all individual-level attributes:
- Name, date of birth, NI number
- Contact details and correspondence preferences
- Vulnerability flags
- Breathing space status
- Insolvency status
- Death flag
- Third-party authority (Power of Attorney, debt management company)

### Account
Holds all debt and recovery attributes:
- Debt amount, balance, payment history
- Claim reference and claim status (ACTIVE, TERMINATED, SUSPENDED)
- Deduction instructions
- I&E assessment
- Treatment stream and segment
- Liability structure (INDIVIDUAL or JOINT_AND_SEVERAL)

### Account-Person Link
- Cardinality: 1 Person per individual account; 2 Persons per joint UC account
- Maximum: **never more than two Persons per Account**
- One Person may be linked to multiple Accounts (one per distinct debt event)

---

## 3. Account Cardinality Rules

| Account type | Persons linked | Benefit basis |
|---|---|---|
| Individual | Exactly 1 | Legacy benefit or single UC claim |
| Joint | Exactly 2 | UC couple joint claim only |

**Non-claimant household members are never linked to an account.** Children and other residents bear no DWP debt liability regardless of whether they benefited from the overpayment.

**A person can be linked to multiple accounts** — one per distinct debt event. Examples:
- A legacy benefit overpayment account from 2019 + a UC overpayment account from 2024
- An individual UC account + a joint UC account from a prior couple relationship
- Two separate UC overpayment accounts from different claim periods

---

## 4. Benefit Award and Claim Status

### UC couples receive one joint award
A UC couple has a single claim reference number and receives one monthly payment. Deductions are applied to this award — not to either individual person separately.

### Claim status belongs on the Account (not the Person)
`ON_BENEFIT` / `OFF_BENEFIT` is not an individual attribute in operational terms. It is a property of the **claim/award**. When the claim terminates, the deduction pathway closes regardless of whether individuals remain legally liable.

Recommended claim status values on Account:
- `ACTIVE` — award ongoing; deductions may be applied
- `TERMINATED` — claim ended; switch to direct billing
- `SUSPENDED` — claim paused (e.g., sanction review, breathing space instruction)
- `SEPARATED` — couple has separated; triggers 50/50 account split

### When one couple member becomes ineligible
If Person A loses UC entitlement but Person B remains entitled, UC claim management recalculates the award (or splits the claim). DCMS must receive that notification from UC claim management and update claim status accordingly. DCMS cannot infer claim status from a Person-level benefit flag.

**Open question — requires DWP client sign-off:** When one UC couple member becomes ineligible, does the joint claim remain active (recalculated) or terminate and split? How is DCMS notified — by claim reference or NI number?

---

## 5. Recovery Modes

### 5.1 On benefit — deduction from the UC award

| Parameter | Value |
|---|---|
| Target | The UC award (account) |
| Standard rate | 15% of standard allowance |
| Extended rate | 25% with written consent from the claimant |
| Fraud rate | 25–40% (no consent required; exact thresholds require DWP confirmation) |
| Legacy benefit rate | Fixed cash amount (£13.95/week as of April 2025); uprated annually each April |
| Cap | 15% without consent; 25% with consent; housing costs deducted independently outside the cap at 10–20% |
| Minimum benefit | 1p remaining (UC); 10p remaining (legacy) |

DCMS **issues** the deduction instruction. The DWP Benefits Payment System **applies** it. DCMS then reconciles the received payment against the expected amount. DCMS does not apply deductions directly.

#### Deduction is not guaranteed
A deduction instruction may not result in a payment if:
- The claimant stops claiming before the next assessment period
- Higher-priority debts saturate the 15% cap
- The claim is suspended or terminated

DCMS must monitor across multiple assessment periods before treating non-receipt as a failure.

#### Priority ordering
UC overpayments rank **16th (lowest priority)** in the statutory deduction priority order (DWP ADM Chapter D2). Higher-ranking deductions (child maintenance, rent, council tax, utilities, fuel, social fund loans) are served first from the same 15% cap. When the cap is full, UC overpayment deductions queue to the next assessment period.

Third-party debts (non-DWP creditors using the Deductions from Benefit Scheme) compete for the same 15% cap and rank higher than UC overpayments.

#### Configurability
All deduction rates, caps, and priority orderings are **statutory policy data** and must be configurable in DCMS without code changes. Annual April uprating is mandatory. Changes must be auditable and effective-dated.

### 5.2 Off benefit — direct billing

| Parameter | Value |
|---|---|
| Target | The account (one bill) |
| Liability | Joint and several — DWP can pursue either or both persons for the full amount |
| Bill split | None at this stage; the joint liability remains whole |

One bill is issued to the account. Both persons on a joint account are jointly and severally liable — each owes the full amount. There is no 50/50 split of the bill while the couple remains together.

### 5.3 Separation — account split

When a UC couple separates:
1. The joint account is split into two new individual accounts
2. Each new account carries 50% of the original debt
3. Any residual pence from odd-number division is written off
4. The 50/50 split is **mandatory and automatic** — fault does not affect apportionment
5. Each new individual account is then recovered independently (on or off benefit as applicable)

The separation event must be recorded as an immutable audit event referencing both original and new accounts.

**Note on Housing Benefit and third-party debt:** 50/50 is mandatory only for UC couple separation. For Housing Benefit overpayments or third-party debt with multiple liable parties, DWP retains discretion and may apportion based on fault assessment.

---

## 6. Segmentation and Treatment

- Segmentation operates at **account level**
- One person with two accounts can be in different treatment streams simultaneously
- Segment taxonomy is evaluated per account using account attributes informed by Person-level attributes (vulnerability, benefit status, regulatory flags)

**Critical rule:** Before applying any treatment or enforcement action, DCMS must check the Person-level regulatory status of **all Persons linked to the account**. Account-level segmentation does not override Person-level protections.

| Person-level attribute | Effect on account treatment |
|---|---|
| Vulnerability | Specialist queue routing; extended I&E; restricted contact rules |
| Breathing space | All collection and enforcement suspended on the account for the duration |
| Insolvency | Enforcement halted; account referred to specialist queue |
| Death | All activity halted; executor/estate workflow triggered |

For joint accounts with mixed regulatory status (e.g., one person in breathing space, the other not), the account is protected against collection as a whole — DWP cannot pursue the non-protected person separately on the same joint debt. See open questions in Section 8.

---

## 7. Income and Expenditure (I&E) Assessment

- I&E is conducted at **account level**
- One account = one I&E assessment
- A person with multiple accounts may have a separate I&E per account (same creditor; DWP policy on consolidation to be confirmed)

| Account type | I&E scope |
|---|---|
| Individual account | Individual income and expenditure |
| Joint UC account | Household income and expenditure combined |

For joint accounts, shared household expenses (rent, council tax, utilities) are captured as a **household total**. Allocation between partners is a case judgement — not a fixed 50/50 rule.

Current delivery scope supports agent-facilitated I&E completed by a DCMS staff member on behalf of the claimant. Self-service I&E, completed by the claimant online, is a future-scope capability unless customer self-service is reopened. The I&E model must not preclude future self-service, and an incomplete I&E must be saveable and resumable (IEC.10).

I&E data informs:
- Repayment plan generation (IEC.4, RPF.2)
- Collections strategy assignment (IEC.6)
- Vulnerability process divergence (extended I&E for vulnerable customers)

**Requirements gap:** The I&E form field structure, mandatory fields, validation rules, retention period, and definition of "extended I&E" for vulnerable customers are not specified in the tender requirements. These require design specification before the I&E feature is built.

---

## 8. Open Questions Requiring DWP Client Sign-Off

These are **blocking** for the features indicated. Do not write acceptance criteria or begin implementation until each is answered.

| # | Question | Blocks |
|---|---|---|
| 1 | When one UC couple member becomes ineligible mid-claim, does the joint claim remain active (recalculated award) or terminate and split into two individual claims? | Account claim status model; deduction eligibility logic |
| 2 | How does UC claim management notify DCMS of claim changes — by claim reference or by NI number? Is it real-time API, batch feed, or manual entry? | Integration design with UC claim management |
| 3 | When one member of a joint account enters breathing space, are both members' collection activities suspended, or can DWP pursue the other member separately? | Enforcement gate logic for joint accounts |
| 4 | When one member of a joint account enters insolvency — does treatment differ by insolvency type (IVA vs. bankruptcy vs. DRO)? Can the other member still be pursued? | Enforcement logic by insolvency type |
| 5 | When one member of a joint account dies — does the survivor assume full liability, 50% liability, or is the account closed? What is the interaction with probate? | Account lifecycle on death event |
| 6 | For a joint account with one vulnerable and one non-vulnerable person — does the whole account receive process divergence (extended I&E, forbearance), or only the vulnerable person's interactions? | Vulnerability-driven process divergence for joint accounts |
| 7 | When a couple separates and a repayment plan is in flight — are existing plans terminated and re-established on the two new individual accounts, or do they carry over? | Repayment plan lifecycle on separation |
| 8 | For fraud deduction rates — which specific fraud categories map to 25% vs. 40%? | Deduction rate lookup table |
| 9 | Is I&E mandatory before any deduction instruction is issued, or only when affordability is disputed by the claimant? | I&E workflow trigger conditions |
| 10 | For a person with multiple accounts with DWP, does DWP conduct one consolidated I&E covering all accounts, or one I&E per account? | I&E workflow for multi-account persons |

---

## 9. Non-Blocking Design Decisions Required

These are not blocking client questions — they require the solution architect to make and record a design decision before the relevant feature is built.

| # | Decision needed |
|---|---|
| 1 | How does DCMS receive regulatory event notifications (breathing space entry, insolvency order, death registration) — real-time API, batch feed, or manual entry by agent? |
| 2 | Consent workflow for deductions above 15% — who collects written consent, how is it stored, how is it enforced in the deduction instruction? |
| 3 | Deduction queuing algorithm — when multiple deduction instructions compete for the same 15% cap in one assessment period, how are they sequenced? |
| 4 | Policy effective dating — when deduction rates or caps are updated (e.g., annual April uprating), do changes apply immediately or from the next assessment period? How are in-flight instructions affected? |
| 5 | I&E form field specification — mandatory fields, conditional fields, validation rules against trigger values, retention period |
| 6 | Affordability assessment logic — automated scoring vs. manual review, thresholds for automatic plan generation, escalation criteria |
| 7 | Extended I&E definition for vulnerable customers — which additional fields or steps distinguish extended from standard I&E? |
| 8 | Housing cost deduction cap interaction — do housing cost deductions (10–20%, outside the 15% cap) reduce the available headroom for other deductions, or operate fully independently? |
| 9 | Off-benefit enforcement mechanisms — court orders, attachment of earnings, and other enforcement methods are not detailed in the tender; a separate enforcement design is needed |

---

## 10. Summary Rules for BA and Builder

### For Business Analyst
1. Debt and recovery operations target the **account**. Regulatory protections and welfare attributes target the **person**.
2. A bill, deduction instruction, I&E assessment, and treatment stream each belong to **one account**.
3. For any feature touching joint accounts, verify which of the open questions in Section 8 are relevant and ensure they are answered before writing acceptance criteria.
4. The 50/50 split applies **only** on couple separation. It does not apply to billing, deduction, or any other recovery action while the couple remains together.

### For Builder
1. Enforce the cardinality constraint: `Account-Person links must be exactly 1 or 2. Never 0, never 3+.`
2. Claim status (`ACTIVE`, `TERMINATED`, `SUSPENDED`, `SEPARATED`) belongs on the Account entity, not on Person.
3. Before executing any enforcement action (deduction instruction, bill, legal referral), check **all linked Persons'** regulatory status (breathing space, insolvency, death). If any Person has an active protection, gate the action.
4. Deduction rates, caps, and priority orderings must be configurable data — never hardcoded. Annual April uprating must be handled without a code change.
5. Separation is a formal, auditable event: close the joint account, create two new individual accounts at 50/50, write off residual pence, update all Account-Person links, record the separation event immutably.
6. DCMS issues deduction instructions; the DWP Benefits Payment System executes them. Do not conflate issuing with applying. Implement reconciliation to detect when a received payment differs from the instruction.

---

## References

- [DWP Benefit Overpayment Recovery Staff Guide (GOV.UK)](https://www.gov.uk/government/publications/benefit-overpayment-recovery-staff-guide/benefit-overpayment-recovery-guide)
- [DWP ADM Chapter D2: Third-Party Deductions UC, JSA & ESA](https://assets.publishing.service.gov.uk/media/68404ee38dd459f8c947b40f/adm-chapter-d2-third-party-deductions-uc.pdf)
- [Universal Credit Third-Party Payments Creditor and Supplier Handbook (GOV.UK)](https://www.gov.uk/government/publications/how-the-deductions-from-benefit-scheme-works-a-handbook-for-creditors/universal-credit-third-party-payments-creditor-and-supplier-handbook)
- [Debt Respite Scheme: Breathing Space Guidance for Creditors (GOV.UK)](https://www.gov.uk/government/publications/debt-respite-scheme-breathing-space-guidance/debt-respite-scheme-breathing-space-guidance-for-creditors)
- [Universal Credit Regulations 2013 (SI 2013/376)](https://www.legislation.gov.uk/uksi/2013/376/contents)
- [Social Security Administration Act 1992](https://www.legislation.gov.uk/ukpga/1992/4/contents)
- [Shelter England: Recovery of Universal Credit Overpayments](https://england.shelter.org.uk/professional_resources/legal/benefits/universal_credit/recovery_of_universal_credit_overpayments)
- [Universal Credit Deductions — House of Commons Library](https://commonslibrary.parliament.uk/research-briefings/cdp-2023-0166/)
- FCA Vulnerability Guidance FG21/1
- RULING-001: Customer Data Model — Unit of Analysis (Individual vs. Household)
- RULING-001: Benefit Deduction Mechanics in DWP Debt Recovery

---

## Audit

| Field | Value |
|---|---|
| Ruling ID | RULING-002 |
| Created | 2026-04-23 |
| Created by | DWP Debt Domain Expert Agent + Business Analyst Agent + Traceability Steward Agent |
| Last updated | 2026-04-23 |
| Status | final |
| Dependent features | CAS.1–4, CAS.8, CAS.11, CAS.15, CAS.17, DIC.5, DIC.6, DIC.11, DIC.16, DIC.20, DIC.26, DW.22, DW.23, DW.32, DW.36, DW.37, DW.44, DW.74, DW.87, IEC.1–11, RPF.1–9 |
