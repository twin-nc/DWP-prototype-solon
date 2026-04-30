# Handoff: RULING-013 Statute-Barred Clock Reset Timing

**From:** DWP Debt Domain Expert  
**To:** Business Analyst, Sprint 3 Developers  
**Date:** 23 April 2026  
**Status:** Implementation-ready (3 open client questions to resolve)

---

## What Has Been Delivered

### 1. RULING-013: Statute-Barred Clock Reset Timing

**File:** `/docs/project-foundation/domain-rulings/RULING-013-statute-barred-clock-reset-timing.md`

**Scope:**
- Resolves the implementation question: **Can the system delay flag clearance by up to 24 hours (nightly batch cycle) without violating the Limitation Act 1980 or FCA guidance?**

**Answer:** YES. The Limitation Act 1980, ss.29–30 do not impose real-time administrative update requirements. A delay of up to 24 hours is legally defensible.

**Key findings:**
- The statute uses prospective language ("shall be deemed to have accrued") referring to *when the new limitation period starts*, not when the creditor updates records.
- FCA Consumer Duty and Treating Customers Fairly (TCF) mandate fair treatment when a debtor engages (payment, acknowledgement, arrangement), but do not prescribe timing for record updates.
- DWP benefit debt has unique policy overlays — client confirmation required on post-reset grace period (DDE-OQ-13).

**Rule statement:** Once a valid RULING-012 event is recorded (acknowledgement, cleared payment, or repayment arrangement), the system MUST NOT take collections-related enforcement action until the flag is confirmed false. This is a **real-time behavioral obligation**, independent of flag clearance timing.

**Recommended implementation:** Event listener (async post-commit) + nightly batch job (dual-path). This is already in the current codebase design (ADR-002, development plan Sprint 3).

---

### 2. DDE-OQ Summary Document

**File:** `/docs/project-foundation/domain-rulings/DDE-OQ-STATUTE-BARRED-SUMMARY.md`

**Purpose:** Quick reference for Delivery Lead to engage DWP client. Three client choices are needed before implementation:

| Question | Recommendation | Impact |
|---|---|---|
| **DDE-OQ-11:** Evaluator timing (sync vs. async vs. nightly only) | Option B: Async post-commit (non-blocking) | Affects thread model, error handling, latency |
| **DDE-OQ-12:** Suppression log integration (yes/no) | Yes (auditable decision point) | Schema change; adds `SUPPRESSION_LOG` reason |
| **DDE-OQ-13:** Grace period post-reset (mandatory waiting period) | Clarify with client | Affects forbearance logic post-clock-reset |

Each option has been documented with pros/cons and regulatory implications.

---

### 3. Implementation Checklist (Sprint 3 & 4)

**File:** `/docs/project-foundation/domain-rulings/RULING-013-IMPLEMENTATION-CHECKLIST.md`

**Structure:**
- **Phase 1 (Week 1):** Foundation — database schema, `StatuteBarredEvaluator` service, nightly job, event listener, audit trail
- **Phase 2 (Week 2):** BPMN integration — statute-barred gateway, signal handler, `segmentation.dmn` update
- **Phase 3 (Conditional):** Suppression log integration (if DDE-OQ-12 = Yes)
- **Phase 4 (Conditional):** Grace period implementation (if DDE-OQ-13 requires it)
- **Phase 5 (Ongoing):** Integration testing (happy path, BPMN workflows, configuration, error handling)
- **Phase 6 (Week 9):** Demo preparation and scenario walkthrough

**Acceptance criteria:** 7 explicit criteria covering flag transitions, BPMN routing, audit trails, and feature flags.

**Code review checklist:** Captures common pitfalls (caching, transaction boundaries, missing audit events).

---

## Status: Ready for BA & Developers

### What the BA Needs to Do

1. **Read RULING-013** (`statute-barred-clock-reset-timing.md`)
   - Understand the regulatory basis (Limitation Act ss.29–30, FCA guidance)
   - Understand the rule statement (real-time behavioral obligation for enforcement halt)
   - Understand the recommended implementation approach (dual-path: async event + nightly job)

