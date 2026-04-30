# Functional Requirements Use Cases
**Source Basis:** [Functional-Requirements-Consolidated.md](/c:/Users/tbi/OneDrive%20-%20Netcompany/Documents/Projects/GTM/DWP/DWP-Debt-Collection/Tender%20requirements%20docs/Functional-Requirements-Consolidated.md)

---

## Purpose
This document translates the consolidated functional requirements into representative user interaction use cases for the envisioned debt collection system.

The goals are to:
- show how the platform would be used in practice by agents, supervisors, admins, analysts, finance users, migration users, and customers
- provide BPMN-style step flows for the first 12 core use cases
- expand the use-case set so the catalogue collectively covers the full functional scope, including MI, analytics, third-party management, interfaces, system of record, change processes, roadmap, and migration
- identify the key `Must have` functional requirements embodied in each use case

---

## Coverage Summary
The use cases in this document collectively cover these functional domains:

| Functional Domain | Covered By Use Cases |
|---|---|
| Customer & Account Structure | UC-01, UC-09, UC-14, UC-21, UC-24 |
| Data & Information Capture | UC-02, UC-03, UC-05, UC-09, UC-15, UC-21, UC-24 |
| Decisions & Workflows | UC-01, UC-03, UC-04, UC-06, UC-07, UC-09, UC-11, UC-14, UC-18, UC-19 |
| Repayment Plan Functionality | UC-04, UC-05, UC-08, UC-09, UC-15 |
| User Access & Admin Functions | UC-07, UC-11, UC-13, UC-14, UC-20, UC-22 |
| MI & Reporting | UC-16 |
| Analytics | UC-17 |
| User Interface Screens | UC-01, UC-02, UC-03, UC-04, UC-07, UC-09, UC-14, UC-15 |
| Work Allocation & Monitoring | UC-07, UC-08, UC-14 |
| Agent Actions & Dispositions | UC-02, UC-03, UC-04, UC-06, UC-08, UC-09, UC-15 |
| 3rd Party Management | UC-10, UC-18 |
| Income & Expenditure Capture | UC-05, UC-15 |
| Bureau & Scorecard Feeds | UC-17 |
| Contact Channels | UC-02, UC-04, UC-05, UC-06, UC-12, UC-19 |
| Interfaces to 3rd Party Systems | UC-02, UC-05, UC-06, UC-08, UC-10, UC-12, UC-18, UC-21, UC-24 |
| System of Record | UC-08, UC-15, UC-18, UC-20, UC-21 |
| Change Processes | UC-11, UC-22 |
| System Development & Roadmap | UC-23 |
| MP Requirements | UC-12, UC-24 |

---

## Core BPMN-Style Use Cases

## UC-01 Agent Opens A Customer Case And Reviews Linked Debts
**Primary actors:** Collections Agent, Debt Collection System

**Trigger:** An agent selects a customer or account from search, queue, or inbound screen-pop.

**Outcome:** The agent sees a unified case view with linked debts, current statuses, balances, next actions, and alerts.

### BPMN-Style Step Flow
1. **Start event:** Agent receives or selects a case.
2. **User task:** Agent opens the customer record.
3. **Service task:** System retrieves customer, account, group, joint-party, and securities relationships.
4. **Service task:** System retrieves current balances, arrears position, strategy, next work date, payment history, comments, and flags.
5. **Exclusive gateway:** Are there linked customers, linked debts, or joint liabilities?
6. **Service task:** If yes, system displays linked relationships and relevant hierarchy/history.
7. **Service task:** System displays current workflow position and recent actions.
8. **Exclusive gateway:** Are there specialist alerts such as vulnerability, insolvency, deceased, or dispute?
9. **Service task:** If yes, system displays banners, flash notes, and treatment restrictions.
10. **User task:** Agent reviews the case and chooses the next action.
11. **End event:** Case is ready for action with full context visible.

