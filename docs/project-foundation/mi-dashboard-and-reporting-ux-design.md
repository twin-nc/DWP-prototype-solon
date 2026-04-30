> ## ⚠ STATUS: UNSETTLED — PLATFORM PIVOT IN EFFECT
>
> A confirmed programme-level decision (ADR-018, 2026-04-30) has pivoted the project to build on top of **Solon Tax** as the base platform. A new design process is underway.
>
> **This document contains accumulated knowledge and analysis that remains relevant to the new design process. It does NOT represent the current or preferred direction.** All decisions and conclusions herein are open for renewed debate. Do not treat this document as a directive or default position.
>
> See [`CLAUDE.md`](../../CLAUDE.md) and [ADR-018](decisions/ADR-018-platform-pivot-solon-tax-confirmed.md) for the current direction.

---

# MI Dashboard and Reporting UX Design

**Project:** DWP Debt Collection Management System (DCMS)  
**Date:** 2026-04-27  
**Status:** Design baseline added  
**Purpose:** Define the staff-facing MI, dashboard, export, freshness, and reporting user experience that sits above the `domain/reporting` and `domain/analytics` read-model design.

---

## 1. Design Intent

The reporting/analytics read-model domain pack defines the backend projection pattern, KPI taxonomy, and query contracts. This document defines how those capabilities appear to users.

The MI surface must help DWP staff answer operational and governance questions without exposing raw Flowable history, bypassing RBAC, or treating live operational telemetry as durable audit evidence.

Core principles:

- dashboards read from governed reporting projections, not from raw transactional tables or Flowable history directly
- every dashboard shows data freshness and projection health
- every aggregate can explain its filters, source period, and policy or target version where relevant
- exports are controlled, audited, classified, and reproducible
- champion/challenger MI is policy-aware, including the active protected-population eligibility rule
- MI helps spot operational risk but does not replace immutable audit records or release evidence packs

---

## 2. Scope

In scope:

- executive MI dashboard
- operations dashboard
- champion/challenger results dashboard
- DCA performance dashboard
- reporting export centre
- dashboard drilldown patterns
- data freshness and projection reconciliation UX
- RAG threshold and KPI target governance
- dashboard roles, permissions, and access states
- phased delivery roadmap

Out of scope:

- redefining the reporting projection schema already covered by the reporting/analytics read-model domain pack
- introducing a separate metrics database, BI platform, or reporting microservice
- replacing audit, case history, release evidence, or statutory recordkeeping
- finalising dashboards for domains that are not yet designed, such as complaints, legal enforcement, finance operations, and retention/archive

---

## 3. User Roles

| Role | MI access intent |
|---|---|
| `SRO` | Executive dashboard, high-level KPI trends, DCA performance summary, controlled exports where authorised. |
| `OPS_MANAGER` | Executive and operations dashboards, queue health, throughput, SLA performance, DCA performance, export requests. |
| `TEAM_LEADER` | Team-level operations dashboard, queue health, SLA warnings, reassignment context, limited exports where authorised. |
| `COMPLIANCE` | Champion/challenger governance panels, harm indicators, suppression and protected-population metrics, evidence-linked reports. |
| `PROCESS_DESIGNER` | Simulation outputs, rule/policy impact summaries, champion/challenger setup results, process-performance indicators. |
| `AGENT` | No general MI dashboard by default. Agent-facing performance or worklist metrics must be separately designed and role-gated. |

Access must follow the RBAC matrix. Aggregate-to-case drilldown is permitted only when the user also has permission to see the underlying case or account data.

---

## 4. Navigation

The reporting surface is part of the main staff frontend, not a separate BI application.

Initial routes:

| Route | Purpose |
|---|---|
| `/reporting/executive` | Executive KPI dashboard and trend view. |
| `/reporting/operations` | Queue health, throughput, SLA, and operational risk. |
| `/reporting/champion-challenger` | Experiment performance, harm monitoring, policy-gate evidence, and promotion readiness. |
| `/reporting/dca` | DCA placement, recall, recovery, commission, and partner-performance metrics. |
| `/reporting/exports` | Request, schedule, download, and audit controlled MI exports. |
| `/reporting/data-health` | Projection freshness, reconciliation status, and rebuild/replay visibility. |

