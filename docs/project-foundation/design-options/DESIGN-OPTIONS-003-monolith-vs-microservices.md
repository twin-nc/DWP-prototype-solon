# DESIGN-OPTIONS-003: Decomposition Strategy for the DCMS Application Layer
*(formerly: "Monolith vs Microservices for the Business Control & Experience Layer")*

**Document ID:** DESIGN-OPTIONS-003
**Date:** 2026-04-30 (updated 2026-05-01 with Solon Tax platform expert review)
**Status:** PROPOSED — for Solution Architect review. Option 2 approved with constraints by Solon Tax platform expert; Option 1 eliminated on platform grounds; Option 3 conditionally approved.
**Author:** Delivery Designer
**Status clarification:** No decomposition option is locked. The platform-expert verdicts in this document are design inputs, not final decisions.
**Reviewer:** Solon Tax platform expert (2026-05-01)
**Scope:** Decomposition strategy for the DCMS custom layer ("Business Control & Experience Layer") that sits on top of Solon Tax. Does not address Solon Tax internal architecture — Solon is microservices and that is fixed.
**Related:** DESIGN-OPTIONS-002 (layer thickness — recommends Option B medium layer); `configuration-layer-architecture v3.drawio`.

---

## Provisional Recommendation (not locked)

This document evaluates decomposition candidates for an Option B-style DCMS custom-domain layer. It does not lock modular monolith, coarse services, or microservices.

The recommendation language below is provisional design input. It must be re-tested against the still-open Option B architecture decisions: runtime placement, workflow ownership, interaction style, data ownership, security boundary, and Solon contract stability.

**Candidate recommendation:** modular monolith for the DCMS custom layer, with a small number (2-3) of deliberately extracted satellite services where operational drivers force it. Build internal module boundaries strictly from day one so a service can be carved out later without a rewrite. Do not adopt the ~20-service decomposition implied by the v3 architecture diagram unless the open decomposition decision lands on fine-grained microservices.

The argument for Option 2 rests on **compliance invariant atomicity** (RULING-010, 014, 016 require in-process gate evaluation), **change-coupling of capabilities** (most non-trivial changes touch many capabilities at once), and **the platform already providing the macro service boundaries** (Solon Tax owns ledger, payment, BPMN, suppression model). These properties hold regardless of the delivery team's size or shape. Per ADR-018, team size is not used as an architectural constraint in this recommendation.

The recommendation flips toward broader microservices only if (a) a service has a fundamentally different scaling profile, runtime, or release cadence, or (b) a security or blast-radius requirement forces isolation, or (c) bounded-context ownership of distinct capability groups becomes durable enough that coordination overhead dominates compliance-coupling benefits — see Lever A.

> ⚠ **Two blockers must be resolved before this recommendation can be locked.** See "Blockers before lock" below: (i) the `revenue-management-be-custom` boundary must be defined; (ii) the RULING-016 gate fallback pattern must be confirmed in light of Amplio 2.3.0's lack of non-interrupting boundary events.

---

## Context

The DCMS custom layer is the new code we write on top of Solon Tax. It contains: configuration workspaces (Strategy, Tasks, Policy Bundles), runtime workspaces (Case Worker, Operations), a BFF tier, and core services (Contact & Communications, Work/Queue/Agent, Risk/I&E/Compliance, Strategy/Analytics/Third-Party Collection).

Solon Tax itself is a microservices platform — that is given. The question here is **how the DCMS layer should be decomposed**, not how Solon should be decomposed.

The v3 configuration-layer architecture diagram shows ~20 services arranged in four groups. That diagram is a *capability map*, not yet a deployment-unit decision. This document evaluates whether each capability needs to be a separately deployed service.

### Scope: two distinct DCMS code locations

DCMS-authored code can live in two architecturally distinct locations:

- **(a) Inside Solon Tax services**, via the `revenue-management-be-custom` extension repository (Solon's documented mechanism for client-specific backend extensions running inside Solon's own service boundary).
- **(b) Outside Solon Tax**, as a separate DCMS application (or applications) that calls Solon over Kafka and REST.

