# DESIGN-OPTIONS-002: Layer Thickness and Boundary Placement

**Document ID:** DESIGN-OPTIONS-002
**Date:** 2026-04-30
**Status:** PROPOSED — Design Critic review complete (2026-04-30); awaiting Solution Architect lock. **Integration design lock BLOCKED** pending resolution of three platform-reality questions and the compliance contradiction surfaced by the Design Critic (see "Platform-Reality Review Findings" and "Design Critic Review Findings" below).
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

## Platform-Reality Review Findings (added 2026-04-30, post solon-tax-platform-expert review)

A platform-expert review against `external_sys_docs/solon/solon_tax_2.3.0_integration_guide.md`, `solon_tax_2.3.0_operations_guide.md`, `api-suppression-management.md`, and the Amplio reference identified factual corrections, missing constraints, and two blockers that must be resolved before integration design is locked. The recommendation (Option B) is unchanged but is conditional on Blocker 1.

### Blockers (must resolve before integration design lock)

| # | Blocker | Owner | Blocks | Resolution path |
|---|---|---|---|---|
| 1 | **Kafka command-name discrepancy.** The "24-command catalogue" cited in the platform reference uses abstracted financial-primitive names (`PostPaymentCommand`, `WriteOffDebtCommand`, `CreatePaymentPlanCommand`, `SendCorrespondenceCommand`, etc.). The integration guide §5 catalogue is dominated by engine-internal `irm.bpmn-engine.*` commands (`CreateHumanTaskCommand`, `TriggerDebtRecoveryProcessCommand`, `SuspendCaseActivityCommand`). It is not confirmed that the abstracted names are externally-publishable Kafka commands. Options A and B treat the 24-command catalogue as the primary inter-layer contract — if the names do not map to externally-triggerable topics, that contract model collapses. | Solution Architect → Solon codebase inspection | Inter-layer contract design under Options A and B | **Resolved by reading the Solon codebase.** Inspect the Kafka producer configuration for actual topic name strings; inspect the `revenue-management-be-custom` module to see how existing extensions publish commands. If the abstracted commands are not externally publishable, redesign the inter-layer contract around the actual publishable surface found in the codebase. |
| 2 | **Solon REST API stability for sync queries.** The integration guide's synchronous HTTP section is thin (only `ExecuteKieRulesCommand` is documented as a sync API). Option A's BFF assumes Solon REST APIs (`account balance`, `active suppressions`, `task state`) are a stable external contract. Their stability guarantees under Solon upgrades are not documented. | Solution Architect → Solon codebase inspection + Solon platform team | Option A BFF endpoints; Option B BFF layer; Option C ACL contract | **Partially resolved by reading the Solon codebase.** Codebase inspection confirms which endpoints exist and what they return. Stability guarantee (whether the Solon team treats these as a versioned external contract) still requires a governance commitment from the Solon platform team. Go into that conversation with a specific list of endpoints derived from codebase inspection. |

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

**S3 resolution path (updated 2026-05-01):** Solon codebase read access resolves the verification question. Read `SuppressionExpiryJob` source directly to determine which conditional branch it enters when `maximumNumberDays: 0`. If it interprets zero as "already expired" (worst case), the mitigation is: never configure an MHCM `SuppressionType` with `maximumNumberDays: 0`. Instead set `maximumNumberDays: 36500` (100 years as a practical ceiling) and `overrideEndDateSW: true`, so that `MHCM_RELEASE_PROCESS.bpmn` can set an explicit end date on professional sign-off. The DCMS-owned release path — professional sign-off → 30-day post-treatment tail calculation → `ReleaseSuppressionCommand` with calculated expiry date → Solon `SuppressionExpiryJob` handles cleanup — becomes the sole mechanism for MHCM suppression end. This is documented as a known operational risk (configuration workaround, not a clean semantic design) and must be recorded in the MHCM operational runbook.

