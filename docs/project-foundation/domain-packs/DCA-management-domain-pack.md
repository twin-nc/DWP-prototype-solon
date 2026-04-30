# DCA Management Domain Pack

**Domain:** Third-Party / DCA (Debt Collection Agency) Management
**Package:** `domain/thirdpartymanagement`
**Status:** Draft — gated on DDE-OQ-10 (pre-placement notice period)
**Date:** 2026-04-27
**Author:** Business Analyst Agent
**Domain ruling in scope:** RULING-008 (DCA Placement — Pre-Placement Disclosure Obligations)
**Requirement IDs covered:** 3PM.1–3PM.18, DW.22, DW.27, DIC.19, DIC.28, I3PS.1, I3PS.3, I3PS.4, I3PS.11, SoR.5, SoR.6

---

## 1. Purpose and Scope

### 1.1 What this domain pack covers

This pack defines the business rules, state machine, entity model, service interfaces, Flowable integration, and acceptance criteria for the DCA Management sub-domain within DCMS. It covers:

- The agency register: configured DCA partners, their contact details, file format specifications, PGP keys, FTPS endpoints, commission rates, and accreditation status.
- The placement lifecycle: eligibility assessment, pre-placement customer disclosure, file generation and transfer, DCA acknowledgement, active placement monitoring, and closure.
- Recall: initiating and confirming recall of a placement from a DCA, including re-entry to internal collection treatment.
- Payment reconciliation: receiving, matching, and allocating DCA-reported payments.
- Commission calculation and ledger: calculating commission due to DCAs based on configured rates and recovered amounts.
- Transfer audit: immutable log of all outbound and inbound file exchanges.
- Reporting: DCA inventory, performance, and financial reconciliation views.

### 1.2 What this domain pack explicitly excludes

- Legal firm / solicitor placements — these share structural similarities but differ in regulatory obligations and are a separate future domain.
- Field agent placements — out of scope for current delivery.
- Customer-facing self-service portal UI - external to DCMS in the current baseline. DCA-originated or portal-originated repayment-to-offer actions are handled through the self-service/integration boundary where relevant; this DCA pack does not own that portal UI.
- Debt sale — noted as `Could have` in the tender (3PM.17); excluded from this pack.
- Real-time DCA portal access (I3PS.4) — noted as `Could have`; batch file interface is the primary model.
- DM6/DCMS migration tooling — Release 2.

### 1.3 Package name recommendation

The MASTER-DESIGN-DOCUMENT.md already names `domain/thirdpartymanagement` as the owner of DCA placement, recall, reconciliation, commission/billing, and third-party partner interfaces (distinct from `domain/integration`, which owns inbound/outbound API adapters and candidate staging as the anti-corruption layer). This pack confirms and adopts `domain/thirdpartymanagement` as the Java package.

`domain/integration` remains the owner of the FTPS transport adapter and file-transfer plumbing (per its anti-corruption layer role). `domain/thirdpartymanagement` calls `domain/integration` for file transfer execution and owns the business logic above the transport layer.

---

## 2. Placement State Machine

### 2.1 States

| State | Meaning |
|---|---|
| `CANDIDATE` | Account has been identified by the Placement Rules Engine as eligible for DCA referral. No disclosure has been sent. |
| `PRE_PLACEMENT_NOTICE` | DISCLOSURE_NOTICE has been sent to the debtor. The mandatory notice timer is running. Placement is blocked until the timer elapses and no dispute is received. |
| `AWAITING_ACKNOWLEDGEMENT` | Placement data file has been transmitted to the DCA. Awaiting DCA acknowledgement within the configured SLA window. |
| `PLACED` | DCA has acknowledged receipt. Account is formally placed. Awaiting DCA to report commencement of activity. |
| `ACTIVE` | DCA has reported commencement of activity. Payments and activity updates are expected. DWP communications are suppressed. |
| `DISPUTED` | Debtor has raised a dispute (either during notice period or during placement). Placement is blocked or suspended. Dispute process owns the account until resolution. |
| `FAILED_TRANSFER` | Outbound placement file transfer has failed after retry exhaustion. Account is not with the DCA. Operational alert raised. |
| `RECALLED` | DWP has instructed recall and the DCA has acknowledged. Account has been returned to internal collection treatment. DWP communications suppression lifted. |
| `CLOSED` | Placement is closed: debt fully recovered, written off, statute-barred, or debtor deceased during placement. Terminal state. |

`PAYMENT_RECEIVED` is not a separate placement state. DCA payment receipts are events recorded against an `ACTIVE` or `RECALLED` placement (payments may arrive after recall); they do not change the placement state machine.

### 2.2 Valid transitions

| From | To | Trigger event | Role required | Flowable action |
|---|---|---|---|---|
| — | `CANDIDATE` | Placement Rules Engine evaluation run completes; account meets eligibility criteria | `SYSTEM` (automated) | Flowable sub-process `DCA_PLACEMENT_PROCESS` started; token parks at disclosure gate |
| `CANDIDATE` | `PRE_PLACEMENT_NOTICE` | `DCAPlacementService.initiatePrePlacementNotice(placementId)` called; DISCLOSURE_NOTICE dispatched via `domain/communications` | `SPECIALIST_AGENT` or `OPS_MANAGER` (if manual); `SYSTEM` (if automated) | Timer boundary event started; value = configured notice period (DDE-OQ-10 gated) |
| `CANDIDATE` | `DISPUTED` | Agent or debtor raises dispute before disclosure is sent | `AGENT`, `SPECIALIST_AGENT` | Signal `DISPUTE_RAISED` fires; process routes to dispute sub-process; DCA placement process suspended |
| `PRE_PLACEMENT_NOTICE` | `AWAITING_ACKNOWLEDGEMENT` | Notice period timer elapses; no dispute received; DPA confirmed in agency register; `DCAFileService.generateAndTransfer(placementId)` succeeds | `SYSTEM` (timer-driven) | Timer boundary event completes; service task fires file generation delegate; token advances |
| `PRE_PLACEMENT_NOTICE` | `DISPUTED` | Debtor raises dispute during notice period | `AGENT`, `SPECIALIST_AGENT`, `SYSTEM` (self-serve signal — future) | Interrupting boundary event cancels timer; routes to dispute sub-process |
| `PRE_PLACEMENT_NOTICE` | `FAILED_TRANSFER` | FTPS transfer fails after retry exhaustion during the notice period file send | `SYSTEM` | Service task delegate raises `DC-FTPS-0003`; process routes to failure lane; alert raised |
| `AWAITING_ACKNOWLEDGEMENT` | `PLACED` | DCA sends acknowledgement file or API callback within SLA | `SYSTEM` (inbound file handler) | `DCAReconciliationService.recordAcknowledgement(placementId)` called; token advances |
| `AWAITING_ACKNOWLEDGEMENT` | `FAILED_TRANSFER` | Acknowledgement SLA timer expires; no acknowledgement received | `SYSTEM` (timer) | SLA timer fires; alert raised; operational queue work item created |
| `AWAITING_ACKNOWLEDGEMENT` | `RECALLED` | Manual recall triggered while awaiting acknowledgement | `OPS_MANAGER`, `SPECIALIST_AGENT` | Signal `RECALL_INSTRUCTED`; recall file sent to DCA; suppression not yet active so no suppression lift needed |
| `PLACED` | `ACTIVE` | DCA reports commencement of activity (inbound activity file or API event) | `SYSTEM` | `CommunicationSuppressionService.activate(SUPPRESSION_REASON=DCA_PLACEMENT_INTERNAL)` called via command bus |
| `PLACED` | `RECALLED` | Recall instruction issued | `OPS_MANAGER`, `SPECIALIST_AGENT` | Signal `RECALL_INSTRUCTED`; `DCARecallService.initiateRecall(placementId)` called |
| `ACTIVE` | `RECALLED` | Recall instruction issued | `OPS_MANAGER`, `SPECIALIST_AGENT` | Signal `RECALL_INSTRUCTED`; `DCARecallService.initiateRecall(placementId)` called; suppression held until acknowledgement |
| `ACTIVE` | `DISPUTED` | Dispute raised while account is active with DCA | `AGENT`, `SPECIALIST_AGENT` | Signal `DISPUTE_RAISED`; `DCARecallService.initiateRecall(placementId)` called automatically; suppression held |
| `ACTIVE` | `CLOSED` | Full recovery confirmed; write-off instruction received; death or insolvency registered | `SYSTEM` | `CommunicationSuppressionService.deactivate(...)` called; placement record closed; commission entry finalised |
| `RECALLED` | `CANDIDATE` | After dispute resolution or business decision, account re-enters eligibility assessment | `SPECIALIST_AGENT`, `OPS_MANAGER` | New placement sub-process run; prior placement record retained (closed) |
| `RECALLED` | `CLOSED` | Account is closed by write-off, statute-barred, death or insolvency after recall | `SYSTEM` | Terminal state |
| `DISPUTED` | `CANDIDATE` | Dispute resolved in DWP's favour; account re-enters eligibility | `SPECIALIST_AGENT` | New placement sub-process can be initiated |
| `DISPUTED` | `CLOSED` | Dispute upheld; debt cancelled or balance adjusted | `SYSTEM`, `SPECIALIST_AGENT` | Terminal state |
| `FAILED_TRANSFER` | `PRE_PLACEMENT_NOTICE` | Manual retry authorised by OPS_MANAGER | `OPS_MANAGER` | Process resumes at transfer step; new transfer log entry created |
| `FAILED_TRANSFER` | `CLOSED` | Decision taken not to retry; account returned to internal treatment | `OPS_MANAGER` | Terminal state |

