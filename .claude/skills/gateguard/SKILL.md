---
name: gateguard
description: >
  Forces agents to declare confirmed facts before high-consequence actions,
  reducing hallucination-driven errors on writes, migrations, and commits.
invocation: /gateguard
inputs:
  - name: action_description
    required: true
    description: Plain-language description of the action about to be taken
  - name: standard_reference
    required: false
    description: Name of the relevant standard or requirement to check against
outputs:
  - name: gate_declaration
    description: Structured fact declaration covering current state, change, rollback, and compliance
roles:
  - backend-builder
  - frontend-builder
  - db-designer
  - devops-release-engineer
---

# GateGuard

## Purpose
Forces a structured fact declaration before any high-consequence action (writes, migrations, commits, deployments). Based on the GateGuard pattern, which has shown a +2.25 measured quality improvement by breaking the chain of assumption propagation before it reaches irreversible actions.

## When to Use
- Before writing or editing a database migration file
- Before committing code that touches state transitions or terminal states
- Before any deployment command
- Before editing a file that multiple services depend on
- When the agent is uncertain about the current state of the system

## Inputs
| Input | Required | Description |
|---|---|---|
| action_description | Yes | What action is about to be taken |
| standard_reference | No | Name of the relevant standard (e.g. "immutable-records-and-corrections") |

## Steps

1. **[Agent]** State the confirmed current state of the system/file/resource. Must be read from source, not assumed.
2. **[Agent]** State exactly what this action will change.
3. **[Agent]** State the rollback procedure if this action is wrong.
4. **[Agent]** Name the relevant standard and confirm the action complies with it.
5. **[Agent]** Proceed only after all four items are answered. If any cannot be answered, stop and investigate.

## Output Contract
A structured gate declaration with four sections:
```
CONFIRMED STATE: <what was read from source>
WHAT CHANGES: <exact delta>
ROLLBACK: <how to undo>
STANDARD COMPLIANCE: <standard name> — <pass/defer with reason>
```

## Guardrails
- Do not complete the gate declaration with assumed facts — only confirmed ones.
- If current state cannot be confirmed, stop and read the relevant file or resource before continuing.
- A gate declaration that says "I assume..." in the CONFIRMED STATE section is invalid.

## Example Invocation
```
/gateguard
action_description: Add a NOT NULL column to the filings table
standard_reference: database-migration-standard
```

Expected output:
```
CONFIRMED STATE: filings table has 12 columns; no existing NOT NULL constraint on status_note; migration V23 is the latest applied.
WHAT CHANGES: V24 adds status_note VARCHAR(500) NOT NULL DEFAULT ''; backfill runs before constraint is applied.
ROLLBACK: V24__rollback.sql drops the column; no data loss because default was empty string.
STANDARD COMPLIANCE: database-migration-standard — PASS. Migration is reversible; backfill runs first; column name follows snake_case convention.
```