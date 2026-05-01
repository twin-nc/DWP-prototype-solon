# DESIGN-OPTIONS-002A: Option A — Minimal Custom Layer

**Document ID:** DESIGN-OPTIONS-002A
**Date:** 2026-05-01
**Status:** PROPOSED — Integration design lock BLOCKED pending resolution of Blocker 1 and Blocker 3 (see below). Blocker 2 applies at reduced severity under Option A; documented in Open Blockers section.
**Parent document:** [DESIGN-OPTIONS-002: Layer Thickness and Boundary Placement](./DESIGN-OPTIONS-002-layer-thickness-and-boundary.md)
**Architecture diagram:** [DESIGN-OPTIONS-002-option-A-architecture.drawio](./DESIGN-OPTIONS-002-option-A-architecture.drawio)
**Author:** Derived from Delivery Designer + Platform Expert + Design Critic reviews

---

## What This Document Is

Option A is one of three layer-thickness options evaluated in DESIGN-OPTIONS-002. This document extracts the full Option A design into a standalone reference. It covers architecture, boundary placement, BPMN approach, UI capabilities, strategy versioning, risks, open blockers, and builder guardrails.

The parent document (DESIGN-OPTIONS-002) retains the three-way tradeoff analysis, recommendation, and decision levers. This document is for teams building or reviewing Option A specifically.

**Current recommendation:** Option B. Option A is a valid path if champion/challenger and IE engine are descoped from initial delivery, or if the programme confirms that only a small number of DWP-specific business rules exist. See parent document Lever 3.

---

## Summary

DCMS is a set of Solon extensions running inside Solon's JVM (Java 17). DWP-specific BPMN process definitions are authored from scratch and deployed into the shared Amplio engine alongside Solon's unmodified reference processes. Solon's financial ledger, batch engine, Kafka command bus, suppression model, and task tray are reused as-is. DWP-specific domain services — breathing space gating, champion/challenger, IE assessment, vulnerability governance, communication suppression, strategy versioning — are Spring beans in a DCMS Maven module (`revenue-management-be-custom`) registered via component scan into Solon's application context.

The user interface is a new custom React application following the three-workspace model (Case Worker, Operations, Configuration). It calls Solon REST APIs via a thin BFF that also exposes a small number of DCMS custom endpoints.

---

## Architecture

### Layer structure

```
┌─────────────────────────────────────────────────────────────┐
│  UI Layer (React SPA)                                        │
│  Case Worker · Operations · Configuration workspaces         │
│  Thin BFF: Solon REST proxy + /dcms/* custom endpoints       │
└──────────────────────────┬──────────────────────────────────┘
                           │ REST
┌──────────────────────────▼──────────────────────────────────┐
│  Solon REST API Gateway · Keycloak Bearer JWT                │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│  Solon Tax v2.3.0 — Single JVM (Java 17)                    │
│                                                              │
│  ┌──────────┐ ┌────────────┐ ┌────────┐ ┌────────────────┐ │
│  │ Amplio   │ │ Suppression│ │ Drools │ │ Kafka Command  │ │
│  │ Process  │ │ Module     │ │ KIE    │ │ Bus            │ │
│  │ Engine   │ │            │ │        │ │                │ │
│  └──────────┘ └────────────┘ └────────┘ └────────────────┘ │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ revenue-management-be-custom (Maven module — DCMS)   │   │
│  │                                                       │   │
│  │  BreathingSpaceGatingService                          │   │
│  │  ChampionChallengerService          ⚠ awkward         │   │
│  │  IEAssessmentService                ⚠ awkward         │   │
│  │  VulnerabilityClassificationService ⚠ awkward         │   │
│  │  CommunicationSuppressionService                      │   │
│  │  StrategyVersioningService                            │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                              │
│  PostgreSQL 16 — Solon schema + DCMS additions               │
│  Data Area JSONB extensions · dcms_strategy_version          │
│  champion_challenger_assignment                               │
└─────────────────────────────────────────────────────────────┘
```

### Key architectural properties

