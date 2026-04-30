---
id: STD-GOV-002
title: Requirements Traceability and ID Governance
status: Approved
owner: Architecture & Governance
applies_to: All services, portals, and pipelines
release_impact_if_violated: Release-blocking unless an approved deferral exists
related_standards:
  - governance/release-evidence-and-signoff.md
  - platform/evidence-immutability-and-replay.md
  - governance/change-classification.md
last_changed: 2026-04-07
---

## Purpose

Ensure every material outcome is traceable from business intent through specification, implementation, and verification to release evidence.

---

## Requirement IDs Are Immutable

- Requirement IDs MUST be stable and never reused.
- Requirements MAY be superseded, but superseded requirements MUST remain discoverable with:
  - a superseded-by reference
  - effective date of supersession
  - migration guidance and evidence expectations

---

## Traceability Is End-to-End

Every material outcome MUST be traceable across:

- **Business intent:** requirement ID(s), legal reference(s), domain overlay(s)
- **Specification:** contract/schema version(s), state model version(s)
- **Implementation:** code change set, configuration/policy bundle version(s)
- **Verification:** tests run, deterministic scenario suite run, contract parity checks
- **Evidence:** evidence pack ID, artifact hashes, signoffs, release decision

### Minimum Trace Links (MUST)

For each release:
- Requirements → tests → evidence pack
- Requirements → contracts (if externally visible)
- Requirements → rule/policy bundle version(s) (if behavior is rule-driven)
- Requirements → operational controls (for security/compliance)

---

## Trace Artifact: Trace Map

Maintain a machine-readable trace map (YAML/JSON or equivalent) that includes:

```yaml
- requirement_id: FR-XXX-001
  description: Brief description
  contract_versions: ["v1.2.0"]
  test_suite_ids: ["integration-suite-run-123"]
  evidence_pack_id: "evidence-pack-v1.2.0"
  owner: Solution Architect
```

### Change Control

- Trace map updates MUST be part of the same cycle as the code/config change they describe.
- A release is **not eligible** for promotion if trace map updates are missing.

---

## Evidence Pack References

Every trace map entry MUST reference a verifiable evidence pack, which includes:
- Test run IDs and reports
- Artifact hashes
- Environment and version metadata
- Signoffs and approvals

See `governance/release-evidence-and-signoff.md` for pack contents.
