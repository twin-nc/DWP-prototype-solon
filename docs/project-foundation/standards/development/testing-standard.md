---
id: STD-DEV-001
title: Testing Standard
status: Approved
owner: Engineering
applies_to: All services, portals, and pipelines
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - governance/test-authority-and-truth-hierarchy.md
  - governance/release-evidence-and-signoff.md
  - platform/determinism.md
last_changed: 2026-04-07
---

## Purpose

Define minimum test coverage requirements, test naming conventions, test data management, CI gate definitions, and test quality rules to ensure every release is backed by objective, reproducible evidence.

---

## Test Layers

### Unit Tests
- **Required for:** all domain logic, all rule computation, all state transitions, all validation logic, all utility functions with non-trivial behavior.
- **Scope:** single class or function in isolation. No I/O, no database, no network.
- **Framework:** per project tech stack (e.g., JUnit 5 + Mockito for Java; Vitest for TypeScript).

### Integration Tests
- **Required for:** all persistence logic (repositories, queries), all API controller behavior, all external service adapters, all event handlers.
- **Scope:** single service component with real infrastructure (real DB, test containers, etc.). No mocks for the layer under test.
- **Framework:** per project tech stack (e.g., Spring Boot Test + Testcontainers for Java; Supertest for TypeScript).

### End-to-End (E2E) Tests
- **Required for:** each user-facing workflow. Minimum one happy-path test + one error-path test per workflow.
- **Scope:** full stack from UI or API entry point to database and back.
- **Framework:** per project tech stack (e.g., Playwright for frontend; REST Assured for API-level E2E).

### Contract Tests
- **Required for:** all external API contracts (OpenAPI/AsyncAPI). Must validate runtime against the published spec.
- **Scope:** provider-side: does the runtime match the spec? Consumer-side: does the consumer handle the contract correctly?

---

## Coverage Requirements

| Layer | Minimum Coverage | Measured By |
|---|---|---|
| Unit | 80% line coverage for domain and rule packages | Coverage report in CI |
| Integration | All repository methods and API endpoints exercised | Test run evidence |
| E2E | All documented user workflows (at least 1 happy + 1 error path each) | Test plan traceability |
| Contract | All external contracts validated on every build | Parity check in CI |

Coverage thresholds are enforced in CI — a build below the unit coverage threshold is **not eligible for merge**.

---

## Test Naming Convention

Use a consistent naming pattern so failing tests are immediately understandable:

**Java / JUnit:**
```java
@Test
void givenExpiredToken_whenSubmitFiling_thenReturns401()
```

**TypeScript / Vitest / Jest:**
```typescript
describe('FilingService', () => {
  it('returns 401 when token is expired', async () => { ... })
})
```

Pattern: `<subject> <condition> <expected outcome>`

---

## Test Data Management

- Tests MUST be deterministic and independent — no shared mutable state between tests.
- Use deterministic seed factories or builders; avoid literals scattered across tests.
- Test data MUST NOT rely on execution order.
- Production data MUST NOT be used in any test environment. Use anonymised or synthetic data only.
- Database-backed tests MUST reset state between tests (transactions, test containers, or explicit cleanup).

---

## CI Gate Definitions

| Gate | Trigger | Failure Behaviour |
|---|---|---|
| Unit + Integration | Every PR | PR is not eligible for merge |
| E2E | Merge to `main` | Build is not eligible for deployment |
| Contract parity | Every build intended for release | Build is not eligible for promotion |
| Coverage threshold | Every PR | PR is not eligible for merge |

Gate definitions MUST be declared in the CI workflow file and referenced in the release evidence pack.

---

## Test Quality Rules

1. No test may pass by relying on execution order.
2. Tests that are non-deterministic (e.g., depend on wall-clock time without mocking) are not permitted as CI gates.
3. Tests must assert behavior, not implementation — avoid asserting on private method calls or internal field values.
4. A test that never fails is not a test. Review tests that always pass.
5. Snapshot / golden tests are authoritative only when derived from approved requirement behavior — see `governance/test-authority-and-truth-hierarchy.md`.

---

## Test Plan Traceability

For every feature or change:
- Each test case MUST map to a requirement ID or acceptance criterion.
- The trace map MUST be updated in the same cycle as the change.
- Test run IDs and reports MUST be included in the release evidence pack.
