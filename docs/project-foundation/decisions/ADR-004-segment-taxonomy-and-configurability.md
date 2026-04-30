> ## ⚠ STATUS: UNDER REVIEW — PLATFORM PIVOT IN EFFECT
>
> A confirmed programme-level decision (ADR-018, 2026-04-30) has pivoted the project to build on top of **Solon Tax** as the base platform. All prior ADRs are under review.
>
> **This document contains accumulated knowledge and analysis that remains relevant to the new design process. It does NOT represent the current or preferred direction.** All decisions and conclusions herein are open for renewed debate. Do not treat this ADR as a directive, default position, or settled constraint.
>
> See [ADR-018](ADR-018-platform-pivot-solon-tax-confirmed.md) for the full pivot record.

---

# ADR-004: Segment taxonomy, exception state distinction, and two-tier configurability model

## Status

Accepted

## Date

2026-04-22

## Context

The workflow diagram shows two treatment lanes (vulnerable on-benefit, standard on-benefit) diverging from a segmentation gateway after case initiation. The monitoring pool shows re-segmentation occurring when circumstances change (benefit ceases, vulnerability level changes).

Requirements establish that segmentation must be configurable — DWP will add, modify, and retire segments as the service matures, without requiring a code deployment:

- DW.9 — all business rules, strategies, workflows must have a version to track development
- DW.16 — create and amend workflow and decisioning on a frequent basis within business admin controls
- DW.32 — use business rules or scorecards to segment accounts and assign to strategies
- DW.38 — visual strategy in easy-to-follow format for non-technical business users
- DW.39 — test changes to strategy and business rules using bulk batches of test accounts
- DW.41 — simulate impact of changes before implementation
- DW.42 — trace outcome back through decisioning logic
- DW.44 — segment customers based on criteria or flags on a customer or account
- CAS.11 — segment and align strategies at different levels based on configurable rules
- BSF.2 — create scores for propensity to pay, likelihood of write-off, treatment or segmentation
- BSF.13 — apply scorecards to drive allocation of accounts to agents, teams, workflow or communications

Two design questions needed resolution:

1. What is the initial segment taxonomy, and how do exception states (breathing space, insolvency, deceased, fraud) relate to segments?
2. What does "configurable without a code deployment" mean in practice, given that adding a new treatment path also requires a new BPMN process definition?

## Decision

### 1. Primary segment taxonomy (initial set)

Segments are defined on two dimensions visible in the workflow diagram:

| Segment code | Vulnerability | Benefit status | Process definition key |
|---|---|---|---|
| `VULNERABLE_ON_BENEFIT` | true | ON_BENEFIT | `treatment-vulnerable-on-benefit` |
| `STANDARD_ON_BENEFIT` | false | ON_BENEFIT | `treatment-standard-on-benefit` |
| `VULNERABLE_OFF_BENEFIT` | true | OFF_BENEFIT | `treatment-vulnerable-off-benefit` |
| `STANDARD_OFF_BENEFIT` | false | OFF_BENEFIT | `treatment-standard-off-benefit` |

These are the four initial process definitions. Additional segments will be added by DWP configuration over time.

Segmentation operates at **account level** (each debt account has a segment) informed by **customer-level attributes** (vulnerability flag, circumstances). A single customer may have accounts in different segments simultaneously (CAS.11).

### 2. Exception states are not segments

Deceased, insolvency, breathing space, fraud, and hardship are **process states**, not segments. They can coexist with any primary segment — a `STANDARD_ON_BENEFIT` debtor can enter breathing space without their underlying segment changing.

- **Segments** → determine which process definition starts → drive `startProcessInstanceByKey`
- **Exception states** → modelled as interrupting/non-interrupting event subprocesses within every process definition (ADR-002) → drive suspension, escalation, or permanent termination

The `strategy` module's segmentation service returns only a primary segment code. Exception state detection is handled by monitoring event subprocesses and does not pass through the segmentation service.

Pre-configured exception strategies required by DW.25 (Deceased, Insolvency, Hardship, Persistent Debt) are BPMN event subprocess definitions, not segment definitions.

### 3. Two-tier configurability model

**Tier 1 — No-code: segment criteria and routing (operational config)**

Segment definitions, their evaluation criteria, and their mapping to process definition keys are stored in the database and editable via an admin UI without any application redeployment.

- Segment definitions table: segment code, label, active flag, effective-from date, process definition key mapping
- Evaluation rules implemented as DMN decision tables in the `strategy` module — non-technical users can read and edit decision tables via admin UI (satisfies DW.38)
- DMN tables are versioned in the database (satisfies DW.9) and testable against bulk account batches (satisfies DW.39)
- Every segmentation decision is recorded with the DMN version that produced it (satisfies DW.42)
- Adding a new segment that reuses an existing process definition key is **purely tier 1** — no BPMN work required

**Tier 2 — Low-code: new process definition deployment (restricted admin)**

If a new segment requires a new treatment path, a BPMN process definition must be authored and deployed. This does not require a Java code change or Spring Boot redeployment — Flowable supports runtime BPMN deployment via its REST API and admin tooling.

