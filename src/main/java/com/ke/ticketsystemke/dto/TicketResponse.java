package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.Ticket;
import com.ke.ticketsystemke.entity.TicketCategory;
import com.ke.ticketsystemke.entity.TicketStatus;

import java.time.Instant;

public record TicketResponse(
        Long id,
        String ticketNumber,
        String customerName,
        String mobileNumber,
        String villageOrArea,
        String productType,
        TicketCategory category,
        String complaintDescription,
        TicketStatus status,
        Instant createdAt,
        Instant updatedAt,
        String createdByEmployeeId,
        String pickedByEmployeeId,
        Instant completedAt,
        String completedByEmployeeId,
        String completionRemark,
        Instant cancelledAt,
        String cancelledByEmployeeId,
        String cancellationReason
) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getCustomerName(),
                ticket.getMobileNumber(),
                ticket.getVillageOrArea(),
                ticket.getProductType(),
                ticket.getCategory(),
                ticket.getComplaintDescription(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getCreatedByEmployeeId(),
                ticket.getPickedByEmployeeId(),
                ticket.getCompletedAt(),
                ticket.getCompletedByEmployeeId(),
                ticket.getCompletionRemark(),
                ticket.getCancelledAt(),
                ticket.getCancelledByEmployeeId(),
                ticket.getCancellationReason()
        );
    }
}
