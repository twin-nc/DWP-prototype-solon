---
name: strategic-compact
description: >
  Decision framework for when to run /compact vs. when to preserve context,
  preventing premature compaction that loses mid-implementation state.
invocation: /strategic-compact
inputs: []
outputs:
  - name: compact_recommendation
    description: COMPACT NOW / HOLD / CLEAR — with rationale
roles:
  - backend-builder
  - frontend-builder
  - solution-architect
---

# Strategic Compact

## Purpose
/compact frees context window space but destroys fine-grained reasoning chains. Running it at the wrong time (mid-implementation, mid-debug) can cause the agent to lose the thread and produce incorrect continuations. This skill provides a decision table for when compaction is safe vs. harmful.

## When to Use
- When the context window is getting full and you are unsure whether to /compact
- When switching from one major task phase to another
- When a subagent asks whether to compact before continuing

## Decision Table

| Current Phase | Transition To | Recommendation | Rationale |
|---|---|---|---|
| Research complete | Planning | **COMPACT** | Research context is summarised in docs; planning needs clean state |
| Planning complete | Implementation | **COMPACT** | Plan is written down; implementation needs space |
| Implementation complete | Testing | **COMPACT** | Code is committed; test authoring needs clean state |
| After failed approach | New approach | **COMPACT** | Failed reasoning chain should not pollute new attempt |
| After debugging session | Continuing same debug | **HOLD** | Compaction loses the failure context needed to continue |
| Mid-implementation | Continuing same feature | **HOLD** | Code decisions made earlier in session are needed for consistency |
| Switching to unrelated task | New task | **CLEAR** | Start a fresh session rather than compact — contamination risk |
| After long research with no output yet | Continuing research | **HOLD** | Summary not yet written; compaction will lose findings |

## Steps
1. Identify the current phase from the table above.
2. Identify the transition target.
3. Apply the recommendation.
4. If COMPACT: run `/compact` and confirm the post-compact summary captures key decisions.
5. If CLEAR: end the session and start fresh with only the relevant inputs.

## Output Contract
```
Current phase: <phase>
Transition target: <target>
Recommendation: COMPACT NOW / HOLD / CLEAR
Rationale: <one sentence>
```

## Guardrails
- Do not compact mid-implementation unless the current implementation task is fully committed.
- Do not compact mid-debug unless the debugging session has produced a written hypothesis.
- Always write down key decisions to a file or issue comment before compacting — the summary produced by /compact is lossy.