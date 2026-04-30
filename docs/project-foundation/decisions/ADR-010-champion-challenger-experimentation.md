> ## ⚠ STATUS: UNDER REVIEW — PLATFORM PIVOT IN EFFECT
>
> A confirmed programme-level decision (ADR-018, 2026-04-30) has pivoted the project to build on top of **Solon Tax** as the base platform. All prior ADRs are under review.
>
> **This document contains accumulated knowledge and analysis that remains relevant to the new design process. It does NOT represent the current or preferred direction.** All decisions and conclusions herein are open for renewed debate. Do not treat this ADR as a directive, default position, or settled constraint.
>
> See [ADR-018](ADR-018-platform-pivot-solon-tax-confirmed.md) for the full pivot record.

---

# ADR-010: Champion/Challenger Experimentation Framework

## Status

Accepted

## Date

2026-04-23

## Context

DWP requirements DW.7 (champion/challenger testing of treatment strategies) and BSF.9 (champion/challenger scorecard testing) are currently marked as gaps in the trace map.

The system's three-tier configurability architecture (ADR-008) creates multiple dimensions on which alternative configurations could be tested simultaneously against live cases: Tier 2 behavioural rules (DMN), Tier 3 process definitions (BPMN), and combinations of both. Without a first-class experimentation framework, the only way to test a proposed change is to deploy it globally — accepting full risk with no empirical comparison.

A champion/challenger framework allows two configurations to run simultaneously against a split of real accounts, with outcome data collected per arm, so that promotion decisions are evidence-based rather than faith-based.

This ADR establishes the design for Phase 1: Tier 2 DMN experimentation. Tier 3 BPMN experimentation and cross-tier bundled experiments are deferred to later phases but the data model is designed to accommodate them without breaking changes.

## Decision

### 1. Scope and phasing

**Phase 1 (this ADR):** Tier 2 DMN experiments only. An experiment tests one rule set (a named DMN decision table) by routing a configured percentage of eligible accounts to a challenger DMN deployment instead of the current champion.

**Phase 2 (deferred):** Tier 3 BPMN experiments. Accounts are assigned to a challenger BPMN process definition at instantiation time and remain committed to it for the life of that process instance.

**Phase 3 (deferred):** Cross-tier bundled experiments. A challenger arm references both a Tier 2 DMN variant and a Tier 3 BPMN variant, activated together via the policy layer.

Tier 1 (foundational reference data) is not a target for champion/challenger experimentation. Tier 1 changes are structural and go through normal deployment governance.

### 2. The Experiment entity

An **Experiment** is a first-class domain entity, not a flag on a DMN deployment. It is owned by the `strategy` module.

An experiment has:
- A **hypothesis** — the business question being answered
- A **target dimension** — `TIER_2_DMN` in Phase 1; extensible to `TIER_3_BPMN` and `CROSS_TIER_BUNDLE`
- A **rule set** — the named DMN decision table under test (e.g. `SEGMENTATION_RULES`, `ESCALATION_THRESHOLDS`)
- A **champion deployment** — the currently active DMN version (must be in `approved` status)
- A **challenger deployment** — the proposed alternative DMN version (must be in `approved` status; cannot be a draft or untested DMN)
- A **traffic split** — percentage of eligible accounts routed to the challenger arm
- An **eligibility policy** — effective-dated champion/challenger policy that defines account eligibility, protected-population handling, required harm metrics, and promotion constraints
- An **eligibility criteria** — account-level filter evaluated against the active eligibility policy at first encounter; locked thereafter with the policy version recorded
- **Outcome metrics** — declared upfront; must include at least one harm indicator alongside financial metrics
- A **minimum sample size** — a warning threshold, not a hard stop; surfaced prominently on the results dashboard
- A **planned start date** and **planned end date** — the experiment runs its full planned duration; no automated early termination
- An **enrolment type** — `ROLLING` (default for Tier 2) or `COHORT_LOCK` (default for Tier 3 when built)

### 3. Assignment semantics

Assignment semantics differ by dimension type and must be explicit:

**Tier 2 DMN (soft assignment):**
Account assignment is a routing record. Evaluation happens fresh at each decision point. If the experiment concludes before an assigned account's next evaluation, the account reverts to the champion DMN on its next evaluation. Experiments have a clean end — there is no tail period.

**Tier 3 BPMN (hard assignment, Phase 2):**
Account assignment is a structural commitment. At process instantiation, the account is committed to either the champion or challenger BPMN definition for the life of that process instance. The experiment has a stop-enrolment date (the planned end date) and a separate fully-concluded date that can only be known retrospectively, when the last enrolled process instance completes. Early abandonment stops new enrolments but cannot unwind in-flight instances.

