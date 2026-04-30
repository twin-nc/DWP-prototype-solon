# RULING-003 Quick Reference: Vulnerability Configurability Governance Gates

**Full ruling:** `RULING-003-vulnerability-configurability-governance-and-constraints.md`

---

## The Answer to Your Six Questions

### 1. Who Can Change Vulnerability Definitions and Action Rules?

| Tier | Who | Gate | Approval |
|---|---|---|---|
| **Tier 1: Definitions** | Operational users via admin UI | Policy approval | DWP policy owner + Domain Expert |
| **Tier 2: Action Rules** | Operational users via DMN / admin UI | Class A gate | Domain Expert + Solution Architect (both must approve PR) |
| **Tier 3: Process** | Developers / restricted admins via BPMN deployment | Class A gate for new definitions; standard gate for variants | Domain Expert (mandatory rules); standard dev gate (variants) |

**Key rule:** Vulnerability definitions and action rule changes require **named approval signatures** in audit. This is not self-service.

---

### 2. Are There Mandatory Fixed Rules That Cannot Be Configured?

**YES — Five absolute hard rules cannot be disabled:**

| Rule | Regulation | Cannot Override |
|---|---|---|
| Breathing space = NO collections contact (except statutory notices) | Debt Respite Scheme Regulations | DMN rules, admin config, supervisor override |
| Insolvency = NO enforcement action (field visit, claim, distraint) | Insolvency Rules 2016 | DMN rules, admin config, supervisor override |
| Vulnerable customer = I&E assessment mandatory before plan/enforcement | FCA Consumer Duty | DMN rules, admin config, supervisor override |
| Copy agreement request = 12-day deadline (CCA s.77–79) | Consumer Credit Act 1974 | Any operational config |
| Default notice = mandatory fields (CCA s.86–86F) | Consumer Credit Act 1974 | Template configuration |

**Implementation:** These are immutable checks in code (e.g., `communications.CollectionsActionAuthorizer.authorize()`, `strategy.RepaymentPlanAuthorizer.authorize()`), not DMN rules.

---

### 3. What Harm Results From Misconfigured Vulnerability Thresholds?

| Misconfiguration | Harm | Regulatory Risk |
|---|---|---|
| **Over-broad** (all customers marked vulnerable) | Collections gridlock; inefficiency; FCA compliance benefit lost | Medium — suggests taxonomy is wrong |
| **Under-restrictive** (only disability flag counts) | Vulnerable customers not identified; I&E skipped; enforcement proceeds against financial hardship; affordability ignored | **Critical — FCA Consumer Duty breach; Enforcement action likely** |
| **Mid-flight rule changes without effective-dating** | Account opens under ruleset A, switches to ruleset B mid-case without re-evaluation; audit evidence of inconsistent treatment | Medium-to-high — fairness dispute risk |
| **Disabling forbearance for vulnerable** | Vulnerable customer receives enforcement without forbearance offer | **Critical — FCA Enforcement action** |
| **Breathing space not checked** | Collections letter sent during protected period; statutory scheme breach | **Critical — regulatory violation** |

---

### 4. Does Effective-Dating of Configuration Changes Matter Legally?

**YES — Critically important.**

**Requirement:** When a vulnerability definition or action rule changes:
- **New accounts** use the new rule version
- **In-flight accounts** remain on the rule version active when opened
- **Mid-flight re-evaluation** is manual, not automatic

**Why:** Changing rules mid-case creates audit evidence that the treatment was inconsistent, which opens DWP to fairness/TCF challenges.

**Implementation:** Every account stores `vulnerability_definition_version_id` and `action_rule_version_id`. Process engine captures the rule version used for each decision in audit log.

---

### 5. What Governance/Approval Process Does DWP Expect?

**Two-level approval process:**

