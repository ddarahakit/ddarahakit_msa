package com.ddarahakit.course.client;

import com.ddarahakit.course.domain.course.model.ReviewDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 리뷰 본문은 review-service 소유다. 코스 상세에 임베드할 리뷰 페이지를 Eureka(lb)로 조회한다.
 * review-service 는 BaseResponse&lt;ReviewPageRes&gt;(success/results 래퍼)를 반환한다.
 */
@FeignClient(name = "review-service")
public interface ReviewClient {

    @GetMapping("/review/{courseIdx}")
    ReviewResponse getReviews(@PathVariable Long courseIdx);

    class ReviewResponse {
        public boolean success;
        public ReviewDto.ReviewPageRes results;
    }
}
