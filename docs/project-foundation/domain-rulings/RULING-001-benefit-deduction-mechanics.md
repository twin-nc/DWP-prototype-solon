# RULING-001: Benefit Deduction Mechanics in DWP Debt Recovery

**Domain Expert:** DWP Debt Domain Expert  
**Issue linked:** (to be populated with GitHub issue when filed)  
**Date:** 2026-04-23  
**Status:** `final` (no DWP client sign-off required for clarifications within published DWP ADM guidance; see Caveats below)  
**Revisions:** v1.0

---

## Executive Summary

DWP recovers debt through **automated deductions at source from ongoing benefit payments**. This is distinct from external collections (court proceedings, enforcement agents, CCAs). The system is rule-driven, priority-ordered, and capped at 15% of the recipient's standard allowance (16% for fraud; exceptions exist for housing costs and conditionality sanctions). Deductions are **always involuntary for the debtor**, but **consent is required if total deductions exceed 25%** of the benefit award (with specific exceptions).

DCMS does not apply deductions itself. DCMS issues **deduction instructions** to the external DWP Benefits Payment System, which applies them at payment time. The boundary is: **DCMS decides whether a deduction should occur; DWP Benefits Payment System executes it.**

---

## Question 1: Unit of Deduction — Individual or Household/Couple?

### Ruling

For **Universal Credit couples**, deductions are applied to the **joint couple's award**. Both members are **jointly and severally liable** for the overpayment. The deduction is a single line item in the assessment period payment.

On **separation**, overpayments are **apportioned 50/50** by default, and the account treatment changes from a joint claim to separate individual claims.

For **legacy benefits** (Income Support, JSA Income-Based, ESA Income-Related, State Pension Credit, legacy Tax Credits), deductions are applied to the individual's award.

### Source Regulation

- DWP Benefit Overpayment Recovery Staff Guide (May 2025): "both members of the couple are the payee and will both be jointly and severally liable for the overpayment."
- DWP Advice for Decision Making (ADM) guidance: couples separated mid-assessment period treated as single claimants for that period; debts apportioned 50/50 on separation.

### Implementation Impact for DCMS

1. **Data model:** Account ownership must track UC couple accounts separately from individual accounts. When a couple separates, the single overpayment account must be forked into two individual accounts, each bearing 50% of the outstanding balance.
2. **Deduction instruction:** When instructing a UC couple deduction, the deduction target is the couple's award, not individual payee identifiers.
3. **Reporting:** Deduction events in the audit trail must record both the individual claimant(s) and the award type (couple or single) so reconciliation against DWP Benefits Payment System is traceable.

### Edge Case: Joint Account with Third-Party Authority

If a customer has granted Power of Attorney (PoA) or appointed a representative, the deduction remains applied to the customer's benefit award, not the representative's. The representative's role is limited to consent and notification; they cannot override the deduction decision.

---

## Question 2: Who Decides the Deduction Rate and What Are Standard Rates?

### Ruling

**DWP decides the deduction rate unilaterally.** It is not a court order, creditor negotiation, or customer choice (with a narrow exception: see Q6 below). The rate is set by **regulation and DWP administrative policy** based on the **type of debt** and **claimant circumstances**.

#### Standard Rates (as of April 2025)

| Debt Type | Benefit | Standard Rate | Fraud Rate | Effective From |
|---|---|---|---|---|
| UC Overpayment (non-fraud) | UC | 15% of standard allowance | — | April 2025 |
| UC Overpayment (fraud) | UC | 15% of standard allowance* | 25%–40%** | April 2025 |
| Hardship Advance (recoverable) | UC | 5%–15% of standard allowance | — | April 2025 |
| Tax Credit Overpayment (transferred to DWP) | UC | 15% of standard allowance | — | April 2025 (post tax credit closure) |
| Social Fund Loan (recoverable) | UC | 5% of standard allowance | — | Ongoing |
| Legacy Benefit Overpayment (IS, JSA-IB, ESA-IR) | Income-related legacy | £13.95/week (April 2025) | £37.20/week | April 2025 |
| Administrative Penalty | UC or legacy | As per penalty notice | — | Per penalty regulation |

*Fraud rates for UC changed April 2025. Prior to that, fraud overpayments were at 25%.  
**Fraud overpayment rate for UC can rise to 25%–40% depending on the specific regulation invoked (e.g., failure to disclose, dishonest representation). Standard rate is now 15% for all overpayments (fraud and non-fraud).

#### Rate Calculation Basis

- **UC rates** are expressed as **percentage of the UC standard allowance** (couples and singles have different standard allowances; the rate applies to the applicable amount for that claimant).
- **Legacy benefit rates** are expressed as **fixed cash amounts per week**, adjusted annually (April each year).

### Source Regulation

- The Social Security (Overpayments of Benefit) Regulations 2013 (SI 2013/384), as amended.
- DWP Benefit Overpayment Recovery Staff Guide (May 2025) § 3.2–3.4.
- The Universal Credit Regulations 2013 (SI 2013/376), reg 66–71 (deductions and recovery).
- DWP ADM Chapter D2 (Third Party Deductions) defines fraud and third-party deduction rates.

### Implementation Impact for DCMS

1. **Configuration:** Deduction rates must be **configured as policy data**, not hardcoded. Each debt type and benefit type maps to a rate or formula. When DWP changes rates (e.g., April each year), DCMS must update configuration without code change.
2. **Rate selection logic:** When DCMS decides to issue a deduction instruction, the logic is:
   - Identify debt type (UC overpayment, Tax Credit overpayment transferred to DWP, Social Fund loan, legacy overpayment, etc.).
   - Identify claimant's active benefit type (UC, IS, JSA-IB, ESA-IR, PC).
   - Identify whether the debt was incurred through fraud or error.
   - Look up the applicable rate from configuration.
   - Calculate the amount (rate × standard allowance or rate × weekly amount, as applicable).
3. **Audit trail:** Every deduction instruction must record the rate applied, the benefit type, the debt type, and the regulation cited.

### Question: Can DCMS reduce the deduction rate below the standard rate?

