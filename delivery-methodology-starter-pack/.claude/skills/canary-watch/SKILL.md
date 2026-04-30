---
name: canary-watch
description: >
  Monitor a deployed URL for regressions after a deploy, merge, or dependency
  upgrade — checking HTTP status, console errors, performance metrics, content
  integrity, and API health against a known baseline.
invocation: /canary-watch
inputs:
  - name: environment
    required: false
    description: "local | dev (default: dev). Controls pre-check method and target URLs."
  - name: target_url
    required: false
    description: "The deployed URL to monitor. Required when environment=dev. Omit when environment=local — URLs are derived from docker-compose defaults."
  - name: baseline_url
    required: false
    description: URL to diff against (e.g. previous deploy or staging vs prod)
  - name: mode
    required: false
    description: "quick | sustained | diff (default: quick)"
  - name: interval_seconds
    required: false
    description: Check interval for sustained mode (default 60)
  - name: duration_minutes
    required: false
    description: How long to run sustained mode (default 10)
outputs:
  - name: watch_report
    description: Per-metric status, delta from baseline, alert classification, and recommended action
roles:
  - devops-release-engineer
  - sre-platform-engineer
---

# Canary Watch

## Purpose
After a deploy it is easy to assume success because the pod started. This skill performs a structured post-deploy check against a live URL, measuring what users actually experience rather than what Kubernetes reports. It catches regressions that smoke tests miss — silent API errors, performance regressions, broken content — before they affect real users.

## Authentication Note

The DCMS application is fully protected by Keycloak. Every application route except `/actuator/health` redirects unauthenticated requests to the Keycloak login page (HTTP 302). This skill **does not acquire tokens** and therefore cannot meaningfully check authenticated routes. Scope all checks to:
- `/actuator/health` (Spring Boot — no auth required)
- Keycloak `/health/ready` (Keycloak — no auth required)
- Static asset delivery (200 on the frontend nginx root)

Do not interpret a 302 from an application route as healthy or unhealthy — it is always expected for unauthenticated requests. Checking authenticated API behaviour requires a separate token acquisition step outside this skill's scope.

## When to Use
- After `docker compose up` completes locally (`environment=local`)
- Immediately after deploying to `dev` (AKS) (`environment=dev`)
- After merging a feature branch to `main` and the dev pipeline completes
- After upgrading a dependency (Spring Boot version bump, Keycloak upgrade, npm package update)
- When a Helm values change is applied to the cluster
- As a manual gate before raising a PR for environment promotion

## Do Not Use This Skill When
- **The service has not yet started** — wait for startup to complete first. For local Docker Compose failures (unhealthy containers, port conflicts, volume errors, Keycloak import failures), use `docker-compose-debugger`. Use `java-build-resolver` if the `api` container is crashing with a Java stack trace specifically.
- You need deep functional testing of a specific workflow — use `generate-e2e-scenarios`
- The failure is already known and you are fixing it — use `ci-failure-root-cause-fixer` or `java-build-resolver`

## Watch Modes

### Quick Check (default)
A single evaluation pass across all monitoring dimensions. Returns a result within 30 seconds. Use after every deploy or stack start as a minimum bar.

### Sustained Watch
Repeated checks at a configurable interval over a set duration. Use when a regression is intermittent or when monitoring a rolling deploy in progress. Records metric trends over time and flags instability (variance, not just threshold breach). Include a `Trend` column in the output table when using this mode.

### Diff Mode
Side-by-side comparison between two URLs — typically staging vs production, or the new deploy vs the previous one. Surfaces deltas rather than absolute thresholds. Use when absolute thresholds are not yet established for a new endpoint.

## Monitoring Dimensions

| Dimension | What to Check | Critical Threshold | Warning Threshold | Info Threshold |
|---|---|---|---|---|
| HTTP status | Response code on unauthenticated routes | Not 200/301/302 | Any 4xx on non-auth route | — |
| Console errors | Browser console error count | >5 new errors | Any new errors vs baseline | — |
| Network failures | Failed resource/API requests | Any 5xx from own APIs | Any failed third-party request | — |
| LCP | Largest Contentful Paint (Lighthouse CLI) | >4 s | >2.5 s | 2–2.5 s regression vs baseline |
| CLS | Cumulative Layout Shift (Lighthouse CLI) | >0.25 | >0.1 | >0.05 regression vs baseline |
| Content integrity | Key page elements present | Missing heading or primary action | Missing secondary elements | Any element count delta vs baseline |
| API health | `/actuator/health` endpoint | DOWN or missing | UNKNOWN | — |
| Keycloak health | Keycloak health endpoint | DOWN or missing | UNKNOWN | — |

