param(
    [string]$Token = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhdXJiYW5vNTM1QGdtYWlsLmNvbSIsInVzZXJJZCI6MiwiaWF0IjoxNzc1NjI0MjgwLCJleHAiOjE3NzU3MTA2ODB9.OaIoY28xtweBFiCxd5BnQCYPJHc3v5fGUIO2STVPLlnthEo9QvqOTvU1FkZtTvHEw3HwTtPRWzTodVRZZYs7hQ",
    [string]$BaseUrl = "https://backend-socialmedia-ixsm.onrender.com",
    [switch]$Verbose = $false
)

# ============================================================================
# BACKEND SOCIALMEDIA - VERTEX AI VIDEO GENERATION TEST
# ============================================================================
# Este script ejecuta tests específicos para validar:
# - Generación de videos con Google Vertex AI (Veo)
# - Consulta de estado de videos
# - Listado de videos generados
# ============================================================================

$ErrorActionPreference = "Continue"
$testResults = @()
$startTime = Get-Date

# ============================================================================
# FUNCIONES AUXILIARES
# ============================================================================

function New-TestResult {
    param(
        [string]$Name,
        [bool]$Passed,
        [string]$Message = "",
        [object]$Data = $null
    )
    return @{
        Name = $Name
        Passed = $Passed
        Message = $Message
        Data = $Data
        Timestamp = Get-Date
    }
}

function Write-Header {
    param([string]$Text)
    Write-Host "`n" + ("=" * 75) -ForegroundColor Cyan
    Write-Host "  $Text" -ForegroundColor Cyan
    Write-Host ("=" * 75) -ForegroundColor Cyan
}

function Write-Section {
    param([string]$Text)
    Write-Host "`n--- $Text" -ForegroundColor Blue
}

function Write-Test {
    param([int]$Number, [string]$Text)
    Write-Host "`n  [$Number] $Text" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Text)
    Write-Host "    [OK] $Text" -ForegroundColor Green
}

function Write-Failed {
    param([string]$Text)
    Write-Host "    [ERROR] $Text" -ForegroundColor Red
}

function Write-Warning {
    param([string]$Text)
    Write-Host "    [WARNING] $Text" -ForegroundColor Yellow
}

function Write-Info {
    param([string]$Text, [bool]$Indent = $true)
    $prefix = if ($Indent) { "    -> " } else { "  -> " }
    Write-Host "$prefix$Text" -ForegroundColor Gray
}

function Write-Verbose-Info {
    param([string]$Text)
    if ($Verbose) {
        Write-Host "    [VERBOSE] $Text" -ForegroundColor DarkGray
    }
}

function Get-ErrorMessage {
    param($ErrorObject)
    try {
        if ($ErrorObject.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($ErrorObject.Exception.Response.GetResponseStream())
            return $reader.ReadToEnd()
        }
        return $ErrorObject.Exception.Message
    } catch {
        return $ErrorObject.Exception.Message
    }
}

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Uri,
        [string]$Method = "GET",
        [hashtable]$Headers = @{},
        [string]$Body = $null,
        [int]$TimeoutSec = 30,
        [scriptblock]$Validation = $null
    )

    try {
        Write-Test $testResults.Count $Name

        $params = @{
            Uri = $Uri
            Method = $Method
            Headers = $Headers
            TimeoutSec = $TimeoutSec
        }

        if ($Body) {
            $params.Body = $Body
        }

        $response = Invoke-RestMethod @params

        if ($Validation) {
            $validationResult = & $Validation $response
            if ($validationResult -eq $true) {
                Write-Success "$Name completado"
                return @{ Success = $true; Data = $response }
            } else {
                Write-Failed "Validación falló: $validationResult"
                return @{ Success = $false; Error = $validationResult; Data = $response }
            }
        } else {
            Write-Success "$Name completado"
            return @{ Success = $true; Data = $response }
        }
    } catch {
        Write-Failed "$Name falló"
        $errorMsg = Get-ErrorMessage $_
        Write-Info "Error: $errorMsg"
        Write-Verbose-Info "Full error: $(ConvertTo-Json -Depth 10 $_)"
        return @{ Success = $false; Error = $errorMsg; Data = $_ }
    }
}

# ============================================================================
# VALIDACIÓN INICIAL
# ============================================================================

