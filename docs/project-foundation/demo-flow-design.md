# DWP Debt Collection — Demo Flow Design

## Demo Flow 1: From Account Intake to First Meaningful Contact

**Theme:** The system knows what to do before an agent touches it.

**Scenario:** A batch of new delinquent accounts arrives from the host system overnight. By the time agents log in the next morning, each account has been profiled, scored, segmented into a treatment strategy, and a first contact attempt is already scheduled or underway.

**Journey Steps:**

1. Overnight bulk data load ingests 500 new accounts via API from the host system
2. System validates and enriches each account — pulling CRA/bureau data, matching to existing customer records, detecting joint account relationships
3. Decisioning engine segments accounts into treatment strategies based on risk score, balance band, product type, and delinquency stage — automatically
4. Work queues populate: high-risk accounts assigned to specialist agents; standard accounts to general queue; self-cure candidates suppressed for 48 hours
5. Automated first communication triggered per strategy — SMS for mobile-first customers, letter for others
6. Agent logs in at 08:00 to a pre-prioritised worklist; opens first account to find full profile, CRA data, strategy assignment, and contact history already populated

**Moment of Truth:** The agent has done nothing — yet the system has already profiled, segmented, communicated with, and prioritised 500 accounts. Show this via the supervisor's real-time dashboard.

**Behavioral Requirements Showcased:**

| # | Requirement |
|---|---|
| 2 | Data Capture & Integration Pipeline |
| 3 | Workflow Automation & Override |
| 5 | Multi-Channel Communication |
| 8 | Work Queue Distribution |
| 9 | Decisioning & Strategy Optimisation |
| 13 | Agent Action Logging & History |

---

## Demo Flow 2: Customer in Financial Difficulty — Vulnerability to Resolution

**Theme:** The system protects the right customers while still driving recovery.

**Scenario:** An agent takes an inbound call from a customer who is struggling. The customer mentions they are dealing with mental health issues and can't afford their current arrangement. This single conversation triggers a chain of automated compliance responses, specialist routing, an affordability-driven new plan, and a full audit trail — without the agent having to know the rules.

**Journey Steps:**

1. Agent opens account — system triggers ID&V prompt and screen pop with account summary and existing arrangement status (currently in breach)
2. Customer discloses mental health difficulty — agent records vulnerability flag; system immediately: suppresses all automated outbound contact, removes account from dialler queue, flags for specialist handling, and logs a timestamped vulnerability event
3. Agent transfers to vulnerability specialist — case history, notes, and flags follow seamlessly with the transfer
4. Specialist opens I&E capture screen — walks customer through income and expenditure; system validates against CRA data and flags discrepancies for discussion
5. Affordability model calculates sustainable repayment range — system presents 3 plan options (weekly/fortnightly/monthly) within that range
6. Customer agrees to a reduced plan — system creates arrangement, schedules payment monitoring, generates confirmation letter/SMS, and sets a 30-day review trigger
7. Audit log shows every action, timestamp, and agent ID across the entire journey

**Moment of Truth:** Show the before/after on the communications dashboard — all automated contact stops the moment the vulnerability flag is set. Then show the arrangement being created in under 2 minutes from I&E completion.

**Behavioral Requirements Showcased:**

| # | Requirement |
|---|---|
| 4 | Repayment Arrangement Lifecycle |
| 6 | I&E Affordability Assessment |
| 7 | RBAC & Audit |
| 12 | Exception Detection & Escalation |
| 13 | Agent Action Logging & History |
| 15 | Dynamic UI & Agent Scripting |
| 18 | Vulnerability & Compliance |

---

## Demo Flow 3: Arrangement Breach → Escalation → DCA Placement

**Theme:** The system escalates without hesitation — and with a full paper trail.

**Scenario:** A customer who had a repayment arrangement misses two consecutive payments. The system detects the breach, attempts re-engagement automatically, exhausts internal treatment options, and escalates the account to a third-party debt collection agency — all within a configurable ruleset, with no manual intervention required unless an agent chooses to override.

**Journey Steps:**

