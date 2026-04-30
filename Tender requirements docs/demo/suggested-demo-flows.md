# Suggested Demo Flows for DWP Debt Collection

## Purpose

This document turns the behavioral analysis into practical demonstration journeys for a DWP debt collection tender response. It is written to help a reader understand not just **what** to demo, but **why each demo matters**, **what it proves**, and **what the evaluator should take away**.

The central idea is simple: the strongest demos do not feel like feature tours. They feel like believable operational stories in which the platform:

- recognises a real DWP debt scenario,
- automates the right decisions without waiting for human intervention,
- protects vulnerable or restricted customers at the exact moment it matters,
- leaves a complete audit trail behind it, and
- gives managers confidence that control remains with the business.

## What The Requirements Are Really Asking For

Across the functional requirements, around 20 major behavioral requirements emerge. Those requirements are broad, but they consistently point to four architectural behaviors that matter most in a live demo:

1. **Data integration pipeline**
   The platform must ingest, validate, enrich, and consistently expose customer, debt, payment, and event data from many sources.

2. **Real-time workflow engine**
   The system must route, suppress, escalate, pause, resume, and reassess cases continuously based on rules, events, and outcomes.

3. **Multi-channel communications orchestration**
   The solution must decide when to send letters, SMS, email, dialler tasks, or no communication at all.

4. **Financial accounting and reconciliation core**
   The system must remain trustworthy whenever money moves, plans are breached, balances change, or third parties are involved.

These four behaviors should appear repeatedly across the demo set. If they are only described in slides and not seen in action, the demonstration will likely feel weak.

## How To Use This Document

The recommended approach is to organise the live demonstration around **6 primary flows** and keep **8 supporting flows** ready for evaluator questions, time permitting, or role-specific deep dives.

- The 6 primary flows provide the clearest coverage of the highest-value, highest-weight behaviors.
- The 8 supporting flows fill coverage gaps and help tailor the day for different stakeholder interests such as audit, self-service, compliance, finance, training, or operational resilience.

Each flow below includes:

- a theme,
- a scenario,
- the story it tells,
- why it matters to DWP,
- the main demonstration steps,
- the moment of truth,
- the behavioral requirements it proves.

## Recommended Demonstration Principles

Before the detailed flows, it helps to align on the demonstration style.

### 1. Tell stories, not modules

Avoid presenting the workflow engine, UI, or communications tooling as isolated capabilities. Instead, show them as part of a single journey with cause and effect.

### 2. Show the platform making decisions

Evaluators will learn more from seeing the system automatically:

- suppress contact,
- assign a queue,
- trigger a letter,
- calculate an affordable plan,
- escalate to a specialist, or
- block a non-compliant action

than from hearing that those capabilities exist.

### 3. Make compliance visible

In DWP debt collection, commercial effectiveness and customer protection are not separate concerns. The demo should repeatedly show both:

- assertive recovery where appropriate,
- immediate restraint where required.

### 4. Include management visibility

DWP is not only buying agent tooling. It is also buying operational control, traceability, and confidence. Where possible, each flow should end with a supervisory or audit view.

### 5. Build around “moment of truth” reveals

Every strong flow should include one moment where the system does something that is both easy to understand and hard to fake manually.

Examples:

- all outbound contact stops the moment a vulnerability or dispute flag is set,
- the system calculates affordable options immediately from I&E,
- a breach escalates automatically to third-party collection placement,
- a strategy simulation replays historical outcomes,
- the platform blocks an agent from making a compliance mistake in real time.

---

## The 6 Primary Demo Flows

These six flows should form the core of the demonstration day because they cover the most critical behaviors and best align to the likely scoring emphasis in the DWP agenda.

## Flow 1: From Account Intake to First Meaningful Contact

**Theme:** The system knows what to do before an agent touches it.

**Scenario:** A large overnight feed of new delinquent accounts arrives from the host system. By the time agents log in, accounts have already been validated, matched, segmented, prioritised, and queued for first treatment.

### What story this flow tells

This flow demonstrates that the platform is not a passive case repository. It is an active operational engine that starts work as soon as data arrives.

