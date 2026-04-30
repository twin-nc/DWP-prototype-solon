# DIAG-04 Talking Points - Entity Model

## Opening - set the scene

"This diagram explains how DCMS thinks about a customer, a debt, and the lifecycle that follows.

The headline is: person-level protections and authority feed each account, but each `DebtAccount` carries its own lifecycle, ledger, Flowable process instance, and repayment arrangements.

That distinction matters because one person can have more than one debt, different debts can be at different stages, and joint Universal Credit debt means more than one person can be linked to the same recoverable account."

Then land the key message:

> "Person facts protect people. DebtAccount facts govern the debt. Flowable runs one lifecycle per DebtAccount."

---

## How to talk through the diagram

Move left to right: `Person`, `Account-Person Link`, `DebtAccount`, `RepaymentArrangement`, then the two lower boxes: case view and treatment guardrail.

---

## 1. Person - who the system is dealing with

"On the left is `Person`. This is the individual claimant, or one member of a joint Universal Credit claim."

The important person-level data is:

- vulnerability flags
- accessibility needs
- third-party authority
- joint liability participation

"These are person-level facts because they describe the individual, not a particular debt. A vulnerability marker, a contact need, or a representative authority can affect how we treat an account, but it starts with the person."

Useful phrase:

> "We do not bury human protections inside an account record. We record them against the person, then every linked account has to respect them."

Mention if useful:

- A person can be linked to many accounts.
- Person-level data is used before treatment, contact, disclosure, or enforcement.
- Third-party authority is person-specific; authority for one person does not automatically grant authority over a joint account partner.

---

## 2. Account-Person Link - the relationship bridge

"The purple box is the `Account-Person Link`. This is the explicit relationship between people and accounts."

Explain the two main cases:

- Individual account: one `Person`, one `DebtAccount`.
- Joint Universal Credit account: two `Persons`, one `DebtAccount`.

"This bridge prevents us from making unsafe assumptions. We do not infer liability just because an account exists. We check the link: who is attached to this account, in what role, and under what liability structure?"

Key phrase:

> "The link is what makes joint liability explicit rather than implied."

Why it matters:

- It supports audit: who was linked to the debt at the time.
- It supports contact and disclosure checks.
- It supports joint-account edge cases: one person vulnerable, one person deceased, one person in breathing space, or one person with third-party authority.

---

## 3. DebtAccount - the process-bearing debt

"The blue box is the most important operational entity: `DebtAccount`. This is the recoverable unit and financial ledger."

DebtAccount carries:

- `breathing_space_flag`
- `insolvency_date`
- `deceased_flag`
- `fraud_marker`
- `flowable_process_instance_id`

"This is where the account's regulatory state and ledger sit. It is also the thing that has exactly one long-running Flowable process instance."

Key message:

> "The debt lifecycle is per account, not per person and not per case view."

Explain why:

"A single person may have several debts at once. One debt might be in an arrangement, another might be under review, another might be awaiting write-off approval. If we used one process instance for the person, those different journeys would get tangled together. One process per `DebtAccount` keeps lifecycle truth clean."

---

## 4. Flowable process instance - one lifecycle per account

"The red box underneath `DebtAccount` is the Flowable process instance. This is the long-running lifecycle for that debt account."

Useful phrasing:

> "Flowable owns lifecycle behaviour; the `DebtAccount` records the durable facts."

Clarify:

- There is exactly one lifecycle process per `DebtAccount`.
- Different accounts for the same person can be at different lifecycle stages.
- Flowable manages timers, tasks, treatment paths, and event subprocesses.
- The database remains the authority for current regulatory facts.

"That last point matters. Process variables can become stale during a long-running workflow. For compliance-sensitive decisions, the process must read current database state, not trust old process snapshots."

---

## 5. RepaymentArrangement - plans belong to one account

"On the right is `RepaymentArrangement`: the plan agreed against one account."

It carries:

- terms
- status
- breach threshold