**Must-have FRs incorporated:**
- `CAS.1`
- `CAS.2`
- `CAS.4`
- `CAS.6`
- `CAS.15`
- `DIC.20`
- `DW.13`
- `UI.12`
- `UI.21`

---

## UC-02 Agent Updates Customer Contact Details After Contact
**Primary actors:** Collections Agent, Customer, Debt Collection System

**Trigger:** During a call, email interaction, or other contact, the customer provides updated contact information.

**Outcome:** Contact details are updated, validated, historised, and made available for future communications.

### BPMN-Style Step Flow
1. **Start event:** Agent is interacting with a customer.
2. **User task:** Agent opens the contact details section of the case.
3. **User task:** Agent enters updated phone, email, or address details.
4. **Service task:** System validates formatting and mandatory fields.
5. **Exclusive gateway:** Is the new data valid?
6. **User task:** If no, agent corrects the data.
7. **Service task:** If yes, system saves the updated details.
8. **Service task:** System records amendment history and user audit trail.
9. **Service task:** System updates customer-level data across associated accounts where appropriate.
10. **Exclusive gateway:** Do preferences or communication rules need recalculation?
11. **Service task:** If yes, system recalculates preferred channels/times and future contact rules.
12. **End event:** Updated details are available for future operations and communications.

**Must-have FRs incorporated:**
- `DIC.1`
- `DIC.2`
- `DIC.20`
- `DIC.24`
- `DIC.25`
- `CC.24`
- `AAD.11`

---

## UC-03 Agent Records A Vulnerability And The System Changes Treatment
**Primary actors:** Collections Agent, Supervisor or Specialist Team, Debt Collection System

**Trigger:** The agent identifies that the customer is vulnerable or has an exception condition requiring specialist treatment.

**Outcome:** The vulnerability is recorded, visible, and drives restricted treatment and routing.

### BPMN-Style Step Flow
1. **Start event:** Agent identifies vulnerability during interaction or case review.
2. **User task:** Agent selects a vulnerability or exception action.
3. **User task:** Agent records vulnerability type and supporting information.
4. **Service task:** System saves the vulnerability marker and audit trail.
5. **Service task:** System updates flags and exception markers on the case.
6. **Exclusive gateway:** Does the vulnerability require restricted treatment or specialist handling?
7. **Service task:** If yes, system suppresses inappropriate actions and updates treatment rules.
8. **Service task:** System reroutes or allocates the case to the appropriate specialist queue or team.
9. **Service task:** System displays prominent banners and flash notes to future users.
10. **User task:** Agent or supervisor confirms next action is compliant with rules.
11. **End event:** The case is safely managed under the correct treatment path.

**Must-have FRs incorporated:**
- `DIC.16`
- `DIC.26`
- `DW.35`
- `DW.45`
- `WAM.20`
- `AAD.18`
- `AAD.20`
- `UI.21`

---

## UC-04 Agent Sets Up A Repayment Arrangement During Contact
**Primary actors:** Collections Agent, Customer, Debt Collection System

**Trigger:** A customer agrees to repay and the agent sets up a plan during the interaction.

**Outcome:** A repayment plan is created, validated, monitored, and confirmed.

### BPMN-Style Step Flow
1. **Start event:** Customer agrees to discuss repayment.
2. **User task:** Agent opens repayment arrangement setup.
3. **User task:** Agent enters arrangement scope, amount, frequency, start date, and payment method.
4. **Service task:** System calculates affordability and arrangement suitability.
5. **Exclusive gateway:** Is the arrangement valid against business rules and tolerances?
6. **Service task:** If no, system presents warnings or authority escalation prompts.
7. **User task:** Agent amends proposal or escalates if needed.
8. **Service task:** If yes, system creates the arrangement at customer or account level.
9. **Service task:** System sets monitoring events, review dates, tolerances, and broken-plan logic.
10. **Exclusive gateway:** Is payment method integration required now, such as DD or phone payment?
11. **Service task:** If yes, system invokes relevant payment validation/integration.
12. **Service task:** System triggers confirmation communications and records the arrangement origin.
13. **End event:** Arrangement is active and queued for monitoring.

