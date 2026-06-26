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
        courseRepository.findById(courseIdx).ifPresentOrElse(
                course -> course.applyRatingBucket(rating, delta),
                () -> log.warn("[rating] 알 수 없는 courseIdx={} 무시(평점 버킷 적용 skip)", courseIdx)
        );
    }

    /** 총 리뷰 수에 delta(+1/-1) 적용. */
    @Transactional
    public void applyCount(Long courseIdx, int delta) {
        courseRepository.findById(courseIdx).ifPresent(course -> course.applyReviewCount(delta));
    }
}
