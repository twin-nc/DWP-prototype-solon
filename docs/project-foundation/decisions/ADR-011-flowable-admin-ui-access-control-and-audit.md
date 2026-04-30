> ## ⚠ STATUS: UNDER REVIEW — PLATFORM PIVOT IN EFFECT
>
> A confirmed programme-level decision (ADR-018, 2026-04-30) has pivoted the project to build on top of **Solon Tax** as the base platform. All prior ADRs are under review.
>
> **This document contains accumulated knowledge and analysis that remains relevant to the new design process. It does NOT represent the current or preferred direction.** All decisions and conclusions herein are open for renewed debate. Do not treat this ADR as a directive, default position, or settled constraint.
>
> See [ADR-018](ADR-018-platform-pivot-solon-tax-confirmed.md) for the full pivot record.

---

# ADR-011: Flowable Admin UI — Access Control and Audit

## Status

Accepted

## Date

2026-04-24

## Context

Flowable's built-in Admin UI allows direct manipulation of running process instances — task reassignment, variable edits, process suspension, deletion, and migration. These operations bypass normal application code paths and therefore bypass the application's standard audit trail. Without an explicit access control and audit strategy, two risks arise:

1. **Uncontrolled access.** Any authenticated user with an Admin UI credential could alter live process state — including process instances managing regulatory obligations (breathing space, deceased handling, write-off approval).

2. **Invisible operations.** If Admin UI actions are not captured in `AUDIT_EVENT`, compliance reviewers have no record of out-of-band changes to debtor case state. This is a regulatory audit failure.

This ADR defines the access model, the implementation mechanism for distinguishing Admin UI operations from programmatic ones, and the audit event obligations that follow.

RBAC role definitions referenced here (OPS_MANAGER, FLOWABLE_ADMIN) are defined in `docs/project-foundation/RBAC-IMPLEMENTATION-DECISIONS.md`. Role definitions are not repeated in this ADR.

## Decision

### 1. Access model

The Flowable Admin UI is not accessible to operational roles. It is accessible only to `FLOWABLE_ADMIN`, a client-scoped role on the `flowable-admin` Keycloak client.

| Capability | OPS_MANAGER | FLOWABLE_ADMIN |
|---|---|---|
| View process instances (read-only) | Yes — via application UI and API | Yes — via Admin UI |
| Edit DMN rules (Tier 2) | Yes — via application admin UI | No |
| Direct process instance manipulation | No | Yes |
| Task reassignment (operational) | Yes — via application API | Yes |
| Process suspension / activation | No | Yes |
| Process instance deletion | No | Yes |
| Process instance migration | No | Yes |

`OPS_MANAGER` has no direct Admin UI access. Operational task reassignment for `OPS_MANAGER` is done through the application API, which applies standard RBAC guards and writes `AUDIT_EVENT` records in the normal flow.

`FLOWABLE_ADMIN` is provisioned only for named technical administrators. At demo scope this is the Lead Developer. Each FLOWABLE_ADMIN user must be provisioned as a named individual in Keycloak with a resolvable UUID in the USER table — not a shared or anonymous account.

### 2. Distinguishing programmatic from Admin UI operations

All application code that calls `TaskService`, `RuntimeService`, or other Flowable services must set a thread-local flag before the call so that the `HistoryEventProcessor` (section 3) can distinguish application-driven from human Admin UI-driven operations.

```java
public final class FlowableOperationContext {

  private static final ThreadLocal<Boolean> PROGRAMMATIC
    = new ThreadLocal<>();
  // Standard ThreadLocal — NOT InheritableThreadLocal.
  // Async Flowable job executor threads must not inherit
  // this value — absent flag correctly signals Admin UI origin.

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
```

Usage pattern — mandatory at every application-side Flowable call site:

```java
FlowableOperationContext.setProgrammatic(true);
try {
  taskService.complete(taskId, variables);
} finally {
  FlowableOperationContext.clear(); // always in finally — never try only
}
```

When the flag is absent (i.e. `isProgrammatic()` returns false), the operation originated in the Admin UI or from the Flowable async job executor. The `HistoryEventProcessor` uses this to decide whether to write an `AUDIT_EVENT`.

### 3. HistoryEventProcessor — event scope

`HistoryEventProcessor` implements `org.flowable.engine.impl.history.HistoryEventHandler` and must be registered as a Spring bean. Flowable auto-detects it via that interface.

Events written to `AUDIT_EVENT`:

| Flowable event | Condition for writing to AUDIT_EVENT |
|---|---|
| `TASK_COMPLETED` | Only when `FlowableOperationContext.isProgrammatic()` is false (i.e. Admin UI completion only) |
| `VARIABLE_UPDATED` | Always |
| `PROCESS_INSTANCE_SUSPENDED` | Always |
| `PROCESS_INSTANCE_ACTIVATED` | Always |
| `PROCESS_INSTANCE_DELETED` | Always |
| `PROCESS_INSTANCE_MIGRATED` | Always |

Routine process advancement events (timer fires, service task execution, automated gateway routing) are not written to `AUDIT_EVENT` — they remain in Flowable's own history tables (`ACT_HI_*`). Duplicating routine events would obscure the compliance-relevant signal.

