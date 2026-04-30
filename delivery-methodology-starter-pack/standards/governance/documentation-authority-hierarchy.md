---
id: STD-GOV-001
title: Documentation Authority Hierarchy
status: Approved
owner: Architecture & Governance
applies_to: All services, portals, and pipelines
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - governance/requirements-traceability-and-id-governance.md
  - governance/release-evidence-and-signoff.md
  - governance/canonical-contract-versioning-and-parity.md
last_changed: 2026-04-07
---

## Purpose

Define what is authoritative when sources conflict (requirements vs standards vs contracts vs tests vs code), and establish a consistent conflict-resolution mechanism.

---

## Definitions

- **Domain overlay:** approved rules, policy, or configuration that applies to a specific domain context and is effective-dated.
- **Canonical contract:** versioned OpenAPI/AsyncAPI schemas and associated JSON schemas that define service interfaces and events.
- **Evidence pack:** append-only bundle of artifacts supporting a release decision (tests, mappings, signoffs, diffs, run IDs).

---

## Authority Order

When sources conflict, the **highest-ranked** source is authoritative:

1. **Applicable law and approved domain overlays** — includes legal references, policy mappings, and official interpretations adopted by the programme.
2. **Master solution requirements baseline** and approved deviations.
3. **This standards pack.**
4. **Canonical contracts and schemas.**
5. **Approved test suites and release evidence definitions.**
6. **Code.**
7. **Examples and prose.**

### Notes

- Standards govern *how* to implement, not *what the law or requirement is*. They cannot override law or approved requirements.
- Contracts define integration behavior and must not drift from runtime (see parity standard).
- Tests validate behavior. Tests do not define behavior unless explicitly mapped to a requirement and approved.
- `docs/memory.md` is an operational context document, not an authority source — it reflects current state but does not override the documents above.

---

## Conflict Handling Procedure

When you discover a conflict:

1. **Identify** all conflicting statements and their sources.
2. **Apply** the authority order above.
3. If the authoritative source is unclear, **open a decision record** and treat the area as **release-blocking** until resolved.
4. If you must ship before resolution, open a **deviation** with explicit signoffs and a remediation plan.

---

## Required Artifacts

- A link or reference ID to the authoritative requirement, overlay, or contract section.
- A trace mapping update (see traceability standard) if behavior changes.
- Evidence that downstreams were informed if contracts change.
