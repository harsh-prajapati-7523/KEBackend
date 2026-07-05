package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.Ticket;

public record CustomerLookupResponse(
        String customerName,
        String villageOrArea,
        String sourceTicketNumber
) {

    public static CustomerLookupResponse from(Ticket ticket) {
        return new CustomerLookupResponse(
                ticket.getCustomerName(),
                ticket.getVillageOrArea(),
                ticket.getTicketNumber()
        );
    }
}
