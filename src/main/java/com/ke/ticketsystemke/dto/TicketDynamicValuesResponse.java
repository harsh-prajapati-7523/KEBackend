package com.ke.ticketsystemke.dto;

import java.util.List;

public record TicketDynamicValuesResponse(
        Long ticketId,
        List<TicketDynamicValueResponse> dynamicValues
) {
}
