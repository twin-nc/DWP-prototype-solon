---
id: STD-PLAT-003
title: Evidence Immutability and Replay
status: Approved
owner: Audit & Evidence Platform
applies_to: All services, portals, and pipelines
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - platform/immutable-records-and-corrections.md
  - governance/release-evidence-and-signoff.md
last_changed: 2026-04-07
---

## Purpose

Ensure outcomes can be reconstructed and defended: "what was decided, why, and under which rules."

---

## Append-Only Evidence (MUST)

- Evidence artifacts MUST be immutable once written.
- Corrections MUST be represented as new artifacts linked to prior ones — never by overwriting.

---

## Minimum Replay Package

For any governed outcome (record, assessment, decision), store:

- Canonical input payload hash
- Normalized context (domain, effective date, period, entity identifiers — as applicable)
- Rule engine version (if rule-driven)
- Policy bundle version(s) (if applicable)
- Contract/schema version(s)
- Reference IDs used (legal citations, policy references — if applicable)
- Deterministic computation outputs
- Correction/amendment lineage pointers (if applicable)
- Timestamps and actor/system identity

---

## Replay Procedure

A replay MUST:
- Reconstruct the same result given the same inputs and versions
- Produce the same error codes if the original failed
- Validate that the same versions were used

---

## Evidence Retention

Retention MUST satisfy project policy and legal/regulatory obligations.
Evidence deletion (if ever permitted) MUST be:
- Explicitly approved by the relevant authority
- Rare
- Logged with justification, scope, and approver
