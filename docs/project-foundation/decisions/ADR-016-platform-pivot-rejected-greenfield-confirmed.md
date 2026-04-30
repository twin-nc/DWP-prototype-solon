# ADR-016: Platform Pivot to Solon Tax / Amplio Rejected — Greenfield Stack Confirmed

## Document History

| Version | Date | Author | Change |
|---|---|---|---|
| 1.0 | 2026-04-27 | Solution Architect | Initial decision — based on Amplio documentation only. Auth (SAML2/OIOSAML3) and DB (Oracle) cited as first two hard blockers. Camunda 7 EOL cited as additional decisive factor. |
| 2.0 | 2026-04-27 | Solution Architect | Revised following Solon Tax v2.3.0 documentation review. Auth blocker RESOLVED (Solon Tax v2.3.0 uses OAuth 2.0 / OIDC). DB blocker RESOLVED (Solon Tax v2.3.0 supports PostgreSQL). Camunda 7 EOL blocker RETIRED (Solon Tax v2.3.0 uses Amplio Process Engine, not Camunda 7). Five new hard blockers identified. Statutory dispatch risk (Blocker 3 from v1.0) carries forward unchanged and remains the primary disqualifier. Decision unchanged: Option C confirmed. |
| 2.1 | 2026-04-28 | Solution Architect | Clarified that the message-boundary-event analysis is a Solon/Amplio implementation feasibility finding, not a change to the accepted Flowable event-subprocess architecture. |

---

## Status

Accepted

## Date

2026-04-27 (v2.0 update: 2026-04-27)

## Context

### Original Assessment (v1.0)

A question was raised during design: could the DCMS be built on top of Solon Tax (which runs Amplio as its processing engine) rather than as a pure greenfield system? Amplio is a Netcompany case management platform used in Danish public sector projects.

The v1.0 assessment was conducted against Amplio documentation only. Solon Tax v2.3.0 documentation was not available at the time of the original decision. The v1.0 assessment characterised Amplio as using SAML2/OIOSAML3 for authentication, Oracle as its primary database, and Java 11. These characterisations were accurate for the Amplio documentation available.

### Re-Assessment Trigger (v2.0)

Solon Tax v2.3.0 documentation became available. A re-assessment was commissioned because the original ADR's first two hard blockers — authentication protocol incompatibility (SAML2) and database incompatibility (Oracle) — were cited as the primary grounds for rejection. Both are resolved in Solon Tax v2.3.0:

- Solon Tax v2.3.0 uses OAuth 2.0 / OIDC — the SAML2/OIOSAML3 blocker does not apply.
- Solon Tax v2.3.0 supports PostgreSQL — the Oracle-only blocker does not apply.

The re-assessment evaluated whether removing these two blockers changes the decision.

### Re-Assessment Method

A full feasibility assessment was conducted across three layers against Solon Tax v2.3.0:

1. **Delivery Designer** — mapped Solon Tax v2.3.0 capabilities against DCMS requirements to identify what is possible, what is not, and how a build-on-Solon-Tax architecture might be structured.
2. **Design Critic** — challenged the proposal adversarially against the accepted ADR baseline, regulatory obligations, and platform standards.
3. **Solution Architect** — reviewed both outputs and locked a final decision.

Three options were evaluated:

| Option | Description |
|---|---|
| A | Solon Tax as platform foundation — full DCMS built as Solon Tax custom-layer extensions |
| B | Solon Tax financial engine only — billing/payments/pay-plans adopted; process engine, auth, and frontend replaced |
| C | Greenfield as currently designed |

---

## Decision

**Option C is adopted. DCMS is built as a greenfield system on the approved stack. No part of Solon Tax is adopted as a platform dependency, process engine, financial engine, or structural component.**

