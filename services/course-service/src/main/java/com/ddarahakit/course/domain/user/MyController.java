package com.ddarahakit.course.domain.user;

import com.ddarahakit.course.common.model.BaseResponse;
import com.ddarahakit.course.config.security.AuthUserDetails;
import com.ddarahakit.course.domain.course.model.CourseDto;
import com.ddarahakit.course.domain.course.service.CourseService;
import com.ddarahakit.course.domain.study.StudyDto;
import com.ddarahakit.course.domain.study.StudyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 마이페이지 집계(course-service 소유분).
 * 모놀리스 은퇴를 위해 내 강의실/주간 학습활동을 course-service 로 이전한다.
 * 인증은 헤더(X-User-Id) 기반 — 현재 사용자 idx 는 AuthUserDetails 에서 획득.
 */
@RequiredArgsConstructor
@RestController
@Tag(name = "마이페이지(코스) 컨트롤러")
public class MyController {

    private final CourseService courseService;
    private final StudyService studyService;

    @Operation(summary = "내 강의실(수강 코스 목록)", description = "현재 사용자의 수강 코스 목록을 진도/다음강의와 함께 조회한다.")
    @GetMapping("/user/ordered")
    public ResponseEntity<BaseResponse<List<CourseDto.CourseRes>>> orderedCourses(
            @AuthenticationPrincipal AuthUserDetails authUserDetails) {
        List<CourseDto.CourseRes> response = courseService.getOrderedCourseList(authUserDetails);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(summary = "주간 학습활동", description = "현재 사용자의 최근 7일 수강완료 기반 주간 학습 집계를 조회한다.")
    @GetMapping("/user/study/weekly")
    public ResponseEntity<BaseResponse<StudyDto.WeeklyRes>> weeklyStudy(
            @AuthenticationPrincipal AuthUserDetails authUserDetails) {
        StudyDto.WeeklyRes response = studyService.weekly(authUserDetails);
        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
