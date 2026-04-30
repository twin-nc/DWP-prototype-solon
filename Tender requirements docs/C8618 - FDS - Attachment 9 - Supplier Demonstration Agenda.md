# C8618 — FDS Attachment 9: Supplier Demonstration Agenda

> **Source:** DWP Future Debt Service ITT, Attachment 9
> **Purpose:** Structured evaluation day agenda — defines exactly what must be demonstrated live and maps to ITT question numbers and weightings.

---

## Schedule Overview

| Time | Item | Duration |
|---|---|---|
| 10:00 | Introductions and objective for the day | 10 mins |
| 10:10 | Introduction to the Supplier and the collections solution | 20 mins |
| 10:30 | **Demo 1: Integration** | 30 min demo + 15 min Q&A |
| 11:15 | Break | 15 mins |
| 11:30 | **Demo 2: System Capability (Part 1)** | 60 min demo + 30 min Q&A |
| 13:00 | Lunch | 45 mins |
| 13:45 | **Demo 2: System Capability (Part 2)** | 30 min demo + 20 min Q&A |
| 14:35 | **Demo 3: System Flexibility** | 45 min demo + 20 min Q&A |
| 15:40 | Break | 15 mins |
| 15:55 | **Demo 4: Deployment Facilitation** | 10 min demo + 10 min Q&A |
| 16:15 | Final Questions | 10 mins |
| 16:25 | Wrap Up | 5 mins |
| 16:30 | Close | |

---

## Demonstration 1 — Integration

**ITT Question:** 3a | **Weighting:** 15%

**Question:** How does the debt management solution facilitate integration with DWP and third-party systems?

### Evaluation Criteria
- **Integration Approach & Suitability** — Approaches, methodologies, tools, and techniques for a complex, evolving corporate IT landscape including DWP-specific business rules and processes
- **Integration Models & APIs** — Available integration models and methods, constraints, limitations, and range of RESTful APIs and documentation provided as standard
- **Tools & Features for Development** — Tools that reduce development timescales, enable integration without specialist technical support, and support integration testing, monitoring, and performance management
- **Security & Privacy** — Security and data privacy for data in transit and at rest; governance and access controls for integration settings, migration, and data management
- **Scalability & Resilience** — How integration scaling meets demand; throughput capacity, throttling, queuing, asynchronous processing, elasticity, and fault tolerance
- **Data Integrity & Error Management** — Data mapping, transformation, reconciliation; error detection and recovery; user notification of errors
- **Continuous Improvement** — Approach to ongoing improvement and long-term adaptability of integrations

### Demonstration Criteria (must be shown)
1. Flexible integration with a range of platforms (Host System, Communications Channels, Self Service Portal, Data Warehouse, Payment Channels) using:
   - Batch processes
   - Bi-directional APIs
   - Consumption of external events
   - Message queue technology
2. Integration models and methods, constraints and limitations
3. RESTful APIs — how they are exposed, documented, and consumed
4. Tools, features and methodologies that:
   - Reduce integration development timescales
   - Enable integration without specialist technical support
   - Support integration testing, monitoring and performance management
5. How debtors and debts are created manually and automatically using API and batch processes
6. Integration with a third-party payment platform to take payments compliant with all relevant regulations
7. How the system calls a Notifications API to trigger sending of a letter/SMS

---

## Demonstration 2 — System Capability

**ITT Question:** 2a | **Weighting:** 17.5% | **Total time:** 135 mins (suggested split: 90 + 45 mins)

**Question:** How does the debt management solution provide DWP with modern debt management capabilities?

