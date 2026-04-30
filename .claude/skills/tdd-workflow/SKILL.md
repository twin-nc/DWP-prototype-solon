---
name: tdd-workflow
description: >
  RED-GREEN-REFACTOR TDD cycle with git checkpoint evidence at each phase.
  Produces commit history that proves test-first development.
invocation: /tdd-workflow
inputs:
  - name: ac_reference
    required: true
    description: Acceptance criteria reference (issue URL or AC list)
  - name: test_file_path
    required: false
    description: Path where test file should be created (if new)
outputs:
  - name: tdd_evidence
    description: Three git commits (RED/GREEN/REFACTOR) with test run output at each phase
roles:
  - backend-builder
  - frontend-builder
  - test-builder
---

# TDD Workflow

## Purpose
Enforce test-first development with auditable git evidence. Prevents the common pattern of writing tests after the fact and producing a passing suite that was never actually failing.

## When to Use
- When implementing any new feature or behaviour change
- When fixing a bug (write a failing test that reproduces the bug first)
- When the Test Builder role is active

## Steps

### Phase 1 — RED
1. Read the acceptance criteria.
2. Write tests that describe the expected behaviour. Tests must fail because the implementation does not exist yet.
3. Run the tests. Confirm they fail with the expected failure message (not a compilation error or import error — a semantic failure).
4. **Git checkpoint:** `git commit -m "test(RED): <ac-reference> — failing tests for <feature>"`

### Phase 2 — GREEN
1. Write the minimum implementation required to make the tests pass.
2. Run the tests. Confirm they all pass.
3. Do not add code beyond what is needed to pass the tests.
4. **Git checkpoint:** `git commit -m "feat(GREEN): <ac-reference> — implementation passes tests"`

### Phase 3 — REFACTOR
1. Improve the implementation structure without changing behaviour.
2. Run the tests after every significant change. All tests must remain passing throughout.
3. **Git checkpoint:** `git commit -m "refactor(REFACTOR): <ac-reference> — cleanup, no behaviour change"`

### Phase 4 — Verification
1. Invoke `/verification-loop` to confirm the branch is ready to push.

## Output Contract
Three git commits visible in `git log` with the RED/GREEN/REFACTOR prefixes.
Test output at each phase is preserved in the commit message or a linked CI run.

## Guardrails
- Do not write implementation code before the RED commit exists.
- If tests pass immediately on first run (before any implementation), the tests are wrong — fix them.
- The RED commit must show test failure output, not compilation failure.
- Do not skip the REFACTOR phase — leaving GREEN-only code accumulates tech debt.