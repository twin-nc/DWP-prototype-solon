---
name: council
description: >
  Four-voice anti-anchoring architecture decision pattern — Architect, Skeptic,
  Pragmatist, Critic each deliberate independently before a verdict is reached.
invocation: /council
inputs:
  - name: decision_question
    required: true
    description: The architecture or design question to resolve
  - name: options
    required: false
    description: Pre-identified options to evaluate (if any)
  - name: constraints
    required: false
    description: Known constraints the decision must respect
outputs:
  - name: council_verdict
    description: Structured verdict with voice positions, convergence outcome, and final recommendation
roles:
  - solution-architect
  - delivery-designer
  - design-critic
---

# Council

## Purpose
Prevents a single agent's anchoring bias from locking in a bad architecture decision. Four independent voices (Architect, Skeptic, Pragmatist, Critic) each deliberate in fresh context before their positions are compared and synthesized. The Solution Architect may not self-review their own proposals.

## When to Use
- Any Class A architecture decision
- When the SA and another stakeholder disagree and neither will concede
- When a proposed design has passed the SA but the team has unresolved concerns
- As the escalation path when the Design Critic and SA cannot converge

## Voices

| Voice | Role | Question They Answer |
|---|---|---|
| **Architect** | Solution Architect | What is the correct technical solution given requirements and constraints? |
| **Skeptic** | Design Critic | What are the failure modes, hidden assumptions, and risks? |
| **Pragmatist** | Backend/Frontend Builder | What can actually be built and delivered by the team in scope? |
| **Critic** | Independent (fresh agent) | What would a neutral expert find wrong with all three other positions? |

## Steps

### 1. Preparation
1. Write the decision question clearly. Include constraints and options if known.
2. Gather relevant inputs: requirements, current architecture, constraints.

### 2. Four Independent Deliberations (fresh context each)
For each voice, open a fresh context and provide:
- The decision question
- Relevant inputs (requirements, constraints)
- The voice's specific question (from the table above)
- **Not:** the other voices' output

Each voice produces: position statement + top 3 supporting reasons + top 2 concerns with their own position.

### 3. Synthesis
1. Compile all four positions.
2. Identify agreements.
3. Identify disagreements — classify as blocking or advisory.
4. Produce a recommended decision that addresses the blocking disagreements.

### 4. Verdict
The Solution Architect issues the final verdict after reviewing all four positions. The verdict must address every blocking disagreement explicitly.

## Output Contract
```
## Council Verdict — <decision question> — <date>

Architect position: <summary>
Skeptic position: <summary>
Pragmatist position: <summary>
Critic position: <summary>

Key agreements: <list>
Blocking disagreements: <list>
Advisory disagreements: <list>

Final decision: <decision>
Rationale: <how it addresses the blocking disagreements>
Open questions: <any unresolved advisory items>
```

## Guardrails
- No voice may see another voice's output before producing its own position.
- The Architect voice cannot be the same session that proposed the design under review.
- The final verdict must explicitly address every blocking disagreement — not just the majority view.
- If three or more voices agree and the Critic is the only dissenter, the Critic's concern must still be addressed (even if ultimately accepted).