# DESIGN-OPTIONS-004: Option B at Full Scope — Three-Workspace, BFF-Fronted, Event-Driven, Solon-Backed DCMS

## What this document is for

This is a **Delivery Designer single-option pass** that takes the recommended architecture from [DESIGN-OPTIONS-002](./DESIGN-OPTIONS-002-layer-thickness-and-boundary.md) — Option B, "Medium Custom Layer / DWP Domain Services Orchestrating Solon Primitives" — and projects it forward to the **full target scope**, not the Release 1 demo subset. It answers a single question:

> *If we commit to delivering every functional and non-functional requirement and serving every demo flow, with the three-workspace UI model fronted by per-workspace BFFs, Solon Tax retained as the system of record beneath, and the inter-component fabric explicitly designed as a secure event-driven architecture — what does the design look like?*

It is **not** an architecture decision (that remains a Solution Architect call recorded as a successor ADR to ADR-018), is **not** a re-evaluation of Options A or C (DESIGN-OPTIONS-002 remains the comparative pass), and is **not** an implementation plan. It assumes the Option B recommendation is upheld and shows the design at the depth needed to plan delivery.

**Audience:** Solution Architect (primary), Design Critic, Solon Tax platform reviewers, Delivery Lead, integration leads for the three workspace BFFs and the inter-service event fabric.

**Document ID:** DESIGN-OPTIONS-004
**Date:** 2026-05-01
**Status:** PROPOSED — single-option full-scope projection of DESIGN-OPTIONS-002 Option B
**Author:** Delivery Designer Agent
**Companion:** [DESIGN-OPTIONS-004-option-B-full-scope-architecture.drawio](./DESIGN-OPTIONS-004-option-B-full-scope-architecture.drawio) — full-scope architecture diagram for this document. The pre-existing demo-depth Option B diagram is [DESIGN-OPTIONS-002-option-B-architecture.drawio](./DESIGN-OPTIONS-002-option-B-architecture.drawio).
**Authoritative inputs read:** ADR-018, ADR-017 (three-workspace frontend), RULING-016 (Breathing Space gate-at-effect), RULING-010, RULING-011, RULING-014, `solon-tax-platform-reference.md`, `amplio-process-engine-reference.md`, `three-workspace-model.md`, `release/release-1-capabilities.md`, `working/demo-flow-capabilities.md`, `Functional-Requirements-Consolidated.md`, NFR set, `KAFKA_EVENT_CATALOG.md`, `solon_tax_2.3.0_integration_guide.md`, `api-suppression-management.md`.

---

## Summary

- **Shape:** Three workspace SPAs (Case Worker, Operations, Configuration) hosted in a shared shell, each backed by a dedicated Backend-for-Frontend service. Behind the BFFs sits the DCMS Custom Layer — a set of bounded domain services (Breathing Space gating, vulnerability governance, champion/challenger, I&E, communications suppression, work allocation, DCA placement, strategy authoring, audit) running in-process within Solon's `revenue-management-be-custom` extension model and orchestrated by DWP-specific BPMN processes deployed into the shared Amplio engine. Beneath sits Solon Tax v2.3.0 as system of record for ledger, payments, suppressions, tasks, correspondence transport, batch, and reference data.
- **Communication fabric:** Event-driven by default. Inter-service interaction is via Kafka commands and domain events on a small set of versioned, schema-registered topics. Synchronous REST is reserved for query paths the BFFs cannot satisfy from cached projections, and for the deterministic gate calls required by RULING-016. Outbox pattern is used at every write boundary that publishes events.
- **Security model:** Zero-trust between every tier. Browser ⇄ BFF is OIDC + PKCE with short-lived access tokens; BFF ⇄ Core Services is mTLS + machine OAuth2 with on-behalf-of token exchange; Kafka traffic is mTLS + SASL/OAUTHBEARER with topic-level ACLs and consumer-group isolation; OPA evaluates all policy decisions at the BFF and at sensitive Core Service entry points; data classification (Restricted / Confidential / Internal) drives both at-rest encryption keys and audit obligations.
- **Scope assumed:** Every functional requirement in `Functional-Requirements-Consolidated.md`, every NFR, and every one of the 14 demo flows in `three-workspace-model.md` §"Complete Demo Flow Mapping", at production depth — not Release 1 demo depth. The §"Six Demo Flows in Focus" section below shows how the same architecture serves the Release 1 six-flow subset without divergence.

---

## Six Demo Flows in Focus

The six demo flows that the Release 1 capability set is built around (per `working/demo-flow-capabilities.md`) are the high-value subset of the full 14. They exercise every architectural seam that matters: the three workspaces, all three BFFs, the event fabric, the Breathing Space gate, the strategy deployment pipeline, and the live read-model projections. This section shows what each flow demands of the architecture so that the rest of the document can be read as the design that satisfies these demands rather than an abstract structural pass.

### The six flows

| # | Flow | Primary workspace | Crosses into |
|---|---|---|---|
| 1 | Account Intake to First Meaningful Contact | Operations (supervisor end-state) | Case Worker (worklist + agent action) |
| 2 | Customer in Financial Difficulty — Vulnerability to Resolution | Case Worker | Operations (audit/oversight) |
| 3 | Arrangement Breach to Escalation to Third-Party Placement | Case Worker + Operations | — |
| 4 | Complex Household — Multiple Accounts, Joint Liability, Competing Workflows | Case Worker | Operations (supervisor view) |
| 5 | Business User Changes a Collection Strategy — No IT Required | Configuration | Operations (live impact observation) |
| 6 | Executive and Operational Dashboard — Real-Time Oversight | Operations | Configuration (strategy version + C/C link) |