**S4. Split correspondence ownership under Option B leaves on-lift disposition of queued communications unspecified.**
RULING-011 requires `DEBT_COLLECTION` communications queued during statutory suppression to be discarded on lift, while those queued during internal policy suppression are held for agent review. Under Option B, transport runs through Solon's `CORRESPONDENCE_PROCESS.bpmn` (Solon owns the queued correspondence entries) but category evaluation and suppression-reason classification live in DCMS's `CommunicationSuppressionService`. The flow on suppression lift — how DCMS inspects Solon's queued correspondence, classifies each entry by the suppression reason that caused it to be queued, and instructs Solon to discard or hold — is not described. The suppression API exposes status, not correspondence queues. Options A and C have cleaner ownership stories on this point (A: pre-dispatch guard inside Solon's process; C: DCMS owns transport too). Option B's split ownership is a structural coupling problem that must be designed out before lock.

**S5. The OPA / Rego policy boundary for DCMS-specific access rules is unspecified across all three options.**
DCMS-specific authorisation rules — `OPS_MANAGER`-only champion/challenger policy modification (RULING-010 guardrail 2), vulnerability data restricted to assigned agents, MHCM status as Restricted under RULING-002 / GDPR Art. 9 (RULING-016 §5) — require DCMS-specific Rego policies. The doc says "DWP realm, OPA Rego policies for DCMS-specific access rules" without specifying whether DCMS Rego policies deploy alongside Solon's policies in the same OPA instance, into a separate sidecar, or inline. If co-located: a Solon upgrade modifying the OPA policy bundle could overwrite DCMS policies. If separate: policy evaluation order, conflict resolution, and request flow must be designed. For a system handling vulnerability and MHCM data, an unspecified authz policy boundary is a likely ICO finding.

### Advisory findings

- **A1. Option A's "fastest" rating in the Tradeoff Summary is contingent but the BPMN modification risk has been resolved.** The original framing assumed Solon's reference BPMN processes would be modified. **This has been superseded**: the confirmed approach is to author all DWP-specific BPMN process definitions from scratch (`dcms-intake-and-first-contact.bpmn`, `dcms-vulnerability-to-resolution.bpmn`, `dcms-payment-monitoring.bpmn`, `dcms-breach-to-placement.bpmn`, `dcms-strategy-review.bpmn`) and deploy them into Amplio alongside Solon's unmodified reference processes. This dissolves the "BPMN modification scope underestimated" risk. The "fastest" rating now depends on Amplio authoring effort for the five new process definitions — Flow 3 (`dcms-breach-to-placement.bpmn`) is the highest-complexity process; Flows 4 and 6 add no BPMN authoring effort at all.
- **A2. Options A and B may collapse to the same architecture in practice.** Both use `revenue-management-be-custom`, run in the same JVM, use the same Kafka topics and Amplio runtime, and share Blocker 1. The stated difference is logical schema separation under B (`dcms` schema vs. shared) — not physical isolation. The SA should declare whether DCMS's beans live in the same Maven module as Solon's `revenue-management-be-custom` beans or in a separate Maven module participating in the same Spring context. If the former, A and B's blast radius and coupling are essentially the same.
- **A3. Java 25 bytecode compatibility ≠ dependency compatibility.** The Forward-Looking Note correctly states JVM-level backward compatibility but does not address whether Solon's transitive dependencies (Drools KIE, Liquibase, Hazelcast) have been tested against Java 25. JVM compatibility is necessary but not sufficient.
- **A4. The recommendation's flip-to-C condition is inconsistent with Lever 1.** The recommendation states the flip requires "Java 21 mandate + Flyway in the DCMS custom tier on a shared database schema." Lever 1 says Java 21 alone flips B to C (because in-process model requires single JVM). One of these statements is wrong. The recommendation summary should be reconciled with Lever 1 before lock.
- **A5. Arrangement-creation gating under RULING-014 is not allocated to any layer in any option.** RULING-014 guardrail 2 requires `createArrangement` to query the live suppression log (not a cached process variable) and reject if a Breathing Space or insolvency suppression is active on the Person. Under Options A and B, the arrangement entry point — whether BPMN start event, REST endpoint, or Spring service method — is not declared as a gate-call site in any of the option designs. This is a statutory requirement; it must be explicitly allocated.

