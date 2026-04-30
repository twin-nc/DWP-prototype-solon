# DIAG-03 — Delegate Command Pattern Talking Points

## Purpose of the slide

This slide explains how DCMS gets the benefits of a BPMN process engine without letting Flowable leak into the business code.

The key message is:

> Flowable owns workflow execution, but domain modules own business decisions. The boundary between them is deliberate, narrow, and governed.

## How to talk through the diagram

Start at the top and move down.

### 1. Domain modules

The top layer is the business/domain code: `account`, `communications`, `repaymentplan`, `payment`, `strategy`, `workallocation`, and related modules.

These modules own business rules and invariants. For example:

- `strategy` decides which treatment segment an account belongs to.
- `communications` decides whether a letter or SMS can be sent.
- `repaymentplan` owns affordability and arrangement logic.
- `account` records regulatory facts such as breathing space, insolvency, deceased, or statute-barred status.

The important design rule is that domain modules do **not** import Flowable and do **not** call Flowable APIs directly.

### 2. Shared process port

The purple layer is `shared/process/port`.

This is the neutral contract layer between the business code and the process engine. It contains interfaces and command types such as:

- `ProcessEventPort`
- `ProcessStartPort`
- `DelegateCommandBus`
- `DelegateCommand`
- `DelegateCommandHandler`

This layer has no Flowable dependency. It is intentionally boring: just stable contracts.

### 3. Process infrastructure

The crimson layer is `infrastructure/process`.

This is the only layer that knows about Flowable. It contains:

- Flowable configuration
- BPMN and DMN resources
- JavaDelegate implementations
- implementations of `ProcessEventPort` and `ProcessStartPort`

So when the rest of the system needs to start or signal a process, it goes through the port. The Flowable-specific work happens here.

### 4. Flowable engine

The bottom layer is the embedded Flowable BPMN/DMN engine.

Flowable executes:

- process definitions
- timers
- event subprocesses
- user tasks
- service tasks
- DMN decisions

But it is reached only through `infrastructure/process`.

## Direction 1 — domain to engine

Use this example:

> A benefit cessation update arrives from an external integration.

The sequence is:

1. The integration/domain layer records the business fact.
2. The domain code calls `ProcessEventPort`.
3. `infrastructure/process` implements the port and fires the Flowable message, for example `BENEFIT_CEASED`.
4. Flowable catches that message in the active treatment process.
5. The process follows its BPMN event subprocess path, usually terminating the current treatment lane and triggering re-segmentation.

The important point:

> Domain code can trigger workflow behaviour without knowing how Flowable works.

## Direction 2 — engine to domain

Use this example:

> The BPMN process reaches a re-segmentation service task.

The sequence is:

1. Flowable invokes a JavaDelegate in `infrastructure/process`, for example `TriggerSegmentationDelegate`.
2. The delegate reads stable identifiers from the process context, such as `accountId`.
3. The delegate creates a command, such as `TriggerSegmentationCommand`.
4. The delegate dispatches the command through `DelegateCommandBus`.
5. The `strategy` module handles the command and determines the correct segment.
6. The process uses the result to route the account into the correct treatment path.

The important point:

> Flowable can ask the business modules to do work, but it does so through commands, not by directly importing or autowiring domain services.

## Where ongoing monitoring fits

Ongoing monitoring fits inside the BPMN treatment process as event subprocesses.

It is not a separate domain module and not a new architecture layer.

Examples of monitoring triggers:

- missed payment timer
- `BENEFIT_CEASED`
- `CIRCUMSTANCE_CHANGE`
- `BREATHING_SPACE_START`
- `INSOLVENCY_REGISTERED`
- `DEATH_REGISTERED`

The monitoring process detects that something has changed. Flowable then routes to the appropriate event subprocess.

For segmentation specifically:

1. A monitoring event fires, such as a change of circumstances or benefit cessation.
2. Flowable enters the `RESEGMENT` path.
3. A delegate dispatches a command through `DelegateCommandBus`.
4. The `strategy` module reads the current account/customer state and selects the correct segment.
5. Flowable starts or continues the treatment process that matches that segment.

This separation matters because the process engine controls the lifecycle, while `strategy` owns the actual segmentation decision.

## Important safety point

For compliance-sensitive routing, the system must not rely on stale Flowable process variables.

Process variables may have been set when the process started, but debt cases can run for weeks or months. A customer's status may change during that time.

So for decisions involving things like breathing space, deceased status, vulnerability, benefit status, or statute-barred status:

> The delegate or domain handler must read the latest state from the database before deciding how to route the account.

This is what keeps ongoing monitoring legally safe and operationally accurate.

## What this prevents

This design prevents three common failure modes:

1. Domain modules becoming tightly coupled to Flowable APIs.
2. BPMN delegates directly calling domain services and creating hidden dependencies.
3. Long-running workflows making routing decisions from stale process snapshots.

## Close-out message

End with:

> The process engine is the orchestration layer, not the business brain. Business decisions stay in the domain modules, Flowable execution stays in infrastructure, and the shared port layer keeps the boundary clean.

