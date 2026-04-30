# ADR-008: Three-Tier Configurability Architecture (Foundational, Behavioural, Process)

## Status

Accepted

## Date

2026-04-23

## Context

Requirement DIC.16 and the broader configurability mandate (DW.9, DW.16, DW.38, DW.39, DW.42, DW.44) establish that DWP must adjust debt collection behaviour frequently without code deployments. ADR-004 defined two-tier configurability (segment definitions + DMN rules for tier 1, BPMN deployment for tier 2). This ADR elaborates that model into three concrete tiers, each with its own persistence model, effective-dating mechanism, governance workflow, and UI surface.

Three design questions require resolution:

1. How do we distinguish between **reference data** (vulnerability types, debt categories, suppression reasons), **behavioural rules** (which vulnerability level blocks which action), and **process structure** (which treatment path runs for which segment)?
2. How do in-flight cases use a **configuration snapshot** from when they were opened, even after the live config changes?
3. Where should **action-level decision logic** live — in DMN tables or in database configuration rows?

## Decision

### 1. Three-tier configurability model

**Tier 1 — Foundational/Definitional (No-code, DWP admin only)**

Reference data tables that define the universe of allowed values and their properties:
- Vulnerability types (codes, labels, severity levels, triggering actions)
- Debt categories (classification scheme, recovery priority)
- Action codes (system-wide action taxonomy)
- Suppression reasons (contact suppression criteria)
- Communication channels (SMS, email, letter, in-app — enabled/disabled per environment)

Tier 1 is **operational configuration, not business rules**. These tables define what things are called and what their intrinsic properties are. They rarely change (quarterly, not weekly). When they change, the change takes effect immediately on new processes; in-flight processes are unaffected.

Tier 1 answers: *What are the allowed values in the system?*

**Tier 2 — Behavioural/Task (Low-code, strategy admin + COMPLIANCE review)**

Decision logic tables that govern behaviour:
- Vulnerability action matrix: vulnerability level × action code → blocked/allowed/escalated
- Channel routing rules: customer circumstances × communication channel → available/unavailable
- Breach thresholds and tolerances: account type × arrangement status → tolerance period, escalation action
- Work allocation rules: debt segment × priority × queue assignment → agent pool
- Escalation triggers: condition × escalation path

Tier 2 is **business rule configuration, deployed as DMN decision tables**. These tables encode the "if-then-else" logic of collections strategy. They change frequently (monthly) and are versioned, testable, and champion/challenger-enabled (DW.2, DW.39).

Tier 2 rules reference Tier 1 definitions by stable ID (foreign key).

Tier 2 answers: *What should happen in this situation, given the rules we've set?*

**Tier 3 — Process (Embedded BPMN editor + developer handoff, Class A review)**

BPMN process definitions that orchestrate workflows:
- Treatment process definitions (one per segment type, or one per business line)
- Exception handling event subprocesses (breathing space, insolvency, death)
- Step ordering and conditional branching
- Integration with external systems (DCA placement, payment posting, notifications)

**Tier 3 authoring model:** Business admins author BPMN diagrams directly using an embedded Flowable Modeler in the admin UI. The system validates the diagram against the available service task library and reports which steps are already implemented vs. which require developer work. A fully-coverable diagram can be approved and deployed without developer involvement. A diagram with unimplemented steps is handed off to a developer to implement the missing service tasks, after which it returns to the approval workflow.

This means:
- Business admin owns the *design* of all treatment paths.
- Developer involvement is required only when new *capabilities* (service tasks) are needed — not for assembling existing ones.
- Over time, as the service task library grows, more paths become developer-free.

Tier 3 still requires a Class A two-person gate (PROCESS_DESIGNER proposes; COMPLIANCE approves) regardless of whether a developer was involved. New changes take effect on new process instances only; in-flight instances continue on their original definition version.

Tier 3 answers: *In what sequence should this debt move through the system?*

### 2. Configuration snapshot and effective-dating mechanism

**Snapshot capture at process start:**

When a debt account is created and a process instance is started:
1. The current **Tier 1 definitions** (vulnerability types, action codes, categories) are recorded by their IDs and labels in the `process_execution_context` record.
2. The current **Tier 2 DMN version** (behavioural rules version) is recorded in the process instance context as `dmn_version_id`.
3. The current **Tier 3 BPMN version** (process definition version) is automatically captured by Flowable as the `process_definition_key` and `process_definition_version`.

**Lookup at decision time:**

