package com.ddarahakit.course.domain.roadmap;

import com.ddarahakit.course.domain.roadmap.model.Roadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoadmapRepository extends JpaRepository<Roadmap, Long> {
    @Query("SELECT r FROM Roadmap r LEFT JOIN FETCH r.roadmapCourses rc LEFT JOIN FETCH rc.course WHERE r.idx = :idx")
    Optional<Roadmap> findByIdWithCourses(@Param("idx") Long idx);
}
