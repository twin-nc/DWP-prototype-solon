# Skill Development Guide

How to create, test, and maintain skills in this starter pack.

---

## When a Skill Warrants Creation

Create a skill when **all three** of the following are true:

1. The workflow is repeated across issues or agents (not a one-off).
2. The workflow has decision logic or sequencing that is easy to get wrong without guidance.
3. A human or agent invoking it by name is more reliable than re-deriving it from memory each time.

Do **not** create a skill for:
- A single agent role's internal behaviour (put that in the agent file).
- A one-time migration or clean-up (put that in a runbook).
- A checklist that belongs in a template (put it there).

---

## Skill File Structure

Every skill lives in its own directory under `.claude/skills/<skill-name>/SKILL.md`.

### Required Frontmatter

```yaml
---
name: skill-name            # kebab-case, matches directory name
description: >              # one sentence — shown in README and AGENTS.md index
  What this skill does and when to invoke it.
invocation: /skill-name     # slash command users type
inputs:                     # list of what the skill needs to run
  - name: issue_url
    required: true
    description: GitHub issue URL
outputs:                    # what the skill produces
  - name: build_plan
    description: Structured plan with role assignments
roles:                      # which agent roles this skill coordinates
  - solution-architect
  - backend-builder
---
```

All frontmatter fields except `roles` and `inputs` are required. Omit `inputs` only if the skill takes no inputs.

### Body Sections (required order)

```markdown
# <Skill Name>

## Purpose
One paragraph. What problem does this skill solve and why does it exist.

## When to Use
Bullet list of trigger conditions. Be specific — avoid "when needed."

## Inputs
Table: | Input | Required | Description |

## Steps
Numbered workflow. Each step names the role responsible, the action, and the output artifact.

## Output Contract
What the skill produces on success. Reference file paths or comment formats where applicable.

## Guardrails
What this skill must never do. At least one item.

## Example Invocation
A realistic example showing inputs and expected output shape.
```

---

## Placement Rules

- Directory: `.claude/skills/<skill-name>/SKILL.md`
- Name must be kebab-case and unique across the index.
- The directory name must match the `name` frontmatter field exactly.
- No other files are required in the directory, but supporting assets (rubrics, templates referenced by the skill) may live alongside `SKILL.md`.

---

## Testing Expectations

Before a new skill is merged:

1. **Dry run** — invoke the skill against a real or synthetic issue and confirm each step produces the expected output.
2. **Guardrails check** — deliberately violate one guardrail condition and confirm the skill surfaces an error rather than proceeding silently.
3. **README entry** — confirm the skill appears in `.claude/skills/README.md` with an accurate one-line description.
4. **AGENTS.md sync** — run `scripts/sync-claude-to-codex.ps1` (or confirm pre-commit hook fires) and verify the skill appears in `AGENTS.md`.

A skill that is not in README.md and AGENTS.md is not discoverable and is considered incomplete.

---

## Codex Mirror

Skills are mirrored to `AGENTS.md` via `scripts/sync-claude-to-codex.ps1`.

Rules:
- Never edit `AGENTS.md` manually — it is generated.
- The pre-commit hook regenerates `AGENTS.md` on every commit that touches `.claude/agents/` or `.claude/skills/`.
- If the hook is not installed, run the sync script before raising a PR.
- A PR that adds or modifies a skill without updating `AGENTS.md` is incomplete.

---

## Updating an Existing Skill

1. Edit the `SKILL.md` file.
2. If the `description` or `invocation` fields change, update `README.md` to match.
3. Run the sync script or confirm the pre-commit hook fires.
4. If the skill's input/output contract changes, notify any agents or runbooks that reference it.

---

## Retiring a Skill

1. Remove the directory from `.claude/skills/`.
2. Remove the entry from `README.md`.
3. Run the sync script.
4. Search for references to the skill name in agent files, runbooks, and WAYS-OF-WORKING.md — update or remove them.
5. Add a one-line note to the decision log explaining why it was retired.
