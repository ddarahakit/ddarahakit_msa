package com.ddarahakit.course.domain.course.controller;


import com.ddarahakit.course.common.model.BaseResponse;
import com.ddarahakit.course.domain.course.model.LectureDto;
import com.ddarahakit.course.domain.course.service.LectureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/lecture")
@Tag(name = "강의 컨트롤러")
public class LectureController {
    private final LectureService lectureService;
    @Operation(
            summary = "강의 생성",
            description = "새로운 강의를 추가한다.")
    @PostMapping("/create")
    public ResponseEntity<BaseResponse<LectureDto.LectureRes>> lectureCreate(
            @RequestBody LectureDto.LectureReq dto) {
        LectureDto.LectureRes response = lectureService.lectureCreate(dto);

        return ResponseEntity.ok(BaseResponse.success(response));
    }
}
