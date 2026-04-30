# RULING-001: Customer Data Model — Unit of Analysis (Individual vs. Household)

**Ruling ID:** RULING-001  
**Linked issue:** [To be populated by BA]  
**Status:** final  
**Requirement IDs:** AAD.x (customer/account entity definition); DIC.x (data model); DW.87, DW.23 (payment allocation, account treatment)  
**Regulatory basis:**
- Social Security Administration Act 1992 (overpayment recovery and claimant definitions)
- Universal Credit Regulations 2013 (SI 2013/376) — couples and household treatment
- Consumer Credit Act 1974 s.189 (definition of "debtor" and "creditor")
- FCA Vulnerability Guidance FG21/1 (individual customer treatment and vulnerability identification)
- Debt Respite Scheme (Breathing Space Moratorium and Mental Health Crisis Moratorium) (England and Wales) Regulations 2020
- DWP Benefit Overpayment Recovery Staff Guidance (internal policy)
- DWP Working with Representatives Guidance (third-party authority scope)

---

## Rule Statement

**Primary unit of analysis in DWP debt is the individual claimant, not the household.**

However, DWP debt law creates three distinct scenarios with different treatment:

### 1. Single Claimant (Individual Debt Account)
An individual who receives a benefit overpayment is personally and solely liable for repayment. Debt is recorded and recovered against that individual. This is the foundational case.

### 2. Joint Claimant in Universal Credit (Single Debt Account, Joint and Several Liability)
When a couple claims Universal Credit jointly, **a single debt account is created with both members jointly and severally liable**. Both members of the couple are the "payee" and bear equal responsibility for the entire overpayment, regardless of which member caused the overpayment or received the benefit.

**Upon separation:**
- The joint debt account is split 50/50 into two separate individual accounts.
- Each separated individual is liable for their half of the original joint overpayment.
- Any residual penny (from odd-number division) is written off.

