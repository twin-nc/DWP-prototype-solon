# CLAUDE.md — DWP Debt Collection

> **Instructions for project initiator:** Delete these instructions before committing.
> This file is read by Claude Code at the start of every session. Keep it accurate and up to date.

---

## What This Is

DWP Debt Collection Management System — a greenfield COTS debt collection platform implementation for the UK Department for Work and Pensions. The system manages the full debt collection lifecycle: customer and account management, automated workflow and decisioning, repayment plans, multi-channel communications (SMS, email, letter, IVR), third-party placements, and management information. Primary users are DWP collections agents, team leaders, operations managers, and compliance staff (~4,000 concurrent users).

Regulated under: Consumer Credit Act 1974, UK GDPR / Data Protection Act 2018, FCA vulnerability guidance (FG21/1), Debt Respite Scheme (breathing space), UK Insolvency Rules 2016, DWP-specific debt recovery policies. UI must comply with WCAG 2.x AA, GDS Open Standards, and DWP Digital Design Authority governance.

**COTS note:** This is a greenfield COTS implementation. The project starts with no pre-existing workflow configuration, but the selected COTS platform still handles its own internal database, workflow engine, and UI. Netcompany builds and owns integration adapters, configuration scripts, and bespoke extension services. Standards apply differently by ownership layer - see `docs/project-foundation/standards/deviations/DEV-001-cots-scope.md` before starting implementation.

Target domains: UK government debt collection — benefit debt recovery, third-party placements, repayment plan management, multi-channel contact strategy.

---

## Mandatory Reading Before Starting Work

| Document | Path | Read When |
|---|---|---|
| **Project Memory** | `docs/memory.md` | Always — shared project context; updated continuously |
| Ways of Working | `docs/project-foundation/WAYS-OF-WORKING.md` | Always — before any task |
| Agent Rules | `docs/project-foundation/AGENT-RULES.md` | Always — before any task |
| Agent Outlines | `docs/project-foundation/AGENT-OUTLINES.md` | Always — before acting in a role |
| Your role instruction file | `.claude/agents/<role>.md` | Before acting in that role |
| **COTS Scope Deviation** | `docs/project-foundation/standards/deviations/DEV-001-cots-scope.md` | Before any implementation — defines which standards apply to which layer |
| **Domain Data Classification** | `docs/project-foundation/standards/security/domain-data-classification.md` | Before any feature involving customer, debt, or vulnerability data |
| Solution Requirements | `docs/project-foundation/SOLUTION-REQUIREMENTS.md` | Before any feature or design work |
| Master Solution Design | `docs/project-foundation/master-solution-design.md` | Before any architecture or implementation work |
| Standards Pack | `docs/project-foundation/standards/` | Before any standards-sensitive work |
| Skills Index | `.claude/skills/README.md` | Before choosing a skill to invoke |
| **Remote Environment Spec** | `docs/project-foundation/remote-environment-spec.md` | Before any remote deployment or remote debugging session |

> **`docs/memory.md` is the primary shared context.** Always update it when decisions are made, architecture changes, or project status shifts.

---

## Project Architecture (Summary)

