package com.ke.ticketsystemke.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class UpdateRoleAccessRequest {

    @NotNull
    @Valid
    private List<UpdateRoleAccessRuleRequest> rules;

    public List<UpdateRoleAccessRuleRequest> getRules() {
        return rules;
    }

    public void setRules(List<UpdateRoleAccessRuleRequest> rules) {
        this.rules = rules;
    }
}
