# Santa Method Rubric — Release Evidence Pack

Use this rubric when applying the Santa Method dual review to a completed Release Evidence Pack before a release is signed off.

Each reviewer scores independently. Neither reviewer sees the other's scoring until both are submitted.

---

## Reviewer Instructions

1. You are a **fresh context window** reviewer. Do not carry reasoning from the session that assembled the evidence pack.
2. Read the Release Evidence Pack document and the associated PR list.
3. Score each dimension independently before reading the other reviewer's output.
4. Flag any item you cannot assess due to missing evidence — do not score it as passing.

---

## Scoring Key

| Score | Meaning |
|---|---|
| **PASS** | Evidence is present, complete, and credible |
| **PARTIAL** | Evidence exists but has gaps — explain |
| **FAIL** | Evidence is missing or does not support the claim |
| **N/A** | Not applicable — explain why |

---

## Rubric Dimensions

### 1. Scope Completeness
| # | Criterion | Score | Notes |
|---|---|---|---|
| 1.1 | All PRs merged to `main` since last release are listed | | |
| 1.2 | Every listed PR links to a closed issue | | |
| 1.3 | No unlisted PRs exist in the release window (checked via git log) | | |

### 2. Traceability
| # | Criterion | Score | Notes |
|---|---|---|---|
| 2.1 | Every functional change traces to a requirement ID | | |
| 2.2 | Every requirement in scope has at least one corresponding change | | |
| 2.3 | Deferred requirements are explicitly listed with justification | | |

### 3. Test Evidence
| # | Criterion | Score | Notes |
|---|---|---|---|
| 3.1 | CI is green on the release branch (link provided) | | |
| 3.2 | Test suite covers the critical paths for this release | | |
| 3.3 | No known test failures deferred without documented acceptance | | |
| 3.4 | E2E or integration test evidence covers cross-service flows (if applicable) | | |

### 4. Sign-offs
| # | Criterion | Score | Notes |
|---|---|---|---|
| 4.1 | Solution Architect has signed architecture section (named, dated PR comment) | | |
| 4.2 | Domain Expert has signed domain-sensitive sections (if applicable) | | |
| 4.3 | DevOps/RE has signed deployment and rollback section | | |
| 4.4 | No proxy signatures — each section signed by its named owner | | |

### 5. Deployment Readiness
| # | Criterion | Score | Notes |
|---|---|---|---|
| 5.1 | Runbook for this release exists and is current | | |
| 5.2 | Rollback procedure is documented and tested (or untested risk is accepted) | | |
| 5.3 | All migrations are applied or scheduled with a clear sequence | | |
| 5.4 | Feature flags are set correctly for release (if applicable) | | |

### 6. Documentation
| # | Criterion | Score | Notes |
|---|---|---|---|
| 6.1 | Markdown docs are updated for all `md update required` changes | | |
| 6.2 | Word documents are republished or deferral is explicitly agreed | | |
| 6.3 | Build Phase Tracker reflects current state | | |

### 7. Known Issues
| # | Criterion | Score | Notes |
|---|---|---|---|
| 7.1 | All known issues are listed with severity and owner | | |
| 7.2 | No CRITICAL known issues are present without explicit acceptance | | |
| 7.3 | Follow-up issues are raised for any HIGH known issues | | |

---

## Reviewer Verdict

**Reviewer Name/ID:**
**Review Completed:**

**Verdict:** APPROVE / REQUEST CHANGES / BLOCK

**Blocking findings (if any):**
**Required changes before release sign-off:**

---

## Convergence Gate

| Outcome | Action |
|---|---|
| Both APPROVE | Release sign-off is complete — proceed |
| One APPROVE, one REQUEST CHANGES | Fix/update evidence; re-review (max 3 iterations) |
| Both REQUEST CHANGES | Fix all; re-review |
| Any BLOCK | Do not release; resolve blocking finding |

**Convergence outcome:**
**Iterations used:**
