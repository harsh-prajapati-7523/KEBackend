package com.ke.ticketsystemke.dto;

import java.util.List;
import java.util.Map;

public record WorkflowTransitionCategoryRulesResponse(
        Map<Long, List<WorkflowTransitionCategoryRuleResponse>> rulesByTransitionId
) {
    public WorkflowTransitionCategoryRulesResponse {
        rulesByTransitionId = rulesByTransitionId == null ? Map.of() : Map.copyOf(rulesByTransitionId);
    }
}
