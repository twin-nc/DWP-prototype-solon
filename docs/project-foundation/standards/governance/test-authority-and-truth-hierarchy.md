---
id: STD-GOV-007
title: Test Authority and Truth Hierarchy
status: Approved
owner: Architecture & Governance
applies_to: All services, portals, and pipelines
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - platform/determinism.md
  - platform/evidence-immutability-and-replay.md
  - governance/change-classification.md
  - development/testing-standard.md
last_changed: 2026-04-07
---

## Purpose

Define what tests are allowed to assert as "truth" and prevent snapshots from encoding incorrect behavior.

---

## Principle

**Requirements and approved overlays define truth.** Tests validate truth; they do not invent it.

---

## Hierarchy

When test suites conflict, the following ordering applies:

1. **Requirements-backed contract tests and deterministic scenario tests**
   — each assertion MUST map to a requirement ID and (where relevant) a legal or policy reference.
2. **Rule replay / lineage tests**
   — reconstructability and effective-date selection.
3. **Golden / snapshot tests**
   — authoritative only when derived from approved requirement behavior.
4. **Property tests**
   — useful for invariants and robustness.
5. **Advisory / exploratory tests**
   — non-blocking unless explicitly marked as a CI gate.

---

## Golden Test Rules

Golden fixtures MUST include:
- Rule/policy bundle version (if applicable)
- Effective date context (if applicable)
- Domain context
- Schema version

Golden fixtures MUST be regenerated only via an approved process that includes:
- Requirement mapping review
- Signoff for Class A changes

---

## Test Gate Definition

Release gates MUST be:
- Declared and versioned
- Included in the release evidence pack

A gate definition change is a Class D change (operational) and requires evidence that the change does not silently drop coverage.
