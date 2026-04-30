# ADR-014: CommunicationSuppressionService — Suppression Authority and Call-Site Enforcement

## Status

Accepted

## Date

2026-04-24

## Context

The system dispatches outbound communications across multiple channels — letters, SMS, email, in-app messages — from multiple call sites across several domain modules and background delegates. Several regulatory and policy grounds require that some or all communications be suppressed for a given account: breathing space moratorium, deceased party mandatory hold, insolvency, mental health crisis, internal dispute, and vulnerability policy.

Without a single authoritative decision point for suppression, two failure modes arise:

1. **Fragmented suppression checks.** Individual call sites each implement their own suppression logic, creating inconsistency: one path checks breathing space but not deceased; another checks policy suppressions but not statutory ones. A new suppression reason added in one place is silently absent elsewhere.

2. **Direct SUPPRESSION_LOG reads at dispatch call sites.** If call sites query `SUPPRESSION_LOG` independently, the priority ordering of suppression reasons (e.g. `ESTATE_ADMINISTRATION` is never blocked even when `DECEASED_MANDATORY` is active) is duplicated and can diverge.

ADR-013 rejected embedding `communicationSuppressionService.isPermitted()` inside `extendArrangementEndDate()` and deferred suppression evaluation to the `communications` module, which presupposes that a bounded authority service exists there. This ADR documents that service as an explicit architecture decision.

## Decision

### 1. Suppression authority

`CommunicationSuppressionService` is the sole authority on whether an outbound communication may be dispatched. It is a singleton service in the `communications` domain package. No other class may consult `SUPPRESSION_LOG` for dispatch decisions.

### 2. Interface contract

```java
public interface CommunicationSuppressionService {

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

### 3. CommunicationCategory enum (locked)

```java
public enum CommunicationCategory {
    DEBT_COLLECTION,
    NON_COLLECTION,
    DUAL_USE,
    ESTATE_ADMINISTRATION
}
```

### 4. isPermitted() decision logic (locked sequence)

The following priority ordering is non-negotiable. Evaluation must proceed in this exact sequence:

```
Step 1 — Estate administration pass-through:
  If category == ESTATE_ADMINISTRATION:
    return PERMITTED unconditionally
    (No suppression reason blocks estate administration communications)

Step 2 — Deceased mandatory block:
  If any active suppression reason == DECEASED_MANDATORY:
    block DEBT_COLLECTION
    block NON_COLLECTION
    (ESTATE_ADMINISTRATION already handled in step 1)

Step 3 — Statutory moratorium block (debt collection only):
  If any active suppression reason in {
    BREATHING_SPACE_STATUTORY,
    MENTAL_HEALTH_CRISIS_STATUTORY,
    INSOLVENCY_STATUTORY }:
    block DEBT_COLLECTION only
    permit NON_COLLECTION

Step 4 — Policy suppression block (debt collection only):
  If any active suppression reason in {
    DISPUTE_INTERNAL,
    VULNERABILITY_POLICY }:
    block DEBT_COLLECTION only
    permit NON_COLLECTION

Step 5 — Default:
  No active suppressions:
    return PERMITTED

  Unknown category:
    throw UnhandledCommunicationCategoryException
    — Java 21 exhaustive switch must enforce this at compile time
```

### 5. Schema

```sql
CREATE UNIQUE INDEX uix_suppression_log_active
  ON suppression_log (account_id, suppression_reason)
  WHERE is_active = true;
