# DCMS Architecture Blueprint

**Project:** DWP Debt Collection Management System (DCMS)  
**Date:** 2026-04-23  
**Status:** Draft for implementation guidance (supplements `MASTER-DESIGN-DOCUMENT.md`, does not override ADRs or standards)

---

## 1. Purpose

This blueprint fleshes out the target architecture into an implementation-ready design view:

- How components interact at runtime
- Where event-driven behavior lives vs where object-oriented domain logic lives
- How data ownership, transaction boundaries, and reconciliation work
- How infrastructure, security, observability, and release governance fit together
- What must be completed to move from scaffold to production-capable architecture

Authoritative precedence remains:
1. Law and approved domain overlays
2. Requirements baseline
3. Standards pack
4. ADRs and canonical contracts
5. This blueprint

---

## 2. Architecture Style

DCMS is a **modular monolith** with a **hybrid control model**:

- **Event-driven workflow orchestration** in Flowable BPMN/DMN
- **Object-oriented domain modules** for business rules and persistence
- **Single deployable backend** and **single PostgreSQL logical system of record**

In practical terms:
- The process engine decides *what happens next* in debt lifecycle flows.
- Domain modules perform *how work is executed* (comms, payment posting, segmentation decisions, work allocation, audit writes).

---

## 3. System Context and Boundaries

### External actors and systems

- DWP staff use frontend screens (agent, supervisor, admin workflows).
- DWP Place and DM6 send referrals and lifecycle events.
- DWP strategic self-service portal/app submits customer actions such as I&E, contact-detail updates, repayment offers, payment events, and engagement signals through the integration boundary.
- Payment gateway, bureau feeds, and DCA partners exchange data with integration boundaries.
- Keycloak provides OIDC/OAuth2 authentication and role claims.

### Deployable boundaries

- `frontend`: React, GOV.UK, Nginx container.
- `backend`: Spring Boot monolith.
- `db`: PostgreSQL 16.
- `keycloak`: Keycloak 24.

No microservice network split currently exists inside backend domains.

---

## 4. Interaction Model

### 4.1 Synchronous interactions

| Interaction | Pattern | Owner |
|---|---|---|
| Frontend -> backend | REST request/response | frontend + backend |
| Source systems -> integration | REST ingest endpoint | `domain/integration` |
| Self-service portal/app -> integration | REST service-to-service API with idempotency keys and correlation IDs | `domain/integration` |
| Backend -> Keycloak token validation | OIDC/JWT | `domain/user` + security config |
| Backend -> external partners | REST or batch adapters | `domain/integration`, `domain/thirdpartymanagement` |

### 4.2 Asynchronous or event-driven interactions

| Interaction | Mechanism | Notes |
|---|---|---|
| Lifecycle transitions | BPMN flow + event subprocesses | authoritative runtime state |
| Customer/debt regulatory events | Flowable messages/signals | correlated by `customerId` or `debtAccountId` |
| Accepted self-service actions | Integration intake -> domain handoff -> audit/process event | contact updates, I&E submissions, arrangement/payment actions, engagement events |
| Service task execution | delegate -> command bus -> handler | preserves domain isolation from Flowable APIs |
| Time-based behavior | BPMN timers | e.g., missed payment timers |

### 4.3 Scheduled/reconciliation interactions

- Reconciliation jobs compare internal authoritative state vs external reported state.
- Divergence creates append-only reconciliation evidence and operator-visible work items.
- Retry logic is bounded and idempotent per `STD-INT-002`.

---

## 5. Internal Backend Architecture

### 5.1 Domain modules

Primary module ownership follows `MASTER-DESIGN-DOCUMENT.md` section 4.

Rules:
- Domain modules own invariants and data consistency for their bounded scope.
- Domain modules do not import Flowable classes.
- Cross-cutting integration with process engine uses shared ports only.

### 5.2 Process infrastructure

`infrastructure/process` owns:
- Flowable engine configuration
- BPMN/DMN resource loading
- `ProcessEventPort` and `ProcessStartPort` implementations
- Java delegates used by BPMN service tasks

`shared/process/port` owns:
- Port interfaces
- `DelegateCommandBus` abstraction
- Command contracts and handler interfaces

### 5.3 Dependency lanes

Allowed:
- `domain/*` -> `shared/process/port`
- `infrastructure/process` -> `shared/process/port`

Not allowed:
- `domain/*` -> `infrastructure/process`
- Any module except `infrastructure/process` importing Flowable types

---

## 6. Workflow and Event Architecture

### 6.1 Process granularity

- One process instance per `DebtAccount`.
- Customer case views are projections across debt accounts, not separate lifecycle aggregates.

### 6.2 Canonical event subprocess set

