---
id: STD-PLAT-001
title: Determinism
status: Approved
owner: Platform Engineering
applies_to: All services, portals, and pipelines
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - platform/date-effective-rules.md
  - integration/error-semantics-and-stability.md
  - platform/evidence-immutability-and-replay.md
last_changed: 2026-04-07
---

## Purpose

Ensure identical inputs under identical context produce identical outputs, including errors.

---

## Determinism Scope

Determinism MUST cover:
- Successful outputs
- Validation failures and error codes
- Rounding and monetary calculations (where applicable)
- Rule selection by effective date, domain context, and applicability keys (where applicable)
- State resolution and amendment logic

---

## Required Deterministic Inputs

Any deterministic computation MUST be a pure function of:
- Request payload (canonicalized)
- Actor identity context (roles/permissions, if relevant to the outcome)
- Domain context (e.g., jurisdiction, effective date, period — if applicable)
- Rule/policy bundle versions (if behavior is rule-driven)
- Contract/schema versions
- System configuration that influences outcomes (explicitly versioned)

If something influences outcomes, it MUST be versioned and included in replay metadata.

---

## Monetary and Date/Time Rules

- All monetary values MUST use a defined scale and rounding mode. These MUST be documented and tested.
- Timezone, business calendar, and "end of period" logic MUST be explicit and consistent.
- Wall-clock time (`now()`) MUST NOT be used as a direct input to deterministic computations — pass time as an explicit parameter.

---

## Canonicalization

- Canonicalize JSON (ordering, whitespace) for hashing.
- Normalize numeric and date formats before hashing or replay.
- Use stable identifiers for entities.

---

## Deterministic Errors

Error envelopes MUST be deterministic:
- Stable `code`
- Stable `http_status`
- Stable `category`
- Stable `retriable` flag
