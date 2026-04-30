# Income & Expenditure (I&E) Engine — Domain Pack

**Domain:** Income & Expenditure Engine
**Package:** `domain/repaymentplan`
**Status:** Draft — gated on DDE-OQ-08 (staleness period) and DDE-OQ-09 (benefit-deduction I&E requirement)
**Date:** 2026-04-27
**Author:** Business Analyst Agent
**Domain rulings in scope:** RULING-007 (I&E Assessment — FCA Affordability Obligations)
**Requirement IDs covered:** IEC.1-IEC.11, RPF.8, RPF.11, RPF.18, RPF.19, RPF.25, DIC.9, DIC.10, DIC.11, DIC.21, DW.22, CC.22, UI.28

---

## 1. Purpose and Scope

### 1.1 What this domain pack covers

This pack defines the business rules, entity model, service interfaces, DMN table structures, Flowable integration approach, and acceptance criteria for the Income & Expenditure (I&E) Engine within DCMS. It covers:

- Capture of point-in-time customer income and expenditure records by agents, including inline NDI calculation and SLA deviation highlighting.
- I&E history — an immutable, time-stamped log of all assessments per customer.
- Affordability assessments — the derived calculation of net disposable income (NDI) from a captured I&E, application of standard living allowances, and generation of arrangement option ranges.
- I&E trigger records — what event caused this I&E to be requested (trigger type, source process instance, source task).
- Review scheduling — the Flowable-driven mechanism for ensuring I&E assessments are refreshed at the required interval or on specific events.
- Configurable form definition — income categories, expenditure categories, and standard living allowances, all updateable without code changes via Tier 1 Foundations admin.
- CRA validation — the optional call to bureau/CRA data to cross-reference declared income, surfacing discrepancies for agent review.
- Forbearance handling — where NDI is zero or negative, the engine routes to a forbearance record with a mandatory review date, per RULING-007.

### 1.2 What this domain pack explicitly excludes

- Customer-facing portal/app UI — owned by the DWP strategic self-service component unless a future scope change assigns it to DCMS. Inbound self-service I&E submissions from that component are in scope through `domain/integration` and are normalised into this I&E engine (IEC.2, IEC.8, CC.22).
- Auto-population of I&E fields from open banking feeds — noted as `Should have` (IEC.9); excluded from this pack.
- Integration with third-party I&E tools (e.g., Standard Financial Statement tooling) — deferred; noted as `Could have` (IEC.8).
- Direct debit setup and payment scheduling — owned by `domain/repaymentplan` separately; this pack covers only I&E inputs to arrangement options.
- Repayment plan lifecycle, breach handling, and arrangement state machine — separate concern within `domain/repaymentplan`.
- CRA feed ingestion and bureau data freshness — owned by `domain/integration` and `domain/analytics`; this pack defines only the validation port called by the I&E engine.
- Scoring and scorecard outputs — owned by `domain/strategy`.

### 1.3 Package ownership

All I&E services, entities, and repository interfaces live within `domain/repaymentplan`. The CRA validation call is an output port defined in `domain/integration` (anti-corruption layer). No Flowable imports enter `domain/repaymentplan`; the domain interacts with the process engine only through `shared/process/port` interfaces.

---

## 2. Entity Model

All tables are owned by `domain/repaymentplan`. All DDL is Flyway-compatible (PostgreSQL 16). Primary keys use `UUID` generated at application layer. Timestamps are `TIMESTAMPTZ`. All financial amounts are `NUMERIC(14,2)`. Enum columns use `VARCHAR(64)` with application-layer enum validation to allow future extension without DDL changes.

### 2.1 `ie_record`

The point-in-time I&E capture record for a customer. Immutable once `status = COMPLETED`. A customer may have many `ie_record` rows; only one may be in status `DRAFT` at any time per customer.

```sql
CREATE TABLE ie_record (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    customer_id             UUID            NOT NULL,
    account_id              UUID,                           -- nullable: may be customer-level rather than account-level
    status                  VARCHAR(64)     NOT NULL,       -- DRAFT | COMPLETED | SUPERSEDED | ABANDONED
    channel                 VARCHAR(64)     NOT NULL,       -- AGENT_UI | TELEPHONY | SYSTEM | THIRD_PARTY_ADVISER | SELF_SERVICE
    trigger_id              UUID,                           -- FK to ie_trigger; nullable on manually initiated
    captured_by             VARCHAR(255)    NOT NULL,       -- Keycloak subject (username) or 'SYSTEM'
    capture_started_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    capture_completed_at    TIMESTAMPTZ,
    household_type          VARCHAR(64)     NOT NULL,       -- SINGLE | COUPLE | SINGLE_WITH_DEPENDANTS | COUPLE_WITH_DEPENDANTS | OTHER
    notes                   TEXT,
    cra_validation_status   VARCHAR(64),                    -- NOT_REQUESTED | PENDING | MATCHED | DISCREPANCY | FAILED
    cra_validated_at        TIMESTAMPTZ,
    sla_version_id          UUID,                           -- FK to ie_standard_living_allowance snapshot version used
    version                 BIGINT          NOT NULL DEFAULT 1,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_ie_record PRIMARY KEY (id),
    CONSTRAINT fk_ie_record_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT chk_ie_record_status CHECK (status IN ('DRAFT','COMPLETED','SUPERSEDED','ABANDONED')),
    CONSTRAINT chk_ie_record_channel CHECK (channel IN ('AGENT_UI','TELEPHONY','SYSTEM','THIRD_PARTY_ADVISER','SELF_SERVICE')),
    CONSTRAINT chk_ie_record_household_type CHECK (household_type IN ('SINGLE','COUPLE','SINGLE_WITH_DEPENDANTS','COUPLE_WITH_DEPENDANTS','OTHER')),
    CONSTRAINT chk_ie_record_cra_status CHECK (cra_validation_status IS NULL OR cra_validation_status IN ('NOT_REQUESTED','PENDING','MATCHED','DISCREPANCY','FAILED'))
);

CREATE INDEX idx_ie_record_customer_id ON ie_record (customer_id);
CREATE INDEX idx_ie_record_status ON ie_record (status);
CREATE INDEX idx_ie_record_capture_completed_at ON ie_record (capture_completed_at);
```

### 2.2 `ie_income_line`

