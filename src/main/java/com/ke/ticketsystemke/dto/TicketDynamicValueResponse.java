package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.TicketDynamicValue;
import com.ke.ticketsystemke.entity.TicketFieldType;

public record TicketDynamicValueResponse(
        Long id,
        String fieldKey,
        String fieldLabel,
        TicketFieldType fieldType,
        String displayValue
) {
    public static TicketDynamicValueResponse from(TicketDynamicValue dynamicValue) {
        return new TicketDynamicValueResponse(
                dynamicValue.getId(),
                dynamicValue.getFieldKeySnapshot(),
                dynamicValue.getFieldLabelSnapshot(),
                dynamicValue.getFieldTypeSnapshot(),
                dynamicValue.getDisplayValue()
        );
    }
}
