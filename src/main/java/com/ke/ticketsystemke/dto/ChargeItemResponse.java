package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.TicketChargeItem;

import java.math.BigDecimal;
import java.time.Instant;

public record ChargeItemResponse(
        Long id,
        String description,
        BigDecimal amount,
        Instant createdAt,
        String createdByEmployeeId
) {

    public static ChargeItemResponse from(TicketChargeItem item) {
        return new ChargeItemResponse(
                item.getId(),
                item.getDescription(),
                item.getAmount(),
                item.getCreatedAt(),
                item.getCreatedByEmployeeId()
        );
    }
}
