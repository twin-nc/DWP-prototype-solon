# Remote Environment Specification

> **Owner:** DevOps / Release Engineer  
> **Created:** 2026-04-23  
> **Last reviewed:** 2026-04-23  
> **Required reading for:** Any change affecting deployment, routing, environment config, or remote diagnostics.
>
> This document is the operational source of truth for local/remote parity and remote deployment behavior.

---

## 1. Environment Inventory

| Environment | Purpose | URL / Endpoint | Maintained By | Notes |
|---|---|---|---|---|
| `local` | Developer full-stack execution | `http://localhost:8080` (frontend), `http://localhost:8081` (api), `http://localhost:9090` (keycloak) | Engineering team | Docker Compose |
| `dev` | Continuous deployment from `main` | `TBD by DevOps` | DevOps / Release Engineer | AKS namespace `dcms-dev` |
| `staging` | Pre-release validation | Not in scope yet | N/A | To be defined |
| `production` | Live traffic | Not in scope yet | N/A | To be defined |

---

## 2. Infrastructure Topology

Current model in scope:

```
Developer browser
  -> Frontend (Nginx, React)
  -> Backend API (Spring Boot)
  -> PostgreSQL
  -> Keycloak

Dev deployment:
Internet
  -> Kubernetes Ingress
  -> Frontend service + API service
  -> PostgreSQL (managed by environment owner)
  -> Keycloak (managed by environment owner)
```

**Container runtime (dev):** AKS  
**Namespace(s):** `dcms-dev`  
**Region:** `TBD by DevOps`  
**Node count and sizing:** `TBD by DevOps`

---

## 3. GitOps Sync Mechanism

| Item | Detail |
|---|---|
| Deployment mechanism | GitHub Actions workflow `deploy-dev.yml` executes `helm upgrade --install` |
| Trigger | Push to `main` or manual `workflow_dispatch` |
| Source repo/path | This repository, Helm chart at `infrastructure/helm/dcms` |
| Sync frequency | Event-driven per workflow run |
| Manual trigger | GitHub Actions -> Run `Deploy to dev` |
| Verify completion | Workflow success + `kubectl rollout status` checks in deploy job |

Current state note: this is CI-driven Helm deployment, not Argo/Flux pull-based GitOps.

---

## 4. Routing and Ingress

| Route / Path | Service | Port | Auth Required | Notes |
|---|---|---|---|---|
| `/` | frontend service | 8080 | Login flow in app | Ingress routes to frontend |
| `/api` | api service | 8081 | Yes (except `/health/*`) | Ingress routes API traffic |
| `/health/live` | api service | 8081 | No | Used for liveness |
| `/health/ready` | api service | 8081 | No | Used for readiness |

**Ingress controller:** Kubernetes Ingress (controller details managed by DevOps)  
**TLS termination:** `TBD by DevOps`  
**DNS ownership:** `TBD by DevOps`

---

## 5. Log Access

### 5.1 Infrastructure Logs

| Log Source | How to Access | Access Level Required | Notes |
|---|---|---|---|
| Pod status/events | `kubectl get pods/events -n dcms-dev` | kubeconfig with namespace access | Primary diagnostic path today |
| API pod logs | `kubectl logs -n dcms-dev deployment/dcms-api --tail=200` | kubeconfig with namespace access | JSON logs with correlation ID |
| Frontend pod logs | `kubectl logs -n dcms-dev deployment/dcms-frontend --tail=200` | kubeconfig with namespace access | Nginx container output |

### 5.2 Solution Logs (Application-Level)

| Service | Log Format | How to Access | Retention | Notes |
|---|---|---|---|---|
| backend API | JSON to stdout | `kubectl logs` (and future centralized platform) | `TBD by DevOps` | Must include `correlationId` |
| frontend | Container stdout | `kubectl logs` | `TBD by DevOps` | Mostly web server level logs |

**Log aggregation system:** `TBD by DevOps`  
**Access provisioning:** `TBD by DevOps`

---

## 6. Environment-Specific Configuration (Parity Statement)

This section fulfills `STD-PLAT-008` parity statement requirements.

| Config Item | Local Value | Remote (dev) Value | Rationale |
|---|---|---|---|
| `PORT` | `8081` | `8081` | Same key, same behavior |
| `LOG_LEVEL` | `debug` (local profile) | `info` (dev profile) | Value differs by environment purpose |
| `SPRING_PROFILES_ACTIVE` | `local` | `dev` | Profile-specific behavior with same key shape |
| `DATABASE_URL` | `postgresql://dcms:dcms@db:5432/dcms` (compose) | Injected by environment owner | Same key, different endpoint/credentials |
| `KEYCLOAK_ISSUER_URI` | `http://keycloak:8080/realms/dcms` (compose network) | Injected by environment owner | Same key, environment-specific issuer URL |

Known divergence notes:
- Local Keycloak is containerized in Compose and reachable by Docker network hostnames.
- Dev Keycloak and DB endpoints are managed externally and injected at deploy/runtime.
- Config **shape** is aligned across local and dev.

**Secrets management on remote:** `TBD by DevOps` (current Helm values indicate secret injection outside this repo)

---

## 7. Access and Credentials

| Access Type | How to Obtain | Who Grants It | Required For |
|---|---|---|---|
| `DEV_KUBECONFIG` secret material | Internal request process | DevOps / Release Engineer | Deployment and diagnostics |
| Container registry credentials | Internal request process | DevOps / Release Engineer | CI image push |
| Future log platform access | `TBD` | `TBD` | Centralized remote debugging |

---

## 8. Common Remote Debugging Runbook

### Service not responding

```bash
kubectl get pods -n dcms-dev
kubectl get events -n dcms-dev --sort-by='.lastTimestamp' | tail -30
kubectl logs -n dcms-dev deployment/dcms-api --tail=200
kubectl rollout status deployment/dcms-api -n dcms-dev
kubectl rollout status deployment/dcms-frontend -n dcms-dev
```

### Deployment did not take effect

```bash
# Confirm latest workflow run succeeded
# GitHub Actions -> Deploy to dev

kubectl get pods -n dcms-dev
kubectl describe deployment dcms-api -n dcms-dev
kubectl describe deployment dcms-frontend -n dcms-dev
```

### Unexpected behavior

```bash
# Use correlation id from API response headers or application logs
kubectl logs -n dcms-dev deployment/dcms-api --tail=500 | grep '<correlation-id>'
```

---

## 9. Change Log

| Date | Changed By | Summary |
|---|---|---|
| 2026-04-23 | Codex | Initial authored version from current Compose, Helm, and workflow configuration |

