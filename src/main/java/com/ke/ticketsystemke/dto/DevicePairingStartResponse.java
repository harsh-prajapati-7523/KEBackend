package com.ke.ticketsystemke.dto;

import java.time.Instant;

public record DevicePairingStartResponse(
        String requestId,
        String nonce,
        Instant expiresAt,
        String signature,
        String pairingToken,
        String status,
        long pollAfterSeconds
) {
}
