# Three-Workspace Model

Source architecture: [configuration-layer-architecture.drawio](./configuration-layer-architecture.drawio)

## Purpose

This document defines the three-workspace model for the Business Control & Experience Layer. It is the primary reference for product scope, role separation, demo design, and delivery planning.

## Overview

The Business Control & Experience Layer presents three distinct role-based workspaces to business users. All three are delivered within a shared product shell that provides common authentication, navigation, notifications, and user context.

| Workspace | Primary users | Architectural position |
|---|---|---|
| Case Worker Workspace | Frontline agents, specialist handlers | Runtime experience |
| Operations Workspace | Supervisors, team leaders, MI analysts, operations managers | Runtime oversight experience |
| Configuration Workspace | Business developers, configurators, analysts, selected admins | Configuration and control-plane experience |

Solon Tax remains the execution core and system of record beneath all three. No workspace replaces Solon Tax. Each workspace composes, governs, and presents Solon Tax capabilities through a coherent product surface.

---

## Design Principles

**1. Role-based separation by workspace, not by product.**
Users are directed to the appropriate workspace by RBAC. The separation is enforced by access control within a shared shell, not by deploying three separate products.

**2. Configuration does not contaminate runtime.**
Frontline case workers have no navigational access to configuration tooling. Operations users have constrained access to selected configuration-adjacent views only where explicitly authorised.

**3. The shared shell is non-optional.**
Cross-workspace journeys — a supervisor drilling from an Operations dashboard into a specific case; a strategy manager moving from Configuration to Operations to observe live impact — require shared session state, shared navigation, and shared notifications. These cannot be achieved without a first-class shared shell.

**4. The BFF owns data composition.**
No workspace calls Solon Tax directly from the browser. A Backend-for-Frontend composition service assembles per-workspace responses from Solon Tax APIs and layer Core Services, applying auth-scoped data filtering per role before returning data to the client.

**5. Solon Tax is the system of record.**
All data changes made in any workspace are written through Solon Tax REST APIs or layer Core Services. No workspace bypasses the API surface.

---

## Case Worker Workspace

### Purpose

The frontline runtime workspace for agents handling live debt collection cases. Designed for speed, guided compliance, and low cognitive load under call conditions.

### Primary users

- Frontline case workers
- Specialist handlers (vulnerability, fraud, disputes, deceased)
- Junior agents working under scripting guardrails

### What users can do

- View account and case detail: timeline, debt balance, status, linked accounts and restrictions
- Complete identity verification steps
- View and act on vulnerability and restriction flags
- Follow guided scripting for DWP-specific journey types
- Capture income and expenditure and receive affordable arrangement options
- Create, amend, and record repayment arrangements
- Record permitted communications
- Record notes and audit-visible case actions
- Transfer cases to specialist queues with full context carried forward

### What users cannot do

- Edit strategy, task definitions, or policy configuration
- Access Foundations Configurator, codelist management, or RBAC administration
- Modify queue routing rules or SLA thresholds
- View champion/challenger strategy performance data
- Access MI and analytics dashboards
- Perform bulk reassignments or queue balancing operations

### Data sources

| Source | Data provided |
|---|---|
| Solon Tax — Case Management | Case detail, case type, debt balance, eligible workers |
| Solon Tax — Human Task Management | Open tasks, task completion, worker profiles |
| Solon Tax — Suppression Management | Active suppressions, vulnerability and breathing space status |
| Solon Tax — Taxpayer Accounting | Liabilities, payment plans, payment events |
| Solon Tax — Registration | Party and party role data, linked accounts, claimants |
| Solon Tax — Contact Management | Contact records, templates |
| Work, Queue & Agent Core Service | Queue context, guided scripting steps, work item assignment |
| Risk, I&E & Compliance Core Service | Vulnerability enforcement decisions, I&E capture, affordability results |
| Contact & Communications Core Service | Contact history, suppression status |
| Operational DB | Contact history, SLA state, I&E records, work item history |

### Key screens

- Account and case view (timeline, debt, status, linked accounts)
- ID&V panel
- Vulnerability and restriction flags panel
- Guided scripting surface (step-by-step, blocking on policy violations)
- Arrangement capture form (I&E-driven options)
- Permitted actions panel
- Notes and audit-visible action recording

### Demo flows primarily served

| Flow | Role of this workspace |
|---|---|
| 2 — Vulnerability to resolution | Primary — all agent steps |
| 3 — Breach to DCA | Primary — agent steps before supervisor exception queue |
| 4 — Complex household | Primary |
| 7 — Self-service to agent handoff | Primary — agent receives resolved case |
| 8 — Dispute | Primary |
| 9 — Deceased | Primary |
| 11 — New agent | Primary — scripting and blocking shown here |
| 14 — Settlement offer | Primary — agent negotiation and blocking |
| 1 — Intake to first contact | Supporting — agent login and worklist |

---

