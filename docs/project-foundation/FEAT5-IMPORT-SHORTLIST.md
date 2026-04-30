# FEAT5 Import Shortlist

## Purpose
Use the FEAT5 document set now present on `origin/main` as input while keeping the authority model unchanged:
- `MASTER-DESIGN-DOCUMENT.md`
- `ARCHITECTURE-BLUEPRINT.md`
- `decisions/ADR-001..ADR-010`
- `trace-map.yaml`

Context update:
- PRs #6, #8, and #9 are merged.
- `feature/wip-main-work-2026-04-23` is fast-forwarded and aligned with `origin/main`.

This shortlist maps what to:
- `Adopt`
- `Adopt with edits`
- `Do not adopt`

## Ground Rules
1. Keep `MASTER-DESIGN-DOCUMENT.md`, `ARCHITECTURE-BLUEPRINT.md`, `decisions/`, and `trace-map.yaml` as authority.
2. Do not import conflicting governance directly; translate useful detail into existing authority structure.
3. Any Class A behavior must respect ruling status gates and standards precedence.

## Import Matrix
| ID | Source artifact (FEAT5 pack on `main`) | Decision delta vs authority docs | Recommendation | Target in this branch | Acceptance check |
|---|---|---|---|---|---|
| I01 | `development-plan.md` RBAC matrix and role table | Authority docs mark RBAC matrix as pending; FEAT5 pack contains concrete permissions/thresholds | Adopt with edits | `MASTER-DESIGN-DOCUMENT.md` section 10 and open questions closure; optionally new `docs/project-foundation/rbac-permissions-matrix.md` | Placeholder RBAC text removed, explicit matrix added, unresolved items marked with owner/date |
| I02 | `architecture-decisions.md` ADR-006 (Flowable Admin UI access split + audit events) | Authority docs have high-level access architecture, not Flowable UI operation detail | Adopt with edits | New ADR in `decisions/` (next number) or extend existing access section | OPS_MANAGER/FLOWABLE_ADMIN capability split documented with audit event model |
| ~~I03~~ | `architecture-decisions.md` ADR-009 (live-state DB reads in delegates) | Aligns with existing determinism and process design intent | Adopt | New ADR-012; cross-reference added to ADR-003 consequences | **Done (2026-04-24)** — ADR-012 created; guardrail extended with governed baseline field list and caching deferral note |
| ~~I04~~ | `architecture-decisions.md` ADR-013 + `development-plan.md` breathing-space resume logic | Provides concrete resume semantics missing in current docs | Adopt with edits | New ADR-013; cross-references added to ADR-003 and ADR-011 (broken reference repaired) | **Done (2026-04-24)** — ADR-013 created; transaction boundary rule and resume delegate design documented; `generateRevisedScheduleNotice` coupling rejected in favour of separate BPMN service task per ADR-008 tier model |
| ~~I05~~ | `architecture-decisions.md` ADR-001 suppression authority pattern | Complements existing rulings and integration reliability | Adopt with edits | New ADR or communications section in blueprint | **Done (2026-04-24)** — ADR-014 created; `CommunicationSuppressionService` singleton authority, locked `isPermitted()` priority sequence, call-site enforcement at `NotifyAdapter`, write authority restriction, and audit obligations documented; sprint language removed; ADR-003 cross-reference added |
| I06 | Domain rulings `RULING-005` and `RULING-011` (`final`) | Final rulings can directly strengthen implementation constraints | Adopt | Merge content into existing rulings framework and map to requirements in `trace-map.yaml` | Final rulings linked to requirements and affected modules/tests |
| I07 | Domain rulings `RULING-006`, `RULING-009`, `RULING-012`, `RULING-013` (awaiting sign-off) | Strong constraints but status-gated; some include DO NOT PROCEED gates | Adopt with edits | Add as "provisional constraints" in current rulings set and open decision list | Status and blocking conditions preserved; no "implemented" language before sign-off |
| I08 | `DDE-OQ-STATUTE-BARRED-SUMMARY.md` and `RULING-013-HANDOFF.md` | Structured client question pack is useful; authority docs still need canonical placement/mapping | Adopt | `domain-rulings/` as decision-support appendix | Owner, deadline, default path, and impact fields preserved |
| I09 | `development-plan.md` section 7 process/DMN definitions | Adds concrete executable model detail not yet explicit in authority docs | Adopt with edits | New appendix doc: `docs/project-foundation/process-catalog-and-dmn-baseline.md` | Process keys, DMN names, hit policies, and key guardrails are trace-linked |
| I10 | `customer-journey-with-roles.drawio`, `vulnerable-track-customer-journey.drawio` | FEAT5 visual assets can improve stakeholder understanding when mapped to canonical diagrams | Adopt | `docs/project-foundation/diagrams/` with README mapping update | Diagram to source-doc mapping and last-verified date added |
| I11 | `development-plan.md` section 14 requirement table | Canonical trace model is `trace-map.yaml` many-to-many schema | Adopt with edits | `trace-map.yaml` only (do not duplicate as markdown table) | Requirement links are migrated into canonical trace schema, no duplicate source of truth |
| I12 | `architecture-decisions.md` as full replacement ADR set | Conflicts with current accepted ADR-001..010 architecture set and numbering semantics | Do not adopt | N/A | No wholesale import; only selected rules promoted into current ADR lineage |
| I13 | `development-plan.md` sprint calendar and staffing plan | Tactical plan, date-sensitive, and partially inconsistent internally | Do not adopt as architecture authority | Optional PM tracker docs only | No architecture/governance source references these dates as normative |
| I14 | `architecture-decisions.md` migration odd/even numbering convention | Conflicts with current migration standard and team scaling model | Do not adopt | Keep current `STD-PLAT-006` usage | Migration policy remains standards-aligned and single-source |
| I15 | `development-plan.md` random-based champion/challenger assignment | Conflicts with deterministic hash assignment already decided in ADR-010 | Do not adopt (logic) / adopt (dashboard metrics shape) | Keep ADR-010 assignment semantics; optionally adopt analytics panel definitions | Assignment remains deterministic; analytics schema can be reused |

## Recommended PR Slices
1. `PR-1 Governance`: Import I02, I03, I04, I05 as new/updated ADRs.
2. `PR-2 RBAC`: Import I01 with explicit unresolved items and sign-off owners.
3. `PR-3 Rulings`: Import I06, I07, I08 with status gates preserved.
4. `PR-4 Process Catalog`: Import I09 plus diagram assets from I10.
5. `PR-5 Traceability`: Apply I11 mapping updates to `trace-map.yaml`.

## Explicit Non-Imports
- Do not treat FEAT5 source documents as replacement authorities:
  - `docs/project-foundation/development-plan.md`
  - `docs/project-foundation/architecture-decisions.md`
- Treat them as donor/candidate sources only.

## Completion Criteria
1. No duplicate authority between markdown tables and `trace-map.yaml`.
2. No conflict with `decisions/ADR-001..ADR-010`.
3. All imported Class A behavior links to ruling status and open-question ownership.
4. All adopted details have a home in existing authoritative docs.
