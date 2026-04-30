# Deviation Record — DEV-001

## Deviation ID
`DEV-001`

## Title
COTS scope boundary — standards apply to integration adapters and configuration scripts only, not to the COTS platform internals

## Date Opened
`2026-04-16`

## Owner
`Solution Architect`

## Status
`Approved` — this is a standing scope clarification, not a compliance gap. It is approved at project initiation and reviewed at contract award.

## Expiry / Review Date
`Contract award date` — review scope once COTS vendor is confirmed and vendor SLAs are known. Amend or close as appropriate.

---

## Standard(s) Deviated From

This deviation clarifies the **scope of application** for the following standards. The standards are not violated — they simply do not apply to the COTS internals:

- `STD-PLAT-006 — Database Migration Standard` — applies to Netcompany-built adapter services only; the COTS manages its own schema and migration lifecycle
- `STD-PLAT-009 — Containerization Standard` — applies to Netcompany-built adapter and extension services only; the COTS vendor manages their own container/runtime
- `STD-PLAT-010 — Local Dev Environment Standard` — applies to Netcompany-built services only; local COTS sandbox environment is vendor-provided
- `STD-OPS-003 — Health Endpoints` — applies to Netcompany-built services; COTS health endpoints are vendor-managed (verify vendor exposes equivalent and document in remote-environment-spec.md)
- `STD-OPS-004 — Structured Logging` — applies to Netcompany-built services; COTS internal logs are vendor-managed (verify vendor log format is compatible with DWP observability requirements SER16)
- `STD-DEV-001 — Testing Standard` — unit and integration test coverage requirements apply to Netcompany-built code; COTS configuration testing follows the COTS vendor's own testing tooling (champion/challenger, strategy simulation — DW.39, DW.41)

---

## Scope

This deviation applies to the current greenfield COTS implementation baseline.

This deviation applies to:
- The COTS platform internals (database, workflow engine, UI, internal APIs)
- The COTS vendor's own deployment and infrastructure

It does **not** apply to:
- Netcompany-built integration adapters (REST, FTPS, event-based)
- Netcompany-built extension services or bespoke modules
- Configuration scripts and deployment tooling written by Netcompany
- The integration layer between the COTS and DWP systems

All Netcompany-built code and services are subject to the full standards pack without exception.

---

## Requirement(s) Impacted

- `OPP05` — COTS is SaaS; vendor manages infrastructure
- `SER04` — Vendor keeps all COTS components within supported versions
- `SER11` — Netcompany configuration changes must not block the COTS upgrade path
- `DW.9` — Business rule versioning is managed within the COTS vendor tooling

---

## Rationale

The DWP Debt Collection system is a COTS implementation. The COTS platform is a vendor-managed SaaS product (OPP05). Applying Netcompany's development standards (database migrations, containerisation, health endpoint conventions) to the COTS internals is neither possible nor appropriate — the vendor owns that layer.

Applying these standards to the COTS would: (a) be technically impossible without vendor cooperation, (b) risk voiding vendor SLAs and support agreements, and (c) create false assurance if standards are notionally applied to code Netcompany does not control.

The standards remain in force and are applied without modification to all Netcompany-owned code and services.

---

## Risk Assessment

**Risk level:** `Low`

The risk of not applying Netcompany standards to the COTS internals is mitigated by:
1. The vendor is contractually responsible for their platform's reliability, security, and upgrade path (SER04, SER11).
2. Vendor SOC1/SOC2 reports or equivalent provide independent assurance of the vendor's internal controls (COM08).
3. DWP's own security risk assessment (COM11b) covers the COTS platform.
4. Netcompany standards apply fully to the integration boundary — this is where Netcompany introduces risk.

---

## Compensating Controls

1. **Vendor assurance review:** Obtain and review the COTS vendor's SOC2 report (or equivalent) before sprint 1. Document findings in `docs/project-foundation/`.
2. **Integration boundary standards:** All Netcompany-built integration adapters are subject to the full standards pack without exception — this is where Netcompany owns the risk.
3. **Vendor health endpoint verification:** Confirm the COTS exposes health signals compatible with DWP's monitoring strategy (SER16). Document in `remote-environment-spec.md §5`.
4. **Vendor log format verification:** Confirm the COTS produces logs in a format compatible with DWP's log access requirements. Document method and format in `remote-environment-spec.md §5`.
5. **Configuration change governance:** All COTS configuration changes (strategies, business rules, workflows) must go through the same PR and CI governance as code changes — see WAYS-OF-WORKING.md.
6. **Upgrade path protection (SER11):** Netcompany configuration changes must be validated against each COTS upgrade cycle. The COTS vendor must be consulted before any configuration is applied that could block upgrade paths.

---

## Remediation Plan

This is a permanent scope clarification, not a temporary gap. No remediation is required. Review at contract award to confirm the scope boundary is correct for the contracted COTS product.

| Step | Owner | Target Date |
|---|---|---|
| Obtain and review COTS vendor SOC2 / equivalent | Solution Architect | Contract award + 2 weeks |
| Confirm COTS health endpoint format and document in remote-environment-spec.md | SRE / Platform Engineer | Before first remote deployment |
| Confirm COTS log format compatibility with DWP observability requirements | SRE / Platform Engineer | Before first remote deployment |
| Review and amend this deviation record based on confirmed COTS product | Solution Architect | Contract award |

---

## Release Impact
`Deferrable` — this is a scope clarification, not a compliance gap. The compensating controls above are in place from project initiation.

---

## Signoffs

| Role | Name | Date | Notes |
|---|---|---|---|
| Standard Owner | *(to be completed)* | | |
| Delivery Lead | *(to be completed)* | | |
| Security Owner | *(to be completed)* | | Confirm vendor assurance approach is acceptable |
| Domain Owner | N/A | | Not a Class A behavior change |

---

## Evidence Pack Reference
`Evidence pack reference: include in first release evidence pack — confirm compensating controls are in place.`
