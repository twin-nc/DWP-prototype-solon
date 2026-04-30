> ## ⚠ STATUS: UNDER REVIEW — PLATFORM PIVOT IN EFFECT
>
> A confirmed programme-level decision (ADR-018, 2026-04-30) has pivoted the project to build on top of **Solon Tax** as the base platform. All prior ADRs are under review.
>
> **This document contains accumulated knowledge and analysis that remains relevant to the new design process. It does NOT represent the current or preferred direction.** All decisions and conclusions herein are open for renewed debate. Do not treat this ADR as a directive, default position, or settled constraint.
>
> See [ADR-018](ADR-018-platform-pivot-solon-tax-confirmed.md) for the full pivot record.

---

# ADR-005: Candidate List Data Model and Case Initiation Entry Point

## Status

Accepted

## Date

2026-04-22

## Context

The DCMS receives debt referrals from DWP source systems (e.g. DWP Place, DM6). These referrals must be held in a reviewable queue before an agent promotes them to live cases. Case initiation is the atomic transaction defined in ADR-001 (Person + Account + Flowable process start). The candidate list is the staging layer that feeds that transaction.

Requirements driving this decision:

- CAS.5 — ingest unique external reference by which an account is known to the client
- DIC.1 — load and amend customer and account data automatically and manually; hold history of data loaded and amended
- DIC.7 — capture external data through interfaces or integration including APIs and batch files
- I3PS.3 — support both real-time and batched interfaces to 3rd party providers and systems
- I3PS.12 — ability to migrate customers and accounts from DM6 to FDS

Three questions were evaluated:

1. **Ingest mechanism** — REST API push vs. batch file pull vs. both
2. **Pre-initiation enrichment** — can an agent edit the candidate record before initiating the case?
3. **Module ownership** — which module owns the inbound contract and staging model?

## Decision

### Ingest mechanism

A single REST API endpoint owned by the `integration` module accepts a list of debt referrals in one call. DWP source systems push to this endpoint. Batch and polling mechanisms are out of scope until separately specified.

```
POST /api/v1/integration/candidates
Body: { "referrals": [ { ...CandidateReferralDto }, ... ] }
```

The endpoint is idempotent on `external_reference`: if a referral with the same `external_reference` already exists in any non-`REJECTED` status, it is marked `DUPLICATE` and not inserted again.

### Pre-initiation enrichment

None. The candidate record is immutable from the moment of ingest. An agent reviews the record as delivered and either initiates the case or rejects it. If data quality on the live Customer or DebtAccount record needs correction after initiation, that is done on those records — not on the candidate.

### Module ownership

The `integration` module owns:
- The REST endpoint
- The `candidate_record` table and Flyway migration
- The `CandidateReferralDto` inbound contract
- Duplicate detection logic
- The `CandidateRecord` JPA entity and repository
- The `CandidateService` — ingest and initiation orchestration

Case initiation (Person + Account + process start) is delegated to the `customer`, `account`, and `shared/process/port` modules respectively. The `integration` module calls these — it does not implement them.

### Data model

```sql
CREATE TABLE candidate_record (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_reference  VARCHAR(100) NOT NULL,
    source_system       VARCHAR(50)  NOT NULL,
    source_batch_id     UUID,
    received_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                        -- PENDING | INITIATED | REJECTED | DUPLICATE

    -- Customer identity (as delivered — immutable after ingest)
    nino                VARCHAR(9),
    forename            VARCHAR(100) NOT NULL,
    surname             VARCHAR(100) NOT NULL,
    date_of_birth       DATE         NOT NULL,
    address_line_1      VARCHAR(200),
    address_line_2      VARCHAR(200),
    town                VARCHAR(100),
    postcode            VARCHAR(10),

    -- Debt fields (as delivered — immutable after ingest)
    debt_type           VARCHAR(50)  NOT NULL,
    debt_reference      VARCHAR(100) NOT NULL,
    debt_date           DATE,
    original_amount     NUMERIC(15,2) NOT NULL,
    outstanding_balance NUMERIC(15,2) NOT NULL,
    benefit_status      VARCHAR(20),
                        -- ON_BENEFIT | OFF_BENEFIT — seeds initial segmentation (ADR-004)
                        -- This is a claim-level value from the source system at referral time,
                        -- not a Person attribute. See RULING-002 §4 for the authoritative model.

    -- Initiation outcome
    initiated_at        TIMESTAMPTZ,
    initiated_by        VARCHAR(100),  -- username of agent
    person_id           UUID,          -- FK to customer.person.id once initiated
    account_id          UUID,          -- FK to account.account.id once initiated
    rejected_reason     VARCHAR(500),

    -- Audit
    raw_payload         JSONB NOT NULL,  -- full inbound payload, preserved verbatim

    CONSTRAINT candidate_record_status_check
        CHECK (status IN ('PENDING', 'INITIATED', 'REJECTED', 'DUPLICATE')),
    CONSTRAINT candidate_record_external_ref_unique
        UNIQUE (external_reference)
);

CREATE INDEX idx_candidate_record_status ON candidate_record (status);
CREATE INDEX idx_candidate_record_received_at ON candidate_record (received_at DESC);
```

