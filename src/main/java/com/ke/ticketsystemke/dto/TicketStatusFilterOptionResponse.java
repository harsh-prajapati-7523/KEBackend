package com.ke.ticketsystemke.dto;

public record TicketStatusFilterOptionResponse(
        String statusKey,
        String displayName,
        boolean active,
        Integer sortOrder,
        boolean terminal
) {
}
