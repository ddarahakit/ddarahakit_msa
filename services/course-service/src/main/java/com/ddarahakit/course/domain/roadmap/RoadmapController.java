package com.ddarahakit.course.domain.roadmap;

import com.ddarahakit.course.common.model.BaseResponse;
import com.ddarahakit.course.domain.roadmap.model.RoadmapDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/roadmap")
@Tag(name = "로드맵 컨트롤러")
public class RoadmapController {
    private final RoadmapService roadmapService;

    @Operation(summary = "로드맵 목록 조회", description = "전체 로드맵 목록을 조회한다.")
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<List<RoadmapDto.RoadmapListRes>>> list() {
        List<RoadmapDto.RoadmapListRes> response = roadmapService.list();
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(summary = "로드맵 상세 조회", description = "로드맵 IDX를 이용해서 로드맵의 상세 정보를 조회한다.")
    @GetMapping("/{roadmapIdx}")
    public ResponseEntity<BaseResponse<RoadmapDto.RoadmapDetailRes>> detail(
            @PathVariable Long roadmapIdx) {
        RoadmapDto.RoadmapDetailRes response = roadmapService.detail(roadmapIdx);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(summary = "로드맵 생성", description = "관리자가 새로운 로드맵을 생성한다.")
    @PostMapping
    public ResponseEntity<BaseResponse<RoadmapDto.RoadmapDetailRes>> create(
            @Valid @RequestBody RoadmapDto.CreateReq dto) {
        RoadmapDto.RoadmapDetailRes response = roadmapService.create(dto);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(summary = "로드맵 수정", description = "관리자가 로드맵을 수정한다. courses를 전달하면 기존 코스 목록을 교체한다.")
    @PutMapping("/{roadmapIdx}")
    public ResponseEntity<BaseResponse<RoadmapDto.RoadmapDetailRes>> update(
            @PathVariable Long roadmapIdx,
            @Valid @RequestBody RoadmapDto.UpdateReq dto) {
        RoadmapDto.RoadmapDetailRes response = roadmapService.update(roadmapIdx, dto);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @Operation(summary = "로드맵 삭제", description = "관리자가 로드맵을 삭제한다.")
    @DeleteMapping("/{roadmapIdx}")
    public ResponseEntity<BaseResponse<Void>> delete(
            @PathVariable Long roadmapIdx) {
        roadmapService.delete(roadmapIdx);
        return ResponseEntity.ok(BaseResponse.success(null));
    }
}
