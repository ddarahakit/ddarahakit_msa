package com.ddarahakit.course.domain.course.repository;


import com.ddarahakit.course.domain.course.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {


    @Query("SELECT c.idx FROM Category c WHERE c.materializedPath LIKE CONCAT(:path, '%')")
    List<Long> findSubCategoryIdxList(@Param("path") String path);

    Optional<Category> findBySlug(String slug);

    List<Category> findByParentIsNull();
}