When a task needs to make a decision (e.g., "can this vulnerability level block this action?"):
1. Retrieve the `dmn_version_id` stored with the process instance.
2. Query the **versioned DMN deployment** (not the current active DMN) for the rules that apply.
3. Flowable's internal versioning handles this: each DMN deployment has a version number; queries can be scoped to a specific version.

**Tier 1 definitions do not need versioning** because they are treated as immutable reference data:
- When you update a vulnerability type label or add a property, existing process instances do not need to "see" the new version — they already have the correct IDs and labels captured at start.
- If a vulnerability type is retired, the process instances that reference it can still operate (the ID exists in their context), but new instances cannot be assigned the retired type.

**Effective-dating policy (STD-PLAT-005):**

- Tier 1 changes take effect immediately (no effective-from date).
- Tier 2 (DMN) changes use effective-from dates: the old DMN version remains deployed and executable for in-flight cases until the effective-from date passes, then the new version becomes active for new instances. Champion/challenger testing may run both versions in parallel.
- Tier 3 (BPMN) changes follow ADR-006 versioning rules: non-breaking changes for new instances, breaking changes require migration + Class A approval.

### 3. Tier 1 — Foundational Definitions: Persistence Model

**Core tables:**

```
vulnerability_type
  ├── id (UUID, primary key)
  ├── code (VARCHAR, unique, immutable)
  ├── label (VARCHAR)
  ├── severity_level (INT: 1=mild, 2=moderate, 3=severe) — used by Tier 2 rules
  ├── regulatory_basis (VARCHAR) — e.g., "FCA_FG21_1" (DIC.16 classification: Restricted)
  ├── active (BOOLEAN) — soft delete flag
  ├── created_at, created_by, updated_at, updated_by
  └── metadata (JSONB) — extensible properties

debt_category
  ├── id (UUID)
  ├── code (VARCHAR, unique)
  ├── label (VARCHAR)
  ├── recovery_priority (INT: 1=high, 2=medium, 3=low)
  ├── active (BOOLEAN)
  └── <audit fields>

action_code
  ├── id (UUID)
  ├── code (VARCHAR, unique)
  ├── label (VARCHAR)
  ├── action_type (VARCHAR) — e.g., "COMMUNICATION", "LEGAL_ACTION", "PAYMENT_POSTING", "WRITE_OFF"
  ├── requires_escalation (BOOLEAN)
  ├── requires_approval_role (VARCHAR) — e.g., "TEAM_LEADER", "OPS_MANAGER"
  ├── active (BOOLEAN)
  └── <audit fields>

suppression_reason
  ├── id (UUID)
  ├── code (VARCHAR, unique)
  ├── label (VARCHAR)
  ├── allows_contact_override (BOOLEAN)
  ├── active (BOOLEAN)
  └── <audit fields>

communication_channel_config
  ├── id (UUID)
  ├── channel_type (VARCHAR: "SMS", "EMAIL", "LETTER", "IN_APP")
  ├── environment (VARCHAR: "local", "dev", "staging", "prod")
  ├── enabled (BOOLEAN)
  ├── retry_attempts (INT)
  ├── retry_backoff_seconds (INT)
  ├── active (BOOLEAN)
  └── <audit fields>
```

**Notes:**
- All columns holding classification or regulatory basis are marked `PII:RESTRICTED` in Flyway migrations per STD-SEC-003.
- `code` fields are immutable (primary identifier across tiers); labels and metadata may change.
- `active` flag allows soft deletion without breaking references.
- `metadata` JSONB allows extension without schema migration (e.g., new vulnerability category properties).

### 4. Tier 2 — Behavioural Rules: Persistence Model and DMN Integration

**Configuration tables:**

