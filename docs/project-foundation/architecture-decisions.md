# Architecture Decision Records
**DWP Debt Collection Management System (DCMS)**
Version 1.0 | April 2026

## ADR Index

| ADR | Title | Status |
|---|---|---|
| ADR-001 | CommunicationSuppressionService — boundary and interface | LOCKED |
| ADR-002 | StatuteBarredEvaluator — boundary and guardrails | LOCKED |
| ADR-003 | Platform-wide transaction boundary rule | LOCKED |
| ADR-004 | Write-off self-approval — three-layer defence | LOCKED |
| ADR-005 | DeceasedPartyHandler — two-phase atomicity | LOCKED |
| ADR-006 | Flowable Admin UI — access control and audit | LOCKED |
| ADR-007 | Breathing space BPMN — single definition, dual path | LOCKED |
| ADR-008 | Champion/Challenger — vulnerable exclusion and analytics | LOCKED |
| ADR-009 | Process variable live state rule | LOCKED |
| ADR-010 | Sprint migration sequence and numbering convention | LOCKED |
| ADR-011 | DISPUTE entity suppression column removal | LOCKED |
| ADR-012 | Statute-barred schema fields | LOCKED |
| ADR-013 | ResumeArrangementMonitoringDelegate design | LOCKED |
| ADR-014 | Champion/Challenger analytics architecture | LOCKED |

---

## ADR-001 — CommunicationSuppressionService

**Status:** LOCKED  
**Date:** April 2026

### Decision

`CommunicationSuppressionService` is a singleton service in the
`communications` package. It is the sole authority on whether an
outbound communication may be dispatched. No other class may read
`SUPPRESSION_LOG` for dispatch decisions.

### Interface Contract

```java
interface CommunicationSuppressionService {
    SuppressionResult isPermitted(UUID accountId,
                                  CommunicationCategory category);
    void activateSuppression(UUID accountId,
                             SuppressionReason reason,
                             UUID activatedBy);
    void liftSuppression(UUID accountId,
                         SuppressionReason reason,
                         UUID liftedBy);
    List<SuppressionLog> getActiveSuppressions(UUID accountId);
}
```

### isPermitted() Decision Logic (locked sequence)

```
If category == ESTATE_ADMINISTRATION:
  return PERMITTED unconditionally

If any active suppression reason == DECEASED_MANDATORY:
  block DEBT_COLLECTION and NON_COLLECTION
  (ESTATE_ADMINISTRATION already handled in step 1)

If any active suppression reason in {
  BREATHING_SPACE_STATUTORY,
  MENTAL_HEALTH_CRISIS_STATUTORY,
  INSOLVENCY_STATUTORY }:
  block DEBT_COLLECTION only
  permit NON_COLLECTION

If any active suppression reason in {
  DISPUTE_INTERNAL,
  VULNERABILITY_POLICY }:
  block DEBT_COLLECTION only
  permit NON_COLLECTION

No active suppressions:
  return PERMITTED

Default (unknown category):
  throw UnhandledCommunicationCategoryException
  — Java 21 exhaustive switch enforces at compile time
```

### CommunicationCategory Enum (locked)

`DEBT_COLLECTION | NON_COLLECTION | DUAL_USE | ESTATE_ADMINISTRATION`

### Schema

```sql
CREATE UNIQUE INDEX uix_suppression_log_active
ON suppression_log (account_id, suppression_reason)
WHERE is_active = true;
```

`activateSuppression()` uses `INSERT ... ON CONFLICT DO NOTHING`
(upsert semantics).

### Guardrails — builders must not violate

1. `NotifyAdapter` must call `isPermitted()` as its final operation
   before dispatch. No exceptions.
2. `activateSuppression()` and `liftSuppression()` are the only
   methods that write to `SUPPRESSION_LOG`. Direct repository writes
   are prohibited outside this service.
3. Both activation and lift must write an `AUDIT_EVENT`.
4. The unique partial index must be in the Sprint 3 migration.
5. `DUAL_USE` override at instance level requires `TEAM_LEADER` role
   check and `AUDIT_EVENT` with `event_type = OVERRIDE`.
6. `v_account_suppression_status` view is read-only — for reporting
   and Admin UI visibility only. All dispatch decisions query
   `SUPPRESSION_LOG` directly.

### Alternatives Rejected

- **Suppression as Flowable gateway per BPMN** — rejected; duplication
  risk; does not cover non-Flowable dispatch paths.
- **AOP annotation only** — rejected as primary mechanism; acceptable
  as additive layer only.

---

## ADR-002 — StatuteBarredEvaluator

**Status:** LOCKED  
**Date:** April 2026

### Decision

`StatuteBarredEvaluator` is a singleton service in the `account`
package. It is the sole authority on statute-barred calculation. Both
the nightly `StatuteBarredCalculationJob` and the
`LimitationClockResetEvent` listener call this service — they do not
implement calculation logic themselves.

### Guardrails — builders must not violate

1. `StatuteBarredCalculationJob` checks
   `statute_barred_calculation.enabled` in `SYSTEM_CONFIG` before
   any account evaluation. If `false`, log WARN and exit.
2. `StatuteBarredEvaluator` validates all `limitation_period.*`
   config values on `@PostConstruct`. Application must not start
   with unparseable values.
