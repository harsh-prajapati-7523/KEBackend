package com.ke.ticketsystemke.dto;

public record WorkflowValidationIssueResponse(
        String code,
        String message,
        Long transitionId
) {
}
