# Release Evidence Pack Template

> **Standard:** STD-GOV-006 (`docs/project-foundation/standards/governance/release-evidence-and-signoff.md`)
> **Purpose:** Fillable template for assembling a release evidence pack. Instantiate by copying to `docs/development/release-evidence-<release-id>.md` and replacing all `[FILL: ...]` placeholders with objective, same-cycle evidence.
> **Prohibited practices (STD-GOV-006):** "Ran it locally" as only evidence; test runs from previous cycles; releasing with known parity failures; releasing with missing trace links.

---

## When This Pack Is Required

| Change Class | Deploying to Remote | Required? |
|---|---|---|
| Class A (behavior / legal outcome) | Any | **Always required before merge to main** |
| Class B (contract change) | Any | **Always required before merge to main** |
| Class C / D / E | Yes | Required before remote deployment |
| Class C / D / E | No (local only) | Not required |

## Ownership and Assembly

- **Assembles:** DevOps / Release Engineer
- **Signs architecture section:** Solution Architect
- **Signs domain-specific sections:** Domain Expert (if present on project)

## What a Signature Means

A PR comment reading: `Signed off as [role] — [date]`

All required signatories must sign before merge or deployment. The named role holder must sign — no proxy signatures.

## Santa Method Dual Review (Class A and release evidence)

For Class A changes and release evidence packs, apply the `santa-method` skill before sign-off:
- Two independent reviewers with no shared context
- Identical rubric (see `templates/SANTA-RUBRIC-RELEASE-EVIDENCE.md`)
- Both must pass — no partial credit
- Maximum 3 fix iterations before human escalation

---

---

## 1. Release Identifier

| Field | Value |
|---|---|
| Release ID | `[FILL: human-readable release label, e.g. Release-1]` |
| Build SHA (git commit) | `[FILL: full SHA of the commit being released]` |
| CI run / pipeline URL | `[FILL: URL of the CI run that produced the artefacts]` |
| Artefact IDs | `[FILL: Docker image tags or other artefact references, one per line]` |
| Release date (planned) | `[FILL: YYYY-MM-DD]` |
| Release date (actual) | `[FILL: YYYY-MM-DD]` |

---

## 2. Environment and Configuration Identifiers

| Field | Value |
|---|---|
| Target environment | `[FILL: e.g. production / staging / UAT]` |
| Infrastructure version / IaC ref | `[FILL: IaC state version or equivalent]` |
| API Gateway config ref | `[FILL: config version or commit SHA]` |
| Auth / Identity provider config | `[FILL: config version or realm export tag]` |
| Secrets rotation status | `[FILL: confirmed rotated / not required — reason]` |
| Feature flags in effect | `[FILL: list any flags active at promotion time, or "none"]` |
| Database migration state | `[FILL: migration tool version applied, e.g. Flyway V42]` |

---

## 3. Contract Versions and Parity Reports

| Contract | OpenAPI Spec Version | Runtime Parity Confirmed | Parity Report Ref |
|---|---|---|---|
| `[FILL: API name]` | `[FILL: semver]` | `[FILL: Yes / No — if No, see §7]` | `[FILL: CI run ID or report URL]` |
| `[FILL: API name]` | `[FILL: semver]` | `[FILL: Yes / No]` | `[FILL: CI run ID or report URL]` |
| `[FILL: additional contract]` | `[FILL: semver]` | `[FILL: Yes / No]` | `[FILL: CI run ID or report URL]` |

**Overall parity status:** `[FILL: PASS / FAIL — if FAIL, all failures must appear in §7]`

---

## 4. Rule / Policy Bundle Versions (if applicable)

> Remove this section if the project does not use effective-dated rule bundles. Replace "N/A" with an explicit note in the pack header.

| Bundle | Version | Effective From | Effective To | Context / Jurisdiction | Ref / Commit |
|---|---|---|---|---|---|
| `[FILL: bundle name]` | `[FILL]` | `[FILL: YYYY-MM-DD]` | `[FILL: YYYY-MM-DD or "open"]` | `[FILL]` | `[FILL: commit SHA or bundle ID]` |

**Effective-date completeness:** `[FILL: confirm all bundles have valid effective-date ranges for the release window, or "N/A — project has no date-effective rules"]`

---

## 5. Test Run IDs and Reports

### 5.1 Unit and Integration Tests

| Item | Detail |
|---|---|
| Suite name | `[FILL: e.g. Full unit + integration suite]` |
| CI run ID | `[FILL: run ID]` |
| Run date | `[FILL: YYYY-MM-DD]` |
| Pass / Fail | `[FILL: N/N PASS]` |
| Report URL / artefact | `[FILL: link or path]` |
| Same-cycle confirmation | `[FILL: Yes — run on commit SHA above / No — explain]` |

### 5.2 Contract Tests

| Item | Detail |
|---|---|
| Suite name | `[FILL: e.g. Contract tests (Pact or equivalent)]` |
| CI run ID | `[FILL: run ID]` |
| Run date | `[FILL: YYYY-MM-DD]` |
| Pass / Fail | `[FILL: N/N PASS]` |
| Report URL / artefact | `[FILL: link or path]` |
| Same-cycle confirmation | `[FILL: Yes / No — explain if No]` |

### 5.3 End-to-End / Acceptance Tests

