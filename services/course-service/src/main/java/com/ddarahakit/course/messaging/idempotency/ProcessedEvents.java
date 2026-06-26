package com.ddarahakit.course.messaging.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 소비 멱등성 게이트. 이미 처리한 (eventId, consumer) 면 false 를 반환해 재처리를 막는다.
 */
@Component
@RequiredArgsConstructor
public class ProcessedEvents {

    private final ProcessedEventRepository repo;

    @Transactional
    public boolean tryInsert(String eventId, String consumer) {
        if (repo.existsByEventIdAndConsumer(eventId, consumer)) {
            return false;
        }
        repo.save(new ProcessedEvent(eventId, consumer, Instant.now()));
        return true;
    }
}
