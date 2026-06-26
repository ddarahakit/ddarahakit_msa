package com.ddarahakit.review.domain.review.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Slice;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;

public class ReviewDto {
    @Getter
    public static class ReviewReq {

        @Schema(description = "댓글 내용", required = true, example = "댓글 내용입니다.")
        @NotBlank(message = "댓글의 내용은 필수입니다.")
        @Size(min = 1, max = 500, message = "댓글의 내용은 최대 500자까지만 가능합니다.")
        private String comment;
        @Schema(description = "별점", required = true, example = "5")
        @Min(value = 1, message = "별점은 최소 1점부터 최대 5점까지만 가능합니다.")
        @Max(value = 5, message = "별점은 최소 1점부터 최대 5점까지만 가능합니다.")
        private int rating;

        public Review toEntity(Long userIdx, Long courseIdx, String authorName) {
            return Review.builder()
                    .comment(comment)
                    .rating(rating)
                    .userIdx(userIdx)
                    .courseIdx(courseIdx)
                    .authorName(authorName)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ReviewRes {
        private Long idx;
        private String comment;
        private int rating;
        private String userName;
        private String createdAt;   // 작성일 (yyyy.MM.dd)

        public static ReviewDto.ReviewRes of(Review entity) {
            return ReviewDto.ReviewRes.builder()
                    .idx(entity.getIdx())
                    .comment(entity.getComment())
                    .rating(entity.getRating())
                    .userName(entity.getAuthorName())
                    .createdAt(entity.getCreatedAt() != null
                            ? new SimpleDateFormat("yyyy.MM.dd").format(entity.getCreatedAt())
                            : null)
                    .build();
        }
    }

    /**
     * 내 리뷰(마이페이지) 응답. 코스명은 review 가 소유하지 않으므로 course-service Feign 으로 주입한다(실패 시 null).
     * 모놀리스 ReviewDto.MyReviewRes(courseIdx/name + 중첩 review) 구조를 따른다.
     */
    @Getter
    @Builder
    public static class MyReviewRes {
        private Long courseIdx;
        private String courseName;
        private Integer rating;
        private String comment;
        private String createdAt;   // 작성일 (yyyy.MM.dd)

        public static MyReviewRes of(Review entity, String courseName) {
            return MyReviewRes.builder()
                    .courseIdx(entity.getCourseIdx())
                    .courseName(courseName)
                    .rating(entity.getRating())
                    .comment(entity.getComment())
                    .createdAt(entity.getCreatedAt() != null
                            ? new SimpleDateFormat("yyyy.MM.dd").format(entity.getCreatedAt())
                            : null)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ReviewPageRes {
        private List<ReviewRes> list;
        private boolean hasNext;  // 다음 페이지 여부

        public static ReviewPageRes of(Slice<Review> reviewPage) {
            return ReviewPageRes.builder()
                    .list(reviewPage != null
                            ? reviewPage.getContent().stream().map(ReviewDto.ReviewRes::of).toList()
                            : Collections.emptyList())
                    .hasNext(reviewPage != null
                            ? reviewPage.hasNext()
                            : false)
                    .build();
        }
    }
}
