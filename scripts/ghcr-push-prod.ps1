<#
  Push production (monolith) images to GHCR (private).
  Targets: ddarahakit/backend:prod, ddarahakit/frontend:prod  (mariadb is an official image)

  Prereq) docker login to GHCR (PAT with write:packages):
    $env:CR_PAT = "ghp_xxx"
    $env:CR_PAT | docker login ghcr.io -u <github-username> --password-stdin

  Usage:
    # tag + push already-built :prod images
    ./scripts/ghcr-push-prod.ps1 -Owner ddarahakit -Tag prod
    # build first (frontend uses VITE_*/domain from root .env) then push
    ./scripts/ghcr-push-prod.ps1 -Owner ddarahakit -Tag prod -Build

  Notes:
   - Frontend bakes the API domain at build time (current image targets https://api.ddarahakit.com).
     Same domain on the new server (Cloudflare repoint) -> reuse as-is. Different domain -> -Build then push.
   - GHCR packages are created private on first push.
#>
param(
  [string]$Owner = "ddarahakit",
  [string]$Tag = "prod",
  [switch]$Build
)
$ErrorActionPreference = "Stop"

# local image -> GHCR repo name
$map = [ordered]@{
  "ddarahakit/backend:prod"  = "backend"
  "ddarahakit/frontend:prod" = "frontend"
}

if ($Build) {
  Write-Host "==> build production images (docker-compose.yml)" -ForegroundColor Cyan
  docker compose -f docker-compose.yml build backend frontend
  if ($LASTEXITCODE -ne 0) { throw "build failed" }
}

foreach ($local in $map.Keys) {
  $remote = "ghcr.io/$Owner/$($map[$local]):$Tag"
  Write-Host "==> tag $local -> $remote" -ForegroundColor Green
  docker tag $local $remote
  if ($LASTEXITCODE -ne 0) { throw "tag failed: $local" }
  docker push $remote
  if ($LASTEXITCODE -ne 0) { throw "push failed: $remote (check: docker login ghcr.io)" }
}

Write-Host "`n==> done: ghcr.io/$Owner/backend:$Tag, ghcr.io/$Owner/frontend:$Tag pushed" -ForegroundColor Cyan
Write-Host "   Verify visibility=Private at GitHub > Packages."
