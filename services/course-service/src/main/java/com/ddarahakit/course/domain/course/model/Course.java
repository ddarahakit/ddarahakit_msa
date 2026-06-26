package com.ddarahakit.course.domain.course.model;

import com.ddarahakit.course.common.model.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;

import java.util.ArrayList;
import java.util.List;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Entity
public class Course extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(length = 200)
    private String image;

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_idx")
    private Category category;

    @Min(0)
    private int originalPrice;

    @Min(0)
    private int salePrice;

    // 코스 난이도. DB(varchar)는 한글(초급/중급/고급)로 저장 → CourseLevelConverter 가 enum 과 변환.
    @Convert(converter = CourseLevelConverter.class)
    @Column(length = 20)
    private CourseLevel level;

    @ColumnDefault("1")
    private Boolean isDisplay;

    @Setter
    @Min(0)
    @ColumnDefault("0")
    private int rating1;

    @Setter
    @Min(0)
    @ColumnDefault("0")
    private int rating2;

    @Setter
    @Min(0)
    @ColumnDefault("0")
    private int rating3;

    @Setter
    @Min(0)
    @ColumnDefault("0")
    private int rating4;

    @Setter
    @Min(0)
    @ColumnDefault("0")
    private int rating5;

    @Setter
    @Min(0)
    @ColumnDefault("0")
    private Integer totalReviewsCount;

    // MSA: user_idx 는 식별자만 보관(User 엔티티는 identity-service 소유).
    @Column(name = "user_idx")
    private Long userIdx;

    @Builder.Default
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Section> sections = new ArrayList<>();

    /**
     * 리뷰 평점 버킷(rating1..5)에 delta(+1/-1)를 적용한다.
     * review-service 의 Review* 이벤트를 소비해 course 가 자기 테이블에 평점을 투영한다.
     */
    public void applyRatingBucket(int rating, int delta) {
        switch (rating) {
            case 1 -> rating1 = Math.max(0, rating1 + delta);
            case 2 -> rating2 = Math.max(0, rating2 + delta);
            case 3 -> rating3 = Math.max(0, rating3 + delta);
            case 4 -> rating4 = Math.max(0, rating4 + delta);
            case 5 -> rating5 = Math.max(0, rating5 + delta);
            default -> {
                // 1~5 범위 밖 평점은 무시
            }
        }
    }

    /** 총 리뷰 수에 delta(+1/-1)를 적용한다. */
    public void applyReviewCount(int delta) {
        int current = totalReviewsCount == null ? 0 : totalReviewsCount;
        totalReviewsCount = Math.max(0, current + delta);
    }
}