### What each flow requires of the architecture

| Flow | Required capabilities the architecture must serve |
|---|---|
| **1 — Intake to first contact** | Inbound ingestion (Solon batch + DWP Place feed), case creation, work allocation queue, agent worklist refresh under SLA, supervisor dashboard reflecting volumes within seconds, ID&V at agent contact, audit of every state change. Touches: Operations BFF dashboards (live queue projection), Case Worker BFF (worklist + case detail), `WorkAllocationService`, Solon Case Management + Human Task Management. |
| **2 — Vulnerability to resolution** | Vulnerability identification by agent or system trigger, immediate (RULING-010) reassignment from CHALLENGER to CHAMPION strategy, FCA FG21/1 tier evaluation, MHCM-vs-standard suppression handling, gate-at-effect per RULING-016, evidence capture, audit-visible reason recording, downstream effect suppression (collection contact, enforcement, DCA, interest, etc.). Touches: `VulnerabilityGovernanceService` (direct in-process call from BPMN — not Data Area mirror), `BreathingSpaceGatingService`, `ChampionChallengerService`, Solon Suppression Management, audit aggregator. |
| **3 — Breach to DCA placement** | Arrangement breach detection in `DWP_ARRANGEMENT_PROCESS`, sequenced breach communications (channel-suppression-aware), routing into supervisor exception queue for placement decision, pre-placement disclosure gate, DCA selection, placement file generation, recall/reconciliation events. Touches: `DWP_ARRANGEMENT_PROCESS.bpmn`, `DWP_DCA_HANDOVER_PROCESS.bpmn` calling Solon `CASE_HANDOVER_COLLECTION_AGENCY_PROCESS.bpmn`, `CommunicationsSuppressionService`, `DCAPlacementService`, Operations BFF DCA performance projection. |
| **4 — Complex household** | Multiple accounts under joint liability, household-level resolution coordination, competing workflows on the same Person, RULING-014 arrangement-creation gate against active suppressions on any household member, consolidated Case Worker view, supervisor visibility of household-level state. Touches: `DWP_HOUSEHOLD_RESOLUTION_PROCESS.bpmn`, `BreathingSpaceGatingService` (Person-level suppression read), Customer master, Solon Taxpayer entity, Case Worker BFF household projection. |
| **5 — Strategy change without IT** | Configuration Workspace strategy assembler, peer-reviewed approval pipeline, simulation against historical journeys before publish, governed deployment to Amplio (BPMN), Drools KIE (rules), and policy bundles, version history + rollback, `StrategyVersionPublished` event, live impact visible in Operations BFF within seconds. Touches: Configuration BFF, `StrategyAuthoringService`, deployment pipeline, Amplio + Drools runtime, Operations BFF dashboard projection. |
| **6 — Executive / operational dashboard** | Live KPI suite — queue volumes, SLA health, breach trends, arrangement health, active placements, champion/challenger results — refreshing on every relevant event without per-render Solon round-trips. Drill-down from KPI to queue to agent to case. Touches: Operations BFF projections (live queue, C/C, DCA, audit-evidence), event-stream-fed read models, all `dcms.*` and selected Solon event topics. |

### Architectural seams these six flows force open

Every seam in this design is forced by at least one of the six flows; none of them is speculative.

| Architectural seam | Forced by which flow(s) |
|---|---|
| Three independent workspaces with distinct NFR profiles | All six (Case Worker latency-critical: 1, 2, 3, 4; Operations read-heavy: 1, 6; Configuration write-heavy + audit-heavy: 5) |
| Per-workspace BFF with read-model projections | Flow 6 (sub-second dashboard refresh impossible against Solon REST per-render); Flow 1 (worklist refresh under SLA); Flow 5 (Configuration audit + version surface) |
| Event-driven fabric with schema-registered events | Flow 6 (live KPI refresh requires event-fed projections); Flow 5 (`StrategyVersionPublished` triggers downstream rebuild); Flow 2 (`VulnerabilityIdentified` fans out to gate, C/C, audit) |
| `BreathingSpaceGatingService` at every effect site | Flow 2 (vulnerability + MHCM compounding); Flow 3 (DCA placement is a debtor-facing effect — must check on day of handover); Flow 4 (household-level Person suppression) |
| Direct in-process call from BPMN to `VulnerabilityGovernanceService` (not Data Area mirror) | Flow 2 — RULING-010's "immediately" requirement does not tolerate Outbox-mirrored staleness |
| Configuration deployment pipeline with simulation + rollback | Flow 5 — without these the flow is unsafe to demo and unsafe to run |
| Audit Aggregator on `dcms.audit.*` | All six — every flow has an audit obligation; Flow 6 surfaces it as a viewable evidence stream |
| Outbox pattern at every state-writing service | Flows 2, 3, 5, 6 — each publishes events whose loss would break a downstream projection or audit trail |

### Requirements (FR + NFR) explicitly exercised by the six flows

This is the subset of `Functional-Requirements-Consolidated.md` and the NFR set that the six flows directly exercise. The full-scope architecture must serve these as a hard floor, plus the remaining requirements for the other eight flows and the unscoped tail.

**Functional (by capability area, indicative — full mapping is owned by the trace map in Action Item 9):**