**This document addresses (b) only.** For logic that lives in (a), decomposition is governed by Solon's own service structure and is not a DCMS architectural choice. The boundary between (a) and (b) — *which* DCMS logic belongs where — is itself an architectural decision and is documented in the new section below ("DCMS application boundary vs `revenue-management-be-custom` extensions"). Until that boundary is fixed, the options below are necessarily provisional for any logic that could plausibly sit on either side.

---

## DCMS application boundary vs `revenue-management-be-custom` extensions

Before evaluating decomposition options for the DCMS application (b), the project must decide which DCMS logic belongs in (a) — i.e. which logic is implemented as a Solon custom-extension and which is implemented in the DCMS application calling Solon from outside.

### Decision criteria

DCMS logic should be implemented inside Solon (`revenue-management-be-custom`) when **all** of the following hold:

1. The logic operates on Solon-owned aggregates (ledger entries, payment allocations, suppression records, write-off records, Solon task tray items) and would otherwise require chatty cross-boundary calls to read/mutate them.
2. The logic must execute synchronously within a Solon transaction or BPMN activity to maintain a Solon-side invariant (e.g. an allocation rule that must hold before a payment row is committed).
3. The logic does not require state, configuration, or workflow that lives only in DCMS (e.g. DCMS-specific strategy bundles, vulnerability protocol state, queue assignment state).

DCMS logic should be implemented in the DCMS application (b) when **any** of the following hold:

1. The logic depends on DCMS-owned state (strategy decisions, queue/agent assignment, vulnerability gating, contact orchestration, workspace presentation).
2. The logic spans Solon plus non-Solon integrations (DCA, dialler, communications channels not owned by Solon).
3. The logic embodies a regulatory invariant that must remain testable, auditable, and version-controllable independently of Solon's own release cadence (RULING-010, RULING-014, RULING-016).
4. The logic is part of a debtor-facing experience (Case Worker workspace, Operations workspace, configuration workspaces).

### Implications for this document

The recommendation (Option 2) addresses (b) only. If a future review concludes that, for example, a Breathing Space gate check belongs in (a) for transactional reasons, the gate's *implementation* moves into the Solon custom-extension layer; the DCMS application's responsibility for *coordinating* the gate at debtor-facing effects does not. The decomposition choice for the DCMS application is unchanged in shape.

This boundary must be documented in a separate ADR (proposed: ADR-019) before build begins. Until then, treat the (a)/(b) split as **open** and avoid putting any RULING-bound logic into a Solon custom-extension without explicit Solution Architect sign-off.

---

## Options

The labels below are decomposition candidates. Do not treat the "recommended" marker on Option 2 as a locked decision; it is a provisional recommendation pending the open Option B architecture questions.

### Option 1 — Full microservices (one service per capability box)

Each box on the v3 diagram becomes its own deployable service: Contact Orchestration, Channel Adapters, Dialler Integration, Suppression, Template Mgmt, Queue Distribution, SLA Tracking, Agent Scripting, Work Item Assignment, Real-time Queue State, Vulnerability Protocol, Breathing Space Handler, I&E Capture, CRA Integration, Vulnerability Review Scheduler, Split Assignment, Outcome Aggregation, Metrics Collection, Third-Party Placement, Third-Party Reconciliation — plus the workspaces and BFFs.

### Option 2 — Modular monolith with satellite services *(candidate)*

A single Spring Boot application containing all capabilities, organised as strict internal modules (hexagonal / package-by-feature, enforced by ArchUnit or Spring Modulith). Separate PostgreSQL schemas per module within one database instance. **Plus 2–3 satellite services** carved out for genuine operational reasons (see "Recommended satellites" below).

### Option 3 — Coarse-grained services aligned to workspaces

Three-to-four services aligned to the high-level groupings: Configuration Service, Runtime Service (Case Worker + Ops), Integration Service (third-party collection, dialler, channel adapters), Analytics Service. A middle ground.

---

## Findings — why microservices is the wrong default for this layer

