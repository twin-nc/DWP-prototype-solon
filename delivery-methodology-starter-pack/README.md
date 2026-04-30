# NC Agentic AI Delivery — Methodology Starter Pack

Version: 1.0 | Last updated: 2026-04-07

## What This Is

A ready-to-use delivery methodology for Netcompany projects that use Claude Code and AI agent-assisted delivery. Copy this folder into a new project repository, fill in the placeholders, and your team has a governed, auditable, agent-ready delivery model from day one.

## What It Covers

- Ways of working, branch strategy, PR governance, and the emergency bypass path
- Agent role structure, rules, handoff protocol, and output standards
- Documentation policy and token-efficient writing standards
- Repo setup checklist and day-zero configuration
- A full standards pack covering governance, integration, platform, security, and operations
- A getting-started guide for new project leads

## How to Use This Pack

Assets in this pack land in **four distinct locations** in the target repo. See `INSTALL-MANIFEST.md` for the authoritative file-to-destination mapping — the table below is a summary only.

Quick summary:

| Target | What goes there |
|---|---|
| `docs/project-foundation/` | All pack files and directories **except** `.claude/`, `.github/`, and `README.md` |
| repo root `.claude/` | `.claude/agents/` and `.claude/skills/` |
| repo root `.github/` | `.github/pull_request_template.md` and `.github/dependabot.yml` |
| repo root `CLAUDE.md` | `CLAUDE-MD-TEMPLATE.md` filled in — place as `CLAUDE.md` at repo root |

**Install steps:**

1. Follow `INSTALL-MANIFEST.md` to copy assets to the correct locations
2. Work through `getting-started-guide.md` from top to bottom
3. Complete `REPO-SETUP-CHECKLIST.md` before the first commit to `main`
4. Fill in `CLAUDE-MD-TEMPLATE.md` and place it as `CLAUDE.md` at the repo root
5. Review and configure the optional sections in `WAYS-OF-WORKING.md`
6. Customise `AGENT-OUTLINES.md` to match your team's agreed role set

## Document Index

### Core Governance
| Document | Purpose |
|---|---|
| [WAYS-OF-WORKING.md](WAYS-OF-WORKING.md) | Branch strategy, PR rules, merge gates, documentation policy |
| [AGENT-RULES.md](AGENT-RULES.md) | Mandatory rules for all project agents |
| [AGENT-OUTLINES.md](AGENT-OUTLINES.md) | Agent role structure, handoff protocol, output standards |
| [DOCUMENTATION-POLICY.md](DOCUMENTATION-POLICY.md) | How to write and maintain project documentation |
| [EMERGENCY-BYPASS-PROCEDURE.md](EMERGENCY-BYPASS-PROCEDURE.md) | Break-glass direct-to-main procedure |
| [SOUL.md](SOUL.md) | Five core invariants — tiebreaker when rules conflict |

### Setup
| Document | Purpose |
|---|---|
| [getting-started-guide.md](getting-started-guide.md) | Step-by-step guide for project leads starting a new NC Agentic AI project |
| [REPO-SETUP-CHECKLIST.md](REPO-SETUP-CHECKLIST.md) | Day-zero configuration checklist |
| [CLAUDE-MD-TEMPLATE.md](CLAUDE-MD-TEMPLATE.md) | Template CLAUDE.md for the project root |
| [PRE-IMPLEMENTATION-CHECKPOINT.md](PRE-IMPLEMENTATION-CHECKPOINT.md) | Four-gate checkpoint before any Builder starts on a non-trivial feature |
| [SKILLS-GUIDE.md](SKILLS-GUIDE.md) | Skills library organisation: what to keep, embed, restructure, and add |
| [SKILL-DEVELOPMENT-GUIDE.md](SKILL-DEVELOPMENT-GUIDE.md) | How to create, test, and maintain skills |
| [REMOTE-DEPLOYMENT-READINESS-GATE.md](REMOTE-DEPLOYMENT-READINESS-GATE.md) | Five-gate checklist before any remote deployment; log access, parity, GitOps, rollback |