### 2.3 Invalid transitions (system must reject)

- Any state -> `ACTIVE` without passing through `AWAITING_ACKNOWLEDGEMENT` and `PLACED`.
- `PRE_PLACEMENT_NOTICE` -> `AWAITING_ACKNOWLEDGEMENT` if no DPA is on file for the agency (`dca_agency.dpa_in_place = false`).
- `PRE_PLACEMENT_NOTICE` -> `AWAITING_ACKNOWLEDGEMENT` if notice period has not elapsed.
- `CANDIDATE` -> `AWAITING_ACKNOWLEDGEMENT` (notice period cannot be skipped).
- `RECALLED` -> `ACTIVE` (once recalled, a new placement sub-process must be started).
- Any transition while a `DISPUTED` record is open and unresolved (dispute must be resolved first).
- `RECALLED` status must not be set unless the DCA has sent a confirmed recall acknowledgement (RULING-008, guardrail 4).

### 2.4 Terminal states

`CLOSED` is the only terminal state. A placement in `CLOSED` state is immutable. No further transitions are permitted.

---

## 3. Entity Model — PostgreSQL DDL (Flyway-compatible)

All tables use `UUID` primary keys. Flyway migration files must be sequentially numbered at merge time per STD-PLAT-006.

```sql
-- V{NNN}__create_dca_agency.sql

CREATE TYPE dca_file_format AS ENUM (
    'CSV_FLAT',
    'FIXED_WIDTH',
    'JSON_REST',
    'XML_BATCH'
);

CREATE TABLE dca_agency (
    id                    UUID        NOT NULL DEFAULT gen_random_uuid(),
    name                  VARCHAR(255) NOT NULL,
    reference_code        VARCHAR(50)  NOT NULL UNIQUE,       -- internal short code used in file naming and logs
    contact_name          VARCHAR(255),
    contact_email         VARCHAR(320),
    contact_phone         VARCHAR(50),
    file_format           dca_file_format NOT NULL,
    pgp_public_key_armored TEXT,                              -- ASCII-armored PGP public key for outbound encryption
    pgp_key_fingerprint   VARCHAR(128),                       -- for verification and logging
    ftps_endpoint_host    VARCHAR(255),
    ftps_endpoint_port    INTEGER      DEFAULT 21,
    ftps_username         VARCHAR(255),                       -- credential reference (actual secret stored in secrets manager)
    inbound_path          VARCHAR(512),                       -- FTPS path for inbound payment/activity files
    outbound_path         VARCHAR(512),                       -- FTPS path for placement file delivery
    commission_rate_pct   NUMERIC(5,4) NOT NULL,              -- e.g. 0.2500 = 25%
    dpa_in_place          BOOLEAN      NOT NULL DEFAULT FALSE, -- Data Processing Agreement confirmed
    dpa_reference         VARCHAR(255),
    fca_accreditation_ref VARCHAR(255),
    fca_accreditation_valid_until DATE,
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by            VARCHAR(255) NOT NULL,
    CONSTRAINT pk_dca_agency PRIMARY KEY (id)
);

CREATE INDEX idx_dca_agency_active ON dca_agency (active);
```

```sql
-- V{NNN}__create_dca_placement.sql

CREATE TYPE dca_placement_status AS ENUM (
    'CANDIDATE',
    'PRE_PLACEMENT_NOTICE',
    'AWAITING_ACKNOWLEDGEMENT',
    'PLACED',
    'ACTIVE',
    'DISPUTED',
    'FAILED_TRANSFER',
    'RECALLED',
    'CLOSED'
);

CREATE TABLE dca_placement (
    id                        UUID         NOT NULL DEFAULT gen_random_uuid(),
    account_id                UUID         NOT NULL,           -- FK to domain/account debt account
    agency_id                 UUID         NOT NULL,           -- FK to dca_agency
    status                    dca_placement_status NOT NULL DEFAULT 'CANDIDATE',
    placement_date            DATE,                            -- date file was successfully transmitted
    recall_date               DATE,                            -- date recall was acknowledged by DCA
    closed_date               DATE,
    pre_notice_sent_at        TIMESTAMPTZ,                     -- timestamp DISCLOSURE_NOTICE dispatched
    notice_period_expires_at  TIMESTAMPTZ,                     -- derived: pre_notice_sent_at + configured notice period
    acknowledged_at           TIMESTAMPTZ,                     -- timestamp DCA acknowledgement received
    process_instance_id       VARCHAR(255),                    -- Flowable process instance ID for correlation
    data_fields_shared        TEXT[],                          -- list of field names transmitted at placement (RULING-008)
    legal_basis               VARCHAR(100) NOT NULL DEFAULT 'UK_GDPR_ART6_1E',
    transmitted_at            TIMESTAMPTZ,                     -- timestamp data file was transmitted
    external_placement_ref    VARCHAR(255),                    -- DCA's own reference for the placement
    idempotency_key           VARCHAR(255) UNIQUE,             -- prevents duplicate placement submissions
    created_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by                VARCHAR(255) NOT NULL,
    CONSTRAINT pk_dca_placement PRIMARY KEY (id),
    CONSTRAINT fk_dca_placement_agency FOREIGN KEY (agency_id) REFERENCES dca_agency (id)
);

CREATE INDEX idx_dca_placement_account    ON dca_placement (account_id);
CREATE INDEX idx_dca_placement_agency     ON dca_placement (agency_id);
CREATE INDEX idx_dca_placement_status     ON dca_placement (status);
CREATE INDEX idx_dca_placement_process    ON dca_placement (process_instance_id);
```

```sql
-- V{NNN}__create_dca_placement_payment.sql

CREATE TABLE dca_placement_payment (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    placement_id        UUID         NOT NULL,
    amount              NUMERIC(15,2) NOT NULL,
    currency            CHAR(3)      NOT NULL DEFAULT 'GBP',
    received_date       DATE         NOT NULL,                 -- date DCA reports payment was received from debtor
    reported_in_file    VARCHAR(255),                          -- name of the inbound file that contained this payment
    payment_reference   VARCHAR(255),                          -- DCA's payment reference
    reconciled          BOOLEAN      NOT NULL DEFAULT FALSE,
    reconciled_at       TIMESTAMPTZ,
    payment_domain_ref  UUID,                                  -- reference to the payment record created in domain/payment
    idempotency_key     VARCHAR(255) UNIQUE,                   -- file name + payment_reference ensures no duplicate posting
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_dca_placement_payment PRIMARY KEY (id),
    CONSTRAINT fk_dpp_placement FOREIGN KEY (placement_id) REFERENCES dca_placement (id)
);

CREATE INDEX idx_dpp_placement    ON dca_placement_payment (placement_id);
CREATE INDEX idx_dpp_reconciled   ON dca_placement_payment (reconciled);
```

