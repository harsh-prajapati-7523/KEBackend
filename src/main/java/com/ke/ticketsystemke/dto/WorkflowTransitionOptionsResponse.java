package com.ke.ticketsystemke.dto;

import java.util.List;

public record WorkflowTransitionOptionsResponse(
        List<WorkflowTransitionOptionResponse> options
) {
}
