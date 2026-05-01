# DESIGN-OPTIONS-002B: Open Architecture Decisions

**Document ID:** DESIGN-OPTIONS-002B-OAD
**Date:** 2026-05-01
**Status:** PROPOSED - decision framing only. No architecture choice is locked by this document.
**Related:** [DESIGN-OPTIONS-002B: Option B - Medium Custom Layer](./DESIGN-OPTIONS-002B-option-B-medium-custom-layer.md), [DESIGN-OPTIONS-002: Layer Thickness and Boundary Placement](./DESIGN-OPTIONS-002-layer-thickness-and-boundary.md), [DESIGN-OPTIONS-003: Decomposition Strategy for the DCMS Application Layer](./DESIGN-OPTIONS-003-monolith-vs-microservices.md)

---

## Purpose

This note separates the Option B domain decision from the architecture decisions that remain open.

Option B means:

> DCMS owns meaningful custom domains on top of Solon.

Option B does not, by itself, decide:

- whether those domains run inside Solon's JVM or outside Solon,
- whether they are deployed as a monolith, coarse services, fine-grained microservices, or a hybrid,
- whether the interaction style is process-led, event-led, CQRS-style, synchronous service-led, or hybrid,
- whether DCMS persists domain state in one schema, per-domain schemas, a separate database, Solon Data Area, or a mix.

The current Option B documents contain candidate architecture text. That text should be read as design input, not as a locked conclusion.

---

## Option B Domain Boundary

Under Option B, the following DWP-specific capabilities are large enough to be treated as DCMS-owned custom domains or domain services rather than thin Solon configuration:

| Domain area | Why it is custom-domain territory |
|---|---|
| Vulnerability management | DWP-specific capture, classification, suppression, reassignment, review, and specialist-routing behaviour. |
| Breathing Space and restriction gating | Runtime gate-at-effect decisions, MHCM handling, deferred-output flags, and compliance evidence. |
| Income and expenditure | DWP affordability model, CFS-style data capture, thresholds, disposable-income calculation, and repayment option generation. |
| Champion/challenger | Assignment, exclusion, promotion, harm indicators, outcome tracking, and statistical comparison. |
| Strategy lifecycle and segmentation | Versioned strategy configuration, review, approval, rollback, and treatment-path selection. |
| Work allocation overlays | DWP queue rules, vulnerability routing, supervisor override, SLA views, and operational controls. |
| Communications suppression and orchestration | Category evaluation, suppression, queued-output disposition, channel coordination, and contact-frequency limits. |
| Third-party collection placement | Pre-placement disclosure, DCA selection, package generation, recall, and reconciliation. |
| Audit and evidence views | Reconstructable DCMS decision history across custom domains and Solon interactions. |

This is the decision that differentiates Option B from Option A. Option A is thin extension/configuration. Option B is domain-thick.

---

## Open Architecture Decisions

### 1. Runtime Placement

Question:

> Do Option B custom domains run inside Solon, outside Solon, or split between both?

Candidate shapes:

| Shape | Description | Main benefit | Main risk |
|---|---|---|---|
| Inside Solon JVM | DCMS code lives in Solon custom extension artefacts such as `revenue-management-be-custom`. | Lowest latency to Solon internals; can participate in Solon-side transactions where supported. | Strong upgrade and release-cadence coupling to Solon; DCMS code can drift into Solon internals. |
| Outside Solon | DCMS is its own application layer and calls Solon through governed REST/Kafka/API surfaces. | Clear ownership, deployment, rollback, and audit boundary for DCMS. | Requires stable Solon contracts; creates network and consistency boundaries. |
| Split placement | Only Solon-owned transactional hooks live inside Solon; DCMS-owned domains live outside. | Keeps exceptional Solon-coupled logic close to Solon while preserving DCMS ownership elsewhere. | Boundary erosion risk; requires a strict extension register and placement criteria. |

Decision evidence needed:

- Confirm which Solon extension points are supported and how they are deployed.
- Confirm whether Solon REST/Kafka surfaces are stable enough for an outside DCMS layer.
- Identify which behaviours genuinely need Solon transaction participation.
- Identify which compliance behaviours must remain DCMS-releaseable and auditable independently.

### 2. Decomposition

Question:

> Are the Option B custom domains one deployable, coarse services, fine-grained microservices, or a hybrid?

Candidate shapes:

| Shape | Description | Main benefit | Main risk |
|---|---|---|---|
| Single modular application | One DCMS deployable with strict internal domain boundaries. | Simple deployment and low-latency cross-domain gates. | Larger blast radius unless module boundaries and release controls are strong. |
| Coarse services | A few deployables aligned to ownership or runtime groups, such as Runtime, Configuration, Integrations, Analytics. | Some independent release and operational isolation without full microservice overhead. | Boundary placement may split compliance gates or workspace render paths. |
| Fine-grained microservices | Each custom domain or capability becomes a separate deployable service. | Maximum independent deployment and scaling. | Highest operational cost; distributed consistency and contract burden. |
| Hybrid | Core compliance/runtime domains in one shape, with satellites for adapters, analytics, or batch-like workloads. | Lets operationally distinct workloads separate without fragmenting core decisions. | Requires clear extraction criteria and ownership discipline. |

Decision evidence needed:

- Determine whether domains have independent owners and release cadences.
- Identify domains with distinct scaling, runtime, or security requirements.
- Define which compliance gates must stay atomic and which can cross a network boundary.
- Confirm local/dev deployment overhead under the starter-pack DevOps standards.

### 3. Interaction Style

Question:

> Is Option B process-led, event-led, CQRS-style, synchronous service-led, or hybrid?

Candidate styles:

