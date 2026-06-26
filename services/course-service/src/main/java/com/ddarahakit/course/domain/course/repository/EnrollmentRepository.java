package com.ddarahakit.course.domain.course.repository;

import com.ddarahakit.course.domain.course.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByUserIdxAndCourseIdx(Long userIdx, Long courseIdx);

    Optional<Enrollment> findByUserIdxAndCourseIdx(Long userIdx, Long courseIdx);

    List<Enrollment> findByUserIdx(Long userIdx);

    /** 코스별 수강권 수(인기순 정렬용): [courseIdx, count]. */
    @Query("SELECT e.courseIdx, COUNT(e) FROM Enrollment e GROUP BY e.courseIdx")
    List<Object[]> countGroupByCourse();

    /** 특정 코스의 수강권 수(구매자 수). */
    long countByCourseIdx(Long courseIdx);

    /** 고유 수강생 수(distinct userIdx). */
    @Query("SELECT COUNT(DISTINCT e.userIdx) FROM Enrollment e")
    long countDistinctStudents();

    void deleteByUserIdxAndCourseIdx(Long userIdx, Long courseIdx);
}
