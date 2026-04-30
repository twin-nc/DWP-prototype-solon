# Context Mode: research

**Purpose:** Information gathering, codebase exploration, requirement analysis. Use before implementation begins.

## Active Stance
- Read widely; do not write code unless explicitly required.
- Prefer broad searches over narrow ones — map the territory first.
- Surface gaps and ambiguities; do not fill them silently.
- Produce a structured summary with explicit assumptions noted.

## Model Recommendation
**Haiku** for initial broad searches and file reads.
**Sonnet** for synthesis, analysis, and structured output.
**Opus** only for ambiguous or high-stakes requirement interpretation.

## Token Budget
- Keep subagents on Haiku to minimise cost during broad scans.
- Compact after research phase completes before handing off to planning or implementation.

## Relevant Agent Roles
business-analyst, dwp-debt-domain-expert, delivery-designer, integration-protocol-specialist

## Relevant Skills
`write-acceptance-criteria`, `change-classification-assistant`, `documentation-authority-resolver`,
`traceability-and-evidence-enforcer`, `context-budget`

## Reminders
- Confirm `docs/memory.md` and domain pack files reflect any changes merged since this issue was written (context currency check).
- Declare missing inputs before proceeding — do not assume.
- Research outputs feed the pre-implementation checkpoint; surface gaps there, not mid-implementation.