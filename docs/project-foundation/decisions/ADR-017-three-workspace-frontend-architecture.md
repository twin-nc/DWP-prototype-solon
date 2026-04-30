# ADR-017: Three-Workspace Frontend Architecture

## Status

Accepted

## Date

2026-04-28

---

## Context

The DCMS serves three distinct user populations with fundamentally different concerns:

- **Frontline agents** (AGENT, SPECIALIST_AGENT, ESCALATIONS_AGENT) handling live debt collection cases under call conditions — they need speed, guided compliance, and low cognitive load
- **Supervisors and operations managers** (TEAM_LEADER, OPS_MANAGER, STRATEGY_MANAGER) overseeing portfolio performance, managing exceptions, and monitoring strategy outcomes
- **Configuration and governance users** (PROCESS_DESIGNER, COMPLIANCE, OPS_MANAGER for Tier 1) managing treatment strategies, rules, templates, and reference data through governed change workflows

These populations have different task structures, different data access requirements, different risk profiles, and — critically — different consequences when a user sees or acts on something outside their intended scope. A frontline agent who can navigate to configuration screens is an audit and compliance risk. A strategy manager who can initiate case-handling actions is an operational risk.

ADR-015 defined the Configuration Workspace (`/admin`) in detail. RBAC-IMPLEMENTATION-DECISIONS.md defined the full role set and permission matrix. Neither document formally established the frontend architecture as a whole — the three workspaces, the shared shell that contains them, and the routing model that enforces the separation.

A proposal document (`docs/project-foundation/proposed-three-workspace-model 1.md`) was reviewed against the design on 2026-04-28. Its structural model — three role-scoped workspaces within a shared shell — was accepted. Its Solon Tax references, separate Core Services, and Drools references were discarded as artefacts of a COTS integration context that does not apply to this project.

---

## Decision

### 1. Three workspaces within a single React SPA

The frontend is a single React application. Within it, three distinct workspaces are presented to users based on their Keycloak role claims:

| Workspace | Route prefix | Primary users |
|---|---|---|
| Case Worker Workspace | `/cases` | AGENT, SPECIALIST_AGENT, ESCALATIONS_AGENT |
| Operations Workspace | `/operations` | TEAM_LEADER, OPS_MANAGER, STRATEGY_MANAGER |
| Configuration Workspace | `/admin` | PROCESS_DESIGNER, COMPLIANCE, OPS_MANAGER (Tier 1) |

TEAM_LEADER has read-only access to both the Case Worker and Operations workspaces — they can view cases within their team scope and have full access to operational dashboards. COMPLIANCE has read-only access to the Configuration Workspace and read-only access to the Operations Workspace (audit evidence viewer). SRO, BACKOFFICE, FLOWABLE_ADMIN have no workspace access beyond their specific entitlements (FLOWABLE_ADMIN accesses `/admin/processes` only, per ADR-015).

### 2. Shared shell

All three workspaces are rendered within a shared shell that owns:

| Concern | Description |
|---|---|
| Authentication | Single Keycloak-backed OIDC session, valid across all workspaces. Token refresh handled by the shell. |
| Navigation | Role-based workspace routing. Users land in their primary workspace on login. The shell renders only the workspace tabs the user's role grants access to. |
| Notifications | Cross-workspace event surface — breach alerts, deployment confirmations, SLA breaches, and assignment notifications surface regardless of which workspace is active. |
| User context | Current user identity, role, team, and active workspace state, accessible to all workspace components. |
| Session lifecycle | Single logout, session timeout, idle detection. |

The shell is non-optional. Cross-workspace journeys occur in normal operations — a supervisor clicking from an Operations dashboard into a specific case, a strategy manager deploying a rule change and immediately navigating to Operations to observe live impact. These cannot be served without a shared session and shared routing. Treating the shell as optional guarantees it will not be built under delivery pressure, producing three inconsistent products with broken cross-workspace journeys.

### 3. Role-to-workspace routing

On login, the shell resolves the user's primary workspace from their JWT role claims and redirects them to it. The resolution order is:

1. If the user holds any of AGENT, SPECIALIST_AGENT, ESCALATIONS_AGENT → primary workspace is Case Worker (`/cases`)
2. Else if the user holds any of TEAM_LEADER, OPS_MANAGER, STRATEGY_MANAGER → primary workspace is Operations (`/operations`)
3. Else if the user holds any of PROCESS_DESIGNER, COMPLIANCE → primary workspace is Configuration (`/admin`)
4. Else if the user holds FLOWABLE_ADMIN → route directly to `/admin/processes`
5. Else → display an access-denied screen; do not default silently to any workspace

Users with multi-role assignments (e.g., OPS_MANAGER who also holds TEAM_LEADER) land at the higher-priority workspace. Navigation tabs for accessible workspaces are shown; tabs for inaccessible workspaces are not rendered.

Role enforcement is two-layer: the React shell hides navigation and route components based on decoded JWT claims (UX layer), and Spring Security enforces access at every API endpoint (authoritative enforcement layer). The React layer is a convenience; a user who manipulates client-side routing must still be rejected at the API.