```
dmn_deployment
  ├── id (UUID)
  ├── deployment_id (VARCHAR, from Flowable)
  ├── version (INT) — auto-incrementing version for this rule set
  ├── rule_set_name (VARCHAR) — e.g., "VULNERABILITY_ACTION_MATRIX"
  ├── effective_from (TIMESTAMP) — when this version becomes active
  ├── effective_to (TIMESTAMP NULL) — when this version is superseded
  ├── is_champion (BOOLEAN) — true if this is the current live version
  ├── is_challenger (BOOLEAN, challenger_traffic_pct INT) — true if testing against champion
  ├── deployed_by (VARCHAR) — user who deployed
  ├── deployed_at (TIMESTAMP)
  └── created_at, updated_at

dmn_decision_table_row
  ├── id (UUID)
  ├── dmn_deployment_id (UUID) — FK to dmn_deployment
  ├── decision_table_name (VARCHAR) — e.g., "vulnerability_action_matrix"
  ├── input_conditions (JSONB) — the "if" side
  │   {
  │     "vulnerability_level": 3,
  │     "action_code_id": "uuid-xxx",
  │     "account_type": "STANDARD_ON_BENEFIT"
  │   }
  ├── output_action (JSONB) — the "then" side
  │   {
  │     "allowed": false,
  │     "requires_escalation": true,
  │     "escalation_path": "TEAM_LEADER_OVERRIDE",
  │     "communication_required": "EMAIL_CUSTOMER"
  │   }
  ├── precedence (INT) — evaluation order
  ├── description (VARCHAR) — rule intent
  └── <audit fields>

dmn_test_result (for DW.39, DW.41 — bulk testing)
  ├── id (UUID)
  ├── dmn_deployment_id (UUID)
  ├── test_batch_id (UUID)
  ├── account_id (UUID)
  ├── input_snapshot (JSONB) — input variables at test time
  ├── expected_output (JSONB)
  ├── actual_output (JSONB)
  ├── passed (BOOLEAN)
  └── created_at
```

**DMN authoring and deployment:**

Tier 2 rules are authored and versioned using Flowable's DMN engine:
- Decision tables are authored in Flowable Modeler or as XML/JSON DMN definitions.
- Each `dmn_deployment` row corresponds to a versioned Flowable DMN deployment.
- The decision table itself (the logic) lives in the `.dmn` file deployed to Flowable.
- The `dmn_decision_table_row` table serves as an **audit trail and searchable index** — it mirrors the DMN definition in relational form for UI browsing and simulation.
- DMN files are stored as versioned artefacts in the codebase (`backend/src/main/resources/dmn/`) and deployed at application startup; subsequent versions are deployed via the admin API.

**Why not store DMN as database CLOB?**

The decision to deploy DMN files as static resources and version them via Flowable's native deployment mechanism ensures:
- Flowable can optimize DMN evaluation (pre-compiled decision trees, caching).
- DMN versioning is native to Flowable (no custom versioning logic needed).
- DMN files can be reviewed, audited, and diffed in version control alongside application code.
- Champion/challenger traffic split is managed by Flowable's native variant mechanism.

### 5. Tier 3 — Process Architecture (BPMN)

**BPMN process definition placement:**

- Location: `backend/src/main/resources/processes/treatment/`
- Naming: `treatment-{segment_code}-v{version}.bpmn`
- Example: `treatment-vulnerable-on-benefit-v2.bpmn`

**Process definition reference:**

The `segment_definition` table (from ADR-004) maps segments to process definition keys:

```
segment_definition
  ├── id (UUID)
  ├── code (VARCHAR, unique) — e.g., "VULNERABLE_ON_BENEFIT"
  ├── label (VARCHAR)
  ├── process_definition_key (VARCHAR) — Flowable key, e.g., "treatment-vulnerable-on-benefit"
  ├── process_definition_version (INT NULL) — optional pin to specific version; NULL means "latest"
  ├── active (BOOLEAN)
  ├── effective_from (TIMESTAMP)
  ├── effective_to (TIMESTAMP NULL)
  └── <audit fields>
```

**Process versioning and migration (ADR-006):**

- Non-breaking BPMN changes are deployed for new instances; in-flight instances remain on their original definition version.
- Breaking changes (e.g., removing a task, renaming a signal correlation key) require a migration strategy and Class A approval.
- The `process_execution_context` table records the BPMN version the process started with, enabling future migration decisions.

### 6. Governance Model: RBAC and Approval Workflows

**Role-based access control:**

| Role | Tier 1 | Tier 2 | Tier 3 |
|---|---|---|---|
| AGENT | View only | View only | View only (audit trail) |
| TEAM_LEADER | View only | View only | View only |
| OPS_MANAGER | Create, modify, retire | View only | View only |
| COMPLIANCE | View, audit trail access | View, audit trail access | View, audit trail access |
| PROCESS_DESIGNER | Create, modify, retire, soft-delete | Submit for review, deploy to non-prod (after COMPLIANCE approval) | Submit for review, deploy to non-prod (after COMPLIANCE approval — Class A) |
| COMPLIANCE | View, consultation | Review + approve (gateway for new versions), champion/challenger configuration | Review + approve (Class A gate) |
| ARCHITECT | View, consultation | Consultation (for cross-segment impact) | Consultation on versioning strategy (not an approval gate role) |

