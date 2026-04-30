# Functional Requirements - Consolidated
**Source:** `C8618 - FDS - Attachment 4a - Functional Requirements.xlsx`

## Workflow Diagram (Editable)
- Draw.io source: [dwp-example-workflow.drawio](./dwp-example-workflow.drawio)
- Reference image: [dwp_example_workflow.png](./dwp_example_workflow.png)

---

## Table of Contents
1. [Capabilities & Additional Info](#1-capabilities--additional-info)
2. [Key Consideration Items](#2-key-consideration-items)
3. [Customer & Account Structure](#3-customer-account-structure)
4. [Data & Information Capture](#4-data-information-capture)
5. [Decisions & Workflows](#5-decisions-workflows)
6. [Repayment Plan Functionality](#6-repayment-plan-functionality)
7. [User Access & Admin Functions](#7-user-access-admin-functions)
8. [MI & Reporting](#8-mi-reporting)
9. [Analytics](#9-analytics)
10. [User Interface Screens](#10-user-interface-screens)
11. [Work Allocation & Monitoring](#11-work-allocation-monitoring)
12. [Agent Actions & Dispositions](#12-agent-actions-dispositions)
13. [3rd Party Management](#13-3rd-party-management)
14. [Income & Expenditure Capture](#14-income-expenditure-capture)
15. [Bureau & Scorecard Feeds](#15-bureau-scorecard-feeds)
16. [Contact Channels](#16-contact-channels)
17. [Interfaces to 3rd Party Systems](#17-interfaces-to-3rd-party-systems)
18. [System of Record](#18-system-of-record)
19. [Change Processes](#19-change-processes)
20. [System Development & Roadmap](#20-system-development-roadmap)
21. [MP Requirements](#21-mp-requirements)

---

## 1. Capabilities & Additional Info

### Capability Groups

| Capability Group | Description | ITT Questions Grouping |
|---|---|---|
| 1. Customer & Account Structure | Defines how customers, accounts, and their relationships are modeled. Includes support for multiple accounts per customer, joint accounts, and hierarchical views (e.g., household or business relationships). | 2b |
| 2. Data & Information Capture | Covers capturing and storing data such as contact details, payment history, delinquency status, and other relevant customer or account attributes, includes inbound interfaces. | 2b |
| 3. Decisions & Workflows | Involves automated or manual decisioning logic (e.g., delinquency stages, treatment paths) and workflow engines to manage and guide collection actions and escalations. | 2b |
| 4. Repayment Plan Functionality | Supports creating, tracking, and managing repayment arrangements, including flexible scheduling, automated recalculations, and breach handling. | 2b |
| 5. User Access & Admin Functions | Enables secure role-based access control, user management, audit trails, and administrative configuration tools for system settings and parameters. | 2b |
| 6. MI & Reporting | Includes management information (MI) dashboards, standard reports, and ad-hoc reporting tools to support performance tracking, compliance, and decision-making. | 2a |
| 7. Analytics | Includes scoring models, predictive analytics, and segmentation tools to optimize treatment strategies, prioritize accounts, and evaluate risks. | 2a |
| 8. User Interface Screens | Refers to the user interface design, including layout, usability, and customer or case views tailored to agent roles and tasks. | 2b |
| 9. Work Allocation & Monitoring | Provides mechanisms for distributing work to collection agents (e.g., by skill, availability, priority) and tools for supervisors to monitor queues and productivity. | 2b |
| 10. Agent Actions & Dispositions | Captures all actions taken by agents (calls, emails, notes) and their outcomes (promises to pay, disputes, broken arrangements), enabling full case history. | 2b |
| 11. 3rd Party Placements | Supports placing accounts with external collection agencies or legal firms, tracking their performance, and reconciling payments or commissions. | 2b |
| 12. Income & Expenditure Capture | Allows agents or customers to input income and expenses to assess affordability, often forming the basis for setting repayment plans. | 2b |
| 13. Bureau & Scorecard Feeds | Integrates data from credit bureaus and internal/external scorecards for enhanced risk assessment and decisioning. | 2c |
| 14. Contact Channels | Supports multi-channel engagement (phone, SMS, email, chatbot, letter), including preference management and communication tracking. | 2b |
| 15. Interfaces to 3rd Party Systems | Covers API or batch interfaces with CRM, core banking, payment gateways, bureau data providers, and other enterprise systems. | 2b |
| 16. System of Record | Designates the system as the authoritative source for collections data, requiring accurate, real-time updates and robust audit logging. | 2a |
| 17. Change Processes | Facilitates controlled changes to business rules, workflows, or system parameters - typically via a governance or change request process. | 2a |
| 18. System Development & Roadmap | Refers to the approach for enhancing the system over time - e.g., through agile releases, backlog management, and roadmap planning. | 2c |
| 19. Functional | Includes performance, scalability, availability, disaster recovery, security, and compliance requirements (e.g., GDPR, PCI DSS). |  |
| 20. Technical | Encompasses the technical architecture, infrastructure, coding standards, deployment model (cloud/on-prem), and integration strategy. |  |

### MoSCoW Rating Definitions

| Rating | Definition |
|---|---|
| **Must have** | Critical to the current delivery timebox. If even one Must have requirement is not included, the delivery should be considered a failure. |
| **Should have** | Important but not necessary for the current delivery timebox. |
| **Could have** | Desirable but not necessary; typically included if time and resources permit. |
| **Won't have (this time)** | Agreed as out of scope for the current delivery timebox. |

### Supplier Response Guidance

| Response Value | Meaning |
|---|---|
| **Fully met** | Everything requested in the requirement is provided by the proposed service or solution. |
| **Partially met** | There are gaps in the proposed service or solution compared with the requirement. |
| **Not met** | The requirement is not satisfied by the proposed solution. |
| **Not applicable** | The requirement does not apply to the proposed solution. |

---

## 2. Key Consideration Items

| Item | Description | Client Requirement |
|---|---|---|
| 1 | Hosted, On-Premise or Managed Service Solution | Ideally SaaS (potential for full managed service) - cloud native where possible. Fully Managed. |
| 2 | Current arrears volumes and future forecast arrears volumes (over 5 years) | Total Portfolio e.g Trasanctional Account volume = x and Loans = xk, xk transactional and x loans in collections<br>Future volumes currently being projected. Volumes likely to increase with extension of products. |
| 3 | Total arrears balance | TBC. Currently, high proportion of accounts that cure in-month. |
| 4 | Expected budget for new System | Budget is not the primary consideration. |
| 5 | Level of functionality sophistication? E.g Simple, Medium, Advanced. | Advanced sophistication with high degrees of integration capability. Would prefer simple to use business configuration logic |

---

## 3. Customer & Account Structure

**Capability Group Description:** Defines how customers, accounts, and their relationships are modeled. Includes support for multiple accounts per customer, joint accounts, and hierarchical views (e.g., household or business relationships).

**Requirement Count:** 17

| Req. ID | Requirement | MoSCoW Rating | Context | ITT Question Grouping |
|---|---|---|---|---|
| CAS.1 | The supplier shall ensure that the system shall have the collections ability to support multi-level view of customers, accounts, groups, and securities e.g. corporate entities, individual customer, joint customer and third parties. | Must have |  | 2b |
| CAS.2 | The supplier shall ensure that the system shall have the ability to automatically link relationships between customer, group, account and securities. This may include but is not limited to corporate entities, individual customer, joint customer and third parties. | Must have |  | 2b |
| CAS.3 | The supplier shall ensure that the system shall have the ability to create, modify and remove relationships. This may include but is not limited to joint debt and splitting joint debts when the relationship changes. | Must have |  | 2b |
| CAS.4 | The supplier shall ensure that the system retains the history of a Household relationship. This may include but is not limited to when a Household relationship was started and ended. The history should be available to view by an agent. | Must have |  | 2b |
| CAS.5 | The supplier shall ensure that the system shall have the ability to ingest a unique external reference by which an account is known to the client and customer, an example of this may be the account number. | Must have |  | 2b |
| CAS.6 | The supplier shall ensure that the system shall have the ability to display all details relevant to the account and customer including but not limited to current balance, arrears/delinquent balance, interest balance, user defined balance, days past due, cycles (past due for cards), interest rate, date into collections, date opened, current strategy, next work date, number of promises made, kept, broken, current status, hot comments, last payment date and amount. | Must have |  | 2b |
| CAS.7 | The supplier shall ensure that the system shall have the ability to support organisational structure within the system. Business rules throughout the system need to account for this structure in terms of inclusion/exclusion. | Must have | This relates to the ability to set up teams and structures based on RBAC using business rules and restricting access across geographic locations where appropriate and necessary. | 2b |
| CAS.8 | The supplier shall ensure that the system shall have the ability to accommodate rules based decisioning at customer or account level. | Must have | This relates to segmentation and having the ability to manage simple and complex rules at customer and account level separately for segmentation for automatic or manual actions. Links in with requirements on the Decision and Workflows tab. | 2b |
| CAS.9 | The supplier shall ensure that the system shall have a verification process for linked customers manually assigned to avoid challenges or errors. | Must have | Once a customer is linked manually, the ability to verify through worklist/queue the validly of the linkage through a approval hierarchy, to remove errors or challenges that could arise due to incorrect manual data entry. | 2b |
| CAS.10 | The supplier shall ensure that the system shall have linked customer and account suggestion/application through rule-based parameterisation. | Must have |  | 2b |
| CAS.11 | The supplier shall ensure that the system shall have the ability to segment and align strategies at different levels based on configurable rules, examples may include but are not limited to customer level contact strategy and account level. | Must have |  | 2b |
| CAS.12 | The supplier shall ensure that the system shall have the ability to classify customers, groups, and accounts appropriately based on all available data and characteristics. | Must have |  | 2b |
| CAS.13 | The supplier could ensure that the system allows a view across all customer accounts always regardless of delinquency status for a full holistic view. | Could have | All accounts linked to customer are viewable. | 2b |
| CAS.14 | The supplier shall ensure that the system shall have the functionality to support archive and purge activities for non-delinquent or written off accounts. | Must have |  | 2b |
| CAS.15 | The supplier shall ensure that the system shall have the ability to group and manage accounts together. | Must have |  | 2b |
| CAS.16 | The supplier shall ensure that the system shall have the ability to add and remove accounts for customer collections. | Must have |  | 2b |
| CAS.17 | The supplier shall ensure that the system shall have the ability to transfer debt liability between customers, including scenarios such as joint debt apportionment to a single customer, transfer to an executor of an estate, or other reasons for liability change. | Must have |  | 2b |

---

## 4. Data & Information Capture

**Capability Group Description:** Covers capturing and storing data such as contact details, payment history, delinquency status, and other relevant customer or account attributes, including inbound interfaces.

**Requirement Count:** 37

| Req. ID | Requirement | MoSCoW Rating | Context | ITT Question Grouping |
|---|---|---|---|---|
| DIC.1 | The supplier shall ensure that the system shall have the ability to load and amend customer and account data automatically and manually, and hold history of data loaded and amended. | Must have |  | 2b |
| DIC.2 | The supplier shall ensure that the system shall allow agents and customers to be able to update contact details held on the system (including, but not limited to, name, address, phone number, email address) manually and via API integration with self service. | Must have |  | 2b |
| DIC.3 | The supplier shall ensure that the system shall have the ability to hold information of customer contact history between agents/system and customers. | Must have |  | 2b |
| DIC.4 | The supplier should ensure that the system enables admin business user ability to extend data model and tables to capture additional information which may include but is not limited to data types, product category, literal data (cycles delinquent, billing date), add custom fields/attributes. | Should have | The data tables used for collections, can be extended through configuration to enable capture of more data as defined by the system administrators. | 2b |
| DIC.5 | The supplier shall ensure that the system shall have the ability to capture data used for, but not limited to, the following: to drive customer and account statuses, deduction rates, re-categorise debt. | Must have | This could be manual or automatic capture of statuses. | 2b |
| DIC.6 | The supplier shall ensure that the system shall have functionality to capture details of customers' circumstances. | Must have | This can relate to circumstances such as accessibility needs, Working or Not etc, based on a drop down list defined by the business. | 2b |
| DIC.7 | The supplier shall ensure that the system shall have the ability to capture external data through interfaces or integration including, but not limited to, API's and batch files. | Must have |  | 2b |
| DIC.8 | The supplier shall ensure that the system shall have availability of data in database to utilise for UI Screens, reporting and communication channels including self-service capability. | Must have | The data available in the collection system can be utilised within the display of UI screens where appropriate, available for reporting and use within the communication channels used e.g. SMS, Letters, email etc. | 2b |
| DIC.9 | The supplier shall ensure that the system shall have the ability to build, customise and utilise scoring rules and calculations within the system. | Must have |  | 2b |
| DIC.10 | The supplier shall ensure that the system shall have the ability to retain key data values used to populate correspondence and sms including, but not limited to, regulatory notices such as disclosure notices, letters, sms and emails. | Must have |  | 2b |
| DIC.11 | The supplier shall ensure that the system shall have the ability to store multiple I&Es i.e. if there's an recent one, it should be able to edit and amend it. This could be done manually or automatically. | Must have |  | 2b |
| DIC.12 | The supplier shall ensure that the system shall have the ability to interface or integrate with external document scanning systems to quickly call up customer documents. | Must have |  | 2b |
| DIC.13 | The supplier shall ensure that the system shall have the functionality to capture reason for arrears and the time they first went into that position, an example of this may be a drop down list of reasons within a user activity. | Must have |  | 2b |
| DIC.14 | The supplier shall ensure that the system shall have the ability to capture the time window which is best suited for contacting each customer, taking regard of their specific circumstances and commitments (including work commitments). | Must have |  | 2b |
| DIC.15 | The supplier shall ensure that the system shall capture all communication channels going out, the system shall connect to the mail management system. | Must have |  | 2b |
| DIC.16 | The supplier shall ensure that the system shall have the ability to capture any type of customer vulnerability from a configurable drop down list. | Must have | The customer vulnerability list would be configurable based on data provided by business providing appropriate vulnerabilities to record/capture. | 2b |
| DIC.17 | The supplier shall ensure that the system shall have a feedback loop from external sources this may include, but is not limited to, a record of any details of email or sms which have been issued by fulfilment teams. | Must have |  | 2b |
| DIC.18 | The supplier shall ensure that the system shall have a feedback loop/hand shake of collections driven activity including, but not limited to, collections memos and arrangements that have been integrated using API or batch files. | Must have | As discussed in workshop, this is the acknowledgement from the API/Interface files to show the success or failure of processing data where integration handshakes are needed. | 2b |
| DIC.19 | The supplier shall ensure that the system shall enable the business system administrator and/or internal capability to configure what data is to be sent to a third party, and the system is to facilitate the transfer of that data, and record any responses or data received from the third party. | Must have |  | 2b |
| DIC.20 | The supplier shall ensure that the system shall capture customer information at a customer level (Individual or Joint) and for that to reflect across all associated accounts, including, but not limited to, contact details such as address, phone or authorised third party information. | Must have | This allows the ability to see specific defined data at account level, which is reflective on customer level. Business configurable. | 2b |
| DIC.21 | The supplier shall ensure that the system shall automatically record any self-serve actions taken by a customer without the need for manual intervention. These should be noted as self serve actions and visible in near real time. | Must have | This allows the automated updating of data from self service through integration, and data is categorised as self Service Actions or categorised based on low level business requirements. | 2b |
| DIC.22 | The supplier shall ensure that the system shall record a dispute against an account including dispute reason, dispute instigator, dispute source, dispute date and dispute resolution. | Must have |  | 2b |
| DIC.23 | The supplier shall ensure that the system shall prompt users to capture specific information based on a users activity. An example of this may be, but is not limited to: Suspense activity requires a reason for suspension of collections. | Must have | The user activity can prompt the adviser to complete additional data that is mandatory for the activity that they have selected. This includes, but is not limited to: if Fraud detected then the user clicks, for example, on a Fraud activity, this results in them having to complete a set of mandatory fields without completion of these they are unable to progress further. | 2b |
| DIC.24 | The supplier shall ensure that the system shall enforce data formatting and validation rules for different data types. These include, but are not limited to, contact phone numbers, emails, address and account numbers. | Must have |  | 2b |
| DIC.25 | The supplier shall ensure that the system shall have the ability to record, amend, delete and view key data items, dates, flags, markers, journey history, Agent notes/comments as governed by RBAC and DWP's Data Retention policies. | Must have |  | 2b |
| DIC.26 | The supplier shall ensure that the system shall have the ability to automatically and manually update, add and remove identification flags when a customer situation changes, examples of this include, but are not limited to, imprisonment, insolvency and breathing space. | Must have |  | 2b |
| DIC.27 | The supplier shall ensure that the system shall have the ability to identify and store key data i.e. counters which include, but are not limited to, times in collections, Dates, days in arrears, date communications issued. | Must have |  | 2b |
| DIC.28 | The supplier shall ensure that the system shall have the ability to record third party information: Authority to Deal (Solicitor, Power of Attorney, Any person acting on their behalf), DMC (Debt Management Company), DCA (Debt Collection Agency), Debt Sale and Employer Details. | Must have |  | 2b |
| DIC.29 | The supplier shall ensure that the system shall have the ability to store multiple email addresses and send comms to all available addresses using DWP notifications and customer contact preferences. | Must have |  | 2b |
| DIC.30 | The supplier shall ensure that the system shall have the ability to see if an email was successfully delivered via DWP notification. | Must have |  | 2b |
| DIC.31 | The supplier shall ensure that the system shall capture and record any information relating to monies owed to a customer or their representatives for the purposes of offsetting against existing debts. | Must have |  | 2b |
| DIC.32 | The supplier shall ensure that the system shall have the ability to record and recognise payments in a pending state so as not to not trigger further recovery action. | Must have |  | 2b |
| DIC.33 | The supplier shall ensure that the system shall have the ability to record and store relevant data for write off including, but not limited to, reason, date, and value. | Must have |  | 2b |
| DIC.34 | The supplier shall ensure that the system shall have the ability to receive, account for and allocate funds from bulk payments, for example, but not limited to, payments from large employers. | Must have |  | 2b |
| DIC.35 | The supplier shall ensure that the system shall have transaction level financial data available which details, but is not limited to, source of funds, general ledger codes, type of funds - BACS/DD/Credit card, and date of transaction. | Must have | The financial transaction interface to the COTS solution to provide all relevant detail of financial transaction being posted to the accounts, to ensure audit traceability. Actual data attributes on the interface to be fully provided in detailed design. Examples of data types high level provided in requirement. | 2b |
| DIC.36 | The supplier shall ensure that the system shall have the ability to store more than 1 mobile number for a customer or account. | Must have |  | 2b |
| DIC.37 | The supplier shall ensure that the system shall have the ability to store and capture via configurable drop down lists, reference data defined by admin business users. | Must have | The drop down lists would be configurable and amendable by the admins user. | 2b |

---

## 5. Decisions & Workflows

**Capability Group Description:** Involves automated or manual decisioning logic (e.g., delinquency stages, treatment paths) and workflow engines to manage and guide collection actions and escalations.

**Requirement Count:** 88

| Req. ID | Requirement | MoSCoW Rating | Context | ITT Question Grouping |
|---|---|---|---|---|
| DW.1 | The supplier shall ensure that the system shall have the ability to support a significant level of workflow and decision automation; manual tasks can be limited to those that need human interaction. | Must have |  | 2b |
| DW.2 | The supplier shall ensure that the system shall have champion challenger capability implemented to drive continuous improvement. | Must have | This is the systematic approach to comparing the performance of two or more strategies/collection processes. | 2b |
| DW.3 | The supplier shall ensure that the system shall have the ability to support advanced decisions based on complex account and customer criteria. | Must have |  | 2b |
| DW.4 | The supplier shall ensure that the system shall have the ability to create pre-defined or ad-hoc tests that can perform one or more outcome based on results. | Must have |  | 2b |
| DW.5 | The supplier shall ensure that the system shall allow users to have the option to amend strategies (with appropriate controls); via configuration toolset. GUI drag and drop workflow is a positive. | Must have |  | 2b |
| DW.6 | The supplier shall ensure that the system shall support multiple workflows and processes simultaneously including, but not limited to, legal processes while in parallel contacting the customer. | Must have |  | 2b |
| DW.7 | The supplier shall ensure that the system shall  have the ability to provide support for workflow and decision testing -  time travel support, ability to model and predict the impact of a change. | Must have |  | 2b |
| DW.8 | The supplier shall ensure that the system shall have the ability to allow the use of batch and real time decisions in the most effective way, including but not limited to real time for data that could update intra-day and batch for high volume decisions. | Must have |  | 2b |
| DW.9 | The supplier shall ensure that all business rules, strategies, workflows and other major components of the system to have a version in order to track development. | Must have |  | 2b |
| DW.10 | The supplier shall ensure that the system shall have a range of actions available to automatically update data, re-segment, classify customer accounts and trigger communication channels. | Must have |  | 2b |
| DW.11 | The supplier shall ensure that the system shall have an exception processing ability for the supervisor/manager to override workflow or strategy move i.e. route the account to a different step than the one prescribed by the strategy or workflow. | Must have |  | 2b |
| DW.12 | The supplier shall ensure that the system shall have the ability to define and change (for restricted roles) a workflow to dynamically route and segment accounts based on statuses and characteristics. | Must have |  | 2b |
| DW.13 | The supplier shall ensure that the system shall have the ability for a user to see on an account level and customer level the previous workflows, full collections lifecycle and account journey. | Must have |  | 2b |
| DW.14 | The supplier shall ensure that the system shall have the ability to run parallel processes, including but not limited to sending letters whilst an account is on dialler strategy. | Must have | This allows the sending of letters manually while the account may be in a dialler worklist to be called on. | 2b |
| DW.15 | The supplier shall ensure that the system shall have the ability to move many customers or accounts to a single point (bulk move). This should include, but not be limited to, being able to move accounts forward and backwards in a process. | Must have |  | 2b |
| DW.16 | The supplier shall ensure that the system shall have the ability to create and amend workflow and decisioning on a frequent basis within business admin controls. | Must have |  | 2b |
| DW.17 | The supplier shall ensure that the system shall have the ability to send accounts to a sub process and return them to the same point of the main process that they left. | Must have |  | 2b |
| DW.18 | The supplier shall ensure that the system shall have the ability to eliminate the possibility of accounts being lost within workflows (i.e. blackholes). | Must have |  | 2b |
| DW.19 | The supplier shall ensure that the system shall have the ability to recognise environment changes and change the workflow accordingly, including but not limited to changes for daylight saving time. | Must have |  | 2b |
| DW.20 | The supplier shall ensure that the system shall present each Customer or Account to the agent with the correct authority level and correct cases for that business area. | Must have |  | 2b |
| DW.21 | The supplier shall ensure that the system shall have the ability to integrate the payment processing solution with the collections system or linked with a separate login, including but not limited to World pay integration. | Must have |  | 2b |
| DW.22 | The supplier shall ensure that the system shall have automated workflow based on I&E or DCA repayment to offer submissions either through a portal or batch process. | Must have |  | 2b |
| DW.23 | The supplier should ensure that the system shall have the ability to define the priority of how debts are to be repaid. This means that each payment must relate to a debt, and where that debt is overpaid, the overpaid funds are not automatically re-allocated to the others where applicable. | Should have | DWP Payment Allocation build will prioritise and provide the account a payment is required to be allocated to. The COTS solution will allocate payment to the appropriate account based on interface file received. Hence this is Should have. | 2b |
| DW.24 | The supplier shall ensure that the system shall have standard exception reporting out of the box for customer and accounts not in an active strategy. | Must have |  | 2b |
| DW.25 | The supplier should ensure that the system shall have pre-configured or suggested system in-built strategies to cater for core topics including, but not limited to Deceased, Insolvency, Hardship, Persistent Debt. | Should have |  | 2b |
| DW.26 | The supplier should ensure that the system shall have the ability to support non-delinquent customers/accounts and be able to cater for pre-arrears (delinquent) strategies. | Should have |  | 2b |
| DW.27 | The supplier shall ensure that the system shall have the ability for automatic generation of Disclosure notification at configurable points in the journey, including but not limited to pre DCA. | Must have | This is relating to Legal notice that are required to be sent to customers in key processes example used is pre DCA process. | 2b |
| DW.28 | The supplier shall ensure that the system shall have the ability to identify and refund overpayments within the collections solution based on user role profile. | Must have |  | 2b |
| DW.29 | The supplier shall ensure that the system shall have the ability to use within business rules historic actions and outcomes on either the account or customer to inform the next action. | Must have |  | 2b |
| DW.30 | The supplier shall ensure that the system shall have the ability to create new collections workflows and make changes to existing collections workflows (sequence of actions). | Must have |  | 2b |
| DW.31 | The supplier shall ensure that the system shall have the ability to remove groups of customers or accounts from contact lists or queues to be actioned (in some instances without changing the current status) where there has been a decision to not contact them and transfer them to another queue for alternative handling (by postcode) or a request from a client to place a specific set of accounts on hold. | Must have |  | 2b |
| DW.32 | The supplier shall ensure that the system shall use business rules or scorecards to segment accounts and assign to strategies, individual agents or queues for teams to work. | Must have |  | 2b |
| DW.33 | The supplier shall ensure that the system shall have the ability to send automated correspondence to customers based on an event (examples include but are not limited to, based on a set of defined business rules, broken payment arrangements, payment reminders, natural disaster communication) via their channel of choice, or the most appropriate. | Must have |  | 2b |
| DW.34 | The supplier shall ensure that the system shall have the ability to take alternative actions on accounts if recommended actions are unsuccessful. For example, if there is no response to attempted contact via a particular channel, contact can be sent via another channel. | Must have |  | 2b |
| DW.35 | The supplier shall ensure that the system shall have the ability to remove an account from collections ownership/workflows when certain scenarios are recorded and include in the appropriate ownership/workflow. | Must have |  | 2b |
| DW.36 | The supplier shall ensure that the system shall have the ability to interact and manage multiple joint accounts at a customer level or at an account level (examples may be, but are not limited to, both parties to the joint account want to make payments, one customer of a joint account is disputing a transaction but the other party is making payments) | Must have | Example: Joint customer has multiple accounts: one account is disputed and the other has no issues. Customer pays the account that is fine and not the disputed account. Collections on the accounts would be managed separately; one would be disputed and the other paid. | 2b |
| DW.37 | The supplier shall ensure that the system shall have the ability to manage multiple accounts for a single customer, where those accounts are in multiple statuses or attributes (an example may be, but is not limited to, customers who are in financial hardship and also affected by a suspension of account). | Must have |  | 2b |
| DW.38 | The supplier shall ensure that there is a visual strategy in an easy to follow format including process, business rules and data elements in the decision engine, for non-technical business users. | Must have |  | 2b |
| DW.39 | The supplier shall ensure that the system shall have the ability to test changes to the strategy and business rules, including champion/challenger, using bulk batches of test accounts. | Must have |  | 2b |
| DW.40 | The supplier shall ensure that the system shall have the ability to reuse sets of rules, components, functionality to reduce effort and improve maintenance. | Must have |  | 2b |
| DW.41 | The supplier shall ensure that the system shall simulate the impact of the changes before they are implemented. This includes SQL script changes, strategy changes and scorecard related changes. | Must have |  | 2b |
| DW.42 | The supplier shall ensure that the system shall trace an outcome back through the decisioning logic to understand how it was achieved. | Must have |  | 2b |
| DW.43 | The supplier shall ensure that the system shall have an automated collection strategy contact to consider, and where appropriate adhere to, the customer's channel of choice. | Must have |  | 2b |
| DW.44 | The supplier shall ensure that the system shall have the ability to segment customers based on criteria or flags on a customer or account. | Must have |  | 2b |
| DW.45 | The supplier shall ensure that the system shall have the ability to flag or tag a customer to identify exceptions, including but not limited to deceased, vulnerable, insolvencies, fraud, breathing space. | Must have |  | 2b |
| DW.46 | The supplier shall ensure that the system shall have the ability to trigger automated responses based on criteria including but not limited to status, collections journey, communications, dialler. | Must have |  | 2b |
| DW.47 | The supplier shall ensure that the system shall have the ability to automate a sequence of activity to take place within a workflow/treatment. | Must have |  | 2b |
| DW.48 | The supplier shall ensure that the system shall have the ability to allow actions for an agent to move an account back to a previous position when a customer situation has changed. | Must have |  | 2b |
| DW.49 | The supplier shall ensure that the system shall have the ability for account to enter, exit and pause within a workflow/treatment. | Must have |  | 2b |
| DW.50 | The supplier shall ensure that the system shall have the ability to manage workflow activities and communication when a customer has multiple accounts. | Must have |  | 2b |
| DW.51 | The supplier shall ensure that the system shall have the ability to pause communications to customers based on account or customer circumstances. | Must have |  | 2b |
| DW.52 | The supplier shall ensure that the system shall have the ability to automate case dealing without the need for manual intervention. When agents work a queue, an automatic push notification is sent advising case already being worked reducing the risk of duplicating contact. | Must have |  | 2b |
| DW.53 | The supplier shall ensure that the system shall have the ability to identify communications status, including but not limited to scheduled, sent, cancelled and not sent. | Must have |  | 2b |
| DW.54 | The supplier shall ensure that the system shall have the ability to diarise an account or customer for contact. | Must have |  | 2b |
| DW.55 | The supplier shall ensure that the system shall have the ability to control comms out to joint customers and third parties | Must have |  | 2b |
| DW.56 | The supplier shall ensure that the system shall have the ability to close an account or customer on default. | Must have |  | 2b |
| DW.57 | The supplier shall ensure that the system shall have the ability to clearly identify a position of an account or customer, including but not limited to, when in collections or recoveries, and the workflow or treatment they are in. | Must have |  | 2b |
| DW.58 | The supplier shall ensure that the system shall have the ability to freeze and unfreeze interest. | Must have |  | 2b |
| DW.59 | The supplier shall ensure that the system shall have the ability for agents to take an action on a case to trigger automated activities: send comms, move workflow, add flags, note account. | Must have |  | 2b |
| DW.60 | The supplier shall ensure that the system shall have an end or exit process for collections and recoveries treatments where arrears have been cleared. | Must have |  | 2b |
| DW.61 | The supplier shall ensure that the system shall have the ability to control repayment plan tolerance and set rules specific to plan type where a control allows set up and assess the plan when payments made or missed. | Must have |  | 2b |
| DW.62 | The supplier shall ensure that the system shall automate balance calculations as a result of transactions, interest freeze, and repayment plans. | Must have |  | 2b |
| DW.63 | The supplier shall ensure that the system shall have the ability to pre determine next steps in the collections journey to determine the position and assess best outcome for the customer. | Must have |  | 2b |
| DW.64 | The supplier shall ensure that the system shall automatically diarise customer or accounts for contact and review when they advance to an Outbound Call step within the communications path. | Must have |  | 2b |
| DW.65 | The supplier shall ensure that the system shall automatically send in app messages to customers as per the contact strategy. | Must have |  | 2b |
| DW.66 | The supplier shall ensure that the system shall automatically send SMS messages to customers as per the contact strategy. | Must have |  | 2b |
| DW.67 | The supplier shall ensure that the system shall automatically send emails to customers as per the contact strategy. | Must have |  | 2b |
| DW.68 | The supplier shall ensure that the system shall automatically send letters to customers as per the contact strategy. | Must have |  | 2b |
| DW.69 | The supplier shall ensure that each time a contact is sent, the account should be updated with the date, time, message type and message details, including contact details used (email or mobile number). | Must have |  | 2b |
| DW.70 | The supplier shall ensure that if automated messages fail, the account or customer should be updated to highlight that the message was not sent. | Must have |  | 2b |
| DW.71 | The supplier shall ensure that for failed messages, a daily exception report should be produced detailing all failed messages. | Must have |  | 2b |
| DW.72 | The supplier shall ensure that each communication will have an appropriate reference ID, for example (but not limited to): "send letter OD51" on day 5". | Must have |  | 2b |
| DW.73 | The supplier shall ensure that the system shall have the ability to change the content (e.g. of letter OD51) without changing the whole communications path. | Must have |  | 2b |
| DW.74 | The supplier shall ensure that for joint customers, all customers should receive the communication(s),including but not limited to Letters and SMS. | Must have | Where joint customers exist, letters etc should be sent to both customers named as joint responsibility. | 2b |
| DW.75 | The supplier shall ensure that the system shall have the ability to send an SMS to the customer once a payment has been received. | Must have |  | 2b |
| DW.76 | The supplier shall ensure that the system shall have the ability to see if an SMS was successfully delivered. | Must have |  | 2b |
| DW.77 | The supplier could ensure that the system has the ability to send comms to highlight that there is money elsewhere they can move. | Could have | This relates to where one account is in credit and another in debt, allowing the ability to communicate to the customer to enable transfer of credit to offset debt. | 2b |
| DW.78 | The supplier shall ensure that the system shall have functionality for customers to be able to make the payment necessary immediately via the link in the SMS message. | Must have |  | 2b |
| DW.79 | The supplier shall ensure that when accounts reach the stage where interest should be supressed, interest should be supressed automatically | Must have |  | 2b |
| DW.80 | The supplier shall ensure that the system shall have the ability to order the queue / worklist based on certain parameters. | Must have |  | 2b |
| DW.81 | The supplier shall ensure that the system shall have the ability to alter the comms strategy if the customer hasnâ€™t logged into the app or website in a period of time (to be configurable). | Must have | Some clients track website/mobile app logins, and where no login active then use alternative channels for communication strategy. | 2b |
| DW.82 | The supplier shall ensure that the system shall have the ability to manage customer or account, based on multiple data parameters including, but not limited to, value, arrears, age, product, and exceptions. | Must have |  | 2b |
| DW.83 | The supplier shall ensure that the system shall have the ability for customers who have overpaid to go into a separate queue. | Must have |  | 2b |
| DW.84 | The supplier shall ensure that the system shall have the ability to be able to identify if there has been fraud on an account. | Must have |  | 2b |
| DW.85 | The supplier shall ensure that customers or accounts will enter an 'exception queue / worklist' where they did not meet the strategy or treatment which they were expected to follow.  Any follow on action (such as a letter being triggered) must be supressed until the action has been ratified by an exceptions handler. | Must have |  | 2b |
| DW.86 | The supplier shall ensure that the system shall have the ability to configure and utilise within rules and segmentation recovery rates, and automatically determine a new single recovery rate using predefined rules to consolidate multiple previous rates. | Must have |  | 2b |
| DW.87 | The supplier shall ensure that the system shall have the ability to allow for the definition, modification and efficient implementation of  payment and deduction allocation rules to govern the most appropriate order in which debt types are recovered. | Must have |  | 2b |
| DW.88 | The supplier shall ensure that the system shall have the ability to automate writing off debt(s) based on DM business rules | Must have |  | 2b |

---

## 6. Repayment Plan Functionality

**Capability Group Description:** Supports creating, tracking, and managing repayment arrangements, including flexible scheduling, automated recalculations, and breach handling.

**Requirement Count:** 37

| Req. ID | Requirement | MoSCoW Rating | Context | ITT Question Grouping |
|---|---|---|---|---|
| RPF.1 | The supplier shall ensure that the system shall support flexible repayment arrangements, including but not limited to one off promise to pay weekly, monthly, flexible dates and values with the ability to set first payment in the future (1st payment date to be configurable e.g. today + x days). | Must have |  | 2b |
| RPF.2 | The supplier shall ensure that the system shall support all repayment types at the customer or account level. | Must have |  | 2b |
| RPF.3 | The supplier shall ensure that the system shall enable Users to set up repayment plans through the agent UI and through interfaces, an example of this may be through existing DWP self-service portal via integration. | Must have |  | 2b |
| RPF.4 | The supplier shall ensure that the system shall have the ability to amend arrangements once created. | Must have |  | 2b |
| RPF.5 | The supplier shall ensure that the system shall have the ability to monitor arrangements to ensure payments are received and appropriate actions are taken. | Must have |  | 2b |
| RPF.6 | The supplier shall ensure that the system shall have the ability to attach business rules, workflow or Strategies to the payment monitoring event, an example of this may be to issue a broken promise notification. | Must have |  | 2b |
| RPF.7 | The supplier shall ensure that the system shall have the ability to include tolerances in the payment date and value of payments, an example of this may be: if paid within 3 days of payment date, then do not mark as broken. | Must have |  | 2b |
| RPF.8 | The supplier shall ensure that the system shall have the ability to apply a review date to an arrangement that is independent from the completion date.  This review date could prompt an automatic or manual review or next action whilst allowing the arrangement to continue. | Must have |  | 2b |
| RPF.9 | The supplier shall ensure that the system shall have the ability to confirm an arrangement and prompt a payment i.e. send a letter or text. This could be a box that is ticked at the point of arrangement set up. | Must have |  | 2b |
| RPF.10 | The supplier shall ensure that the system shall have the ability to recognise the end of an arrangement and therefore automatically re-assess the arrangement at the appropriate time. | Must have |  | 2b |
| RPF.11 | The supplier shall ensure that the system shall have the ability to raise an automatic â€˜warningâ€™ to prompt agents at the point of set up in regards to the suitability of the arrangement for example length and amount, based on configurable business rules. | Must have |  | 2b |
| RPF.12 | The supplier shall ensure that the system shall have the ability for appending payments to the arrangement, triggering where appropriate - e.g. thank you for your payment. | Must have |  | 2b |
| RPF.13 | The supplier shall ensure that the system shall include auto interface with dialler file or outbound contact channel. | Must have |  | 2b |
| RPF.14 | The supplier shall ensure that the system shall have the ability to identify source of repayment plan setup -  List of payment sources should be configurable | Must have |  | 2b |
| RPF.15 | The supplier shall ensure that the system shall have the ability to record and identify the origin of any plans set i.e. Third party debt management company or customer direct. | Must have |  | 2b |
| RPF.16 | The supplier shall ensure that the system shall have the ability for re-aging to be automated in line with configurable business rules and existing constraints and control. | Must have | Re-aging relates to the age of debt and any payments coming in clearing oldest debt first, however this would be configurable based on business requirements, supports reporting. | 2b |
| RPF.17 | The supplier shall ensure that the system shall enable reason for broken arrangements - List of reasons should be configurable | Must have |  | 2b |
| RPF.18 | The supplier shall ensure that the system shall have the ability for automated workflow based on repayment offer or settlement submission. | Must have |  | 2b |
| RPF.19 | The supplier shall ensure that the system shall have an arrangement calculator to support appropriate payment plans catering for but not limited to interest and monthly payment | Must have |  | 2b |
| RPF.20 | The supplier shall ensure that the system shall have the ability to set up arrangements at a customer (group of accounts managing one customer balance) or individual account level. | Must have |  | 2b |
| RPF.21 | The supplier should ensure that the system shall integrate and leverage the business' current payment gateway | Should have | This related to integrating payment providers with the COTS solution, e.g. World Pay for DWP. | 2b |
| RPF.22 | The supplier shall ensure that the system shall have the capability to manage the set up, coordination, and re-submission of direct debits on payment arrangements controlled by business rules configured within the system. Direct Debit (DD) setup process needs to ensure bank details provided are valid through API integration, and to proceed only if valid. DD functionality must be UK banking compliant. | Must have |  | 2b |
| RPF.23 | The supplier shall ensure that payments scheduled for weekends or configurable dates (including but not limited to public holidays) to be processed on the business day after the payment is due. | Must have |  | 2b |
| RPF.24 | The supplier shall ensure that the system shall have the ability to take one off payments that are configurable and selectable to be either form or do not form part of the active payment arrangement. These payments could affect the balance of the account but not affect the payment amount or schedule of an active payment arrangement. | Must have |  | 2b |
| RPF.25 | The supplier shall ensure that the system shall record a customer forbearance start and end dates and all other relevant information. | Must have |  | 2b |
| RPF.26 | The supplier shall ensure that the system shall have the ability to take a payment over the phone. | Must have |  | 2b |
| RPF.27 | The supplier shall ensure that the system shall have the ability to capture and process customer payments via existing DWP IVR functionality | Must have |  | 2b |
| RPF.28 | The supplier shall ensure that the system shall have the ability for customers to make payments via bank transfer. | Must have |  | 2b |
| RPF.29 | The supplier shall ensure that the system shall have the ability to change payment dates for certain plans determined by the Authority, whilst considering the necessary controls to ensure the term of the repayment schedule is not impacted. | Must have |  | 2b |
| RPF.30 | The supplier shall ensure that the system shall have the ability for exception reports where automated allocation of monies is not possible (i.e. requiring manual judgement). | Must have |  | 2b |
| RPF.31 | The supplier shall ensure that the system shall have the functionality to take payments and apply to correct account or oldest arrears outstanding. | Must have |  | 2b |
| RPF.32 | The supplier shall ensure that the system shall have the functionality to display a broken plan 'counter' over a given period of time. | Must have |  | 2b |
| RPF.33 | The supplier shall ensure that the system shall have the ability to allow the solution integrate with any solution necessary to initiate, authorise and issue payments from the DWP to external parties (customer or non-customer). | Must have |  | 2b |
| RPF.34 | The supplier shall ensure that the system shall have the ability to receive recoveries from internal services and the appropriate level of Personally Identifiable Information (PII) to accurately account for the recoveries. | Must have |  | 2b |
| RPF.35 | The supplier shall ensure that the system shall have the ability to record any amounts owed to a customer. These recorded amounts can then be considered, based on defined business rules, as a potential source for offsetting arrears against existing debts. | Must have |  | 2b |
| RPF.36 | The supplier shall ensure that there is the ability to allow authorised users or the system to initiate and process payments specifically intended to pay off a designated debt. This may involve overriding standard payment allocation rules. | Must have |  | 2b |
| RPF.37 | The supplier shall ensure that the system shall have the ability to be configured to exclude specific debt types from recovery processes when they fall under a certain pre-defined strategy. | Must have |  | 2b |

---

## 7. User Access & Admin Functions

**Capability Group Description:** Enables secure role-based access control, user management, audit trails, and administrative configuration tools for system settings and parameters.

**Requirement Count:** 26

| Req. ID | Requirement | MoSCoW Rating | Context | ITT Question Grouping |
|---|---|---|---|---|
| UAAF.1 | The supplier shall ensure that the system shall have the ability to create, amend and delete users within the system and the ability to create role based access via API (i.e. integrate with DWP Place so a line manager approving access, results in a user profile with the correct permissions being created). | Must have |  | 2b |
| UAAF.2 | The supplier shall ensure that the system shall have the ability to assign roles and security level to users and business units including, but not limited to agents, team leaders, operations managers, compliance, and backoffice. | Must have |  | 2b |
| UAAF.3 | The supplier shall ensure that the system shall have the ability to update holiday dates of users so they can not be assigned work over the period. | Must have |  | 2b |
| UAAF.4 | The supplier shall ensure that the system shall have the ability to have role based access to data and functionality, such that user profiles have access based on the different job functions within the collections department | Must have |  | 2b |
| UAAF.5 | The supplier shall ensure that the system shall enable integration with business sign-on (DWP single sign on service). | Must have |  | 2b |
| UAAF.6 | The supplier shall ensure that the system shall have the ability to group teams and users as appropriate. | Must have |  | 2b |
| UAAF.7 | The supplier shall ensure that the system shall have the ability to segregate access to groups of accounts including, but not limited to, department or team level. | Must have |  | 2b |
| UAAF.8 | The supplier shall ensure that the system shall have the ability to view and report access history and failed attempts. | Must have |  | 2b |
| UAAF.9 | The supplier shall ensure that the system shall have a calendar based on geographic location, that sets all the working and non-working days for the organisations such that when accounts are actioned to a non-working day they can be dated backwards or forwards depending on the rule. | Must have |  | 2b |
| UAAF.10 | The supplier shall ensure that the system shall have settings and rules that apply to the entire system regardless of where it fits within the organisation, including but not limited to, current system date, system time zone, system geo-graphic location, initial strategy or step, initial status, organisation details. | Must have |  | 2b |
| UAAF.11 | The supplier shall ensure that the system shall have custom labels that appear on the User interface and Reports. | Must have |  | 2b |
| UAAF.12 | The supplier shall ensure that the system shalls allow all screens to be configurable and controlled by role based security i.e. some users will only able to see certain pieces of information. | Must have |  | 2b |
| UAAF.13 | The supplier shall ensure that the system shall have the ability to record a full diary of actions and contacts performed on the account both automatically and manually and for a copy of this contact history to be sent back to the host system. | Must have |  | 2b |
| UAAF.14 | The supplier shall ensure that the system shall allow users to open more than one customer or account at one time. | Must have |  | 2b |
| UAAF.15 | The supplier shall ensure that the system shall restrict treatments to certain users - an example may include but is not limited to having role profiles drilled down such as specialist team, escalations team, general collectors, and DCA. | Must have |  | 2b |
| UAAF.16 | The supplier shall ensure that the system shall not lock down access to an account following prior access by another agent. | Must have |  | 2b |
| UAAF.17 | The supplier shall ensure that the system shall, where possible, remove the need for IT requests to unlock, re-grant access to user profiles. This process must be efficient and straight forward. | Must have |  | 2b |
| UAAF.18 | The supplier shall ensure that the system shall have broadcast message functionality to all users, examples may be but are not limited to: â€˜system goes down early tonight at 7:45pmâ€™, or â€˜payment systems are presently downâ€™. | Must have |  | 2b |
| UAAF.19 | The supplier shall ensure that the system shall include a calendar concept to add in user rota, tasks, schedules, as well as linking to a time management solution for non-working days and related possibilities. | Must have |  | 2b |
| UAAF.20 | The supplier should ensure that the system shall have the ability to enable DWP to comply with GDPR requirements including, but not limited to, data anonymisation, archive and purge. | Should have |  | 2b |
| UAAF.21 | The supplier shall ensure that limits can be set per user role including, but not limited to, write-off limits, and reversal/credits. | Must have |  | 2b |
| UAAF.22 | The supplier shall enable wide access to the system with relevant RBAC to control functions, capability, and actions including, but not limited to, referral from other departments. | Must have |  | 2b |
| UAAF.23 | The supplier shall ensure that all system access and actions to be logged and easily viewable and reportable. | Must have |  | 2b |
| UAAF.24 | The supplier shall ensure that the system shall send alerts when an automated operation has failed through batch or API or where an exception has been triggered. | Must have |  | 2b |
| UAAF.25 | The supplier shall ensure that the system shall have the ability for designated individuals to override rules if deemed necessary for the customer, based on RBAC. | Must have |  | 2b |
| UAAF.26 | The supplier shall ensure that the system shall have the ability for designated individuals to manually do transactions to update debt balances for accounting and audit purposes within controlled restrictions. Designated individuals may include, but are not limited to, team manager and advisor roles. | Must have |  | 2b |

---

## 8. MI & Reporting

**Capability Group Description:** Includes management information dashboards, standard reports, and ad-hoc reporting tools to support performance tracking, compliance, and decision-making.

**Requirement Count:** 2

| Req. ID | Requirement | MoSCoW Rating | Context | ITT Question Grouping |
|---|---|---|---|---|
| MIR.1 | The supplier shall ensure that the system shall provide single source for all collections data items. | Must have | To be provided by DWP Strategic solution (Power BI) | 2a |
| MIR.2 | The supplier shall ensure that the system shall have the ability to (i) export MI data; and (ii) import and integrate other data sources into the system, including into the reporting and MI data function (both in real time and by batch). | Must have | To be provided by DWP Strategic solution (Power BI) | 2a |

---

## 9. Analytics

**Capability Group Description:** Includes scoring models, predictive analytics, and segmentation tools to optimize treatment strategies, prioritize accounts, and evaluate risks.

**Requirement Count:** 2

| Req. ID | Requirement | MoSCoW Rating | Context | ITT Question Grouping |
|---|---|---|---|---|
| A.1 | The supplier shall ensure that the system shall have the ability to configure champion challenger strategies on predictive analysis | Must have |  | 2a |
| A.2 | The supplier shall ensure that the system shall have decision engine or capabilities to analyse and forecast trends, segment debtors and debts. | Must have |  | 2a |

---

## 10. User Interface Screens

**Capability Group Description:** Refers to the user interface design, including layout, usability, and customer or case views tailored to agent roles and tasks.

**Requirement Count:** 30

| Req. ID | Requirement | MoSCoW Rating | Context | ITT Question Grouping |
|---|---|---|---|---|
| UI.1 | The supplier shall ensure that the system shall have a modern and intuitive interface, with search functions for simple and complex wildcard searches across all major entities of the system (Account, Customer, and Third Party). | Must have |  | 2b |
| UI.2 | The supplier shall ensure that the system shall provide full view of communications panel and must also have ability to trigger ad-hoc communications. | Must have |  | 2b |
| UI.3 | The supplier shall ensure that the system shall have case history and notes available and searchable, with filters to easily interrogate key information. Where possible, this information should be held in structured data field. | Must have |  | 2b |
| UI.4 | The supplier shall ensure that the system shall provide the user with a view of pertinent information at each stage of a call based on individual account characteristics - this may be, but is not limited to, screen pop ups based on rules/conditions i.e. "This customer isâ€¦ Offer DD,.. Offer Payment Plans". | Must have |  | 2b |
| UI.5 | The supplier shall ensure that the system shall enable collectors/agents to key in important comments when there is a specific piece of information that requires sensitivity (including, but not limited to, deceased or recent loss) or any complaints made by the customer. | Must have |  | 2b |
| UI.6 | The supplier could ensure that the system shall allow use of RAG statuses on scores. | Could have |  | 2b |
| UI.7 | The supplier shall ensure that the system shall have the options for default screen layout which can be customisable based on best workflow. | Must have |  | 2b |
| UI.8 | The supplier could ensure that the system has multilingual ability based on role, group, or individual agent user level or preference (this may include, but is not limited to, English and Welsh). | Could have |  | 2b |
| UI.9 | The supplier shall ensure that the system shall have the ability to work on multiple accounts, customers or cases in parallel, including, but not limited to, interactive communications channels and manual worklists. | Must have |  | 2b |
| UI.10 | The supplier should ensure that the system shall have online agent capability, including, but not limited to, capability to share and discuss accounts with different skilled colleagues. | Should have |  | 2b |
| UI.11 | The supplier shall ensure that the system shall have built-in online help tools and agent scripting. | Must have |  | 2b |
| UI.12 | The supplier shall ensure that the system shall have a full view of all pertinent collections information such as, but not limited to, statuses, current position in collections, current strategy, and next activities. | Must have |  | 2b |
| UI.13 | The supplier shall ensure that the system shall have calendar functionality for historic actions view and upcoming tasks, strategy actions and reminders. | Must have |  | 2b |
| UI.14 | The supplier shall ensure that the system shall have the ability to have Printable history. | Must have |  | 2b |
| UI.15 | The supplier should maintain and change the look and feel, branding and theming as required post implementation of the system. | Should have |  | 2b |
| UI.16 | The supplier shall ensure that the system shall enable an agent to see a single customer view and an account view of all transactions, interactions, payment arrangements and related parties to the customer and account. | Must have |  | 2b |
| UI.17 | The supplier shall ensure that the system shall enable an agent to see their portfolio segmented based on different criteria (including, but not limited to, action date overdue, status, and client). | Must have |  | 2b |
| UI.18 | The supplier could ensure that the system shall enable an agent to use keyboard shortcuts. | Could have |  | 2b |
| UI.19 | The supplier shall ensure that the system shall have the ability to configure screen layout and contents with minimal to no technology team involvement. | Must have |  | 2b |
| UI.20 | The supplier should ensure that the system shall have prompts and compliance checkpoints throughout the customer interaction to direct the call to the most appropriate outcome. | Should have |  | 2b |
| UI.21 | The supplier shall ensure that the system shall have the ability for flash notes or banners for specialist treatments and exceptions when accessing a customer case and their accounts, including, but not limited to vulnerability identified, insolvencies, deceased, forbearance, gone away/trace. | Must have |  | 2b |
| UI.22 | The supplier should ensure that the system shall have graphical strategy history for agent view to tailor conversations on previous contact and upcoming actions. | Should have |  | 2b |
| UI.23 | The supplier shall ensure that the system shall have identification and verification functionality which will prompt the agent to ask the correct security question to the customer. | Must have |  | 2b |
| UI.24 | The supplier shall ensure that the system shall have the ability for agents to be able to see a 3rd party screen. | Must have |  | 2b |
| UI.25 | The supplier shall ensure that the system shall be configurable:<br>Who the 3rd party is<br>Why the 3rd part is relevant<br>How to contact 3rd party<br>ID&V for 3rd party | Must have |  | 2b |
| UI.26 | The supplier shall ensure that agents have the ability to diarise an outbound call, choosing date and time period (e.g. am / pm). | Must have |  | 2b |
| UI.27 | The supplier shall ensure that agents have the ability to add notes to each case to record what manual actions were carried out. | Must have |  | 2b |
| UI.28 | The supplier shall ensure that the system shall have automated prompting of completion of I&E requirement for selected workflows. | Must have |  | 2b |
| UI.29 | The supplier shall ensure that the system shall give robot users appropriate access based on RBAC. | Must have | Robot users have appropriate access based on RBAC | 2b |
| UI.30 | The supplier shall ensure that the system shall allow administrators to build in business logic and decision points, to provide agents with guided workflows, ensuring accurate completion of sub tasks, tasks and workflows. | Must have |  | 2b |

---

## 11. Work Allocation & Monitoring

**Capability Group Description:** Provides mechanisms for distributing work to collection agents (e.g., by skill, availability, priority) and tools for supervisors to monitor queues and productivity.

**Requirement Count:** 28

| Req. ID | Requirement | MoSCoW Rating | Context | ITT Question Grouping |
|---|---|---|---|---|
| WAM.1 | The supplier shall ensure that the system shall support use of shared and case managed queues or work lists as appropriate. | Must have |  | 2b |
| WAM.2 | The supplier shall ensure that the system shall have the ability to manually move work between teams, as well as manually refer case to another agent, such as a senior agent. | Must have | Relates to when a customer is in a worklist or a queue | 2b |
| WAM.3 | The supplier shall ensure that the system shall enable debt accounts to be automatically assigned through workflow. | Must have |  | 2b |
| WAM.4 | The supplier shall ensure that the system shall have the ability to have real time worklists updates through the day. | Must have |  | 2b |
| WAM.5 | The supplier shall ensure that the system shall have the ability to prioritise and filter worklists based on complex criteria to be defined through configuration. | Must have |  | 2b |
| WAM.6 | The supplier shall ensure that the system shall enable users to generate worklist reports detailing numbers completed and disposition of the worked accounts (including, but not limited to, no contact, wrong party, dated out, and accounts referred). | Must have |  | 2b |
| WAM.7 | The supplier shall ensure that the system shall enable control over whether accounts need to be worked in order or can be selected by the agent (if applicable). | Must have |  | 2b |
| WAM.8 | The supplier shall ensure that the system shall have the ability to work on arrears and non-arrears by exception i.e. flexibility to bring accounts in and out of arrears, for example, pre-delinquency. | Should have |  | 2b |
| WAM.9 | The supplier shall ensure that the system shall have out of the box worklist reporting, SLAs, and notifications (including, but not limited to, on-screen and email alerts). | Must have |  | 2b |
| WAM.10 | The supplier could ensure that the system shall have configurable worklist layouts of based on user role and preference. | Could have |  | 2b |
| WAM.11 | The supplier shall ensure that the system shall allow worklists to be assigned to roles and user groups containing multiple agents. | Must have |  | 2b |
| WAM.12 | The supplier shall ensure that the system shall have the ability to set worklists for user selection or automated work allocation. | Must have |  | 2b |
| WAM.13 | The supplier shall ensure that the system shall allow relevant RBAC to control which roles and users can view accounts within certain worklists - including, but not limited to, staff accounts and sensitive cases. | Must have |  | 2b |
| WAM.14 | The supplier shall ensure that the system shall allow different views available based on the worklists in question for pertinent immediate information including sorting and filtering. | Must have |  | 2b |
| WAM.15 | The supplier shall ensure that the system shall allow automatic distribution of cases based on configurable rules - including, but not limited to initial allocation rules, and daily rules based on agent availability (which may include, but is not limited to, holiday and sickness). | Must have |  | 2b |
| WAM.16 | The supplier shall ensure that the system shall have the ability to assign and re-assign cases by team manager (or applicable authority role) manually. | Must have |  | 2b |
| WAM.17 | The supplier shall ensure that the system shall allow automatic assignment of workflow exceptions to be worked by a user or team as allocated appropriately based on business rules. | Must have |  | 2b |
| WAM.18 | The supplier shall ensure that agent notifications are based on assigned accounts and cases. | Must have |  | 2b |
| WAM.19 | The supplier shall ensure that agents can see just the accounts they need to work to reduce distractions and improve directed workflow adherence. This should not limit the ability to search other accounts if there is an inbound call. | Must have |  | 2b |
| WAM.20 | The supplier shall ensure that the system shall have the ability for defined segments or groups of customers to be exclusively managed by specialist agents (including, but not limited to, vulnerable customers).  Accounts that are classified in this manner should be controlled by permissions and access restricted accordingly. | Must have |  | 2b |
| WAM.21 | The supplier shall ensure that the system shall prioritise work but allow agents access to full portfolio. | Must have |  | 2b |
| WAM.22 | The supplier shall ensure that the system shall assign accounts or customers to agents based on a strategy with minimal or no manual intervention. | Must have |  | 2b |
| WAM.23 | The supplier shall ensure that the system shall flag an account to highlight particular characteristics that impact how it should be managed (including, but not limited to, property owner, and secured debt with security). This information should be prominently displayed on the screen. | Must have |  | 2b |
| WAM.24 | The supplier shall ensure that the system shall refer a case to someone with appropriate authority, by system recommendation, where the action is outside user delegated authority (examples include, but are not limited to, payment arrangement falls outside of business defined thresholds, and escalation requirement to a team leader). | Must have |  | 2b |
| WAM.25 | The supplier shall ensure that the system shall enable team leaders and managers to have ability to manage the workload and allocation amongst their team. | Must have |  | 2b |
| WAM.26 | The supplier shall ensure that the system shall enable a report that identifies where accounts have not been actioned within defined timeframes or SLA. | Must have |  | 2b |
| WAM.27 | The supplier shall ensure that the system shall have the ability to verify that required workflow actions have being completed as scheduled. | Must have |  | 2b |
| WAM.28 | The supplier shall ensure that accounts in queues and worklist stay in the queue or worklist until either completed or positive activity taken. | Must have |  | 2b |

---

## 12. Agent Actions & Dispositions

**Capability Group Description:** Captures all actions taken by agents and their outcomes, enabling a full case history.

**Requirement Count:** 27

| Req. ID | Requirement | MoSCoW Rating | Context | ITT Question Grouping |
|---|---|---|---|---|
| AAD.1 | The supplier shall ensure that the system shall make a full range of actions available to the user based on process type. | Must have |  | 2b |
| AAD.2 | The supplier shall ensure that the system shall have the ability to configure actions to perform functions including, but not limited to, updating workflow, data, status, notes, calculations, and debt write off. | Must have |  | 2b |
| AAD.3 | The supplier shall ensure that the system shall have the availability of user actions and limited by workflow, including Rules based. | Must have |  | 2b |
| AAD.4 | The supplier shall ensure that the system shall have the ability to have controls in place to monitor, warn and prevent inappropriate user actions based on RBAC. | Must have |  | 2b |
| AAD.5 | The supplier shall ensure that the system shall record full history of agent actions for audit and control purposes. | Must have |  | 2b |
| AAD.6 | The supplier shall ensure that the system shall integrate with DWP payments solution provider to remove the need to copy and paste account, card and address details into a separate payment window. | Must have |  | 2b |
| AAD.7 | The supplier shall ensure that the system shall pre-record standardised notes on the system | Must have |  | 2b |
| AAD.8 | The supplier could ensure that the system Payment solution will allow future dated payment feature. | Could have |  | 2b |
| AAD.9 | The supplier should ensure that the system shall auto stamp key activities back to memos. i.e. "payment taken for X" should be applied automatically. | Should have |  | 2b |
| AAD.10 | The supplier shall ensure that the system shall allow actions to be operated against all of the customers accounts, or just one. This allows agents to action all debts associated with a customer, including relevant notes and memos on customer accounts. | Must have |  | 2b |
| AAD.11 | The supplier shall ensure that actions are controlled through RBAC of user roles and privileges. | Must have |  | 2b |
| AAD.12 | The supplier shall ensure that bulk actions that can be operated against all debt accounts or a subset of accounts based on pre-defined parameters, to be controlled through RBAC. | Must have |  | 2b |
| AAD.13 | The supplier shall ensure that the system shall have standard out of the box controls to monitor, warn and prevent inappropriate user actions. | Must have |  | 2b |
| AAD.14 | The supplier shall ensure that the system shall have the ability to escalate to a manager or higher authority role on a real-time basis, or flagged to higher authority role queues to be reviews, assessed, and worked. | Must have |  | 2b |
| AAD.15 | The supplier could ensure that the system shall have the ability to redact content within notes and documents in the system and export the content for external use. | Could have |  | 2b |
| AAD.16 | The supplier shall ensure that the system shall have the ability to mark a contact, across any system entity (including, but not limited to, customer, account, contact, and lead), as a 'Do Not Contact' and for this information to apply across the system for that number. This restriction could be reversed in future should the need change. | Must have |  | 2b |
| AAD.17 | The supplier shall ensure that the system shall have the ability to set up an authorised third party for a customer. | Must have |  | 2b |
| AAD.18 | The supplier shall ensure that the system shall provide the ability to set flags against a customer or account for different circumstances (including, but not limited to, domestic violence and skip trace). | Must have |  | 2b |
| AAD.19 | system can restrict next action based on defined rules - i.e. maximum days for vulnerable, specialist, etc | Must have |  | 2b |
| AAD.20 | The supplier shall ensure that all manual action steps within the communications path workflows and customer outcome workflows have controls to ensure collections and recoveries â€˜rulesâ€™ are adhered to, including, but not limited to:<br>- No payment in x days<br>- No customer dispute or exception<br>- Diarise customer call back within x days.<br>- PTP / ATP  arrangement must clear the arrears amount. | Must have |  | 2b |
| AAD.21 | The supplier shall ensure that the system shall have the ability to provide the functionality for authorised users to recall records currently held by external collections agencies. Following recall the system ensures the reinstatement of the record to the most appropriate strategy for that customer. | Must have |  | 2b |
| AAD.22 | The supplier shall ensure that the system shall have the ability to allow authorised users to manually transfer designated debts to external collection agencies and includes the automated transfer in line with pre-defined business rules. | Must have |  | 2b |
| AAD.23 | The supplier shall ensure that the system shall have the ability to suspend and to stop collections at customer or account level manually or automatically. | Must have |  | 2b |
| AAD.24 | The supplier shall ensure that the system shall have the ability to manually add and amend Financial transactions with appropriate RBAC. | Must have |  | 2b |
| AAD.25 | The supplier shall ensure that the system shall have the ability to manually amend balances on accounts with appropriate RBAC. | Must have |  | 2b |
| AAD.26 | The supplier shall ensure that the system shall have the ability to re-categorise accounts with appropriate RBAC. | Must have |  | 2b |
| AAD.27 | The supplier shall ensure that the system shall have the ability to manually load new accounts including, but not limited to, ILS/ELS and UC. | Must have |  | 2b |

---

## 13. 3rd Party Management

**Capability Group Description:** Supports placing accounts with external collection agencies or legal firms, tracking their performance, and reconciling payments or commissions.

**Requirement Count:** 18

| Req. ID | Requirement | MoSCoW Rating | Context | ITT Question Grouping |
|---|---|---|---|---|
| 3PM.1 | The supplier should ensure that a DCA management functionality is integrated into the system in order to optimise placements based on configurable business rules. | Should have |  | 2b |
| 3PM.2 | The supplier should ensure that the system has the ability to handle allocation, reconciliation, recall, updates to and from third parties, payments management, payment reconciliation, performance monitoring, audit, fee calculation and payments checking for (DCAâ€™s, Legal, Field Agents). | Should have |  | 2b |
| 3PM.3 | The supplier should ensure that the system has the ability to facilitate manual placement of accounts with a third party. | Should have |  | 2b |
| 3PM.4 | The supplier should ensure that the system has the ability to assign accounts to specific third parties based on user defined criteria. | Should have |  | 2b |
| 3PM.5 | The supplier should ensure that the system includes an interface for new assignments and future financial and non-financial updates to the account. | Should have |  | 2b |
| 3PM.6 | The supplier should ensure that the system has the ability to calculate and post commission to a financial transaction i.e. must have an interface to calculate agency commission. | Should have |  | 2b |
| 3PM.7 | The supplier should ensure that the system has the ability to recall the debt from the agency by means of strategy rules, manually, and bulk. | Should have |  | 2b |
| 3PM.8 | The supplier should ensure that the system has the ability to include an audit mechanism to ensure that the agency and client records are in-line. | Should have |  | 2b |
| 3PM.9 | The supplier should ensure that the system reporting is generated based on agency performance tracking. | Should have |  | 2b |
| 3PM.10 | The supplier should ensure that the system enables customer level placement to ensure the same customer is not placed with the same agent twice or with a different agent at the same time. | Should have |  | 2b |
| 3PM.11 | The supplier should ensure that out of the box integrations support litigation, enforcement, asset management, DMCâ€™s, and CCBC. | Should have |  | 2b |
| 3PM.12 | The supplier should ensure that the system has advanced system logic that allows clients to configure the rules around debt assignment and exclusion, including rules for re-assignment to third parties. | Should have |  | 2b |
| 3PM.13 | The supplier should ensure that the system has account reconciliation processes to safeguard from data inaccuracies. | Should have |  | 2b |
| 3PM.14 | The supplier could ensure that the system has a query portal / integration for accounts being managed by third-parties for queries to be logged and dealt with intraday. | Could have |  | 2b |
| 3PM.15 | The supplier should ensure that the system has in-depth standard out of the box third-party management reporting suite. | Should have |  | 2b |
| 3PM.16 | The supplier should ensure that the system has standard reconciliation in place with 3rd parties to provide full control and audit reporting. | Should have |  | 2b |
| 3PM.17 | The supplier could ensure that the system has standard debt sale functionality to cater for differing portfolios and sale types on an ad hoc and forward flow perspective. | Could have |  | 2b |
| 3PM.18 | The supplier could ensure that the system has configurable commission schemes and models with a variety of metrics and flexible rulesets. | Could have |  | 2b |

---

## 14. Income & Expenditure Capture

**Capability Group Description:** Allows agents or customers to input income and expenses to assess affordability, often forming the basis for setting repayment plans.

**Requirement Count:** 11

| Req. ID | Requirement | MoSCoW Rating | Context | ITT Question Grouping |
|---|---|---|---|---|
| IEC.1 | The supplier shall ensure that the system shall integrate Income and expenditure capture into the collections system. | Must have |  | 2b |
| IEC.2 | I&E form to be available online and app and so can be completed by the Customer at their convenience and then submitted (i.e. via self service or direct contact) | Must have | If I&E updated via SS or app then integration feed would pass relevant information to COTS solution. | 2b |
| IEC.3 | The supplier shall ensure that values entered into the system should be validated against trigger values, account data and CRA data. | Must have |  | 2b |
| IEC.4 | The supplier shall ensure that the system shall: (i) enable repayment options to be automatically generated through the collections system and digital channels, based on I&E data for a customer [/and/or an account]; and (ii) present all available repayment options to users and permit users to select one of the available repayment options for the relvant customer [and/or account]. | Must have |  | 2b |
| IEC.5 | The supplier shall ensure that the system shall have configurable parameterisation for linkage between I&E and payment arrangement offerings. | Must have |  | 2b |
| IEC.6 | The supplier shall ensure that the system shall enable I&E data to be used to inform collections strategy. | Must have |  | 2b |
| IEC.7 | The supplier should ensure that the system shall allow linkage to third-party data sources to capture and verify customer data including, but not limited to, open banking and bureau data. | Should have |  | 2b |
| IEC.8 | The supplier should ensure that the system shall have automated linkage between I&E submission and payment arrangement suggestions through customer portal or agent UI. | Should have |  | 2b |
| IEC.9 | The supplier should ensure that the system shall utilise data available (internal or 3rd party) to auto populate and suggest I&E values based on known transactional data. | Should have |  | 2b |
| IEC.10 | The supplier shall ensure that the system shall have the ability to "save" incomplete I&E either by an agent or a customer to revisit when required - use case, not all information to hand. | Must have |  | 2b |
| IEC.11 | The supplier shall ensure that I&E can be configurable where required by the client based on client attributes. | Must have | <br> | 2b |

---

## 15. Bureau & Scorecard Feeds

**Capability Group Description:** Integrates data from credit bureaus and internal/external scorecards for enhanced risk assessment and decisioning.

**Requirement Count:** 15

| Req. ID | Requirement | MoSCoW Rating | Context | ITT Question Grouping |
|---|---|---|---|---|
| BSF.1 | The supplier should ensure that the system supports both single and multiple two-way bureau data feeds. | Should have |  | 2c |
| BSF.2 | The supplier should ensure that the system has the ability to create a variety of scores relevant for the collection and recovery process including, but not limited to, propensity to pay, likelihood of write-off, trigger into collection, treatment or segmentation. | Should have |  | 2c |
| BSF.3 | The supplier should ensure that the system has standard integration with credit bureaus such as, but not limited to, Experian, Equifax, and TransUnion for standard data ingestion and enrichment. | Should have |  | 2c |
| BSF.4 | The supplier should ensure that the system has standard or example models within scorecard functionality to provide initial benefit and direction to clients. | Should have |  | 2c |
| BSF.5 | The supplier should ensure that the system has portfolio performance evaluation to propose effective scorecard and decisioning segmentation. | Should have |  | 2c |
| BSF.6 | The supplier should ensure that the system has the ability to configure bureau data feeds within strategy decision engine to manage workflow. | Should have |  | 2c |
| BSF.7 | The supplier should ensure that the system has modelling techniques for predictive and adaptive modelling using standard modelling languages (PMML, Python, R). | Should have |  | 2c |
| BSF.8 | The supplier should ensure that the system has auto integration with data cleanse partners based on communication results including, but not limited to, dialler bad numbers, and email bounce backs. | Should have |  | 2c |
| BSF.9 | The supplier should ensure that the system has the ability to integrate directly with 3rd party decisioning, scorecard, and modelling solutions. | Should have |  | 2c |
| BSF.10 | The supplier should ensure that the system has scorecard creation, simulation, and evaluation tools out of the box  including, but not limited to propensity to pay, likelihood of write-off, and success at litigation. | Should have |  | 2c |
| BSF.11 | The supplier should ensure that the system has ID&V decision modeller to provide different questions and answers for different customers depending on characteristics and answers. | Should have |  | 2c |
| BSF.12 | The supplier should ensure that the system has the capability for ingestion of bureau data or triggers based on industry recognised data including, but not limited to, insolvency notification, deceased register, and address change via post. | Should have |  | 2c |
| BSF.13 | The supplier should ensure that the system gsa the ability to apply scorecards to drive the allocation of accounts to agents, teams, workflow or communications. | Should have |  | 2c |
| BSF.14 | The supplier should ensure that the system has the ability to import a scorecard into the system to reduce effort and improve accuracy. | Should have |  | 2c |
| BSF.15 | The supplier should ensure that the system has the ability to monitor the accuracy and performance of the scorecards used across the collection platform. | Should have |  | 2c |

---

## 16. Contact Channels

**Capability Group Description:** Supports multi-channel engagement such as phone, SMS, email, chatbot, and letter, including preference management and communication tracking.

**Requirement Count:** 35

| Req. ID | Requirement | MoSCoW Rating | Context | ITT Question Grouping |
|---|---|---|---|---|
| CC.1 | The supplier could ensure that the system shall implement Omni (or Opti channel communications) and create a common communications platform. | Could have |  | 2b |
| CC.2 | The supplier shall ensure that the system shall support letters, email (one and two way), SMS (one and two way), dialler, IVR, IVM, Webchat, via integration with DWP strategic solution. | Must have |  | 2b |
| CC.3 | The supplier should ensure that the system shall have the ability to save previous history and create analytical models around optimised contact channel, best time to contact. | Should have |  | 2b |
| CC.4 | The supplier shall ensure that the system shall have interfaces to all channels. | Must have |  | 2b |
| CC.5 | The supplier shall ensure that the system shall have the ability to support champion challenge strategy analysis, | Must have |  | 2b |
| CC.6 | The supplier shall ensure that the system shall make it possible for the content of any correspondence sent or received to be viewable. | Must have |  | 2b |
| CC.7 | The supplier shall ensure that the system shall have the ability to re-trigger correspondence, i.e. to re-send a letter to a customer at any given time. | Must have |  | 2b |
| CC.8 | The supplier shall ensure that the system shall have the ability to manage multiple occurrences for the same channel (for example, but not limited to, sending 2 letters, one to the customer and one to the landlord).  This includes the ability to utilise different addresses for example to site and the account address. | Must have |  | 2b |
| CC.9 | The supplier shall ensure that the system shall have the ability to leave a footprint on the account record to show that an action has taken place, and send this footprint back to the host. This should be possible both real time (for example, but not limited to, for SMS and Email) and batch (for example, but not limited to, for letters). | Must have |  | 2b |
| CC.10 | The supplier could ensure that the system shall have pre-built decisioning and/or models for the best time and method to contact. | Could have |  | 2b |
| CC.11 | The supplier shall ensure that the system shall have a single customer communication UI that can display all communications with a customer on a single screen for agents to follow the conversation easily. | Must have |  | 2b |
| CC.12 | The supplier could ensure that the system shall have standard out of the box integration with wider digital communication channels – examples include, but are not limited to, WhatsApp, Facebook Messenger and various Chat Bot platforms. | Could have |  | 2b |
| CC.13 | The supplier should ensure that the system shall have integration priority based blended technologies for inbound and outbound communications, an example of this is, but is not limited to, an agent may be available for inbound or outbound calls on a single routing telephony platform, alongside being set up for receiving webchat requests from customers. | Should have |  | 2b |
| CC.14 | The supplier shall ensure that the system shall perform document upload where necessary that can be interfaced back into document management systems. | Must have |  | 2b |
| CC.15 | The supplier shall ensure that the system shall allow multiple response channels to be carried out at once for example, but not limited to, 2-way SMS, web chat, email. | Must have |  | 2b |
| CC.16 | The supplier could ensure that the system shall have the ability to record and screen capture for quality, training, and monitoring. | Could have |  | 2b |
| CC.17 | The supplier could ensure that the system shall have Integrated speech analytics. | Could have |  | 2b |
| CC.18 | The supplier shall ensure that the system shall do message scheduling / throttling based on configurable parameters. | Must have |  | 2b |
| CC.19 | The supplier shall ensure that the system shall have screen-pop capability for inbound communications. | Must have |  | 2b |
| CC.20 | The supplier shall ensure that the system shall perform real-time updates following any customer communication deemed relevant to Collections based on, but not limited to, trigger values, set disposition, and action codes. | Must have |  | 2b |
| CC.21 | The supplier shall ensure that the system shall capture, flag, receive notification of communication exclusion (per channel) for appropriate customer contact. | Must have |  | 2b |
| CC.22 | The supplier shall ensure that the system shall have the ability to self-serve via online portal and/or app - I&E, arrangements, payment, change of details using DWP strategic component. | Must have |  | 2b |
| CC.23 | The supplier shall ensure that standard contact templates can be created by business system administrators. | Must have |  | 2b |
| CC.24 | The supplier shall ensure that the system shall have the ability to set and capture customer's preferred contact times and methods including language and accessibility needs and for these to apply to automated contact strategies. | Must have |  | 2b |
| CC.25 | The supplier shall ensure that the system shall have the ability for any documentation supplied by third parties (including, but not limited to, originator and third party governing bodies) to be retained and stored against the customer and/or account. | Must have |  | 2b |
| CC.26 | The supplier shall ensure that the system has the ability to set up campaigns and strategies to send out communication to customers that allows them to make a payment, arrangement, offer or settlement and this interaction captured within the system without any manual intervention. | Must have |  | 2b |
| CC.27 | The supplier shall ensure that the number of customer contacts and defined contact times and limits are configurable to enable DWP to set and update them (for example, but not limited to, should legislation change). | Must have |  | 2b |
| CC.28 | The supplier shall ensure that Dialler to operate in predictive, progressive and preview modes. | Must have |  | 2b |
| CC.29 | The supplier shall ensure that outbound calls to be sorted automatically into queues depending on configurable parameters. | Must have |  | 2b |
| CC.30 | The supplier shall ensure that the system shall have the ability to automatically record an unsuccessful outbound call to allow additional follow up comms to be automatically sent. | Must have |  | 2b |
| CC.31 | The supplier shall ensure that the system shall have the ability to automatically supress communications based on DM business rules. | Must have |  | 2b |
| CC.32 | The supplier shall ensure that the system shall allow users to manually supress communications based on DM business rules and user permissions. | Must have |  | 2b |
| CC.33 | The supplier shall ensure that the system shall be able to automatically remove suppression of communications based on DM business rules. | Must have |  | 2b |
| CC.34 | The supplier shall ensure that the system shall allow users to manually remove suppression of communications based on DM business rules and user permissions. | Must have |  | 2b |
| CC.35 | The supplier shall ensure that the system shall have the ability to send communications to third parties. | Must have |  | 2b |

---

## 17. Interfaces to 3rd Party Systems

**Capability Group Description:** Covers API or batch interfaces with CRM, core banking, payment gateways, bureau data providers, and other enterprise systems.

**Requirement Count:** 15

| Req. ID | Requirement | MoSCoW Rating | Context | ITT Question Grouping |
|---|---|---|---|---|
| I3PS.1 | The supplier could ensure that the system shall support real-time full lifecycle interaction with DCA's. | Could have |  | 2b |
| I3PS.2 | The supplier shall ensure that the system shall support real-time interaction with web portals including, but not limited to, customer self service and IVR. | Must have |  | 2b |
| I3PS.3 | The supplier shall ensure that the system shall support both real-time and batched interfaces to 3rd party providers and systems. | Must have |  | 2b |
| I3PS.4 | The supplier could ensure that the system shall allow access to the system remotely for third parties such as DCA's. | Could have |  | 2b |
| I3PS.5 | The supplier shall ensure that the system shall support data exchange in real time with web services i.e. IVR. | Must have |  | 2b |
| I3PS.6 | The supplier shall ensure that the system shall support integration with host systems to carry out or invoke standard procedures such as, but not limited to: Write-off, account blocks, closure, account management, card management, customer details. | Must have |  | 2b |
| I3PS.7 | The supplier could ensure that the system shall support integration with data providers to enrich data such as, but not limited to: contact details (address, phone, email etc.), demographic data, and employer information. | Could have |  | 2b |
| I3PS.8 | The supplier could ensure that the system shall have the ability to integrate with recognised parties e.g. Insolvency register, deceased register, post, councils, etc. | Could have |  | 2b |
| I3PS.9 | The supplier shall ensure that the system shall have the ability to integrate with the general ledger/core accounting system. | Must have |  | 2b |
| I3PS.10 | The supplier shall ensure that the system shall interface or integrate with external document scanning systems to quickly call up customer documents. | Must have | There can or will be integrations internally within DWP | 2b |
| I3PS.11 | The supplier shall ensure that the system shall have the ability to integrate with third party, internal and outsource providers in a seamless way that could be via batch or real time integration. | Must have | There can or will be integrations internally within DWP and Outside. | 2b |
| I3PS.12 | The supplier shall ensure that the system shall have the ability to migrate customers and accounts from DM6 to FDS (Future Debt Service). | Must have |  | 2b |
| I3PS.13 | The supplier shall ensure that the system shall have the ability to back out customer and account migrated to FDS and transfer back to DM6. | Must have |  | 2b |
| I3PS.14 | The supplier shall ensure that the system shall include dialler functionality to support outbound call process. | Must have |  | 2b |
| I3PS.15 | The supplier shall ensure that the system shall have the ability to trigger/alert to Archive/Purge the collections solution. | Must have |  | 2b |

---

## 18. System of Record

**Capability Group Description:** Designates the system as the authoritative source for collections data, requiring accurate, real-time updates and robust audit logging.

**Requirement Count:** 21

| Req. ID | Requirement | MoSCoW Rating | Context | ITT Question Grouping |
|---|---|---|---|---|
| SoR.1 | The supplier shall ensure that the collection system shall be the system of record. | Must have |  | 2a |
| SoR.2 | The supplier shall ensure that the system shall have functionality to process payments through integration with DWP strategic tools. | Must have |  | 2a |
| SoR.3 | The supplier shall ensure that the system shall calculate interest accrual, for example, but not lited to: SMI with ability to calculate interest from date earlier than when the debt was loaded. | Must have |  | 2a |
| SoR.4 | The supplier should ensure that the system shall have the ability to manage the DCA/client billing and management including defining contractual agreements between the DCA and the contingent debt provider. For example, but not limited to: Whether credit card payments are allowed, if the DCA can use enrichment.<br>These terms should be applied at client level within the configuration, but with ability to adjust parameters for individual batches of accounts received. These rules should also feed into the activities and operation permitted of those accounts within the system. | Should have |  | 2a |
| SoR.5 | The supplier shall ensure that the system shall allow contractual models to be set up and configured in order to actively calculate fees, including flat fees, percentage rates and tiered fees. | Must have |  | 2a |
| SoR.6 | The supplier shall ensure that in the system it shall be possible for complex fee and commission structures to be set up through configurable management. | Must have |  | 2a |
| SoR.7 | The supplier shall ensure that the system shall have the functionality for linking individual customer debts for legal action to combine charges and fees exposed to the customer. | Must have |  | 2a |
| SoR.8 | The supplier could ensure that the system shall have true accounting functionality, designed to operate like a mini core billing system that can either interface directly with organisation's general ledger system, or in fact be used in this regard. | Should have |  | 2a |
| SoR.9 | The supplier could ensure that the system shall have standard integrations with general ledger solutions for maintaining/reconciling balances and accounts. | Should have |  | 2a |
| SoR.10 | The supplier shall ensure that the system shall have flexibility to configure transaction types and define how balances are made up through transaction types such as the main capital and interest, along with the operation of monthly accruals. | Must have |  | 2a |
| SoR.11 | The supplier shall ensure that the system shall have full reconciliation and audit capability across the whole financial and accounting elements. | Must have |  | 2a |
| SoR.12 | The supplier shall ensure that the system shall have full OOTB reconciliation functionality over all data aspects shared/transitioned with services and other organisations. | Must have |  | 2a |
| SoR.13 | The supplier shall ensure that the system shall have relevant controls in place for, but not limited to: data inaccuracies, data quality issues, and mismatches. | Must have |  | 2a |
| SoR.14 | The supplier should ensure that the system shall have recalculation capability for circumstances where balances need to be recalculated. | Should have |  | 2a |
| SoR.15 | The supplier could ensure that the system shall have parameterisation available to determine the days in which accounting elements are run. | Could have |  | 2a |
| SoR.16 | The supplier could ensure that the system shall have the ability to link the core accounting functionality to an invoice creation process for end customers in order to drive statements of debts and relevant legal notices. | Could have |  | 2a |
| SoR.17 | The supplier shall ensure that the system shall have appropriate segmenting of portfolios. | Must have |  | 2a |
| SoR.18 | The supplier shall ensure that the system shall map data files received from clients for purchased account portfolios to load the accounts into the system. | Must have |  | 2a |
| SoR.19 | The supplier shall ensure that the system shall have the ability for a user to review and amend any errors or exceptions (these should cover both process and data validation) from files loaded into the system. Once corrected records should progress automatically based on strategy decisioning. | Must have |  | 2a |
| SoR.20 | The supplier shall ensure that the system shall match and link new accounts loaded to existing customers based on configurable business rules. | Must have |  | 2a |
| SoR.21 | The supplier shall ensure that the system shall capture working days and pubic holidays to manage contact rules and payment processing rules. | Must have |  | 2a |

---

## 19. Change Processes

**Capability Group Description:** Facilitates controlled changes to business rules, workflows, or system parameters, typically via governance and change control processes.

**Requirement Count:** 11

| Req. ID | Requirement | MoSCoW Rating | Context | ITT Question Grouping |
|---|---|---|---|---|
| CP.1 | The supplier shall ensure that the system shall allow creation of development team to facilitate small, medium and large change. | Must have |  |  |
| CP.2 | The supplier could ensure that the system shall have multiple configuration and test environments set up. | Could have | This relates to having the ability to setup multiple configuration environments, typically organisations have one configuration environment, which deploys code to the remaining environments e.g. Training, UAT etc, however there are options to have multiple, which come with an overhead and maintenance.  NFR requirements have captured environment needs further. |  |
| CP.3 | The supplier should ensure that the system shall have the ability to support code promotion between environments (for example, but not limited to: from development environment to test environment) | Should have |  |  |
| CP.4 | The supplier shall ensure that the system shall have testing automation and features including, but not limited to: time travel, data anonymisation, save and restore. | Must have |  |  |
| CP.5 | The supplier shall ensure that the system shall have standard change control processes, documentation, procedures, protocols, and overall a runbook for change go lives and releases. | Must have |  |  |
| CP.6 | The supplier shall ensure that the system shall have automated change, defect or issue logging in place from a ticket raising, assignment, investigation, and resolution perspective. | Must have |  |  |
| CP.7 | The supplier shall ensure that the system shall have the ability to rollback to a previous release or state quickly and easily. | Must have |  |  |
| CP.8 | The supplier shall ensure that the system shall have the functionality to compare changes from one release to another to confirm what has changed. | Must have |  |  |
| CP.9 | The supplier shall ensure that the system shall maintain a regression test area of scripts that can be automatically run to confirm changes have not inadvertently impacted other areas of the system. | Must have |  |  |
| CP.10 | The supplier shall ensure that the system shall have automated release management via, for example, GitHub; peer-reviewed pull requests for quality assurance. | Must have |  |  |
| CP.11 | The supplier shall ensure that the system shall have the ability to selectively promote change, i.e. if two developers have introduced changes, only one developers changes are promoted to test/production environment. | Must have |  |  |

---

## 20. System Development & Roadmap

**Capability Group Description:** Refers to the approach for enhancing the system over time through releases, backlog management, and roadmap planning.

**Requirement Count:** 7

| Req. ID | Requirement | MoSCoW Rating | Context | ITT Question Grouping |
|---|---|---|---|---|
| SD.1 | The supplier shall ensure that systems development and roadmap should be influenced by industry trends. | Must have |  | 2c |
| SD.2 | The supplier shall ensure that there is roadmap clarity over the short, medium and longer term. | Must have |  | 2c |
| SD.3 | The supplier should ensure that the roadmap contains a mixture of issue resolution and new features or functionality. | Should have |  | 2c |
| SD.4 | The supplier should ensure thatupgrades must be planned and scheduled well in advance. | Should have |  | 2c |
| SD.5 | The supplier shall provide a process for carrying out upgrdaes that includes mechanisms and steps which are subject to the Authority's agreement, which deliver the upgrades with minimal business impact. | Must have |  | 2c |
| SD.6 | The supplier should introduce future technologies and methodologies to maintain currency, examples may include, but are not limited to: AI, ML, and cloud native. | Should have |  | 2c |
| SD.7 | The supplier shall ensure that the system shall provide a view of the standard planned upgrades, developments, and release cycles over the proposed contractual length. | Must have |  | 2c |

---

## 21. MP Requirements

**Capability Group Description:** Defines migration requirements and data migration considerations for moving legacy debt-management data into the future collections service.

### Migration Requirements

**Requirement Count:** 15

| Req. ID | Requirement | MoSCoW Rating | Context |
|---|---|---|---|
| MR1 | The solution must migrate all relevant data from the existing (previous) Debt Management systems<br>e.g.<br>• Debt Manager System<br>• CRU IT system<br>• Clerical Databases |  |  |
| MR2 | The solution must allow the business flexibility on how migration is approached |  |  |
| MR3 | The solution must be able to index archive data by reference number |  |  |
| MR4 | The solution must be able to migrate data within timescales specified by the business |  |  |
| MR5 | The solution must allow the user to view migrated data within timescales specified by the business |  |  |
| MR6 | The solution must be able to undertake migration outside normal Departmental working hours |  |  |
| MR7 | The solution must be able to bulk migrate data<br>e.g. 'big bang' / phased |  |  |
| MR8 | The solution must be able to indicate that the account and associated debts were migrated and show previous unique debt identifiers |  |  |
| MR9 | The solution must recognise the debt has been migrated and no longer continue to manage that debt on the previous system to avoid duplication |  |  |
| MR10 | The solution must allow for the ongoing management of the migrated debt |  |  |
| MR11 | The  solution must allow for specific migration data to be held  against migrated cases<br>e.g. Fields that are required now for legacy reasons but won't be required for new debt processing |  |  |
| MR12 | The solution must be able to manage and resolve failures during the Debt migration |  |  |
| MR13 | The solution must be able to report on each stage of the migration and highlight any failures that require corrective action |  |  |
| MR14 | The solution must be able to suppress trigger actions for migrated accounts<br>e.g. Duplicate letters, Payment collections , Direct Debits, Duplicate Financial GL Updates  when migrating/re-opening accounts |  |  |
| MR15 | The solution must allow manual input of migration reference data<br>e.g. NINo, migration indicator |  |  |

### Data Migration Considerations

| Data Domain | Data Element | Data Mastered In | Collections System Requirement | Integrations |
|---|---|---|---|---|
| Customer Information | Customer full name | Host System | Recipient Only | N/A |
| Customer Information | Date of birth | Host System | Recipient Only | N/A |
| Customer Information | Current address | Host System | Amendments Possible | Feedback to Host |
| Customer Information | Historical addresses | Host System | Recipient Only | N/A |
| Customer Information | Most recent customer contact information (email, phone numbers) | Host System | Amendments Possible | Feedback to Host |
| Customer Information | Historic customer contact information (email, phone numbers) | Host System | Recipient Only | N/A |
| Customer Information | Unique customer identifiers (e.g., Customer ID) | Host System | Recipient Only | N/A |
| Customer Information | Current employment details (employer name, role, and contact information) | Host System | Amendments Possible | Feedback to Host |
| Customer Information | Historic employment details (employer name, role, and contact information) | Host System | Recipient Only | N/A |
| Customer Information | Most recent vulnerability markers (e.g., mental health issues, financial distress) | Host System | Amendments Possible | Feedback to Host |
| Customer Information | Historic vulnerability markers (e.g., mental health issues, financial distress) | Host System | Recipient Only | N/A |
| Customer Information | Most recent income and expenditure assessment | Either Host or Collections System | Master / Amendments Possible | Feedback to Host |
| Customer Information | Historic income and expenditure assessment | Either Host or Collections System | Master or Recipient | N/A |
| Customer Information | Details of third parties and relationship to customer | Host System | Amendments Possible | Feedback to Host |
| Customer Information | Date markers for changes to customer information | Either Host or Collections System | Master or Recipient | N/A |
| Customer Information | Consent records (e.g. vulnerability data processing consent) | Either Host or Collections System | Master or Recipient | N/A |
| Customer Information | Prefer records (e.g. preferred communication channel) | Either Host or Collections System | Master / Amendments Possible | Feedback to Host |
| Debt Information | Original credit details | Host System | Recipient Only | N/A |
| Debt Information | Define role (e.g., primary borrower, guarantor) | Host System | Recipient Only | N/A |
| Debt Information | Link customers to one or multiple debts (e.g., joint loans) | Host System | Recipient Only | N/A |
| Debt Information | Track inherited or transferred debts | Host System | Recipient Only | N/A |
| Debt Information | Outstanding amount | Host System | Recipient Only | N/A |
| Debt Information | Account interest | Host System | Recipient Only | N/A |
| Debt Information | Account charges | Host System | Recipient Only | N/A |
| Debt Information | Payment schedules and amounts | Either Host or Collections System | Master / Amendments Possible | Feedback to Host |
| Debt Information | Date of account opening and default date | Host System | Recipient Only | N/A |
| Debt Information | Debt status (e.g., active, in dispute, written off) | Collections System | Master | Feedback to Host |
| Debt Information | Payment agreements or arrangements (e.g., payment plans, forbearance terms) | Either Host or Collections System | Master / Amendments Possible | Feedback to Host |
| Debt Information | Resolution outcomes (e.g., settlements, write-offs) | Collections System | Master | Feedback to Host |
| Debt Information | Recovery actions taken and timestamps (e.g., legal notices, repossessions) | Collections System | Master | Feedback to Host |
| Debt Information | Escalation details (e.g., third-party involvement, specialist recovery teams) | Collections System | Master | N/A |
| Debt Information | Associate related debts (e.g., debts consolidated into a single account). | Either Host or Collections System | Master / Recipient | N/A |
| Debt Information | Maintain records of partial or complete transfers to third parties. | Host System | Recipient Only | N/A |
| Transaction History | Payment history (dates, amounts, methods) | Host System | Recipient Only | N/A |
| Transaction History | Refunds or chargebacks | Host System | Recipient Only | N/A |
| Transaction History | Failed payments and retry attempts | Host System | Recipient Only | N/A |
| Communications | Communications log (e.g., SMS, emails, letters, phone calls) | Either Host or Collections System | Master / Amendments Possible | Feedback to Host / Data repository |
| Dispute Details | Dispute reasons (and supporting information / documentation) | Either Host or Collections System | Master / Amendments Possible | Feedback to Host |
| Dispute Details | Dispute resolution status and timestamps | Either Host or Collections System | Master / Amendments Possible | Feedback to Host |
| Complaint details | Complaint reasons (and supporting information / documentation) | Either Host or Collections System | Master / Amendments Possible | Feedback to Host |
| Complaint details | complaint resolution status and timestamps | Either Host or Collections System | Master / Amendments Possible | Feedback to Host |
