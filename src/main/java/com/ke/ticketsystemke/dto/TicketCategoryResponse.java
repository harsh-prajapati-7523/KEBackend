package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.TicketCategoryConfig;

import java.time.Instant;

public record TicketCategoryResponse(
        Long id,
        String categoryKey,
        String displayName,
        boolean active,
        boolean systemCategory,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
    public static TicketCategoryResponse from(TicketCategoryConfig category) {
        return new TicketCategoryResponse(
                category.getId(),
                category.getCategoryKey(),
                category.getDisplayName(),
                category.isActive(),
                category.isSystemCategory(),
                category.getSortOrder(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