The resolution of the authentication and database blockers from v1.0 does not change the decision. The statutory dispatch risk blocker (carried from v1.0 as Blocker 3) remains standing and is independently disqualifying. Five additional hard blockers are identified from Solon Tax v2.3.0 that did not appear in the v1.0 analysis. The net scope reduction available from any Solon Tax adoption path is 10–15% at most — insufficient to justify the integration risk, the statutory liability exposure, or the platform coupling cost.

---

## Rationale

### Blockers Resolved by Solon Tax v2.3.0 (carried from v1.0 — no longer applicable)

**v1.0 Blocker 1 — Authentication Protocol (RESOLVED)**

The original ADR identified SAML2/OIOSAML3 as a hard blocker because INT01 mandates OAuth 2.0 / OIDC for DWP SSO across 100+ domains. Solon Tax v2.3.0 uses OAuth 2.0 / OIDC. This blocker is resolved and does not carry forward. Keycloak 24 remains the adopted identity provider, satisfying INT01, INT02, INT03, and INT28 in the greenfield design.

**v1.0 Blocker 2 — Database Incompatibility (RESOLVED)**

The original ADR identified Oracle as a hard blocker because DCMS uses PostgreSQL-specific compliance-enforcement constructs (partial index `uix_suppression_log_active`, JSONB columns, `INSERT ... ON CONFLICT DO NOTHING`) and Flyway Community Edition does not support Oracle. Solon Tax v2.3.0 supports PostgreSQL. This blocker is resolved and does not carry forward.

**v1.0 Additional Factor — Camunda 7 End of Life (RETIRED)**

The original ADR cited Camunda 7 EOL (October 2025) as a decisive factor against Option B. Solon Tax v2.3.0 does not use Camunda 7; it uses the Amplio Process Engine. This factor is retired. The process engine question is addressed separately under Blocker 1 below.

---

### Hard Blocker 1 — Solon/Amplio Regulatory Event Semantics (ADR-002, statutory criminal liability)

This blocker carries forward from v1.0 Blocker 3, unchanged in substance. It is the primary disqualifier for both Option A and Option B.

This is a Solon implementation feasibility finding, not a change to the accepted Flowable architecture. ADR-002 requires regulated events such as `breathing-space-received`, `change-of-circumstances`, `payment-missed`, and `supervisor-override` to be handled inside the governed BPMN event-subprocess model so lifecycle state, auditability, and regulatory effects remain under process control. In a Solon/Amplio implementation, the closest native mechanism for reproducing the required concurrent handling semantics is message boundary event handling. The Amplio Process Engine (embedded in Solon Tax v2.3.0) supports interrupting message boundary events only; non-interrupting message boundary events are not implemented. This is architecturally incompatible with the breathing space regulatory model for the following reasons:

- An interrupting boundary event on the main collection process terminates that process instance when a breathing space message arrives. A terminated process instance cannot be resumed in BPMN semantics.
- A Solon-based DCMS would need the outcome provided by non-interrupting/concurrent handling semantics because the debt continues to exist; the moratorium suppresses or suspends prohibited activity for a defined period, after which the original lifecycle must resume safely with arrangement monitoring state intact.
- ADR-002 explicitly evaluated and rejected the application-layer Spring service approach (Option 3 in ADR-002) as the mechanism for achieving this effect. That rejection stands regardless of process engine choice.
- Using Solon Tax's native dispatch path during an active breathing space moratorium — even if routed through a custom suppression gate — creates criminal liability under the Debt Respite Scheme Regulations 2020 because Solon Tax's dispatch paths have no awareness of the `CommunicationSuppressionService` five-step priority sequence (ADR-014). Any attempt to route the dispatch path through a custom suppression gate reduces Solon Tax to a pass-through wrapper that contributes nothing and adds integration surface area.

Sending a debt collection communication during an active breathing space moratorium is a criminal offence under the Debt Respite Scheme Regulations 2020. This is not a feature gap. It cannot be resolved by configuration.

**This blocker independently disqualifies Options A and B.**

### Hard Blocker 2 — No Native DMN Engine (ADR-008, ADR-010)

