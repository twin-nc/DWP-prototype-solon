# Ways of Working

## Purpose
This document defines how the team collaborates in GitHub, keeping `main` as the single source of truth (SSOT).

---

## Operating Model

This methodology is designed for a team of humans orchestrating AI agents. The coordination problem is human-to-human — agents do not need to coordinate with each other. Key implications:

- Fresh context windows are the primary independence mechanism for critic and reviewer roles. A new context window with the artifact and evaluation criteria only is a sufficient substitute for a separate human reviewer when team capacity does not allow it.
- Critic and reviewer roles should be invoked by a different human than the one who produced the work, where team capacity allows. Where it does not, a fresh context window with a consciously constructed prompt is the fallback.
- The pre-implementation checkpoint is also an agent context preparation step — not just a document gate. Confirm context is current before invoking a builder, not just that documents were approved at some prior point.
- Agent output quality depends heavily on how the human frames the invocation. Context window scope — what the agent knows at invocation time — governs output quality.

---

## Core Rules
1. `main` is the SSOT. It must always represent the latest trusted project state.
2. No direct pushes to `main`. All changes go through a PR.
3. Every change starts from a GitHub Issue.
4. Every change is delivered through a Pull Request (PR) into `main`.
5. One team member must approve the PR before merge (non-author).
5a. **(ENABLED — Option A) Approval requirements by change class:**
    - Class A (behavior / legal outcome): at least one approver must have domain or requirements knowledge relevant to the change. The DWP Debt Domain Expert must be consulted before merge on any change touching: disclosure obligations, vulnerability handling, insolvency, breathing space, CCA-governed processes, or DWP payment allocation rules. Self-approval is prohibited.
    - Class B (contract change): contract owner must be one of the approvers.
    - Class C/D/E: standard single non-author approval applies.
6. Emergency direct pushes to `main` follow the break-glass procedure in `EMERGENCY-BYPASS-PROCEDURE.md`.

---

## Branch and PR Rules
1. Create branches from `main`.
2. Branch naming: `feat/<issue-number>-short-name` or `fix/<issue-number>-short-name`.
3. Hotfix branches (emergency): `hotfix/<YYYY-MM-DD>-short-name`.
4. PR title must include the issue number.
5. PR description must include:
   - Scope summary
   - Linked issue (`Closes #<issue-number>`)
   - Change class (A / B / C / D / E — see standards)
   - Documentation impact classification
   - Docs updated or `no doc impact` confirmation
   - Tests added/updated
   - Test evidence (CI link or manual description)

---

## Pre-Implementation Checkpoint
Before any Builder starts implementation on a non-trivial feature, the following must exist on the linked issue:
1. Acceptance criteria covering **all** target contexts (not just the primary one).
2. An infrastructure or technology decision record if any new dependency, tool, or protocol is introduced — including license, ecosystem fit, and operational constraint verification.
3. A scope statement: explicitly in or out of the current release, with documented rationale.
4. For multi-language systems: i18n key catalogue entries pre-registered for any new user-facing strings.

See `PRE-IMPLEMENTATION-CHECKPOINT.md` for the full checklist. The Delivery Designer and Business Analyst are responsible for ensuring the checkpoint is complete before handing off to a Builder.

## Required Merge Gate
A PR may merge only when all of the following are true:
1. One non-author team member has approved the PR.
2. The branch is up to date with latest `main`.
3. The full CI test suite passes on the PR branch.
4. New tests added in the PR are included in that same CI run.
5. Immediately before the final push, sync with `origin/main`; if `main` moved, rebase, rerun all tests, then push.
6. Traceability links are updated in this cycle — not deferred to release time.
7. **(ENABLED — Option C) For Class A changes**, Solution Architect sign-off must appear as a PR comment or a linked Decision Log entry before any Builder merges. Class A changes include: any behavior change with legal outcome (disclosure, vulnerability treatment, insolvency handling, breathing space, CCA obligations), any change to payment allocation logic, any change to audit trail fields required by COM06/COM07.

---

## Issue Rules
1. Do not start work without a GitHub Issue.
2. Close issues through merged PRs only (use `Closes #<number>` in PR description).

---

## Agent and Skill Standard
1. Use only the agreed agent and skill set for this project.
2. Do not use custom or additional agents/skills without team agreement.
3. Agreed agents: see `AGENT-OUTLINES.md`.
4. Agreed skills: see `.claude/skills/README.md`.

