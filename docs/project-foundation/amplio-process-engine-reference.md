> ## STATUS: PRIMARY DESIGN INPUT — PLATFORM PIVOT IN EFFECT
>
> Following ADR-018 (2026-04-30), Solon Tax is the confirmed base platform for DCMS. Amplio is Solon Tax's process engine and is therefore a **primary design input** for the new design process. Read this document to understand what Amplio provides and its known constraints (particularly around non-interrupting boundary events), which must be addressed in the new architecture design.

---

# Amplio Process Engine Reference

**Source docs:** `external_sys_docs/solon/solon_tax_2.3.0_integration_guide.md` (Section 3–4, 13–16), `external_sys_docs/solon/solon_tax_2.3.0_operations_guide.md` (Section 4, 12, 13, 16)

## Product Identity

- First-party Netcompany BPMN process engine (NOT an open-source product)
- Ships as primary process engine in Solon Tax v2.3.0+
- Replaces Camunda 8 (deprecated in 2.3.0); Camunda 7 retained as legacy option
- Implements BPMN 2.0 subset — NOT full spec compliance
- State persistence: database (PostgreSQL or Oracle); distributed in-memory state via Hazelcast

## BPMN Element Support Matrix

### Fully Supported

| Element | Notes |
|---|---|
| Start Event (None) | Standard process start |
| Start Event (Message) | Trigger via Kafka message |
| Start Event (Timer) | Cron or duration expression |
| End Event (None) | Normal termination |
| End Event (Terminate) | Kills all active tokens |
| User Task | Full human task model (see below) |
| Service Task | Java delegate or HTTP connector |
| Send Task | Kafka publish via Outbox |
| Receive Task | Kafka consume, waits for correlation |
| Script Task | FEEL expressions ONLY |
| Exclusive Gateway (XOR) | Standard conditional routing |
| Event-Based Gateway | Wait on first matching event |
| Timer Boundary Event | Interrupting and non-interrupting both supported |
| Error Boundary Event | Interrupting only |
| Escalation Boundary Event | Interrupting only |
| Call Activity | Subprocess invocation (sync) |
| Embedded Subprocess | Inline subprocess |
| Intermediate Catch Event (Timer) | Supported |
| Intermediate Catch Event (Message) | Supported |
| Intermediate Throw Event (Message) | Kafka publish |
| Data Object | Supported |
| Lane / Pool | Supported (for documentation; no execution effect) |

### Partially Supported (Critical Constraints)

| Element | Constraint |
|---|---|
| **Message Boundary Event** | **INTERRUPTING ONLY. Non-interrupting NOT supported. Messages arriving before the boundary event is active are LOST (no buffering).** |
| **Parallel Gateway (AND)** | **Sequential execution only — branches execute one at a time, not concurrently. Cross-branch variable dependencies are NOT supported.** |
| **Multi-Instance Activity** | **Sequential only. Parallel multi-instance NOT supported.** |
| **Business Rule Task** | **No native DMN. Must be implemented as Service Task with custom Java calling Drools KIE.** |
| **Script Task** | **FEEL expressions only. Job worker pattern NOT supported.** |
| **HTTP Service Task** | **Query parameters must be a single JSON object string — not key-value pairs.** |

### Not Supported

| Element | Notes |
|---|---|
| Compensation events | Not implemented |
| Link events | Not implemented |
| Conditional start events | Not implemented |
| Ad-hoc subprocesses | Not implemented |
| Camunda 8 job worker pattern | Removed in 2.3.0 |
| DMN Decision Table (native) | No native DMN engine — use Drools |

## Critical Constraints vs Camunda 8 (6 Key Differences)

1. **Non-interrupting message boundary events**: NOT supported in Amplio. Camunda 8 supports them. This is the primary blocker for DCMS (Debt Respite Scheme 2020 requires non-interrupting events to allow parallel suppression).
2. **Parallel Gateway**: Sequential-only in Amplio. Camunda 8 executes branches truly concurrently.
3. **Multi-Instance**: Sequential-only in Amplio. Camunda 8 supports parallel multi-instance.
4. **Business Rule Task**: Amplio has no DMN engine — must use Drools. Camunda 8 has native DMN.
5. **Script Task**: FEEL-only in Amplio. Camunda 8 supports job workers for script tasks.
6. **Job Worker pattern**: Removed entirely in Amplio 2.3.0. Camunda 8 uses it extensively.

## Process Triggering Model

Three trigger mechanisms:
1. **Kafka message** — `StartProcessCommand` on `solon.process.commands` topic with `processDefinitionKey` and `variables` payload
2. **HTTP API** — `POST /process/start` with same payload; synchronous response with `processInstanceId`
3. **Timer** — BPMN timer start event (cron or duration); managed by Amplio scheduler

Correlation for in-flight process messages: `correlationKey` in message payload matched against active process variables.

## Human Task Model