Individual income source lines belonging to a parent `ie_record`. Immutable once parent record is COMPLETED.

```sql
CREATE TABLE ie_income_line (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    ie_record_id            UUID            NOT NULL,
    income_category_id      UUID            NOT NULL,       -- FK to ie_income_category
    declared_amount         NUMERIC(14,2)   NOT NULL,
    frequency               VARCHAR(32)     NOT NULL,       -- WEEKLY | FORTNIGHTLY | FOUR_WEEKLY | MONTHLY | ANNUAL
    monthly_equivalent      NUMERIC(14,2)   NOT NULL,       -- computed at capture time and stored
    is_verified             BOOLEAN         NOT NULL DEFAULT FALSE,
    notes                   TEXT,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_ie_income_line PRIMARY KEY (id),
    CONSTRAINT fk_ie_income_line_record FOREIGN KEY (ie_record_id) REFERENCES ie_record (id),
    CONSTRAINT fk_ie_income_line_category FOREIGN KEY (income_category_id) REFERENCES ie_income_category (id),
    CONSTRAINT chk_ie_income_frequency CHECK (frequency IN ('WEEKLY','FORTNIGHTLY','FOUR_WEEKLY','MONTHLY','ANNUAL')),
    CONSTRAINT chk_ie_income_declared_amount CHECK (declared_amount >= 0)
);

CREATE INDEX idx_ie_income_line_record ON ie_income_line (ie_record_id);
```

### 2.3 `ie_expenditure_line`

Individual expenditure lines belonging to a parent `ie_record`. Immutable once parent record is COMPLETED.

```sql
CREATE TABLE ie_expenditure_line (
    id                          UUID            NOT NULL DEFAULT gen_random_uuid(),
    ie_record_id                UUID            NOT NULL,
    expenditure_category_id     UUID            NOT NULL,   -- FK to ie_expenditure_category
    declared_amount             NUMERIC(14,2)   NOT NULL,
    frequency                   VARCHAR(32)     NOT NULL,   -- WEEKLY | FORTNIGHTLY | FOUR_WEEKLY | MONTHLY | ANNUAL
    monthly_equivalent          NUMERIC(14,2)   NOT NULL,   -- computed at capture time and stored
    sla_amount                  NUMERIC(14,2),              -- SLA reference amount for this category/household_type; stored for audit
    exceeds_sla                 BOOLEAN         NOT NULL DEFAULT FALSE,
    agent_override_reason       TEXT,                       -- required when declared_amount > sla_amount and agent confirms
    notes                       TEXT,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_ie_expenditure_line PRIMARY KEY (id),
    CONSTRAINT fk_ie_expenditure_line_record FOREIGN KEY (ie_record_id) REFERENCES ie_record (id),
    CONSTRAINT fk_ie_expenditure_line_category FOREIGN KEY (expenditure_category_id) REFERENCES ie_expenditure_category (id),
    CONSTRAINT chk_ie_expenditure_frequency CHECK (frequency IN ('WEEKLY','FORTNIGHTLY','FOUR_WEEKLY','MONTHLY','ANNUAL')),
    CONSTRAINT chk_ie_expenditure_declared_amount CHECK (declared_amount >= 0)
);

CREATE INDEX idx_ie_expenditure_line_record ON ie_expenditure_line (ie_record_id);
```

### 2.4 `ie_affordability_assessment`

