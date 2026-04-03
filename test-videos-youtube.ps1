#!/usr/bin/env powershell

# ============================================
# Script de Testing - Endpoints Videos & YouTube
# ============================================
# Uso: ./test-videos-youtube.ps1 -BaseUrl "https://tu-dominio.onrender.com" -Token "tu_jwt_token"

param(
    [string]$BaseUrl = "https://tu-dominio.onrender.com",
    [string]$Token = ""
)

$ErrorActionPreference = "Continue"

# Colores para output
$Green = [System.ConsoleColor]::Green
$Red = [System.ConsoleColor]::Red
$Yellow = [System.ConsoleColor]::Yellow
$Cyan = [System.ConsoleColor]::Cyan
$White = [System.ConsoleColor]::White

function Write-Header {
    param([string]$Message)
    Write-Host ""
    Write-Host "════════════════════════════════════════════════════" -ForegroundColor $Cyan
    Write-Host "🎬 $Message" -ForegroundColor $Cyan
    Write-Host "════════════════════════════════════════════════════" -ForegroundColor $Cyan
}

function Write-Success {
    param([string]$Message)
    Write-Host "✅ $Message" -ForegroundColor $Green
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "❌ $Message" -ForegroundColor $Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "ℹ️  $Message" -ForegroundColor $Yellow
}

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [object]$Body = $null,
        [boolean]$RequiresAuth = $false
    )

    Write-Host ""
    Write-Host "📍 $Name" -ForegroundColor $Yellow
    Write-Host "   $Method $Url" -ForegroundColor $White

    try {
        $headers = @{
            "Content-Type" = "application/json"
        }

        if ($RequiresAuth) {
            if (-not $Token) {
                Write-Error-Custom "Token no proporcionado"
                return $null
            }
            $headers["Authorization"] = "Bearer $Token"
        }

        $params = @{
            Uri = $Url
            Headers = $headers
            Method = $Method
            TimeoutSec = 10
        }

        if ($Body) {
            $params["Body"] = $Body | ConvertTo-Json -Depth 5
        }

        $response = Invoke-RestMethod @params
        Write-Success "✓ Response exitosa"

        # Mostrar respuesta formateada
        $json = $response | ConvertTo-Json -Depth 3
        Write-Host "📦 Response:" -ForegroundColor $Cyan
        Write-Host ($json | Select-String -NotMatch "^$") | ForEach-Object { "   $_" } -ForegroundColor Gray

        return $response

    } catch {
        Write-Error-Custom "Error: $($_.Exception.Message)"
        return $null
    }
}

# ============================================
# INICIO PRUEBAS
# ============================================

Clear-Host
Write-Host "🎬 Testing Videos & YouTube Endpoints" -ForegroundColor $Cyan
Write-Host "📌 Base URL: $BaseUrl" -ForegroundColor Gray
Write-Host ""

if (-not $Token) {
    Write-Error-Custom "⚠️  Token no proporcionado"
    Write-Info "Usa: ./test-videos-youtube.ps1 -Token 'tu_jwt_token'"
    exit 1
}

Write-Success "Token verificado ✓"
Write-Host ""

# ============================================
# PRUEBA 1: VERIFICAR USUARIO AUTENTICADO
# ============================================

Write-Header "PASO 1: Verificar Usuario Autenticado"

$user = Test-Endpoint `
    -Name "1.1 GET /api/auth/me - Obtener usuario actual" `
    -Method "GET" `
    -Url "$BaseUrl/api/auth/me" `
    -RequiresAuth $true

if (-not $user) {
    Write-Error-Custom "No se pudo obtener el usuario. Verifica el token."
    exit 1
}

$userId = $user.id
Write-Info "Usuario ID: $userId"
Write-Info "Usuario: $($user.name) ($($user.email))"

# ============================================
# PRUEBA 2: CREAR VIDEOS
# ============================================

Write-Header "PASO 2: Crear Videos"

$videoIds = @()

# Video 1
$videoData1 = @{
    title = "Video de Prueba 1 - $(Get-Date -Format 'HH:mm:ss')"
    description = "Este es mi primer video de prueba creado desde PowerShell"
    prompt = "Un gato durmiendo en una hamaca bajo el sol"
}

