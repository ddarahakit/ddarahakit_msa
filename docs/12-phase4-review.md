# 12. 4단계 — review-service 추출 (완료)

[← ARCHITECTURE.md](../ARCHITECTURE.md) · 관련: [02. Kafka 이벤트](02-event-driven-kafka.md) · [04. DB 스키마](04-database-schema.md)

수강평을 `review-service`(`review_db`)로 분리. 핵심은 모놀리스의 **`UPDATE Course SET rating1..5`(리뷰→코스 직접쓰기)를 제거**하고 **`ReviewCreated/Updated/Deleted` 이벤트**로 전환한 것 — [02 §6 평점 투영](02-event-driven-kafka.md)의 실현.

## 추출 범위
- **소유 테이블(review_db)**: `review`, `outbox`
- **엔드포인트**: `GET/POST/PUT/DELETE /review/{courseIdx}`

## 핵심: 직접쓰기 → 이벤트
모놀리스 `ReviewService` 는 리뷰 저장과 동시에 `reviewRepository.adjustRatingBucket`/`adjustTotalReviewsCount` 로 **course 테이블의 rating 컬럼을 직접 UPDATE** 했다. MSA 에선 course 가 별도 서비스이므로 불가 → **두 @Modifying UPDATE 쿼리를 삭제**하고, 대신 아웃박스로 이벤트를 발행한다.

| 동작 | 발행 이벤트 | payload |
|---|---|---|
| createReview | `ReviewCreated` | reviewId, courseId, userId, rating |
| updateReview(평점 변경 시) | `ReviewUpdated` | reviewId, courseId, oldRating, newRating |
| remove | `ReviewDeleted` | reviewId, courseId, rating |

- 토픽 `review.review.v1`, 키 `courseId`(같은 코스 이벤트 순서 보장). 아웃박스→릴레이→Kafka(commerce 와 동일 패턴).
- **소비자(course rating 투영)는 5단계**에서 추가 — 지금은 토픽에 적재되어 대기.

## FK 평문화 + 스냅샷 / Feign
- Review: `@ManyToOne User/Course` → `Long userIdx/courseIdx` + `authorName` 스냅샷(작성자 표시명).
- 작성 시 **IdentityClient**(Feign)로 작성자명 스냅샷, **CourseClient**(Feign, url=monolith)로 코스 존재 확인.
- 헤더 인증(게이트웨이 신뢰): GET 공개, POST/PUT/DELETE 인증.

## 데이터 이관 + 백필
review 13건 이관, `author_name` 을 모놀리스 user 조인으로 백필(전부 채움).

## E2E 검증 (게이트웨이 http://localhost:8080)
| 호출 | 결과 |
|---|---|
| `GET /review/11` | **200** — 작성자 스냅샷(한지우·이서윤) |
| `POST /review/11` {rating:5} | **200** — review 114 생성, Feign 작성자(심준보) |
| Kafka `review.review.v1` | **`ReviewCreated` 발행 확인** — `{"type":"ReviewCreated","payload":{"reviewId":114,"courseId":11,"userId":1,"rating":5}}` |
| course/user/community/orders/review 5개 도메인 | 전부 **200** |

## 현재 라우팅 맵 (4서비스 추출 완료)
```
identity  ← /user(인증)·/oauth2        commerce ← /orders·/cart   (Kafka OrderPaid)
community ← /community                 review   ← /review/**       (Kafka Review*)
monolith  ← /course, /roadmap, /stats, /user(집계)
```

## 누적된 전환기 갭(5단계에서 해소)
- 신규 주문 → 모놀리스 수강권 미반영(OrderPaid 대기)
- 신규 리뷰 → 모놀리스 course rating 미반영(Review* 대기)
→ **5단계 course-service** 가 `OrderPaid`·`Review*` 를 **구독**해 `enrollment`·`rating` 읽기모델을 만들면 두 갭이 동시에 닫힌다. course 추출과 함께 모놀리스는 stats/roadmap 정도만 남고, 집계는 BFF 로.

## 다음(5단계) — course-service 코어 + stats BFF
가장 많이 의존받는 코어. course/section/lecture/category/roadmap + **enrollment·rating 읽기모델(이벤트 소비)**. 마이페이지 집계는 게이트웨이 BFF 로.
