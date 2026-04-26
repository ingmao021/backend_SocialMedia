param(
    [string]$Token = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhdXJiYW5vNTM1QGdtYWlsLmNvbSIsInVzZXJJZCI6MiwiaWF0IjoxNzc1NjI0MjgwLCJleHAiOjE3NzU3MTA2ODB9.OaIoY28xtweBFiCxd5BnQCYPJHc3v5fGUIO2STVPLlnthEo9QvqOTvU1FkZtTvHEw3HwTtPRWzTodVRZZYs7hQ",
    [string]$BaseUrl = "https://backend-socialmedia-ixsm.onrender.com",
    [switch]$Confirm = $true,
    [switch]$Verbose = $false
)

$headers = @{
    "Authorization" = "Bearer $Token"
    "Content-Type" = "application/json"
}

function Write-Header {
    param([string]$Text)
    Write-Host "`n" + ("=" * 75) -ForegroundColor Red
    Write-Host "  $Text" -ForegroundColor Red
    Write-Host ("=" * 75) -ForegroundColor Red
}

function Write-Info {
    param([string]$Text, [bool]$Indent = $true)
    $prefix = if ($Indent) { "    -> " } else { "  -> " }
    Write-Host "$prefix$Text" -ForegroundColor Gray
}

function Write-Success {
    param([string]$Text)
    Write-Host "    [OK] $Text" -ForegroundColor Green
}

function Write-Warning {
    param([string]$Text)
    Write-Host "    [WARNING] $Text" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Text)
    Write-Host "    [ERROR] $Text" -ForegroundColor Red
}

try {
    Write-Header "ELIMINAR TODOS LOS VIDEOS"
    
    # 1. Obtener lista de todos los videos
    Write-Host "`n[1] Obteniendo lista de videos..." -ForegroundColor Cyan
    
    $listResponse = Invoke-RestMethod -Uri "$BaseUrl/api/videos" -Headers $headers -Method GET
    
    if ($null -eq $listResponse -or $listResponse.Count -eq 0) {
        Write-Success "No hay videos para eliminar"
        exit 0
    }
    
    Write-Info "Se encontraron $($listResponse.Count) videos:"
    $listResponse | ForEach-Object {
        $statusColor = if ($_.status -eq "COMPLETED") { "Green" } elseif ($_.status -eq "PROCESSING") { "Yellow" } else { "Red" }
        Write-Host "           - ID: $($_.id) | Status: $($_.status) | $($_.title)" -ForegroundColor $statusColor
    }
    
    # 2. Confirmar eliminación
    if ($Confirm) {
        Write-Host "`n[2] ¿Estás seguro de que quieres eliminar todos los videos? (y/N)" -ForegroundColor Yellow -NoNewline
        $confirmation = Read-Host
        
        if ($confirmation -ne "y" -and $confirmation -ne "Y") {
            Write-Warning "Operación cancelada por el usuario"
            exit 0
        }
    }
    
    # 3. Eliminar videos uno por uno
    Write-Host "`n[3] Eliminando videos..." -ForegroundColor Cyan
    
    $deletedCount = 0
    $errorCount = 0
    
    foreach ($video in $listResponse) {
        try {
            Write-Host "`n    Eliminando video ID: $($video.id) - $($video.title)..." -ForegroundColor Yellow
            
            $deleteResponse = Invoke-RestMethod -Uri "$BaseUrl/api/videos/$($video.id)" -Headers $headers -Method DELETE
            
            Write-Success "Video ID: $($video.id) eliminado"
            $deletedCount++
            
            if ($Verbose) {
                Write-Info "Título: $($video.title)"
                Write-Info "Estado anterior: $($video.status)"
            }
            
        } catch {
            Write-Error "Error al eliminar video ID: $($video.id)"
            Write-Info "Error: $($_.Exception.Message)"
            $errorCount++
        }
    }
    
    # 4. Resumen
    Write-Host "`n" + ("=" * 75) -ForegroundColor Cyan
    Write-Host "  RESUMEN DE ELIMINACIÓN" -ForegroundColor Cyan
    Write-Host ("=" * 75) -ForegroundColor Cyan
    
    Write-Info "Videos eliminados exitosamente: $deletedCount" $false
    Write-Info "Videos con error: $errorCount" $false
    Write-Info "Total procesados: $($listResponse.Count)" $false
    
    if ($errorCount -eq 0) {
        Write-Success "Todos los videos fueron eliminados exitosamente"
    } else {
        Write-Warning "Algunos videos no pudieron ser eliminados"
    }
    
    # 5. Verificación final
    Write-Host "`n[4] Verificando que no queden videos..." -ForegroundColor Cyan
    
    try {
        $finalCheck = Invoke-RestMethod -Uri "$BaseUrl/api/videos" -Headers $headers -Method GET
        
        if ($null -eq $finalCheck -or $finalCheck.Count -eq 0) {
            Write-Success "Verificación completada: No quedan videos"
        } else {
            Write-Warning "Aún quedan $($finalCheck.Count) videos"
            $finalCheck | ForEach-Object {
                Write-Host "           - ID: $($_.id) | Status: $($_.status)" -ForegroundColor Red
            }
        }
    } catch {
        Write-Error "Error en verificación final: $($_.Exception.Message)"
    }
    
} catch {
    Write-Host "`n" + ("!" * 75) -ForegroundColor Red
    Write-Host "  ERROR CRÍTICO" -ForegroundColor Red
    Write-Host ("!" * 75) -ForegroundColor Red
    Write-Error "Error general: $($_.Exception.Message)"
    Write-Info "Revisa tu conexión y que el backend esté funcionando"
    exit 1
}

Write-Host "`nProceso completado." -ForegroundColor Gray