---

## MCP Server Configuration
MCP servers are defined in `.claude/claude_mcp_config.json` at the repo root. Required environment variables for each server are documented in `docs/project-foundation/CI-SECRET-REGISTER.md`. Do not add or remove MCP servers without team agreement.

---

## GitOps and Remote Deployment

These rules apply to all projects with remote deployment targets. They exist because diverging from an agreed deployment and routing way-of-working makes for time-consuming debugging and costly rebuild/redeploy cycles.

### Core rules

1. DevOps / Release Engineer must be involved from the **design phase** for any feature with remote deployment, infrastructure, or routing impact. Do not defer DevOps involvement to build or release.
2. The remote environment specification (`docs/project-foundation/remote-environment-spec.md`) must be created and current before any remote deployment. It is mandatory reading for agents before any remote debugging session.
3. The `REMOTE-DEPLOYMENT-READINESS-GATE.md` checklist must be completed and signed off before promoting to any remote environment for the first time, or after any change to infrastructure, routing, or GitOps configuration.
4. Before DevOps begins infrastructure work (Key Vault, TLS, production Helm values, GitOps wiring) on any containerised service, the dev team must have completed and signed `templates/DEV-DEVOPS-HANDOFF-CHECKLIST.md`. This checklist is the formal pre-condition for DevOps starting. If it is not signed, DevOps must not proceed — raise a blocker issue instead.

### Containerisation standards (dev team obligations)

All containerised services must satisfy these standards before handoff. DevOps/RE enforces these at the handoff gate — they are not DevOps responsibilities.

| Standard | File | Covers |
|---|---|---|
| STD-PLAT-009 | `standards/platform/containerization.md` | Multi-stage Dockerfile, pinned images, non-root, SIGTERM, PORT env var |
| STD-PLAT-010 | `standards/platform/local-dev-environment.md` | docker-compose, service_healthy, .env.example, README, fail-fast config |
| STD-OPS-003 | `standards/operations/health-endpoints.md` | /health/live and /health/ready semantics and probe config |
| STD-OPS-004 | `standards/operations/structured-logging.md` | JSON stdout, mandatory log fields, correlationId propagation |

Use the templates in `templates/DOCKERFILE-TEMPLATE.md`, `templates/DOCKER-COMPOSE-TEMPLATE.md`, and `templates/HELM-CHART-TEMPLATE/` as starting points.

### Helm chart changes

1. Any change to a Helm chart requires DevOps/RE review before the PR is merged.
2. Helm chart changes must be communicated to all active team members before deployment — not after.
3. Changes to `compose.yml` (local) and Helm charts (remote) that affect the same service must be made together in the same PR. Do not update one without the other.
4. Never deploy a Helm chart change to remote without confirming that the corresponding local `compose.yml` reflects the same config shape (see STD-PLAT-008).
5. Before handoff, run `helm lint ./chart && helm template ./chart` locally. Both must pass with no errors. The Helm chart must conform to the structure in `templates/HELM-CHART-TEMPLATE/`.

### GitOps sync

1. Do not assume a config change has propagated to remote until sync completion is verified. The sync mechanism and verification method are documented in `remote-environment-spec.md §3`.
2. When a manual sync is required, record it in the deployment PR or issue comment before closing.
3. Agents working on remote debugging must confirm the current GitOps sync state before diagnosing config-related issues.

### Routing changes

1. Routing rule changes (ingress, path, auth policy) require DevOps/RE sign-off.
2. Routing changes must be documented in `remote-environment-spec.md §4` before deployment.
3. Any divergence from the agreed routing rules must be resolved before merge — not worked around.

### Log access

Log access is not a nice-to-have. It is a deployment prerequisite.

1. Infrastructure logs and solution logs must be accessible and **tested** before the first remote deployment of any service.
2. Access methods must be documented in `remote-environment-spec.md §5` with copy-pasteable commands.
3. When an agent is asked to debug a remote issue, it must read `remote-environment-spec.md §5` first to locate logs before attempting diagnosis.

---

## Environment Map
Fill this in on day zero and update when environments are added or changed.

