---
id: STD-PLAT-011
title: Local + Dev Deployment Baseline (k3d / AKS)
status: Approved
owner: DevOps / Release Engineer
applies_to: All projects with k3d local and AKS dev deployment
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - platform/local-dev-environment.md
  - platform/local-remote-parity.md
  - platform/containerization.md
  - operations/ci-cd-secret-management.md
  - operations/health-endpoints.md
last_changed: 2026-04-16
---

## Purpose

This standard defines the non-functional baseline for deploying to the two supported pre-production environments:

- **local** — developer machine, k3d cluster
- **dev** — shared AKS cluster hosted in Azure

No other environment types are governed by this standard. Higher environments (staging, production) are out of scope here and require separate NFR documents.

---

## Local Cluster NFRs (k3d)

### NFR-LC-01 — Single-command bootstrap

A clean developer machine MUST be able to create the k3d cluster and bootstrap all prerequisites from one documented command. No manual pre-steps beyond tool installation are permitted.

### NFR-LC-02 — Idempotency

Local bootstrap and deploy scripts MUST be re-runnable without manual cleanup. Re-running on an already-provisioned cluster MUST NOT produce duplicate or broken resources.

### NFR-LC-03 — Fast failure on missing prerequisites

If any required tool (`docker`, `kubectl`, `helm`, `k3d`) is absent or below the minimum supported version, the script MUST exit immediately with a non-zero code and print actionable install guidance. Silent failures or degraded runs are prohibited.

### NFR-LC-04 — Local ingress usability

Any hostname routing required for local ingress MUST be either fully automated (e.g. `/etc/hosts` entry injected by the bootstrap script) or clearly documented in the runbook with exact commands. A developer MUST NOT need to discover this by trial and error.

### NFR-LC-05 — Deterministic local images

Local deployment MUST support using locally built or pre-loaded images without forced pulls from a remote registry. The build and load workflow MUST be documented in the runbook.

### NFR-LC-06 — Resource baseline documented

The runbook MUST document the minimum local machine resources (CPU cores, RAM) required to run the full local cluster. This MUST be verified against the actual resource profile of a standard developer machine.

---

## Build NFRs

### NFR-BLD-01 — Interactive and non-interactive support

The build script MUST support both interactive use (developer terminal) and non-interactive invocation (CI pipeline) without modification. Non-interactive mode MUST be triggered by a documented flag or environment variable.

### NFR-BLD-02 — Selective build

The build script MUST support building all components and a named subset of components. A developer MUST be able to rebuild a single service without triggering a full build.

### NFR-BLD-03 — Traceable image tags

Every built image tag MUST be traceable to a source revision. Tags MUST be either SHA-based (e.g. `git rev-parse --short HEAD`) or an explicit release tag. `latest` MUST NOT be used as the sole tag for any built image.

### NFR-BLD-04 — Fail-fast on build or push failure

A build or push failure MUST return a non-zero exit code and halt all further steps immediately. Silent continuation after a failed step is prohibited.

### NFR-BLD-05 — No embedded credentials

Build and push scripts MUST NOT hardcode registry credentials, cloud credentials, or service principal details. All credentials MUST be supplied via environment variables or a secrets manager, and MUST be registered in the CI Secret Register (`templates/CI-SECRET-REGISTER.md`).

---

## Deploy NFRs (Local + Dev)

### NFR-DPL-01 — Single-command deploy

The deploy script MUST support a one-command path that resolves Helm dependencies and performs the full deployment. No additional manual steps between invocation and a running deployment are permitted.

### NFR-DPL-02 — Two overlays only

Deployment configuration MUST use exactly two overlays on top of a shared base:

- `values-local.yaml` — local (k3d) overrides
- `values-dev.yaml` — dev (AKS) overrides

Additional overlays require a deviation record.

### NFR-DPL-03 — Release safety on dev

Dev deployments MUST use rollback-safe Helm behaviour (`--atomic` or equivalent). A failed upgrade MUST automatically roll back to the last known-good release without manual intervention.

### NFR-DPL-04 — Configurable timeout

The Helm/deploy timeout MUST be configurable via a script parameter or environment variable. The default value MUST be documented in the runbook. Hardcoded timeouts are prohibited.

### NFR-DPL-05 — Readiness gate

Deployment MUST be considered failed if any critical workload does not reach a ready state within the configured timeout. The script MUST exit non-zero in this case; it MUST NOT report success while pods are crashing or pending.

### NFR-DPL-06 — Context guard

Before any deployment, the script MUST print the current kube context and target namespace and prompt for confirmation (or accept an explicit `--yes` flag in non-interactive mode). Deploying to the wrong cluster due to an active context is a P1 incident risk.

### NFR-DPL-07 — Diagnostics on failure

On deployment failure, the script MUST automatically output diagnostic information before exiting. Required output:

- Pod status (`kubectl get pods -n <namespace>`)
- Recent events (`kubectl get events -n <namespace> --sort-by=.lastTimestamp`)
- Rollout/log hints for any non-ready pods

---

## Dev (AKS on Azure) NFRs

### NFR-DEV-01 — AKS as the only shared remote target

