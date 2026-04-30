# Solon Tax v2.3.0 Platform Feasibility Analysis
# DCMS Build Strategy Assessment

**Date:** 2026-04-27
**Status:** Final — decision locked in ADR-016 v2.0
**Outcome:** Option C (Greenfield) confirmed

---

## Purpose

This document records the full Solon Tax v2.3.0 feasibility assessment for the DCMS platform decision. It answers three questions in sequence:

1. What would be possible if DCMS were built on Solon Tax?
2. What would not be possible?
3. How would such a solution be designed — and why is that design path not taken?

The assessment supersedes the v1.0 analysis (which was based on Amplio documentation only, with no Solon Tax product documentation available).

---

## Solon Tax v2.3.0 — What It Actually Is

Solon Tax is a Revenue Management Platform product — an ERP for national and regional tax authorities. It is not a generic case management framework. Its domain model, reference processes, and financial engine are designed for tax assessment, billing, collection, and objection handling.

**Confirmed tech stack (v2.3.0):**

| Layer | Solon Tax v2.3.0 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot (microservices) |
| Database | PostgreSQL 14+ OR Oracle 19c+ (dual support) |
| Authentication | Keycloak OAuth 2.0 + OIDC |
| Process engine | Amplio Process Engine (primary; BPMN 2.0) |
| Rules engine | Drools KIE (DRL, no native DMN) |
| Authorisation | OPA (Open Policy Agent) with Rego policies |
| Async messaging | Apache Kafka + Outbox Pattern |
| Caching | Redis (session), Hazelcast (distributed process state) |
| Schema migrations | Liquibase |
| Batch | Foundation Batch Engine (Spring Batch-based) |
| Frontend | Angular (revenue-management-ui + self-service-ui) |
| Reporting | Jasper Reports |
| Deployment | Kubernetes (customer-hosted), Helm v3.x |

### What v2.3.0 resolves from the v1.0 assessment

The original rejection of the Amplio/Solon Tax pivot (ADR-016 v1.0) cited two opening hard blockers:

- **Auth (SAML2/OIOSAML3 vs OAuth 2.0):** RESOLVED — Solon Tax v2.3.0 uses Keycloak OAuth 2.0 + OIDC.
- **Database (Oracle-only):** RESOLVED — Solon Tax v2.3.0 supports PostgreSQL 14+.
- **Camunda 7 EOL:** RETIRED — Solon Tax v2.3.0 uses Amplio Process Engine, not Camunda 7.

These resolutions prompted a full re-assessment.

---

## What Is Possible: Solon Tax Capabilities That Map to DCMS

### Financial Domain

Solon Tax's strongest match to DCMS requirements is in the financial domain.

**Bill Segments and Bills**
Solon Tax's core debit/credit unit (analogous to DCMS `account` ledger). Liabilities include a statute of limitation field. Financial Transactions (FTs) post balance changes through Drools posting rules. Journal Entry Export to external ERP/GL systems is supported. ISO 20022 / PAIN.001/002 support for bank payment files is shipped.

*What this means for DCMS:* The account balance and transaction posting structures have analogues. A DCMS adoption of Solon Tax would not need to build a payment ledger from scratch.

**Pay Plans (Instalment Arrangements)**
Solon Tax ships a payment arrangement model covering instalment schedules. `PAYMENT_DEFERRAL.bpmn` is a reference process. Direct debit, bank transfer, EBT, credit card, and digital wallet payment types are supported.

*What this means for DCMS:* The repayment arrangement concept exists. However, DWP-specific requirements (re-aging, breach tolerance, DD mandate management, CCA allocation order) would be custom regardless.

**Write-off and Overpayment Management**
Write-off and overpayment management (offsetting, refund, write-off) are shipped features.

*What this means for DCMS:* Write-off authority structures (tier limits, ADR-011) and the CCA allocation order are custom, but the underlying financial mechanics exist.

### Reference BPMN Processes

