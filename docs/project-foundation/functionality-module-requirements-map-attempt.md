# Functionality, Module, and Tender Requirement Map

**Date:** 2026-04-27
**Status:** Current design-level map; granular trace baseline still incomplete
**Primary trace source:** `docs/project-foundation/trace-map.yaml`

## Purpose

This document provides a human-readable cross-reference between:

- modules included in the current DCMS design
- functionality slices included in the current DCMS design
- tender functional and non-functional requirement families
- functionality that exists in the design but is not explicitly requested as a named tender requirement

It does not replace `trace-map.yaml`. Requirement-to-functionality links that are already present in `trace-map.yaml` are treated as confirmed or proposed according to that file. Broader family-level mappings in this document are planning links and must be promoted into `trace-map.yaml` one delivery slice at a time.

## Source Hierarchy Used

1. Tender baseline:
   - `Tender requirements docs/Functional-Requirements-Consolidated.md`
   - `Tender requirements docs/Non-Functional-Requirements-Consolidated.md`
2. Canonical trace:
   - `docs/project-foundation/trace-map.yaml`
3. Architecture and module ownership:
   - `docs/project-foundation/MASTER-DESIGN-DOCUMENT.md`
   - `docs/project-foundation/ARCHITECTURE-BLUEPRINT.md`
   - `docs/project-foundation/functional-requirements-module-map.md`
   - `docs/project-foundation/requirements-functionality-traceability-system.md`
   - `docs/project-foundation/self-service-integration-design.md`
4. Detailed design packs and rulings:
   - `docs/project-foundation/domain-packs/`
   - `docs/project-foundation/domain-rulings/`
   - `docs/project-foundation/decisions/`
   - `docs/project-foundation/architecture-decisions.md`

Important: `development-plan.md` section 14 is explicitly flagged in `trace-map.yaml` as having mismatched requirement IDs. It must not be used as an authority for requirement-to-functionality links until the baseline mismatch is resolved.

## Current Coverage Snapshot

| Item | Current finding |
|---|---:|
| Functional baseline IDs identified in module map | 442 |
| Functional requirement entries currently present in `trace-map.yaml` | 438 |
| Missing functional IDs from `trace-map.yaml` | `DIC.23`, `DIC.35`, `DW.23`, `DW.36` |
| Functionality slices in `trace-map.yaml` | 22 |
| Requirement entries with non-empty functionality mappings | 13 |
| Requirement entries still marked as bootstrap gaps | 407 |
| Requirement entries with linked test suite IDs | 0 |
| Requirement entries with evidence pack IDs | 0 |
| NFR baseline IDs seeded into canonical trace map | 0 |

## Module Catalogue

### Backend Domain Modules

| Module ID | Module | Design responsibility | Repository status | Main tender coverage |
|---|---|---|---|---|
| `MOD-BE-CUSTOMER` | `domain/customer` | Customer identity, profile, circumstances, vulnerability markers, relationships, household/joint-party facts. | Scaffolded | `CAS`, `DIC`, `IEC`, `CC`, `UI` |
| `MOD-BE-ACCOUNT` | `domain/account` | Debt account facts, balances, ledger-facing facts, regulatory facts, account/person recovery relationship. | Scaffolded | `CAS`, `DIC`, `SoR`, `RPF`, `DW` |
| `MOD-BE-STRATEGY` | `domain/strategy` | Treatment routing, segmentation, strategy execution, champion/challenger assignment, affordability decisioning. | Scaffolded | `DW`, `BSF`, `A`, `CAS`, `IEC` |
| `MOD-BE-ANALYTICS` | `domain/analytics` | Strategy performance projections, scoring, segmentation analytics, bureau/scorecard analytics. | Scaffolded | `A`, `BSF`, `MIR`, `DW` |
| `MOD-BE-WORKALLOCATION` | `domain/workallocation` | Queues, worklists, prioritisation, assignment, monitoring, supervisor visibility. | Scaffolded | `WAM`, `DW`, `AAD`, `UAAF` |
| `MOD-BE-REPAYMENTPLAN` | `domain/repaymentplan` | I&E capture, affordability-linked arrangement creation, schedules, breach and review lifecycle. | Scaffolded | `RPF`, `IEC`, `DW`, `UI` |
| `MOD-BE-PAYMENT` | `domain/payment` | Payment posting, allocation, financial event tracking, pending-payment and reconciliation hooks. | Scaffolded | `RPF`, `SoR`, `DIC`, `3PM` |
| `MOD-BE-COMMS` | `domain/communications` | Contact history, correspondence state, templates, channel events, suppression rules. | Scaffolded | `CC`, `DIC`, `AAD`, `DW` |
| `MOD-BE-INTEGRATION` | `domain/integration` | Inbound/outbound APIs, batch interfaces, anti-corruption layer, candidate ingest, external handoffs, DWP strategic self-service portal/app integration boundary. | Scaffolded | `I3PS`, `MR`, `DIC`, `CAS`, `SoR`, `CC`, `IEC`, `RPF` |
| `MOD-BE-THIRDPARTYMGMT` | `domain/thirdpartymanagement` | DCA placement, recall, partner status sync, billing, commission and DCA reconciliation ownership. | Not scaffolded | `3PM`, `I3PS`, `SoR`, `MIR` |
| `MOD-BE-USER` | `domain/user` | User lifecycle, RBAC, team scope, Keycloak integration, admin access boundaries. | Scaffolded | `UAAF`, `CP`, NFR `INT01`-`INT03`, `INT27`-`INT28` |
| `MOD-BE-AUDIT` | `domain/audit` | Append-only events, activity history, override evidence, trace records, compliance evidence. | Partially implemented | `AAD`, `SoR`, `DIC`, `CP`, NFR `COM06`-`COM07` |
| `MOD-BE-REPORTING` | `domain/reporting` | KPI projections, MI exports, queue health and DCA performance read models. | Scaffolded | `MIR`, `WAM`, `3PM`, `SoR`, NFR `OPP04` |

