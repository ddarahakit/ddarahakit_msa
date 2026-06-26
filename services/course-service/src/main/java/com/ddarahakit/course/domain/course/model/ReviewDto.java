package com.ddarahakit.course.domain.course.model;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

/**
 * 코스 상세에 임베드되는 리뷰 응답 DTO.
 * 리뷰 본문은 review-service 소유이므로 ReviewClient(Feign) 로 조회한 결과를 그대로 담는다.
 * (review-service 의 ReviewDto.ReviewPageRes / ReviewRes 와 동일 형태)
 */
public class ReviewDto {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ReviewRes {
        private Long idx;
        private String comment;
        private int rating;
        private String userName;
        private String createdAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ReviewPageRes {
        private List<ReviewRes> list;
        private boolean hasNext;

        public static ReviewPageRes empty() {
            ReviewPageRes res = new ReviewPageRes();
            res.list = Collections.emptyList();
            res.hasNext = false;
            return res;
        }
    }
}
