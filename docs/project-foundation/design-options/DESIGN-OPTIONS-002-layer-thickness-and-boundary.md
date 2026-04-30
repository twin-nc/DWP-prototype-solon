# DESIGN-OPTIONS-002: Layer Thickness and Boundary Placement

**Document ID:** DESIGN-OPTIONS-002
**Date:** 2026-04-30
**Status:** PROPOSED — awaiting Design Critic review, then Solution Architect lock
**Author:** Delivery Designer Agent
**Supersedes:** Any layer-thickness framing from the pre-pivot period. The peer-Flowable question is closed.
**Authoritative inputs read:** ADR-018, RULING-016, `solon-tax-platform-reference.md`, `amplio-process-engine-reference.md`, `solon-tax-feasibility-analysis.md`, `docs/memory.md`, `communications-domain-pack.md`, RULING-010.

---

## Missing Input Declaration

Before proceeding: the following inputs are absent or unresolved and affect detail but not the options pass itself.

| Gap | Impact |
|---|---|
| DDE-OQ-BS-PROCESS-01 (Breathing Space comms scope — DWP policy overlay vs statutory floor) | Affects suppression service category model depth; does not change where the service sits |
| DDE-OQ-BS-PROCESS-02 (Deduction-from-benefit suspension ownership) | Affects whether moratorium-start BPMN includes an outbound integration call or a manual task; does not change boundary placement |
| DDE-OQ-12 / DDE-OQ-13 (Champion/challenger thresholds and vulnerable-customer policy) | Affects champion/challenger service configuration; does not change which option is chosen |
| No confirmed Java 17 vs Java 21 resolution | Material under all options; flagged in decision levers |
| No confirmed Drools vs DMN resolution | Material under all options; flagged in decision levers |

---

## Recommendation (read this first)

**Recommend Option B: Medium Custom Layer — DWP Domain Services Orchestrating Solon Primitives.**

RULING-016 changed the design problem from "how do we freeze the engine" to "where does the gating service live." That reframe favours a medium layer: Solon's process engine (Amplio), ledger (billing/payment/write-off), batch engine, Kafka bus, suppression model, and task tray are reused as-is. A dedicated `BreathingSpaceGatingService` lives in the DCMS custom layer, called as a Spring bean guard before every Amplio step that produces a debtor-facing effect. DWP-specific BPMN processes are new process definitions deployed into the same Amplio engine alongside Solon's reference processes. Solon's financial primitives are addressed via Kafka commands and the documented 24-command catalogue; the DCMS layer does not call Solon repositories directly.

Option A (minimal) is tempting for speed but cedes too much DWP-specific behaviour to Solon configuration, leaving champion/challenger, income and expenditure, vulnerability governance, and the gating service structurally awkward. Option C (heavy) rebuilds components — ledger, payment allocation, batch — that Solon provides adequately, removing the platform value with no corresponding gain.

The recommendation flips to Option A if DWP confirms that only a handful of DWP-specific business rules exist and that champion/challenger and vulnerability tiers are out of scope for the initial delivery. It flips to Option C if programme direction changes to require Java 21 or Flyway in the DCMS custom tier on a shared database schema with Solon — making the integration surface too hazardous.

---

## Context: What RULING-016 Changes for This Design Pass

The prior framing — "how do we implement a non-interrupting boundary event for Breathing Space when Amplio only supports interrupting" — assumed we needed to freeze the Amplio process engine. RULING-016 (30 April 2026) rules that the Debt Respite Scheme Regulations 2020 are effect-enumerated, not process-prohibiting. Amplio can continue advancing moratorium-flagged cases through internal state transitions. The compliance obligation is a gate placed at every step that could produce a prohibited debtor-facing effect (collection contact, enforcement instruction, deduction-from-benefit instruction, DCA instruction, interest accrual). This is a gating service question, not a process-freeze question.

Two open DWP sign-off questions remain (DDE-OQ-BS-PROCESS-01 and DDE-OQ-BS-PROCESS-02) but neither blocks the options pass. Both affect internal detail of the gating service, not its existence or placement.

---

## Option A: Minimal Custom Layer — Configure and Extend Solon

### Layer thickness

Thin. DCMS is a set of Solon extensions: Drools rule additions in the custom KIE container, Data Area JSONB extensions on Solon entities, a small number of custom Spring beans registered via component scan in `revenue-management-be-custom`, and a new React UI calling Solon's existing REST APIs. Solon's reference BPMN processes are used substantially as-is, with light modification for DWP-specific routing.

### Boundary placement

