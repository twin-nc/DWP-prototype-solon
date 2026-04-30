# Options, Limitations, Decision — Talking Points (Slides 17–22)

These six slides take the audience from the headline decision through the seven-problem analysis, the two rejected options, the chosen option, and finally why the decision is locked. The arc is: **claim → evidence → A rejected → B rejected → C adopted → why we will not reopen this**.

Total speaking time target: 8–10 minutes. Do not rush slide 22 — it is the slide leadership will quote back at us.

---

## Opening framing — before slide 17 (15 seconds)

"The next six slides cover the single most consequential decision on this programme: whether any part of Solon Tax becomes a foundation for the DWP debt system. We looked at three options. We rejected two. I want to walk you through why — because the decision needs to hold under challenge."

---

## Slide 17 — Claude's Identified Options and Recommendations (90 seconds)

**Purpose:** the executive summary. One slide, one verdict.

- "Three options. Two rejected. One confirmed. The headline number — 30–35% reuse for Option A — is a paper number. Once we account for the seven problems behind it, the **real** benefit is 5–10%."
- "Option B looks safer at 20–25% paper reuse, but the net is 10–15% — and that saving is consumed by the integration cost of running two systems side by side."
- "Option C — build our own — is the baseline. No major problems. That's why it's confirmed."
- **The one line that ends the debate:** "Solon's workflow engine cannot safely meet UK debt-pause rules. Every workaround relies on developer discipline rather than the system itself — and under the 2020 Debt Respite regulations, that is criminal-liability exposure."
- "Solon's latest release — v2.3.0 — fixes some of the older issues we flagged. Seven serious problems remain. The decision does not change."

> If asked: "Why are we revisiting this if v2.3.0 came out?" — "Because we owe leadership a current answer. We re-ran the analysis against v2.3.0. The legal blocker is unchanged."

---

## Slide 18 — The 7 Problems with Reusing Solon (2 minutes)

**Purpose:** the evidence pack. This is the slide that has to be defensible line by line.

- "This is the heart of the analysis. Seven problems, three options, side by side. I'll not read every cell — I'll call out the shape."
- **Decision frame at the top:** "5–15% net scope reduction does not justify statutory, integration, or platform-coupling risk. That's the test. Everything below is the working."
- **Problem 1 (legal):** "This is the one that matters most. Our chosen workflow engine pauses and resumes natively. Solon's stops and restarts — which is not the same thing in law."
- **Problems 2–7 in one breath:** "Rule editing, Java version, database tooling, frontend framework, service topology, authorisation model — every one of these is a standard we have already set, and adopting Solon means rewriting it."
- "Look down the Option C column. Every row says 'by design' or 'out of the box' or 'from day one'. That column is what we mean when we say 'no major problems'."

> If pressed on any single row: each cell maps to an architecture decision record. Offer to share the ADR pack as supporting evidence.

---

## Slide 19 — Option A: Reuse the Whole Platform (90 seconds)

**Purpose:** show what "full reuse" actually costs once you add up the consequences.

- "Option A is the version where Solon becomes the foundation. On paper, 30–35% reuse. In practice, six things break."
- "Legal safety, decision rules, programming language — those are the first three. Each one means we either build a converter, accept a weaker safety check, or accept criminal-liability exposure."
- "Then the rest of the stack — database tooling, frontend, architecture, security — every standard we've set gets rewritten. Not adapted. Rewritten."
- **The cost line that lands:** "Operating Solon's many-service design forces the team from 6–7 people to roughly 12–15. Once you add the converters and rebuilds, this option costs **more** than building our own — for less benefit."
- "Schedule impact: weeks of re-evaluation alone. Build start on 4 May slips. The 8 July demo is at risk."
- **Verdict:** "Claude rejects. This option carries legal liability we cannot accept."

---

## Slide 20 — Option B: Reuse Only the Billing Engine (90 seconds)

**Purpose:** kill the "compromise" option. This is the one leadership will be tempted by, because it sounds reasonable.