> **Note (2026-04-24):** This governance table covers Tier 1/2/3 configurability access only. Operational case permissions (e.g., suppression override per ADR-001 DUAL_USE) are governed by the RBAC permission matrix in `RBAC-IMPLEMENTATION-DECISIONS.md`, not by this table. TEAM_LEADER is view-only for configurability governance; TEAM_LEADER holds suppression override permission in the operational case context. DOMAIN_EXPERT was an unprovisioned role — its governance gate function is absorbed by COMPLIANCE. ADMIN is not provisioned — deploy capability is reattributed to PROCESS_DESIGNER.

**Tier 1 workflow:**

1. OPS_MANAGER creates a new vulnerability type or modifies an existing one (label, severity level).
2. Change is persisted to `vulnerability_type` table with `active` flag.
3. Audit log records who made the change and when.
4. The change is effective immediately on new process instances; in-flight instances see the old definition via their snapshot.

**Tier 2 workflow (DMN):**

1. PROCESS_DESIGNER authors a new DMN decision table or modifies an existing one in Flowable Modeler or via XML.
2. PROCESS_DESIGNER submits the DMN for review by selecting "Request Approval" in the admin UI.
3. COMPLIANCE reviews the rule set (impact analysis, coverage testing). If approved, they click "Approve".
4. Approved DMN is deployed to Flowable and a `dmn_deployment` record is created with `is_champion = true`.
5. Audit trail records the approval, effective-from date, and deployed version.
6. Existing in-flight instances continue to use the DMN version recorded in their `process_execution_context`.
7. New instances use the approved DMN.
8. Champion/challenger testing: PROCESS_DESIGNER can mark a new DMN as `is_challenger` and configure traffic split (e.g., 10% of new instances). Analytics track outcomes.

**Tier 3 workflow (BPMN — embedded editor + Class A):**

1. Business admin authors a new BPMN process definition in the embedded Flowable Modeler within the admin UI.
2. On save, the system validates the diagram against the available service task library and produces one of two outcomes:
   - **All steps available:** Diagram proceeds directly to approval workflow.
   - **Steps missing:** Diagram enters `AWAITING_IMPLEMENTATION` state. A developer ticket is raised listing the required service tasks. Once implemented, the diagram returns to the approval workflow.
3. PROCESS_DESIGNER proposes; COMPLIANCE approves (Class A two-person gate). DOMAIN_EXPERT is not a provisioned role — absorbed by COMPLIANCE per RBAC design review (2026-04-24).
4. After Class A approval, the BPMN is deployed to Flowable via the admin API.
5. The `segment_definition` table is updated to point to the new process definition key.
6. New accounts matching the segment criteria are routed to the new pathway; in-flight accounts remain on their original process.

**Audit trail:**

All three tiers write to a unified `configuration_audit_log`:

```
configuration_audit_log
  ├── id (UUID)
  ├── tier (INT: 1, 2, or 3)
  ├── entity_type (VARCHAR) — e.g., "VULNERABILITY_TYPE", "DMN_DEPLOYMENT", "SEGMENT_DEFINITION"
  ├── entity_id (UUID)
  ├── action (VARCHAR) — "CREATE", "UPDATE", "RETIRE", "APPROVE", "DEPLOY"
  ├── change_summary (JSONB) — what changed
  ├── performed_by (VARCHAR, FK to Keycloak user)
  ├── approved_by (VARCHAR NULL, FK to Keycloak user)
  ├── approval_reason (TEXT NULL)
  ├── effective_from (TIMESTAMP)
  ├── performed_at (TIMESTAMP)
  └── correlation_id (VARCHAR) — links related changes across tiers
```

### 7. Tier 2 Rule Content: What Goes into DMN vs. What Stays Hardcoded

**Decision: Action-level rules live in DMN; operational constants live in code.**

**In DMN (Tier 2):**
- Vulnerability level × action code → allowed/blocked/escalated
- Account segment × communication channel → available/unavailable
- Breach tolerance periods (by account type, arrangement status)
- Work allocation queues (by priority and specialisation)
- Escalation triggers and paths

**Hardcoded in Java (not configurable):**
- Calculation of interest accrual (CCA-mandated formula, not a business discretion)
- Payment allocation order (CCA-mandated: arrears, then interest, then principal)
- Regulatory minimum forbearance periods (breathing space duration, statutory rest periods)
- GDPR/DPA compliance rules (data retention, deletion, anonymisation)
- Audit trail immutability enforcement
- PII redaction rules for logs

**Rationale:**

DMN is for *DWP business discretion* — "we've decided this vulnerability level blocks this action, effective next month." Java is for *regulatory mandate and platform integrity* — "the law says interest accrues daily, we cannot make that configurable without risking compliance."

