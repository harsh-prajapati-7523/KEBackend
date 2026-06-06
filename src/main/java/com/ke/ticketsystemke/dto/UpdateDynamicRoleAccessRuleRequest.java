package com.ke.ticketsystemke.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateDynamicRoleAccessRuleRequest {

    private String accessKey;

    @NotNull
    private Boolean allowed;

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public Boolean getAllowed() {
        return allowed;
    }

    public void setAllowed(Boolean allowed) {
        this.allowed = allowed;
    }
}
