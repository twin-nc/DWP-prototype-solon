# Planning Notes — PM Tracker Reference

> **Status: NON-AUTHORITATIVE.** This file is a planning reference only.
> It does not set architecture, governance, or requirements baselines.
> Source: `development-plan.md` §12 (demo scenario).
> Imported under FEAT5 scope item I13 (2026-04-24).

---

## Migration Block Convention

| Flyway version block | Purpose |
|---|---|
| V001–V009 | Foundation migrations |
| V010–V019 | Vulnerability, fraud |
| V020–V029 | Collection workflow |
| V030–V039 | Repayment, breathing space |
| V040–V049 | Write-off, I&E |
| V050–V059 | Champion/challenger, analytics |
| V060–V069 | Household/joint debt, disputes |
| V070–V079 | Integration hardening |
| V080–V089 | Final stabilisation |
| V900+ | Seed data (separate Flyway location) |

Claim a version number before creating a migration file to avoid conflicts.

## Team Role Assignments

| Designation | Primary focus |
|---|---|
| Dev1 | Backend lead: Flowable engine, process definitions, complex business logic |
| Dev2 | Backend: payments, communications, audit |
| Dev3 | Backend: customer, account, vulnerability, repayment |
| Dev4 | Frontend lead: component library, routing, complex screens |
| Dev5 | Frontend: forms, worklists, communication screens |
| Dev6 | Full-stack: integration adapters/stubs, admin screens, analytics |
| Dev7 (if available) | Backend: overflow + BPMN implementation support |
| DevOps1 | CI/CD, containerisation, AKS, Helm, image registry, load testing |
| DevOps2 | Keycloak config, security infra, network policy, database ops |
| QA | Test plans, Playwright E2E automation, manual acceptance testing, WCAG checks |

## Demo Schedule (8 Jul 2026 target)

| Key date | Event |
|---|---|
| 4 Jul 2026 | Demo dry-run. Demo namespace locked after dry-run passes. |
| 8 Jul 2026 | Target client demo date. |

### Demo Scenarios (summary)

| Scenario | Core flow |
|---|---|
| A: Standard collection end-to-end | Search → statute-barred check → segmentation → I&E → arrangement → payment → comms |
| B: Vulnerable customer — specialist track | Vulnerability flag → access control → breathing space → suppression log |
| C: Write-off multi-tier approval | Agent request → DMN limit check → TEAM_LEADER → OPS_MANAGER → audit trail |
| D: Champion/challenger live A/B test | Strategy admin → ingest accounts → DMN assignment → analytics dashboard |
| E: Household and joint debt split | Joint account → query → joint_debt_split.dmn → child accounts → audit trail |
| F: Dispute management and forbearance | I&E → forbearance DMN → dispute suppression → resolution → audit trail |

Contingency for Demo E: if DDE-OQ-04 is unconfirmed, show household view and DMN design in Admin UI only; joint debt split execution deferred to post-award.

---

> This file must not be referenced by ADRs, rulings, or `trace-map.yaml`.
