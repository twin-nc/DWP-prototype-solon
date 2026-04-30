---
id: STD-PLAT-009
title: Containerization
status: Approved
owner: DevOps / Release Engineer
applies_to: All services deployed as containers
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - platform/local-dev-environment.md
  - platform/local-remote-parity.md
  - operations/health-endpoints.md
last_changed: 2026-04-13
---

## Purpose

Define the minimum requirements for containerising a service so that it is safe, reproducible, and operable in Kubernetes without surprises at handoff to the DevOps team.

---

## Rule Set

### PLAT-009-R1 — Multi-stage build required

All Dockerfiles MUST use multi-stage builds:

- A **build stage** installs build tools and compiles the application
- A **runtime stage** copies only the compiled artefact and installs only runtime dependencies

Build tools (compilers, package managers, test frameworks, dev dependencies) MUST NOT be present in the final image.

**Why:** Multi-stage builds reduce image size significantly and eliminate a large class of supply-chain vulnerabilities by excluding build tooling from the runtime attack surface.

### PLAT-009-R2 — Base images must be pinned to a specific version

Both the build stage and the runtime stage MUST specify an exact version tag. `latest`, `stable`, or any other floating tag is prohibited.

```dockerfile
# Correct — version pinned
FROM maven:3.9.6-eclipse-temurin-21 AS build
FROM eclipse-temurin:21-jre-jammy

# Not acceptable
FROM eclipse-temurin:latest
FROM openjdk:jre
```

**Why:** Floating tags produce silent, non-reproducible differences between builds over time and across environments.

### PLAT-009-R3 — Container must not run as root

The Dockerfile MUST explicitly create a dedicated non-root user and switch to it before the `ENTRYPOINT` or `CMD`.

```dockerfile
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser
USER appuser
```

The user MUST be created, not assumed to exist. Do not rely on a base image user unless it is documented as non-root.

**Why:** Running as root inside a container gives an attacker root-equivalent access to the host if container isolation is bypassed.

### PLAT-009-R4 — Port must be configurable and EXPOSE must match

The application MUST read its listen port from the `PORT` environment variable, defaulting to `8080` if absent. The Dockerfile MUST `EXPOSE` the same default port.

```dockerfile
EXPOSE 8080
```

The `EXPOSE` directive and the actual port the application listens on MUST always be identical. If `PORT` is overridden at runtime, the override takes effect — `EXPOSE` documents the default.

### PLAT-009-R5 — SIGTERM must be handled gracefully

The application MUST handle `SIGTERM` gracefully in this order:

1. Stop accepting new requests
2. Finish processing all in-flight requests
3. Release resources and exit cleanly (exit code 0)

For frameworks that support it, this requires explicit configuration:

```
# Spring Boot — application.properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

**Why:** Kubernetes sends `SIGTERM` before terminating a pod. An application that ignores `SIGTERM` is force-killed after the grace period, dropping in-flight requests.

### PLAT-009-R6 — Image must be locally buildable

The image MUST be buildable with a single command before handoff:

```bash
docker build -t <registry>/<project>:<git-sha> .
```

This command MUST succeed without any manual preparation beyond cloning the repository and copying `.env.example` to `.env`.

---

## Image Tag Convention

Images MUST be tagged with the full or short git SHA of the commit they were built from. Pushing `latest` as a deployment artefact to any registry is prohibited.

```bash
GIT_SHA=$(git rev-parse --short HEAD)
docker build -t <registry>/<project>:${GIT_SHA} .
```

---

## Handoff Gate

The following MUST be true before handing off to the DevOps team:

- [ ] Dockerfile present at repository root
- [ ] Multi-stage build — pinned base image in both build and runtime stages
- [ ] Final image contains only runtime dependencies, not build tools
- [ ] Non-root user explicitly created and switched to before `ENTRYPOINT`
- [ ] Application listens on port 8080 by default, configurable via `PORT` env var
- [ ] `EXPOSE` in Dockerfile matches the actual default listen port
- [ ] SIGTERM handling explicitly configured — not assumed
- [ ] `docker build -t <registry>/<project>:<git-sha> .` succeeds locally with no manual steps

---

## Related Documents

- `templates/DOCKERFILE-TEMPLATE.md` — annotated Dockerfile skeleton for common runtimes
- `templates/DEV-DEVOPS-HANDOFF-CHECKLIST.md` — full pre-handoff checklist
- `standards/platform/local-dev-environment.md` — STD-PLAT-010: compose and local stack requirements
- `standards/operations/health-endpoints.md` — STD-OPS-003: /health/live and /health/ready