This separates the **operating model** (Tier 2) from the **platform constraints** (code).

### 8. Snapshot Mechanism: Process Execution Context

When a debt account is initiated (segment evaluated, process started):

```
process_execution_context
  ├── id (UUID)
  ├── process_instance_id (VARCHAR) — Flowable process instance ID
  ├── account_id (UUID)
  ├── segment_code (VARCHAR) — the segment assigned
  ├── segment_version_id (UUID) — if versioning segment definitions
  ├── dmn_version_id (UUID) — FK to dmn_deployment (the DMN version active at start)
  ├── process_definition_key (VARCHAR) — e.g., "treatment-vulnerable-on-benefit"
  ├── process_definition_version (INT) — Flowable version number
  ├── vulnerability_type_snapshot (JSONB) — captured vulnerability type definitions
  │   [
  │     { "id": "uuid-1", "code": "HEALTH_CONDITION", "label": "Health condition", "severity": 3 },
  │     { "id": "uuid-2", "code": "ESTRANGED", "label": "Estranged from support", "severity": 2 }
  │   ]
  ├── created_at (TIMESTAMP)
  └── started_at (TIMESTAMP)
```

When a delegate task needs to evaluate a rule (e.g., "block this action for this vulnerability?"):
1. Retrieve the `dmn_version_id` from the process execution context.
2. Call Flowable's DMN engine with `dmn_version_id` as a constraint.
3. Flowable evaluates the decision table from that specific version.
4. The result is recorded in the process audit trail.

This ensures **determinism and auditability** (DW.42): every decision is traceable back to the rule version that produced it.

### 9. UI Surface: Configuration Screens (Conceptual)

**Tier 1 admin screen — Vulnerability Types:**

```
┌─────────────────────────────────────┐
│ Vulnerability Types Configuration   │
├─────────────────────────────────────┤
│ [+ New vulnerability type]           │
├──────────┬──────┬──────┬─────────────┤
│ Code     │Label │ Level│ Regulatory  │
├──────────┼──────┼──────┼─────────────┤
│ HEALTH   │Health│  3   │ FCA_FG21_1  │
│ ESTRANG. │Est.  │  2   │ FCA_FG21_1  │
│ HARDSHIP │Hard. │  2   │ CMA_1806    │
└──────────┴──────┴──────┴─────────────┘
[Edit] [Retire] [View audit history]
```

**Tier 2 admin screen — Decision Table Viewer:**

```
┌──────────────────────────────────────────┐
│ DMN Rules: Vulnerability Action Matrix   │
├──────────────────────────────────────────┤
│ Version: 3 (Live) | v2 (Archived)        │
│ Effective from: 2026-05-01               │
│ Deployed by: policy.admin@dwp            │
├──────────────────────────────────────────┤
│ [Test against batch] [Compare versions]  │
│ [Champion/Challenger] [Simulate]         │
├──────────────────────────────────────────┤
│ Vulnerability│ Action      │ Result       │
├──────────────┼─────────────┼──────────────┤
│ Severe       │ LEGAL_SUIT  │ BLOCKED      │
│              │             │ Escalate:CCC │
│ Moderate     │ SMS_DEMAND  │ ALLOWED      │
│ Mild         │ WRITE_OFF   │ ESCALATE:LEAD│
│ ANY          │ COMMS_SUPP. │ ALLOWED      │
└──────────────┴─────────────┴──────────────┘
[Add rule] [Edit] [Delete] [Request approval]
```

**Tier 3 screen — Process Definitions (read-only to most):**

```
┌──────────────────────────────────────┐
│ Treatment Process Definitions         │
├──────────────────────────────────────┤
│ Segment: VULNERABLE_ON_BENEFIT       │
│ Active definition: v2                │
├──────────────────────────────────────┤
│ [View BPMN diagram] [View history]   │
│ [In-flight instances: 14,782]        │
│ (Admin only) [Upload new BPMN]       │
│ (Admin only) [Request deployment]    │
└──────────────────────────────────────┘
```

### 10. What NOT to Make Configurable

The following must remain hardcoded or policy-driven, not UI configurable:

1. **Regulatory minimums and formulas:**
   - Interest accrual calculation (CCA Act 1974 prescribed rate).
   - Payment allocation order (CCA statutory priority).
   - Breathing space duration (Debt Respite Scheme 2021 — 60 days, non-negotiable).
   - Statutory minimum contact forbearance periods.

