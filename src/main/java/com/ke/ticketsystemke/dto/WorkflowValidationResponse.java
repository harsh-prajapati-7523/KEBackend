package com.ke.ticketsystemke.dto;

import java.util.List;

public record WorkflowValidationResponse(
        Long categoryId,
        String categoryKey,
        boolean valid,
        List<WorkflowValidationIssueResponse> issues,
        boolean readyToActivate,
        List<WorkflowValidationIssueResponse> blockingIssues,
        List<WorkflowValidationIssueResponse> warnings
) {
    public WorkflowValidationResponse(
            Long categoryId,
            String categoryKey,
            boolean valid,
            List<WorkflowValidationIssueResponse> issues
    ) {
        this(categoryId, categoryKey, valid, issues, valid, issues, List.of());
    }

    public WorkflowValidationResponse {
        issues = issues == null ? List.of() : List.copyOf(issues);
        blockingIssues = blockingIssues == null ? List.of() : List.copyOf(blockingIssues);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