## Operations Workspace

### Purpose

The supervisory and analytical workspace for team leaders, operations managers, and MI users. Designed for visibility, intervention, trend detection, and operational control across the portfolio.

### Primary users

- Supervisors and team leaders
- Operations managers
- MI and analytics users
- DCA relationship managers
- Compliance and audit reviewers

### What users can do

- View queue volumes, SLA health, breach trends, and campaign KPIs on live dashboards
- Drill into specific queues, agents, or accounts from aggregate views
- Perform bulk reassignment and queue balancing
- Trigger supervisory interventions on individual cases
- Manage exception and specialist work queues
- View champion/challenger strategy performance and comparative results
- Review DCA placement status and performance
- Access MI and analytics for strategy, channel, and operational performance
- Export governance and management reports
- View audit history and compliance evidence for regulatory review
- Manage self-service portal extension configuration and monitoring

### What users cannot do

- Edit core strategy, process definitions, or BPMN deployments
- Create or modify task definitions or Drools rules
- Change foundational codelists or RBAC configuration without specific authorisation
- Perform standard frontline case handling actions (case-level actions should remain in the Case Worker Workspace)

### Data sources

| Source | Data provided |
|---|---|
| Strategy, Analytics & DCA Core Service | Champion/challenger results, KPI metrics, DCA placement and reconciliation data |
| Work, Queue & Agent Core Service | Real-time queue state, SLA timers, work item history, agent workloads |
| Risk, I&E & Compliance Core Service | Vulnerability review scheduling, breathing space log |
| Solon Tax — Operational Plan Management | Campaign KPIs, objectives, weighted targets, completion rates |
| Solon Tax — Case Management | Case-level drill-down data |
| Analytics & DCA DB | Historical KPI metrics, C/C results, DCA records and commission ledger |
| Operational DB | Queue state, SLA timers, work item history |
| Audit & Compliance DB | Breathing space log, I&E history, fraud records |

### Key screens

- Top-level operational dashboard (queue volumes, SLA, breach trends, arrangement health, active placements)
- Queue drill-down and balancing view
- Supervisor intervention and bulk reassignment interface
- Exception and specialist work queue management
- Champion/challenger performance and promotion interface
- DCA performance and reconciliation reporting
- Audit and compliance evidence viewer
- MI and analytics report suite

### Demo flows primarily served

| Flow | Role of this workspace |
|---|---|
| 6 — Executive dashboard | Primary |
| 12 — Regulatory audit | Primary |
| 13 — Month-end surge | Primary |
| 1 — Intake to first contact | Primary — supervisor dashboard at end-state |
| 3 — Breach to DCA | Supporting — supervisor exception queue and DCA reporting |
| 5 — Strategy change | Supporting — live impact visible here after deployment |
| 10 — Write-off and reactivation | Supporting — supervisor approval |
| 11 — New agent | Supporting — supervisor coaching and oversight |
| 14 — Settlement offer | Supporting — approval workflow |

---

## Configuration Workspace

### Purpose

The strategy, task, policy, and foundations configuration workspace for business developers, configurators, and analysts. Designed for safe, governed, peer-reviewed change management. No IT involvement required for routine changes.

### Primary users

- Business developers and strategy managers
- Configurators and analysts
- Foundations administrators (codelists, RBAC, reference data)

### What users can do

- Design and assemble collection strategies on a visual canvas
- Edit, version, and publish task definitions and step libraries
- Author and test Drools rules through the rule builder UI
- Design task forms defining the agent view for each task type
- Configure and package policy bundles (vulnerability, fraud, repayment, breathing space)
- Manage foundations configuration: codelists, RBAC, users
- Run simulations replaying historical journeys against proposed strategies
- Set up and review champion/challenger test configurations
- Approve and deploy strategy changes through a governed pipeline with peer review
- View version history and perform rollbacks

### What users cannot do

- Perform day-to-day operational case handling
- Access live customer data outside of simulation and testing contexts
- Deploy configuration changes directly to production without the review and approval workflow
- Modify Solon Tax internal configuration directly

### Data sources

| Source | Data provided |
|---|---|
| Strategy Engine | Step library, strategy authoring and compilation, deployment pipeline, simulation engine |
| Task Configurator | Task definitions, rule builder, task form designer |
| Policy Bundles | Vulnerability, fraud, repayment, breathing space configuration |
| Foundations Configurator | Solon Reference Data API (codelists), Keycloak (RBAC, users) |
| Strategy & Config DB | Strategies, step library, templates, deployment records |
| Solon Tax — Drools Runtime | Rule execution for testing and validation |
| Solon Tax — Process Management | BPMN deployment target for compiled strategies |

### Key screens

- Strategy assembler canvas
- Deployment pipeline and peer review and approval workflow
- Simulation engine and outcome comparison view
- Champion/challenger setup and results
- Step library and task definition editor
- Drools rule builder
- Task form designer
- Policy bundle packaging interface
- Foundations configurator (codelists, RBAC, users)
- Strategy version history and rollback

