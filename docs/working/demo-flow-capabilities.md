# Demo Flow Capabilities — Working Document

> **Purpose:** Capture the system capabilities required for each of the six primary demo flows, to be consolidated into a Release 1 build scope.
> **Status:** In progress — Flow 1 complete, Flows 2–6 pending.

---

## Flow 1 — Account Intake to First Meaningful Contact

### Data Intake

- Accept a structured batch file of account records (customer + debt data)
- Accept account records via a real-time API push
- Display the integration channel configuration: scheduled intake window, expected format, recent run history
- Show a live progress indicator as records are received and processed
- Both intake paths (batch and API) operate through a single configurable intake layer, not two separate systems

### Validation and Data Quality

- Validate each incoming record against a configurable ruleset (required fields, field format, logical consistency)
- Classify validation failures by severity — hard stop (reject) vs. soft warning (flag and continue)
- Quarantine invalid records in a reviewable exception queue with a specific reason code per record
- Preserve the full original data of quarantined records — nothing is silently discarded
- Show each quarantined record with the reason code, the failing field, and a timestamp

### Customer Matching and Deduplication

- Attempt to match each incoming customer against existing records using NI number, DOB, name, and address
- Apply configurable field precedence and confidence scoring to matching decisions
- Automatically link a matched incoming account to the existing customer record, preserving prior accounts, notes, and flags
- Automatically create a new customer record where no match is found
- Flag potential household linkage for manual review where address or relationship indicators match an existing household member

### Account Enrichment

- Pull balance and product data from the host system via API call as part of the intake process
- Check each account against internal suppression lists (deceased, dispute, prior bankruptcy)
- Apply a benefit status flag (On Benefit / Off Benefit) as a structured, queryable data field
- Score each account against risk and collections segmentation models
- Record an enrichment audit trail per account: each enriched field carries its source, timestamp, and version reference

### Segmentation and Prioritisation

- Evaluate a configurable set of account attributes (balance, delinquency stage, risk band, benefit status, product type) to assign a segment code
- Calculate a treatment priority score per account
- Derive debt age in days from the debt effective date and use it as a segmentation input
- Allow a business user to modify segmentation thresholds, add segments, and adjust priority ordering without code changes
- Display the batch filtered by segment — showing how accounts have distributed across segments

### Queue Assignment and Work Allocation

- Automatically assign each segmented account to a work queue based on configurable routing rules
- Route specialist debt types to specialist queues
- Route vulnerability-flagged accounts to a vulnerable-customer team queue
- Route high-value accounts to a senior-agent queue
- Display queue management view: queue names, current depth, capacity targets, segment mix — side by side
- Flag queues at risk of capacity breach before the working day begins

### Collection Strategy and Automated First Actions

- Define collection strategies as configurable, versioned sets of rules — not hardcoded behaviour
- Automatically trigger first-contact communications (letter, SMS) for accounts that meet configured segment and strategy conditions, without any agent action
- Record the strategy rule that triggered each communication against the account event log
- Support configurable communication conditions: only send if no payment received, no active suppression, etc.

### Multi-Channel Communications

- Queue outbound letters to a third-party communications platform via the Notifications API
- Queue outbound SMS messages for accounts with a mobile number and a permitting strategy step
- Maintain a communications log per account: channel, template reference, trigger source, send timestamp, delivery status

### Agent Worklist and Case Screen

- Present each agent with a pre-populated, pre-prioritised worklist on login — no manual triage required
- Display a recommended first action per worklist item
- Show full case context on opening an item: account history, customer profile, linked debts, prior communications, notes, and flags — in one screen without navigating to multiple systems

### Supervisor Dashboard

- Show an operational summary of the overnight intake transformation: records received, validated, enriched, segmented, queued, and communicated — with counts per stage
- Display current queue volumes and team capacity utilisation side by side
- Show exception counts: validation failures, manual review items
- Flag any queues with a potential SLA or capacity risk before agents begin work

### Role-Based Access Control

- Agent sees only their own worklist and permitted case actions
- Supervisor sees team-level queue and capacity data
- Business user can modify strategy and segmentation configuration within their permitted scope
- All access is controlled by role, not by maintaining separate data sets

### Audit Trail

