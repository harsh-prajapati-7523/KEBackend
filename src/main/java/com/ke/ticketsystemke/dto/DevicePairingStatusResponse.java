package com.ke.ticketsystemke.dto;

import java.time.Instant;

public record DevicePairingStatusResponse(
        String status,
        Instant expiresAt,
        LoginResponse session
) {
}