### Process, Frontend, Platform, and Governance Modules

| Module ID | Module | Design responsibility | Repository status | Main tender coverage |
|---|---|---|---|---|
| `MOD-BE-PROCESS` | `infrastructure/process` | Flowable BPMN/DMN runtime, process starts/events, delegates, workflow versioning, event subprocesses. | Not scaffolded | `DW`, `RPF`, `WAM`, `AAD`, `CC`, `UAAF` |
| `MOD-SHARED-PROCESS-PORT` | `shared/process/port` | Process ports, delegate command bus, command contracts between Flowable and domains. | Not scaffolded | Enables `DW` process execution; not a tender capability by itself |
| `MOD-FE-APP` | `frontend/src` | GOV.UK React staff UI, dashboards, forms, workflow screens, admin/configuration surface, staff visibility of self-service actions. | Scaffolded | `UI`, plus screens for all operational families; NFR `ACC`, `INT08`-`INT10` |
| `MOD-PLAT-INFRA` | `infrastructure/` | Docker Compose, Helm, Keycloak realm, deployment topology and environment parity. | Implemented baseline | `CP`, `SD`, NFR `AVA`, `OPP`, `REC`, `SCA`, `SEC`, `INT` |
| `MOD-PLAT-CICD` | `.github/workflows/` | CI, smoke tests, Helm lint, dev deployment, release gates. | Implemented baseline | `CP`, `SD`, NFR `COM11`, `SEC03` |
| `MOD-GOV-DOCS` | `docs/project-foundation/` | Standards, ADRs, traceability, domain rulings, release evidence templates, governance process. | Implemented baseline | `CP`, `SD`, NFR `COM`, `DC`, `SEC`, `ACC` evidence |

## Functionality Catalogue and Requirement Mapping

Confidence legend:

- `trace-linked` means at least one explicit requirement-to-functionality link exists in `trace-map.yaml`.
- `family-mapped` means the functionality is clearly covered by tender requirement families, but most individual requirement IDs are still bootstrap gaps in `trace-map.yaml`.
- `design-extension` means the functionality is included in the design primarily to make the solution governable, operable, safe, or deliverable; it is not explicitly named as a tender functional requirement.

