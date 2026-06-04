package com.ke.ticketsystemke.dto;

public record TicketActionAvailabilityResponse(
        boolean available,
        String reasonCode,
        String message
) {
}
