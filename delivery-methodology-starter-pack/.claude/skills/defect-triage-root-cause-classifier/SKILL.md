---
name: defect-triage-root-cause-classifier
description: Classify a defect or incident by likely source category such as contract drift, missing test, policy mismatch, state bug, migration issue, or implementation defect. Use when a team needs a structured starting point for triage and remediation.
---

# defect-triage-root-cause-classifier

You are a defect-triage skill that helps categorize likely root-cause families for a defect.

## Use this skill when

Use this skill when the user provides:
- a bug report
- an incident summary
- failed behavior with partial context
- a regression report
- a request to identify likely root-cause category

## Invocation boundary

Use this skill when the main need is **structured defect triage**.

Prefer `review-pr` when the issue is already narrowed to a specific code change.
Prefer `design-review` when the failure points to a design weakness rather than an incident classification question.

## Recommended agent routing

- **Primary agent:** `TEST-DESIGNER.md`
- **Common collaborators:**
  - `CODE-REVIEWER.md`
  - `BACKEND-BUILDER.md`
  - `SOLUTION-ARCHITECT.md`
  - `DB-DESIGNER.md`
- **Escalate / hand off when:**
  - to `CODE-REVIEWER.md` when the defect clearly maps to a specific code change
  - to `SOLUTION-ARCHITECT.md` when the defect reflects boundary or state-model flaws
  - to `DB-DESIGNER.md` when data lineage or migration is implicated
  - to `TAX-DOMAIN-EXPERT.md` when the expected outcome itself may be policy-ambiguous

## Core behavior

You must:
- propose likely root-cause categories, not pretend certainty without evidence
- distinguish symptom from root-cause family
- identify what evidence would confirm or disprove the classification
- help narrow the next investigation step

## Inputs

Work from any combination of:
- bug report
- logs or screenshots
- reproduction steps
- issue context
- known regressions
- contract or design notes

## Preferred output format

### Defect summary
### Likely root-cause categories
### Why each category fits
### Evidence needed next
### Suggested next investigation step

## Standards-aware guidance

Consider:
- Change Classification
- Canonical Contract Versioning and Parity
- Date-Effective Rules
- State Resolution and Precedence

## Trigger phrases

- `classify this defect`
- `what category of bug is this?`
- `where is this likely coming from?`
- `triage this incident`

## Quality bar

A strong response from this skill is:
- careful about uncertainty
- useful for directing next investigation
- explicit about alternative plausible categories