```sql
-- V{NNN}__create_dca_commission_entry.sql

CREATE TYPE dca_commission_status AS ENUM (
    'PENDING',
    'APPROVED',
    'DISPUTED',
    'PAID',
    'REVERSED'
);

CREATE TABLE dca_commission_entry (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    placement_id        UUID         NOT NULL,
    agency_id           UUID         NOT NULL,
    period_start        DATE         NOT NULL,                 -- commission period start (typically monthly)
    period_end          DATE         NOT NULL,
    recovered_amount    NUMERIC(15,2) NOT NULL,
    commission_rate_pct NUMERIC(5,4) NOT NULL,                 -- rate at time of calculation (not live from agency)
    commission_amount   NUMERIC(15,2) NOT NULL,                -- recovered_amount * commission_rate_pct
    currency            CHAR(3)      NOT NULL DEFAULT 'GBP',
    status              dca_commission_status NOT NULL DEFAULT 'PENDING',
    approved_by         VARCHAR(255),
    approved_at         TIMESTAMPTZ,
    notes               TEXT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_dca_commission_entry PRIMARY KEY (id),
    CONSTRAINT fk_dce_placement FOREIGN KEY (placement_id) REFERENCES dca_placement (id),
    CONSTRAINT fk_dce_agency    FOREIGN KEY (agency_id)    REFERENCES dca_agency (id)
);

CREATE INDEX idx_dce_placement ON dca_commission_entry (placement_id);
CREATE INDEX idx_dce_agency    ON dca_commission_entry (agency_id);
CREATE INDEX idx_dce_period    ON dca_commission_entry (period_start, period_end);
CREATE INDEX idx_dce_status    ON dca_commission_entry (status);
```

```sql
-- V{NNN}__create_dca_transfer_log.sql

CREATE TYPE dca_transfer_direction AS ENUM ('OUTBOUND', 'INBOUND');
CREATE TYPE dca_transfer_status    AS ENUM ('SUCCESS', 'FAILED', 'PARTIAL', 'PENDING');

CREATE TABLE dca_transfer_log (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    agency_id           UUID         NOT NULL,
    file_name           VARCHAR(512) NOT NULL,
    direction           dca_transfer_direction NOT NULL,
    transfer_timestamp  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    file_size_bytes     BIGINT,
    checksum_sha256     VARCHAR(64),
    checksum_verified   BOOLEAN,
    pgp_verified        BOOLEAN,
    transfer_status     dca_transfer_status NOT NULL,
    error_code          VARCHAR(50),                           -- e.g. DC-FTPS-0001, DC-FTPS-0003
    error_message       TEXT,
    retry_count         INTEGER      NOT NULL DEFAULT 0,
    related_placement_id UUID,                                 -- nullable; not all transfers relate to a single placement
    CONSTRAINT pk_dca_transfer_log PRIMARY KEY (id),
    CONSTRAINT fk_dtl_agency FOREIGN KEY (agency_id) REFERENCES dca_agency (id)
);

CREATE INDEX idx_dtl_agency    ON dca_transfer_log (agency_id);
CREATE INDEX idx_dtl_timestamp ON dca_transfer_log (transfer_timestamp);
CREATE INDEX idx_dtl_status    ON dca_transfer_log (transfer_status);
```

---

## 4. Configuration Entities — Tier 1 Foundations

Tier 1 configuration is stored as editable lookup tables, managed by a no-code admin interface (Foundations Configurator). Changes require the `FLOWABLE_ADMIN` or `OPS_MANAGER` role and are effective-dated per STD-PLAT-005.

```sql
-- V{NNN}__create_dca_placement_eligibility_rule.sql

CREATE TABLE dca_placement_eligibility_rule (
    id                                    UUID         NOT NULL DEFAULT gen_random_uuid(),
    rule_name                             VARCHAR(255) NOT NULL UNIQUE,
    description                           TEXT,
    minimum_balance_pence                 BIGINT       NOT NULL DEFAULT 0,          -- minimum outstanding balance to be eligible
    minimum_account_age_days              INTEGER      NOT NULL DEFAULT 0,          -- minimum days since debt was created
    minimum_days_since_last_payment       INTEGER,                                  -- NULL = not required
    required_treatment_stages_exhausted   BOOLEAN      NOT NULL DEFAULT TRUE,       -- internal treatment must be exhausted first
    excluded_if_vulnerable                BOOLEAN      NOT NULL DEFAULT TRUE,       -- accounts flagged vulnerable are excluded
    excluded_if_in_dispute                BOOLEAN      NOT NULL DEFAULT TRUE,       -- accounts with open dispute are excluded
    excluded_if_breathing_space           BOOLEAN      NOT NULL DEFAULT TRUE,       -- accounts in breathing space are excluded
    excluded_if_insolvency_registered     BOOLEAN      NOT NULL DEFAULT TRUE,
    excluded_if_deceased                  BOOLEAN      NOT NULL DEFAULT TRUE,
    excluded_if_dpa_missing               BOOLEAN      NOT NULL DEFAULT TRUE,       -- DPA must be on file for target DCA
    excluded_if_prior_placement_active    BOOLEAN      NOT NULL DEFAULT TRUE,       -- 3PM.10: same customer not placed twice simultaneously
    max_active_placements_per_customer    INTEGER      NOT NULL DEFAULT 1,          -- 3PM.10 enforcement
    effective_from                        DATE         NOT NULL,
    effective_to                          DATE,
    created_at                            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at                            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by                            VARCHAR(255) NOT NULL,
    CONSTRAINT pk_dca_eligibility_rule PRIMARY KEY (id)
);
```

The active rule is selected by effective date at runtime (STD-PLAT-005). Only one rule set may be active at any point in time.

---

## 5. DMN Tables — Tier 2 Rules

### 5.1 `dca-placement-eligibility.dmn`

**Purpose:** Given account attributes, determines whether an account is eligible for DCA placement and (if eligible) which agency tier should receive it.

**Hit policy:** `FIRST` — the first matching rule wins.

**Inputs:**

| Input | Type | Description |
|---|---|---|
| `outstandingBalancePence` | long | Outstanding balance in pence |
| `accountAgeDays` | integer | Days since debt account was created |
| `daysSinceLastPayment` | integer | Days since last payment (NULL-safe: treat NULL as very large) |
| `treatmentStagesExhausted` | boolean | Whether all internal treatment stages have completed |
| `vulnerabilityFlag` | boolean | Whether the account has an active vulnerability flag |
| `inDisputeFlag` | boolean | Whether an open dispute exists on the account |
| `breathingSpaceActive` | boolean | Whether a breathing space moratorium is active |
| `insolvencyRegistered` | boolean | |
| `deceasedFlag` | boolean | |
| `dpaInPlace` | boolean | Whether a Data Processing Agreement is on file for the target DCA |
| `priorActivePlacement` | boolean | Whether the customer has another active placement (3PM.10) |

**Outputs:**

| Output | Type | Description |
|---|---|---|
| `eligible` | boolean | Whether the account may proceed to placement |
| `ineligibilityReason` | string | Human-readable reason if ineligible (logged and shown to agent) |
| `recommendedAgencyTier` | string (enum) | `TIER_1`, `TIER_2`, `TIER_3`, or `NULL` if ineligible |

**Illustrative rules (non-exhaustive; full rules defined by business configuration):**

| Rule | vulnerabilityFlag | inDisputeFlag | breathingSpaceActive | insolvencyRegistered | deceasedFlag | dpaInPlace | priorActivePlacement | treatmentStagesExhausted | outstandingBalancePence >= minimum | eligible | ineligibilityReason | recommendedAgencyTier |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | true | — | — | — | — | — | — | — | — | false | "Account has active vulnerability flag" | null |
| 2 | — | true | — | — | — | — | — | — | — | false | "Open dispute on account" | null |
| 3 | — | — | true | — | — | — | — | — | — | false | "Breathing space moratorium active" | null |
| 4 | — | — | — | true | — | — | — | — | — | false | "Insolvency registered" | null |
| 5 | — | — | — | — | true | — | — | — | — | false | "Deceased" | null |
| 6 | — | — | — | — | — | false | — | — | — | false | "No DPA on file for target DCA" | null |
| 7 | — | — | — | — | — | — | true | — | — | false | "Customer already has active DCA placement" | null |
| 8 | false | false | false | false | false | true | false | false | — | false | "Treatment stages not yet exhausted" | null |
| 9 | false | false | false | false | false | true | false | true | false | false | "Balance below minimum threshold" | null |
| 10 | false | false | false | false | false | true | false | true | true | true | null | "TIER_1" |

Agency tier assignment rules (rule 10 expanded) are configured separately in the Tier 1 agency register and are not expressed in this DMN — the DMN recommends a tier; actual agency selection uses the agency register.

### 5.2 `dca-commission-rate.dmn`

**Assessment:** A full DMN decision table is not warranted at this stage. Commission rates are per-agency, stored in `dca_agency.commission_rate_pct`, and do not vary by recovery type or amount band in the current model. If DWP requires tiered commission (e.g., higher rate for early recovery, lower rate for long-tail debt), this must be raised as a new open question before implementation.

