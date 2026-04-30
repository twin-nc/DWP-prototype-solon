> ## ⚠ STATUS: UNDER REVIEW — PLATFORM PIVOT IN EFFECT
>
> A confirmed programme-level decision (ADR-018, 2026-04-30) has pivoted the project to build on top of **Solon Tax** as the base platform. All prior ADRs are under review.
>
> **This document contains accumulated knowledge and analysis that remains relevant to the new design process. It does NOT represent the current or preferred direction.** All decisions and conclusions herein are open for renewed debate. Do not treat this ADR as a directive, default position, or settled constraint.
>
> See [ADR-018](ADR-018-platform-pivot-solon-tax-confirmed.md) for the full pivot record.

---

# ADR-009: Policy Layer — Cross-Tier Configuration Bundles

## Status

Accepted

## Date

2026-04-23

## Context

ADR-008 defines three configurability tiers: Tier 1 (foundational vocabulary), Tier 2 (behavioural rules as DMN), and Tier 3 (process definitions as BPMN). Each tier has its own admin workflow, governance gate, and effective date.

A new administration or a significant policy shift — such as a fraud crackdown — requires coordinated changes across all three tiers simultaneously. Without a coordinating layer, the following problems arise:

1. **Coordination failure:** A business admin activates Tier 2 fraud rules before the Tier 3 fraud treatment path exists. Cases get blocked by rules but have nowhere to go.
2. **Split effective dates:** Tier 1 changes go live Tuesday, Tier 2 on Thursday, Tier 3 the following Monday. During the gaps, the system is in a partially-configured state.
3. **Audit fragmentation:** Three separate change records with no shared identity. Impossible to answer "what exactly changed when we introduced the fraud policy?"
4. **Rollback complexity:** Undoing a policy requires hunting down and reverting three separate sets of changes made on different days.
5. **Language mismatch:** DWP stakeholders think in terms of policies ("we're introducing a fraud crackdown policy"), not configuration tiers.

A **policy layer** addresses these problems by treating a coherent set of cross-tier changes as a named, versioned, approvable unit.

## Decision

### 1. What a policy is

A **policy** is a named bundle of configuration changes across one or more tiers that represents a coherent business intent. It has:
- A name and description (human-readable, stakeholder-facing)
- An owner (the DWP team or role responsible)
- A single effective date (propagated to all constituent changes)
- A status lifecycle: `DRAFT → READY → APPROVED → ACTIVE → RETIRED`
- A unified audit trail and rollback unit

A policy does **not** replace the individual tier approval workflows. Tier 2 DMN changes within a policy still require Domain Expert approval. Tier 3 BPMN changes still require Class A sign-off. The policy layer coordinates them and enforces that all components reach `APPROVED` before the policy can be activated.

### 2. Not all changes require a policy

Lightweight, single-tier operational changes do not need to be wrapped in a policy:

| Change | Policy required? |
|---|---|
| Add a new vulnerability type (Tier 1) | No — standard Tier 1 CRUD |
| Adjust a breach threshold (Tier 2) | No — standard DMN approval workflow |
| Fix a routing bug in a treatment path (Tier 3) | No — standard Class A PR |
| Introduce a new fraud recovery approach spanning all three tiers | **Yes** |
| Implement new government hardship relief requiring adapted contact, new I&E rules, and a new treatment path | **Yes** |

The heuristic: a policy is appropriate when the change has a named business intent that stakeholders will recognise and when it spans more than one tier or requires coordinated activation.

### 3. Policy status lifecycle

```
DRAFT
  │  Business admin assembles components across tiers
  │  Each component goes through its own tier approval workflow
  ▼
READY
  │  All components are individually approved
  │  Policy owner confirms intent and effective date
  ▼
APPROVED
  │  Final sign-off (OPS_MANAGER or COMPLIANCE depending on policy type)
  ▼
ACTIVE  ←── effective date reached, system activates all components atomically
  │
  ▼
RETIRED  ←── policy superseded or withdrawn; components deactivated
```

A policy cannot move from `READY` to `APPROVED` while any constituent component is still in `AWAITING_IMPLEMENTATION` or `AWAITING_APPROVAL`.