Solon Tax v2.3.0 uses Drools KIE for rule evaluation. This is incompatible with two accepted ADRs:

- ADR-008 Tier 2 requires a DMN authoring UI usable by non-technical `PROCESS_DESIGNER` and `COMPLIANCE` role holders without code deployment. Drools DRL requires developer involvement for rule changes; no DMN-standard graphical editor is available in the Drools KIE toolchain.
- ADR-010 champion/challenger uses version-scoped DMN deployment routing — a named Flowable deployment is assigned to each experiment variant and the segment hash is used to route deterministically to the correct deployment. No equivalent mechanism exists in Drools KIE without full custom implementation, which negates any platform benefit.

Drools DRL is not a substitute for DMN. The OMG Decision Model and Notation standard is the basis for the Tier 2 non-technical authoring requirement. These are different standards with different toolchain ecosystems.

### Hard Blocker 3 — Java 17 vs Java 21 (CLAUDE.md mandate, ADR-014 compile-time safety)

CLAUDE.md mandates Java 21 (`eclipse-temurin:21`). Solon Tax v2.3.0 runs on Java 17.

This is not a minor version gap. ADR-014 relies on JEP 441 (exhaustive switch expressions with sealed interfaces, finalised in Java 21, preview-only in Java 17) to enforce at compile time that every `CommunicationCategory` value is handled by `CommunicationSuppressionService`. On Java 17, a new `CommunicationCategory` value added to the sealed interface compiles without error if a switch arm is omitted — the build-time safety guarantee is silently lost. The consequence is a statutory suppression gap reachable in production: a communication category added without a corresponding suppression rule would reach dispatch without statutory authority evaluation.

This is a statutory safety mechanism, not a code style preference.

### Hard Blocker 4 — Liquibase vs Flyway (CLAUDE.md mandate)

CLAUDE.md mandates Flyway Community Edition for database migrations. Solon Tax v2.3.0 uses Liquibase. These two tools maintain mutually exclusive migration history tables (`flyway_schema_history` vs `databasechangelog`). Running two migration tools against the same database produces unpredictable ordering and cannot be safely reconciled. Flyway cannot read or adopt Liquibase changesets.

For Option B (financial engine adoption), this blocker persists unless Solon Tax provides a one-time Flyway-compatible schema export and subsequently abandons Liquibase — an arrangement that is not offered and would constitute a non-standard integration support obligation on the Solon Tax programme.

### Hard Blocker 5 — Angular Frontend vs React + GOV.UK Design System (CLAUDE.md mandate)

CLAUDE.md mandates React + TypeScript + GOV.UK Design System. Solon Tax v2.3.0 uses Angular. No GOV.UK Design System component library exists for Angular. This is a full technology divergence, not a theming gap. DCMS must implement the GOV.UK Design System to meet UK public sector accessibility and design standards. The frontend must be built in React regardless of platform choice, meaning Solon Tax's Angular frontend contributes nothing.

### Hard Blocker 6 — Microservices vs Single Monolith (ADR-007)

ADR-007 mandates a single Spring Boot monolith with well-defined internal packages per domain. Solon Tax v2.3.0 is a microservices architecture. Adopting any structural part of Solon Tax requires either decomposing the DCMS into microservices (violating ADR-007 and invalidating the six-to-seven person team sizing that ADR-007 was written against) or extracting individual Solon Tax services as libraries (which they are not designed to be and which would require ongoing porting effort for every Solon Tax release).

### Hard Blocker 7 — OPA Authorisation vs RBAC via Keycloak JWT (CLAUDE.md mandate)

CLAUDE.md mandates RBAC via JWT claims from Keycloak 24. Solon Tax v2.3.0 uses Open Policy Agent (OPA) with Rego policies for row-level security and authorisation decisions. A split authorisation model — OPA for some paths, Keycloak JWT for others — creates security gaps at the boundary where token claims are not evaluated against OPA policies and vice versa. The authorisation model must be singular and consistent across all protected resources.