### Evaluation Criteria
- **Debtor & Debt Data Management** — Single debtor view including debtor/debt relationship variations; creation, matching, exception management; search, retrieval, and data quality standards
- **Low Code / No Code Functionality** — Decisioning tools for assignment, treatment, and segmentation; strategy design, testing, and adaptation; workflows built, configured, and controlled at user/team level with minimal technical knowledge
- **Treatment & Strategy Execution** — Treatments scheduled, paused, adapted, or triggered by events including user-managed exceptions (e.g. Vulnerability); rapid improvement methods (A/B testing, simulations, scenario modelling); communications automated, sequenced, and tracked with full audit trail
- **Payment Processing** — Payment options available (single, partial, recurring, overpayment, refund, plans); creation/amendment/cancellation; scheduling and behaviours; manual/automated work allocation
- **Compliance & Control** — Manual work managed at debtor (customer) and debt (account) levels; tools to ensure compliance with policy, procedures, and regulation; changes controlled, traced, and audited
- **Performance Reporting** — Intraday operational reporting, analytics, and integration with DWP's strategic MI solution (Microsoft Power BI)

### Demonstration Criteria (must be shown)
1. Customer and account level view where a customer has multiple debts
2. Debtor, debt, and third-party relationships — establishment and display
3. Search functionality to retrieve customer and account information
4. Decisioning tools for assignment, treatment, and segmentation of debtors/debts
5. How strategies can be designed, tested, and adapted
6. Workflows built, configured, and controlled at user/team level with minimal technical knowledge
7. Creating a new collections strategy and amending an existing strategy
8. Communications (Letter, SMS, Email) set up in the system using a third-party platform that holds templates; how they are used within strategies
9. Communications automated, sequenced, and tracked across channels with full audit trail
10. System data used effectively in strategies (e.g. customer segmentation data)
11. **Champion/challenger strategies** configured to support continuous improvement
12. Agents writing/uploading free-format letters, emails, and SMS
13. Communications sent to multiple parties simultaneously (e.g. joint debtors / guarantors)
14. Treatments scheduled, paused, adapted, or triggered by events, including user-managed exceptions (e.g. Vulnerability)
15. Payment options available (single, partial, recurring, overpayment, refund, plans) — creation/amendment/cancellation
16. Repayment plan setup — flexibility on frequency, amounts, dates; at both customer and/or account level; distinct types of payment plans
17. Tolerances built within the system; how tolerance can be changed
18. System monitoring of a repayment plan
19. Work allocation and prioritisation between different teams and users
20. Access to work separated by entity (limiting specific entities in customer/account structure)
21. System ensures accounts worked at right time with right actions
22. User/role access — setting up a user, setting up and assigning roles
23. All system user actions audited (CRUD operations)
24. User actions restricted or limited by workflow
25. System configured to report against built-in KPIs and SLAs; users generate and view reporting on worklists (worked/non-worked accounts, volumes by criteria)

---

## Demonstration 3 — System Flexibility

**ITT Question:** 2b | **Weighting:** 17.5% | **Total time:** 60 mins (suggested split: 45 + 15 mins)

**Question:** How does the debt management solution provide DWP with flexibility to manage unique and complex debt management processes?

### Evaluation Criteria
- **System Entity Alignment** — How core system entities, data fields, and relationships between entities meet DWP needs; presentation in the UI
- **Configuration & Extension** — Extent to which DWP can configure and extend the data model; constraints; potential impacts on performance or reporting
- **User Interface & Data Presentation** — How data and interface can be configured to meet DWP needs
- **Debt Management Process Support** — How the solution can be configured to support DWP-specific debt management processes
- **Payment Allocation** — How payments are allocated to debtors and debts; allocation rules configured; interface with custom DWP systems for benefit deductions; dependencies, constraints, and limits
- **Payment Channels & Collection** — Low-friction payment across all channels (online, phone, bank transfer, DEA); Direct Debit collection managed end-to-end including automation

