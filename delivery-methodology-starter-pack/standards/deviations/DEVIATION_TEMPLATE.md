# Deviation Record Template

**Instructions:** Copy this file, fill in all fields, and save to `docs/project-foundation/standards/deviations/DEV-<NNN>-<short-name>.md`. Raise as a PR. The deviation is not valid until approved signoffs are in place.

---

## Deviation ID
`DEV-<NNN>`

## Title
`<Short descriptive title>`

## Date Opened
`YYYY-MM-DD`

## Owner
`<Name or role>`

## Status
`Proposed` / `Approved` / `Closed` / `Overdue`

## Expiry / Review Date
`YYYY-MM-DD` — Deviations MUST have an expiry date or a scheduled review date. An open-ended deviation requires explicit justification.

---

## Standard(s) Deviated From
- `STD-XXX-NNN — <Standard title>`

## Scope
Which services, pipelines, environments, or data flows does this deviation apply to?

## Requirement(s) Impacted
- `FR-XXX-NNN — <brief description>`

---

## Rationale
*Why is compliance with the standard not currently possible or practical?*

## Risk Assessment
*What risks does this deviation introduce? Consider: data integrity, audit trail, security, legal exposure, operational stability.*

**Risk level:** `Low` / `Medium` / `High` / `Critical`

## Compensating Controls
*What mitigating actions are in place to reduce the risk of the deviation?*

---

## Remediation Plan
*How and when will full compliance be restored? Include specific steps and dates.*

| Step | Owner | Target Date |
|---|---|---|
| | | |

---

## Release Impact
`Release-blocking` — this deviation must be resolved before production promotion
`Deferrable` — this deviation is acceptable for production with the compensating controls above

---

## Signoffs

| Role | Name | Date | Notes |
|---|---|---|---|
| Standard Owner | | | |
| Delivery Lead | | | |
| Security Owner (if security standard) | | | |
| Domain Owner (if Class A) | | | |

---

## Evidence Pack Reference
This deviation MUST appear in the evidence pack for any release made while it is open.
`Evidence pack reference: <leave blank until release>`

---

## Example (delete before use)

> **DEV-001 — Flyway validate-on-migrate disabled for dev01 recovery**
>
> Standard deviated from: STD-PLAT-006 (Database Migration Standard — validate-on-migrate must be enabled)
>
> Rationale: WIP migration renumbering was applied to dev01 before merge, causing checksum failures. Temporarily disabling validation allows the service to start while the schema is corrected.
>
> Risk: LOW — dev01 only; no production impact. Corrective migration is idempotent.
>
> Compensating control: Idempotent V40 migration corrects the schema state; validation re-enabled in the same release.
>
> Expiry: Same sprint — resolved by PR #870.
>
> Signoffs: Delivery Lead (informal approval, logged in PR #870 comments).
