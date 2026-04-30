---
id: STD-OPS-001
title: Observability and Signal-to-Noise
status: Approved
owner: SRE / Operations
applies_to: All services, portals, and pipelines
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - integration/integration-reliability-and-reconciliation.md
  - platform/evidence-immutability-and-replay.md
  - security/data-sensitivity-and-redaction.md
last_changed: 2026-04-07
---

## Purpose

Provide actionable operational signals without drowning engineers in noise or leaking sensitive data.

---

## Correlation (MUST)

All logs, traces, and metrics MUST use stable correlation identifiers that link:
- Inbound request
- Domain event(s)
- Integration attempt(s)
- Reconciliation attempt(s)
- Evidence pack/artifact identifiers (where appropriate)

Use OpenTelemetry SDK for correlation across service boundaries.

---

## Logs vs Evidence

- **Logs** are for operations and debugging. They are ephemeral.
- **Evidence artifacts** are for audit, release, and legal replay. They are append-only and retained.

Do not rely on ephemeral logs as the sole audit evidence.

---

## Metrics

At minimum track:
- Request volumes
- Success/error rates by error `code` and `category`
- Retry and reconciliation counts
- Latency percentiles per endpoint/topic
- Contract parity failures (CI gate)

---

## Alerting

Alerts SHOULD be:
- **Symptom-based** (user impact, SLA breach) — not cause-based
- **Actionable** (every alert links to a runbook)
- **Rate-limited** to prevent alert storms

Every alert MUST have a named runbook. Runbooks without an alert are not required but recommended for common operational procedures.

---

## Required Runbooks

Before a service goes to production, runbooks MUST exist for:
- Service startup and health check
- Secret rotation for any secret the service uses
- Rolling back a deployment
- Diagnosing and resolving reconciliation failures
- Responding to high error rate alerts

---

## Privacy and Redaction

- Do not log sensitive payloads unless explicitly approved and access-controlled.
- Redact secrets, identifiers, and personal data per `security/data-sensitivity-and-redaction.md`.
- Log at `INFO` level by default in production. `DEBUG` logging in production requires explicit operator enablement.