### Why this matters to DWP

This is the clearest way to prove that high-volume debt recovery can be operationally efficient from day one. It also shows that the solution can absorb scale without relying on manual triage.

### Suggested journey

1. Ingest a batch of new accounts from a host-system API or file feed.
2. Validate incoming records and flag or route any data-quality exceptions.
3. Enrich accounts with bureau/CRA or internal contextual data.
4. Match customers to existing records and detect linked or joint relationships.
5. Segment accounts by balance, risk, product, delinquency stage, or treatment policy.
6. Assign queues automatically by skill, priority, or specialist need.
7. Trigger first communications according to strategy rules.
8. Show the agent login experience with a pre-prioritised worklist and full account context already available.
9. End with a supervisor dashboard showing the entire overnight transformation from raw intake to ready-to-work portfolio.

### Moment of truth

The evaluator sees that hundreds of accounts have already been classified, prioritised, and in some cases contacted before a single agent starts work.

### Behavioral requirements demonstrated

- 2. Data Capture and Integration Pipeline
- 3. Workflow Automation and Override
- 5. Multi-Channel Communication
- 8. Work Queue Distribution
- 9. Decisioning and Strategy Optimisation
- 13. Agent Action Logging and History

---

## Flow 2: Customer in Financial Difficulty - Vulnerability to Resolution

**Theme:** The system protects the right customers while still driving recovery.

**Scenario:** A customer calls while struggling financially and discloses mental health difficulties. The platform must immediately apply protection rules, route the case correctly, support affordability assessment, and create a sustainable plan.

### What story this flow tells

This flow shows that the platform embeds vulnerable-customer treatment into the operational core rather than treating it as a side process.

### Why this matters to DWP

DWP must recover debt while demonstrating strict ethical and compliance discipline. This flow proves that the platform handles that tension safely and efficiently.

### Suggested journey

1. An agent opens the account and sees a contextual screen pop with ID&V prompts, current arrangement status, and any existing warnings.
2. The customer discloses a mental health issue.
3. The agent records the vulnerability marker.
4. The system immediately suppresses outbound communications, removes dialler activity, flags the case, and records a timestamped event.
5. The case is transferred to a specialist, carrying the history, notes, and restrictions with it.
6. The specialist completes an income and expenditure capture.
7. The platform validates and compares the information against available reference or bureau data.
8. The system calculates affordable repayment options and presents suitable frequencies and amounts.
9. The specialist agrees a revised arrangement.
10. The platform creates the arrangement, schedules monitoring, issues confirmation communications, and sets review triggers.
11. The audit trail is shown end to end.

### Moment of truth

The instant the vulnerability marker is saved, all outbound collection activity stops. The evaluator sees customer protection happen automatically, not by agent memory.

### Behavioral requirements demonstrated

- 4. Repayment Arrangement Lifecycle
- 6. I&E Affordability Assessment
- 7. RBAC and Audit
- 12. Exception Detection and Escalation
- 13. Agent Action Logging and History
- 15. Dynamic UI and Agent Scripting
- 18. Vulnerability and Compliance

---

## Flow 3: Arrangement Breach to Escalation to Third-Party Collection Placement

**Theme:** The system escalates without hesitation and with a full paper trail.

**Scenario:** A customer misses consecutive arrangement payments. The platform detects the breach, applies breach rules, attempts re-engagement, and then places the account with a third-party agency when internal recovery is exhausted.

### What story this flow tells

This flow demonstrates the platform’s ability to move from monitoring to escalation without losing control, auditability, or financial accuracy.

### Why this matters to DWP

This is a strong proof point for automated treatment progression, communications sequencing, third-party management, and accounting integrity.

### Suggested journey

1. A scheduled payment fails or is not received by the due date.
2. The platform detects the missed payment and applies tolerance rules.
3. The account is confirmed as breached.
4. A breach workflow automatically begins with sequenced communications and contact attempts.
5. The system records every success, failure, and elapsed time milestone.
6. After non-response, the case is promoted to a pre-placement strategy.
7. An agent or supervisor sees the exception queue and chooses whether to override.
8. If rules are met, the system places the account with a configured third-party collection provider.
9. Placement data is sent and acknowledged.
10. Internal contact is suppressed so no competing treatment continues in parallel.
11. A payment later returns from the agency, and the platform allocates it correctly and updates balance and commission tracking.
12. A supervisor reviews third-party collection performance reporting.

