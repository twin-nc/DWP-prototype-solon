# DWP Debt Collection Management System

## Project Overview

**Client:** UK Department for Work and Pensions (DWP)
**Project:** Debt Collection Management System (DCMS)
**Nature:** Greenfield build — this is a new solution being designed and built from scratch.

> This is NOT a configuration or integration project based on SOLON tax or any existing COTS platform. We are designing and building a new system.

## What We're Building

A debt collection management platform for the UK Department for Work and Pensions. The system will manage the end-to-end lifecycle of DWP debt — from identification and assessment through recovery, enforcement, and write-off.

---

## Tech Stack

All components are fully open source.

### Backend

| Layer | Choice |
|---|---|
| Language | Java 21 (OpenJDK via `eclipse-temurin:21`) |
| Framework | Spring Boot 3.4.x |
| Build tool | Maven 3.9.6+ |
| Database | PostgreSQL 16 |
| DB migrations | Flyway (Community Edition) |
| DB access | Spring Data JPA + Hibernate |
| Logging | Logstash Logback Encoder (JSON to stdout) |

### Frontend

| Layer | Choice |
|---|---|
| Framework | React + TypeScript |
| Design system | GOV.UK Design System |
| Container | Nginx (Alpine) |

### Authentication & Authorisation

| Concern | Choice |
|---|---|
| Identity Provider | Keycloak 24 |
| Protocol | OAuth 2.0 + OpenID Connect |
| Spring integration | Spring Security OAuth2 Resource Server |
| Authorisation model | RBAC via JWT claims from Keycloak |
| Service accounts | OAuth2 Client Credentials grant |

Keycloak satisfies INT01, INT02, INT03 (OAuth 2.0 + OIDC SSO) and INT28 (multi-domain federation).

### Infrastructure

| Layer | Choice |
|---|---|
| Containers | Docker with multi-stage builds |
| Local orchestration | Docker Compose (`docker compose up` starts full stack) |
| Local K8s | k3d (K3s in Docker) |
| Remote K8s | Kubernetes (any cluster — no vendor lock-in) |
| Package manager | Helm 3 |

### Base Images (pinned)

| Service | Image |
|---|---|
| App runtime | `eclipse-temurin:21-jre-jammy` |
| App build stage | `eclipse-temurin:21-jdk-jammy` |
| Frontend | `node:20-alpine` (build) / `nginx:1.27-alpine` (runtime) |
| Database | `postgres:16` |
| Identity provider | `quay.io/keycloak/keycloak:24` |

---

## Repository Structure

Monorepo layout:

```
/
├── backend/                  ← Spring Boot application (Maven)
├── frontend/                 ← React + TypeScript application
├── infrastructure/
│   ├── docker-compose.yml    ← Full local stack (app + frontend + postgres + keycloak)
│   ├── keycloak/             ← Keycloak realm config and import files
│   └── helm/                 ← Helm chart for remote Kubernetes deployment
├── docs/
│   └── project-foundation/   ← Architecture, requirements, ADRs
└── CLAUDE.md
```

---

## Environments

Two environments are in scope, per STD-PLAT-011:

| Environment | Platform | Purpose |
|---|---|---|
| `local` | Docker Compose + k3d | Developer laptop — full stack, no cloud credentials |
| `dev` | AKS on Azure | Shared remote cluster |

Higher environments (test, UAT, production) are out of scope until separately specified.

**Parity rule (STD-PLAT-008):** local and dev must remain functionally equivalent — same config keys, same auth flow (Keycloak runs locally, never skipped), same JSON log format. All divergences must be documented in `docs/project-foundation/remote-environment-spec.md`.

---

## Architecture

**Single Spring Boot monolith** with well-defined internal packages per domain. No microservices split. Two services in total: backend + frontend.

Domain packages (internal to the monolith):
- `customer` — customer/household/joint entity model, contact details, vulnerability flags, third-party authority
- `account` — financial ledger for a debt: balances, payment history, write-off. Records regulatory facts (breathing space date, insolvency date, death, fraud marker) for audit. Does NOT own lifecycle position or behavioural effect of these events — owned by the process engine (ADR-001, ADR-002)
- `strategy` — decision engine, treatment paths, segmentation, champion/challenger, automation rules
- `repaymentplan` — payment arrangements, plan lifecycle, tolerances, breach handling, direct debit, re-aging
- `payment` — financial transactions, payment allocation, refunds, overpayments
- `communications` — letters, SMS, email, in-app messages, channel preference, suppression, contact history
- `workallocation` — queues, worklists, agent assignment, exception queues, supervisor override
- `integration` — DWP Place, DCA placement, payment gateway, bureau feeds, batch file transfer (anti-corruption layer)
- `audit` — audit trail, CRUD event log, compliance reporting
- `user` — RBAC, user management, Keycloak integration

**Infrastructure packages** (internal to the monolith, not domain modules):
- `infrastructure/process` — Flowable engine config, BPMN/DMN resources, JavaDelegate implementations; all Flowable imports confined here
- `shared/process/port` — ProcessEventPort, ProcessStartPort, DelegateCommandBus interfaces and command types; no Flowable imports; imported by domain modules

**Note:** The RBAC role model (`AGENT`, `TEAM_LEADER`, `OPS_MANAGER`, `COMPLIANCE`, `ADMIN`) is a placeholder. A full permissions matrix — covering write-off limits, override capability, geographic/team access restrictions, and specialist role profiles — must be designed before the `user` feature is built.

**Note:** The process engine (Flowable) is infrastructure, not a domain module. All Flowable imports are confined to `infrastructure/process`. Domain modules interact with the process engine only via interfaces in `shared/process/port`.

---

## Team

Six to seven full-stack developers owning everything — backend, frontend, infrastructure, and DevOps. No dedicated specialist roles. Claude agents fill the specialist roles (architect, domain expert, DevOps engineer, etc.) as needed.

---

## Source Control & CI/CD

| Concern | Choice |
|---|---|
| Repository | [github.com/nctct/DWP-system-prototype](https://github.com/nctct/DWP-system-prototype) |
| Default branch | `main` (branch-protected, PR-only) |
| CI/CD | GitHub Actions |
| Container runtime | Docker (used from day one) |
| Container registry | `ufstpit-dev-docker.repo.netcompany.com` |
| Image naming | `ufstpit-dev-docker.repo.netcompany.com/dcms_v0/dcms_v0-{service}:{sha}` |
| Tag strategy | `{git-sha}` + `:dev` on main-branch merges (no `latest`) |

---

*This file will be expanded as onboarding progresses.*
