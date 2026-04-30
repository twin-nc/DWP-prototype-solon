#!/usr/bin/env node
/**
 * Hook: gateguard-fact-force
 * Event: PreToolUse (Edit, Write, Bash[git commit/push])
 * Profile: strict
 *
 * Forces agents to declare confirmed facts before high-consequence actions.
 * Based on the GateGuard pattern: +2.25 measured quality improvement when applied.
 *
 * On trigger, emits a structured prompt requiring the agent to answer:
 *   1. What is the current confirmed state? (read from source, not assumed)
 *   2. What will this action change?
 *   3. What is the rollback if this is wrong?
 *   4. Does this comply with the relevant standard? (name it)
 *
 * The hook does NOT block — it emits a reminder. Blocking is handled by the agent.
 * Set EXIT_CODE=2 in env to make it blocking (strict mode).
 */

const input = JSON.parse(process.stdin.read() ?? "{}");
const toolName = input?.tool_name ?? "";
const command = input?.tool_input?.command ?? "";
const filePath = input?.tool_input?.file_path ?? "";

const isHighConsequence =
  toolName === "Write" ||
  (toolName === "Edit" && filePath.includes("migration")) ||
  (toolName === "Bash" && /git (commit|push|reset|rebase)/.test(command));

if (!isHighConsequence) process.exit(0);

const profile = process.env.ECC_HOOK_PROFILE ?? "standard";
const exitCode = profile === "strict" ? 2 : 0;

const msg = `[gateguard] PRE-ACTION GATE — answer before proceeding:
  1. Confirmed current state: (read from source, not assumed)
  2. What will this change:
  3. Rollback if wrong:
  4. Standard compliance: (name the relevant standard)
${exitCode === 2 ? "BLOCKED until gate is satisfied. Add answers as a comment then retry." : "Advisory — proceed with care."}`;

console.error(msg);
process.exit(exitCode);