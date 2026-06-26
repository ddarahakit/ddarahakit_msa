package com.ddarahakit.review.domain.review.model;

import com.ddarahakit.review.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "review",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_idx", "course_idx"})
        }
)
public class Review extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column(length = 100, nullable = false)
    private String comment;

    @Column(length = 200, nullable = false)
    private int rating;

    @Column(name = "user_idx", nullable = false)
    private Long userIdx;

    @Column(name = "course_idx", nullable = false)
    private Long courseIdx;

    /** 작성자 표시명 스냅샷(identity-service 에서 조회한 값을 저장). */
    @Column(name = "author_name")
    private String authorName;

}
