# Strategy Simulation Engine Design

**Project:** DWP Debt Collection Management System (DCMS)  
**Date:** 2026-04-27  
**Status:** Design baseline with phased roadmap  
**Purpose:** Define offline simulation for proposed rules, policies, and process changes before they affect live debt collection behaviour.

---

## 1. Design Intent

The old additional-layer diagram included a "Simulation Engine (replay historical journeys)". That capability is useful, but the Solon-style framing is not.

In DCMS, the **Strategy Simulation Engine** is an offline, deterministic evidence tool for PROCESS_DESIGNER and COMPLIANCE. It answers:

> If this proposed rule, policy, or process change had applied to a known account cohort, what decisions and operational impacts would have changed?

Simulation is not live experimentation. It is pre-live evidence used before approval, activation, or champion/challenger setup.

---

## 2. Scope Principles

Simulation must be:

- **Read-only** — no live domain state is mutated.
- **Deterministic** — same cohort, data cut-off, rule versions, policy versions, and process/action catalogue versions produce the same result.
- **Evidence-producing** — each run records inputs, versions, user, timestamp, result hash, and exceptions.
- **Policy-aware** — effective-dated policies and protected-population rules are part of the simulation context.
- **Governance-aligned** — simulation supports approval but does not bypass Class A gates.

Simulation must not:

- start Flowable process instances
- complete tasks
- send communications
- post payments
- write account facts
- create work items
- silently use "latest" rules without recording exact versions

---

## 3. Phased Roadmap

| Phase | Name | Capability | Primary UI home | Status |
|---|---|---|---|---|
| 1 | DMN rule simulation | Run current vs proposed DMN/rule versions against a selected account cohort | `/admin/rules/simulations` | Recommended MVP |
| 2 | Policy impact simulation | Simulate policy-bundle changes across Tier 1/Tier 2/Templates where rules and policies interact | `/admin/policies/{id}/simulation` | Next after MVP |
| 3 | Strategy path simulation | Simulate a sequence of configured decisions/actions using Process Action Library metadata, without executing side effects | `/admin/processes/simulations` | Later |
| 4 | BPMN process replay | Replay historical event timelines through candidate BPMN definitions and compare paths/timers/escalations | `/admin/processes/replay` | Advanced/future |
| 5 | Promotion evidence integration | Attach simulation evidence to champion/challenger setup, policy approval, and promotion decisions | Rules, Policies, Experiments | Cross-phase integration |

Phase 1 should be built first because it aligns with ADR-015 "bulk test against account batch" and does not require full Flowable replay. Phase 4 must not be promised as initial delivery: BPMN replay depends on event timing, timers, in-flight state, external outcomes, and historic fact reconstruction.

---

## 4. Phase 1 - DMN Rule Simulation

### User workflow

1. PROCESS_DESIGNER opens a Tier 2 rule screen in `/admin/rules`.
2. Selects "Run simulation".
3. Selects baseline rule version, proposed rule version, account cohort, data cut-off timestamp, and policy version.
4. System runs both versions against the cohort.
5. Results show aggregate deltas and account-level changed decisions.
6. PROCESS_DESIGNER attaches the simulation run to a rule approval request.
7. COMPLIANCE reviews the evidence during approval.

### Inputs

| Input | Requirement |
|---|---|
| Rule set | Named DMN/rule table, e.g. segmentation, DCA placement, queue routing |
| Baseline version | Approved active or explicitly selected historic version |
| Proposed version | Draft/submitted/approved version under review |
| Cohort definition | Saved filter, uploaded account IDs, or query-backed sample |
| Data cut-off | Timestamp used to freeze account facts for deterministic replay |
| Policy version | Effective-dated policy context used by the simulation |
| Protected-population handling | Read from the selected policy version, not a per-run toggle |

### Outputs

| Output | Purpose |
|---|---|
| Cohort summary | Accounts evaluated, excluded, errored, and sampled |
| Decision distribution | Baseline vs proposed result counts |
| Decision deltas | Accounts whose decision changed |
| Protected-population impact | Vulnerability, breathing space, deceased, insolvency, statute-barred counts |
| Operational load proxy | Expected extra work items, communications, manual reviews, DCA referrals |
| Harm indicator proxy | Complaints/enforcement/breach risk indicators where historical labels exist |
| Exceptions | Missing facts, invalid inputs, unsupported rule branches |
| Evidence hash | Stable hash of inputs and result payload for approval evidence |

---

## 5. Phase 2 - Policy Impact Simulation

Policy simulation evaluates a proposed policy bundle before activation.

It supports questions such as:

- What changes if vulnerable-customer champion/challenger eligibility policy changes?
- What changes if DCA placement policy and disclosure templates activate together?
- What changes if communication fallback order changes alongside suppression policy?

Phase 2 must simulate the bundle as a coherent effective-dated unit. It must not simulate each component independently and then treat the results as equivalent.

---

## 6. Phase 3 - Strategy Path Simulation

Strategy path simulation evaluates configured decision/action sequences without executing side effects.

This phase uses:

- Process Action Library metadata
- action input/output schemas
- declared side-effect categories
- allowed contexts
- current/proposed DMN and policy versions

It can estimate actions selected, work items created, communications attempted, suppressions applied, manual reviews required, and DCA eligibility decisions reached.

It does not invoke production command handlers. It uses simulation adapters or pure decision functions so no live side effects occur.

---

## 7. Phase 4 - BPMN Process Replay

BPMN replay is the advanced phase.

It replays historic account event timelines through a candidate BPMN definition or process version and compares:

- path taken
- timers scheduled
- event subprocesses triggered
- service tasks selected
- manual work created
- hold/suppression states
- terminal or current process state

