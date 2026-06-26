package com.ddarahakit.community.domain.community;

import com.ddarahakit.community.common.model.BaseResponse;
import com.ddarahakit.community.config.security.AuthUserDetails;
import com.ddarahakit.community.domain.community.model.CommunityDto.PostSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 마이페이지 집계(community-service 소유분).
 * 모놀리스 은퇴를 위해 내 질문/내 게시글 조회를 community-service 로 이전한다. 인증은 헤더(X-User-Id) 기반.
 */
@Tag(name = "마이페이지(커뮤니티) 컨트롤러")
@RequiredArgsConstructor
@RestController
public class MyController {

    private final CommunityService communityService;

    @Operation(summary = "내 질문 조회", description = "현재 사용자가 작성한 질문 게시글 목록을 조회한다.")
    @GetMapping("/user/myquestion")
    public ResponseEntity<BaseResponse<List<PostSummaryResponse>>> myQuestions(
            @AuthenticationPrincipal AuthUserDetails authUserDetails) {
        List<PostSummaryResponse> response = communityService.getMyQuestionList(authUserDetails);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(summary = "내 게시글 조회", description = "현재 사용자가 작성한 게시글(질문/공지 제외) 목록을 조회한다.")
    @GetMapping("/user/mypost")
    public ResponseEntity<BaseResponse<List<PostSummaryResponse>>> myPosts(
            @AuthenticationPrincipal AuthUserDetails authUserDetails) {
        List<PostSummaryResponse> response = communityService.getMyPostList(authUserDetails);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
