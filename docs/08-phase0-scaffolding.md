# 08. 0단계 — 스캐폴딩 (구현 완료)

[← ARCHITECTURE.md](../ARCHITECTURE.md) · 관련: [06. docker-compose](06-docker-compose.md) · [07. 패키지 구조](07-package-structure.md)

모노레포 골격 + 인프라(Eureka·Gateway) + 공통 모듈을 구성하고, **기존 모놀리스를 게이트웨이 뒤에 정적 라우팅**으로 두었다(Strangler Fig 출발점). 모놀리스 코드는 전혀 건드리지 않는다.

## 구성한 것

```
ddarahakit_portfolio/                 # = MSA 모노레포 루트
├─ settings.gradle / build.gradle     # 멀티모듈 + Spring Boot 3.4.2 / Spring Cloud 2024.0.0 BOM
├─ gradlew, gradle/wrapper/           # Gradle 8.10 래퍼
├─ common/                            # ✅ 이벤트 계약·공통 응답 (java-library, 빌드 검증됨)
│   └─ …/event/{EventEnvelope,EventType,Topics, payload/*, serde/EventSerde}, response/BaseResponse
├─ infra/
│   ├─ discovery/                     # ✅ Eureka 서버 (bootJar 검증됨)
│   ├─ gateway/                       # ✅ Spring Cloud Gateway (bootJar 검증됨)
│   │   └─ filter/JwtAuthGlobalFilter # ATOKEN 검증 → X-User-* 주입 + 위조헤더 strip
│   │   └─ config/{RoutesConfig,CorsConfig}
│   └─ db/init/01-schemas.sql         # 5개 스키마 + ddarahakit(모놀리스) 생성
├─ ddarahakit_backend/                # 기존 모놀리스(0단계 Strangler 대상, 미변경)
└─ docker-compose.msa.yml             # mariadb·kafka·eureka·gateway·monolith
```

## 빌드 검증
```
$ ./gradlew :common:build :infra:discovery:build :infra:gateway:build -x test
BUILD SUCCESSFUL — common(jar) · discovery(bootJar) · gateway(bootJar)
```

## 게이트웨이 동작 (0단계)
- **모든 경로 `/**` → 모놀리스**(`RoutesConfig`, 정적 `MONOLITH_URI`). 서비스 추출 시 해당 경로만 `lb://<service>`로 위에 추가.
- **`JwtAuthGlobalFilter`**: `ATOKEN` 쿠키의 액세스 JWT 검증(모놀리스와 동일 secret/issuer/claims) → `X-User-Id`/`X-User-Role` 주입, 클라이언트 위조 헤더 제거. 무효/부재는 익명 통과(보호는 모놀리스가 그대로 수행).
  - 0단계엔 모놀리스가 쿠키를 직접 검증하므로 주입 헤더는 **포워드룩(1단계부터 서비스가 신뢰)**.
- **CORS**: 0단계는 게이트웨이 CORS off(`gateway.cors.enabled` 기본 false) → 모놀리스가 CORS 처리(ACAO 중복 방지). 서비스 추출 후 게이트웨이로 승격.

## 실행
```bash
# .env 에 DB_ROOT_PASSWORD/DB_USER/DB_PASSWORD/JWT_SECRET/AES_SECRET_KEY/OAuth/PortOne/ALLOWED_ORIGINS 필요
docker compose -f docker-compose.msa.yml up -d --build
docker compose -f docker-compose.msa.yml ps
# 확인
curl http://localhost:8080/course/list        # gateway → monolith
open http://localhost:8761                     # Eureka 대시보드
```
> 이 MSA 스택은 운영 스택(`docker-compose.yml`)과 **별도 실행**(mariadb 3307 로 노출해 충돌 회피). 모놀리스 DB(`ddarahakit`)는 빈 스키마로 시작 → 필요 시 운영 덤프 import.

## 인프라 선반영(1단계 대비)
- **Kafka(KRaft)** + 토픽 3종(`commerce.order.v1`·`review.review.v1`·`identity.user.v1`) 자동 생성.
- **MariaDB** 에 서비스 스키마 5종 선생성(`identity_db`…`review_db`).

## 다음(1단계) 착수 지점
1. `services/identity-service` 모듈 추가(`settings.gradle` 주석 해제) — User/RefreshToken/EmailVerify 이관, `identity_db`.
2. 게이트웨이 `RoutesConfig`에 `identity` 라우트(`/user/**`,`/oauth2/**`) → `lb://identity-service` 추가(모놀리스 라우트 위).
3. identity 가 Eureka 등록 → 게이트웨이가 디스커버리로 호출.
4. 헤더 인증 필터(`HeaderAuthenticationFilter`)로 identity 가 `X-User-*` 신뢰([07 참조](07-package-structure.md)).
