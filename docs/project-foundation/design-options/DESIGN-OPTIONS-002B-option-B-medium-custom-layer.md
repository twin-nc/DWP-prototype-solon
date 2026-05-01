# DESIGN-OPTIONS-002B: Option B - Medium Custom Layer

**Document ID:** DESIGN-OPTIONS-002B
**Date:** 2026-05-01
**Status:** PROPOSED - RECOMMENDED, but integration design lock BLOCKED pending resolution of Blocker 1, Blocker 2, Blocker 3, and the `suspendActiveInstancesSW` compliance contradiction.
**Parent document:** [DESIGN-OPTIONS-002: Layer Thickness and Boundary Placement](./DESIGN-OPTIONS-002-layer-thickness-and-boundary.md)
**Architecture diagram:** [DESIGN-OPTIONS-002-option-B-architecture.drawio](./DESIGN-OPTIONS-002-option-B-architecture.drawio)
**Author:** Derived from Delivery Designer + Platform Expert + Design Critic reviews

---

## What This Document Is

Option B is one of three layer-thickness options evaluated in DESIGN-OPTIONS-002. This document extracts the full Option B design into a standalone reference. It covers architecture, boundary placement, BPMN approach, service ownership, data ownership, UI capabilities, open blockers, risks, and builder guardrails.

The parent document (DESIGN-OPTIONS-002) retains the three-way tradeoff analysis, decision levers, and option-flip conditions. This document is for teams building or reviewing Option B specifically.

**Current recommendation:** Option B. It is the preferred architecture because DWP-specific domain services - champion/challenger, income and expenditure, vulnerability governance, communication suppression, strategy management, and Breathing Space gating - are large enough to warrant their own schemas and internal service contracts while still reusing Solon's financial, process, task, batch, auth, and observability capabilities.

---

## Summary

DCMS is a medium custom layer running inside Solon's JVM and application context. Solon contributes the financial ledger, payment and write-off primitives, batch engine, Kafka command bus, Amplio process engine, Keycloak/OPA auth, and ELK observability stack. DCMS contributes DWP-owned BPMN process definitions, DWP-owned domain services, a dedicated `dcms` PostgreSQL schema, and a custom React application exposed through a richer BFF.

DWP-specific BPMN processes are authored from scratch and deployed into Amplio alongside Solon's unmodified reference processes. DCMS service tasks call DCMS-owned Spring beans for domain decisions, and those beans orchestrate Solon primitives through the supported Kafka and REST surfaces where available.

The main design value is ownership clarity: DCMS owns the DWP domain model and schema while Solon remains the substrate for reusable tax-platform capabilities. The main design risk is that Option B still runs in-process. Its upgrade blast radius is reduced by discipline and boundary rules, not by a hard runtime boundary.

---

## Architecture

### Layer structure

```text
+------------------------------------------------------------------+
| UI Layer (React SPA)                                             |
| Case Worker / Operations / Configuration workspaces              |
| Rich BFF: aggregates Solon REST responses + DCMS service data     |
+----------------------------------+-------------------------------+
                                   | REST
+----------------------------------v-------------------------------+
| Solon REST API Gateway / Keycloak Bearer JWT / OPA               |
+----------------------------------+-------------------------------+
                                   |
+----------------------------------v-------------------------------+
| Solon Tax v2.3.0 - Single JVM (documented Java 17 baseline)      |
|                                                                  |
|  Amplio Process Engine / Suppression / Drools KIE / Kafka Bus    |
|  Financial ledger / payment / write-off / batch / task tray      |
|                                                                  |
|  DCMS custom layer                                               |
|  - DWP BPMN process definitions deployed into Amplio             |
|  - revenue-management-be-custom Spring beans                     |
|  - revenue-management-batch-custom batch definitions             |
|  - DCMS custom KIE module for DWP rules                          |
|                                                                  |
|  DCMS domain services                                            |
|  - BreathingSpaceGatingService                                   |
|  - ChampionChallengerService                                     |
|  - IEAssessmentService                                           |
|  - VulnerabilityClassificationService                            |
|  - CommunicationSuppressionService                               |
|  - SegmentationService                                           |
|  - DCAPlacementService                                           |
|                                                                  |
|  PostgreSQL 16                                                   |
|  - Solon schema                                                  |
|  - dedicated dcms schema                                         |
+------------------------------------------------------------------+
```

