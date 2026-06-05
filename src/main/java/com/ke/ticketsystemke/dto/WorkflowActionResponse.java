package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.WorkflowAction;

import java.time.Instant;

public record WorkflowActionResponse(
        Long id,
        String actionKey,
        String displayName,
        String buttonLabel,
        String description,
        boolean active,
        boolean systemAction,
        boolean protectedAction,
        Integer sortOrder,
        boolean requiresComment,
        boolean confirmationRequired,
        Instant createdAt,
        Instant updatedAt
) {

    public static WorkflowActionResponse from(WorkflowAction action) {
        return new WorkflowActionResponse(
                action.getId(),
                action.getActionKey(),
                action.getDisplayName(),
                action.getButtonLabel(),
                action.getDescription(),
                action.isActive(),
                action.isSystemAction(),
                action.isProtectedAction(),
                action.getSortOrder(),
                action.isRequiresComment(),
                action.isConfirmationRequired(),
                action.getCreatedAt(),
                action.getUpdatedAt()
        );
    }
}
