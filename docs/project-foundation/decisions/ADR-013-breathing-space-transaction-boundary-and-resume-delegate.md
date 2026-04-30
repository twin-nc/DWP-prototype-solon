# ADR-013: Platform-wide Flowable Transaction Boundary Rule and Breathing Space Resume Delegate Design

## Status

Accepted

## Date

2026-04-24

## Context

Two related gaps exist in the authority ADR set:

**Gap 1 — Transaction boundary rule has no home.**
ADR-011 (line 171) references "ADR-003: Platform-wide transaction boundary rule (Flowable calls outside application transaction)" but ADR-003 contains no such content. The rule exists in the donor source artifact but has never been promoted into an authority ADR. This ADR closes that gap and repairs the broken ADR-011 reference.

**Gap 2 — Breathing space resume semantics are not documented.**
ADR-002 establishes that breathing space is a process engine state handled by an interrupting event subprocess. RULING-005 rules 3–5 establish that `ARRANGEMENT_MONITORING_PROCESS` must be suspended (not cancelled), must not trigger breach during the moratorium, and must resume from its last valid state. No authority ADR documents how the resume delegate implements these rules, how the moratorium duration is applied, or what happens when the Admin UI resumes an instance without a recorded suspension timestamp.

Both gaps are addressed here. The two decisions are closely related: the `REQUIRES_NEW` exception in Decision 2 is only coherent in the context of the platform-wide transaction rule in Decision 1.

Requirements addressed:
- DW.18 — eliminate blackholes (accounts lost in workflow)
- DW.42 — trace outcome back through decisioning logic
- DW.45 — flag deceased, vulnerable, insolvency, fraud, breathing space
- DW.49 — account can enter, exit, pause within a workflow/treatment
- CAS.BS-05 — arrangement monitoring during moratorium
- CAS.BS-06 — breach suppression during moratorium

## Decision

### Decision 1 — Platform-wide Flowable Transaction Boundary Rule

**Application state must be committed before any Flowable service call. Flowable calls must not occur inside an `@Transactional` boundary.**

Flowable uses its own transaction internally. Wrapping a Flowable call inside a Spring `@Transactional` boundary creates a two-resource transaction without XA coordination. If the Spring transaction rolls back after the Flowable call has already committed process state, the application DB and the process engine are left inconsistent with no recovery path.

The correct pattern is:

1. Commit all application state changes (entity writes, audit events, suppression log entries) in a dedicated `@Transactional` method.
2. Outside that transaction, call the Flowable service.
3. If the Flowable call fails, catch the exception, log `MANUAL_REVIEW_REQUIRED` to `AUDIT_EVENT`, and do not rethrow. The committed application state stands; a human resolves the Flowable state via the Admin UI.

**Never reverse this order.** Calling Flowable first and then committing application state means a DB failure leaves a process instance running with no corresponding application record.

#### Transaction Boundary Table

| Operation | In `@Transactional` | Outside transaction | Compensating action on Flowable failure |
|---|---|---|---|
| Activate breathing space | Set flag + LEGAL_HOLD + SUPPRESSION_LOG + AUDIT_EVENT | Flowable process start (breathing space process) | Log `MANUAL_REVIEW_REQUIRED`; operator starts process manually via Admin UI |
| Lift breathing space | Lift flag + close SUPPRESSION_LOG + discard queued comms + AUDIT_EVENT | Resume `ARRANGEMENT_MONITORING_PROCESS` + `COLLECTION_PROCESS` | Log `MANUAL_REVIEW_REQUIRED` |
| Set deceased flag | Set flag + SUPPRESSION_LOG + AUDIT_EVENT | Suspend all active Flowable instances for the Person | Log per-instance `MANUAL_REVIEW_REQUIRED` |
| Write-off approval | Create WRITE_OFF record + AUDIT_EVENT + update DEBT_ACCOUNT | Flowable task completion + COMPLIANCE notification | COMPLIANCE notification: retry queue; Flowable task: Admin UI manual completion |
| Activate suppression | Create SUPPRESSION_LOG + AUDIT_EVENT | None | N/A |
| Lift suppression | Close SUPPRESSION_LOG + AUDIT_EVENT + update COMMUNICATION records | Flowable signal if statute-barred un-bar | Log `MANUAL_REVIEW_REQUIRED` |
| Joint debt split | Create child accounts + WRITE_OFF + close original + AUDIT_EVENT | Flowable process starts for child accounts | Log `MANUAL_REVIEW_REQUIRED` per failed start |

#### Exception: `REQUIRES_NEW` for isolated domain writes triggered from within Flowable

Some domain writes are initiated from inside a Flowable delegate execution. The delegate itself runs inside Flowable's own transaction context. A domain write that must be independent of that context (so that it commits regardless of subsequent Flowable execution outcome) must use `@Transactional(propagation = REQUIRES_NEW)`.

