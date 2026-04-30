# Development Plan — Flowable Process Engine

**DWP Debt Collection Management System (DCMS)**
Version 2.0 | April 2026

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
   - [1.4 Key Design Decisions](#14-key-design-decisions)
2. [System Architecture](#2-system-architecture)
3. [Technology Stack](#3-technology-stack)
4. [Domain Model & Entity Relationship Diagram](#4-domain-model--entity-relationship-diagram)
5. [Actor Model & Use Cases](#5-actor-model--use-cases)
6. [RBAC Permissions Matrix](#6-rbac-permissions-matrix)
7. [Workflow & Decision Architecture (Flowable)](#7-workflow--decision-architecture-flowable)
8. [Champion/Challenger A/B Testing Design](#8-championchallenger-ab-testing-design)
9. [Integration Architecture](#9-integration-architecture)
10. [Pre-Build Phase (22 Apr – 3 May)](#10-pre-build-phase-22-apr--3-may)
11. [Sprint Plan — 9 × 1-Week Sprints (5 May – 4 Jul)](#11-sprint-plan--9--1-week-sprints-5-may--4-jul)
12. [Demo Scenario Design (8 Jul 2026)](#12-demo-scenario-design-8-jul-2026)
13. [Risk Register](#13-risk-register)
14. [Requirements Traceability](#14-requirements-traceability)

---

## 1. Executive Summary

### 1.1 Purpose

This document is the complete technical design and build plan for the DWP Debt
Collection Management System (DCMS) using **Flowable 6.8.x** as the embedded
BPMN/DMN/CMMN process engine. It covers everything the team needs to deliver a
compelling tender demonstration in **early July 2026**.

**Team composition:** six to seven developers (Dev1–Dev6/7), two DevOps engineers
(DevOps1–DevOps2), and one tester (QA). Total: nine to ten people.

### 1.2 Option A vs Option B at a Glance

| Concern | Option A (Flowable) | Option B (Camunda 7) |
|---|---|---|
| Engine | Flowable 6.8.x | Camunda 7.21.x |
| BPMN 2.0 | Yes | Yes |
| DMN 1.3 | Yes | Yes |
| CMMN | Yes | No |
| Admin UI | Flowable Admin UI (5th Docker service, port 8091) | Cockpit/Tasklist/Admin bundled |
| Live process visualisation | Flowable Admin UI | Camunda Cockpit |
| Runtime DMN editing | Yes — live via Admin UI **(DW.38 differentiator)** | No — redeploy required |
| EOL risk | None — actively maintained, Apache 2.0 | Community EOL Oct 2025 |
| Licence | Apache 2.0 | Apache 2.0 (Community) |

### 1.3 Key Decisions

- Flowable engine **embedded** in the Spring Boot monolith via
  `flowable-spring-boot-starter`
- Flowable Admin UI runs as a **separate Docker Compose service** (port 8091)
- All Flowable tables live in a dedicated **`flowable` PostgreSQL schema**
- **Champion/Challenger** A/B testing driven by a DMN decision table
- All external integrations are **controllable stubs** for demo flexibility
- **9 × 1-week sprints**, 4 May – 3 July 2026; demo target **8 July 2026** (early July)
- **Team:** Dev1–Dev6/7 (backend/frontend/full-stack), DevOps1–DevOps2 (infra/CI), QA (test)

### 1.4 Key Design Decisions

#### Low-code / no-code process configuration

The system is designed as a **low-code/no-code platform** for operational configuration of debt collection workflows. Business analysts and operations specialists can adjust treatment paths, escalation timing, and decision rules (DMN tables) directly in the Flowable Modeler UI — without a software release cycle.

**What is configurable at runtime (low-code):**
- BPMN treatment paths — step sequencing, branching conditions, wait timers
- DMN decision tables — segmentation rules, strategy assignment thresholds, champion/challenger split ratios
- Escalation timing parameters — days-before-escalation, retry intervals

**What must remain code-controlled (git + CI only — never runtime-editable):**
- Statute-barred clock logic (RULING-013)
- Breathing space enforcement
- Vulnerability flag suppression of treatments
- Any flow that touches Article 9 health data (vulnerability type, income/expenditure raw data)

**Role model for process editing:**

| Role | Design (Modeler) | Deploy to Dev | Deploy to Prod | Monitor instances |
|---|---|---|---|---|
| `PROCESS_DESIGNER` | Yes | Yes | No | No |
| `COMPLIANCE` | No | No | Approve/sign-off | Yes |
| `OPS_MANAGER` | No | No | No | Yes (pause/retry) |
| `ADMIN` | No | No | No | Yes (pause/retry) |
| `TEAM_LEADER` / `AGENT` | No | No | No | No |

Production deployments require a two-person gate: `PROCESS_DESIGNER` proposes, `COMPLIANCE` approves. The export-to-git step is mandatory before promoting any process definition beyond dev, preserving the audit trail in version control.

---

#### SPECIALIST_AGENT role — tender-grounded, not a design invention

The system defines two distinct caseworker roles: `AGENT` (general collector) and `SPECIALIST_AGENT` (handles vulnerable, complex, and fraud-flagged cases).

This is explicitly required by the tender:

- **WAM.20** (Must have): "The system shall have the ability for defined segments or groups of customers to be **exclusively managed by specialist agents** (including, but not limited to, vulnerable customers). Accounts that are classified in this manner should be controlled by permissions and access restricted accordingly."
- **UAAF.15** (Must have): "The system shall restrict treatments to certain users — an example may include but is not limited to having role profiles drilled down such as **specialist team, escalations team, general collectors**, and DCA."

**Design choice:** The constraint is one-directional. WAM.20 requires that vulnerable accounts are only accessible to specialist agents — it does not prohibit a specialist from also handling standard cases. A single caseworker can therefore hold both `AGENT` and `SPECIALIST_AGENT` roles in Keycloak, giving DWP flexibility in how they staff teams without violating the tender requirement. The data access gate on Article 9 health data (`VULNERABILITY_TYPE`, `INCOME_EXPENDITURE.raw_data`) remains enforced by role regardless of staffing model.

---

## 2. System Architecture

### 2.1 C4 Context — Services Overview

```
┌─────────────────────────────────────────────────────────────────┐
│  Browser                                                         │
│  React SPA (GOV.UK Design System, TypeScript)  :3000            │
└──────────────────────┬──────────────────────────────────────────┘
                       │ HTTPS + JWT Bearer
┌──────────────────────▼──────────────────────────────────────────┐
│  Spring Boot Monolith  :8080                                     │
│                                                                  │
│  REST Controllers → Service Layer → Flowable Engine (embedded)  │
│                                                                  │
│  Domain packages:                                                │
│    customer │ account │ strategy │ repaymentplan │ payment       │
│    communications │ workallocation │ integration │ audit │ user  │
│                                                                  │
│  Spring Security (Keycloak JWT) │ Spring Data JPA │ Spring AOP  │
└──────────────────┬──────────────────────────────────────────────┘
                   │
         ┌─────────┴──────────┐
         │  PostgreSQL :5432   │
         │  schema: public     │
         │  schema: flowable   │
         └────────────────────┘

Supporting services (Docker Compose):
  Keycloak :8180          ← Identity provider (OIDC/OAuth2)
  Flowable Admin UI :8091 ← BPMN/DMN/CMMN editor + process monitoring

Mock stubs (in-process or sidecar):
  DWP Place │ DCA API │ Payment Gateway │ GOV.UK Notify │ RTBS │ Bureau
```

### 2.2 Docker Compose Services

| Service | Image | Port | Purpose |
|---|---|---|---|
| postgres | postgres:16 | 5432 | Application + Flowable DB |
| keycloak | quay.io/keycloak/keycloak:24 | 8180 | OIDC identity provider |
| backend | dcms-backend (eclipse-temurin:21) | 8080 | Spring Boot + Flowable |
| frontend | dcms-frontend (nginx:1.27-alpine) | 3000 | React SPA |
| flowable-ui | flowable/flowable-ui:6.8.0 | 8091 | Admin UI / process monitor |

### 2.3 Request Flow — Authenticated API Call

```
Browser → Keycloak (PKCE login) → JWT issued
Browser → GET /api/accounts/{id} [Bearer JWT]
  → Spring Security validates JWT against Keycloak JWKS
  → @PreAuthorize("hasRole('AGENT')") passes
  → AccountService.findById()
  → Spring Data JPA → PostgreSQL
  → Spring AOP audit interceptor writes AUDIT_EVENT
  → Response returned
```

---

## 3. Technology Stack

| Layer | Technology | Version | Notes |
|---|---|---|---|
| Language | Java | 21 (OpenJDK) | LTS, virtual threads |
| Framework | Spring Boot | 3.4.x | Web, Security, Data, AOP |
| Process Engine | **Flowable** | **6.8.x** | BPMN + DMN + CMMN, Apache 2.0 |
| Build | Maven | 3.9.6+ | |
| Database | PostgreSQL | 16 | public + flowable schemas |
| Migrations | Flyway Community | 10.x | Two locations: schema + seed |
| ORM | Spring Data JPA + Hibernate | 6.x | |
| Auth/AuthZ | Keycloak | 24 | OIDC/OAuth2, RBAC |
| Frontend | React + TypeScript | 18 / 5.x | |
| Design System | GOV.UK Design System | 5.x | WCAG AA |
| Containers | Docker | 25.x | Multi-stage builds |
| Orchestration | Docker Compose + Helm 3 | v2 / 3.x | Local + AKS dev |
| CI/CD | GitHub Actions | — | |
| Logging | Logstash Logback Encoder | 7.x | JSON stdout |
| Process UI | Flowable Admin UI | 6.8.x | Separate Docker service |

### 3.1 Key Maven Dependencies

```xml
<!-- Flowable — BPMN + DMN + CMMN embedded engine -->
<dependency>
  <groupId>org.flowable</groupId>
  <artifactId>flowable-spring-boot-starter</artifactId>
  <version>6.8.0</version>
</dependency>

<!-- Spring Security — Keycloak JWT validation -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>

<!-- Database -->
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
</dependency>
<dependency>
  <groupId>org.flywaydb</groupId>
  <artifactId>flyway-core</artifactId>
</dependency>

<!-- AOP for audit trail -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### 3.2 Flyway Configuration

```yaml
spring:
  flyway:
    locations:
      - classpath:db/migration/schema
      - classpath:db/migration/seed
    schemas: public, flowable
```

Schema migrations: `V{version}__{description}.sql`  
Seed migrations: `V900__{description}.sql` onward — sort after all schema migrations.

---

## 4. Domain Model & Entity Relationship Diagram

### 4.1 Bounded Contexts

| Bounded Context | Core Tables |
|---|---|
| Party | PARTY, PARTY_ADDRESS, PARTY_CONTACT, CONTACT_CONSENT |
| Vulnerability & Fraud | VULNERABILITY_ASSESSMENT, VULNERABILITY_TYPE, ASSESSMENT_VULNERABILITY_TYPE, FRAUD_FLAG |
| Account & Household | DEBT_ACCOUNT, JOINT_DEBT_LINK, DEBT_STATUS_HISTORY, INTEREST_ACCRUAL, LEGAL_HOLD, DISCLOSURE_NOTICE, WRITE_OFF |
| Strategy & A/B Testing | STRATEGY, STRATEGY_TEST, STRATEGY_ASSIGNMENT, STRATEGY_OUTCOME_METRIC, DECISION_TRACE |
| Case & Workflow | CASE, CASE_NOTE, WORK_QUEUE, PROCESS_CASE_LINK, DISPUTE |
| Repayment | REPAYMENT_ARRANGEMENT, ARRANGEMENT_BREACH, PROMISE_TO_PAY, INCOME_EXPENDITURE, AFFORDABILITY_FORMULA_VERSION, FORBEARANCE_RECORD |
| Payment | PAYMENT, PAYMENT_ALLOCATION, SUSPENSE_PAYMENT |
| Communication | COMMUNICATION, COMMUNICATION_TEMPLATE, COMMUNICATION_THREAD, SUPPRESSION_LOG |
| Organisation | USER, TEAM, THIRD_PARTY_PLACEMENT, DCA |
| System | BROADCAST_MESSAGE, SYSTEM_CONFIG, AUDIT_EVENT, MIGRATION_BATCH, MIGRATION_RECORD |

### 4.2 Entity Relationship Diagram

```
PARTY {uuid id PK, party_type, full_name, date_of_birth,
       nino_encrypted, nino_hash, deceased_flag,
       deceased_flagged_at TIMESTAMP WITH TIME ZONE,
       deceased_flagged_by FK USER,
       created_at}

PARTY_ADDRESS {id PK, party_id FK, address_type,
               line1..line4, postcode, country_code,
               valid_from, valid_to, is_current}

PARTY_CONTACT {id PK, party_id FK, contact_type,
               contact_value_encrypted, is_primary,
               valid_from, valid_to}

CONTACT_CONSENT {id PK, party_contact_id FK, channel,
                 consent_given, consent_date,
                 withdrawn_date, source}

VULNERABILITY_ASSESSMENT {id PK, party_id FK,
  status ENUM(IDENTIFIED|ASSESSED|ACTIVE|RESOLVED),
  severity ENUM(LOW|MEDIUM|HIGH|CRITICAL),
  assessed_by FK USER, assessed_at, review_due_date,
  notes_encrypted}
-- DATA CLASSIFICATION: notes_encrypted = Restricted /
--   potential Article 9 special category.
--   DWP DPO review required before production use.

VULNERABILITY_TYPE {id PK, code, name, category}
-- DATA CLASSIFICATION: Restricted where health-related.
--   Access: SPECIALIST_AGENT and above only.

ASSESSMENT_VULNERABILITY_TYPE {
  assessment_id FK, vulnerability_type_id FK, PK(both)}

FRAUD_FLAG {id PK, party_id FK,
            flag_type ENUM(SUSPECTED|CONFIRMED|CLEARED),
            raised_by FK USER, raised_at,
            investigation_ref, cleared_at}

DEBT_ACCOUNT {uuid id PK, party_id FK,
  account_ref UNIQUE, debt_type,
  original_amount, outstanding_balance,
  accrued_interest, currency,
  status ENUM(ACTIVE|ARRANGEMENT|SUSPENDED|
              LEGAL|WRITTEN_OFF|CLOSED),
  benefit_status ENUM(ON_BENEFIT|OFF_BENEFIT|UNKNOWN),
  delinquency_days,
  breathing_space_flag,
  breathing_space_type ENUM(STANDARD|MENTAL_HEALTH_CRISIS),
  breathing_space_end_date,
  insolvency_flag, insolvency_type,
  deceased_flag, fraud_flag,
  assigned_agent_id FK USER, assigned_team_id FK TEAM,
  champion_challenger_variant,
  cause_of_action_date DATE,
  last_acknowledgement_date DATE,
  is_statute_barred BOOLEAN NOT NULL DEFAULT false,
  created_at, updated_at}

-- INVARIANTS:
-- breathing_space_flag: set/cleared by
--   CommunicationSuppressionService only.
-- is_statute_barred: set by StatuteBarredEvaluator only.
-- cause_of_action_date: required for new accounts
--   (service layer); nullable for migrated accounts.
-- Status valid transitions:
--   ACTIVE → ARRANGEMENT | SUSPENDED | LEGAL | WRITTEN_OFF
--   ARRANGEMENT → ACTIVE (breach) | COMPLETED → CLOSED
--   SUSPENDED → ACTIVE | LEGAL
--   LEGAL → WRITTEN_OFF | CLOSED
--   WRITTEN_OFF → CLOSED
--   CLOSED: terminal — reopen via OPS_MANAGER override only

JOINT_DEBT_LINK {id PK, primary_account_id FK,
  secondary_account_id FK,
  relationship_type ENUM(JOINT|GUARANTOR), linked_at}

DEBT_STATUS_HISTORY {id PK, account_id FK,
  previous_status, new_status,
  changed_by FK USER, changed_at, reason_code, notes,
  limitation_clock_reset BOOLEAN DEFAULT false}
-- limitation_clock_reset = true only for events in
--   RULING-012: debtor-initiated cleared payments,
--   REPAYMENT_ARRANGEMENT creation, formal written
--   acknowledgement (CASE_NOTE type ACKNOWLEDGEMENT).

INTEREST_ACCRUAL {id PK, account_id FK, accrual_date,
  rate_applied, amount_accrued, calculated_at}

LEGAL_HOLD {id PK, account_id FK,
  hold_type ENUM(
    BREATHING_SPACE_STANDARD|
    BREATHING_SPACE_MENTAL_HEALTH_CRISIS|
    COURT_ORDER|INSOLVENCY|TRIBUNAL|OTHER),
  start_date, end_date,
  -- end_date MUST be NULL for
  --   BREATHING_SPACE_MENTAL_HEALTH_CRISIS
  reference, created_by FK USER}

DISCLOSURE_NOTICE {id PK, account_id FK, notice_type,
  sent_at, channel, reference}

WRITE_OFF {id PK, account_id FK, amount, reason_code,
  requested_by FK USER,
  approved_by FK USER,
  -- CHECK (requested_by != approved_by)
  approval_tier ENUM(
    AGENT|TEAM_LEADER|OPS_MANAGER|SRO|SYSTEM),
  write_off_date, gl_journal_ref}
-- SYSTEM tier: auto-approved de minimis joint split
--   residuals only (pending DDE-OQ-05 threshold).
-- SRO tier: pending DDE-OQ-03 role confirmation.

STRATEGY {id PK, name, description, is_active,
  is_champion, created_at, retired_at}

STRATEGY_TEST {id PK, name,
  champion_strategy_id FK, challenger_strategy_id FK,
  split_percentage INT(0-100),
  start_date, end_date,
  status ENUM(DRAFT|ACTIVE|PAUSED|COMPLETED),
  winner_strategy_id FK,
  min_sample_size INT,
  min_duration_days INT}
-- min_sample_size / min_duration_days: nullable for demo;
--   populated post-award per DDE-OQ-09.

STRATEGY_ASSIGNMENT {id PK, account_id FK,
  strategy_test_id FK, strategy_id FK,
  variant ENUM(CHAMPION|CHALLENGER),
  vulnerability_override BOOLEAN DEFAULT false,
  assigned_at,
  assignment_method ENUM(DMN|MANUAL|MIGRATION)}
-- vulnerability_override = true: always CHAMPION due to
--   vulnerability_flag. Excluded from A/B analytics.

STRATEGY_OUTCOME_METRIC {id PK,
  strategy_assignment_id FK,
  metric_type ENUM(PAYMENT_MADE|ARRANGEMENT_AGREED|
    ACCOUNT_CLOSED|COMPLAINT|DEFAULT),
  value NUMERIC, recorded_at,
  source ENUM(PAYMENT_EVENT|PROCESS_EVENT|MANUAL)}
-- Written for ALL accounts including vulnerable.
-- Exclusion applied at query level only.

DECISION_TRACE {id PK, account_id FK, decision_key,
  input_snapshot JSONB, output_snapshot JSONB,
  executed_at, flowable_execution_id}

PROCESS_CASE_LINK {id PK, case_id FK,
  flowable_process_instance_id,
  process_definition_key, started_at, ended_at,
  status ENUM(ACTIVE|COMPLETED|TERMINATED|SUSPENDED)}

CASE {uuid id PK, account_id FK, case_ref UNIQUE,
  case_type, status, priority,
  assigned_agent_id FK USER, assigned_team_id FK TEAM,
  opened_at, closed_at, sla_due_date,
  created_at, updated_at}

CASE_NOTE {id PK, case_id FK, author_id FK USER,
  note_text,
  note_type ENUM(ACTION|CONTACT_ATTEMPT|SYSTEM|
    ESCALATION|OVERRIDE|ACKNOWLEDGEMENT),
  created_at, is_sensitive}
-- ACKNOWLEDGEMENT: formal written acknowledgement of
--   debt. Resets limitation clock per RULING-012.

WORK_QUEUE {id PK, queue_name,
  queue_type ENUM(STANDARD|EXCEPTION|ESCALATION|
    LEGAL|SPECIALIST|DCA|DISPUTE),
  team_id FK TEAM, capacity, priority_order, is_active}

DISPUTE {id PK, account_id FK, party_id FK, case_id FK,
  dispute_type ENUM(
    PAYMENT_DISPUTE|PROCESS_DISPUTE|
    FRAUD_DISPUTE|OTHER),
  status ENUM(OPEN|UNDER_REVIEW|RESOLVED|
    ESCALATED|CLOSED),
  raised_by FK USER, raised_at, description,
  resolution_notes, resolved_by FK USER, resolved_at}
-- communications_suppressed REMOVED.
-- Suppression managed via SUPPRESSION_LOG
--   reason: DISPUTE_INTERNAL.

SUPPRESSION_LOG {id PK, account_id FK,
  suppression_reason ENUM(
    BREATHING_SPACE_STATUTORY|
    MENTAL_HEALTH_CRISIS_STATUTORY|
    DISPUTE_INTERNAL|
    VULNERABILITY_POLICY|
    INSOLVENCY_STATUTORY|
    DECEASED_MANDATORY),
  activated_at TIMESTAMP WITH TIME ZONE,
  lifted_at TIMESTAMP WITH TIME ZONE,
  activated_by FK USER,
  lifted_by FK USER,
  is_active BOOLEAN DEFAULT true}

-- UNIQUE partial index:
--   CREATE UNIQUE INDEX uix_suppression_log_active
--   ON suppression_log (account_id, suppression_reason)
--   WHERE is_active = true;
-- activateSuppression() uses upsert (ON CONFLICT DO NOTHING).
-- Both activation and lift write AUDIT_EVENT.
-- CommunicationSuppressionService is the SOLE writer.

PROMISE_TO_PAY {id PK, account_id FK,
  promised_amount, promised_date, agent_id FK USER,
  status ENUM(ACTIVE|KEPT|BROKEN|CANCELLED), created_at}

REPAYMENT_ARRANGEMENT {uuid id PK, account_id FK,
  arrangement_type ENUM(
    INSTALMENT|LUMP_SUM|DIRECT_DEBIT|
    DEDUCTION_FROM_BENEFIT),
  instalment_amount, frequency,
  start_date, end_date,
  status ENUM(ACTIVE|BREACHED|CANCELLED|
    COMPLETED|SUSPENDED),
  approved_by FK USER,
  affordability_assessment_id FK IE,
  direct_debit_reference,
  created_at, updated_at}
-- end_date extended by moratorium duration on
--   breathing space resume (BR-ARRANGEMENT-001).

ARRANGEMENT_BREACH {id PK, arrangement_id FK,
  breach_date, missed_amount, breach_count,
  action_taken ENUM(
    WARNING|SUSPENSION|TERMINATION|ESCALATION),
  resolved_at}
-- INVARIANT: No ARRANGEMENT_BREACH created during active
--   BREATHING_SPACE_STATUTORY or
--   MENTAL_HEALTH_CRISIS_STATUTORY suppression.

INCOME_EXPENDITURE {id PK, party_id FK,
  assessment_date, income_total, expenditure_total,
  disposable_income, affordability_score,
  formula_version_id FK, assessed_by FK USER,
  data_source ENUM(
    AGENT_INPUT|BUREAU_FEED|SELF_DECLARED|OPEN_BANKING),
  raw_data JSONB, created_at}
-- DATA CLASSIFICATION: raw_data = Restricted.
--   Access: SPECIALIST_AGENT and above only.
-- Staleness: assessments > 12 months trigger soft warning.

AFFORDABILITY_FORMULA_VERSION {id PK, version_number,
  formula_config JSONB, effective_from, effective_to,
  created_by FK USER}

FORBEARANCE_RECORD {id PK, account_id FK,
  forbearance_type ENUM(
    PAYMENT_HOLIDAY|INTEREST_FREEZE|
    REDUCED_PAYMENT|BREATHING_SPACE|HARDSHIP),
  recommended_type,
  agent_selected_type,
  start_date, end_date, reason,
  approved_by FK USER, created_at}

PAYMENT {uuid id PK, account_id FK,
  payment_ref UNIQUE, amount, currency,
  payment_method ENUM(
    DIRECT_DEBIT|CARD|BANK_TRANSFER|CASH|
    DEDUCTION_FROM_BENEFIT|THIRD_PARTY),
  status ENUM(
    PENDING|CLEARED|FAILED|REFUNDED|REVERSED|SUSPENSE),
  payment_date, received_at, gateway_ref, created_at}
-- REFUNDED: customer-requested return.
-- REVERSED: DWP-initiated correction or chargeback.
--   Compensating PAYMENT_ALLOCATION entries required.
-- CLEARED (debtor-initiated): resets limitation clock.
-- DEDUCTION_FROM_BENEFIT: DWP-initiated —
--   does NOT reset limitation clock.

PAYMENT_ALLOCATION {id PK, payment_id FK,
  account_id FK,
  allocated_to ENUM(PRINCIPAL|INTEREST|FEES|CHARGES),
  amount, allocated_at}

SUSPENSE_PAYMENT {id PK, payment_id FK, reason_code,
  reviewed_by FK USER, resolved_at,
  resolution ENUM(ALLOCATED|REFUNDED|
    WRITTEN_OFF|PENDING)}

COMMUNICATION {uuid id PK, party_id FK, case_id FK,
  template_id FK, thread_id FK,
  channel ENUM(LETTER|SMS|EMAIL|IN_APP|PHONE),
  direction ENUM(OUTBOUND|INBOUND),
  status ENUM(QUEUED|SENT|DELIVERED|FAILED|READ|DISCARDED),
  communication_category ENUM(
    DEBT_COLLECTION|NON_COLLECTION|
    DUAL_USE|ESTATE_ADMINISTRATION),
  suppression_reason VARCHAR,
  content_snapshot_encrypted,
  sent_at, delivered_at, notify_ref, created_at}
-- communication_category: inherited from template.
-- DUAL_USE override: TEAM_LEADER role + AUDIT_EVENT.
-- suppression_reason: set when status = QUEUED due to
--   suppression; used by lift process for disposition.
-- DISCARDED: queued DEBT_COLLECTION comms discarded on
--   statutory suppression lift (BR-COMMS-001).

COMMUNICATION_TEMPLATE {id PK, template_code UNIQUE,
  template_name, channel,
  communication_category ENUM(
    DEBT_COLLECTION|NON_COLLECTION|
    DUAL_USE|ESTATE_ADMINISTRATION),
  subject, body_template, version,
  is_active, created_at}
-- ESTATE_ADMINISTRATION templates: Phase 2 only.

COMMUNICATION_THREAD {id PK, party_id FK, subject,
  created_at, last_message_at}

THIRD_PARTY_PLACEMENT {id PK, account_id FK, dca_id FK,
  placement_ref, placed_at, recalled_at,
  status ENUM(ACTIVE|RECALLED|CLOSED),
  commission_rate, amount_placed, amount_recovered,
  disclosure_notice_id FK DISCLOSURE_NOTICE,
  data_fields_shared JSONB,
  lawful_basis_article VARCHAR(20),
  recall_acknowledged_at TIMESTAMP WITH TIME ZONE}
-- disclosure_notice_id: NON-NULLABLE.
-- dca.placement.notice_period_days in SYSTEM_CONFIG.

DCA {id PK, name, api_endpoint, authorisation_code,
  is_active, contract_start, contract_end}

USER {uuid id PK, keycloak_user_id,
  username, full_name, email,
  role ENUM(AGENT|SPECIALIST_AGENT|TEAM_LEADER|
    OPS_MANAGER|COMPLIANCE|ADMIN|BACKOFFICE|
    FLOWABLE_ADMIN|SRO|SYSTEM),
  team_id FK TEAM, is_active, created_at}
-- keycloak_user_id: nullable at DB level for SYSTEM user;
--   non-null enforced at service layer for human users.
-- SYSTEM user: id = 00000000-0000-0000-0000-000000000001

TEAM {id PK, team_name,
  team_type ENUM(GENERAL|SPECIALIST|ESCALATIONS|
    LEGAL|COMPLIANCE|BACKOFFICE),
  manager_id FK USER, created_at}

BROADCAST_MESSAGE {id PK, title, body,
  severity ENUM(INFO|WARNING|CRITICAL),
  created_by FK USER, published_at, expires_at}

SYSTEM_CONFIG {id PK, config_key UNIQUE, config_value,
  description, last_updated_by FK USER, updated_at}
-- Key entries:
--   statute_barred_calculation.enabled (default: false)
--   limitation_period.{debt_type} (awaiting DDE-OQ-08)
--   mhc_escalation_days
--   dca.placement.notice_period_days (awaiting DDE-OQ-07)
--   write_off_limit.AGENT|TL|OPS_MANAGER (provisional)

AUDIT_EVENT {uuid id PK, entity_type, entity_id UUID,
  event_type ENUM(
    CREATE|READ_SENSITIVE|UPDATE|DELETE|
    STATUS_CHANGE|LOGIN|OVERRIDE|WRITE_OFF_APPROVAL|
    DECEASED_FLAG_SET|SUPPRESSION_LIFTED|
    STATUTE_BARRED_FLAG_SET|MANUAL_REVIEW_REQUIRED),
  actor_id FK USER, actor_role,
  old_value JSONB, new_value JSONB,
  ip_address INET, session_id,
  occurred_at TIMESTAMP WITH TIME ZONE}
-- IMMUTABLE: no UPDATE or DELETE permitted.
-- actor_id = SYSTEM UUID for automated events.

MIGRATION_BATCH {id PK, batch_name, source_system,
  started_at, completed_at,
  status ENUM(PENDING|RUNNING|COMPLETED|FAILED|PARTIAL),
  record_count, error_count}

MIGRATION_RECORD {id PK, batch_id FK, source_ref,
  target_entity_type, target_entity_id UUID,
  status ENUM(PENDING|MIGRATED|FAILED|SKIPPED),
  error_message, processed_at}
-- FAILED: cause_of_action_date cannot be derived
--   from source data.

Relationships:
  PARTY ||--o{ PARTY_ADDRESS
  PARTY ||--o{ PARTY_CONTACT
  PARTY_CONTACT ||--o{ CONTACT_CONSENT
  PARTY ||--o{ VULNERABILITY_ASSESSMENT
  VULNERABILITY_ASSESSMENT }o--o{ VULNERABILITY_TYPE
  PARTY ||--o{ FRAUD_FLAG
  PARTY ||--o{ DEBT_ACCOUNT
  DEBT_ACCOUNT }o--o{ DEBT_ACCOUNT (via JOINT_DEBT_LINK)
  DEBT_ACCOUNT ||--o{ DEBT_STATUS_HISTORY
  DEBT_ACCOUNT ||--o{ INTEREST_ACCRUAL
  DEBT_ACCOUNT ||--o{ LEGAL_HOLD
  DEBT_ACCOUNT ||--o{ WRITE_OFF
  DEBT_ACCOUNT ||--o{ STRATEGY_ASSIGNMENT
  DEBT_ACCOUNT ||--o{ CASE
  DEBT_ACCOUNT ||--o{ SUPPRESSION_LOG
  CASE ||--o{ CASE_NOTE
  CASE ||--o{ PROCESS_CASE_LINK
  CASE ||--o{ COMMUNICATION
  REPAYMENT_ARRANGEMENT ||--o{ ARRANGEMENT_BREACH
  PAYMENT ||--o{ PAYMENT_ALLOCATION
  STRATEGY_ASSIGNMENT ||--o{ STRATEGY_OUTCOME_METRIC
  USER ||--o{ AUDIT_EVENT
  DEBT_ACCOUNT ||--o{ DISPUTE
  CASE ||--o{ DISPUTE
```

---

## 5. Actor Model & Use Cases

### 5.1 Actors

| Actor | Description | System Role |
|---|---|---|
| AGENT | Front-line debt collector | Keycloak AGENT role |
| SPECIALIST_AGENT | Handles vulnerable, complex, fraud-flagged customers | Keycloak SPECIALIST_AGENT role |
| TEAM_LEADER | Manages agents, approves tier-2 write-offs | Keycloak TEAM_LEADER role |
| OPS_MANAGER | Full operational access, system config, tier-3 write-off | Keycloak OPS_MANAGER role |
| COMPLIANCE | Read-only access + audit trail review | Keycloak COMPLIANCE role |
| ADMIN | User management and system configuration | Keycloak ADMIN role |
| BACKOFFICE | Payment processing, correspondence | Keycloak BACKOFFICE role |
| FLOWABLE_ADMIN | Direct process instance manipulation via Admin UI | Keycloak FLOWABLE_ADMIN — system administrator only |
| SRO | Write-off approval above OPS_MANAGER limit | Keycloak SRO — placeholder pending DDE-OQ-03 |
| DCA (External) | Third-party debt collection agency | Service account (client credentials) |
| System | Flowable automated tasks | OAuth2 client credentials |

### 5.2 Use Cases

**UC-01: Inbound Contact & Case Management**  
Actor: AGENT | Trigger: Customer calls about debt

1. Search customer by name / NI / account ref
2. Verify identity (PARTY details)
3. Open or locate existing CASE
4. Log contact attempt (CASE_NOTE)
5. Update case disposition
6. Queue outbound communication
7. Flowable COLLECTION_PROCESS advances on `CONTACT_MADE` signal

---

**UC-02: Repayment Arrangement**  
Actor: AGENT / SPECIALIST_AGENT | Trigger: Customer agrees to pay

1. Complete Income & Expenditure form → INCOME_EXPENDITURE record
2. Check I&E staleness — warn if assessment > 12 months old
3. Affordability DMN runs → minimum instalment calculated
4. REPAYMENT_ARRANGEMENT created; limitation clock reset recorded
5. If DD: mandate setup (Payment Gateway stub)
6. Flowable starts ARRANGEMENT_MONITORING subprocess
7. Constraint: I&E required if outstanding balance > £500 (RPF.8)
8. DEDUCTION_FROM_BENEFIT: I&E required unless DDE-OQ-06 confirms
   exemption

---

**UC-03: Breathing Space Activation**  
Actor: AGENT / SYSTEM | Trigger: Customer requests or referral received

1. Verify eligibility
2. Determine type: STANDARD or MENTAL_HEALTH_CRISIS
3. Set `breathing_space_flag` and `breathing_space_type` via
   `CommunicationSuppressionService`
4. Create `LEGAL_HOLD`:
   - STANDARD: `BREATHING_SPACE_STANDARD`, `end_date = today + 60d`
   - MENTAL_HEALTH_CRISIS: `BREATHING_SPACE_MENTAL_HEALTH_CRISIS`,
     `end_date = NULL`
5. Create `SUPPRESSION_LOG` entry
6. Suppress `DEBT_COLLECTION` communications only
7. Suspend (not cancel) any active `REPAYMENT_ARRANGEMENT`
8. Flowable:
   - STANDARD → intermediate timer catch event `PT1440H`
   - MENTAL_HEALTH_CRISIS → manual path; boundary escalation timer
     from `SYSTEM_CONFIG: mhc_escalation_days`
9. At expiry: discard queued DEBT_COLLECTION comms; lift flags;
   check remaining suppressions; resume arrangement monitoring;
   extend arrangement end date; send NON_COLLECTION revised
   schedule notice

---

**UC-04: Multi-Tier Write-Off Approval**  
Actor: AGENT → TEAM_LEADER → OPS_MANAGER → SRO

1. AGENT submits request — `requested_by` set
2. `write_off_limit_check` DMN evaluates amount vs role limits
3. `CreateTaskListener` removes requestor from candidate group;
   if group becomes empty, conflict flagged to COMPLIANCE
4. Tier 1 (≤ £500 provisional): auto-approved
5. Tier 2 (≤ £2,000 provisional): TEAM_LEADER
6. Tier 3 (≤ £10,000 provisional): OPS_MANAGER
7. Tier 4 (> £10,000): SRO (pending DDE-OQ-03)
8. COMPLIANCE notified at all tiers (not as approver)
9. `WRITE_OFF` record created; `AUDIT_EVENT` logged

---

**UC-05: Champion/Challenger Assignment**  
Actor: SYSTEM | Trigger: Account enters collection pipeline

1. Check `is_statute_barred` — if true, route to LEGAL_REVIEW
2. Check `vulnerability_flag` — if true, assign CHAMPION;
   set `vulnerability_override = true`
3. Segmentation DMN runs
4. If active STRATEGY_TEST: `champion_challenger_assignment` DMN
5. `STRATEGY_ASSIGNMENT` record created
6. `STRATEGY_OUTCOME_METRIC` written for all accounts

---

**UC-06: Vulnerability Management**  
Actor: SPECIALIST_AGENT / AGENT

1. Create `VULNERABILITY_ASSESSMENT` (IDENTIFIED)
   — AGENT sees soft flag; collection activity blocked
2. SPECIALIST_AGENT classifies types (Restricted data)
3. Advance ASSESSED → ACTIVE (SPECIALIST_AGENT access only)
4. Create `SUPPRESSION_LOG` entry: `VULNERABILITY_POLICY`
5. Review date set; flag visible on account
6. RESOLVED: routing restored; suppression log closed

---

**UC-07: DCA Placement**  
Actor: TEAM_LEADER / OPS_MANAGER

1. Check `SUPPRESSION_LOG` — block if any statutory suppression
2. Check `is_statute_barred` — block if true
3. Send pre-placement `DISCLOSURE_NOTICE`
4. Flowable timer: wait `dca.placement.notice_period_days`
5. Create `THIRD_PARTY_PLACEMENT` — `disclosure_notice_id`
   non-nullable; `data_fields_shared` records personal data
6. On recall: DCA notified immediately;
   `recall_acknowledged_at` recorded; suppression log closed

---

**UC-08: Compliance Audit Review**  
Actor: COMPLIANCE

1. Access `AUDIT_EVENT` log — filter by entity, actor, date, type
2. View `DECISION_TRACE` entries
3. View `SUPPRESSION_LOG` history
4. Export report (CSV / JSON)
5. Constraint: read-only; `AUDIT_EVENT` is immutable

---

**UC-09: Runtime Rule Update**  
Actor: OPS_MANAGER

1. Navigate to Flowable Admin UI (port 8091)
2. Open DMN decision table
3. Edit rules — no redeployment required (DW.38)
4. `DECISION_TRACE` confirms new rule in effect
5. Note: direct process instance manipulation requires
   `FLOWABLE_ADMIN` role

---

**UC-10: Household Creation & Joint Debt Split**  
Actor: AGENT / TEAM_LEADER

1. Create / locate two PARTY records
2. Create DEBT_ACCOUNT; create JOINT_DEBT_LINK
3. Household view: both parties, shared balance, benefit_status
4. Block split if: active LEGAL_HOLD, BREATHING_SPACE,
   INSOLVENCY flag, open DISPUTE, or active
   REPAYMENT_ARRANGEMENT
5. `joint_debt_split.dmn` fires; two child accounts at balance/2;
   residual ≤ 1p auto-written-off (SYSTEM approval tier);
   original CLOSED
6. TEAM_LEADER ratio override available before confirmation
7. Full `AUDIT_EVENT` trail

---

**UC-11: Dispute Management**  
Actor: AGENT / SPECIALIST_AGENT / COMPLIANCE

1. Agent creates DISPUTE record
2. `dispute_handling.dmn` → queue routing
3. `ActivateDisputeSuppressionDelegate` creates
   `SUPPRESSION_LOG` entry: `DISPUTE_INTERNAL`
4. If FRAUD_DISPUTE: `FRAUD_FLAG` raised; routed to
   SPECIALIST_AGENT
5. On RESOLVED: `LiftDisputeSuppressionDelegate` closes
   suppression log; remaining suppressions checked;
   `AUDIT_EVENT` logged

---

**UC-NEW-01: Deceased Party Handling**  
Actor: AGENT / SYSTEM | Sprint 1 minimum; estate track Phase 2

**Phase 1 — Transactional (atomic):**

1. TEAM_LEADER confirms deceased flag
2. `PARTY.deceased_flag = true`, `deceased_flagged_at`,
   `deceased_flagged_by` set
3. `SUPPRESSION_LOG` entry: `DECEASED_MANDATORY`
4. `AUDIT_EVENT`: `DECEASED_FLAG_SET`

**Phase 2 — Non-transactional (failures logged, do not roll back):**

5. Query `JOINT_DEBT_LINK` for all accounts
6. Sole-holder accounts: suspend Flowable process instances
   individually; catch exceptions; log `MANUAL_REVIEW_REQUIRED`
7. Joint-holder accounts: do NOT suspend; create
   SPECIALIST_AGENT `WORK_QUEUE` task
8. Active `REPAYMENT_ARRANGEMENT`: status = SUSPENDED

---

## 6. RBAC Permissions Matrix

### 6.1 Roles Summary

| Role | Scope | Write-Off Limit | Key Restriction |
|---|---|---|---|
| AGENT | Own assigned cases | £500 (provisional) | No override, no queue admin |
| SPECIALIST_AGENT | Vulnerability / complex cases | £500 (provisional) | Specialist queue only |
| TEAM_LEADER | Team's cases + override | £2,000 (provisional) | |
| OPS_MANAGER | All cases + system config | £10,000 (provisional) | No direct Flowable process manipulation |
| COMPLIANCE | All read + audit | £0 | Read-only |
| ADMIN | User/config management | £0 | No case access |
| BACKOFFICE | Payment/correspondence | £0 | Own work queue only |
| FLOWABLE_ADMIN | Direct Flowable process manipulation | £0 | System administrator only |
| PROCESS_DESIGNER | Flowable Modeler design + dev deploy | £0 | No case access; no prod deploy |
| SRO | Write-off above OPS_MANAGER limit | TBD | Pending DDE-OQ-03 |

> Write-off limits are provisional. Confirmed values loaded post-award per
> DDE-OQ-02. Self-approval prohibited for all roles.

### 6.2 Feature Permissions

| Feature | AGENT | SPEC | TL | OPS | COMP | ADMIN | BO | FA | PD |
|---|---|---|---|---|---|---|---|---|---|
| View customer/account | Own | Assigned | Team | All | All | No | Queue | No | No |
| Edit customer details | Own | Assigned | Team | All | No | No | Queue | No | No |
| View vulnerability assessment (full) | Flag only | Yes | Yes | Yes | Yes | No | No | No | No |
| View vulnerability types (health) | No | Yes | Yes | Yes | Yes | No | No | No | No |
| View I&E raw_data | No | Yes | Yes | Yes | Yes | No | No | No | No |
| Create repayment plan | Yes | Yes | Yes | Yes | No | No | No | No | No |
| Approve write-off tier 1 | Yes | Yes | No | No | No | No | No | No | No |
| Approve write-off tier 2 | No | No | Yes | No | No | No | No | No | No |
| Approve write-off tier 3 | No | No | No | Yes | No | No | No | No | No |
| Self-approve write-off | No | No | No | No | No | No | No | No | No |
| Override Flowable user task (app) | No | No | Yes | Yes | No | No | No | No | No |
| Direct Flowable process manipulation | No | No | No | No | No | No | No | Yes | No |
| Edit DMN rules (Flowable UI) | No | No | No | Yes | No | No | No | Yes | Yes |
| View process instances (read-only) | No | No | No | Yes | No | No | No | Yes | Yes |
| View all work queues | No | No | Yes | Yes | Yes | No | No | No | No |
| Configure SYSTEM_CONFIG | No | No | No | Yes | No | Yes | No | No | No |
| Manage users | No | No | No | No | No | Yes | No | No | No |
| Access AUDIT_EVENT | No | No | No | Yes | Yes | No | No | No | No |
| Export compliance reports | No | No | Yes | Yes | Yes | No | No | No | No |
| DCA placement | No | No | Yes | Yes | No | No | No | No | No |
| Activate breathing space (standard) | Yes | Yes | Yes | Yes | No | No | No | No | No |
| Activate breathing space (MH crisis) | No | Yes | Yes | Yes | No | No | No | No | No |
| Confirm deceased flag | No | No | Yes | Yes | No | No | No | No | No |
| Vulnerability assessment | Yes | Yes | Yes | Yes | No | No | No | No | No |
| DUAL_USE communication override | No | No | Yes | Yes | No | No | No | No | No |

> FA = FLOWABLE_ADMIN

### 6.3 Keycloak Configuration

```
Realm: dcms
Clients:
  dcms-frontend   (public, PKCE, redirect: http://localhost:3000/*)
  dcms-backend    (confidential, client credentials)
  flowable-admin  (confidential, Flowable Admin UI SSO)

Realm Roles:
  AGENT, SPECIALIST_AGENT, TEAM_LEADER, OPS_MANAGER,
  COMPLIANCE, ADMIN, BACKOFFICE, FLOWABLE_ADMIN, PROCESS_DESIGNER, SRO, SYSTEM

JWT claim path: realm_access.roles
Spring usage:   @PreAuthorize("hasRole('TEAM_LEADER')")
```

---

## 7. Workflow & Decision Architecture (Flowable)

### 7.1 Delegate Naming Convention

```
All Flowable service task delegates follow `{Action}{Subject}Delegate`:
ActivateBreathingSpaceSuppressionDelegate
LiftBreathingSpaceSuppressionDelegate
DiscardQueuedCollectionCommunicationsDelegate
ResumeArrangementMonitoringDelegate
ActivateDisputeSuppressionDelegate
LiftDisputeSuppressionDelegate
SetDeceasedFlagDelegate
CheckStatuteBarredDelegate
RunSegmentationDmnDelegate
RunChampionChallengerDmnDelegate
```

### 7.2 Process Variable Live State Rule

Delegates must read the following fields from the database at execution
time — not from Flowable process variables:

- `DEBT_ACCOUNT.breathing_space_flag`
- `DEBT_ACCOUNT.deceased_flag`
- `DEBT_ACCOUNT.is_statute_barred`
- `DEBT_ACCOUNT.benefit_status`
- `VULNERABILITY_ASSESSMENT.status`

### 7.3 Transaction Boundary Rule

Application database writes are always inside `@Transactional`. Flowable
engine calls are always outside. Methods calling Flowable services must not
be annotated `@Transactional`. Flowable call failures are caught, logged as
`MANUAL_REVIEW_REQUIRED`, and do not roll back committed application state.

Exception: `extendArrangementEndDate()` uses
`@Transactional(propagation = REQUIRES_NEW)`.

### 7.4 Process Definitions

#### COLLECTION_PROCESS

```
Start Event
│
▼
CheckStatuteBarredDelegate (reads from DB)
│
▼
Gateway: Statute barred?
├─ [Yes] User Task: Legal Review [TEAM_LEADER/OPS_MANAGER]
│        Signal catch: STATUTE_BARRED_CLEARED → re-route
└─ [No]  RunSegmentationDmnDelegate
         │
         ▼
         Vulnerability check (reads VULNERABILITY_ASSESSMENT from DB)
         │
         ▼
         Gateway: Vulnerability flag?
         ├─ [Yes] Assign CHAMPION; vulnerability_override=true
         └─ [No]  RunChampionChallengerDmnDelegate
                  │
                  ▼
                  User Task: Initial Contact Attempt [AGENT]
                  Timer (3 days, repeat ×3)
                  │
                  ▼
                  Gateway: Contact Made?
                  ├─ [No ×3] Escalation → TEAM_LEADER queue
                  └─ [Yes]   User Task: Review & Propose Treatment
                             │
                             ├─ Sub-Process: ARRANGEMENT_MONITORING
                             ├─ Sub-Process: WRITE_OFF_APPROVAL
                             └─ Service Task: LEGAL_ACTION_PROCESS
│
▼
End Event
```

#### WRITE_OFF_APPROVAL_PROCESS

```
Start Event
│
▼
Run write_off_limit_check DMN
│
▼
Gateway: Within AGENT limit?
├─ [Yes] Auto-approve → End
└─ [No]  User Task: Team Leader Review [TEAM_LEADER]
         (CreateTaskListener removes requestor;
          empty group check → COMPLIANCE notification)
         │
         ▼
         Gateway: Within TL limit?
         ├─ [Yes] Approve → Notify COMPLIANCE → End
         └─ [No]  User Task: Ops Manager Review [OPS_MANAGER]
                  (CreateTaskListener removes requestor)
                  │
                  ▼
                  Gateway: Within OPS limit?
                  ├─ [Yes] Approve → Notify COMPLIANCE → End
                  └─ [No]  User Task: SRO Review [SRO]
                           (CreateTaskListener removes requestor;
                            empty group → MANUAL_REVIEW_REQUIRED)
                           │
                           ▼
                           Approve → Notify COMPLIANCE → End
```

#### BREATHING_SPACE_PROCESS

```
Start Event (breathingSpaceType variable)
│
▼
Gateway: Type?
│
├─ [STANDARD] ──────────────────────────────────────────────────────────────────┐
│                                                                                │
│  ActivateBreathingSpaceSuppressionDelegate                                     │
│    (sets breathing_space_flag via CommunicationSuppressionService,             │
│     creates LEGAL_HOLD BREATHING_SPACE_STANDARD end_date=+60d,                 │
│     creates SUPPRESSION_LOG BREATHING_SPACE_STATUTORY,                         │
│     sets arrangementSuspendedAt on ARRANGEMENT_MONITORING_PROCESS              │
│       via runtimeService.setVariable() BEFORE suspending,                      │
│     suspends ARRANGEMENT_MONITORING_PROCESS instances)                         │
│  │                                                                             │
│  ▼                                                                             │
│  Intermediate Timer Catch Event: ${breathingSpaceEndDateISO}                   │
│  │                                                                             │
│  ▼                                                                             │
│  DiscardQueuedCollectionCommunicationsDelegate                                 │
│  │                                                                             │
│  ▼                                                                             │
│  LiftBreathingSpaceSuppressionDelegate                                         │
│  │                                                                             │
│  ▼                                                                             │
│  ResumeArrangementMonitoringDelegate                                           │
│    (@Transactional(REQUIRES_NEW) for extendArrangementEndDate;                 │
│     MANUAL_REVIEW_REQUIRED if revised schedule notice suppressed)              │
│  │                                                                             │
│  ▼                                                                             │
│  User Task: Post-review [SPECIALIST_AGENT]                                     │
│  │                                                                             │
│  ▼                                                                             │
│  End Event                                                          ◄──────────┘
│
└─ [MENTAL_HEALTH_CRISIS] ───────────────────────────────────────────────────────┐
                                                                                  │
   ActivateBreathingSpaceSuppressionDelegate                                      │
     (LEGAL_HOLD BREATHING_SPACE_MENTAL_HEALTH_CRISIS end_date=NULL)              │
   │                                                                              │
   ▼                                                                              │
   User Task: Awaiting MH confirmation [SPECIALIST_AGENT]                         │
   │  Boundary Escalation Timer: ${mhcEscalationDays}d                            │
   │  └─ [Escalation] User Task: MH Crisis Review [OPS_MANAGER]                  │
   │                                                                              │
   ▼ [Confirmed]                                                                  │
   DiscardQueuedCollectionCommunicationsDelegate                                  │
   │                                                                              │
   ▼                                                                              │
   LiftBreathingSpaceSuppressionDelegate                                          │
   │                                                                              │
   ▼                                                                              │
   ResumeArrangementMonitoringDelegate                                            │
   │                                                                              │
   ▼                                                                              │
   End Event                                                           ◄──────────┘
```


#### ARRANGEMENT_MONITORING_PROCESS

Process variables:

| Variable | Type | Purpose |
|---|---|---|
| `arrangementMonitoringStep` | String | Current step: AWAITING_TIMER \| CHECKING_PAYMENT \| BREACH_EVALUATION \| AGENT_CONTACT_TASK |
| `nextDueDateISO` | String (ISO-8601) | Timer catch event date expression |
| `arrangementSuspendedAt` | String (ISO-8601) | Set by ActivateBreathingSpaceSuppressionDelegate before suspension |
| `skipBreachOnResume` | Boolean | Prevents false breach on moratorium-interrupted cycle |

```
Start Event
│  [arrangementMonitoringStep = AWAITING_TIMER]
▼
Intermediate Timer Catch Event: ${nextDueDateISO}
│  [arrangementMonitoringStep = CHECKING_PAYMENT]
▼
Check breathing_space_flag (reads from DB — ADR-009)
│
▼
Gateway: Breathing space active?
├─ [Yes] Advance to next cycle; loop ──────────────────────────────────────► ↑
└─ [No]
   │
   ▼
   Gateway: skipBreachOnResume?
   ├─ [Yes] Reset flag; advance to next cycle ────────────────────────────► ↑
   └─ [No]
      │
      ▼
      Gateway: Payment received?
      ├─ [Yes]
      │  │
      │  ▼
      │  Gateway: Final payment?
      │  ├─ [Yes] Complete → End
      │  └─ [No]  Loop ──────────────────────────────────────────────────► ↑
      └─ [No]
         │  [arrangementMonitoringStep = BREACH_EVALUATION]
         ▼
         Increment breach count
         │
         ▼
         Gateway: Breach number?
         ├─ [1st] [arrangementMonitoringStep = AGENT_CONTACT_TASK]
         │        User Task: Contact [AGENT]
         ├─ [2nd] Warning letter
         └─ [3rd] Escalate to TEAM_LEADER
```

### 7.5 DMN Decision Tables

#### segmentation.dmn — Hit Policy: PRIORITY

| Pri | debt_type | balance | delinquency_days | vuln_flag | statute_barred | treatment_path | priority_level |
|---|---|---|---|---|---|---|---|
| 1 | ANY | any | any | any | true | LEGAL_REVIEW | CRITICAL |
| 2 | ANY | any | any | true | false | SPECIALIST | CRITICAL |
| 3 | BENEFIT_FRAUD | any | any | false | false | FRAUD_TRACK | HIGH |
| 4 | OVERPAYMENT | ≥5000 | >90 | false | false | HIGH_VALUE_ENFORCEMENT | HIGH |
| 5 | ANY | any | >180 | false | false | LEGAL_CONSIDERATION | HIGH |
| 6 | OVERPAYMENT | any | ≤30 | false | false | EARLY_INTERVENTION | MEDIUM |
| 7 | ANY | any | any | false | false | STANDARD | LOW |

#### write_off_limit_check.dmn — Hit Policy: UNIQUE

> All thresholds provisional pending DDE-OQ-02.

| requestor_role | write_off_amount | approved | escalate_to |
|---|---|---|---|
| AGENT | ≤ 500 | true | — |
| AGENT | > 500 | false | TEAM_LEADER |
| TEAM_LEADER | ≤ 2000 | true | — |
| TEAM_LEADER | > 2000 | false | OPS_MANAGER |
| OPS_MANAGER | ≤ 10000 | true | — |
| OPS_MANAGER | > 10000 | false | SRO |

#### channel_selection.dmn

| consent_email | consent_sms | vuln_flag | preferred_channel | fallback |
|---|---|---|---|---|
| any | any | true | LETTER | — |
| true | any | false | EMAIL | SMS |
| false | true | false | SMS | LETTER |
| false | false | false | LETTER | — |

#### plan_suitability.dmn — Hit Policy: PRIORITY

| Pri | disposable_income | outstanding_balance | plan_type | min_instalment |
|---|---|---|---|---|
| 1 | any | > 10000 | LEGAL_CONSIDERATION | — |
| 2 | < 50 | any | REDUCED_PLAN | £1 |
| 3 | 50–200 | ≤ 5000 | STANDARD_INSTALMENT | disposable × 0.5 |
| 4 | > 200 | any | ACCELERATED_PLAN | disposable × 0.7 |

#### champion_challenger_assignment.dmn — Hit Policy: PRIORITY

| Pri | active_test | vuln_flag | random (0–100) | split_pct | variant | vuln_override |
|---|---|---|---|---|---|---|
| 1 | any | true | any | any | CHAMPION | true |
| 2 | true | false | ≤ split_pct | any | CHALLENGER | false |
| 3 | true | false | > split_pct | any | CHAMPION | false |
| 4 | false | false | any | any | CHAMPION | false |

#### joint_debt_split.dmn

| split_trigger | balance_pence mod 2 | split_method | auto_write_off_residual | residual_max_pence |
|---|---|---|---|---|
| MANUAL | 0 | EQUAL_SPLIT | false | 0 |
| MANUAL | 1 | EQUAL_SPLIT | true | 1 |
| AUTO_THRESHOLD | any | EQUAL_SPLIT | true | 1 |

#### dispute_handling.dmn

| dispute_type | vuln_flag | queue_type | assigned_queue | action_required |
|---|---|---|---|---|
| FRAUD_DISPUTE | any | SPECIALIST_EXCEPTION | SPECIALIST | FRAUD_INVESTIGATION |
| PAYMENT_DISPUTE | true | SPECIALIST_EXCEPTION | SPECIALIST | SPECIALIST_REVIEW |
| PAYMENT_DISPUTE | false | EXCEPTION | STANDARD_EXCEPTION | AGENT_REVIEW |
| PROCESS_DISPUTE | any | EXCEPTION | COMPLIANCE_QUEUE | COMPLIANCE_REVIEW |
| OTHER | any | EXCEPTION | STANDARD_EXCEPTION | AGENT_REVIEW |

#### forbearance_suitability.dmn — Hit Policy: PRIORITY

| Pri | disposable_income | vuln_flag | bs_active | recommended_forbearance | review_days |
|---|---|---|---|---|---|
| 1 | any | any | true | [already active] | — |
| 2 | ≤ 0 | any | false | BREATHING_SPACE | 60 |
| 3 | >0 and <50 | true | false | HARDSHIP | 30 |
| 4 | >0 and <50 | false | false | REDUCED_PAYMENT | 30 |
| 5 | 50–100 | any | false | PAYMENT_HOLIDAY | 14 |
| 6 | > 100 | any | false | INTEREST_FREEZE | 30 |

### 7.6 Flowable Spring Boot Configuration

```yaml
flowable:
  async-executor-activate: true
  database-schema-update: true
  process:
    definition-cache-limit: 50
  idm:
    enabled: false
  rest:
    enabled: false
  job-executor:
    thread-count: 5

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/dcms
    hikari:
      schema: flowable
  flyway:
    locations:
      - classpath:db/migration/schema
      - classpath:db/migration/seed
    schemas: public, flowable
  threads:
    virtual:
      enabled: true
```

### 7.7 Flowable Admin UI Docker Compose

```yaml
flowable-ui:
  image: flowable/flowable-ui:6.8.0
  ports: ["8091:8080"]
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/dcms
    FLOWABLE_COMMON_APP_IDM-URL: http://keycloak:8080
  depends_on: [postgres, keycloak]
```

Capabilities:
- BPMN process instance monitoring — active instances, variables, token position
- DMN decision table editing **without redeployment** — satisfies DW.38
- CMMN case instance management
- User task reassignment and management — satisfies WAM.24

---

## 8. Champion/Challenger A/B Testing Design

### 8.1 Purpose

Enables DWP to scientifically compare two collection strategies on live
accounts, measure recovery outcomes, and promote the winner without
disrupting the broader system.

### 8.2 Assignment Flow

```
Account enters collection pipeline
    │
    ▼
Statute-barred check: if true → LEGAL_REVIEW (no assignment)
    │
    ▼
Vulnerability check: if true → CHAMPION (vulnerability_override=true)
    │
    ▼
Segmentation DMN: treatment_path, priority
    │
    ▼
STRATEGY_TEST active?
  ├─ No  → CHAMPION
  └─ Yes → champion_challenger_assignment DMN
               ├─ CHALLENGER
               └─ CHAMPION
                    │
                    ▼
             STRATEGY_ASSIGNMENT created
```

### 8.3 Outcome Tracking

`StrategyOutcomeListener` writes `STRATEGY_OUTCOME_METRIC` for all accounts
including vulnerable. Exclusion applied at query level only.

### 8.4 Analytics Dashboard — Three Panels

**Panel 1 — A/B Comparison (non-vulnerable only):**

```sql
SELECT sa.variant,
  COUNT(DISTINCT sa.account_id)                                   AS accounts,
  SUM(som.value) FILTER (WHERE som.metric_type='PAYMENT_MADE')    AS total_recovered,
  COUNT(*)       FILTER (WHERE som.metric_type='ARRANGEMENT_AGREED') AS arrangements,
  AVG(som.value) FILTER (WHERE som.metric_type='PAYMENT_MADE')    AS avg_payment
FROM strategy_assignment sa
LEFT JOIN strategy_outcome_metric som ON som.strategy_assignment_id = sa.id
WHERE sa.strategy_test_id = :testId
  AND sa.vulnerability_override = false
GROUP BY sa.variant;
```

**Panel 2 — Vulnerable Account Outcomes:**

```sql
SELECT COUNT(DISTINCT sa.account_id)                                AS vulnerable_accounts,
  SUM(som.value) FILTER (WHERE som.metric_type='PAYMENT_MADE')      AS total_recovered,
  COUNT(*)       FILTER (WHERE som.metric_type='ARRANGEMENT_AGREED') AS arrangements
FROM strategy_assignment sa
LEFT JOIN strategy_outcome_metric som ON som.strategy_assignment_id = sa.id
WHERE sa.strategy_test_id = :testId
  AND sa.vulnerability_override = true;
```

**Panel 3 — Overall Population:**

```sql
SELECT COUNT(DISTINCT sa.account_id)                             AS total_accounts,
  SUM(som.value) FILTER (WHERE som.metric_type='PAYMENT_MADE')   AS total_recovered
FROM strategy_assignment sa
LEFT JOIN strategy_outcome_metric som ON som.strategy_assignment_id = sa.id
WHERE sa.strategy_test_id = :testId;
```

### 8.5 Winner Promotion

1. OPS_MANAGER reviews analytics at test end
2. Promote winner API checks `min_sample_size` and `min_duration_days`
   (nullable for demo; populated post-award per DDE-OQ-09)
3. `winner_strategy_id` set; `status = COMPLETED`
4. Winning STRATEGY: `is_champion = true`; previous champion:
   `retired_at = now()`

---

## 9. Integration Architecture

### 9.1 External System Stubs

| System | Purpose | Protocol | Demo Control Endpoint |
|---|---|---|---|
| DWP Place | Address/identity verification | REST/JSON | `POST /mock/dwp-place/control` |
| DCA Placement API | Send account to agency | REST/JSON | `POST /mock/dca/control` |
| Payment Gateway | Card / DD processing | REST/JSON | `POST /mock/payment-gateway/control` |
| GOV.UK Notify | SMS/email despatch | REST/JSON | `POST /mock/notify/control` |
| RTBS Feed | Real-time benefit status | FTPS/CSV | `POST /mock/rtbs/control` |
| Credit Bureau | Address / income data | REST/JSON | `POST /mock/bureau/control` |
| DEA (Direct Earnings Attachment) | Instruct employer deduction from wages | REST/JSON stub | `POST /mock/dea/control` |

> **DEA — Phase 2 dependency:** The stub accepts a placement instruction and returns a mock confirmation. Production integration requires access to DWP's Deductions API (out of scope for this prototype). The stub demonstrates the collection channel exists and is selectable on a REPAYMENT_ARRANGEMENT (arrangement_type = `DEDUCTION_FROM_BENEFIT`). Demo 3 criterion requires DEA to be shown as a payment channel option.

> **Dialler/IVR — Phase 2:** Outbound dialler and IVR are DWP strategic components owned by the telephony estate. For the demo, the agent manually diarises a call via CASE_NOTE and triggers an SMS/letter via GOV.UK Notify stub. Full dialler integration (screen-pop, call recording, wrap codes) is documented as a Phase 2 integration point in the anti-corruption layer (`DiallerAdapter implements DiallerPort`).

### 9.2 Mock Control Pattern

```http
POST /mock/payment-gateway/control
Content-Type: application/json

{
  "scenario": "PAYMENT_SUCCESS",
  "responses": [
    { "status": 200, "body": { "txnRef": "TXN-DEMO-001",
                               "status": "CLEARED" } }
  ]
}
```

### 9.3 Anti-Corruption Layer

```
DWPPlaceAdapter       implements AddressVerificationPort
DCAAdapter            implements DCAPlacementPort
PaymentGatewayAdapter implements PaymentGatewayPort
NotifyAdapter         implements NotificationPort
  — calls CommunicationSuppressionService.isPermitted()
    before every dispatch (non-negotiable)
RTBSAdapter           implements BenefitStatusPort
DEAAdapter            implements DEAPlacementPort (stub; Phase 2)
DiallerAdapter        implements DiallerPort       (stub; Phase 2)
```

---

## 10. Pre-Build Phase (22 Apr – 1 May)

> Bank holiday: 4 May (UK Early May Bank Holiday). Sprint 1 effective start is Tuesday 5 May.

| Task | Owner | Days | Done When |
|---|---|---|---|
| Git repo setup, branch protection, CODEOWNERS | Dev1 | 0.5 | PR-only to main, CI required |
| GitHub Actions: build, test, lint pipeline | DevOps1 | 1 | Green on first PR |
| Docker Compose: all 5 services start cleanly | DevOps2 | 1 | `docker compose up` — all services healthy |
| Keycloak realm: all roles incl. FLOWABLE_ADMIN, SRO placeholder, test users per role | DevOps2 | 1 | Each role can log in; PKCE flow verified |
| Spring Boot scaffold: packages, Flyway (two locations), JPA, health endpoint | Dev1 | 1 | `/actuator/health` returns UP |
| Flowable spike (a): deploy BPMN, verify intermediate timer catch event fires correctly (PT1440H) — go/no-go written sign-off required | Dev1 | 0.5 | Timer fires correctly; process visible in Flowable UI. Written go/no-go recorded in `docs/project-foundation/architecture-decisions.md`. |
| Flowable spike (b): Keycloak 24 OIDC handshake against Flowable Admin UI container — full PKCE flow, JWT, JWKS validation — go/no-go written sign-off required | Dev1 + DevOps2 | 0.5 | SSO login to Flowable Admin UI succeeds with Keycloak-issued JWT. Written go/no-go by 30 April. If FAIL: architecture decision required — accept basic auth fallback and de-scope live DMN editing from demo, or escalate. |
| Flowable Admin UI SSO; FLOWABLE_ADMIN restriction verified | DevOps2 | 0.5 | OPS_MANAGER cannot manipulate instances directly |
| Provision PROCESS_DESIGNER Keycloak role; configure Flowable Modeler SSO to require it; add PROCESS_DESIGNER test user; smoke-test Modeler login | DevOps2 | 0.5 | PROCESS_DESIGNER test user can access Flowable Modeler; AGENT cannot |
| React scaffold: GOV.UK DS, routing, Keycloak PKCE | Dev4 | 1 | Protected route redirects to Keycloak login |
| AKS dev namespace + Helm chart skeleton | DevOps1 | 1 | `helm install dcms-dev ./helm` succeeds |
| Container registry access, image push/pull verified | DevOps1 | 0.5 | Tagged image pushed to `ufstpit-dev-docker.repo.netcompany.com` |
| ADR: Flowable chosen | Dev1 | 0.5 | ADR committed to `docs/` |
| Migration numbering convention in CONTRIBUTING.md | Dev1 | 0.5 | Published before Sprint 1 |
| QA: test strategy document + test framework scaffold (Playwright or REST Assured) | QA | 2 | Framework checked in; smoke test template passing |

---

## 11. Sprint Plan — 9 × 1-Week Sprints (5 May – 3 Jul)

> Note: 4 May is a UK bank holiday. Sprint 1 begins Tuesday 5 May and runs to Friday 8 May.

**Team roles across sprints:**

| Designation | Primary focus |
|---|---|
| Dev1 | Backend lead: Flowable engine, process definitions, complex business logic |
| Dev2 | Backend: payments, communications, audit |
| Dev3 | Backend: customer, account, vulnerability, repayment |
| Dev4 | Frontend lead: component library, routing, complex screens |
| Dev5 | Frontend: forms, worklists, communication screens |
| Dev6 | Full-stack: integration adapters/stubs, admin screens, analytics |
| Dev7 (if available) | Backend: overflow + BPMN implementation support |
| DevOps1 | CI/CD, containerisation, AKS, Helm, image registry, load testing |
| DevOps2 | Keycloak config, security infra, network policy, database ops |
| QA | Test plans, Playwright E2E automation, manual acceptance testing, WCAG checks |

**Migration block convention:**

| Sprint | Block | Assignment |
|---|---|---|
| 1 | V001–V009 | Claim specific numbers on sprint board before creating the file |
| 2 | V010–V019 | Same — board-claim prevents conflicts |
| 3 | V020–V029 | Same |
| 4 | V030–V039 | Same |
| 5 | V040–V049 | Same |
| 6 | V050–V059 | Same |
| 7 | V060–V069 | Same |
| 8 | V070–V079 | Same |
| 9 | V080–V089 | Same |
| Seed | V900+ | Separate Flyway location |

With multiple developers running parallel migrations, numbers must be claimed before the file is created — never assume the next free number without checking the board.

---

### Sprint 1: 5 May – 8 May — Foundation

**Goal:** Core entities, RBAC, deceased handling minimum, suppression log. CI/CD green. Test framework live.

| Story | Pts | Owner |
|---|---|---|
| V001–V003: TEAM (no FK), USER, TEAM ALTER; V900: SYSTEM user seed | 2 | Dev3 |
| V004–V006: PARTY, PARTY_ADDRESS/CONTACT, DEBT_ACCOUNT (all new fields) | 3 | Dev3 |
| V007–V009: AUDIT_EVENT, DEBT_STATUS_HISTORY, SUPPRESSION_LOG (full FKs, unique partial index) | 2 | Dev2 |
| Customer search API (backend) | 3 | Dev1 |
| Customer search screen — GOV.UK search pattern | 3 | Dev4 |
| Customer profile screen | 3 | Dev5 |
| Account detail screen (incl. `is_statute_barred` read-only field) | 3 | Dev4 |
| Household / joint account view | 3 | Dev5 |
| Spring Security RBAC (all roles including FLOWABLE_ADMIN, SRO) | 3 | Dev1 |
| Spring AOP audit interceptor | 3 | Dev2 |
| DeceasedPartyHandler: Phase 1 (atomic); Phase 2 stub; joint account path | 3 | Dev3 |
| CommunicationSuppressionService: stub (DECEASED_MANDATORY only) | 2 | Dev2 |
| CI pipeline: compile, unit test, lint gates enforced on every PR | 2 | DevOps1 |
| Docker image build + push to registry in CI | 2 | DevOps1 |
| Keycloak realm export checked in; all test users verified end-to-end | 2 | DevOps2 |
| QA: test strategy finalised; Playwright scaffold + first smoke test (login flow) | 2 | QA |

---

### Sprint 2: 11 May – 15 May — Vulnerability, Fraud & Specialist Routing

| Story | Pts | Owner |
|---|---|---|
| V010–V012: VULNERABILITY_ASSESSMENT, VULNERABILITY_TYPE, FRAUD_FLAG | 2 | Dev3 |
| Vulnerability state machine (IDENTIFIED soft flag; ACTIVE specialist-only) | 5 | Dev1 |
| FRAUD_FLAG CRUD + propagation | 3 | Dev2 |
| Specialist queue routing | 5 | Dev3 |
| Vulnerability assessment screen (AGENT: flag indicator; SPECIALIST+: full) | 3 | Dev4 |
| SUPPRESSION_LOG: add VULNERABILITY_POLICY reason | 2 | Dev2 |
| AKS dev namespace + Helm chart deployed; `helm upgrade` in CI on main merges | 3 | DevOps1 |
| Container registry secrets and pull policies configured in AKS | 2 | DevOps2 |
| QA: Sprint 1 acceptance tests (customer search, RBAC guards, deceased flag) | 3 | QA |
| QA: vulnerability test cases written (access control matrix for Article 9 data) | 2 | QA |

---

### Sprint 3: 18 May – 22 May — Collection Workflow, Segmentation & Suppression

| Story | Pts | Owner |
|---|---|---|
| V020–V022: STRATEGY, STRATEGY_TEST (incl. min_sample_size, min_duration_days), STRATEGY_ASSIGNMENT | 2 | Dev3 |
| V023: v_account_suppression_status view; ALTER DISPUTE drop communications_suppressed | 2 | Dev2 |
| CommunicationSuppressionService: full implementation (all reasons, isPermitted() logic, exhaustive switch, DUAL_USE audit) | 5 | Dev1 |
| COLLECTION_PROCESS.bpmn (statute-barred gate, segmentation, vulnerability, champion/challenger, signal catch) | 8 | Dev1 |
| segmentation.dmn (PRIORITY, 7 rules) | 3 | Dev1 |
| champion_challenger_assignment.dmn (PRIORITY, vulnerability_override) | 3 | Dev2 |
| StatuteBarredCalculationJob + StatuteBarredEvaluator (feature flag, @PostConstruct, nightly, event hook, signal) | 5 | Dev3 |
| SYSTEM_CONFIG seed entries | 1 | Dev2 |
| Pre-configured STRATEGY templates (3 records) | 2 | Dev6 |
| Case management screen (ACKNOWLEDGEMENT note type) | 3 | Dev5 |
| FlowableOperationContext (ThreadLocal) | 2 | Dev1 |
| Integration test harness: Spring Boot test slice + test DB with Flyway clean/migrate | 3 | QA |
| QA: Sprint 2 acceptance tests (vulnerability access control, fraud flag propagation) | 3 | QA |
| AKS: liveness/readiness probes, resource limits, HPA skeleton | 2 | DevOps1 |
| Postgres backup policy + connection pool tuning in AKS | 2 | DevOps2 |

---

### Sprint 4: 26 May – 29 May — Repayment Plans, Affordability & Joint Debt

> **Bank holiday: 25 May (Spring Bank Holiday).** Sprint 4 is a 4-day sprint. Plan scope accordingly.

> **HARD GATE:** Joint debt split requires DDE-OQ-04 confirmed. Decision authority: DWP Debt Domain Expert (via Delivery Lead). Hard deadline: **22 May (end of Sprint 3)**. Delivery Lead must confirm escalation path in writing by **30 April**.
>
> **If DDE-OQ-04 is not confirmed by 22 May:** Demo E executes the contingency narrative (household view, DMN admin walkthrough, "Phase 2 — Pending Legal Confirmation" banner); joint debt split execution is deferred to post-award. This is not a failure state — it is an explicit, pre-planned scope boundary.

| Story | Pts | Owner |
|---|---|---|
| V030–V034: INCOME_EXPENDITURE, AFFORDABILITY_FORMULA_VERSION, REPAYMENT_ARRANGEMENT, ARRANGEMENT_BREACH, FORBEARANCE_RECORD | 2 | Dev3 |
| I&E form + persistence; staleness warning; raw_data Restricted | 5 | Dev1 |
| plan_suitability.dmn (PRIORITY) | 3 | Dev1 |
| forbearance_suitability.dmn (PRIORITY) | 3 | Dev2 |
| REPAYMENT_ARRANGEMENT CRUD + status machine; limitation clock reset | 5 | Dev3 |
| Joint debt split: DMN + JointDebtSplitService + UI ratio override | 5 | Dev6 |
| ARRANGEMENT_MONITORING_PROCESS.bpmn (all process variables, DB reads, ${nextDueDateISO} timer) | 5 | Dev1 |
| Repayment plan screen (React) | 3 | Dev4 |
| Joint debt / household screen (React) | 3 | Dev5 |
| QA: Sprint 3 acceptance tests (collection workflow, statute-barred gate, suppression service) | 3 | QA |
| AKS: monitoring stack (Prometheus scrape config, basic Grafana dashboard) | 3 | DevOps1 |
| Network policy: deny all ingress except frontend→backend, backend→postgres | 2 | DevOps2 |

---

### Sprint 5: 1 June – 5 June — Write-Off, Breathing Space & Dispute

| Story | Pts | Owner |
|---|---|---|
| V040–V042: WRITE_OFF (CHECK constraint), LEGAL_HOLD (updated types), DISPUTE | 2 | Dev3 |
| WRITE_OFF_APPROVAL_PROCESS.bpmn (4-tier; CreateTaskListener with empty-group check) | 8 | Dev1 |
| BREATHING_SPACE_PROCESS.bpmn (dual path; cross-process variable setting; REQUIRES_NEW; MANUAL_REVIEW_REQUIRED fallback) | 8 | Dev1 |
| DISPUTE_HANDLING_PROCESS.bpmn (Activate/LiftDisputeSuppressionDelegate) | 5 | Dev2 |
| DeceasedPartyHandler Phase 2 (full Flowable suspension; joint account path) | 3 | Dev3 |
| FlowableHistoryEventProcessor (6 event types; thread-local flag) | 3 | Dev2 |
| Write-off screens (React) | 4 | Dev4 |
| Breathing space screen: type selection (React) | 3 | Dev5 |
| Dispute raise screen (React) | 3 | Dev5 |
| QA: Sprint 4 acceptance tests (repayment arrangement lifecycle, joint debt split, affordability DMN) | 3 | QA |
| QA: WCAG AA check on Sprint 1–4 screens (automated axe-core scan) | 2 | QA |
| Security scanning in CI: OWASP dependency-check, Trivy image scan | 2 | DevOps1 |
| Secrets management: K8s Secrets for DB credentials and Keycloak client secrets | 2 | DevOps2 |

---

### Sprint 6: 8 June – 12 June — Payments & Allocations

| Story | Pts | Owner |
|---|---|---|
| V050–V052: PAYMENT, PAYMENT_ALLOCATION, SUSPENSE_PAYMENT | 2 | Dev3 |
| Payment allocation engine (waterfall; limitation clock reset on CLEARED debtor payments) | 5 | Dev1 |
| SUSPENSE_PAYMENT handling | 3 | Dev2 |
| Payment Gateway stub + control endpoint | 3 | Dev6 |
| DEA stub + control endpoint | 2 | Dev6 |
| Payment history screen (React) | 3 | Dev4 |
| Direct Debit mandate setup | 3 | Dev5 |
| MIGRATION_BATCH + MIGRATION_RECORD tables (V053–V054) + stub MigrationService | 3 | Dev3 |
| QA: Sprint 5 acceptance tests (write-off 4-tier approval, breathing space dual path, dispute suppression) | 3 | QA |
| QA: Payment allocation edge cases (reversed payment, limitation clock) | 2 | QA |
| Load test baseline: k6 script for 100 concurrent users against AKS dev | 3 | DevOps1 |
| TLS termination at AKS ingress; certificate management | 2 | DevOps2 |

---

### Sprint 7: 15 June – 19 June — Communications & Channel Selection

| Story | Pts | Owner |
|---|---|---|
| V060–V062: COMMUNICATION (category, suppression_reason, DISCARDED), COMMUNICATION_TEMPLATE (category), COMMUNICATION_THREAD | 2 | Dev3 |
| channel_selection.dmn | 3 | Dev1 |
| GOV.UK Notify stub + control endpoint; isPermitted() before every dispatch | 3 | Dev6 |
| Communication suppression: queued comm disposition (BR-COMMS-001) | 4 | Dev2 |
| Communication template seeding (all templates classified) | 2 | Dev2 |
| Communication thread view (React) | 4 | Dev4 |
| PROMISE_TO_PAY CRUD + process signal | 3 | Dev3 |
| QA: Sprint 6 acceptance tests (payment allocation, suspense, DEA stub, DD mandate) | 3 | QA |
| QA: Full regression suite run against AKS dev; defect triage | 3 | QA |
| Monitoring: alerts for pod restarts, DB connection pool saturation, 5xx rate | 2 | DevOps1 |
| Log aggregation: JSON stdout → centralised log viewer in AKS dev | 2 | DevOps1 |
| DCA stub + RTBS stub + Bureau stub + control endpoints | 4 | Dev6 |

---

### Sprint 8: 22 June – 26 June — Work Allocation & Supervisor Tools

| Story | Pts | Owner |
|---|---|---|
| WORK_QUEUE management | 5 | Dev1 |
| Flowable user task → queue/agent assignment | 5 | Dev1 |
| Dispute queue routing; suppression check before outbound comms | 3 | Dev3 |
| Supervisor task override (app API + CASE_NOTE) | 4 | Dev2 |
| Queue monitoring dashboard (React) | 5 | Dev4 |
| DCA placement (disclosure notice prerequisite; notice period timer; recall acknowledgement) | 4 | Dev6 |
| BROADCAST_MESSAGE + info banner (React) | 2 | Dev5 |
| QA: Sprint 7 acceptance tests (communications suppression, channel selection DMN, template classification) | 3 | QA |
| QA: Load test for SCA02 (4,000 concurrent users) using k6 against AKS dev | 4 | QA + DevOps1 |
| QA: WCAG AA audit on all screens built to date | 3 | QA |
| Demo infrastructure: dedicated demo AKS namespace; idempotent seed script in Helm | 3 | DevOps1 |
| Demo deployment runbook drafted and dry-run | 2 | DevOps2 |

---

### Sprint 9: 29 June – 3 July — Demo Hardening

| Story | Pts | Owner |
|---|---|---|
| Demo seed data: 8 accounts, all 6 scenarios; cause_of_action_date set safely; idempotent | 5 | Dev2 |
| Champion/Challenger analytics dashboard: three panels; promote winner gate | 5 | Dev6 |
| DECISION_TRACE viewer | 3 | Dev4 |
| Compliance audit screen (AUDIT_EVENT search, SUPPRESSION_LOG history, export) | 5 | Dev5 |
| Consistency check endpoint: GET /admin/consistency/breathing-space | 2 | Dev3 |
| End-to-end run-through of all 6 demo scenarios; critical bug fixes | 5 | Dev1 + Dev4 |
| Simplified process diagrams for demo presentation | 1 | Dev1 |
| QA: Sprint 8 acceptance tests (work allocation, DCA placement, supervisor override) | 3 | QA |
| QA: Full E2E automated suite run against demo namespace; sign-off report | 4 | QA |
| QA: Final WCAG AA accessibility sign-off | 2 | QA |
| Demo dry-run: full 6-scenario walkthrough on demo namespace (4 July target) | 3 | All |
| Demo namespace locked: no deployments after dry-run passes | 1 | DevOps1 |

---

## 12. Demo Scenario Design (8 Jul 2026 — early July target)

> Demo dry-run: 4 July. Demo namespace locked after dry-run passes. Target demo date: 8 July 2026.

### Demo A: Standard Collection End-to-End

1. Search "John Smith" — £3,200 outstanding, 47 days delinquent
2. Statute-barred check passes; segmentation DMN → `EARLY_INTERVENTION`,
   `MEDIUM`
3. Vulnerability check passes; account tagged `CHAMPION`
4. I&E captured; affordability DMN → £85/month; limitation clock reset
5. Arrangement created; `${nextDueDateISO}` timer shown in Flowable UI
6. Fast-forward; mock payment received; arrangement progresses
7. `NON_COLLECTION` communication sent via Notify stub; EMAIL selected

### Demo B: Vulnerable Customer — Specialist Track

1. Load account with `ACTIVE` vulnerability — banner shown; AGENT sees
   flag indicator only
2. AGENT denied case access; SPECIALIST_AGENT logs in
3. Segmentation DMN: vulnerability → `SPECIALIST`, `CRITICAL`
4. Breathing space activated (STANDARD); intermediate timer catch event
   shown in Admin UI
5. ARRANGEMENT_MONITORING_PROCESS suspended; `arrangementSuspendedAt` set
6. Channel selection: vulnerability → `LETTER` only
7. `DEBT_COLLECTION` comms blocked; `NON_COLLECTION` permitted;
   suppression log visible in audit trail

### Demo C: Write-Off Multi-Tier Approval

1. Agent requests £4,500 write-off; `requested_by` recorded
2. DMN: AGENT limit exceeded → escalate
3. TEAM_LEADER task created; requestor excluded from candidate group;
   TL limit exceeded → escalate
4. OPS_MANAGER approves; COMPLIANCE notified (not as approver)
5. Full `AUDIT_EVENT` trail shown
6. `DECISION_TRACE` DMN inputs/outputs shown
7. Self-approval attempt: requestor cannot see approval task

### Demo D: Champion/Challenger Live A/B Test

1. STRATEGY_TEST admin screen — 30% split active
2. Ingest 3 non-vulnerable accounts; DMN assigns 2 CHAMPION, 1 CHALLENGER
3. Show vulnerable account always assigned CHAMPION
   (`vulnerability_override=true`)
4. CHALLENGER follows modified treatment path
5. Fast-forward; `STRATEGY_OUTCOME_METRIC` captured
6. Three-panel analytics dashboard shown
7. Promote winner: gate null for demo; new champion active

### Demo E: Household & Joint Debt Split

1. Create Household: two parties via JOINT_DEBT_LINK
2. Account screen: household panel, shared £6,400, benefit_status shown
3. Query: all debts for household; individual party debts
4. Block conditions checked (none active); `joint_debt_split.dmn` fires
5. Two child accounts at £3,200; 1p residual auto-written-off (SYSTEM)
6. Original CLOSED; JOINT_DEBT_LINK updated
7. TEAM_LEADER ratio override demonstrated
8. DECISION_TRACE and AUDIT_EVENT trail shown
9. DEA arrangement on child account demonstrated

> **Contingency (if DDE-OQ-04 unconfirmed by 22 May):** Show household view, query, and DMN design in Admin UI. Split screen shown with "Phase 2 — Pending Legal Confirmation" banner. Joint debt split execution is deferred to post-award. See HARD GATE in Sprint 4.

### Demo F: Dispute Management & Forbearance Appropriateness

1. Load account — disposable income £35/month
2. I&E captured; `forbearance_suitability.dmn` → `REDUCED_PAYMENT`
3. Agent selects `HARDSHIP` override; FORBEARANCE_RECORD stores both types
4. Customer raises PAYMENT_DISPUTE
5. `ActivateDisputeSuppressionDelegate` creates `DISPUTE_INTERNAL`
   suppression log entry
6. `dispute_handling.dmn` → STANDARD_EXCEPTION queue
7. `DEBT_COLLECTION` comms halted; `NON_COLLECTION` permitted
8. Agent resolves; `LiftDisputeSuppressionDelegate` closes suppression;
   remaining suppressions checked; AUDIT_EVENT trail complete

### Phase 2 Roadmap

| Item | Rationale |
|---|---|
| Dialler/IVR integration | DWP telephony estate dependency |
| Bureau/scorecard feeds | Real-time credit data |
| Customer self-service portal | DWP strategic dependency |
| Power BI integration | DWP strategic MI platform |
| DM6 → FDS data migration | ETL design scoped separately |
| DEA production API | DWP Deductions API access required |
| Estate administration workflow | Pending DDE-OQ-10 |
| Joint debt legal liability sign-off | Pending DDE-OQ-04 |

---

## 13. Risk Register

| ID | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R01 | Flowable Admin UI SSO incompatible with Keycloak 24 | Medium | High | Pre-build spike; fallback: basic auth |
| R02 | Flowable timer duplicate execution in multi-pod K8s | Medium | High | Async executor exclusive jobs; test in pre-build |
| R03 | Sprint velocity insufficient | Low | High | 6-7 devs + DevOps + QA; strict MoSCoW; defer post-award; bank holidays in Sprints 1 and 4 absorbed in plan |
| R04 | flowable schema conflicts with app migrations | Low | Medium | Separate Flyway locations |
| R05 | WCAG AA failures found late | Medium | High | GOV.UK DS from Sprint 1; progressive testing |
| R06 | Demo seed data fails on demo day | High | Critical | Idempotent seed; dry run 4 July; cause_of_action_date safe |
| R07 | DMN error in live demo | Medium | High | OPS_MANAGER can edit live in Admin UI |
| R08 | Spring Boot 3 / Flowable 6.8 compatibility | Low | High | Pre-build spike; verify timer pattern |
| R09 | SCA02: 4,000 concurrent users | Medium | Medium | HikariCP, async executor, K8s HPA |
| R10 | Analytics too slow for demo | Low | Medium | Pre-aggregate; materialised view if needed |
| R11 | Joint debt split not completed before Demo 3 | High | Critical | Sprint 4 dedicated; DDE-OQ-04 hard gate; contingency defined |
| R12 | DEA stub insufficient | Medium | High | Brief evaluators; Phase 2 production API |
| R13 | Sprint capacity exceeded | High | High | Dispute handling can defer to Sprint 8 |
| R14 | Mental health crisis timer fires incorrectly | High | Critical | Intermediate timer catch event; pre-build verification |
| R15 | Statute-barred debt pursued | Medium | Critical | Feature flag default false; segmentation gate; safe seed data |
| R16 | Write-off self-approval — MPM violation | Medium | High | Three-layer defence: TaskListener + service + DB constraint |
| R17 | DCA placement without disclosure notice | Medium | Critical | disclosure_notice_id non-nullable; configurable timer |
| R18 | Deceased party receives debt collection communication | High | Critical | DECEASED_MANDATORY suppression; Phase 1 atomic; Sprint 1 gate |
| R19 | Vulnerable customer assigned CHALLENGER | High | High | DMN priority rule 1; excluded from A/B analytics |
| R20 | Joint debt split without legal confirmation | High | Critical | DDE-OQ-04 hard gate; contingency defined; Delivery Lead to escalate |
| R21 | Arrangement breach fires during breathing space | High | High | DB read at execution time; skipBreachOnResume flag |
| R22 | Flowable Admin UI bypasses regulatory controls | Medium | High | FLOWABLE_ADMIN role restricted; HistoryEventProcessor audits all operations |
| R23 | Arrangement end date extension rolled back on retry | Medium | High | REQUIRES_NEW propagation |
| R24 | arrangementSuspendedAt not set on correct process instance | High | High | Cross-process runtimeService.setVariable() before suspension |
| R25 | Migration numbering conflict between developers | High | Medium | Board-claim convention (no odd/even split — 6+ devs need explicit reservation); claim on sprint board before creating file |

---

## 14. Requirements Traceability

| Requirement ID | Description | Satisfied By |
|---|---|---|
| DW.28 | Configurable collection workflows | Flowable BPMN + Admin UI |
| DW.30 | Workflow visualisation | Flowable Admin UI (OPS_MANAGER read-only) |
| DW.38 | Runtime DMN rule editing | Flowable Admin UI — OPS_MANAGER; no redeploy |
| DW.57 | Process decision audit trail | Flowable history + DECISION_TRACE + HistoryEventProcessor |
| DW.84 / DIC.27 | Statute-barred debt handling | is_statute_barred + StatuteBarredCalculationJob + segmentation DMN |
| DW.11 / Demo 2 crit. 7 | Creating and amending strategies | Pre-configured STRATEGY templates; amend via Admin UI |
| RPF.8 | Affordability assessment required | INCOME_EXPENDITURE + plan_suitability.dmn; staleness warning |
| RPF.15 / Demo 3 crit. 16 | Most appropriate forbearance | forbearance_suitability.dmn + FORBEARANCE_RECORD |
| RPF.20 | Breathing space | BREATHING_SPACE_PROCESS (dual path) + SUPPRESSION_LOG |
| RPF.21 | 60-day freeze timer | Intermediate timer catch event PT1440H (STANDARD only) |
| UAAF.2 | Named RBAC roles | Keycloak realm roles → @PreAuthorize |
| UAAF.12 | Screen-level RBAC | React route guards + API security |
| UAAF.15 | Treatment path restrictions | Vulnerability → specialist routing |
| UAAF.21 | Write-off limits configurable | write_off_limit_check.dmn + SYSTEM_CONFIG |
| UAAF.25 | Override capability | TEAM_LEADER+ reassign via application API |
| WAM.24 | Delegated authority thresholds | WRITE_OFF_APPROVAL_PROCESS (4-tier) |
| COM06 | Immutable audit trail | AUDIT_EVENT — append-only |
| COM07 | CRUD event log | Spring AOP + HistoryEventProcessor |
| COM12 | DCA pre-placement disclosure | DISCLOSURE_NOTICE prerequisite; non-nullable FK |
| INT01/INT02 | OAuth 2.0 / OIDC | Keycloak + Spring OAuth2 Resource Server |
| OPP01 | UK data residency | AKS UK South |
| ACC01–03 | WCAG AA | GOV.UK DS + Sprint 9 audit |
| P&C02 | 90th percentile ≤ 3s | HikariCP + async executor |
| SCA02 | 4,000 concurrent sessions | K8s HPA + connection pool |
| SEC01–05 | Security | HTTPS, JWT, Keycloak, Flyway schema |
| CC.1 / Demo 3 crit. 1–6 | Household — joint debt | JOINT_DEBT_LINK + joint_debt_split.dmn + JointDebtSplitService |
| CC.2 / Demo 3 crit. 7 | Benefit status | DEBT_ACCOUNT.benefit_status |
| CC.3 / Demo 3 crit. 8 | Vulnerability | VULNERABILITY_ASSESSMENT state machine |
| CC.4 / Demo 3 crit. 15–17 | Affordability + forbearance | INCOME_EXPENDITURE + forbearance_suitability.dmn |
| CC.5 / Demo 3 crit. 22 | DEA payment channel | DEDUCTION_FROM_BENEFIT + DEAAdapter stub |
| IEC.1 / Demo 2 crit. 14 | Dispute management | DISPUTE + DISPUTE_HANDLING_PROCESS + SUPPRESSION_LOG |
| RULING-003 | Write-off self-approval prohibition | TaskListener + WriteOffService + DB CHECK |
| RULING-006 | Deceased mandatory handling | UC-NEW-01 + two-phase atomicity |
| RULING-008 | DCA pre-placement disclosure | DISCLOSURE_NOTICE + configurable notice period |
| RULING-010 | Vulnerable CHALLENGER exclusion | DMN priority 1 + vulnerability_override |
| RULING-011 | Queued communication disposition | BR-COMMS-001 + DiscardQueuedDelegate + suppression_reason |
| RULING-012 | Limitation clock reset events | DEBT_STATUS_HISTORY.limitation_clock_reset + RULING-012 events |