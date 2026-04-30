# RBAC Implementation Decisions

**Project:** DWP Debt Collection Management System (DCMS)  
**Date:** 2026-04-24  
**Last updated:** 2026-04-28 — STRATEGY_MANAGER role added (section 1.4)  
**Status:** Decisions agreed — pending documentation propagation  
**Authority:** Supersedes placeholder RBAC statements in MASTER-DESIGN-DOCUMENT.md and ARCHITECTURE-BLUEPRINT.md; extends ADR-004, ADR-006, ADR-008, ADR-010

---

## Purpose

This document captures all RBAC design decisions made during the April 2026 design review, including the complete role set, permission matrix, write-off tier model, team scope enforcement, case transfer process, and the exact changes required to each existing document. It is the single reference for implementing these decisions into the prototype documentation and codebase.

---

## 1. Final Role Set

### 1.1 Operational roles — Keycloak realm roles

| Role | Description |
|---|---|
| `AGENT` | Front-line debt collection agent. General caseload. |
| `SPECIALIST_AGENT` | Welfare queue agent handling vulnerable customer cases (previously called Welfare Queue Agent in journey maps). |
| `ESCALATIONS_AGENT` | Handles escalated cases and exception queues. |
| `TEAM_LEADER` | Supervisor. Manages own team. Write-off authority up to Tier 2. Case transfer authority. |
| `OPS_MANAGER` | Operations manager. Write-off authority up to Tier 3. SYSTEM_CONFIG operational keys. Cross-team visibility. |
| `SRO` | Senior Responsible Officer. Highest in-system write-off authority (Tier 4). Provisioned per SRO design (section 4). |
| `COMPLIANCE` | Oversight and governance. Approves Class A changes (ADR-008, ADR-010 governance gate). Receives routing on above-ceiling write-off amounts. Not a write-off approver authority. |
| `BACKOFFICE` | Administrative support. Read access to case data, no financial or treatment actions. |
| `PROCESS_DESIGNER` | Authors and deploys BPMN/DMN changes (Class A proposer). Not an operational role. |
| `STRATEGY_MANAGER` | Monitors live strategy performance (champion/challenger results, KPIs). Triggers C/C promotions. No authoring capability. Not a case-handling role. |

### 1.2 Technical roles — Keycloak client-scoped (`flowable-admin` client)

| Role | Description |
|---|---|
| `FLOWABLE_ADMIN` | Flowable Admin UI access. Confined to operational monitoring, task management, and incident recovery. Audit events generated via HistoryEventProcessor — actor_id must be provisioned user UUID, not SYSTEM UUID. |
| `SYSTEM` | Service-to-service calls. JWT client credentials grant. No human user. |

### 1.3 STRATEGY_MANAGER role — design rationale (added 2026-04-28)

`STRATEGY_MANAGER` is a distinct role from `PROCESS_DESIGNER`. The separation reflects a real organisational boundary at DWP: the person who monitors whether a champion/challenger experiment is working and decides to promote the challenger is not necessarily the same person who authored the DMN rule. Conflating them would give a monitoring actor unnecessary authoring permissions, and would give an authoring actor a UI entitlement (C/C promotion) that carries a different risk profile.

**What STRATEGY_MANAGER can do:**
- View champion/challenger performance dashboards and comparative results in the Operations Workspace
- View strategy KPIs (arrangement rates, breach rates, campaign performance)
- Trigger a C/C promotion — swap a challenger DMN version to champion status
- Read-only access to the Configuration Workspace Rules section (for context when reviewing results)