The dev environment is AKS in Azure. It is the only shared remote target supported by this baseline. No other cloud or shared environments are valid deploy targets under this standard.

### NFR-DEV-02 — Pinned image tags on dev

Dev deployments MUST use explicit image tags. Implicit `latest` behaviour is prohibited. Tags MUST satisfy NFR-BLD-03 (traceable to a source revision).

### NFR-DEV-03 — Secrets from Kubernetes / external secret integration

Dev secrets MUST be sourced from Kubernetes Secrets or an external secret integration (e.g. Azure Key Vault via External Secrets Operator). Plaintext credentials in values files committed to source control are prohibited. See `standards/operations/ci-cd-secret-management.md`.

### NFR-DEV-04 — Documented access model

The runbook MUST document the kubeconfig and service principal setup required for deployment automation to function. A new team member MUST be able to configure access without tribal knowledge.

---

## Documentation and Verification NFRs

### NFR-OPS-01 — Runbook completeness

The project runbook MUST cover all of the following sections. Each section must contain working commands, not prose placeholders:

| Section | Required Content |
|---|---|
| Prerequisites | Tools, minimum versions, access requirements |
| Bootstrap | Cluster creation + prerequisite setup command(s) |
| Build | How to build all components and individual components |
| Deploy | How to deploy to local and dev |
| Verify | Pod health check commands + service health endpoint checks |
| Rebuild after change | How to rebuild and redeploy a single changed service |
| Teardown | How to destroy the local cluster cleanly |
| Troubleshooting | Common failure modes with diagnostic commands |

### NFR-OPS-02 — Version policy documented

The runbook MUST document the minimum supported versions for all critical tools (`docker`, `kubectl`, `helm`, `k3d`) and any Helm chart dependencies. This MUST be kept current with the actual version requirements of the scripts.

### NFR-OPS-03 — Verification contract

Standard post-deploy verification MUST include at minimum:

1. Pod health: all critical pods in `Running` state, zero restarts
2. Service health: HTTP `GET /health/ready` returns `200` for all services that expose a readiness endpoint (see `standards/operations/health-endpoints.md`)

### NFR-OPS-04 — Command-based acceptance checks

Each NFR group (LC, BLD, DPL, DEV, OPS) MUST have at least one concrete, command-based acceptance check documented in either the runbook or a `verify/` script in the repository. Acceptance checks MUST be runnable by any team member with standard access.

---

## Handoff Gate

Before handing off deployment scripts/runbook for team use:

- [ ] Bootstrap is idempotent and single-command (NFR-LC-01, NFR-LC-02)
- [ ] Missing prerequisites produce fast failure with install guidance (NFR-LC-03)
- [ ] Local ingress hostname routing is automated or documented (NFR-LC-04)
- [ ] Local image load workflow is documented (NFR-LC-05)
- [ ] Minimum resource requirements are in the runbook (NFR-LC-06)
- [ ] Build script supports interactive and non-interactive modes (NFR-BLD-01)
- [ ] Build script supports selective component builds (NFR-BLD-02)
- [ ] All image tags are traceable; `latest`-only tags are absent (NFR-BLD-03)
- [ ] Build failure halts immediately with non-zero exit (NFR-BLD-04)
- [ ] No credentials hardcoded in scripts; all registered in CI Secret Register (NFR-BLD-05)
- [ ] Single-command deploy path works end-to-end (NFR-DPL-01)
- [ ] Only `values-local.yaml` and `values-dev.yaml` overlays exist (NFR-DPL-02)
- [ ] Dev deploy uses `--atomic` or equivalent rollback safety (NFR-DPL-03)
- [ ] Timeout is configurable and documented (NFR-DPL-04)
- [ ] Deploy fails non-zero if workloads do not become ready (NFR-DPL-05)
- [ ] Context guard prints and confirms kube context before deploy (NFR-DPL-06)
- [ ] Diagnostics are printed automatically on failure (NFR-DPL-07)
- [ ] Dev secrets sourced from Kubernetes Secrets / External Secrets, not values files (NFR-DEV-03)
- [ ] Runbook covers all eight required sections (NFR-OPS-01)
- [ ] Tool version policy is documented (NFR-OPS-02)
- [ ] Verification contract covers pod health + readiness endpoints (NFR-OPS-03)
- [ ] Each NFR group has at least one command-based acceptance check (NFR-OPS-04)

---

## Related Documents

- `standards/platform/local-dev-environment.md` — STD-PLAT-010: docker-compose local stack requirements
- `standards/platform/local-remote-parity.md` — STD-PLAT-008: config and topology parity rules
- `standards/platform/containerization.md` — STD-PLAT-009: Dockerfile and image requirements
- `standards/operations/ci-cd-secret-management.md` — secret register and CI pipeline secret rules
- `standards/operations/health-endpoints.md` — STD-OPS-003: readiness/liveness endpoint semantics
- `templates/RUNBOOK-TEMPLATE.md` — runbook section template
- `templates/CI-SECRET-REGISTER.md` — credential registration template
- `REMOTE-DEPLOYMENT-READINESS-GATE.md` — gate checklist for dev promotion
