# Release 1 Gap Analysis — Demo Flow Capabilities vs Tender Requirements

> **Date:** 2026-04-30
> **Basis:** Six primary demo flows as documented in [`docs/working/demo-flow-capabilities.md`](demo-flow-capabilities.md), cross-referenced against [`Tender requirements docs/Functional-Requirements-Consolidated.md`](../../Tender%20requirements%20docs/Functional-Requirements-Consolidated.md).
> **Scope:** Must Have requirements that are either absent from, or only partially addressed by, the current capability list. Should Have requirements are noted separately where they are likely to be visible during demonstration.
> **Update status:** Remaining Tier 1, Tier 2, and Tier 3 items from this analysis have now been incorporated into the consolidated Release 1 capability list in [`docs/working/demo-flow-capabilities.md`](demo-flow-capabilities.md). This document remains as the trace of why those additions were made.
> **Release scope authority:** The authoritative Release 1 scope baseline is [`../release/release-1-capabilities.md`](../release/release-1-capabilities.md).

---

## How to Read This Document

The demo flow capabilities document is comprehensive for the *story* of each flow. The gaps below are capabilities that the six flows depend on but have not yet been written down — and Must Have requirements from the tender that fall outside the flow narratives entirely.

Gaps are grouped into three tiers:

- **Tier 1 — Demo-blocking:** The six flows cannot run without this. It must be in Release 1 scope.
- **Tier 2 — Tender-required, not in flows:** Must Have requirements not surfaced by any of the six flows. Release 1 must satisfy them even if they are not demoed.
- **Tier 3 — Should Have / visible risk:** Not Must Have, but likely to be questioned by evaluators.

### Integration Scope Principle

Release 1 is not expected to build live integrations to third-party or DWP strategic components where access is not available. It is also not expected to include live outbound contact capabilities such as a production dialler, automated SMS sender, email sender, or payment-link provider. For those requirements, Release 1 must demonstrate DCMS-side integration and orchestration readiness: the owned adapter boundary, request and response contracts, state transitions, retry/error handling assumptions, audit evidence, and simulated responses from stubs or test harnesses.

The goal is to show that DCMS can support the integration and that the surrounding workflow behaves correctly. Production connectivity can then replace the simulator when third-party access is available.

---

## Tier 1 — Demo-Blocking Gaps

These are capabilities that the flow scripts assume exist but that are not yet written into the capability list.

### 1.1 Dialler Integration — Predictive, Progressive, and Preview Modes

**Tender requirements:** CC.28, I3PS.14
**Flows affected:** Flow 1, Flow 2, Flow 3, Flow 6

Every breach workflow and outbound contact sequence references "outbound call tasks." The tender requires a fully integrated dialler operating in three modes: predictive, progressive, and preview. The capability list records only "create outbound call tasks for agents." That is call-task creation — not dialler integration. These are materially different. A call task that sits in a queue is not a dialler.

Release 1 must explicitly document the boundary. DCMS should demonstrate the outbound call-task contract, queueing rules, expected dialler status callbacks, and workflow behaviour using a simulator or stubbed adapter. A live dialler, predictive dialler operation, or real outbound calling capability is not required for the Release 1 demo.

---

### 1.2 Disclosure Notification — Automatic Generation at Configurable Points

**Tender requirements:** DW.27
**Flows affected:** Flow 3 (pre-DCA placement)

The tender is explicit: the system must automatically generate disclosure notifications at configurable points in the journey, with pre-DCA placement called out as the primary example. Flow 3 covers the full breach-to-placement sequence but does not mention disclosure generation. This is a legally consequential Must Have that must be in the Release 1 capability list and built into the placement workflow.

---

### 1.3 Delegated Authority Enforcement on Manual Actions

**Tender requirements:** WAM.24, UAAF.21, AAD.20
**Flows affected:** Flow 2 (arrangement creation), Flow 3 (supervisor override of placement), Flow 6 (bulk reassignment)

The flows describe RBAC at a high level. The tender requires the system to *actively recommend escalation* when an agent's proposed action exceeds their delegated authority — for example, an arrangement amount above a threshold triggering automatic referral to a team leader. The capability list mentions RBAC role definitions and permission blocking, but not the authority-limit referral logic that turns a permission boundary into an automated escalation.

The distinction is between *blocking* an action (covered) and *routing it upward for approval* (not covered).

---

### 1.4 Screen Pop for Inbound Calls

**Tender requirements:** CC.19
**Flows affected:** Flow 2