**Current model:** `DCACommissionService` reads `commission_rate_pct` directly from `dca_agency` at commission calculation time, recording the rate snapshot in `dca_commission_entry.commission_rate_pct` for auditability. No DMN table is required unless rate differentiation is confirmed.

**Open question raised:** If commission rate varies by recovery speed, age band, or debt type, a `dca-commission-rate.dmn` with inputs `agencyId`, `daysToRecovery`, `debtType`, `recoveredAmountBand` will be needed. This must be confirmed with DWP before the commission service is built (see section 11).

---

## 6. Service API — Java Interface Signatures

All interfaces are in package `uk.gov.dwp.dcms.thirdpartymanagement`. No Flowable imports in this package — process interaction is via `shared/process/port` command types only.

```java
package uk.gov.dwp.dcms.thirdpartymanagement;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Manages the DCA placement lifecycle: identifying candidates, initiating
 * the pre-placement notice, recording DCA acknowledgements, and updating status.
 *
 * Requirements: 3PM.1, 3PM.2, 3PM.3, 3PM.4, 3PM.12, DW.27
 * Domain ruling: RULING-008
 */
public interface DCAPlacementService {

    /**
     * Runs the placement eligibility DMN against a batch of accounts and returns
     * those that meet the current eligibility rule. Does not initiate placement.
     *
     * @param agencyId the target DCA; eligibility checks include DPA verification
     * @return list of account IDs that pass eligibility, with recommended agency tier
     */
    List<PlacementCandidate> identifyCandidates(UUID agencyId);

    /**
     * Creates a dca_placement record in CANDIDATE state and starts the
     * DCA_PLACEMENT_PROCESS Flowable sub-process.
     *
     * Preconditions (validated before accepting):
     * - Account passes eligibility DMN
     * - dca_agency.dpa_in_place = true
     * - No active placement exists for this customer (3PM.10)
     *
     * @return created placement ID
     * @throws PlacementEligibilityException if preconditions not met
     */
    UUID initiateCandidate(UUID accountId, UUID agencyId, String initiatedBy);

    /**
     * Dispatches the DISCLOSURE_NOTICE to the debtor via domain/communications,
     * records pre_notice_sent_at and notice_period_expires_at, and transitions
     * placement to PRE_PLACEMENT_NOTICE.
     *
     * The notice period value is read from Tier 1 configuration at call time.
     * DDE-OQ-10: notice period value is AWAITING DWP SIGN-OFF.
     *
     * @throws PlacementStateException if placement is not in CANDIDATE state
     */
    void initiatePrePlacementNotice(UUID placementId, String initiatedBy);

    /**
     * Records the DCA acknowledgement of the placement file.
     * Transitions placement from AWAITING_ACKNOWLEDGEMENT to PLACED.
     * Sets acknowledged_at and external_placement_ref.
     *
     * Fires RULING-008 guardrail: acknowledgement record is immutable once written.
     *
     * @throws PlacementStateException if placement is not in AWAITING_ACKNOWLEDGEMENT state
     */
    void recordAcknowledgement(UUID placementId, String dcaAcknowledgementRef, String receivedBy);

    /**
     * Updates placement status (e.g., PLACED -> ACTIVE on receipt of DCA activity report).
     * Validates the transition is permitted per the state machine.
     *
     * @throws PlacementStateException if the transition is not valid
     */
    void updateStatus(UUID placementId, DcaPlacementStatus newStatus, String updatedBy);

    /**
     * Returns the current placement record for an account. Returns the most
     * recent non-CLOSED placement if multiple exist historically.
     */
    PlacementView getActivePlacement(UUID accountId);

    /**
     * Returns the full placement history for an account (all placement records,
     * including CLOSED and RECALLED). Required for SAR support (RULING-008).
     */
    List<PlacementView> getPlacementHistory(UUID accountId);
}
```

```java
package uk.gov.dwp.dcms.thirdpartymanagement;

import java.util.UUID;

/**
 * Generates, encrypts, and transfers DCA placement files.
 * Delegates transport to domain/integration (FTPS adapter).
 * Logs every transfer to dca_transfer_log.
 *
 * Requirements: I3PS.3, I3PS.11, DIC.19
 * Standards: STD-INT-003 (File Transfer — FTPS and PGP)
 */
public interface DCAFileService {

    /**
     * Generates a placement data file for the given placement in the agency's
     * configured file format (dca_agency.file_format).
     * Encrypts with the agency's PGP public key.
     * Appends SHA-256 checksum file.
     * File name must not contain PII (STD-INT-003).
     *
     * @return path/reference to the encrypted file (for transfer step)
     */
    GeneratedFile generatePlacementFile(UUID placementId);

    /**
     * Transfers the generated placement file via FTPS to the DCA's configured
     * endpoint. Uses the FTPS adapter in domain/integration.
     * Logs the transfer to dca_transfer_log.
     * Retries per STD-INT-002 retry policy.
     *
     * On retry exhaustion: sets placement to FAILED_TRANSFER and raises alert.
     *
     * @throws FileTransferException with error code DC-FTPS-0003 if exhausted
     */
    TransferResult transferPlacementFile(UUID placementId, GeneratedFile file);

    /**
     * Verifies the SHA-256 checksum of an inbound file received from a DCA.
     * Verifies PGP signature if applicable.
     * Rejects with DC-FTPS-0001 (PGP) or DC-FTPS-0002 (checksum) if invalid.
     *
     * @throws FileIntegrityException if checksum or PGP verification fails
     */
    void verifyInboundFile(UUID agencyId, String fileName, byte[] fileContent);

    /**
     * Convenience method: generate, encrypt, and transfer in a single call.
     * Used by the Flowable service task delegate.
     */
    TransferResult generateAndTransfer(UUID placementId);
}
```

```java
package uk.gov.dwp.dcms.thirdpartymanagement;

import java.util.List;
import java.util.UUID;

/**
 * Ingests inbound payment and activity files from DCAs.
 * Matches payment records to placement records.
 * Instructs domain/payment to allocate payments.
 * Updates placement status and reconciliation records.
 *
 * Requirements: 3PM.2, 3PM.8, 3PM.13, 3PM.16
 * Standards: STD-INT-002, STD-INT-003
 */
public interface DCAReconciliationService {

    /**
     * Ingests and parses an inbound payment file from a DCA.
     * Verifies file integrity (checksum, PGP) via DCAFileService.
     * Returns a list of unresolved payment records for reconciliation.
     *
     * @param agencyId  identifies which DCA sent the file
     * @param fileName  file name for logging and idempotency
     * @param fileContent raw file bytes (decrypted in-memory per STD-INT-003)
     */
    List<UnreconciledPayment> ingestPaymentFile(UUID agencyId, String fileName, byte[] fileContent);

    /**
     * Attempts to match each unreconciled payment to a dca_placement record
     * by external_placement_ref or account reference.
     * Records matched payments in dca_placement_payment.
     * Returns payments that could not be matched (for manual workbench).
     */
    List<UnreconciledPayment> reconcilePayments(List<UnreconciledPayment> payments);

    /**
     * For a reconciled payment, instructs domain/payment to post the payment
     * to the debt account ledger. Sets dca_placement_payment.reconciled = true
     * and records payment_domain_ref.
     *
     * Idempotent: if payment_domain_ref already set, no-op with audit log.
     */
    void allocatePayment(UUID placementPaymentId, String allocatedBy);

    /**
     * Produces a reconciliation report for an agency for a given period.
     * Used by finance operations and audit (3PM.16).
     */
    ReconciliationReport produceReconciliationReport(UUID agencyId,
                                                      java.time.LocalDate periodStart,
                                                      java.time.LocalDate periodEnd);
}
```

```java
package uk.gov.dwp.dcms.thirdpartymanagement;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Calculates commission due to DCAs, records commission ledger entries,
 * and produces commission liability reports.
 *
 * Requirements: 3PM.2, SoR.5, SoR.6
 */
public interface DCACommissionService {

    /**
     * Calculates commission due for a placement for a given period.
     * Reads commission_rate_pct from dca_agency at calculation time.
     * Creates a dca_commission_entry in PENDING state.
     * Records the snapshot rate in the entry for auditability.
     *
     * Note: If tiered commission rates are confirmed by DWP (DDE-OQ-15),
     * this method signature must be extended.
     *
     * @throws CommissionCalculationException if placement has no reconciled payments in period
     */
    UUID calculateCommission(UUID placementId, LocalDate periodStart, LocalDate periodEnd, String calculatedBy);

    /**
     * Approves a commission entry (PENDING -> APPROVED).
     * Requires TEAM_LEADER or OPS_MANAGER role.
     */
    void approveCommissionEntry(UUID commissionEntryId, String approvedBy);

    /**
     * Produces a commission report for an agency for a given period.
     * Summarises recovered amounts, commission rates, and commission totals.
     * Required for finance operations and audit (SoR.6).
     */
    CommissionReport produceCommissionReport(UUID agencyId, LocalDate periodStart, LocalDate periodEnd);
}
```

