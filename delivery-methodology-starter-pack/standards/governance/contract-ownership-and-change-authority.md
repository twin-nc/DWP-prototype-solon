---
id: STD-GOV-004
title: Contract Ownership and Change Authority
status: Approved
owner: Architecture & Governance
applies_to: All services, portals, and pipelines
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - governance/canonical-contract-versioning-and-parity.md
  - integration/integration-reliability-and-reconciliation.md
last_changed: 2026-04-07
---

## Purpose

Define who can change contracts and how decisions are made and communicated.

---

## Ownership

Every contract MUST have an owner group (service team or platform team). Ownership includes responsibility for:
- Versioning and deprecation policy
- Parity checks and consumer compatibility
- Change communication and migration support

---

## Change Authority

### External Contracts

Breaking changes MUST:
- Introduce a new major version or new endpoint/topic
- Provide a migration plan and a deprecation window
- Include consumer testing or compatibility evidence
- Be approved by the contract owner and affected consumers (or programme governance)

### Internal Contracts

Internal contracts MAY evolve faster but MUST still maintain parity and stability. If an internal contract becomes externally consumed, it MUST be promoted to external contract governance immediately.

---

## BFF and Portal Boundary

- A BFF (Backend for Frontend) MUST NOT become an ungoverned policy engine.
- Business rules MUST remain in governed rule/policy services or bundles — not hidden in UI or BFF code.
- UI/BFF contracts MUST follow the same versioning and evidence rules as backend services.

---

## Runtime/Spec Parity

No release promotion if runtime and published specs diverge. See `governance/canonical-contract-versioning-and-parity.md`.
