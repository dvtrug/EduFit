[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8080",
    [ValidateSet("offline", "live", "either")]
    [string]$ExpectedAiMode = "offline",
    [switch]$ExerciseQuota,
    [switch]$VerifyDatabase
)

$ErrorActionPreference = "Stop"
$BaseUrl = $BaseUrl.TrimEnd("/")
$DemoPassword = "Password123@"
$StudentId = "11111111-1111-1111-1111-000000000001"
$TutorId = "22222222-2222-2222-2222-000000000001"

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw $Message }
}

function Invoke-Json {
    param(
        [ValidateSet("GET", "POST", "PUT", "DELETE")][string]$Method,
        [string]$Path,
        [Microsoft.PowerShell.Commands.WebRequestSession]$Session,
        [string]$CsrfToken,
        [object]$Body
    )
    $arguments = @{
        Uri = "$BaseUrl$Path"
        Method = $Method
        WebSession = $Session
        Headers = @{ Accept = "application/json" }
    }
    if ($CsrfToken) { $arguments.Headers["X-XSRF-TOKEN"] = $CsrfToken }
    if ($null -ne $Body) {
        $arguments.ContentType = "application/json"
        $arguments.Body = $Body | ConvertTo-Json -Depth 10 -Compress
    }
    Invoke-RestMethod @arguments
}

function Get-Csrf {
    param([Microsoft.PowerShell.Commands.WebRequestSession]$Session)
    $response = Invoke-Json -Method GET -Path "/api/v1/auth/csrf" -Session $Session
    Assert-True ($response.data.headerName -eq "X-XSRF-TOKEN") "Unexpected CSRF header contract"
    $response.data.token
}

