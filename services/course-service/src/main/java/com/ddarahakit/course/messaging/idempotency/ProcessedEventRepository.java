package com.ddarahakit.course.messaging.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEvent.Pk> {
    boolean existsByEventIdAndConsumer(String eventId, String consumer);
}