3. When `is_statute_barred` transitions `true → false`, a
   `STATUTE_BARRED_CLEARED` signal must be sent to any active
   `COLLECTION_PROCESS` instances for the account.
4. `DEBT_STATUS_HISTORY.limitation_clock_reset = true` is set only
   for events defined in RULING-012. Builders must not add new reset
   triggers without a domain ruling.
5. Service task delegates must read `DEBT_ACCOUNT.is_statute_barred`
   from the database at execution time — not from Flowable process
   variables (see ADR-009).

### Limitation Clock Reset Events (RULING-012)

Events that reset the clock:
- `PAYMENT.status = CLEARED` where `payment_method !=
  DEDUCTION_FROM_BENEFIT` (debtor-initiated payment)
- `REPAYMENT_ARRANGEMENT` created with `status = ACTIVE`
- `CASE_NOTE` with `note_type = ACKNOWLEDGEMENT` (formal written
  acknowledgement)

Events that do NOT reset the clock:
- DWP-generated communications
- `DEDUCTION_FROM_BENEFIT` payments (DWP-initiated)
- Agent-only case notes (non-ACKNOWLEDGEMENT types)
- DWP-unilateral account status changes

---

## ADR-003 — Platform-Wide Transaction Boundary Rule

**Status:** LOCKED  
**Date:** April 2026

### Decision

The following transaction boundary rule applies to all features in
this system without exception.

**Rule:** Application database writes (entity state, suppression log,
audit events, status history) are always inside a `@Transactional`
boundary. Flowable engine calls (`RuntimeService`, `TaskService`,
`ExecutionService`) are always outside the application transaction
boundary.

### Rationale

Flowable manages its own transaction on the `flowable` schema.
Nesting a Flowable call inside an application `@Transactional`
creates a two-phase commit problem that Flowable's embedded engine
does not support cleanly. A Flowable exception will roll back the
application transaction, leaving the customer unprotected.

### Transaction Boundary Map

| Operation | In Transaction | Outside Transaction | Compensating Action |
|---|---|---|---|
| Activate breathing space | Set flag + LEGAL_HOLD + SUPPRESSION_LOG + AUDIT_EVENT | Flowable process start | Log MANUAL_REVIEW_REQUIRED; Admin UI manual start |
| Lift breathing space | Lift flag + close SUPPRESSION_LOG + discard queued comms + AUDIT_EVENT | Resume ARRANGEMENT_MONITORING + COLLECTION_PROCESS | Log MANUAL_REVIEW_REQUIRED |
| Set deceased flag | Set flag + SUPPRESSION_LOG + AUDIT_EVENT | Suspend Flowable instances | Log per-instance MANUAL_REVIEW_REQUIRED |
| Write-off approval | Create WRITE_OFF + AUDIT_EVENT + update DEBT_ACCOUNT | Flowable task completion + COMPLIANCE notification | COMPLIANCE notification: retry queue; Flowable: Admin UI |
| Activate suppression | Create SUPPRESSION_LOG + AUDIT_EVENT | None | N/A |
| Lift suppression | Close SUPPRESSION_LOG + AUDIT_EVENT + update COMMUNICATION records | Flowable signal if statute-barred un-bar | Log MANUAL_REVIEW_REQUIRED |
| Joint debt split | Create child accounts + WRITE_OFF + close original + AUDIT_EVENT | Flowable process starts for child accounts | Log MANUAL_REVIEW_REQUIRED |

### Guardrails — builders must not violate

1. Any method that calls a Flowable service must not be annotated
   `@Transactional` or called from within a `@Transactional` method.
2. Flowable call failures must be caught, logged as
   `MANUAL_REVIEW_REQUIRED` in `AUDIT_EVENT`, and must not propagate
   as exceptions that roll back committed application state.
3. Commit application state first, then call Flowable. Never the
   reverse.
4. Exception: `extendArrangementEndDate()` uses
   `@Transactional(propagation = REQUIRES_NEW)` to ensure
   independence from any surrounding Flowable transaction context.

### Alternatives Rejected

- **Single transaction spanning application DB and Flowable** —
  rejected; two-phase commit risk with embedded Flowable.
- **Flowable-first pattern** — rejected; Flowable exception would
  leave application state uncommitted.

---

## ADR-004 — Write-Off Self-Approval Prevention

**Status:** LOCKED  
**Date:** April 2026

### Decision

Three-layer defence. All three layers are required.

**Layer 1 (Primary):** Flowable `CreateTaskListener` on write-off
approval user tasks removes the requestor from the candidate group at
task creation time.

**Layer 2 (Secondary):** `WriteOffService.approve()` validates
`requestedBy != approverId` before calling the DMN. Throws
`SelfApprovalViolationException` if violated.

**Layer 3 (Tertiary):** Database CHECK constraint:
`CHECK (requested_by != approved_by)` on `WRITE_OFF` table.

### Guardrails — builders must not violate

1. `self_approval_check` is NOT an input to
   `write_off_limit_check.dmn`. The DMN scope is amount and role
   routing only.
2. The `CreateTaskListener` is non-negotiable. Service layer check
   alone is insufficient.
