---
name: drawio-diagram
description: Create or edit draw.io (.drawio XML) diagrams for presentations, architecture documentation, or technical communication. Use when the user asks to create a diagram, update a .drawio file, or produce a visual that will be used in a slide deck or document. Covers layout standards, colour conventions, connector routing, and export-readiness.
version: 1.0.0
---

# draw.io Diagram Skill

Produce production-ready draw.io XML diagrams suitable for presentation slides, architecture documentation, and technical communication. All output is valid `.drawio` XML that opens directly in VS Code (draw.io extension), draw.io desktop, or app.diagrams.net.

## When This Skill Applies

- User asks to create or update a `.drawio` file
- User needs a diagram for a presentation slide or leadership deck
- User asks for an architecture diagram, flow diagram, state machine, ERD, or layered box diagram
- User asks to improve or fix an existing diagram

---

## Tooling Rationale

Always use `.drawio` XML. Do not use Mermaid, matplotlib, or other programmatic renderers unless the user explicitly requires them.

| Tool | Problem |
|---|---|
| Mermaid C4 | No layout control; poor results at presentation scale |
| Python/matplotlib | Requires execution; produces static rasters; hard to maintain |
| draw.io | Opens natively; one-click PNG/SVG export; XML is editable by both Claude and humans |

---

## Slide-Ready Quality Standard

Every diagram produced by this skill must meet this bar before it is considered done.

### Canvas and Export

- Target a **16:9 widescreen canvas** (~1654 × 929 px at 96 dpi, or scaled equivalent). The diagram must fill the slide area without dead space and without requiring scrolling or cropping.
- Export-ready at **150 DPI minimum** for PNG; SVG at any scale. Set `width` and `height` on the root `mxGraph` element accordingly.
- Prefer SVG export for diagrams with many text labels; PNG for dense colour fills.

### Contrast and Legibility

- **High contrast only.** Avoid light grey text on white; avoid pastel fills with white text. Every label must remain legible when projected on a screen at distance.
- **Font size tradeoff:** prefer completeness over large fonts. Do not omit content to increase font size. But no label should be so small it is illegible when the file is zoomed to fill a screen.
- **White text on dark fills** (`#FFFFFF` on any fill darker than ~40% brightness).

### Layout Rules

- **No overlapping shapes.** Every box and connector must have clear whitespace around it. If a layout is too dense, split into two diagrams.
- **No crossing connectors.** Arrange nodes so connectors do not cross. Where a crossing is unavoidable, use a bridge/hop marker to make it explicit.
- **Consistent spacing.** Equal padding inside boxes; equal gap between adjacent boxes at the same level. Irregular spacing signals an unfinished diagram.
- **Label every connector** whose meaning is not unambiguous from the node names alone.

---

## XML Structure Patterns

### Document skeleton

```xml
<mxGraphModel dx="1422" dy="762" grid="1" gridSize="10" guides="1"
              tooltips="1" connect="1" arrows="1" fold="1" page="1"
              pageScale="1" pageWidth="1654" pageHeight="929"
              math="0" shadow="0">
  <root>
    <mxCell id="0"/>
    <mxCell id="1" parent="0"/>
    <!-- content cells here, all with parent="1" or a container id -->
  </root>
</mxGraphModel>
```

### Swimlane containers (system/service boundaries)

Use swimlanes for any named boundary (system, service, layer, team). Do **not** use plain rectangles for boundaries.

```xml
<mxCell id="boundary-1" value="System Name" style="swimlane;startSize=32;fillColor=#1565C0;fontColor=#FFFFFF;strokeColor=#0D47A1;fontStyle=1;fontSize=13;" vertex="1" parent="1">
  <mxGeometry x="80" y="80" width="800" height="500" as="geometry"/>
</mxCell>
```

- `startSize=32` reserves the header bar.
- Children of the swimlane set `parent="boundary-1"` and use **relative** coordinates inside the container.
- Nest sub-boundaries (e.g. a monolith inside a system boundary) as swimlane children of the outer swimlane.

### Individual component boxes

Each component, package, or service gets its own box. Do not list multiple items as plain text inside a single box.

```xml
<mxCell id="pkg-1" value="&lt;b&gt;package-name&lt;/b&gt;&lt;br/&gt;brief description line"
        style="rounded=1;whiteSpace=wrap;html=1;fillColor=#283593;fontColor=#FFFFFF;strokeColor=#1A237E;fontSize=11;"
        vertex="1" parent="boundary-1">
  <mxGeometry x="20" y="60" width="160" height="60" as="geometry"/>
</mxCell>
```

- Use `&lt;b&gt;...&lt;/b&gt;` for bold titles within HTML labels.
- Grid layout: prefer 4 columns × N rows for package-style diagrams.
- Full-width row for items that logically span the width.

### Connectors

Use orthogonal routing for all connectors. Set explicit port anchors to prevent diagonal lines.