| Functionality | Name | Primary module | Supporting modules | Tender requirement mapping | Confidence |
|---|---|---|---|---|---|
| `FNC-PROC-001` | Collection process orchestration | `MOD-BE-PROCESS` | `strategy`, `workallocation`, frontend, audit | `DW`, `WAM`, `AAD`, `UI`; supports `CAS`, `DIC`, `SoR` during case initiation | `trace-linked` for selected `DW`; otherwise `family-mapped` |
| `FNC-PROC-002` | Write-off approval workflow | `MOD-BE-PROCESS` | account, workallocation, audit | `DW`, `UAAF`, `WAM`, `AAD`, `SoR` | `family-mapped` |
| `FNC-PROC-003` | Treatment path history and override management | `MOD-BE-STRATEGY` | process, frontend, audit | `DW`, `AAD`, `UAAF`, `UI`, `SoR` | `trace-linked` for selected `DW`; otherwise `family-mapped` |
| `FNC-PROC-004` | Breathing space process | `MOD-BE-PROCESS` | account, repaymentplan, audit | `DW`, `RPF`, `CC`, `AAD`, `SoR`; governed by rulings | `family-mapped` |
| `FNC-PROC-005` | Contact suppression enforcement | `MOD-BE-COMMS` | process, strategy, audit | `CC`, `DIC`, `AAD`, `DW`, `SoR`; linked to suppression rulings | `trace-linked` for `CC.5`; otherwise `family-mapped` |
| `FNC-PROC-006` | Champion/challenger treatment assignment | `MOD-BE-STRATEGY` | process, analytics | `DW`, `BSF`, `A`, `MIR`; governed by ADR-010 and RULING-010 | `trace-linked` for selected `BSF`; otherwise `family-mapped` |
| `FNC-PROC-007` | Arrangement monitoring and breach detection | `MOD-BE-REPAYMENTPLAN` | process, payment, workallocation, audit | `RPF`, `DW`, `WAM`, `AAD`, `SoR` | `trace-linked` for `RPF.8`; otherwise `family-mapped` |
| `FNC-PROC-008` | Statute-barred debt identification | `MOD-BE-STRATEGY` | process, account | `DW`, `DIC`, `SoR`, `CC`; governed by RULING-009, RULING-012, RULING-013 | `family-mapped`; awaiting ruling confirmations |
| `FNC-RPF-001` | Income and expenditure capture | `MOD-BE-REPAYMENTPLAN` | customer, frontend, audit | `IEC`, `RPF`, `UI`, `DIC`; governed by RULING-007 | `trace-linked` for selected `IEC`; otherwise `family-mapped` |
| `FNC-RPF-002` | Repayment arrangement creation and management | `MOD-BE-REPAYMENTPLAN` | payment, process, frontend, audit | `RPF`, `DW`, `UI`, `SoR`, `IEC` | `family-mapped` |
| `FNC-AUTH-001` | User account and identity management | `MOD-BE-USER` | infrastructure, audit | `UAAF`, `CP`; NFR `INT01`, `INT02`, `INT03`, `INT27`, `INT28` | `trace-linked` for `UAAF.2`; otherwise `family-mapped` |
| `FNC-AUTH-002` | Team-based and role-based access control | `MOD-BE-USER` | workallocation, frontend | `UAAF`, `WAM`, `UI`, `CAS.7`; NFR `MOB02`, `MOB04` | `family-mapped` |
| `FNC-AUTH-003` | Vulnerability flag capture and workflow routing | `MOD-BE-CUSTOMER` | process, strategy, frontend | `DIC`, `DW`, `UAAF`, `WAM`, `CC`; governed by RULING-002 | `trace-linked` for `DW.28`; otherwise `family-mapped` |
| `FNC-AUTH-004` | Write-off authority threshold enforcement | `MOD-BE-PROCESS` | account, workallocation | `UAAF`, `WAM`, `DW`, `AAD`, `SoR`; governed by RULING-003 | `family-mapped` |
| `FNC-CUST-001` | Customer profile and contact management | `MOD-BE-CUSTOMER` | frontend, audit | `CAS`, `DIC`, `CC`, `UI` | `family-mapped` |
| `FNC-CUST-002` | Account balance, ledger, and payment history | `MOD-BE-ACCOUNT` | payment, frontend, audit | `CAS`, `DIC`, `SoR`, `RPF`, `UI` | `family-mapped` |
| `FNC-CUST-003` | Relationship and household management | `MOD-BE-CUSTOMER` | account, frontend, audit | `CAS`, `CC`, `UI`, `DIC`; governed by joint-debt rulings | `family-mapped` |
| `FNC-CUST-004` | Affordability assessment | `MOD-BE-STRATEGY` | repaymentplan, customer | `IEC`, `RPF`, `DW`; governed by RULING-007 | `family-mapped` |
| `FNC-AUD-001` | Immutable event audit log | `MOD-BE-AUDIT` | process, frontend | `AAD`, `SoR`, `CP`, `DIC`; NFR `COM03`, `COM06`, `COM07` | `family-mapped` |
| `FNC-AUD-002` | Compliance and override audit reporting | `MOD-BE-AUDIT` | reporting, frontend | `AAD`, `MIR`, `SoR`, `CP`; NFR `COM06`, `COM07` | `family-mapped` |
| `FNC-INTG-001` | DCA placement and integration | `MOD-BE-INTEGRATION` | thirdpartymanagement, audit | `3PM`, `I3PS`, `CC`, `SoR`, `MIR`; governed by RULING-008 | `family-mapped` |
| `FNC-INTG-002` | DWP Place integration and debt ingest | `MOD-BE-INTEGRATION` | customer, account, process | `I3PS`, `CAS`, `DIC`, `SoR`, `MR`; NFR `DM01`, `INT11`-`INT26`, `REC05` | `trace-linked` for selected `I3PS`; otherwise `family-mapped` |
| `FNC-SS-001` | Self-service integration boundary | `MOD-BE-INTEGRATION` | user, audit, platform | `CC.22`, `I3PS.2`; NFR `INT`, `SEC`, `MOB` | `family-mapped`; new design slice |
| `FNC-SS-002` | Self-service contact-detail update intake | `MOD-BE-INTEGRATION` | customer, communications, audit, frontend | `DIC.2`, `DIC.21`, `CC.22` | `family-mapped`; new design slice |
| `FNC-SS-003` | Self-service I&E submission intake | `MOD-BE-INTEGRATION` | repaymentplan, strategy, audit, frontend | `IEC.2`, `IEC.8`, `CC.22`, `DW.22` | `family-mapped`; new design slice |
| `FNC-SS-004` | Self-service arrangement and payment action intake | `MOD-BE-INTEGRATION` | repaymentplan, payment, process, audit | `RPF.3`, `CC.22`, `DW.22` | `family-mapped`; new design slice |
| `FNC-SS-005` | Self-service action audit and timeline visibility | `MOD-BE-AUDIT` | integration, customer, frontend, reporting | `DIC.21`, `DIC.8`, `AAD`, `SoR` | `family-mapped`; new design slice |
| `FNC-SS-006` | Portal/app engagement signal for communication strategy | `MOD-BE-INTEGRATION` | communications, strategy, audit | `DW.81`, `DW.65`, `CC.22` | `family-mapped`; new design slice |

## Requirement Family to Functionality and Module Map