$video1 = Test-Endpoint `
    -Name "2.1 POST /api/videos/generate - Crear primer video" `
    -Method "POST" `
    -Url "$BaseUrl/api/videos/generate" `
    -Body $videoData1 `
    -RequiresAuth $true

if ($video1) {
    $videoIds += $video1.id
    Write-Info "Video 1 creado con ID: $($video1.id)"
    Write-Info "Estado: $($video1.status)"
}

Write-Host ""

# Video 2
$videoData2 = @{
    title = "Video de Prueba 2 - $(Get-Date -Format 'HH:mm:ss')"
    description = "Mi segundo video de prueba para YouTube"
    prompt = "Un perro corriendo en la playa al atardecer"
}

$video2 = Test-Endpoint `
    -Name "2.2 POST /api/videos/generate - Crear segundo video" `
    -Method "POST" `
    -Url "$BaseUrl/api/videos/generate" `
    -Body $videoData2 `
    -RequiresAuth $true

if ($video2) {
    $videoIds += $video2.id
    Write-Info "Video 2 creado con ID: $($video2.id)"
    Write-Info "Estado: $($video2.status)"
}

if ($videoIds.Count -eq 0) {
    Write-Error-Custom "No se pudieron crear videos. Verifica la API."
    exit 1
}

# ============================================
# PRUEBA 3: LISTAR VIDEOS
# ============================================

Write-Header "PASO 3: Listar Todos los Videos"

$videosList = Test-Endpoint `
    -Name "3.1 GET /api/videos - Listar todos los videos" `
    -Method "GET" `
    -Url "$BaseUrl/api/videos" `
    -RequiresAuth $true

if ($videosList) {
    Write-Info "Total de videos: $($videosList.Count)"
    $videosList | ForEach-Object {
        Write-Host "   • ID: $($_.id) | Título: $($_.title) | Estado: $($_.status)" -ForegroundColor Gray
    }
}

# ============================================
# PRUEBA 4: OBTENER VIDEO ESPECÍFICO
# ============================================

Write-Header "PASO 4: Obtener Detalles de Video Específico"

if ($videoIds.Count -gt 0) {
    $firstVideoId = $videoIds[0]

    $videoDetails = Test-Endpoint `
        -Name "4.1 GET /api/videos/$firstVideoId - Obtener detalles del video" `
        -Method "GET" `
        -Url "$BaseUrl/api/videos/$firstVideoId" `
        -RequiresAuth $true

    if ($videoDetails) {
        Write-Info "Detalles obtenidos exitosamente"
        Write-Host "   Título: $($videoDetails.title)" -ForegroundColor Gray
        Write-Host "   Descripción: $($videoDetails.description)" -ForegroundColor Gray
        Write-Host "   Estado: $($videoDetails.status)" -ForegroundColor Gray
    }
}

# ============================================
# PRUEBA 5: PUBLICAR EN YOUTUBE
# ============================================

Write-Header "PASO 5: Publicar Videos en YouTube"

$uploadIds = @()

if ($videoIds.Count -gt 0) {
    # Publicar Video 1
    $publishData1 = @{
        videoId = $videoIds[0]
        title = "Mi Video Generado - $(Get-Date -Format 'HH:mm:ss')"
        description = "Este video fue generado con inteligencia artificial y probado desde PowerShell"
        visibility = "UNLISTED"
    }

    $upload1 = Test-Endpoint `
        -Name "5.1 POST /api/youtube/publish - Publicar primer video en YouTube" `
        -Method "POST" `
        -Url "$BaseUrl/api/youtube/publish" `
        -Body $publishData1 `
        -RequiresAuth $true

    if ($upload1) {
        $uploadIds += $upload1.id
        Write-Info "Video publicado en YouTube"
        Write-Info "Upload ID: $($upload1.id)"
        Write-Info "YouTube Video ID: $($upload1.youtubeVideoId)"
        Write-Info "URL: $($upload1.youtubeUrl)"
        Write-Info "Estado: $($upload1.status)"
    }
}

if ($videoIds.Count -gt 1) {
    Write-Host ""

    # Publicar Video 2
    $publishData2 = @{
        videoId = $videoIds[1]
        title = "Segundo Video Generado - $(Get-Date -Format 'HH:mm:ss')"
        description = "Segundo video de prueba en YouTube"
        visibility = "PRIVATE"
    }

    $upload2 = Test-Endpoint `
        -Name "5.2 POST /api/youtube/publish - Publicar segundo video en YouTube" `
        -Method "POST" `
        -Url "$BaseUrl/api/youtube/publish" `
        -Body $publishData2 `
        -RequiresAuth $true

    if ($upload2) {
        $uploadIds += $upload2.id
        Write-Info "Video publicado en YouTube"
        Write-Info "Upload ID: $($upload2.id)"
    }
}

