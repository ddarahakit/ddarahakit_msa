package com.ddarahakit.review.domain.review;

import com.ddarahakit.review.common.model.BaseResponse;
import com.ddarahakit.review.config.security.AuthUserDetails;
import com.ddarahakit.review.domain.review.model.ReviewDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 마이페이지 집계(review-service 소유분).
 * 모놀리스 은퇴를 위해 내 리뷰 조회를 review-service 로 이전한다. 인증은 헤더(X-User-Id) 기반.
 */
@RequiredArgsConstructor
@RestController
@Tag(name = "마이페이지(리뷰) 컨트롤러")
public class MyController {

    private final ReviewService reviewService;

    @Operation(summary = "내 리뷰 조회", description = "현재 사용자가 작성한 리뷰 목록을 코스명과 함께 조회한다.")
    @GetMapping("/user/myreview")
    public ResponseEntity<BaseResponse<List<ReviewDto.MyReviewRes>>> myReviews(
            @AuthenticationPrincipal AuthUserDetails authUserDetails) {
        List<ReviewDto.MyReviewRes> response = reviewService.getMyReviewList(authUserDetails);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