3. After removing the requestor from the candidate group, the listener
   must check if the group is now empty. If empty:
   - Do NOT remove the requestor
   - Restore original candidate group
   - Write `AUDIT_EVENT` with `event_type = OVERRIDE`,
     `notes = "Self-approval conflict: only candidate is the
     requestor. Manual resolution required."`
   - Notify COMPLIANCE via `CommunicationService`
   - Write `MANUAL_REVIEW_REQUIRED` audit event
4. The SRO Keycloak placeholder role must be created in the
   pre-build phase. The write-off approval process must not have an
   empty candidate group at any tier.
5. All write-off thresholds are provisional pending DDE-OQ-02.
   Values are stored in `SYSTEM_CONFIG` and must not be hard-coded.

### Alternatives Rejected

- **DMN-only enforcement** — rejected; DMN self-approval input is a
  tautology (the service layer must compute the boolean before
  calling the DMN; the DMN adds no independent check).
- **Service layer only** — rejected; does not prevent task assignment
  to the requestor before the service check fires, leaving a Flowable
  process instance in an error state on violation.

---

## ADR-005 — DeceasedPartyHandler Two-Phase Atomicity

**Status:** LOCKED  
**Date:** April 2026

### Decision

`DeceasedPartyHandler` executes in two phases. Phase 1 is
transactional and must complete before Phase 2 begins. Phase 2 is
non-transactional.

### Phase 1 — Transactional (must not fail silently)

```java
@Transactional
private void setDeceasedFlagAndSuppression(
    UUID partyId, UUID confirmedBy) {

  party.setDeceasedFlag(true);
  party.setDeceasedFlaggedAt(Instant.now());
  party.setDeceasedFlaggedBy(confirmedBy);

  suppressionLogService.activate(
    accountIds, DECEASED_MANDATORY, confirmedBy);

  auditService.log(DECEASED_FLAG_SET, partyId, confirmedBy);
  // Transaction commits here before Flowable is touched
}
```

### Phase 2 — Non-transactional (failures logged, not propagated)

```java
private void suspendFlowableInstances(List<UUID> accountIds) {
  for (UUID accountId : accountIds) {

    boolean isJointHolder = jointDebtLinkRepository
      .hasActiveJointLink(accountId);

    if (isJointHolder) {
      // Do NOT suspend — surviving party liability continues
      workQueueService.createSpecialistReviewTask(accountId,
        "Joint account holder deceased — review required");
      caseNoteService.addSystemNote(accountId,
        "Deceased flag set on joint account holder.");
      continue;
    }

    List<ProcessCaseLink> links = processCaseLinkRepository
      .findActiveByAccountId(accountId);

    for (ProcessCaseLink link : links) {
      try {
        runtimeService.suspendProcessInstanceById(
          link.getFlowableProcessInstanceId());
        auditService.log(PROCESS_SUSPENDED,
          link.getFlowableProcessInstanceId());
      } catch (FlowableObjectNotFoundException e) {
        auditService.log(MANUAL_REVIEW_REQUIRED,
          link.getFlowableProcessInstanceId(),
          "Process completed before suspension — verify state");
      } catch (Exception e) {
        auditService.log(MANUAL_REVIEW_REQUIRED,
          link.getFlowableProcessInstanceId(),
          e.getMessage());
      }
    }
  }
}
```

### Guardrails — builders must not violate

1. Phase 1 is `@Transactional`. Phase 2 has no transaction
   annotation. They must be separate methods.
2. A failure in Phase 2 must never cause a retry of Phase 1. The
   deceased flag is set once.
3. `JOINT_DEBT_LINK` must be queried before any suspension decision.
   Sole-holder assumption is not permitted.
4. Joint-holder accounts: do not suspend process instances; create
   `SPECIALIST_AGENT` `WORK_QUEUE` task only.
5. Active `REPAYMENT_ARRANGEMENT` records: set `status = SUSPENDED`,
   not `CANCELLED`.
6. `PARTY` must have `deceased_flagged_at` and `deceased_flagged_by`
   fields populated in Phase 1. These support the Phase 2 (post-demo)
   `unflag_deceased` operation.

### Alternatives Rejected

- **Single transaction including Flowable suspension** — rejected;
  Flowable `ObjectNotFoundException` on a completing process instance
  would roll back the deceased flag, leaving the customer unprotected.

---

## ADR-006 — Flowable Admin UI Access Control and Audit

**Status:** LOCKED  
**Date:** April 2026

### Decision

Flowable Admin UI capabilities are split across two Keycloak roles.

| Capability | OPS_MANAGER | FLOWABLE_ADMIN |
|---|---|---|
| View process instances (read-only) | Yes | Yes |
| Edit DMN rules | Yes | Yes |
| Direct process instance manipulation | No | Yes |
| Task reassignment (operational) | Yes (via app API) | Yes |

`FLOWABLE_ADMIN` is assigned to the technical system administrator
only. For the demo this is the Lead Developer.

### HistoryEventProcessor Scope

Events written to `AUDIT_EVENT`:

| Event Type | Condition |
|---|---|
| TASK_COMPLETED | Only when programmatic flag NOT set (i.e. Admin UI completion) |
| VARIABLE_UPDATED | Always |
| PROCESS_INSTANCE_SUSPENDED | Always |
| PROCESS_INSTANCE_ACTIVATED | Always |
| PROCESS_INSTANCE_DELETED | Always |
| PROCESS_INSTANCE_MIGRATED | Always |

