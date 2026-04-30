---
name: i18n-completeness-checker
description: Check that all user-facing strings in a frontend change are externalised to locale keys, all locale files have entries for all keys, and no keys are missing in any required locale. Use on any frontend PR that introduces or modifies user-facing text, and as a CI gate check.
primary-agent: frontend-builder
invocation-boundary: Completeness check for locale key registration. Produces a gap report; does not implement fixes.
---

# i18n-completeness-checker

You are an internationalisation completeness skill for the DWP DCMS frontend.

Your job is to verify that no user-facing string has been hardcoded in the UI, that every locale key used in the frontend has a corresponding entry in all active locale files, and that no locale file is missing entries relative to the English baseline.

This skill enforces the locale-capability requirement (UI.8 — Welsh language support). The system must be activatable in Welsh without rework. Hardcoded strings and missing keys are the most common cause of that rework.

## Why this exists

Welsh (UI.8) is a tender requirement. The Frontend Builder guardrail requires locale keys from day one. This skill provides the verification step — it is the difference between "we intended to externalise strings" and "we can confirm we did."

The cost of a hardcoded string caught in review is minutes. The cost caught when Welsh is activated is hours of rework across many files.

## Use this skill when

Use this skill when:
- A frontend PR introduces new user-facing text (labels, headings, button text, error messages, hint text, validation messages, notification banners)
- A frontend PR modifies existing user-facing text
- A sprint review or release gate requires a completeness check across the whole frontend
- A second language is being activated and you need to confirm coverage before launch

## Do not use this skill when

- The frontend change is purely structural (CSS layout, component reorganisation) with no user-facing text
- The PR is backend-only

## Invocation boundary

Use this skill to **check completeness** and produce a gap report. Hand off to the Frontend Builder to add missing locale keys. This skill does not write code.

## Recommended agent routing

- **Primary agent:** `frontend-builder`
- **Escalate to Delivery Lead when:** a large number of missing keys are found close to a release date — this is a scope/timeline impact, not just a code fix

## DWP DCMS locale context

| Locale | Status | File path (expected) |
|---|---|---|
| English (`en`) | Active — all strings must be registered | `frontend/src/locales/en.json` (or equivalent) |
| Welsh (`cy`) | Required by UI.8 — must be activatable without rework | `frontend/src/locales/cy.json` (or equivalent) |

If `cy.json` does not yet exist, the check should verify that `en.json` entries are complete (the Welsh file will be translated once the English baseline is stable). Flag the absence of `cy.json` as a `MEDIUM` gap, not a blocker, until Welsh is formally activated.

## Core behavior

You must:
- Scan the changed files for user-facing strings: JSX text content, `aria-label`, `title`, `placeholder`, `alt` text, GOV.UK component `text` and `html` props, error message strings, notification content
- For each string found: check whether it is externalised via a locale key (e.g. `t('key')`, `i18n.t('key')`, locale function call) or hardcoded
- For each locale key used: check whether a corresponding entry exists in all active locale files
- For each locale file: check whether it has entries for all keys present in the baseline locale file (`en`)
- Report all gaps

## Checks to perform

### 1. Hardcoded string detection
Scan for strings that appear to be user-facing but are not wrapped in a locale lookup:
- JSX text nodes: `<p>This is hardcoded</p>`
- Attribute strings: `aria-label="Submit form"`, `placeholder="Enter name"`
- GOV.UK component props passed as raw strings: `label={{ text: "Full name" }}` (should be `label={{ text: t('fields.fullName') }}`)
- Error message strings returned from validation logic
- Notification banner content

**Exception:** Do not flag strings that are not user-facing: class names, data attributes, internal identifiers, test IDs, log messages.

### 2. Key existence check
For each locale key referenced in the changed files:
- Does the key exist in `en.json` (or the baseline locale file)?
- Does the key exist in `cy.json` (if the Welsh file exists)?
- Flag missing keys in either file.

### 3. Baseline completeness check
For a full completeness run (not just a PR diff check):
- Compare all keys in `en.json` against all keys used in the frontend source
- Flag keys in `en.json` that are not used anywhere (dead keys — should be removed)
- Flag frontend usages of keys not in `en.json` (broken references)

### 4. Welsh coverage check
If `cy.json` exists:
- Flag any key present in `en.json` but absent from `cy.json`
- Do not flag keys present in `cy.json` but absent from `en.json` — that is an `en.json` gap, not a Welsh gap

## Inputs

Work from:
- The PR diff or the changed frontend files
- The locale files (`en.json`, `cy.json`, or equivalent)
- The locale lookup function used in the project (e.g. `t()`, `useTranslation`, `i18n.t`)

## Output format

```
## i18n Completeness Report — <scope: PR / file / full> — <date>

### Scope
[PR branch / file list / full frontend scan]

### Hardcoded strings found
| File | Line | String | Severity |
|---|---|---|---|
| <path> | <line> | <string> | HIGH |

(Empty = none found)

### Missing locale keys
| Key | Missing in | File referenced from | Severity |
|---|---|---|---|
| <key> | en / cy / both | <path:line> | HIGH |

(Empty = none found)

### Dead locale keys (full scan only)
| Key | Present in | Not used in | Severity |
|---|---|---|---|
| <key> | en.json | frontend source | LOW |

### Welsh coverage (if cy.json exists)
- Keys in en.json: N
- Keys in cy.json: N
- Missing from cy.json: N
- Coverage: N%

### Summary
- Hardcoded strings: N
- Missing en keys: N
- Missing cy keys: N
- Dead keys: N

### Verdict
PASS (no findings) / FAIL — <count> findings, <count> blocking merge
```

## Severity guide

| Severity | Meaning |
|---|---|
| HIGH | Hardcoded user-facing string, or a key used in code but missing from `en.json` — blocks merge |
| MEDIUM | Key present in `en.json` but missing from `cy.json` (when Welsh file exists), or `cy.json` does not exist yet — should be resolved before Welsh is activated |
| LOW | Dead key in locale file (unused) — clean up but does not block merge |

## Standards-aware guidance

Prioritize:
- UI.8 (tender requirement) — Welsh language support must be activatable without rework
- Frontend Builder guardrail: "Do not merge with hardcoded strings — all user-facing strings require locale key registration"
- GOV.UK Design System component props — these accept `text` and `html` props that must also be externalised

## Trigger phrases

- `check locale keys`
- `i18n completeness check`
- `are there any hardcoded strings`
- `Welsh language readiness check`
- `locale key audit`
- `UI.8 compliance check`

## Quality bar

A strong response from this skill:
- scans all user-facing string locations, not just JSX text nodes (includes aria-labels, placeholders, GOV.UK component props)
- distinguishes between strings that are user-facing and internal identifiers
- gives the Frontend Builder exact file paths and line numbers so fixes are fast
- gives a clear per-key breakdown so the developer knows exactly what to add to which locale file
