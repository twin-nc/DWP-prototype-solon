# Agent Outlines

## Purpose
Defines the team's agent-role structure, ownership, handoff protocol, and output standards.
Detailed per-role instructions are in `.claude/agents/`.

---

## Build-Oriented Specialist Roles

| Agent Role | Purpose | Default Mode |
|---|---|---|
| Delivery Designer | Proposes design options, module boundaries, and implementation alternatives before any decision is locked. | Execute |
| Solution Architect | Locks final system design decisions, integration constraints, and architecture guardrails. | Execute |
| Business Analyst | Domain rules, state machines, acceptance criteria, and business invariants. | Execute |
| DB Designer | Schema design, migrations, indexing, and persistence modeling. | Execute |
| DevOps / Release Engineer | CI/CD, pipeline evidence, deployment safety, release controls, and environment operational correctness. | Execute |
| SRE / Platform Engineer | Day-to-day operational health: Compose/Helm parity, environment config, platform observability, infrastructure correctness. | Execute |
| Backend Builder | Services, APIs, controllers, and backend implementation. | Execute |
| Frontend Builder | Forms, pages, workflow screens, and user-facing implementation. | Execute |
| Integration / Protocol Specialist | External system adapters, integration protocols (REST, SFTP, event streaming), idempotency across boundaries, schema compatibility, partial-failure handling. | Execute |
| Test Builder | Unit, integration, and end-to-end test implementation. | Execute |
| Refactorer | Cleanup and restructuring without behavior change. | Execute |
| Traceability Steward | Maintains requirements↔contract↔test↔evidence trace map; updates per feature, not per release. | Execute |
| Integration / API Contract Agent | Drafts and governs OpenAPI/AsyncAPI contracts; tracks consumer compatibility. | Execute |

> **Test Designer** is not a standalone invocation role. Test strategy is embedded as a step within build planning — invoke the `test-plan` skill or include test planning in the feature's build plan (via `issue-to-build-plan`). A dedicated Test Designer session is only warranted for large, multi-service features with complex coverage decisions.

> **Domain Expert roles** (e.g., Tax Domain Expert, Legal Specialist, Regulatory Specialist) are optional but should be provisioned at project start if the domain has jurisdiction-specific, legally consequential, or regulated rule interpretation requirements. Domain Experts should be **one sprint ahead of the Backend Builder** at all times — domain interpretation is the slowest and least parallelisable part of delivery.

---

## Agent Invocation Protocol

What the orchestrator must prepare before invoking any agent.

### Required inputs for every invocation

| Input | What it is | Where it comes from |
|---|---|---|
| Issue text | Scoped GitHub issue including acceptance criteria | GitHub |
| Relevant file paths | Files the agent needs to read to do its work | Orchestrator judgment |
| Upstream Handoff Declaration | Structured output from the prior role in the chain | Prior agent session |

Do not pass full documents the role file already summarises. Do not pass the reasoning chain from a prior session.

### Invocation model by role class

| Role class | Invocation model | Reason |
|---|---|---|
| Producer roles (builder, test-builder, db-designer) | May be invoked implicitly by a parent session when inputs are fully prepared | Inputs are well-defined; outputs are deterministic |
| Design and analysis roles (BA, Delivery Designer, SA) at feature start | Invoke explicitly — human constructs the brief | Requires current project context and human framing |
| Critic and reviewer roles (Design Critic, Code Reviewer) | Always invoke explicitly — human carries only artifact and evaluation criteria to a new context window | Independence guarantee; prompt construction discipline |

### Critic and reviewer invocation prompt format

```
## [BLOCKING / ADVISORY] <Role> Review Required

Open a new context window and invoke the <Role> with this prompt:

---
You are a <Role>. Review the following [artifact type] for [issue/feature reference].

Evaluate against:
- [Link to relevant requirements or acceptance criteria]
- [Link to applicable standard(s)]

Artifact:
[Pasted content or file path]
---

Return findings before [next step that is blocked / before merge].
```

---

## Reviewer and Critic Roles

These roles are independent, read-only checks unless the team explicitly asks for edits.

