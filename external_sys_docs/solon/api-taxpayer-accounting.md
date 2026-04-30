# Taxpayer Accounting API — Reference
**Version:** 2.1.0  
**Base path:** `/taxpayer-accounting` (assumed; routed via Solon API Gateway)  
**Auth:** Bearer JWT from Keycloak on every request

> This is Solon's core financial accounting service. It owns liabilities, bill segments, payment events, payment plans, financial transactions, and overpayment processes. The additional layer reads financial data here and triggers financial lifecycle actions (pause plan, write-off, refund).

---

## Endpoints by resource group

### Liabilities

| Method | Path | Summary |
|---|---|---|
| GET | `/liabilities` | Search liabilities |
| GET | `/liabilities/{id}` | Get liability by ID |
| PUT | `/liabilities/{id}/activate` | Activate a liability |
| PUT | `/liabilities/{id}/cancel` | Cancel a liability |
| GET | `/liabilities/{id}/bill-segments` | Get bill segments for a liability |

---

### Bill Segments

| Method | Path | Summary |
|---|---|---|
| GET | `/bill-segments` | Search bill segments |
| GET | `/bill-segments/{id}` | Get bill segment by ID |
| POST | `/bill-segments/quick-freeze` | Create, generate, and freeze a bill segment in one operation |
| PUT | `/bill-segments/{id}/cancel` | Cancel a bill segment |
| GET | `/bill-segment-types` | List bill segment type configuration |
| GET | `/bill-types` | Search bill types |
| GET | `/bill-types/{code}` | Get bill type by code |

**`TransferAmountQuickFreeze` schema (for `POST /bill-segments/quick-freeze`):**

| Field | Notes |
|---|---|
| `sourceLiabilityId` | Liability the charge originates from |
| `sourceBillSegmentId` | Source bill segment |
| `sourcePaymentSegmentId` | Source payment segment |
| `overpaymentProcessId` | Linked overpayment process |
| `offsetAmount` | Amount to apply |
| `billSegmentTypeCode` | Type of bill segment to create |
| `billTypeCode` | Bill type |

---

### Bills

| Method | Path | Summary |
|---|---|---|
| GET | `/bills` | Search bills |
| GET | `/bills/{id}` | Get bill by ID |
| PUT | `/bills/{id}/cancel` | Cancel a bill |
| PUT | `/bills/{id}/complete` | Complete a bill |

---

### Payment Plans

| Method | Path | Summary |
|---|---|---|
| GET | `/pay-plans` | Search payment plans |
| GET | `/pay-plans/{id}` | Get payment plan by ID |
| PUT | `/pay-plans/{id}/activate` | Activate a payment plan |
| PUT | `/pay-plans/{id}/complete` | Complete a payment plan |
| PUT | `/pay-plans/{id}/cancel` | Cancel a payment plan |
| PUT | `/pay-plans/{id}/pause` | **Pause a payment plan** (relevant for vulnerability / breathing space) |
| GET | `/pay-plan-types` | List payment plan type configuration |

---

### Payment Events

| Method | Path | Summary |
|---|---|---|
| GET | `/payment-events` | Search payment events |
| PUT | `/payment-events/{id}/distribute` | Distribute a payment across liabilities |
| PUT | `/payment-events/{id}/complete` | Complete a payment event |
| GET | `/payment-hints` | Search payment hints |
| GET | `/payment-orders` | Search payment orders |
| GET | `/payment-files` | Search payment files |
| GET | `/payment-records` | Search payment records |

---

### Overpayment Processes (Waivers, Write-offs, Refunds)

| Method | Path | Summary |
|---|---|---|
| GET | `/overpayment-processes` | Search overpayment processes |
| PUT | `/overpayment-processes/{id}/actions/{entityStatusCL}` | Execute a status transition on an overpayment process |
| POST | `/overpayment-processes/offset` | Offset an overpayment against an outstanding liability |
| POST | `/overpayment-processes/refund` | Process a refund for an overpayment |
| POST | `/overpayment-processes/refund-interest` | Refund interest on a bill segment |
| POST | `/overpayment-processes/write-off` | **Write off an overpayment** |
| POST | `/offsetting/distribute` | Distribute offsetting across obligations |

**`OverpaymentProcessWriteOff` schema:**

| Field | Notes |
|---|---|
| `overpaymentProcessId` | The overpayment process to write off |
| `remainingAmount` | Amount remaining to write off |

**`OverpaymentProcessRefund` schema:**

| Field | Notes |
|---|---|
| `overpaymentProcessId` | The process to refund |
| `remainingAmount` | Amount to refund |

**`OverpaymentProcessOffset` schema:**