### State machine

```
PENDING ──► INITIATED   (agent initiates case — triggers ADR-001 atomic transaction)
PENDING ──► REJECTED    (agent rejects referral with reason)
[on ingest] ──► DUPLICATE  (external_reference already exists in PENDING or INITIATED)
```

`INITIATED` and `REJECTED` are terminal. `DUPLICATE` is terminal. No transitions out of terminal states.

### `raw_payload` rationale

The full inbound JSON is stored verbatim in `raw_payload` regardless of which fields are parsed into columns. This satisfies DIC.1 audit history and supports schema evolution: if DWP Place adds fields, they are preserved before the application knows what to do with them.

### `benefit_status` at referral time

The source system provides `benefit_status` (ON_BENEFIT / OFF_BENEFIT) at referral time. This value seeds the initial segmentation decision at case initiation (ADR-004). It is not authoritative after initiation — the process engine owns segment state from that point forward.

Per RULING-002 §4, `benefit_status` is a property of the UC claim/award (the Account), not of the individual Person. The candidate record carries it as a snapshot of the claim's status at the time of referral. Once the Account is live, claim status is tracked on the Account entity directly.

### Case initiation flow

1. Agent views candidate records in `PENDING` status via the frontend worklist
2. Agent selects a record and triggers "Initiate Case"
3. `CandidateService.initiateCase(candidateId, initiatingUserId)`:
   a. Loads candidate record, asserts status is `PENDING`
   b. Calls `PersonService.findOrCreate(nino, forename, surname, dateOfBirth, ...)` → returns `personId`
   c. Calls `AccountService.create(personId, debtType, debtReference, originalAmount, ...)` → returns `accountId`
   d. Calls `SegmentationService.evaluate(accountId, benefitStatus, vulnerabilityFlag)` → returns initial `segmentCode` (ADR-004; `strategy` module owns this)
   e. Calls `ProcessStartPort.startTreatment(accountId, segmentCode)` → starts Flowable process instance
   f. Updates candidate record: `status = INITIATED`, `initiated_at`, `initiated_by`, `person_id`, `account_id`
   g. All of steps b–f run within a single `@Transactional` boundary (per ADR-001)
4. Frontend redirects agent to the case view for the newly initiated DebtAccount

### Duplicate customer handling

Step 3b uses `findOrCreate` keyed on NINO. If a Person already exists for that NINO, the existing record is used and a new Account is created against it (supporting CAS.1 multi-account, CAS.16 — one Person can hold multiple Accounts per RULING-002 §3). No duplicate Person records are created.

## Consequences

- The `integration` module owns the inbound REST endpoint and the `candidate_record` table — no other module writes to this table
- Candidate records are immutable after ingest; data corrections post-initiation go to the live `person` / `account` records
- The frontend must expose a candidate worklist view (filtered by `status = PENDING`) for agents to action
- `external_reference` uniqueness is enforced at the database level — duplicate detection is a hard constraint, not a best-effort check
- `raw_payload` must be treated as Restricted data (STD-SEC-003) — it may contain PII fields not mapped to columns
- The `CandidateReferralDto` inbound contract is versioned per STD-GOV-004; changes to required fields are breaking changes

## Alternatives Rejected

### Batch file pull (polling)

DWP source systems deposit files to a shared location; DCMS polls and ingests.

**Rejected (for now):** adds operational complexity (file transfer, polling schedule, error recovery for partial files) with no benefit over a direct API push for the current scope. I3PS.3 leaves batch as an option for future specification.

### Pre-initiation enrichment on the candidate record

Agent edits candidate record fields (correct address, add phone) before initiating.

**Rejected:** adds a staging edit workflow and versioning complexity to the candidate record. Corrections belong on the live Customer record where they can be audited against the full customer history. The candidate record is a referral receipt — it should reflect exactly what the source system sent.

## References

- ADR-001: Process instance per debtor + debt pair (atomic case initiation transaction)
- ADR-004: Segment taxonomy and configurability (benefit_status seeds initial segment)
- STD-GOV-004: Contract versioning standard
- STD-SEC-003: Data classification — Restricted data handling
- CAS.5, DIC.1, DIC.7, I3PS.3, I3PS.12