### Demonstration Criteria (must be shown)
1. **Household** — create/display a Household (joint debt with two people on the debt)
2. Data structure underpinning the Household — display
3. Query the data structure to retrieve debts relating to a Household
4. Query the data structure to retrieve joint debts in the context of either person in the Household
5. **Joint debt split** — manually and automatically split a joint Household debt into two equal-value debts
6. Residual value after split (1p) written off without manual intervention
7. Capture, classify, and process customer data: **benefit status (On Benefit / Off Benefit)**, account information such as **Fraud Flag**
8. **Customer vulnerability** — configure vulnerability levels; identify and manage customers through the process; date of capture, type, temporary vulnerabilities
9. Other customer segments and DWP processing data capture examples
10. Setting up a new communication template (letter/SMS/email) to interface with a third-party communication delivery platform that holds templates
11. Information accessible to Agents and tailored to their needs
12. **Agent alerts** — displayed with key information or specific actions required/restricted; how this is configured
13. Transaction visibility/accessibility to Agents
14. Customers in financial difficulty — management in the Collections System
15. **Affordability** — income and expenditure information captured; configuration; influence on treatment options
16. Most appropriate forbearance solutions presented to the Agent
17. Appropriateness of forbearance plan tracked (Agent selects most appropriate plan)
18. Agent free-format letter/email/SMS upload
19. Payment allocation when a payment plan is in place
20. Third-party system used to allocate a payment to a specific customer and debt
21. Audit trail of the transaction
22. Creation of **Direct Debit** and monitoring of payment

---

## Demonstration 4 — Deployment Facilitation

**ITT Question:** 3b | **Weighting:** 10% | **Total time:** 15 mins

**Question:** How will rapid deployment of a secure debt management solution be facilitated?

### Evaluation Criteria
- **Architecture & Deployment** — Solution architecture meeting DWP performance/security requirements; CI/CD support; configuration releases with version control
- **Configuration Management & Collaboration** — Methods/tooling for parallel development teams, merge conflict management, selective deployment of release-ready configurations, Supplier update communication
- **Release & Rollback Controls** — Stakeholder communication for updates/releases; rollout control of new Supplier features; release failure protocols; rollback procedures (DWP and Supplier)
- **Change & Risk Management** — Change impact assessments; risk and issue communication; planned updates, patches, proactive monitoring
- **Security & Compliance** — Positive security posture; industry certifications; tools/features to protect customer/account data; immutable audit information
- **Security Operations** — Risk management, continuous monitoring, incident response; threat detection; breach containment and remediation
- **Business Continuity** — Data backup and disaster recovery; data safety, recoverability, and availability

### Demonstration Criteria (must be shown)
1. Developers/teams can work in parallel; merge conflicts managed; release-ready configurations selectively deployed
2. Methods for controlling rollout of new Supplier product features

---

## Scoring Summary

| Demo | Question | Weighting | Time Allocated |
|---|---|---|---|
| Demo 1: Integration | 3a | **15%** | 45 mins |
| Demo 2: System Capability | 2a | **17.5%** | 135 mins |
| Demo 3: System Flexibility | 2b | **17.5%** | 60 mins |
| Demo 4: Deployment Facilitation | 3b | **10%** | 15 mins |
| **Total demonstrable** | | **60%** | **255 mins** |

---

## DWP-Specific Requirements Identified (Critical for Demo Preparation)

These are DWP-unique items not found in standard debt management platforms:

| Item | Demo | Priority |
|---|---|---|
| **Household concept** — joint debt linking multiple debtors | Demo 3 | Critical |
| **Joint debt split** — automatic equal split + residual write-off | Demo 3 | Critical |
| **Benefit status** (On Benefit / Off Benefit) as a data field | Demo 3 | Critical |
| **Fraud Flag** as an account-level data field | Demo 3 | High |
| **Customer vulnerability** with levels, date, type, temporary status | Demos 2 & 3 | Critical |
| **Affordability** — income & expenditure capture influencing treatment | Demo 3 | Critical |
| **DEA (Direct Earnings Attachment)** as a payment channel | Demo 3 | Critical |
| **Champion/challenger strategies** | Demo 2 | High |
| **A/B testing, simulations, scenario modelling** | Demo 2 | High |
| **Third-party communication template platform** integration | Demos 1, 2 & 3 | High |
| **Microsoft Power BI** integration for strategic MI | Demo 2 | High |
| **Notifications API** for letter/SMS triggering | Demo 1 | High |
| **Forbearance appropriateness tracking** | Demo 3 | Medium |
| **Agent alerts** with configured required/restricted actions | Demo 3 | Medium |