Dashboard navigation must make data status visible before the user drills into details. A dashboard with stale or failed projections must not look normal.

---

## 5. Dashboard Definitions

### 5.1 Executive Dashboard

Primary audience: `SRO`, `OPS_MANAGER`, `COMPLIANCE`.

Purpose:

- show whole-service health
- surface high-level risks
- provide trend and comparative views without case-level operational detail by default

Required content:

| Area | Content |
|---|---|
| KPI summary | Total debt stock, active accounts, collections, recoveries, write-offs, suspended accounts, unresolved exceptions. |
| Trend panels | Month-on-month and week-on-week movement for collections, active stock, arrangement performance, queue backlog, and suppression volume. |
| Operational risk | SLA breach volume, aged work items, failed handoffs, stale communications, DCA recall exceptions. |
| Strategy summary | Active strategy versions, active policy bundles, champion/challenger experiments, promotion gate status. |
| DCA summary | Active placements, collections by agency, recalls, failed acknowledgements, commission estimates. |
| Data status | `as_of`, last successful refresh, next expected refresh, projection health state, and active warning banners. |

The executive dashboard should default to aggregate data and provide drilldown into lower-level dashboards rather than directly to case lists.

### 5.2 Operations Dashboard

Primary audience: `OPS_MANAGER`, `TEAM_LEADER`.

Purpose:

- manage daily operational pressure
- identify work queues needing intervention
- support reassignment and escalation decisions

Required content:

| Area | Content |
|---|---|
| Queue health | Open work items by queue, priority, age band, SLA status, team, and assignment state. |
| SLA warnings | Approaching breach, breached, paused, and escalated items. |
| Throughput | Created, completed, reassigned, reopened, and cancelled work items by period. |
| Agent/team view | Role-gated workload, availability, capacity, and completion indicators. |
| Exception pressure | Failed process handoffs, held communications, manual review items, failed DCA placements. |
| Drilldowns | Queue-level and team-level detail, with case/account lists only where RBAC permits. |

The operations dashboard complements the work-allocation supervisor dashboard. It gives MI context and trend views; it must not become a second work item command surface unless explicitly designed.

### 5.3 Champion/Challenger Dashboard

Primary audience: `COMPLIANCE`, `OPS_MANAGER`, `PROCESS_DESIGNER`, `SRO`.

Purpose:

- show experiment performance
- protect customers from harmful changes
- support promotion, rollback, or continuation decisions

Required content:

| Area | Content |
|---|---|
| Experiment header | Experiment key, champion version, challenger version, policy version, start date, planned end date, assignment ratio, current lifecycle state. |
| Eligibility policy | Active inclusion/exclusion predicates, protected-population handling, vulnerable-customer treatment rule, and policy approval reference. |
| Outcome panel | Collections, arrangement setup, payment adherence, failed contact, complaint indicators, manual-review volume, and suppression volume. |
| Harm panel | Breathing-space impacts, vulnerability-related outcomes, complaint spikes, failed communications, broken-arrangement rate, escalation rate. |
| Protected-population panel | Metrics for populations excluded from A/B comparison under the active policy, including current vulnerable-customer outcome metrics. |
| Statistical readiness | Sample size, duration, confidence notes, minimum threshold status, and gate failures. |
| Governance actions | Evidence links, simulation links, approval status, promotion recommendation state, rollback status. |

Important rule:

Protected-population inclusion is policy-driven. The current approved policy assigns vulnerable customers to champion treatment and excludes them from A/B comparison analytics while reporting their outcomes separately. A future policy may change that rule only through the configured policy approval path and required DWP sign-off.

### 5.4 DCA Performance Dashboard

Primary audience: `OPS_MANAGER`, `COMPLIANCE`, `SRO`.

Purpose:

- monitor third-party debt collection performance
- identify recall, acknowledgement, commission, and reconciliation issues

Required content:

| Area | Content |
|---|---|
| Placement summary | Active placements, new placements, recalls, returned accounts, failed handoffs. |
| Agency performance | Recoveries, payment adherence, acknowledgement SLA, recall SLA, exception volume, complaint indicators. |
| Financial view | Collections, commission estimates, reconciliation exceptions, period-end status. |
| Compliance view | Placement exclusions, vulnerable-party handling, notice evidence, recall evidence, unresolved disputes. |
| Drilldowns | Agency, cohort, placement batch, and case/account lists subject to RBAC. |