- Write a timestamped event to the account record for every automated system action (intake, validation, enrichment, segmentation, queue assignment, communication trigger)
- Each event records: the acting system or user, the action taken, the data state before and after, and the timestamp
- The full intake-to-first-contact journey for any account is reconstructable from its event log alone

---

## Flow 2 — Customer in Financial Difficulty: Vulnerability to Resolution

### Inbound Call Handling and Screen Pop

- Identify the customer at the point of an inbound call and surface their account automatically (screen pop)
- Present ID&V prompts on the agent screen at call start
- Display current arrangement status, outstanding balance, delinquency stage, treatment history, prior contact outcomes, and existing flags — all without the agent navigating away

### Vulnerability Recording

- Provide a structured vulnerability capture form — not a free-text notes field
- Capture: vulnerability type (from a configurable reference list), severity level, temporary or ongoing, start date, optional end date, source of disclosure (e.g. self-declared)
- Allow the vulnerability type reference list to be maintained by a permissioned business user without code changes
- Record the capturing agent's ID against the vulnerability record

### Automatic Suppression on Vulnerability Save

- On saving a vulnerability record, simultaneously and automatically:
  - Suppress all queued and scheduled outbound contact activities (letters, SMS, dialler tasks)
  - Remove the account from the standard collection queue
  - Write a suppression event to the audit trail with a timestamp
  - Flag the case on the supervisor dashboard as a recent vulnerability declaration
  - Generate a routing recommendation to the specialist vulnerable-customer team
- Suppression must be immediate — not deferred to a batch process
- The suppression event must be visible and distinguishable in the communications queue (suppressed items shown, not deleted)
- A standard agent cannot override or reverse suppression — it requires a permissioned user

### Vulnerability Type to Suppression Behaviour Mapping

- Support configurable mapping between vulnerability type and resulting system behaviour (e.g. mental health → full contact suppression; temporary bereavement → lighter-touch protocol)
- Allow a permissioned user to update or remove a vulnerability record; log the removal event; lift suppression automatically when the flag is removed

### Case Transfer with Full Context

- Transfer a case from a standard agent to a specialist queue, carrying all case history, notes, suppression events, and vulnerability details
- Record the transfer event: sending agent, receiving queue, timestamp
- Specialist receives the case with identical context to what the standard agent had, plus the vulnerability disclosure details

### Income and Expenditure Capture

- Provide a structured I&E capture form covering: household income sources, fixed outgoings, variable expenditure, assets
- Use configurable reference data for standard expenditure thresholds, categorised by household composition
- Flag significant variances where declared expenditure deviates materially from reference thresholds
- Support optional comparison against bureau or credit reference data where available
- Store the I&E record as a structured data entity — each field validated, not free text

### Affordability Calculation

- Calculate disposable income from the completed I&E record using a configurable calculation model
- The calculation formula, floor, ceiling, and reference expenditure table must all be configurable by a permissioned user without code changes
- Present repayment options at multiple frequencies (weekly, fortnightly, monthly) calibrated to the calculated disposable figure
- Present statutory forbearance options (breathing space, time to pay) alongside plan options

### Repayment Arrangement Creation

- Create a repayment arrangement from the agreed frequency and amount
- Display the resulting arrangement: start date, full payment schedule, amounts per instalment, total expected recovery, calculated end date
- Automatically generate and queue a confirmation communication on arrangement creation
- Immediately start a payment monitoring workflow on arrangement creation — no manual scheduling required

### Payment Monitoring Workflow

- Watch for each payment due date on an active arrangement
- Apply configurable tolerance rules for late payments before confirming a breach
- Automatically escalate if a payment is missed beyond the tolerance window
- The monitoring workflow must run without agent involvement — no calendar management required

### Audit Trail — End to End

- Record a timestamped event for every action in the flow: screen pop, vulnerability disclosure, suppression, transfer, I&E capture, affordability calculation, arrangement creation, confirmation communication queued
- Each event records: acting user or automated system, action taken, data state before and after, timestamp
- The complete case history from first call to arrangement creation must be reconstructable from the audit trail alone

---

## Flow 3 — Arrangement Breach to Escalation to Third-Party Placement

### Payment Monitoring

- Monitor each payment due date on every active repayment arrangement continuously and automatically
- Detect when a payment has not been received by its due date
- Apply a configurable grace/tolerance window before confirming breach — grace period length configurable per segment or strategy
- Display the payment due date, expected amount, received status, and tolerance window status on the arrangement view
- Record a timestamped event when the grace window opens and when it expires