| Field | Notes |
|---|---|
| `overpaymentProcessId` | The process to offset |

---

### Financial Transactions

| Method | Path | Summary |
|---|---|---|
| GET | `/financial-transactions` | Search financial transactions |
| PUT | `/financial-transactions/{id}/post` | Post a financial transaction |
| GET | `/financial-transactions/{id}/versions` | Get version history |
| GET | `/financial-transaction-types` | List transaction type configuration |

---

### Assessments

| Method | Path | Summary |
|---|---|---|
| GET | `/assessments` | Search assessments |
| GET | `/assessments/{id}` | Get assessment by ID |
| PUT | `/assessments/{id}/cancel` | Cancel an assessment |
| PUT | `/assessments/{id}/complete` | Complete an assessment |
| PUT | `/assessments/{id}/is-obsolete` | Check if assessment is obsolete |
| GET | `/assessment-types` | List assessment type configuration |
| GET | `/assessment-types/dropdown` | Dropdown list of assessment types |
| GET | `/assessment-calculation-types` | List calculation types |

---

### Allocation

| Method | Path | Summary |
|---|---|---|
| GET | `/allocation-links` | Search allocation links |
| POST | `/allocation-links/allocate/{obligationActionId}` | Allocate an obligation |
| POST | `/allocation-links/testAllocate/{obligationActionId}` | Test allocation without committing |
| GET | `/allocation-segments` | Search allocation segments |
| GET | `/allocation-segments/{id}` | Get allocation segment by ID |
| GET | `/allocation-segment-types` | List allocation segment types |
| GET | `/allocation-segment-types/dropdown` | Dropdown list |
| DELETE | `/allocation-segment-types/{code}` | Delete a segment type |

---

### Accounting Calendars & Periods

| Method | Path | Summary |
|---|---|---|
| GET | `/accounting-calendars` | Search accounting calendars |
| POST | `/accounting-calendars` | Create a new accounting calendar |
| GET | `/accounting-calendars/{code}` | Get by code |
| PUT | `/accounting-calendars/{code}` | Update |
| DELETE | `/accounting-calendars/{code}` | Delete |
| GET | `/accounting-calendars/{code}/exists` | Check existence |
| GET | `/accounting-periods` | List accounting periods |
| GET | `/accounting-periods/{id}/exists` | Check existence |

---

### General Ledger

| Method | Path | Summary |
|---|---|---|
| GET | `/journal-entries` | Search journal entries |
| GET | `/gl-accounts` | Search GL accounts |
| GET | `/ledgers` | Search ledgers |

---

### Financial Details & Summaries

| Method | Path | Summary |
|---|---|---|
| GET | `/financial-details/*` | Financial summaries by debt category, obligation, revenue type |
| GET | `/icarus-self-service/*` | Party role financial data (used by Self-Service Portal) |

---

### Configuration Resources

| Method | Path | Summary |
|---|---|---|
| GET | `/deposit-registers` | List deposit registers |
| GET | `/deposit-sources` | List deposit sources |
| GET | `/dynamic-rules` | List dynamic rules |

---

## Architectural significance for the additional layer

### Pay Plans (confirmed)
Full payment plan lifecycle is owned here. The additional layer's **Repay Plan & I&E UI** creates/manages plans via this API:
- Search existing plans: `GET /pay-plans?partyRoleId={id}`
- Pause during vulnerability/breathing space: `PUT /pay-plans/{id}/pause`
- Cancel on account closure: `PUT /pay-plans/{id}/cancel`

### Write-offs and Waivers (confirmed)
`POST /overpayment-processes/write-off` confirms write-off capability. The **Fraud & Dispute** handling and vulnerability resolution flows can trigger write-offs or refunds via this API.

### Bill Segments
Bill segment data that appears in `GET /case-management/cases/{id}/balance` originates from this service. The `POST /bill-segments/quick-freeze` operation creates and freezes charges in a single step — relevant for penalty interest or fee generation.

### Payment Distribution
`PUT /payment-events/{id}/distribute` allocates received payments across liabilities — relevant to the Third-Party Collection Reconciliation & Commission Calculation Service when reconciling payments from third-party collection accounts.

### Self-Service Portal data
`GET /icarus-self-service/*` endpoints provide party role financial summaries, confirming that the Self-Service Portal's debt summary view is backed by this API.

---

## Common headers

| Header | Values | Purpose |
|---|---|---|
| `Authorization` | `Bearer {jwt}` | Keycloak JWT — required |
| `irm-origin-client` | `UI-CLIENT` or `FORMS` | Origin indicator |
| `irm-include-labels` | `TRUE` or `FALSE` | Whether to include labels |
| `Accept-Language` | e.g. `en-GB` | Language for returned descriptions |
