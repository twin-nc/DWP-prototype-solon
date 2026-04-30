# C1/C4 — DevOps Requirements

## DevOps Requirements
*The application will be run locally using Docker Compose before handoff to the DevOps team. The goal is that any developer can clone the repo and run the full stack locally with a single command.*

---

**NFR-C4-004 — Containerization**
- Application must be containerized using **Docker**
- A `Dockerfile` must be provided at the repository root
- Use **multi-stage builds** to keep the image small
- Base image must be **pinned to a specific version** (no `latest`)
- Final image must contain only runtime dependencies, not build tools
- Container must run as a **non-root user** — explicitly create and set one:
  ```dockerfile
  RUN adduser --disabled-password appuser
  USER appuser
  ```
- Application must listen on port **8080** by default, configurable via `PORT` environment variable
- `Dockerfile` must `EXPOSE` the same port the application listens on
- Application must handle **SIGTERM gracefully**:
  - Stop accepting new requests
  - Finish in-flight requests
  - Then exit cleanly
- The image must be buildable locally with:
  ```bash
  docker build -t ufstpit-dev-docker.repo.netcompany.com/c1c4:<git-sha> .
  ```
- The dev team does **not** need to push the image to the registry — that is handled by the DevOps team

---

**NFR-C4-005 — Local Development Environment**
- A `docker-compose.yml` must be provided enabling the full local stack with:
  ```bash
  docker compose up
  ```
- Must include all dependencies as services
- Must work **without any cloud credentials or external connectivity**
- `docker-compose.yml` must use `depends_on` with `condition: service_healthy` so the application does not start before the database is ready:
  ```yaml
  depends_on:
    db:
      condition: service_healthy
  ```
- A `.env.example` file must be committed documenting all required environment variables with a comment explaining each one and its default value:
  ```bash
  # Port the application listens on (default: 8080)
  PORT=8080

  # Log level: debug | info | warn | error (default: info)
  LOG_LEVEL=info

  # PostgreSQL connection string
  DATABASE_URL=postgresql://user:password@localhost:5432/c1c4
  ```
- `.env.example` must include **every** variable the application reads, including optional ones
- `.env` must be **gitignored**

---

**NFR-C4-006 — Configuration**
- All configuration must be provided via **environment variables** — nothing hardcoded
- Application must **fail fast with a clear error message** if a required variable is missing
- No secrets or environment-specific values committed to source control
- The dev team does **not** need to worry about how secrets are managed in Kubernetes — the application just needs to read from environment variables and the DevOps team will handle the rest

---

**NFR-C4-007 — Health Endpoints**
- The application must expose:
  - `GET /health/live` — is the process alive?
  - `GET /health/ready` — is the app ready to serve traffic?
- `/health/live` must return `200` as long as the process is running
- `/health/ready` must **actively verify database connectivity** before returning `200`:
  - If the database is reachable → return `200`
  - If the database is unreachable → return `503`
- **No authentication required** on either endpoint
- These endpoints are used by Kubernetes for liveness and readiness probes — they must always be available

---

**NFR-C4-008 — Logging**
- All logs must be written to **stdout/stderr** — never to files
- Log format must be **structured JSON**
- Minimum fields per log entry:

  | Field | Description |
  |---|---|
  | `timestamp` | ISO 8601 format |
  | `level` | debug / info / warn / error |
  | `message` | Human readable description |
  | `correlationId` | Unique ID per request |

- A `correlationId` must be included in every request:
  - Accept from incoming request header if present
  - Generate a new one if absent
  - Include in all log entries for that request

---

**NFR-C4-009 — Database**
- The database must be **PostgreSQL** — no other database engine is acceptable
- The official `postgres` Docker image must be used in `docker-compose.yml`, pinned to a specific version:
  ```yaml
  image: postgres:16
  ```
- The database service must define a **healthcheck** in `docker-compose.yml`:
  ```yaml
  healthcheck:
    test: ["CMD-SHELL", "pg_isready -U postgres"]
    interval: 5s
    timeout: 5s
    retries: 5
  

```
- Connection must be fully configurable via environment variable:
  ```bash
  DATABASE_URL=postgresql://user:password@localhost:5432/c1c4
  ```
- Migrations must run **automatically on application startup** — no manual steps required
- The dev team does **not** need to worry about how the database is hosted in Azure — the DevOps team will provide the connection string via Kubernetes Secret and the application just needs to read `DATABASE_URL` from the environment

