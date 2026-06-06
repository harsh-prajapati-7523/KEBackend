package com.ke.ticketsystemke.dto;

public record DynamicRoleAccessRuleResponse(
        String accessKey,
        String displayName,
        String description,
        String category,
        boolean active,
        boolean allowed,
        boolean systemKey,
        boolean protectedKey,
        Integer sortOrder
) {
}
