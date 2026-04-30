# The 20 Major Behavioral Requirements

**Current baseline note:** This document summarises the broad tender/strategic behavioural requirement set. It is not the current delivery-scope register. Customer self-service and portal/app journeys are strategic or future-scope items in the current design baseline unless scope is explicitly reopened.

## 1. Multi-Dimensional Customer & Debt Relationship Management
The system must manage customers with multiple accounts, joint liabilities, and complex household relationships — tracking all relationship history and transitions.

Contributing sections: Customer & Account Structure, Decisions & Workflows, Data & Information Capture, Work Allocation

Key IDs: CAS.1–4, CAS.15, CAS.17, DW.36–37, DW.50, DIC.20

MoSCoW: 100% Must

---

## 2. Comprehensive Data Capture, Validation & Integration Pipeline
The system must serve as the definitive repository — ingesting data from bulk APIs, real-time feeds, agents, and future self-service channels where in scope; validating against rules; and exposing data consistently across UI, reporting, and comms channels.

Contributing sections: Data & Information Capture (37 reqs), System of Record, Interfaces to 3rd Party Systems, Contact Channels

Key IDs: DIC.1–37, SoR.18–20, I3PS.2–3, I3PS.5, I3PS.11

MoSCoW: ~90% Must

---

## 3. Real-Time Workflow Automation with Manual Override
The system must automatically route and move accounts through treatment paths based on rules and events, while allowing supervisors to override, escalate, or re-route manually at exception points.

Contributing sections: Decisions & Workflows (88 reqs), Work Allocation, Agent Actions

Key IDs: DW.1–88, DW.5 (strategy changes), DW.11 (exception override), DW.15 (bulk move), WAM.2, WAM.16

MoSCoW: ~95% Must — largest single behavioral requirement by volume

---

## 4. Flexible, Monitored Repayment Arrangement Lifecycle
The system must create and manage repayment plans with flexible terms, automatically monitor compliance, trigger breach workflows, support automated re-assessment, and escalate failures — integrated with payment capture.

Contributing sections: Repayment Plan Functionality (37 reqs), Decisions & Workflows, Data & Information Capture

Key IDs: RPF.1–37, DW.22, DW.61–62, DIC.11, DIC.32–34

MoSCoW: ~95% Must

---

## 5. Multi-Channel Communication Strategy Execution
The system must automatically orchestrate outbound and inbound communications (letters, SMS, email, IVR, dialler, chat) per strategy and account events — tracking all messages, handling failures, and suppressing based on rules.

Contributing sections: Contact Channels (35 reqs), Decisions & Workflows, Data & Information Capture, User Interface, Repayment Plans

Key IDs: CC.1–35, DW.33, DW.43, DW.51, DW.65–76, DIC.10/15/17/21

MoSCoW: ~88% Must

---

## 6. Income & Expenditure-Driven Affordability Assessment
The system must collect I&E data via staff-facing journeys in current scope and via self-service only if that future scope is reopened; validate against bureau/CRA data; automatically generate repayment options; and use affordability as a direct input to collection strategy routing.

Contributing sections: Income & Expenditure Capture (11 reqs), Repayment Plans, Decisions & Workflows, Bureau & Scorecard Feeds

Key IDs: IEC.1–11, RPF.1, RPF.19, DW.22, DIC.5–6, BSF.2

MoSCoW: ~88% Must

---

## 7. Role-Based Access Control with Audit & Delegation
The system must enforce fine-grained RBAC across all data, functions, and actions; log all access and activity; support delegation of authority (write-off limits, reversals); and integrate with DWP SSO.

Contributing sections: User Access & Admin Functions (26 reqs), Agent Actions, Work Allocation, Data & Information Capture, UI

Key IDs: UAAF.1–26, AAD.4, AAD.11–12, WAM.13, DIC.25, UI.12, UI.19

MoSCoW: ~96% Must

---

## 8. Intelligent Work Queue Distribution & Real-Time Monitoring
The system must automatically assign work by skill, availability, and priority; allow supervisors to manually redistribute; display personalised queues; track SLAs; and report on completion in near real-time.

Contributing sections: Work Allocation & Monitoring (28 reqs), Decisions & Workflows, Data & Information Capture, User Access

Key IDs: WAM.1–28, DW.24, DW.64, DW.80, DIC.1/3, UAAF.19

MoSCoW: ~93% Must

---

