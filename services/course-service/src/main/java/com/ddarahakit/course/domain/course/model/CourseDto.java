package com.ddarahakit.course.domain.course.model;

import com.ddarahakit.course.utils.TimeAgoUtil;
import lombok.Builder;
import lombok.Getter;

import java.util.*;

public class CourseDto {

    @Getter
    @Builder
    public static class CourseListRes {
        private List<CategoryRes> category;
        List<CourseSummaryRes> courses;
        public static CourseListRes of(List<CourseSummaryRes> courseResList) {
            return CourseListRes.builder()
                    .courses(courseResList)
                    .build();
        }

        public static CourseListRes of(Category category, List<CourseSummaryRes> courseResList) {
            List<CategoryRes> categoryResList = new ArrayList<>();

            categoryResList.add(CategoryRes.of(category));
            while(category.getParent() != null) {
                category = category.getParent();
                categoryResList.add(CategoryRes.of(category));
            }

            Collections.reverse(categoryResList);
            return CourseListRes.builder()
                    .category(categoryResList)
                    .courses(courseResList)
                    .build();
        }
    }

    /**
     * 코스 목록/검색/카테고리 응답 전용 슬림 DTO.
     * totalOrderedCount 는 enrollment(수강권) 집계로 산출해 주입한다(엔티티가 orders 컬렉션을 갖지 않음).
     */
    @Getter
    @Builder
    public static class CourseSummaryRes {
        private Long idx;
        private String name;
        private String image;
        private String text;
        private String description;
        private List<CategoryRes> category;
        private int originalPrice;
        private int salePrice;
        private String level;
        private String levelDescription;
        private int totalOrderedCount;
        private int totalReviewsCount;
        private int rating1;
        private int rating2;
        private int rating3;
        private int rating4;
        private int rating5;

        public static CourseSummaryRes of(Course entity) {
            return of(entity, 0);
        }

        public static CourseSummaryRes of(Course entity, int totalOrderedCount) {
            List<CategoryRes> path = new ArrayList<>();
            buildPath(entity.getCategory(), path);

            return CourseSummaryRes.builder()
                    .idx(entity.getIdx())
                    .name(entity.getName())
                    .image(entity.getImage())
                    .text(entity.getText())
                    .description(entity.getDescription())
                    .category(path)
                    .originalPrice(entity.getOriginalPrice())
                    .salePrice(entity.getSalePrice())
                    .level(entity.getLevel() != null ? entity.getLevel().name() : null)
                    .levelDescription(entity.getLevel() != null ? entity.getLevel().getDisplayName() : null)
                    .totalOrderedCount(totalOrderedCount)
                    .totalReviewsCount(entity.getTotalReviewsCount() == null ? 0 : entity.getTotalReviewsCount())
                    .rating1(entity.getRating1())
                    .rating2(entity.getRating2())
                    .rating3(entity.getRating3())
                    .rating4(entity.getRating4())
                    .rating5(entity.getRating5())
                    .build();
        }

        private static void buildPath(Category category, List<CategoryRes> path) {
            if (category == null) {
                return;
            }
            if (category.getParent() != null) {
                buildPath(category.getParent(), path);
            }
            path.add(CategoryRes.of(category));
        }
    }

    @Getter
    @Builder
    public static class CourseRes {
        private Long idx;
        private String name;
        private String image;
        private String text;
        private String description;
        private List<CategoryRes> category;
        private int originalPrice;
        private int salePrice;
        private String level;
        private String levelDescription;
        private int totalOrderedCount;
        private List<SectionRes> sections;
        private int totalReviewsCount;
        private int rating1;
        private int rating2;
        private int rating3;
        private int rating4;
        private int rating5;
        private boolean isReviewed;
        private boolean isOrdered;
        private ReviewDto.ReviewPageRes reviews;
        private Long nextLectureIdx;
        private String updatedAt;


        private static CourseRes.CourseResBuilder buildCommon(Course entity, int totalOrderedCount) {
            List<CategoryRes> path = new ArrayList<>();
            buildPath(entity.getCategory(), path);

            return CourseRes.builder()
                    .idx(entity.getIdx())
                    .name(entity.getName())
                    .image(entity.getImage())
                    .text(entity.getText())
                    .description(entity.getDescription())
                    .category(path)
                    .originalPrice(entity.getOriginalPrice())
                    .salePrice(entity.getSalePrice())
                    .level(entity.getLevel() != null ? entity.getLevel().name() : null)
                    .levelDescription(entity.getLevel() != null ? entity.getLevel().getDisplayName() : null)
                    .totalOrderedCount(totalOrderedCount)
                    .totalReviewsCount(entity.getTotalReviewsCount() == null ? 0 : entity.getTotalReviewsCount())
                    .rating1(entity.getRating1())
                    .rating2(entity.getRating2())
                    .rating3(entity.getRating3())
                    .rating4(entity.getRating4())
                    .rating5(entity.getRating5());
        }