### Breach Detection and Confirmation

- Confirm an account as breached when the grace period expires with no payment received
- Write a breach confirmation event to the audit trail: timestamp, breach type, arrangement reference
- Flag the breached account on the exception dashboard immediately on breach confirmation
- Support configurable breach rules per segment — different segments may have different grace periods and breach thresholds

### Breach Communications Workflow

- Automatically initiate a sequenced breach communications workflow on breach confirmation — no agent action required
- The sequence is configurable per strategy: which channels, in which order, with which waiting periods between steps
- Generate and queue a first breach letter automatically
- Generate and queue a first breach SMS automatically where the strategy and account permit it
- Create outbound call tasks for agents where the strategy includes a call step
- Record every contact event: channel, timestamp, outcome (delivered, no answer, declined, promise to pay)

### Promise-to-Pay Handling

- Allow an agent to record a promise-to-pay outcome against a call contact event
- On recording a promise to pay, automatically create a new payment monitoring step watching for the promised payment by the promised date
- If the promised payment is not received, resume the breach workflow from the appropriate point

### Non-Response Escalation

- After the configured re-engagement sequence is exhausted with no response or payment, automatically promote the account to a pre-placement holding state
- Surface the account in a supervisor exception queue with the full breach timeline visible: breach date, contacts attempted, outcomes, elapsed time
- Show clearly whether the account meets the configured automatic escalation criteria

### Supervisor Override

- Allow a supervisor to override the escalation and retain the case internally, with a mandatory review note
- Place the overridden case in a short-term hold with a configurable follow-up trigger
- Remove the overridden case from the placement queue without affecting other accounts
- Record the override decision: supervisor ID, timestamp, reason note

### Third-Party Collection Placement

- Transmit a placement data package to a configured third-party collection provider when escalation criteria are met and no override has been applied
- Placement package includes: customer details, debt summary, balance, full contact history
- Transmit via a configurable channel (API or structured file)
- Receive and log a placement acknowledgement from the third-party provider
- On placement, immediately suspend all internal contact strategy activity for the placed account — letters, SMS, call tasks must not fire while the account is with the third-party provider
- Enforce mutual exclusivity between internal collection activity and third-party placement — the two cannot run in parallel

### Post-Placement Payment Handling

- Receive and record payments returned from the third-party provider
- Allocate each payment to the correct debt components in a configurable waterfall order (e.g. principal, fees, interest)
- Update the account balance on payment allocation
- Calculate and record any applicable third-party commission or fee
- Create a reconciliation record for each payment received from the third-party provider
- Handle partial payments as well as full settlements

### Third-Party Provider Management

- Display a performance summary per configured third-party provider: placements made, payments received, recovery rate, average time to first payment, outstanding balance with the provider
- Data in the performance view must be real-time — reflecting the most recently recorded payment, not a batch calculation

### Recall and Case Return

- Send a recall instruction to the third-party provider when a placed debt is resolved (paid in full, written off, or recalled)
- Lift the placement flag on recall
- Track the reconciliation between internal records and third-party provider records

### Audit Trail — End to End

- Record a timestamped event for every step in the breach-to-placement lifecycle: payment due, grace period opened, breach confirmed, each contact attempt and outcome, escalation gate reached, supervisor decision, placement transmitted, acknowledgement received, payment allocated
- The complete timeline from first missed payment to placement must be reconstructable from the audit trail alone and presentable as a single chronological view

---

## Flow 4 — Complex Household: Multiple Accounts, Joint Liability, Competing Workflows

> **Note on entity model:** The exact data model underlying this flow is not yet settled. Domain rulings indicate that the primary unit of analysis is the individual claimant, not a named household entity, and that joint UC couple accounts are represented via Account-Person Links (max 2 persons per account). However, the platform pivot (ADR-018) means prior architectural decisions are under review. The capabilities below describe what the system must be able to *do* for the demo regardless of how the underlying model is eventually structured.

### Linked Customer and Account View

- Represent the relationship between two individuals on a joint UC account as a navigable, explicitly modelled structure — not inferred from a shared address
- Display all accounts and debts associated with linked individuals from a single unified view
- Show status indicators per account: active plan, legal treatment, restriction active, etc.
- Search for a customer by name, NI number, or other identifier and navigate directly to their linked-account view