1. Payment due date passes — system detects missed payment within the hour, checks tolerance rules (is this within agreed flexibility?), confirms breach
2. Automated breach workflow fires: SMS sent day 1, letter generated day 3, outbound dialler attempt day 5
3. Customer does not respond to any contact across 10 days — system logs all failed attempts, escalates to "pre-placement" strategy
4. Agent is alerted via exception queue — reviews account, sees full breach history and prior I&E data, decides not to override
5. Account meets all DCA placement rules — system triggers automated placement to pre-configured agency, sends encrypted account data file via FTPS, logs placement event
6. DCA acknowledges receipt — system records handoff, suspends internal contact (no parallel contact), sets performance monitoring trigger
7. DCA collects partial payment — reconciliation report flows back, system allocates payment, updates balance, updates DCA commission ledger
8. Supervisor runs DCA performance report — shows placement outcomes, recovery rates, time-to-collect across all active placements

**Moment of Truth:** Show the full timeline from missed payment to DCA placement — elapsed time, every automated action taken, every decision point — without a single agent having to intervene.

**Behavioral Requirements Showcased:**

| # | Requirement |
|---|---|
| 3 | Workflow Automation & Override |
| 4 | Repayment Arrangement Lifecycle |
| 5 | Multi-Channel Communication |
| 10 | 3rd Party Placement & Reconciliation |
| 11 | Financial Accounting & Reconciliation |
| 12 | Exception Detection & Escalation |
| 20 | Parallel Process Management |

---

## Demo Flow 4: Complex Household — Multiple Accounts, Joint Liability, Competing Workflows

**Theme:** No matter how complex the customer, the system holds it together.

**Scenario:** A customer holds three accounts with DWP: one personal loan in active repayment, one benefit overpayment in legal proceedings, and one joint account with a partner who is separately in a breathing space arrangement. The system manages all three simultaneously without conflicting contact, without duplicate escalation, and with a single unified customer view.

**Journey Steps:**

1. Agent searches by customer name — system returns unified household view: 3 accounts, 2 individuals, 3 active workflows in different stages
2. Personal loan account: on a repayment plan, next payment due in 4 days, no action needed — shown as green
3. Benefit overpayment: in legal proceedings — system has automatically suppressed all collection contact for this account, legal team has a separate view
4. Joint account: partner has a breathing space order active — system has flagged the joint account with a contact restriction that applies to both parties
5. Agent attempts to log a manual call to the customer — system prompts: "breathing space restriction active on joint account — confirm this call relates only to personal loan account [REF]"
6. Agent proceeds — call outcome logged against personal loan account only, joint account untouched
7. Strategy engine shows all three workflows running in parallel — zero conflicts, zero duplicate letters generated

**Moment of Truth:** Show the system preventing the agent from making a compliance error in real time — and demonstrate that three separate automated workflows are running simultaneously without interfering with each other.

**Behavioral Requirements Showcased:**

| # | Requirement |
|---|---|
| 1 | Relationship Management |
| 3 | Workflow Automation & Override |
| 5 | Multi-Channel Communication |
| 7 | RBAC & Audit |
| 12 | Exception Detection & Escalation |
| 18 | Vulnerability & Compliance |
| 20 | Parallel Process Management |

---

## Demo Flow 5: Business User Changes a Collection Strategy — No IT Required

**Theme:** Business users own their own strategy. IT is not in the loop.

**Scenario:** A collections strategy manager wants to test whether adding an SMS contact attempt on day 2 of a new delinquency cycle improves early cure rates. She designs the change, tests it against historical data, runs a champion/challenger split, and promotes it to production — all within the system, all without raising a change request with IT.

**Journey Steps:**

1. Strategy manager opens the visual workflow designer — drag-and-drop canvas showing the current "Early Delinquency — Standard" strategy as a flowchart
2. She adds a new SMS node at day 2, configures the message template, sets a suppression rule (skip if mobile number unverified), and saves as a draft
3. Peer review workflow fires — second manager receives a notification, reviews the change, approves it
4. Manager runs a simulation: "If this strategy had been live for the last 90 days, what would have happened?" — system replays historical account journeys through the new strategy and shows projected outcome difference
5. She sets up a champion/challenger split: 20% of new accounts go to the new strategy, 80% stay on the current one
6. After 30 days, MI report shows the challenger strategy has a 4% higher early cure rate — manager promotes it to 100%
7. Rollback button remains available; full version history of every strategy change is visible and auditable