**Must-have FRs incorporated:**
- `RPF.1`
- `RPF.3`
- `RPF.5`
- `RPF.6`
- `RPF.7`
- `RPF.8`
- `RPF.9`
- `RPF.19`
- `RPF.20`
- `RPF.22`
- `DW.33`

---

## UC-05 Customer Completes Income & Expenditure Through Self-Service
**Primary actors:** Customer, Debt Collection System, Strategic Portal or App

**Trigger:** The customer accesses self-service to provide affordability information.

**Outcome:** I&E data is captured, validated, and used to inform treatment and repayment options.

### BPMN-Style Step Flow
1. **Start event:** Customer logs into the portal or app.
2. **User task:** Customer selects Income & Expenditure.
3. **User task:** Customer enters income, expenses, and related data.
4. **Service task:** System validates the data against configured rules and available account or reference data.
5. **Exclusive gateway:** Is the submission complete and valid?
6. **User task:** If no, customer corrects or saves as incomplete for later.
7. **Service task:** If yes, system stores the I&E record.
8. **Service task:** System records the self-service action in near real time.
9. **Service task:** System evaluates repayment options and collections strategy implications.
10. **Exclusive gateway:** Should repayment options be offered immediately?
11. **Service task:** If yes, system generates and displays available repayment options.
12. **End event:** I&E is captured and available for both automated and agent-led use.

**Must-have FRs incorporated:**
- `IEC.1`
- `IEC.2`
- `IEC.3`
- `IEC.4`
- `IEC.5`
- `IEC.6`
- `IEC.10`
- `IEC.11`
- `DIC.21`
- `CC.22`
- `I3PS.2`

---

## UC-06 System Automatically Sends The Right Communication Based On Strategy
**Primary actors:** Debt Collection System, Customer, Strategic Communication Components

**Trigger:** A strategy event occurs, such as a missed payment, journey step, or planned contact point.

**Outcome:** The system selects the correct communication and records the action and outcome.

### BPMN-Style Step Flow
1. **Start event:** A strategy rule, workflow event, or account trigger fires.
2. **Service task:** System evaluates customer/account status, strategy position, prior contact outcomes, and channel preferences.
3. **Exclusive gateway:** Is communication currently suppressed or excluded on the required channel?
4. **Service task:** If yes, system either selects an alternative permitted channel or suppresses the action.
5. **Service task:** System selects the best available communication template and channel.
6. **Service task:** System schedules or sends the message through the integrated channel.
7. **Service task:** System writes a communication footprint to the account and host where required.
8. **Exclusive gateway:** Does the channel return a delivery or response outcome?
9. **Service task:** If yes, system records delivery, failure, exclusion, or response status.
10. **Service task:** System updates workflow state or triggers follow-on actions if needed.
11. **End event:** Communication history and any downstream actions are updated.

**Must-have FRs incorporated:**
- `DW.10`
- `DW.27`
- `DW.33`
- `DW.34`
- `DW.43`
- `DW.46`
- `CC.2`
- `CC.6`
- `CC.9`
- `CC.18`
- `CC.20`
- `CC.21`
- `CC.23`
- `CC.24`
- `CC.27`

---

## UC-07 Supervisor Reviews An Exception Queue And Overrides Workflow
**Primary actors:** Supervisor, Debt Collection System

**Trigger:** A case enters an exception queue because it could not continue through its normal automated path.

**Outcome:** The supervisor reviews the reason and approves, reroutes, or holds the case.