function Login-Demo {
    param([string]$Email)
    $session = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
    $csrf = Get-Csrf $session
    $null = Invoke-Json -Method POST -Path "/api/v1/auth/login" -Session $session `
        -CsrfToken $csrf -Body @{ email = $Email; password = $DemoPassword }
    [pscustomobject]@{ Session = $session; Csrf = (Get-Csrf $session) }
}

function Assert-HttpStatus {
    param([int]$ExpectedStatus, [scriptblock]$Action, [string]$Label)
    try {
        & $Action
        throw "$Label unexpectedly succeeded"
    }
    catch {
        $response = $_.Exception.Response
        $actual = if ($response -and $response.StatusCode) { [int]$response.StatusCode } else { 0 }
        if ($actual -ne $ExpectedStatus) { throw "$Label expected HTTP $ExpectedStatus, got $actual" }
    }
}

function New-Goal {
    param($Actor, [int]$SubjectId)
    $deadline = (Get-Date).Date.AddMonths(2).ToString("yyyy-MM-dd")
    $created = Invoke-Json -Method POST -Path "/api/v1/profiles/students/me/goals" `
        -Session $Actor.Session -CsrfToken $Actor.Csrf -Body @{
            subjectId = $SubjectId
            currentLevel = 1
            desiredLevel = 2
            goalType = "IMPROVE_GRADES"
            deadline = $deadline
            mode = "ONLINE"
            area = $null
            budgetMin = 100000
            budgetMax = 300000
        }
    $goalId = $created.data.goalId
    $null = Invoke-Json -Method PUT -Path "/api/v1/profiles/students/me/goals/$goalId/availability" `
        -Session $Actor.Session -CsrfToken $Actor.Csrf -Body @{
            slots = @(@{ dayOfWeek = 1; startTime = "18:00"; endTime = "20:00" })
        }
    $goalId
}

function Wait-BackendHealth {
    param([int]$TimeoutSeconds = 90)
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            $health = Invoke-RestMethod -Method GET -Uri "$BaseUrl/actuator/health"
            if ($health.status -eq "UP") { return $health }
        }
        catch {
            # A cold-started container can accept TCP before Spring has completed startup.
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    throw "Backend health did not become UP within $TimeoutSeconds seconds"
}

$provider = if ($ExpectedAiMode -eq "live") { "gemini" } elseif ($ExpectedAiMode -eq "offline") { "stub" } else { "unknown" }
$model = if ($ExpectedAiMode -eq "offline") { "stub-rule-based" } else { "not-applicable" }
if ($ExpectedAiMode -eq "live") {
    $apiKey = [Environment]::GetEnvironmentVariable("GEMINI_API_KEY")
    Assert-True (-not [string]::IsNullOrWhiteSpace($apiKey)) `
        "GEMINI_API_KEY must be set in the process environment for live smoke"
    $model = [Environment]::GetEnvironmentVariable("GEMINI_MODEL")
    if ([string]::IsNullOrWhiteSpace($model)) { $model = "gemini-3.5-flash-lite" }
    $modelInfo = Invoke-RestMethod -Method GET `
        -Uri "https://generativelanguage.googleapis.com/v1beta/models/$([Uri]::EscapeDataString($model))" `
        -Headers @{ "x-goog-api-key" = $apiKey; Accept = "application/json" }
    Assert-True ($modelInfo.supportedGenerationMethods -contains "generateContent") `
        "Configured Gemini model does not advertise generateContent"
    Write-Host "Gemini model check passed: $model"
}

$health = Wait-BackendHealth
$openApi = Invoke-RestMethod -Method GET -Uri "$BaseUrl/v3/api-docs"
Assert-True ($openApi.components.securitySchemes.SessionCookieAuth.name -eq "EDUFIT_SESSION") `
    "OpenAPI does not document the runtime session cookie"
Assert-True ($openApi.security.Count -gt 0) "OpenAPI has no global session security requirement"
Assert-True ($openApi.paths.'/api/v1/discovery/tutors'.get.security.Count -eq 0) `
    "Public tutor search is incorrectly documented as protected"
$matchSecurity = $openApi.paths.'/api/v1/discovery/match'.post.security
Assert-True (($null -eq $matchSecurity) -or ($matchSecurity.Count -gt 0)) `
    "Protected matching operation has no OpenAPI security requirement"
$catalogs = Invoke-RestMethod -Method GET -Uri "$BaseUrl/api/v1/catalogs"
Assert-True ($catalogs.data.subjects.Count -gt 0) "Subject catalog is empty"
$search = Invoke-RestMethod -Method GET -Uri "$BaseUrl/api/v1/discovery/tutors?subjectId=1"
Assert-True ($search.data.content.tutorId -contains $TutorId) "Demo tutor missing from public search"
$detail = Invoke-RestMethod -Method GET -Uri "$BaseUrl/api/v1/discovery/tutors/$TutorId"
Assert-True ($detail.data.tutorId -eq $TutorId) "Demo tutor detail mismatch"

$student = Login-Demo "student.demo@edufit.vn"
$null = Invoke-Json -Method PUT -Path "/api/v1/profiles/students/me" -Session $student.Session `
    -CsrfToken $student.Csrf -Body @{ educationLevelId = 4; area = "Cau Giay, Ha Noi" }
$goalId = New-Goal $student 1

$anonymous = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
$anonymousCsrf = Get-Csrf $anonymous
Assert-HttpStatus 401 {
    Invoke-Json -Method POST -Path "/api/v1/discovery/match" -Session $anonymous `
        -CsrfToken $anonymousCsrf -Body @{ goalId = $goalId; topN = 5 }
} "anonymous match"
Assert-HttpStatus 403 {
    Invoke-Json -Method POST -Path "/api/v1/discovery/match" -Session $student.Session `
        -CsrfToken "invalid-smoke-token" -Body @{ goalId = $goalId; topN = 5 }
} "invalid CSRF token"

Assert-HttpStatus 400 {
    Invoke-Json -Method POST -Path "/api/v1/discovery/match" -Session $student.Session `
        -CsrfToken $student.Csrf -Body @{ goalId = $goalId; topN = 11 }
} "invalid topN"

$studentMatch = Invoke-Json -Method POST -Path "/api/v1/discovery/match" -Session $student.Session `
    -CsrfToken $student.Csrf -Body @{ goalId = $goalId; topN = 5 }
Assert-True ($studentMatch.data.Count -gt 0) "Matching returned no tutors for the demo goal"
Assert-True ($studentMatch.data[0].tutorId -eq $TutorId) "Unexpected top tutor"
Assert-True (-not [string]::IsNullOrWhiteSpace($studentMatch.data[0].explanation)) `
    "Top match has no grounded explanation"
if ($ExpectedAiMode -eq "live") {
    Assert-True $studentMatch.data[0].aiGenerated "Live smoke fell back instead of using Gemini"
}
elseif ($ExpectedAiMode -eq "offline") {
    Assert-True (-not $studentMatch.data[0].aiGenerated) "Offline smoke unexpectedly reports AI output"
}

