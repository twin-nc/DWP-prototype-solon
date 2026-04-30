# codex-context-post-commit.ps1
# Non-blocking helper hook for Codex sessions.
# Writes a post-commit checkpoint to .codex/session-YYYY-MM-DD.jsonl.

param(
    [string]$RepoRoot = ""
)

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

    $commitHash = (git rev-parse HEAD 2>$null | Select-Object -First 1)
    $commitSubject = (git log -1 --pretty=%s 2>$null | Select-Object -First 1)
    $branch = (git rev-parse --abbrev-ref HEAD 2>$null | Select-Object -First 1)
    $filesChanged = @(git show --name-only --pretty=format: --no-renames HEAD 2>$null | Where-Object {
            -not [string]::IsNullOrWhiteSpace($_)
        })

    if ([string]::IsNullOrWhiteSpace($commitHash)) {
        $commitHash = "unknown"
    }

    if ([string]::IsNullOrWhiteSpace($commitSubject)) {
        $commitSubject = "unknown"
    }

    if ([string]::IsNullOrWhiteSpace($branch)) {
        $branch = "unknown"
    }

    $sessionDir = Join-Path $RepoRoot ".codex"
    $sessionFile = Join-Path $sessionDir ("session-{0}.jsonl" -f (Get-Date -Format "yyyy-MM-dd"))

    $checkpoint = [ordered]@{
        type              = "post-commit"
        timestamp         = (Get-Date).ToString("o")
        profile           = $profile
        branch            = $branch
        commit            = $commitHash
        commit_subject    = $commitSubject
        changed_file_count = $filesChanged.Count
    }

    New-Item -ItemType Directory -Path $sessionDir -Force | Out-Null
    ($checkpoint | ConvertTo-Json -Compress) + "`n" | Add-Content -Path $sessionFile -Encoding utf8
} catch {
    # Context logging must never block a commit.
}

exit 0
