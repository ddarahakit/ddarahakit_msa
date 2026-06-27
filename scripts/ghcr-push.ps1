<#
  Build/tag/push MSA images to GHCR (private).

  Prereq) docker login to GHCR (PAT with write:packages):
    $env:CR_PAT = "ghp_xxx"
    $env:CR_PAT | docker login ghcr.io -u <github-username> --password-stdin

  Usage:
    # build + push (frontend MUST be built with the target server public API URL)
    ./scripts/ghcr-push.ps1 -Owner ddarahakit -Tag 1.0.0 -Build -ApiBaseUrl "https://app.example.com"

    # tag + push already-built :dev images
    ./scripts/ghcr-push.ps1 -Owner ddarahakit -Tag 1.0.0

  Notes:
   - GHCR packages are created private on first push.
   - Frontend (VITE_*) bakes the API URL at build time -> build with the target server public URL
     so the browser can reach the gateway.
#>
param(
  [string]$Owner = "ddarahakit",
  [string]$Tag = "latest",
  [switch]$Build,
  [string]$ApiBaseUrl,   # e.g. https://app.example.com  or  http://1.2.3.4:8080
  [string]$WsUrl         # optional; derived from ApiBaseUrl if omitted
)
$ErrorActionPreference = "Stop"

# local image (:dev) -> GHCR repo name
$map = [ordered]@{
  "ddarahakit/msa-discovery:dev" = "msa-discovery"
  "ddarahakit/msa-gateway:dev"   = "msa-gateway"
  "ddarahakit/msa-identity:dev"  = "msa-identity"
  "ddarahakit/msa-community:dev" = "msa-community"
  "ddarahakit/msa-commerce:dev"  = "msa-commerce"
  "ddarahakit/msa-review:dev"    = "msa-review"
  "ddarahakit/msa-course:dev"    = "msa-course"
  "ddarahakit/msa-mentoring:dev" = "msa-mentoring"
  "ddarahakit/msa-frontend:dev"  = "msa-frontend"
}

if ($Build) {
  Write-Host "==> build backend/infra images (docker-compose.msa.yml)" -ForegroundColor Cyan
  docker compose -f docker-compose.msa.yml build `
    eureka gateway identity-service community-service commerce-service review-service course-service mentoring-service
  if ($LASTEXITCODE -ne 0) { throw "backend build failed" }

  if (-not $ApiBaseUrl) { throw "frontend build needs -ApiBaseUrl (target server public gateway URL)" }
  if (-not $WsUrl) {
    $WsUrl = ($ApiBaseUrl -replace '^https', 'wss' -replace '^http', 'ws') + "/mentoring/ws/signal"
  }
  Write-Host "==> build frontend (VITE_API_BASE_URL=$ApiBaseUrl, WS=$WsUrl)" -ForegroundColor Cyan
  docker build -t ddarahakit/msa-frontend:dev ./ddarahakit_frontend `
    --build-arg VITE_API_BASE_URL=$ApiBaseUrl `
    --build-arg VITE_IMG_BASE_URL=$ApiBaseUrl `
    --build-arg VITE_MENTORING_WS_URL=$WsUrl
  if ($LASTEXITCODE -ne 0) { throw "frontend build failed" }
}

foreach ($local in $map.Keys) {
  $remote = "ghcr.io/$Owner/$($map[$local]):$Tag"
  Write-Host "==> tag $local -> $remote" -ForegroundColor Green
  docker tag $local $remote
  if ($LASTEXITCODE -ne 0) { throw "tag failed: $local (build first with -Build)" }
  docker push $remote
  if ($LASTEXITCODE -ne 0) { throw "push failed: $remote (check: docker login ghcr.io)" }
}

Write-Host "`n==> done: ghcr.io/$Owner/msa-* : $Tag pushed" -ForegroundColor Cyan
Write-Host "   Verify each package visibility=Private at GitHub > Packages."
