# RULING-013 Implementation Checklist — Sprint 3 & 4

**Ruling:** RULING-013 (Statute-Barred Clock Reset Timing)  
**Status:** Implementation-ready (pending DWP confirmation of DDE-OQ choices)  
**Target Sprint:** Sprint 3 (5 May – 4 June 2026)  
**Developers:** Dev 1, Dev 2  

---

## Pre-Implementation Gate

- [ ] **DWP Client Confirmation Received**
  - [ ] DDE-OQ-11: Evaluator timing approach (A, B, or C) confirmed by Delivery Lead
  - [ ] DDE-OQ-12: Suppression log integration (Yes/No) confirmed
  - [ ] DDE-OQ-13: Grace period requirement clarified or defaulted
  - [ ] Delivery Lead has filed the confirming document in `/docs/project-foundation/domain-rulings/DDE-OQ-STATUTE-BARRED-DECISIONS.md`

---

## Phase 1: Foundation (Week 1, 5 May – 9 May)

### 1.1 Database Schema Updates

- [ ] **V024_statute_barred_schema_update.sql** (Flyway migration)
  - [ ] Add `DEBT_ACCOUNT.clock_reset_at TIMESTAMP` column (nullable; populated when clock resets)
  - [ ] Create index on `(account_id, clock_reset_at)` for grace period queries
  - [ ] Add `SYSTEM_CONFIG` seed rows:
    ```sql
    INSERT INTO system_config (config_key, config_value, config_type) VALUES
    ('statute_barred_calculation.enabled', 'true', 'BOOLEAN'),
    ('statute_barred_calculation.nightly_job_enabled', 'true', 'BOOLEAN'),
    ('statute_barred_calculation.async_event_listener_enabled', 'true', 'BOOLEAN'),
    ('statute_barred_calculation.grace_period_days', '0', 'INTEGER'), -- 0 = no grace period
    ('limitation_period.OVERPAYMENT', '6', 'INTEGER'),
    ('limitation_period.CONTRACTED_DEBT', '6', 'INTEGER'),
    ('limitation_period.COURT_ORDERED', '6', 'INTEGER'),
    ('limitation_period.OTHER', '6', 'INTEGER');
    ```
  - [ ] If DDE-OQ-12 = Yes: Add `STATUTE_BARRED_STATUTORY` to `suppression_reason` enum

- [ ] **AUDIT_EVENT Schema Check**
  - [ ] Confirm `AUDIT_EVENT` table exists with columns: `event_type`, `account_id`, `old_value`, `new_value`, `reason`, `triggered_by`, `created_at`, `created_by`
  - [ ] Add index on `(account_id, created_at)` for audit queries

---

### 1.2 Service Layer: StatuteBarredEvaluator

**File:** `backend/src/main/java/uk/dwp/dcms/account/service/StatuteBarredEvaluator.java`

- [ ] **Constructor & Initialization**
  - [ ] Accept `SystemConfigService`, `DebtAccountRepository`, `AuditEventService`
  - [ ] On `@PostConstruct`:
    - [ ] Load all `limitation_period.*` config values
    - [ ] Parse as integers (years); throw `ApplicationStartupException` if invalid
    - [ ] Log loaded periods: "OVERPAYMENT=6, CONTRACTED_DEBT=6, ..."

- [ ] **Core Method: `recalculate(UUID accountId)`**
  - [ ] Signature: `public void recalculate(UUID accountId) throws StatuteBarredEvaluationException`
  - [ ] Load account from DB (catch if not found)
  - [ ] Get `cause_of_action_date` from account (skip if null; log WARN)
  - [ ] Get `last_acknowledgement_date` from `DEBT_STATUS_HISTORY` (or use cause_of_action_date if none found)
  - [ ] Query most recent `DEBT_STATUS_HISTORY` entry with `limitation_clock_reset = true`; use its `changed_at` as the effective clock-reset date
  - [ ] Get `limitation_period` for the account's debt type from config
  - [ ] Calculate `statute_barred_date = clock_reset_date + limitation_period_years`
  - [ ] Compare to today:
    - [ ] If today >= statute_barred_date: set `is_statute_barred = true`
    - [ ] If today < statute_barred_date: set `is_statute_barred = false`
  - [ ] If flag value changed:
    - [ ] Update `DEBT_ACCOUNT.is_statute_barred` in a `@Transactional` method
    - [ ] Write `AUDIT_EVENT` with reason, old/new values (see guardrail 7, RULING-013)
    - [ ] If transitioning `true → false`: trigger `STATUTE_BARRED_CLEARED` signal (see Phase 2)
  - [ ] Return (void; side effects via DB writes)

