---
name: santa-method
description: >
  Dual independent review with convergence loop for Class A changes,
  release evidence packs, and security-sensitive PRs.
invocation: /santa-method
inputs:
  - name: artifact_path
    required: true
    description: Path to the artifact to review (PR diff, evidence pack, design doc)
  - name: rubric
    required: true
    description: Rubric to use — class-a-change | release-evidence | security-review
  - name: reviewer_a_context
    required: false
    description: Any context specific to Reviewer A (leave blank for true independence)
  - name: reviewer_b_context
    required: false
    description: Any context specific to Reviewer B (leave blank for true independence)
outputs:
  - name: review_a
    description: Reviewer A's completed rubric with verdict
  - name: review_b
    description: Reviewer B's completed rubric with verdict
  - name: convergence_verdict
    description: Final verdict after convergence gate evaluation
roles:
  - code-reviewer
  - design-critic
  - test-designer
---

# Santa Method

## Purpose
Two independent reviewers assess the same artifact using the same rubric without seeing each other's output first. Their verdicts are then compared. Agreement is fast-path to approval. Disagreement triggers a focused fix cycle (max 3 iterations). The independence constraint prevents anchoring — the most common failure mode of sequential review.

## When to Use
**Required (not optional) for:**
- Class A changes (architecture impact, cross-module, schema migration, high blast radius)
- Release evidence pack sign-off
- Security-sensitive PRs (auth, PII, secrets handling)

**Optional for:**
- Any change where a single reviewer's confidence is low
- Changes that have caused production incidents in the past

## Steps

### 1. Artifact Preparation
1. Confirm the artifact is complete and ready for review (CI passing, branch up to date, no unresolved conflicts).
2. Select the appropriate rubric from `templates/SANTA-RUBRIC-<type>.md`.
3. Record the artifact path and rubric in this skill invocation.

### 2. Reviewer A — Fresh Context Window
1. Open a fresh Claude Code session (or invoke a subagent with no prior context).
2. Provide: artifact, rubric, acceptance criteria, and linked issue only.
3. Do NOT provide: reasoning from the session that produced the artifact.
4. Reviewer A completes the rubric and produces a verdict.

### 3. Reviewer B — Fresh Context Window (Independent)
1. Open a second fresh Claude Code session, independently of Reviewer A.
2. Provide: same inputs as Reviewer A (artifact, rubric, AC, issue link).
3. Reviewer B completes the rubric and produces a verdict independently.

### 4. Convergence Gate
1. Compare verdicts.
2. Apply the convergence gate rules from the rubric.
3. If both APPROVE: proceed.
4. If divergent: identify the specific disagreements, fix those items, and re-run (max 3 iterations).
5. If any BLOCK from either reviewer: do not proceed; fix the blocking issue.

### 5. Evidence
Record the convergence outcome in the PR description or release evidence pack.

## Output Contract
```
## Santa Method Review — <artifact> — <date>

Rubric: <rubric name>
Reviewer A verdict: APPROVE / REQUEST CHANGES / BLOCK
Reviewer B verdict: APPROVE / REQUEST CHANGES / BLOCK
Disagreements: <list or "none">
Convergence outcome: APPROVED / FIX CYCLE (iteration N of 3) / BLOCKED
```

## Guardrails
- Reviewer A and Reviewer B must never see each other's output before producing their own verdict.
- Do not use the same context window for both reviewers — independence is the point.
- Do not exceed 3 fix iterations; if convergence is not reached, escalate to the Solution Architect.
- A BLOCK from either reviewer cannot be overridden by the other reviewer's APPROVE.