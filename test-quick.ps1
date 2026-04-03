param(
    [string]$Token = ""
)

if (-not $Token) {
    Write-Host "Uso: ./test-quick.ps1 -Token 'tu_token'" -ForegroundColor Red
    exit 1
}

$base = "https://backend-socialmedia-ixsm.onrender.com"
$headers = @{ "Authorization" = "Bearer $Token"; "Content-Type" = "application/json" }

Write-Host "1. Verificando usuario..." -ForegroundColor Cyan
$user = Invoke-RestMethod -Uri "$base/api/auth/me" -Headers $headers
Write-Host "OK - $($user.name)" -ForegroundColor Green

Write-Host "`n2. Generando video con Hugging Face..." -ForegroundColor Cyan
Write-Host "   (puede tardar 1-3 minutos)" -ForegroundColor Yellow
try {
    $video = Invoke-RestMethod -Uri "$base/api/videos/generate" -Method POST -Headers $headers -Body (@{
        title = "Mi Video IA"
        description = "Video generado con Hugging Face"
        prompt = "A cat sleeping in a hammock under the sun"
    } | ConvertTo-Json)

    Write-Host "OK - Video ID: $($video.id) | Estado: $($video.status)" -ForegroundColor Green
    Write-Host "URL: $($video.videoUrl)" -ForegroundColor Gray

    if ($video.status -eq "COMPLETED" -and $video.videoUrl) {
        Write-Host "`n3. Publicando en YouTube..." -ForegroundColor Cyan
        try {
            $youtube = Invoke-RestMethod -Uri "$base/api/youtube/publish" -Method POST -Headers $headers -Body (@{
                videoId = $video.id
                title = "Video IA - $($video.title)"
                description = "Video generado con inteligencia artificial"
                visibility = "PRIVATE"
            } | ConvertTo-Json)
            Write-Host "OK - YouTube URL: $($youtube.youtubeUrl)" -ForegroundColor Green
        } catch {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            Write-Host "Error YouTube: $($reader.ReadToEnd())" -ForegroundColor Red
        }
    } else {
        Write-Host "`nVideo en estado $($video.status) - no se puede publicar en YouTube aun" -ForegroundColor Yellow
    }

} catch {
    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    Write-Host "Error: $($reader.ReadToEnd())" -ForegroundColor Red
}