---
id: STD-OPS-003
title: Health Endpoints
status: Approved
owner: DevOps / SRE
applies_to: All HTTP services deployed to Kubernetes
release_impact_if_violated: Release-blocking — Kubernetes cannot manage pods safely without correct health endpoints
related_standards:
  - operations/observability-and-signal-to-noise.md
  - platform/containerization.md
  - platform/local-dev-environment.md
last_changed: 2026-04-13
---

## Purpose

Define the two health endpoints every HTTP service MUST expose for Kubernetes liveness and readiness probes. Getting these wrong causes cascading deployment failures: services route traffic to unready pods, Kubernetes restarts healthy pods unnecessarily, or broken pods are never restarted. These are some of the most time-consuming and misleading failure modes in production deployments.

---

## Rule Set

### OPS-003-R1 — Two endpoints required at fixed paths

Every HTTP service MUST expose both of these endpoints:

| Endpoint | Purpose | Kubernetes probe type |
|---|---|---|
| `GET /health/live` | Is the process alive and not deadlocked? | Liveness probe |
| `GET /health/ready` | Is the app ready to serve traffic? | Readiness probe |

The paths `/health/live` and `/health/ready` are fixed. They MUST NOT be changed without coordinating with the DevOps team, as the Helm chart probe configuration depends on them.

### OPS-003-R2 — /health/live semantics

`GET /health/live` MUST:

- Return `200 OK` as long as the process is running and not in a deadlock or crash loop
- **Never** check external dependencies (database, message brokers, downstream APIs)
- Respond in under 100ms
- Require no authentication

If this endpoint returns a non-2xx response or times out, Kubernetes will **restart** the pod. Checking the database here will cause spurious restarts during transient DB slowness — do not do it.

Minimal correct implementation:
```json
// GET /health/live → 200 OK
{ "status": "ok" }
```

### OPS-003-R3 — /health/ready semantics

`GET /health/ready` MUST:

- Actively verify that all required dependencies are reachable before returning `200`
- Return `503 Service Unavailable` (not `200`, not `500`) if any required dependency is unreachable
- Require no authentication

Minimum check: database connectivity (execute a trivial query, e.g., `SELECT 1`).

```json
// GET /health/ready → 200 OK when all dependencies reachable
{ "status": "ok" }

// GET /health/ready → 503 when database is unreachable
{
  "status": "degraded",
  "checks": {
    "database": "unreachable"
  }
}
```

If this endpoint returns non-2xx, Kubernetes **stops routing traffic** to the pod but does not restart it. This is correct behaviour during startup (DB not yet ready) and during transient dependency failures.

### OPS-003-R4 — No authentication on either endpoint

Both `/health/live` and `/health/ready` MUST be accessible without any authentication, API key, or session. Authentication middleware MUST be explicitly bypassed for these paths.

**Why:** Kubernetes probes do not send credentials. If authentication is required, every probe will fail — Kubernetes will immediately restart or stop routing to all pods, taking the service down.

### OPS-003-R5 — Endpoints must be available from process start

Both health endpoints MUST be bound and responding before:
- Database migrations run
- Any slow initialisation logic completes
- Any application startup hook completes

**Why:** If the endpoints are only available after full startup completes, Kubernetes will kill the pod before it finishes starting (liveness probe timeout). The application must be able to accept probe traffic from the moment the process binds to its port.

---

## Kubernetes Probe Configuration

The following values are the standard starting point. The DevOps team will tune them per environment — do not change probe paths.

```yaml
livenessProbe:
  httpGet:
    path: /health/live
    port: http
  initialDelaySeconds: 10
  periodSeconds: 10
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /health/ready
    port: http
  initialDelaySeconds: 15
  periodSeconds: 10
  failureThreshold: 3
```

These values MUST appear in the `Deployment.yaml` template in the Helm chart. See `templates/HELM-CHART-TEMPLATE/`.

---

## docker-compose Healthcheck

In `docker-compose.yml`, the application service SHOULD define a healthcheck using the `/health/ready` endpoint so that dependent services wait for readiness:

```yaml
api:
  healthcheck:
    test: ["CMD-SHELL", "wget -qO- http://localhost:8080/health/ready || exit 1"]
    interval: 10s
    timeout: 5s
    retries: 6
    start_period: 40s
```

---

## Common Mistakes

| Mistake | Consequence |
|---|---|
| `/health/ready` returns `200` when DB is down | Kubernetes routes traffic to the pod; all requests fail |
| `/health/live` checks the DB | DB slowness causes spurious pod restarts |
| Either endpoint requires authentication | All Kubernetes probes fail; service is taken down |
| `/health/ready` returns `500` instead of `503` when DB is down | Inconsistent handling — always use `503` |
| Endpoints only available after full startup | Kubernetes kills the pod before it finishes starting |
| Paths differ from `/health/live` and `/health/ready` | Helm chart probe config breaks at handoff |

---

## Handoff Gate

- [ ] `GET /health/live` returns `200` as long as the process is running
- [ ] `GET /health/ready` returns `200` when all dependencies are reachable
- [ ] `GET /health/ready` returns `503` when the database is unreachable
- [ ] Neither endpoint requires authentication
- [ ] Both endpoints respond before migrations or slow initialisation complete

---

## Related Documents

- `templates/HELM-CHART-TEMPLATE/templates/Deployment.yaml` — standard probe config
- `templates/DOCKER-COMPOSE-TEMPLATE.md` — healthcheck pattern for compose
- `standards/operations/observability-and-signal-to-noise.md` — STD-OPS-001
- `templates/DEV-DEVOPS-HANDOFF-CHECKLIST.md`
