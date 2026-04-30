---
id: STD-PLAT-006
title: Database Migration Standard
status: Approved
owner: Platform Engineering
applies_to: All services with database persistence
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - platform/evidence-immutability-and-replay.md
  - platform/immutable-records-and-corrections.md
last_changed: 2026-04-07
---

## Purpose

Ensure database schema migrations are safe, irreversible once applied, and compatible with zero-downtime deployment patterns.

---

## Migration Immutability (MUST)

- A migration script is **immutable once it has been applied to any environment** (including dev).
- MUST NOT: renumber, rename, or modify the content of an applied migration script.
- MUST NOT: delete a migration script that has been applied to any environment.
- Violating this rule causes migration tool checksum failures (e.g., Flyway `FlywayValidateException`) and crash-loops on next deployment.

**For work-in-progress migrations on a feature branch:**
- WIP migrations may be freely renumbered and modified on the feature branch.
- Before merging to `main`, renumber the migration to the next available sequence number.
- Once merged to `main` and applied to any environment, the migration is immutable.

---

## Idempotency (MUST)

All data migrations (INSERT, UPDATE, DELETE on data, not schema) MUST be idempotent:
- Use `INSERT ... ON CONFLICT DO NOTHING` or equivalent upsert patterns
- Use `IF NOT EXISTS` / `IF EXISTS` for DDL where the migration tool does not enforce this
- Assume the migration may be re-run and must produce the same final state

---

## Backward Compatibility and Zero-Downtime Deployment

When a schema change would break the running application before the new code is deployed, use the **expand-contract pattern**:

1. **Expand:** add the new column/table/index as nullable or with a default. Old code continues to work.
2. **Migrate:** backfill data if required (idempotent job or migration).
3. **Contract:** in a subsequent release, make the column NOT NULL, remove old columns, etc.

Do not drop columns or tables in the same migration that adds their replacement.

---

## Migration File Requirements

Every migration file MUST include a comment header:
```sql
-- Migration: V{number}__{description}
-- Issue: #{github-issue-number}
-- Reversible: Yes / No
-- Notes: {any important context}
```

---

## Validate-on-Migrate Setting

For production environments, `validate-on-migrate` MUST be enabled (this is the default for Flyway and similar tools). Disabling it is a deviation and requires:
- An approved deviation record
- A time-limited scope (e.g., only for a specific recovery deployment)
- Re-enabling in the next release

---

## Dev/CI Environment Recovery

If applied migrations on a dev environment are corrupted (e.g., by a WIP rebase that was accidentally pushed):
- Do NOT modify the migration scripts to match the corrupted state
- Use `validate-on-migrate=false` as a temporary, deviation-approved measure to allow the service to start
- Correct the database state via a new idempotent migration (`IF NOT EXISTS` pattern)
- Re-enable validation immediately after recovery
