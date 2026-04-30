# Communications & Multi-Channel Contact Orchestration Domain Pack

**Domain:** Communications & Multi-Channel Contact Orchestration
**Package:** `domain/communications`
**Author:** Business Analyst Agent
**Date:** 2026-04-27
**Status:** Phase 3 complete (design baseline locked) - open policy answers tracked as provisional defaults under DDE-OQ-COMMS-01/02/03/04/05
**Domain rulings in scope:** RULING-001 (breathing space communication suppression), RULING-011 (queued communication disposition on suppression lift)
**Requirement IDs covered:** CC.1, CC.4, CC.8, CC.11, CC.15, CC.19, CC.21, CC.23, CC.24, CC.27, CC.30, CC.31, CC.32, CC.33, CC.34, CC.35, DIC.3, DIC.8, DIC.14, DIC.15, DIC.25, DIC.27, DIC.29, DW.33, DW.34, DW.43, DW.51, DW.53, DW.55, DW.59, DW.65, DW.66, DW.67, DW.68, DW.69, DW.72, DW.73, UI.2, AAD.16, CAS.COM-07, CAS.COM-08

---

## 1. Domain Ownership

### 1.1 What this domain owns

`domain/communications` owns:

- **Outbound communication orchestration** — receiving communication instructions (from Flowable process delegates or from agent-initiated actions), routing them through suppression evaluation, selecting the correct channel, and instructing the transport layer to dispatch.
- **Communication event log** — the master audit record of every outbound and inbound communication attempt, including status, template used, and outcome. This is the source of truth for the contact history timeline shown to agents.
- **Suppression decision log** — a dedicated log of every suppression evaluation performed by `CommunicationSuppressionService`. This is distinct from the `suppression_log` table (which records active suppressions by account) and from the `vulnerability_suppression_log` table (owned by `domain/customer`). The suppression decision log records the evaluation event and its outcome for audit purposes.
- **Contact preferences** — customer-level channel preferences, verified status flags, time-of-contact windows, SMS consent, and Do Not Contact (DNC) flags. These are managed by agents and referenced by suppression and channel-selection logic.
- **Template library** — communication templates (letter, SMS, email, in-app) including lifecycle management (DRAFT, PENDING_APPROVAL, APPROVED, RETIRED), variable declarations, approval workflow, and rendered-content production.
- **Self-service in-app communication instructions** - suppression-checked in-app message requests and portal/app engagement signals received through `domain/integration`; the DWP strategic portal/app owns customer-facing display and delivery mechanics.
- **Contact frequency tracking** — rolling contact counts per customer per channel, used by `CommunicationSuppressionService` to enforce configurable frequency limits.
- **Returned mail handling** — intake records for returned letters and invalid contact details, resolution workflow, and address validity flagging.
- **Inbound contact logging** — structured recording of inbound calls, inbound letters, and agent notes against a customer or account.

### 1.2 What this domain reads from other domains

| Source domain | What is read | Purpose |
|---|---|---|
| `domain/customer` | Active vulnerability flags (boolean indicator only — no category detail), breathing space period status (active/inactive, type), accessibility needs (communication restrictions JSONB) | `CommunicationSuppressionService` evaluates these before every dispatch |
| `domain/account` | Account reference data (account number, balance, debt type, debt status) | Template variable substitution for rendered content |
| `domain/customer` | Contact details (address, phone, email) | Dispatch address resolution for channel adapters |
| `domain/workallocation` | Queue identifiers (for returned mail work item creation) | Returned mail creates a BACKOFFICE queue work item |

This domain does not write to any table in `domain/customer`, `domain/account`, or `domain/workallocation` directly. Cross-domain interaction is via service interfaces and port interfaces only — no direct cross-package repository calls.

### 1.3 Relationship with `domain/integration`

`domain/communications` owns orchestration and content. `domain/integration` owns the external gateway adapters.

| Concern | Owner |
|---|---|
| Decide whether to send a communication | `domain/communications` (`CommunicationSuppressionService`) |
| Select the channel and template | `domain/communications` (`ContactOrchestrationService`) |
| Render the template with resolved variables | `domain/communications` (`TemplateManagementService`) |
| Record the communication_event attempt | `domain/communications` |
| Transport the rendered content to the external gateway | `domain/integration` (SMTP gateway adapter, SMS gateway adapter, print/letter queue adapter) |
| Transport in-app message instructions to the DWP strategic portal/app | `domain/integration` (self-service portal/app adapter) |
| Handle gateway delivery receipts and bounce-backs | `domain/integration` (feeds back to `domain/communications` via the outcome callback) |
| Receive portal/app engagement events for communication strategy | `domain/integration` (feeds accepted facts to `domain/communications`) |

`domain/communications` calls `domain/integration` adapters via interfaces defined in `domain/integration`. `domain/integration` never calls back into `domain/communications` domain logic — it fires outcome events via `ProcessEventPort` or a dedicated callback interface, which `domain/communications` handles to update the `communication_event` status.

---

## 2. Core Data Model

All tables reside in the default PostgreSQL schema. Flyway migration numbering uses the next available V-number in the project sequence. A placeholder prefix `V{n}__` is used throughout. The exact migration number must be assigned by the Builder following the project numbering convention (ADR-010).

### 2.1 `communication_event` — master record of every communication attempt

```sql
-- V{n}__create_communication_event.sql
CREATE TABLE communication_event (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    customer_id             UUID            NOT NULL,
    account_id              UUID,           -- nullable: some comms are customer-level (e.g., accessibility ack)
    event_type              VARCHAR(30)     NOT NULL,
    channel                 VARCHAR(20)     NOT NULL,
    direction               VARCHAR(10)     NOT NULL,
    template_id             UUID,           -- FK to communication_template; nullable for inbound/agent notes
    status                  VARCHAR(25)     NOT NULL,
    communication_category  VARCHAR(25)     NOT NULL,
    initiated_by            VARCHAR(255)    NOT NULL, -- agent UUID or 'SYSTEM'
    flowable_instance_id    VARCHAR(255),   -- nullable; populated when initiated by a Flowable process
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),
    dispatched_at           TIMESTAMPTZ,
    outcome_at              TIMESTAMPTZ,
    outcome_code            VARCHAR(50),    -- e.g. DELIVERED, BOUNCED, RETURNED, CALL_ANSWERED
    reference_id            VARCHAR(100),   -- unique reference per communication (DW.72)

    CONSTRAINT pk_communication_event PRIMARY KEY (id),
    CONSTRAINT fk_ce_template
        FOREIGN KEY (template_id)
        REFERENCES communication_template (id),
    CONSTRAINT chk_ce_event_type
        CHECK (event_type IN (
            'OUTBOUND_LETTER',
            'OUTBOUND_SMS',
            'OUTBOUND_EMAIL',
            'OUTBOUND_IN_APP',
            'INBOUND_CALL',
            'INBOUND_LETTER',
            'INBOUND_EMAIL',
            'AGENT_NOTE'
        )),
    CONSTRAINT chk_ce_channel
        CHECK (channel IN ('LETTER', 'SMS', 'EMAIL', 'IN_APP', 'PHONE', 'AGENT_NOTE')),
    CONSTRAINT chk_ce_direction
        CHECK (direction IN ('OUTBOUND', 'INBOUND')),
    CONSTRAINT chk_ce_status
        CHECK (status IN (
            'PENDING',
            'DISPATCHED',
            'DELIVERED',
            'FAILED',
            'SUPPRESSED',
            'RETURNED',
            'HELD_FOR_REVIEW',
            'DISCARDED_STATUTORY'
        )),
    CONSTRAINT chk_ce_communication_category
        CHECK (communication_category IN (
            'DEBT_COLLECTION',
            'NON_COLLECTION',
            'DUAL_USE',
            'ESTATE_ADMINISTRATION'
        )),
    CONSTRAINT chk_ce_dispatched_requires_dispatched_at
        CHECK (
            status NOT IN ('DISPATCHED', 'DELIVERED', 'FAILED', 'RETURNED')
            OR dispatched_at IS NOT NULL
        )
);

COMMENT ON COLUMN communication_event.status IS
    'Immutable after DELIVERED, FAILED, DISCARDED_STATUTORY, or RETURNED. outcome_at and outcome_code are updated via the integration outcome callback — they are the only mutable fields after initial creation.';
COMMENT ON COLUMN communication_event.reference_id IS
    'Unique human-readable reference per communication, e.g. OD51-20260427-A1B2. Required by DW.72. Populated at creation time.';
COMMENT ON COLUMN communication_event.communication_category IS
    'Used by CommunicationSuppressionService to apply category-specific rules (RULING-011 guardrail 4). Must match the CommunicationCategory enum.';
COMMENT ON COLUMN communication_event.flowable_instance_id IS
    'When populated, this event was initiated by a Flowable delegate. Used to signal Flowable with the dispatch outcome via ProcessEventPort.';
COMMENT ON COLUMN communication_event.initiated_by IS
    'UUID of the agent who initiated the communication, or the literal string ''SYSTEM'' for automated dispatch via Flowable or batch process.';

CREATE INDEX idx_ce_customer_id ON communication_event (customer_id);
CREATE INDEX idx_ce_account_id ON communication_event (account_id) WHERE account_id IS NOT NULL;
CREATE INDEX idx_ce_created_at ON communication_event (created_at);
CREATE INDEX idx_ce_status ON communication_event (status);
CREATE INDEX idx_ce_flowable_instance_id ON communication_event (flowable_instance_id) WHERE flowable_instance_id IS NOT NULL;
CREATE UNIQUE INDEX uix_ce_reference_id ON communication_event (reference_id) WHERE reference_id IS NOT NULL;
```

### 2.2 `communication_suppression_decision` — log every suppression evaluation

This table records every evaluation made by `CommunicationSuppressionService`, whether suppressed or permitted. It is the regulatory evidence log for suppression decisions. It is distinct from `suppression_log` (which records active suppression states per account) and from `vulnerability_suppression_log` (owned by `domain/customer`).

