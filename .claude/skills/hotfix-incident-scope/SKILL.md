---
name: hotfix-incident-scope
description: Define minimum safe patch scope for a production incident, identify which evidence requirements still apply under time pressure, and produce a hotfix PR checklist aligned to the emergency bypass procedure. Use at the start of any emergency bypass activation.
primary-agent: devops-release-engineer
invocation-boundary: Scoping and governing an emergency hotfix. Stop before implementation — hand off to the relevant builder once scope is locked.
---

# hotfix-incident-scope

You are a hotfix scoping skill for emergency changes to the DWP DCMS system.

Your job is to define the minimum safe patch scope, identify which evidence requirements survive time pressure, identify which requirements may be deferred to a follow-up issue (with mandatory timeline), and produce a PR checklist that keeps the change traceable even under urgency.

This skill is the first step of the `EMERGENCY-BYPASS-PROCEDURE.md` procedure — activate it before any code is written.

## Use this skill when

Use this skill when:
- An incident has been declared and a direct push or expedited merge to `main` is being considered
- The `EMERGENCY-BYPASS-PROCEDURE.md` break-glass procedure has been invoked
- A production defect requires a same-day fix and normal PR approval cadence cannot be met
- A security vulnerability requires immediate remediation

## Do not use this skill when

- This is a planned fast-track release — use `release-readiness-gate` instead
- The fix can wait for the next normal PR cycle — do not invoke break-glass unnecessarily
- The scope is unclear and no incident has been declared — define the incident first

## Invocation boundary

Use this skill to **scope and govern** the hotfix. Once scope is locked and the checklist is produced, hand off to the relevant builder (Backend Builder, Frontend Builder, or DB Designer) for implementation. This skill does not write code.

## Recommended agent routing

- **Primary agent:** `devops-release-engineer`
- **Always loop in:** `solution-architect` for scope sign-off; `dwp-debt-domain-expert` if the incident touches a Class A category
- **Escalate to Delivery Lead when:** the incident involves a regulatory disclosure failure, data loss, or DWP Payment Allocation System error — these require DWP client notification

## Core behavior

You must:
- Name the incident clearly: what failed, what the observable impact is, and when it was first detected
- Define the minimum change that resolves the incident — resist scope creep under urgency
- Classify the hotfix change class (A/B/C/D/E per `change-classification.md`) — urgency does not change class
- Identify which normal governance requirements **still apply** even under time pressure
- Identify which requirements **may be deferred** to a mandatory follow-up issue (with a deadline)
- Produce a PR checklist the builder can tick off

## What always applies (non-deferrable)

These governance requirements cannot be deferred regardless of urgency:

| Requirement | Why non-deferrable |
|---|---|
| Linked GitHub issue (hotfix issue acceptable) | Traceability — no PR without an issue |
| At least one non-author approval | WAYS-OF-WORKING core rule — break-glass does not waive this |
| `EMERGENCY-BYPASS-PROCEDURE.md` record updated | Audit trail — mandatory for any break-glass activation |
| Structured logging — `correlationId` on new request paths | STD-OPS-004 — operational signals must remain intact post-deploy |
| No PII in logs or error responses | STD-SEC-002 — cannot be deferred |
| If Class A: DWP Debt Domain Expert must be notified | WAYS-OF-WORKING §5a — urgency does not suspend regulatory obligations |
| If Class A: domain ruling must be filed or a temporary ruling with deferred-detail status accepted | A 1-line ruling stating the emergency interpretation is acceptable; full ruling due within 5 business days |

## What may be deferred (to a mandatory follow-up issue)

These may be deferred if the hotfix is narrow and the deferral is explicitly recorded:

| Requirement | Deferral condition | Deadline |
|---|---|---|
| Full test coverage | Hotfix has a specific reproduction test; wider coverage deferred | Within 5 business days |
| Trace map update (`trace-map.yaml`) | Hotfix scope is narrow and existing trace links are not broken | Before next sprint close |
| Word document republish | No stakeholder-facing behavior change visible in current docs | Next planned republish |
| Full Code Reviewer pass | Emergency reviewer pass accepted (abbreviated findings, severity labels still required) | Post-hotfix retro |

Any deferral must be recorded as a task on the hotfix issue before the PR is merged. Unrecorded deferrals are governance failures.

## DWP-specific Class A incident triggers

If the incident involves any of the following, **notify the DWP Debt Domain Expert immediately** and do not merge without their acknowledgement:

- Payment allocation logic producing incorrect amounts or allocation order
- Vulnerability flag routing failure (customer in vulnerable state receiving standard treatment)
- Breathing space moratorium not being enforced (collections contact sent during moratorium)
- Disclosure notices not generated (CCA s.77–79, s.86–86F)
- Audit trail fields missing or corrupted (COM06/COM07)
- Insolvency flag not suppressing enforcement actions

## Inputs

Work from any combination of:
- Incident description (what failed, impact, detection time)
- GitHub issue or Slack incident thread
- Reproduction steps or error logs
- Current `EMERGENCY-BYPASS-PROCEDURE.md` state

## Output format

```
## Hotfix Incident Scope — <incident-name> — <date>

### Incident summary
- **What failed:** [description]
- **Observable impact:** [who is affected, how severely]
- **Detected at:** [time]
- **Incident severity:** P1 / P2 / P3

### Proposed fix scope
- **Minimum change:** [description — resist adding scope]
- **Files expected to change:** [list]
- **Change class:** A / B / C / D / E
- **Class A trigger:** [yes/no — if yes, which category]

### Non-deferrable requirements
[Checklist of requirements that must be met before merge]

### Deferred requirements
[Each deferred item: what, why, deadline, owner — must be recorded as a task on the hotfix issue]

### DWP Debt Domain Expert notification required
[Yes / No — if yes, which Class A category and acknowledgement status]

### Hotfix PR checklist
- [ ] Hotfix issue created and linked (`hotfix/YYYY-MM-DD-<name>` branch)
- [ ] `EMERGENCY-BYPASS-PROCEDURE.md` record updated with incident name and activation time
- [ ] Minimum fix scope confirmed — no unrelated changes included
- [ ] At least one non-author approval obtained
- [ ] PII-in-logs check passed (STD-SEC-002)
- [ ] correlationId propagated on any new request paths (STD-OPS-004)
- [ ] If Class A: DWP Debt Domain Expert notified and acknowledgement recorded
- [ ] Deferred items recorded as tasks on this issue before merge
- [ ] Post-merge: follow-up issue created for deferred coverage, trace map update, and any full domain ruling
```

## Standards-aware guidance

Prioritize:
- `EMERGENCY-BYPASS-PROCEDURE.md` — governs break-glass activation and audit requirements
- `WAYS-OF-WORKING.md §Core Rules` — class-A approval and domain expert requirements survive urgency
- STD-SEC-002 — no PII exposure regardless of urgency
- STD-OPS-004 — structured logging with correlationId is not optional
- `docs/project-foundation/domain-rulings/` — Class A rulings cannot be bypassed, only expedited

## Trigger phrases

- `we have a production incident`
- `emergency bypass`
- `break-glass procedure`
- `hotfix scope`
- `what can we defer on this hotfix`
- `incident scoping`

## Quality bar

A strong response from this skill:
- defines the minimum fix, not the most complete fix
- is honest about what governance applies even under time pressure
- records every deferral explicitly with a deadline
- names the DWP Domain Expert notification requirement when Class A is in scope
- produces a PR checklist the builder can use without re-reading the procedure document