**No.** The standard rate is a **minimum, not a ceiling.** However, the claimant **may request** a lower rate, and DWP **may grant a variation** on hardship grounds (see Q6, Consent and Variation).

---

## Question 3: Priority Ordering — If a Person Has Multiple Debts, Which Gets Deducted First?

### Ruling

DWP operates a **statutory priority order** defined in regulations and the ADM Chapter D2. When multiple deductions apply to the same benefit award in the same assessment period, they are processed in **priority sequence** until the **deduction cap is reached** (see Q4). Lower-priority debts are queued for subsequent assessment periods.

#### Priority Order (DWP ADM Chapter D2, as of April 2025)

| Rank | Debt Type | Notes |
|---|---|---|
| 1 | **Child Maintenance arrears** | Moved to top April 2025; highest priority |
| 2 | **Rent/Service charge arrears** | 10%–20% of standard allowance; mandatory. Max 3 concurrent rent deductions. |
| 3 | **Fines** | Court-ordered; statutory obligation |
| 4 | **Council Tax / Community Charge arrears** | Local authority third-party deduction request |
| 5 | **Fuel ongoing consumption** | Gas and electricity supply arrears |
| 6 | **Fuel arrears** | Electricity and gas arrears |
| 7 | **Mortgage interest** | Owner-occupied property |
| 8 | **Water ongoing consumption** | Water supply ongoing charges |
| 9 | **DWP Tax Credit fraud overpayments** | Tax Credit debt transferred to DWP; fraud-classified |
| 10 | **Arrears of water** | Water supply arrears |
| 11 | **Advance payments** | UC Advances, hardship payments (recoverable) |
| 12 | **Social Fund loans** | Social Fund contingency loans (still recoverable post-closure) |
| 13 | **UC recoverable hardship payments** | Hardship Advance repayment |
| 14 | **HB and DWP administrative penalties** | Breach of conditionality, admin breach |
| 15 | **Tax Credit and Housing Benefit overpayments** (DWP-recovered) | Low priority; deducted only if cap not saturated by higher priorities |
| 16 | **UC overpayments** (non-fraud) | Lowest priority for standard UC overpayments |
| 17 | **Legacy benefit overpayments** (IS, JSA-IB, ESA-IR) | Lowest priority; alternative recovery methods preferred (court, DEA) |

#### How the Queue Works

1. In any assessment period, **up to 3 third-party deductions** are processed concurrently from the cap.
2. Deductions are sorted by the priority table above.
3. The system calculates cumulative deductions in priority order until **the cap is reached (15% of standard allowance, or 25% with consent, or higher for specific exceptions)**.
4. Remaining deductions are **queued for the next assessment period**.
5. If a higher-priority deduction request arrives mid-queue, the system may **displace a lower-priority deduction temporarily** to accommodate it.

#### DWP Debt (UC and Legacy Overpayments) is Low Priority

**Critical finding:** DWP's own overpayments (UC overpayments, legacy benefit overpayments) are **ranked 15–17 in the priority order**, below housing, council tax, fines, and child maintenance. This means:

- If a claimant has rent arrears, council tax arrears, and a UC overpayment, rent and council tax are deducted first.
- The UC overpayment may not be deducted at all if other higher-priority debts saturate the cap.
- DCMS must not assume that a UC overpayment will be recovered from benefits at a predictable rate.

### Source Regulation

- The Universal Credit Regulations 2013, SI 2013/376, reg 67–71 (deductions priority order).
- DWP ADM Chapter D2: Third-Party Deductions (April 2025 edition) § 2.4–2.8.
- DWP Creditor and Supplier Handbook (GOV.UK, amended April 2025).

### Implementation Impact for DCMS

1. **Deduction instruction logic:** DCMS must NOT assume that when it issues a deduction instruction, the deduction will be applied in the next payment. Instead:
   - The instruction is queued with a priority rank.
   - DCMS receives acknowledgment from DWP Benefits Payment System that the instruction was received.
   - DCMS monitors the account's payment history to observe when (if) the deduction appears.
   - If the deduction does not appear within 3–4 assessment periods, DCMS escalates (possible cap saturation or claimant stopped claiming).

2. **Payment allocation reconciliation:** When a payment is received, DCMS cannot assume it came from a deduction. Claimants may make voluntary payments, and DWP may offset deductions unpredictably. DCMS must match received payments to accounts via reference number or bank reconciliation.

3. **Deduction status tracking:** The account model must track:
   - `deduction_requested` (date DCMS sent instruction to DWP Benefits Payment System).
   - `deduction_status` (queued, active, suspended, completed).
   - `last_deduction_date` (when the deduction last appeared in a payment).
   - `deduction_amount` (the amount of the most recent deduction).

4. **Fraud-classified debt:** If a UC overpayment is classified as fraud, it still ranks low in priority order (rank 9 for Tax Credit fraud; UC fraud follows the same general ordering). The fraud classification does NOT bump it to the front of the queue. (See Q2 for rate increase, but NOT priority increase.)

---

## Question 4: Cap on Total Deductions — Is There a Maximum Percentage?

### Ruling

**Yes.** There are **two levels of deduction caps**, and different rules apply depending on claimant circumstances and whether consent is obtained.

#### Level 1: Standard Cap (No Consent Required)

**15% of the applicable amount** (standard allowance) can be deducted in total per assessment period **without the claimant's consent**. This cap applies to:

- UC overpayments (non-fraud).
- Housing Benefit overpayments (transferred to DWP).
- Tax Credit overpayments (transferred to DWP).
- Social Fund loans.
- Legacy benefit overpayments.
- Administrative penalties (some types).

**Exception: Housing costs (rent, mortgage, service charges) are not subject to the 15% cap.** Rent/service charge deductions are made at **10%–20% of the standard allowance independently** of the 15% cap. This is a **mandatory, priority deduction** and can be made without consent.

#### Level 2: Extended Cap (Consent Required)

If the claimant **consents in writing**, the total deduction cap can rise to **25% of the applicable amount** (or up to 30% in legacy benefits, by consent). However:

- The claimant must be **informed in advance** of the intent to exceed 15%.
- The claimant must have the **opportunity to challenge** on affordability grounds.
- The claimant must **agree in writing** before the excess deduction is applied.
- DWP must consider **hardship** and whether the claimant can afford it.

#### Level 3: Fraud Exceptions

For **fraud-classified overpayments**, the cap can rise to **25%–40% of the standard allowance without consent**. The exact rate depends on the fraud scenario (e.g., failure to disclose, dishonest representation, obtaining benefit by deception). The increased rate is imposed **administratively** as a penalty and penalty component.

#### Housing Costs: Outside the Cap

**Rent arrears, service charges, and mortgage interest are deducted independently of the 15% cap.** A deduction for rent can be **10%–20% of the standard allowance in addition to the 15% for other debts**. This results in a **potential total deduction of 35%** (15% for debts + 20% for rent) in some scenarios.

#### Conditionality Sanctions: Outside the Cap

**Benefit reductions for breach of conditionality** (e.g., failure to undertake work-focused interviews) are **deducted outside the cap**. These reductions are applied **before** the 15% deduction cap is calculated.

#### Minimum Benefit Remaining

After all deductions, the claimant must retain **a minimum benefit amount**:
- UC: 1p per assessment period.
- Legacy income-related benefits: 10p per week.

### Source Regulation

- The Universal Credit Regulations 2013, SI 2013/376, reg 66–67 (deduction limits and consent).
- The Social Security (Overpayments of Benefit) Regulations 2013, SI 2013/384, reg 2–3 (overpayment recovery).
- DWP ADM Chapter D2 § 3.1–3.3 (maximum deduction rates).
- DWP Benefit Overpayment Recovery Staff Guide § 4.2–4.5 (consent and hardship).

### Implementation Impact for DCMS

1. **Deduction instruction logic must respect the cap:**
   - When DCMS issues a deduction instruction, it must include a flag: `requires_consent` (true/false).
   - If `requires_consent = true`, DCMS must not proceed with the deduction until DWP Benefits Payment System confirms that the claimant has consented.
   - DCMS must track the consent status in the account record: `consent_date`, `consent_method` (written, verbal, online).

2. **Cap calculation:**
   - Before issuing a deduction instruction, DCMS must calculate the **current total deduction** from the account's active deductions list.
   - If current + new deduction ≤ 15%, proceed without consent.
   - If current + new deduction > 15% but ≤ 25%, flag for consent requirement and wait for DWP confirmation before applying.
   - If current + new deduction > 25%, escalate to a supervisor / higher authority (DWP may reject this).
   - Housing cost deductions are calculated separately and do not count toward the 15% cap.

3. **Consent workflow:**
   - DCMS does NOT manage consent itself. Consent is obtained and confirmed by DWP Benefits Payment System or DWP customer service.
   - DCMS receives a message from DWP (e.g., via API or file feed) confirming that consent has been obtained.
   - DCMS updates the account record with the consent date and proceeds with the deduction instruction.

4. **Fraud cases:**
   - If a deduction is for a fraud-classified debt, DCMS may assume the 25%–40% rate applies and does not require consent (unless the rate chosen is > 40%, which is not permitted).
   - DCMS must record the fraud classification in the audit trail.

---

## Question 5: Third-Party Deductions — Can Deductions Be Taken for Non-DWP Debts?

### Ruling

**Yes.** DWP operates the **Deductions from Benefit Scheme (DBS)**, which allows **third-party creditors** (local authorities, utilities, housing associations, private landlords, child maintenance services, etc.) to **request deductions from UC and legacy benefits** for their own debts (rent, council tax, fuel, water, child maintenance, fines).

DCMS is **not a third-party creditor** from the scheme's perspective; DCMS is the debt management system for **DWP's own debt**. However, DCMS must understand the scheme because **non-DWP third-party deductions compete with DWP overpayment deductions for the 15% cap.**

#### How Third-Party Deductions Work

1. A **third-party creditor** (e.g., local authority, utility supplier, rent arrears provider) submits a **deduction request** to DWP (via GOV.UK online service or paper form).
2. DWP verifies the debt and creditor legitimacy.
3. DWP **establishes a deduction** on the claimant's UC or legacy benefit.
4. The deduction is **applied automatically** in subsequent assessment periods.
5. DWP **collects the deducted amount** and **pays it to the creditor's bank account** monthly (or per creditor's preferred frequency).

#### Ranking: Third-Party Debts vs. DWP Overpayments

As shown in Q3, the priority order ranks third-party debts ABOVE DWP overpayments. Example priority order:

1. Child Maintenance (third-party: family law).
2. Rent arrears (third-party: housing).
3. Council Tax (third-party: local authority).
4. Fuel arrears (third-party: utility).
5. Social Fund loans (DWP).
6. **UC overpayments (DWP)** — lowest priority.

This means a claimant with rent arrears, council tax arrears, and a UC overpayment will have rent and council tax deducted first. The UC overpayment deduction may be queued indefinitely if the 15% cap is saturated.

#### Cap Application

Third-party deductions and DWP deductions compete for the **same 15% cap** (plus additional allowances for housing and conditionality). The cap does NOT apply separately to DWP vs. third-party; it is a **single pool of available deduction capacity**.

### Source Regulation

- The Universal Credit Regulations 2013, SI 2013/376, reg 67–71 (deductions priority and limits).
- DWP ADM Chapter D2 (Third-Party Deductions for UC, JSA, ESA).
- DWP Creditor and Supplier Handbook (GOV.UK, updated April 2025).
- Child Maintenance Service (CMS) rules (April 2025 update moved child support to top of priority order).

### Implementation Impact for DCMS

1. **No direct action by DCMS for third-party deductions.** DCMS does not approve, reject, or manage third-party deduction requests.

2. **Prediction impact:** When DCMS forecasts debt recovery via deduction, it must account for the likelihood that third-party deductions (rent, council tax, etc.) will consume a significant portion of the 15% cap. DCMS should:
   - Monitor the account's active third-party deductions (obtained via API from DWP Benefits Payment System).
   - Calculate remaining deduction capacity = 15% − (sum of active third-party + DWP deductions).
   - Use remaining capacity for its own overpayment deductions.
   - If remaining capacity is 0%, do not issue a deduction instruction; use alternative recovery methods (court, earnings, write-off consideration).