        private static void buildPath(Category category, List<CategoryRes> path) {
            if (category == null) {
                return;
            }
            if (category.getParent() != null) {
                buildPath(category.getParent(), path);
            }
            path.add(CategoryRes.of(category));
        }

        private static List<SectionRes> mapSections(Course entity, List<Long> lectureCompletes) {
            return entity.getSections() != null
                    ? entity.getSections().stream()
                    .map(section -> SectionRes.of(section, lectureCompletes))
                    .toList()
                    : Collections.emptyList();
        }

        public static CourseRes of(Course entity) {
            return buildCommon(entity, 0)
                    .sections(mapSections(entity, Collections.emptyList()))
                    .nextLectureIdx(0L)
                    .build();
        }

        public static CourseRes of(Course entity, int totalOrderedCount, List<Long> lectureCompletes) {
            return buildCommon(entity, totalOrderedCount)
                    .sections(mapSections(entity, lectureCompletes))
                    .build();
        }

        /**
         * 내 강의실(수강 코스 목록) 전용. 진도(완료 강의)·다음 강의(이어듣기) idx 를 포함한다.
         * 모놀리스 CourseDto.CourseRes.of(course, nextLectureIdx, lectureCompletes) 와 동일한 의미.
         */
        public static CourseRes of(Course entity, Long nextLectureIdx, List<Long> lectureCompletes) {
            return buildCommon(entity, 0)
                    .sections(mapSections(entity, lectureCompletes))
                    .nextLectureIdx(nextLectureIdx)
                    .build();
        }

        public static CourseRes of(Course entity,
                                   int totalOrderedCount,
                                   ReviewDto.ReviewPageRes reviewPage,
                                   boolean isReviewed,
                                   boolean isOrdered,
                                   Long nextLectureIdx,
                                   List<Long> lectureCompletes) {
            return buildCommon(entity, totalOrderedCount)
                    .sections(mapSections(entity, lectureCompletes))
                    .reviews(reviewPage)
                    .isReviewed(isReviewed)
                    .isOrdered(isOrdered)
                    .nextLectureIdx(nextLectureIdx)
                    .updatedAt(entity.getUpdatedAt() != null ? TimeAgoUtil.timeAgo(entity.getUpdatedAt()) : null)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class CategoryTreeRes {
        private Long idx;
        private String name;
        private String slug;
        private int courseCount;
        private List<CategoryTreeRes> children;

        public static CategoryTreeRes of(Category entity, Map<Long, Long> courseCountMap) {
            return CategoryTreeRes.builder()
                    .idx(entity.getIdx())
                    .name(entity.getName())
                    .slug(entity.getSlug())
                    .courseCount(courseCountMap.getOrDefault(entity.getIdx(), 0L).intValue())
                    .children(entity.getChildren().stream()
                            .map(child -> CategoryTreeRes.of(child, courseCountMap))
                            .toList())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class CategoryRes {
        private Long idx;
        private String name;
        private String slug;

        public static CategoryRes of(Category entity) {
            return CategoryRes.builder()
                    .idx(entity.getIdx())
                    .name(entity.getName())
                    .slug(entity.getSlug())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class SectionRes {
        private Long idx;
        private String name;
        private List<LectureRes> lectures;

        private static List<LectureRes> mapLectures(Section entity, List<Long> lectureCompletes) {
            return entity.getLectures() != null
                    ? entity.getLectures().stream()
                    .map(lecture -> LectureRes.of(lecture, lectureCompletes))
                    .toList()
                    : Collections.emptyList();
        }


        public static SectionRes of(Section entity, List<Long> lectureCompletes) {
            return SectionRes.builder()
                    .idx(entity.getIdx())
                    .name(entity.getName())
                    .lectures(mapLectures(entity, lectureCompletes))
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
        private boolean isComplete;

        public static LectureRes of(Lecture entity, List<Long> lectureCompletes) {
            return LectureRes.builder()
                    .idx(entity.getIdx())
                    .name(entity.getName())
                    .free(entity.isFree())
                    .playTime(entity.getPlayTime())
                    .videoUrl(entity.isFree() ? entity.getVideoUrl() : null)
                    .isComplete(lectureCompletes.contains(entity.getIdx()))
                    .build();
        }

        public static LectureRes of(Lecture entity) {
            return LectureRes.builder()
                    .idx(entity.getIdx())
                    .name(entity.getName())
                    .playTime(entity.getPlayTime())
                    .videoUrl(entity.isFree() ? entity.getVideoUrl() : null)
                    .isComplete(false)
                    .build();
        }
    }
}
