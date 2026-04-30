#!/usr/bin/env node
/**
 * Hook: config-protection
 * Event: PreToolUse (Edit, Write)
 * Profile: standard, strict
 *
 * Blocks edits to linter, formatter, and build config files without explicit human approval.
 * Rationale: silent config changes break CI for the whole team without obvious cause.
 *
 * Protected files (adapt for your stack):
 *   - .eslintrc*, .eslintignore, eslint.config.*
 *   - .prettierrc*, prettier.config.*
 *   - pom.xml (Maven)
 *   - checkstyle*.xml
 *   - spotbugs*.xml
 *   - .editorconfig
 *   - sonar-project.properties
 *   - tsconfig*.json
 *   - jest.config.*
 *   - .stylelintrc*
 */

const input = JSON.parse(process.stdin.read() ?? "{}");
const filePath = input?.tool_input?.file_path ?? "";

const protectedPatterns = [
  /\.eslintrc/,
  /\.eslintignore/,
  /eslint\.config\./,
  /\.prettierrc/,
  /prettier\.config\./,
  /\bpom\.xml$/,
  /checkstyle.*\.xml$/,
  /spotbugs.*\.xml$/,
  /\.editorconfig$/,
  /sonar-project\.properties$/,
  /tsconfig.*\.json$/,
  /jest\.config\./,
  /\.stylelintrc/,
];

for (const pattern of protectedPatterns) {
  if (pattern.test(filePath)) {
    const msg = `[config-protection] BLOCKED: edit to protected config file '${filePath}'.\n` +
      `Config changes affect the entire team's CI. Get explicit human approval before modifying.\n` +
      `To proceed: have a human review the change and temporarily remove this block for the session.`;
    console.error(msg);
    process.exit(2);
  }
}

process.exit(0);