```sql
-- V{n}__create_communication_suppression_decision.sql
CREATE TABLE communication_suppression_decision (
    id                          UUID            NOT NULL DEFAULT gen_random_uuid(),
    customer_id                 UUID            NOT NULL,
    account_id                  UUID,
    evaluated_at                TIMESTAMPTZ     NOT NULL DEFAULT now(),
    channel                     VARCHAR(20)     NOT NULL,
    communication_category      VARCHAR(25)     NOT NULL,
    suppressed                  BOOLEAN         NOT NULL,
    suppression_reasons         JSONB           NOT NULL DEFAULT '[]'::jsonb,
    primary_reason_code         VARCHAR(50),    -- null when suppressed = false
    evaluated_by                VARCHAR(50)     NOT NULL DEFAULT 'SYSTEM',
    communication_event_id      UUID,           -- FK nullable; may not yet have an event at evaluation time

    CONSTRAINT pk_communication_suppression_decision PRIMARY KEY (id),
    CONSTRAINT fk_csd_communication_event
        FOREIGN KEY (communication_event_id)
        REFERENCES communication_event (id),
    CONSTRAINT chk_csd_channel
        CHECK (channel IN ('LETTER', 'SMS', 'EMAIL', 'IN_APP', 'PHONE')),
    CONSTRAINT chk_csd_category
        CHECK (communication_category IN (
            'DEBT_COLLECTION',
            'NON_COLLECTION',
            'DUAL_USE',
            'ESTATE_ADMINISTRATION'
        )),
    CONSTRAINT chk_csd_primary_reason_when_suppressed
        CHECK (
            (suppressed = false)
            OR (suppressed = true AND primary_reason_code IS NOT NULL)
        )
);

COMMENT ON COLUMN communication_suppression_decision.suppression_reasons IS
    'JSONB array of all reason codes that apply, even when only the primary reason drives the suppression outcome. Example: ["MHC_BREATHING_SPACE", "FREQUENCY_LIMIT_REACHED"]. All reasons are logged even when only the first applies.';
COMMENT ON COLUMN communication_suppression_decision.communication_event_id IS
    'Nullable: a suppression evaluation may occur before a communication_event record exists (e.g., pre-flight check in a Flowable delegate before the event record is created). Populated on link after event creation.';
COMMENT ON TABLE communication_suppression_decision IS
    'Append-only regulatory evidence log. No UPDATE or DELETE permitted. Every call to CommunicationSuppressionService.evaluateAndLog() must produce exactly one row in this table.';

CREATE INDEX idx_csd_customer_id ON communication_suppression_decision (customer_id);
CREATE INDEX idx_csd_account_id ON communication_suppression_decision (account_id) WHERE account_id IS NOT NULL;
CREATE INDEX idx_csd_evaluated_at ON communication_suppression_decision (evaluated_at);
CREATE INDEX idx_csd_suppressed ON communication_suppression_decision (suppressed);
CREATE INDEX idx_csd_primary_reason ON communication_suppression_decision (primary_reason_code) WHERE primary_reason_code IS NOT NULL;
```

### 2.3 `contact_preference` — customer-level channel preferences

```sql
-- V{n}__create_contact_preference.sql
CREATE TABLE contact_preference (
    customer_id                     UUID            NOT NULL,
    preferred_channel               VARCHAR(20)     NOT NULL,
    email_verified                  BOOLEAN         NOT NULL DEFAULT false,
    phone_verified                  BOOLEAN         NOT NULL DEFAULT false,
    sms_consent                     BOOLEAN         NOT NULL DEFAULT false,
    letter_address_valid            BOOLEAN         NOT NULL DEFAULT true,
    preferred_contact_window_start  TIME,
    preferred_contact_window_end    TIME,
    do_not_contact                  BOOLEAN         NOT NULL DEFAULT false,
    dnc_reason                      VARCHAR(100),
    dnc_set_by                      VARCHAR(255),   -- agent UUID or 'SYSTEM'
    dnc_set_at                      TIMESTAMPTZ,
    updated_at                      TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT pk_contact_preference PRIMARY KEY (customer_id),
    CONSTRAINT chk_cp_preferred_channel
        CHECK (preferred_channel IN ('LETTER', 'SMS', 'EMAIL', 'IN_APP', 'PHONE')),
    CONSTRAINT chk_cp_contact_window
        CHECK (
            (preferred_contact_window_start IS NULL AND preferred_contact_window_end IS NULL)
            OR (preferred_contact_window_start IS NOT NULL AND preferred_contact_window_end IS NOT NULL
                AND preferred_contact_window_end > preferred_contact_window_start)
        ),
    CONSTRAINT chk_cp_dnc_fields
        CHECK (
            (do_not_contact = false)
            OR (do_not_contact = true AND dnc_reason IS NOT NULL AND dnc_set_by IS NOT NULL AND dnc_set_at IS NOT NULL)
        )
);

COMMENT ON COLUMN contact_preference.preferred_contact_window_start IS
    'Time of day (no date) from which the customer prefers to be contacted. Evaluated by CommunicationSuppressionService step 7. NULL means no preference restriction. DIC.14.';
COMMENT ON COLUMN contact_preference.sms_consent IS
    'Explicit consent for SMS marketing/collection contact. Must be true before any SMS outbound is permitted. Distinct from phone_verified.';
COMMENT ON COLUMN contact_preference.do_not_contact IS
    'DNC flag set by agent or system. When true, CommunicationSuppressionService returns SUPPRESS with reason DNC_ACTIVE for all DEBT_COLLECTION and NON_COLLECTION categories. AAD.16.';
COMMENT ON COLUMN contact_preference.letter_address_valid IS
    'Set to false when a letter is returned (returned_mail_record) and no valid address can be confirmed. Triggers suppression of LETTER channel.';

CREATE INDEX idx_cp_do_not_contact ON contact_preference (do_not_contact) WHERE do_not_contact = true;
```

### 2.4 `communication_template` — template library

```sql
-- V{n}__create_communication_template.sql
CREATE TABLE communication_template (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    template_code           VARCHAR(50)     NOT NULL,
    name                    VARCHAR(200)    NOT NULL,
    channel                 VARCHAR(20)     NOT NULL,
    status                  VARCHAR(25)     NOT NULL,
    body_template           TEXT            NOT NULL,
    subject_template        TEXT,           -- populated for EMAIL channel only
    max_character_count     INTEGER,        -- populated for SMS channel; enforced at render time
    communication_category  VARCHAR(25)     NOT NULL,
    version                 INTEGER         NOT NULL DEFAULT 1,
    approved_by             VARCHAR(255),   -- agent UUID of approver
    approved_at             TIMESTAMPTZ,
    retired_at              TIMESTAMPTZ,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT pk_communication_template PRIMARY KEY (id),
    CONSTRAINT uix_ct_code_version UNIQUE (template_code, version),
    CONSTRAINT chk_ct_channel
        CHECK (channel IN ('LETTER', 'SMS', 'EMAIL', 'IN_APP')),
    CONSTRAINT chk_ct_status
        CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'RETIRED')),
    CONSTRAINT chk_ct_category
        CHECK (communication_category IN (
            'DEBT_COLLECTION',
            'NON_COLLECTION',
            'DUAL_USE',
            'ESTATE_ADMINISTRATION'
        )),
    CONSTRAINT chk_ct_approved_fields
        CHECK (
            (status NOT IN ('APPROVED', 'RETIRED'))
            OR (approved_by IS NOT NULL AND approved_at IS NOT NULL)
        ),
    CONSTRAINT chk_ct_retired_fields
        CHECK (
            status != 'RETIRED'
            OR retired_at IS NOT NULL
        ),
    CONSTRAINT chk_ct_sms_character_count
        CHECK (
            channel != 'SMS'
            OR max_character_count IS NOT NULL
        ),
    CONSTRAINT chk_ct_email_subject
        CHECK (
            channel != 'EMAIL'
            OR subject_template IS NOT NULL
        ),
    CONSTRAINT chk_ct_version_positive
        CHECK (version >= 1)
);

COMMENT ON COLUMN communication_template.body_template IS
    'Template body with {{variable_name}} placeholders. Variables must be declared in communication_template_variable. DW.73.';
COMMENT ON COLUMN communication_template.template_code IS
    'Business-facing reference code, e.g. OD51, ARREARS_NOTICE_01. Used in SendCommunicationCommand and in DW.72 reference ID generation.';
COMMENT ON COLUMN communication_template.communication_category IS
    'Category of the resulting communication. Used by CommunicationSuppressionService to apply suppression rules. Must match the category expected by the calling process.';

CREATE INDEX idx_ct_template_code ON communication_template (template_code);
CREATE INDEX idx_ct_status ON communication_template (status);
CREATE INDEX idx_ct_channel ON communication_template (channel);
```

### 2.5 `communication_template_variable` — declared variables per template

```sql
-- V{n}__create_communication_template_variable.sql
CREATE TABLE communication_template_variable (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    template_id     UUID            NOT NULL,
    variable_name   VARCHAR(100)    NOT NULL,
    data_type       VARCHAR(20)     NOT NULL,
    is_mandatory    BOOLEAN         NOT NULL DEFAULT true,
    description     TEXT,

    CONSTRAINT pk_communication_template_variable PRIMARY KEY (id),
    CONSTRAINT fk_ctv_template
        FOREIGN KEY (template_id)
        REFERENCES communication_template (id),
    CONSTRAINT uix_ctv_template_variable UNIQUE (template_id, variable_name),
    CONSTRAINT chk_ctv_data_type
        CHECK (data_type IN ('STRING', 'DATE', 'AMOUNT', 'ACCOUNT_NUMBER', 'URL', 'INTEGER'))
);

COMMENT ON COLUMN communication_template_variable.variable_name IS
    'Must match the {{variable_name}} placeholder used in communication_template.body_template. TemplateManagementService validates at render time that all mandatory variables are provided.';

CREATE INDEX idx_ctv_template_id ON communication_template_variable (template_id);
```

### 2.6 `contact_frequency_record` — rolling contact count per customer per channel

```sql
-- V{n}__create_contact_frequency_record.sql
CREATE TABLE contact_frequency_record (
    customer_id         UUID            NOT NULL,
    channel             VARCHAR(20)     NOT NULL,
    window_start        TIMESTAMPTZ     NOT NULL,
    window_end          TIMESTAMPTZ     NOT NULL,
    contact_count       INTEGER         NOT NULL DEFAULT 0,
    last_updated_at     TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT pk_contact_frequency_record PRIMARY KEY (customer_id, channel, window_start),
    CONSTRAINT chk_cfr_channel
        CHECK (channel IN ('LETTER', 'SMS', 'EMAIL', 'IN_APP', 'PHONE')),
    CONSTRAINT chk_cfr_window
        CHECK (window_end > window_start),
    CONSTRAINT chk_cfr_count_non_negative
        CHECK (contact_count >= 0)
);

COMMENT ON TABLE contact_frequency_record IS
    'Rolling contact count used by CommunicationSuppressionService step 6. Window definition (sliding vs calendar) is a DWP policy question — see DDE-OQ-COMMS-02 (BLOCKING). Window boundaries are set by the contact frequency job and by ContactOrchestrationService on each successful dispatch.';
COMMENT ON COLUMN contact_frequency_record.window_start IS
    'Start of the rolling or calendar window. Definition pending DDE-OQ-COMMS-02.';

CREATE INDEX idx_cfr_customer_id ON contact_frequency_record (customer_id);
CREATE INDEX idx_cfr_channel ON contact_frequency_record (channel);
CREATE INDEX idx_cfr_window_end ON contact_frequency_record (window_end);
```

### 2.7 `returned_mail_record` — returned letters and invalid contact handling

