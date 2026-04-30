# Project Memory

> **Purpose:** AI session-continuity. Read at the start of every session to restore context.
> This file is not an authority source - it reflects current state but does not override requirements,
> standards, contracts, or code. See `docs/project-foundation/standards/governance/documentation-authority-hierarchy.md`.
>
> **Update triggers (AGENT-RULES rule 18):** named architecture or technology decision, requirements baseline change, work phase or status shift, known constraint or risk change.
> Stale memory must be corrected before closing the issue (DOCUMENTATION-POLICY rule 5).

---

## Repository

- **Remote:** `https://github.com/nctct/DWP-system-prototype`
- **Default branch:** `main`
- **CI/CD:** Implemented in `.github/workflows/` and still evolving

---

## Project Overview

DWP Debt Collection Management System - a greenfield COTS debt collection platform implementation for the UK Department for Work and Pensions. The system manages the full debt collection lifecycle: customer and account management, automated workflow and decisioning, repayment plans, multi-channel communications (SMS, email, letter, IVR, dialler), third-party placements, income and expenditure capture, and management information. Primary users are DWP collections agents, team leaders, operations managers, and compliance staff (~4,000 concurrent users).

Netcompany designs and delivers the implementation from scratch around a selected COTS platform. The COTS vendor manages platform internals (workflow runtime, decisioning runtime, core UI/data internals), while Netcompany owns integration adapters, configuration governance, and bespoke extension services.

---

## Current State

- **Phase:** Foundation
- **Architecture baseline:** Greenfield COTS implementation (no pre-existing workflow configuration in place)
- **Requirements baseline:** v0.1, sourced from tender documents C8618-FDS-Attachment-4a (Functional) and C8618-FDS-Attachment-4b (Non-Functional)
- **Last updated:** 2026-04-22

---

## Tech Stack

| Layer | Technology | Notes |
|---|---|---|
| COTS platform | Vendor/product TBC | Vendor-managed SaaS core workflow and decisioning runtime |
| Integration adapters | Java/Spring (TBC final profile) | Netcompany-owned APIs, adapters, and extensions |
| Auth | DWP SSO / OAuth 2.0 | Mandatory - INT01 |
| Payment gateway | WorldPay | Mandatory - RPF.21, DW.21 |
| File transfer | FTPS + TLS 1.2 + PGP | Mandatory - INT07, INT17-INT25 |
| Data integration | DWP Data Integration Platform / Power BI | Mandatory - INT06, MIR.1 |
| Container runtime | TBC | Must align with STD-PLAT-009/010 for Netcompany-owned services |
| Secrets | TBC | Must align with DWP Key Vault approach |
| Observability | TBC | Must align with DWP monitoring and alerting strategy (SER16) |
| CI/CD | GitHub Actions | Present, continue hardening |

---

## Key Decisions

| Date | Decision | Rationale | Decision Log link |
|---|---|---|---|
| 2026-04-22 | Architecture baseline clarified as greenfield COTS implementation | Solution is built from scratch, but COTS internals remain vendor-managed after selection | `docs/project-foundation/standards/deviations/DEV-001-cots-scope.md` |
| TBC | COTS vendor and product selection | Confirm workflow/decisioning platform and contract constraints | N/A |
| TBC | Integration adapter stack profile finalization | Confirm exact versions and deployment profile for Netcompany-owned services | N/A |

---

## Active Work