Routine process advancement events (timer fires, service task
execution) are not duplicated into `AUDIT_EVENT` — they remain in
Flowable's own history tables.

### Thread-Local Flag Pattern

```java
public final class FlowableOperationContext {

  private static final ThreadLocal<Boolean> PROGRAMMATIC
    = new ThreadLocal<>();
  // Standard ThreadLocal — NOT InheritableThreadLocal.
  // Async Flowable job executor threads do not inherit
  // this value, which is correct behaviour.

  public static void setProgrammatic(boolean value) {
    PROGRAMMATIC.set(value);
  }

  public static boolean isProgrammatic() {
    return Boolean.TRUE.equals(PROGRAMMATIC.get());
  }

  public static void clear() {
    PROGRAMMATIC.remove();
  }
}

// Usage pattern in all TaskService calls:
FlowableOperationContext.setProgrammatic(true);
try {
  taskService.complete(taskId, variables);
} finally {
  FlowableOperationContext.clear(); // always in finally
}
```

### AUDIT_EVENT Fields for HistoryEventProcessor Entries

```
entity_type  = FLOWABLE_PROCESS_INSTANCE or FLOWABLE_TASK
entity_id    = process instance ID or task ID (UUID)
event_type   = OVERRIDE
actor_id     = 00000000-0000-0000-0000-000000000001 (SYSTEM)
               — Admin UI user identity in Keycloak access log
occurred_at  = timestamp from Flowable history event
```

### Guardrails — builders must not violate

1. `FlowableOperationContext` uses standard `ThreadLocal` — not
   `InheritableThreadLocal`.
2. `FlowableOperationContext.clear()` must always be in a `finally`
   block — never in the try block only.
3. `HistoryEventProcessor` must be registered as a Spring bean.
   Flowable auto-detects it via
   `org.flowable.engine.impl.history.HistoryEventHandler`.
4. Admin UI actor identity limitation must be documented in the
   compliance audit screen — evaluators must understand that Admin UI
   operations show SYSTEM actor with Keycloak log cross-reference.
5. `HistoryEventProcessor` implementation must be complete before
   Sprint 5 when breathing space and write-off processes go live.

---

## ADR-007 — Breathing Space BPMN Pattern

**Status:** LOCKED  
**Date:** April 2026

### Decision

Single BPMN definition (`breathing_space_process.bpmn20.xml`) with
an exclusive gateway routing to two paths. One process definition key.

### STANDARD Path Timer

**Intermediate timer catch event** — not a boundary event.

```
Service Tasks (activate flags, suppression, suspend arrangement)
    → Intermediate Timer Catch Event: ${breathingSpaceEndDateISO}
      [process token waits here — Flowable async executor fires
       after PT1440H and advances the token]
    → DiscardQueuedCollectionCommunicationsDelegate
    → LiftBreathingSpaceSuppressionDelegate
    → ResumeArrangementMonitoringDelegate
    → User Task: Post-review [SPECIALIST_AGENT]
    → End Event
```

### MENTAL_HEALTH_CRISIS Path

No timer. Manual resolution only.

```
Service Tasks (activate flags, suppression, suspend arrangement)
    → User Task: Awaiting MH confirmation [SPECIALIST_AGENT]
      Boundary Escalation Timer: ${mhcEscalationDays}d
      → [Escalation] User Task: MH Crisis Review [OPS_MANAGER]
    → [Confirmed]
    → DiscardQueuedCollectionCommunicationsDelegate
    → LiftBreathingSpaceSuppressionDelegate
    → ResumeArrangementMonitoringDelegate
    → End Event
```

### Guardrails — builders must not violate

1. The STANDARD path timer is an **intermediate timer catch event**,
   not a boundary event on a task. A boundary event on a service task
   will either fire immediately or never — this is a critical
   implementation error.
2. `LEGAL_HOLD.end_date` must be set conditionally:
   - STANDARD: `end_date = today + 60 days`
   - MENTAL_HEALTH_CRISIS: `end_date = NULL`
   A non-null end date on a mental health crisis hold is a data
   integrity violation.
3. `DiscardQueuedCollectionCommunicationsDelegate` must execute
   before the suppression lift service task on both expiry paths.
4. `ResumeArrangementMonitoringDelegate` must execute after
   suppression is lifted, not before.
5. Verify intermediate timer catch event behaviour in the pre-build
   Flowable spike before Sprint 5.

### Alternatives Rejected

- **Two separate BPMN process definitions** — rejected; creates
  complexity in `PROCESS_CASE_LINK` queries, monitoring, and demo
  presentation.
- **Event sub-process for timer** — rejected; advanced Flowable
  pattern with marginal gain at this scale.

---

## ADR-008 — Champion/Challenger Vulnerable Customer Policy

**Status:** LOCKED  
**Date:** April 2026

### Decision

Champion/challenger protected-population handling is governed by the
active champion/challenger policy version. The current approved policy
assigns vulnerable customers to the CHAMPION variant regardless of test
split. This is enforced in `champion_challenger_assignment.dmn` or the
equivalent effective-dated assignment policy as priority rule 1.
`STRATEGY_ASSIGNMENT.vulnerability_override = true` is recorded when
this current policy rule fires.