```sql
-- V{n}__create_returned_mail_record.sql
CREATE TABLE returned_mail_record (
    id                          UUID            NOT NULL DEFAULT gen_random_uuid(),
    customer_id                 UUID            NOT NULL,
    channel                     VARCHAR(20)     NOT NULL,
    address_or_number_attempted TEXT            NOT NULL,
    communication_event_id      UUID,           -- FK nullable; return may be received without matched event
    returned_at                 TIMESTAMPTZ     NOT NULL,
    return_reason               VARCHAR(50)     NOT NULL,
    handled_by                  VARCHAR(255),   -- agent UUID
    resolution                  VARCHAR(25)     NOT NULL DEFAULT 'PENDING',
    resolved_at                 TIMESTAMPTZ,
    work_item_id                UUID,           -- reference to domain/workallocation work item

    CONSTRAINT pk_returned_mail_record PRIMARY KEY (id),
    CONSTRAINT fk_rmr_communication_event
        FOREIGN KEY (communication_event_id)
        REFERENCES communication_event (id),
    CONSTRAINT chk_rmr_channel
        CHECK (channel IN ('LETTER', 'SMS', 'EMAIL', 'PHONE')),
    CONSTRAINT chk_rmr_return_reason
        CHECK (return_reason IN (
            'ADDRESS_NOT_FOUND',
            'ADDRESSEE_GONE_AWAY',
            'REFUSED',
            'UNABLE_TO_FORWARD',
            'INVALID_NUMBER',
            'EMAIL_BOUNCED',
            'OTHER'
        )),
    CONSTRAINT chk_rmr_resolution
        CHECK (resolution IN ('PENDING', 'UPDATED', 'CONFIRMED_INVALID')),
    CONSTRAINT chk_rmr_resolved_fields
        CHECK (
            resolution = 'PENDING'
            OR (resolution != 'PENDING' AND resolved_at IS NOT NULL AND handled_by IS NOT NULL)
        )
);

COMMENT ON COLUMN returned_mail_record.resolution IS
    'UPDATED: valid new contact detail found and saved to customer record. CONFIRMED_INVALID: contact detail confirmed as invalid; contact_preference.letter_address_valid (or equivalent) set to false. PENDING: awaiting BACKOFFICE resolution.';
COMMENT ON COLUMN returned_mail_record.work_item_id IS
    'Reference to the work item created in domain/workallocation for BACKOFFICE resolution. Not a FK constraint (cross-domain); stored as opaque reference only.';

CREATE INDEX idx_rmr_customer_id ON returned_mail_record (customer_id);
CREATE INDEX idx_rmr_resolution ON returned_mail_record (resolution);
CREATE INDEX idx_rmr_returned_at ON returned_mail_record (returned_at);
```

---

## 3. Tier 1 Foundations Configuration Tables

### 3.1 `channel_suppression_rule` — configurable channel-level suppression settings

```sql
-- V{n}__create_channel_suppression_rule.sql
CREATE TABLE channel_suppression_rule (
    id                          UUID            NOT NULL DEFAULT gen_random_uuid(),
    channel                     VARCHAR(20)     NOT NULL,
    max_contacts_per_window     INTEGER         NOT NULL,
    window_days                 INTEGER         NOT NULL,
    contact_window_start        TIME,           -- earliest permitted contact time for this channel
    contact_window_end          TIME,           -- latest permitted contact time for this channel
    channel_kill_switch         BOOLEAN         NOT NULL DEFAULT false, -- disables all outbound on this channel
    effective_from              DATE            NOT NULL,
    effective_to                DATE,
    approved_by                 VARCHAR(255),
    approved_at                 TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT pk_channel_suppression_rule PRIMARY KEY (id),
    CONSTRAINT uix_csr_channel_effective UNIQUE (channel, effective_from),
    CONSTRAINT chk_csr_channel
        CHECK (channel IN ('LETTER', 'SMS', 'EMAIL', 'IN_APP', 'PHONE')),
    CONSTRAINT chk_csr_max_contacts
        CHECK (max_contacts_per_window > 0),
    CONSTRAINT chk_csr_window_days
        CHECK (window_days > 0),
    CONSTRAINT chk_csr_contact_window
        CHECK (
            (contact_window_start IS NULL AND contact_window_end IS NULL)
            OR (contact_window_start IS NOT NULL AND contact_window_end IS NOT NULL
                AND contact_window_end > contact_window_start)
        ),
    CONSTRAINT chk_csr_effective_dates
        CHECK (effective_to IS NULL OR effective_to > effective_from)
);

COMMENT ON COLUMN channel_suppression_rule.channel_kill_switch IS
    'When true, CommunicationSuppressionService returns SUPPRESS with reason CHANNEL_UNAVAILABLE for all outbound on this channel. Used for planned maintenance windows or regulatory holds on a specific channel.';
COMMENT ON COLUMN channel_suppression_rule.max_contacts_per_window IS
    'Maximum number of contacts permitted per rolling window (window_days). E.g. 3 SMS per 7 days. CC.27.';
COMMENT ON TABLE channel_suppression_rule IS
    'Tier 1 Foundations configuration. Changes require BACKOFFICE edit + OPS_MANAGER approval. Effective-dated — changes do not affect in-flight communications initiated before the effective_from date.';

CREATE INDEX idx_csr_channel ON channel_suppression_rule (channel);
CREATE INDEX idx_csr_effective_from ON channel_suppression_rule (effective_from);
```

### 3.2 `communication_suppression_threshold` — frequency enforcement thresholds

This table provides the configurable parameters consumed by `CommunicationSuppressionService` step 6. It is a secondary configuration table supporting `channel_suppression_rule` and exists for use cases where different thresholds may apply across communication categories.

```sql
-- V{n}__create_communication_suppression_threshold.sql
CREATE TABLE communication_suppression_threshold (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid(),
    channel                 VARCHAR(20)     NOT NULL,
    communication_category  VARCHAR(25)     NOT NULL,
    window_days             INTEGER         NOT NULL,
    max_contacts            INTEGER         NOT NULL,
    effective_from          DATE            NOT NULL,
    effective_to            DATE,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT pk_communication_suppression_threshold PRIMARY KEY (id),
    CONSTRAINT uix_cst_channel_category_effective UNIQUE (channel, communication_category, effective_from),
    CONSTRAINT chk_cst_channel
        CHECK (channel IN ('LETTER', 'SMS', 'EMAIL', 'IN_APP', 'PHONE')),
    CONSTRAINT chk_cst_category
        CHECK (communication_category IN (
            'DEBT_COLLECTION',
            'NON_COLLECTION',
            'DUAL_USE',
            'ESTATE_ADMINISTRATION'
        )),
    CONSTRAINT chk_cst_window_days
        CHECK (window_days > 0),
    CONSTRAINT chk_cst_max_contacts
        CHECK (max_contacts > 0),
    CONSTRAINT chk_cst_effective_dates
        CHECK (effective_to IS NULL OR effective_to > effective_from)
);

COMMENT ON TABLE communication_suppression_threshold IS
    'Category-specific frequency thresholds. When a row exists for a channel+category combination, it takes precedence over the channel-level max_contacts_per_window in channel_suppression_rule. If absent, the channel-level rule applies. CC.27.';
```

---

## 4. Tier 2 DMN Tables

### 4.1 `channel-selection.dmn` — determines the outbound channel for a communication event

**Decision table ID:** `channel-selection`
**Hit policy:** FIRST — first matching rule fires; if no rule matches, the delegate must throw `NoChannelAvailableException`.
**Deployed to:** Flowable DMN engine via `domain/infrastructure/process`

**Business context:** This DMN is evaluated by `ContactOrchestrationService` when the `SendCommunicationCommand` has a null `channel` (preference-based selection). When a channel is explicitly specified in the command, this DMN is not evaluated.

| # | Input: `preferredChannel` | Input: `channelVerified` | Input: `vulnerabilityActive` | Input: `contactWindowActive` | Input: `channelAvailable` | Output: `selectedChannel` | Output: `fallbackChannel` | Output: `suppressIfNoFallback` |
|---|---|---|---|---|---|---|---|---|
| 1 | `EMAIL` | `true` | `false` | `true` | `true` | `EMAIL` | `LETTER` | `false` |
| 2 | `EMAIL` | `true` | `false` | `false` | `true` | `EMAIL` | `LETTER` | `false` |
| 3 | `EMAIL` | `false` | - | - | - | `LETTER` | - | `false` |
| 4 | `SMS` | `true` | `false` | `true` | `true` | `SMS` | `LETTER` | `false` |
| 5 | `SMS` | `false` | - | - | - | `LETTER` | - | `false` |
| 6 | `LETTER` | `true` | - | - | `true` | `LETTER` | - | `true` |
| 7 | `LETTER` | `false` | - | - | - | - | - | `true` |
| 8 | `EMAIL` | `true` | `true` | - | - | `LETTER` | - | `false` |
| 9 | `SMS` | `true` | `true` | - | - | `LETTER` | - | `false` |
| 10 | - | - | - | - | `false` | - | - | `true` |

**Notes:**
- `channelVerified` = `true` means the relevant field in `contact_preference` is true (e.g., `email_verified`, `sms_consent`).
- `vulnerabilityActive` = `true` means an active vulnerability flag exists with an accessibility need restricting the preferred channel. `CommunicationSuppressionService` handles statutory suppression separately — this DMN handles channel selection only.
- `contactWindowActive` = `true` means the current time is within the customer's `preferred_contact_window_start` / `preferred_contact_window_end`. A false value for digital channels does not necessarily suppress — it is advisory for scheduling.
- `channelAvailable` = `false` means `channel_suppression_rule.channel_kill_switch = true` for this channel in the active configuration.
- When `suppressIfNoFallback = true` and no fallback channel is available (or the fallback is also unavailable), `ContactOrchestrationService` must call `CommunicationSuppressionService.evaluateAndLog()` with reason `CHANNEL_UNAVAILABLE` and not dispatch.
- **BLOCKING — DDE-OQ-COMMS-03:** The fallback channel order (e.g., SMS → LETTER vs EMAIL → SMS) is a DWP policy decision. The table above uses LETTER as the universal fallback. This must be confirmed by DWP before production deployment of this DMN.

### 4.2 `suppression-reason-priority.dmn` — determines the primary suppression reason code for audit

**Decision table ID:** `suppression-reason-priority`
**Hit policy:** FIRST — statutory reasons always precede non-statutory. MHC overrides all.
**Deployed to:** Flowable DMN engine. Also callable directly by `CommunicationSuppressionService` for primary reason code assignment in the suppression decision log.

**Business context:** When multiple suppression reasons are active simultaneously, this DMN assigns the `primary_reason_code` for the `communication_suppression_decision` log entry. All applicable reasons are always recorded in the `suppression_reasons` JSONB array regardless of priority.

| # | Input: `mhcBreathingSpaceActive` | Input: `breathingSpaceActive` | Input: `vulnerabilitySuppressionActive` | Input: `dncActive` | Input: `frequencyLimitReached` | Input: `outOfContactWindow` | Input: `channelUnverified` | Input: `channelUnavailable` | Output: `primaryReasonCode` |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `true` | - | - | - | - | - | - | - | `MHC_BREATHING_SPACE_STATUTORY` |
| 2 | `false` | `true` | - | - | - | - | - | - | `BREATHING_SPACE_STATUTORY` |
| 3 | `false` | `false` | `true` | - | - | - | - | - | `VULNERABILITY_POLICY` |
| 4 | `false` | `false` | `false` | `true` | - | - | - | - | `DNC_ACTIVE` |
| 5 | `false` | `false` | `false` | `false` | - | - | `true` | - | `CHANNEL_UNVERIFIED` |
| 6 | `false` | `false` | `false` | `false` | `true` | - | `false` | - | `FREQUENCY_LIMIT_REACHED` |
| 7 | `false` | `false` | `false` | `false` | `false` | `true` | `false` | - | `OUT_OF_CONTACT_WINDOW` |
| 8 | `false` | `false` | `false` | `false` | `false` | `false` | `false` | `true` | `CHANNEL_UNAVAILABLE` |