- [ ] **Logging & Error Handling**
  - [ ] Log at INFO level: `"Evaluated account [id] — statute_barred=[true|false] (clock reset: [date])"`
  - [ ] Log at WARN level: `"Account [id] has no cause_of_action_date; skipping evaluation"`
  - [ ] Catch and wrap `DataAccessException`: throw `StatuteBarredEvaluationException` with account ID for caller to handle

- [ ] **Tests**
  - [ ] Unit test: recalculate() when clock reset is 6+ years old → flag true
  - [ ] Unit test: recalculate() when clock reset is <6 years old → flag false
  - [ ] Unit test: recalculate() with null cause_of_action_date → logged WARN, flag unchanged
  - [ ] Unit test: recalculate() with missing limitation_period config → startup exception

---

### 1.3 Job: StatuteBarredCalculationJob (Nightly Batch)

**File:** `backend/src/main/java/uk/dwp/dcms/scheduled/StatuteBarredCalculationJob.java`

- [ ] **Class Structure**
  - [ ] Annotation: `@Component`
  - [ ] Inject: `StatuteBarredEvaluator`, `DebtAccountRepository`, `SystemConfigService`, `AuditEventService`

- [ ] **Scheduled Method**
  - [ ] Annotation: `@Scheduled(cron = "0 2 * * *")` (02:00 UTC nightly)
  - [ ] Signature: `public void evaluateAllAccounts()`
  - [ ] Logic:
    ```
    if not statute_barred_calculation.enabled:
      log WARN "Statute-barred calculation disabled; exiting"
      return
    
    log INFO "Starting nightly statute-barred evaluation..."
    accounts = debtAccountRepository.findAll()
    successCount = 0
    failureCount = 0
    
    for each account:
      try:
        statuteBarredEvaluator.recalculate(account.id)
        successCount++
      catch StatuteBarredEvaluationException:
        log WARN "Failed to evaluate account [id]: [exception]"
        failureCount++
        (continue; do not fail job)
    
    log INFO "Statute-barred evaluation complete: [successCount] succeeded, [failureCount] failed"
    if failureCount > 0:
      log WARN "Review failed accounts in AUDIT_EVENT with reason=EVALUATION_FAILED"
    ```

- [ ] **Error Handling**
  - [ ] Job must not fail if individual account evaluation fails; log and continue
  - [ ] Log `failureCount` to allow post-run inspection

- [ ] **Feature Flag**
  - [ ] Check `statute_barred_calculation.nightly_job_enabled` at start; if false, exit
  - [ ] Config key default: `true` (seed in migration)

- [ ] **Tests**
  - [ ] Integration test: Job runs; all accounts re-evaluated; flags updated correctly
  - [ ] Integration test: Job disabled via config; exits immediately with WARN log

---

### 1.4 Event Listener (Option B: Async Post-Commit) or Event Hook (Option A/C)

**File:** `backend/src/main/java/uk/dwp/dcms/account/event/LimitationClockResetEventListener.java`

Choose implementation based on DDE-OQ-11 decision:

#### Option A: Synchronous

- [ ] **Class Structure**
  - [ ] Annotation: `@Component`
  - [ ] Implement `ApplicationListener<LimitationClockResetEvent>` (Spring event)
  - [ ] Inject: `StatuteBarredEvaluator`

- [ ] **Event Handler**
  - [ ] Signature: `public void onApplicationEvent(LimitationClockResetEvent event)`
  - [ ] Logic:
    ```
    accountId = event.getAccountId()
    log DEBUG "LimitationClockResetEvent received for account [accountId]"
    try:
      statuteBarredEvaluator.recalculate(accountId)
    catch StatuteBarredEvaluationException as e:
      log ERROR "Failed to recalculate statute-barred status for account [accountId]"
      (do not re-throw; log and allow process to continue)
    ```