### Finding 1: Independent deployability is monetised by independent ownership, not by service count

Microservices economics assume one *bounded-context owner* per service — a team or squad that owns its release cadence, its on-call, and its contract evolution. Where a single owner spans many services, every service boundary becomes coordination cost paid by the same group: meetings about contracts they wrote on both sides, version-skew between services they themselves deploy, integration tests across services they themselves run.

**Why this matters:** The benefit of fine-grained services scales with how many *independent* owners exist for the resulting services, not with how many services exist. Per ADR-018, team size is not an input to this decision — but the underlying property (whether the DCMS custom layer has multiple bounded-context owners with durable independence) is. If the answer is "one bounded owner," fine decomposition pays cost without delivering benefit. If the answer becomes "several bounded owners over time," see Lever A.

### Finding 2: The capabilities change together

A change to debt strategy typically touches Strategy Engine, Step Library, Queue Distribution, Contact Orchestration, Vulnerability gating, and Audit simultaneously. A change to Breathing Space rules touches Suppression, Contact, Queue, Audit, and Communications.

**Why this matters:** When deploys are coupled, splitting services produces choreographed multi-service releases, feature flags spanning services, and "did service X get the new contract version?" incidents. The complexity is paid daily.

### Finding 3: Statutory and regulatory invariants cross capability boundaries

- **RULING-016** (Breathing Space gating) requires gate evaluation at every debtor-facing effect — Contact, Strategy, Arrangement, Communications, Third-Party Placement.
- **RULING-010** (champion/challenger) requires *immediate* reassignment from CHALLENGER to CHAMPION on a vulnerability status change.
- **RULING-014** (arrangement creation) requires querying the live suppression log at the point of arrangement creation.

These invariants demand atomic, low-latency, in-process evaluation. Splitting Vulnerability, Breathing Space, Suppression, and Strategy across services turns an in-process method call into a distributed-transaction problem (sagas, compensation, idempotency keys, eventual-consistency windows). The integration design lock for DESIGN-OPTIONS-002 is *already* blocked partly because Solon's compensation events are not implemented and Amplio's parallel gateway is sequential — adding more network hops at this layer makes those constraints harder to live with.

**Why this matters:** Distributed systems are appropriate when consistency can be relaxed. DCMS's compliance posture requires the opposite.

**RULING-016 implementation pattern (consequence of this finding combined with Amplio constraints).** Amplio 2.3.0 does not support non-interrupting message boundary events (see "Open blockers requiring escalation" in the platform expert review). RULING-016 therefore cannot be implemented as a BPMN process event in Amplio. The intended pattern is: **gate evaluation runs as synchronous in-process application logic in the DCMS monolith, invoked at every debtor-facing effect (Contact, Strategy execution, Arrangement creation, Communications dispatch, Third-Party Placement) before the effect is committed.** The gate reads suppression state from the Solon-owned suppression model via a fast, cached read path. This pattern is only safely implementable if the evaluating code shares a process with the code committing the effect — i.e., it depends on Option 2. Builders must not attempt to implement the gate as an Amplio boundary event.

### Finding 4: Latency in the workspace render path

Case Worker Workspace already aggregates from multiple capabilities through the BFF: account view, vulnerability flags, suppressions, tasks, scripted journeys, arrangement state, audit-visible actions. Each capability becoming a network hop multiplies latency, error surface, and timeout-handling complexity.

**Why this matters:** Frontline agents in a contact centre measure productivity in seconds. A 200ms render becomes a 1.5s render once it fans out across services and waits for the slowest tail.

### Finding 5: Operational cost scales with service count, not feature count

Twenty services means twenty deploy pipelines, twenty Helm charts, twenty alert rule sets, twenty dashboards, twenty secret rotations, twenty health checks, twenty service-mesh policies, twenty consumer-driven contract test suites, plus — per the Solon Tax operations guide — twenty OPA policy distributions (sidecar or central) and twenty Keycloak confidential-client registrations. The platform-engineering load is fixed cost per service, paid forever.

