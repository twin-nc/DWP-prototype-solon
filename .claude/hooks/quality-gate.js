#!/usr/bin/env node
/**
 * Hook: quality-gate
 * Event: PostToolUse (Edit, Write)
 * Profile: standard, strict
 *
 * Runs formatter/linter after file edits to catch quality issues early.
 * Emits a warning (non-blocking) if the check fails so the agent can self-correct.
 *
 * Adapt the commands below for your stack. Examples provided for:
 *   - TypeScript/JavaScript (eslint + prettier)
 *   - Java (checkstyle via Maven)
 *
 * Set ECC_QUALITY_GATE_BLOCKING=true in env to make failures blocking (exit 2).
 */

const { execSync } = require("child_process");
const path = require("path");
const input = JSON.parse(process.stdin.read() ?? "{}");
const filePath = input?.tool_input?.file_path ?? "";

if (!filePath) process.exit(0);

const ext = path.extname(filePath);
const blocking = process.env.ECC_QUALITY_GATE_BLOCKING === "true";

let command = null;

if ([".ts", ".tsx", ".js", ".jsx"].includes(ext)) {
  command = `npx eslint --max-warnings 0 "${filePath}" 2>&1`;
} else if (ext === ".java") {
  // Maven checkstyle — requires mvn on PATH and checkstyle config present
  command = `mvn -q checkstyle:check -Dcheckstyle.includeTestSourceDirectory=false 2>&1 | tail -20`;
}

if (!command) process.exit(0);

try {
  execSync(command, { stdio: "pipe" });
} catch (e) {
  const output = e.stdout?.toString() ?? e.message;
  console.error(
    `[quality-gate] Quality check failed for '${path.basename(filePath)}':\n${output.substring(0, 500)}\n` +
    `${blocking ? "BLOCKED — fix before continuing." : "Advisory — review and fix before committing."}`
  );
  process.exit(blocking ? 2 : 0);
}

process.exit(0);