**Notes:**
- MHC breathing space overrides all other reasons. This is a statutory obligation (RULING-001) — it is not configurable and is not subject to any agent override.
- Statutory breathing space overrides all non-statutory reasons (RULING-001 §4).
- `OUT_OF_CONTACT_WINDOW` is a soft suppression — communications may be retried later within the same process instance when the window opens. The retry logic is owned by the Flowable process, not by this DMN.
- `secondary_reason_codes` JSONB output is the full set of all applicable reason codes other than the primary. `CommunicationSuppressionService` populates `suppression_reasons` with all applicable codes from its evaluation sequence (section 10) regardless of this DMN output.

---

## 5. Java Service Interfaces

All interfaces are defined in `domain/communications`. No Flowable imports appear in these interfaces. Process engine interaction is via `ProcessEventPort` and `DelegateCommandBus` only (ADR-003, CLAUDE.md architecture constraints).

### 5.1 `CommunicationSuppressionService`

```java
/**
 * Single authority for all outbound communication suppression decisions (ADR-014, ADR-001).
 * Every outbound communication MUST pass through this service before dispatch.
 * No channel adapter, template service, or Flowable delegate may bypass this service.
 *
 * The evaluation sequence is defined in section 10 of the Communications Domain Pack
 * and is locked. Changes to the sequence require a new ADR.
 */
public interface CommunicationSuppressionService {

    /**
     * Evaluates whether a communication is permitted for the given customer, account,
     * channel, and communication category. Records a communication_suppression_decision
     * row regardless of outcome. This method is the single enforcement gate.
     *
     * Evaluation sequence is locked (domain pack section 10):
     *  1. MHC breathing space active → SUPPRESS (no override)
     *  2. Statutory breathing space active → SUPPRESS (no override)
     *  3. DNC flag active → SUPPRESS
     *  4. Vulnerability suppression active for this channel → SUPPRESS
     *  5. Channel verified = false → SUPPRESS
     *  6. Frequency limit reached for this channel in rolling window → SUPPRESS
     *  7. Outside preferred contact window → SUPPRESS (retryable)
     *  8. Channel availability (kill-switch) → SUPPRESS
     *
     * All applicable reasons are logged in suppression_reasons JSONB even when
     * only the primary reason is needed. Every call produces a
     * communication_suppression_decision record.
     *
     * @param customerId      the customer to evaluate
     * @param accountId       the account (nullable for customer-level communications)
     * @param channel         the channel being evaluated
     * @param eventContext    context object containing communication_category, template_id,
     *                        flowable_instance_id (all nullable where not applicable)
     * @return SuppressionDecision containing: suppressed (boolean), primary reason code,
     *         all applicable reason codes, and the ID of the suppression_decision record created
     */
    SuppressionDecision evaluateAndLog(UUID customerId,
                                       UUID accountId,
                                       Channel channel,
                                       CommunicationEventContext eventContext);

    /**
     * Activates a named suppression reason for an account. Creates a suppression_log entry
     * (ADR-001). Used by domain/customer (breathing space, vulnerability) and by process
     * delegates for DCA_PLACEMENT_INTERNAL and similar internal reasons.
     * Writes an AUDIT_EVENT of type SUPPRESSION_ACTIVATED.
     *
     * Uses INSERT ... ON CONFLICT DO NOTHING semantics (ADR-001 §Schema).
     */
    void activateSuppression(UUID accountId, SuppressionReason reason, UUID activatedBy);

    /**
     * Lifts a named suppression reason for an account. Closes the suppression_log entry.
     * Must not lift a statutory reason via any automated path — only explicit moratorium-end
     * events may call this for BREATHING_SPACE_STATUTORY or MENTAL_HEALTH_CRISIS_STATUTORY
     * (RULING-001 guardrail 2).
     * Writes an AUDIT_EVENT of type SUPPRESSION_LIFTED.
     *
     * @throws StatutorySuppressionLiftViolationException if attempting to lift
     *         BREATHING_SPACE_STATUTORY or MENTAL_HEALTH_CRISIS_STATUTORY
     *         without a valid moratorium-end event context.
     */
    void liftSuppression(UUID accountId, SuppressionReason reason, UUID liftedBy);

    /**
     * Returns all currently active suppression reasons for an account.
     * Used by the agent UI suppression panel (read-only view) and by
     * ContactOrchestrationService before beginning channel selection.
     */
    List<SuppressionLog> getActiveSuppressions(UUID accountId);

    /**
     * Returns the suppression decision log for a customer within a time range.
     * Used by COMPLIANCE and TEAM_LEADER for audit review.
     */
    List<SuppressionDecisionSummary> getSuppressionDecisionHistory(UUID customerId,
                                                                    Instant from,
                                                                    Instant to);
}
```

### 5.2 `ContactOrchestrationService`

```java
/**
 * Receives communication instructions — from Flowable delegates (via DelegateCommandBus)
 * or from agent-initiated actions — and orchestrates the full dispatch sequence:
 * suppression evaluation, channel selection, template rendering, transport dispatch,
 * and event logging.
 *
 * This service is the integration point between the process engine (or agent UI) and
 * the communications pipeline. It does not make suppression decisions itself —
 * it delegates entirely to CommunicationSuppressionService.
 */
public interface ContactOrchestrationService {

    /**
     * Processes a SendCommunicationCommand. The full sequence:
     * 1. Look up the communication_template by templateCode and status = APPROVED.
     *    Throw TemplateNotApprovedForDispatchException if not found.
     * 2. Call CommunicationSuppressionService.evaluateAndLog() for the target channel
     *    (or evaluate all channels if channel = null, using channel-selection.dmn).
     * 3. If suppressed: create communication_event with status = SUPPRESSED,
     *    link to suppression_decision record, signal Flowable with outcome SUPPRESSED.
     * 4. If not suppressed: render template via TemplateManagementService.render(),
     *    create communication_event with status = PENDING, dispatch via the appropriate
     *    domain/integration adapter, update contact_frequency_record,
     *    signal Flowable with outcome DISPATCHED.
     *
     * For DEBT_COLLECTION category communications queued during a suppression period
     * that has since lifted: apply RULING-011 disposition rules (DISCARD for statutory
     * suppression; HOLD_FOR_REVIEW for internal policy suppression).
     *
     * @throws TemplateNotApprovedForDispatchException if the template is not APPROVED
     * @throws NoChannelAvailableException if channel = null and no channel can be selected
     */
    CommunicationEventResult orchestrate(SendCommunicationCommand command);

    /**
     * Records an inbound contact event (call received, letter reply received).
     * Creates a communication_event record with direction = INBOUND.
     * Does not invoke suppression evaluation (suppression applies to outbound only).
     * Updates DIC.3 contact history.
     *
     * @param command contains customerId, accountId, channel, eventType, outcome code,
     *                agent who recorded it, and optional notes
     */
    CommunicationEvent recordInboundContact(RecordInboundContactCommand command);

    /**
     * Records an agent note against a customer or account.
     * Creates a communication_event with event_type = AGENT_NOTE.
     * Agent notes are always permitted — they do not pass through suppression evaluation.
     * Visible in the contact history timeline.
     */
    CommunicationEvent addAgentNote(AddAgentNoteCommand command);
}
```

### 5.3 `TemplateManagementService`

```java
/**
 * CRUD for communication templates, template approval workflow, variable resolution,
 * and content rendering. Only APPROVED templates may be used in production dispatch.
 *
 * Template lifecycle: DRAFT → PENDING_APPROVAL → APPROVED (or back to DRAFT on rejection)
 * → RETIRED (terminal).
 */
public interface TemplateManagementService {

    /**
     * Creates a new template in DRAFT status.
     * Validates that all {{variable_name}} placeholders in body_template have
     * corresponding communication_template_variable declarations.
     * For SMS: validates body_template.length() <= max_character_count.
     *
     * @throws TemplateDuplicateCodeVersionException if (template_code, version) already exists
     */
    CommunicationTemplate createDraft(CreateTemplateCommand command);

    /**
     * Submits a DRAFT template for approval.
     * Sets status = PENDING_APPROVAL. Creates a work item in the approvals queue
     * for OPS_MANAGER or COMPLIANCE review. Caller must hold BACKOFFICE or PROCESS_DESIGNER role.
     */
    CommunicationTemplate submitForApproval(UUID templateId, UUID submittedByUserId);

    /**
     * Approves a PENDING_APPROVAL template.
     * Sets status = APPROVED, stamps approved_by and approved_at.
     * Caller must hold OPS_MANAGER or COMPLIANCE role.
     * Writes an AUDIT_EVENT of type TEMPLATE_APPROVED.
     *
     * @throws TemplateSelfApprovalViolationException if the approver also created the template
     */
    CommunicationTemplate approve(UUID templateId, UUID approvedByUserId);

    /**
     * Rejects a PENDING_APPROVAL template with a rejection note.
     * Returns status to DRAFT. Writes an AUDIT_EVENT of type TEMPLATE_REJECTED.
     * Caller must hold OPS_MANAGER or COMPLIANCE role.
     */
    CommunicationTemplate reject(UUID templateId, UUID rejectedByUserId, String rejectionNote);

    /**
     * Retires an APPROVED template. Sets status = RETIRED, stamps retired_at.
     * A retired template cannot be used in dispatch. In-flight communications
     * using this template at the time of retirement are not affected.
     * Caller must hold OPS_MANAGER role.
     */
    CommunicationTemplate retire(UUID templateId, UUID retiredByUserId);

    /**
     * Renders a template by substituting {{variable_name}} placeholders with
     * the supplied variable values. Validates that all mandatory variables are present.
     * For SMS: validates rendered length <= max_character_count.
     * Returns the rendered body (and subject for EMAIL) as plain text or HTML
     * per the channel convention.
     *
     * @throws MandatoryVariableMissingException if a mandatory variable has no value in the map
     * @throws SmsSizeExceededException if the rendered SMS body exceeds max_character_count
     */
    RenderedCommunication render(UUID templateId, Map<String, String> variables);

    /**
     * Returns an APPROVED template by its template_code.
     * Used by ContactOrchestrationService to look up the template before dispatch.
     *
     * @throws TemplateNotApprovedForDispatchException if the template exists but is not APPROVED
     * @throws TemplateNotFoundException if no template with this code exists
     */
    CommunicationTemplate getApprovedByCode(String templateCode);
}
```

### 5.4 `ContactPreferenceService`

```java
/**
 * Read and write customer contact preferences, verified status flags,
 * time-of-contact windows, and Do Not Contact (DNC) management.
 *
 * Contact preferences are customer-level (one row per customer_id).
 * The DNC flag is a high-consequence action (AAD.16) and requires TEAM_LEADER
 * or higher role to set.
 */
public interface ContactPreferenceService {

    /**
     * Returns the contact preference record for a customer.
     * Returns a default (all unverified, no DNC, standard channel) if no record exists.
     */
    ContactPreference getPreference(UUID customerId);

    /**
     * Creates or updates the contact preference record for a customer.
     * Caller must hold AGENT or higher role.
     * Writes an AUDIT_EVENT of type CONTACT_PREFERENCE_UPDATED.
     */
    ContactPreference upsertPreference(UpsertContactPreferenceCommand command);

    /**
     * Sets the Do Not Contact flag for a customer.
     * Caller must hold TEAM_LEADER or higher role (AAD.16).
     * Writes an AUDIT_EVENT of type DNC_FLAG_SET.
     * Does not automatically activate suppression — DNC is read by
     * CommunicationSuppressionService at evaluation time.
     *
     * @throws InsufficientRoleException if caller does not hold TEAM_LEADER or higher
     */
    ContactPreference setDoNotContact(UUID customerId, String dncReason, UUID setByUserId);

    /**
     * Lifts the Do Not Contact flag for a customer.
     * Caller must hold TEAM_LEADER or higher role.
     * Writes an AUDIT_EVENT of type DNC_FLAG_LIFTED.
     */
    ContactPreference liftDoNotContact(UUID customerId, UUID liftedByUserId);

    /**
     * Marks the channel-specific verified status (email_verified, phone_verified,
     * sms_consent, letter_address_valid). Used after successful delivery confirmation
     * or agent-verified contact.
     * Caller must hold AGENT or higher role.
     */
    ContactPreference updateVerifiedStatus(UUID customerId,
                                           Channel channel,
                                           boolean verified,
                                           UUID updatedByUserId);
}
```