if (-not $Token) {
    Write-Host "`n========================================" -ForegroundColor Red
    Write-Host "  BACKEND SOCIALMEDIA - VERTEX AI VIDEO TEST" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "  Error: Token JWT requerido" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red

    Write-Host "`n  Uso:" -ForegroundColor Yellow
    Write-Host "  .\test-vertex-ai.ps1 -Token 'eyJhbGc...' [-BaseUrl 'https://...'] [-Verbose]`n"

    Write-Host "Pasos para obtener un token:" -ForegroundColor Cyan
    Write-Host "  1. Ir a: https://accounts.google.com/o/oauth2/v2/auth?..." -ForegroundColor Gray
    Write-Host "  2. Autorizar la aplicación" -ForegroundColor Gray
    Write-Host "  3. Copiar el token JWT de la respuesta" -ForegroundColor Gray
    Write-Host ""
    exit 1
}

Write-Header "BACKEND SOCIALMEDIA - VERTEX AI VIDEO GENERATION TEST"
Write-Info "URL Base: $BaseUrl" $false
Write-Info "Modo Verbose: $Verbose" $false
Write-Info "Timestamp: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" $false

# ============================================================================
# PREPARAR HEADERS
# ============================================================================

$headers = @{
    "Authorization" = "Bearer $Token"
    "Content-Type" = "application/json"
}

# ============================================================================
# SECCIÓN 1: VALIDACIÓN BÁSICA DE AUTENTICACIÓN
# ============================================================================

Write-Section "AUTENTICACIÓN BÁSICA"

# Test 1.1: Validar token (solo para asegurar que la API funciona)
$authTest = Test-Endpoint `
    -Name "Validar token JWT" `
    -Uri "$BaseUrl/api/auth/me" `
    -Headers $headers `
    -Validation {
        param($response)
        if ($response.id -and $response.email) {
            Write-Verbose-Info "User: $($response.name) ($($response.email))"
            return $true
        }
        return "Respuesta incompleta"
    }

$testResults += New-TestResult "Auth: Validate Token" $authTest.Success $authTest.Error $authTest.Data

if ($authTest.Success) {
    $userId = $authTest.Data.id
    $userName = $authTest.Data.name
    $userEmail = $authTest.Data.email

    Write-Info "ID: $userId"
    Write-Info "Nombre: $userName"
    Write-Info "Email: $userEmail"
} else {
    Write-Failed "No se pudo validar el usuario. Abortando tests."
    exit 1
}

# ============================================================================
# SECCIÓN 2: GENERACIÓN DE VIDEOS CON VERTEX AI
# ============================================================================

Write-Section "GENERACIÓN DE VIDEOS CON VERTEX AI"

$videoPayload = @{
    title = "Vertex AI Test - $(Get-Date -Format 'HHmmss')"
    description = "Test automático de generación de video con Vertex AI"
    prompt = "A crystal clear waterfall flowing through a lush green forest, water flowing smoothly, sunlight filtering through trees, nature documentary style"
} | ConvertTo-Json

# Test 2.1: Generar video con Vertex AI
Write-Test ($testResults.Count + 1) "Generar video con Google Vertex AI"
Write-Warning "Este paso toma 2-5 minutos. Esperando..."

