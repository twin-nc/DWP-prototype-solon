---
id: STD-PLAT-004
title: State Resolution and Precedence
status: Approved
owner: Domain Platform
applies_to: All services with stateful domain records
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - platform/immutable-records-and-corrections.md
  - integration/integration-reliability-and-reconciliation.md
last_changed: 2026-04-07
---

## Purpose

Define how "current state" is derived from immutable history and how conflicts are resolved deterministically.

---

## Legal Record vs Derived View

- The authoritative record is immutable (events, records, corrections).
- "Current state" is a derived projection from immutable history.
- Derived views MAY be rebuilt but MUST be reproducible from the immutable record.

---

## Precedence Rules (MUST)

When deriving state from history:

1. Validated corrections/amendments supersede the records they correct for the same entity and period.
2. If multiple corrections exist:
   - Choose the correction chain with the latest accepted timestamp
   - If ties, apply deterministic identifier ordering
3. If an item is voided or withdrawn, it remains in history but is excluded from the "active" view based on status.

---

## State Transitions

Projects MUST define explicit state transition models for all stateful domain entities. Each model MUST include:
- All valid states
- All valid transitions (from → to)
- Which transitions are terminal (irreversible)
- Which events trigger each transition

All transitions MUST be:
- Explicit (not inferred)
- Versioned (state model version)
- Traceable to events in the immutable record

---

## Conflict Detection

Divergent or impossible state sequences MUST be detectable and produce a `reconciliation_required` signal (see `integration/integration-reliability-and-reconciliation.md`).
