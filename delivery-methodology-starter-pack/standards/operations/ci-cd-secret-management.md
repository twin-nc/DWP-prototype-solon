---
id: STD-OPS-002
title: CI/CD Secret Management
status: Approved
owner: DevOps / Release Engineering
applies_to: All CI/CD pipelines and deployment workflows
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - security/security-boundaries-and-fail-fast-controls.md
  - security/data-sensitivity-and-redaction.md
last_changed: 2026-04-07
---

## Purpose

Ensure CI/CD pipeline secrets are documented, governed, rotated, and never exposed in logs or source control.

---

## Secret Register (MUST)

Every secret used in GitHub Actions (or equivalent CI/CD) workflows MUST be listed in `docs/project-foundation/CI-SECRET-REGISTER.md` with:
- Secret name (as it appears in the workflow)
- Purpose
- Source (where to obtain the value)
- Owner (who is responsible for it)
- Rotation policy
- Last rotated date

A CI workflow that references a `secrets.*` value not in the register is a policy violation.

---

## Day-Zero Requirement

All required CI/CD secrets MUST be added to the platform (GitHub Actions Secrets) before the first CI run. Missing secrets on the first run are a setup failure, not a configuration error to be debugged mid-sprint.

---

## Secret Hygiene (MUST)

- Secrets MUST NEVER appear in:
  - Source code
  - Configuration files committed to the repository
  - PR descriptions, issue comments, or commit messages
  - CI/CD logs (use masked variables)
  - Documentation (the register stores names only, never values)
- Pipelines MUST use platform-native secret masking (GitHub Actions masks secrets in logs automatically, but do not construct secrets from visible components in log output)

---

## Rotation (MUST)

- Secrets MUST be rotated on the schedule defined in the register (default: annually).
- Rotation MUST be:
  1. Announced to the team at least 24 hours before execution
  2. Applied to the CI/CD platform first
  3. Verified with a CI run before the old secret is deactivated
  4. Logged (update `Last Rotated` in the register and commit)
- If a secret is compromised or suspected compromised, rotation is immediate and takes priority over any in-progress work.

---

## Expiry and Outage Prevention

- If a secret expires or is revoked unexpectedly, the owner must update the CI/CD platform **within the same working day**.
- The team must be notified immediately of an expiry-caused CI failure.
- Do not leave the pipeline broken overnight due to an expired secret.

---

## Separation of Concerns

- Secrets for different environments (dev / staging / production) MUST be separate values — never share a production secret with a non-production environment.
- Developer local secrets (e.g., personal API keys) are in developers' local shell profiles and never in the repository.
