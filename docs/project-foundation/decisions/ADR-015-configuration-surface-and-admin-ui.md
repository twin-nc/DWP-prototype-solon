> ## ⚠ STATUS: UNDER REVIEW — PLATFORM PIVOT IN EFFECT
>
> A confirmed programme-level decision (ADR-018, 2026-04-30) has pivoted the project to build on top of **Solon Tax** as the base platform. All prior ADRs are under review.
>
> **This document contains accumulated knowledge and analysis that remains relevant to the new design process. It does NOT represent the current or preferred direction.** All decisions and conclusions herein are open for renewed debate. Do not treat this ADR as a directive, default position, or settled constraint.
>
> See [ADR-018](ADR-018-platform-pivot-solon-tax-confirmed.md) for the full pivot record.

---

# ADR-015: Configuration Surface and Role-Scoped Admin UI

## Status

Accepted

## Date

2026-04-27

## Context

ADR-008 defined a three-tier configurability architecture — Tier 1 (Foundational reference data, OPS_MANAGER editable, no-code), Tier 2 (Behavioural DMN decision tables, PROCESS_DESIGNER submit, COMPLIANCE approve), and Tier 3 (BPMN process definitions, PROCESS_DESIGNER submit, Class A gate, COMPLIANCE approve). ADR-009 defined the policy layer: cross-tier change bundles with a single effective date, approval workflow, and rollback.

ADR-008 §9 sketched a conceptual UI surface for each tier but explicitly deferred resolution of the following questions:

1. Whether the three tiers are presented in one unified admin UI or as separate applications.
2. How the UI enforces role-scoped access — what each role sees and can act on.
3. Where the Flowable Modeler (embedded BPMN editor) fits — embedded within the admin UI or accessed as a separate application.
4. How domain-specific configuration screens (DCA rules, SLA thresholds, template management, queue routing DMN) are organised within the configuration surface.
5. What the URL and navigation structure is.

A gap analysis conducted 2026-04-27 confirmed that the following domain-specific configuration surfaces are needed and have not been placed in any tier or screen:

- **I&E schema** — income categories, expenditure categories, standard living allowances. Tier 1/2 boundary: categories are Tier 1 definitional; allowance thresholds are Tier 2 behavioural.
- **DCA placement rules** — eligibility criteria for debt placement with external collection agencies, agency register. Tier 1/2 boundary: agency register is Tier 1; placement eligibility rules are Tier 2 DMN.
- **Communication template management** — template CRUD, channel-specific content variants, approval workflow. Tier 2 in governance weight but not a DMN artefact; carries user-visible text content rather than decision logic.
- **Queue routing rules** — skill-based routing configuration, SLA thresholds. Tier 2 DMN.
- **Vulnerability protocol rules** — what communications are suppressed per vulnerability level, routing overrides, review intervals. Tier 2 DMN (extends the vulnerability action matrix from ADR-008).

The gap analysis also proposed organising configuration across three separate applications — a "Process Configurator", a "Task Configurator", and a "Foundations Configurator" — drawing from a Solon Tax COTS context. That model assumes separate integration APIs and independent deployment units. This project is a Spring Boot monolith with a single React frontend; separate configurator applications would build three frontends where one is sufficient, fragment navigation for the same user population, and impose integration overhead with no benefit.

A further question arose from ADR-008's governance table: TEAM_LEADER's level of access to the admin UI was not explicitly settled, and the placement of the ADR-009 policy bundle screen within the navigation was unspecified.

## Decision

### 1. Single unified admin UI

There is one admin section within the existing React application, accessible at the route `/admin`. This is not a separate application, not an embedded iframe to a separate deployment, and not three separate frontends. All configuration surfaces across Tier 1, Tier 2, Tier 3, and Templates are within this single route tree.

Role-scoped navigation controls what each user sees. Navigation items render or hide based on the Keycloak JWT role claims present in the bearer token. A user who has no write or read entitlement for a section does not see it in the nav. A user who has read-only access sees the section as non-editable.

### 2. Navigation structure

