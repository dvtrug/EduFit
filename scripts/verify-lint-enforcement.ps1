$ErrorActionPreference = "Stop"

$fixtureDirectory = Join-Path $PSScriptRoot "..\backend\src\test\java\com\edufit\backend\lintfixture"
$fixtureTarget = Join-Path $fixtureDirectory "BadConvention.java"
$fixtureSource = Join-Path $PSScriptRoot "..\backend\src\test\resources\lint-fixtures\BadConvention.java.fixture"

New-Item -ItemType Directory -Force -Path $fixtureDirectory | Out-Null
Copy-Item -LiteralPath $fixtureSource -Destination $fixtureTarget

try {
    Push-Location (Join-Path $PSScriptRoot "..\backend")
    try {
        $lintOutput = (& cmd.exe /d /c ".\mvnw.cmd -B -ntp -DskipTests spotless:check checkstyle:check 2>&1" | Out-String)
        Write-Host $lintOutput
        $lintExitCode = $LASTEXITCODE
    }
    finally {
        Pop-Location
    }
}
finally {
    Remove-Item -LiteralPath $fixtureTarget -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $fixtureDirectory -Force -ErrorAction SilentlyContinue
}

if ($lintExitCode -eq 0) {
    throw "Expected lint to fail for the intentionally invalid fixture, but it passed."
}

if ($lintOutput -notmatch "BadConvention.java") {
    throw "The command failed for an unrelated reason; the invalid fixture was not reported."
}

Write-Host "PASS: lint rejected the intentionally invalid Java fixture."
exit 0