- [ ] **Event Publishing**
  - [ ] Where: In any service method that writes `DEBT_STATUS_HISTORY.limitation_clock_reset = true`
  - [ ] Example:
    ```java
    // In PaymentService.markAsCleared() or RepaymentArrangementService.create()
    @Transactional
    public void createArrangement(...) {
      // ... create arrangement ...
      debtStatusHistoryRepository.save(debtStatusHistory with limitation_clock_reset=true);
      // (end transaction here)
    }
    
    // Outside transaction; publish event
    public void createArrangement(...) { // outer method, non-transactional
      createArrangementInternal(...); // transactional method above
      eventPublisher.publishEvent(new LimitationClockResetEvent(accountId));
    }
    ```

#### Option B: Asynchronous (Recommended)

- [ ] **Class Structure**
  - [ ] Annotation: `@Component`
  - [ ] Implement `ApplicationListener<LimitationClockResetEvent>`
  - [ ] Inject: `StatuteBarredEvaluator`, `TaskScheduler` (Spring)

- [ ] **Event Handler**
  - [ ] Signature: `public void onApplicationEvent(LimitationClockResetEvent event)`
  - [ ] Logic:
    ```
    accountId = event.getAccountId()
    log DEBUG "LimitationClockResetEvent received for account [accountId]; queueing async evaluation"
    taskScheduler.schedule(
      () -> {
        try:
          statuteBarredEvaluator.recalculate(accountId)
        catch StatuteBarredEvaluationException as e:
          log ERROR "Async statute-barred recalculation failed for account [accountId]"
          // Nightly job will retry
      },
      Instant.now().plus(100, ChronoUnit.MILLIS) // 100ms delay before async
    )
    ```

#### Option C: Nightly Only (No Event Listener)

- [ ] Do not implement event listener; rely on `StatuteBarredCalculationJob` nightly run only

- [ ] **If DDE-OQ-12 = Yes (Suppression Log):** Manually trigger suppression lift at nightly job end
  - [ ] After all accounts evaluated, query for accounts with `is_statute_barred = false` and suppression reason `STATUTE_BARRED_STATUTORY` active
  - [ ] Lift suppression for those accounts

---

### 1.5 Event Definition

**File:** `backend/src/main/java/uk/dwp/dcms/account/event/LimitationClockResetEvent.java`

- [ ] **Class Structure** (if not already exists)
  - [ ] Extend `ApplicationEvent`
  - [ ] Fields: `accountId (UUID)`, `resetDate (LocalDate)`, `resetReason (String)` (for audit)
  - [ ] Constructor: `public LimitationClockResetEvent(UUID accountId, LocalDate resetDate, String resetReason)`
  - [ ] Getters: `getAccountId()`, `getResetDate()`, `getResetReason()`

---

### 1.6 Audit Event Creation

**File:** `backend/src/main/java/uk/dwp/dcms/audit/service/AuditEventService.java` (extend if needed)

- [ ] **Method: logStatuteBarredFlagChange()**
  - [ ] Signature: `public void logStatuteBarredFlagChange(UUID accountId, boolean oldValue, boolean newValue, String reason, String triggeredBy)`
  - [ ] Create `AUDIT_EVENT` record with:
    - [ ] `event_type = "STATUTE_BARRED_FLAG_CHANGED"`
    - [ ] `account_id = accountId`
    - [ ] `old_value = oldValue.toString()`
    - [ ] `new_value = newValue.toString()`
    - [ ] `reason = reason` (e.g., "CLOCK_RESET_EVENT", "LIMITATION_EXPIRED", "ADMIN_OVERRIDE")
    - [ ] `triggered_by = triggeredBy` (e.g., "NIGHTLY_JOB", "EVENT_LISTENER", "MANUAL_OVERRIDE")
    - [ ] `created_at = NOW()`
    - [ ] `created_by = system_user_uuid`
  - [ ] Write to DB

- [ ] **Tests**
  - [ ] Unit test: Log flag change; AUDIT_EVENT created with correct fields

---

## Phase 2: BPMN Integration (Week 2, 12 May – 16 May)

### 2.1 COLLECTION_PROCESS.bpmn Update

