# Functionality Gap Analysis vs Current Design Baseline

**Project:** DWP Debt Collection Management System (DCMS)  
**Date:** 2026-04-27  
**Purpose:** Reconcile older functionality gap documents against the current authoritative design set.

---

## 1. Scope Reviewed

Primary documents reviewed:

- `docs/project-foundation/functionality-design-gaps.md`
- `docs/project-foundation/gap-analysis-additional-layer.md`
- `docs/project-foundation/MASTER-DESIGN-DOCUMENT.md`
- `docs/project-foundation/architecture-decisions.md`
- Accepted ADRs in `docs/project-foundation/decisions/`
- Domain packs in `docs/project-foundation/domain-packs/`
- Domain rulings in `docs/project-foundation/domain-rulings/`

---

## 2. Authority Order Applied

This comparison uses the following authority order:

1. `MASTER-DESIGN-DOCUMENT.md` for current system-wide architecture intent and current open-design-question register.
2. Accepted architecture ADRs in `docs/project-foundation/decisions/` for architecture decisions.
3. Locked implementation ADRs in `docs/project-foundation/architecture-decisions.md` for non-negotiable implementation patterns.
4. Indexed domain rulings in `docs/project-foundation/domain-rulings/` for legally consequential behaviour and build gates.
5. Domain packs as area-level design detail.

Interpretation rule used:

- Where an older gap note conflicts with the master design or ADRs, the older gap note is obsolete.
- Where a domain pack is `Phase 3 complete` / `Phase 4 complete`, the old gap is treated as materially closed at design level.
- Where a domain pack is `Draft`, the gap is narrowed but not fully closed unless the master document has already adopted the design.

---

## 3. Executive Summary

### 3.1 Main conclusion

The older gap documents now materially overstate the amount of unresolved design work.

Two things changed after those documents were written:

1. The architecture was decisively locked as a **single modular monolith**, which invalidates the separate "additional layer" assumption.
2. Several major functional areas that were previously genuine gaps now have substantial design coverage in accepted ADRs, locked implementation ADRs, and locked domain packs.

### 3.2 Biggest historical mismatch

The original additional-layer analysis was no longer aligned with the authoritative design baseline. Its central assumption was a separate additional layer with its own services, databases, and UI surface. That conflicted with:

- ADR-007 monolith decision
- the master design's package ownership model
- the explicit placement of I&E, communications, work allocation, reporting, analytics, and DCA management as in-monolith domain modules

`gap-analysis-additional-layer.md` has now been rewritten as a reconciliation note. It should be used to understand what to reject, keep, and adapt from the old Solon-oriented material; it must not be used as target architecture for service boundaries or deployment topology.

---

## 4. Gap Disposition by Area

| Area from older gap docs | Current position vs updated design | Status |
|---|---|---|
| Additional layer as separate architecture | Superseded by accepted monolith architecture and current domain package model | **Obsolete assumption** |
| Reporting / MI / dashboards | Largely designed by master design, Phase 4 reporting/read-model pack, and `mi-dashboard-and-reporting-ux-design.md` | **Design largely closed** |
| Communications templates / delivery / fallback / suppression | Largely designed by ADR-015, locked suppression ADRs, RULING-001/RULING-011, and Phase 3 communications pack | **Design largely closed** |
| Contact preferences / verification / DNC / returned mail | Substantially designed in communications pack | **Design largely closed** |
| Work allocation / queues / supervisor dashboard / agent desktop | Substantially designed in Phase 3 work-allocation pack | **Design largely closed** |
| DCA placement / recall / reconciliation / commission | Substantially designed in DCA domain pack and RULING-008, but still gated by policy answers | **Design narrowed, not fully closed** |
| I&E / affordability / review scheduling | Designed in master section 7a and draft I&E pack, but blocked by policy values | **Design narrowed, not fully closed** |
| Champion/challenger governance and analytics | Architecturally designed in ADR-010 plus locked ADR-008/014 and RULING-010 | **Design largely closed** |
| Vulnerability management | Covered by rulings and draft vulnerability pack; some policy questions remain | **Design narrowed, not fully closed** |
| Frontend workflow screen catalogue | Only partially decomposed; admin, worklist, dashboard, and some specialist UX are defined, full end-user screen map is not | **Still a real gap** |
| Operational recovery tooling | Partially covered by Flowable Admin UI controls, manual review pattern, failed handoff paths; not fully end-to-end designed | **Still a real gap** |
| Finance accounting operations | Still thin in current authoritative design | **Still a real gap** |
| Manual corrections / amendment workflows | Immutable/audit principles exist, but explicit correction journeys are still sparse | **Still a real gap** |
| Complaints handling | Not yet established as a first-class workflow | **Still a real gap** |
| Legal / enforcement workflows | Still referenced, not decomposed into workflow/domain detail | **Still a real gap** |
| Retention / archival / closure lifecycle | Only partially covered in isolated areas; no coherent whole-system lifecycle design | **Still a real gap** |
| Notifications / alerts / escalation framework | Present in pockets, but not yet unified as a cross-domain design | **Still a real gap** |