### 4. Case Worker Workspace scope

The Case Worker Workspace is the runtime surface for frontline debt collection. It provides:

- Account and case view: timeline, debt balance, status, linked accounts, restriction flags
- Identity verification panel
- Vulnerability and restriction flags panel
- Guided scripting surface (step-by-step, blocking on policy violations — enforced by Tier 2 DMN rules via the process engine)
- Income and expenditure capture with affordability-driven arrangement options
- Arrangement creation, amendment, and recording
- Permitted communications recording
- Notes and audit-visible case action recording
- Case transfer (for TEAM_LEADER and above)
- Worklist and queue view (own assignments)

**What this workspace does not expose:**
- Strategy, task definitions, or policy configuration screens
- Champion/challenger performance data
- MI and analytics dashboards
- Bulk reassignment or queue balancing operations
- Any `/admin` route

Data is served from the DCMS backend domain modules: `customer`, `account`, `workallocation`, `repaymentplan`, `payment`, `communications`, `strategy` (read-only for guided scripting decisions).

### 5. Operations Workspace scope

The Operations Workspace is the supervisory and analytical surface for oversight, intervention, and strategy monitoring. It provides:

- Top-level operational dashboard: queue volumes, SLA health, breach trends, arrangement health, active DCA placements
- Queue drill-down and balancing view
- Bulk reassignment and supervisor intervention interface
- Exception and specialist work queue management
- Champion/challenger strategy performance and promotion interface (STRATEGY_MANAGER and OPS_MANAGER read; STRATEGY_MANAGER can trigger promotion)
- DCA performance and reconciliation reporting
- Audit and compliance evidence viewer (COMPLIANCE, OPS_MANAGER, TEAM_LEADER scoped)
- MI and analytics report suite
- Management report export
- Case drill-down from aggregate views (read-only case view for supervisors, within team scope)

**What this workspace does not expose:**
- Core strategy authoring, BPMN/DMN authoring, or template management
- Tier 1/2/3 configuration write access
- Standard frontline case-handling actions (notes, arrangement capture, guided scripting)

Data is served from the DCMS backend domain modules: `workallocation`, `strategy`, `account` (read-model), `repaymentplan`, `audit`, `integration` (DCA data).

### 6. Configuration Workspace scope

The Configuration Workspace is the `/admin` route tree defined in full in ADR-015. Summary:

- `/admin/foundations` — Tier 1 reference data (OPS_MANAGER write; TEAM_LEADER, STRATEGY_MANAGER, PROCESS_DESIGNER read)
- `/admin/rules` — Tier 2 DMN decision tables (PROCESS_DESIGNER propose; COMPLIANCE approve; STRATEGY_MANAGER, OPS_MANAGER, TEAM_LEADER read)
- `/admin/processes` — Tier 3 BPMN definitions and embedded Flowable Modeler (PROCESS_DESIGNER propose; COMPLIANCE approve; FLOWABLE_ADMIN operational view)
- `/admin/templates` — Communication template management (PROCESS_DESIGNER draft/submit; COMPLIANCE approve)
- `/admin/policies` — Cross-tier policy bundles (ADR-009)

Full role-scoped visibility rules are in ADR-015 §5.

### 7. Implementation approach

For the tender demo and initial delivery: **single SPA with workspace routing** (this ADR). The three workspaces are route subtrees within one React build. This is the correct choice for a 6-7 person team.

For production with separate delivery teams (12+ developers, one team per workspace): **micro-frontend shell** becomes the appropriate evolution. Each workspace becomes an independently deployable module behind a shared shell host. This is a future decision gate, not a current commitment — it follows the same staged-extraction principle as ADR-007 for the backend.

The portal-launcher model (three separate SPAs with no shared session) is explicitly rejected. It cannot serve cross-workspace journeys and produces three separate authentication contexts.

---

## Consequences

1. The React application gains two new top-level route trees: `/cases` (Case Worker) and `/operations` (Operations). The `/admin` tree (Configuration) is already defined in ADR-015.

2. A shared shell component owns authentication state, navigation rendering, notification subscription, and user context. All workspace components consume these from the shell via React context or equivalent — they do not manage their own auth state.

3. The workspace routing logic (section 3) must be implemented in the shell's login callback handler before any workspace-specific work begins. An incorrect routing implementation would silently land users in the wrong workspace.

4. The Operations Workspace introduces the most net-new frontend surface — queue dashboards, C/C results, supervisor intervention, DCA reporting, MI reports. This is the largest frontend build scope outside of the Case Worker Workspace case handling screens. It should be scoped and sequenced explicitly in the delivery plan.

5. Cross-workspace navigation (supervisor clicking from Operations into a specific case) requires the shell router to accept a deep-link parameter — e.g., navigating to `/cases/{accountId}` from within an Operations screen. The shell handles the route transition; the destination workspace component handles the data fetch. No special "handoff" protocol is needed beyond standard React Router navigation.

