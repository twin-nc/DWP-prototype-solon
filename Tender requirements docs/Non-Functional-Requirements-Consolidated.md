# Non-Functional Requirements — Consolidated
**Source:** C8618 - FDS - Attachment 4b - Non Functional Requirements.xlsx

---

## Table of Contents
1. [Glossary](#1-glossary)
2. [Instructions & MoSCoW Definitions](#2-instructions--moscow-definitions)
3. [Non-Functional Requirements](#3-non-functional-requirements)

---

## 1. Glossary

| Acronym | Term | Definition |
|---|---|---|
| API | Application Programming Interface | An application programming interface (API) is a piece of software that lets one program access or control another program. APIs allow applications to share data without requiring developers to share software code. |
| BCDR | Business Continuity and Disaster Recovery | It encompasses the strategies, policies, and procedures organizations put in place to ensure they can continue operating during and after a disruptive event. |
| CCA | Consumer Credit Act | The Consumer Credit Act (CCA) 1974 is a UK law that regulates the provision of credit and hire agreements to consumers. |
| FTPS | File Transfer Protocol Secure | It is a secure extension of the widely used File Transfer Protocol (FTP), adding support for the Transport Layer Security (TLS) and, formerly, Secure Sockets Layer (SSL) cryptographic protocols to encrypt the connection. |
| GDPR | General Data Protection Regulations | It is a UK law that regulates how organizations can collect, use, and protect the personal data of individuals within the UK. |
| ITIL | Information Technology Infrastructure Library | A widely adopted framework of best practices for IT service management. |
| MPLS | Multiprotocol Label Switching | It is a method for setting up dedicated paths across networks without relying on the typical routing process. |
| OAuth 2.0 | Open Authorization | It is an open standard for authorization that allows third-party applications to access resources on behalf of a user without needing to know the user's credentials. |
| RPO | Recovery Point Objective | It's the point in time to which systems and data can be restored after a disruption or outage. |
| RTO | Recovery Time Objective | It refers to the maximum acceptable time it takes to restore a business process or system after an outage or disaster. |
| SOC1 | System and Organization Controls 1 | It's a type of report issued by an independent auditor to evaluate a service organization's internal controls related to financial reporting. |
| SOC2 | System and Organization Controls 2 | It is a framework developed by the AICPA to assess how service organizations manage customer data based on five "trust service principles": security, availability, processing integrity, confidentiality, and privacy. |
| TLS 2.0 | Transport Layer Security | It is a widely used security protocol that encrypts data transmitted over networks, ensuring secure communication between devices and applications. |
| UI | User Interface | The user interface (UI) is the point of human-computer interaction and communication in a device. |
| WCAG | Web Content Accessibility Guidelines | Set of internationally recognized technical standards developed by the World Wide Web Consortium (W3C) to improve the accessibility of web content for people with disabilities. |

---

## 2. Instructions & MoSCoW Definitions

### About This Document

The Authority has legal and regulatory obligations to verify that the suppliers it works with have reasonable standards in place to protect Authority data, assets and the rights of individuals. These include the Public Sector Bodies (Websites and Mobile Applications) (No.2) Accessibility Regulations 2018 and the Equality Act 2010.

The Authority requires assurance of the proposed service or solution in relation to the requirements set out in this Non-Functional Requirements document. The Authority will gather and review questionnaire responses and supporting comments to understand the technical capabilities of the Supplier and how the proposed service or solution aligns with the Authority's legal obligations.

### Supplier Response Guidance

In the Non-Functional Requirements tab, suppliers should use the drop-down list in the column marked "COTS Supplier Meets Requirements" and provide supporting comments in the column marked "COTS Supplier Comments". References to any additional supplied documents (e.g. certifications) should be included in the comments column.

| Response Value | Meaning |
|---|---|
| **Yes** | Everything requested in the requirement is provided by the proposed service or solution. |
| **No** | There are gaps in the proposed service or solution compared with the requirement, or the requirement is not satisfied. Suppliers should specify the gap and may suggest mitigations. |

### MoSCoW Rating Definitions

| Rating | Definition |
|---|---|
| **Must have** | Critical to the current delivery timebox. If even one Must have requirement is not included, the project delivery should be considered a failure. MUST can also be considered an acronym for the Minimum Usable Subset. |
| **Should have** | Important but not necessary for delivery in the current delivery timebox. Often not as time-critical as Must have, or there may be another way to satisfy the requirement, so that it can be held back until a future delivery timebox. |
| **Could have** | Desirable but not necessary; could improve user experience or customer satisfaction for little development cost. Typically included if time and resources permit. |
| **Won't have (this time)** | Agreed by stakeholders as the least-critical or lowest-payback items, or not appropriate at that time. Not planned into the schedule for the next delivery timebox. |

---

## 3. Non-Functional Requirements

The requirements below are grouped by capability area. The ITT Question Grouping column references the relevant ITT section (3a = Non-Functional; 3b = Non-Functional Performance).

### Accessibility (ACC)

| ID | Requirement | MoSCoW | ITT Grouping |
|---|---|---|---|
| ACC01 | The Supplier shall ensure that the solution complies with the Public Sector Bodies (Websites and Mobile Applications) (No. 2) Accessibility Regulations 2018, which includes conformance with the published version of the Web Content Accessibility Guidelines (WCAG) to AA level, and the publication of a valid accessibility statement. | Must | 3a |
| ACC02 | The Supplier shall ensure that the System(s) Solution works with common assistive technologies and must function properly with each of the following three types of assistive software: (a) Screen reader (such as JAWS, NVDA, Voiceover or Narrator); (b) Screen magnifier (such as ZoomText, Apple Zoom or Windows Magnifier); (c) Voice recognition application (such as Dragon, VoiceControl or Windows Speech). | Must | 3a |
| ACC03 | The Supplier shall ensure that the System(s) Solution does not contravene the Equality Act 2010 in any way. | Must | 3a |

### Availability (AVA)

| ID | Requirement | MoSCoW | ITT Grouping |
|---|---|---|---|
| AVA01 | The Supplier shall ensure that the production System(s) Solution is available for use 24 hours a day, 7 days a week (except for any contractually allowed non-emergency downtime/scheduled maintenance) to achieve service availability of at least 99.9%. | Must | 3b |
| AVA02 | The Supplier shall ensure that the production System(s) Solution is available for use (office hours), Monday to Friday between 0730 and 2100 and Saturday between 0730 and 1730 UK time. The system should be available on Sundays when agents work on an ad hoc (over time) basis. | Must | 3b |
| AVA03 | The supplier shall ensure that the solution provides high levels of availability to achieve the required availability and resilience. | Must | 3b |

### Compliance (COM)

| ID | Requirement | MoSCoW | ITT Grouping |
|---|---|---|---|
| COM01 | The Supplier shall demonstrate appropriate procedures have been implemented to ensure compliance with legislative, regulatory and contractual requirements related to intellectual property rights and use of proprietary software products. | Must | 3a |
| COM02 | The Supplier shall ensure that the System(s) Solution is UK Data Protection Act 2018 / General Data Protection Regulation (GDPR) compliant by design, referred to by the UK Information Commissioner's Office (ICO) as "data protection by design and default". | Must | 3a |
| COM03 | The Supplier shall ensure that records within the System(s) Solution are protected from loss, destruction, falsification, unauthorised access and unauthorised release. | Must | 3a |
| COM05 | The Supplier shall ensure that the System(s) Solution's non-production environments provide the option to scrub, mask, or similar to the personal data transferred into it. | Must | 3a |
| COM06 | The Supplier shall ensure that the audit functionality within the System(s) Solution records details of all of the following actions / transactions: Create, Read, Update, Delete. The records are made available to the Authority. | Must | 3a |
| COM07 | The Supplier shall ensure that all audited actions / transactions are capable of being passed to a third party Authority auditing solution, in near real time, including the following identifications: GUID/user ID, time/date stamp of action / transaction, MAC address, IP address, Browser type, and Session ID. | Must | 3a |
| COM08 | The Supplier should comply with SSAE18 or equivalent standard. Please provide SOC 1 and SOC 2 report or equivalent as evidence. | Should | 3a |
| COM09 | The Supplier shall provide all necessary licenses required by the Authority and any other End User of the System(s) Solution in order to access the System(s) Solution. | Must | 3a |
| COM10 | The Supplier shall comply with all relevant aspects of the Government Digital Service Open Standards (www.gov.uk/government/publications/open-standards-for-government) and detail how compliance will be achieved, if bespoke software development is provided. | Should | 3a |
| COM11 | The Supplier shall work with the Authority to adhere to the Authority's digital governance processes, including: (a) gaining approval of the System(s) Solution at the Authority's Digital Design Authority; (b) completion and sign off of Authority security risk assessments; (c) completion and sign off of Authority accessibility compliance assessment; (d) completion and sign off of an Authority Data Protection Impact Assessment (DPIA); and (e) completion and sign off of a Service Assessment. | Must | 3a |
| COM12 | The Supplier shall ensure that the System(s) Solution complies with CCA (Consumer Credit Act). | Could | 3a |
| COM13 | The Supplier shall ensure that the solution always complies with the published version of the Web Content Accessibility Guidelines (WCAG). | Must | 3a |

### Configuration (CON)

| ID | Requirement | MoSCoW | ITT Grouping |
|---|---|---|---|
| CON01 | The Authority has an expectation that there is no customisation required to meet the Authority's specific needs as detailed in the Functional System Requirements, and if customisation is needed to meet these Requirements, that it be minimal and only with the Authority's Approval. | Must | 3a |

### Data Controller (DC)

| ID | Requirement | MoSCoW | ITT Grouping |
|---|---|---|---|
| DC01 | Where the Supplier Processes any Personal Data (either on behalf of the Authority or for its own purposes as a Data Controller), the Supplier shall comply at all times with the Data Protection Legislation. | Must | 3a |
| DC02 | The Supplier shall notify the Authority within 2 hours if they become aware of, or suspects, a personal data breach or other data security breach and shall assist the Authority (as reasonably required) in any action to be taken by the Authority in relation to such breach(es). | Must | 3a |

### Data Migration (DM)

| ID | Requirement | MoSCoW | ITT Grouping |
|---|---|---|---|
| DM01 | The Supplier shall ensure that the System(s) Solution provides a data load tool or API sufficient to support bulk and case by case data migration into and out of the System(s) Solution. | Must | 3a |

### General (GEN)

| ID | Requirement | MoSCoW | ITT Grouping |
|---|---|---|---|
| GEN01 | If Microsoft Office components/apps are leveraged, the System(s) Solution shall be compatible with Microsoft 365. | Must | 3a |

### Interoperability (INT)

| ID | Requirement | MoSCoW | ITT Grouping |
|---|---|---|---|
| INT01 | The Supplier shall ensure that the System(s) Solution supports and implements single sign on for Authority users via OAuth 2.0 (with multiple identity providers). | Must | 3a |
| INT02 | The Supplier shall ensure that the System(s) Solution supports and implements single sign on for non-Authority users via OAuth 2.0 (with multiple identity providers). | Should | 3a |
| INT03 | The Supplier shall ensure that the System(s) Solution supports the OpenID Connect protocol. | Should | 3a |
| INT04 | The Supplier shall ensure that the System(s) Solution ensures all data in transit is encrypted at the application layer in such a way as to meet DWP's control and encryption standards even when the data flows across the Internet. The system should not rely on the underlying network transport to perform encryption. | Must | 3a |
| INT05 | The Supplier shall ensure that the System(s) Solution is configured to send all communications traffic solely via HTTPS. | Must | 3a |
| INT06 | The Supplier shall provide a set of data extraction processes as part of the System(s) Solution to support the transfer of data from the System(s) Solution to the DWP Data Integration Platform. Unless otherwise Approved by the Authority, the extracts are made on a daily basis or such other interval as the parties may agree. | Must | 3a |
| INT07 | To support the transfer of data from the System(s) Solution to the Data Integration Platform, the Supplier shall ensure: (a) message-based interfaces in XML/JSON format; (b) the Supplier provides metadata that describes the data contained within all the data extracts (data feeds) created by the Supplier for the Authority — this metadata is provided as uploadable data and conforms to the Authority's specific standards in respect of naming conventions; (c) each data extract is either batch file or message-based, and batch files over 2GB are separated and assigned sequenced file names; (d) for each data extract file, an accompanying SHA 256 is generated, and the file is encrypted for transfer using PGP. | Must | 3a |
| INT08 | The Supplier shall ensure that the System(s) Solution can be accessed by and supported on devices operating on Windows 10+, macOS v13+, iOS v17+, Android v12+. | Must | 3a |
| INT09 | The Supplier shall ensure that the System(s) Solution does not rely on any client-side plugins. JavaScript, Cookies and 3rd-party cookies are enabled. | Must | 3a |
| INT10 | The Supplier shall ensure that the System(s) Solution uses a responsive design. The System(s) Solution presents on any screen size with minimal scrolling. | Must | 3a |
| INT11 | The Supplier shall ensure that the System(s) Solution provides end-points using REST API that permits bidirectional transfer of data. | Must | 3a |
| INT12 | The Supplier shall ensure that the System(s) Solution provides the ability for bi-directional file based integrations using FTPS. | Must | 3a |
| INT13 | The Supplier shall ensure that the System shall support FTPS transfers using explicit passive mode. | Must | 3a |
| INT14 | The Supplier shall ensure that the System shall support FTPS transfer requirement for a response back on the same external IP address provided to DWP (i.e. no Network Address Translation). | Must | 3a |
| INT15 | The Supplier shall ensure that FTPS transfer will be pushed. | Should | 3a |
| INT16 | The Supplier shall ensure that FTPS transfer will be pulled. | Could | 3a |
| INT17 | The Supplier shall ensure that FTPS transfer will be encrypted using TLS 1.2. | Must | 3a |
| INT18 | The Supplier shall ensure that FTPS transfer will be externally signed with a SHA-2 certificate. | Must | 3a |
| INT19 | FTPS transfer requires a certificate from an approved Certificate Authority list, including: Digicert, Comodo/AddTrust, Verisign/Symantec, GoDaddy, Thawte, QuoVadis, DWP Root CA, and Entrust. | Must | 3a |
| INT20 | The Supplier shall ensure that FTPS transfers will use a recognised cipher suite, including: TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384, TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256, TLS_DHE_RSA_WITH_AES_256_GCM_SHA384, TLS_DHE_RSA_WITH_AES_128_GCM_SHA256, TLS_RSA_WITH_AES_256_GCM_SHA384, TLS_RSA_WITH_AES_128_GCM_SHA256. | Must | 3a |
| INT21 | The Supplier shall ensure that files for pick up/drop off will be in the default folder available upon connection. | Must | 3a |
| INT22 | The Supplier could ensure that SFTP protocol is available where FTPS cannot be supported. | Could | 3a |
| INT23 | The Supplier shall ensure that files transferred from DWP will be encrypted using PGP and must not be stored on a system in an unencrypted state. | Must | 3a |
| INT24 | DWP will initiate the transfer of data from DWP to the Supplier system. | Must | 3a |
| INT25 | DWP will initiate the transfer of data from a Supplier system to DWP. | Must | 3a |
| INT26 | The Supplier shall provide an appropriate SFTP/FTPS server to connect to. | Must | 3a |
| INT27 | The Supplier should ensure that accounts on the system should be automatically provisioned with default user rights if logging on using Single Sign On service. | Should | 3a |
| INT28 | The Supplier shall ensure that user authentication is possible across multiple domains (e.g. dwp.gov.uk, 3rdparty1.com, 3rdparty2.com) with a minimum of 100 domains supported. | Must | 3a |
| INT29 | The Supplier shall ensure that communication from a client application to a back-end server is through HTTPS. | Must | 3a |
| INT30 | The Supplier could ensure that communication with DWP systems follows a loosely coupled architecture using secure web services for non-batch transactions. | Could | 3a |
| INT31 | The Supplier could ensure that traffic between the Supplier and DWP is able to operate through multiple NAT translations. | Could | 3a |
| INT32 | The Supplier could ensure any DNS records are available to DWP through the PDNS service. | Could | 3a |
| INT33 | If the Supplier requires a private network hand off, it is the responsibility of the Supplier to provision all network connectivity including cross connects up to the DWP network points of presence. | Could | 3a |
| INT34 | The supplier should not assume the use of existing DWP private network connectivity services, such as MPLS or PSN. | Could | 3a |

### Mobile Application/Access (MOB)

| ID | Requirement | MoSCoW | ITT Grouping |
|---|---|---|---|
| MOB01 | The Supplier shall ensure that any mobile connectivity/application proposed by the supplier ensures that any such application will protect any personal or user data that is stored on the device. | Must | 3a |
| MOB02 | The Supplier will ensure that any such application (referenced in MOB01) will ensure that the user's access permissions are consistent to that assigned, i.e. the user only sees data they are authorised to access. | Must | 3a |
| MOB03 | The Supplier shall ensure that the System(s) Solution allows the Authority to choose which elements of functionality are available via a user's mobile device, which the Supplier acknowledges may be different dependant on the user type. | Must | 3a |
| MOB04 | The Supplier shall ensure that access to the mobile application can be restricted by user type. | Must | 3a |
| MOB05 | The Supplier shall ensure that the mobile application or its equivalent is available from a public app store. | Must | 3a |
| MOB06 | The Supplier should ensure that the mobile application or its equivalent is available from a private app store. | Should | 3a |
| MOB07 | The Supplier shall ensure that the mobile application supports an additional level of authentication (such as a PIN or similar) which is prompted for on each use of the application and following a short period of inactivity. | Must | 3a |
| MOB08 | The Supplier shall ensure that the System(s) supports an additional level of user authentication provided to the Supplier by the Authority to secure the application. | Must | 3a |
| MOB09 | The Supplier shall ensure that the System(s) does not transmit or share information about itself or usage back to the Supplier or any other 3rd party, without the Authority's Approval (e.g. location, usage info, version, error reporting). | Must | 3a |
| MOB10 | The Supplier shall ensure that the access to the System(s) Solution from a personal device or computer does not force the user to have any additional security software, including a Mobile Device Management (MDM) or equivalent deployed to their device. | Must | 3a |
| MOB11 | The Supplier shall ensure that the System does not use SSL Pinning, so that traffic can be inspected. | Must | 3a |
| MOB12 | The Supplier shall ensure that the System does use Application Transport Security. | Must | 3a |
| MOB13 | The Supplier shall ensure that the System does use Standard Operating System encryption methods. | Must | 3a |

### Operability (OPP)

| ID | Requirement | MoSCoW | ITT Grouping |
|---|---|---|---|
| OPP01 | The Supplier shall ensure that no part of the System(s) Solution is located/hosted outside of the UK. All support staff should be in the UK with relevant vetting unless agreed otherwise. | Must | 3a |
| OPP02 | In addition to the locations of the Supplier's datacentres, the Supplier shall disclose information on any location where data is stored and processed and where they manage the service from. | Must | 3a |
| OPP03 | The Supplier shall not relocate the data to an alternative datacentre without the Approval of the Authority. | Must | 3a |
| OPP04 | The Supplier shall ensure that all views, reports and analytics within the System(s) Solution is based on live data, reflecting the current position at the point the report was initially viewed/generated. | Must | 3a |
| OPP05 | The Supplier shall ensure that the System(s) Solution is available as a Software as a Service (SaaS). The Authority is not responsible for managing or controlling the underlying infrastructure including network, servers, operating systems, storage and/or individual application capabilities, save for any user specific application configuration settings. | Must | 3a |
| OPP06 | The Supplier shall ensure that a minimum of one production environment is provided. | Must | 3a |
| OPP07 | The Supplier shall ensure that a minimum of 3 non-production (DEV, UAT, Training, Staging) environments are provided. | Must | 3a |
| OPP08 | The Supplier should provision an environment that supports the effective training, learning and onboarding experience for users. | Should | 3a |

### Performance & Capacity (P&C)

| ID | Requirement | MoSCoW | ITT Grouping |
|---|---|---|---|
| P&C01 | The Supplier should ensure that the System(s) Solution provides reporting on how long it takes key transactions to action within the System(s) Solution. | Should | 3b |
| P&C02 | The Supplier should outline the expected response times for its solution and take proactive steps when performance is impacted; 90th percentile system response time should be between 1–3 seconds, including for navigation between features and page transitions, except where explicitly agreed. | Should | 3b |
| P&C03 | The Supplier shall ensure that the System(s) Solution's architecture supports the Authority's large user base with 'architectural elegance' and without undue 'architectural complexity'. | Must | 3b |
| P&C04 | The Supplier shall ensure that all application / system / service components are performant. This includes (but is not limited to) the user interface, data processing and storage, middleware service components, APIs, batch, messaging and queues. | Must | 3b |
| P&C05 | The Supplier could document and provide to DWP a traffic matrix showing the expected volumes of network traffic that will be transmitted and received between Supplier and DWP systems and protocols in use. | Could | 3b |

### Portability (POR)

| ID | Requirement | MoSCoW | ITT Grouping |
|---|---|---|---|
| POR01 | The System(s) Solution is capable of exporting all data in a format that will enable data portability of all data between the Supplier and the Authority. | Should | 3a |
| POR02 | The Supplier shall ensure at the end of the Term that all data relating to the Authority's business and operations will be returned to the Authority and all copies held by the Supplier within the System(s) Solution systems are properly, securely, and permanently deleted. The Supplier provides evidence of disposal where the Authority so requires. | Must | 3a |
| POR03 | The Authority owns all data relating to the Authority that is stored on the System(s) Solution. | Must | 3a |

### Recoverability (REC)

| ID | Requirement | MoSCoW | ITT Grouping |
|---|---|---|---|
| REC01 | The Supplier shall ensure that the System(s) Solution is capable of having a Recovery Point Objective (RPO) of 2 hours. | Must | 3a |
| REC02 | The Supplier shall ensure that the System(s) Solution includes a comprehensive disaster recovery plan and is compliant with and satisfies the Business Continuity/Disaster Recovery (BCDR) Plan. | Must | 3a |
| REC03 | The Supplier shall clarify if and how system components can "fail safe" (e.g. self-recover from a server reboot, dependent component failure such as a queue process) and provide evidence of the System(s) Solution's backup & Disaster Recovery (DR) testing procedures. The supplier shall take part in regular BC/DR tests. | Must | 3a |
| REC04 | The Supplier shall ensure that the System(s) Solution is capable of having a Recovery Time Objective (RTO) of 4 hours. | Must | 3a |
| REC05 | The Supplier shall be able to generate a reconciliation report that contains all details of any event that was sent to / received from an external integration (inbound and outbound) up to the time that the solution failed. This requirement is outside of the published Recovery Point Objective (RPO). | Must | 3a |
| REC06 | The Supplier shall notify the Buyer of incidents, outages and disaster events within 30 minutes of detection. | Must | 3a |

### Reliability (REL)

| ID | Requirement | MoSCoW | ITT Grouping |
|---|---|---|---|
| REL01 | The Supplier shall ensure that support can be provided over the telephone in the event of system outage. | Must | 3b |

### Scalability (SCA)

| ID | Requirement | MoSCoW | ITT Grouping |
|---|---|---|---|
| SCA01 | The Supplier shall ensure that the System(s) Solution is architected to provide horizontal expansion and contraction, so that resources can be increased and decreased depending on demand. The Supplier shall explain: (a) how the System(s) Solution design enables scalability (e.g. horizontal and vertical scalability); (b) examples and evidence of how performance increases horizontally; (c) clear indication of any solution components that can only scale vertically; and (d) application and integration scalability, in particular managing periods of heavy usage (month end expense runs and performance management tasks, daily login). | Must | 3b |
| SCA02 | The Supplier shall ensure that the System(s) Solution is capable of supporting 4,000 concurrent user sessions and theoretically capable of supporting 5,000 concurrent sessions for short periods. The Supplier shall demonstrate: (a) examples and evidence of system performance including load, stress, performance and robustness testing evidence; (b) any limiting factors that could cause latency issues outside of the supplier's control; (c) how the component(s) design mitigates poor response times (e.g. data caching); and (d) platform performance availability enables the Supplier to meet any applicable KPI. | Must | 3b |
| SCA03 | The Supplier shall have an operational reference client for the System(s) Solution architecture to be used at the Authority (i.e. of similar size and complexity), and the Supplier shall ensure that neither the Authority nor the System(s) Solution is a test-bed for operationally untested architecture. | Must | 3b |
| SCA04 | The Supplier shall ensure that at all times, all data in the System(s) Solution is in a consistent state and all reporting / responses are based on the committed data. | Must | 3b |

### Security (SEC)

| ID | Requirement | MoSCoW | ITT Grouping |
|---|---|---|---|
| SEC01 | As part of the System(s) Solution, the Supplier shall provide a security incident plan ("Security Incident Plan"). | Must | 3b |
| SEC02 | If at any time the Supplier suspects or has reason to believe Authority data or personal data has or may become corrupted, lost or sufficiently degraded in any way for any reason, then the Supplier shall notify the Authority immediately (and within 2 hours of identification) and inform the Authority of the remedial action the Supplier proposes to take. | Must | 3b |
| SEC03 | The Supplier shall ensure that any certificates and accreditations are maintained, and conducts relevant security tests including penetration testing. As soon as practicable, the Supplier shall provide the Authority the resultant certificate and/or audit report with actions taken. | Must | 3b |
| SEC04 | The Supplier shall clarify how their solution supports a Bring Your Own Key (BYOK) encryption facility to ensure DWP data is encrypted at rest using DWP encryption keys. | Must | 3b |
| SEC05 | The Supplier shall perform Security patching to comply with DWP's Security Patching Standards. | Must | 3b |

### Service (SER)

| ID | Requirement | MoSCoW | ITT Grouping |
|---|---|---|---|
| SER01 | The Supplier shall ensure that its service management approach aligns with DWP's Service Management tooling. The System(s) Solution shall be underpinned by a support model and tooling that provides support to the Authority to resolve queries, incidents and problems. Incident resolution service levels are at least: (a) Priority 1 Incidents: within 2 hours, 24/7; (b) Priority 2 Incidents: within 8 hours, 24/7; (c) Priority 3 Incidents: within 2 Working Days (20 hours), Monday to Friday 8am–6pm; (d) Priority 4 Incidents: within 3 Working Days (30 hours), Monday to Friday 8am–6pm. Priority definitions — P1: complete failure of a Business Critical Application/Service or inaccurate production of Business Critical outputs; P2: complete failure affecting all users at one or more Key Sites; P3: partial failure of a Business Critical Application/Service, complete failure at secondary sites, or failure affecting multiple sites; P4: partial failure of a Secondary Application/Service at a single secondary site, or any other incident not classified as P1–P3. | Must | 3b |
| SER02 | The Supplier shall provide the Authority with all service delivery documentation no later than ten (10) Working Days before the Operational Services Commencement Date and thereafter within one (1) Working Day of Authority request. The Delivery Documentation shall include: (a) details of the System(s) Solution and technology used; (b) details of procedures and processes used by Supplier Personnel; (c) mechanisms used to measure the service levels defined in Schedule 2.2; and (d) measures taken to protect Authority Data and any Personal Data. The Supplier maintains the Delivery Documentation and issues an updated copy within five (5) Working Days of any material change. | Must | 3b |
| SER03 | The Supplier shall provide and maintain all detailed architectural documentation of its System(s) Solution which clearly demonstrates how the Supplier Solution meets the Authority's Requirements. | Must | 3b |
| SER04 | All components of the System(s) Solution shall be kept updated by the Supplier and within the respective software/hardware Supplier's support for the Term. | Must | 3b |
| SER05 | All System user accounts and allocation are required to be managed as instructed by the Authority. The Supplier shall obtain approval from the Authority for all requests for non-Authority new user accounts. | Must | 3b |
| SER06 | The Supplier shall manage and control changes to the System(s) Solution in order to minimise the risk of disruption to any part of the Services. | Must | 3a |
| SER07 | The Supplier shall immediately report to the Authority any System failures that will impact the delivery of the Services provided to either the Authority in accordance with all agreed Functional System Requirements. | Must | 3b |
| SER08 | The Supplier on request from the Authority shall provide visibility of maintenance and service management documentation within one (1) Working Day, including: (a) maintenance policy, processes, plans; (b) activity logs, issue and risk logs, incident logs and problem logs; (c) defect logs; and (d) lessons-learned logs. | Must | 3b |
| SER09 | The Supplier shall provide monthly service performance management information and exception reports, including details of: (a) Availability of the Supplier System(s); (b) System Responsiveness; (c) number of minor and major degradations of service; (d) System Recovery Times; (e) recovery times; (f) diagnostics and lessons-learned; (g) failures in regulatory and/or security compliance; (h) data integrity; and (i) actual vs target compliance with SLAs/KPIs. | Must | 3b |
| SER10 | The Supplier shall provide a named Service Management contact to work directly with the Customer's Senior Service Manager with a designated telephone number and e-mail address for all communication. Any change to these details shall be notified to the Authority in writing a minimum of three (3) Working Days in advance. The Supplier shall provide a named deputy when the main contact is not available. | Must | 3b |
| SER11 | The Supplier shall ensure that the Solution does not block the normal upgrade path of the software Supplier's product due to Supplier applied configuration changes within the Best Practice guidelines of the product, and any changes are preserved in the event of any form of upgrade path or patching requirement, without need for significant effort to protect such changes. | Must | 3b |
| SER12 | The Supplier shall ensure that its approach to maintenance and downtime aligns to DWP IT Operations Change and Release Policies and Procedures. The Supplier adheres to the following ITIL practices: (a) Change enablement; (b) Continual improvement; (c) Deployment management; (d) Incident management; (e) Information security management; (f) IT asset management; (g) Monitoring and event management; (h) Problem management; (i) Relationship management; (j) Release management; (k) Service configuration management; (l) Service desk; (m) Service level management; (o) Service request management; (p) Supplier management. | Must | 3b |
| SER13 | The Supplier shall publish a schedule of service downtime for the maintenance, patching, upgrade or fix of all services at least two weeks before the event. Maintenance is undertaken outside extended office hours, managed appropriately during the maintenance periods, and accounts for the needs of data back-ups and any necessary testing and quality assurance activities. | Must | 3b |
| SER14 | The supplier shall be able to align to DWP Digital's strategy relating to Licensing, Patching, and Certificate management. | Must | 3b |
| SER15 | The Supplier shall interlock with the Service Operations Design and Transition team to ensure a comprehensive set of support artefacts are produced and handed over into live service teams, ahead of any service go live. Including (but not exhaustive): (a) Service Design Pack (ITIL); (b) Service Transition approach (including Knowledge Transfer Plan, Early Life Support plan, and Agreed testing/assurance strategy). | Must | 3b |
| SER16 | The supplier's approach to monitoring and alerting shall align with DWP's monitoring and alerting strategy. | Must | 3b |
| SER17 | The supplier shall confirm that they are able to comply with the requirements outlined in the DWP Place Code of Connectivity policy (CoCo) which will be provided separately. | Must | 3b |

### Solution Integration (SIN)

| ID | Requirement | MoSCoW | ITT Grouping |
|---|---|---|---|
| SIN01 | The Supplier shall integrate the System(s) Solution with the Authority System (and those of its suppliers and business partners), in order to provide the required functionality. The Supplier shall integrate systems with a view to achieving the greatest possible performance, reliability, and simplicity of operating and maintaining the data. | Must | 3a |
| SIN02 | The Supplier shall produce a control document for any integration interface, when any of the following criteria apply to the exchange of data between systems: exchanges of data via automated system to system interfaces; and exchanges of data via manual process between system to system interface(s). | Must | 3a |
| SIN03 | The Supplier shall make any revisions necessary to the Delivery Documentation and shall make them available to the Authority. | Must | 3a |
| SIN04 | The Supplier shall ensure that if an enhancement to an interface is required and a new version of the interface is provided, there is no impact on previous versions of that interface or on other interfaces used by external systems. | Must | 3a |
| SIN05 | The Supplier shall analyse all errors or deficiencies identified by or notified to the Supplier with respect to the Services provided, and resolve these to the extent the error is caused by the operation of the Services. In all cases the Supplier shall cooperate with any third parties to resolve errors. | Must | 3a |
| SIN06 | The Supplier shall ensure that all interfaces and all functionality behind these are fully operational and available to their respective user groups based on the operating regime as a minimum. | Must | 3a |
| SIN07 | The Supplier shall ensure that maintenance for interfaces will be to an agreed maintenance period and frequency, which are published to the Authority in advance and managed appropriately during the maintenance periods. | Must | 3a |
| SIN08 | The Supplier shall ensure appropriate segregation of data to prevent leakage of information that could compromise the Authority or the Supplier. | Must | 3a |
| SIN09 | The Supplier shall look to improve the performance and capability of the System(s) Solution throughout the Term. Recommendations will be made quarterly to the Authority on potential improvements to either people, process, or systems. The Authority shall determine if any changes should be made to the Services in accordance with the Change Control Procedure. | Must | 3a |
| SIN10 | Any connections, interfaces or integrations with Authority information systems or digital services will be subject to the Authority information risk assessment. The Supplier shall make design and controls information available to the Authority upon request. | Must | 3a |
| SIN11 | The Supplier shall ensure that data input and output integrity routines (i.e. reconciliation and edit checks) are implemented for application interfaces and databases to prevent manual or systematic processing errors, corruption of data, or misuse. | Must | 3a |

### Supplier Exit (SUP)

| ID | Requirement | MoSCoW | ITT Grouping |
|---|---|---|---|
| SUP01 | During the Mobilisation Phase, the Supplier shall provide for the Authority's Approval the Exit Detailed Solution detailing the activity to be completed by the Supplier at either the expiry of the Term or where terminated early, on termination. | Must | 3a |
| SUP02 | The Supplier shall ensure all data, records, documents, or other information, created or used in performance of the solution are able to be returned to the Authority. | Must | 3a |
| SUP03 | At a time to be agreed with the Authority, the Supplier shall hand over all data, records and information pertaining to the Authority Estate that may be contained within the System(s) Solution and/or other documents in the possession of the Supplier to either the Authority or any new supplier. The format of the data will be determined by the Authority and notified to the Supplier not less than three (3) months prior to the date on which the Authority requires the data to be provided. After handing over, the Supplier shall confirm that all copies (including technical architecture and sensitive Authority information such as IP addresses) have been securely destroyed. | Must | 3a |
| SUP04 | The Supplier shall cooperate with the Authority and/or any other existing and/or future contractor(s) in transitioning the Service / System(s) Solution to any new supplier at the end of the Term, should the Authority require it. | Must | 3a |
| SUP05 | The Supplier shall assist in Authority data being securely transferred to any new solution and/or any new supplier in any instance where this Agreement is terminated. | Must | 3a |

### Usability (USE)

| ID | Requirement | MoSCoW | ITT Grouping |
|---|---|---|---|
| USE01 | The Supplier shall provide information (metadata) that enables system administrators or application users to understand: (a) any data presented on any screen, report or dashboard that is part of the application; (b) any application data available for extraction or reporting; and (c) any data produced as a result of the operation of the application (e.g. audit/logging data, performance and usage data, system events). This information includes as a minimum: (a) names and descriptions of datasets (e.g. files or tables); (b) names and descriptions of data items within each dataset; (c) descriptions of relationships between datasets; (d) data item formats, sizes and optionality; (e) an indication of whether each data item is intended to hold personal data; and (f) the meanings of status, type or other item values predefined by the Supplier. | Must | 3a |
| USE02 | The Supplier shall ensure that the metadata provided is made available in a format that can be uploaded directly into a database. | Must | 3a |
| USE03 | When future application changes made by the Supplier cause the previously provided metadata to become incomplete or invalid, the Supplier shall issue timely updates to the metadata in the same format(s) as the original. | Must | 3a |
| USE04 | The Supplier shall provide information (metadata) that describes the data contained within all of the data extracts (data feeds) created by the Supplier for the Authority. This metadata is provided as uploadable data, and conforms to Authority-specific naming conventions. The entity and attribute descriptions include at least: name, description, format, size, personally identifiable information (PII) indicator and validation rules. | Must | 3a |
| USE05 | The Supplier shall ensure that roll-out of the System(s) Solution uses demonstrable proven change methodology to ensure effective and efficient use of the System(s) Solution by End Users. | Must | 3a |
| USE06 | The Supplier shall ensure that at all times (whether during and/or after the Term) the Authority retains ownership of all data collected, used by, and contained within the System(s) Solution. | Must | 3a |
| USE07 | The Supplier shall ensure that the Authority has access to the System(s) Solution or aspects of the System(s) Solution by way of such licenses and other non-proprietary licensing as are appropriate. | Must | 3a |
| USE08 | The Supplier shall ensure interaction with any Authority Systems, with minimal human intervention / maximum automation. | Must | 3a |
| USE09 | The Supplier shall ensure that the Authority is not responsible for providing any part of the System(s) Solution, nor for any enabling facilities or resources relating to it. The Authority has the right (and ability to) audit any part of the System(s) Solution at any time. | Must | 3a |
| USE10 | The Supplier shall ensure that the Solution provides a single URL as a direct entry point to access the Supplier Solution. | Must | 3a |
| USE11 | The Supplier should ensure that the Solution provides a URL as a direct entry point to a specific function within the solution. | Should | 3a |
