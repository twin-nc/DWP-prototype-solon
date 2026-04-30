# Dev → DevOps Handoff Checklist

Complete this checklist before handing off to the DevOps team. All items must be checked before the DevOps team begins infrastructure configuration, Key Vault integration, TLS, or production-grade Helm work.

**Project:** `{{PROJECT_NAME}}`  
**Completed by:** `{{DEV_LEAD_NAME}}`  
**Date:** `{{YYYY-MM-DD}}`  
**Reviewed by (DevOps):** `{{DEVOPS_NAME}}`  

---

## A — Containerization (STD-PLAT-009)

- [ ] Dockerfile present at repository root
- [ ] Multi-stage build — separate build and runtime stages
- [ ] Both stages use pinned base image tags — no `latest` or floating tags
- [ ] Final image contains only runtime dependencies, not build tools
- [ ] Non-root user explicitly created and switched to before `ENTRYPOINT`
- [ ] Application listens on port 8080 by default, configurable via `PORT` env var
- [ ] `EXPOSE` in Dockerfile matches the actual default listen port
- [ ] SIGTERM handling explicitly configured (graceful shutdown — not assumed)
- [ ] Image builds successfully locally:
  ```bash
  docker build -t <registry>/<project>:<git-sha> .
  ```

---

## B — Local Development Environment (STD-PLAT-010)

- [ ] `docker compose up` starts the full stack — no manual steps beyond copying `.env.example`
- [ ] `depends_on` uses `condition: service_healthy` for all stateful dependencies
- [ ] All services referenced by `depends_on: condition: service_healthy` define a `healthcheck` block
- [ ] PostgreSQL service uses `postgres:16` (or a pinned version agreed with DevOps)
- [ ] Database service healthcheck defined:
  ```yaml
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-postgres}"]
    interval: 5s
    timeout: 5s
    retries: 5
  ```

---

## C — Configuration (STD-PLAT-010)

- [ ] `.env.example` committed to repository root
- [ ] `.env.example` documents every variable the application reads — required and optional
- [ ] Every variable in `.env.example` has a comment explaining its purpose and default
- [ ] `.env` is gitignored
- [ ] All application configuration is read from environment variables — nothing hardcoded in source
- [ ] Application fails fast with a clear error message if a required variable is missing
- [ ] No secrets, credentials, or environment-specific values committed to source control

---

## D — Health Endpoints (STD-OPS-003)

- [ ] `GET /health/live` returns `200` when the process is alive
- [ ] `GET /health/ready` returns `200` when all dependencies (database, etc.) are reachable
- [ ] `GET /health/ready` returns `503` when the database is unreachable
- [ ] Neither endpoint requires authentication
- [ ] Both endpoints respond before database migrations complete (available from process start)

---

## E — Structured Logging (STD-OPS-004)

- [ ] All logs written to `stdout` in JSON format — no file appenders, no plain-text formatters
- [ ] Every log entry includes: `timestamp` (ISO 8601), `level`, `message`, `correlationId`
- [ ] `correlationId` accepted from incoming `X-Correlation-ID` request header if present
- [ ] `correlationId` generated as UUID if header is absent
- [ ] `correlationId` propagated to every log entry for the duration of the request
- [ ] `LOG_LEVEL` environment variable controls minimum log level without redeployment
- [ ] No sensitive data (passwords, tokens, PII) appears in any log entry

---

## F — Database (STD-PLAT-006)

- [ ] Database is PostgreSQL — no other database engine
- [ ] Database migrations run automatically on application startup — no manual migration steps
- [ ] Migrations are immutable (no modification of applied migrations)

---

## G — Helm Chart (STD-PLAT-011)

- [ ] `/chart` directory present in the repository
- [ ] Chart contains exactly these five templates and no others:
  - [ ] `Deployment.yaml`
  - [ ] `Service.yaml`
  - [ ] `ConfigMap.yaml`
  - [ ] `Ingress.yaml`
  - [ ] `ServiceAccount.yaml`
- [ ] No unused default templates left over from `helm create`
- [ ] `Deployment.yaml` uses `image`, `port`, and `env` from `values.yaml` — nothing hardcoded
- [ ] `Deployment.yaml` includes liveness and readiness probes pointing to `/health/live` and `/health/ready`
- [ ] `Service.yaml` exposes port 8080 from `values.yaml`
- [ ] `ConfigMap.yaml` present with all non-sensitive config values
- [ ] `Ingress.yaml` present — no TLS configured (DevOps will handle TLS)
- [ ] `ServiceAccount.yaml` present — no annotations (DevOps will handle workload identity)
- [ ] `values.yaml` contains all required fields: `image`, `replicaCount`, `service.port`, `ingress`, `env`, `secrets`
- [ ] Every environment variable the application reads is present in `values.yaml` under `env`
- [ ] Sensitive values (database credentials, API keys) are **empty** in `values.yaml` — not hardcoded
- [ ] `helm lint ./chart` passes with no errors
- [ ] `helm template ./chart` renders without errors

---

## H — Documentation

- [ ] `README.md` present at repository root
- [ ] README covers Prerequisites with exact tool versions
- [ ] README covers Getting Started (step-by-step from clone to running)
- [ ] README covers how to access portal, API, and health endpoints locally
- [ ] README documents default local credentials (any seeded accounts)
- [ ] README documents how to stop the stack (`docker compose down`)
- [ ] README documents how to reset the database (`docker compose down -v`)

---

## Sign-off

Record this sign-off in the linked GitHub issue or PR before DevOps begins infrastructure work:

```
## Dev → DevOps Handoff Sign-off

- A (Containerization):      ✅ Complete / ❌ Incomplete — [reason or deferral]
- B (Local dev environment):  ✅ Complete / ❌ Incomplete — [reason or deferral]
- C (Configuration):          ✅ Complete / ❌ Incomplete — [reason or deferral]
- D (Health endpoints):       ✅ Complete / ❌ Incomplete — [reason or deferral]
- E (Structured logging):     ✅ Complete / ❌ Incomplete — [reason or deferral]
- F (Database):               ✅ Complete / ❌ Incomplete — [reason or deferral]
- G (Helm chart):             ✅ Complete / ❌ Incomplete — [reason or deferral]
- H (Documentation):          ✅ Complete / ❌ Incomplete — [reason or deferral]

Handoff approved for DevOps: ✅ / ❌
Dev Lead sign-off:   [name] — [date]
DevOps sign-off:     [name] — [date]
```

---

## What the DevOps team handles after this handoff

The following are explicitly **not** the dev team's responsibility and will be configured by the DevOps team:

- Key Vault / secret store integration
- TLS configuration in Ingress
- Workload identity annotations in ServiceAccount
- Production values in `values.yaml`
- GitOps sync configuration
- Kubernetes namespace, RBAC, and network policy
- Container registry credentials and image pull secrets
- Monitoring, alerting, and log aggregation pipeline configuration

---

## Related Standards

- `standards/platform/containerization.md` — STD-PLAT-009
- `standards/platform/local-dev-environment.md` — STD-PLAT-010
- `standards/operations/health-endpoints.md` — STD-OPS-003
- `standards/operations/structured-logging.md` — STD-OPS-004
- `standards/platform/database-migration-standard.md` — STD-PLAT-006
- `REMOTE-DEPLOYMENT-READINESS-GATE.md` — the gate that runs after this handoff