| Tender requirement family | IDs | Primary modules | Main functionality slices | Notes |
|---|---|---|---|---|
| Customer & Account Structure | `CAS.1`-`CAS.17` | `customer`, `account` | `FNC-CUST-001`, `FNC-CUST-002`, `FNC-CUST-003`, `FNC-INTG-002`, `FNC-AUTH-002`, `FNC-PROC-006` | Covers relationships, account/customer views, external references, organisation structure and customer/account-level decisioning. |
| Data & Information Capture | `DIC.1`-`DIC.37` | `customer`, `account`, `communications`, `audit`, `integration` | `FNC-CUST-001`, `FNC-CUST-002`, `FNC-AUTH-003`, `FNC-PROC-005`, `FNC-AUD-001`, `FNC-INTG-002`, `FNC-SS-002`, `FNC-SS-005` | Adds explicit self-service contact update and self-service action recording coverage for `DIC.2`, `DIC.8`, and `DIC.21`. `DIC.23` and `DIC.35` are missing from `trace-map.yaml`. |
| Decisions & Workflows | `DW.1`-`DW.88` | `process`, `strategy`, `workallocation`, `integration` | `FNC-PROC-001`-`FNC-PROC-008`, `FNC-CUST-004`, `FNC-AUTH-003`, `FNC-AUTH-004`, `FNC-RPF-002`, `FNC-SS-003`, `FNC-SS-004`, `FNC-SS-006` | Adds self-service I&E/offer workflow trigger coverage for `DW.22`, in-app message support for `DW.65`, and portal/app engagement signal support for `DW.81`. `DW.23` and `DW.36` are missing from `trace-map.yaml`; most granular links remain bootstrap gaps. |
| Repayment Plan Functionality | `RPF.1`-`RPF.37` | `repaymentplan`, `payment`, `integration` | `FNC-RPF-001`, `FNC-RPF-002`, `FNC-PROC-004`, `FNC-PROC-007`, `FNC-CUST-004`, `FNC-CUST-002`, `FNC-SS-003`, `FNC-SS-004` | Arrangement creation, monitoring, affordability, payment interaction, and self-service arrangement setup via integration (`RPF.3`). |
| User Access & Admin Functions | `UAAF.1`-`UAAF.26` | `user`, `process` | `FNC-AUTH-001`, `FNC-AUTH-002`, `FNC-AUTH-004`, `FNC-PROC-002`, `FNC-AUD-001` | RBAC matrix is designed in `RBAC-IMPLEMENTATION-DECISIONS.md`; threshold values still need DWP confirmation. |
| MI & Reporting | `MIR.1`-`MIR.2` | `reporting` | `FNC-AUD-002`, reporting read models, MI dashboard/export design | Detailed MI UX exists, but only two functional IDs explicitly cover this broad area. |
| Analytics | `A.1`-`A.2` | `analytics`, `strategy` | `FNC-PROC-006`, analytics projections, strategy performance analysis | Analytics module is scaffolded; detailed analytics functions are partly design extensions. |
| User Interface Screens | `UI.1`-`UI.30` | frontend | All staff-facing forms, dashboards, workflow screens, admin/configuration screens | UI requirements cover screen presence/usability; detailed workflow functionality traces to backend families. |
| Work Allocation & Monitoring | `WAM.1`-`WAM.28` | `workallocation` | `FNC-PROC-001`, `FNC-PROC-002`, `FNC-PROC-007`, `FNC-AUTH-002`, `FNC-AUTH-003` | Phase 3 work-allocation domain pack expands SLA, skills, queue and supervisor read model design. |
| Agent Actions & Dispositions | `AAD.1`-`AAD.27` | `communications`, `audit`, `workallocation` | `FNC-AUD-001`, `FNC-AUD-002`, `FNC-PROC-003`, `FNC-PROC-005`, `FNC-PROC-007` | Captures contact outcomes, notes, dispositions, overrides and audit evidence. |
| 3rd Party Management | `3PM.1`-`3PM.18` | `thirdpartymanagement`, `integration` | `FNC-INTG-001`, DCA reporting/read models, DCA reconciliation | Module not scaffolded; RULING-008 governs placement prerequisites. |
| Income & Expenditure Capture | `IEC.1`-`IEC.11` | `repaymentplan`, `customer`, `strategy`, `integration` | `FNC-RPF-001`, `FNC-CUST-004`, `FNC-RPF-002`, `FNC-SS-003` | Adds explicit self-service I&E submission and I&E-to-arrangement suggestion coverage for `IEC.2` and `IEC.8`. I&E staleness period and benefit-deduction treatment remain open policy values. |
| Bureau & Scorecard Feeds | `BSF.1`-`BSF.15` | `analytics`, `strategy`, `integration` | `FNC-PROC-006`, strategy scoring/segmentation feeds | Bureau feed contracts and scorecard integration need detailed API/file contract work. |
| Contact Channels | `CC.1`-`CC.35` | `communications`, `integration`, `audit` | `FNC-PROC-005`, `FNC-CUST-001`, `FNC-INTG-001`, `FNC-SS-001`, `FNC-SS-002`, `FNC-SS-003`, `FNC-SS-004`, `FNC-SS-006`, communications templates/domain pack | Includes multi-channel contact, suppression, history, in-app messaging, and DWP strategic portal/app self-service integration (`CC.22`). |
| Interfaces to 3rd Party Systems | `I3PS.1`-`I3PS.15` | `integration` | `FNC-INTG-001`, `FNC-INTG-002`, `FNC-SS-001` | REST and file transfer expectations also appear in NFR `INT`; `I3PS.2` covers real-time web portal/self-service interaction. |
| System of Record | `SoR.1`-`SoR.21` | `account`, `payment`, `audit`, `integration`, `reporting` | `FNC-CUST-002`, `FNC-AUD-001`, `FNC-AUD-002`, `FNC-INTG-002`, `FNC-PROC-*` where lifecycle state is involved | Must preserve clear state authority: Flowable owns lifecycle; account/payment own financial facts; audit owns evidence. |
| Change Processes | `CP.1`-`CP.11` | governance docs, CI/CD, infrastructure, user | `FNC-AUTH-001`, `FNC-AUD-001`, release/traceability governance, CI/CD gates | Much of this is governed through standards and delivery artifacts rather than domain code. |
| System Development & Roadmap | `SD.1`-`SD.7` | governance docs, CI/CD | ADRs, roadmap, standards, release evidence, traceability system | Satisfied through documentation and delivery process controls. |
| Migration Requirements | `MR1`-`MR15` | `integration` | `FNC-INTG-002`, migration tooling/runbooks, migration evidence | Migration architecture is still an open design area. |

