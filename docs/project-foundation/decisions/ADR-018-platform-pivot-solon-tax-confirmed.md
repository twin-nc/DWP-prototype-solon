# ADR-018: Platform Pivot — Solon Tax Confirmed as Base Platform

## Status

**ACCEPTED** — Supersedes ADR-016.

## Date

2026-04-30

---

## Context

A confirmed programme-level decision has been made to pivot the DCMS build strategy. The previous direction (ADR-016: Option C — greenfield) is superseded. This ADR records the new direction and its implications for all prior decisions.

This decision was made at programme/client level. The technical blockers documented in ADR-016 remain on record as accumulated knowledge and must inform the new design process — they are not discarded. However, the conclusion of ADR-016 (Option C locked) no longer stands.

---

## Decision

**DCMS will be built on top of Solon Tax as its base platform.**

The system must still satisfy all DWP functional and non-functional requirements. Solon Tax will be used as much as possible. Where Solon Tax does not satisfy a requirement, new code will be written on top of it.

**The user interface will be a new, custom-built React application.** It is intended to look and feel like Solon Tax, but it is not Solon Tax's own UI. The UI will be built from scratch, styled to resemble Solon Tax's visual language.

---

## Implications

### All Prior ADRs Are Unlocked

ADR-001 through ADR-017 are no longer locked. Every prior decision is open for renewed debate in the new design process. This does not mean they are wrong — the analysis behind them remains valuable input. It means no prior ADR may be treated as a constraint or a default without re-evaluation in the context of the Solon Tax platform.

Agents and developers must not cite any prior ADR as a settled decision or building block without first confirming it has been re-evaluated under this new direction.

### Team Size and Deadline Are Not Constraints

The number of human developers on this project is not specified and must not be used as an architectural constraint. No timeline or deadline is in force. Neither team size nor deadline should appear in documentation as a constraint that shapes design decisions.

### Accumulated Knowledge Is Retained

All domain rulings, domain packs, requirements analysis, gap analyses, and feasibility work remain in place as design inputs. The knowledge gathered under the previous direction is valuable precisely because it will inform the new design process. Documents containing that knowledge are marked **UNSETTLED** — meaning their conclusions are open for debate, not that their content is discarded.

### New Design Process

A new design process will be conducted to determine how DCMS capabilities are delivered on top of Solon Tax. This process must consider:

- What Solon Tax provides natively that satisfies requirements
- What must be built on top of Solon Tax
- Which prior ADR decisions remain valid, which need revision, and which are superseded by Solon Tax platform capabilities
- How the new React UI integrates with and presents Solon Tax data and processes

---

## What ADR-016 Documented That Remains Relevant

ADR-016 contains a detailed module-by-module analysis of Solon Tax v2.3.0 capabilities. The following findings from that analysis are retained as design inputs and should inform the new design process:

- The breathing space / non-interrupting event boundary analysis (Blocker 1) remains a live design problem that must be solved — the new design process must address how this regulatory obligation is met on the Solon Tax platform.
- The DMN vs Drools analysis (Blocker 2) remains relevant to configurability tier design.
- The Java 17 vs Java 21 analysis (Blocker 3) remains relevant to compile-time safety decisions.
- The module-by-module scope assessment (account, payment, repaymentplan, workallocation, etc.) remains a useful starting point for identifying where Solon Tax contributes and where custom code is needed.

These are inputs to the new design process, not settled outcomes.

---

## Cross-References

| Reference | Relevance |
|---|---|
| ADR-016 | Superseded by this ADR. Analysis retained as design input. |
| `solon-tax-feasibility-analysis.md` | Full feasibility analysis — conclusion superseded, analysis retained. |
| `solon-tax-platform-reference.md` | Platform reference for Solon Tax v2.3.0 capabilities. Now a primary input. |
| `amplio-process-engine-reference.md` | Amplio process engine reference. Now a primary design input. |
| `Functional-Requirements-Consolidated.md` | Requirements baseline. Unchanged — still the authority. |
| All ADR-001–ADR-017 | Unlocked. Status changed to UNDER REVIEW. |
