package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.TicketWorkflowHistory;
import com.ke.ticketsystemke.entity.WorkflowStatus;
import com.ke.ticketsystemke.entity.WorkflowTransition;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TicketWorkflowHistoryResponseTest {

    @Test
    void fromUsesTransitionStatusMetadataWhenHistoryStatusRecordsAreMissing() {
        WorkflowStatus inProgress = status("IN_PROGRESS", "In Progress", TicketStatus.IN_PROGRESS);
        WorkflowStatus repairCompleted = status(
                "REPAIR_COMPLETED",
                "Repair Completed / Ready for Delivery",
                TicketStatus.IN_PROGRESS
        );

        WorkflowTransition transition = new WorkflowTransition();
        transition.setFromStatusRecord(inProgress);
        transition.setToStatusRecord(repairCompleted);

        TicketWorkflowHistory history = new TicketWorkflowHistory();
        history.setActionKey("MARK_REPAIR_COMPLETED");
        history.setFromStatus("IN_PROGRESS");
        history.setToStatus("IN_PROGRESS");
        setField(history, "workflowTransition", transition);

        TicketWorkflowHistoryResponse response = TicketWorkflowHistoryResponse.from(history);

        assertEquals("IN_PROGRESS", response.fromStatus());
        assertEquals("IN_PROGRESS", response.toStatus());
        assertEquals("IN_PROGRESS", response.fromStatusKey());
        assertEquals("REPAIR_COMPLETED", response.toStatusKey());
        assertEquals("IN_PROGRESS", response.fromBehaviorBucket());
        assertEquals("IN_PROGRESS", response.toBehaviorBucket());
        assertEquals("Repair Completed / Ready for Delivery", response.toStatusDisplayName());
    }

    @Test
    void fromFallsBackToLegacyStatusValuesWhenNoStatusMetadataExists() {
        TicketWorkflowHistory history = new TicketWorkflowHistory();
        history.setActionKey("START_REPAIR_WORK");
        history.setFromStatus("NEW");
        history.setToStatus("IN_PROGRESS");

        TicketWorkflowHistoryResponse response = TicketWorkflowHistoryResponse.from(history);

        assertEquals("NEW", response.fromStatusKey());
        assertEquals("IN_PROGRESS", response.toStatusKey());
        assertEquals("NEW", response.fromBehaviorBucket());
        assertEquals("IN_PROGRESS", response.toBehaviorBucket());
    }

    private static WorkflowStatus status(String statusKey, String displayName, TicketStatus behaviorBucket) {
        WorkflowStatus status = new WorkflowStatus();
        status.setStatusKey(statusKey);
        status.setDisplayName(displayName);
        status.setBehaviorBucket(behaviorBucket);
        return status;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError("Unable to set test field " + fieldName, ex);
        }
    }
}
