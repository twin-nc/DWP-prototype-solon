#!/usr/bin/env node
/**
 * Hook: block-no-verify
 * Event: PreToolUse (Bash)
 * Profile: minimal, standard, strict
 *
 * Blocks any git command that uses --no-verify or --no-gpg-sign.
 * Rationale: pre-commit hooks enforce standards; bypassing them masks real failures.
 */

const input = JSON.parse(process.stdin.read() ?? "{}");
const command = input?.tool_input?.command ?? "";

const forbidden = ["--no-verify", "--no-gpg-sign", "-c commit.gpgsign=false"];

for (const flag of forbidden) {
  if (command.includes(flag)) {
    const msg = `[block-no-verify] BLOCKED: '${flag}' detected in git command.\n` +
      `Git hooks exist to enforce standards. Fix the underlying issue instead of bypassing the hook.\n` +
      `If this is a legitimate emergency, follow the EMERGENCY-BYPASS-PROCEDURE.md and get human approval first.`;
    console.error(msg);
    process.exit(2); // exit code 2 = block with message
  }
}

process.exit(0);