"A repayment arrangement belongs to a specific `DebtAccount`. That keeps the financial logic precise. Payments, breach rules, and monitoring are tied to the debt they are intended to recover."

Important point:

> "Arrangement monitoring can pause during breathing space, and breach logic must read current account state before taking action."

Why:

"If an account is under breathing space, deceased handling, insolvency, or another protective state, the system must not blindly continue breach activity just because a repayment timer fired."

---

## 6. Case view - what agents see

"The cream box at the bottom left is the case view. This is what an agent needs in practice: a joined-up view across the person, debts, arrangements, and process history."

Important distinction:

> "The case view is a read-model aggregation, not a separate lifecycle object."

Explain:

"That means the UI can show an agent the whole customer picture without creating another source of truth. It summarises across `Person`, `DebtAccount`, arrangements, and Flowable history. But the lifecycle still belongs to the individual debt account."

This prevents:

- duplicate lifecycle state
- confusion over whether the case or account is the real status
- one customer-level process trying to manage several unrelated account journeys

---

## 7. Treatment guardrail - protective checks gate action

"The red-bordered box on the lower right is the guardrail. Before contact, disclosure, deduction, legal referral, or DCA placement, the system checks the current regulatory status of every linked person."

Examples:

- If any linked person is in breathing space, contact and enforcement may be blocked.
- If a linked person is deceased, communication and collection handling change immediately.
- If a person is vulnerable, treatment, affordability, and contact rules may change.
- If a representative is involved, disclosure depends on that person's authority scope.

Key phrase:

> "The account may be the unit of recovery, but people carry the protections that gate recovery."

---

## What the diagram is trying to prove

"There are four design claims on this slide."

1. "Person-level protections are durable facts and must be checked by every linked account."
2. "`DebtAccount` is the recoverable unit, ledger, and process-bearing entity."
3. "`RepaymentArrangement` belongs to one account, so breach and monitoring stay financially precise."
4. "The agent case view is an aggregation, not a competing lifecycle model."

Close this section with:

> "This model lets DCMS handle simple individual debts and complex joint-account situations without losing auditability or mixing up lifecycle state."

---

## Likely questions

**"Why not have one process per customer?"**
Because one customer can have multiple debts at different lifecycle stages. A customer-level process would either become too vague or would have to duplicate account-level lifecycle state. One process per `DebtAccount` keeps each debt journey independent and auditable.

**"Why are vulnerability and third-party authority on Person rather than Account?"**
Because they describe the individual. The same person-level protection or authority may affect more than one account. In joint accounts, authority or vulnerability may apply to one linked person but not the other, so the account must evaluate linked-person facts before acting.

**"What is the Account-Person Link for?"**
It makes liability explicit. It records whether an account is individual or joint, which people are linked to it, and what relationship they have to the debt. That is essential for contact, disclosure, enforcement, and audit.

**"Is the case view a real entity?"**
No. It is a read model for agents. It joins person, account, repayment, and process history into a usable screen, but it does not own lifecycle state.

**"Why does DebtAccount have regulatory flags if regulatory events are person-level?"**
The detailed regulatory event is recorded against the person where appropriate, but account-level flags or dates provide the current derived state needed for fast gating and audit. The design still requires protective decisions to consider all linked persons.

**"What happens in a joint account when one person is protected and the other is not?"**
That is exactly why the link exists. Treatment must check every linked person and apply the most protective relevant rule. Some edge-case policy choices, such as exact deceased or mixed-vulnerability treatment, require DWP sign-off before implementation.

**"Can one repayment arrangement cover multiple accounts?"**
In this model, no. A `RepaymentArrangement` is agreed against one `DebtAccount`. That keeps terms, breach thresholds, and ledger effects clear.

---

## Close-out message

"The model is intentionally simple but careful: people carry protections and authority, accounts carry debt lifecycle and ledger truth, repayment arrangements belong to accounts, and the case view pulls it together for agents without becoming a second source of lifecycle state."

---

*Sources: diag-04-entity-model.drawio, MASTER-DESIGN-DOCUMENT, RULING-001, ADR-012*
