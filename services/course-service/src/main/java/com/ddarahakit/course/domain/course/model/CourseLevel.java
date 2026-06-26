package com.ddarahakit.course.domain.course.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 코스 난이도.
 * DB(course.level, varchar)는 한글 라벨(초급/중급/고급)로 저장된다.
 * description = DB 저장값(한글 초급/중급/고급) 이며, {@code CourseLevelConverter} 가 enum ↔ 한글 라벨을 변환한다.
 * displayName = 화면 표시용 라벨(쉬움/보통/어려움) — DTO 의 levelDescription 으로 노출된다.
 */
@Getter
@RequiredArgsConstructor
public enum CourseLevel {
    BEGINNER("초급", "쉬움"),
    INTERMEDIATE("중급", "보통"),
    ADVANCED("고급", "어려움");

    private final String description; // DB 저장 라벨(초급/중급/고급) — 컨버터 전용
    private final String displayName; // 화면 표시 라벨(쉬움/보통/어려움)

    /**
     * DB 한글 라벨 → enum. 미지/널/이미 영문 코드까지 방어적으로 처리해 조회가 500 나지 않게 한다.
     */
    public static CourseLevel fromDbLabel(String raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return null;
        }
        for (CourseLevel level : values()) {
            if (level.description.equals(value) || level.displayName.equals(value)) {
                return level;
            }
        }
        for (CourseLevel level : values()) {
            if (level.name().equalsIgnoreCase(value)) {
                return level;
            }
        }
        return null;
    }
}
