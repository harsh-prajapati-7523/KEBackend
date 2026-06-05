package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.entity.Ticket;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowStatus;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class EffectiveStatusResolver {

    public ResolvedTicketStatus resolve(Ticket ticket) {
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket is required");
        }

        TicketStatus legacyStatus = ticket.getStatus();
        WorkflowStatus statusRecord = ticket.getStatusRecord();
        boolean statusRecordMissing = statusRecord == null;
        boolean legacyTerminal = isTerminal(legacyStatus);

        String fallbackStatusKey = fallbackStatusKey(legacyStatus);
        String fallbackDisplayName = formatStatusDisplayName(legacyStatus);
        Boolean fallbackTerminal = legacyStatus == null ? null : legacyTerminal;

        Long actualStatusId = statusRecord == null ? null : statusRecord.getId();
        String actualStatusKey = statusRecord == null
                ? fallbackStatusKey
                : firstNonBlank(statusRecord.getStatusKey(), fallbackStatusKey);
        String actualStatusDisplayName = statusRecord == null
                ? fallbackDisplayName
                : firstNonBlank(statusRecord.getDisplayName(), formatStatusDisplayName(actualStatusKey));
        Boolean actualStatusActive = statusRecord == null ? Boolean.TRUE : statusRecord.isActive();
        Boolean actualStatusTerminal = statusRecord == null ? fallbackTerminal : statusRecord.isTerminal();

        boolean bucketFromFallback = statusRecord == null || statusRecord.getBehaviorBucket() == null;
        TicketStatus behaviorBucket = bucketFromFallback ? legacyStatus : statusRecord.getBehaviorBucket();
        Boolean behaviorBucketTerminal = behaviorBucket == null ? null : isTerminal(behaviorBucket);

        boolean statusMismatch = statusRecord != null
                && legacyStatus != null
                && isPresent(statusRecord.getStatusKey())
                && !legacyStatus.name().equals(statusRecord.getStatusKey());
        boolean behaviorBucketMismatch = behaviorBucket != null
                && legacyStatus != null
                && behaviorBucket != legacyStatus;

        ResolvedTicketStatus.WarningCode warningCode = resolveWarningCode(
                legacyStatus,
                statusRecordMissing,
                actualStatusKey,
                statusMismatch,
                behaviorBucketMismatch
        );

        return new ResolvedTicketStatus(
                legacyStatus,
                actualStatusId,
                actualStatusKey,
                actualStatusDisplayName,
                actualStatusActive,
                actualStatusTerminal,
                behaviorBucket,
                behaviorBucketTerminal,
                legacyTerminal,
                statusRecordMissing,
                bucketFromFallback,
                statusMismatch,
                behaviorBucketMismatch,
                warningCode
        );
    }

    boolean isTerminal(TicketStatus status) {
        return status == TicketStatus.COMPLETED || status == TicketStatus.CANCELLED;
    }

    String formatStatusDisplayName(TicketStatus status) {
        return status == null ? null : formatStatusDisplayName(status.name());
    }

    private ResolvedTicketStatus.WarningCode resolveWarningCode(
            TicketStatus legacyStatus,
            boolean statusRecordMissing,
            String actualStatusKey,
            boolean statusMismatch,
            boolean behaviorBucketMismatch
    ) {
        if (legacyStatus == null || !isPresent(actualStatusKey)) {
            return ResolvedTicketStatus.WarningCode.STATUS_METADATA_MISSING;
        }
        if (behaviorBucketMismatch) {
            return ResolvedTicketStatus.WarningCode.BEHAVIOR_BUCKET_MISMATCH;
        }
        if (statusMismatch) {
            return ResolvedTicketStatus.WarningCode.STATUS_KEY_MISMATCH;
        }
        if (statusRecordMissing) {
            return ResolvedTicketStatus.WarningCode.STATUS_RECORD_MISSING;
        }
        return ResolvedTicketStatus.WarningCode.NONE;
    }

    private String fallbackStatusKey(TicketStatus status) {
        return status == null ? null : status.name();
    }

    private String formatStatusDisplayName(String statusKey) {
        if (!isPresent(statusKey)) {
            return null;
        }

        String[] words = statusKey.toLowerCase(Locale.ROOT).split("_");
        StringBuilder displayName = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!displayName.isEmpty()) {
                displayName.append(' ');
            }
            displayName.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                displayName.append(word.substring(1));
            }
        }
        return displayName.toString();
    }

    private String firstNonBlank(String first, String fallback) {
        return isPresent(first) ? first : fallback;
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
