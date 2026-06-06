package com.ke.ticketsystemke.dto;

import java.util.List;

public record DynamicRoleAccessResponse(
        Long roleId,
        String roleKey,
        String roleDisplayName,
        boolean protectedRole,
        List<DynamicRoleAccessRuleResponse> rules
) {
}