### BPMN-Style Step Flow
1. **Start event:** A case fails a decision rule or reaches an exception condition.
2. **Service task:** System routes the case to an exception queue with reason and context.
3. **User task:** Supervisor opens the exception queue item.
4. **User task:** Supervisor reviews case data, strategy state, and exception reason.
5. **Exclusive gateway:** Can the case continue on the original path?
6. **Service task:** If yes, system reinstates the case to the correct strategy step.
7. **Exclusive gateway:** Should the case be rerouted, paused, or escalated?
8. **User task:** Supervisor selects override action.
9. **Service task:** System records the override, audit trail, and updated routing.
10. **Service task:** System assigns the case to the correct team, queue, or owner.
11. **End event:** The case resumes under controlled supervision.

**Must-have FRs incorporated:**
- `DW.11`
- `DW.18`
- `DW.24`
- `WAM.17`
- `WAM.24`
- `UAAF.2`
- `UAAF.21`

---

## UC-08 Team Manager Allocates Work To Specialist Queues
**Primary actors:** Team Manager, Debt Collection System

**Trigger:** New or existing work must be distributed across teams and users.

**Outcome:** Cases are allocated according to business rules, skills, authority, and specialist handling needs.

### BPMN-Style Step Flow
1. **Start event:** New work arrives or reallocation is needed.
2. **Service task:** System evaluates queue rules, segment attributes, agent availability, authority levels, and specialist markers.
3. **Exclusive gateway:** Can the work be auto-assigned under current rules?
4. **Service task:** If yes, system allocates the case automatically.
5. **User task:** If no, manager opens work allocation controls.
6. **User task:** Manager reviews queue volumes, case characteristics, and team capacity.
7. **Exclusive gateway:** Does the case require specialist ownership or restricted visibility?
8. **Service task:** If yes, system restricts access and allocates to the relevant specialist queue.
9. **User task:** Manager manually reassigns or redistributes work if needed.
10. **Service task:** System updates ownership, queue placement, and monitoring/reporting data.
11. **End event:** Work is assigned to the appropriate queue or user.

**Must-have FRs incorporated:**
- `WAM.1`
- `WAM.2`
- `WAM.9`
- `WAM.13`
- `WAM.15`
- `WAM.16`
- `WAM.20`
- `WAM.23`
- `UAAF.2`
- `UAAF.7`

---

## UC-09 Agent Handles A Joint Customer With Multiple Accounts In Different States
**Primary actors:** Collections Agent, Customer, Debt Collection System

**Trigger:** An agent works a customer linked to multiple debts with different statuses.

**Outcome:** The system supports coordinated but distinct treatment across linked debts.

### BPMN-Style Step Flow
1. **Start event:** Agent opens a customer with multiple linked accounts.
2. **Service task:** System loads all linked debts, joint parties, disputes, arrangements, and statuses.
3. **User task:** Agent reviews account-level and customer-level positions.
4. **Exclusive gateway:** Do all linked accounts follow the same treatment path?
5. **Service task:** If no, system presents separate actions and restrictions by account.
6. **User task:** Agent records actions on one or more accounts.
7. **Service task:** System preserves distinct workflow states while maintaining customer-level context.
8. **Exclusive gateway:** Is liability transfer or relationship change required?
9. **User task:** If yes, agent initiates relationship or liability change.
10. **Service task:** System updates linked relationships and case history.
11. **End event:** The linked case is managed coherently across multiple debt states.

**Must-have FRs incorporated:**
- `CAS.1`
- `CAS.3`
- `CAS.17`
- `DW.36`
- `DW.37`
- `DIC.22`
- `RPF.20`
- `UI.9`
- `UI.12`

---

## UC-10 Agent Places An Account With A Third Party And Later Recalls It
**Primary actors:** Authorised User, Third Party Agency, Debt Collection System

**Trigger:** The organisation decides to place a case externally and may later recall it.

**Outcome:** Case data is shared, tracked, and later reinstated internally if recalled.

