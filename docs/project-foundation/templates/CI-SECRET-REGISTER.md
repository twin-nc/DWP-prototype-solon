# CI/CD Secret Register

**Instructions:** Copy this file to `docs/project-foundation/CI-SECRET-REGISTER.md`.
Add every secret used in GitHub Actions workflows. Update when secrets are added, rotated, or removed.
See standard: `standards/operations/ci-cd-secret-management.md`

---

## GitHub Actions Secrets

| Secret Name | Purpose | Source / Where to Obtain | Rotation Policy | Owner | Last Rotated | Notes |
|---|---|---|---|---|---|---|
| `REGISTRY_USERNAME` | Container registry login | {{source}} | Annual | {{name}} | {{date}} | |
| `REGISTRY_PASSWORD` | Container registry password | {{source}} | Annual | {{name}} | {{date}} | |

---

## Developer Local Environment Variables

These variables must be set in each developer's shell profile. They are never stored in GitHub.

| Variable | Purpose | Where to Obtain | Notes |
|---|---|---|---|
| `GITHUB_PERSONAL_ACCESS_TOKEN` | MCP GitHub server | GitHub → Settings → Developer settings → PATs | Classic token; scopes: repo, read:org |
| `BRAVE_API_KEY` | MCP Brave Search server | https://brave.com/search/api/ | Free tier available |

---

## Rules
1. All secrets used in CI/CD workflows must be listed here.
2. Secrets are never stored in code, config files, or documentation other than this register (by name only — never the value).
3. When a secret is rotated: update the `Last Rotated` date, update the GitHub Actions secret, and notify the team.
4. When a secret expires or is invalidated unexpectedly: the owner must update GitHub Actions within the same working day.
5. Secret names in GitHub Actions must exactly match the names in this register.
