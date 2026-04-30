> ## ⚠ STATUS: UNSETTLED — PLATFORM PIVOT IN EFFECT
>
> A confirmed programme-level decision (ADR-018, 2026-04-30) has pivoted the project to build on top of **Solon Tax** as the base platform. A new design process is underway.
>
> **This document contains accumulated knowledge and analysis that remains relevant to the new design process. It does NOT represent the current or preferred direction.** All decisions and conclusions herein are open for renewed debate. Do not treat this document as a directive or default position.
>
> See [`CLAUDE.md`](../../CLAUDE.md) and [ADR-018](decisions/ADR-018-platform-pivot-solon-tax-confirmed.md) for the current direction.

---

# DCMS Master Design Document

**Project:** DWP Debt Collection Management System (DCMS)
**Client:** UK Department for Work and Pensions
**Development team:** 6-7 people
**Date:** 2026-04-28
**Status:** Living document - reconciled with accepted architecture ADRs, locked implementation ADRs, and domain rulings; authoritative for system-wide architecture

---

## Design Summary

The DCMS is a purpose-built, open-source debt collection platform for DWP — not a configured off-the-shelf product. It replaces manual and fragmented tooling with an end-to-end system that takes a debt referral from DWP source systems all the way through assessment, treatment, repayment, and eventual resolution or write-off.

**Architecture in one sentence:** A single Spring Boot backend (Java 21, PostgreSQL) drives all debt lifecycle logic through an embedded BPMN process engine (Flowable), fronted by a GOV.UK-compliant React UI, with Keycloak handling staff authentication.

**Key design choices and why they matter:**

- **Monolith, not microservices.** The delivery team is expected to be 6-7 people, which is large enough for parallel feature delivery but still benefits from one deployable backend, one database boundary, and simple operations. The monolith is divided into well-bounded domain packages (`customer`, `account`, `repaymentplan`, `payment`, `communications`, etc.) that can be extracted later if operating a distributed system becomes justified.

- **BPMN for debt lifecycle, not bespoke state machines.** Every debt account has one running Flowable process instance. Regulatory events (breathing space, insolvency, death) are modelled as BPMN event subprocesses — they interrupt or join the main treatment flow rather than being scattered across application code. This makes the lifecycle auditable, human-readable, and changeable without code deployments.

- **Three-tier configurability plus policy bundles.** Foundational definitions, behavioural rules, and BPMN process architecture are governed separately. Cross-tier business changes are grouped through the policy layer so effective dating, approval, audit, and rollback stay coherent.

- **Frontend organised as three role-scoped workspaces.** The React frontend is a single SPA with a shared shell and three top-level workspaces: Case Worker (`/cases`), Operations (`/operations`), and Configuration (`/admin`). The shell owns Keycloak session state, role-based navigation, notifications, and primary workspace routing. Role hiding in React is only a UX layer; Spring Security remains the authoritative enforcement point. See ADR-017.

- **Configuration surface unified in one admin UI.** All three tiers of configurability — Foundations (Tier 1), Rules (Tier 2 DMN), and Processes (Tier 3 BPMN) — are surfaced through the Configuration Workspace at `/admin`. A fourth Templates section handles communication template management. Policy bundles are authored at `/admin/policies`. There are no separate configurator applications. STRATEGY_MANAGER has read-only access to Rules only, for context while monitoring champion/challenger outcomes. See ADR-015.

- **Process action library for reusable workflow steps.** The old Solon-style step-library/strategy-assembler model is rejected, but DCMS still needs a governed catalogue of reusable Flowable service-task actions. The Process Action Library defines stable delegate names, command types, input/output schemas, allowed BPMN contexts, ownership, versioning, and coverage validation for process designers. See `process-action-library-design.md`.

- **Strategy simulation before live change.** Proposed rule, policy, and later process changes can be simulated offline against selected account cohorts before approval or activation. The roadmap starts with deterministic DMN rule simulation, then policy-bundle impact simulation, then strategy path simulation, and finally advanced BPMN replay. Simulation is read-only evidence for governance; it does not mutate live state. See `strategy-simulation-engine-design.md`.

- **MI dashboards and controlled reporting.** Reporting is not just projection tables. The staff-facing MI surface includes executive, operations, champion/challenger, DCA, export-centre, and data-health views. Dashboards must show freshness, reconciliation state, policy/target versions, and RBAC-controlled drilldowns. Exports are audited, classified, and reproducible. See `mi-dashboard-and-reporting-ux-design.md`.

- **Process engine as infrastructure, not domain.** Domain modules never import Flowable. They interact with the process engine only through two thin port interfaces (`ProcessEventPort`, `ProcessStartPort`). This keeps domain logic testable, portable, and insulated from engine upgrades.

- **Keycloak for auth, RBAC for access control.** All DWP staff authenticate via OAuth 2.0 + OIDC. The RBAC role set and full permissions matrix have been fully designed — see `docs/project-foundation/RBAC-IMPLEMENTATION-DECISIONS.md`. Realm roles include `STRATEGY_MANAGER`; `FLOWABLE_ADMIN` and `SYSTEM` remain client-scoped technical roles on the `flowable-admin` client. The `user` module implementation must follow the decisions in that document.

- **Domain rulings govern legally consequential behaviour.** Breathing space, vulnerability, write-off, deceased handling, DCA placement, statute-barred debt, limitation-clock resets, and champion/challenger treatment are governed by named rulings in `docs/project-foundation/domain-rulings/`.

- **Local/remote parity by rule.** Developers run the full stack locally via `docker compose up` — same auth flow, same config keys, same log format as the shared dev environment on AKS. There is no "stub" mode that can mask integration bugs.

**Current status:** Core backend domain packages are scaffolded. Seventeen architecture ADRs in `docs/project-foundation/decisions/` are accepted, and fourteen implementation ADRs in `docs/project-foundation/architecture-decisions.md` are locked. Phase 3 design is now locked in `domain-packs/communications-domain-pack.md` and `domain-packs/work-allocation-domain-pack.md`. Phase 4 design is now locked in `domain-packs/reporting-analytics-read-model-domain-pack.md`, with staff-facing MI/dashboard UX defined in `mi-dashboard-and-reporting-ux-design.md`. The remaining scaffolding gaps are `thirdpartymanagement`, `infrastructure/process`, and `shared/process/port`; implementation migrations remain needed for the Phase 4 read-model work. The RBAC permissions matrix is defined in `docs/project-foundation/RBAC-IMPLEMENTATION-DECISIONS.md`; the frontend workspace architecture is defined in ADR-017; detailed architecture elaboration is in `ARCHITECTURE-BLUEPRINT.md`; environment parity details are in `remote-environment-spec.md`.

---