2. **Confirm DWP Answers** (with Delivery Lead)
   - **DDE-OQ-11:** Which evaluator timing approach? (Option A/B/C)
   - **DDE-OQ-12:** Suppress statute-barred as suppression reason? (Yes/No)
   - **DDE-OQ-13:** Mandatory grace period post-reset? (Days if yes)
   - Document answers in a new file: `/docs/project-foundation/domain-rulings/DDE-OQ-STATUTE-BARRED-DECISIONS.md`

3. **Refine Acceptance Criteria** (if needed)
   - The checklist has 7 baseline criteria; add client-specific criteria based on DWP answers
   - Ensure each story in Sprint 3 backlog references RULING-013 in the requirements section

4. **Create Sprint 3 Stories** (from the checklist)
   - Story 1: StatuteBarredEvaluator service + unit tests
   - Story 2: StatuteBarredCalculationJob (nightly) + feature flag
   - Story 3: LimitationClockResetEvent listener (sync/async based on DDE-OQ-11)
   - Story 4: COLLECTION_PROCESS.bpmn statute-barred gateway + signal handler
   - Story 5: segmentation.dmn statute-barred rule + tests
   - Story 6: Audit trail for flag transitions
   - (Story 7–8 conditional on DDE-OQ-12/13)

### What the Developers Need to Do

1. **Read the Implementation Checklist** (`RULING-013-IMPLEMENTATION-CHECKLIST.md`)
   - It is structured by phase and by task
   - Each checklist item is a concrete code/test requirement
   - Follow the sequence; do not skip phases

2. **Implement Phases 1–2** (mandatory, Sprint 3)
   - Phase 1: Service layer, nightly job, event listener, audit trail
   - Phase 2: BPMN integration, gateway, signal handler, DMN rules
   - **Duration:** Estimate 8 story points (4 points Phase 1, 4 points Phase 2)

3. **Implement Phases 3–4** (conditional, Sprint 3 or 4)
   - Implement only if DWP confirms DDE-OQ-12 or DDE-OQ-13
   - Estimate 3 points Phase 3 (if yes), 2 points Phase 4 (if required)

4. **Testing**
   - Follow the integration testing matrix (Phase 5)
   - Achieve >85% code coverage for new classes
   - Prepare demo scenario (Phase 6) for 8 July tender demo

### What Blocks This Work

**BLOCKING:** DWP Client confirmation of:
- DDE-OQ-11: Evaluator timing approach
- DDE-OQ-12: Suppression log integration
- DDE-OQ-13: Grace period requirement

**Unblock path:**
1. Delivery Lead sends three questions to DWP client contact by **24 April 2026**
2. Expect response by **30 April 2026** (4 working days)
3. File answers in `DDE-OQ-STATUTE-BARRED-DECISIONS.md`
4. Update RULING-013 status from `awaiting-client-sign-off` to `final`
5. Sprint 3 implementation begins 5 May 2026

**If client does not respond by 30 April:**
- Implement **Option B + Yes + No** (async evaluator, suppression integration, no grace period) as defaults
- Document decision as "client default choice due to no response by deadline"
- Post-award: Refine to match actual client policy in maintenance release

---

## Architecture Decisions (Locked in ADR-002, ADR-003)

For developers' reference:

**ADR-002: StatuteBarredEvaluator — Boundary and Guardrails**
- Singleton service; sole authority on statute-barred calculation
- Both nightly job and event listener call this service (no duplication)
- When flag transitions `true → false`, send `STATUTE_BARRED_CLEARED` signal to active COLLECTION_PROCESS instances
- Limit clock-reset triggers to RULING-012 events only
- Read flag from DB at task execution time; never cache in process variables

**ADR-003: Platform-Wide Transaction Boundary Rule**
- Application writes (entity, suppression, audit) are **inside** `@Transactional`
- Flowable engine calls (RuntimeService, TaskService) are **outside** `@Transactional`
- Violating this creates two-phase commit problems that embedded Flowable doesn't support

---

## Regulatory References (For Context)

- **Limitation Act 1980, s.29–30:** Acknowledgement and payment reset the clock. Prospective language; no real-time update requirement.
- **FCA Guidance FG21/1:** Vulnerability flag requires process divergence; isolation from escalation.
- **FCA Consumer Duty / TCF:** Forbearance obligations apply when debtor engages (payment, acknowledgement, arrangement). Implies consideration before immediate escalation post-reset.
- **DWP Internal Policy:** Benefit debt recovery has specific overlays; requires client confirmation on grace period (DDE-OQ-13).