**File:** `backend/src/main/resources/processes/COLLECTION_PROCESS.bpmn`

- [ ] **Statute-Barred Gateway**
  - [ ] At start of process: Add exclusive gateway to check `is_statute_barred`
  - [ ] If true: Route to LEGAL_REVIEW queue (parallel case creation; do not escalate)
  - [ ] If false: Continue to segmentation/champion-challenger logic

- [ ] **Service Task: ReadAccountStatuteBarredStatus**
  - [ ] Type: `Service Task`
  - [ ] Delegate class: `ReadAccountStatuteBarredStatusDelegate`
  - [ ] Logic:
    ```
    account = accountRepository.findById(execution.getVariable("accountId"))
    isStatuteBarred = account.isStatuteBarred()
    execution.setVariable("isStatuteBarred", isStatuteBarred)
    ```
  - [ ] **CRITICAL:** Read from database at execution time; do NOT use cached process variables (ADR-009)

- [ ] **Signal Handler: STATUTE_BARRED_CLEARED**
  - [ ] Type: `Signal Catch Event`
  - [ ] Signal name: `STATUTE_BARRED_CLEARED`
  - [ ] Effect: If process is suspended at LEGAL_REVIEW or other hold point, signal resumes the process or triggers re-segmentation
  - [ ] Implementation: Route to a re-evaluation service task that re-reads the flag and re-routes

- [ ] **Tests**
  - [ ] BPMN test: Account with `is_statute_barred = true` → routed to LEGAL_REVIEW
  - [ ] BPMN test: Account with `is_statute_barred = false` → routed to segmentation
  - [ ] BPMN test: Signal `STATUTE_BARRED_CLEARED` received while in LEGAL_REVIEW → process resumes

---

### 2.2 Delegate: StatuteBarredClearedDelegate

**File:** `backend/src/main/java/uk/dwp/dcms/integration/flowable/delegate/StatuteBarredClearedDelegate.java`

- [ ] **Purpose:** Handle signal to resume process after statute-barred flag clears

- [ ] **Class Structure**
  - [ ] Annotation: `@Component`
  - [ ] Implement `FlowableDelegate` or `JavaDelegate`
  - [ ] Inject: `DebtAccountRepository`, `SegmentationService`

- [ ] **Execution Logic**
  - [ ] Get `accountId` from process variable
  - [ ] Read current `is_statute_barred` from DB
  - [ ] If false:
    - [ ] Re-run segmentation logic (call `SegmentationService.segment()`)
    - [ ] Update process variables with new treatment path
    - [ ] Log `AUDIT_EVENT` with reason = "STATUTE_BARRED_CLEARED"
  - [ ] If true (flag not yet cleared):
    - [ ] Log WARN; remain suspended; await next signal

- [ ] **Tests**
  - [ ] Unit test: Signal received; flag false; process re-segmented
  - [ ] Unit test: Signal received; flag still true; process remains suspended

---

### 2.3 segmentation.dmn Update

**File:** `backend/src/main/resources/decisions/segmentation.dmn`

- [ ] **Modify entry rule or add pre-check**
  - [ ] Before all other rules: Check `is_statute_barred`
  - [ ] If true: Output `TREATMENT_PATH = LEGAL_REVIEW`
  - [ ] If false: Continue to existing rules

- [ ] **Example DMN rule (pseudo-code):**
  ```
  Rule 1 (Statute-barred check):
    If is_statute_barred = true:
      Output: TREATMENT_PATH = "LEGAL_REVIEW"
  
  Rule 2 (Vulnerability check):
    If is_statute_barred = false AND vulnerability_flag = true:
      Output: TREATMENT_PATH = "FORBEARANCE"
  
  Rule 3+ (Other rules): ...
  ```

- [ ] **Tests**
  - [ ] DMN test: Input `is_statute_barred = true` → Output `TREATMENT_PATH = LEGAL_REVIEW`
  - [ ] DMN test: Input `is_statute_barred = false, vulnerability_flag = true` → Output `TREATMENT_PATH = FORBEARANCE`

---

### 2.4 AuditEventService Integration

- [ ] Ensure `logStatuteBarredFlagChange()` is called whenever the flag transitions
- [ ] Call location: Within `StatuteBarredEvaluator.recalculate()` immediately after DB update

