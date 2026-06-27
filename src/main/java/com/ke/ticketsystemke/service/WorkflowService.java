package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.CreateWorkflowTransitionRequest;
import com.ke.ticketsystemke.dto.UpdateWorkflowTransitionRequest;
import com.ke.ticketsystemke.dto.WorkflowTransitionOptionResponse;
import com.ke.ticketsystemke.dto.WorkflowTransitionOptionsResponse;
import com.ke.ticketsystemke.dto.WorkflowTransitionResponse;
import com.ke.ticketsystemke.config.CacheNames;
import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowTransition;
import com.ke.ticketsystemke.entity.WorkflowStatus;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.repository.WorkflowActionRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionRepository;
import com.ke.ticketsystemke.repository.WorkflowStatusRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    private final WorkflowStatusRepository workflowStatusRepository;
    private final WorkflowActionRepository workflowActionRepository;

    public WorkflowService(
            WorkflowTransitionRepository workflowTransitionRepository,
            EmployeeRepository employeeRepository,
            WorkflowStatusRepository workflowStatusRepository,
            WorkflowActionRepository workflowActionRepository
    ) {
        this.workflowTransitionRepository = workflowTransitionRepository;
        this.employeeRepository = employeeRepository;
        this.workflowStatusRepository = workflowStatusRepository;
        this.workflowActionRepository = workflowActionRepository;
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
                    .map(transition -> transition.isActive() && hasValidStatusMetadata(transition))
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
    @Cacheable(cacheNames = CacheNames.WORKFLOW_TRANSITIONS, key = "'all'")
    public List<WorkflowTransitionResponse> listTransitions() {
        return workflowTransitionRepository.findAllByOrderBySortOrderAscIdAsc()
                .stream()
                .map(WorkflowTransitionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheNames.WORKFLOW_TRANSITION_OPTIONS, key = "'safeOptions'")
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
    @CacheEvict(cacheNames = {
            CacheNames.WORKFLOW_TRANSITIONS,
            CacheNames.WORKFLOW_TRANSITION_OPTIONS
    }, allEntries = true)
    public WorkflowTransitionResponse createTransition(
            CreateWorkflowTransitionRequest request,
            String employeeId
    ) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow transition request is required");
        }
        String actionKey = validateTransitionActionKey(request.getActionKey());

        String displayName = request.getDisplayName() == null ? "" : request.getDisplayName().trim();
        if (displayName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Display name cannot be blank");
        }

        String lookupEmployeeId = employeeId == null ? "" : employeeId.trim();
        Employee employee = employeeRepository.findByEmployeeIdIgnoreCase(lookupEmployeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid employee"));

        WorkflowTransition transition;
        if (request.getFromStatusId() != null || request.getToStatusId() != null) {
            transition = buildCustomTransition(request, actionKey, displayName, employee);
        } else {
            transition = buildSafeTransition(request, actionKey, displayName, employee);
        }

        WorkflowTransition saved = workflowTransitionRepository.save(transition);
        log.info("event=workflow_transition_created employeeId={} transitionId={} actionKey={} fromStatus={} toStatus={} result=created",
                employeeId,
                saved.getId(),
                saved.getActionKey(),
                saved.getFromStatus(),
                saved.getToStatus());
        return WorkflowTransitionResponse.from(saved);
    }

    private WorkflowTransition buildSafeTransition(
            CreateWorkflowTransitionRequest request,
            String actionKey,
            String displayName,
            Employee employee
    ) {
        AccessKey systemActionKey = parseSystemWorkflowActionKey(actionKey);
        SafeTransitionOption safeOption = findSafeTransitionOption(
                systemActionKey,
                request.getFromStatus(),
                request.getToStatus()
        );

        if (safeOption == null) {
            log.warn("event=workflow_transition_create_rejected actionKey={} fromStatus={} toStatus={} result=unsupported",
                    actionKey,
                    request.getFromStatus(),
                    request.getToStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported workflow transition");
        }

        if (workflowTransitionRepository.findByActionKeyAndFromStatusAndToStatus(
                actionKey,
                request.getFromStatus(),
                request.getToStatus()
        ).isPresent()) {
            log.warn("event=workflow_transition_create_rejected actionKey={} fromStatus={} toStatus={} result=duplicate",
                    actionKey,
                    request.getFromStatus(),
                    request.getToStatus());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Workflow transition already exists");
        }

        WorkflowTransition transition = new WorkflowTransition();
        transition.setActionKey(actionKey);
        transition.setDisplayName(displayName);
        transition.setFromStatus(request.getFromStatus());
        transition.setToStatus(request.getToStatus());
        transition.setFromStatusRecord(resolveWorkflowStatusForTicketStatus(request.getFromStatus()));
        transition.setToStatusRecord(resolveWorkflowStatusForTicketStatus(request.getToStatus()));
        transition.setActive(request.getActive() == null || request.getActive());
        transition.setSortOrder(request.getSortOrder() == null ? safeOption.sortOrder() : request.getSortOrder());
        transition.setSystemTransition(false);
        transition.setProtectedTransition(false);
        transition.setUpdatedByEmployee(employee);
        return transition;
    }

    private WorkflowTransition buildCustomTransition(
            CreateWorkflowTransitionRequest request,
            String actionKey,
            String displayName,
            Employee employee
    ) {
        if (request.getFromStatusId() == null || request.getToStatusId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Custom workflow transitions require fromStatusId and toStatusId");
        }

        if (isProtectedFixedAction(actionKey)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Protected workflow action keys cannot be used for custom transitions");
        }

        workflowActionRepository.findByActionKey(actionKey)
                .filter(action -> action.isActive() && !action.isProtectedAction())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Active custom workflow action not found"));

        WorkflowStatus fromStatus = findActiveStatus(request.getFromStatusId(), "Source workflow status not found");
        WorkflowStatus toStatus = findActiveStatus(request.getToStatusId(), "Target workflow status not found");
        validateCustomTransitionStatusPair(fromStatus, toStatus);

        if (!workflowTransitionRepository.findAllByActionKeyAndFromStatusRecord_IdAndToStatusRecord_IdOrderByIdAsc(
                actionKey,
                fromStatus.getId(),
                toStatus.getId()
        ).isEmpty()) {
            log.warn("event=workflow_transition_create_rejected actionKey={} fromStatusId={} toStatusId={} result=duplicate",
                    actionKey,
                    fromStatus.getId(),
                    toStatus.getId());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Workflow transition already exists");
        }

        WorkflowTransition transition = new WorkflowTransition();
        transition.setActionKey(actionKey);
        transition.setDisplayName(displayName);
        transition.setFromStatus(fromStatus.getBehaviorBucket());
        transition.setToStatus(toStatus.getBehaviorBucket());
        transition.setFromStatusRecord(fromStatus);
        transition.setToStatusRecord(toStatus);
        transition.setActive(request.getActive() == null || request.getActive());
        transition.setSortOrder(request.getSortOrder());
        transition.setSystemTransition(false);
        transition.setProtectedTransition(false);
        transition.setUpdatedByEmployee(employee);
        return transition;
    }

    private WorkflowStatus findActiveStatus(Long statusId, String notFoundMessage) {
        WorkflowStatus status = workflowStatusRepository.findById(statusId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, notFoundMessage));
        if (!status.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow status is inactive");
        }
        return status;
    }

    private void validateCustomTransitionStatusPair(WorkflowStatus fromStatus, WorkflowStatus toStatus) {
        if (fromStatus.getBehaviorBucket() == null || toStatus.getBehaviorBucket() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow statuses require behavior buckets");
        }
        if (fromStatus.isTerminal() || isTerminalStatus(fromStatus.getBehaviorBucket())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Terminal workflow statuses cannot be transition sources");
        }
        if (toStatus.isTerminal() && toStatus.getBehaviorBucket() != TicketStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Terminal custom target statuses must use COMPLETED behavior bucket");
        }
        if (toStatus.isTerminal() && (toStatus.isSystemStatus() || toStatus.isProtectedStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Custom transitions cannot target protected terminal statuses");
        }
        if (!toStatus.isTerminal() && isTerminalStatus(toStatus.getBehaviorBucket())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Non-terminal custom target statuses cannot use terminal behavior buckets");
        }
    }

    private String validateTransitionActionKey(String rawActionKey) {
        String actionKey = rawActionKey == null ? "" : rawActionKey.trim().toUpperCase(java.util.Locale.ROOT);
        if (!actionKey.matches("^[A-Z0-9_]{2,60}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Action key must contain 2 to 60 uppercase letters, digits, or underscores");
        }
        return actionKey;
    }

    private AccessKey parseSystemWorkflowActionKey(String actionKey) {
        try {
            AccessKey accessKey = AccessKey.valueOf(actionKey);
            validateWorkflowActionKey(accessKey);
            return accessKey;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid workflow action key");
        }
    }

    private boolean isProtectedFixedAction(String actionKey) {
        return WORKFLOW_ACTION_KEYS.stream().anyMatch(accessKey -> accessKey.name().equals(actionKey));
    }

    @Transactional
    @CacheEvict(cacheNames = {
            CacheNames.WORKFLOW_TRANSITIONS,
            CacheNames.WORKFLOW_TRANSITION_OPTIONS
    }, allEntries = true)
    public WorkflowTransitionResponse updateTransition(
            Long id,
            UpdateWorkflowTransitionRequest request,
            String employeeId
    ) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow transition request is required");
        }
        WorkflowTransition transition = workflowTransitionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow transition not found"));

        validateTransitionStatusMetadata(transition);
        if (isTerminalStatus(transition.getFromStatus()) || Boolean.TRUE.equals(transition.getFromStatusRecord().isTerminal())) {
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

        String lookupEmployeeId = employeeId == null ? "" : employeeId.trim();
        Employee employee = employeeRepository.findByEmployeeIdIgnoreCase(lookupEmployeeId)
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

    private void validateWorkflowActionKey(String actionKey) {
        try {
            validateWorkflowActionKey(actionKey == null ? null : AccessKey.valueOf(actionKey));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid workflow action key");
        }
    }

    private boolean isTerminalStatus(TicketStatus status) {
        return status == TicketStatus.COMPLETED || status == TicketStatus.CANCELLED;
    }

    private WorkflowStatus resolveWorkflowStatusForTicketStatus(TicketStatus status) {
        WorkflowStatus workflowStatus = workflowStatusRepository.findByStatusKey(status.name())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Workflow status metadata missing for transition status " + status.name()
                ));

        if (!workflowStatus.isSystemStatus()
                || !workflowStatus.isProtectedStatus()
                || !workflowStatus.isActive()
                || workflowStatus.getBehaviorBucket() != status
                || !status.name().equals(workflowStatus.getStatusKey())) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Workflow status metadata is invalid for transition status " + status.name()
            );
        }

        return workflowStatus;
    }

    private boolean hasValidStatusMetadata(WorkflowTransition transition) {
        return isWorkflowStatusMetadataValid(transition.getFromStatusRecord(), transition.getFromStatus())
                && isWorkflowStatusMetadataValid(transition.getToStatusRecord(), transition.getToStatus());
    }

    private void validateTransitionStatusMetadata(WorkflowTransition transition) {
        if (!hasValidStatusMetadata(transition)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow transition status metadata is invalid");
        }
    }

    private boolean isWorkflowStatusMetadataValid(WorkflowStatus workflowStatus, TicketStatus expectedStatus) {
        return WorkflowStatusValidationHelper.isSystemStatusMetadataValid(workflowStatus, expectedStatus)
                || WorkflowStatusValidationHelper.evaluateCustomStatusExecutability(workflowStatus, true).executable()
                && workflowStatus.getBehaviorBucket() == expectedStatus;
    }

    private SafeTransitionOption findSafeTransitionOption(
            AccessKey actionKey,
            TicketStatus fromStatus,
            TicketStatus toStatus
    ) {
        TransitionKey key = new TransitionKey(actionKey == null ? null : actionKey.name(), fromStatus, toStatus);
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
            return new TransitionKey(actionKey.name(), fromStatus, toStatus);
        }
    }

    private record TransitionKey(
            String actionKey,
            TicketStatus fromStatus,
            TicketStatus toStatus
    ) {
    }
}
