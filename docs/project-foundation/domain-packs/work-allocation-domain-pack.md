# Work Allocation & Agent Desktop — Domain Pack

**Domain:** Work Allocation & Agent Desktop
**Package:** `domain/workallocation`
**Status:** Phase 3 complete (design baseline locked) - open policy answers tracked as provisional defaults under DDE-OQ-WA-01 and DDE-OQ-WA-04
**Date:** 2026-04-27
**Author:** Business Analyst Agent
**Domain rulings in scope:** RULING-002 (vulnerability data classification — governs vulnerability flash note content by role), RULING-003 (vulnerability configurability — governs specialist queue routing triggers)
**Requirement IDs covered:** WAM.1–WAM.28, DW.11, DW.31, DW.32, DW.52, DW.80, DW.83, DW.85, UI.21, UI.9, AAD.14, AAD.18, AAD.19, CAS.9

---

## 1. Purpose and Scope

### 1.1 What this domain owns

The `domain/workallocation` package owns:

- **Queue definitions** — the configuration and lifecycle of named work queues, including skill tags, SLA targets, escalation routing, and exception queue designation.
- **Work items** — individual units of work assigned to agents or queues; the authoritative record of what an agent is working, what state it is in, and what SLA applies.
- **Work item history** — the append-only audit log of every status transition and assignment change for a work item.
- **Agent skill profiles** — the mapping of skill tags and workload capacity to individual agents; the runtime availability state of each agent.
- **Agent sessions** — login/logout lifecycle; per-session work completion metrics.
- **SLA tracking** — SLA timer start, warning events, and breach events at the work-item level.
- **Queue routing rule configuration** (Tier 1) — configurable routing rules evaluated to determine which queue an incoming work item enters.
- **Agent availability configuration** (Tier 1) — configurable thresholds governing concurrent item caps and auto-pause behaviour.
- **DMN-driven priority scoring** (Tier 2) — the `work-item-priority.dmn` and `queue-assignment.dmn` decision tables.
- **Supervisor dashboard data** — queue health, agent utilisation, and SLA breach aggregates consumed by the supervisor UI.
- **Agent desktop service** — session management, worklist retrieval, and current item context for the agent-facing UI.

### 1.2 What this domain delegates to other domains

| Concern | Owning domain | Interaction pattern |
|---|---|---|
| Vulnerability flag status and category | `domain/customer` | Read via `VulnerabilityFlagService.hasActiveVulnerabilityFlag()` and `getActiveFlags()` |
| Vulnerability flash note content rules | `domain/customer` + RULING-002 | Content rules are defined in the vulnerability domain pack; this domain triggers the flash note pre-render check |
| Account balance and risk tier | `domain/account` | Read via account query port |
| Repayment arrangement status | `domain/repaymentplan` | Read via arrangement status query port |
| Contact history display | `domain/communications` | Read-only: contact history entries fetched for agent desktop display |
| Segmentation / treatment stage | `domain/strategy` | Read via strategy context port |
| Communication suppression | `domain/communications` | No write dependency from this domain; routing signals are consumed by `domain/customer` via `WorkAllocationPort` |
| I&E review work item creation | `domain/repaymentplan` | Receives `CreateWorkItemCommand` via `DelegateCommandBus`; creates the work item in the appropriate queue |
| Vulnerability review work item creation | `domain/customer` | Receives `CreateWorkItemCommand` via `DelegateCommandBus`; creates the work item in the specialist queue |
| Process engine lifecycle | `infrastructure/process` | Flowable human task lifecycle signals sync to work items via `DelegateCommandBus` |
| Audit event writing | `domain/audit` | All significant transitions write structured audit events |

### 1.3 What this domain explicitly excludes

- **Communication channel selection and suppression** — owned by `domain/communications`.
- **Repayment plan and I&E business logic** — owned by `domain/repaymentplan`.
- **Vulnerability protocol enforcement** (suppression activation, blocked action types) — owned by `domain/customer`.
- **Strategy treatment decisions** — owned by `domain/strategy` and the Flowable process engine.
- **User provisioning and Keycloak management** — owned by `domain/user` and Keycloak.
- **Dialler integration** — referenced in tender (CC.29); not in scope for this pack. If added, it would be a separate integration concern.
- **Customer self-service portal** — confirmed out of scope for current delivery.

---

## 2. Core Data Model

All tables reside in the default PostgreSQL schema. Flyway migration prefix placeholder: `V{n}__`. All primary keys are `UUID` generated at application layer. All timestamps are `TIMESTAMPTZ`. Financial amounts where present are `NUMERIC(14,2)`.

### 2.1 `work_queue`

Queue definitions. Configured by `OPS_MANAGER` or `TEAM_LEADER` via the Tier 1 Foundations admin UI. Changes to queue definitions are effective-dated.

```sql
-- V{n}__create_work_queue.sql
CREATE TABLE work_queue (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    code                    VARCHAR(100)    NOT NULL,
    name                    VARCHAR(255)    NOT NULL,
    description             TEXT,
    skill_tags              JSONB           NOT NULL DEFAULT '[]',
    sla_hours               INTEGER         NOT NULL,
    escalation_queue_id     UUID,
    is_exception_queue      BOOLEAN         NOT NULL DEFAULT FALSE,
    is_active               BOOLEAN         NOT NULL DEFAULT TRUE,
    team_scope              VARCHAR(100),
    -- Restricts visibility to a named team. NULL = visible to all authorised roles.
    effective_from          DATE            NOT NULL,
    effective_to            DATE,
    created_by_user_id      UUID            NOT NULL,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_work_queue PRIMARY KEY (id),
    CONSTRAINT uix_work_queue_code UNIQUE (code),
    CONSTRAINT fk_wq_escalation_queue
        FOREIGN KEY (escalation_queue_id)
        REFERENCES work_queue (id),
    CONSTRAINT chk_wq_sla_hours CHECK (sla_hours > 0),
    CONSTRAINT chk_wq_effective_dates
        CHECK (effective_to IS NULL OR effective_to > effective_from),
    CONSTRAINT chk_wq_no_self_escalation
        CHECK (escalation_queue_id IS NULL OR escalation_queue_id != id)
);

CREATE INDEX idx_wq_is_active ON work_queue (is_active);
CREATE INDEX idx_wq_is_exception ON work_queue (is_exception_queue);
CREATE INDEX idx_wq_escalation ON work_queue (escalation_queue_id);

COMMENT ON COLUMN work_queue.skill_tags IS
    'JSONB array of skill tag strings required to work items in this queue. E.g. ["VULNERABILITY_SPECIALIST","MENTAL_HEALTH"]. Evaluated by WorkItemAssignmentService for skill-match routing.';
COMMENT ON COLUMN work_queue.escalation_queue_id IS
    'Self-referential FK. Null = no escalation path configured. Must not reference itself. Escalation timeout governed by queue_routing_rule configuration.';
COMMENT ON COLUMN work_queue.is_exception_queue IS
    'True for exception-handling queues (DW.85). Items in exception queues suppress follow-on actions until ratified by an exceptions handler.';
COMMENT ON COLUMN work_queue.team_scope IS
    'When non-null, restricts queue visibility to the named team. TEAM_LEADER may only see queues where team_scope matches their team assignment (DDE-OQ-WA-03 pending resolution for cross-team visibility).';
```

### 2.2 `work_item`

Individual units of work, one per Flowable human task (or per manually created work item). The primary operational record for the agent desktop.

```sql
-- V{n}__create_work_item.sql
CREATE TABLE work_item (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    account_id              UUID,
    -- Nullable: some work items are customer-level (e.g., vulnerability review before account assignment)
    customer_id             UUID            NOT NULL,
    queue_id                UUID            NOT NULL,
    assigned_agent_id       UUID,
    -- Null = unassigned (in queue); non-null = assigned to a specific agent
    status                  VARCHAR(30)     NOT NULL,
    priority                INTEGER         NOT NULL DEFAULT 50,
    -- 0 = highest priority, 100 = lowest. Populated by work-item-priority.dmn output.
    item_type               VARCHAR(64)     NOT NULL,
    -- DEBT_COLLECTION | IE_REVIEW | VULNERABILITY_REVIEW | EXCEPTION | MANUAL | DISPUTE_HANDLING | OTHER
    sla_due_at              TIMESTAMPTZ     NOT NULL,
    sla_breached            BOOLEAN         NOT NULL DEFAULT FALSE,
    flowable_task_id        VARCHAR(255),
    -- Flowable human task ID. Null for manually created work items.
    flowable_process_instance_id VARCHAR(255),
    lock_version            BIGINT          NOT NULL DEFAULT 0,
    -- Optimistic lock for concurrent assignment prevention
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_work_item PRIMARY KEY (id),
    CONSTRAINT fk_wi_queue
        FOREIGN KEY (queue_id)
        REFERENCES work_queue (id),
    CONSTRAINT chk_wi_status
        CHECK (status IN (
            'PENDING',
            'ASSIGNED',
            'IN_PROGRESS',
            'ON_HOLD',
            'ESCALATED',
            'COMPLETED',
            'CANCELLED'
        )),
    CONSTRAINT chk_wi_item_type
        CHECK (item_type IN (
            'DEBT_COLLECTION',
            'IE_REVIEW',
            'VULNERABILITY_REVIEW',
            'EXCEPTION',
            'MANUAL',
            'DISPUTE_HANDLING',
            'OTHER'
        )),
    CONSTRAINT chk_wi_priority_range CHECK (priority BETWEEN 0 AND 100),
    CONSTRAINT chk_wi_assigned_status
        CHECK (
            (assigned_agent_id IS NOT NULL AND status IN ('ASSIGNED', 'IN_PROGRESS', 'ON_HOLD'))
            OR (assigned_agent_id IS NULL AND status IN ('PENDING', 'ESCALATED', 'COMPLETED', 'CANCELLED'))
            OR (assigned_agent_id IS NOT NULL AND status IN ('ESCALATED', 'COMPLETED', 'CANCELLED'))
            -- Escalated/completed/cancelled items may retain the last assigned agent for history
        )
);

CREATE INDEX idx_wi_queue_id ON work_item (queue_id);
CREATE INDEX idx_wi_assigned_agent ON work_item (assigned_agent_id);
CREATE INDEX idx_wi_status ON work_item (status);
CREATE INDEX idx_wi_customer_id ON work_item (customer_id);
CREATE INDEX idx_wi_account_id ON work_item (account_id);
CREATE INDEX idx_wi_sla_due_at ON work_item (sla_due_at);
CREATE INDEX idx_wi_sla_breached ON work_item (sla_breached) WHERE sla_breached = TRUE;
CREATE INDEX idx_wi_flowable_task ON work_item (flowable_task_id) WHERE flowable_task_id IS NOT NULL;
CREATE INDEX idx_wi_priority_sla ON work_item (priority ASC, sla_due_at ASC)
    WHERE status IN ('PENDING', 'ASSIGNED');
-- Composite index for the primary worklist ordering query (priority then SLA proximity)

COMMENT ON COLUMN work_item.priority IS
    'Integer priority score 0-100. Lower number = higher priority. Set by work-item-priority.dmn at item creation and updated on re-evaluation. Ties resolved by sla_due_at ascending.';
COMMENT ON COLUMN work_item.lock_version IS
    'Optimistic locking counter. Incremented on every assignment change. Assignment service must specify the expected lock_version to prevent concurrent double-assignment (DW.52).';
COMMENT ON COLUMN work_item.flowable_task_id IS
    'The Flowable engine human task ID corresponding to this work item. Null for work items created outside of a Flowable process (e.g., manually created exceptions). Used to signal task completion back to Flowable.';
```

