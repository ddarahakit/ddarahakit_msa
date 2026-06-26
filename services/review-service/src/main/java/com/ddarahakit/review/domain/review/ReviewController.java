package com.ddarahakit.review.domain.review;

import com.ddarahakit.review.common.model.BaseResponse;
import com.ddarahakit.review.config.security.AuthUserDetails;
import com.ddarahakit.review.domain.review.model.ReviewDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/review")
@RestController
@Tag(name = "리뷰 컨트롤러")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(
            summary = "댓글 더 보기",
            description = "페이지 번호 입력으로 댓글 내용 더 보기.")
    @GetMapping("/{courseIdx}")
    public ResponseEntity<BaseResponse<ReviewDto.ReviewPageRes>> readReview(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @PathVariable Long courseIdx,
            @PageableDefault(size = 4, sort = "idx", direction = Sort.Direction.DESC) Pageable pageable) {
        ReviewDto.ReviewPageRes response = reviewService.readReview(authUserDetails, courseIdx, pageable);

        return ResponseEntity.ok(BaseResponse.success(response));
    }


    @Operation(
            summary = "댓글 작성",
            description = "댓글 내용 및 별점 입력으로 댓글 내용 작성.")
    @PostMapping("/{courseIdx}")
    public ResponseEntity<BaseResponse<ReviewDto.ReviewRes>> createReview(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @PathVariable Long courseIdx,
            @Valid @RequestBody ReviewDto.ReviewReq dto) {
        ReviewDto.ReviewRes response = reviewService.createReview(authUserDetails, courseIdx, dto);

        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(
            summary = "댓글 수정",
            description = "댓글 내용 및 별점 입력으로 댓글 내용 수정.")
    @PutMapping("/{courseIdx}")
    public ResponseEntity<BaseResponse<ReviewDto.ReviewRes>> updateReview(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @PathVariable Long courseIdx,
            @Valid @RequestBody ReviewDto.ReviewReq dto) {
        ReviewDto.ReviewRes response = reviewService.updateReview(authUserDetails, courseIdx, dto);

        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(
            summary = "댓글 삭제",
            description = "코스 IDX로 해당 댓글 삭제")
    @DeleteMapping("/{courseIdx}")
    public ResponseEntity<BaseResponse<ReviewDto.ReviewRes>> removeReview(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @PathVariable Long courseIdx) {

        ReviewDto.ReviewRes response = reviewService.remove(authUserDetails, courseIdx);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
