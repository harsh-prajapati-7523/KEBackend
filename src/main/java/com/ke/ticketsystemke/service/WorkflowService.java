package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.CreateWorkflowTransitionRequest;
import com.ke.ticketsystemke.dto.UpdateWorkflowTransitionRequest;
import com.ke.ticketsystemke.dto.WorkflowTransitionOptionResponse;
import com.ke.ticketsystemke.dto.WorkflowTransitionOptionsResponse;
import com.ke.ticketsystemke.dto.WorkflowTransitionResponse;
import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowTransition;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class WorkflowService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowService.class);
    private static final Set<AccessKey> WORKFLOW_ACTION_KEYS = EnumSet.of(
            AccessKey.PICK_TICKET,
            AccessKey.START_WORK,
            AccessKey.COMPLETE_TICKET,
            AccessKey.CANCEL_TICKET
    );
    private static final List<SafeTransitionOption> SAFE_TRANSITION_OPTIONS = List.of(
            new SafeTransitionOption(AccessKey.PICK_TICKET, "Pick Ticket", TicketStatus.NEW, TicketStatus.PICKED, 10),
            new SafeTransitionOption(AccessKey.PICK_TICKET, "Pick Ticket", TicketStatus.PICKED, TicketStatus.PICKED, 20),
            new SafeTransitionOption(AccessKey.START_WORK, "Start Work", TicketStatus.PICKED, TicketStatus.IN_PROGRESS, 30),
            new SafeTransitionOption(AccessKey.COMPLETE_TICKET, "Complete Ticket", TicketStatus.IN_PROGRESS, TicketStatus.COMPLETED, 40),
            new SafeTransitionOption(AccessKey.CANCEL_TICKET, "Cancel Ticket", TicketStatus.NEW, TicketStatus.CANCELLED, 50),
            new SafeTransitionOption(AccessKey.CANCEL_TICKET, "Cancel Ticket", TicketStatus.PICKED, TicketStatus.CANCELLED, 60),
            new SafeTransitionOption(AccessKey.CANCEL_TICKET, "Cancel Ticket", TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED, 70)
    );

    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final EmployeeRepository employeeRepository;

    public WorkflowService(
            WorkflowTransitionRepository workflowTransitionRepository,
            EmployeeRepository employeeRepository
    ) {
        this.workflowTransitionRepository = workflowTransitionRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public boolean isTransitionAllowed(AccessKey actionKey, TicketStatus fromStatus, TicketStatus toStatus) {
        try {
            validateWorkflowActionKey(actionKey);
            if (isTerminalStatus(fromStatus)) {
                log.warn("event=workflow_transition_denied actionKey={} fromStatus={} toStatus={} decision=terminal", actionKey, fromStatus, toStatus);
                return false;
            }
            return workflowTransitionRepository.findByActionKeyAndFromStatusAndToStatus(actionKey, fromStatus, toStatus)
                    .map(WorkflowTransition::isActive)
                    .orElse(false);
        } catch (RuntimeException ex) {
            log.warn("event=workflow_transition_check_failed actionKey={} fromStatus={} toStatus={} decision=deny", actionKey, fromStatus, toStatus);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public void requireTransitionAllowed(AccessKey actionKey, TicketStatus fromStatus, TicketStatus toStatus) {
        if (!isTransitionAllowed(actionKey, fromStatus, toStatus)) {
            log.warn("event=workflow_transition_denied actionKey={} fromStatus={} toStatus={} decision=deny", actionKey, fromStatus, toStatus);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow transition is not active");
        }
    }

    @Transactional(readOnly = true)
    public List<WorkflowTransitionResponse> listTransitions() {
        return workflowTransitionRepository.findAllByOrderBySortOrderAscIdAsc()
                .stream()
                .map(WorkflowTransitionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkflowTransitionOptionsResponse getTransitionOptions(String employeeId) {
        Map<TransitionKey, WorkflowTransition> transitionsByKey = new HashMap<>();
        for (WorkflowTransition transition : workflowTransitionRepository.findAll()) {
            transitionsByKey.put(
                    new TransitionKey(transition.getActionKey(), transition.getFromStatus(), transition.getToStatus()),
                    transition
            );
        }

        List<WorkflowTransitionOptionResponse> options = SAFE_TRANSITION_OPTIONS
                .stream()
                .map(option -> toOptionResponse(option, transitionsByKey.get(option.key())))
                .toList();

        log.info("event=workflow_transition_options_returned employeeId={} optionCount={}", employeeId, options.size());
        return new WorkflowTransitionOptionsResponse(options);
    }

    @Transactional
    public WorkflowTransitionResponse createTransition(
            CreateWorkflowTransitionRequest request,
            String employeeId
    ) {
        SafeTransitionOption safeOption = findSafeTransitionOption(
                request.getActionKey(),
                request.getFromStatus(),
                request.getToStatus()
        );

        if (safeOption == null) {
            log.warn("event=workflow_transition_create_rejected employeeId={} actionKey={} fromStatus={} toStatus={} result=unsupported",
                    employeeId,
                    request.getActionKey(),
                    request.getFromStatus(),
                    request.getToStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported workflow transition");
        }

        if (workflowTransitionRepository.findByActionKeyAndFromStatusAndToStatus(
                request.getActionKey(),
                request.getFromStatus(),
                request.getToStatus()
        ).isPresent()) {
            log.warn("event=workflow_transition_create_rejected employeeId={} actionKey={} fromStatus={} toStatus={} result=duplicate",
                    employeeId,
                    request.getActionKey(),
                    request.getFromStatus(),
                    request.getToStatus());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Workflow transition already exists");
        }

        String displayName = request.getDisplayName().trim();
        if (displayName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name cannot be blank");
        }

        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid employee"));

        WorkflowTransition transition = new WorkflowTransition();
        transition.setActionKey(request.getActionKey());
        transition.setDisplayName(displayName);
        transition.setFromStatus(request.getFromStatus());
        transition.setToStatus(request.getToStatus());
        transition.setActive(request.getActive() == null || request.getActive());
        transition.setSortOrder(request.getSortOrder() == null ? safeOption.sortOrder() : request.getSortOrder());
        transition.setSystemTransition(false);
        transition.setProtectedTransition(false);
        transition.setUpdatedByEmployee(employee);

        WorkflowTransition saved = workflowTransitionRepository.save(transition);
        log.info("event=workflow_transition_created employeeId={} transitionId={} actionKey={} fromStatus={} toStatus={} result=created",
                employeeId,
                saved.getId(),
                saved.getActionKey(),
                saved.getFromStatus(),
                saved.getToStatus());
        return WorkflowTransitionResponse.from(saved);
    }

    @Transactional
    public WorkflowTransitionResponse updateTransition(
            Long id,
            UpdateWorkflowTransitionRequest request,
            String employeeId
    ) {
        WorkflowTransition transition = workflowTransitionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow transition not found"));

        validateWorkflowActionKey(transition.getActionKey());
        if (isTerminalStatus(transition.getFromStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Terminal workflow transitions cannot be updated");
        }

        if (request.getActive() != null) {
            transition.setActive(request.getActive());
        }

        if (request.getDisplayName() != null) {
            String displayName = request.getDisplayName().trim();
            if (displayName.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name cannot be blank");
            }
            transition.setDisplayName(displayName);
        }

        if (request.getSortOrder() != null) {
            transition.setSortOrder(request.getSortOrder());
        }

        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid employee"));
        transition.setUpdatedByEmployee(employee);

        WorkflowTransition saved = workflowTransitionRepository.save(transition);
        log.info("event=workflow_transition_updated employeeId={} transitionId={} actionKey={} fromStatus={} toStatus={} active={}",
                employeeId,
                saved.getId(),
                saved.getActionKey(),
                saved.getFromStatus(),
                saved.getToStatus(),
                saved.isActive());
        return WorkflowTransitionResponse.from(saved);
    }

    private void validateWorkflowActionKey(AccessKey actionKey) {
        if (!WORKFLOW_ACTION_KEYS.contains(actionKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid workflow action key");
        }
    }

    private boolean isTerminalStatus(TicketStatus status) {
        return status == TicketStatus.COMPLETED || status == TicketStatus.CANCELLED;
    }

    private SafeTransitionOption findSafeTransitionOption(
            AccessKey actionKey,
            TicketStatus fromStatus,
            TicketStatus toStatus
    ) {
        TransitionKey key = new TransitionKey(actionKey, fromStatus, toStatus);
        return SAFE_TRANSITION_OPTIONS.stream()
                .filter(option -> option.key().equals(key))
                .findFirst()
                .orElse(null);
    }

    private WorkflowTransitionOptionResponse toOptionResponse(
            SafeTransitionOption option,
            WorkflowTransition transition
    ) {
        return new WorkflowTransitionOptionResponse(
                option.actionKey(),
                option.displayName(),
                option.fromStatus(),
                option.toStatus(),
                transition != null,
                transition == null ? null : transition.getId(),
                transition == null ? null : transition.isActive(),
                transition == null ? null : transition.getSortOrder(),
                transition == null ? null : transition.isSystemTransition(),
                transition == null ? null : transition.isProtectedTransition()
        );
    }

    private record SafeTransitionOption(
            AccessKey actionKey,
            String displayName,
            TicketStatus fromStatus,
            TicketStatus toStatus,
            Integer sortOrder
    ) {

        private TransitionKey key() {
            return new TransitionKey(actionKey, fromStatus, toStatus);
        }
    }

    private record TransitionKey(
            AccessKey actionKey,
            TicketStatus fromStatus,
            TicketStatus toStatus
    ) {
    }
}
