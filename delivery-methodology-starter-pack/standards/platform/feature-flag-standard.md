---
id: STD-PLAT-007
title: Feature Flag Standard
status: Approved
owner: Platform Engineering
applies_to: All services, portals, and pipelines
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - development/testing-standard.md
  - security/data-sensitivity-and-redaction.md
last_changed: 2026-04-07
---

## Purpose

Define how feature flags are introduced, documented, managed in CI, and removed to prevent flags accumulating as permanent technical debt.

---

## Registration (MUST)

Every feature flag MUST be registered in `docs/project-foundation/FEATURE-FLAG-REGISTER.md` with:
- Flag name (exact string)
- Type: `release`, `experiment`, or `ops`
- Scope: which services/frontends use it
- Owner
- Issue number that introduced it
- Date introduced
- Target removal date or version

A PR that introduces a flag without a register entry is not eligible for merge.

---

## CI Handling (MUST)

Flags that affect test behavior MUST be explicitly set in all relevant CI job environment blocks.

- Do not rely on default absence of a flag as a test signal — tests that behave differently based on flag presence must set the flag explicitly.
- If a flag enables a feature that changes the outcome of an E2E test, both "flag on" and "flag off" paths must be either tested or explicitly documented as out of scope.

---

## Types and Lifecycle

### Release Flags
- Purpose: control rollout of a feature in progress
- MUST be removed when the feature is fully released and stable
- Target removal date MUST be set at introduction

### Experiment Flags
- Purpose: A/B testing or gradual rollout
- MUST be removed when the experiment concludes
- Experiment results MUST be documented before flag removal

### Ops Flags
- Purpose: operational kill switches, circuit breakers, emergency disablement
- MAY be permanent, but MUST be reviewed annually
- Permanent ops flags must be re-confirmed in the register each year

---

## Cleanup Obligation

- A flag that passes its target removal date without being removed automatically becomes a tracked tech-debt issue.
- Business logic MUST NOT bifurcate permanently through a flag — flags are for rollout management only, not for long-term conditional behavior.
- When a flag is removed, the PR MUST remove the flag from the register and clean up all conditional branches in code.

---

## Prohibited Uses

- Flags MUST NOT be used to hide incomplete or broken features in production environments without a corresponding approved deviation.
- Flags MUST NOT be used to bypass security controls.
- Flags MUST NOT carry sensitive configuration values — use secrets management for credentials and keys.
