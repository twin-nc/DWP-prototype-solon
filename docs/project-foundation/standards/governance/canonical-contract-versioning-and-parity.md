---
id: STD-GOV-005
title: Canonical Contract Versioning and Parity
status: Approved
owner: Architecture & Governance
applies_to: All services, portals, and pipelines
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - governance/contract-ownership-and-change-authority.md
  - integration/error-semantics-and-stability.md
last_changed: 2026-04-07
---

## Purpose

Ensure all interfaces are versioned, contract-first, and remain consistent with runtime behavior.

---

## Contracts Are Canonical

OpenAPI/AsyncAPI (and associated schemas) are the canonical definition of:
- Endpoints/topics
- Request/response/event schemas
- Error envelopes
- Authentication/authorization semantics at a contract level

Runtime behavior derives from the contract. The contract is never derived from the runtime.

---

## Versioning

Use semantic versioning for contracts.

- **Breaking changes** require a major version bump and a migration plan.
- **Additive changes** MUST be backward compatible and MUST be optional for consumers.
- **Behavioral changes** without schema change still require a change classification and evidence — they are not automatically non-breaking.

---

## Runtime/Spec Parity (MUST)

For every build intended for release:
- Generate or validate runtime endpoints against the published spec.
- Validate request/response examples (where used) against schemas.
- Ensure error envelopes and status codes match the spec.

If parity fails, the build is **not eligible** for promotion.

---

## Deprecation

Deprecations MUST:
- Be announced to consumers with a minimum notice period (set by project policy)
- Have an end-of-life date
- Provide migration guidance
- Remain supported through the announced window

---

## Compatibility Evidence

At minimum for external contracts, one of:
- Consumer contract tests, or
- Compatibility harness with golden request/response fixtures tied to requirements
