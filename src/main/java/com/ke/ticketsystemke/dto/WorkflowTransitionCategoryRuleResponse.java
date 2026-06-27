package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.TicketCategoryConfig;
import com.ke.ticketsystemke.entity.WorkflowTransitionCategoryRule;

public record WorkflowTransitionCategoryRuleResponse(
        Long id,
        Long transitionId,
        Long categoryId,
        String categoryKey,
        String categoryDisplayName,
        boolean active
) {
    public static WorkflowTransitionCategoryRuleResponse from(WorkflowTransitionCategoryRule rule) {
        TicketCategoryConfig category = rule.getCategory();
        return new WorkflowTransitionCategoryRuleResponse(
                rule.getId(),
                rule.getWorkflowTransition().getId(),
                category.getId(),
                category.getCategoryKey(),
                category.getDisplayName(),
                rule.isActive()
        );
    }
}
