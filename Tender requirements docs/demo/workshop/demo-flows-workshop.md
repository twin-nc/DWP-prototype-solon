# DWP Debt Collection — Workshop Demo Flows
## Detailed Presenter Guide: Flows 1–6

> **Audience:** Solution architects, business analysts, consultants, and pre-sales engineers preparing to demonstrate the Netcompany / Solon Tax–based debt collection solution in response to the DWP Future Debt Service ITT.
>
> **Purpose:** This document provides a thorough, narrative-level description of each of the six primary demonstration flows. It is intended as a preparation guide for the demonstration team and as supporting material for workshop sessions. It goes beyond the high-level scenario outline to describe what the evaluator will see at each step, what is happening inside the platform, what the DWP-specific sensitivities are, and how each moment should be framed.

---

## How to Use This Guide

Each flow is structured as follows:

1. **Context and framing** — what this flow is really proving and why DWP cares
2. **Pre-conditions** — what needs to be set up before the demo begins
3. **Detailed journey** — step-by-step walkthrough with presenter notes, screen descriptions, and behind-the-scenes explanation
4. **Moment of truth** — the single most impactful reveal and how to deliver it
5. **Evaluator Q&A preparation** — likely questions and how to answer them well
6. **Requirements coverage** — which ITT criteria are evidenced, and how

---

## Flow 1: From Account Intake to First Meaningful Contact

### Theme
**The platform knows what to do before an agent touches a single account.**

### Context and Framing

This flow should open the day. It establishes the fundamental proposition that the Solon Tax–based debt collection platform is an active operational engine, not a passive case database. Every debt management system can store accounts. The evaluator is asking whether the platform can autonomously classify, prioritise, enrich, and act on those accounts at scale and speed before any human involvement.

The DWP context is important here. DWP expects to manage very large volumes of benefit overpayment and debt accounts with a workforce that cannot be sized to manually triage everything. The only credible answer to that constraint is a platform that does the triage itself — and this flow should make that answer undeniable.

This flow also sets up the narrative thread for the rest of the day. Every subsequent flow shows what happens after an account enters the system. Flow 1 shows how accounts get there and how the system prepares them.

### Pre-Conditions

- A realistic batch of 50–200 new delinquent accounts has been prepared as a structured data file or is available via a simulated host-system API endpoint.
- The accounts include a realistic spread: some with clean data, some with validation errors, some with matches to existing customer records, and some that will trigger specialist routing (vulnerability flags, high balance, fraud markers).
- Collection strategies, segmentation rules, and queue configurations are already set up.
- Agent and supervisor logins are ready to demonstrate post-intake state.

### Detailed Journey

#### Step 1 — Initiating the Intake

The presenter opens the batch or API integration configuration view. This is not a blank screen — the integration channel shows the scheduled intake window, the expected file format or API contract, and the history of recent successful runs.

**What to show:** The presenter triggers the intake manually (or it fires automatically on schedule). A live progress indicator shows records being received and processed. The count ticks up.

**What's happening in the platform:** The Solon Tax process engine fires the intake BPMN process. Records arrive in the integration layer, where they are queued for sequential processing. Each record is handled as an individual case creation event.

**Presenter note:** This is a good moment to mention that the same mechanism works for real-time API-pushed single accounts and bulk batch files. Integration is not two separate systems — it is one configurable intake layer.

---

#### Step 2 — Validation and Data Quality Gating

As records are processed, the platform validates each one against a configurable ruleset. Required fields are checked. Known-invalid formats (malformed National Insurance numbers, impossible dates) are flagged. Records that cannot be processed are quarantined.

**What to show:** Open the validation exception queue. A handful of records are there, each with a specific reason code. One might say "NI number format invalid." Another might say "Debt amount missing." Show that these records have not been discarded — they are held in a reviewable state with full original data preserved.

**What's happening in the platform:** Each validation check is a configurable rule in the intake process. The list of checks, their severity (hard stop vs. soft warning), and their routing outcomes are all defined in the strategy layer, not hardcoded.

**Presenter note:** Emphasise that data quality problems are surfaced as workable exceptions, not silently dropped records. This directly addresses ITT Demo 1 criteria on data integrity and error management.

---

#### Step 3 — Customer Matching and Deduplication

For each valid incoming account, the platform attempts to match the arriving customer to an existing customer record. The matching logic evaluates National Insurance number, date of birth, name, and address fields — in a configurable precedence order.

**What to show:** Navigate to one of the newly created cases. Show the customer record. Highlight that the platform identified this as an existing customer and has linked the new debt to the existing customer profile, preserving the relationship to previous accounts and any historic notes, restrictions, or vulnerability markers.

For contrast, show a case where no match was found and a new customer record was created automatically.

**What's happening in the platform:** The matching engine runs as part of the intake BPMN workflow. Match confidence scoring determines whether the link is confirmed automatically or routed for manual review.

**Presenter note:** This is where to introduce the DWP household concept briefly. If two incoming accounts share an address and one is already marked as part of an existing household, the intake logic can flag potential household linkage for review. This threads forward neatly to Flow 4.

---

#### Step 4 — Enrichment

Once matched or created, each account is enriched. Enrichment may include:

- pulling balance and product data from the host system via API,
- checking internal suppression lists (deceased, dispute, prior bankruptcies),
- scoring the account against risk and collections segmentation models,
- applying benefit status flags (On Benefit / Off Benefit) from DWP internal data feeds.

**What to show:** Open a newly enriched account. The case now shows fields that were not present in the raw intake file — benefit status, delinquency stage, risk band, and a calculated priority score. Show the enrichment audit trail: each data item has a source, a timestamp, and a version reference.

**What's happening in the platform:** Enrichment steps are modelled as service task calls within the intake BPMN process. Each enrichment source is a configurable connector. New sources can be added without redesigning the flow.

---

#### Step 5 — Segmentation and Prioritisation

Once enriched, the platform assigns each account to a segment and calculates a treatment priority. Segmentation is rule-driven: a configurable matrix evaluates balance, delinquency stage, risk band, benefit status, and product type to produce a segment code.

**What to show:** Filter the intake batch by segment. Show that accounts have spread across multiple segments automatically. Highlight one high-value, early-delinquency account that has been ranked top priority, and one lower-balance account that has been placed in a lower-intensity treatment path.

**What's happening in the platform:** Segmentation rules are defined in the strategy configuration layer. Business users can modify them — adding new segment criteria, adjusting thresholds, reordering priorities — without touching code.

---