### BPMN-Style Step Flow
1. **Start event:** Authorised user decides to place an account with a third party.
2. **User task:** User selects placement action and target third party.
3. **Service task:** System prepares data package according to configured data-sharing rules.
4. **Service task:** System transmits placement data via real-time or batch interface.
5. **Service task:** System records placement status, ownership state, and audit trail.
6. **Exclusive gateway:** Is the account later recalled?
7. **User task:** If yes, authorised user initiates recall.
8. **Service task:** System receives or confirms recall and reinstates internal ownership.
9. **Service task:** System routes the case to the most appropriate strategy or queue.
10. **Service task:** System reconciles third-party updates and history as needed.
11. **End event:** The account is either actively managed externally or correctly restored internally.

**Must-have FRs incorporated:**
- `DIC.19`
- `I3PS.3`
- `I3PS.11`
- `AAD.21`
- `DW.35`
- `DW.57`
- `SoR.11`
- `SoR.12`

---

## UC-11 Business Administrator Changes A Collections Strategy Without A Code Release
**Primary actors:** Business Administrator, Release Manager, Debt Collection System

**Trigger:** A business rule or strategy needs to be updated.

**Outcome:** A controlled, testable, reversible change is made through configuration.

### BPMN-Style Step Flow
1. **Start event:** Business identifies a need to change strategy or workflow.
2. **User task:** Business admin opens configuration tooling.
3. **User task:** Admin edits rule, workflow, segmentation logic, or treatment step.
4. **Service task:** System versions the change and records change metadata.
5. **User task:** Admin runs simulation, test cases, or bulk test accounts.
6. **Exclusive gateway:** Do test results meet expectations?
7. **User task:** If no, admin revises the change.
8. **User task:** If yes, admin submits for promotion or release.
9. **Service task:** System supports controlled release, comparison to previous versions, and deployment traceability.
10. **Exclusive gateway:** Does the release need rollback?
11. **Service task:** If yes, system restores previous release/state.
12. **End event:** Change is safely introduced or rolled back.

**Must-have FRs incorporated:**
- `DW.5`
- `DW.9`
- `DW.16`
- `DW.30`
- `DW.38`
- `DW.39`
- `DW.41`
- `DW.42`
- `CP.4`
- `CP.5`
- `CP.7`
- `CP.8`
- `CP.10`

---

## UC-12 Migration Operator Loads Legacy Debt Data And Resolves Exceptions
**Primary actors:** Migration Operator, Debt Collection System, Legacy Systems

**Trigger:** Legacy customer/account data is migrated into the target debt collection system.

**Outcome:** Records are loaded, linked, validated, and corrected where necessary without duplicate operational impact.

### BPMN-Style Step Flow
1. **Start event:** Migration batch or migration event is initiated.
2. **Service task:** System ingests migrated customer, account, and reference data.
3. **Service task:** System maps records to the target model and attempts to link them to existing customers where applicable.
4. **Service task:** System applies migration indicators, legacy identifiers, and suppression controls.
5. **Exclusive gateway:** Do any records fail validation, matching, or loading?
6. **Service task:** If yes, system routes failed records to migration exception handling.
7. **User task:** Migration operator reviews failures and corrects data or reference mappings.
8. **Service task:** Corrected records are reprocessed automatically.
9. **Service task:** System records migration progress, failures, and audit history.
10. **Exclusive gateway:** Is rollback or back-out required for some migrated records?
11. **Service task:** If yes, system transfers relevant customers/accounts back to legacy state where supported.
12. **End event:** Migrated records are safely loaded for ongoing operational management.

**Must-have FRs incorporated:**
- `I3PS.12`
- `I3PS.13`
- `SoR.18`
- `SoR.19`
- `SoR.20`
- `MR1`
- `MR7`
- `MR8`
- `MR9`
- `MR12`
- `MR13`
- `MR14`
- `MR15`

---

## Expanded Use Case Catalogue

## UC-13 Security Administrator Creates Users, Roles, And Authority Limits
**Primary actors:** Security Administrator, Debt Collection System

**Trigger:** A new user, team, or business unit needs access to the platform.

**Outcome:** Access is provisioned with role-based limits, segregation, and governed capabilities.