| Responsibility | Lives in Solon | Lives in DCMS custom layer |
|---|---|---|
| Case lifecycle BPMN | Solon reference processes (modified) | Minor DWP-specific sub-processes |
| Financial ledger, payment, write-off | Solon core | — |
| Debt recovery workflow | DEBT_RECOVERY_PROCESS.bpmn (modified) | — |
| DCA handover | CASE_HANDOVER_COLLECTION_AGENCY_PROCESS.bpmn | DWP-specific pre-placement disclosure gate |
| Business rules (segmentation, strategy) | Drools KIE (DWP rules added to custom KIE module) | — |
| Suppression model | Solon CreateSuppression / ReleaseSuppression | Thin wrapper Spring bean |
| Breathing Space gating service | Near-Solon: a Spring bean in the custom layer calling Solon's suppression query | Called from within modified Solon BPMN service tasks |
| Champion/challenger | Not present in Solon — new custom Spring bean | Champion/challenger assignment service |
| Income and expenditure | Not present in Solon — new custom Spring bean | IE capture service |
| Vulnerability governance | Not present in Solon — custom Spring bean | VulnerabilityClassificationService |
| Communication suppression | Solon CORRESPONDENCE_PROCESS.bpmn with a pre-dispatch guard | CommunicationSuppressionService bean |
| UI | — | New React app, calling Solon REST APIs + custom endpoints |
| Auth | Solon Keycloak + OPA | DWP realm configuration |

**Breathing Space gating service location under Option A:** Spring bean in the DCMS custom layer (`revenue-management-be-custom`), in-process with Solon, registered via component scan. Called as a pre-execution guard within service task Java delegate overrides. Solon's existing suppression query (`CreateSuppression` / active suppression check) is the underlying source of truth. The gating bean wraps this check and enforces RULING-016 guardrail 2 (check at point of effect, not at case intake). The interrupting-vs-non-interrupting constraint is dissolved by RULING-016: Amplio does not need to non-interrupt; the gate stops the prohibited effect before it exits the service task.

**Data ownership under Option A:** DCMS extends Solon entities almost entirely through Data Area JSONB. DWP-specific fields (vulnerability flags, breathing space hold type, IE assessment reference, champion/challenger variant, DWP debt type, deduction-from-benefit flag) land in the `data_area` column of Solon's Taxpayer, Case, Suppression, and Task entities. No parallel schema tables are introduced except for the champion/challenger assignment log (a new table, needed because Solon has no analogue).

**Contract between layers:** In-process Spring beans. No HTTP boundary. The DCMS custom layer is a Maven module (`revenue-management-be-custom`) that runs inside the same JVM as Solon. Solon's Kafka command catalogue is used for all async operations (CreateSuppressionCommand, PostPaymentCommand, SendCorrespondenceCommand, etc.).

### Consequences for delivery

- Fastest to initial working system. Solon's reference processes run from day one; DWP customisation is additive.
- Highest Solon-internals learning curve. Modifying Solon reference BPMN processes requires understanding Solon's process model, Drools posting rules, and Amplio's variable scoping — before any DWP logic is written.
- Solon upgrades carry high blast radius: modified reference BPMN processes must be re-validated against each Solon version. The DCMS team becomes a downstream consumer of Solon's release cadence.
- Champion/challenger, IE engine, and vulnerability governance are structurally awkward: they are significant domain services that have no Solon scaffold, yet they live inside the Solon custom extension module, constrained by Solon's entity model and lifecycle.
- Non-technical DMN authoring (ADR-008 requirement, UNDER REVIEW) cannot be met. Drools DRL requires developer involvement for rule changes. This must be re-confirmed as acceptable or resolved before Option A can be locked.
- DWP UX is constrained by what Solon's REST APIs expose. If DWP agents need task views, account timelines, or investigation screens that Solon's Angular UI serves, the React UI must reconstruct these from Solon's API surface. API gaps require custom endpoints added to the custom layer.

### Risks unique to Option A

| Risk | Severity | Mitigation |
|---|---|---|
| Solon BPMN modification scope underestimated — tax authority processes do not map cleanly to benefit debt without substantial change | High | Run a BPMN fit-gap spike against each Solon reference process before committing; classify each as use-as-is, light-modify, or replace |
| Drools rule authoring lock-in — DWP policy team cannot make rule changes without a developer | High | Accept as a constraint for v1, plan a Drools authoring UI for v2; or replace with a DMN engine (Option B solves this differently) |
| Solon Data Area JSONB fields are schema-less — querying them for reporting requires jsonb operators; complex queries degrade | Medium | Materialise DWP-specific fields into a read-model (reporting DB or event-sourced projection) from day one |
| Option A carries the Java 17 / Liquibase constraints with no practical escape path — custom modules must use Java 17 in-process | High | Confirm Java version decision before committing to Option A |
| Champion/challenger and IE engine are significant domain services; embedding them in the Solon custom module constrains their evolution | Medium | Define clear internal hexagonal boundaries within the custom module from day one |