#### Step 6 — Queue Assignment and Work Allocation

Each segmented account is assigned to a work queue. Queue assignment follows configurable routing rules: specialist debt types go to specialist queues, vulnerability-flagged accounts go to the vulnerable-customer team, high-value accounts may go to a senior-agent queue.

**What to show:** Open the queue management view. Show the overnight batch has populated multiple queues. Some queues already have accounts waiting. Agent capacity targets and current queue depths are visible side by side.

**What's happening in the platform:** Queue assignment is an automatic step in the post-segmentation workflow. Rules evaluate account attributes, agent skill profiles, team capacity, and SLA parameters to determine optimal assignment.

---

#### Step 7 — First Communications Triggered

For accounts in the appropriate early-delinquency strategy, the platform has already triggered first-contact communications. Letters have been queued to the third-party communications platform. SMS messages are pending for accounts with a mobile number and a strategy step that permits SMS.

**What to show:** Open the communications log for a newly ingested account. Show a demand letter queued and, where applicable, an SMS scheduled. Show the strategy rule that drove this — it fired automatically because the account met the segment conditions.

**Presenter note:** This is a good time to show the communications platform integration. The platform calls the Notifications API with the template reference and the account-specific data. The physical sending of the letter or SMS happens in the third-party platform. The debt management system holds the event log and the trigger record.

---

#### Step 8 — Agent Experience at Login

Switch to an agent persona. Log in as an agent. Show the agent's worklist: it is pre-populated, pre-prioritised, and ready. Each item on the list shows the customer name, debt amount, delinquency stage, product type, and the recommended first action.

The agent does not need to decide what to work next. The platform has already made that decision.

**What to show:** Open the top item on the worklist. Show the full case context — account history, customer profile, linked debts, prior communications, notes, and any flags. Everything the agent needs to handle the first interaction is available without navigating to multiple systems.

---

#### Step 9 — Supervisor Dashboard

End the flow by opening the supervisor view. Show the overnight transformation: accounts received, validated, enriched, segmented, queued, and in some cases already communicated with — all before agents began work.

Show queue volumes, capacity utilisations, and exception counts. Show which segments have grown overnight and whether SLA risk is visible. Show the system has already flagged any queues with potential capacity issues before the working day starts.

### Moment of Truth

**The evaluator sees that several hundred accounts have been classified, prioritised, and — in many cases — already contacted before a single agent has started their day.** Pause here. Name the number. "Two hundred and twelve new accounts were received overnight. Every one of them has been validated, matched, segmented, and queued. Forty-seven have already had a first communication sent. No agent made any of those decisions."

This is not a feature. It is an operational model. That distinction should be explicit.

### Evaluator Q&A Preparation

**"What happens if the host system is unavailable?"**
The intake process includes configurable retry and fault-tolerance logic. Records are queued and processing resumes when connectivity is restored. No data is lost. An alert is raised to the operations team.

**"Can we change the segmentation rules ourselves?"**
Yes. Segmentation rules are defined in the strategy configuration layer. A suitably permissioned business user can modify thresholds, add segments, and activate changes through a review-and-approve workflow without IT involvement.

**"How does this handle the benefit status flag?"**
Benefit status is treated as a first-class data field, not a free-text note. It influences segmentation (On Benefit accounts may receive different treatment strategies) and can trigger specific workflows such as forbearance assessment.

### Requirements Coverage

| DWP Demo Criteria | How This Flow Evidences It |
|---|---|
| Demo 1.1 — Flexible integration (batch, API, events) | Batch file intake and host-system API call shown |
| Demo 1.5 — Debtors and debts created via API and batch | Both creation paths shown |
| Demo 1.7 — Notifications API for letters and SMS | First-treatment communications triggered |
| Demo 2.4 — Decisioning for assignment and segmentation | Automated segmentation and queue assignment |
| Demo 2.6 — Workflows at user/team level | Queue rules and strategy rules are user-configurable |
| Demo 2.19 — Work allocation and prioritisation | Worklist pre-populated for agents |

---

## Flow 2: Customer in Financial Difficulty — Vulnerability to Resolution

### Theme
**The platform protects the right customers immediately and automatically. Compliance is not a manual step.**

### Context and Framing

This is the most emotionally resonant flow in the demonstration day. DWP debt collection sits at the intersection of financial recovery and public-sector obligations toward citizens who may be in genuine distress. The tension between recovering money that is owed and protecting people who are vulnerable is not resolved by policy alone — it has to be embedded in the platform.

This flow proves that protection is automated, immediate, and complete — not dependent on an agent remembering to check a box. It also proves that protection and recovery are not mutually exclusive: the platform does not simply freeze the case. It routes it correctly, supports a structured affordability conversation, and creates a sustainable repayment outcome where one is possible.

DWP evaluators will weight this flow heavily. The requirements explicitly call out vulnerability management, affordability assessment, and compliance controls. This is one of the clearest ways to demonstrate all three together.

### Pre-Conditions

- An account exists in active treatment — it has had one or more standard collection contacts and is currently in a standard queue.
- The account has no existing vulnerability flags.
- An income and expenditure reference data set is configured (standard expenditure thresholds, household composition categories).
- Two agent personas are available: a standard collection agent and a specialist vulnerability agent with appropriate role permissions.

### Detailed Journey

#### Step 1 — Agent Opens the Account with Full Context

The standard collection agent receives a call. The account appears on their screen via screen pop — the customer has been identified through ID&V. The agent sees:

- the current arrangement status (active plan in place, or no plan),
- the outstanding balance and the delinquency stage,
- the treatment history — what letters have been sent, whether previous calls resulted in promises to pay,
- any prior notes,
- the customer's current contact restrictions (none at this point).

**What to show:** The case screen. Highlight the richness of the context available without any manual searching. The agent is fully briefed before saying a word.

---

#### Step 2 — Customer Discloses a Vulnerability

During the conversation, the customer discloses that they are experiencing mental health difficulties. This may be expressed in many ways. The agent interface guides the agent through the disclosure recording process.

**What to show:** The agent opens the vulnerability capture workflow. A structured form prompts for the type of vulnerability, severity, any supporting notes, and the source of disclosure (self-declared by customer). The form also prompts the agent to confirm whether temporary or ongoing.

**What's happening in the platform:** The vulnerability record is a structured data entity — not a free-text note field. It carries a type code, a severity level, a start date, an optional end date for temporary vulnerabilities, and the agent ID of the recorder.