### 3. Legacy Benefits (Individual Debt for Each Claimant)
Individual claimants on legacy benefits (Income Support, Jobseeker's Allowance, etc.) have separate individual debt accounts. Couples on legacy benefits do not have a joint claim mechanism; they maintain separate claims and separate debts.

---

## Household vs. Benefit Unit vs. Individual

DWP uses three related but distinct concepts:

| Concept | Definition | Debt Relevance |
|---|---|---|
| **Individual / Claimant** | A person aged 18+ who makes a benefit claim | Primary unit for debt liability. Every debt is ultimately attributed to one or more identified individuals. |
| **Benefit unit** | The group eligible to claim as one unit (e.g., couple in UC) | For UC, couples form a single benefit unit and receive a single payment. Debts attach to the benefit unit (i.e., both individuals jointly). For legacy benefits, there is no "benefit unit" — only individual claims. |
| **Household** | All people living in the same residence (including children, non-claimants) | Irrelevant to debt liability in DWP context. Only claimants (and co-claimants in joint UC claims) bear debt liability. Non-claimant household members are not liable for DWP debt, even if they benefit indirectly from the payment. |

---

## Vulnerability, Correspondence, Third-Party Authority, and Contact Preferences

All of these are **individual-level attributes**, even in joint UC claims:

### Vulnerability Flagging
Each individual claimant has their own vulnerability status. A couple in a joint UC claim can have:
- Person A vulnerable, Person B not vulnerable, OR
- Both vulnerable, OR
- Neither vulnerable

Vulnerability is recorded and actioned at the individual level. Process divergence (e.g., extended I&E assessment period, specific forbearance requirement) applies to the vulnerable individual, not to the joint claim as a whole.

**Implication for DCMS:** Vulnerability is a `Person` attribute, not a `Account` or `Couple` attribute.

### Correspondence and Contact Preferences
Each individual has their own correspondence address, email, SMS number, and contact preference (e.g., "do not contact by phone," "correspondence to representative only").

In a joint UC claim, DWP communications may reach:
- Both individuals (for joint liability communications),
- One individual only (for individual-specific vulnerability or process matters).

**Implication for DCMS:** Communication preferences and contact details are `Person` attributes. The communications module must be able to address individual persons within a joint account.

### Third-Party Authority
DWP authorization allows a representative to act on behalf of **a specific individual only**. The DWP guidance on "Working with Representatives" uses singular language throughout: "the customer," "the person."

- A Power of Attorney or Debt Management Company representative acts for an individual, not for a couple.
- A couple cannot grant a single representative to act for both members jointly; each member must authorize the representative separately.
- Authority is transaction-specific and time-limited; it does not confer indefinite scope.

**Implication for DCMS:** A third-party authority record links a `Representative` to a `Person`, not to a couple or joint account.

---

## Breathing Space and Insolvency (Individual-Level Protections)

### Breathing Space (Debt Respite Scheme)
Breathing space is claimed by an **individual debtor**. The Debt Respite Scheme Regulations define the scheme as protection for "eligible individual debtors with problem debt."

When an individual in a couple enters breathing space:
- That individual's liabilities (including joint debts) become breathing space debts and receive protection.
- **Joint debts are protected even if only one party is in breathing space.** The Regulations state: "Joint debts can be included in a breathing space, even if only one person applies for a breathing space, and the joint debt would become a breathing space debt with protections applying to the other people who owe that debt."
- The other member of the couple is not automatically in breathing space; they may still be subject to collections activity on other debts, but not on the joint debt.

### Insolvency (IVA, Bankruptcy, Debt Relief Order)
Insolvency protections (IVA, bankruptcy, DRO) apply to an **individual only**. When one member of a couple enters an insolvency procedure:
- The individual's liabilities are covered by the procedure.
- Joint debts become subject to the insolvency procedure for that individual; the other member remains liable but the creditor may have limited ability to pursue them (depends on the specific insolvency type).
- The other member is not in insolvency and can still be pursued for recovery.

**Implication for DCMS:** Breathing space and insolvency event flags are `Person` attributes. The account-level treatment (collection suspension, enforcement prohibition) must reflect the status of **each individual** connected to that account.

---

## CCA-Governed Debt (Individual vs. Joint Regulated Agreements)

The Consumer Credit Act 1974 defines regulated agreements with "the debtor" (singular or plural if joint).

- A regulated agreement (credit card, personal loan, hire purchase) governed by the CCA may be:
  - **Sole debtor:** One individual (sole liability).
  - **Joint debtors:** Two individuals (joint and several liability).

DWP benefit overpayment debts are **not** CCA-regulated because they are statutory debts, not credit agreements. However, if DWP ever needs to disclose information about DCA-placed debts or enforcement action, it must distinguish between:
- Individual liability (sole debtor).
- Joint liability (joint debtors — both equally responsible).

**Implication for DCMS:** For future DCA integration and disclosure, the account must be able to record whether it represents sole or joint liability.

---

## Payment Allocation

Payment allocation in DWP is handled by the DWP Payment Allocation System, which DCMS must invoke and follow. When a payment is received:

- **For individual accounts:** Payment is allocated to that individual's debts, per DWP allocation rules (e.g., oldest debt first, or per individual arrangement).
- **For joint accounts:** Payment is allocated to the joint debt, per DWP allocation rules. No apportionment is made to the individuals unless the account is later split (on separation).

**Critical:** DCMS does not override or reapportion allocation decisions. It receives allocation instructions from the DWP Payment Allocation System and records the result.

---

## Data Model Implications

### Required Entities

1. **Person** — individual claimant or co-claimant
   - Attributes: name, date of birth, NI number, contact details, correspondence address, contact preferences
   - Relationships: may be part of zero or one Account (solo claimant), or part of one Joint Account (co-claimant in UC couple)
   - Vulnerability status, third-party authority, insolvency event, breathing space status: **all Person-level**

2. **Account** — represents a single debt liability
   - Attributes: account number, liability type (UC overpayment, legacy benefit overpayment, etc.), original overpayment amount, creation date, status (active, paused, closed)
   - Relationships: linked to one or more Persons (1 for individual account; 2 for joint UC account)
   - Liability structure: records whether liability is individual or joint and several
   - Regulatory facts (breathing space start date, insolvency event date, death flag, fraud flag): **recorded at Account level for audit**, but enforcement/collection decisions are driven by the status of the linked Person(s)

3. **Joint Account** (specialization or flag on Account)
   - Marks an Account as representing joint and several liability
   - References exactly two Persons (the couple)
   - Tracks original joint state and separation date (if applicable)
   - Upon separation, links to a "Separation Event" that creates two new individual Accounts with 50/50 apportionment

4. **Account-Person Link** — explicit relationship entity
   - Clarifies which person(s) own which account
   - For individual accounts: 1:1 link
   - For joint UC accounts: 2:1 link (two Persons, one Account)
   - Useful for audit trail and querying accounts by person

5. **Regulatory Event** (e.g., Breathing Space Event, Insolvency Event, Death Event, Fraud Event)
   - Attributes: event type, effective date, evidence reference, recording date, recorded by
   - Linked to: Person (for breathing space, insolvency, death) OR Account (for fraud flag)
   - Immutable audit trail

### Entity Relationships (ERD Sketch)

```
Person
  ├─ 1:0..1 Account-Person Link (individual account)
  ├─ 1:0..1 Account-Person Link (co-claimant in joint account)
  ├─ 1:* Regulatory Event (breathing space, insolvency, death)
  ├─ 1:* Third-Party Authority
  ├─ 1:* Vulnerability Flag
  └─ 1:* Contact Preference

Account
  ├─ 1:* Account-Person Link (1 link for individual, 2 for joint)
  ├─ 1:* Payment
  ├─ 1:* Regulatory Event (fraud flag; account-level audit facts)
  ├─ 0..1 Joint Account Marker (if joint and several)
  └─ 1:* Audit Event (CRUD log)

Joint Account Marker (or flag on Account)
  ├─ Account
  ├─ Separation Event (created when couple separates, triggers account split)
  └─ Two Person references (the couple)
```

### Key Design Rules

1. **Never assume a Person's liability from Account type.** Always check the Account-Person link relationship and the Liability Structure field to determine whether an individual is liable as sole debtor or joint and several.

2. **Vulnerability, breathing space, and insolvency decisions must consider the Person's status, not just the Account's status.** An account may be jointly liable, but enforcement decisions differ depending on whether Person A, Person B, or both are in breathing space.

3. **Contact and correspondence details are Person-level.** Ensure communications modules can target individual Persons within a joint Account.

4. **Third-party authority is Person-level.** Authorization to act on Account X does not imply authorization to act for both co-claimants on a joint Account X.

5. **Separation is a critical event.** Upon UC couple separation, trigger a formal "Separation Event" that:
   - Terminates the joint Account.
   - Creates two new individual Accounts.
   - Apportions the debt 50/50 (write off any remaining penny).
   - Updates Account-Person Links.
   - Records the event for audit.

6. **Regulatory events (breathing space, insolvency, death) must be linked to Persons.** Account-level treatment is derived from the Person events, not vice versa.

---

## Edge Cases and Interaction Scenarios

### Scenario 1: Couple in Breathing Space, One Member in Insolvency
- **Person A:** In breathing space (60 days).
- **Person B:** In bankruptcy.
- **Joint Account:** Breathing space protection applies because Person A is in breathing space (joint debts are protected). Additionally, Person B's liability is covered by bankruptcy.
- **Enforcement decision:** Account is protected against all collection and enforcement action for 60 days (or until bankruptcy concludes, whichever is longer).
- **Data recording:** Account has two Regulatory Events (Breathing Space for Person A, Insolvency for Person B). Account-level treatment is determined by the most protective status for any linked Person.

### Scenario 2: Deceased Claimant in Joint Account
- **Person A:** Deceased.
- **Person B:** Living.
- **Joint Account:** Account treatment depends on DWP policy (awaiting client confirmation).
  - Option 1: Account is closed; Person B may have residual liability if debt predates death.
  - Option 2: Account is converted to individual account for Person B.
- **Data recording:** Account has a Death Event linked to Person A. Account-Person Link for Person A is marked as inactive. DWP client policy determines next step.
- **Note:** This scenario requires DWP sign-off on the precise treatment.

### Scenario 3: Joint Account, Both Members Separated, One Repays Their Half
- **Person A & B:** Separated. Joint account split into two 50/50 accounts.
- **Account A1 (Person A's share):** £5,000.
- **Account A2 (Person B's share):** £5,000.
- **Payment received:** Person A makes payment of £5,000.
- **Allocation:** Payment goes to Account A1 (Person A's liability). No automatic apportionment to Account A2.
- **Data recording:** Payment links to Account A1 and Person A. Separation Event recorded showing the 50/50 split.

### Scenario 4: Couple in Joint Account, One Member Becomes Vulnerable
- **Person A:** Not vulnerable.
- **Person B:** Becomes vulnerable (e.g., mental health crisis).
- **Joint Account:** Account-level treatment must respect Person B's vulnerability status even though the account is joint.
  - Collections contact rules may diverge (e.g., extended I&E period for Person B, but not for Person A).
  - Must confirm with DWP whether a joint account with one vulnerable member requires process divergence for the account as a whole or only for Person B.
- **Data recording:** Vulnerability flag on Person B. Account must have logic to check linked Persons' vulnerability status before applying standard collections process.
- **Note:** This scenario requires DWP client sign-off on process divergence rules for joint accounts with mixed vulnerability.

### Scenario 5: Third-Party Authority in Joint Account
- **Person A & B:** Joint account.
- **Representative:** Authorized by Person A only (via Power of Attorney) to act on Account.
- **Representative's scope:** Can act for Person A on the account but not for Person B.
- **Communications and decisions:** Representative can receive correspondence and provide representations for Person A's liability, but cannot make decisions affecting Person B's liability without separate authorization from Person B.
- **Data recording:** Third-Party Authority linked to Person A (not to the Joint Account). Calls to the account must check both Persons' authorization statuses before disclosing information or accepting decisions.

---

## Open Questions Requiring DWP Client Sign-Off

1. **Deceased claimant in joint account:** What is the treatment of a joint UC account if one member dies?
   - Is the account closed immediately?
   - Does the surviving member convert to an individual account and remain liable for the full original debt or the proportional share?
   - What is the interaction with probate and estate recovery?
   - **Owner:** Delivery Lead (escalate to DWP Policy)
   - **Blocker:** Feature for deceased customer account treatment (DW.45, DIC.26).

2. **Joint account with mixed vulnerability:** If one member of a joint UC couple is vulnerable and the other is not, does DWP require:
   - Process divergence for the entire account (treating both as vulnerable for affordability assessment purposes)?
   - Process divergence only for the vulnerable individual?
   - Different collections contact rules for the two members of the couple?
   - **Owner:** Delivery Lead (escalate to DWP Policy)
   - **Blocker:** Feature for vulnerability-driven process divergence (DW.45, DW.25).

3. **Separation event and debt apportionment:** DWP guidance states "split 50/50 on separation." Confirm:
   - Is DWP responsible for identifying separations, or must DCMS receive a notification from UC claim management?
   - If repayment is in progress when separation occurs, what happens to any in-flight payment plans? Are existing arrangements terminated and re-established on the two new accounts?
   - **Owner:** Delivery Lead (escalate to DWP Policy)
   - **Blocker:** Feature for account separation and restructuring (DW.87, DW.23).

4. **Joint account breathing space:** Confirm the treatment of a joint account when one member enters breathing space:
   - Are both members' collection activities suspended, or only the entering member's?
   - If Person B is not in breathing space, can DWP pursue Person B separately while Person A is protected?
   - **Owner:** Delivery Lead (escalate to DWP Policy)
   - **Blocker:** Feature for breathing space handling in joint accounts (DW.45, DW.51).

---

## Data Classification Flags

No new Restricted or special-category-adjacent data elements are identified by this ruling. However, the following existing elements are flagged for attention:

- **Vulnerability flag (DIC.16):** FCA Vulnerability Guidance FG21/1 treats vulnerability as information requiring additional protection. This ruling treats vulnerability as a `Person` attribute, but confirms it should be marked Restricted and handled with need-to-know access control (DIC.16 classification pending DWP legal confirmation per STD-SEC-003).

- **Third-party authority details (DIC.19, DIC.28):** Power of Attorney and representative information includes third-party personal data and legal status. Mark as Restricted.

- **Household composition (new):** To track couple relationships, DCMS must record household composition (i.e., "Person A and Person B are in a relationship and claiming UC jointly"). This is not currently classified. Recommend: Restricted (personal relationship data).

---

## Summary for BA and Builder

### For Business Analyst

Before writing acceptance criteria for any customer-facing feature, confirm:

1. Does the feature apply to individuals or to couples (joint accounts)?
2. If joint accounts are in scope, are there process divergences between the individual and joint cases?
3. For vulnerability, breathing space, insolvency, and third-party authority features, confirm they are Person-level attributes, not Account-level.
4. If the feature involves account separation (on UC couple separation), confirm the 50/50 apportionment and the creation of new individual accounts.

### For Builder

Implement the data model with the following structure:

1. **Person entity** — individual claimant.
2. **Account entity** — single debt liability, linked to 1 or 2 Persons.
3. **Account-Person Link entity** — explicit relationship showing which Persons own which Account.
4. **Regulatory Event entity** — immutable record of regulatory status changes (breathing space, insolvency, death, etc.), linked to Persons.
5. **Third-Party Authority entity** — linked to Person, not Account.
6. **Vulnerability Flag entity** — linked to Person.

Do not implement:
- A "Couple" or "Household" entity (not a DWP concept for debt purposes).
- Household-level regulatory events (all events are Person-level; Account-level treatment is derived).
- Account-level third-party authority (authority is Person-level).

Ensure the Account entity can represent both individual and joint liability structures, and enforce the rule: **enforcement decisions must consider the regulatory status of all linked Persons, not just the Account status.**

---

## References

- [Social Security Administration Act 1992](https://www.legislation.gov.uk/ukpga/1992/4/contents)
- [Universal Credit Regulations 2013 (SI 2013/376)](https://www.legislation.gov.uk/uksi/2013/376/contents)
- [Consumer Credit Act 1974](https://www.legislation.gov.uk/ukpga/1974/39/contents)
- [DWP Benefit Overpayment Recovery Guide (GOV.UK)](https://www.gov.uk/government/publications/benefit-overpayment-recovery-staff-guide/benefit-overpayment-recovery-guide)
- [DWP Working with Representatives Guidance (GOV.UK)](https://www.gov.uk/government/publications/working-with-representatives-guidance-for-dwp-staff/working-with-representatives-guidance-for-dwp-staff)
- [Debt Respite Scheme Guidance for Creditors (GOV.UK)](https://www.gov.uk/government/publications/debt-respite-scheme-breathing-space-guidance/debt-respite-scheme-breathing-space-guidance-for-creditors)
- FCA Vulnerability Guidance FG21/1
- [Shelter England: Recovery of Universal Credit Overpayments](https://england.shelter.org.uk/professional_resources/legal/benefits/universal_credit/recovery_of_universal_credit_overpayments)
- [CPAG: Universal Credit, Deductions and 'Sexually Transmitted' Debt](https://www.tandfonline.com/doi/full/10.1080/09649069.2022.2136712)

---

## Audit

| Field | Value |
|---|---|
| Ruling ID | RULING-001 |
| Created | 2026-04-23 |
| Created by | DWP Debt Domain Expert Agent |
| Last updated | 2026-04-23 |
| Status | final |
| Linked issue | [To be populated by BA] |
| Dependent features | DW.87, DW.23, DW.45, DW.51, DW.31, DIC.16, DIC.19, DIC.28, AAD.x |