### 5.5 `ContactHistoryService`

```java
/**
 * Retrieves the communication event timeline for a customer or account.
 * Provides the single customer communication UI (CC.11): all communications
 * on one screen in chronological order.
 *
 * Read-only service. All writes go through ContactOrchestrationService.
 */
public interface ContactHistoryService {

    /**
     * Returns all communication_event records for a customer, ordered by created_at descending.
     * Includes inbound and outbound events, agent notes, and suppressed events.
     * Suppression decision records are included as metadata on suppressed events.
     *
     * AGENT and higher roles may call this. No role restriction beyond authentication.
     * CC.11, DIC.3.
     */
    List<CommunicationEventSummary> getCustomerHistory(UUID customerId,
                                                        Instant from,
                                                        Instant to);

    /**
     * Returns all communication_event records for a specific account,
     * ordered by created_at descending. Subset of customer history.
     */
    List<CommunicationEventSummary> getAccountHistory(UUID accountId,
                                                       Instant from,
                                                       Instant to);

    /**
     * Returns the count of outbound communications dispatched per channel
     * within the given window. Used by the agent UI communications summary panel.
     */
    Map<Channel, Integer> getDispatchSummary(UUID customerId, Instant windowStart, Instant windowEnd);
}
```

### 5.6 `ReturnedMailService`

```java
/**
 * Intake of returned mail and invalid contact detail records,
 * resolution workflow, and contact_preference validity flagging.
 *
 * Returned mail creates a work item in domain/workallocation for BACKOFFICE resolution.
 * Resolution outcomes feed back to contact_preference (letter_address_valid = false
 * on CONFIRMED_INVALID).
 */
public interface ReturnedMailService {

    /**
     * Records a returned mail or invalid contact event.
     * Creates a returned_mail_record with resolution = PENDING.
     * Creates a work item in domain/workallocation BACKOFFICE queue via WorkAllocationPort.
     * Sets communication_event.status = RETURNED for the originating event (if linkable).
     *
     * @param command contains customerId, channel, addressOrNumberAttempted, returnedAt,
     *                returnReason, and optional communication_event_id
     */
    ReturnedMailRecord intake(IntakeReturnedMailCommand command);

    /**
     * Resolves a returned mail record.
     * - UPDATED: new valid contact detail has been confirmed. Updates contact_preference.
     *   If letter channel: sets letter_address_valid = true on new address.
     * - CONFIRMED_INVALID: contact detail confirmed invalid.
     *   If letter channel: sets contact_preference.letter_address_valid = false.
     * Caller must hold BACKOFFICE or OPS_MANAGER role.
     * Writes an AUDIT_EVENT of type RETURNED_MAIL_RESOLVED.
     *
     * @throws ReturnedMailRecordNotFoundException if not found or already resolved
     */
    ReturnedMailRecord resolve(UUID returnedMailRecordId,
                               ReturnedMailResolution resolution,
                               UUID resolvedByUserId);

    /**
     * Returns all unresolved returned mail records for BACKOFFICE queue display.
     * Ordered by returned_at ascending (oldest first).
     */
    List<ReturnedMailRecord> getUnresolved();

    /**
     * Returns the returned mail history for a customer.
     */
    List<ReturnedMailRecord> getCustomerReturnedMailHistory(UUID customerId);
}
```

---

## 6. Flowable Integration

### 6.1 How Flowable strategy processes instruct communications

Flowable strategy processes (BPMN service tasks) instruct the communications domain via the `DelegateCommandBus`. A JavaDelegate implementation in `infrastructure/process` receives the BPMN execution context, constructs a `SendCommunicationCommand`, and dispatches it via the command bus. The `domain/communications` command handler receives the command and calls `ContactOrchestrationService.orchestrate()`.

This pattern ensures that `domain/communications` has no Flowable imports. The Flowable delegate constructs the command and signals process outcomes; the domain service executes the business logic.

### 6.2 `SendCommunicationCommand` — fields and semantics

```java
public record SendCommunicationCommand(
    UUID customerId,                // required
    UUID accountId,                 // nullable — for customer-level communications
    String templateCode,            // required — must match an APPROVED template
    Channel channel,                // nullable — if null, ContactOrchestrationService selects via channel-selection.dmn
    CommunicationCategory communicationCategory, // required — must match the template's communication_category
    Instant scheduledAt,            // nullable — if null, dispatch is immediate
    String flowableInstanceId,      // nullable — populated for Flowable-initiated commands; used to signal back
    String flowableExecutionId,     // nullable — the specific execution within the process instance for signalling
    Map<String, String> templateVariables, // required if the template has mandatory variables
    String initiatedBy              // 'SYSTEM' for Flowable-initiated; agent UUID for agent-initiated
) {}
```

**Invariant:** `templateCode` must resolve to an APPROVED `communication_template` at command execution time. If the template is not APPROVED, `ContactOrchestrationService` throws `TemplateNotApprovedForDispatchException` and signals Flowable with outcome `TEMPLATE_NOT_APPROVED`. The process delegate must handle this signal and escalate to an agent task.

### 6.3 Flowable delegate sequence — outbound dispatch

```
[BPMN Service Task: Send Communication]
    → SendCommunicationDelegate (infrastructure/process)
        1. Reads templateCode, channel, templateVariables from Flowable process variables
        2. Constructs SendCommunicationCommand
        3. Dispatches via DelegateCommandBus → CommunicationCommandHandler (domain/communications)
        4. CommunicationCommandHandler calls ContactOrchestrationService.orchestrate(command)

[ContactOrchestrationService.orchestrate()]
    1. Look up template by templateCode — must be APPROVED
    2. If channel == null: evaluate channel-selection.dmn via Flowable DMN engine (read-only call)
    3. Call CommunicationSuppressionService.evaluateAndLog()
       → Creates communication_suppression_decision record
    4a. If SUPPRESSED:
        - Create communication_event with status = SUPPRESSED
        - Link communication_event_id on suppression_decision record
        - Signal Flowable via ProcessEventPort: outcome = SUPPRESSED, reason = primaryReasonCode
    4b. If NOT SUPPRESSED:
        - Render template via TemplateManagementService.render()
        - Create communication_event with status = PENDING
        - Call domain/integration adapter (SMTP / SMS gateway / print queue)
        - Increment contact_frequency_record
        - Update communication_event status to DISPATCHED
        - Signal Flowable via ProcessEventPort: outcome = DISPATCHED
```

**Transaction boundary (ADR-003):** All domain writes (communication_event, suppression_decision, contact_frequency_record) are inside `@Transactional`. The Flowable `ProcessEventPort` signal is called outside the transaction boundary, after commit.

### 6.4 Flowable delegate sequence — suppressed communication disposition (RULING-011)

When `CommunicationSuppressionService` returns SUPPRESSED, the `communication_event` status is set to `SUPPRESSED` and linked to the `communication_suppression_decision` record. The Flowable process receives `outcome = SUPPRESSED` and parks the token.

On suppression lift (e.g., breathing space period ends), the process that triggered the original communication must apply RULING-011 disposition:

- **Statutory suppression** (BREATHING_SPACE_STATUTORY, MENTAL_HEALTH_CRISIS_STATUTORY, INSOLVENCY_STATUTORY, DECEASED_MANDATORY): the `communication_event` status is updated to `DISCARDED_STATUTORY`. The event record is retained — it must not be deleted. A fresh communication must be generated by the resuming process if still required. The Flowable delegate reads the `primary_reason_code` from the original `communication_suppression_decision` record to determine which disposition rule applies.
- **Internal policy suppression** (DISPUTE_INTERNAL, VULNERABILITY_POLICY): the `communication_event` status is updated to `HELD_FOR_REVIEW`. A work item is created for the agent to explicitly approve or discard each held communication before dispatch. The Flowable process token parks at a user task "Review held communications — confirm appropriateness before dispatch."

**Invariant:** The `communication_event` record for a discarded or held communication is never deleted. The audit trail must show its full lifecycle from PENDING through SUPPRESSED to DISCARDED_STATUTORY or HELD_FOR_REVIEW.

### 6.5 Inbound events — how Flowable receives signals

Inbound events (call received, letter reply, email reply) are recorded via `ContactOrchestrationService.recordInboundContact()`. When an inbound event is relevant to an active Flowable process (identified by a `flowableInstanceId` on the associated account), the command handler signals Flowable via `ProcessEventPort` with the event type and outcome.

Example: an inbound PAYMENT_PLAN_OFFER_RESPONSE letter triggers `INBOUND_RESPONSE_RECEIVED` signal to the active `COLLECTION_PROCESS` instance for the account. The BPMN process has an intermediate catching message event listening for this signal.

---

## 7. Admin UI Specification

The communications admin functions are part of the `/admin` unified UI (referenced in ADR-015). The Templates section and Suppression Rule Configurator are accessible at `/admin/communications`. PROCESS_DESIGNER and OPS_MANAGER roles may access this section.

### 7.1 Template Library Screen

**Path:** `/admin/communications/templates`
**Access:** PROCESS_DESIGNER, OPS_MANAGER

| UI element | Behaviour |
|---|---|
| Template list | Displays: template_code, name, channel, communication_category, status, version, approved_at (formatted date or "Not yet approved") |
| Filter bar | Filter by: channel (multi-select), status (multi-select), communication_category |
| Search | Free-text search on template_code and name |
| "New template" button | Opens Template Editor with status = DRAFT |
| Row action: "Edit" | Opens Template Editor (DRAFT and PENDING_APPROVAL templates only; APPROVED and RETIRED are view-only) |
| Row action: "Submit for approval" | Transitions DRAFT → PENDING_APPROVAL; disabled if template has unresolved variable validation errors |
| Row action: "Retire" | Transitions APPROVED → RETIRED; requires OPS_MANAGER role; shows confirmation modal |
| Approvals badge | Count of PENDING_APPROVAL templates; shown to OPS_MANAGER and COMPLIANCE |

### 7.2 Template Editor

**Path:** `/admin/communications/templates/new` and `/admin/communications/templates/{id}/edit`
**Access:** BACKOFFICE (create/edit DRAFT), PROCESS_DESIGNER (create/edit DRAFT), OPS_MANAGER (all)

