# SUM-02: Requirements Coverage Summary

Coverage status: **Covered** means ownership and design are materially defined; **Tier A gap** means a core design artifact is still missing; **Tier B blocked** means design exists but policy/client sign-off blocks completion; **Tier C scaffold** means ownership is agreed but code/package scaffolding remains.

## Covered

| Capability group | Count | Module owner | Coverage status |
|---|---:|---|---|
| Customer & Account Structure (`CAS`) | 17 | `customer`, `account` | Covered |
| Data & Information Capture (`DIC`) | 37 | `customer`, `account`, `communications`, `audit`, `integration` | Covered |
| Repayment Plan Functionality (`RPF`) | 37 | `repaymentplan`, `payment`, `integration` | Covered |
| User Access & Admin Functions (`UAAF`) | 26 | `user`, Keycloak, frontend admin surfaces | Covered |
| MI & Reporting (`MIR`) | 2 | `reporting` | Covered |
| Analytics & Segmentation (`A`) | 2 | `analytics`, `strategy` | Covered |
| Agent Actions & Dispositions (`AAD`) | 27 | `communications`, `audit`, `workallocation` | Covered |
| Interfaces to 3rd Party Systems (`I3PS`) | 15 | `integration` | Covered |
| Change Processes (`CP`) | 11 | `docs/project-foundation`, `.github/workflows` | Covered |
| System Development & Roadmap (`SD`) | 7 | `docs/project-foundation` | Covered |

## Gaps and Blockers

| Capability group | Count | Module owner | Coverage status |
|---|---:|---|---|
| User Interface Screens (`UI`) | 30 | `frontend/src` | Tier A gap: full frontend workflow catalogue remains incomplete |
| System of Record (`SoR`) | 21 | `account`, `payment`, `audit`, `integration` | Tier A gap: finance/accounting operations depth remains thin |
| Migration Requirements (`MR`) | 15 | `integration`, migration tooling | Tier A gap: migration tooling design not yet specified |
| Work Allocation & Monitoring (`WAM`) | 28 | `workallocation` | Tier B blocked: SLA tier defaults, concurrency policy, and supervisor visibility scope need confirmation |
| Income & Expenditure Capture (`IEC`) | 11 | `repaymentplan`, `customer`, `integration` | Tier B blocked: I&E policy values and benefit-deduction applicability remain open |
| Contact Channels (`CC`) | 35 | `communications`, `integration`, `audit` | Tier B blocked: fallback order, phone-suppression scope, returned-mail SLA, and in-app scope remain open |
| 3rd Party Management (`3PM`) | 18 | `thirdpartymanagement` | Tier B blocked + Tier C scaffold: DCA policy inputs remain open and package is not scaffolded |
| Bureau & Scorecard Feeds (`BSF`) | 15 | `analytics`, `strategy`, `integration` | Tier B blocked: feed refresh, score governance, and champion/challenger thresholds need confirmation |
| Decisions & Workflows (`DW`) | 88 | `infrastructure/process`, `shared/process/port`, `strategy`, `workallocation` | Tier C scaffold: Flowable infrastructure and shared process ports are not yet scaffolded |
