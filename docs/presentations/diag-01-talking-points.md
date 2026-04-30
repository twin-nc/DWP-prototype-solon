# DIAG-01 Talking Points - Debt Lifecycle

## Opening - set the scene

"This slide is the scope anchor for DCMS. It shows the full DWP debt journey from the point a debt is identified through to assessment, recovery, enforcement, and write-off.

The important number is at the top: 442 functional requirements, grouped into 19 confirmed capability groups. This diagram is not trying to show every requirement individually. It shows where those requirement families sit in the end-to-end journey."

Then land the main message:

> "DCMS is not a single debt ledger or a single workflow screen. It is the operating system for the whole debt lifecycle."

---

## How to read the diagram

"Read it left to right across the five lifecycle stages. Each coloured column is a stage in the debt journey. The chips inside each column are capability groups: families of requirements that primarily operate in that stage."

Mention the small note:

"Some capability groups appear more than once because they span the lifecycle. That is intentional. For example, audit, workflow, work allocation, and reporting do not live in only one moment of the journey."

Useful phrase:

> "The chips show primary operating areas, not strict handoff boundaries."

---

## 1. Identification

"The first stage is Identification: debt referral, customer and account creation, evidence capture, and source-system intake."

What happens here:

- DCMS receives debt referrals from source systems.
- Customer and account records are created or matched.
- Evidence and supporting information are captured.
- Migration data can enter the system.
- The system establishes the initial source-of-record account facts.

Capability groups shown:

- `CAS` - Customer & Account Structure
- `DIC` - Data & Information Capture
- `MR` - Migration Requirements
- `SoR` - Source account record / System of Record

Presenter line:

> "This is where the debt becomes a managed DCMS account rather than just an inbound referral."

---

## 2. Assessment

"The second stage is Assessment. Here the system understands the account, the customer circumstances, and the right treatment path."

What happens here:

- Bureau and scorecard feeds enrich the view.
- Analytics and segmentation classify the account.
- Income and expenditure may be captured.
- Decisions and workflows choose the treatment route.
- Work is allocated to the right queue or user group.

Capability groups shown:

- `BSF` - Bureau & Scorecard Feeds
- `A` - Analytics & Segmentation
- `IEC` - Income & Expenditure Capture
- `DW` - Decisions & Workflows
- `WAM` - Work Allocation & Monitoring

Key phrase:

> "Assessment is where DCMS moves from 'we have a debt' to 'we know how this debt should be treated.'"

---

## 3. Recovery

"The third stage is Recovery: the main operational heart of the system."

What happens here:

- The system manages repayment arrangements.
- It supports payments and benefit-handling mechanics.
- It manages customer contact across channels.
- Agents record actions and dispositions.
- Ledger, payment, and audit facts are updated.
- Gateway and portal integrations feed in payments, I&E, messages, and customer actions.

Capability groups shown:

- `RPF` - Repayment Plan Functionality
- `CC` - Contact Channels
- `AAD` - Agent Actions & Dispositions
- `SoR` - payments, ledger, and audit facts
- `I3PS` - gateway and portal interfaces

Presenter line:

> "Recovery is not just taking payments. It is contact, arrangement management, payment posting, audit evidence, and integration with the channels around the customer."

---

## 4. Enforcement

"The fourth stage is Enforcement. This is where accounts eligible for escalation move into DCA, legal, field-agent, or other specialist enforcement routes."

What happens here:

- Third-party debt collection agency placement and recall are managed.
- Legal or field interfaces may be triggered.
- Disclosure and escalation paths are controlled.
- Specialist queues and oversight keep high-risk work visible.

Capability groups shown:

- `3PM` - 3rd Party Management
- `I3PS` - DCA / legal / field interfaces
- `DW` - disclosure and escalation paths
- `WAM` - specialist queues and oversight

Important point:

> "Enforcement is gated. Accounts do not simply fall into DCA or legal action; eligibility, disclosure, vulnerability, breathing space, and other protections have to be checked before escalation."

---

## 5. Write-Off

"The fifth stage is Write-Off: controlled closure where the system approves, evidences, records, and reports the outcome."

What happens here:

- Write-off or closure decisions are controlled through workflow.
- Role limits and approvals are enforced.
- Source-of-record closure and write-off facts are recorded.
- MI and reporting evidence is produced.

Capability groups shown:

- `DW` - controlled outcome workflow
- `UAAF` - role limits and approvals
- `SoR` - write-off / closure record
- `MIR` - MI & Reporting

Presenter line:

> "Write-off is not a delete button. It is a governed outcome with approvals, evidence, ledger impact, and reporting."

---

## Cross-lifecycle controls

"The bottom band shows the capabilities that cut across every stage."

Walk through them:

- `UI` - staff screens and journeys across every stage.
- `CP` - change process and release controls.
- `SD` - roadmap and delivery governance.
- `UAAF` - access control and admin boundaries.
- `AAD / SoR / MIR` - actions, audit facts, and reporting evidence.
- `DW` - BPMN lifecycle control across the whole journey.

Key message:

> "These are not side features. They are the controls that make the lifecycle governable."

Explain:

"For example, access control matters at identification, recovery, enforcement, and write-off. Audit evidence matters at every stage. Workflow is not one feature; it is the lifecycle control running through the journey."

---

## What this diagram is trying to prove

"There are four claims this slide makes."

1. "The full scope is known: 442 requirements across 19 capability groups."
2. "Every capability group has a place in the lifecycle."
3. "The system covers the full journey, not only recovery or case management."
4. "Governance, audit, access control, and workflow run across the whole lifecycle."

Close this section with:

> "This gives us a shared map: when we talk about architecture, release scope, or demo coverage later, this is the journey those decisions are serving."

---

## Likely questions

**"Are these columns separate systems?"**
No. They are lifecycle stages. The underlying architecture is one DCMS system with domain modules and workflow orchestration; the columns help explain where capability groups primarily operate.

**"Why do some groups appear more than once?"**
Because some capabilities span more than one lifecycle moment. `DW`, `SoR`, `WAM`, and `I3PS` are good examples: workflow, record keeping, allocation, and integration recur across the journey.

**"Does this mean all 442 requirements are fully designed?"**
The requirement groups are confirmed and mapped. Some areas still have blockers or design depth to complete, but the ownership and lifecycle placement are known.

**"Where does customer self-service sit?"**
In the current baseline, DCMS exposes backend integration capability for the future DWP self-service portal. That primarily appears through `I3PS`, `IEC`, `RPF`, `CC`, and `SoR`; the public portal UI itself is outside DCMS.

**"Why is write-off a lifecycle stage rather than just an account status?"**
Because write-off carries approval, evidence, financial record, role-limit, audit, and reporting requirements. It is an outcome workflow, not just a flag.

**"Where do statutory protections fit?"**
They run through the lifecycle. Vulnerability, breathing space, deceased handling, insolvency, suppression, and affordability gates can affect assessment, recovery, enforcement, and write-off decisions.

---

## Close-out message

"This is the whole DCMS problem space in one picture: identify the debt, assess the right treatment, recover fairly, escalate only when permitted, and close or write off with evidence. The rest of the deck explains how the architecture makes that lifecycle controlled, auditable, and deliverable."

---

*Sources: diag-01-debt-lifecycle.drawio, leadership-presentation-outline.md, functional-requirements-module-map.md, MASTER-DESIGN-DOCUMENT*