| UI element | Behaviour |
|---|---|
| Template code | Required; unique per version; validated against (template_code, version) uniqueness constraint on save |
| Name | Required; free text |
| Channel selector | Single-select: LETTER, SMS, EMAIL, IN_APP |
| Communication category selector | Single-select: DEBT_COLLECTION, NON_COLLECTION, DUAL_USE, ESTATE_ADMINISTRATION |
| Body editor | Rich text editor with `{{variable}}` syntax highlighting; for SMS: live character counter against max_character_count |
| Subject field | Shown for EMAIL channel only; required when channel = EMAIL |
| Max character count | Shown and required for SMS channel only |
| Variable declarations panel | Add/remove variable declarations; each has: variable_name (matching placeholder), data_type, is_mandatory, description; validation error shown if a placeholder has no declaration |
| Preview mode | Renders body_template with sample values from variable declarations; shows rendered character count for SMS |
| Save as draft button | Saves status = DRAFT; validates all placeholders declared |
| Submit for approval button | Transitions to PENDING_APPROVAL; disabled if validation errors present |

### 7.3 Template Approval Workflow

**Path:** `/admin/communications/templates/approvals`
**Access:** OPS_MANAGER, COMPLIANCE

| UI element | Behaviour |
|---|---|
| Approvals queue | Lists all PENDING_APPROVAL templates; sorted by submission date ascending |
| Preview panel | Renders the template with sample variable values; shows all variable declarations |
| Approve button | Calls `TemplateManagementService.approve()`; status → APPROVED; stamped approved_by and approved_at; self-approval blocked with inline error |
| Reject button | Opens rejection note input; calls `TemplateManagementService.reject()`; status → DRAFT with rejection note displayed to original submitter |
| Version history | Shows prior versions of the same template_code with their statuses and approval history |

### 7.4 Suppression Rule Configurator (Tier 1 Foundations)

**Path:** `/admin/communications/suppression-rules`
**Access:** BACKOFFICE (edit rows), OPS_MANAGER (approve changes)

| UI element | Behaviour |
|---|---|
| Rules table | Displays channel_suppression_rule rows: channel, max_contacts_per_window, window_days, contact_window_start, contact_window_end, channel_kill_switch, effective_from, effective_to |
| Edit row | BACKOFFICE may edit any field; creates a new version row (effective_from = proposed date); does not overwrite existing active row |
| Kill-switch toggle | Channel-level kill-switch; toggle requires OPS_MANAGER confirmation modal; activates immediately (effective_from = now) |
| Pending changes queue | Changes awaiting OPS_MANAGER approval; shows diff between current and proposed values |
| Approve change button | OPS_MANAGER approves a pending change; stamps approved_by and approved_at; change becomes active on effective_from |
| Reject change button | Discards pending change; notifies submitter |

---

## 8. RBAC Matrix

| Permission | AGENT | TEAM_LEADER | OPS_MANAGER | COMPLIANCE | BACKOFFICE | PROCESS_DESIGNER | SYSTEM |
|---|---|---|---|---|---|---|---|
| View contact history (communication_event timeline) | Yes | Yes | Yes | Yes (read-only) | Yes | No | No |
| Manually record inbound contact | Yes | Yes | Yes | No | No | No | No |
| Add agent note | Yes | Yes | Yes | No | No | No | No |
| View suppression decisions (communication_suppression_decision) | No | Yes | Yes | Yes (read-only) | No | No | No |
| Set DNC flag | No | Yes | Yes | No | No | No | No |
| Lift DNC flag | No | Yes | Yes | No | No | No | No |
| Update contact preferences | Yes | Yes | Yes | No | Yes | No | No |
| Create/edit template DRAFT | No | No | Yes | No | Yes | Yes | No |
| Submit template for approval | No | No | Yes | No | Yes | Yes | No |
| Approve/reject template | No | No | Yes | Yes | No | No | No |
| Retire template | No | No | Yes | No | No | No | No |
| Edit suppression rule config | No | No | No | No | Yes | No | No |
| Approve suppression rule config change | No | No | Yes | No | No | No | No |
| Activate channel kill-switch | No | No | Yes | No | No | No | No |
| Resolve returned mail | No | No | Yes | No | Yes | No | No |
| Send communications (automated) | No | No | No | No | No | No | Yes |
| Bypass CommunicationSuppressionService | No | No | No | No | No | No | No |

**Invariant:** No role may bypass `CommunicationSuppressionService`. This is enforced at the `ContactOrchestrationService` level. The `SYSTEM` role (used by Flowable delegates and batch processes) must pass through the same suppression evaluation as any agent-initiated action.

---

## 9. Integration Points

### 9.1 `domain/customer`

| Integration | Direction | Mechanism | Purpose |
|---|---|---|---|
| Vulnerability flag status (boolean — is any flag active?) | Read | Service interface (`VulnerabilityFlagService.hasActiveVulnerabilityFlag`) | `CommunicationSuppressionService` step 4 |
| Breathing space period (active/inactive, type) | Read | Service interface (`BreathingSpaceService.isBreathingSpaceActive`, `getActivePeriod`) | `CommunicationSuppressionService` steps 1 and 2 |
| Accessibility needs (communication_restrictions JSONB) | Read | Service interface | Channel selection — if accessibility need restricts a channel, `channel-selection.dmn` input `vulnerabilityActive` is set true |
| Customer contact details (address, email, phone) | Read | Service interface | Dispatch address resolution before sending to `domain/integration` adapter |

`domain/communications` does not write to `domain/customer` tables. The `vulnerability_suppression_log` (in `domain/customer`) is written by `domain/customer` when it receives suppression reason codes from `CommunicationSuppressionService` callbacks — not by this domain.

### 9.2 `domain/integration`

| Integration | Direction | Mechanism | Purpose |
|---|---|---|---|
| SMTP gateway dispatch | Outbound (communications → integration) | Interface call to `EmailGatewayPort` | Sends rendered email content to external mail service |
| SMS gateway dispatch | Outbound (communications → integration) | Interface call to `SmsGatewayPort` | Sends rendered SMS to external SMS provider |
| Print/letter queue dispatch | Outbound (communications → integration) | Interface call to `LetterQueuePort` | Enqueues rendered letter for print/post |
| Delivery receipt callback | Inbound (integration → communications) | `CommunicationOutcomeCallback` interface | Updates `communication_event.status`, `outcome_at`, `outcome_code` on delivery confirmation or bounce |

`domain/integration` never makes logic decisions about whether to send. It executes transport only.

### 9.3 `domain/account`

| Integration | Direction | Mechanism | Purpose |
|---|---|---|---|
| Account reference data (account number, balance, debt type) | Read | Service interface | Template variable substitution — balance and account number are common mandatory template variables |

### 9.4 `domain/workallocation`

| Integration | Direction | Mechanism | Purpose |
|---|---|---|---|
| Create BACKOFFICE work item for returned mail | Outbound (communications → workallocation) | `WorkAllocationPort` | `ReturnedMailService.intake()` creates a work item for BACKOFFICE resolution |
| Create agent task for HELD_FOR_REVIEW communications | Outbound (communications → workallocation) | `WorkAllocationPort` | After internal policy suppression lift, held communications require agent approval task |

---

## 10. `CommunicationSuppressionService` — Detailed Specification

### 10.1 Evaluation order

The following sequence is locked. Changes require a new ADR. The sequence must be implemented exactly as stated. Steps are evaluated in order; the first step that produces a SUPPRESS result determines the `primary_reason_code`. All applicable reasons from all steps must still be evaluated and logged in `suppression_reasons` JSONB — the evaluation does not short-circuit after finding the first reason.

| Step | Check | Reason code | Can be overridden? |
|---|---|---|---|
| 1 | MHC breathing space active for this customer | `MHC_BREATHING_SPACE_STATUTORY` | No — statutory prohibition. No role or automation may override. |
| 2 | Statutory breathing space active for this customer | `BREATHING_SPACE_STATUTORY` | No — statutory prohibition (RULING-001). |
| 3 | DNC flag active for this customer (`contact_preference.do_not_contact = true`) | `DNC_ACTIVE` | TEAM_LEADER or OPS_MANAGER may lift the DNC flag via `ContactPreferenceService.liftDoNotContact()`, which is a separate workflow from the suppression check |
| 4 | Vulnerability suppression active for this channel — active vulnerability flag AND accessibility need restricts this channel | `VULNERABILITY_POLICY` | SPECIALIST_AGENT or TEAM_LEADER may resolve the vulnerability flag via `VulnerabilityFlagService.resolveFlag()` |
| 5 | Channel verified = false (email_verified, sms_consent, letter_address_valid per channel) | `CHANNEL_UNVERIFIED` | Agent may update verified status via `ContactPreferenceService.updateVerifiedStatus()` |
| 6 | Contact frequency limit reached — `contact_frequency_record.contact_count >= max_contacts` for this channel in the active window | `FREQUENCY_LIMIT_REACHED` | Not overridable within the current window; auto-lifts at window_end |
| 7 | Outside preferred contact window — current time is outside `preferred_contact_window_start` / `preferred_contact_window_end` | `OUT_OF_CONTACT_WINDOW` | Retryable — Flowable process may retry when window opens; this is a soft SUPPRESS |
| 8 | Channel availability — `channel_suppression_rule.channel_kill_switch = true` for this channel | `CHANNEL_UNAVAILABLE` | OPS_MANAGER may deactivate kill-switch via admin UI |

**Special case — ESTATE_ADMINISTRATION category:** All steps are bypassed. `isPermitted()` returns PERMITTED unconditionally for `ESTATE_ADMINISTRATION` communications, per ADR-001 decision logic. A `communication_suppression_decision` record is still written with `suppressed = false` and no reason codes, as evidence that the evaluation occurred.

**Special case — DECEASED_MANDATORY suppression:** This suppression reason is owned by `domain/account` and activated by `DeceasedPartyHandler`. It blocks all communications (DEBT_COLLECTION and NON_COLLECTION) to the deceased person. Step 2 must check for `DECEASED_MANDATORY` in addition to `BREATHING_SPACE_STATUTORY`. ESTATE_ADMINISTRATION communications to the estate (not the deceased person) are unaffected.

### 10.2 Logging requirement

Every invocation of `evaluateAndLog()` must produce exactly one `communication_suppression_decision` row. This is a regulatory evidence requirement. There are no exceptions — even evaluations that return PERMITTED must be logged.

### 10.3 Non-bypassable invariant

No code path may dispatch an outbound communication without first calling `evaluateAndLog()`. This is enforced architecturally: `ContactOrchestrationService` is the only entry point for outbound dispatch, and it always calls `CommunicationSuppressionService.evaluateAndLog()` as step 3 of its orchestration sequence. No channel adapter in `domain/integration` may initiate a dispatch independently.

---

## 11. Open Questions

All open questions require DWP policy or product owner input before the affected schema, DMN, or service behaviour can be finalised. No silent resolutions will be made.

---

### DDE-OQ-COMMS-01 — Agent-initiated outbound calls and suppression scope

**Status:** Open
**Owner:** DWP Debt Domain Expert
**BLOCKING for:** `ContactOrchestrationService` PHONE channel handling; RBAC matrix for AGENT calling behaviour

**Question:** Are agent-initiated outbound telephone calls subject to the same `CommunicationSuppressionService.evaluateAndLog()` check as automated outbound communications? Specifically: if a customer has an active DNC flag or is in breathing space, should the system prevent an agent from logging an outbound call, or should it warn the agent but permit the log entry?

**Impact:** If calls are subject to suppression, `ContactOrchestrationService.orchestrate()` must be called with channel = PHONE before any outbound call is recorded. If calls are advisory only, a separate "log call outcome" path is needed that records the event but does not enforce suppression. The distinction affects the PHONE channel handling in `channel_suppression_rule` and the RBAC matrix.