---

## Phase 3: Suppression Log Integration (Conditional on DDE-OQ-12)

### 3.1 Suppression Reason Enum Update (If DDE-OQ-12 = Yes)

**File:** Database schema + `SuppressionReason.java`

- [ ] **Add to enum:** `STATUTE_BARRED_STATUTORY`

- [ ] **Where:** Query `suppression_log` for this reason in `CommunicationSuppressionService`

---

### 3.2 StatuteBarredEvaluator: Suppression Log Activation (If DDE-OQ-12 = Yes)

- [ ] **On flag transition `false → true`:**
  - [ ] Activate suppression reason `STATUTE_BARRED_STATUTORY`
  - [ ] Call: `suppressionService.activateSuppression(accountId, SuppressionReason.STATUTE_BARRED_STATUTORY, systemUserId)`

- [ ] **On flag transition `true → false`:**
  - [ ] Lift suppression reason `STATUTE_BARRED_STATUTORY`
  - [ ] Call: `suppressionService.liftSuppression(accountId, SuppressionReason.STATUTE_BARRED_STATUTORY, systemUserId)`

- [ ] **Tests**
  - [ ] Unit test: Flag transitions false → true; suppression activated
  - [ ] Unit test: Flag transitions true → false; suppression lifted

---

## Phase 4: Grace Period Implementation (If DDE-OQ-13 = Yes)

### 4.1 Database Schema

- [ ] **V025_grace_period_schema.sql** (Flyway migration)
  - [ ] Column already added in Phase 1: `DEBT_ACCOUNT.clock_reset_at TIMESTAMP`
  - [ ] Add `SYSTEM_CONFIG` entry: `statute_barred_calculation.grace_period_days` (default: 0)

---

### 4.2 StatuteBarredEvaluator: Grace Period Check

- [ ] **Modify `recalculate()` method**
  - [ ] When flag would transition `true → false`:
    - [ ] Get `grace_period_days` from config
    - [ ] If grace_period_days > 0:
      - [ ] Calculate `can_resume_at = clock_reset_at + grace_period_days`
      - [ ] If today < can_resume_at:
        - [ ] Do NOT clear flag; remain true
        - [ ] Log INFO: `"Account [id] within grace period; resuming collections on [can_resume_at]"`
      - [ ] If today >= can_resume_at:
        - [ ] Clear flag (set to false)
    - [ ] If grace_period_days == 0:
      - [ ] Clear flag immediately

- [ ] **Audit trail**
  - [ ] When skipping flag clear due to grace period, log `AUDIT_EVENT` with reason = "WITHIN_GRACE_PERIOD"

- [ ] **Tests**
  - [ ] Unit test: Clock reset 3 days ago; grace_period = 7 days → flag remains true
  - [ ] Unit test: Clock reset 8 days ago; grace_period = 7 days → flag clears to false
  - [ ] Unit test: grace_period = 0 → flag clears immediately

---

## Phase 5: Integration Testing (Ongoing, Weeks 1–4)

### 5.1 Happy Path Tests

- [ ] **Test: Payment triggers clock reset**
  - [ ] Create account, mark as statute-barred
  - [ ] Record a CLEARED payment (debtor-initiated)
  - [ ] Trigger evaluator (event listener or immediate call)
  - [ ] Assert: `is_statute_barred = false`
  - [ ] Assert: `AUDIT_EVENT` logged with reason = "CLOCK_RESET_EVENT"

- [ ] **Test: Repayment arrangement triggers clock reset**
  - [ ] Create account, mark as statute-barred
  - [ ] Create REPAYMENT_ARRANGEMENT (status = ACTIVE)
  - [ ] Trigger evaluator
  - [ ] Assert: `is_statute_barred = false`

- [ ] **Test: Acknowledgement triggers clock reset**
  - [ ] Create account, mark as statute-barred
  - [ ] Create CASE_NOTE (note_type = ACKNOWLEDGEMENT)
  - [ ] Trigger evaluator
  - [ ] Assert: `is_statute_barred = false`

---

### 5.2 BPMN Workflow Tests

