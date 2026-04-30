# Workshop Brief — DWP Debt Collection Layer

**Date:** 2026-04-29
**Format:** Two teams, same task, 75 minutes, compare at the end

---

## Background

Netcompany is building a **Business Control & Experience Layer** on top of Solon Tax — a commercial debt collection platform already running in a dedicated environment the team can deploy to.

Solon Tax handles case execution, process orchestration, payments, and data of record. It does not provide the business-facing experience DWP needs: guided agent workspaces, real-time operational oversight, strategy configuration without IT involvement, vulnerability enforcement at point of action, or I&E-driven affordability. That is what the layer builds.

The architecture has been designed and documented. Six demonstration flows define what needs to be built. Your task is to plan how to build it — and to agree how your team of 7 will work with Agentic AI to do it.

---

## Suggested time split

| | |
|---|---|
| 0–10 min | Read this brief; skim the gap analysis short doc — it tells you what Solon already provides and what must be built |
| 10–55 min | Produce the two outputs below |
| 55–75 min | Each team presents; comparison and debrief |

---

## What you are producing

**Output 1 — Component map (primary)**
A single table. For each of the six flows: what Solon Tax already handles, and what the layer needs to build. Use the gap analysis as your starting point — do not re-derive what is already documented there. Spend your time on sequencing and risk, not on re-reading APIs.

| Flow | Solon provides | Layer builds | Biggest risk |
|---|---|---|---|
| 1 — Intake | … | … | … |
| 2 — Vulnerability | … | … | … |
| 3 — Breach/third-party collection | … | … | … |
| 4 — Household | … | … | … |
| 5 — Strategy change | … | … | … |
| 6 — Dashboard | … | … | … |

**Output 2 — Delivery milestones and WoW sketch (secondary)**
Five to seven milestones from environment setup to demo-ready, with a note on which component each milestone unlocks. If time allows, add a single paragraph on how the team of 7 uses Agentic AI: what goes in the shared context file, and one explicit human-review rule for this project.

---

## The six flows

| Flow | Core theme |
|---|---|
| 1 — Intake to first contact | Automated overnight intake; agents arrive to a pre-populated, pre-prioritised worklist |
| 2 — Vulnerability to resolution | Vulnerability disclosed; protection enforced instantly; I&E capture; affordable arrangement |
| 3 — Breach to third-party collection placement | Missed payment detected; re-engagement sequenced; third-party collection placement when internal recovery exhausted |
| 4 — Complex household | Two individuals, three debts, active breathing space restriction; compliance enforced at runtime |
| 5 — Strategy change without IT | Business user adds SMS step, runs simulation, configures champion/challenger, promotes better strategy |
| 6 — Executive dashboard | Real-time operational view; drill-down; bulk reassignment; C/C results; governance export |

---

## The architecture in one paragraph

Three role-based workspaces (Case Worker, Operations, Configuration) inside a shared product shell with a single Keycloak session. A BFF assembles workspace views from Solon APIs and layer Core Services. The layer owns four databases; all Solon data is read via REST API only. The Solon Kafka stream is consumed observe-only for KPI aggregation. Full detail in `configuration-layer-architecture v2.drawio` and `three-workspace-model.md` — but the gap analysis is the most useful starting point for planning.

---

## The environment

A dedicated Solon Tax instance is already running and teams can deploy to it. The gap analysis documents what Solon provides out of the box, what requires configuration or layer build on top of Solon, and what are genuine gaps the layer must own entirely.

---

## Materials — read in this order

| Priority | Document | What to take from it |
|---|---|---|
| First | `dwp-demo-gap-analysis-short.md` | What Solon gives you; what you build; three decisions that must be made |
| Second | `demo-flows-workshop-short.md` | Key steps and moments of truth per flow |
| Reference | `three-workspace-model.md` | Which workspace owns which flow |
| Reference | `dwp-demo-gap-analysis.md` | Effort estimates and risk detail |
| Reference | `api-usage-guide.md` + `api-*.md` | Solon API detail if needed |
| Reference | `configuration-layer-architecture v2.drawio` | Full layer architecture |
| Reference | `architecture-decision-record.md` | Why key decisions were made |

---

## End-of-session comparison

Both teams present in sequence — approximately 8 minutes each. The comparison focuses on: where plans diverge on critical path, and where WoW models make different calls on AI autonomy vs human review. There is no single correct answer. The comparison is the output.
