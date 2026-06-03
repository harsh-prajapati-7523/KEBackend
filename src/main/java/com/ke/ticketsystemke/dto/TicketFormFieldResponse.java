package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.CategoryFieldConfig;
import com.ke.ticketsystemke.entity.TicketFieldDefinition;
import com.ke.ticketsystemke.entity.TicketFieldType;

public record TicketFormFieldResponse(
        Long categoryFieldConfigId,
        Long fieldDefinitionId,
        String fieldKey,
        String displayName,
        TicketFieldType fieldType,
        boolean required,
        Integer sortOrder,
        String helpText
) {
    public static TicketFormFieldResponse from(CategoryFieldConfig config) {
        TicketFieldDefinition fieldDefinition = config.getFieldDefinition();
        return new TicketFormFieldResponse(
                config.getId(),
                fieldDefinition.getId(),
                fieldDefinition.getFieldKey(),
                fieldDefinition.getDisplayName(),
                fieldDefinition.getFieldType(),
                config.isRequired(),
                config.getSortOrder(),
                fieldDefinition.getHelpText()
        );
    }
}