Four top-level sections under `/admin`, plus a cross-cutting policies screen:

**Foundations** (`/admin/foundations`) — Tier 1

Reference data managed by OPS_MANAGER. Screens in this section:

- Vulnerability types (code, label, severity level, regulatory basis, active flag)
- Debt categories (code, label, recovery priority)
- Action codes (code, label, action type, escalation requirement, approval authority)
- Suppression reasons (code, label, override permission)
- Channel configuration (per environment: enabled/disabled, retry settings)
- I&E schema (income categories, expenditure categories, standard living allowances by household type)
- DCA agency register (agency code, name, placement eligibility tier, status)

OPS_MANAGER has create, modify, and retire access. All other roles with access to this section are read-only.

**Rules** (`/admin/rules`) — Tier 2

DMN decision tables authored and versioned by PROCESS_DESIGNER, approved by COMPLIANCE. Screens in this section:

- Vulnerability action matrix (vulnerability level × action code → allowed/blocked/escalated)
- Channel routing rules (customer circumstances × channel → available/unavailable)
- Breach thresholds and tolerances (account type × arrangement status → tolerance period, escalation action)
- Work allocation and queue routing rules (debt segment × priority × skill profile → agent pool, SLA threshold)
- DCA placement eligibility rules (account attributes → eligible/ineligible, agency assignment)
- Communication channel preference rules (customer profile → channel priority order)
- Vulnerability protocol rules (vulnerability level → suppression scope, routing, review interval)

Each screen supports: view current version, compare versions, request approval, champion/challenger configuration (where applicable), and bulk test against account batch. PROCESS_DESIGNER can propose and submit. COMPLIANCE can review and approve. All other roles with access are read-only.

Rule simulation is surfaced from this section as `/admin/rules/simulations`. Phase 1 simulation supports deterministic current-vs-proposed DMN evaluation against selected account cohorts and can attach evidence to rule approval requests.

**Processes** (`/admin/processes`) — Tier 3

BPMN process definitions, segment-to-process mapping, and the embedded Flowable Modeler. Screens in this section:

- Treatment process definitions viewer (per-segment: active version, definition key, in-flight instance count, version history)
- Segment-to-process mapping (segment code → process definition key, effective version pin)
- Process Action Library (governed catalogue of reusable Flowable service-task actions, delegate contracts, and input/output schemas)
- BPMN deployment management (pending deployments, AWAITING_IMPLEMENTATION queue, deployment history)
- Flowable Modeler (embedded component within this section — PROCESS_DESIGNER and FLOWABLE_ADMIN only)

Service task coverage validation runs on save in the modeler. Diagrams with uncovered service tasks enter AWAITING_IMPLEMENTATION state and cannot proceed to approval until all referenced service tasks are implemented and registered. PROCESS_DESIGNER submits; COMPLIANCE approves (Class A gate). FLOWABLE_ADMIN has operational access to this section only.

**Templates** (`/admin/templates`) — Template tier

Communication template management. Screens in this section:

- Template library (list by channel type: SMS, email, letter, in-app)
- Template detail and editor (content body, variable schema, channel variants)
- Template approval workflow (draft → review → approved → active → retired)
- Template version history

Templates are a distinct fourth section rather than a subsection of Rules because they carry user-visible content, not decision logic. Conflating them with DMN tables would mix content governance with rule governance and complicate PII classification. PROCESS_DESIGNER can draft and submit for review. COMPLIANCE can review and approve. Content editors (if provisioned) may draft only.

**Policies** (`/admin/policies`) — Cross-cutting

Policy-bundle simulation is surfaced from this section as `/admin/policies/{policyBundleId}/simulation` once Phase 2 of the Strategy Simulation Engine is implemented.

The policy bundle screen defined in ADR-009. Groups pending changes from any tier and the Templates section into a named policy bundle with a single effective date and a unified approval workflow. Not all changes require a policy wrapper — single-tier operational changes (e.g., a Tier 1 label correction or a standalone template update) proceed directly through their tier's workflow. Policy bundles are used when a coordinated multi-tier change must land atomically (e.g., a new vulnerability category in Tier 1, a new action matrix rule in Tier 2, and a revised template in Templates — all taking effect on the same date).