$generateTest = Test-Endpoint `
    -Name "Generar video con Vertex AI" `
    -Uri "$BaseUrl/api/videos/generate" `
    -Method "POST" `
    -Headers $headers `
    -Body $videoPayload `
    -TimeoutSec 60 `
    -Validation {
        param($response)
        if ($response.id -and $response.status) {
            Write-Verbose-Info "Video ID: $($response.id)"
            Write-Verbose-Info "Status: $($response.status)"
            return $true
        }
        return "Respuesta incompleta"
    }

$testResults += New-TestResult "Vertex AI: Generate Video" $generateTest.Success $generateTest.Error $generateTest.Data

if ($generateTest.Success) {
    $videoId = $generateTest.Data.id
    $videoStatus = $generateTest.Data.status

    Write-Info "ID: $videoId"
    Write-Info "Estado: $videoStatus"
    Write-Info "Título: $($generateTest.Data.title)"
    Write-Info "Creado: $($generateTest.Data.createdAt)"

    if ($Verbose) {
        Write-Verbose-Info "Google Job ID: $($generateTest.Data.googleJobId)"
        Write-Verbose-Info "Respuesta completa: $(ConvertTo-Json -Depth 10 $generateTest.Data)"
    }
} else {
    Write-Failed "Error al generar video con Vertex AI"
    $testResults += New-TestResult "Vertex AI: Fetch Status" $false "Generación falló"
    $testResults += New-TestResult "Vertex AI: List Videos" $false "Salto por error anterior"
}

# ============================================================================
# SECCIÓN 3: CONSULTA DE ESTADO Y LISTADO
# ============================================================================

if ($generateTest.Success) {
    Write-Section "CONSULTA DE ESTADO Y LISTADO"

    # Test 3.1: Obtener estado del video
    $fetchTest = Test-Endpoint `
        -Name "Obtener estado del video" `
        -Uri "$BaseUrl/api/videos/$videoId" `
        -Headers $headers `
        -Validation {
            param($response)
            if ($response.id -eq $videoId) {
                Write-Verbose-Info "Status: $($response.status)"
                return $true
            }
            return "ID no coincide"
        }

    $testResults += New-TestResult "Vertex AI: Fetch Status" $fetchTest.Success $fetchTest.Error $fetchTest.Data

    if ($fetchTest.Success) {
        $currentStatus = $fetchTest.Data.status
        Write-Info "Estado actual: $currentStatus"
        Write-Info "Actualizado: $($fetchTest.Data.updatedAt)"

        if ($fetchTest.Data.videoUrl) {
            Write-Success "URL del video disponible"
            Write-Info "URL: $($fetchTest.Data.videoUrl)"
        } else {
            Write-Warning "Video aún en procesamiento"
            if ($currentStatus -eq "PROCESSING") {
                Write-Info "Tiempo estimado: 2-3 minutos más"
            }
        }

        if ($fetchTest.Data.errorMessage) {
            Write-Failed "Error en procesamiento: $($fetchTest.Data.errorMessage)"
        }
    }

    # Test 3.2: Listar videos del usuario
    $listTest = Test-Endpoint `
        -Name "Listar videos generados" `
        -Uri "$BaseUrl/api/videos" `
        -Headers $headers `
        -Validation {
            param($response)
            if ($response -is [array]) {
                Write-Verbose-Info "Total de videos: $($response.Count)"
                return $true
            }
            Write-Verbose-Info "Sin videos"
            return $true
        }

    $testResults += New-TestResult "Vertex AI: List Videos" $listTest.Success $listTest.Error $listTest.Data

    if ($listTest.Success) {
        if ($listTest.Data -is [array]) {
            Write-Info "Total de videos: $($listTest.Data.Count)"
            $listTest.Data | ForEach-Object {
                $statusColor = if ($_.status -eq "COMPLETED") { "Green" } else { "Yellow" }
                Write-Host "           - ID: $($_.id) | Status: $($_.status) | $($_.title)" -ForegroundColor $statusColor
            }
        } else {
            Write-Info "Sin videos"
        }
    }
}

# ============================================================================
# RESUMEN Y REPORTE
# ============================================================================

Write-Header "RESUMEN DE TESTS VERTEX AI"

$passed = @($testResults | Where-Object { $_.Passed }).Count
$failed = @($testResults | Where-Object { -not $_.Passed }).Count
$total = $testResults.Count
$duration = (Get-Date) - $startTime

Write-Info "Pruebas Pasadas: $passed/$total" $false
Write-Info "Pruebas Fallidas: $failed/$total" $false
Write-Info "Duración: $($duration.Minutes)m $($duration.Seconds)s" $false

Write-Section "DETALLE DE RESULTADOS"

$testResults | ForEach-Object {
    $symbol = if ($_.Passed) { "[OK]" } else { "[ERROR]" }
    $color = if ($_.Passed) { "Green" } else { "Red" }
    Write-Host "  $symbol $($_.Name)" -ForegroundColor $color
    if ($_.Message) {
        Write-Info $_.Message
    }
}

# ============================================================================
# RECOMENDACIONES
# ============================================================================

Write-Header "RECOMENDACIONES VERTEX AI"

if ($failed -eq 0) {
    Write-Success "Todos los tests de Vertex AI pasaron exitosamente"
} else {
    Write-Warning "Algunos tests fallaron. Revisa los errores arriba."
}

if ($generateTest.Success -and $generateTest.Data.status -eq "PROCESSING") {
    Write-Info "El video está en procesamiento. Espera 2-3 minutos y ejecuta nuevamente" $false
    Write-Info "Comando: .\test-vertex-ai.ps1 -Token '$Token' -BaseUrl '$BaseUrl'" $false
}

if ($Verbose -eq $false) {
    Write-Info "Para más detalles, ejecuta con -Verbose" $false
}

Write-Info "Para probar diferentes prompts, modifica la variable \$videoPayload" $false

# ============================================================================
# EXPORTAR RESULTADOS
# ============================================================================

$reportFile = "vertex-ai-test-results-$(Get-Date -Format 'yyyyMMdd-HHmmss').json"
$testResults | ConvertTo-Json -Depth 10 | Out-File $reportFile -Encoding UTF8

Write-Host "`n  Reporte guardado: $reportFile" -ForegroundColor Gray
Write-Host ""
