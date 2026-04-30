# DCMS Leadership Presentation — Outline

**Audience:** Company leadership
**Tone:** Strategic confidence — decisions are made, risk is managed, delivery is scoped
**Target length:** ~20 slides / 45–60 min including Q&A
**Appendix:** 9 feature deep-dive slides, not presented unless asked during Q&A

---

## Section 1 — What We Are Building (Slides 1–3)

### Slide 1 — Mission Statement
- One sentence: what DCMS does, who it serves, why it matters
- Explicit call-out: greenfield build — not a configuration of an existing platform, not COTS
- Supporting doc needed: none — distilled from MASTER-DESIGN-DOCUMENT.md

### Slide 2 — Scope in One Picture
- Single lifecycle diagram: Identification → Assessment → Recovery → Enforcement → Write-Off
- 19 capability groups mapped to lifecycle stages
- Key number: 442 functional requirements, 19 groups, all confirmed
- Supporting doc needed: **DIAGRAM — debt lifecycle with capability group overlay**

### Slide 3 — What Makes This Different
Three differentiators:
1. Regulatory hardcoding — compliance is law, not configuration (breathing space, insolvency, deceased)
2. Business-user strategy authoring — no IT ticket required to change decision rules
3. Full audit-to-question trail — any regulatory query answered from within the system in under 10 minutes
- Supporting doc needed: none — distilled from existing ADRs and design docs

---

## Section 2 — Architecture Overview (Slides 4–6)

### Slide 4 — System Architecture in One Diagram
- Single monolith, two services (backend + frontend), Keycloak, PostgreSQL
- Domain package wheel: 13 domain packages, process engine at centre
- Key message: monolith is intentional — right-sized for a 6–7 person team
- Supporting doc needed: **DIAGRAM — system architecture (monolith + services + infra)**

### Slide 5 — The Process Engine Decision
- Flowable BPMN/DMN as the process engine
- One Flowable process instance per DebtAccount — the debt lifecycle *is* the process
- Delegate command pattern: domain code never imports Flowable directly (architectural isolation boundary)
- BPMN event subprocesses for regulatory events — when breathing space, deceased, or insolvency fires, the engine preserves lifecycle continuity while suppression/suspension handling prevents prohibited collection activity.
- Supporting doc needed: **DIAGRAM — delegate command pattern (domain → port → process engine)**

### Slide 6 — Key Architecture Decisions Table
One-page table: 5–6 most consequential ADRs

| Decision | Rationale |
|---|---|
| Single monolith | Right-sized for 6–7 person team; complexity budget preserved |
| One Flowable process instance per DebtAccount | The debt lifecycle is the process |
| Java 21 exhaustive switch | Compile-time guarantee on suppression coverage — a missed suppression is a legal breach |
| Transaction boundary rule | DB writes commit before Flowable calls; process engine failures never roll back protective state |
| Write-off self-approval prevention | Three-layer enforcement: Flowable + service + DB constraint |
| Keycloak RBAC via JWT | No OPA, no database roles — standard OAuth2/OIDC with Spring Security |

- Supporting doc needed: **SUMMARY — ADR digest (one paragraph per key ADR)**

---

## Section 3 — Requirements Coverage (Slides 7–9)

### Slide 7 — Requirement Coverage Map
- Heat-map or table: 19 capability groups × (covered / design gap / blocked on DWP)
- Key message: all 19 groups have confirmed module ownership; gaps are classified, not unknown
- Supporting doc needed: **SUMMARY — requirements coverage one-pager (distilled from functional-requirements-module-map.md)**

### Slide 8 — Gap Classification
Three categories:

| Tier | Description | Risk owner |
|---|---|---|
| A | Design not yet done (complaints, legal/enforcement, finance depth) | Delivery team |
| B | Design complete, blocked on DWP policy values (I&E staleness, DCA notice period, etc.) | DWP programme |
| C | Scaffolding only — code structure exists, build not started (analytics, reporting, thirdparty) | Delivery team (sequencing) |

- Key message: no unknown gaps — every gap is named, owned, and categorised
- Supporting doc needed: none — distilled from functionality-gap-analysis-vs-current-design.md

