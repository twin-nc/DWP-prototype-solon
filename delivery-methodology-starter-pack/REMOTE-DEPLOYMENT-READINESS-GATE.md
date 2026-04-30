# Remote Deployment Readiness Gate

## Purpose
Before any service is deployed to a remote environment for the first time — and before any change that alters infrastructure, routing, or GitOps configuration — this gate must pass. Its purpose is to prevent the most expensive remote deployment failure modes:

1. **Agents working blind** — no remote environment documentation means every debug session starts from first principles.
2. **Log access discovered late** — inability to read infrastructure or solution logs on remote multiplies the detect-to-fix cycle, sometimes by an order of magnitude.
3. **Local/remote config drift** — when local and remote diverge silently, bugs appear only on remote and cannot be reproduced locally.
4. **GitOps surprises** — Helm or routing changes that were not communicated to the team break remote deployments in ways that are slow to diagnose.

**Owner:** DevOps / Release Engineer  
**Reviewer:** SRE / Platform Engineer  
**Required for:** First deployment to any remote environment; any change to infrastructure, routing, Helm charts, or GitOps configuration.

---

## Gate Checklist

### A — Remote Environment Documentation

- [ ] `docs/project-foundation/remote-environment-spec.md` exists and is current
- [ ] The spec covers: infrastructure topology, GitOps sync mechanism, routing and ingress rules, all environment-specific config differences from local
- [ ] The spec was reviewed and approved by DevOps/RE before this deployment
- [ ] Agents have been directed to read this document before any remote debugging session

### B — Log Access

- [ ] Infrastructure logs (container runtime, Kubernetes events, API gateway) are accessible to the team
- [ ] Solution logs (application-level structured logs) are accessible to the team
- [ ] Log access has been tested — not just provisioned — by at least one team member
- [ ] Log access method is documented in `remote-environment-spec.md` (e.g., kubectl logs, Loki/Grafana, log aggregation URL)
- [ ] Agents can reach logs programmatically or through documented manual steps during a debug session

> **Why this is a hard gate:** If logs are not accessible before deployment, the first production bug will be debugged without them. The team will improvise log access under time pressure — a predictably worse outcome than establishing access beforehand.

### C — Local/Remote Parity

- [ ] A local/remote parity statement exists (see STD-PLAT-008)
- [ ] All documented divergences have a rationale; undocumented divergences have been resolved
- [ ] Local development setup can reproduce the failure conditions that will occur on remote (config shape, auth flow, service dependencies)
- [ ] `docker-compose.yml` (or equivalent) and Helm charts have been reviewed for config shape parity

### D — GitOps and Deployment Workflow

- [ ] GitOps sync mechanism is documented and the team understands when it triggers vs. when a manual sync is needed
- [ ] Helm chart change protocol has been communicated to all active team members
- [ ] Routing rules for this deployment are documented; any changes from previous state are explicit
- [ ] The team knows who to contact if a GitOps sync fails or a routing change does not propagate

### E — Rollback

- [ ] Rollback procedure is documented in the relevant runbook or release evidence pack
- [ ] Rollback has been tested or a dry-run procedure is documented
- [ ] The team can execute a rollback without needing to diagnose the root cause first

---

## Sign-off Block

Before promoting to remote, record this in the linked GitHub issue or PR:

```
## Remote Deployment Readiness Gate
- Gate A (Remote environment docs): ✅ Complete / ❌ Incomplete — [reason]
- Gate B (Log access): ✅ Verified / ❌ Not verified — [reason]
- Gate C (Local/remote parity): ✅ Confirmed / ❌ Gaps — [list]
- Gate D (GitOps/routing): ✅ Communicated / ❌ Pending — [reason]
- Gate E (Rollback): ✅ Documented / ❌ Deferred — [reason and Delivery Lead sign-off]

Remote deployment approved: ✅ / ❌
Approved by: [DevOps/RE name]
Date: [YYYY-MM-DD]
```

---

## Integration with Other Procedures

- The remote environment spec required by Gate A is created from `templates/REMOTE-ENVIRONMENT-SPEC-TEMPLATE.md`.
- Log access requirements feed into the observability standard (STD-OPS-001).
- Local/remote parity requirements are governed by STD-PLAT-008.
- For emergency deployments: Gates B and D are non-negotiable even under time pressure. Gates C and E may be deferred with explicit Delivery Lead sign-off, with a mandatory retroactive issue within 24 hours. See `EMERGENCY-BYPASS-PROCEDURE.md`.
