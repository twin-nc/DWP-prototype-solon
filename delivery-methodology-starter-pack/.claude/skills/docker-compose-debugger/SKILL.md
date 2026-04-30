---
name: docker-compose-debugger
description: >
  Diagnose and fix Docker Compose stack failures for the DCMS local environment —
  containers that fail to start, crash on healthcheck, or block dependent services.
  Covers db, keycloak, api, and frontend service failures.
invocation: /docker-compose-debugger
inputs:
  - name: failing_service
    required: false
    description: "Name of the service that is failing: db | keycloak | api | frontend. Omit to run full-stack triage."
  - name: compose_output
    required: false
    description: Output from `docker compose up` or `docker compose logs <service>` if already captured
outputs:
  - name: fix_plan
    description: Root cause category, affected service, fix applied, and verification command
roles:
  - devops-release-engineer
  - backend-builder
---

# Docker Compose Debugger

## Purpose
`docker compose up` can fail silently — a container exits, its dependent services stall in `starting`, and the error is buried in logs. This skill provides a structured triage for DCMS local stack failures, covering the four services (`db`, `keycloak`, `api`, `frontend`) and the failure patterns specific to each.

**Scope: local Docker Compose stack only.** This skill is for failures when running `docker compose up` from the `infrastructure/` directory on a developer laptop. For post-start regressions on a running stack, use `canary-watch`. For Spring Boot application startup failures inside the `api` container (compilation, Flyway, autoconfiguration), use `java-build-resolver`. For CI failures on a PR, use `ci-failure-root-cause-fixer`.

## When to Use
- `docker compose up` exits or hangs and one or more services never reach `healthy`
- A container shows status `exited` or `unhealthy` in `docker compose ps`
- A dependent service stays in `starting` because its dependency never became healthy
- Port conflict errors at compose startup
- Volume mount or permission errors
- Keycloak realm import fails and the `api` cannot authenticate

## Do Not Use This Skill When
- **All services are up and healthy but the app behaves wrongly** — use `canary-watch` to check health endpoints, or `agent-introspection-debugging` if the issue is agent behaviour
- **The `api` container is crashing due to a Spring Boot error** — check `docker compose logs api` first; if you see a Java stack trace, use `java-build-resolver`
- **CI is failing** — use `ci-failure-root-cause-fixer`

## Service Dependency Order

```
db (healthcheck: pg_isready)
  └── keycloak (healthcheck: /health/ready)
        └── api (healthcheck: /actuator/health)
              └── frontend (no healthcheck — nginx static)
```

A failure in any service blocks all downstream services. Always identify the **first** unhealthy service in this chain — that is the root cause, not the downstream stalls.

## Error Categories and Fix Approach

### Category 1 — Port Conflict
**Signal:** `Bind for 0.0.0.0:<port> failed: port is already allocated`

Fix sequence:
1. Run `docker compose ps` to confirm which service is affected.
2. Find the conflicting process: `lsof -i :<port>` (Mac/Linux) or `netstat -ano | findstr :<port>` (Windows).
3. Stop the conflicting process, or change the host port in `docker-compose.yml` (left side of `ports:` mapping only — never change the container port).
4. Re-run `docker compose up`.

Default port assignments:
- `db`: 5432
- `keycloak`: 9090 (host) → 8080 (container)
- `api`: 8081 (host) → 8080 (container)
- `frontend`: 8080 (host) → 80 (container)

### Category 2 — Volume or Permission Error
**Signal:** `mkdir: cannot create directory`, `Permission denied`, `Error response from daemon: invalid mount config`

Fix sequence:
1. Run `docker compose down -v` to remove named volumes (this resets database state — confirm this is acceptable).
2. Check that the path referenced in `volumes:` exists and is accessible.
3. On Windows with WSL2: confirm the project is on the WSL filesystem, not a Windows path (`/mnt/c/...`) — Docker bind mounts on Windows paths have known permission and performance issues.
4. Re-run `docker compose up`.

### Category 3 — Database Healthcheck Failure
**Signal:** `db` service shows `unhealthy`; `pg_isready` exits non-zero

Fix sequence:
1. Run `docker compose logs db` and look for PostgreSQL startup errors.
2. Common causes:
   - **Data directory incompatibility:** a previous `postgres:15` volume is present but the image is now `postgres:16`. Fix: `docker compose down -v` to wipe the volume, then `docker compose up`.
   - **Init script error:** a file in `infrastructure/postgres/init/` has a syntax error. Fix: correct the SQL file.
   - **Insufficient shared memory:** rare on Linux containers. Fix: add `shm_size: 128mb` under the `db` service in `docker-compose.yml`.
3. Confirm `db` is healthy before investigating downstream services.

