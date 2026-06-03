package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.TicketFieldDefinition;
import com.ke.ticketsystemke.entity.TicketFieldType;

import java.time.Instant;

public record TicketFieldDefinitionResponse(
        Long id,
        String fieldKey,
        String displayName,
        TicketFieldType fieldType,
        boolean active,
        boolean systemField,
        String helpText,
        boolean defaultRequired,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
    public static TicketFieldDefinitionResponse from(TicketFieldDefinition fieldDefinition) {
        return new TicketFieldDefinitionResponse(
                fieldDefinition.getId(),
                fieldDefinition.getFieldKey(),
                fieldDefinition.getDisplayName(),
                fieldDefinition.getFieldType(),
                fieldDefinition.isActive(),
                fieldDefinition.isSystemField(),
                fieldDefinition.getHelpText(),
                fieldDefinition.isDefaultRequired(),
                fieldDefinition.getSortOrder(),
                fieldDefinition.getCreatedAt(),
                fieldDefinition.getUpdatedAt()
        );
    }
}