---

## 5. Detailed Findings

### 5.1 Findings on the old additional-layer material

The old additional-layer material was the least aligned with the current baseline.

#### What is no longer valid

- It assumed a separate platform layer for strategy engine, champion/challenger, vulnerability, I&E, contact orchestration, work allocation, DCA management, and analytics.
- It assumed multiple additional-layer databases as the primary design direction.
- It assumed separate additional-layer services around capabilities that the current master design assigns directly to monolith domain modules.

#### What remains useful

- Its functional decomposition was directionally helpful.
- Several capability areas it highlighted were later absorbed into current domain designs:
  - communications
  - work allocation
  - reporting/read models
  - DCA management
  - I&E
  - vulnerability

#### Conclusion

Use the reconciled `gap-analysis-additional-layer.md` only as a historical capability checklist and disposition table. Do not use the old diagrams or old additional-layer assumptions as a source of truth for target architecture, module boundaries, or deployment topology.

### 5.2 Findings on `functionality-design-gaps.md`

This document remains useful as a gap inventory, but many entries now need reclassification.

#### Gaps that are no longer true at design level

- **Reporting and Management Information** is no longer thin in the same way. The reporting/read-model pack defines KPI taxonomy, projection tables, refresh strategy, executive dashboard queries, operations dashboard queries, and DCA/strategy performance projections. `mi-dashboard-and-reporting-ux-design.md` adds the staff-facing dashboard, export-centre, freshness, reconciliation, target-governance, and drilldown UX baseline.
- **Communication Template and Delivery Management** is no longer under-specified. The communications pack defines template lifecycle, approval, variable declarations, delivery events, suppression-decision logging, fallback, returned mail, and contact history.
- **Contactability, Preferences, and Consent** is no longer a major open design hole. The communications pack defines `contact_preference`, verification flags, DNC, contact windows, and returned mail handling.
- **Operational Admin and Recovery Tooling** is no longer wholly undefined. The design now includes role-scoped Flowable Admin access, audit capture for admin actions, failed DCA handoff handling, held-for-review communications, manual reconciliation queues, and manual review evidence patterns.
- **Reporting/dashboard views** and **supervisor queue monitoring / reassignment screens** are now explicitly designed in the work allocation and reporting packs.

#### Gaps that are now narrower than stated

- **Third-Party Debt Collection Management** remains a live gap only around unresolved policy inputs and implementation scaffolding. The lifecycle, state model, partner contracts, failed handoff handling, and commission/reconciliation model are now drafted in substantial detail.
- **Analytics, Scoring, and Decision Support** is only partly open now. Champion/challenger governance is well defined, but bureau feed refresh policy, score explainability detail, and some outcome-metric governance still need completion.
- **Frontend screens and staff workflows** should now be split into:
  - screens already designed at concept/spec level
  - screens still not decomposed
  The old document treats the whole surface as equally open, which is no longer accurate.
- **Notifications, Alerts, and Escalations** is partially addressed in work allocation and operational flows, but still lacks a system-wide pattern and ownership model.

#### Gaps that still stand

- Complaints handling
- Legal and enforcement pathways
- Finance/accounting operations depth
- Manual corrections and amendment journeys
- Data retention, closure, archival, and reactivation lifecycle
- Full frontend workflow catalogue outside the admin/work allocation/specialist areas

