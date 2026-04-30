---
id: STD-AI-001
title: Agent Responsibility Boundaries
status: Approved
owner: Governance
applies_to: All services, portals, and pipelines
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - governance/release-evidence-and-signoff.md
  - security/data-sensitivity-and-redaction.md
last_changed: 2026-04-07
---

## Purpose

Define the boundaries of AI agent authority, approved tooling, data handling requirements, and mandatory human oversight to allow productive AI-assisted delivery without compromising legal, governance, or quality outcomes.

---

## Approved Tools

Only the following AI tools are approved for use on this project:
- **Claude Code** (Anthropic) — primary AI CLI and agent runtime
- **MCP servers** as configured in `.claude/claude_mcp_config.json` — no others without team agreement

Using unapproved AI tools on project work (including for design, code generation, documentation, or data analysis) requires team agreement and must be documented.

---

## Assistive-Only Rule (MUST)

Agents and AI tooling MAY:
- Draft documentation, specifications, and acceptance criteria
- Propose and generate code changes for human review
- Propose requirement-to-test-to-evidence mappings
- Summarise traces, evidence packs, and project state
- Suggest design options and tradeoff analyses
- Execute build, test, and deployment tasks within a governed workflow

Agents and AI MUST NOT:
- Make legal or policy decisions
- Silently change rule or policy outcomes
- Approve releases, deviations, or exceptions
- Override human governance requirements
- Push to production without a human in the loop (emergency bypass procedure is the only exception, and it requires human acknowledgment)
- Self-approve PRs

---

## Review Requirements

AI-generated code, documentation, and design artifacts carry the **same review bar** as human-authored equivalents. "AI wrote it" is not a justification for lighter review.

- PRs containing AI-generated code must be reviewed by a human who understands the change.
- AI-generated acceptance criteria must be confirmed by the Business Analyst or product owner before use.
- AI-assisted evidence pack artifacts must be clearly labeled as AI-assisted.

---

## Data Handling (MUST)

- Inputs to agents MUST follow the data sensitivity standard (STD-SEC-002).
- **Restricted data** (secrets, signing keys, production credentials, unredacted personal identifiers, full sensitive payloads) MUST NOT be pasted into AI prompts.
- Use anonymised or synthetic examples for analysis and troubleshooting.
- Do not share production environment configurations, connection strings, or infrastructure details in AI prompts unless they are already public.

---

## Scope Limitation

Each agent operates within its declared role scope (see `AGENT-OUTLINES.md`):
- An agent acting outside its role boundary must escalate rather than proceed.
- An agent that discovers a conflict between its instructions and a standard must escalate to the standard's named owner — not silently proceed or silently defer.

---

## Memory and Continuity

- `docs/memory.md` is the primary AI session-continuity document.
- Agents must update it when named decisions are made, architecture changes, or project status shifts.
- Agents must not act on memory that contradicts current file state — read the current file first.
- Stale memory must be corrected, not acted upon.

---

## Approval and Oversight

- Any AI-suggested change to policy, rules, or governance MUST be reviewed and approved by the appropriate human owner before it takes effect.
- AI output used in evidence packs MUST be labeled as AI-assisted and confirmed by the relevant human role.
- Agent tools MUST NOT have access beyond their declared scope (principle of least privilege applies to agents as well as humans).