### 4. Policy types and dimensions

Reviewing the full requirements taxonomy, policies naturally group into a small number of **dimensions** — distinct concerns that rarely conflict with each other because they answer different questions about a debt case:

| Dimension | What it governs | Example policy |
|---|---|---|
| **Customer protection** | How the system treats a customer given their circumstances | Vulnerability forbearance policy, hardship relief policy |
| **Debt classification** | How a debt is categorised and what recovery rules apply | Fraud crackdown policy, social fund loan policy |
| **Recovery mechanics** | Rates, thresholds, and plan terms | Annual benefit deduction uprating, write-off threshold review |
| **Enforcement** | When and how enforcement action is taken | DCA placement criteria update, field visit restrictions |
| **Regulatory compliance** | Changes driven by new law or regulatory guidance | Breathing space scheme update, CCA amendment |
| **Operational** | Process efficiency, queue management, SLAs | Champion/challenger trial for new segmentation rules |

Policies within the same dimension are more likely to interact than policies in different dimensions. The dimension is a classification field on the policy record, used for impact analysis and conflict detection.

### 5. Policy conflict model

Most policies do not conflict because they operate on different dimensions — a fraud policy (debt classification) and a vulnerability policy (customer protection) answer different questions and apply simultaneously without contradiction.

When conflicts do arise, two resolution mechanisms apply:

**Mechanism 1 — Dimensional precedence (automatic)**

When two active policies both constrain the same action for the same case, the protective/welfare dimension takes precedence over the recovery dimension:

```
Regulatory compliance > Customer protection > Enforcement > Debt classification > Recovery mechanics > Operational
```

Example: Fraud policy (debt classification) says pursue aggressively. Vulnerability policy (customer protection) says no field visits for severity 2+. Vulnerability policy wins on field visits. The fraud recovery proceeds through other means within the vulnerability constraints.

**Mechanism 2 — Explicit conflict resolution rules (for arithmetic conflicts)**

Some conflicts cannot be resolved by precedence because they are arithmetic rather than categorical — two policies both set a deduction rate, or both set a breach threshold. These require an explicit resolution rule defined when either policy is authored:

```
policy_conflict_rule
  ├── id (UUID)
  ├── policy_a_id (UUID)
  ├── policy_b_id (UUID)
  ├── conflict_dimension (VARCHAR) — e.g., "DEDUCTION_RATE", "BREACH_THRESHOLD"
  ├── resolution (VARCHAR) — "TAKE_LOWER", "TAKE_HIGHER", "TAKE_POLICY_A", "TAKE_POLICY_B", "ESCALATE"
  ├── rationale (TEXT)
  └── <audit fields>
```

If no conflict rule exists and the precedence order doesn't resolve it, the system escalates to a supervisor rather than making an automated call.

**Mechanism 3 — Regulatory floor (always wins, not configurable)**

Breathing space, insolvency, and the I&E mandate for vulnerable customers are not policies in the configurability system — they are hardcoded invariants. No policy, regardless of dimension or precedence, can override them.

### 6. Persistence model

```
policy
  ├── id (UUID)
  ├── name (VARCHAR) — e.g., "Fraud Crackdown 2026"
  ├── description (TEXT)
  ├── dimension (VARCHAR) — one of the six dimensions above
  ├── owner_role (VARCHAR) — role responsible for this policy
  ├── status (VARCHAR) — DRAFT / READY / APPROVED / ACTIVE / RETIRED
  ├── effective_date (DATE) — single date propagated to all components
  ├── approved_by (VARCHAR NULL)
  ├── approved_at (TIMESTAMP NULL)
  ├── activated_at (TIMESTAMP NULL)
  ├── retired_at (TIMESTAMP NULL)
  ├── retirement_reason (TEXT NULL)
  └── <audit fields>

policy_component
  ├── id (UUID)
  ├── policy_id (UUID) — FK to policy
  ├── tier (INT: 1, 2, or 3)
  ├── entity_type (VARCHAR) — e.g., "VULNERABILITY_TYPE", "DMN_DEPLOYMENT", "SEGMENT_DEFINITION"
  ├── entity_id (UUID NULL) — set once the component is created/approved
  ├── description (TEXT) — what this component does within the policy
  ├── status (VARCHAR) — DRAFT / AWAITING_IMPLEMENTATION / AWAITING_APPROVAL / APPROVED
  └── <audit fields>

policy_conflict_rule
  ├── id (UUID)
  ├── policy_a_id (UUID)
  ├── policy_b_id (UUID)
  ├── conflict_dimension (VARCHAR)
  ├── resolution (VARCHAR)
  ├── rationale (TEXT)
  └── <audit fields>
```

