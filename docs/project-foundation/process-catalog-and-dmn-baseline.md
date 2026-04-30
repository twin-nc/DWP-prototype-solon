# Process Catalog and DMN Baseline

**Scope:** I09 — imported from `docs/project-foundation/development-plan.md` §7 (Workflow & Decision Architecture).  
**Status:** Provisional appendix. This document is *not* an authority document. Consult `MASTER-DESIGN-DOCUMENT.md`, `ARCHITECTURE-BLUEPRINT.md`, and `decisions/ADR-001–ADR-010` for authority.  
**Last verified against source:** 2026-04-24

---

## Open Items

These items must be resolved before the process definitions or DMN tables below are used as implementation authority.

| Ref | Item | Blocking | Owner |
|---|---|---|---|
| OI-I09-01 | Process deployment role model — `PROCESS_DESIGNER`, `SRO` role names and the two-person production deployment gate (PROCESS_DESIGNER proposes, COMPLIANCE approves) are provisional. Final role names and capability split depend on Dev A governance stream (I01 RBAC matrix + I02 Flowable Admin UI access ADR). Do not implement process-deployment access controls until I02 is merged. | Dev A I01 + I02 | Dev A |
| OI-I09-02 | `write_off_limit_check.dmn` thresholds (£500 / £2,000 / £10,000 / SRO) are explicitly provisional pending DDE-OQ-02 client response. Do not implement as hard limits. | DDE-OQ-02 | TBC |
| OI-I09-03 | `champion_challenger_assignment.dmn` uses `account_hash_bucket` (deterministic hash of account ID, 0–99) as the split input per ADR-010. Verify hash distribution is uniform before enabling live tests. | ADR-010 | TBC |
| OI-I09-04 | `ARRANGEMENT_MONITORING_PROCESS` uses `@Transactional(propagation = REQUIRES_NEW)` for `extendArrangementEndDate()`. This exception to the general transaction boundary rule (§3 below) requires dedicated integration test coverage before shipping. | — | TBC |

---

## 1. Delegate Naming Convention

Source: `development-plan.md` §7.1

All Flowable service task delegates follow the `{Action}{Subject}Delegate` convention:

```
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

---

## 2. Process Variable Live State Rule

Source: `development-plan.md` §7.2 — aligns with ADR-009 (live-state DB reads in delegates).

Delegates must read the following fields from the database at execution time. These fields must **not** be read from Flowable process variables, which may be stale:

| Field | Table |
|---|---|
| `breathing_space_flag` | `DEBT_ACCOUNT` |
| `deceased_flag` | `DEBT_ACCOUNT` |
| `is_statute_barred` | `DEBT_ACCOUNT` |
| `benefit_status` | `DEBT_ACCOUNT` |
| `status` | `VULNERABILITY_ASSESSMENT` |

---

## 3. Transaction Boundary Rule

Source: `development-plan.md` §7.3 — aligns with ADR-003, ADR-006.

- Application database writes are always inside `@Transactional`.
- Flowable engine calls are always outside `@Transactional`. Methods calling Flowable services must not be annotated `@Transactional`.
- Flowable call failures are caught, logged as `MANUAL_REVIEW_REQUIRED`, and do not roll back committed application state.
- **Exception (see OI-I09-04):** `extendArrangementEndDate()` uses `@Transactional(propagation = REQUIRES_NEW)`.

---

## 4. Process Definitions

Source: `development-plan.md` §7.4

### 4.1 COLLECTION_PROCESS

```
Start Event
│
▼
CheckStatuteBarredDelegate (reads is_statute_barred from DB — see §2)
│
▼
Gateway: Statute barred?
├─ [Yes] User Task: Legal Review [TEAM_LEADER / OPS_MANAGER]
│        Signal catch: STATUTE_BARRED_CLEARED → re-route
└─ [No]  RunSegmentationDmnDelegate
         │
         ▼
         Vulnerability check (reads VULNERABILITY_ASSESSMENT.status from DB)
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
                             ├─ Sub-Process: ARRANGEMENT_MONITORING_PROCESS
                             ├─ Sub-Process: WRITE_OFF_APPROVAL_PROCESS
                             └─ Service Task: LEGAL_ACTION_PROCESS