**Moment of Truth:** Show the strategy simulation — the system telling the manager what would have happened if this strategy had been live. This is the single most commercially differentiating feature in the requirements.

**Behavioral Requirements Showcased:**

| # | Requirement |
|---|---|
| 3 | Workflow Automation & Override |
| 9 | Decisioning & Strategy Optimisation |
| 16 | Controlled Change Management |
| 19 | Roadmap & Upgrades |

---

## Demo Flow 6: Executive & Operational Dashboard — Real-Time Oversight

**Theme:** Leadership sees the operation in real time. Nothing is hidden.

**Scenario:** A collections operations director opens their dashboard at 09:30. They want to understand today's queue health, whether SLAs are being met, which agents are underperforming, and whether last week's strategy change is showing results — all without asking anyone.

**Journey Steps:**

1. Director opens dashboard — top-level KPIs: accounts in queue, contact rate today, cure rate this week, arrangements active, arrangements in breach, DCA placements live
2. Drills into breach trend — sees a spike in breaches for a specific product type over the last 5 days; clicks to see which strategy those accounts are on
3. Drills into work queue — sees two agents with 40+ accounts sitting untouched for over 2 hours; sends a supervisor alert directly from the dashboard
4. Checks champion/challenger performance report — new SMS strategy (from Demo 5) showing early positive results, projected annualised recovery uplift displayed
5. Opens exception report — 12 accounts flagged as vulnerable but not yet assigned to vulnerability specialist queue; clicks to bulk-reassign
6. Exports monthly service performance report in one click — covers availability, response times, SLA compliance, data integrity — ready for submission to DWP governance

**Moment of Truth:** Show the director identifying a problem (breach spike), tracing it to a root cause (strategy), and taking corrective action (reassignment) — entirely within the system, in under 3 minutes.

**Behavioral Requirements Showcased:**

| # | Requirement |
|---|---|
| 3 | Workflow Automation & Override |
| 7 | RBAC & Audit |
| 8 | Work Queue Distribution |
| 9 | Decisioning & Strategy Optimisation |
| 11 | Financial Accounting & Reconciliation |
| 12 | Exception Detection & Escalation |
| 13 | Agent Action Logging & History |

---

## Demo Flow 7: Customer Self-Service — Portal to Agent Handoff

**Current baseline status:** Partially in baseline. The customer-facing portal/app UI is external to DCMS and remains deferred unless scope is reopened, but the DCMS backend self-service integration boundary is now in scope via `self-service-integration-design.md`.

**Theme:** The customer does the work. The system connects it seamlessly.

**Scenario:** A customer receives an SMS with a link to DWP's self-service portal. Without speaking to anyone, they update their contact details, complete an I&E form, and set up a repayment arrangement. The agent sees everything in real time — and only needs to intervene when the customer requests a call.

**Journey Steps:**

1. Customer clicks SMS link, authenticates via portal (OAuth 2.0 / GOV.UK Verify)
2. Portal presents account summary — balance, current status, options available
3. Customer completes I&E digitally — system pre-populates known data (employment status, benefits) from DWP records
4. System calculates affordable repayment range, presents three arrangement options
5. Customer selects fortnightly plan — confirmation sent instantly via SMS and email
6. Agent's queue updates in real time — account moves from "awaiting contact" to "arrangement agreed — monitoring"
7. Agent opens account: sees the complete self-service journey timestamped, I&E captured, arrangement live — nothing to action

**Moment of Truth:** The DWP strategic portal/app completes the customer-facing journey, while DCMS accepts the resulting contact update, I&E, arrangement/payment actions, and audit events through integration. Show the agent's queue and timeline — the account is either resolved without manual re-keying or routed only where policy gates require staff intervention.

**Gaps filled:** #14 Self-Service & API Integration

---

## Demo Flow 8: Debt Dispute — Freeze, Investigate, Resolve

**Theme:** When a customer disputes, the system protects everyone.

**Scenario:** A customer calls claiming they don't owe the debt — they believe it was paid off two years ago. The agent raises a dispute. Immediately, all collection activity freezes, an investigation workflow opens for the back-office team, and the customer receives written acknowledgement — all within seconds of the flag being set.