- A restricted admin function (privileged role, separate from operational config) allows upload and deployment of BPMN definitions to Flowable at runtime
- After deployment, the segment-to-process-definition-key mapping is updated via tier 1 admin UI to point to the new definition key
- Deploying a new or modified BPMN process definition is a **Class A change** — it alters live debt collection behaviour and requires PROCESS_DESIGNER proposal + COMPLIANCE approval (two-person gate) per RBAC-IMPLEMENTATION-DECISIONS.md section 5.
- New process definitions must include the standard event subprocess call activity (see ADR-002 amendment below) — this is enforced by the Class A review gate, not by technical constraint

### 4. Shared event subprocess as a call activity (amendment to ADR-002)

All treatment process definitions must include the standard monitoring event subprocesses (breathing space, benefit ceased, insolvency, death, payment missed, supervisor override). These are defined **once** as a shared Flowable call activity subprocess definition:

- Definition key: `standard-event-subprocesses`
- Deployed at application startup as a static resource from `backend/src/main/resources/processes/fragments/standard-event-subprocesses.bpmn`
- Each treatment process definition calls it via a BPMN `callActivity` element at root scope
- When the shared definition is updated (e.g. a new regulatory signal added), all treatment processes inherit the change automatically on next process instance start — no per-treatment-process update required
- The shared definition is a versioned Flowable deployment; in-flight instances remain on the version they started

This replaces the "copy-on-use fragment" approach implied in ADR-002 implementation note 5.

### 5. Segment-to-process-definition-key mapping ownership

The mapping from segment code to Flowable process definition key is owned by the `strategy` module as runtime configuration data. The `infrastructure/process` layer has no knowledge of segments — it receives a process definition key from `ProcessStartPort` and starts the instance. Segment business logic does not leak into the process infrastructure.

## Consequences

- `strategy` module must implement: segment definition persistence, DMN-based evaluation engine, segment version history, admin API for segment CRUD, bulk test execution against batches (DW.39, DW.41)
- `strategy` module admin UI screens are required before any segment-driven routing can be tested end-to-end
- DMN decision tables are the canonical representation of segmentation logic — no hardcoded segment evaluation in Java
- New segments that reuse existing process definitions require no code change and no BPMN work — purely database config
- New segments requiring new treatment paths require BPMN authoring, runtime deployment, and Class A sign-off
- The shared `standard-event-subprocesses` call activity must always be deployed and must always be current — it is a mandatory dependency for all treatment processes
- Segment changes take effect on new process instances only — in-flight instances continue on the segment/process definition they started with (consistent with Flowable's versioning model)
- Champion/challenger testing (DW.2, DW.39) operates at the DMN table level — two versions of segmentation rules can run simultaneously against a split of incoming cases

## Alternatives Rejected

### Segment taxonomy as a Java enum (Rejected)

Hard-coding segments as an enum satisfies the initial four segments but blocks DWP from adding segments without a code deployment. Rejected: violates DW.16 and the no-code/low-code requirement.

### Exception states as segments (Rejected)

Modelling deceased/insolvency/breathing space as segments would require terminating the current treatment process and starting a "deceased process" — but these are not treatment paths that run to completion, they are holds that either resolve or permanently terminate. The event subprocess model (ADR-002) handles them correctly without conflating them with treatment segmentation.

### Copy-on-use BPMN fragment for event subprocesses (Rejected)

Embedding the standard event subprocess fragment inline in each treatment BPMN means that updating the fragment (e.g. adding a new regulatory signal) requires updating every deployed treatment process definition. The call activity pattern centralises this in one definition, updated once.

## Implementation Notes

1. **DMN tooling:** Flowable includes a DMN editor in its modelling suite. Decision tables authored there are deployable as `.dmn` files alongside BPMN. The initial four-segment DMN table should be the first `strategy` module artefact built.

2. **Segment definition schema (database):**
   - `segment_definition`: id, code, label, process_definition_key, active, effective_from, effective_to, created_by, created_at
   - `segment_rule_version`: id, segment_definition_id, dmn_deployment_id, version, deployed_at, deployed_by
   - Effective-date handling follows STD-PLAT-005 (date-effective rules).

3. **Segmentation audit:** every segmentation decision records: debtor account ID, segment code assigned, DMN version used, input variables, timestamp. Stored in `audit_log` via the standard audit domain event.

4. **Champion/challenger:** two active DMN versions for the same segment definition can run simultaneously — one as champion, one as challenger — with traffic split configurable. Outcome tracking is an `analytics` module concern.

5. **Class A gate for BPMN deployment:** the admin function for uploading a new process definition must require a two-person gate (PROCESS_DESIGNER proposes, COMPLIANCE approves) before the definition is published to Flowable. DOMAIN_EXPERT is not a provisioned role — its governance function is absorbed by COMPLIANCE per RBAC design review (2026-04-24). This is a workflow gate, not just a permission check.

6. **Call activity resource:** `backend/src/main/resources/processes/fragments/standard-event-subprocesses.bpmn` is deployed by `FlowableProcessEngineConfiguration` at application startup via `setDeploymentResources`. It must be present — application startup should fail if it is missing.

## References

- ADR-001: Process instance per debtor+debt pair
- ADR-002: Monitoring and event handling via BPMN event subprocesses (amended by this ADR — implementation note 5)
- ADR-003: Process engine as infrastructure — delegate command pattern
- WAYS-OF-WORKING.md §5a (Class A change triggers)
- STD-PLAT-005 (date-effective rules)
- STD-GOV-004 (contract versioning)
- DW requirements: DW.2, DW.9, DW.16, DW.32, DW.38, DW.39, DW.41, DW.42, DW.44
- CAS.11, BSF.2, BSF.13
