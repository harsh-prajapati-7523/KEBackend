package com.ke.ticketsystemke.dto;

public record TicketDynamicActionResponse(
        Long transitionId,
        String actionKey,
        String displayName,
        String fromStatus,
        String toStatus,
        boolean allowed
) {
}
