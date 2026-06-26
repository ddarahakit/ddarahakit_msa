package com.ddarahakit.course.domain.course.service;

import com.ddarahakit.course.domain.course.model.Enrollment;
import com.ddarahakit.course.domain.course.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 수강권 읽기모델 투영(commerce OrderPaid/OrderRefunded 소비).
 */
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;

    /** 수강권 부여. 멱등 — 이미 있으면 skip(unique(userIdx, courseIdx)). */
    @Transactional
    public void grant(Long userIdx, Long courseIdx, Long orderId) {
        if (enrollmentRepository.existsByUserIdxAndCourseIdx(userIdx, courseIdx)) {
            return;
        }
        enrollmentRepository.save(Enrollment.builder()
                .userIdx(userIdx)
                .courseIdx(courseIdx)
                .orderId(orderId)
                .grantedAt(Instant.now())
                .build());
    }

    /** 수강권 회수(환불). 없으면 no-op. */
    @Transactional
    public void revoke(Long userIdx, Long courseIdx) {
        enrollmentRepository.deleteByUserIdxAndCourseIdx(userIdx, courseIdx);
    }
}