Solon Tax ships 28 reference BPMN processes. The following are directly relevant to DCMS:

| Process | DCMS Relevance |
|---|---|
| `DEBT_RECOVERY_PROCESS.bpmn` | Demand letter → warrant letter → collection agency handover — directly maps to DCMS treatment path |
| `OBJECTION_PROCESS.bpmn` | Full dispute lifecycle (timeliness check → hearing → decision → outcome letter) |
| `CASE_HANDOVER_COLLECTION_AGENCY_PROCESS.bpmn` | DCA handover — maps to DCMS `integration` DCA placement |
| `VAT_OVERPAYMENT_PROCESS.bpmn` | Partial analogue to DCMS overpayment handling |
| `PAYMENT_DEFERRAL.bpmn` | Repayment deferral request |
| `AGREEMENT.bpmn` | Third-party authority (delegation) |
| `NOTIFY_OVERDUE_DEBTORS.bpmn` | Batch notification to overdue accounts |

These processes provide reference architecture for debt recovery workflows. They cannot be used as-is (they are tax authority processes, not benefit debt collection processes) but represent meaningful reference material.

### Customisation Model

Solon Tax's customisation architecture is designed for extension:
- Standard layer (`revenue-management-be`) + Custom layer (`revenue-management-be-custom`)
- Client code in custom layer only; no core code modification
- Data Area (schema-less JSONB/BLOB per entity) for custom fields without schema migrations
- BPMN hot-redeploy at runtime (running instances continue on old version)
- Drools rules deployable via KIE container without code change
- OPA Rego policies configurable for row-level security
- Process triggers configurable via Eventplans system parameter

This is a legitimate and intended pattern for building client-specific capability on top of Solon Tax.

### Batch Engine

Foundation Batch Engine (Spring Batch-based) with:
- Scheduler, Engine, Item, and Core modules
- Reader → Processor (optional) → Writer job structure
- Failed items marked failed; job continues; restartable
- Step partitioning for high-volume concurrent processing
- REST API for job management
- Cron scheduling

*What this means for DCMS:* Batch processing infrastructure for DM6 file transfer, bureau feeds, and bulk communication dispatch would have a framework. However, all DCMS-specific batch logic (DM6 validation, back-out capability, suppression-aware bulk dispatch) would be custom regardless.

---

## What Is Not Possible: Hard Blockers

These are not gaps that can be closed with effort. They are architectural incompatibilities with accepted DCMS ADRs or statutory requirements.

### Blocker 1 — Non-Interrupting Message Boundary Events (Primary Disqualifier)

**What Solon Tax does:** The Amplio Process Engine supports interrupting boundary events only. Early messages are lost (no buffering).

**What DCMS requires:** ADR-002 requires non-interrupting message boundary events for `breathing-space-received`, `change-of-circumstances`, `payment-missed`, and `supervisor-override`. Non-interrupting means the event triggers a parallel subprocess while the main process continues running.

**Why this is a hard blocker:**

Breathing space moratorium (Debt Respite Scheme Regulations 2020) requires that when a `BREATHING_SPACE_START` event arrives, the main collection process is *suspended* — not cancelled. The debt still exists. Enforcement must resume after the moratorium ends. A process terminated by an interrupting event cannot be resumed in BPMN semantics — it is gone. The arrangement monitoring state (instalment schedule position, breach tolerance count, payment history) is destroyed.

An interrupting event would also trigger `change-of-circumstances` (re-segmentation), `payment-missed` (breach handling), and `supervisor-override` (move to specified step) in a way that cancels the main treatment process rather than running in parallel. These events are specifically non-interrupting because the main process must continue.

ADR-002 explicitly evaluated and rejected the application-layer Spring service approach as a workaround (Option 3 in ADR-002 evaluation). That path produces a lifecycle split between the process engine and application logic that is invisible to the Flowable audit trail, cannot be champion/challenger tested, and creates competing process state sources.

