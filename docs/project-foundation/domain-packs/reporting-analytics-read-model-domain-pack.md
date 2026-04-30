# Reporting & Analytics Read Model - Domain Pack

**Domain:** Collections MI and Analytics Read Models
**Packages:** `domain/reporting`, `domain/analytics`
**Status:** Phase 4 complete (design baseline locked) - implementation pending scaffold and migrations
**Date:** 2026-04-27
**Author:** Codex (design consolidation)
**Primary ADR alignment:** ADR-007 (monolith + read model), ADR-008 (three-tier configurability), ADR-010 (champion/challenger), ADR-014 (analytics panels)
**Requirement IDs covered:** MIR.1, MIR.2, A.1, A.2, WAM.26, WAM.27, WAM.28, BSF.9, DW.39, DW.42

---

## 1. Purpose and Scope

This pack defines the read-side model for collections reporting and analytics in DCMS.

It locks the Phase 4 design for:

- KPI taxonomy for collections performance.
- Projection-table architecture in PostgreSQL (no separate metrics datastore).
- Event-to-projection update strategy from immutable audit and domain events.
- Dashboard query contracts for executive and operations views.
- Champion/challenger analytics panels aligned to ADR-014.

This pack does not define frontend layout details; it defines backend read model semantics and query contracts. Staff-facing dashboard, export, freshness, reconciliation, and drilldown UX is defined in `docs/project-foundation/mi-dashboard-and-reporting-ux-design.md`.

---

## 2. Design Decisions Locked

1. **Single PostgreSQL datastore**
   - Reporting and analytics projections are stored in PostgreSQL tables in the application schema.
   - No dedicated time-series database is introduced in this phase.

2. **Projection-first read model**
   - UI and MI exports query projection tables.
   - Raw Flowable history tables are not queried directly by UI paths.

3. **Event-driven projection updates**
   - Projection tables are updated from `audit_event` and domain outcome events.
   - Projection writes are idempotent and deterministic.

4. **Champion/challenger panel semantics**
   - Panel 1 applies the active champion/challenger policy's comparison-eligibility predicate.
   - Under the current approved policy, Panel 1 excludes vulnerable override assignments.
   - Panel 2 contains vulnerable outcomes only under the current approved policy.
   - Panel 3 contains full population.

5. **No architecture split**
   - `domain/analytics` owns experimentation and performance comparison logic.
   - `domain/reporting` owns read models, snapshots, and export views.

---

## 3. KPI Taxonomy

### 3.1 Core KPI set

| KPI | Definition | Numerator | Denominator | Grain |
|---|---|---|---|---|
| `cure_rate` | Accounts that return to compliant status | accounts cured in period | accounts active in treatment in period | day/week/month |
| `contact_rate` | Accounts successfully contacted | accounts with at least one successful outbound contact | contact-eligible accounts in period | day/week/month |
| `arrangement_rate` | Accounts with arrangement agreed | accounts entering `ARRANGEMENT_ACTIVE` | accounts assessed for arrangement in period | day/week/month |
| `breach_rate` | Arrangements that breach | arrangements transitioned to breach | active arrangements in period | day/week/month |
| `dca_recovery_rate` | DCA recoveries vs. placed balance | amount recovered by DCA | amount placed to DCA | month |
| `write_off_rate` | Balance written off | write-off amount in period | outstanding collectible balance in period | month |

### 3.2 Supporting operational KPIs

- `sla_warning_count`
- `sla_breach_count`
- `queue_throughput`
- `agent_productivity_index`
- `suppressed_communication_count`

---

## 4. Projection Model

### 4.1 Tables

#### `kpi_snapshot_daily`

Daily aggregate KPI snapshot by organizational scope.

```sql
CREATE TABLE kpi_snapshot_daily (
    snapshot_date            DATE            NOT NULL,
    team_scope               VARCHAR(100)    NOT NULL,
    segment_code             VARCHAR(100),
    cure_rate                NUMERIC(7,4)    NOT NULL,
    contact_rate             NUMERIC(7,4)    NOT NULL,
    arrangement_rate         NUMERIC(7,4)    NOT NULL,
    breach_rate              NUMERIC(7,4)    NOT NULL,
    dca_recovery_rate        NUMERIC(7,4)    NOT NULL,
    write_off_rate           NUMERIC(7,4)    NOT NULL,
    accounts_in_scope        INTEGER         NOT NULL,
    computed_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT pk_kpi_snapshot_daily
        PRIMARY KEY (snapshot_date, team_scope, segment_code)
);

CREATE INDEX idx_kpi_snapshot_daily_team_date
    ON kpi_snapshot_daily (team_scope, snapshot_date DESC);
```

#### `strategy_performance_projection`

Aggregated performance by champion/challenger strategy arm and metric.

```sql
CREATE TABLE strategy_performance_projection (
    strategy_test_id         UUID            NOT NULL,
    metric_date              DATE            NOT NULL,
    variant                  VARCHAR(20)     NOT NULL,
    vulnerability_override   BOOLEAN         NOT NULL,
    accounts_count           INTEGER         NOT NULL,
    payments_total           NUMERIC(14,2)   NOT NULL,
    arrangements_count       INTEGER         NOT NULL,
    breaches_count           INTEGER         NOT NULL,
    complaints_count         INTEGER         NOT NULL,
    computed_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT pk_strategy_perf_projection
        PRIMARY KEY (strategy_test_id, metric_date, variant, vulnerability_override)
);
```

