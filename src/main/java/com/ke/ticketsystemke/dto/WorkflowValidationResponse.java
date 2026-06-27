package com.ke.ticketsystemke.dto;

import java.util.List;

public record WorkflowValidationResponse(
        Long categoryId,
        String categoryKey,
        boolean valid,
        List<WorkflowValidationIssueResponse> issues
) {
    public WorkflowValidationResponse {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }
}
