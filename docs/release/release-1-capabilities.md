# Release 1 Capabilities

> **Status:** Authoritative Release 1 scope baseline.
> **Date:** 2026-04-30
> **Source trace:** Derived from the six demo flows in [`../working/demo-flow-capabilities.md`](../working/demo-flow-capabilities.md) and the gap rationale in [`../working/release-1-gap-analysis.md`](../working/release-1-gap-analysis.md).

---

## Scope Principles

Release 1 is a prototype/demo release. It must demonstrate the DCMS-owned capability, workflow behaviour, state handling, audit trail, and integration boundaries needed to support the tender-relevant journeys.

Release 1 demonstrates DCMS-side integration and contact orchestration readiness through adapter boundaries, contracts, simulated responses, state transitions, and audit evidence. Live third-party integrations, production dialler operation, automated SMS/email sending, payment-link providers, and live bank-validation APIs are outside Release 1 scope unless access and delivery scope change.

Dashboard surfaces are not yet fixed. The capability list references several distinct dashboard and reporting views: an operational summary, a queue management view, a supervisor intake view, a linked-account view, and a champion/challenger analytics view. These do not need to be separate dashboards. The required scope is the functionality, not the final screen decomposition.

---

## Data Intake and Integration

- Accept structured batch file intake of customer and debt account records
- Accept account records via real-time API push
- Operate both intake paths through a single configurable intake layer
- Display integration channel configuration: scheduled window, expected format, recent run history
- Show a live progress indicator as records are received and processed
- Transmit placement data packages to third-party collection providers via configurable channel (API or structured file)
- Receive and log acknowledgements from third-party providers
- Receive and process inbound payment notifications from third-party providers
- Define and demonstrate outbound host-system action invocation for write-off, account block, closure, account management, card management, and customer detail update events using a simulated host adapter
- Define and demonstrate general ledger posting events, acknowledgement/rejection handling, reconciliation states, and audit evidence using a simulated GL adapter

## Validation and Data Quality

- Validate each incoming record against a configurable ruleset (required fields, format, logical consistency)
- Classify validation failures by severity: hard stop vs. soft warning
- Quarantine invalid records in a reviewable exception queue with a specific reason code per record
- Preserve the full original data of quarantined records; nothing is silently discarded

## Customer and Account Model

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

## Account Enrichment

- Pull balance and product data from the host system via API as part of the intake process
- Check each account against internal suppression lists (deceased, dispute, prior bankruptcy)
- Apply a benefit status flag (On Benefit / Off Benefit) as a structured, queryable field
- Score each account against risk and collections segmentation models
- Record an enrichment audit trail per field: source, timestamp, version reference
- Record amounts owed to the customer or their representative and make those amounts available as potential offsets against existing debts based on configured business rules

## Segmentation and Prioritisation

- Evaluate configurable account attributes (balance, delinquency stage, risk band, benefit status, product type, debt age) to assign a segment code
- Calculate a treatment priority score per account
- Derive debt age in days from the debt effective date
- Allow a business user to modify segmentation thresholds, add segments, and adjust priority ordering without code changes
- Support predictive analysis and trend forecasting inputs for strategy design and debtor/debt segmentation

## Collection Strategy

- Define collection strategies as configurable, versioned sets of rules; not hardcoded behaviour
- Present strategies in a visual or structured interface navigable and interpretable by a business user
- Allow a business user to add, remove, reorder, and configure strategy steps through a form-driven interface with no code editor or IT change request
- Allow selection of communication templates from a simulated third-party platform template library within the strategy interface
- Allow configuration of step conditions in business-readable terms
- Save every change as a new draft version; the live strategy continues without interruption
- Display live and draft versions side by side with a readable diff
- Maintain complete version lineage; every prior version preserved and inspectable
- Require peer review and approval before any strategy change reaches live accounts
- Support configurable approval workflow: number of reviewers, escalation path, permission roles
- Prevent logically inconsistent strategy configurations from being saved
- Support deployment to non-production environments before live promotion
- Allow one-click rollback to any prior version
- Record every strategy lifecycle event in the audit trail
- Provide pre-configured or suggested strategy templates for deceased, insolvency, hardship, and persistent debt scenarios
- Support pre-arrears and non-delinquent customer strategies for early intervention before delinquency
- Support campaign strategies that send simulated outbound contact requests and automatically capture simulated customer responses such as payment, arrangement, offer, or settlement

## Historical Simulation and Champion/Challenger