`extendArrangementEndDate()` (Decision 2) is the canonical example: it extends the `RepaymentArrangement` end date as a consequence of moratorium lift. This write must commit whether or not subsequent BPMN steps succeed, because it records a regulatory fact about the arrangement. `REQUIRES_NEW` creates an independent DB transaction that commits before the surrounding Flowable transaction concludes.

This is the only sanctioned use of `REQUIRES_NEW` in this codebase. Other uses require explicit architecture review.

#### Guardrails

1. No method that calls a Flowable service (`RuntimeService`, `TaskService`, `ManagementService`, etc.) may be annotated `@Transactional` or invoked from within an active `@Transactional` boundary.
2. Flowable call failures must be caught at the call site, logged as `MANUAL_REVIEW_REQUIRED` in `AUDIT_EVENT` with the operation type and affected entity IDs, and must not propagate as exceptions that roll back committed application state.
3. Commit application state first, then call Flowable. Deviation from this order requires explicit architecture sign-off and must be documented in the PR as a named exception.

---

### Decision 2 — ResumeArrangementMonitoringDelegate Design

`ResumeArrangementMonitoringDelegate` is the Flowable `JavaDelegate` that executes when `ARRANGEMENT_MONITORING_PROCESS` resumes after a breathing space moratorium. It implements RULING-005 rules 3–5 and the "arrangement expiry date falls within moratorium" edge case.

The delegate reads `arrangementMonitoringStep` to determine where in the monitoring cycle the process was when it was suspended, and applies the appropriate resume behaviour for each case.

#### Required Process Variables

| Variable | Type | Set by | Purpose |
|---|---|---|---|
| `arrangementMonitoringStep` | String | Execution listener at each monitoring step | Current position: `AWAITING_TIMER` \| `CHECKING_PAYMENT` \| `BREACH_EVALUATION` \| `AGENT_CONTACT_TASK` |
| `nextDueDateISO` | String (ISO-8601 date) | Process start; updated by this delegate on resume | Date expression for the timer catch event: `${nextDueDateISO}` |
| `arrangementSuspendedAt` | String (ISO-8601 datetime) | `ActivateBreathingSpaceSuppressionDelegate`, set before suspension | Used to calculate moratorium duration on resume; null if process was not suspended via breathing space |
| `skipBreachOnResume` | Boolean (default `false`) | This delegate on resume | Prevents false breach evaluation on the first cycle after moratorium — set `true` only when the monitoring cycle was interrupted mid-payment-check |

#### Cross-Process Variable Setting Pattern

`ActivateBreathingSpaceSuppressionDelegate` executes inside the breathing space process (`breathing_space_process`). Before suspending the `ARRANGEMENT_MONITORING_PROCESS` instance, it must write `arrangementSuspendedAt` directly onto that instance's variable store. The sequence is non-negotiable: set the variable, then suspend. Reversing the order leaves the monitoring process suspended with no suspension timestamp, triggering the Admin UI resume fallback path on lift.

```java
// Inside ActivateBreathingSpaceSuppressionDelegate.execute()
List<ProcessCaseLink> links = processCaseLinkRepository
    .findActiveByAccountIdAndProcessKey(
        accountId,
        "arrangement-monitoring-process");

links.forEach(link -> {
    runtimeService.setVariable(
        link.getFlowableProcessInstanceId(),
        "arrangementSuspendedAt",
        LocalDateTime.now().toString());

    runtimeService.suspendProcessInstanceById(
        link.getFlowableProcessInstanceId());
});
```

> **Open design item:** `processCaseLinkRepository` depends on a `ProcessCaseLink` entity that links a Flowable process instance ID to an account ID and process definition key. This entity is not yet defined in the authority schema. The design of this cross-process link table must be resolved before `ActivateBreathingSpaceSuppressionDelegate` is implemented. Until resolved, treat the pattern above as illustrative.

#### Delegate Logic

The delegate applies the moratorium duration (`arrangementSuspendedAt` to now, in days) according to the step at which the process was suspended.

