package com.ddarahakit.mentoring.domain.mentoring;

import com.ddarahakit.mentoring.common.model.BaseResponse;
import com.ddarahakit.mentoring.config.security.AuthUserDetails;
import com.ddarahakit.mentoring.domain.mentoring.model.MentoringDto;
import com.ddarahakit.mentoring.domain.mentoring.model.MentoringStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/mentoring")
@Tag(name = "멘토링 컨트롤러")
public class MentoringController {
    private final MentoringService mentoringService;

    @Operation(summary = "멘토링 세션 목록 조회", description = "멘토링 세션 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<BaseResponse<MentoringDto.SessionListRes>> list(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) MentoringStatus status
    ) {
        MentoringDto.SessionListRes response = mentoringService.list(authUserDetails, page, size, keyword, status);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(summary = "멘토링 세션 상세/대화 내역 조회", description = "멘토링 세션 상세와 대화 내역을 조회합니다.")
    @GetMapping("/{sessionIdx}")
    public ResponseEntity<BaseResponse<MentoringDto.SessionDetailRes>> detail(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @PathVariable Long sessionIdx
    ) {
        MentoringDto.SessionDetailRes response = mentoringService.detail(authUserDetails, sessionIdx);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(summary = "멘토링 세션 메시지 목록 조회", description = "멘토링 세션 메시지 목록을 조회합니다.")
    @GetMapping("/{sessionIdx}/messages")
    public ResponseEntity<BaseResponse<MentoringDto.MessagePageRes>> messages(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @PathVariable Long sessionIdx,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long before
    ) {
        MentoringDto.MessagePageRes response = mentoringService.messages(authUserDetails, sessionIdx, page, size, before);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(summary = "멘토링 세션 생성", description = "멘토링 세션을 생성합니다.")
    @PostMapping
    public ResponseEntity<BaseResponse<MentoringDto.SessionRes>> create(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @Valid @RequestBody MentoringDto.CreateSessionReq req
    ) {
        MentoringDto.SessionRes response = mentoringService.createSession(authUserDetails, req);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(summary = "멘토링 메시지 전송", description = "멘토링 세션에 메시지를 전송합니다.")
    @PostMapping("/{sessionIdx}/messages")
    public ResponseEntity<BaseResponse<MentoringDto.MessageRes>> sendMessage(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @PathVariable Long sessionIdx,
            @Valid @RequestBody MentoringDto.SendMessageReq req
    ) {
        MentoringDto.MessageRes response = mentoringService.sendMessage(authUserDetails, sessionIdx, req);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(summary = "멘토링 세션 읽음 처리", description = "멘토링 세션을 읽음 처리합니다.")
    @PatchMapping("/{sessionIdx}/read")
    public ResponseEntity<BaseResponse<MentoringDto.SessionRes>> markAsRead(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @PathVariable Long sessionIdx
    ) {
        MentoringDto.SessionRes response = mentoringService.markAsRead(authUserDetails, sessionIdx);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(summary = "멘토링 세션 종료/삭제", description = "멘토링 세션을 종료합니다.")
    @DeleteMapping("/{sessionIdx}")
    public ResponseEntity<BaseResponse<MentoringDto.SessionRes>> close(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @PathVariable Long sessionIdx
    ) {
        MentoringDto.SessionRes response = mentoringService.close(authUserDetails, sessionIdx);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
