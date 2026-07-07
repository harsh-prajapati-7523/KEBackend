package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.AuthenticationMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class UpdateDynamicRoleAccessRequest {

    @NotNull
    @Valid
    private List<UpdateDynamicRoleAccessRuleRequest> rules;

    private AuthenticationMode authenticationMode;

    public List<UpdateDynamicRoleAccessRuleRequest> getRules() {
        return rules;
    }

    public void setRules(List<UpdateDynamicRoleAccessRuleRequest> rules) {
        this.rules = rules;
    }

    public AuthenticationMode getAuthenticationMode() {
        return authenticationMode;
    }

    public void setAuthenticationMode(AuthenticationMode authenticationMode) {
        this.authenticationMode = authenticationMode;
    }
}
