package com.ddarahakit.course.domain.course.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "lecture_complete",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_idx", "lecture_idx"})
        }
)
public class LectureComplete {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    // MSA: user_idx 는 식별자만 보관(User 엔티티는 identity-service 소유).
    @Column(name = "user_idx")
    private Long userIdx;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lecture_idx")
    private Lecture lecture;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_idx")
    private Course course;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    void onCreate() {
        if (this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        }
    }
}
