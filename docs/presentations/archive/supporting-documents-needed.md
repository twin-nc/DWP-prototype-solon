# Supporting Documents Needed — Leadership Presentation

Each item below is required to produce a specific slide in the presentation outline.
Items are ordered by slide dependency. Type is either **DIAGRAM** (visual/schematic) or **SUMMARY** (condensed markdown narrative).

## Diagram Quality Standard

All diagrams are destined for PowerPoint slides in a leadership presentation. They will be viewed on a projected screen at distance, not read up-close on a monitor. Every diagram must be production-ready for that context before it is considered done.

**PowerPoint / slide-specific requirements:**
- **Designed for a slide canvas** — assume a 16:9 widescreen slide (approximately 33 cm × 19 cm). The diagram must fill the slide area comfortably without dead space, and must not be cropped or require scrolling.
- **High contrast** — use colours that remain legible when projected. Avoid light grey text on white backgrounds; avoid colour combinations that disappear under a projector or on a low-contrast screen.
- **Font size** — there is a deliberate tradeoff between including necessary information and font size. Prefer completeness; do not omit content just to increase font size. However, no label should be so small that it is illegible when zoomed in on screen.
- **Export-ready** — the source file must be exportable as a PNG or SVG at sufficient resolution (minimum 150 DPI for PNG) for direct insertion into PowerPoint without blurriness.

**General readability requirements:**
- **No overlapping boxes or lines** — every shape and connector must have clear whitespace around it. If a layout is too dense, split into two diagrams rather than compressing.
- **No crossing lines** — arrange nodes so that connectors do not cross each other. Where a crossing is unavoidable, use a bridge/hop marker to make the crossing explicit.
- **Consistent spacing** — equal padding inside boxes; equal gap between adjacent boxes at the same level. Irregular spacing signals an unfinished diagram.
- **Labels on connectors** — every arrow or line must be labelled if its meaning is not unambiguous from the node names alone.

These are not aesthetic preferences — an unreadable or poorly laid out diagram in a leadership presentation undermines the message it is trying to convey.

---

## Tooling Standard — draw.io (Lessons Learned from DIAG-02)

**Use draw.io (`.drawio` XML) as the source format for all diagrams.** Do not use Mermaid, matplotlib/Python, or other programmatic renderers. The reasons:

- draw.io files open natively in VS Code (draw.io extension), the draw.io desktop app, and app.diagrams.net — no build step required.
- Export to PNG/SVG at any resolution is one click, directly into PowerPoint.
- The XML format is editable by Claude and human alike; changes are precise and reversible.
- Mermaid C4 diagrams cannot be layout-controlled and produce poor results at presentation scale.
- Python/matplotlib scripts require execution, produce static rasters that cannot be edited after the fact, and embed layout maths that is hard to maintain.

**Structural lessons from DIAG-02:**

- **Swimlane containers for system boundaries** — use `style="swimlane;startSize=32;..."` for DCMS and External Systems boundaries. This gives a labelled header bar and a clean interior canvas. Do not use plain rectangles for boundaries.
- **Nested swimlane for the monolith** — the backend monolith is itself a swimlane child of the DCMS boundary. This keeps all package boxes scoped correctly and prevents connectors from escaping the boundary.
- **Individual boxes per domain package** — do not list domain packages as plain text inside a single box. Each package gets its own `rounded=1` box with a bold title and 2–3 line body. Grid layout: 4 columns × N rows. Full-width row for packages that span the whole width (e.g. `thirdpartymanagement`).
- **Colour palette by layer** — use distinct, high-contrast fill colours per layer type. The validated palette:
  - Users / actors: `#37474F` (dark slate)
  - Runtime service (Frontend): `#1565C0` (blue)
  - Infrastructure (Database, Keycloak): `#2E7D32` (green)
  - Domain packages: `#283593` (dark indigo)
  - Port interface layer: `#6A1B9A` (purple)
  - Process infrastructure: `#AD1457` (crimson)
  - Flowable engine: `#B71C1C` (deep red)
  - External systems: `#5D4037` (brown)
  - All text on dark fills: `#FFFFFF`
- **Connector routing** — use `edgeStyle=orthogonalEdgeStyle` for all connectors. Set explicit `exitX/exitY` and `entryX/entryY` to control which port connectors attach to, avoiding diagonal lines across the diagram.
- **Internal process chain** — the isolation chain (domain → port → infra → engine) is shown as vertical arrows inside the monolith swimlane, with label text identifying what crosses each boundary.
- **Legend** — include a legend box bottom-left (outside the DCMS boundary) showing each fill colour and its meaning. Use the same fill colours as the diagram boxes.
- **Footer note** — a single italic line below the DCMS boundary summarising the 2–3 key architectural constraints visible in the diagram.