- Replay a historical set of accounts through a proposed strategy in a sandboxed environment
- Produce a results comparison: contact rate, promise-to-pay rate, call volume, arrangement creation rate
- Label all simulation results as modelled projections; live data unaffected
- Support predictive analysis and trend forecasting inputs for champion/challenger configuration
- Configure a champion/challenger split by percentage ratio, start date, and evaluation period
- Apply the split to new accounts only; existing accounts unaffected
- Assign splits randomly and reproducibly; retrospective path identification supported
- Provide a champion/challenger analytics view showing side-by-side comparative performance for champion and challenger cohorts, including contact rate, promise-to-pay rate, call volume, arrangement creation rate, breach rate, and a statistical significance indicator
- Champion/challenger analytics must be accessible both from the strategy module and from the operational dashboard without navigating away
- Results must be drawn from real operational data; actual outcomes of accounts routed through each path, not simulated
- Allow promotion of the winning strategy directly from the results view
- Allow the test to be stopped and the challenger archived without live account impact

## Queue Management and Work Allocation

- Automatically assign each segmented account to a work queue based on configurable routing rules
- Route specialist, vulnerability-flagged, and high-value accounts to appropriate queues
- Account for agent availability, holiday, sickness, geographic working calendars, and non-working days when assigning work and scheduling actions
- Display queue management view: names, depth, capacity targets, segment mix, SLA targets
- Flag queues at risk of capacity or SLA breach
- Display queue ageing: accounts by time band against SLA target
- Present ageing as a heatmap or ranked list
- Automatically escalate accounts breaching SLA threshold in priority
- Allow bulk reassignment of accounts between queues or teams from the supervisor view
- Allow authorised users to bulk move accounts forward or backward to a selected point within a workflow, recording reason and audit evidence
- Record every reassignment: actor, timestamp, accounts affected, source, destination, reason
- Present each agent with a pre-populated, pre-prioritised worklist on login
- Display a recommended first action per worklist item

## Case Screen and Agent Experience

- Display full case context on opening an item: account history, customer profile, linked accounts, prior communications, notes, and flags in one screen
- Identify an inbound caller and surface their account automatically (screen pop)
- Present ID&V prompts on the agent screen at call start
- Capture and apply customer contact preferences for preferred times, channels, language, accessibility needs, and channel exclusions when automated contact strategies create outbound requests
- Transfer a case between agents or queues carrying all history, notes, suppression events, and flags
- Record the transfer event: sending agent, receiving queue, timestamp

## Vulnerability Management

- Provide a structured vulnerability capture form: type, severity, temporary/ongoing, start date, optional end date, source of disclosure, capturing agent ID
- Maintain the vulnerability type reference list as configurable reference data
- On saving a vulnerability record, simultaneously and automatically suppress all outbound contact, remove from standard queue, write a suppression audit event, flag on supervisor dashboard, and generate specialist routing recommendation
- Suppression must be immediate; not deferred to a batch process
- Show suppressed communications in the queue as suppressed; not deleted
- Prevent a standard agent from overriding suppression
- Support configurable mapping between vulnerability type and resulting suppression behaviour
- Allow a permissioned user to update or remove a vulnerability record; log the removal; lift suppression automatically
- Monitor vulnerability flag age continuously; generate exception alerts when specialist review is overdue
- Surface cases approaching temporary vulnerability expiry for review

## Restrictions and Compliance Enforcement

- Support multiple restriction types (breathing space, dispute, deceased, legal) applicable at account level
- When any individual linked to a joint account has an active protection, gate the entire account
- Enforce restrictions at runtime; evaluated at the moment of action, not pre-calculated
- Block any outbound action (letter, SMS, call task, dialler) before it is initiated when a restriction is active
- Display an explicit alert: what was blocked, which restriction caused it, what is and is not permitted
- Leave no trace of a blocked action in outbound systems
- Prevent standard agents from overriding runtime blocks
- Apply end dates to time-limited restrictions; lift automatically on expiry; log the lift event
- Support fraud as a separately flaggable account state and enforce any resulting account restrictions through the same runtime action-gating model

## Communications

- Queue outbound letter requests to a simulated third-party communications platform / Notifications API boundary
- Queue outbound SMS requests to a simulated channel boundary
- Create outbound call tasks for agents
- Demonstrate dialler handoff through simulated call-task contracts, queueing rules, expected dialler status callbacks, and outcome handling; live outbound calling is outside Release 1 scope
- Enforce configurable contact frequency limits and permitted contact time windows before creating any outbound contact request
- Maintain a communications log per account: channel, template reference, trigger source, send timestamp, delivery status
- Generate correspondence to multiple parties on a joint debt from a single trigger, producing separate correctly addressed copies
- Automatically generate and queue confirmation communications on arrangement creation and other key lifecycle events
- Automatically initiate sequenced breach communications workflows without agent action
- Record every contact event outcome: delivered, no answer, declined, promise to pay
- Automatically record simulated unsuccessful outbound call outcomes and trigger configured fallback contact through an alternative simulated channel
- Automatically generate disclosure notifications at configurable points in the journey, including pre-DCA placement