### Templates
| Document | Purpose |
|---|---|
| [templates/MEMORY-TEMPLATE.md](templates/MEMORY-TEMPLATE.md) | Project memory template — copy to `docs/memory.md` on day one |
| [templates/FEATURE-NOTE-TEMPLATE.md](templates/FEATURE-NOTE-TEMPLATE.md) | Feature note template |
| [templates/DECISION-LOG-TEMPLATE.md](templates/DECISION-LOG-TEMPLATE.md) | Decision log entry template |
| [templates/RUNBOOK-TEMPLATE.md](templates/RUNBOOK-TEMPLATE.md) | Runbook template |
| [templates/FEATURE-FLAG-REGISTER.md](templates/FEATURE-FLAG-REGISTER.md) | Live feature flag register (project-level) |
| [templates/CI-SECRET-REGISTER.md](templates/CI-SECRET-REGISTER.md) | CI/CD secret register (project-level) |
| [templates/PROJECT-STATUS-REPORT-TEMPLATE.md](templates/PROJECT-STATUS-REPORT-TEMPLATE.md) | Stakeholder-facing project progress report |
| [templates/BUILD-PHASE-TRACKER-TEMPLATE.md](templates/BUILD-PHASE-TRACKER-TEMPLATE.md) | Build phase workstream and milestone tracker |
| [templates/RELEASE-EVIDENCE-PACK-TEMPLATE.md](templates/RELEASE-EVIDENCE-PACK-TEMPLATE.md) | Release evidence pack (fillable; required by STD-GOV-006) |
| [templates/REMOTE-ENVIRONMENT-SPEC-TEMPLATE.md](templates/REMOTE-ENVIRONMENT-SPEC-TEMPLATE.md) | Remote environment spec — topology, GitOps, routing, log access, parity (fill in before first remote deployment) |
| [templates/SANTA-RUBRIC-CLASS-A-CHANGE.md](templates/SANTA-RUBRIC-CLASS-A-CHANGE.md) | Dual-reviewer scoring rubric for Class A changes |
| [templates/SANTA-RUBRIC-RELEASE-EVIDENCE.md](templates/SANTA-RUBRIC-RELEASE-EVIDENCE.md) | Dual-reviewer scoring rubric for release evidence packs |
| [templates/SANTA-RUBRIC-SECURITY-REVIEW.md](templates/SANTA-RUBRIC-SECURITY-REVIEW.md) | Dual-reviewer scoring rubric for security-sensitive PRs |

### Agent Roles
| File | Role | Notes |
|---|---|---|
| [.claude/agents/solution-architect.md](.claude/agents/solution-architect.md) | Solution Architect | Locks final architecture decisions |
| [.claude/agents/delivery-designer.md](.claude/agents/delivery-designer.md) | Delivery Designer | Proposes 2-3 options; SA decides. Renamed from "Solution Designer" |
| [.claude/agents/business-analyst.md](.claude/agents/business-analyst.md) | Business Analyst | Business rules, state models, acceptance criteria |
| [.claude/agents/db-designer.md](.claude/agents/db-designer.md) | DB Designer | Schema, migrations, indexes |
| [.claude/agents/backend-builder.md](.claude/agents/backend-builder.md) | Backend Builder | Backend services, APIs, error handling |
| [.claude/agents/frontend-builder.md](.claude/agents/frontend-builder.md) | Frontend Builder | UI screens, forms, i18n |
| [.claude/agents/devops-release-engineer.md](.claude/agents/devops-release-engineer.md) | DevOps / Release Engineer | CI/CD, deployment, release evidence |
| [.claude/agents/integration-protocol-specialist.md](.claude/agents/integration-protocol-specialist.md) | Integration / Protocol Specialist | API contracts, protocol design, external handoffs |
| [.claude/agents/sre-platform-engineer.md](.claude/agents/sre-platform-engineer.md) | SRE / Platform Engineer | Reliability, observability, runbooks |
| [.claude/agents/code-reviewer.md](.claude/agents/code-reviewer.md) | Code Reviewer *(read-only)* | PR review, regression risk |
| [.claude/agents/design-critic.md](.claude/agents/design-critic.md) | Design Critic *(read-only)* | Mandatory gate before SA locks any decision |
| [.claude/agents/test-builder.md](.claude/agents/test-builder.md) | Test Builder | Implements unit, integration, and e2e tests |
| [.claude/agents/test-designer.md](.claude/agents/test-designer.md) | Test Designer | Test strategy; typically embedded in build planning |
| [.claude/agents/refactorer.md](.claude/agents/refactorer.md) | Refactorer | Structural cleanup without behavior change |
| [.claude/agents/dwp-debt-domain-expert.md](.claude/agents/dwp-debt-domain-expert.md) | **DWP Debt Domain Expert** | CCA, GDPR, FCA vulnerability, breathing space, insolvency, DWP payment allocation — must be one sprint ahead of Backend Builder |

