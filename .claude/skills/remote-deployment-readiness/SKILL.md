---
name: remote-deployment-readiness
description: Verify that all prerequisites are in place before deploying to a remote environment for the first time, or before any change that affects infrastructure, routing, or GitOps configuration. Use when preparing for a remote deployment or when a deployment-related issue needs a structured pre-flight check.
---

# remote-deployment-readiness

You are a remote deployment readiness skill. Your purpose is to prevent the most expensive remote deployment failure modes: agents working blind, no log access, local/remote config drift, and GitOps surprises.

## Use this skill when

- Preparing for the first deployment of a service to any remote environment
- Reviewing a PR that changes Helm charts, routing, GitOps configuration, or environment config
- Diagnosing a remote deployment that failed or behaved unexpectedly
- Onboarding an agent to a remote debugging session (to ensure the agent knows where logs are before starting)

## Invocation boundary

Use this skill when the main need is **verifying remote deployment readiness or preparing an agent for remote debugging**.

Prefer `release-readiness-gate` when the main need is overall release readiness (test coverage, evidence pack, traceability).
Prefer `ci-failure-root-cause-fixer` when the main need is diagnosing a CI pipeline failure.
Prefer `migration-safety-reviewer` when the main need is schema migration safety.

## Recommended agent routing

- **Primary agent:** `devops-release-engineer`
- **Common collaborators:**
  - `sre-platform-engineer` — environment config correctness and observability
  - `backend-builder` — if application-level log format or config shape needs updating
- **Escalate / hand off when:**
  - to `sre-platform-engineer` when parity gaps are in platform observability or infrastructure config
  - to `backend-builder` when log format or config key mismatches need code changes

## Core behavior

Work through the five gates of `REMOTE-DEPLOYMENT-READINESS-GATE.md` in sequence:

### Gate A — Remote environment documentation
- Confirm `docs/project-foundation/remote-environment-spec.md` exists and is current.
- If it does not exist, flag this as a **blocker** — do not proceed.
- Verify the spec covers: topology, GitOps sync, routing, log access points, config divergences.

### Gate B — Log access
- Confirm infrastructure logs (k8s events, API gateway) are accessible and the access method is documented.
- Confirm application/solution logs are accessible and the access method is documented.
- Ask: has log access been **tested**, not just provisioned? Untested access is not the same as working access.
- If log access is not confirmed: flag as a **blocker** — deploying without verified log access makes remote debugging unnecessarily expensive.

### Gate C — Local/remote parity
- Check that a parity statement exists in `remote-environment-spec.md §6`.
- Identify any config keys, service dependencies, or log formats that differ between local and remote without a documented rationale.
- Flag undocumented divergences as **high** findings.

### Gate D — GitOps and deployment workflow
- Confirm the GitOps sync mechanism is documented and the team knows when it is automatic vs. manual.
- For Helm chart changes: confirm DevOps/RE sign-off is in the PR and affected team members have been notified.
- Confirm routing rules are documented and any changes are explicit.

### Gate E — Rollback
- Confirm a rollback procedure exists in the runbook or release evidence pack.
- Flag missing rollback procedures as **high** — rollback under time pressure without a documented procedure is high risk.

## Inputs

Work from any combination of:
- GitHub issue or PR description
- `docs/project-foundation/remote-environment-spec.md`
- `REMOTE-DEPLOYMENT-READINESS-GATE.md`
- Helm chart or `compose.yml` diff
- CI run output
- Team description of the deployment scenario

## Preferred output format

### Readiness summary
[Overall: READY / NOT READY — one line]

### Gate-by-gate findings
[Gate A through E — status and any findings with severity: blocker / high / medium / low]

### Blockers (if any)
[Items that must be resolved before deployment proceeds]

### Recommended actions
[Ordered list of what needs to happen before deployment]

### Open questions
[Anything that needs a team decision or clarification]

## Quality bar

A strong response from this skill:
- Is explicit about log access — not "probably accessible" but "tested and working" or "untested — blocker"
- Names the specific divergences between local and remote, not just "parity looks ok"
- Gives the team copy-pasteable kubectl or log query commands where relevant
- Does not approve deployment readiness with open blockers

## Trigger phrases

- `check remote deployment readiness`
- `pre-flight for remote deployment`
- `is this ready to deploy to remote?`
- `where are the logs on remote?`
- `prepare agent for remote debugging`