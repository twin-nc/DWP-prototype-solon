#!/usr/bin/env node
/**
 * Hook: session-end
 * Event: Stop
 * Profile: standard, strict
 *
 * Persists a session summary to .claude/session-<date>.jsonl for cross-session continuity.
 * Captures: final tool call, open todos (if any), last file touched.
 */

const fs = require("fs");
const path = require("path");
const input = JSON.parse(process.stdin.read() ?? "{}");

const today = new Date().toISOString().split("T")[0];
const sessionFile = path.join(process.cwd(), ".claude", `session-${today}.jsonl`);

const summary = {
  type: "session-end",
  timestamp: new Date().toISOString(),
  stop_reason: input?.stop_reason ?? "unknown",
  last_tool: input?.last_tool_use?.tool_name ?? null,
};

try {
  fs.mkdirSync(path.dirname(sessionFile), { recursive: true });
  fs.appendFileSync(sessionFile, JSON.stringify(summary) + "\n");
} catch (_) { /* non-blocking */ }

process.exit(0);