package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.AuthenticationMode;

import java.util.List;

public record DynamicRoleAccessResponse(
        Long roleId,
        String roleKey,
        String roleDisplayName,
        boolean protectedRole,
        AuthenticationMode authenticationMode,
        List<DynamicRoleAccessRuleResponse> rules
) {
}
