package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.AccessKey;

public record RoleAccessRuleResponse(
        AccessKey accessKey,
        boolean allowed
) {
}
