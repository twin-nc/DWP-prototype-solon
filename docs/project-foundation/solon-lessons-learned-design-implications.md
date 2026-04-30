# Solon Lessons Learned and Design Implications

> **Status:** Evolving design-input document.
> **Purpose:** Capture verified Solon Tax and Amplio platform findings that should shape DCMS design decisions.
> **Authority note:** Findings in this document must be grounded in `external_sys_docs/` or verified implementation evidence. Do not add unsourced assumptions.
> **Last updated:** 2026-04-30

---

## How to Use This Document

Use this document when designing Release 1 capabilities on top of Solon Tax. Each lesson records:

- the verified platform fact,
- the design implication for DCMS,
- the practical design rule to carry forward,
- the source evidence used.

This is not a replacement for ADRs. If a lesson leads to a locked architecture decision, create or update the relevant ADR or decision record.

---

## Lesson 1 - Amplio Parallel Constructs Are Not True Concurrency Inside One Process

### Verified Platform Fact

Amplio supports BPMN parallel gateways and multi-instance activities only with sequential execution semantics:

- Parallel gateway branches execute sequentially, not concurrently.
- Multi-instance activities are sequential only.
- Message boundary events are interrupting only and do not buffer early messages.

This does **not** mean the platform can only run one account at a time. It means concurrency must not be assumed **inside a single Amplio process instance**.

### Design Implication

DCMS should not design one account-level process where correctness depends on multiple BPMN branches being active or progressing concurrently.

For example, avoid modelling one account process as:

```text
Account process
  -> branch A monitors payments
  -> branch B runs contact strategy
  -> branch C waits for vulnerability or restriction events
  -> branch D handles placement/reconciliation
all assumed to run concurrently
```

In Amplio, those branches may be processed sequentially. That can make timing, SLA, retry, and state assumptions misleading.

### What Is Still Safe

The following remain valid design patterns:

- One account, debt, or case has its own process instance.
- Many account process instances can be ongoing at the same time.
- Exclusive decisions such as "if vulnerable choose path A, else choose path B" are fine; that is an exclusive gateway, not a parallel gateway.
- Batch jobs, Kafka/outbox fan-out, and worker services can process many accounts concurrently outside a single BPMN process instance.

### Practical Design Rule

Design parallelism outside a single Amplio process instance.

Use Amplio for account-level lifecycle state, guided workflows, human tasks, and explicit state transitions. Use batch jobs, asynchronous services, workers, Kafka/outbox flows, or separate process instances for bulk, concurrent, fan-out, or independently retryable work.

For high-volume intake, the preferred shape is:

```text
Batch/job receives many accounts
        |
        v
Create independent account work items
        |
        v
Workers process items concurrently
        |
        v
Create or update one account/case/process per account
        |
        v
Track failures per account with reason codes
```

Do not model a batch of accounts as one BPMN process containing thousands of parallel branches or a parallel multi-instance activity.

### Release 1 Areas Affected

This lesson affects design for:

- batch intake and validation,
- enrichment and risk scoring,
- queue assignment and work allocation,
- campaign contact generation,
- historical simulation and replay,
- champion/challenger split assignment,
- bulk account moves,
- SLA and dashboard timing,
- third-party placement and reconciliation fan-out.

### Source Evidence

- `external_sys_docs/solon/solon_tax_2.3.0_integration_guide.md:150` - Parallel Gateway executes sequentially.
- `external_sys_docs/solon/solon_tax_2.3.0_integration_guide.md:167` - Message Boundary is interrupting only; no early-message buffering.
- `external_sys_docs/solon/solon_tax_2.3.0_integration_guide.md:201-203` - Parallel Gateway, Multi-Instance Activity, and Message Boundary Event differences from Camunda 8.
- `external_sys_docs/solon/solon_tax_2.3.0_operations_guide.md:245-247` - Operations guide repeats the same limitations.
- `external_sys_docs/solon/solon_tax_2.3.0_integration_guide.md:421` - Example notes that a BPMN-modeled parallel flow is executed sequentially by Amplio.

### Open Design Question

For each Release 1 workflow, identify whether concurrent behaviour is:

- unnecessary,
- needed only across separate accounts,
- needed inside a single account lifecycle.

Only the third case requires a specific workaround design.