Accounts with `vulnerability_override = true` are excluded from the
A/B comparison analytics panel. They appear in the vulnerable account
outcomes panel only under the current approved policy.

### Guardrails — builders must not violate

1. The active champion/challenger assignment policy must be evaluated
   before split logic.
2. Under the current approved policy, a vulnerable customer must not
   appear in the CHALLENGER variant. Any change to this rule is a Class
   A policy change requiring effective dating, rationale, approval
   evidence, and DWP sign-off where applicable.
3. The A/B comparison analytics query must include the active policy's
   A/B inclusion rule. Under the current approved policy this is
   `WHERE sa.vulnerability_override = false`.
4. `STRATEGY_OUTCOME_METRIC` records are written for all accounts
   including vulnerable — do not suppress metric collection.
   Exclusion is at query level only.
5. `STRATEGY_TEST.min_sample_size` and `min_duration_days` fields
   must be in the Sprint 3 migration (nullable for demo). The promote
   winner API must check these fields even when null.
6. Every assignment must record the champion/challenger policy version
   used and the policy decision snapshot that produced the arm.

### Alternatives Rejected

- **Silently include vulnerable accounts in CHAMPION analytics under
  the current policy** — rejected;
  vulnerable customers may have systematically different payment
  behaviour, which would bias the CHAMPION metric and make the A/B
  comparison misleading.
- **Ordinary per-experiment toggle for vulnerable challenger
  eligibility** — rejected; protected-population eligibility must be a
  governed policy change, not a setup-screen checkbox.

---

## ADR-009 — Process Variable Live State Rule

**Status:** LOCKED  
**Date:** April 2026

### Decision

Flowable process variables set at process start are read-only
snapshots used for initial routing only. They must not be used for
mid-process business decisions on live account state.

### Fields That Must Be Read from the Database

Any service task delegate that makes a routing or decision based on
the following fields must read directly from the database entity at
execution time:

- `DEBT_ACCOUNT.breathing_space_flag`
- `DEBT_ACCOUNT.deceased_flag`
- `DEBT_ACCOUNT.is_statute_barred`
- `DEBT_ACCOUNT.benefit_status`
- `VULNERABILITY_ASSESSMENT.status`

### Required Delegate Pattern

```java
@Override
public void execute(DelegateExecution execution) {
  UUID accountId = (UUID) execution.getVariable("accountId");

  DebtAccount account = debtAccountRepository
    .findById(accountId)
    .orElseThrow(() -> new AccountNotFoundException(accountId));

  // Use account.getBreathingSpaceFlag() etc.
  // NOT execution.getVariable("breathingSpaceFlag")
}
```

### Guardrails — builders must not violate

1. No delegate that makes a business decision on the listed fields
   may use `execution.getVariable()` for those fields.
2. Process variables for these fields may be set at process start for
   logging or tracing purposes only.
3. This rule applies to all nine sprints without exception.
4. Production note: at production scale, a request-scoped cache may
   reduce repeated DB reads within a single job execution. This is a
   post-award performance optimisation — do not implement for demo.

### Rationale

Flowable process variables persist in `ACT_RU_VARIABLE` for the
lifetime of the process instance. A variable set at process start
will be stale by the time a long-running process timer fires. Using
stale state for compliance-sensitive decisions (breathing space,
deceased, statute-barred) is a regulatory risk.

---

## ADR-010 — Sprint Migration Sequence and Numbering Convention

**Status:** LOCKED  
**Date:** April 2026

### Decision

#### Sprint 1 Migration Sequence

| Migration | Content |
|---|---|
| V001 | TEAM (without manager_id FK) |
| V002 | USER (with team_id FK TEAM) |
| V003 | TEAM ALTER — add manager_id FK USER |
| V004 | PARTY |
| V005 | PARTY_ADDRESS, PARTY_CONTACT |
| V006 | DEBT_ACCOUNT (incl. cause_of_action_date, last_acknowledgement_date, is_statute_barred BOOLEAN NOT NULL DEFAULT false, breathing_space_type ENUM, deceased_flagged_at, deceased_flagged_by) |
| V007 | AUDIT_EVENT (all event types) |
| V008 | DEBT_STATUS_HISTORY (incl. limitation_clock_reset) |
| V009 | SUPPRESSION_LOG (full FK constraints; unique partial index) |
| V900 | SYSTEM user seed record (id = 00000000-0000-0000-0000-000000000001) |

#### Sprint Block Reservation

| Sprint | Block |
|---|---|
| 1 | V001–V009 |
| 2 | V010–V019 |
| 3 | V020–V029 |
| 4 | V030–V039 |
| 5 | V040–V049 |
| 6 | V050–V059 |
| 7 | V060–V069 |
| 8 | V070–V079 |
| 9 | V080–V089 |
| Seed | V900+ (separate Flyway location) |

#### Within-Sprint Number Assignment

- Lead Developer: odd numbers within the block
  (e.g. Sprint 3: V021, V023, V025…)
- Dev 2: even numbers within the block
  (e.g. Sprint 3: V020, V022, V024…)

Numbers are claimed on the GitHub sprint board before the file is
created — not after.