**Why this matters:** This is the largest hidden cost of microservices. It is paid by whoever is operating DCMS, regardless of how that delivery is organised. It does not shrink as the team learns; it grows linearly with service count.

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
2a. **Extract the module's OPA Rego policies and configure either a dedicated OPA sidecar for the new service or a shared OPA instance reachable from it (per Solon Tax operations guide §4.11). Authorisation enforcement must be live before the new service accepts traffic.** Register the new service in Keycloak as an OAuth2 confidential client and ensure all incoming and outgoing calls carry valid Client Credentials grant tokens.
3. The module's schema becomes the new service's database (or a defined slice of it).
4. Replace in-process calls to the port with HTTP/Kafka calls, behind the same port interface, so callers do not change.
5. Move the module's Liquibase changelog to the new service.
6. Stand up the new service's pipeline, alerts, dashboards, runbook.

If steps 2–4 cannot be done in a single sprint, the modular boundaries were not real. That is the test.

---

## Tradeoff summary

| Dimension | Option 1 — Full Microservices | Option 2 — Modular Monolith + Satellites *(candidate)* | Option 3 — Coarse Services |
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

### Lever A: Multiple durable bounded-context owners emerge for distinct capability groups

If DCMS gains multiple bounded-context owners with durable, independent responsibility for distinct capability groups (e.g. one owner for Strategy & Configuration, one for Runtime & Workspaces, one for Integrations), Option 3 (coarse services) becomes more attractive because each owner gets independent deploy and on-call. The compliance-coupling argument (Finding 3) still constrains how the boundary may be drawn — gate logic, suppression evaluation, and vulnerability protocol enforcement must remain on one side of any coarse split, not crossing it. **If this lever activates, modular-monolith → coarse services *along ownership lines, not capability lines*, with compliance gates kept atomic on whichever side hosts the debtor-facing effect.**

Per ADR-018, this lever is framed in terms of ownership structure, not team size or headcount.

### Lever B: A capability shows a fundamentally different scaling or runtime profile

Already addressed by the satellite list. If a new capability emerges with these traits, extract it as a satellite. The recommendation does not change in shape.

### Lever C: A security or blast-radius requirement forces isolation

Vulnerability or MHCM data handling under RULING-002 / GDPR Art. 9 may require process-level isolation rather than schema-level. If an ICO or DWP security review concludes that schema-level isolation is insufficient, the affected module is extracted as a hardened satellite with its own JVM, secrets, and network position. The rest of the recommendation is unchanged.

### Lever D: Solon's contract surface forces it

DESIGN-OPTIONS-002 Blocker 1 (Kafka command catalogue uncertainty) and Blocker 2 (sync REST stability) are unresolved. If the resolution forces DCMS to wrap each Solon capability in a per-capability adapter for upgrade-survival reasons, those adapters might naturally become satellite services. This would shift Option 2 toward Option 3 along the integration boundary, not toward Option 1.

---

## Blockers before lock

The following must be resolved before the Solution Architect locks Option 2. Until they are, the recommendation stands as **provisional**.

1. **`revenue-management-be-custom` boundary defined in an ADR.** The (a)/(b) split for DCMS logic must be documented (proposed: ADR-019). Without it, builders will place compliance-bound logic on the wrong side of the boundary and refactoring will be expensive. See "DCMS application boundary vs `revenue-management-be-custom` extensions" above for criteria.
2. **RULING-016 gate fallback pattern confirmed.** Amplio 2.3.0 does not support non-interrupting message boundary events. The fallback pattern is in-process synchronous gates in the DCMS monolith (see Finding 3). This must be confirmed by the Solution Architect and, if necessary, escalated to the Solon Tax vendor for any platform-side mitigation. If the fallback is rejected, Option 2 itself is at risk and decomposition must be re-opened.

## Open questions for the Solution Architect