### 4. Traffic allocation

All allocation uses deterministic hash-based routing:

```
hash(account_id + experiment_id) mod 100 < challenger_traffic_pct
```

This ensures:
- The same account always gets the same arm within an experiment
- The allocation decision is reproducible from logged identifiers alone — no state is required to reconstruct it
- Random-number approaches that could assign the same account to different arms on re-evaluation are avoided

### 5. Mutual exclusivity

Mutual exclusivity is enforced **within a dimension**, not across all dimensions:
- No account may be in two active experiments targeting the same rule set simultaneously
- An account may be in a `SEGMENTATION_RULES` experiment and an `ESCALATION_THRESHOLDS` experiment simultaneously — these dimensions are considered independent
- The system enforces the within-dimension constraint at experiment activation time and rejects activation if it would create a conflict

### 6. Champion/challenger eligibility policy

Champion/challenger eligibility is policy-governed, not hardcoded in the assignment engine.

The assignment engine provides the mechanism: deterministic allocation, eligibility evaluation, assignment audit, outcome attribution, and promotion gates. The active champion/challenger policy provides the content: which populations are eligible, which populations are excluded or forced to champion, which harm indicators are mandatory, and which promotion constraints apply.

The current approved policy requires vulnerable customers to receive CHAMPION treatment and to be excluded from the A/B comparison panel. That rule is enforced through the active policy version, not by an unchangeable code constant.

Any policy change that would allow vulnerable customers, or another protected population, to receive CHALLENGER treatment is a Class A policy change. It must go through the policy approval process, record legal/policy rationale, carry an effective date, and receive DWP sign-off where the relevant ruling requires it.

Every assignment must record:

- champion/challenger policy version used
- eligibility inputs used
- whether a protected-population rule affected assignment
- resulting arm
- override reason, where applicable

### 7. Experiment lifecycle

```
DRAFT → SUBMITTED → APPROVED → ACTIVE → CONCLUDED → ARCHIVED
                ↘ REJECTED → (back to DRAFT)
         ACTIVE → ABANDONED
         CONCLUDED → ABANDONED (before archival decision)
```

**DRAFT:** Designer configures the experiment. Challenger DMN must already be in `approved` status before it can be nominated. The experiment cannot reference a draft or unapproved variant.

**SUBMITTED:** System validates configuration integrity (both deployments approved, no conflicting active experiment, dates valid). Approval task is created and routed per WAYS-OF-WORKING §5a.

**APPROVED:** Approvers review the experiment *design* — eligibility scope, traffic split proportionality, outcome metrics (including harm indicators), and duration. This is not a re-review of the DMN quality; that was done when the DMN was approved. Both approvers must sign off. Either may reject, returning to DRAFT with a reason.

**ACTIVE:** Experiment is running. New eligible accounts are assigned on first encounter (rolling enrolment). Activated on planned_start_date (or manually if that date is today or past; never early).

**CONCLUDED:** Reached planned_end_date. New enrolments stop. Already-assigned accounts continue to be evaluated against their arm's DMN and outcome events continue to be collected. Owner reviews results and decides: promote, abandon, or extend (extension requires re-approval with new end date).

**ABANDONED:** Available from ACTIVE or CONCLUDED. Stops new enrolments immediately. Assigned accounts revert to champion DMN on their next evaluation. Mandatory reason required. Requires OPS_MANAGER or COMPLIANCE role.

**ARCHIVED:** Terminal. All records retained for audit. Read-only.

### 8. Experiment activation is a Class A change

Activating an experiment directly affects which rules govern real debtors, potentially including vulnerable people. Per WAYS-OF-WORKING §5a and §6:
- At least one approver must have domain or requirements knowledge relevant to the rule set under test
- COMPLIANCE must be consulted if the experiment touches vulnerability handling, disclosure obligations, or payment allocation rules
- COMPLIANCE approval is required before activation (absorbs DOMAIN_EXPERT governance gate — DOMAIN_EXPERT is not a provisioned role per RBAC design review 2026-04-24)

### 9. Promotion

When a concluded experiment's owner decides to promote the challenger:

1. `is_champion = true` set on the challenger DMN deployment
2. `is_champion = false` set on the prior champion; `effective_to = now()`
3. `is_challenger = false` and `challenger_traffic_pct = 0` cleared on the former challenger
4. An audit event is written with the experiment ID as provenance — the promotion is traceable to the experiment that justified it
5. Experiment status set to ARCHIVED

Promotion is itself a Class A change. The `POST /experiments/{id}/promote` endpoint enforces this via role check and the active champion/challenger policy's promotion constraints.

