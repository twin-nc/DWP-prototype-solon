> ## ⚠ STATUS: UNDER REVIEW — PLATFORM PIVOT IN EFFECT
>
> A confirmed programme-level decision (ADR-018, 2026-04-30) has pivoted the project to build on top of **Solon Tax** as the base platform. All prior ADRs are under review.
>
> **This document contains accumulated knowledge and analysis that remains relevant to the new design process. It does NOT represent the current or preferred direction.** All decisions and conclusions herein are open for renewed debate. Do not treat this ADR as a directive, default position, or settled constraint.
>
> See [ADR-018](ADR-018-platform-pivot-solon-tax-confirmed.md) for the full pivot record.

---

# ADR-003: Process engine as infrastructure, not domain — delegate command pattern for domain integration

**Status:** Accepted

**Date:** 2026-04-22

## Context

The scaffold placed the process engine boundary at `domain/process`, treating it as a peer of domain modules like `account` and `repaymentplan`. After ADR-001 and ADR-002, the process engine's actual responsibilities are:

- Hosting BPMN/DMN model resources
- Providing `ProcessEventPort` and `ProcessStartPort` (from ADR-002)
- Hosting JavaDelegate implementations that call domain services from BPMN service tasks
- Wiring Flowable Spring Boot configuration

It owns no domain data, no JPA entities, no business invariants. It is a technical infrastructure boundary, not a domain. Placing it at `domain/process` falsely implies it is a peer of domain modules and leaks Flowable types into the domain layer.

Additionally, BPMN service tasks need to call domain services (send a letter, assess affordability, post a payment deduction, trigger segmentation). The naive approach of autowiring domain services directly into JavaDelegate implementations creates compile-time imports from the process infrastructure into every domain module — the wrong dependency direction.

Requirements addressed:
- DW.40 — reuse sets of rules and components
- DW.42 — trace outcome back through decisioning logic
- DW.47 — automate a sequence of activity within a workflow/treatment
- DW.59 — agent action triggers automated activities (send comms, move workflow, note account)

## Decision

**Move the process engine boundary from `domain/process` to `infrastructure/process`.**

**Introduce a command-based delegate pattern with three layers:**

**Layer 1 — Delegate implementations** (in `infrastructure/process/delegate/`)

One `JavaDelegate` implementation per named BPMN service task action. Each delegate reads process variables from `DelegateExecution`, constructs a command object, and dispatches via `DelegateCommandBus`. No domain module imports.

**Layer 2 — `DelegateCommandBus`** (interface in `shared/process/port/`)

A single Spring bean that accepts a `DelegateCommand` and routes to the registered handler. The infrastructure layer does not know which domain handles which command.

```java
public interface DelegateCommandBus {
    void dispatch(DelegateCommand command);
}
```

**Layer 3 — Command handlers** (in domain modules)

Each domain module registers handlers for the commands it owns. Handlers are plain Spring `@Component` beans — no Flowable imports.

```java
// Example in communications module
@Component
public class SendLetterCommandHandler implements DelegateCommandHandler<SendLetterCommand> { ... }
```

**Dependency direction:**

- Domain modules depend on `shared/process/port` (command interfaces and types) — never on `infrastructure/process`
- `infrastructure/process` depends on `shared/process/port` for the bus interface
- `infrastructure/process` has zero compile-time knowledge of domain modules
- Flowable types (`JavaDelegate`, `DelegateExecution`, `RuntimeService`) are confined to `infrastructure/process` only

**BPMN service task delegate binding:**

BPMN service tasks reference delegates by Spring bean name via `flowable:delegateExpression`. The bean name is a stable contract — renaming a delegate class is a breaking change to any BPMN model that references it and must be treated as such.

```xml
<serviceTask id="sendLetter" flowable:delegateExpression="${sendLetterDelegate}" />
```

**Package structure:**