### 3. Flowable Modeler placement

The Flowable Modeler is embedded within the Processes section at `/admin/processes/modeler`. It is not a separate application and is not the Flowable Admin UI (which is a technical operations tool for FLOWABLE_ADMIN and is not user-facing in the governance sense).

Business users (PROCESS_DESIGNER) author BPMN using the embedded modeler component. The recommended integration is the Flowable JavaScript modeler library embedded as a React component. If the library licensing or integration complexity makes this impractical, an iframe embed of the Flowable Admin UI's modeler endpoint is acceptable as a fallback — but in either case the modeler must be accessed within the `/admin/processes` route, not as a standalone application.

Access to the modeler is restricted to PROCESS_DESIGNER and FLOWABLE_ADMIN. COMPLIANCE sees the resulting diagram (read-only) during the approval workflow but does not have modeler write access.

### 4. Template management as a distinct section

Template management is the fourth section in the admin UI rather than a subsection of Rules for the following reasons:

- Templates carry content (user-visible text), not logic. The approval concern is content governance (is the wording correct, is the tone appropriate, does it comply with plain-English standards), not rule governance (does the logic produce the right outputs under test).
- Template content may contain account-holder-addressable fields (e.g., `{{customer.firstName}}`, `{{account.totalBalance}}`). These fields are PII:RESTRICTED per STD-SEC-003. Mixing them into the DMN table UI would create a classification boundary within a single screen.
- Template versioning follows the same effective-dating pattern as Tier 2 (templates have an `effective_from` timestamp and a status lifecycle) but templates are not Flowable DMN artefacts and must not be deployed to the Flowable DMN engine.
- The approval workflow for templates is content review, not rule simulation. COMPLIANCE does not run bulk batch tests against templates; they review the content directly.

Template content is classified PII:RESTRICTED per STD-SEC-003 where it contains account-holder-addressable variable fields. The `content_body` column in `communication_template` must be marked accordingly in Flyway migrations.

### 5. Role-scoped visibility rules

The following table defines what each role can see and act on within the admin UI. "No access" means the route is not rendered and returns 403. "Read-only" means all edit controls are hidden or disabled and the role cannot call any state-changing API endpoint via the UI.

| Role | Foundations | Rules | Processes | Templates | Policies |
|---|---|---|---|---|---|
| AGENT | No access | No access | No access | No access | No access |
| ESCALATIONS_AGENT | No access | No access | No access | No access | No access |
| SPECIALIST_AGENT | No access | No access | No access | No access | No access |
| TEAM_LEADER | Read-only | Read-only | No access | No access | No access |
| OPS_MANAGER | Full write (Tier 1) | Read-only | No access | No access | Read-only |
| STRATEGY_MANAGER | No access | Read-only | No access | No access | No access |
| PROCESS_DESIGNER | Read-only | Submit/propose | Submit/propose (Class A) | Draft/submit | Submit/propose |
| COMPLIANCE | Read-only | Read + approve | Read + approve (Class A) | Read + approve | Read + approve |
| FLOWABLE_ADMIN | No access | No access | Processes section only (operational view + modeler) | No access | No access |
| SRO | No access | No access | No access | No access | No access |
| BACKOFFICE | No access | No access | No access | No access | No access |

Notes on this table:

- TEAM_LEADER read-only access to Foundations and Rules is for operational context — team leads need to understand what rules are active when managing their agents' work. They have no write access to any tier.
- OPS_MANAGER has no write access to Rules, Processes, or Templates. This is intentional: the Tier 1/Tier 2 boundary separates definitional authority (OPS_MANAGER) from behavioural rule authority (PROCESS_DESIGNER + COMPLIANCE).
- STRATEGY_MANAGER has read-only access to Rules for context when reviewing champion/challenger performance. They have no write access to any section. Their primary workspace is Operations (C/C dashboards and promotion controls), not the admin UI. The Rules read-only access exists so they can inspect the rule they are monitoring without switching to a different tool.
- COMPLIANCE cannot propose or draft changes in any section. Separation of duties: COMPLIANCE is an approval gate, not an author. This mirrors the write-off self-approval prohibition in ADR-004 and the general principle that the person who approves a change must not be the person who authored it.
- FLOWABLE_ADMIN has access to the Processes section only. This role is a technical operations role (Flowable engine management, deployment recovery, instance inspection) and has no need for Foundations, Rules, or Template governance screens.
- Role enforcement is implemented at two layers: React navigation (hides non-permitted items based on decoded JWT claims) and Spring Security (enforces method-level and endpoint-level access control). The React layer is a UX convenience; the Spring Security layer is the authoritative enforcement point. A user who manipulates client-side nav must still be rejected at the API.

### 6. Audit trail integration

All admin UI state changes write to `configuration_audit_log` as defined in ADR-008. This covers Tier 1, Tier 2, and Tier 3 changes.

Template changes write to a separate `template_audit_log` table with the same schema shape as `configuration_audit_log`, distinguished because templates are not configuration artefacts in the ADR-008 sense and must be separately queryable for content audit purposes (e.g., "show me every approved version of the breathing-space SMS template and who approved each one").

```
template_audit_log
  ├── id (UUID)
  ├── template_id (UUID) — FK to communication_template
  ├── action (VARCHAR) — "CREATE", "UPDATE", "SUBMIT", "APPROVE", "RETIRE", "ACTIVATE"
  ├── change_summary (JSONB) — field-level diff
  ├── performed_by (VARCHAR) — Keycloak user ID
  ├── approved_by (VARCHAR NULL) — Keycloak user ID
  ├── approval_reason (TEXT NULL)
  ├── effective_from (TIMESTAMP)
  ├── performed_at (TIMESTAMP)
  └── correlation_id (VARCHAR) — links to policy bundle if part of one
```

Every approval action in both tables records `approved_by`, `approval_reason`, and `effective_from`. These fields are mandatory on any `APPROVE` action; the API rejects approval requests that omit them.

### 7. New persistence artefact: communication_template

Template management requires one new table not defined in any prior ADR:

```
communication_template
  ├── id (UUID, primary key)
  ├── name (VARCHAR) — human-readable template name
  ├── channel_type (VARCHAR) — "SMS", "EMAIL", "LETTER", "IN_APP"
  ├── content_body (TEXT) — template text with variable placeholders; PII:RESTRICTED
  ├── variable_schema (JSONB) — declared variable names, types, and sources
  │   { "customer.firstName": "string", "account.totalBalance": "currency" }
  ├── version (INT) — auto-incrementing per template ID
  ├── status (VARCHAR) — ENUM: DRAFT, APPROVED, ACTIVE, RETIRED
  ├── effective_from (TIMESTAMP NULL) — when this version becomes active
  ├── created_by (VARCHAR) — Keycloak user ID
  ├── approved_by (VARCHAR NULL) — Keycloak user ID
  ├── created_at (TIMESTAMP)
  └── updated_at (TIMESTAMP)
```

This table is separate from `suppression_log` (which records suppression events, not templates) and from `communication_channel_config` (which holds channel-level operational settings, not content). Channel variants are represented as separate rows with distinct `channel_type` values sharing the same logical template name and version.

### 8. No separate configurator applications

The gap analysis's framing of "Process Configurator / Task Configurator / Foundations Configurator" as three separate applications is rejected. That model was designed for a Solon Tax COTS context with separately deployed modules and distinct integration APIs. In this project:

- We own the full stack. There is no API boundary between the frontend and the configuration backend that would justify separate deployments.
- The user population for all three tiers overlaps significantly (PROCESS_DESIGNER touches all three; COMPLIANCE reviews all three). Separate applications would require users to navigate between applications to complete a single policy bundle change.
- Building three frontends increases maintenance surface without providing isolation benefit.

