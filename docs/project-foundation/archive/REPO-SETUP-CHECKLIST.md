# Repository Setup Checklist

Complete this checklist on project day zero. Commit the completed checklist to `main` as the permanent record that setup was done correctly.

**Project:** `{{PROJECT_NAME}}`
**Completed by:** `{{NAME}}`
**Date:** `{{YYYY-MM-DD}}`

---

## 1. GitHub Repository

- [ ] Repository created under the correct GitHub organisation
- [ ] Repository visibility set correctly (private / internal / public)
- [ ] Default branch set to `main`
- [ ] Branch protection rule configured for `main`:
  - [ ] Require pull request before merging ✅
  - [ ] Require status checks to pass ✅
  - [ ] Require branches to be up to date ✅
  - [ ] Require at least 1 approving review ✅
  - [ ] Dismiss stale approvals when new commits pushed ✅
  - [ ] Restrict who can push to `main` (repo admins only) ✅
  - [ ] Allow administrators to bypass (break-glass enabled) ✅
  - [ ] Allow force pushes ❌ OFF
  - [ ] Allow deletions ❌ OFF
- [ ] Issue labels created: `feat`, `fix`, `hotfix`, `emergency-bypass`, `doc`, `decision`, `blocked`, `chore`
- [ ] PR template created at `.github/pull_request_template.md`
- [ ] CODEOWNERS file created at `.github/CODEOWNERS` (if applicable)

---

## 2. GitHub Actions Secrets

List all secrets required by the CI/CD pipeline. For each:

| Secret Name | Purpose | Source | Owner | Rotation Policy | Added |
|---|---|---|---|---|---|
| `REGISTRY_USERNAME` | Container registry login | `{{WHERE}}` | `{{NAME}}` | Annual | [ ] |
| `REGISTRY_PASSWORD` | Container registry password | `{{WHERE}}` | `{{NAME}}` | Annual | [ ] |
| | | | | | [ ] |

- [ ] All secrets in the table above have been added to GitHub Actions (Settings → Secrets → Actions)
- [ ] CI secret register committed to `docs/project-foundation/CI-SECRET-REGISTER.md`
- [ ] Secret rotation owner designated and documented in the register

---

## 3. MCP Server Configuration

- [ ] `.claude/claude_mcp_config.json` committed to the repo
- [ ] Required environment variables documented (see WAYS-OF-WORKING.md §MCP Server Configuration)
- [ ] Each developer has set required env vars in their local shell profile
- [ ] MCP connection tested by each developer: run `claude` in the repo, verify MCP tools available

---

## 4. Local Developer Setup (each team member)

- [ ] Claude Code installed (`npm install -g @anthropic-ai/claude-code`)
- [ ] `GITHUB_PERSONAL_ACCESS_TOKEN` set in shell profile
- [ ] `BRAVE_API_KEY` set in shell profile (if Brave Search MCP is used)
- [ ] Any project-specific tools installed (see tech stack in `CLAUDE.md`)
- [ ] Docker Desktop running and tested
- [ ] kubectl configured and namespace access confirmed (if remote environments used)
- [ ] VPN access confirmed (if remote environments require it)
- [ ] First test PR completed: trivial doc edit through branch → CI → merge flow

---

## 5. Project Foundation Documents

- [ ] `CLAUDE.md` created at repo root from `CLAUDE-MD-TEMPLATE.md` — all placeholders filled in
- [ ] `docs/memory.md` created with initial project context
- [ ] `docs/project-foundation/SOLUTION-REQUIREMENTS.md` exists (even a v0.1 stub)
- [ ] `docs/project-foundation/master-solution-design.md` exists (even a stub)
- [ ] `docs/project-foundation/FEATURE-FLAG-REGISTER.md` created (from template, initially empty)
- [ ] `docs/project-foundation/CI-SECRET-REGISTER.md` created and populated

---

## 6. Remote Environment (complete before first remote deployment)

- [ ] DevOps / Release Engineer provisioned and involved in design phase
- [ ] Deployment pipeline designed and agreed before any Builder starts on features with remote deployment impact
- [ ] `docs/project-foundation/remote-environment-spec.md` created from `templates/REMOTE-ENVIRONMENT-SPEC-TEMPLATE.md`
- [ ] Remote environment spec covers: topology, GitOps sync, routing, log access, config divergences
- [ ] **Infrastructure log access tested** (not just provisioned) by at least one team member
- [ ] **Solution log access tested** by at least one team member
- [ ] Log access method documented with copy-pasteable commands in `remote-environment-spec.md §5`
- [ ] Local/remote parity statement documented in `remote-environment-spec.md §6`
- [ ] `REMOTE-DEPLOYMENT-READINESS-GATE.md` checklist completed and signed off before first remote deployment
- [ ] GitOps sync mechanism documented and understood by all active team members
- [ ] Helm chart change communication protocol agreed and added to `WAYS-OF-WORKING.md §GitOps`
- [ ] For containerised services: dev team has completed `templates/DEV-DEVOPS-HANDOFF-CHECKLIST.md` before DevOps begins infrastructure, Key Vault, or TLS work
- [ ] Containerization standard reviewed by dev lead: `standards/platform/containerization.md` (STD-PLAT-009)
- [ ] Local dev environment standard reviewed by dev lead: `standards/platform/local-dev-environment.md` (STD-PLAT-010)
- [ ] Health endpoint standard reviewed by dev lead: `standards/operations/health-endpoints.md` (STD-OPS-003)
- [ ] Structured logging standard reviewed by dev lead: `standards/operations/structured-logging.md` (STD-OPS-004)

