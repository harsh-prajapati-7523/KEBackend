package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.TicketWorkflowHistory;
import com.ke.ticketsystemke.entity.WorkflowAction;
import com.ke.ticketsystemke.entity.WorkflowStatus;
import com.ke.ticketsystemke.entity.WorkflowTransition;

import java.time.Instant;
import java.util.Locale;

public record TicketWorkflowHistoryResponse(
        Long id,
        Long ticketId,
        String ticketNumber,
        String actionKey,
        String actionDisplayName,
        String fromStatus,
        String toStatus,
        String fromStatusDisplayName,
        String toStatusDisplayName,
        String fromStatusKey,
        String toStatusKey,
        String fromBehaviorBucket,
        String toBehaviorBucket,
        String executedByEmployeeId,
        String executedByEmployeeNameSnapshot,
        String previousOwnerEmployeeId,
        String newOwnerEmployeeId,
        String comment,
        String reason,
        String result,
        boolean systemTransition,
        boolean customTransition,
        Instant createdAt
) {

    public static TicketWorkflowHistoryResponse from(TicketWorkflowHistory history) {
        WorkflowStatus fromStatusRecord = resolveEffectiveFromStatusRecord(history);
        WorkflowStatus toStatusRecord = resolveEffectiveToStatusRecord(history);

        return new TicketWorkflowHistoryResponse(
                history.getId(),
                history.getTicketId(),
                history.getTicketNumber(),
                history.getActionKey(),
                resolveActionDisplayName(history),
                history.getFromStatus(),
                history.getToStatus(),
                resolveStatusDisplayName(fromStatusRecord, history.getFromStatus()),
                resolveStatusDisplayName(toStatusRecord, history.getToStatus()),
                resolveStatusKey(fromStatusRecord, history.getFromStatus()),
                resolveStatusKey(toStatusRecord, history.getToStatus()),
                resolveBehaviorBucket(fromStatusRecord, history.getFromStatus()),
                resolveBehaviorBucket(toStatusRecord, history.getToStatus()),
                history.getExecutedByEmployeeId(),
                history.getExecutedByEmployeeNameSnapshot(),
                history.getPreviousOwnerEmployeeId(),
                history.getNewOwnerEmployeeId(),
                history.getComment(),
                history.getReason(),
                history.getResult(),
                history.isSystemTransition(),
                history.isCustomTransition(),
                history.getCreatedAt()
        );
    }

    private static WorkflowStatus resolveEffectiveFromStatusRecord(TicketWorkflowHistory history) {
        if (history.getFromStatusRecord() != null) {
            return history.getFromStatusRecord();
        }

        WorkflowTransition transition = history.getWorkflowTransition();
        return transition == null ? null : transition.getFromStatusRecord();
    }

    private static WorkflowStatus resolveEffectiveToStatusRecord(TicketWorkflowHistory history) {
        if (history.getToStatusRecord() != null) {
            return history.getToStatusRecord();
        }

        WorkflowTransition transition = history.getWorkflowTransition();
        return transition == null ? null : transition.getToStatusRecord();
    }

    private static String resolveActionDisplayName(TicketWorkflowHistory history) {
        WorkflowAction action = history.getWorkflowAction();
        if (action != null && isPresent(action.getDisplayName())) {
            return action.getDisplayName();
        }
        return formatLabel(history.getActionKey());
    }

    private static String resolveStatusDisplayName(WorkflowStatus status, String fallbackStatusKey) {
        if (status != null && isPresent(status.getDisplayName())) {
            return status.getDisplayName();
        }
        return formatLabel(fallbackStatusKey);
    }

    private static String resolveStatusKey(WorkflowStatus status, String fallbackStatusKey) {
        if (status != null && isPresent(status.getStatusKey())) {
            return status.getStatusKey();
        }
        return fallbackStatusKey;
    }

    private static String resolveBehaviorBucket(WorkflowStatus status, String fallbackStatus) {
        if (status != null && status.getBehaviorBucket() != null) {
            return status.getBehaviorBucket().name();
        }
        return fallbackStatus;
    }

    private static String formatLabel(String value) {
        if (!isPresent(value)) {
            return null;
        }

        String[] words = value.toLowerCase(Locale.ROOT).split("_");
        StringBuilder label = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!label.isEmpty()) {
                label.append(' ');
            }
            label.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                label.append(word.substring(1));
            }
        }
        return label.toString();
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
