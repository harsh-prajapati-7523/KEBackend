package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowStatus;
import com.ke.ticketsystemke.entity.WorkflowTransition;

public final class WorkflowStatusValidationHelper {

    private WorkflowStatusValidationHelper() {
    }

    public static boolean isSystemStatusMetadataValid(
            WorkflowStatus workflowStatus,
            TicketStatus expectedStatus
    ) {
        return workflowStatus != null
                && expectedStatus != null
                && workflowStatus.isActive()
                && workflowStatus.isSystemStatus()
                && workflowStatus.isProtectedStatus()
                && workflowStatus.getBehaviorBucket() == expectedStatus
                && expectedStatus.name().equals(workflowStatus.getStatusKey());
    }

    public static CustomStatusExecutability evaluateCustomStatusExecutability(
            WorkflowStatus targetStatus,
            boolean allowTerminalTarget
    ) {
        if (targetStatus == null) {
            return CustomStatusExecutability.blocked("STATUS_METADATA_MISSING");
        }
        if (!targetStatus.isActive()) {
            return CustomStatusExecutability.blocked("STATUS_METADATA_INACTIVE");
        }
        if (targetStatus.getBehaviorBucket() == null) {
            return CustomStatusExecutability.blocked("BEHAVIOR_BUCKET_MISSING");
        }
        if (targetStatus.isSystemStatus() || targetStatus.isProtectedStatus()) {
            return CustomStatusExecutability.blocked("SYSTEM_OR_PROTECTED_STATUS");
        }
        if (targetStatus.isTerminal() && !allowTerminalTarget) {
            return CustomStatusExecutability.blocked("TERMINAL_TARGET_NOT_ALLOWED");
        }
        return CustomStatusExecutability.executable(targetStatus.getBehaviorBucket());
    }

    public static boolean isTransitionMetadataActive(WorkflowTransition transition) {
        return transition != null
                && transition.isActive()
                && transition.getFromStatusRecord() != null
                && transition.getFromStatusRecord().isActive()
                && transition.getToStatusRecord() != null
                && transition.getToStatusRecord().isActive();
    }

    public record CustomStatusExecutability(
            boolean executable,
            TicketStatus behaviorBucket,
            String blockedReasonCode
    ) {

        private static CustomStatusExecutability executable(TicketStatus behaviorBucket) {
            return new CustomStatusExecutability(true, behaviorBucket, null);
        }

        private static CustomStatusExecutability blocked(String reasonCode) {
            return new CustomStatusExecutability(false, null, reasonCode);
        }
    }
}
