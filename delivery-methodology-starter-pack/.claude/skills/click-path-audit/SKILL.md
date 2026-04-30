---
name: click-path-audit
description: >
  Trace every user-facing button and touchpoint through its complete state
  change sequence to find interaction bugs invisible to unit tests — sequential
  undo, async races, stale closures, and missing transitions.
invocation: /click-path-audit
inputs:
  - name: component_path
    required: false
    description: File path of the component or page to audit
  - name: user_journey
    required: false
    description: Named journey to focus on (e.g. "repayment plan creation", "vulnerability flag")
outputs:
  - name: audit_report
    description: Per-touchpoint execution trace, conflict findings, and fix recommendations
roles:
  - frontend-builder
---

# Click-Path Audit

## Purpose
Unit tests verify that individual functions work. Click-path audit verifies that sequential user interactions produce the correct final UI state. The failure mode it targets is invisible to static analysis: function A works, function B works, but calling A then B leaves the UI in a wrong state because B undoes what A did.

This matters especially for GOV.UK Design System multi-step flows — debt assessment, repayment plan creation, vulnerability flagging — where the user performs several sequential actions before reaching a final state, and a single interaction cancelling a prior one causes silent data loss or a misleading UI.

**Guard:** at least one of `component_path` or `user_journey` must be provided. "Audit all interactive elements" is unbounded and will not produce a useful report. If both are omitted, ask the user to scope to a component or a named journey before proceeding.

## When to Use
- After refactoring a state store (adding, removing, or renaming actions)
- After adding new actions to an existing Redux/Zustand/Context slice
- When a user reports a button that "does nothing" despite no console errors
- After a major component restructure or page-level state lift
- Before raising a PR that touches shared state across multiple components
- When a component renders incorrectly on first load (Phase 0 covers this case)

## Do Not Use This Skill When
- The bug is a compilation or type error — fix that first
- **The bug reproduces on first interaction with no prior state** — begin with Phase 0 (Initial Render Check) rather than skipping straight to sequential tracing
- You need end-to-end scenario coverage — use `generate-e2e-scenarios`

## Bug Categories

| Category | Description | Signal | Severity |
|---|---|---|---|
| **Wrong initial render** | Component renders incorrectly before any user interaction | Visual or data error visible immediately on page load | Critical |
| **Sequential undo** | A later action resets state set by an earlier action | Final state matches initial state despite user input | Critical |
| **Async race condition** | Two async operations resolve in unpredictable order | Flaky final state; outcome depends on network timing | Critical |
| **Stale closure** | Event handler captures a stale variable value | Action uses outdated data; ignores the latest state | Critical |
| **Missing state transition** | A handler is wired to a button but has no implementation | Button appears functional; nothing changes in state | Critical |
| **Dead code path** | A conditional branch is never reachable given valid input | Feature appears to exist but can never be triggered | Warning |
| **useEffect interference** | A lifecycle hook fires in response to state change and reverses a user action | State briefly flashes correct then resets | Critical |
| **ARIA state mismatch** | Accessible state (`aria-expanded`, `aria-selected`, `aria-disabled`) does not reflect the component's visual or logical state after an interaction | Screen reader announces wrong state; GOV.UK DS component behaves correctly visually but fails accessibility audit | Warning |

**Severity definitions:**
- **Critical** — user cannot complete the journey, or data is silently lost or corrupted.
- **Warning** — user experience is degraded or accessibility is impaired, but the journey can still complete.
- **Info** — a latent risk with no currently observable symptom (e.g. a dependency array that could cause a future race).

## Steps

### Phase 0 — Initial Render Check
Run this phase first, even for sequential bugs. A wrong initial render invalidates all subsequent interaction tracing.

1. Identify the component's initial state: what props are passed in, what the store slice contains at mount time, and what the initial values of all `useState` hooks are.
2. Trace every `useEffect` with an empty dependency array `[]` — these run once on mount and can set or override initial state.
3. Check any data-fetching effect: what does the component render before the fetch resolves? Is there a loading state? If the fetch is slow, does the component render stale or empty data?
4. For GOV.UK DS components (`<radios>`, `<checkboxes>`, `<select>`): confirm the initially selected value matches the store state or the prop passed in — not the DOM default.
5. If the initial render is correct, proceed to Phase 1. If it is wrong, classify as **Wrong initial render** and fix before continuing.

### Phase 1 — Map State Actions
1. Identify the state store(s) used by the component or journey.
2. List every action/dispatch call and what state it sets, clears, or transforms.
3. List every `useEffect` and identify which state changes trigger it and what it does.
4. Record the initial state values for all relevant slices.

### Phase 2 — Trace Each Touchpoint
For every user-facing button, link, form submission, and input change in scope:
1. Record the element label and its handler.
2. Follow the handler to every state mutation it triggers, including async side effects.
3. Note which other actions or effects are triggered as a consequence.
4. Record the expected final state after this single interaction. Use the acceptance criteria for the relevant story or domain rule as the reference — not the current UI behaviour. If no acceptance criteria exist, use the GOV.UK Design System guidance for the component pattern as the fallback reference.
5. For GOV.UK DS components (`<details>`, `<accordion>`, `<radios>`, `<checkboxes>`), also record the expected ARIA state after the interaction (e.g. `aria-expanded="true"`).

### Phase 3 — Test Sequential Combinations
"Simulate" means trace the reducer/handler chain in code — follow each dispatch through the Redux/Zustand/Context action, read the resulting state shape, and reason about what the next render would produce. This is a static code trace, not browser testing.

For each multi-step path in the journey:
1. Apply each interaction in order, updating state at each step.
2. After each step, check whether a prior state value has been overwritten unexpectedly.
3. Specifically test: first action then second action, second action then first action, and repeated same action.
4. Flag any combination where the final state does not match what the acceptance criteria or GOV.UK DS guidance implies.

### Phase 4 — Classify and Report
For each finding:
1. Assign a bug category from the table above.
2. Identify the specific file, function, and line where the conflict originates.
3. Propose the minimum fix (reorder effects, add a guard condition, correct the dependency array, fix the action implementation, correct the initial state derivation).

## Output Contract

```
## Click-Path Audit Report

Component / journey: <name>
State stores in scope: <list>

### Phase 0 — Initial Render
Initial render: CORRECT / WRONG
Findings (if wrong): <description>

### Touchpoint Map
| Element | Handler | State mutations | Side effects |
|---------|---------|-----------------|--------------|
| <label> | <fn>    | <list>          | <list>       |

### Findings
| # | Category | Path | File:Line | Description | Recommended Fix |
|---|----------|------|-----------|-------------|-----------------|
| 1 | Sequential undo | A -> B | DebtForm.tsx:84 | submitDraft clears accountId set by selectAccount | Move accountId into a ref or restructure action order |

### Summary
Total touchpoints traced: <N>
Findings: <N critical> / <N warning> / <N info>
OVERALL: CLEAN / ISSUES FOUND
```

## Guardrails
- Always run Phase 0 before sequential tracing — a wrong initial render invalidates all downstream state expectations.
- Do not propose fixes that change behaviour outside the declared component or journey scope.
- Do not mark a journey as clean if any `useEffect` dependency array contains values that are mutated inside the same effect — this is always a latent race.
- Do not skip async paths — trace what happens when a fetch resolves slowly, not just the happy-path timing.

## Integration
- Run `verification-loop` after applying fixes to confirm no regressions.
