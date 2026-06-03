package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.TicketFieldDefinition;
import com.ke.ticketsystemke.entity.TicketFieldType;
import com.ke.ticketsystemke.entity.DropdownSource;

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
        Long dropdownSourceId,
        String dropdownSourceKey,
        String dropdownSourceDisplayName,
        boolean dropdownSourceActive,
        Instant createdAt,
        Instant updatedAt
) {
    public static TicketFieldDefinitionResponse from(TicketFieldDefinition fieldDefinition) {
        DropdownSource dropdownSource = fieldDefinition.getDropdownSource();
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
                dropdownSource == null ? null : dropdownSource.getId(),
                dropdownSource == null ? null : dropdownSource.getSourceKey(),
                dropdownSource == null ? null : dropdownSource.getDisplayName(),
                dropdownSource != null && dropdownSource.isActive(),
                fieldDefinition.getCreatedAt(),
                fieldDefinition.getUpdatedAt()
        );
    }
}