2. **Data consistency and integrity rules:**
   - Audit trail immutability.
   - PII redaction enforcement in logs.
   - Encryption at rest for Restricted data.
   - Debt principal non-negativity.

3. **Platform security and reliability:**
   - Keycloak integration configuration.
   - Database connection pooling and timeouts.
   - Flowable engine persistence settings.
   - Retry and backoff exponential parameters (code-based with env override, not UI).

4. **Organisational structure:**
   - Team and queue hierarchies (belongs in a separate `organisational_unit` domain, not configurability).
   - User role definitions (RBAC matrix).
   - Approval authority delegation (handled by Keycloak groups/roles).

5. **Feature toggles and deployment gates:**
   - Feature flag definitions (define in code, enable/disable via env or feature-flag table for very limited cases).
   - CI/CD deployment rules.
   - Release candidate gates.

## Consequences

1. **Three separate UI modules required:**
   - `domain/configuration/tier1` — vulnerability types, categories, action codes, channels (simple CRUD).
   - `domain/configuration/tier2` — DMN rule authoring, version control, approval workflow, testing UI.
   - `domain/configuration/tier3` — BPMN deployment UI (for PROCESS_DESIGNER users with COMPLIANCE-approved Class A gate).

2. **DMN versioning is native to Flowable; no custom versioning engine needed** — reduces infrastructure complexity.

3. **Snapshot-based determinism:**
   - Every process instance carries a record of the configuration it started with.
   - Changes to Tier 1, Tier 2, or Tier 3 never affect in-flight instances retroactively.
   - Audit trail ties every decision back to the rule version that produced it (DW.42).

4. **Class A gate applies only to Tier 3** (BPMN deployment):
   - Tier 1 changes are low-risk and can be deployed by OPS_MANAGER.
   - Tier 2 changes (DMN rules) require COMPLIANCE approval but not full Class A review.
   - Tier 3 changes (new treatment paths) are architectural and require full Class A review.

5. **Champion/challenger testing (DW.2, DW.39) operates at Tier 2 only:**
   - Two DMN versions can run simultaneously with traffic split.
   - Outcome tracking and comparison is an analytics concern (outside this ADR).
   - BPMN versioning follows ADR-006 (no parallel variants).

6. **Effective-dating complexity:**
   - Tier 1 changes are immediate.
   - Tier 2 changes use effective-from dates (old version remains executable until date passes).
   - Tier 3 changes follow Flowable's versioning (new instances use new version, in-flight stay on old).

7. **Dependency chain:**
   - Tier 2 rules reference Tier 1 definitions by ID.
   - Tier 3 processes call Tier 2 DMN via Flowable's native decision task.
   - If a Tier 1 definition is retired (soft-delete), Tier 2 rules that reference it must be updated or disabled.

8. **Data classification (DIC.16):**
   - All Tier 1 and Tier 2 configuration tables holding vulnerability types or severity levels are marked `PII:RESTRICTED`.
   - Access to these screens is gated to PROCESS_DESIGNER, OPS_MANAGER, and COMPLIANCE roles only.
   - Configuration changes are audited with user identity and approval chain.

## Alternatives Rejected

### Monolithic single-tier configurability (Rejected)

Storing all configuration (definitions, rules, process logic) in a single database-driven engine would:
- Require custom rule engine, DMN reimplementation, or BPMN parsing logic in the application.
- Make versioning, champion/challenger testing, and effective-dating harder to implement.
- Remove the ability to version BPMN processes in code and track them in git.

Rejected because it increases complexity and limits Flowable's native capabilities.

### Storing DMN tables as CLOB/JSON in database (Rejected)

Storing `.dmn` files as BLOBs in the database would:
- Lose Flowable's native DMN optimization (pre-compiled decision trees, caching).
- Require custom DMN deployment and versioning logic.
- Make DMN versions untrackable in version control.

Rejected because it reduces observability and performance without commensurate flexibility gain.

### Retaining Tier 1 definitions by enum in Java (Rejected)

Hard-coding vulnerability types as a Java enum would:
- Require a code deployment to add a new vulnerability type.
- Violate DW.16 (frequent changes without code deployments).

Rejected: same as segment taxonomy enum rejection in ADR-004.

### No snapshot mechanism (Rejected)

Allowing in-flight processes to see live configuration changes would:
- Break auditability (DW.42) — a decision made months ago could not be traced back to the rule that produced it.
- Create non-determinism (same input, different output if the rule changed).
- Violate the "in-flight cases must see the config snapshot from when they started" requirement.