### Moment of truth

The full elapsed timeline from missed payment to third-party collection placement is visible, with every automated step and rule-based decision preserved.

### Behavioral requirements demonstrated

- 3. Workflow Automation and Override
- 4. Repayment Arrangement Lifecycle
- 5. Multi-Channel Communication
- 10. Third-Party Placement and Reconciliation
- 11. Financial Accounting and Reconciliation
- 12. Exception Detection and Escalation
- 20. Parallel Process Management

---

## Flow 4: Complex Household - Multiple Accounts, Joint Liability, Competing Workflows

**Theme:** No matter how complex the customer, the system holds it together.

**Scenario:** One household includes multiple people, multiple debts, joint liability, an active repayment plan, legal activity, and a breathing space restriction. The system must maintain a coherent customer and compliance view across all of them.

### What story this flow tells

This flow proves that the data model and workflow engine can handle complex real-world debt relationships without creating duplicate contact, conflicting actions, or fragmented visibility.

### Why this matters to DWP

The household and joint-liability requirements are among the more distinctive and operationally sensitive DWP needs. Showing them well can be a differentiator.

### Suggested journey

1. Search for a customer and open a unified customer or household view.
2. Display multiple linked accounts with different statuses and workflows.
3. Show one account on a repayment plan, another in legal treatment, and another affected by a linked-party breathing space restriction.
4. Attempt a manual action such as a call or communication.
5. The platform warns the agent about account-specific restrictions and asks them to confirm the intended scope of the action.
6. The agent proceeds only on the valid account.
7. The action is recorded against the correct account without affecting restricted ones.
8. Show that separate workflows continue safely in parallel with no duplicate letters or invalid contact.

### Moment of truth

The system prevents a likely compliance mistake in real time by identifying that a restriction on one linked debt should not contaminate or confuse work on another.

### Behavioral requirements demonstrated

- 1. Relationship Management
- 3. Workflow Automation and Override
- 5. Multi-Channel Communication
- 7. RBAC and Audit
- 12. Exception Detection and Escalation
- 18. Vulnerability and Compliance
- 20. Parallel Process Management

---

## Flow 5: Business User Changes a Collection Strategy - No IT Required

**Theme:** Business users own strategy change. IT is not in the loop.

**Scenario:** A strategy manager wants to improve early-stage recovery by inserting an extra SMS contact step and testing whether it works before full rollout.

### What story this flow tells

This is the clearest flow for proving that the platform is configurable by the business, safely governed, and capable of continuous performance optimisation.

### Why this matters to DWP

The requirements strongly emphasise no-code or low-code change, simulation, champion/challenger testing, peer review, versioning, and rollback. This is one of the most commercially differentiating areas.

### Suggested journey

1. Open the current strategy in a visual or understandable workflow design surface.
2. Add a new SMS step at a chosen point in the early delinquency journey.
3. Configure message conditions and suppression logic.
4. Save the change as a draft version rather than publishing directly.
5. Trigger peer review and approval.
6. Run a historical simulation or replay against past data.
7. Show projected outcome differences between old and proposed strategy.
8. Create a champion/challenger split for new accounts.
9. Display ongoing comparative results after a defined period.
10. Promote the better strategy to full use.
11. Show version history and rollback controls.

### Moment of truth

The evaluator sees the system answer the question: “What would have happened if this strategy had been live already?” That is far more persuasive than simply showing a flowchart editor.

### Behavioral requirements demonstrated

- 3. Workflow Automation and Override
- 9. Decisioning and Strategy Optimisation
- 16. Controlled Change Management
- 19. Roadmap and Upgrades

---

## Flow 6: Executive and Operational Dashboard - Real-Time Oversight

**Theme:** Leadership sees the operation in real time. Nothing is hidden.

