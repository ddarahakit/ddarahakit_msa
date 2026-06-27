package com.ddarahakit.course.domain.course.controller;

import com.ddarahakit.course.domain.course.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서비스 간 내부 호출 전용(게이트웨이 라우트에 미노출). 수강권(enrollment) 보유 여부 확인.
 * review-service 가 리뷰 작성 전 구매 검증에 사용한다.
 */
@RestController
@RequestMapping("/internal/enrollments")
@RequiredArgsConstructor
public class EnrollmentInternalController {

    private final EnrollmentRepository enrollmentRepository;

    /** userIdx 가 courseIdx 를 수강(구매)했는지 여부. */
    @GetMapping("/exists")
    public boolean exists(@RequestParam Long userIdx, @RequestParam Long courseIdx) {
        return enrollmentRepository.existsByUserIdxAndCourseIdx(userIdx, courseIdx);
    }
}
