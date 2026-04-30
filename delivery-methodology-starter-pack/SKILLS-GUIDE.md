# Skills Guide

## Purpose
This document defines how to organise, use, and extend the project skills library. It captures lessons from real agentic AI delivery about which skills provide genuine value, which should be embedded in role workflows rather than invoked standalone, and which skills are commonly missing from starter libraries.

Skills encode domain knowledge and governance requirements for specific recurring tasks. They make compliance the path of least resistance — an agent using a skill automatically applies the right constraints without consulting standards documents.

---

## Skills Library Principles

1. **A skill must encode project-specific governance, domain knowledge, or recurring judgment** — not just structure a prompt. If a skill is generic enough to work for any software project unchanged, it is a utility, not a skill.
2. **Skills invoked as standalone agent calls should produce a discrete, linkable artifact** (a document, a plan, a review). Skills that are always sub-steps of another role's workflow should be embedded, not standalone.
3. **Skills should be reviewed and pruned** — a skill that is consistently bypassed in practice is not providing value. Make it mandatory, merge it, or remove it.

---

## Core Skills — Keep and Invoke Regularly

These skills encode the highest-value recurring judgment and should be used on every relevant task.

| Skill | When to Use |
|---|---|
| `issue-to-build-plan` | First step for any non-trivial feature issue |
| `write-acceptance-criteria` | Before any implementation; ensures edge and negative cases are captured |
| `release-readiness-gate` | Before any production promotion |
| `release-evidence-pack-builder` | Assembles the evidence pack for a release |
| `review-pr` | Every PR before merge |
| `migration-safety-reviewer` | Every schema migration PR |
| `traceability-and-evidence-enforcer` | Every feature PR — not just at release |
| `change-classification-assistant` | Every PR to set the correct class and evidence requirements |
| `ci-failure-root-cause-fixer` | Before manually debugging a CI failure |
| `update-docs-for-change` | Any PR with `md update required` or higher classification |
| `verification-loop` | Before opening any PR — run locally before pushing |
| `tdd-workflow` | Any feature or bug fix — enforces RED→GREEN→REFACTOR with evidence checkpoints |
| `santa-method` | Class A changes, release evidence sign-off, security-sensitive merges |
| `strategic-compact` | Reference when deciding whether to `/compact` mid-session |
| `gateguard` | Reference for customising the GateGuard hook gate prompts |

For rule-driven or policy-driven projects, also keep:

| Skill | When to Use |
|---|---|
| `policy-bundle-change-reviewer` | Any change to effective-dated rules or policy bundles |
| `date-effective-policy-integrator` | Implementing or reviewing date-effective policy selection |
| `deterministic-implementation-builder` | Implementing logic where identical inputs must produce identical outputs |
| `immutable-filings-and-amendments-guard` | Any change touching append-only legal records |

---

## Skills to Embed in Role Workflows (not standalone invocations)

These skills do genuine work but are most effective as named steps within a role's workflow, not as independent agent calls. Routing them as standalone invocations creates unnecessary handoff questions.

| Skill | Embed in... | Why |
|---|---|---|
| `generate-domain-tests` | Test Builder workflow | Always follows test planning; separating it creates a routing question with no good answer |
| `seed-data-builder` | Test Builder workflow | Seed data is created as part of test implementation, not independently |
| `test-plan` | Issue-to-build-plan or Test Builder pre-step | Test planning is a step within build planning for most features, not a separate invocation |
| `defect-triage-root-cause-classifier` | Code Reviewer output template | Make it a mandatory field in every bug fix PR review: root cause category, whether a test should have caught it, whether a follow-up issue is needed |

**Implementation guidance:** Update the `.claude/agents/test-builder.md` and `.claude/agents/code-reviewer.md` role files to include these as named sub-steps, not as optional skill invocations.

---

## Skills to Restructure

### Merge: `standards-governance-reviewer` + `documentation-authority-resolver`

Both address "which source wins when sources conflict." In practice they are always invoked together — a conflict requires both the authority hierarchy and the governance resolution procedure. Merge into a single `governance-conflict-resolver` skill.

### Relocate: `docx-template-from-md`

This converts Markdown to Word document format. It encodes no project-specific governance or domain knowledge — it is a formatting utility. Move it out of the skills library into a tools or scripts section. Its presence in the skills index alongside `policy-bundle-change-reviewer` dilutes the signal about what the skills library is for.

---

## Skills Currently Missing — Recommended Additions

These skills were identified from project retrospective analysis as covering high-cost recurring failure modes. Create them as project skills for any project where the failure mode is relevant.

### `i18n-completeness-checker`
**For:** Any project with user-facing text in multiple languages.
**Problem it solves:** Hardcoded strings and missing translations accumulate as iterative fix commits across weeks. Each is cheap individually; together they represent significant rework.
**What it does:**
- Maps user-facing strings to required locale keys
- Identifies strings without locale key entries
- Validates that all locale files have entries for all keys
- Produces a completeness report and a list of missing keys
**When to invoke:** As a CI gate (fail on missing keys) and as part of the Frontend Builder's pre-merge checklist.
**Trigger condition:** Any PR that introduces or modifies user-facing text.

