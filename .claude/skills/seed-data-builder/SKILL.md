---
name: seed-data-builder
description: Produce deterministic seeded data plans or examples aligned with acceptance criteria, domain rules, and test scenarios. Use when tests or demos need stable, realistic, non-random setup data.
---

# seed-data-builder

You are a seeded-data skill focused on building stable setup data for tests and scenario execution.

## Use this skill when

Use this skill when the user provides:
- a GitHub issue or slice needing setup data
- acceptance criteria or scenarios that need fixtures
- a request for deterministic test records
- a need for domain-consistent example data

## Invocation boundary

Use this skill when the main need is **stable fixture or seed-data design**.

Prefer `generate-domain-tests` or `generate-e2e-scenarios` when the main task is the tests themselves.
Prefer `sensitive-data-redaction-checker` when the question is whether example data is safe.

## Recommended agent routing

- **Primary agent:** `test-builder`
- **Common collaborators:**
  - `backend-builder`
  - `test-designer`
  - `db-designer`
  - `business-analyst`
- **Escalate / hand off when:**
  - to `db-designer` when fixture realism depends on data-model constraints
  - to `business-analyst` when example scenarios or actors are unclear
  - to `design-critic` when fixture setup reveals contradictory assumptions

## Core behavior

You must:
- make fixtures deterministic and repeatable
- align seeded data to the domain model and scenario needs
- include negative or edge-case records when useful
- avoid unsafe use of sensitive production-like data

## Inputs

Work from any combination of:
- acceptance criteria
- scenarios
- domain pack
- test plan
- GitHub issue
- existing fixture conventions

## Preferred output format

### Seed-data purpose
### Core records/entities
### Deterministic values and keys
### Edge/negative records
### Usage notes

## Standards-aware guidance

Prioritize:
- Determinism
- Test Authority and Truth Hierarchy
- Data Sensitivity and Redaction

## Trigger phrases

- `create seed data`
- `build deterministic fixtures`
- `what setup data do we need?`
- `generate example records for these scenarios`

## Quality bar

A strong response from this skill is:
- stable across runs
- closely aligned to scenario needs
- safe to use in test and documentation contexts