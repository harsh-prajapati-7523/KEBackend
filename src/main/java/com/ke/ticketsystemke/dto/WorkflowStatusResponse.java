package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.WorkflowStatus;

import java.time.Instant;

public record WorkflowStatusResponse(
        Long id,
        String statusKey,
        String displayName,
        boolean active,
        boolean systemStatus,
        boolean protectedStatus,
        boolean terminal,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt
) {

    public static WorkflowStatusResponse from(WorkflowStatus status) {
        return new WorkflowStatusResponse(
                status.getId(),
                status.getStatusKey(),
                status.getDisplayName(),
                status.isActive(),
                status.isSystemStatus(),
                status.isProtectedStatus(),
                status.isTerminal(),
                status.getSortOrder(),
                status.getCreatedAt(),
                status.getUpdatedAt()
        );
    }
}
