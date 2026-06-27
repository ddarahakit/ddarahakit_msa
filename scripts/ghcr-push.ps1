<#
  GHCR(GitHub Container Registry) private 로 MSA 이미지 빌드·태깅·푸시.

  사전 1) GHCR 로그인 (PAT 은 write:packages 권한; classic PAT 또는 fine-grained)
    $env:CR_PAT = "ghp_xxx"
    $env:CR_PAT | docker login ghcr.io -u <github-username> --password-stdin

  사용 예)
    # 빌드 + 푸시 (프론트는 대상 서버 공개 API URL 로 빌드해야 함!)
    ./scripts/ghcr-push.ps1 -Owner ddarahakit -Tag 1.0.0 -Build `
        -ApiBaseUrl "https://app.example.com"

    # 이미 빌드된 :dev 이미지를 태깅만 해서 푸시
    ./scripts/ghcr-push.ps1 -Owner ddarahakit -Tag 1.0.0

  ※ GHCR 패키지는 처음 push 시 private 으로 생성된다(공개로 바꾸지 않는 한 private 유지).
  ※ 프론트(VITE_*)는 "빌드타임" 에 URL 이 번들에 박힌다 → 대상 서버 공개주소로 빌드해야 브라우저가 게이트웨이를 찾는다.
#>
param(
  [string]$Owner = "ddarahakit",
  [string]$Tag = "latest",
  [switch]$Build,
  [string]$ApiBaseUrl,   # 예: https://app.example.com  또는  http://1.2.3.4:8080
  [string]$WsUrl         # 미지정 시 ApiBaseUrl 에서 자동 유도(http→ws, https→wss) + /mentoring/ws/signal
)
$ErrorActionPreference = "Stop"

# 로컬 이미지(:dev) → GHCR 리포지토리명
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
  Write-Host "==> 백엔드/인프라 이미지 빌드 (docker-compose.msa.yml)" -ForegroundColor Cyan
  docker compose -f docker-compose.msa.yml build `
    eureka gateway identity-service community-service commerce-service review-service course-service mentoring-service
  if ($LASTEXITCODE -ne 0) { throw "backend build 실패" }

  if (-not $ApiBaseUrl) { throw "프론트 빌드에는 -ApiBaseUrl (대상 서버 공개 API/게이트웨이 URL) 이 필요합니다." }
  if (-not $WsUrl) {
    $WsUrl = ($ApiBaseUrl -replace '^https', 'wss' -replace '^http', 'ws') + "/mentoring/ws/signal"
  }
  Write-Host "==> 프론트 빌드 (VITE_API_BASE_URL=$ApiBaseUrl, WS=$WsUrl)" -ForegroundColor Cyan
  docker build -t ddarahakit/msa-frontend:dev ./ddarahakit_frontend `
    --build-arg VITE_API_BASE_URL=$ApiBaseUrl `
    --build-arg VITE_IMG_BASE_URL=$ApiBaseUrl `
    --build-arg VITE_MENTORING_WS_URL=$WsUrl
  if ($LASTEXITCODE -ne 0) { throw "frontend build 실패" }
}

foreach ($local in $map.Keys) {
  $remote = "ghcr.io/$Owner/$($map[$local]):$Tag"
  Write-Host "==> tag $local -> $remote" -ForegroundColor Green
  docker tag $local $remote
  if ($LASTEXITCODE -ne 0) { throw "tag 실패: $local (먼저 -Build 로 빌드했는지 확인)" }
  docker push $remote
  if ($LASTEXITCODE -ne 0) { throw "push 실패: $remote (docker login ghcr.io 확인)" }
}

Write-Host "`n==> 완료: ghcr.io/$Owner/msa-* : $Tag 푸시됨" -ForegroundColor Cyan
Write-Host "   GitHub > Packages 에서 각 패키지 visibility 가 private 인지 확인하세요."