All configuration surfaces live within the single admin frontend section at `/admin`.

## Consequences

1. The React frontend gains an `/admin` route tree. Navigation items are conditionally rendered based on the role claims in the decoded Keycloak JWT. The JWT is validated on every request by Spring Security OAuth2 Resource Server — client-side role rendering is a UX layer only.

2. Domain configuration screens for I&E schema, DCA agency register, DCA placement eligibility rules, communication template management, queue routing rules, and vulnerability protocol rules are each dedicated pages within the appropriate section. They do not require their own ADRs — their tier placement and governance model are resolved here.

3. The embedded Flowable Modeler requires a frontend integration. The Flowable JavaScript modeler library is the preferred integration. This is the only dependency on Flowable's frontend tooling in the React application. If this integration requires significant frontend engineering effort, it should be scoped as a separate frontend task with a spike to validate the embed approach before committing to it.

4. Template management introduces the `communication_template` table (schema in Decision §7 above) and the `template_audit_log` table (schema in Decision §6 above). Both require Flyway migrations. The `content_body` column in `communication_template` must be annotated as PII:RESTRICTED in the migration per STD-SEC-003.

5. The policy bundle screen defined in ADR-009 has a concrete UI home at `/admin/policies`. The ADR-009 data model (policy bundle, effective date, approval state) is unchanged; this ADR adds only the navigation placement.

6. COMPLIANCE approval workflows for Tier 2, Tier 3, and Templates are triggered by state transitions in the relevant database tables (`dmn_deployment.status`, `bpmn_deployment.status`, `communication_template.status`). Approval tasks are created in the `workallocation` domain as in-app tasks assigned to the COMPLIANCE role. There is no separate notification microservice. This aligns with the workallocation domain's responsibility for queue and task management.

7. The distinction between FLOWABLE_ADMIN (technical operations — Processes section only) and PROCESS_DESIGNER (governed authoring — Rules, Processes, Templates) is now explicitly enforced at the navigation and API layers. The Flowable Admin UI (the Flowable-native admin application) remains available to FLOWABLE_ADMIN for direct engine operations (process instance inspection, job recovery, deployment history) but is not the governed authoring surface for business users.

8. ADR-008's consequence 1 ("three separate UI modules required") is superseded by this ADR. The correct model is one `/admin` route tree with four top-level sections, not three separate modules.

9. TEAM_LEADER's read-only access to Foundations and Rules is a deliberate operational concession. Team leads need configuration context when coaching agents or investigating anomalies. They must not be able to modify configuration. This access level must be enforced at the API layer, not only in the UI.

## Alternatives Rejected

### Three separate configurator applications (Solon Tax model)

Building three separate frontend applications (Process Configurator, Task Configurator, Foundations Configurator) would replicate a design from a COTS integration context where separate applications correspond to separate integration APIs and deployment units. In this project, there is one monolith, one frontend, and one auth context. Three separate applications would:

- Require three separate build pipelines, three sets of Nginx configs, and three separate routing rules in Kubernetes ingress.
- Force users (especially PROCESS_DESIGNER and COMPLIANCE, who span all tiers) to authenticate separately or navigate between applications to complete a single cross-tier change.
- Create three separate places where role-scoped access must be enforced — tripling the attack surface for misconfiguration.
- Provide no isolation benefit because all three talk to the same backend services.

Rejected.

### Embedding template management in Tier 2 Rules

Folding template management into the Rules section (treating templates as another Tier 2 artefact) was considered because templates are subject to COMPLIANCE approval and effective-dating — the same governance pattern as Tier 2 DMN tables. Rejected because:

- Templates carry PII:RESTRICTED content (account-holder-addressable variable fields). Rendering them alongside DMN input/output conditions in the same screen would create a mixed-classification display that is harder to audit and harder to explain to a compliance reviewer.
- The approval concern for templates is content review (wording, tone, plain English), not rule simulation. The Tier 2 bulk test and champion/challenger tooling is irrelevant to template approval.
- Template versioning and the DRAFT/APPROVED/ACTIVE/RETIRED lifecycle is structurally similar to Tier 2 but requires different tooling (a text editor, not a decision table editor) and different display.