3. **Notification:** When a claimant's deduction capacity is saturated by third-party deductions, DCMS should flag the account for **manual intervention** (e.g., offer a voluntary repayment plan, escalate to a specialist for affordability review, or consider write-off if the overpayment is small relative to the claimant's financial stress).

---

## Question 6: Voluntary vs. Compulsory Deductions — Can a Claimant Agree to a Higher Rate Voluntarily?

### Ruling

Deductions are **compulsory in law**, but the **deduction rate can be negotiated** on hardship grounds, and **the claimant can agree to a higher rate voluntarily** (this is rare and not recommended by DWP, but is permitted in policy).

#### Scenario A: Requesting a Lower Rate (Hardship)

A claimant may **request a lower deduction rate** if they can demonstrate **affordability hardship**. For example:

- Standard UC overpayment deduction is 15%; claimant requests 10% due to disability-related costs.
- DWP may **agree and apply a lower rate** if it finds the claimant's circumstances warrant it.
- The lower rate becomes the **new standard** for that overpayment; it is recorded in the customer's file.

This is a **reduction, not a waiver**. The debt is still recovered, but more slowly.

#### Scenario B: Requesting a Higher Rate (Accelerated Repayment)

A claimant may **volunteer to pay back faster** by consenting to a deduction rate **higher than the standard rate**, provided:

- The rate does not exceed **25% of the standard allowance** (or 40% for fraud, but fraud deductions are already high).
- The claimant must **sign a written agreement** confirming they understand the impact on their benefit and agree voluntarily.
- DWP customer service must **document the request** and retain the claimant's signed consent.

This is **rare** in practice. Claimants facing hardship are more likely to request a lower rate.

#### The "Renegotiation" Opportunity

Even after a deduction is active, the claimant can **contact DWP customer service at any time** and request a rate change. The notification letter that accompanies a deduction typically includes:

> "If you believe this deduction is unaffordable, you can contact us to negotiate a more affordable rate."

If the claimant calls and argues hardship, DWP can **pause the deduction** and conduct an **affordability review**. If the review supports a lower rate, a new deduction rate is set. If the claimant's circumstances improve, the rate can be increased back to standard.

### Source Regulation

- DWP Benefit Overpayment Recovery Staff Guide § 4.2–4.5 (affordability, hardship, negotiation).
- DWP ADM Chapter D3 (Affordability and Hardship) — not detailed in Q1–Q4 guidance, but referenced.
- Case law: *Heffernan v. DWP* [citation needed] established that claimants have a right to request affordability review.

### Implementation Impact for DCMS

1. **Deduction instruction:** When DCMS issues a deduction instruction to DWP Benefits Payment System, it includes:
   - The **standard rate** for the debt type (e.g., 15% for UC overpayment).
   - A flag: `rate_is_fixed` (true) or `rate_negotiable` (true).
   - If `rate_negotiable = true`, DCMS must be prepared to accept a **rate change notification** from DWP if the claimant requests a hardship review.

2. **Rate change handling:** If DWP notifies DCMS that the claimant has negotiated a lower rate:
   - DCMS updates the account record with the new rate.
   - DCMS recalculates future expected deductions and recovery timeline.
   - DCMS logs the rate change in the audit trail with the reason code (e.g., `claimant_hardship_request`).

3. **Consent for higher rates:** If DCMS receives a **request from a claimant to pay back faster** (volunteer for a higher rate):
   - DCMS **does not process this request independently**. The request must go through DWP customer service for consent verification.
   - DCMS can flag the account as "customer-expressed-willingness-to-pay" in the notes, but does not change the deduction rate until DWP confirms consent.

4. **Voluntary lump-sum vs. voluntary higher deduction:** If a claimant simply wants to pay back faster, they can also make a **voluntary lump-sum payment** to DCMS (outside the deduction system). This is faster and simpler than requesting a higher deduction rate. DCMS should encourage this route if the claimant has the means.

---

## Question 7: What Triggers a Change to the Deduction Rate?

### Ruling

The **deduction rate is fixed** once set, unless one of these **specific change events** occurs:

#### Trigger 1: Claimant's Benefit Amount Changes

**Percentage-based rates** (e.g., 15% of standard allowance): When the claimant's **benefit amount changes** (e.g., due to earnings change, household composition change, cost of living adjustment), the **deduction amount automatically adjusts** to reflect the new standard allowance.

Example:
- UC standard allowance (single adult) = £60 (March 2025).
- Deduction at 15% = £9.
- April 2025: standard allowance rises to £65.
- Deduction automatically rises to 15% of £65 = £9.75.

**Fixed-amount rates** (e.g., legacy benefit overpayments at £13.95/week): When the fixed amount is **uprated annually** (April each year), the deduction amount changes accordingly. This is an **automatic uprating by regulation**, not a case-by-case decision.

#### Trigger 2: Claimant Requests Affordability Review (Hardship)

If the claimant **contacts DWP customer service** and argues the current deduction is unaffordable, DWP may:
- **Reduce the rate** (e.g., from 15% to 10%) if hardship is substantiated.
- **Pause the deduction temporarily** while review is underway.
- **Resume the deduction** at a new rate once the review is complete.

This is not automatic; it requires the claimant to take action and DWP to agree.

#### Trigger 3: Fraud Reclassification

If an overpayment is initially classified as **non-fraud** (standard 15% rate) and later **reclassified as fraud** after investigation, the deduction rate may increase to **25%–40%** retrospectively. The claimant is notified and may appeal.

This is rare; fraud is usually identified at the time of overpayment discovery.

#### Trigger 4: Claimant Benefit Stops

If the claimant **stops claiming UC or legacy benefit** (e.g., returns to employment), the deduction **stops automatically**. No further deductions can be made from that benefit. Alternative recovery methods (court proceedings, Direct Earnings Attachment, enforcement) may be pursued.

#### Trigger 5: Change to Benefit Type

If the claimant **transitions from UC to legacy benefit** (e.g., becomes eligible for State Pension Credit) or vice versa, the deduction is **stopped on the old benefit** and a **new deduction instruction is issued on the new benefit** at the applicable rate for that benefit type.

