# DWP Debt Collection Management System

---

> ## ⚠ PLATFORM PIVOT IN EFFECT — READ THIS FIRST
>
> A confirmed programme-level decision (ADR-018, 2026-04-30) has changed the fundamental build strategy for this project.
>
> **Previous direction:** Greenfield build on a custom stack (Flowable, React, Spring Boot, Keycloak).
> **New direction:** Built on top of **Solon Tax** as the base platform.
>
> **A new design process is underway.** All prior architecture decisions (ADR-001 through ADR-017) are **UNDER REVIEW** — open for renewed debate. They represent accumulated knowledge and analysis, not settled constraints. Do not treat any prior ADR as a default or preferred direction without explicit re-confirmation.
>
> ADR-016 (which locked the greenfield direction) is **SUPERSEDED** by ADR-018.
>
> See [`docs/project-foundation/decisions/ADR-018-platform-pivot-solon-tax-confirmed.md`](docs/project-foundation/decisions/ADR-018-platform-pivot-solon-tax-confirmed.md) for the full pivot record.

---

## Project Overview

**Client:** UK Department for Work and Pensions (DWP)
**Project:** Debt Collection Management System (DCMS)
**Nature:** Built on top of Solon Tax as the base platform. Not a greenfield build. Not a pure Solon Tax configuration project. A new system that uses Solon Tax where it satisfies requirements and adds new code where it does not.

## What We're Building

A debt collection management platform for the UK Department for Work and Pensions. The system will manage the end-to-end lifecycle of DWP debt — from identification and assessment through recovery, enforcement, and write-off.

The system must satisfy all DWP functional and non-functional requirements. Solon Tax is used as much as possible. Where Solon Tax does not satisfy a requirement, new code is written on top of it.

**The user interface is a new, custom-built React application** styled to look and feel like Solon Tax. It is not Solon Tax's own UI. It is built from scratch using the Solon Tax visual language as its design reference.

---

## Design Status

The new design process is underway. The following documents contain accumulated knowledge that feeds this process but **do not represent the current or preferred direction**:

- All ADRs in `docs/project-foundation/decisions/` — UNDER REVIEW
- `docs/project-foundation/MASTER-DESIGN-DOCUMENT.md` — UNSETTLED
- `docs/project-foundation/ARCHITECTURE-BLUEPRINT.md` — UNSETTLED
- `docs/project-foundation/solon-tax-feasibility-analysis.md` — conclusion superseded, analysis retained as design input
- `docs/project-foundation/proposed-three-workspace-model 1.md` — UNSETTLED

The following documents remain fully authoritative and unchanged:
- All domain rulings (`docs/project-foundation/domain-rulings/`)
- All domain packs (`docs/project-foundation/domain-packs/`)
- Tender requirements (`Functional-Requirements-Consolidated.md`, NFRs)
- All standards (`docs/project-foundation/standards/`)

---

## Platform

**Base platform:** Solon Tax v2.3.0

Key platform references (fully authoritative as reference material):
- [`docs/project-foundation/solon-tax-platform-reference.md`](docs/project-foundation/solon-tax-platform-reference.md)
- [`docs/project-foundation/amplio-process-engine-reference.md`](docs/project-foundation/amplio-process-engine-reference.md)
- [`external_sys_docs/solon/`](external_sys_docs/solon/) — Solon Tax integration guide, operations guide, API list, data dictionary

---

## Known Stack Elements

The following elements are known. Others are subject to the new design process.

### Confirmed

| Layer | Choice | Note |
|---|---|---|
| Base platform | Solon Tax v2.3.0 | All capabilities to be used where they satisfy requirements |
| Frontend | React + TypeScript | New custom UI, styled to resemble Solon Tax |
| Design reference | Solon Tax visual language | UI must look like Solon Tax; it is not Solon Tax's UI |
| Database | PostgreSQL 16 | Solon Tax v2.3.0 supports PostgreSQL |
| Auth protocol | OAuth 2.0 + OpenID Connect | Solon Tax v2.3.0 uses OAuth 2.0 / OIDC; INT01 mandatory |
| Containers | Docker with multi-stage builds | |
| Local orchestration | Docker Compose | |

### Under Review (prior decisions — not confirmed)

| Layer | Prior choice | Status |
|---|---|---|
| Process engine | Flowable (embedded) | Under review — Solon Tax uses Amplio; re-evaluation required |
| DB migrations | Flyway Community Edition | Under review — Solon Tax uses Liquibase |
| Identity Provider | Keycloak 24 | Under review |
| Authorisation model | RBAC via Keycloak JWT | Under review — Solon Tax uses OPA |
| Java version | Java 21 | Under review — Solon Tax runs on Java 17 |
| Architecture pattern | Single Spring Boot monolith | Under review — Solon Tax is microservices |

---

## Repository Structure

Monorepo layout:

```
/
├── backend/                  ← Spring Boot application (Maven)
├── frontend/                 ← React + TypeScript application
├── infrastructure/
│   ├── docker-compose.yml    ← Full local stack
│   ├── keycloak/             ← Keycloak realm config (under review)
│   └── helm/                 ← Helm chart for remote Kubernetes deployment
├── docs/
│   └── project-foundation/   ← Architecture, requirements, ADRs
├── external_sys_docs/
│   └── solon/                ← Solon Tax v2.3.0 reference documentation
└── CLAUDE.md
```

---

## Environments

Two environments are in scope:

| Environment | Platform | Purpose |
|---|---|---|
| `local` | Docker Compose | Developer laptop — full stack |
| `dev` | AKS on Azure | Shared remote cluster |

**Parity rule:** local and dev must remain functionally equivalent. All divergences must be documented in `docs/project-foundation/remote-environment-spec.md`.

---

## Domain Packages

The following domain areas remain valid regardless of platform. Delivery mechanism is under review.

- `customer` — customer/household/joint entity model, contact details, vulnerability flags, third-party authority
- `account` — financial ledger for a debt: balances, payment history, write-off, regulatory facts
- `strategy` — decision engine, treatment paths, segmentation, champion/challenger, automation rules
- `repaymentplan` — payment arrangements, plan lifecycle, tolerances, breach handling, direct debit, re-aging
- `payment` — financial transactions, payment allocation, refunds, overpayments
- `communications` — letters, SMS, email, in-app messages, channel preference, suppression, contact history
- `workallocation` — queues, worklists, agent assignment, exception queues, supervisor override
- `integration` — DWP Place, DCA placement, payment gateway, bureau feeds, batch file transfer
- `audit` — audit trail, CRUD event log, compliance reporting
- `user` — RBAC, user management, identity provider integration

---

## Source Control & CI/CD

| Concern | Choice |
|---|---|
| Repository | [github.com/nctct/DWP-system-prototype](https://github.com/nctct/DWP-system-prototype) |
| Default branch | `main` (branch-protected, PR-only) |
| CI/CD | GitHub Actions |
| Container runtime | Docker |
| Container registry | `ufstpit-dev-docker.repo.netcompany.com` |
| Image naming | `ufstpit-dev-docker.repo.netcompany.com/dcms_v0/dcms_v0-{service}:{sha}` |
| Tag strategy | `{git-sha}` + `:dev` on main-branch merges (no `latest`) |