### Key architectural properties

- DCMS is not a separate service tier. It is a custom layer inside Solon's JVM.
- DCMS domain services have dedicated internal APIs and tables in a dedicated `dcms` schema.
- DCMS BPMN processes are new process definitions deployed into Amplio. Solon reference BPMN is read for patterns and is not modified.
- Solon financial primitives are reused through Kafka commands and documented REST query APIs, subject to Blocker 1 and Blocker 2.
- DCMS custom beans are registered in Solon's application context, using the Solon customisation model.
- The BFF is richer than Option A. It aggregates Solon and DCMS responses and shields the React UI from raw Solon API shapes.
- The architecture remains in-process. Solon internal refactors can still break DCMS if boundary discipline is not enforced.

---

## Boundary Placement

| Responsibility | Lives in Solon | Lives in DCMS custom layer |
|---|---|---|
| Financial ledger, balances, financial transactions, write-off | Solon core | Calls via confirmed Kafka or REST contract |
| Payment processing and allocation | Solon core | DWP-specific CCA allocation order enforced before posting |
| Repayment plan lifecycle | Solon payment-plan primitives | DWP-specific arrangement rules, re-aging, breach tolerance, gating |
| Batch engine | Solon Foundation Batch | DWP-specific batch jobs in `revenue-management-batch-custom` |
| Kafka command bus | Solon infrastructure | DCMS consumes/publishes only confirmed topics; DCMS-owned events stay in DCMS namespace |
| Task tray and work allocation primitives | Solon task management | DWP queue rules, worklist views, supervisor override |
| Debt recovery BPMN | Solon reference processes, unmodified | New `dcms-debt-recovery.bpmn` process family deployed into Amplio |
| DCA handover | Solon handover primitives where usable | DWP pre-placement disclosure gate and DCA selection logic |
| Breathing Space hold state | Solon suppression record | DCMS gating decision, gate log, MHCM-specific evaluation |
| Champion/challenger | Not present in Solon | DCMS service owns assignment, promotion, harm indicators |
| Income and expenditure | Not present in Solon | DCMS service owns CFS model and affordability calculation |
| Vulnerability governance | Not present in Solon | DCMS service owns FCA FG21/1 tiers and reassignment logic |
| Communication transport | Solon correspondence process where usable | DCMS category evaluation, suppression decision, on-lift disposition design |
| Strategy and segmentation | Solon Drools runtime | DCMS custom KIE module and strategy lifecycle service |
| UI | - | New React SPA and DCMS BFF |
| Auth | Solon Keycloak + OPA | DWP realm configuration and DCMS-specific Rego policies |

### Breathing Space gating service

The `BreathingSpaceGatingService` is a DCMS-owned Spring bean with a dedicated schema table, `breathing_space_gate_log`. It reads Solon's active suppression state and adds DWP-specific decision logic: moratorium type, standard Breathing Space versus MHCM, vulnerability overlap, deferred-output flagging, and RULING-016 gate-at-effect compliance.

Every DCMS BPMN service task that could produce a prohibited debtor-facing effect calls this service before executing. The service returns a `GatingDecision`:

| Decision | Meaning |
|---|---|
| `PERMIT` | The effect may proceed. |
| `SUPPRESS` | The effect must not be emitted. |
| `DEFER_PENDING_MORATORIUM_END` | The effect is recorded as deferred and may require later review or release handling. |

The BPMN process continues internal progression. The gate stops prohibited effects at the effect boundary rather than cancelling the process token.

**Important unresolved compliance point:** the `suspendActiveInstancesSW` setting for each relevant Solon `SuppressionType` must be declared before Option B can be locked. If it is `true`, Solon may suspend active BPMN tokens and undermine the RULING-016 gate-at-effect model. If it is `false`, DCMS carries the effect-suppression responsibility and Solon suppression expiry only changes the suppression record that DCMS reads.

### MHCM suppression

The Mental Health Crisis Moratorium has no fixed end date. It lasts for the duration of mental health crisis treatment plus 30 days and ends only on professional sign-off.