### New blocker (added by Design Critic)

| # | Blocker | Owner | Blocks | Resolution path |
|---|---|---|---|---|
| 3 | **MHCM `SuppressionType` behaviour under `maximumNumberDays: 0`.** Whether `SuppressionExpiryJob` interprets this value as "no maximum" or "already expired" is undocumented. The wrong interpretation auto-expires MHCM suppressions without professional sign-off, breaching Reg 21 of the Debt Respite Scheme Regulations 2020. Cannot be inferred from available source docs and cannot be left to detailed design. | Solution Architect → Solon codebase inspection | MHCM detailed design under Options A and B; criminal-liability exposure | **Resolved by reading the Solon codebase.** Inspect `SuppressionExpiryJob` source to determine the conditional logic for `maximumNumberDays: 0`. If the "already expired" interpretation is confirmed, the mitigation is: set `maximumNumberDays: 36500` and `overrideEndDateSW: true` on the MHCM `SuppressionType`; make `MHCM_RELEASE_PROCESS.bpmn` the sole release path via `ReleaseSuppressionCommand` with a calculated expiry date. Document as a known operational risk (configuration workaround). If the "no maximum" interpretation is confirmed, `maximumNumberDays: 0` is safe and no workaround is needed. |

### Compliance contradiction requiring resolution

**The `suspendActiveInstancesSW` setting versus RULING-016's gate-at-effect model (C3) is the single most consequential issue raised by this review.** It is not a detail that can be resolved in builder handoff. The SA must explicitly choose, per `SuppressionType`, whether Solon auto-suspends BPMN tokens on suppression creation, and the choice must be reconciled with RULING-016 guardrails before the Option B design is locked.

### Required actions before SA lock

1. Resolve **C1** (Blocker 1) as a complete Kafka contract verification — names, payloads, response events, isolation guarantees.
2. Resolve **C3** by declaring `suspendActiveInstancesSW` per DCMS suppression type and reconciling with RULING-016.
3. Resolve **S3** (Blocker 3 above) — verify `SuppressionExpiryJob` behaviour against `maximumNumberDays: 0` by reading the Solon codebase. If the "already expired" interpretation is confirmed, apply the `maximumNumberDays: 36500` + `overrideEndDateSW: true` + DCMS-owned release path mitigation and document as an operational risk.
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

## Context: What RULING-016 Changes for This Design Pass

The prior framing — "how do we implement a non-interrupting boundary event for Breathing Space when Amplio only supports interrupting" — assumed we needed to freeze the Amplio process engine. RULING-016 (30 April 2026) rules that the Debt Respite Scheme Regulations 2020 are effect-enumerated, not process-prohibiting. Amplio can continue advancing moratorium-flagged cases through internal state transitions. The compliance obligation is a gate placed at every step that could produce a prohibited debtor-facing effect (collection contact, enforcement instruction, deduction-from-benefit instruction, DCA instruction, interest accrual). This is a gating service question, not a process-freeze question.

Two open DWP sign-off questions remain (DDE-OQ-BS-PROCESS-01 and DDE-OQ-BS-PROCESS-02) but neither blocks the options pass. Both affect internal detail of the gating service, not its existence or placement.

---

## Option A: Minimal Custom Layer — Configure and Extend Solon

### Layer thickness

Thin. DCMS is a set of Solon extensions: Drools rule additions in the custom KIE container, Data Area JSONB extensions on Solon entities, a small number of custom Spring beans registered via component scan in `revenue-management-be-custom`, new DWP-specific BPMN process definitions deployed into the shared Amplio engine, and a new React UI calling Solon's existing REST APIs. Solon's reference BPMN processes are **not modified**; instead, DWP-specific processes are authored from scratch as new BPMN definitions and deployed into Amplio alongside Solon's.