$noResultGoal = New-Goal $student 6
$noResult = Invoke-Json -Method POST -Path "/api/v1/discovery/match" -Session $student.Session `
    -CsrfToken $student.Csrf -Body @{ goalId = $noResultGoal; topN = 5 }
Assert-True ($noResult.data.Count -eq 0) "No-result scenario unexpectedly returned tutors"

$parent = Login-Demo "parent.demo@edufit.vn"
$null = Invoke-Json -Method POST -Path "/api/v1/connections/parent-student/invite" `
    -Session $parent.Session -CsrfToken $parent.Csrf `
    -Body @{ targetEmail = "student.demo@edufit.vn" }
$invitations = Invoke-Json -Method GET -Path "/api/v1/connections/parent-student/invitations" `
    -Session $student.Session
$invitation = $invitations.data | Where-Object status -eq "PENDING" | Select-Object -First 1
Assert-True ($null -ne $invitation) "Student did not receive the Parent invitation"
$accepted = Invoke-Json -Method POST -Path "/api/v1/connections/parent-student/respond" `
    -Session $student.Session -CsrfToken $student.Csrf `
    -Body @{ invitationId = $invitation.invitationId; accept = $true }
$linkId = $accepted.data.linkId
$parentGoals = Invoke-Json -Method GET -Path "/api/v1/discovery/students/$StudentId/goals" `
    -Session $parent.Session
Assert-True ($parentGoals.data.goalId -contains $goalId) "Parent cannot read the linked Student goal"
$parentMatch = Invoke-Json -Method POST -Path "/api/v1/discovery/match" -Session $parent.Session `
    -CsrfToken $parent.Csrf -Body @{ goalId = $goalId; topN = 5 }
Assert-True ($parentMatch.data.Count -gt 0) "Parent matching returned no tutors"
$null = Invoke-Json -Method DELETE -Path "/api/v1/connections/parent-student/links/$linkId" `
    -Session $student.Session -CsrfToken $student.Csrf -Body @{ reason = "Smoke test revoke" }
Assert-HttpStatus 403 {
    Invoke-Json -Method GET -Path "/api/v1/discovery/students/$StudentId/goals" `
        -Session $parent.Session
} "revoked Parent goal read"

$request = Invoke-Json -Method POST -Path "/api/v1/connections/requests" `
    -Session $student.Session -CsrfToken $student.Csrf -Body @{
        goalId = $goalId
        tutorId = $TutorId
        desiredSlots = @("MONDAY 18:00-20:00")
        message = "AI Discovery smoke"
    }
$tutor = Login-Demo "tutor.demo@edufit.vn"
$inbox = Invoke-Json -Method GET -Path "/api/v1/connections/requests" -Session $tutor.Session
Assert-True ($inbox.data.requestId -contains $request.data.requestId) "Tutor inbox missing request"
$class = Invoke-Json -Method POST `
    -Path "/api/v1/connections/requests/$($request.data.requestId)/respond" `
    -Session $tutor.Session -CsrfToken $tutor.Csrf -Body @{ accept = $true; reason = $null }
Assert-True (-not [string]::IsNullOrWhiteSpace($class.data.classId)) "Tutor accept did not create a class"

if ($ExerciseQuota) {
    for ($i = 0; $i -lt 10; $i++) {
        $last = Invoke-Json -Method POST -Path "/api/v1/discovery/match" `
            -Session $student.Session -CsrfToken $student.Csrf -Body @{ goalId = $goalId; topN = 5 }
    }
    Assert-True (-not $last.data[0].aiGenerated) "Quota scenario did not fall back"
}

$dbStatus = "not-checked"
if ($VerifyDatabase) {
    $query = 'select status from ai_request_log where user_id=''11111111-1111-1111-1111-111111111111'' order by created_at desc limit 1'
    $dbStatus = (& docker compose exec -T db sh -c "psql -U `"`$POSTGRES_USER`" -d `"`$POSTGRES_DB`" -tAc `"$query`"").Trim()
    Assert-True ($LASTEXITCODE -eq 0) "Could not query AI audit status from Compose PostgreSQL"
    $expectedDbStatus = if ($ExpectedAiMode -in @("live", "offline")) { "SUCCESS" } else { $null }
    if ($expectedDbStatus) { Assert-True ($dbStatus -eq $expectedDbStatus) "Latest AI audit status is $dbStatus" }
}

$timestamp = (Get-Date).ToUniversalTime().ToString("o")
Write-Host "Smoke passed at $timestamp"
Write-Host "Backend=$BaseUrl mode=$ExpectedAiMode provider=$provider model=$model auditStatus=$dbStatus goalId=$goalId tutorId=$TutorId aiGenerated=$($studentMatch.data[0].aiGenerated) classId=$($class.data.classId)"
