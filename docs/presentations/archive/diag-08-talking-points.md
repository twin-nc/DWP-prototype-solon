# DIAG-08 — Strategy Simulation Engine: Talking Points

---

## Opening line (when the slide comes up)

> "This is the engine's answer to: *show your working before you change the rules.*"

Pause. Let that land. Then go into the problem.

---

## The problem it solves

Start here — before explaining what it does. The audience needs to feel the problem first.

**Say something like:**

> "The strategy engine makes automated decisions about debt accounts — which treatment path to put someone on, whether to refer to a Debt Collection Agency, how we handle someone in breathing space. Those decisions are driven by rules. And rules need to change over time — policy updates, new guidance, regulatory requirements.
>
> But when a policy analyst says 'increase the DCA referral threshold', you need to be able to answer: *what would have happened differently last month, across all 50,000 active accounts, if this rule had been in place?*
>
> You can't answer that by reading the rule. And you absolutely cannot answer it by deploying the change and watching — because that would affect real people."

**The tension to land:** the only way to know the real-world impact of a rule change is to run it. But running it on live data causes the thing you're trying to measure.

---

## What a simulation does

Walk through the three inputs, then the output — left to right across the slide.

### Inputs (left panel)

> "You give the simulation three things: the current rule and the proposed new rule — the before and after decision tables. A cohort — a defined set of accounts at a frozen point in time. And a policy snapshot that specifically captures how protected groups are handled."

**Key point to emphasise:** the data is frozen. The simulation runs against a snapshot, not the live database. That's what makes it safe to run repeatedly.

### What it does with them (centre panel)

> "It runs both versions of the rule against every account in the cohort and compares the outcomes side by side. For each account you get: what the baseline rule decided, what the proposed rule would have decided, and the delta — whether anything changed."

**The result_hash** — mention this if the audience is governance or compliance minded:

> "The run record is immutable. It gets a hash — a tamper-proof fingerprint of exactly what was tested, against exactly which cohort. That hash gets attached to the approval record. So when someone approves a rule change, the approval is tied to specific, verifiable evidence."

### Outputs (right panel)

Walk through in order:

1. **Cohort summary** — how many accounts, basic breakdown
2. **Decision distribution** — where the two rule versions diverge at population level
3. **Decision deltas** — the specific accounts that flipped
4. **Protected-population impact** — *pause here*

> "This one is highlighted in red because it's the most consequential output. DWP handles some of the UK's most financially vulnerable people. A rule change that looks minor on paper could — at scale — push thousands of people in breathing space or with a vulnerability flag into a more aggressive treatment. This output gives you those counts explicitly, before any change is approved."

5. **Evidence hash** — connects back to the governance gate

---

## The hard read-only guarantee (red warning box)

> "The simulation engine has one absolute constraint: it cannot start a process, send a letter, post a payment, or write anything to an account. Read-only. No side effects. You can run it a hundred times and nothing in the live system changes."

This is worth saying clearly — stakeholders often worry that running a simulation *does* something.

---

## Governance gate (bottom strip)

> "For four types of change, simulation is mandatory — not optional, not a best practice. Champion/challenger setups, anything affecting protected populations, DCA placement eligibility changes, and vulnerability protocol changes. You cannot activate those changes without a completed simulation run attached to the approval."

Then the note:

> "But simulation is evidence — it cannot approve a change on its own. It tells you what *would have* happened. A human still makes the call."

---

## Closing line

> "The point of this isn't bureaucracy. It's that at DWP scale, a rule change isn't a configuration tweak — it's a policy decision that affects hundreds of thousands of accounts. Simulation is how you make that decision with your eyes open."

---

## Likely questions

**"How long does a simulation run take?"**
Not defined yet — depends on cohort size and rule complexity. The design is async (status lifecycle: RUNNING → COMPLETED/FAILED) so it doesn't block the UI. Worth flagging as something to benchmark once we have data volumes.

**"Who can trigger a simulation?"**
Access-controlled via RBAC. Likely policy analysts and ops managers — not frontline agents. Exact role mapping to be confirmed when the user permissions matrix is designed.

**"Can you run a simulation against a future cohort / hypothetical data?"**
No. The cohort definition uses a `data_cutoff_at` timestamp — it's always a historical snapshot of real accounts. Hypothetical/synthetic cohorts are out of scope for this design.

**"Does the simulation engine use the same DMN evaluator as the live system?"**
Yes — that's the point. Same engine, same rules, just run offline against frozen data. If it used a different evaluator the results wouldn't be trustworthy as evidence.

---

*Sources: diag-08-strategy-simulation-engine.drawio, strategy-simulation-engine-design.md, ADR-010*
