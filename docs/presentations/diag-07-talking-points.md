# DIAG-07 Talking Points - Champion/Challenger

## Opening - set the scene

"This diagram shows how DCMS tests a proposed strategy change on live accounts without rolling it out to everyone at once.

The current approved strategy is the `CHAMPION`. The proposed alternative is the `CHALLENGER`. Both are approved DMN versions, and the system routes eligible accounts between them so we can compare real outcomes before deciding whether to promote the challenger."

Then land the main message:

> "Champion/challenger gives DWP evidence-based strategy change, but with protected-population guardrails, deterministic assignment, and governed promotion."

---

## Start on the left - account routing

"Start with the left panel. Incoming accounts arrive for strategy evaluation. The first question is not 'which percentage split applies?' The first question is: is this a vulnerable customer?"

### Vulnerable customers

"If the account is associated with a vulnerable customer, the account is forced to the champion variant."

Why:

"The current policy is that vulnerable customers must not receive experimental challenger treatment. They still appear in outcome monitoring, but not in the A/B comparison panel."

Key phrase:

> "Protected-population eligibility is checked before the split. The split never overrides vulnerability handling."

Important nuance:

"The diagram says changing this rule is possible only as a Class A policy change requiring DWP sign-off. So it is policy-governed, not a casual experiment setting."

### Non-vulnerable customers

"If the customer is not vulnerable, the account goes through deterministic A/B routing."

The routing rule:

> "`abs(accountId.hashCode()) % 100` is compared with the challenger percentage."

Explain in plain English:

"Each account gets a stable bucket from 0 to 99. If the challenger percentage is 10%, roughly 10 out of 100 eligible accounts go to challenger. The same account always lands in the same arm for the duration of the experiment."

Why deterministic matters:

- It is reproducible from logged identifiers.
- It avoids random reassignment on later evaluations.
- It makes audit and outcome attribution possible.

Presenter line:

> "This is not a coin toss every time the account is evaluated. It is stable, deterministic assignment."

---

## Champion and challenger variants

"The green box is the champion: the current production strategy and the majority of accounts. The purple box is the challenger: the approved alternative being tested on a configured percentage."

What can differ:

- decision thresholds
- routing outputs
- treatment path assignment
- challenger split percentage

Example:

"The champion may use a GBP 500 threshold for enhanced monitoring, while the challenger tests GBP 350. Or the champion may route a debt category to queue A, while the challenger tests queue B for faster cycle time."

Important point:

> "The challenger is not an unapproved draft. It is an approved DMN version being tested under controlled conditions."

---

## Middle panel - comparison dashboard

"The middle panel is how we decide whether the challenger is actually better."

### A/B comparison

"The top comparison panel is for non-vulnerable accounts only: champion versus challenger on declared metrics."

Example metrics:

- account count
- total recovered
- arrangements made
- average payment per account

"This gives us a clean A/B comparison for the population that was eligible for the split."

### Vulnerable outcomes

"The second panel is vulnerable outcomes. Vulnerable accounts are all on champion, so there is no A/B split here. But we still monitor their outcomes as a separate cohort."

Why:

"This prevents the experiment from ignoring harm indicators or hiding the effect on protected populations just because they are excluded from challenger treatment."

### Overall population

"The third panel shows the overall population view. This is the operational picture: what is happening across all accounts, not just the experiment comparison set."

### Promotion guards

"The green promotion guard box is there to stop premature promotion. Minimum sample size and minimum duration guard against promoting a challenger after a short lucky run."

Key phrase:

> "The dashboard can show positive results, but promotion still has guardrails."

---

## Role boundary

"The red box under the dashboard is a role boundary."

Explain:

- `OPS_MANAGER` can view the panels.
- `OPS_MANAGER` cannot trigger promotion.
- Promotion is a `STRATEGY_MANAGER` action.

"That separation matters because monitoring operations and changing strategy are different responsibilities. Promotion changes which rule version governs future accounts, so it has a named owner and audit event."

---

## Right panel - what can and cannot change