### Joint Liability

- Model a debt as jointly owned by two linked individuals
- Display both individuals as debtors on a joint debt record
- Generate correspondence addressable to either or both parties on a joint debt from a single trigger
- When triggered for a joint debt, automatically produce separate, correctly addressed communications for each party

### Joint Debt Split

- Split a joint debt into two individually owned debts — either manually by an authorised user or automatically by a configured rule
- Apportion the balance equally (50/50) between the two resulting individual debts
- Automatically write off any residual rounding amount (e.g. 1p) without requiring manual intervention
- Preserve the original cause-of-action date on both resulting debts — the limitation clock does not reset on split
- Block a split if an active repayment arrangement or legal hold exists on the account
- Record the split event, the apportionment, and the residual write-off in the audit trail

### Restriction Enforcement

- Support multiple restriction types applicable at account level (e.g. breathing space, dispute, deceased, legal)
- When any individual linked to a joint account has an active protection, gate the entire account — both individuals are protected
- Enforce restrictions at runtime — evaluated at the moment an action is attempted, not pre-calculated and cached
- A restriction applied minutes ago must immediately block the very next attempted action against that account

### Runtime Action Blocking

- Check all active restrictions before executing any outbound action (letter, SMS, call task, dialler)
- If a restriction is active, block the action before it is initiated — nothing must be partially sent
- Display an explicit alert to the agent: what was blocked, which restriction caused it, and what the agent is and is not permitted to do
- Leave no trace of a blocked action in the outbound systems — a blocked attempt must not appear as a sent communication
- A standard agent must not be able to override a runtime block — override requires a permissioned user

### Proceeding on Valid Accounts

- Allow actions to proceed normally on accounts associated with the same individuals that have no active restrictions
- Record the completed action against only the account it was performed on — no other accounts are affected
- The event log for a restricted account must show no new entries as a result of actions taken on other accounts

### Parallel Workflow Management

- Run separate, independent workflow process instances per account — one account's workflow does not interfere with another's
- Each account carries its own event timeline, upcoming actions, and communication history
- Cross-account compliance is enforced through restriction checks, not by coupling the workflow instances together
- Display each account's workflow state and upcoming actions independently within the linked-account view

### Supervisor Linked-Account View

- Display all accounts and their statuses for linked individuals as a unit in the supervisor dashboard
- Show active restriction indicators, plan health, and specialist flags across all accounts at a glance
- Allow a supervisor to identify in seconds whether all accounts for a set of linked individuals are being handled correctly

### Restriction Lifecycle

- Apply an end date to time-limited restrictions (e.g. breathing space — 60 days)
- Automatically lift a restriction when its end date passes — no manual step required
- Log the restriction lift event with a timestamp
- Resume permitted outbound activity automatically after restriction lift

### Audit Trail — End to End

- Record a timestamped event for every action attempted against any account in the linked set: successful actions, blocked actions, restriction applications, restriction lifts, transfers, split events
- Blocked action attempts must be logged — the audit trail must show that a block occurred, which restriction triggered it, and that nothing was sent
- The complete interaction history across all linked accounts must be reconstructable from the audit trail alone

---

## Flow 5 — Business User Changes a Collection Strategy: No IT Required

### Strategy Authoring Interface

- Present collection strategies in a visual or structured interface that is navigable and interpretable by a business user — not raw configuration data or XML
- Display the sequence of communication steps, timing rules between steps, branching conditions, and end states in a readable form
- Allow a business user to add, remove, reorder, and configure steps through a form-driven interface — no code editor, no IT change request
- Allow selection of communication templates from the third-party communications platform's template library, browsable from within the strategy interface
- Allow configuration of step conditions in business-readable terms (e.g. "send only if no payment received and no active suppression") — not code expressions

### Strategy Versioning

- Save every change as a new draft version — the live strategy continues running for current accounts without interruption
- Display the live version and the draft version side by side with a readable diff showing exactly what changed
- Every version carries: version number, author, timestamp, and status (draft, in review, live, archived)
- Maintain the complete version lineage of a strategy — every prior version is preserved and inspectable

### Peer Review and Approval Workflow

- Allow a strategy author to submit a draft for peer review
- Notify the designated reviewer of a pending review request
- Allow the reviewer to inspect the strategy diff, add comments, and approve or reject
- The number of required reviewers, the escalation path if a reviewer is unavailable, and the permission roles involved must all be configurable
- A strategy cannot be published without completing the configured approval workflow