---

## Diagrams

### DIAG-01 — Debt Lifecycle with Capability Group Overlay
- **For:** Slide 2 (Scope in One Picture)
- **Description:** Linear flow: Identification → Assessment → Recovery → Enforcement → Write-Off. Each stage annotated with the capability groups that operate within it (e.g., DIC/SoR at Identification; DW/RPF at Recovery; 3PM/LR at Enforcement).
- **Source material:** functional-requirements-module-map.md, MASTER-DESIGN-DOCUMENT.md
- **Format:** Horizontal swimlane or linear flow diagram. Five stages minimum; capability group labels can be small text callouts.

### DIAG-02 — System Architecture Overview
- **For:** Slide 4 (System Architecture in One Diagram)
- **Description:** Two runtime services (backend monolith, frontend Nginx). Supporting infrastructure: PostgreSQL 16, Keycloak 24. Monolith interior: domain package wheel with 13 packages, process engine (Flowable) at centre. External boundaries: DWP Place, payment gateway, bureau feeds, DCA systems, customer portal (external, inbound API only).
- **Source material:** MASTER-DESIGN-DOCUMENT.md, CLAUDE.md (tech stack)
- **Format:** Container diagram (C4 level 2 style). Keep it clean — no class-level detail.

### DIAG-03 — Delegate Command Pattern
- **For:** Slide 5 (The Process Engine Decision)
- **Description:** Show the isolation boundary. Domain module calls ProcessEventPort (shared/process/port). ProcessEventPort is implemented by infrastructure/process. infrastructure/process is the only layer that imports Flowable. Flowable BPMN engine sits at the bottom. No arrows from domain → Flowable directly.
- **Source material:** ADR-003, MASTER-DESIGN-DOCUMENT.md
- **Format:** Simple layered box diagram. Three layers: Domain → Port Interface → Process Infrastructure → Flowable Engine.

### DIAG-04 — Entity Model (Party / DebtAccount / RepaymentArrangement)
- **For:** Slide 10 (How a Debt Becomes an Account)
- **Description:** Party (1) → (many) DebtAccount. DebtAccount (1) → (many) RepaymentArrangement. Party has: vulnerability flags, accessibility needs, third-party authority, joint liability link. DebtAccount has: regulatory state fields (breathing_space_flag, insolvency_date, deceased_flag, fraud_marker), one Flowable process instance ID. RepaymentArrangement has: terms, status, breach threshold.
- **Source material:** MASTER-DESIGN-DOCUMENT.md, domain-packs
- **Format:** ERD or UML class diagram, simplified. No field-level detail beyond the fields listed above.

### DIAG-05 — Three-Tier Configuration Model with Author Roles
- **For:** Slide 12 (Three-Tier Configurability Model)
- **Description:** Three horizontal tiers stacked. Tier 1 (bottom): reference data, authored by Operations. Tier 2 (middle): DMN decision tables, authored by Business Admin. Tier 3 (top): BPMN process definitions, authored by Business Admin via Flowable Modeler. Show change frequency annotation on each tier. Show that a Policy Bundle can wrap all three tiers with a single effective date.
- **Source material:** ADR-008, ADR-009
- **Format:** Stacked layer diagram with role callouts and frequency labels.

### DIAG-06 — Policy Bundle Lifecycle (Status State Machine)
- **For:** Slide 13 (Policy Bundles)
- **Description:** State machine: DRAFT → READY → APPROVED → ACTIVE → RETIRED. Show that ACTIVE → RETIRED is the normal path. Show that activation failure on APPROVED → ACTIVE rolls back to APPROVED (not DRAFT). Annotate who can perform each transition (e.g., PROCESS_DESIGNER creates DRAFT; OPS_MANAGER approves; system activates on effective date).
- **Source material:** ADR-009, MASTER-DESIGN-DOCUMENT.md
- **Format:** State machine diagram with labelled transitions and role annotations.

---

## Summaries

