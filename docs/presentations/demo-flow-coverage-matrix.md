# SUM-03: Demo Flow Coverage Matrix

Legend: `X` = requirement demonstrated; `P` = headline primary flow. Headline flows are Flow 2, Flow 5, and Flow 12.

| Flow | Primary | One-line description | R1 | R2 | R3 | R4 | R5 | R6 | R7 | R8 | R9 | R10 | R11 | R12 | R13 | R14 | R15 | R16 | R17 | R18 | R19 | R20 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1. Intake to first contact |  | New accounts are ingested, profiled, segmented, contacted, and queued before agents log in. |  | X | X |  | X |  |  | X | X |  |  |  | X |  |  |  |  |  |  |  |
| 2. Vulnerability to resolution | P | Vulnerability disclosure triggers suppression, specialist routing, I&E, arrangement creation, and audit. |  |  |  | X |  | X | X |  |  |  |  | X | X |  | X |  |  | X |  |  |
| 3. Breach to DCA placement |  | Missed payments trigger breach workflow, automated contact, DCA placement, and reconciliation. |  |  | X | X | X |  |  |  |  | X | X | X |  |  |  |  |  |  |  | X |
| 4. Complex household |  | Multiple debts, joint liability, legal state, and breathing-space restrictions are coordinated together. | X |  | X |  | X |  | X |  |  |  |  | X |  |  |  |  |  | X |  | X |
| 5. Strategy change without IT | P | A business user designs, simulates, tests, and promotes a strategy change with governed rollback. |  |  | X |  |  |  |  |  | X |  |  |  |  |  |  | X |  |  | X |  |
| 6. Executive dashboard |  | Leadership sees queue health, breach trends, strategy performance, exceptions, and exports. |  |  | X |  |  |  | X | X | X |  | X | X | X |  |  |  |  |  |  |  |
| 7. Self-service handoff |  | The external portal/app sends I&E, arrangement, payment, and engagement events into DCMS. |  |  |  | X |  | X |  |  |  |  |  |  | X | X |  |  |  |  |  |  |
| 8. Dispute |  | A dispute freezes collection activity, opens investigation, corrects the record, and preserves audit. |  |  | X |  |  |  | X |  |  |  | X | X |  |  |  |  |  |  |  |  |
| 9. Deceased protocol |  | Death notification suppresses collection, starts estate handling, and blocks unsafe agent actions. |  |  | X |  | X |  |  |  |  |  |  | X |  |  | X |  |  | X |  |  |
| 10. Write-off and reactivation |  | Exhausted recovery routes lead to write-off approval, later payment detection, and reactivation. |  |  | X |  |  |  | X |  |  | X | X | X |  |  |  |  |  |  |  |  |
| 11. New agent journey |  | A new agent is guided by queues, scripting, authority checks, escalation, and supervisor coaching. |  |  |  |  |  | X | X | X |  |  |  | X |  |  | X |  |  | X |  |  |
| 12. Regulatory audit | P | Audit reconstructs a disputed contact event with timestamps, rule versions, actor IDs, and export. |  |  |  |  |  |  | X |  |  |  |  | X | X |  |  |  |  | X |  |  |
| 13. Month-end surge |  | Batch processing, queue recalibration, bulk reassignment, communications, and reporting handle volume. |  | X | X |  | X |  |  | X | X |  | X |  |  |  |  |  |  |  |  |  |
| 14. Settlement offer |  | Agent-negotiated settlement is checked against authority, approved, booked, and audited. |  |  |  | X |  |  | X |  |  | X | X |  | X |  |  |  |  |  |  |  |

## Requirement Legend

| ID | Behavioural requirement |
|---|---|
| R1 | Relationship management |
| R2 | Data capture and integration pipeline |
| R3 | Workflow automation and override |
| R4 | Repayment arrangement lifecycle |
| R5 | Multi-channel communication |
| R6 | I&E affordability assessment |
| R7 | RBAC and audit |
| R8 | Work queue distribution |
| R9 | Decisioning and strategy optimisation |
| R10 | Third-party placement and reconciliation |
| R11 | Financial accounting and reconciliation |
| R12 | Exception detection and escalation |
| R13 | Agent logging and history |
| R14 | Self-service and API integration |
| R15 | Dynamic UI and agent scripting |
| R16 | Controlled change management |
| R17 | Legacy migration |
| R18 | Vulnerability and compliance |
| R19 | Roadmap and upgrades |
| R20 | Parallel process management |