- "Option B is the compromise. Keep our stack, but plug Solon's billing engine in behind it. On paper, 20–25% reuse."
- "It looks safer than Option A. It isn't — for one reason: the legal safety problem is **only** solved if the programme bans Solon's workflow engine outright. That's not a structural protection, that's a rule we have to enforce ourselves, in code review, forever."
- "Then the structural costs land:
  - Two databases — every financial flow becomes a sync problem.
  - Two security models — every cross-boundary call needs review, vulnerability surface roughly doubles.
  - A permanent translation layer — for the life of the programme.
  - Solon's release schedule starts driving ours."
- **The kill shot:** "Whatever we save by reusing Solon's billing engine is roughly equal to what we spend on the integration. The 10–15% net benefit disappears into the boundary."
- **Verdict:** "Claude rejects unless forced. The savings disappear into integration costs."

> If asked: "Could we just isolate the billing engine cleanly?" — "We modelled that. The cleanest isolation still needs the translation layer, still couples our release schedule, still doubles the security surface. The boundary is the cost."

---

## Slide 21 — Option C: Build Our Own — The Only Obstacle Is Political (90 seconds)

**Purpose:** name the real risk to Option C. It is not technical. It is reputational and political. Acknowledge it openly.

- "Option C has no major technical problems. The obstacles are entirely about how we present it."
- "Six things to manage:
  1. **Stakeholder framing** — leadership reads '30–35% reuse' as a real saving. We have to explain the gap honestly.
  2. **'Not invented here' optics** — declining a Netcompany asset can read as preference. The truth is structural: Solon was built for tax collection, not debt collection.
  3. **Senior commitments** — if anyone has already publicly committed to Solon, this decision relitigates that. We need the paper trail.
  4. **Legal framing** — Problem 1 is criminal-liability exposure under the 2020 Debt Respite rules. That's the line that ends the debate.
  5. **Governance** — recorded decision, evidence pack, signed-off ADR. If we're overruled, we require **written** acceptance of the legal risk from the accountable executive.
  6. **Schedule narrative** — under Solon adoption, build start slips, team grows to 12–15, demo at risk. Frame the trade honestly."
- **Verdict:** "Claude adopts. The only option with built-in legal compliance. The remaining work is convincing leadership — which is what these slides are for."

---

## Slide 22 — Why the Decision Is Locked (90 seconds)

**Purpose:** the close. One slide that anyone who walks in late can read on its own.

- "One slide. Plain language. If anyone challenges this decision after today, this is the slide we point them at."
- **Lead with the legal blocker:** "Solon Tax's process engine can only stop a workflow — it cannot pause and resume. Breathing space needs pause and resume. Contacting a debtor during breathing space is a **criminal offence**. We cannot ship on a platform that makes that mistake possible."
- "Then the rest, briefly:
  - No business-rules engine — every rule change needs a developer.
  - Older Java — we lose compiler safety nets.
  - Different migration tool — our scripts don't carry across.
  - Angular front-end — doesn't fit the GOV.UK Design System we're required to use.
  - Pre-split microservices — we chose one service for a reason.
  - Different access-control stack — we'd run two auth systems side by side."
- "The hybrid keeps most of these and adds a permanent translation layer — for 10–15% net benefit."
- **The lock:** "We'd only reopen this decision if **DWP mandates** a platform, or a future Solon release fixes **all seven** issues. Neither has happened. The decision stands."

---

## Anticipated questions and short answers

| Question | Short answer |
|---|---|
| "Has v2.3.0 been properly evaluated?" | Yes. Re-run against v2.3.0. Three of the older issues are fixed. The seven listed remain. Legal blocker unchanged. |
| "Could a Solon roadmap commitment change this?" | A roadmap commitment is not a structural guarantee. We'd reopen on a **shipped** release that fixes all seven, not a promise. |
| "Is this not-invented-here?" | No. Solon is a tax-collection platform. We are building a debt-collection platform. Different domain, different regulatory regime, different lifecycle. |
| "What if DWP mandates Solon?" | Then we re-plan against the legal exposure and require written executive acceptance of the criminal-liability risk under the 2020 Debt Respite rules. |
| "What's the cost of being wrong about Option C?" | Bounded — we own the stack, we can change components. The cost of being wrong about Option A or B is unbounded — legal exposure plus permanent platform coupling. |

---

## Closing line — after slide 22 (10 seconds)

"That's the decision. Built our own, on our own stack, with the legal protection designed in from day one. The next section walks through what that delivery looks like."
