package com.ke.ticketsystemke.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class RepairWorkflowFeatureFlag {

    private static final Set<String> REPAIR_WORKFLOW_ACTION_KEYS = Set.of(
            "START_REPAIR_WORK",
            "MARK_MISSING_PART",
            "MARK_PART_AVAILABLE",
            "RESUME_WORK",
            "NEED_CUSTOMER_APPROVAL",
            "CUSTOMER_APPROVED",
            "MARK_IN_WARRANTY",
            "LOG_WARRANTY_COMPLAINT",
            "MARK_REPAIR_COMPLETED",
            "CUSTOMER_DECLINED_REPAIR",
            "CANCEL_PENDING_DELIVERY",
            "DELIVER_TO_CUSTOMER"
    );

    private final boolean enabled;

    public RepairWorkflowFeatureFlag(@Value("${ke.workflow.repair.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isRepairWorkflowAction(String actionKey) {
        return actionKey != null && REPAIR_WORKFLOW_ACTION_KEYS.contains(actionKey);
    }

    public List<String> repairWorkflowActionKeys() {
        return REPAIR_WORKFLOW_ACTION_KEYS.stream().sorted().toList();
    }
}