**Key interaction summary:** The administrator creates users, assigns roles, business units, access scopes, delegated authority limits, treatment permissions, and broadcast or system-level settings relevant to their operational area.

**Must-have FRs incorporated:**
- `UAAF.1`
- `UAAF.2`
- `UAAF.7`
- `UAAF.10`
- `UAAF.15`
- `UAAF.21`
- `UAAF.22`

---

## UC-14 Agent Works A Daily Queue With Screen Pops, Priorities, And Next-Best Action
**Primary actors:** Collections Agent, Debt Collection System

**Trigger:** Agent starts a shift and opens their queue or receives an inbound interaction.

**Outcome:** The agent works prioritised cases with context, restrictions, and recommended actions.

**Key interaction summary:** The system presents a queue ordered by rules, supports screen-pop and case context, highlights specialist flags, and shows available actions within the agent's authority while updating productivity and SLA metrics.

**Must-have FRs incorporated:**
- `WAM.1`
- `WAM.6`
- `WAM.7`
- `WAM.9`
- `WAM.15`
- `WAM.24`
- `UI.4`
- `UI.12`
- `UI.17`
- `AAD.3`
- `AAD.4`

---

## UC-15 Agent Records A Dispute, Complaint, Or Manual Financial Adjustment
**Primary actors:** Collections Agent, Specialist User, Debt Collection System

**Trigger:** A customer disputes a debt item, raises a complaint, or a manual financial correction is required.

**Outcome:** The system records the exception, enforces controls, and updates subsequent treatment.

**Key interaction summary:** The user records dispute details, complaint context, or manual financial adjustment, and the system applies the necessary RBAC, status changes, workflow restrictions, and audit history.

**Must-have FRs incorporated:**
- `DIC.22`
- `DIC.23`
- `DIC.25`
- `AAD.11`
- `AAD.12`
- `AAD.24`
- `AAD.25`
- `AAD.26`
- `DW.35`
- `SoR.13`

---

## UC-16 Operations Manager Runs MI Dashboards And Standard Reports
**Primary actors:** Operations Manager, Debt Collection System

**Trigger:** The manager needs performance, operational, compliance, or queue reporting.

**Outcome:** The manager can view dashboards and standard MI to monitor performance and risk.

**Key interaction summary:** The manager opens MI screens, views dashboards and operational reports, checks queues, outcomes, productivity, and performance indicators, and uses these outputs to make operational decisions.

**Must-have FRs incorporated:**
- `MIR.1`
- `MIR.2`
- `WAM.9`
- `SoR.11`

---

## UC-17 Risk Analyst Reviews Scorecards And Bureau-Enriched Segmentation
**Primary actors:** Risk Analyst, Debt Collection System, Bureau/Data Providers

**Trigger:** The organisation wants to improve treatment strategy using bureau or scorecard insight.

**Outcome:** Bureau data and analytic models inform segmentation and allocation.

**Key interaction summary:** The analyst reviews score outputs, enrichment feeds, model results, and scorecard-driven segmentation to improve prioritisation, communication strategy, and team allocation.

**Must-have FRs incorporated:**
- `A.1`
- `A.2`

**Important supporting non-must FRs in this area:** `BSF.1` to `BSF.15`

---

## UC-18 Third-Party Manager Monitors External Placement Performance And Reconciliation
**Primary actors:** Third-Party Manager, Debt Collection System, External Agencies

**Trigger:** Accounts are under management by third parties and operational oversight is required.

**Outcome:** The manager can review status, audit trail, payments, recalls, and performance.

**Key interaction summary:** The manager monitors accounts placed externally, tracks updates and payments, recalls records where needed, and reconciles operational and financial information between internal and third-party processes.

**Must-have FRs incorporated:**
- `I3PS.3`
- `I3PS.11`
- `AAD.21`
- `SoR.11`
- `SoR.12`

**Important supporting non-must FRs in this area:** `3PM.1` to `3PM.18`

---