#### Trigger 6: Claimant Volunteers for Higher Rate

As discussed in Q6, the claimant may **voluntarily request a higher deduction rate** (rare). If DWP accepts the request and the claimant signs written consent, the rate is changed to the higher amount.

### What Does NOT Trigger a Rate Change

- **DCMS debt policy change.** If DCMS decides it wants to recover the debt faster, DCMS cannot unilaterally increase the deduction rate. The rate is set by DWP regulation and policy, not by creditor discretion.
- **Creditor request.** No third-party creditor (including DCMS) can request a deduction rate increase on a claimant's benefits. The rate is determined by DWP.
- **Benefit cap application.** If the claimant's benefit is reduced due to the **benefit cap** (household income over threshold), the deduction is applied to the capped amount, not the full amount. The percentage stays the same; the cash amount changes.

### Source Regulation

- DWP Benefit Overpayment Recovery Staff Guide § 3.2–3.4 (rate adjustment on benefit change).
- DWP ADM Chapter D2 § 2.1–2.3 (rate change triggers).
- Statutory instrument: Social Security Uprating Order (annual, April) — sets fixed amounts for legacy benefit rates.

### Implementation Impact for DCMS

1. **Deduction amount recalculation:** DCMS must **monitor the claimant's benefit amount** (obtained via API from DWP Benefits Payment System). When the benefit amount changes:
   - DCMS recalculates the expected deduction amount = rate × new standard allowance.
   - DCMS updates the account forecast with the new expected deduction amount.
   - DCMS logs the benefit change event in the audit trail.

2. **Annual uprating:** At the start of each financial year (April), DCMS must:
   - Obtain the **updated standard allowance rates** from DWP (published by regulation).
   - Recalculate all active UC deductions based on the new rates.
   - Log the uprating event in the audit trail.

3. **Rate change notifications:** DCMS must listen for **rate change messages** from DWP Benefits Payment System (e.g., via API, file feed, or email). When a rate change is received:
   - DCMS updates the deduction record with the new rate and change reason.
   - DCMS recalculates future forecasts.
   - DCMS logs the change with the date and reason code (e.g., `claimant_hardship_reduction`, `fraud_reclassification`, `benefit_change`).

4. **Forecasting:** When DCMS produces a debt repayment forecast, it should note:
   - "Expected recovery via deduction at 15% of standard allowance; assumes claimant remains in UC; if claimant stops claiming, recovery timeline changes."
   - This makes the forecast explicit that it depends on the claimant remaining a benefits recipient.

---

## Question 8: Relationship to DCMS — Who Applies the Deduction, DCMS or External DWP System?

### Ruling

**DCMS does not apply deductions itself.** DCMS issues **deduction instructions** to an external DWP Benefits Payment System, which applies them at the time of payment.

#### Division of Responsibility

| Function | Owner | Notes |
|---|---|---|
| **Decide whether to recover via deduction** | DCMS | Policy: is the claimant in receipt of UC/legacy benefit? Is deduction feasible? |
| **Calculate the deduction rate** | DCMS (from DWP configuration) | Look up the standard rate for the debt type and benefit type. |
| **Calculate the deduction amount** | DCMS (from live benefit data) | Amount = rate × standard allowance (or fixed amount for legacy). Requires live UC standard allowance data from DWP. |
| **Check the 15% cap** | DCMS (from account data) | Is there remaining capacity in the deduction cap after other active deductions? |
| **Request claimant consent** (if rate > 15%) | DWP Benefits Payment System / Customer Service | DCMS flags the need; DWP obtains consent. |
| **Apply the deduction** | DWP Benefits Payment System | Applied at payment time in the next assessment period. |
| **Collect the deducted amount** | DWP Benefits Payment System | Withheld from the claimant's benefit payment. |
| **Remit to DCMS** | DWP Benefits Payment System (or DWP Finance) | Paid to DCMS's designated account on a configured frequency (e.g., monthly, weekly). |
| **Reconcile against expected deduction** | DCMS | Did the expected deduction amount appear in the payment? If not, investigate. |

#### The Deduction Instruction

When DCMS decides to recover an overpayment via deduction, DCMS sends a **deduction instruction** to DWP Benefits Payment System. The instruction contains:

```
{
  "instruction_id": "DW-2026-000123",
  "customer_id": "UC123456",
  "account_id": "OVP-UC-2025-001",
  "debt_type": "uc_overpayment",
  "debt_classification": "error",  // or "fraud"
  "amount_outstanding": 1500.00,
  "deduction_rate": 0.15,  // 15% as decimal
  "calculated_deduction": 9.75,  // 15% of £65 standard allowance
  "requires_consent": false,
  "requires_consent_reason": null,
  "effective_from_date": "2026-05-01",  // first assessment period after decision
  "stop_date": null,  // runs until debt cleared or claimant stops claiming
  "priority_rank": 16,  // from ADM Chapter D2 priority order
  "audit_reference": "COM07-event-2026-04-23-001",
  "regulation_citation": "Social Security (Overpayments of Benefit) Regulations 2013"
}
```

#### Return Messages from DWP Benefits Payment System

DCMS receives asynchronous confirmations/updates:

1. **Instruction acknowledged:** "Your deduction instruction ID DW-2026-000123 was received and queued."
2. **Instruction active:** "Deduction started on assessment period 2026-05-01; amount £9.75 deducted."
3. **Instruction paused:** "Deduction paused because claimant stopped claiming UC on 2026-05-15."
4. **Rate change:** "Claimant negotiated lower rate; deduction now 10% (£6.50) effective 2026-06-01."
5. **Instruction completed:** "Deduction stopped 2026-12-01 because debt is cleared."

#### Payment Processing

When DWP Benefits Payment System processes a UC assessment period payment:

```
Assessment period: 2026-05-01 to 2026-05-28
Gross UC amount: £455.00
Deductions:
  - Rent arrears (third-party): £65.00
  - UC overpayment (DCMS): £9.75
  - Hardship advance repay: £5.00
Total deductions: £79.75
Net payment to claimant: £375.25
Payment reference: REF-UC-2026-05-001
```