**The criminal liability dimension:** Sending a debt collection communication during an active breathing space moratorium is a criminal offence under the Debt Respite Scheme Regulations 2020. Any architecture that cannot implement reliable moratorium enforcement — including enforcement during the interval when the `BREATHING_SPACE_START` message was received and before a workaround mechanism could act — creates criminal liability for DWP.

**Resolution path:** None within Solon Tax's Amplio Process Engine. Resolving this requires not using Solon Tax's process engine.

### Blocker 2 — No Native DMN Engine

**What Solon Tax does:** Drools KIE for rule evaluation. Business Rule Tasks in BPMN are implemented as Service Tasks calling Drools containers via Java.

**What DCMS requires:** ADR-008 Tier 2 mandates a DMN authoring UI for non-technical `PROCESS_DESIGNER` and `COMPLIANCE` role holders. Changes to segmentation and treatment rules must be deployable without a code commit. ADR-010 champion/challenger uses version-scoped DMN deployment routing where deterministic hash routing selects between approved DMN deployments by `deployment_id`.

**Why this is a hard blocker:**

DMN (Decision Model and Notation) is an OMG standard with graphical decision table editors that non-technical staff can operate. Drools DRL (Drools Rule Language) requires rule authoring in a Java-adjacent syntax that PROCESS_DESIGNER role holders cannot use without developer involvement.

The ADR-008 governance model is built entirely on DMN: PROCESS_DESIGNER authors a rule change in the admin UI decision table editor, submits for COMPLIANCE sign-off, and the deployment runs without touching version control. This is the mechanism that allows DWP to change treatment segment thresholds, interest rate bands, and treatment paths within a programme cycle without code deployments. Replacing this with Drools makes every rule change a developer-gated event.

ADR-010's champion/challenger versioning semantics rely on Flowable's DMN deployment model (first-class versioned deployments, version-scoped evaluation at query time). There is no equivalent in Drools KIE without custom infrastructure.

**Resolution path:** Add a separate DMN engine alongside Solon Tax. This is technically possible but adds a component Solon Tax does not support natively, requires maintaining two rule engines in parallel, and negates any scope-reduction benefit of the platform adoption.

### Blocker 3 — Java 17 vs Java 21

**What Solon Tax does:** Java 17 runtime.

**What DCMS requires:** CLAUDE.md mandates Java 21 (`eclipse-temurin:21`).

**Why this is a hard blocker beyond the mandate:**

ADR-014 uses JEP 441 (exhaustive switch expressions on sealed interfaces, finalised in Java 21; preview only in Java 17) to enforce at compile time that every `CommunicationCategory` value is handled in `CommunicationSuppressionService.isPermitted()`. On Java 17, a new `CommunicationCategory` value added to the sealed interface compiles without error even if no switch arm handles it — the compile-time guarantee is silently lost. The consequence: a communication category added by a developer without updating the suppression service switch reaches the dispatch path without statutory authority evaluation.

This is a statutory safety mechanism embedded in the Java 21 mandate, not a language preference.

**Resolution path:** Build DCMS custom modules in a separate Java 21 JVM process, integrating with Solon Tax via API. This reduces Solon Tax to a called service rather than a shared platform — equivalent in complexity to building from scratch.

### Blocker 4 — Liquibase vs Flyway

**What Solon Tax does:** Liquibase for schema migrations (`databasechangelog` table).

**What DCMS requires:** CLAUDE.md mandates Flyway Community Edition (`flyway_schema_history` table).

**Why this is a hard blocker:** Two migration tools against the same PostgreSQL database produce mutually exclusive migration history tables with incompatible migration ordering semantics. There is no safe reconciliation path. Flyway cannot adopt Liquibase changesets.

**Resolution path:** Solon Tax provides a one-time Flyway-compatible schema export and transfers schema ownership to DCMS. This is non-standard, not offered by the product, and transfers upgrade responsibility to the DCMS team — eliminating the product's schema maintenance benefit.

### Blockers 5, 6, 7 (CLAUDE.md mandate violations)

