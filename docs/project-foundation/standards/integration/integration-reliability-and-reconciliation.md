---
id: STD-INT-002
title: Integration Reliability and Reconciliation
status: Approved
owner: Integration Platform
applies_to: All services, portals, and pipelines
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - integration/error-semantics-and-stability.md
  - platform/state-resolution-precedence.md
  - platform/evidence-immutability-and-replay.md
last_changed: 2026-04-07
---

## Purpose

Make cross-system flows reliable, idempotent, and reconcilable.

---

## Idempotency (MUST)

- Submission endpoints MUST support idempotency keys for operations that create records with legal or financial significance.
- Replays and retries MUST NOT create duplicate records.
- Idempotency behavior MUST be documented and tested.

---

## Retry Taxonomy (MUST)

Define and enforce:
- **Retryable errors:** dependency unavailable, rate limit, transient infrastructure
- **Non-retryable errors:** validation failure, authorization failure, conflict (duplicate record)

Retries MUST:
- Be bounded (maximum attempts + backoff policy)
- Preserve correlation IDs and evidence linkage
- Not alter the idempotency guarantee

---

## Reconciliation (MUST)

When systems diverge, reconciliation MUST be possible and governed:

- Define a reconciliation trigger (periodic checks / event-driven mismatch / manual operator with audit trail)
- Define ownership for reconciliation decisions
- Reconciliation actions MUST create append-only evidence artifacts

---

## External Boundary Semantics

The platform MUST clearly distinguish:
- Authoritative internal state
- Externally reported state
- Pending or provisional integration attempts

---

## Outputs to External Systems

If the platform produces outputs for external systems (e.g., data exports, handoff payloads):
- Outputs MUST be deterministic and idempotent
- Each output MUST reference the source record lineage and versions used
