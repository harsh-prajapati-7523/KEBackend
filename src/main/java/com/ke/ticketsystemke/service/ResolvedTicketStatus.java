package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.entity.TicketStatus;

public record ResolvedTicketStatus(
        TicketStatus legacyStatus,
        Long actualStatusId,
        String actualStatusKey,
        String actualStatusDisplayName,
        Boolean actualStatusActive,
        Boolean actualStatusTerminal,
        TicketStatus behaviorBucket,
        Boolean behaviorBucketTerminal,
        Boolean legacyTerminal,
        Boolean statusRecordMissing,
        Boolean bucketFromFallback,
        Boolean statusMismatch,
        Boolean behaviorBucketMismatch,
        WarningCode warningCode
) {

    public enum WarningCode {
        NONE,
        STATUS_RECORD_MISSING,
        STATUS_KEY_MISMATCH,
        BEHAVIOR_BUCKET_MISMATCH,
        STATUS_METADATA_MISSING
    }
}
