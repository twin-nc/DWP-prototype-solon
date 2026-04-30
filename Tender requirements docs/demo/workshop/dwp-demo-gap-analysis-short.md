# DWP Demo — Gap Analysis (Short)

**Date:** 2026-04-28

---

## Genuine Gaps at a Glance

| # | Gap | Workaround |
|---|---|---|
| 1 | **Household as a first-class entity** — unconfirmed in Solon | Assemble from linked party roles in the layer |
| 2 | **Visual strategy editor** — no business-facing canvas in Solon | Structured-form screen with editable parameters |
| 3 | **Historical simulation engine** — no equivalent in Solon | Pre-compute a mock result *(commercial impact: high)* |
| 4 | **Champion/Challenger tracking and reporting** — no C/C concept in Solon | Pre-seed comparative data before demo day |
| 5 | **Benefit status as a structured field** — not a named Solon field | Carry in `dataArea`; read via Drools rules |
| 6 | **Structured vulnerability entity** — Solon stores the suppression, not the clinical record | Layer-owned (Operational DB, ADR-006) |
| 7 | **third-party collection placement transmission + acknowledgement** | Mock third-party collection endpoint — 1–2 day build |
| 8 | **Real-time operational dashboard** (Flow 6) | Must be built in the layer — planned scope (ADR-001) |
| 9 | **Debt age field** — must be derived from `effectiveDate` | Derive in Drools; validate early |
| 10 | **Strategy version history + rollback UI** | Simple version list + re-deploy of prior BPMN definition |

---

## What Solon Provides Out-of-the-Box

- BPMN process engine with timer-boundary events and sub-processes
- Case management auto-starting workflows on case creation
- Human task AUTO_ASSIGN with worker eligibility scoring
- Contact management (letter, SMS, email, dialler) with generate/dispatch
- Suppression with automatic process suspension (`suspendActiveInstancesSW`)
- Breathing Space enforcement (60-day maximum, statutory)
- Payment plan lifecycle including pause; write-off; payment allocation
- third-party collection handover initiation
- Candidate lists, risk scoring via Drools
- Operational plan tracking with actual-vs-planned metrics
- Keycloak RBAC; Drools stateless rule execution

---

## Three Decisions That Must Be Made Before Build

1. **Household** — is it a Solon entity or a layer composition? (Confirm technically)
2. **Simulation engine** — real historical replay or pre-computed mock for Flow 5?
3. **Demo data seeding** — produce an end-to-end seeding plan covering all six flows before build ends
