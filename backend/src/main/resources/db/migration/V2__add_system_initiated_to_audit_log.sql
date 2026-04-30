-- Add system_initiated flag to audit_log to distinguish background Flowable
-- job executor operations (true) from human Admin UI actions (false).
-- Required by ADR-011 HistoryEventProcessor actor resolution pattern.
-- Default false: existing rows are attributed to operational (non-background) context.
ALTER TABLE audit_log
    ADD COLUMN system_initiated BOOLEAN NOT NULL DEFAULT false;
