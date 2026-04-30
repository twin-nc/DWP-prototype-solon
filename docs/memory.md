# Project Memory

> **Purpose:** AI session-continuity. Read at the start of every session to restore context.
> This file is not an authority source - it reflects current state but does not override requirements,
> standards, contracts, or code. See `docs/project-foundation/standards/governance/documentation-authority-hierarchy.md`.
>
> **Update triggers (AGENT-RULES rule 18):** named architecture or technology decision, requirements baseline change, work phase or status shift, known constraint or risk change.
> Stale memory must be corrected before closing the issue (DOCUMENTATION-POLICY rule 5).

---

## ⚠ CRITICAL: Platform Pivot in Effect (2026-04-30)

ADR-018 confirmed. DCMS will be built **on top of Solon Tax v2.3.0** as the base platform. This supersedes the previous greenfield direction.

- All prior ADRs (ADR-001 through ADR-017) are **UNDER REVIEW** — their analysis is retained but decisions are not locked.
- ADR-016 is **SUPERSEDED** — its conclusion (reject platform pivot) is overturned; its analysis remains valuable.
- ADR-018 is the authority anchor. Read it before making any architecture decision.
- A new design process is underway. No decisions from the greenfield phase should be treated as current direction.

**Authority documents:**
- `docs/project-foundation/decisions/ADR-018-platform-pivot-solon-tax-confirmed.md` — pivot authority record
- `docs/release/release-1-capabilities.md` — authoritative Release 1 product-design and delivery-scope baseline
- `docs/project-foundation/solon-tax-platform-reference.md` — what Solon Tax provides
- `docs/project-foundation/amplio-process-engine-reference.md` — Amplio constraints (primary process engine candidate)
- `docs/project-foundation/solon-tax-feasibility-analysis.md` — analysis only; conclusion superseded

---

## Repository

- **Remote:** `https://github.com/nctct/DWP-system-prototype`
- **Default branch:** `main`
- **Active branch:** `docs/platform-pivot-solon-tax`
- **CI/CD:** Implemented in `.github/workflows/`

---

## Project Overview

DWP Debt Collection Management System — a debt collection platform for the UK Department for Work and Pensions. The system manages the full debt collection lifecycle: customer and account management, automated workflow and decisioning, repayment plans, multi-channel communications, third-party placements, income and expenditure capture, and management information.

**Platform direction (ADR-018):** Built on top of Solon Tax v2.3.0. Solon Tax is used where it satisfies requirements. A new custom React UI is built on top, styled to resemble Solon Tax's visual language but built fresh (not Solon Tax's own Angular UI). New code fills gaps where Solon Tax does not satisfy requirements.

---

## Current State

- **Phase:** New design process — platform pivot in effect
- **Architecture baseline:** Under review. Solon Tax v2.3.0 is the base platform. All prior greenfield decisions are open for renewed debate.
- **Requirements baseline:** v0.1, sourced from tender documents C8618-FDS-Attachment-4a (Functional) and C8618-FDS-Attachment-4b (Non-Functional)
- **Release 1 scope baseline:** `docs/release/release-1-capabilities.md`
- **Last updated:** 2026-04-30

---

## Tech Stack

**Confirmed (from Solon Tax v2.3.0 base platform):**

| Layer | Technology | Notes |
|---|---|---|
| Base platform | Solon Tax v2.3.0 | Netcompany ERP — revenue management platform |
| Language | Java 17 | Solon Tax ships on Java 17 |
| Framework | Spring Boot microservices | Solon Tax architecture |
| Database | PostgreSQL 14+ or Oracle 19c+ | Solon Tax data stores |
| DB migrations | Liquibase | Solon Tax standard (replaces Flyway from greenfield) |
| Process engine | Amplio (primary) / Camunda 7 (legacy) | Camunda 8 deprecated in 2.3.0 |
| Business rules | Drools KIE | No native DMN — see known constraints |
| Auth | Keycloak 24 (OAuth 2.0 + OIDC) | Solon Tax standard |
| Authorisation | OPA (Open Policy Agent) + Rego | Replaces simple RBAC-via-JWT from greenfield |
| Messaging | Apache Kafka + Outbox Pattern | Solon Tax event bus |
| Frontend | React + TypeScript (new, custom-built) | NOT Solon Tax's Angular UI — new build |
| Design system | GOV.UK Design System | Mandatory (styled to resemble Solon Tax) |
| Container | Kubernetes + Helm 3 | Customer-hosted |
| Observability | ELK Stack (Elasticsearch, Logstash, Kibana, APM) | Solon Tax standard |

**Mandatory integrations (unchanged from requirements):**

| Integration | Standard | Notes |
|---|---|---|
| DWP SSO | OAuth 2.0 (INT01) | 100+ domains (INT28) |
| WorldPay | REST/API (RPF.21, DW.21) | Mandatory payment gateway |
| DWP IVR | API (RPF.27) | Self-service payment |
| DWP Notifications | API (DIC.15, DIC.29) | SMS and email |
| DWP Data Integration Platform / Power BI | API (INT06, MIR.1) | MI and reporting |
| FTPS bulk file exchange | FTPS + TLS 1.2 + PGP (INT07, INT17-INT25) | Files >2GB split |
| DWP Payment Allocation | API (DW.23, DW.87) | Allocation instructions must be followed |
| DM6 (legacy system) | Migration (I3PS.12, I3PS.13) | Migration and back-out |

