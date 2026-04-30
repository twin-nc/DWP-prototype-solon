#!/usr/bin/env node
/**
 * Hook: governance-capture
 * Event: PreToolUse (Bash)
 * Profile: standard, strict
 *
 * Emits structured governance events for:
 *   - Commands touching secrets (env vars, .env files, credential paths)
 *   - Commands requiring approval (force push, branch deletion, prod deploy)
 *
 * Events are written to .claude/governance-log.jsonl (append-only).
 * Does NOT block — it records. Blocking is handled by other hooks.
 */

const fs = require("fs");
const path = require("path");
const input = JSON.parse(process.stdin.read() ?? "{}");
const command = input?.tool_input?.command ?? "";

const secretPatterns = [
  /\bSECRET\b/, /\bPASSWORD\b/, /\bTOKEN\b/, /\bAPI_KEY\b/,
  /\.env\b/, /credentials/, /\.pem\b/, /\.key\b/,
];

const approvalPatterns = [
  /git push.*--force/, /git branch.*-[Dd]\b/, /kubectl.*delete/,
  /docker.*rm/, /helm.*uninstall/, /terraform.*destroy/,
];

const categories = [];
for (const p of secretPatterns) { if (p.test(command)) { categories.push("secret"); break; } }
for (const p of approvalPatterns) { if (p.test(command)) { categories.push("approval-required"); break; } }

if (categories.length === 0) process.exit(0);

const event = {
  timestamp: new Date().toISOString(),
  categories,
  command: command.substring(0, 200),
  tool: input?.tool_name,
};

const logPath = path.join(process.cwd(), ".claude", "governance-log.jsonl");
try {
  fs.mkdirSync(path.dirname(logPath), { recursive: true });
  fs.appendFileSync(logPath, JSON.stringify(event) + "\n");
} catch (_) { /* non-blocking — log failure is not fatal */ }

if (categories.includes("approval-required")) {
  console.error(`[governance-capture] APPROVAL-REQUIRED command detected: '${command.substring(0, 80)}...'\nThis event has been logged. Confirm you have human approval before proceeding.`);
}

process.exit(0);