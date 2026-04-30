# DESIGN-OPTIONS-003: Monolith vs Microservices for the Business Control & Experience Layer

**Document ID:** DESIGN-OPTIONS-003
**Date:** 2026-04-30
**Status:** PROPOSED — for Solution Architect review
**Author:** Delivery Designer
**Scope:** Decomposition strategy for the DCMS custom layer ("Business Control & Experience Layer") that sits on top of Solon Tax. Does not address Solon Tax internal architecture — Solon is microservices and that is fixed.
**Related:** DESIGN-OPTIONS-002 (layer thickness — recommends Option B medium layer); `configuration-layer-architecture v3.drawio`.

---

## Recommendation (read this first)

**Recommend a modular monolith for the DCMS custom layer, with a small number (2–3) of deliberately extracted satellite services where operational drivers force it.** Build internal module boundaries strictly from day one so a service can be carved out later without a rewrite. Do not adopt the ~20-service decomposition implied by the v3 architecture diagram.

The recommendation flips toward broader microservices only if (a) team size grows beyond ~3 squads with clear bounded ownership, (b) a service has a fundamentally different scaling profile, runtime, or release cadence, or (c) a security or blast-radius requirement forces isolation.

---

## Context

The DCMS custom layer is the new code we write on top of Solon Tax. It contains: configuration workspaces (Strategy, Tasks, Policy Bundles), runtime workspaces (Case Worker, Operations), a BFF tier, and core services (Contact & Communications, Work/Queue/Agent, Risk/I&E/Compliance, Strategy/Analytics/Third-Party Collection).

Solon Tax itself is a microservices platform — that is given. The question here is **how the DCMS layer should be decomposed**, not how Solon should be decomposed.

The v3 configuration-layer architecture diagram shows ~20 services arranged in four groups. That diagram is a *capability map*, not yet a deployment-unit decision. This document evaluates whether each capability needs to be a separately deployed service.

---

## Options

### Option 1 — Full microservices (one service per capability box)

Each box on the v3 diagram becomes its own deployable service: Contact Orchestration, Channel Adapters, Dialler Integration, Suppression, Template Mgmt, Queue Distribution, SLA Tracking, Agent Scripting, Work Item Assignment, Real-time Queue State, Vulnerability Protocol, Breathing Space Handler, I&E Capture, CRA Integration, Vulnerability Review Scheduler, Split Assignment, Outcome Aggregation, Metrics Collection, Third-Party Placement, Third-Party Reconciliation — plus the workspaces and BFFs.

### Option 2 — Modular monolith with satellite services *(recommended)*

A single Spring Boot application containing all capabilities, organised as strict internal modules (hexagonal / package-by-feature, enforced by ArchUnit or Spring Modulith). Separate PostgreSQL schemas per module within one database instance. **Plus 2–3 satellite services** carved out for genuine operational reasons (see "Recommended satellites" below).

### Option 3 — Coarse-grained services aligned to workspaces

Three-to-four services aligned to the high-level groupings: Configuration Service, Runtime Service (Case Worker + Ops), Integration Service (third-party collection, dialler, channel adapters), Analytics Service. A middle ground.

---

## Findings — why microservices is the wrong default for this layer

### Finding 1: The team is one team, not twenty

Microservices economics assume roughly one team per service. With one delivery team owning the DCMS custom layer, every service boundary becomes a coordination cost paid by the same engineers — meetings about contracts they wrote on both sides, version-skew between services they themselves deploy, integration tests across services they themselves run.

**Why this matters:** The "independent deployability" benefit of microservices is monetised by independent teams. One team gets the cost without the benefit.

### Finding 2: The capabilities change together

A change to debt strategy typically touches Strategy Engine, Step Library, Queue Distribution, Contact Orchestration, Vulnerability gating, and Audit simultaneously. A change to Breathing Space rules touches Suppression, Contact, Queue, Audit, and Communications.

**Why this matters:** When deploys are coupled, splitting services produces choreographed multi-service releases, feature flags spanning services, and "did service X get the new contract version?" incidents. The complexity is paid daily.

### Finding 3: Statutory and regulatory invariants cross capability boundaries

- **RULING-016** (Breathing Space gating) requires gate evaluation at every debtor-facing effect — Contact, Strategy, Arrangement, Communications, Third-Party Placement.
- **RULING-010** (champion/challenger) requires *immediate* reassignment from CHALLENGER to CHAMPION on a vulnerability status change.
- **RULING-014** (arrangement creation) requires querying the live suppression log at the point of arrangement creation.

These invariants demand atomic, low-latency, in-process evaluation. Splitting Vulnerability, Breathing Space, Suppression, and Strategy across services turns an in-process method call into a distributed-transaction problem (sagas, compensation, idempotency keys, eventual-consistency windows). The integration design lock for DESIGN-OPTIONS-002 is *already* blocked partly because Solon's compensation events are not implemented and Amplio's parallel gateway is sequential — adding more network hops at this layer makes those constraints harder to live with.

