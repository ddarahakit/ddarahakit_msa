package com.ddarahakit.course.messaging.consumer;

import com.ddarahakit.common.event.EventEnvelope;
import com.ddarahakit.common.event.EventType;
import com.ddarahakit.common.event.Topics;
import com.ddarahakit.common.event.payload.OrderEvents;
import com.ddarahakit.common.event.serde.EventSerde;
import com.ddarahakit.course.domain.course.service.EnrollmentService;
import com.ddarahakit.course.messaging.idempotency.ProcessedEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * commerce.order.v1 소비 → 수강권(enrollment) 투영.
 * OrderPaid: 결제된 코스에 수강권 부여. OrderRefunded: 환불 코스 수강권 회수.
 */
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private static final String CONSUMER = "course-enrollment";

    private final ProcessedEvents processedEvents;
    private final EnrollmentService enrollmentService;

    @KafkaListener(topics = Topics.COMMERCE_ORDER, groupId = CONSUMER)
    @Transactional
    public void onOrder(String message) {
        EventEnvelope e = EventSerde.fromJson(message);
        if (!processedEvents.tryInsert(e.eventId(), CONSUMER)) {
            return;
        }
        switch (e.type()) {
            case EventType.ORDER_PAID -> {
                var p = EventSerde.payload(e, OrderEvents.OrderPaid.class);
                for (Long c : p.courseIds()) {
                    enrollmentService.grant(p.userId(), c, p.orderId());
                }
            }
            case EventType.ORDER_REFUNDED -> {
                var p = EventSerde.payload(e, OrderEvents.OrderRefunded.class);
                for (Long c : p.courseIds()) {
                    enrollmentService.revoke(p.userId(), c);
                }
            }
            default -> {
                // 관심 없는 타입은 무시
            }
        }
    }
}
