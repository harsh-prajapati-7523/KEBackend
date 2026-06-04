package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.entity.TicketStatus;

public record WorkflowTransitionOptionResponse(
        AccessKey actionKey,
        String displayName,
        TicketStatus fromStatus,
        TicketStatus toStatus,
        boolean alreadyConfigured,
        Long transitionId,
        Boolean active,
        Integer sortOrder,
        Boolean systemTransition,
        Boolean protectedTransition
) {
}
