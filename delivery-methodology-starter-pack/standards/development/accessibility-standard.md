---
id: STD-DEV-002
title: Accessibility Standard
status: Approved
owner: Frontend Engineering / QA
applies_to: All user-facing interfaces — agent UI, self-service portal, mobile application, any generated correspondence viewed in a browser
release_impact_if_violated: Release-blocking — non-compliance with WCAG AA is a Must have tender requirement (ACC01) and a legal obligation under the Public Sector Bodies Accessibility Regulations 2018 and Equality Act 2010
related_standards:
  - development/testing-standard.md
  - security/data-sensitivity-and-redaction.md
last_changed: 2026-04-16
---

## Purpose

Define mandatory accessibility requirements for all user-facing surfaces, derived from tender requirements ACC01–ACC03, COM13, and INT08–INT10. Non-compliance is not a deviation — it is a legal breach.

---

## Legal Basis

| Obligation | Source |
|---|---|
| WCAG 2.x AA conformance | ACC01, COM13 — Public Sector Bodies (Websites and Mobile Applications) (No.2) Accessibility Regulations 2018 |
| Assistive technology compatibility | ACC02 |
| Equality Act 2010 compliance | ACC03 |
| DWP Digital Design Authority accessibility sign-off | COM11c |
| DPIA covering accessibility data (e.g., disability flags) | COM11d |

---

## WCAG Conformance (MUST)

1. All user-facing interfaces MUST conform to **WCAG 2.x AA** at minimum. The specific published version in force at the time of delivery applies (COM13).
2. Conformance is assessed against all four principles: **Perceivable, Operable, Understandable, Robust** (POUR).
3. A valid **accessibility statement** MUST be published and kept current (ACC01). The statement must:
   - Declare the conformance level achieved
   - List any known non-conformances and their remediation timelines
   - Provide a contact mechanism for accessibility feedback
4. The accessibility statement MUST be updated whenever a known non-conformance is introduced or resolved.

---

## Assistive Technology Test Matrix (MUST)

Every release candidate MUST be tested against the following three categories of assistive technology (ACC02). At least one product from each category must pass without loss of functionality:

| Category | Required Products to Test |
|---|---|
| Screen reader | At least two from: JAWS, NVDA, VoiceOver (macOS/iOS), Narrator (Windows) |
| Screen magnifier | At least one from: ZoomText, Apple Zoom, Windows Magnifier |
| Voice recognition | At least one from: Dragon NaturallySpeaking, VoiceControl (macOS/iOS), Windows Speech Recognition |

Testing must cover:
- All primary agent workflows (account search, strategy view, repayment plan setup, note-taking)
- All form inputs (I&E capture, contact detail update, arrangement calculator)
- All notification and alert surfaces
- Mobile application (if applicable — MOB01-MOB13)

---

## Device and Browser Compatibility (MUST)

All interfaces MUST function correctly on (INT08):
- Windows 10+
- macOS v13+
- iOS v17+
- Android v12+

Responsive design is required — the interface MUST present on any screen size with minimal scrolling (INT10). No client-side plugins may be required; JavaScript, standard cookies, and first-party cookies are permitted (INT09).

---

## Responsive Design (MUST)

1. Layout MUST reflow at all viewport widths without horizontal scrolling for content (WCAG 1.4.10 Reflow).
2. Text MUST be resizable up to 200% without loss of content or functionality (WCAG 1.4.4).
3. Target touch areas MUST meet minimum size requirements (WCAG 2.5.5 / 2.5.8 at AA).

---

## Colour and Contrast (MUST)

1. Text contrast ratio MUST meet WCAG AA minimums:
   - Normal text (< 18pt or < 14pt bold): **4.5:1**
   - Large text (≥ 18pt or ≥ 14pt bold): **3:1**
2. UI component and graphical object contrast: **3:1** against adjacent colours (WCAG 1.4.11).
3. Information MUST NOT be conveyed by colour alone (WCAG 1.4.1). RAG status indicators (UI.6) MUST include a non-colour indicator (icon, label, or pattern).