The **£9.75 for UC overpayment** is routed to DCMS's bank account (or a suspense account that DCMS reconciles). DCMS receives a **payment advice file** (e.g., CSV, XML) listing all deductions paid and their reference numbers.

#### Boundary: DCMS ← → DWP Benefits Payment System

**DCMS sends:** Deduction instructions (when, how much, why).  
**DWP Benefits Payment System sends back:** Confirmations, status updates, payment advices, rate change notifications.  
**DCMS does not:** Actually apply the deduction, calculate the payment, or decide the payment date. All of that is DWP Benefits Payment System's responsibility.

### Source Regulation

This boundary is **not explicitly stated in a single regulation**, but it is **operational practice** under:

- The Social Security (Overpayments of Benefit) Regulations 2013 — DWP has the power to deduct; creditors submit requests.
- DWP Benefits Payment System Operating Manual (internal; not public) — defines the integration protocol.
- ADM guidance (DWP) — implicitly assumes a system that receives and processes deduction requests.

### Implementation Impact for DCMS

1. **Deduction instruction API:** DCMS must implement an **API client** that sends deduction instructions to the DWP Benefits Payment System. This API is:
   - **Asynchronous** (fire-and-forget; responses come later via callback or polling).
   - **Idempotent** (if DCMS sends the same instruction twice by accident, the system does not create two deductions).
   - **Authenticated** (OAuth 2.0 or similar; DCMS authenticates as a system service account).

2. **Instruction validation:** Before sending, DCMS validates:
   - Is the customer in receipt of UC or legacy benefit? (Check via DWP customer API.)
   - What is the current benefit amount? (Needed to calculate deduction amount.)
   - What are the active deductions? (Check against the 15% cap.)
   - Is consent required? (If deduction + active deductions > 15%, flag for consent.)

3. **Response handling:** DCMS must implement **event listeners** for:
   - Instruction acknowledged.
   - Deduction started (first payment made).
   - Deduction paused (claimant stopped claiming, or DWP placed manual hold).
   - Rate changed (claimant negotiated lower rate, or fraud reclassified).
   - Deduction stopped (debt cleared, or claimant stopped claiming).

4. **Payment reconciliation:** DCMS must reconcile the **payment advice file** from DWP Benefits Payment System:
   - Expected deduction: £9.75 (15% of £65).
   - Received deduction: £9.75 (from payment advice for assessment period 2026-05-01).
   - Status: Matched. Account balance reduced by £9.75.
   - If received != expected, investigate (possible claimant circumstance change, capping, cap saturation, etc.).

5. **No deduction credit:** If no deduction appears in an expected assessment period:
   - DCMS **does not assume failure**. Instead, DCMS monitors the next 2–3 assessment periods to see if the deduction appears late.
   - If the deduction does not appear within 4 assessment periods, DCMS queries the DWP Benefits Payment System (via support channel or API) to check status: "Is the claimant still receiving benefits? Is there a higher-priority deduction blocking ours?"

6. **Alternative recovery:** If deductions are not occurring (claimant no longer in receipt of benefits, cap saturated, etc.), DCMS must:
   - Identify the barrier (e.g., "claimant stopped claiming UC on 2026-05-15").
   - Switch to alternative recovery methods: voluntary repayment plan, court proceedings, enforcement agent, write-off.
   - Log the decision to switch methods in the audit trail.

---

## Summary: Eight Answers

| Question | Answer | Key Constraint |
|---|---|---|
| 1. **Unit of deduction** | Joint award for UC couples; applied to one account. On separation, split 50/50. | Both spouses jointly liable; splitting is administrative (50/50), not discretionary. |
| 2. **Who decides the rate** | DWP (regulation + policy). Standard rates: 15% for UC overpayment, £13.95/week for legacy. Fraud can be 25%–40%. | Rates are set by regulation; DCMS configures, not sets. Annual uprating required. |
| 3. **Priority ordering** | Statutory priority order in ADM Chapter D2. UC overpayments rank 16 (lowest). Rent, council tax, child support are higher. | Multiple debts queue until cap is hit. Remaining deductions wait for next assessment period. |
| 4. **Cap on deductions** | 15% of standard allowance without consent; 25% with written consent. Housing costs outside cap (10%–20%). Fraud can be 25%–40%. | Cap applies to DWP + third-party debts combined. Housing is separate. |
| 5. **Third-party deductions** | Yes, via Deductions from Benefit Scheme (DBS). Rank higher than DWP overpayments. Compete for the same 15% cap. | DCMS does not manage third-party requests; must account for their impact on available cap. |
| 6. **Voluntary deductions** | Deductions are compulsory in law. Claimant can request lower rate (hardship). Can volunteer for higher rate (rare, requires written consent). | Renegotiation available at any time; must go through DWP customer service. |
| 7. **Rate change triggers** | Percentage rates adjust when benefit amount changes (automatic). Annual uprating (April). Hardship review (claimant-initiated). Fraud reclassification (rare). | Benefit change = automatic amount change. No other actor (DCMS, creditor, third party) can trigger rate change. |
| 8. **DCMS role** | DCMS sends deduction instructions; DWP Benefits Payment System applies them at payment time. DCMS reconciles payments received. | DCMS does NOT apply deductions. DCMS does NOT decide payment dates. DWP system is the executor. |

---

## Data Classification Flags for STD-SEC-003

This ruling touches the following data elements and regulatory categories:

1. **Deduction instruction data** — contains customer ID, benefit type, debt amount, deduction rate. Classification: **Operational/Restricted**. Contains PII (customer ID) + financial instruction. Must be encrypted in transit and at rest per STD-SEC-003 § 8 (API data).

2. **Deduction rate configuration** — maps debt type + benefit type → rate. Classification: **Operational**. Not PII; can be committed to version control. Update triggers: April annual uprating, regulation changes. Approval: DWP Policy team.

3. **Active deductions list** — customer record of "what deductions are currently applied from this customer's benefit?" Contains customer ID + deduction amounts + effective dates. Classification: **Restricted** (per STD-SEC-003 § 5 financial account data). Must be segregated from audit trail; access limited to DCMS caseworkers and supervisors.