DCMS owns the MHCM release path:

1. AMHP or clinician professional sign-off is captured in DCMS.
2. `MHCM_RELEASE_PROCESS.bpmn` calculates the 30-day post-treatment tail date.
3. DCMS issues the confirmed release command or API call to Solon.
4. Solon suppression state changes.
5. `BreathingSpaceGatingService` re-reads suppression state on the next invocation and permits only when the active hold is gone.

**Blocker 3 applies:** `SuppressionExpiryJob` behaviour for `maximumNumberDays: 0` is undocumented. Until the source is inspected, Option B must not assume that zero means "no maximum." The defensive mitigation is `maximumNumberDays: 36500` plus `overrideEndDateSW: true`, with DCMS-owned release and a documented operational risk.

---

## BPMN Approach

DWP-specific collection workflows are new BPMN process definitions authored by DCMS and deployed into Amplio. They do not fork or modify Solon's reference processes.

| Process definition | Demo flow | Amplio complexity |
|---|---|---|
| `dcms-intake-and-first-contact.bpmn` | Flow 1 - Intake to first contact | Low - linear workflow |
| `dcms-vulnerability-to-resolution.bpmn` | Flow 2 - Vulnerability to resolution | Medium - suppression atomicity and vulnerability state changes |
| `dcms-payment-monitoring.bpmn` | Flows 2 and 3 - arrangement monitoring | Low - timer and status-check pattern |
| `dcms-breach-to-placement.bpmn` | Flow 3 - Breach to third-party placement | High - PTP loop, supervisor branching, inter-process correlation |
| `dcms-strategy-review.bpmn` | Flow 5 - Strategy change approval | Low - review and approval sequence |
| `dcms-mhcm-release.bpmn` | MHCM release handling | Medium - professional sign-off, tail-date calculation, release command |

Flows 4 and 6 are primarily service/read-model driven. Flow 4 depends on household and restriction enforcement services; Flow 6 depends on BFF aggregation and operational read models.

### Amplio authoring constraints

These constraints apply to all DCMS BPMN under Option B:

1. Parallel Gateway is sequential-only. Do not design for true concurrent execution.
2. Script Tasks are FEEL-only. Imperative logic belongs in Java delegate classes.
3. Business Rule Tasks must be Service Tasks. DMN is not supported; rule evaluation uses Drools KIE.
4. Compensation events are not implemented. Use explicit error boundary events and compensation service tasks.
5. Use only Kafka topics confirmed in the integration guide and codebase. Do not derive topic names from abstract command names until Blocker 1 is resolved.
6. All debtor-facing service tasks must use the mandatory gated service-task base class.

---

## Data Ownership

Option B uses a dedicated `dcms` schema co-located with Solon's PostgreSQL database unless migration-scope review forces a separate database.

| Entity | Owned by | Store |
|---|---|---|
| Debt account ledger | Solon | Solon PostgreSQL schema |
| Payment records | Solon | Solon PostgreSQL schema |
| Repayment plan | Solon lifecycle + DCMS rules | Solon entity plus DCMS metadata |
| Breathing Space hold | Solon suppression state + DCMS gate decision | Solon suppression table plus `dcms.breathing_space_gate_log` |
| Champion/challenger assignments | DCMS | `dcms.champion_challenger_assignment` |
| Income and expenditure assessment | DCMS | `dcms.ie_assessment` |
| Vulnerability flags | DCMS source of truth | `dcms.vulnerability_record` |
| Communication events | DCMS | `dcms.communication_event` |
| Strategy versions | DCMS | `dcms.strategy_version` |
| Audit trail | DCMS | `dcms.audit_event`, insert-only |
| Customer identity | DCMS master + Solon process entity | `dcms.customer` linked to Solon Taxpayer external reference |

### Core DCMS tables

```sql
dcms.breathing_space_gate_log (
  gate_log_id       UUID PRIMARY KEY,
  case_id           UUID NOT NULL,
  effect_category   VARCHAR NOT NULL,
  decision          VARCHAR NOT NULL,
  suppression_ref   VARCHAR,
  reason_code       VARCHAR NOT NULL,
  evaluated_at      TIMESTAMPTZ NOT NULL,
  evaluated_by      VARCHAR NOT NULL
)
```

