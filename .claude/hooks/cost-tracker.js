#!/usr/bin/env node
/**
 * Hook: cost-tracker
 * Event: Stop
 * Profile: standard, strict
 *
 * Appends token usage and estimated cost to .claude/costs.jsonl after each session.
 * Provides visibility into cumulative spend without requiring external tooling.
 *
 * Pricing (update if model pricing changes — see Anthropic pricing page):
 *   Haiku 4.5:  input $0.80/M, output $4.00/M
 *   Sonnet 4.6: input $3.00/M, output $15.00/M
 *   Opus 4.6:   input $15.00/M, output $75.00/M
 */

const fs = require("fs");
const path = require("path");
const input = JSON.parse(process.stdin.read() ?? "{}");

const usage = input?.usage ?? {};
const model = input?.model ?? process.env.CLAUDE_MODEL ?? "unknown";

const pricing = {
  "claude-haiku-4-5": { input: 0.80, output: 4.00 },
  "claude-sonnet-4-6": { input: 3.00, output: 15.00 },
  "claude-opus-4-6": { input: 15.00, output: 75.00 },
};

const modelKey = Object.keys(pricing).find((k) => model.includes(k)) ?? null;
const rates = modelKey ? pricing[modelKey] : null;

const inputTokens = usage.input_tokens ?? 0;
const outputTokens = usage.output_tokens ?? 0;
const estimatedCostUsd = rates
  ? (inputTokens / 1_000_000) * rates.input + (outputTokens / 1_000_000) * rates.output
  : null;

const record = {
  timestamp: new Date().toISOString(),
  model,
  input_tokens: inputTokens,
  output_tokens: outputTokens,
  estimated_cost_usd: estimatedCostUsd !== null ? Math.round(estimatedCostUsd * 10000) / 10000 : "unknown",
};

const costFile = path.join(process.cwd(), ".claude", "costs.jsonl");
try {
  fs.mkdirSync(path.dirname(costFile), { recursive: true });
  fs.appendFileSync(costFile, JSON.stringify(record) + "\n");
} catch (_) { /* non-blocking */ }

process.exit(0);