### BPMN approach under Option A

DWP-specific collection workflows are new BPMN process definitions authored by the DCMS team and deployed into Amplio — not forks or modifications of Solon's tax-authority reference processes (`VAT_RETURN.bpmn`, `BUS_REG.bpmn`, etc.). This decision was taken because Solon's reference processes are tax-authority-named and do not map cleanly to benefit debt without substantial rework; authoring from scratch is faster and removes the Solon-upgrade re-validation burden on modified reference processes.

The six demo flows map to the following new BPMN process definitions:

| Process definition | Demo flow | Amplio complexity |
|---|---|---|
| `dcms-intake-and-first-contact.bpmn` | Flow 1 — Intake to first contact | Low — linear sequence, no parallel gateways |
| `dcms-vulnerability-to-resolution.bpmn` | Flow 2 — Vulnerability to resolution | Medium — suppression atomicity; child process spawn for payment monitoring |
| `dcms-payment-monitoring.bpmn` | Flows 2 and 3 — spawned on arrangement creation | Low — timer + REST poll pattern |
| `dcms-breach-to-placement.bpmn` | Flow 3 — Breach to third-party placement | High — PTP loop, supervisor branching, inter-process message correlation |
| `dcms-strategy-review.bpmn` | Flow 5 — Strategy change approval workflow | Low — three-to-five task sequence |

Flows 4 (complex household) and 6 (executive dashboard) require no new BPMN processes. Flow 4's restriction enforcement is a pre-execution guard Spring bean called from within service task delegates; Flow 6 is a pure read path against Solon REST APIs.

**Solon reference processes used as read-only design reference only.** No Solon reference process is deployed with DCMS modifications. `DEBT_RECOVERY_PROCESS.bpmn`, `CASE_HANDOVER_COLLECTION_AGENCY_PROCESS.bpmn`, `SUSPEND_PROCESS_CHECK.bpmn`, and `PAYMENT_DEFERRAL.bpmn` are studied for Amplio variable scoping patterns and Kafka command usage, but DCMS deploys its own equivalent processes.

**Amplio authoring constraints (builder guardrails — apply to all DCMS BPMN):**

1. All gateways are exclusive. Parallel Gateway branches execute sequentially in Amplio; do not design for concurrent execution.
2. Script Tasks are FEEL-only. No Groovy or JavaScript. All imperative logic goes in Java delegate classes.
3. Business Rule Tasks must be Service Tasks wired to Drools KIE (`POST /drools-runtime/execute-stateless/{containerId}`). DMN is not supported.
4. Compensation events are not implemented. Error boundary events routing to compensation logic are the pattern.
5. Use only Kafka topics confirmed in integration guide §5 — subject to Blocker 1.

### Boundary placement

| Responsibility | Lives in Solon | Lives in DCMS custom layer |
|---|---|---|
| Case lifecycle BPMN | Solon reference processes (unmodified, used as design reference) | New DWP BPMN process definitions deployed into Amplio |
| Financial ledger, payment, write-off | Solon core | Calls via Solon Kafka commands |
| Debt recovery workflow | Solon DEBT_RECOVERY_PROCESS.bpmn (unmodified) | `dcms-intake-and-first-contact.bpmn`, `dcms-breach-to-placement.bpmn` |
| DCA handover | Solon CASE_HANDOVER_COLLECTION_AGENCY_PROCESS.bpmn (called as sub-process if needed) | DWP-specific pre-placement disclosure gate in `dcms-breach-to-placement.bpmn` |
| Business rules (segmentation, strategy) | Drools KIE (DWP rules added to custom KIE module) | — |
| Suppression model | Solon CreateSuppression / ReleaseSuppression | Thin wrapper Spring bean |
| Breathing Space gating service | Near-Solon: a Spring bean in the custom layer calling Solon's suppression query | Called from within DCMS BPMN service task delegates |
| Champion/challenger | Not present in Solon — new custom Spring bean | Champion/challenger assignment service |
| Income and expenditure | Not present in Solon — new custom Spring bean | IE capture service |
| Vulnerability governance | Not present in Solon — custom Spring bean | VulnerabilityClassificationService |
| Communication suppression | Solon CORRESPONDENCE_PROCESS.bpmn for transport | CommunicationSuppressionService bean — pre-dispatch guard |
| UI | — | New React app — three-workspace model (Case Worker, Operations, Configuration); calls Solon REST APIs + thin custom BFF endpoints |
| Auth | Solon Keycloak + OPA | DWP realm configuration |