## 9. Advanced Decisioning, Segmentation & Strategy Optimisation
The system must support rule-based and analytical decisioning (including champion/challenger testing), segment accounts by configurable criteria, and allow business users to model/test strategy changes before go-live without IT involvement.

Contributing sections: Decisions & Workflows, Analytics, Bureau & Scorecard Feeds, Customer & Account Structure

Key IDs: DW.2–4, DW.7, DW.38–42, A.1–2, BSF.2–15, CAS.8/11/12

MoSCoW: Must/Should mix — Analytics (2/2 Must), BSF mostly Should

---

## 10. Third-Party Placement, Reconciliation & Performance Tracking
The system must place accounts with external agencies (rules-based or manually), track their performance and payments, reconcile balances, calculate commissions, and recall debts — preventing duplicate placements.

Contributing sections: 3rd Party Management (18 reqs), Agent Actions, Data & Information Capture, System of Record, Interfaces

Key IDs: 3PM.1–18, AAD.21–22, DIC.19/28, SoR.4–6, I3PS.1

MoSCoW: Primarily Should Have — notably lighter on Must than other areas

---

## 11. Financial Accounting, Balance Management & Reconciliation
The system must act as an accounting engine — tracking all transactions, calculating interest/fees, allocating payments per rules, reconciling against GL/host systems, and maintaining a full audit trail of all financial movements.

Contributing sections: System of Record (21 reqs), Data & Information Capture, Repayment Plans, Decisions & Workflows, Agent Actions, Interfaces

Key IDs: SoR.1–21, DIC.35, RPF.31, DW.86–87, DW.62, AAD.24–25, I3PS.9

MoSCoW: ~90% Must

---

## 12. Exception Detection, Flagging & Specialist Escalation
The system must identify accounts with special circumstances (vulnerability, deceased, fraud, insolvency, disputes, breathing space), flag them prominently, restrict actions per compliance rules, and route to specialist handlers.

Contributing sections: Decisions & Workflows, Data & Information Capture, Agent Actions, User Interface, Work Allocation, Contact Channels

Key IDs: DW.45, DW.85, DIC.16/22/26, AAD.18, UI.21, WAM.20/24, CC.21/31–34, DW.51

MoSCoW: ~85% Must

---

## 13. Agent Action Logging & Case History Visibility
The system must record all agent actions, outcomes, and notes in structured format; maintain complete, searchable case history; and expose it in the UI and back to host systems for audit purposes.

Contributing sections: Agent Actions & Dispositions, User Access & Admin, Data & Information Capture, Decisions & Workflows, UI

Key IDs: AAD.5/7/9/10, UAAF.13, DIC.3/25, DW.13, UI.3/13/16, DW.69

MoSCoW: 100% Must

---

## 14. Self-Service Portal & API-First Integration
The current design baseline covers the DCMS backend integration boundary for DWP's strategic customer portal/app: I&E capture, payment events, arrangement setup requests, contact-detail updates, and engagement signals are accepted through secured APIs with audit, workflow, and domain handoff. The customer-facing portal/app UI itself remains outside DCMS unless scope is reopened.

Contributing sections: Interfaces to 3rd Party Systems, Data & Information Capture, Repayment Plans, Contact Channels, I&E Capture

Key IDs: I3PS.2/3/5/11, DIC.2/7/18/21, RPF.3, CC.2/22, IEC.2, DW.21

MoSCoW: ~90% Must

---

## 15. Dynamic, Role-Tailored UI & Agent Scripting
The system must provide configurable, role-specific screens with context-aware pop-ups (screen pops), agent scripting, ID&V prompts, mandatory field validation per activity type, and real-time multi-case handling — all configurable without IT.

Contributing sections: User Interface Screens (30 reqs), Decisions & Workflows, User Access, Agent Actions, Data & Information Capture

Key IDs: UI.1–30, DW.20, DW.59, UAAF.12, AAD.20, DIC.23

MoSCoW: ~91% Must

---

## 16. Controlled Change Management with Simulation & Rollback
The system must support staged rollout of changes (strategies, rules, code) with peer review, automated regression testing, time-travel simulation, and rapid rollback — across dev/test/production environments.

Contributing sections: Change Processes (11 reqs), Decisions & Workflows, System Development & Roadmap

Key IDs: CP.1–11, DW.7 (time-travel testing), DW.9 (version control), DW.39/41–42, SD.5

MoSCoW: 100% Must

---

## 17. Data Migration from Legacy Systems with Verification & Suppression
The system must migrate accounts from DM6/CRU via bulk or phased approach, verify migrated data, suppress duplicate action triggers, allow fallback to legacy systems, and track migration status and failures.

