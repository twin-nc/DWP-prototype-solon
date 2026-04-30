#!/usr/bin/env node
/**
 * Hook: pre-compact
 * Event: PreCompact
 * Profile: standard, strict
 *
 * Logs a compaction boundary marker to the session file before context is compacted.
 * Helps reconstruct conversation flow and identify where context was lost.
 *
 * Output: appends a marker to .claude/session-<date>.jsonl
 */

const fs = require("fs");
const path = require("path");

const today = new Date().toISOString().split("T")[0];
const sessionFile = path.join(process.cwd(), ".claude", `session-${today}.jsonl`);

const marker = {
  type: "compaction-boundary",
  timestamp: new Date().toISOString(),
  note: "Context compacted at this point. Prior tool results and reasoning may not be available.",
};

try {
  fs.mkdirSync(path.dirname(sessionFile), { recursive: true });
  fs.appendFileSync(sessionFile, JSON.stringify(marker) + "\n");
} catch (_) { /* non-blocking */ }

process.exit(0);