---

### DDE-OQ-COMMS-02 — Contact frequency window: sliding vs calendar

**Status:** Open — **BLOCKING** for `contact_frequency_record` schema semantics and `CommunicationSuppressionService` step 6 implementation
**Owner:** DWP policy team

**Question:** Is the contact frequency limit (e.g., max 3 SMS per 7 days) a sliding window (always the last 7 days from the current moment) or a calendar window (fixed periods, e.g., Mon–Sun)?

**Impact:** Determines `contact_frequency_record.window_start` and `window_end` population logic. A sliding window requires a different query pattern than a calendar window. The schema is compatible with both, but the service implementation and the meaning of `window_start` / `window_end` differ. This must be confirmed before `CommunicationSuppressionService` step 6 is built.

---

### DDE-OQ-COMMS-03 — Channel fallback order when preferred channel is unavailable

**Status:** Open — **BLOCKING** for `channel-selection.dmn` production deployment
**Owner:** DWP policy team

**Question:** When a customer's preferred channel is unavailable or unverified, what is the DWP-mandated fallback order? The DMN in section 4.1 uses LETTER as a universal fallback. Is LETTER always the final fallback, or is there a policy-defined order (e.g., EMAIL → SMS → LETTER, or EMAIL → LETTER → SMS)?

**Impact:** The `fallbackChannel` output column of `channel-selection.dmn` must reflect the DWP-approved fallback order. Implementing the wrong fallback order could result in customer contact via an incorrect or unpreferred channel.

---

### DDE-OQ-COMMS-04 — Returned mail SLA: when is an address definitively marked invalid?

**Status:** Open — **BLOCKING** for `ReturnedMailService.resolve()` CONFIRMED_INVALID timing rules
**Owner:** DWP policy team / operational policy

**Question:** Is there a defined SLA — in calendar days — between the return of a letter and the point at which an address is considered definitively invalid (CONFIRMED_INVALID) rather than pending verification? If a returned mail work item is not resolved within the SLA, should the system auto-escalate or auto-set the address to CONFIRMED_INVALID?

**Impact:** Determines whether the `returned_mail_record` needs an SLA due-date field and whether an automated escalation timer (Flowable or batch) must be built. Without an SLA, the address remains PENDING indefinitely, which may suppress legitimate letters longer than necessary.

---

### DDE-OQ-COMMS-05 — In-app messages: in scope for this domain or deferred?

**Status:** Open
**Owner:** Delivery Lead / DWP product owner

**Question:** The tender (DW.65) requires in-app messages as part of the contact strategy. The system currently has no customer self-service portal. Should `domain/communications` model the IN_APP channel (templates, suppression, event log) now in anticipation of a future portal, or should IN_APP be excluded from the initial delivery scope?

**Impact:** If IN_APP is included in scope now: `communication_template` rows with channel = IN_APP must be deployable and the `domain/integration` layer must have a stub adapter for the IN_APP channel. If deferred: the channel enum should include IN_APP as a declared-but-unimplemented value to avoid a breaking schema change later, but no templates or adapters are built.

---

## 12. Acceptance Criteria

All acceptance criteria are sourced to requirement IDs in `Tender requirements docs/Functional-Requirements-Consolidated.md` and governed by the domain rulings listed in the document header.

---

**AC-COMMS-01 — MHC breathing space blocks all DEBT_COLLECTION dispatch; no role may override**
*Requirement IDs: CAS.BS-01, CAS.BS-02, DIC.26*
*Ruling: RULING-001 §2 and §5, ADR-001 guardrail 2*

Given customer C has an active `MENTAL_HEALTH_CRISIS_STATUTORY` breathing space period
And the system (or an agent action) initiates a `SendCommunicationCommand` with `communicationCategory = DEBT_COLLECTION` for any channel
When `CommunicationSuppressionService.evaluateAndLog()` is called
Then `suppressed = true` is returned with `primaryReasonCode = MHC_BREATHING_SPACE_STATUTORY`
And a `communication_suppression_decision` row is written with `suppressed = true`, `primary_reason_code = MHC_BREATHING_SPACE_STATUTORY`
And `communication_event.status = SUPPRESSED` is set for the attempted dispatch
And Flowable receives `outcome = SUPPRESSED` with reason `MHC_BREATHING_SPACE_STATUTORY`

**Negative case:** An OPS_MANAGER calls `CommunicationSuppressionService.liftSuppression()` with reason `MENTAL_HEALTH_CRISIS_STATUTORY` without a valid moratorium-end event context. The service throws `StatutorySuppressionLiftViolationException`. The active suppression record is unchanged. An `AUDIT_EVENT` of type `STATUTORY_SUPPRESSION_LIFT_REJECTED` is written with the caller's user ID.

---

**AC-COMMS-02 — Suppression decision is always logged before dispatch attempt**
*Requirement IDs: CC.31, DIC.25*
*Ruling: RULING-001, ADR-001 guardrail 1*

Given a `SendCommunicationCommand` is received for customer C, account A, channel SMS, category DEBT_COLLECTION
When `ContactOrchestrationService.orchestrate()` executes
Then `CommunicationSuppressionService.evaluateAndLog()` is called before any call to a `domain/integration` channel adapter
And a `communication_suppression_decision` row exists with the correct customer_id, account_id, channel, category, and evaluated_at
And the `communication_event_id` on the suppression_decision row is linked to the `communication_event` record created in the same orchestration run

**Negative case:** A hypothetical direct call to the `domain/integration` `SmsGatewayPort` without going through `ContactOrchestrationService` (e.g., a test harness) is blocked at the integration layer — no adapter may accept a dispatch request without a linked `communication_event_id` that has an associated `communication_suppression_decision` record with `suppressed = false`.

---

**AC-COMMS-03 — Template with non-APPROVED status cannot be used in production dispatch**
*Requirement IDs: CC.23, DW.73*
*Ruling: none — business rule*

Given a communication_template with `template_code = ARREARS_NOTICE_01` exists with `status = PENDING_APPROVAL`
When a `SendCommunicationCommand` with `templateCode = ARREARS_NOTICE_01` is received
Then `ContactOrchestrationService.orchestrate()` throws `TemplateNotApprovedForDispatchException`
And no `communication_event` record with `status = DISPATCHED` is created
And Flowable receives `outcome = TEMPLATE_NOT_APPROVED`
And a `communication_event` record with `status = FAILED` is written as evidence

**Negative case:** The same template, after being approved (`status = APPROVED`), is used in a dispatch command. The exception is not thrown and dispatch proceeds through the normal suppression-evaluation sequence.

---

**AC-COMMS-04 — Contact frequency limit suppresses dispatch when limit reached**
*Requirement IDs: CC.27, CC.31*
*Ruling: none — Tier 1 Foundations configuration*

Given `channel_suppression_rule` has `channel = SMS`, `max_contacts_per_window = 3`, `window_days = 7` active
And customer C has a `contact_frequency_record` with `channel = SMS`, `contact_count = 3` within the current window
When a `SendCommunicationCommand` with `channel = SMS` is received for customer C
Then `CommunicationSuppressionService.evaluateAndLog()` returns `suppressed = true` with `primary_reason_code = FREQUENCY_LIMIT_REACHED`
And the communication is not dispatched
And `contact_frequency_record.contact_count` is NOT incremented (suppressed communications do not count towards the limit)

**Negative case:** After the window expires (`window_end` has passed), a new `contact_frequency_record` row is created with `contact_count = 0`. The next `SendCommunicationCommand` for SMS is permitted (assuming no other suppression reason applies).

---

**AC-COMMS-05 — Statutory suppression: DEBT_COLLECTION communications are discarded on suppression lift; audit trail preserved**
*Requirement IDs: CAS.COM-07, CAS.COM-08*
*Ruling: RULING-011 §1 and guardrail 2*

Given customer C had an active `BREATHING_SPACE_STATUTORY` suppression
And a `communication_event` with `status = SUPPRESSED` and `primary_reason_code = BREATHING_SPACE_STATUTORY` was created for a DEBT_COLLECTION communication during that period
When the breathing space period ends and suppression is lifted
Then the `communication_event.status` is updated to `DISCARDED_STATUTORY`
And the `communication_event` record is not deleted — it remains in the audit trail
And a fresh communication is NOT automatically dispatched — the resuming Flowable process must generate a new `SendCommunicationCommand` if the communication is still required
And an `AUDIT_EVENT` of type `COMMUNICATION_DISCARDED_STATUTORY` is written referencing the `communication_event.id`

**Negative case:** An internal policy suppression (`VULNERABILITY_POLICY`) lifts for the same customer. A DEBT_COLLECTION communication that was `SUPPRESSED` under `VULNERABILITY_POLICY` has its status updated to `HELD_FOR_REVIEW` (not `DISCARDED_STATUTORY`). A BACKOFFICE agent work item is created requiring explicit approval before dispatch.

---

**AC-COMMS-06 — Internal policy suppression: held communications require explicit agent approval**
*Requirement IDs: CAS.COM-08, CC.32, CC.34*
*Ruling: RULING-011 §2 and guardrail 3*

Given a `communication_event` with `status = SUPPRESSED` and `primary_reason_code = DISPUTE_INTERNAL` exists for account A
When the `DISPUTE_INTERNAL` suppression is lifted
Then `communication_event.status` is updated to `HELD_FOR_REVIEW`
And a work item is created in `domain/workallocation` with description "Review held communications for [account A] — confirm appropriateness before dispatch"
And the Flowable process token parks at the "Review held communications" user task
And no dispatch to `domain/integration` occurs automatically

**Negative case:** An agent attempts to directly set `communication_event.status = DISPATCHED` from `HELD_FOR_REVIEW` without going through the work item approval workflow. The service layer rejects the status transition with `InvalidStatusTransitionException` — only the explicit "approve for dispatch" action in `ContactOrchestrationService` may transition from `HELD_FOR_REVIEW` to `DISPATCHED`.

---

**AC-COMMS-07 — Returned mail creates BACKOFFICE work item and flags address**
*Requirement IDs: DIC.3, DIC.25, BSF.8*
*Ruling: none — operational workflow*

Given a letter is returned for customer C at address "10 Example Street"
When `ReturnedMailService.intake()` is called with `return_reason = ADDRESS_NOT_FOUND`
Then a `returned_mail_record` is created with `resolution = PENDING`
And a work item is created in the BACKOFFICE queue via `WorkAllocationPort` with reference to the `returned_mail_record.id`
And `contact_preference.letter_address_valid` is NOT automatically set to false — that happens only on resolution = CONFIRMED_INVALID
When a BACKOFFICE agent resolves the record with `resolution = CONFIRMED_INVALID`
Then `contact_preference.letter_address_valid = false`
And an `AUDIT_EVENT` of type `RETURNED_MAIL_RESOLVED` is written

**Negative case:** A BACKOFFICE agent attempts to resolve the returned mail record with `resolution = CONFIRMED_INVALID` without providing a `resolved_at` and `handled_by`. The service throws `ReturnedMailResolutionInvalidException`. The record remains at `resolution = PENDING`.

---

**AC-COMMS-08 — Channel fallback when preferred channel unavailable or unverified**
*Requirement IDs: DW.34, DW.43, CC.4*
*Ruling: none — pending DDE-OQ-COMMS-03*

