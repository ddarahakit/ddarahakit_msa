package com.ddarahakit.course.domain.course.controller;


import com.ddarahakit.course.common.model.BaseResponse;
import com.ddarahakit.course.config.security.AuthUserDetails;
import com.ddarahakit.course.domain.course.model.CourseDto;
import com.ddarahakit.course.domain.course.model.CourseLevel;
import com.ddarahakit.course.domain.course.model.LectureDto;
import com.ddarahakit.course.domain.course.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/course")
@Tag(name = "코스 컨트롤러")
public class CourseController {
    private final CourseService courseService;

    @Operation(summary = "카테고리 목록 조회", description = "전체 카테고리 목록을 트리 구조로 조회한다.")
    @GetMapping("/category")
    public ResponseEntity<BaseResponse<List<CourseDto.CategoryTreeRes>>> categoryList() {
        List<CourseDto.CategoryTreeRes> response = courseService.categoryList();
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(
            summary = "코스 목록 조회",
            description = "코스 목록을 정렬(sort=popular|latest|rating)·필터(filter=free|new|subscribed)·난이도(level=BEGINNER|INTERMEDIATE|ADVANCED)로 조회한다. 파라미터가 모두 없으면 전체 목록.")
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<CourseDto.CourseListRes>> list(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) CourseLevel level
    ) {
        CourseDto.CourseListRes response =
                (sort == null && filter == null && level == null)
                        ? courseService.list()
                        : courseService.list(sort, filter, level, authUserDetails);

        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(
            summary = "카테고리별 코스 목록 조회",
            description = "카테고리별 코스 목록을 조회한다.")
    @GetMapping("/list/{slug}")
    public ResponseEntity<BaseResponse<CourseDto.CourseListRes>> list(
            @PathVariable String slug
    ) {
        CourseDto.CourseListRes response = courseService.list(slug);

        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(
            summary = "코스 검색",
            description = "키워드(강의명/소개/설명)로 코스를 검색한다. 키워드가 없으면 빈 목록.")
    @GetMapping("/search")
    public ResponseEntity<BaseResponse<CourseDto.CourseListRes>> search(
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        CourseDto.CourseListRes response = courseService.search(keyword);

        return ResponseEntity.ok(BaseResponse.success(response));
    }


    @Operation(
            summary = "코스 상세 조회",
            description = "코스 IDX를 이용해서 코스의 상세 정보를 조회한다.")
    @GetMapping("/{courseIdx}")
    public ResponseEntity<BaseResponse<CourseDto.CourseRes>> read(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @PathVariable Long courseIdx) {
        CourseDto.CourseRes response = courseService.readCourse(authUserDetails, courseIdx);

        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(
            summary = "강의 상세 조회",
            description = "강의 IDX를 이용해서 강의의 상세 정보를 조회한다.")
    @GetMapping("/lecture/{courseIdx}/{lectureIdx}")
    public ResponseEntity<BaseResponse<LectureDto.LectureRes>> readLecture(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @PathVariable Long courseIdx,
            @PathVariable Long lectureIdx) {
        LectureDto.LectureRes response = courseService.readLecture(authUserDetails, courseIdx, lectureIdx);

        return ResponseEntity.ok(BaseResponse.success(response));
    }


    @Operation(
            summary = "강의 수강 완료",
            description = "코스 IDX와 강의 IDX를 이용해서 강의 수강 완료 정보를 저장한다.")
    @PostMapping("/lecture/complete")
    public ResponseEntity<BaseResponse<LectureDto.LectureCompleteRes>> lectureComplete(
            @AuthenticationPrincipal AuthUserDetails authUserDetails,
            @RequestBody LectureDto.LectureCompleteReq dto) {
        LectureDto.LectureCompleteRes response = courseService.lectureComplete(authUserDetails, dto);

        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
