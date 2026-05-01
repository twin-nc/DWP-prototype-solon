# ADR-019: DCMS Code Boundary — Application Layer vs Solon `revenue-management-be-custom` Extensions

## Status

**PROPOSED** — for Solution Architect lock. Required as a precondition to locking DESIGN-OPTIONS-003 (decomposition strategy for the DCMS application layer).

## Date

2026-05-01

---

## Context

ADR-018 confirmed Solon Tax v2.3.0 as the base platform. DCMS-authored code can live in two architecturally distinct locations on that platform:

- **(a) Inside Solon Tax services**, via the `revenue-management-be-custom` extension repository. This is Solon's documented mechanism for client-specific backend extensions that run inside Solon's own service boundary, share Solon's transactions, and deploy on Solon's release cadence.
- **(b) Outside Solon Tax**, as a separate DCMS application (or applications) that calls Solon over Kafka and REST. This is the layer addressed by DESIGN-OPTIONS-003.

The two locations are *not* interchangeable. They differ in:

| Property | (a) `revenue-management-be-custom` | (b) DCMS application |
|---|---|---|
| Process / JVM | Inside a Solon Tax microservice | Separate application |
| Transactional scope | Participates in Solon transactions and BPMN activities | Outside Solon transactions; calls Solon as a client |
| Release cadence | Solon Tax release cadence | DCMS release cadence |
| Deployment ownership | Solon-side artefacts, Solon-side ops | DCMS-side artefacts, DCMS-side ops |
| Data access | Direct access to Solon-owned aggregates | API/Kafka access only |
| Failure-mode coupling | Tight — extension defects can affect Solon-owned behaviour | Loose — failures isolated to DCMS application |
| Platform-version coupling | Must be re-validated on every Solon Tax upgrade | Coupled only to Solon's documented contract surface |
| Audit / traceability | Audit trail follows Solon's conventions | Audit trail in DCMS-owned stores |

DESIGN-OPTIONS-003 addresses (b) only. Without an explicit boundary between (a) and (b), builders will place compliance-bound or DCMS-owned logic on the wrong side. The cost of misplacement is high: code in (a) that should be in (b) becomes coupled to Solon's release cadence and harder to evolve under DWP regulatory change; code in (b) that should be in (a) introduces chatty cross-boundary calls, distributed-transaction problems, and cache-coherence questions on Solon-owned aggregates.

This ADR establishes the boundary and the decision criteria.

---

## Decision

**The default location for DCMS-authored code is (b) — the DCMS application layer. Code may only be implemented in (a) — Solon's `revenue-management-be-custom` extension layer — when *all four* placement criteria below are satisfied AND no exclusion criterion applies.**

### Placement criteria for (a) `revenue-management-be-custom`

To place DCMS logic inside Solon, **all four** must hold:

1. **Solon-owned aggregate dependency.** The logic operates primarily on Solon-owned aggregates — ledger entries, payment allocations, suppression records, write-off records, Solon task tray items, Solon BPMN process variables — and would otherwise require chatty cross-boundary calls to read or mutate them.
2. **Transactional necessity.** The logic must execute synchronously within a Solon transaction or BPMN activity to maintain a Solon-side invariant (e.g., an allocation rule that must hold before a payment row is committed; a Solon BPMN service-task implementation).
3. **No DCMS-only state dependency.** The logic does not depend on state, configuration, or workflow that lives only in DCMS (DCMS strategy bundles, vulnerability protocol state, queue/agent assignment, contact orchestration history, workspace-specific data).
4. **No DCMS-only release cadence requirement.** The logic does not need to be released, version-controlled, or rolled back independently of Solon Tax's release cycle.

### Exclusion criteria

Even when the four placement criteria are met, the logic **must remain in (b)** if any of the following apply:

- **E1. Statutory regulatory invariant.** The logic implements RULING-010 (champion/challenger reassignment), RULING-014 (arrangement creation suppression check), RULING-016 (Breathing Space gating), or any future ruling whose implementation must be testable, auditable, and version-controllable independently of Solon's release cadence.
- **E2. Multi-system span.** The logic depends on non-Solon integrations (DCA, dialler/CTI, channel adapters not owned by Solon, bureau feeds).
- **E3. Debtor-facing experience.** The logic is part of Case Worker workspace, Operations workspace, or any configuration workspace presentation, validation, or workflow.
- **E4. DCMS-owned domain state.** The logic owns or coordinates DCMS domain state (strategy decisions, vulnerability protocol state, queue/agent assignment, contact orchestration, communications dispatch coordination).
- **E5. Compliance-coupled with logic that is in (b).** The logic forms part of a compliance gate evaluation chain whose other steps are in (b). Splitting a gate across the boundary is forbidden — see "Forbidden patterns" below.