```java
// Illustrative pattern — verify field names against current entity model before implementation
public void execute(DelegateExecution execution) {

    String step = (String) execution.getVariable("arrangementMonitoringStep");
    String suspendedAtStr = (String) execution.getVariable("arrangementSuspendedAt");

    if (suspendedAtStr == null) {
        // Admin UI resume path — no suspension timestamp recorded
        auditService.log(MANUAL_REVIEW_REQUIRED,
            execution.getProcessInstanceId(),
            "Arrangement monitoring resumed without arrangementSuspendedAt " +
            "— manual schedule verification required");
        execution.setVariable("skipBreachOnResume", false);
        return;
    }

    long moratoriumDays = ChronoUnit.DAYS.between(
        LocalDateTime.parse(suspendedAtStr),
        LocalDateTime.now());

    switch (step) {
        case "AWAITING_TIMER" -> {
            // Timer had not yet fired — extend due date and arrangement end date
            LocalDate originalDate = LocalDate.parse(
                (String) execution.getVariable("nextDueDateISO"));
            execution.setVariable("nextDueDateISO",
                originalDate.plusDays(moratoriumDays).toString());
            execution.setVariable("skipBreachOnResume", false);
            extendArrangementEndDate(
                (UUID) execution.getVariable("accountId"), moratoriumDays);
        }
        case "CHECKING_PAYMENT" -> {
            // Mid-cycle — extend end date, suppress breach check on first resume cycle
            execution.setVariable("skipBreachOnResume", true);
            extendArrangementEndDate(
                (UUID) execution.getVariable("accountId"), moratoriumDays);
        }
        case "BREACH_EVALUATION", "AGENT_CONTACT_TASK" -> {
            // Pre-moratorium breach in progress — preserve it; do not extend
            execution.setVariable("skipBreachOnResume", false);
        }
    }

    execution.setVariable("arrangementSuspendedAt", null);
}
```

#### `extendArrangementEndDate()` — Pure Domain Write

This method extends the `RepaymentArrangement` end date by the moratorium duration. It is a pure domain write — it does not perform suppression checks, does not send communications, and does not call any Flowable service.

```java
// Illustrative pattern
@Transactional(propagation = REQUIRES_NEW)
public void extendArrangementEndDate(UUID accountId, long moratoriumDays) {
    RepaymentArrangement arrangement =
        repaymentArrangementRepository.findActiveByAccountId(accountId);

    arrangement.setEndDate(arrangement.getEndDate().plusDays(moratoriumDays));

    auditService.log(ARRANGEMENT_END_DATE_EXTENDED, accountId,
        "End date extended by " + moratoriumDays + " days for breathing space moratorium");
}
```

`REQUIRES_NEW` is required here per Decision 1: this write commits independently of the surrounding Flowable transaction context so that the date extension is durable regardless of subsequent BPMN execution outcome.

#### Revised Schedule Notice — Separate BPMN Service Task

After `ResumeArrangementMonitoringDelegate` executes, the BPMN process definition includes a **separate subsequent service task** that dispatches `SendRevisedScheduleNoticeCommand` via `DelegateCommandBus`. The `communications` module handler receives this command and evaluates whether the notice can be sent using the standard Tier 2 channel routing path (versioned `dmn_version_id` snapshot from `process_execution_context`, per ADR-008 section 2).

If the channel routing DMN determines the notice is suppressed (e.g. active suppression log entry), the `communications` handler logs `MANUAL_REVIEW_REQUIRED` and completes normally. The BPMN task completes; no exception is thrown back to the process engine.

This is the correct placement because:
- The suppression check is a Tier 2 DMN rule ("account circumstances × communication channel → available/unavailable"); it must run through the snapshotted versioned path, not as a direct service call inside a `REQUIRES_NEW` transaction
- The notice step is visible in the BPMN diagram; business admins and COMPLIANCE reviewers can see and reason about it
- The `communications` module owns the suppression decision; the process engine does not need to know why a notice was suppressed

The BPMN sequence for the `AWAITING_TIMER` and `CHECKING_PAYMENT` paths is:

```
ResumeArrangementMonitoringDelegate (extend end date)
  → SendRevisedScheduleNoticeDelegate (dispatch SendRevisedScheduleNoticeCommand)
  → [existing monitoring cycle resumes]
```

The `BREACH_EVALUATION` and `AGENT_CONTACT_TASK` paths do not extend the end date and do not dispatch the notice — the process resumes from the pre-moratorium breach state.

## Consequences

- Every method that calls a Flowable service must be reviewed against the transaction boundary table and guardrails — this is a standing code review checklist item
- The broken ADR-011 reference ("ADR-003: Platform-wide transaction boundary rule") is repaired — ADR-011 now references ADR-013
- `extendArrangementEndDate()` commits independently via `REQUIRES_NEW`; if the subsequent notice task fails, the date extension is not rolled back — the audit trail records both events separately
- `processCaseLinkRepository` requires a schema design decision before `ActivateBreathingSpaceSuppressionDelegate` can be implemented; see open design item above
- The revised schedule notice step must appear explicitly in the `breathing_space_process` BPMN definition as a named service task — it must not be embedded in Java infrastructure code
- `skipBreachOnResume` is a non-configurable platform flag — it is not a Tier 2 DMN input and must not be moved to the rule layer
- Breach records created before the moratorium are preserved on resume (RULING-005 guardrail 1)
- Admin UI resume (null `arrangementSuspendedAt`) always produces a `MANUAL_REVIEW_REQUIRED` audit event — operators must verify the schedule manually