---

## Option B: Medium Custom Layer — DWP Domain Services Orchestrating Solon Primitives

### Layer thickness

Medium. Solon contributes its financial engine (ledger, payment, write-off), batch engine, Kafka command bus, Amplio process engine, Keycloak/OPA auth, and ELK observability stack. DCMS contributes a set of purpose-built domain services: DWP-specific BPMN process definitions deployed into the same Amplio engine, a `BreathingSpaceGatingService` with a dedicated schema table, DWP-specific domain services (champion/challenger, IE engine, vulnerability governance, communication suppression, DCA placement, work allocation), and the React UI. Solon's reference processes are used as reference material for BPMN design but are not modified; instead, DWP processes are new definitions that call Solon entities via the Kafka command catalogue.

### Boundary placement

| Responsibility | Lives in Solon | Lives in DCMS custom layer |
|---|---|---|
| Financial ledger (balances, FTs, write-off) | Solon core | Calls via Kafka (PostPaymentCommand, WriteOffDebtCommand, etc.) |
| Payment processing, allocation | Solon core | Calls via Kafka; DWP-specific CCA allocation order enforced in DCMS service before posting |
| Repayment plan lifecycle | Solon PAYMENT_PLAN_PROCESS.bpmn | DWP-specific arrangement rules (re-aging, breach tolerance) in DCMS BPMN + service |
| Batch engine | Solon Foundation Batch | DWP-specific batch jobs (DM6, bureau feeds, bulk communication dispatch) as custom batch definitions |
| Kafka command bus | Solon infrastructure | DWP-specific topics (dcms.breathingspace.commands, dcms.strategy.events, etc.) |
| Task tray, work allocation primitives | Solon TASK_ASSIGNMENT_PROCESS.bpmn | DWP-specific queue rules, worklist views, supervisor override in DCMS services |
| Debt recovery BPMN | Solon reference (read for patterns) | New DWP_DEBT_RECOVERY_PROCESS.bpmn deployed in Amplio |
| DCA handover BPMN | Solon CASE_HANDOVER_COLLECTION_AGENCY_PROCESS.bpmn (called as sub-process) | DWP pre-placement disclosure gate, DCA selection logic |
| Breathing Space gating service | Solon suppression API (source of truth for hold status) | BreathingSpaceGatingService — owns gating logic, dedicated schema table, called in every DCMS BPMN service task before debtor-facing effect |
| Champion/challenger | Not in Solon | ChampionChallengerService — owns variant assignment, promotion, harm-indicator tracking |
| Income and expenditure | Not in Solon | IEAssessmentService — CFS standard model, affordability calculation |
| Vulnerability governance | Not in Solon | VulnerabilityClassificationService — FCA FG21/1 tiers, suppression overlap |
| Communication suppression | Solon CORRESPONDENCE_PROCESS.bpmn for transport | CommunicationSuppressionService — owns category evaluation, calls Solon SendCorrespondenceCommand when permitted |
| Strategy / segmentation engine | Solon Drools (existing rules as a starting point) | SegmentationService — DWP-specific rules; Drools custom KIE module in DCMS |
| UI | — | New React app; calls DCMS API layer (BFF), which calls Solon and DCMS services |
| Auth | Solon Keycloak + OPA | DWP realm, OPA Rego policies for DCMS-specific access rules |

**Breathing Space gating service location under Option B:** The `BreathingSpaceGatingService` is a DCMS-owned Spring bean with its own schema table (`breathing_space_gate_log`). It does not modify Solon's suppression model; instead it reads from it (via Solon's `active_suppressions` query) and adds DWP-specific evaluation logic (moratorium type, MHCM vs standard, deferred output flagging, RULING-016 guardrail compliance). Every DCMS BPMN service task that could produce a prohibited debtor-facing effect calls this service before executing. The service returns a `GatingDecision` (PERMIT / SUPPRESS / DEFER_PENDING_MORATORIUM_END). The BPMN service task acts on this decision — suppressing or deferring the effect rather than cancelling the process token. This cleanly satisfies RULING-016: Amplio advances internal state; the gate stops prohibited effects. Solon's `SuppressionExpiryJob` handles moratorium expiry; DCMS's gating service re-reads the suppression status on each invocation, so moratorium end is automatically reflected without a DCMS-side timer.

**MHCM handling:** Per RULING-016 section 5, the Mental Health Crisis Moratorium has no fixed end date. The gating service must not apply a timer-based end to MHCM cases. A DCMS `MHCM_RELEASE_PROCESS.bpmn` (manual professional sign-off trigger) issues a `ReleaseSuppressionCommand` to Solon, which then removes the active suppression. The gating service re-reads and PERMITS on the next invocation.

**Data ownership under Option B:**