---

## Key Files Summary

| File | Purpose | Audience |
|---|---|---|
| `RULING-013-statute-barred-clock-reset-timing.md` | Legal ruling; regulatory basis; rule statement; edge cases; open questions | BA, Developers, Delivery Lead |
| `DDE-OQ-STATUTE-BARRED-SUMMARY.md` | Quick reference for three client choice questions | Delivery Lead (to send to DWP) |
| `RULING-013-IMPLEMENTATION-CHECKLIST.md` | Detailed sprint plan; phase-by-phase checklist; AC criteria; demo scenario | Developers, BA |
| This file | Handoff summary; what's ready, what's blocked, next steps | All roles |

---

## Next Steps (By Role)

### Delivery Lead (Today – 24 April)
1. Review RULING-013 for context
2. Prepare email to DWP contact with three questions from `DDE-OQ-STATUTE-BARRED-SUMMARY.md`
3. Request response by 30 April 2026
4. File the response in `DDE-OQ-STATUTE-BARRED-DECISIONS.md` (or notify team of default choice)

### Business Analyst (25–30 April)
1. Read RULING-013 in detail
2. Refine acceptance criteria (add DWP-specific criteria based on client answers)
3. Create Sprint 3 backlog stories from RULING-013-IMPLEMENTATION-CHECKLIST.md
4. Link each story to RULING-013 in the requirements section
5. Estimate and prioritize (suggest 8 points baseline for Phases 1–2)

### Developers (5 May – 4 June, Sprint 3)
1. Check out RULING-013-IMPLEMENTATION-CHECKLIST.md
2. Implement Phase 1 (week 1) and Phase 2 (week 2) in parallel
3. Phase 1 owner: Dev 2 (StatuteBarredEvaluator, job, listener)
4. Phase 2 owner: Dev 1 (BPMN, DMN, signal handler)
5. Both: Integration tests (Phase 5, ongoing)
6. Prepare demo scenario (Phase 6, final week)

---

## Appendix: Regulatory Q&A

**Q: Why doesn't the Limitation Act require real-time updates?**
A: The statute focuses on when the *new limitation period begins* ("shall be deemed to have accrued"), not on the creditor's administrative timeliness. The debtor's legal protection kicks in on the date of the event, not on the date the creditor updates its records.

**Q: Is a 24-hour delay legal?**
A: Yes. The system must *behave correctly* (halt enforcement) based on the event, independent of when the flag is updated. A delay until the next nightly job is legally defensible.

**Q: What if a collection action proceeds during the flag-clearance window?**
A: If the action is traceable to a **decision gate that reads the flag from the database at execution time** (not from cached variables), the system is defensible. Recommend implementing suppression log integration (DDE-OQ-12 = Yes) as defense-in-depth.

**Q: Does DWP benefit debt follow the Limitation Act 1980?**
A: Benefit debt itself is statutory, not governed by the Limitation Act. However, overpayment debt and any contractual elements (e.g., third-party recovery costs) may be governed by the Limitation Act. DWP policy may also impose additional restrictions (grace period, mandatory review). Confirmation required via DDE-OQ-13.

**Q: Why async over sync?**
A: Async (post-commit, non-blocking) scales better for high-volume accounts and follows Spring event best practices. It introduces a small window (<1 minute) where the flag may still be true in the DB, but this is mitigated by reading at execution time and suppression log integration.

---

## Sign-Off

**Domain Expert:** Ready to hand off to BA and Developers.

**Status:** 
- RULING-013: `awaiting-client-sign-off` on DDE-OQ-11/12/13
- Implementation: Unblocked for Phase 1–2 baseline design (Option B + Yes + No as defaults)
- Acceptance criteria: In checklist; ready for BA refinement

**Next review:** Once DWP confirms DDE-OQ answers, update RULING-013 status to `final` and file amendment.

---

**Issued:** 23 April 2026  
**Delivery Lead point of contact:** [Name and email]  
**DWP Domain Expert contact:** [Name and email]
