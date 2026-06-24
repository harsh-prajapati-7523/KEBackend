package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.GenericTransitionExecutionRequest;
import com.ke.ticketsystemke.dto.GenericTransitionPreviewRequest;
import com.ke.ticketsystemke.dto.GenericTransitionPreviewResponse;
import com.ke.ticketsystemke.dto.TicketResponse;
import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.entity.Ticket;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowAction;
import com.ke.ticketsystemke.entity.WorkflowStatus;
import com.ke.ticketsystemke.entity.WorkflowTransition;
import com.ke.ticketsystemke.repository.TicketRepository;
import com.ke.ticketsystemke.repository.WorkflowActionRepository;
import com.ke.ticketsystemke.repository.WorkflowStatusRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GenericTransitionExecutorService {

    private static final String GENERIC_DISABLED_REASON = "Not available for this ticket.";
    private static final java.util.List<String> PROTECTED_FIXED_ACTION_KEYS = java.util.List.of(
            AccessKey.PICK_TICKET.name(),
            AccessKey.START_WORK.name(),
            AccessKey.COMPLETE_TICKET.name(),
            AccessKey.CANCEL_TICKET.name()
    );

    private final TicketRepository ticketRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final WorkflowActionRepository workflowActionRepository;
    private final WorkflowStatusRepository workflowStatusRepository;
    private final EffectiveStatusResolver effectiveStatusResolver;
    private final AccessService accessService;
    private final TicketChargeService ticketChargeService;
    private final TicketWorkflowHistoryService ticketWorkflowHistoryService;
    private final WorkflowTransitionRoleScopeValidator workflowTransitionRoleScopeValidator;
    private final WorkflowTransitionCategoryScopeValidator workflowTransitionCategoryScopeValidator;

    public GenericTransitionExecutorService(
            TicketRepository ticketRepository,
            WorkflowTransitionRepository workflowTransitionRepository,
            WorkflowActionRepository workflowActionRepository,
            WorkflowStatusRepository workflowStatusRepository,
            EffectiveStatusResolver effectiveStatusResolver,
            AccessService accessService,
            TicketChargeService ticketChargeService,
            TicketWorkflowHistoryService ticketWorkflowHistoryService,
            WorkflowTransitionRoleScopeValidator workflowTransitionRoleScopeValidator,
            WorkflowTransitionCategoryScopeValidator workflowTransitionCategoryScopeValidator
    ) {
        this.ticketRepository = ticketRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
        this.workflowActionRepository = workflowActionRepository;
        this.workflowStatusRepository = workflowStatusRepository;
        this.effectiveStatusResolver = effectiveStatusResolver;
        this.accessService = accessService;
        this.ticketChargeService = ticketChargeService;
        this.ticketWorkflowHistoryService = ticketWorkflowHistoryService;
        this.workflowTransitionRoleScopeValidator = workflowTransitionRoleScopeValidator;
        this.workflowTransitionCategoryScopeValidator = workflowTransitionCategoryScopeValidator;
    }

    @Transactional(readOnly = true)
    public GenericTransitionPreviewResponse previewTransition(
            Long ticketId,
            Long workflowTransitionId,
            String employeeId,
            GenericTransitionPreviewRequest request
    ) {
        try {
            GenericTransitionExecutionPlan plan = prepareExecution(ticketId, workflowTransitionId, employeeId);
            WorkflowTransition transition = plan.transition();
            WorkflowAction action = plan.action();
            WorkflowStatus fromStatus = plan.fromStatus();
            WorkflowStatus toStatus = plan.toStatus();

            return new GenericTransitionPreviewResponse(
                    ticketId,
                    workflowTransitionId,
                    true,
                    action.getActionKey(),
                    action.getDisplayName(),
                    transition.getFromStatus().name(),
                    transition.getToStatus().name(),
                    fromStatus.getDisplayName(),
                    toStatus.getDisplayName(),
                    action.isRequiresComment(),
                    false,
                    null
            );
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw ex;
            }
            return GenericTransitionPreviewResponse.denied(ticketId, workflowTransitionId, GENERIC_DISABLED_REASON);
        }
    }

    @Transactional
    public TicketResponse executeTransition(
            Long ticketId,
            Long workflowTransitionId,
            String employeeId,
            GenericTransitionExecutionRequest request
    ) {
        GenericTransitionExecutionPlan plan = prepareExecution(ticketId, workflowTransitionId, employeeId);
        Ticket ticket = plan.ticket();
        WorkflowTransition transition = plan.transition();
        TicketStatus fromStatus = ticket.getStatus();
        Long fromStatusId = plan.fromStatus().getId();

        ticket.setStatus(plan.toStatusBehaviorBucket());
        ticket.setStatusRecord(plan.toStatus());

        Ticket saved = ticketRepository.save(ticket);
        ticketWorkflowHistoryService.recordSuccessfulGenericAction(
                saved,
                plan.action(),
                transition,
                fromStatus,
                plan.toStatusBehaviorBucket(),
                fromStatusId,
                plan.toStatus().getId(),
                employeeId,
                request == null ? null : request.getComment(),
                request == null ? null : request.getReason(),
                plan.systemTransition(),
                plan.customTransition()
        );

        return TicketResponse.from(
                saved,
                ticketChargeService.calculateTotalCharge(saved.getId()),
                effectiveStatusResolver.resolve(saved)
        );
    }

    @Transactional(readOnly = true)
    public GenericTransitionExecutionPlan prepareExecution(
            Long ticketId,
            Long workflowTransitionId,
            String employeeId
    ) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        ResolvedTicketStatus currentStatus = effectiveStatusResolver.resolve(ticket);
        WorkflowTransition transition = workflowTransitionRepository.findById(workflowTransitionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workflow transition not found"));
        WorkflowAction action = workflowActionRepository.findByActionKey(transition.getActionKey())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow action metadata missing"));
        WorkflowStatus fromStatus = resolveWorkflowStatus(transition.getFromStatusRecord(), transition.getFromStatus());
        WorkflowStatus toStatus = resolveWorkflowStatus(transition.getToStatusRecord(), transition.getToStatus());

        validateTransitionActive(transition);
        validateCurrentStatus(ticket, currentStatus, transition);
        TicketStatus toStatusBehaviorBucket = resolveTargetStatusBehaviorBucket(toStatus, transition.getToStatus());
        validateAction(action);
        accessService.requireAllowed(employeeId, action.getActionKey());
        validateTerminalProtection(currentStatus);
        validateTransitionEligibility(transition, action, fromStatus, toStatus, toStatusBehaviorBucket);
        workflowTransitionRoleScopeValidator.requireAllowed(transition, employeeId);
        workflowTransitionCategoryScopeValidator.requireAllowed(transition, ticket);

        return new GenericTransitionExecutionPlan(
                ticket,
                transition,
                action,
                fromStatus,
                toStatus,
                toStatusBehaviorBucket,
                currentStatus,
                employeeId,
                transition.isSystemTransition(),
                !transition.isSystemTransition() || isCustomTargetStatus(toStatus)
        );
    }

    private WorkflowStatus resolveWorkflowStatus(WorkflowStatus statusRecord, TicketStatus fallbackStatus) {
        if (isWorkflowStatusMetadataValid(statusRecord, fallbackStatus)
                || isCustomStatusMetadataValid(statusRecord, fallbackStatus)) {
            return statusRecord;
        }
        if (statusRecord != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow status metadata is invalid");
        }

        WorkflowStatus workflowStatus = workflowStatusRepository.findByStatusKey(fallbackStatus.name())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Workflow status metadata missing"
                ));
        if (!isWorkflowStatusMetadataValid(workflowStatus, fallbackStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow status metadata is invalid");
        }
        return workflowStatus;
    }

    private void validateTransitionActive(WorkflowTransition transition) {
        if (!transition.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow transition is not active");
        }
    }

    private void validateCurrentStatus(
            Ticket ticket,
            ResolvedTicketStatus currentStatus,
            WorkflowTransition transition
    ) {
        if (ticket.getStatus() == null || ticket.getStatus() != transition.getFromStatus()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket is not in the transition source status");
        }
        if (currentStatus.behaviorBucket() == null || currentStatus.behaviorBucket() != transition.getFromStatus()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket effective status does not match transition source status");
        }
        if (Boolean.FALSE.equals(currentStatus.actualStatusActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket workflow status is not active");
        }
    }

    private TicketStatus resolveTargetStatusBehaviorBucket(WorkflowStatus toStatus, TicketStatus targetStatus) {
        if (isWorkflowStatusMetadataValid(toStatus, targetStatus)) {
            return targetStatus;
        }

        WorkflowStatusValidationHelper.CustomStatusExecutability executability =
                WorkflowStatusValidationHelper.evaluateCustomStatusExecutability(toStatus, false);
        if (!executability.executable() || executability.behaviorBucket() != targetStatus) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target workflow status is not supported for generic execution");
        }
        return executability.behaviorBucket();
    }

    private void validateAction(WorkflowAction action) {
        if (!action.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow action is not active");
        }
    }

    private void validateTerminalProtection(ResolvedTicketStatus currentStatus) {
        if (Boolean.TRUE.equals(currentStatus.legacyTerminal())
                || Boolean.TRUE.equals(currentStatus.behaviorBucketTerminal())
                || Boolean.TRUE.equals(currentStatus.actualStatusTerminal())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Terminal tickets cannot be changed by generic execution");
        }
    }

    private void validateTransitionEligibility(
            WorkflowTransition transition,
            WorkflowAction action,
            WorkflowStatus fromStatus,
            WorkflowStatus toStatus,
            TicketStatus toStatusBehaviorBucket
    ) {
        if (transition.isProtectedTransition()
                || action.isProtectedAction()
                || PROTECTED_FIXED_ACTION_KEYS.contains(action.getActionKey())
                || fromStatus.isTerminal()
                || toStatus.isTerminal()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transition requires dedicated workflow handling");
        }
        if (requiresUnsupportedBusinessSideEffect(toStatusBehaviorBucket)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transition requires unsupported business side effects");
        }
    }

    private boolean requiresUnsupportedBusinessSideEffect(TicketStatus toStatus) {
        return toStatus == TicketStatus.PICKED
                || toStatus == TicketStatus.COMPLETED
                || toStatus == TicketStatus.CANCELLED;
    }

    private boolean isWorkflowStatusMetadataValid(WorkflowStatus workflowStatus, TicketStatus expectedStatus) {
        return WorkflowStatusValidationHelper.isSystemStatusMetadataValid(workflowStatus, expectedStatus);
    }

    private boolean isCustomStatusMetadataValid(WorkflowStatus workflowStatus, TicketStatus expectedStatus) {
        WorkflowStatusValidationHelper.CustomStatusExecutability executability =
                WorkflowStatusValidationHelper.evaluateCustomStatusExecutability(workflowStatus, false);
        return executability.executable() && executability.behaviorBucket() == expectedStatus;
    }

    private boolean isCustomTargetStatus(WorkflowStatus workflowStatus) {
        return workflowStatus != null
                && !workflowStatus.isSystemStatus()
                && !workflowStatus.isProtectedStatus();
    }

    public record GenericTransitionExecutionPlan(
            Ticket ticket,
            WorkflowTransition transition,
            WorkflowAction action,
            WorkflowStatus fromStatus,
            WorkflowStatus toStatus,
            TicketStatus toStatusBehaviorBucket,
            ResolvedTicketStatus currentStatus,
            String executedByEmployeeId,
            boolean systemTransition,
            boolean customTransition
    ) {
    }
}