**Work item status lifecycle:**

| Status | Meaning | Allowed transitions |
|---|---|---|
| `PENDING` | In queue, unassigned | -> `ASSIGNED`, `CANCELLED`, `ESCALATED` |
| `ASSIGNED` | Assigned to an agent, not yet opened | -> `IN_PROGRESS`, `PENDING` (released), `ESCALATED`, `CANCELLED` |
| `IN_PROGRESS` | Agent has opened the item and is actively working it | -> `ON_HOLD`, `COMPLETED`, `ESCALATED`, `PENDING` (released) |
| `ON_HOLD` | Paused pending an external event (e.g., awaiting callback) | -> `IN_PROGRESS`, `ESCALATED`, `CANCELLED` |
| `ESCALATED` | Referred to a higher authority or exception queue | -> `ASSIGNED` (to escalation handler), `COMPLETED`, `CANCELLED` |
| `COMPLETED` | Work done; Flowable task completed (if applicable) | Terminal |
| `CANCELLED` | Work item withdrawn (account closed, DCA placed, etc.) | Terminal |

### 2.3 `work_item_history`

Append-only audit log of all status transitions and assignment changes. No `UPDATE` or `DELETE` permitted.

```sql
-- V{n}__create_work_item_history.sql
CREATE TABLE work_item_history (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    work_item_id    UUID            NOT NULL,
    from_status     VARCHAR(30),
    to_status       VARCHAR(30)     NOT NULL,
    from_agent_id   UUID,
    to_agent_id     UUID,
    from_queue_id   UUID,
    to_queue_id     UUID,
    reason          TEXT,
    -- Required for ESCALATED and reassignment transitions; optional otherwise
    changed_by      UUID            NOT NULL,
    -- User ID of the actor (may be SYSTEM UUID for automated transitions)
    change_source   VARCHAR(30)     NOT NULL,
    -- AGENT_ACTION | SUPERVISOR_ACTION | SYSTEM_AUTOMATED | FLOWABLE_TASK_EVENT | BULK_REASSIGNMENT
    changed_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_work_item_history PRIMARY KEY (id),
    CONSTRAINT fk_wih_work_item
        FOREIGN KEY (work_item_id)
        REFERENCES work_item (id),
    CONSTRAINT chk_wih_to_status
        CHECK (to_status IN (
            'PENDING', 'ASSIGNED', 'IN_PROGRESS', 'ON_HOLD',
            'ESCALATED', 'COMPLETED', 'CANCELLED'
        )),
    CONSTRAINT chk_wih_change_source
        CHECK (change_source IN (
            'AGENT_ACTION',
            'SUPERVISOR_ACTION',
            'SYSTEM_AUTOMATED',
            'FLOWABLE_TASK_EVENT',
            'BULK_REASSIGNMENT'
        ))
);

CREATE INDEX idx_wih_work_item_id ON work_item_history (work_item_id);
CREATE INDEX idx_wih_changed_at ON work_item_history (changed_at);
CREATE INDEX idx_wih_changed_by ON work_item_history (changed_by);

COMMENT ON TABLE work_item_history IS
    'Append-only audit log. Flyway migration must include a trigger rejecting UPDATE and DELETE. Provides the complete provenance trail for any work item.';
```

### 2.4 `agent_skill_profile`

One row per agent. Records skill tags, capacity settings, and live workload state.

```sql
-- V{n}__create_agent_skill_profile.sql
CREATE TABLE agent_skill_profile (
    id                          UUID            NOT NULL DEFAULT gen_random_uuid(),
    agent_user_id               UUID            NOT NULL UNIQUE,
    -- FK to the Keycloak user UUID stored in domain/user
    skill_tags                  JSONB           NOT NULL DEFAULT '[]',
    -- JSONB array of skill tag strings matching queue.skill_tags for matching
    max_concurrent_items        INTEGER         NOT NULL DEFAULT 5,
    current_workload_count      INTEGER         NOT NULL DEFAULT 0,
    is_available                BOOLEAN         NOT NULL DEFAULT FALSE,
    -- Set to TRUE on session start, FALSE on session end or auto-pause
    auto_paused                 BOOLEAN         NOT NULL DEFAULT FALSE,
    -- TRUE when the system has automatically paused allocation due to breach threshold
    last_availability_change_at TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_agent_skill_profile PRIMARY KEY (id),
    CONSTRAINT chk_asp_max_concurrent CHECK (max_concurrent_items > 0),
    CONSTRAINT chk_asp_workload_non_negative CHECK (current_workload_count >= 0),
    CONSTRAINT chk_asp_workload_cap
        CHECK (current_workload_count <= max_concurrent_items OR auto_paused = TRUE)
    -- When auto_paused is TRUE the workload count may exceed max_concurrent_items
    -- transiently while existing items are completed
);

CREATE INDEX idx_asp_agent_user_id ON agent_skill_profile (agent_user_id);
CREATE INDEX idx_asp_is_available ON agent_skill_profile (is_available);

COMMENT ON COLUMN agent_skill_profile.skill_tags IS
    'JSONB array of skill tag strings held by this agent. WorkItemAssignmentService evaluates whether the agent holds all required tags for a given queue before assignment (SKILL_MATCH mode).';
COMMENT ON COLUMN agent_skill_profile.max_concurrent_items IS
    'Configurable per agent. Default loaded from agent_availability_config.default_max_concurrent_items. Hard cap vs. soft advisory behaviour is pending DDE-OQ-WA-02.';
COMMENT ON COLUMN agent_skill_profile.auto_paused IS
    'TRUE when the system has automatically suspended push allocation because the agent has reached the auto_pause_on_breach_count threshold defined in agent_availability_config.';
```

### 2.5 `agent_session`

Records each agent login/logout cycle with per-session productivity metrics.

```sql
-- V{n}__create_agent_session.sql
CREATE TABLE agent_session (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    agent_user_id       UUID            NOT NULL,
    session_start       TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    session_end         TIMESTAMPTZ,
    items_completed     INTEGER         NOT NULL DEFAULT 0,
    queue_ids           JSONB           NOT NULL DEFAULT '[]',
    -- JSONB array of work_queue.id values the agent worked during this session
    logout_reason       VARCHAR(30),
    -- NORMAL | FORCED_LOGOUT | SESSION_TIMEOUT | SYSTEM_EVENT
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_agent_session PRIMARY KEY (id),
    CONSTRAINT chk_as_session_end
        CHECK (session_end IS NULL OR session_end >= session_start),
    CONSTRAINT chk_as_items_completed CHECK (items_completed >= 0),
    CONSTRAINT chk_as_logout_reason
        CHECK (logout_reason IS NULL OR logout_reason IN (
            'NORMAL', 'FORCED_LOGOUT', 'SESSION_TIMEOUT', 'SYSTEM_EVENT'
        ))
);

CREATE INDEX idx_as_agent_user_id ON agent_session (agent_user_id);
CREATE INDEX idx_as_session_start ON agent_session (session_start);
CREATE INDEX idx_as_session_end ON agent_session (session_end);

COMMENT ON COLUMN agent_session.queue_ids IS
    'JSONB array of work_queue.id values for queues this agent was active in during the session. Updated on queue membership change during the session. Used for per-session queue-level reporting.';
COMMENT ON COLUMN agent_session.items_completed IS
    'Count of work items transitioned to COMPLETED during this session. Incremented by WorkItemAssignmentService on each completion event. Granularity of utilisation reporting is pending DDE-OQ-WA-01.';
```

### 2.6 `queue_sla_log`

Append-only log of SLA timer events — warnings and breaches at the work-item level.

```sql
-- V{n}__create_queue_sla_log.sql
CREATE TABLE queue_sla_log (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    work_item_id        UUID            NOT NULL,
    queue_id            UUID            NOT NULL,
    sla_threshold_pct   INTEGER         NOT NULL,
    -- Percentage of SLA elapsed at which this event fired (e.g., 75 for warning, 100 for breach)
    event_type          VARCHAR(20)     NOT NULL,
    -- WARNING | BREACH
    assigned_agent_id   UUID,
    -- Null if unassigned at time of event
    fired_at            TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_queue_sla_log PRIMARY KEY (id),
    CONSTRAINT fk_qsl_work_item
        FOREIGN KEY (work_item_id)
        REFERENCES work_item (id),
    CONSTRAINT fk_qsl_queue
        FOREIGN KEY (queue_id)
        REFERENCES work_queue (id),
    CONSTRAINT chk_qsl_event_type CHECK (event_type IN ('WARNING', 'BREACH')),
    CONSTRAINT chk_qsl_threshold_pct CHECK (sla_threshold_pct BETWEEN 1 AND 100)
);

CREATE INDEX idx_qsl_work_item_id ON queue_sla_log (work_item_id);
CREATE INDEX idx_qsl_queue_id ON queue_sla_log (queue_id);
CREATE INDEX idx_qsl_fired_at ON queue_sla_log (fired_at);
CREATE INDEX idx_qsl_event_type ON queue_sla_log (event_type);

COMMENT ON TABLE queue_sla_log IS
    'Append-only SLA event log. Used to populate WAM.26 SLA breach reporting and supervisor dashboard breach counts. WARNING events fire at the configured sla_warning_threshold_pct from agent_availability_config; BREACH events fire when sla_threshold_pct = 100.';
```

---

## 3. Tier 1 Foundations Configuration Tables

Managed by `OPS_MANAGER` via the admin UI. Changes are effective-dated and governed by the policy bundle mechanism (ADR-009).

### 3.1 `queue_routing_rule`

Configurable routing rules that determine which queue an incoming work item should enter, and whether it should be escalated immediately.