```java
package uk.gov.dwp.dcms.thirdpartymanagement;

import java.util.UUID;

/**
 * Processes recall instructions: notifies the DCA via file or API,
 * waits for acknowledgement (RULING-008 guardrail 4),
 * updates placement status, and signals the process engine to
 * return the account to internal collection treatment.
 *
 * Requirements: 3PM.2, 3PM.5
 * Domain ruling: RULING-008 (guardrails 4, 5)
 */
public interface DCARecallService {

    /**
     * Initiates a recall: generates and transfers a recall instruction file
     * to the DCA. Transitions placement to RECALLED state only after
     * DCA acknowledgement is confirmed (RULING-008 guardrail 4).
     *
     * If the DCA API/FTPS is unavailable, the recall is queued and retried.
     * An operational alert is raised. The placement status remains ACTIVE or
     * PLACED — it MUST NOT be set to RECALLED until acknowledgement is received.
     *
     * @throws RecallPendingException if recall has been initiated but not yet acknowledged
     */
    void initiateRecall(UUID placementId, String reason, String initiatedBy);

    /**
     * Records the DCA recall acknowledgement. Transitions placement to RECALLED.
     * Fires the DWP-side communication suppression deactivation
     * (SUPPRESSION_REASON = DCA_PLACEMENT_INTERNAL) via command bus.
     * Signals the Flowable process engine to return account to internal treatment.
     *
     * @throws PlacementStateException if placement is not in a recallable state
     */
    void recordRecallAcknowledgement(UUID placementId, String dcaAcknowledgementRef, String receivedBy);

    /**
     * Returns the account to internal collection strategy by signalling
     * the Flowable process engine via ProcessEventPort.
     * Called internally by recordRecallAcknowledgement after suppression is lifted.
     */
    void returnToInternalTreatment(UUID placementId);
}
```

---

## 7. Flowable Integration

### 7.1 Process and sub-process ownership

DCA placement is governed by a Flowable BPMN sub-process named `DCA_PLACEMENT_PROCESS`. This is a sub-process called from the main debt account lifecycle process (`DEBT_ACCOUNT_LIFECYCLE_PROCESS`) when the strategy engine determines that DCA placement is the next treatment.

The `DCA_PLACEMENT_PROCESS` is defined in `infrastructure/process/src/main/resources/bpmn/dca-placement-process.bpmn20.xml`.

All JavaDelegate implementations are in `infrastructure/process`. Domain logic is invoked only via `DelegateCommandBus` using command types defined in `shared/process/port`. No Flowable types appear in `domain/thirdpartymanagement`.

### 7.2 BPMN structure outline

```
DCA_PLACEMENT_PROCESS (sub-process, called from DEBT_ACCOUNT_LIFECYCLE_PROCESS)
  |
  [StartEvent: PLACEMENT_INITIATED]
  |
  [ServiceTask: CHECK_ELIGIBILITY_AND_DPA]
    Delegate -> DcaEligibilityCheckDelegate
    Command   -> CheckDcaEligibilityCommand
    Outcome:  eligible=true -> continue; eligible=false -> ErrorEndEvent INELIGIBLE
  |
  [ServiceTask: DISPATCH_DISCLOSURE_NOTICE]
    Delegate -> DispatchDisclosureNoticeDelegate
    Command   -> DispatchDcaDisclosureNoticeCommand
    Action:   Calls DCAPlacementService.initiatePrePlacementNotice()
              Records pre_notice_sent_at, notice_period_expires_at
  |
  [UserTask + TimerBoundaryEvent: WAIT_FOR_NOTICE_PERIOD]
    Timer duration: ${dcaNoticePeriodDays}D   <- value from Tier 1 config; DDE-OQ-10 GATED
    Interrupting boundary events:
      - Message DISPUTE_RAISED -> [SubProcess: HANDLE_DISPUTE] -> [EndEvent: PLACEMENT_BLOCKED]
  |
  [ServiceTask: GENERATE_AND_TRANSFER_FILE]
    Delegate -> DcaFileTransferDelegate
    Command   -> GenerateAndTransferDcaFileCommand
    Action:   Calls DCAFileService.generateAndTransfer()
    On error: [BoundaryErrorEvent DC-FTPS-0003] -> [ServiceTask: RAISE_TRANSFER_FAILURE_ALERT]
                                                 -> [UserTask: AWAIT_MANUAL_RETRY_OR_ABANDON]
  |
  [TimerBoundaryEvent: AWAIT_ACKNOWLEDGEMENT_SLA]
    Timer duration: ${dcaAcknowledgementSlaDays}D   <- Tier 1 config; default 5 working days
    On timer expiry: -> [ServiceTask: RAISE_ACK_SLA_BREACH_ALERT]
                     -> [UserTask: AWAIT_MANUAL_RESOLUTION_OR_RECALL]
  |
  [ReceiveTask or Message: DCA_ACKNOWLEDGED]
    Correlation: processInstanceId or externalPlacementRef
    Action:   Calls DCAPlacementService.recordAcknowledgement()
  |
  [ServiceTask: ACTIVATE_COMMUNICATION_SUPPRESSION]
    Delegate -> ActivateDcaSuppressionDelegate
    Command   -> ActivateCommunicationSuppressionCommand (SUPPRESSION_REASON=DCA_PLACEMENT_INTERNAL)
  |
  [SubProcess: ACTIVE_PLACEMENT_MONITOR]
    Event subprocesses (non-interrupting unless specified):
      - Message DCA_PAYMENT_RECEIVED    -> [ServiceTask: RECORD_PAYMENT]
      - Message DCA_ACTIVITY_REPORTED   -> [ServiceTask: UPDATE_STATUS_ACTIVE]
      - Message RECALL_INSTRUCTED       -> [CallActivity: DCA_RECALL_SUBPROCESS] (interrupting)
      - Message DISPUTE_RAISED          -> [CallActivity: DCA_RECALL_SUBPROCESS] (interrupting)
      - Message DEBT_WRITTEN_OFF        -> [ServiceTask: CLOSE_PLACEMENT] (interrupting)
      - Message DEATH_REGISTERED        -> [ServiceTask: CLOSE_PLACEMENT] (interrupting)
      - Message INSOLVENCY_REGISTERED   -> [ServiceTask: CLOSE_PLACEMENT] (interrupting)
  |
  [EndEvent: PLACEMENT_CLOSED]

DCA_RECALL_SUBPROCESS
  |
  [ServiceTask: GENERATE_AND_TRANSFER_RECALL_FILE]
    Delegate -> DcaRecallFileDelegate
    Command   -> InitiateDcaRecallCommand
  |
  [TimerBoundaryEvent: AWAIT_RECALL_ACK_SLA]
    Timer: ${dcaRecallAcknowledgementSlaDays}D
    On expiry: alert raised; retry queued (RULING-008 guardrail 4)
  |
  [ReceiveTask: DCA_RECALL_ACKNOWLEDGED]
    Action:   Calls DCARecallService.recordRecallAcknowledgement()
              Communication suppression deactivated here (RULING-008 guardrail 5)
  |
  [ServiceTask: RETURN_TO_INTERNAL_TREATMENT]
    Delegate -> ReturnToInternalTreatmentDelegate
    Command   -> ReturnAccountToInternalTreatmentCommand
    Action:   Signals DEBT_ACCOUNT_LIFECYCLE_PROCESS to resume internal treatment lane
  |
  [EndEvent: RECALL_COMPLETE]
```

### 7.3 Timer boundary event — pre-placement notice period

The notice period timer is modelled as an **interrupting timer boundary event** on the `WAIT_FOR_NOTICE_PERIOD` user task. The timer duration expression is `${dcaNoticePeriodDays}D`, where `dcaNoticePeriodDays` is injected from the Tier 1 configuration table at process start.

