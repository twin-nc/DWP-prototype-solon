---
id: STD-SEC-002
title: Data Sensitivity and Redaction
status: Approved
owner: Security Engineering
applies_to: All services, portals, and pipelines
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - operations/observability-and-signal-to-noise.md
  - integration/error-semantics-and-stability.md
  - security/domain-data-classification-template.md
last_changed: 2026-04-07
---

## Purpose

Prevent leakage of sensitive data in logs, errors, traces, evidence packs, and AI/automation outputs.

---

## Sensitivity Classes

| Class | Description |
|---|---|
| **Public** | Intentionally visible to external parties. No access control required. |
| **Internal** | For team and system use. Not for external publication. |
| **Confidential** | Business-sensitive or operationally sensitive. Access on a need-to-know basis. |
| **Restricted** | Highest sensitivity. Must not appear in logs, errors, or AI prompts. Encrypted at rest and in transit. |

For project-specific data element classification, see `security/domain-data-classification-template.md`.

---

## Examples of Restricted Data

- Secrets (API keys, signing keys, tokens)
- Production credentials
- Unredacted personal identifiers (names, national IDs, email in some contexts)
- Full sensitive payloads (financial line items, health data)
- Detailed infrastructure configuration (connection strings, internal hostnames)

---

## Redaction Rules (MUST)

- Restricted data MUST NOT be logged or returned in error responses.
- If payload logging is required for debugging, it MUST be:
  - Explicitly approved by the security owner
  - Time-limited
  - Access-controlled
  - Redacted by default — only specific non-sensitive fields logged

---

## Evidence Packs

- Evidence packs MUST contain only the minimum necessary sensitive data.
- If restricted data is required in an evidence pack, it MUST be encrypted, access-controlled, and referenced by hash only in the main pack body.

---

## AI and Automation Usage

- Do not paste restricted production data into AI prompts.
- Use anonymised or synthetic examples for analysis, troubleshooting, and demonstration.
- AI tools are subject to the same data sensitivity rules as human operators.
- See `ai/agent-responsibility-boundaries.md` for AI-specific requirements.
