# Feature Flag Register

**Instructions:** Copy this file to `docs/project-foundation/FEATURE-FLAG-REGISTER.md`.
Any PR introducing a feature flag must add a row. Any PR removing a flag must remove the row.
See standard: `standards/platform/feature-flag-standard.md`

---

| Flag Name | Type | Scope | Owner | Issue | Introduced | Target Removal | Notes |
|---|---|---|---|---|---|---|---|
| `EXAMPLE_FLAG` | Release | Frontend | Frontend team | #0 | v0.1 | v1.0 | Template row — delete before use |

---

## Flag Types
- **Release** — controls a feature that is in progress; removed when the feature is fully rolled out
- **Experiment** — controls an A/B or gradual rollout; removed after experiment concludes
- **Ops** — controls operational behavior (e.g., circuit breaker, kill switch); may be permanent with review

## Lifecycle Rules
1. Every flag must have a `Target Removal` date or version. If no date is known, set a review checkpoint.
2. A flag that passes its target removal date without being removed becomes a tracked tech-debt item.
3. Ops flags that are intended to be permanent must be reviewed annually and re-confirmed.
4. Flags that affect CI test behavior **must** be explicitly set in all CI job env blocks.
