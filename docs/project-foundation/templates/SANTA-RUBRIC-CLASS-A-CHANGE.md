# Santa Method Rubric — Class A Change

Use this rubric when applying the Santa Method dual review to a Class A change (architecture impact, cross-module, schema migration, security-sensitive, or high-blast-radius).

Each reviewer scores independently. Neither reviewer sees the other's scoring until both are submitted.

---

## Reviewer Instructions

1. You are a **fresh context window** reviewer. Do not carry reasoning from the session that produced this change.
2. Read the PR diff, linked issue, and acceptance criteria only.
3. Score each dimension independently before reading the other reviewer's output.
4. Flag any item you cannot assess due to missing context — do not score it as passing.

---

## Scoring Key

| Score | Meaning |
|---|---|
| **PASS** | Criterion is fully satisfied |
| **PARTIAL** | Criterion is partially satisfied — explain gap |
| **FAIL** | Criterion is not satisfied — explain risk |
| **N/A** | Not applicable to this change — explain why |

---

## Rubric Dimensions

### 1. Requirements Traceability
| # | Criterion | Score | Notes |
|---|---|---|---|
| 1.1 | Every changed behaviour traces to a named requirement or AC | | |
| 1.2 | No behaviour is added that has no corresponding requirement | | |
| 1.3 | Removed behaviour is explicitly justified in the PR description | | |

### 2. Correctness
| # | Criterion | Score | Notes |
|---|---|---|---|
| 2.1 | Logic matches the intent described in the linked issue | | |
| 2.2 | Edge cases identified in ACs are handled | | |
| 2.3 | Error paths return the correct error envelope and status code | | |
| 2.4 | No silent failures — all exceptions are either handled or allowed to propagate | | |

### 3. State Integrity
| # | Criterion | Score | Notes |
|---|---|---|---|
| 3.1 | Terminal states are not overwritten | | |
| 3.2 | State transitions are valid per the documented state machine | | |
| 3.3 | Append-only records are never mutated | | |

### 4. Test Coverage
| # | Criterion | Score | Notes |
|---|---|---|---|
| 4.1 | Happy path is covered by at least one test | | |
| 4.2 | At least one negative/error path is covered | | |
| 4.3 | Edge cases from the ACs are covered | | |
| 4.4 | Tests are deterministic (no time/random dependencies without seeding) | | |

### 5. Security
| # | Criterion | Score | Notes |
|---|---|---|---|
| 5.1 | No secrets or credentials in code or logs | | |
| 5.2 | User input is validated at system boundaries | | |
| 5.3 | No stack traces or internal detail in API error messages | | |
| 5.4 | Auth/authz checks are present on new endpoints | | |

### 6. Observability
| # | Criterion | Score | Notes |
|---|---|---|---|
| 6.1 | Errors are logged at the appropriate level | | |
| 6.2 | Key business events emit structured log entries or metrics | | |
| 6.3 | Correlation IDs are propagated | | |

### 7. Operability
| # | Criterion | Score | Notes |
|---|---|---|---|
| 7.1 | Migrations are reversible or a rollback procedure is documented | | |
| 7.2 | No breaking API changes without versioning | | |
| 7.3 | Feature flags are present for high-risk rollouts (if applicable) | | |

---

## Reviewer Verdict

**Reviewer Name/ID:**
**Review Completed:**

| Overall Verdict | Condition |
|---|---|
| **APPROVE** | All dimensions PASS or N/A, no FAIL or unresolved PARTIAL |
| **REQUEST CHANGES** | Any FAIL, or PARTIAL without a documented acceptance of the gap |
| **BLOCK** | Any FAIL in dimensions 3 (State Integrity) or 5 (Security) |

**Verdict:**
**Blocking findings (if any):**
**Required changes before merge:**

---

## Convergence Gate (filled by lead after both reviews)

| Outcome | Action |
|---|---|
| Both APPROVE | Proceed to merge |
| One APPROVE, one REQUEST CHANGES | Fix cycle — address all REQUEST CHANGES items, re-review (max 3 iterations) |
| Both REQUEST CHANGES on same item | Fix required |
| Both REQUEST CHANGES on different items | Fix both; lead judges if re-review needed |
| Any BLOCK | Do not merge; fix and restart review cycle |

**Convergence outcome:**
**Iterations used:**
