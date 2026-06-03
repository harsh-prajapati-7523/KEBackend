package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.CategoryFieldConfig;
import com.ke.ticketsystemke.entity.TicketFieldDefinition;
import com.ke.ticketsystemke.entity.TicketFieldType;

import java.util.Collections;
import java.util.List;

public record TicketFormFieldResponse(
        Long categoryFieldConfigId,
        Long fieldDefinitionId,
        String fieldKey,
        String displayName,
        TicketFieldType fieldType,
        boolean required,
        Integer sortOrder,
        String helpText,
        List<TicketFormFieldOptionResponse> options
) {
    public static TicketFormFieldResponse from(CategoryFieldConfig config) {
        return from(config, Collections.emptyList());
    }

    public static TicketFormFieldResponse from(
            CategoryFieldConfig config,
            List<TicketFormFieldOptionResponse> options
    ) {
        TicketFieldDefinition fieldDefinition = config.getFieldDefinition();
        return new TicketFormFieldResponse(
                config.getId(),
                fieldDefinition.getId(),
                fieldDefinition.getFieldKey(),
                fieldDefinition.getDisplayName(),
                fieldDefinition.getFieldType(),
                config.isRequired(),
                config.getSortOrder(),
                fieldDefinition.getHelpText(),
                options == null ? Collections.emptyList() : options
        );
    }
}
