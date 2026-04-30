---
name: safety-guard
description: >
  Three operating modes (Careful/Freeze/Guard) for scoping agent permissions
  during high-risk operations to prevent blast radius from agent mistakes.
invocation: /safety-guard
inputs:
  - name: mode
    required: true
    description: "Operating mode: careful | freeze | guard"
  - name: scope
    required: false
    description: File paths or resource types the agent is allowed to touch in this mode
outputs:
  - name: mode_declaration
    description: Explicit statement of what the agent can and cannot do in this mode
roles:
  - backend-builder
  - devops-release-engineer
  - db-designer
---

# Safety Guard

## Purpose
Agents operating without explicit scope constraints will sometimes make edits outside the intended area, particularly when following a chain of reasoning that crosses module boundaries. Safety Guard declares explicit operating modes before high-consequence work to contain the blast radius of mistakes.

## When to Use
- Before any database migration work
- Before any deployment or infrastructure change
- When working in a large codebase with cross-cutting concerns
- When a subagent is given a broad instruction that could touch many files

## Modes

### Careful Mode
**Scope:** Agent may read anything but must confirm before editing any file outside the declared scope.
**Use for:** Feature implementation in a defined module.

```
SAFETY GUARD — CAREFUL MODE
Allowed writes: <list of file paths or patterns>
All other writes: require explicit confirmation before proceeding
Reads: unrestricted
```

### Freeze Mode
**Scope:** Agent may not edit any file. Read-only. Any write attempt is an error.
**Use for:** Code review, research, audit, design critique.

```
SAFETY GUARD — FREEZE MODE
Allowed writes: NONE
All writes: blocked — this is a read-only operation
Reads: unrestricted
```

### Guard Mode
**Scope:** Agent may only touch files explicitly listed. Any other edit is blocked without human approval.
**Use for:** Database migrations, deployment scripts, security-sensitive files.

```
SAFETY GUARD — GUARD MODE
Allowed writes: <explicit list only — no patterns>
All other writes: blocked — requires human approval to expand scope
Reads: unrestricted
```

## Steps
1. Declare the mode and scope before any work begins.
2. If the agent encounters a needed edit outside the declared scope, it must STOP and declare the scope expansion request before proceeding.
3. The scope expansion request must be answered by a human.
4. After the operation completes, explicitly lift the mode.

## Output Contract
```
SAFETY GUARD ACTIVE
Mode: <CAREFUL | FREEZE | GUARD>
Declared scope: <list>
Activated: <timestamp>
Lifted: <timestamp or "still active">
Scope expansions requested: <list or "none">
```

## Guardrails
- Do not silently expand scope — always declare first.
- Freeze mode is absolute — no writes permitted, even for "quick fixes."
- Guard mode scope must be file-specific, not directory-wide, for migrations and deployments.