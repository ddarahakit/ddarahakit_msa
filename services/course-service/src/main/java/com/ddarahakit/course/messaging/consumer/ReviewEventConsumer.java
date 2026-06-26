package com.ddarahakit.course.messaging.consumer;

import com.ddarahakit.common.event.EventEnvelope;
import com.ddarahakit.common.event.EventType;
import com.ddarahakit.common.event.Topics;
import com.ddarahakit.common.event.payload.ReviewEvents;
import com.ddarahakit.common.event.serde.EventSerde;
import com.ddarahakit.course.domain.course.service.RatingService;
import com.ddarahakit.course.messaging.idempotency.ProcessedEvents;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * review.review.v1 소비 → 코스 평점(rating1..5, totalReviewsCount) 투영.
 */
@Component
@RequiredArgsConstructor
public class ReviewEventConsumer {

    private static final String CONSUMER = "course-rating";

    private final ProcessedEvents processedEvents;
    private final RatingService ratingService;

    @KafkaListener(topics = Topics.REVIEW_REVIEW, groupId = CONSUMER)
    @Transactional
    public void onReview(String message) {
        EventEnvelope e = EventSerde.fromJson(message);
        if (!processedEvents.tryInsert(e.eventId(), CONSUMER)) {
            return;
        }
        switch (e.type()) {
            case EventType.REVIEW_CREATED -> {
                var p = EventSerde.payload(e, ReviewEvents.ReviewCreated.class);
                ratingService.apply(p.courseId(), p.rating(), +1);
                ratingService.applyCount(p.courseId(), +1);
            }
            case EventType.REVIEW_DELETED -> {
                var p = EventSerde.payload(e, ReviewEvents.ReviewDeleted.class);
                ratingService.apply(p.courseId(), p.rating(), -1);
                ratingService.applyCount(p.courseId(), -1);
            }
            case EventType.REVIEW_UPDATED -> {
                var p = EventSerde.payload(e, ReviewEvents.ReviewUpdated.class);
                ratingService.apply(p.courseId(), p.oldRating(), -1);
                ratingService.apply(p.courseId(), p.newRating(), +1);
            }
            default -> {
                // 관심 없는 타입은 무시
            }
        }
    }
}
