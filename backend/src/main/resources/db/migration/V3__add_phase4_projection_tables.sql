-- Phase 4 read-model projection tables for reporting and analytics.
-- This migration intentionally avoids cross-domain foreign keys so scaffolding
-- can land before full module entities are implemented.

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
    CONSTRAINT pk_strategy_performance_projection
        PRIMARY KEY (strategy_test_id, metric_date, variant, vulnerability_override)
);

CREATE INDEX idx_strategy_performance_metric_date
    ON strategy_performance_projection (metric_date DESC);

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

CREATE INDEX idx_queue_health_projection_as_of_ts
    ON queue_health_projection (as_of_ts DESC);

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
    CONSTRAINT pk_dca_performance_projection
        PRIMARY KEY (agency_id, metric_month)
);

CREATE INDEX idx_dca_performance_metric_month
    ON dca_performance_projection (metric_month DESC);

