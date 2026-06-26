package com.ddarahakit.course.domain.roadmap;

import com.ddarahakit.course.common.exception.BaseException;
import com.ddarahakit.course.domain.course.model.Course;
import com.ddarahakit.course.domain.course.repository.CourseRepository;
import com.ddarahakit.course.domain.roadmap.model.Roadmap;
import com.ddarahakit.course.domain.roadmap.model.RoadmapCourse;
import com.ddarahakit.course.domain.roadmap.model.RoadmapDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.ddarahakit.course.common.model.BaseResponseStatus.*;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class RoadmapService {
    private final RoadmapRepository roadmapRepository;
    private final CourseRepository courseRepository;

    public List<RoadmapDto.RoadmapListRes> list() {
        return roadmapRepository.findAll().stream()
                .map(RoadmapDto.RoadmapListRes::of)
                .toList();
    }

    public RoadmapDto.RoadmapDetailRes detail(Long idx) {
        Roadmap roadmap = roadmapRepository.findByIdWithCourses(idx).orElseThrow(
                () -> BaseException.of(ROADMAP_NOT_FOUND)
        );
        return RoadmapDto.RoadmapDetailRes.of(roadmap);
    }

    @Transactional
    public RoadmapDto.RoadmapDetailRes create(RoadmapDto.CreateReq dto) {
        Roadmap roadmap = Roadmap.builder()
                .name(dto.getName())
                .image(dto.getImage())
                .description(dto.getDescription())
                .build();

        if (dto.getCourses() != null) {
            for (RoadmapDto.CourseItem item : dto.getCourses()) {
                Course course = courseRepository.findById(item.getCourseIdx()).orElseThrow(
                        () -> BaseException.of(COURSE_NOT_FOUND)
                );

                RoadmapCourse roadmapCourse = RoadmapCourse.builder()
                        .roadmap(roadmap)
                        .course(course)
                        .sortOrder(item.getSortOrder())
                        .build();

                roadmap.getRoadmapCourses().add(roadmapCourse);
            }
        }

        roadmapRepository.save(roadmap);
        return RoadmapDto.RoadmapDetailRes.of(roadmap);
    }

    @Transactional
    public RoadmapDto.RoadmapDetailRes update(Long roadmapIdx, RoadmapDto.UpdateReq dto) {
        Roadmap roadmap = roadmapRepository.findByIdWithCourses(roadmapIdx).orElseThrow(
                () -> BaseException.of(ROADMAP_NOT_FOUND)
        );

        roadmap.update(dto.getName(), dto.getImage(), dto.getDescription());

        if (dto.getCourses() != null) {
            roadmap.getRoadmapCourses().clear();
            for (RoadmapDto.CourseItem item : dto.getCourses()) {
                Course course = courseRepository.findById(item.getCourseIdx()).orElseThrow(
                        () -> BaseException.of(COURSE_NOT_FOUND)
                );

                RoadmapCourse roadmapCourse = RoadmapCourse.builder()
                        .roadmap(roadmap)
                        .course(course)
                        .sortOrder(item.getSortOrder())
                        .build();

                roadmap.getRoadmapCourses().add(roadmapCourse);
            }
        }

        roadmapRepository.save(roadmap);
        return RoadmapDto.RoadmapDetailRes.of(roadmap);
    }

    @Transactional
    public void delete(Long roadmapIdx) {
        Roadmap roadmap = roadmapRepository.findById(roadmapIdx).orElseThrow(
                () -> BaseException.of(ROADMAP_NOT_FOUND)
        );
        roadmapRepository.delete(roadmap);
    }
}
