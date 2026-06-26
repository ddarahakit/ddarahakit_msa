package com.ddarahakit.course.domain.course.service;


import com.ddarahakit.course.client.ReviewClient;
import com.ddarahakit.course.common.exception.BaseException;
import com.ddarahakit.course.config.security.AuthUserDetails;
import com.ddarahakit.course.domain.course.model.*;
import com.ddarahakit.course.domain.course.repository.CategoryRepository;
import com.ddarahakit.course.domain.course.repository.CourseRepository;
import com.ddarahakit.course.domain.course.repository.EnrollmentRepository;
import com.ddarahakit.course.domain.course.repository.LectureCompleteRepository;
import com.ddarahakit.course.domain.course.repository.LectureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.ddarahakit.course.common.model.BaseResponseStatus.*;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class CourseService {
    private final CourseRepository courseRepository;
    private final LectureRepository lectureRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LectureCompleteRepository lectureCompleteRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewClient reviewClient;

    public List<CourseDto.CategoryTreeRes> categoryList() {
        Map<Long, Long> courseCountMap = courseRepository.countByCategoryGrouped().stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        return categoryRepository.findByParentIsNull().stream()
                .map(category -> CourseDto.CategoryTreeRes.of(category, courseCountMap))
                .toList();
    }

    public CourseDto.CourseListRes list() {
        List<Course> result = courseRepository.findAll();
        Map<Long, Long> orderCountMap = orderCountMap();

        return CourseDto.CourseListRes.of(result.stream()
                .map(c -> CourseDto.CourseSummaryRes.of(c, orderedCount(orderCountMap, c.getIdx())))
                .toList());
    }

    public CourseDto.CourseListRes search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return CourseDto.CourseListRes.of(List.of());
        }
        List<Course> result = courseRepository.searchByKeyword(keyword.trim());
        Map<Long, Long> orderCountMap = orderCountMap();

        return CourseDto.CourseListRes.of(result.stream()
                .map(c -> CourseDto.CourseSummaryRes.of(c, orderedCount(orderCountMap, c.getIdx())))
                .toList());
    }

    /**
     * 정렬/필터/난이도 적용 코스 목록.
     * - sort: popular(수강권 수)/latest(idx)/rating(평점). 기본 latest.
     * - filter: free/new/subscribed(수강권 보유, 로그인 필요). 기본 없음.
     * - level: BEGINNER/INTERMEDIATE/ADVANCED. 기본 전체.
     */
    public CourseDto.CourseListRes list(String sort, String filter, CourseLevel level, AuthUserDetails authUserDetails) {
        boolean freeOnly = "free".equalsIgnoreCase(filter);
        List<Course> result = new ArrayList<>(courseRepository.findForList(level, freeOnly));
        Map<Long, Long> orderCountMap = orderCountMap();

        // filter=subscribed → 수강권 보유 코스만 (로그인 시에만 의미)
        if ("subscribed".equalsIgnoreCase(filter) && authUserDetails != null) {
            Set<Long> purchased = enrollmentRepository.findByUserIdx(authUserDetails.getIdx())
                    .stream().map(Enrollment::getCourseIdx).collect(Collectors.toSet());
            result = result.stream().filter(c -> purchased.contains(c.getIdx())).collect(Collectors.toList());
        }

        // 정렬
        String sortKey = sort == null ? "latest" : sort.toLowerCase();
        switch (sortKey) {
            case "popular" -> result.sort((a, b) -> Long.compare(
                    orderCountMap.getOrDefault(b.getIdx(), 0L),
                    orderCountMap.getOrDefault(a.getIdx(), 0L)));
            case "rating" -> result.sort((a, b) -> Integer.compare(
                    b.getTotalReviewsCount() == null ? 0 : b.getTotalReviewsCount(),
                    a.getTotalReviewsCount() == null ? 0 : a.getTotalReviewsCount()));
            default -> result.sort((a, b) -> Long.compare(b.getIdx(), a.getIdx())); // latest
        }

        // filter=new → 최신 상위 8개
        if ("new".equalsIgnoreCase(filter)) {
            result.sort((a, b) -> Long.compare(b.getIdx(), a.getIdx()));
            if (result.size() > 8) {
                result = new ArrayList<>(result.subList(0, 8));
            }
        }

        List<Course> finalResult = result;
        return CourseDto.CourseListRes.of(finalResult.stream()
                .map(c -> CourseDto.CourseSummaryRes.of(c, orderedCount(orderCountMap, c.getIdx())))
                .toList());
    }


    public CourseDto.CourseListRes list(String slug) {
        Category category = categoryRepository.findBySlug(slug).orElseThrow(
                () -> BaseException.of(CATEGORY_NOT_FOUND)
        );
        List<Long> categoryIdxList = categoryRepository.findSubCategoryIdxList(category.getMaterializedPath());
        List<Course> result = courseRepository.findCoursesBycategoryIdxList(categoryIdxList);
        Map<Long, Long> orderCountMap = orderCountMap();

        return CourseDto.CourseListRes.of(category, result.stream()
                .map(c -> CourseDto.CourseSummaryRes.of(c, orderedCount(orderCountMap, c.getIdx())))
                .toList());
    }


    public CourseDto.CourseRes readCourse(AuthUserDetails authUserDetails, Long courseIdx) {
        boolean isReviewed = false;
        boolean isOrdered = false;
        Long nextLectureIdx = 0L;
        List<Long> lectureCompleteList = List.of();

        Course course = courseRepository.findById(courseIdx).orElseThrow(
                () -> BaseException.of(COURSE_NOT_FOUND)
        );

        // 리뷰 본문은 review-service 가 소유 → Feign 으로 조회(실패 시 빈 페이지 폴백).
        ReviewDto.ReviewPageRes reviewPage = fetchReviews(courseIdx);

        if (authUserDetails != null) {
            boolean isOrderedByUser = enrollmentRepository
                    .existsByUserIdxAndCourseIdx(authUserDetails.getIdx(), courseIdx);

            if (isOrderedByUser) {
                isOrdered = true;
                nextLectureIdx = readNextLecture(authUserDetails, courseIdx).getIdx();

                lectureCompleteList = lectureCompleteRepository
                        .findByUserIdxAndCourseIdx(authUserDetails.getIdx(), courseIdx)
                        .stream()
                        .map(lc -> lc.getLecture().getIdx())
                        .toList();

                // isReviewed: 조회된 리뷰 작성자에 현재 사용자가 있는지로 도출(없으면 false 폴백).
                isReviewed = isAuthoredByCurrentUser(reviewPage, authUserDetails.getIdx());
            }
        }

        int totalOrderedCount = (int) enrollmentRepository.countByCourseIdx(courseIdx);

        return CourseDto.CourseRes.of(course, totalOrderedCount, reviewPage, isReviewed, isOrdered,
                nextLectureIdx, lectureCompleteList);
    }

    public LectureDto.LectureRes readLecture(AuthUserDetails authUserDetails, Long courseIdx, Long lectureIdx) {
        Course course = courseRepository.findById(courseIdx).orElseThrow(
                () -> BaseException.of(COURSE_NOT_FOUND)
        );

        Lecture lecture = lectureRepository.findById(lectureIdx).orElseThrow(
                () -> BaseException.of(LECTURE_NOT_FOUND)
        );

        if (!lecture.getSection().getCourse().getIdx().equals(course.getIdx())) {
            throw BaseException.of(LECTURE_NOT_IN_COURSE);
        }

        List<Long> lectureCompleteList = List.of();

        if (authUserDetails != null) {
            boolean hasPurchased = enrollmentRepository
                    .existsByUserIdxAndCourseIdx(authUserDetails.getIdx(), courseIdx);

            if (!hasPurchased) {
                throw BaseException.of(ORDERS_NOT_ORDERED);
            }

            lectureCompleteList = lectureCompleteRepository
                    .findByUserIdxAndCourseIdx(authUserDetails.getIdx(), courseIdx)
                    .stream()
                    .map(lc -> lc.getLecture().getIdx())
                    .toList();
        }

        return LectureDto.LectureRes.of(lecture, lectureCompleteList);
    }


    public LectureDto.LectureCompleteRes readNextLecture(AuthUserDetails authUserDetails, Long courseIdx) {
        Long lectureIdx = lectureCompleteRepository
                .findTopByUserIdxAndCourseIdxOrderByLectureIdxDesc(authUserDetails.getIdx(), courseIdx)
                .map(lectureComplete -> lectureComplete.getLecture().getIdx())
                .orElse(0L);

        List<Lecture> lectureList = lectureRepository.findAllByCourseIdxOrderByLectureIdxAsc(courseIdx);

        if (lectureList.isEmpty()) {
            return LectureDto.LectureCompleteRes.builder().idx(0L).build();
        }

        if (lectureIdx == 0L || lectureList.get(lectureList.size() - 1).getIdx().equals(lectureIdx)) {
            return LectureDto.LectureCompleteRes.of(lectureList.get(0));
        }

        return lectureList.stream()
                .filter(lecture -> lecture.getIdx() > lectureIdx)
                .findFirst()
                .map(LectureDto.LectureCompleteRes::of)
                .orElse(LectureDto.LectureCompleteRes.of(lectureList.get(0)));
    }

    @Transactional
    public LectureDto.LectureCompleteRes lectureComplete(AuthUserDetails authUserDetails, LectureDto.LectureCompleteReq dto) {
        Optional<LectureComplete> result = lectureCompleteRepository
                .findByUserIdxAndCourseIdxAndLectureIdx(authUserDetails.getIdx(), dto.getCourseIdx(), dto.getLectureIdx());

        if (result.isPresent()) {
            throw BaseException.of(ALREADY_LECTURE_COMPLETE);
        }

        LectureComplete lectureComplete = lectureCompleteRepository.save(dto.toEntity(authUserDetails.getIdx()));

        return LectureDto.LectureCompleteRes.of(lectureComplete.getLecture());
    }

    // === 내부 헬퍼 ===

    /** 코스별 수강권(주문) 수 집계 맵. */
    private Map<Long, Long> orderCountMap() {
        return enrollmentRepository.countGroupByCourse().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    private int orderedCount(Map<Long, Long> orderCountMap, Long courseIdx) {
        return orderCountMap.getOrDefault(courseIdx, 0L).intValue();
    }

    /** review-service 에서 코스 리뷰 페이지 조회. 실패 시 빈 페이지 폴백(상세 200 보장). */
    private ReviewDto.ReviewPageRes fetchReviews(Long courseIdx) {
        try {
            ReviewClient.ReviewResponse response = reviewClient.getReviews(courseIdx);
            if (response != null && response.results != null) {
                return response.results;
            }
        } catch (Exception ex) {
            log.warn("[review] courseIdx={} 리뷰 조회 실패 → 빈 페이지 폴백: {}", courseIdx, ex.getMessage());
        }
        return ReviewDto.ReviewPageRes.empty();
    }

    /**
     * 조회된 리뷰 페이지에 현재 사용자의 리뷰가 있는지(isReviewed) 도출.
     * 리뷰 본문에 userId 가 없으므로 정확 매칭은 불가 → review-service 가 noting 시 false 폴백.
     */
    private boolean isAuthoredByCurrentUser(ReviewDto.ReviewPageRes reviewPage, Long userIdx) {
        // ReviewRes 에 userId 가 노출되지 않으므로 false 폴백(상세 응답에는 영향 없음).
        return false;
    }
}
