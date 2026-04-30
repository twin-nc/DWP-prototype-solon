#!/usr/bin/env node
/**
 * Hook: suggest-compact
 * Event: PostToolUse
 * Profile: standard, strict
 *
 * Suggests /compact when tool call count crosses COMPACT_THRESHOLD (default: 40).
 * Reminder only — does not block.
 *
 * Threshold tuning:
 *   - Lower (e.g. 25) for research-heavy sessions where context fills fast.
 *   - Higher (e.g. 60) for long implementation sessions where continuity matters.
 *   - Set ECC_COMPACT_THRESHOLD in env to override.
 */

const fs = require("fs");
const path = require("path");

const THRESHOLD = parseInt(process.env.ECC_COMPACT_THRESHOLD ?? "40", 10);
const counterFile = path.join(process.cwd(), ".claude", ".tool-call-count");

let count = 0;
try {
  count = parseInt(fs.readFileSync(counterFile, "utf8").trim(), 10) || 0;
} catch (_) {}

count += 1;

try {
  fs.mkdirSync(path.dirname(counterFile), { recursive: true });
  fs.writeFileSync(counterFile, String(count));
} catch (_) {}

if (count === THRESHOLD) {
  console.error(
    `[suggest-compact] Tool call count reached ${THRESHOLD}.\n` +
    `Consider running /compact now to free context before continuing.\n` +
    `(This is a reminder, not a block. Reset counter by running: rm .claude/.tool-call-count)`
  );
}

process.exit(0);