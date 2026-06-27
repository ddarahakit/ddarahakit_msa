package com.ddarahakit.course.domain.course.service;

import com.ddarahakit.course.domain.course.model.Course;
import com.ddarahakit.course.domain.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 코스 평점 읽기모델 투영(review ReviewCreated/Updated/Deleted 소비).
 * course 가 자기 테이블(rating1..5, totalReviewsCount)을 직접 갱신한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RatingService {

    private final CourseRepository courseRepository;

    /** 평점 버킷(rating1..5)에 delta(+1/-1) 적용. */
    @Transactional
    public void apply(Long courseIdx, int rating, int delta) {
        // 코스가 없으면 예외로 트랜잭션을 롤백 → processed_event 미커밋 → 이벤트 재전달(재시도).
        // (조용히 skip 하면 평점 projection 이 영구 유실되어 drift 발생)
        Course course = courseRepository.findById(courseIdx)
                .orElseThrow(() -> new IllegalStateException(
                        "[rating] 알 수 없는 courseIdx=" + courseIdx + " — 재처리 위해 롤백"));
        course.applyRatingBucket(rating, delta);
    }

    /** 총 리뷰 수에 delta(+1/-1) 적용. */
    @Transactional
    public void applyCount(Long courseIdx, int delta) {
        Course course = courseRepository.findById(courseIdx)
                .orElseThrow(() -> new IllegalStateException(
                        "[rating] 알 수 없는 courseIdx=" + courseIdx + " — 재처리 위해 롤백"));
        course.applyReviewCount(delta);
    }
}
