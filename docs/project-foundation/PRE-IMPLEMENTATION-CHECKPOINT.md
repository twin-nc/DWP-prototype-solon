# Pre-Implementation Checkpoint

## Purpose
Before any Builder starts implementation on a non-trivial feature, this checkpoint must be complete. Its purpose is to prevent the three most expensive delivery failures in agentic AI projects:

1. **Prototyping against one context** — domain model gaps emerge when a second context is added, requiring schema migrations and multi-service rework.
2. **Late infrastructure reversals** — adopting a tool without vetting license, ecosystem, and operational fit, then reversing mid-project at high cost.
3. **Scope ambiguity** — builders implement features that are later confirmed out of scope, or over-engineer for a deferred phase.

The Delivery Designer and Business Analyst are jointly responsible for completing this checkpoint before handing off to any Builder role.

---

## When to Apply This Checkpoint

Apply for any feature that:
- Introduces a new domain entity, state machine, or aggregate
- Adopts a new external dependency, library, or integration protocol
- Spans multiple target contexts (jurisdictions, tenants, customer types)
- Has regulatory, legal, or compliance implications
- Involves schema changes

For trivial bug fixes, documentation updates, or minor UI changes: skip the checkpoint.

---

## The Checkpoint

### Gate 1 — Acceptance Criteria Coverage

- [ ] Acceptance criteria exist on the GitHub issue
- [ ] ACs cover **all** target contexts, not just the primary one
- [ ] At least one negative case is stated (what must NOT happen)
- [ ] Edge cases identified: boundary dates, empty/null inputs, concurrent operations
- [ ] For multi-language systems: all user-facing strings have locale key entries pre-registered (do not defer i18n to a follow-up commit)
- [ ] **Context currency:** Confirm that `docs/memory.md` and the relevant domain pack files reflect any changes merged since this issue was written. If either is stale relative to the issue, update before invoking the builder.

**Who completes this:** Business Analyst
**Artifact:** Acceptance criteria on the issue (or dedicated AC file)

---

### Gate 2 — Infrastructure and Dependency Vetting

Required whenever a new tool, library, integration protocol, or external service is being adopted.

- [ ] License verified: confirmed OSI-approved or explicitly acceptable under project policy
- [ ] Ecosystem fit verified: compatible with the project's language stack and runtime
- [ ] Operational fit verified: can be operated by the team; runbook requirements understood
- [ ] Existing infrastructure reviewed: confirm an existing component does not already cover this need
- [ ] Decision record created: Decision Log entry with alternatives considered and rejection rationale

**For features with remote deployment impact** (new service, new route, infrastructure change, Helm chart change):
- [ ] DevOps / Release Engineer consulted during design — not deferred to build or release phase
- [ ] Deployment pipeline for this feature is designed and agreed before implementation starts
- [ ] Impact on GitOps sync, routing, and Helm charts is understood and documented
- [ ] Remote environment spec (`docs/project-foundation/remote-environment-spec.md`) is current and covers this feature's deployment requirements

> **Why this gate exists:** Infrastructure reversals at agentic velocity are expensive. A decision that takes one session to make can take ten sessions to unwind. Front-load the vetting. For features touching remote infrastructure specifically: DevOps/RE involvement at design phase is not optional — late inclusion forces the team to retrofit deployment design after code is written, under time pressure.

**Who completes this:** Delivery Designer (with Solution Architect sign-off; DevOps/RE for remote deployment sub-checks)
**Artifact:** Decision Log entry at `docs/development/decision-<date>-<name>.md`

---

### Gate 3 — Scope Statement

- [ ] Feature is explicitly confirmed **in scope** for the current release/phase
- [ ] For Release 1 work, scope has been checked against `docs/release/release-1-capabilities.md`
- [ ] Any adjacent features that are **out of scope** (deferred) are named
- [ ] Rationale for the scope boundary is documented (legal, capacity, dependency, deliberate deferral)
- [ ] The BA and Delivery Lead have confirmed the scope boundary

> **Why this gate exists:** Scope ambiguity causes builders to over-engineer for deferred features. The cost is design complexity and rework when the deferral is confirmed late.

**Who completes this:** Business Analyst + Delivery Lead
**Artifact:** Scope statement on the GitHub issue (or in the feature's build plan)

---

### Gate 4 — Domain Pack (multi-context projects only)

Required when the feature introduces or modifies domain behavior and the project serves more than one context (jurisdiction, tenant type, entity class).

- [ ] Domain entities and state machines prototyped against **all** target contexts
- [ ] Fields, rules, or transitions that differ between contexts are identified and named
- [ ] Discriminator fields required by multi-context modeling are identified before schema is locked
- [ ] Tax, legal, or regulatory references confirmed for each context

> **Why this gate exists:** A domain model that works for Context A often requires schema migration and multi-service changes when Context B is added. Catching the divergence before coding is dramatically cheaper than after.

**Who completes this:** Business Analyst + Domain Expert (if applicable)
**Artifact:** Domain pack or issue comment confirming cross-context coverage

---

## Checkpoint Sign-Off

Before the first Builder commit, the issue must contain:

```
## Pre-Implementation Checkpoint
- Gate 1 (ACs): ✅ Complete — all contexts covered
- Gate 2 (Infrastructure): ✅ Complete / ⏭️ Not applicable (no new dependencies)
- Gate 3 (Scope): ✅ Confirmed in scope for Release X (Release 1 checked against `docs/release/release-1-capabilities.md` where applicable)
- Gate 4 (Domain pack): ✅ Complete / ⏭️ Not applicable (single-context feature)

Ready for implementation: ✅
```

If any gate is incomplete, the issue is not ready for a Builder. Do not start implementation with gates open — fix them or document an explicit deferral with Delivery Lead sign-off.

---

## Integration with the Handoff Protocol

The pre-implementation checkpoint is the artifact that enables the handoff from Delivery Designer → Solution Architect → Builder. No Builder should receive a task without confirmed checkpoint sign-off on the linked issue.

See `AGENT-OUTLINES.md` §Role Handoff Protocol.
