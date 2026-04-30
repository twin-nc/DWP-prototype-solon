# DIAG-05 Talking Points - Three-Tier Configuration Model

## Opening - set the scene

"This diagram explains how DCMS lets the business change strategy without turning every change into a software release.

The important point is that not all configuration is the same. A vulnerability type, a routing rule, a treatment process, and a customer letter template all change the system in different ways. So they need different governance, different author roles, and different approval evidence."

Then land the main message:

> "DCMS separates business change into three governed tiers: definitions, rules, and process structure. Business users can change strategy, but statutory protections and approval evidence remain non-negotiable."

---

## Where this sits in the system

"This sits in the Configuration Workspace at `/admin`, with policy coordination under `/admin/policies`. It is not a developer-only back office tool. It is the governed business-authoring surface."

Useful context:

- OPS_MANAGER maintains Tier 1 reference data.
- PROCESS_DESIGNER authors Tier 2 DMN rules and Tier 3 BPMN processes.
- COMPLIANCE approves Tier 2 rules, Tier 3 process changes, and governed customer wording.
- Regulatory hard stops are not made configurable.

Presenter line:

> "The business can change operating strategy through the system, but the system controls who can change what and what evidence is required."

---

## Talk through the tiers bottom-up

Start at the bottom because the upper tiers depend on the lower tiers.

---

## Tier 1 - Reference Data and Definitions

"Tier 1 is the green layer: reference data and definitions. This is the vocabulary the rest of the system uses."

Examples:

- vulnerability types
- suppression reasons
- action codes
- debt categories
- communication channels

"Tier 1 answers: what are the allowed values in the system?"

How to explain it:

"This is not decision logic. It does not say what treatment path to use or whether an action is allowed. It defines stable codes and labels that rules can reference."

Governance:

- Authored by Operations / `OPS_MANAGER`.
- Stable IDs are audited.
- Changes are rare or as-needed.
- Behaviour-changing definitions are controlled.

Useful phrase:

> "Tier 1 gives the system a governed vocabulary."

---

## Tier 2 - DMN Decision Tables

"Tier 2 is the purple layer: DMN decision tables. This is where business rule logic lives."

Examples:

- segmentation rules
- routing rules
- vulnerability action matrix
- thresholds and tolerances
- champion/challenger rules

"Tier 2 answers: given the facts we know, what should the system do?"

Explain the dependency:

"Tier 2 rules reference Tier 1 stable IDs. For example, a DMN table might say: if vulnerability type is X and action code is Y, then block, escalate, or allow the action."

Governance:

- Authored by `PROCESS_DESIGNER`.
- Approved by `COMPLIANCE`.
- Versioned, testable, and effective-dated.
- Monthly cadence is expected.
- Every decision records the rule version used.

Presenter line:

> "This is the no-code or low-code strategy layer: business users can change decision logic without asking developers to redeploy the application."

---

## Tier 3 - BPMN Process Definitions

"Tier 3 is the red layer: BPMN process definitions. This is the structure of the workflow."

Examples:

- treatment paths
- escalation flows
- exception event subprocesses
- integration handoffs

"Tier 3 answers: in what sequence should this debt move through the system?"

Explain how it uses Tier 2:

"The process does not hardcode all decisions. Process tasks evaluate Tier 2 decision tables. So BPMN provides the journey, and DMN decides key branches within that journey."

Governance:

- Authored by Business Admin / `PROCESS_DESIGNER` via Flowable Modeler.
- Approved by `COMPLIANCE`.
- Class A gate for structural process changes.
- Quarterly cadence is expected.
- New process versions affect new instances only unless a governed migration is approved.

Key phrase:

> "Tier 3 changes the shape of the journey; Tier 2 changes decisions inside the journey."

---

## Runtime snapshot - why in-flight behaviour stays deterministic

"The blue box on the left is critical. At process start, each account captures the configuration versions it is running under."

The snapshot records:

- Tier 1 IDs and labels.
- Tier 2 DMN version.
- Tier 3 BPMN version.

"That means an in-flight account does not suddenly change behaviour because someone updates a rule table halfway through the process. New accounts can use the new approved versions; existing accounts stay deterministic unless we explicitly migrate them."

Presenter line:

> "Configuration is flexible, but live account behaviour is not random. The account knows which configuration version it started with."

