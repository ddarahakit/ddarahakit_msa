package com.ddarahakit.course.domain.course.repository;

import com.ddarahakit.course.domain.course.model.LectureComplete;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LectureCompleteRepository extends JpaRepository<LectureComplete, Long> {
    Optional<LectureComplete> findTopByUserIdxAndCourseIdxOrderByLectureIdxDesc(Long userIdx, Long courseIdx);

    Optional<LectureComplete> findByUserIdxAndCourseIdxAndLectureIdx(Long userIdx, Long courseIdx, Long lectureIdx);

    List<LectureComplete> findByUserIdxAndCourseIdx(Long userIdx, Long courseIdx);

    /**
     * 내 강의실(여러 코스 진도 일괄 조회): 사용자의 수강완료를 course/lecture 와 함께 한 번에 로딩.
     * course/lecture 를 fetch join 해 그룹핑 시 N+1 을 제거한다.
     */
    @Query("""
            SELECT lc FROM LectureComplete lc
            JOIN FETCH lc.course c
            JOIN FETCH lc.lecture l
            WHERE lc.userIdx = :userIdx AND c.idx IN :courseIdxList
            """)
    List<LectureComplete> findByUserIdxAndCourseIdxIn(@Param("userIdx") Long userIdx,
                                                      @Param("courseIdxList") List<Long> courseIdxList);

    /** 사용자의 총 수강완료 강의 수. */
    long countByUserIdx(Long userIdx);

    /**
     * 주간 학습활동: 지정 시각(from) 이후 완료된 수강완료의 '완료 시각' 목록(최신순). completedAt NULL 은 제외.
     */
    @Query("""
            SELECT lc.completedAt FROM LectureComplete lc
            WHERE lc.userIdx = :userIdx AND lc.completedAt IS NOT NULL AND lc.completedAt >= :from
            ORDER BY lc.completedAt DESC
            """)
    List<LocalDateTime> findCompletedAtByUserSince(@Param("userIdx") Long userIdx,
                                                   @Param("from") LocalDateTime from);
}