Flow 2 references "identify the customer at the point of an inbound call and surface their account automatically (screen pop)" — but screen pop does not appear in the consolidated capability list. It is present in the flow narrative but absent from the Release 1 scope section. This must be made explicit as a capability.

---

### 1.5 Sub-Process Re-Entry After Promise-to-Pay Detour

**Tender requirements:** DW.17
**Flows affected:** Flow 3

The capability list states "record a promise-to-pay outcome and create a new monitoring step for the promised payment." Flow 3 further specifies "if the promised payment is not received, resume the breach workflow from the appropriate point." The resume-from-position behaviour — the ability for an account to re-enter a sub-process and return to the same point in the main process — is not stated as a system capability. Without it, breach workflows that involve a PTP detour lose their position and the automation breaks.

---

### 1.6 Interest Freeze and Automatic Suppression

**Tender requirements:** DW.58, DW.79
**Flows affected:** Flow 2 (vulnerability / breathing space), Flow 4 (restrictions)

Two Must Have requirements deal with interest handling: the ability to freeze and unfreeze interest manually, and automatic interest suppression when accounts reach a configured stage. Neither appears anywhere in the capability list. Flow 2 and Flow 4 both involve breathing space and vulnerability periods where interest suppression would apply.

---

### 1.7 Benefit Offset Against Debt

**Tender requirements:** RPF.35, DIC.31
**Flows affected:** Flow 1 (enrichment), Flow 2 (affordability)

The tender requires the system to record amounts owed *to* the customer (e.g. unclaimed benefits, overpaid tax) and to consider those amounts as potential offsets against existing debts. The I&E and affordability capability list does not mention this. It is a DWP-specific requirement — DWP can offset benefit entitlements against overpayments — and it will be visible in any I&E affordability conversation.

---

### 1.8 Account Re-Entry to Internal Strategy After Recall from Third-Party Placement

**Tender requirements:** AAD.21
**Flows affected:** Flow 3

The capability list covers sending a recall instruction and lifting the placement flag. The tender requires that on recall, the account is automatically reinstated to the most appropriate internal collection strategy — not just unmarked as placed. The "what happens next" after recall is not specified. Flow 3 ends at "reconciliation record created" with no return path to internal recovery.

---

## Tier 2 — Tender-Required Capabilities Not Covered by the Six Flows

These are Must Have requirements that are not naturally surfaced by any of the six demo flows, but the system must satisfy them regardless.

### 2.1 Host System Action Invocation — Bidirectional Integration

**Tender requirements:** I3PS.6
**Flows affected:** All intake and financial flows

The data intake model in Flow 1 describes receiving data from host systems. The tender also requires DCMS to invoke actions *back* to the host system: write-off postings, account blocks, closures, account management, card management, customer detail updates. This is bidirectional integration, not just inbound. For Release 1, live host-system access is not required, but DCMS must define and demonstrate the outbound invocation contract, status handling, audit trail, and reconciliation behaviour using a simulated host adapter.

---

### 2.2 General Ledger Integration

**Tender requirements:** I3PS.9
**Flows affected:** Flow 3, all financial flows

Payment allocation is covered in Flow 3. The tender requires integration with the general ledger / core accounting system so that financial transactions in DCMS are posted to the GL. This is not mentioned anywhere in the capability list. For Release 1, the expected scope is not live GL posting, but a defined GL posting contract, simulated posting acknowledgements/rejections, reconciliation states, and audit evidence for financial events that would be sent to the GL.

---

### 2.3 Customer Contact Preferences Driving Automated Strategy Execution

**Tender requirements:** CC.24
**Flows affected:** Flow 1, Flow 2, Flow 3

The communication capability mentions queuing outbound letters and SMS. The tender requires the system to capture the customer's preferred contact times, preferred channels, language requirements, and accessibility needs — and for those preferences to *automatically govern* what the contact strategy does at runtime. This is more than storing a preference field; it is a runtime constraint on automated contact decisions. Release 1 should demonstrate the orchestration decision and queued/simulated channel request, not live message delivery.

---

### 2.4 Contact Frequency Limits and Time-Window Controls

**Tender requirements:** CC.27
**Flows affected:** All communications flows

The tender requires configurable rules limiting how many times a customer can be contacted per period, and within what daily time windows — with those limits updateable when legislation or DWP policy changes. Without this, the communications engine could breach FCA / DWP contact rules on its own automation. Release 1 should enforce those rules before creating simulated outbound contact requests; it does not need to send live messages or calls.

