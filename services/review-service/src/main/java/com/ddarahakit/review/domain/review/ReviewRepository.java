package com.ddarahakit.review.domain.review;

import com.ddarahakit.review.domain.review.model.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByUserIdxAndCourseIdx(Long userIdx, Long courseIdx);

    Slice<Review> findByCourseIdxOrderByCreatedAtDesc(Long courseIdx, Pageable pageable);

    Slice<Review> findByCourseIdx(Long courseIdx, Pageable pageable);

    Slice<Review> findByUserIdxNotAndCourseIdxOrderByCreatedAtDesc(Long userIdx, Long courseIdx, Pageable pageable);

    List<Review> findByUserIdx(Long userIdx);

    /** 통계용: 전체 리뷰 수. */
    @Query("SELECT COUNT(r) FROM Review r")
    long countAllReviews();

    /** 통계용: 만족(4점 이상) 리뷰 수. */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.rating >= 4")
    long countSatisfiedReviews();
}
