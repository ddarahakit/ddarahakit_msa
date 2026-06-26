package com.ddarahakit.course.domain.roadmap.model;

import com.ddarahakit.course.domain.course.model.Course;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

public class RoadmapDto {

    @Getter
    @Builder
    public static class CreateReq {
        @NotBlank(message = "로드맵 이름을 입력해주세요.")
        @Size(max = 100, message = "로드맵 이름은 100자 이하로 입력해주세요.")
        private String name;
        private String image;
        private String description;
        private List<CourseItem> courses;
    }

    @Getter
    @Builder
    public static class UpdateReq {
        @NotBlank(message = "로드맵 이름을 입력해주세요.")
        @Size(max = 100, message = "로드맵 이름은 100자 이하로 입력해주세요.")
        private String name;
        private String image;
        private String description;
        private List<CourseItem> courses;
    }

    @Getter
    @Builder
    public static class CourseItem {
        private Long courseIdx;
        private int sortOrder;
    }

    @Getter
    @Builder
    public static class RoadmapListRes {
        private Long idx;
        private String name;
        private String image;
        private String description;
        private int courseCount;

        public static RoadmapListRes of(Roadmap entity) {
            return RoadmapListRes.builder()
                    .idx(entity.getIdx())
                    .name(entity.getName())
                    .image(entity.getImage())
                    .description(entity.getDescription())
                    .courseCount(entity.getRoadmapCourses().size())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class RoadmapDetailRes {
        private Long idx;
        private String name;
        private String image;
        private String description;
        private List<RoadmapCourseRes> courses;

        public static RoadmapDetailRes of(Roadmap entity) {
            List<RoadmapCourseRes> courses = entity.getRoadmapCourses() != null
                    ? entity.getRoadmapCourses().stream()
                    .map(RoadmapCourseRes::of)
                    .toList()
                    : Collections.emptyList();

            return RoadmapDetailRes.builder()
                    .idx(entity.getIdx())
                    .name(entity.getName())
                    .image(entity.getImage())
                    .description(entity.getDescription())
                    .courses(courses)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class RoadmapCourseRes {
        private int sortOrder;
        private Long courseIdx;
        private String courseName;
        private String courseImage;

        public static RoadmapCourseRes of(RoadmapCourse entity) {
            Course course = entity.getCourse();
            return RoadmapCourseRes.builder()
                    .sortOrder(entity.getSortOrder())
                    .courseIdx(course.getIdx())
                    .courseName(course.getName())
                    .courseImage(course.getImage())
                    .build();
        }
    }
}