### Skills Pack
| Location | Contents |
|---|---|
| [.claude/skills/README.md](.claude/skills/README.md) | Full skills index — 47 skills |
| [.claude/skills/](.claude/skills/) | Individual skill files (one subdirectory per skill) |
| [.claude/contexts/](.claude/contexts/) | Context mode files — dev, research, review |
| [.claude/hooks/](.claude/hooks/) | Automation hooks (10 hooks across minimal/standard/strict profiles) |
| [.claude/hooks.json](.claude/hooks.json) | Hook registry and profile configuration |
| [.claude/settings.json](.claude/settings.json) | Default environment variable configuration |

See [SKILLS-GUIDE.md](SKILLS-GUIDE.md) for guidance on which skills to always invoke, which to embed in role workflows, and which are missing for common project types.

### GitHub Config
| File | Purpose |
|---|---|
| [.github/pull_request_template.md](.github/pull_request_template.md) | Standard PR template — copy to `.github/` in new project |
| [.github/dependabot.yml](.github/dependabot.yml) | Dependabot config example — customise per tech stack |

### Standards Pack
| Document | Purpose |
|---|---|
| [standards/README.md](standards/README.md) | Standards index and authority model |
| [standards/ai/](standards/ai/) | AI agent governance |
| [standards/development/](standards/development/) | Testing, development, and accessibility standards |
| [standards/development/accessibility-standard.md](standards/development/accessibility-standard.md) | **STD-DEV-002** — WCAG AA, assistive tech matrix, DWP Digital Design Authority sign-off |
| [standards/governance/](standards/governance/) | Change classification, traceability, release evidence, contracts |
| [standards/integration/](standards/integration/) | Error semantics, integration reliability, file transfer |
| [standards/integration/file-transfer-standard.md](standards/integration/file-transfer-standard.md) | **STD-INT-003** — FTPS/PGP, TLS 1.2, SHA-256, DWP Data Integration Platform |
| [standards/operations/](standards/operations/) | Observability, CI/CD secret management |
| [standards/platform/](standards/platform/) | Determinism, migrations, feature flags, evidence, state |
| [standards/security/](standards/security/) | Security boundaries, data sensitivity, classification |
| [standards/security/domain-data-classification.md](standards/security/domain-data-classification.md) | **DWP Debt Collection data classification** — populated from tender requirements; requires security owner + DWP client review |
| [standards/deviations/](standards/deviations/) | Deviation record template and approved deviations |
| [standards/deviations/DEV-001-cots-scope.md](standards/deviations/DEV-001-cots-scope.md) | **COTS scope boundary** — which standards apply to COTS internals vs Netcompany-built services |

## What to Customise Per Project

| Item | Where | Notes |
|---|---|---|
| Project name and description | `CLAUDE-MD-TEMPLATE.md` | Fill in all `{{PROJECT_*}}` placeholders |
| Error code prefix | `WAYS-OF-WORKING.md` §Error Code Standard | Replace `PROJ` with your project prefix |
| Approval rules | `WAYS-OF-WORKING.md` §Project Configuration Options | Opt in to stricter controls if appropriate |
| PR size guidance | `WAYS-OF-WORKING.md` §Recommended Practices | Enable if team agrees |
| Agent role set | `AGENT-OUTLINES.md` + `.claude/agents/` | Remove roles not needed; add Domain Expert role if project has regulatory/legal domain; do not add roles without team agreement |
| Skills index primary agents | `.claude/skills/README.md` | Update `domain-expert` references to your project's actual domain role name |
| Skills audit | `SKILLS-GUIDE.md` | Mark N/A skills at kickoff; add missing skills for known failure modes |
| Domain data classification | `standards/security/domain-data-classification-template.md` | Replace generic examples with project-specific data elements |
| Environment map | `WAYS-OF-WORKING.md` §Environment Map | Fill in actual environment names and deploy targets |
| MCP servers | `.claude/claude_mcp_config.json` (repo root) | Configure per project toolset |
| Dependabot config | `.github/dependabot.yml` | Uncomment and configure the correct package ecosystems for your stack |
| Release evidence pack §3 | `templates/RELEASE-EVIDENCE-PACK-TEMPLATE.md` | Replace placeholder API names with actual contracts for your project |