- Intake & ingestion: account ingestion from DWP source, case creation, batch-driven onboarding (Flow 1)
- Work allocation: queue rules, worklist composition, SLA timers, supervisor reassignment, exception queues (Flows 1, 3, 6)
- ID&V: identity verification at agent contact (Flow 1)
- Vulnerability: identification, FCA FG21/1 tiering, MHCM, evidence capture, review scheduling, immediate effect on strategy (Flow 2)
- Breathing Space: standard 60-day moratorium, MHCM moratorium, gate-at-effect, deferred-output flagging, on-lift correspondence disposition (Flow 2)
- Arrangements: creation under affordability, breach detection, sequenced breach comms, re-aging, breach tolerance (Flow 3)
- I&E: CFS standard model, affordability calculation, plan options (Flows 2, 3)
- Communications: channel-suppression-aware dispatch, statutory vs internal-hold disposition (Flows 2, 3)
- DCA placement: pre-placement disclosure, selection, placement, recall, reconciliation (Flow 3)
- Household: joint liability, multi-account coordination, household-level state (Flow 4)
- Strategy authoring: assembler, simulation, peer review, publish, deploy, rollback, version history (Flow 5)
- Champion/challenger: variant assignment, harm-indicator tracking, results materialisation (Flows 2, 6)
- MI / dashboards / KPIs: live queue, SLA, breach, arrangement, placement, C/C results (Flow 6)
- Audit: append-only audit, regulatory-evidence query surface (all six)

**Non-functional (where the six flows directly bind a budget):**

- **Latency:** Case Worker BFF screens p95 ≤ 1 s under call conditions (Flows 1, 2, 3, 4); Operations dashboard refresh ≤ 5 s end-to-end from event publish (Flows 1, 6); Configuration publish-to-live-impact visible ≤ 10 s (Flow 5).
- **Availability:** No single-workspace failure cascades to the other two (forced by independent BFFs and shell isolation); Kafka replication factor ≥ 3.
- **Security:** Zero-trust between every tier; data classification with Restricted-tier handling for vulnerability and MHCM (Flow 2); on-behalf-of token propagation so audit trails carry the original user identity (all six).
- **Auditability:** End-to-end correlation/causation ids; INSERT-only `audit_event`; reconciliation with Solon's audit; reconstructable evidence per case for regulator review (Flow 6 + the unscoped Flow 12).
- **Scalability:** Independent scaling per BFF; Operations BFF read-model can horizontally scale on dashboard load (Flow 6).
- **Accessibility:** WCAG 2.1 AA across all three workspaces.
- **Observability:** Distributed tracing through BFF → Core Service → Solon with the user identity propagated; structured logs and RED metrics on every Core Service.

The remainder of this document specifies the architecture that satisfies these requirements at full scope; the six flows above are the read-this-first lens on it.

---

## Action Items

Owned by **Solution Architect** unless otherwise noted. The first three items are inherited from DESIGN-OPTIONS-002 and remain gating; the rest are new and specific to the full-scope projection.

