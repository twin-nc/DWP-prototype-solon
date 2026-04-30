---
name: context-budget
description: >
  Audits the active MCP server count and total tool count against budget limits
  to prevent context window bloat that degrades agent quality.
invocation: /context-budget
inputs: []
outputs:
  - name: budget_report
    description: MCP server count, tool count, and pass/fail against limits
roles:
  - solution-architect
  - devops-release-engineer
---

# Context Budget

## Purpose
Each MCP server and tool consumes context window space at session start. Too many tools degrade reasoning quality and increase cost. The context budget audit ensures the toolset stays within the limits proven to preserve output quality.

## Budget Limits
| Resource | Limit | Rationale |
|---|---|---|
| MCP servers | ≤ 10 | Each server registers multiple tools; >10 servers routinely exceeds 80 total tools |
| Total tools (all sources) | ≤ 80 | Empirical quality degradation threshold for Claude models |

## When to Use
- Before adding a new MCP server to `claude_mcp_config.json`
- When agent output quality has unexpectedly degraded
- As part of the REPO-SETUP-CHECKLIST Section 9 validation
- During periodic toolset review (recommended: each sprint)

## Steps

1. **Count MCP servers** — read `claude_mcp_config.json` and count configured servers.
2. **Count total tools** — for each server, count the tools it registers (or estimate from docs).
3. **Add built-in tools** — count the always-present Claude Code tools (Read, Write, Edit, Bash, Glob, Grep, Agent, etc.).
4. **Compare against limits** — flag any limit breach.
5. **Produce recommendations** — for each breach, identify which server or tool can be removed or deferred.

## Output Contract
```
## Context Budget Audit — <date>

MCP Servers: <N> / 10 — PASS / BREACH
Total Tools (estimated): <N> / 80 — PASS / BREACH

MCP Servers registered:
  - <server-name>: <tool count estimate>
  ...

Recommendations:
  - <action if breach, or "within budget">
```

## Guardrails
- Do not add a new MCP server without running this audit first.
- If the audit shows a breach, do not proceed with the addition until a server is removed or deferred.
- Estimates are acceptable for tool counts — exact counts require calling each server's list endpoint.