| Entity | Owned by | Store |
|---|---|---|
| Debt account ledger | Solon | Solon PostgreSQL (bill segments, FTs) |
| Payment records | Solon | Solon PostgreSQL |
| Repayment plan | Solon (lifecycle) + DCMS (DWP rules) | Solon entity + DCMS Data Area JSONB on PaymentPlan |
| Breathing space hold | Solon (suppression table) + DCMS (gate log) | Solon suppression + `breathing_space_gate_log` (DCMS schema) |
| Champion/challenger assignments | DCMS | `champion_challenger_assignment` (DCMS schema) |
| IE assessment | DCMS | `ie_assessment` (DCMS schema) |
| Vulnerability flags | DCMS (source of truth) | `vulnerability_record` (DCMS schema); mirrored into Solon Data Area for BPMN variable access |
| Communication events | DCMS | `communication_event` (DCMS schema) |
| Audit trail | DCMS | `audit_event` (DCMS schema, INSERT-only) |
| Customer identity | DCMS (master) + Solon (Taxpayer entity as the process entity) | DCMS `customer` table + Solon Taxpayer entity linked by external reference |

DWP-specific fields that Solon BPMN processes need to evaluate (vulnerability flag, moratorium status, DWP debt type) are materialised into Solon Data Area JSONB so that Amplio FEEL expressions and Drools rules can access them without crossing a service boundary. DCMS maintains these fields as write-through: when DCMS updates a vulnerability record, it also issues an update to the Solon Taxpayer Data Area.

**Contract between DCMS layer and Solon:**

- Async command/event: Solon's 24-command Kafka catalogue. DCMS publishes commands; Solon raises events; DCMS listens.
- Sync HTTP: Solon's documented REST APIs for queries (account balance, active suppressions, task state).
- In-process Spring beans: DCMS custom beans registered in Solon's application context for service tasks called from DCMS BPMN definitions. This is Solon's own customisation model (`revenue-management-be-custom` module).
- No direct cross-schema SQL. DCMS domain tables are co-located in the same PostgreSQL instance but in a dedicated `dcms` schema, separated by schema ownership.

### Consequences for delivery

- More development work than Option A, but domain services are cleanly bounded and owned. Champion/challenger, IE engine, and vulnerability governance are not squeezed into Solon's extension seams — they have their own services with their own schemas.
- Solon upgrade blast radius is contained to the Kafka command API and the Solon REST API surface. DCMS BPMN processes are new definitions; they are not forks of Solon reference processes and do not need re-validation against Solon reference process changes.
- The DCMS team learns Amplio's operational model (how to deploy BPMN, how to use the Admin UI, how to handle incidents) but does not need to understand every Solon reference process. Lower Solon-internal learning curve than Option A.
- Non-technical DMN authoring remains constrained to Drools unless a separate DMN sidecar is introduced (a decision lever — see below). Under Option B, DWP Drools rules live in the DCMS custom KIE module, separate from Solon's own rules, which reduces the risk of Solon rules bleeding into DWP rule logic.
- React UI calls a DCMS BFF (Backend for Frontend) layer that aggregates Solon REST responses and DCMS service responses. This insulates the UI from Solon API shape changes.
- Java version is a live lever. If the programme resolves Java 17 as the runtime, DCMS custom modules compile to Java 17. If Java 21 is required, it forces Option C (separate JVM process), because Option B's in-process model requires a single JVM.

### Risks unique to Option B

| Risk | Severity | Mitigation |
|---|---|---|
| DCMS schema (`dcms`) co-located with Solon schema on the same PostgreSQL instance — Solon upgrades might run Liquibase migrations that affect shared infrastructure | Medium | Use separate PostgreSQL schemas with distinct Liquibase contexts. Confirm Solon's migration scope is schema-bounded. If not, escalate to separate PostgreSQL instance (increases infra cost). |
| Solon Kafka command API is the integration contract — undocumented or unofficial commands might be used and later removed | Low (24-command catalogue is documented) | Pin to documented commands only; treat any usage of undocumented commands as a red flag requiring Solon-team coordination |
| Data Area JSONB write-through (DCMS → Solon Taxpayer entity) creates a dual-write path; failure in the write-through leaves Solon Data Area stale | Medium | Make the write-through idempotent and retry-safe via the Outbox Pattern; Amplio FEEL expressions that read Data Area must tolerate a short staleness window |
| BreathingSpaceGatingService must be called at every debtor-facing service task — developer discipline gap could leave a task ungated | High | Enforce via a custom Amplio service task base class that makes the gate call mandatory; fail-fast if the gate is not registered for a task declared as debtor-facing |
| Champion/challenger harm indicator tracking requires analytics schema from day one (RULING-010 guardrail 3) | Medium | Build `champion_challenger_assignment` table with harm indicator columns in the first delivery sprint; do not defer |

