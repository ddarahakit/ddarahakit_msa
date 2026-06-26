package com.ddarahakit.course.domain.study;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class StudyDto {

    @Getter
    @Builder
    @Schema(description = "주간 학습활동")
    public static class WeeklyRes {
        @Schema(description = "최근 7일 일별 수강완료 수(오래된→최신 순, 7개)")
        private final List<DayCount> days;

        @Schema(description = "연속 학습일 수(오늘 또는 어제부터 거꾸로, 하루 1강 이상 완료 기준)")
        private final int streakDays;

        @Schema(description = "이번 주(최근 7일) 총 수강완료 수")
        private final long weeklyCompleted;

        @Schema(description = "주간 목표 대비 달성률(0~100). 목표 = WEEKLY_GOAL 강(기본 5)")
        private final int goalRate;

        @Schema(description = "주간 목표 강의 수(가정값)")
        private final int weeklyGoal;
    }

    @Getter
    @Builder
    @Schema(description = "일별 수강완료 수")
    public static class DayCount {
        @Schema(description = "날짜(yyyy-MM-dd)")
        private final String date;
        @Schema(description = "해당 일 수강완료 강의 수")
        private final long count;
    }
}