### Historical Simulation

- Allow a user to select a historical date range and a subset of accounts and replay those accounts through the proposed strategy instead of the live strategy
- Produce a results comparison showing projected outcome differences: contact rate, promise-to-pay rate, call volume, arrangement creation rate
- Run the simulation in a sandboxed environment — live accounts and live operational data are not affected
- Label all simulation results clearly as modelled projections based on historical behaviour, not live outcomes
- The simulation must complete within a time that is practical during a live demonstration

### Champion/Challenger Configuration

- Configure a split of new accounts between a champion strategy (existing) and a challenger strategy (proposed) by a configurable percentage ratio
- Set a start date and an evaluation period for the test
- Apply the split to new accounts only — accounts already in the live strategy are not affected
- The split assignment must be random and reproducible — the system can retrospectively identify which path each account was on
- Allow the test to be stopped and the challenger archived without any impact on live accounts

### Champion/Challenger Results Monitoring

- Display comparative performance results side by side for champion and challenger accounts during and after the evaluation period
- Metrics shown: contact rate, promise-to-pay rate, call volume, arrangement creation rate, breach rate
- Include a statistical significance indicator — confirm whether the sample size is sufficient to draw a confident conclusion
- Results must be drawn from real operational data — actual outcomes of accounts routed through each path, not simulated

### Strategy Promotion and Rollback

- Promote a challenger strategy to full deployment in a single explicit action
- On promotion, archive the previous live version
- Allow one-click rollback to any prior version — the previous strategy definition is preserved exactly and can be redeployed without reconstruction
- Record the promotion and rollback events in the version history with actor, timestamp, and reason

### Strategy Deployment Governance

- Prevent a logically inconsistent strategy configuration from being saved
- Require explicit approval before any strategy change reaches live accounts
- Support deployment to non-production environments for UAT before live promotion
- The environment promotion path must be configurable and version-controlled

### Audit Trail — End to End

- Record a timestamped event for every strategy lifecycle action: draft created, step added or modified, review submitted, review outcome, simulation run, champion/challenger configured, promotion, rollback
- Each event records: acting user, action taken, version before and after, timestamp
- The complete change history of a strategy must be reconstructable from the audit trail alone

---

## Flow 6 — Executive and Operational Dashboard: Real-Time Oversight

### Top-Level Operational Dashboard

- Display a single-screen operational summary showing the current state of the entire collections operation
- Metrics shown at the top level: queue volumes by team and product type, active arrangement count and health (green/amber/red), today's contact rate by channel, breach count for the current period (total and by segment), active third-party placements and balance with agencies, active vulnerability cases and specialist queue depth
- All data must be live — reflecting current operational state, not a cached or batch-calculated snapshot
- The dashboard must be accessible at any time without requesting anything from an analyst or operations team

### Drill-Down Navigation

- Allow a user to click any metric tile and navigate to a detailed breakdown view
- Drill-down views show: account count, rate versus prior period, time-series chart over a configurable lookback window, segment and product breakdowns
- Navigation must be fluid — no separate report requests, no page reloads to a separate reporting system

### Strategy Performance Traceability

- Display strategy performance metrics per segment: contact rate, promise-to-pay rate, arrangement creation rate, breach rate — compared against a configurable baseline period
- Link a performance anomaly directly to the strategy change audit trail — show the most recent change event for the relevant strategy alongside the performance data
- Allow a user to navigate from a performance metric directly to the strategy definition that drove it, without leaving the dashboard

### Queue Ageing and SLA Management

- Display queue ageing across all queues: accounts in queue by time band, SLA target per queue, and accounts breaching SLA threshold
- Present ageing as a heatmap or ranked list — immediately visible which queues are at risk
- Automatically escalate accounts that breach the SLA threshold in priority and surface them in exception views
- Flag capacity risk on any queue before the next SLA breach occurs

### Supervisory Intervention

- Allow a supervisor or operations director to bulk-reassign selected accounts from one queue or team to another directly from the dashboard
- Record every reassignment: actor, timestamp, accounts affected, source queue, destination queue, reason code
- The receiving team sees newly assigned items in their queue immediately — no delay
- Bulk reassignment is a permissioned action — not available to all roles

