package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.Ticket;
import com.ke.ticketsystemke.entity.TicketCategory;
import com.ke.ticketsystemke.entity.TicketCategoryConfig;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowStatus;
import com.ke.ticketsystemke.service.ResolvedTicketStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;

public record TicketResponse(
        Long id,
        String ticketNumber,
        String customerName,
        String mobileNumber,
        String villageOrArea,
        String productType,
        String category,
        Long categoryId,
        String categoryKey,
        String categoryDisplayName,
        String complaintDescription,
        TicketStatus status,
        Long statusId,
        String statusKey,
        String statusDisplayName,
        Boolean statusActive,
        Boolean statusTerminal,
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
        BigDecimal totalCharge
) {

    public static TicketResponse from(Ticket ticket) {
        return from(ticket, BigDecimal.ZERO.setScale(2));
    }

    public static TicketResponse from(Ticket ticket, BigDecimal totalCharge) {
        return from(ticket, totalCharge, null);
    }

    public static TicketResponse from(Ticket ticket, BigDecimal totalCharge, ResolvedTicketStatus resolvedStatus) {
        TicketCategoryConfig categoryRecord = ticket.getCategoryRecord();
        StatusMetadata statusMetadata = resolveStatusMetadata(ticket, resolvedStatus);
        String categoryKey = categoryRecord != null ? categoryRecord.getCategoryKey() : fallbackCategoryKey(ticket.getCategory());
        return new TicketResponse(
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getCustomerName(),
                ticket.getMobileNumber(),
                ticket.getVillageOrArea(),
                ticket.getProductType(),
                categoryKey,
                categoryRecord != null ? categoryRecord.getId() : null,
                categoryKey,
                categoryRecord != null ? categoryRecord.getDisplayName() : null,
                ticket.getComplaintDescription(),
                ticket.getStatus(),
                statusMetadata.statusId(),
                statusMetadata.statusKey(),
                statusMetadata.statusDisplayName(),
                statusMetadata.statusActive(),
                statusMetadata.statusTerminal(),
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
                totalCharge != null ? totalCharge.setScale(2, RoundingMode.UNNECESSARY) : BigDecimal.ZERO.setScale(2)
        );
    }

    private static String fallbackCategoryKey(TicketCategory category) {
        return category == null ? null : category.name();
    }

    private static StatusMetadata resolveStatusMetadata(Ticket ticket, ResolvedTicketStatus resolvedStatus) {
        if (resolvedStatus != null) {
            return new StatusMetadata(
                    resolvedStatus.actualStatusId(),
                    resolvedStatus.actualStatusKey(),
                    resolvedStatus.actualStatusDisplayName(),
                    resolvedStatus.actualStatusActive(),
                    resolvedStatus.actualStatusTerminal()
            );
        }

        WorkflowStatus statusRecord = ticket.getStatusRecord();
        return new StatusMetadata(
                statusRecord != null ? statusRecord.getId() : null,
                statusRecord != null ? statusRecord.getStatusKey() : fallbackStatusKey(ticket.getStatus()),
                statusRecord != null ? statusRecord.getDisplayName() : fallbackStatusDisplayName(ticket.getStatus()),
                statusRecord != null ? statusRecord.isActive() : Boolean.TRUE,
                statusRecord != null ? statusRecord.isTerminal() : fallbackStatusTerminal(ticket.getStatus())
        );
    }

    private static String fallbackStatusKey(TicketStatus status) {
        return status == null ? null : status.name();
    }

    private static String fallbackStatusDisplayName(TicketStatus status) {
        if (status == null) {
            return null;
        }

        String[] words = status.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder displayName = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!displayName.isEmpty()) {
                displayName.append(' ');
            }
            displayName.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                displayName.append(word.substring(1));
            }
        }
        return displayName.toString();
    }

    private static Boolean fallbackStatusTerminal(TicketStatus status) {
        return status == TicketStatus.COMPLETED || status == TicketStatus.CANCELLED;
    }

    private record StatusMetadata(
            Long statusId,
            String statusKey,
            String statusDisplayName,
            Boolean statusActive,
            Boolean statusTerminal
    ) {
    }
}
