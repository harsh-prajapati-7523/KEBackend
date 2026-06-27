package com.ke.ticketsystemke.dto;

import jakarta.validation.constraints.NotNull;

public class UpsertWorkflowTransitionRoleRuleRequest {

    @NotNull
    private Long roleId;

    private Boolean active;

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