### 4. AUDIT_EVENT field mapping for HistoryEventProcessor entries

```
entity_type   = FLOWABLE_PROCESS_INSTANCE | FLOWABLE_TASK
entity_id     = process instance ID or task ID (UUID string)
event_type    = OVERRIDE
actor_id      = UUID of the authenticated FLOWABLE_ADMIN user
                (resolved from Keycloak JWT subject claim)
occurred_at   = timestamp from the Flowable history event
system_initiated = false  (for human Admin UI actions)
```

For background Flowable internal operations where no authenticated user context is present:

```
actor_id         = 00000000-0000-0000-0000-000000000001 (SYSTEM)
system_initiated = true
```

`system_initiated = true` entries are distinguishable from human entries and excluded from compliance audit reports that require a named human actor. Human-initiated Admin UI actions must never record with SYSTEM actor — the `HistoryEventProcessor` implementation must not default to SYSTEM when a JWT is resolvable.

### 5. Actor resolution

`HistoryEventProcessor` must resolve the actor UUID at event time from the active security context:

```java
UUID actorId;
boolean systemInitiated;

Authentication auth = SecurityContextHolder.getContext().getAuthentication();
if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
  actorId = UUID.fromString(jwt.getSubject());
  systemInitiated = false;
} else {
  actorId = SYSTEM_UUID; // 00000000-0000-0000-0000-000000000001
  systemInitiated = true;
}
```

If the resolved UUID does not exist in the USER table, the processor must log WARN and fall back to SYSTEM actor with `system_initiated = true`. It must not throw an exception that disrupts the Flowable event pipeline.

### 6. Compliance audit screen

The application's compliance audit screen must display a notice for any `AUDIT_EVENT` record where `system_initiated = false` and `entity_type` is `FLOWABLE_PROCESS_INSTANCE` or `FLOWABLE_TASK`:

> "This event was recorded by the Flowable Admin UI. The named actor above is the provisioned FLOWABLE_ADMIN user. For Admin UI session details, cross-reference the Keycloak access log."

This notice is mandatory. Compliance reviewers must understand that Admin UI operations are identified by the provisioned user account, not by a session-level action record in the application audit trail.

## Guardrails — builders must not violate

1. `FlowableOperationContext` uses standard `ThreadLocal` — never `InheritableThreadLocal`. Async Flowable job executor threads must not inherit the programmatic flag.
2. `FlowableOperationContext.clear()` must always be in a `finally` block. Never in the `try` block only.
3. Every application-side call to `TaskService`, `RuntimeService`, `ExecutionService`, or `ManagementService` must wrap the call with `setProgrammatic(true)` / `clear()` in finally.
4. `HistoryEventProcessor` must be a registered Spring bean. Do not instantiate it manually.
5. Human Admin UI actions must never record `actor_id` as the SYSTEM UUID. Actor resolution failure falls back to SYSTEM with `system_initiated = true` and a WARN log — it does not silently use SYSTEM for a human action.
6. `FLOWABLE_ADMIN` users must each be provisioned as a named individual in Keycloak and the USER table. Shared accounts are prohibited.
7. `HistoryEventProcessor` implementation must be complete and verified before Sprint 5, when breathing space and write-off processes go live. Admin UI operations against those processes before the processor is live are unaudited.
8. `OPS_MANAGER` must not be granted the `flowable-admin` client role. If a Keycloak configuration change would grant this, it is a Class A change requiring COMPLIANCE approval.

## Consequences

1. **Programmatic vs. human Admin UI operations are unambiguously distinguishable** at audit time via the `FlowableOperationContext` flag. There is no reliance on naming conventions or call-stack inspection.
2. **Named actor on every Admin UI action.** Compliance reviewers always have a named individual, not SYSTEM, for any human-initiated Admin UI change. Cross-reference to Keycloak access logs provides session-level detail beyond the application audit trail.
3. **Background Flowable operations are distinguishable from human actions** via `system_initiated = true`. These are not compliance-reportable without additional investigation.
4. **OPS_MANAGER retains operational capability without Admin UI access.** Task reassignment and process visibility are available through the application API, which applies RBAC guards and standard audit writing.
5. **Sprint 5 gate.** If `HistoryEventProcessor` is not implemented before Sprint 5, breathing space and write-off processes will go live without Admin UI audit coverage. This is the hard deadline.

## References

- `docs/project-foundation/RBAC-IMPLEMENTATION-DECISIONS.md` — role definitions, FLOWABLE_ADMIN provisioning requirements, section 9.6 (actor resolution fix)
- ADR-013: Platform-wide Flowable transaction boundary rule (Flowable calls outside application transaction)
- ADR-004: Write-off self-approval prevention
- ADR-007: Breathing space BPMN pattern
- UAAF.1 (RBAC for all user functions), UAAF.4 (least-privilege access), UAAF.11 (privileged access management for admin functions), UAAF.15 (read-only access for compliance), UAAF.16 (configurable approval workflows for sensitive operations), UAAF.17 (user activity reporting for compliance)
- `architecture-decisions.md` ADR-006 (FEAT5 donor source — superseded on actor resolution by RBAC-IMPLEMENTATION-DECISIONS.md section 9.6)