### Slide 9 — Implied Build Scope
Things the requirements do not explicitly state but which the design team identified as necessary to deliver stated requirements safely and correctly. Framing: discovered scope, not gold-plating.

| Item | Why it is necessary |
|---|---|
| Audit trail completeness (process variables, DMN invocations, suppression evaluations) | Without this, regulatory queries cannot be answered from the system |
| Configuration version snapshot at process start | Without this, a policy activation mid-collection changes the rules for accounts already in a treatment path |
| Compile-time exhaustive switch on suppression reasons | A runtime miss on a suppression reason is a legal breach; compile-time guarantee makes that impossible |
| Three-layer write-off self-approval prevention | Financial control — any single layer is insufficient |
| DeceasedPartyHandler two-phase atomicity | Process engine failure must not leave a deceased account in active collection state |
| Champion/challenger simulation harness | Strategy changes must be testable against historical data before activation on live accounts |
| DCA nine-state pre-placement notice window | GDPR-mandatory disclosure — state machine enforces it; no shortcut to PLACED |

- Supporting doc needed: none — content is in this outline and in existing ADRs

---

## Section 4 — Customer and Account Model (Slides 10–11)

### Slide 10 — How a Debt Becomes an Account
Entity model walkthrough: Party → DebtAccount → RepaymentArrangement

Key points:
- DebtAccount is the primary process-bearing entity — one Flowable process instance per account for its full lifetime
- A Party (customer) can have multiple DebtAccounts; joint liability is modelled
- Vulnerability flags, accessibility needs, and third-party authority live on the Party, not the account
- Supporting doc needed: **DIAGRAM — entity model (Party / DebtAccount / RepaymentArrangement relationships)**

### Slide 11 — Regulatory State on the Account
Four critical account-level states: breathing space, insolvency, deceased, fraud marker

Key points:
- These are DB-first facts — process engine reads them fresh at every decision point, never from stale process variables
- These states cannot be misconfigured out of effect — they are hardcoded law, not configuration
- Supporting doc needed: none — distilled from MASTER-DESIGN-DOCUMENT.md and ADR-002

---

## Section 5 — Policy Configurator (Slides 12–13)

### Slide 12 — Three-Tier Configurability Model
| Tier | Covers | Who authors | Change frequency |
|---|---|---|---|
| 1 | Reference data (vulnerability types, suppression reasons, fee codes) | Operations team | Rarely |
| 2 | DMN decision tables (segmentation rules, routing logic, thresholds) | Business admin — no IT | Monthly |
| 3 | BPMN process definitions (treatment paths, escalation flows) | Business admin via Flowable Modeler | Quarterly |

- Supporting doc needed: **DIAGRAM — three-tier configuration model with author roles**

### Slide 13 — Policy Bundles
- A policy bundle groups Tier 1 + 2 + 3 changes under a single effective date
- Status lifecycle: DRAFT → READY → APPROVED → ACTIVE → RETIRED
- Activation is atomic — all components activate together; any failure rolls back the entire bundle
- Single rollback unit; single audit trail entry across all three tiers
- Key commercial message: business users change strategy without raising an IT ticket
- Supporting doc needed: **DIAGRAM — policy bundle lifecycle (status state machine)**

---

## Section 6 — Demo Flow Preview (Slides 14–15)

### Slide 14 — 14 Flows Covering 20 Behavioural Requirements
- Coverage matrix: flows mapped to behavioural requirements
- All 20 behavioural requirements covered by at least one flow
- Supporting doc needed: **SUMMARY — demo flow coverage matrix (distilled from demo-flow-design.md)**

### Slide 15 — Three Headline Demo Moments
1. **Flow 5 — Strategy change without IT:** business admin edits DMN table → creates policy bundle → runs champion/challenger simulation → activates atomically. Zero IT involvement.
2. **Flow 2 — Vulnerability to resolution in under 2 minutes:** vulnerability flag → suppression → I&E → affordable arrangement. Compliance path is hardcoded, not configured.
3. **Flow 12 — Regulatory audit in under 10 minutes:** full account history in the system; no external system lookup; no manual reconstruction.
- Supporting doc needed: none — distilled from demo-flow-design.md

