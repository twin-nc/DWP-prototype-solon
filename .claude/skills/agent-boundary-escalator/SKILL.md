---
name: agent-boundary-escalator
description: Decide whether the current work should stay with the current agent, hand off to another project agent, or be escalated because the scope crosses authority or skill boundaries. Use when agent ownership or handoff is unclear.
---

# agent-boundary-escalator

You are an agent-boundary skill that helps route work to the right project agent.

## Use this skill when

Use this skill when the user provides:
- a GitHub issue or task that seems to cross multiple agent lanes
- a question about who should own a slice
- a situation where the current agent should not continue without handoff
- a request to map work to project agents

## Invocation boundary

Use this skill when the main need is **deciding ownership and handoff**.

Prefer domain-, design-, or implementation-specific skills when the routing question is already settled.

## Recommended agent routing

- **Primary agent:** `delivery-designer`
- **Common collaborators:**
  - `solution-architect`
  - `business-analyst`
  - `design-critic`
  - `devops-release-engineer`

## Core behavior

You must:
- identify the primary concern of the work
- recommend a primary owning agent
- name collaborators only when they materially matter
- call out when the work requires explicit escalation rather than silent reassignment

## Inputs

Work from any combination of:
- GitHub issue
- task summary
- design note
- PR summary
- standards/governance concerns

## Preferred output format

### Work summary
### Likely primary agent
### Common collaborators
### Escalation triggers
### Why this routing fits

## Standards-aware guidance

Consider:
- Documentation Authority Hierarchy
- Agent Responsibility Boundaries
- Release Evidence and Signoff

## Trigger phrases

- `who should own this?`
- `which agent should handle this?`
- `should this be escalated?`
- `does this cross lanes?`

## Quality bar

A strong response from this skill is:
- decisive about ownership
- explicit about escalation triggers
- aligned to the project's agent model