### 10. Outcome attribution

The `strategy` module records which experiment arm was used for every DMN evaluation. The `analytics` module aggregates outcome events (payments, plan arrangements, escalations, complaints, breathing space applications) against experiment assignments. The `strategy` module does not own outcome analysis — it owns routing and recording.

Outcome events are emitted by other domain modules using a standard `ExperimentOutcomeEvent` carrying: experiment ID, assignment ID, account ID, arm, outcome type, outcome value, and source module. The experiment infrastructure does not need to know the semantics of each outcome type.

### 11. Relationship to the policy layer (ADR-009)

Champion/challenger experimentation and the policy layer are related but separate concerns:
- The policy layer coordinates multi-tier configuration changes with a shared effective date
- The experimentation framework gathers evidence to inform whether a configuration change should be permanently adopted
- The champion/challenger eligibility policy governs who may participate in challenger treatment and what evidence is required before promotion

They integrate at one point: experiment promotion can optionally trigger a policy activation when the promoted change is part of a broader policy bundle. This is a future integration point, not a Phase 1 requirement.

Single-tier experiment promotions (Tier 2 DMN only) do not require a policy wrapper — they are direct DMN champion flag updates.

Changes to champion/challenger eligibility policy do require policy governance even when the experiment itself is Tier 2 only.

## Data Model

```
experiment
  ├── id (UUID)
  ├── name (VARCHAR)
  ├── hypothesis (TEXT)
  ├── owner (VARCHAR — user ID)
  ├── dimension (VARCHAR) — 'TIER_2_DMN' | 'TIER_3_BPMN' | 'CROSS_TIER_BUNDLE'
  ├── rule_set_name (VARCHAR) — references dmn_deployment.rule_set_name
  ├── cc_policy_version_id (UUID FK → champion_challenger_policy_version)
  ├── champion_deployment_id (UUID FK → dmn_deployment)
  ├── challenger_deployment_id (UUID FK → dmn_deployment)
  ├── traffic_split_pct (INT) — % routed to challenger (1–99)
  ├── allocation_method (VARCHAR) — 'DETERMINISTIC_HASH' (only valid value in Phase 1)
  ├── eligibility_criteria (JSONB) — account filter evaluated at assignment time
  ├── outcome_metrics (JSONB) — metric types to track, declared upfront
  ├── minimum_sample_size (INT) — warning threshold, not a hard stop
  ├── enrolment_type (VARCHAR) — 'ROLLING' | 'COHORT_LOCK'
  ├── planned_start_date (DATE)
  ├── planned_end_date (DATE)
  ├── actual_start_date (DATE NULL)
  ├── actual_end_date (DATE NULL)
  ├── status (VARCHAR) — DRAFT | SUBMITTED | APPROVED | ACTIVE | CONCLUDED | ABANDONED | ARCHIVED
  ├── approved_by (VARCHAR NULL)
  ├── approved_at (TIMESTAMP NULL)
  ├── rejection_reason (TEXT NULL)
  ├── abandonment_reason (TEXT NULL)
  └── <audit fields>

experiment_assignment
  ├── id (UUID)
  ├── experiment_id (UUID FK → experiment)
  ├── account_id (UUID FK → account)
  ├── arm (VARCHAR) — 'CHAMPION' | 'CHALLENGER'
  ├── assigned_at (TIMESTAMP)
  ├── assignment_basis (JSONB) — snapshot of eligibility inputs at assignment time
  ├── policy_version_id (UUID FK → champion_challenger_policy_version)
  ├── policy_decision_snapshot (JSONB) — protected-population rule and eligibility outcome
  └── enrolment_type (VARCHAR) — 'ROLLING' | 'COHORT_LOCK'

experiment_evaluation_event
  ├── id (UUID)
  ├── experiment_id (UUID FK → experiment)
  ├── assignment_id (UUID FK → experiment_assignment)
  ├── account_id (UUID)
  ├── rule_set_name (VARCHAR)
  ├── dmn_deployment_id (UUID) — which version was evaluated
  ├── arm (VARCHAR)
  ├── input_snapshot (JSONB)
  ├── output_snapshot (JSONB)
  ├── evaluated_at (TIMESTAMP)
  └── process_instance_id (VARCHAR NULL)

experiment_outcome_event
  ├── id (UUID)
  ├── experiment_id (UUID FK → experiment)
  ├── assignment_id (UUID FK → experiment_assignment)
  ├── account_id (UUID)
  ├── arm (VARCHAR)
  ├── outcome_type (VARCHAR) — 'PAYMENT_MADE' | 'PLAN_ARRANGED' | 'ESCALATION' |
  │                             'COMPLAINT' | 'BREATHING_SPACE' | 'PLAN_BREACH' | etc.
  ├── outcome_value (JSONB)
  ├── occurred_at (TIMESTAMP)
  └── source_module (VARCHAR)

champion_challenger_policy_version
  ├── id (UUID)
  ├── policy_key (VARCHAR) — e.g. 'DEFAULT_CC_ELIGIBILITY'
  ├── version (INT)
  ├── effective_from (TIMESTAMP)
  ├── effective_to (TIMESTAMP NULL)
  ├── protected_population_rules (JSONB)
  ├── mandatory_harm_metrics (JSONB)
  ├── promotion_constraints (JSONB)
  ├── legal_policy_reference (TEXT)
  ├── status (VARCHAR) — DRAFT | SUBMITTED | APPROVED | ACTIVE | RETIRED
  ├── approved_by (VARCHAR NULL)
  └── audit fields
```

