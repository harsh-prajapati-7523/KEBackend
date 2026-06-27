package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.Role;
import com.ke.ticketsystemke.entity.WorkflowTransitionRoleRule;

public record WorkflowTransitionRoleRuleResponse(
        Long id,
        Long transitionId,
        Long roleId,
        String roleKey,
        String roleDisplayName,
        boolean active
) {
    public static WorkflowTransitionRoleRuleResponse from(WorkflowTransitionRoleRule rule) {
        Role role = rule.getRole();
        return new WorkflowTransitionRoleRuleResponse(
                rule.getId(),
                rule.getWorkflowTransition().getId(),
                role.getId(),
                role.getRoleKey(),
                role.getDisplayName(),
                rule.isActive()
        );
    }
}