---

## Section 7 — Solon Tax: Options, Limitations, and Decision (Slides 16–17)

### Slide 16 — Three Options Evaluated
| Option | Description | Scope reduction | Blockers |
|---|---|---|---|
| A | Full Solon Tax platform adoption | 10–15% | 7 hard blockers |
| B | Solon Tax financial engine only | 10–15% | 6 of 7 hard blockers |
| C | Greenfield (current path) | N/A | Zero |

- Key frame: does 10–15% scope reduction justify the risk? Answer: no.
- Supporting doc needed: none — distilled from solon-tax-feasibility-analysis.md and ADR-016

### Slide 17 — Why the Decision Is Locked
Lead blocker:
> This is a **Solon implementation feasibility finding**, not a change to the accepted Flowable architecture. DCMS requires regulatory events such as breathing space, deceased, and insolvency to preserve lifecycle continuity while immediately suppressing or suspending prohibited collection activity. In a Solon/Amplio implementation, the closest native BPMN mechanism is message boundary handling, but Amplio supports **interrupting** message boundary events only; **non-interrupting** message boundary events are not implemented (`amplio-process-engine-reference.md` §BPMN Element Support Matrix, line 46). Sending a debt collection communication during an active breathing space moratorium is a **criminal offence** under the Debt Respite Scheme Regulations 2020. This blocker independently disqualifies any Solon option that relies on Amplio to provide the regulatory event semantics.

**What interrupting-only means for a Solon implementation:**
- An **interrupting** message boundary event, when it fires, *terminates* the activity it is attached to and routes flow to the event handler. The main path stops; it is not resumed where it left off.
- A **non-interrupting** message boundary event, when it fires, leaves the main activity running and *spawns a parallel branch* to handle the event. Two paths now execute concurrently — main collection and suppression — sharing the same process instance and variables.
- A Solon-based DCMS would need the outcome provided by the second behaviour: lifecycle state must remain recoverable, prohibited collection activity must be suppressed immediately, and expiry must resume safely without manually reconstructing where the account was.

**Is there a workaround? Yes, technically. But it is not safe enough.**
Three workaround shapes exist on Amplio:

1. **Interrupt-then-restart**: use an interrupting boundary event to terminate the current activity, persist the resumption point manually, and restart at that point on moratorium expiry. *Problem:* the resumption point is now application-managed state, not engine-managed state. Every activity that could be interrupted needs hand-written restart logic. A missed restart path is a stuck account; a wrong restart path is a regulatory breach.
2. **Suppression-as-guard pattern**: drop boundary events entirely and instead check `suppressionActive` on every outbound communication step in the BPMN. *Problem:* this turns the compliance guarantee from a structural property of the process model into a discipline applied at every gateway. One forgotten check anywhere in any treatment path is one letter sent during breathing space — one criminal offence. The exhaustive-switch protection on the Java side does not extend to BPMN authored by business admins.
3. **Parallel process instance for suppression**: run a separate Amplio process instance for the suppression lifecycle, coordinated via Kafka signals. *Problem:* compounded by Amplio's sequential-only Parallel Gateway (`amplio-process-engine-reference.md:47`) and lack of cross-instance variable sharing. Coordination state lives outside the process engine, defeating the point of using one. Amplio's own suppression model (lines 97–106) takes this shape and is documented as incompatible with ADR-002.

**Why the workarounds do not save the option:** all three move the regulatory guarantee out of Solon/Amplio's process semantics and into application code, integration choreography, or BPMN authoring discipline. The current DCMS architecture keeps that guarantee inside the governed Flowable model plus the central `CommunicationSuppressionService`. The Solon workarounds replace a structural guarantee with "shouldn't happen if every custom guard is present." Given criminal liability, that downgrade is not acceptable.

