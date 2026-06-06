package com.ke.ticketsystemke.dto;

public record DynamicRoleAccessRuleResponse(
        String accessKey,
        String displayName,
        String category,
        boolean active,
        boolean allowed,
        boolean systemKey,
        boolean protectedKey
) {
}
