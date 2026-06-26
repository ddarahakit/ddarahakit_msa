package com.ddarahakit.course.domain.stats;

import com.ddarahakit.course.common.model.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/stats")
@Tag(name = "통계 컨트롤러")
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "서비스 요약 통계", description = "코스 수/수강생 수/만족도를 조회한다. 비로그인 허용.")
    @GetMapping("/summary")
    public ResponseEntity<BaseResponse<StatsDto.SummaryRes>> summary() {
        return ResponseEntity.ok(BaseResponse.success(statsService.summary()));
    }

    @Operation(summary = "메인 통계(overview)", description = "/stats/summary 와 동일 응답(코스 수/수강생 수/만족도). 비로그인 허용.")
    @GetMapping("/overview")
    public ResponseEntity<BaseResponse<StatsDto.SummaryRes>> overview() {
        return ResponseEntity.ok(BaseResponse.success(statsService.summary()));
    }
}