Rejected because it introduces unacceptable audit and regulatory risk.

## Implementation Notes

1. **Tier 1 seed data:**
   - Initial vulnerability types and severity levels are defined in a Flyway migration (`V001__INITIAL_VULNERABILITY_TYPES.sql`).
   - Seed data is version-controlled and includes all UK regulatory bases (FCA FG21/1, CMA, CCA, DRS).

2. **Tier 2 DMN deployment:**
   - Initial decision tables are authored in Flowable and committed as `.dmn` files to `backend/src/main/resources/dmn/`.
   - `FlowableProcessEngineConfiguration` deploys DMN files at application startup.
   - Subsequent versions are uploaded via admin API; `ProcessEngineDeploymentListener` records the deployment in the `dmn_deployment` table.

3. **Tier 3 BPMN versioning:**
   - Initial four treatment processes are authored and committed to `backend/src/main/resources/processes/treatment/`.
   - `segment_definition` table is seeded with mappings to these definitions.
   - The shared `standard-event-subprocesses.bpmn` is deployed separately per ADR-004 amendment.

4. **Snapshot capture mechanism:**
   - When `ProcessStartPort.startProcess(...)` is called, a `ProcessStartCommand` handler invokes a service in `domain/strategy` to capture the snapshot.
   - The snapshot is persisted to `process_execution_context` in the same transaction as the process start.
   - If snapshot capture fails, process start is rolled back (fail-fast).

5. **DMN version constraint:**
   - When a Flowable service task evaluates a decision, the task's input parameters include `dmn_version_id`.
   - The Flowable delegate implementation passes `dmn_version_id` to `FlowableDecisionEngine.evaluateDecision(...)` to scope evaluation to a specific version.
   - If the version does not exist, evaluation fails loudly (audit-critical).

6. **Approval workflow gate:**
   - The "Request Approval" button for Tier 2 rules triggers a state change in the `dmn_deployment` record to `approval_requested`.
   - A Keycloak group `@compliance_approvers` is notified (via email/in-app task).
   - COMPLIANCE reviews and clicks "Approve" or "Reject".
   - If approved, the DMN is deployed to Flowable and the state changes to `approved` with an effective-from date.
   - Audit trail records the approval chain.

7. **Class A review gate for Tier 3:**
   - BPMN deployment follows the existing Class A change gate in WAYS-OF-WORKING.md.
   - The `ProcessEngineDeploymentListener` records the deployment but does not activate it until the Class A review is passed.
   - A feature flag or manual approval step controls whether new BPMN versions are immediately used for new instances or require explicit activation.

8. **Test bulk batch execution (DW.39, DW.41):**
   - The admin UI includes a "Test against batch" feature.
   - User selects a Tier 2 DMN version and a batch of account IDs (pre-computed test set).
   - For each account, the system retrieves its input variables (vulnerability type, segment, benefit status, etc.) and runs the DMN against the test version.
   - Results are stored in `dmn_test_result` and displayed in a comparison grid (current version vs. test version).
   - Test results inform approval decisions (e.g., "does the new rule set change outcomes for 50 accounts in unexpected ways?").

## References

- ADR-001: Process instance per debt
- ADR-004: Segment taxonomy and two-tier configurability
- ADR-006: BPMN versioning and in-flight migration
- STD-PLAT-005: Date-effective rules
- STD-SEC-003: Domain data classification (DIC.16 — vulnerability data classification as Restricted)
- STD-GOV-004: Contract versioning
- WAYS-OF-WORKING.md: Class A change gate definition
- Requirements: DW.2, DW.9, DW.16, DW.25, DW.38, DW.39, DW.41, DW.42, DW.44, DW.45, DIC.16, CAS.11

---

## Appendix A: Configuration Lifecycle Examples

### Example 1: Adding a new vulnerability type (Tier 1)

1. OPS_MANAGER logs into admin UI → Configuration → Vulnerability Types.
2. Clicks [+ New vulnerability type].
3. Enters: Code="DOMESTIC_ABUSE", Label="Domestic abuse", Severity=3, Regulatory="FCA_FG21_1".
4. Clicks Save.
5. Database record is created with `active=true`.
6. Audit log records the change.
7. New processes can assign this vulnerability type.
8. In-flight processes ignore the new type (they use their snapshot).

### Example 2: Updating an action matrix rule (Tier 2, DMN)

