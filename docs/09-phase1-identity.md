# 09. 1단계 — identity-service 추출 (완료)

[← ARCHITECTURE.md](../ARCHITECTURE.md) · 관련: [01. 서비스 명세](01-services.md) · [03. 인증·게이트웨이](03-auth-gateway.md)

모놀리스의 **인증/회원 서브시스템**을 `identity-service`로 분리하고 `identity_db`를 소유시켰다. 마이페이지 집계는 남겨두고(모놀리스), 게이트웨이가 경로별로 분기한다.

## 추출 범위
- **소유 테이블(identity_db)**: `user`, `refresh_token`, `email_verify`
- **이관 코드**: SecurityConfig·JWT(JwtProvider/Filter/EntryPoint)·OAuth2(UserService/SuccessHandler/AuthorizedClientRepository)·TokenService/회전·UserController·UserService·EmailService·FileUploadService(프로필 이미지)·common/utils. 패키지 `com.ddarahakit.backend` → `com.ddarahakit.identity`.
- **제거(집계는 identity 소관 아님)**: UserController/UserService 의 `/ordered·/myreview·/mypost·/myquestion·/payments·/study/weekly` 및 course/orders/community/review 의존 → **모놀리스에 잔류**(게이트웨이가 모놀리스로 라우팅).

## 게이트웨이 라우팅 (RoutesConfig)
```
identity-service ← /user/login, /user/social/**, /user/logout(/all), /user/token/**,
                   /user/signup, /user/email/**, /user/check, /user/uuid/**,
                   /user/password/**, /user/profile, /oauth2/**, /login/oauth2/**
monolith(잔여)   ← 그 외 전부 (/user/ordered·myreview·…, /course, /orders, …)
```
- 라우트는 `lb://identity-service`(Eureka 디스커버리). 마이페이지 집계 경로는 identity 라우트에서 **의도적으로 제외** → 모놀리스 캐치올로.

## 인증 연속성
- identity 와 모놀리스가 **동일 `JWT_SECRET`·issuer·claims(idx/role/type=access)** 사용 → identity 가 발급한 ATOKEN 을 **모놀리스도 그대로 검증**. 쿠키는 dev 포맷(Domain=localhost, SameSite=Lax)으로 로컬 게이트웨이(http://localhost:8080)에서 동작.

## 데이터 이관
```sql
-- 같은 인스턴스 내 스키마 간 복사 (운영 무관, MSA 복사본만)
SET FOREIGN_KEY_CHECKS=0;
INSERT INTO identity_db.user          SELECT * FROM ddarahakit.user;
INSERT INTO identity_db.refresh_token SELECT * FROM ddarahakit.refresh_token;
INSERT INTO identity_db.email_verify  SELECT * FROM ddarahakit.email_verify;
SET FOREIGN_KEY_CHECKS=1;
```
컬럼 구조 동일(동일 엔티티) 확인 후 `SELECT *` 이관. 회원 15명 이관.

## E2E 검증 (게이트웨이 http://localhost:8080)
| 호출 | 라우팅 | 결과 |
|---|---|---|
| `POST /user/login` | → identity | **200**, ATOKEN 발급 |
| `GET /user/profile` | → identity (쿠키) | **200** |
| `GET /course/list` | → 모놀리스 | **200** (회귀 OK) |
| `GET /user/ordered` | → 모놀리스 (identity 토큰) | **200** + 실데이터 (모놀리스가 identity 토큰 수용) |

identity-service 는 Eureka 에 `IDENTITY-SERVICE` 로 등록, 게이트웨이가 `lb://` 로 호출.

## 다음(2단계) — community-service
의존 가장 적은 리프 도메인. `community_db` 분리, 게이트웨이 `/community/**` → `lb://community-service`. `UserDeleted` 구독(작성자 정리)·작성자 표시명은 BFF/투영으로.
