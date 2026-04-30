---
name: flaky-test-triage
description: >
  Diagnose and fix intermittently failing tests — classifying the flake cause
  (timing, fixture isolation, shared state, non-deterministic data), applying a
  targeted fix, and confirming stability over multiple runs.
invocation: /flaky-test-triage
inputs:
  - name: test_identifier
    required: true
    description: "Class name, method name, or test file path of the failing test (e.g. RepaymentPlanServiceTest#shouldCreatePlan)"
  - name: failure_log
    required: false
    description: Stack trace or assertion failure output from the flaky run
  - name: run_count
    required: false
    description: "How many times the test has been observed to fail (helps distinguish genuine flake from consistent failure)"
outputs:
  - name: triage_report
    description: Flake category, root cause, fix applied, and stability verification result
roles:
  - backend-builder
  - frontend-builder
  - test-builder
---

# Flaky Test Triage

## Purpose
A flaky test is one that fails intermittently without a code change. It is worse than a consistently failing test — it erodes trust in the suite, causes false CI blocks, and often masks a real concurrency or isolation bug that will eventually cause a production incident.

This skill classifies the flake cause, applies a targeted fix, and verifies stability by running the test multiple times before reporting it fixed.

**Scope: isolated flaky-test triage.** This skill is for a single test (or a small set of related tests) that are intermittently failing. If a PR has multiple failing checks that share a root cause, use `ci-failure-root-cause-fixer` — it handles broader CI triage with a consolidated fix strategy.

## When to Use
- A test passes locally but fails intermittently in CI
- A test fails on some machines but not others
- A test passes in isolation but fails when the full suite runs
- The same test has failed on multiple unrelated PRs with no associated code change
- CI shows a test failure that disappears on rerun without any fix

## Do Not Use This Skill When
- **The test fails consistently on every run** — this is a regression, not a flake. Use `ci-failure-root-cause-fixer` or fix the code directly.
- **Multiple unrelated tests are failing** — investigate shared infrastructure first; use `ci-failure-root-cause-fixer`.
- **The test does not compile** — use `java-build-resolver` (backend) or fix the TypeScript error directly (frontend).

## Flake Categories

### Category 1 — Timing / Race Condition
**Signal:** Test passes when run slowly or in isolation; fails under load or parallel execution. Assertion on an async result that sometimes resolves after the assertion fires.

**Backend signals:** `AssertionError` on a value that should have been written by an async operation; `TransactionTimeoutException`; `OptimisticLockException` appearing intermittently.

**Frontend signals:** `waitFor` timeout in React Testing Library; element not found despite being rendered; state update not reflected before assertion.

Fix sequence:
1. Identify the async operation the test is asserting against.
2. Backend: replace `Thread.sleep()` with `Awaitility.await().untilAsserted(...)` with a reasonable timeout (5s default).
3. Frontend: wrap assertions in `await waitFor(() => ...)` rather than asserting synchronously after an async trigger.
4. Never increase sleep durations as a fix — they make the suite slower without eliminating the race.

### Category 2 — Test Isolation Failure (Shared State)
**Signal:** Test passes in isolation (`mvn test -Dtest=<ClassName>`) but fails when the full suite runs. Failure is in state-dependent assertions.

**Backend signals:** Database rows from a prior test are visible; a singleton bean has been mutated by a prior test; an in-memory cache was populated by a prior test.

**Frontend signals:** A global store (Redux/Zustand) carries state from a prior test; a mocked module was not reset between tests.

Fix sequence:
1. Backend: confirm the test class is annotated with `@Transactional` (rolls back after each test) or uses `@Sql` with `executionPhase = AFTER_TEST_METHOD` to clean up. If using `@SpringBootTest`, prefer `@DirtiesContext` only as a last resort — it is expensive.
2. Backend: if a singleton bean is mutated, reset it in a `@BeforeEach` / `@AfterEach` method.
3. Frontend: confirm `beforeEach` calls `store.dispatch(resetState())` or equivalent; confirm `jest.resetAllMocks()` / `jest.clearAllMocks()` is called in `afterEach`.
4. Frontend: for module mocks, use `jest.isolateModules()` if state bleeds between test files.

### Category 3 — Non-Deterministic Data
**Signal:** Assertion depends on ordering, timestamps, UUIDs, or random values that differ between runs.

