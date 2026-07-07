package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.Role;
import com.ke.ticketsystemke.entity.AuthenticationMode;

import java.time.Instant;

public record RoleResponse(
        Long id,
        String roleKey,
        String displayName,
        boolean active,
        boolean systemRole,
        AuthenticationMode authenticationMode,
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
                role.getAuthenticationMode(),
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }
}