## UC-19 Communications Administrator Configures Templates, Contact Rules, And Campaigns
**Primary actors:** Business Administrator, Debt Collection System

**Trigger:** The organisation needs to configure or update communication templates, throttling, suppression, or campaign logic.

**Outcome:** Communications are centrally controlled and aligned to business and legislative rules.

**Key interaction summary:** The administrator sets templates, channel limits, scheduling, suppression rules, preferred contact constraints, and multi-channel campaign settings that later drive automated or user-initiated communications.

**Must-have FRs incorporated:**
- `CC.18`
- `CC.21`
- `CC.23`
- `CC.24`
- `CC.26`
- `CC.27`
- `CC.31`
- `CC.32`
- `CC.33`
- `CC.34`
- `DW.33`

---

## UC-20 Finance Or Reconciliation User Validates Balances, Interest, And Accounting Outcomes
**Primary actors:** Finance User, Debt Collection System

**Trigger:** Finance, reconciliation, or audit activity is required.

**Outcome:** Financial states are consistent, reconcilable, and operationally trustworthy.

**Key interaction summary:** The finance user reviews transactions, balances, interest accruals, fees, exceptions, and reconciliation outputs to ensure the platform operates as a trustworthy system of record for collections.

**Must-have FRs incorporated:**
- `SoR.1`
- `SoR.2`
- `SoR.3`
- `SoR.5`
- `SoR.6`
- `SoR.10`
- `SoR.11`
- `SoR.12`
- `SoR.13`
- `SoR.17`

---

## UC-21 Interface Operator Monitors Real-Time And Batch Integrations
**Primary actors:** Integration Support User, Debt Collection System, External/Internal Systems

**Trigger:** Data exchange with portals, payment systems, document systems, host systems, or external providers must be monitored or corrected.

**Outcome:** Interfaces are reliable, auditable, and recoverable when errors occur.

**Key interaction summary:** The user monitors inbound and outbound interfaces, checks handshake results, resolves integration failures, replays or reprocesses data, and confirms data has been correctly applied within the collection platform.

**Must-have FRs incorporated:**
- `DIC.7`
- `DIC.17`
- `DIC.18`
- `I3PS.2`
- `I3PS.3`
- `I3PS.5`
- `I3PS.6`
- `I3PS.9`
- `I3PS.10`
- `I3PS.11`
- `I3PS.15`

---

## UC-22 Release Manager Promotes Tested Configuration Through Controlled Environments
**Primary actors:** Release Manager, Business Administrator, Debt Collection System

**Trigger:** A tested change is ready for controlled release.

**Outcome:** Release promotion, release comparison, automated controls, and rollback are available.

**Key interaction summary:** The release manager promotes approved changes, checks regression outcomes, compares releases, logs defects or issues where needed, and executes rollback if post-release issues arise.

**Must-have FRs incorporated:**
- `CP.4`
- `CP.5`
- `CP.6`
- `CP.7`
- `CP.8`
- `CP.9`
- `CP.10`
- `CP.11`

---

## UC-23 Product Owner Reviews Supplier Roadmap And Planned Platform Upgrades
**Primary actors:** Product Owner, Service Manager, Supplier Representative, Debt Collection System

**Trigger:** The organisation reviews platform evolution, upcoming releases, or supplier roadmap commitments.

**Outcome:** DWP gains visibility into planned upgrades and future platform direction.

**Key interaction summary:** Stakeholders review roadmap artefacts, near-term and long-term planned changes, issue resolution versus new features, and upgrade timing and impact, enabling governance over future change.

**Must-have FRs incorporated:**
- `SD.1`
- `SD.2`
- `SD.5`
- `SD.7`

---

## UC-24 Migration Analyst Reviews Historical Data And Legacy Identifiers Post-Cutover
**Primary actors:** Migration Analyst, Collections Agent, Debt Collection System

**Trigger:** After migration, a user needs to verify migrated data, legacy references, and ongoing operational status.

