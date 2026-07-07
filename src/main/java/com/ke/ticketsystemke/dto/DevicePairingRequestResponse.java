package com.ke.ticketsystemke.dto;

import java.time.Instant;

public record DevicePairingRequestResponse(
        String requestId,
        String status,
        String deviceLabel,
        String deviceFingerprint,
        Instant createdAt,
        Instant expiresAt,
        Instant approvedAt,
        String employeeId
) {
}
