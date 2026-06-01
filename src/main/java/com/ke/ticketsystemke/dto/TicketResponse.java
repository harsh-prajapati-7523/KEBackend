package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.Ticket;
import com.ke.ticketsystemke.entity.TicketCategory;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.ManufacturerStatus;
import com.ke.ticketsystemke.entity.WarrantyStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        String cancellationReason,
        WarrantyStatus warrantyStatus,
        ManufacturerStatus manufacturerStatus,
        String manufacturerComplaintNumber,
        String manufacturerOrBrandName,
        String productSerialNumber,
        Instant warrantyUpdatedAt,
        String warrantyUpdatedByEmployeeId,
        BigDecimal totalCharge
) {

    public static TicketResponse from(Ticket ticket) {
        return from(ticket, BigDecimal.ZERO.setScale(2));
    }

    public static TicketResponse from(Ticket ticket, BigDecimal totalCharge) {
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
                ticket.getCancellationReason(),
                ticket.getWarrantyStatus(),
                ticket.getManufacturerStatus(),
                ticket.getManufacturerComplaintNumber(),
                ticket.getManufacturerOrBrandName(),
                ticket.getProductSerialNumber(),
                ticket.getWarrantyUpdatedAt(),
                ticket.getWarrantyUpdatedByEmployeeId(),
                totalCharge != null ? totalCharge.setScale(2, RoundingMode.UNNECESSARY) : BigDecimal.ZERO.setScale(2)
        );
    }
}
