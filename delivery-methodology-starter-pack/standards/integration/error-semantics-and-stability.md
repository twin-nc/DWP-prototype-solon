---
id: STD-INT-001
title: Error Semantics and Stability
status: Approved
owner: Integration Platform
applies_to: All services, portals, and pipelines
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - integration/integration-reliability-and-reconciliation.md
  - security/security-boundaries-and-fail-fast-controls.md
  - platform/determinism.md
last_changed: 2026-04-07
---

## Purpose

Provide stable, machine-usable error responses that support retries, reconciliation, and audit.

---

## Error Envelope (MUST)

All APIs MUST return a stable error envelope:

```json
{
  "code": "PROJ-API-0001",
  "message": "Human-readable message safe for end users",
  "http_status": 400,
  "category": "validation",
  "retriable": false,
  "correlation_id": "uuid"
}
```

Fields:
- `code` — stable string identifier from the project error code catalog
- `message` — human-readable, safe for display to end users; no internal detail
- `http_status` — matches the HTTP response status code
- `category` — one of: `validation`, `authentication`, `authorization`, `not_found`, `conflict`, `dependency`, `rate_limit`, `internal`, `reconciliation_required`
- `retriable` — boolean; true if the caller can safely retry without side effects
- `correlation_id` — stable identifier linking to logs, traces, and evidence
- `details` — optional bounded schema for structured additional context; MUST NOT contain secrets or sensitive data

---

## 401 vs 403 (MUST)

- **401 Unauthorized:** authentication is missing, invalid, or expired.
- **403 Forbidden:** authenticated but not authorized for this resource or action.

These MUST NOT be confused. Using the wrong code misleads clients and breaks retry logic.

---

## Stability Rules

- `code` MUST be stable across versions unless explicitly deprecated.
- Changes to error codes are contract changes and must be classified, versioned, and tested.
- Do not leak secrets, stack traces, or sensitive internal details in `message` or `details`.
- Error envelopes MUST be deterministic — same input produces same error code and category.

---

## Retry Semantics

- Errors MUST declare `retriable` vs non-retriable.
- For dependency errors, include a stable dependency identifier in `details` where safe.
- Retriable errors: `dependency`, `rate_limit`, `internal` (transient)
- Non-retriable errors: `validation`, `authorization`, `conflict`, `not_found`