### Champion/Challenger Reporting

- Display champion and challenger strategy performance side by side, accessible from the dashboard without navigating to the strategy module
- Metrics: contact rate, promise-to-pay rate, arrangement creation rate, breach rate — for champion and challenger cohorts
- Include a statistical significance indicator confirming whether the sample is sufficient for a confident conclusion
- Allow a user to promote the winning strategy directly from the results view

### Vulnerability Exception Reporting

- Surface cases where a vulnerability flag has been active beyond a configured review period without a specialist review
- Surface cases where the specialist queue has breached its SLA
- Surface cases where a temporary vulnerability record is approaching its expiry date and requires review
- Vulnerability management is active — the platform monitors flag age and status continuously and generates exception alerts when review is overdue

### Management and Governance Report Export

- Allow a user to select a report type (operational summary, vulnerability management, SLA performance, strategy effectiveness), a reporting period, and an output format
- Generate a structured, branded report pulling from the same live data as the dashboard — not a separate data extract
- Report templates must be configurable: DWP can define the metrics, layout, and comparison baselines for standard reports without code changes
- Support export to PDF and structured data formats

### Role-Based Dashboard Views

- An agent sees only their own queue and permitted case actions
- A team leader sees their team's queues and performance
- An operations director sees the full operation
- Access is filtered by role from the same data layer — not maintained as separate data sets per role
- Dashboard configuration (KPIs, thresholds, comparison baselines) must be customisable per role or user without code changes

### Audit Trail — End to End

- Record a timestamped event for every management action taken from the dashboard: bulk reassignments, report exports, threshold changes, strategy promotions
- Each event records: acting user, action taken, accounts or entities affected, timestamp
- All user actions taken from the dashboard are fully auditable — nothing is an anonymous or untracked operation

---

## Consolidated Release 1 Capability List

> Compiled from all six demo flows. Capabilities are grouped by functional domain. Where the same capability appears across multiple flows it is listed once.

> **Dashboard surfaces — not yet decided.** The capability list references several distinct dashboard and reporting views: an operational summary, a queue management view, a supervisor intake view, a linked-account view, and a champion/challenger analytics view. These do not need to be separate dashboards — the screen model has not been decided. What is decided is that all the functionalities described across these views must exist in the system. How they are organised into screens, tabs, or drill-downs is a design decision to be made later.

### Data Intake and Integration

- Accept structured batch file intake of customer and debt account records
- Accept account records via real-time API push
- Operate both intake paths through a single configurable intake layer
- Display integration channel configuration: scheduled window, expected format, recent run history
- Show a live progress indicator as records are received and processed
- Transmit placement data packages to third-party collection providers via configurable channel (API or structured file)
- Receive and log acknowledgements from third-party providers
- Receive and process inbound payment notifications from third-party providers

### Validation and Data Quality

- Validate each incoming record against a configurable ruleset (required fields, format, logical consistency)
- Classify validation failures by severity: hard stop vs. soft warning
- Quarantine invalid records in a reviewable exception queue with a specific reason code per record
- Preserve the full original data of quarantined records — nothing is silently discarded

### Customer and Account Model

- Match incoming customers against existing records using NI number, DOB, name, and address with configurable field precedence and confidence scoring
- Automatically link a matched account to the existing customer record, preserving prior accounts, notes, and flags
- Automatically create a new customer record where no match is found
- Flag potential linked-individual relationships for manual review
- Represent the relationship between linked individuals (e.g. joint UC couple) as an explicitly modelled, navigable structure
- Display all accounts and debts for linked individuals from a single unified view with status indicators per account
- Search for a customer by name, NI number, or other identifier and navigate directly to their linked-account view
- Model a debt as jointly owned by two linked individuals
- Display both individuals as debtors on a joint debt record
- Split a joint debt into two individually owned debts (manually or by configured rule), apportioning 50/50
- Automatically write off residual rounding amounts on split without manual intervention
- Preserve cause-of-action date on both resulting debts after a split
- Block a joint debt split if an active repayment arrangement or legal hold exists

### Account Enrichment

- Pull balance and product data from the host system via API as part of the intake process
- Check each account against internal suppression lists (deceased, dispute, prior bankruptcy)
- Apply a benefit status flag (On Benefit / Off Benefit) as a structured, queryable field
- Score each account against risk and collections segmentation models
- Record an enrichment audit trail per field: source, timestamp, version reference

