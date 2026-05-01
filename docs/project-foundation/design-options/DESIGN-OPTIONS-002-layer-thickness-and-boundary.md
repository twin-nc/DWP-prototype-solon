# DESIGN-OPTIONS-002: Layer Thickness and Boundary Placement

## What this document is for

This is a **Delivery Designer options pass** that frames the central post-pivot architecture question for DCMS: *how thick should the DCMS custom layer be on top of Solon Tax, and where should the boundary between DCMS code and Solon platform sit?* It puts three end-to-end options on the table (A: minimal extension, B: medium custom layer, C: heavy custom layer with Solon as financial primitives only), names the decision levers that would flip the choice, and surfaces the blockers and compliance issues that must be resolved before a Solution Architect locks the answer.

It is **not** an architecture decision (that is the SA's job, recorded as a successor ADR to ADR-018), and it is **not** an implementation plan. Downstream artefacts — domain-pack design, integration contracts, BPMN authoring, build plans, trace map updates — are gated on the option lock that follows this document.

**Audience:** Solution Architect (primary), Design Critic, platform expert reviewers, Delivery Lead, DWP client-side reviewers for the open questions section.

## Summary

- **Recommendation:** **Option B — Medium Custom Layer.** Reuse Solon's Amplio process engine, ledger, batch engine, Kafka bus, suppression model, and task tray. A dedicated `BreathingSpaceGatingService` lives in the DCMS custom layer and guards every Amplio step that produces a debtor-facing effect (RULING-016 gate-at-effect). DWP-specific BPMN processes deploy into the same Amplio engine alongside Solon's reference processes.
- **For Release 1 specifically:** **Option B-Demo** — Option B's architecture delivered at demo-depth implementation matched to Release 1's prototype/demo scope. Option A is no longer recommended for Release 1 (R2 review showed Release 1 capabilities cannot be cleanly delivered through Option A's extension model). Option C remains the fallback if integration-contract surface cannot be stabilised or if a DCMS-side Flyway-on-shared-schema constraint is imposed.
- **Status:** PROPOSED. **Integration design lock is BLOCKED** pending resolution of four blockers and one compliance contradiction (see Action Items).
- **Decision levers still live:** (1) Java version (Java 17 vs Java 21 — potentially dissolved if Solon ships on Java 25, see Forward-Looking Note); (2) Strategy-authoring application scope (broadened from DMN sidecar per R3); ~~(3) Champion/challenger and IE engine scope — CLOSED 2026-05-01, both confirmed in scope per `release-1-capabilities.md`~~.
- **Mandatory conditions on Option B (from Solon platform expert review):** (i) default DCMS→Solon commands to REST until Kafka topic externalisation is confirmed; (ii) `suspendActiveInstancesSW = false` for all DCMS suppression types; (iii) Blocker 3 widened from MHCM-only to also cover Vulnerability suppressions.

## Action Items

Owned by **Solution Architect** unless otherwise noted. The first four items gate integration design lock.

| # | Action | Owner | Gates |
|---|---|---|---|
| 1 | **Resolve Blocker 1** — verify whether the platform-reference 24-command catalogue (`PostPaymentCommand`, `WriteOffDebtCommand`, …) maps to externally-publishable Kafka topics in integration guide §5, including payload schemas, response-event existence, and consumer-group isolation. Not a name-mapping task. | SA → Solon platform team | Inter-layer contract design (Options A and B) |
| 2 | **Resolve Blocker 2** — obtain Solon REST API stability guarantees for the sync queries the BFF depends on (account balance, active suppressions, task state) under Solon upgrades. | SA → Solon platform team | Option B BFF; Option C ACL |
| 3 | **Resolve Blocker 3 (widened)** — verify by test how `SuppressionExpiryJob` interprets `maximumNumberDays: 0` for both MHCM and Vulnerability `SuppressionType`s. Wrong interpretation auto-expires MHCM without professional sign-off (Reg 21 breach / criminal-liability exposure). | SA → Solon platform team (verify by test) | MHCM and Vulnerability detailed design under Options A and B |
| 4 | **Resolve `suspendActiveInstancesSW` compliance contradiction** — declare the setting for every DCMS `SuppressionType` and reconcile with RULING-016 gate-at-effect. Recommended: `false` for all DCMS suppression types. | SA | Option B compliance design |
| 5 | Confirm Option B-Demo as the Release 1 delivery framing, or call out specific service-depth concerns (most likely strategy authoring). | SA | Release 1 build plan |
| 6 | Resolve the three live decision levers (Java version; strategy-authoring application scope; CLOSED champion/challenger/IE) and update this document accordingly. | SA → DWP client | Final option lock |
| 7 | Produce the closed enumeration of gated effect categories from RULING-005, RULING-010, RULING-011, RULING-014, RULING-016 (Design Critic S1). | SA → Domain Expert | DCMS BPMN authoring under Options A and B |
| 8 | Decide vulnerability-flag access path: Data Area mirroring (with staleness window) vs direct in-process service call (Design Critic S2; RULING-010 immediacy). | SA | Option B vulnerability/champion-challenger interaction |
| 9 | Define on-lift disposition flow for queued correspondence under split Solon-transport / DCMS-classification ownership (Design Critic S4). | SA | Option B `CommunicationSuppressionService` design |
| 10 | Define OPA / Rego policy deployment model and Solon-upgrade survival for DCMS-specific authz rules (Design Critic S5). | SA → Solon platform team | All three options' authz design |
| 11 | Declare the layer that gates `createArrangement` against active suppressions (Design Critic A5; RULING-014 guardrail 2). | SA | Options A and B compliance design |
| 12 | Decide the Maven-module deployment boundary between DCMS beans and Solon `revenue-management-be-custom` beans (Design Critic A2) — same module or separate module in shared Spring context. | SA | Option A vs Option B differentiation |
| 13 | Resolve open DWP policy questions: DDE-OQ-BS-PROCESS-01 (moratorium comms scope), DDE-OQ-BS-PROCESS-02 (deduction-from-benefit suspension ownership), DDE-OQ-12 / DDE-OQ-13 (champion/challenger thresholds, vulnerable-customer policy). | Delivery Lead → DWP client | Detail design of comms/moratorium/champion-challenger services |
| 14 | If Solon GA confirms Java 25, apply the changes listed in "Action if Java 25 is confirmed at GA" (CLAUDE.md tables; strike Lever 1; reduce Option A risk row 4). | SA | Document hygiene only — recommendation strengthens, no re-lock needed |

**Next role after action items 1–6 are resolved:** Solution Architect locks the option in a successor ADR to ADR-018; downstream artefacts (domain-pack designs, integration contracts, BPMN authoring, build plans, trace map updates) then proceed.

---

**Document ID:** DESIGN-OPTIONS-002
**Date:** 2026-04-30 (revised 2026-05-01)
**Status:** PROPOSED — Design Critic review complete (2026-04-30); Release 1 capability review complete (2026-05-01); awaiting Solution Architect lock. **Integration design lock BLOCKED** pending resolution of three platform-reality questions, the compliance contradiction surfaced by the Design Critic, and the structural reframing required by the Release 1 capability review (see "Platform-Reality Review Findings", "Design Critic Review Findings", and "Release 1 Capability Review Findings" below).
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

**Updated 2026-05-01 (Release 1 capability review):** The architectural recommendation remains Option B. For Release 1 specifically, the recommended delivery framing is **Option B-Demo** — Option B's architecture delivered at demo-depth implementation matched to Release 1's prototype/demo scope (see new "Option B-Demo" section). Option A is no longer recommended for Release 1 production architecture: the Release 1 capability review (R2) shows that several Release 1 capabilities cannot be delivered through Option A's extension model without quietly converging on Option B's pattern; Option A is retained for contrast and traceability. Lever 3 (champion/challenger + IE descope) is CLOSED per R1 — both are confirmed in scope for Release 1. Option C remains the fallback if programme direction changes to require Flyway in the DCMS custom tier on a shared database schema with Solon, or if the integration contract surface (Blockers 1, 2, 3) cannot be stabilised. The Java-version flip-to-C is dissolved if Solon ships on Java 25 (see Forward-Looking Note).

**Updated 2026-05-01 (Solon platform expert review):** The Option B / B-Demo recommendation is confirmed as most defensible but carries three mandatory conditions that must be met before SA lock: (1) Kafka contract verification — default all DCMS→Solon commands to REST until the Solon platform team confirms that the abstracted command names (`PostPaymentCommand`, `WriteOffDebtCommand`, etc.) are externally-publishable Kafka topics; (2) `suspendActiveInstancesSW` must be set to `false` for all DCMS suppression types (standard Breathing Space, MHCM, Vulnerability, internal policy holds) — placing 100% of effect-suppression responsibility on `BreathingSpaceGatingService` and in alignment with RULING-016's gate-at-effect model; (3) Blocker 3 is widened from MHCM-only to cover Vulnerability suppressions as well — both use `maximumNumberDays: 0` in documented examples and both are exposed to `SuppressionExpiryJob` ambiguity.

---

## Platform-Reality Review Findings (added 2026-04-30, post solon-tax-platform-expert review)

A platform-expert review against `external_sys_docs/solon/solon_tax_2.3.0_integration_guide.md`, `solon_tax_2.3.0_operations_guide.md`, `api-suppression-management.md`, and the Amplio reference identified factual corrections, missing constraints, and two blockers that must be resolved before integration design is locked. The recommendation (Option B) is unchanged but is conditional on Blocker 1.

### Blockers (must resolve before integration design lock)

| # | Blocker | Owner | Blocks |
|---|---|---|---|
| 1 | **Kafka command-name discrepancy.** The "24-command catalogue" cited in the platform reference uses abstracted financial-primitive names (`PostPaymentCommand`, `WriteOffDebtCommand`, `CreatePaymentPlanCommand`, `SendCorrespondenceCommand`, etc.). The integration guide §5 catalogue is dominated by engine-internal `irm.bpmn-engine.*` commands (`CreateHumanTaskCommand`, `TriggerDebtRecoveryProcessCommand`, `SuspendCaseActivityCommand`). It is not confirmed that the abstracted names are externally-publishable Kafka commands. Options A and B treat the 24-command catalogue as the primary inter-layer contract — if the names do not map to externally-triggerable topics, that contract model collapses. | Solution Architect → Solon platform team (or `revenue-management-adapters` source inspection) | Inter-layer contract design under Options A and B |
| 2 | **Solon REST API stability for sync queries.** The integration guide's synchronous HTTP section is thin (only `ExecuteKieRulesCommand` is documented as a sync API). Option B's BFF assumes Solon REST APIs (`account balance`, `active suppressions`, `task state`) are a stable external contract. Their stability guarantees under Solon upgrades are not documented. | Solution Architect → Solon platform team | Option B BFF layer; Option C ACL contract |

### Factual corrections to existing options

- **Solon natively enforces the standard 60-day Breathing Space ceiling — but NOT MHCM.** The suppression API supports `maximumNumberDays: 60`, `overrideEndDateSW: false`, and a `BREATHING_SPACE` value in `suppressionReasonCL`. For the standard moratorium, configure at the `SuppressionType` level — DCMS does not need to own 60-day enforcement in `BreathingSpaceGatingService`. **The Mental Health Crisis Moratorium (MHCM) is structurally different and remains DCMS-owned**, per RULING-016 §5: it has no fixed end date, lasts for the duration of mental health crisis treatment plus 30 days, and ends only by professional sign-off. This means:
  - **Two distinct `SuppressionType` configurations** are required in Solon: a standard `BREATHING_SPACE` type with `maximumNumberDays: 60` / `overrideEndDateSW: false`, and an `MHCM` type with `maximumNumberDays: 0` (no maximum) and a regime that does not auto-expire. Whether `overrideEndDateSW: false` correctly blocks indefinite suppressions in the MHCM type is unclear from the docs and must be verified during the Option B detailed design — flagged as a sub-question of Blocker 2.
  - **MHCM release path stays DCMS-owned**: `MHCM_RELEASE_PROCESS.bpmn` (manual professional sign-off trigger) issues a `ReleaseSuppressionCommand` to Solon. DCMS owns the AMHP/clinician evidence capture, the 30-day post-treatment tail calculation, and re-confirmation cycles.
  - **`BreathingSpaceGatingService` retains MHCM-specific logic**: distinguishing standard moratorium from MHCM, applying any DWP-specific MHCM overlay (e.g. tighter suppression categories during active crisis vs the 30-day tail), and ensuring no timer-based expiry is silently applied to MHCM cases.
  - **Vulnerability overlap with MHCM is non-trivial.** A debtor in MHCM is by definition vulnerable under FCA FG21/1; the gating service must evaluate MHCM and vulnerability suppressions as compounding, not alternatives. This is unchanged from the original Option B design but warrants explicit re-confirmation in detailed design.
- **Option A's Breathing Space extension point is concrete, not free-form.** `SUSPEND_PROCESS_CHECK.bpmn` is the Call Activity child of `DEBT_RECOVERY_PROCESS.bpmn` that runs the timer-based suppression check loop (querying `DEBT_REC_C` suppression). Under Option A, DCMS-specific gating extends this loop, not arbitrary service-task delegate overrides. MHCM handling under Option A is awkward: the check loop is timer-driven, but MHCM has no timer — DCMS must inject an MHCM-aware branch into the check or maintain a parallel non-timed check. This is a further structural argument against Option A for any programme that treats MHCM as first-class.
- **BPMN reference process names are abstractions.** The 28-process list in the platform reference uses clean abstract names; integration guide §13 lists actual filenames, most of which are tax-authority-specific (`VAT_RETURN.bpmn`, `BUS_REG.bpmn`, `CIT_RETURN.bpmn`). The DCMS-relevant extension targets under Option A are concretely `DEBT_RECOVERY_PROCESS.bpmn` and `SUSPEND_PROCESS_CHECK.bpmn`. Option A's blast radius is therefore narrower than "modified reference BPMN processes" implies — concentrated on these two processes.
- **Boundary-event constraint is narrower than previously framed.** Only *message* boundary events lack non-interrupting support in Amplio; timer boundary events are fully supported in both interrupting and non-interrupting modes. RULING-016 already dissolves the original concern; recorded for completeness.
- **Two custom repos exist, not one.** `revenue-management-be-custom` (Spring beans) and `revenue-management-batch-custom` (batch definitions) are separate per the operations guide. Option B's "DWP-specific batch jobs as custom batch definitions" land in `revenue-management-batch-custom`, not `revenue-management-be-custom`.

### Builder guardrails — Amplio constraints not previously surfaced

These are platform-forced constraints on any DCMS BPMN authored under Option A or B. They must be added to builder-facing handoff notes before any DCMS BPMN authoring begins.

1. **Parallel Gateway is sequential-only.** Amplio executes Parallel Gateway branches sequentially. Any DCMS BPMN that depends on concurrent execution (e.g. concurrent notification dispatch, concurrent write-off approval and DCA instruction) will silently serialise. Design accordingly.
2. **Script Tasks are FEEL-only.** No Groovy or JavaScript. Imperative scripting is unavailable.
3. **Business Rule Tasks must be Service Tasks.** DMN is not supported. Rule evaluation requires a Service Task wired synchronously to Drools KIE (`POST /drools-runtime/execute-stateless/{containerId}`).
4. **Compensation events are not implemented.** SAGA-style flows under Option B must use explicit error boundary events routing to compensation logic — not BPMN compensation markers.
5. **Breathing Space configuration belongs in Solon `SuppressionType` for the standard 60-day moratorium only.** MHCM is a separate `SuppressionType` with no maximum and DCMS-owned release. The gating service evaluates *whether* a moratorium applies and *which* effects to suppress; it does not duplicate the 60-day ceiling for the standard case.
6. **Use only Kafka topics named in integration guide §5.** Do not derive topic names from the platform reference's abstracted command list without confirming the actual topic in `external_sys_docs/solon/solon_tax_2.3.0_integration_guide.md` §5 (subject to Blocker 1).

### Impact on the recommendation

- **Option B remains the most defensible**, conditional on Blocker 1. If the platform-reference commands are not externally publishable, Option B's contract model must be redesigned around `irm.bpmn-engine.*` topics or shifted to sync REST (which then surfaces Blocker 2).
- **Option A is marginally less risky than originally framed for the standard 60-day moratorium** (narrower BPMN modification scope) but **becomes more awkward for MHCM** because `SUSPEND_PROCESS_CHECK.bpmn` is timer-driven and MHCM is not. The structural awkwardness of champion/challenger, IE, and vulnerability governance is unchanged.
- **Option C's self-assessment ("removes most platform value") stands.** Amplio remains the BPMN runtime under Option C, which is the one meaningful platform contribution retained beyond financial APIs. Under Option C, MHCM ownership is fully and cleanly DCMS-side — the cleanest MHCM model of the three options, but at the cost called out in the original Option C analysis.

---

## Design Critic Review Findings (added 2026-04-30, post design-critic pass)

A design-critic review against the integration guide, Kafka event catalog (`KAFKA_EVENT_CATALOG.md`), suppression API reference, Amplio reference, and rulings RULING-005 / RULING-010 / RULING-011 / RULING-014 / RULING-016 surfaced one new critical compliance contradiction, raised the MHCM suppression-type question to blocker level, and identified five significant gaps the SA must resolve before lock. The recommendation of Option B is directionally retained but **cannot be locked** until C1, C3, and S3 below are resolved.

### Critical findings

**C1. The Kafka contract model under Option B may not exist as framed.**
The platform reference's 24-command catalogue (`PostPaymentCommand`, `WriteOffDebtCommand`, `SendCorrespondenceCommand`, etc.) does not appear in the integration guide §5 or `KAFKA_EVENT_CATALOG.md`. The actual catalogue uses `irm.bpmn-engine.*` topics carrying internal BPMN engine signals (`CreateHumanTaskCommand`, `TriggerDebtRecoveryProcessCommand`, `SuspendCaseActivityCommand`). The doc already flags this as Blocker 1 but understates its consequence. Three structural concerns the SA must treat as part of resolving Blocker 1, not after:

- If the abstracted commands are not externally publishable, Option B's "DCMS publishes commands; Solon raises events" model collapses. DCMS must either issue internal BPMN engine signals (tighter coupling than Option A) or fall back to sync REST (activates Blocker 2).
- The Kafka catalogue shows no `PaymentPostedEvent`, `DebtWrittenOffEvent`, or equivalent domain-semantic response events on named response topics. Even if commands are externally publishable, DCMS BPMN processes have no documented way to correlate command outcomes asynchronously without using entity-version or suppression-status events as proxies.
- Blocker 1 must therefore be resolved as a complete contract verification: topic names, payload schemas, response-event existence, and consumer-group isolation guarantees. It is not a name-mapping task.

**C2. Option B's "in-process custom module" and "contained upgrade blast radius" claims are mutually contradictory.**
Option B states (i) DCMS custom beans run inside Solon's application context via `revenue-management-be-custom` component scan, AND (ii) the contract surface between DCMS and Solon is the Kafka command catalogue and documented REST APIs (so Solon upgrades only affect that surface). These are incompatible. In-process beans see the entire Solon Spring class graph — repositories, services, internal beans. A Solon refactor of `case-management-custom` or a renamed internal bean breaks DCMS at upgrade time even if the documented Kafka/REST surface is unchanged. The blast-radius difference between Options A and B is a matter of intent and discipline, not architectural separation. The recommendation should rest on the cleaner domain-service ownership argument and not on a containment claim it cannot support.

**C3. The Breathing Space `SuppressionType` design contradicts RULING-016 by triggering automatic BPMN process-token suspension.**
The suppression API reference shows that a `SuppressionTypeActionType` with `"suspendActiveInstancesSW": true` causes Solon to issue `SuspendCaseActivityCommand` → `irm.bpmn-engine.suspend-case-activity` → `case-management-custom`, suspending active BPMN process tokens for the scoped liability. The pre-configured Breathing Space `SuppressionType` example referenced in the Platform-Reality Review section sets this flag true. Effects:

- If `suspendActiveInstancesSW: true`: Solon auto-suspends process tokens on suppression creation. This violates RULING-016 §3 guardrails 1 and 4 — the process must not be halted entirely; voluntary payments, balance recalculation, and required audit trail events must continue to flow. The Breathing Space gating service's gate-at-effect role is bypassed because the engine has already stopped.
- If `suspendActiveInstancesSW: false`: Solon records the suppression as metadata only; DCMS's `BreathingSpaceGatingService` carries 100% of effect-suppression. Solon's "native enforcement of the 60-day ceiling" claim made in the Platform-Reality Review then degrades — `SuppressionExpiryJob` will still expire the suppression record, but the enforcement of effect prohibition during the 60 days is entirely DCMS-owned.

Both branches are coherent but mutually exclusive. The doc currently asserts both simultaneously: it claims Solon natively enforces the 60-day ceiling (which requires the suppression type to be active and enforcing) AND that the gating service handles all effect suppression at the DCMS layer (which requires the suppression to NOT auto-suspend tokens). Before lock, the SA must declare the `suspendActiveInstancesSW` setting for each DCMS suppression type (standard Breathing Space, MHCM, vulnerability overlay, internal policy holds) and explain how that setting interacts with the gate-at-effect model. The current design cannot be implemented for a compliant moratorium without resolving this.

### Significant findings

**S1. The "debtor-facing effect" boundary is not crisp enough to be applied consistently.**
The doc enumerates: collection contact, enforcement instruction, deduction-from-benefit instruction, DCA instruction, interest accrual. RULING-016 adds court applications, adverse credit reporting, default notices. RULING-014 guardrail 2 adds **arrangement creation during a moratorium** (a new arrangement is itself a demand for payment). RULING-011 adds **dispatching queued `DEBT_COLLECTION` communications on suppression lift** unless the suppression reason is internal. RULING-005 adds **`ARRANGEMENT_BREACH` record generation** during a moratorium. The current informal definition will produce inconsistent gating decisions: a developer triggering a `StrategyEvaluationEvent` and a downstream `ChampionChallengerReassignmentEvent` during MHCM may not call the gate at all because reassignment isn't visibly "debtor-facing" — yet RULING-016 §2 requires deferred outputs to be flagged `DEFERRED_PENDING_MORATORIUM_END`, which is itself a gate concern. **Before any DCMS BPMN authoring begins, a closed enumeration of gated effect categories must be produced**, derived from RULING-016, RULING-011, RULING-014, and RULING-005. The mandatory base class mitigation (Option B risk row 2) is correct but insufficient without the enumeration.

**S2. The vulnerability-flag dual-write to Solon Data Area introduces a staleness window that RULING-010 does not permit.**
Option B mirrors vulnerability flags into Solon Taxpayer Data Area JSONB so Amplio FEEL expressions and Drools rules can read them in-process. The mitigation accepts that "FEEL expressions that read Data Area must tolerate a short staleness window." RULING-010 guardrail 1 requires the vulnerable-customer exclusion rule to be evaluated at every relevant service task, and RULING-010 explicitly mandates **immediate** reassignment from CHALLENGER to CHAMPION on a vulnerability status change to IDENTIFIED or above. An Outbox-based eventual write-through does not satisfy "immediately." A newly-vulnerable debtor in the staleness window remains on a CHALLENGER strategy and the audit event (`CC_ASSIGNMENT_OVERRIDDEN`, `reason = VULNERABILITY_POLICY`) is missing. The MHCM population — by definition vulnerable per FCA FG21/1 — is the highest-risk cohort for this gap. **Counter-question for the SA:** why is this mirrored at all rather than the BPMN service task calling `VulnerabilityClassificationService` directly via the same in-process bean call the gating service already uses? The mirroring approach has not been justified against a direct-call alternative.

**S3. MHCM `SuppressionType` behaviour with `maximumNumberDays: 0` is undocumented and must be raised to a standalone blocker.**
The doc currently lists "MHCM `SuppressionType` configuration" as a sub-question of Blocker 2 in the Open Questions table. This understates it. The behaviour of Solon's `SuppressionExpiryJob` when `maximumNumberDays: 0` is set is not documented in any source in this repository. The job runs daily at 06:00. Two failure modes are possible and neither is observable from the available docs:
- The job interprets `maximumNumberDays: 0` as "zero days, already expired" and auto-expires MHCM suppressions on first run.
- The job interprets it as "no maximum" and skips MHCM records correctly.
If the first interpretation is correct, an MHCM debtor would have their moratorium silently lifted without professional sign-off — a breach of Regulation 21 of the Debt Respite Scheme Regulations 2020 with criminal-liability consequence. **Promote to standalone blocker (Blocker 3 below).**

**S4. Split correspondence ownership under Option B leaves on-lift disposition of queued communications unspecified.**
RULING-011 requires `DEBT_COLLECTION` communications queued during statutory suppression to be discarded on lift, while those queued during internal policy suppression are held for agent review. Under Option B, transport runs through Solon's `CORRESPONDENCE_PROCESS.bpmn` (Solon owns the queued correspondence entries) but category evaluation and suppression-reason classification live in DCMS's `CommunicationSuppressionService`. The flow on suppression lift — how DCMS inspects Solon's queued correspondence, classifies each entry by the suppression reason that caused it to be queued, and instructs Solon to discard or hold — is not described. The suppression API exposes status, not correspondence queues. Options A and C have cleaner ownership stories on this point (A: pre-dispatch guard inside Solon's process; C: DCMS owns transport too). Option B's split ownership is a structural coupling problem that must be designed out before lock.

**S5. The OPA / Rego policy boundary for DCMS-specific access rules is unspecified across all three options.**
DCMS-specific authorisation rules — `OPS_MANAGER`-only champion/challenger policy modification (RULING-010 guardrail 2), vulnerability data restricted to assigned agents, MHCM status as Restricted under RULING-002 / GDPR Art. 9 (RULING-016 §5) — require DCMS-specific Rego policies. The doc says "DWP realm, OPA Rego policies for DCMS-specific access rules" without specifying whether DCMS Rego policies deploy alongside Solon's policies in the same OPA instance, into a separate sidecar, or inline. If co-located: a Solon upgrade modifying the OPA policy bundle could overwrite DCMS policies. If separate: policy evaluation order, conflict resolution, and request flow must be designed. For a system handling vulnerability and MHCM data, an unspecified authz policy boundary is a likely ICO finding.

### Advisory findings

- **A1. Option A's "fastest" rating in the Tradeoff Summary is contingent.** It assumes Solon's reference BPMN processes can be re-used with light modification. Integration guide §13 shows the actual BPMN files are tax-authority-named (`VAT_RETURN.bpmn`, `BUS_REG.bpmn`, `CIT_RETURN.bpmn`); only `DEBT_RECOVERY_PROCESS.bpmn`, `CASE_HANDOVER_COLLECTION_AGENCY_PROCESS.bpmn`, `SUSPEND_PROCESS_CHECK.bpmn`, and `PAYMENT_DEFERRAL.bpmn` are debt-relevant. The "fastest" rating depends on the BPMN fit-gap spike returning a favourable result, which is not yet evidence.
- **A2. Options A and B may collapse to the same architecture in practice.** Both use `revenue-management-be-custom`, run in the same JVM, use the same Kafka topics and Amplio runtime, and share Blocker 1. The stated difference is logical schema separation under B (`dcms` schema vs. shared) — not physical isolation. The SA should declare whether DCMS's beans live in the same Maven module as Solon's `revenue-management-be-custom` beans or in a separate Maven module participating in the same Spring context. If the former, A and B's blast radius and coupling are essentially the same.
- **A3. Java 25 bytecode compatibility ≠ dependency compatibility.** The Forward-Looking Note correctly states JVM-level backward compatibility but does not address whether Solon's transitive dependencies (Drools KIE, Liquibase, Hazelcast) have been tested against Java 25. JVM compatibility is necessary but not sufficient.
- **A4. The recommendation's flip-to-C condition is inconsistent with Lever 1.** The recommendation states the flip requires "Java 21 mandate + Flyway in the DCMS custom tier on a shared database schema." Lever 1 says Java 21 alone flips B to C (because in-process model requires single JVM). One of these statements is wrong. The recommendation summary should be reconciled with Lever 1 before lock.
- **A5. Arrangement-creation gating under RULING-014 is not allocated to any layer in any option.** RULING-014 guardrail 2 requires `createArrangement` to query the live suppression log (not a cached process variable) and reject if a Breathing Space or insolvency suppression is active on the Person. Under Options A and B, the arrangement entry point — whether BPMN start event, REST endpoint, or Spring service method — is not declared as a gate-call site in any of the option designs. This is a statutory requirement; it must be explicitly allocated.

### New blocker (added by Design Critic)

| # | Blocker | Owner | Blocks |
|---|---|---|---|
| 3 | **MHCM and Vulnerability `SuppressionType` behaviour under `maximumNumberDays: 0`.** Whether `SuppressionExpiryJob` interprets this value as "no maximum" or "already expired" is undocumented. Both MHCM and Vulnerability suppressions use `maximumNumberDays: 0` in the documented examples; both are exposed to this ambiguity. The wrong interpretation auto-expires MHCM suppressions without professional sign-off, breaching Reg 21 of the Debt Respite Scheme Regulations 2020, and silently lifts Vulnerability suppressions without agent action. Cannot be inferred from available source docs and cannot be left to detailed design. **Widened 2026-05-01 (Solon platform expert review) from MHCM-only to cover Vulnerability suppressions.** | Solution Architect → Solon platform team (verify via test or platform-team confirmation, not by reading docs) | MHCM and Vulnerability detailed design under Options A and B; criminal-liability exposure for MHCM; RULING-010 compliance for Vulnerability |

### Compliance contradiction requiring resolution

**The `suspendActiveInstancesSW` setting versus RULING-016's gate-at-effect model (C3) is the single most consequential issue raised by this review.** It is not a detail that can be resolved in builder handoff. The SA must explicitly choose, per `SuppressionType`, whether Solon auto-suspends BPMN tokens on suppression creation, and the choice must be reconciled with RULING-016 guardrails before the Option B design is locked.

### Required actions before SA lock

1. Resolve **C1** (Blocker 1) as a complete Kafka contract verification — names, payloads, response events, isolation guarantees.
2. Resolve **C3** by declaring `suspendActiveInstancesSW` per DCMS suppression type and reconciling with RULING-016.
3. Resolve **S3** (Blocker 3 above) — verify `SuppressionExpiryJob` behaviour against `maximumNumberDays: 0`.
4. Reconcile **C2** — drop the "contained upgrade blast radius via Kafka/REST" containment claim or shift the recommendation rationale to the (sound) domain-service ownership argument.
5. Reconcile **A4** — align the recommendation's flip-to-C condition with Lever 1.

### Required actions before BPMN authoring begins

6. Produce a closed enumeration of gated effect categories (**S1**), derived from RULING-005, RULING-010, RULING-011, RULING-014, RULING-016.
7. Allocate arrangement-creation gating (**A5**) explicitly to a layer.

### Significant gaps to close in detailed design

8. Replace Data Area mirroring of vulnerability flags with a direct in-process service call, or justify why the staleness window is acceptable against RULING-010's "immediately" requirement (**S2**).
9. Design the on-lift disposition flow for queued correspondence under split Solon/DCMS ownership (**S4**).
10. Specify OPA policy deployment model and Solon-upgrade survival for DCMS-specific Rego policies (**S5**).

### Sign-off status

**No-material-risk sign-off NOT issued.** The doc cannot receive sign-off in its current state due to C1, C3, and S3.

---

## Release 1 Capability Review Findings (added 2026-05-01, post comparison against `docs/release/release-1-capabilities.md`)

A review of this document against the authoritative Release 1 capabilities baseline (date 2026-04-30) closes Lever 3, broadens Lever 2 from a DMN-authoring sidecar question into a first-class strategy-authoring application, and renders Option A non-viable as a production architecture for Release 1 scope. The recommendation of Option B is structurally hardened. A new delivery framing — **Option B-Demo** (Option B architecture with Release 1 demo-depth implementation) — is added to reconcile Release 1's prototype/demo scope with Option B's architectural shape. See the new "Option B-Demo" section and the new "Release 1 Capability Workarounds Under Option A" sub-section in §"Option A".

### Critical findings

**R1. Lever 3 is closed: champion/challenger and IE are explicitly in scope for Release 1.**
Release 1 enumerates champion/challenger as a full capability (configurable splits, statistical significance indicator, results from real operational data, promotion of winners, dedicated analytics view accessible from both strategy module and operational dashboard) and Income & Expenditure as a configurable engine (structured form, configurable reference data, configurable disposable-income model with formula/floor/ceiling/reference table, statutory forbearance presentation alongside plan options). The "flip B → A" branch in the Recommendation and Lever 3 is unavailable. **Strike Lever 3.**

**R2. Option A is not viable as a production architecture for Release 1 scope.**
Option A's coherence depended on the components that have no Solon scaffold being descoped or radically simplified — champion/challenger, IE engine, vulnerability governance. Release 1 scopes all three in. The "Release 1 Capability Workarounds Under Option A" table now in §"Option A" shows that of eleven Release 1 capability rows, three (strategy authoring, champion/challenger analytics, multiple dashboards) cannot be delivered in a way that stays structurally within Option A — the workaround quietly is Option B compressed into Solon's custom module — and three more (joint debts, bulk move, audit trail) work for the demo only and force a production rebuild that converges on Option B. **Option A remains in this document for contrast and traceability but is not recommended for Release 1 production architecture. The Option B-Demo framing replaces Option A as the demo-cost-conscious delivery option.**

**R3. Strategy authoring is a first-class Release 1 capability that no option originally designed for.**
Release 1 lines 75-92 specify: visual or structured authoring interface navigable by business users; form-driven with no code editor or IT change request; draft versioning, side-by-side diff with the live version, complete version lineage; configurable peer review and approval workflow with permission roles; deployment to non-production environments before live promotion; one-click rollback to any prior version; full audit lineage for every strategy lifecycle event; logical-consistency validation before save; pre-configured templates for deceased, insolvency, hardship, and persistent debt scenarios. This is not a DMN-authoring sidecar — it is a strategy-management application with its own data model, lifecycle, environment-promotion mechanism, and review workflow. Drools DRL in a custom KIE module satisfies the *runtime* but not the authoring surface. **Lever 2 must be rewritten:** the question is no longer "is Drools acceptable for non-technical authoring" but "where does the DCMS-owned strategy-authoring service live, and how does it deploy strategy versions into the runtime engine." Both Option B and Option C must extend their designs to include this service. The Option B-Demo framing scopes its Release 1 implementation depth to a parameter-form-over-pre-baked-strategies prototype with mocked review workflow — this is its single largest exposure.

### Significant findings

**R4. Multi-restriction-type runtime gating is broader than RULING-016 alone.**
Release 1 lines 148-157 require five restriction types — breathing space, dispute, deceased, legal, fraud — all enforced at runtime at the moment of action through a single gating model, with explicit alerts naming what was blocked, no trace of the blocked action in outbound systems, and override prevention for standard agents. The current Option B design discusses Breathing Space gating exhaustively but does not generalise the gating service to the full restriction-type set. The gated effect categories enumeration required by Design Critic S1 must be parameterised by restriction type, not Breathing Space only. Each restriction type may have different effect categories (e.g. dispute permits balance enquiries but blocks enforcement; deceased blocks all debtor-facing effects; fraud may permit some collection activity with elevated authorisation).

**R5. Joint debt and household-linked-individual modelling is not aligned with Solon's per-Taxpayer entity.**
Release 1 lines 41-55 require an explicitly modelled, navigable household relationship structure, joint debt ownership by two linked individuals, joint-to-individual debt split with 50/50 apportionment and rounding write-off, cause-of-action preservation across split, block on split when an active arrangement or legal hold exists, and a unified linked-account view with status indicators per account. Solon's `Taxpayer` entity is per-person; joint ownership and household relationships are not native. **Option B's data ownership table must declare Customer, HouseholdRelationship, and JointDebt as DCMS-owned aggregates explicitly.** Under Option B-Demo these aggregates exist at full schema depth from the start; volume and identity-resolution depth are scoped down.

**R6. Bulk-move-accounts-to-workflow-point requires deep BPMN process surgery.**
Release 1 line 120 requires authorised users to bulk move accounts forward or backward to a selected point within a workflow, with reason and audit. Under Option B (and Option B-Demo), DCMS-authored processes are designed with explicit bulk-move entry points from the start. Under Option A, every modified Solon BPMN file must be augmented with bulk-move entry points, compounding the upgrade-fork blast radius (Option A risk row 1). The Amplio Parallel Gateway sequential-execution constraint (Builder Guardrail 1) must be considered when authoring those entry points.

**R7. Configurable-without-code spans the platform and demands DCMS-owned admin surfaces.**
Release 1 specifies configuration-without-code for: validation rulesets, segmentation thresholds and ordering, vulnerability type-to-suppression mapping, IE reference data and disposable-income model, payment waterfall order, contact frequency limits, dashboard KPIs and thresholds, custom data capture extension, role-configurable views, role-based delegated authority limits. Some pieces (Data Area JSONB extensibility for per-entity DWP fields) fit Option A. The majority require DCMS-owned admin services with their own data models. **Widen the Tradeoff Summary row "Champion/challenger / IE / vulnerability governance" to "DCMS-owned configurable domain logic"** — the structural argument applies across many Release 1 capabilities, not just the three originally named.

**R8. Audit trail completeness requires a DCMS-owned audit shape.**
Release 1 lines 244-250 require complete reconstruction of any account, case, strategy, or management action from the audit trail alone, with before/after state, accessibility to permissioned users without analyst involvement, and explicit logging of blocked actions naming the triggering restriction. Solon's audit infrastructure exists but its event shape is tax-authority-oriented. DWP-specific audit events (vulnerability flag change, strategy version promotion, champion/challenger reassignment, restriction override attempt, bulk move, joint-debt split, household-link review, blocked-action with restriction reference) need a DCMS-controlled audit schema. Under Option A this would either be routed through Solon's audit (giving up DWP control of the schema) or maintained as a parallel DCMS audit that diverges from Solon's. Under Option B and Option B-Demo, DCMS owns `audit_event` cleanly. **Reinforces Option B.**

### Advisory findings

**R9. Multiple dashboard surfaces over mixed Solon + DCMS data require a BFF.**
Release 1 references operational summary, queue management, supervisor intake, linked-account view, and champion/challenger analytics dashboards (line 17 notes these may share screens but the functionality is separate). Each spans data Solon does not own (champion/challenger assignments, vulnerability records, IE assessments, household relationships, restriction-state metadata). Option A's "React calls Solon REST APIs directly with thin custom endpoints" path does not satisfy this aggregation requirement; a BFF tier is required, which is an Option B characteristic. **Further argues Option A reduces to Option B under Release 1 scope.**

**R10. "Suppression must be immediate; not deferred to a batch process" (line 140) reinforces Design Critic S2.**
Vulnerability suppression effect must apply immediately on save. Option B's previously-discussed vulnerability-flag dual-write to Solon Data Area via Outbox introduces a staleness window that breaches both this Release 1 requirement and RULING-010's "immediately." **Drop the Data Area dual-write as a default. Replace with direct in-process bean call from BPMN service tasks** — already adopted in Option B-Demo's VulnerabilityClassificationService row.

**R11. Release 1's prototype/demo scope sets the contract-and-evidence bar, not a live-integration bar.**
Release 1's scope principles (lines 9-15) state it is a prototype/demo release demonstrating DCMS-owned capability through adapter boundaries, contracts, simulated responses, state transitions, and audit evidence. Live third-party integrations, production dialler operation, automated SMS/email sending, payment-link providers, and live bank-validation APIs are out of scope. **This means Blockers 1, 2, 3 must be resolved at the *contract* level (topic names, payload schemas, response semantics, expiry-job behaviour) for design-lock, but do not require running production Kafka against Solon for Release 1 to ship.** This finding is the foundation of the Option B-Demo framing — demo depth is acceptable across the implementation surface; architectural shape must still be locked.

**R12. Pre-DCA disclosure notification is a Release 1 capability that maps cleanly to existing option designs.**
Release 1 line 172 requires automatic generation of disclosure notifications at configurable points in the journey, including pre-DCA placement. This is consistent with the DWP pre-placement disclosure gate already named in Options A and B's boundary tables (extending `CASE_HANDOVER_COLLECTION_AGENCY_PROCESS.bpmn`). No new design implication; recorded for traceability.

### Required actions before SA lock

19. Strike Lever 3 (champion/challenger / IE descope) from this document; Release 1 closes it.
20. Rewrite Lever 2 to reflect the strategy-authoring application requirement; add "strategy-authoring service" to Option B's boundary table and to Option B-Demo's depth table (already present in the new B-Demo section).
21. Confirm Option A's status as contrast/traceability rather than recommended-for-Release-1; Option B-Demo replaces it as the demo-cost-conscious delivery option.
22. Generalise the gated effect categories enumeration (Design Critic S1) by restriction type — Breathing Space, dispute, deceased, legal, fraud — rather than Breathing Space only.
23. Declare Customer, HouseholdRelationship, and JointDebt as DCMS-owned aggregates in Option B's data ownership table.
24. Drop the Data Area dual-write mitigation for vulnerability flags (R10 + Design Critic S2); direct in-process bean call from BPMN service tasks is the default.
25. Confirm Option B-Demo as the recommended Release 1 delivery framing or call out the specific service whose Release 1 demo-depth is unsafe (most likely strategy authoring).

### Sign-off status

**No-material-risk sign-off NOT issued.** This review reinforces the four prior blockers and adds a structural reframing requirement: the option set is revised to add Option B-Demo and to demote Option A from candidate-for-Release-1 to traceability-and-contrast. Option A as currently described cannot deliver Release 1 as a production architecture; Option B-Demo is the cost-conscious recommendation for Release 1 scope.

---

## Solon Platform Expert Review Findings (added 2026-05-01)

A platform expert review against `external_sys_docs/solon/solon_tax_2.3.0_integration_guide.md`, `external_sys_docs/solon/KAFKA_EVENT_CATALOG.md`, `external_sys_docs/solon/api-suppression-management.md`, `external_sys_docs/amplio-documentation/` (process engine reference), and `external_sys_docs/solon/solon_tax_2.3.0_operations_guide.md` produced the following findings. The platform expert role is grounded exclusively in `external_sys_docs/`; design recommendations in prior sections that are not contradicted below are unchanged.

### Critical findings

**PE-C1. Abstracted Kafka command names are not confirmed in `external_sys_docs/` — boundary tables present an unconfirmed contract.**
The boundary tables across Options A, B, and B-Demo cite `PostPaymentCommand`, `WriteOffDebtCommand`, `CreatePaymentPlanCommand`, `SendCorrespondenceCommand`, and related names as the DCMS→Solon Kafka contract. None of these names appear in `external_sys_docs/solon/KAFKA_EVENT_CATALOG.md` or in `external_sys_docs/solon/solon_tax_2.3.0_integration_guide.md` §5. The confirmed external Kafka surface in those documents is the `irm.bpmn-engine.*` topic family carrying internal BPMN engine signals. Every boundary table row that lists an abstracted command name is presenting an unconfirmed contract. This is not a naming-alias question: if the abstracted names are not externally-publishable topics, every boundary row that depends on them collapses and must be redesigned. The mandatory safe default before Blocker 1 is resolved is REST (`POST /process-management/process-instances` or the relevant financial-primitive REST endpoints) — not Kafka. No builder should implement Kafka publication of abstracted command names until the platform team confirms the topic exists and is externally consumable.

**PE-C2. `suspendActiveInstancesSW: true` in documented Breathing Space and Vulnerability `SuppressionType` examples is incompatible with RULING-016 — must be `false` for all DCMS suppression types.**
The suppression API reference (`external_sys_docs/solon/api-suppression-management.md`) documents that `SuppressionTypeActionType` with `suspendActiveInstancesSW: true` causes Solon to issue `SuspendCaseActivityCommand` → `irm.bpmn-engine.suspend-case-activity` → `case-management-custom`, which suspends active BPMN process tokens for the scoped liability. The documented Breathing Space `SuppressionType` example sets this flag `true`. Setting `suspendActiveInstancesSW: true` for any DCMS suppression type means:

- Solon auto-suspends process tokens on suppression creation, halting the BPMN instance entirely.
- RULING-016 §3 guardrails 1 and 4 explicitly require the process to continue advancing — voluntary payments, balance recalculation, and audit trail events must continue.
- The `BreathingSpaceGatingService` gate-at-effect model is bypassed because the engine has already stopped before the gate is reached.

The setting must be `false` for all DCMS suppression types (standard Breathing Space, MHCM, Vulnerability, internal policy holds). Consequence: 100% of effect-suppression is owned by `BreathingSpaceGatingService`. Solon's `SuppressionExpiryJob` still manages the suppression record lifecycle (expiry at 60 days for standard Breathing Space); enforcement of prohibited effects during the active window is entirely DCMS-owned. This is the only configuration compatible with RULING-016 and must be declared in every DCMS `SuppressionType` configuration before the Option B detailed design is locked.

**PE-C3. Blocker 3 must be widened to cover Vulnerability suppressions.**
As confirmed under Design Critic S3 (promoted to Blocker 3), `maximumNumberDays: 0` behaviour in `SuppressionExpiryJob` is undocumented. The vulnerability `SuppressionType` in the documented examples also uses `maximumNumberDays: 0` — the same ambiguity that affects MHCM applies to Vulnerability. A `SuppressionExpiryJob` "zero days = already expired" interpretation would silently lift Vulnerability suppressions on first daily run at 06:00, exposing vulnerable debtors to unconstrained collection activity without agent awareness. Blocker 3 must cover both MHCM and Vulnerability. See Blocker 3 entry above (already widened by this review).

### Significant findings

**PE-S1. Contact-management bypass: Solon-internal BPMN can create contacts via `irm.bpmn-engine.create-contact`, undetectable by DCMS REST gating.**
The integration guide §5 lists `CreateContactCommand` on the `irm.bpmn-engine.create-contact` topic as a Solon-internal signal used by `CORRESPONDENCE_PROCESS.bpmn` and related processes to create contact records. If a Solon BPMN process creates a contact via this internal signal, no DCMS REST endpoint is called — the `BreathingSpaceGatingService` REST gate is not in the path. A debtor-facing contact record could be created during a suppression window without DCMS's gate being consulted. The safe gating pattern for contact management is: gate `POST /contact-management/contacts/{id}/generate` (the documented REST endpoint for contact generation dispatch), not contact record creation. Contact creation without dispatch does not by itself constitute a prohibited debtor-facing effect; dispatch does. Design must gate dispatch, not creation.

**PE-S2. Amplio reference cites `solon.process.commands` topic — this topic does not exist in `KAFKA_EVENT_CATALOG.md`; authoritative path is REST.**
The Amplio process engine reference document (`external_sys_docs/amplio-documentation/`) references `solon.process.commands` as a topic for process instance start signals. This topic name does not appear in `external_sys_docs/solon/KAFKA_EVENT_CATALOG.md`. Absent confirmation from the Solon platform team, `solon.process.commands` must not be treated as a consumable external topic. The authoritative documented mechanism for starting a process instance from outside Solon is REST: `POST /process-management/process-instances` (integration guide §4). Builders must use REST for process starts until the platform team confirms otherwise.

**PE-S3. OPA Rego extension surface for DCMS-specific policies is undocumented in `external_sys_docs/`.**
No document under `external_sys_docs/solon/` or `external_sys_docs/amplio-documentation/` describes how custom Rego policies are deployed into Solon's OPA instance, whether a separate sidecar is supported, or how policy evaluation order and conflict resolution work when DCMS policies co-exist with Solon policies. This gap (also noted as Design Critic S5) cannot be resolved from the available documentation. The Solution Architect must obtain deployment model documentation from the Solon platform team before DCMS-specific Rego policy authoring begins.

**PE-C4. Design Critic C2 is confirmed: in-process containment claim is overstated.**
`revenue-management-be-custom` shares a JVM with `revenue-management-be` per the operations guide (`external_sys_docs/solon/solon_tax_2.3.0_operations_guide.md`). DCMS Spring beans scanned into this context see the full Solon Spring class graph. The "contained upgrade blast radius via Kafka/REST surface" claim is not supported by the physical deployment model. The recommendation must rest on the domain-service ownership argument (clean bounded context, DCMS owns its aggregates, Solon is addressed via documented APIs) and must drop the containment claim. This was flagged by the Design Critic; this review confirms it from the operations guide source.

### Builder Guardrails (Solon platform expert — added 2026-05-01)

These guardrails are non-negotiable constraints derived from `external_sys_docs/`. They extend and do not replace the Builder Guardrails in the Platform-Reality Review section.

1. **Use REST for all DCMS→Solon commands until Blocker 1 is resolved.** Default to `POST /process-management/process-instances` and documented financial-primitive REST endpoints. Do not publish to Kafka topics whose names are not confirmed in `external_sys_docs/solon/KAFKA_EVENT_CATALOG.md`. Source: integration guide §4–5; `KAFKA_EVENT_CATALOG.md`.

2. **Consume only outbound Kafka topics listed in `KAFKA_EVENT_CATALOG.md`.** Do not derive consumable topic names from abstracted platform reference documentation or Amplio reference docs. Source: `external_sys_docs/solon/KAFKA_EVENT_CATALOG.md`.

3. **Parallel Gateway branches are sequential, not concurrent.** Any DCMS BPMN that requires concurrent execution (parallel notification dispatch, concurrent approval and instruction) will silently serialise. Design processes to tolerate sequential branch execution. Source: `external_sys_docs/amplio-documentation/` (process engine reference, gateway semantics).

4. **Script Tasks are FEEL-only.** Groovy and JavaScript are not supported. Do not author Script Tasks with imperative scripting. Source: `external_sys_docs/amplio-documentation/` (process engine reference, supported expression languages).

5. **Business Rule Tasks require a Service Task wired to Drools KIE.** DMN evaluation is not supported in Amplio. Rule evaluation requires `POST /drools-runtime/execute-stateless/{containerId}`. Source: `external_sys_docs/amplio-documentation/` (process engine reference, supported task types).

6. **Set `suspendActiveInstancesSW: false` for every DCMS `SuppressionType` configuration.** Setting this flag `true` causes Solon to auto-suspend BPMN tokens, bypassing the gate-at-effect model required by RULING-016. Source: `external_sys_docs/solon/api-suppression-management.md` (`SuppressionTypeActionType` definition).

7. **Do not use `solon.process.commands` as a Kafka topic for process instance starts.** This topic name is not present in `KAFKA_EVENT_CATALOG.md`. Use `POST /process-management/process-instances` instead. Source: `external_sys_docs/solon/KAFKA_EVENT_CATALOG.md`; `external_sys_docs/solon/solon_tax_2.3.0_integration_guide.md` §4.

8. **Gate `POST /contact-management/contacts/{id}/generate`, not contact record creation.** Solon-internal BPMN creates contact records via `irm.bpmn-engine.create-contact` without calling DCMS REST endpoints. Contact dispatch (generate) is the prohibited debtor-facing effect; gate there. Source: `external_sys_docs/solon/solon_tax_2.3.0_integration_guide.md` §5 (`CreateContactCommand`); `external_sys_docs/solon/` contact management API.

9. **Do not use BPMN compensation events.** Amplio does not implement compensation event semantics. SAGA-style error recovery must use explicit error boundary events routing to compensating Service Tasks. Source: `external_sys_docs/amplio-documentation/` (process engine reference, unsupported BPMN elements).

10. **Deploy DCMS BPMN process definitions via `POST /process-management/process-definitions`.** This is the documented REST endpoint for deploying new or updated process definitions into the Amplio engine. Do not assume file-drop or class-path deployment is supported for custom processes. Source: `external_sys_docs/solon/solon_tax_2.3.0_integration_guide.md` §4 (process management API).

### Sign-off status

**Approved with conditions.** Option B / B-Demo is the most defensible option. Three mandatory conditions must be met before the Solution Architect locks the integration design:

1. **Kafka contract verification (Blocker 1):** default all DCMS→Solon commands to REST; do not proceed with Kafka publication of abstracted command names until the Solon platform team confirms topic names, payload schemas, response-event existence, and consumer-group isolation in writing.
2. **`suspendActiveInstancesSW: false` declared for all DCMS suppression types:** MHCM, standard Breathing Space, Vulnerability, and internal policy holds — with the rationale that 100% of effect-suppression responsibility rests with `BreathingSpaceGatingService` per RULING-016.
3. **Blocker 3 widened and resolved for both MHCM and Vulnerability:** `SuppressionExpiryJob` behaviour under `maximumNumberDays: 0` must be confirmed by the Solon platform team before MHCM or Vulnerability suppression type configuration is finalised.

No Solon-related integration design element downstream of these three conditions should be treated as locked until each condition is discharged and recorded in the decision log.

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

### Release 1 Capability Workarounds Under Option A (added 2026-05-01)

Reviewed against `docs/release/release-1-capabilities.md`. Each row records the demo-depth workaround that Option A's extension model can deliver, the cost the workaround imposes, and whether the workaround structurally remains Option A or quietly converges on Option B's pattern.

| Release 1 capability | Demo-depth workaround under Option A | Cost / risk | Stays Option A? |
|---|---|---|---|
| **Strategy authoring** (lines 75-92) — visual interface, draft versioning, peer review, side-by-side diff, rollback, environment promotion | Ship 3-5 pre-baked DRL strategies. Build a thin React form editing a small parameter set (thresholds, contact frequency, channel choice) writing to a config table read by Drools. Mock peer-review/approval workflow with hardcoded version records. Scripted demo of draft → review → approve → deploy → rollback. | High. The visual rule-editor and configurability-without-code claim is the centrepiece of the tender. A parameter form over pre-baked DRL may not survive a probing audience. Production rebuild is a full strategy-management application. | **No — pre-baked strategies + parameter form is materially below Release 1's stated capability.** |
| **Champion/challenger analytics** (lines 94-107) — split configuration, real-data analytics, statistical significance, promotion of winners | Custom Spring bean implements split assignment (random + reproducible). Operational data flows through whichever cohort the bean selected. Analytics view as a BFF query against a `champion_challenger_assignment` table populated during BPMN execution. Significance indicator as a simple computed field. | Medium. Demo achievable. The required `champion_challenger_assignment` table is DCMS-owned schema — the structural argument against Option A applies. | **No — workaround is structurally Option B compressed into `revenue-management-be-custom`.** |
| **Income & Expenditure engine** (lines 188-196) — structured form, configurable reference data, configurable disposable-income model | Pre-baked CFS reference data. Configurable model exposed via small admin form (formula type, floor, ceiling, reference table version). Affordability calculation in a single Drools rule. IE record stored in Data Area JSONB on `Taxpayer`. | Medium. Demo-quality achievable. Reporting and historical IE record retrieval degrade as data grows; sufficient for demo, not production. | Borderline — Data Area JSONB is genuinely Option A; reference-data admin form is a DCMS surface. |
| **Vulnerability governance** (lines 134-145) — structured capture, configurable type-to-suppression mapping, immediate suppression, expiry monitoring | Hardcode the type-to-suppression mapping in a config table; expose via admin form labelled "configurable." Suppression call to Solon via existing Kafka command. Direct in-process bean call from BPMN service tasks (skip the dual-write debate; demo doesn't need write-through). Exception alerts as a scheduled query. | Low for demo. The "immediately" requirement is satisfied trivially when no dual-write exists. | Yes for demo, with the caveat that the production version needs DCMS-owned `vulnerability_record` schema. |
| **Joint debts and household linking** (lines 41-55) — explicit relationship structure, joint ownership, joint-to-individual split, unified linked-account view | Data Area JSONB on `Taxpayer`: `linkedTaxpayerExternalId`, `jointDebtFlag`, `cause_of_action_date`. Unified view as BFF query joining two Taxpayers by external ID. Joint-to-individual split as custom Spring bean: creates second Taxpayer, cancels parent case, creates two new cases. Block-on-active-arrangement check in the bean. | Medium for demo (≤10 demo customers). Search-by-NI across the relationship is slow; reporting on joint debts is a JSONB scan. Production needs real entities. | Yes for demo only — production rebuild required. |
| **Multi-restriction-type runtime gating** (lines 148-157) — breathing space, dispute, deceased, legal, fraud through a single model | Build the gating service for Breathing Space properly. Stub dispute, deceased, legal, fraud as "calls same gate; demonstrates architecture." Demo runs one or two restriction scenarios end-to-end. | Low for demo. The demo doesn't need to exercise all five restrictions — the architecture is the deliverable. | Yes for demo. |
| **Bulk move accounts to workflow point** (line 120) — authorised bulk forward/backward move with reason and audit | Implement on the one BPMN process used in the demo flow. Don't retrofit across the rest of the modified Solon reference processes. | Low for demo, High for production — bulk-move entry points must then be added to every modified Solon BPMN file, compounding the upgrade-fork blast radius. | Demo: yes. Production: forces broader BPMN modification, increasing Option A's headline risk row 1. |
| **Audit trail completeness** (lines 244-250) — full reconstruction, before/after state, blocked-action logging | Solon audits Solon-side events. DCMS logs DWP-specific events (vulnerability change, strategy promotion, champion/challenger reassignment, override attempt, bulk move, joint-debt split, blocked-action) to a `dcms_audit` table. Reconstruction view as a BFF query joining the two. | Low for demo, Medium for production — schema divergence between Solon audit and `dcms_audit` accumulates as DWP-specific events grow. | Demo: yes. Production: DCMS-owned audit shape is required, which is Option B. |
| **Configurable validation, segmentation, dashboards** | Hardcode demo data; expose 2-3 knobs through admin forms; mark the rest as "configurable in production roadmap." | Standard demo move. Defensible if scoped clearly to evaluators. | Yes for demo. |
| **Restrictions runtime alert** (line 153) — explicit alert showing what was blocked and which restriction caused it | Gating service returns a `BlockedAction` record. React surfaces it as a banner. | Trivial. | Yes. |
| **Multiple dashboard surfaces** (line 17, 222-232) — operational, queue, supervisor intake, linked-account, champion/challenger | Each dashboard as a React view backed by a BFF query. Aggregate Solon REST + DCMS state. | Medium. Option A's "React calls Solon REST APIs directly" path doesn't satisfy aggregation across DCMS-owned data. A BFF tier is required. | **No — BFF is an Option B characteristic.** |

**Summary observation:** Of eleven Release 1 capability rows above, three (strategy authoring, champion/challenger analytics, multiple dashboards) cannot be delivered in a way that stays structurally within Option A — the workaround quietly is Option B compressed into Solon's custom module. Three more (joint debts, bulk move, audit trail) work for the demo only and force production rebuild that converges on Option B. The remaining five fit Option A's extension model legitimately.

The headline issue this surfaces: **Option A's demo viability and Option A's architectural identity diverge.** A demo can be built within Option A's extension surfaces, but several capabilities demanded by Release 1 require workarounds that are Option B's pattern in disguise. If Release 1 is delivered under "Option A," the resulting system is partially Option B with worse separation, and Release 2 starts by tearing out the demo workarounds. See also the new framing **"Option B with Release 1 Demo-Depth Implementation"** below, which preserves the architectural shape while matching Release 1's prototype/demo depth.

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

## Option B-Demo: Option B Architecture with Release 1 Demo-Depth Implementation (added 2026-05-01)

### Framing

This is not a fourth architectural option. It is a delivery framing that pairs Option B's architecture with implementation depth scoped to Release 1's prototype/demo bar. The architectural shape — separate `dcms` schema, dedicated DWP domain services, DCMS-authored BPMN deployed into Amplio, BFF tier between React and the service mesh, in-process gating service called at every debtor-facing service task — is delivered as designed. What is scoped is the *depth* of each service's implementation, matching what Release 1 explicitly accepts: simulated integrations, adapter boundaries with contract evidence, demo-set data volumes, scripted scenarios.

This framing exists because of the issue surfaced under Option A's Release 1 Capability Workarounds table: an "Option A" delivery for Release 1 ends up adopting Option B's pattern for several capabilities anyway, but inside `revenue-management-be-custom` with no schema separation, no BFF, and a forked Solon BPMN library. Option B-Demo preserves the architectural shape that Release 2 will inherit while keeping Release 1's build cost honest about its prototype scope.

### Implementation depth per service

| Service | Architectural shape (kept) | Demo-depth implementation (Release 1 only) | Production-depth gap |
|---|---|---|---|
| Strategy-authoring service | DCMS-owned service with strategy-version data model, draft/review/approve/deploy lifecycle, BFF endpoints, audit hooks | Form-driven editor over a constrained parameter set (thresholds, contact frequency, channel choice). Pre-baked DRL strategy templates for deceased / insolvency / hardship / persistent debt / pre-arrears. Mocked peer-review and approval workflow with hardcoded reviewer roles. Side-by-side diff implemented as a JSON-tree comparison view. One-click rollback against the version table. Non-prod environment promotion as a flag toggle, not a full deployment pipeline. | Visual or graph-based rule editor; full RBAC-driven approval workflow; multi-environment promotion pipeline; rule-consistency validator beyond schema validation |
| BreathingSpaceGatingService | Spring bean with `breathing_space_gate_log` schema, called at every debtor-facing service task via mandatory base class | Full implementation. Demo runs end-to-end Breathing Space scenarios (entry, gating during, lift). MHCM scenario is scripted but logic is real. | None for Breathing Space; multi-restriction generalisation (R4) sits here in production |
| Multi-restriction-type gating | Single gating model parameterised by restriction type (BS, dispute, deceased, legal, fraud) | Breathing Space implemented in full. Other types stubbed at the contract level — gate returns correct decision; downstream effect is logged but not exhaustively wired across all BPMN service tasks. Demo runs one dispute and one deceased scenario end-to-end. | Full restriction-type effect catalogue per Design Critic S1 generalised by R4 |
| ChampionChallengerService | DCMS-owned service with `champion_challenger_assignment` table including harm-indicator columns from sprint 1 | Split assignment fully implemented (random + reproducible). Promotion-of-winner action implemented. Statistical significance indicator computed via simple t-test on demo data. Analytics view fully built. | Full predictive-analysis inputs; live cohort-management at scale |
| IEAssessmentService | DCMS-owned service with `ie_assessment` schema, configurable disposable-income model | Full structured form. CFS reference data pre-loaded. Disposable-income model exposed via admin form (formula type, floor, ceiling, reference table version). Statutory forbearance options presented alongside plan options. | Configurable household composition lookup; affordability stress-testing |
| VulnerabilityClassificationService | DCMS-owned service with `vulnerability_record` schema, **direct in-process bean call from BPMN service tasks (no Data Area dual-write)** per R10 | Full structured capture form. Configurable type-to-suppression mapping table with admin form. Immediate suppression on save (no deferral). Exception alerts as scheduled query. | Specialist-routing recommendation engine depth; vulnerability-type machine-learning predictions |
| CommunicationSuppressionService | DCMS-owned service with `communication_event` schema; category evaluation in DCMS, transport via Solon `CORRESPONDENCE_PROCESS.bpmn` | Full category-evaluation logic. On-lift disposition flow per Design Critic S4 designed and implemented. Transport simulated against a stubbed Notifications API adapter. Demo shows queued-during-suppression and disposition-on-lift scenarios. | Live Notifications API; SMS/dialler provider integration |
| Customer / HouseholdRelationship / JointDebt | First-class DCMS-owned aggregates per R5 | Full schema. Joint-debt split with rounding write-off and cause-of-action preservation. Block-on-active-arrangement enforced. Unified linked-account view backed by BFF query. Search by NI / name across the relationship. Demo set ≤50 customers including ≥3 joint debts. | Scale tuning; full identity-resolution against external sources |
| Bulk-move-to-workflow-point | DCMS BPMN processes designed with explicit bulk-move entry points from authoring | Implemented on `DWP_DEBT_RECOVERY_PROCESS.bpmn` and `DWP_ARRANGEMENT_PROCESS.bpmn`. Other DCMS processes have the entry-point shape but bulk-move is not exercised in demo. | Cross-process consistency rules; bulk-move to a derived state in long-running flows |
| Configurable validation, segmentation, intake | DCMS-owned admin services with their own data models | Full validation ruleset and segmentation threshold editing. Intake configuration as an admin surface. Live progress indicator implemented. | Full audit trail of configuration changes (separate from data audit); configurable approval workflow on configuration changes |
| Audit trail | DCMS-owned `audit_event` schema, INSERT-only, with full DWP event vocabulary | Full schema. Every DWP-specific event recorded with before/after state. Permissioned UI access. Reconstruction queries demonstrated end-to-end on demo set. | Long-retention partitioning; cross-system reconstruction joins with Solon audit at scale |
| Multi-dashboard surfaces | BFF tier aggregating Solon REST + DCMS service responses; React views | All five Release 1 dashboard surfaces (operational summary, queue management, supervisor intake, linked-account, champion/challenger analytics) implemented at demo depth with hardcoded role-filter rules. | Full role-configurable KPI/threshold authoring without code |
| Integrations | Adapter contracts with simulated implementations | All Release 1 integration adapter boundaries defined: WorldPay, Notifications API, dialler, DWP Place, GL host, third-party DCA. Each backed by a simulator returning scripted responses. Acknowledgement, reconciliation, and audit evidence flows demonstrated. | Live third-party integrations (out of Release 1 scope by definition) |

### Consequences for delivery

- Release 1 build cost is materially lower than full Option B: the architectural shape is delivered, but the implementation depth of each service is scoped to demo. Service implementations grow to production depth across Release 2 and beyond without architectural rework.
- The architectural shape is what DWP evaluators see — DCMS-owned services, DCMS-owned schema, BFF, runtime gating model, audit shape. The demo proves "DCMS-owned capability through adapter boundaries, contracts, simulated responses, state transitions, and audit evidence" (Release 1 scope principles).
- All four blockers and the compliance contradiction still apply at the contract level. They must be resolved before lock; their resolution timeline can align with the simulator-driven build per R11.
- The strategy-authoring service is the single most exposed component. A parameter form over pre-baked DRL strategies is honest demo depth but will need to be presented carefully to evaluators — pitched as "Release 1 prototype of the visual-authoring surface; full visual editor in Release 2 of the delivery roadmap" rather than "this is what configurability looks like." If DWP probes hard on this surface, this is where the demo is weakest.
- All risks listed under Option B continue to apply. None are weakened by demo-depth scoping; the dual-write risk (S2) is dropped as a default per R10.

### When to choose this framing over full Option B

- Default for Release 1, given that Release 1 is explicitly a prototype/demo and full Option B depth is not required by the scope principles.
- Switch back to full Option B depth at the point Release 2 scope is locked or when a specific service's production depth is needed earlier (e.g. live integration to a regulated partner).

### When this framing is the wrong call

- If Release 1 is being treated by stakeholders as more than a prototype — i.e. expected to handle real DWP debt accounts in any form — demo-depth scoping is unsafe and full Option B is required.
- If the strategy-authoring evaluation is judged as a make-or-break tender criterion that cannot survive a parameter-form-with-pre-baked-strategies demo, Option B-Demo's biggest exposure is fatal and the strategy-authoring service must be built closer to full depth even at Release 1 cost.

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

### Lever 2: Strategy-authoring application (rewritten 2026-05-01 per Release 1 review R3)

**Previous framing (superseded):** "Non-technical DMN authoring is a hard requirement." Treated as an incremental complication: Drools DRL plus an optional DMN sidecar.

**Current state per Release 1 review R3:** Release 1 lines 75-92 specify a first-class strategy-authoring application — visual or structured authoring interface, draft versioning, side-by-side diff, configurable peer-review and approval workflow, deployment to non-production environments, one-click rollback, complete version lineage, audit logging of every lifecycle event, pre-configured strategy templates. This is materially beyond a DMN sidecar.

**Implication:** Both Option B and Option C must add a DCMS-owned **strategy-authoring service** with its own data model (strategy, version, draft, approval), lifecycle, environment-promotion mechanism, and review workflow. Drools remains as the runtime evaluator; the authoring service deploys versioned strategies into the runtime. Under Option B-Demo, this service is delivered at demo depth (parameter form over pre-baked DRL templates with mocked approval workflow) — its biggest exposure if probed by evaluators. Under full Option B and Option C, this service is a major delivery component.

**Does this lever flip the recommendation?** No. The strategy-authoring service is required under Option B and Option C. Option A is already excluded from Release 1 production architecture per R2. The lever now informs *scope and Release 1 depth* of the strategy-authoring service, not option choice.

### Lever 3: ~~DWP scope confirmation on champion/challenger and IE engine~~ — CLOSED (2026-05-01)

**Status:** CLOSED per Release 1 review R1. `docs/release/release-1-capabilities.md` confirms champion/challenger and Income & Expenditure as first-class Release 1 capabilities (lines 94-107 and 188-196 respectively). The "flip B → A" branch is unavailable. This lever is retained as a historical record of the option-set construction and is no longer a live decision lever.

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
| ~~Champion/challenger and IE engine: are these in scope for initial delivery?~~ **CLOSED 2026-05-01 per R1** | Solution Architect → DWP client | Option selection (flips B → A if both descoped) |
| DDE-OQ-12 / DDE-OQ-13: Champion/challenger thresholds and vulnerable-customer policy | Delivery Lead → DWP client | ChampionChallengerService configuration detail |
| **BLOCKER 1** — Kafka command-name discrepancy: do the platform-reference 24 commands map to externally-publishable topics in integration guide §5? | Solution Architect → Solon platform team | Inter-layer contract design (Options A and B) |
| **BLOCKER 2** — Solon REST API stability guarantees for sync queries used by the BFF (account balance, active suppressions, task state) | Solution Architect → Solon platform team | Option B BFF; Option C ACL |
| **BLOCKER 3** (added by Design Critic) — MHCM `SuppressionType` behaviour: does `SuppressionExpiryJob` interpret `maximumNumberDays: 0` as "no maximum" or "already expired"? Wrong interpretation auto-expires MHCM without professional sign-off (Reg 21 breach). | Solution Architect → Solon platform team (verify by test) | MHCM detailed design under Options A and B; criminal-liability exposure |
| **COMPLIANCE CONTRADICTION** (added by Design Critic) — `suspendActiveInstancesSW` setting per DCMS `SuppressionType` must be declared and reconciled with RULING-016's gate-at-effect model. `true` halts the process (RULING-016 §3 violation); `false` makes DCMS gating service the sole effect-suppression mechanism and degrades the "Solon natively enforces 60-day ceiling" claim. | Solution Architect | Option B compliance design |
| Closed enumeration of gated effect categories (Design Critic S1) — required from RULING-005, RULING-010, RULING-011, RULING-014, RULING-016 before any DCMS BPMN authoring | Solution Architect → Domain Expert | DCMS BPMN authoring under Options A and B |
| Vulnerability-flag access path (Design Critic S2) — Data Area mirroring (with staleness window) vs. direct in-process service call. RULING-010 requires immediate reassignment; staleness window may breach. | Solution Architect | Option B vulnerability/champion-challenger interaction |
| On-lift disposition flow for queued correspondence under split Solon-transport / DCMS-classification ownership (Design Critic S4) | Solution Architect | Option B `CommunicationSuppressionService` design |
| OPA / Rego policy deployment model and Solon-upgrade survival for DCMS-specific authorisation rules (Design Critic S5) | Solution Architect → Solon platform team | All three options' authz design |
| Arrangement-creation gating allocation (Design Critic A5, RULING-014 guardrail 2) — declare layer that gates `createArrangement` against active suppressions | Solution Architect | Options A and B compliance design |
| Maven-module deployment boundary between DCMS beans and Solon `revenue-management-be-custom` beans (Design Critic A2) — same module or separate module in shared Spring context? | Solution Architect | Option A vs. Option B differentiation |

---

## Handoff Declaration

- **Completed:** Options pass on DCMS layer thickness and boundary placement, with Amplio mandated and RULING-016's gate-at-effect model as the corrected Breathing Space design framing. Platform-reality review (2026-04-30) added factual corrections, builder guardrails, MHCM/standard-moratorium split, and two blockers gating integration design lock. Design Critic review (2026-04-30) added a third blocker (MHCM `SuppressionType` expiry behaviour), a compliance contradiction (`suspendActiveInstancesSW` vs. RULING-016), and seven significant/advisory gaps the SA must resolve before lock. Release 1 capability review (2026-05-01) closed Lever 3 (champion/challenger and IE confirmed in scope), broadened Lever 2 from DMN-authoring sidecar into a first-class strategy-authoring application requirement, demoted Option A from candidate-for-Release-1 to traceability-and-contrast (R2), and added the **Option B-Demo** delivery framing — Option B's architecture delivered at Release 1 demo-depth implementation.
- **Files changed:** `docs/project-foundation/design-options/DESIGN-OPTIONS-002-layer-thickness-and-boundary.md` (created, updated 2026-04-30 with platform-reality findings, then updated 2026-04-30 with design-critic findings, then updated 2026-05-01 with Release 1 capability review findings, Option A workarounds table, and new Option B-Demo delivery framing)
- **Open blockers gating integration design lock:**
  1. Kafka command-name discrepancy between platform reference and integration guide §5 (Options A and B inter-layer contract).
  2. Solon REST API stability guarantees for sync queries (Option B BFF; Option C ACL).
  3. MHCM `SuppressionType` behaviour under `maximumNumberDays: 0` (Reg 21 / criminal-liability exposure).
  4. `suspendActiveInstancesSW` setting per DCMS `SuppressionType` reconciled with RULING-016 gate-at-effect model.
- **ACs satisfied:** Options produced (3 options, parallel structure), recommendation stated, decision levers named, Breathing Space gating service addressed per option, data ownership addressed per option, interface and dependency impact addressed, key risks per option named, open questions with owners listed.
- **ACs not satisfied:** None — this is a design artefact, not an implementation AC.
- **Assumptions made:**
  - Solon's 24-command Kafka catalogue is stable and the documented commands are the full supported set.
  - Solon's `revenue-management-be-custom` module pattern (Spring bean component scan) is available and supported in Solon v2.3.0 as described in the platform reference.
  - No new Solon BPMN reference processes outside the documented 28 are relevant to DCMS.
  - RULING-016 is final as filed; only DDE-OQ-BS-PROCESS-01 and DDE-OQ-BS-PROCESS-02 remain open within its scope.
  - Team size and delivery deadline are not constraints per ADR-018.
- **Missing inputs encountered (historical):** Java version decision (now potentially resolved by Solon Java 25 signal — see Forward-Looking Note); DMN authoring hard requirement status (now broadened into strategy-authoring application requirement per Release 1 review R3); champion/challenger and IE engine in/out-of-scope for initial delivery (CLOSED 2026-05-01 per R1 — both confirmed in scope by `release-1-capabilities.md`).
- **Next role:** Solution Architect (Design Critic + Release 1 capability review passes complete).
- **What they need:** This document. RULING-005, RULING-010, RULING-011, RULING-014, RULING-016. `external_sys_docs/solon/solon_tax_2.3.0_integration_guide.md` §5 and `KAFKA_EVENT_CATALOG.md` for Blocker 1. `external_sys_docs/solon/api-suppression-management.md` for the C3 compliance contradiction and Blocker 3. `solon-tax-platform-reference.md` and `amplio-process-engine-reference.md` for context. The four blockers above must be resolved before the SA locks an option; the three decision levers (Java version, DMN authoring, champion/challenger scope) remain the live option-flipping levers.

---

`[BLOCKING] Design Critic Review Complete (2026-04-30)` — four blockers and one compliance contradiction filed against this document. Solution Architect must resolve Blockers 1, 2, 3 and the `suspendActiveInstancesSW` contradiction before locking an option.

`[BLOCKING] Release 1 Capability Review Complete (2026-05-01)` — Lever 3 closed (champion/challenger and IE confirmed in scope by `docs/release/release-1-capabilities.md`); Lever 2 broadened from "DMN authoring" to first-class strategy-authoring application; Option A demoted from candidate-for-Release-1 to traceability-and-contrast (R2); new **Option B-Demo** delivery framing added pairing Option B architecture with Release 1 demo-depth implementation. Solution Architect must confirm Option B-Demo as the recommended Release 1 delivery framing or call out specific service depth concerns (most likely strategy authoring).

