package com.ddarahakit.mentoring.domain.mentoring;

import com.ddarahakit.mentoring.domain.mentoring.model.MentoringSession;
import com.ddarahakit.mentoring.domain.mentoring.model.MentoringStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MentoringSessionRepository extends JpaRepository<MentoringSession, Long> {
    @Query(value = """
            select s from MentoringSession s
            where (s.mentorIdx = :userIdx or s.menteeIdx = :userIdx)
              and (:status is null or s.status = :status)
              and (:keyword is null or s.subject like %:keyword%)
            order by s.updatedAt desc
            """,
            countQuery = """
            select count(s) from MentoringSession s
            where (s.mentorIdx = :userIdx or s.menteeIdx = :userIdx)
              and (:status is null or s.status = :status)
              and (:keyword is null or s.subject like %:keyword%)
            """)
    Page<MentoringSession> findForUser(
            @Param("userIdx") Long userIdx,
            @Param("status") MentoringStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