All treatment processes include the standard event subprocess fragment:

- `BREATHING_SPACE_START`
- `BREATHING_SPACE_END`
- `BENEFIT_CEASED`
- `CIRCUMSTANCE_CHANGE`
- `INSOLVENCY_REGISTERED`
- `DEATH_REGISTERED`
- `SUPERVISOR_OVERRIDE`
- Timer-driven `PAYMENT_MISSED`

### 6.3 Message/signal contract governance

- Names are versioned contracts.
- Additive changes are non-breaking.
- Rename/removal/correlation key changes are breaking and require migration strategy.

### 6.4 Runtime sequence examples

#### Case initiation
1. Candidate is promoted by agent.
2. Customer and debt account records are created or linked.
3. Segment is evaluated.
4. Process starts via `ProcessStartPort`.
5. Any failure rolls back the transaction.

#### Benefit ceased event
1. Integration receives benefit cessation update.
2. `ProcessEventPort.fireMessage(..., BENEFIT_CEASED, ...)` is called.
3. Interrupting event subprocess terminates current treatment lane.
4. `RESEGMENT` pathway determines new segment and starts new treatment definition.
5. Delegates trigger domain command handlers for required downstream actions.

---

## 7. Data Architecture

### 7.1 Data ownership

| Data concern | Owner |
|---|---|
| Customer identity and attributes | `domain/customer` |
| Debt financial ledger | `domain/account` + `domain/payment` |
| Repayment arrangements | `domain/repaymentplan` |
| Process execution state | Flowable runtime/history tables |
| Audit evidence | `domain/audit` (append-only design intent) |
| Candidate referral staging | `domain/integration` |
| Self-service action intake and idempotency records | `domain/integration` |
| Portal/app engagement facts | `domain/communications` or `domain/customer` by final ownership decision |

### 7.2 Transaction boundaries

- Case initiation is atomic across customer, account, and process start.
- Domain handlers are transaction-scoped for each command execution.
- Cross-system operations use idempotency keys and reconciliation rather than distributed transactions.

### 7.3 Read model strategy

To prevent Flowable query bottlenecks (ADR-007 risk):

- Introduce projection read models (for example, `case_summary`) for UI and reporting.
- Keep Flowable as write-side lifecycle authority.
- Do not query Flowable history tables directly from UI endpoints at scale.

### 7.4 Idempotency and replay

- Ingest operations use stable external references for duplicate prevention.
- Outbound integration commands include idempotency identity and retry-safe payloads.
- Replays produce deterministic results or explicit no-op outcomes.

---

## 8. Integration Reliability Architecture

Per `STD-INT-002`:

- Every financially or legally significant create operation supports idempotency.
- Retry taxonomy is explicit:
  - Retryable: dependency unavailable, transient network, rate limiting.
  - Non-retryable: validation, authorization, deterministic conflict.
- Reconciliation ownership is explicit (domain owner + ops support lane).
- Reconciliation outcomes are append-only evidence artifacts.

Recommended baseline components:
- Outbound attempt log with status and retry metadata
- Reconciliation queue for unresolved divergence
- Supervisor-visible resolution work item

### 8.1 Self-service integration reliability

Self-service is treated as an integration boundary, not as a DCMS-owned public portal. The DWP strategic portal/app is the customer-facing system; DCMS is responsible for accepting trusted service-to-service actions and applying domain rules.

Baseline controls:

- All inbound actions include an idempotency key, external action reference, source channel, and correlation ID.
- The portal/app service identity is authenticated; customer-facing authentication is handled by the DWP strategic component.
- Accepted actions create append-only intake/audit evidence before or within the same transaction as the domain handoff.
- Contact-detail updates, I&E submissions, arrangement offers, payment events, and engagement events use stable validation errors and deterministic duplicate handling.
- Domain modules remain authoritative for their facts: integration records the intake, while customer, repaymentplan, payment, communications, and audit apply the business effect.

---

## 9. Security and Access Architecture

### 9.1 Authentication

- Keycloak OIDC for interactive users.
- JWT bearer token validation in backend.
- Service-to-service access via client credentials where required.

### 9.2 Authorization

- Role-based access in backend and frontend.
- Full role set and permission matrix defined in `docs/project-foundation/RBAC-IMPLEMENTATION-DECISIONS.md`. Keycloak realm roles: AGENT, SPECIALIST_AGENT, ESCALATIONS_AGENT, TEAM_LEADER, OPS_MANAGER, SRO, COMPLIANCE, BACKOFFICE, PROCESS_DESIGNER. Client-scoped roles on `flowable-admin` client: FLOWABLE_ADMIN, SYSTEM. Roles ADMIN, LEGAL_AGENT, and DOMAIN_EXPERT are not provisioned — see RBAC-IMPLEMENTATION-DECISIONS.md for rationale.