The NDI calculation output and arrangement options derived from a completed `ie_record`. One-to-one with `ie_record` (a record produces at most one completed assessment; a superseded record's assessment is retained for history).

```sql
CREATE TABLE ie_affordability_assessment (
    id                          UUID            NOT NULL DEFAULT gen_random_uuid(),
    ie_record_id                UUID            NOT NULL UNIQUE,
    customer_id                 UUID            NOT NULL,
    account_id                  UUID,
    total_monthly_income        NUMERIC(14,2)   NOT NULL,
    total_monthly_expenditure   NUMERIC(14,2)   NOT NULL,
    net_disposable_income       NUMERIC(14,2)   NOT NULL,   -- may be zero or negative
    ndi_band                    VARCHAR(64)     NOT NULL,   -- NEGATIVE | ZERO | LOW | MEDIUM | HIGH (thresholds from DMN)
    arrangement_type            VARCHAR(64)     NOT NULL,   -- REPAYMENT | FORBEARANCE
    option_weekly               NUMERIC(14,2),              -- null when arrangement_type = FORBEARANCE
    option_fortnightly          NUMERIC(14,2),
    option_monthly              NUMERIC(14,2),
    forbearance_review_date     DATE,                       -- required when arrangement_type = FORBEARANCE (RULING-007 §4)
    sla_version_id              UUID            NOT NULL,   -- snapshot of SLA table version used
    dmn_decision_key            VARCHAR(255),               -- Flowable DMN decision key used for traceability
    calculated_at               TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    calculated_by               VARCHAR(255)    NOT NULL,   -- Keycloak subject or 'SYSTEM'
    CONSTRAINT pk_ie_affordability_assessment PRIMARY KEY (id),
    CONSTRAINT fk_ie_assessment_record FOREIGN KEY (ie_record_id) REFERENCES ie_record (id),
    CONSTRAINT fk_ie_assessment_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT chk_ie_assessment_arrangement_type CHECK (arrangement_type IN ('REPAYMENT','FORBEARANCE')),
    CONSTRAINT chk_ie_assessment_ndi_band CHECK (ndi_band IN ('NEGATIVE','ZERO','LOW','MEDIUM','HIGH')),
    CONSTRAINT chk_ie_forbearance_review CHECK (
        arrangement_type != 'FORBEARANCE' OR forbearance_review_date IS NOT NULL
    )
);

CREATE INDEX idx_ie_affordability_customer ON ie_affordability_assessment (customer_id);
CREATE INDEX idx_ie_affordability_calculated_at ON ie_affordability_assessment (calculated_at);
```

### 2.5 `ie_review_schedule`

One row per customer. Records the next I&E review due date and the scheduling basis. Upserted on every I&E completion and on arrangement breach.

```sql
CREATE TABLE ie_review_schedule (
    id                          UUID            NOT NULL DEFAULT gen_random_uuid(),
    customer_id                 UUID            NOT NULL UNIQUE,
    next_review_due_date        DATE            NOT NULL,
    review_interval_months      INTEGER         NOT NULL,   -- from ie-review-frequency.dmn output
    scheduling_basis            VARCHAR(64)     NOT NULL,   -- PERIODIC | POST_BREACH | VULNERABILITY_CHANGE | MANUAL_OVERRIDE
    source_ie_record_id         UUID,                       -- the IE record that drove this schedule entry
    flowable_process_instance_id VARCHAR(255),              -- Flowable process instance managing the timer
    flowable_timer_activity_id  VARCHAR(255),               -- activity ID of the timer event within that process
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_ie_review_schedule PRIMARY KEY (id),
    CONSTRAINT fk_ie_review_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT chk_ie_review_basis CHECK (scheduling_basis IN ('PERIODIC','POST_BREACH','VULNERABILITY_CHANGE','MANUAL_OVERRIDE')),
    CONSTRAINT chk_ie_review_interval CHECK (review_interval_months > 0)
);
```

### 2.6 `ie_trigger`

Records what caused this I&E to be requested. One trigger row is created when a new I&E is initiated; it is then linked to the resulting `ie_record` via `ie_record.trigger_id`.

```sql
CREATE TABLE ie_trigger (
    id                          UUID            NOT NULL DEFAULT gen_random_uuid(),
    trigger_type                VARCHAR(64)     NOT NULL,
    -- ARRANGEMENT_OFFER | ARRANGEMENT_BREACH | PERIODIC_REVIEW | AGENT_INITIATED | SELF_SERVICE_SUBMISSION | VULNERABILITY_FLAG_CHANGE
    customer_id                 UUID            NOT NULL,
    account_id                  UUID,
    source_process_instance_id  VARCHAR(255),               -- Flowable process instance that raised the trigger
    source_task_id              VARCHAR(255),               -- Flowable task ID that raised the trigger (if applicable)
    requested_by                VARCHAR(255)    NOT NULL,   -- Keycloak subject or 'SYSTEM'
    requested_at                TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_ie_trigger PRIMARY KEY (id),
    CONSTRAINT fk_ie_trigger_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT chk_ie_trigger_type CHECK (trigger_type IN (
        'ARRANGEMENT_OFFER',
        'ARRANGEMENT_BREACH',
        'PERIODIC_REVIEW',
        'AGENT_INITIATED',
        'SELF_SERVICE_SUBMISSION',
        'VULNERABILITY_FLAG_CHANGE'
    ))
);

CREATE INDEX idx_ie_trigger_customer ON ie_trigger (customer_id);
CREATE INDEX idx_ie_trigger_type ON ie_trigger (trigger_type);
CREATE INDEX idx_ie_trigger_requested_at ON ie_trigger (requested_at);
```

---

## 3. Configuration Entities (Tier 1 Foundations)

These tables are owned and versioned via the Tier 1 Foundations Configurator in the `/admin` React UI. They are read-only at runtime by the I&E engine. Changes to these tables are admin-only actions requiring the `COMPLIANCE` or `ADMIN` role. Effective-dated change sets are managed via policy bundles (ADR-009).

### 3.1 `ie_income_category`

```sql
CREATE TABLE ie_income_category (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    code                VARCHAR(64)     NOT NULL UNIQUE,    -- e.g. EMPLOYMENT, UNIVERSAL_CREDIT, CHILD_BENEFIT
    display_name        VARCHAR(255)    NOT NULL,
    display_order       INTEGER         NOT NULL,
    is_benefit_income   BOOLEAN         NOT NULL DEFAULT FALSE,  -- flags DWP-origin income sources
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    effective_from      DATE            NOT NULL,
    effective_to        DATE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_ie_income_category PRIMARY KEY (id)
);
```

`is_benefit_income = TRUE` drives DDE-OQ-09 logic: if all income is benefit income and DWP policy ultimately exempts such cases from I&E (pending sign-off), the trigger engine can filter accordingly.

### 3.2 `ie_expenditure_category`

```sql
CREATE TABLE ie_expenditure_category (
    id                          UUID            NOT NULL DEFAULT gen_random_uuid(),
    code                        VARCHAR(64)     NOT NULL UNIQUE,  -- e.g. RENT, COUNCIL_TAX, FOOD, UTILITIES
    display_name                VARCHAR(255)    NOT NULL,
    display_order               INTEGER         NOT NULL,
    is_priority_expenditure     BOOLEAN         NOT NULL DEFAULT FALSE,
    -- TRUE for rent, council tax, utilities: priority creditors; highlighted in UI
    is_active                   BOOLEAN         NOT NULL DEFAULT TRUE,
    effective_from              DATE            NOT NULL,
    effective_to                DATE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_ie_expenditure_category PRIMARY KEY (id)
);
```

### 3.3 `ie_standard_living_allowance`

SLA amounts by household type and expenditure category. These are the DWP/FCA reference amounts. When a declared expenditure line exceeds the SLA for its category and household type, the agent must provide an override reason.

```sql
CREATE TABLE ie_standard_living_allowance (
    id                          UUID            NOT NULL DEFAULT gen_random_uuid(),
    expenditure_category_id     UUID            NOT NULL,
    household_type              VARCHAR(64)     NOT NULL,
    -- SINGLE | COUPLE | SINGLE_WITH_DEPENDANTS | COUPLE_WITH_DEPENDANTS | OTHER
    monthly_allowance_amount    NUMERIC(14,2)   NOT NULL,
    effective_from              DATE            NOT NULL,
    effective_to                DATE,
    policy_reference            VARCHAR(255),               -- e.g. "FCA CONC 7.3 / DWP SFS v2.1 2025"
    is_active                   BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_ie_standard_living_allowance PRIMARY KEY (id),
    CONSTRAINT fk_ie_sla_category FOREIGN KEY (expenditure_category_id) REFERENCES ie_expenditure_category (id),
    CONSTRAINT chk_ie_sla_household_type CHECK (household_type IN (
        'SINGLE','COUPLE','SINGLE_WITH_DEPENDANTS','COUPLE_WITH_DEPENDANTS','OTHER'
    )),
    CONSTRAINT chk_ie_sla_amount CHECK (monthly_allowance_amount >= 0)
);

CREATE INDEX idx_ie_sla_category_household ON ie_standard_living_allowance (expenditure_category_id, household_type);
CREATE INDEX idx_ie_sla_effective ON ie_standard_living_allowance (effective_from, effective_to);
```

---

## 4. DMN Tables (Tier 2 Rules)

Both DMN tables are owned by `PROCESS_DESIGNER` and approved by `COMPLIANCE` before deployment. They are versioned and deployed through the Flowable DMN engine via the ADR-008 Tier 2 workflow. They are accessible via the Rules section of the `/admin` UI (ADR-015).

### 4.1 `ie-review-frequency.dmn`

**Decision key:** `ie-review-frequency`
**Hit policy:** FIRST (first matching rule wins)
**Purpose:** Determines the I&E review interval (in months) given the customer's vulnerability flags, arrangement type, and account risk tier.

| Input: `vulnerabilityFlag` | Input: `arrangementType` | Input: `accountRiskTier` | Output: `reviewIntervalMonths` | Output: `reviewBasis` |
|---|---|---|---|---|
| `HIGH` | any | any | `6` | `VULNERABILITY_HIGH` |
| `MEDIUM` | any | any | `9` | `VULNERABILITY_MEDIUM` |
| any | `FORBEARANCE` | any | `3` | `FORBEARANCE_MANDATORY` |
| any | `REPAYMENT` | `HIGH` | `9` | `HIGH_RISK_ACCOUNT` |
| any | `REPAYMENT` | `MEDIUM` | `12` | `STANDARD` |
| any | `REPAYMENT` | `LOW` | `12` | `STANDARD` |
| any | any | any | `12` | `DEFAULT` |

Notes:
- `vulnerabilityFlag` corresponds to the severity level on the most recent active vulnerability record for the customer (`HIGH`, `MEDIUM`, `LOW`, `NONE`).
- `arrangementType` corresponds to `ie_affordability_assessment.arrangement_type`.
- `accountRiskTier` corresponds to the segmentation risk tier output from the strategy engine.
- The 12-month default row is subject to DDE-OQ-08 sign-off — DWP may mandate a shorter period.
- The forbearance 3-month row implements RULING-007 §4 (guardrail 3): a forbearance plan must not be open-ended.

### 4.2 `ie-arrangement-options.dmn`

**Decision key:** `ie-arrangement-options`
**Hit policy:** FIRST
**Purpose:** Given the NDI band produced by the affordability calculation, determines the weekly, fortnightly, and monthly arrangement option amounts to present to the agent, and the minimum permissible instalment.

| Input: `ndiMonthly` (range) | Output: `arrangementType` | Output: `weeklyOption` | Output: `fortnightlyOption` | Output: `monthlyOption` | Output: `minimumInstalment` |
|---|---|---|---|---|---|
| `<= 0` | `FORBEARANCE` | — | — | — | `0.00` |
| `> 0 and <= 25` | `REPAYMENT` | `5.00` | `10.00` | NDI * 1.0 | `5.00` |
| `> 25 and <= 75` | `REPAYMENT` | NDI / 4.33 (floor to 0.50) | NDI / 2.17 | NDI * 1.0 | `5.00` |
| `> 75 and <= 200` | `REPAYMENT` | NDI / 4.33 | NDI / 2.17 | NDI * 1.0 | `10.00` |
| `> 200` | `REPAYMENT` | NDI / 4.33 | NDI / 2.17 | NDI * 1.0 | `25.00` |

Notes:
- The `<= 0` row implements RULING-007 §4 and guardrail 2: if `disposable_income <= 0`, then `minimum_instalment = 0` and `arrangement_type = FORBEARANCE`. A `£5` floor must not fire before this rule.
- `NDI / 4.33` converts monthly NDI to weekly (52 weeks / 12 months = 4.333). Values are floored to the nearest `£0.50` at application layer to avoid presenting fractional pence amounts.
- Threshold bands for LOW / MEDIUM / HIGH are illustrative. DWP must confirm before this DMN is deployed to production. The configuration surface (Tier 2 admin) allows threshold updates without code deployment.

---

## 5. Service API

All interfaces live in `domain/repaymentplan`. No Flowable imports. Command dispatch to the process engine uses `shared/process/port.DelegateCommandBus` and `shared/process/port.ProcessEventPort`.

### 5.1 `IECaptureService`

```java
package domain.repaymentplan.service;

/**
 * Manages the lifecycle of IE records — creation, incremental update during
 * agent capture, completion, and retrieval.
 *
 * An IE record is immutable once status transitions to COMPLETED.
 * Only one DRAFT record may exist per customer at a time.
 */
public interface IECaptureService {

    /**
     * Initiates a new IE capture record for a customer. Creates an ie_trigger
     * row and an ie_record in DRAFT status.
     *
     * @param command contains customer_id, account_id (optional), trigger_type,
     *                household_type, channel, captured_by, and source process/task IDs
     * @return the created IERecord
     * @throws IEDraftAlreadyExistsException if a DRAFT record already exists for the customer
     */
    IERecord initiateCapture(InitiateIECaptureCommand command);

    /**
     * Adds or replaces income lines on a DRAFT IE record.
     * Computes monthly_equivalent at the time of persistence.
     *
     * @throws IERecordNotEditableException if the record is not in DRAFT status
     */
    IERecord updateIncomeLines(UUID ieRecordId, List<IEIncomeLineInput> lines);

    /**
     * Adds or replaces expenditure lines on a DRAFT IE record.
     * Computes monthly_equivalent and evaluates exceeds_sla against the current
     * active SLA for the household type and expenditure category.
     *
     * @throws IERecordNotEditableException if the record is not in DRAFT status
     * @throws SLAVersionNotFoundException if no active SLA exists for the household type
     */
    IERecord updateExpenditureLines(UUID ieRecordId, List<IEExpenditureLineInput> lines);

    /**
     * Completes the IE record: transitions status to COMPLETED, sets
     * capture_completed_at, and triggers AffordabilityCalculationService.
     *
     * Emits a ProcessEventPort event to notify the Flowable process instance
     * that the IE capture task is complete.
     *
     * @throws IERecordNotEditableException if the record is not in DRAFT status
     * @throws MissingSLAOverrideReasonException if any expenditure line exceeds SLA
     *         without an agent_override_reason
     */
    IERecord completeCapture(UUID ieRecordId, String completedBy);

    /**
     * Abandons a DRAFT IE record without completing it. Status transitions to ABANDONED.
     * A reason must be supplied for audit.
     */
    IERecord abandonCapture(UUID ieRecordId, String abandonedBy, String reason);

    /**
     * Retrieves the full IE record with all income and expenditure lines.
     */
    IERecord getIERecord(UUID ieRecordId);

    /**
     * Returns all IE records for a customer, ordered by capture_completed_at descending.
     * Includes DRAFT, COMPLETED, SUPERSEDED, and ABANDONED records.
     */
    List<IERecord> getIEHistory(UUID customerId);

    /**
     * Returns the most recent COMPLETED IE record for a customer, or empty if none exists.
     */
    Optional<IERecord> getMostRecentCompleted(UUID customerId);

    /**
     * Evaluates whether the most recent COMPLETED IE record is stale relative to the
     * configured staleness threshold and the last known circumstance change date.
     *
     * Returns a StalenessAssessment containing: isStale (boolean), ageInDays,
     * stalenessThresholdDays, and lastCircumstanceChangeDate.
     *
     * Implements RULING-007 guardrail 1: staleness check uses the shorter of
     * (assessment_date + threshold) and (last_circumstance_change_date).
     */
    StalenessAssessment assessStaleness(UUID customerId);
}
```

### 5.2 `AffordabilityCalculationService`

```java
package domain.repaymentplan.service;

/**
 * Computes net disposable income (NDI) from a completed IERecord,
 * applies standard living allowance rules, and produces an
 * AffordabilityAssessment with arrangement options.
 *
 * This service is called automatically on IE record completion.
 * It may also be called explicitly (e.g., for recalculation preview).
 *
 * NDI = total_monthly_income - total_monthly_expenditure (capped to SLA where applicable)
 * Arrangement options are derived from the ie-arrangement-options.dmn decision table.
 *
 * RULING-007 guardrail 2: if NDI <= 0, arrangement_type = FORBEARANCE
 * and minimum_instalment = 0. The DMN must contain this rule before any
 * minimum floor rule fires.
 */
public interface AffordabilityCalculationService {

    /**
     * Calculates and persists an AffordabilityAssessment for the given IE record.
     * Invokes the ie-arrangement-options.dmn Flowable decision via DelegateCommandBus.
     *
     * @param ieRecordId must reference a record in COMPLETED status
     * @return the persisted AffordabilityAssessment
     * @throws IERecordNotCompletedException if the record is not in COMPLETED status
     * @throws SLAVersionNotFoundException if the SLA version used during capture is no longer resolvable
     */
    AffordabilityAssessment calculate(UUID ieRecordId);

    /**
     * Returns a preview of arrangement options without persisting — for use
     * by the agent UI while the IE record is still in DRAFT status.
     * Uses the current income/expenditure lines as captured so far.
     */
    AffordabilityPreview preview(UUID draftIeRecordId);
}
```

### 5.3 `IEReviewSchedulerService`

```java
package domain.repaymentplan.service;

/**
 * Manages IE review schedules. An IEReviewSchedule record holds the next
 * review due date per customer. When the due date is reached, Flowable
 * fires a timer and the domain receives an IEReviewDueCommand via DelegateCommandBus,
 * which causes this service to create a human task work item.
 */
public interface IEReviewSchedulerService {

    /**
     * Creates or updates the IEReviewSchedule for a customer after a completed
     * IE capture. Invokes ie-review-frequency.dmn to determine the interval.
     * Sends a IE_REVIEW_DUE_DATE_SET message to the Flowable process instance
     * so the timer intermediate catch event is updated.
     *
     * @param customerId the customer for whom the schedule is being set
     * @param sourceIeRecordId the IE record that triggered this schedule update
     * @param vulnerabilityLevel the current highest vulnerability severity for the customer
     * @param arrangementType the arrangement type from the new affordability assessment
     * @param accountRiskTier the risk tier from the strategy engine
     */
    IEReviewSchedule scheduleNextReview(
        UUID customerId,
        UUID sourceIeRecordId,
        String vulnerabilityLevel,
        String arrangementType,
        String accountRiskTier
    );

    /**
     * Cancels the pending review schedule for a customer (e.g., on account closure,
     * write-off, or transfer to DCA). Sends CANCEL_IE_REVIEW_TIMER signal to Flowable.
     */
    void cancelReview(UUID customerId, String reason);

    /**
     * Called when Flowable fires the IE review timer. Creates a work item of type
     * IE_REVIEW via CreateWorkItemCommand dispatched through DelegateCommandBus.
     * The work item is assigned to the SPECIALIST_AGENT queue unless a vulnerability
     * flag is active, in which case the ESCALATIONS queue is used.
     */
    void handleReviewDue(UUID customerId, String flowableProcessInstanceId);
}
```

### 5.4 `CRAValidationPort` (output port — `domain/integration`)

```java
package domain.integration.port.output;

/**
 * Output port for CRA/bureau validation of declared income.
 * Implemented in domain/integration as part of the anti-corruption layer.
 * Called optionally by domain/repaymentplan during or after IE capture.
 *
 * CRA output does NOT override agent-captured IE without explicit agent confirmation.
 * Discrepancies are surfaced as flags on the ie_record (cra_validation_status = DISCREPANCY)
 * and must be reviewed by the agent before the IE record can be completed.
 */
public interface CRAValidationPort {

    /**
     * Submits the income lines from a DRAFT IE record to the configured bureau API
     * for cross-reference. Returns a CRAValidationResult containing:
     * - validationStatus: MATCHED | DISCREPANCY | FAILED
     * - discrepancyDetails: list of category + declared amount + bureau amount pairs
     * - validatedAt: timestamp from bureau response
     *
     * Asynchronous: the bureau API may not respond synchronously. The port
     * implementation must handle polling or callback and update ie_record
     * .cra_validation_status when the response arrives.
     *
     * @throws CRAServiceUnavailableException if the bureau API is unreachable after retry
     */
    CRAValidationResult validateIncome(UUID ieRecordId, List<IEIncomeLineInput> incomeLines);
}
```

---

## 6. Flowable Integration

### 6.1 Review scheduling mechanism

I&E reviews are scheduled as Flowable timer events, not as application-layer cron jobs. This is consistent with the master design document §7a and ADR-008 (process engine as infrastructure).

**Message-driven timer configuration:**

1. When `IEReviewSchedulerService.scheduleNextReview(...)` is called, it dispatches a `SetIEReviewTimerCommand` via `shared/process/port.DelegateCommandBus`.
2. A `JavaDelegate` implementation in `infrastructure/process` receives the command and sets (or resets) a Flowable intermediate timer catch event within the account's running Flowable process instance. The timer duration is set to the ISO 8601 date corresponding to `next_review_due_date`.
3. The timer activity ID is stored in `ie_review_schedule.flowable_timer_activity_id` for audit and for cancellation.

**Timer firing:**

1. When the timer fires, Flowable advances the process token and invokes the `IEReviewDueDelegate` JavaDelegate in `infrastructure/process`.
2. `IEReviewDueDelegate` reads the `customer_id` from the process variable scope and dispatches an `IEReviewDueCommand` via `DelegateCommandBus`.
3. `domain/repaymentplan` receives the command and calls `IEReviewSchedulerService.handleReviewDue(...)`.
4. `handleReviewDue(...)` dispatches a `CreateWorkItemCommand` via `DelegateCommandBus`. The work item is of type `IE_REVIEW` and is routed to the appropriate queue:

| Condition | Queue | Role |
|---|---|---|
| Active high-severity vulnerability flag | `ESCALATIONS_IE_REVIEW` | `ESCALATIONS_AGENT` |
| Forbearance arrangement active | `SPECIALIST_IE_REVIEW` | `SPECIALIST_AGENT` |
| Default | `STANDARD_IE_REVIEW` | `AGENT` |

**Cancellation:**

When `IEReviewSchedulerService.cancelReview(...)` is called, it dispatches a `CancelIEReviewTimerCommand`. The `CancelIEReviewTimerDelegate` in `infrastructure/process` uses the stored `flowable_process_instance_id` and `flowable_timer_activity_id` to locate and cancel the timer boundary event, preventing a stale review task from being created after account closure or DCA transfer.

### 6.2 Human task produced when review is due

The Flowable human task created by the `CreateWorkItemCommand` carries the following payload:

| Field | Value |
|---|---|
| `taskType` | `IE_REVIEW` |
| `customerId` | from process variable |
| `accountId` | from process variable (may be null for customer-level reviews) |
| `dueDate` | `next_review_due_date` from `ie_review_schedule` |
| `priority` | `HIGH` if vulnerability active, else `NORMAL` |
| `candidateGroup` | one of the queue names in the routing table above |
| `triggerType` | `PERIODIC_REVIEW` |

The agent completing the task must initiate a new IE capture (which creates a new `ie_trigger` of type `PERIODIC_REVIEW` and a new `ie_record`). Completing the IE capture automatically reschedules the next review.

---

## 7. Trigger Types

The following events cause the I&E engine to request a new I&E assessment. Each maps to a value in `ie_trigger.trigger_type`.

| Trigger Type | Enum value | Description | Gate condition |
|---|---|---|---|
| New repayment arrangement offer | `ARRANGEMENT_OFFER` | An agent is creating or offering a new repayment arrangement. The I&E staleness check must pass, or a fresh I&E must be completed first (RULING-007 §2). | Outstanding balance >= I&E threshold (RPF.8; threshold configurable, default £500). |
| Existing arrangement breach | `ARRANGEMENT_BREACH` | The repayment plan engine has detected a missed payment breach. A fresh I&E is triggered to reassess affordability before the next treatment decision. | Triggered automatically by breach handling logic in `domain/repaymentplan`. |
| Periodic review due | `PERIODIC_REVIEW` | Flowable timer fires when `next_review_due_date` is reached. | Interval driven by `ie-review-frequency.dmn`. |
| Agent-initiated (manual) | `AGENT_INITIATED` | An agent manually initiates an I&E outside of a structured trigger (e.g., customer calls to report changed circumstances). | Role `AGENT`, `SPECIALIST_AGENT`, or `ESCALATIONS_AGENT`. |
| Self-service customer submission | `SELF_SERVICE_SUBMISSION` | Customer submits I&E via the DWP strategic self-service portal/app; `domain/integration` validates and hands off to the I&E engine. | Customer-facing portal/app UI is external to DCMS; inbound integration is in scope (IEC.2, IEC.8, CC.22). |
| Vulnerability flag change | `VULNERABILITY_FLAG_CHANGE` | A vulnerability flag is added or severity is increased on the customer record. The vulnerability protocol may require an I&E to assess whether the current arrangement remains suitable. | Only triggers I&E when the vulnerability protocol matrix routes to `REQUIRES_IE_REVIEW`. Governed by vulnerability configurability (RULING-003). |

---

## 8. Open Questions and Gates

### DDE-OQ-08: Confirmed staleness period for I&E assessments

**Question:** Does DWP policy set an I&E staleness period shorter than the FCA CONC 7.3 12-month benchmark? What is the confirmed DWP operational threshold?

**Status:** AWAITING DWP CLIENT SIGN-OFF.

**What this gates:**
- The default row in `ie-review-frequency.dmn` (`reviewIntervalMonths = 12`). If DWP policy mandates 6 months, this DMN row must be updated before going live.
- The staleness check in `IECaptureService.assessStaleness(...)`. The threshold value must be loaded from Tier 1 configuration, not hardcoded, so DWP can update it without a code deployment. The configuration key is `ie.staleness.threshold.months`.
- Any warning presented to agents during arrangement offer when the I&E is approaching or past the threshold.

**Design assumption active until sign-off:** 12 months (FCA CONC 7.3 standard), with the shorter-of rule applied when a circumstance change date is recorded.

### DDE-OQ-09: I&E requirement for benefit-deduction arrangements

**Question:** Does DWP policy require a full I&E assessment before setting a deduction-from-benefit arrangement, for hardship-prevention purposes?

**Status:** AWAITING DWP CLIENT SIGN-OFF.

**What this gates:**
- The `ARRANGEMENT_OFFER` trigger logic. If the answer is no, the trigger engine must suppress the I&E prompt when `account.recovery_method = BENEFIT_DEDUCTION`.
- The income category flags: `ie_income_category.is_benefit_income` is already modelled so that benefit-only income profiles can be identified at trigger evaluation time.
- If the answer is yes, this domain pack applies without modification. If the answer is no, the trigger condition for `ARRANGEMENT_OFFER` must include a `recovery_method != BENEFIT_DEDUCTION` gate.

**Design assumption active until sign-off:** I&E is required for all arrangements regardless of recovery method (conservative default; aligns with RULING-007 §3 which clarifies FCA CONC does not apply but leaves DWP policy open).

### New question surfaced by this domain pack: NQ-IE-001

**Question:** When a third-party FCA-regulated debt adviser (e.g., StepChange) submits an I&E on behalf of a customer, what is the intake process? Is there a structured data format (e.g., Standard Financial Statement XML) or is the adviser's document attached manually?

**Owner:** DWP Debt Domain Expert to clarify; Delivery Lead to confirm system interface scope.

**What this gates:**
- Whether `ie_record.channel = THIRD_PARTY_ADVISER` requires a structured inbound API (integration domain) or a manual document attachment and agent re-entry.
- Whether the CRA validation step applies to third-party I&E submissions.

### New question surfaced by this domain pack: NQ-IE-002

**Question:** When a customer refuses to provide I&E data, what is the default arrangement rate to be used? Is this a configurable policy value or a fixed regulatory floor?

**Owner:** DWP Debt Domain Expert. Referenced in RULING-007 §Customer refuses I&E.

**What this gates:**
- Whether the I&E engine needs a `REFUSED` status and a separate code path that applies a default rate from Tier 1 configuration.
- Whether the `AffordabilityAssessment` record should be created in a `DEFAULT_APPLIED` state when no real I&E data exists.

---

## 9. Acceptance Criteria

Requirement IDs are sourced from `Tender requirements docs/Functional-Requirements-Consolidated.md`. RULING-007 is sourced from `docs/project-foundation/domain-rulings/RULING-007-ie-assessment-affordability.md`.

---

**AC-IE-001 — Agent initiates I&E capture and completes all fields (golden path)**
Source: IEC.1, IEC.4, RPF.19

Given an agent with role `AGENT` or above is viewing a customer account  
And the account has an outstanding balance at or above the configured I&E threshold  
And no DRAFT IE record exists for this customer  
When the agent initiates an I&E capture with a valid household type, income lines, and expenditure lines  
And the agent completes the capture  
Then an `ie_record` is persisted with `status = COMPLETED` and `capture_completed_at` set  
And an `ie_affordability_assessment` is created with `total_monthly_income`, `total_monthly_expenditure`, `net_disposable_income`, and `ndi_band` correctly computed  
And arrangement options (weekly, fortnightly, monthly) are present on the assessment  
And the assessment references the `sla_version_id` of the SLA table active at the time of capture.

---

**AC-IE-002 — NDI at or below zero routes to forbearance (RULING-007)**
Source: DIC.9, RPF.25, RULING-007 §4 and guardrail 2

Given an agent has completed an IE record where total monthly income is less than or equal to total monthly expenditure  
When the affordability calculation runs  
Then `ie_affordability_assessment.net_disposable_income` is zero or negative  
And `arrangement_type = FORBEARANCE`  
And `minimum_instalment = 0.00`  
And `forbearance_review_date` is set to a date within the interval produced by `ie-review-frequency.dmn` for `arrangementType = FORBEARANCE`  
And the agent UI displays a forbearance notice, not a repayment amount.

---

**AC-IE-003 — Staleness check blocks arrangement offer when IE is stale**
Source: RPF.8, RPF.11, IEC.11, RULING-007 §2

Given a customer has a COMPLETED IE record with `capture_completed_at` more than the configured staleness threshold (default 12 months) in the past  
When an agent attempts to create a new repayment arrangement for that customer  
Then the system presents a staleness warning before the arrangement creation screen is reachable  
And the agent must either confirm the existing IE is still valid (with a reason recorded) or initiate a new IE capture  
And if the agent proceeds without refreshing the IE, the arrangement creation is blocked at the point of confirmation.

---

**AC-IE-004 — Circumstance change overrides staleness threshold**
Source: IEC.11, RULING-007 §2 and guardrail 1

Given a customer has a COMPLETED IE record captured 8 months ago  
And a vulnerability flag was added to the customer record 2 months after the IE was completed  
When the staleness assessment runs  
Then the system determines the IE is stale because the date of the last known circumstance change (the vulnerability flag addition) is more recent than the IE capture date  
And a staleness warning is shown even though the IE is less than 12 months old.

---

**AC-IE-005 — Only one DRAFT IE record permitted per customer**
Source: IEC.10

Given a customer already has an IE record in DRAFT status  
When any user or system process attempts to initiate a second IE capture for the same customer  
Then the system rejects the request with an error indicating a DRAFT record already exists  
And the existing DRAFT record is unchanged.

---

**AC-IE-006 — SLA deviation is flagged and requires agent override reason**
Source: IEC.5, IEC.11, DIC.9

Given an agent is completing an IE record for a SINGLE household type  
And the agent enters a declared monthly expenditure for the FOOD category that exceeds the configured SLA amount for SINGLE households  
Then `ie_expenditure_line.exceeds_sla = TRUE` is set for that line  
And the SLA reference amount is stored in `ie_expenditure_line.sla_amount`  
And the UI highlights the deviation to the agent  
When the agent attempts to complete the IE record without providing an override reason for the SLA-exceeding line  
Then the system rejects the completion request with a validation error referencing the specific expenditure line  
And the IE record remains in DRAFT status.

---

**AC-IE-007 — Periodic review task is created by Flowable timer when review is due**
Source: IEC.7, RPF.8, UI.28

Given a customer has a completed IE record and an active `ie_review_schedule` with `next_review_due_date = today`  
When the Flowable timer fires for that customer's process instance  
Then `IEReviewSchedulerService.handleReviewDue(...)` is invoked  
And a work item of type `IE_REVIEW` is created in the appropriate queue based on the customer's vulnerability level and arrangement type  
And the work item carries the customer ID, account ID, and trigger type `PERIODIC_REVIEW`  
And no second review task is created if one is already open for this customer.

---

**AC-IE-008 — CRA validation discrepancy blocks IE completion pending agent review**
Source: IEC.3, IEC.7 (cross-reference with bureau)

Given an agent has entered income lines on a DRAFT IE record  
And the agent has triggered CRA validation  
When the CRA response returns with `cra_validation_status = DISCREPANCY` for one or more income lines  
Then `ie_record.cra_validation_status` is updated to `DISCREPANCY`  
And the discrepancy details (category, declared amount, bureau amount) are surfaced to the agent  
And the IE record cannot be completed until the agent explicitly acknowledges each discrepancy (accepting declared or accepting bureau figure)  
And the agent's acknowledgement decision is recorded for audit.

---

**AC-IE-009 — IE history is immutable and retained on supersession**
Source: IEC.6, IEC.10, DIC.11, SoR requirements (audit)

Given a customer has two COMPLETED IE records (IE-A captured 14 months ago, IE-B captured today)  
When IE-B is completed  
Then IE-A transitions to `status = SUPERSEDED`  
And both IE-A and IE-B remain in the `ie_record` table and are retrievable via `IECaptureService.getIEHistory(...)`  
And neither record's income lines, expenditure lines, nor affordability assessment can be modified  
And the audit trail records the supersession event referencing both record IDs.

---

**AC-IE-010 — Arrangement offer trigger prompts I&E when IE threshold is met**
Source: IEC.4, IEC.5, DW.22, UI.28

Given an account has an outstanding balance at or above the configured I&E threshold  
And the most recent COMPLETED IE record is stale or does not exist  
When an agent navigates to the arrangement creation screen for that account  
Then the system automatically creates an `ie_trigger` of type `ARRANGEMENT_OFFER`  
And presents the agent with the IE capture form as a mandatory step before arrangement options are shown  
And the arrangement creation screen is not reachable until IE capture is completed.

---

**AC-IE-011 — Review is cancelled on account closure**
Source: RPF.10, IEC.7 (negative case — no orphan timer)

Given a customer has an active `ie_review_schedule` with a Flowable timer running  
When the account is closed (full recovery, write-off, or DCA placement confirmed)  
Then `IEReviewSchedulerService.cancelReview(...)` is called with the appropriate reason  
And the Flowable timer intermediate catch event is cancelled via `CancelIEReviewTimerCommand`  
And no `IE_REVIEW` work item is created for that customer after the cancellation  
And the `ie_review_schedule` record reflects the cancellation with a reason and timestamp.

---

**AC-IE-012 - Self-service I&E submission is accepted through integration**
Source: IEC.2, IEC.8, CC.22, DW.22, DIC.21

Given the DWP strategic self-service portal/app submits a valid I&E payload for a known customer through `domain/integration`
When DCMS accepts the submission
Then the I&E engine creates an `ie_trigger` of type `SELF_SERVICE_SUBMISSION`
And creates or completes an `ie_record` with `channel = SELF_SERVICE`
And records the source self-service action reference for audit traceability
And runs the affordability calculation without manual re-keying
And either generates arrangement options or routes the case to workflow review according to the same policy gates used for agent-led I&E capture.

---

**AC-IE-013 — Configurable income and expenditure categories are loaded at runtime**
Source: IEC.11, RPF.14

Given a `COMPLIANCE` or `ADMIN` user has added a new income category (e.g., `SELF_EMPLOYMENT`) to `ie_income_category` via the Tier 1 Foundations admin UI  
And the new category has `is_active = TRUE` and an `effective_from` date of today or earlier  
When an agent initiates a new IE capture on or after that date  
Then the new income category appears in the IE capture form income line selector  
And existing completed IE records are not retroactively affected.

---

## Handoff Declaration

**Completed:** Domain pack for the Income & Expenditure Engine written, covering entity model (DDL), configuration entities, DMN table structures, Java service interfaces, Flowable integration approach, trigger type enumeration, open questions, and 13 acceptance criteria.

**Files changed:**
- `/c/Users/twin/OneDrive - Netcompany/Documents/DWP/DWP-system-prototype/docs/project-foundation/domain-packs/IE-engine-domain-pack.md` (created)

**Requirement IDs covered:** IEC.1, IEC.2, IEC.3, IEC.4, IEC.5, IEC.6, IEC.7, IEC.8, IEC.9 (deferred — noted), IEC.10, IEC.11, RPF.8, RPF.11, RPF.18, RPF.19, RPF.25, DIC.9, DIC.10, DIC.11, DIC.21, DW.22, CC.22, UI.28 — all sourced from `Tender requirements docs/Functional-Requirements-Consolidated.md`.

**Domain rulings used:** RULING-007 (I&E Assessment — FCA Affordability Obligations).

**ACs satisfied:** AC-IE-001 through AC-IE-013.

**ACs not satisfied:** None within scope. Customer-facing portal/app UI remains external to DCMS; inbound self-service I&E integration is covered by AC-IE-012.

**Assumptions made:**
- Staleness threshold defaults to 12 months pending DDE-OQ-08 sign-off; stored as configurable Tier 1 key `ie.staleness.threshold.months`.
- I&E is required for all arrangement types pending DDE-OQ-09 sign-off (conservative default).
- Third-party adviser I&E intake is channel `THIRD_PARTY_ADVISER` with agent re-entry; no structured inbound third-party adviser API assumed until NQ-IE-001 is resolved.
- Self-service I&E intake uses channel `SELF_SERVICE` and is received only from a trusted DWP strategic portal/app integration boundary, not from an anonymous public endpoint.

**Open questions:**
- DDE-OQ-08 — staleness period — owner: DWP Client.
- DDE-OQ-09 — benefit-deduction I&E requirement — owner: DWP Client.
- DDE-OQ-SS-03 — synchronous vs asynchronous portal/app I&E validation — owner: Product Owner + Debt Domain Expert.
- NQ-IE-001 — third-party adviser I&E intake process — owner: DWP Debt Domain Expert + Delivery Lead.
- NQ-IE-002 — refused I&E default rate — owner: DWP Debt Domain Expert.

**Next role:** Delivery Designer / Solution Architect.

**What they need:** This domain pack, RULING-007, MASTER-DESIGN-DOCUMENT.md §7a, and ADR-008 (three-tier configurability). The Delivery Designer must confirm module boundary decisions for `IECaptureService` (whether it is a standalone Spring `@Service` within `domain/repaymentplan` or a sub-package) and confirm the Flowable process model for the IE review timer (separate sub-process or integrated into the main account process).
