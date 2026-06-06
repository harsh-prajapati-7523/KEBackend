package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.AccessKeyMetadata;

import java.time.Instant;

public record AccessKeyMetadataResponse(
        Long id,
        String accessKey,
        String displayName,
        String description,
        String category,
        boolean active,
        boolean systemKey,
        boolean protectedKey,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt
) {

    public static AccessKeyMetadataResponse from(AccessKeyMetadata metadata) {
        return new AccessKeyMetadataResponse(
                metadata.getId(),
                metadata.getAccessKey(),
                metadata.getDisplayName(),
                metadata.getDescription(),
                metadata.getCategory(),
                metadata.isActive(),
                metadata.isSystemKey(),
                metadata.isProtectedKey(),
                metadata.getSortOrder(),
                metadata.getCreatedAt(),
                metadata.getUpdatedAt()
        );
    }
}