### 9.3 Security boundaries

- Health endpoints remain unauthenticated by design.
- Sensitive data must be redacted in logs and error surfaces.
- Secrets are not committed in repo and must be injected by environment.

---

## 10. Observability Architecture

Per `STD-OPS-004` and related operations standards:

- JSON logs to stdout/stderr only.
- Required log fields: `timestamp`, `level`, `message`, `correlationId`.
- Correlation ID propagation across inbound and outbound boundaries.
- No sensitive data in logs.

Recommended telemetry additions for production readiness:
- Process event counters by definition key and version
- Migration/failure counters for BPMN migrations
- Event propagation failure alert (customer-level signal propagation incomplete)
- Reconciliation backlog age and count metrics

---

## 11. Environment and Deployment Architecture

### 11.1 Local environment

- Docker Compose runs full stack, including Keycloak.
- Local authentication and log format must match remote behavior.

### 11.2 Dev environment

- AKS deployment via Helm from GitHub Actions.
- Image tags pinned to commit short SHA.
- Atomic Helm upgrades with rollout verification.

### 11.3 Parity model

- Config key shape must be identical local vs remote.
- Value differences are allowed and documented in `remote-environment-spec.md`.
- Any documented divergence needs rationale and owner.

---

## 12. Change Governance Architecture

### 12.1 Change classes

- **Class A:** behavior/legal-impacting changes (BPMN logic, event handling semantics, payment allocation changes).
- **Class B:** standard implementation changes without behavior/legal impact.

### 12.2 BPMN versioning

- Non-breaking BPMN changes: deploy for new instances, keep in-flight on existing version.
- Breaking and bug-fix BPMN changes: mandatory migration script plus Class A sign-off.
- Migration failures route to controlled manual resolution; no silent force-termination.

---

## 13. Scalability and Resilience Posture

### 13.1 Scale strategy (current)

- Horizontal scale backend replicas in Kubernetes.
- Keep DB as single authoritative store with index and query tuning.
- Use projection tables to offload expensive lifecycle history queries.

### 13.2 Resilience strategy

- Bounded retries with backoff.
- Idempotent operations for retry safety.
- Compensation workflow for partial customer-level signal propagation failures.
- Readiness gates and health checks at container and platform level.

---

## 14. Architecture Delivery Roadmap

### Phase 1 - Core process infrastructure baseline

- Scaffold `shared/process/port` and `infrastructure/process`.
- Implement port interfaces, command bus abstraction, and first delegate-handler pairs.
- Add contract tests for message/signal catalogue and correlation rules.

### Phase 2 - Workflow execution readiness

- Author initial treatment BPMN definitions and shared event fragment.
- Implement segmentation DMN baseline and mapping config.
- Add projection read model for case summaries.

### Phase 3 - Integration reliability and reconciliation

- Implement outbound attempt tracking and retry policy framework.
- Implement reconciliation workflows and supervisor visibility.
- Add operational alerts and runbooks for propagation and migration failures.

### Phase 4 - Access and reporting hardening

- Implement RBAC per `docs/project-foundation/RBAC-IMPLEMENTATION-DECISIONS.md` — role provisioning in Keycloak, Spring Security `hasAuthority()` guards, team scope enforcement (Option B + C), write-off tier service-layer checks, and self-approval three-layer defence.
- Scaffold analytics/thirdpartymanagement/reporting modules.
- Implement reporting and export surfaces according to `docs/project-foundation/mi-dashboard-and-reporting-ux-design.md`, including freshness, reconciliation, audited exports, and retention/classification controls.

---

## 15. Open Design Decisions (Remaining)

1. RBAC permissions matrix complete — see `RBAC-IMPLEMENTATION-DECISIONS.md`. One open item remains: DDE-OQ-02 (confirmed write-off tier threshold values, pending DWP client confirmation).
2. DMN authoring governance for champion/challenger operation and approval workflow.
3. Third-party management API contract and reconciliation ownership model.
4. Self-service portal/app contract: trust model, canonical identifiers, synchronous/asynchronous validation, in-app message ownership, and payment source-of-truth responsibilities.
5. Reporting export format/versioning, export retention values, and operational runbook ownership.
6. Final migration tooling architecture for DM6 historical import and replay evidence.

---

## 16. Companion Artifacts

- `MASTER-DESIGN-DOCUMENT.md`
- `docs/project-foundation/decisions/ADR-001...ADR-011`
- `docs/project-foundation/diagrams/07-hybrid-architecture-overview.drawio`
- `docs/project-foundation/remote-environment-spec.md`

