# DIAG-02 — System Architecture Overview

**For:** Slide 4 — System Architecture in One Diagram
**Standard:** C4 Level 2 (Container diagram)

---

## Diagram A — Container View

```mermaid
C4Container
    title DCMS — System Architecture (C4 Container Level)

    Person(agent, "DWP Agent / Supervisor", "Internal staff. Manages debt cases, reviews queues, authorises write-offs.")
    Person(admin, "Business Admin / Ops Manager", "Configures DMN rules, BPMN processes, reference data, and policy bundles.")

    System_Boundary(dcms, "DCMS — Debt Collection Management System") {

        Container(frontend, "Frontend", "React + TypeScript / Nginx 1.27", "GOV.UK Design System UI. Served as static assets. Communicates with backend via REST JSON.")

        Container(backend, "Backend Monolith", "Spring Boot 3.4 / Java 21", "Single deployable JAR. All domain logic, process engine integration, and outbound adapters. 13 domain packages + infrastructure/process.")

        ContainerDb(db, "Application Database", "PostgreSQL 16", "Two schemas: application schema (public) managed by Flyway, and flowable schema owned by the Flowable engine.")

        Container(keycloak, "Identity Provider", "Keycloak 24", "OAuth 2.0 + OIDC. Issues signed JWT tokens. Enforces RBAC roles (AGENT, TEAM_LEADER, OPS_MANAGER, COMPLIANCE, ADMIN). Supports multi-domain federation.")
    }

    System_Boundary(ext, "External Systems") {
        System_Ext(dwpplace, "DWP Place / DM6", "DWP source system. Submits debt referrals to the DCMS ingest API.")
        System_Ext(portal, "DWP Self-Service Portal", "Customer-facing portal. Calls DCMS APIs for I&E submission, payment, and secure messaging.")
        System_Ext(gateway, "Payment Gateway", "Processes direct debit and card payments. Receives payment instructions from DCMS.")
        System_Ext(bureau, "Credit Reference Agency", "Provides bureau scorecard data for affordability assessment and segmentation.")
        System_Ext(dca, "Debt Collection Agents (DCA)", "External collectors. DCMS manages placement, recall, and reconciliation via REST or batch file.")
    }

    Rel(agent, frontend, "Uses", "HTTPS")
    Rel(admin, frontend, "Uses", "HTTPS")
    Rel(frontend, backend, "REST API calls", "HTTPS / JSON")
    Rel(frontend, keycloak, "Authenticate", "OIDC / redirect")
    Rel(backend, keycloak, "Validate JWT", "HTTPS / JWKS endpoint")
    Rel(backend, db, "Read / Write", "JDBC / JPA")

    Rel(dwpplace, backend, "Debt referral ingest", "HTTPS / REST")
    Rel(portal, backend, "I&E, payments, messaging", "HTTPS / REST")
    Rel(backend, gateway, "Payment instructions", "HTTPS / REST")
    Rel(backend, bureau, "Pull scorecard data", "HTTPS / REST")
    Rel(backend, dca, "Placement, recall, reconciliation", "HTTPS / REST or batch")
```

---

## Diagram B — Monolith Interior

```mermaid
graph TD
    subgraph monolith["Backend Monolith — Internal Structure"]

        subgraph domain["Domain Packages (13 total)"]
            direction LR
            customer["**customer**\nIdentity · Vulnerability\nJoint liability · 3rd-party authority"]
            account["**account**\nLedger · Regulatory state\nWrite-off · Breathing space"]
            strategy["**strategy**\nDecisioning · Segmentation\nTreatment routing · DMN rules"]
            workalloc["**workallocation**\nQueues · Agent assignment\nSupervisor override"]
            repayment["**repaymentplan**\nArrangements · I&E\nBreach handling"]
            payment["**payment**\nPosting · Allocation\nRefunds · Reconciliation"]
            comms["**communications**\nLetters · SMS · Email\nSuppression · Contact history"]
            integration["**integration**\nInbound/Outbound adapters\nAnti-corruption layer"]
            thirdparty["**thirdpartymanagement**\nDCA placement · Recall\nCommission · Billing"]
            user["**user**\nRBAC · Keycloak integration"]
            audit["**audit**\nImmutable event log\nCompliance evidence"]
            analytics["**analytics**\nScoring · Segmentation\nDMN table evaluation"]
            reporting["**reporting**\nMI exports · KPI read models"]
        end

        subgraph process["Process Infrastructure (Flowable isolation boundary)"]
            port["**shared/process/port**\nProcessEventPort · ProcessStartPort\nDelegateCommandBus\n⚠ Zero Flowable imports"]
            infra["**infrastructure/process**\nFlowable config · BPMN/DMN resources\nJavaDelegate implementations\n⚠ ALL Flowable imports confined here"]
            engine[("**Flowable BPMN/DMN Engine**\nEmbedded in JVM · Manages own\ntransaction on flowable schema")]
        end

        db2[("**PostgreSQL 16**\nflowable schema")]
    end

    customer --> port
    account --> port
    strategy --> port
    repayment --> port
    payment --> port
    comms --> port

    port -->|"Port interfaces only\nno Flowable types"| infra
    infra -->|"Flowable API calls"| engine
    engine -->|"JDBC"| db2
```

---

## Architecture Notes

| Constraint | Detail |
|---|---|
| **One process instance per DebtAccount** | Multiple debts for one customer run as independent Flowable process instances. |
| **Transaction boundary (ADR-003)** | Application DB writes are always inside `@Transactional`. Flowable engine calls are always outside. Prevents two-phase commit across the two schemas. |
| **No domain module imports Flowable** | `shared/process/port` has zero Flowable dependencies. Domain modules are testable without a running process engine. |
| **All external calls via integration ACL** | No domain module calls an external system directly. All outbound integrations route through `domain/integration`. |
| **Auth on every request** | Backend validates JWT on every request via Spring Security OAuth2 Resource Server. RBAC roles enforced at controller layer. |
