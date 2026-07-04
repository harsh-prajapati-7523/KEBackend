package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowAction;
import com.ke.ticketsystemke.entity.WorkflowStatus;
import com.ke.ticketsystemke.entity.WorkflowTransition;

import java.util.ArrayList;
import java.util.List;

public final class WorkflowStatusValidationHelper {

    private static final List<String> PROTECTED_FIXED_ACTION_KEYS = List.of(
            AccessKey.PICK_TICKET.name(),
            AccessKey.START_WORK.name(),
            AccessKey.COMPLETE_TICKET.name(),
            AccessKey.CANCEL_TICKET.name()
    );

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

    public static boolean isProtectedFixedActionKey(String actionKey) {
        return PROTECTED_FIXED_ACTION_KEYS.contains(actionKey);
    }

    public static boolean isAllowedCustomTerminalTarget(WorkflowStatus workflowStatus) {
        return isCustomTargetStatus(workflowStatus)
                && workflowStatus.isTerminal()
                && workflowStatus.isActive()
                && workflowStatus.getBehaviorBucket() == TicketStatus.COMPLETED;
    }

    public static boolean isTerminalBehavior(TicketStatus status) {
        return status == TicketStatus.COMPLETED || status == TicketStatus.CANCELLED;
    }

    public static boolean requiresUnsupportedBusinessSideEffect(
            TicketStatus toStatus,
            boolean customTerminalTarget
    ) {
        return toStatus == TicketStatus.PICKED
                || (toStatus == TicketStatus.COMPLETED && !customTerminalTarget)
                || toStatus == TicketStatus.CANCELLED;
    }

    public static GenericTransitionEligibility evaluateGenericTransitionEligibility(
            WorkflowTransition transition,
            WorkflowAction action,
            WorkflowStatus fromStatus,
            WorkflowStatus toStatus,
            TicketStatus toStatusBehaviorBucket
    ) {
        List<GenericTransitionEligibilityIssue> issues = new ArrayList<>();
        boolean customTerminalTarget = isAllowedCustomTerminalTarget(toStatus);

        if (transition != null && transition.isProtectedTransition()) {
            issues.add(GenericTransitionEligibilityIssue.PROTECTED_TRANSITION);
        }
        if (action != null && action.isProtectedAction()) {
            issues.add(GenericTransitionEligibilityIssue.PROTECTED_ACTION);
        }
        if (action != null && isProtectedFixedActionKey(action.getActionKey())) {
            issues.add(GenericTransitionEligibilityIssue.PROTECTED_FIXED_ACTION_KEY);
        }
        if (fromStatus != null && fromStatus.isTerminal()) {
            issues.add(GenericTransitionEligibilityIssue.TERMINAL_SOURCE_STATUS);
        }
        if (toStatus != null && toStatus.isTerminal() && !customTerminalTarget) {
            issues.add(GenericTransitionEligibilityIssue.PROTECTED_TERMINAL_TARGET_STATUS);
        }
        if (requiresUnsupportedBusinessSideEffect(toStatusBehaviorBucket, customTerminalTarget)) {
            issues.add(GenericTransitionEligibilityIssue.UNSUPPORTED_TARGET_SIDE_EFFECT);
        }

        return new GenericTransitionEligibility(issues);
    }

    private static boolean isCustomTargetStatus(WorkflowStatus workflowStatus) {
        return workflowStatus != null
                && !workflowStatus.isSystemStatus()
                && !workflowStatus.isProtectedStatus();
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

    public record GenericTransitionEligibility(
            List<GenericTransitionEligibilityIssue> issues
    ) {

        public GenericTransitionEligibility {
            issues = List.copyOf(issues);
        }

        public boolean executable() {
            return issues.isEmpty();
        }

        public boolean contains(GenericTransitionEligibilityIssue issue) {
            return issues.contains(issue);
        }
    }

    public enum GenericTransitionEligibilityIssue {
        PROTECTED_TRANSITION,
        PROTECTED_ACTION,
        PROTECTED_FIXED_ACTION_KEY,
        TERMINAL_SOURCE_STATUS,
        PROTECTED_TERMINAL_TARGET_STATUS,
        UNSUPPORTED_TARGET_SIDE_EFFECT
    }
}