- DCMS is not a separate service. It is a Maven module running inside Solon's JVM.
- No HTTP boundary between DCMS and Solon. Communication between DCMS Spring beans and Solon internals is in-process.
- Solon reference BPMN processes are **not modified**. DWP processes are new definitions deployed into Amplio alongside Solon's.
- All async operations use Solon's Kafka command catalogue. No proprietary DCMS Kafka topics are introduced.
- DCMS extends Solon entities through Data Area JSONB. Two new tables are added: `dcms_strategy_version` and `champion_challenger_assignment`.
- The UI is a new React application. It is not Solon's UI. It uses the Solon visual language as its design reference.

---

## Boundary Placement

| Responsibility | Lives in Solon | Lives in DCMS custom layer |
|---|---|---|
| Case lifecycle BPMN | Solon reference processes (unmodified — design reference only) | Five new DWP BPMN definitions deployed into Amplio |
| Financial ledger, payment, write-off | Solon core | Called via Solon Kafka commands |
| Debt recovery workflow | `DEBT_RECOVERY_PROCESS.bpmn` (unmodified) | `dcms-intake-and-first-contact.bpmn`, `dcms-breach-to-placement.bpmn` |
| DCA handover | `CASE_HANDOVER_COLLECTION_AGENCY_PROCESS.bpmn` (called as sub-process if needed) | DWP pre-placement disclosure gate in `dcms-breach-to-placement.bpmn` |
| Business rules (segmentation, strategy) | Drools KIE (DWP rules added to custom KIE module) | — |
| Suppression model | Solon `CreateSuppression` / `ReleaseSuppression` | Thin wrapper Spring bean |
| Breathing Space gating service | Source of truth: Solon suppression query | Spring bean in `revenue-management-be-custom`, called from DCMS BPMN service task delegates |
| Champion/challenger | Not present in Solon | `ChampionChallengerService` — custom Spring bean (⚠ structurally awkward) |
| Income and expenditure | Not present in Solon | `IEAssessmentService` — custom Spring bean (⚠ structurally awkward) |
| Vulnerability governance | Not present in Solon | `VulnerabilityClassificationService` — custom Spring bean (⚠ structurally awkward) |
| Communication suppression | `CORRESPONDENCE_PROCESS.bpmn` for transport | `CommunicationSuppressionService` — pre-dispatch guard called from DCMS BPMN service tasks |
| Strategy versioning and peer review | — | `StrategyVersioningService` — reads/writes `dcms_strategy_version`; peer review as `dcms-strategy-review.bpmn` |
| UI | — | New React SPA — three-workspace model (Case Worker, Operations, Configuration) |
| Auth | Solon Keycloak + OPA | DWP realm configuration; DCMS-specific Rego policies (deployment model TBD — see Open Blockers) |

### Breathing Space gating service

The `BreathingSpaceGatingService` is a Spring bean in `revenue-management-be-custom`. It calls Solon's suppression query to determine whether a moratorium is active, then applies DCMS-specific evaluation logic — moratorium type, MHCM vs standard breathing space, deferred output flagging — and returns a `GatingDecision` (PERMIT / SUPPRESS / DEFER_PENDING_MORATORIUM_END).

Every DCMS BPMN service task that could produce a prohibited debtor-facing effect calls this service before executing. RULING-016's gate-at-effect model is satisfied: Amplio advances internal state; the gate stops prohibited effects before they exit the service task. No process-freeze is required.

**Standard Breathing Space (60 days):** Solon `SuppressionType` configured with `maximumNumberDays: 60` / `overrideEndDateSW: false`. `SuppressionExpiryJob` handles the 60-day ceiling natively. DCMS gating service calls the suppression query; it does not duplicate the 60-day enforcement.