Given customer C has `preferred_channel = EMAIL` and `email_verified = false`
And a `SendCommunicationCommand` with `channel = null` (preference-based) is received
When `channel-selection.dmn` is evaluated
Then `selectedChannel = LETTER` (fallback — preferred channel unverified)
And `CommunicationSuppressionService.evaluateAndLog()` is called for channel = LETTER
And if LETTER is also suppressed (e.g., `letter_address_valid = false`): `ContactOrchestrationService` throws `NoChannelAvailableException` and the event is logged as `SUPPRESSED` with reason `CHANNEL_UNVERIFIED`

**Negative case:** Customer C's `email_verified` is updated to true via `ContactPreferenceService.updateVerifiedStatus()`. The next `SendCommunicationCommand` with `channel = null` evaluates `channel-selection.dmn` and returns `selectedChannel = EMAIL` — the preferred channel is now used.

---

**AC-COMMS-09 — Agent note appears in contact history timeline immediately**
*Requirement IDs: DIC.3, CC.11, UI.2, UAAF.13*
*Ruling: none — operational requirement*

Given an AGENT calls `ContactOrchestrationService.addAgentNote()` for customer C, account A, with note text "Customer called to report change of address"
When the command is processed
Then a `communication_event` with `event_type = AGENT_NOTE`, `direction = INBOUND`, `status = DELIVERED` is created immediately
And `ContactHistoryService.getAccountHistory(accountId)` returns this event as the most recent entry
And the event is visible in the single customer communication UI (CC.11) for all roles that can view contact history

**Negative case:** An agent note creation is attempted without a valid `customerId`. The command handler throws `CustomerNotFoundException` before any `communication_event` record is created.

---

**AC-COMMS-10 — Template mandatory variable missing at render time throws exception; no partial dispatch**
*Requirement IDs: DW.73, CC.23*
*Ruling: none — business rule*

Given template `ARREARS_NOTICE_01` has status = APPROVED and declares variable `OUTSTANDING_BALANCE` with `is_mandatory = true`
And a `SendCommunicationCommand` is received with `templateVariables = {}` (no variables supplied)
When `TemplateManagementService.render()` is called
Then `MandatoryVariableMissingException` is thrown with message indicating `OUTSTANDING_BALANCE` is missing
And no `communication_event` with `status = DISPATCHED` is created
And Flowable receives `outcome = TEMPLATE_RENDER_FAILED`

**Negative case:** The same command with `templateVariables = {"OUTSTANDING_BALANCE": "£142.50"}` renders successfully. The rendered body is returned and dispatch proceeds.

---

**AC-COMMS-11 — Joint customer: all named parties receive relevant communications**
*Requirement IDs: DW.55, DW.74, CC.8*
*Ruling: RULING-004 (joint debt constraints)*

Given account A is a joint account with customer C1 and customer C2
And a `SendCommunicationCommand` for a DEBT_COLLECTION letter is received for account A with `communicationCategory = DEBT_COLLECTION`
When `ContactOrchestrationService.orchestrate()` executes
Then `CommunicationSuppressionService.evaluateAndLog()` is called separately for each of C1 and C2 with their respective channel and preference data
And a separate `communication_event` record is created for each customer
And if C1's suppression evaluation returns SUPPRESSED but C2's returns PERMITTED: C2's letter is dispatched and C1's is logged as SUPPRESSED — the suppression of one party does not suppress the other's communication

**Negative case:** Both C1 and C2 have active breathing space suppression. Both `evaluateAndLog()` calls return SUPPRESSED. No letter is dispatched for either party. Two `communication_event` records with `status = SUPPRESSED` are created.

---

**AC-COMMS-12 — Suppression decision log entry written even when communication is permitted**
*Requirement IDs: DIC.25, CC.31*
*Ruling: ADR-001; domain pack section 10.2*

Given customer C has no active suppressions and `email_verified = true`
And a `SendCommunicationCommand` with `channel = EMAIL` and `communicationCategory = NON_COLLECTION` is received
When `CommunicationSuppressionService.evaluateAndLog()` is called
Then a `communication_suppression_decision` record is written with `suppressed = false` and `suppression_reasons = []`
And the communication proceeds to dispatch
And `ContactHistoryService.getCustomerHistory()` returns the dispatched `communication_event` with no suppression annotation

**Negative case:** Querying `communication_suppression_decision` for a date range where no communications were attempted returns an empty list — rows are created only when `evaluateAndLog()` is called, not speculatively.

---

**AC-COMMS-13 — APPROVED template retirement prevents future dispatch; in-flight unaffected**
*Requirement IDs: CC.23, DW.73*
*Ruling: none — business rule*

Given template `OD51` has `status = APPROVED`
And there is an in-flight `communication_event` with `status = PENDING` for template `OD51` currently being processed by the integration layer
When an OPS_MANAGER retires template `OD51` (`status → RETIRED`, `retired_at` stamped)
Then the in-flight `communication_event` continues to process to completion — retirement does not cancel in-flight events
And any subsequent `SendCommunicationCommand` referencing `templateCode = OD51` throws `TemplateNotApprovedForDispatchException`
And `TemplateManagementService.getApprovedByCode("OD51")` throws `TemplateNotApprovedForDispatchException`

**Negative case:** A BACKOFFICE user attempts to retire a template with `status = DRAFT`. The service throws `InvalidTemplateStatusTransitionException` — only APPROVED templates may be retired.

---

**AC-COMMS-14 — DNC flag suppresses communications; flag lift restores them**
*Requirement IDs: AAD.16, CC.31, CC.34*
*Ruling: none — operational policy*

Given customer C has `do_not_contact = true` set by a TEAM_LEADER
When any outbound `SendCommunicationCommand` is received for customer C with `communicationCategory = DEBT_COLLECTION` or `NON_COLLECTION`
Then `CommunicationSuppressionService.evaluateAndLog()` returns `suppressed = true` with `primary_reason_code = DNC_ACTIVE`
And `communication_event.status = SUPPRESSED`
When a TEAM_LEADER calls `ContactPreferenceService.liftDoNotContact()` for customer C
Then `contact_preference.do_not_contact = false`
And an `AUDIT_EVENT` of type `DNC_FLAG_LIFTED` is written
And subsequent `SendCommunicationCommand` requests are no longer suppressed at step 3 (assuming no other active suppression reasons)

**Negative case:** An AGENT (not TEAM_LEADER) calls `ContactPreferenceService.setDoNotContact()`. The service throws `InsufficientRoleException` and the DNC flag is not changed.

---

**AC-COMMS-15 — Contact history timeline shows all event types in chronological order**
*Requirement IDs: CC.11, DIC.3, UI.2, UAAF.13*
*Ruling: none — operational requirement*

Given customer C has the following events: an outbound letter (3 days ago), an inbound call (yesterday), an agent note (this morning), and a suppressed SMS (this morning — suppressed due to frequency limit)
When an AGENT calls `ContactHistoryService.getCustomerHistory(customerId, fromDate, toDate)`
Then the response contains all four events in descending chronological order (most recent first)
And each event includes: `event_type`, `channel`, `direction`, `status`, `created_at`, and `initiated_by`
And the suppressed SMS event includes the `primary_reason_code = FREQUENCY_LIMIT_REACHED` from its linked `communication_suppression_decision` record
And suppressed events are not hidden — they appear in the timeline with `status = SUPPRESSED`

**Negative case:** Requesting the history for a `customerId` that does not exist returns an empty list (not a 404), consistent with the "no communications yet" state for a new customer.

---

## Handoff Declaration

- **Completed:** Communications & Multi-Channel Contact Orchestration domain pack written — 7 core data tables, 2 Tier 1 configuration tables, 2 Tier 2 DMN tables, 6 Java service interfaces, full Flowable integration specification including `SendCommunicationCommand` and RULING-011 suppression-lift disposition, Admin UI specification (3 screens), RBAC matrix (7 roles × 14 permissions), 4 integration point specifications, detailed `CommunicationSuppressionService` evaluation sequence (8 steps, locked), 5 open questions, and 15 acceptance criteria.
- **Files changed:** `docs/project-foundation/domain-packs/communications-domain-pack.md` (created)
- **Requirement IDs covered:** CC.1, CC.4, CC.8, CC.11, CC.15, CC.21, CC.23, CC.24, CC.27, CC.30, CC.31, CC.32, CC.33, CC.34, CC.35, DIC.3, DIC.8, DIC.14, DIC.15, DIC.25, DIC.27, DIC.29, DW.33, DW.34, DW.43, DW.51, DW.53, DW.55, DW.59, DW.65, DW.66, DW.67, DW.68, DW.69, DW.72, DW.73, UI.2, AAD.16, CAS.COM-07, CAS.COM-08 — all sourced to `Tender requirements docs/Functional-Requirements-Consolidated.md`
- **Domain rulings used:** RULING-001 (breathing space communication suppression), RULING-011 (queued communication disposition on suppression lift)
- **ACs satisfied:** AC-COMMS-01 through AC-COMMS-15 (15 acceptance criteria, covering golden paths, negative cases, statutory non-negotiables, and operational workflows)
- **ACs not satisfied:** None declared. Note: AC-COMMS-08 (channel fallback) uses LETTER as a universal fallback pending DDE-OQ-COMMS-03. The AC remains testable with this assumption but must be revised once DWP confirms the fallback order.
- **Assumptions made:**
  1. LETTER is used as the universal channel fallback in `channel-selection.dmn` pending DDE-OQ-COMMS-03. This is an assumption, not a DWP policy decision.
  2. Agent notes (`AGENT_NOTE`) are not subject to `CommunicationSuppressionService` evaluation — they are internal records, not external communications to the customer.
  3. The `SYSTEM` RBAC role covers both Flowable delegate dispatch and batch processes. No distinction is made between them for suppression purposes.
  4. In-app (IN_APP) channel is included in the schema as a declared enum value but without a `domain/integration` adapter, pending DDE-OQ-COMMS-05.
  5. Joint account communications (AC-COMMS-11) create separate `communication_event` records per customer. Account-level vs customer-level suppression semantics for joint accounts are addressed in RULING-004 — this pack defers to that ruling for the joint debt split model and evaluates suppression at the customer level.
- **Open questions:**
  - DDE-OQ-COMMS-01 — Agent-initiated call suppression scope — owner: DWP Debt Domain Expert
  - DDE-OQ-COMMS-02 — Sliding vs calendar frequency window — owner: DWP policy team (**BLOCKING** for step 6 implementation)
  - DDE-OQ-COMMS-03 — Channel fallback order — owner: DWP policy team (**BLOCKING** for `channel-selection.dmn` production deployment)
  - DDE-OQ-COMMS-04 — Returned mail SLA — owner: DWP policy team / operational policy
  - DDE-OQ-COMMS-05 — In-app message scope — owner: Delivery Lead / DWP product owner
- **Next role:** Delivery Designer (for API contract draft, module boundary confirmation, and BPMN subprocess design for `SendCommunicationDelegate`, suppression-lift disposition handler, and returned mail work item creation)
- **What they need:** This domain pack in full; ADR-001 (`CommunicationSuppressionService` interface contract); RULING-001 (breathing space suppression); RULING-011 (queued communication disposition); the vulnerability domain pack (for `domain/customer` integration points); `ARCHITECTURE-BLUEPRINT.md` for package boundary conventions; `MASTER-DESIGN-DOCUMENT.md` for `domain/integration` adapter interface conventions.

