# Getting Started: NC Agentic AI Project

A practical guide for project leads setting up a new Netcompany project using Claude Code and the NC Agentic AI delivery methodology.

---

## What Is NC Agentic AI Delivery?

This methodology uses Claude Code (Anthropic's AI CLI) with a structured set of agent roles, skills, and governance standards to run a software delivery project. Instead of one developer doing everything, work is delegated to specialist agents — Solution Architect, Backend Builder, Test Designer, etc. — each operating under defined rules, handoff protocols, and output standards.

The methodology is:
- **Governed** — all decisions are traceable to requirements and standards
- **Auditable** — evidence artifacts are append-only and linked to releases
- **Deterministic** — identical inputs produce identical outputs
- **Scalable** — add jurisdictions, domains, or team members without forking services

---

## Prerequisites

### Tools Required
| Tool | Version | Purpose |
|---|---|---|
| Claude Code | Latest | Primary AI CLI and agent runtime |
| Git | 2.x+ | Version control |
| GitHub CLI (`gh`) | Latest | PR, issue, and CI management |
| Docker Desktop | Latest | Local container runtime |
| kubectl | Latest | Kubernetes access (if remote environments are used) |

### Accounts Required
| Account | Purpose |
|---|---|
| GitHub account with repo access | Source control and CI/CD |
| Anthropic Claude account (Pro or Team) | Claude Code access |
| Brave Search API key | MCP web search capability |
| Container registry access | Pushing/pulling images |

### Claude Code Setup
```bash
# Install Claude Code
npm install -g @anthropic-ai/claude-code

# Verify
claude --version

# Set required environment variables in your shell profile (~/.bashrc or ~/.zshrc)
export GITHUB_PERSONAL_ACCESS_TOKEN=<your PAT>
export BRAVE_API_KEY=<your Brave API key>
```

---

## Day Zero — Before Anyone Writes Code

Work through this in order. Do not skip steps.

### Step 1: Create the GitHub repository

```bash
gh repo create <org>/<repo-name> --private
cd <repo-name>
git init
git remote add origin https://github.com/<org>/<repo-name>.git
```

### Step 2: Install the starter pack

Assets in this pack land in **four distinct locations** — not one. `INSTALL-MANIFEST.md` is the authoritative mapping. The table below is a summary only; use the manifest to avoid mis-installing.

| Target | What goes there |
|---|---|
| `docs/project-foundation/` | All pack files and directories **except** `.claude/`, `.github/`, and `README.md` |
| repo root `.claude/` | `.claude/agents/` and `.claude/skills/` |
| repo root `.github/` | `.github/pull_request_template.md` and `.github/dependabot.yml` |
| repo root `CLAUDE.md` | `CLAUDE-MD-TEMPLATE.md` filled in — place as `CLAUDE.md` at repo root |

**Do not** copy the whole pack into `docs/project-foundation/`. The `.claude/` and `.github/` assets will not work from inside `docs/`, and `README.md` must not be copied — the new project has its own. See `INSTALL-MANIFEST.md` for the full file list and reference shell commands.

### Step 3: Fill in the CLAUDE.md template

Copy `CLAUDE-MD-TEMPLATE.md` to `CLAUDE.md` at the **repo root**. Fill in every `{{PROJECT_*}}` placeholder. This is the first file Claude Code reads in every session — it must be complete before any agent work starts.

Key things to fill in:
- Project name, description, and purpose
- Target environments and jurisdictions (if applicable)
- Current requirements baseline version
- Tech stack decisions (if already made)
- Agent role set agreed for this project

### Step 4: Configure branch protection

In GitHub: Settings → Branches → Add rule for `main`:

```
✅ Require a pull request before merging
✅ Require status checks to pass before merging
✅ Require branches to be up to date before merging
✅ Require at least 1 approving review
✅ Dismiss stale pull request approvals when new commits are pushed
✅ Restrict who can push to matching branches (repo admins only)
✅ Allow administrators to bypass required pull requests  ← the emergency break-glass switch
☐ Allow force pushes  ← LEAVE OFF
☐ Allow deletions  ← LEAVE OFF
```

> The "allow administrators to bypass" setting is what enables the emergency direct-push path. See [EMERGENCY-BYPASS-PROCEDURE.md](EMERGENCY-BYPASS-PROCEDURE.md). It is not a license to bypass routinely.

### Step 5: Register CI/CD secrets

Before your first CI run, every secret the pipeline needs must exist in GitHub Actions.

1. Open `templates/CI-SECRET-REGISTER.md`, copy it to `docs/project-foundation/CI-SECRET-REGISTER.md`, and fill in all required secrets for your project.
2. Add each secret to GitHub: Settings → Secrets and variables → Actions → New repository secret.
3. Designate a secret rotation owner — document their name in the register.

Common secrets needed from day one:
- Container registry credentials
- Any external API credentials used in CI

### Step 6: Create the PR template

Create `.github/pull_request_template.md`:

```markdown
## Summary
<!-- What does this PR do? Which issue does it close? -->
Closes #

## Change class
<!-- Class A (behavior/legal), B (contract), C (refactor), D (ops), E (docs) -->

## Documentation impact
<!-- no doc impact / md update required / docx republish required / both required -->

## Tests added/updated
<!-- What tests cover this change? -->

## Test evidence
<!-- Link to CI run, test output, or describe manual verification -->

## Checklist
- [ ] Issue linked
- [ ] Branch up to date with main
- [ ] CI passing
- [ ] Feature note created (if required)
- [ ] Decision log created (if required)
- [ ] docs/memory.md updated (if state/decision changed)
```

### Step 7: Create token optimization and hook configuration

Create `.claude/settings.json`:

```json
{
  "env": {
    "MAX_THINKING_TOKENS": "10000",
    "CLAUDE_AUTOCOMPACT_PCT_OVERRIDE": "50",
    "CLAUDE_CODE_SUBAGENT_MODEL": "haiku",
    "ECC_HOOK_PROFILE": "standard"
  }
}
```

This cuts thinking token costs ~70% and routes subagent exploration tasks to Haiku (~80% cheaper) without quality loss.

Each developer adds to their shell profile (`~/.bashrc` or `~/.zshrc`):

```bash
export ECC_HOOK_PROFILE=standard
export ECC_MCP_HEALTH_FAIL_OPEN=1   # local dev only — NOT in CI
```

> **Hook edits do not hot-reload.** After changing any file in `.claude/hooks/` or `hooks.json`, restart the Claude Code session for the change to take effect.

### Step 7a: Configure MCP servers

Create `.claude/claude_mcp_config.json`:

```json
{
  "mcpServers": {
    "github": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "env": {
        "GITHUB_PERSONAL_ACCESS_TOKEN": "${GITHUB_PERSONAL_ACCESS_TOKEN}"
      }
    },
    "brave-search": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-brave-search"],
      "env": {
        "BRAVE_API_KEY": "${BRAVE_API_KEY}"
      }
    },
    "context7": {
      "command": "npx",
      "args": ["-y", "@upstash/context7-mcp"]
    }
  }
}
```

Add additional servers only with team agreement and document each in the register.

### Step 7b: Install the pre-commit hook and generate the Codex mirror

Before the first commit, install the pre-commit hook that keeps `AGENTS.md` in sync:

```powershell
powershell -File scripts/install-git-hooks.ps1
```

Generate the initial mirror and validate it:

```powershell
powershell -File scripts/sync-claude-to-codex.ps1
powershell -File scripts/validate-claude-codex-sync.ps1
```

Commit `AGENTS.md` as part of your day-zero commit. After this, the hook maintains it automatically on every commit. If you add or modify any agent or skill without the hook installed, run the sync script manually before opening a PR.

### Step 8: Complete and commit the repo setup checklist

Open [REPO-SETUP-CHECKLIST.md](REPO-SETUP-CHECKLIST.md), work through it, and commit the completed checklist to `main` as your first commit. This is the permanent record that day-zero setup was done correctly.

```bash
git add .
git commit -m "chore: initialise project with NC Agentic AI delivery methodology"
git push -u origin main
```

### Step 9: Create the remote environment spec (before first remote deployment)

Before any service is deployed to a remote environment, DevOps/RE must create `docs/project-foundation/remote-environment-spec.md` from the template at `templates/REMOTE-ENVIRONMENT-SPEC-TEMPLATE.md`.

This document is mandatory reading for agents before any remote debugging session. It must cover:
- Infrastructure topology and container runtime
- GitOps sync mechanism (how and when config propagates)
- Routing and ingress rules
- **Log access** — both infrastructure logs and solution logs, with tested, copy-pasteable access commands
- All configuration differences between local and remote

> **Log access is not optional.** The single biggest multiplier on the detect-to-fix cycle for remote bugs is whether the team can read logs. Establish and test log access before the first deployment — not after the first incident.

The `REMOTE-DEPLOYMENT-READINESS-GATE.md` checklist must pass before promotion to any remote environment.

### Step 10: Containerisation and DevOps handoff readiness (before DevOps begins infrastructure work)

Before the DevOps team begins Key Vault integration, TLS configuration, production Helm values, or any other infrastructure work, the dev team must satisfy the handoff checklist. This is a hard gate — if it is not met, the DevOps team has nothing stable to build on.

**Dev team must complete before DevOps starts:**

1. Complete `templates/DEV-DEVOPS-HANDOFF-CHECKLIST.md` — all sections A through H checked off and signed.
2. Ensure the Dockerfile satisfies STD-PLAT-009: multi-stage, pinned images, non-root user, SIGTERM handling, `PORT` env var. Use `templates/DOCKERFILE-TEMPLATE.md` as the starting point.
3. Ensure `docker-compose.yml` satisfies STD-PLAT-010: `depends_on: condition: service_healthy`, `.env.example` complete, full stack starts with `docker compose up`. Use `templates/DOCKER-COMPOSE-TEMPLATE.md` as the starting point.
4. Ensure `/health/live` and `/health/ready` are implemented per STD-OPS-003: live returns 200 always, ready returns 503 when DB is down, no auth on either.
5. Ensure structured logging satisfies STD-OPS-004: JSON to stdout, all four mandatory fields present, `correlationId` propagated per request.
6. Ensure the Helm chart is present under `/chart`, passes `helm lint ./chart && helm template ./chart`, and uses `templates/HELM-CHART-TEMPLATE/` as the structural reference.

The DevOps team is explicitly **not** responsible for the items above. They take ownership from the point the handoff checklist is signed.

---

## First Week — Project Foundation

### Sequence domain expert work ahead of builders

If your project has domain-specific rule interpretation (regulatory, legal, tax, compliance), the Domain Expert role must be **provisioned and working at least one sprint ahead of the Backend Builder at all times**. Domain interpretation is the slowest and least parallelisable part of delivery. If builders start before domain rules are stable, they will need to reverse implementation when rules are corrected.

### Sequence DevOps/RE work from the design phase

The DevOps / Release Engineer must be involved from the **design phase**, not the build phase. Late DevOps inclusion is one of the most expensive delivery failure modes for projects with remote deployment targets. Remote environments have constraints — infrastructure topology, GitOps sync behaviour, routing rules, network policies, log access — that are not visible to builders until they try to deploy, at which point the cost of reversals is high.

**Minimum DevOps/RE involvement from day one:**
- Co-own the environment map in `WAYS-OF-WORKING.md`
- Define the deployment pipeline before builders start
- Create `docs/project-foundation/remote-environment-spec.md` (see Step 9 below) before any remote deployment
- Participate in the design phase for any feature with infrastructure, integration, or deployment impact

This mirrors the domain expert sequencing principle: **DevOps/RE must be working on remote environment design at least one sprint before builders start deploying to remote.**

### Create your first GitHub issues

Before any design or build work starts, create issues for:
1. Solution requirements baseline (initial version)
2. Master solution design (initial version)
3. Tech stack decisions (if not already decided)
4. Agent role set agreement
5. Domain pack for each target context (if multi-context — do this before sprint 1)
6. Remote environment specification (DevOps/RE — create before any remote deployment)

Use the issue-first rule: **no work without a linked issue**.

### Establish the master documents

Two documents are mandatory before implementation begins:

| Document | Owner | Template |
|---|---|---|
| `docs/project-foundation/SOLUTION-REQUIREMENTS.md` | Business Analyst | Write fresh, or adapt from domain requirements |
| `docs/project-foundation/master-solution-design.md` | Solution Architect | Write fresh, or adapt from design inputs |

These are living documents — keep them updated as baseline versions as the project evolves.

### Set up `docs/memory.md`

Create `docs/memory.md` as the AI session-continuity document. It is read at the start of every session to restore project context.

Copy the template and fill in the initial values:

```bash
cp docs/project-foundation/templates/MEMORY-TEMPLATE.md docs/memory.md
```

Fill in at minimum: repository URL, project overview, current phase, and tech stack decisions made so far. Leave placeholder rows for decisions and risks — agents will populate them as work progresses.

Agents will keep this updated according to AGENT-RULES rule 18. You should review it weekly.

---

## How the Agent System Works

### Roles

Agents are specialist roles that Claude Code performs when asked. Each role has:
- A defined purpose and scope
- Mandatory reading before acting
- A handoff protocol (who it receives work from and passes work to)
- An output format

See [AGENT-OUTLINES.md](AGENT-OUTLINES.md) for the full role set.

### Agent session onboarding

Before acting in any role, an agent must be able to answer these questions from the project documents. This is the verification step — if the agent cannot answer them, it must read the relevant documents first.

1. What is the current requirements baseline version?
2. What is the agreed tech stack (backend, frontend, database, auth, container runtime)?
3. What is the primary architecture constraint that must not be violated?
4. Who are the active delivery roles on this project?
5. What is the current project phase and the most recent named decision?

These answers live in `CLAUDE.md` and `docs/memory.md`. If either is stale, update them before proceeding.

### Invoking an agent

In Claude Code, you switch to an agent role by starting a session with context:

```
Act as the [Role Name] for this task. Read AGENT-OUTLINES.md and your role file first.
Task: [issue number and description]
```

Or use a skill shortcut — skills automate the most common agent workflows:

```
/issue-to-build-plan
/write-acceptance-criteria
/api-contract-draft
/review-pr
```

### How agents hand off to each other

The handoff protocol (defined in AGENT-OUTLINES.md) ensures work moves correctly:

```
BA writes ACs on the issue
  → Delivery Designer produces 2-3 design options
    → Solution Architect selects and locks the approach
      → Builder implements
        → Test Builder adds tests
          → Code Reviewer reviews the PR
            → DevOps/Release Engineer deploys
```

For Class A changes (behavior or legal outcome), Solution Architect sign-off is required before any Builder starts.

### Reviewer roles are read-only

Code Reviewer and Design Critic do not edit files. They produce findings with severity labels. The relevant Builder role acts on findings.

---

## Daily Workflow

### Starting a new feature

```
1. Create a GitHub issue
2. Invoke Business Analyst → acceptance criteria on the issue
3. Invoke Delivery Designer → options doc (if design is non-trivial)
4. Invoke Solution Architect → decision on the issue
5. Create branch: feat/<issue-number>-short-name
6. Invoke Backend Builder / Frontend Builder → implementation
7. Invoke Test Builder → tests
8. Open PR
9. Invoke Code Reviewer → findings (handle any blockers)
10. Merge when CI is green and one approval is in
```

### Fixing a bug

```
1. Create a GitHub issue
2. Create branch: fix/<issue-number>-short-name
3. Invoke appropriate Builder → fix
4. Open PR → merge when CI is green
```

### Context management — compaction discipline

Do not rely on automatic compaction — it fires at the wrong time and discards relevant context. Use manual `/compact` at logical breakpoints:

| Phase Transition | Action | Why |
|---|---|---|
| Research → Planning | `/compact` | Research context is bulky; plan is the distilled output |
| Planning → Implementation | `/compact` | Plan is in TodoWrite or a file; free context for code |
| After debugging session resolved | `/compact` | Debug traces pollute context for unrelated work |
| After a failed approach | `/compact` | Clear dead-end reasoning before trying a new approach |
| Mid-implementation | **Do NOT compact** | Losing variable names, file paths, partial state is costly |
| Switching to unrelated task | `/clear` | Full reset is safer than partial compaction |

The `suggest-compact` hook will prompt at 40 tool calls — treat it as a signal to check the table above, not an automatic instruction to compact.

### Model tiering — cost management

| Task Class | Model | Why |
|---|---|---|
| File exploration, index builds, simple queries | Haiku | ~80% cheaper; sufficient for read-heavy research |
| Builder tasks, code review, most daily work | Sonnet | Default; best cost/quality ratio |
| Class A Design Critic, Security Review, Council decisions | Opus | Deep reasoning required; use selectively |

Set `CLAUDE_CODE_SUBAGENT_MODEL=haiku` (already in `.claude/settings.json`) to route subagent exploration automatically. Switch to Opus explicitly when invoking critic roles for Class A changes.

### When CI fails

1. Check the CI logs directly
2. Invoke the `ci-failure-root-cause-fixer` skill for a systematic diagnosis
3. Fix on the same branch, push, and re-check
4. If the failure is unrelated to your change: document it as a known issue and raise a separate issue

### When production is broken

Use the [EMERGENCY-BYPASS-PROCEDURE.md](EMERGENCY-BYPASS-PROCEDURE.md). The short version:
1. Notify one other team member
2. Apply the targeted fix
3. Push directly to main (admin only)
4. Trigger CI manually
5. Create a retroactive issue and PR within 24 hours

---

## Customising the Methodology

### What you should customise
- Error code prefix (set to your project acronym)
- Agent role set (remove roles you won't use)
- Domain data classification (add project-specific data elements)
- Environment map (fill in real environment names)
- Optional governance controls (see WAYS-OF-WORKING §Project Configuration Options)

### What you should not change without team agreement
- Core branch rules (no direct push to main)
- Issue-first workflow
- Definition of done
- The standards pack authority order
- Agent rules 1–14 (the mandatory rules)

### Adding a new standard

1. Create the standard file under `docs/project-foundation/standards/<category>/`
2. Use the existing frontmatter format (id, title, status, owner, etc.)
3. Add it to `standards/README.md`
4. Raise the addition as a PR so the team reviews it

### Adding a new agent role

1. Create the role file under `.claude/agents/<role-name>.md`
2. Add it to `AGENT-OUTLINES.md`
3. Define its output format and handoff position
4. Raise as a PR — team agreement required before anyone uses the role

---

## Checklist: Am I Ready to Start?

Complete these before calling the first sprint:

- [ ] `CLAUDE.md` exists at repo root with all placeholders filled in
- [ ] Branch protection configured (no direct push, require CI, require approval)
- [ ] All CI secrets added to GitHub Actions and documented in `CI-SECRET-REGISTER.md`
- [ ] MCP servers configured and tested (run `claude` in the repo and verify MCP connects)
- [ ] `docs/memory.md` created with initial project context
- [ ] PR template in place at `.github/pull_request_template.md`
- [ ] Solution requirements baseline exists (even a v0.1 is fine)
- [ ] Master solution design exists (even a stub is fine)
- [ ] Every team member has completed local developer setup (see REPO-SETUP-CHECKLIST.md §Local Developer Setup)
- [ ] Every team member has done at least one test PR through the full branch → CI → merge flow
- [ ] For containerised services: `docker build` and `docker compose up` both succeed locally (STD-PLAT-009, STD-PLAT-010)
- [ ] For containerised services: `/health/live` and `/health/ready` implemented and manually verified (STD-OPS-003)
- [ ] For containerised services: logs emit JSON to stdout with all four mandatory fields (STD-OPS-004)
- [ ] For containerised services: `templates/DEV-DEVOPS-HANDOFF-CHECKLIST.md` completed and signed before DevOps begins infrastructure work

---

## Common Mistakes to Avoid

| Mistake | Consequence | Prevention |
|---|---|---|
| Pushing directly to main without using the emergency procedure | Bypasses CI, no audit trail, policy violation | Branch protection enforces this; emergency path documented |
| Starting work without a GitHub issue | No traceability, no way to close properly | Agent Rules rule 5: issue-first |
| Prototyping a domain model against only one context | Schema gaps emerge when a second context is added | Pre-implementation checkpoint: ACs must cover all target contexts |
| Adopting infrastructure without vetting license, ecosystem, and ops fit | Forced reversal mid-project is expensive at agentic velocity | Pre-implementation checkpoint: infrastructure decision record required |
| Letting the domain expert role fall behind the builders | Builders implement rules that are later corrected → rework | Domain Expert is always one sprint ahead; never concurrent with builders |
| Including DevOps only at build/release phase | Remote deployment constraints discovered late; CI/CD pipeline designed after code is written; log access and routing set up under pressure during incidents | DevOps/RE in design phase from day one; remote environment spec before first deployment |
| Deploying to remote without tested log access | First production bug debugged without logs; detect-to-fix cycle multiplied by guesswork | Gate B of REMOTE-DEPLOYMENT-READINESS-GATE.md — log access tested before deployment, not after first incident |
| Diverging from GitOps and routing ways-of-working | Routing changes that aren't communicated break remote deployments; rebuild and redeploy cycles are expensive | WAYS-OF-WORKING.md §GitOps — all Helm and routing changes go through the agreed protocol |
| Local and remote configs diverging silently | Bugs only reproducible on remote; agents cannot diagnose without remote access | Local/remote parity standard (STD-PLAT-008); all divergences documented in remote-environment-spec.md |
| Renumbering or editing applied DB migration scripts | Checksum failures crash the application on deploy | DB migration standard (STD-PLAT-006) |
| Introducing a feature flag without registering it | CI tests fail when the flag is not set | Feature flag standard (STD-PLAT-007) |
| Hardcoding user-facing strings in multi-language systems | i18n fix commits accumulate across weeks | Frontend Builder rule: no merge with unregistered strings |
| Treating traceability as an end-of-phase activity | Release gate blocked by 15% traceability gap | Agent Rules rule 21: trace links updated same cycle as change |
| SA reviewing their own design — no Design Critic pass | Blind spots persist into implementation | Agent Rules rule 20 + AGENT-OUTLINES Design Critic mandatory gate |
| Using production data in AI prompts | Data sensitivity violation | AI standard (STD-AI-001), data classification standard |
| Merging without updating docs/memory.md after a decision | Next AI session has stale context | Agent Rules rule 18 |
| Deferring the repo setup checklist | Missing secrets/protection discovered mid-sprint | Day-zero checklist is mandatory before first feature commit |
| Skipping the dev→DevOps handoff checklist | DevOps discovers non-root, SIGTERM, or health endpoint gaps mid-infrastructure work; rework is expensive at that stage | Complete `templates/DEV-DEVOPS-HANDOFF-CHECKLIST.md` before DevOps starts — it is a hard gate |
| Container running as root | Privilege escalation if container isolation is bypassed | STD-PLAT-009-R3: explicitly create and switch to a non-root user in the Dockerfile |
| No SIGTERM handling | In-flight requests dropped on every Kubernetes rolling deploy | STD-PLAT-009-R5: configure graceful shutdown in the framework (e.g., `server.shutdown=graceful` in Spring Boot) |
| `/health/ready` returns 200 even when the DB is down | Kubernetes routes traffic to the pod; all requests fail while the DB is unreachable | STD-OPS-003-R3: `/health/ready` must return 503 when any required dependency is unreachable |
| `/health/live` checks the database | DB slowness causes spurious pod restarts that look like application crashes | STD-OPS-003-R2: `/health/live` must never check external dependencies |
| Logs not in JSON format | Logs cannot be queried in Loki/Splunk/Azure Monitor; first production incident is debugged without structured search | STD-OPS-004-R1: JSON to stdout from day one — never plain-text, never to files |
| Missing correlationId in logs | A single user-visible failure produces dozens of disconnected log entries; detect-to-fix cycle multiplied | STD-OPS-004-R3: accept from incoming header or generate UUID; propagate to every log entry for the request |