**What STRATEGY_MANAGER cannot do:**
- Author or modify DMN rules (that is PROCESS_DESIGNER's remit)
- Approve rule changes (that is COMPLIANCE's remit)
- Access any case-handling screens
- Access any Tier 1 or Tier 3 configuration surfaces

**C/C promotion mechanics:** Promoting a challenger to champion changes `dmn_deployment.is_champion` and `dmn_deployment.is_challenger` flags on the relevant deployment records. This is a state transition on an already-approved DMN version — the DMN itself was approved via the Tier 2 workflow before it became a challenger. Promotion does not require a new Tier 2 approval cycle. It does write an `AUDIT_EVENT` of type `CC_PROMOTION` with the actor_id of the STRATEGY_MANAGER who triggered it.

**Who can also see C/C results:** OPS_MANAGER has read access to C/C results (they appear in the Operations Workspace that OPS_MANAGER uses). OPS_MANAGER cannot trigger promotion — that is confined to STRATEGY_MANAGER to maintain a clear owner for strategy performance decisions.

### 1.4 Roles removed relative to feat/5 proposal

| Removed role | Reason |
|---|---|
| `ADMIN` | Identified as self-audit risk and separation-of-duties failure. User management moves to OPS_MANAGER (own team) or out-of-band Keycloak administration. SYSTEM_CONFIG access split — operational keys to OPS_MANAGER, structural keys to OPS_MANAGER + COMPLIANCE dual-entry. |
| `LEGAL_AGENT` | Out of scope by explicit user decision. LEGAL team type and LEGAL queue type remain in schema for referral recording purposes only. No in-scope legal case handling. |
| `DOMAIN_EXPERT` | Referenced in ADR-008 and ADR-010 but not provisioned. User decision: this maps to an existing DWP role. Resolved as COMPLIANCE absorbing the governance gate function. All ADR references to DOMAIN_EXPERT are reattributed to COMPLIANCE. |

---

## 2. Permission Matrix

Key: **Y** = permitted | **N** = not permitted | **Own** = own team/cases only | **Read** = read-only

| Feature | AGENT | SPEC_AGENT | ESC_AGENT | TEAM_LEADER | OPS_MANAGER | SRO | COMPLIANCE | BACKOFFICE | STRATEGY_MANAGER |
|---|---|---|---|---|---|---|---|---|---|
| View own caseload | Y | Y | Y | Own | Y | Y | Read | Read | N |
| View all cases | N | N | N | Own | Y | Y | Read | N | N |
| Work case (update contact, notes) | Y | Y | Y | Own | Y | N | N | N | N |
| Initiate treatment action | Y | Y | Y | Own | N | N | N | N | N |
| Suppression override (DUAL_USE per ADR-001) | N | Y | Y | Y | Y | N | N | N | N |
| Write-off — Tier 1 | Y | Y | Y | Y | Y | Y | N | N | N |
| Write-off — Tier 2 | N | N | N | Y | Y | Y | N | N | N |
| Write-off — Tier 3 | N | N | N | N | Y | Y | N | N | N |
| Write-off — Tier 4 | N | N | N | N | N | Y | N | N | N |
| Approve write-off (any tier, not self) | N | N | N | Y | Y | Y | N | N | N |
| Case transfer | N | N | N | Y | Y | N | N | N | N |
| Queue assignment | N | N | N | Y | Y | N | N | N | N |
| SYSTEM_CONFIG — operational keys | N | N | N | N | Y | N | N | N | N |
| SYSTEM_CONFIG — structural keys | N | N | N | N | Y+COMP | N | Y+OPS | N | N |
| Flowable Admin UI | N | N | N | N | N | N | N | N | N |
| Class A change — propose | N | N | N | N | N | N | N | N | N |
| Class A change — approve | N | N | N | N | N | N | Y | N | N |
| View audit trail | N | N | N | Own | Y | Y | Y | N | N |
| View C/C results and strategy KPIs | N | N | N | N | Read | N | N | N | Y |
| Trigger C/C promotion (champion swap) | N | N | N | N | N | N | N | N | Y |
| PROCESS_DESIGNER actions | See section 5 |

Notes:
- Suppression override (DUAL_USE) is an ADR-001 operational permission, not a configurability governance permission. ADR-008 view-only label for TEAM_LEADER applies to configurability governance only — it does not restrict ADR-001 operational permissions.
- COMPLIANCE is not a write-off approver. Above-ceiling amounts route to COMPLIANCE as a notification+review trigger (MANUAL_REVIEW_REQUIRED), not as an approval workflow.
- OPS_MANAGER structural key entry requires countersignature from COMPLIANCE (dual-entry pattern). COMPLIANCE structural key entry requires countersignature from OPS_MANAGER. Neither can act alone on structural SYSTEM_CONFIG keys.
- STRATEGY_MANAGER has no case-handling permissions and no configuration authoring permissions. Access is confined to the Operations Workspace (C/C results, strategy KPIs) and the Configuration Workspace Rules section (read-only, for context). C/C promotion triggers a state change on a `dmn_deployment` record (champion swap) — this is an operational analytics action, not a rule authoring action, and does not require the Tier 2 approval workflow.

---

## 3. Write-Off Tier Model

### 3.1 Tier thresholds — Tier 1 configurability (provisional)

All values are provisional pending DWP client sign-off (DDE-OQ-02). These are recommended seed values only.

| Key | SYSTEM_CONFIG key | Provisional threshold | Authority |
|---|---|---|---|
| Tier 1 | `write_off_limit.tier1` | £500 | AGENT, SPECIALIST_AGENT, ESCALATIONS_AGENT |
| Tier 2 | `write_off_limit.tier2` | £2,000 | TEAM_LEADER and above |
| Tier 3 | `write_off_limit.tier3` | £10,000 | OPS_MANAGER and above |
| Tier 4 | `write_off_limit.tier4` | £50,000 | SRO only |
| Above Tier 4 | (no key) | No ceiling — routes to COMPLIANCE | MANUAL_REVIEW_REQUIRED + COMPLIANCE notification |

**Open item DDE-OQ-02:** All four threshold values require DWP confirmation. The service layer must read from SYSTEM_CONFIG at decision time (ADR-009 live-state rule). No thresholds are hardcoded.

### 3.2 Tier invariant

The service layer enforces at write time: `tier1 < tier2 < tier3 < tier4`. Violation raises a validation error and prevents the config update.

### 3.3 Tier 1 configurability placement

Write-off thresholds are Tier 1 (foundational config) per ADR-008, not Tier 2 (DMN). Rationale: thresholds are delegated authority ceilings with a regulatory basis (MPM), not behavioural segmentation rules.

### 3.4 Above-ceiling routing (no hard block)

Amounts above the SRO ceiling (Tier 4) do not produce a service-layer rejection. They produce:
1. `AUDIT_EVENT` of type `MANUAL_REVIEW_REQUIRED`
2. Routing to COMPLIANCE as a notification
3. Empty candidate group guard prevents silent process continuation

This is the same pattern as write-off self-approval prevention (section 4.2 below). No tender requirement mandates a hard block.

### 3.5 Self-approval prevention — three-layer defence

Per ADR-004 (extended):

1. **Flowable layer:** `CreateTaskListener` removes the requestor's user ID from the candidate group at task creation.
2. **Service layer:** validates `requestedBy != approverId` before processing. Throws `IllegalStateException` if violated.
3. **Database layer:** `CHECK` constraint on WRITE_OFF table enforces `requested_by != approved_by`.

All three layers must be implemented. Passing any single layer is not sufficient.

---

## 4. SRO Design

### 4.1 Decision summary

- **Job title:** Senior Responsible Officer (existing DWP designation)
- **Threshold seed:** £50,000 (Tier 4, from `write_off_limit.tier4` in SYSTEM_CONFIG)
- **In-system actor:** Yes. SRO is provisioned in Keycloak as a named user with role `SRO`. Not an out-of-band approval.
- **Provisioning sequence:** SRO must be provisioned in Keycloak and the USER table before `write_off_limit.tier4` can be set. The service layer checks for at least one active SRO user before accepting a Tier 4 config value.

### 4.2 Empty candidate group guard

When the write-off approval task targets the SRO candidate group and no SRO user is provisioned (or all SROs are excluded by self-approval rule), the process engine must not silently continue. Behaviour:
- Task is created with an empty candidate group.
- A companion `MANUAL_REVIEW_REQUIRED` audit event is emitted.
- A supervisor-visible work item is raised.
- The process waits. No auto-resolution.

This matches the pattern described in ADR-004 for the SRO placeholder guardrail. It is now the confirmed production behaviour, not a placeholder.

### 4.3 Implications of SRO design

1. At least one named SRO user must be active for Tier 4 write-offs to be completable.
2. If a single SRO user requests a Tier 4 write-off, they cannot approve it themselves (self-approval prevention). The process parks until a second SRO is provisioned or the request is withdrawn.
3. Provisioning and de-provisioning SRO accounts is a governed operation — not a routine OPS_MANAGER action.
4. SRO accounts should be audited separately from operational accounts.

---

## 5. PROCESS_DESIGNER Role

### 5.1 Capability

| Action | Permitted |
|---|---|
| Author BPMN/DMN in design tool | Y |
| Propose Class A change (submit for approval) | Y |
| Deploy Tier 2 DMN to non-prod | Y |
| Deploy Tier 3 BPMN to non-prod | Y |
| Approve own Class A change | N |
| Deploy to production | N (COMPLIANCE approval required first) |
| Access operational case data | N |

### 5.2 Two-person gate (Class A changes)

- PROCESS_DESIGNER proposes.
- COMPLIANCE approves.
- Neither can act alone.
- Mandatory export-to-git before production promotion.

### 5.3 ADR-008 attribution fix

ADR-008 currently attributes Tier 2 and Tier 3 deploy to `ADMIN`. This is incorrect — `ADMIN` is removed. The deploy column in ADR-008 must be reattributed to `PROCESS_DESIGNER`. See section 9.4 for the exact change.

---

## 6. Team Scope Enforcement

### 6.1 Mechanism

**Option B + Option C (both required):**

- **Option B (primary enforcement):** On each request, the `team_id` is resolved from the USER table using the JWT subject claim. This `team_id` is applied as a JPA repository predicate to filter query results. Agents and team leaders see only cases within their team scope.
- **Option C (defence in depth):** Work queue assignment ensures cases are routed to queues owned by the correct team. Agents pulling from queues cannot receive out-of-scope cases as a matter of normal workflow. Option C alone is not sufficient (it can be bypassed by direct case lookup). Both must be implemented.

### 6.2 Schema join path

`ACCOUNT` does not carry a `team_id` directly. The join path is:

```
ACCOUNT → WORK_QUEUE → TEAM
```

This is a deliberate architectural decision: team assignment is a work allocation concern, not a financial ledger concern. The `account` domain module must not carry team routing state. The `workallocation` domain module owns team scope.

### 6.3 Case transfer — de-escalation edge case

When a case transfers from a SPECIALIST team to a GENERAL team (de-escalation), the Article 9 access gates change. Specifically:
- SPECIALIST_AGENT permissions on the case are revoked at the repository layer on transfer completion.
- AGENT permissions are granted to the receiving team.
- This change takes effect at the moment of transfer, not retrospectively.
- The old team loses access immediately on transfer — no grace period.

---

## 7. Case Transfer Process

This process was not defined in any prior document. The following is the agreed design.

### 7.1 CASE_TRANSFER table (new schema)

```sql
CREATE TABLE case_transfer (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      UUID NOT NULL REFERENCES account(id),
    from_team_id    UUID NOT NULL REFERENCES team(id),
    to_team_id      UUID NOT NULL REFERENCES team(id),
    transferred_by  UUID NOT NULL REFERENCES users(id),
    reason_code     VARCHAR(50) NOT NULL,
    notes           TEXT,
    transferred_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
```

### 7.2 Reason codes (bounded enumeration)

| Code | Description |
|---|---|
| `ESCALATION` | Case complexity exceeds team capability |
| `DE_ESCALATION` | Case resolved to standard track |
| `SPECIALIST_REFERRAL` | Vulnerability or welfare referral |
| `GEOGRAPHIC_REASSIGNMENT` | Team boundary change |
| `WORKLOAD_REBALANCE` | Supervisor-initiated workload management |
| `ERROR_CORRECTION` | Initial assignment was incorrect |

Reason codes are a closed enumeration in the application layer. New codes require a schema migration + Class B change.

### 7.3 Transfer mechanics

On case transfer:
1. Requester must hold `TEAM_LEADER` or `OPS_MANAGER` role.
2. Active work queue entry for the case in the source queue is closed.
3. Agent assignment on the case is cleared.
4. New work queue entry is created in the target team's queue.
5. `CASE_TRANSFER` record is written (append-only).
6. `AUDIT_EVENT` of type `STATUS_CHANGE` is written with sub-type `CASE_TRANSFER`.
7. Team scope predicate update takes effect immediately (no async).

### 7.4 MI reporting

`CASE_TRANSFER` is a first-class MI reporting table. Reason code distribution, transfer frequency by team, and de-escalation rate are expected MI outputs.

---

## 8. SYSTEM_CONFIG Split

### 8.1 Operational keys — OPS_MANAGER alone

Keys that OPS_MANAGER can update without countersignature:
- `comms.suppression.default_channel_preference`
- `workallocation.queue.priority_weights`
- Operational threshold adjustments that do not affect delegated authority ceilings

### 8.2 Structural keys — dual-entry (OPS_MANAGER + COMPLIANCE)

Keys that require both OPS_MANAGER and COMPLIANCE to confirm:
- `write_off_limit.tier1`
- `write_off_limit.tier2`
- `write_off_limit.tier3`
- `write_off_limit.tier4`
- Any key that affects delegated authority, regulatory obligation, or audit scope

Dual-entry pattern: the first actor submits a proposed value; the second actor must explicitly confirm it within a defined time window before the value becomes live. Proposed values are audited even if not confirmed.

---

## 9. Required Changes to Existing Documents

### 9.1 MASTER-DESIGN-DOCUMENT.md

**Find all instances of the placeholder RBAC statement.** The placeholder reads approximately:

> "The RBAC role model (AGENT, TEAM_LEADER, OPS_MANAGER, COMPLIANCE, ADMIN) is a placeholder. A full permissions matrix — covering write-off limits, override capability, geographic/team access restrictions, and specialist role profiles — must be designed before the `user` feature is built."

**Replace with:**

> "The RBAC role model has been fully designed. See `docs/project-foundation/RBAC-IMPLEMENTATION-DECISIONS.md` for the complete role set, permission matrix, write-off tier model, team scope enforcement, and case transfer process. The `user` feature implementation must follow the decisions in that document. The open design question on RBAC is closed except for DDE-OQ-02 (confirmed write-off thresholds, pending DWP sign-off)."

**Also update section on open design questions:** Remove write-off limits and override capability as open items. Retain only DDE-OQ-02 (threshold confirmation) as an open item.

### 9.2 ARCHITECTURE-BLUEPRINT.md section 9.2 and section 15

**Section 9.2 (Authorization) — replace:**

> "Placeholder roles exist; full permissions matrix remains pending and is a release-critical design input for `domain/user`."

**With:**

> "Full role set and permission matrix defined in `docs/project-foundation/RBAC-IMPLEMENTATION-DECISIONS.md`. Keycloak realm roles: AGENT, SPECIALIST_AGENT, ESCALATIONS_AGENT, TEAM_LEADER, OPS_MANAGER, SRO, COMPLIANCE, BACKOFFICE, PROCESS_DESIGNER. Client-scoped roles on `flowable-admin` client: FLOWABLE_ADMIN, SYSTEM. Roles ADMIN, LEGAL_AGENT, and DOMAIN_EXPERT are not provisioned — see RBAC-IMPLEMENTATION-DECISIONS.md for rationale."

**Section 15, item 1 — replace:**

> "RBAC permissions matrix with write-off and override thresholds by role."

**With:**

> "RBAC permissions matrix complete — see `RBAC-IMPLEMENTATION-DECISIONS.md`. One open item remains: DDE-OQ-02 (confirmed write-off tier threshold values, pending DWP client confirmation)."

### 9.3 ADR-004 write-off approval section

ADR-004 contains a placeholder noting the SRO role as "to be confirmed." Update:

- SRO is confirmed as an in-system actor with Keycloak realm role `SRO`.
- Provisioning sequence: SRO Keycloak user created before `write_off_limit.tier4` is set.
- Empty candidate group guard is the confirmed production behaviour (not a placeholder guardrail).
- DDE-OQ-03 is closed: SRO = Senior Responsible Officer, in-system, £50,000 seed threshold.
- DDE-OQ-04: maps to the empty candidate group guard + MANUAL_REVIEW_REQUIRED pattern. Implementation is the confirmed behaviour.

### 9.4 ADR-008 three fixes required

**Fix 1 — TEAM_LEADER governance vs operational permissions:**

Add a note to the governance table:

> "This governance table covers Tier 1/2/3 configurability access only. Operational case permissions (e.g., suppression override per ADR-001 DUAL_USE) are governed by the RBAC permission matrix in `RBAC-IMPLEMENTATION-DECISIONS.md`, not by this table. TEAM_LEADER is view-only for configurability governance; TEAM_LEADER has suppression override permission in operational context."

**Fix 2 — ADMIN deploy column reattributed to PROCESS_DESIGNER:**

In the ADR-008 governance table, find any row or column that assigns Tier 2 or Tier 3 deploy capability to `ADMIN`. Replace `ADMIN` with `PROCESS_DESIGNER` in those cells. The `ADMIN` role does not exist in the final role set.

**Fix 3 — DOMAIN_EXPERT reattributed to COMPLIANCE:**

In ADR-008, find all references to `DOMAIN_EXPERT` in the context of Class A approval gates. Replace `DOMAIN_EXPERT` with `COMPLIANCE`. Add a note:

> "DOMAIN_EXPERT was identified as an unprovisioned role. Per RBAC design review (2026-04-24), this governance gate function is absorbed by COMPLIANCE, which is an existing DWP role designation. DOMAIN_EXPERT is not provisioned in Keycloak."

### 9.5 ADR-010 DOMAIN_EXPERT reattribution

Same as ADR-008 fix 3 above. Find all references to `DOMAIN_EXPERT` in ADR-010. Replace with `COMPLIANCE`. Add the same note.

### 9.6 ADR-006 FLOWABLE_ADMIN audit actor

ADR-006 currently shows `actor_id = SYSTEM UUID` for Flowable Admin UI actions. This is identified in the ADR as a regulatory risk. The confirmed resolution is:

- FLOWABLE_ADMIN users must be provisioned as named individuals in Keycloak.
- `HistoryEventProcessor` implementation must resolve the actor from the JWT subject claim, not default to a SYSTEM UUID.
- If no authenticated user can be resolved (e.g., a background Flowable internal operation), the actor_id records as `SYSTEM` with an explicit flag `system_initiated = true` on the AUDIT_EVENT row — this is distinguishable from a human actor.
- Human-initiated Admin UI actions must never record as SYSTEM actor.

Update ADR-006 to mark this risk as resolved with the above implementation constraint.

### 9.7 RULING-003 updates

**DDE-OQ-03 — close:**

> "DDE-OQ-03 is closed. SRO = Senior Responsible Officer. In-system actor. £50,000 seed threshold provisioned via `write_off_limit.tier4` SYSTEM_CONFIG key. Provisioned as Keycloak realm role `SRO`. Decision date: 2026-04-24."

**DDE-OQ-04 — resolve:**

> "DDE-OQ-04 is resolved. Above-ceiling amounts (above Tier 4) route to COMPLIANCE notification + MANUAL_REVIEW_REQUIRED audit event. Empty candidate group guard prevents silent continuation. No hard service-layer rejection — no tender requirement mandates a block. Decision date: 2026-04-24."

**DDE-OQ-02 — remains open:**

> "DDE-OQ-02 remains open. Provisional thresholds: Tier 1 £500, Tier 2 £2,000, Tier 3 £10,000, Tier 4 £50,000. All values require DWP client confirmation before production configuration. Values are in SYSTEM_CONFIG, not hardcoded — can be updated without a code change once confirmed."

---

## 10. Keycloak Configuration Changes

### 10.1 Realm roles to create

```
AGENT
SPECIALIST_AGENT
ESCALATIONS_AGENT
TEAM_LEADER
OPS_MANAGER
SRO
COMPLIANCE
BACKOFFICE
PROCESS_DESIGNER
STRATEGY_MANAGER
```

### 10.2 Client-scoped roles to create (on `flowable-admin` client)

```
FLOWABLE_ADMIN
SYSTEM
```

### 10.3 Roles NOT to create (explicitly excluded)

```
ADMIN           — removed, rationale in section 1.3
LEGAL_AGENT     — out of scope, rationale in section 1.3
DOMAIN_EXPERT   — not provisioned, absorbed by COMPLIANCE, rationale in section 1.3
```

### 10.4 Spring Security configuration

Use `hasAuthority()` not `hasRole()` in `@PreAuthorize` annotations. The `hasRole()` method prepends `ROLE_` prefix; Keycloak JWT claims do not carry this prefix. Using `hasRole()` will silently fail all role checks.

Example:
```java
@PreAuthorize("hasAuthority('OPS_MANAGER') or hasAuthority('SRO')")
public void approveWriteOff(...) { ... }
```

---

## 11. Open Items

### 11.1 DDE-OQ-02 — Write-off threshold confirmation (open)

All four write-off tier thresholds are provisional seeds. They cannot be treated as confirmed values. DWP client must sign off on:
- Tier 1: £500
- Tier 2: £2,000
- Tier 3: £10,000
- Tier 4: £50,000

Until confirmed, these values should be documented as provisional and the SYSTEM_CONFIG entries should carry a `provisional = true` metadata marker if the schema supports it.

### 11.2 LEGAL queue — out of scope (closed, no action required)

The LEGAL team type (`TEAM.team_type = LEGAL`) and LEGAL queue type (`WORK_QUEUE.queue_type = LEGAL`) remain in the schema. They are used for referral recording only — when a case is referred to DWP legal services, the referral fact is recorded against a LEGAL queue entry.

No `LEGAL_AGENT` role is provisioned. No in-scope legal case handling is built. If legal case management is brought in scope in future, this decision must be revisited and a new RBAC entry created.

---

## 12. AUDIT_EVENT Types Referenced by RBAC Design

The following AUDIT_EVENT types are introduced or confirmed by this design:

| Event type | Trigger |
|---|---|
| `WRITE_OFF_REQUESTED` | Agent submits write-off request |
| `WRITE_OFF_APPROVED` | Approver confirms write-off |
| `WRITE_OFF_REJECTED` | Approver rejects write-off |
| `MANUAL_REVIEW_REQUIRED` | Above-ceiling routing, or empty candidate group guard triggered |
| `STATUS_CHANGE` (sub-type `CASE_TRANSFER`) | Case transferred between teams |
| `OVERRIDE` | Supervisor or OPS_MANAGER override action |
| `CONFIG_CHANGE` | SYSTEM_CONFIG key updated (includes actor_id of both actors for structural keys) |
| `CC_PROMOTION` | STRATEGY_MANAGER promotes a challenger DMN version to champion status |

Mandatory fields on `WRITE_OFF_APPROVED`:
- `actor_id` — approver UUID
- `amount`
- `debt_account_id`
- `tier` — which tier threshold applied
- `requestor_id` — must differ from `actor_id` (three-layer self-approval prevention)

---

*End of RBAC Implementation Decisions*
