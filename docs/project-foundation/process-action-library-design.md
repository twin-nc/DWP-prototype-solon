> ## ⚠ STATUS: UNSETTLED — PLATFORM PIVOT IN EFFECT
>
> A confirmed programme-level decision (ADR-018, 2026-04-30) has pivoted the project to build on top of **Solon Tax** as the base platform. A new design process is underway.
>
> **This document contains accumulated knowledge and analysis that remains relevant to the new design process. It does NOT represent the current or preferred direction.** All decisions and conclusions herein are open for renewed debate. Do not treat this document as a directive or default position.
>
> See [`CLAUDE.md`](../../CLAUDE.md) and [ADR-018](decisions/ADR-018-platform-pivot-solon-tax-confirmed.md) for the current direction.

---

# Process Action Library Design

**Project:** DWP Debt Collection Management System (DCMS)  
**Date:** 2026-04-27  
**Status:** Design baseline  
**Purpose:** Define the governed catalogue of reusable Flowable process actions available to BPMN process designers.

---

## 1. Design Intent

The old additional-layer diagrams referred to a "Step Library" and "Step Library Editor". That Solon-style model is not part of the DCMS architecture: we are not building a JSON-to-BPMN assembler, a separate step-library database, or a separate task configurator application.

DCMS still needs a governed library of reusable process actions so PROCESS_DESIGNER users can compose BPMN workflows safely in Flowable. This document defines that library in Flowable terms.

The DCMS name for this capability is the **Process Action Library**.

---

## 2. Definition

A **process action** is a reusable BPMN service-task capability exposed to Flowable through the delegate-command pattern in ADR-003.

Each process action has:

- a stable action key
- a stable Flowable delegate bean name
- a command type in `shared/process/port`
- a command handler in the owning domain module
- declared input variables
- declared output variables or emitted events
- validation rules
- allowed BPMN usage contexts
- ownership, version, and audit metadata

Process actions are not domain services themselves. They are governed contracts between BPMN models and domain-owned command handlers.

---

## 3. Architecture Placement

| Concern | Placement |
|---|---|
| BPMN service task | Flowable BPMN process definition |
| Delegate implementation | `infrastructure/process/delegate` |
| Command type and shared interfaces | `shared/process/port` |
| Business behaviour | Owning domain module command handler |
| Catalogue metadata | Application configuration schema, surfaced in `/admin/processes/action-library` |
| Approval and audit | Configuration governance workflow and `configuration_audit_log` |

Only `infrastructure/process` imports Flowable types. Domain modules must not import Flowable.

---

## 4. Catalogue Record

Each process action catalogue entry must contain:

| Field | Meaning |
|---|---|
| `action_key` | Stable logical key, e.g. `communications.send-letter` |
| `display_name` | Human-readable name shown to PROCESS_DESIGNER |
| `description` | What the action does and when it should be used |
| `owner_module` | Domain module that owns the command handler |
| `delegate_bean_name` | Stable Flowable delegate expression target, e.g. `sendLetterDelegate` |
| `command_type` | Java command type in `shared/process/port` |
| `input_schema` | Required and optional process variables, types, and source rules |
| `output_schema` | Variables written back, events emitted, or "none" |
| `allowed_contexts` | Treatment processes, event subprocesses, admin-only recovery flows, or specific process families |
| `guardrails` | Non-overridable constraints, e.g. "suppression checked in communications module" |
| `idempotency_key_rule` | How duplicate execution is detected or prevented where needed |
| `failure_semantics` | BPMN error, retryable technical failure, or manual-review evidence |
| `version` | Catalogue contract version |
| `status` | `DRAFT`, `ACTIVE`, `DEPRECATED`, or `RETIRED` |
| `effective_from` | Date/time from which this contract is valid |

Catalogue entries are contracts. Renaming `action_key`, `delegate_bean_name`, or `command_type` is a breaking change for any BPMN model that references the action.

---

## 5. Initial Action Catalogue

This is the initial v1 catalogue derived from ADR-003 and the master design.

| Action key | Delegate bean | Command type | Owner module | Primary use |
|---|---|---|---|---|
| `communications.send-letter` | `sendLetterDelegate` | `SendLetterCommand` | `communications` | Generate and queue a letter through the communications module |
| `communications.send-sms` | `sendSmsDelegate` | `SendSmsCommand` | `communications` | Generate and queue an SMS through the communications module |
| `repaymentplan.assess-affordability` | `assessAffordabilityDelegate` | `AssessAffordabilityCommand` | `repaymentplan` | Run I&E affordability assessment and expose decision outcome |
| `payment.post-deduction` | `postPaymentDeductionDelegate` | `PostPaymentDeductionCommand` | `payment` | Record a benefit deduction or payment posting event |
| `strategy.trigger-segmentation` | `triggerSegmentationDelegate` | `TriggerSegmentationCommand` | `strategy` | Re-evaluate treatment routing using configured DMN/rules |
| `workallocation.create-work-item` | `createWorkItemDelegate` | `CreateWorkItemCommand` | `workallocation` | Create an agent, specialist, or compliance work item |
| `account.record-note` | `recordAccountNoteDelegate` | `RecordAccountNoteCommand` | `account` | Add a system note to the account timeline |
| `account.record-regulatory-fact` | `recordRegulatoryFactDelegate` | `RecordRegulatoryFactCommand` | `account` | Record durable regulatory facts such as breathing-space or insolvency dates |

This catalogue is intentionally small. New entries are added only when a BPMN process needs a reusable action that cannot be represented by an existing action plus configuration.

---

## 6. Governance Rules