| Environment | Branch / Tag | Deploy Trigger | Notes |
|---|---|---|---|
| dev | `main` | Push to main (CD) | Continuous deployment |
| staging | `release/*` or manual | Manual or release tag | Pre-release validation |
| production | `vX.Y.Z` tag | Tagged release with evidence pack | Requires release evidence |

---

## Error Code Standard
1. Use one central error code catalog per project (single SSOT).
2. Error codes are stable and unique — never reuse retired codes.
3. Format: `<PROJECT>-<DOMAIN>-<4 digits>` (example: `PROJ-API-0001`). Set `<PROJECT>` to your project acronym in `CLAUDE.md`.
4. API error responses must include: `code`, `message` (safe for users), `correlationId`.
5. Never expose stack traces or sensitive details in API error messages.
6. Any PR introducing a new error path must add the code to the catalog and assert it in tests.
7. Full error envelope spec: see `standards/integration/error-semantics-and-stability.md`.

---

## Documentation Governance
1. Baseline governance docs are stored in `docs/project-foundation/`. Talk to the team before changing them.
2. Day-to-day documentation policy is in `DOCUMENTATION-POLICY.md`.
3. On project day zero, complete and commit `REPO-SETUP-CHECKLIST.md`.

---

## Documentation Policy
1. Repository Markdown in `docs/` is the working SSOT for project documentation.
2. Word documents are published stakeholder-facing snapshots derived from Markdown.
3. If MD and Word conflict, treat Markdown as authoritative until Word is republished.
4. Every issue, task, and PR must declare documentation impact using one of:
   - `no doc impact` — bug fix, trivial refactor, no behavior change visible to stakeholders
   - `md update required` — new feature, changed API behavior, new config option, new error code
   - `docx republish required` — requirements baseline change, architecture change visible to stakeholders
   - `both required` — combined behavior and stakeholder-visible change
5. A Word document must be republished when a stakeholder needs a downloadable copy for review, approval, or audit, or when the current published Word version would mislead readers.

---

## Definition of Done
Work is done only when:
1. Issue linked and scoped correctly.
2. PR approved by a non-author.
3. Full CI is green on an up-to-date branch.
4. PR merged into `main`.
5. Issue auto-closed by merge.
6. Required Markdown documentation is updated.
7. Required stakeholder-facing Word documents are republished or explicitly deferred with team agreement.
8. `docs/memory.md` updated if a named decision was made or project state shifted.
9. Traceability links updated — at least one test case and one evidence record type declared on the linked issue.
10. Design Critic pass confirmed if the feature involved a Solution Architect decision.

---

## Project Configuration Options

These settings are **not active by default**. The project initiator should review them at project kickoff and enable those that fit the project's risk profile.

### Option A — Strengthened Approval Requirements
*Recommended for projects with legal, financial, or compliance-sensitive outcomes.*

Enable by adding the following to this document under Core Rules:

```
5a. (ENABLED) Approval requirements by change class:
    - Class A (behavior / legal outcome): at least one approver must have domain or
      requirements knowledge relevant to the change. Self-approval is prohibited.
    - Class B (contract change): contract owner must be one of the approvers.
    - Class C/D/E: standard single non-author approval applies.
```

### Option B — PR Size Guidance
*Recommended for teams that have experienced review quality degradation on large PRs.*

Enable by adding the following to Branch and PR Rules:

```
6. (ENABLED) PRs exceeding ~400 changed lines must include a decomposition
   justification in the PR description. Reviewers may request decomposition
   for large PRs without this justification.
```

### Option C — Mandatory Class A Design Gate
*Recommended for projects where behavior changes carry legal or financial risk.*

Enable by adding the following to the merge gate:

```
6. (ENABLED) For Class A changes, Solution Architect sign-off must appear as
   a PR comment or a linked Decision Log entry before any Builder merges.
```

---

## Recommended Practices

These are team-level guidance items, not hard rules. Adopt them if they suit the project.

- **Keep PRs focused.** Smaller PRs get better reviews. If a PR naturally grows beyond ~400 lines, consider whether it can be split by layer (e.g., schema + logic + tests as separate PRs).
- **Commit early to a draft PR.** Opening a draft PR early gives the team visibility into in-progress work and catches direction problems before too much code is written.
- **Reference the issue in every commit message.** This links git history to requirements traceability without extra effort.
- **Use the `ci-failure-root-cause-fixer` skill** before manually debugging a CI failure — it often identifies the root cause faster.