```
domain/
  customer/
  account/
  strategy/
  workallocation/
  repaymentplan/
  payment/
  communications/
  integration/
  thirdpartymanagement/
  user/
  audit/
  reporting/
  analytics/

infrastructure/
  process/
    config/          ← FlowableSpringConfiguration, Flowable bean wiring
    delegate/        ← JavaDelegate implementations (one per service task action)
    port/impl/       ← Implementations of ProcessEventPort and ProcessStartPort

shared/
  process/
    port/            ← ProcessEventPort, ProcessStartPort, DelegateCommandBus (interfaces)
                        DelegateCommand (marker interface), DelegateCommandHandler<T> (interface)
                        Command types: SendLetterCommand, AssessAffordabilityCommand,
                        PostPaymentDeductionCommand, TriggerSegmentationCommand, etc.
  health/
  error/
```

## Consequences

- `domain/process` package is removed — no domain package exists for the process engine
- Flowable imports (`flowable-spring-boot-starter`) are confined to `infrastructure/process`; no other module may import Flowable types directly
- Every new BPMN service task action requires: a command type in `shared/process/port/`, a delegate in `infrastructure/process/delegate/`, and a handler in the owning domain module
- Adding a command type is non-breaking; renaming or removing a command type or delegate bean name is a breaking change and requires contract versioning per STD-GOV-004
- Domain modules remain independently testable — command handlers are plain Spring beans with no Flowable dependency
- Delegates are testable by injecting a mock `DelegateCommandBus` — no Flowable engine required for delegate unit tests
- BPMN model resources live in `backend/src/main/resources/processes/` — accessible to Flowable at runtime, not tied to any domain package
- Delegates that make decisions on compliance-sensitive account state (breathing space, deceased, statute-barred, benefit status, vulnerability) must read from the DB at execution time, not from `DelegateExecution.getVariable()`. Process variables for these fields are read-only snapshots for initial routing only. See ADR-012.
- The platform-wide rule that Flowable service calls must not occur inside an `@Transactional` boundary, and that application state must be committed before any Flowable call, is documented in ADR-013.
- Suppression decisions for outbound communications dispatched from delegates are never made inside the delegate itself — `communications` module owns `CommunicationSuppressionService` as the sole authority. Delegates dispatch via `SendRevisedScheduleNoticeCommand` or equivalent; the `communications` module applies suppression at dispatch time. See ADR-014.

## Alternatives Rejected

**Autowire domain services directly into JavaDelegate implementations (Rejected)**

Direct `@Autowired` domain services in delegates creates compile-time imports from infrastructure into every domain module. Swapping the process engine would require touching all domain modules. Rejected: wrong dependency direction.

**Keep `domain/process` as a domain module (Rejected)**

Owns no entities, no invariants, no business data. The label "domain" misleads developers into treating it as a peer of real domain modules and obscures where Flowable dependencies are permitted. Rejected: architectural misclassification.

## Implementation Notes

1. **BPMN resources location:** `backend/src/main/resources/processes/` — subdirectories: `treatments/`, `fragments/`, `decisions/`

2. **Shared port package:** `backend/src/main/java/com/netcompany/dcms/shared/process/port/`

3. **Infrastructure process package:** `backend/src/main/java/com/netcompany/dcms/infrastructure/process/`

4. **Initial command type catalogue (v1):**
   - `SendLetterCommand` — owned by `communications`
   - `SendSmsCommand` — owned by `communications`
   - `AssessAffordabilityCommand` — owned by `repaymentplan`
   - `PostPaymentDeductionCommand` — owned by `payment`
   - `TriggerSegmentationCommand` — owned by `strategy`
   - `CreateWorkItemCommand` — owned by `workallocation`
   - `RecordAccountNoteCommand` — owned by `account`
   - `RecordRegulatoryFactCommand` — owned by `account` (breathing space date, insolvency date, etc.)

5. **Flowable dependency:** `flowable-spring-boot-starter` added to `pom.xml` scoped to the infrastructure layer. No other module declares a Flowable dependency.

## References

- ADR-001: Process instance per debtor+debt pair
- ADR-002: Monitoring and event handling via BPMN event subprocesses
- STD-GOV-004 (contract versioning)
- Flowable User Guide: Spring Boot Integration, Java Delegates, Delegate Expressions
- DW requirements: DW.40, DW.42, DW.47, DW.59