DCA dashboard values remain policy-gated until DCA notice, acknowledgement, recall, and commission policies are confirmed.

---

## 6. Drilldown Rules

All dashboards use a consistent drilldown model:

1. KPI tile or trend line
2. filtered aggregate breakdown
3. segment, queue, agency, or policy-version breakdown
4. case/account list only where RBAC and data classification permit
5. case/account detail through the normal operational screen, not through a reporting-only view

Every drilldown view must show:

- source dashboard and KPI key
- applied filters
- data period
- `as_of` timestamp
- projection version or snapshot identifier where available
- target/threshold version where applicable

Vulnerability and other sensitive attributes must not be exposed in drilldowns unless the user role is authorised and there is a clear operational need.

---

## 7. Export Centre

The export centre provides controlled report generation and download. It is not a raw database extract tool.

Export types:

| Export type | Purpose |
|---|---|
| Standard MI export | Predefined operational or executive report with approved columns and filters. |
| Dashboard snapshot | Point-in-time summary of a dashboard, including filters, targets, freshness, and warning state. |
| Champion/challenger evidence export | Experiment outcome, policy version, simulation references, gate status, and harm indicators. |
| DCA performance export | Agency performance and reconciliation summary for an approved period. |

Each export record must capture:

- export key and version
- requested by
- generated by
- generated at
- source projection version or snapshot identifier
- data period
- filters
- row count
- file hash
- classification
- retention category
- access log

Exports must be auditable. Downloading an export is itself an auditable action.

---

## 8. Data Freshness and Projection Health

Every dashboard must display data status.

Required status fields:

| Field | Meaning |
|---|---|
| `as_of` | The newest source event or snapshot represented in the dashboard. |
| `last_successful_refresh_at` | Last time the projection refreshed successfully. |
| `next_expected_refresh_at` | Next scheduled or expected refresh. |
| `freshness_state` | `FRESH`, `DELAYED`, `STALE`, `REBUILDING`, or `FAILED`. |
| `projection_version` | Projection implementation or schema version where available. |
| `source_period` | Reporting period represented by the view. |

Dashboard behaviour:

| State | UX requirement |
|---|---|
| `FRESH` | Show normal data status. |
| `DELAYED` | Show a warning banner and keep data visible. |
| `STALE` | Show prominent warning and disable export unless explicitly authorised. |
| `REBUILDING` | Show last known data with clear rebuild status, or empty rebuilding state if no usable data exists. |
| `FAILED` | Show failure banner, last successful refresh, and route to data-health details. |

Dashboards must not silently hide missing or stale data.

---

## 9. Reconciliation and Data-Health UX

The `/reporting/data-health` view gives authorised users confidence that MI projections are trustworthy.

Required content:

| Area | Content |
|---|---|
| Projection list | Projection key, owner module, refresh cadence, current status, last success, last failure. |
| Reconciliation checks | Source event count, projection row count, expected aggregate totals, mismatch count, tolerance status. |
| Replay/rebuild status | Current rebuild jobs, queued replay jobs, last completed replay, failed replay reason. |
| Dashboard impact | Dashboards and exports affected by a stale or failed projection. |
| Evidence | Reconciliation run identifier, timestamp, check version, result, and operator action where applicable. |

Projection reconciliation failures are operational incidents, not user-facing calculation disputes. The UI should route users to affected dashboards and explain whether data is delayed, incomplete, or unavailable.

---

## 10. KPI Targets and RAG Thresholds

RAG thresholds and KPI targets must be configurable and effective-dated. They must not be hardcoded into frontend components.

Target records should include:

| Field | Purpose |
|---|---|
| `kpi_key` | Stable KPI identifier. |
| `scope_type` | Global, team, queue, agency, segment, or experiment. |
| `scope_id` | Optional scoped identifier. |
| `effective_from` / `effective_to` | Date-effective target period. |
| `amber_threshold` | Threshold for warning state. |
| `red_threshold` | Threshold for critical state. |
| `direction` | Whether higher or lower values are better. |
| `owner_role` | Role accountable for target definition. |
| `approval_reference` | Policy/config approval link where required. |

Changing a KPI target affects future interpretation only. Historic dashboard snapshots and exports must preserve the target version used at generation time.