---

## Forms and Error Handling (MUST)

1. All form inputs MUST have programmatically associated labels (WCAG 1.3.1, 3.3.2).
2. Error messages MUST:
   - Identify the field in error
   - Describe the error in plain language
   - Suggest a correction where possible (WCAG 3.3.3)
3. Required fields MUST be identified programmatically (not colour alone).
4. Timeout warnings MUST give users sufficient time to respond and must be dismissable (WCAG 2.2.1).

---

## Keyboard and Focus (MUST)

1. All functionality MUST be operable via keyboard alone (WCAG 2.1.1).
2. Focus order MUST be logical and meaningful (WCAG 2.4.3).
3. Focus indicator MUST be visible (WCAG 2.4.7; enhanced visibility at AA+: 2.4.11 if applicable version requires).
4. No keyboard traps (WCAG 2.1.2).

---

## Dynamic Content (MUST)

1. All dynamic content updates (e.g., strategy changes, queue updates, real-time alerts — UAAF.24) MUST be announced to screen readers via ARIA live regions where appropriate.
2. Modal dialogs and overlays MUST trap focus appropriately and return focus on close.
3. Loading states and progress indicators MUST be announced programmatically.

---

## Mobile Application (MUST)

For any mobile application (MOB01–MOB13):
1. MUST NOT use SSL Pinning (MOB11 — enables traffic inspection for accessibility tools).
2. MUST use Application Transport Security (MOB12).
3. MUST use standard OS encryption (MOB13).
4. MUST support the same assistive technology categories as the web interface.
5. Additional authentication layer (PIN or equivalent) MUST be accessible without assistive technology interference (MOB07).

---

## Accessibility Testing in CI (MUST)

1. Automated accessibility scanning MUST be included in the CI pipeline for all UI changes:
   - Tooling: {{to be confirmed — examples: axe-core, Pa11y, Lighthouse}}
   - Minimum: WCAG 2.x AA rule set
   - Gate: automated scan MUST pass with zero violations before a PR can merge
2. Automated scanning is a necessary but not sufficient gate — manual assistive technology testing (see matrix above) is required for release candidates.
3. Accessibility regression: if a previously passing interface fails an automated check, the PR is blocked. The failure is not deferrable without a deviation record.

---

## DWP Digital Design Authority Sign-off (MUST)

1. An accessibility compliance assessment MUST be completed and signed off by DWP before any user-facing interface goes to UAT (COM11c).
2. The assessment evidence must be included in the release evidence pack.
3. Any known non-conformance at sign-off time must be logged as a deviation record with a remediation timeline acceptable to DWP.

---

## Multilingual Considerations (SHOULD)

The interface SHOULD support English and Welsh where applicable (UI.8). If Welsh language support is implemented:
- All WCAG requirements apply equally to Welsh content.
- Screen reader language attributes MUST be set correctly for Welsh text (`lang="cy"`).
- Welsh content MUST be reviewed by a native speaker before release.

---

## Prohibited Practices

- **MUST NOT** use colour as the only means of conveying information (WCAG 1.4.1).
- **MUST NOT** use `display:none` or `visibility:hidden` to hide content that should be accessible to screen readers.
- **MUST NOT** use `tabindex` values greater than 0 (breaks natural focus order).
- **MUST NOT** suppress browser focus indicators without providing a custom equivalent.
- **MUST NOT** deploy to UAT or production without automated CI scan passing.
- **MUST NOT** defer DWP Digital Design Authority accessibility sign-off to post-UAT.

---

## Open Items

| Item | Owner | Status |
|---|---|---|
| Confirm automated accessibility tooling (axe-core vs Pa11y vs other) | Frontend lead | Pending tech stack decision |
| Confirm WCAG version in force at delivery date (2.1 or 2.2) | DWP client | Awaiting confirmation |
| Confirm Welsh language requirement scope (full UI or specific screens) | DWP client | Awaiting confirmation |
| DWP Digital Design Authority accessibility assessment process and timeline | DWP client | Awaiting onboarding |
