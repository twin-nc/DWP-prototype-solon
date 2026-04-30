# Architecture Decision Record — Business Control & Experience Layer

This document records the key architectural decisions made for the Business Control & Experience Layer of the DWP Debt Collection platform. Each record captures the context that drove the decision, the decision itself, the rationale, and the consequences that follow from it.

Decisions should be read in order. Later decisions build on earlier ones.

---

## ADR-001 — Adopt the Business Control & Experience Layer Model

**Status:** Accepted  
**Date:** 2026-04-27

### Context

The initial architecture description documented a four-component model: Solon Tax as execution core, a Configuration Tool as a thin configuration layer, Agentic AI as a developer toolbox, and a Case Worker UI sitting on Solon Tax. This model was appropriate for describing configuration tooling but understated the full scope that had already been designed.

The additional-layer-architecture.drawio had already evolved beyond this: it described Core Services owning queue distribution, SLA tracking, vulnerability enforcement, I&E capture, agent scripting, third-party collection orchestration, analytics, and contact orchestration — each with their own persistent storage. These are not configuration concerns. They are operational execution concerns that Solon Tax does not own.

The question was whether to treat the additional layer as primarily a configuration wrapper (thin, supplementary, replaceable) or as the primary business control and experience layer over Solon Tax (the main operational surface for all business-facing users).

### Decision

The additional layer is the **Business Control & Experience Layer**. It owns the complete business-facing operational experience: frontline case handling, supervisory oversight, analytics, third-party collection management, compliance management, strategy configuration, and policy governance.

Solon Tax remains the execution core and system of record. The Business Control & Experience Layer does not replace Solon Tax. It composes, governs, enriches, and presents Solon Tax capabilities through a coherent product surface.

### Rationale

- The Core Services already designed are not configuration services — they are operational execution services. The architecture had already implied the bigger model before the naming caught up.
- The demo flows require cross-domain orchestration that cannot be assembled from a thin UI: vulnerability enforcement, guided scripting, queue context, and I&E capture must all be available in a single agent journey. A configuration layer cannot provide this.
- The tender narrative is materially stronger: "We have built a control-and-experience layer over Solon Tax" is a defensible product position. "We have added a configuration tool" is a supplement to someone else's product.
- The Solon Tax API surface is confirmed at v2.1.0. The layer has a complete, stable API surface to compose from.

### Consequences

- The layer owns UX, Core Services, and persistent storage for the operational domain.
- Delivery scope is larger than a pure configuration tool. This must be managed explicitly in planning and estimation.
- The layer must define and maintain API and event contracts with Solon Tax. Changes to the Solon API surface are a dependency risk for the layer.
- Solon Tax upgrade planning must account for layer dependencies on the Solon API surface.
- The tender proposition must be framed around the full layer scope, not only the configuration tooling.

---

## ADR-002 — Case Worker Workspace Delivered in the Layer, Not on Solon Tax

**Status:** Accepted  
**Date:** 2026-04-27

### Context

The initial architecture described the Case Worker UI as sitting "on top of Solon Tax rather than on top of the Configuration Tool." This implied using or extending Solon Tax's native case management UI for frontline agents.

The question was whether to accept Solon Tax's native UI for frontline case handling, extend it, or replace it with a workspace delivered and owned by the Business Control & Experience Layer.

### Decision

The **Case Worker Workspace is delivered within the Business Control & Experience Layer**. It is not built on or extended from Solon Tax's native UI. It is a purpose-built workspace that composes data from Solon Tax APIs and from the layer's own Core Services.

### Rationale

A competitive frontline case worker experience for DWP requires data and behaviour that Solon Tax does not own:

- **Guided scripting** is owned by the Work, Queue & Agent Core Service — not Solon Tax
- **Vulnerability enforcement at point of action** is owned by the Risk, I&E & Compliance Core Service — not Solon Tax
- **Queue context and real-time SLA state** are owned by the Work, Queue & Agent Core Service — not Solon Tax
- **Contact history enriched with channel outcomes** is owned by the layer's Operational DB — not Solon Tax's contact records alone
- **I&E capture and affordability calculation results** are owned by the layer's Core Service and Operational DB — not Solon Tax

A Case Worker UI sitting on Solon Tax would still need to call all of these layer services, creating a UI that straddles two layers with no single team accountable for the agent experience. Policy enforcement decisions (blocking a non-compliant action, applying a vulnerability suppression) would span a Solon-owned UI and layer-owned services with no clear failure boundary.

Building on Solon's native UI also creates a maintenance liability: every Solon upgrade is a regression risk on custom extensions, and the UX paradigm is constrained by what Solon's UI framework supports.

### Consequences

- The layer delivery team owns the Case Worker Workspace end to end: UX, behaviour, accessibility, and performance.
- The workspace must implement graceful degradation when layer Core Services are unavailable: basic case viewing and action recording must survive without guided scripting or real-time queue state.
- WCAG 2.2 accessibility compliance for the Case Worker Workspace is the layer team's responsibility.
- Solon Tax's native case management UI is not used for frontline operations. If Solon Tax ships UI improvements to its case views, these do not automatically appear in the Case Worker Workspace.
- The delivery team must define and own the resilience contract for the workspace (see ADR-005 on the BFF).

