# Install Manifest

This manifest defines exactly where each asset from this starter pack lands in a new project repository. Follow it during Step 2 of `getting-started-guide.md`.

Assets go to **four separate locations** in the target repo. Copying the entire pack into a single folder is incorrect and will break `.claude/` and `.github/` assets.

---

## Target 1 — docs/project-foundation/

Copy all files and directories from the pack root **except** `.claude/` and `.github/` to `docs/project-foundation/` in the new repo.

| Pack asset | Destination |
|---|---|
| `AGENT-OUTLINES.md` | `docs/project-foundation/AGENT-OUTLINES.md` |
| `AGENT-RULES.md` | `docs/project-foundation/AGENT-RULES.md` |
| `CLAUDE-MD-TEMPLATE.md` | `docs/project-foundation/CLAUDE-MD-TEMPLATE.md` *(also fill in and copy to repo root as `CLAUDE.md`)* |
| `DOCUMENTATION-POLICY.md` | `docs/project-foundation/DOCUMENTATION-POLICY.md` |
| `EMERGENCY-BYPASS-PROCEDURE.md` | `docs/project-foundation/EMERGENCY-BYPASS-PROCEDURE.md` |
| `getting-started-guide.md` | `docs/project-foundation/getting-started-guide.md` |
| `INSTALL-MANIFEST.md` | `docs/project-foundation/INSTALL-MANIFEST.md` |
| `PRE-IMPLEMENTATION-CHECKPOINT.md` | `docs/project-foundation/PRE-IMPLEMENTATION-CHECKPOINT.md` |
| `REMOTE-DEPLOYMENT-READINESS-GATE.md` | `docs/project-foundation/REMOTE-DEPLOYMENT-READINESS-GATE.md` |
| `REPO-SETUP-CHECKLIST.md` | `docs/project-foundation/REPO-SETUP-CHECKLIST.md` |
| `SKILLS-GUIDE.md` | `docs/project-foundation/SKILLS-GUIDE.md` |
| `WAYS-OF-WORKING.md` | `docs/project-foundation/WAYS-OF-WORKING.md` |
| `standards/` *(entire directory)* | `docs/project-foundation/standards/` |
| `templates/` *(entire directory)* | `docs/project-foundation/templates/` |

> Do **not** copy `README.md` (this pack's README) — the new project will have its own README at the repo root.

---

## Target 2 — repo root .claude/

Copy the `.claude/` directory from the pack to `.claude/` at the **repo root** of the new project. These files must live at repo root — Claude Code looks for `.claude/` relative to the repo root, not inside `docs/`.

| Pack asset | Destination |
|---|---|
| `.claude/agents/` *(entire directory)* | `.claude/agents/` *(at repo root)* |
| `.claude/skills/` *(entire directory)* | `.claude/skills/` *(at repo root)* |

After copying, customise the agent set in `.claude/agents/` to match the project's agreed roles (see `AGENT-OUTLINES.md` and the customisation table in the pack `README.md`).

---

## Target 3 — repo root .github/

Copy the `.github/` files from the pack to `.github/` at the **repo root** of the new project. GitHub only reads `.github/` at the repo root.

| Pack asset | Destination |
|---|---|
| `.github/pull_request_template.md` | `.github/pull_request_template.md` *(at repo root)* |
| `.github/dependabot.yml` | `.github/dependabot.yml` *(at repo root)* |

After copying, edit `dependabot.yml` to uncomment and configure the correct package ecosystems for the project's tech stack.

---

## Target 4 — repo root (CLAUDE.md)

| Action | Destination |
|---|---|
| Copy `CLAUDE-MD-TEMPLATE.md`, fill in all `{{PLACEHOLDER}}` values, delete the instructions header | `CLAUDE.md` *(at repo root)* |

`CLAUDE.md` is the first file Claude Code reads in every session. It must be accurate, complete, and at the repo root before any agent work begins. The copy at `docs/project-foundation/CLAUDE-MD-TEMPLATE.md` is the blank template for reference.

---

## Shell commands (reference — adjust paths for your environment)

```bash
# From inside your new (empty) repo
PACK="/path/to/delivery-methodology-starter-pack"

# Target 1 — docs/project-foundation/
mkdir -p docs/project-foundation
rsync -av --exclude='.claude' --exclude='.github' --exclude='README.md' \
  "$PACK/" docs/project-foundation/

# Target 2 — .claude/ at repo root
cp -r "$PACK/.claude" .

# Target 3 — .github/ at repo root
mkdir -p .github
cp "$PACK/.github/pull_request_template.md" .github/
cp "$PACK/.github/dependabot.yml" .github/

# Target 4 — CLAUDE.md at repo root
cp "$PACK/CLAUDE-MD-TEMPLATE.md" CLAUDE.md
# Edit CLAUDE.md and fill in all {{PLACEHOLDER}} values
```

---

## Verification checklist after install

- [ ] `docs/project-foundation/WAYS-OF-WORKING.md` exists
- [ ] `docs/project-foundation/standards/` directory exists with subdirectories
- [ ] `docs/project-foundation/templates/` directory exists
- [ ] `.claude/agents/` exists at repo root (not inside `docs/`)
- [ ] `.claude/skills/` exists at repo root (not inside `docs/`)
- [ ] `.github/pull_request_template.md` exists at repo root
- [ ] `CLAUDE.md` exists at repo root with all placeholders filled in
- [ ] No `.claude/` or `.github/` directory inside `docs/project-foundation/`