```sql
dcms.champion_challenger_assignment (
  assignment_id     UUID PRIMARY KEY,
  case_id           UUID NOT NULL,
  customer_id       UUID NOT NULL,
  strategy_id       UUID NOT NULL,
  variant           VARCHAR NOT NULL, -- champion | challenger
  assigned_at       TIMESTAMPTZ NOT NULL,
  assignment_reason VARCHAR NOT NULL,
  harm_indicator_status VARCHAR NOT NULL,
  overridden_at     TIMESTAMPTZ,
  override_reason   VARCHAR
)
```

```sql
dcms.vulnerability_record (
  vulnerability_record_id UUID PRIMARY KEY,
  customer_id             UUID NOT NULL,
  vulnerability_tier      VARCHAR NOT NULL,
  status                  VARCHAR NOT NULL,
  source                  VARCHAR NOT NULL,
  effective_from          TIMESTAMPTZ NOT NULL,
  effective_to            TIMESTAMPTZ,
  created_at              TIMESTAMPTZ NOT NULL
)
```

```sql
dcms.strategy_version (
  version_id        UUID PRIMARY KEY,
  strategy_id       UUID NOT NULL,
  version_number    INTEGER NOT NULL,
  status            VARCHAR NOT NULL,
  configuration     JSONB NOT NULL,
  created_by        VARCHAR NOT NULL,
  created_at        TIMESTAMPTZ NOT NULL,
  reviewed_by       VARCHAR,
  reviewed_at       TIMESTAMPTZ,
  promoted_at       TIMESTAMPTZ
)
```

### Solon Data Area mirroring

Some DWP-specific fields may need to be visible to Amplio FEEL expressions or Drools rules. The parent document originally proposed mirroring selected DCMS values into Solon Data Area JSONB.

This remains an open design issue for Option B. The Design Critic review found that eventual write-through may be too weak for vulnerability reassignment because RULING-010 requires immediate reassignment from challenger to champion when vulnerability is identified. Before lock, the Solution Architect must choose between:

| Approach | Benefit | Risk |
|---|---|---|
| Data Area mirroring with outbox | FEEL/Drools can read local process data | Staleness window may breach immediate vulnerability reassignment |
| Direct in-process service call | Stronger consistency for critical decisions | More explicit coupling from BPMN delegate to DCMS service |

Until resolved, vulnerability status used for champion/challenger exclusion should be treated as direct-call territory, not eventual-mirror territory.

---

## Contract Between DCMS and Solon

| Contract type | Intended use | Status |
|---|---|---|
| Kafka command/event | Financial and process commands | Blocked by Blocker 1 until topic names, payload schemas, response events, and consumer-group isolation are confirmed |
| Sync HTTP | Query account balance, active suppressions, task state | Blocked by Blocker 2 until endpoint existence and stability guarantees are confirmed |
| In-process Spring beans | DCMS BPMN service task delegates | Supported as Solon's customisation model, but upgrade blast radius must be managed |
| Database | Separate `dcms` schema in same PostgreSQL instance | Requires migration-safety review of Solon Liquibase scope |

No direct cross-schema SQL is allowed from DCMS services into Solon tables unless explicitly approved as an exception. DCMS may link by stable external references and call supported APIs or confirmed service interfaces.

---

## Strategy Versioning

Strategy lifecycle state is held in `dcms.strategy_version`. The Configuration Workspace reads and writes through DCMS BFF endpoints.

| Operation | Mechanism |
|---|---|
| Draft creation | POST `/dcms/strategy/versions` creates a `draft` row |
| Peer review submission | Status update to `pending-review`; starts `dcms-strategy-review.bpmn` |
| Approval | Reviewer task updates status to `approved` |
| Promotion to live | One version becomes `live`; prior live version becomes `archived` |
| Rollback | Prior approved version promoted; rollback event written |
| Audit trail | Every lifecycle event written to `dcms.audit_event` |

Drools DRL remains the default execution model unless the programme explicitly adds a DMN sidecar. Non-technical authoring of entirely new rule types is not met by Option B without that additional sidecar.

---

## User Interface