6. The champion/challenger promotion action (STRATEGY_MANAGER triggering a champion swap) is an API call to the backend `strategy` domain, not a Tier 2 configuration change. It does not enter the DMN approval workflow. It emits a `CC_PROMOTION` audit event. The Operations Workspace hosts the promotion UI; the `/admin/rules` screen shows the resulting state change (read-only) to users with access.

7. Notifications are a shell concern. The backend emits events (breach alerts, deployment confirmations, SLA warnings, assignment notifications) that the shell subscribes to — via Server-Sent Events or WebSocket. The notification surface must be implemented in the shell, not duplicated per workspace.

---

## Alternatives Rejected

### Three separate React applications

Building separate frontend applications for each workspace would require three build pipelines, three Nginx configs, three Kubernetes ingress rules, and three separate authentication contexts. Users with multi-workspace access (TEAM_LEADER who spans Case Worker read and Operations full access; COMPLIANCE who spans Operations read and Configuration approval) would need to authenticate separately or navigate between applications to complete normal workflows. Cross-workspace journeys (the supervisor drill-down, the strategy manager deploy-then-observe flow) would break at application boundaries.

Rejected on the same grounds as the three-configurator-applications model in ADR-015.

### Case Worker and Operations as one combined workspace

Merging Case Worker and Operations into a single "operational" workspace with role-based screen hiding was considered as a simplification. Rejected because:

- The distinction is not just about which screens are visible — it is about cognitive load and task structure. Frontline agents work case-by-case under call conditions. Supervisors work across portfolios and exceptions. Presenting both task structures in one surface makes the agent's experience noisier and harder to use without adding value.
- The "what users cannot do" constraints (agents cannot see C/C data, strategy KPIs, or bulk reassignment controls) are compliance guardrails, not just UX preferences. Enforcing them via screen hiding within a combined workspace is harder to audit than routing enforcement at the workspace boundary.

---

## Role-to-Workspace Access Summary

| Role | Case Worker | Operations | Configuration (`/admin`) |
|---|---|---|---|
| AGENT | Full access | No access | No access |
| SPECIALIST_AGENT | Full access | No access | No access |
| ESCALATIONS_AGENT | Full access | No access | No access |
| TEAM_LEADER | Read-only (own team cases) | Full access | Read-only (Foundations, Rules) |
| OPS_MANAGER | No access | Full access | Full write (Tier 1); read-only (Rules, Processes, Templates) |
| STRATEGY_MANAGER | No access | Full access (C/C + promotion) | Read-only (Rules only) |
| SRO | No access | No access | No access |
| COMPLIANCE | No access | Read-only (audit evidence viewer) | Read + approve (Rules, Processes, Templates, Policies) |
| BACKOFFICE | No access | No access | No access |
| PROCESS_DESIGNER | No access | No access | Submit/propose (Rules, Processes, Templates, Policies); read-only (Foundations) |
| FLOWABLE_ADMIN | No access | No access | Processes section only |

---

## Demo Flow Mapping

The following maps the 14 tender demo flows to their primary and supporting workspaces, adopted from the reviewed proposal.

| Flow | Primary workspace | Supporting workspace |
|---|---|---|
| 1 — Intake to first contact | Operations (supervisor end-state dashboard) | Case Worker (agent worklist and login) |
| 2 — Vulnerability to resolution | Case Worker | Operations (audit trail view) |
| 3 — Breach to DCA placement | Case Worker + Operations | — |
| 4 — Complex household | Case Worker | — |
| 5 — Strategy change without IT | Configuration | Operations (live impact observation) |
| 6 — Executive dashboard | Operations | Configuration (strategy version and C/C link) |
| 7 — Self-service to agent handoff | Case Worker | — |
| 8 — Dispute | Case Worker | — |
| 9 — Deceased | Case Worker | — |
| 10 — Write-off and reactivation | Case Worker | Operations (supervisor approval) |
| 11 — New agent | Case Worker | Operations (supervisor coaching view) |
| 12 — Regulatory audit | Operations | Configuration (deployment and version records) |
| 13 — Month-end surge | Operations | Case Worker (queue and worklist view) |
| 14 — Settlement offer | Case Worker | Operations (delegated authority approval) |

---

## References

- ADR-007: Single deployable monolith — one backend, one frontend; this ADR defines how the frontend is structured internally
- ADR-015: Configuration Workspace (`/admin`) — full definition of the Configuration Workspace surface and role-scoped visibility
- ADR-010: Champion/challenger experimentation — C/C mechanics that the Operations Workspace surfaces for STRATEGY_MANAGER
- ADR-009: Policy layer and cross-tier change bundles — the `/admin/policies` screen
- RBAC-IMPLEMENTATION-DECISIONS.md: Full role set, permission matrix, write-off tier model, STRATEGY_MANAGER design rationale (section 1.3)
- `docs/project-foundation/proposed-three-workspace-model 1.md`: Reviewed input; structural model accepted, Solon Tax and Drools references discarded