```sql
-- V{n}__create_queue_routing_rule.sql
CREATE TABLE queue_routing_rule (
    id                              UUID            NOT NULL DEFAULT gen_random_uuid(),
    rule_name                       VARCHAR(255)    NOT NULL,
    description                     TEXT,
    priority_order                  INTEGER         NOT NULL,
    -- Evaluation order (ascending). First matching rule wins (mirrors FIRST hit policy of work-item-priority.dmn).
    account_segment                 VARCHAR(64),
    -- Null = matches any segment. Maps to strategy segmentation output.
    account_risk_tier               VARCHAR(20),
    -- NULL | LOW | MEDIUM | HIGH | CRITICAL
    vulnerability_flag_active       BOOLEAN,
    -- Null = not evaluated; TRUE = requires active vulnerability flag; FALSE = requires no active flag
    arrangement_status              VARCHAR(30),
    -- NULL | NONE | ACTIVE | BREACHED
    days_delinquent_min             INTEGER,
    days_delinquent_max             INTEGER,
    target_queue_code               VARCHAR(100)    NOT NULL,
    escalate_immediately            BOOLEAN         NOT NULL DEFAULT FALSE,
    escalation_threshold_hours      INTEGER,
    -- Hours before auto-escalation fires. Null = use queue default sla_hours.
    required_skill_tags             JSONB           NOT NULL DEFAULT '[]',
    is_active                       BOOLEAN         NOT NULL DEFAULT TRUE,
    effective_from                  DATE            NOT NULL,
    effective_to                    DATE,
    created_by_user_id              UUID            NOT NULL,
    created_at                      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_qrr PRIMARY KEY (id),
    CONSTRAINT uix_qrr_priority_active UNIQUE (priority_order, effective_from),
    CONSTRAINT chk_qrr_risk_tier
        CHECK (account_risk_tier IS NULL OR account_risk_tier IN (
            'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'
        )),
    CONSTRAINT chk_qrr_arrangement_status
        CHECK (arrangement_status IS NULL OR arrangement_status IN (
            'NONE', 'ACTIVE', 'BREACHED'
        )),
    CONSTRAINT chk_qrr_delinquency_range
        CHECK (
            (days_delinquent_min IS NULL AND days_delinquent_max IS NULL)
            OR (days_delinquent_min IS NOT NULL AND days_delinquent_max IS NOT NULL
                AND days_delinquent_min <= days_delinquent_max)
        ),
    CONSTRAINT chk_qrr_effective_dates
        CHECK (effective_to IS NULL OR effective_to > effective_from)
);

CREATE INDEX idx_qrr_priority_order ON queue_routing_rule (priority_order);
CREATE INDEX idx_qrr_is_active ON queue_routing_rule (is_active);
CREATE INDEX idx_qrr_effective ON queue_routing_rule (effective_from, effective_to);

COMMENT ON COLUMN queue_routing_rule.priority_order IS
    'Ascending integer. The routing engine evaluates rules in this order and applies the first matching rule (consistent with FIRST hit policy in Tier 2 DMN). Ties are not permitted within the same effective date range.';
COMMENT ON COLUMN queue_routing_rule.required_skill_tags IS
    'JSONB array of skill tags that the target queue must require for this rule to be applicable. Prevents routing to a queue for which no agent holds the required skills.';
```

### 3.2 `agent_availability_config`

Configurable thresholds governing agent capacity and auto-pause behaviour. A single active row drives system defaults; per-queue or per-team overrides may be added as additional rows.

```sql
-- V{n}__create_agent_availability_config.sql
CREATE TABLE agent_availability_config (
    id                              UUID            NOT NULL DEFAULT gen_random_uuid(),
    config_scope                    VARCHAR(30)     NOT NULL DEFAULT 'GLOBAL',
    -- GLOBAL | QUEUE | TEAM
    scope_reference_id              UUID,
    -- Null for GLOBAL; queue_id or team reference for scoped rows
    default_max_concurrent_items    INTEGER         NOT NULL DEFAULT 5,
    skill_tier_max_concurrent       JSONB,
    -- JSONB object keyed by skill tier name with integer cap values.
    -- E.g. {"VULNERABILITY_SPECIALIST": 3, "STANDARD": 8}
    auto_pause_on_breach_count      INTEGER         NOT NULL DEFAULT 3,
    -- Number of SLA breaches within a session before auto-pause triggers
    sla_warning_threshold_pct       INTEGER         NOT NULL DEFAULT 75,
    -- Percentage of SLA elapsed at which a WARNING event is fired
    push_allocation_enabled         BOOLEAN         NOT NULL DEFAULT TRUE,
    -- When FALSE, allocation mode defaults to agent self-select (pull)
    effective_from                  DATE            NOT NULL,
    effective_to                    DATE,
    is_active                       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_by_user_id              UUID            NOT NULL,
    created_at                      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_aac PRIMARY KEY (id),
    CONSTRAINT chk_aac_scope
        CHECK (config_scope IN ('GLOBAL', 'QUEUE', 'TEAM')),
    CONSTRAINT chk_aac_max_concurrent CHECK (default_max_concurrent_items > 0),
    CONSTRAINT chk_aac_breach_count CHECK (auto_pause_on_breach_count > 0),
    CONSTRAINT chk_aac_warning_pct CHECK (sla_warning_threshold_pct BETWEEN 1 AND 99),
    CONSTRAINT chk_aac_effective_dates
        CHECK (effective_to IS NULL OR effective_to > effective_from)
);

COMMENT ON COLUMN agent_availability_config.skill_tier_max_concurrent IS
    'Optional per-skill-tier capacity overrides. Takes precedence over default_max_concurrent_items for agents matching the skill tier. Whether this is enforced as a hard cap or soft advisory is pending DDE-OQ-WA-02.';
COMMENT ON COLUMN agent_availability_config.auto_pause_on_breach_count IS
    'When an agent accumulates this many SLA breaches within a single session, push allocation to that agent is automatically suspended and auto_paused is set TRUE on agent_skill_profile. The agent must manually resume or a TEAM_LEADER must resume on their behalf.';
```

---

## 4. Tier 2 DMN Tables

Both DMN tables are deployed and versioned via the Flowable DMN engine. Changes are Class A changes requiring `COMPLIANCE` and `PROCESS_DESIGNER` approval before deployment (ADR-008 §4, ADR-010 governance gate).

### 4.1 `work-item-priority.dmn`

**Decision key:** `work-item-priority`
**Hit policy:** FIRST (highest-priority condition first; first matching rule wins)
**Purpose:** Determines the priority score (0–100), target queue, and whether a work item should be immediately escalated.

| Input: `accountRiskTier` (String: LOW/MEDIUM/HIGH/CRITICAL) | Input: `vulnerabilityActive` (Boolean) | Input: `slaProximityPct` (Integer: 0–100) | Input: `arrangementStatus` (String: NONE/ACTIVE/BREACHED) | Input: `daysDelinquent` (Integer) | Output: `priorityScore` (Integer) | Output: `targetQueueName` (String) | Output: `escalateImmediately` (Boolean) |
|---|---|---|---|---|---|---|---|
| `CRITICAL` | `true` | any | any | any | `0` | `SPECIALIST_ESCALATION` | `true` |
| `CRITICAL` | `false` | any | any | any | `5` | `HIGH_RISK_QUEUE` | `true` |
| `HIGH` | `true` | any | any | any | `10` | `SPECIALIST_VULNERABILITY` | `true` |
| `HIGH` | `false` | any | `BREACHED` | any | `15` | `HIGH_RISK_QUEUE` | `false` |
| `HIGH` | `false` | `>= 75` | any | any | `20` | `HIGH_RISK_QUEUE` | `false` |
| `HIGH` | `false` | any | any | any | `25` | `HIGH_RISK_QUEUE` | `false` |
| `MEDIUM` | `true` | any | any | any | `30` | `SPECIALIST_VULNERABILITY` | `false` |
| `MEDIUM` | `false` | any | `BREACHED` | any | `35` | `STANDARD_QUEUE` | `false` |
| `MEDIUM` | `false` | `>= 75` | any | any | `40` | `STANDARD_QUEUE` | `false` |
| `MEDIUM` | `false` | any | any | `>= 90` | `45` | `STANDARD_QUEUE` | `false` |
| `MEDIUM` | `false` | any | any | any | `50` | `STANDARD_QUEUE` | `false` |
| `LOW` | `true` | any | any | any | `40` | `SPECIALIST_VULNERABILITY` | `false` |
| `LOW` | `false` | any | `BREACHED` | any | `55` | `STANDARD_QUEUE` | `false` |
| `LOW` | `false` | any | any | any | `70` | `STANDARD_QUEUE` | `false` |

**Default (catch-all if no rule matches):** `priorityScore = 80`, `targetQueueName = STANDARD_QUEUE`, `escalateImmediately = false`.

**Notes:**
- Queue name output values are Tier 1 `work_queue.code` values. The codes above are illustrative; actual codes are defined in Tier 1 configuration and must be seeded before DMN deployment.
- `slaProximityPct` is calculated by `SLATrackingService` as `(elapsed_hours / sla_hours) * 100` at the time of priority evaluation.
- `vulnerabilityActive` is read from `VulnerabilityFlagService.hasActiveVulnerabilityFlag(customerId)` — this method returns a boolean with no category detail (RULING-002 guardrail).
- This DMN table is illustrative pending DWP confirmation of queue taxonomy (DDE-OQ-WA-04 and DWP policy on queue naming).

### 4.2 `queue-assignment.dmn`

**Decision key:** `queue-assignment`
**Hit policy:** FIRST
**Purpose:** Determines the assignment mode to apply when selecting an agent for a work item from a given queue.

| Input: `requiredSkillTagsCount` (Integer) | Input: `availableAgentsWithSkillMatch` (Integer) | Input: `currentWorkloadPct` (Integer: 0–100) | Output: `assignmentMode` (String) |
|---|---|---|---|
| `> 0` | `>= 1` | `< 80` | `SKILL_MATCH` |
| `> 0` | `>= 1` | `>= 80` | `SKILL_MATCH` |
| `> 0` | `0` | any | `MANUAL` |
| `0` | any | `< 60` | `LOWEST_WORKLOAD` |
| `0` | any | `>= 60 and < 85` | `ROUND_ROBIN` |
| `0` | any | `>= 85` | `MANUAL` |

**Assignment mode semantics:**

| Mode | Behaviour |
|---|---|
| `SKILL_MATCH` | Assign to the available agent with matching skill tags who has the lowest current workload count. |
| `LOWEST_WORKLOAD` | Assign to the available agent (any skill) with the lowest current_workload_count. |
| `ROUND_ROBIN` | Distribute to available agents in round-robin order regardless of current workload. |
| `MANUAL` | No automatic assignment. Work item remains `PENDING` in queue; supervisor or agent self-selects. |

**Notes:**
- `currentWorkloadPct` is the aggregate across all available agents: `(sum of current_workload_count) / (sum of max_concurrent_items) * 100`.
- `MANUAL` mode is always the fallback when no eligible agent is available — it must never result in a work item being lost or silently dropped.

---

## 5. Java Service Interfaces

All interfaces are defined in `domain/workallocation`. No Flowable imports. Process engine interaction is via `shared/process/port.DelegateCommandBus` and `shared/process/port.ProcessEventPort` only.

### 5.1 `WorkQueueService`