The user interface is a custom React application following the three-workspace model defined in [`../three-workspace-model.md`](../three-workspace-model.md). Option B uses a richer BFF than Option A. The BFF composes Solon REST responses, DCMS domain service responses, and DCMS read-model data.

### What the UI can do

| Capability | How it works |
|---|---|
| Case Worker - case screen, account timeline, linked accounts | BFF aggregates Solon case/account data and DCMS customer context |
| Case Worker - ID&V, vulnerability, I&E | DCMS services own capture and persistence |
| Case Worker - arrangement creation and schedule display | DCMS validates DWP rules and gating; Solon owns financial posting |
| Case Worker - restriction flag display and runtime action blocking | DCMS services evaluate restrictions and gate prohibited effects |
| Case Worker - transfer with context | Solon task primitives plus DCMS notes/context |
| Operations - queues, SLA, breach volumes | BFF queries Solon tasks plus DCMS operational read models |
| Operations - supervisor override and bulk reassignment | DCMS work allocation rules call Solon task actions where applicable |
| Operations - vulnerability exception reporting | DCMS vulnerability records and gate logs |
| Operations - third-party placement performance | DCMS placement records and Solon financial outcomes |
| Configuration - strategy versioning, diff, approve, rollback | DCMS strategy service and review BPMN |
| Configuration - segmentation threshold editing | DCMS strategy configuration and Drools KIE packaging |
| Configuration - RBAC/user administration | Keycloak/OPA with DCMS policy model |
| Champion/challenger results | DCMS assignment and harm-indicator data |
| Historical simulation results | DCMS read model or pre-computed simulation data |

### What the UI cannot claim until blockers are resolved

| Capability | Reason |
|---|---|
| Stable Solon-backed account/task/suppression views as an external contract | Blocker 2: REST API stability guarantees are not confirmed |
| Asynchronous financial command outcome correlation | Blocker 1: response events and topic semantics are not confirmed |
| Fully compliant vulnerability-driven champion/challenger reassignment | Vulnerability access path is unresolved; eventual Data Area mirroring may be too slow |
| Complete correspondence on-lift disposition | Split ownership between Solon transport and DCMS classification is not yet designed |
| Final MHCM suppression configuration | Blocker 3: `SuppressionExpiryJob` behaviour is not confirmed |

### Workspace-by-workspace capability summary

**Case Worker Workspace** - fully intended under Option B. Main risk is ensuring all action-producing service tasks call the gate and use fresh vulnerability state.

**Operations Workspace** - stronger than Option A because DCMS owns operational read models and can aggregate without relying only on raw Solon REST calls.

**Configuration Workspace** - stronger than Option A for strategy lifecycle, peer review, champion/challenger, and versioning. New rule-type authoring remains developer-gated unless a DMN sidecar is added.

---

## Open Blockers

The following blockers must be resolved before integration design is locked. They are inherited from DESIGN-OPTIONS-002 and apply directly to Option B.

### Blocker 1 - Kafka command-name discrepancy

**What:** The platform reference cites a 24-command catalogue using abstracted financial names such as `PostPaymentCommand`, `WriteOffDebtCommand`, `CreatePaymentPlanCommand`, and `SendCorrespondenceCommand`. The integration guide and Kafka event catalogue instead expose `irm.bpmn-engine.*` topics carrying internal BPMN engine signals. It is not confirmed that the abstracted names are externally publishable Kafka commands.

**Impact on Option B:** The intended inter-layer contract - DCMS publishes commands, Solon emits response events, DCMS correlates outcomes - may not exist as framed. If it does not exist, Option B must either use internal engine signals, which increases coupling, or shift some interactions to sync REST, which activates Blocker 2.

**Resolution:** Inspect the Solon codebase for producer configuration, topic names, payload schemas, response events, and consumer-group isolation. Inspect `revenue-management-be-custom` for existing custom publication patterns. Redesign the contract around the confirmed surface.

### Blocker 2 - Solon REST API stability

**What:** The synchronous HTTP surface is not documented with sufficient stability guarantees for Option B's BFF assumptions.

**Impact on Option B:** The BFF depends on account balance, active suppression, task state, and potentially correspondence/task APIs. If these endpoints are internal or unstable, the BFF cannot treat them as a durable contract.