**Presenter note:** Show the list of configurable vulnerability types. DWP may have its own taxonomy. This list is not hardcoded — it is reference data that can be maintained by a configured business user.

---

#### Step 3 — Automatic Suppression on Save

The agent saves the vulnerability record. **At this exact moment, the following happen simultaneously without any further agent action:**

1. All outbound automated contact activities are suppressed — letters in queue, scheduled SMS, dialler tasks.
2. The account is removed from the standard collection queue.
3. A suppression event is written to the audit trail with a timestamp.
4. The account is flagged on the supervisor dashboard as a recently vulnerability-declared case.
5. A routing recommendation is generated: specialist vulnerable-customer team.

**What to show:** After saving, navigate to the communications queue. The pending letter and SMS that were waiting are now suppressed — they have not been sent, and they will not send while the vulnerability flag is active. Return to the case. Show the suppression event in the audit trail. Show the new queue assignment.

**Presenter note:** Make the pause deliberate here. "Watch what happened when the flag was saved." Then show the suppressed communications and the audit event. This is the moment of truth. Do not rush it.

---

#### Step 4 — Transfer to Specialist with Full Context

The standard agent transfers the case to the vulnerability specialist queue. The transfer carries the full case context — notes, history, suppression event, and the vulnerability record itself.

**What to show:** The specialist logs in and sees the case in their queue, flagged as a new vulnerability referral. They open it and see the same complete history the standard agent had — with the addition of the vulnerability disclosure details now visible.

**What's happening in the platform:** Case transfer is a workflow action. The platform tracks the transfer event, the sending agent, the receiving queue, and the timestamp. No context is lost between agents.

---

#### Step 5 — Income and Expenditure Capture

The specialist begins the affordability conversation. The I&E capture form guides the specialist through a structured conversation: household income sources, fixed outgoings, variable expenditure, and assets.

**What to show:** Walk through a realistic I&E capture — employment income, housing cost, utility costs, food, transport. Show that the form uses configurable reference data for standard expenditure thresholds (the platform suggests reasonable ranges based on household composition).

**What's happening in the platform:** The I&E record is a structured data entity. Each field is validated. The platform can compare declared expenditure against reference thresholds and flag significant variances for the specialist's attention.

**Presenter note:** If the platform connects to bureau or credit reference data, this is where to show that contextual comparison. "The customer has declared housing costs of £850 per month. The platform shows that the median for a two-adult household in this postcode is £780–£920. No variance flagged."

---

#### Step 6 — Affordability Calculation and Option Presentation

Once I&E is complete, the platform calculates the disposable income — the amount available for debt repayment after essential outgoings. It then presents a set of repayment options:

- the minimum affordable amount,
- a realistic plan given the debt balance and likely repayment duration,
- any statutory forbearance options (breathing space, time to pay).

**What to show:** The specialist sees the calculated disposable income. The platform presents three repayment frequencies (weekly, fortnightly, monthly) with amounts calibrated to the disposable figure. The specialist can discuss these with the customer.

**What's happening in the platform:** The affordability engine applies a configurable calculation model — the formula, the floor, the ceiling, and the reference table for standard expenditure are all configurable. DWP can adapt this model to its policy requirements without code changes.

---

#### Step 7 — Arrangement Creation

The specialist and customer agree on a plan. The specialist selects the agreed frequency and amount. The platform creates the arrangement.

**What to show:** The arrangement is created. The case shows the new plan: start date, payment schedule, amounts, total expected recovery, and the calculated end date. A confirmation communication is automatically generated and queued.

**What's happening in the platform:** The arrangement creation triggers a new monitoring workflow. The system will now watch for each payment due date, apply tolerance rules if a payment is slightly late, and escalate automatically if a payment is missed. The specialist does not need to calendar-manage this case.

---

#### Step 8 — Audit Trail End-to-End

End the flow with the full audit trail open.

**What to show:** The chronological event history on this case since the call began: screen pop, I&E capture, vulnerability disclosure, automatic suppression, transfer to specialist, I&E completion, affordability calculation, arrangement creation, confirmation communication queued. Every event shows the acting user or automated system, the timestamp, and the data state before and after the change.

**Presenter note:** "If DWP were ever asked by a regulator or parliamentary committee to explain the treatment of a specific customer on a specific date, this is the answer. It is already here. Nobody needs to reconstruct it."

### Moment of Truth

**The instant the vulnerability marker is saved, all outbound collection activity stops.** The evaluator sees protection happen automatically — before the specialist has spoken a word, before a supervisor has been notified, before any manual step has taken place. Frame it clearly: "Customer protection is not a training issue on this platform. It is an automated system behaviour."

### Evaluator Q&A Preparation

**"What if the vulnerability flag is incorrect and needs to be removed?"**
A permissioned user can update or remove a vulnerability record. The removal event is also logged. Automatic suppression lifts when the flag is removed, based on the new data state. No manual queue restoration is needed.

**"Can vulnerability types be configured to different suppression behaviours?"**
Yes. A mental health disclosure might trigger full contact suppression. A temporary vulnerability (recent bereavement) might trigger a different, less restrictive protocol. The mapping between vulnerability type and the resulting system behaviour is configurable.

**"What happens to the arrangement if the customer's circumstances change?"**
The arrangement is a live entity. It can be reviewed, amended, or escalated. If the customer re-enters difficulty, a new I&E can be run and the arrangement adjusted. Every amendment is versioned and audited.

### Requirements Coverage

| DWP Demo Criteria | How This Flow Evidences It |
|---|---|
| Demo 2.14 — Treatments triggered by vulnerability events | Automatic suppression and re-routing on vulnerability save |
| Demo 2.15 — Payment options (plans, flexibility) | Arrangement creation with configurable frequency/amounts |
| Demo 2.16 — Repayment plan flexibility | Frequency, amounts, start date all agent-selectable |
| Demo 3.8 — Customer vulnerability with levels, type, temporary | Structured vulnerability form with full attribute set |
| Demo 3.14–15 — Customers in difficulty and affordability | I&E capture, disposable income calculation, options presented |
| Demo 3.16–17 — Forbearance appropriateness | Platform presents options; specialist confirms most appropriate |
| Demo 2.23–24 — Audit and restricted user actions | Full timestamped event log; automatic suppression not overridable by standard agent |

---

## Flow 3: Arrangement Breach — Escalation to Third-Party Collection Placement

### Theme
**The platform escalates without hesitation, without error, and without gaps in the paper trail.**

### Context and Framing

