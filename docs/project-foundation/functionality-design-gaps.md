> ## ⚠ STATUS: UNSETTLED — PLATFORM PIVOT IN EFFECT
>
> A confirmed programme-level decision (ADR-018, 2026-04-30) has pivoted the project to build on top of **Solon Tax** as the base platform. A new design process is underway.
>
> **This document contains accumulated knowledge and analysis that remains relevant to the new design process. It does NOT represent the current or preferred direction.** All decisions and conclusions herein are open for renewed debate. Do not treat this document as a directive or default position.
>
> See [`CLAUDE.md`](../../CLAUDE.md) and [ADR-018](decisions/ADR-018-platform-pivot-solon-tax-confirmed.md) for the current direction.

---

# Functionality Design Gaps

**Project:** DWP Debt Collection Management System (DCMS)  
**Date:** 2026-04-27  
**Status:** Reclassified against current authoritative design baseline  
**Scope basis:** Design-gap inventory only. This is not a tender coverage statement.

---

## Purpose

This document records functionality design gaps after reconciliation with the current authoritative design set:

- `docs/project-foundation/MASTER-DESIGN-DOCUMENT.md`
- accepted ADRs in `docs/project-foundation/decisions/`
- locked implementation ADRs in `docs/project-foundation/architecture-decisions.md`
- domain rulings in `docs/project-foundation/domain-rulings/`
- domain packs in `docs/project-foundation/domain-packs/`

This version replaces the earlier broad gap list with four categories:

1. Superseded / obsolete gap statements
2. Design closed at architecture/domain-pack level
3. Design narrowed but still policy-gated
4. Still-open design gaps

---

## Out of Scope or Deferred

| Area | Decision | Notes |
|---|---|---|
| Customer self-service | Out of scope for current delivery | Portal, customer authentication, and customer-led workflow initiation remain excluded unless scope is reopened. |
| Migration tooling and batch migration design | Deferred | Migration design remains a separate stream; still listed as an open item in the master design. |

---

## 1. Superseded or Obsolete Gap Statements

These are not current gaps in the authoritative baseline.

### Additional-layer assumption

Any gap statement that assumes a separate "additional layer" as the target architecture is obsolete.

Current baseline:

- single modular monolith
- domain-owned capabilities inside the application boundary
- no separate platform layer for the domains previously explored in `gap-analysis-additional-layer.md`

### Reporting and communications as "thin / not yet decomposed"

These statements are no longer accurate at design level:

- reporting and MI are now substantially designed
- communications orchestration, templates, suppression, fallback, and contact preferences are now substantially designed
- work allocation and supervisor tooling are now substantially designed

---

## 2. Design Closed at Architecture / Domain-Pack Level

These areas were previously listed as gaps, but now have sufficient architecture or domain-pack design to be removed from the "major unresolved design gap" list. They may still require implementation, scaffolding, migrations, or policy answers, but they are no longer design-vacuum areas.

### 2.1 Reporting and Management Information

Now covered by:

- `docs/project-foundation/domain-packs/reporting-analytics-read-model-domain-pack.md`
- `docs/project-foundation/mi-dashboard-and-reporting-ux-design.md`
- `docs/project-foundation/MASTER-DESIGN-DOCUMENT.md`

Designed elements now include:

- KPI taxonomy
- projection-table pattern
- executive dashboard query contracts
- operations dashboard query contracts
- DCA performance projections
- champion/challenger reporting panels
- queue-health read models
- staff-facing executive, operations, champion/challenger, DCA, export-centre, and data-health dashboard UX
- controlled export metadata and access-audit pattern
- data freshness, stale-data warning, and projection reconciliation UX
- configurable KPI target and RAG-threshold governance
- RBAC-controlled aggregate-to-case drilldown pattern

### 2.2 Communication Template and Delivery Management

Now covered by:

- `docs/project-foundation/domain-packs/communications-domain-pack.md`
- ADR-015
- ADR-014 / locked suppression authority
- RULING-001
- RULING-011

Designed elements now include:

- template lifecycle and approval
- template variables and rendering controls
- delivery status tracking
- suppression-decision logging
- channel fallback model
- returned mail handling
- held-for-review vs statutory discard behaviour
- contact history timeline

