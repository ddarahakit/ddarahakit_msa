# GHCR(private) 로 다른 서버에 MSA 배포

GitHub Container Registry(private)에 이미지를 올리고, 다른 서버에서 pull 해 운영하는 절차.

- 커스텀 이미지 9개를 GHCR 에 push: `msa-discovery / msa-gateway / msa-frontend / msa-identity / msa-community / msa-commerce / msa-review / msa-course / msa-mentoring`
- 인프라(`mariadb` / `apache/kafka` / `openzipkin/zipkin`)는 공식 이미지라 GHCR 불필요
- **비밀은 이미지에 굽지 않고** 대상 서버의 `.env` 로 런타임 주입

---

## A. dev 머신: 빌드 & GHCR push

### 1) GHCR 로그인
GitHub PAT 발급 — Settings → Developer settings → Personal access tokens.
- classic: `write:packages`, `read:packages`, `delete:packages` 체크
- fine-grained: Packages 권한 Read+Write

```powershell
$env:CR_PAT = "ghp_xxx"
$env:CR_PAT | docker login ghcr.io -u <github-username> --password-stdin
```

### 2) 빌드 + push
프론트는 **빌드타임에 공개 API URL 이 번들에 박히므로**, 반드시 대상 서버의 공개 게이트웨이 주소로 빌드한다.

```powershell
# 도메인 + HTTPS
./scripts/ghcr-push.ps1 -Owner ddarahakit -Tag 1.0.0 -Build -ApiBaseUrl "https://app.example.com"

# 또는 IP + HTTP (게이트웨이 8080)
./scripts/ghcr-push.ps1 -Owner ddarahakit -Tag 1.0.0 -Build -ApiBaseUrl "http://1.2.3.4:8080"
```

이미 빌드된 `:dev` 이미지를 태깅만 해서 올리려면 `-Build` 없이 실행.

### 3) private 확인
GitHub → 프로필/조직 → **Packages** → 각 패키지 → Package settings → **Visibility = Private** 확인.
(GHCR 는 최초 push 시 private 으로 생성됨)

---

## B. 대상 서버: 설치 & 실행

### 1) Docker 설치 (Ubuntu 예시)
```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER   # 재로그인
```

### 2) 배포 파일 준비
이 3가지를 서버에 둔다(레포 클론 또는 직접 복사):
- `docker-compose.deploy.yml`
- `infra/db/init/`  (스키마 7개 생성 SQL)
- `.env`  (아래 3번에서 작성)

### 3) GHCR 로그인 + .env 작성
```bash
echo "ghp_xxx" | docker login ghcr.io -u <github-username> --password-stdin   # read:packages PAT
cp .env.deploy.example .env
vi .env     # 모든 비밀/URL 채우기 (.env.deploy.example 주석 참고)
```
> ⚠️ `ALLOWED_ORIGINS` 는 프론트 빌드 시 넣은 `VITE_API_BASE_URL` 의 출처(프론트가 떠 있는 주소)와 맞춰야 CORS 통과.
> ⚠️ 다른 서버에선 `APP_TOKEN_ACCESS_FORMAT/REFRESH_FORMAT` 으로 쿠키 `Domain`/`Secure` 를 환경에 맞게 주입(미설정 시 localhost 기본값 → 쿠키 거부됨).

### 4) 실행
```bash
docker compose -f docker-compose.deploy.yml pull
docker compose -f docker-compose.deploy.yml up -d
docker compose -f docker-compose.deploy.yml ps
```

### 5) DB 데이터 주입(선택)
컨테이너 최초 기동 시 `infra/db/init` 으로 **스키마(빈 테이블)만** 생성된다. 기존 데이터를 옮기려면 덤프를 import:
```bash
# dev 머신에서 스키마별 덤프(예: --single-transaction 비잠금)
# 대상 서버에서:
docker exec -i msa-mariadb mariadb -uddarahakit -p"$DB_PASSWORD" identity_db  < identity_db.sql
docker exec -i msa-mariadb mariadb -uddarahakit -p"$DB_PASSWORD" course_db    < course_db.sql
# ... commerce_db / community_db / review_db / mentoring_db
```

---

## C. 외부 노출 / 방화벽
- **8080**(gateway), **8081**(frontend)만 공개. DB/Kafka/Eureka/Zipkin 은 내부망 전용(포트 미매핑).
- 운영이라면 8080/8081 앞에 **리버스 프록시(nginx/Caddy)+HTTPS** 를 두는 것을 권장(그 경우 프론트/쿠키를 https/wss·도메인으로 빌드·주입).

## D. 업데이트(재배포)
```powershell
# dev: 새 태그로 push
./scripts/ghcr-push.ps1 -Owner ddarahakit -Tag 1.0.1 -Build -ApiBaseUrl "https://app.example.com"
```
```bash
# server: .env 의 IMAGE_TAG=1.0.1 로 바꾼 뒤
docker compose -f docker-compose.deploy.yml pull
docker compose -f docker-compose.deploy.yml up -d
```

## 트러블슈팅
- `pull` 시 `denied`/`unauthorized` → 서버에서 `docker login ghcr.io`(read:packages) 재확인, 패키지 visibility/권한 확인.
- 로그인은 되는데 화면에서 로그인 유지 안 됨 → 쿠키 `Domain` 불일치. `APP_TOKEN_*_FORMAT` 을 접속 주소에 맞게(IP면 Domain 생략).
- 프론트에서 API 401/CORS → 프론트 빌드 `VITE_API_BASE_URL` 과 `ALLOWED_ORIGINS` 불일치. 동일 출처로 맞추고 프론트 재빌드·push.
- 기동 직후 503 → Eureka 등록 지연(~30s). 잠시 후 정상.