This flow proves that the platform does not depend on human vigilance to advance accounts through the collections lifecycle. When an arrangement is breached, the system detects it, acts on it, communicates appropriately, and — when internal recovery is exhausted — places the account with a third-party agency. Every step leaves an evidence trail.

For DWP, this is important for two reasons. First, DWP needs confidence that accounts will not sit in breached-but-unnoticed states because an agent forgot to check or a supervisor missed a report. Second, the financial and legal accuracy of third-party placement and payment reconciliation is a material risk area. This flow should demonstrate both operational automation and financial control in the same storyline.

### Pre-Conditions

- An account exists with an active repayment arrangement — a plan has been agreed, and at least one prior payment has been recorded.
- The system is configured with breach tolerance rules (e.g. a payment more than three days late triggers a first warning; a missed payment after a previous warning triggers breach).
- A third-party collection provider has been configured as a third-party placement target with the relevant data-mapping and API or file transfer settings.
- A supervisor persona is available for the override/exception step.

### Detailed Journey

#### Step 1 — Payment Due and Not Received

The arrangement has a payment due on a specific date. The payment monitoring workflow is watching for it. The due date passes. No payment is received.

**What to show:** Open the account and the arrangement monitor. Show the payment due date and the status: not received. Show the tolerance window — the system applies a configurable grace period before confirming breach. Show that the grace window is now active.

**What's happening in the platform:** The payment monitoring process — a BPMN timer-boundary event — fires when the due date passes. It evaluates the payment received flag. If no payment has been recorded, the tolerance timer starts.

---

#### Step 2 — First Breach Notification

The grace period expires with no payment received. The platform confirms the account as breached.

**What to show:** The breach event is logged. A first breach communication is automatically generated — a letter or SMS depending on the strategy — and queued to the communications platform. The account is flagged on the exception dashboard.

**What's happening in the platform:** The breach workflow fires. Breach workflows are configured per segment and strategy: the sequencing of communications, the waiting periods between contacts, the maximum number of contact attempts, and the conditions that advance the case to the next stage are all strategy-level configurations.

---

#### Step 3 — Sequenced Re-Engagement Attempts

The platform sequences multiple contact attempts over the configured breach period. Letters, SMS messages, and possibly outbound call tasks are generated according to the strategy.

**What to show:** Navigate through the communications log over the breach period. Show each contact event: first letter sent, first SMS sent, outbound call task created and worked by an agent. Each event has a timestamp, a channel, a result (delivered, no answer, declined).

**Presenter note:** This is a good place to show how the platform tracks contact outcomes. If an SMS is not responded to, the next step fires. If an agent makes a call and the customer promises to pay, that promise is recorded as a contact outcome event — and the system creates a new monitoring step to watch for that promise payment.

---

#### Step 4 — Non-Response Escalation

After the configured re-engagement sequence is exhausted with no response or payment, the platform promotes the case to a pre-placement holding state. The account appears on an exception queue for supervisor review.

**What to show:** The exception queue. The supervisor sees the account listed with the full breach timeline: date of breach, contacts attempted, outcomes, elapsed time since breach. The supervisor can see immediately whether the account meets the configured escalation criteria.

**What's happening in the platform:** The escalation gate is a workflow decision point. The rules that determine whether automatic placement is permitted or whether supervisor approval is required are configurable. Some segments may auto-place; others may require explicit approval.

---

#### Step 5 — Supervisor Override Option

Show the supervisor reviewing the account. They can choose to override and retain the case internally — for example, if they know the customer has been trying to contact the team and a payment is expected shortly.

**What to show:** The supervisor opens the account and sees the full history. They use the override control to pause the escalation and add a review note. The case is removed from the placement queue and placed in a short-term hold with a follow-up trigger.

**Presenter note:** Show both paths if time allows — override for one account, and let another proceed to placement. The key point is that the system surfaces the decision and records the outcome, but does not prevent legitimate human judgement.

---

#### Step 6 — Third-Party Collection Placement

For accounts that meet the criteria and are not overridden, the platform places the account with the configured third-party collection provider.

**What to show:** The placement event. The platform generates a placement data package — customer details, debt summary, balance, contact history — and transmits it to the third-party collection provider via the configured channel (API or structured file). A placement acknowledgement is received and logged.

**What's happening in the platform:** At the moment of placement, the internal contact strategy for this account is suspended. No letters, SMS messages, or call tasks will fire while the account is with the third-party collection provider provider. The two processes cannot run in parallel — the system enforces this.

**Presenter note:** "This is the financial handover moment. From here, DWP's internal collection activity stops. The third-party collection provider is now responsible for contact. If DWP's system continued to send letters at the same time, DWP would be in breach of its own process — and the customer would receive contradictory communications. The platform prevents that automatically."

---

#### Step 7 — Payment Received via Third-Party Collection

A payment is subsequently received from the third-party collection provider. This might be a full settlement or a partial payment.

**What to show:** The payment is recorded. The platform allocates it to the correct debt — principal, fees, interest, in the configured allocation order. The balance is updated. The third-party collection's commission or fee is calculated and recorded if applicable. A reconciliation record is created.

**What's happening in the platform:** Payment allocation follows a configurable waterfall rule. The order in which payments are applied — to which components of the debt — is defined in the strategy layer. This can be adapted to DWP's specific accounting requirements.

---

#### Step 8 — Third-Party Collection Performance Reporting

Open the third-party collection management view. Show the performance reporting for the configured agency: placements made, payments received, recovery rate, average time to first payment, and outstanding balance with the agency.

**What to show:** A summary view for the third-party collection provider. Show that the data is real-time — it reflects the payment just recorded. A supervisor can assess whether the third-party collection provider is performing within expected parameters or whether intervention is needed.

### Moment of Truth

**The full elapsed timeline from first missed payment to third-party collection placement is visible in a single view — every automated step, every contact attempt, every rule-based decision, and every elapsed time marker.** Open the timeline view and read it aloud: "Payment missed on the 3rd. Grace period applied. Breach confirmed on the 6th. First letter sent same day. SMS sent on the 9th. Outbound call attempted on the 12th — no answer. Escalated for supervisor review on the 15th. Placed with third-party collection provider on the 16th." This is not a report that was compiled afterwards. It was written in real time by the system as events occurred.

### Evaluator Q&A Preparation

**"What if the third-party collection provider sends a payment back for a different amount than expected?"**
The payment reconciliation process handles partial and unexpected payments. Variances are flagged for review if they fall outside tolerance. Every allocation is audited.