Contributing sections: Interfaces to 3rd Party Systems, Migration Requirements (15 reqs), Data & Information Capture, Decisions & Workflows

Key IDs: I3PS.12–13, MR.1–15, MR.14 (duplicate suppression), DIC.26–27, DW.31

MoSCoW: Mostly Must — approach flexibility noted

---

## 18. Vulnerability, Accessibility & Compliance Handling
The system must capture and honour customer accessibility needs and vulnerability markers (mental health, financial hardship, domestic violence), apply contact restrictions, enforce breathing space/insolvency/deceased protocols, and support GDPR anonymisation, archival, and purge.

Contributing sections: Data & Information Capture, Decisions & Workflows, Agent Actions, User Access, UI, Contact Channels

Key IDs: DIC.6/16, DW.45/51, AAD.19, UAAF.20, DW.25, UI.21, CC.24

MoSCoW: ~80% Must — GDPR elements Should

---

## 19. System Roadmap, Upgrades & Industry Currency
The system must maintain a transparent published roadmap (short/medium/long-term), incorporate emerging technologies (AI, ML, cloud-native), and deliver upgrades with minimal business disruption.

Contributing sections: System Development & Roadmap (7 reqs), Change Processes

Key IDs: SD.1–7, CP.7

MoSCoW: Mixed — SD.1/2/5/7 Must; SD.3/4/6 Should

---

## 20. Competing Parallel Process Management
The system must handle scenarios where a single customer or account is simultaneously subject to multiple treatments (e.g., legal + dialler + arrangement) without workflow conflicts or duplicate contact events.

Contributing sections: Decisions & Workflows

Key IDs: DW.6, DW.14, DW.36–37, DW.50, DW.52

MoSCoW: 100% Must

---

## Summary Table

| # | Major Behavioral Requirement | Primary Sections | Req. Count | Must % | Criticality |
|---|---|---|---|---|---|
| 1 | Relationship Management | CAS, DW, DIC | ~10 | 100% | High |
| 2 | Data Capture & Integration Pipeline | DIC, SoR, I3PS | ~47 | 90% | Critical |
| 3 | Workflow Automation & Override | DW, WAM, AAD | ~98 | 95% | Critical |
| 4 | Repayment Arrangement Lifecycle | RPF, DW, DIC | ~46 | 95% | Critical |
| 5 | Multi-Channel Communication | CC, DW, DIC | ~56 | 88% | Critical |
| 6 | I&E Affordability Assessment | IEC, RPF, DW | ~32 | 88% | High |
| 7 | RBAC & Audit | UAAF, AAD, WAM | ~56 | 96% | Critical |
| 8 | Work Queue Distribution | WAM, DW, DIC | ~37 | 93% | Critical |
| 9 | Decisioning & Strategy Optimisation | DW, A, BSF | ~90 | Mixed | High |
| 10 | 3rd Party Placement & Reconciliation | 3PM, AAD, SoR | ~30 | 45% Should | Medium |
| 11 | Financial Accounting & Reconciliation | SoR, DIC, RPF | ~40 | 90% | Critical |
| 12 | Exception Detection & Escalation | DW, DIC, AAD | ~35 | 85% | Critical |
| 13 | Agent Action Logging & History | AAD, UAAF, UI | ~18 | 100% | High |
| 14 | Self-Service & API Integration | I3PS, DIC, CC | ~40 | 90% | Critical |
| 15 | Dynamic UI & Agent Scripting | UI, DW, UAAF | ~47 | 91% | High |
| 16 | Change Management & Simulation | CP, DW, SD | ~22 | 100% | High |
| 17 | Legacy Data Migration | I3PS, MR | ~17 | Mostly Must | High |
| 18 | Vulnerability & Compliance | DIC, DW, CC | ~35 | 80% | Critical |
| 19 | Roadmap & Upgrades | SD, CP | ~8 | 75% | Medium |
| 20 | Parallel Process Management | DW | ~5 | 100% | High |

---

## Four Foundational Behaviors (Architecture-Level)

Four of these behavioral requirements underpin everything else and would be considered architectural cornerstones:

- **Data Integration Pipeline** — everything depends on clean, validated, consistently-exposed data
- **Real-Time Workflow Engine** — the central orchestrator for all automated collection activity
- **Multi-Channel Communication Orchestration** — the primary mechanism of customer engagement
- **Financial Accounting Core** — the system of record for all money movements
