package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.AccessKey;
import jakarta.validation.constraints.NotNull;

public class UpdateRoleAccessRuleRequest {

    @NotNull
    private AccessKey accessKey;

    @NotNull
    private Boolean allowed;

    public AccessKey getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(AccessKey accessKey) {
        this.accessKey = accessKey;
    }

    public Boolean getAllowed() {
        return allowed;
    }

    public void setAllowed(Boolean allowed) {
        this.allowed = allowed;
    }
}
