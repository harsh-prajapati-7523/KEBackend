package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowTransition;
import com.ke.ticketsystemke.entity.WorkflowStatus;

import java.time.Instant;
import java.util.Locale;

public record WorkflowTransitionResponse(
        Long id,
        AccessKey actionKey,
        String displayName,
        TicketStatus fromStatus,
        Long fromStatusId,
        String fromStatusKey,
        String fromStatusDisplayName,
        Boolean fromStatusActive,
        Boolean fromStatusTerminal,
        TicketStatus toStatus,
        Long toStatusId,
        String toStatusKey,
        String toStatusDisplayName,
        Boolean toStatusActive,
        Boolean toStatusTerminal,
        boolean active,
        Integer sortOrder,
        boolean systemTransition,
        boolean protectedTransition,
        Instant createdAt,
        Instant updatedAt
) {

    public static WorkflowTransitionResponse from(WorkflowTransition transition) {
        WorkflowStatus fromStatusRecord = transition.getFromStatusRecord();
        WorkflowStatus toStatusRecord = transition.getToStatusRecord();
        return new WorkflowTransitionResponse(
                transition.getId(),
                transition.getActionKey(),
                transition.getDisplayName(),
                transition.getFromStatus(),
                fromStatusRecord != null ? fromStatusRecord.getId() : null,
                fromStatusRecord != null ? fromStatusRecord.getStatusKey() : fallbackStatusKey(transition.getFromStatus()),
                fromStatusRecord != null ? fromStatusRecord.getDisplayName() : fallbackStatusDisplayName(transition.getFromStatus()),
                fromStatusRecord != null ? fromStatusRecord.isActive() : Boolean.TRUE,
                fromStatusRecord != null ? fromStatusRecord.isTerminal() : fallbackStatusTerminal(transition.getFromStatus()),
                transition.getToStatus(),
                toStatusRecord != null ? toStatusRecord.getId() : null,
                toStatusRecord != null ? toStatusRecord.getStatusKey() : fallbackStatusKey(transition.getToStatus()),
                toStatusRecord != null ? toStatusRecord.getDisplayName() : fallbackStatusDisplayName(transition.getToStatus()),
                toStatusRecord != null ? toStatusRecord.isActive() : Boolean.TRUE,
                toStatusRecord != null ? toStatusRecord.isTerminal() : fallbackStatusTerminal(transition.getToStatus()),
                transition.isActive(),
                transition.getSortOrder(),
                transition.isSystemTransition(),
                transition.isProtectedTransition(),
                transition.getCreatedAt(),
                transition.getUpdatedAt()
        );
    }

    private static String fallbackStatusKey(TicketStatus status) {
        return status == null ? null : status.name();
    }

    private static String fallbackStatusDisplayName(TicketStatus status) {
        if (status == null) {
            return null;
        }

        String[] words = status.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder displayName = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (displayName.length() > 0) {
                displayName.append(' ');
            }
            displayName.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                displayName.append(word.substring(1));
            }
        }
        return displayName.toString();
    }

    private static Boolean fallbackStatusTerminal(TicketStatus status) {
        return status == TicketStatus.COMPLETED || status == TicketStatus.CANCELLED;
    }
}
