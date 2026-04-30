---
id: STD-SEC-003
title: Domain Data Classification — DWP Debt Collection
status: Draft — requires security owner and DWP client review before sprint 1
owner: Security Engineering
applies_to: All services, portals, pipelines, and integration adapters in the DWP Debt Collection system
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - security/data-sensitivity-and-redaction.md
  - security/security-boundaries-and-fail-fast-controls.md
last_changed: 2026-04-16
---

## Purpose

Map the DWP Debt Collection system's data elements to sensitivity classes so every team member and agent applies consistent classification without guessing.

This classification must be reviewed by the Security Owner and DWP client before sprint 1. Classifications marked `⚠ confirm with DWP` require explicit DWP sign-off before being treated as final.

---

## Classification Table

### Identity and Personal Data

| Data Element | Class | Req Ref | Notes |
|---|---|---|---|
| Customer name | Restricted | DIC.2, DIC.20 | PII — redact in all logs |
| National Insurance number / DWP customer ID | Restricted | CAS.5 | Primary identifier — never expose in API errors |
| Date of birth | Restricted | DIC.2 | PII — pseudonymise in test data |
| Home address | Restricted | DIC.2, DIC.20 | PII — redact in logs |
| Phone numbers (mobile, landline) | Restricted | DIC.36, DIC.2 | PII — multiple numbers per customer |
| Email addresses | Restricted | DIC.29, DIC.2 | PII — multiple addresses per customer |
| Bank account / sort code | Restricted | RPF.22, RPF.28 | Financial PII; UK banking compliance; never log |
| Authentication credentials / session tokens | Restricted | INT01, UAAF.5 | Never log; never return in errors; rotate on compromise |
| IP address, MAC address, browser type, session ID | Restricted | COM07 | Required for DWP audit trail — log only to audit sink, not general application logs |

### Financial and Debt Data

| Data Element | Class | Req Ref | Notes |
|---|---|---|---|
| Debt balance (current, arrears, interest) | Restricted | CAS.6 | Financial PII — not for external API responses |
| Repayment plan terms (amount, schedule, status) | Confidential | RPF.1–RPF.37 | Internal operational; CCA audit trail required |
| Payment transaction detail (amount, source, date, ledger code) | Restricted | DIC.35 | Source of funds + individual linkage = Restricted |
| Write-off amount and reason | Restricted | DIC.33, DW.88 | Financial + legal record — immutable |
| Overpayment and offset amounts | Confidential | RPF.35, DW.28 | Internal financial operational |
| Direct debit mandate data | Restricted | RPF.22 | UK banking compliance; treat as bank account data |
| Third-party debt sale data | Restricted ⚠ confirm with DWP | DIC.19 | May include DCA commission rates — confirm classification |
| Income & Expenditure data | Restricted | DIC.11, chapter 14 | Financial PII + FCA affordability; CCA-adjacent |

### Vulnerability and Circumstance Data

| Data Element | Class | Req Ref | Notes |
|---|---|---|---|
| Vulnerability flags and categories (DIC.16) | Restricted | DIC.16, DW.45 | Likely health/circumstance-related — potential GDPR special category; treat as Restricted pending legal review ⚠ confirm with DWP |
| Deceased flag | Restricted | DW.45 | Sensitive circumstance — communications must cease |
| Breathing space / moratorium status | Restricted | DW.45, DW.51 | Regulatory status — misuse triggers compliance breach |
| Insolvency status (IVA, bankruptcy, DRO) | Restricted | DW.45, DW.25 | Legal status — strict handling rules apply |
| Imprisonment flag | Restricted | DIC.26 | Sensitive circumstance — redact in all logs |
| Fraud flag | Restricted | DW.45, DW.84 | Investigation-sensitive — strict need-to-know |
| Reason for arrears | Confidential | DIC.13 | Internal operational; may contain sensitive context |
| Preferred contact time window | Internal | DIC.14 | Operational routing — not sensitive in isolation |

### Workflow and Decisioning Data