---

## ADR-003 — Three-Workspace Model Within a Shared Product Shell

**Status:** Accepted  
**Date:** 2026-04-27

### Context

The additional-layer-architecture.drawio combined Agent Desktop, Supervisor Dashboard, MI & Analytics, Foundations Configurator, and Self-Service Portal Extensions into a single "Agent & Operations UI" block. This created two risks: role confusion (a frontline agent mentally inside a configuration-adjacent product) and governance risk (the same surface casually mixing case handling with RBAC editing).

Three options were considered:

1. Keep a single combined UI with internal access controls
2. Build three fully separate products with an optional shared design system
3. Build three role-based workspaces within a shared product shell

### Decision

Deliver **three distinct workspaces within a shared product shell**:

- **Case Worker Workspace** — frontline runtime experience
- **Operations Workspace** — supervisory, MI, queue, exception, and oversight experience
- **Configuration Workspace** — strategy, tasks, policy, and foundations configuration

Workspaces are role-separated by RBAC within the shared shell. They are not three separate deployable products.

### Rationale

A single combined UI creates role confusion and a difficult governance position. A frontline agent should not be navigating within a surface that also contains RBAC administration and strategy deployment. The access control complexity required to make this safe is higher than the complexity of simply separating the workspaces.

Fully separate products with an optional shared design system will not produce a coherent result under delivery pressure. The shared shell will never be built, three inconsistent products will emerge, and cross-workspace journeys will break at product boundaries.

The three-workspace model gives clean role separation while preserving cross-workspace journeys. A supervisor can drill from an Operations dashboard into a specific case. A strategy manager can move from a Configuration deployment to the Operations Workspace to observe live impact. These journeys require a shared product context, which the shared shell provides.

The demo story is also stronger: one product with three windows into the operation is more compelling than three separate tools that happen to be about the same platform.

### Consequences

- Each workspace has a defined scope, a defined user set, and a defined governance model. Product backlog must be structured per workspace to prevent scope bleed.
- Cross-workspace navigation must be designed and tested as a first-class scenario, not as an edge case.
- Role definitions must be explicit and enforced by Keycloak claims. Access to a workspace is not governed by trust or convention — it is enforced at the shell routing layer.
- The Configuration Workspace must be clearly separated from both runtime workspaces in the shell navigation model. A frontline user should have no path to configuration tooling.

---

## ADR-004 — Shared Product Shell Is Non-Optional

**Status:** Accepted  
**Date:** 2026-04-27

### Context

When the three-workspace model was first discussed, the framing included the phrase "optional shared design system or shell across all three." The question was whether the shell was a design consistency measure or a required architectural component.

### Decision

The **shared product shell is a first-class architectural commitment**. It is not optional and it is not a design system. It owns:

- Single Keycloak-backed authentication session valid across all workspaces
- Role-based workspace routing and navigation
- Cross-workspace notification surface
- Shared user context (identity, role, team, active workspace state)
- Single session lifecycle (login, logout, timeout, token refresh)

### Rationale

Cross-workspace journeys are present in normal operations and in the primary demo flows:

- A supervisor in the Operations Workspace navigating to a specific case requires crossing into the Case Worker Workspace within a single browser session. This requires shared routing and a shared session.
- A strategy manager observing live operational impact after a Configuration deployment requires moving from the Configuration Workspace to the Operations Workspace. That is a single user workflow that requires shared navigation.
- A breach alert generated by the Risk, I&E & Compliance Core Service must surface as a notification regardless of which workspace the supervisor currently has open. That requires a shared notification subscription and a shared notification surface.

Marking the shell as optional is equivalent to accepting that it will not be built. Under delivery pressure, optional infrastructure components are deferred and eventually dropped. The result is three inconsistent products with three separate authentication contexts and no cross-workspace navigation.

### Consequences

- The shell must be designed and delivered as a product component with its own scope, tests, and owner.
- For the tender demo, a single SPA with workspace routing is the pragmatic implementation choice.
- For production delivery with separate teams per workspace, a micro-frontend shell with independently deployable workspace modules is the preferred long-term architecture.
- The shell must be designed for workspace isolation: a runtime failure in one workspace module must not affect the availability of other workspaces.
- Route-based deep linking must work across workspace boundaries to support cross-workspace navigation from notifications and supervisor drill-downs.

---

## ADR-005 — Backend-for-Frontend (BFF) as a Required Architectural Component

**Status:** Accepted  
**Date:** 2026-04-27

### Context

Each workspace view requires data assembled from multiple sources simultaneously: Solon Tax APIs for case, process, task, and financial data; layer Core Services for queue state, scripting, vulnerability, and I&E; and layer storage for contact history and audit records. Without a server-side composition layer, this assembly must happen either in the browser or duplicated across multiple services.

### Decision

