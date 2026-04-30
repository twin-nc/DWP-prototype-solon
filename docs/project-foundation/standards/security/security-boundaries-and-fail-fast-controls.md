---
id: STD-SEC-001
title: Security Boundaries and Fail-Fast Controls
status: Approved
owner: Security Engineering
applies_to: All services, portals, and pipelines
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - integration/error-semantics-and-stability.md
  - security/data-sensitivity-and-redaction.md
  - governance/release-evidence-and-signoff.md
last_changed: 2026-04-07
---

## Purpose

Ensure the system enforces clear authorization boundaries and fails safely when critical controls are misconfigured.

---

## Authentication and Authorization Boundaries

- Services MUST enforce authentication and authorization as defined by the project's auth design.
- Authorization decisions MUST be auditable and traceable.
- Authentication MUST use the project's approved identity provider — no bespoke auth implementations.
- See `CLAUDE.md` §Tech Stack for the approved identity provider for this project.

---

## Fail-Fast Controls (MUST)

The system MUST fail at startup or deployment (not limp along silently) when:
- Signing keys are missing or invalid
- Critical auth configuration is absent or malformed
- Required encryption settings are not present
- Policy bundle integrity checks fail (if applicable)

Fail-fast events MUST:
- Be observable (logged at ERROR, alert triggered)
- Produce a stable error code
- Not leak secrets or sensitive configuration in error output

---

## Least Privilege

- Credentials MUST be scoped to the minimum required privileges.
- Service accounts MUST NOT have admin-level access unless strictly required and documented.
- Separation of duties SHOULD exist between policy authors, approvers, and deployers.

---

## Security Evidence

Security control checks MUST be included in release evidence packs when security controls or auth boundaries are impacted. This applies to Class A and Class D changes affecting auth, encryption, or signing.

---

## Dependency Scanning

- CI MUST include dependency vulnerability scanning (e.g., `npm audit`, OWASP Dependency-Check, Dependabot).
- High and critical vulnerabilities MUST be resolved or have an approved deviation before production promotion.