**Outcome:** Users can safely work migrated cases with retained history and without duplicate activity.

**Key interaction summary:** The analyst or agent checks migrated flags, previous identifiers, retained historical data, account and customer relationships, suppression of duplicate triggers, and whether migrated cases are correctly usable in live operations.

**Must-have FRs incorporated:**
- `MR3`
- `MR5`
- `MR8`
- `MR9`
- `MR10`
- `MR11`
- `MR14`
- `SoR.18`
- `SoR.19`
- `SoR.20`

---

## UC-25 Customer Makes A Payment Through IVR, Portal, Or Agent-Assisted Channel
**Primary actors:** Customer, Agent, Debt Collection System, Payment Services

**Trigger:** The customer chooses to make a payment against one or more debts.

**Outcome:** Payment is accepted, allocated correctly, reflected in balances, and can drive follow-on workflow.

**Key interaction summary:** The payment may be taken by agent, IVR, bank transfer, or integrated channel; the system records the transaction, applies allocation rules, updates balances, and may trigger confirmations or next-step treatment.

**Must-have FRs incorporated:**
- `RPF.5`
- `RPF.12`
- `RPF.26`
- `RPF.27`
- `RPF.28`
- `RPF.30`
- `RPF.31`
- `RPF.33`
- `RPF.34`
- `RPF.35`
- `RPF.36`
- `SoR.2`

---

## UC-26 Agent Or System Suspends, Reinstates, Archives, Or Purges A Case Under Rule Control
**Primary actors:** Collections Agent, Supervisor, Debt Collection System

**Trigger:** A case must be held, removed from action, archived, or purged according to rules or retention policy.

**Outcome:** The case is correctly suppressed, paused, archived, or removed from the active operational path.

**Key interaction summary:** The system or user suspends collections due to an exception, later reinstates the case when appropriate, or archives/purges non-delinquent or written-off cases under configured controls and retention rules.

**Must-have FRs incorporated:**
- `CAS.14`
- `DIC.25`
- `DW.31`
- `DW.35`
- `CC.31`
- `CC.33`
- `I3PS.15`

---

## UC-27 Agent Retrieves Supporting Documents During Case Handling
**Primary actors:** Collections Agent, Debt Collection System, Document Systems

**Trigger:** The agent needs evidence or historical documentation while handling a case.

**Outcome:** Relevant documents are available within the collections workflow.

**Key interaction summary:** The agent views inbound or historical documents, uploaded files, external scans, or third-party documentation as part of a live collections interaction without leaving the operational context.

**Must-have FRs incorporated:**
- `DIC.12`
- `CC.14`
- `CC.25`
- `I3PS.10`
- `UI.12`

---

## UC-28 Agent Uses Do-Not-Contact, Preference, And Channel Exclusion Controls During Outreach
**Primary actors:** Collections Agent, Debt Collection System

**Trigger:** The agent is preparing to contact a customer and must respect preference and exclusion rules.

**Outcome:** Contact is compliant, channel-appropriate, and correctly restricted where needed.

**Key interaction summary:** Before contacting the customer, the system checks preferred channels, preferred times, exclusions, and do-not-contact indicators, allowing only compliant action and logging the decision.

**Must-have FRs incorporated:**
- `DIC.14`
- `CC.21`
- `CC.24`
- `AAD.16`
- `DW.43`

---

## Notes On Requirement Coverage
- The first 12 use cases are the most operationally central and are intentionally written as BPMN-style flows.
- The expanded catalogue is designed to cover the remaining functional domains and specialist interactions not fully surfaced by the first 12.
- Some domains, especially `3rd Party Management`, `Bureau & Scorecard Feeds`, and parts of `System Development & Roadmap`, are more platform-capability-oriented than end-user-transaction-oriented. Their use cases therefore focus on oversight, configuration, and governance interactions.
- The migration section contains unrated `MR` requirements in the source file. They are still included here because they materially shape system behaviour during onboarding and cutover.