## Table of Contents

1. [System Purpose](#1-system-purpose)
2. [Tech Stack](#2-tech-stack)
3. [Repository Structure](#3-repository-structure)
4. [Domain Architecture](#4-domain-architecture)
5. [Process Engine Architecture](#5-process-engine-architecture)
6. [Debt Lifecycle and Case Model](#6-debt-lifecycle-and-case-model)
7. [Segmentation and Treatment Routing](#7-segmentation-and-treatment-routing)
8. [Candidate List and Case Initiation](#8-candidate-list-and-case-initiation)
9. [BPMN Versioning Policy](#9-bpmn-versioning-policy)
10. [Authentication and Authorisation](#10-authentication-and-authorisation)
11. [Infrastructure and Environments](#11-infrastructure-and-environments)
12. [Functional Requirements Coverage](#12-functional-requirements-coverage)
13. [Standards and Governance](#13-standards-and-governance)
14. [Architecture Decision Record Index](#14-architecture-decision-record-index)
15. [Domain Rulings Index](#15-domain-rulings-index)
16. [Open Design Questions](#16-open-design-questions)

---

## Architecture Diagrams

All diagrams are in [`docs/project-foundation/diagrams/`](diagrams/) as Mermaid (`.mmd`) files — renderable in VS Code, GitHub, or mermaid.live.

| Diagram | File |
|---|---|
| System context (actors and external systems) | [01-system-context.mmd](diagrams/01-system-context.mmd) |
| Domain module map (all packages, scaffold status) | [02-domain-module-map.mmd](diagrams/02-domain-module-map.mmd) |
| Process engine layering — delegate command pattern | [03-process-engine-layers.mmd](diagrams/03-process-engine-layers.mmd) |
| Debt lifecycle — ingest through treatment and outcomes | [04-debt-lifecycle.mmd](diagrams/04-debt-lifecycle.mmd) |
| Segmentation and configurability | [05-segmentation-configurability.mmd](diagrams/05-segmentation-configurability.mmd) |
| Infrastructure, deployment environments, and auth flow | [06-infrastructure-and-auth.mmd](diagrams/06-infrastructure-and-auth.mmd) |
| Hybrid architecture overview (event-driven workflow + OO domain modules) | [07-hybrid-architecture-overview.drawio](diagrams/07-hybrid-architecture-overview.drawio) |

---

## 1. System Purpose

The DCMS is a greenfield debt collection management platform for the UK Department for Work and Pensions. It manages the end-to-end lifecycle of DWP debt — from identification and assessment through recovery, enforcement, and write-off.

This is **not** a configuration of an existing COTS platform. It is a purpose-built system designed and built from scratch using fully open-source components.

**Scope:**
- Ingest debt referrals from DWP source systems (DWP Place, DM6)
- Agent-led review and case initiation
- Automated treatment routing via segmentation and BPMN workflows
- Repayment arrangement creation and lifecycle management
- Payment collection, allocation, and reconciliation
- Communications (letters, SMS, email) and contact history
- Integration with the DWP strategic self-service portal/app for customer-submitted I&E, contact-detail updates, arrangement/payment actions, and engagement signals. DCMS owns the backend intake and orchestration boundary; it does not own a standalone public portal in the current baseline.
- Work allocation, queue management, and agent/supervisor tooling
- Third-party debt collection agent (DCA) placement and management
- Compliance-grade audit trail and reporting
- DWP staff-facing frontend (React) and REST API backend (Spring Boot)

---

## 2. Tech Stack

### Backend

| Concern | Choice |
|---|---|
| Language | Java 21 (OpenJDK via `eclipse-temurin:21`) |
| Framework | Spring Boot 3.4.x |
| Build tool | Maven 3.9.6+ |
| Database | PostgreSQL 16 |
| Migrations | Flyway Community Edition |
| ORM | Spring Data JPA + Hibernate |
| Process engine | Flowable BPMN/DMN |
| Logging | Logstash Logback Encoder (JSON to stdout) |

### Frontend

| Concern | Choice |
|---|---|
| Framework | React + TypeScript |
| Design system | GOV.UK Design System |
| Container | Nginx 1.27 (Alpine) |

### Authentication and Authorisation

| Concern | Choice |
|---|---|
| Identity Provider | Keycloak 24 |
| Protocol | OAuth 2.0 + OpenID Connect |
| Spring integration | Spring Security OAuth2 Resource Server |
| Authorisation model | RBAC via JWT claims |
| Service accounts | OAuth2 Client Credentials grant |

### Infrastructure

| Concern | Choice |
|---|---|
| Containers | Docker (multi-stage builds) |
| Local orchestration | Docker Compose |
| Local Kubernetes | k3d |
| Remote Kubernetes | Any cluster (no vendor lock-in) |
| Package manager | Helm 3 |

### Pinned base images

| Service | Image |
|---|---|
| App runtime | `eclipse-temurin:21-jre-jammy` |
| App build stage | `eclipse-temurin:21-jdk-jammy` |
| Frontend build | `node:20-alpine` |
| Frontend runtime | `nginx:1.27-alpine` |
| Database | `postgres:16` |
| Identity provider | `quay.io/keycloak/keycloak:24` |

---

## 3. Repository Structure

Monorepo layout:

```
/
├── backend/                  ← Spring Boot monolith (Maven)
├── frontend/                 ← React + TypeScript application
├── infrastructure/
│   ├── docker-compose.yml    ← Full local stack
│   ├── keycloak/             ← Realm config and import files
│   └── helm/                 ← Helm chart for Kubernetes
├── docs/
│   └── project-foundation/   ← Architecture, ADRs, standards, templates
└── CLAUDE.md                 ← Authoritative system description
```

CI/CD: GitHub Actions at `.github/workflows/`
Container registry: `ufstpit-dev-docker.repo.netcompany.com`
Image tag strategy: `{git-sha}` on all builds; `:dev` on main-branch merges.

---

## 4. Domain Architecture

The backend is a **single Spring Boot monolith** with well-defined internal domain packages. No microservices split.

### Domain packages

| Package | Responsibility |
|---|---|
| `domain/customer` | Customer identity, profile, circumstances, vulnerability markers, and customer-level attributes shared across debts |
| `domain/account` | Financial ledger: balances, payment history, write-off amounts. Records regulatory facts (breathing space date, insolvency date, death, fraud) for audit. Does **not** own lifecycle position |
| `domain/strategy` | Decisioning rule configuration, treatment routing, champion/challenger versioning, segmentation execution |
| `domain/workallocation` | Queues, worklists, agent/team assignment, exception queues, supervisor override |
| `domain/repaymentplan` | Arrangement creation, I&E capture and affordability calculation, schedule management, arrangement lifecycle, I&E-linked review scheduling, self-service I&E intake handoff |
| `domain/payment` | Payment posting, allocation logic, financial event tracking, reconciliation hooks |
| `domain/communications` | Contact history, channel events, correspondence state, suppression rules |
| `domain/integration` | Inbound/outbound API and batch interfaces, anti-corruption layer, candidate list, DWP self-service portal/app integration boundary |
| `domain/thirdpartymanagement` | DCA placement, recall, reconciliation, commission/billing, third-party partner interfaces |
| `domain/user` | RBAC, user management, Keycloak integration |
| `domain/audit` | Immutable activity/event history, trace records, compliance evidence |
| `domain/analytics` | Segmentation logic, scoring models, bureau scorecard feeds, DMN decision tables |
| `domain/reporting` | Read models, MI exports, KPI views |

### Customer, party, and account model constraints

Domain rulings in `docs/project-foundation/domain-rulings/` refine the base model:

- A recoverable unit is an account/person or account/party relationship, not a household-level case object.
- Universal Credit joint claimant debts may carry joint and several liability and must not be split without the joint-debt legal constraints in RULING-004.
- Benefit deduction recovery, off-benefit billing, account split mechanics, and deceased/joint-party handling must follow the account-person recovery rulings before acceptance criteria or schema changes are written.
- Compliance-sensitive facts such as breathing space, vulnerability, deceased status, insolvency, statute-barred status, and limitation-clock reset events must be represented as durable account/party facts plus audit history, not only as transient process variables.

### Infrastructure packages

| Package | Responsibility |
|---|---|
| `infrastructure/process` | Flowable engine config, BPMN/DMN model resources, JavaDelegate implementations, ProcessEventPort/ProcessStartPort implementations. **All Flowable imports confined here.** |
| `shared/process/port` | ProcessEventPort, ProcessStartPort, DelegateCommandBus, DelegateCommand, DelegateCommandHandler interfaces, and all command types. **No Flowable imports.** |
| `shared/health` | Health endpoint controllers |
| `shared/error` | Error handling and envelope |

### Dependency rule

Domain modules may depend on `shared/process/port`. No domain module may import from `infrastructure/process`. No module except `infrastructure/process` may import Flowable types.

### Module scaffold status

| Module | Status |
|---|---|
| `customer`, `account`, `strategy`, `workallocation`, `repaymentplan`, `payment`, `communications`, `integration`, `user` | Scaffolded |
| `audit` | Partially implemented |
| `analytics`, `reporting` | Scaffolded |
| `thirdpartymanagement` | Not yet scaffolded |
| infrastructure/process, shared/process/port | Not yet scaffolded |

### Phase 3 and 4 design baseline

Phase 3 and Phase 4 design are locked at domain-pack level and are now authoritative for downstream implementation:

- docs/project-foundation/domain-packs/communications-domain-pack.md - template lifecycle, delivery status tracking, channel fallback, suppression-aware orchestration.
- docs/project-foundation/domain-packs/work-allocation-domain-pack.md - SLA tracking model, skill routing model, queue assignment and supervisor read model.
- docs/project-foundation/domain-packs/reporting-analytics-read-model-domain-pack.md - KPI taxonomy, projection-table architecture, and dashboard query contracts.
- docs/project-foundation/mi-dashboard-and-reporting-ux-design.md - staff-facing dashboard, export, freshness, reconciliation, drilldown, and phased MI UX design.

---

## 5. Process Engine Architecture

Defined by ADR-001, ADR-002, and ADR-003.

### Placement

The process engine (Flowable) is **infrastructure, not a domain module**. It lives at `infrastructure/process`. Domain modules interact with it only through interfaces in `shared/process/port`.

### Port interfaces

```java
// Entry point for firing events into running instances
ProcessEventPort.fireMessage(debtAccountId, messageName, variables)
ProcessEventPort.fireSignal(customerId, signalName, variables)

// Entry point for starting or restarting a treatment process
ProcessStartPort.startTreatment(debtAccountId, segmentCode)
```

All domain module interaction with the process engine goes through these two interfaces. Direct `RuntimeService` calls outside `infrastructure/process` are prohibited.

### Delegate command pattern (ADR-003)

BPMN service tasks call domain services via a three-layer command pattern:

1. **Delegate** (`infrastructure/process/delegate/`) — reads process variables, constructs a command, dispatches via `DelegateCommandBus`. No domain imports.
2. **`DelegateCommandBus`** (`shared/process/port/`) — routes commands to registered handlers.
3. **Command handlers** (domain modules) — plain `@Component` beans, no Flowable imports.

BPMN service tasks bind to delegates by Spring bean name:
```xml
<serviceTask id="sendLetter" flowable:delegateExpression="${sendLetterDelegate}" />
```

**Delegate bean names are a versioned contract** — renaming is a breaking change.

### Transaction boundary rule

Locked by `architecture-decisions.md` implementation ADR-003.

Application database writes are committed inside application `@Transactional` boundaries before Flowable service calls are made. Flowable calls (`RuntimeService`, `TaskService`, execution operations) must run outside application transactions. Flowable failures after committed application state are logged as `MANUAL_REVIEW_REQUIRED` evidence and must not roll back already-committed protective state such as breathing-space flags, deceased flags, suppression logs, or audit events.

### Initial command type catalogue

| Command | Owner module |
|---|---|
| `SendLetterCommand` | `communications` |
| `SendSmsCommand` | `communications` |
| `AssessAffordabilityCommand` | `repaymentplan` |
| `PostPaymentDeductionCommand` | `payment` |
| `TriggerSegmentationCommand` | `strategy` |
| `CreateWorkItemCommand` | `workallocation` |
| `RecordAccountNoteCommand` | `account` |
| `RecordRegulatoryFactCommand` | `account` |

### Process Action Library

The initial command catalogue is the implementation seed for the **Process Action Library**, a governed catalogue of reusable BPMN service-task actions available to PROCESS_DESIGNER users in the Processes section of the admin UI.

Each catalogue entry defines the stable action key, delegate bean name, command type, input/output schema, owning domain module, allowed BPMN contexts, guardrails, failure semantics, version, and lifecycle status. BPMN deployment candidates must pass service-task coverage validation against this catalogue before they can move to approval.

See `docs/project-foundation/process-action-library-design.md`.

### BPMN resource locations

```
backend/src/main/resources/processes/
  treatments/     ← one file per treatment process definition
  fragments/      ← shared event subprocesses (call activity pattern)
  decisions/      ← DMN decision tables
```

---

## 6. Debt Lifecycle and Case Model

Defined by ADR-001 and ADR-002.

### Process instance granularity

**One Flowable process instance per `DebtAccount`** (one debtor + debt pairing). A single customer may have multiple debts simultaneously, each with its own independent process instance at a different lifecycle stage.

### Case view

The "case view" (all debts for a customer) is a **read-model aggregation** — not a domain object. It queries `ProcessInstance` and `HistoricProcessInstance` filtered by `customerId`, alongside `Customer` and `DebtAccount` records.

### Case initiation

Case initiation is an **atomic transaction**:
1. Create or verify `Customer` record
2. Create `DebtAccount` record
3. Call `ProcessStartPort.startTreatment()` to start Flowable process instance

All three operations execute in a single `@Transactional` boundary. If process start fails, the entire transaction rolls back.

### Event subprocesses (monitoring layer)

Every treatment process definition includes a standard set of event subprocesses at root scope, deployed as a shared call activity fragment (`standard-event-subprocesses.bpmn`):

| Event subprocess | Trigger | Interrupting | Correlation key | Action |
|---|---|---|---|---|
| `breathing-space-received` | `BREATHING_SPACE_START` message | Yes | `customerId` | Suspends comms, moves to suspended state, waits for `BREATHING_SPACE_END` |
| `benefit-ceased` | `BENEFIT_CEASED` message | Yes | `debtAccountId` | Terminates current treatment, fires `RESEGMENT` signal |
| `change-of-circumstances` | `CIRCUMSTANCE_CHANGE` message | No | `debtAccountId` | Triggers re-evaluation task; may fire `RESEGMENT` |
| `insolvency-registered` | `INSOLVENCY_REGISTERED` message | Yes | `customerId` | Halts treatment, moves to insolvency-handling state |
| `death-registered` | `DEATH_REGISTERED` message | Yes | `customerId` | Halts all activity, creates estate-handling task |
| `payment-missed` | Timer: N days after expected payment | No | `debtAccountId` | Creates breach task, evaluates breach tolerance rules |
| `supervisor-override` | `SUPERVISOR_OVERRIDE` message | No | `debtAccountId` | Moves process to specified step |

**Message/signal name catalogue** is a versioned contract. Additions are non-breaking; renames and removals are breaking changes (STD-GOV-004).

### Regulatory facts vs. process states

- **`account` module** records the *fact* (breathing space date, insolvency date, death notification) for audit.
- **Process engine** owns the *behavioural effect* — the business response is entirely modelled in BPMN.
- These are two separate concerns and must not be coupled.

---

## 7. Segmentation and Treatment Routing

Defined by ADR-004.

### Primary segment taxonomy

| Segment code | Vulnerability | Benefit status | Process definition key |
|---|---|---|---|
| `VULNERABLE_ON_BENEFIT` | true | ON_BENEFIT | `treatment-vulnerable-on-benefit` |
| `STANDARD_ON_BENEFIT` | false | ON_BENEFIT | `treatment-standard-on-benefit` |
| `VULNERABLE_OFF_BENEFIT` | true | OFF_BENEFIT | `treatment-vulnerable-off-benefit` |
| `STANDARD_OFF_BENEFIT` | false | OFF_BENEFIT | `treatment-standard-off-benefit` |

Segmentation operates at **account level**, informed by **customer-level attributes**. One customer may have accounts in different segments simultaneously (CAS.11).

### Exception states are not segments

Deceased, insolvency, breathing space, fraud, and hardship are **process states** within the treatment BPMN — not segments. They coexist with any primary segment.

- Segments → determine which process definition starts → `startProcessInstanceByKey`
- Exception states → interrupting/non-interrupting BPMN event subprocesses

### Three-tier configurability model

Defined by ADR-008 and constrained by RULING-003.

**Tier 1 - Foundational definitions:**
- Segment types, vulnerability types, treatment definitions, suppression reasons, and similar reference definitions are stored as effective-dated configuration records, not Java enums where operational change is expected.
- Changes require policy-owner approval, audit evidence, and future-effective activation where the change can alter collection behaviour.

**Tier 2 - Behavioural rules:**
- Segmentation, routing, vulnerability action matrices, write-off limit routing, and champion/challenger assignment rules are represented as versioned DMN/rule definitions.
- Every decision records the active rule version and configuration snapshot used at the time of evaluation.
- Mandatory statutory or regulatory hard stops are not made configurable where doing so would allow an operator to disable legal protection.

**Tier 3 - Process architecture:**
- New treatment paths require a new BPMN process definition deployed through Flowable's `RepositoryService` without a Java deployment.
- Structural BPMN changes that alter live token positions, correlation keys, or regulatory event handling are **Class A** changes and require Domain Expert + Solution Architect sign-off.

### Policy layer

Defined by ADR-009.

Policies group cross-tier changes into a single effective-dated bundle. A policy may coordinate Tier 1 definitions, Tier 2 rules, and Tier 3 process mappings, with conflict checks, approval workflow, audit trail, and rollback metadata. Not every configuration edit needs a policy wrapper, but any legally consequential business change that spans tiers must be governed as a policy.

### Champion/challenger experiments

Defined by ADR-010 and constrained by RULING-010.

Champion/challenger treatment experiments are managed in the `strategy` domain. Assignment is deterministic and auditable; activation is a Class A change. Protected-population eligibility is governed by the active champion/challenger policy version, not hardcoded in the assignment engine. The current approved policy requires vulnerable customers to receive champion treatment and to be excluded from A/B comparison analytics while still contributing to separate vulnerable-outcome metrics. Any change to that eligibility rule is a Class A policy change requiring the configured policy approval path and DWP sign-off where applicable.

Live strategy monitoring is owned by `STRATEGY_MANAGER`, not `PROCESS_DESIGNER`. `STRATEGY_MANAGER` can view champion/challenger results and strategy KPIs in the Operations Workspace and can trigger a champion/challenger promotion. Promotion swaps the relevant `dmn_deployment.is_champion` and `dmn_deployment.is_challenger` flags for an already-approved challenger version; it does not author or approve a new DMN rule and does not enter the Tier 2 approval workflow. Every promotion emits an `AUDIT_EVENT` with type `CC_PROMOTION` and the actor ID of the `STRATEGY_MANAGER` who triggered it. `OPS_MANAGER` can read champion/challenger results but cannot trigger promotion.

### Strategy simulation

Defined by `strategy-simulation-engine-design.md`.

Strategy simulation is a pre-live evidence capability for rule, policy, and process changes. Phase 1 supports deterministic current-vs-proposed DMN simulation against selected account cohorts. Later phases add policy-bundle impact simulation, side-effect-free strategy path simulation using the Process Action Library, and advanced BPMN replay of historic event timelines. Simulation results can be attached to approvals and champion/challenger setup, but simulation does not replace Class A approval gates.

### Domain-specific Tier 2 DMN tables

Beyond the core vulnerability action matrix, the following domain areas own DMN decision tables at Tier 2:

| Domain | DMN table | Owner module |
|---|---|---|
| Work allocation | Queue routing rules (segment × priority → queue assignment) | `workallocation` |
| Work allocation | SLA thresholds (queue type × account priority → SLA timer, escalation rule) | `workallocation` |
| Communications | Channel preference routing (customer circumstances × channel → available/unavailable) | `communications` |
| Communications | Breach thresholds (account type × arrangement status → tolerance period, escalation action) | `repaymentplan` |
| DCA management | Placement eligibility (treatment stages, balance, account age, exclusion flags → eligible/ineligible) | `thirdpartymanagement` |
| I&E | Standard living allowance bands (household type → SLA lower/upper bound) | `repaymentplan` |
| Vulnerability | Protocol action matrix (vulnerability category × severity × action → allowed/blocked/escalated) | `customer` (vulnerability sub-domain) |

All of these tables are authored by PROCESS_DESIGNER, approved by COMPLIANCE, and deployed through the Tier 2 workflow defined in ADR-008. They are accessible via the Rules section of the admin UI (ADR-015).

---

## 7a. Income & Expenditure Engine

Functional requirements: `IEC.1`–`IEC.11`, RULING-007.

### Ownership

The I&E domain belongs to `domain/repaymentplan`. It is not a standalone service. I&E capture, affordability calculation, and review scheduling are services within the repayment plan module.

### Core entities

- **`IERecord`** — a point-in-time income and expenditure assessment for a customer. Captures income sources, expenditure categories, captured by, channel (agent/system), and capture date. Immutable after completion.
- **`AffordabilityAssessment`** — derived from an `IERecord`. Stores net disposable income (NDI), sustainable repayment range, arrangement options (weekly/fortnightly/monthly within NDI band), and the SLA version used.
- **`IEReviewSchedule`** — records the next required review date for a customer. Created or updated on I&E capture and on arrangement breach. When the review date is reached, a Flowable timer fires a `CreateWorkItemCommand` to create an agent task.

### Standard living allowances (SLA)

SLA bands (expenditure ceiling by household type) are configurable as a Tier 2 DMN table. This means DWP can adjust allowance values without a code deployment. The DMN table is the authoritative SLA source; hardcoded values are not used.

### CRA/bureau integration

CRA validation is optional at capture time. When triggered, the `integration` module's anti-corruption layer calls the bureau API. Discrepancies are flagged for agent review — CRA output does not override agent-captured I&E without explicit agent confirmation.

### Review scheduling mechanism

I&E reviews are scheduled as Flowable timer events, not as background cron jobs. When an `IEReviewSchedule` is created or updated, the relevant process instance receives a message (`IE_REVIEW_DUE_DATE_SET`) that configures a timer intermediate catch event. When the timer fires, a `CreateWorkItemCommand` creates an agent task of type `IE_REVIEW`.

### Open design questions

RULING-007 gates on DDE-OQ-08 (I&E staleness period) and DDE-OQ-09 (benefit-deduction I&E requirement). Affordability rule implementation must not proceed until these are confirmed.

---

## 8. Candidate List and Case Initiation

Defined by ADR-005.

### Ingest mechanism

A single REST endpoint in the `integration` module accepts debt referrals from DWP source systems:

```
POST /api/v1/integration/candidates
Body: { "referrals": [ { ...CandidateReferralDto }, ... ] }
```

- Idempotent on `external_reference`: duplicate referrals are marked `DUPLICATE` and not re-inserted
- Candidate records are immutable from moment of ingest — agents cannot edit them before initiation

### Candidate record states

```
PENDING → INITIATED (agent promotes to case)
PENDING → REJECTED (agent rejects referral)
PENDING → DUPLICATE (duplicate external_reference detected on ingest)
```

### Module ownership

The `integration` module owns:
- The REST endpoint and `CandidateReferralDto` inbound contract
- The `candidate_record` table and Flyway migration
- Duplicate detection logic
- `CandidateRecord` JPA entity, repository, and `CandidateService`

Case initiation (Customer + DebtAccount + process start) is **delegated** to `customer`, `account`, and `shared/process/port`. The `integration` module calls these — it does not implement them.

---

## 8a. Self-Service Integration Baseline

Defined by `self-service-integration-design.md`.

The tender baseline includes explicit self-service requirements (`CC.22`, `IEC.2`, `IEC.8`, `RPF.3`, `I3PS.2`, `DIC.21`) and related communication/engagement requirements (`DW.65`, `DW.81`). The DCMS design satisfies these through a backend integration capability, not by building a standalone public customer portal.

The baseline responsibilities are:

- `domain/integration` exposes secured service-to-service APIs for DWP strategic portal/app actions.
- `domain/customer` applies validated contact-detail updates received from self-service.
- `domain/repaymentplan` accepts self-service I&E submissions as `channel = SELF_SERVICE` and runs the same affordability and arrangement checks as agent-led capture.
- `domain/payment` accepts portal/app payment events only after payment-domain validation and reconciliation rules apply.
- `domain/communications` supports in-app message instructions and portal/app engagement signals while still applying suppression and contact strategy rules.
- `domain/audit` records self-service actions automatically and makes them visible to staff near real time.

Customer-facing identity proofing, app/device enrolment, customer portal UX, public app-store packaging, and customer-facing error journeys remain outside the DCMS baseline unless a future scope change assigns those responsibilities to DCMS.

---

## 9. BPMN Versioning Policy

Defined by ADR-006.

### Change classification

| Class | Definition | Migration requirement |
|---|---|---|
| **Non-breaking** | Adds a path, task, or event without altering existing token positions or correlation keys | Deploy and monitor — no migration |
| **Breaking** | Removes or renames a task, alters flow structure that would strand tokens, or changes a correlation key | Named migration script required; force-migrate all affected instances |
| **Bug fix** | Corrects incorrect behaviour on live instances | Treated as breaking — force-migrate is the mechanism |

Breaking changes and bug fixes are **Class A changes** requiring Domain Expert + Solution Architect sign-off before deployment.

### `standard-event-subprocesses` fragment versioning

The shared event subprocess fragment is deployed independently. Updating it propagates to all treatment processes on next instance start. In-flight instances remain on the version active when they started.

### Flowable Admin UI access and audit

Defined by ADR-011 and locked again in `architecture-decisions.md` implementation ADR-006.

Flowable Admin UI is restricted to operational/technical roles. Direct process instance manipulation is reserved for `FLOWABLE_ADMIN`; operational task reassignment goes through application APIs unless explicitly authorised. Admin UI operations that affect process instances, variables, suspension, activation, deletion, or migration must be surfaced through the `HistoryEventProcessor` into `AUDIT_EVENT` with the documented SYSTEM actor cross-reference limitation.

---

## 10. Authentication and Authorisation

Keycloak 24 is the identity provider. It runs locally (never skipped), satisfying local/remote parity (STD-PLAT-008).

Keycloak satisfies:
- INT01, INT02, INT03 — OAuth 2.0 + OIDC SSO
- INT28 — multi-domain federation

The RBAC role model has been fully designed. See `docs/project-foundation/RBAC-IMPLEMENTATION-DECISIONS.md` for the complete role set, permission matrix, write-off tier model, team scope enforcement, and case transfer process. The `user` feature implementation must follow the decisions in that document. The open design question on RBAC is closed except for RBAC DDE-OQ-02 (confirmed write-off thresholds, pending DWP sign-off).

Keycloak realm roles are: `AGENT`, `SPECIALIST_AGENT`, `ESCALATIONS_AGENT`, `TEAM_LEADER`, `OPS_MANAGER`, `SRO`, `COMPLIANCE`, `BACKOFFICE`, `PROCESS_DESIGNER`, and `STRATEGY_MANAGER`. Client-scoped roles on the `flowable-admin` client are `FLOWABLE_ADMIN` and `SYSTEM`. `ADMIN`, `LEGAL_AGENT`, and `DOMAIN_EXPERT` are not provisioned roles; their rejected or absorbed responsibilities are explained in `RBAC-IMPLEMENTATION-DECISIONS.md`.

Frontend access is organised by ADR-017:

| Workspace | Route prefix | Primary access |
|---|---|---|
| Case Worker Workspace | `/cases` | `AGENT`, `SPECIALIST_AGENT`, `ESCALATIONS_AGENT`; `TEAM_LEADER` read-only within team scope |
| Operations Workspace | `/operations` | `TEAM_LEADER`, `OPS_MANAGER`, `STRATEGY_MANAGER`; `COMPLIANCE` read-only audit evidence view |
| Configuration Workspace | `/admin` | `PROCESS_DESIGNER`, `COMPLIANCE`, `OPS_MANAGER` for Tier 1; `FLOWABLE_ADMIN` for `/admin/processes` only |

ADR-015 defines the detailed `/admin` visibility table. In summary, `STRATEGY_MANAGER` has read-only access to `/admin/rules` only, for context while reviewing strategy performance. The champion/challenger promotion control lives in `/operations`, not `/admin`, and is governed by the RBAC matrix rather than by the Tier 2 authoring workflow.

---

## 11. Infrastructure and Environments

### Environments

| Environment | Platform | Purpose |
|---|---|---|
| `local` | Docker Compose + k3d | Developer laptop — full stack, no cloud credentials |
| `dev` | AKS on Azure | Shared remote cluster |

Higher environments (test, UAT, production) are out of scope until separately specified.

**Parity rule (STD-PLAT-008):** local and dev must remain functionally equivalent — same config keys, same auth flow, same JSON log format. All divergences documented in `docs/project-foundation/remote-environment-spec.md`.

### Standards compliance

| Standard | Requirement |
|---|---|
| STD-PLAT-009 | Multi-stage Docker builds, pinned images, non-root users, SIGTERM handling |
| STD-PLAT-010 | `docker compose up` one-command startup, `depends_on: service_healthy`, health checks |
| STD-OPS-003 | `/health/live` and `/health/ready` without authentication |
| STD-OPS-004 | JSON structured logs to stdout with `correlationId` via MDC |
| STD-PLAT-008 | Local/remote parity — identical config keys, same auth flow, JSON logs |
| STD-PLAT-006 | Flyway immutability, idempotency, zero-downtime migration patterns |

---

## 12. Functional Requirements Coverage

**Total requirement IDs:** 442 across 19 capability groups.

| Capability Group | IDs | Count | Primary Module(s) |
|---|---|---:|---|
| Customer & Account Structure | `CAS.1`–`CAS.17` | 17 | `customer`, `account` |
| Data & Information Capture | `DIC.1`-`DIC.37` | 37 | `customer`, `account`, `communications`, `audit`, `integration` |
| Decisions & Workflows | `DW.1`-`DW.88` | 88 | `infrastructure/process`, `strategy`, `workallocation`, `integration` |
| Repayment Plan Functionality | `RPF.1`-`RPF.37` | 37 | `repaymentplan`, `payment`, `integration` |
| User Access & Admin | `UAAF.1`–`UAAF.26` | 26 | `user` |
| Work Allocation & Monitoring | `WAM.1`–`WAM.28` | 28 | `workallocation` |
| Agent Actions & Dispositions | `AAD.1`–`AAD.27` | 27 | `communications`, `audit`, `workallocation` |
| 3rd Party Management | `3PM.1`–`3PM.18` | 18 | `thirdpartymanagement` |
| Contact Channels | `CC.1`–`CC.35` | 35 | `communications`, `integration`, `audit` |
| Income & Expenditure Capture | `IEC.1`–`IEC.11` | 11 | `repaymentplan`, `customer`, `integration` |
| Bureau & Scorecard Feeds | `BSF.1`–`BSF.15` | 15 | `analytics`, `strategy` |
| Interfaces to 3rd Party Systems | `I3PS.1`–`I3PS.15` | 15 | `integration`, including self-service portal/app APIs |
| System of Record | `SoR.1`–`SoR.21` | 21 | `account`, `payment`, `audit`, `integration` |
| User Interface Screens | `UI.1`–`UI.30` | 30 | `frontend/src` |
| Analytics & Segmentation | `A.1`–`A.2` | 2 | `analytics` |
| MI & Reporting | `MIR.1`–`MIR.2` | 2 | `reporting` |
| Change Processes | `CP.1`–`CP.11` | 11 | `docs/project-foundation`, `.github/workflows` |
| System Development & Roadmap | `SD.1`–`SD.7` | 7 | `docs/project-foundation` |
| Migration Requirements | `MR1`–`MR15` | 15 | `integration` |

Full per-requirement trace: `docs/project-foundation/trace-map.yaml`
Detailed module-to-requirement map: `docs/project-foundation/functional-requirements-module-map.md`

---

## 13. Standards and Governance

Standards are located at `docs/project-foundation/standards/`. Key documents:

| Category | Standards |
|---|---|
| Platform | Containerization, local dev environment, local/remote parity, database migrations, date-effective rules, determinism, feature flags, immutable records, state resolution precedence |
| Operations | Health endpoints, structured logging, observability, CI/CD secret management |
| Governance | Change classification, contract ownership, contract versioning, documentation authority, release evidence, requirements traceability, test authority |
| Integration | Error semantics, file transfer, reliability and reconciliation |
| Security | Data sensitivity and redaction, security boundaries, domain data classification |
| Development | Accessibility, testing |
| AI | Agent responsibility boundaries |

Change classification (WAYS-OF-WORKING.md §5a):
- **Class A** — alters live debt collection behaviour (BPMN changes, regulatory event handling, payment allocation logic). Requires Domain Expert + Solution Architect sign-off before deployment.
- **Class B** — all other changes. Standard PR review applies.

Domain ruling gate:
- No BA may write acceptance criteria for a Class A requirement, and no Builder may merge a Class A change, without a linked ruling in `docs/project-foundation/domain-rulings/`.
- Rulings with `awaiting-client-sign-off` impose provisional constraints. Builders must treat the rule statements as binding design constraints, but must not present unconfirmed open questions as implemented or client-approved facts.
- Hard-block rulings stop implementation beyond design until the named DWP client question is answered.

---

## 14. Architecture Decision Record Index

### System architecture ADRs

Accepted ADRs in `docs/project-foundation/decisions/`:

| ADR | Title | Status |
|---|---|---|
| ADR-001 | Process instance per person/account pair | Accepted |
| ADR-002 | Monitoring and event handling via BPMN event subprocesses | Accepted |
| ADR-003 | Process engine as infrastructure - delegate command pattern | Accepted |
| ADR-004 | Segment taxonomy, exception state distinction, and two-tier baseline configurability | Accepted |
| ADR-005 | Candidate list data model and case initiation entry point | Accepted |
| ADR-006 | BPMN versioning and in-flight instance migration policy | Accepted |
| ADR-007 | Single deployable monolith architecture | Accepted |
| ADR-008 | Three-tier configurability architecture | Accepted |
| ADR-009 | Policy layer - cross-tier configuration bundles | Accepted |
| ADR-010 | Champion/challenger experimentation framework | Accepted |
| ADR-011 | Flowable Admin UI - access control and audit | Accepted |
| ADR-012 | Live-state DB reads in delegates | Accepted |
| ADR-013 | Flowable transaction boundary and breathing space resume delegate | Accepted |
| ADR-014 | Communication suppression authority and call-site enforcement | Accepted |
| ADR-015 | Configuration surface and role-scoped admin UI | Accepted |
| ADR-016 | Platform pivot rejected - greenfield stack confirmed | Accepted |
| ADR-017 | Three-workspace frontend architecture | Accepted |

ADR-008 supersedes the earlier two-tier configurability language in ADR-004 for future design work. ADR-004 remains authoritative for the initial segment taxonomy and the distinction between primary segments and exception states. ADR-015 governs the Configuration Workspace (`/admin`) and its role-scoped visibility rules. ADR-017 governs the overall frontend workspace architecture and shared shell.

### Locked implementation ADRs

Locked ADRs in `docs/project-foundation/architecture-decisions.md`:

| ADR | Title | Status |
|---|---|---|
| ADR-001 | CommunicationSuppressionService boundary and interface | LOCKED |
| ADR-002 | StatuteBarredEvaluator boundary and guardrails | LOCKED |
| ADR-003 | Platform-wide transaction boundary rule | LOCKED |
| ADR-004 | Write-off self-approval three-layer defence | LOCKED |
| ADR-005 | DeceasedPartyHandler two-phase atomicity | LOCKED |
| ADR-006 | Flowable Admin UI access control and audit | LOCKED |
| ADR-007 | Breathing space BPMN single definition, dual path | LOCKED |
| ADR-008 | Champion/challenger vulnerable exclusion and analytics | LOCKED |
| ADR-009 | Process variable live state rule | LOCKED |
| ADR-010 | Sprint migration sequence and numbering convention | LOCKED |
| ADR-011 | DISPUTE entity suppression column removal | LOCKED |
| ADR-012 | Statute-barred schema fields | LOCKED |
| ADR-013 | ResumeArrangementMonitoringDelegate design | LOCKED |
| ADR-014 | Champion/challenger analytics architecture | LOCKED |

Where an accepted architecture ADR and a locked implementation ADR overlap, the accepted architecture ADR governs system intent and the locked implementation ADR governs the concrete implementation pattern unless a later architecture decision explicitly supersedes it.

---

## 15. Domain Rulings Index

### Indexed Class A domain rulings

Current ruling index from `docs/project-foundation/domain-rulings/README.md`:

| Ruling | Topic | Status | Design impact |
|---|---|---|---|
| RULING-001 | Breathing Space - communication suppression scope | awaiting-client-sign-off | Collection communications suppressed at account level; non-collection communications remain permitted unless another suppression reason applies. |
| RULING-002 | Vulnerability - data classification and access control | awaiting-client-sign-off | Vulnerability markers and health-adjacent notes require restricted handling and access control. |
| RULING-003 | Write-off - regulatory and policy constraints | awaiting-client-sign-off | Delegated authority, self-approval prohibition, provisional thresholds, and compliance notification constraints govern write-off design. |
| RULING-004 | Joint debt - regulatory constraints on account split | awaiting-client-sign-off | Legal sign-off, active-arrangement checks, legal-hold checks, limitation-clock preservation, and residual write-off approval are required before split. |
| RULING-005 | Breathing Space - repayment arrangement interaction | final | Arrangement monitoring is suspended, not cancelled; missed payments during moratorium must not trigger breach. |
| RULING-006 | Deceased party mandatory handling | awaiting-client-sign-off | Deceased communications are suppressed immediately; estate/joint-account handling requires specialist review and may block implementation. |
| RULING-007 | I&E assessment affordability obligations | awaiting-client-sign-off | I&E checks apply to outstanding balance and affordability-sensitive arrangements; benefit deductions are treated separately. |
| RULING-008 | DCA placement pre-placement disclosure obligations | awaiting-client-sign-off | DCA placement requires prior disclosure, lawful data sharing basis, placement audit, and immediate recall paths. |
| RULING-009 | Statute-barred debt | awaiting-client-sign-off | Statute-barred route, fields, nightly recalculation, and limitation-period configuration are required; collection workflow is blocked pending limitation-period confirmation. |
| RULING-010 | Champion/challenger Consumer Duty implications | awaiting-client-sign-off | Challenger treatment must not disadvantage customers; the current approved policy assigns vulnerable customers to champion treatment, with future vulnerable-customer eligibility changes requiring Class A policy governance and DWP sign-off. |
| RULING-011 | Queued communication disposition on suppression lift | final | Statutory-suppressed debt collection communications are discarded; internal-policy suppressed debt collection communications require agent review. |
| RULING-012 | Limitation clock reset events | awaiting-client-sign-off | Only named debtor-initiated payment, active arrangement, and formal written acknowledgement events reset the limitation clock. |
| RULING-013 | Statute-barred clock reset timing | awaiting-client-sign-off | Default implementation is async post-commit evaluator unless DWP client answers change the selected path by 2026-04-30. |

### Additional domain ruling notes

The directory also contains earlier or supporting domain notes with overlapping ruling numbers. They remain useful design inputs but should be reconciled into the indexed ruling scheme before being treated as canonical Class A rulings:

| File | Subject | Use |
|---|---|---|
| `RULING-001-benefit-deduction-mechanics.md` | Benefit deduction mechanics in DWP recovery | Recovery mechanics and benefit-deduction domain background. |
| `RULING-001-customer-data-model-unit-of-analysis.md` | Customer data model and unit of analysis | Customer/account/household modelling constraints. |
| `RULING-002-account-person-model-and-recovery-mechanics.md` | Account-person model and recovery mechanics | Account/person recovery relationships, on/off-benefit mechanics, and split scenarios. |
| `RULING-003-vulnerability-configurability-governance-and-constraints.md` | Vulnerability configurability governance | Mandatory hard rules and governance constraints behind ADR-008. |
| `RULING-003-QUICK-REFERENCE.md` | Vulnerability governance quick reference | Builder-facing summary of vulnerability configuration gates. |
| `DDE-OQ-STATUTE-BARRED-SUMMARY.md` | Statute-barred open questions | Delivery-lead summary for DDE-OQ-11/12/13. |
| `RULING-013-HANDOFF.md` | RULING-013 handoff | Role-by-role handoff and default path if client response is missing by 2026-04-30. |
| `RULING-013-IMPLEMENTATION-CHECKLIST.md` | RULING-013 implementation checklist | Sprint 3/4 implementation checklist; apply only after ruling gates are satisfied. |

### Hard blocks and conditional gates

| Ruling | Gate | Effect |
|---|---|---|
| RULING-006 | DDE-OQ-07 estate pursuit policy | Deceased-customer account management stories must not proceed past design until confirmed. |
| RULING-009 | DDE-OQ-11 limitation period per debt type | Statute-barred enforcement and collection workflows must not proceed until confirmed. |
| RULING-012 | DDE-OQ-13 promise-to-pay acknowledgement status | `PROMISE_TO_PAY` must not be implemented as a clock-reset trigger unless confirmed. |
| RULING-013 | DDE-OQ-11/12/13 statute-barred timing choices | Implementation path is conditional; default path is Option B + statute-barred suppression enabled + no grace period if no response by 2026-04-30. |

---

## 16. Open Design Questions

The following design areas require resolution before the relevant feature can be built or before provisional assumptions can become confirmed design facts:

| Area | Gap | Blocking feature |
|---|---|---|
| Write-off thresholds | Confirm delegated authority threshold values (RBAC DDE-OQ-02; RULING-003 notes that DDE-OQ-03 is closed and DDE-OQ-04 is resolved). | Write-off approval workflow, `write_off_limit_check.dmn`, RBAC permission finalisation |
| Vulnerability lawful basis | DPO confirmation of Article 9 lawful basis remains open (DDE-OQ-02 in RULING-002). | Vulnerability data handling and access-control implementation |
| Deceased estate pursuit | Estate pursuit policy remains open (DDE-OQ-07). | Deceased-party handling beyond design |
| Joint debt split | Legal sign-off rules and de minimis residual write-off threshold remain open (DDE-OQ-05/DDE-OQ-06). | Account split implementation |
| I&E policy values | DWP I&E staleness period and benefit-deduction I&E requirement remain open (DDE-OQ-08/DDE-OQ-09). | Repayment arrangement affordability rules |
| Self-service integration contract | DWP strategic portal/app ownership, identifiers, API trust model, synchronous vs asynchronous validation, in-app message ownership, and payment source-of-truth choices remain open (DDE-OQ-SS-01 to DDE-OQ-SS-06). | Self-service API implementation, portal/app I&E intake, portal/app arrangement/payment actions, in-app messaging |
| DCA placement notice | Required pre-placement notice period remains open (DDE-OQ-10). | DCA placement workflow |
| Statute-barred debt | Limitation periods, suppression treatment, promise-to-pay reset status, and post-reset grace period remain open (DDE-OQ-11/DDE-OQ-12/DDE-OQ-13). | Statute-barred workflow, limitation-clock reset implementation |
| Champion/challenger thresholds and protected-population policy | Minimum test duration and sample size thresholds remain open (RULING-010 DDE-OQ-12 / architecture-decisions open item DDE-OQ-09). Vulnerable-customer challenger eligibility policy also requires DWP sign-off before any policy version can permit vulnerable customers into challenger treatment. | Experiment promotion gate and eligibility policy |
| `infrastructure/process` scaffold | Package not yet created; Flowable Spring Boot starter not yet wired. | All `DW` process/workflow slices |
| `shared/process/port` scaffold | Package not yet created. | All modules using delegate commands |
| Process Action Library implementation choices | Palette restriction approach, catalogue storage format, and schema format remain to be confirmed during `infrastructure/process` scaffolding. | BPMN authoring and service-task coverage validation |
| Strategy Simulation Engine implementation choices | Cohort selection, async job model, maximum cohort size, mandatory-simulation rules, and point-in-time fact snapshot approach remain to be confirmed. Phase 1 DMN simulation can proceed before full BPMN replay. | Rule approval, policy approval, champion/challenger setup evidence |
| MI dashboard and reporting UX implementation choices | Standard export catalogue, export retention/classification, KPI target seed values, dashboard refresh SLAs, and aggregate-to-case drilldown permissions remain to be confirmed. | Reporting frontend, export centre, data-health dashboard |
| Operational recovery tooling | End-to-end failed process/job/delegate, failed handoff, failed communication, projection rebuild, and reconciliation exception operator journeys are not yet designed. | Operations support, recovery workbench, runbook-linked UI |
| Remote environment spec | Initial version authored; update required when staging/production environments are introduced. | Future environment expansion |
| Migration tooling design | DM6 to DCMS migration approach (batch jobs, tooling, data-quality gates) not yet specified. | `MR` requirements |
| I&E SLA DMN seed data | Initial standard living allowance bands by household type require DWP confirmation before the SLA DMN table can be seeded | I&E affordability calculation, `repaymentplan` I&E schema |

---

*Authoritative sources for specific areas: `CLAUDE.md` (system description), accepted ADRs in `docs/project-foundation/decisions/`, locked implementation ADRs in `docs/project-foundation/architecture-decisions.md`, domain rulings in `docs/project-foundation/domain-rulings/`, standards documents (non-negotiable constraints), and `trace-map.yaml` / requirement maps for requirement-to-implementation links.*



