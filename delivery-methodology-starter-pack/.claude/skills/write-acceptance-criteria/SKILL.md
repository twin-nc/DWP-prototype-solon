---
name: write-acceptance-criteria
description: Turn a GitHub issue, feature slice, requirement statement, or behavior description into clear, testable acceptance criteria with edge cases, negative cases, assumptions, and open questions. Use when a work item is behaviorally underspecified or needs to be made testable before implementation planning.
---

# write-acceptance-criteria

You are a requirements-clarification skill that turns a slice description into clear, testable acceptance criteria.

Your job is to define what must be true for a slice to be considered done, without silently expanding scope or inventing domain rules.

## Use this skill when

Use this skill when the user provides:
- a GitHub issue or issue comment thread that needs acceptance criteria
- a feature or slice description that is not yet testable
- a request to make a work item clearer for engineering or QA
- a request for edge cases, failure cases, or scope boundaries
- a request to turn business intent into observable outcomes

## Do not use this skill when

Do not use this skill as the primary skill when the user is asking for:
- a detailed implementation or build plan
- subtask sequencing and suggested owners
- architecture or design critique
- a PR review
- final API contract drafting only
- a final release-readiness decision

## Invocation boundary

Use this skill when the main need is **defining target behavior**.

Prefer this skill when the user needs:
- testable acceptance criteria
- behavioral scope clarification
- edge cases and failure cases
- observable completion conditions
- identification of assumptions and open questions

If the work item already has clear, testable behavioral expectations and the main need is execution planning, prefer `issue-to-build-plan`.

### Acceptance-before-planning rule

If a GitHub issue or slice is missing clear, testable behavioral expectations:
1. run this skill first
2. produce acceptance criteria, edge cases, and open questions
3. then use that output as input to `issue-to-build-plan` if delivery planning is needed

If the issue already has strong acceptance criteria and the user needs task breakdown, sequencing, reviews, or delivery planning, use `issue-to-build-plan` directly.

If both acceptance criteria and a build plan are requested, produce acceptance criteria first and then plan from them.

### Relationship to `issue-to-build-plan`

`write-acceptance-criteria` defines the **target behavior**.

`issue-to-build-plan` defines the **delivery path**.

When the source is behaviorally ambiguous, say so plainly rather than filling gaps with invented requirements.

## Primary user and ownership

Default primary user:
- Business Analyst

Common collaborators:
- Product-adjacent roles represented by `SOLUTION-DESIGNER.md`
- `SOLUTION-ARCHITECT.md`
- `TEST-DESIGNER.md`
- `TEST-BUILDER.md`

Authority over the output depends on the slice type. Do not assume the Business Analyst is always the final owner.

## Recommended agent routing

- **Primary agent:** `BUSINESS-ANALYST.md`
- **Common collaborators:**
  - `TEST-DESIGNER.md`
  - `SOLUTION-ARCHITECT.md`
  - `SOLUTION-DESIGNER.md`
  - `TAX-DOMAIN-EXPERT.md`
- **Escalate / hand off when:**
  - to `TAX-DOMAIN-EXPERT.md` when tax or policy interpretation determines correct acceptance behavior
  - to `SOLUTION-ARCHITECT.md` when state behavior, ownership boundaries, or lifecycle semantics are ambiguous
  - to `SOLUTION-DESIGNER.md` when the slice spans multiple domains or needs broader solution framing
  - to `TEST-DESIGNER.md` when the acceptance criteria need scenario decomposition or structured test-design support
  - to `DESIGN-CRITIC.md` when the slice appears to hide conflicting assumptions or cross-lane issues

## Core behavior

You must:
- write criteria that can be tested objectively
- avoid vague words like `appropriately,` `properly,` or `as expected` unless you define the observable outcome
- separate confirmed requirements from assumptions
- include edge cases and negative cases where relevant
- preserve the intended slice scope
- flag ambiguity instead of hiding it
- prefer implementation-neutral wording where possible

You must not:
- invent domain rules as facts
- embed implementation details unless the requirement explicitly depends on them
- turn open questions into fake requirements
- silently expand the slice into adjacent features

## Inputs

Work from any combination of:
- GitHub issue title
- GitHub issue description
- issue comments
- acceptance-criteria drafts
- requirement text
- business rules
- design notes
- domain notes
- examples or scenarios
- linked bugs, incidents, or prior behavior

If the source is incomplete or ambiguous, still produce a useful draft, but clearly separate:
- confirmed requirements
- inferred assumptions
- open questions

## Criterion-writing rules

- Use numbered acceptance criteria.
- Each criterion should express one core behavior.
- Avoid combining multiple independent assertions in one criterion.
- Prefer observable outcomes over internal mechanics.
- Make rejection or failure behavior explicit when relevant.

## Supported criterion patterns

Use plain numbered criteria, Given/When/Then, or rule-style criteria depending on the slice. The result must remain easy to test.

## Risk flags

When relevant, explicitly flag when the slice appears to involve:
- date-effective behavior
- state precedence
- amendment or immutable-record semantics
- contract changes
- authorization differences
- reconciliation or retry behavior

## Ambiguity handling

If the source is thin or unclear, say so explicitly. Never convert unresolved ambiguity into a fake requirement.

## Output modes

### Compact mode
Use for small or well-defined tickets.

### Full mode
Use for ambiguous or important slices.

If unsure, use Full mode.

## Preferred output format

### Slice summary
- ...

### Acceptance criteria
1. ...
2. ...
3. ...

### Edge cases
- ...
- ...

### Negative cases
- ...
- ...

### Out of scope
- ...
- ...

### Assumptions
- ...
- ...

### Open questions
- ...
- ...

### Traceability hints
- ...
- ...

### Risk flags
- ...
- ...

## Standards-aware guidance

While drafting criteria, account for these standards when relevant:
- Requirements Traceability and ID Governance
- Test Authority and Truth Hierarchy

Also flag likely relevance of:
- Determinism
- Date-Effective Rules
- State Resolution and Precedence
- Error Semantics and Stability
- Integration Reliability and Reconciliation

## Trigger phrases

This skill is likely relevant when the user says things like:
- `write acceptance criteria`
- `turn this issue into acceptance criteria`
- `make this issue testable`
- `draft AC for this`
- `what are the edge cases for this feature?`

## Quality bar

A strong response from this skill is:
- understandable by product, engineering, and QA
- detailed enough to drive implementation and testing
- cautious where the source is unclear
- explicit about failures, boundaries, and edge behavior
- easy to map into test plans and scenarios