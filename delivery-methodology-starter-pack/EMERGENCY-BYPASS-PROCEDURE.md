# Emergency Bypass Procedure

## Purpose
This document defines the break-glass procedure for pushing directly to `main` when normal PR flow would cause greater damage than the defect itself.

Direct pushes to `main` are **never routine**. This procedure exists for genuine production emergencies only.

---

## Who Can Use This

Repo admins only. GitHub branch protection enforces this — non-admins cannot push directly to `main` regardless of this document.

If the person who needs to make the fix is not a repo admin, they must pair with one.

---

## When It Applies

All three conditions must be true:

1. The defect is **actively causing or will imminently cause** production data loss, incorrect legal/financial outcomes, or a complete service outage.
2. The fix is **known and targeted** — not exploratory. You know what to change.
3. Normal PR cycle **cannot complete in time** — for example, CI takes 20 minutes and the production SLA breach window is shorter.

### What is NOT an emergency
- CI is slow but not failing
- A feature is needed urgently but production is not broken
- "We want to avoid review friction on a sensitive change"
- A fix is straightforward and you want to save time

Using the emergency path for non-emergencies is a policy violation.

---

## The Procedure

### During the incident

**Step 1** — Create a local branch named `hotfix/<YYYY-MM-DD>-<short-description>`.
Even if you end up pushing directly, this gives you a recoverable local state.

**Step 2** — Notify one other team member **synchronously** before pushing.
Call, Teams message, or Slack DM. Get their written acknowledgment: *"I acknowledge this emergency push."*
Save the evidence (screenshot, message link, timestamp).

**Step 3** — Apply the targeted fix. Keep the change as small as possible.

**Step 4** — Commit using this mandatory format:
```
HOTFIX(emergency): <description>

Emergency justification: <one sentence — why normal PR is not possible>
Acknowledged by: <name> at <timestamp>
Retroactive issue: to be created within 24h
```

**Step 5** — Push directly to `main`:
```bash
git push origin main
```

**Step 6** — Immediately trigger CI manually.
GitHub → Actions → select the main CI workflow → Run workflow on `main`.
Monitor until green. If CI goes red, you own fixing it immediately (do not leave it red).

---

### Within 24 hours (mandatory — non-negotiable)

**Step 7** — Create a GitHub issue:
- Title: `[HOTFIX] <description> — retroactive for <commit-sha-short>`
- Label: `hotfix`, `emergency-bypass`
- Body: what broke, why normal flow was bypassed, what was changed, any follow-up needed

**Step 8** — Create a retroactive PR from the `hotfix/` branch you kept in Step 1 (or a no-op diff PR if the branch and `main` are now identical).
- Link the retroactive issue
- One team member must comment with an approval acknowledgment
- This PR does not need to be merged — it exists as the review and audit record

**Step 9** — Add a post-incident note to the PR description:
- Root cause of the emergency
- Why normal flow was bypassed
- Whether any documentation or standards need updating as a result
- Follow-up issues created (if any)

**Step 10** — If CI found issues after the push, create fix issues immediately. Do not leave `main` red.

---

## Audit Trail

After the procedure is complete, the following artifacts must exist:

| Artifact | Where |
|---|---|
| Emergency commit on `main` | Git history (commit message with justification and acknowledgment) |
| Acknowledgment evidence | Saved by the person who pushed |
| Retroactive GitHub issue | GitHub issues, labeled `emergency-bypass` |
| Retroactive PR | GitHub PRs, linked to the issue |
| Post-incident note | Retroactive PR description |

---

## What Happens if the Procedure Is Not Followed

A direct push to `main` without following this procedure is a policy violation. Consequences:
- The team lead must be informed immediately
- The change must be reviewed retroactively regardless
- Repeated violations may result in admin rights being removed
- The change must be documented as a deviation if it deviated from any standard

---

## GitHub Configuration Required

Branch protection must be configured before this procedure is meaningful.

In repo Settings → Branches → Protection rule for `main`:

| Setting | Value |
|---|---|
| Require a pull request before merging | ✅ Enabled |
| Require status checks to pass | ✅ Enabled |
| Require branches to be up to date | ✅ Enabled |
| Require at least 1 approving review | ✅ Enabled |
| Restrict who can push to matching branches | ✅ Repo admins only |
| Allow administrators to bypass | ✅ Enabled (this is the break-glass switch) |
| Allow force pushes | ❌ Disabled (even for admins) |
| Allow deletions | ❌ Disabled |

> Force push is always prohibited. Emergency direct push (non-force) is allowed for admins only. The git audit trail is always intact.