---

### Scope Assessment

The gross scope reduction estimate of 30–35% cited during initial Solon Tax consideration does not survive module-by-module accounting.

| DCMS Module | Solon Tax v2.3.0 Contribution |
|---|---|
| `customer` | None — vulnerability classification (FCA FG21/1), third-party authority, household/joint model are full custom |
| `strategy` | None — segmentation engine, champion/challenger (ADR-010), DMN decision tables (ADR-008 Tier 2) are full custom |
| `communications` | None — dispatch path is incompatible with CommunicationSuppressionService (Blocker 1); entire module is full custom |
| `integration` | None — DWP Place, DCA placement, DM6, WorldPay, bureau feeds are DWP-specific; full custom |
| `audit` | None — INSERT-only audit schema, COM06/COM07 mandatory fields are full custom |
| `user` | None — RBAC via Keycloak JWT (Blocker 7); Solon Tax's OPA model is incompatible |
| `account` | Partial scaffold — financial ledger structure has analogues, but compliance-critical constructs (partial index, JSONB, idempotent insert paths per ADR-014) require PostgreSQL-specific implementation confirmed safe under Flyway governance |
| `payment` | Partial scaffold — payment transaction structures have analogues, but CCA allocation order, refund and overpayment logic are full custom |
| `repaymentplan` | Partial scaffold — plan lifecycle structures have analogues, but breach handling, re-aging, direct debit integration are full custom |
| `workallocation` | Partial scaffold — queue structures have analogues, but skill-based routing, supervisor override, exception queue logic are full custom |

The realistic net scope reduction available from Option A (full Solon Tax adoption, after resolving all blockers) is **10–15%** of total delivery effort. This is the upper bound; the lower bound is closer to 10% because the `communications` module — which must be entirely replaced — accounts for significant delivery scope.

For Option B (financial engine only), the scope reduction after blocker resolution (Liquibase/Flyway remains) is **10–15% gross**, reduced further by the integration surface area cost of maintaining an upstream dependency on Solon Tax's financial data model across the programme lifetime.

Neither option offers scope reduction sufficient to justify the integration risk, the statutory liability exposure at Blocker 1, or the platform coupling cost across the 6–7 person team.

---

## Confirmed Stack

The following stack is confirmed and locked. No Solon Tax or Amplio component is introduced as a dependency at any layer.

| Layer | Choice | Mandate source |
|---|---|---|
| Language | Java 21 (OpenJDK via `eclipse-temurin:21`) | CLAUDE.md; ADR-014 (compile-time exhaustive switch enforcement) |
| Framework | Spring Boot 3.4.x | CLAUDE.md |
| Build tool | Maven 3.9.6+ | CLAUDE.md |
| Database | PostgreSQL 16 | CLAUDE.md; ADR-014 (partial index enforcement, JSONB audit columns) |
| DB migrations | Flyway Community Edition | CLAUDE.md |
| Process engine | Flowable (embedded) | ADR-001, ADR-002, ADR-003 |
| Identity Provider | Keycloak 24 | CLAUDE.md; INT01, INT02, INT03, INT28 |
| Auth protocol | OAuth 2.0 + OpenID Connect | INT01 (mandatory) |
| Authorisation model | RBAC via JWT claims from Keycloak | CLAUDE.md |
| Frontend | React + TypeScript, GOV.UK Design System | CLAUDE.md |
| Base images | `eclipse-temurin:21-jre-jammy` / `eclipse-temurin:21-jdk-jammy` / `node:20-alpine` / `nginx:1.27-alpine` / `postgres:16` / `quay.io/keycloak/keycloak:24` | CLAUDE.md |

---

## Useful Outputs from the Analysis

The Solon Tax v2.3.0 feasibility analysis did not produce design signals of the same character as the v1.0 Amplio analysis. The module-by-module accounting confirms that Solon Tax's partial scaffolding for `account`, `payment`, `repaymentplan`, and `workallocation` does not reduce meaningful delivery scope once DWP-specific compliance requirements are applied. No Solon Tax patterns are adopted as reference inputs.