**Gate:** This timer duration value is blocked on DDE-OQ-10. The process definition may be built with a placeholder value for development, but no placement may proceed to production until DWP confirms the required notice period. The variable injection must fail-fast if the configuration value is absent (throw `IllegalStateException` rather than defaulting to zero).

### 7.4 Communication suppression

- **Activated:** in the `ACTIVATE_COMMUNICATION_SUPPRESSION` service task, after the DCA acknowledges the placement. Suppression reason: `DCA_PLACEMENT_INTERNAL`.
- **Deactivated:** only in `DCA_RECALL_SUBPROCESS`, after the DCA sends a confirmed recall acknowledgement. Not deactivated on recall initiation (RULING-008 guardrail 5).
- **Implementation:** via `ActivateCommunicationSuppressionCommand` and `DeactivateCommunicationSuppressionCommand` dispatched through `DelegateCommandBus`. The handler in `domain/communications` calls `CommunicationSuppressionService`.

### 7.5 Return to internal treatment

When a recall is acknowledged, the `DCA_RECALL_SUBPROCESS` fires a Flowable signal `DCA_RECALL_COMPLETE` which is caught by the `DEBT_ACCOUNT_LIFECYCLE_PROCESS`. The main process then re-evaluates the account's segment and resumes the appropriate internal treatment lane. This models the return-to-strategy requirement (3PM.2) without tightly coupling the DCA sub-process to the strategy engine directly.

---

## 8. Pre-Placement Disclosure

### 8.1 What the DISCLOSURE_NOTICE must contain (RULING-008)

The DISCLOSURE_NOTICE is a mandatory communication dispatched before DCA placement. Per RULING-008, it must include:

1. The DCA's name, or a statement that a DCA will be appointed (if multi-agency selection has not yet occurred at notice time, the latter formulation is used).
2. The outstanding amount being referred (at the time of the notice).
3. The debtor's right to dispute the debt.

Note: The CCA 1974 Sections 86A–86F notice obligations do not apply to DWP statutory debt (RULING-008 — DWP debts are not regulated credit agreements). DWP's own disclosure policy governs. Legal interpretation of DWP policy is not within BA scope — the content specification above is taken directly from RULING-008 and must not be modified without a new domain ruling.

### 8.2 How the notice is generated

The DISCLOSURE_NOTICE is generated and dispatched by `domain/communications` in response to a `DispatchDcaDisclosureNoticeCommand` issued by the `DispatchDisclosureNoticeDelegate` in `infrastructure/process`. The communications domain selects the appropriate template and channel based on the debtor's contact preferences. The letter template must be defined in the communications template register (a separate design concern for `domain/communications`).

### 8.3 Evidence storage

Evidence of disclosure must be stored to support Subject Access Requests and audit (RULING-008):

- `dca_placement.pre_notice_sent_at` — timestamp the notice was dispatched.
- `dca_placement.notice_period_expires_at` — calculated expiry.
- `domain/audit` — an audit event `DCA_DISCLOSURE_NOTICE_SENT` with `accountId`, `placementId`, `channelUsed`, `dispatchedAt`, `templateVersion`.
- `domain/communications` — the communication record including delivery status feedback (DIC.17).

The placement service must validate that `pre_notice_sent_at` is not null and `notice_period_expires_at` has passed before calling `DCAFileService.generateAndTransfer()` (RULING-008 guardrail 1).

### 8.4 Timer gate

The Flowable timer boundary event on `WAIT_FOR_NOTICE_PERIOD` enforces the gap between notice dispatch and file transfer. The timer cannot be shortened by a user action. The only permitted interruptions during the notice period are:

- `DISPUTE_RAISED` message (interrupts and blocks placement).
- `DEBT_WRITTEN_OFF`, `DEATH_REGISTERED`, `INSOLVENCY_REGISTERED` (interrupts and closes placement).

---

## 9. Failed Handoff Handling

### 9.1 FTPS transfer failure

When the FTPS transfer of the placement file fails:

1. The `DCAFileTransferDelegate` catches the `FileTransferException` (error code `DC-FTPS-0003`).
2. The Flowable boundary error event routes the process to the failure lane.
3. `dca_placement.status` is set to `FAILED_TRANSFER`.
4. A `dca_transfer_log` entry is written with `transfer_status = FAILED` and the error code.
5. An observability alert is raised (structured log event at ERROR level, metric counter `dca.transfer.failure` incremented — STD-OPS-001).
6. An operational queue work item is created in `domain/workallocation` for an `OPS_MANAGER` to action.
7. The `OPS_MANAGER` may authorise a retry (resumes the Flowable process at the transfer step) or may close the placement (returns account to internal treatment).
8. Each retry creates a new `dca_transfer_log` entry. Retry count is bounded per STD-INT-002.

Files must not be partially transmitted. The adapter must verify the SHA-256 checksum of the received file from the DCA's server after upload where the DCA's FTPS server supports response verification.

### 9.2 DCA acknowledgement SLA breach

When the DCA does not acknowledge the placement file within the configured SLA window:

1. The `AWAIT_ACKNOWLEDGEMENT_SLA` timer fires.
2. An alert is raised and an operational work item is created.
3. The `SPECIALIST_AGENT` or `OPS_MANAGER` may:
   - Chase the DCA by out-of-band means and manually record the acknowledgement via `DCAPlacementService.recordAcknowledgement()` if confirmation is received.
   - Initiate a recall via `DCARecallService.initiateRecall()` if the DCA cannot confirm.
4. The account remains in `AWAITING_ACKNOWLEDGEMENT` state during this period. No communication suppression is activated until `PLACED` state is reached and confirmed activity commences.

### 9.3 Inbound payment file reconciliation failure

When an inbound payment file cannot be reconciled:

1. `DCAReconciliationService.ingestPaymentFile()` validates file integrity. A checksum mismatch raises `DC-FTPS-0002`; the file is rejected wholesale and logged. The DCA is notified (out-of-band or via return file, depending on the configured interface).
2. If the file passes integrity checks but individual payment records cannot be matched to a placement (no matching `external_placement_ref` or `account_id`), those records are returned as `UnreconciledPayment` objects.
3. Unreconciled payments are held in a manual reconciliation workbench queue in `domain/workallocation` for a `SPECIALIST_AGENT` or `BACKOFFICE` role to resolve.
4. Unreconciled payments are not posted to the payment domain until matched. No partial posting is permitted.
5. If a payment is unreconciled for more than a configurable number of days (Tier 1 config), an escalation alert is raised to `OPS_MANAGER`.

---

## 10. Partner Interface Contracts

### 10.1 General principle

Exact file formats are agency-specific and must be configured per agency using `dca_agency.file_format`. The field tables below define the canonical content that must be present regardless of format encoding. Format-specific serialisation details (field delimiter, record length, XML schema, JSON schema) are stored as configuration artefacts per agency and are not fixed in this domain pack.

### 10.2 Outbound placement file — canonical field set

Sent by DCMS to the DCA when a placement is initiated (after notice period elapses).

| Field | Type | Description | Mandatory | Notes |
|---|---|---|---|---|
| `dcms_placement_ref` | UUID | DCMS internal placement ID | Yes | Used for acknowledgement correlation |
| `account_reference` | String | DCMS debt account reference | Yes | Not the internal UUID; the human-readable reference |
| `customer_full_name` | String | Debtor's full name | Yes | PII — file must be PGP encrypted |
| `customer_date_of_birth` | Date (ISO 8601) | | Yes | PII |
| `customer_nino` | String | National Insurance Number | Conditional | Where held and relevant |
| `current_address_line_1` | String | | Yes | PII |
| `current_address_line_2` | String | | No | PII |
| `current_address_town` | String | | Yes | PII |
| `current_address_postcode` | String | | Yes | PII |
| `phone_primary` | String | | No | PII |
| `email_primary` | String | | No | PII |
| `outstanding_balance_pence` | Long | Balance at placement date in pence | Yes | |
| `debt_type` | String | DWP debt category code | Yes | |
| `debt_created_date` | Date (ISO 8601) | Date debt was created on DCMS | Yes | |
| `placement_date` | Date (ISO 8601) | Date of file generation | Yes | |
| `legal_basis` | String | Lawful basis for data sharing | Yes | Default: `UK_GDPR_ART6_1E` (RULING-008) |
| `disclosure_notice_sent_at` | DateTime (ISO 8601) | Timestamp of pre-placement disclosure | Yes | RULING-008 evidence |
| `dcms_agency_ref` | String | DCA's own agency code as held in DCMS | Yes | For DCA's internal routing |

Fields listed as PII must not appear unencrypted in logs, transfer log records, or file names (STD-INT-003, STD-SEC-003).

