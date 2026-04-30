# NC Agentic AI Delivery — Standards Pack v1

This standards pack provides **implementation-governance standards** for building solutions aligned to the NC Agentic AI delivery methodology. The standards make delivery deterministic, auditable, replayable, and contract-safe.

---

## Authority Model

When sources conflict, follow the highest-ranked source:

1. **Applicable law and approved domain overlays** (including legal reference mappings)
2. **Master solution requirements baseline** and approved deviations
3. **This standards pack**
4. **Canonical contracts and schemas** (OpenAPI/AsyncAPI, JSON schema, event schemas)
5. **Approved test suites and release evidence definitions**
6. **Code**
7. **Examples and prose**

Standards govern *how* to implement. They cannot override law or approved requirements.

---

## How to Use

- Standards are **normative**. Use MUST/SHOULD/MAY language as defined in RFC 2119.
- Every change MUST be classified (see `governance/change-classification.md`) and linked to: requirements impacted, contracts impacted, tests/evidence updated, risk and rollout plan.
- Releases MUST satisfy evidence requirements (see `governance/release-evidence-and-signoff.md`).
- If you cannot comply with a standard, open a deviation record (see Deviation Process below).

---

## Index

### Governance
- [`governance/documentation-authority-hierarchy.md`](governance/documentation-authority-hierarchy.md) — what is authoritative when sources conflict
- [`governance/requirements-traceability-and-id-governance.md`](governance/requirements-traceability-and-id-governance.md) — requirement IDs, trace maps, end-to-end traceability
- [`governance/change-classification.md`](governance/change-classification.md) — change class taxonomy (A/B/C/D/E)
- [`governance/contract-ownership-and-change-authority.md`](governance/contract-ownership-and-change-authority.md) — who can change contracts and how
- [`governance/canonical-contract-versioning-and-parity.md`](governance/canonical-contract-versioning-and-parity.md) — contract versioning and runtime/spec parity
- [`governance/release-evidence-and-signoff.md`](governance/release-evidence-and-signoff.md) — evidence packs, signoffs, prohibited release practices
- [`governance/test-authority-and-truth-hierarchy.md`](governance/test-authority-and-truth-hierarchy.md) — what tests may assert as truth

### Development
- [`development/testing-standard.md`](development/testing-standard.md) — test coverage, naming, data management, CI gates
- [`development/accessibility-standard.md`](development/accessibility-standard.md) — STD-DEV-002: WCAG AA conformance, assistive tech test matrix, DWP Digital Design Authority sign-off (ACC01–ACC03)

### Platform
- [`platform/determinism.md`](platform/determinism.md) — identical inputs must produce identical outputs
- [`platform/date-effective-rules.md`](platform/date-effective-rules.md) — effective-dated rule selection and replayability
- [`platform/evidence-immutability-and-replay.md`](platform/evidence-immutability-and-replay.md) — append-only evidence, replay packages
- [`platform/state-resolution-precedence.md`](platform/state-resolution-precedence.md) — deriving current state from immutable history
- [`platform/immutable-records-and-corrections.md`](platform/immutable-records-and-corrections.md) — immutable legal/audit records, correction modeling
- [`platform/database-migration-standard.md`](platform/database-migration-standard.md) — migration immutability, idempotency, zero-downtime patterns
- [`platform/feature-flag-standard.md`](platform/feature-flag-standard.md) — flag lifecycle, CI requirements, cleanup obligations
- [`platform/local-remote-parity.md`](platform/local-remote-parity.md) — parity statement requirements, acceptable divergences, enforcement
- [`platform/containerization.md`](platform/containerization.md) — STD-PLAT-009: multi-stage Dockerfile, pinned images, non-root, SIGTERM, port convention
- [`platform/local-dev-environment.md`](platform/local-dev-environment.md) — STD-PLAT-010: docker-compose, service_healthy, .env.example, README requirements
- [`platform/deployment-baseline-local-dev.md`](platform/deployment-baseline-local-dev.md) — STD-PLAT-011: k3d local + AKS dev deployment baseline (build, deploy, secrets, runbook, verification NFRs)

### Integration and Operations
- [`integration/error-semantics-and-stability.md`](integration/error-semantics-and-stability.md) — error envelope, 401 vs 403, retry semantics
- [`integration/integration-reliability-and-reconciliation.md`](integration/integration-reliability-and-reconciliation.md) — idempotency, retries, reconciliation
- [`integration/file-transfer-standard.md`](integration/file-transfer-standard.md) — STD-INT-003: FTPS/PGP file exchange, TLS 1.2, SHA-256 checksums, DWP Data Integration Platform (INT07, INT12–INT26)
- [`operations/observability-and-signal-to-noise.md`](operations/observability-and-signal-to-noise.md) — correlation, metrics, alerting, privacy
- [`operations/ci-cd-secret-management.md`](operations/ci-cd-secret-management.md) — secret register, rotation, CI pipeline requirements
- [`operations/health-endpoints.md`](operations/health-endpoints.md) — STD-OPS-003: /health/live vs /health/ready semantics, Kubernetes probe requirements
- [`operations/structured-logging.md`](operations/structured-logging.md) — STD-OPS-004: JSON stdout, mandatory fields, correlationId propagation

### Security
- [`security/security-boundaries-and-fail-fast-controls.md`](security/security-boundaries-and-fail-fast-controls.md) — auth boundaries, fail-fast, least privilege
- [`security/data-sensitivity-and-redaction.md`](security/data-sensitivity-and-redaction.md) — sensitivity classes, redaction rules, evidence packs
- [`security/domain-data-classification-template.md`](security/domain-data-classification-template.md) — project-specific data classification template (generic — use domain-data-classification.md below for this project)
- [`security/domain-data-classification.md`](security/domain-data-classification.md) — **DWP Debt Collection data classification** (populated from tender requirements — requires security owner and DWP client review)

### Deviations
- [`deviations/DEVIATION_TEMPLATE.md`](deviations/DEVIATION_TEMPLATE.md) — template for all deviation records
- [`deviations/DEV-001-cots-scope.md`](deviations/DEV-001-cots-scope.md) — COTS scope boundary: which standards apply to COTS internals vs Netcompany-built services

### Responsible AI
- [`ai/agent-responsibility-boundaries.md`](ai/agent-responsibility-boundaries.md) — AI agent governance, approved tools, review requirements

---

## Deviation Process

If you cannot comply with a standard:
1. Create a deviation record under `standards/deviations/` using `standards/deviations/DEVIATION_TEMPLATE.md`.
2. Classify the deviation as release-blocking or deferrable.
3. Obtain required signoffs (see `governance/release-evidence-and-signoff.md`).
4. Ensure traceability to impacted requirements, tests, and evidence.
5. Deviations are never silent — they must appear in the release evidence pack.

---

## Amending a Standard

Standards must not be amended without team review:
1. Open a GitHub issue describing the proposed change and the reason.
2. Create a PR with the change to the standard file.
3. At least one team member with relevant expertise must approve.
4. Update the `last_changed` field in the standard's frontmatter.
5. Announce the change to the whole team before merging.

---

Version: v1.1 | Last updated: 2026-04-16 | DWP Debt Collection project additions: accessibility standard, file transfer standard, domain data classification, COTS scope deviation
