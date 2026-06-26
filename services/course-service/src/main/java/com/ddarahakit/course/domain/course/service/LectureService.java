package com.ddarahakit.course.domain.course.service;

import com.ddarahakit.course.domain.course.model.Lecture;
import com.ddarahakit.course.domain.course.model.LectureDto;
import com.ddarahakit.course.domain.course.repository.LectureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class LectureService {
    private final LectureRepository lectureRepository;


    @Transactional
    public LectureDto.LectureRes lectureCreate(LectureDto.LectureReq dto) {
        Lecture lecture = lectureRepository.save(dto.toEntity());

        return LectureDto.LectureRes.of(lecture);
    }
}
