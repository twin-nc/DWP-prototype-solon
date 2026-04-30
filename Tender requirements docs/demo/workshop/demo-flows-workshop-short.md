# DWP Debt Collection — Demo Flows 1–6
## Workshop Summary Guide

> This document provides a concise reference for each of the six primary demonstration flows. It is designed for rapid orientation before a rehearsal or the live demonstration day. For full presenter notes, step-by-step detail, and evaluator Q&A preparation, see the detailed guide.

---

## Flow 1: From Account Intake to First Meaningful Contact

**Theme:** The platform knows what to do before an agent touches a single account.

**Scenario:** A large overnight feed of new delinquent accounts arrives. By the time agents log in, every account has already been validated, enriched, matched, segmented, prioritised, and — in many cases — contacted.

### Key Steps

1. Trigger a batch intake from a host-system file or API feed.
2. Show validation: clean records proceed; exceptions are quarantined with reason codes.
3. Show customer matching: incoming accounts linked to existing records where found.
4. Show enrichment: benefit status, risk band, delinquency stage, and priority score applied automatically.
5. Show segmentation and queue assignment: accounts distributed across teams by rule.
6. Show first communications already triggered for eligible accounts.
7. Log in as an agent: worklist is pre-populated and pre-prioritised — no manual triage needed.
8. Open the supervisor dashboard: the overnight transformation from raw intake to ready-to-work portfolio is visible at a glance.

### Moment of Truth

Hundreds of accounts have been classified, queued, and in some cases contacted before a single agent begins work. State the number explicitly.

### Requirements Evidenced

Data integration (batch and API), workflow automation, multi-channel communications, work queue distribution, decisioning and segmentation, agent action logging.

---

## Flow 2: Customer in Financial Difficulty — Vulnerability to Resolution

**Theme:** The platform protects the right customers immediately and automatically.

**Scenario:** A customer calls and discloses mental health difficulties. The platform must instantly apply protection rules, route the case to a specialist, support an affordability conversation, and create a sustainable repayment plan.

### Key Steps

1. Agent opens the account: full context visible — balance, history, prior contacts, current status.
2. Customer discloses a vulnerability; agent records the type, severity, and whether it is temporary or ongoing.
3. **On save: all outbound contact is automatically suppressed** — queued letters, SMS, and dialler tasks are stopped.
4. Case is transferred to the specialist vulnerability queue, carrying the full history.
5. Specialist completes an income and expenditure capture; platform validates against reference thresholds.
6. Platform calculates disposable income and presents affordable repayment options across frequencies.
7. Specialist agrees and creates an arrangement; monitoring workflow starts automatically.
8. Show the end-to-end audit trail: every event timestamped from first contact to arrangement creation.

### Moment of Truth

The instant the vulnerability marker is saved, all outbound collection activity stops — automatically, before any supervisor or specialist has acted. Protection is a system behaviour, not a training task.

### Requirements Evidenced

Vulnerability management with levels, type, and temporary status; affordability and I&E capture; repayment arrangement lifecycle; compliance and audit; restricted user actions; specialist queue routing.

---

## Flow 3: Arrangement Breach — Escalation to Third-Party Collection Placement

**Theme:** The platform escalates without hesitation and leaves a complete paper trail.

**Scenario:** A customer misses consecutive arrangement payments. The system detects the breach, sequences re-engagement communications, and — when internal recovery is exhausted — places the account with a third-party collection provider.

### Key Steps

1. Payment due date passes with no payment received; grace period timer activates.
2. Grace period expires: breach confirmed, first breach communication generated automatically.
3. Platform sequences contact attempts — letters, SMS, outbound call tasks — as configured in the strategy.
4. After the configured re-engagement window, case escalates to a supervisor exception queue.
5. Supervisor reviews: option to override and retain internally, or allow placement to proceed.
6. Placement fires: account data transmitted to the third-party collection provider; internal contact strategy suspended simultaneously.
7. The third-party collection provider returns a payment; platform allocates it correctly — principal, fees, commission — and updates the balance.
8. third-party collection performance reporting shows placements made, payments received, and recovery rate.

### Moment of Truth

The full timeline from missed payment to third-party collection placement is visible in one view — every automated step, every contact attempt, every elapsed time marker — written in real time by the system as events occurred.

### Requirements Evidenced

Repayment arrangement monitoring, breach tolerance rules, sequenced multi-channel communications, third-party placement and reconciliation, financial accounting, exception escalation, supervisor override with audit.

---

## Flow 4: Complex Household — Multiple Accounts, Joint Liability, Competing Workflows

**Theme:** No matter how complex the customer relationship, the platform holds it together and prevents compliance mistakes in real time.