**Scenario:** An operations director wants to understand queue health, breach trends, strategy performance, SLA risk, and vulnerable-case handling without asking analysts to prepare a pack.

### What story this flow tells

This flow demonstrates that the platform is not only automating work, but also making that work governable, measurable, and improvable.

### Why this matters to DWP

Senior stakeholders need confidence that the system supports operational management, assurance, and intervention in real time.

### Suggested journey

1. Open a top-level operational dashboard.
2. Show queue volumes, contact rates, arrangement health, breaches, and active placements.
3. Drill into an emerging breach trend by product or segment.
4. Trace the trend back to the active treatment strategy.
5. Review queue ageing and identify under-served work.
6. Trigger a supervisor intervention or bulk reassignment.
7. Show champion/challenger reporting linked to the strategy changes in Flow 5.
8. Review an exception report for vulnerable cases or unassigned specialist work.
9. Export or present a management or governance report.

### Moment of truth

The viewer sees a senior user identify a problem, trace the cause, and take corrective action inside the same system and within minutes.

### Behavioral requirements demonstrated

- 3. Workflow Automation and Override
- 7. RBAC and Audit
- 8. Work Queue Distribution
- 9. Decisioning and Strategy Optimisation
- 11. Financial Accounting and Reconciliation
- 12. Exception Detection and Escalation
- 13. Agent Action Logging and History

---

## The 8 Supporting Demo Flows

These supporting flows are useful for filling coverage gaps, tailoring the demo to specific evaluators, or handling follow-up questions in a way that still feels scenario-driven.

## Flow 7: Customer Self-Service - Portal to Agent Handoff

**Theme:** The customer does the work and the system connects it seamlessly.

This flow proves that a customer can authenticate, view their position, update details, complete I&E, and agree a plan through self-service, with the outcome reflected in agent queues immediately. It is especially important for demonstrating requirement 14 around self-service and API-first integration.

**Moment of truth:** The account never appears as agent work because the customer resolves it themselves end to end.

**Behavioral requirements demonstrated:** 4, 6, 13, 14

---

## Flow 8: Debt Dispute - Freeze, Investigate, Resolve

**Theme:** When a customer disputes, the system protects everyone.

This flow shows that raising a dispute instantly freezes all automated collection activity, creates an investigation path, applies SLA tracking, and preserves a full evidential thread while finance and balance corrections remain controlled.

**Moment of truth:** The dispute flag is saved and every active collection action stops at once.

**Behavioral requirements demonstrated:** 3, 7, 11, 12, 13

---

## Flow 9: Deceased Customer - Compliance Protocol in Motion

**Theme:** The most sensitive scenario, handled without a single misstep.

This flow demonstrates the strictest contact controls. Once a deceased marker is recorded, the platform suppresses collection activity, moves the case into an estate-oriented process, and blocks agents from issuing standard collection actions.

**Moment of truth:** An attempted collection action is actively blocked, with the system explaining what is allowed instead.

**Behavioral requirements demonstrated:** 3, 5, 12, 15, 18

---

## Flow 10: Write-Off, Recovery and Reactivation

**Theme:** Closing a case is never truly the end.

This flow is useful for showing authority-based approval, accounting postings, archival states, and the ability to reactivate a previously written-off account when money unexpectedly arrives later.

**Moment of truth:** An unexpected payment on a written-off account automatically creates a reactivation workflow instead of being lost in back-office reconciliation.

**Behavioral requirements demonstrated:** 3, 7, 10, 11, 12

---

## Flow 11: New Agent - From Login to First Resolved Case

**Theme:** The system makes a new agent effective from day one.

This flow is particularly strong for user experience and control. It shows queue assignment by skill, guided scripting, mandatory capture, policy-based blocking, and supervisor oversight for coaching and safety.

**Moment of truth:** The platform prevents a junior agent from setting an inappropriate arrangement and guides them toward the compliant route.

**Behavioral requirements demonstrated:** 7, 8, 12, 13, 15, 18

---

## Flow 12: Regulatory Audit - Full Traceability on Demand

**Theme:** Any action, any account, any time, explained in full.