---

## Key Decisions

| Date | Decision | Rationale | Link |
|---|---|---|---|
| 2026-04-30 | Platform pivot: DCMS built on Solon Tax v2.3.0 | Programme-level decision; confirmed in ADR-018 | `docs/project-foundation/decisions/ADR-018-platform-pivot-solon-tax-confirmed.md` |
| 2026-04-30 | All ADR-001–ADR-017 reopened for debate | Prior decisions were greenfield-first; must be reassessed against Solon Tax base | ADR-018 §Consequences |
| 2026-04-30 | New React UI (not Solon Tax Angular UI) | GOV.UK Design System required; Solon Tax UI is Angular | ADR-018 §Decision |

---

## Known Constraints (Platform Pivot Layer)

### Amplio Process Engine Constraints
- **Non-interrupting message boundary events: NOT supported.** Required for DCMS Debt Respite Scheme (Breathing Space) compliance. This is the primary design gap — must be resolved in architecture.
- **Parallel Gateway: sequential-only.** Any DCMS flow requiring true concurrent branch execution cannot use Amplio Parallel Gateway natively.
- **Multi-Instance Activity: sequential-only.** Parallel multi-instance not available.
- **No native DMN.** Business rules require Drools KIE — non-technical rule authoring (ADR-008 requirement) must be reassessed.

### Platform Constraints
- Java 17 (Solon Tax) vs Java 21 (previous greenfield requirement — ADR-014). Must resolve.
- Liquibase (Solon Tax) vs Flyway (previous greenfield choice — ADR-011). Must resolve.
- OPA + Rego authorisation model replaces simple RBAC-via-JWT; complexity is higher.
- Solon Tax is microservices; previous greenfield was a single monolith. Must resolve.

### Regulatory (Unchanged)
- Consumer Credit Act 1974 — disclosure obligations, CCA-governed collections
- UK GDPR / Data Protection Act 2018 — DPIA required
- FCA Vulnerability Guidance FG21/1 — mandatory vulnerable customer treatment
- Debt Respite Scheme (Breathing Space) — mandatory; conflicts with Amplio constraint above
- UK Insolvency Rules 2016

### Infrastructure (Unchanged)
- All hosting and data storage must be in the UK (OPP01, OPP03)
- DWP Payment Allocation instructions must be followed without local override (DW.23, DW.87)

### Performance and Scale (Unchanged)
- 4,000 concurrent users (SCA)
- 99.9% availability (AVA)
- RTO: 4 hours; RPO: 4 hours (REC)

---

## Critical Requirement Notes

- **DW.23 / DW.87:** Payment allocation must follow DWP Payment Allocation instructions without local override.
- **CAS.10:** Breathing Space (Debt Respite Scheme 2020) — non-interrupting suppression required. Amplio supports interrupting only — this is the primary architecture blocker to resolve.
- **ADR-008:** Non-technical DMN authoring requirement. Drools KIE does not satisfy this as written. Must be resolved.
- **AAD.5:** Full audit history — Class A, legally sensitive.
- **RPF.21 / DW.21:** WorldPay mandatory.
- **COM11a–e:** Governance gates (DDA, security, accessibility, DPIA, Service Assessment) — unchanged.

---

## Class A Change Triggers (domain expert required)

The following areas require DWP Debt Domain Expert consultation before any behavior change is merged:

- Disclosure obligations under CCA 1974
- Vulnerability customer treatment (FCA FG21/1) — CAS.6, WAM.20, AAD.18/19
- Breathing space / debt respite scheme — CAS.10
- Insolvency handling — CAS.12, BSF.12
- Payment allocation logic — DW.23, DW.87
- Audit trail fields required by COM06/COM07 — AAD.5
- Any change to payment processing flows — RPF.21, DW.21
- Agent action controls and RBAC — AAD.4, AAD.11, AAD.13

---

## Open Risks and Blockers

| Risk / Blocker | Owner | Status |
|---|---|---|
| Amplio non-interrupting boundary events absent — Breathing Space compliance path not yet designed | Solution Architect | Open — primary design gap |
| Amplio sequential parallel gateway — concurrent flow requirements need architecture solution | Solution Architect | Open |
| No native DMN — non-technical rule authoring requirement (ADR-008) may not be satisfiable with Drools | Solution Architect | Open |
| Java 17 vs Java 21 (ADR-014 exhaustive switch) — must pick one and lock | Solution Architect | Open |
| Liquibase vs Flyway (ADR-011) — must confirm with Solon Tax base | Solution Architect | Open |
| New design process not yet formally initiated — no new ADRs beyond ADR-018 | Architecture team | Open |
| `docs/project-foundation/remote-environment-spec.md` does not yet exist | DevOps / RE | Open — mandatory before remote deployment |