| Agent Role | Purpose | Default Mode |
|---|---|---|
| Code Reviewer | Reviews PR-sized changes for readability, maintainability, conventions, regressions, and missing tests. | Read-only |
| Design Critic | Challenges architecture choices, assumptions, edge cases, domain gaps, and failure modes. **Mandatory before any Solution Architect decision is locked.** | Read-only |
| Security Reviewer | Reviews security boundaries, auth controls, sensitive data handling, and fail-fast controls. | Read-only |

> **Design Critic is not optional.** Before any Solution Architect decision is finalised and recorded in a Decision Log, a Design Critic pass is required. This prevents the SA from reviewing their own proposals. The DC need not produce a long document — a brief challenge pass with explicit "no material risk found" sign-off is sufficient for low-complexity decisions. See AGENT-RULES.md rule 23.

> **Fresh context window required.** Critic and reviewer roles must always be invoked in a fresh context window with only the artifact and evaluation criteria. See AGENT-RULES.md rule 11.

> **Santa Method for Class A changes and release evidence.** For Class A decisions, release evidence sign-off, and security-sensitive merges, use the `santa-method` skill: two independent reviewers with identical rubric, no shared context, both must pass. See `.claude/skills/santa-method/SKILL.md`.

### Code Reviewer — Severity Action Table

| Severity | Action | What It Means |
|---|---|---|
| CRITICAL | BLOCK — PR cannot merge | Security vulnerability, data loss, broken invariant |
| HIGH | WARN — must be resolved or explicitly deferred with reason | Bug under specific conditions, missing test for key path |
| MEDIUM | INFO — should be resolved before next release | Code smell, suboptimal pattern, minor inconsistency |
| LOW | NOTE — author discretion | Style, naming, documentation suggestion |

**Pre-review requirements:** CI must be passing, branch must be up to date, and conflicts must be resolved before invoking the Code Reviewer. Reviewer time on a failing branch is waste.

---

## Human Roles (non-agent)

| Role | Responsibilities |
|---|---|
| Delivery Lead | Stakeholder communication, Build Phase Tracker ownership, escalation point for scope and priority disputes, final approval for deviations |
| Product Owner / BA lead | Requirements sign-off, acceptance criteria approval, legal/compliance liaison |

---

## Role Disambiguation Rules

These rules exist because role boundaries blur in practice. Apply them at the point of routing a question or task.

| If the question is about... | Route to... |
|---|---|
| What the domain/business requires — rules, states, constraints | Business Analyst |
| Legal, regulatory, or jurisdiction-specific interpretation | Domain Expert |
| How a feature should be modelled across modules — options | Delivery Designer |
| Which option to select — final architectural decision | Solution Architect |
| Whether the selected option has blind spots or failure modes | Design Critic (mandatory before SA locks) |
| How to implement across service/system boundaries | Integration / Protocol Specialist |
| Whether a schema change is safe | DB Designer |
| Whether a deployment is safe | DevOps / Release Engineer |
| Whether the environment is correctly configured | SRE / Platform Engineer |
| Whether tests cover the right things | Test Builder (with `test-plan` skill) |
| Whether requirements trace to evidence | Traceability Steward |

---

## Role Handoff Protocol

Work moves through roles in a defined sequence. Do not start a downstream role until the upstream artifact exists.

| From | To | Required artifact before handoff |
|---|---|---|
| Product Owner / BA lead | Business Analyst | Issue created with scope and context |
| Business Analyst | Delivery Designer | Acceptance criteria written on the issue |
| Delivery Designer | Design Critic | Options doc (2-3 options with recommendation) |
| Design Critic | Solution Architect | DC challenge pass complete (even if "no material risk") |
| Solution Architect | Builder (Backend / Frontend / DB) | Design decision recorded (Decision Log or issue comment) |
| Solution Architect | Integration / Protocol Specialist | If feature crosses a system boundary |
| Builder | Test Builder | Feature note draft (scope and behavior) |
| Builder | Code Reviewer | PR opened with description complete |
| Test Builder | Code Reviewer | Tests committed to PR branch |
| Code Reviewer | Builder | Findings with severity labels delivered |
| Builder | DevOps / Release Engineer | CI green, feature note complete, PR approved |
| DevOps / Release Engineer | SRE / Platform Engineer | If deployment changes environment configuration or platform infrastructure |
| Delivery Designer | DevOps / Release Engineer | If feature has remote deployment, infrastructure, or routing impact — DevOps/RE must be consulted during design, not just at release |

