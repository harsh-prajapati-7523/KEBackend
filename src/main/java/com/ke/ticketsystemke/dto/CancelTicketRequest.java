package com.ke.ticketsystemke.dto;

import jakarta.validation.constraints.NotBlank;

public class CancelTicketRequest {

    @NotBlank
    private String cancellationReason;

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}