**Why this matters:** Distributed systems are appropriate when consistency can be relaxed. DCMS's compliance posture requires the opposite.

### Finding 4: Latency in the workspace render path

Case Worker Workspace already aggregates from multiple capabilities through the BFF: account view, vulnerability flags, suppressions, tasks, scripted journeys, arrangement state, audit-visible actions. Each capability becoming a network hop multiplies latency, error surface, and timeout-handling complexity.

**Why this matters:** Frontline agents in a contact centre measure productivity in seconds. A 200ms render becomes a 1.5s render once it fans out across services and waits for the slowest tail.

### Finding 5: Operational cost scales with service count, not feature count

Twenty services means twenty deploy pipelines, twenty Helm charts, twenty alert rule sets, twenty dashboards, twenty secret rotations, twenty health checks, twenty service-mesh policies, twenty consumer-driven contract test suites. The platform-engineering load on the one team that runs DCMS is fixed cost per service, paid forever.

**Why this matters:** This is the largest hidden cost of microservices. It does not shrink as the team learns; it grows linearly with service count.

### Finding 6: Solon already provides the heavy boundaries

Solon owns ledger, payment, write-off, BPMN engine, suppression model, task tray, batch engine, Kafka bus. The DCMS layer's job is *coordination and policy on top of* those primitives. Coordination logic is exactly the kind of code that hates being split — every split introduces a network boundary across what is conceptually one decision.

**Why this matters:** The platform already gives you the macro service boundaries. The micro splits inside the DCMS layer add cost without adding architectural value the platform doesn't already deliver.

### Finding 7: The "future scale" argument is weak here

DCMS is a single-tenant DWP system with a knowable, bounded user population (DWP debt management agents and supervisors) and a knowable transaction profile (UK debt cases). It is not an internet-scale workload that needs per-capability horizontal scaling. The few capabilities that *do* have unusual load profiles (Kafka stream consumer for analytics, dialler/CTI integration, batch file generation) are already candidates for satellite extraction.

**Why this matters:** "We might need to scale" is the most common reason given for premature microservices. Here the scale envelope is constrained, so the argument has little force.

---

## Findings — when extraction *is* justified

A capability earns its own service when at least one of these is true. None of them apply to most DCMS capabilities; a few apply to specific ones.

| Driver | Applies to |
|---|---|
| **Different scaling profile** — bursty, hot, or sized very differently from the rest | Dialler/CTI integration (call-arrival bursts), Kafka metrics consumer (continuous stream, bursty backfill) |
| **Different release cadence or ownership** — owned by a different team or governed by a different change-control regime | Third-party collection file generation (often integrations team), Channel adapters (sometimes vendor-coupled) |
| **Different runtime model** — needs a different lifecycle, language, or framework than a Spring Boot web app | Kafka consumer (long-running stream worker), Batch jobs (scheduled, not request/response) |
| **Different blast radius / security boundary** — handles externally-trusted data or partner credentials and must be isolated | Third-party (DCA) integration, Bureau feed ingestion |
| **Different data sensitivity** — Restricted-class data that requires hardened isolation | Vulnerability or MHCM data — but isolation here is achieved through schema-level and OPA controls inside the monolith, not via a separate service |

### Recommended satellites (from this list)

1. **Kafka Metrics Consumer** — long-running stream worker; different runtime; should not share a JVM with the request-handling app. Reads Solon's Kafka stream, writes to Analytics DB.
2. **Dialler / CTI Adapter** — bursty load profile, vendor-coupled lifecycle, often needs a network position closer to the dialler vendor.
3. **(Conditional) Third-Party Collection Integration** — extract if the integrations team owns it, or if DCA partner credentials and SFTP key handling justify isolation. Otherwise keep in the monolith.

Everything else stays in the modular monolith for v1.

---

## How to keep the door open for future extraction

A modular monolith only retains optionality if its internal boundaries are *real*. Without discipline, it becomes a ball of mud and extraction becomes a rewrite.

### Required from day one

1. **Package-by-feature, not by-layer.** Modules are domain-aligned (`vulnerability`, `breathingspace`, `strategy`, `queue`, `contact`), not technical-aligned (`controllers`, `services`, `repositories`).
2. **Hexagonal boundaries per module.** Each module exposes a port interface; other modules call only the port, never internal classes.
3. **Build-time enforcement.** ArchUnit rules or Spring Modulith forbid cross-module access except through declared ports. Violations break CI.
4. **Separate PostgreSQL schemas per module.** No cross-schema joins. Enforced at the role/permission level so the application user for module A cannot read module B's tables directly. This is the single biggest determinant of whether extraction is possible later — once cross-schema joins exist, extraction is a multi-quarter project.
5. **Per-module Liquibase changelogs.** A module's schema migrations live with the module, not in a central pile.
6. **Per-module integration tests.** Each module has a test suite that exercises it through its port, with the rest of the system stubbed.
7. **No shared mutable domain entities across modules.** Modules exchange DTOs, not JPA entities. An entity defined in module A is never imported by module B.
8. **Audit and observability tagged per module.** Logs and metrics carry the originating module name so operational visibility is per-capability even though deployment is unified.