```java
package domain.workallocation.service;

/**
 * Manages the lifecycle of work queues and evaluates routing rules for
 * incoming work items. Queues are Tier 1 configuration; changes are
 * effective-dated and governed by policy bundles (ADR-009).
 */
public interface WorkQueueService {

    /**
     * Creates a new queue definition. Only OPS_MANAGER may call this.
     * Validates that the escalation_queue_id references an existing active queue.
     * Writes an AUDIT_EVENT of type WORK_QUEUE_CREATED.
     *
     * @throws QueueCodeAlreadyExistsException if the queue code is already in use.
     * @throws EscalationQueueNotFoundException if escalation_queue_id does not exist.
     */
    WorkQueue createQueue(CreateWorkQueueCommand command);

    /**
     * Updates an existing queue definition. Creates a new effective-dated version;
     * does not mutate the existing row. In-flight work items retain their original queue_id;
     * new items entering the queue after the effective_from date use the updated definition.
     *
     * Only OPS_MANAGER or TEAM_LEADER may call this.
     */
    WorkQueue updateQueue(UUID queueId, UpdateWorkQueueCommand command);

    /**
     * Deactivates a queue. An active queue with PENDING or ASSIGNED items may not be
     * deactivated until all items are resolved or moved. Throws
     * QueueHasActiveItemsException if active items remain.
     */
    void deactivateQueue(UUID queueId, UUID deactivatedByUserId, String reason);

    /**
     * Returns the active queue definition for the given code as of today.
     *
     * @throws QueueNotFoundException if no active queue with the given code exists.
     */
    WorkQueue getByCode(String queueCode);

    /**
     * Evaluates the ordered set of active queue_routing_rule rows against the given
     * routing context and returns the first matching rule. If no rule matches, returns
     * the configured default queue (STANDARD_QUEUE).
     *
     * This is called by WorkItemAssignmentService.intakeWorkItem() to determine
     * target queue on creation.
     */
    RoutingDecision evaluateRoutingRules(WorkItemRoutingContext context);

    /**
     * Returns all active queues visible to the caller's role and team scope.
     * TEAM_LEADER receives only queues matching their team_scope.
     * OPS_MANAGER receives all queues.
     */
    List<WorkQueue> getVisibleQueues(UUID callerUserId, UserRole callerRole, String callerTeamScope);
}
```

### 5.2 `WorkItemAssignmentService`

```java
package domain.workallocation.service;

/**
 * Manages the lifecycle of work items — creation (intake), assignment,
 * reassignment, release, completion, and cancellation.
 *
 * All state transitions are recorded in work_item_history.
 * Concurrent assignment prevention uses optimistic locking on work_item.lock_version.
 */
public interface WorkItemAssignmentService {

    /**
     * Creates a new work item from an incoming request (from a Flowable delegate,
     * a domain command, or a supervisor action). Evaluates routing rules via
     * WorkQueueService.evaluateRoutingRules() to determine the target queue.
     * Invokes work-item-priority.dmn to set initial priority_score and sla_due_at.
     *
     * If escalate_immediately = true from DMN, the item is created with status ESCALATED
     * and placed directly into the escalation queue.
     *
     * Writes PENDING to work_item_history.
     *
     * @throws QueueNotFoundException if the target queue does not exist or is inactive.
     */
    WorkItem intakeWorkItem(CreateWorkItemCommand command);

    /**
     * Assigns a PENDING or ASSIGNED work item to a specific agent.
     * Validates that the agent holds the required skill tags for the target queue.
     * Uses optimistic locking (lock_version) to prevent concurrent double-assignment.
     * Updates agent_skill_profile.current_workload_count.
     * Writes ASSIGNED to work_item_history.
     *
     * @throws AgentNotAvailableException if agent is_available = false.
     * @throws AgentSkillMismatchException if agent does not hold required skill tags.
     * @throws WorkItemLockException if lock_version does not match (concurrent conflict).
     * @throws AgentAtCapacityException if current_workload_count >= max_concurrent_items
     *         (when hard cap mode is configured — pending DDE-OQ-WA-02).
     */
    WorkItem assignToAgent(UUID workItemId, UUID agentUserId, UUID assignedByUserId, long expectedLockVersion);

    /**
     * Automatically assigns eligible PENDING items in the given queue based on
     * the assignment mode returned by queue-assignment.dmn.
     * Called by the SLA tracking scheduler and on agent availability change.
     *
     * Returns the number of items assigned.
     */
    int runAutoAssignment(UUID queueId);

    /**
     * Reassigns a work item from its current agent to a new agent or queue.
     * Only TEAM_LEADER (within their team) or OPS_MANAGER (any) may reassign.
     * Writes ASSIGNED (or PENDING if returned to queue) to work_item_history.
     * Reason is mandatory for supervisor-initiated reassignments.
     */
    WorkItem reassign(ReassignWorkItemCommand command);

    /**
     * Releases a work item from the current agent back to the PENDING state in its queue.
     * The releasing agent must be the assigned agent, or the caller must be TEAM_LEADER or OPS_MANAGER.
     * Decrements agent_skill_profile.current_workload_count.
     * Writes PENDING to work_item_history.
     */
    WorkItem release(UUID workItemId, UUID releasedByUserId, String reason);

    /**
     * Marks a work item as IN_PROGRESS. Called when the agent opens the case in the
     * agent desktop. Only the assigned agent or a supervisor may call this.
     * Writes IN_PROGRESS to work_item_history.
     */
    WorkItem startWork(UUID workItemId, UUID agentUserId);

    /**
     * Completes a work item. If the item has a flowable_task_id, dispatches
     * CompleteWorkItemCommand via DelegateCommandBus to signal Flowable task completion.
     * Decrements agent_skill_profile.current_workload_count.
     * Increments agent_session.items_completed for the current session.
     * Writes COMPLETED to work_item_history.
     *
     * @throws WorkItemAlreadyCompletedException if status is already COMPLETED or CANCELLED.
     */
    WorkItem complete(UUID workItemId, UUID completedByUserId, String completionNote);

    /**
     * Escalates a work item to the configured escalation queue for the current queue.
     * If no escalation queue is configured, throws EscalationQueueNotConfiguredException.
     * Dispatches EscalateWorkItemCommand via DelegateCommandBus.
     * Writes ESCALATED to work_item_history with the escalation reason.
     *
     * WAM.24 requirement: escalation must include a system-generated authority check
     * (e.g., arrangement outside delegated authority thresholds).
     */
    WorkItem escalate(UUID workItemId, UUID escalatedByUserId, String escalationReason);

    /**
     * Cancels a work item (e.g., account closed, DCA placed, dispute raised).
     * If the item has a flowable_task_id, Flowable must be notified via command bus.
     * Writes CANCELLED to work_item_history.
     */
    WorkItem cancel(UUID workItemId, UUID cancelledByUserId, String cancellationReason);

    /**
     * Returns whether the given agent holds all skill tags required by the given queue.
     */
    boolean agentMatchesQueueSkills(UUID agentUserId, UUID queueId);

    /**
     * Performs a bulk reassignment from one agent or queue to another.
     * Only OPS_MANAGER may call this.
     * Each item in the bulk operation writes its own work_item_history row with
     * change_source = BULK_REASSIGNMENT.
     * Returns a BulkReassignmentResult containing counts of succeeded, skipped, and failed items.
     */
    BulkReassignmentResult bulkReassign(BulkReassignCommand command);
}
```

### 5.3 `SLATrackingService`

```java
package domain.workallocation.service;

/**
 * Manages application-level SLA timers for work items.
 *
 * SLA tracking is implemented at application level rather than via Flowable timer
 * boundary events. Rationale: work items may exist outside of a Flowable process
 * instance (manually created exceptions, agent self-created items). Centralising
 * SLA enforcement in the application tier ensures uniform tracking regardless of
 * whether a Flowable process is associated. See section 6.3 for full rationale.
 */
public interface SLATrackingService {

    /**
     * Starts the SLA timer for a newly created work item.
     * Sets work_item.sla_due_at = NOW() + queue.sla_hours.
     * No queue_sla_log entry is written at start — only at WARNING and BREACH.
     *
     * Called by WorkItemAssignmentService.intakeWorkItem().
     */
    void startSLA(UUID workItemId);

    /**
     * Updates the SLA due time when a work item is moved to a different queue.
     * The new sla_due_at = MAX(original_sla_due_at, NOW() + new_queue.sla_hours).
     * This prevents a queue move from being used to artificially extend the SLA.
     *
     * Writes an AUDIT_EVENT of type WORK_ITEM_SLA_UPDATED.
     */
    void updateSLAOnQueueChange(UUID workItemId, UUID newQueueId);

    /**
     * Evaluates the SLA status of all non-terminal work items and fires
     * WARNING and BREACH events as appropriate.
     *
     * Called by the application-level scheduler (Spring @Scheduled) at a
     * configurable interval (default: every 5 minutes).
     *
     * For each item where elapsed_pct >= sla_warning_threshold_pct and no WARNING
     * has been fired: writes a WARNING queue_sla_log entry.
     *
     * For each item where elapsed_pct >= 100 and sla_breached = FALSE:
     * sets sla_breached = TRUE on work_item, writes a BREACH queue_sla_log entry,
     * and emits an SLA_BREACH_EVENT via ProcessEventPort if a flowable_process_instance_id exists.
     */
    void evaluateSLATimers();

    /**
     * Returns the current SLA status for a work item.
     * Returns: sla_due_at, elapsed_pct, is_warning, is_breached.
     */
    SLAStatus getSLAStatus(UUID workItemId);

    /**
     * Pauses the SLA clock for a work item placed ON_HOLD.
     * Records the pause start time. elapsed_hours does not accumulate while paused.
     * Whether SLA pause is permitted and its configurable threshold is pending DDE-OQ-WA-04.
     */
    void pauseSLA(UUID workItemId);

    /**
     * Resumes the SLA clock when a work item moves from ON_HOLD back to IN_PROGRESS or ASSIGNED.
     * sla_due_at is recalculated as: (original_sla_due_at - elapsed_before_pause) + NOW().
     */
    void resumeSLA(UUID workItemId);

    /**
     * Returns all work items currently in SLA breach for a given queue.
     * Used by SupervisorDashboardService for breach summary tiles.
     */
    List<WorkItem> getBreachedItems(UUID queueId);
}
```

### 5.4 `AgentDesktopService`

