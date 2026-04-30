# Documentation Policy

## Purpose
Development documentation is team memory for changing code. It exists to preserve key context with the lowest reasonable overhead. This document defines both the policy and the day-to-day workflow.

---

## Principles

1. Write only what code and tests do not already make obvious.
2. Keep one source of truth per topic and link to it instead of repeating content.
3. Use bullet points and short sections, not long narrative text.
4. Start long docs with a short TL;DR (3-5 bullets).
5. Keep each document scoped to one concern.
6. Remove stale content whenever a related section is touched.
7. Treat tokens as a limited budget — AI-assisted workflows are context-window constrained.

---

## Document Register

Every agent must know what exists, who maintains it, and when Word publication is required.

| Document | Location | Format | Audience | Maintained By | Word Published When |
|---|---|---|---|---|---|
| Master Requirements | `docs/project-foundation/SOLUTION-REQUIREMENTS.md` | MD + Word | Stakeholders & team | Traceability Steward | Requirements baseline, review, or audit |
| Release 1 Capabilities | `docs/release/release-1-capabilities.md` | MD | Product design, delivery planning, agents | Product Owner / BA Lead | Stakeholder review, release scope approval, or audit |
| Master Solution Design | `docs/project-foundation/master-solution-design.md` | MD + Word | Stakeholders & team | Solution Architect | Architecture baseline or stakeholder review |
| Ways of Working | `docs/project-foundation/WAYS-OF-WORKING.md` | MD | Team & agents | Team | External audit or governance handover only |
| Agent Rules | `docs/project-foundation/AGENT-RULES.md` | MD | Agents | Team | N/A |
| Agent Outlines | `docs/project-foundation/AGENT-OUTLINES.md` | MD | Agents | Team | N/A |
| Standards Pack | `docs/project-foundation/standards/` | MD | Team & agents | Solution Architect | N/A |
| Project Memory | `docs/memory.md` | MD | Agents & team | All agents + team | N/A |
| Build Phase Tracker | `docs/stakeholder/BUILD-PHASE-TRACKER.md` | MD → Word | Stakeholders | DevOps / Release Engineer + Delivery Lead | Weekly during build phase |
| Feature Notes | `docs/development/feat-<issue>-<name>.md` | MD | Team | Builder agent on the issue | N/A |
| Decision Logs | `docs/development/decision-<date>-<name>.md` | MD | Team | Agent making the decision | N/A |
| Runbooks | `docs/development/runbook-<name>.md` | MD | Team | DevOps / Release Engineer | N/A |
| Feature Flag Register | `docs/project-foundation/FEATURE-FLAG-REGISTER.md` | MD | Team | Agent introducing the flag | N/A |
| CI Secret Register | `docs/project-foundation/CI-SECRET-REGISTER.md` | MD | Team | DevOps / Release Engineer | N/A |
| Release Evidence Pack | `docs/development/release-evidence-<release-id>.md` | MD | Team & auditors | DevOps / Release Engineer | Required for Class A/B changes; required for Class C/D/E remote deployments. See `templates/RELEASE-EVIDENCE-PACK-TEMPLATE.md` |

---

## When to Add Documentation

1. During implementation: update docs when behavior, interfaces, configuration, or operations change.
2. When a non-obvious decision is made: record it immediately in a decision log entry.
3. Before opening a PR: ensure docs reflect the final implemented behavior.
4. Before merge: remove stale or contradictory doc text.
5. When `docs/memory.md` content becomes stale: update it before closing the issue.

## Product Design Scope Authority

`docs/release/release-1-capabilities.md` is the authoritative product-design and delivery-scope baseline for Release 1. Product designs, acceptance criteria, build plans, UI flows, and implementation tasks must reference this document when confirming whether a capability is in Release 1 scope.

Working notes such as `docs/working/demo-flow-capabilities.md` and `docs/working/release-1-gap-analysis.md` explain derivation and rationale. They do not override the release capabilities baseline.

---

## Minimum Documentation Types

### Feature Notes
- **When:** See AGENT-RULES.md rule 15 for exact criteria.
- **Template:** `docs/templates/FEATURE-NOTE-TEMPLATE.md`
- **Target length:** ≤ 250 words
- **Content:** Scope, key decisions, verification, follow-ups.

### Decision Logs
- **When:** See AGENT-RULES.md rule 16 for exact criteria.
- **Template:** `docs/templates/DECISION-LOG-TEMPLATE.md`
- **Target length:** ≤ 150 words
- **Content:** Decision, alternatives rejected, rationale, owner.

### Runbooks
- **When:** See AGENT-RULES.md rule 17.
- **Template:** `docs/templates/RUNBOOK-TEMPLATE.md`
- **Content:** Step lists only — no long background sections.

---

## Documentation Impact Classification

Every PR must declare one of these:

| Classification | When to use |
|---|---|
| `no doc impact` | Bug fix, trivial refactor, no behavior or stakeholder-visible change |
| `md update required` | New feature, changed API behavior, new config option, new error code, new environment variable |
| `docx republish required` | Requirements baseline change, architecture change visible to stakeholders |
| `both required` | Combined behavior and stakeholder-visible change |

---

## Word Document Policy

Published Word documents must state:
- Source Markdown file(s)
- Owner
- Version
- Published date
- Status: `Draft`, `For Review`, `Approved`, or `Superseded`

Agents maintain Markdown continuously. Word publication happens at review, approval, release, audit, or stakeholder checkpoint.

Do not create or update Word documents for minor edits that do not materially change stakeholder understanding.

---

## Diagram Governance

When diagrams exist in `design/drawings/`:
- Maintain a mapping: each diagram file → source docs/sections → last verified date
- When a design decision changes, flag diagrams that need refresh
- Diagrams should be regenerated from authoritative doc sections, not maintained independently

---

## Governance-Safe Edits

For any edit inside `docs/project-foundation/`:
1. Propose the diff as a PR — do not apply directly
2. At least one other team member must review and approve
3. If the change affects agent behavior, announce it to the whole team before merging
