package com.ke.ticketsystemke.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class UpdateDynamicRoleAccessRequest {

    @NotNull
    @Valid
    private List<UpdateDynamicRoleAccessRuleRequest> rules;

    public List<UpdateDynamicRoleAccessRuleRequest> getRules() {
        return rules;
    }

    public void setRules(List<UpdateDynamicRoleAccessRuleRequest> rules) {
        this.rules = rules;
    }
}