## Admin API

### Experiment lifecycle

```
POST   /api/v1/experiments                              — create draft
GET    /api/v1/experiments                              — list (filter: status, rule_set, owner)
GET    /api/v1/experiments/{id}                         — get detail
PUT    /api/v1/experiments/{id}                         — update (DRAFT status only)
POST   /api/v1/experiments/{id}/submit                  — submit for approval
POST   /api/v1/experiments/{id}/approve                 — record approval (identity from JWT)
POST   /api/v1/experiments/{id}/reject                  — record rejection with reason
POST   /api/v1/experiments/{id}/activate                — manual activation (APPROVED, start_date <= today)
POST   /api/v1/experiments/{id}/conclude                — manual early conclusion (stops new enrolment)
POST   /api/v1/experiments/{id}/abandon                 — abandon with mandatory reason
```

### Promotion and results

```
POST   /api/v1/experiments/{id}/promote                 — make challenger the new champion (CONCLUDED only)
POST   /api/v1/experiments/{id}/archive                 — move to ARCHIVED
GET    /api/v1/experiments/{id}/assignments             — account assignments (paginated)
GET    /api/v1/experiments/{id}/assignments/summary     — counts by arm, enrolment date distribution
GET    /api/v1/experiments/{id}/evaluations             — DMN evaluation events (paginated)
GET    /api/v1/experiments/{id}/outcomes                — outcome events by arm (paginated)
GET    /api/v1/experiments/{id}/outcomes/summary        — aggregated outcomes by arm and metric type
```

### Supporting lookups

```
GET    /api/v1/experiments/eligible-rule-sets           — rule sets with champion + ≥1 approved challenger
GET    /api/v1/experiments/eligible-rule-sets/{name}/deployments — deployment options for a rule set
GET    /api/v1/experiments/active                       — currently active experiments (for router cache)
```

## Consequences

1. **Evidence-based promotion.** DMN changes that have passed an experiment carry an audit trail back to the hypothesis, the population tested, and the outcomes observed. Promotion is not a leap of faith.

2. **Harm indicators are mandatory.** By requiring at least one harm indicator in `outcome_metrics`, the framework prevents purely financial optimisation that ignores debtor welfare — a DWP-specific obligation.

3. **Protected-population handling is policy-governed.** The current policy requires vulnerable customers to receive champion treatment, but that rule is represented as effective-dated policy content rather than an immutable code constant.

4. **The DMN engine is unaware of experiments.** The `strategy` module's evaluation service owns routing. Flowable is not extended or modified. Experiment concern is confined to the `strategy` module.

5. **Clean separation from the policy layer.** Experiments gather evidence; policies coordinate deployment and govern eligibility rules. The assignment engine enforces the active policy version but does not embed policy constants.

6. **Phase 2 readiness without Phase 1 debt.** The `dimension` field, `enrolment_type` field, and nullable `process_instance_id` on evaluation events are placeholders for Tier 3. They add no Phase 1 complexity but avoid a breaking schema change when Tier 3 is built.

7. **Mutual exclusivity within dimension** prevents contaminated outcome data for the same decision point while allowing genuinely independent experiments to run concurrently.

8. **Promotion is traceable.** The `experiment_id` provenance on the DMN champion promotion event satisfies DWP audit requirements — regulators can ask why a rule changed and receive a direct answer.

## References

- ADR-004: Segment taxonomy and configurability
- ADR-008: Three-tier configurability architecture
- ADR-009: Policy layer
- WAYS-OF-WORKING.md §5a, §6 (Class A approval gate)
- DW.7 (champion/challenger testing of treatment strategies)
- BSF.9 (champion/challenger scorecard testing)
- DW.2, DW.39, DW.41