**Journey Steps:**

1. Agent takes inbound call — customer disputes liability; agent selects "Dispute" action from disposition menu
2. System immediately: pauses all automated workflows, removes account from all diallers and letter queues, generates written acknowledgement to customer (GDPR-compliant, within 2-hour SLA)
3. Investigation task auto-assigned to back-office team with 10-working-day SLA — supervisor can see it in the exceptions dashboard
4. Back-office team accesses full transaction history, payment records, host system data — all within the system
5. Investigation concludes: debt is valid but balance incorrect — system corrects balance, logs amendment with full audit trail, notifies agent
6. Agent calls customer with corrected position — dispute closed, account re-enters collection strategy at correct balance and delinquency stage
7. Entire dispute lifecycle — open to close — visible as a single auditable thread

**Moment of Truth:** Show the millisecond the dispute flag is set — every automated action against that account stops simultaneously. Then show the audit trail of the investigation, proving nothing happened to the account without authorisation.

**Behavioral Requirements:** #3 Workflow Automation, #7 RBAC & Audit, #11 Financial Accounting, #12 Exception Detection, #13 Agent Logging

---

## Demo Flow 9: Deceased Customer — Compliance Protocol in Motion

**Theme:** The most sensitive scenario, handled without a single misstep.

**Scenario:** A customer's family member calls to report a bereavement. From the moment the agent records the notification, the system enforces a strict protocol — contact ceases, the account transitions to an estate management workflow, probate correspondence is triggered, and no agent can accidentally send a collection letter.

**Journey Steps:**

1. Agent records "deceased" notification — date of death, name of caller, relationship
2. System immediately locks all outbound collection activity across every account held by the customer; flags all linked joint accounts for review
3. Pre-configured "Deceased" strategy activates — generates a bereavement acknowledgement letter to the estate address, sets a 30-day hold
4. Agent attempting any collection action on the account is blocked with an explanatory prompt — the system enforces the protocol, not the agent's memory
5. After 30 days, system prompts for probate status — agent records outcome; strategy branches based on whether estate has an executor
6. Probate correspondence generated with correct legal wording from approved template library
7. Supervisor report shows all deceased accounts, days since notification, probate status, and next required action

**Moment of Truth:** Show an agent trying to generate a standard collection letter on the deceased account — the system blocks it, explains why, and shows what action is permitted instead.

**Behavioral Requirements:** #3 Workflow Automation, #5 Multi-Channel Comms, #12 Exception Detection, #15 Dynamic UI & Scripting, #18 Vulnerability & Compliance

---

## Demo Flow 10: Write-Off, Recovery & Reactivation

**Theme:** Closing a case is never truly the end.

**Scenario:** An account reaches the end of all treatment paths — internal collection, DCA placement, and legal action have all been exhausted. The system routes it for write-off approval, which requires a senior manager sign-off. Six months later, the customer makes an unexpected payment — the system reactivates the account, reverses the write-off, and notifies the finance team automatically.

**Journey Steps:**

1. Account exhausts final strategy stage — system generates write-off recommendation with supporting data (treatment history, total recovery to date, balance remaining)
2. Write-off approval workflow opens — routed to team leader first, then senior manager based on balance threshold; each level has a configured authority limit
3. Senior manager approves in the authorisation queue — system books write-off transaction to GL, generates regulatory reporting entry, archives account
4. Six months later: customer makes an unexpected partial payment via bank transfer — payment file arrives from host system
5. System detects payment against a written-off account — automatically creates reactivation alert, routes to recoveries team
6. Recoveries agent reviews — reverses write-off, allocates payment, assesses whether remaining balance is economically viable to pursue
7. Full financial audit trail shows the write-off booking, reversal, and payment allocation as discrete, timestamped events

**Moment of Truth:** Show the unexpected payment arriving and the system automatically knowing it relates to a closed account — triggering the right workflow without any manual triage.

**Behavioral Requirements:** #3 Workflow Automation, #7 RBAC & Audit, #10 3rd Party Placement, #11 Financial Accounting, #12 Exception Detection

---

## Demo Flow 11: New Agent — From Login to First Resolved Case

**Theme:** The system makes a new agent effective from day one.