---

## 7. CI/CD Pipeline

- [ ] GitHub Actions workflow file(s) committed
- [ ] CI runs successfully on a test PR (unit tests, build, etc.)
- [ ] CD pipeline configured (if applicable) and deployment to dev confirmed
- [ ] Required CI status checks registered in branch protection rule
- [ ] `docker build -t <registry>/<project>:<git-sha> .` succeeds locally before CI integration (STD-PLAT-009)
- [ ] `docker compose up` starts the full local stack before CI integration (STD-PLAT-010)
- [ ] `GET /health/live` returns 200 and `GET /health/ready` returns 200/503 correctly before CI integration (STD-OPS-003)
- [ ] All logs emitted as JSON to stdout with the four mandatory fields before CI integration (STD-OPS-004)

---

## 8. Agent Role File Completeness

Before any role is used on a real PR, verify each role file satisfies all of the following:

- [ ] All mandatory sections present with no empty bullet points
- [ ] `Not In Scope` names at least 2 specific forbidden **actions** (not just domains)
- [ ] `Output Contract` items are named artifacts with a one-line completeness criterion each — not just response headings
- [ ] At least one `Escalate When` trigger is a low-threshold early-warning condition, not a crisis description
- [ ] Role file embeds the Ways of Working rules relevant to it, rather than referencing them externally
- [ ] `Handoff Declaration` section present in all producer role files

Dry-run protocol for each role before production use:
1. Create a GitHub issue titled `[DRY RUN] Onboard <role-name> — <short task description>`. Label: `dry-run`.
2. Select a task that references at least 2 real file paths and has a checkable output a downstream role could consume.
3. Invoke the role with its Required Inputs list.
4. Check each `Output Contract` item: present or absent.
5. Check each `Guardrail`: did the role stay within its `Not In Scope` boundaries?
6. If all outputs present and no guardrails violated: close the issue with label `dry-run-passed`.
7. If any output missing or guardrail violated: tighten the failing section and repeat.

For builder roles (backend-builder, frontend-builder, test-builder): the dry-run must produce real code — a stub implementation of a real but non-critical function. Prose output alone is insufficient.

---

## 9. Hook Configuration

- [ ] `.claude/hooks/` directory committed with all hook scripts
- [ ] `.claude/hooks.json` committed with full hook registry
- [ ] `.claude/settings.json` committed with token optimization settings
- [ ] Each developer has added to their shell profile:
  - [ ] `ECC_HOOK_PROFILE=standard`
  - [ ] `ECC_MCP_HEALTH_FAIL_OPEN=1` (local dev only — NOT in CI)
- [ ] `ECC_GOVERNANCE_CAPTURE=1` added to release branch CI environment
- [ ] Hook profile tiers agreed and documented:
  - `minimal` — spikes and prototyping
  - `standard` — normal sprint delivery (default)
  - `strict` — release branches and Class A changes
- [ ] Each developer has restarted Claude Code after hook setup (hook edits require session restart to take effect)
- [ ] Cost tracking confirmed: run a test session and verify `~/.claude/metrics/costs.jsonl` is being written

MCP tool count discipline:
- [ ] Enabled MCP servers: under 10
- [ ] Total tools across all servers: under 80

---

## 10. Codex Mirror

- [ ] Pre-commit hook installed: `powershell -File scripts/install-git-hooks.ps1`
- [ ] `AGENTS.md` generated and committed: `powershell -File scripts/sync-claude-to-codex.ps1`
- [ ] Sync validated: `powershell -File scripts/validate-claude-codex-sync.ps1` runs clean
- [ ] Each developer has confirmed the pre-commit hook fires on a test commit
- [ ] Team agreement recorded: `AGENTS.md` is never edited manually

---

## 11. First Commit to Main

- [ ] This completed checklist committed to `docs/project-foundation/REPO-SETUP-CHECKLIST.md`
- [ ] Commit message: `chore: complete day-zero repo setup — see REPO-SETUP-CHECKLIST.md`

---

## Sign-off

Delivery Lead sign-off that day-zero setup is complete and the team is ready to begin sprint 1:

**Name:** `{{NAME}}`
**Date:** `{{YYYY-MM-DD}}`
**Notes:** `{{any exceptions or deferred items with justification}}`