| Item | Owner | Status | Notes |
|---|---|---|---|
| Repo setup - install manifest (issue #1) | tct | Merged | CLAUDE.md, .claude/, .github/, docs/project-foundation/ in place |
| docs/memory.md baseline update | tct | In progress | Greenfield baseline aligned |

---

## Open Risks and Blockers

| Risk / Blocker | Owner | Status |
|---|---|---|
| COTS vendor not yet confirmed and no contracted runtime profile is available | Solution Architect | Open |
| Tech stack decisions for Netcompany-owned adapters and extensions not yet finalized | Solution Architect | Open |
| COTS health endpoint format compatibility with platform runbooks not yet verified | SRE / Platform Engineer | Open - due before first remote deployment |
| COTS log format compatibility with DWP observability not yet verified | SRE / Platform Engineer | Open - due before first remote deployment |
| `docs/project-foundation/remote-environment-spec.md` does not yet exist | DevOps / RE | Open - mandatory before any remote deployment |
| `gh` CLI not installed on developer machine | tct | Open - document `gh` path in CLAUDE.md when resolved |

---

## Known Constraints

### Regulatory
- Consumer Credit Act 1974 - governs credit agreements, disclosure obligations, CCA-governed collections processes
- UK GDPR / Data Protection Act 2018 - data handling, DPIA required before any personal data is processed (COM11d)
- FCA Vulnerability Guidance FG21/1 - mandatory vulnerable customer treatment requirements
- Debt Respite Scheme (Breathing Space) - mandatory breathing space handling
- UK Insolvency Rules 2016 - insolvency treatment requirements
- DWP-specific debt recovery policies

### Infrastructure
- All hosting and data storage must be in the UK - no exceptions (OPP01, OPP03)
- COTS platform internals are vendor-managed; Netcompany standards apply fully to Netcompany-owned adapters, extensions, and integration boundary components
- DWP Payment Allocation system provides payment-to-account allocation instructions - COTS and integration services must follow these instructions and must not derive conflicting allocation logic (DW.23, DW.87)

### Performance and Scale
- 4,000 concurrent users (SCA)
- 99.9% availability target (AVA)
- P1 incident resolution within 2 hours, 24/7 (SER)
- Recovery Time Objective (RTO): 4 hours; Recovery Point Objective (RPO): 4 hours (REC)

### UI and Accessibility
- WCAG 2.x AA compliance mandatory (ACC01-ACC03, COM11c)
- GDS Open Standards
- DWP Digital Design Authority governance

### Governance gates (mandatory - must not be bypassed)
- Digital Design Authority approval (COM11a) - before first production deployment
- Security risk assessment sign-off (COM11b) - before first production deployment
- Accessibility compliance assessment (COM11c) - before any UI goes to UAT
- DPIA (COM11d) - before any personal data is processed
- Service Assessment (COM11e) - before go-live
- Place Code of Connectivity (CoCo) policy (SER17) - before any DWP network connection

---

## Critical Requirement Notes

- **DW.9:** Business rule versioning is managed in COTS tooling and associated governance controls.
- **DW.23 / DW.87:** Payment allocation must follow DWP Payment Allocation instructions without local override.
- **SoR.1:** COTS platform is the system of record for collections data.
- **AAD.5:** Full audit history remains Class A and legally sensitive.
- **RPF.21 / DW.21:** WorldPay integration remains mandatory.

---

## Integration Points

| Integration | Standard | Direction | Notes |
|---|---|---|---|
| DWP SSO | OAuth 2.0 (INT01) | Inbound auth | 100+ domains (INT28); user provisioning via DWP Place (UAAF.1) |
| WorldPay | REST/API (RPF.21, DW.21) | Outbound payment | Removes manual copy-paste; future-dated payment possible (AAD.8) |
| DWP IVR | API (RPF.27) | Bi-directional | Self-service payment via IVR |
| DWP Notifications | API (DIC.15, DIC.29) | Outbound | SMS and email via DWP strategic platform |
| DWP Data Integration Platform / Power BI | API (INT06, MIR.1) | Outbound MI | Management information and reporting |
| FTPS bulk file exchange | FTPS + TLS 1.2 + PGP (INT07, INT17-INT25) | Bi-directional | Files >2GB split with sequential naming; SHA-256 checksums |
| DWP Payment Allocation | API (DW.23, DW.87) | Inbound instructions | COTS and integration services follow allocation instructions and do not derive independent allocation logic |
| DM6 (legacy system) | Migration (I3PS.12, I3PS.13) | Bi-directional | Migration and back-out capability required |
| Credit bureaus (Experian, Equifax, TransUnion) | API (BSF.3) | Inbound data | Should have - not mandatory |
| General ledger | API (I3PS.9, SoR.8/9) | Bi-directional | Must have for financial reconciliation |
| Document scanning | API (I3PS.10) | Inbound | Must have |
| DCAs / legal firms / field agents | Batch or API (AAD.21/22, 3PM) | Bi-directional | Should have for 3PM; could have for real-time DCA |

---

## Class A Change Triggers (domain expert required)

The following areas require DWP Debt Domain Expert consultation before any behavior change is merged:

- Disclosure obligations under CCA 1974
- Vulnerability customer treatment (FCA FG21/1) - CAS.6, WAM.20, AAD.18/19
- Breathing space / debt respite scheme - CAS.10
- Insolvency handling - CAS.12, BSF.12
- Payment allocation logic - DW.23, DW.87
- Audit trail fields required by COM06/COM07 - AAD.5
- Any change to payment processing flows - RPF.21, DW.21
- Agent action controls and RBAC - AAD.4, AAD.11, AAD.13