Fix sequence:
1. Replace `new Date()` / `LocalDateTime.now()` in test fixtures with a fixed clock. Backend: inject a `Clock` bean and stub it in tests with `Clock.fixed(...)`. Frontend: use `jest.useFakeTimers()` with `jest.setSystemTime(new Date('2026-01-01'))`.
2. Replace sort-order assertions on unordered collections with `assertThat(result).containsExactlyInAnyOrder(...)` rather than `containsExactly(...)`.
3. Never assert on UUID values — assert on the presence and shape of a UUID field, not its specific value.

### Category 4 — External Dependency / Network Call
**Signal:** Test passes with network access; fails in CI due to firewall rules, DNS, or rate limits. Flake correlates with network latency.

Fix sequence:
1. Identify the external call — HTTP client, SMTP, S3, Keycloak token endpoint.
2. Backend: introduce a WireMock stub for the endpoint. If WireMock is not already in the test dependencies, add `wiremock-standalone` to `pom.xml` test scope.
3. Backend: for Keycloak JWT validation in tests, configure `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` in `application-test.yml` to point to a WireMock stub rather than a live issuer URI.
4. Frontend: mock the fetch/axios call with `jest.spyOn(global, 'fetch').mockResolvedValue(...)` or use `msw` (Mock Service Worker) if already in the project.
5. Never allow tests to make real network calls — they are inherently non-deterministic in CI.

### Category 5 — Test Execution Order Dependency
**Signal:** A specific test fails only when a particular other test runs before it. Identified by running the suite in a fixed order and bisecting.

Fix sequence:
1. Find the test that mutates shared state and ensure it cleans up (see Category 2).
2. If the dependency is intentional (integration test sequence), make it explicit with `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` and `@Order` — but prefer isolation over ordering.
3. Backend: run `mvn test -pl backend -Dsurefire.runOrder=random` several times to surface order dependencies systematically.

### Category 6 — CI Environment Difference
**Signal:** Test always passes locally, always or frequently fails in GitHub Actions. No shared-state or timing signal.

Fix sequence:
1. Check for environment differences: file system case sensitivity (Linux CI vs Windows/Mac local), locale/timezone (`TZ` env var), available memory or CPU count affecting thread pools.
2. Check for missing env vars in the CI workflow — compare `env:` blocks in the GitHub Actions YAML against what is set locally.
3. For timezone issues: set `@BeforeEach` to `TimeZone.setDefault(TimeZone.getTimeZone("UTC"))` or use `Clock.fixed()` (Category 3 fix).
4. For file system case issues: rename the file to match the exact case used in imports.

## Steps

1. Confirm the test is genuinely flaky — run it 5 times in isolation: `mvn test -pl backend -Dtest=<ClassName>#<method> -am` (or `npx jest <file> --testNamePattern="<name>"` for frontend). If it fails consistently, treat as a regression, not a flake.
2. Run the full suite with the failing test 3 times and note whether the failure pattern is consistent or varies.
3. Check whether the test passes in isolation but fails in the full suite — this immediately points to Category 2 or 5.
4. Match to a category above.
5. Apply the fix.
6. Run the test 10 times in isolation to confirm stability: `for i in $(seq 1 10); do mvn test -pl backend -Dtest=<ClassName>#<method> -am -q; done` — all 10 must pass.
7. Run the full suite once to confirm no regression.
8. Produce the output contract.

## Output Contract

```
## Flaky Test Triage Report

Test: <class#method or file/describe/it>
Layer: <backend | frontend>
Flake category: <Category 1-6>
Root cause: <one sentence>
Evidence: <what confirmed this classification — isolation run, timing pattern, etc.>
Fix applied: <description of the change>
Stability verification: <N/10 runs passed in isolation>
Full-suite verification: <PASS / FAIL>
Result: STABLE / STILL FLAKY
Residual risk: <any follow-on risk or "none">
```

## Guardrails
- Do not mark a test stable after fewer than 10 isolation runs — a flaky test that passes 3 times is not fixed.
- Do not increase `Thread.sleep()` durations — use `Awaitility` or `waitFor` instead.
- Do not use `@DirtiesContext` as the first fix — it reloads the full Spring context and makes the suite significantly slower. Exhaust isolation and cleanup fixes first.
- Do not mock away an external dependency without understanding why the real call was being made — the mock may hide a missing test for the integration path.
- Do not label a failure as flaky without at least two separate runs showing the same test passing and failing without a code change.

## Integration
- Use `ci-failure-root-cause-fixer` when multiple tests are failing on a PR and a shared root cause is suspected.
- Use `java-build-resolver` if the test class fails to compile.
- Use `verification-loop` after applying a fix to confirm no surrounding tests regress.