### Extraction protocol when a satellite needs to be carved off

1. Confirm one of the four extraction drivers applies and is durable, not a passing pressure.
2. The module's port interface becomes the new service's API contract. No new contract design needed if the port discipline was kept.
3. The module's schema becomes the new service's database (or a defined slice of it).
4. Replace in-process calls to the port with HTTP/Kafka calls, behind the same port interface, so callers do not change.
5. Move the module's Liquibase changelog to the new service.
6. Stand up the new service's pipeline, alerts, dashboards, runbook.

If steps 2–4 cannot be done in a single sprint, the modular boundaries were not real. That is the test.

---

## Tradeoff summary

| Dimension | Option 1 — Full Microservices | Option 2 — Modular Monolith + Satellites *(recommended)* | Option 3 — Coarse Services |
|---|---|---|---|
| Time to first working system | Slowest | Fastest | Medium |
| Operational cost | Highest (~20 pipelines, dashboards, alerts) | Lowest (1 + 2–3 satellites) | Medium (~4) |
| Workspace render latency | Highest (multi-hop fan-out) | Lowest (in-process) | Medium |
| Compliance invariant safety (RULING-010, 014, 016) | Worst — distributed transactions across gates | Best — atomic in-process gates | Acceptable if gates kept on one side |
| Coordination tax for one team | Highest | Lowest | Medium |
| Independent deploy value (with one team) | Negative — you pay the cost without using the benefit | N/A — single deploy unit | Low |
| Independent scaling | Per service | Only for satellites | Per coarse group |
| Extraction optionality if needs change | Already extracted | Strong if module discipline is enforced | Partial |
| Blast radius of a bad change | Per-service blast | Whole monolith — mitigated by feature flags + canary deploys | Per coarse group |
| Skill demands on the team | Distributed systems daily | Modular design discipline daily | Distributed systems weekly |
| Risk profile | Operational complexity, latency, distributed-transaction bugs | Module discipline erosion over time | Boundary placement may be wrong |

---

## Decision levers — what would change the recommendation

### Lever A: Team grows beyond ~3 squads with clear bounded ownership

If DCMS is delivered by multiple co-funded squads with durable bounded ownership (e.g. one squad owns Strategy & Configuration, another owns Runtime & Workspaces, a third owns Integrations), Option 3 (coarse services) becomes attractive because each squad gets independent deploy. **Modular-monolith → coarse services along squad lines.**

### Lever B: A capability shows a fundamentally different scaling or runtime profile

Already addressed by the satellite list. If a new capability emerges with these traits, extract it as a satellite. The recommendation does not change in shape.

### Lever C: A security or blast-radius requirement forces isolation

Vulnerability or MHCM data handling under RULING-002 / GDPR Art. 9 may require process-level isolation rather than schema-level. If an ICO or DWP security review concludes that schema-level isolation is insufficient, the affected module is extracted as a hardened satellite with its own JVM, secrets, and network position. The rest of the recommendation is unchanged.

### Lever D: Solon's contract surface forces it

DESIGN-OPTIONS-002 Blocker 1 (Kafka command catalogue uncertainty) and Blocker 2 (sync REST stability) are unresolved. If the resolution forces DCMS to wrap each Solon capability in a per-capability adapter for upgrade-survival reasons, those adapters might naturally become satellite services. This would shift Option 2 toward Option 3 along the integration boundary, not toward Option 1.

---

## Open questions for the Solution Architect

1. Is the DCMS custom layer expected to be delivered by one team, multiple squads, or a federated set of teams? The decomposition decision should not be locked without this answer.
2. Does the security review for Vulnerability and MHCM data require process-level isolation, or is schema + OPA isolation sufficient?
3. Will third-party collection integration be owned by the DCMS team or by an integrations team? If the latter, extract as a satellite from day one.
4. Is the Kafka Metrics Consumer expected to handle backfill from historical Solon Kafka topics, or only live stream? Backfill load profile would tilt the case for extraction even harder.
5. Are there latency budgets for the Case Worker workspace render that would be violated by network fan-out?

---

## What this means for the v3 diagram

The v3 diagram remains valid as a **capability map** — it shows what the layer must do. It should not be read as a deployment-unit map. Before lock, the diagram should be annotated to distinguish:

- **Modules inside the monolith** (most boxes)
- **Satellite services** (Kafka Metrics Consumer; Dialler/CTI Adapter; conditionally Third-Party Collection)
- **BFF endpoints** within the monolith (not separate services)

This avoids the architectural-aspiration trap where boxes on a diagram are quietly read by the build team as "deployable units."

---

## Recommendation restated

**Adopt Option 2: a modular monolith for the DCMS custom layer, with strict module discipline enforced at build time, separate PostgreSQL schemas per module, and 2–3 satellite services extracted only where operational drivers genuinely justify them. Reassess the decomposition at the end of v1 against actual operational pain, not against architectural fashion.**
