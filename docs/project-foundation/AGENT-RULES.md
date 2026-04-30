# Agent Rules

## Purpose
Mandatory operating rules for all project agents. Your role file is self-contained for the rules you need to act — read it. If your task touches architecture, requirements, or standards, read the relevant source document before acting. Read `docs/memory.md` when you need project context — not as a ritual, but when your task requires it.

---

## Mandatory Rules

### Preparation
1. Your role file is self-contained for the rules you need to act. Read it. If your task touches architecture, requirements, or standards, read the relevant source document before acting — not before every task.
2. `docs/memory.md` is the current project state. Read it when you need project context. Do not read it as a ritual — read it when your task requires it.
3. If unsure about requirements or scope, ask one focused clarifying question before implementation. Do not ask multiple questions in sequence — consolidate.

**Release 1 scope authority:** `docs/release/release-1-capabilities.md` is the authoritative product-design and delivery-scope baseline for Release 1. Read it before designing product behaviour, writing acceptance criteria, planning Release 1 work, or implementing Release 1 features.

### Ambiguity and missing inputs
4. If a Required Input is missing or ambiguous, declare the gap explicitly before proceeding. Do not resolve a missing input with an undeclared assumption. Silently filled gaps are the most damaging handoff failure mode — they propagate baked-in assumptions that neither party declared.

### Scope and workflow
5. Stay within assigned scope. Do not modify unrelated files.
6. Follow issue-first workflow: no work without a linked GitHub issue.
7. Never push directly to `main`. Use the branch + PR flow. Emergency exceptions follow `EMERGENCY-BYPASS-PROCEDURE.md`.
8. Before the final push for merge, sync with latest `origin/main`. If `main` changed, rebase and rerun all tests before pushing.

### Hook discipline
9. Hook bypass is prohibited. Never use `--no-verify`, `-n`, or `-c core.hooksPath=<override>` with any git command. If a hook fails during a commit, investigate and fix the underlying issue — do not bypass. If the hook is failing incorrectly, raise it as a separate issue. The `block-no-verify` hook enforces this at the tool level.

### Emergency bypass
10. If you perform an emergency direct push to `main`, you are personally responsible for:
    - Notifying a second team member synchronously before pushing
    - Creating a retroactive GitHub issue within 24 hours
    - Creating a retroactive PR within 24 hours
    - Triggering CI manually immediately after the push
    Non-compliance converts the emergency bypass into a policy violation.

### Critic and reviewer independence
11. Critic and reviewer roles (Design Critic, Code Reviewer, Security Reviewer) must be invoked in a fresh context window, separate from the session that produced the work being reviewed. Pass only the artifact and evaluation criteria — not the reasoning chain that produced it.

    **What to pass to a critic:**
    - The artifact (design document, code, contract)
    - The evaluation criteria (relevant requirements, applicable standards)

    **What NOT to pass:**
    - Why this approach was chosen over alternatives
    - Prior conversation context
    - Any framing that indicates a preferred outcome

12. Reviewer and Critic roles (Code Reviewer, Design Critic, Security Reviewer) are read-only unless explicitly asked to edit files.
13. When providing feedback, label each finding by severity and include a concrete next action.

### Critic invocation protocol
14. When a producer role completes work that requires a critic or reviewer pass, it must output a ready-to-use invocation prompt for the human to carry to a new context window. The prompt must include the artifact and evaluation criteria only. The producing role must signal urgency using one of two headers:

    `## [BLOCKING] Design Critic Review Required`
    `## [ADVISORY] Code Review Recommended`

    `[BLOCKING]` means the next role in the chain cannot proceed without the critic output. `[ADVISORY]` means work can continue but the finding must be resolved before merge.

### Documentation
15. Assess documentation impact before closing any task.
16. If work changes requirements, architecture, integrations, operations, governance, or stakeholder-visible behavior — update the relevant Markdown source or explicitly state why no update is needed.
17. If human stakeholders need a downloadable document, or if the current published Word document would be misleading, flag or perform Word republication according to project ownership.
18. Escalate when documentation ownership, location, or publication responsibility is unclear.

