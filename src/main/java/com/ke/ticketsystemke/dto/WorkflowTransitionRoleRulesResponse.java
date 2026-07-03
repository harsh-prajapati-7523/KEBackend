package com.ke.ticketsystemke.dto;

import java.util.List;
import java.util.Map;

public record WorkflowTransitionRoleRulesResponse(
        Map<Long, List<WorkflowTransitionRoleRuleResponse>> rulesByTransitionId
) {
    public WorkflowTransitionRoleRulesResponse {
        rulesByTransitionId = rulesByTransitionId == null ? Map.of() : Map.copyOf(rulesByTransitionId);
    }
}