If more than 5 migrations are needed per developer per sprint, the
sprint scope is too large — raise with Delivery Lead.

### Guardrails — builders must not violate

1. Migration numbers must be taken from the sprint's reserved block
   only.
2. No migration may be created outside the reserved block without SA
   approval.
3. Seed migrations are in a separate Flyway location
   (`db/migration/seed`) — not mixed with schema migrations.
4. `USER.keycloak_user_id` is nullable at DB level to accommodate the
   SYSTEM user record. Non-null is enforced at service layer for all
   human users.
5. The SYSTEM user UUID
   (`00000000-0000-0000-0000-000000000001`) is the `actor_id` for
   all automated `AUDIT_EVENT` entries.

### Flyway Location Configuration

```yaml
spring:
  flyway:
    locations:
      - classpath:db/migration/schema
      - classpath:db/migration/seed
    schemas: public, flowable
```

Seed migrations use prefix `V900` onward to ensure they sort after
all schema migrations.

---

## ADR-011 — DISPUTE Entity Suppression Column Removal

**Status:** LOCKED  
**Date:** April 2026

### Decision

`DISPUTE.communications_suppressed` boolean column is removed in the
Sprint 3 Flyway migration. Communication suppression for disputes is
managed exclusively via `SUPPRESSION_LOG` with reason
`DISPUTE_INTERNAL`.

### Sprint 3 Migration

```sql
-- In Sprint 3 block (V020–V029)
ALTER TABLE dispute DROP COLUMN communications_suppressed;
```

### Replacement Pattern

On dispute creation:
`ActivateDisputeSuppressionDelegate` calls
`CommunicationSuppressionService.activateSuppression(
  accountId, DISPUTE_INTERNAL, agentId)`

On dispute resolution:
`LiftDisputeSuppressionDelegate` calls
`CommunicationSuppressionService.liftSuppression(
  accountId, DISPUTE_INTERNAL, agentId)`

### Guardrails — builders must not violate

1. No code written after Sprint 3 may reference
   `DISPUTE.communications_suppressed`.
2. Sprint 5 dispute stories must reference
   `CommunicationSuppressionService` only.
3. The two new delegates follow the naming convention:
   `ActivateDisputeSuppressionDelegate` and
   `LiftDisputeSuppressionDelegate`.

---

## ADR-012 — Statute-Barred Schema Fields

**Status:** LOCKED  
**Date:** April 2026

### Decision

Three fields are added to `DEBT_ACCOUNT` in the Sprint 1 V006
migration.

```sql
cause_of_action_date      DATE,
last_acknowledgement_date DATE,
is_statute_barred         BOOLEAN NOT NULL DEFAULT false
```

### Field Rules

| Field | DB Constraint | Application Constraint |
|---|---|---|
| `cause_of_action_date` | Nullable | Required in `CreateDebtAccountRequest` DTO; service layer validation |
| `last_acknowledgement_date` | Nullable | Set only by `StatuteBarredEvaluator` on RULING-012 events |
| `is_statute_barred` | NOT NULL DEFAULT false | Set only by `StatuteBarredEvaluator` |

### Account Detail Screen

`is_statute_barred` is visible as a read-only field from Sprint 1:

- Job disabled (`statute_barred_calculation.enabled = false`):
  display "Statute review pending"
- `is_statute_barred = true`: display "Statute barred"
- `is_statute_barred = false` and job enabled: field blank

### Migration Data Quality Gate

`MIGRATION_RECORD.status = FAILED` if `cause_of_action_date` cannot
be derived from source data. The account is still migrated but
flagged for manual review.

### Guardrails — builders must not violate

1. `CreateDebtAccountRequest` must validate `cause_of_action_date`
   as non-null.
2. `is_statute_barred` is never set directly by application code
   outside `StatuteBarredEvaluator`.
3. `last_acknowledgement_date` is never set directly — only via
   `StatuteBarredEvaluator.onLimitationClockReset()`.
4. Demo seed data must set `cause_of_action_date` to a date well
   within any plausible limitation period. No seed account should be
   near statute-barred status.

---

## ADR-013 — ResumeArrangementMonitoringDelegate Design

**Status:** LOCKED  
**Date:** April 2026

### Decision

`ResumeArrangementMonitoringDelegate` handles all resume scenarios
for `ARRANGEMENT_MONITORING_PROCESS` after a breathing space
moratorium. It reads the `arrangementMonitoringStep` process variable
to determine the correct resume behaviour.

### Required Process Variables

| Variable | Type | Set By | Purpose |
|---|---|---|---|
| `arrangementMonitoringStep` | String | Execution listener at each step | Current step: AWAITING_TIMER \| CHECKING_PAYMENT \| BREACH_EVALUATION \| AGENT_CONTACT_TASK |
| `nextDueDateISO` | String (ISO-8601 date) | Process start; updated by delegate | Timer catch event date expression — `${nextDueDateISO}` |
| `arrangementSuspendedAt` | String (ISO-8601 datetime) | `ActivateBreathingSpaceSuppressionDelegate` | Set BEFORE suspension; used to calculate moratorium duration |
| `skipBreachOnResume` | Boolean (default false) | Delegate on resume | Prevents false breach on moratorium-interrupted cycle |

### Cross-Process Variable Setting Pattern