### 2.3 Contactability, Preferences, and Consent

Now covered primarily by:

- `docs/project-foundation/domain-packs/communications-domain-pack.md`

Designed elements now include:

- customer contact preferences
- channel verification flags
- do-not-contact flag
- preferred contact windows
- returned-mail handling and invalid-address workflow
- channel-level suppression and availability rules

### 2.4 Work Allocation, Queues, and Supervisor Tooling

Now covered by:

- `docs/project-foundation/domain-packs/work-allocation-domain-pack.md`

Designed elements now include:

- queue model
- work item lifecycle
- SLA timers and breach events
- routing and assignment DMN structure
- supervisor dashboard
- agent worklist
- bulk reassignment
- queue pause / resume / escalation patterns

### 2.5 Champion / Challenger Governance and Analytics Guardrails

Now covered by:

- ADR-010
- locked ADR-008
- locked ADR-014
- RULING-010
- `docs/project-foundation/strategy-simulation-engine-design.md`

Designed elements now include:

- deterministic assignment
- vulnerable-customer exclusion from challenger treatment
- panelised analytics pattern
- promotion gates
- harm-indicator tracking expectations
- phased pre-live simulation, starting with DMN rule simulation and later adding policy impact, strategy path, and BPMN replay

---

## 3. Design Narrowed but Still Policy-Gated

These areas now have substantial design, but are still materially constrained by unresolved DWP policy answers, ruling sign-off, or remaining design elaboration.

### 3.1 Third-Party Debt Collection Management

Current position:

- no longer an undefined area
- substantially designed in `docs/project-foundation/domain-packs/DCA-management-domain-pack.md`
- governed by RULING-008

Remaining live gaps:

- required pre-placement notice period
- acknowledgement SLA
- recall acknowledgement SLA
- any tiered commission model
- whether real-time DCA API capability is in current scope
- final legal/content sign-off on disclosure notice content

### 3.2 Income & Expenditure / Affordability

Current position:

- owned by `domain/repaymentplan`
- architecturally integrated in the master design
- detailed draft pack exists in `docs/project-foundation/domain-packs/IE-engine-domain-pack.md`
- governed by RULING-007

Remaining live gaps:

- DWP-confirmed I&E staleness period
- benefit-deduction applicability
- seed policy values for standard living allowance bands

### 3.3 Vulnerability Management

Current position:

- no longer a major design vacuum
- current design inputs include rulings plus `docs/project-foundation/domain-packs/vulnerability-domain-pack.md`

Remaining live gaps:

- Article 9 lawful basis confirmation
- DWP vulnerability taxonomy
- system-indicator suppression timing policy
- joint-account vulnerability treatment semantics

### 3.4 Communications Open Policy Questions

Current position:

- domain largely designed

Remaining live gaps:

- outbound phone-call suppression scope
- contact-frequency window semantics
- DWP-approved fallback order
- returned-mail invalidation SLA
- whether `IN_APP` is in current scope or declared-but-deferred

### 3.5 Work Allocation Open Policy Questions

Current position:

- domain largely designed

Remaining live gaps:

- utilisation-reporting granularity
- maximum concurrent work-item policy
- team-leader cross-team visibility
- final SLA tier and escalation defaults

---

## 4. Still-Open Design Gaps

These remain the main unresolved functionality design gaps in the current baseline.

### 4.1 Frontend Screen Catalogue and End-to-End Staff Workflow Mapping

The current design now covers some important UI areas, but the full staff-facing workflow surface is still not decomposed end to end.

Still-open items include:

- customer overview and multi-debt case navigation map
- debt account detail composition outside the work-allocation desktop context
- full repayment-plan journeys
- dispute workflow screens
- write-off and settlement approval screens
- closure / archival views
- legal / enforcement operational screens
- finance operations screens
- consistency map across all workflow states, empty states, and error states

The MI/reporting dashboard surface is now separately designed in `mi-dashboard-and-reporting-ux-design.md`; the remaining frontend gap is the full end-to-end operational screen catalogue across all staff workflows.

### 4.2 Financial Accounting and Finance Operations