```java
package domain.workallocation.service;

/**
 * Powers the agent-facing desktop UI. Manages agent session lifecycle,
 * worklist retrieval, and current item context.
 *
 * This service coordinates reads across work_item, agent_skill_profile,
 * and agent_session. It does not write to domains outside workallocation —
 * cross-domain data (contact history, account balance, vulnerability status)
 * is fetched via read ports and assembled by the agent desktop API layer.
 */
public interface AgentDesktopService {

    /**
     * Starts an agent session. Sets agent_skill_profile.is_available = TRUE.
     * Creates a new agent_session row with session_start = NOW().
     * Triggers auto-assignment evaluation for queues the agent is eligible for.
     *
     * @throws AgentSessionAlreadyActiveException if an open session exists for the agent.
     */
    AgentSession startSession(UUID agentUserId, List<UUID> initialQueueIds);

    /**
     * Ends an agent session. Sets agent_skill_profile.is_available = FALSE.
     * Sets agent_session.session_end = NOW().
     * Releases all ASSIGNED items (not yet IN_PROGRESS) back to PENDING.
     * Items that are IN_PROGRESS are left assigned to the agent for continuity.
     *
     * @throws AgentSessionNotFoundException if no active session exists.
     */
    AgentSession endSession(UUID agentUserId, String logoutReason);

    /**
     * Returns the agent's current worklist: all work items in status ASSIGNED or
     * IN_PROGRESS for the given agent, ordered by priority ASC, sla_due_at ASC.
     *
     * For each item, includes: customer name, account reference, debt balance (from
     * domain/account read port), item type, priority score, sla_due_at, sla_breached,
     * and a vulnerability_active boolean (from VulnerabilityFlagService.hasActiveVulnerabilityFlag).
     *
     * Does NOT include vulnerability category detail (RULING-002 guardrail for AGENT role).
     */
    List<WorklistItem> getWorklist(UUID agentUserId);

    /**
     * Returns the full work item context for display on the agent case screen.
     * Assembles: work item detail, customer summary (from domain/customer), account
     * summary (from domain/account), latest arrangement status (from domain/repaymentplan),
     * and whether a vulnerability flash note must fire (from domain/customer).
     *
     * Does NOT return vulnerability category detail for AGENT role callers (RULING-002).
     *
     * @throws WorkItemNotFoundException if the work item does not exist.
     * @throws WorkItemAccessDeniedException if the caller does not have RBAC access
     *         to the queue containing the item.
     */
    WorkItemContext getWorkItemContext(UUID workItemId, UUID callerUserId, UserRole callerRole);

    /**
     * Records that the agent has acknowledged the vulnerability flash note for a given
     * work item / account open event. Writes an AUDIT_EVENT of type
     * VULNERABILITY_FLASH_NOTE_ACKNOWLEDGED.
     *
     * The account detail view must not be rendered until this method has been successfully
     * called (UI.21 requirement; see vulnerability domain pack section 9).
     */
    void acknowledgeVulnerabilityFlashNote(UUID workItemId, UUID agentUserId, List<UUID> activeFlagIds);

    /**
     * Returns whether auto-assignment is paused for the given agent (auto_paused = TRUE).
     * Used by the UI to display an availability status indicator.
     */
    boolean isAgentAutoPaused(UUID agentUserId);

    /**
     * Manually resumes auto-assignment for an auto-paused agent.
     * Only the agent themselves or a TEAM_LEADER / OPS_MANAGER may call this.
     * Sets agent_skill_profile.auto_paused = FALSE and is_available = TRUE.
     * Writes AUDIT_EVENT of type AGENT_AUTO_PAUSE_LIFTED.
     */
    void resumeAgentAvailability(UUID agentUserId, UUID resumedByUserId);
}
```

### 5.5 `SupervisorDashboardService`

```java
package domain.workallocation.service;

/**
 * Powers the supervisor dashboard. Aggregates queue health, agent utilisation,
 * and SLA breach data for TEAM_LEADER and OPS_MANAGER roles.
 *
 * All read operations enforce team scope: TEAM_LEADER receives data only for
 * queues and agents within their team_scope. OPS_MANAGER receives all data.
 * (Cross-team supervisor visibility scope pending DDE-OQ-WA-03.)
 */
public interface SupervisorDashboardService {

    /**
     * Returns queue health tiles for the supervisor's visible queue set.
     * Each tile contains: queue_code, queue_name, queue_depth (PENDING count),
     * assigned_count, sla_breach_count_today, agents_online.
     *
     * Refreshed in near-real-time (WAM.4 requirement).
     */
    List<QueueHealthTile> getQueueHealthTiles(UUID supervisorUserId, UserRole supervisorRole, String teamScope);

    /**
     * Returns per-agent utilisation rows for the supervisor's team (or all teams for OPS_MANAGER).
     * Each row contains: agent_user_id, display_name, items_in_progress, items_completed_today,
     * sla_breach_count_today, is_available, auto_paused, current_session_start.
     *
     * Granularity of "today" vs. per-shift is pending DDE-OQ-WA-01.
     */
    List<AgentUtilisationRow> getAgentUtilisation(UUID supervisorUserId, UserRole supervisorRole, String teamScope);

    /**
     * Returns a summary of all SLA breaches today for the supervisor's visible queues.
     * Includes: total_breached_today, by_queue breakdown, oldest_breached_item_age_hours.
     */
    SLABreachSummary getSLABreachSummary(UUID supervisorUserId, UserRole supervisorRole, String teamScope);

    /**
     * Bulk-reassigns all PENDING and ASSIGNED items from one queue to another.
     * Only OPS_MANAGER may call this method.
     * Writes a BULK_REASSIGNMENT audit record.
     * Delegates to WorkItemAssignmentService.bulkReassign() for individual item operations.
     *
     * @throws QueueNotFoundException if source or target queue does not exist.
     * @throws InsufficientAuthorityException if caller is not OPS_MANAGER.
     */
    BulkReassignmentResult reassignQueue(UUID sourceQueueId, UUID targetQueueId, UUID supervisorUserId, String reason);

    /**
     * Pauses incoming work allocation to a queue (no new items are pushed to agents).
     * Existing IN_PROGRESS items are unaffected.
     * Only OPS_MANAGER may call this.
     * Writes AUDIT_EVENT of type QUEUE_PAUSED.
     */
    void pauseQueue(UUID queueId, UUID supervisorUserId, String reason);

    /**
     * Resumes a paused queue, re-enabling push allocation.
     * Only OPS_MANAGER may call this.
     * Writes AUDIT_EVENT of type QUEUE_RESUMED.
     */
    void resumeQueue(UUID queueId, UUID supervisorUserId);

    /**
     * Force-escalates all PENDING and ASSIGNED items in a queue that have exceeded
     * a specified SLA percentage threshold. Only OPS_MANAGER may call this.
     * Each escalated item follows the standard escalate() path in WorkItemAssignmentService.
     *
     * Returns the count of items escalated.
     */
    int forceEscalateBreachedItems(UUID queueId, int slaThresholdPct, UUID supervisorUserId, String reason);

    /**
     * Generates a worklist report for a queue over a given date range.
     * Returns: item counts by status, by disposition type, completed/not-contacted/wrong-party/dated-out.
     * WAM.6 requirement.
     */
    WorklistReport generateWorklistReport(UUID queueId, LocalDate fromDate, LocalDate toDate, UUID requestedByUserId);
}
```

---

## 6. Flowable Integration

### 6.1 Human task to work item mapping

Each Flowable human task in a BPMN process instance corresponds to exactly one `work_item` row. The mapping is maintained via `work_item.flowable_task_id`.

**Creation path:**
1. A Flowable process reaches a `<userTask>` activity.
2. The `CreateWorkItemDelegate` (in `infrastructure/process`) is invoked by the task assignment listener.
3. `CreateWorkItemDelegate` dispatches a `CreateWorkItemCommand` via `DelegateCommandBus`.
4. `WorkItemAssignmentService.intakeWorkItem()` receives the command, evaluates routing and priority DMN, creates the `work_item` record, and sets `flowable_task_id` = Flowable task ID.
5. The work item ID and queue assignment are stored as Flowable process variables for downstream delegates.

**Completion path:**
1. The agent completes the work item via the agent desktop.
2. `WorkItemAssignmentService.complete()` is called.
3. If `flowable_task_id` is non-null, a `CompleteWorkItemCommand` is dispatched via `DelegateCommandBus`.
4. The `CompleteWorkItemDelegate` in `infrastructure/process` calls `taskService.complete(flowableTaskId, variables)`, advancing the process token.

**Escalation path:**
1. `WorkItemAssignmentService.escalate()` is called.
2. An `EscalateWorkItemCommand` is dispatched via `DelegateCommandBus`.
3. The `EscalateWorkItemDelegate` in `infrastructure/process` signals the Flowable process with the `WORK_ITEM_ESCALATED` signal event, allowing the BPMN to define escalation sub-process behaviour.

### 6.2 DelegateCommandBus commands

| Command | Direction | Purpose |
|---|---|---|
| `CreateWorkItemCommand` | Process -> Domain | Instructs the domain to create a work item for a Flowable human task |
| `CompleteWorkItemCommand` | Domain -> Process | Instructs Flowable to complete the human task corresponding to the work item |
| `EscalateWorkItemCommand` | Domain -> Process | Signals Flowable of a work item escalation event |
| `CancelWorkItemCommand` | Domain -> Process | Instructs Flowable to cancel/interrupt the human task (e.g., on account closure) |
| `SetSLADueCommand` | Domain internal | Internal: not a Flowable command. SLA tracking is application-level (see 6.3) |

### 6.3 SLA timer architecture: application-level vs. Flowable timer boundary events

**Decision:** SLA timers are implemented at application level using a Spring `@Scheduled` job calling `SLATrackingService.evaluateSLATimers()`. Flowable timer boundary events are not used for SLA tracking.

**Rationale:**

1. **Work items outside Flowable processes.** Manually created work items (exceptions, agent-initiated tasks, bulk-imported items) do not have a `flowable_process_instance_id`. A Flowable timer boundary event cannot fire for items that exist outside the engine. Application-level SLA evaluation covers all items uniformly.

2. **SLA clock management across queue moves.** When a work item is moved between queues, the SLA parameters change. Resetting a Flowable timer boundary event mid-flight requires process variable manipulation and re-evaluation of the boundary event, which is fragile across inflight migrations (ADR-006). Application-level tracking recalculates `sla_due_at` cleanly on queue move via `SLATrackingService.updateSLAOnQueueChange()`.

3. **Supervisor dashboard near-real-time requirements.** WAM.4 requires real-time worklist updates. WAM.9 requires SLA notifications. A polling-based `@Scheduled` approach at 5-minute intervals provides a consistent, observable event source for dashboard tiles and SLA breach counts. Flowable timer granularity for many concurrent items can create engine load spikes at timer fire times.

4. **Auditability.** All SLA events are written to `queue_sla_log`. The application-level approach keeps SLA event evidence in the domain's own tables, not dependent on Flowable's ACT_RU_TIMER_JOB being queryable.

**Tradeoff acknowledged:** At exactly `sla_due_at`, there is up to a 5-minute evaluation lag before the BREACH event is recorded. This is acceptable for the operational SLA management purpose. If a specific BPMN path requires a precise timer at breach (e.g., auto-escalation after a fixed SLA window), a Flowable timer boundary event on the specific BPMN activity is still permitted for that use case — it would fire the `EscalateWorkItemCommand` via the delegate pattern.

---

## 7. Agent Desktop UI Specification

### 7.1 Agent Worklist Screen

**Route:** `/agent/worklist`
**Roles:** `AGENT`, `SPECIALIST_AGENT`, `ESCALATIONS_AGENT`, `TEAM_LEADER` (own team items)

**Purpose:** Displays all work items currently assigned to the authenticated agent, ordered by priority ascending then SLA remaining ascending (lowest priority score = most urgent; least time remaining = most urgent within priority band).

**Queue view — column definitions:**

