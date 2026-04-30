# Scaffolding Overview (As Implemented)

## Snapshot

This is a current-state overview of the scaffold as it exists in the repository today.

- Snapshot date: 2026-04-21
- Scope reviewed: `docs/project-foundation/`, `backend/`, `frontend/`, `infrastructure/`, `.github/workflows/`, `.claude/`, `AGENTS.md`

## 1) Project Foundation and Governance Module

**Primary path:** `docs/project-foundation/`

### What currently exists

- Core foundation docs are present and populated:
  - `getting-started-guide.md`
  - `INSTALL-MANIFEST.md`
  - `REPO-SETUP-CHECKLIST.md`
  - `PRE-IMPLEMENTATION-CHECKPOINT.md`
  - `REMOTE-DEPLOYMENT-READINESS-GATE.md`
  - `WAYS-OF-WORKING.md`
  - `DOCUMENTATION-POLICY.md`
  - `AGENT-OUTLINES.md`
  - `AGENT-RULES.md`
  - `SKILLS-GUIDE.md`
  - `scaffold-design-plan.md`
  - `trace-map.yaml`

- Standards pack is installed and structured:
  - `standards/` contains 35 standards documents
  - domains covered: governance, development, platform, integration, operations, security, deviations, ai
  - `standards/README.md` states standards pack version `v1.1` and last updated date `2026-04-16`

- Templates are installed and reusable:
  - `templates/` contains 24 templates
  - includes delivery templates (decision log, runbook, release evidence pack, status report)
  - includes technical templates (Dockerfile, Docker Compose, Helm chart template)

## 2) Backend Module

**Primary path:** `backend/`

### Technology baseline currently scaffolded

- Java 21
- Spring Boot `3.4.4`
- Maven project version `0.1.0-SNAPSHOT`
- Dependencies include web, security, oauth2 resource server, data-jpa, flyway, validation, actuator, structured logging

### Runtime behavior currently implemented

- Service runs on port `8081` by default (`server.port=${PORT:8081}`)
- JWT issuer defaults to `http://localhost:9090/realms/dcms`
- Health endpoints:
  - `GET /health/live` -> always `200` with `{ "status": "UP" }`
  - `GET /health/ready` -> `200` when DB reachable, else `503`
- Security:
  - `/health/live` and `/health/ready` are unauthenticated
  - all other endpoints require JWT auth
  - Keycloak realm roles are mapped from `realm_access.roles` to `ROLE_*`
- Correlation ID:
  - `X-Correlation-ID` header accepted or generated
  - value is returned in response header and written to MDC as `correlationId`
- Error handling:
  - global error envelope via `ErrorResponse`
  - includes `correlationId`, `status`, `error`, `message`, `timestamp`

### Data and schema scaffold currently implemented

- Flyway migration currently contains one foundational table:
  - `audit_log` with indexes on `(entity_type, entity_id)` and `created_at`
- No business domain tables yet

### Code footprint currently implemented

- Java entry point: `DcmsApplication`
- Config classes: `SecurityConfig`, `CorrelationIdFilter`, `ApplicationProperties`
- Shared modules implemented: `shared/health`, `shared/error`
- Domain package folders exist only as placeholders (`.gitkeep`):
  - `account`, `audit`, `communications`, `customer`, `integration`, `payment`, `repaymentplan`, `strategy`, `user`, `workallocation`

### Test scaffold currently implemented

- 1 integration test: `HealthEndpointIT`
- Test verifies `GET /health/live` returns `200`

## 3) Frontend Module

**Primary path:** `frontend/`

### Technology baseline currently scaffolded

- React 18 + TypeScript + Vite
- GOV.UK Frontend integrated in global styles
- Test stack: Vitest + Testing Library + vitest-axe

### Current app behavior

- Routing currently has 2 outcomes:
  - `/` -> Dashboard page
  - any other path -> Not Found page
- Layout shell is implemented:
  - GOV.UK header
  - page container with `<Outlet />`
  - footer with Crown copyright
- Dashboard is currently static welcome content

### API scaffold currently implemented

- Axios client configured with base URL `/api`
- Request interceptor adds UUID `X-Correlation-ID` to each request
- No domain-specific API modules/endpoints yet

### Styling scaffold currently implemented

- `globals.scss` imports GOV.UK all styles
- `govuk-overrides.scss` exists as extension point (currently placeholder comments)

