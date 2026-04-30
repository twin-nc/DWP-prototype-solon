> ## ⚠ STATUS: UNDER REVIEW — PLATFORM PIVOT IN EFFECT
>
> A confirmed programme-level decision (ADR-018, 2026-04-30) has pivoted the project to build on top of **Solon Tax** as the base platform. All prior ADRs are under review.
>
> **This document contains accumulated knowledge and analysis that remains relevant to the new design process. It does NOT represent the current or preferred direction.** All decisions and conclusions herein are open for renewed debate. Do not treat this ADR as a directive, default position, or settled constraint.
>
> See [ADR-018](ADR-018-platform-pivot-solon-tax-confirmed.md) for the full pivot record.

---

# ADR-007: Single Deployable Monolith Architecture

**Status:** Accepted

**Date:** 2026-04-23

**Decision Maker:** Architecture Council (four-voice review: Architect, Skeptic, Pragmatist, Critic)

---

## Context

With a team scaling to 6-7 full-stack developers, a deliberate architecture review was commissioned to decide between a single deployable monolith and a microservices architecture. The review was conducted as a four-voice council with independent deliberations to eliminate anchoring bias. No prior decision was taken as given — this was a fresh analysis from first principles.

The system must satisfy 438+ functional requirements across 10 domain packages, comply with UK regulatory obligations (CCA 1974, breathing space, insolvency handling), and maintain an immutable audit trail of all debt lifecycle events. The team has no prior operational experience with this codebase.

---

## Options Considered

### Option A: Single Spring Boot Monolith

One deployable JAR containing all domain packages. Single PostgreSQL database. All domain logic shares one transaction boundary. Flowable process engine runs inside the application context.

### Option B: Microservices (one service per domain or per domain cluster)

Each domain package (or cluster) deployed as an independent service with its own database, CI/CD pipeline, and network boundary. Services communicate via REST or an event bus.

### Option C: Modular Monolith with Staged Extraction

Start as a monolith with Maven-module-enforced domain boundaries. Extract one or two high-churn services (e.g., `communications`) after the team has operational maturity.

---

## Decision

**Option A — Single Spring Boot Monolith.**

Option C (staged extraction) remains available as a future path if team size or throughput demands it, but no extraction is planned in the current scope.

---

## Rationale

### 1. Regulatory compliance requires atomic transactions

Breathing space, insolvency events, and CCA-governed write-offs must propagate atomically across customer, account, payment, and audit records. In a monolith, this is a single database transaction. In a microservices architecture, this requires a saga orchestration layer with compensating transactions — adding failure modes that are difficult to audit and harder to prove deterministic to a regulator. The risk of an event reaching some debt process instances but not others is unacceptable under UK regulatory frameworks. A monolith eliminates this class of failure entirely.

### 2. Flowable is structurally monolithic

The BPMN/DMN process engine (Flowable) is stateful and tied to a single database schema for process instances, variables, and history. Distributing Flowable across service boundaries would require distributed locking and state synchronisation that Flowable is not designed to provide. ADR-001 establishes one process instance per debt account; ADR-003 confines all Flowable imports to `infrastructure/process`. Both decisions are natural and low-cost in a monolith. In microservices, they would require an additional orchestration layer (e.g., Temporal, Conductor) running alongside Flowable — doubling the process engine surface area.

### 3. The team cannot operationalise 10 services

A 10-domain microservices architecture requires: 10 independent CI/CD pipelines, per-service database schema governance, inter-service contract testing, distributed tracing, cross-service Keycloak authentication, and per-service observability. A 6-7 person team that owns backend, frontend, infrastructure, and DevOps simultaneously cannot sustain this operational load while delivering 438+ functional requirements. The complexity budget must go to domain logic, not infrastructure plumbing.

### 4. Kubernetes provides cloud-native operations without microservices

The system runs on Kubernetes (AKS for `dev`, k3d for `local`). Horizontal Pod Autoscaling, rolling deployments, health probes, and readiness gates work identically for a monolith as for microservices. Cloud-native operations are not contingent on service decomposition.

### 5. Audit trail is simpler and more reliable