---

## 11. Frontend States

Each dashboard and export view must handle:

- loading
- empty data
- partial data
- stale data
- failed projection
- no permission
- export pending
- export failed
- export expired
- no drilldown permission
- target missing
- policy version missing

Empty states must distinguish "no data exists for this filter" from "data is not available because the projection is delayed or failed".

---

## 12. Evidence and Audit Boundaries

MI dashboards are decision-support tools. They are not the source of legal truth.

Boundary rules:

- account and customer histories remain the authoritative operational and audit records
- release evidence packs remain the authoritative evidence for releases and Class A changes
- simulation outputs are controlled evidence attachments when linked to an approval
- dashboard snapshots and exports are report evidence only for their generated period and filters
- logs, metrics, and traces are operational diagnostics, not durable audit evidence

---

## 13. Phased Roadmap

### Phase 1 - Reporting Shell and Core Dashboards

Deliver:

- `/reporting` navigation and RBAC route guards
- executive dashboard MVP
- operations dashboard MVP
- shared filter bar and date-period controls
- freshness badges and warning banners
- read-only drilldown to aggregate breakdowns

Acceptance criteria:

- dashboards read only from reporting projections
- every dashboard shows `as_of` and freshness state
- unauthorised users cannot access dashboards or drilldowns
- stale data is visibly marked

### Phase 2 - Export Centre and Data Health

Deliver:

- `/reporting/exports`
- standard MI export request/download flow
- export metadata and access audit
- `/reporting/data-health`
- projection freshness and reconciliation view
- export blocking or warning behaviour for stale projections

Acceptance criteria:

- exports record source projection, filters, period, row count, file hash, and requester
- export download is auditable
- projection failures are visible to authorised users
- affected dashboards are clearly identified

### Phase 3 - Champion/Challenger and DCA Dashboards

Deliver:

- champion/challenger dashboard
- protected-population policy panel
- harm-indicator panel
- promotion-readiness view
- simulation and evidence links
- DCA performance dashboard

Acceptance criteria:

- champion/challenger MI uses the active policy version to determine comparison populations
- vulnerable-customer outcomes are visible in the protected-population panel under the current approved policy
- promotion gates display sample-size, duration, harm, and evidence status
- DCA metrics remain clearly marked where policy values are provisional

### Phase 4 - Remaining Domain Dashboards

Deliver after the relevant domain designs exist:

- complaints MI
- legal/enforcement MI
- finance operations MI
- retention/archive MI
- manual corrections/amendments MI

Acceptance criteria:

- no dashboard is built ahead of its authoritative domain design
- each new dashboard follows the same freshness, reconciliation, target, export, and drilldown rules

---

## 14. Open Questions

| Area | Open question |
|---|---|
| Export catalogue | Which standard MI exports are contractually required by DWP, and which are optional operational reports? |
| Retention | What retention period and classification apply to each export type and dashboard snapshot? |
| KPI targets | Which initial KPI targets and RAG thresholds are DWP-approved seed values? |
| Refresh SLAs | Which dashboards require near-real-time projections and which can use scheduled snapshots? |
| Drilldown | Which roles may drill from aggregate MI into account lists, and under what data-classification limits? |
| DCA metrics | Which acknowledgement, recall, commission, and notice policies govern DCA performance reporting? |
| Future domains | What MI is required for complaints, legal/enforcement, finance operations, and retention/archive once those designs are complete? |

---

## 15. Relationship to Other Artefacts

| Artefact | Relationship |
|---|---|
| `domain-packs/reporting-analytics-read-model-domain-pack.md` | Owns projection architecture, KPI taxonomy, and query contracts. |
| `strategy-simulation-engine-design.md` | Provides simulation outputs and evidence links consumed by champion/challenger MI. |
| ADR-010 | Governs champion/challenger assignment, policy gating, and promotion. |
| ADR-014 | Governs champion/challenger analytics architecture and suppression interactions. |
| ADR-015 | Governs admin/configuration surfaces; reporting is adjacent staff UI, not a separate configurator app. |
| `RBAC-IMPLEMENTATION-DECISIONS.md` | Governs dashboard, export, and drilldown access. |
| `functionality-design-gaps.md` | Tracks which reporting/MI design gaps are closed and which remain policy-gated or domain-dependent. |