"The right panel is the change boundary."

### What can change

"The challenger DMN can test different thresholds, different routing outputs, different treatment-path assignment logic, and a configured split percentage."

Examples:

- Lowering an enhanced monitoring threshold.
- Routing a category to a different queue.
- Trying a lighter-touch first contact path.
- Setting the challenger percentage from 0 to 50%.

### What cannot change

"The red section is the hard constraint area."

Cannot be changed casually:

- Protected-population eligibility.
- Hash routing mechanism.
- Metric collection.
- Mandatory outcome recording.

Key phrase:

> "The experiment can test strategy; it cannot turn off the measurement or safety system."

Protected-population point:

"Vulnerable customers always go to champion under the current policy. Allowing them into challenger treatment would require a governed policy change, not a per-experiment toggle."

Hash routing point:

"The hash mechanism is locked so an account stays on its assigned variant. This prevents contamination of the results."

Metric point:

"Outcome records are written for all accounts. You cannot suppress measurement for a population just because it might make the experiment look worse."

---

## Governance path

"At the bottom of the right panel is the governance path."

Walk it:

1. `PROCESS_DESIGNER` authors the challenger DMN.
2. `COMPLIANCE` approves it.
3. Simulation is required if eligibility or outcome behaviour materially changes.
4. `STRATEGY_MANAGER` activates or promotes through the Class A gate.

Presenter line:

> "The experiment tests an already-approved rule version; it does not smuggle unapproved logic into production."

---

## Bottom panel - promotion path

"The bottom strip shows what happens if the challenger wins."

Walk left to right:

1. Challenger starts as the tested alternative.
2. `STRATEGY_MANAGER` triggers promotion from the Operations Workspace.
3. The challenger becomes the new champion.
4. A `CC_PROMOTION` audit event records who promoted it, when, and which experiment evidence supported the decision.

"OPS_MANAGER can read the results, but cannot promote. Promotion is deliberately tied to the `STRATEGY_MANAGER` role."

Key phrase:

> "Promotion is evidence-backed, role-controlled, and auditable."

---

## What this diagram is trying to prove

"There are four design claims on this slide."

1. "Assignment is deterministic, not random at each evaluation."
2. "Vulnerable customers are protected from challenger treatment under the current policy."
3. "Outcomes are measured across comparison, protected, and overall population views."
4. "Promotion is a governed strategy action with audit evidence."

Close this section with:

> "This is how DCMS can learn from real operational outcomes without treating vulnerable customers as experiment subjects or losing the audit trail."

---

## Likely questions

**"Is this the same as simulation?"**
No. Simulation is offline and read-only: it asks what would have happened historically. Champion/challenger is live experimentation on eligible new accounts, with deterministic routing and outcome measurement.

**"Can vulnerable customers ever go to challenger?"**
Not under the current approved policy. Changing that would be a Class A policy change requiring DWP sign-off and updated eligibility governance.

**"Why use hash routing instead of random assignment?"**
Hash routing is deterministic. The same account stays in the same arm, and the assignment can be reconstructed later from the account ID and experiment ID.

**"Can the challenger be a draft rule?"**
No. The challenger DMN must already be approved. The experiment tests an approved alternative under controlled traffic, not unreviewed logic.

**"What stops a weak result being promoted?"**
Promotion guards such as minimum sample size and minimum duration. The dashboard also includes harm indicators, not just financial uplift.

**"Who can promote the challenger?"**
`STRATEGY_MANAGER`. `OPS_MANAGER` can view results but cannot trigger promotion.

**"What happens after promotion?"**
The challenger DMN becomes the new champion, the old champion is retired as the active version, and an audit event records the promotion with experiment provenance.

---

## Close-out message

"Champion/challenger is the controlled live-learning mechanism. It lets DWP compare strategy variants using real outcomes, while deterministic routing, protected-population rules, outcome metrics, promotion guards, and audit events keep the experiment governable."

---

*Sources: diag-07-champion-challenger.drawio, ADR-010, RULING-010, strategy-simulation-engine-design.md*
