# DWP Demo — Solon Tax Gap Analysis

**Date:** 2026-04-28 | **Version:** 1.0

---

## Context

This document maps the DWP Future Debt Service demo requirements against Solon Tax v2.3.0 capabilities. Items are classified as: **Out-of-the-box** (works now), **Config/build** (feasible within the platform), or **Gap** (not supported; workaround or net-new layer build required).

---

## Out-of-the-Box Capabilities

| Capability | Demo relevance |
|---|---|
| BPMN process engine (Camunda-backed) | Drives collection workflows; timer events for grace periods and breach detection |
| Case management with auto-start workflow on case creation | Core segmentation-to-workflow mechanism |
| Human task management with AUTO_ASSIGN and worker eligibility scoring | Automatic routing to Welfare Team, High Value Team, etc. |
| Contact management — letter, SMS, email, dialler with generate/dispatch | Communication steps in all workflows |
| Suppression with `suspendActiveInstancesSW` | "Vulnerability saved → all collection activity stops" |
| Breathing Space enforcement (`maximumNumberDays = 60`) | Statutory Breathing Space — no development needed |
| Payment plan lifecycle including pause (`PUT /pay-plans/{id}/pause`) | Vulnerability and forbearance scenarios |
| Write-off (`POST /overpayment-processes/write-off`) | Residual 1p write-off and Flow 10 |
| Payment distribution/allocation | third-party collection payment reconciliation |
| Candidate lists and risk scoring via Drools | Segmentation and campaign management |
| third-party collection handover (`POST /cases/{id}/debt-handover`) | Initiates third-party collection placement in Flow 3 |
| Operational plan tracking with actual-vs-planned metrics | Dashboard KPI reporting (Flow 6) |
| Keycloak RBAC with role-based validation paths | Role separation across all workspaces |
| Drools stateless rule execution with independently versioned KIE containers | Affordability and segmentation rules |

---

## Configuration and Build Required (Feasible)

All items below are achievable within Solon Tax's supported extension model or the layer architecture. None require changes to the Solon product.

| Item | Effort | Notes |
|---|---|---|
| Reference data and codelists (revenue types, liability types, FT types, candidate list types, suppression types, pay plan types) | 5–8 days | Foundations Configurator; no code |
| Case types linked to BPMN process types (Standard Recovery, Vulnerable Outreach, Low Value Automated, Champion, Challenger) | 1 day | Config only |
| Case worker profiles (Welfare Team, High Value Team, General Collections) with scoring criteria | 0.5 day | Config only |
| BPMN process definitions — Champion, Challenger, Vulnerable, Low Value workflows | 5–10 days | **Largest dev workstream.** Existing IRM BPMN templates provide patterns |
| Demo-safe admin/config screen (editable parameters: wait days, template, queue assignment) | 3–5 days | Configuration Workspace component; not a native Solon screen |
| Inbound customer batch job (Party ID, name, DOB, benefit status, vulnerability fields; validation and reject handling) | 3–5 days | Calls Registration API; code must be written |
| Inbound debt batch job (Debt ID, Party ID, revenue type, amount, due date; failed-record handling) | 3–5 days | Calls Taxpayer Accounting API |
| Real-time create/update API endpoints (Demo 1) | 1–2 days | Thin layer wrapping existing Solon APIs |
| Event/queue demo — trigger endpoint, status screen, downstream re-evaluation wiring | 3–5 days | "Income Updated" → Drools → treatment change |
| Payment callback adapter (inbound payment → match → allocate → audit) | 2–3 days | Uses `PUT /payment-events/{id}/distribute` |
| Drools decision rules — segmentation, affordability, vulnerability scoring | 3–5 days | KIE containers must be authored and deployed |
| Income & expenditure capture UI and affordability calculation model | 5–8 days | Layer-built; Drools runtime and pay plan pause exist |
| Joint debt split logic (50/50, 1p residual write-off) | 3–5 days | Custom BPMN/service on top of write-off API |
| Appointee routing (multi-party contact send) | 1–2 days | `recipientPartyRole` field exists; relationship type config needed |
| Direct debit mandate/schedule and payment monitoring | 2–4 days | May require provider integration |
| Operational report view — payment performance by case type | 2–3 days | Data exists in Solon; dashboard surface is layer-built |
| Strategy simulation and champion/challenger results dashboard | 8–15 days | Layer-owned; Risk Analysis API provides primitives |