This remains under-designed compared with other areas.

Still-open items include:

- suspense handling
- refund handling
- overpayment handling
- partial settlement accounting
- write-off reversal rules
- journal export structure
- finance reconciliation operating model
- post-write-off payment treatment
- finance-facing correction / amendment workflow

### 4.3 Manual Corrections and Amendments

The current baseline strongly establishes immutable-record and audit principles, but the operational amendment journeys remain sparse.

Still-open items include:

- balance correction workflow
- customer-detail correction workflow
- misapplied-payment correction
- mistaken marker / fact correction flow
- amendment evidence model as a user-facing process
- approval boundaries by correction type
- corrected-vs-original display behaviour

### 4.4 Complaints Handling

Complaints are still not treated as a fully designed first-class workflow.

Still-open items include:

- complaint capture
- categorisation
- SLA tracking
- escalation
- workflow linkage to debt/account handling
- complaint-driven collection holds or restrictions
- complaint response pack generation
- complaint reporting and audit model

### 4.5 Legal and Enforcement Pathways

Legal / enforcement remains referenced in the master design but not yet decomposed into an explicit workflow/domain pack.

Still-open items include:

- legal referral criteria
- legal hold states
- evidence-pack generation
- legal team queue model
- proceedings status tracking
- return-from-legal path into normal treatment
- interaction with disputes, breathing space, insolvency, deceased handling, and arrangements

### 4.6 Data Retention, Closure, Archival, and Reactivation

This is still a genuine cross-domain design gap.

Still-open items include:

- closure-state model
- archival / retrieval design
- retention by data class
- evidence-retention treatment
- report-retention treatment
- deletion / anonymisation boundaries
- reactivation rules
- archived-record visibility model
- operational archival controls

### 4.7 Cross-Domain Notification, Alert, and Escalation Framework

Alerts and notifications exist in some local designs, but the system does not yet have a unified cross-domain model.

Still-open items include:

- notification-centre ownership
- cross-domain alert taxonomy
- deduplication / noise controls
- user subscriptions or role-based defaults
- acknowledgement model
- critical-alert audit behaviour
- distinction between operational alerts, workflow reminders, and compliance escalations

### 4.8 Operational Recovery Tooling

Operational recovery is partially covered through Flowable Admin controls, manual-review evidence patterns, failed handoff paths, and the reporting data-health design. It is not yet decomposed as an end-to-end operator journey.

Still-open items include:

- failed process-start recovery queue
- failed delegate / job recovery user journey
- failed DCA handoff recovery journey
- failed communication dispatch recovery journey
- projection rebuild / replay operator controls
- reconciliation exception workbench
- recovery action audit and evidence model
- runbook-to-UI linkage for high-risk recovery actions

---

## 5. Implementation / Scaffolding Gaps That Should Not Be Misread as Design Gaps

The master design explicitly identifies some remaining gaps as implementation or scaffolding work rather than unresolved product design.

These are:

- `analytics`
- `thirdpartymanagement`
- `reporting`
- `infrastructure/process`
- `shared/process/port`

These should be tracked separately from business-functionality design gaps.

---

## 6. Recommended Next Design Priorities

Based on the current state, the highest-value remaining design work is:

1. complaints handling
2. legal / enforcement pathways
3. finance operations and accounting depth
4. manual corrections and amendments
5. retention / closure / archival lifecycle
6. full frontend workflow and screen catalogue
7. operational recovery tooling
8. closure of policy-gated open questions in DCA, I&E, vulnerability, communications, and work allocation

---

## 7. Practical Reading Guidance

Use this document for:

- current design-gap triage
- identifying which capability areas still need domain packs or workflow design
- separating true design gaps from already-designed-but-not-yet-built areas

Do not use this document as evidence that:

- communications is still under-designed
- reporting is still under-designed
- work allocation is still under-designed
- the system architecture is still expected to use a separate additional layer

---

## 8. Summary

The current baseline is materially more mature than the earlier gap inventory suggested.

The remaining work is no longer "design everything." It is now:

- close the still-open domains
- resolve policy-gated questions
- scaffold the not-yet-built modules
- keep older exploratory documents clearly marked so they do not distort planning
