package com.ke.ticketsystemke.dto;

import java.util.List;

public record DynamicRoleAccessResponse(
        Long roleId,
        String roleKey,
        String roleDisplayName,
        List<DynamicRoleAccessRuleResponse> rules
) {
}