---

**NFR-C4-010 — Local Development Guide**
- A `README.md` must be provided at the repository root
- Must be written so that a new team member with no prior knowledge of the project can get it running in **under 10 minutes**
- Minimum required sections:

```markdown
## Prerequisites
- Docker Desktop 4.x
- Docker Compose v2.x
- Git

## Getting Started
1. Clone the repository
   git clone <repo-url>
2. Copy .env.example to .env
   cp .env.example .env
3. Fill in any required values in .env
4. Start the full stack
   docker compose up

## Accessing the Application
- Portal: http://localhost:8080
- API:    http://localhost:8080/api
- Health: http://localhost:8080/health/live
          http://localhost:8080/health/ready

## Default Credentials
Document any default admin/user accounts seeded for local development

## Stopping the Application
docker compose down

## Resetting the Database
docker compose down -v
```

- The guide must be kept up to date — if a new environment variable is added, `.env.example` and `README.md` must be updated in the same commit

---

**NFR-C4-011 — Helm Chart (Basic)**

*The dev team must provide a basic Helm chart that the DevOps team will build upon. The dev team is not responsible for Key Vault integration, TLS, or production-grade settings — those will be handled by the DevOps team.*

- A Helm chart must be provided under `/chart` in the repository
- Use `helm create chart` as a starting point
- Remove any default templates generated by `helm create` that are not listed below
- The chart must contain exactly these templates:

  | Template | Responsibility |
  |---|---|
  | `Deployment.yaml` | Run the application pods with correct image, port and environment variables |
  | `Service.yaml` | Expose the application internally on port 8080 |
  | `ConfigMap.yaml` | Hold non-sensitive configuration values |
  | `Ingress.yaml` | Basic ingress — no TLS, DevOps will configure |
  | `ServiceAccount.yaml` | Basic service account — no annotations, DevOps will configure |

- `values.yaml` must contain at minimum:
  ```yaml
  image:
    repository: ufstpit-dev-docker.repo.netcompany.com/c1c4
    tag: latest
    pullPolicy: IfNotPresent

  replicaCount: 1

  service:
    port: 8080

  ingress:
    enabled: true
    host: ""

  env:
    PORT: "8080"
    LOG_LEVEL: "info"
    DATABASE_URL: ""

  # To be configured by DevOps team - do not fill in
  secrets: {}
  ```
- Every environment variable the application reads must be present in the `env` section
- Sensitive values such as `DATABASE_URL` must be left empty — no credentials hardcoded
- All values in `values.yaml` must be wired into the templates — no hardcoded values inside templates
- Do not configure TLS in `Ingress.yaml` — DevOps will handle this
- Do not add annotations to `ServiceAccount.yaml` — DevOps will handle this
- Do not hardcode any environment-specific values, secrets or credentials anywhere in the chart
- The following commands must both pass with no errors before handoff:
  ```bash
  helm lint ./chart
  helm template ./chart
  ```

---

Non-Functional Requirements: Local + Dev Deployment Baseline
Scope
This baseline covers only:

local: developer machine deployment to k3d
dev: shared AKS deployment hosted in Azure
No other environment types are required by this document.

