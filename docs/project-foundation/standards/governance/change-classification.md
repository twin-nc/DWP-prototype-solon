---
id: STD-GOV-003
title: Change Classification
status: Approved
owner: Architecture & Governance
applies_to: All services, portals, and pipelines
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - governance/release-evidence-and-signoff.md
  - governance/canonical-contract-versioning-and-parity.md
  - integration/error-semantics-and-stability.md
last_changed: 2026-04-07
---

## Purpose

Provide a consistent taxonomy for changes so approvals, evidence, and rollout requirements are predictable.

---

## Classes

### Class A — Behavior / outcome change (release-blocking)

Changes that can alter business, legal, or financial outcomes:
- rule logic or policy data changes
- rounding, thresholds, currency handling
- state transition logic impacting domain record status
- changes to authorization that affect what outcomes are visible to which actors

**Required:**
- Deterministic scenario suite rerun
- Rule replay tests (if applicable)
- Updated domain reference mappings
- Updated trace map
- Release evidence pack + signoffs including domain/legal/policy owner

### Class B — External contract change (release-blocking unless strictly additive)

Changes to OpenAPI/AsyncAPI or externally observed behavior:
- breaking changes require a migration plan and version bump
- additive changes require compatibility evidence

**Required:**
- Contract parity checks
- Consumer impact assessment
- Versioning and migration documentation
- Updated trace map
- Release evidence pack + signoffs including contract owner

### Class C — Internal refactor (typically non-blocking)

Changes that do not alter externally observed behavior:
- code refactors
- performance improvements with no outcome change
- internal data structure changes behind stable contracts

**Required:**
- Regression tests
- Parity checks if relevant
- Updated trace map if requirement mapping changes

### Class D — Operational / observability / deployment (sometimes blocking)

Changes to:
- logging, metrics, tracing
- infrastructure, deployment pipelines
- security controls
- runtime configurations

**Required:**
- Verification in a target-like environment
- Security control checks if applicable
- Updated evidence requirements if CI gates change

### Class E — Documentation only (non-blocking)

Documentation updates without behavior or contract changes.

**Required:**
- Standard peer review

---

## Classification Rules

- If uncertain between classes, choose the **more restrictive** class.
- Any change involving money amounts, dates, or rule selection MUST be Class A unless proven otherwise.
- Any change that can affect retries or reconciliation behavior MUST be Class B or D depending on external visibility.
- A behavioral change without schema change still requires a change classification and evidence (it is not automatically Class E).

---

## Using the Classification

1. Add the class to the PR description.
2. Ensure the correct evidence is in place before merge (see `governance/release-evidence-and-signoff.md`).
3. If you use the `change-classification-assistant` skill, it will guide classification and surface the required evidence checklist.