A **Backend-for-Frontend (BFF) composition service** is a required component of the Business Control & Experience Layer. It sits between the workspace UI layer and the Core Services and Solon API gateway.

The BFF is responsible for:

- Assembling per-workspace data responses from multiple upstream sources in a single server-side call
- Applying auth-scoped data filtering per role before returning data to the browser
- Optimising API call patterns for each workspace type (field selection, data shaping, pagination)
- Owning the composition contract between the workspace UI and the rest of the layer
- Implementing graceful degradation: returning partial responses when upstream services degrade, rather than failing the whole workspace

### Rationale

Loading a Case Worker Workspace account view requires: Solon case data, Solon task list, layer vulnerability flags, layer queue context, layer contact history, and layer scripting state — simultaneously. Without a BFF this is five or more parallel browser requests with inconsistent error handling, inconsistent auth token propagation, and no single point of control.

Auth-scoped data filtering belongs in a server-side component. A junior agent must not see data fields that are restricted to specialists. Enforcing this in the browser is not a secure architecture.

Workspace-specific API optimisation cannot be done cleanly without a layer that owns the composition contract. The BFF allows the UI to request exactly what each workspace screen needs, without exposing internal service granularity to the browser.

The BFF is also the natural place to implement resilience contracts. If the Agent Scripting Service is unavailable, the BFF returns a partial response with a degradation flag, and the Case Worker Workspace falls back to an unscripted view. The workspace remains available. Without a BFF, this logic is scattered across browser-side API calls.

### Consequences

- Each workspace should have a dedicated BFF module, or a clearly scoped route within a shared BFF service, to prevent workspace concerns from mixing.
- The BFF owns the interface contract between the workspace UI and the rest of the layer. Changes to Core Service APIs must be managed through the BFF contract, not exposed directly to the browser.
- The BFF must implement circuit breakers and partial response strategies for all upstream dependencies.
- The BFF is not a general-purpose API gateway. It is workspace-aware and role-aware. It must not be used as a pass-through proxy.
- Resilience contracts per workspace — which features degrade gracefully and which block — must be defined and documented as part of BFF design.

---

## ADR-006 — Layer Owns Its Own Databases; Solon Data Accessed Via API Only

**Status:** Accepted  
**Date:** 2026-04-27

### Context

The question was raised whether the layer's operational data — vulnerability records, queue state, I&E records, contact history, SLA timers, champion/challenger assignments, metrics time-series, third-party collection records, audit logs — could be stored in Solon Tax's databases rather than in layer-owned stores, to reduce infrastructure complexity.

### Decision

The **Business Control & Experience Layer owns and manages its own persistent storage**. It does not store data in Solon Tax's databases. Solon Tax data is accessed exclusively through Solon's published REST API. The layer's four owned databases are:

- **Strategy & Config DB** — strategies, step library, templates, deployment records
- **Operational DB** — vulnerability registry, C/C assignment DB, queue state DB, SLA timers DB, contact history DB, work item history, I&E records
- **Analytics & third-party collection DB** — KPI metrics, C/C results, third-party collection records and commission ledger
- **Audit & Compliance DB** — breathing space log, I&E history, fraud records

### Rationale

Several categories of layer data do not exist in Solon Tax at all: champion/challenger assignment records, strategy authoring artefacts, metrics time-series, third-party collection commission ledger, queue state, and SLA timers are layer concepts that Solon has no schema for. Storing these in Solon would require Solon to own data for concepts it does not execute. When Solon is upgraded, its migration scripts have no obligation to preserve layer-owned tables.

For data that overlaps with Solon records (vulnerability, I&E, contact history), the layer's version is materially richer. Solon's suppression record captures the enforcement outcome. The layer's vulnerability record captures the full clinical assessment: bureau data, specialist notes, review scheduling, review history, and affordability context. These are different things with different retention and access requirements.

Storing in Solon's databases would also:

- Create deployment coupling: a Solon database maintenance window takes down the layer
- Prevent independent scaling: analytical queries (metrics, C/C, KPI dashboards) run against Solon's transactional database and contend with live case processing
- Prevent independent backup and restore of the layer
- Give the Solon team schema ownership over layer data, creating a cross-team dependency on every layer data model change

The principle "Solon data accessed via API only" enforces clean bounded context separation in both directions. The layer calls Solon's APIs. Solon does not read the layer's databases.

### Consequences

- The layer team owns schema design, migration management, backup, and recovery for all four databases.
- Analytical queries run against layer-owned stores and do not contend with Solon's transactional workloads. The Metrics Time-Series data in the Analytics & third-party collection DB is populated by consuming Solon's Kafka event stream, not by querying Solon's operational DB.
- Where data exists in both Solon and the layer (e.g., a vulnerability suppression), ownership must be explicit: Solon owns the enforcement record (the suppression API entry); the layer owns the clinical assessment record that produced it. These are separate entities with separate lifecycles.
- The layer must not directly read or write Solon's database at any point. All integration is through Solon's published REST API surface and the Solon Kafka event stream (consume-only).
- The layer's four databases must be included in infrastructure planning, DR strategy, and capacity planning from the start of delivery.
