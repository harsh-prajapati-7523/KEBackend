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

    @Query("""
            SELECT config
            FROM CategoryFieldConfig config
            JOIN config.fieldDefinition fieldDefinition
            WHERE config.category.id = :categoryId
              AND config.visible = true
              AND fieldDefinition.active = true
              AND (
                    fieldDefinition.fieldType IN (
                        com.ke.ticketsystemke.entity.TicketFieldType.TEXT,
                        com.ke.ticketsystemke.entity.TicketFieldType.NUMBER,
                        com.ke.ticketsystemke.entity.TicketFieldType.TEXTAREA
                    )
                    OR (
                        fieldDefinition.fieldType = com.ke.ticketsystemke.entity.TicketFieldType.DROPDOWN
                        AND fieldDefinition.dropdownSource IS NOT NULL
                        AND fieldDefinition.dropdownSource.active = true
                        AND EXISTS (
                            SELECT dropdownOption
                            FROM DropdownOption dropdownOption
                            WHERE dropdownOption.source = fieldDefinition.dropdownSource
                              AND dropdownOption.active = true
                        )
                    )
              )
            ORDER BY config.sortOrder ASC, fieldDefinition.fieldKey ASC, config.id ASC
            """)
    List<CategoryFieldConfig> findRenderableFormFieldsByCategoryId(@Param("categoryId") Long categoryId);

    Optional<CategoryFieldConfig> findByIdAndCategoryId(Long id, Long categoryId);
}
