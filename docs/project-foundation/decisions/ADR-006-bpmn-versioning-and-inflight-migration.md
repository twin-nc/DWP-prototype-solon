> ## ⚠ STATUS: UNDER REVIEW — PLATFORM PIVOT IN EFFECT
>
> A confirmed programme-level decision (ADR-018, 2026-04-30) has pivoted the project to build on top of **Solon Tax** as the base platform. All prior ADRs are under review.
>
> **This document contains accumulated knowledge and analysis that remains relevant to the new design process. It does NOT represent the current or preferred direction.** All decisions and conclusions herein are open for renewed debate. Do not treat this ADR as a directive, default position, or settled constraint.
>
> See [ADR-018](ADR-018-platform-pivot-solon-tax-confirmed.md) for the full pivot record.

---

# ADR-006: BPMN Versioning and In-Flight Instance Migration Policy

## Status

Accepted

## Date

2026-04-22

## Context

Flowable maintains a version history for every deployed process definition. When a new version of a BPMN definition is deployed, existing process instances continue running against the version they started on. New instances start on the latest version.

This creates an operational problem: bugs in live BPMN definitions persist on in-flight instances indefinitely unless explicitly migrated. For debt collection, cases can run for months — a bad routing decision or stuck wait state cannot wait for natural case closure.

At the same time, migrating in-flight instances carelessly risks changing the rules on a debtor mid-arrangement, breaking correlation state, or producing an audit trail that cannot explain why a case moved.

The `standard-event-subprocesses` call activity fragment (ADR-002) introduces an additional versioning concern: the fragment is a shared definition deployed independently of individual treatment process definitions.

Three options were evaluated:

1. **Version lock (strict)** — in-flight instances always run on the version they started with; no migration tooling
2. **Migrate on next wait state** — automatic migration to the latest version at the next user task or timer boundary
3. **Hybrid by change class** — non-breaking changes leave in-flight instances on their version; breaking changes and bug fixes require an explicit named migration script with force-migrate capability

## Decision

**Option 3 chosen: hybrid by change class, with force-migrate for breaking changes and bug fixes.**

### Change classification

| Class | Definition | Migration requirement |
|---|---|---|
| **Non-breaking** | Adds a new path, task, or event that does not alter existing token positions or correlation keys. New instances use it; in-flight instances are unaffected. | None — deploy and monitor |
| **Breaking** | Removes or renames a task, changes flow structure that would strand existing tokens, or alters a message/signal correlation key. | Named migration script required; force-migrate all affected instances |
| **Bug fix** | Any change intended to correct incorrect behaviour on live instances, regardless of structural impact. | Treated as breaking for migration purposes — force-migrate is the mechanism |

A breaking change or bug fix is always a **Class A change** and requires PROCESS_DESIGNER proposal + COMPLIANCE approval (two-person gate) before deployment. DOMAIN_EXPERT is not a provisioned role — its governance function is absorbed by COMPLIANCE per RBAC design review (2026-04-24).

### Non-breaking deployment

1. New BPMN version deployed via Flowable's `RepositoryService.createDeployment()`
2. New process instances start on the new version automatically
3. In-flight instances continue on their version — no action required
4. Deployment is logged with deployer identity, timestamp, and definition key+version in the audit trail

### Breaking change and bug fix: force-migrate path

A migration script is a Java class implementing `ProcessMigrationScript`:

```java
public interface ProcessMigrationScript {
    String sourceDefinitionKey();
    int    sourceVersion();         // -1 = all versions
    String targetDefinitionKey();
    ProcessInstanceMigrationBuilder migrationBuilder(
            RepositoryService repositoryService);
    String migrationReason();       // recorded in audit trail
    String approvedBy();            // COMPLIANCE approver
}
```

The migration script must be committed in the same PR as the BPMN change. The PR cannot be merged without a linked domain ruling (RULING-NNN) or architecture sign-off recorded in `docs/project-foundation/decisions/`.

Migration execution:

1. Deploy new BPMN version
2. Run migration script via Flowable's `ProcessMigrationService` for all instances matching `sourceDefinitionKey` + `sourceVersion` (exact API method depends on Flowable version in use — consult Flowable User Guide: Process Instance Migration)
3. Flowable validates each instance can be migrated before committing (token positions, active tasks, correlation state)
4. If validation fails for any instance, the migration is aborted for that instance; a `MigrationFailureRecord` is written to the audit trail with instance ID, reason, and current token position
5. Failed instances remain on their original version and are flagged for manual review in the supervisor queue

### Migration failure handling

Instances that fail automated migration are not force-terminated. They are:
- Flagged with a `MIGRATION_FAILED` marker visible in the supervisor worklist
- Frozen at their current wait state (no new outbound actions triggered)
- Resolved manually by a supervisor: either corrected and re-attempted, or closed and re-initiated as a new case

This is a deliberate policy choice: a migration failure on a live debt account is a data quality event, not an application error, and requires human judgement.

### `standard-event-subprocesses` fragment

In-flight instances remain on the fragment version active when they started (per ADR-002). This is intentional: the fragment contains interrupting event subprocesses for breathing space and insolvency. Automatic fragment updates mid-flight risk silently changing correlation behaviour or interrupt semantics for a case already part-way through a regulatory hold.

If a fragment change must reach in-flight instances (e.g. a bug fix to the `breathing-space-received` subprocess), it goes through the force-migrate path above. The migration script targets instances on the affected fragment version explicitly. This makes the propagation deliberate, signed-off, and auditable.

Fragment version is tracked in the deployment metadata separately from treatment process definition versions.

### Deployment metadata record

Every BPMN deployment writes a record to `process_definition_deployment`:

```sql
CREATE TABLE process_definition_deployment (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    definition_key      VARCHAR(200) NOT NULL,
    version             INT          NOT NULL,
    flowable_deploy_id  VARCHAR(100) NOT NULL,
    deployed_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deployed_by         VARCHAR(100) NOT NULL,
    change_class        VARCHAR(20)  NOT NULL,  -- NON_BREAKING | BREAKING | BUG_FIX
    migration_script    VARCHAR(200),           -- class name, if applicable
    approved_by         VARCHAR(100),           -- required for BREAKING and BUG_FIX
    notes               TEXT
);
```

### What is NOT permitted

- Deploying a breaking BPMN change without a committed migration script in the same PR
- Force-terminating in-flight instances to work around a migration failure — close and re-initiate instead
- Skipping the Class A sign-off gate for any breaking change or bug fix, regardless of urgency
- Modifying `process_definition_deployment` records after the fact

## Consequences

- Every BPMN PR must include a change classification in the PR description
- Breaking changes and bug fixes require a `ProcessMigrationScript` implementation committed alongside the BPMN file
- The supervisor worklist must surface `MIGRATION_FAILED` instances as a distinct work item type
- Fragment versioning is tracked independently; fragment changes that must reach in-flight cases follow the same force-migrate path
- Deployment tooling must write to `process_definition_deployment` on every deploy — this is a required step in the CI/CD pipeline, not optional
- In-flight instance counts by definition version must be visible in the ops dashboard so deployers can assess migration scope before executing

## Alternatives Rejected

### Option 1 — Version lock (strict)

In-flight instances always run on their starting version; no migration path.

**Rejected because:** bugs in live BPMN definitions would persist for the lifetime of the case — potentially months. For a debt collection system processing vulnerable customers, this is not acceptable. A stuck workflow is a regulatory risk (DW.18 — eliminate blackholes).

### Option 2 — Migrate on next wait state (automatic)

All in-flight instances automatically migrate to the latest version at their next user task or timer boundary.

**Rejected because:** automatic migration conflates non-breaking updates with breaking ones. An agent adding a new treatment path should not trigger silent re-routing of every active case. Automatic migration also produces an audit trail that cannot clearly explain why a case moved to a different process version — a compliance problem for DWP audit obligations.

## References

- ADR-001: Atomic case initiation transaction
- ADR-002: `standard-event-subprocesses` call activity pattern and fragment versioning
- ADR-005: Candidate list and case initiation entry point
- WAYS-OF-WORKING.md §5a (Class A change triggers and sign-off requirements)
- Flowable User Guide: Process Instance Migration
- DW.18 — eliminate blackholes (accounts lost in workflow)
