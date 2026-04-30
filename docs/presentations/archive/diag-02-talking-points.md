# DIAG-02 Talking Points - System Architecture

## Opening - set the scene

"This slide is the whole DCMS architecture in one view. The main point is that this is deliberately simple at deployment level, but strongly bounded inside the application.

We have a React frontend, a Spring Boot backend monolith, PostgreSQL, and Keycloak. Around that are the external systems DCMS has to integrate with: DWP Place, the future self-service portal, payment gateway, credit reference agency, and debt collection agents.

The leadership message is: this is not a microservice estate. It is a right-sized monolith for a 6-7 person team, with clean internal boundaries where the complexity actually matters."

---

## How to talk through the diagram

Start on the left, move through the middle, then finish on the right.

### 1. Users and channels

"On the left we have the human users and channels. DWP staff - agents, supervisors, and business admins - use the frontend over HTTPS. The future customer portal is shown separately because DCMS is not building the public portal UI; DCMS exposes backend APIs that a portal can call."

Key points to land:

- Internal users use the DCMS frontend.
- Customers use a future self-service portal outside DCMS.
- The portal calls DCMS APIs for things like income and expenditure, payments, and secure messages.
- This keeps DCMS as the debt collection management system, not the owner of every digital channel.

---

### 2. Frontend

"The frontend is React and TypeScript, served through Nginx. It uses the GOV.UK Design System and talks to the backend using REST JSON. It is a runtime service, but it does not own business rules. It presents workflows, forms, validation, and role-specific workspaces."

Useful phrasing:

> "The frontend is the operator experience. The backend remains the authority for decisions, permissions, and audit evidence."

Mention if needed:

- Frontend role hiding is user experience.
- Backend authorization is the security boundary.
- The frontend authenticates through Keycloak using OIDC.

---

### 3. Database and identity

"The green components are infrastructure. PostgreSQL is the application database and also hosts the Flowable engine schema. Keycloak is the identity provider. It issues JWT claims with role information, and the backend validates those claims on every request."

Key points:

- PostgreSQL 16 has two logical areas: the application schema and the Flowable schema.
- Flyway manages the application schema.
- Flowable owns its own engine tables.
- Keycloak provides OAuth2/OIDC and RBAC through JWT claims.

Presenter line:

> "There is no hidden authorization model in the database and no separate policy engine. Roles come from Keycloak, and Spring Security enforces them at the backend boundary."

---

### 4. Backend monolith

"The large blue area is the backend monolith: Spring Boot 3.4 on Java 21. This is the main design decision. It is one deployable backend, not a set of distributed services."

Then explain why:

"For this programme, a monolith is a control choice. It reduces deployment complexity, keeps transaction boundaries understandable, and fits the expected team size. But it is not an unstructured monolith. The domain is split into packages that match the business capabilities."

Walk a few packages, not all thirteen:

- `customer` owns identity, vulnerability, joint liability, and third-party authority.
- `account` owns ledger state, regulatory state, write-off, breathing space, insolvency, deceased, and fraud markers.
- `strategy` owns decisioning, segmentation, treatment routing, and champion/challenger.
- `repaymentplan` owns income and expenditure, arrangements, schedule management, and breach handling.
- `communications` owns letters, SMS, email, suppression, and contact history.
- `audit` owns immutable evidence and trace records.
- `integration` is the anti-corruption layer for inbound and outbound APIs.
- `thirdpartymanagement` owns DCA placement, recall, commission, billing, and partner interfaces.

Useful phrase:

> "The monolith is a deployment shape, not a licence for modules to call each other casually."

---

## The most important internal boundary

"The lower half of the backend shows the process engine isolation boundary. This is the part of the diagram I would spend most time on."

### Shared process port

"The purple layer is `shared/process/port`. It contains interfaces such as `ProcessEventPort`, `ProcessStartPort`, and `DelegateCommandBus`. Domain modules can call these interfaces, but there are zero Flowable imports here."