## Non-Functional Requirement Cross-Reference

NFRs are not yet seeded into `trace-map.yaml`. This table records the current design-level ownership until the NFR trace is created.

| NFR family | Design owner modules | Current design coverage |
|---|---|---|
| `ACC` Accessibility | frontend, governance docs, CI/test | GOV.UK Design System, accessibility standard, frontend a11y test setup. |
| `AVA` Availability | platform infrastructure, backend, database, CI/CD | Kubernetes/Helm direction, health endpoints, readiness/liveness checks; production HA detail remains environment-specific. |
| `COM` Compliance | governance docs, audit, user, platform, frontend | Standards pack, domain rulings, audit trail design, accessibility and security governance. |
| `CON` Configuration | process, strategy, frontend admin, governance docs | Three-tier configurability, policy bundles, admin UI, process/rule governance. |
| `DC` Data Controller | governance docs, audit, security, integration | Data sensitivity/redaction standards; incident and breach operational procedures need release-specific evidence. |
| `DM` Data Migration | integration, platform, audit, governance docs | Migration requirement family exists; detailed tooling and runbooks remain open. |
| `GEN` General | platform, frontend | Microsoft 365 compatibility only applies if Office components are used; none are currently designed as core dependencies. |
| `INT` Interoperability | integration, user, platform, frontend | OIDC/Keycloak, REST APIs, HTTPS, file-transfer standards; FTPS/SFTP implementation contract remains future work. |
| `MOB` Mobile access | frontend, user, security | Responsive web design and RBAC support; native public/private app store requirements are not part of current web-first design. |
| `OPP` Operability | platform, reporting, governance docs | Local/dev environment design, live-data reporting intent, structured logs; production operating model remains to be defined. |
| `P&C` Performance and capacity | backend, frontend, reporting, platform | Read-model/projection strategy and horizontal scale posture; load evidence not yet produced. |
| `POR` Portability | integration, reporting, platform, governance docs | Export/read-model design and data ownership posture; end-of-term disposal evidence process needs definition. |
| `REC` Recoverability | platform, integration, audit, reporting | Recovery objectives, reconciliation report design intent, health checks; DR plan/test evidence pending future environments. |
| `REL` Reliability | platform, operations docs | Support model/runbooks need completion for production readiness. |
| `SCA` Scalability | backend, reporting, platform | Modular monolith, projection read models, Kubernetes horizontal scaling; performance tests pending. |
| `SEC` Security | user, platform, audit, governance docs | Keycloak, Spring Security, security standards, redaction, audit; penetration/accreditation evidence pending. |

## Design Functionality Not Explicitly Named in Tender Requirements

These capabilities are included in the current design even though the tender requirements do not explicitly name them as functional requirements. Most are justified as implementation controls for functional, non-functional, governance, audit, or regulatory outcomes.

| Designed capability | Owning module/artifact | Why it exists | Requirement relationship |
|---|---|---|---|
| Embedded Flowable process engine with one process instance per debt/account-person recoverable unit | `MOD-BE-PROCESS`, ADR-001 to ADR-003 | Provides auditable and changeable lifecycle orchestration. | Enables `DW`, `WAM`, `AAD`, `RPF`; not explicitly mandated as Flowable/BPMN. |
| Delegate command bus and `shared/process/port` isolation | `MOD-SHARED-PROCESS-PORT`, `MOD-BE-PROCESS` | Keeps domain modules free of Flowable dependencies and preserves testability. | Architecture control; not explicitly tendered. |
| Standard BPMN event subprocess library for breathing space, death, insolvency, benefit ceased and missed payment | `MOD-BE-PROCESS` | Centralises legally significant lifecycle interruptions. | Enables `DW`, `CC`, `RPF`, `SoR`; specific mechanism not tendered. |
| Three-tier configurability architecture | ADR-008, frontend admin, strategy/process modules | Separates foundational config, rules/DMN, and BPMN process architecture. | Supports `CON01`, `CP`, `DW`; three-tier model not explicitly tendered. |
| Cross-tier policy bundle layer with effective dating and approval | ADR-009, admin/policies design | Allows coherent release and rollback of related rule/process/config changes. | Supports `CP`, `SD`, governance and audit; policy-bundle construct is design-added. |
| Unified role-scoped admin UI for Foundations, Rules, Processes, Templates and Policies | ADR-015, `MOD-FE-APP`, `MOD-BE-USER` | Gives authorised users one controlled configuration surface. | Supports `UAAF`, `CP`, `CON01`; exact admin IA not explicit. |
| Process Action Library | `process-action-library-design.md` | Governed catalogue of reusable BPMN service-task actions with validation. | Supports `DW`, `CP`, `SD`; not explicitly tendered. |
| Strategy simulation engine | `strategy-simulation-engine-design.md` | Lets teams test DMN/policy/process impact before activation. | Supports `A`, `BSF`, `DW`, `CP`; simulation engine not explicit. |
| Domain ruling gate for Class A features | `domain-rulings/`, governance standards | Prevents legally consequential behaviour being implemented without approved interpretation. | Supports compliance and traceability; not a named tender function. |
| Release evidence pack and traceability operating model | `trace-map.yaml`, standards, templates | Provides auditable requirement-to-test-to-evidence chain. | Supports `CP.6`, `CP.8`, `SD`; detailed artifact model is design-added. |
| Local/remote parity standard and no-stub auth flow | platform standards, Docker Compose, Keycloak | Avoids environment drift and hidden integration defects. | Supports `CP.5`, NFR `OPP`/`INT`; implementation rule not explicit. |
| Structured logging with correlation ID | backend config, operations standards | Provides diagnosis and cross-boundary support without leaking sensitive data. | Supports NFR `SEC`, `REC`, `OPP`; exact log contract not explicit. |
| Projection/read-model strategy for case summaries and MI dashboards | `MOD-BE-REPORTING`, reporting domain pack | Prevents UI/reporting from querying Flowable history at scale. | Supports `MIR`, `OPP04`, `SCA`; projection architecture not explicit. |
| Dashboard freshness, reconciliation state and export audit controls | MI/dashboard UX design, reporting module | Makes MI trustworthy and operationally safe. | Supports `MIR`, `SoR`, `COM`, `REC05`; specific UX controls not explicit. |
| Deterministic champion/challenger assignment by hash | ADR-010, strategy/analytics | Prevents random or unfair treatment assignment and supports repeatable evidence. | Supports `A`, `BSF`, `DW`; deterministic hash method not explicit. |
| Append-only reconciliation evidence for external handoffs | integration standards, audit/reporting | Makes failed or divergent cross-system interactions recoverable and auditable. | Supports `I3PS`, `3PM`, `REC05`, `SoR`; evidence model not explicit. |

