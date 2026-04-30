---
name: continuous-agent-loop
description: >
  Loop selection decision tree and failure recovery for long-running or
  multi-step agentic tasks that must complete without human intervention.
invocation: /continuous-agent-loop
inputs:
  - name: task_description
    required: true
    description: The multi-step task to complete
  - name: max_iterations
    required: false
    description: Maximum loop iterations before pausing for human review (default 5)
  - name: success_criteria
    required: true
    description: Explicit pass criteria — how the agent knows the task is complete
outputs:
  - name: loop_result
    description: Final state, iterations used, and any human escalations triggered
roles:
  - backend-builder
  - frontend-builder
  - devops-release-engineer
---

# Continuous Agent Loop

## Purpose
Multi-step tasks (fix CI, implement feature, run migration) benefit from a structured loop that checks success criteria at each iteration and has defined exit conditions. Without this, agents either loop indefinitely or stop too early. This skill provides the loop structure, decision tree, and failure recovery.

## When to Use
- Any task with more than 3 sequential dependent steps
- CI fix cycles (run CI → diagnose failure → fix → repeat)
- TDD cycles where multiple test failures need sequential resolution
- Any task where "done" is defined by an observable state (tests pass, CI green, etc.)

## Loop Decision Tree

```
START
  → Execute step
  → Check success criteria
    → PASS: task complete — exit loop
    → FAIL: diagnose failure
      → Is this the same failure as last iteration?
        → YES: apply agent-introspection-debugging
        → NO: apply fix, increment iteration
      → Iteration > max_iterations?
        → YES: pause — request human review
        → NO: continue loop
```

## Steps

### Pre-Loop Setup
1. Define success criteria explicitly (observable, not subjective).
2. Set max_iterations (default: 5 for most tasks; 10 for CI fix cycles).
3. Declare the initial state.

### Each Iteration
1. Execute the next step.
2. Check success criteria.
3. If PASS: produce loop result output and exit.
4. If FAIL: classify the failure (same-as-previous vs. new).
5. If same-as-previous: invoke `/agent-introspection-debugging` before retrying.
6. If new failure: apply fix, record what changed, increment iteration.
7. If max_iterations reached: pause and produce an escalation summary for human review.

### Post-Loop
1. Record final state and iterations used.
2. If escalated, include what was tried and what is needed from the human.

## Output Contract
```
## Continuous Loop Result — <task> — <date>

Success criteria: <list>
Max iterations: <N>

Iterations:
  1. <action> → <result>
  2. <action> → <result>
  ...

Final state: COMPLETE / ESCALATED
Iterations used: <N>
Exit reason: <success criteria met / max iterations / human escalation>
Escalation summary (if applicable): <what was tried, what is needed>
```

## Guardrails
- Do not run without explicit success criteria — "looks good" is not a success criterion.
- Do not run more than max_iterations without pausing for human review.
- If the same failure repeats twice in a row, invoke agent-introspection-debugging before retrying — do not brute-force.
- Do not use this skill for tasks with irreversible steps without confirming each step with a human first.