- **COTS Platform** — {{COTS vendor and product name — to be confirmed on contract award}}. Vendor-managed SaaS (OPP05); hosted in UK only (OPP01). Handles workflow engine, decisioning, UI, and internal data storage.
- **Integration Adapters** — Netcompany-built services connecting COTS to DWP systems. Key integrations: DWP SSO (OAuth 2.0, INT01), DWP Data Integration Platform / Power BI (INT06, MIR.1), WorldPay payment gateway (DW.21, RPF.21), DWP IVR (RPF.27), DWP notifications (DIC.15, DIC.29), bureau/scorecard feeds (chapter 15).
- **File Transfer Layer** — FTPS with TLS 1.2 and PGP encryption for bulk data exchange with DWP (INT07, INT17-INT25). SHA-256 checksums required. Files >2GB split with sequential naming.
- **Auth** — DWP SSO via OAuth 2.0 (INT01). User provisioning via DWP Place line-manager approval flow (UAAF.1). Minimum 100 domains supported (INT28).
- **Observability** — {{to be confirmed — must align with DWP's monitoring and alerting strategy (SER16)}}

Key constraints:
- All hosting and data storage in UK only — no exceptions (OPP01, OPP03).
- COTS internal schema and infrastructure is vendor-managed — do not apply Netcompany migration or containerisation standards to the COTS layer. See DEV-001.
- DWP Payment Allocation system provides payment-to-account allocation instructions — COTS must follow these instructions, not derive its own allocation logic (DW.23, DW.87).

---

## Current Requirements Baseline

Version: `v0.1` — sourced from tender documents C8618-FDS-Attachment-4a (Functional) and C8618-FDS-Attachment-4b (Non-Functional).

Key requirement groups (21 capability areas):
- **CAS** — Customer & Account Structure (17 requirements)
- **DIC** — Data & Information Capture (37 requirements)
- **DW** — Decisions & Workflows (88 requirements)
- **RPF** — Repayment Plan Functionality (37 requirements)
- **UAAF** — User Access & Admin Functions (26 requirements)
- **MIR** — MI & Reporting | **A** — Analytics | **UI** — User Interface Screens
- **WAM** — Work Allocation & Monitoring | **AAD** — Agent Actions & Dispositions
- **TPM** — 3rd Party Management | **IE** — Income & Expenditure | **BSF** — Bureau & Scorecard
- **CC** — Contact Channels | **ITP** — Interfaces to 3rd Party Systems
- **SOR** — System of Record | **CP** — Change Processes | **SDR** — System Development & Roadmap
- **MP** — MP (Member of Parliament) Requirements

Non-functional requirements: ACC (Accessibility), AVA (Availability — 99.9%), COM (Compliance), INT (Interoperability), SEC (Security), SCA (Scalability — 4,000 concurrent users), SER (Service management — P1 within 2h 24/7).

Full requirements: `docs/project-foundation/SOLUTION-REQUIREMENTS.md`
Full solution design: `docs/project-foundation/master-solution-design.md`

> **Note:** Requirements baseline reflects tender documents only. Formal requirements baseline will be established at contract award. All agent work must trace to a requirement ID.

---

## Tech Stack

| Layer | Technology | Decision Record |
|---|---|---|
| COTS Platform | {{vendor and product — confirmed on contract award}} | {{link to decision log}} |
| Integration adapters | {{language/framework — to be decided}} | {{link to decision log}} |
| Auth | DWP SSO / OAuth 2.0 (INT01) | Tender requirement |
| Payment gateway | WorldPay (RPF.21, DW.21) | Tender requirement |
| File transfer | FTPS + TLS 1.2 + PGP (INT07, INT17-INT25) | Tender requirement |
| Data integration | DWP Data Integration Platform / Power BI (INT06, MIR.1) | Tender requirement |
| Container runtime | {{to be confirmed — must align with STD-PLAT-009/010 for adapter services}} | {{link to decision log}} |
| Secrets | {{to be confirmed — must align with DWP Key Vault approach}} | {{link to decision log}} |
| Observability | {{to be confirmed — must align with DWP monitoring and alerting strategy (SER16)}} | {{link to decision log}} |
| CI/CD | {{to be confirmed}} | {{link to decision log}} |

---

## Ways of Working (Summary)

1. `main` is the single source of truth. No direct pushes.
2. Every change requires a GitHub Issue before work starts.
3. Branch naming: `feat/<issue-number>-short-name` or `fix/<issue-number>-short-name`.
4. Every change is delivered via Pull Request into `main`.
5. One non-author approval required before merge.
6. Before final push, sync with `origin/main`; if it moved, rebase and rerun all tests.
7. Emergency direct pushes follow `docs/project-foundation/EMERGENCY-BYPASS-PROCEDURE.md`.

Full rules: `docs/project-foundation/WAYS-OF-WORKING.md`

---

## Definition of Done

Work is complete only when all of the following are true:
1. Issue linked and scoped.
2. PR approved by a non-author.
3. Full CI is green on an up-to-date branch.
4. PR merged into `main`.
5. Issue auto-closed by merge.
6. Required Markdown documentation updated.
7. Required Word documents republished or explicitly deferred with team agreement.
8. `docs/memory.md` updated if a named decision was made or project state shifted.

---

## Local Development

- Start stack: `{{to be confirmed — depends on COTS vendor dev environment approach}}`
- Runbook: `{{to be confirmed}}`

> **Note:** Local dev environment for the COTS platform is vendor-dependent. Netcompany-built integration adapters must follow STD-PLAT-010 (local-dev-environment standard). Confirm with the COTS vendor what local/sandbox environment they provide before applying containerisation standards.

## DWP Compliance Gates

The following DWP governance steps are mandatory and must not be bypassed. Confirm each with the DWP client before the relevant milestone:

| Gate | Requirement | When |
|---|---|---|
| Digital Design Authority approval | COM11a | Before first production deployment |
| Security risk assessment sign-off | COM11b | Before first production deployment |
| Accessibility compliance assessment | COM11c, ACC01-ACC03 | Before any UI goes to UAT |
| Data Protection Impact Assessment (DPIA) | COM11d | Before any personal data is processed |
| Service Assessment | COM11e | Before go-live |
| Place Code of Connectivity (CoCo) policy | SER17 | Before any DWP network connection |

---

## Agent Roles

`.claude/agents/` is the canonical agent source. Do not use roles outside this set without team agreement.

> **Codex mirror:** `AGENTS.md` at the repo root is generated from `.claude/agents/` and `.claude/skills/` via `scripts/sync-claude-to-codex.ps1`. It must not be edited manually. The pre-commit hook (installed via `scripts/install-git-hooks.ps1`) regenerates it automatically on every commit. If the hook is not installed, run the sync script manually after any change to agents or skills, and validate with `scripts/validate-claude-codex-sync.ps1`.

| Role | File |
|---|---|
| Solution Architect | `.claude/agents/solution-architect.md` |
| Delivery Designer | `.claude/agents/delivery-designer.md` |
| Business Analyst | `.claude/agents/business-analyst.md` |
| **DWP Debt Domain Expert** | `.claude/agents/dwp-debt-domain-expert.md` |
| DB Designer | `.claude/agents/db-designer.md` |
| DevOps / Release Engineer | `.claude/agents/devops-release-engineer.md` |
| SRE / Platform Engineer | `.claude/agents/sre-platform-engineer.md` |
| Backend Builder | `.claude/agents/backend-builder.md` |
| Frontend Builder | `.claude/agents/frontend-builder.md` |
| Integration / Protocol Specialist | `.claude/agents/integration-protocol-specialist.md` |
| Test Designer | `.claude/agents/test-designer.md` |
| Test Builder | `.claude/agents/test-builder.md` |
| Refactorer | `.claude/agents/refactorer.md` |
| Code Reviewer *(read-only)* | `.claude/agents/code-reviewer.md` |
| Design Critic *(read-only)* | `.claude/agents/design-critic.md` |
| Traceability Steward | `.claude/agents/traceability-steward.md` |

> **DWP Debt Domain Expert must be one sprint ahead of the Backend Builder at all times.** No builder works on legally consequential behavior (disclosure, vulnerability, insolvency, breathing space, CCA-governed processes) without a domain ruling from this role.

---

## Skills

All available skills are indexed at `.claude/skills/README.md`.

Key skills for common tasks:

| Task | Skill |
|---|---|
| Plan work from an issue | `issue-to-build-plan` |
| Write acceptance criteria | `write-acceptance-criteria` |
| Draft an API contract | `api-contract-draft` |
| Classify a change | `change-classification-assistant` |
| Review a PR | `review-pr` |
| Critique a design | `design-review` |
| Check release readiness | `release-readiness-gate` |
| Generate domain tests | `generate-domain-tests` |
| Generate E2E scenarios | `generate-e2e-scenarios` |
| Check traceability | `traceability-and-evidence-enforcer` |
| Update docs for a change | `update-docs-for-change` |
| Fix a CI failure | `ci-failure-root-cause-fixer` |
| Check remote deployment readiness | `remote-deployment-readiness` |

---

## Error Code Standard

- Format: `DC-<DOMAIN>-<4 digits>` (e.g., `DC-API-0001`, `DC-WORKFLOW-0001`, `DC-FTPS-0001`)
- Codes are stable and unique — never reuse a retired code.
- Every API error response must include: `code`, `message` (safe for users), `http_status`, `category`, `retriable`, `correlation_id`.
- Never expose stack traces, debt data, or customer PII in API error messages.
- Any PR introducing a new error path must add the code to the catalog and assert it in tests.

Full error envelope spec: `docs/project-foundation/standards/integration/error-semantics-and-stability.md`

---

## Documentation Governance

- Markdown in `docs/` is the working source of truth.
- Word documents are published stakeholder snapshots — derived from Markdown.
- If MD and Word conflict, treat Markdown as authoritative until Word is republished.
- Every issue or PR must declare one of: `no doc impact` / `md update required` / `docx republish required` / `both required`.

Full policy: `docs/project-foundation/DOCUMENTATION-POLICY.md`

---

## MCP Servers

Configured in `.claude/claude_mcp_config.json`. Required environment variables:

| Server | Env Var | Purpose |
|---|---|---|
| GitHub | `GITHUB_PERSONAL_ACCESS_TOKEN` | GitHub API access |
| Brave Search | `BRAVE_API_KEY` | Web search |
| Context7 | *(none required)* | Library documentation |
| {{COTS vendor API — to be confirmed}} | `{{ENV_VAR}}` | COTS configuration API access |

Never hardcode secrets. Never commit `.env` files.

---

## Required Output Minimum

Every agent response must include:
1. What was done.
2. Files impacted (if any).
3. Verification or review basis.
4. Remaining risk or open question.
5. Documentation impact classification.
6. Documentation updated, required, or explicitly deferred.

Keep outputs concise — bullets, no repeated context.

---

## GitHub CLI

{{If `gh` is not on PATH in your environment, document the full path here.}}
Example: `"/c/Program Files/GitHub CLI/gh.exe"`