The v1.0 useful outputs (document storage separation, alert framework reference, AI Gateway future scope) remain valid and are not affected by this update.

---

## Conditions for Revisiting

This decision is locked. It will be revisited only if:

1. A formal DWP procurement or programme decision mandates a specific platform at programme level, requiring a documented requirement change to INT01 and the CLAUDE.md constraint.
2. A future Solon Tax version ships with: native support for non-interrupting BPMN message boundary events, a DMN-standard authoring toolchain for non-technical users, Java 21 runtime, and Flowable as its process engine — at which point a fresh evaluation would be warranted against the full compliance requirements baseline.

Condition 2 requires resolution of Blocker 1 (non-interrupting events), Blocker 2 (DMN), and Blocker 3 (Java 21) as a minimum threshold before re-evaluation. Blockers 4, 5, 6, and 7 would also require resolution. No such Solon Tax version is anticipated.

---

## Recommendation and Operating Posture

### Recommendation

Stay on Option C (greenfield with Flowable). Do not revisit unless one of the two conditions above is met.

### Short reasoning

1. **The lead Solon feasibility blocker is structural.** Amplio 2.3.0 has no roadmap signal that non-interrupting message boundary events will be added (`amplio-process-engine-reference.md:173`). It is not a wait-and-see gap.
2. **The 10–15% scope reduction is illusory once the Solon workaround is priced.** Every workaround shape (interrupt-then-restart, suppression-as-guard, parallel process instance) moves the regulatory guarantee out of Solon/Amplio's process semantics and into application code, integration choreography, or BPMN-authoring discipline. The savings are eaten by the workaround, and the compliance posture is weaker than greenfield.
3. **The six secondary blockers compound.** Drools instead of DMN breaks non-technical rule authoring (ADR-008). Sequential-only Parallel Gateway is a silent correctness hazard. Java 17 silently loses the exhaustive-switch guarantee (ADR-014). Each is survivable alone; together they erode the architectural guarantees that make the system defensible to a regulator.
4. **The decision is cheap to keep locked, expensive to keep relitigating.** Every week spent re-evaluating Solon Tax is a week the build does not progress. The 4 May 2026 build start and 8 July 2026 demo do not move.

### Concrete operating posture

- Treat ADR-016 as closed. Reference it when asked; do not reopen it absent a Condition 1 or Condition 2 trigger (see *Conditions for Revisiting* above).
- In leadership and stakeholder forums, lead with the criminal-offence framing (Blocker 1). The other six blockers are appendix material and should not consume stage time.
- Be ready for the predictable pushback *"can we use Solon Tax for the financial engine only?"* The answer depends on what is retained. If Option B still depends on Solon/Amplio process or dispatch behaviour, Blocker 1 still applies. If those layers are replaced, the remaining benefit collapses to financial data-model scaffolding with Liquibase/Flyway migration risk and long-term integration coupling.
- If a re-evaluation is forced anyway, the honest cost is two weeks to reach the same conclusion, with the demo date moving by the same two weeks.

---

## Consequences

- The CLAUDE.md constraint ("This is NOT a configuration or integration project based on SOLON tax or any existing COTS platform. We are designing and building a new system.") is confirmed correct and requires no update.
- ADR-001 through ADR-015 remain locked with no amendments required.
- Any backlog items, spikes, or design branches created under the assumption of a Solon Tax pivot are closed.
- The `communications` module backlog must include explicit acceptance criteria for `CommunicationSuppressionService` integration at every dispatch path, including any bulk or batch send path. This requirement is unchanged from v1.0.
- The platform question is closed at the programme level. Further Solon Tax or Amplio evaluation is not required.

---

## Cross-References