BPMN replay requires historic event timeline reconstruction, process-variable snapshot rules, historic domain fact snapshots or deterministic point-in-time reads, timer simulation, side-effect stubs for all Process Action Library actions, and clear handling of missing historic data.

Replay is a decision-support approximation unless every historic input and external event can be reconstructed. The UI must label confidence and exception counts clearly.

---

## 8. Evidence and Audit Model

Every simulation run creates immutable evidence.

```sql
simulation_run
  id UUID PRIMARY KEY
  simulation_type VARCHAR NOT NULL -- DMN_RULE | POLICY_BUNDLE | STRATEGY_PATH | BPMN_REPLAY
  status VARCHAR NOT NULL -- RUNNING | COMPLETED | FAILED | CANCELLED
  requested_by VARCHAR NOT NULL
  requested_at TIMESTAMP NOT NULL
  completed_at TIMESTAMP NULL
  cohort_definition JSONB NOT NULL
  data_cutoff_at TIMESTAMP NOT NULL
  baseline_context JSONB NOT NULL
  proposed_context JSONB NOT NULL
  policy_versions JSONB NOT NULL
  result_summary JSONB NULL
  exception_summary JSONB NULL
  result_hash VARCHAR NULL
  attached_to_type VARCHAR NULL -- RULE_CHANGE | POLICY_BUNDLE | EXPERIMENT | BPMN_DEPLOYMENT
  attached_to_id UUID NULL
```

Large account-level results may be stored separately:

```sql
simulation_result_detail
  simulation_run_id UUID NOT NULL
  account_id UUID NOT NULL
  baseline_result JSONB NOT NULL
  proposed_result JSONB NOT NULL
  delta JSONB NOT NULL
  exception_code VARCHAR NULL
  PRIMARY KEY (simulation_run_id, account_id)
```

Simulation evidence is approval evidence, not operational telemetry.

---

## 9. UI Surface

| Area | Route | Phase |
|---|---|---|
| Rule simulations | `/admin/rules/simulations` | 1 |
| Policy-bundle simulations | `/admin/policies/{policyBundleId}/simulation` | 2 |
| Strategy path simulations | `/admin/processes/simulations` | 3 |
| BPMN process replay | `/admin/processes/replay` | 4 |

Phase 3 and 4 routes should not appear in normal MVP navigation unless marked as future/unavailable.

---

## 10. API Sketch

```text
POST /api/v1/simulations/rule
POST /api/v1/simulations/policy-bundle
POST /api/v1/simulations/strategy-path
POST /api/v1/simulations/bpmn-replay

GET  /api/v1/simulations
GET  /api/v1/simulations/{id}
GET  /api/v1/simulations/{id}/details
POST /api/v1/simulations/{id}/attach
POST /api/v1/simulations/{id}/cancel
```

MVP should implement only rule simulation plus list/detail/attach APIs.

---

## 11. Governance Rules

Simulation is required before approval for:

- champion/challenger experiment setup where a challenger rule has materially different eligibility or outcome behaviour
- policy bundles that alter protected-population eligibility
- DCA placement eligibility changes
- vulnerability protocol rule changes
- communication suppression or fallback changes

Simulation is recommended, but not always mandatory, for minor threshold changes, queue routing adjustments, and non-protected-population segmentation changes.

Simulation cannot approve a change by itself. It supplies evidence for the normal approval path.

---

## 12. Roadmap Acceptance Criteria

### Phase 1 acceptance

- PROCESS_DESIGNER can run current-vs-proposed DMN simulation for a selected account cohort.
- Results are deterministic for the same inputs.
- Results show decision deltas and protected-population impact.
- Simulation run can be attached to a rule approval request.
- COMPLIANCE can view the attached evidence.
- No live domain state is mutated.

### Phase 2 acceptance

- Policy bundle simulation evaluates all included changes as one effective-dated unit.
- Conflicts and missing dependencies are surfaced.
- Protected-population impact is shown explicitly.
- Evidence attaches to policy approval.

### Phase 3 acceptance

- Process Action Library metadata can drive side-effect-free strategy path simulation.
- Unsupported action branches are reported as exceptions, not silently skipped.
- Operational load proxy is produced.

### Phase 4 acceptance

- Historic event timelines can be replayed through candidate BPMN definitions.
- Timer and event subprocess behaviour is simulated.
- Replay confidence and missing-data exceptions are visible.
- Replay results are evidence-linked but labelled as replay estimates.

---

## 13. Open Design Questions

| Question | Impact |
|---|---|
| What cohort selection mechanisms are required for MVP: saved filters, upload, or both? | Determines UI and query builder scope. |
| Should simulation runs be synchronous for small cohorts and async for large cohorts? | Affects API and job model. |
| What maximum cohort size is acceptable for Phase 1? | Affects performance and evidence storage. |
| Which rule tables require mandatory simulation before approval? | Governance configuration and COMPLIANCE workflow. |
| Do we need point-in-time domain fact snapshots for Phase 1, or is data cut-off with current durable facts sufficient? | Determines whether Phase 1 can be built before full snapshot support. |
| Which side-effect stubs are needed before Phase 3? | Depends on Process Action Library coverage. |

---

## 14. Related Artefacts

- ADR-008: Three-tier configurability architecture
- ADR-009: Policy layer and cross-tier change bundles
- ADR-010: Champion/challenger experimentation framework
- ADR-015: Configuration surface and role-scoped admin UI
- `process-action-library-design.md`
- `domain-packs/reporting-analytics-read-model-domain-pack.md`
- `MASTER-DESIGN-DOCUMENT.md`