Remaining six blockers (brief):
1. No native DMN — Drools DRL requires developer involvement; incompatible with non-technical governance model
2. Java 17 vs Java 21 — exhaustive switch is a statutory safety mechanism; downgrade breaks the compile-time guarantee
3. Liquibase vs Flyway — incompatible migration history tables; migration history is the audit trail
4. Angular vs React + GOV.UK Design System — GOV.UK DS compliance is a UK government requirement
5. Microservices vs monolith — Solon Tax assumes microservices; DCMS monolith is intentional
6. OPA vs Keycloak JWT RBAC — different security models, different integration paths

Decision is locked. Revisit only if DWP mandates a specific platform or a future Solon Tax version resolves all 7 blockers.

**Recommendation and operating posture (compact):**
- **Stay on Option C.** Do not revisit absent a DWP platform mandate or an Amplio version resolving all seven blockers.
- **Why, in four lines:** the Solon feasibility blocker is structural with no roadmap fix; the 10–15% saving is eaten by workaround cost and weakens compliance posture; the six secondary blockers compound; relitigating the decision costs delivery time without changing the answer.
- **In stakeholder forums:** lead with the criminal-offence framing. Treat the other six blockers as appendix material.
- **Predictable pushback — *"financial engine only?"*** If Option B still depends on Solon/Amplio process or dispatch behaviour, the lead blocker still applies. If those layers are replaced, the remaining benefit collapses to financial data-model scaffolding with Liquibase/Flyway and integration-coupling cost.
- **If forced to re-evaluate:** two weeks to reach the same answer; demo date moves by two weeks.
- Full version in ADR-016 §*Recommendation and Operating Posture*.

- Supporting doc needed: none — content fully covered in existing docs

---

## Section 8 — Development Releases (Slides 18–19)

### Slide 18 — Release 1 Scope
Focus: the core collection loop end-to-end, demonstrable in a running environment.

Modules in scope:
- customer, debtaccount, repaymentplan, communications (basic), workallocation (basic)
- strategy (Tier 2 DMN only — champion/challenger phased in)
- infrastructure/process, shared/process/port
- audit, Keycloak RBAC

Demo flows covered: Flows 1, 2, 3, 4, 6 (core agent workflow)

Not in Release 1:
- DCA placement (9-state machine)
- Legal/enforcement
- Analytics and reporting
- Full I&E depth
- External integrations (DWP Place, payment gateway, bureau feeds)
- Self-service customer API

- Supporting doc needed: **SUMMARY — Release 1 vs Release 2 scope table**

### Slide 19 — Release 2 and Beyond
| Release | Key additions |
|---|---|
| Release 2 | DCA placement (9-state machine), champion/challenger full rollout, policy bundle UI, self-service integration API, finance accounting depth (suspense, refunds, write-off reversal) |
| Release 3 | Legal/enforcement, complaints handling, analytics/reporting, migration tooling, test/UAT environments |

Key message: releases are milestone gates, not waterfall phases. Release 1 is working software in a real environment.

- Supporting doc needed: none — distilled from gap analysis and module map

---

## Section 9 — Close (Slide 20)

### Slide 20 — What Leadership Needs to Decide
Short list of open items blocked on DWP input (Tier B gaps):

| Item | Why it blocks delivery |
|---|---|
| I&E staleness period | Determines when I&E must be recaptured before an arrangement |
| DCA notice period | Sets the PRE_PLACEMENT_NOTICE window in the 9-state machine |
| Champion/challenger activation thresholds | Defines the % split and promotion criteria for strategy tests |
| Vulnerability lawful basis for data processing | Required before vulnerability data can be processed under GDPR |
| Statute-barred timing | Determines when a debt is legally unenforceable |

Key message: the design is complete and all architecture decisions are locked. Delivery starts now. Unblocking these five items unblocks the corresponding features.

---

## Appendix — Feature Deep Dives (Slides A–I)
*Not presented. Available for Q&A.*

### Appendix A — Champion/Challenger
- Two DMN versions deployed simultaneously
- Deterministic account-level routing by percentage split (version-scoped — accounts stay on assigned version for the duration of the test)
- Comparison dashboard for performance metrics
- Promotion path: challenger becomes champion, old champion retired
- Source: ADR-010, strategy-simulation-engine-design.md

