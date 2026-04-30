# Remote Environment Specification

> **Owner:** DevOps / Release Engineer  
> **Created:** `{{YYYY-MM-DD}}`  
> **Last reviewed:** `{{YYYY-MM-DD}}`  
> **Required reading for:** Any agent or team member working on remote deployment, debugging remote issues, or changing infrastructure/routing configuration.
>
> This document is the single source of truth for how the remote environment is structured. Agents must read it before any remote debugging session. It must be kept current — stale information here is worse than no information.

---

## 1. Environment Inventory

| Environment | Purpose | URL / Endpoint | Maintained By | Notes |
|---|---|---|---|---|
| `dev` | Continuous deployment from `main` | `{{URL}}` | `{{NAME}}` | |
| `staging` | Pre-release validation | `{{URL}}` | `{{NAME}}` | |
| `production` | Live | `{{URL}}` | `{{NAME}}` | |
| `{{other}}` | `{{purpose}}` | `{{URL}}` | `{{NAME}}` | |

---

## 2. Infrastructure Topology

Describe the infrastructure layout. Include a diagram if available (Mermaid or linked image).

```
{{e.g.}}
Internet → [Cloudflare / CDN] → [API Gateway (Kong / nginx)] → [Services (k3s / ECS / AKS)]
                                                                      ↓
                                                             [PostgreSQL RDS]
                                                             [Secrets (Vault / Key Vault)]
                                                             [Observability stack]
```

**Container runtime:** `{{e.g., k3s v1.28 / ECS / AKS}}`  
**Namespace(s):** `{{list}`}  
**Node count and sizing:** `{{e.g., 3 × t3.medium}}`  
**Region:** `{{e.g., eu-west-1}}`

---

## 3. GitOps Sync Mechanism

| Item | Detail |
|---|---|
| GitOps tool | `{{e.g., Argo CD / Flux / manual Helm}}`  |
| Sync trigger | `{{e.g., automatic on push to main / manual sync required}}`  |
| Sync repo / path | `{{e.g., github.com/org/infra-repo, path: environments/staging}}`  |
| Sync frequency | `{{e.g., continuous / every 3 minutes / manual only}}`  |
| How to trigger a manual sync | `{{e.g., argocd app sync <app-name> / kubectl apply / UI step}}`  |
| How to verify sync completed | `{{e.g., argocd app status <app-name> / watch kubectl rollout status}}`  |

> **Important:** Do not assume a config change has propagated until you have verified sync completion using the method above. Many remote debugging sessions begin with the assumption that the latest config is live when it is not.

---

## 4. Routing and Ingress

| Route / Path | Service | Port | Auth Required | Notes |
|---|---|---|---|---|
| `{{e.g., /api/v1/*}}` | `{{service-name}}` | `{{8080}}` | `{{Yes — JWT / No}}` | |
| `{{e.g., /admin/*}}` | `{{service-name}}` | `{{8081}}` | `{{Yes — admin role}}` | |

**Ingress controller:** `{{e.g., Kong Ingress Controller / nginx-ingress}}`  
**TLS termination:** `{{e.g., at ingress / at load balancer}}`  
**DNS:** `{{how DNS is managed and where records are updated}}`

> **Helm chart changes affecting routing:** Any routing change requires DevOps/RE review before deployment. See `WAYS-OF-WORKING.md §GitOps and Remote Deployment`.

---

## 5. Log Access

This section must be complete before the first remote deployment. Log access that has not been tested should be marked `⚠️ Untested`.

### 5.1 Infrastructure Logs

| Log Source | How to Access | Access Level Required | Notes |
|---|---|---|---|
| Kubernetes events | `{{e.g., kubectl get events -n <namespace>}}` | `{{kubeconfig with namespace access}}` | |
| Pod logs | `{{e.g., kubectl logs -n <namespace> <pod> -f}}` | `{{kubeconfig}}` | |
| API Gateway logs | `{{e.g., Grafana → Loki → label: app=kong / CloudWatch log group}}` | `{{Grafana viewer / AWS console}}` | |
| Node / host logs | `{{e.g., kubectl debug node/<node> / SSH}}` | `{{cluster admin}}` | |

### 5.2 Solution Logs (Application-Level)

| Service | Log Format | How to Access | Retention | Notes |
|---|---|---|---|---|
| `{{service-name}}` | `{{JSON structured / plain text}}` | `{{e.g., Grafana → Loki → {app="service-name"} / kubectl logs}}` | `{{e.g., 30 days}}` | |

**Log aggregation system:** `{{e.g., Loki + Grafana at https://grafana.{{env}}.{{domain}} / CloudWatch / Datadog}}`  
**Access provisioning:** `{{how a new team member gets log access — who to ask, what role/group is required}}`

> **Agent sessions:** When debugging a remote issue, an agent must be directed to the log access method above before attempting to diagnose. The log access commands should be copy-pasteable. Do not assume an agent will discover log access independently.

---

## 6. Environment-Specific Configuration

Document only the differences from local development. If a value is the same locally and remotely, omit it.

| Config Item | Local Value | Remote Value | Notes |
|---|---|---|---|
| `AUTH_ISSUER_URL` | `http://localhost:8080/auth` | `https://auth.{{domain}}` | |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/db` | `{{RDS endpoint}}` | |
| `FEATURE_FLAG_X` | `true` | `false` | Disabled pending rollout |
| `{{config key}}` | `{{local}}` | `{{remote}}` | |

**Secrets management on remote:** `{{e.g., OpenBao / AWS Secrets Manager / Azure Key Vault — how secrets are injected into pods}}`

---

## 7. Access and Credentials

| Access Type | How to Obtain | Who Grants It | Required For |
|---|---|---|---|
| kubeconfig | `{{e.g., DevOps/RE creates and shares per-developer config}}` | `{{NAME}}` | kubectl, pod logs |
| Grafana / log UI | `{{e.g., SSO via GitHub org membership}}` | `{{NAME}}` | Log access |
| Database (read-only) | `{{e.g., request from DevOps/RE for incident debugging only}}` | `{{NAME}}` | Incident debugging |
| Argo CD / GitOps UI | `{{e.g., SSO}}` | `{{NAME}}` | Sync management |

---

## 8. Common Remote Debugging Runbook

> Quick reference for the most common remote debugging scenarios. Each entry should be copy-pasteable.

### Service is not responding

```bash
# 1. Check pod status
kubectl get pods -n {{namespace}}

# 2. Check recent events
kubectl get events -n {{namespace}} --sort-by='.lastTimestamp' | tail -20

# 3. Check pod logs (last 100 lines)
kubectl logs -n {{namespace}} deployment/{{service-name}} --tail=100

# 4. Check if GitOps sync is current
{{sync check command}}
```

### Unexpected behavior / wrong response

```bash
# 1. Get structured logs for the request (use correlation ID from response)
{{e.g., Grafana LogQL: {app="{{service-name}}"} |= "{{correlationId}}"}}

# 2. Check feature flags in effect
{{how to inspect active feature flags on remote}}

# 3. Check config actually applied
kubectl get configmap -n {{namespace}} {{configmap-name}} -o yaml
```

### Deployment did not take effect

```bash
# 1. Check rollout status
kubectl rollout status deployment/{{service-name}} -n {{namespace}}

# 2. Check GitOps sync
{{sync status command}}

# 3. Force sync if needed (GitOps tool specific)
{{force sync command}}
```

---

## 9. Change Log

| Date | Changed By | Summary |
|---|---|---|
| `{{YYYY-MM-DD}}` | `{{NAME}}` | Initial version |
