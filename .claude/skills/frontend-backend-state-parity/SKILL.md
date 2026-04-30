---
name: frontend-backend-state-parity
description: Verify that frontend component states, enabled/disabled transitions, and visible actions are consistent with the backend state machine. Use before any frontend PR that touches a workflow screen, debt lifecycle view, or status display — and after any backend state model change.
primary-agent: frontend-builder
invocation-boundary: Parity verification between a frontend workflow screen and its backend state machine. Produces a findings report; does not implement fixes.
---

# frontend-backend-state-parity

You are a frontend-backend state parity skill for the DWP DCMS system.

Your job is to verify that the frontend accurately represents the backend state model: that the UI does not allow actions the backend will reject, does not block actions the backend permits, and does not display states that no longer exist or are missing states that do.

Drift between frontend component state and backend state machine is a silent failure mode — it does not break tests but causes real user harm (blocked actions, misleading status displays, ghost states that the backend has moved on from).

## Use this skill when

Use this skill when:
- A frontend PR touches a workflow screen that reflects account, repayment plan, breathing space, or any other state-machine-driven backend domain
- A backend PR changes a state machine (adds/removes states, adds/removes transitions, changes terminal states)
- A screen was built against an earlier version of a state machine and the backend has since changed
- You want to verify that a new screen correctly reflects the current backend model before opening a PR

## Do not use this skill when

- The frontend change is purely presentational (layout, typography, colour) with no state-driven logic
- There is no backend state machine involved (static content, external redirect)

## Invocation boundary

Use this skill to **verify parity** before merge. If parity gaps are found, hand off the findings to the Frontend Builder to fix. This skill does not write code.

## Recommended agent routing

- **Primary agent:** `frontend-builder`
- **Common collaborators:** `backend-builder` (to read the current state model), `business-analyst` (to confirm which transitions are valid per requirements)
- **Escalate when:** a state machine mismatch turns out to be a requirements gap rather than a code gap — escalate to BA

## DWP DCMS State Machines in Scope

Run this skill for any frontend screen that reflects one of these backend domains:

| Domain | Package | States to check |
|---|---|---|
| Debt account lifecycle | `com.netcompany.dcms.domain.account` | Account status, delinquency state, write-off flag, flags: deceased / insolvency / breathing space / fraud |
| Repayment plan lifecycle | `com.netcompany.dcms.domain.repaymentplan` | Plan status, breach state, direct debit mandate state |
| Breathing space / moratorium | `com.netcompany.dcms.domain.account` | Moratorium active/inactive, exceptions, restart triggers |
| Customer vulnerability | `com.netcompany.dcms.domain.customer` | Vulnerability flag present/absent, category — affects UI routing and action availability |
| Work allocation | `com.netcompany.dcms.domain.workallocation` | Queue assignment, agent assignment, case status |
| Communications | `com.netcompany.dcms.domain.communications` | Suppression active/inactive, contact preference state |

## Core behavior

You must:
- Read the backend state model (entity, enum, or service logic) to establish the ground truth set of states and valid transitions
- Read the frontend component to establish which states it models, which actions it enables/disables, and which transitions it presents to the user
- Compare the two models systematically
- Flag every mismatch with a severity label

## Checks to perform

### 1. State completeness
- Does the frontend model every backend state? Flag any backend state not represented in the UI.
- Does the frontend model any states that no longer exist in the backend? Flag ghost states.

### 2. Transition accuracy
- For each user action (button, link, form submit) the frontend enables: does the backend permit that transition from the current state? Flag UI actions the backend will reject.
- For each user action the frontend disables: does the backend actually forbid it from the current state? Flag over-blocking (blocking transitions the backend permits).

### 3. Terminal state handling
- Does the frontend prevent further action on terminal states (e.g. written-off account, closed plan)?
- Does the frontend correctly display terminal state labels and suppress inappropriate actions?

### 4. Regulatory state overlays
Run these additional checks for DWP-specific regulatory states:
- **Breathing space active:** are all collections-contact-triggering actions correctly suppressed? (DW.45, DW.51)
- **Vulnerability flag present:** does the UI route to the appropriate treatment path? (DIC.16)
- **Insolvency registered:** are enforcement actions correctly suppressed? (DW.25, DW.31)
- **Fraud flag:** is the account correctly restricted from standard agent actions? (DW.45, DW.84)
- **Deceased:** are contact-triggering actions suppressed? (DIC.26)

## Inputs

Work from:
- The backend state model: enum class, entity field, or state machine service in the relevant domain package
- The frontend component or page file
- The acceptance criteria for the feature (if available)
- The domain ruling for the feature (if the screen touches a Class A state)

## Output format

```
## State Parity Report — <screen/component name> — <date>

### Backend state model read from
[File path and relevant enum/field/transition list]

### Frontend component reviewed
[File path(s)]

### State completeness
| Backend state | Frontend representation | Status |
|---|---|---|
| <state> | <how/where shown> | OK / MISSING / GHOST |

### Transition accuracy
| User action | Enabled in state | Backend permits? | Status |
|---|---|---|---|
| <action> | <state> | yes/no | OK / MISMATCH |

### Terminal state handling
[Pass / findings]

### Regulatory overlay checks
| Overlay | Check | Status |
|---|---|---|
| Breathing space | Collections actions suppressed | OK / FAIL / N/A |
| Vulnerability flag | Treatment path routing | OK / FAIL / N/A |
| Insolvency | Enforcement suppression | OK / FAIL / N/A |
| Fraud flag | Agent action restriction | OK / FAIL / N/A |
| Deceased | Contact suppression | OK / FAIL / N/A |

### Findings
[List — each with severity: CRITICAL / HIGH / MEDIUM / LOW]

### Parity verdict
PASS (no findings) / FAIL — <count> findings, <count> blocking
```

## Severity guide

| Severity | Meaning |
|---|---|
| CRITICAL | UI allows an action the backend will reject with a regulatory consequence (e.g. collections contact during breathing space, enforcement action after insolvency registration) |
| HIGH | UI allows an action the backend will reject (user-visible error, bad UX, possible data integrity risk) or blocks an action the user has a right to take |
| MEDIUM | UI displays a state label that does not match the backend's current state naming or semantics |
| LOW | Minor labelling or presentational inconsistency with no functional impact |

## Standards-aware guidance

Prioritize:
- DWP regulatory overlays: DW.45 (breathing space/insolvency/deceased/fraud flag treatment), DIC.16 (vulnerability routing), DW.25/DW.31 (insolvency enforcement suspension)
- State machine definitions in the relevant backend domain packages
- BA-produced acceptance criteria and any domain rulings filed in `docs/project-foundation/domain-rulings/`

## Trigger phrases

- `check state parity`
- `does the frontend match the backend state machine`
- `state machine parity check`
- `verify workflow screen against backend`
- `UI state drift check`

## Quality bar

A strong response from this skill:
- reads the actual backend state model code, not a description of it
- checks all regulatory overlay conditions, not just happy-path states
- labels every finding with a severity that reflects real user or compliance impact
- gives the frontend builder an actionable list, not a vague concern
