package com.ddarahakit.course.domain.stats;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

public class StatsDto {

    @Getter
    @Builder
    @Schema(description = "서비스 요약 통계")
    public static class SummaryRes {
        @Schema(description = "전체 코스 수")
        private final long courseCount;

        @Schema(description = "고유 수강생 수(수강권 보유 기준)")
        private final long studentCount;

        @Schema(description = "만족도(4점 이상 리뷰 비율, 0~100). 리뷰가 없으면 0")
        private final int satisfactionRate;
    }
}