| Item | Detail |
|---|---|
| Suite name | `[FILL: e.g. E2E acceptance scenarios]` |
| CI run ID | `[FILL: run ID]` |
| Run date | `[FILL: YYYY-MM-DD]` |
| Pass / Fail | `[FILL: N/N PASS]` |
| Report URL / artefact | `[FILL: link or path]` |
| Same-cycle confirmation | `[FILL: Yes / No — explain if No]` |

### 5.4 Security Controls Checks

| Item | Detail |
|---|---|
| SAST tool | `[FILL: tool name and version]` |
| SAST CI run ID | `[FILL: run ID]` |
| SAST result | `[FILL: PASS / findings — list critical/high only; others in full report]` |
| DAST status | `[FILL: Executed (run ID) / Deferred — see §7]` |
| Dependency scan CI run ID | `[FILL: run ID]` |
| Dependency scan result | `[FILL: PASS / findings]` |
| Pen test status | `[FILL: Executed (report ref) / Deferred — see §7]` |

### 5.5 Accessibility Scan

> *Optional section — include if applicable; if not applicable for this release, record "N/A — [reason]" in each field.*

| Item | Detail |
|---|---|
| Tool | `[FILL: tool name and version, e.g. axe-core via Playwright]` |
| CI run ID | `[FILL: run ID]` |
| Run date | `[FILL: YYYY-MM-DD]` |
| Pass / Fail | `[FILL: PASS / violations — list critical only; full report linked]` |
| Report URL / artefact | `[FILL: link or path]` |
| Same-cycle confirmation | `[FILL: Yes / No — explain if No]` |

### 5.6 Operational Validation Results

> *Required by STD-GOV-006. Record results or explicitly state "Not required" with justification. Do not leave blank.*

| Item | Detail |
|---|---|
| Required for this release | `[FILL: Yes / No — if No, state reason]` |
| Validation type | `[FILL: e.g. smoke tests post-deploy, health-check confirmation, canary run — or "N/A"]` |
| CI / run ID | `[FILL: run ID or "N/A"]` |
| Run date | `[FILL: YYYY-MM-DD or "N/A"]` |
| Pass / Fail | `[FILL: PASS / FAIL / N/A]` |
| Report URL / artefact | `[FILL: link or path or "N/A — reason"]` |

---

## 6. Traceability Snapshot Reference

| Field | Value |
|---|---|
| Traceability matrix document | `[FILL: path to traceability matrix, e.g. docs/project-foundation/master-solution-design.md §X]` |
| Snapshot commit SHA | `[FILL: SHA of the commit used for this release decision]` |
| Closure status | `[FILL: 100% closed / partial — list open items]` |
| Confirmed by | `[FILL: name / role]` |
| Confirmation date | `[FILL: YYYY-MM-DD]` |

---

## 7. Approved Deviations

> Each deviation must be explicit. Silent deviations are prohibited (STD-GOV-006). Signoff for each deviation must be obtained from the relevant authority before promotion.

Repeat one block per deviation. Delete this section entirely if there are no deviations.

---

**Deviation ID:** `[FILL: DEV-<release-id>-<seq>, e.g. DEV-R1-001]`

| Field | Value |
|---|---|
| Requirement(s) impacted | `[FILL: requirement IDs]` |
| Description | `[FILL: what is missing or not met]` |
| Risk accepted | `[FILL: clear statement of the risk being accepted]` |
| Remediation plan | `[FILL: what will be done and by when]` |
| Target remediation date | `[FILL: YYYY-MM-DD]` |
| Signed off by | `[FILL: name / role]` |
| Signoff date | `[FILL: YYYY-MM-DD]` |

---

*(Add additional deviation blocks as needed.)*

---

## 8. Signoff Table

> All required signatories must sign before release promotion. Adjust "Required" only with explicit governance agreement.

| Role | Name | Required | Date | Signature / Reference |
|---|---|---|---|---|
| Solution Architect | `[FILL: name]` | Yes | `[FILL: YYYY-MM-DD]` | `[FILL: reference or "signed in GitHub PR #N"]` |
| Domain / Policy Owner | `[FILL: name]` | Yes (Class A changes) | `[FILL: YYYY-MM-DD]` | `[FILL: reference]` |
| Security | `[FILL: name]` | Yes (if security controls changed) | `[FILL: YYYY-MM-DD]` | `[FILL: reference]` |
| Release Manager | `[FILL: name]` | Yes | `[FILL: YYYY-MM-DD]` | `[FILL: reference]` |
| Contract Owner | `[FILL: name]` | Yes (if external interfaces changed) | `[FILL: YYYY-MM-DD]` | `[FILL: reference]` |

**Release decision:** `[FILL: GO / NO-GO]`
**Decision date:** `[FILL: YYYY-MM-DD]`
**Decision recorded by:** `[FILL: name / role]`

---

## 9. Pack Immutability Confirmation

| Field | Value |
|---|---|
| Pack hash (SHA-256) | `[FILL: SHA-256 hash of this document — MUST be computed and recorded before pack is finalised; do not promote with this field empty]` |
| Hash computed by | `[FILL: name / tool]` |
| Retention location | `[FILL: path or system where immutable copy is stored]` |
| Retention period | `[FILL: per retention policy]` |

---

*Template version: 1.0. Governed by STD-GOV-006.*
