package com.ddarahakit.mentoring.domain.mentoring;

import com.ddarahakit.mentoring.domain.mentoring.model.MentoringMessage;
import com.ddarahakit.mentoring.domain.mentoring.model.MentoringSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MentoringMessageRepository extends JpaRepository<MentoringMessage, Long> {
    Page<MentoringMessage> findBySessionOrderByIdxDesc(MentoringSession session, Pageable pageable);

    Page<MentoringMessage> findBySessionAndIdxLessThanOrderByIdxDesc(MentoringSession session, Long beforeIdx, Pageable pageable);
}
