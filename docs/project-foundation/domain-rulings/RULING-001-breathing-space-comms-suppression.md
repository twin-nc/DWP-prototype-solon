# RULING-001: Breathing Space — Communication Suppression Scope

**Ruling ID:** RULING-001
**Linked issue:** TBD
**Status:** `awaiting-client-sign-off`
**Date issued:** 23 April 2026
**Domain expert:** DWP Debt Domain Expert

---

## Requirement IDs Covered

- CAS.BS-01 (Breathing space moratorium trigger)
- CAS.BS-02 (Communication suppression during moratorium)
- DIC.14 (Debt Respite Scheme compliance)

---

## Regulatory Basis

**Debt Respite Scheme (Breathing Space Moratorium and Mental Health Crisis Moratorium) (England and Wales) Regulations 2020, Regulations 7–10.**

The Debt Respite Scheme imposes a statutory moratorium, not a discretionary suppression flag.

---

## Rule Statement

### 1. Statutory nature of suppression

During a breathing space moratorium, DWP must not contact the debtor to collect the debt. This is a legal obligation, not a system configuration choice.

### 2. Account-level scope

The moratorium applies to all qualifying debts on the account — it cannot be selectively applied per debt line.

### 3. Non-collection communications not suppressed

The moratorium does not prohibit all contact. DWP may still communicate about non-debt matters (e.g., benefit entitlement). The system must distinguish `communication_category = DEBT_COLLECTION` from `communication_category = NON_COLLECTION`.

### 4. Suppression reason precedence

When breathing space suppression and dispute suppression (DISPUTE_INTERNAL) are both active:

- Breathing space suppression is legally mandatory and cannot be overridden by any internal system state.
- Dispute suppression is an internal policy control.
- Breathing space governs — not because dispute suppression is weaker, but because the legal obligation exists independently of any internal flag.
- When breathing space expires, the system must evaluate all remaining suppression triggers before re-enabling collections communications. Lifting one reason must not inadvertently lift a statutory suppression.

### 5. Suppression reason model

The suppression model must distinguish `SUPPRESSION_REASON` using the following enum values:

| Value | Type | Governs |
|---|---|---|
| `BREATHING_SPACE_STATUTORY` | Statutory | Legal moratorium — cannot be overridden |
| `MENTAL_HEALTH_CRISIS_STATUTORY` | Statutory | Legal moratorium — no fixed end date |
| `INSOLVENCY_STATUTORY` | Statutory | Legal hold |
| `DECEASED_MANDATORY` | Mandatory | All contact to deceased person |
| `DISPUTE_INTERNAL` | Internal policy | Internal policy — can be lifted by agent |
| `VULNERABILITY_POLICY` | Internal policy | DWP policy — can be lifted by agent |

---

## Edge Cases

### Mental Health Crisis Moratorium

The standard breathing space is 60 days (`PT1440H`). A Mental Health Crisis Moratorium has no fixed end date — it continues until a registered mental health professional confirms the crisis has ended.

**Required domain model change:** `LEGAL_HOLD.hold_type` must distinguish `BREATHING_SPACE_STANDARD` from `BREATHING_SPACE_MENTAL_HEALTH_CRISIS`. The BPMN timer boundary event must only apply to `BREATHING_SPACE_STANDARD`. Mental health crisis cases require a manual review trigger, not a timer expiry.

### Re-enabling collections on expiry

When a breathing space moratorium expires:

1. The system checks whether any other suppression trigger is active before re-enabling collections.
2. If `DISPUTE_INTERNAL` is also active, collections remain suppressed.
3. If no other trigger is active, collections may resume from the last valid process state.

---

## Open Questions Requiring DWP Client Sign-Off

### DDE-OQ-01: Mental Health Crisis referral pathway

**Question:** Does DWP expect to receive Mental Health Crisis Moratorium referrals via this system, or via a separate upstream pathway?

**Impact:** Determines whether the BPMN must model the referral intake process or only the hold management process.

**Status:** ⚠ **AWAITING DWP SIGN-OFF**

---

## Data Classification Flags

- `LEGAL_HOLD.hold_type` — Operational. Not Restricted.
- `SUPPRESSION_LOG.suppression_reason` — Operational. Not Restricted.
- Any case note recording the reason for a mental health crisis moratorium — **Restricted; potential Article 9 special category health data** (see RULING-002).

---

## Guardrails — Builders Must Not Violate

1. `CommunicationSuppressionService` is the single point of suppression decisions (ADR-001). No service may bypass it.
2. Suppression reason `BREATHING_SPACE_STATUTORY` and `MENTAL_HEALTH_CRISIS_STATUTORY` must not be lifted by any automated process — only by explicit moratorium-end events (timer or manual professional sign-off respectively).
3. `NON_COLLECTION` communications must not be blocked by `BREATHING_SPACE_STATUTORY`. The `isPermitted()` method must check `communication_category` before applying statutory suppression.
4. A `SUPPRESSION_LOG` entry must be written for every suppression activation and deactivation, with `reason`, `activated_at`, `deactivated_at`, and `deactivated_by`.

---

## Version History

| Version | Date | Author | Change |
|---|---|---|---|
| 1.0 | 23 Apr 2026 | DWP Debt Domain Expert | Initial ruling |