### Category 4 — Keycloak Startup or Realm Import Failure
**Signal:** `keycloak` shows `unhealthy`; `/health/ready` returns non-200; realm import error in logs

Fix sequence:
1. Run `docker compose logs keycloak` and look for the first ERROR line.
2. Common causes:
   - **Realm import file missing or malformed:** the file mounted at `/opt/keycloak/data/import/` is invalid JSON or missing. Fix: confirm `infrastructure/keycloak/realm-export.json` exists and is valid JSON.
   - **Database not yet ready:** Keycloak started before `db` was healthy. This should not happen if `depends_on: db: condition: service_healthy` is set. If it is missing, add it.
   - **Admin credentials conflict:** `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` env vars are missing. Fix: confirm they are set in `docker-compose.yml` or `.env`.
3. Once Keycloak is healthy, confirm the realm exists: `curl http://localhost:9090/realms/dcms` should return a JSON object (not 404).

### Category 5 — API Container Crash (Spring Boot)
**Signal:** `api` shows `exited` or `unhealthy`; Java stack trace visible in `docker compose logs api`

Fix sequence:
1. Run `docker compose logs api` and capture the full output.
2. If you see a Java compilation error or Maven failure: the image was not built correctly. Run `mvn package -pl backend -am -DskipTests` locally to confirm the JAR builds, then `docker compose build api`.
3. If you see a Spring Boot startup exception (APPLICATION FAILED TO START): use `java-build-resolver` to diagnose — this is a Spring autoconfiguration, Flyway, or property issue, not a Docker issue.
4. If you see `Connection refused` to `db` or `keycloak`: the `api` started before its dependencies were healthy. Confirm `depends_on` conditions are set for both `db` and `keycloak` in `docker-compose.yml`.
5. After fixing: `docker compose up --build api` to rebuild and restart only the api service.

### Category 6 — Frontend Container Failure
**Signal:** `frontend` shows `exited`; nginx error in logs

Fix sequence:
1. Run `docker compose logs frontend`.
2. Common causes:
   - **Build failure:** the React build failed before the nginx stage. Run `npm run build` in `frontend/` to see the error.
   - **nginx config error:** `nginx.conf` has a syntax error. Run `docker compose exec frontend nginx -t` to validate.
   - **Missing static files:** the multi-stage build did not copy the `dist/` output. Check the `Dockerfile` COPY step.
3. After fixing: `docker compose up --build frontend`.

### Category 7 — Image Pull Failure
**Signal:** `Unable to find image`, `pull access denied`, `manifest unknown`

Fix sequence:
1. Check the image name and tag in `docker-compose.yml` match the pinned versions in `CLAUDE.md`.
2. For `quay.io/keycloak/keycloak:24`: confirm internet access and that quay.io is not blocked.
3. For internal images (`ufstpit-dev-docker.repo.netcompany.com/...`): confirm registry credentials are set in `~/.docker/config.json` or run `docker login ufstpit-dev-docker.repo.netcompany.com`.

## Steps

1. Run `docker compose ps` from `infrastructure/` and note every service not in `running (healthy)` state.
2. Identify the first unhealthy service in the dependency chain (`db` → `keycloak` → `api` → `frontend`).
3. Run `docker compose logs <service>` for that service and find the first ERROR line.
4. Match to a category above and apply the fix.
5. Run `docker compose up` (or `docker compose up --build <service>` for rebuild) to verify.
6. Once all services are healthy, run `canary-watch` with `environment=local` to confirm the stack is responding correctly.

## Output Contract

```
## Docker Compose Debugger Report

Failing service: <db | keycloak | api | frontend>
Error category: <Category 1-7>
Root cause: <one sentence>
Fix applied: <description of the change>
Verification command: <command run>
Verification output: <relevant line(s) confirming healthy or remaining error>
Result: PASS / STILL FAILING
Residual risk: <any follow-on risk or "none">
```

## Guardrails
- Always identify the first unhealthy service in the dependency chain — do not diagnose downstream stalls as independent failures.
- Do not run `docker compose down -v` without confirming that resetting database state is acceptable — it destroys all local data.
- Do not change container-side port numbers — only host-side ports in the `ports:` mapping.
- After any fix involving `docker compose down`, re-run `docker compose up` and confirm all four services reach `healthy` before reporting success.
- Hand off to `java-build-resolver` as soon as a Java stack trace appears in `api` logs — do not attempt Spring Boot diagnosis here.

## Integration
- Run `canary-watch` with `environment=local` after confirming all services are healthy.
- Use `java-build-resolver` when `docker compose logs api` shows a Spring Boot startup exception.
- Use `ci-failure-root-cause-fixer` when the same failure is appearing on a GitHub Actions PR build.