This is the best flow for auditors, governance, and risk stakeholders. It demonstrates chronological event history, rule version traceability, evidence export, and root-cause analysis without needing to reconstruct events manually.

**Moment of truth:** A suspected compliance breach is explained in minutes using structured evidence already stored in the system.

**Behavioral requirements demonstrated:** 7, 12, 13, 18

---

## Flow 13: Month-End Surge - Operations at Scale

**Theme:** The system does not blink when volumes spike.

This flow demonstrates scale, throughput, and queue self-organisation. It is useful for showing that batch processing, prioritisation, communications, and supervisory balancing can all operate under peak demand.

**Moment of truth:** Thousands of accounts are categorised and assigned before agents begin work, with live queue balancing visible to supervisors.

**Behavioral requirements demonstrated:** 2, 3, 5, 8, 9, 11

---

## Flow 14: Lump Sum Settlement Offer - Agent-Negotiated, System-Governed

**Theme:** Every agent negotiates consistently and authority limits are enforced automatically.

This flow demonstrates settlement governance, approval hierarchy, accounting treatment, and auditability. It is especially useful when evaluators want to understand delegated authority and financial control.

**Moment of truth:** The platform blocks the settlement until approval arrives, then completes payment allocation, write-off, correspondence, and closure in one governed sequence.

**Behavioral requirements demonstrated:** 4, 7, 10, 11, 13

---

## Coverage Summary

Taken together, the 14 flows provide broad coverage across the 20 major behavioral requirements.

### Strongest-covered areas

- Workflow automation and manual override
- Vulnerability, compliance, and exception handling
- Repayment lifecycle and affordability
- Work allocation and supervisory oversight
- Auditability and action history
- Decisioning and strategy optimisation
- Financial accounting and balance control

### Coverage gaps to watch

The only major requirement area not naturally foregrounded in the flows above is:

- **17. Legacy data migration from legacy systems with verification and suppression**

This does not lend itself as naturally to a dramatic customer journey, but it is still important. If DWP is expected to score this materially during demonstration or Q&A, it may be worth preparing a short technical walkthrough or mini-flow showing:

1. staged migration intake,
2. duplicate action suppression,
3. migration validation and failure handling,
4. coexistence or fallback position during transition.

## Suggested Mapping To The Demonstration Agenda

A practical way to use the flows against the DWP agenda is:

### Demonstration 1 - Integration

Use:

- Flow 1 as the anchor
- Flow 7 as supporting proof of API-first and self-service integration
- selected elements of Flow 3 for third-party placement and payment integration

### Demonstration 2 - System Capability

Use:

- Flow 2
- Flow 3
- Flow 6
- supporting extracts from Flow 11 and Flow 14

### Demonstration 3 - System Flexibility

Use:

- Flow 4
- Flow 5
- supporting extracts from Flow 9

### Demonstration 4 - Deployment Facilitation

Use:

- the governance and controlled-change parts of Flow 5
- supplementary explanation on versioning, rollback, peer review, selective rollout, and simulation

## Recommended Demo Set If Time Is Tight

If the team cannot credibly perform all 14 flows live, the following shortlist offers the best balance of coverage and impact:

1. Flow 1 - Intake to first contact
2. Flow 2 - Vulnerability to resolution
3. Flow 3 - Breach to third-party collection placement
4. Flow 4 - Complex household and competing workflows
5. Flow 5 - Strategy change without IT
6. Flow 6 - Executive and operational oversight
7. Flow 7 - Self-service to agent handoff
8. Flow 12 - Regulatory audit traceability

This set covers the most visible operational, compliance, and strategic control themes while still giving room for audience-specific follow-up.

## Final Recommendation

If the demonstration needs a single unifying message, it should be this:

> The platform does not simply store debt cases. It continuously interprets data, decides the next best action, protects customers when circumstances require it, controls agents through policy and permissions, and exposes every action for operational and regulatory scrutiny.

That message aligns strongly with the major behavioral requirements and reflects the core tension within the DWP brief: **effective debt recovery delivered within strict compliance, vulnerability, and audit constraints**.