**Note:** LCP and CLS checks require Lighthouse CLI (`npm install -g lighthouse`). If Lighthouse is not available in the current environment, skip those dimensions and note the gap in the report rather than leaving them silently empty.

## Steps

### Pre-check — Verify Service Readiness

#### environment=local (Docker Compose)
1. Run `docker compose ps` from the `infrastructure/` directory.
2. Confirm all four services (`db`, `keycloak`, `api`, `frontend`) show status `running` and their healthchecks show `healthy`.
3. If any service is `starting`, wait and re-check. If any service is `exited` or `unhealthy`, do not proceed — use `java-build-resolver` for `api` failures or inspect `docker compose logs <service>` for others.
4. Record the current git SHA (`git rev-parse --short HEAD`) for the report in place of a deploy SHA.
5. Use these fixed local URLs for all checks:
   - API health: `http://localhost:8081/actuator/health`
   - Frontend: `http://localhost:8080/`
   - Keycloak health: `http://localhost:9090/health/ready`

#### environment=dev (AKS)
1. Run `kubectl get pods -n <namespace>` and confirm the target pod is `Running` with all containers `Ready` (e.g. `2/2`).
2. If the pod is in `Pending`, `CrashLoopBackOff`, or `Init` state: do not proceed — use `ci-failure-root-cause-fixer` or `java-build-resolver` to resolve the startup failure first.
3. Record the pod name and the deploy SHA (`kubectl get pod <name> -o jsonpath='{.metadata.annotations}'`) for the report.
4. `target_url` must be provided — this is the AKS ingress URL. If it is not yet configured (ingress host is blank in `values-dev.yaml`), stop and flag that the dev environment is not yet reachable.

### Phase 1 — Establish Baseline
1. If `baseline_url` is provided, capture its current metrics before checking the target.
2. If no baseline, use the thresholds in the table above as the reference.
3. Record the deploy SHA or git SHA being tested.

### Phase 2 — Run Checks
For each dimension in the table:
1. Make the request or measure the metric.
2. Compare to baseline or threshold.
3. Classify as Critical / Warning / Info / Pass.

### Phase 3 — Classify Alerts
- **Critical** — stop here; do not promote; raise an incident or revert the deploy.
- **Warning** — document and decide: can this ship with a follow-up issue, or must it be fixed first?
- **Info** — log for awareness; no blocking action required.
- **Pass** — no action needed.

### Phase 4 — Report and Recommend
Produce the output contract. For each Critical or Warning finding, state the recommended action (revert, hotfix, raise issue, or accept with mitigation).

## Output Contract

```
## Canary Watch Report

Environment: <local | dev>
Target:      <url or "localhost defaults">
Baseline:    <url or "threshold-based">
Mode:        <quick | sustained | diff>
Deploy/SHA:  <git SHA or Helm release>
Time:        <ISO timestamp>

| Dimension         | Result | Value        | Baseline/Threshold | Delta |
|-------------------|--------|--------------|--------------------|-------|
| HTTP status       | PASS   | 200          | 200                | —     |
| Console errors    | WARN   | 3 new errors | 0                  | +3    |
| Network failures  | PASS   | 0            | 0                  | —     |
| LCP               | PASS   | 1.8 s        | <2.5 s             | —     |
| CLS               | PASS   | 0.05         | <0.1               | —     |
| Content integrity | PASS   | all present  | all present        | —     |
| API health        | PASS   | UP           | UP                 | —     |
| Keycloak health   | PASS   | UP           | UP                 | —     |

OVERALL: PASS WITH WARNINGS / CRITICAL / PASS

Findings:
- [WARN] 3 new console errors — <describe errors> — Recommended action: raise issue #X

Next step: <promote / investigate / revert / no action>
```

## Guardrails
- Do not mark a stack as healthy if any Critical alert is open.
- Do not accept Warning findings without raising a follow-up issue — "we'll fix it later" without a ticket is not acceptable.
- Do not run canary watch against production until higher environments are in scope — current scope is `local` and `dev` only (STD-PLAT-011).
- Record the deploy SHA or git SHA in every report so findings can be correlated with git history.
- If `environment=dev` and `target_url` is not set, stop and report that the dev ingress host is not yet configured — do not guess a URL.

## Integration
- For `environment=local`: run after `docker compose up` reports all services healthy.
- For `environment=dev`: run after `remote-deployment-readiness` confirms prerequisites are met.
- Use `ci-failure-root-cause-fixer` if a regression traces back to a build or test failure.
- Use `java-build-resolver` if the regression is a Spring Boot startup failure.
