# 14. 마이페이지 집계 이전 — 모놀리스 은퇴 (완료)

[← ARCHITECTURE.md](../ARCHITECTURE.md) · 관련: [03. 인증·게이트웨이](03-auth-gateway.md) · [13. course-service](13-phase5-course.md)

5단계까지 추출 후 모놀리스에 남은 건 **마이페이지 집계 6종**뿐이었다. 이 데이터는 이미 각 서비스가 소유하므로(BFF 합성이 아니라) **소유 서비스로 라우팅**하면 모놀리스가 완전히 비워진다.

## 이전한 엔드포인트 (모놀리스 → 소유 서비스)
| 엔드포인트 | 이전 대상 | 데이터 출처 |
|---|---|---|
| `/user/ordered` (내 강의실) | **course-service** | enrollment + lecture_complete(진도) |
| `/user/study/weekly` (주간 학습) | **course-service** | lecture_complete 주간 집계 |
| `/user/myreview` (내 리뷰) | **review-service** | review + 코스명 Feign(course-service) |
| `/user/mypost`, `/user/myquestion` | **community-service** | post(작성자/코스명 스냅샷) + 댓글수 COUNT |
| `/user/payments` (결제내역) | **commerce-service** | orders(코스명/금액 스냅샷, 페이징) |

- 각 서비스는 헤더 인증(`X-User-Id`)으로 현재 사용자 식별. 신규 컨트롤러만 추가(기존 로직 재사용).
- 대부분 **단일 서비스 소유**라 합성 BFF 불필요. `/user/myreview` 만 코스명을 course-service 에서 Feign 으로 보강.

## 게이트웨이 라우팅 (최종)
```
identity   ← /user(login·logout·token·signup·email·password·profile·check·uuid)·/oauth2
community  ← /community/**            + /user/mypost, /user/myquestion
commerce   ← /orders/**, /cart/**     + /user/payments
review     ← /review/**               + /user/myreview
course     ← /course·/roadmap·/stats  + /user/ordered, /user/study/**
monolith   ← (캐치올, 트래픽 0 — 은퇴)
```

## 검증 (게이트웨이 http://localhost:8080)
| 호출 | 결과 |
|---|---|
| `/user/ordered` | **200** — enrollment 기반, **course 1 포함**(OrderPaid 이벤트로 받은 수강권) → course-service 라우팅 증명 |
| `/user/study/weekly` | **200** (course-service) |
| `/user/myreview` | **200** (review-service, 코스명 Feign) |
| `/user/mypost`·`/user/myquestion` | **200** (community-service) |
| `/user/payments` | **200** (commerce-service) |

→ **모든 `/user/*` 경로가 identity(인증) 또는 소유 서비스(집계)로 라우팅** → 모놀리스 캐치올로 가는 트래픽 0.

## 모놀리스 완전 제거 (완료)
- `docker-compose.msa.yml` 에서 `monolith` 서비스 삭제, 게이트웨이 캐치올 라우트(`RoutesConfig`)·`MONOLITH_URI` env 제거.
- 모놀리스를 URL Feign 으로 호출하던 곳(commerce 가격검증·review 코스존재)의 `MONOLITH_URL` 을 `http://course-service:8080` 으로 전환(course-service 가 `/course/{id}` 제공) → 코드 변경 없이 재배선.
- 결과: **컨테이너 9개**(mariadb·kafka·eureka·gateway + 5서비스). `msa-monolith` 제거.
- 제거 후 검증: 전 도메인·집계 6종 200, **commerce 주문생성**(course Feign 가격검증)·**review 작성**(course Feign 존재확인) 200 — 모놀리스 없이 완전 동작.

## 최종 결과
모놀리식 Spring Boot → **이벤트 기반 마이크로서비스 5종**(identity·community·commerce·review·course) + 게이트웨이 + Eureka + Kafka 로 **Strangler Fig 무중단 전환 완료**. 동기(Feign)·비동기(아웃박스→Kafka→멱등 소비자 투영)·DB-per-service·헤더 인증·스냅샷 비정규화·읽기모델(enrollment/rating) 등 정석 패턴을 **실데이터 E2E 로 검증**하고, 마지막으로 모놀리스를 은퇴시켜 전환을 마무리했다.
