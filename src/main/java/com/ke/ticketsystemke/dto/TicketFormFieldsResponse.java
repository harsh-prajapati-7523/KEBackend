package com.ke.ticketsystemke.dto;

import java.util.List;

public record TicketFormFieldsResponse(
        Long categoryId,
        String categoryKey,
        String categoryDisplayName,
        List<TicketFormFieldResponse> fields
) {
}
