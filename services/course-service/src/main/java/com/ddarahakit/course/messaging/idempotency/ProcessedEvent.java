package com.ddarahakit.course.messaging.idempotency;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * 소비 멱등성 기록. (eventId, consumer) 복합 PK.
 * 동일 이벤트를 같은 소비자가 두 번 처리하지 않도록 차단한다.
 */
@Entity
@Table(name = "processed_event")
@IdClass(ProcessedEvent.Pk.class)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", length = 100, nullable = false)
    private String eventId;

    @Id
    @Column(name = "consumer", length = 100, nullable = false)
    private String consumer;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pk implements Serializable {
        private String eventId;
        private String consumer;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pk pk = (Pk) o;
            return Objects.equals(eventId, pk.eventId) && Objects.equals(consumer, pk.consumer);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventId, consumer);
        }
    }
}