### Segmentation and Prioritisation

- Evaluate configurable account attributes (balance, delinquency stage, risk band, benefit status, product type, debt age) to assign a segment code
- Calculate a treatment priority score per account
- Derive debt age in days from the debt effective date
- Allow a business user to modify segmentation thresholds, add segments, and adjust priority ordering without code changes

### Collection Strategy

- Define collection strategies as configurable, versioned sets of rules — not hardcoded behaviour
- Present strategies in a visual or structured interface navigable and interpretable by a business user
- Allow a business user to add, remove, reorder, and configure strategy steps through a form-driven interface with no code editor or IT change request
- Allow selection of communication templates from the third-party platform's template library within the strategy interface
- Allow configuration of step conditions in business-readable terms
- Save every change as a new draft version — the live strategy continues without interruption
- Display live and draft versions side by side with a readable diff
- Maintain complete version lineage — every prior version preserved and inspectable
- Require peer review and approval before any strategy change reaches live accounts
- Support configurable approval workflow: number of reviewers, escalation path, permission roles
- Prevent logically inconsistent strategy configurations from being saved
- Support deployment to non-production environments before live promotion
- Allow one-click rollback to any prior version
- Record every strategy lifecycle event in the audit trail

### Historical Simulation and Champion/Challenger

- Replay a historical set of accounts through a proposed strategy in a sandboxed environment
- Produce a results comparison: contact rate, promise-to-pay rate, call volume, arrangement creation rate
- Label all simulation results as modelled projections — live data unaffected
- Configure a champion/challenger split by percentage ratio, start date, and evaluation period
- Apply the split to new accounts only — existing accounts unaffected
- Assign splits randomly and reproducibly — retrospective path identification supported
- Provide a champion/challenger analytics view showing: side-by-side comparative performance for champion and challenger cohorts, metrics including contact rate, promise-to-pay rate, call volume, arrangement creation rate, and breach rate, and a statistical significance indicator confirming whether the sample is sufficient for a confident conclusion
- Champion/challenger analytics must be accessible both from the strategy module and from the operational dashboard without navigating away
- Results must be drawn from real operational data — actual outcomes of accounts routed through each path, not simulated
- Allow promotion of the winning strategy directly from the results view
- Allow the test to be stopped and the challenger archived without live account impact

### Queue Management and Work Allocation

- Automatically assign each segmented account to a work queue based on configurable routing rules
- Route specialist, vulnerability-flagged, and high-value accounts to appropriate queues
- Display queue management view: names, depth, capacity targets, segment mix, SLA targets
- Flag queues at risk of capacity or SLA breach
- Display queue ageing: accounts by time band against SLA target
- Present ageing as a heatmap or ranked list
- Automatically escalate accounts breaching SLA threshold in priority
- Allow bulk reassignment of accounts between queues or teams from the supervisor view
- Record every reassignment: actor, timestamp, accounts affected, source, destination, reason
- Present each agent with a pre-populated, pre-prioritised worklist on login
- Display a recommended first action per worklist item

### Case Screen and Agent Experience

- Display full case context on opening an item: account history, customer profile, linked accounts, prior communications, notes, and flags — in one screen
- Identify an inbound caller and surface their account automatically (screen pop)
- Present ID&V prompts on the agent screen at call start
- Transfer a case between agents or queues carrying all history, notes, suppression events, and flags
- Record the transfer event: sending agent, receiving queue, timestamp

### Vulnerability Management

- Provide a structured vulnerability capture form: type, severity, temporary/ongoing, start date, optional end date, source of disclosure, capturing agent ID
- Maintain the vulnerability type reference list as configurable reference data
- On saving a vulnerability record, simultaneously and automatically: suppress all outbound contact, remove from standard queue, write a suppression audit event, flag on supervisor dashboard, generate specialist routing recommendation
- Suppression must be immediate — not deferred to a batch process
- Show suppressed communications in the queue as suppressed — not deleted
- Prevent a standard agent from overriding suppression
- Support configurable mapping between vulnerability type and resulting suppression behaviour
- Allow a permissioned user to update or remove a vulnerability record; log the removal; lift suppression automatically
- Monitor vulnerability flag age continuously; generate exception alerts when specialist review is overdue
- Surface cases approaching temporary vulnerability expiry for review