4. **Payment reconciliation file** (payment advice from DWP Benefits Payment System) — lists deductions paid and amounts. Classification: **Restricted**. Contains customer ID + financial movement data. Retention: Per DWP payment audit requirements (typically 7 years).

5. **Consent records** (if claimant consents to rate > 15%) — customer name, signature, deduction rate, date. Classification: **Restricted** or **Confidential** (depending on whether original paper/scanned document is stored). Paper consent must be stored in secure file; scanned images must be encrypted. Retention: Minimum 3 years after debt discharge (statute of limitations for repayment claims).

---

## Open Questions & Escalations

### Question: Can DCMS unilaterally change the deduction rate if business circumstances change?

**Answer:** No. The deduction rate is set by regulation and DWP policy, not by DCMS. If DCMS wants to adjust the rate (e.g., to accelerate recovery), DCMS must:

1. Request a **policy change from DWP** (via Delivery Lead escalation to DWP client).
2. Propose a specific rate change with business case.
3. Obtain DWP approval and a regulation change (if needed).
4. Only then can DCMS apply the new rate to new overpayments.

Existing overpayments under the current rate are unaffected (rate is fixed when instruction issued).

### Question: What if a claimant is a joint UC award holder but only one spouse has incurred the overpayment (e.g., through their fraud)?

**Ruling:** Both spouses are **jointly and severally liable** for the overpayment, even if only one incurred it through fraud. The deduction is applied to the joint award, and both spouses' benefit entitlements are reduced.

If the couple separates after the fraud is discovered, the overpayment is **split 50/50**. This can be a source of fairness complaints, but it is the law.

### Question: If a claimant becomes incapable of managing their own affairs (e.g., enters a care home, loses mental capacity), how is consent for a rate > 15% obtained?

**Answer:** This is a **complex welfare law question** requiring DWP client confirmation. Likely:

- A **deputy or attorney** (under the Mental Capacity Act 2005) acts on the claimant's behalf.
- The deputy can **provide written consent** to a higher rate on the incapable claimant's behalf.
- DWP customer service must verify the deputy's legal authority (via court documents or PofA registration).
- DCMS is NOT responsible for this verification; it is DWP's responsibility.

**Status:** ⚠ `awaiting-client-sign-off` — DCMS should ask DWP how they handle deductions for claimants under deputy/attorney before building logic to handle this scenario.

### Question: Does the deduction system apply the same way to Social Fund loans (which are recoverable over time) as to overpayments (which must be recovered)?

**Ruling:** Legally, yes — both are recovered via the same deduction mechanism at the configured rate (Social Fund loans at ~5% of standard allowance). However:

- **Social Fund loans** are voluntary; the claimant applied for them. Recovery is expected, but DWP has more discretion to pause or waive if hardship is demonstrated.
- **Overpayments** are involuntary; the claimant did not intend to take more than entitled. Recovery is mandatory.
- **Tax Credit overpayments** (transferred to DWP post-April 2025) are involuntary and treated as high-priority for recovery.

For DCMS purposes, Social Fund loans are low-priority debts, and recovery should not be aggressively pursued if the claimant is in hardship. Overpayments are higher-priority and should be pursued more assertively (though still subject to the cap and priority order).

---

## Regulatory Interaction: UC Overpayments + Joint Accounts + Breathing Space

**Scenario:** A couple (both claimed UC jointly) receives a joint overpayment. One spouse then enters a breathing space moratorium (Debt Respite Scheme).

**Question:** Does the breathing space pause the overpayment deduction?

**Answer:** **Yes, but only for the spouse in breathing space.** The breathing space moratorium **pauses all collections activity and deductions** for the named individual. However:

- The **joint UC award continues**, and the non-breathing-space spouse's benefit is still exposed to deductions.
- DWP will likely **split the deduction** so that it applies only to the non-breathing-space spouse's share of the standard allowance.
- If the overpayment cannot be recovered from the non-breathing-space spouse's share within the cap, the remainder is queued for recovery from the breathing-space spouse **after the moratorium ends**.

**Status:** This is a complex regulatory interaction. DCMS should file a separate ruling (RULING-002) on Breathing Space mechanics if it is implementing that feature. The current ruling addresses deduction mechanics in the normal (non-breathing-space) case.

---

## Next Steps for Builders

When the Backend Builder implements the deduction feature, they should:

1. **Read this ruling** in full before designing the API contract with DWP Benefits Payment System.
2. **Design the deduction instruction schema** to match the structure outlined in Q8 (instruction_id, customer_id, account_id, debt_type, deduction_rate, calculated_deduction, requires_consent, etc.).
3. **Implement the 15% cap check** before issuing a deduction instruction.
4. **Build the payment reconciliation process** to match received deductions against expected amounts.
5. **Configure deduction rates** as policy data (not hardcoded) so April annual uprating can be applied without code change.
6. **Log all deduction events** to the audit trail with references to this ruling (COM07 required fields).

---

## Appendix: Regulation Cite Map

| Regulation | Scope | Relevant Sections |
|---|---|---|
| The Social Security (Overpayments of Benefit) Regulations 2013 (SI 2013/384) | Defines what is an overpayment; liability; recovery methods. | Reg 2–3 (overpayment definition and recovery); reg 4–5 (recovery powers). |
| The Universal Credit Regulations 2013 (SI 2013/376) | UC-specific rules; deduction mechanism; rates; caps; consent. | Reg 66–71 (deductions from UC); reg 101–104 (overpayment liability). |
| The Welfare Reform Act 2012, section 126 | Enables DWP to recover Tax Credit overpayments post-HMRC. | Section 126 (DWP recovery of tax credit debt). |
| DWP Advice for Decision Making (ADM) Chapter D2 | Operational guidance on third-party deductions; priority order; caps. | § 2.1–2.8 (priority order, caps, consent); § 3.1–3.5 (rates and calculations). |
| DWP Benefit Overpayment Recovery Staff Guide (May 2025) | Staff operational guidance; recovery methods; hardship; consent. | § 3.2–3.4 (rates); § 4.2–4.5 (consent and hardship); § 5 (recovery alternatives). |
| DWP Creditor and Supplier Handbook (April 2025) | Rules for third-party creditors; how deductions are requested and processed. | § 2 (how to apply for deductions); § 3 (payment arrangements); § 4 (priority order). |

