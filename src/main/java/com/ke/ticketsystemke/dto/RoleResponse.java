package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.Role;

import java.time.Instant;

public record RoleResponse(
        Long id,
        String roleKey,
        String displayName,
        boolean active,
        boolean systemRole,
        Instant createdAt,
        Instant updatedAt
) {
    public static RoleResponse from(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getRoleKey(),
                role.getDisplayName(),
                role.isActive(),
                role.isSystemRole(),
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }
}
