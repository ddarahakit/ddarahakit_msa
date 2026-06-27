package com.ddarahakit.course.domain.course.repository;


import com.ddarahakit.course.domain.course.model.Lecture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

    /** 해당 강의가 특정 코스에 속하는지(lecture → section → course) 검증용. */
    boolean existsByIdxAndSection_Course_Idx(Long idx, Long courseIdx);

    @Query("SELECT l FROM Lecture l " +
            "JOIN FETCH l.section s " +
            "JOIN FETCH s.course c " +
            "WHERE c.idx = :courseIdx " +
            "ORDER BY l.idx ASC")
    List<Lecture> findAllByCourseIdxOrderByLectureIdxAsc(Long courseIdx);

    @Query("SELECT l FROM Lecture l " +
            "JOIN FETCH l.section s " +
            "JOIN FETCH s.course c " +
            "WHERE c.idx IN :courseIdxList " +
            "ORDER BY c.idx ASC, l.idx ASC")
    List<Lecture> findAllByCourseIdxInOrderByLectureIdxAsc(@Param("courseIdxList") List<Long> courseIdxList);
}