**Class A changes (behavior or legal outcome):** Solution Architect sign-off is required as a PR comment or Decision Log entry before any Builder merges. For Class A decisions where the SA and DC disagree, or where the Delivery Designer's options doc has no clear winner, invoke the `council` skill (four-voice anti-anchoring decision framework).

**Pre-implementation checkpoint:** Before any Builder starts implementation on a non-trivial feature, the pre-implementation checkpoint in `PRE-IMPLEMENTATION-CHECKPOINT.md` must be completed. Confirm context currency before invoking a builder — not just that documents were approved at some prior point.

---

## Feedback Instructions

### Delivery Designer
*(formerly Solution Designer — renamed to clarify: this role proposes, the Solution Architect decides)*

1. Provide 2-3 feasible options with clear tradeoffs.
2. Recommend one option and state why.
3. Call out interface impact, dependency impact, and rollout impact.
4. List unresolved decisions that need team input before implementation starts.
5. Flag any infrastructure, license, ecosystem, or operational constraints that need vetting before a decision is made.

**Output format:** Options doc saved to the issue or `docs/development/`, structured as: Option A / Option B / Option C / Recommendation / Open questions / Constraints to vet.

### Solution Architect
1. State the selected approach and the alternatives explicitly rejected.
2. Define integration constraints and guardrails that Builders must not violate.
3. Flag any standards that apply and how the design satisfies them.
4. List what remains unresolved and who owns the resolution.
5. Confirm a Design Critic pass was completed before this decision is recorded.

**Output format:** Decision Log entry or issue comment. Builders must be able to start from this without ambiguity.

### Business Analyst

> **Disambiguation:** This role owns domain rules, state machines, and acceptance criteria. It does NOT own module boundary decisions (→ Delivery Designer) or legal/regulatory interpretation (→ Domain Expert). If a question spans both, split it: BA defines the business invariant, Domain Expert confirms the legal basis.

1. Write acceptance criteria in the form: *Given / When / Then* or numbered testable statements.
2. Include at least one negative case (what must NOT happen).
3. Call out invariants that must hold across all paths.
4. Flag requirements that conflict or are ambiguous — do not resolve them silently.
5. For multi-context projects: acceptance criteria must cover **all** target contexts, not just the first. Flag gaps explicitly.

**Output format:** Acceptance criteria added to the GitHub issue. For complex features, a dedicated AC file at `docs/development/feat-<issue-number>-acceptance-criteria.md`.

### DB Designer
1. State the schema change, migration number, and idempotency approach.
2. Confirm the migration satisfies the database migration standard (STD-PLAT-006).
3. Flag any rollback risk or expand-contract requirement.
4. List indexes added and the query patterns they support.

**Output format:** Feature note with schema diff summary. Migration file(s) in the codebase.

### DevOps / Release Engineer

> **Scope:** This role owns release gates and CI/CD pipeline correctness. It does NOT own day-to-day operational environment health (→ SRE / Platform Engineer). If a deployment change also affects environment configuration, loop in the SRE role.

1. State what was changed in CI/CD or deployment configuration.
2. List new secrets or environment variables required and confirm they are in the register.
3. Confirm evidence requirements for the release class.
4. Flag any deployment sequencing requirement (e.g., migration must run before new pods start).
5. Confirm Compose/Helm parity if either file changed — both must be updated in the same PR.

**Output format:** Feature note or Runbook update. CI/secret register updated.

### SRE / Platform Engineer

> **Scope:** This role owns operational correctness between releases — environment configuration, infrastructure health, platform observability, and the gap between local (Docker Compose) and production-equivalent (Helm/Kubernetes) configuration. Distinct from DevOps/RE which owns release gates and pipeline safety.

1. Confirm environment configuration parity (Compose and Helm/Kubernetes are consistent).
2. Flag environment-specific secrets or configuration that is not in the CI secret register.
3. Identify operational gaps: missing runbooks, uncovered alert conditions, stale environment state.
4. For infrastructure changes: confirm the change does not introduce drift between environments.

**Output format:** Runbook update or environment health note. Parity confirmation in PR description.

### Backend Builder
1. State what was implemented and which ACs it satisfies.
2. List all files changed with a one-line description of each.
3. Describe test coverage added and any gaps explicitly deferred.
4. Flag any deviation from the agreed design or any standards touched.

