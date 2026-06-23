package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.Ticket;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.TicketWorkflowHistory;
import com.ke.ticketsystemke.entity.WorkflowAction;
import com.ke.ticketsystemke.entity.WorkflowTransition;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.repository.TicketWorkflowHistoryRepository;
import com.ke.ticketsystemke.repository.WorkflowActionRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketWorkflowHistoryService {

    private static final int TICKET_NUMBER_MAX_LENGTH = 40;
    private static final int ACTION_KEY_MAX_LENGTH = 60;
    private static final int STATUS_MAX_LENGTH = 60;
    private static final int EMPLOYEE_ID_MAX_LENGTH = 80;
    private static final int EMPLOYEE_NAME_MAX_LENGTH = 120;
    private static final int COMMENT_MAX_LENGTH = 1000;
    private static final int REASON_MAX_LENGTH = 500;
    private static final int RESULT_MAX_LENGTH = 30;
    private static final int FAILURE_REASON_CODE_MAX_LENGTH = 80;
    private static final int FAILURE_MESSAGE_MAX_LENGTH = 255;
    private static final String SUCCESS_RESULT = "SUCCESS";

    private final TicketWorkflowHistoryRepository ticketWorkflowHistoryRepository;
    private final WorkflowActionRepository workflowActionRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final EmployeeRepository employeeRepository;

    public TicketWorkflowHistoryService(
            TicketWorkflowHistoryRepository ticketWorkflowHistoryRepository,
            WorkflowActionRepository workflowActionRepository,
            WorkflowTransitionRepository workflowTransitionRepository,
            EmployeeRepository employeeRepository
    ) {
        this.ticketWorkflowHistoryRepository = ticketWorkflowHistoryRepository;
        this.workflowActionRepository = workflowActionRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordSuccessfulFixedAction(
            Ticket ticket,
            AccessKey actionKey,
            TicketStatus fromStatus,
            TicketStatus toStatus,
            Long fromStatusId,
            Long toStatusId,
            String executedByEmployeeId,
            String previousOwnerEmployeeId,
            String newOwnerEmployeeId,
            String comment,
            String reason
    ) {
        if (ticket == null || ticket.getId() == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Ticket audit context missing");
        }
        if (actionKey == null || fromStatus == null || toStatus == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Workflow audit context missing");
        }

        WorkflowAction workflowAction = workflowActionRepository.findByActionKey(actionKey.name())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Workflow action metadata missing"
                ));
        WorkflowTransition workflowTransition = workflowTransitionRepository
                .findByActionKeyAndFromStatusAndToStatus(actionKey, fromStatus, toStatus)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Workflow transition metadata missing"
                ));

        String employeeNameSnapshot = employeeRepository.findByEmployeeIdIgnoreCase(
                        executedByEmployeeId == null ? "" : executedByEmployeeId.trim()
                )
                .map(Employee::getName)
                .orElse(null);

        TicketWorkflowHistory history = prepareHistoryEntry(new HistoryEntryDraft(
                ticket.getId(),
                ticket.getTicketNumber(),
                actionKey.name(),
                workflowAction.getId(),
                workflowTransition.getId(),
                fromStatus.name(),
                toStatus.name(),
                fromStatusId,
                toStatusId,
                executedByEmployeeId,
                employeeNameSnapshot,
                previousOwnerEmployeeId,
                newOwnerEmployeeId,
                comment,
                reason,
                SUCCESS_RESULT,
                null,
                null,
                true,
                false,
                null
        ));

        ticketWorkflowHistoryRepository.save(history);
    }

    TicketWorkflowHistory prepareHistoryEntry(HistoryEntryDraft draft) {
        TicketWorkflowHistory history = new TicketWorkflowHistory();
        history.setTicketId(draft.ticketId());
        history.setTicketNumber(sanitize(draft.ticketNumber(), TICKET_NUMBER_MAX_LENGTH));
        history.setActionKey(sanitize(draft.actionKey(), ACTION_KEY_MAX_LENGTH));
        history.setWorkflowActionId(draft.workflowActionId());
        history.setWorkflowTransitionId(draft.workflowTransitionId());
        history.setFromStatus(sanitize(draft.fromStatus(), STATUS_MAX_LENGTH));
        history.setToStatus(sanitize(draft.toStatus(), STATUS_MAX_LENGTH));
        history.setFromStatusId(draft.fromStatusId());
        history.setToStatusId(draft.toStatusId());
        history.setExecutedByEmployeeId(sanitize(draft.executedByEmployeeId(), EMPLOYEE_ID_MAX_LENGTH));
        history.setExecutedByEmployeeNameSnapshot(sanitize(draft.executedByEmployeeNameSnapshot(), EMPLOYEE_NAME_MAX_LENGTH));
        history.setPreviousOwnerEmployeeId(sanitize(draft.previousOwnerEmployeeId(), EMPLOYEE_ID_MAX_LENGTH));
        history.setNewOwnerEmployeeId(sanitize(draft.newOwnerEmployeeId(), EMPLOYEE_ID_MAX_LENGTH));
        history.setComment(sanitize(draft.comment(), COMMENT_MAX_LENGTH));
        history.setReason(sanitize(draft.reason(), REASON_MAX_LENGTH));
        history.setResult(sanitize(draft.result(), RESULT_MAX_LENGTH));
        history.setFailureReasonCode(sanitize(draft.failureReasonCode(), FAILURE_REASON_CODE_MAX_LENGTH));
        history.setFailureMessage(sanitize(draft.failureMessage(), FAILURE_MESSAGE_MAX_LENGTH));
        history.setSystemTransition(draft.systemTransition());
        history.setCustomTransition(draft.customTransition());
        history.setMetadataJson(draft.metadataJson());
        return history;
    }

    private String sanitize(String value, int maxLength) {
        if (value == null) {
            return null;
        }

        String sanitized = value.trim();
        if (sanitized.isEmpty()) {
            return null;
        }

        sanitized = sanitized.replaceAll("[\\r\\n\\t]+", " ");
        sanitized = sanitized.replaceAll(" {2,}", " ");

        return sanitized.length() > maxLength ? sanitized.substring(0, maxLength) : sanitized;
    }

    record HistoryEntryDraft(
            Long ticketId,
            String ticketNumber,
            String actionKey,
            Long workflowActionId,
            Long workflowTransitionId,
            String fromStatus,
            String toStatus,
            Long fromStatusId,
            Long toStatusId,
            String executedByEmployeeId,
            String executedByEmployeeNameSnapshot,
            String previousOwnerEmployeeId,
            String newOwnerEmployeeId,
            String comment,
            String reason,
            String result,
            String failureReasonCode,
            String failureMessage,
            boolean systemTransition,
            boolean customTransition,
            String metadataJson
    ) {
    }
}