### 7. UI surface

**Policy management screen:**

A business admin sees a list of policies with their status, dimension, and effective date. They can:
- Create a new policy (name, description, dimension, effective date)
- Add components to the policy — each component opens the relevant tier admin screen (Tier 1 CRUD, Tier 2 DMN editor, Tier 3 BPMN editor) in a mode that associates the change with the policy
- Track component readiness: a status board shows which components are approved, awaiting implementation, or awaiting approval
- Submit the policy for final approval once all components are ready
- View the audit trail for the entire policy as a single timeline

**Policy activation:**

On the effective date, the system atomically:
1. Activates all Tier 1 components (sets `active=true` on new reference data).
2. Promotes Tier 2 DMN deployments from `approved` to `is_champion=true`.
3. Updates `segment_definition` to point to new Tier 3 BPMN process definitions.

If any component fails activation, the entire policy rolls back and an alert is raised.

### 8. Policy rollback

A policy in `ACTIVE` state can be retired by an authorised admin (OPS_MANAGER or COMPLIANCE). Retirement:
1. Deactivates Tier 1 components added by this policy (sets `active=false`).
2. Reverts Tier 2 DMN to the prior champion version.
3. Reverts `segment_definition` to prior process definition keys.
4. Records `retired_at` and `retirement_reason` on the policy record.
5. In-flight process instances are unaffected — they continue on their snapshot configuration.

Rollback does not undo history — it creates a new configuration state. The audit trail records both the activation and the retirement.

## Consequences

1. **Business admins can think and work in policy terms**, not tier terms. A new government mandate becomes a policy with components, not three separate admin tasks with no shared identity.

2. **Coordinated activation eliminates partial-configuration states.** All components of a policy go live together on a single effective date.

3. **A unified audit trail per policy** satisfies DWP audit requirements — regulators can ask "what changed when you introduced the fraud policy?" and receive a single coherent answer.

4. **Rollback is a single operation** — retire the policy, all components revert.

5. **Single-tier operational changes remain lightweight** — they do not require a policy wrapper.

6. **Conflict detection is explicit** — dimensional precedence handles most cases automatically; arithmetic conflicts require declared resolution rules, preventing silent override behaviour.

7. **The regulatory floor remains non-configurable** — breathing space, insolvency, and I&E mandate are not in the policy system and cannot be affected by any policy activation or retirement.

## Alternatives Rejected

### Implicit policy through correlated audit records (Rejected)

Linking tier changes through a shared `correlation_id` in the audit log without a formal policy entity. Rejected because it provides no coordinated activation, no status lifecycle, no rollback unit, and no stakeholder-facing language.

### All changes require a policy wrapper (Rejected)

Requiring even simple Tier 1 CRUD changes to be wrapped in a policy. Rejected because it adds unnecessary overhead for routine operational maintenance and would discourage timely updates to reference data.

### Policy layer replaces tier approval workflows (Rejected)

Having the policy approval serve as the only approval gate, bypassing individual tier workflows. Rejected because Tier 2 DMN changes need Domain Expert technical review and Tier 3 BPMN changes need Class A architectural review — these are independent of whether the change belongs to a policy.

## References

- ADR-008: Three-tier configurability architecture
- ADR-001: Process instance per debt
- ADR-006: BPMN versioning and in-flight migration
- STD-PLAT-005: Date-effective rules
- WAYS-OF-WORKING.md: Class A change gate definition
- RULING-003: Vulnerability configurability governance and constraints
- Requirements: DW.9, DW.16, DW.38, DW.42, DIC.16