---

### `multi-context-parity-validator`
**For:** Projects serving more than one context (jurisdiction, tenant type, customer class) with context-specific rules or policies.
**Problem it solves:** Individual context bundles are reviewed in isolation. Nobody checks that two bundles handle the same scenarios consistently where they should, and diverge only where the domain requires.
**What it does:**
- Takes two or more context configurations/policy bundles
- Identifies scenarios that both contexts must handle (common cases)
- Verifies the common cases produce consistent behavior
- Identifies where contexts intentionally diverge and confirms each divergence maps to a documented domain requirement
**When to invoke:** Before any context-specific feature implementation; after any policy bundle update.

---

### `hotfix-incident-scope`
**For:** All projects with production environments and legally or operationally consequential outcomes.
**Problem it solves:** Emergency hotfixes are handled ad hoc. Evidence requirements don't relax under time pressure, but the process often does — patches are merged without the normal governance artifacts, creating a retroactive compliance gap.
**What it does:**
- Given an incident description, defines the minimum safe patch scope
- Lists which evidence requirements still apply under time pressure (traceability, test coverage)
- Lists which requirements can be deferred to a follow-up issue (with mandatory timeline)
- Produces a hotfix PR checklist aligned to the emergency bypass procedure
**When to invoke:** At the start of any emergency bypass procedure. See `EMERGENCY-BYPASS-PROCEDURE.md`.

---

### `frontend-backend-state-parity`
**For:** Projects with event-sourced or state-machine-driven backends and a corresponding frontend UI.
**Problem it solves:** Frontend component states, disabled conditions, and transitions drift from the backend state model. The UI allows actions the backend will reject, or blocks actions the backend permits. This is a common failure mode in event-sourced systems and is not caught by standard contract tests.
**What it does:**
- Takes the backend state machine definition (states, transitions, terminal states)
- Maps each state to the frontend component states that represent it
- Identifies frontend disabled/enabled conditions that are inconsistent with the backend's valid transitions
- Flags transitions the frontend blocks that the backend permits, and vice versa
**When to invoke:** Before any frontend PR that touches workflow screens, filing/submission flows, or status displays. After any backend state machine change.

---

## Adding a New Skill

1. Create the skill file under `.claude/skills/<skill-name>/SKILL.md` (one subdirectory per skill).
2. Add it to `.claude/skills/README.md` with a one-line description, primary agent, and invocation boundary.
3. Add it to the relevant section of this guide.
4. Raise as a PR — team agreement required before anyone uses it.
5. Run the Codex sync: `powershell -File scripts/sync-claude-to-codex.ps1`
6. Validate the sync: `powershell -File scripts/validate-claude-codex-sync.ps1`
7. Commit `AGENTS.md` in the same PR as the skill.

Skills that encode project-specific governance or domain knowledge are first-class project assets. Treat them with the same review rigour as the standards pack.

### The Codex Mirror

`AGENTS.md` at the repo root is a generated mirror of `.claude/agents/` and `.claude/skills/`. It exists so that Codex and other tools that cannot read `.claude/` directly have access to the full agent and skill surface.

**Rules:**
- Never edit `AGENTS.md` manually — it will be overwritten on the next sync
- The pre-commit hook regenerates it automatically if installed (`scripts/install-git-hooks.ps1`)
- If the hook is not installed, run the sync script manually after any change to agents or skills
- A PR that adds or modifies an agent or skill without a corresponding `AGENTS.md` update is incomplete

### Skill file structure

Each skill lives at `.claude/skills/<skill-name>/SKILL.md` with this frontmatter:

```yaml
---
name: skill-name
description: "One sentence describing what the skill does and when to use it."
primary-agent: role-name
invocation-boundary: "One sentence describing the scope boundary — what triggers this skill and where it stops."
---
```

See `docs/project-foundation/SKILL-DEVELOPMENT-GUIDE.md` for the full authoring guide.

---

## Decision and Governance Skills (invoke as needed)

| Skill | When to Use |
|---|---|
| `council` | Class A decisions where SA and DC disagree, or options doc has no clear winner |
| `safety-guard` | Scope a builder agent to a specific directory; prevent destructive operations |
| `context-budget` | After adding any new agent, skill, or MCP server; run monthly |
| `cost-aware-llm-pipeline` | When building applications that call LLM APIs; reference for model routing decisions |
| `agent-introspection-debugging` | When an agent run is failing repeatedly without forward progress |
| `continuous-agent-loop` | When running autonomous loops (CI fix cycles, batch processing) |

---

## Skills Audit Cadence

At the end of each release phase, review the skills library:
1. Which skills were used on every relevant task? → Keep, no change.
2. Which skills were consistently bypassed? → Make mandatory, embed in a role workflow, or remove.
3. Which failure modes recurred that a skill could prevent? → Add to the missing skills list.

A skills library that grows without pruning becomes noise. A skills library that is audited stays signal.