**"Can different segments have different breach escalation paths?"**
Yes. A high-balance commercial debt might have a longer internal re-engagement window and require senior approval before placement. A lower-value consumer debt might auto-escalate after two missed contacts. These paths are configured separately in the strategy layer.

**"How is the third-party collection relationship managed when the debt is resolved?"**
When a debt placed with a third-party collection is resolved — paid in full, written off, or recalled — the platform sends a recall instruction to the third-party collection provider and lifts the placement flag. The reconciliation between internal records and third-party collection records is tracked in the third-party management module.

### Requirements Coverage

| DWP Demo Criteria | How This Flow Evidences It |
|---|---|
| Demo 2.17–18 — Tolerances and plan monitoring | Grace period rules and automated payment monitoring |
| Demo 2.14 — Treatments triggered by events | Breach event triggers automatic workflow |
| Demo 1.1 — Third-party integration (third-party collection placement) | Structured data transmission to third-party collection via API or file |
| Demo 2.9 — Communications with full audit trail | Sequenced breach communications logged end-to-end |
| Demo 2.11 — Champion/challenger (strategy testing) | Breach strategy parameters are configurable and testable |
| Demo 2.24 — User actions restricted by workflow | Supervisor override captured; auto-placement rules enforced |

---

## Flow 4: Complex Household — Multiple Accounts, Joint Liability, Competing Workflows

### Theme
**No matter how complicated the customer relationship, the platform holds it together — and protects against compliance mistakes in real time.**

### Context and Framing

This flow is one of the most DWP-specific demonstrations in the entire day. The household concept — multiple people linked by address or relationship, potentially carrying joint debts or individually-owned debts that affect each other's treatment — is a distinguishing feature of DWP's debt landscape compared to typical commercial debt management.

The risk this flow must address is clear: if two agents work related accounts in the same household without awareness of each other's actions, the customer may receive contradictory treatment, restricted accounts may be contacted in error, and the organisation may breach its own compliance obligations. This flow shows that the platform prevents those mistakes by maintaining a coherent cross-account view and enforcing restrictions at runtime.

### Pre-Conditions

- A household has been configured containing at least two individuals.
- One individual has two debts: one on an active repayment plan, one in legal treatment.
- The second individual has one debt with no current treatment, but is linked to the first individual through the household record.
- A breathing space restriction has been applied to the household or to the second individual — restricting all contact.
- Two agent personas are available, both attempting to work accounts in this household.

### Detailed Journey

#### Step 1 — Customer Search and Household View

An agent searches for a customer. The search returns the individual record. From the individual record, the agent navigates to the household view.

**What to show:** The household view. Show the household entity: the two individuals linked together, the three debts across the household (two for Person A, one for Person B), and the status icons on each. Person A's first debt shows "Active Plan." Person A's second debt shows "Legal." Person B's debt shows a red restriction indicator.

**What's happening in the platform:** The household entity is a first-class data structure in the platform. It is not derived from a shared address — it is explicitly modelled. Each individual is a member of the household, and each debt is linked to its owner within the household structure.

**Presenter note:** "DWP's requirement for a household concept is explicit in the tender. This is not a workaround or a reporting view. It is a data model."

---

#### Step 2 — Navigating the Joint Liability Detail

Navigate to the joint debt if one has been configured — a debt jointly owed by both individuals in the household. Show the joint debt view: both individuals are listed as debtors, both names appear on correspondence, and correspondence can be addressed to either or both.

**What to show:** The joint debt entity showing both parties. Show that a communication to both parties simultaneously is possible from this view — with a single trigger generating two correctly addressed communications.

**What's happening in the platform:** Joint debt is a configurable relationship type. The debt entity has a multi-party liability model. Each communication generated for a joint debt creates separate, correctly addressed copies for each party.

---

#### Step 3 — Attempting a Restricted Action

Now demonstrate the compliance protection at the heart of this flow.

The agent identifies Person B's debt and attempts to make an outbound contact — initiating a call task or generating a letter.

**What to show:** The agent opens Person B's debt and triggers the action. **The platform raises an alert.** A warning appears: "This account is subject to a breathing space restriction. Outbound contact is not permitted. No action has been taken." The action has been blocked. It has not been partially sent. It has not been quietly suppressed without the agent knowing. The agent is explicitly told what happened and why.

**What's happening in the platform:** Every action in the workflow layer checks active restrictions before executing. Restrictions are evaluated at runtime — not pre-calculated and cached. This means a restriction applied five minutes ago will immediately block the very next attempted action.

**Presenter note:** Pause here. "The agent attempted contact. The platform stopped it. In a system without this capability, that contact would have been sent. DWP would have breached a breathing space restriction. The customer could complain. A regulator could investigate. Here, the platform is the last line of defence."

---

#### Step 4 — Proceeding on a Valid Account

Now show the agent working one of Person A's debts — the active repayment plan. This account has no active restrictions (the breathing space is on Person B's account). The agent makes a contact attempt: it succeeds. The action is recorded against the correct account.

**What to show:** The successful action and the resulting event log entry — showing the account reference, the agent, the action, and the timestamp. Show clearly that the action is recorded only against this one account. Person B's account shows no new events. The restricted account has not been affected.

---

#### Step 5 — Parallel Workflows in the Same Household

Show that Person A's two debts are running separate, parallel workflows simultaneously. The repayment plan is being actively monitored by its own monitoring process. The legal treatment is in a separate, specialist workflow.

**What to show:** Navigate between the two debts. Each shows its own event timeline, its own upcoming actions, its own communication history. The two workflows do not interfere with each other.

**What's happening in the platform:** The BPMN process engine runs separate process instances per account. Each instance has its own state and its own timeline. Cross-account awareness is enforced through restriction checks — not by coupling the workflows together.

---

#### Step 6 — Supervisor View of the Household

Open the supervisor or team leader view. Show that the household is visible as a unit — the supervisor can see all accounts, their statuses, and any active alerts across the household at a glance.

**What to show:** The household summary in the supervisor dashboard. Highlight the breathing space restriction indicator, the active plan on Account 1, and the legal flag on Account 2. A supervisor can identify in seconds whether this household is being handled correctly.

### Moment of Truth

**The platform prevents a compliance mistake in real time.** The agent attempted to contact a breathing-space-restricted customer. The platform stopped the action before it happened, explained why, and left no trace of a contact attempt in the outbound systems. Frame it: "Without this capability, the only safeguard is the agent's memory. That is not good enough."

### Evaluator Q&A Preparation

