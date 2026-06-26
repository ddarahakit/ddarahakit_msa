# 05. 공통 모듈 — 이벤트 계약 코드

[← ARCHITECTURE.md](../ARCHITECTURE.md) · 관련: [02. Kafka](02-event-driven-kafka.md) · [04. DB 스키마](04-database-schema.md)

`common` 모듈은 **이벤트 계약·공통 응답만** 담는다. 서비스 로직/엔티티는 절대 넣지 않는다(공유 라이브러리 과결합 방지).
이벤트는 발행/소비 양쪽이 공유하는 **단일 진실의 계약**이다.

```
common/
├─ build.gradle
└─ src/main/java/com/ddarahakit/common/
    ├─ response/   BaseResponse.java, BaseResponseStatus.java   (모놀리스에서 이관)
    └─ event/
        ├─ EventEnvelope.java        # 봉투(메타 + payload)
        ├─ EventType.java            # 이벤트 타입 상수
        ├─ Topics.java               # 토픽명 상수
        ├─ payload/                  # 이벤트별 페이로드 record
        │   ├─ OrderPaid.java
        │   ├─ OrderRefunded.java
        │   ├─ ReviewCreated.java
        │   ├─ ReviewUpdated.java
        │   ├─ ReviewDeleted.java
        │   ├─ UserRegistered.java
        │   ├─ UserProfileChanged.java
        │   └─ UserDeleted.java
        └─ serde/                    # 직렬화 헬퍼(Jackson)
            └─ EventSerde.java
```

> ⚠️ `common`은 **Spring·JPA 의존 없이** 순수 record + Jackson만. 그래야 어떤 서비스든 가볍게 의존 가능.

---

## 1. 봉투(Envelope)

```java
package com.ddarahakit.common.event;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

/**
 * 모든 도메인 이벤트의 공통 봉투.
 * - eventId: 멱등 키(소비자 중복 차단)
 * - type/version: 라우팅·계약 버전
 * - traceId: 분산추적 전파
 * - payload: 이벤트별 본문(JsonNode → 소비 측에서 record 로 역직렬화)
 */
public record EventEnvelope(
        String eventId,
        String type,        // EventType 상수
        int version,
        Instant occurredAt,
        String traceId,
        JsonNode payload
) {
    public static EventEnvelope of(String eventId, String type, int version,
                                   Instant occurredAt, String traceId, JsonNode payload) {
        return new EventEnvelope(eventId, type, version, occurredAt, traceId, payload);
    }
}
```

## 2. 토픽 / 타입 상수

```java
public final class Topics {
    public static final String COMMERCE_ORDER = "commerce.order.v1";
    public static final String REVIEW_REVIEW  = "review.review.v1";
    public static final String IDENTITY_USER  = "identity.user.v1";
    private Topics() {}
}

public final class EventType {
    public static final String ORDER_PAID      = "OrderPaid";
    public static final String ORDER_REFUNDED  = "OrderRefunded";
    public static final String REVIEW_CREATED  = "ReviewCreated";
    public static final String REVIEW_UPDATED  = "ReviewUpdated";
    public static final String REVIEW_DELETED  = "ReviewDeleted";
    public static final String USER_REGISTERED = "UserRegistered";
    public static final String USER_PROFILE_CHANGED = "UserProfileChanged";
    public static final String USER_DELETED    = "UserDeleted";
    private EventType() {}
}
```

## 3. 페이로드 계약 (record)

```java
package com.ddarahakit.common.event.payload;
import java.time.Instant;
import java.util.List;

// commerce → commerce.order.v1
public record OrderPaid(long orderId, long userId, List<Long> courseIds, Instant paidAt) {}
public record OrderRefunded(long orderId, long userId, List<Long> courseIds) {}

// review → review.review.v1   (key = courseId)
public record ReviewCreated(long reviewId, long courseId, long userId, int rating) {}
public record ReviewUpdated(long reviewId, long courseId, int oldRating, int newRating) {}
public record ReviewDeleted(long reviewId, long courseId, int rating) {}

// identity → identity.user.v1  (key = userId)
public record UserRegistered(long userId, String email, String name) {}
public record UserProfileChanged(long userId, String name, String profileImageUrl) {}
public record UserDeleted(long userId) {}
```
> **하위호환 규칙**: 같은 `vN` 토픽에는 **필드 추가만** 허용(소비자는 모르는 필드 무시). 필드 의미 변경/삭제는 `v(N+1)` 신토픽.

