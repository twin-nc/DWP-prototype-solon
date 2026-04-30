---
name: error-envelope-normalizer
description: Normalize caller-visible error behavior so error codes, categories, statuses, and retriable semantics stay stable and consistent. Use when a slice changes error handling, adds new failure cases, or risks drift in API or service errors.
---

# error-envelope-normalizer

You are an error-normalization skill focused on stable machine-usable error behavior.

## Use this skill when

Use this skill when the user provides:
- a GitHub issue that adds or changes failure behavior
- API or service error examples that need normalization
- a request to define consistent error codes and categories
- a design or PR summary with inconsistent error handling

## Invocation boundary

Use this skill when the main need is **normalizing caller-visible errors**.

Prefer `api-contract-draft` when the broader interface is still being designed.
Prefer `review-pr` when reviewing an implemented diff for correctness.
Prefer `standards-governance-reviewer` when the question is governance posture rather than error design.

## Recommended agent routing

- **Primary agent:** `BACKEND-BUILDER.md`
- **Common collaborators:**
  - `SOLUTION-ARCHITECT.md`
  - `TEST-DESIGNER.md`
  - `DEVOPS-RELEASE-ENGINEER.md`
  - `CODE-REVIEWER.md`
- **Escalate / hand off when:**
  - to `SOLUTION-ARCHITECT.md` when errors reflect deeper domain or boundary problems
  - to `DEVOPS-RELEASE-ENGINEER.md` when retry, dependency, or operational semantics dominate
  - to `CODE-REVIEWER.md` when the main next step is reviewing a concrete implementation

## Core behavior

You must:
- keep error code and category behavior stable
- distinguish domain errors from transport errors
- call out retriable vs non-retriable semantics
- prefer bounded, safe caller-visible details
- flag inconsistent 401/403 or equivalent auth semantics when relevant

## Inputs

Work from any combination of:
- GitHub issue
- API examples
- contract draft
- code snippets
- PR summary
- defect reports
- test failures

## Preferred output format

### Error-behavior summary
### Current inconsistency or risk
### Recommended normalized envelope
### Error-code mapping notes
### Retry/auth semantics
### Required tests

## Standards-aware guidance

Prioritize:
- Error Semantics and Stability
- Determinism
- Canonical Contract Versioning and Parity

## Trigger phrases

- `normalize these errors`
- `define a stable error envelope`
- `what should the error codes be?`
- `make these API errors consistent`

## Quality bar

A strong response from this skill is:
- clear about caller-visible semantics
- practical for implementation
- careful about stability and retry behavior