---

## Genuine Gaps

Items below are not available out-of-the-box and cannot be resolved by Solon configuration alone.

| # | Gap | Demo impact | Workaround |
|---|---|---|---|
| 1 | **Household as a first-class entity** — Solon supports linked party role relationships; whether a named Household entity exists independently is unconfirmed | Flow 4 fails without a household view | Assemble household view from linked party roles in the layer — feasible but non-trivial build |
| 2 | **Visual strategy editor / no-code configuration surface** — Solon BPMN engine accepts XML only; no business-facing canvas exists | Flow 5 commercial case at risk | Simplify to structured-form screen with editable parameters; explicitly acknowledged as the "demo-safe" scope reduction |
| 3 | **Historical simulation engine** ("what would have happened?") | **Highest commercial risk.** Transforms Flow 5 from a flowchart tool into an optimisation tool | Pre-compute a convincing mock result and present it as an output — weakens impact; team must decide early |
| 4 | **Champion/Challenger assignment, tracking, and comparative reporting** — no C/C concept in Solon natively | Flow 5 / Flow 6 — C/C results view cannot run live | Pre-seed C/C data with meaningful comparative results before demo day |
| 5 | **Benefit status as a structured data field** — not a confirmed named field in Solon Registration API | Segmentation and treatment logic depends on it | Carry in `dataArea`/`additionalInfo`; read via Drools — functional but not a clean native field |
| 6 | **Structured vulnerability entity with clinical richness** — Solon stores the enforcement outcome (suppression) but not the clinical assessment (bureau data, review history, severity, multiple concurrent vulnerabilities) | Vulnerability capture form (Flow 2) and vulnerability exception report (Flow 6) are entirely layer-built | Known design decision (ADR-006); Operational DB in Risk, I&E & Compliance Core Service owns this data |
| 7 | **third-party collection placement transmission and acknowledgement** — `POST /cases/{id}/debt-handover` initiates handover; outbound transmission and acknowledgement receipt are not native | Flow 3 third-party collection placement step | Mock third-party collection endpoint for demo — 1–2 day build |
| 8 | **Real-time operational dashboard** — queue volumes, SLA health, breach trends, arrangement health, C/C results, vulnerability exception depth | Flow 6 is entirely layer-built | Planned scope (ADR-001); no workaround — must be built |
| 9 | **Debt age field** — no explicit "debt age in days" field; must be derived from `effectiveDate` | Segmentation rules depend on age bands | Derive from `effectiveDate` in Drools — technically straightforward; needs testing before demo |
| 10 | **Strategy version history, rollback, and peer review as a UI experience** — Solon supports deploying new BPMN versions; no native comparison or rollback UI | Flow 5 version management story | Simple version list + re-deploy of a prior BPMN definition — functional but not polished |

---

## Risk Summary

| Risk | Level | Decision needed |
|---|---|---|
| Flow 5 simulation engine — real replay vs. pre-computed mock | **HIGH** | Decide before build starts. Mock weakens the commercial "moment of truth" but eliminates 8–15 days of build risk. |
| Household entity model — Solon native vs. layer composition | **HIGH** | Confirm technically before sprint planning. Affects Flow 4 scope significantly. |
| Demo data seeding across all six flows | **HIGH** | Plan and execute before build ends — not at the end. Household relationships, joint liabilities, C/C baseline data, and breach state all need realistic pre-seeded records. |
| Benefit status field — confirm `dataArea` workaround works in Drools rules | **MEDIUM** | Validate early; segmentation rules depend on it. |
| third-party collection placement — mock vs. real third-party collection API | **LOW** | Mock endpoint is sufficient for demo; confirm with DWP expectations. |

---

## Key Decisions Required Before Build

1. Is Solon Tax's Household a first-class entity or a layer composition? (Needs technical confirmation)
2. Real historical simulation engine or pre-computed mock for Flow 5?
3. Benefit status: confirm `dataArea`/`additionalInfo` workaround is Drools-callable
4. third-party collection placement: mock endpoint vs. real third-party collection API integration for demo
5. End-to-end demo data seeding plan — covering all six primary flows — produced before build begins
