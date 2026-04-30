# Methodology Starter Pack Leadership Presentation - Outline

**Audience:** Company leadership, delivery leadership, project directors, practice leads  
**Tone:** Strategic, practical, and evidence-led - this is a reusable delivery control system, not a documentation bundle  
**Target length:** ~18 slides / 40-50 min including Q&A  
**Purpose:** Explain what the methodology starter pack contains, why it matters, what we learned from previous projects, and what leadership should decide to standardise adoption.

---

## Section 1 - Executive Frame (Slides 1-3)

### Slide 1 - Why This Exists
- Core message: the starter pack gives new projects a governed, auditable, agent-ready delivery model from day one.
- Problem it solves: each project otherwise reinvents delivery rules, agent roles, standards, evidence expectations, and setup mechanics under pressure.
- Leadership framing: this reduces startup friction, improves delivery consistency, and makes AI-assisted delivery governable.
- Suggested visual: "before / after" comparison of project startup without and with the starter pack.

### Slide 2 - What the Starter Pack Is
- A reusable methodology pack for Claude Code and agent-assisted Netcompany delivery.
- Covers ways of working, agent roles, skills, hooks, standards, templates, setup, documentation policy, deployment readiness, and release evidence.
- Designed to be copied into a new repo and customised, not treated as a central static manual.
- Key phrase: a project operating system for AI-assisted delivery.

### Slide 3 - Leadership Outcomes
- Faster project mobilisation with fewer blank-page decisions.
- Better control over AI agent behaviour through roles, rules, hooks, and review gates.
- Repeatable evidence for audits, release sign-off, and stakeholder assurance.
- Lower delivery risk on regulated, multi-role, and remote-deployed projects.
- Clearer onboarding path for project leads and developers.

---

## Section 2 - What's in the Starter Pack (Slides 4-7)

### Slide 4 - Pack Contents at a Glance
- Core governance: `WAYS-OF-WORKING.md`, `AGENT-RULES.md`, `AGENT-OUTLINES.md`, `DOCUMENTATION-POLICY.md`, `SOUL.md`.
- Setup guides: `getting-started-guide.md`, `INSTALL-MANIFEST.md`, `REPO-SETUP-CHECKLIST.md`, `CLAUDE-MD-TEMPLATE.md`.
- Delivery gates: `PRE-IMPLEMENTATION-CHECKPOINT.md`, `REMOTE-DEPLOYMENT-READINESS-GATE.md`, `EMERGENCY-BYPASS-PROCEDURE.md`.
- Standards pack: governance, development, platform, integration, operations, security, AI, and deviations.
- Templates: memory, feature notes, decisions, runbooks, release evidence, project status, feature flags, CI secrets, Docker, Compose, Helm.
- Suggested visual: package map grouped by "governance", "execution", "automation", and "evidence".

### Slide 5 - Where the Pack Lands in a Repo
- `docs/project-foundation/`: methodology docs, standards, templates.
- repo root `.claude/`: agents, skills, contexts, hooks, settings.
- repo root `.github/`: PR template and Dependabot baseline.
- repo root `CLAUDE.md`: project-specific operating brief read at the start of every Claude Code session.
- Key lesson: installation location matters; `.claude/` and `.github/` do not work correctly if buried in documentation folders.

### Slide 6 - Standards and Governance Backbone
- Standards define the non-negotiables: traceability, change classification, release evidence, contracts, testing, migrations, determinism, local/remote parity, logging, security, and data classification.
- `SOUL.md` provides the five tie-breaker invariants: governed, deterministic, append-only, research-first, evidence-backed.
- Ways of Working defines issue-first delivery, PR gates, branch rules, documentation impact classification, and emergency bypass.
- Leadership message: the pack turns governance into default workflow, not end-of-release paperwork.

### Slide 7 - Templates and Evidence Assets
- Templates make governance repeatable: feature note, decision log, runbook, release evidence pack, status report, CI secret register, feature flag register.
- Release evidence is planned from the start rather than reconstructed at the end.
- Project memory keeps current context concise for agents and humans.
- Suggested callout: "A merged PR without traceability or evidence is incomplete, not merely undocumented."

---

## Section 3 - Agent Operating Model (Slides 8-10)

### Slide 8 - Agents: Specialist Roles, Not Generic Chat
- Agents model delivery roles: Solution Architect, Delivery Designer, Business Analyst, Domain Expert, DB Designer, Backend Builder, Frontend Builder, DevOps / Release Engineer, SRE / Platform Engineer, Integration Specialist, Test Builder, Refactorer, Traceability Steward, Code Reviewer, Design Critic.
- Each role has a defined purpose, mode, boundaries, and handoff expectations.
- Review and critic roles are read-only by default and must run in fresh context for independence.
- Leadership message: AI is treated like a controlled delivery workforce with role boundaries, not an unrestricted assistant.

### Slide 9 - How Work Moves Between Agents
- Work starts with a GitHub issue and acceptance criteria.
- Design and analysis roles prepare scope, options, domain rules, and decisions.
- Builder roles implement only after the pre-implementation checkpoint is satisfied.
- Test, review, traceability, and release evidence are part of the same delivery cycle.
- Handoff declarations prevent silent assumptions from moving downstream.
- Suggested visual: issue -> BA / design -> architecture decision -> build -> test -> review -> traceability -> evidence -> merge.