### Default rule

Where placement is ambiguous, the logic is implemented in (b). Moving logic from (b) into (a) later is an additive change; moving logic from (a) into (b) requires unwinding Solon-side coupling and is more expensive.

---

## Forbidden patterns

The following patterns are explicitly prohibited regardless of how the criteria are read:

1. **Splitting a single regulatory gate across (a) and (b).** A RULING-010, RULING-014, or RULING-016 gate evaluation must execute atomically on one side. The DCMS application coordinates *invocation* of the gate at debtor-facing effects; the gate's evaluation logic does not straddle the boundary.
2. **Placing DCMS strategy decisioning in (a).** Strategy bundles, champion/challenger experimentation, segmentation, and treatment-path selection are DCMS domain state and remain in (b).
3. **Implementing DCMS BPMN orchestration as Solon BPMN extensions.** DCMS-defined process orchestration is implemented in DCMS application code (per the open question on Amplio multi-tenant process isolation in DESIGN-OPTIONS-003). DCMS does not deploy custom BPMN definitions into Solon's Amplio engine via the custom-extension layer.
4. **Calling DCMS application services from (a).** A `revenue-management-be-custom` extension must not synchronously call out to the DCMS application. If logic in (a) needs DCMS state, that is a sign the logic belongs in (b). Asynchronous emission of a Kafka event consumed by DCMS is acceptable only when there is a documented Solon-supported event surface.
5. **Mutating Solon-owned aggregates from (b) directly.** Mutations to Solon aggregates go through Solon's documented API/Kafka contract surface, never via direct database access.

---

## Worked examples

The following examples illustrate the boundary in practice. They are not exhaustive — they exist to anchor the criteria.

### Examples that belong in (a) `revenue-management-be-custom`

| Logic | Why (a) |
|---|---|
| Solon BPMN service-task implementation that DCMS needs to customise (e.g., a custom `PaymentAllocationServiceTask` referenced by a Solon-owned process) | Criterion 1 (Solon aggregate), 2 (BPMN activity), 3 (no DCMS state), 4 (Solon release cadence). No exclusion applies. |
| A custom Drools KIE rule deployed into Solon's KIE container that operates on Solon-owned facts only | Criteria 1–4 met. No exclusion. |
| A Solon allocation-rule extension that must commit atomically with a payment row | Criteria 1–4 met. Transactional necessity (criterion 2) forces (a). |

### Examples that belong in (b) DCMS application

| Logic | Why (b) |
|---|---|
| RULING-016 Breathing Space gate evaluation invoked at every debtor-facing effect | E1 (statutory invariant). Also forbidden pattern 1 (cannot split). |
| Champion/challenger reassignment on vulnerability flag change (RULING-010) | E1, E4 (DCMS state). |
| Suppression check at arrangement creation (RULING-014) | E1, E5 (compliance-coupled with arrangement creation flow which is in (b)). |
| Strategy bundle evaluation and treatment-path selection | E4 (DCMS-owned strategy state). |
| Case Worker workspace render aggregating account, vulnerability, suppressions, tasks, scripted journeys, arrangement state | E3 (debtor-facing experience), E4. |
| Queue/agent assignment, work item routing, exception queue handling | E4. |
| DCA placement file generation, third-party reconciliation | E2 (non-Solon integration). |
| Dialler/CTI integration | E2. |
| Communications dispatch coordination across email, SMS, letter | E2, E4. |

### Examples that need explicit Solution Architect ruling

| Logic | Why ambiguous | Default until ruled |
|---|---|---|
| A payment allocation rule that depends on DCMS-owned strategy classification | Criterion 1 (Solon aggregate) and criterion 2 (transactional) push toward (a); criterion 3 fails (depends on DCMS state); E4 applies | (b) by default rule, with strategy classification published to Solon via documented API at the point of allocation |
| A write-off eligibility check that combines Solon ledger state with DCMS vulnerability protocol state | Criterion 1 partially holds; criterion 3 fails | (b) by default rule |
| A Solon BPMN process variable initialiser that needs to read DCMS-owned configuration once at process start | Criterion 1, 2, 4 hold; criterion 3 fails — but the dependency is read-once and could be passed in at process start | Solution Architect ruling required; lean toward (b) passing the value into Solon at start |