**"Can the household model support more than two individuals?"**
Yes. The household entity is a many-to-many structure. Any number of individuals can be members. Complex multi-generational or multi-occupancy scenarios can be represented.

**"What happens when the breathing space restriction expires?"**
The restriction has an end date. When that date passes, the restriction is automatically lifted. Outbound contact is permitted again without any manual step. The lift event is logged.

**"Can a restriction on one household member affect all members?"**
This is configurable. Some restriction types are individual. Others can be applied at household level, automatically restricting all members. The mapping between restriction type and scope is configurable in the policy rules.

**"How does the platform handle a joint debt split?"**
A joint debt can be split into two individual debts — either manually by an authorised user or automatically by a configured rule. After the split, each individual debt follows its own treatment path. The residual rounding amount (1p, in the DWP requirement) is written off automatically without requiring manual intervention.

### Requirements Coverage

| DWP Demo Criteria | How This Flow Evidences It |
|---|---|
| Demo 3.1–4 — Household creation and query | Household entity created and displayed; queries for individual and joint debts |
| Demo 3.5–6 — Joint debt split and residual write-off | Split process shown; automatic residual handling |
| Demo 2.1 — Customer and account level view with multiple debts | Household view across all accounts |
| Demo 2.13 — Communications to multiple parties simultaneously | Joint debt communication to both parties |
| Demo 2.20 — Access separated by entity | Restriction enforcement per account |
| Demo 2.24 — User actions restricted by workflow | Blocked action when breathing space is active |

---

## Flow 5: Business User Changes a Collection Strategy — No IT Required

### Theme
**Business users own strategy change. The platform governs it safely without requiring IT involvement.**

### Context and Framing

This flow is the most commercially differentiating demonstration in the day. It speaks directly to DWP's stated requirement for no-code and low-code configuration — the ability for the collections operations team to modify, test, and deploy strategy changes without writing code, without submitting IT change requests, and without waiting for a release cycle.

The stakes are high in both directions. If the platform can credibly deliver this capability, it reduces DWP's total cost of operation, accelerates improvement cycles, and gives the business genuine control over its collections model. If the claim is weak or the interface too technical for a business user to navigate, the evaluator will discount it.

This flow must feel like a business operation, not a developer exercise. Every step should be performed in a business-facing interface by a persona that is recognisably a strategy manager — not a developer or configuration engineer.

### Pre-Conditions

- An existing collection strategy is deployed and in active use — at least one segment of accounts is being worked by this strategy.
- A strategy simulation data set is available — ideally a subset of historical accounts that were processed by this strategy in a recent period.
- A second user (reviewer/approver) persona is available.
- A champion/challenger infrastructure is enabled in the platform.

### Detailed Journey

#### Step 1 — Opening the Strategy in the Visual Editor

The strategy manager opens the strategy management module. They navigate to the early-stage delinquency strategy.

**What to show:** The strategy is displayed as a visual, navigable workflow or decision tree — not as raw configuration data. The manager can see the sequence of communication steps, the timing rules between them, the conditions that move an account forward or sideways, and the end states.

**What's happening in the platform:** The strategy is stored as structured configuration data and rendered visually by the strategy editor. The manager is editing configuration, not code.

---

#### Step 2 — Adding a New SMS Step

The manager decides to add a new SMS contact step between the first letter and the first outbound call attempt. Their hypothesis is that an early SMS — arriving two days after the letter — will prompt a proportion of customers to self-serve before an agent needs to call.

**What to show:** The manager clicks to add a new step at the chosen point in the strategy. They select the step type: SMS. They select the template from the third-party communications platform's template library — visible and browsable from within the strategy editor. They configure the timing: send two days after the letter. They set the conditions: send only if no payment has been received and the account is not subject to any contact suppression.

**What's happening in the platform:** The step is added to the strategy's process definition as a new node. The SMS template reference is stored against the step. The timing and conditions are business-readable rules — not code expressions.

**Presenter note:** "The manager has not opened a code editor. They have not submitted a change request. They have not asked IT to update a routing table. They have added a communications step to a live collection strategy by clicking through a form."

---

#### Step 3 — Saving as Draft (Not Publishing)

The manager saves the change as a new draft version of the strategy. The existing strategy continues to run for current accounts without interruption.

**What to show:** The strategy now shows two versions: the live version (Version 2.1, deployed, running) and the draft version (Version 2.2, in review). A version comparison is available — the manager can see exactly what changed between the two versions in a readable diff view.

**What's happening in the platform:** Version control is built into the strategy layer. Every change creates a new version. Publishing and rollback are explicit user actions with their own permission requirements.

---

#### Step 4 — Peer Review and Approval

The manager submits the draft for peer review. A notification is sent to the designated reviewer.

**What to show:** Switch to the reviewer persona. The reviewer receives the notification and opens the review request. They can see the strategy diff — the new SMS step is highlighted. They can add review comments and either approve or reject.

**What's happening in the platform:** The review workflow is a configurable approval process. The number of reviewers required, the escalation path if a reviewer is unavailable, and the permission roles involved are all configurable.

---

#### Step 5 — Historical Simulation

Before approval, the manager runs a simulation: "What would have happened if this version of the strategy had been live for the last three months?"

**What to show:** The simulation interface. The manager selects a historical date range and a subset of accounts. The platform replays those accounts through the proposed strategy instead of the live strategy. A results comparison appears: with the new SMS step, projected contact rate at day 3 increases by X%. Promise-to-pay rate at day 5 is Y% higher. Predicted call volume is Z% lower because more customers self-serve.

**What's happening in the platform:** The simulation engine runs historical account states through the proposed strategy in a sandboxed environment. It does not affect live accounts or live operational data. The results are projections based on historical behaviour, clearly labelled as modelled outcomes.

**Presenter note:** "The question the evaluator should have in their mind after this step is: 'When did I last see a vendor show me this?' Most platforms show you a flowchart editor. This platform shows you the evidence for why the change should be made. That is a different proposition entirely."

---

#### Step 6 — Champion/Challenger Configuration

The manager decides not to fully deploy the new strategy yet. Instead, they configure a champion/challenger split: 80% of new early-stage accounts will continue on the existing strategy (champion). 20% will be routed to the new strategy (challenger).

**What to show:** The champion/challenger configuration screen. The manager sets the split ratio, the start date, and the evaluation period (30 days). The platform shows a preview of how accounts will be allocated — which segment, which proportion.

**What's happening in the platform:** The routing layer applies the configured split to new accounts as they are assigned to this strategy. Historical accounts already in the strategy are not affected. The split is random and reproducible — the platform can retrospectively identify which path each account was on.