**Breathing Space gating service location under Option A:** Spring bean in the DCMS custom layer (`revenue-management-be-custom`), in-process with Solon, registered via component scan. Called as a pre-execution guard within service task Java delegate overrides. Solon's existing suppression query (`CreateSuppression` / active suppression check) is the underlying source of truth. The gating bean wraps this check and enforces RULING-016 guardrail 2 (check at point of effect, not at case intake). The interrupting-vs-non-interrupting constraint is dissolved by RULING-016: Amplio does not need to non-interrupt; the gate stops the prohibited effect before it exits the service task.

**Data ownership under Option A:** DCMS extends Solon entities almost entirely through Data Area JSONB. DWP-specific fields (vulnerability flags, breathing space hold type, IE assessment reference, champion/challenger variant, DWP debt type, deduction-from-benefit flag) land in the `data_area` column of Solon's Taxpayer, Case, Suppression, and Task entities. One additional table is introduced: `dcms_strategy_version` — a narrow table in Solon's PostgreSQL schema used for strategy versioning, peer review state, and rollback (see Strategy Versioning section below). The champion/challenger assignment log is a second new table, needed because Solon has no analogue.

**Contract between layers:** In-process Spring beans. No HTTP boundary. The DCMS custom layer is a Maven module (`revenue-management-be-custom`) that runs inside the same JVM as Solon. Solon's Kafka command catalogue is used for all async operations (CreateSuppressionCommand, PostPaymentCommand, SendCorrespondenceCommand, etc.).

### UI under Option A

The user interface is a new, custom-built React application following the three-workspace model: Case Worker Workspace, Operations Workspace, and Configuration Workspace. The three-workspace model is a UI-layer decision independent of backend architecture option — it exists under Option A exactly as described in [`../three-workspace-model.md`](../three-workspace-model.md).

Under Option A the UI is thinner and more constrained than under Option B or C, because it calls Solon REST APIs directly (with a thin BFF for DCMS-specific endpoints) rather than a rich aggregation layer.

#### What the UI can do under Option A

| Capability | How it works |
|---|---|
| Case Worker Workspace — case screen, account timeline, linked accounts | BFF queries Solon Case Management + Registration REST APIs; rendered in React |
| Case Worker Workspace — ID&V panel, vulnerability capture form, I&E form | Custom DCMS endpoints in `revenue-management-be-custom`; data written to Solon Data Area JSONB |
| Case Worker Workspace — arrangement creation and payment schedule display | BFF calls Solon Taxpayer Accounting REST API; arrangement written via Kafka `CreatePaymentPlanCommand` |
| Case Worker Workspace — restriction flag display and runtime action blocking | Pre-execution guard Spring bean; UI shows explicit block alert when guard fires |
| Case Worker Workspace — transfer with full context carried forward | Solon Human Task Management API — reassign task; notes and suppression state carried on Solon entities |
| Operations Workspace — queue volumes, SLA health, breach count | BFF queries Solon Human Task Management (task counts) and Case Management (case stage counts) |
| Operations Workspace — bulk reassignment | BFF calls Solon Human Task Management — reassign task; writes audit event |
| Operations Workspace — vulnerability exception report | BFF queries Solon Suppression Management — active suppression counts and flag age |
| Operations Workspace — third-party placement performance | BFF reads from DCMS JSONB extension on placement case entities |
| Configuration Workspace — strategy version authoring, diff, peer review, approve, rollback | Custom DCMS endpoints reading and writing `dcms_strategy_version` table |
| Configuration Workspace — segmentation threshold editing | Custom DCMS endpoints; parameter changes write to Drools KIE container configuration JSONB |
| Configuration Workspace — vulnerability type reference list | Solon Reference Data API (codelists) |
| Configuration Workspace — RBAC and user management | Keycloak admin API, called via custom DCMS BFF endpoint |
| Champion/challenger configuration and results view | Custom DCMS endpoints; results read from `champion_challenger_assignment` table and DCMS JSONB |
| Historical simulation results view | Pre-computed simulation results stored in JSONB; rendered as static comparison view |