| Column | Data source | Notes |
|---|---|---|
| Customer name | `domain/customer` read | Surname, First name |
| Account reference | `domain/account` read | DWP account identifier |
| Debt balance | `domain/account` read | Current outstanding balance (NUMERIC, formatted as GBP) |
| Item type | `work_item.item_type` | Badge: DEBT_COLLECTION, IE_REVIEW, VULNERABILITY_REVIEW, EXCEPTION, DISPUTE_HANDLING |
| Priority | `work_item.priority` | Integer displayed as LOW/MEDIUM/HIGH/CRITICAL band (threshold values configurable in Tier 1) |
| SLA remaining | Derived: `sla_due_at - NOW()` | Displayed as HH:MM remaining. Red when <= 25% remaining; amber when <= 50% remaining |
| Vulnerability indicator | `VulnerabilityFlagService.hasActiveVulnerabilityFlag()` | Icon only — no category detail for `AGENT` role (RULING-002) |
| Status | `work_item.status` | Badge |

**Actions per item:**
- **Open item** — navigates to Account/Case View; triggers flash note check.
- **Release item** — releases the item back to the queue (`WorkItemAssignmentService.release()`). Requires confirmation.
- **Request reassignment** — creates a reassignment request for TEAM_LEADER action; does not immediately reassign.

**Queue selector (side panel):** Displays the queues the agent is eligible for. Agent may switch to view items from a different queue (WAM.19 — agents see their own assigned items by default but can view the broader queue with TEAM_LEADER permission).

**Behaviour requirements:**
- Worklist must refresh in near-real-time (WAM.4). Recommended implementation: server-sent events or 30-second polling interval. Push notification on new assignment (WAM.18, DW.52).
- When a new item is pushed to the agent, an on-screen notification banner fires indicating the case reference and item type.
- When another agent opens an item simultaneously, the second agent receives an alert: "This case is currently being worked" (DW.52 anti-duplication requirement).

### 7.2 Account/Case View (Agent Desktop)

**Route:** `/agent/case/{workItemId}`
**Roles:** All operational roles (read access gated by queue RBAC; WAM.13)

**Pre-render gate — Vulnerability Flash Note:**

Before the account case view renders, the frontend calls `AgentDesktopService.getWorkItemContext()`. If the response contains `vulnerabilityFlashNoteRequired = true`:
1. A modal is displayed fullscreen, blocking the case view.
2. Modal content is defined by the vulnerability domain pack (section 9.2 of the vulnerability domain pack). For `AGENT` role: boolean indicator and referral instruction only. For `SPECIALIST_AGENT`, `TEAM_LEADER`, `OPS_MANAGER`, `COMPLIANCE`: category names, severity tiers, review dates.
3. The agent must click a labelled "I acknowledge" button to dismiss the modal.
4. On dismissal: `AgentDesktopService.acknowledgeVulnerabilityFlashNote()` is called. The acknowledgement is audit-logged. The case view renders only after a successful acknowledgement response.
5. Dismissal by browser back, closing the tab, or any mechanism other than the "I acknowledge" button must prevent the case view from loading.

**Case view header (always visible):**

| Field | Source |
|---|---|
| Customer name | `domain/customer` |
| National Insurance number | `domain/customer` (masked per RBAC — AGENT sees last 4 characters only) |
| Debt type | `domain/account` |
| Total outstanding balance | `domain/account` |
| Case / work item status | `work_item.status` |
| Vulnerability active indicator | Boolean (RULING-002: no category detail for AGENT role) |
| Breathing space active indicator | `domain/customer.BreathingSpaceService` |

**Tab sections:**

| Tab | Content | Roles permitted |
|---|---|---|
| Summary | Overview of debt, current treatment stage, last contact date, next action due | All |
| Contact History | Read from `domain/communications` — all contacts recorded against this account | All |
| Repayment Plan | Current arrangement status, plan terms, next payment due, payment history | All |
| I&E Summary | Most recent completed I&E record, NDI band, arrangement options generated | All |
| Documents | Document list attached to the account (letters, notices, evidence) | All |
| Audit Trail | Read from `domain/audit` — all CRUD events for this account | `TEAM_LEADER`, `OPS_MANAGER`, `COMPLIANCE`, `SRO` only |

**Quick action panel (right-hand side):**

| Action | Roles | Notes |
|---|---|---|
| Add note | `AGENT` and above | Adds a free-text note to the account; creates contact history entry |
| Record contact | `AGENT` and above | Records a contact event (channel, outcome, date/time) |
| Request I&E | `AGENT` and above | Creates an IE_REVIEW work item or initiates IE capture if no open draft exists |
| Raise dispute | `AGENT` and above | Initiates the dispute workflow; may trigger work item escalation |
| Escalate | `AGENT` and above | Calls `WorkItemAssignmentService.escalate()`; reason required |
| Override workflow/strategy | `TEAM_LEADER`, `OPS_MANAGER` | DW.11: supervisor exception processing — route account to different treatment step |
| Release item | `AGENT` and above | Returns item to queue |

**Blocked actions:**

If `VulnerabilityProtocolEnforcementService.getBlockedActionTypes(customerId)` returns a non-empty set, the corresponding quick actions are disabled in the UI with a tooltip: "Action blocked due to active vulnerability protocol." The authoritative enforcement gate is at the service layer, not the UI alone.

### 7.3 Supervisor Dashboard

**Route:** `/supervisor/dashboard`
**Roles:** `TEAM_LEADER`, `OPS_MANAGER`, `SRO` (read-only)

**Queue health tiles (one per visible queue):**

| Field | Source |
|---|---|
| Queue name | `work_queue.name` |
| Queue depth (PENDING) | Count of `work_item` rows with `status = PENDING` in this queue |
| Items assigned | Count of `work_item` rows with `status IN ('ASSIGNED', 'IN_PROGRESS')` in this queue |
| SLA breaches today | Count of `queue_sla_log` rows with `event_type = BREACH` and `fired_at >= today 00:00` for this queue |
| Agents online | Count of `agent_skill_profile` rows with `is_available = TRUE` and matching skill tags |

**Agent utilisation table:**

| Column | Source |
|---|---|
| Agent name | `domain/user` read |
| Items in progress | `agent_skill_profile.current_workload_count` |
| Completed today | Sum of `work_item_history` COMPLETED transitions for this agent since 00:00 today (DDE-OQ-WA-01 pending) |
| SLA breach count (session) | Count of `queue_sla_log` BREACH events for items assigned to this agent today |
| Status | `is_available` + `auto_paused` — displayed as: Online / Auto-paused / Offline |

**Bulk actions:**

| Action | Roles | Behaviour |
|---|---|---|
| Reassign queue | `OPS_MANAGER` | Calls `SupervisorDashboardService.reassignQueue()`. All PENDING and ASSIGNED items in source queue are moved to target queue. Confirmation required. |
| Pause queue | `OPS_MANAGER` | Calls `SupervisorDashboardService.pauseQueue()`. Stops push allocation; existing in-progress items unaffected. |
| Force escalate (SLA threshold) | `OPS_MANAGER` | Calls `SupervisorDashboardService.forceEscalateBreachedItems()` for the selected queue at a specified SLA percentage threshold. |
| Resume auto-assignment for agent | `TEAM_LEADER`, `OPS_MANAGER` | Calls `AgentDesktopService.resumeAgentAvailability()` for an auto-paused agent. |

**Dashboard refresh:** Near-real-time (WAM.4). Recommended: 60-second auto-refresh of tile data; agent utilisation table refreshes on demand or every 2 minutes.

---

## 8. RBAC Matrix

| Action | AGENT | SPECIALIST_AGENT | ESCALATIONS_AGENT | TEAM_LEADER | OPS_MANAGER | SRO | COMPLIANCE | BACKOFFICE |
|---|---|---|---|---|---|---|---|---|
| View own assigned work items | Y | Y | Y | Y | Y | Read | Read | N |
| View all items in own team's queues | N | N | N | Y (own team) | Y | Read | Read | N |
| View all items across all teams | N | N | N | N | Y | Read | N | N |
| Open and work an assigned item | Y | Y | Y | Y (own team) | N | N | N | N |
| Release own item back to queue | Y | Y | Y | Y | N | N | N | N |
| Assign item to self (self-select) | Y | Y | Y | Y | N | N | N | N |
| Reassign item within team | N | N | N | Y | Y | N | N | N |
| Bulk reassign queue | N | N | N | N | Y | N | N | N |
| Create queue | N | N | N | N | Y | N | N | N |
| Update queue definition | N | N | N | Y | Y | N | N | N |
| Pause/resume queue | N | N | N | N | Y | N | N | N |
| View supervisor dashboard | N | N | N | Y (own team) | Y | Read | N | N |
| Force escalate via dashboard | N | N | N | N | Y | N | N | N |
| Resume auto-paused agent | N | N | N | Y (own team) | Y | N | N | N |
| View audit trail tab | N | N | N | Y | Y | Y | Y | N |
| Work specialist vulnerability queue | N | Y | N | Y | N | N | N | N |
| Work escalation queue | N | Y | Y | Y | N | N | N | N |
| Exception ratification (DW.85) | N | N | Y | Y | Y | N | N | N |
| Override workflow/strategy (DW.11) | N | N | N | Y | Y | N | N | N |
| Worklist report generation (WAM.6) | N | N | N | Y | Y | Y | N | N |

**Notes:**
- `SPECIALIST_AGENT` works the vulnerability specialist queue (WAM.20). Items in this queue are access-restricted by `work_queue.skill_tags` and RBAC — an `AGENT` without the `VULNERABILITY_SPECIALIST` skill tag cannot self-assign items from it.
- `ESCALATIONS_AGENT` works the exception queue. Exception items carry `is_exception_queue = TRUE` on their associated queue; follow-on action suppression applies until an `ESCALATIONS_AGENT`, `TEAM_LEADER`, or `OPS_MANAGER` ratifies the item (DW.85).
- Cross-team visibility for `TEAM_LEADER` is pending DDE-OQ-WA-03. Current model: `team_scope` restricts `TEAM_LEADER` to their own team's queues.
- `SRO` has read-only dashboard access (no operational actions).
- `COMPLIANCE` has read-only audit trail and dashboard access for governance purposes.

---

## 9. Integration Points

| Domain | Direction | Data accessed | Interface |
|---|---|---|---|
| `infrastructure/process` (Flowable) | Bidirectional | Human task lifecycle events create and complete work items | `DelegateCommandBus` — `CreateWorkItemCommand`, `CompleteWorkItemCommand`, `EscalateWorkItemCommand`, `CancelWorkItemCommand` |
| `domain/customer` | Read | Vulnerability flag active status (boolean); customer name; NI number; breathing space active status | `VulnerabilityFlagService.hasActiveVulnerabilityFlag()`, `getActiveFlags()` (role-gated); `BreathingSpaceService.isBreathingSpaceActive()` |
| `domain/customer` | Receive signal | Vulnerability flag creation triggers routing signal to this domain to reassign existing work items | `WorkAllocationPort` (inbound signal from `VulnerabilityProtocolEnforcementService`) |
| `domain/account` | Read | Account risk tier; debt balance; account reference | Account query port (read-only) |
| `domain/repaymentplan` | Read | Arrangement status (NONE/ACTIVE/BREACHED) for use in routing DMN | Arrangement status query port (read-only) |
| `domain/communications` | Read | Contact history for display in agent desktop Contact History tab | Contact history read port (read-only) |
| `domain/strategy` | Read | Segmentation output and current treatment stage for routing context | Strategy context read port (read-only) |
| `domain/audit` | Write | Audit events for all significant work item transitions and supervisor actions | `AuditEventPort` (write) |
| `shared/process/port` | Bidirectional | Process engine command and event interfaces | `DelegateCommandBus`, `ProcessEventPort` |

