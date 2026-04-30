---
name: verification-loop
description: >
  Six-phase pre-push verification sequence ensuring code quality, tests, and
  documentation are complete before any branch is pushed.
invocation: /verification-loop
inputs:
  - name: branch_name
    required: false
    description: Branch to verify (defaults to current branch)
outputs:
  - name: verification_report
    description: Pass/fail status for each phase with blocking issues listed
roles:
  - backend-builder
  - frontend-builder
  - test-builder
---

# Verification Loop

## Purpose
A six-phase pre-push checklist that catches the most common causes of CI failure and rework before code leaves the local environment. Replaces ad-hoc "I think it's ready" checks with a repeatable, evidence-producing sequence.

## When to Use
- Before pushing any branch for PR review
- After completing a feature implementation
- Before marking an issue as ready for review
- As the final step of the `tdd-workflow` skill

## Steps

### Phase 1 — Tests Pass
1. Run the full test suite locally.
2. Confirm zero failures and zero errors.
3. Record the command and result.

### Phase 2 — Build is Clean
1. Run the project build (compile + package).
2. Confirm zero build errors and zero warnings that would fail CI.
3. Record the command and result.

### Phase 3 — Linter/Formatter
1. Run the project linter and formatter check.
2. Fix any issues or confirm they are pre-existing and accepted.
3. Record the command and result.

### Phase 4 — Coverage Check
1. Confirm test coverage has not regressed from the pre-change baseline.
2. If coverage tooling is configured, produce a coverage summary.
3. Flag any new code paths with zero coverage.

### Phase 5 — Documentation Impact
1. Classify the change: `no doc impact` / `md update required` / `docx republish required` / `both required`.
2. If MD update required: confirm the relevant docs are updated.
3. If DOCX republish required: raise or reference the deferral agreement.

### Phase 6 — Branch Up to Date
1. Fetch from origin.
2. Confirm branch is not behind `main`.
3. If behind: rebase, re-run phases 1–4, then proceed.

## Output Contract
```
## Verification Report — <branch-name> — <date>

Phase 1 Tests:      PASS / FAIL — <command> — <summary>
Phase 2 Build:      PASS / FAIL — <command> — <summary>
Phase 3 Lint:       PASS / FAIL — <command> — <summary>
Phase 4 Coverage:   PASS / SKIP — <baseline> → <current>
Phase 5 Docs:       <classification> — <action taken or deferred>
Phase 6 Branch:     UP TO DATE / REBASED — <git status summary>

OVERALL: READY TO PUSH / BLOCKED (list blocking phases)
```

## Guardrails
- Do not push if Phase 1, 2, or 6 is FAIL.
- Do not skip Phase 6 — stale branches cause rebase conflicts for reviewers.
- Do not mark documentation as "no doc impact" for behaviour-changing features without confirming against the change classification standard.