### 10.3 Inbound payment/activity file — canonical field set

Received by DCMS from the DCA (periodically, typically daily or weekly).

| Field | Type | Description | Mandatory | Notes |
|---|---|---|---|---|
| `dcms_placement_ref` | UUID | DCMS placement reference (for matching) | Conditional | Must be present unless `account_reference` is used |
| `account_reference` | String | DCMS debt account reference | Conditional | Fallback if `dcms_placement_ref` absent |
| `dca_payment_ref` | String | DCA's own payment reference | Yes | Used as idempotency key |
| `payment_amount_pence` | Long | Amount received from debtor in pence | Yes | |
| `payment_received_date` | Date (ISO 8601) | Date DCA received payment from debtor | Yes | |
| `payment_type` | String | e.g., `DIRECT_PAYMENT`, `PROMISE_FULFILLED` | No | |
| `activity_type` | String | e.g., `CONTACT_ATTEMPTED`, `NO_TRACE` | No | Activity files may omit payment fields |
| `dca_status` | String | DCA's internal status for the placement | No | For monitoring; not authoritative for DCMS state |
| `file_sequence_number` | Integer | Sequence number within a multi-part file | Conditional | Required if file is split |

### 10.4 Outbound recall instruction file

| Field | Type | Description | Mandatory |
|---|---|---|---|
| `dcms_placement_ref` | UUID | | Yes |
| `account_reference` | String | | Yes |
| `recall_date` | Date (ISO 8601) | Date recall was instructed by DWP | Yes |
| `recall_reason` | String | Coded reason (e.g., `DISPUTE`, `INSOLVENCY`, `MANAGEMENT_DECISION`) | Yes |

---

## 11. Open Questions and Gates

### DDE-OQ-10 — Required pre-placement notice period (BLOCKING)

**Question:** What is the minimum number of days between sending the pre-placement DISCLOSURE_NOTICE to the debtor and initiating DCA file transfer?

**Why it blocks:** This value is the duration of the Flowable timer boundary event on the `WAIT_FOR_NOTICE_PERIOD` user task. Without it, the timer cannot be configured, and the placement workflow cannot be completed. A zero-day or instantaneous notice period is not permissible under any reading of DWP policy.

**What gates on this answer:**
- The `dca-placement-process.bpmn20.xml` timer duration expression (`${dcaNoticePeriodDays}D`).
- The `dca_placement.notice_period_expires_at` calculation in `DCAPlacementService.initiatePrePlacementNotice()`.
- Acceptance criteria AC-03 and AC-04 (state machine timing).
- The UI display of "placement pending — notice period expires [date]" to agents.

**Owner:** DWP Client / DWP Debt Domain Expert.
**Status:** AWAITING DWP SIGN-OFF (RULING-008, open question DDE-OQ-10).

---

### DDE-OQ-14 (new) — DCA acknowledgement SLA

**Question:** What is the maximum number of working days DCMS should wait for a DCA to acknowledge a placement file before raising an SLA breach alert and escalating to OPS_MANAGER?

**Default used in design:** 5 working days (placeholder only; must be confirmed).

**Owner:** DWP Client / Delivery Lead.
**Status:** New question raised by this domain pack.

---

### DDE-OQ-15 (new) — Tiered commission rates

**Question:** Does DWP require commission rates that vary by recovery speed, debt age band, debt type, or recovered amount band? Or is a flat per-agency rate (as currently modelled in `dca_agency.commission_rate_pct`) sufficient?

**Impact:** If tiered rates are required, a `dca-commission-rate.dmn` decision table must be designed before the commission service is built, and the `dca_agency` table will need additional fields or a separate `dca_commission_rate_band` table.

**Owner:** DWP Client / OPS_MANAGER business lead.
**Status:** New question raised by this domain pack.

---

### DDE-OQ-16 (new) — DCA recall acknowledgement SLA

**Question:** What is the maximum period DCMS should wait for a DCA to acknowledge a recall instruction before escalating and moving to forced-close?

**Impact:** Timer duration on the `AWAIT_RECALL_ACK_SLA` boundary event in `DCA_RECALL_SUBPROCESS`.

**Owner:** DWP Client / Delivery Lead.
**Status:** New question raised by this domain pack.

---

### DDE-OQ-17 (new) — DISCLOSURE_NOTICE content sign-off

**Question:** Does DWP Legal approve the three mandatory content elements in section 8.1 (DCA name or appointment statement, outstanding amount, right to dispute)? Are any additional mandatory elements required under DWP internal policy?

**Impact:** Communications domain letter template design; RULING-008 revision if content changes.

**Owner:** DWP Legal / DWP Debt Domain Expert.
**Status:** New question raised by this domain pack. RULING-008 is currently `awaiting-client-sign-off`, so this is an additional sub-question within that ruling's sign-off gate.

---

### DDE-OQ-18 (new) — DCA portal / real-time API interface (I3PS.1, I3PS.4)

**Question:** I3PS.1 (real-time full lifecycle interaction with DCAs) and I3PS.4 (remote access for DCAs) are both rated `Could have`. Is there a confirmed requirement for a real-time API interface with DCAs in the current delivery scope, or is batch file exchange (FTPS) the sole interface model?

**Impact:** If a real-time DCA API is in scope, the `DCAFileService` interface must be extended with a REST adapter path, and the `dca_agency` table needs `api_endpoint` and `api_auth_type` fields.

**Owner:** DWP Client / Solution Architect.
**Status:** New question raised by this domain pack.

---

## 12. Acceptance Criteria

Requirement IDs are sourced from `Tender requirements docs/Functional-Requirements-Consolidated.md`. Domain ruling references are to `docs/project-foundation/domain-rulings/RULING-008-dca-placement-pre-placement-disclosure.md`.

---

**AC-01 — Eligibility check blocks ineligible accounts**

*Requirement IDs:* 3PM.1, 3PM.12
*Domain ruling:* RULING-008

Given an account with an active vulnerability flag,
When the Placement Rules Engine evaluates the account against the current eligibility rule,
Then the account must not be promoted to `CANDIDATE` state,
And the ineligibility reason "Account has active vulnerability flag" must be recorded,
And no `dca_placement` record is created.

Negative case: Given an account where all eligibility criteria are met (balance above minimum, treatment stages exhausted, no exclusion flags, DPA in place), the account must be promoted to `CANDIDATE` and a `dca_placement` record created.

---

**AC-02 — DPA must be confirmed before placement proceeds**

*Requirement IDs:* DIC.19, I3PS.11
*Domain ruling:* RULING-008 (guardrail 2)

Given an account that passes all other eligibility criteria,
And `dca_agency.dpa_in_place = false` for the target agency,
When `DCAPlacementService.initiateCandidate()` is called,
Then a `PlacementEligibilityException` is thrown,
And no `dca_placement` record is created,
And an audit event `DCA_PLACEMENT_BLOCKED_NO_DPA` is written.

---

**AC-03 — Notice period enforced before file transfer**

*Requirement IDs:* DW.27
*Domain ruling:* RULING-008 (guardrail 1), DDE-OQ-10 GATED

Given a placement in `PRE_PLACEMENT_NOTICE` state,
When the notice period timer has not yet elapsed,
Then any attempt to call `DCAFileService.generateAndTransfer()` directly must be rejected by the service with a `PlacementStateException`,
And the Flowable process must not advance past the `WAIT_FOR_NOTICE_PERIOD` task until the timer fires.

Note: The specific timer duration is subject to DDE-OQ-10 sign-off. This AC must be re-verified with the confirmed value before production release.

---

**AC-04 — Dispute during notice period blocks placement**

*Requirement IDs:* 3PM.2
*Domain ruling:* RULING-008 (edge case — debtor disputes after notice, before placement)

Given a placement in `PRE_PLACEMENT_NOTICE` state with the notice period timer running,
When a dispute is raised against the account (message `DISPUTE_RAISED` fired),
Then the Flowable timer boundary event must be cancelled,
And the placement must transition to `DISPUTED` state,
And no placement file must be generated or transferred,
And an audit event `DCA_PLACEMENT_BLOCKED_DISPUTE` is written.

---

**AC-05 — Placement file must include disclosure evidence and legal basis**

*Requirement IDs:* DIC.19
*Domain ruling:* RULING-008 (guardrails 1, 3)

