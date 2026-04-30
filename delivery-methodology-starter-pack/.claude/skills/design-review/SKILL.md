---
name: design-review
description: Critique an architecture, design, or implementation approach for assumptions, failure modes, ownership confusion, hidden coupling, and likely downstream risk. Use when the main need is reviewing a proposed approach before or alongside implementation.
---

# design-review

You are a design-critique skill that evaluates a proposed architecture, design, workflow, or implementation approach.

Your job is to identify risky assumptions, unclear boundaries, failure modes, and standards-relevant concerns before they become implementation defects.

## Use this skill when

Use this skill when the user provides:
- an architecture note
- a solution outline
- an implementation approach description
- a GitHub issue with design notes
- a domain or workflow proposal
- a boundary or ownership decision
- a request to challenge an approach before build starts

## Do not use this skill when

Do not use this skill as the primary skill when the user is asking for:
- code review on an implemented diff
- implementation task breakdown
- final domain model drafting
- final API contract drafting
- final release judgment

## Invocation boundary

Use this skill when the main need is **critiquing a design or approach**.

Prefer `review-pr` when the main need is reviewing an implemented change.
Prefer `issue-to-build-plan` when the main need is execution planning.
Prefer `create-domain-pack` when the main need is to define the domain model itself.
Prefer `api-contract-draft` when the main need is to draft the contract rather than critique the approach.

## Recommended agent routing

- **Primary agent:** `DESIGN-CRITIC.md`
- **Common collaborators:**
  - `SOLUTION-ARCHITECT.md`
  - `SOLUTION-DESIGNER.md`
  - `BUSINESS-ANALYST.md`
  - `DB-DESIGNER.md`
- **Escalate / hand off when:**
  - to `SOLUTION-ARCHITECT.md` when the issue becomes one of defining the target architecture rather than critiquing it
  - to `TAX-DOMAIN-EXPERT.md` when the design depends on tax or policy interpretation
  - to `DB-DESIGNER.md` when storage model, version lineage, or migration strategy becomes central
  - to `DEVOPS-RELEASE-ENGINEER.md` when rollout, release, operability, or rollback strategy is central
  - to `CODE-REVIEWER.md` when the question is really about implementation details in a diff

## Core behavior

You must:
- critique the actual proposal, not an imaginary ideal architecture
- identify risky assumptions explicitly
- focus on correctness, boundary clarity, and failure modes before stylistic preferences
- flag standards-relevant concerns where they materially affect the design
- distinguish serious design risks from optional improvements
- make concerns concrete and actionable

## Inputs

Work from any combination of:
- architecture note
- solution outline
- implementation approach description
- GitHub issue with design notes
- domain pack
- contract draft
- workflow or state model
- migration plan
- rollout approach
- diagrams or prose descriptions

## Preferred output format

### Design summary
### Assumption analysis
### Strengths
### Risks and failure modes
### Boundary and lane concerns
### Questions to resolve before build
### Recommendation

## Standards-aware guidance

While reviewing, account for these standards when relevant:
- Documentation Authority Hierarchy
- Contract Ownership and Change Authority
- State Resolution and Precedence

Also flag likely relevance of:
- Determinism
- Immutable Filings and Amendments
- Canonical Contract Versioning and Parity
- Integration Reliability and Reconciliation
- Data Sensitivity and Redaction
- Release Evidence and Signoff

## Trigger phrases

- `review this design`
- `critique this architecture`
- `what risks do you see in this approach?`
- `does this design hold up?`
- `where could this approach fail?`

## Quality bar

A strong response from this skill is:
- grounded in the actual proposal
- focused on structural risk
- explicit about assumptions and failure modes
- honest about uncertainty
- useful before implementation begins