## Demo Flow Coverage Analysis

### Demo sources reviewed

Primary sources:

- `docs/project-foundation/demo-flow-design.md` - project-authored 14-flow demo narrative.
- `Tender requirements docs/C8618 - FDS - Attachment 9 - Supplier Demonstration Agenda.md` - tender evaluation agenda and mandatory demonstration criteria.

Supporting cross-check:

- `docs/project-foundation/planning-notes.md` - non-authoritative planning reference imported from the archived development plan. It is useful for scenario labels, but must not override canonical requirements, ADRs, rulings, or `trace-map.yaml`.

### Demo flow to functionality coverage

| Demo flow / agenda area | Main functionality covered | Primary modules exercised | Coverage assessment |
|---|---|---|---|
| Demo Flow 1 - Account intake to first meaningful contact | `FNC-INTG-002`, `FNC-CUST-001`, `FNC-CUST-002`, `FNC-PROC-001`, `FNC-PROC-006`, `FNC-PROC-005` | integration, customer, account, process, strategy, analytics, workallocation, communications | Strong design coverage for ingest, matching, segmentation, first contact and worklist prioritisation. |
| Demo Flow 2 - Vulnerability to resolution | `FNC-AUTH-003`, `FNC-PROC-005`, `FNC-RPF-001`, `FNC-CUST-004`, `FNC-RPF-002`, `FNC-AUD-001` | customer, process, communications, repaymentplan, strategy, audit, frontend | Strong coverage; depends on vulnerability ruling and I&E policy values before becoming closure-grade. |
| Demo Flow 3 - Arrangement breach to DCA placement | `FNC-PROC-007`, `FNC-PROC-005`, `FNC-INTG-001`, `FNC-CUST-002`, `FNC-AUD-001`, `FNC-AUD-002` | repaymentplan, payment, communications, integration, thirdpartymanagement, audit, reporting | Mostly covered; DCA module is not scaffolded and payment/DCA reconciliation needs more granular functionality. |
| Demo Flow 4 - Complex household and joint liability | `FNC-CUST-003`, `FNC-PROC-004`, `FNC-PROC-005`, `FNC-AUD-001`, `FNC-AUTH-002` | customer, account, process, communications, audit, user | Partially covered; joint debt split and estate/breathing-space edge rules remain ruling-gated and need dedicated trace links. |
| Demo Flow 5 - Business user changes collection strategy | `FNC-PROC-003`, `FNC-PROC-006`, design extensions for policy bundles, simulation and admin UI | strategy, process, analytics, frontend, audit, governance docs | Covered as design intent; visual workflow authoring, simulation and promotion controls are broader than current `FNC-*` granularity. |
| Demo Flow 6 - Executive and operational dashboard | `FNC-AUD-002`, reporting read models, analytics projections, `FNC-PROC-006`, workallocation reporting | reporting, analytics, workallocation, audit, frontend | Covered by reporting/analytics design; only weakly represented by two `MIR` functional IDs and needs MI-specific trace expansion. |
| Demo Flow 7 - Customer self-service portal | `FNC-SS-001`-`FNC-SS-006` for backend integration; customer-facing portal UI remains external/deferred | integration, customer, repaymentplan, payment, communications, audit, frontend staff timeline | Backend self-service capability is now covered as DWP strategic portal/app integration. A DCMS-owned public portal journey remains out of current baseline unless scope is reopened. |
| Demo Flow 8 - Debt dispute freeze and resolution | `FNC-PROC-005`, `FNC-AUD-001`, `FNC-CUST-002`, likely future dispute workflow slice | communications, audit, account, process, workallocation | Partially covered through suppression/audit/account amendment principles, but there is no dedicated dispute lifecycle functionality slice. |
| Demo Flow 9 - Deceased customer protocol | `FNC-PROC-005`, `FNC-AUD-001`, `FNC-CUST-001`, likely future deceased-party workflow slice | customer, account, communications, process, audit | Partially covered; deceased handling is ruling-gated and lacks a dedicated functionality slice. |
| Demo Flow 10 - Write-off, recovery and reactivation | `FNC-PROC-002`, `FNC-AUTH-004`, `FNC-AUD-001`, `FNC-CUST-002`, payment/reactivation behavior | process, account, payment, workallocation, audit, user | Partially covered; write-off approval is mapped, but post-write-off recovery/reactivation needs its own functionality or explicit extension of `FNC-PROC-002`. |
| Demo Flow 11 - New agent guided resolution | `FNC-AUTH-001`, `FNC-AUTH-002`, `FNC-RPF-001`, `FNC-RPF-002`, `FNC-AUD-001`, frontend guided UI | user, workallocation, frontend, repaymentplan, audit | Covered at module level; guided scripting/coaching is not yet a named functionality slice. |
| Demo Flow 12 - Regulatory audit on demand | `FNC-AUD-001`, `FNC-AUD-002`, `FNC-PROC-003`, `FNC-PROC-004`, `FNC-PROC-005` | audit, reporting, process, communications, frontend | Strong design coverage for audit intent; evidence export details need trace/test/evidence links. |
| Demo Flow 13 - Month-end surge | `FNC-INTG-002`, `FNC-PROC-001`, `FNC-PROC-006`, `FNC-PROC-007`, reporting/analytics projections | integration, process, strategy, analytics, workallocation, communications, reporting | Covered as design intent; load/performance evidence is still an NFR gap. |
| Demo Flow 14 - Lump sum settlement offer | `FNC-AUTH-004`, `FNC-PROC-002`, `FNC-CUST-002`, `FNC-AUD-001`, `FNC-RPF-002` | process, account, payment, audit, user, frontend | Partially covered; partial settlement is not yet a dedicated functionality slice distinct from write-off/arrangement behavior. |
| Tender Demo 1 - Integration | `FNC-INTG-001`, `FNC-INTG-002`, `FNC-SS-001`, `FNC-SS-004`, `FNC-PROC-005`, payment integration behavior | integration, communications, payment, audit, platform | Mostly covered at design level, including Self Service Portal integration as a platform type. Message queues, payment platform integration, integration monitoring and developer tooling need more explicit design/trace. |
| Tender Demo 2 - System capability | Most operational slices: `FNC-CUST-*`, `FNC-PROC-*`, `FNC-RPF-*`, `FNC-AUTH-*`, `FNC-AUD-*` | all business modules plus frontend/reporting | Broadly covered; gaps remain around free-format communication upload, multi-party communications, and user/team-level workflow configuration specifics. |
| Tender Demo 3 - System flexibility | `FNC-CUST-003`, `FNC-AUTH-003`, `FNC-RPF-001`, `FNC-CUST-004`, `FNC-AUTH-004`, payment allocation behavior | customer, account, repaymentplan, strategy, process, user, payment | Partially covered; household/joint split, Direct Debit/DEA, forbearance tracking and benefit/fraud data capture need first-class trace entries. |
| Tender Demo 4 - Deployment facilitation | design extensions for CI/CD, release evidence, policy bundles, local/remote parity | CI/CD, infrastructure, governance docs, frontend admin | Covered by governance/platform design rather than business `FNC-*`; NFR trace still missing. |