---

## Option C: Heavy Custom Layer — DCMS Owns Domain Logic, Solon as Substrate

### Layer thickness

Heavy. DCMS owns most domain logic end-to-end: debt case lifecycle BPMN, strategy engine, communication suppression, work allocation, IE engine, vulnerability governance, DCA placement, champion/challenger. Solon contributes as a lower-level substrate: ledger primitives (posting FTs, write-off), payment processing engine, batch runtime, Kafka bus, Keycloak/OPA, and ELK. DCMS does not call Solon's reference BPMN processes; it calls Solon's financial APIs directly as services.

This is closest to the pre-pivot greenfield direction (ADR-016 Option C), with Solon retained as the financial and infrastructure substrate rather than a domain partner.

### Boundary placement

| Responsibility | Lives in Solon | Lives in DCMS |
|---|---|---|
| Financial posting (FT, balance) | Solon — called via Kafka / REST as a financial service | DCMS issues commands; does not own posting logic |
| Payment receipt and matching | Solon | DCMS owns CCA allocation order; issues AllocatePaymentCommand |
| Write-off | Solon | DCMS owns authority tiers and approval flow |
| Batch runtime | Solon Foundation Batch | All batch logic is DCMS-defined jobs |
| Kafka bus | Solon infrastructure | DWP topics added |
| Auth | Solon Keycloak + OPA | DCMS-specific Rego policies |
| BPMN processes | Not used (Amplio used as runtime, but no Solon reference processes adopted) | All BPMN process definitions are DCMS-authored, deployed into Amplio |
| Breathing Space gating service | Not in Solon | BreathingSpaceGatingService in DCMS, with its own `breathing_space_hold` table |
| Communication suppression | Not in Solon | CommunicationSuppressionService in DCMS |
| Customer entity | Not in Solon | DCMS `customer` table (Solon Taxpayer entity used only as a process anchor via external reference) |
| Account entity | Delegates FT posting to Solon | DCMS `account` table owns balance semantics; Solon is a transaction log |
| All other domain services | Not in Solon | DCMS |

**Breathing Space gating service location under Option C:** Fully DCMS-owned. The gating service does not call Solon's suppression model at all — it reads from its own `breathing_space_hold` table. Moratorium state is owned by DCMS. Solon is not involved in moratorium management. This is the cleanest ownership model for the gating service but eliminates reuse of Solon's `SuppressionExpiryJob` and `CreateSuppression / ReleaseSuppressionCommand` infrastructure.

**Data ownership under Option C:** DCMS owns the schema for all domain entities. Solon's entity model is not extended; it is bypassed. The only Solon tables used are the financial transaction log and payment tables — accessed via Solon's REST/Kafka API, not by direct SQL. A dedicated `dcms` PostgreSQL database (separate from Solon) is viable under this option, which resolves the Liquibase/Flyway and Java 17/21 conflicts: DCMS can run its own migration tooling against its own database.

**Contract between DCMS and Solon:** HTTP REST calls to Solon financial APIs (PostPayment, WriteOff, PaymentPlan) treated as an external dependency — the same pattern as calling WorldPay or DWP Place. An anti-corruption layer (ACL) wraps these calls.

### Consequences for delivery

- Most development work. DCMS is effectively a full domain platform that happens to use Solon's financial engine and infrastructure, not a system that extends Solon.
- Highest team autonomy. No Solon-internal learning required beyond the financial API surface. Java version, migration tooling, and architecture patterns are entirely under DCMS control.
- Java 21 is viable (DCMS runs in its own JVM; Solon is a called service). Flyway is viable (DCMS has its own database). ADR-014's exhaustive switch safety mechanism can be restored.
- Solon upgrade blast radius is minimal: only the Solon financial API surface needs re-validation.
- Removes most of the platform value. Solon is chosen as the base platform because it contributes debt recovery workflow, task management, batch engine, and Amplio BPMN runtime. Under Option C, most of that contribution is bypassed. The platform provides financial posting and infrastructure; the DCMS team rebuilds domain logic that Solon's reference processes already encode.
- Champion/challenger, IE engine, vulnerability governance, communication suppression — all built fully from scratch, as in the pre-pivot greenfield. If these components were well-understood and deliverable on their own, the programme would not have pivoted to Solon.

### Risks unique to Option C

