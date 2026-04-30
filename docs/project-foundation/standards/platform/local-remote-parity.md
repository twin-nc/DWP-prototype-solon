---
id: STD-PLAT-008
title: Local/Remote Environment Parity
status: active
owner: DevOps / Release Engineer
applies-to: All environments
---

# STD-PLAT-008 — Local/Remote Environment Parity

## Purpose

The local development environment must be functionally equivalent to remote environments in all respects that affect application behavior. When local and remote diverge silently, bugs appear only on remote and cannot be reproduced locally — this is one of the most expensive debug patterns in software delivery.

This standard defines what parity means, what divergences are acceptable, and how divergences must be managed.

---

## Rule Set

### PLAT-008-R1 — Parity statement required before first remote deployment

Before any service is deployed to a remote environment for the first time, a parity statement must exist in `docs/project-foundation/remote-environment-spec.md §6`. It must document every known difference between local and remote configuration.

### PLAT-008-R2 — Config shape must match

The shape (keys, structure) of configuration must be identical between local and remote. Values may differ; keys must not. A config key that exists only in one environment is a parity failure.

**Acceptable:** `DATABASE_URL=jdbc:postgresql://localhost:5432/db` (local) vs `DATABASE_URL=<RDS endpoint>` (remote). Same key, different value.  
**Not acceptable:** `FEATURE_FLAG_X` present in remote config but absent from local `.env.example` and `compose.yml`.

### PLAT-008-R3 — Service dependency topology must match

All services and dependencies that run in remote must also run locally — either as a real instance or a documented, justified substitute (e.g., a mock for an external third-party API). Absent dependencies must be explicitly acknowledged, not silently omitted.

### PLAT-008-R4 — Log format must match

Application logs must use the same format and structure locally and remotely. A log line that appears in production must be producible locally. This is required for local reproduction of remote issues.

### PLAT-008-R5 — Auth flow must be testable locally

The authentication and authorisation flow used in remote environments must be fully exercisable locally. Teams must not rely on "skip auth locally" patterns that cannot be disabled for debugging.

### PLAT-008-R6 — Documented divergences only

Any divergence from rules R2–R5 must be documented in `remote-environment-spec.md §6` with a rationale. Undocumented divergences are parity failures regardless of intent.

---

## Parity Statement Format

Each divergence documented in `remote-environment-spec.md §6` must include:

| Config Item | Local Value | Remote Value | Rationale |
|---|---|---|---|
| `CONFIG_KEY` | `local-value` | `remote-value` | Why they differ |

If there are no divergences, state: `No divergences — local and remote configs are functionally equivalent.`

---

## Enforcement

- DevOps / Release Engineer owns the parity statement and must review it before every remote deployment.
- The Remote Deployment Readiness Gate (Gate C) requires a current parity statement before remote promotion is approved.
- Any PR that changes `compose.yml`, Helm charts, or environment configuration must include a parity review note in the PR description.

---

## Common Parity Failures (lessons learned)

| Failure Pattern | Consequence | Prevention |
|---|---|---|
| Feature flag enabled remotely, absent locally | Bug only reproducible on remote; agents cannot diagnose without remote log access | Register all feature flags in `FEATURE-FLAG-REGISTER.md`; ensure all flags are present in local config |
| Auth skipped locally | Auth-related bugs appear only on remote | Run local auth stack (e.g., Keycloak in Compose) from day one |
| Log format differs (plain locally, JSON remotely) | Log queries that work on Grafana fail locally | Set JSON log format in both environments from day one |
| External service mocked locally without documentation | Mock diverges from real service over time | Document every mock; include a link to the real service contract |
| Helm chart updated without updating `compose.yml` | Environment-specific bug appears only after Helm deployment | Treat `compose.yml` and Helm chart as a pair; update together |

---

## Related Documents

- `templates/REMOTE-ENVIRONMENT-SPEC-TEMPLATE.md` — where the parity statement lives
- `REMOTE-DEPLOYMENT-READINESS-GATE.md` — Gate C enforces this standard
- `standards/operations/observability-and-signal-to-noise.md` — log format requirements
- `WAYS-OF-WORKING.md §GitOps and Remote Deployment` — Helm/GitOps change protocol
