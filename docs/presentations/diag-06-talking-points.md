# DIAG-06 Talking Points - Policy Bundle Lifecycle

## Opening - set the scene

"This diagram explains what happens when a business change is bigger than a single rule edit.

The previous diagram showed the three configuration tiers: reference data, DMN rules, and BPMN processes. This diagram shows the coordinating layer above them: the policy bundle.

A policy bundle is a named business change that links approved configuration items across tiers and activates them together on one effective date."

Then land the main message:

> "A policy bundle turns several technical configuration changes into one governed business change: one effective date, one audit trail, one rollback unit."

---

## Why a bundle is needed

"Start on the left. These are examples of changes that are not just one rule edit."

Use the fraud example:

"A fraud recovery approach might need a new debt category, stricter breach DMN rules, a fraud treatment BPMN path, and revised customer letters. If those go live separately, the system can land in a half-configured state."

Then the hardship example:

"A hardship relief policy might need a new hardship definition, I&E threshold rules, a forbearance process path, and adapted contact wording. Again, the point is coordination."

Key phrase:

> "DWP stakeholders think in policies, not in configuration tiers."

What can go wrong without a bundle:

- Tier 2 rules activate before the Tier 3 process exists.
- Customer wording changes before the underlying eligibility rule changes.
- Different components have different effective dates.
- Audit evidence is spread across several unrelated change records.
- Rollback requires manually finding and reverting each component.

---

## Centre panel - what the bundle links together

"The centre panel shows one policy bundle touching four governed surfaces."

Walk top to bottom:

### Tier 3 - Processes / BPMN

"At the top is the process change: for the fraud example, a new fraud treatment BPMN path. This changes the journey an account can follow."

### Tier 2 - Rules / DMN

"Next is the decision logic: fraud breach and routing DMN. These rules decide when an account qualifies for the fraud path or how breach handling changes."

### Tier 1 - Foundations / reference data

"Then the foundational vocabulary: a fraud debt category and action codes. These are stable IDs that the DMN rules can reference."

### Templates

"At the bottom is customer wording: the approved fraud contact template. Templates are not a configuration tier, because they are content rather than decision logic, but they can still be part of the same coordinated policy activation."

Presenter line:

> "The dashed orange box is the important idea: these separate approved items are governed as one business change."

---

## What the bundle adds

"The right-hand panel shows why the bundle exists."

### Coordinated activation

"All linked items go live together on the same effective date. That prevents partial rollout."

Example phrasing:

> "We do not want the fraud rule active on Monday, the fraud process path active on Wednesday, and the customer wording active the week after."

### Unified audit trail

"The policy gives us one identity for the change. If someone later asks 'what changed when Fraud Recovery 2026 was introduced?', we can answer with one policy record, not a scavenger hunt across separate admin screens."

The audit trail answers:

- what changed
- why it changed
- who approved it
- when it became active
- which components were included

### Rollback unit

"If the policy needs to be retired, the bundle knows which components belong together. Retiring the policy deactivates or reverts its linked components without rewriting history."

Important phrase:

> "Rollback does not delete history. It creates a new governed configuration state."

### Not every edit needs this

"The white box is just as important: not every edit needs a policy bundle. A single Tier 1 label correction, a standalone threshold adjustment, or a copy edit can still follow its normal lightweight workflow."

Key message:

> "Bundles are for coordinated business change, not routine admin maintenance."

---

## Bottom panel - lifecycle and failure rule

"The bottom strip is the policy lifecycle."

Walk it left to right:

### DRAFT - assemble

"In `DRAFT`, the business admin assembles the bundle: name, business intent, effective date, and the linked components across tiers."

### READY - components approved

"The bundle can move to `READY` only when every component has completed its own tier workflow. The policy bundle does not bypass Tier 2 DMN approval, Tier 3 Class A review, or template approval."

Important line:

> "The bundle coordinates approved components; it does not weaken their governance."

### APPROVED - sign-off held

"In `APPROVED`, the final sign-off is complete and the system is waiting for the effective date."

### ACTIVE - system activates

"On the effective date, the system activates all linked components together."

Examples:

- Tier 1 reference data is activated.
- Tier 2 DMN deployment becomes the active decision version.
- Tier 3 process mapping points to the approved BPMN version for new instances.
- Template version becomes available for dispatch where applicable.

### RETIRED - superseded

"Eventually a policy may be superseded or withdrawn. It moves to `RETIRED`, and its components are deactivated or reverted according to the retirement rules."

---

## Activation failure rule

"The red callout at the bottom is the failure rule. If activation fails, the system rolls the attempted activation back to `APPROVED`, not to `DRAFT`."

Explain why:

"That matters because the approvals are still valid. The policy is not un-designed and not un-approved; activation failed operationally. So it remains approved, an alert is raised, and the activation can be investigated or retried without rebuilding the whole policy."

Key phrase:

> "Activation is atomic: all components activate together, or none do."

---

## Important constraints

"There are three constraints to make clear."

1. "The bundle does not bypass each tier's own governance."
2. "The bundle does not change in-flight process snapshots; existing accounts continue under their captured versions unless a separate governed migration is approved."
3. "The bundle cannot override statutory hard stops such as breathing space, insolvency, deceased handling, mandatory suppression, or audit evidence."

Close this section with:

> "The policy layer coordinates flexibility; it is not a shortcut around control."

---

## Likely questions

**"Is a policy bundle required for every configuration change?"**
No. It is used when changes have a named business intent and need coordinated activation across tiers or templates. Routine single-tier changes keep their normal workflow.

**"Does policy approval replace Tier 2 or Tier 3 approval?"**
No. Each component must still pass its own governance path. The bundle can become `READY` only when all linked components are individually approved.

**"What happens if one component fails activation?"**
The whole activation rolls back to `APPROVED`. Nothing is partially activated. An alert is raised so the failure can be investigated.

**"Does retiring a policy rewrite historical decisions?"**
No. History is append-only. Retirement creates a new configuration state and records why the policy was retired. Past decisions and audit records remain intact.

**"What happens to accounts already in-flight?"**
They continue using their captured configuration snapshot. New policy versions affect new instances unless a separate, governed migration is approved.

**"Can a policy bundle include templates?"**
Yes. Templates are not one of the three configuration tiers, but they are governed content and can be bundled when customer wording must activate with the business rule or process change.

**"Who owns the bundle?"**
The policy has a named owner role and final approver, typically `OPS_MANAGER` or `COMPLIANCE` depending on the policy type. The component authors and approvers remain governed by their tier.

---

## Close-out message

"The policy bundle is what makes business-owned change safe at scale. It lets DWP introduce a recognisable policy, like Fraud Recovery 2026, while DCMS keeps the technical pieces coordinated, approved, effective-dated, auditable, and rollback-safe."

---

*Sources: diag-06-policy-bundle-lifecycle.drawio, ADR-009, ADR-015, MASTER-DESIGN-DOCUMENT*
