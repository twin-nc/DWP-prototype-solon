# Santa Method Rubric — Security-Sensitive PR Review

Use this rubric when applying the Santa Method dual review to a PR that touches auth/authz, secrets handling, PII, API boundaries, or data access controls.

Each reviewer scores independently. Neither reviewer sees the other's scoring until both are submitted.

---

## Reviewer Instructions

1. You are a **fresh context window** reviewer. Do not carry reasoning from the session that produced this change.
2. Read the PR diff, linked issue, and acceptance criteria only.
3. Score each dimension independently before reading the other reviewer's output.
4. When in doubt on a security item, score FAIL and require clarification — security ambiguity is not PASS.

---

## Scoring Key

| Score | Meaning |
|---|---|
| **PASS** | Criterion is fully satisfied |
| **PARTIAL** | Criterion is partially satisfied — explain gap |
| **FAIL** | Criterion is not satisfied — explain risk |
| **N/A** | Not applicable — explain why |

---

## Rubric Dimensions

### 1. Secrets and Credentials
| # | Criterion | Score | Notes |
|---|---|---|---|
| 1.1 | No secrets, tokens, or credentials in code, comments, or test fixtures | | |
| 1.2 | Secrets are referenced via environment variables or a secrets manager | | |
| 1.3 | No secrets in log output (including debug logs) | | |
| 1.4 | `.env` files and credential files are in `.gitignore` | | |

### 2. Authentication and Authorization
| # | Criterion | Score | Notes |
|---|---|---|---|
| 2.1 | New endpoints have authentication guards | | |
| 2.2 | Authorization checks are present (role/scope validation) | | |
| 2.3 | Auth bypass is not possible via malformed input | | |
| 2.4 | Token validation includes expiry and signature checks | | |

### 3. Input Validation
| # | Criterion | Score | Notes |
|---|---|---|---|
| 3.1 | All user-controlled inputs are validated at system boundaries | | |
| 3.2 | SQL/NoSQL injection is not possible (parameterized queries or ORM) | | |
| 3.3 | Command injection is not possible (no shell exec with user input) | | |
| 3.4 | XSS vectors are sanitized in any output rendered to browser | | |

### 4. Data Exposure
| # | Criterion | Score | Notes |
|---|---|---|---|
| 4.1 | Stack traces and internal error detail are not returned in API responses | | |
| 4.2 | PII fields are not logged in plaintext | | |
| 4.3 | API responses do not include fields the caller is not authorized to see | | |
| 4.4 | Error messages are safe for end users (no internal paths or class names) | | |

### 5. Data Integrity
| # | Criterion | Score | Notes |
|---|---|---|---|
| 5.1 | Immutable records are not overwritten | | |
| 5.2 | Concurrent write paths have appropriate locking or optimistic concurrency | | |
| 5.3 | Audit records are append-only | | |

### 6. Dependency and Supply Chain
| # | Criterion | Score | Notes |
|---|---|---|---|
| 6.1 | New dependencies are from trusted, maintained sources | | |
| 6.2 | No dependencies with known HIGH/CRITICAL CVEs (check at review time) | | |
| 6.3 | Dependency version is pinned or bounded to prevent silent upgrades | | |

### 7. Security Test Coverage
| # | Criterion | Score | Notes |
|---|---|---|---|
| 7.1 | At least one test covers an auth failure path | | |
| 7.2 | Input validation is covered by at least one test per boundary | | |
| 7.3 | Known vulnerability patterns for this change type are tested | | |

---

## Reviewer Verdict

**Reviewer Name/ID:**
**Review Completed:**

**Verdict:** APPROVE / REQUEST CHANGES / BLOCK

> **BLOCK criteria for security review:**
> - Any FAIL in dimensions 1 (Secrets), 2 (Auth/Authz), or 3 (Input Validation)
> - Any FAIL in 4.1 (stack trace exposure) or 5.1 (immutable record overwrite)

**Blocking findings (if any):**
**Required changes before merge:**

---

## Convergence Gate

| Outcome | Action |
|---|---|
| Both APPROVE | Proceed to merge |
| One APPROVE, one REQUEST CHANGES | Fix cycle — address all items, re-review (max 3 iterations) |
| Any BLOCK | Do not merge; fix and restart security review cycle |
| Reviewers disagree on a FAIL | Escalate to Security or Solution Architect — do not resolve by averaging |

**Convergence outcome:**
**Iterations used:**
