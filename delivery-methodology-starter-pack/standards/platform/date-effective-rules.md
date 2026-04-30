---
id: STD-PLAT-002
title: Date-Effective Rules
status: Approved
owner: Rules & Policy Platform
applies_to: All services with rule-driven or policy-driven behavior
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - platform/determinism.md
  - platform/evidence-immutability-and-replay.md
last_changed: 2026-04-07
---

## Purpose

Support rule and policy changes over time with full replayability and legal/audit defensibility.

> **Note:** This standard applies to projects with effective-dated rules or policies (e.g., regulatory rates, configuration policies, jurisdiction-specific logic). If your project has no rule-driven behavior, this standard is not applicable — mark it as N/A in your deviation record.

---

## Effective Dating Model (MUST)

Rules and policy data MUST be effective-dated and selected using:
- Domain context (e.g., jurisdiction, entity type)
- Effective date (and where applicable: period, type)
- Explicit applicability keys

The selection algorithm MUST be deterministic and documented.

---

## Tie-Breakers (MUST)

When multiple rule entries are applicable:
1. Most specific applicability (more keys matched)
2. Most recent effective date not after the context date
3. Highest precedence flag (explicit)
4. Stable identifier order as final tie-break

---

## Rule References (MUST)

Each rule/policy entry MUST reference:
- A legal citation, policy document, or programme reference
- Change rationale or reference
- Effective date range

---

## Code vs Policy Separation

- Rule/policy changes SHOULD be expressed in policy data (bundles, config) where feasible — not in application code.
- Engine/runtime code changes that alter rule behavior MUST be governed separately and require full evidence.

---

## Versioning

Rule engine version and policy bundle version MUST be included in every outcome record and evidence pack.
