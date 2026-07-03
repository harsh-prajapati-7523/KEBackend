package com.ke.ticketsystemke.dto;

import java.util.List;

public record TicketAvailableActionsResponse(
        Long ticketId,
        List<TicketDynamicActionResponse> dynamicActions
) {

    public TicketAvailableActionsResponse {
        dynamicActions = dynamicActions == null ? List.of() : List.copyOf(dynamicActions);
    }
}