**Output format:** PR description and Feature Note (if criteria met).

### Frontend Builder
1. State what was implemented and which ACs it satisfies.
2. List all components and pages changed.
3. Confirm accessibility requirements met (or deferred with justification).
4. List feature flags introduced or modified.
5. For multi-language systems: confirm all user-facing strings have locale keys registered. Do not merge with hardcoded strings.

**Output format:** PR description and Feature Note (if criteria met). Feature Flag Register updated if a flag was added.

### Integration / Protocol Specialist

> **Scope:** This role owns the correctness of system-to-system integration — external adapters, protocol handling (REST, SFTP, event streaming, message queues), schema compatibility across boundaries, idempotency, partial-failure handling, and retry semantics. The Backend Builder nominally covers implementation; this role covers the integration-specific instincts that are different from service-internal logic.

1. Confirm the integration is idempotent under retry and partial failure.
2. State the schema compatibility approach (versioned, backward compatible, or migration required).
3. Define the retry taxonomy: what is retryable, bounded how, with what backoff.
4. Flag any divergence risk between the internal authoritative state and the external system's state.
5. Define the reconciliation trigger and ownership if systems can diverge.

**Output format:** Integration design note or feature note. Protocol decisions recorded in Decision Log if non-obvious.

### Test Builder
1. State which test cases from the test plan (or ACs) were implemented.
2. List test files created or modified.
3. Confirm tests are deterministic and independent (no shared mutable state).
4. Flag any test cases from the plan that were deferred.

> **Note:** Domain test generation and seed data creation are sub-steps within this role's workflow — use the `generate-domain-tests` and `seed-data-builder` skills as tools within a Test Builder session, not as separate agent invocations.

**Output format:** PR description. Test coverage summary in the Feature Note.

### Refactorer
1. State what was changed and confirm no behavior was altered.
2. List the files changed.
3. Describe how you verified no behavior change (tests passed, parity checks, etc.).
4. Flag any latent issues discovered but not changed (raise as separate issues).

**Output format:** PR description. Feature Note only if the refactor is non-trivial.

### Traceability Steward

> **Critical operating rule:** See AGENT-RULES.md rule 24 — trace map updates happen in the same cycle as the change they describe.

1. For each change, confirm the requirement → contract → test → evidence chain is intact.
2. Flag any trace gaps as `blocking` (missing entirely) or `non-blocking` (documentation gap only).
3. Update the trace map as part of the same cycle as the change.
4. Confirm evidence pack references are included where required.

**Output format:** Trace map update (YAML/JSON or markdown table). Issue comment confirming trace status.

### Integration / API Contract Agent
1. State the contract change (additive / breaking / behavioral without schema change).
2. Apply the contract versioning standard (STD-GOV-005) and state the correct version bump.
3. Produce or update the OpenAPI/AsyncAPI specification.
4. Flag affected consumers and required compatibility evidence.

**Output format:** Updated spec file in the codebase. Contract change classification in the PR description.

### Code Reviewer
1. Label findings by severity: `blocker`, `high`, `medium`, `low`.
2. Reference exact file paths and line numbers where possible.
3. For each finding, state expected behavior and the risk if unchanged.
4. Prefer minimal corrective guidance over broad rewrites.
5. If no material issue is found, explicitly confirm it.
6. For every bug fix PR: include a triage classification — root cause category, whether a test existed that should have caught it, and whether a follow-up issue is needed.

**Output format:** PR review comment thread. Severity-labeled findings.

### Design Critic
1. Challenge assumptions, not people.
2. Focus on failure modes, edge cases, and cross-module risks.
3. For each concern, state impact and probability.
4. If no material risk is found, explicitly say so — this sign-off is required before the SA locks the decision.

**Output format:** Written critique (issue comment or dedicated doc). Concerns labeled by impact: `critical`, `significant`, `advisory`. Explicit "no material risk" sign-off where applicable.

### Security Reviewer
1. Review authentication and authorization boundaries against the security standard (STD-SEC-001).
2. Check for sensitive data exposure in logs, errors, or responses (STD-SEC-002).
3. Verify fail-fast controls are in place for critical configuration.
4. Label findings: `release-blocking`, `high`, `medium`, `advisory`.

**Output format:** PR review comment thread or security review note. Severity-labeled findings.
