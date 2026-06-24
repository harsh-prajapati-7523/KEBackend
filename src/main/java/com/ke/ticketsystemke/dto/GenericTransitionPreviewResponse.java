package com.ke.ticketsystemke.dto;

public record GenericTransitionPreviewResponse(
        Long ticketId,
        Long transitionId,
        boolean allowed,
        String actionKey,
        String actionDisplayName,
        String fromStatus,
        String toStatus,
        String fromStatusDisplayName,
        String toStatusDisplayName,
        boolean requiresComment,
        boolean requiresReason,
        String disabledReason
) {

    public static GenericTransitionPreviewResponse denied(
            Long ticketId,
            Long transitionId,
            String disabledReason
    ) {
        return new GenericTransitionPreviewResponse(
                ticketId,
                transitionId,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                disabledReason
        );
    }
}