**Blocker 5 — Angular vs React + GOV.UK Design System:** GOV.UK Design System has no Angular component library. DCMS frontend must be React. Solon Tax's Angular frontend contributes nothing.

**Blocker 6 — Microservices vs single monolith (ADR-007):** ADR-007 mandates a single Spring Boot monolith. Solon Tax is a microservices architecture. These are structurally incompatible.

**Blocker 7 — OPA authorisation vs RBAC via Keycloak JWT:** CLAUDE.md mandates RBAC via JWT claims from Keycloak 24. Solon Tax uses OPA with Rego policies. A split authorisation model creates security gaps.

---

## How a Solon Tax Solution Would Be Designed (and Why It Isn't)

Three options were evaluated.

### Option A: Solon Tax as Platform Foundation

DCMS is built as a Solon Tax custom-layer extension (`revenue-management-be-custom`).

**What the architecture would look like:**

```
Solon Tax standard layer (revenue-management-be)
    └─ DCMS custom layer (revenue-management-be-custom)
         ├─ CommunicationSuppressionService (custom — replaces Solon Tax dispatch)
         ├─ VulnerabilityClassificationService (custom — FCA FG21/1)
         ├─ SegmentationEngine (custom — no Solon Tax equivalent)
         ├─ ChampionChallengerService (custom — no Solon Tax equivalent)
         ├─ DWP integrations (custom — DWP Place, DCA, WorldPay, DM6)
         ├─ INSERT-only audit schema (custom — no Solon Tax equivalent)
         └─ GOV.UK Design System frontend (React — replaces Solon Tax Angular)
```

**Gross scope available from Solon Tax:** 30–35% (financial ledger, pay plans, batch engine, task tray, reference BPMN processes)

**Scope that must be built regardless:**
All of: customer module, strategy module (including all DMN/segmentation), communications module (full replacement — Solon Tax dispatch path is incompatible), integration module, audit module, user/RBAC module, three-tier configurability admin UI, champion/challenger framework, GOV.UK Design System frontend.

**Net scope after blockers:** 5–10%

**Why it isn't taken:** Seven hard blockers, the most critical being Blocker 1 (criminal liability), Blocker 2 (no DMN for non-technical governance), and Blocker 3 (Java 21 statutory safety). The net scope reduction does not justify resolving these blockers — most of which cannot be resolved within Option A.

### Option B: Solon Tax Financial Engine Only

DCMS adopts Solon Tax's billing, payment, and pay plan engine as a dependency. Process engine, auth, frontend, and migration tooling are replaced with DCMS's mandated stack.

**What the architecture would look like:**

```
DCMS monolith (Java 21, Spring Boot 3.4.x, Flowable, Keycloak 24, PostgreSQL 16)
    ├─ Anti-corruption layer → Solon Tax Financial API
    │    ├─ AccountFinancialAdapter (bill segments, FTs, write-off)
    │    ├─ PaymentAdapter (posting, allocation, overpayment)
    │    └─ RepaymentPlanAdapter (arrangement lifecycle)
    ├─ All domain modules (custom — identical to Option C except account/payment)
    └─ GOV.UK Design System frontend (React)

Solon Tax Financial Service (separate deployment)
    └─ Billing, payments, pay plans, interest/penalty, ISO 20022
```

**Gross scope available from Solon Tax:** 20–25%

**Net scope after blockers:** 10–15%

**Why it isn't taken:**

The Liquibase/Flyway blocker (Blocker 4) remains even in Option B unless Solon Tax provides a Flyway-compatible schema — which is not offered. The integration surface area cost (maintaining an anti-corruption layer across Solon Tax financial engine upgrades with a 6–7 person team) compounds over the programme lifetime. The financial ledger, payment allocation, and repayment arrangement components are well-understood Spring Data JPA engineering — the effort to build them in greenfield is comparable to the effort of building and maintaining the Solon Tax integration layer, without the upstream dependency risk.

### Option C: Greenfield (Confirmed)