---

#### Step 7 — Monitoring Comparative Results

After a defined period, the manager returns to review the champion/challenger results.

**What to show:** The comparative results dashboard. Side by side: champion performance vs challenger performance. Contact rate, promise-to-pay rate, call volume, arrangement creation rate, breach rate. The challenger is performing better. The manager promotes it to full use.

**What's happening in the platform:** The results are calculated from real operational data — the actual outcomes of accounts routed through each path. This is not a simulation at this point. It is live evidence.

---

#### Step 8 — Promotion and Version History

The manager promotes the challenger strategy to full deployment. The previous version is archived. The version history shows the complete lineage: original strategy, draft with SMS step, approval event, champion/challenger test period, promotion to full use.

**What to show:** The version history timeline. Show the rollback button. "If we needed to revert to the previous version at any point, one click would do it. The previous strategy definition is preserved exactly."

### Moment of Truth

**The evaluator sees the platform answer the question: "What would have happened if this strategy had been live already?"** That simulation step is the reveal. It transforms the strategy editor from a workflow design tool into a strategy optimisation tool. Frame the contrast: most platforms let you draw a flowchart. This platform lets you test it against real data before you use it.

### Evaluator Q&A Preparation

**"How do we prevent an inexperienced user from publishing a broken strategy?"**
The publishing step requires explicit approval. The approval workflow can be configured to require sign-off from a senior user. The draft state is a safety gate. Additionally, validation rules can prevent logically inconsistent strategy configurations from being saved.

**"Can strategies be tested in a UAT environment before live deployment?"**
Yes. Strategies can be deployed to non-production environments. The environment promotion path is configurable and version-controlled.

**"What if the champion/challenger results are inconclusive?"**
The evaluation period can be extended. The split ratio can be adjusted. The test can be stopped and the challenger archived without any customer impact — no live accounts are affected by the configuration change.

### Requirements Coverage

| DWP Demo Criteria | How This Flow Evidences It |
|---|---|
| Demo 2.5 — Strategy design, test, and adaptation | Visual editor, simulation, champion/challenger |
| Demo 2.6 — Workflows at user level with minimal technical knowledge | Business user persona throughout; no code visible |
| Demo 2.7 — Create and amend strategies | New step added; version created and deployed |
| Demo 2.11 — Champion/challenger strategies | Explicitly configured and monitored |
| Demo 2.8 — Communications in strategies (SMS template) | SMS template selected from third-party platform |
| Demo 4.1 — Parallel development and version control | Strategy versioning, approval, and rollback shown |

---

## Flow 6: Executive and Operational Dashboard — Real-Time Oversight

### Theme
**Senior leadership sees the operation clearly, acts when needed, and never needs an analyst to prepare a report first.**

### Context and Framing

This flow serves a different audience than the preceding five. Flows 1 through 5 demonstrate operational capabilities — intake, vulnerability, breach, household management, and strategy control. Flow 6 demonstrates that all of that activity is visible, governable, and manageable from a senior leadership perspective.

DWP is not only buying an operational platform for case workers. It is buying a management and governance platform for operations directors, compliance managers, and senior stakeholders who need to know that the operation is running correctly, that risks are visible, and that problems can be identified and addressed without waiting for periodic reporting.

This flow should be presented last among the primary flows because it works best when the evaluator has already seen the operational activity that the dashboard is summarising. When the breach data, vulnerability cases, and strategy performance visible on the dashboard connect back to what the evaluator has already seen earlier in the day, the dashboard becomes far more compelling than if it were shown in isolation.

### Pre-Conditions

- The platform has a meaningful volume of data reflecting the scenarios shown in earlier flows: intake accounts, vulnerability cases, breached arrangements, third-party collection placements, strategy champion/challenger results.
- A dashboard has been configured for an operations director persona with role-appropriate views.
- Exception and SLA reporting is enabled.

### Detailed Journey

#### Step 1 — Opening the Top-Level Dashboard

The operations director opens their personalised operational dashboard.

**What to show:** A single-screen operational summary. Queue volumes by team and product type. Active arrangement count and health — green/amber/red. Today's contact rate across channels. Breach count for the current period — total and segmented. Active third-party collection placements and balance with agencies. Active vulnerability cases and specialist queue depth.

**What's happening in the platform:** The dashboard data is live — reflecting the current operational state, not a cached or batch-calculated snapshot. Each tile is a real-time query against the operational data.

**Presenter note:** "This is not a Business Intelligence report. This is not a Power BI extract run at midnight. This is the live operational state of the collections operation right now. The operations director can see this at any time of day, from any device, without requesting anything from an analyst."

---

#### Step 2 — Drilling into an Emerging Trend

The operations director notices that the breach count for the Personal Loan segment has risen sharply in the last 48 hours — the tile is amber. They click on it.

**What to show:** The drill-down view for that segment. Account count, breach rate versus prior period, average days since breach, communication response rates. A chart showing breach count by day over the last 30 days — with a clear upward inflection in the last two days.

**Presenter note:** Make the narrative sharp: "Something changed in the last 48 hours for this segment. The director needs to know whether this is a data quality issue, a seasonal pattern, a strategy problem, or a capacity problem."

---

#### Step 3 — Tracing the Trend to its Cause

The director drills further — this time into the strategy dimension. Which treatment strategy is most of this segment on? What is the performance of that strategy compared to its recent baseline?

**What to show:** The strategy performance view for the segment. Show that the strategy's contact rate has dropped — fewer customers are responding to communications in this segment over the last 48 hours. Compare to the prior 30-day baseline.

Navigate to the strategy detail. Show the most recent change event: two days ago, a strategy amendment was approved and deployed. A new letter template was activated. The drop in contact rate correlates with that timing.

**What's happening in the platform:** The audit trail of strategy changes is accessible from the dashboard. The director can directly trace a performance anomaly back to a specific configuration change without needing to interview the strategy manager.

---

#### Step 4 — Queue Ageing and Capacity Risk

Navigate to the queue management view. Show queue ageing — accounts that have been in a queue without being worked for longer than the SLA target.

**What to show:** A heatmap or list view of queues by ageing. One queue — high-value accounts awaiting specialist review — shows 23 accounts more than 48 hours over SLA. Flag this as a risk.

**What's happening in the platform:** The SLA configuration defines the target working time for each queue. Accounts that breach the SLA threshold automatically escalate in priority and appear in exception views.

---

#### Step 5 — Triggering a Supervisory Intervention