**Scenario:** One household contains two individuals, three debts across them, an active repayment plan, a case in legal treatment, and a breathing space restriction. The platform must maintain a coherent view and enforce the right restrictions.

### Key Steps

1. Search for a customer; navigate from the individual record to the household view.
2. Show the household entity: both individuals, all three debts, status icons for each.
3. Show the joint debt — two named parties, communications addressable to either or both simultaneously.
4. Attempt an outbound action on the breathing-space-restricted account.
5. **Platform blocks the action and explains why** — the restriction is enforced at runtime, not by agent memory.
6. Agent works one of the unrestricted accounts successfully; action recorded only against that account.
7. Show the two parallel workflows on Person A's accounts running independently without interference.
8. Show the supervisor household summary: all accounts and restrictions visible at a glance.

### Moment of Truth

The platform prevents a live compliance mistake. An attempted contact on a restricted account is stopped before it reaches the customer — the evaluator sees protection enforced by the system, not by procedure.

### Requirements Evidenced

Household creation and display, joint debt with multi-party liability, joint debt split with automatic residual write-off, breathing space and restriction enforcement, parallel process management, RBAC and audit.

---

## Flow 5: Business User Changes a Collection Strategy — No IT Required

**Theme:** Business users own strategy change. The platform governs it safely without requiring IT involvement.

**Scenario:** A strategy manager wants to improve early-stage recovery by inserting a new SMS step, testing whether it works before full rollout, and then deploying the better-performing version.

### Key Steps

1. Open the existing strategy in the visual strategy editor — no code visible.
2. Add a new SMS step at the chosen point; select the template from the third-party communications platform.
3. Save the change as a draft version; the live strategy continues to run unchanged.
4. Submit for peer review and approval; reviewer approves via the configured workflow.
5. Run a historical simulation: "What would have happened if this strategy had been live for the last three months?" — projected contact and arrangement rates shown.
6. Configure a champion/challenger split: 80% on the existing strategy, 20% on the new one.
7. After the evaluation period, view comparative results side by side.
8. Promote the better strategy to full use; show version history and the one-click rollback control.

### Moment of Truth

The platform answers: "What would have happened if this strategy had already been live?" The simulation step transforms a workflow editor into a strategy optimisation tool.

### Requirements Evidenced

No-code strategy design and amendment, simulation and scenario modelling, champion/challenger testing, communications template integration, version control, peer review and approval, rollback controls.

---

## Flow 6: Executive and Operational Dashboard — Real-Time Oversight

**Theme:** Senior leadership sees the operation clearly, acts when needed, and never needs an analyst to prepare a report first.

**Scenario:** An operations director wants to understand queue health, breach trends, strategy performance, SLA risk, and vulnerable-case handling — in real time, from a single screen.

### Key Steps

1. Open the operational dashboard: queue volumes, arrangement health, breach counts, third-party collection placements, and vulnerability case depth — all live.
2. Drill into an amber breach trend for a specific segment; view the count by day over 30 days.
3. Trace the trend to its cause: a strategy configuration change deployed 48 hours ago correlates with the performance drop.
4. Review queue ageing; identify accounts over SLA in the specialist review queue.
5. Bulk-reassign ageing accounts to a team with available capacity; action logged against the director's user ID.
6. Open champion/challenger results from Flow 5; the challenger is outperforming — statistical significance confirmed.
7. Review the vulnerability exception report: overdue specialist reviews and flags approaching expiry.
8. Export a governance report — period, metrics, and format selectable — ready for a board pack or regulatory submission.

### Moment of Truth

A senior user identifies a performance problem, traces its cause to a specific change, and takes corrective action — inside the same platform, in under five minutes, without asking anyone for data.

### Requirements Evidenced

Real-time operational reporting, KPI and SLA configuration, work allocation and bulk reassignment, champion/challenger performance visibility, vulnerability exception management, Power BI–compatible data exposure, full audit of management actions.

---

## Coverage Summary

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
| Financial Accounting | | | ✓ | | | |
| Exception Detection and Escalation | ✓ | ✓✓ | ✓ | ✓ | | ✓ |
| Agent Action Logging and History | ✓ | ✓✓ | ✓ | ✓ | | ✓ |
| Vulnerability and Compliance | | ✓✓ | | ✓✓ | | ✓ |
| Household and Joint Liability | | | | ✓✓ | | |
| Parallel Process Management | | | ✓ | ✓✓ | | |
| Strategy Change Without IT | | | | | ✓✓ | |
| Champion/Challenger Testing | | | | | ✓✓ | ✓ |
| Operational Reporting and MI | ✓ | | | | | ✓✓ |

*✓✓ = primary evidence point; ✓ = supporting evidence*
