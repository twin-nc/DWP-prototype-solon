# Domain Rulings

Named, traceable regulatory interpretations produced by the DWP Debt Domain Expert.

## Purpose

Each ruling documents the regulatory or DWP policy basis for a specific business rule, so that Business Analysts and Builders have unambiguous, legally defensible guidance before writing acceptance criteria or code.

## Naming convention

```
RULING-NNN-<slug>.md
```

Example: `RULING-001-breathing-space-comms-suspension.md`

## Required fields in every ruling

- **Ruling ID** — `RULING-NNN`
- **Linked issue** — GitHub issue number
- **Status** — `final` | `awaiting-client-sign-off`
- **Requirement IDs** — requirement references this ruling covers (CAS.x, DIC.x, DW.x, etc.)
- **Regulatory basis** — specific regulation or DWP policy section
- **Rule statement** — unambiguous statement of the rule
- **Edge cases** — boundary conditions and interaction scenarios
- **Open questions** — items requiring DWP client sign-off, with named owner
- **Data classification flags** — any Restricted or special-category-adjacent elements identified (reference STD-SEC-003)

## Rulings index

| ID | File | Topic | Status |
|---|---|---|---|
| RULING-001 | [RULING-001-breathing-space-comms-suppression.md](RULING-001-breathing-space-comms-suppression.md) | Breathing Space: Communication Suppression Scope | awaiting-client-sign-off |
| RULING-002 | [RULING-002-vulnerability-data-classification.md](RULING-002-vulnerability-data-classification.md) | Vulnerability: Data Classification & Access Control | awaiting-client-sign-off |
| RULING-003 | [RULING-003-write-off-regulatory-constraints.md](RULING-003-write-off-regulatory-constraints.md) | Write-Off: Regulatory and Policy Constraints | awaiting-client-sign-off |
| RULING-004 | [RULING-004-joint-debt-split-constraints.md](RULING-004-joint-debt-split-constraints.md) | Joint Debt: Regulatory Constraints on Split | awaiting-client-sign-off |
| RULING-005 | [RULING-005-breathing-space-arrangement-interaction.md](RULING-005-breathing-space-arrangement-interaction.md) | Breathing Space: REPAYMENT_ARRANGEMENT Interaction | final |
| RULING-006 | [RULING-006-deceased-party-mandatory-handling.md](RULING-006-deceased-party-mandatory-handling.md) | Deceased Party: Mandatory Handling | awaiting-client-sign-off |
| RULING-007 | [RULING-007-ie-assessment-affordability.md](RULING-007-ie-assessment-affordability.md) | I&E Assessment: FCA Affordability Obligations | awaiting-client-sign-off |
| RULING-008 | [RULING-008-dca-placement-pre-placement-disclosure.md](RULING-008-dca-placement-pre-placement-disclosure.md) | DCA Placement: Pre-Placement Disclosure Obligations | awaiting-client-sign-off |
| RULING-009 | [RULING-009-statute-barred-debt.md](RULING-009-statute-barred-debt.md) | Statute-Barred Debt | awaiting-client-sign-off |
| RULING-010 | [RULING-010-champion-challenger-consumer-duty.md](RULING-010-champion-challenger-consumer-duty.md) | Champion/Challenger: Consumer Duty Implications | awaiting-client-sign-off |
| RULING-011 | [RULING-011-queued-communication-disposition.md](RULING-011-queued-communication-disposition.md) | Queued Communication Disposition on Suppression Lift | final |
| RULING-012 | [RULING-012-limitation-clock-reset-events.md](RULING-012-limitation-clock-reset-events.md) | Limitation Clock Reset Events | awaiting-client-sign-off |
| RULING-013 | [RULING-013-statute-barred-clock-reset-timing.md](RULING-013-statute-barred-clock-reset-timing.md) | Statute-Barred Clock Reset: Synchronous vs. Delayed Clearance | awaiting-client-sign-off |
| RULING-014 | [RULING-014-repayment-arrangement-concurrency.md](RULING-014-repayment-arrangement-concurrency.md) | Repayment Arrangement Concurrency — One or Many Active Arrangements per Account | awaiting-client-sign-off |
| RULING-015 | [RULING-015-consolidated-repayment-arrangement.md](RULING-015-consolidated-repayment-arrangement.md) | Consolidated (Multi-Debt) Repayment Arrangement — Permissibility and Data Model | awaiting-client-sign-off |

## Provisional Constraints and DO NOT PROCEED Gates

The following rulings are `awaiting-client-sign-off`. They impose **provisional constraints** on implementation — builders must treat the rule statements as binding until sign-off is received, but must not rewrite them as confirmed implemented facts.

### Hard blocks — implementation must not proceed past design

| Ruling | Topic | Blocking condition | Open question |
|---|---|---|---|
| RULING-006 | Deceased party mandatory handling | Sprint 1 account management stories for deceased customers **DO NOT PROCEED** until estate pursuit policy confirmed | DDE-OQ-07: estate pursuit policy |
| RULING-009 | Statute-barred debt | Enforcement/collection workflow implementation **DO NOT PROCEED** until limitation periods confirmed | DDE-OQ-11: DWP benefit debt limitation period |

### Conditional implementation — design only, no enforcement code

| Ruling | Topic | Condition | Open questions |
|---|---|---|---|
| RULING-012 | Limitation clock reset events | `PROMISE_TO_PAY` clock-reset trigger blocked pending DWP legal confirmation | DDE-OQ-13: PTP as written acknowledgement |
| RULING-013 | Statute-barred clock reset timing | Three implementation paths conditional on client answers; default Option B + Yes + No applies if no response by 30 April 2026 | DDE-OQ-11, DDE-OQ-12, DDE-OQ-13 |

### Decision-support appendix

| File | Purpose |
|---|---|
| [DDE-OQ-STATUTE-BARRED-SUMMARY.md](DDE-OQ-STATUTE-BARRED-SUMMARY.md) | Quick reference for Delivery Lead: three client choice questions (DDE-OQ-11/12/13) with pros/cons and regulatory impact |
| [RULING-013-HANDOFF.md](RULING-013-HANDOFF.md) | Handoff summary for RULING-013: what is ready, what is blocked, next steps by role, default path if client does not respond by 30 April 2026 |

---

## Gate

No BA may write acceptance criteria for a Class A requirement, and no Builder may merge a Class A change, without a ruling in this directory linked on the PR.

Class A categories are defined in WAYS-OF-WORKING.md §5a and in the DWP Debt Domain Expert agent profile (`.claude/agents/dwp-debt-domain-expert.md`).
