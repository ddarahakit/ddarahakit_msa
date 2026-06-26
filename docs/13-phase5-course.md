# 13. 5단계 — course-service 코어 추출 + 이벤트 소비자 (완료)

[← ARCHITECTURE.md](../ARCHITECTURE.md) · 관련: [02. Kafka 이벤트](02-event-driven-kafka.md) · [01. 서비스 명세](01-services.md)

가장 많이 의존받는 **코어(코스/강의/로드맵/통계)**를 `course-service`(`course_db`)로 분리하고, **이벤트 소비자**를 붙여 이벤트 드리븐의 **소비자 측을 완성**했다. 3·4단계에서 토픽에 쌓이던 `OrderPaid`·`Review*` 가 비로소 읽기모델로 투영된다.

## 추출 범위
- **소유 테이블(course_db)**: course, section, lecture, category, lecture_complete, roadmap, roadmap_course, **enrollment(수강권 읽기모델)**, **processed_event(멱등)**
- **엔드포인트**: `/course/**`, `/roadmap/**`, `/stats/**`

## 핵심: 이벤트 소비자 (★ 소비자 측 완성)
```
commerce ─ OrderPaid ─→ Kafka ─→ [course] OrderEventConsumer ─→ enrollment 부여/회수
review   ─ Review*   ─→ Kafka ─→ [course] ReviewEventConsumer ─→ course.rating 버킷 투영
```
- **멱등 소비**: `processed_event(eventId, consumer)` 로 중복 차단. `auto-offset-reset: earliest` 로 기동 시 토픽의 기존 이벤트도 소비.
- `enrollmentService.grant/revoke` (unique(userIdx,courseIdx) 멱등), `ratingService.apply/applyCount` (course 가 자기 테이블이므로 직접 갱신).

## orders/review 의존 제거
- 모놀리스 CourseService 의 `ordersItemRepository`(구매여부·인기순·수강생수) → **`EnrollmentRepository`** 로 전면 교체.
- 상세의 임베디드 리뷰 → **ReviewClient(Feign, lb://review-service)** `GET /review/{courseIdx}`(실패 시 빈 페이지 폴백).
- readLecture 수강권 = `enrollment.existsByUserIdxAndCourseIdx`.
- Stats: courseCount(자기), studentCount(enrollment distinct), 만족도(course rating 버킷).

## 데이터 이관 + enrollment 백필
- course 10 / section 81 / lecture 375 / lecture_complete 등 이관(course·lecture_complete 는 컬럼 순서 차이로 명시 컬럼 INSERT).
- enrollment 16건 = 결제완료 주문 백필 + `OrderPaid(110)` 소비분.

## E2E 검증 — 이벤트 루프 완성 (게이트웨이 http://localhost:8080)
| 검증 | 결과 |
|---|---|
| `OrderPaid(110)` 소비 → enrollment | user1 수강 `1,4,5,8,9,10` (course **1 은 commerce 이벤트로 부여**) |
| `/course/1` 상세 `ordered` | **true** (enrollment 기반) |
| `/course/lecture/1/9` 수강권 | **200** (enrollment 로 시청 허용) |
| **평점 투영**: `POST /review/8`(rating 5) | course 8 `rating5 1→2`, `total 2→3` — **review 이벤트 → course 소비자 투영** |
| 6개 도메인 라우팅(`/course·/roadmap/list·/stats·/community·/orders·/review·/user`) | 전부 **200** |
| Eureka | GATEWAY + 5 서비스 등록 |

## 최종 아키텍처
```
                         ┌──────────── API Gateway (JWT 검증·X-User-* 전파·라우팅) ───────────┐
 identity-service  ──────┤ /user(인증)·/oauth2                                               │
 community-service ──────┤ /community/**                          (UserDeleted 구독 예정)    │
 commerce-service  ──────┤ /orders·/cart       ── OrderPaid ──┐                              │
 review-service    ──────┤ /review/**          ── Review*   ──┤                              │
 course-service    ──────┤ /course·/roadmap·/stats  ◀── 소비 ─┘  enrollment·rating 투영     │
 monolith(잔여)    ──────┤ /user 집계(ordered·myreview·…) → 후속 BFF                          │
                         └──────────── Eureka · Kafka · MariaDB(스키마 6) ───────────────────┘
```

## 남은 일(후속)
- **마이페이지 BFF**: 모놀리스에 남은 집계 6종을 게이트웨이 `/me/**` BFF 로 이전(각 서비스 병렬 조합) → 모놀리스 완전 은퇴.
- community/review 의 `UserDeleted` 구독(탈퇴 정리), 표시명 이벤트 투영 등 보강.
- 운영 배포(엣지 nginx 업스트림을 게이트웨이로), 관측성(Zipkin), Debezium CDC 아웃박스 승급.

## 결과
모놀리식 → **이벤트 기반 MSA(5서비스 + 게이트웨이 + Eureka + Kafka)** 전환을 Strangler Fig 로 무중단 점진 완료. **동기(Feign 가격검증·작성자 조회)와 비동기(아웃박스→Kafka→멱등 소비자 투영)**, **DB-per-service·FK 평문화·스냅샷 비정규화·헤더 인증** 등 정석 패턴을 실데이터로 검증.