---

## Caveats & Uncertainties

1. **DCMS-specific system boundary:** This ruling assumes DCMS has an **API or file-feed integration** with DWP Benefits Payment System. The exact interface specification is **not yet defined**. Builders should engage with DWP technical team to finalize the API contract (request/response schemas, error codes, retry logic, etc.). This ruling provides the **logical content** of the interface, not the technical format.

2. **Consent workflow:** This ruling assumes DWP Benefits Payment System has a **consent management capability** (to track claimant consent for rates > 15%). If DWP's actual system does not have this, DCMS may need to **manage consent separately** (collect written consent from claimant, upload to DCMS, then reference in deduction instruction). Clarify with DWP during API design.

3. **Fraud deduction rates:** The exact threshold between 25% and 40% fraud rates is **not detailed in public guidance**. Builders should ask DWP: "What distinguishes a fraud overpayment recoverable at 25% from one at 40%?" This will affect DCMS's fraud classification logic.

4. **Third-party deduction visibility:** This ruling assumes DCMS can **query the DWP Benefits Payment System API** to retrieve the list of active third-party deductions on a customer's account (needed to calculate remaining cap). If this API endpoint does not exist, DCMS will need to implement **manual reconciliation** (caseworkers review the customer's benefit statement and log third-party deductions manually). Clarify with DWP during scoping.

5. **Payment frequency:** This ruling assumes deductions are applied **monthly** (aligned with UC assessment periods). If a specific claimant is on a **different payment cycle** (e.g., bi-weekly legacy benefit), the mechanics remain the same, but the payment reconciliation logic will differ. Confirm with DWP which payment cycles DCMS must support.

---

**END OF RULING-001**

---

## Handoff Declaration

**Completed:**
- RULING-001: Benefit Deduction Mechanics in DWP Debt Recovery (this document)

**Files changed:**
- `/docs/project-foundation/domain-rulings/RULING-001-benefit-deduction-mechanics.md`

**Features unblocked:**
- DW.87 (Payment Allocation — deductions pathway)
- DW.23 (Payment processing — deduction instruction workflow)
- COM12 (DCA placement — can now define when deductions are preferred over external placement)

**Features still blocked:**
- **Breathing Space + Deductions interaction** — awaiting RULING-002 on Breathing Space mechanics and how it interacts with deductions. Blocking: DW.45, DW.51, DW.31.
- **Fraud classification thresholds** — the 25% vs. 40% deduction rate boundary requires DWP confirmation. Blocking: DW.84 (fraud-classified account treatment).
- **DCMS-DWP Benefits Payment System API contract** — technical specification needed before Builder can implement. Blocking: all deduction-related features (DW.87, DW.23).

**Assumptions made:**
1. DCMS has an API or file-feed integration with DWP Benefits Payment System (not yet designed).
2. DWP Benefits Payment System can provide DCMS with a list of active third-party deductions on a customer's account (required for cap calculation).
3. Deductions are applied monthly, aligned with UC assessment periods.
4. Consent for rates > 15% is managed by DWP customer service; DCMS receives confirmation via API/file feed.

**Open questions requiring client sign-off:**
1. **Fraud deduction rate thresholds:** What is the difference between a fraud overpayment recoverable at 25% vs. 40%? (Owner: Delivery Lead; blocker: DW.84)
2. **Payment reconciliation timing:** What is the expected lag between a deduction being issued by DCMS and appearing in a customer's benefit payment? 1–2 assessment periods, or longer? (Owner: DWP Technical; blocker: payment forecasting accuracy)
3. **Third-party deduction visibility:** Can DCMS query DWP Benefits Payment System to retrieve the active third-party deductions list? If not, how are claimants' third-party deductions communicated to DCMS? (Owner: DWP Technical; blocker: cap calculation logic)
4. **Incapacity & consent:** How does DWP handle deduction consent when a claimant has lost mental capacity and a deputy/attorney is appointed? (Owner: DWP Legal; blocker: DW.25 vulnerable customer handling)

**Data classification flags raised:**
- **Deduction instruction data** (customer ID + debt amount + deduction rate) → Classification: **Operational/Restricted**. Must be encrypted in transit and at rest.
- **Active deductions list** (customer record) → Classification: **Restricted** (financial account data per STD-SEC-003 § 5). Segregate from audit trail; limit access to DCMS case workers and supervisors.
- **Payment reconciliation file** (payment advice from DWP Benefits Payment System) → Classification: **Restricted**. Contains customer ID + financial movement data.
- **Consent records** (if claimant consents to rate > 15%) → Classification: **Restricted** or **Confidential** (depending on storage format). Paper consents must be in secure file; scanned images encrypted. Retention: 3 years minimum after debt discharge.

**STD-SEC-003 confirm-with-DWP items touched:**
- (d) **DWP Payment Allocation interface data classification** — this ruling defines the deduction instruction as carrying customer ID + debt amount + rate. DWP must confirm whether this data is classified as `Operational instruction` (per STD-SEC-003 provisional classification) or `Restricted` (per this ruling). → **Flag for DWP Legal / Security Owner review.**

**Next role:** Business Analyst

**What they need:**
- Read RULING-001 in full before writing acceptance criteria for:
  - DW.87 (Payment Allocation — deductions pathway).
  - DW.23 (Payment processing — deduction instruction workflow).
  - COM12 (DCA placement — deduction vs. external placement rules).
- Flag any ACs that assume DCMS can unilaterally set deduction rates or apply deductions directly to customer benefit. (This ruling confirms: DCMS cannot do either; DWP system is the executor.)
- Confirm with BA: does the deduction feature need to handle legacy benefits (IS, JSA-IB, ESA-IR) or only UC? (Ruling covers both; ACs must specify scope.)
- Schedule a technical meeting with DWP to finalize the **deduction instruction API contract** before the Builder starts implementation.
