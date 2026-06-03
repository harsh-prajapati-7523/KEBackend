package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.AccessKey;

import java.util.Map;

public record CurrentAccessResponse(
        Long roleId,
        String roleKey,
        String roleDisplayName,
        boolean superAdmin,
        Map<AccessKey, Boolean> access
) {
}