- [ ] **Test: Statute-barred account routes to LEGAL_REVIEW**
  - [ ] Create account, set `is_statute_barred = true`
  - [ ] Start COLLECTION_PROCESS instance
  - [ ] Assert: Process routed to LEGAL_REVIEW queue (parallel case created)

- [ ] **Test: Non-statute-barred account continues to segmentation**
  - [ ] Create account, set `is_statute_barred = false`
  - [ ] Start COLLECTION_PROCESS instance
  - [ ] Assert: Process continues to segmentation/champion-challenger logic

- [ ] **Test: STATUTE_BARRED_CLEARED signal resumes process**
  - [ ] Create statute-barred account in LEGAL_REVIEW
  - [ ] Trigger flag clearance (payment event)
  - [ ] Assert: Signal sent to process instance
  - [ ] Assert: Process re-evaluates and re-routes

---

### 5.3 Configuration Tests

- [ ] **Test: Feature flag disables calculation**
  - [ ] Set `statute_barred_calculation.enabled = false`
  - [ ] Run nightly job
  - [ ] Assert: Job exits with WARN log; no accounts evaluated

- [ ] **Test: Missing config value causes startup exception**
  - [ ] Remove `limitation_period.OVERPAYMENT` from config
  - [ ] Start application
  - [ ] Assert: Application startup fails with clear error message

---

### 5.4 Error Handling Tests

- [ ] **Test: Evaluation failure in nightly job (Option A/B/C)**
  - [ ] Mock `accountRepository.findAll()` to throw exception on 2nd account
  - [ ] Run nightly job
  - [ ] Assert: Job continues; logs exception; completes with partial results

- [ ] **Test: Async evaluator failure (Option B)**
  - [ ] Mock `StatuteBarredEvaluator.recalculate()` to throw exception
  - [ ] Publish `LimitationClockResetEvent`
  - [ ] Assert: Exception caught and logged; process continues
  - [ ] Assert: Nightly job will retry next run

---

## Phase 6: Demo Preparation (Week 9, 30 Jun – 4 Jul)

### 6.1 Demo Scenario

**Scenario:** Vulnerable customer in statute-barred account makes a cleared payment.

- [ ] **Setup:**
  - [ ] Account: Overpayment debt, cause_of_action_date = 10 Jan 2020 (6+ years ago)
  - [ ] Flag: `is_statute_barred = true`
  - [ ] Status: ACTIVE
  - [ ] Vulnerability: `vulnerability_flag = true`
  - [ ] Treatment path: LEGAL_REVIEW (due to statute-barred check in DMN)

- [ ] **Live Demo Steps:**
  1. Load account detail screen; show `is_statute_barred = true` in read-only field
  2. Navigate to Payments; create new CLEARED payment (£100, debtor-initiated)
  3. Payment saved; trigger evaluator (event listener or manual call)
  4. Show system logs: "LimitationClockResetEvent received...", "Evaluated account — statute_barred=false"
  5. Reload account detail screen; show flag now `false`
  6. Check AUDIT_EVENT table; show entry with reason = "CLOCK_RESET_EVENT"
  7. If BPMN running: Show COLLECTION_PROCESS instance; demonstrate signal `STATUTE_BARRED_CLEARED` received and process re-routed (to FORBEARANCE, not LEGAL_REVIEW)

- [ ] **Demo Script:**
  ```
  Demo operator: "This account was statute-barred 6 years after the cause of action. 
  The debtor has now made a payment, which resets the limitation clock under ss.29–30 
  of the Limitation Act 1980. Let's record that payment and see how the system handles it."
  
  [Record payment]
  
  "The evaluator has now re-calculated the clock. The account is no longer 
  statute-barred. The collection process can now resume — in this case, with 
  forbearance due to the vulnerability flag. The system has automatically sent 
  a signal to re-route the workflow."
  ```

---

### 6.2 Configuration for Demo

```yaml
statute_barred_calculation:
  enabled: true
  nightly_job_enabled: true
  async_event_listener_enabled: true
  grace_period_days: 0  # No grace period for demo; show immediate effect

limitation_period:
  OVERPAYMENT: 6
  CONTRACTED_DEBT: 6
  COURT_ORDERED: 6
  OTHER: 6
```

---

## Acceptance Criteria (From Requirements)

