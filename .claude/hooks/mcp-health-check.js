#!/usr/bin/env node
/**
 * Hook: mcp-health-check
 * Event: PreToolUse (mcp__* tools)
 * Profile: standard, strict
 *
 * Probes MCP server health before any MCP tool call.
 * If the server is unreachable, fails fast with a clear message rather than
 * letting the tool call hang or return a confusing error.
 *
 * Configure MCP_SERVERS in env as a comma-separated list of "name=healthUrl" pairs.
 * Example: MCP_SERVERS=github=http://localhost:3100/health,brave=http://localhost:3101/health
 *
 * If MCP_SERVERS is not set, the hook is a no-op.
 */

const { execSync } = require("child_process");
const input = JSON.parse(process.stdin.read() ?? "{}");
const toolName = input?.tool_name ?? "";

if (!toolName.startsWith("mcp__")) process.exit(0);

const serverName = toolName.split("__")[1] ?? "";
const serverConfig = process.env.MCP_SERVERS ?? "";

if (!serverConfig) process.exit(0); // no health URLs configured — skip

const entries = Object.fromEntries(
  serverConfig.split(",").map((s) => s.trim().split("=")).filter((p) => p.length === 2)
);

const healthUrl = entries[serverName];
if (!healthUrl) process.exit(0); // no health URL for this server — skip

try {
  execSync(`curl -sf --max-time 2 "${healthUrl}"`, { stdio: "pipe" });
} catch (_) {
  console.error(
    `[mcp-health-check] MCP server '${serverName}' is not responding at ${healthUrl}.\n` +
    `Verify the server is running before retrying. Check your MCP server configuration.`
  );
  process.exit(2);
}

process.exit(0);