### When to create a Feature Note
19. Create a Feature Note from `docs/templates/FEATURE-NOTE-TEMPLATE.md` and save to `docs/development/feat-<issue-number>-<short-name>.md` if **any** of the following are true:
    - More than 50 lines of new logic added
    - A new external dependency introduced
    - A state machine or observable API behavior changed
    - A new environment variable, configuration key, or feature flag added
    - The change required non-obvious implementation choices

### When to create a Decision Log entry
20. Create a Decision Log entry from `docs/templates/DECISION-LOG-TEMPLATE.md` and save to `docs/development/decision-<YYYY-MM-DD>-<short-name>.md` if **any** of the following are true:
    - Two or more implementation alternatives were seriously considered
    - A deviation from a project standard was made or considered
    - An irreversible architectural or schema choice was made
    - A third-party tool or library was selected over alternatives
    - The team explicitly agreed on something that future agents might reverse without this record

### When to create a Runbook
21. Create or update a Runbook from `docs/templates/RUNBOOK-TEMPLATE.md` and save to `docs/development/runbook-<name>.md` for any new or changed operational procedure (setup, deployment, rollback, secret rotation, data migration).

### Project memory
22. Update `docs/memory.md` whenever:
    - A named architecture or technology decision is made
    - A requirement baseline version changes
    - The current work phase or status shifts
    - A known constraint or risk changes

    `docs/memory.md` retention rules:
    - Remove a decision entry when it is superseded — do not accumulate historical decisions
    - Keep only what is still decision-relevant and not derivable from reading the current code or git history
    - Maximum 150–200 lines — agents stop reading documents above this threshold in practice
    - If it reads like a journal, prune it

### Design Critic gate
23. Before any Solution Architect decision is locked in a Decision Log entry or issue comment, a Design Critic pass is required. For low-complexity decisions, a brief "no material risk found" statement from a Design Critic pass is sufficient. For Class A changes or irreversible architecture choices, the Design Critic output must be explicit and linked from the Decision Log entry. The SA must not self-review. See rule 11 for fresh context window requirement.

### Continuous traceability
24. Trace map updates happen in the **same cycle** as the change they describe. Do not defer traceability work to release time. A merged PR that introduces new behavior without a corresponding trace link is incomplete. The Traceability Steward role exists to enforce this — invoke it as part of every non-trivial feature.

### Conflict escalation
25. When two agents disagree on approach:
    - Design or architecture disputes → escalate to Solution Architect (after Design Critic pass)
    - Requirements or domain disputes → escalate to Business Analyst (and domain expert if applicable)
    - Standards compliance disputes → escalate to the standard's named owner
    Do not resolve disputes by silence or by the more assertive agent proceeding.

### Research first
26. Before implementing new logic, search the existing codebase for existing patterns, utilities, or similar implementations. Check library documentation for built-in solutions. Do not write new code that duplicates existing capability.

---

## Output Minimum

Every agent response must include:
1. What was done.
2. Files impacted (if any).
3. Verification or review basis.
4. Remaining risk or open question.
5. Documentation impact classification (`no doc impact` / `md update required` / `docx republish required` / `both required`).
6. Documentation updated, required, or explicitly deferred.

---

## Handoff Declaration (producer roles)

When handing off to the next role, include this block:

```
## Handoff Declaration
- **Completed:** [what was done]
- **Files changed:** [list]
- **ACs satisfied:** [which ones, explicitly]
- **ACs not satisfied:** [which ones, why, what is needed]
- **Assumptions made:** [any declared, explicit assumptions]
- **Missing inputs encountered:** [any gaps found during work]
- **Next role:** [who picks this up]
- **What they need:** [what the next role must read to start without ambiguity]
```