`ActivateBreathingSpaceSuppressionDelegate` must set
`arrangementSuspendedAt` on the `ARRANGEMENT_MONITORING_PROCESS`
instance before suspending it:

```java
List<ProcessCaseLink> links = processCaseLinkRepository
  .findActiveByAccountIdAndProcessKey(
    accountId,
    "arrangement-monitoring-process");

links.forEach(link -> {
  // Set variable on the OTHER process instance
  runtimeService.setVariable(
    link.getFlowableProcessInstanceId(),
    "arrangementSuspendedAt",
    LocalDateTime.now().toString());

  // Then suspend
  runtimeService.suspendProcessInstanceById(
    link.getFlowableProcessInstanceId());
});
```

### Delegate Logic

```java
public void execute(DelegateExecution execution) {

  String step = (String) execution
    .getVariable("arrangementMonitoringStep");
  String suspendedAtStr = (String) execution
    .getVariable("arrangementSuspendedAt");

  if (suspendedAtStr == null) {
    // Not suspended via breathing space — Admin UI resume
    auditService.log(MANUAL_REVIEW_REQUIRED,
      execution.getProcessInstanceId(),
      "Arrangement monitoring resumed without " +
      "arrangementSuspendedAt — manual schedule " +
      "verification required");
    execution.setVariable("skipBreachOnResume", false);
    return;
  }

  long moratoriumDays = ChronoUnit.DAYS.between(
    LocalDateTime.parse(suspendedAtStr),
    LocalDateTime.now());

  switch (step) {
    case "AWAITING_TIMER" -> {
      LocalDate originalDate = LocalDate.parse(
        (String) execution.getVariable("nextDueDateISO"));
      LocalDate newDate = originalDate.plusDays(moratoriumDays);
      execution.setVariable("nextDueDateISO",
        newDate.toString());
      execution.setVariable("skipBreachOnResume", false);
      extendArrangementEndDate(
        (UUID) execution.getVariable("accountId"),
        moratoriumDays);
    }
    case "CHECKING_PAYMENT" -> {
      execution.setVariable("skipBreachOnResume", true);
      extendArrangementEndDate(
        (UUID) execution.getVariable("accountId"),
        moratoriumDays);
    }
    case "BREACH_EVALUATION", "AGENT_CONTACT_TASK" -> {
      // Pre-moratorium breach — preserve; do not extend
      execution.setVariable("skipBreachOnResume", false);
    }
  }

  execution.setVariable("arrangementSuspendedAt", null);
}

@Transactional(propagation = REQUIRES_NEW)
public void extendArrangementEndDate(
    UUID accountId, long moratoriumDays) {

  RepaymentArrangement arrangement =
    repaymentArrangementRepository
      .findActiveByAccountId(accountId);

  arrangement.setEndDate(
    arrangement.getEndDate().plusDays(moratoriumDays));

  SuppressionResult result =
    communicationSuppressionService.isPermitted(
      accountId, NON_COLLECTION);

  if (result.isPermitted()) {
    communicationService.generateRevisedScheduleNotice(
      accountId, arrangement.getEndDate());
  } else {
    auditService.log(MANUAL_REVIEW_REQUIRED, accountId,
      "Revised arrangement schedule notice suppressed " +
      "by active suppression. Manual notification required.");
  }
}
```

### Guardrails — builders must not violate

1. `extendArrangementEndDate()` must use
   `@Transactional(propagation = REQUIRES_NEW)` — independent of
   any surrounding Flowable transaction context.
2. `arrangementSuspendedAt` must be set on the
   `ARRANGEMENT_MONITORING_PROCESS` instance via
   `runtimeService.setVariable()` before
   `suspendProcessInstanceById()` is called. Sequence is
   non-negotiable.
3. The timer catch event duration expression must be
   `${nextDueDateISO}` — not a hard-coded ISO duration.
4. Admin UI resume (null `arrangementSuspendedAt`) generates a
   `MANUAL_REVIEW_REQUIRED` audit event. Do not attempt date
   recalculation without `arrangementSuspendedAt`.
5. Pre-moratorium breach records (`ARRANGEMENT_BREACH`) are
   preserved on resume — do not clear them.
6. `generateRevisedScheduleNotice()` failure due to suppression
   must log `MANUAL_REVIEW_REQUIRED` — it must not throw an
   exception or roll back the end date extension.

---

## ADR-014 — Champion/Challenger Analytics Architecture

**Status:** LOCKED  
**Date:** April 2026

### Decision

The analytics dashboard presents three panels. Under the current
approved champion/challenger policy, vulnerable accounts
(`vulnerability_override = true`) are excluded from the A/B comparison
panel and appear in the vulnerable outcomes panel only. Future policy
versions may define different protected-population inclusion rules, but
they must be represented explicitly in the analytics query contract and
approval evidence.

### Panel Definitions

**Panel 1 — A/B Comparison (policy-eligible comparison population):**