### 6.1 Adding a process action

Adding a new process action requires:

1. Catalogue entry in `process_action_catalogue`.
2. Command type in `shared/process/port`.
3. Delegate implementation in `infrastructure/process/delegate`.
4. Command handler in the owning domain module.
5. Unit tests for delegate-to-command mapping.
6. Unit or integration tests for the command handler.
7. BPMN coverage validation proving the delegate bean is registered.

If the action can alter live debt collection behaviour, it is a Class A change and requires Domain Expert + Solution Architect sign-off before deployment.

### 6.2 Changing a process action

Changes are classified as:

| Change | Classification |
|---|---|
| Add optional input with default | Additive |
| Add output variable not used by existing BPMN | Additive |
| Rename action key, command type, or delegate bean | Breaking |
| Remove or change required input | Breaking |
| Change business outcome for same inputs | Behaviour change, usually Class A |
| Deprecate action but keep runtime support | Non-breaking if all active BPMN remains supported |
| Retire action and remove delegate support | Breaking unless no deployed/in-flight BPMN can reference it |

Breaking changes require BPMN impact analysis and migration handling under ADR-006.

### 6.3 Deprecation and retirement

Deprecated actions remain executable for all active and in-flight BPMN definitions. A deprecated action is hidden from new process-authoring palettes but remains visible in read-only process inspection.

An action can be retired only when:

- no active BPMN definition references it
- no in-flight process instance can reach it
- migration evidence or deployment history proves it is no longer reachable

---

## 7. Admin UI Behaviour

The Process Action Library is surfaced under:

`/admin/processes/action-library`

It is part of the Tier 3 Processes section from ADR-015.

| Role | Access |
|---|---|
| PROCESS_DESIGNER | Read catalogue; propose new or changed entries |
| COMPLIANCE | Read and approve entries where approval is required |
| FLOWABLE_ADMIN | Read operational registration status |
| TEAM_LEADER / OPS_MANAGER | No write access; read access only if needed for operational context |
| AGENT roles | No access |

The Flowable Modeler must use this catalogue as the allowed service-task palette where technically feasible. If the embedded modeler cannot restrict the palette directly, save-time validation must reject BPMN definitions that reference unknown or disallowed delegate bean names.

---

## 8. Service Task Coverage Validation

Every BPMN deployment candidate must pass service-task coverage validation before it can move from draft to approval.

Validation checks:

- every `flowable:delegateExpression` resolves to an active or deprecated catalogue entry
- the referenced action is allowed in the current process context
- required input variables are present or produced by an upstream step
- output variables do not overwrite protected process variables
- deprecated actions are allowed only in existing process versions or approved migrations
- no direct domain service invocation is embedded in BPMN

Failed validation moves the deployment to `AWAITING_IMPLEMENTATION` or `REJECTED`, depending on whether the missing action is planned or invalid.

---

## 9. Persistence Sketch

The process action catalogue can be implemented as a table once `infrastructure/process` and `shared/process/port` are scaffolded.

```sql
process_action_catalogue
  id UUID PRIMARY KEY
  action_key VARCHAR NOT NULL
  version INT NOT NULL
  display_name VARCHAR NOT NULL
  description TEXT NOT NULL
  owner_module VARCHAR NOT NULL
  delegate_bean_name VARCHAR NOT NULL
  command_type VARCHAR NOT NULL
  input_schema JSONB NOT NULL
  output_schema JSONB NOT NULL
  allowed_contexts JSONB NOT NULL
  guardrails JSONB NOT NULL
  idempotency_key_rule TEXT NULL
  failure_semantics VARCHAR NOT NULL
  status VARCHAR NOT NULL
  effective_from TIMESTAMP NOT NULL
  created_by VARCHAR NOT NULL
  approved_by VARCHAR NULL
  created_at TIMESTAMP NOT NULL
  updated_at TIMESTAMP NOT NULL
```

Recommended constraints:

- unique `(action_key, version)`
- unique active `delegate_bean_name`
- status enum: `DRAFT`, `ACTIVE`, `DEPRECATED`, `RETIRED`
- JSON schema validation for `input_schema`, `output_schema`, and `allowed_contexts`

---

## 10. Explicit Non-Goals

The Process Action Library is not:

- a JSON-to-BPMN strategy compiler
- a separate Step Library DB outside the monolith
- a drag-and-drop strategy assembler replacing BPMN authoring
- a separate Task Configurator application
- a way for business users to bypass Class A governance
- a replacement for domain services or domain invariants

---

## 11. Open Design Questions

| Question | Impact |
|---|---|
| Can the embedded Flowable Modeler palette be restricted directly from the catalogue? | If not, enforcement relies on save-time validation. |
| Should catalogue entries be database-managed from day one or initially seeded from code/YAML? | Database management supports admin UI earlier; seed files reduce first implementation complexity. |
| What exact input/output schema format should be used? | JSON Schema is the default recommendation unless the implementation chooses a lighter internal schema. |
| Should PROCESS_DESIGNER be able to propose catalogue entries directly, or should new entries require developer implementation first? | Affects whether `AWAITING_IMPLEMENTATION` is a design queue or a developer backlog handoff state. |

---

## 12. Related Decisions

- ADR-003: Process engine as infrastructure and delegate-command pattern
- ADR-006: BPMN versioning and in-flight migration policy
- ADR-008: Three-tier configurability architecture
- ADR-009: Policy layer and cross-tier change bundles
- ADR-015: Configuration surface and role-scoped admin UI
- `MASTER-DESIGN-DOCUMENT.md` section 5: Process Engine Architecture