Local Cluster NFRs (k3d)
NFR-LC-01 (Single-command bootstrap): A clean developer machine can create cluster + bootstrap prerequisites from one documented command.
NFR-LC-02 (Idempotency): Local bootstrap/deploy scripts can be re-run without manual cleanup and without duplicate/broken resources.
NFR-LC-03 (Fast failure): Missing prerequisites (docker, kubectl, helm, k3d) must fail immediately with install guidance.
NFR-LC-04 (Local ingress usability): Hostname routing requirements for local ingress must be automated or clearly documented.
NFR-LC-05 (Deterministic local images): Local deployment must support using locally built/preloaded images without forced external pulls.
NFR-LC-06 (Resource baseline): Minimum local machine resources (CPU/RAM) must be documented in the runbook.
Build NFRs
NFR-BLD-01 (Interactive + non-interactive): Build script must support both interactive use and non-interactive invocation.
NFR-BLD-02 (Selective build): Build script must support all components and selected components.
NFR-BLD-03 (Traceable tags): Every built image tag must be traceable to a source revision (for example SHA-based or explicit release tag).
NFR-BLD-04 (Fail-fast): Build or push failure must return non-zero exit code and stop further steps.
NFR-BLD-05 (No embedded credentials): Scripts must not hardcode registry or cloud credentials.
Deploy NFRs (Local + Dev)
NFR-DPL-01 (Single command deploy): Deploy script must support a one-command path for dependency resolution and Helm deployment.
NFR-DPL-02 (Two overlays only): Deployment config must support values-dev and values-local overlays on top of shared base values.
NFR-DPL-03 (Release safety): Dev deployments must use rollback-safe Helm behavior (--atomic or equivalent).
NFR-DPL-04 (Configurable timeout): Helm/deploy timeout must be configurable from script parameters.
NFR-DPL-05 (Readiness gate): Deployment must fail when critical workloads do not become ready within timeout.
NFR-DPL-06 (Context guard): Script must show current kube context/target namespace and warn before risky mis-targeting.
NFR-DPL-07 (Diagnostics on failure): On deploy failure, script must output immediate diagnostic commands/results (pods, events, rollout/log hints).
Dev (AKS on Azure) NFRs
NFR-DEV-01 (Target platform): The dev environment is AKS in Azure and must be the only shared remote target supported by this baseline.
NFR-DEV-02 (Pinned image deploy): Dev deployments must use explicit image tags (no implicit latest behavior).
NFR-DEV-03 (Secret source): Dev secrets must come from Kubernetes secrets/external secret integration, not from plaintext values files.
NFR-DEV-04 (Access model): Deployment automation must work with a provided kubeconfig/service principal setup documented in runbook.
Documentation and Verification NFRs
NFR-OPS-01 (Runbook completeness): Runbook must include prerequisites, bootstrap, build, deploy, verify, rebuild-after-change, teardown, and troubleshooting.
NFR-OPS-02 (Version policy): Minimum supported versions for critical tools/charts must be documented.
NFR-OPS-03 (Verification contract): Standard verification must include pod health and service health endpoint checks.
NFR-OPS-04 (Simple acceptance): Each NFR group must have at least one concrete command-based check in runbook/scripts.

## Handoff Checklist
*The dev team must confirm all of the following before handing off to the DevOps team:*

```
[ ] Dockerfile present at repository root
[ ] Dockerfile uses multi-stage build with pinned base image
[ ] Final Docker image contains only runtime dependencies
[ ] Non-root user explicitly created and set in Dockerfile
[ ] Application listens on port 8080 (configurable via PORT env variable)
[ ] Dockerfile EXPOSE matches the actual application port
[ ] Application handles SIGTERM gracefully
[ ] Image builds successfully locally with:
    docker build -t ufstpit-dev-docker.repo.netcompany.com/c1c4:<git-sha> .

[ ] docker-compose.yml runs the full stack with docker compose up
[ ] docker-compose.yml uses depends_on with service_healthy condition
[ ] PostgreSQL defined as a service in docker-compose.yml using postgres:16
[ ] Database service has a healthcheck defined in docker-compose.yml

[ ] .env.example documents every variable with a comment and default value
[ ] All config read from environment variables — nothing hardcoded
[ ] App fails fast with a clear error if a required variable is missing

[ ] GET /health/live returns 200 when process is alive
[ ] GET /health/ready returns 200 when DB is reachable, 503 when not
[ ] Both health endpoints work without authentication
[ ] Database migrations run automatically on startup

[ ] README.md present at repository root
[ ] README covers prerequisites with exact tool versions
[ ] README covers step-by-step local setup
[ ] README documents how to access portal, API and health endpoints locally
[ ] README documents default local credentials
[ ] README documents how to stop and reset the stack

[ ] /chart directory present in the repository
[ ] Chart contains only the five templates listed above
[ ] No unused default templates left over from helm create
[ ] Deployment.yaml uses image, port and env vars from values.yaml
[ ] Service.yaml exposes port 8080
[ ] ConfigMap.yaml present with non-sensitive config
[ ] Ingress.yaml present with no TLS configured
[ ] ServiceAccount.yaml present with no annotations
[ ] values.yaml contains all required fields listed above
[ ] Every application env variable present in values.yaml env section
[ ] No secrets or credentials hardcoded anywhere in the chart
[ ] helm lint ./chart passes with no errors
[ ] helm template ./chart renders without errors
```