1. Does the security review for Vulnerability and MHCM data require process-level isolation, or is schema + OPA isolation sufficient?
2. Will third-party collection integration be owned by the DCMS bounded-context owner or by an integrations bounded-context owner? If the latter, extract as a satellite from day one.
3. Is the Kafka Metrics Consumer expected to handle backfill from historical Solon Kafka topics, or only live stream? Backfill load profile would tilt the case for extraction even harder.
4. Are there latency budgets for the Case Worker workspace render that would be violated by network fan-out? **This question must include the Drools KIE round-trip time as a component**, since DCMS calls to Solon's KIE rules endpoint are synchronous (operations guide; KIE is on the request path whenever DCMS evaluates a Solon-side rule). If KIE latency is material, Finding 4's argument applies inside Option 2 as well as against Option 1.
5. Does Amplio 2.3.0 support multi-tenant deployment of DCMS-defined BPMN process definitions into the shared engine? If not, DCMS process orchestration must be entirely in DCMS application code — which strengthens Option 2 but must be stated explicitly. Escalate to vendor.
6. Can DCMS-defined custom event types be registered in the Eventplans system parameter (integration guide §4.1)? If not, DCMS process triggering must use direct `StartProcessCommand` calls.

---

## What this means for the v3 diagram

The v3 diagram remains valid as a **capability map** — it shows what the layer must do. It should not be read as a deployment-unit map. Before lock, the diagram should be annotated to distinguish:

- **Modules inside the monolith** (most boxes)
- **Satellite services** (Kafka Metrics Consumer; Dialler/CTI Adapter; conditionally Third-Party Collection)
- **BFF endpoints** within the monolith (not separate services)

This avoids the architectural-aspiration trap where boxes on a diagram are quietly read by the build team as "deployable units."

---

## Recommendation Restated - Provisional

This section is not a lock. It records the current decomposition candidate to be tested after the Option B runtime placement, interaction style, workflow ownership, and data ownership decisions are made.

**Keep Option 2 as a candidate:** a modular monolith for the DCMS custom layer, with strict module discipline enforced at build time, separate PostgreSQL schemas per module, and 2-3 satellite services extracted only where operational drivers genuinely justify them. Do not adopt it until the open Option B architecture decisions are resolved. Reassess the decomposition against actual operational and compliance forces, not architectural fashion.

---

## Solon Tax Platform Expert Review (2026-05-01)

The Solon Tax platform expert reviewed this document against `external_sys_docs/solon/solon_tax_2.3.0_integration_guide.md`, `external_sys_docs/solon/solon_tax_2.3.0_operations_guide.md`, and the Amplio process engine reference. Findings reinforce the document's recommendation and add platform-grounded constraints that builders need before implementation begins.

### Per-option verdicts

| Option | Verdict | Platform reasoning |
|---|---|---|
| **Option 1 — Full microservices** | **ELIMINATED** | Amplio does not implement BPMN compensation events (integration guide §3.2, operations guide §4.6). Parallel gateways execute sequentially. A 20-service DCMS layer that fans out across services has no documented rollback path inside Solon Tax — the only Amplio-supported compensation mechanism is error boundary routing inside a BPMN process, meaning Amplio must own saga coordination. This is a correctness defect, not a performance issue. |
| **Option 2 — Modular monolith + satellites** | **APPROVED with constraints** | Platform-coherent. A Spring Boot monolith is a valid Kafka consumer and REST client; nothing in the Solon Tax docs constrains how DCMS-authored code is decomposed. Constraints below must be resolved before build. |
| **Option 3 — Coarse-grained services** | **CONDITIONALLY APPROVED** | Viable only if RULING-010 / RULING-014 / RULING-016 gate logic stays entirely on one side of the coarse boundary. Solon's suppression model is a fire-and-forget four-step Kafka command sequence with no synchronous response; placing the gate evaluation across a DCMS-internal network hop adds latency to the already-critical Breathing Space gate path. |

### Additional platform-grounded risks (not previously raised)