| Style | Good fit | Caution |
|---|---|---|
| Process-led | Long-running case and debt lifecycle steps, approvals, timers, and human tasks. | Amplio constraints may limit concurrency, compensation, and non-interrupting event behaviour. |
| Event-led | Integration notifications, audit signals, outcome updates, analytics, and asynchronous handoffs. | Not suitable where immediate compliance decisions require fresh state. |
| CQRS-style | Workspace read models, dashboards, operational summaries, and analytics views. | Read projections must not become the source for compliance-critical gates unless freshness is guaranteed. |
| Synchronous service-led | Runtime gates, arrangement creation checks, vulnerability reassignment, and debtor-facing action blocking. | Can create latency and coupling if overused across service boundaries. |
| Hybrid | Most likely candidate: use each style where its failure mode is acceptable. | Requires explicit rules so builders know when each style is allowed. |

Decision evidence needed:

- Enumerate compliance decisions that must be synchronous and fresh.
- Identify read views that can tolerate eventual consistency.
- Confirm Solon event topics, response events, and correlation semantics.
- Confirm whether DCMS process definitions can safely run in shared Amplio.

### 4. Data Ownership

Question:

> Where does Option B store and expose custom-domain state?

Candidate shapes:

| Shape | Description | Main benefit | Main risk |
|---|---|---|---|
| Single `dcms` schema | One DCMS schema for all custom-domain tables. | Simple boundary from Solon. | Weaker per-domain extraction boundary. |
| Per-domain DCMS schemas | Separate schema per custom domain. | Stronger ownership and future extraction path. | More migration and permission discipline. |
| Separate DCMS database | DCMS owns its database outside Solon persistence. | Strong runtime and migration boundary. | Requires robust Solon integration and reconciliation. |
| Solon Data Area | Selected fields stored on Solon entities as JSON custom fields. | Makes values visible to Solon/Amplio where supported. | Staleness, ownership ambiguity, and weak typing. |
| Mixed model | DCMS-owned source tables plus selective Solon mirroring. | Pragmatic if Solon needs visibility into selected state. | Requires source-of-truth rules and outbox/reconciliation design. |

Decision evidence needed:

- Declare source of truth for each custom-domain entity.
- Decide whether vulnerability and restriction state can be mirrored or must be read live.
- Review Liquibase/Flyway and migration ownership constraints.
- Decide whether cross-schema SQL is allowed, forbidden, or exception-based.

### 5. Workflow Ownership

Question:

> Are DWP workflows authored in Solon/Amplio, in DCMS application code, or split?

Candidate shapes:

| Shape | Description | Main benefit | Main risk |
|---|---|---|---|
| Solon/Amplio-owned workflows | DWP BPMN definitions run in Solon's process engine. | Reuses Solon process tooling, admin UI, timers, and task patterns. | Depends on Amplio feature limits and process isolation support. |
| DCMS-owned orchestration | DCMS application owns workflow/state transitions. | Keeps DWP lifecycle under DCMS release and test control. | Rebuilds process capabilities Solon may already provide. |
| Split workflow | Solon handles Solon-native flows; DCMS handles DWP-specific orchestration and calls Solon primitives. | Preserves platform reuse while keeping DWP decisions explicit. | Requires clear handoff and correlation model. |

Decision evidence needed:

- Confirm whether DCMS-defined BPMN can be safely deployed into shared Amplio.
- Confirm whether custom event types and Eventplans can be registered for DCMS.
- Decide how gate-at-effect requirements work with Amplio limitations.
- Identify which flows are genuinely long-running workflow versus service-state transitions.

### 6. Security and Authorisation Boundary

Question:

> How are Keycloak, OPA, row-level security, and DCMS-specific policy distributed across the chosen runtime/deployment shape?

Decision evidence needed:

- Decide whether DCMS has its own Keycloak clients, realms, or policy model.
- Decide whether OPA is central, sidecar-per-service, or Solon-provided only.
- Define row-level/data-scope enforcement for DCMS custom domains.
- Decide whether vulnerability/MHCM data needs process-level isolation.

### 7. Upgrade and Release Boundary

Question:

> What coupling to Solon is acceptable, and what must survive Solon upgrades?

Decision evidence needed:

- Identify every use of Solon internals versus supported contracts.
- Define upgrade smoke tests and contract tests.
- Decide whether DCMS custom extensions follow Solon release cadence or DCMS release cadence.
- Define rollback strategy for DCMS-only changes and Solon-coupled changes.

---

## Decision Dependencies

The questions above should be decided in this order:

1. Confirm Option B domain scope: which custom domains are in scope for Release 1.
2. Decide runtime placement: inside Solon, outside Solon, or split.
3. Decide decomposition: monolith, coarse services, microservices, or hybrid.
4. Decide workflow ownership: Amplio, DCMS orchestration, or split.
5. Decide interaction style: where synchronous, event-led, process-led, and CQRS-style patterns are allowed.
6. Decide data ownership and migration model.
7. Decide security, observability, deployment, and upgrade guardrails for the chosen shape.

---

## Non-Decisions

The following must not be inferred from Option B until explicitly decided:

- Option B does not mean "inside Solon's JVM."
- Option B does not mean "outside Solon."
- Option B does not mean "modular monolith."
- Option B does not mean "microservices."
- Option B does not mean "event-sourced."
- Option B does not mean "pure CQRS."
- Option B does not mean all DCMS BPMN runs in Amplio.

---

## Required Follow-Up

Before Option B can be treated as implementation-ready, produce a decision record or locked design note covering:

1. Runtime placement.
2. Decomposition.
3. Interaction style.
4. Workflow ownership.
5. Data ownership.
6. Security and authorisation boundary.
7. Upgrade and release boundary.

Until those are resolved, Option B should be described as **domain-thick but deployment-neutral**.