**Resolution:** Inspect endpoint existence and response shapes in the Solon codebase. Bring a concrete endpoint list to the Solon platform governance conversation and obtain a versioning/stability position.

### Blocker 3 - MHCM `SuppressionExpiryJob` behaviour

**What:** The behaviour of `SuppressionExpiryJob` when `maximumNumberDays: 0` is set is undocumented. If zero means "already expired," MHCM suppressions could be lifted without professional sign-off.

**Impact on Option B:** MHCM compliance cannot be locked until expiry behaviour is known.

**Resolution:** Read `SuppressionExpiryJob` source. If zero is unsafe, use `maximumNumberDays: 36500` plus `overrideEndDateSW: true` and the DCMS-owned release path.

### Compliance contradiction - `suspendActiveInstancesSW`

**What:** `suspendActiveInstancesSW: true` can cause Solon to suspend active BPMN tokens on suppression creation. RULING-016 requires the process to continue internally while prohibited effects are gated at the boundary.

**Impact on Option B:** The design cannot simultaneously claim gate-at-effect semantics and token suspension for the same suppression type.

**Resolution:** Declare `suspendActiveInstancesSW` per suppression type: standard Breathing Space, MHCM, vulnerability overlay, and internal policy holds. Reconcile each setting with the gate-at-effect model.

---

## Risks

| Risk | Severity | Mitigation |
|---|---|---|
| Kafka contract model may not exist as framed | High | Resolve Blocker 1 as full contract verification, not name mapping |
| `BreathingSpaceGatingService` not called at every debtor-facing effect | High | Mandatory service-task base class; fail-fast registration; integration test coverage by gated effect category |
| `suspendActiveInstancesSW` undermines RULING-016 gate-at-effect model | High | Declare setting per suppression type before lock |
| Vulnerability Data Area mirroring creates a staleness window | High | Prefer direct in-process service call for vulnerability decisions unless SA proves mirroring satisfies RULING-010 |
| Split correspondence ownership leaves on-lift disposition unspecified | High | Design explicit on-lift flow for discard versus hold-for-review by suppression reason |
| DCMS schema co-located with Solon schema on the same PostgreSQL instance | Medium | Separate schema, distinct Liquibase context, migration-scope review; escalate to separate DB if needed |
| Champion/challenger harm indicator tracking omitted early | Medium | Build harm indicator columns in sprint 1; do not defer |
| Drools DRL remains developer-gated for new rule types | Medium | Accept as Release 1 constraint or add DMN sidecar |
| In-process Spring context still creates Solon upgrade blast radius | Medium | Enforce internal boundaries, avoid Solon internal beans, add upgrade smoke tests |

---

## Required Actions Before SA Lock

1. Resolve Blocker 1 as complete Kafka contract verification: topic names, payload schemas, response events, and consumer-group isolation.
2. Resolve Blocker 2 by confirming Solon REST endpoint existence, response shape, and stability governance.
3. Resolve Blocker 3 by reading `SuppressionExpiryJob` and documenting the safe MHCM configuration.
4. Declare `suspendActiveInstancesSW` per DCMS suppression type and reconcile with RULING-016.
5. Decide the vulnerability access path: Data Area mirroring versus direct in-process service call.
6. Design the correspondence on-lift disposition flow for queued communications.
7. Decide the Maven-module deployment boundary: same `revenue-management-be-custom` module with package boundaries or separate module in the shared Spring context.
8. Confirm whether Drools-only parameterised authoring is acceptable for Release 1, or whether a DMN sidecar is required.

## Required Actions Before BPMN Authoring Begins

1. Produce a closed enumeration of gated effect categories derived from RULING-005, RULING-010, RULING-011, RULING-014, and RULING-016.
2. Allocate arrangement-creation gating explicitly. `createArrangement` must query live suppression and insolvency state and reject prohibited arrangement creation.
3. Define the mandatory gated service-task base class and startup validation.
4. Design `dcms-breach-to-placement.bpmn` in full before build begins, including PTP loops, supervisor branches, and payment-monitoring correlation.
5. Confirm Amplio constraints in the authoring checklist: sequential parallel gateway, FEEL-only script tasks, service-task rule evaluation, no compensation markers.