1. **Service-to-service authentication.** The DCMS monolith and any satellites must be registered in Keycloak as OAuth2 confidential clients. All calls to Solon Tax endpoints must carry a valid Client Credentials grant token (operations guide §4.2). This is a deployment pre-condition, not a design choice.
2. **OPA policy distribution across satellites.** OPA is deployed as a sidecar or standalone service per-component (operations guide §4.11). Each separately deployed satellite needs either its own OPA sidecar carrying DCMS Rego policies, or must call a central OPA instance. Affects satellites today and any future module extraction.
3. **`revenue-management-be-custom` boundary.** Solon Tax provides a `revenue-management-be-custom` repository for client-specific backend extensions that run *inside* Solon Tax services. The DCMS architecture must explicitly distinguish between (a) code that extends Solon internals via this custom layer and (b) code that runs as a separate DCMS application calling Solon from outside. This document assumes (b). If any DCMS logic ends up in (a), the monolith vs microservices question is partially moot for that logic — its decomposition is governed by Solon's own structure.
4. **Drools KIE synchronous dependency.** If DCMS calls Solon's KIE rules endpoint (`POST /drools-runtime/execute-stateless/{containerId}`), the call is synchronous to the Solon Drools runtime — its latency and availability are part of any DCMS request path that hits it. Relevant to Finding 4 (workspace render latency).
5. **DCMS process isolation in shared Amplio cluster.** The Solon Tax docs do not address whether DCMS can deploy its own BPMN process definitions into the shared Amplio engine, or whether multi-tenant process isolation is supported. Unresolved — escalate to Solon Tax vendor.
6. **Eventplans registration scope.** Event-type-to-process mappings are managed via the Eventplans system parameter in Amplio Admin (integration guide §4.1). It is not clear whether DCMS-defined custom event types can be registered. If not, DCMS process triggering must use direct `StartProcessCommand` calls instead of Eventplans-based dispatch.

### Pre-conditions before build (Option 2)

The expert flagged that the project's `solon-tax-platform-reference.md` document contained factual errors that would propagate into build-time integration code. Those errors have been corrected in the same review pass:

- **Kafka topic naming** corrected from `solon.{domain}.commands` to the authoritative `irm.bpmn-engine.{event-type}` pattern (per `external_sys_docs/solon/KAFKA_EVENT_CATALOG.md` and integration guide §5).
- **Kubernetes namespace model** corrected from `solon-core` / `solon-process` / `solon-infra` / etc. to the actual `[env]-platform-ms` / `[env]-platform-infra` / `[env]-aux-camunda-platform` / `[env]-platform-adapters` / `[env]-platform-batch` pattern (operations guide §3.1).
- **BPMN reference process list** corrected to match the actual `.bpmn` files in `external_sys_docs/solon/bpmn/irm/` — the previous list contained invented filenames (e.g. `AUDIT_PROCESS.bpmn`, `COMPLIANCE_CHECK_PROCESS.bpmn`, `RISK_ASSESSMENT_PROCESS.bpmn`, `BATCH_MONITORING_PROCESS.bpmn`) that do not exist.

Builders must use the corrected `solon-tax-platform-reference.md` as the only authoritative reference. Any earlier copies, slides, or derivative diagrams that quote the old topic names or namespace names must be retired or updated.

### Open blockers requiring escalation

1. **Breathing Space gate mechanism.** Non-interrupting message boundary events are not supported in Amplio 2.3.0. The suppression model uses interrupting boundary events. RULING-016 requires a non-interrupting gate at every debtor-facing effect. No Solon-supported pattern for this exists in the documentation. Escalate to Solon Tax vendor or Solution Architect before any DCMS debt lifecycle process design is locked.
2. **DCMS process isolation within shared Amplio cluster.** As above — escalate to vendor.

These two blockers affect all three options equally and are not resolved by choosing Option 2. They must be resolved on the integration side regardless of decomposition.

### Updated implementation guardrails

Builders working on the DCMS custom layer must not design processes that rely on:

- Compensation BPMN events (not implemented in Amplio 2.3.0)
- Concurrent execution via parallel gateways (Amplio executes them sequentially)
- Non-interrupting message boundary events (not supported)
- Native DMN (Drools KIE only)

These constraints apply regardless of which decomposition option is chosen.
