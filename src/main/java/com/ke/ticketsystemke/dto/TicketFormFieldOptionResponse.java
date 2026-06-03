package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.DropdownOption;

public record TicketFormFieldOptionResponse(
        String optionKey,
        String displayValue
) {
    public static TicketFormFieldOptionResponse from(DropdownOption option) {
        return new TicketFormFieldOptionResponse(
                option.getOptionKey(),
                option.getDisplayValue()
        );
    }
}