│
▼
End Event
```

**Note:** The vulnerability branch forces CHAMPION and sets `vulnerability_override=true`. This implements RULING-010 (provisional, awaiting sign-off).

### 4.2 WRITE_OFF_APPROVAL_PROCESS

**Note:** Monetary thresholds are provisional pending DDE-OQ-02 (OI-I09-02). Role names at the top of the escalation chain (`SRO`) depend on I01/I02 governance (OI-I09-01).

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
                  └─ [No]  User Task: SRO Review [SRO]       ← OI-I09-01
                           (CreateTaskListener removes requestor;
                            empty group → MANUAL_REVIEW_REQUIRED)
                           │
                           ▼
                           Approve → Notify COMPLIANCE → End
```

**Self-approval prevention:** The `CreateTaskListener` removes the requestor from the candidate group at each tier. This is the implementation mechanism for RULING-003 (provisional, awaiting sign-off).

### 4.3 BREATHING_SPACE_PROCESS

**Note:** The `breathingSpaceType` variable controls the path. Both paths are governed by RULING-005 (provisional, awaiting sign-off).

```
Start Event (breathingSpaceType variable)
│
▼
Gateway: Type?
│
├─ [STANDARD] ─────────────────────────────────────────────────────────────────┐
│                                                                               │
│  ActivateBreathingSpaceSuppressionDelegate                                    │
│    (sets breathing_space_flag via CommunicationSuppressionService,            │
│     creates LEGAL_HOLD BREATHING_SPACE_STANDARD end_date=+60d,                │
│     creates SUPPRESSION_LOG BREATHING_SPACE_STATUTORY,                        │
│     sets arrangementSuspendedAt on ARRANGEMENT_MONITORING_PROCESS             │
│       via runtimeService.setVariable() BEFORE suspending,                     │
│     suspends ARRANGEMENT_MONITORING_PROCESS instances)                        │
│  │                                                                            │
│  ▼                                                                            │
│  Intermediate Timer Catch Event: ${breathingSpaceEndDateISO}                  │
│  │                                                                            │
│  ▼                                                                            │
│  DiscardQueuedCollectionCommunicationsDelegate                                │
│  │                                                                            │
│  ▼                                                                            │
│  LiftBreathingSpaceSuppressionDelegate                                        │
│  │                                                                            │
│  ▼                                                                            │
│  ResumeArrangementMonitoringDelegate                                          │
│    (see OI-I09-04 — uses REQUIRES_NEW for extendArrangementEndDate;           │
│     MANUAL_REVIEW_REQUIRED if revised schedule notice suppressed)             │
│  │                                                                            │
│  ▼                                                                            │
│  User Task: Post-review [SPECIALIST_AGENT]                                    │
│  │                                                                            │
│  ▼                                                                            │
│  End Event                                                         ◄──────────┘
│
└─ [MENTAL_HEALTH_CRISIS] ──────────────────────────────────────────────────────┐
                                                                                 │
   ActivateBreathingSpaceSuppressionDelegate                                     │
     (LEGAL_HOLD BREATHING_SPACE_MENTAL_HEALTH_CRISIS end_date=NULL)             │
   │                                                                             │
   ▼                                                                             │
   User Task: Awaiting MH confirmation [SPECIALIST_AGENT]                        │
   │  Boundary Escalation Timer: ${mhcEscalationDays}d                           │
   │  └─ [Escalation] User Task: MH Crisis Review [OPS_MANAGER]                 │
   │                                                                             │
   ▼ [Confirmed]                                                                 │
   DiscardQueuedCollectionCommunicationsDelegate                                 │
   │                                                                             │
   ▼                                                                             │
   LiftBreathingSpaceSuppressionDelegate                                         │
   │                                                                             │
   ▼                                                                             │
   ResumeArrangementMonitoringDelegate                                           │
   │                                                                             │
   ▼                                                                             │
   End Event                                                          ◄──────────┘
```

### 4.4 ARRANGEMENT_MONITORING_PROCESS

**Process variables:**