---

### 2.5 Automatic Recording of Failed Outbound Calls and Channel Fallback

**Tender requirements:** CC.30, DW.34, DW.70
**Flows affected:** Flow 3

The capability list states "record every contact event outcome." The tender specifies that failed outbound calls must be automatically recorded (not reliant on an agent updating the record) and must trigger follow-up contact via an alternative channel. Release 1 should demonstrate this through simulated dialler/SMS outcomes and fallback orchestration; it does not need live outbound calls or automated SMS delivery.

---

### 2.6 Overpayment Detection and Refund Initiation

**Tender requirements:** DW.28
**Flows affected:** Financial flows

The payment allocation capability covers allocating incoming payments. The tender requires the system to identify overpayments and support initiating a refund from within DCMS, controlled by user role. This is not covered in the capability list.

---

### 2.7 Bulk Account Moves Within a Workflow — Forward and Backward

**Tender requirements:** DW.15
**Flows affected:** Operational flows

The supervisor capability includes bulk reassignment between queues. The tender also requires bulk moves *within a workflow* — advancing or reversing groups of accounts to a specific step in a treatment path. This is distinct from queue reassignment. An example: moving a cohort back to step 1 of a strategy because a rule change means they must restart. The capability list does not cover this.

---

### 2.8 SLA Push Alerts and Breach Reporting

**Tender requirements:** WAM.9, WAM.26
**Flows affected:** Flow 6

Flow 6 references queue ageing and SLA risk flags. The tender requires on-screen alerts and email notifications when SLA thresholds are approached or breached, plus dedicated reports identifying accounts not actioned within SLA timeframes. A dashboard metric requires a user to look; an SLA alert is pushed. The push-notification aspect is not stated in the capability list.

---

### 2.9 Workforce Calendar — Holiday, Sickness, and Non-Working Days

**Tender requirements:** UAAF.3, UAAF.9, WAM.15
**Flows affected:** Flow 1, Flow 6

Queue assignment is described as automatic and rules-based. The tender requires the system to track agent availability (including holiday and sickness records) and to account for geographic calendars and non-working days when distributing work and scheduling actions. An action diarised for a bank holiday must roll to the next working day automatically. None of this appears in the capability list.

---

### 2.10 Fraud Flag

**Tender requirements:** DW.84
**Flows affected:** Flow 4 (restriction types)

The restriction types listed in the capability list are: breathing space, dispute, deceased, legal. The tender lists fraud as a separately flaggable account state with Must Have status. Fraud is absent from the restriction list and does not appear in the capability list at all.

---

### 2.11 Archive and Purge for Non-Delinquent and Written-Off Accounts

**Tender requirements:** CAS.14
**Flows affected:** Post-write-off flows

Written-off and non-delinquent account archival is referenced briefly in Flow 3. The tender requires configurable archive and purge capabilities for non-delinquent and written-off accounts. This is not stated in the capability list.

---

### 2.12 Predictive Analytics, Forecasting, and Champion/Challenger Analysis

**Tender requirements:** A.1, A.2
**Flows affected:** Flow 5, Flow 6

Flow 5 and Flow 6 include champion/challenger configuration and comparative reporting, but the tender requirements go further: champion/challenger strategies must be configurable on predictive analysis, and the decisioning layer must analyse and forecast trends and segment debtors and debts. The current capability list covers comparative performance reporting, but does not explicitly state predictive analysis or trend forecasting as decisioning capabilities.

---

### 2.13 Direct Debit Setup

**Tender requirements:** RPF.22
**Flows affected:** Flow 2

Flow 2 creates a repayment arrangement from an agreed amount and frequency. The tender requires Direct Debit setup, coordination, and resubmission to be controlled by business rules. Release 1 should define the Direct Debit setup and resubmission workflow boundary and demonstrate the DCMS-side arrangement behaviour. Live bank-detail validation and API integration to a banking/payment provider are out of Release 1 scope.

---

### 2.14 Campaign Management with Automated Response Capture

**Tender requirements:** CC.26
**Flows affected:** Flow 1, Flow 3, Flow 5

The communications capability covers queuing outbound letters and SMS, and Flow 5 covers strategy authoring. The tender also requires campaigns and strategies that allow customers to make a payment, arrangement, offer, or settlement, with the interaction captured automatically without manual intervention. Release 1 should define the campaign-response contract and demonstrate automatic capture through simulated outbound channel requests and simulated customer responses, because live contact channels and external channel surfaces are not available.