# ============================================
# PRUEBA 6: LISTAR PUBLICACIONES YOUTUBE
# ============================================

Write-Header "PASO 6: Listar Publicaciones en YouTube"

$youtubeUploads = Test-Endpoint `
    -Name "6.1 GET /api/youtube/uploads - Listar todas las publicaciones" `
    -Method "GET" `
    -Url "$BaseUrl/api/youtube/uploads" `
    -RequiresAuth $true

if ($youtubeUploads) {
    Write-Info "Total de publicaciones YouTube: $($youtubeUploads.Count)"
    $youtubeUploads | ForEach-Object {
        Write-Host "   • ID: $($_.id) | Video ID: $($_.videoId) | YouTube ID: $($_.youtubeVideoId) | Estado: $($_.status)" -ForegroundColor Gray
    }
}

# ============================================
# PRUEBA 7: OBTENER ESTADO DE PUBLICACIÓN
# ============================================

Write-Header "PASO 7: Obtener Estado de Publicación"

if ($uploadIds.Count -gt 0) {
    $firstUploadId = $uploadIds[0]

    $uploadStatus = Test-Endpoint `
        -Name "7.1 GET /api/youtube/uploads/$firstUploadId - Obtener estado de publicación" `
        -Method "GET" `
        -Url "$BaseUrl/api/youtube/uploads/$firstUploadId" `
        -RequiresAuth $true

    if ($uploadStatus) {
        Write-Info "Estado obtenido exitosamente"
        Write-Host "   Video ID: $($uploadStatus.videoId)" -ForegroundColor Gray
        Write-Host "   YouTube Video ID: $($uploadStatus.youtubeVideoId)" -ForegroundColor Gray
        Write-Host "   URL: $($uploadStatus.youtubeUrl)" -ForegroundColor Gray
        Write-Host "   Estado: $($uploadStatus.status)" -ForegroundColor Gray
    }
}

# ============================================
# RESUMEN FINAL
# ============================================

Write-Header "RESUMEN DE PRUEBAS"

Write-Success "Videos creados: $($videoIds.Count)"
Write-Success "Videos publicados en YouTube: $($uploadIds.Count)"

if ($videoIds.Count -gt 0 -and $uploadIds.Count -gt 0) {
    Write-Host ""
    Write-Success "¡TODAS LAS PRUEBAS PASARON! ✓"
    Write-Host ""
    Write-Host "Resumen:" -ForegroundColor $Cyan
    Write-Host "  ✓ Se crearon $($videoIds.Count) videos correctamente" -ForegroundColor $Green
    Write-Host "  ✓ Se publicaron $($uploadIds.Count) videos en YouTube correctamente" -ForegroundColor $Green
    Write-Host "  ✓ Los datos se guardaron en la base de datos" -ForegroundColor $Green
} else {
    Write-Host ""
    Write-Error-Custom "Algunas pruebas fallaron"
    if ($videoIds.Count -eq 0) {
        Write-Error-Custom "  ✗ No se pudieron crear videos"
    }
    if ($uploadIds.Count -eq 0) {
        Write-Error-Custom "  ✗ No se pudieron publicar videos en YouTube"
    }
}

Write-Host ""
Write-Host "📊 Próximos pasos:" -ForegroundColor $Cyan
Write-Host "  1. Verifica la base de datos:" -ForegroundColor Gray
Write-Host "     SELECT * FROM generated_videos;" -ForegroundColor Gray
Write-Host "     SELECT * FROM youtube_uploads;" -ForegroundColor Gray
Write-Host "  2. Revisa los logs en Render Dashboard" -ForegroundColor Gray
Write-Host "  3. Prueba eliminar videos si quieres" -ForegroundColor Gray
Write-Host ""