```xml
<mxCell id="conn-1" value="label" style="edgeStyle=orthogonalEdgeStyle;rounded=0;orthogonalLoop=1;jettySize=auto;exitX=0.5;exitY=1;exitDx=0;exitDy=0;entryX=0.5;entryY=0;entryDx=0;entryDy=0;"
        edge="1" source="pkg-1" target="pkg-2" parent="1">
  <mxGeometry relative="1" as="geometry"/>
</mxCell>
```

- `exitX/exitY`: which side the arrow leaves the source (0=left, 0.5=centre, 1=right; 0=top, 0.5=middle, 1=bottom).
- `entryX/entryY`: which side the arrow enters the target.
- Label every connector unless its meaning is unambiguous from context.

---

## Colour Palette

This palette is validated for projection legibility. Use it as a starting point; adapt to the project's visual identity but maintain contrast ratios.

| Layer / Role | Fill | Stroke | Text |
|---|---|---|---|
| User / Actor | `#37474F` | `#263238` | `#FFFFFF` |
| Frontend service | `#1565C0` | `#0D47A1` | `#FFFFFF` |
| Backend service | `#1565C0` | `#0D47A1` | `#FFFFFF` |
| Database / Infrastructure | `#2E7D32` | `#1B5E20` | `#FFFFFF` |
| Domain package | `#283593` | `#1A237E` | `#FFFFFF` |
| Port / Interface layer | `#6A1B9A` | `#4A148C` | `#FFFFFF` |
| Process infrastructure | `#AD1457` | `#880E4F` | `#FFFFFF` |
| External system | `#5D4037` | `#3E2723` | `#FFFFFF` |
| Decision / DMN node | `#E65100` | `#BF360C` | `#FFFFFF` |
| State / Status box | `#00695C` | `#004D40` | `#FFFFFF` |
| Warning / Regulatory callout | `#F57F17` | `#E65100` | `#000000` |
| Neutral / Note | `#F5F5F5` | `#9E9E9E` | `#212121` |

---

## Diagram Type Recipes

### Layered box / isolation chain

Use for showing architectural layers, dependency direction, or isolation boundaries. Stack layers vertically. Use downward arrows between layers labelled with what crosses the boundary.

Minimum structure:
1. Top layer (caller) — swimlane or box
2. Interface/port layer — purple box
3. Implementation layer — crimson box
4. Engine/runtime — deep red box

### State machine

- One rounded rectangle per state, neutral fill.
- Directed connectors labelled with the triggering event.
- Annotate who or what triggers each transition (role or system actor).
- Show the normal path (solid line) and exception/rollback path (dashed line).
- Entry and exit states may use a filled circle (start) and double circle (end).

### Entity / ERD

- One box per entity, domain-package fill colour.
- Relationship lines use crow's foot notation or explicit `(1)` / `(many)` labels on connectors.
- Show only the fields specified — do not add field-level detail beyond what is requested.

### Flow diagram

- Use decision diamonds (`rhombus` style in draw.io) for branching.
- Horizontal flows: left-to-right. Vertical flows: top-to-bottom. Do not mix orientations within one diagram.
- Stage labels above each swimlane column/row.

---

## Standard Furniture

### Legend

Include a legend only when colour usage would not be obvious to a viewer unfamiliar with the diagram. A legend is **not** needed if every shape is directly labelled and colour is purely decorative or follows an obvious convention (e.g. red = warning, green = active).

When a legend is warranted, place it outside and below-left of the main boundary, with one small coloured box and label per layer type used.

```xml
<mxCell id="legend" value="Legend" style="swimlane;startSize=24;fillColor=#EEEEEE;strokeColor=#9E9E9E;fontSize=11;" vertex="1" parent="1">
  <mxGeometry x="20" y="820" width="260" height="90" as="geometry"/>
</mxCell>
```

### Footer note

A single italic line below the main boundary summarising the 2–3 key constraints or decisions visible in the diagram. Keep it to one line — this is a prompt for the viewer, not a caption.

```xml
<mxCell id="footer" value="&lt;i&gt;Note: ...&lt;/i&gt;"
        style="text;html=1;align=left;verticalAlign=middle;resizable=0;points=[];autosize=1;fontSize=10;fontColor=#616161;"
        vertex="1" parent="1">
  <mxGeometry x="80" y="820" width="600" height="30" as="geometry"/>
</mxCell>
```

---

## Output Protocol

1. **Read any existing `.drawio` file** before editing. Never overwrite without reading first.
2. **Produce complete valid XML.** The output must open without errors. Do not produce partial snippets unless the user explicitly asks for a snippet.
3. **Write to the path specified by the user** (or the path inferred from context).
4. **Confirm what was produced**: state the diagram type, node count, and output path in one sentence.
5. **Do not open or render the file.** The user will open it in their draw.io viewer.