```

`activateSuppression()` uses `INSERT ... ON CONFLICT DO NOTHING` (upsert semantics). This prevents duplicate active suppression records for the same account and reason without raising an error on re-activation.

### 6. Write authority

`activateSuppression()` and `liftSuppression()` are the only methods that write to `SUPPRESSION_LOG`. Direct repository writes to `SUPPRESSION_LOG` outside `CommunicationSuppressionService` are prohibited.

### 7. Call-site enforcement

`NotifyAdapter` — the outbound dispatch adapter in `communications` — must call `isPermitted()` as its final operation before dispatch. This applies to every channel and every dispatch trigger. There are no exceptions.

Suppression checks in delegates or domain services upstream of `NotifyAdapter` are permitted as an additive layer (e.g. to decide whether to enqueue a notice) but do not replace the `NotifyAdapter` gate. The `NotifyAdapter` gate is the enforcement point, not an advisory check.

### 8. DUAL_USE override

A `DUAL_USE` communication may only be dispatched when the `TEAM_LEADER` role check is satisfied. The override must write an `AUDIT_EVENT` with `event_type = OVERRIDE`. This check is performed within `CommunicationSuppressionService.isPermitted()` — it is not delegated to call sites.

### 9. Audit obligations

Both `activateSuppression()` and `liftSuppression()` must write an `AUDIT_EVENT`. There are no exceptions to this rule. Suppression state changes without an audit record are a compliance failure.

### 10. Reporting view

`v_account_suppression_status` is a read-only database view used for compliance reporting and Admin UI visibility only. All dispatch decisions must query `SUPPRESSION_LOG` directly via `CommunicationSuppressionService`. The view must never be used for dispatch gating.

## Guardrails — builders must not violate

1. `NotifyAdapter` must call `isPermitted()` as its final operation before dispatch. No exceptions, no bypasses.
2. No code outside `CommunicationSuppressionService` may write to `SUPPRESSION_LOG` directly — all writes go through `activateSuppression()` or `liftSuppression()`.
3. Both `activateSuppression()` and `liftSuppression()` must write an `AUDIT_EVENT`. Suppression state changes are always auditable.
4. `isPermitted()` must follow the locked priority sequence (steps 1–5 above). No reordering. No omission of steps.
5. `DUAL_USE` override requires `TEAM_LEADER` role check and `AUDIT_EVENT` with `event_type = OVERRIDE`. This is enforced inside `isPermitted()`, not at call sites.
6. `v_account_suppression_status` is for reporting and UI visibility only. It must never be used to gate dispatch decisions.
7. Any new `SuppressionReason` value must be evaluated against the `isPermitted()` priority sequence at design time. Adding a reason without updating the sequence is a Class A change requiring an ADR amendment.
8. The unique partial index `uix_suppression_log_active` must be present before any suppression logic is exercised. It is in the V009 Flyway migration (Sprint 1 block, per ADR-010 migration map).

## Consequences

1. **Single enforcement point.** Any suppression reason added to `SUPPRESSION_LOG` is automatically applied at the `NotifyAdapter` gate for all channels. There is no risk of a new reason being silently absent from one dispatch path.
2. **Priority ordering is explicit and locked.** The five-step sequence prevents ambiguous outcomes when multiple suppression reasons are simultaneously active (e.g. breathing space + dispute). Compliance reviewers can audit the decision logic directly from this document.
3. **Upstream advisory checks remain valid.** Delegates and services that call `isPermitted()` before enqueuing a communication are additive defensive layers. They do not replace the `NotifyAdapter` gate but reduce unnecessary work.
4. **`extendArrangementEndDate()` does not embed suppression logic.** Per ADR-013, the revised schedule notice after a breathing space moratorium is dispatched as a separate BPMN service task via `SendRevisedScheduleNoticeCommand` through the `DelegateCommandBus`. The `communications` module handles suppression evaluation at dispatch time, using the `dmn_version_id` snapshot for Tier 2 DMN evaluation. This preserves the tier model and BPMN visibility.
5. **Compliance reporting uses the view, not the service.** `v_account_suppression_status` isolates reporting from dispatch logic, preventing reporting queries from accidentally influencing enforcement state.

## Alternatives Rejected

**Suppression check as Flowable gateway per BPMN process** (Rejected)

Encoding suppression as a Flowable exclusive gateway on BPMN processes creates a duplication risk: non-Flowable dispatch paths (direct API calls, background jobs, bulk communications) are not covered. Adding new dispatch paths requires updating every relevant BPMN model. Rejected: does not cover all dispatch paths; creates divergence risk.

**AOP annotation as primary suppression mechanism** (Rejected)

An `@SuppressIfBlocked` or similar AOP annotation applied to dispatch methods is not visible to BPMN tooling, is difficult to test in isolation, and is invisible to compliance reviewers inspecting the codebase. Acceptable as an additive layer only on top of `NotifyAdapter.isPermitted()`. Rejected as the primary mechanism.

## References

- ADR-003: Delegate command pattern — `communications` module is a domain module; suppression service is internal to it
- ADR-013: `extendArrangementEndDate()` — suppression check deferred to communications module; this ADR defines the boundary that makes that deferral valid
- ADR-010: Migration map — V009 carries `SUPPRESSION_LOG` schema and unique partial index
- RULING-001: Breathing space communications suppression scope — defines which `SuppressionReason` values apply during breathing space moratorium
- RULING-011: Queued communication disposition on suppression lift — defines what happens to queued communications when suppression is lifted
- `architecture-decisions.md` ADR-001 (donor source — CommunicationSuppressionService boundary and interface)
