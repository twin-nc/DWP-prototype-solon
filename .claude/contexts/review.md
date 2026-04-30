# Context Mode: review

**Purpose:** Quality assurance, security review, architecture critique. Use when the primary output is a finding, not code.

## Active Stance
- Read-only by default; do not edit implementation unless explicitly requested.
- Independent context window required for critic and reviewer roles — do not carry reasoning from the generator session.
- Label all findings with severity: CRITICAL / HIGH / MEDIUM / LOW.
- Sign off explicitly when no material findings exist — silence is not approval.

## Model Recommendation
**Opus** for Class A changes, security-sensitive PRs, or when the reviewer is acting as Design Critic.
**Sonnet** for standard code review and test coverage checks.
**Haiku** is not appropriate for review tasks — findings quality is too low.

## Token Budget
- Do not compact mid-review — compaction can lose context needed to identify cross-file issues.
- Compact after review is complete, before handing off to the fix cycle.

## Relevant Agent Roles
code-reviewer *(read-only)*, design-critic *(read-only)*, test-designer

## Relevant Skills
`review-pr`, `design-review`, `santa-method`, `release-readiness-gate`,
`traceability-and-evidence-enforcer`, `standards-governance-reviewer`

## Reminders
- Pre-review requirements: CI passing, branch up to date, conflicts resolved.
- Class A changes require Santa Method dual review (two independent reviewers, convergence loop).
- Design Critic must not review a proposal it was involved in generating.
- Every CRITICAL finding blocks merge; every HIGH finding requires explicit deferral with reason.