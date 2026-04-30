---
id: STD-PLAT-005
title: Immutable Records and Corrections
status: Approved
owner: Domain Platform
applies_to: All services with records that carry legal, financial, or audit significance
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - platform/evidence-immutability-and-replay.md
  - platform/state-resolution-precedence.md
last_changed: 2026-04-07
---

## Purpose

Ensure records with legal, financial, or audit significance are immutable once created, and that corrections are modeled as additive supersession.

> **Scope note:** This standard applies to records whose history must be preserved for audit, legal, or financial replay purposes (e.g., submitted forms, financial transactions, assessment decisions, signed documents). For projects without such records, this standard may not apply — document the decision in a deviation record.

---

## Immutable Record Rule (MUST)

- A record with legal, financial, or audit significance MUST NEVER be edited in-place after creation.
- Any correction MUST be modeled as a new record that supersedes the prior one, with:
  - A reference to the record it supersedes
  - Full context for replay (versions, effective dates, actor identity)
  - A new, stable identifier

---

## Corrections

Corrections MUST:
- Reference the record they correct
- Capture sufficient context for replay
- Create a new authoritative record in history
- Not delete or overwrite the original

"Current state" for a corrected record is computed from history, not stored as the only record.

---

## Backfills and Repairs

- Backfill jobs MUST NOT mutate immutable records.
- Repairs MUST be modeled as:
  - New events or records
  - New derived projections
  - Or explicitly approved data correction procedures that preserve the original as evidence

---

## Projections and Derived Views

Derived read models MAY be updated or rebuilt, but MUST:
- Be reproducible from immutable history
- Include versioning
- Not erase prior projections without a trace

---

## DWP Debt Collection — Mandatory Audit Trail Fields

*This section applies to the DWP Debt Collection project. It implements requirements COM06 and COM07.*

All CRUD audit events (create, read, update, delete) on records of legal, financial, or compliance significance MUST capture the following fields (COM07). These fields are **Restricted** class and MUST be routed to the DWP auditing solution sink — not to general application logs.

| Field | Requirement | Notes |
|---|---|---|
| `guid` / `userId` | COM07 | The acting user's system GUID or user ID |
| `timestamp` | COM07 | ISO 8601 UTC timestamp of the action |
| `action` | COM06 | One of: `CREATE`, `READ`, `UPDATE`, `DELETE` |
| `resourceType` | COM06 | The entity type acted upon (e.g., `ACCOUNT`, `REPAYMENT_PLAN`, `VULNERABILITY_FLAG`) |
| `resourceId` | COM06 | Stable identifier of the affected record |
| `macAddress` | COM07 | Client MAC address where available; may be omitted for server-side automated actions with documented justification |
| `ipAddress` | COM07 | Client IP address |
| `browserType` | COM07 | User-agent string (truncated to type/version — no PII) |
| `sessionId` | COM07 | Session identifier linking to the authenticated session |
| `correlationId` | — | Links to application logs and traces for this operation |

**Near-real-time delivery (MUST):** Audit events MUST be delivered to the DWP auditing solution in near-real-time (COM07). Batch-only audit delivery is non-compliant.

**Automated system actions:** Server-side automated actions (batch jobs, workflow triggers) MUST still produce audit events. Where user-agent fields are not applicable, use the system service identity instead and document the pattern.

**Records in scope for this project:**
- Customer and account records
- Repayment plans and payment events
- Vulnerability flags and circumstance markers
- Write-off records and adjustments
- User access events (login, failed login, role changes)
- Strategy and workflow configuration changes
- All third-party placement records
