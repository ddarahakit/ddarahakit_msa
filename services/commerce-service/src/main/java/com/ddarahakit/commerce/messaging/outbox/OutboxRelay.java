package com.ddarahakit.commerce.messaging.outbox;

import com.ddarahakit.common.event.EventEnvelope;
import com.ddarahakit.common.event.serde.EventSerde;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 미발행 아웃박스 레코드를 주기적으로 폴링해 Kafka 로 발행하고 publishedAt 을 마킹한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {
    private final OutboxRepository repo;
    private final KafkaTemplate<String, String> kafka;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publish() {
        for (OutboxEntity o : repo.findTop100ByPublishedAtIsNullOrderById()) {
            EventEnvelope env = EventEnvelope.of(o.getEventId(), o.getEventType(), 1,
                    o.getCreatedAt(), null, parseJson(o.getPayload()));
            String json = EventSerde.toJson(env);
            if (o.getTraceparent() != null) {
                var rec = new org.apache.kafka.clients.producer.ProducerRecord<String, String>(
                        o.getTopic(), o.getAggregateId(), json);
                rec.headers().add("traceparent",
                        o.getTraceparent().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                kafka.send(rec);
            } else {
                kafka.send(o.getTopic(), o.getAggregateId(), json);
            }
            o.setPublishedAt(java.time.Instant.now());
        }
    }

    private com.fasterxml.jackson.databind.JsonNode parseJson(String s) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