Given a placement in `PRE_PLACEMENT_NOTICE` state where the notice period has elapsed,
When `DCAFileService.generateAndTransfer()` is called,
Then the placement data file must include `disclosure_notice_sent_at` and `legal_basis = UK_GDPR_ART6_1E`,
And `dca_placement.data_fields_shared` must be populated with the list of fields included in the file,
And `dca_placement.transmitted_at` must be set to the timestamp of successful transfer.

---

**AC-06 — Communications suppression activated only after DCA commencement**

*Requirement IDs:* 3PM.2
*Domain ruling:* RULING-008 (rule 6)

Given a placement that has transitioned to `PLACED` state (DCA acknowledged),
When the DCA reports commencement of activity (message `DCA_ACTIVITY_REPORTED` received),
Then `CommunicationSuppressionService` must activate suppression with `SUPPRESSION_REASON = DCA_PLACEMENT_INTERNAL` for the account,
And all outbound DWP communications to the debtor must be suppressed from that point.

Negative case: Communications suppression must NOT be activated at `PRE_PLACEMENT_NOTICE` or `AWAITING_ACKNOWLEDGEMENT` state.

---

**AC-07 — Recall must not set RECALLED status until DCA acknowledges**

*Requirement IDs:* 3PM.2
*Domain ruling:* RULING-008 (guardrail 4)

Given a placement in `ACTIVE` state,
When `DCARecallService.initiateRecall()` is called,
Then the recall instruction file must be generated and transferred to the DCA,
And the placement status must remain `ACTIVE` (not changed to `RECALLED`),
Until `DCARecallService.recordRecallAcknowledgement()` is called with a valid DCA acknowledgement reference,
At which point the status must transition to `RECALLED`.

Negative case: Calling `updateStatus(RECALLED)` directly without a DCA acknowledgement reference must be rejected with a `PlacementStateException`.

---

**AC-08 — Communication suppression lifted only after confirmed recall acknowledgement**

*Requirement IDs:* 3PM.2
*Domain ruling:* RULING-008 (guardrail 5)

Given a placement in `ACTIVE` state with communication suppression active,
When `DCARecallService.initiateRecall()` is called,
Then communication suppression must remain active,
Until `DCARecallService.recordRecallAcknowledgement()` is called and the placement transitions to `RECALLED`,
At which point `CommunicationSuppressionService.deactivate(SUPPRESSION_REASON = DCA_PLACEMENT_INTERNAL)` must be called.

---

**AC-09 — DCA payment receipts are reconciled before allocation**

*Requirement IDs:* 3PM.2, 3PM.13
*Standard:* STD-INT-002

Given an inbound DCA payment file passes integrity verification (checksum and PGP),
When `DCAReconciliationService.reconcilePayments()` runs,
Then each payment record matched to a `dca_placement` must have a `dca_placement_payment` record written with `reconciled = false`,
And `DCAReconciliationService.allocatePayment()` must set `reconciled = true` and `payment_domain_ref` only after the payment domain confirms the posting.

Negative case: A payment record whose `dcms_placement_ref` does not match any known placement must be placed in the manual reconciliation workbench queue, not posted.

---

**AC-10 — FTPS transfer failure sets FAILED_TRANSFER and raises alert**

*Requirement IDs:* I3PS.3
*Standard:* STD-INT-003 (error code DC-FTPS-0003)

Given a placement in `PRE_PLACEMENT_NOTICE` state where the notice period has elapsed,
When the FTPS transfer of the placement file fails after retry exhaustion,
Then the placement status must transition to `FAILED_TRANSFER`,
And a `dca_transfer_log` entry must be written with `transfer_status = FAILED` and `error_code = DC-FTPS-0003`,
And an observability alert must be raised,
And an operational work item must be created in the workallocation queue for `OPS_MANAGER`.

---

**AC-11 — Same customer cannot have two simultaneous active placements**

*Requirement IDs:* 3PM.10

Given a customer with an existing placement in `ACTIVE` state with DCA Agency A,
When `DCAPlacementService.initiateCandidate()` is called for any account belonging to the same customer with any DCA agency,
Then a `PlacementEligibilityException` must be thrown with reason "Customer already has active DCA placement",
And no new `dca_placement` record is created.

---

**AC-12 — Commission entry records rate snapshot, not live rate**

*Requirement IDs:* SoR.5, SoR.6

Given a `dca_commission_entry` is created for a placement period,
When `dca_agency.commission_rate_pct` is subsequently changed,
Then the existing `dca_commission_entry.commission_rate_pct` must not be updated,
And the commission amount on the entry must remain as calculated at the time of creation.

---

**AC-13 — Placement data shared is auditable for SAR purposes**

*Requirement IDs:* DIC.19, DIC.28
*Domain ruling:* RULING-008 (edge case — SAR during placement)

Given a debtor submits a Subject Access Request while their account is in `ACTIVE` placement with a DCA,
When DCMS constructs the SAR response,
Then the response must be able to include the `dca_placement.data_fields_shared` list, `legal_basis`, `transmitted_at`, and the identity of the DCA (`dca_agency.name`).

This AC defines the data that must be retained; the SAR fulfilment process itself is an operational procedure outside this domain pack's scope.

---

**AC-14 — FCA accreditation check before placement proceeds**

*Requirement IDs:* 3PM.2
*Domain ruling:* RULING-008 (edge case — DCA loses accreditation during placement)

Given a DCA agency where `dca_agency.fca_accreditation_valid_until` has passed (accreditation expired),
When the Placement Rules Engine evaluates accounts for that agency,
Then no accounts must be promoted to `CANDIDATE` for that agency,
And an alert must be raised for `OPS_MANAGER` to either renew the DCA's accreditation record or mark the agency as `active = false`.

Negative case: An existing `ACTIVE` placement with an agency whose accreditation has since expired must trigger an automatic recall initiation (per RULING-008 edge case: "DCA loses FCA accreditation during placement").

---

## Handoff Declaration

- **Completed:** DCA Management domain pack written, covering purpose/scope, package name recommendation, placement state machine, entity DDL, Tier 1 config table, DMN descriptions, Java service interfaces, Flowable integration, pre-placement disclosure rules, failed handoff handling, partner interface contracts, open questions, and 14 acceptance criteria.
- **Files changed:** `docs/project-foundation/domain-packs/DCA-management-domain-pack.md` (created).
- **Requirement IDs covered:** 3PM.1–3PM.16, DW.22, DW.27, DIC.19, DIC.28, I3PS.1, I3PS.3, I3PS.4, I3PS.11, SoR.5, SoR.6 — all sourced to `Tender requirements docs/Functional-Requirements-Consolidated.md`.
- **Domain rulings used:** RULING-008 (DCA Placement — Pre-Placement Disclosure Obligations) — sourced from `docs/project-foundation/domain-rulings/RULING-008-dca-placement-pre-placement-disclosure.md`.
- **ACs satisfied:** AC-01 through AC-14 defined and testable from current inputs, except AC-03 (notice period timer duration blocked on DDE-OQ-10).
- **ACs not satisfied:** AC-03 timer duration value cannot be finalised until DDE-OQ-10 is answered by DWP. The AC structure is complete; only the specific day count is missing.
- **Assumptions made:**
  - `domain/thirdpartymanagement` is confirmed as the package name (consistent with MASTER-DESIGN-DOCUMENT.md).
  - Commission rates are flat per-agency in the current model (DDE-OQ-15 raises whether tiered rates are needed).
  - Acknowledgement SLA default of 5 working days is a placeholder pending DDE-OQ-14.
  - Recall acknowledgement SLA is unspecified pending DDE-OQ-16.
  - Real-time DCA API (I3PS.1, I3PS.4) is treated as out of scope (`Could have`) pending DDE-OQ-18.
- **Open questions:** DDE-OQ-10 (DWP Client), DDE-OQ-14 (DWP Client / Delivery Lead), DDE-OQ-15 (DWP Client / OPS_MANAGER lead), DDE-OQ-16 (DWP Client / Delivery Lead), DDE-OQ-17 (DWP Legal / DWP Debt Domain Expert), DDE-OQ-18 (DWP Client / Solution Architect).
- **Next role:** Delivery Designer.
- **What they need:** This domain pack; RULING-008; MASTER-DESIGN-DOCUMENT.md section 4 (module ownership); ARCHITECTURE-BLUEPRINT.md sections 4–7 (interaction model, process infrastructure); STD-INT-003 (file transfer); STD-INT-002 (reliability/reconciliation); ADR-002 (monitoring event subprocess pattern). DDE-OQ-10 must be answered before Flowable BPMN timer is wired. All other open questions should be chased in parallel.
