package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowTransition;

import java.time.Instant;

public record WorkflowTransitionResponse(
        Long id,
        AccessKey actionKey,
        String displayName,
        TicketStatus fromStatus,
        TicketStatus toStatus,
        boolean active,
        Integer sortOrder,
        boolean systemTransition,
        boolean protectedTransition,
        Instant createdAt,
        Instant updatedAt
) {

    public static WorkflowTransitionResponse from(WorkflowTransition transition) {
        return new WorkflowTransitionResponse(
                transition.getId(),
                transition.getActionKey(),
                transition.getDisplayName(),
                transition.getFromStatus(),
                transition.getToStatus(),
                transition.isActive(),
                transition.getSortOrder(),
                transition.isSystemTransition(),
                transition.isProtectedTransition(),
                transition.getCreatedAt(),
                transition.getUpdatedAt()
        );
    }
}
