# codex-context-pre-commit.ps1
# Non-blocking helper hook for Codex sessions.
# Writes a pre-commit checkpoint to .codex/session-YYYY-MM-DD.jsonl and
# suggests a context reset/compact when staged change size is large.

param(
    [string]$RepoRoot = ""
)

function Get-IntEnvOrDefault {
    param(
        [string]$Name,
        [int]$DefaultValue
    )

    $raw = [Environment]::GetEnvironmentVariable($Name)
    if ([string]::IsNullOrWhiteSpace($raw)) {
        return $DefaultValue
    }

    $parsed = 0
    if ([int]::TryParse($raw, [ref]$parsed)) {
        return $parsed
    }

    return $DefaultValue
}

try {
    if ([string]::IsNullOrWhiteSpace($RepoRoot)) {
        $RepoRoot = Join-Path $PSScriptRoot ".."
    }

    $RepoRoot = (Resolve-Path $RepoRoot).Path
    Set-Location $RepoRoot

    $profile = [Environment]::GetEnvironmentVariable("ECC_HOOK_PROFILE")
    if ([string]::IsNullOrWhiteSpace($profile)) {
        $profile = "standard"
    }

    if ($profile -eq "minimal") {
        exit 0
    }

    $stagedNumStat = @(git diff --cached --numstat -- 2>$null)

    $fileCount = 0
    $addedLines = 0
    $deletedLines = 0

    foreach ($line in $stagedNumStat) {
        if ([string]::IsNullOrWhiteSpace($line)) {
            continue
        }

        $parts = $line -split "`t"
        if ($parts.Count -lt 3) {
            continue
        }

        $fileCount += 1

        if ($parts[0] -match '^\d+$') {
            $addedLines += [int]$parts[0]
        }

        if ($parts[1] -match '^\d+$') {
            $deletedLines += [int]$parts[1]
        }
    }

    $totalLineDelta = $addedLines + $deletedLines
    $fileThreshold = Get-IntEnvOrDefault -Name "CODEX_COMPACT_FILE_THRESHOLD" -DefaultValue 25
    $lineThreshold = Get-IntEnvOrDefault -Name "CODEX_COMPACT_LINE_THRESHOLD" -DefaultValue 800
    $recommendCompact = ($fileCount -ge $fileThreshold) -or ($totalLineDelta -ge $lineThreshold)

    if ($recommendCompact) {
        Write-Host (("[codex-suggest-compact] Staged {0} file(s), {1} line delta. " +
            "Consider /compact (or a fresh session) before the next major step.") -f $fileCount, $totalLineDelta)
    }

    $branch = (git rev-parse --abbrev-ref HEAD 2>$null | Select-Object -First 1)
    if ([string]::IsNullOrWhiteSpace($branch)) {
        $branch = "unknown"
    }

    $sessionDir = Join-Path $RepoRoot ".codex"
    $sessionFile = Join-Path $sessionDir ("session-{0}.jsonl" -f (Get-Date -Format "yyyy-MM-dd"))

    $checkpoint = [ordered]@{
        type                    = "pre-commit"
        timestamp               = (Get-Date).ToString("o")
        profile                 = $profile
        branch                  = $branch
        staged_files            = $fileCount
        added_lines             = $addedLines
        deleted_lines           = $deletedLines
        total_line_delta        = $totalLineDelta
        compact_recommendation  = $(if ($recommendCompact) { "CONSIDER" } else { "NONE" })
        file_threshold          = $fileThreshold
        line_threshold          = $lineThreshold
    }

    New-Item -ItemType Directory -Path $sessionDir -Force | Out-Null
    ($checkpoint | ConvertTo-Json -Compress) + "`n" | Add-Content -Path $sessionFile -Encoding utf8
} catch {
    # Context logging must never block a commit.
}

exit 0