**Interaction constraints:**
- This domain does not import from `domain/customer`, `domain/account`, `domain/repaymentplan`, `domain/communications`, or `domain/strategy`. All cross-domain reads go via published interfaces or ports. No direct JPA repository access across domain boundaries.
- Flowable imports are confined to `infrastructure/process`. This domain's services and repositories do not reference any `org.flowable` package.

---

## 10. Open Questions

### DDE-OQ-WA-01 — Agent utilisation reporting granularity (NON-BLOCKING for schema; BLOCKING for dashboard build)

**Status:** Open — no DWP policy answer received
**Owner:** DWP Product Owner / Operations Lead

**Question:** At what granularity should agent productivity be reported?
- Per shift (requires a shift definition concept and shift start/end times in `agent_session`)?
- Per calendar day (00:00–23:59, which may split a night shift)?
- Rolling 24-hour window?

**Impact:** `agent_session.items_completed` is currently counted per session. If DWP requires per-shift reporting, `agent_session` must carry a `shift_id` FK and the shift definition must be a Tier 1 configuration concept. The supervisor dashboard "completed today" metric is illustrative until this is resolved.

---

### DDE-OQ-WA-02 — Maximum concurrent work item policy: hard cap vs. soft advisory (NON-BLOCKING for schema; BLOCKING for assignment service build) **BLOCKING**

**Status:** Open — no DWP policy answer received
**Owner:** DWP Operations Lead

**Question:** When an agent reaches `agent_skill_profile.max_concurrent_items`, should the system:
- **Hard cap:** reject any further assignment attempts with `AgentAtCapacityException`; agent cannot receive new items until an existing item is completed or released, or
- **Soft advisory:** permit assignment beyond the cap with an alert to the supervisor but no hard rejection?

**Impact:** Affects `WorkItemAssignmentService.assignToAgent()` — the `AgentAtCapacityException` throw is conditional on this decision. The schema supports both models; the application logic differs. Cannot be finalised without DWP sign-off.

---

### DDE-OQ-WA-03 — Supervisor cross-team visibility scope (NON-BLOCKING for schema; BLOCKING for RBAC build)

**Status:** Open — no DWP policy answer received
**Owner:** DWP Operations Lead / Information Security

**Question:** Can a `TEAM_LEADER` view items and queues belonging to teams other than their own? Current model restricts `TEAM_LEADER` to queues where `work_queue.team_scope` matches their team. But some operational scenarios (e.g., cross-team cover, shared overflow queues) may require cross-team visibility.

**Impact:** The `team_scope` column on `work_queue` and the `getVisibleQueues()` RBAC filter in `WorkQueueService` must align with the confirmed policy. If cross-team visibility is required for certain queue types, an additional `team_scope_exceptions` JSONB column on `work_queue` may be needed, or a separate team-to-queue mapping table.

---

### DDE-OQ-WA-04 — Inter-queue escalation timeout defaults **BLOCKING**

**Status:** Open — no DWP policy answer received
**Owner:** DWP Operations Lead

**Question:** What are the default SLA hours for each queue tier (standard, specialist, escalation, exception)? And what is the default escalation timeout — i.e., how long before an unworked escalated item automatically escalates further or fires a supervisor alert?

**Impact:**
- `work_queue.sla_hours` seed data cannot be finalised without DWP confirming SLA tiers.
- `queue_routing_rule.escalation_threshold_hours` default values cannot be set.
- `work-item-priority.dmn` threshold values (e.g., `slaProximityPct >= 75` for WARNING) are illustrative and depend on the confirmed SLA targets.
- This blocks production seeding of the Tier 1 queue configuration and the DMN table.

---

## 11. Acceptance Criteria

All acceptance criteria are sourced to requirement IDs from `Tender requirements docs/Functional-Requirements-Consolidated.md` and are governed by the domain rulings listed in the document header.

---

**AC-WA-01 — SLA timer starts on work item creation and a WARNING event fires at the configured threshold**
*Requirement IDs: WAM.9, WAM.26*

Given a new work item is created in a queue with `sla_hours = 24`
And `agent_availability_config.sla_warning_threshold_pct = 75`
When 18 hours have elapsed since `sla_due_at` was set (75% of 24 hours)
And `SLATrackingService.evaluateSLATimers()` executes
Then a `queue_sla_log` row is written with `event_type = WARNING`, `sla_threshold_pct = 75`, `work_item_id`, and `queue_id`
And `work_item.sla_breached` remains `FALSE`
And no BREACH event is written at this point

**Negative case:** If the work item has `status = COMPLETED` at the time of evaluation, no WARNING or BREACH event is written. Terminal-status items are excluded from the SLA evaluation run.

---

**AC-WA-02 — SLA breach is recorded and work_item.sla_breached is set TRUE**
*Requirement IDs: WAM.9, WAM.26*

Given a work item has `sla_due_at` in the past
And `work_item.sla_breached = FALSE`
When `SLATrackingService.evaluateSLATimers()` executes
Then `work_item.sla_breached` is set to `TRUE`
And a `queue_sla_log` row is written with `event_type = BREACH` and `sla_threshold_pct = 100`
And if the work item has a `flowable_process_instance_id`, an `SLA_BREACH_EVENT` is emitted via `ProcessEventPort`
And the supervisor dashboard SLA breach count for the item's queue increments by 1

**Negative case:** If `sla_breached` is already `TRUE`, a second BREACH log entry is not written on the next evaluation cycle. The breach is idempotent.

---

**AC-WA-03 — Vulnerability flash note fires before account case view renders; dismissal is audit-logged**
*Requirement IDs: UI.21, WAM.20*
*Ruling: RULING-002 §3*

Given a work item is linked to an account where the customer has at least one active vulnerability flag
When an `AGENT` opens the case view for that work item
Then a full-screen modal fires before the case detail is rendered
And the modal shows the text "This customer has an active vulnerability indicator" and "Refer to specialist — do not progress collection activity"
And the modal does NOT show the vulnerability category name or severity tier
And the case view does not render until the agent clicks "I acknowledge"
And `AgentDesktopService.acknowledgeVulnerabilityFlashNote()` is called successfully
And an `AUDIT_EVENT` of type `VULNERABILITY_FLASH_NOTE_ACKNOWLEDGED` is written with: agent user ID, role, work item ID, active flag IDs, and timestamp

**Negative case:** The agent attempts to navigate away from the modal using the browser back button. The case view does not load. On return to the work item URL, the modal fires again.

---

**AC-WA-04 — Work item priority DMN routes CRITICAL + vulnerability to escalation queue immediately**
*Requirement IDs: WAM.3, WAM.17, WAM.22*

Given a `CreateWorkItemCommand` arrives for a customer where `accountRiskTier = CRITICAL` and `vulnerabilityActive = true`
When `WorkItemAssignmentService.intakeWorkItem()` evaluates `work-item-priority.dmn`
Then `priorityScore = 0` is returned
And `targetQueueName = SPECIALIST_ESCALATION` is returned
And `escalateImmediately = true` is returned
And the work item is created with `status = ESCALATED` and placed in the `SPECIALIST_ESCALATION` queue
And a `work_item_history` row is written with `to_status = ESCALATED` and `change_source = SYSTEM_AUTOMATED`

**Negative case:** The same `CreateWorkItemCommand` with `accountRiskTier = LOW` and `vulnerabilityActive = false` produces `priorityScore >= 70`, `targetQueueName = STANDARD_QUEUE`, and `escalateImmediately = false`. The item is created with `status = PENDING`.

---

**AC-WA-05 — Bulk reassignment moves all PENDING and ASSIGNED items; IN_PROGRESS items are excluded**
*Requirement IDs: WAM.2, WAM.16, WAM.25*

Given queue A has 10 work items: 4 PENDING, 3 ASSIGNED, 2 IN_PROGRESS, 1 COMPLETED
When an `OPS_MANAGER` calls `SupervisorDashboardService.reassignQueue(queueA, queueB, supervisorId, reason)`
Then 4 PENDING and 3 ASSIGNED items are reassigned to queue B
And 2 IN_PROGRESS items remain in queue A (their assigned agents are not disrupted)
And 1 COMPLETED item remains unchanged
And each of the 7 reassigned items has a `work_item_history` row written with `change_source = BULK_REASSIGNMENT` and the supervisor user ID
And a `BulkReassignmentResult` is returned with `succeeded = 7`, `skipped = 2` (IN_PROGRESS), `failed = 0`

**Negative case:** An `AGENT` attempts to call `reassignQueue()`. The service throws `InsufficientAuthorityException`. No items are moved.

---

**AC-WA-06 — Skill-based assignment: item is held in MANUAL mode when no agent holds required skills**
*Requirement IDs: WAM.15, WAM.22*

Given a work item is created in a queue with `skill_tags = ["MENTAL_HEALTH_SPECIALIST"]`
And no available agents have `"MENTAL_HEALTH_SPECIALIST"` in their `skill_tags`
When `queue-assignment.dmn` is evaluated
Then `availableAgentsWithSkillMatch = 0` is the input
And the DMN returns `assignmentMode = MANUAL`
And the work item remains `PENDING` in the queue
And no auto-assignment is attempted
And a supervisor alert is raised (via `ProcessEventPort` notification event) indicating no skill-matched agent is available

**Negative case:** When an agent with `"MENTAL_HEALTH_SPECIALIST"` in their skill profile starts their session (is_available becomes TRUE), `runAutoAssignment()` is triggered for the queue. The DMN now returns `SKILL_MATCH`, and the item is assigned to the available agent.

---

**AC-WA-07 — Supervisor team scope enforcement: TEAM_LEADER cannot see items outside their team**
*Requirement IDs: WAM.13, WAM.25*

Given queue X has `team_scope = TEAM_ALPHA`
And queue Y has `team_scope = TEAM_BETA`
When a `TEAM_LEADER` whose team assignment is `TEAM_ALPHA` calls `WorkQueueService.getVisibleQueues()`
Then queue X is returned
And queue Y is not returned
And attempting to call `WorkItemAssignmentService.reassign()` with a target item from queue Y throws `WorkItemAccessDeniedException`

**Negative case (OPS_MANAGER):** An `OPS_MANAGER` calling `getVisibleQueues()` with no team scope restriction receives both queue X and queue Y.

---

**AC-WA-08 — Exception queue suppresses follow-on actions until ratified**
*Requirement IDs: DW.85, WAM.17*

Given a work item exists in a queue where `is_exception_queue = TRUE`
And the work item has `status = PENDING`
When a Flowable process delegate attempts to trigger a follow-on action (e.g., send a letter) for the account linked to this item
Then the action is suppressed with reason `EXCEPTION_QUEUE_UNRATIFIED`
And the suppression is logged in `queue_sla_log` as a contextual note
And the action is only permitted after an `ESCALATIONS_AGENT`, `TEAM_LEADER`, or `OPS_MANAGER` calls `WorkItemAssignmentService.complete()` with a ratification outcome on the exception item