| # | Action | Owner | Gates |
|---|---|---|---|
| 1 | Close DESIGN-OPTIONS-002 Blockers 1, 2, 3 and the `suspendActiveInstancesSW` contradiction. This document inherits and does not re-state them. | SA → Solon platform team | All inter-layer contract design |
| 2 | Confirm whether the three workspace BFFs deploy as three separate services or as one BFF process exposing three role-scoped API surfaces (see §"BFF Deployment Topology"). | SA | BFF build plans, deploy pipelines, OPA policy bundles |
| 3 | Decide micro-frontend shell vs single-SPA-with-workspace-routing for the production shell (`three-workspace-model.md` already flags this; in full scope a micro-frontend shell is recommended). | SA → Frontend lead | Frontend repo and build topology |
| 4 | Lock the canonical event catalogue: every domain event DCMS publishes, every Solon event DCMS consumes, every command DCMS issues. Schemas held in a Confluent-compatible Schema Registry; PR-reviewed; versioned. | SA → Integration lead | All Core Service builds |
| 5 | Lock the Outbox + idempotency-key contract that every Core Service must follow when publishing events (table shape, dispatcher process, retry semantics, dedupe window). | SA → Integration lead | All Core Service builds |
| 6 | Define the DCMS OPA policy bundle layout, deployment model, and Solon-upgrade survival rules (carried over from DESIGN-OPTIONS-002 finding S5; required at full scope). | SA → Solon platform team | All BFF and Core Service authz |
| 7 | Lock the projection store strategy — which read models each BFF holds locally vs queries Solon for, including staleness budgets per projection (see §"Read Models and Projections"). | SA → Integration lead | BFF performance NFRs |
| 8 | Produce the closed enumeration of gated effect categories (DESIGN-OPTIONS-002 finding S1) at the full-scope effect set — moratorium, vulnerability, deceased, dispute, insolvency, MHCM, internal policy holds. Required before any DCMS BPMN authoring begins. | SA → Domain Expert | DCMS BPMN authoring |
| 9 | Map every one of the 14 demo flows (the six in focus first) to a traced realisation: workspace screens → BFF endpoints → events/commands → Core Services → Solon APIs → events back. Output goes into the trace map. | Delivery Designer → Traceability Steward | Test plan, release evidence pack |
| 10 | ~~Produce the full-scope drawio companion to this document~~ — DONE 2026-05-01: [DESIGN-OPTIONS-004-option-B-full-scope-architecture.drawio](./DESIGN-OPTIONS-004-option-B-full-scope-architecture.drawio). Open for visual review with SA and Design Critic. | Delivery Designer | Visual review with SA and Design Critic |
| 11 | Confirm the Configuration Workspace's deployment-pipeline mechanism for compiled strategy artefacts: how strategy bundles, BPMN deployments, Drools KIE updates, and policy-bundle changes flow from Configuration Workspace → governed approval → Amplio / Drools runtime in the shared cluster, with full audit (RULING-002, demo flow 5). | SA → Integration lead | Configuration Workspace build plan |
| 12 | Decide where audit-event aggregation lives (DCMS audit Core Service vs Solon's own audit) and how the two audit streams are reconciled in the regulatory-audit demo flow (flow 12). | SA → Domain Expert | Regulatory audit demo evidence |

---

## Scope Statement

This document assumes the full target scope:

| Dimension | Scope assumed |
|---|---|
| Functional requirements | 100% of `Functional-Requirements-Consolidated.md` |
| Non-functional requirements | 100% of NFR set (performance, availability, security, accessibility, observability, audit, retention) |
| Demo flows | All 14 flows in `three-workspace-model.md` §"Complete Demo Flow Mapping"; the six Release 1 flows from `working/demo-flow-capabilities.md` are the focus subset above |
| Workspaces | Case Worker, Operations, Configuration — all three at production depth |
| Domain packs | All packs listed in CLAUDE.md (customer, account, strategy, repaymentplan, payment, communications, workallocation, integration, audit, user) |
| Channels | All inbound and outbound (agent UI, self-service portal handoff, letters, SMS, email, in-app, batch file, DWP Place, DCA placement, payment gateway, bureau feeds) |
| Environments | local + dev (per CLAUDE.md), with a defined route to production-equivalent for NFR validation |

Out of scope for this document (handled elsewhere): the live Java version lever, Drools-vs-DMN lever, and the Configuration Workspace's internal sub-architecture (separate document per `three-workspace-model.md` §"Internal structure").

---

## Architecture at a Glance

```
   ┌────────────────────────┐  ┌────────────────────────┐  ┌────────────────────────┐
   │   Case Worker SPA      │  │   Operations SPA       │  │  Configuration SPA     │
   │   (frontline runtime)  │  │   (oversight + MI)     │  │  (strategy + control)  │
   └───────────┬────────────┘  └───────────┬────────────┘  └───────────┬────────────┘
               │ OIDC + PKCE                │                            │
               │ HTTPS / GraphQL or REST    │                            │
   ┌───────────▼────────────┐  ┌───────────▼────────────┐  ┌───────────▼────────────┐
   │    Case Worker BFF     │  │    Operations BFF      │  │  Configuration BFF     │
   │  composition + authz   │  │  composition + authz   │  │  composition + authz   │
   │  read-model cache      │  │  read-model cache      │  │  read-model cache      │
   └───────────┬────────────┘  └───────────┬────────────┘  └───────────┬────────────┘
               │ mTLS + OAuth2 (machine)    │                            │
               │ on-behalf-of token exchange│                            │
   ┌───────────▼────────────────────────────▼────────────────────────────▼────────────┐
   │                       DCMS Custom Layer (Core Services)                          │
   │                                                                                  │
   │  ┌────────────────────┐  ┌────────────────────┐  ┌────────────────────┐         │
   │  │ Breathing Space    │  │ Vulnerability      │  │ Champion /         │         │
   │  │ Gating Service     │  │ Governance Service │  │ Challenger Service │         │
   │  └────────────────────┘  └────────────────────┘  └────────────────────┘         │
   │  ┌────────────────────┐  ┌────────────────────┐  ┌────────────────────┐         │
   │  │ I&E / Affordability│  │ Comms Suppression  │  │ Work Allocation    │         │
   │  │ Service            │  │ Service            │  │ Service            │         │
   │  └────────────────────┘  └────────────────────┘  └────────────────────┘         │
   │  ┌────────────────────┐  ┌────────────────────┐  ┌────────────────────┐         │
   │  │ DCA Placement      │  │ Strategy Authoring │  │ Audit Aggregator   │         │
   │  │ Service            │  │ Service            │  │ Service            │         │
   │  └────────────────────┘  └────────────────────┘  └────────────────────┘         │
   │                                                                                  │
   │  + DWP-specific BPMN processes deployed into the shared Amplio engine            │
   └───┬──────────────────────────────────────────────────────────────────────────┬──┘
       │                                                                          │
       │ Kafka (commands + events, mTLS + OAUTHBEARER, schema-registered)         │
       │ Sync REST (queries) where unavoidable                                    │
       │                                                                          │
   ┌───▼──────────────────────────────────────────────────────────────────────────▼──┐
   │                            Solon Tax v2.3.0                                     │
   │  Amplio (BPMN runtime, shared) │ Ledger / Payments / Write-off                  │
   │  Suppression Management        │ Correspondence transport (CORRESPONDENCE_PROCESS)│
   │  Human Task Management         │ Reference Data / Codelists                     │
   │  Foundation Batch              │ Drools KIE Runtime                             │
   │  Keycloak (auth) + OPA (authz) │ ELK observability stack                        │
   └─────────────────────────────────────────────────────────────────────────────────┘
```

The shared product shell (auth, navigation, notifications, user context) wraps the three SPAs and is described in `three-workspace-model.md` §"Shared Product Shell". At full scope it is a micro-frontend shell so the three workspaces deploy independently; the demo build collapses this to a single SPA per ADR-017.

---

## Frontend: Three Workspaces, One Shell, Three BFFs

### Workspaces

The three workspaces are unchanged from `three-workspace-model.md`. Their full-scope obligations are:

| Workspace | Full-scope obligation |
|---|---|
| **Case Worker** | Every agent-facing interaction across all 14 flows — ID&V, vulnerability, breathing space, scripted flows, I&E capture, arrangement creation/amendment/breach, communications, dispute, deceased, settlement, write-off initiation, self-service handoff receipt. NFR-bound for first-meaningful-paint and time-to-action under call conditions. |
| **Operations** | Live queue and SLA dashboards, supervisor intervention, bulk reassignment, exception queues, champion/challenger results, DCA performance, MI/analytics suite, audit/evidence viewer, settlement and write-off approval, month-end surge tooling. NFR-bound for dashboard refresh rates and query latencies on aggregate data. |
| **Configuration** | Strategy assembler, deployment pipeline with peer review, Drools rule builder, task form designer, policy bundles (vulnerability, fraud, repayment, breathing space), foundations (codelists, RBAC), simulation engine, version history, rollback. NFR-bound for safe deployment governance and complete audit on every published change. |

### Shared product shell at full scope

A **micro-frontend shell** (Module Federation or equivalent) hosts each workspace as an independently deployable remote, with:

- a single Keycloak-backed session;
- a shell-level notification bus (server-sent events from a notifications BFF endpoint, routed to whichever workspace is open);
- shell-level routing that enforces RBAC visibility of workspace tabs;
- shared design system (Solon-styled component library) consumed by all three workspaces.

This costs more upfront than the demo's single-SPA approach but is required at full scope because the three workspaces have independent release cadences, different teams, and different NFR profiles.

### Three BFFs

Each workspace is paired with a dedicated BFF. The BFF responsibilities are uniform, but the API surface and read-model set differ per workspace.

| BFF responsibility | Description |
|---|---|
| API shape | One BFF endpoint per workspace screen / panel; coarse-grained, designed against the screen, not against backend services. GraphQL is preferred where the screen does heavy composition; REST elsewhere. |
| Authentication | Validates the SPA's OIDC access token; rejects tokens missing the workspace's required role claim before any backend call. |
| Authorisation | Calls OPA with `(subject, action, resource, context)` for every request. The DCMS OPA policy bundle (Action Item 6) holds DCMS-specific Rego policies. |
| Composition | Aggregates Solon REST, Core Service REST, and local read-model projections into the screen-shaped response. |
| Read-model cache | Holds projections built from the event stream so dashboards and case-detail screens hit the BFF, not Solon. Staleness budgets are explicit per projection (see §"Read Models and Projections"). |
| Token exchange | Exchanges the user's access token for an on-behalf-of machine token when calling Core Services or Solon APIs, so downstream audit trails carry the original user identity. |
| Idempotency | Issues idempotency keys for all mutating commands so retries on the wire are safe. |
| Observability | Emits per-request structured logs, metrics, and traces with the user identity and workspace context propagated end-to-end. |

### BFF Deployment Topology (Action Item 2)

Two viable shapes:

- **Three separate BFF services** — one per workspace, deployed independently. Cleaner blast radius, separate scaling profiles, separate OPA policy bundles. Recommended at full scope.
- **One BFF process exposing three role-scoped API namespaces** — same code base, runtime separation by URL prefix and role gate. Lower infra cost. Acceptable if separate scaling is not required.

The recommendation at full scope is three separate BFF services. The Operations BFF is read-heavy and benefits from independent horizontal scaling; the Configuration BFF has very different NFRs (write-heavy, audit-heavy, much lower QPS) and benefits from isolation; the Case Worker BFF is the latency-critical one and must not share contention with the other two.

---

## DCMS Custom Layer (Core Services)

The Core Services are the DWP-specific domain logic, structurally identical to the Option B definition in DESIGN-OPTIONS-002 §"Option B" but enumerated here at full-scope depth.

| Core Service | Owns | Reads / writes Solon |
|---|---|---|
| **Breathing Space Gating Service** | Gate-at-effect evaluation per RULING-016, gate log table, MHCM handling, DEFER_PENDING_MORATORIUM_END flagging, arrangement-creation gating per RULING-014, gated-effect enumeration applied at every Core Service effect site | Reads Solon `active_suppressions`; issues `CreateSuppressionCommand` / `ReleaseSuppressionCommand` |
| **Vulnerability Governance Service** | FCA FG21/1 vulnerability tiers, identification triggers, immediate reassignment per RULING-010, MHCM / vulnerability compounding, evidence capture and review scheduling | Source of truth for vulnerability flags; direct in-process call from BPMN service tasks (replacing the demo's Data Area mirroring per DESIGN-OPTIONS-002 finding S2) |
| **Champion / Challenger Service** | Variant assignment, harm-indicator tracking, promotion logic, RULING-010 guardrails, results materialised for Operations dashboards | Strategy variant metadata stored in DCMS schema; assignments published as events |
| **I&E / Affordability Service** | CFS standard model, affordability calculation, deficit handling, plan-option generation | Issues `CreatePaymentPlanCommand` to Solon ledger via Kafka |
| **Communications Suppression Service** | RULING-011 category evaluation, on-lift disposition (statutory discard vs internal-hold review), pre-dispatch guard, channel-preference and consent tracking | Calls Solon `SendCorrespondenceCommand`; consumes Solon correspondence-queue events; instructs discard/hold |
| **Work Allocation Service** | DWP queue rules, worklist composition, supervisor override, exception-queue routing, SLA timer enforcement | Reads Solon Human Task Management state; issues task assignment commands |
| **DCA Placement Service** | Pre-placement disclosure gate, DCA selection logic, placement file generation, performance reconciliation, recall handling | Calls Solon `CASE_HANDOVER_COLLECTION_AGENCY_PROCESS.bpmn` as a sub-process; consumes recall events |
| **Strategy Authoring Service** | Strategy assembler runtime, compiled-strategy pipeline, peer-review and approval workflow, simulation engine, version history, rollback | Deploys compiled artefacts to Amplio (BPMN), Drools KIE (rules), and policy-bundle store; full audit |
| **Audit Aggregator Service** | Append-only DCMS audit event store, regulatory-audit query surface, retention enforcement, cross-stream reconciliation with Solon's own audit | Consumes events from every Core Service and selected Solon topics; writes to `audit_event` (INSERT-only) |

### DWP-specific BPMN

The following BPMN process definitions are authored by DCMS and deployed into the shared Amplio engine alongside Solon's reference processes:

- `DWP_DEBT_RECOVERY_PROCESS.bpmn` (root debt lifecycle, full scope — not a fork of `DEBT_RECOVERY_PROCESS.bpmn`)
- `DWP_ARRANGEMENT_PROCESS.bpmn` (re-aging, breach tolerance, breach-to-DCA branch)
- `DWP_DCA_HANDOVER_PROCESS.bpmn` (calls Solon `CASE_HANDOVER_COLLECTION_AGENCY_PROCESS.bpmn` as sub-process for handover transport)
- `MHCM_RELEASE_PROCESS.bpmn` (manual professional sign-off, evidence capture, 30-day-tail handling)
- `DWP_DISPUTE_PROCESS.bpmn`
- `DWP_DECEASED_PROCESS.bpmn`
- `DWP_WRITE_OFF_PROCESS.bpmn` (authority-tier approvals, calls Solon `WriteOffDebtCommand`)
- `DWP_SETTLEMENT_PROCESS.bpmn` (delegated authority approval flow)
- `DWP_HOUSEHOLD_RESOLUTION_PROCESS.bpmn` (complex household per flow 4)
- `DWP_SELF_SERVICE_HANDOFF_PROCESS.bpmn` (flow 7 receiver-side)
- `DWP_BREATHING_SPACE_INTAKE_PROCESS.bpmn`

Every one of these processes calls `BreathingSpaceGatingService` before any debtor-facing effect, enforced by a custom Amplio service-task base class (DESIGN-OPTIONS-002 Option B risk row 2 mitigation, mandatory at full scope).

### Amplio constraints carried forward

The six Amplio guardrails listed in DESIGN-OPTIONS-002 §"Builder guardrails" (sequential parallel gateway, FEEL-only script tasks, service-task-only rules, no compensation events, two `SuppressionType` configurations, integration-guide-§5-only Kafka topics) apply to every DCMS BPMN at full scope. They are not relaxed by the larger scope.

---

## Event-Driven Architecture

### Why event-driven at full scope

Full scope adds: 14 demo flows with cross-workspace journeys; live dashboards refreshing on every state change; a self-service portal feeding inbound events; DCA placement and reconciliation feeds from external partners; bureau feeds; month-end surge handling; immediate vulnerability reassignment per RULING-010. Synchronous request/response across these would force tight temporal coupling between every Core Service, Solon, and the BFFs. Event-driven by default lets each Core Service own its writes, publish a domain event, and let interested parties (other Core Services, BFF projection builders, the audit aggregator, Solon itself) react asynchronously.

Synchronous calls remain where they are correct: the Breathing Space gate (must be deterministic and pre-effect), some Solon REST queries the BFFs cannot satisfy from projections, and the Drools rule evaluation path that Amplio service tasks already use.

### Event taxonomy

Three classes of message on the bus:

| Class | Purpose | Examples | Naming |
|---|---|---|---|
| **Commands** (DCMS → Solon) | Imperative requests against Solon's financial / suppression / correspondence primitives | `PostPaymentCommand`, `WriteOffDebtCommand`, `CreatePaymentPlanCommand`, `SendCorrespondenceCommand`, `CreateSuppressionCommand`, `ReleaseSuppressionCommand` | Per Solon integration guide §5 (subject to Blocker 1) |
| **Domain events** (DCMS → world) | Past-tense facts about DWP-specific state changes | `VulnerabilityIdentified`, `MoratoriumApplied`, `MoratoriumLifted`, `ArrangementCreated`, `ArrangementBreached`, `ChampionChallengerAssigned`, `DCAPlacementInstructed`, `WriteOffApproved`, `StrategyVersionPublished` | `dcms.<bounded-context>.<event-name>.v<n>` |
| **Solon events** (Solon → DCMS) | Solon platform events DCMS reacts to | Suppression created/expired, payment posted, task created, correspondence dispatched | Per Solon `KAFKA_EVENT_CATALOG.md` |

Every event carries: event id, event type, schema version, occurred-at timestamp, correlation id, causation id, idempotency key, partition key, payload, and a small metadata envelope (publishing service, schema registry pointer, classification tag).

Schemas live in the Schema Registry. Breaking schema changes are forbidden; additive evolution only. Any breaking change goes via a parallel `vN+1` topic with a defined cutover window.

### Outbox pattern

Every Core Service that writes state and publishes events follows the Outbox pattern (Action Item 5):

- the state write and the outbox row are committed in the same database transaction;
- a dispatcher process reads the outbox, publishes to Kafka, and marks the row dispatched;
- consumers dedupe on the event id within a defined window;
- retries are at-least-once; idempotent consumers are mandatory.

This is the only way to make "DCMS publishes a domain event when X happens" safe across crashes and restarts, and it removes the dual-write hazard that the demo design already flagged for the vulnerability mirror (DESIGN-OPTIONS-002 finding S2). At full scope, mirroring is replaced by direct in-process calls to `VulnerabilityGovernanceService`, and the outbox is reserved for genuine cross-service events.

### Topic ownership and isolation

| Topic family | Owner | Notes |
|---|---|---|
| `dcms.breathingspace.*` | Breathing Space Gating Service | Includes `MoratoriumApplied`, `MoratoriumLifted`, `MoratoriumDeferredEffect` |
| `dcms.vulnerability.*` | Vulnerability Governance Service | Includes `VulnerabilityIdentified`, `VulnerabilityTierChanged`, `VulnerabilityReviewed` |
| `dcms.strategy.*` | Strategy Authoring Service | Includes `StrategyVersionPublished`, `StrategyDeploymentApproved` |
| `dcms.cc.*` | Champion/Challenger Service | Includes `ChampionChallengerAssigned`, `CCResultMaterialised` |
| `dcms.arrangement.*` | I&E + arrangement BPMN | Includes `ArrangementCreated`, `ArrangementBreached` |
| `dcms.dca.*` | DCA Placement Service | Placement, recall, reconciliation events |
| `dcms.audit.*` | Audit Aggregator (sink, not source) | Read-only by everyone except the aggregator |
| `irm.bpmn-engine.*` | Solon | Consumed only; never published to by DCMS unless the topic is documented as DCMS-publishable |

Consumer-group isolation is mandatory: each Core Service consumes only from its declared input topics, registered in IaC.

### Read Models and Projections (Action Item 7)

The three BFFs maintain their own read-model projections built from the event stream, scoped to the screens they serve:

| Projection | Owner BFF | Source events | Staleness budget |
|---|---|---|---|
| Case timeline read model | Case Worker BFF | All `dcms.*` events for a case + Solon case events | < 2 s |
| Worklist read model | Case Worker BFF | Work-allocation events | < 5 s |
| Live queue dashboard read model | Operations BFF | Work-allocation + SLA timer events | < 5 s |
| C/C results read model | Operations BFF | `dcms.cc.*` + outcome events | minute-scale (analytics) |
| DCA placement read model | Operations BFF | `dcms.dca.*` | < 30 s |
| Strategy version history read model | Configuration BFF | `dcms.strategy.*` | < 5 s |
| Audit-evidence read model | Operations BFF (audit viewer) | `dcms.audit.*` | < 10 s |

Projections that need stronger consistency (or that Solon owns the truth for, such as live ledger balance) are not projected — the BFF reads them synchronously from Solon REST and caches with a short TTL.

---

## Security Model

The architecture is zero-trust between every tier. No service trusts the network; every call carries an identity, an authorisation decision, and an audit trail.

### Browser ⇄ BFF

- OIDC Authorization Code flow with PKCE.
- Access tokens short-lived (≤ 15 min), refresh tokens rotated.
- Tokens carry role claims that the BFF validates before reading any application input.
- All transport is TLS 1.3.
- CSP, HSTS, SameSite cookies, anti-CSRF tokens for state-changing endpoints.
- Per-user session inactivity timeout enforced server-side.

### BFF ⇄ Core Services / Solon

- mTLS between every service in the cluster (service mesh terminated).
- Machine OAuth2 with on-behalf-of token exchange so downstream services see the original user identity. Audit trails everywhere carry that identity.
- OPA policy decision points: at the BFF (request-level) and at sensitive Core Service entry points (defence in depth). The DCMS Rego bundle (Action Item 6) is deployed as a sidecar or co-located bundle, with a deployment model that survives Solon upgrades.

### Core Services ⇄ Kafka

- mTLS for transport.
- SASL/OAUTHBEARER for service authentication.
- Topic-level ACLs in Kafka — producers and consumers explicitly granted per topic.
- Schema Registry enforces schema compatibility on publish.
- Sensitive payloads (vulnerability, MHCM) are field-level encrypted using a separate key, with decryption restricted to authorised consumers.

### Data classification and at-rest

Three classifications drive both encryption keys and audit obligations:

| Class | Examples | Storage controls |
|---|---|---|
| Restricted | MHCM status, vulnerability detail, evidence documents, special-category data | Field-level encryption with separate KMS key; access requires elevated role + OPA decision; every read audited |
| Confidential | Personal data, financial transactions, contact details | Database-level encryption at rest; row-level access controls where supported; bulk read alerted |
| Internal | Reference data, codelists, queue metadata | Standard at-rest encryption |

Audit aggregation (Action Item 12) consumes from `dcms.audit.*` and selected Solon audit events, producing the regulatory-audit evidence surface used by demo flow 12.

### Configuration Workspace governance

Because the Configuration Workspace deploys executable change (BPMN, Drools rules, policy bundles, codelists), it has additional controls:

- All published changes go through a peer-reviewed approval pipeline (Action Item 11);
- Every published change emits a `dcms.strategy.StrategyVersionPublished` event with the full diff, the approver, and the deployment target;
- No direct production deployment — the pipeline writes through Solon's deployment APIs (Amplio for BPMN, Drools KIE for rules);
- Rollback is a first-class operation, not a manual restoration;
- A simulation environment lets configurators replay historical journeys against a proposed change before publish.

---

## NFR Coverage at Full Scope

Indicative; the full mapping is owned by the test plan and traceability artefacts.

| NFR family | Where addressed |
|---|---|
| Performance / latency | BFF projections + read-model caches; staleness budgets per projection; Solon REST for ground-truth queries with TTL cache; mTLS overhead measured and budgeted |
| Availability | Stateless BFFs behind load balancer; Core Services horizontally scalable; Kafka replication factor ≥ 3; Solon HA per platform; outbox dispatcher idempotent so restarts don't duplicate |
| Scalability | Independent scaling per BFF; Core Service scaling per topic throughput; Kafka partition strategy per partition key |
| Security | §"Security Model" above |
| Auditability | Audit Aggregator + classification-driven audit obligations; correlation/causation ids end-to-end; INSERT-only `audit_event`; reconciliation with Solon audit |
| Accessibility | Shell + workspaces conform to WCAG 2.1 AA; design system enforces |
| Observability | Structured logs, RED metrics, distributed traces with user-identity propagation; Solon ELK extended to DCMS Core Services |
| Retention | Per-classification retention rules; immutable history for legal/audit data per `immutable-records-guard` skill |
| Internationalisation | Locale-keyed strings; CI checker (per `i18n-completeness-checker` skill) enforces completeness on any frontend PR |

---

## Inherited Blockers (from DESIGN-OPTIONS-002)

These remain open and gating. This document does not re-litigate them but cannot be locked while they are open:

| # | Blocker | Owner |
|---|---|---|
| 1 | Kafka command-name discrepancy — confirm whether the abstracted command catalogue maps to externally-publishable topics, with payload schemas, response events, and consumer-group isolation | SA → Solon platform team |
| 2 | Solon REST API stability for sync queries the BFFs depend on | SA → Solon platform team |
| 3 | `SuppressionExpiryJob` behaviour with `maximumNumberDays: 0` for MHCM and Vulnerability suppressions | SA → Solon platform team (verify by test) |
| C | `suspendActiveInstancesSW` compliance contradiction — declared per `SuppressionType`, reconciled with RULING-016 | SA |

---

## Risks at Full Scope

| Risk | Severity | Mitigation |
|---|---|---|
| Three BFFs drift in their authz logic, projection contracts, or token-handling, producing inconsistent user experience or audit gaps | Medium | Shared BFF library for auth/OPA/idempotency/token-exchange; OPA policy bundle is single source of truth; contract tests across all three BFFs |
| Event schema drift breaks downstream consumers silently | High | Schema Registry compatibility checks in CI; contract tests for every published event; additive-only evolution rule |
| Outbox dispatcher lag during peak load (month-end surge, demo flow 13) starves projections and stalls Operations dashboards | Medium | Dispatcher horizontal scale; backpressure on writes; dashboard projections degrade gracefully with last-good staleness indicator |
| In-process direct call from BPMN service tasks to `VulnerabilityGovernanceService` couples Amplio runtime tightly to that service's availability | Medium | Service runs in same JVM as `revenue-management-be-custom`; failure mode is fail-closed (block effect, raise task) — never fail-open |
| Configuration Workspace publishes a strategy that breaks live operations | High | Mandatory simulation step; peer-reviewed approval; staged rollout via champion/challenger; one-click rollback; full audit |
| Solon upgrades change Kafka topic semantics or REST payloads | Medium | Pinned Solon version; consumer-driven contract tests; upgrade-rehearsal environment |
| MHCM handling falls between Solon's suppression model and DCMS's gate | High (criminal-liability exposure) | Resolve Blockers 3 and C first; explicit MHCM `SuppressionType` configuration; `BreathingSpaceGatingService` carries MHCM-specific logic; release path is DCMS-owned via `MHCM_RELEASE_PROCESS.bpmn` |
| Three independent SPAs deviate visually from Solon's design language | Low | Shared design system enforced; design review on every release |

---

## What This Document Does Not Decide

- The architecture lock — that is the SA's call in a successor ADR to ADR-018.
- The Java version (lever from DESIGN-OPTIONS-002).
- Drools-vs-DMN.
- The Configuration Workspace's internal sub-architecture (covered in `configuration-tool-architecture.drawio` and an upcoming companion document).
- The Release 1 vs full-scope delivery split — Release 1 is governed by `release/release-1-capabilities.md` and Option B-Demo in DESIGN-OPTIONS-002. This document explicitly assumes full scope, with the six Release 1 flows surfaced up-front as the focus subset.

---

## Handoff Declaration

This document is ready for:

- Solution Architect review against the Inherited Blockers and Action Items above;
- Design Critic critique focused on full-scope failure modes (event-fabric drift, projection staleness, BFF inconsistency, security boundary gaps, NFR coverage);
- Solon Tax platform expert review of the event-driven design assumptions (Blockers 1 and 2 are the central questions);
- Traceability Steward to seed the trace map with the six-flow realisation first, then the full 14 (Action Item 9);
- Delivery Designer follow-up to produce the full-scope drawio companion (Action Item 10).
