package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.DropdownSource;
import com.ke.ticketsystemke.entity.DropdownSourceType;

import java.time.Instant;

public record DropdownSourceResponse(
        Long id,
        String sourceKey,
        String displayName,
        DropdownSourceType sourceType,
        boolean active,
        boolean systemSource,
        Instant createdAt,
        Instant updatedAt
) {
    public static DropdownSourceResponse from(DropdownSource source) {
        return new DropdownSourceResponse(
                source.getId(),
                source.getSourceKey(),
                source.getDisplayName(),
                source.getSourceType(),
                source.isActive(),
                source.isSystemSource(),
                source.getCreatedAt(),
                source.getUpdatedAt()
        );
    }
}