The director identifies the capacity issue and acts. They open the reassignment tool.

**What to show:** The director selects 15 of the ageing accounts and bulk-reassigns them to a different team with available capacity. The reassignment is logged. The receiving team sees the new items in their queue immediately.

**What's happening in the platform:** Bulk reassignment is a permissioned management action. The action is logged against the director's user ID with a timestamp and a reason code. The accounts' assignment history is preserved.

---

#### Step 6 — Champion/Challenger Strategy Reporting

Navigate to the strategy performance module and open the champion/challenger results from Flow 5 — the SMS-step test.

**What to show:** The comparative performance view. Contact rate, promise-to-pay rate, arrangement creation rate, breach rate — side by side for champion and challenger accounts. The challenger is clearly outperforming on contact rate. Show the statistical significance indicator — the platform confirms whether the sample size is sufficient to draw a confident conclusion.

---

#### Step 7 — Vulnerable Case Exception Report

Open the exception report for vulnerable cases and specialist queue management.

**What to show:** Cases where a vulnerability flag has been active for more than a configured period without a specialist review. Cases where the specialist queue has breached its SLA. Any cases where a vulnerability record is approaching its temporary expiry date and needs review.

**What's happening in the platform:** Vulnerability management is not a set-and-forget operation. The platform actively monitors the age and status of vulnerability flags and generates exception alerts when review is overdue.

---

#### Step 8 — Management and Governance Report

The director exports a governance-ready report for a board pack or regulatory submission.

**What to show:** The report export function. The director selects the report type (operational summary, vulnerability management, SLA performance, strategy effectiveness), the period, and the format. A report is generated — PDF or structured data. Show the output: it is structured, branded, and contains the key metrics with time-series comparisons.

**What's happening in the platform:** Report templates are configurable. DWP can define the metrics, layout, and narrative structure of standard reports. Reports pull from the same live data as the dashboard — they are not separate data exports.

### Moment of Truth

**A senior user identifies a problem, traces its cause to a specific strategy change made 48 hours ago, and takes corrective action — all inside the same platform, in under five minutes, without asking anyone for data.** Frame it explicitly: "In a traditional environment, this sequence would require an email to the analytics team, a query run against the data warehouse, a spreadsheet produced and emailed back, and perhaps a meeting to discuss findings. Here, the operations director sees the problem, understands the cause, and acts — before the next SLA breach occurs."

### Evaluator Q&A Preparation

**"Does this integrate with Power BI?"**
Yes. The platform can expose its operational data through an API or data feed that Power BI can consume. DWP's strategic MI solution can display any platform metric in Power BI dashboards. The platform's own operational dashboard is complementary — providing real-time operational views — while Power BI serves strategic and cross-system reporting needs.

**"Who can see what on the dashboard?"**
Dashboard access is controlled by the role-based access model. An agent sees their own queue. A team leader sees their team. An operations director sees the full operation. The same data layer serves all views — access is filtered by role, not maintained as separate data sets.

**"Can we configure our own KPIs?"**
Yes. The reporting framework allows DWP to define custom KPIs — targets, thresholds, and comparison baselines — and present them on the dashboard. Standard KPIs (contact rate, arrangement health, breach rate, SLA compliance) are available out of the box.

### Requirements Coverage

| DWP Demo Criteria | How This Flow Evidences It |
|---|---|
| Demo 2.25 — System configured for KPIs and SLAs; users generate reporting | Live dashboard with configurable KPIs; queue ageing reports |
| Demo 2.19 — Work allocation and prioritisation | Bulk reassignment from supervisor view |
| Demo 2.6 — Workflows at user/team level | Operations director taking management actions directly |
| Demo 2.11 — Champion/challenger reporting | Strategy comparison accessible from dashboard |
| Demo 2.23 — All user actions audited | Director's reassignment action logged |
| Demo 3.14 — Vulnerable cases management view | Vulnerability exception report shown |

---

## Summary: Coverage Across All Six Flows

| Requirement Area | Flow 1 | Flow 2 | Flow 3 | Flow 4 | Flow 5 | Flow 6 |
|---|---|---|---|---|---|---|
| Data Integration and Intake | ✓✓ | | | | | |
| Workflow Automation and Override | ✓ | ✓ | ✓✓ | ✓ | ✓ | ✓ |
| Multi-Channel Communications | ✓ | ✓ | ✓✓ | ✓ | ✓ | |
| Repayment Arrangement Lifecycle | | ✓✓ | ✓ | | | |
| Affordability and I&E | | ✓✓ | | | | |
| RBAC and Audit | | ✓ | ✓ | ✓ | ✓ | ✓ |
| Work Queue Distribution | ✓✓ | ✓ | | | | ✓ |
| Decisioning and Strategy Optimisation | ✓ | | | | ✓✓ | ✓ |
| Third-Party Placement and Reconciliation | | | ✓✓ | | | |
| Financial Accounting and Reconciliation | | | ✓ | | | |
| Exception Detection and Escalation | ✓ | ✓✓ | ✓ | ✓ | | ✓ |
| Agent Action Logging and History | ✓ | ✓✓ | ✓ | ✓ | | ✓ |
| Vulnerability and Compliance | | ✓✓ | | ✓✓ | | ✓ |
| Household and Joint Liability | | | | ✓✓ | | |
| Parallel Process Management | | | ✓ | ✓✓ | | |
| Strategy Change Without IT | | | | | ✓✓ | |
| Champion/Challenger Testing | | | | | ✓✓ | ✓ |
| Operational Reporting and MI | ✓ | | | | | ✓✓ |

*✓✓ = primary evidence point for this requirement; ✓ = supporting evidence*

---

## Presenter Checklist Before the Day

- [ ] All demo data is loaded and realistic — names, amounts, dates, and statuses are consistent
- [ ] At least two browser sessions are open: one for agent persona, one for supervisor/director persona
- [ ] The strategy simulation data set is pre-loaded and the simulation can run within 30 seconds
- [ ] third-party collection configuration is verified — the placement transmission can be shown without network error
- [ ] The communications platform integration is live and a test letter or SMS can be queued
- [ ] All vulnerability types, queue names, and segment codes reflect DWP-appropriate terminology
- [ ] Version history in the strategy editor shows at least two prior versions for credibility
- [ ] The champion/challenger split has been running long enough to show meaningful comparative data
- [ ] All user personas have appropriate permissions — no unexpected access denied errors during demo
- [ ] A backup script or recorded walkthrough is available in case of technical failure
