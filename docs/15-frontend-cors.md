# 15. MSA 프론트엔드 + 게이트웨이 CORS (완료)

[← ARCHITECTURE.md](../ARCHITECTURE.md) · 관련: [03. 인증·게이트웨이](03-auth-gateway.md)

브라우저로 MSA 스택을 사용하도록 **기존 Vue SPA 를 게이트웨이 대상으로 빌드한 프론트 컨테이너**를 추가하고, **게이트웨이 CORS 를 일원화**했다.

## 프론트엔드 컨테이너
- 기존 `ddarahakit_frontend` 를 그대로 사용. Dockerfile 이 빌드 시 `VITE_API_BASE_URL` 을 번들에 인라인 → **`http://localhost:8080`(게이트웨이)** 로 빌드.
- 런타임은 nginx 정적 서빙(SPA history fallback) — api 프록시/TLS 없음(브라우저가 게이트웨이를 직접 호출).
- 호스트 **8081** 노출(운영 80/443 과 충돌 회피). `docker-compose.msa.yml` 의 `frontend` 서비스.

## CORS 일원화 (게이트웨이)
브라우저(`localhost:8081`) → 게이트웨이(`localhost:8080`) 는 **교차 출처**(포트 상이)라 CORS 필요. 게이트웨이가 CORS 를 전담:
- `gateway.cors.enabled=true`(env `GATEWAY_CORS_ENABLED`) → 조건부 `CorsConfig`(CorsWebFilter) 활성. 허용 출처 = `APP_ALLOWED_ORIGINS`(`http://localhost:8081`), `allowCredentials=true`(쿠키 인증).
- **이중 ACAO 방지**: 다운스트림 중 identity 만 자체 CORS 보유 → 게이트웨이와 헤더가 중복될 수 있음. `default-filters: DedupeResponseHeader=Access-Control-Allow-Origin Access-Control-Allow-Credentials, RETAIN_UNIQUE` 로 단일화.
- 프리플라이트(OPTIONS)는 게이트웨이 CorsWebFilter 가 단락 처리(서비스로 미전달).

## 쿠키 관점
- `localhost:8081` ↔ `localhost:8080` 은 **포트만 다른 동일 사이트**(site=scheme+host) → 교차 출처지만 same-site. identity 가 발급하는 `ATOKEN`(Domain=localhost, SameSite=Lax) 이 XHR 에 전송됨. CORS `allowCredentials=true` + 명시 출처(와일드카드 아님)와 정합.

## 검증
| 항목 | 결과 |
|---|---|
| `GET http://localhost:8081/` | **200** (SPA index) |
| `OPTIONS /user/login` (Origin=8081) | **200**, ACAO=`http://localhost:8081`, Allow-Credentials=true, Allow-Methods/Headers |
| `POST /user/login` ACAO 헤더 수 | **1** (게이트웨이+identity 중복 → dedupe 단일화) |
| `GET /course/list` ACAO | 단일(서비스 CORS 없음) |

## 구성 (최종, 10 컨테이너)
```
브라우저 → frontend(8081, 정적 SPA) ─XHR(CORS+쿠키)→ gateway(8080)
                                                       → identity·community·commerce·review·course
                                                       + Eureka · Kafka · MariaDB(스키마 6)
```

> 이미지 자원: 코스 썸네일은 Cloudflare R2 절대 URL 이라 정상 로드. 프로필 등 상대 경로(`/profile/...`) 이미지는 업로드 서비스 정적 서빙 미연동 시 404 가능(기능 영향 없음, 후속).
