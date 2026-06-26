package com.ddarahakit.course.domain.course.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class LectureDto {
    @Getter
    @Builder
    public static class LectureReq {
        private String name;
        private boolean free;
        private int playTime;
        private String videoUrl;
        private String text;
        private String content;
        @Schema(description = "섹션 IDX", required = true, example = "1")
        @Min(message = "올바른 섹션 IDX를 입력해주세요.", value = 1)
        private Long sectionIdx;

        public Lecture toEntity() {
            return Lecture.builder()
                    .name(name)
                    .free(free)
                    .playTime(playTime)
                    .videoUrl(videoUrl)
                    .text(text)
                    .content(content)
                    .section(Section.builder().idx(sectionIdx).build())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class LectureRes {
        private Long idx;
        private String name;
        private boolean free;
        private int playTime;
        private String videoUrl;
        private String content;
        private boolean complete;
        private CourseDto.CourseRes course;

        public static LectureDto.LectureRes of(Lecture entity, List<Long> lectureCompletes) {
            return LectureRes.builder()
                    .idx(entity.getIdx())
                    .name(entity.getName())
                    .free(entity.isFree())
                    .playTime(entity.getPlayTime())
                    .videoUrl(entity.getVideoUrl())
                    .content(entity.getContent())
                    .complete(lectureCompletes.contains(entity.getIdx()))
                    .course(CourseDto.CourseRes.of(entity.getSection().getCourse(), 0, lectureCompletes))
                    .build();
        }

        public static LectureDto.LectureRes of(Lecture entity) {
            return LectureRes.builder()
                    .idx(entity.getIdx())
                    .name(entity.getName())
                    .free(entity.isFree())
                    .playTime(entity.getPlayTime())
                    .videoUrl(entity.getVideoUrl())
                    .content(entity.getContent())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class LectureCompleteRes {
        private Long idx;
        public static LectureDto.LectureCompleteRes of(Lecture entity) {
            return LectureCompleteRes.builder()
                    .idx(entity.getIdx())
                    .build();
        }

    }

    @Getter
    @Builder
    public static class LectureCompleteReq {
        private Long courseIdx;
        private Long lectureIdx;

        public LectureComplete toEntity(Long userIdx) {
            return LectureComplete.builder()
                    .userIdx(userIdx)
                    .course(Course.builder().idx(courseIdx).build())
                    .lecture(Lecture.builder().idx(lectureIdx).build())
                    .build();
        }
    }

}
