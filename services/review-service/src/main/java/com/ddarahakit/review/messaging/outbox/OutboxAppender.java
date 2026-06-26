package com.ddarahakit.review.messaging.outbox;

import com.ddarahakit.common.event.serde.EventSerde;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 비즈니스 트랜잭션과 동일한 DB 트랜잭션 안에서 호출되어 아웃박스 테이블에 이벤트를 적재한다.
 * 실제 Kafka 발행은 {@link OutboxRelay} 가 비동기로 수행한다(트랜잭셔널 아웃박스 패턴).
 */
@Component
@RequiredArgsConstructor
public class OutboxAppender {
    private final OutboxRepository repo;

    public void append(String topic, String eventType, String aggregateId, Object payload) {
        OutboxEntity e = new OutboxEntity();
        e.setEventId(java.util.UUID.randomUUID().toString());
        e.setTopic(topic);
        e.setEventType(eventType);
        e.setAggregate(topic);
        e.setAggregateId(aggregateId);
        e.setPayload(EventSerde.toNode(payload).toString());
        e.setCreatedAt(java.time.Instant.now());
        repo.save(e);
    }
}
