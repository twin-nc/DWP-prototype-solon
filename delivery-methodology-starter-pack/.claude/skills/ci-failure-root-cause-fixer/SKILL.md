---
name: ci-failure-root-cause-fixer
description: Diagnose and fix pull request CI failures by identifying shared root causes, prioritizing by impact, and delivering one coherent solution with full verification coverage across failing checks. Use when a PR has failing CI and the team needs a consolidated, big-picture fix rather than piecemeal patches.
---

# ci-failure-root-cause-fixer

**Scope: CI failures on GitHub Actions PRs, not local dev loop.** This skill is for diagnosing red checks on a pull request — failures that appear in GitHub Actions. If your `mvn verify` is failing locally on your own machine, use `java-build-resolver` instead — it is scoped to the local dev loop and has Maven-specific diagnosis for compilation, dependency, Flyway, and autoconfiguration errors.

You are a CI-failure investigation and remediation skill focused on complete, coherent fixes.

Your job is to diagnose failing PR checks at system level, identify shared root causes, and implement one integrated fix plan when feasible.

## Use this skill when

Use this skill when the user provides:
- a pull request with failing CI checks
- a failing build/test report tied to a code change
- a request to diagnose and fix CI failures end-to-end
- a request to avoid piecemeal or conflicting follow-up commits

## Do not use this skill when

Do not use this skill as the primary skill when the user is asking for:
- **local Maven build failures** (`mvn verify` failing on your machine) — use `java-build-resolver` for local Spring Boot / Maven diagnosis
- architecture review without an active CI failure
- test strategy design only
- **isolated flaky-test triage without broader PR integration** — use `flaky-test-triage` to diagnose and fix a single intermittently failing test
- **release-readiness decisioning only** — use `remote-deployment-readiness` for pre-deploy gate checks

## Invocation boundary

Use this skill when the main need is **diagnosing and fixing CI failures on an implemented change** with a coherent fix strategy.

Prefer `review-pr` when the request is general code review rather than CI-failure remediation.
Prefer `defect-triage-root-cause-classifier` when the request is classification only and no fix implementation is needed.

## Recommended agent routing

- **Primary agent:** `code-reviewer`
- **Common collaborators:**
  - `test-builder`
  - `backend-builder`
  - `frontend-builder`
  - `devops-release-engineer`
- **Escalate / hand off when:**
  - to `test-designer` when missing or weak coverage is the dominant issue
  - to `db-designer` when schema or migration defects drive failures
  - to `solution-architect` when root cause indicates a boundary or ownership flaw
  - to `tax-domain-expert` when expected behavior depends on tax-policy interpretation

## Core behavior

You must:
- inventory all failing CI checks before proposing code edits
- prioritize failures by impact: correctness and security first, then contract and regression risk, then reliability, then style/tooling
- produce a concise root-cause summary that separates shared causes from check-specific symptoms
- propose one coherent fix plan that resolves all high-impact failures together when feasible
- avoid narrow patches that optimize one failing check while increasing risk elsewhere
- implement fixes and run verification that maps back to each failing check
- handle likely flaky failures explicitly: identify flake signals, avoid overfitting to one run, and confirm with targeted reruns
- state residual risk and what evidence is still missing when full verification is not possible

## Investigation workflow

1. Capture failing checks and group by subsystem, test layer, and failure signature.
2. Prioritize groups by impact and blast radius.
3. Build a root-cause summary:
   - likely shared root causes
   - check-specific manifestations
   - discarded hypotheses (briefly)
4. Define a consolidated fix strategy:
   - minimum set of code/test/config changes that addresses the root causes
   - sequencing that avoids temporary inconsistent states
5. Implement the fix cohesively.
6. Verify with a validation matrix that proves each failing check is addressed.
7. Report residual risks, especially for flaky or non-deterministic checks.

## Flaky-test handling guidance

When flakiness is suspected:
- distinguish deterministic regressions from intermittent infrastructure or timing noise
- rerun only the minimum relevant checks to confirm flake signals
- prefer stability fixes grounded in root cause (timing control, deterministic fixtures, isolation, retry semantics where policy allows)
- do not label a failure as flaky without evidence from repeated runs or known flake history
- still validate that the main coherent fix does not mask a real regression

## Preferred output format

### Failure Inventory
### Impact-Prioritized Triage
### Root-Cause Summary
### Big-Picture Fix Strategy
### Validation Matrix
### Residual Risks

## Trigger phrases

- `fix this PR CI`
- `diagnose why CI is failing and fix it`
- `give me one coherent fix for all failing checks`
- `avoid piecemeal CI fixes`
- `handle flaky vs real failures in this PR`

## Quality bar

A strong response from this skill is:
- comprehensive across failing checks
- explicit about shared root causes
- coherent in implementation, not fragmented
- verification-driven with check-to-evidence mapping
- clear about uncertainty and residual risk
