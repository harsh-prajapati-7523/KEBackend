package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.AccessKey;

import java.util.List;
import java.util.Map;

public record TicketAvailableActionsResponse(
        Long ticketId,
        Map<AccessKey, TicketActionAvailabilityResponse> actions,
        List<TicketDynamicActionResponse> dynamicActions
) {

    public TicketAvailableActionsResponse(
            Long ticketId,
            Map<AccessKey, TicketActionAvailabilityResponse> actions
    ) {
        this(ticketId, actions, List.of());
    }

    public TicketAvailableActionsResponse {
        dynamicActions = dynamicActions == null ? List.of() : List.copyOf(dynamicActions);
    }
}