## 4. 직렬화 헬퍼

```java
public final class EventSerde {
    private static final ObjectMapper M = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    public static String toJson(EventEnvelope e) { /* writeValueAsString */ }
    public static EventEnvelope fromJson(String json) { /* readValue */ }
    public static <T> T payload(EventEnvelope e, Class<T> type) {
        return M.convertValue(e.payload(), type);   // envelope.payload(JsonNode) → record
    }
    public static JsonNode toNode(Object payload) { return M.valueToTree(payload); }
}
```

---

## 5. 발행 측 — 아웃박스 적재 (서비스 코드 예시)

비즈니스 변경과 **같은 트랜잭션**에서 outbox에 INSERT만 한다(실제 Kafka 전송은 릴레이가 담당).

```java
// commerce-service
@Transactional
public void confirmPaid(Order order) {
    order.markPaid();                                  // orders UPDATE
    var payload = new OrderPaid(order.id(), order.userId(), order.courseIds(), Instant.now());
    outbox.append(Topics.COMMERCE_ORDER, EventType.ORDER_PAID,
                  String.valueOf(/*key*/ order.firstCourseId()), payload);  // outbox INSERT
}                                                      // ← 둘이 원자적 커밋
```

```java
// OutboxAppender (각 발행 서비스 공통)
public void append(String topic, String type, String aggregateId, Object payload) {
    var e = new OutboxEntity();
    e.setEventId(UUID.randomUUID().toString());
    e.setTopic(topic); e.setEventType(type);
    e.setAggregate(topic); e.setAggregateId(aggregateId);   // aggregateId = Kafka 파티션 키
    e.setPayload(EventSerde.toNode(payload).toString());
    e.setCreatedAt(Instant.now());
    outboxRepo.save(e);
}
```

## 6. 릴레이 — 미발행 outbox → Kafka

```java
@Scheduled(fixedDelay = 1000)
@Transactional
public void publishPending() {
    for (OutboxEntity o : outboxRepo.findTop100ByPublishedAtIsNullOrderById()) {
        var env = EventEnvelope.of(o.getEventId(), o.getEventType(), 1,
                                   o.getCreatedAt(), TraceContext.current(), parse(o.getPayload()));
        kafka.send(o.getTopic(), o.getAggregateId(), EventSerde.toJson(env));  // key=aggregateId
        o.setPublishedAt(Instant.now());                                       // 멱등(중복 전송은 소비자가 흡수)
    }
}
```
> 운영 승급 시 이 폴링 릴레이를 **Debezium CDC**로 교체(코드 변경 없이 outbox→Kafka 커넥터).

## 7. 소비 측 — 멱등 처리 (course-service 평점 투영)

```java
@KafkaListener(topics = Topics.REVIEW_REVIEW, groupId = "course-rating")
@Transactional
public void onReview(String message) {
    EventEnvelope e = EventSerde.fromJson(message);
    if (!processedEvents.tryInsert(e.eventId(), "course-rating")) return;  // 멱등 가드
    switch (e.type()) {
        case EventType.REVIEW_CREATED -> {
            var p = EventSerde.payload(e, ReviewCreated.class);
            courseRating.apply(p.courseId(), p.rating(), +1);
        }
        case EventType.REVIEW_DELETED -> {
            var p = EventSerde.payload(e, ReviewDeleted.class);
            courseRating.apply(p.courseId(), p.rating(), -1);
        }
        case EventType.REVIEW_UPDATED -> {
            var p = EventSerde.payload(e, ReviewUpdated.class);
            courseRating.apply(p.courseId(), p.oldRating(), -1);
            courseRating.apply(p.courseId(), p.newRating(), +1);
        }
        default -> { /* 관심 없는 타입 무시 */ }
    }
}
```

---

## 8. 의존 규약
- `common`은 `gateway`를 제외한 **전 서비스가 의존**(이벤트 계약 공유).
- **버전 충돌 방지**: `common`은 가급적 추가-호환만 릴리스. 멀티모듈이라 같은 빌드에서 일괄 반영.
- gateway는 이벤트를 다루지 않으므로 `common`의 `response` 패키지만(또는 의존 없이) 사용.
