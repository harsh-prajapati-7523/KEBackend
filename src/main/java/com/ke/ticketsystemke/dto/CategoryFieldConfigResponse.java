package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.CategoryFieldConfig;
import com.ke.ticketsystemke.entity.TicketCategoryConfig;
import com.ke.ticketsystemke.entity.TicketFieldDefinition;
import com.ke.ticketsystemke.entity.TicketFieldType;

import java.time.Instant;

public record CategoryFieldConfigResponse(
        Long id,
        Long categoryId,
        String categoryKey,
        String categoryDisplayName,
        Long fieldDefinitionId,
        String fieldKey,
        String fieldDisplayName,
        TicketFieldType fieldType,
        boolean fieldActive,
        boolean required,
        boolean visible,
        Integer sortOrder,
        String helpText,
        boolean defaultRequired,
        Instant createdAt,
        Instant updatedAt
) {
    public static CategoryFieldConfigResponse from(CategoryFieldConfig config) {
        TicketCategoryConfig category = config.getCategory();
        TicketFieldDefinition fieldDefinition = config.getFieldDefinition();
        return new CategoryFieldConfigResponse(
                config.getId(),
                category.getId(),
                category.getCategoryKey(),
                category.getDisplayName(),
                fieldDefinition.getId(),
                fieldDefinition.getFieldKey(),
                fieldDefinition.getDisplayName(),
                fieldDefinition.getFieldType(),
                fieldDefinition.isActive(),
                config.isRequired(),
                config.isVisible(),
                config.getSortOrder(),
                fieldDefinition.getHelpText(),
                fieldDefinition.isDefaultRequired(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }
}
