# Requirements to Functionality Traceability System

## Purpose

Define a practical many-to-many system for mapping:

- requirement IDs -> functionality slices
- functionality slices -> module ownership
- functionality slices -> contracts, tests, and evidence

This keeps module architecture stable while allowing real delivery work to span multiple modules.

## Decision Summary

Use this canonical model:

1. `Requirement <-> Functionality` is many-to-many.
2. Every functionality has exactly one `primary_module`.
3. Every functionality can have zero or more `supporting_modules`.
4. `Requirement -> Module` is derived from requirement-to-functionality links and must not be manually maintained as a separate source of truth.

## Canonical Artifacts

- Machine-readable trace artifact: `docs/project-foundation/trace-map.yaml`
- Module ownership baseline: `docs/project-foundation/functional-requirements-module-map.md`
- This operating guide: `docs/project-foundation/requirements-functionality-traceability-system.md`
- Starter template for new teams/slices: `docs/project-foundation/templates/TRACE-MAP-V2-TEMPLATE.yaml`

## Data Model

`trace-map.yaml` is organized into these sections:

1. `module_catalog`: stable module IDs and ownership boundaries.
2. `functionality_catalog`: deliverable behavior slices; each has one `primary_module` and optional `supporting_modules`.
3. `requirement_functionality_links`: many-to-many links from baseline requirement IDs to one or more functionality IDs.
4. `functionality_artifact_links`: contracts, policy bundles, tests, and evidence linked to each functionality.
5. `release_views`: optional release-level coverage snapshots for governance and signoff.
6. `legacy_flattened_trace_map`: backward-compatible per-requirement projection for audits and older consumers.

## ID Conventions

- Module IDs: `MOD-<AREA>-<NAME>`
- Functionality IDs: `FNC-<AREA>-<NNN>`
- Link IDs: `LNK-<NNNN>`
- Requirement IDs: keep baseline source IDs unchanged (for example `CAS.6`, `DW.12`, `MR7`)

Example functionality IDs:

- `FNC-ACCOUNT-001` debt lifecycle state transitions
- `FNC-RPF-003` arrangement setup and schedule generation
- `FNC-COMMS-002` outbound contact orchestration

## Operating Workflow

Run this sequence for every non-trivial issue or feature slice:

1. Define or select affected functionality IDs in `functionality_catalog`.
2. Confirm one primary module owner per functionality.
3. Add all affected requirement IDs to `requirement_functionality_links`.
4. Attach contracts, tests, and evidence targets to `functionality_artifact_links`.
5. Implement and test.
6. Update test run IDs and evidence references in the same PR cycle.
7. Update `legacy_flattened_trace_map` for any requirement touched.
8. Merge only when trace links are complete or explicitly marked `deviation` with approved rationale.

## Quality Rules

1. Do not close a requirement with only module links. A requirement must link to at least one functionality.
2. Do not mark functionality `complete` without at least one test suite ID and one evidence reference.
3. Class A requirements must include `domain_ruling` before status is set to `complete`.
4. Use `proposed` confidence for links inferred during planning; switch to `confirmed` in implementation PRs.
5. Keep historical IDs immutable; never recycle IDs.

## Review Gate Checklist

Before merge, confirm:

1. Every changed requirement ID has at least one functionality link.
2. Every touched functionality has exactly one primary module.
3. At least one test suite ID is linked per touched functionality.
4. Evidence reference is present (or marked as approved deferral).
5. `legacy_flattened_trace_map` reflects final status for touched requirements.

## Bootstrap Plan (Current Repository)

1. Seed all functional requirement IDs from the baseline into `legacy_flattened_trace_map`.
2. Create first-pass `functionality_catalog` entries for top delivery slices.
3. Map requirement families (`CAS`, `DIC`, `DW`, etc.) to initial functionality IDs.
4. Add release-view snapshot for the next planned release.