### Functionality coverage summary

| Coverage category | Functionality / capability | Finding |
|---|---|---|
| Clearly demo-covered by primary flows | `FNC-INTG-002`, `FNC-CUST-001`, `FNC-CUST-002`, `FNC-PROC-001`, `FNC-PROC-003`, `FNC-PROC-005`, `FNC-PROC-006`, `FNC-PROC-007`, `FNC-RPF-001`, `FNC-RPF-002`, `FNC-AUTH-001`, `FNC-AUTH-002`, `FNC-AUTH-003`, `FNC-AUTH-004`, `FNC-CUST-004`, `FNC-AUD-001`, `FNC-AUD-002`, `FNC-INTG-001`, `FNC-SS-001`-`FNC-SS-006` | These appear directly in either the 14 project demo flows, the tender demo agenda, or both. Most are still only design-level/family-level mapped, not trace-complete. |
| Demo-covered but ruling-gated or incomplete | `FNC-PROC-002`, `FNC-PROC-004`, `FNC-PROC-008`, `FNC-CUST-003` | Write-off, breathing space, statute-barred and joint/household behavior are central to demos, but depend on domain rulings, open DWP questions, or more granular trace. `FNC-PROC-008` is only present in planning notes and statute-barred ruling material, not in the main 14-flow demo narrative. |
| Covered only as design extension / platform governance | Policy bundles, Process Action Library, strategy simulation, release evidence packs, local/remote parity, dashboard freshness, reconciliation evidence | Tender demos require these outcomes, but the exact design mechanisms are not explicit tender requirements. Keep them labelled as enabling controls. |
| Covered as integration boundary; public portal UI deferred | Customer self-service portal journey | DCMS now covers the backend self-service capability via `FNC-SS-001`-`FNC-SS-006`. A live DCMS-owned public portal journey remains deferred because the baseline assumes the DWP strategic portal/app is the customer-facing component. |

### Demo criteria not yet represented as first-class functionality slices

These are the most important items to add or explicitly defer in `functionality_catalog` if demo traceability is required.