Task types are catalogued in the integration guide §14. Key properties on every User Task:
- `assignee` — specific user (Keycloak user ID)
- `candidateGroups` — Keycloak role(s) that can claim the task
- `dueDate` — ISO 8601 datetime; drives SLA breach escalation
- `priority` — integer (0–100); drives queue ordering
- `formKey` — Angular form component key for UI rendering

Task lifecycle: `CREATED → CLAIMED → COMPLETED` (or `CANCELLED` via process termination)

Task assignment via `AssignTaskCommand` Kafka command or directly in BPMN `assignee` expression.

Escalation: Timer Boundary Event on User Task triggers ESCALATION_PROCESS via `EscalateTaskCommand`.

## Suppression / Breathing Space Model

Four-step model implemented via BPMN signal + Kafka:

1. `CreateSuppressionCommand` → Kafka → `SuppressionCreatedEvent`
2. Amplio receives signal: `SuspendCaseActivity` — suspends active tokens in case process
3. External trigger (timer or user action): `ReleaseSuppressionCommand` → Kafka → `SuppressionReleasedEvent`
4. Amplio: `ResumeCaseActivity` — resumes suspended tokens

**Critical:** Uses INTERRUPTING message boundary events. The suppression signal interrupts the current activity. This is incompatible with DCMS requirement (ADR-002) which requires non-interrupting suppression so the main process continues running while suppression is active.

`SuppressionExpiryJob` (daily 06:00) auto-releases expired suppressions.

## BPMN Versioning Behaviour

- New process definition version deployed: existing process instances continue on old version
- New instances use latest version by default
- Can pin instance to version via `version` parameter in `StartProcessCommand`
- Version tagged by `versionTag` attribute in BPMN XML
- Deployment via REST API: `POST /process/deploy` with BPMN file; or via Amplio Admin UI

## Process Variables

- Stored in database per process instance (serialised JSON)
- Accessible in FEEL expressions and Java delegates via `execution.getVariable(name)`
- Runtime override (without redeployment): Amplio Admin UI → Process Variable Override section
- Variable precedence: runtime override > process definition default > engine default

## Hazelcast Distributed State

- All active process instance state replicated across Amplio pod cluster via Hazelcast
- Minimum 3 Amplio pods for HA
- Hazelcast cluster discovery: Kubernetes service discovery (not multicast)
- If a pod fails, another pod resumes from Hazelcast state — no message loss for in-flight tokens

## Amplio Administration UI (9 Sub-Sections)

Accessed via Solon Tax staff UI under Admin → Process Engine.

| Sub-section | Capability |
|---|---|
| Process Definitions | List deployed BPMN versions; view XML; activate/suspend versions |
| Process Instances | List, search, filter; view current token position; inspect variables |
| Process Instance Detail | Token map view; variable editor; incident list |
| Human Tasks | List all active tasks; filter by assignee/group/status; force-complete or cancel |
| Incidents | List failed jobs/errors; retry or resolve; view stack trace |
| Jobs | List scheduled and async jobs; retrigger; view execution history |
| Process Variable Override | Set runtime variable overrides by process definition; takes effect on new instances |
| BPMN Deployment | Upload new BPMN files; deployment history; rollback to previous version |
| Metrics Dashboard | Active instance count, completed/failed rate, task queue depth; last 24h / 7d / 30d |

## HA Configuration

| Component | HA Mode |
|---|---|
| Amplio pods | 3+ pods minimum; Hazelcast cluster for shared in-memory state |
| Hazelcast | Embedded in Amplio pod; auto-forms cluster via K8s service discovery |
| Database (process store) | Primary + read replica; Amplio writes to primary only |
| Kafka (command/event bus) | 3 brokers, replication factor 3; Outbox Pattern ensures no message loss |

## Batch Engine Integration (Foundation Batch)

Amplio-native batch jobs are triggered and monitored via the Foundation Batch Engine (Spring Batch-based). Batch jobs can start Amplio processes via `StartProcessCommand`. Batch job status is observable via Amplio Admin UI → Jobs section and via `GET /batch/jobs/{jobId}` REST endpoint.

## Logging and Observability

- Structured JSON logs: mandatory fields `traceId`, `spanId`, `serviceId`, `correlationId`, `processInstanceId`, `activityId`
- Elastic APM agent embedded in Amplio pods; distributed trace continues across Kafka boundaries via trace context propagation
- Incident alerting: Kibana alert rules on incident count > 0 for critical process definitions

## Key Facts for DCMS Assessment

- Amplio is the reason Solon Tax cannot host DCMS: non-interrupting message boundary events are absent.
- All other Camunda 7 / Camunda 8 migration path knowledge does NOT apply — Amplio is a separate engine with its own constraints.
- Drools (no DMN) means non-technical rule authoring (ADR-008 requirement) cannot be met.
- Sequential parallel gateway means any DCMS flow requiring concurrent branch execution would silently execute sequentially — a correctness defect, not a configuration issue.
- There is no roadmap information in the 2.3.0 docs indicating non-interrupting events will be added.
