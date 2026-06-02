package com.ke.ticketsystemke.dto;

import java.util.List;

public record CustomerHistoryResponse(
        int previousTicketCount,
        List<CustomerHistoryTicketResponse> tickets
) {
}