---

## 6. Domain-Package and Ruling Impact

### 6.1 Areas now covered by locked or near-locked design

- **Communications:** Phase 3 complete and design baseline locked.
- **Work allocation / agent desktop:** Phase 3 complete and design baseline locked.
- **Reporting / analytics read model:** Phase 4 complete and design baseline locked, now with an explicit MI/dashboard UX artefact.
- **Champion/challenger guardrails:** locked in implementation ADRs and governed by RULING-010.
- **Suppression behaviour:** locked across ADRs and rulings.

### 6.2 Areas covered only by draft designs

- **DCA management:** detailed draft exists, but policy gates remain open.
- **I&E engine:** detailed draft exists, but affordability-policy answers remain open.
- **Vulnerability domain:** detailed draft exists, but taxonomy / lawful-basis / joint-account questions remain open.

### 6.3 Areas still lacking equivalent replacement design

- Complaints domain pack or workflow design
- Legal / enforcement domain pack or workflow design
- Financial accounting operations pack
- Closure / retention / archival pack
- Manual amendments / corrections pack
- Cross-domain notifications / alerts design note

---

## 7. Current Genuine Gaps

The following are the most credible remaining design gaps after reconciliation with the updated baseline.

### Tier A — Still missing core design artefacts

1. Complaints handling as a first-class workflow and reporting domain.
2. Legal / enforcement workflow decomposition, state model, and evidence model.
3. Finance operations depth:
   - suspense handling
   - refunds
   - overpayments
   - write-off reversal handling
   - journal/export structure
4. Manual correction and amendment journeys aligned with immutable-record principles.
5. Retention, closure, archive, and reactivation lifecycle across domain data and evidence.
6. Operational recovery tooling as an end-to-end operator journey.
7. Full screen/workflow inventory for the broader staff frontend.

### Tier B — Designed, but blocked by open policy or client sign-off

1. I&E staleness period and benefit-deduction applicability.
2. DCA pre-placement notice period.
3. DCA acknowledgement / recall SLAs and any tiered commission policy.
4. Champion/challenger minimum duration and sample-size thresholds.
5. Vulnerability lawful basis, taxonomy, and some joint-account semantics.
6. Statute-barred timing/configuration questions tracked in RULING-013.
7. Communications fallback order, phone-suppression scope, returned-mail invalidation SLA, and IN_APP scope.
8. Work-allocation SLA tier defaults, concurrency policy, and supervisor visibility scope.

### Tier C — Designed conceptually, but not yet scaffolded in code structure

1. `analytics`
2. `thirdpartymanagement`
3. `reporting`
4. `infrastructure/process`
5. `shared/process/port`

These are implementation/scaffolding gaps, not primarily functionality-design gaps.

---

## 8. Recommended Document Actions

### 8.1 `gap-analysis-additional-layer.md`

Action completed:

- Rewritten as a reconciliation document against the current Flowable monolith baseline.
- Marks Solon/additional-layer architecture as rejected while retaining useful capabilities as adapted DCMS designs.

### 8.2 `functionality-design-gaps.md`

Action completed:

- Reclassified entries into:
  - obsolete
  - design closed
  - design narrowed / policy-gated
  - still open

The document should now be maintained as the current design-gap register rather than as a raw backlog of older gap statements.

### 8.3 Planning implication

Future design work should prioritise the genuinely missing artefacts:

1. complaints
2. legal/enforcement
3. finance operations
4. manual amendments/corrections
5. retention/archive/closure
6. operational recovery tooling
7. full frontend workflow catalogue

---

## 9. Final Assessment

The updated design baseline is materially more mature than the older gap notes imply.

The real story on 2026-04-27 is not "most functionality is still undefined." The real story is:

- the architecture is now settled
- several major operational domains are designed
- the remaining gaps are concentrated in a smaller set of still-undesigned domains, plus a non-trivial set of DWP policy sign-off questions

That means the next planning step should be **targeted closure of the remaining true gaps**, not continued use of the additional-layer model or the original broad gap inventory as if the design had not moved on.