```sql
SELECT sa.variant,
  COUNT(DISTINCT sa.account_id) AS accounts,
  SUM(som.value)
    FILTER (WHERE som.metric_type = 'PAYMENT_MADE')
    AS total_recovered,
  COUNT(*)
    FILTER (WHERE som.metric_type = 'ARRANGEMENT_AGREED')
    AS arrangements,
  AVG(som.value)
    FILTER (WHERE som.metric_type = 'PAYMENT_MADE')
    AS avg_payment
FROM strategy_assignment sa
LEFT JOIN strategy_outcome_metric som
  ON som.strategy_assignment_id = sa.id
WHERE sa.strategy_test_id = :testId
  AND sa.vulnerability_override = false
GROUP BY sa.variant;
```

The `vulnerability_override = false` predicate is the current approved
policy rule. If a future approved policy permits a protected population
into challenger treatment, Panel 1 must use that policy version's
explicit comparison-eligibility predicate and record the policy version
in query/audit metadata.

**Panel 2 — Vulnerable Account Outcomes:**

```sql
SELECT
  COUNT(DISTINCT sa.account_id) AS vulnerable_accounts,
  SUM(som.value)
    FILTER (WHERE som.metric_type = 'PAYMENT_MADE')
    AS total_recovered,
  COUNT(*)
    FILTER (WHERE som.metric_type = 'ARRANGEMENT_AGREED')
    AS arrangements
FROM strategy_assignment sa
LEFT JOIN strategy_outcome_metric som
  ON som.strategy_assignment_id = sa.id
WHERE sa.strategy_test_id = :testId
  AND sa.vulnerability_override = true;
```

**Panel 3 — Overall Population:**

```sql
SELECT
  COUNT(DISTINCT sa.account_id) AS total_accounts,
  SUM(som.value)
    FILTER (WHERE som.metric_type = 'PAYMENT_MADE')
    AS total_recovered
FROM strategy_assignment sa
LEFT JOIN strategy_outcome_metric som
  ON som.strategy_assignment_id = sa.id
WHERE sa.strategy_test_id = :testId;
```

### Promote Winner Gate

`STRATEGY_TEST` fields:

```sql
min_sample_size  INT,  -- nullable; populated post-award
min_duration_days INT  -- nullable; populated post-award
```

The promote winner API checks these fields before allowing
`winner_strategy_id` to be set. If null, no gate is enforced (demo
behaviour). Post-award values loaded per DDE-OQ-09.

### Guardrails — builders must not violate

1. Panel 1 query must include the active policy's
   comparison-eligibility predicate. Under the current approved policy
   this is `WHERE sa.vulnerability_override = false`.
2. `STRATEGY_OUTCOME_METRIC` records are written for all accounts
   including vulnerable. Do not suppress metric collection.
   Exclusion is at query level only.
3. `STRATEGY_TEST.min_sample_size` and `min_duration_days` must be
   in the Sprint 3 migration as nullable columns.
4. The promote winner API must check these fields even when null.
5. Under the current approved policy, vulnerable accounts must not
   appear in Panel 1 data — including edge cases where
   `vulnerability_override` was set after assignment. Any future change
   requires a governed champion/challenger policy version and updated
   analytics evidence.

---

## Domain Rulings Referenced by ADRs

The following domain rulings produced by the DWP Debt Domain Expert
are referenced by the ADRs above. Full ruling documents are stored in
`docs/project-foundation/domain-rulings/`.

| Ruling | Title | Referenced By |
|---|---|---|
| RULING-001 | Breathing space communications suppression scope | ADR-001, ADR-007 |
| RULING-002 | Vulnerability data classification and access control | ADR-009 |
| RULING-003 | Write-off delegated authority and self-approval prohibition | ADR-004 |
| RULING-004 | Joint debt split legal constraints | Development Plan UC-10 |
| RULING-005 | Breathing space and repayment arrangement interaction | ADR-007, ADR-013 |
| RULING-006 | Deceased party mandatory handling | ADR-005 |
| RULING-007 | I&E assessment affordability obligations | Development Plan UC-02 |
| RULING-008 | DCA pre-placement disclosure obligations | Development Plan UC-07 |
| RULING-009 | Statute-barred debt | ADR-002, ADR-012 |
| RULING-010 | Champion/Challenger Consumer Duty implications | ADR-008 |
| RULING-011 | Queued communication disposition on suppression lift | ADR-001 |
| RULING-012 | Limitation clock reset events | ADR-002, ADR-012 |

## Open Post-Award Items

The following items were resolved for demo scope but require
confirmed values or policy decisions post-award:

| ID | Item | Affects |
|---|---|---|
| DDE-OQ-02 | Confirmed write-off delegated authority thresholds | ADR-004; write_off_limit_check.dmn; SYSTEM_CONFIG |
| DDE-OQ-03 | Correct escalation role above OPS_MANAGER | ADR-004; SRO Keycloak role |
| DDE-OQ-06 | I&E required for benefit deduction arrangements? | Development Plan UC-02 |
| DDE-OQ-07 | Required DCA pre-placement notice period | Development Plan UC-07; SYSTEM_CONFIG |
| DDE-OQ-08 | Applicable limitation period per debt type | ADR-002; ADR-012; SYSTEM_CONFIG |
| DDE-OQ-09 | Minimum A/B test duration and sample size | ADR-014; STRATEGY_TEST fields |
| DDE-OQ-11 | I&E staleness period (DWP policy vs 12-month default) | Development Plan UC-02 |
| RULING-012 partial | Whether PROMISE_TO_PAY constitutes written acknowledgement | ADR-002 |
