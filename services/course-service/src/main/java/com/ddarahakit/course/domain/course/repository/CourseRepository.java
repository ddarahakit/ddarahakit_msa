package com.ddarahakit.course.domain.course.repository;


import com.ddarahakit.course.domain.course.model.Course;
import com.ddarahakit.course.domain.course.model.CourseLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    @Query("SELECT c FROM Course c WHERE c.category.idx IN :categoryIdxList ORDER BY c.idx")
    List<Course> findCoursesBycategoryIdxList(@Param("categoryIdxList") List<Long> categoryIdxList);

    @Query("SELECT c.category.idx, COUNT(c) FROM Course c GROUP BY c.category.idx")
    List<Object[]> countByCategoryGrouped();

    @Query("""
            SELECT c FROM Course c
            LEFT JOIN FETCH c.category
            WHERE (:level IS NULL OR c.level = :level)
              AND (:freeOnly = false OR c.salePrice = 0)
            """)
    List<Course> findForList(@Param("level") CourseLevel level,
                             @Param("freeOnly") boolean freeOnly);

    @Query("""
            SELECT c FROM Course c
            LEFT JOIN FETCH c.category
            WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.text) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY c.idx DESC
            """)
    List<Course> searchByKeyword(@Param("keyword") String keyword);
}