| Data Element | Class | Req Ref | Notes |
|---|---|---|---|
| Strategy / workflow assignment | Internal | DW.32, DW.57 | Operational routing |
| Champion/challenger test group assignment | Internal | DW.2, A.1 | Operational — not personal |
| Scorecard / bureau score | Confidential | DIC.9, chapter 15 | Internal risk data; not for external API responses |
| Bureau / credit reference data | Restricted | chapter 15 | Third-party PII; governed by credit reference agency agreements |
| Collections journey history (per account) | Confidential | DW.13, UAAF.13 | Internal audit trail; not for external exposure |
| Agent notes / hot comments | Confidential | CAS.6, UI.5 | May contain sensitive circumstance data; RBAC-controlled |

### Communication and Contact Data

| Data Element | Class | Req Ref | Notes |
|---|---|---|---|
| Outbound communication content (letter, SMS, email) | Confidential | DIC.10, DW.69 | Contains customer-specific debt detail |
| Communication delivery status (sent, failed, bounced) | Internal | DW.53, DW.76 | Operational routing; not personal in isolation |
| Customer contact history | Confidential | DIC.3 | Aggregate interaction record — internal audit |
| Self-service portal actions | Internal | DIC.21 | Operational log — categories only |
| Third-party authority details (PoA, solicitor, DMC) | Restricted | DIC.28 | Third-party PII linked to debt |

### Audit and System Data

| Data Element | Class | Req Ref | Notes |
|---|---|---|---|
| CRUD audit events (create, read, update, delete) | Restricted | COM06, COM07 | Must be passed to DWP auditing solution in near-real-time; never in general application logs |
| GUID / user ID on audit events | Restricted | COM07 | PII in audit context |
| Error codes and correlation IDs | Internal | All APIs | Safe for API responses and general logs |
| System configuration parameters (strategy rules, thresholds) | Internal | DW.9 | Operational; not personal |
| Feature flag states | Internal | DW.9 | Operational |
| Infrastructure connection strings / API keys | Restricted | All | Never log; secrets management only |
| Aggregate statistics without personal linkage | Internal | MIR.1, MIR.2 | Safe for dashboards and reporting |

---

## Classification Guidance

When in doubt:
- Any data element that can identify a specific individual → **Restricted**
- Any data element that reveals financial position, debt status, or transaction detail of an individual → **Restricted**
- Any data element that reveals health, circumstance, or vulnerability of an individual → **Restricted** (potentially special category under GDPR)
- Any data element needed for regulatory audit with individual linkage → **Restricted**
- Any data element needed for internal operational monitoring without personal linkage → **Internal**

If classification is genuinely ambiguous, escalate to the Security Reviewer and DWP Domain Expert. Do not default to a lower class without written confirmation.

---

## DWP-Specific Rules

1. **Audit trail data (COM07)** — GUID, timestamp, MAC address, IP address, browser type, and session ID must be routed to the DWP auditing solution sink, not to general application logs. These fields are Restricted even though they are operational in nature.
2. **Vulnerability data** — Until DWP legal confirm the GDPR lawful basis and whether vulnerability categories constitute special category data (Art. 9), treat all vulnerability flags as Restricted with access restricted to need-to-know roles.
3. **Bureau data** — Subject to third-party data agreements. Do not store or transmit beyond the scope agreed with the credit reference agency. Confirm retention period with DWP legal. ⚠ confirm with DWP.
4. **DWP Payment Allocation data** — Allocation instructions from DWP systems are operational instructions, not financial transactions in the COTS sense. Classification of this interface data requires DWP confirmation. ⚠ confirm with DWP.

---

## Sensitivity Classes Reference

| Class | Summary |
|---|---|
| Public | Intentionally visible externally |
| Internal | Team/system use only |
| Confidential | Business-sensitive; need-to-know |
| Restricted | Highest sensitivity; encrypted at rest and in transit; never in logs; never in AI prompts |

---

## Review History

| Date | Reviewer | Notes |
|---|---|---|
| 2026-04-16 | Delivery team (initial draft) | Populated from tender requirements analysis. Awaiting Security Owner and DWP client review. |
| *(to be completed)* | Security Owner | |
| *(to be completed)* | DWP client | Confirm items marked ⚠ |