---

## Comparison with Option A

Option A and Option B share the same runtime: Solon's JVM, Amplio, Kafka, Drools, Keycloak/OPA, and PostgreSQL. They differ in ownership depth.

| Dimension | Option A | Option B |
|---|---|---|
| DCMS service beans | Minimal extension beans inside Solon custom module | Explicit DWP domain services with internal contracts |
| Data ownership | Mostly Solon Data Area JSONB plus a small number of tables | Dedicated `dcms` schema with domain-owned tables |
| Champion/challenger / IE / vulnerability | Structurally awkward | First-class DCMS domain services |
| BFF | Thin Solon REST proxy | Rich aggregation layer |
| UI coupling | More constrained by Solon API shape | Better insulated by BFF and DCMS read models |
| Delivery speed | Faster initial path | More work, cleaner ownership |
| Upgrade isolation | Low | Medium by discipline, not by hard runtime separation |

Option B is preferred when champion/challenger, IE assessment, vulnerability governance, and strategy lifecycle are in scope for Release 1. If those services are descoped, Option A becomes materially more attractive.

---

## Comparison with Option C

Option B and Option C both give DCMS stronger domain ownership than Option A. Option C adds a hard process/database boundary and rebuilds more platform capability.

| Dimension | Option B | Option C |
|---|---|---|
| Runtime | Same JVM as Solon | Separate DCMS JVM/process |
| Financial primitives | Reused from Solon | Potentially wrapped or rebuilt |
| Data model | Dedicated `dcms` schema co-located with Solon | DCMS-owned database with anti-corruption layer |
| Upgrade isolation | Medium | High |
| Platform value retained | Substantial | Low to medium |
| Delivery cost | Medium | High |
| Java version escape | Not if Solon remains Java 17 | Yes |

Option C becomes stronger if the programme mandates a JVM version incompatible with Solon's host JVM or if Solon exposes no stable financial/process contract. If Solon moves to Java 25 as anticipated but unconfirmed in the parent document, Option B strengthens because the Java-version reason for Option C weakens.

---

## Interface and Dependency Impact

### Upstream - Solon platform team

- Agreement required on DCMS custom module structure, BPMN deployment into Amplio, Drools KIE packaging, and custom batch deployment.
- Kafka contract verification required before DCMS relies on asynchronous financial commands.
- REST stability governance required before the BFF treats Solon query endpoints as stable.
- Suppression configuration and `suspendActiveInstancesSW` semantics require explicit design review.

### Downstream - DWP integrations

Option B connects to the same DWP integration points as Options A and C: DWP SSO, WorldPay, DWP IVR, DWP Notifications, DWP Data Integration Platform, FTPS, and DWP Payment Allocation. The option choice does not remove those integrations, but it determines that orchestration and domain policy decisions live in DCMS.

### React UI

The React application calls the DCMS BFF. The BFF shields the UI from raw Solon response shapes and composes DCMS domain data with Solon financial/task/suppression data. This gives the UI more stable screen-specific view models than Option A.

---

## Constraints Summary

- **Single JVM:** Option B runs inside Solon's JVM. The documented baseline is Java 17 until Solon GA release notes confirm otherwise.
- **Liquibase:** Solon uses Liquibase. DCMS schema changes must use compatible migration discipline unless separately scoped.
- **Drools DRL:** DMN is not available by default. New structural rule types remain developer-gated unless a DMN sidecar is added.
- **Kafka topics:** Only confirmed topics may be used. Blocker 1 must be resolved before relying on abstracted command names.
- **REST APIs:** Only endpoints with confirmed existence and stability may be treated as BFF contracts.
- **Amplio Parallel Gateway:** Sequential-only.
- **Amplio Script Tasks:** FEEL-only.
- **BPMN compensation:** Not implemented; use explicit error paths.
- **Solon reference BPMN:** Must not be modified. DCMS deploys new process definitions.
- **MHCM:** Do not assume `maximumNumberDays: 0` is safe until `SuppressionExpiryJob` is inspected.
- **Gating:** All debtor-facing effects must pass through the DCMS gate before execution.