**Scenario:** A new collections agent joins DWP. It's their first week. They have no knowledge of collection regulations, vulnerability rules, or DWP-specific procedures. The demo shows how the system guides them through every interaction — prompting, scripting, blocking unsafe actions, and escalating when they hit something outside their authority.

**Journey Steps:**

1. Agent logs in — system shows personalised queue with accounts appropriate to their skill level and authority tier (junior agents get lower-complexity accounts)
2. Agent opens first account — system automatically runs ID&V script, prompting the agent line by line; mandatory fields must be completed before proceeding
3. Customer mentions they're on a zero-hours contract — system prompts agent to capture employment circumstances; flags affordability consideration
4. Agent attempts to set up a repayment plan above the maximum without an I&E — system blocks it: "I&E required before arrangements above £X per month"
5. Agent completes I&E with system guidance — arrangement created within approved parameters
6. Agent attempts to access supervisor-only function — system denies access, offers to escalate to supervisor
7. Supervisor watching via monitoring dashboard sees the new agent's session, can co-browse in real time, and sends a coaching note without disrupting the call

**Moment of Truth:** Show the system actively preventing the new agent from making a compliance error — not through training, but through design. The system is the training.

**Behavioral Requirements:** #7 RBAC & Audit, #8 Work Queue Distribution, #12 Exception Detection, #13 Agent Logging, #15 Dynamic UI & Scripting, #18 Vulnerability & Compliance

---

## Demo Flow 12: Regulatory Audit — Full Traceability on Demand

**Theme:** Any action, any account, any time. Explained in full.

**Scenario:** DWP's internal audit team receives a complaint from a customer claiming they were contacted during a period when they had a breathing space order in place. The audit team needs to reconstruct exactly what happened, who authorised it, and whether the system functioned correctly — within a single afternoon.

**Journey Steps:**

1. Auditor searches by customer reference — account opens showing full chronological event log: every action, every automated trigger, every agent interaction, with timestamps and user IDs
2. Auditor filters by date range — all events during the disputed period are shown; breathing space flag is visible with exact date/time it was set
3. Auditor sees a letter was generated after the breathing space flag was set — clicks through to the audit record: letter was triggered by an automated batch job 4 minutes before the flag was applied
4. System shows the exact rule version active at that time, the strategy step that triggered the letter, and the batch job ID
5. Auditor exports the complete event log as a structured report — all events, user IDs, IP addresses, session IDs, MAC addresses — ready for submission
6. System also shows that the issue was caused by a timing gap in the batch, not agent error — root cause identified without interviewing anyone

**Moment of Truth:** The entire investigation — from question to answer — takes under 10 minutes. Everything the auditor needs is already in the system, structured and exportable.

**Behavioral Requirements:** #7 RBAC & Audit, #12 Exception Detection, #13 Agent Logging, #18 Vulnerability & Compliance

---

## Demo Flow 13: Month-End Surge — Operations at Scale

**Theme:** The system doesn't blink when volumes spike.

**Scenario:** It's the first Monday of the month. Benefit payments have processed over the weekend, triggering a wave of promise-to-pay follow-ups, new delinquencies, and arrangement reviews simultaneously. Show the platform absorbing the surge — queues recalibrating, batch jobs completing, agents getting the right work — without a supervisor manually intervening.

**Journey Steps:**

1. Overnight batch runs: 2,400 accounts reviewed — payment received (close), payment missed (breach), new delinquency (strategy assign)
2. By 08:00, work queues have automatically recalibrated: breach accounts prioritised by balance and risk; cures archived; new accounts segmented
3. Supervisor opens operations dashboard — today's queue health, estimated throughput, agent utilisation, SLA risk indicators all visible
4. System identifies a queue imbalance: two agents are overloaded, three are underutilised — supervisor redistributes with a single bulk-reassign action
5. Automated comms fire throughout the morning — 800 SMS messages, 300 letters batched — system shows delivery status in real time
6. By midday, supervisor sees contact rate tracking above target — one strategy is underperforming; she A/B splits it in real time to test an alternative treatment
7. End-of-day report auto-generates: throughput, resolution rate, SLA compliance, comms sent — available for the director before close of business

**Moment of Truth:** Show the overnight batch results arriving and the queues self-organising — 2,400 accounts categorised and assigned before a single agent has logged in.

