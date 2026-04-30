> ## ⚠ STATUS: UNDER REVIEW — PLATFORM PIVOT IN EFFECT
>
> A confirmed programme-level decision (ADR-018, 2026-04-30) has pivoted the project to build on top of **Solon Tax** as the base platform. All prior ADRs are under review.
>
> **This document contains accumulated knowledge and analysis that remains relevant to the new design process. It does NOT represent the current or preferred direction.** All decisions and conclusions herein are open for renewed debate. Do not treat this ADR as a directive, default position, or settled constraint.
>
> See [ADR-018](ADR-018-platform-pivot-solon-tax-confirmed.md) for the full pivot record.

---

# ADR-012: Live-State DB Reads in Delegates — No Stale Process Variables for Compliance Decisions

## Status

Accepted

## Date

2026-04-24

## Context

ADR-003 establishes the delegate command pattern: each `JavaDelegate` implementation reads process variables from `DelegateExecution`, constructs a command, and dispatches via `DelegateCommandBus`. It does not specify which variables are prohibited from being used as decision inputs.

Flowable process variables are persisted in `ACT_RU_VARIABLE` for the lifetime of the process instance. A variable written at process start — such as `breathingSpaceFlag` or `isStatuteBarred` — will be stale by the time a long-running process timer fires, which may be days, weeks, or months later. Using that stale snapshot to make a compliance-sensitive routing or enforcement decision is a regulatory risk.

ADR-002 establishes that breathing space, insolvency, and death are process engine states modelled as interrupting event subprocesses. However, a delegate executing a service task mid-process cannot rely on those event subprocesses having fired — the DB record may have been updated externally (bureau feed, DWP Place integration) without a corresponding message reaching the process instance yet. The delegate must read the authoritative DB state directly.

Requirements addressed:
- DW.18 — eliminate blackholes (accounts lost in workflow)
- DW.42 — trace outcome back through decisioning logic
- DW.45 — flag deceased, vulnerable, insolvency, fraud, breathing space
- DW.49 — account can enter, exit, pause within a workflow/treatment

## Decision

**Process variables set at process start are read-only snapshots used for initial routing only. They must not be used for mid-process business decisions on live account state.**

Any `JavaDelegate` that makes a routing or enforcement decision based on compliance-sensitive account state must read directly from the database entity at execution time via the repository, not from `DelegateExecution.getVariable()`.

### Fields That Must Be Read from the Database

The following fields constitute the v1 baseline. Any new compliance-sensitive field added to `account` or `customer` entities must be evaluated against this rule at design time — the list is a governed baseline, not an exhaustive closed set.

| Field | Entity / Table | Compliance concern |
|---|---|---|
| `breathingSpaceFlag` | `DebtAccount` / `DEBT_ACCOUNT` | Debt Respite Scheme — enforcement and contact prohibited during moratorium |
| `deceasedFlag` | `DebtAccount` / `DEBT_ACCOUNT` | Estate handling — active collection must halt on death notification |
| `isStatuteBarred` | `DebtAccount` / `DEBT_ACCOUNT` | Limitation Act 1980 — statute-barred debt must not be actively enforced |
| `benefitStatus` | `DebtAccount` / `DEBT_ACCOUNT` | Determines applicable deduction bands and treatment eligibility |
| `status` | `VulnerabilityAssessment` / `VULNERABILITY_ASSESSMENT` | Vulnerability protocols — treatment path and contact rules differ for vulnerable customers |

### Required Delegate Pattern

Delegates must fetch the entity by `accountId` from the repository and use entity accessors. They must not call `execution.getVariable()` for any field in the table above.

```java
@Override
public void execute(DelegateExecution execution) {
    UUID accountId = (UUID) execution.getVariable("accountId");

    DebtAccount account = debtAccountRepository
        .findById(accountId)
        .orElseThrow(() -> new AccountNotFoundException(accountId));

    // Correct: account.isBreathingSpaceFlag()
    // Prohibited: execution.getVariable("breathingSpaceFlag")
}
```

The `accountId` process variable is a stable identifier, not state — reading it from `DelegateExecution` is permitted and required.

### Permitted use of process variables for compliance-sensitive fields

Process variables for these fields may be written at process start for logging and tracing purposes only. They must not be read back as decision inputs anywhere in the delegate execution path.

## Consequences

- Every delegate that gates or routes on account state must perform a DB read at execution time — this is a hard requirement, not an optimisation choice
- Field names in the baseline table are indicative; verify against the current JPA entity model before implementation. Schema changes to these fields are Class A changes
- At production scale, a request-scoped cache may reduce repeated DB reads within a single job execution — this is a deferred performance optimisation. The rule itself is unconditional; do not implement a cache ahead of load profiling evidence
- New compliance-sensitive fields added to `account` or `customer` entities must be assessed at design time and added to the baseline table via ADR amendment if the field is decision-relevant
- Delegates are unit-testable in isolation: inject a mock repository returning the required entity state

## Guardrails

Builders must not violate:

1. No delegate that makes a business decision on any field in the baseline table may use `execution.getVariable()` for that field.
2. Process variables for these fields may be set at process start for logging or tracing purposes only — never read back as decision inputs.
3. This rule has no sprint or phase exception. It applies from the first delegate implementation.

## Rationale

Flowable process variables persist in `ACT_RU_VARIABLE` for the lifetime of the process instance. A debt collection process instance may run for months. A variable written at case initiation will be stale by the time any timer-driven service task fires. Using stale state for compliance-sensitive decisions — whether to contact a customer in breathing space, whether to enforce a statute-barred debt, whether to apply vulnerability protocols — is a regulatory risk with direct DWP liability consequences.

Reading the current DB state at execution time is the only safe approach. It aligns with the ADR-002 principle that the `account` module records regulatory facts authoritatively; the process engine consults those facts rather than caching them internally.

## Alternatives Rejected

### Keep compliance flags as process variables, updated via message events (Rejected)

When a regulatory event fires (e.g. `BREATHING_SPACE_START`), update the process variable in the same transaction.

**Rejected because:** this requires guaranteed synchronisation between the event subprocess firing and the variable update. If a delegate executes between the external DB write and the message delivery, it reads a stale variable. The DB record is the single authoritative source; a variable mirror adds a synchronisation problem without benefit.

### Use a shared Flowable data object / transient variable (Rejected)

Store a live-fetched snapshot in a transient process variable scoped to the current job execution.

**Rejected because:** transient variables in Flowable are not persisted but are also not type-safe or visible to tooling. They reintroduce implicit passing of state between delegates within the same transaction, making the read dependency invisible. Direct repository reads are explicit, testable, and unambiguous.

## References

- ADR-001: Process instance per Person + Account pair
- ADR-002: Monitoring and event handling via BPMN event subprocesses — `account` module records regulatory facts; process engine owns behavioural effect
- ADR-003: Delegate command pattern — this ADR extends ADR-003 with a constraint on which process variables delegates may use for decisions
- Source: `docs/project-foundation/architecture-decisions.md` ADR-009 (donor artifact, not authority)
- DW requirements: DW.18, DW.42, DW.45, DW.49