### Slide 10 - Domain Expert Pattern
- Regulated or legally consequential projects should add a project-specific Domain Expert role from day one.
- Domain Experts should work one sprint ahead of builders because domain interpretation is often the slowest, least parallelisable task.
- Example from this project: DWP Debt Domain Expert covers CCA, GDPR, vulnerability, breathing space, insolvency, and payment allocation concerns.
- Leadership decision point: require a domain expert role for regulated, public-sector, financial, health, or legally consequential programmes.

---

## Section 4 - Skills and Hooks (Slides 11-13)

### Slide 11 - Skills: Reusable Judgment, Not Prompt Snippets
- Skills encode recurring governance, domain, or delivery judgment.
- Core examples: `issue-to-build-plan`, `write-acceptance-criteria`, `change-classification-assistant`, `review-pr`, `migration-safety-reviewer`, `release-readiness-gate`, `release-evidence-pack-builder`, `traceability-and-evidence-enforcer`, `verification-loop`, `tdd-workflow`, `santa-method`.
- Rule-driven projects add policy and determinism skills such as `date-effective-policy-integrator`, `policy-bundle-change-reviewer`, and `deterministic-implementation-builder`.
- Best practice: skills must produce value and be audited; a growing skill library without pruning becomes noise.

### Slide 12 - Hooks: Automated Guardrails Around Agent Actions
- Hook profiles: `minimal`, `standard`, `strict`.
- Key hooks:
  - `block-no-verify`: prevents bypassing git hooks.
  - `config-protection`: protects build and tooling config from casual edits.
  - `governance-capture`: records governance-sensitive actions.
  - `quality-gate`: runs formatter/linter checks after edits.
  - `mcp-health-check`: prevents relying on unavailable MCP tools.
  - `session-end`, `pre-compact`, `suggest-compact`, `cost-tracker`: preserve continuity and manage context/cost.
  - `gateguard-fact-force`: strict-mode fact declaration before high-consequence actions.
- Leadership message: hooks make the safe path automatic and the risky path visible.

### Slide 13 - Codex Mirror and Multi-Tool Compatibility
- `AGENTS.md` is generated from `.claude/agents/` and `.claude/skills/` so tools that cannot read `.claude/` still see the agent and skill surface.
- Pre-commit automation regenerates the mirror when installed.
- Manual edits to `AGENTS.md` are prohibited because they will be overwritten.
- Leadership message: the methodology is tool-aware; Claude Code is primary, but the operating model can be mirrored for other agent tools.

---

## Section 5 - Lessons Learned From Previous Projects (Slides 14-16)

### Slide 14 - Lesson 1: Governance Must Be in the Workflow
- Retrospective pattern: teams can agree governance principles, but under delivery pressure they skip evidence, traceability, and documentation unless the workflow makes them unavoidable.
- Pack response: issue-first work, PR gates, documentation impact classification, trace map updates in the same cycle, release evidence templates, and hook-based guardrails.
- Leadership takeaway: governance cannot depend on memory or goodwill; it has to be built into the path to merge.

### Slide 15 - Lesson 2: Agent Boundaries Need to Be Explicit
- Retrospective pattern: generic agents blur architecture, domain, build, review, and approval responsibilities.
- Pack response: role outlines, mandatory handoff declarations, fresh-context critic/reviewer independence, read-only reviewer defaults, and escalation rules when agents disagree.
- Leadership takeaway: AI delivery scales only when authority and review boundaries are as clear as they are for human teams.

### Slide 16 - Lesson 3: Context, Setup, and Environment Drift Are Delivery Risks
- Retrospective pattern: agents lose time when repo setup, remote environments, logs, secrets, feature flags, or MCP servers are undocumented or inconsistent.
- Pack response: install manifest, setup checklist, `CLAUDE.md`, project memory, remote deployment readiness gate, local/remote parity standard, CI secret register, feature flag register, MCP configuration rules.
- Leadership takeaway: "day zero" discipline prevents weeks of avoidable rework later.

---

## Section 6 - Best Practices for Adoption (Slides 17-18)

### Slide 17 - Adoption Playbook
- Start with the install manifest; place files in the correct repo locations.
- Fill in `CLAUDE.md` before any agent work starts.
- Customise the agent set and domain expert role for the project.
- Mark irrelevant skills and standards as N/A at kickoff instead of letting them create noise.
- Install hooks and branch protections before first delivery work.
- Complete the repo setup checklist and create project memory on day zero.
- Run a skills audit at the end of each release phase.

### Slide 18 - Leadership Decisions and Next Steps
- Decide which project types must adopt the starter pack by default.
- Decide the minimum required hook profile for normal delivery and high-risk work.
- Decide whether regulated projects must define a Domain Expert role before build starts.
- Decide ownership for maintaining the central starter pack and approving new skills, standards, and templates.
- Decide how adoption will be measured: setup completeness, PR evidence quality, traceability coverage, release evidence completeness, and reduction in avoidable rework.
- Close message: standardise the operating model, customise the domain layer, and keep the pack alive through retrospectives.

---

## Appendix - Optional Deep Dives

### Appendix A - Full Starter Pack File Inventory
- Walkthrough of governance docs, standards, templates, `.claude/`, and `.github/`.

### Appendix B - Agent Role Catalogue
- One slide listing each agent role, what it owns, and when to invoke it.

### Appendix C - Skills Catalogue
- Core skills, embedded workflow skills, policy/rule-driven skills, and skills recommended from retrospectives.

### Appendix D - Hook Profiles
- Minimal vs standard vs strict profile comparison, including when each should be used.

### Appendix E - Release Evidence Example
- Example of how a feature traces from issue -> acceptance criteria -> implementation -> tests -> evidence -> release pack.

### Appendix F - Adoption Checklist
- Day-zero checklist suitable for project kickoff governance.