### Appendix B — Strategy Simulation Engine
- Simulation runs a proposed rule change against a historical account population before activation on live accounts
- The only safe way to test a segmentation or threshold change without live account risk
- Outputs: projected treatment distribution, predicted outcomes vs current policy
- Source: strategy-simulation-engine-design.md

### Appendix C — Policy Bundle Activation
- Lifecycle: DRAFT → READY → APPROVED → ACTIVE → RETIRED
- Atomic activation across Tier 1/2/3 — one effective date, one approval, one audit entry
- Any component failure on activation rolls back the entire bundle
- Lightweight single-tier operational changes do not require a full policy bundle
- Source: ADR-009, MASTER-DESIGN-DOCUMENT.md

### Appendix D — Vulnerability Handling
- Vulnerability flag set on Party by agent
- CommunicationSuppressionService immediately suppresses all outbound contact
- I&E mandated before any repayment arrangement can be created
- Affordable arrangement calculated within policy floor (hardcoded minimum — no configuration can override)
- Entire path from vulnerability flag to arrangement is hardcoded compliance, not configuration
- Source: RULING-003, RULING-005, communications-domain-pack.md

### Appendix E — Breathing Space (Debt Respite Scheme)
- Flowable event subprocess handling preserves the account lifecycle while breathing-space suppression/suspension is applied.
- Moratorium applied immediately — outbound collection actions are suspended for the statutory period.
- On expiry, the lifecycle resumes safely from the governed process state. No Solon-specific manual restart logic or application-managed reconstruction point is required.
- Criminal liability if handled incorrectly — the architectural guarantee is that prohibited collection activity is blocked by the governed process model plus central suppression authority.
- **Solon Tax cannot replicate this structurally without custom workaround logic.** Amplio supports interrupting message boundary events only (`amplio-process-engine-reference.md:46`). A Solon implementation would need one of three workaround shapes (interrupt-then-restart, guard-checks on every send, parallel process instance), and all three move the guarantee out of the engine and into application code, integration choreography, or BPMN authoring discipline. See Slide 17 for the full Solon feasibility analysis. Primary disqualifier in ADR-016.
- Source: RULING-001, ADR-016, Debt Respite Scheme Regulations 2020, `amplio-process-engine-reference.md` §BPMN Element Support Matrix

### Appendix F — Deceased Party Handling
- Two-phase handler:
  - Phase 1 (transactional): deceased flag set + suppression logged + audit written — commits atomically
  - Phase 2 (non-transactional): Flowable process instances suspended
- Phase 2 failure is safe — the DB commit is the legal protection
- Process engine suspension failure never rolls back Phase 1
- Source: RULING-007, MASTER-DESIGN-DOCUMENT.md

### Appendix G — DCA Placement State Machine
Nine states: CANDIDATE → PRE_PLACEMENT_NOTICE → AWAITING_ACKNOWLEDGEMENT → PLACED → ACTIVE
Terminal states: RECALLED, CLOSED, DISPUTED, FAILED_TRANSFER

Key point: PRE_PLACEMENT_NOTICE window is GDPR-mandatory disclosure. State machine enforces it — no transition shortcut to PLACED.
- Source: DCA-management-domain-pack.md, RULING-008

### Appendix H — Regulatory Audit Trail
- Every action, every DMN invocation, every suppression evaluation, every process variable snapshot logged with actor, timestamp, and rule version
- Single query returns complete account history
- Any regulatory question answered from within DCMS — no external system lookups, no manual reconstruction
- Demo Flow 12: question to answer in under 10 minutes
- Source: MASTER-DESIGN-DOCUMENT.md, audit domain package

### Appendix I — Self-Service Customer Integration
- DCMS provides backend APIs only: I&E submission, payment events, arrangement setup requests, contact detail updates
- Customer-facing portal is external to DCMS — DCMS is the integration boundary, not the customer UI
- Important expectation-setter: DCMS does not include a customer portal; it exposes the contract a portal would consume
- Demo Flow 7 shows the API contract and workflow handoff, not a customer UI
- Source: MASTER-DESIGN-DOCUMENT.md, integration domain package
