package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.DropdownOption;

import java.time.Instant;

public record DropdownOptionResponse(
        Long id,
        Long sourceId,
        String optionKey,
        String displayValue,
        boolean active,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
    public static DropdownOptionResponse from(DropdownOption option) {
        return new DropdownOptionResponse(
                option.getId(),
                option.getSource().getId(),
                option.getOptionKey(),
                option.getDisplayValue(),
                option.isActive(),
                option.getSortOrder(),
                option.getCreatedAt(),
                option.getUpdatedAt()
        );
    }
}