- [ ] **AC-001:** When `DEBT_STATUS_HISTORY.limitation_clock_reset = true` is recorded, the `StatuteBarredEvaluator` re-calculates `DEBT_ACCOUNT.is_statute_barred` within 100ms (async) or synchronously (depending on DDE-OQ-11 choice).
- [ ] **AC-002:** When `is_statute_barred` transitions `true → false`, a `STATUTE_BARRED_CLEARED` signal is sent to any active `COLLECTION_PROCESS` instance for the account.
- [ ] **AC-003:** All flag transitions are logged to `AUDIT_EVENT` with reason, old value, new value, and trigger source (nightly job, event listener, or manual override).
- [ ] **AC-004:** The `COLLECTION_PROCESS.bpmn` reads `is_statute_barred` at task execution time from the database; it does not cache the value in process variables.
- [ ] **AC-005:** A statute-barred account (flag `true`) is routed to LEGAL_REVIEW queue, not escalated to enforcement.
- [ ] **AC-006:** The nightly job respects the `statute_barred_calculation.enabled` feature flag; if false, it exits with a WARN log.
- [ ] **AC-007:** If a grace period is configured, the flag is not cleared until `DATEDIFF(NOW(), clock_reset_at) >= grace_period_days`.

---

## Code Review Checklist

Before merging Phase 1–4 features:

- [ ] **Evaluator service**
  - [ ] No direct references to Flowable inside `StatuteBarredEvaluator` (Flowable calls go outside transaction per ADR-003)
  - [ ] Config validation on `@PostConstruct` is thorough
  - [ ] Audit logging is comprehensive (old/new values, reason, trigger source)

- [ ] **Nightly job**
  - [ ] Feature flag is checked and logged
  - [ ] Errors in individual account evaluations do not fail the job
  - [ ] Success/failure counts are logged
  - [ ] Job runs at a reasonable time (02:00 UTC) to minimize user impact

- [ ] **Event listener (Option A/B)**
  - [ ] Event is published outside the `@Transactional` boundary
  - [ ] Listener is non-blocking (especially for Option B)
  - [ ] Errors are caught and logged; process continues

- [ ] **BPMN updates**
  - [ ] Statute-barred check is the first gateway in COLLECTION_PROCESS
  - [ ] Database read at execution time (not cached process variables)
  - [ ] Signal handler is properly wired
  - [ ] Re-segmentation logic is correct

- [ ] **DMN updates**
  - [ ] Statute-barred rule is the top rule (priority)
  - [ ] Output for true case is LEGAL_REVIEW
  - [ ] No conflicts with other rules

- [ ] **Tests**
  - [ ] Coverage > 85% for StatuteBarredEvaluator and related services
  - [ ] Integration tests cover happy path + error cases
  - [ ] BPMN tests verify routing logic
  - [ ] All open questions (DDE-OQ-11/12/13) are addressed in implementation choices

---

## Sign-Off

- [ ] **Sprint 3 Acceptance:** All Phase 1–2 stories marked complete; code reviewed and merged to main
- [ ] **Sprint 4+ Acceptance:** Phases 3–4 (if required) completed; demo scenario tested
- [ ] **Demo Ready:** Demo scenario runs successfully; narrative explained to panel
- [ ] **RULING-013 Status Updated:** Status changed from `awaiting-client-sign-off` to `final` once DWP confirms DDE-OQ choices

---

## Appendix: Common Pitfalls to Avoid

1. **Caching flag in process variables:** Do NOT read flag once and reuse it in the process. Always read from DB at decision points.
2. **Forgetting ADR-003 transaction boundary:** Do NOT call Flowable services inside a `@Transactional` method. Call them outside.
3. **Missing audit events:** Every flag transition must log to AUDIT_EVENT. Do not skip "internal" transitions.
4. **Not publishing event:** The event listener will never fire if the event is not published. Check the publication point.
5. **Not setting grace period config:** If DDE-OQ-13 requires grace period, ensure it's seeded in the database. Default to 0 if client doesn't specify.
6. **Feature flag not checked:** The nightly job MUST check the feature flag. Do not assume it's enabled in all environments.

---

**Last Updated:** 23 Apr 2026  
**Status:** Implementation-ready (pending DWP sign-off on DDE-OQ decisions)