---

## Author and approval roles

"The white box on the left is the separation-of-duties model."

Roles:

- `OPS_MANAGER` maintains Tier 1 reference data.
- `PROCESS_DESIGNER` authors Tier 2 DMN, Tier 3 BPMN, and template drafts.
- `COMPLIANCE` approves rules, process changes, and contact wording.

"This prevents a single user from both creating and approving high-impact behaviour changes. The approval role is deliberately separate from the author role."

Important point:

> "Regulatory hard stops are not made configurable. Nobody in the admin UI can configure away breathing space, deceased handling, statutory suppression, audit evidence, or other legal protections."

---

## Templates - adjacent, not Tier 2

"On the right is an adjacent governed surface: templates."

Explain:

"Templates are customer contact wording for letters, SMS, email, and in-app messages. They are governed, versioned, approved, and effective-dated, but they are not Tier 2 DMN rules."

Why:

- Template wording is content, not decision logic.
- DMN decides whether or when contact is allowed.
- Templates define what the customer sees once contact is allowed.

Key phrase:

> "DMN decides whether contact is allowed. Templates decide what approved wording is used."

Mention the callouts:

- Policy bundles can include template versions when wording must activate with a business rule change.
- Single-template copy edits can still use the template workflow without creating a policy bundle.

---

## Policy bundles - coordinated change

"The orange dashed boundary is the policy workspace idea. A policy bundle groups related changes across tiers, and sometimes templates, under one business intent and one effective date."

Example:

"If DWP introduces a new vulnerability handling approach, that may require a new Tier 1 vulnerability type, Tier 2 rules for suppression and routing, a Tier 3 treatment path, and new customer wording. Those should not go live on different days."

Policy bundle gives:

- one named business change
- one effective date
- one approval trail
- coordinated activation
- one rollback unit

Presenter line:

> "A policy bundle prevents partial configuration: the vocabulary, rules, process, and wording activate together or not at all."

---

## What this diagram is trying to prove

"There are four claims this slide makes."

1. "Business change is split by risk and meaning: definitions, rules, process, and content."
2. "Each tier has a named author role and an approval path."
3. "Runtime snapshots make in-flight behaviour deterministic."
4. "Policy bundles coordinate multi-tier changes so the system never lands in a half-configured state."

Close this section with:

> "This is how DCMS gives the business control over strategy without giving anyone the ability to accidentally bypass legal safeguards."

---

## Likely questions

**"Why three tiers instead of one big configuration screen?"**
Because these are different kinds of change. Reference definitions, behavioural rules, and process structure have different risk levels, author skills, test evidence, and approval needs. Combining them would blur governance and make audit weaker.

**"Why is Tier 2 DMN rather than database rows?"**
DMN is a decision-table standard suited to business-readable if-then logic. It supports versioned decision definitions, testing, and clearer review than ad hoc database configuration for behavioural rules.

**"Can business users change processes without developers?"**
Yes, when the process is assembled from existing service-task capabilities. If a new process step needs a capability that does not exist, it enters an implementation handoff before it can be approved and activated.

**"Do in-flight accounts change when a new rule is approved?"**
No, not automatically. Each process captures the Tier 1, Tier 2, and Tier 3 versions at process start. New versions apply to new instances unless a governed migration is explicitly approved.

**"Why are templates separate?"**
Templates are customer-facing content, not decision logic. They need wording review and content audit, not DMN rule simulation. They can still be bundled with policy changes when wording and rules need to activate together.

**"Who can approve their own change?"**
No one for governed Tier 2, Tier 3, or template changes. `PROCESS_DESIGNER` proposes; `COMPLIANCE` approves. This keeps separation of duties intact.

**"Can statutory protections be changed in these tiers?"**
No. Business users can change strategy, thresholds, routing, and treatment paths, but they cannot configure away statutory protections such as breathing space handling, deceased handling, mandatory communication suppression, audit evidence, or legal holds.

---

## Close-out message

"The three-tier model is the control system for business-owned change. It lets DWP adjust strategy through governed configuration, while versioning, approval, snapshots, and policy bundles keep the system deterministic, auditable, and legally safe."

---

*Sources: diag-05-three-tier-configuration-model.drawio, ADR-008, ADR-009, ADR-015*