**Level 1 — Vulnerability Definition Changes (Tier 1)**
- Change proposed by DWP policy team
- Reviewed by: DWP Debt Domain Expert (regulatory compliance) + Solution Architect (process impact)
- Approval gate: recorded in `vulnerability_definition_audit` table with approver names and rationale
- Implementation: database reference data update; no code deployment required
- Can be effective-dated to a future date

**Level 2 — Action Rule Changes (Tier 2)**
- Change proposed by BA or business analyst
- Reviewed by: DWP Debt Domain Expert + Solution Architect
- Approval gate: **Class A PR gate (WAYS-OF-WORKING §5a, Option C)** — both must approve before merge
- Implementation: DMN rule version deployment; Flowable runtime update
- Can be effective-dated to a future date

**Expected lead time:** 1–4 weeks, depending on policy review complexity.

---

### 6. Any Other DWP-Specific Constraints?

**Yes — Four critical constraints:**

1. **Vulnerability data is Restricted** — Treat as GDPR potential special category; access control via need-to-know roles (pending DWP legal confirmation of lawful basis per STD-SEC-003).

2. **I&E assessment is mandatory for vulnerable customers** — Not optional. If configuration disables it, the system must fail hard. This is an FCA Consumer Duty obligation.

3. **For joint UC accounts with mixed vulnerability** — Open question: does the whole account receive process divergence or only the vulnerable person? **BA must get DWP answer to section 8 question 4 before writing ACs.**

4. **Breathing space and insolvency are absolute** — Not policy choices. They are statutory obligations. If system design attempts to make them configurable, Domain Expert must reject it.

---

## Blocked Features Awaiting Client Sign-Off

**BA cannot write acceptance criteria for these until DWP provides answers:**

| Feature | Open Question | Blocker |
|---|---|---|
| DIC.16 (configurable vulnerability drop-down) | What are the initial vulnerability types DWP will recognize? | Section 8, Q1 |
| DW.45 (vulnerability-driven collections control) | For mixed-vulnerability joint accounts, what is the process divergence rule? | Section 8, Q4 |
| IEC.1–11 (I&E capture) | Is I&E mandatory before any plan/enforcement, or only when disputed? | Section 8, Q5 |
| DW.25 (exception strategy routing) | What are the escalation criteria for vulnerable customers? | Section 8, Q8 |
| DW.45 | Does DWP have field visit restrictions for vulnerable customers? | Section 8, Q9 |

**Owner for client escalation:** Delivery Lead → DWP policy team.

---

## For the Builder: Hard Rules (Immutable Code)

Before you write any code for vulnerability handling:

1. **Read section 5 of the full ruling.** These are the five absolute rules you must code as invariant checks, not as configurable rules.

2. **Write test cases first (TDD):**
   - Breathing space active → collections action refuses
   - Vulnerable without I&E → repayment plan authorization refuses
   - Insolvency active → enforcement action refuses
   - Vulnerability rule version changes → new accounts use new version; old accounts remain on old version

3. **Code locations:**
   - `communications` module: breathing space prohibition, default notice validation, copy agreement deadline
   - `strategy` module: vulnerability definition versioning, rule version assignment
   - `repaymentplan` module: I&E mandatory check before plan creation
   - `workallocation` module: insolvency enforcement suspension

4. **Do not make breathing space, insolvency, I&E, or copy agreement deadline configurable.** If a BA or supervis requests it, escalate to Domain Expert immediately.

---

## Key Files

- **Full ruling:** `/docs/project-foundation/domain-rulings/RULING-003-vulnerability-configurability-governance-and-constraints.md`
- **Data classification (vulnerability data):** `/docs/project-foundation/standards/security/domain-data-classification.md` (STD-SEC-003)
- **Ways of Working (Class A gate):** `/docs/project-foundation/WAYS-OF-WORKING.md` (§5a, Option C)
- **Architecture decision:** `/docs/project-foundation/decisions/ADR-004-segment-taxonomy-and-configurability.md`

---

End of quick reference.