### Restrictions and Compliance Enforcement

- Support multiple restriction types (breathing space, dispute, deceased, legal) applicable at account level
- When any individual linked to a joint account has an active protection, gate the entire account
- Enforce restrictions at runtime — evaluated at the moment of action, not pre-calculated
- Block any outbound action (letter, SMS, call task, dialler) before it is initiated when a restriction is active
- Display an explicit alert: what was blocked, which restriction caused it, what is and is not permitted
- Leave no trace of a blocked action in outbound systems
- Prevent standard agents from overriding runtime blocks
- Apply end dates to time-limited restrictions; lift automatically on expiry; log the lift event

### Communications

- Queue outbound letters to a third-party communications platform via the Notifications API
- Queue outbound SMS messages
- Create outbound call tasks for agents
- Maintain a communications log per account: channel, template reference, trigger source, send timestamp, delivery status
- Generate correspondence to multiple parties on a joint debt from a single trigger, producing separate correctly addressed copies
- Automatically generate and queue confirmation communications on arrangement creation and other key lifecycle events
- Automatically initiate sequenced breach communications workflows without agent action
- Record every contact event outcome: delivered, no answer, declined, promise to pay

### Repayment Arrangement Lifecycle

- Create a repayment arrangement from an agreed frequency and amount
- Display the arrangement: start date, full payment schedule, amounts, total expected recovery, calculated end date
- Monitor each payment due date on every active arrangement continuously and automatically
- Apply configurable grace/tolerance windows before confirming breach
- Confirm breach automatically when grace period expires with no payment received
- Record a promise-to-pay outcome and create a new monitoring step for the promised payment
- Support arrangement amendment, versioning, and review
- Pause an arrangement (e.g. during vulnerability or forbearance period)

### Income and Expenditure

- Provide a structured I&E capture form: household income sources, fixed outgoings, variable expenditure, assets
- Use configurable reference data for standard expenditure thresholds by household composition
- Flag significant variances against reference thresholds
- Calculate disposable income using a configurable model (formula, floor, ceiling, reference table — all configurable without code)
- Present repayment options at multiple frequencies calibrated to disposable income
- Present statutory forbearance options (breathing space, time to pay) alongside plan options

### Payment Allocation and Reconciliation

- Allocate payments to debt components in a configurable waterfall order (principal, fees, interest)
- Update account balance on payment allocation
- Calculate and record third-party commission or fee where applicable
- Create a reconciliation record for each payment
- Handle partial payments and full settlements
- Send recall instructions to third-party providers on debt resolution; lift placement flag; track reconciliation

### Third-Party Provider Management

- Display real-time performance summary per provider: placements, payments received, recovery rate, average time to first payment, outstanding balance
- Enforce mutual exclusivity between internal collection activity and third-party placement

### Write-Off

- Write off residual amounts automatically (e.g. 1p split residual) with a system-generated reason code
- Support authorised manual write-off with approval workflow

### Operational Dashboard and Reporting

- Display a live single-screen operational summary: queue volumes, arrangement health, contact rate, breach count, third-party balances, vulnerability queue depth
- Support drill-down from any metric to a detailed breakdown view with time-series chart
- Link performance anomalies to the strategy change audit trail
- Display strategy performance metrics per segment against a configurable baseline
- Display vulnerability exception reports: overdue reviews, SLA breaches, approaching expiries
- Allow bulk reassignment directly from the dashboard
- Generate structured, branded governance reports from live data — configurable templates, multiple output formats
- Role-filtered views: agent, team leader, operations director — from the same data layer

### Role-Based Access Control

- Control all access by role — what a user can see and do is determined by their assigned role
- Support at minimum: standard agent, specialist agent, supervisor/team leader, operations director, strategy manager, business administrator
- All access filtering from a single data layer — not separate data sets per role
- Dashboard configuration (KPIs, thresholds, baselines) customisable per role without code changes

### Audit Trail (System-Wide)

- Write a timestamped event to the relevant record for every system action and every user action
- Each event records: acting user or system, action taken, data state before and after, timestamp
- The complete lifecycle of any account, case, strategy, or management action must be reconstructable from the audit trail alone
- Blocked actions must be logged — the audit trail shows that a block occurred, which restriction triggered it, and that nothing was sent
- Audit trail must be accessible to a permissioned user without requiring analyst involvement
