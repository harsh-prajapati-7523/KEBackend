package com.ke.ticketsystemke.dto;

public record PinLoginStatusResponse(
        boolean pinLoginAvailable,
        String employeeName,
        boolean sessionValid
) {
}
