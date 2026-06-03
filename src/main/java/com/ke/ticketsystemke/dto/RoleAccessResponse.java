package com.ke.ticketsystemke.dto;

import java.util.List;

public record RoleAccessResponse(
        Long roleId,
        String roleKey,
        String roleDisplayName,
        boolean protectedRole,
        List<RoleAccessRuleResponse> rules
) {
}