#### `queue_health_projection`

Current queue-level operational view used by supervisor dashboard.

```sql
CREATE TABLE queue_health_projection (
    queue_id                 UUID            NOT NULL,
    as_of_ts                 TIMESTAMPTZ     NOT NULL,
    queue_depth              INTEGER         NOT NULL,
    unassigned_count         INTEGER         NOT NULL,
    sla_warning_count        INTEGER         NOT NULL,
    sla_breach_count         INTEGER         NOT NULL,
    avg_age_minutes          INTEGER         NOT NULL,
    team_scope               VARCHAR(100),
    CONSTRAINT pk_queue_health_projection
        PRIMARY KEY (queue_id)
);
```

#### `dca_performance_projection`

Agency-level performance metrics for DCA dashboard and executive rollups.

```sql
CREATE TABLE dca_performance_projection (
    agency_id                UUID            NOT NULL,
    metric_month             DATE            NOT NULL,
    placements_count         INTEGER         NOT NULL,
    active_placements_count  INTEGER         NOT NULL,
    recovered_amount         NUMERIC(14,2)   NOT NULL,
    placed_amount            NUMERIC(14,2)   NOT NULL,
    recovery_rate            NUMERIC(7,4)    NOT NULL,
    commission_accrued       NUMERIC(14,2)   NOT NULL,
    recall_count             INTEGER         NOT NULL,
    computed_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT pk_dca_perf_projection
        PRIMARY KEY (agency_id, metric_month)
);
```

### 4.2 Refresh strategy

- `queue_health_projection`: near-real-time incremental update (event-driven).
- `kpi_snapshot_daily`: daily batch rollup at 00:15 UTC.
- `strategy_performance_projection`: incremental intra-day updates + daily reconciliation pass.
- `dca_performance_projection`: hourly incremental updates + month-end close recalculation.

All projection writers must support idempotent replay by using deterministic upsert keys.

---

## 5. Event Sources

Projection writers consume these canonical sources:

- `audit_event` (authoritative lifecycle transitions and compliance events).
- `communication_event` and suppression outcomes from `domain/communications`.
- `work_item`, `work_item_history`, and SLA events from `domain/workallocation`.
- `strategy_assignment` and outcome metrics from `domain/strategy` / `domain/analytics`.
- `dca_placement`, reconciliation, and commission events from `domain/thirdpartymanagement`.

Projection writers must not consume UI-layer events.

---

## 6. Dashboard Query Contracts

### 6.1 Executive dashboard

Must provide:

- latest KPI row per team and overall.
- 30-day trend from `kpi_snapshot_daily`.
- strategy comparison summary from `strategy_performance_projection`.
- DCA league table from `dca_performance_projection`.

### 6.2 Operations dashboard

Must provide:

- current queue health from `queue_health_projection`.
- same-day contact rate and throughput counters.
- SLA warning and breach counts by queue and team.

### 6.3 Champion/challenger panels (ADR-014)

- **Panel 1**: active policy's comparison-eligible population. Current policy: `vulnerability_override = false` only.
- **Panel 2**: protected/vulnerable outcomes population. Current policy: `vulnerability_override = true` only.
- **Panel 3**: no vulnerability filter.

This split is policy-governed and must be represented in reporting service query methods as explicit clauses. The current policy predicates must not be silently bypassed; any changed predicate requires a governed champion/challenger policy version and updated analytics evidence.

---

## 7. Module Boundaries

- `domain/analytics`
  - Owns strategy experiment outcome aggregation logic.
  - Owns panel-specific analytics services.

- `domain/reporting`
  - Owns projection table schemas, readers, and export APIs.
  - Owns scheduled rollups and read-only DTO contracts for MI exports.

- `domain/audit`
  - Remains source-of-truth for immutable events.
  - Does not own dashboard query semantics.

---

## 8. Non-Adopt Decisions (Locked)

The following are explicitly rejected for this phase:

- Separate metrics microservice.
- Separate time-series database.
- Separate reporting database connection.
- Kafka dependency between app and Flowable for internal reporting semantics.

If scale later requires storage specialization (for example TimescaleDB), that is a new ADR and is not part of this baseline.

---

## 9. Open Policy Inputs (Do Not Block Build)

- Champion/challenger promotion thresholds remain policy-gated by DDE-OQ-12.
- Some KPI target values are operational-policy inputs and are configurable, not hardcoded.

These do not block implementation of projection schemas or query APIs.

---

## 10. Phase 4 Completion Criteria

Phase 4 design is considered complete when:

1. Projection schema above is accepted and referenced by `MASTER-DESIGN-DOCUMENT.md`.
2. Panel definitions match ADR-014 semantics without reinterpretation.
3. Reporting/analytics boundaries are explicit and consistent with monolith architecture.
4. No unresolved architectural decision remains for KPI storage or read model pattern.
5. Staff-facing MI/dashboard UX is covered by `mi-dashboard-and-reporting-ux-design.md`.

This domain pack satisfies those criteria.
