package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.Ticket;
import com.ke.ticketsystemke.entity.TicketCategory;
import com.ke.ticketsystemke.entity.TicketStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public record CustomerHistoryTicketResponse(
        Long id,
        String ticketNumber,
        String customerName,
        String productType,
        TicketCategory category,
        TicketStatus status,
        Instant createdAt,
        Instant completedAt,
        Instant cancelledAt,
        BigDecimal totalCharge
) {

    public static CustomerHistoryTicketResponse from(Ticket ticket, BigDecimal totalCharge) {
        return new CustomerHistoryTicketResponse(
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getCustomerName(),
                ticket.getProductType(),
                ticket.getCategory(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getCompletedAt(),
                ticket.getCancelledAt(),
                totalCharge != null ? totalCharge.setScale(2, RoundingMode.UNNECESSARY) : BigDecimal.ZERO.setScale(2)
        );
    }
}