## Repayment Arrangement Lifecycle

- Create a repayment arrangement from an agreed frequency and amount
- Display the arrangement: start date, full payment schedule, amounts, total expected recovery, calculated end date
- Monitor each payment due date on every active arrangement continuously and automatically
- Apply configurable grace/tolerance windows before confirming breach
- Confirm breach automatically when grace period expires with no payment received
- Record a promise-to-pay outcome and create a new monitoring step for the promised payment
- Return accounts from a promise-to-pay sub-process to the same point in the main workflow when the promised payment is not received
- Support arrangement amendment, versioning, and review
- Pause an arrangement (e.g. during vulnerability or forbearance period)
- Support Direct Debit setup, coordination, and resubmission workflow boundaries for repayment arrangements; live bank-detail validation and provider API integration are outside Release 1 scope

## Income and Expenditure

- Provide a structured I&E capture form: household income sources, fixed outgoings, variable expenditure, assets
- Use configurable reference data for standard expenditure thresholds by household composition
- Flag significant variances against reference thresholds
- Calculate disposable income using a configurable model (formula, floor, ceiling, reference table; all configurable without code)
- Present repayment options at multiple frequencies calibrated to disposable income
- Present statutory forbearance options (breathing space, time to pay) alongside plan options
- Consider recorded amounts owed to the customer as potential offsets when assessing affordability and repayment options, where configured business rules permit

## Payment Allocation and Reconciliation

- Allocate payments to debt components in a configurable waterfall order (principal, fees, interest)
- Update account balance on payment allocation
- Freeze and unfreeze interest by authorised manual action and automatically suppress interest when an account reaches a configured stage
- Identify overpayments and support role-controlled refund initiation from within DCMS
- Calculate and record third-party commission or fee where applicable
- Create a reconciliation record for each payment
- Handle partial payments and full settlements
- Send recall instructions to third-party providers on debt resolution; lift placement flag; track reconciliation
- On recall from a third-party provider, reinstate the account to the most appropriate internal collection strategy automatically

## Third-Party Provider Management

- Display real-time performance summary per provider: placements, payments received, recovery rate, average time to first payment, outstanding balance
- Enforce mutual exclusivity between internal collection activity and third-party placement

## Write-Off and Data Lifecycle

- Write off residual amounts automatically (e.g. 1p split residual) with a system-generated reason code
- Support authorised manual write-off with approval workflow
- Support configurable archive and purge handling for non-delinquent and written-off accounts
- Support GDPR data anonymisation, archive, and purge controls where required for data lifecycle compliance

## Operational Dashboard and Reporting

- Display a live single-screen operational summary: queue volumes, arrangement health, contact rate, breach count, third-party balances, vulnerability queue depth
- Support drill-down from any metric to a detailed breakdown view with time-series chart
- Link performance anomalies to the strategy change audit trail
- Display strategy performance metrics per segment against a configurable baseline
- Display vulnerability exception reports: overdue reviews, SLA breaches, approaching expiries
- Generate pushed on-screen and email-style SLA alerts when thresholds are approached or breached
- Provide reports identifying accounts not actioned within defined SLA timeframes
- Allow bulk reassignment directly from the dashboard
- Generate structured, branded governance reports from live data; configurable templates, multiple output formats
- Role-filtered views: agent, team leader, operations director; from the same data layer

## Role-Based Access Control and Administration

- Control all access by role; what a user can see and do is determined by their assigned role
- Support at minimum: standard agent, specialist agent, supervisor/team leader, operations director, strategy manager, business administrator
- All access filtering from a single data layer; not separate data sets per role
- Dashboard configuration (KPIs, thresholds, baselines) customisable per role without code changes
- Enforce delegated authority thresholds on manual actions and recommend referral to an appropriately authorised user when the proposed action exceeds the user's authority
- Allow user-role limits for write-off, reversal, credits, arrangement thresholds, and other controlled actions
- Allow admin business users to extend configured data capture with additional fields, attributes, reference data, and controlled dropdown values without code changes

## Audit Trail

- Write a timestamped event to the relevant record for every system action and every user action
- Each event records: acting user or system, action taken, data state before and after, timestamp
- The complete lifecycle of any account, case, strategy, or management action must be reconstructable from the audit trail alone
- Blocked actions must be logged; the audit trail shows that a block occurred, which restriction triggered it, and that nothing was sent
- Audit trail must be accessible to a permissioned user without requiring analyst involvement
