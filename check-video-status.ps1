param(
    [int]$VideoId = 1,
    [string]$Token = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhdXJiYW5vNTM1QGdtYWlsLmNvbSIsInVzZXJJZCI6MiwiaWF0IjoxNzc1NjIxMTYzLCJleHAiOjE3NzU3MDc1NjN9.cSI5W3ebbKChiaUVcL4DwByThm9ruLidwQSG2X0Tn2DzxIi6kj6b5if1YByyRKFPcV8v1DQExwDoWEWo3oHshg",
    [string]$BaseUrl = "https://backend-socialmedia-ixsm.onrender.com",
    [int]$CheckIntervalSeconds = 30,
    [switch]$Once = $false
)

$headers = @{
    "Authorization" = "Bearer $Token"
    "Content-Type" = "application/json"
}

function Check-VideoStatus {
    try {
        $response = Invoke-RestMethod -Uri "$BaseUrl/api/videos/$VideoId" -Headers $headers -Method GET
        
        Write-Host "`n=== VIDEO STATUS CHECK ===" -ForegroundColor Cyan
        Write-Host "ID: $($response.id)" -ForegroundColor White
        Write-Host "Title: $($response.title)" -ForegroundColor White
        Write-Host "Status: $($response.status)" -ForegroundColor $(if($response.status -eq "COMPLETED") { "Green" } elseif($response.status -eq "PROCESSING") { "Yellow" } else { "Red" })
        Write-Host "Created: $($response.createdAt)" -ForegroundColor Gray
        
        if ($response.videoUrl) {
            Write-Host "Video URL: $($response.videoUrl)" -ForegroundColor Green
        }
        
        if ($response.errorMessage) {
            Write-Host "Error: $($response.errorMessage)" -ForegroundColor Red
        }
        
        Write-Host "Updated: $($response.updatedAt)" -ForegroundColor Gray
        Write-Host "========================`n" -ForegroundColor Cyan
        
        return $response
    } catch {
        Write-Host "Error checking video status: $($_.Exception.Message)" -ForegroundColor Red
        return $null
    }
}

if ($Once) {
    Check-VideoStatus
} else {
    Write-Host "Checking video $VideoId status every $CheckIntervalSeconds seconds..." -ForegroundColor Yellow
    Write-Host "Press Ctrl+C to stop`n" -ForegroundColor Gray
    
    while ($true) {
        $status = Check-VideoStatus
        
        if ($status -and $status.status -eq "COMPLETED") {
            Write-Host "VIDEO IS READY! URL: $($status.videoUrl)" -ForegroundColor Green
            Write-Host "Press any key to exit..." -ForegroundColor Green
            $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
            break
        }
        
        if ($status -and $status.status -eq "FAILED") {
            Write-Host "VIDEO GENERATION FAILED!" -ForegroundColor Red
            Write-Host "Error: $($status.errorMessage)" -ForegroundColor Red
            break
        }
        
        Start-Sleep -Seconds $CheckIntervalSeconds
    }
}