**Why it is taken:**

- Zero hard blockers. All accepted ADRs (ADR-001 through ADR-015) work as designed.
- ADR-002 non-interrupting event subprocesses work on Flowable exactly as specified.
- ADR-008 three-tier configurability uses Flowable DMN natively with a business-user graphical editor.
- ADR-010 champion/challenger routes between Flowable DMN deployments with version-scoped evaluation.
- ADR-014 Java 21 exhaustive switch enforcement for CommunicationSuppressionService is fully available.
- ADR-007 monolith mandate is satisfied; 6–7 person team owns everything without upstream dependency management.
- No stack version conflicts; no Liquibase/Flyway race conditions; no Angular/React divergence.

**Financial engine in Option C:**

The `account` and `payment` modules are built with Spring Data JPA + PostgreSQL 16. The components are:
- Account/ledger: balance tracking, FT posting, write-off, regulatory facts (breathing space date, insolvency date, death, fraud marker)
- Payment allocation: CCA-mandated hierarchy implemented as deterministic allocation logic
- Repayment plans: instalment schedule lifecycle, breach detection, re-aging, direct debit mandate management
- ISO 20022 PAIN.001/002: bank payment file generation for BACS/FPS

These are well-understood engineering components. The ISO 20022 and direct debit mandate integrations are the highest-effort items; they are bounded scope with clear external specifications (UK BACS/FPS schemes, ISO 20022 standard).

---

## Scope Reduction Summary

| Module | Option A (Full Platform) | Option B (Financial Engine) | Option C (Greenfield) |
|---|---|---|---|
| `account` | Partial scaffold | Partial scaffold | Full custom build |
| `customer` | None | None | Full custom build |
| `strategy` | None | None | Full custom build |
| `repaymentplan` | Partial scaffold | Partial scaffold | Full custom build |
| `payment` | Partial scaffold | Partial scaffold | Full custom build |
| `communications` | None (dispatch incompatible) | None | Full custom build |
| `workallocation` | Partial scaffold | None | Full custom build |
| `integration` | None | None | Full custom build |
| `audit` | None | None | Full custom build |
| `user` | None | None | Full custom build |
| Three-tier config admin UI | None | None | Full custom build |
| Champion/challenger | None | None | Full custom build |
| GOV.UK React frontend | None | None | Full custom build |
| **Net scope reduction** | **5–10%** | **10–15%** | **0% (baseline)** |

---

## Final Decision

ADR-016 v2.0 locks Option C. The decision is unchanged from v1.0, but the rationale is now accurate. The primary disqualifier is the non-interrupting message boundary event limitation in the Amplio Process Engine (Blocker 1), which creates an irresolvable architecture incompatibility with breathing space statutory moratorium handling and carries criminal liability consequences. Six additional hard blockers independently disqualify Options A and B.

The net scope reduction available from any Solon Tax adoption path (5–15%) does not justify the integration risk, statutory liability exposure, or platform coupling cost.

---

## References

| Document | Path |
|---|---|
| ADR-016 v2.0 | `docs/project-foundation/decisions/ADR-016-platform-pivot-rejected-greenfield-confirmed.md` |
| ADR-002 | `docs/project-foundation/decisions/ADR-002-*.md` |
| ADR-007 | `docs/project-foundation/decisions/ADR-007-*.md` |
| ADR-008 | `docs/project-foundation/decisions/ADR-008-*.md` |
| ADR-010 | `docs/project-foundation/decisions/ADR-010-*.md` |
| ADR-014 | `docs/project-foundation/decisions/ADR-014-*.md` |
| Solon Tax integration guide | `external_sys_docs/solon/solon_tax_2.3.0_integration_guide.md` |
| Solon Tax operations guide | `external_sys_docs/solon/solon_tax_2.3.0_operations_guide.md` |
| Solon Tax API list | `external_sys_docs/solon/api-list.md` |
| Solon Tax batch engine | `external_sys_docs/solon/Batch-Engine.txt` |
