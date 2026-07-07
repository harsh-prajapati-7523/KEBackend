package com.ke.ticketsystemke.dto;

import java.time.Instant;

public record TrustedDeviceResponse(
        Long id,
        String employeeId,
        String employeeName,
        String deviceLabel,
        String deviceFingerprint,
        Instant trustedAt,
        Instant revokedAt,
        String approvedByEmployeeId
) {
}
