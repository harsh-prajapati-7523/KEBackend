package com.ke.ticketsystemke.dto;

import java.util.List;

public record TicketWorkflowHistoryPageResponse(
        Long ticketId,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        List<TicketWorkflowHistoryResponse> history
) {
}