---

## Tier 3 — Should Have Requirements Likely to Surface During Demonstration

These are not Must Have but are sufficiently high-profile that evaluators are likely to ask about them.

| Requirement | Description | Risk if absent from demo |
|---|---|---|
| DW.25 | Pre-configured strategies for Deceased, Insolvency, Hardship, Persistent Debt | Flow 4 references these protocols; out-of-box templates will be expected |
| DW.26 | Pre-arrears / non-delinquent customer strategy support | Operations-background evaluators commonly probe early-intervention capability |
| DIC.4 | Admin user ability to extend the data model without code changes | Evaluators focused on configurability will probe this |
| UAAF.20 | GDPR data anonymisation, archive, and purge support | CAS.14 covers Must Have archive/purge; evaluators may still ask how anonymisation supports GDPR obligations |

---

## Summary Table

| Gap | Area | Tier | Must Have Req(s) | Flows Affected |
|---|---|---|---|---|
| 1.1 | Dialler integration — predictive, progressive, preview | 1 | CC.28, I3PS.14 | F1, F2, F3, F6 |
| 1.2 | Disclosure notification — pre-DCA | 1 | DW.27 | F3 |
| 1.3 | Delegated authority referral / escalation | 1 | WAM.24, UAAF.21, AAD.20 | F2, F3, F6 |
| 1.4 | Screen pop (capability list omission) | 1 | CC.19 | F2 |
| 1.5 | Sub-process re-entry after PTP detour | 1 | DW.17 | F3 |
| 1.6 | Interest freeze / automatic suppression | 1 | DW.58, DW.79 | F2, F4 |
| 1.7 | Benefit offset against debt | 1 | RPF.35, DIC.31 | F1, F2 |
| 1.8 | Recall → auto re-strategy | 1 | AAD.21 | F3 |
| 2.1 | Host system action invocation — bidirectional | 2 | I3PS.6 | All intake / financial flows |
| 2.2 | General ledger integration | 2 | I3PS.9 | F3, financial flows |
| 2.3 | Contact preferences driving strategy execution | 2 | CC.24 | F1, F2, F3 |
| 2.4 | Contact frequency / time-window limits | 2 | CC.27 | All comms flows |
| 2.5 | Auto-record failed calls + channel fallback | 2 | CC.30, DW.34, DW.70 | F3 |
| 2.6 | Overpayment detection and refund | 2 | DW.28 | Financial flows |
| 2.7 | Bulk move within workflow — forward and backward | 2 | DW.15 | Operational flows |
| 2.8 | SLA push alerts and breach reporting | 2 | WAM.9, WAM.26 | F6 |
| 2.9 | Workforce calendar — holiday, sickness, non-working days | 2 | UAAF.3, UAAF.9, WAM.15 | F1, F6 |
| 2.10 | Fraud flag | 2 | DW.84 | F4 |
| 2.11 | Archive and purge for non-delinquent / written-off accounts | 2 | CAS.14 | Post-write-off flows |
| 2.12 | Predictive analytics, forecasting, champion/challenger analysis | 2 | A.1, A.2 | F5, F6 |
| 2.13 | Direct Debit setup | 2 | RPF.22 | F2 |
| 2.14 | Campaign management with automated response capture | 2 | CC.26 | F1, F3, F5 |

---

## Recommended Next Steps

1. **Confirm the dialler boundary** (gap 1.1) as the highest-priority architecture decision. DCMS should produce call tasks and consume dialler outcomes through a documented boundary, demonstrated with a simulator. Live dialler operation, vendor selection, and real outbound calling are outside Release 1 demo scope unless access and delivery scope change.

2. **Add gaps 1.1–1.8 to the Release 1 capability list** immediately. These are demo-blocking. Flows 2 and 3 in particular cannot be credibly demonstrated without them.

3. **Scope gaps 2.1–2.14 into the Release 1 technical backlog** even where they are not demoed. These are Must Have tender requirements. Leaving them unscoped risks a scoring gap in the written response.

4. **State the DWP strategic component and contact-channel boundaries explicitly in the capability list.** The tender requires the *system* to support integrations with the customer portal, the dialler, the GL, and outbound communications. If those components sit outside DCMS or live contact channels are unavailable for Release 1, the capability list must still state the DCMS-side responsibility, expected handoff, stub/simulator approach, and demo evidence required. The current capability list conflates DCMS-owned capabilities with assumed DWP strategic components and live channel delivery.