## Guardrails

Builders must not violate:

1. No method that calls a Flowable service may be annotated `@Transactional` or invoked from within an active `@Transactional` boundary.
2. Flowable call failures must be caught, logged as `MANUAL_REVIEW_REQUIRED`, and must not propagate as rollback-triggering exceptions.
3. Commit application state first, then call Flowable — always.
4. `extendArrangementEndDate()` must use `@Transactional(propagation = REQUIRES_NEW)` and must contain no suppression checks, no communication dispatches, and no Flowable calls.
5. `arrangementSuspendedAt` must be set on the `ARRANGEMENT_MONITORING_PROCESS` instance via `runtimeService.setVariable()` before `suspendProcessInstanceById()` is called. Sequence is non-negotiable.
6. The revised schedule notice must be dispatched via `SendRevisedScheduleNoticeCommand` through the `DelegateCommandBus` in a separate BPMN service task — never inside `extendArrangementEndDate()` or any `REQUIRES_NEW` method.
7. Pre-moratorium `ARRANGEMENT_BREACH` records must not be cleared on resume.
8. Admin UI resume (null `arrangementSuspendedAt`) must generate a `MANUAL_REVIEW_REQUIRED` audit event and must not attempt date recalculation.

## Open Design Items

| Item | Blocking what | Owner | Status |
|---|---|---|---|
| `ProcessCaseLink` entity and `processCaseLinkRepository` — schema for cross-process instance linking by account ID and process definition key | `ActivateBreathingSpaceSuppressionDelegate` implementation | Architecture | Unresolved — must be resolved before breathing space activation delegate is built |

## Alternatives Rejected

### Embed `generateRevisedScheduleNotice()` inside `extendArrangementEndDate()` (Rejected)

The source artifact (architecture-decisions.md ADR-013) calls `communicationSuppressionService.isPermitted(accountId, NON_COLLECTION)` inside the `REQUIRES_NEW` method and conditionally sends the notice from there.

**Rejected** for three reasons:

1. **Tier model violation.** The suppression check is a Tier 2 channel routing rule ("account circumstances × communication channel → available/unavailable", ADR-008 section 7). Calling it directly inside a `REQUIRES_NEW` domain write method bypasses the `dmn_version_id` snapshot mechanism (ADR-008 section 2). The suppression decision is not traceable to a versioned rule deployment — it cannot be audited against the rule that was in effect at decision time. This violates DW.42.

2. **Tier 3 visibility.** The notice step becomes invisible in the BPMN diagram. Business admins and COMPLIANCE reviewers cannot see or reason about it. ADR-008 Tier 3 goal is that business admins own treatment path design — burying a communications decision in Java infrastructure code defeats this.

3. **Coupling direction.** `extendArrangementEndDate()` is a `repaymentplan` domain write. Calling `communicationSuppressionService` from within it creates a compile-time dependency from `repaymentplan` into `communications` inside a transaction boundary method — the wrong direction per ADR-003.

### Single transaction spanning application DB and Flowable (Rejected)

Using a single `@Transactional` boundary that encompasses both application writes and Flowable calls.

**Rejected** because Flowable uses its own internal transaction. Without XA coordination, a two-resource transaction has no rollback guarantee. A Spring transaction rollback after a Flowable commit leaves the process engine and application DB in an inconsistent state with no automated recovery path.

### Flowable-first pattern (Rejected)

Call Flowable first, then commit application state.

**Rejected** because a DB failure after the Flowable call leaves a running process instance with no corresponding application record. The process instance advances while the domain state it depends on does not exist — an undetectable data integrity failure.

## References

- ADR-001: Process instance per Person + Account pair
- ADR-002: Monitoring and event handling via BPMN event subprocesses — `BREATHING_SPACE_START` / `BREATHING_SPACE_END` messages
- ADR-003: Delegate command pattern — this ADR provides the transaction boundary rule that ADR-003 defers to
- ADR-006: BPMN versioning and in-flight migration
- ADR-008: Three-tier configurability — section 7 (hardcoded list includes breathing space duration), section 2 (DMN version snapshot mechanism), Tier 2 (channel routing rules)
- ADR-012: Live-state DB reads in delegates — `breathing_space_flag` is one of the fields that must be read from DB, not from process variables
- RULING-005: Breathing Space — REPAYMENT_ARRANGEMENT Interaction — this ADR is the mechanical implementation of RULING-005 rules 3–5 and the "arrangement expiry date falls within moratorium" edge case
- Source: `docs/project-foundation/architecture-decisions.md` ADR-013 (donor artifact, not authority)
- DW requirements: DW.18, DW.42, DW.45, DW.49
- CAS requirements: CAS.BS-05, CAS.BS-06