| Risk | Severity | Mitigation |
|---|---|---|
| Rebuilds what Solon provides — if the programme pivoted to avoid this rebuild, Option C undoes the pivot's value | High | Only viable if a specific, bounded reason forces it (Java 21 mandate, Flyway mandate) |
| Anti-corruption layer to Solon financial API adds integration surface that must be maintained across Solon upgrades | Medium | Treat Solon as an external service from day one; consumer-driven contract testing on the ACL |
| DCMS must implement full BPMN process library from scratch — DWP_DEBT_RECOVERY, DWP_ARRANGEMENT, DWP_DCA_HANDOVER, etc. | High | Reference Solon's 28 process definitions as design input; do not start from a blank page |
| Duplicate financial state: DCMS account table plus Solon FT log — divergence over time is a data integrity risk | High | Source of truth must be declared and enforced from day one; if DCMS account table is the authority, Solon FT log is a shadow; if vice versa, DCMS must never compute its own balance |
| If Java 21 is later deprioritised, Option C loses its primary justification | Low | Confirm Java version decision before selecting Option C |

---

## Tradeoff Summary

| Dimension | Option A (Minimal) | Option B (Medium) | Option C (Heavy) |
|---|---|---|---|
| Time to first working system | Fastest | Medium | Slowest |
| DWP domain control | Low — Solon dictates structure | High — DCMS owns DWP logic | Highest |
| Solon internals learning curve | High | Medium | Low |
| Solon upgrade blast radius | High | Medium | Low |
| Java 17 / Liquibase constraint | Hard constraint — cannot escape | Hard constraint unless separated JVM | Escapable — own JVM, own DB |
| Breathing Space gating placement | In-process custom bean, calls Solon suppression | In-process custom bean with own schema, reads Solon suppression | Fully DCMS-owned, no Solon suppression use |
| Platform value retained | Most | Substantial | Minimal |
| Champion/challenger / IE / vulnerability governance | Awkward — squeezed into custom module | Clean — own services with own schemas | Full rebuild |
| Non-technical DMN authoring | Not met (Drools only) | Not met unless a DMN sidecar is added | Not met unless a DMN sidecar is added |
| Complexity | Low (layer), High (Solon knowledge) | Medium | High (rebuild scope) |
| Risk profile | Solon coupling, Drools governance | Integration contract, dual-write | Rebuild scope, financial state divergence |

---

## Decision Levers — What Would Flip the Recommendation

### Lever 1: Java version is locked to Java 21

**Current state:** Under review (memory.md, known constraints).

**If confirmed as Java 21:** Option B's in-process model requires the DCMS custom module to compile and run in the same JVM as Solon (Java 17). Mixing Java 21 bytecode into a Java 17 JVM is not possible. The only escape is a separate JVM process (a DCMS service tier that calls Solon via HTTP/Kafka), which functionally becomes Option C. **Java 21 mandate flips the recommendation from B to C.**

ADR-014's exhaustive switch safety for `CommunicationSuppressionService` is a meaningful statutory safety argument. The SA must decide whether to accept a Drools-based equivalent guard (achievable in Java 17) or treat Java 21 as mandatory.

### Lever 2: Non-technical DMN authoring is a hard requirement

**Current state:** ADR-008 specified DMN authoring for `PROCESS_DESIGNER` and `COMPLIANCE` roles — UNDER REVIEW.

