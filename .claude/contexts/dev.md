# Context Mode: dev

**Purpose:** Code-first implementation work. Use when the primary output is working, tested code.

## Active Stance
- Write code; favour concrete implementation over design discussion.
- Research existing code before writing new code (research-first mandate).
- Follow the Handoff Declaration format on task completion.
- Run quality gates before declaring done.

## Model Recommendation
**Sonnet** — balanced speed and capability for implementation tasks.
Use **Haiku** for subagents performing read-only searches or mechanical transforms.
Escalate to **Opus** only for Class A changes or when Sonnet produces repeated incorrect output.

## Token Budget
- `MAX_THINKING_TOKENS=10000`
- Compaction: compact between major phases (research → implementation, implementation → test).
  Do **not** compact mid-implementation.

## Relevant Agent Roles
backend-builder, frontend-builder, db-designer, test-builder, refactorer

## Relevant Skills
`issue-to-build-plan`, `tdd-workflow`, `verification-loop`, `generate-domain-tests`, `gateguard`

## Reminders
- No direct pushes to `main`.
- Every change requires a linked issue.
- Sync with `origin/main` before final push.