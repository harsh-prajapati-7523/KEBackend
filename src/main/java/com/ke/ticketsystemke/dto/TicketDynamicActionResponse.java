package com.ke.ticketsystemke.dto;

public record TicketDynamicActionResponse(
        String actionKey,
        Long transitionId,
        String displayName,
        String buttonLabel,
        String description,
        boolean available,
        String reasonCode,
        String message,
        Long fromStatusId,
        String fromStatusKey,
        String fromStatusDisplayName,
        Long toStatusId,
        String toStatusKey,
        String toStatusDisplayName,
        Boolean toStatusTerminal,
        boolean requiresComment,
        boolean confirmationRequired,
        Integer sortOrder
) {
}
