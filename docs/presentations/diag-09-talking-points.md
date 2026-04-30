# DIAG-09 Talking Points - Vulnerability Handling

## Opening - set the scene

"This diagram shows what happens when the system identifies that a customer may be vulnerable.

The core idea is that vulnerability is not just a label on a customer record. It changes what the system is allowed to do: communications may be suppressed, I&E becomes mandatory, affordability has to be checked, repayment arrangements may need forbearance, and the case may move into specialist handling."

Then land the main message:

> "Vulnerability is policy-defined, rule-applied, and process-routed, but the legal guardrails cannot be configured away."

---

## Walk the flow left to right

"The top row is the operational path. Read it from left to right."

---

## 0. Tier 1 defines vulnerability taxonomy

"The first green box is Tier 1. This is where DWP defines the approved vulnerability taxonomy."

Tier 1 defines:

- vulnerability type
- severity
- evidence source
- data classification
- version
- effective date

"This matters because the system cannot invent its own vulnerability categories. What counts as a vulnerability, what evidence is acceptable, and how sensitive that data is are DWP policy decisions."

Useful phrase:

> "Tier 1 defines the governed vocabulary for vulnerability."

Mention the governance point:

"Changing the taxonomy is not routine operational admin. It requires DWP policy approval because it changes the population that receives protected treatment."

---

## 1. Agent applies vulnerability flag

"The second box is the agent action. An agent applies a governed Tier 1 vulnerability flag to the Party record."

Explain:

"The flag is recorded against the person, not buried inside a single account. That allows every linked account to respect the same person-level protection."

Important access-control point:

"A standard `AGENT` should not see all vulnerability details. They see an indicator and referral instruction. Full details are restricted to roles like `SPECIALIST_AGENT`, `TEAM_LEADER`, `OPS_MANAGER`, and `COMPLIANCE` according to the access model."

Presenter line:

> "The system has to act on vulnerability without overexposing sensitive vulnerability data."

---

## 2. Communication suppression

"The magenta box is the first hard guard: `CommunicationSuppressionService.activateSuppression()`."

Explain:

"For relevant vulnerability categories, the system activates `VULNERABILITY_POLICY` suppression. Debt collection communications may be held, blocked, or sent for review depending on the rules. The action also writes an audit event."

Important distinction:

"This is not the same as breathing space statutory suppression. Breathing space is a legal moratorium. Vulnerability policy suppression is an internal protection path, but it still has to be enforced consistently."

Key phrase:

> "Once the vulnerability protocol is active, communication routes must pass through suppression checks before anything goes out."

---

## 3. I&E assessment

"The next blue box is I&E assessment: income and expenditure."

Explain:

"For vulnerable customers, the system must understand affordability before imposing or continuing repayment pressure. Tier 2 supplies rules for what I&E data is needed, what bands apply, and what thresholds matter."

Important guardrail:

> "The obligation to do I&E for vulnerable customers is not optional configuration."

"The form fields and thresholds can be governed, but the system must not allow a repayment plan or enforcement action to proceed for a vulnerable customer without the required affordability evidence."

---

## 4. Affordability calculation

"The fourth box is the affordability calculation. This uses the I&E result plus the policy floor."

Explain:

"The calculation can be rule-driven, but there is a hard lower bound. We cannot configure a rule that creates an unaffordable arrangement just because it improves recovery."

Presenter line:

> "Rules can shape affordability decisions; they cannot remove the affordability floor."

---

## 5. Repayment arrangement created

"The green repayment box shows the outcome when an arrangement is still appropriate."

Explain:

"The Tier 3 process routes the customer into the right treatment path: for example a vulnerable-on-benefit path, vulnerable-off-benefit path, specialist queue, forbearance review, or monitoring variant."

Important point:

"An arrangement can still be created for a vulnerable customer, but it must be created through the protected path, using the affordability result and any relevant forbearance rules."

---

## 6. Ongoing monitoring

"The final blue box is ongoing monitoring."

Explain:

"Once an arrangement exists, monitoring does not become ordinary collections again. Breach logic, review timing, and monitoring suspension rules still need to respect current account and vulnerability state."

Examples:

- arrangement monitoring may be suspended or adjusted
- breach action may route to specialist review
- vulnerability review tasks may be scheduled
- communications remain subject to suppression checks

Key phrase:

> "The protection is not a one-time gate. It continues through monitoring."

---

## Champion/challenger note

"The top-right note connects this to the strategy experimentation model."

Explain:

"Vulnerable customers are always assigned to `CHAMPION` under the current approved champion/challenger policy. If DWP ever wants vulnerable customers included in challenger treatment, that is not a per-experiment toggle. It is a Class A policy change requiring DWP sign-off."

Presenter line:

> "Experimentation cannot override vulnerability protection."

---

## Open DPO question

"The red box on the right is an open question, and it is important to say plainly."

Explain:

"DDE-OQ-02 is DPO confirmation of the Article 9 lawful basis for processing health-related vulnerability data. Until DWP confirms that basis, production handling of health-condition vulnerability data remains blocked or constrained."

Useful phrase:

> "The design is ready to protect and restrict the data, but DWP must confirm the legal basis before health-related vulnerability data is processed in production."

---

## Configuration boundary

"The large green panel at the bottom is the most important summary."

Walk it:

- Tier 1 is configurable with DWP policy approval: taxonomy, severity, evidence source.
- Tier 2 is configurable with Class A approval: action matrix, allowance bands, thresholds, segmentation and routing.
- Tier 3 is configurable for process variants: treatment paths, specialist queues, review flows.
- Hardcoded regulatory guards remain non-configurable: suppression activation, mandatory I&E gate, affordability floor, restricted access, and audit evidence.

Key message:

> "The business can configure how vulnerability is handled, but not whether vulnerability is handled."

---

## What this diagram is trying to prove

"There are four design claims here."

1. "Vulnerability definitions are governed policy configuration, not casual dropdown values."
2. "Applying a flag triggers real system behaviour: suppression, I&E, affordability, routing, and monitoring."
3. "Sensitive vulnerability data is restricted and audited."
4. "The legal protections are hard guards, not DMN preferences."

Close this section with:

> "This is how DCMS separates flexible vulnerability policy from mandatory customer protection."

---

## Likely questions

**"Is vulnerability handling fully configurable?"**
No. The taxonomy, action rules, thresholds, and treatment routes are configurable under governance, but suppression enforcement, mandatory I&E, affordability floor, restricted access, and audit evidence are hard guards.

**"Who can define a new vulnerability type?"**
DWP policy owners must approve vulnerability taxonomy changes. Operational users may maintain configured records only within that governed approval path.

**"What does a standard agent see?"**
A standard `AGENT` sees an indicator and referral instruction, not full vulnerability details. Full detail is restricted to specialist and governance roles.

**"Does every vulnerability automatically block all contact?"**
No. The applicable suppression behaviour depends on the governed vulnerability type and Tier 2 rules. But any contact route must pass through suppression and audit checks.

**"Why is I&E mandatory?"**
Because vulnerable-customer treatment requires an affordability assessment before repayment pressure or enforcement action. The fields and thresholds can be configured, but the gate itself cannot be disabled.

**"How does this affect champion/challenger?"**
Vulnerable customers are forced to champion treatment under the current approved policy. Any change to that eligibility rule is a Class A policy change requiring DWP sign-off.

**"What is the open DPO issue?"**
DWP must confirm the Article 9 lawful basis for processing health-related vulnerability data. Until confirmed, production processing of health-condition vulnerability notes or categories remains constrained.

---

## Close-out message

"Vulnerability handling is a good example of the DCMS design philosophy: policy teams can define the categories, rules, and treatment paths, but the system still enforces the mandatory protections that keep the customer safe and the audit trail defensible."

---

*Sources: diag-09-vulnerability-handling.drawio, RULING-002, RULING-003, RULING-007, ADR-008, ADR-009, vulnerability-domain-pack.md*