**Negative case:** A standard `AGENT` attempts to ratify an exception item. The service throws `InsufficientAuthorityException`. The exception remains open.

---

**AC-WA-09 — Agent push notification fires when new item is assigned; anti-duplication alert fires on concurrent open**
*Requirement IDs: DW.52, WAM.18*

Given agent A has an active session
When a new work item is auto-assigned to agent A via `WorkItemAssignmentService.runAutoAssignment()`
Then an in-app push notification is sent to agent A containing the case reference and item type
And the notification is visible in the agent's worklist within the configured refresh interval

Given agent A has the case view open for work item W
When agent B (also assigned to a shared queue) opens the same work item W
Then agent B receives an alert: "This case is currently being worked"
And agent B may still view the case in read-only mode but may not take write actions

**Negative case:** If agent A's session ends while item W is IN_PROGRESS, agent B's warning is cleared on the next worklist refresh and agent B may take over the item.

---

**AC-WA-10 — Agent auto-pause triggers when SLA breach threshold is reached**
*Requirement IDs: WAM.9, WAM.15*

Given `agent_availability_config.auto_pause_on_breach_count = 3`
And agent A has accumulated 3 SLA breach events in the current session (`queue_sla_log` BREACH entries for items assigned to agent A today)
When `SLATrackingService.evaluateSLATimers()` fires the third breach event for agent A
Then `agent_skill_profile.auto_paused` is set to `TRUE`
And `agent_skill_profile.is_available` is set to `FALSE`
And no further items are auto-assigned to agent A
And an in-app notification is sent to agent A and their `TEAM_LEADER` indicating auto-pause has been triggered

**Negative case:** A `TEAM_LEADER` calls `AgentDesktopService.resumeAgentAvailability()` for agent A. `auto_paused` is set to `FALSE` and `is_available` is set to `TRUE`. An `AUDIT_EVENT` of type `AGENT_AUTO_PAUSE_LIFTED` is written with the TEAM_LEADER's user ID and timestamp.

---

**AC-WA-11 — Work item moved to different queue receives updated SLA; earlier deadline is not extended**
*Requirement IDs: WAM.5, WAM.28*

Given a work item was created in queue A with `sla_hours = 24` and `sla_due_at = T+24h`
And 12 hours have elapsed (T+12h), so 12 hours remain
When the item is moved to queue B with `sla_hours = 6` by a supervisor
Then `SLATrackingService.updateSLAOnQueueChange()` is called
And the new `sla_due_at = MAX(original T+24h, NOW() + 6h) = MAX(T+24h, T+18h) = T+24h`
And the original SLA deadline is preserved because queue A's deadline is later than queue B's
And a second scenario: if queue B has `sla_hours = 48`, `new sla_due_at = MAX(T+24h, T+60h) = T+60h`
And an `AUDIT_EVENT` of type `WORK_ITEM_SLA_UPDATED` is written in both cases

**Negative case:** Moving to a queue with a shorter SLA that would expire in the past (`sla_hours = 1`, `NOW() + 1h = T+13h < T+24h`) retains the original `T+24h` deadline. The SLA cannot be artificially shortened by a queue move.

---

**AC-WA-12 — Completing a work item with a Flowable task ID signals Flowable task completion**
*Requirement IDs: WAM.3, WAM.27*

Given a work item has `status = IN_PROGRESS` and `flowable_task_id = "task-abc-001"`
When `WorkItemAssignmentService.complete(workItemId, agentUserId, note)` is called
Then `work_item.status` transitions to `COMPLETED`
And a `work_item_history` row is written with `to_status = COMPLETED`
And a `CompleteWorkItemCommand(flowableTaskId = "task-abc-001")` is dispatched via `DelegateCommandBus`
And `agent_skill_profile.current_workload_count` is decremented by 1
And `agent_session.items_completed` is incremented by 1

**Negative case:** Attempting to complete a work item with `status = COMPLETED` throws `WorkItemAlreadyCompletedException`. No Flowable command is dispatched and no history row is written.

---

**AC-WA-13 — RBAC: AGENT cannot view items outside their assigned worklist without TEAM_LEADER role**
*Requirement IDs: WAM.13, WAM.19, WAM.20*

Given agent A with role `AGENT` has work items in queue STANDARD_QUEUE
When agent A attempts to call `getVisibleQueues()` for SPECIALIST_VULNERABILITY queue (which requires `VULNERABILITY_SPECIALIST` skill tag and `SPECIALIST_AGENT` role)
Then the queue is not returned in the visible queue list
And if agent A directly requests a work item from SPECIALIST_VULNERABILITY queue via the API
Then `WorkItemAccessDeniedException` is returned (HTTP 403)
And an `AUDIT_EVENT` of type `UNAUTHORISED_QUEUE_ACCESS_ATTEMPT` is written

**Negative case:** A `SPECIALIST_AGENT` with the `VULNERABILITY_SPECIALIST` skill tag in their profile calls `getVisibleQueues()` and receives SPECIALIST_VULNERABILITY queue in the result. They may self-assign items from it.

---

**AC-WA-14 — Concurrent assignment is prevented via optimistic locking**
*Requirement IDs: DW.52, WAM.22*

Given a work item has `status = PENDING` and `lock_version = 5`
And agent A and agent B both read the item simultaneously with `lock_version = 5`
When agent A calls `assignToAgent(workItemId, agentA, supervisorId, expectedLockVersion = 5)` and the update succeeds, setting `lock_version = 6`
And agent B then calls `assignToAgent(workItemId, agentB, supervisorId, expectedLockVersion = 5)`
Then agent B's call throws `WorkItemLockException` (the stored `lock_version = 6` does not match the expected `5`)
And the work item remains assigned to agent A only
And no duplicate assignment history row is written for agent B

**Negative case:** Agent B re-reads the item, sees `lock_version = 6` and `status = ASSIGNED` to agent A, and does not retry the assignment.

---

**AC-WA-15 — Worklist report includes completed, no-contact, wrong-party, and dated-out dispositions**
*Requirement IDs: WAM.6, WAM.26*

Given queue STANDARD_QUEUE has been active for the reporting period
When a `TEAM_LEADER` calls `SupervisorDashboardService.generateWorklistReport(queueId, fromDate, toDate, userId)`
Then the report includes: total items worked, count by `item_type`, count by final `to_status` (COMPLETED, CANCELLED), and where completion notes include recognised disposition codes (NO_CONTACT, WRONG_PARTY, DATED_OUT, REFERRED), these are counted separately
And the report is accessible to `TEAM_LEADER`, `OPS_MANAGER`, and `COMPLIANCE` roles
And the report does not expose customer personal data — only aggregate counts and account reference counts

**Negative case:** A request for a report for a queue outside the `TEAM_LEADER`'s `team_scope` returns `WorkItemAccessDeniedException`.

---

## Handoff Declaration

- **Completed:** Work Allocation & Agent Desktop domain pack written — covering domain ownership, core data model (6 tables with full DDL), Tier 1 configuration tables (2), Tier 2 DMN tables (2), Java service interfaces (5 with full javadoc), Flowable integration design with SLA architecture rationale, Agent Desktop UI specification (3 screens), RBAC matrix, integration points, 4 open questions, and 15 acceptance criteria.
- **Files changed:** `docs/project-foundation/domain-packs/work-allocation-domain-pack.md` (created)
- **Requirement IDs covered:** WAM.1–WAM.28, DW.11, DW.31, DW.32, DW.52, DW.80, DW.83, DW.85, UI.21, UI.9, AAD.14, AAD.18, AAD.19, CAS.9 — all sourced from `Tender requirements docs/Functional-Requirements-Consolidated.md`
- **Domain rulings used:** RULING-002 (vulnerability data classification — governs flash note content by role); RULING-003 (vulnerability configurability — governs specialist queue routing triggers). No Class A requirements in this domain require new rulings; work allocation is not subject to FCA/CCA/GDPR regulatory interpretation beyond the flash note RBAC content rules already governed by RULING-002.
- **ACs satisfied:** AC-WA-01 through AC-WA-15 (15 acceptance criteria covering SLA timer start and breach, vulnerability flash note, queue routing priority, bulk reassignment, skill-based assignment fallback, supervisor visibility boundaries, concurrent assignment prevention, exception queue suppression, auto-pause, SLA queue-move logic, Flowable task completion signalling, RBAC access control, and worklist reporting)
- **ACs not satisfied:** None declared — all scoped requirements have corresponding acceptance criteria. DMN priority score thresholds and queue name values in AC-WA-04 are illustrative pending DDE-OQ-WA-04 resolution.
- **Assumptions made:**
  1. SLA timers are implemented at application level (Spring `@Scheduled`), not via Flowable timer boundary events. Rationale documented in section 6.3. This is a design recommendation; the Delivery Designer must confirm before build.
  2. `team_scope` on `work_queue` restricts `TEAM_LEADER` visibility to their own team. Cross-team scope rules are pending DDE-OQ-WA-03.
  3. Queue code names in the DMN tables (SPECIALIST_ESCALATION, HIGH_RISK_QUEUE, SPECIALIST_VULNERABILITY, STANDARD_QUEUE) are illustrative. Actual codes are DWP operational decisions and must be confirmed before DMN deployment.
  4. "Today" in supervisor dashboard utilisation counts is calendar day (00:00–23:59) pending DDE-OQ-WA-01 resolution.
  5. `max_concurrent_items` is modelled as a hard cap; the hard vs. soft advisory distinction is pending DDE-OQ-WA-02 sign-off.
- **Open questions:**
  - DDE-OQ-WA-01 — agent utilisation reporting granularity — owner: DWP Product Owner / Operations Lead (non-blocking for schema, blocking for dashboard build)
  - DDE-OQ-WA-02 — hard cap vs. soft advisory for concurrent item limit — owner: DWP Operations Lead (BLOCKING for assignment service build)
  - DDE-OQ-WA-03 — supervisor cross-team visibility scope — owner: DWP Operations Lead / Information Security (blocking for RBAC build)
  - DDE-OQ-WA-04 — inter-queue escalation timeout defaults and queue SLA tiers — owner: DWP Operations Lead (BLOCKING for queue seed data and DMN finalisation)
- **Next role:** Delivery Designer (for module boundary confirmation, API contract draft, and confirmation of SLA timer architecture choice — application-level scheduler vs. Flowable timer boundary events)
- **What they need:** This domain pack in full; vulnerability domain pack (section 8 — WorkAllocation interaction, WorkAllocationPort signal); RBAC-IMPLEMENTATION-DECISIONS.md; ADR-008 (three-tier configurability); ADR-001 (process instance per debt — for Flowable task mapping); ARCHITECTURE-BLUEPRINT.md for package boundary conventions. The Delivery Designer must confirm whether the `work-item-priority.dmn` and `queue-assignment.dmn` decision keys are deployed as standalone DMN resources or embedded within BPMN service task sequences.

