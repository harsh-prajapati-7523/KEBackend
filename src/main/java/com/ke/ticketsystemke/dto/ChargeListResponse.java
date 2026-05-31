package com.ke.ticketsystemke.dto;

import java.math.BigDecimal;
import java.util.List;

public record ChargeListResponse(
        List<ChargeItemResponse> chargeItems,
        BigDecimal totalCharge
) {
}
