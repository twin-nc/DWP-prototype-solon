# install-git-hooks.ps1
# Installs local git hooks used by this repository:
# - pre-commit: regenerate AGENTS.md when .claude agents/skills changed
# - pre-commit: emit Codex context suggestion + log pre-commit checkpoint
# - post-commit: log commit checkpoint for Codex session continuity
# Run once after cloning the repo.

param(
    [string]$RepoRoot = $PSScriptRoot + "\.."
)

$RepoRoot = (Resolve-Path $RepoRoot).Path
$HooksDir = Join-Path $RepoRoot ".git\hooks"
$PreCommitHookFile = Join-Path $HooksDir "pre-commit"
$PostCommitHookFile = Join-Path $HooksDir "post-commit"

if (-not (Test-Path $HooksDir)) {
    Write-Error "Not a git repository or .git/hooks not found at: $HooksDir"
    exit 1
}

$preCommitHookContent = @'
#!/bin/sh
# Auto-regenerate AGENTS.md when agent or skill files change.
if git diff --cached --name-only | grep -qE '^\.claude/(agents|skills)/'; then
    echo "Regenerating AGENTS.md..."
    powershell -NonInteractive -File scripts/sync-claude-to-codex.ps1
    git add AGENTS.md
fi

# Codex-native context hinting + checkpoint logging (non-blocking).
powershell -NonInteractive -File scripts/codex-context-pre-commit.ps1 || true
'@

$postCommitHookContent = @'
#!/bin/sh
# Codex-native commit checkpoint logging (non-blocking).
powershell -NonInteractive -File scripts/codex-context-post-commit.ps1 || true
'@

$preCommitHookContent | Out-File -FilePath $PreCommitHookFile -Encoding utf8 -NoNewline
$postCommitHookContent | Out-File -FilePath $PostCommitHookFile -Encoding utf8 -NoNewline

# Make executable on Unix-like systems (no-op on Windows but harmless)
if ($IsLinux -or $IsMacOS) {
    chmod +x $PreCommitHookFile
    chmod +x $PostCommitHookFile
}

Write-Host "Pre-commit hook installed at: $PreCommitHookFile"
Write-Host "Post-commit hook installed at: $PostCommitHookFile"
Write-Host "AGENTS.md sync + Codex context checkpoint hooks are now active."