1. PROCESS_DESIGNER logs into admin UI → Configuration → DMN Rules → Vulnerability Action Matrix.
2. Clicks [Edit rule] for "Severe vulnerability + LEGAL_SUIT = BLOCKED".
3. Changes the rule to also set `escalation_path=SPECIALIST_TEAM`.
4. Saves as a new DMN version (v4).
5. Clicks [Request approval].
6. COMPLIANCE receives notification.
7. COMPLIANCE reviews the change and [Test against batch] on 1,000 historical accounts.
8. Sees that 30 accounts now route differently. Reviews the justification.
9. Approves. DMN v4 is deployed to Flowable with `effective_from=2026-06-01`.
10. From 2026-06-01 onwards, new process instances use v4.
11. Instances started before 2026-06-01 continue to use v3 (recorded in their `process_execution_context`).
12. Audit trail shows the approval chain and effective-from date.

### Example 3: Creating a new treatment pathway (Tier 3, BPMN)

1. Business admin identifies the need: "we need a specialized pathway for cases with mixed vulnerability in joint accounts."
2. Admin opens the embedded Flowable Modeler in the admin UI and designs the treatment path — dragging steps (contact attempt, I&E assessment, escalation, etc.) onto the canvas and connecting them.
3. On save, the system validates the diagram against the service task library:
   - Steps using existing tasks (SendLetterDelegate, EscalateToQueueDelegate, etc.) are marked as available.
   - Any step using a new capability (e.g., JointAccountSplitAssessmentDelegate, not yet implemented) is flagged.
4a. **If all steps available:** Diagram proceeds to Class A approval (step 6).
4b. **If steps missing:** Diagram enters `AWAITING_IMPLEMENTATION` state. Developer is notified with the list of required service tasks. Developer implements them, marks them as available in the service task registry, and the diagram automatically re-validates and moves to approval.
5. PROCESS_DESIGNER proposes; COMPLIANCE approves (Class A two-person gate). DOMAIN_EXPERT is not a provisioned role — absorbed by COMPLIANCE per RBAC design review (2026-04-24).
6. After Class A approval, the BPMN is deployed to Flowable via admin API.
7. New `segment_definition` is created: Code="JOINT_MIXED_VULN", process_definition_key="treatment-joint-mixed-vulnerability".
8. New accounts matching the segment criteria are routed to the new pathway.
9. In-flight accounts on the old pathway remain on their original process.

---

## Appendix B: DMN Decision Table Schema Example

A Tier 2 decision table in DMN XML (Flowable format):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="https://www.omg.org/spec/DMN/20191111/MODEL/"
             xmlns:dmndi="https://www.omg.org/spec/DMN/20191111/DMNDI/"
             xmlns:dc="http://www.omg.org/spec/DMN/20130516/DC"
             xmlns:di="http://www.omg.org/spec/DMN/20130516/DI"
             id="VulnerabilityActionMatrix"
             name="Vulnerability Action Matrix">

  <decision id="vulnerability_action_decision" name="Vulnerability Action Decision">
    <decisionTable id="vulnerability_action_table" hitPolicy="FIRST">
      <input id="vuln_level_input" label="Vulnerability Level">
        <inputExpression typeRef="number">
          <text>vulnerability_level</text>
        </inputExpression>
      </input>
      <input id="action_code_input" label="Action Code">
        <inputExpression typeRef="string">
          <text>action_code</text>
        </inputExpression>
      </input>
      <output id="action_allowed" name="Allowed" typeRef="boolean" />
      <output id="action_escalation" name="Escalation Path" typeRef="string" />

      <rule id="rule_1">
        <description>Severe vulnerability blocks legal action</description>
        <inputEntry id="input_1_1">
          <text>3</text>
        </inputEntry>
        <inputEntry id="input_1_2">
          <text>"LEGAL_SUIT"</text>
        </inputEntry>
        <outputEntry id="output_1_1">
          <text>false</text>
        </outputEntry>
        <outputEntry id="output_1_2">
          <text>"TEAM_LEADER_OVERRIDE"</text>
        </outputEntry>
      </rule>

      <rule id="rule_2">
        <description>Moderate vulnerability allows SMS</description>
        <inputEntry id="input_2_1">
          <text>2</text>
        </inputEntry>
        <inputEntry id="input_2_2">
          <text>"SMS_DEMAND"</text>
        </inputEntry>
        <outputEntry id="output_2_1">
          <text>true</text>
        </outputEntry>
        <outputEntry id="output_2_2">
          <text>null</text>
        </outputEntry>
      </rule>

      <!-- Additional rules... -->
    </decisionTable>
  </decision>
</definitions>
```

The same table is mirrored in the `dmn_decision_table_row` relational table for searchability and UI display.