#### What the UI cannot do under Option A (hard constraints)

| Capability | Reason |
|---|---|
| Live champion/challenger results from real operational data pipeline | No analytics pipeline in Option A. C/C results are seeded from pre-computed historical data (Gap 1 workaround A4). A real-time operational pipeline is out of scope for Release 1 under Option A. |
| Fully interactive historical simulation against live process replay | No sandbox process engine. Simulation results are pre-computed from a historical snapshot (Gap 2 workaround A3) — not live replay of selected accounts through a proposed strategy. |
| Non-technical strategy authoring for new rule types | Structural Drools DRL changes require a developer. The UI provides a parameterised template library where business users can adjust parameters of existing rule templates. New rule types are developer-gated (Gap 3 hybrid workaround A2/A3). |
| Strategy diff across arbitrary version pairs without developer involvement | Strategy diff is visible in the UI for consecutive versions held in `dcms_strategy_version`. Comparing non-consecutive versions or rendering complex structural diffs may require developer tooling (Gap 4 constraint). |
| UI shape fully decoupled from Solon API response structure | The BFF under Option A is thin — mostly a proxy. If Solon's REST API for a given entity does not expose a field DCMS needs, a custom endpoint must be added to `revenue-management-be-custom`. API gaps are discovered iteratively and resolved flow by flow. |
| Dashboard drill-down backed by a separate analytics database | No dedicated analytics or operational DB under Option A. All dashboard data is read from Solon REST APIs and DCMS JSONB at query time. Complex aggregations (time-series for breach trend, segment performance over time) may require materialised views or a small read-model table, added incrementally as gaps surface. |

#### Three-workspace model under Option A

All three workspaces are present and navigable under Option A. The shared product shell (single SPA, Keycloak-backed session, role-based workspace routing) is unchanged. What changes relative to the full Option B vision is the data surface behind each workspace:

- **Case Worker Workspace** — fully functional. All six demo flows that touch case worker actions (Flows 1–4) are served from Solon REST + DCMS JSONB with no structural gaps.
- **Operations Workspace** — functional for live queue, SLA, and breach data. Dashboard read performance is the main risk — aggregation queries against Solon REST under concurrent dashboard loads must be validated early (Blocker 2).
- **Configuration Workspace** — functional for parameterised strategy editing, version management, peer review, and RBAC. Non-technical authoring of new rule types is not possible without developer involvement.

### Consequences for delivery

- BPMN authoring scope is bounded and predictable: five new process definitions for six demo flows. Flows 4 and 6 add no BPMN authoring effort. Flow 3 (`dcms-breach-to-placement.bpmn`) is the highest-complexity process and must be designed carefully before build begins.
- No Solon reference BPMN process is modified, which eliminates the Solon-upgrade re-validation burden on modified reference processes. DCMS processes are new definitions and are not affected by Solon's reference process changes.
- Solon-internals learning curve is still present but narrower than originally framed: the team must understand Amplio's deployment model, Java delegate patterns, and variable scoping — but not the internals of Solon's tax-authority process definitions.
- Solon upgrades carry moderate blast radius: DCMS processes call Solon via Kafka commands and REST APIs; only those surface areas need re-validation. Solon reference process changes do not affect DCMS processes.
- Champion/challenger, IE engine, and vulnerability governance are structurally awkward: they are significant domain services that have no Solon scaffold, yet they live inside the Solon custom extension module, constrained by Solon's entity model and lifecycle.
- Non-technical DMN authoring (ADR-008 requirement, UNDER REVIEW) cannot be met. Drools DRL requires developer involvement for new rule types. Parameterised template editing is the Release 1 workaround. This must be re-confirmed as acceptable or resolved before Option A can be locked.
- DWP UX is constrained by what Solon's REST APIs expose. API gaps require custom endpoints added to the custom layer; these are discovered iteratively as UI screens are built against actual Solon API responses.

