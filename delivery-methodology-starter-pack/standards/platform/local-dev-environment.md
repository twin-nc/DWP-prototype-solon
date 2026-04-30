---
id: STD-PLAT-010
title: Local Development Environment
status: Approved
owner: DevOps / Release Engineer
applies_to: All projects with containerised services
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - platform/local-remote-parity.md
  - platform/containerization.md
  - operations/health-endpoints.md
last_changed: 2026-04-13
---

## Purpose

Every developer MUST be able to run the full application stack locally with a single command and without any cloud credentials or external network connectivity. This is a prerequisite for reproducible development, local debugging, and compliance with STD-PLAT-008 (local/remote parity).

---

## Rule Set

### PLAT-010-R1 — docker-compose.yml required at repository root

A `docker-compose.yml` MUST be present at the repository root. It MUST define every service the application depends on so that `docker compose up` starts the complete working stack.

Services that MUST be defined:
- The application itself (or all application services if the project has multiple)
- Every external dependency the application requires to function (database, cache, message broker, etc.)

### PLAT-010-R2 — Full stack must start with one command

```bash
docker compose up
```

This single command MUST bring up the entire working stack. No additional manual steps are permitted after copying `.env.example` to `.env` and filling in any required values. If any step beyond this is required, it is a violation of this standard.

### PLAT-010-R3 — No cloud credentials or external connectivity required

The local stack MUST function entirely offline. It MUST NOT require:

- Cloud provider credentials (AWS, Azure, GCP, etc.)
- VPN access
- External API keys for core application functionality

Where an external third-party service is genuinely unavoidable, it MUST be represented by a local mock, and the mock MUST be documented in `docs/project-foundation/remote-environment-spec.md §3`.

### PLAT-010-R4 — depends_on with condition: service_healthy required

Application services MUST NOT start before their dependencies are healthy. `depends_on` MUST use `condition: service_healthy`. The default `condition: service_started` is prohibited for database and stateful dependencies.

```yaml
depends_on:
  db:
    condition: service_healthy
```

Every service listed in `depends_on` with `condition: service_healthy` MUST define a `healthcheck` block:

```yaml
db:
  image: postgres:16
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-postgres}"]
    interval: 5s
    timeout: 5s
    retries: 5
```

**Why:** Without `service_healthy`, the application starts while the database is still initialising and fails with connection errors, producing misleading and hard-to-diagnose startup failures.

### PLAT-010-R5 — .env.example required and must be complete

A `.env.example` file MUST be committed to the repository root. It MUST:

- List **every** environment variable the application reads — required and optional
- Include a comment above each variable explaining its purpose and safe default
- Use safe, non-secret default values suitable for local development
- Be updated in the **same commit** whenever a variable is added, renamed, or removed

```bash
# Port the application listens on (default: 8080)
PORT=8080

# Log level: debug | info | warn | error (default: info)
LOG_LEVEL=info

# PostgreSQL connection string
DATABASE_URL=postgresql://postgres:changeme@localhost:5432/myapp
```

### PLAT-010-R6 — .env must be gitignored

`.env` MUST appear in `.gitignore`. Committing `.env` is a security violation.

### PLAT-010-R7 — All configuration via environment variables

All application configuration MUST be supplied through environment variables. Nothing MUST be hardcoded in source code. The application MUST fail fast at startup with a clear, human-readable error if a required variable is missing:

```
FATAL: Required environment variable DATABASE_URL is not set.
       Copy .env.example to .env and fill in the required values.
```

No secrets, credentials, or environment-specific values MUST ever be committed to source control.

### PLAT-010-R8 — README.md must cover local setup completely

A README.md MUST exist at the repository root. A new team member with no prior knowledge of the project MUST be able to follow it and reach a running local stack.

Required sections:

| Section | Content |
|---|---|
| Prerequisites | Every required tool with its exact minimum version |
| Getting Started | Step-by-step from `git clone` to running application |
| Accessing the Application | URLs for every service and health endpoint |
| Default Credentials | Any seeded accounts for local development |
| Stopping the Application | `docker compose down` |
| Resetting the Database | `docker compose down -v` or equivalent |

The README MUST be updated in the same commit when a new service, variable, or credential is added.

---

## Handoff Gate

Before handing off to the DevOps team:

- [ ] `docker compose up` starts the full stack — no manual steps beyond copying `.env.example`
- [ ] `depends_on` uses `condition: service_healthy` for all stateful dependencies
- [ ] All services referenced by `depends_on: condition: service_healthy` define a `healthcheck`
- [ ] `.env.example` documents every variable (required and optional) with a comment
- [ ] `.env` is gitignored
- [ ] All configuration read from environment variables — nothing hardcoded in source
- [ ] Application fails fast with a clear error message on missing required variables
- [ ] README covers all required sections listed above

---

## Related Documents

- `templates/DOCKER-COMPOSE-TEMPLATE.md` — annotated compose skeleton
- `templates/DOCKERFILE-TEMPLATE.md` — Dockerfile requirements (STD-PLAT-009)
- `templates/DEV-DEVOPS-HANDOFF-CHECKLIST.md` — full pre-handoff checklist
- `standards/platform/local-remote-parity.md` — STD-PLAT-008
- `standards/platform/containerization.md` — STD-PLAT-009
- `standards/operations/health-endpoints.md` — STD-OPS-003
