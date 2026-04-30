# RULING-008: DCA Placement — Pre-Placement Disclosure Obligations

**Ruling ID:** RULING-008
**Linked issue:** TBD
**Status:** `awaiting-client-sign-off`
**Date issued:** 23 April 2026
**Domain expert:** DWP Debt Domain Expert

---

## Requirement IDs Covered

- INT.DCA-01 (DCA placement workflow)
- INT.DCA-02 (Pre-placement disclosure)
- INT.DCA-03 (Data sharing with DCAs)
- DIC.21 (ICO guidance on data sharing with third parties)

---

## Regulatory Basis

- **DWP pre-placement disclosure policy** — debtor notification before DCA referral
- **UK GDPR Article 6(1)(e)** — public task as lawful basis for data sharing
- **ICO guidance on data sharing with DCAs**
- **Note:** Consumer Credit Act 1974 Sections 86A–86F (notice of sums in arrears) do **not** apply to DWP statutory debt — DWP debts are not regulated credit agreements. DWP's own disclosure policy governs.

---

## Rule Statement

### 1. Pre-placement disclosure is required

DWP policy requires that the debtor is notified that their account may be referred to a DCA before placement occurs. This notification is a `DISCLOSURE_NOTICE` and must be sent via the debtor's preferred or consented channel.

The notification must include:
- The DCA's name (or that a DCA will be appointed)
- The amount being referred
- The debtor's right to dispute the debt

### 2. Minimum notice period before placement

There must be a minimum notice period between the disclosure notice and actual placement. The current design does not model this waiting period. The required notice period must be confirmed by DWP before the DCA placement workflow is built.

**Required addition:** A BPMN timer boundary event in the `DCA_PLACEMENT_PROCESS` that enforces the notice period. Placement may not be initiated until the timer has elapsed and no dispute has been received.

### 3. Data sharing requires lawful basis and DPA

Sharing personal data (PARTY details, debt information) with a DCA requires a lawful basis under UK GDPR Article 6. For DWP statutory debt, this is likely **Article 6(1)(e)** (public task). A **Data Processing Agreement** (DPA) must exist with each DCA before any placement occurs.

### 4. Placement event must log data shared

The `THIRD_PARTY_PLACEMENT` record must reference which data fields were shared and under which legal basis. The system must be capable of responding to a Subject Access Request showing what data was shared with whom and when.

### 5. DCA recall must be immediate

When a DCA placement is recalled, the DCA must be notified immediately so that the DCA ceases contact. The mock DCA API must model a recall acknowledgement response. The system must not consider a recall complete until the DCA has acknowledged it.

### 6. DWP communication suppression during placement

DWP's communications suppression during DCA placement (already in the design) is correct and must be maintained. The `CommunicationSuppressionService` must activate `SUPPRESSION_REASON = DCA_PLACEMENT_INTERNAL` on placement and deactivate it on recall.

---

## Edge Cases

### Debtor disputes debt after disclosure notice but before placement

If the debtor disputes the debt after the disclosure notice is sent but before the notice period expires, the placement must be blocked and the dispute process must be initiated. The BPMN timer must be cancelled and the process must route to the dispute workflow.

### DCA is wound up or loses accreditation during placement

If the DCA loses FCA accreditation during an active placement, the placement must be recalled immediately and a new DCA placement process must begin (if applicable). The system must store the DCA's accreditation status and check it before each placement action.

### Data Subject Access Request during placement

If a debtor submits a SAR while their account is with a DCA, the response must include the data shared with the DCA at placement time. The `THIRD_PARTY_PLACEMENT` audit log must contain sufficient detail to construct this response.

---

## Open Questions Requiring DWP Client Sign-Off

### DDE-OQ-10: Required notice period before DCA placement

**Question:** What is the minimum number of days between sending the pre-placement disclosure notice and initiating DCA placement?

**Status:** ⚠ **AWAITING DWP SIGN-OFF**

---

## Data Classification Flags

- `THIRD_PARTY_PLACEMENT` records — **Restricted; data transfer to third party**. Each record must include `data_fields_shared`, `legal_basis`, and `dca_id`.
- `AUDIT_EVENT` for placement — must include sufficient detail to respond to a SAR. **Restricted** during active placement.
- `DISCLOSURE_NOTICE` communications — Operational (standard debt communication).

---

## Guardrails — Builders Must Not Violate

1. The DCA placement service must validate that a `DISCLOSURE_NOTICE` event exists for the account and that the required notice period has elapsed before initiating placement.
2. The DCA placement service must validate that a Data Processing Agreement is on file for the target DCA (stored in a `DCA_REGISTER` table with `dpa_in_place = true`) before any data is shared.
3. Every `THIRD_PARTY_PLACEMENT` record must be written with `data_fields_shared` (list of fields transmitted), `legal_basis` (default `UK_GDPR_ART6_1E`), and `transmitted_at` timestamp.
4. Recall must be processed synchronously with the DCA API — the placement status must not be set to `RECALLED` until the DCA acknowledges. If the DCA API is unavailable, the recall must be queued and retried, and an alert raised.
5. DWP-side communications must remain suppressed until DCA recall is acknowledged. Suppression must not be lifted on initiation of recall — only on confirmed acknowledgement.

---

## Version History

| Version | Date | Author | Change |
|---|---|---|---|
| 1.0 | 23 Apr 2026 | DWP Debt Domain Expert | Initial ruling |