**Behavioral Requirements:** #2 Data Pipeline, #3 Workflow Automation, #5 Multi-Channel Comms, #8 Work Queue Distribution, #9 Decisioning & Strategy, #11 Financial Accounting

---

## Demo Flow 14: Lump Sum Settlement Offer — Agent-Negotiated, System-Governed

**Theme:** Every agent negotiates consistently. Authority limits are enforced automatically.

**Scenario:** A customer calls offering to settle their debt in full, but for less than the outstanding balance — a partial settlement. The agent needs to assess the offer, check whether it falls within their authority, escalate if it doesn't, get it approved, and close the account. The system governs the entire authority chain without the agent needing to know the policy.

**Journey Steps:**

1. Agent opens account — balance £4,200, customer offers £3,000 as full and final settlement
2. Agent opens settlement screen — system checks: is £3,000 within this agent's write-off authority? (No — limit is £500 variance)
3. System automatically escalates to team leader approval queue with the offer details, account history, and a recommendation
4. Team leader reviews — balance, payment history, recovery likelihood — approves within their authority (£1,500 variance limit)
5. Approval notification returns to agent — settlement confirmed, agent informs customer
6. System books the settlement: allocates £3,000 payment, writes off £1,200 remainder, generates settlement letter, closes account, posts GL entry
7. Full authority chain — who requested, who approved, at what time, at what balance — preserved in audit log

**Moment of Truth:** Show what happens if the agent tries to confirm the settlement without approval — the system blocks it. Then show the approval arriving and the account closing in a single automated sequence.

**Behavioral Requirements:** #4 Repayment Lifecycle, #7 RBAC & Audit, #10 3rd Party Placement, #11 Financial Accounting, #13 Agent Logging

---

## Updated Coverage Map (All 14 Flows)

| Behavioral Requirement | F1 | F2 | F3 | F4 | F5 | F6 | F7 | F8 | F9 | F10 | F11 | F12 | F13 | F14 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1. Relationship Management | | | | ✓ | | | | | | | | | | |
| 2. Data Capture & Pipeline | ✓ | | | | | | | | | | | | ✓ | |
| 3. Workflow Automation | ✓ | | ✓ | ✓ | ✓ | ✓ | | ✓ | ✓ | ✓ | | | ✓ | |
| 4. Repayment Lifecycle | | ✓ | ✓ | | | | ✓ | | | | | | | ✓ |
| 5. Multi-Channel Comms | ✓ | | ✓ | ✓ | | | | | ✓ | | | | ✓ | |
| 6. I&E Affordability | | ✓ | | | | | ✓ | | | | ✓ | | | |
| 7. RBAC & Audit | | ✓ | | ✓ | | ✓ | | ✓ | | ✓ | ✓ | ✓ | | ✓ |
| 8. Work Queue Distribution | ✓ | | | | | ✓ | | | | | ✓ | | ✓ | |
| 9. Decisioning & Strategy | ✓ | | | | ✓ | ✓ | | | | | | | ✓ | |
| 10. 3rd Party Placement | | | ✓ | | | | | | | ✓ | | | | ✓ |
| 11. Financial Accounting | | | ✓ | | | ✓ | | ✓ | | ✓ | | | ✓ | ✓ |
| 12. Exception & Escalation | | ✓ | ✓ | ✓ | | ✓ | | ✓ | ✓ | ✓ | ✓ | ✓ | | |
| 13. Agent Logging & History | ✓ | ✓ | | | | ✓ | ✓ | | | | | ✓ | | ✓ |
| 14. Self-Service & API | | | | | | | ✓ | | | | | | | |
| 15. Dynamic UI & Scripting | | ✓ | | | | | | | ✓ | | ✓ | | | |
| 16. Change Management | | | | | ✓ | | | | | | | | | |
| 17. Legacy Migration | | | | | | | | | | | | | | |
| 18. Vulnerability & Compliance | | ✓ | | ✓ | | | | | ✓ | | ✓ | ✓ | | |
| 19. Roadmap & Upgrades | | | | | ✓ | | | | | | | | | |
| 20. Parallel Processing | | | ✓ | ✓ | | | | | | | | | | |