### Test scaffold currently implemented

- accessibility smoke test runs axe checks against `App`
- test setup imports `@testing-library/jest-dom`

## 4) Infrastructure and Deployment Module

**Primary path:** `infrastructure/`

### Docker Compose stack currently implemented

`docker-compose.yml` defines 4 services:

1. `db` (PostgreSQL 16)
  - port `5432:5432`
  - user/password/db all `dcms`
2. `keycloak` (`quay.io/keycloak/keycloak:24.0.5`)
  - port `9090:8080`
  - starts with `start-dev --import-realm`
  - uses in-memory DB (`KC_DB: dev-mem`)
3. `api` (builds from `backend/Dockerfile`)
  - port `8081:8081`
  - profile `local`
  - depends on healthy db + keycloak
4. `frontend` (builds from `frontend/Dockerfile`)
  - port `8080:8080`
  - depends on healthy api

All services include health checks.

### Keycloak scaffold currently implemented

- Realm file: `infrastructure/keycloak/realm-dcms.json`
- Realm: `dcms`
- Clients present:
  - `dcms-frontend`
  - `dcms-backend`
  - `dcms-robot`
- Seed users present:
  - `admin` (realm role `DCMS_ADMIN`)
  - `caseworker1` (realm role `CASE_WORKER`)
  - `manager1` (realm roles `CASE_MANAGER`, `CASE_WORKER`)

### Helm chart scaffold currently implemented

- Chart metadata:
  - `name: dcms`
  - `version: 0.1.0`
  - `appVersion: 0.1.0`
- Templates present:
  - `Deployment.yaml` (separate API + frontend deployments)
  - `Service.yaml` (separate ClusterIP services)
  - `Ingress.yaml` (routes `/` to frontend and `/api` to API)
  - `ConfigMap.yaml` (PORT, LOG_LEVEL, profile, DATABASE_URL, KEYCLOAK_ISSUER_URI)
  - `ServiceAccount.yaml`
  - `_helpers.tpl`
- Values files:
  - `values.yaml` base values
  - `values-local.yaml` local override (`pullPolicy: Never`, `LOG_LEVEL=debug`)
  - `values-dev.yaml` dev override (`replicaCount: 2`, `pullPolicy: Always`)
- `secrets` section is currently empty placeholder (`secrets: {}`)

## 5) CI/CD and Repository Automation Module

**Primary path:** `.github/workflows/`

### Workflows currently implemented

- `ci.yml`
  - backend: build/test, docker build, trivy scan
  - frontend: typecheck/test/build, docker build, trivy scan
- `compose-smoke-test.yml`
  - spins up full compose stack and verifies `/health/live` and `/health/ready`
- `helm-lint.yml`
  - lints and renders Helm chart with base/local/dev values
- `deploy-dev.yml`
  - on `main`: build images, trivy scan, push registry tags, helm deploy to `dcms-dev`

### Other GitHub scaffolding present

- `dependabot.yml`
- `pull_request_template.md`

## 6) AI Agent and Skill Module (Supporting)

**Primary paths:** `.claude/` and `AGENTS.md`

### Current installed surface

- `.claude/agents/`: 16 agent files
- `.claude/skills/`: 50 skill directories
- `.claude/hooks/`: 10 hook scripts
- `AGENTS.md` exists as generated mirror of agents and skills for Codex compatibility

## 7) Tender Requirements Module (Supporting)

**Primary path:** `Tender requirements docs/`

### Current artifacts present

- Functional requirements (markdown + spreadsheet source)
- Non-functional requirements (markdown + spreadsheet source)
- Supplier demonstration agenda (markdown + docx)

## 8) Current Scaffold Maturity (Important)

This scaffold is a strong foundation, but it is still at platform/bootstrap stage.

### Already implemented

- Monorepo structure
- Security, health, logging, error envelope foundations
- Local stack orchestration (DB, Keycloak, API, frontend)
- Helm packaging for API and frontend
- CI/CD and deployment workflow skeletons
- Governance standards/templates and agent operating model

### Not implemented yet (business feature layer)

- Domain entities/services/controllers beyond health and shared infra concerns
- Business workflows for debt collection lifecycle
- Frontend feature screens beyond dashboard and not-found
- Domain-specific persistence model beyond initial audit table
- End-to-end functional user journeys
