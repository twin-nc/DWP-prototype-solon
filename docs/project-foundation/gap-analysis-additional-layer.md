> ## ⚠ STATUS: UNSETTLED — PLATFORM PIVOT IN EFFECT
>
> A confirmed programme-level decision (ADR-018, 2026-04-30) has pivoted the project to build on top of **Solon Tax** as the base platform. A new design process is underway.
>
> **This document contains accumulated knowledge and analysis that remains relevant to the new design process. It does NOT represent the current or preferred direction.** All decisions and conclusions herein are open for renewed debate. Do not treat this document as a directive or default position.
>
> See [`CLAUDE.md`](../../CLAUDE.md) and [ADR-018](decisions/ADR-018-platform-pivot-solon-tax-confirmed.md) for the current direction.

---

# Additional-Layer Gap Analysis - Current Reconciliation

**Project:** DWP Debt Collection Management System (DCMS)  
**Date:** 2026-04-27  
**Status:** Reconciled against current Flowable-based design baseline  
**Purpose:** Explain which concepts from the old "additional layer" analysis and diagrams are rejected, which capabilities survive, and where surviving capabilities now live.

---

## Current Position

The old additional-layer analysis is no longer a target architecture.

It was written for a model where DCMS-like capabilities would sit as an extra layer on top of **Solon Tax**. In that model, the additional layer had to compensate for Solon runtime boundaries: Amplio BPMN, Drools/KIE, OPA, Solon Kafka command topics, Solon human-task APIs, SCDF batch APIs, separate reference-data services, and Solon UI/reporting surfaces.

That is not our design.

The current baseline is a **purpose-built Spring Boot modular monolith** using **embedded Flowable BPMN/DMN** as infrastructure. Domain capabilities are owned inside the monolith and exposed through DCMS application APIs and a DCMS React frontend. We are building on Flowable, not Solon.

Authoritative sources:

- `docs/project-foundation/MASTER-DESIGN-DOCUMENT.md`
- accepted ADRs in `docs/project-foundation/decisions/`
- locked implementation ADRs in `docs/project-foundation/architecture-decisions.md`
- domain rulings in `docs/project-foundation/domain-rulings/`
- domain packs in `docs/project-foundation/domain-packs/`

---

## Design Rule

Use the additional-layer material only as a historical capability checklist.

Do not use it for:

- deployment topology
- service boundaries
- database boundaries
- process-engine integration patterns
- Solon integration patterns
- UI application boundaries
- implementation planning without cross-checking the current baseline

---

## What We Reject, Keep, Or Adapt

| Old additional-layer / diagram concept | Current Flowable DCMS equivalent | Disposition | Why |
|---|---|---|---|
| Separate "Additional Layer" runtime above Solon Tax | Single Spring Boot modular monolith with embedded Flowable | **Reject** | ADR-007 locks a single deployable backend. DCMS is not a Solon extension. |
| Solon Tax execution runtime | Flowable BPMN/DMN in `infrastructure/process` | **Reject** | We are building on Flowable, not Solon. There is no Solon runtime boundary in the target design. |
| Amplio Process Engine | Flowable BPMN engine | **Reject** | Flowable is the process engine. No Amplio adapter, gateway, or deployment API is needed. |
| Strategy Compiler Service converting JSON to BPMN XML | Authored BPMN/DMN assets deployed through Flowable governance | **Reject** | We own BPMN/DMN natively. A compiler would create a second source of truth. |
| Step Library and Step Library Editor | Process Action Library for governed Flowable service-task actions | **Adapt** | The Solon-style assembler library is rejected, but a governed action catalogue is retained for safe BPMN authoring. See `process-action-library-design.md`. |
| Simulation Engine replaying historical journeys | Strategy Simulation Engine with phased DMN, policy, strategy-path, and BPMN replay roadmap | **Adapt** | The capability is retained, but implemented as deterministic offline evidence inside DCMS governance rather than a Solon-side replay service. See `strategy-simulation-engine-design.md`. |
| Strategy deployment pipeline into Amplio REST APIs | Flowable deployment and BPMN versioning policy | **Reject** | Deployment is internal to Flowable and governed by ADR-006. |
| Drools Rule Builder / KIE wrapping | Flowable DMN plus service-layer invariant checks | **Reject** | Tier 2 rules use DMN. Non-overridable legal and safety rules remain in domain services. |
| OPA policy service | Policy bundles, DMN, and domain rulings | **Reject** | ADR-009 gives us policy-bundle governance without a separate OPA runtime. |
| Kafka commands to Solon BPMN topics | Flowable messages/signals via process ports | **Reject** | We do not need Kafka to command an embedded engine. Flowable correlation is local infrastructure. |
| Additional-layer-owned Kafka topics for internal orchestration | In-process command bus, domain events where justified, Flowable events | **Mostly reject** | Kafka is not the default boundary between in-monolith modules. |
| Human Task Management REST API wrapper | Flowable `TaskService` hidden behind application/work-allocation APIs | **Reject** | Human tasks are owned by Flowable and surfaced through DCMS APIs, not Solon wrappers. |
| Redis / Hazelcast distributed cache | PostgreSQL, Flowable runtime/history tables, targeted Spring caching if justified | **Reject as baseline** | Core correctness depends on durable state, not distributed cache coordination. |
| SCDF Batch Engine REST API | Spring scheduled jobs, Spring Batch where needed, Flowable timers for workflow due dates | **Reject** | SCDF is unnecessary for the current monolith and Flowable timer model. |
| Separate Strategy DB, Step Library DB, Deployment Records DB | Application schema plus Flowable schema and versioned config records | **Reject** | Domain data belongs to domain modules; process runtime/history belongs to Flowable. |
| Separate Queue State DB and SLA Timers DB | `domain/workallocation` tables plus Flowable timers/event subprocesses | **Reject** | Queue state is a work-allocation concern, not an additional-layer store. |
| Contact suppression through Solon Kafka commands | `CommunicationSuppressionService` and `SUPPRESSION_LOG` | **Reject** | ADR-014 makes suppression a communications-module authority enforced at dispatch call sites. |
| Process Configurator, Task Configurator, Foundations Configurator as separate apps | One role-scoped `/admin` surface for Foundations, Rules, Processes, Templates, and Policies | **Adapt** | ADR-015 keeps the configurability tiers but rejects separate configurator applications. |
| Policy bundles across process/task/foundation config | Policy layer for cross-tier effective-dated changes | **Keep and adapt** | ADR-009 retains policy bundles inside the monolith with approval, audit, rollback, and effective dating. |
| Agent Desktop and Supervisor Dashboard as additional-layer UIs | DCMS React frontend, work-allocation APIs, reporting read models | **Keep capability, reject placement** | The UI capability is real, but it is not a Solon overlay. |
| Self-Service Portal extensions | Current scope excludes customer self-service | **Reject/defer** | Customer self-service is out of current delivery scope unless scope is reopened. |
| Jasper Reports / IRM Queries | `domain/reporting` read models and query contracts | **Reject** | Reporting is designed through DCMS read models, not Solon/Jasper/IRM query surfaces. |
| TXPACC pay-plan dependency | `domain/repaymentplan`, I&E/affordability, Flowable arrangement monitoring | **Reject** | Repayment plan lifecycle is DCMS-owned. |
| Solon Keycloak wrapper / reference-data wrapper | Keycloak 24 plus DCMS `user` and configuration modules | **Reject wrapper, keep Keycloak** | Keycloak remains the identity provider, but not through Solon reference-data or UI wrapping. |
| ELK / Kubernetes / Helm inherited from Solon runtime | DCMS platform, deployment, and observability standards | **Reject Solon dependency framing** | Containers and observability may still exist, but as DCMS platform choices. |