**MHCM (no fixed end date):** Solon `SuppressionType` configured with `maximumNumberDays: 36500` (100-year practical ceiling) and `overrideEndDateSW: true`. See [MHCM suppression](#mhcm-suppression) below.

**⚠ The `suspendActiveInstancesSW` setting on each DCMS `SuppressionType` must be declared before lock.** If `true`, Solon auto-suspends BPMN process tokens on suppression creation — violating RULING-016 §3 guardrails 1 and 4 (voluntary payments, balance recalculation, and required audit events must continue). If `false`, DCMS's gating service carries 100% of effect-suppression responsibility. Both branches are coherent but mutually exclusive. The SA must choose per suppression type and record the decision.

### MHCM suppression

Per RULING-016 §5, the Mental Health Crisis Moratorium has no fixed end date. It lasts for the duration of mental health crisis treatment plus 30 days and ends only on professional sign-off.

**Configuration:** Never set `maximumNumberDays: 0` on the MHCM `SuppressionType`. The behaviour of `SuppressionExpiryJob` under `maximumNumberDays: 0` is undocumented. If the job interprets zero as "already expired," MHCM suppressions are silently lifted without professional sign-off — a breach of Regulation 21 of the Debt Respite Scheme Regulations 2020 (criminal-liability consequence). This is Blocker 3 and must be resolved by reading the `SuppressionExpiryJob` source before MHCM detailed design is locked.

**Mitigation in effect:** Set `maximumNumberDays: 36500` and `overrideEndDateSW: true`. The DCMS-owned release path is:

1. AMHP/clinician professional sign-off captured in DCMS via `MHCM_RELEASE_PROCESS.bpmn`
2. DCMS calculates the 30-day post-treatment tail end date
3. DCMS issues `ReleaseSuppressionCommand` to Solon with the calculated expiry date
4. `SuppressionExpiryJob` handles cleanup on the scheduled date

This configuration workaround is documented as a known operational risk and must be recorded in the MHCM operational runbook. The gating service evaluates MHCM and standard breathing space suppressions as compounding (not alternatives) because a debtor in MHCM is by definition vulnerable under FCA FG21/1.

---

## BPMN Approach

### New process definitions

DWP-specific collection workflows are new BPMN process definitions authored by the DCMS team and deployed into Amplio. No Solon reference process is modified. Solon's reference processes (`DEBT_RECOVERY_PROCESS.bpmn`, `SUSPEND_PROCESS_CHECK.bpmn`, `CASE_HANDOVER_COLLECTION_AGENCY_PROCESS.bpmn`, `PAYMENT_DEFERRAL.bpmn`) are studied for Amplio variable-scoping patterns and Kafka command usage, but DCMS deploys its own equivalent processes.

| Process definition | Demo flow | Amplio complexity |
|---|---|---|
| `dcms-intake-and-first-contact.bpmn` | Flow 1 — Intake to first contact | Low — linear sequence, no parallel gateways |
| `dcms-vulnerability-to-resolution.bpmn` | Flow 2 — Vulnerability to resolution | Medium — suppression atomicity; child process spawn for payment monitoring |
| `dcms-payment-monitoring.bpmn` | Flows 2 and 3 — spawned on arrangement creation | Low — timer and REST poll pattern |
| `dcms-breach-to-placement.bpmn` | Flow 3 — Breach to third-party placement | **High** — PTP loop, supervisor branching, inter-process message correlation |
| `dcms-strategy-review.bpmn` | Flow 5 — Strategy change approval workflow | Low — three-to-five task sequence |

Flows 4 (complex household) and 6 (executive dashboard) require no new BPMN processes. Flow 4's restriction enforcement is a pre-execution guard Spring bean called from within service task delegates. Flow 6 is a pure read path against Solon REST APIs.

**`dcms-breach-to-placement.bpmn` must be designed in full before the build sprint begins.** The PTP loop, supervisor-approval branching, and inter-process message correlation for the payment monitoring child process are the highest-complexity BPMN in the six flows. Timebox two days for design and Amplio constraint validation before build begins.

### Amplio authoring constraints (apply to all DCMS BPMN)

These are platform-forced constraints. They must be added to all builder-facing handoff notes before BPMN authoring begins.

1. **Parallel Gateway is sequential-only.** Amplio executes Parallel Gateway branches sequentially. Do not design for concurrent execution. Any service task that depends on concurrent notification dispatch or concurrent write-off approval must be redesigned as sequential steps.
2. **Script Tasks are FEEL-only.** No Groovy or JavaScript. All imperative logic goes in Java delegate classes.
3. **Business Rule Tasks must be Service Tasks.** DMN is not supported. Rule evaluation requires a Service Task wired synchronously to Drools KIE (`POST /drools-runtime/execute-stateless/{containerId}`).
4. **Compensation events are not implemented.** SAGA-style flows must use explicit error boundary events routing to compensation logic — not BPMN compensation markers.
5. **Use only Kafka topics confirmed in integration guide §5.** Do not derive topic names from the platform reference's abstracted 24-command list without confirming the actual Kafka topic in `external_sys_docs/solon/solon_tax_2.3.0_integration_guide.md` §5. This is subject to Blocker 1.

---

## Data Ownership

DCMS extends Solon entities almost entirely through Data Area JSONB. DWP-specific fields land in the `data_area` column of Solon's Taxpayer, Case, Suppression, and Task entities.

| Field | Solon entity extended | JSONB path |
|---|---|---|
| Vulnerability flag and tier | Taxpayer | `data_area.dcms.vulnerabilityTier` |
| Breathing space hold type (standard / MHCM) | Suppression | `data_area.dcms.holdType` |
| IE assessment reference | Case | `data_area.dcms.ieAssessmentRef` |
| Champion/challenger variant | Case | `data_area.dcms.ccVariant` |
| DWP debt type | Case | `data_area.dcms.dwpDebtType` |
| Deduction-from-benefit flag | Case | `data_area.dcms.dfbFlag` |

Two new tables are introduced in Solon's PostgreSQL schema:

### `dcms_strategy_version`

```sql
dcms_strategy_version (
  version_id        UUID PRIMARY KEY,
  strategy_id       UUID NOT NULL,
  version_number    INTEGER NOT NULL,
  status            VARCHAR NOT NULL,  -- draft | pending-review | approved | live | archived
  configuration     JSONB NOT NULL,
  created_by        VARCHAR NOT NULL,
  created_at        TIMESTAMPTZ NOT NULL,
  reviewed_by       VARCHAR,
  reviewed_at       TIMESTAMPTZ,
  promoted_at       TIMESTAMPTZ
)
```

### `champion_challenger_assignment`

Records which variant (champion or challenger) each account was assigned to. Required from the first delivery sprint — RULING-010 guardrail 3 requires harm indicator tracking. Harm indicator columns must be present from day one; do not defer.

---

## Strategy Versioning

Strategy version state is held in `dcms_strategy_version`. The Configuration Workspace UI reads and writes this table via custom DCMS BFF endpoints.

| Operation | Mechanism |
|---|---|
| Draft creation | POST `/dcms/strategy/versions` — creates new row in `draft` status |
| Peer review submission | Status update to `pending-review`; triggers `dcms-strategy-review.bpmn` in Amplio |
| Approval | Reviewer action in `dcms-strategy-review.bpmn`; status updated to `approved` |
| Promotion to live | Status update to `live`; prior live row archived |
| Rollback | Status update on target prior version to `live`; current live row archived; new row written for the rollback event |
| Audit trail | Every strategy lifecycle event written to DCMS audit trail |

The Configuration Workspace shows live and draft versions side by side. Consecutive-version diff is rendered from the `configuration` JSONB delta. Comparing non-consecutive versions may require developer tooling.

---

## User Interface

The user interface is a new custom-built React application. It follows the three-workspace model defined in [`../three-workspace-model.md`](../three-workspace-model.md). All three workspaces are present and navigable under Option A.

The shared product shell (single SPA, Keycloak-backed session, role-based workspace routing) is unchanged. Under Option A, the BFF is thin — mostly a Solon REST proxy with a small number of DCMS custom endpoints (`/dcms/ie`, `/dcms/vulnerability`, `/dcms/strategy`, `/dcms/champion-challenger`).

### What the UI can do

| Capability | How it works |
|---|---|
| Case Worker — case screen, account timeline, linked accounts | BFF queries Solon Case Management + Registration REST APIs |
| Case Worker — ID&V panel, vulnerability capture form, I&E form | Custom DCMS endpoints in `revenue-management-be-custom`; data written to Solon Data Area JSONB |
| Case Worker — arrangement creation and payment schedule display | BFF calls Solon Taxpayer Accounting REST API; arrangement written via Kafka `CreatePaymentPlanCommand` |
| Case Worker — restriction flag display and runtime action blocking | Pre-execution guard Spring bean; UI shows explicit block alert when guard fires |
| Case Worker — transfer with full context carried forward | Solon Human Task Management API — reassign task; notes and suppression state carried on Solon entities |
| Operations — queue volumes, SLA health, breach count | BFF queries Solon Human Task Management (task counts) and Case Management (case stage counts) |
| Operations — bulk reassignment | BFF calls Solon Human Task Management — reassign task; writes audit event |
| Operations — vulnerability exception report | BFF queries Solon Suppression Management — active suppression counts and flag age |
| Operations — third-party placement performance | BFF reads from DCMS JSONB extension on placement case entities |
| Configuration — strategy version authoring, diff, peer review, approve, rollback | Custom DCMS endpoints reading and writing `dcms_strategy_version` |
| Configuration — segmentation threshold editing | Custom DCMS endpoints; parameter changes write to Drools KIE container configuration JSONB |
| Configuration — vulnerability type reference list | Solon Reference Data API (codelists) |
| Configuration — RBAC and user management | Keycloak admin API called via custom DCMS BFF endpoint |
| Champion/challenger configuration and results view | Custom DCMS endpoints; results from `champion_challenger_assignment` table and DCMS JSONB |
| Historical simulation results view | Pre-computed simulation results stored in JSONB; rendered as static comparison view |

### What the UI cannot do (hard constraints under Option A)

| Capability | Reason |
|---|---|
| Live champion/challenger results from real operational data pipeline | No analytics pipeline in Option A. C/C results are seeded from pre-computed historical data. A real-time operational pipeline is out of scope for Release 1 under Option A. |
| Fully interactive historical simulation against live process replay | No sandbox process engine. Simulation results are pre-computed from a historical snapshot — not live replay of selected accounts through a proposed strategy. |
| Non-technical strategy authoring for new rule types | Structural Drools DRL changes require a developer. The UI provides a parameterised template library where business users adjust parameters of existing rule templates. New rule types are developer-gated. |
| Strategy diff across arbitrary non-consecutive version pairs | Consecutive-version diff is visible in the UI. Comparing non-consecutive versions may require developer tooling. |
| UI shape fully decoupled from Solon API response structure | The BFF is thin. If Solon's REST API for a given entity does not expose a field DCMS needs, a custom endpoint must be added to `revenue-management-be-custom`. API gaps are discovered iteratively per screen. |
| Dashboard drill-down backed by a separate analytics database | No analytics or operational DB under Option A. All dashboard data is read from Solon REST APIs and DCMS JSONB at query time. Complex aggregations may require materialised views added incrementally. |

### Workspace-by-workspace capability summary

**Case Worker Workspace** — fully functional. All six demo flows that touch case worker actions (Flows 1–4) are served from Solon REST + DCMS JSONB with no structural gaps.

**Operations Workspace** — functional for live queue, SLA, and breach data. Dashboard read performance is the main risk. Aggregation queries against Solon REST under concurrent dashboard loads must be validated early against a Solon instance under realistic load.

**Configuration Workspace** — functional for parameterised strategy editing, version management, peer review, and RBAC. Non-technical authoring of new rule types is not possible without developer involvement.

---

## Open Blockers

The following blockers must be resolved before integration design is locked. They are inherited from DESIGN-OPTIONS-002 and apply to Option A.

### Blocker 1 — Kafka command-name discrepancy

**What:** The 24-command catalogue cited in the platform reference (`PostPaymentCommand`, `WriteOffDebtCommand`, `CreatePaymentPlanCommand`, `SendCorrespondenceCommand`, etc.) does not appear as externally-publishable topics in integration guide §5. The actual catalogue uses `irm.bpmn-engine.*` topics carrying internal BPMN engine signals. If the abstracted names are not externally-publishable Kafka commands, the inter-layer contract model collapses.

**Impact on Option A:** The intake process start event depends on an externally-triggerable Kafka topic. If no such topic exists, `dcms-intake-and-first-contact.bpmn` cannot be started via Kafka.

**Resolution:** Inspect the Kafka producer configuration in the Solon codebase for actual topic name strings. Inspect `revenue-management-be-custom` to see how existing extensions publish commands. If abstracted commands are not externally publishable, redesign the inter-layer contract around the actual publishable surface. Use REST polling as a fallback trigger if Kafka topics are not available.

**Verify:** Topic names · payload schemas · response events · consumer-group isolation.

### Blocker 2 — Solon REST API stability (reduced severity under Option A)

**What:** The integration guide's synchronous HTTP section is thin. The stability of Solon REST APIs (`account balance`, `active suppressions`, `task state`) under Solon upgrades is not documented.

**Impact on Option A:** The BFF calls these endpoints directly. API gaps are discovered per screen and resolved by adding custom endpoints to `revenue-management-be-custom`. Stability risk is lower than under Option B because the BFF is a thin proxy — breaking changes are surfaced quickly and resolved in the custom layer. However, the governance question (whether the Solon team treats these as a versioned external contract) still requires a conversation with the Solon platform team.

**Resolution:** Inspect the Solon codebase to confirm which endpoints exist and what they return. Bring a specific endpoint list to the Solon platform team governance conversation.

### Blocker 3 — MHCM SuppressionExpiryJob behaviour

**What:** The behaviour of `SuppressionExpiryJob` when `maximumNumberDays: 0` is set is undocumented. If the job interprets zero as "already expired," MHCM suppressions are silently lifted without professional sign-off — a breach of Regulation 21 of the Debt Respite Scheme Regulations 2020 (criminal-liability consequence).

**Resolution:** Read `SuppressionExpiryJob` source from the Solon codebase. Inspect the conditional branch for `maximumNumberDays`.

- If "already expired" is confirmed: apply the `maximumNumberDays: 36500` + `overrideEndDateSW: true` + DCMS-owned release path mitigation described in the [MHCM suppression](#mhcm-suppression) section above. Document as a known operational risk in the MHCM runbook.
- If "no maximum" is confirmed: `maximumNumberDays: 0` is safe and no workaround is needed.

**Current status:** Mitigation (`maxDays: 36500` / `overrideEndDateSW: true`) is applied by default until Solon codebase inspection confirms the safe interpretation.

---

## Risks

| Risk | Severity | Mitigation |
|---|---|---|
| `dcms-breach-to-placement.bpmn` authoring complexity underestimated — PTP loop, supervisor branching, inter-process message correlation | High | Design and Amplio-constraint-validate this process before build begins. Timebox two days. |
| Blocker 1 blocks intake trigger — if no externally-publishable Kafka topic, `dcms-intake-and-first-contact.bpmn` cannot start via Kafka | High | Resolve Blocker 1 before authoring begins. Fallback: REST polling as the intake trigger. |
| Drools DRL requires developer involvement for new rule types — DWP policy team cannot make structural rule changes without IT | High | Accept as a constraint for Release 1. Parameterised template library is the workaround. Document as a known constraint and obtain DWP sign-off. |
| Java 17 / Liquibase constraint — DCMS custom modules must use Java 17 in-process; no escape path within Option A | High | Confirm Java version decision before committing to Option A. If Java 21 is required, this forces Option C. |
| Solon REST API performance under dashboard query load — Operations Workspace aggregates multiple REST endpoints at query time with no analytics DB buffer | Medium | Early performance spike against Solon REST APIs under realistic concurrent load. Add read-model tables incrementally if latency is unacceptable. |
| Solon Data Area JSONB fields are schema-less — complex dashboard aggregation queries degrade | Medium | Materialise DWP-specific fields into a read-model from day one for any field queried in dashboard aggregations. |
| Champion/challenger and IE engine are significant domain services embedded inside the Solon custom module — evolution constrained by Solon entity model | Medium | Define clear internal hexagonal boundaries within `revenue-management-be-custom` from day one. These services (⚠ marked awkward) must have explicit internal API contracts, not free-form access to Solon repositories. |
| `BreathingSpaceGatingService` not called at all debtor-facing service tasks — developer discipline gap leaves a task ungated | High | Enforce via a custom Amplio service task base class that makes the gate call mandatory. Fail-fast at startup if the gate is not registered for a task declared `debtor_facing = true`. Integration test asserts gate coverage per process. |

---

## Required Actions Before SA Lock

These are inherited from the Design Critic review in DESIGN-OPTIONS-002 and are mandatory before Option A can be locked.

1. Resolve **Blocker 1** as a complete Kafka contract verification — topic names, payload schemas, response events, consumer-group isolation guarantees.
2. Resolve **Blocker 3** — read `SuppressionExpiryJob` source. Apply the `maxDays: 36500` + `overrideEndDateSW: true` mitigation if the "already expired" interpretation is confirmed.
3. Declare **`suspendActiveInstancesSW`** per DCMS suppression type (standard Breathing Space, MHCM, vulnerability overlay, internal policy holds) and reconcile with RULING-016 guardrails.
4. Confirm that **non-technical DMN authoring** is acceptable as a Release 1 constraint (parameterised template library workaround). Obtain DWP sign-off.

## Required Actions Before BPMN Authoring Begins

5. Produce a **closed enumeration of gated effect categories** derived from RULING-005, RULING-010, RULING-011, RULING-014, RULING-016. The informal definition in the current design will produce inconsistent gating decisions across BPMN authors.
6. Allocate **arrangement-creation gating** (RULING-014 guardrail 2) explicitly to a layer. `createArrangement` must query the live suppression log and reject if a Breathing Space or insolvency suppression is active. The gate-call site (BPMN start event, REST endpoint, or Spring service method) must be declared.

---

## Comparison with Option B

Option A and Option B share the same runtime (Solon JVM, Amplio, Kafka, Drools) and the same BPMN authoring approach (new process definitions, not modified reference processes). The stated differences are:

| Dimension | Option A | Option B |
|---|---|---|
| DCMS service beans | Inside `revenue-management-be-custom` — same module as Solon's custom beans | Separate Maven module in the same Spring context, or same module with explicit internal package boundaries |
| Data ownership | All DCMS data in Solon Data Area JSONB extensions | DCMS data in a dedicated `dcms` schema; separate tables per domain service |
| Champion/challenger / IE / vulnerability | Squeezed into Solon extension seam — structurally awkward | Own services with own schemas — cleaner ownership |
| BFF | Thin Solon REST proxy + small number of DCMS custom endpoints | Richer aggregation layer that decouples UI from Solon API shapes |
| Upgrade blast radius | DCMS beans share Solon's class graph — Solon refactors break DCMS even if the Kafka/REST surface is unchanged | Claimed to be contained, but in-process model means the blast radius is a matter of discipline, not architectural separation |

The recommendation favours Option B because champion/challenger, IE assessment, and vulnerability governance are large enough domain services to warrant their own schemas and internal API contracts. Squeezing them into Solon's extension seam (Option A) constrains their evolution and makes the internal boundaries harder to enforce. If these three services are descoped from initial delivery, Option A becomes the faster and lower-risk path.

---

## Interface and Dependency Impact

### Upstream — Solon platform team

- Agreement required on the DCMS Maven module structure in `revenue-management-be-custom`, Drools KIE packaging, and BPMN deployment process into the shared Amplio engine.
- Kafka command catalogue used as-is — no new Solon commands proposed; only documented commands used (subject to Blocker 1).
- Solon REST API stability governance conversation required before BFF endpoint list is finalised.

### Downstream — DWP integrations

Option A connects to the same DWP integration points as Options B and C: DWP SSO (INT01), WorldPay, DWP IVR, DWP Notifications, DWP Data Integration Platform, FTPS, DWP Payment Allocation. No downstream impact from the option choice.

### React UI

Under Option A, the React application calls Solon REST APIs via a thin BFF. The UI shape is constrained by Solon API responses. API gaps are discovered and resolved per screen by adding custom endpoints to `revenue-management-be-custom`.

---

## Constraints Summary

- **Java 17** — hard constraint. No escape path within Option A. DCMS custom modules compile to Java 17 in-process.
- **Liquibase** — Solon uses Liquibase. DCMS additions to Solon's PostgreSQL schema use Liquibase. Flyway is not available within Option A.
- **Drools DRL** — new rule types require developer involvement. DMN is not supported. Parameterised template library is the Release 1 workaround.
- **Kafka topics** — only topics confirmed in integration guide §5 may be used. Do not derive topic names from the abstracted platform reference. (Subject to Blocker 1.)
- **Amplio Parallel Gateway** — sequential-only. No concurrent branch execution.
- **Amplio Script Tasks** — FEEL only.
- **Solon reference BPMN** — must not be modified. DCMS deploys new process definitions.
- **MHCM SuppressionType** — never set `maximumNumberDays: 0`. Use `maximumNumberDays: 36500` + `overrideEndDateSW: true` until Blocker 3 is resolved.
