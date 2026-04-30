---
name: agent-introspection-debugging
description: >
  Four-phase agent failure recovery — Capture, Diagnose, Recover, Report —
  for when an agent produces wrong output, loops, or halts unexpectedly.
invocation: /agent-introspection-debugging
inputs:
  - name: failure_description
    required: true
    description: What the agent did wrong or what unexpected state was reached
  - name: last_successful_state
    required: false
    description: The last known good output or state before the failure
outputs:
  - name: recovery_plan
    description: Root cause, recovery action, and prevention recommendation
roles:
  - backend-builder
  - frontend-builder
  - solution-architect
---

# Agent Introspection Debugging

## Purpose
When an agent produces wrong output or gets stuck, the naive response is to retry with the same prompt. This usually produces the same failure. This skill provides a structured four-phase recovery that identifies root cause before retrying.

## When to Use
- Agent produces incorrect output after 2+ attempts with the same approach
- Agent enters a loop (same tool call repeated)
- Agent halts with a vague error or "I cannot do this"
- Agent's output contradicts something established earlier in the session
- Agent makes a change that breaks a previously passing test

## Phases

### Phase 1 — Capture
Document the failure state precisely:
1. What was the exact prompt or instruction?
2. What was the expected output?
3. What was the actual output?
4. How many attempts were made?
5. What was the last successful state before the failure?

### Phase 2 — Diagnose
Classify the failure:

| Category | Description | Signal |
|---|---|---|
| **Context loss** | Agent forgot earlier decisions | Contradicts earlier output |
| **Assumption propagation** | Agent assumed a fact that is wrong | Output based on unverified state |
| **Scope creep** | Agent edited outside declared scope | Unexpected files changed |
| **Tool failure** | Tool returned unexpected result | Error in tool output |
| **Prompt ambiguity** | Instruction had multiple valid interpretations | Agent chose the wrong one |
| **Capability limit** | Task exceeds current model's reasoning depth | Correct approach but wrong result |

### Phase 3 — Recover
Apply the recovery action for the diagnosed category:

| Category | Recovery Action |
|---|---|
| Context loss | Compact, write key decisions to file, restart with explicit state |
| Assumption propagation | Use GateGuard to force fact confirmation before retry |
| Scope creep | Invoke Safety Guard GUARD mode; revert out-of-scope changes |
| Tool failure | Retry tool; if persistent, check MCP health; fall back to manual |
| Prompt ambiguity | Rewrite the instruction with explicit constraints and examples |
| Capability limit | Escalate model tier (Haiku→Sonnet→Opus) or decompose the task |

### Phase 4 — Report
Document the failure and recovery in the session log or issue comment:
- Root cause category
- Recovery action taken
- Whether a prevention change is needed (hook, guardrail, skill update)
- Raise a follow-up issue if the same failure is likely to recur

## Output Contract
```
## Agent Introspection Report

Failure: <description>
Attempts before escalating: <N>
Root cause category: <category>
Evidence: <what led to this classification>
Recovery action: <what was done>
Result: <resolved / partially resolved / escalated>
Prevention: <hook, guardrail, or skill update needed, or "none">
Follow-up issue: <link or "none">
```

## Guardrails
- Do not retry the same approach more than twice without changing something.
- Do not escalate to Opus without first diagnosing root cause — it may not solve a scope or assumption problem.
- Do not discard session context without writing key decisions to a file first.