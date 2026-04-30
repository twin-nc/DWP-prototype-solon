---
name: cost-aware-llm-pipeline
description: >
  Model tiering strategy for agentic pipelines — assigns Haiku/Sonnet/Opus
  by task class to minimize cost without sacrificing output quality.
invocation: /cost-aware-llm-pipeline
inputs:
  - name: task_description
    required: true
    description: Description of the task or pipeline step to tier
outputs:
  - name: model_recommendation
    description: Recommended model with rationale and cost estimate
roles:
  - solution-architect
  - devops-release-engineer
---

# Cost-Aware LLM Pipeline

## Purpose
Running every task on the most capable model is expensive and unnecessary. Most agentic pipelines have a mix of mechanical tasks (file reads, searches, formatting) and high-reasoning tasks (design, review, debugging). Assigning models by task class typically reduces cost by 60–80% while preserving output quality where it matters.

## Model Tier Table

| Model | Use When | Approximate Cost (relative) |
|---|---|---|
| **Haiku 4.5** | File reads, keyword searches, mechanical transforms, subagent data gathering | 1x |
| **Sonnet 4.6** | Feature implementation, test writing, PR descriptions, standard code review | 5x |
| **Opus 4.6** | Class A architecture decisions, security reviews, complex debugging, council voice | 25x |

## Task Classification

| Task Class | Recommended Model | Notes |
|---|---|---|
| Codebase search / read | Haiku | No reasoning needed — just retrieval |
| Mechanical code transform (rename, format) | Haiku | Deterministic output |
| Standard feature implementation | Sonnet | Default for builder roles |
| Test writing | Sonnet | Haiku acceptable for simple unit tests |
| Code review (standard) | Sonnet | |
| Code review (Class A / security) | Opus | Independence and depth required |
| Architecture design | Sonnet → Opus | Start Sonnet; escalate to Opus if output quality is low |
| Debugging (simple) | Sonnet | |
| Debugging (complex / multi-session) | Opus | |
| Council voice | Opus | Independence + depth required |
| Documentation writing | Sonnet | |
| Subagent (parallel gather) | Haiku | Minimise cost for fan-out tasks |

## Steps
1. Describe the task.
2. Match to the classification table.
3. Start with the recommended model.
4. If output quality is insufficient after 2 attempts, escalate one tier.
5. Record the model used in the session log (cost-tracker hook captures this automatically).

## Output Contract
```
Task: <description>
Task class: <class from table>
Recommended model: <model>
Rationale: <one sentence>
Estimated cost tier: <low / medium / high>
```

## Guardrails
- Do not default every task to Opus — it is not always better for mechanical tasks.
- Do not use Haiku for review, architecture, or debugging tasks — quality is insufficient.
- If unsure, use Sonnet — it is the safe default for most tasks.
- Record model selection in cost-tracker output for retrospective review.