### SUM-01 — ADR Digest
- **For:** Slide 6 (Key Architecture Decisions Table)
- **Description:** One paragraph per key ADR. Cover: monolith decision, one-process-per-account, Java 21 exhaustive switch, transaction boundary rule, write-off self-approval prevention, Keycloak RBAC. Each paragraph: decision made, why, and what it prevents.
- **Source material:** MASTER-DESIGN-DOCUMENT.md, architecture-decisions.md, ADR-001, ADR-003, ADR-007, ADR-011 through ADR-014, RBAC-IMPLEMENTATION-DECISIONS.md
- **Format:** AsciiDoc. Six sections, one short paragraph each (~4–6 sentences). No jargon — audience is leadership, not engineers.
- **Output path:** `docs/presentations/adr-digest.adoc`

### SUM-02 — Requirements Coverage One-Pager
- **For:** Slide 7 (Requirement Coverage Map)
- **Description:** Condensed table: 19 capability groups, requirement count per group, module owner, and coverage status (Covered / Tier A gap / Tier B blocked / Tier C scaffold). Add a one-sentence explanation of the coverage status categories at the top.
- **Source material:** functional-requirements-module-map.md, functionality-gap-analysis-vs-current-design.md
- **Format:** Markdown table. Fits on one slide when exported. Group by status (Covered first, then gaps).
- **Output path:** `docs/presentations/requirements-coverage-summary.md`

### SUM-03 — Demo Flow Coverage Matrix
- **For:** Slide 14 (14 Flows Covering 20 Behavioural Requirements)
- **Description:** Matrix table: rows = 14 demo flows (with one-line description each), columns = 20 behavioural requirements (abbreviated). Cell = tick if the flow demonstrates that requirement. Add a "primary flow" indicator for the three headline moments (Flows 2, 5, 12).
- **Source material:** demo-flow-design.md, major-behavioral-requirements.md
- **Format:** Markdown table. May need landscape orientation in slides. Mark the three headline flows clearly.
- **Output path:** `docs/presentations/demo-flow-coverage-matrix.md`

### SUM-04 — Release 1 vs Release 2 Scope Table
- **For:** Slide 18 (Release 1 Scope)
- **Description:** Two-column table: Release 1 scope (modules in, demo flows covered, what is explicitly excluded) vs Release 2 additions. Add a third column for Release 3. Include a note on the definition of done for Release 1 (working software in a running environment, not a feature-complete system).
- **Source material:** functionality-gap-analysis-vs-current-design.md, functional-requirements-module-map.md, MASTER-DESIGN-DOCUMENT.md
- **Format:** Markdown table with a short preamble paragraph.
- **Output path:** `docs/presentations/release-scope-table.md`

---

## Existing Documents That Can Be Used Directly (No New Doc Needed)

These already exist and contain sufficient content for their slides. They need to be read at slide-creation time, not recreated.

| Slide | Source document |
|---|---|
| Slide 1 (Mission) | MASTER-DESIGN-DOCUMENT.md — executive summary section |
| Slide 3 (Differentiators) | ADR-008, ADR-009, MASTER-DESIGN-DOCUMENT.md |
| Slide 8 (Gap classification) | functionality-gap-analysis-vs-current-design.md |
| Slide 9 (Implied build scope) | This outline + existing ADRs |
| Slide 11 (Regulatory state) | MASTER-DESIGN-DOCUMENT.md, ADR-002 |
| Slides 16–17 (Solon Tax) | solon-tax-feasibility-analysis.md, ADR-016 |
| Slide 19 (Release 2+) | functionality-gap-analysis-vs-current-design.md |
| Slide 20 (Close / decisions needed) | functionality-gap-analysis-vs-current-design.md (Tier B section) |
| Appendix A–I | Existing domain packs, ADRs, MASTER-DESIGN-DOCUMENT.md |

---

## Creation Priority

| Priority | Item | Blocking slide |
|---|---|---|
| 1 | DIAG-02 — System architecture | Slide 4 — early in deck, sets context |
| 2 | DIAG-01 — Debt lifecycle | Slide 2 — second slide in deck |
| 3 | SUM-02 — Requirements coverage | Slide 7 — anchor for Section 3 |
| 4 | DIAG-04 — Entity model | Slide 10 — anchor for Section 4 |
| 5 | DIAG-05 — Three-tier config | Slide 12 — anchor for Section 5 |
| 6 | DIAG-06 — Policy bundle lifecycle | Slide 13 |
| 7 | SUM-03 — Demo flow matrix | Slide 14 |
| 8 | SUM-04 — Release scope table | Slide 18 |
| 9 | DIAG-03 — Delegate command pattern | Slide 5 — technical, lower leadership priority |
| 10 | SUM-01 — ADR digest | Slide 6 — technical, lower leadership priority |
