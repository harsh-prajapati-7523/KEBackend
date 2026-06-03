package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.CategoryFieldConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryFieldConfigRepository extends JpaRepository<CategoryFieldConfig, Long> {

    boolean existsByCategoryIdAndFieldDefinitionId(Long categoryId, Long fieldDefinitionId);

    @Query("""
            SELECT config
            FROM CategoryFieldConfig config
            JOIN config.fieldDefinition fieldDefinition
            WHERE config.category.id = :categoryId
            ORDER BY config.sortOrder ASC, fieldDefinition.fieldKey ASC, config.id ASC
            """)
    List<CategoryFieldConfig> findAllByCategoryIdSorted(@Param("categoryId") Long categoryId);

    Optional<CategoryFieldConfig> findByIdAndCategoryId(Long id, Long categoryId);
}