| Reference | Relevance |
|---|---|
| ADR-001 | One process instance per account — Flowable confirmed |
| ADR-002 | Monitoring event subprocess pattern — Flowable confirmed; Solon/Amplio feasibility blocked because Amplio lacks the non-interrupting/concurrent event semantics needed to reproduce the required regulatory behaviour safely |
| ADR-003 | Process engine placement and delegate pattern — Flowable confirmed |
| ADR-007 | Single monolith — Solon Tax microservices architecture confirmed incompatible (Blocker 6) |
| ADR-008 | Three-tier configurability — DMN Tier 2 non-technical authoring confirmed on Flowable; Drools KIE confirmed incompatible (Blocker 2) |
| ADR-010 | Champion/challenger — version-scoped DMN deployment routing on Flowable confirmed; no Drools KIE equivalent (Blocker 2) |
| ADR-012 | Live-state DB reads in delegates — confirmed independent of engine choice |
| ADR-013 | Breathing space transaction boundary — confirmed independent of engine choice |
| ADR-014 | Communication suppression authority — PostgreSQL partial index confirmed; Java 21 exhaustive switch enforcement confirmed (Blocker 3); Solon Tax dispatch path incompatibility confirmed (Blocker 1) |
| INT01 | DWP SSO mandatory — OAuth 2.0 / OIDC; Solon Tax v2.3.0 auth blocker RESOLVED; Keycloak 24 adopted |
| COM06 / COM07 | Audit trail mandatory fields — full custom build confirmed |
| CLAUDE.md | Greenfield mandate confirmed authoritative; Java 21, Flyway, React, Keycloak, monolith constraints confirmed as blockers (Blockers 3, 4, 5, 6, 7) |
| Debt Respite Scheme Regulations 2020 | Criminal offence to send debt collection communications during breathing space moratorium — primary statutory basis for Blocker 1 |

---

## Appendix — Forced-Choice Posture (Hypothetical)

This appendix exists for one purpose: to give the delivery team a defensible answer if a forcing party (DWP, Netcompany leadership, programme governance) mandates Option A or Option B against this ADR's recommendation. It is not a fallback plan and is not adopted. Recording it here removes the need to reconstruct the analysis under time pressure.

### Forced Option B — Solon Tax financial engine only

Adopt Solon Tax for billing, payment ledger, and pay-plan calculation primitives. Replace everything else.

| Blocker | Workaround | Residual risk |
|---|---|---|
| 1 — Non-interrupting events (criminal liability) | Do not use Amplio. Run Flowable as the process engine on top of Solon Tax data. Treat Solon Tax as a financial database, not an orchestration platform. | Acceptable — the workaround is "do not use the broken component." |
| 2 — No DMN | Replace Drools with the Flowable DMN engine. Drools never enters the codebase. | Acceptable. |
| 3 — Java 17 vs Java 21 | DCMS code runs on Java 21. Solon Tax services are called over the wire (HTTP/Kafka). Language version does not cross the boundary. | Acceptable. Loses the option of embedding Solon Tax libraries in-process. |
| 4 — Liquibase vs Flyway | Two databases. Solon Tax owns its schema with Liquibase; DCMS owns its schema with Flyway. They never share tables. Cross-system writes go via Solon Tax APIs. | Material — every cross-system financial flow becomes a distributed transaction or eventual-consistency reconciliation problem. |
| 5 — Angular frontend | Discard the Solon Tax frontend. Build React + GOV.UK DS against Solon Tax APIs. | Acceptable — frontend was custom regardless. |
| 6 — Microservices vs monolith | Accept the boundary mismatch. The DCMS monolith calls Solon Tax microservices as external services. They are not absorbed. | Acceptable, but adds operational surface (service discovery, retry, circuit breaker per dependent service). |
| 7 — OPA vs Keycloak JWT | Solon Tax accepts a Keycloak-issued JWT at its API edge. A JWT-to-Rego claim translator is supplied if Solon Tax requires OPA inputs internally. | Material — split authorisation model. Requires security review at every boundary to confirm no claim is dropped or re-asserted incorrectly. |

