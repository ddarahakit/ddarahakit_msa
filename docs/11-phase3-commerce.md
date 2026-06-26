# 11. 3단계 — commerce-service 추출 (완료)

[← ARCHITECTURE.md](../ARCHITECTURE.md) · 관련: [02. Kafka 이벤트](02-event-driven-kafka.md) · [05. 공통 이벤트 계약](05-common-events.md)

주문+장바구니를 `commerce-service`(`commerce_db`)로 분리. 가장 복잡한 단계로 **첫 Kafka 이벤트 발행(트랜잭션 아웃박스)** 과 **모놀리스로의 Feign 가격검증**을 도입했다.

## 추출 범위
- **소유 테이블(commerce_db)**: `orders`, `orders_item`, `cart`, `cart_item`, `outbox`
- **엔드포인트**: `/orders/**`(create·verify·free-complete·refund·receipt·check·delete), `/cart/**`(목록·담기·삭제·비우기·개수)

## 새 패턴 ① 트랜잭션 아웃박스 → Kafka (★ 핵심)
"DB 변경 + 이벤트 발행"의 이중쓰기 불일치를 막기 위해, 비즈니스 변경과 **같은 트랜잭션**에서 `outbox` 테이블에 적재하고 별도 릴레이가 Kafka 로 옮긴다.
```
결제확정(verify/free-complete) ─┐ 같은 @Transactional
                              ├─ orders.paid=true (markPaidIfUnpaid, 멱등)
                              └─ outbox INSERT (OrderPaid)
        OutboxRelay(@Scheduled 1s) → KafkaTemplate.send(commerce.order.v1, key=courseId, EventEnvelope) → published_at 마킹
```
- 발행 지점: `verify`·`freeComplete` 성공 → `OrderPaid`; `refund` 성공 → `OrderRefunded`.
- 봉투는 common 의 `EventEnvelope`+`EventSerde`+`Topics`+`OrderEvents` 사용(서비스 간 단일 계약).
- 소비자(course enrollment)는 5단계에서 추가 — 지금은 토픽에 적재만(이벤트는 영속, 멱등).

## 새 패턴 ② Feign → 모놀리스 (가격검증)
course-service 가 아직 없으므로 **모놀리스를 직접 URL 로 호출**:
```java
@FeignClient(name="course-pricing", url="${monolith.url}")  // http://monolith:8080
GET /course/{idx} → BaseResponse<{name, image, salePrice, originalPrice}>
```
- 주문 생성 시 각 코스 가격을 조회해 `paymentPrice == Σ salePrice` 서버측 재검증(위변조 차단 보존).
- 장바구니 담기 시 코스 name/image/price 를 조회해 `cart_item` 스냅샷에 저장.

## FK 평문화 + 스냅샷
| 엔티티 | 평문 ID | 스냅샷 |
|---|---|---|
| Orders | userIdx | — |
| OrdersItem | courseIdx | courseName, **unitPrice(결제시점 가격)** |
| Cart | userIdx | — |
| CartItem | courseIdx | courseName, courseImage, salePrice, originalPrice |

## 데이터 이관 + 백필
orders 13 / orders_item 15(코스명·unitPrice 백필) / cart 4 / cart_item 0. `unitPrice`·`courseName` 은 모놀리스 course 조인으로 채움.

## E2E 검증 (게이트웨이 http://localhost:8080)
| 호출 | 결과 |
|---|---|
| `POST /orders/create` {courseIdxList:[1],paymentPrice:0} | **200** — 모놀리스 Feign 가격검증 통과, 주문 110 생성 |
| `POST /orders/110/free-complete` | **200** — paid=true, `OrderPaid` 아웃박스 적재 |
| Kafka `commerce.order.v1` | **OrderPaid 발행 확인** — `{"type":"OrderPaid","payload":{"orderId":110,"userId":1,"courseIds":[1]}}` |
| `POST /cart` {courseIdx:3} → `GET /cart` | **200** — Feign 스냅샷("Docker", price) 표시 |
| `/course/list`·`/user/profile`·`/community/list`·`/orders/check/1` | **200** (4개 도메인 라우팅 정상) |

## 현재 라우팅 맵
```
identity   ← /user(인증/프로필)·/oauth2
community  ← /community/**
commerce   ← /orders/**, /cart/**          ← 3단계 추가
monolith   ← /course, /review, /roadmap, /stats, /user(집계)
```

## 알려진 전환기 갭(설계상 의도)
- 모놀리스의 수강권 확인(readLecture 등)은 자신의 `ddarahakit.orders` 를 읽으므로, commerce 에서 **새로 생성된 주문**은 모놀리스 수강권에 즉시 반영되지 않는다. → 5단계에서 course-service 가 `OrderPaid` 를 구독해 `enrollment` 읽기모델을 만들면 해소(현재 OrderPaid 는 토픽에 적재되어 대기).

## 다음(4단계) — review-service
`review_db` 분리. 모놀리스의 `UPDATE Course SET rating` 직접쓰기를 **`ReviewCreated/Updated/Deleted` 이벤트**로 전환(평점 투영). 두 번째 이벤트 발행 도메인.
