package com.ddarahakit.review.messaging.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "outbox")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(unique = true)
    String eventId;

    String aggregate, aggregateId, topic, eventType;

    @Column(columnDefinition = "JSON")
    String payload;

    @Column(nullable = true)
    String traceparent;

    java.time.Instant createdAt;
    java.time.Instant publishedAt;
}