**Net effect:** Solon Tax retained only for the payment ledger and pay-plan primitives. Process engine, DMN, frontend, auth, schema management remain greenfield. Scope reduction collapses from 10–15% to roughly **5–8%**. Permanent integration boundary against a system whose release cadence DCMS does not control.

**Required programme-level decision under Option B:** an explicit ban on Amplio from the DCMS architecture. Without that ban, Blocker 1 returns through any Amplio-orchestrated dispatch path inside Solon Tax.

### Forced Option A — Solon Tax as platform foundation

Genuinely harder. Workarounds for Blocker 1 are the load-bearing problem and none of them deliver the structural guarantee that non-interrupting message boundary events provide.

**Blocker 1 workaround options, in order of viability:**

1. **Suppression-as-guard with deployment-time static checker and runtime kill-switch.** Every dispatch path checks `breathingSpaceActive` before sending. A custom BPMN static-analysis check rejects any process model containing a dispatch task without a preceding suppression-guard gateway. A runtime kill-switch drops any outbound message whose trace context lacks a fresh suppression check. This is the most viable of the three options because it stacks defences. It is still a *discipline*, not an architectural impossibility.
2. **Interrupt-then-restart.** Interrupting events terminate the activity; resumption state is persisted in a DCMS-owned table; the activity is restarted on moratorium expiry. Hand-written resumption logic for every interruptible activity. Maintenance cost grows linearly with process complexity.
3. **Parallel coordinating process instance.** Two Amplio process instances per account (collection + suppression) coordinated via Kafka. Defeats the purpose of using a single engine. Compounded by Blocker 2's sequential-only Parallel Gateway.

**Selected approach for Forced Option A:** workaround (1), accepting that the compliance posture is defence-in-depth rather than structural impossibility. Programme-level legal-risk acceptance from DWP would be required before delivery proceeds.

**Other Option A blockers:**

| Blocker | Workaround |
|---|---|
| 2 — No DMN | Build a DMN-to-Drools transpiler so business admins author DMN and the system compiles to Drools at deploy time. Significant build investment; vendor risk if Drools KIE changes. |
| 3 — Java 17 | Backport the exhaustive-switch suppression coverage to Java 17 via a custom annotation processor and a build-failing CI check. Loses the JEP 441 compile-time guarantee; replaces it with a CI gate. |
| 4 — Liquibase | Adopt Liquibase, retire Flyway. One-time migration cost. |
| 5 — Angular | Either build a GOV.UK DS Angular component library (very large undertaking) or run a React frontend alongside the Solon Tax Angular UI (split UX, governance burden on which UI surfaces what). |
| 6 — Microservices | Accept the architecture. ADR-007's six-to-seven-person sizing breaks; delivery requires closer to 12–15 staff for microservices on this scope. |
| 7 — OPA | Adopt OPA, retire Keycloak-claim-based RBAC inside the app. Keycloak continues to issue tokens; OPA evaluates them. |

**Net effect:** scope reduction nominally 10–15% but offset by the DMN transpiler, the Java 17 annotation-processor work, the migration tooling, the frontend build-out, the team-size increase, and the OPA adoption. Realistic net delivery cost is *higher* than greenfield, with a weaker compliance posture.

### Recommendation under forcing

If forced, prefer **Option B with an explicit ban on Amplio**. That collapses Blocker 1 to "do not use Amplio" and turns the remaining blockers into integration engineering rather than compliance engineering.

If even the Amplio ban is not acceptable to the forcing party — that is, if they require DCMS to run *on* Amplio — DCMS cannot meet the Debt Respite Scheme 2020 obligations with structural guarantees. A written legal-risk acceptance from DWP would be required before delivery proceeds. This is a programme-level escalation, not a delivery decision.

### Status of this appendix

Hypothetical. Recorded for preparedness. Not adopted. Option C remains the locked decision per the body of this ADR.
