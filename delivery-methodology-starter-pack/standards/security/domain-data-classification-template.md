---
id: STD-SEC-003
title: Domain Data Classification
status: Template — fill in per project
owner: Security Engineering
applies_to: All services, portals, and pipelines
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - security/data-sensitivity-and-redaction.md
last_changed: 2026-04-07
---

## Purpose

Map the project's specific data elements to sensitivity classes, so every team member and agent can apply consistent classification without guessing.

This is a template. Replace the example table with your project's actual data elements before sprint 1.

---

## Instructions

1. Copy this file to `docs/project-foundation/standards/security/domain-data-classification.md`.
2. Remove this instructions section.
3. Replace the example table rows with your project's actual data elements.
4. Review with the security owner and team lead before the first feature sprint.
5. Update whenever new sensitive data elements are introduced.

---

## Classification Table

| Data Element | Class | Notes |
|---|---|---|
| **[EXAMPLE] User credentials (passwords, tokens)** | Restricted | Never log; never return in errors |
| **[EXAMPLE] Personal identifiers (name, national ID, email)** | Restricted | Redact in logs; use pseudonyms in test data |
| **[EXAMPLE] Financial amounts tied to an individual** | Confidential | Internal system use; not for external logging |
| **[EXAMPLE] Operational status of a record (e.g., submitted/approved)** | Internal | Safe to log; safe to return in errors |
| **[EXAMPLE] Error codes and correlation IDs** | Internal | Safe to log; safe to return in API responses |
| **[EXAMPLE] Public API documentation** | Public | Designed for external consumers |
| **[EXAMPLE] Infrastructure connection strings** | Restricted | Never log; use secrets management |
| **[EXAMPLE] Aggregate statistics without personal linkage** | Internal | Safe to expose in dashboards |
| **[YOUR DATA ELEMENT]** | *Class* | *Notes* |

---

## Classification Guidance

When in doubt:
- Any data element that can identify a specific individual → **Restricted**
- Any data element that reveals financial position or transaction detail of an individual → **Restricted** or **Confidential**
- Any data element whose exposure would cause regulatory or reputational harm → **Restricted**
- Any data element needed for operational monitoring without personal linkage → **Internal**

If classification is genuinely ambiguous, escalate to the Security Reviewer. Do not default to a lower class without confirmation.

---

## Sensitivity Classes Reference

| Class | Summary |
|---|---|
| Public | Intentionally visible externally |
| Internal | Team/system use only |
| Confidential | Business-sensitive; need-to-know |
| Restricted | Highest sensitivity; encrypted; never in logs or AI prompts |
