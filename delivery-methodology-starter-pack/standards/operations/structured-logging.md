---
id: STD-OPS-004
title: Structured Logging
status: Approved
owner: SRE / Platform Engineering
applies_to: All services and portals
release_impact_if_violated: Release-blocking — logs that cannot be queried in production are operationally blind
related_standards:
  - operations/observability-and-signal-to-noise.md
  - security/data-sensitivity-and-redaction.md
last_changed: 2026-04-13
---

## Purpose

Define the minimum structured logging requirements so that logs are machine-queryable in any log aggregation platform (Loki, Elasticsearch, Splunk, Azure Monitor, etc.) and every request can be traced end-to-end via a correlation ID. This standard is more specific than STD-OPS-001, which governs higher-level observability strategy. STD-OPS-004 governs how individual log entries must be shaped.

---

## Rule Set

### OPS-004-R1 — JSON to stdout, never to files

All logs MUST be written to `stdout` or `stderr` in JSON format. Writing logs to files is prohibited.

**Why:** Container runtimes and log aggregation agents collect `stdout`/`stderr` automatically. File-based logging requires volume mounts, log rotation configuration, and additional sidecar agents — all operationally expensive and unnecessary for containerised workloads.

### OPS-004-R2 — Minimum required fields per log entry

Every log entry MUST include at minimum these four fields:

| Field | Type | Format / Values | Description |
|---|---|---|---|
| `timestamp` | string | ISO 8601: `2026-04-13T10:15:30.123Z` | When the event occurred |
| `level` | string | `debug` / `info` / `warn` / `error` | Severity |
| `message` | string | Human-readable | Description of the event |
| `correlationId` | string | UUID | Unique ID for the originating request |

Example of a compliant log entry:

```json
{
  "timestamp": "2026-04-13T10:15:30.123Z",
  "level": "info",
  "message": "Invoice accepted and queued for dispatch",
  "correlationId": "3f4a8b2c-d91e-4f6a-b823-1234567890ab"
}
```

Additional fields (service name, environment, HTTP method, path, duration, exception class, stack trace) are encouraged where relevant but are not mandated by this standard.

### OPS-004-R3 — correlationId must span the full request lifecycle

A `correlationId` MUST be included in every log entry produced during a request. The rules for determining the ID are:

1. **Accept from incoming header if present** — check for `X-Correlation-ID` on the incoming request (or the project-agreed header name, documented in `CLAUDE.md`)
2. **Generate a new UUID if absent** — if no header is present, generate a new `UUID v4`
3. **Propagate through the request** — every log statement during the request MUST use the same `correlationId`
4. **Propagate to outbound calls** — when making downstream service calls, forward the `correlationId` in the outbound request header

**Why:** Without a stable correlation ID, a single user-visible failure may produce dozens of log entries that cannot be connected to each other or to the originating request. Diagnosing such failures under production pressure is extremely expensive.

### OPS-004-R4 — Log level must be runtime-configurable

The minimum log level MUST be controllable via an environment variable (`LOG_LEVEL` or the project-agreed equivalent). The default production level MUST be `info`. Switching to `debug` MUST NOT require a redeployment.

```bash
# .env.example
# Log level: debug | info | warn | error (default: info)
LOG_LEVEL=info
```

### OPS-004-R5 — Sensitive data must not appear in logs

Sensitive data MUST NOT appear in any log entry. This includes:

- Passwords and credentials
- API keys and tokens
- Personal Identifiable Information (PII)
- Payment card data

See `security/data-sensitivity-and-redaction.md` for the full data classification and redaction rules. When logging request or response bodies, redact sensitive fields before writing the log entry — never log and redact later.

---

## Log Level Guidelines

| Level | Use for |
|---|---|
| `debug` | Detailed internal state — not emitted in production by default |
| `info` | Normal operations: requests received, jobs completed, migrations run |
| `warn` | Something unexpected that did not cause failure: retried once, config default used, deprecated path called |
| `error` | Failure requiring attention: uncaught exceptions, dependency outages, data consistency errors |

Do not emit `error` for user-caused failures (e.g., validation errors, 404s, 401s). These are `info` or `warn`.

---

## Framework Implementation Notes

These notes are informational. The requirement is the standard above; implementation details vary by language and framework.

### Spring Boot (Java)

Use `logstash-logback-encoder` to produce JSON output:

```xml
<dependency>
  <groupId>net.logstash.logback</groupId>
  <artifactId>logstash-logback-encoder</artifactId>
  <version>7.4</version>
</dependency>
```

Configure `logback-spring.xml` to use `LogstashEncoder` for the console appender. Use MDC to propagate `correlationId`:

```java
// In a request filter / interceptor
MDC.put("correlationId", resolvedCorrelationId);
// logstash-logback-encoder will include all MDC fields in every log entry automatically
```

### Node.js / Next.js

Use `pino` with JSON output:

```js
import pino from 'pino';
const logger = pino({ level: process.env.LOG_LEVEL || 'info' });
```

Use `AsyncLocalStorage` (or a request-scoped context library) to propagate `correlationId` through the async call chain without passing it explicitly to every function.

---

## Handoff Gate

- [ ] All logs written to `stdout` in JSON format — no file appenders, no plain-text formatters
- [ ] Every log entry includes `timestamp`, `level`, `message`, `correlationId`
- [ ] `correlationId` accepted from incoming request header or generated as UUID if absent
- [ ] `correlationId` propagated to every log entry for the duration of the request
- [ ] `correlationId` forwarded in outbound request headers to downstream services
- [ ] `LOG_LEVEL` environment variable controls minimum log level without redeployment
- [ ] No sensitive data (credentials, PII, tokens) appears in any log entry

---

## Related Documents

- `standards/operations/observability-and-signal-to-noise.md` — STD-OPS-001: higher-level observability strategy, metrics, alerting
- `security/data-sensitivity-and-redaction.md` — what must never appear in logs
- `templates/DEV-DEVOPS-HANDOFF-CHECKLIST.md`