A single PostgreSQL instance with a single audit event table provides one source of truth for all regulatory events. Distributed audit across multiple services requires correlation ID propagation across every service boundary and log aggregation infrastructure to reconstruct a complete event trace — both of which introduce failure modes that can produce incomplete audit records. The monolith avoids this class of problem.

---

## Risks and Mitigations

The council identified four advisory risks that must be addressed before go-live. These are pre-conditions, not future work.

### Risk 1: Audit trail is not immutable by default

A standard PostgreSQL table can be modified or deleted. UK regulatory obligations require tamper-proof, append-only audit records.

**Mitigation (required before go-live):** The `audit_events` table must be created in a separate PostgreSQL schema with a dedicated database role that has INSERT-only access. No application code may UPDATE or DELETE from it. The Flyway migration that creates this schema must include a comment locking it against modification. This must be in place from v0.1.

### Risk 2: Domain boundaries have no enforcement mechanism

The command bus (ADR-003) and port interfaces (`shared/process/port`) define logical boundaries. Without automated enforcement, a shortcut import under deadline pressure collapses the boundary model silently.

**Mitigation (required before first feature delivery):** ArchUnit tests must be added to the CI pipeline enforcing that no class in `domain.X` may import from `domain.Y` internal packages. All cross-domain calls must go through `domain.*/service/` public interfaces. Build failures on violation.

### Risk 3: Flowable query performance at scale

ADR-001 creates one process instance per debt account. Customers in chronic debt may have 50+ active process instances. Flowable's `HistoricProcessInstance` queries are not optimised for high-volume per-customer filtering and will degrade significantly at Year 2 scale if used for UI rendering.

**Mitigation (required in data model design):** A CQRS read layer must be implemented from the start. Flowable is the write side (process execution only). A separate `case_summary` projection table, updated by process event listeners, is the read side for all UI and reporting queries. No UI component may query Flowable history tables directly.

### Risk 4: Breathing space signal failure under partial propagation

A customer-level regulatory event (breathing space, insolvency, death) must propagate to all active process instances for that customer. If Flowable's signal delivery fails mid-propagation, some process instances receive the event and others do not. This is an undefined regulatory state.

**Mitigation (required before go-live):** An operational runbook must document the compensation procedure for partial signal propagation failures. Flowable's built-in retry handles transient failures; the runbook covers retry exhaustion. Observability alerts must fire when signal delivery for a customer-level event does not complete within a defined window.

---

## Consequences

### Positive

- Single artifact build and deployment reduces operational overhead
- ACID transactions across all domain operations eliminate distributed consistency problems
- Full audit trail in one database with one transaction context
- `docker compose up` starts the complete stack locally; no inter-service networking to configure
- Team focuses on domain logic rather than distributed systems operations
- Flowable process engine operates within its designed single-database model

### Negative

- Individual domains cannot be scaled independently; the full monolith must be scaled
- All domains share a deployment cycle; an unstable change in one module affects all others
- Flyway migrations are shared; concurrent schema changes from multiple teams require coordination
- Extracting a service later (if team grows to 12+) requires decomposing shared state — more expensive than starting with clear service boundaries

### Neutral / Deferred

- `communications` and `integration` are candidates for future extraction at 12+ developers or if merge conflict rate exceeds 1 per sprint, given their inbound-only dependency profile. This is a future decision gate, not a current commitment.
- The RBAC permissions matrix must be designed before the `user` module is built, regardless of this architecture choice.

---

## Review Trigger

This decision should be re-evaluated when:

- The team grows to 12 or more developers, **or**
- A single domain sustains >500 requests/second and cannot be addressed by vertical scaling or read replicas, **or**
- Regulatory requirements introduce a domain with a materially different security boundary (e.g., a payment card processing domain requiring PCI-DSS isolation)

---

## Related Decisions

- [ADR-001](ADR-001-process-instance-per-debt.md) — Process instance per debt: natural in a monolith, expensive to distribute
- [ADR-002](ADR-002-monitoring-event-subprocess-pattern.md) — Monitoring event subprocess pattern: relies on single-transaction process state
- [ADR-003](ADR-003-process-engine-placement-and-delegate-pattern.md) — Process engine placement and delegate pattern: enforces Flowable confinement to `infrastructure/process`
