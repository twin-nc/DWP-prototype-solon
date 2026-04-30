# FEAT5 Integration Teammate Handoff (Agentic AI Ready)

## 1. Objective
Integrate selected FEAT5 content into the authoritative design baseline without replacing authority docs wholesale.

This handoff is for the teammate workstream (rulings/process/trace/diagrams), designed for parallel execution with agentic AI.

## 2. Current Git Situation (Updated)
1. All previously open PRs are merged to `origin/main` (including PRs #6, #8, #9).
2. `feature/wip-main-work-2026-04-23` has been fast-forwarded and is aligned with `origin/main`.
3. Teammate should branch from `origin/main` (not from `origin/feat/5-master-document`).
4. FEAT5 donor artifacts now exist on `main`; treat them as candidate input, not automatic authority.

Quick start for teammate:
1. `git fetch origin`
2. `git switch main`
3. `git pull --ff-only`
4. `git switch -c feat/feat5-teammate-<scope>`

## 3. Authority and Guardrails
Authority references to preserve:
1. `docs/project-foundation/MASTER-DESIGN-DOCUMENT.md`
2. `docs/project-foundation/ARCHITECTURE-BLUEPRINT.md`
3. `docs/project-foundation/decisions/ADR-001...ADR-010`
4. `docs/project-foundation/trace-map.yaml`
5. Standards pack in `docs/project-foundation/standards/`

Rules:
1. Do not replace authority docs with `development-plan.md` or `architecture-decisions.md`.
2. Translate useful detail into the authority structure.
3. Preserve status gates (`final`, `awaiting-client-sign-off`, `DO NOT PROCEED`) exactly.

## 4. Scope Ownership (Teammate)
Owned IDs from `docs/project-foundation/FEAT5-IMPORT-SHORTLIST.md`:
1. ~~`I06` final rulings import~~ **Done (2026-04-24)**
2. ~~`I07` provisional rulings import with status gates preserved~~ **Done (2026-04-24)**
3. ~~`I08` DDE-OQ pack import~~ **Done (2026-04-24)**
4. ~~`I09` process/DMN baseline appendix~~ **Done (2026-04-24) — open items OI-I09-01 through OI-I09-04 flagged in process-catalog-and-dmn-baseline.md**
5. ~~`I10` diagram import + mapping~~ **Done (2026-04-24)**
6. ~~`I11` trace-map migration into canonical schema~~ **Done (2026-04-24) — functionality_catalog populated (22 FNC entries); 11 requirement_links updated; ID mismatch with dev-plan §14 flagged as I11-OI-01**
7. ~~`I13` optional planning extraction~~ **Done (2026-04-24) — planning-notes.md created, non-authoritative**

Out of scope:
1. `I01`, `I02`, `I03`, `I04`, `I05`, `I12`, `I14`, `I15` (governance/RBAC stream)

## 5. Dependency and Sequencing
Recommended sequence:
1. ~~Execute `I06`, `I07`, `I08`, `I10` first (parallel-safe).~~ **Done — PR `feat/feat5-teammate-rulings` (2026-04-24)**
2. ~~Wait for governance contract merge from Dev A (`I02` + `I01`) before finalizing `I09` and `I11`.~~ **Unblocked: I09 and I11 delivered with explicit open-item flags (OI-I09-01, I11-OI-01) pending Dev A governance terms.**
3. Finalize `I09` process catalog wording after governance terms are stable. — **Pending: I09 document created with provisional sections and open-item flags (OI-I09-01, OI-I09-04), but role model wording in §7.7 and write-off thresholds in §5.8 cannot be finalized until Dev A delivers I01/I02.**
4. ~~Finalize `I11` trace-map links after rulings/process docs are settled.~~ **Done — 22 FNC entries added; 11 requirement_links wired; further links deferred pending subject-matter verification on ruling-linked entries.**

Single dependency handshake:
1. Request final role/control terms from Dev A before locking process and trace language.

## 6. Source Artifacts to Read (Now on Main)
Candidate source files now live directly on `main`:
1. `docs/project-foundation/development-plan.md`
2. `docs/project-foundation/architecture-decisions.md`
3. `docs/project-foundation/domain-rulings/RULING-013-HANDOFF.md`
4. `docs/project-foundation/domain-rulings/*`
5. `docs/project-foundation/diagrams/customer-journey-with-roles.drawio`
6. `docs/project-foundation/diagrams/vulnerable-track-customer-journey.drawio`

## 7. Target Artifacts to Update
Primary target files:
1. `docs/project-foundation/domain-rulings/*` (existing structure)
2. `docs/project-foundation/diagrams/*` and `docs/project-foundation/diagrams/README.md`
3. `docs/project-foundation/trace-map.yaml` (canonical, no duplicate markdown trace table)
4. New appendix file (recommended): `docs/project-foundation/process-catalog-and-dmn-baseline.md`

## 8. Branch-Aware Retrieval Tips (Updated)
Use commit-based comparison when you need "before FEAT5-doc merge" context:
1. Pre-PR-6 baseline snapshot (after PR #9): `cd849eb0f67f67e47c79434d3673fd28b6fd9b4d`
2. Current `main` after PR #6 merge: `bf5f6476f63fee3de02ade3f599f47ec0a3fa457`

Examples:
1. `git show cd849eb0f67f67e47c79434d3673fd28b6fd9b4d:docs/project-foundation/MASTER-DESIGN-DOCUMENT.md`
2. `git show bf5f6476f63fee3de02ade3f599f47ec0a3fa457:docs/project-foundation/development-plan.md`
3. `git diff cd849eb0f67f67e47c79434d3673fd28b6fd9b4d..bf5f6476f63fee3de02ade3f599f47ec0a3fa457 -- docs/project-foundation`

## 9. Working Rules (Conflict Reduction)
1. Do not edit `MASTER-DESIGN-DOCUMENT.md`, `ARCHITECTURE-BLUEPRINT.md`, or `decisions/*` unless explicitly requested by Dev A.
2. Keep all ruling statuses intact when importing/adapting.
3. Preserve all "DO NOT PROCEED" gates as gates (never rewrite as implemented fact).
4. Do not introduce second source-of-truth trace tables in markdown.
5. Every imported behavior must map into `trace-map.yaml`.
6. Mark unresolved policy items as open decisions with owner and due date.

## 10. Deliverables and Done Criteria
Deliverables:
1. Imported rulings package (`I06`/`I07`/`I08`) with preserved status/governance semantics.
2. Process/DMN baseline appendix (`I09`) aligned to current ADR governance.
3. Imported diagrams with source mapping and verification date (`I10`).
4. `trace-map.yaml` updates only in canonical schema (`I11`).

Done criteria:
1. No candidate source file used as full replacement of authoritative docs.
2. No contradictory trace source introduced.
3. All Class A relevant additions have ruling linkage or explicit `awaiting-ruling`/`awaiting-sign-off` status.
4. PR description lists IDs addressed and exact source file references used.

## 11. Agentic AI Prompt Template (for Teammate)
Use this with your coding/review agent:

```text
You are assisting with selective FEAT5 integration from files already present on origin/main into the authoritative documentation model.

Context:
- Base branch: origin/main
- Working branch: feat/feat5-teammate-<scope>
- Scope IDs: I06, I07, I08, I09, I10, I11, I13
- Out of scope IDs: I01, I02, I03, I04, I05, I12, I14, I15
- Candidate source files: docs/project-foundation/development-plan.md, docs/project-foundation/architecture-decisions.md, docs/project-foundation/domain-rulings/RULING-013-HANDOFF.md, docs/project-foundation/domain-rulings/*, docs/project-foundation/diagrams/customer-journey-with-roles.drawio, docs/project-foundation/diagrams/vulnerable-track-customer-journey.drawio

Non-negotiables:
1) Do not replace authoritative architecture/governance docs wholesale.
2) Preserve ruling statuses and DO NOT PROCEED gates.
3) Keep traceability in docs/project-foundation/trace-map.yaml only (no duplicate markdown trace table).
4) Minimize cross-file edits outside owned scope.

Tasks:
1) Extract only relevant content for in-scope IDs.
2) Adapt wording and structure to the existing authoritative doc model.
3) Produce edits with clear source citations (file + section/line where practical).
4) Flag any dependency on unresolved governance terms from Dev A.

Output format:
1) Files changed
2) IDs completed
3) Open dependencies/questions
4) Residual risks
```

## 12. PR Template (Teammate)
Use this in each PR description:

1. Scope IDs completed: `...`
2. Source files used (from `main`): `...`
3. Target files changed: `...`
4. Status-gated items preserved: `yes/no + details`
5. Trace-map updates included: `yes/no + requirement IDs`
6. Dependencies on Dev A stream: `...`