Rejected.

### Giving COMPLIANCE write access to any tier

COMPLIANCE was considered as a co-author role — allowing COMPLIANCE to both author and approve changes in order to reduce the number of steps in the approval chain. Rejected on separation-of-duties grounds. COMPLIANCE is an approval gate. Allowing the approver to be the author removes the independent review that the Class A gate and Tier 2 approval workflow are designed to provide. This mirrors the write-off self-approval prohibition in ADR-004: the person who proposes a change must not be the same person who approves it. If COMPLIANCE could author Tier 2 rules, a single person could construct and approve a rule change without independent review.

Rejected.

### Flowable Admin UI as the primary BPMN editing surface for business users

Using the Flowable Admin UI directly (rather than embedding the Flowable Modeler within the admin section) was considered because the Flowable Admin UI already includes a modeler component and engine management screens, reducing frontend integration work. Rejected because:

- The Flowable Admin UI is a technical operations tool. It provides direct access to the process engine without the governed workflow (AWAITING_IMPLEMENTATION state, service task coverage check, PROCESS_DESIGNER → COMPLIANCE approval chain) that Tier 3 changes require.
- FLOWABLE_ADMIN is a technical role. PROCESS_DESIGNER is a business role. Giving PROCESS_DESIGNER access to the Flowable Admin UI would expose engine internals (job queues, process instance management, deployment history in raw Flowable format) that are irrelevant and potentially hazardous to a business user.
- The governance audit trail for Tier 3 changes must be recorded in `configuration_audit_log` against the DCMS identity of the proposer and approver. The Flowable Admin UI does not write to this table.

Rejected. The Flowable Admin UI remains available to FLOWABLE_ADMIN for engine operations only.

### OPS_MANAGER having write access to Tier 2 Rules

Granting OPS_MANAGER write access to Tier 2 Rules (in addition to Tier 1 Foundations) was considered on the grounds that OPS_MANAGER understands the operational effect of behavioural rules and would reduce reliance on PROCESS_DESIGNER for routine rule adjustments. Rejected because:

- Tier 2 rules encode "if-then-else" decision logic in DMN. Authoring them requires understanding of DMN semantics, hit policies, and input/output schema compatibility with the deployed service task library. OPS_MANAGER is defined as a no-code operator; DMN authoring is low-code.
- The PROCESS_DESIGNER → COMPLIANCE approval chain for Tier 2 ensures that a specialist with rule-authoring capability proposes the change and a separate compliance function approves it. Introducing OPS_MANAGER as a third author role would require clarifying which author type's proposals take precedence and complicates the approval workflow.
- If OPS_MANAGER needs to initiate a rule change, the correct path is to raise a change request for PROCESS_DESIGNER to implement, not to author DMN directly.

Rejected.

## References

- ADR-004: Segment taxonomy and two-tier configurability (superseded in part by ADR-008)
- ADR-006: BPMN versioning and in-flight migration
- ADR-008: Three-tier configurability architecture (this ADR supersedes ADR-008 §9 and ADR-008 consequence 1)
- ADR-009: Policy layer and cross-tier change bundles (this ADR provides the UI home for the policy bundle screen)
- ADR-010: Champion/challenger architecture (Phase 1 DMN-only; Tier 2 Rules section hosts the champion/challenger configuration controls)
- RBAC-IMPLEMENTATION-DECISIONS.md: Operational case permissions (distinct from configuration governance permissions in this ADR)
- STD-SEC-003: Domain data classification — PII:RESTRICTED classification for vulnerability data and template content fields
- WAYS-OF-WORKING.md: Class A change gate definition
- Requirements: DW.2 (champion/challenger), DW.9 (no-code configurability), DW.16 (frequent changes without code deployment), DW.38 (behavioural rule governance), DW.39 (bulk rule testing), DW.42 (audit trail and traceability), DW.44 (effective-dating), UI.1–UI.30 (admin UI requirements)
