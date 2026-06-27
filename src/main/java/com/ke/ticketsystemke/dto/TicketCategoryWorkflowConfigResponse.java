package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.TicketCategoryConfig;
import com.ke.ticketsystemke.entity.WorkflowMode;

import java.time.Instant;

public record TicketCategoryWorkflowConfigResponse(
        Long categoryId,
        String categoryKey,
        String categoryDisplayName,
        WorkflowMode workflowMode,
        boolean dbWorkflowEnabled,
        boolean fixedActionsEnabled,
        Instant workflowModeUpdatedAt,
        String workflowModeUpdatedByEmployeeId
) {
    public static TicketCategoryWorkflowConfigResponse from(TicketCategoryConfig category) {
        return new TicketCategoryWorkflowConfigResponse(
                category.getId(),
                category.getCategoryKey(),
                category.getDisplayName(),
                category.getWorkflowMode(),
                category.isDbWorkflowEnabled(),
                category.isFixedActionsEnabled(),
                category.getWorkflowModeUpdatedAt(),
                category.getWorkflowModeUpdatedByEmployee() == null
                        ? null
                        : category.getWorkflowModeUpdatedByEmployee().getEmployeeId()
        );
    }
}
