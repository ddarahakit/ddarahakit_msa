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
            try {
                if (o.getTraceparent() != null) {
                    var rec = new org.apache.kafka.clients.producer.ProducerRecord<String, String>(
                            o.getTopic(), o.getAggregateId(), json);
                    rec.headers().add("traceparent",
                            o.getTraceparent().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    kafka.send(rec).get(5, java.util.concurrent.TimeUnit.SECONDS);
                } else {
                    kafka.send(o.getTopic(), o.getAggregateId(), json)
                            .get(5, java.util.concurrent.TimeUnit.SECONDS);
                }
                // 브로커 ACK 확인 후에만 발행 완료로 마킹(실패 시 publishedAt=null 유지 → 다음 폴링 재시도)
                o.setPublishedAt(java.time.Instant.now());
            } catch (Exception e) {
                log.warn("아웃박스 발행 실패 eventId={} type={} topic={} (다음 폴링에서 재시도)",
                        o.getEventId(), o.getEventType(), o.getTopic(), e);
                // 한 건 실패가 배치 전체를 막지 않도록 계속 진행. 미마킹 건은 다음 폴링에서 재발행된다.
            }
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
