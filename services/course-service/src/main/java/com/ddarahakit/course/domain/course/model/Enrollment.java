package com.ddarahakit.course.domain.course.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 수강권 읽기모델(이벤트 투영).
 * commerce-service 의 OrderPaid 이벤트를 소비해 (userIdx, courseIdx) 수강권을 적재하고,
 * OrderRefunded 로 회수한다. 구매 여부/수강생 수/인기순 산출의 단일 출처.
 */
@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "enrollment",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_idx", "course_idx"})
        }
)
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column(name = "user_idx", nullable = false)
    private Long userIdx;

    @Column(name = "course_idx", nullable = false)
    private Long courseIdx;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;
}