| Variable | Type | Purpose |
|---|---|---|
| `arrangementMonitoringStep` | String | Current step: `AWAITING_TIMER` \| `CHECKING_PAYMENT` \| `BREACH_EVALUATION` \| `AGENT_CONTACT_TASK` |
| `nextDueDateISO` | String (ISO-8601) | Timer catch event date expression |
| `arrangementSuspendedAt` | String (ISO-8601) | Set by `ActivateBreathingSpaceSuppressionDelegate` before suspension |
| `skipBreachOnResume` | Boolean | Prevents false breach on moratorium-interrupted cycle |

```
Start Event
│  [arrangementMonitoringStep = AWAITING_TIMER]
▼
Intermediate Timer Catch Event: ${nextDueDateISO}
│  [arrangementMonitoringStep = CHECKING_PAYMENT]
▼
Check breathing_space_flag (reads from DB — see §2 and ADR-009)
│
▼
Gateway: Breathing space active?
├─ [Yes] Advance to next cycle; loop ─────────────────────────────────────► ↑
└─ [No]
   │
   ▼
   Gateway: skipBreachOnResume?
   ├─ [Yes] Reset flag; advance to next cycle ──────────────────────────► ↑
   └─ [No]
      │
      ▼
      Gateway: Payment received?
      ├─ [Yes]
      │  │
      │  ▼
      │  Gateway: Final payment?
      │  ├─ [Yes] Complete → End
      │  └─ [No]  Loop ───────────────────────────────────────────────► ↑
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

---

## 5. DMN Decision Tables

Source: `development-plan.md` §7.5

### 5.1 segmentation.dmn — Hit Policy: PRIORITY

| Pri | debt_type | balance | delinquency_days | vuln_flag | statute_barred | treatment_path | priority_level |
|---|---|---|---|---|---|---|---|
| 1 | ANY | any | any | any | true | `LEGAL_REVIEW` | CRITICAL |
| 2 | ANY | any | any | true | false | `SPECIALIST` | CRITICAL |
| 3 | `BENEFIT_FRAUD` | any | any | false | false | `FRAUD_TRACK` | HIGH |
| 4 | `OVERPAYMENT` | ≥ 5000 | > 90 | false | false | `HIGH_VALUE_ENFORCEMENT` | HIGH |
| 5 | ANY | any | > 180 | false | false | `LEGAL_CONSIDERATION` | HIGH |
| 6 | `OVERPAYMENT` | any | ≤ 30 | false | false | `EARLY_INTERVENTION` | MEDIUM |
| 7 | ANY | any | any | false | false | `STANDARD` | LOW |

### 5.2 write_off_limit_check.dmn — Hit Policy: UNIQUE

> **All thresholds provisional pending DDE-OQ-02 (OI-I09-02). Do not implement as hard limits.**

| requestor_role | write_off_amount | approved | escalate_to |
|---|---|---|---|
| `AGENT` | ≤ £500 | true | — |
| `AGENT` | > £500 | false | `TEAM_LEADER` |
| `TEAM_LEADER` | ≤ £2,000 | true | — |
| `TEAM_LEADER` | > £2,000 | false | `OPS_MANAGER` |
| `OPS_MANAGER` | ≤ £10,000 | true | — |
| `OPS_MANAGER` | > £10,000 | false | `SRO` |

### 5.3 channel_selection.dmn

| consent_email | consent_sms | vuln_flag | preferred_channel | fallback |
|---|---|---|---|---|
| any | any | true | `LETTER` | — |
| true | any | false | `EMAIL` | `SMS` |
| false | true | false | `SMS` | `LETTER` |
| false | false | false | `LETTER` | — |

### 5.4 plan_suitability.dmn — Hit Policy: PRIORITY

| Pri | disposable_income | outstanding_balance | plan_type | min_instalment |
|---|---|---|---|---|
| 1 | any | > £10,000 | `LEGAL_CONSIDERATION` | — |
| 2 | < £50 | any | `REDUCED_PLAN` | £1 |
| 3 | £50–£200 | ≤ £5,000 | `STANDARD_INSTALMENT` | disposable × 0.5 |
| 4 | > £200 | any | `ACCELERATED_PLAN` | disposable × 0.7 |

### 5.5 champion_challenger_assignment.dmn — Hit Policy: PRIORITY

`account_hash_bucket` = `abs(accountId.hashCode()) % 100` — deterministic per ADR-010.

| Pri | active_test | vuln_flag | account_hash_bucket (0–99) | split_pct | variant | vuln_override |
|---|---|---|---|---|---|---|
| 1 | any | true | any | any | `CHAMPION` | true |
| 2 | true | false | < split_pct | any | `CHALLENGER` | false |
| 3 | true | false | ≥ split_pct | any | `CHAMPION` | false |
| 4 | false | false | any | any | `CHAMPION` | false |

### 5.6 joint_debt_split.dmn

| split_trigger | balance_pence mod 2 | split_method | auto_write_off_residual | residual_max_pence |
|---|---|---|---|---|
| `MANUAL` | 0 | `EQUAL_SPLIT` | false | 0 |
| `MANUAL` | 1 | `EQUAL_SPLIT` | true | 1 |
| `AUTO_THRESHOLD` | any | `EQUAL_SPLIT` | true | 1 |

### 5.7 dispute_handling.dmn

| dispute_type | vuln_flag | queue_type | assigned_queue | action_required |
|---|---|---|---|---|
| `FRAUD_DISPUTE` | any | `SPECIALIST_EXCEPTION` | `SPECIALIST` | `FRAUD_INVESTIGATION` |
| `PAYMENT_DISPUTE` | true | `SPECIALIST_EXCEPTION` | `SPECIALIST` | `SPECIALIST_REVIEW` |
| `PAYMENT_DISPUTE` | false | `EXCEPTION` | `STANDARD_EXCEPTION` | `AGENT_REVIEW` |
| `PROCESS_DISPUTE` | any | `EXCEPTION` | `COMPLIANCE_QUEUE` | `COMPLIANCE_REVIEW` |
| `OTHER` | any | `EXCEPTION` | `STANDARD_EXCEPTION` | `AGENT_REVIEW` |

### 5.8 forbearance_suitability.dmn — Hit Policy: PRIORITY

| Pri | disposable_income | vuln_flag | bs_active | recommended_forbearance | review_days |
|---|---|---|---|---|---|
| 1 | any | any | true | [already active] | — |
| 2 | ≤ £0 | any | false | `BREATHING_SPACE` | 60 |
| 3 | > £0 and < £50 | true | false | `HARDSHIP` | 30 |
| 4 | > £0 and < £50 | false | false | `REDUCED_PAYMENT` | 30 |
| 5 | £50–£100 | any | false | `PAYMENT_HOLIDAY` | 14 |
| 6 | > £100 | any | false | `INTEREST_FREEZE` | 30 |

---

## 6. Flowable Spring Boot Configuration

Source: `development-plan.md` §7.6

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

---

## 7. Flowable Admin UI — Docker Compose Service

Source: `development-plan.md` §7.7

```yaml
flowable-ui:
  image: flowable/flowable-ui:6.8.0
  ports: ["8091:8080"]
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/dcms
    FLOWABLE_COMMON_APP_IDM-URL: http://keycloak:8080
  depends_on: [postgres, keycloak]
```

**Capabilities:**

| Capability | Tender ref |
|---|---|
| BPMN process instance monitoring — active instances, variables, token position | — |
| DMN decision table editing without redeployment | DW.38 |
| CMMN case instance management | — |
| User task reassignment and management | WAM.24 |

**Access role model (provisional — see OI-I09-01):**

| Role | Design (Modeler) | Deploy to Dev | Deploy to Prod | Monitor instances |
|---|---|---|---|---|
| `PROCESS_DESIGNER` | Yes | Yes | No | No |
| `COMPLIANCE` | No | No | Approve / sign-off | Yes |
| `OPS_MANAGER` | No | No | No | Yes (pause / retry) |
| `ADMIN` | No | No | No | Yes (pause / retry) |
| `TEAM_LEADER` / `AGENT` | No | No | No | No |

Production deployments require a two-person gate: `PROCESS_DESIGNER` proposes, `COMPLIANCE` approves. The export-to-git step is mandatory before promoting any process definition beyond dev.