---

## Process

1. **Default placement.** Builders place new DCMS logic in (b) unless the four placement criteria clearly hold and no exclusion applies.
2. **Borderline cases.** If a builder believes logic should be in (a), they raise it to the Solution Architect with a written justification against the four criteria and a check against all five exclusions. The default is (b); the burden of justification is on (a).
3. **Documentation.** Each accepted use of (a) is recorded in `docs/project-foundation/solon-extensions-register.md` (to be created) with: the logic, which criteria justified placement, which exclusions were checked, and which Solon Tax services it extends. This register exists so the upgrade impact of any future Solon Tax version bump is bounded and visible.
4. **Review cadence.** The register is reviewed before every Solon Tax version upgrade. Extensions whose justification has weakened (e.g., DCMS state has crept in) are migrated to (b) before the upgrade proceeds.
5. **Reversal.** If logic placed in (a) later acquires DCMS-only state, DCMS-only release cadence, or compliance-coupling that violates an exclusion, it is migrated to (b). This is a planned refactor, not an emergency.

---

## Consequences

### Positive

- Compliance-bound logic stays in DCMS-owned, DCMS-released, DCMS-audited code. RULING-010, 014, 016 implementations remain testable and rollback-able independently of Solon Tax.
- Solon Tax upgrade impact is bounded by an explicit register of extensions, not by code archaeology.
- The decomposition recommendation in DESIGN-OPTIONS-003 (Option 2: modular monolith) is unblocked — it has a defined scope.
- Distributed-transaction problems are confined to genuinely cross-boundary flows (which exist anyway because Solon owns ledger and DCMS owns coordination).

### Negative

- Some logic that would be operationally simpler in (a) — e.g., allocation rules that need DCMS strategy context — must instead push the relevant DCMS context into Solon at the right moment. This adds DCMS-side responsibility and integration plumbing.
- The register adds documentation discipline. Without it, the boundary erodes.
- Some Solon-side performance optimisations are foregone (e.g., a (b)-side check that could have been an in-process Solon extension now incurs a network call). This is intentional — the cost is paid in exchange for compliance independence.

### Risks

- **R1. Boundary erosion.** Over time, individual decisions that look small (e.g., "just one more state field needed in this extension") accumulate into a (a)-side codebase that effectively owns DCMS state. Mitigated by the upgrade-cadence review and the requirement that exclusion E4 is checked on every (a) placement.
- **R2. Solon API surface gaps.** If Solon does not expose the data or events that a (b)-side flow needs, the path of least resistance is to push the logic into (a) instead of pressing Solon for the API. Mitigated by the default-to-(b) rule and Solution Architect oversight on (a) placements.
- **R3. Premature optimisation pressure.** Performance arguments for placing logic in (a) ("avoid the network hop") may be raised before there is evidence the hop matters. Mitigated by requiring measured latency budgets, not assumed ones, before any performance-driven (a) placement.

---

## Cross-references

| Reference | Relevance |
|---|---|
| ADR-018 | Established Solon Tax as the base platform; this ADR refines the code-location boundary that pivot implies. |
| DESIGN-OPTIONS-003 | Decomposition strategy for (b). Locked only after this ADR is locked. |
| `external_sys_docs/solon/solon_tax_2.3.0_integration_guide.md` | Authoritative description of `revenue-management-be-custom` and Solon's contract surface. |
| `docs/project-foundation/solon-tax-platform-reference.md` | Platform reference. |
| `docs/project-foundation/amplio-process-engine-reference.md` | Amplio constraints relevant to BPMN-extension placement. |
| RULING-010, RULING-014, RULING-016 | Exclusion E1 anchor. |
| `docs/project-foundation/solon-extensions-register.md` | To be created on lock of this ADR. |

---

## Open questions before lock

1. Does Solon Tax provide a documented mechanism for a (a)-side extension to publish DCMS-relevant events back to (b) via Kafka? If yes, asynchronous data flows from (a) to (b) are clean. If no, forbidden pattern 4 ("calling DCMS application services from (a)") becomes more constraining and the criteria must reflect that.
2. Is `revenue-management-be-custom` deployed per Solon service or shared across Solon services? This affects how granularly extensions can be released and rolled back, and may affect criterion 4.
3. Does Solon Tax's upgrade process re-validate `revenue-management-be-custom` extensions, or is that the customer's responsibility? Affects R1 mitigation.

These should be confirmed with the Solon Tax vendor or platform expert before this ADR is moved to ACCEPTED.