| Demo criterion / flow need | Current coverage | Gap |
|---|---|---|
| Self Service Portal integration and customer self-service journey | `FNC-SS-001`-`FNC-SS-006` now define the DCMS backend integration capability. | Add these slices to `trace-map.yaml` and draft endpoint-level API contracts. A DCMS-owned public portal UI remains explicitly out of current baseline. |
| Direct Debit creation and monitoring | Payment and repayment modules exist; tender Demo 3 criterion 22 explicitly requires this. | No dedicated payment-channel/direct-debit functionality slice. |
| DEA / benefit deduction payment channel | Mentioned as DWP-specific critical demo item in the tender agenda; planning notes mention DEA as a payment channel. | No dedicated DEA/benefit-deduction functionality slice in the current catalogue. |
| Third-party payment platform integration | Tender Demo 1 criterion 6 requires third-party payment platform integration. | Payment integration is implicit in `payment`/`integration`; add explicit payment gateway/integration slice or trace under `FNC-INTG-*`. |
| Free-format letter/email/SMS upload | Tender Demo 2 criterion 12 and Demo 3 criterion 18. | Communications capability exists, but no explicit free-format/upload functionality slice. |
| Communications to multiple parties simultaneously | Tender Demo 2 criterion 13. | Relationship and communications modules support the concept, but no explicit multi-party communication orchestration slice. |
| Communication template setup through third-party template platform | Tender Demo 2 criterion 8 and Demo 3 criterion 10. | Communications templates are in design/domain packs, but not a named `FNC-*` slice. |
| Message queue technology and integration monitoring/developer tooling | Tender Demo 1 criteria 1 and 4. | Integration module owns external handoffs, but queueing/tooling/monitoring are not first-class functionality slices. |
| Manual and automatic joint debt split with residual 1p write-off | Covered conceptually by `FNC-CUST-003` and write-off authority, governed by joint-debt rulings. | Needs dedicated trace and ruling-gated implementation criteria; current mapping is too broad. |
| Benefit status and fraud flag data capture | Account/customer data structures can own these facts; tender Demo 3 criterion 7 makes them explicit. | Not named in current functionality catalogue; should be traced either under `FNC-CUST-002` or a new regulatory/account markers slice. |
| Forbearance recommendation and appropriateness tracking | Affordability assessment exists through `FNC-CUST-004`; tender Demo 3 criteria 16-17 are more specific. | No explicit forbearance tracking slice. |
| Dispute lifecycle | Demo Flow 8 covers freeze/investigate/resolve. | Suppression and audit are present, but no dedicated dispute workflow/functionality slice. |
| Deceased-party lifecycle | Demo Flow 9 covers deceased notification and estate workflow. | Ruling-gated design exists, but no dedicated deceased-party functionality slice. |
| Post-write-off recovery/reactivation | Demo Flow 10 covers unexpected payment against written-off account. | Write-off approval exists, but recovery/reactivation behavior is not explicitly modelled. |
| Agent scripting, prompts, and coaching | Demo Flows 2, 9, 11 and tender Demo 3 criterion 12 require guided prompts/alerts. | Frontend/user/workflow modules support this, but there is no named guided-agent-experience slice. |

### Demo coverage conclusion

The current design covers the majority of the primary demo narrative at module level, especially ingest, segmentation, workflow orchestration, vulnerability handling, repayment/I&E, audit, reporting, DCA placement, RBAC and strategy change. The main demo risk is not absence of architectural coverage; it is that several demo-critical items are still implicit inside broad modules rather than represented as traceable functionality slices.

For demo-readiness traceability, the next catalogue update should add or explicitly defer first-class slices for: payment channels including Direct Debit and DEA, payment gateway integration, communication template/free-format upload, multi-party communications, dispute lifecycle, deceased-party lifecycle, joint-debt split, forbearance tracking, account regulatory markers, post-write-off reactivation, and guided agent scripting/alerts. Self-service now has first-class design slices, but those slices still need canonical trace-map entries and API contracts.

## Immediate Traceability Gaps

1. Add the four missing functional requirement IDs to `trace-map.yaml`: `DIC.23`, `DIC.35`, `DW.23`, `DW.36`.
2. Decide whether NFRs should be added to the same canonical `trace-map.yaml` or maintained in a parallel NFR trace artifact.
3. Promote the family-level mappings above into granular requirement-to-functionality links for priority slices first: vulnerability, breathing space, statute-barred debt, write-off, repayment arrangements, suppression, DCA placement, RBAC, migration and MI.
4. Add test suite IDs and evidence placeholders before any requirement is moved beyond `partial`.
5. Scaffold or explicitly defer missing target modules:
   - `backend/src/main/java/com/netcompany/dcms/infrastructure/process/`
   - `backend/src/main/java/com/netcompany/dcms/shared/process/port/`
   - `backend/src/main/java/com/netcompany/dcms/domain/thirdpartymanagement/`
6. Reconcile `functional-requirements-module-map.md` module statuses with the current repository: `analytics` and `reporting` now exist; `thirdpartymanagement`, `infrastructure/process`, and `shared/process/port` do not.
7. Resolve the `development-plan.md` section 14 requirement ID mismatch at a baseline review before using it for trace closure.
8. Promote `FNC-SS-001` to `FNC-SS-006` into `trace-map.yaml`, linking at minimum `CC.22`, `IEC.2`, `IEC.8`, `RPF.3`, `I3PS.2`, `DIC.2`, `DIC.8`, `DIC.21`, `DW.22`, `DW.65`, and `DW.81`.

## Working Conclusion

The design now covers the tender functional requirement families at module and capability level, and it also includes several governance, safety, operability and architecture controls that go beyond the literal tender wording. The main remaining gap is not conceptual coverage; it is trace granularity. Most individual requirement IDs still need confirmed `FNC-*` links, tests and evidence in `trace-map.yaml`.
