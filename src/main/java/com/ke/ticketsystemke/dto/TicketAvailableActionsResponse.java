package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.AccessKey;

import java.util.Map;

public record TicketAvailableActionsResponse(
        Long ticketId,
        Map<AccessKey, TicketActionAvailabilityResponse> actions
) {
}