**If confirmed as hard:** Drools DRL is not satisfiable for non-technical users under either Option A, B, or C without a DMN sidecar. Adding a DMN engine alongside Drools is possible under Option B (the DMN sidecar lives in the DCMS custom layer; Drools remains for Solon's internal rules). This is an incremental addition to Option B, not a reason to switch options. However, if the programme concludes that Drools is simply unacceptable for DWP governance and a full DMN engine must replace it — including for Solon's internal rules — then Option C's separation makes the swap cleaner. **A hard DMN mandate incrementally complicates Option B but does not flip the recommendation unless Drools must be eliminated from Solon's core.**

### Lever 3: DWP scope confirmation on champion/challenger and IE engine

**Current state:** Champion/challenger and IE assessment are in the domain packs and domain rulings; RULING-010 confirms champion/challenger is in scope.

**If DWP confirms both are descoped from the initial delivery:** Option A becomes materially more attractive. The components that are "awkward" in Option A's thin layer are precisely these two and vulnerability governance. With them removed, Option A's extension model handles the remaining DWP-specific logic (Breathing Space gating, deduction-from-benefit, DCA pre-placement disclosure) without structural strain. **Champion/challenger + IE descope flips the recommendation from B to A.**

---

## Forward-Looking Note: Solon Tax on Java 25

**Programme signal (unconfirmed at time of writing):** A Solon Tax release expected to ship in early May 2026 is reported by programme stakeholders to run on Java 25 LTS. The official 2.3.0 operations guide checked into this repository (`external_sys_docs/solon/solon_tax_2.3.0_operations_guide.md` line 817) currently specifies JDK 17. Until the GA release notes confirm the runtime version, the options pass above is locked against the documented Java 17 baseline. This section captures the forward-looking impact so that nothing in the recommendation is invalidated if the Java 25 signal is confirmed.

### What changes if Solon ships on Java 25

**Lever 1 (Java version) is materially weakened, possibly dissolved.**

The current Lever 1 reasoning — "Java 21 mandate flips B to C, because mixing Java 21 bytecode into a Java 17 JVM is not possible" — assumes a Java 17 host JVM. If Solon's host JVM is Java 25:

- DCMS custom modules can compile to Java 17, 21, or 25 and run in Solon's JVM (Java 25 is fully backward-compatible at the bytecode level for Java 17 and 21 class files). The in-process model no longer constrains DCMS to Java 17.
- ADR-014's exhaustive switch safety mechanism (the canonical reason Java 21 was previously argued for) is satisfiable inside Option B's in-process model.
- Java 21 ceases to be a forcing function toward Option C. The lever "Java 21 mandate flips B to C" is dissolved.

**Recommendation hardening:** Under a Java 25 Solon, Option B becomes materially stronger. The single largest residual reason to consider Option C — programme insistence on a modern Java for DCMS code — disappears. Option C is then justifiable only on grounds of platform-value rebuild scope (which under this design pass is not a sufficient reason).

**Recommendation softening for Option A:** Option A's risk row "Option A carries the Java 17 / Liquibase constraints with no practical escape path" weakens to "Option A inherits Solon's runtime, which under Java 25 is no longer a constraint." This reduces one of the High-severity risks against Option A but does not change the structural awkwardness of squeezing champion/challenger and IE engine into the extension seams.

### What does *not* change

- **Liquibase constraint** is independent of Java version. Solon uses Liquibase for migrations; Option B's in-process model still requires DCMS to use Liquibase (or a separately-scoped migration tool against a separate schema) regardless of JVM version. Java 25 does not unlock Flyway in Option B.
- **Drools-vs-DMN constraint** (Lever 2) is independent of Java version.
- **Champion/challenger and IE engine scope** (Lever 3) is independent of Java version.
- **Solon's 24-command Kafka catalogue, BPMN runtime, and `revenue-management-be-custom` extension model** are platform-architectural and orthogonal to JVM version.
- **Option C's relative position** is largely unchanged: it remains the most expensive in rebuild scope and the lowest in retained platform value. What changes is only that one of its justifications (Java 21 in DCMS code) evaporates.

### Action if Java 25 is confirmed at GA

1. Update CLAUDE.md "Confirmed" / "Under Review" tables to reflect Java 25 as the Solon runtime.
2. Strike Lever 1 from this document, or rewrite it as a historical note.
3. Update the Tradeoff Summary row "Java 17 / Liquibase constraint" to "Liquibase constraint" only.
4. Reduce Option A risk row 4 from High to Low.
5. No change to the recommendation (Option B); the recommendation strengthens.

### Action if Java 25 is *not* confirmed at GA

No change. The options pass above stands as written.

---

## Interface and Dependency Impact

### Upstream (Solon platform team)

- Option A and B require agreement on the DCMS custom Maven module (`revenue-management-be-custom`) structure, Drools KIE packaging, and BPMN deployment process into the shared Amplio engine.
- All three options use Solon's Kafka command catalogue — no new Solon commands are proposed; only documented commands are used.
- Option B's write-through to Solon Data Area (Taxpayer entity) requires confirmation that the Data Area write path is safe for concurrent external updates. If not, the vulnerability flag mirroring must use an event (a custom Kafka topic) rather than a direct Data Area update.
- Option C requires Solon to expose stable financial REST APIs as an external-facing contract. If Solon's REST APIs are internal and not covered by a stability guarantee, this is a risk that the SA must call out to the Solon platform team before locking Option C.

### Downstream (DWP integrations)

- All three options connect to the same DWP integration points: DWP SSO (INT01), WorldPay, DWP IVR, DWP Notifications, DWP Data Integration Platform, FTPS, DWP Payment Allocation. These sit in the DCMS integration domain regardless of option. No downstream impact from option choice.
- Deduction-from-benefit suspension (DDE-OQ-BS-PROCESS-02): when resolved, determines whether moratorium-start BPMN includes a Kafka command to the benefit system or a manual task. Both are possible under Option B or C. Under Option A, the BPMN modification to the relevant Solon process must accommodate this call.

### React UI

- Option A: React calls Solon REST APIs with thin DCMS custom endpoints. UI shape is constrained by Solon API responses.
- Option B: React calls a DCMS BFF that aggregates Solon and DCMS service responses. UI is decoupled from Solon API shapes.
- Option C: React calls DCMS APIs exclusively. Most complete UI control.

---

## Key Risks and Mitigation Ideas

### Option A

1. **Breathing Space gating not enforced at every service task** — mitigation: a mandatory base class for all service task delegates that calls the gating service. Fail-fast if not registered.
2. **Solon BPMN modification scope larger than expected** — mitigation: BPMN fit-gap spike before committing (one-week timebox per reference process).
3. **Drools rule governance is developer-gated** — mitigation: accept for v1 with a planned authoring UI in v2; document as a known constraint with DWP sign-off.

### Option B (recommended)

1. **Dual-write to Solon Data Area (vulnerability flag mirroring) fails silently** — mitigation: Outbox Pattern for the write-through; idempotent update; alerting on update failures.
2. **BreathingSpaceGatingService not called at all debtor-facing tasks** — mitigation: mandatory base class; integration test that asserts every DCMS BPMN process has a registered gate for each task marked `debtor_facing = true`.
3. **DCMS schema co-located with Solon on same PostgreSQL** — mitigation: dedicated `dcms` schema; Liquibase context boundaries; confirm Solon's migration scope. Escalate to separate PostgreSQL instance if schema isolation is insufficient.
4. **Champion/challenger harm indicator tracking omitted early** — mitigation: `champion_challenger_assignment` table with harm indicator columns in sprint 1; RULING-010 guardrail 3.

### Option C

1. **Financial state divergence between DCMS account table and Solon FT log** — mitigation: one authoritative source declared at design time; the other is read-only or eliminated.
2. **Platform value loss** — mitigation: audit which Solon capabilities are genuinely bypassed before committing; if more than 60% of what Solon provides is bypassed, escalate to SA and client as a pivot-value question.

---

## Open Questions Requiring DWP Sign-Off

| Question | Owner | Blocks |
|---|---|---|
| DDE-OQ-BS-PROCESS-01: Scope of DWP communications policy during moratorium (statutory floor vs. DWP policy overlay) | Delivery Lead → DWP client policy team | `CommunicationSuppressionService` category model finalisation |
| DDE-OQ-BS-PROCESS-02: Deduction-from-benefit suspension mechanism — DCMS automated call, DWP manual, or DWP Place? | Delivery Lead → DWP integration team | Moratorium-start BPMN integration design |
| Java version: Java 17 (Solon native) vs Java 21 (pre-pivot ADR-014 mandate) | Solution Architect → DWP client | Option selection (flips B → C if Java 21 required) |
| Non-technical DMN authoring: is Drools DRL acceptable for DWP policy team? | Solution Architect → DWP client | Configurability tier design |
| Champion/challenger and IE engine: are these in scope for initial delivery? | Solution Architect → DWP client | Option selection (flips B → A if both descoped) |
| DDE-OQ-12 / DDE-OQ-13: Champion/challenger thresholds and vulnerable-customer policy | Delivery Lead → DWP client | ChampionChallengerService configuration detail |

---

## Handoff Declaration

- **Completed:** Options pass on DCMS layer thickness and boundary placement, with Amplio mandated and RULING-016's gate-at-effect model as the corrected Breathing Space design framing. Two open DWP sign-off questions (DDE-OQ-BS-PROCESS-01, DDE-OQ-BS-PROCESS-02) noted and carried forward.
- **Files changed:** `docs/project-foundation/design-options/DESIGN-OPTIONS-002-layer-thickness-and-boundary.md` (created)
- **ACs satisfied:** Options produced (3 options, parallel structure), recommendation stated, decision levers named, Breathing Space gating service addressed per option, data ownership addressed per option, interface and dependency impact addressed, key risks per option named, open questions with owners listed.
- **ACs not satisfied:** None — this is a design artefact, not an implementation AC.
- **Assumptions made:**
  - Solon's 24-command Kafka catalogue is stable and the documented commands are the full supported set.
  - Solon's `revenue-management-be-custom` module pattern (Spring bean component scan) is available and supported in Solon v2.3.0 as described in the platform reference.
  - No new Solon BPMN reference processes outside the documented 28 are relevant to DCMS.
  - RULING-016 is final as filed; only DDE-OQ-BS-PROCESS-01 and DDE-OQ-BS-PROCESS-02 remain open within its scope.
  - Team size and delivery deadline are not constraints per ADR-018.
- **Missing inputs encountered:** Java version decision (critical — flips option if Java 21 required); DMN authoring hard requirement status; champion/challenger and IE engine in/out-of-scope for initial delivery.
- **Next role:** Design Critic, then Solution Architect.
- **What they need:** This document. RULING-016 (breathing space gating). `solon-tax-platform-reference.md` (Solon capabilities and customisation model). `amplio-process-engine-reference.md` (Amplio constraints). The three decision levers (Java version, DMN authoring, champion/challenger scope) should be resolved before the SA locks the option — they are the live levers.

---

`[BLOCKING] Design Critic Review Required` — Solution Architect must not lock an option without a Design Critic pass on this document.