---

## Capabilities That Survive

The old additional-layer analysis was wrong about architecture, but it identified real business capabilities. Those capabilities now belong to current DCMS modules and design artefacts.

| Capability from old analysis | Current authoritative home | Current status |
|---|---|---|
| Strategy design, treatment routing, segmentation, champion/challenger | `domain/strategy`, `domain/analytics`, ADR-008, ADR-009, ADR-010, locked ADR-008/014, RULING-010 | Designed, with some promotion-threshold questions still open |
| Communications, templates, suppression, contact preferences | `domain/communications`, communications domain pack, ADR-014, RULING-001, RULING-011 | Design largely closed; some channel-policy questions remain |
| Work allocation, queues, agent desktop, supervisor dashboard | `domain/workallocation`, work-allocation domain pack | Design largely closed; some operational defaults remain open |
| Reporting, MI, KPI views, dashboard projections | `domain/reporting`, reporting/analytics read-model domain pack, `mi-dashboard-and-reporting-ux-design.md` | Design largely closed; implementation scaffolding remains |
| I&E, affordability, review scheduling | `domain/repaymentplan`, I&E engine pack, RULING-007 | Designed but policy-gated |
| DCA placement, recall, reconciliation, commission | `domain/thirdpartymanagement`, DCA management pack, RULING-008 | Designed but policy-gated |
| Vulnerability handling | `domain/customer`, `domain/communications`, vulnerability pack, RULING-002, vulnerability governance notes | Designed in part; taxonomy/lawful-basis questions remain |
| Configuration tiers and policy-bundle governance | ADR-008, ADR-009, ADR-015 | Architecture accepted |
| Reusable process-action catalogue | `process-action-library-design.md`, ADR-003, ADR-015 | Design baseline added |
| Strategy simulation and historical replay roadmap | `strategy-simulation-engine-design.md`, ADR-008, ADR-009, ADR-010, ADR-015 | Design baseline added; BPMN replay is a later phase |
| Flowable admin and process governance | ADR-006, ADR-011, locked Flowable admin ADR | Architecture accepted and implementation pattern locked |

---

## Current True Gaps

Do not treat rejected Solon/additional-layer components as missing DCMS functionality.

The remaining true gaps are the current gaps recorded in `functionality-design-gaps.md` and the master design open-question register:

| Gap type | Current gap |
|---|---|
| Missing design artefacts | Complaints handling, legal/enforcement pathways, finance operations, manual corrections/amendments, retention/archive/closure, operational recovery tooling, full frontend workflow catalogue |
| Policy-gated areas | I&E policy values, DCA notice/acknowledgement/commission policy, vulnerability lawful basis/taxonomy, statute-barred timing, champion/challenger thresholds, communications fallback/suppression defaults, work-allocation SLA/concurrency defaults |
| Implementation scaffolding | `analytics`, `thirdpartymanagement`, `reporting`, `infrastructure/process`, `shared/process/port` |

---

## Reading Guidance

Use this file when someone asks:

- "What happened to the additional layer?"
- "Why are we not building the Solon-style architecture?"
- "Which parts of the old diagrams still matter?"
- "What should we build instead?"

Short answer:

- The additional layer is rejected.
- The business capabilities mostly survive.
- The implementation moves into the Flowable-based modular monolith.
- Remaining gaps are tracked in the current design-gap and master-design documents, not in the old Solon-oriented diagrams.