Key message:

> "Domain code can trigger workflow behaviour without knowing Flowable exists."

### Infrastructure process

"The crimson layer is `infrastructure/process`. This is the only part of the application that imports Flowable types. It contains the Flowable engine config, BPMN and DMN resources, JavaDelegate implementations, and port implementations."

Key message:

> "All Flowable-specific knowledge is confined to infrastructure."

### Flowable engine

"At the bottom is the embedded Flowable BPMN/DMN engine. It runs inside the same JVM as the backend, but it manages its own transaction on the Flowable schema."

Important safety point:

> "Application database writes are transactional. Flowable calls sit outside that transaction boundary. That rule avoids a two-phase commit problem and stops process-engine failure from rolling back protective business facts."

---

## External systems on the right

"On the right are the main system boundaries DCMS has to integrate with."

Walk them top to bottom:

- DWP Place / DM6 sends debt referrals into the DCMS ingest API.
- The future self-service portal calls DCMS APIs for I&E, payments, and messages.
- The payment gateway receives direct debit and card payment instructions.
- The credit reference agency provides bureau and scorecard data.
- Debt Collection Agents receive placement and recall instructions and return reconciliation data.

Key architectural message:

> "External systems do not reach into domain packages. They go through the integration layer, which gives us one place to handle mapping, protocol differences, retries, and partner-specific quirks."

---

## What the diagram is trying to prove

"There are three claims this slide makes."

1. "The deployment model is simple: frontend, backend, database, identity provider."
2. "The business model is modular: thirteen domain packages aligned to DWP debt collection capabilities."
3. "The risky boundary is controlled: Flowable is powerful, but isolated behind a port layer so it cannot leak into domain code."

Close this section with:

> "So the architecture is intentionally boring where operations need predictability, and deliberately strict where legal and workflow complexity create risk."

---

## Likely questions

**"Why a monolith instead of microservices?"**
Because the expected team is small and the domain needs strong consistency across account state, communications suppression, payment allocation, process state, and audit evidence. Microservices would add deployment, integration, and data consistency overhead before the team has a scale problem that justifies it.

**"Does monolith mean hard to change later?"**
No. The key is the internal boundaries. The domain packages, integration anti-corruption layer, and process port boundary keep responsibilities separate. If a future service split is needed, these boundaries make that easier than starting from a distributed design too early.

**"Is Flowable a separate service?"**
No. Flowable is embedded in the backend JVM. It has its own engine tables in PostgreSQL, but it is not deployed as a separate runtime service.

**"Why isolate Flowable so strongly?"**
Because process engines are useful orchestration tools, but they should not become the business domain. The domain modules own decisions and invariants. Flowable owns long-running workflow execution, timers, user tasks, BPMN paths, and DMN evaluation.

**"Where are external integrations implemented?"**
Through the `integration` package and, for DCA-specific capability, `thirdpartymanagement`. Domain modules should not call external systems directly.

**"What is the transaction rule?"**
Application database writes happen inside Spring `@Transactional` boundaries. Flowable engine calls happen outside those boundaries. This avoids distributed transaction coupling across the application schema and Flowable schema.

**"How is security enforced?"**
Keycloak issues signed JWTs. The backend validates JWTs using Spring Security and enforces RBAC at controller and service boundaries. The frontend can adapt the user experience by role, but backend checks are the authority.

---

## Close-out message

"The diagram is meant to show confidence rather than novelty. DCMS uses standard, supportable technology: React, Spring Boot, PostgreSQL, Keycloak, and Flowable. The important design choice is how those pieces are arranged: one right-sized backend, clear domain modules, external systems behind integration boundaries, and Flowable isolated so workflow orchestration never takes over the business model."

---

*Sources: diag-02-system-architecture.drawio, leadership-presentation-outline.md, ADR-003, ADR-007*