### Strategy versioning under Option A

Strategy version state is held in a narrow `dcms_strategy_version` table co-located in Solon's PostgreSQL schema:

```
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

The Configuration Workspace UI reads and writes this table via custom DCMS BFF endpoints. The peer review approval workflow runs as `dcms-strategy-review.bpmn` in Amplio. Rollback is a status update on the target prior version plus a new `live` row. All strategy lifecycle events are written to the DCMS audit trail.

### Risks unique to Option A

| Risk | Severity | Mitigation |
|---|---|---|
| BPMN authoring effort for Flow 3 (`dcms-breach-to-placement.bpmn`) is underestimated — PTP loop, supervisor branching, and inter-process message correlation are the highest-complexity BPMN in the six flows | High | Design `dcms-breach-to-placement.bpmn` and `dcms-payment-monitoring.bpmn` in full before build sprint begins; timebox two days per process for design and Amplio constraint validation |
| Blocker 1 (Kafka topic names) blocks `dcms-intake-and-first-contact.bpmn` start event — if intake trigger topic is not externally publishable, the intake process cannot be triggered via Kafka | High | Resolve Blocker 1 before authoring begins; use REST polling as a fallback trigger if Kafka topics are not available |
| Solon REST API performance under dashboard query load — Operations Workspace aggregates multiple REST endpoints at query time with no analytics DB buffer | Medium | Early performance spike against Solon REST APIs for queue count, case count, and suppression count queries; add read-model tables incrementally if latency is unacceptable |
| Drools rule authoring lock-in — DWP policy team cannot make new rule types without a developer | High | Accept as a constraint for Release 1; parameterised template library is the workaround; document as a known constraint with DWP sign-off |
| Solon Data Area JSONB fields are schema-less — querying them for reporting requires jsonb operators; complex queries degrade | Medium | Materialise DWP-specific fields into a read-model from day one for any field queried in dashboard aggregations |
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
| Risk profile | Solon coupling, Drools governance, Flow 3 BPMN authoring complexity | Integration contract, dual-write | Rebuild scope, financial state divergence |

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
2. **Flow 3 BPMN authoring complexity underestimated** — `dcms-breach-to-placement.bpmn` contains the most complex process logic (PTP loop, supervisor branching, inter-process message correlation). Mitigation: design and Amplio-constraint-validate this process definition before the build sprint begins; timebox two days for design. Note: the original "Solon BPMN modification scope" risk is dissolved — DWP BPMN processes are authored from scratch, not modified Solon reference processes.
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
| **BLOCKER 1** — Kafka command-name discrepancy: do the platform-reference 24 commands map to externally-publishable topics in integration guide §5? | Solution Architect → Solon platform team | Inter-layer contract design (Options A and B) |
| **BLOCKER 2** — Solon REST API stability guarantees for sync queries used by the BFF (account balance, active suppressions, task state) | Solution Architect → Solon platform team | Option B BFF; Option C ACL |
| **BLOCKER 3** (added by Design Critic) — MHCM `SuppressionType` behaviour: does `SuppressionExpiryJob` interpret `maximumNumberDays: 0` as "no maximum" or "already expired"? Wrong interpretation auto-expires MHCM without professional sign-off (Reg 21 breach). **Resolution path:** Read `SuppressionExpiryJob` source from Solon codebase. If "already expired" is confirmed, mitigation is `maximumNumberDays: 36500` + `overrideEndDateSW: true` + DCMS-owned release path via `ReleaseSuppressionCommand`. | Solution Architect → Solon codebase inspection | MHCM detailed design under Options A and B; criminal-liability exposure |
| **COMPLIANCE CONTRADICTION** (added by Design Critic) — `suspendActiveInstancesSW` setting per DCMS `SuppressionType` must be declared and reconciled with RULING-016's gate-at-effect model. `true` halts the process (RULING-016 §3 violation); `false` makes DCMS gating service the sole effect-suppression mechanism and degrades the "Solon natively enforces 60-day ceiling" claim. | Solution Architect | Option B compliance design |
| Closed enumeration of gated effect categories (Design Critic S1) — required from RULING-005, RULING-010, RULING-011, RULING-014, RULING-016 before any DCMS BPMN authoring | Solution Architect → Domain Expert | DCMS BPMN authoring under Options A and B |
| Vulnerability-flag access path (Design Critic S2) — Data Area mirroring (with staleness window) vs. direct in-process service call. RULING-010 requires immediate reassignment; staleness window may breach. | Solution Architect | Option B vulnerability/champion-challenger interaction |
| On-lift disposition flow for queued correspondence under split Solon-transport / DCMS-classification ownership (Design Critic S4) | Solution Architect | Option B `CommunicationSuppressionService` design |
| OPA / Rego policy deployment model and Solon-upgrade survival for DCMS-specific authorisation rules (Design Critic S5) | Solution Architect → Solon platform team | All three options' authz design |
| Arrangement-creation gating allocation (Design Critic A5, RULING-014 guardrail 2) — declare layer that gates `createArrangement` against active suppressions | Solution Architect | Options A and B compliance design |
| Maven-module deployment boundary between DCMS beans and Solon `revenue-management-be-custom` beans (Design Critic A2) — same module or separate module in shared Spring context? | Solution Architect | Option A vs. Option B differentiation |

---

## Handoff Declaration

- **Completed:** Options pass on DCMS layer thickness and boundary placement, with Amplio mandated and RULING-016's gate-at-effect model as the corrected Breathing Space design framing. Platform-reality review (2026-04-30) added factual corrections, builder guardrails, MHCM/standard-moratorium split, and two blockers gating integration design lock. Design Critic review (2026-04-30) added a third blocker (MHCM `SuppressionType` expiry behaviour), a compliance contradiction (`suspendActiveInstancesSW` vs. RULING-016), and seven significant/advisory gaps the SA must resolve before lock.
- **Files changed:** `docs/project-foundation/design-options/DESIGN-OPTIONS-002-layer-thickness-and-boundary.md` (created, updated 2026-04-30 with platform-reality findings, then updated 2026-04-30 with design-critic findings)
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
- **Missing inputs encountered:** Java version decision (critical — flips option if Java 21 required); DMN authoring hard requirement status; champion/challenger and IE engine in/out-of-scope for initial delivery.
- **Next role:** Solution Architect (Design Critic pass complete).
- **What they need:** This document. RULING-005, RULING-010, RULING-011, RULING-014, RULING-016. `external_sys_docs/solon/solon_tax_2.3.0_integration_guide.md` §5 and `KAFKA_EVENT_CATALOG.md` for Blocker 1. `external_sys_docs/solon/api-suppression-management.md` for the C3 compliance contradiction and Blocker 3. `solon-tax-platform-reference.md` and `amplio-process-engine-reference.md` for context. The four blockers above must be resolved before the SA locks an option; the three decision levers (Java version, DMN authoring, champion/challenger scope) remain the live option-flipping levers.

---

`[BLOCKING] Design Critic Review Complete (2026-04-30)` — four blockers and one compliance contradiction filed against this document. Solution Architect must resolve Blockers 1, 2, 3 and the `suspendActiveInstancesSW` contradiction before locking an option.
