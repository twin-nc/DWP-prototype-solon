---
id: STD-GOV-006
title: Release Evidence and Signoff
status: Approved
owner: Architecture & Governance
applies_to: All services, portals, and pipelines
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - governance/requirements-traceability-and-id-governance.md
  - platform/evidence-immutability-and-replay.md
  - governance/test-authority-and-truth-hierarchy.md
last_changed: 2026-04-07
---

## Purpose

Make release decisions based on objective, same-cycle evidence that is auditable and reproducible.

---

## Core Rule

A release MUST NOT be promoted to production unless it has:
- Complete trace mapping updates
- Required tests executed in the same cycle
- An evidence pack with immutable references
- Required signoffs

---

## Evidence Pack (MUST)

An evidence pack is an append-only bundle containing:

- Release identifier (build SHA, artifact IDs)
- Environment and configuration identifiers
- Contract versions and parity reports
- Rule/policy bundle versions and effective-date metadata (if applicable)
- Test run IDs and reports:
  - Unit and integration test suite
  - Contract tests
  - E2E test suite (at minimum for `main` → deployment)
  - Security controls checks (if applicable)
- Operational validation results (where required)
- Trace map snapshot used for the decision
- Signoffs and timestamps
- Any approved deviations with mitigation

Evidence packs MUST be immutable (use content hashes) and retained per project retention policy.

---

## Signoffs (MUST)

At minimum:
- **Contract owner signoff** for external interface changes (Class B)
- **Domain/legal/policy owner signoff** for Class A changes
- **Security signoff** when security controls or auth boundaries change
- **Release manager / governance signoff** for promotion to production

---

## Deviations and Deferrals

- Deviations MUST be explicit and linked to:
  - Requirements impacted
  - Risks accepted
  - Remediation plan and date
- Deviations are **not silent**. They require signoff and must appear in the evidence pack.
- A deviation without signoff is a release blocker, not a shortcut.

---

## Prohibited Release Practices

- "We ran it locally" as the only evidence
- Using test runs from a previous cycle
- Releasing with known contract parity failures
- Releasing with missing trace links
- Releasing with a deviation that has no signoff