### Internal structure

The Configuration Workspace internal structure is described separately in [configuration-architecture.drawio](./configuration-architecture.drawio). It follows a nested model:

- **Process Management** — strategy-level configuration (nestable, process of processes)
- **Tasks & Rules** — task and rule configuration (nestable, task of tasks)
- **Foundations** — reference data, codelists, RBAC (nested inside Tasks & Rules)

Three cross-cutting policy bundles — Vulnerability, Repayment Plan, and Fraud — span all three layers.

### Demo flows primarily served

| Flow | Role of this workspace |
|---|---|
| 5 — Strategy change without IT | Primary |
| 6 — Executive dashboard | Supporting — strategy version and C/C setup link |
| 12 — Regulatory audit | Supporting — version history and deployment records |

---

## Shared Product Shell

### What the shell owns

| Concern | Description |
|---|---|
| Authentication | Single Keycloak-backed login session, valid across all workspaces |
| Navigation | Role-based workspace routing; users land in their primary workspace; RBAC determines which workspace tabs are visible |
| Notifications | Cross-workspace event surface — breach alerts, deployment confirmations, and SLA breaches surface regardless of which workspace the user is in |
| User context | Current user identity, role, team, and active workspace state, shared across the shell |
| Session lifecycle | Single logout, session timeout, token refresh |

### Why the shell is non-optional

Cross-workspace journeys exist in normal operations and in the demo flows:

- A supervisor monitoring an Operations dashboard clicks through to a specific case being handled by a specific agent. That navigation crosses from Operations Workspace into Case Worker Workspace and requires a shared session and shared routing.
- A strategy manager deploys a new strategy in the Configuration Workspace and immediately navigates to the Operations Workspace to observe live impact on queue volumes. That is a single user workflow across two workspaces.
- A breach alert generated by the Risk, I&E & Compliance Core Service must surface as a notification regardless of which workspace the supervisor currently has open. That requires a shared notification subscription and surface.

Marking the shell as optional guarantees it will not be built under delivery pressure. The result is three inconsistent products with broken cross-workspace journeys and three separate authentication contexts.

### Implementation options

| Approach | Suited for | Trade-off |
|---|---|---|
| Single SPA with workspace routing | Tender demo, initial delivery, small team | Simpler to build; shared bundle means workspace updates redeploy the whole shell |
| Micro-frontend shell | Production with separate delivery teams per workspace | Full isolation; independent deployability per workspace; more infrastructure upfront |
| Portal launcher with separate SPAs | Do not use | No shared session or notifications; cross-workspace journeys break at workspace boundaries |

For the tender demo a single SPA with workspace routing is the pragmatic choice. For production with separate delivery teams, a micro-frontend shell is the stronger long-term architecture.

---

## Role and Access Model

| Role | Case Worker Workspace | Operations Workspace | Configuration Workspace |
|---|---|---|---|
| Frontline case worker | Full access | No access | No access |
| Specialist handler | Full access | No access | No access |
| Supervisor / team leader | Read-only case view | Full access | No access |
| Operations manager | No access | Full access | No access |
| MI / analytics user | No access | Analytics and reporting only | No access |
| Strategy manager | No access | C/C results view | Strategy and simulation |
| Business developer / configurator | No access | No access | Full access |
| Foundations admin | No access | No access | Foundations Configurator only |
| Compliance / audit reviewer | No access | Audit and evidence viewer | Version history, read-only |

Access is enforced by the shared shell via Keycloak role claims. No user navigates to a workspace outside their RBAC grant. The shell routes users to their primary workspace on login and suppresses navigation tabs for inaccessible workspaces.

---

## Complete Demo Flow Mapping

| Flow | Primary workspace | Supporting workspace |
|---|---|---|
| 1 — Intake to first contact | Operations (supervisor end-state) | Case Worker (agent worklist and login) |
| 2 — Vulnerability to resolution | Case Worker | Operations (audit trail view) |
| 3 — Breach to DCA placement | Case Worker + Operations | — |
| 4 — Complex household | Case Worker | — |
| 5 — Strategy change without IT | Configuration | Operations (live impact observation) |
| 6 — Executive dashboard | Operations | Configuration (strategy version and C/C link) |
| 7 — Self-service to agent handoff | Case Worker | — |
| 8 — Dispute | Case Worker | — |
| 9 — Deceased | Case Worker | — |
| 10 — Write-off and reactivation | Case Worker | Operations (supervisor approval) |
| 11 — New agent | Case Worker | Operations (supervisor coaching view) |
| 12 — Regulatory audit | Operations | Configuration (deployment and version records) |
| 13 — Month-end surge | Operations | Case Worker (queue and worklist view) |
| 14 — Settlement offer | Case Worker | Operations (delegated authority approval) |
