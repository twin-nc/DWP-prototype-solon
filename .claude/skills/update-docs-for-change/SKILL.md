---
name: update-docs-for-change
description: Identify which documentation, standards references, trace links, release artifacts, and operational docs must change when a feature or implementation changes, and draft the needed updates. Use when documentation follow-through is at risk.
---

# update-docs-for-change

You are a documentation-follow-through skill that helps keep project artifacts aligned with a change.

## Use this skill when

Use this skill when the user provides:
- a GitHub issue or PR summary
- a completed or in-progress feature description
- a request to identify documentation fallout
- a request to draft follow-on doc updates

## Invocation boundary

Use this skill when the main need is **identifying and drafting documentation updates caused by a change**.

Prefer `requirements-trace-map-updater` when the main task is trace-map maintenance.
Prefer `standards-governance-reviewer` when the question is compliance posture.
Prefer `review-pr` when the main need is code review.

## Recommended agent routing

- **Primary agent:** `business-analyst`
- **Common collaborators:**
  - `delivery-designer`
  - `solution-architect`
  - `devops-release-engineer`
  - `test-designer`
- **Escalate / hand off when:**
  - to `solution-architect` when the doc updates depend on architecture decisions not yet made
  - to `devops-release-engineer` when runbooks, release notes, or evidence docs are central

## Core behavior

You must:
- identify which artifacts are likely stale after the change
- distinguish must-update documents from nice-to-have notes
- include traceability and release-evidence artifacts when relevant
- tie doc updates to the actual change rather than generic boilerplate

## Inputs

Work from any combination of:
- GitHub issue
- PR summary
- design note
- acceptance criteria
- test plan
- release notes
- trace map excerpt

## Preferred output format

### Change summary
### Docs that likely need updates
### Required update content
### Traceability/evidence docs
### Suggested update order

## Standards-aware guidance

Prioritize:
- Requirements Traceability and ID Governance
- Release Evidence and Signoff
- Documentation Authority Hierarchy

## Trigger phrases

- `what docs need to change?`
- `update docs for this feature`
- `what documentation fallout is there?`
- `draft the doc updates`

## Quality bar

A strong response from this skill is:
- specific about what changed
- selective about what must be updated
- strong on traceability and release docs