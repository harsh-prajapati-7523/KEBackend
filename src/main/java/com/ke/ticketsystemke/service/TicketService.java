package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.CancelTicketRequest;
import com.ke.ticketsystemke.dto.CompleteTicketRequest;
import com.ke.ticketsystemke.dto.CreateTicketDynamicValueRequest;
import com.ke.ticketsystemke.dto.CreateTicketRequest;
import com.ke.ticketsystemke.dto.CustomerHistoryResponse;
import com.ke.ticketsystemke.dto.CustomerHistoryTicketResponse;
import com.ke.ticketsystemke.dto.TicketActionAvailabilityResponse;
import com.ke.ticketsystemke.dto.TicketAvailableActionsResponse;
import com.ke.ticketsystemke.dto.TicketDynamicActionResponse;
import com.ke.ticketsystemke.dto.TicketDynamicValueResponse;
import com.ke.ticketsystemke.dto.TicketDynamicValuesResponse;
import com.ke.ticketsystemke.dto.TicketResponse;
import com.ke.ticketsystemke.dto.TicketStatusFilterOptionResponse;
import com.ke.ticketsystemke.entity.CategoryFieldConfig;
import com.ke.ticketsystemke.entity.DropdownOption;
import com.ke.ticketsystemke.entity.DropdownSource;
import com.ke.ticketsystemke.entity.ManufacturerStatus;
import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.entity.Ticket;
import com.ke.ticketsystemke.entity.TicketCategory;
import com.ke.ticketsystemke.entity.TicketCategoryConfig;
import com.ke.ticketsystemke.entity.TicketDynamicValue;
import com.ke.ticketsystemke.entity.TicketFieldDefinition;
import com.ke.ticketsystemke.entity.TicketFieldType;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WarrantyStatus;
import com.ke.ticketsystemke.entity.WorkflowStatus;
import com.ke.ticketsystemke.entity.WorkflowTransition;
import com.ke.ticketsystemke.repository.CategoryFieldConfigRepository;
import com.ke.ticketsystemke.repository.DropdownOptionRepository;
import com.ke.ticketsystemke.repository.TicketCategoryRepository;
import com.ke.ticketsystemke.repository.TicketDynamicValueRepository;
import com.ke.ticketsystemke.repository.TicketRepository;
import com.ke.ticketsystemke.repository.TicketSpecifications;
import com.ke.ticketsystemke.repository.WorkflowStatusRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int DYNAMIC_TEXT_MAX_LENGTH = 255;
    private static final int DYNAMIC_TEXTAREA_MAX_LENGTH = 1000;
    private static final int DEFAULT_TICKET_READ_SIZE = 100;
    private static final int MAX_TICKET_READ_SIZE = 100;
    private static final String ROLE_ACCESS_DENIED = "ROLE_ACCESS_DENIED";
    private static final String WORKFLOW_TRANSITION_INACTIVE = "WORKFLOW_TRANSITION_INACTIVE";
    private static final String STATUS_NOT_ALLOWED = "STATUS_NOT_ALLOWED";
    private static final String OWNER_REQUIRED = "OWNER_REQUIRED";
    private static final String ADMIN_REQUIRED = "ADMIN_REQUIRED";
    private static final String TERMINAL_STATUS = "TERMINAL_STATUS";
    private static final List<String> PROTECTED_FIXED_ACTION_KEYS = List.of(
            AccessKey.PICK_TICKET.name(),
            AccessKey.START_WORK.name(),
            AccessKey.COMPLETE_TICKET.name(),
            AccessKey.CANCEL_TICKET.name()
    );

    private final TicketRepository repository;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final CategoryFieldConfigRepository categoryFieldConfigRepository;
    private final DropdownOptionRepository dropdownOptionRepository;
    private final TicketDynamicValueRepository ticketDynamicValueRepository;
    private final TicketChargeService ticketChargeService;
    private final WorkflowService workflowService;
    private final AccessService accessService;
    private final WorkflowStatusRepository workflowStatusRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final EffectiveStatusResolver effectiveStatusResolver;
    private final GenericTransitionExecutorService genericTransitionExecutorService;
    private final TicketWorkflowHistoryService ticketWorkflowHistoryService;
    private final RepairWorkflowFeatureFlag repairWorkflowFeatureFlag;

    public TicketService(
            TicketRepository repository,
            TicketCategoryRepository ticketCategoryRepository,
            CategoryFieldConfigRepository categoryFieldConfigRepository,
            DropdownOptionRepository dropdownOptionRepository,
            TicketDynamicValueRepository ticketDynamicValueRepository,
            TicketChargeService ticketChargeService,
            WorkflowService workflowService,
            AccessService accessService,
            WorkflowStatusRepository workflowStatusRepository,
            WorkflowTransitionRepository workflowTransitionRepository,
            EffectiveStatusResolver effectiveStatusResolver,
            GenericTransitionExecutorService genericTransitionExecutorService,
            TicketWorkflowHistoryService ticketWorkflowHistoryService,
            RepairWorkflowFeatureFlag repairWorkflowFeatureFlag
    ) {
        this.repository = repository;
        this.ticketCategoryRepository = ticketCategoryRepository;
        this.categoryFieldConfigRepository = categoryFieldConfigRepository;
        this.dropdownOptionRepository = dropdownOptionRepository;
        this.ticketDynamicValueRepository = ticketDynamicValueRepository;
        this.ticketChargeService = ticketChargeService;
        this.workflowService = workflowService;
        this.accessService = accessService;
        this.workflowStatusRepository = workflowStatusRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
        this.effectiveStatusResolver = effectiveStatusResolver;
        this.genericTransitionExecutorService = genericTransitionExecutorService;
        this.ticketWorkflowHistoryService = ticketWorkflowHistoryService;
        this.repairWorkflowFeatureFlag = repairWorkflowFeatureFlag;
    }

    @Transactional
    public TicketResponse createTicket(
            CreateTicketRequest request,
            String createdByEmployeeId
    ) {
        // Business-level create log
        log.info("event=ticket_create_requested employeeId={}", createdByEmployeeId);

        TicketCategoryConfig category = resolveAssignableCategory(request.getCategoryId(), request.getCategory());
        List<DynamicValueDraft> dynamicValueDrafts = validateDynamicValues(category, request.getDynamicValues());

        Ticket ticket = new Ticket();
        ticket.setTicketNumber(formatTicketNumber(repository.getNextTicketNumberValue()));
        ticket.setCustomerName(request.getCustomerName().trim());
        ticket.setMobileNumber(request.getMobileNumber());
        ticket.setVillageOrArea(trimToNull(request.getVillageOrArea()));
        ticket.setProductType(request.getProductType().trim());
        assignCategory(ticket, category);
        ticket.setComplaintDescription(trimToEmpty(request.getComplaintDescription()));
        ticket.setStatus(TicketStatus.NEW);
        ticket.setStatusRecord(resolveWorkflowStatusForTicketStatus(TicketStatus.NEW));
        ticket.setCreatedByEmployeeId(createdByEmployeeId);
        ticket.setWarrantyStatus(WarrantyStatus.NOT_CHECKED);
        ticket.setManufacturerStatus(ManufacturerStatus.NOT_REQUIRED);

        Ticket saved = repository.save(ticket);
        saveDynamicValues(saved, dynamicValueDrafts);
        return toTicketResponse(saved, BigDecimal.ZERO.setScale(2));
    }

    @Transactional(readOnly = true, noRollbackFor = ResponseStatusException.class)
    public TicketAvailableActionsResponse getAvailableActions(
            Long ticketId,
            String employeeId,
            String role
    ) {
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        Map<AccessKey, TicketActionAvailabilityResponse> actions = new EnumMap<>(AccessKey.class);
        actions.put(AccessKey.PICK_TICKET, evaluatePickAvailability(ticket, employeeId));
        actions.put(AccessKey.START_WORK, evaluateStartWorkAvailability(ticket, employeeId));
        actions.put(AccessKey.COMPLETE_TICKET, evaluateCompleteAvailability(ticket, employeeId, role));
        actions.put(AccessKey.CANCEL_TICKET, evaluateCancelAvailability(ticket, employeeId, role));
        List<TicketDynamicActionResponse> dynamicActions = evaluateDynamicActions(ticket, employeeId);

        log.info("event=ticket_available_actions_returned employeeId={} ticketId={}", employeeId, ticketId);
        return new TicketAvailableActionsResponse(ticket.getId(), actions, dynamicActions);
    }

    @Transactional
    public TicketResponse pickTicket(
            Long ticketId,
            String employeeId
    ) {
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        TicketStatus status = ticket.getStatus();
        if (status == TicketStatus.NEW || status == TicketStatus.PICKED) {
            workflowService.requireTransitionAllowed(AccessKey.PICK_TICKET, status, TicketStatus.PICKED);
            String previousOwner = ticket.getPickedByEmployeeId();
            Long fromStatusId = resolveStatusRecordId(ticket);
            WorkflowStatus toStatusRecord = resolveWorkflowStatusForTicketStatus(TicketStatus.PICKED);
            ticket.setStatus(TicketStatus.PICKED);
            ticket.setStatusRecord(toStatusRecord);
            ticket.setPickedByEmployeeId(employeeId);
            Ticket saved = repository.save(ticket);
            ticketWorkflowHistoryService.recordSuccessfulFixedAction(
                    saved,
                    AccessKey.PICK_TICKET,
                    status,
                    TicketStatus.PICKED,
                    fromStatusId,
                    resolveStatusRecordId(toStatusRecord),
                    employeeId,
                    previousOwner,
                    employeeId,
                    null,
                    null
            );
            TicketResponse resp = toTicketResponse(saved, ticketChargeService.calculateTotalCharge(saved.getId()));
            log.info("event=ticket_picked ticketId={} previousOwner={} newOwner={}", ticketId, previousOwner, employeeId);
            return resp;
        }

        log.warn("event=invalid_status_transition ticketId={} status={} attemptedAction=pick", ticketId, status);
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Ticket cannot be picked in its current status"
        );
    }

    @Transactional
    public TicketResponse startWork(
            Long ticketId,
            String employeeId
    ) {
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        if (ticket.getStatus() == TicketStatus.PICKED) {
            workflowService.requireTransitionAllowed(AccessKey.START_WORK, TicketStatus.PICKED, TicketStatus.IN_PROGRESS);
            Long fromStatusId = resolveStatusRecordId(ticket);
            WorkflowStatus toStatusRecord = resolveWorkflowStatusForTicketStatus(TicketStatus.IN_PROGRESS);
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticket.setStatusRecord(toStatusRecord);
            String previousOwner = ticket.getPickedByEmployeeId();
            ticket.setPickedByEmployeeId(employeeId);
            Ticket saved = repository.save(ticket);
            ticketWorkflowHistoryService.recordSuccessfulFixedAction(
                    saved,
                    AccessKey.START_WORK,
                    TicketStatus.PICKED,
                    TicketStatus.IN_PROGRESS,
                    fromStatusId,
                    resolveStatusRecordId(toStatusRecord),
                    employeeId,
                    previousOwner,
                    employeeId,
                    null,
                    null
            );
            TicketResponse resp = toTicketResponse(saved, ticketChargeService.calculateTotalCharge(saved.getId()));
            log.info("event=ticket_started ticketId={} previousOwner={} employeeId={} statusTransition=PICKED->IN_PROGRESS", ticketId, previousOwner, employeeId);
            return resp;
        }

        log.warn("event=invalid_status_transition ticketId={} status={} attemptedAction=start-work", ticketId, ticket.getStatus());
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Ticket can only be started from PICKED status"
        );
    }

    @Transactional
    public TicketResponse completeTicket(
            Long ticketId,
            CompleteTicketRequest request,
            String employeeId,
            String role
    ) {
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        if (ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            log.warn("event=invalid_status_transition ticketId={} status={} attemptedAction=complete", ticketId, ticket.getStatus());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ticket can only be completed from IN_PROGRESS status"
            );
        }

        workflowService.requireTransitionAllowed(AccessKey.COMPLETE_TICKET, TicketStatus.IN_PROGRESS, TicketStatus.COMPLETED);

        if (!isAdminRole(role) && !employeeId.equals(ticket.getPickedByEmployeeId())) {
            log.warn("event=completion_denied ticketId={} status={} employeeId={} role={}", ticketId, ticket.getStatus(), employeeId, role);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Not authorized to complete this ticket"
            );
        }

        String owner = ticket.getPickedByEmployeeId();
        Long fromStatusId = resolveStatusRecordId(ticket);
        WorkflowStatus toStatusRecord = resolveWorkflowStatusForTicketStatus(TicketStatus.COMPLETED);
        ticket.setStatus(TicketStatus.COMPLETED);
        ticket.setStatusRecord(toStatusRecord);
        ticket.setCompletedAt(Instant.now());
        ticket.setCompletedByEmployeeId(employeeId);
        ticket.setCompletionRemark(trimToNull(request.getCompletionRemark()));

        Ticket saved = repository.save(ticket);
        ticketWorkflowHistoryService.recordSuccessfulFixedAction(
                saved,
                AccessKey.COMPLETE_TICKET,
                TicketStatus.IN_PROGRESS,
                TicketStatus.COMPLETED,
                fromStatusId,
                resolveStatusRecordId(toStatusRecord),
                employeeId,
                owner,
                owner,
                saved.getCompletionRemark(),
                null
        );
        TicketResponse resp = toTicketResponse(saved, ticketChargeService.calculateTotalCharge(saved.getId()));
        log.info("event=ticket_completed ticketId={} ticketNumber={} employeeId={} statusTransition=IN_PROGRESS->COMPLETED", ticketId, ticket.getTicketNumber(), employeeId);
        return resp;
    }

    @Transactional
    public TicketResponse cancelTicket(
            Long ticketId,
            CancelTicketRequest request,
            String employeeId,
            String role
    ) {
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        if (ticket.getStatus() == TicketStatus.COMPLETED || ticket.getStatus() == TicketStatus.CANCELLED) {
            log.warn("event=invalid_status_transition ticketId={} status={} attemptedAction=cancel", ticketId, ticket.getStatus());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ticket cannot be cancelled in its current status"
            );
        }

        if (!isAdminRole(role)) {
            log.warn("event=cancellation_denied ticketId={} status={} employeeId={} role={}", ticketId, ticket.getStatus(), employeeId, role);
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Not authorized to cancel this ticket"
            );
        }

        TicketStatus previousStatus = ticket.getStatus();
        String previousOwner = ticket.getPickedByEmployeeId();
        Long fromStatusId = resolveStatusRecordId(ticket);
        workflowService.requireTransitionAllowed(AccessKey.CANCEL_TICKET, previousStatus, TicketStatus.CANCELLED);
        WorkflowStatus toStatusRecord = resolveWorkflowStatusForTicketStatus(TicketStatus.CANCELLED);
        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setStatusRecord(toStatusRecord);
        ticket.setCancelledAt(Instant.now());
        ticket.setCancelledByEmployeeId(employeeId);
        ticket.setCancellationReason(request.getCancellationReason().trim());

        Ticket saved = repository.save(ticket);
        ticketWorkflowHistoryService.recordSuccessfulFixedAction(
                saved,
                AccessKey.CANCEL_TICKET,
                previousStatus,
                TicketStatus.CANCELLED,
                fromStatusId,
                resolveStatusRecordId(toStatusRecord),
                employeeId,
                previousOwner,
                null,
                null,
                saved.getCancellationReason()
        );
        TicketResponse resp = toTicketResponse(saved, ticketChargeService.calculateTotalCharge(saved.getId()));
        log.info("event=ticket_cancelled ticketId={} ticketNumber={} employeeId={} statusTransition={}->CANCELLED", ticketId, ticket.getTicketNumber(), employeeId, previousStatus);
        return resp;
    }

    private Long resolveStatusRecordId(Ticket ticket) {
        return ticket.getStatusRecord() == null ? null : ticket.getStatusRecord().getId();
    }

    private Long resolveStatusRecordId(WorkflowStatus workflowStatus) {
        return workflowStatus == null ? null : workflowStatus.getId();
    }

    private WorkflowStatus resolveWorkflowStatusForTicketStatus(TicketStatus status) {
        WorkflowStatus workflowStatus = workflowStatusRepository.findByStatusKey(status.name())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Workflow status metadata missing for ticket status " + status.name()
                ));

        if (!workflowStatus.isSystemStatus()
                || !workflowStatus.isProtectedStatus()
                || !workflowStatus.isActive()
                || !status.name().equals(workflowStatus.getStatusKey())) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Workflow status metadata is invalid for ticket status " + status.name()
            );
        }

        return workflowStatus;
    }

    private boolean isAdminRole(String role) {
        return "SUPER_ADMIN".equals(role) || "ADMIN".equals(role);
    }

    private boolean isSuperAdminRole(String role) {
        return "SUPER_ADMIN".equals(role);
    }

    private boolean isOperationalRole(String role) {
        return isAdminRole(role) || "EMPLOYEE".equals(role) || "TECHNICIAN".equals(role);
    }

    private List<TicketDynamicActionResponse> evaluateDynamicActions(Ticket ticket, String employeeId) {
        Long currentStatusId = resolveStatusRecordId(ticket);
        if (currentStatusId == null) {
            log.warn(
                    "event=ticket_dynamic_actions_skipped ticketId={} status={} reason=status_record_missing",
                    ticket.getId(),
                    ticket.getStatus()
            );
            return List.of();
        }

        List<TicketDynamicActionResponse> dynamicActions = new ArrayList<>();
        Set<String> seenTransitionKeys = new LinkedHashSet<>();
        List<WorkflowTransition> candidates = workflowTransitionRepository
                .findByFromStatusRecord_IdAndActiveTrueOrderBySortOrderAscIdAsc(currentStatusId);
        for (WorkflowTransition candidate : candidates) {
            if (isProtectedFixedAction(candidate.getActionKey())) {
                continue;
            }
            if (isDisabledRepairWorkflowAction(candidate)) {
                log.info(
                        "event=ticket_dynamic_action_skipped ticketId={} transitionId={} actionKey={} reason=repair_workflow_disabled",
                        ticket.getId(),
                        candidate.getId(),
                        candidate.getActionKey()
                );
                continue;
            }
            if (isLegacySystemOnlyDynamicTransition(candidate)) {
                log.warn(
                        "event=ticket_dynamic_action_skipped ticketId={} transitionId={} actionKey={} currentStatusId={} reason=legacy_system_only_dynamic_transition",
                        ticket.getId(),
                        candidate.getId(),
                        candidate.getActionKey(),
                        currentStatusId
                );
                continue;
            }
            if (!isExactCurrentStatusTransition(candidate, currentStatusId)) {
                log.warn(
                        "event=ticket_dynamic_action_skipped ticketId={} transitionId={} actionKey={} currentStatusId={} fromStatusId={} reason=from_status_mismatch",
                        ticket.getId(),
                        candidate.getId(),
                        candidate.getActionKey(),
                        currentStatusId,
                        resolveStatusRecordId(candidate.getFromStatusRecord())
                );
                continue;
            }
            String transitionKey = dynamicTransitionKey(candidate);
            if (!seenTransitionKeys.add(transitionKey)) {
                log.warn(
                        "event=ticket_dynamic_action_skipped ticketId={} transitionId={} actionKey={} currentStatusId={} reason=duplicate_transition_key",
                        ticket.getId(),
                        candidate.getId(),
                        candidate.getActionKey(),
                        currentStatusId
                );
                continue;
            }
            try {
                GenericTransitionExecutorService.GenericTransitionExecutionPlan plan =
                        genericTransitionExecutorService.prepareExecution(ticket.getId(), candidate.getId(), employeeId);
                dynamicActions.add(toDynamicActionResponse(plan));
            } catch (ResponseStatusException ex) {
                log.info(
                        "event=ticket_dynamic_action_hidden ticketId={} transitionId={} actionKey={} status={} reason={}",
                        ticket.getId(),
                        candidate.getId(),
                        candidate.getActionKey(),
                        ex.getStatusCode(),
                        ex.getReason()
                );
            }
        }
        return dynamicActions;
    }

    private boolean isExactCurrentStatusTransition(WorkflowTransition transition, Long currentStatusId) {
        Long fromStatusId = transition == null ? null : resolveStatusRecordId(transition.getFromStatusRecord());
        return currentStatusId != null && currentStatusId.equals(fromStatusId);
    }

    private boolean isDisabledRepairWorkflowAction(WorkflowTransition transition) {
        return !repairWorkflowFeatureFlag.isEnabled()
                && transition != null
                && repairWorkflowFeatureFlag.isRepairWorkflowAction(transition.getActionKey());
    }

    private boolean isLegacySystemOnlyDynamicTransition(WorkflowTransition transition) {
        return isProtectedSystemStatus(transition.getFromStatusRecord())
                && isProtectedSystemStatus(transition.getToStatusRecord());
    }

    private boolean isProtectedSystemStatus(WorkflowStatus status) {
        return status != null && status.isSystemStatus() && status.isProtectedStatus();
    }

    private String dynamicTransitionKey(WorkflowTransition transition) {
        return String.join(
                "|",
                transition.getActionKey() == null ? "" : transition.getActionKey(),
                String.valueOf(resolveStatusRecordId(transition.getFromStatusRecord())),
                String.valueOf(resolveStatusRecordId(transition.getToStatusRecord()))
        );
    }

    private boolean isProtectedFixedAction(String actionKey) {
        return actionKey != null && PROTECTED_FIXED_ACTION_KEYS.contains(actionKey);
    }

    private TicketDynamicActionResponse toDynamicActionResponse(
            GenericTransitionExecutorService.GenericTransitionExecutionPlan plan
    ) {
        WorkflowTransition transition = plan.transition();
        return new TicketDynamicActionResponse(
                transition.getId(),
                plan.action().getActionKey(),
                plan.action().getDisplayName(),
                transition.getFromStatus().name(),
                transition.getToStatus().name(),
                true
        );
    }

    private TicketActionAvailabilityResponse evaluatePickAvailability(Ticket ticket, String employeeId) {
        TicketStatus status = ticket.getStatus();
        TicketStatus targetStatus = status == TicketStatus.NEW || status == TicketStatus.PICKED
                ? TicketStatus.PICKED
                : null;
        return evaluateWorkflowAvailability(
                ticket,
                employeeId,
                AccessKey.PICK_TICKET,
                targetStatus,
                null
        );
    }

    private TicketActionAvailabilityResponse evaluateStartWorkAvailability(Ticket ticket, String employeeId) {
        TicketStatus targetStatus = ticket.getStatus() == TicketStatus.PICKED
                ? TicketStatus.IN_PROGRESS
                : null;
        return evaluateWorkflowAvailability(
                ticket,
                employeeId,
                AccessKey.START_WORK,
                targetStatus,
                null
        );
    }

    private TicketActionAvailabilityResponse evaluateCompleteAvailability(Ticket ticket, String employeeId, String role) {
        TicketActionAvailabilityResponse baseAvailability = evaluateWorkflowAvailability(
                ticket,
                employeeId,
                AccessKey.COMPLETE_TICKET,
                ticket.getStatus() == TicketStatus.IN_PROGRESS ? TicketStatus.COMPLETED : null,
                null
        );
        if (!baseAvailability.available()) {
            return baseAvailability;
        }

        boolean isTicketOwner = employeeId != null && employeeId.equals(ticket.getPickedByEmployeeId());
        if (!isAdminRole(role) && !isTicketOwner) {
            return unavailable(
                    OWNER_REQUIRED,
                    "Only the assigned technician or admin can complete this ticket."
            );
        }

        return available();
    }

    private TicketActionAvailabilityResponse evaluateCancelAvailability(Ticket ticket, String employeeId, String role) {
        TicketActionAvailabilityResponse baseAvailability = evaluateWorkflowAvailability(
                ticket,
                employeeId,
                AccessKey.CANCEL_TICKET,
                canCancelFromStatus(ticket.getStatus()) ? TicketStatus.CANCELLED : null,
                null
        );
        if (!baseAvailability.available()) {
            return baseAvailability;
        }

        if (!isAdminRole(role)) {
            return unavailable(
                    ADMIN_REQUIRED,
                    "Only an admin can cancel this ticket."
            );
        }

        return available();
    }

    private TicketActionAvailabilityResponse evaluateWorkflowAvailability(
            Ticket ticket,
            String employeeId,
            AccessKey actionKey,
            TicketStatus targetStatus,
            String statusMessage
    ) {
        if (!accessService.isAllowed(employeeId, actionKey)) {
            return unavailable(
                    ROLE_ACCESS_DENIED,
                    "You do not have access to perform this action."
            );
        }

        TicketStatus status = ticket.getStatus();
        if (status == TicketStatus.COMPLETED || status == TicketStatus.CANCELLED) {
            return unavailable(
                    TERMINAL_STATUS,
                    "Completed and cancelled tickets are terminal."
            );
        }

        if (targetStatus == null) {
            return unavailable(
                    STATUS_NOT_ALLOWED,
                    statusMessage != null ? statusMessage : "This action is not available for the current ticket status."
            );
        }

        if (!workflowService.isTransitionAllowed(actionKey, status, targetStatus)) {
            return unavailable(
                    WORKFLOW_TRANSITION_INACTIVE,
                    "This action is disabled for the current workflow status."
            );
        }

        return available();
    }

    private boolean canCancelFromStatus(TicketStatus status) {
        return status == TicketStatus.NEW || status == TicketStatus.PICKED || status == TicketStatus.IN_PROGRESS;
    }

    private int compareStatusFilterOptions(
            TicketStatusFilterOptionResponse first,
            TicketStatusFilterOptionResponse second,
            Map<String, Integer> enumOrderByKey
    ) {
        Integer firstSortOrder = first.sortOrder();
        Integer secondSortOrder = second.sortOrder();
        if (firstSortOrder != null && secondSortOrder != null) {
            int sortOrderComparison = firstSortOrder.compareTo(secondSortOrder);
            if (sortOrderComparison != 0) {
                return sortOrderComparison;
            }
            return first.statusKey().compareTo(second.statusKey());
        }
        if (firstSortOrder != null) {
            return -1;
        }
        if (secondSortOrder != null) {
            return 1;
        }
        return Integer.compare(
                enumOrderByKey.getOrDefault(first.statusKey(), Integer.MAX_VALUE),
                enumOrderByKey.getOrDefault(second.statusKey(), Integer.MAX_VALUE)
        );
    }

    private String formatTicketStatusLabel(TicketStatus status) {
        String[] words = status.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder displayName = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (displayName.length() > 0) {
                displayName.append(' ');
            }
            displayName.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                displayName.append(word.substring(1));
            }
        }
        return displayName.toString();
    }

    private boolean isTerminalTicketStatus(TicketStatus status) {
        return status == TicketStatus.COMPLETED || status == TicketStatus.CANCELLED;
    }

    private TicketActionAvailabilityResponse available() {
        return new TicketActionAvailabilityResponse(true, null, null);
    }

    private TicketActionAvailabilityResponse unavailable(String reasonCode, String message) {
        return new TicketActionAvailabilityResponse(false, reasonCode, message);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listTickets() {
        return listTickets(null, null);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listTickets(Integer page, Integer size) {
        PageRequest pageRequest = ticketReadPageRequest(page, size);
        List<Ticket> tickets = repository.findAllByOrderByCreatedAtDesc(pageRequest);
        List<TicketResponse> list = toTicketResponses(tickets);
        log.info("event=ticket_list_returned count={}", list.size());
        return list;
    }

    @Transactional(readOnly = true)
    public TicketResponse getTicket(Long ticketId) {
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));
        return toTicketResponse(ticket, ticketChargeService.calculateTotalCharge(ticket));
    }

    @Transactional(readOnly = true)
    public List<TicketStatusFilterOptionResponse> getTicketStatusFilterOptions(String employeeId) {
        List<TicketStatus> ticketStatuses = Arrays.asList(TicketStatus.values());
        List<String> statusKeys = ticketStatuses.stream()
                .map(Enum::name)
                .toList();
        Map<String, WorkflowStatus> statusesByKey = new HashMap<>();
        for (WorkflowStatus workflowStatus : workflowStatusRepository.findAllByStatusKeyInAndSystemStatusTrueAndProtectedStatusTrue(statusKeys)) {
            statusesByKey.put(workflowStatus.getStatusKey(), workflowStatus);
        }

        Map<String, Integer> enumOrderByKey = new HashMap<>();
        List<TicketStatusFilterOptionResponse> options = new ArrayList<>();
        for (TicketStatus status : ticketStatuses) {
            enumOrderByKey.put(status.name(), status.ordinal());
            WorkflowStatus workflowStatus = statusesByKey.get(status.name());
            options.add(new TicketStatusFilterOptionResponse(
                    status.name(),
                    workflowStatus != null ? workflowStatus.getDisplayName() : formatTicketStatusLabel(status),
                    workflowStatus == null || workflowStatus.isActive(),
                    workflowStatus == null ? null : workflowStatus.getSortOrder(),
                    workflowStatus != null ? workflowStatus.isTerminal() : isTerminalTicketStatus(status)
            ));
        }

        options.sort((first, second) -> compareStatusFilterOptions(first, second, enumOrderByKey));
        log.info("event=ticket_status_filter_options_returned employeeId={} optionCount={}", employeeId, options.size());
        return options;
    }

    @Transactional(readOnly = true)
    public CustomerHistoryResponse getCustomerHistory(Long ticketId, String employeeId) {
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        List<Ticket> historyTickets = repository
                .findTop10ByMobileNumberAndIdNotOrderByCreatedAtDesc(ticket.getMobileNumber(), ticketId);
        Map<Long, BigDecimal> totalsByTicketId = ticketChargeService.calculateTotalChargesByTicketIds(
                historyTickets.stream()
                        .map(Ticket::getId)
                        .toList()
        );
        List<CustomerHistoryTicketResponse> tickets = historyTickets.stream()
                .map(historyTicket -> CustomerHistoryTicketResponse.from(
                        historyTicket,
                        totalsByTicketId.getOrDefault(historyTicket.getId(), BigDecimal.ZERO.setScale(2))
                ))
                .toList();

        log.info("event=customer_history_returned employeeId={} ticketId={} resultCount={}",
                employeeId, ticketId, tickets.size());
        return new CustomerHistoryResponse(tickets.size(), tickets);
    }

    @Transactional(readOnly = true)
    public TicketDynamicValuesResponse getDynamicValues(Long ticketId, String employeeId) {
        if (!repository.existsById(ticketId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found");
        }

        List<TicketDynamicValueResponse> dynamicValues = ticketDynamicValueRepository
                .findByTicketIdOrderByCreatedAtAscIdAsc(ticketId)
                .stream()
                .map(TicketDynamicValueResponse::from)
                .toList();

        log.info("event=ticket_dynamic_values_returned employeeId={} ticketId={} resultCount={}",
                employeeId, ticketId, dynamicValues.size());
        return new TicketDynamicValuesResponse(ticketId, dynamicValues);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> searchTickets(String query) {
        return searchTickets(query, null, null);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> searchTickets(String query, Integer page, Integer size) {
        String normalizedQuery = normalizeSearchQuery(query);
        if (normalizedQuery == null) {
            return Collections.emptyList();
        }

        PageRequest pageRequest = ticketReadPageRequest(page, size);
        List<Ticket> tickets = repository.searchTickets(
                escapeLikeWildcards(normalizedQuery),
                pageRequest.getPageSize(),
                offset(pageRequest)
        );
        return toTicketResponses(tickets);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> queryTickets(
            String search,
            String status,
            String category,
            String createdFrom,
            String createdTo,
            String mine,
            String employeeId
    ) {
        return queryTickets(search, status, category, createdFrom, createdTo, mine, employeeId, null, null);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> queryTickets(
            String search,
            String status,
            String category,
            String createdFrom,
            String createdTo,
            String mine,
            String employeeId,
            Integer page,
            Integer size
    ) {
        String normalizedSearch = normalizeSearchQuery(search);
        TicketStatus parsedStatus = parseEnum(TicketStatus.class, status, "status");
        TicketCategory parsedCategory = parseEnum(TicketCategory.class, category, "category");
        LocalDate parsedCreatedFrom = parseDate(createdFrom, "createdFrom");
        LocalDate parsedCreatedTo = parseDate(createdTo, "createdTo");
        boolean parsedMine = parseBoolean(mine, "mine");

        if (parsedCreatedFrom != null && parsedCreatedTo != null && parsedCreatedFrom.isAfter(parsedCreatedTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "createdFrom must not be after createdTo");
        }

        Instant createdFromInclusive = atStartOfBusinessDay(parsedCreatedFrom);
        Instant createdToExclusive = parsedCreatedTo == null ? null : atStartOfBusinessDay(nextDay(parsedCreatedTo));
        PageRequest pageRequest = ticketReadPageRequest(page, size);

        List<Ticket> tickets = repository.findAll(
                        TicketSpecifications.queryTickets(
                                normalizedSearch == null ? null : escapeLikeWildcards(normalizedSearch),
                                parsedStatus,
                                parsedCategory,
                                createdFromInclusive,
                                createdToExclusive,
                                parsedMine ? employeeId : null
                        ),
                        pageRequest
                )
                .getContent();
        return toTicketResponses(tickets);
    }

    private TicketResponse toTicketResponse(Ticket ticket, BigDecimal totalCharge) {
        return TicketResponse.from(ticket, totalCharge, effectiveStatusResolver.resolve(ticket));
    }

    private List<TicketResponse> toTicketResponses(List<Ticket> tickets) {
        Map<Long, BigDecimal> totalsByTicketId = ticketChargeService.calculateTotalChargesByTicketIds(
                tickets.stream()
                        .map(Ticket::getId)
                        .toList()
        );
        return tickets.stream()
                .map(ticket -> toTicketResponse(ticket, totalsByTicketId.getOrDefault(ticket.getId(), BigDecimal.ZERO.setScale(2))))
                .toList();
    }

    private PageRequest ticketReadPageRequest(Integer page, Integer size) {
        return PageRequest.of(
                normalizePage(page),
                normalizeTicketReadSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    private int normalizeTicketReadSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_TICKET_READ_SIZE;
        }
        return Math.min(size, MAX_TICKET_READ_SIZE);
    }

    private int offset(PageRequest pageRequest) {
        long offset = pageRequest.getOffset();
        return offset > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) offset;
    }

    private List<DynamicValueDraft> validateDynamicValues(
            TicketCategoryConfig category,
            List<CreateTicketDynamicValueRequest> requestedValues
    ) {
        List<CategoryFieldConfig> configs = categoryFieldConfigRepository.findRenderableFormFieldsByCategoryId(category.getId());
        Map<Long, CategoryFieldConfig> configsById = new HashMap<>();
        for (CategoryFieldConfig config : configs) {
            configsById.put(config.getId(), config);
        }

        List<DynamicValueDraft> drafts = new ArrayList<>();
        Map<Long, Boolean> submittedFieldDefinitionIds = new HashMap<>();

        for (CreateTicketDynamicValueRequest valueRequest : requestedValues == null ? Collections.<CreateTicketDynamicValueRequest>emptyList() : requestedValues) {
            if (valueRequest == null
                    || valueRequest.getCategoryFieldConfigId() == null
                    || valueRequest.getFieldDefinitionId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid dynamic field value");
            }

            CategoryFieldConfig config = configsById.get(valueRequest.getCategoryFieldConfigId());
            if (config == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dynamic field is not configured for this category");
            }

            TicketFieldDefinition fieldDefinition = config.getFieldDefinition();
            if (!fieldDefinition.getId().equals(valueRequest.getFieldDefinitionId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dynamic field does not match its category configuration");
            }

            if (submittedFieldDefinitionIds.put(fieldDefinition.getId(), Boolean.TRUE) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate dynamic field value submitted");
            }

            DynamicValueDraft draft = validateDynamicValue(config, valueRequest.getValue());
            if (draft != null) {
                drafts.add(draft);
            }
        }

        for (CategoryFieldConfig config : configs) {
            if (config.isRequired() && !submittedFieldDefinitionIds.containsKey(config.getFieldDefinition().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, config.getFieldDefinition().getDisplayName() + " is required");
            }
        }

        return drafts;
    }

    private DynamicValueDraft validateDynamicValue(CategoryFieldConfig config, String rawValue) {
        TicketFieldDefinition fieldDefinition = config.getFieldDefinition();
        TicketFieldType fieldType = fieldDefinition.getFieldType();
        String trimmedValue = trimToNull(rawValue);

        if (trimmedValue == null) {
            if (config.isRequired()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldDefinition.getDisplayName() + " is required");
            }
            return null;
        }

        if (fieldType == TicketFieldType.TEXT) {
            validateMaxLength(trimmedValue, DYNAMIC_TEXT_MAX_LENGTH, fieldDefinition.getDisplayName());
            return new DynamicValueDraft(config, trimmedValue, null, trimmedValue);
        }

        if (fieldType == TicketFieldType.TEXTAREA) {
            validateMaxLength(trimmedValue, DYNAMIC_TEXTAREA_MAX_LENGTH, fieldDefinition.getDisplayName());
            return new DynamicValueDraft(config, trimmedValue, null, trimmedValue);
        }

        if (fieldType == TicketFieldType.NUMBER) {
            BigDecimal numberValue = parseDynamicNumber(trimmedValue, fieldDefinition.getDisplayName());
            return new DynamicValueDraft(config, null, numberValue, numberValue.toPlainString());
        }

        if (fieldType == TicketFieldType.DROPDOWN) {
            DropdownSource dropdownSource = fieldDefinition.getDropdownSource();
            if (dropdownSource == null || !dropdownSource.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldDefinition.getDisplayName() + " is not available");
            }

            DropdownOption option = dropdownOptionRepository
                    .findBySourceIdAndOptionKeyAndActiveTrue(dropdownSource.getId(), trimmedValue)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldDefinition.getDisplayName() + " must use an active option"));

            return new DynamicValueDraft(config, option.getOptionKey(), null, option.getDisplayValue());
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported dynamic field type");
    }

    private void validateMaxLength(String value, int maxLength, String fieldLabel) {
        if (value.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldLabel + " must be at most " + maxLength + " characters");
        }
    }

    private BigDecimal parseDynamicNumber(String value, String fieldLabel) {
        try {
            BigDecimal number = new BigDecimal(value);
            if (number.scale() > 2 || number.precision() - number.scale() > 10) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldLabel + " must fit within 12 digits and 2 decimal places");
            }
            return number;
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldLabel + " must be a valid number");
        }
    }

    private void saveDynamicValues(Ticket ticket, List<DynamicValueDraft> drafts) {
        if (drafts.isEmpty()) {
            return;
        }

        List<TicketDynamicValue> dynamicValues = drafts.stream()
                .map(draft -> toDynamicValue(ticket, draft))
                .toList();
        ticketDynamicValueRepository.saveAll(dynamicValues);
    }

    private TicketDynamicValue toDynamicValue(Ticket ticket, DynamicValueDraft draft) {
        CategoryFieldConfig config = draft.config();
        TicketFieldDefinition fieldDefinition = config.getFieldDefinition();

        TicketDynamicValue dynamicValue = new TicketDynamicValue();
        dynamicValue.setTicket(ticket);
        dynamicValue.setFieldDefinition(fieldDefinition);
        dynamicValue.setCategoryFieldConfig(config);
        dynamicValue.setFieldKeySnapshot(fieldDefinition.getFieldKey());
        dynamicValue.setFieldLabelSnapshot(fieldDefinition.getDisplayName());
        dynamicValue.setFieldTypeSnapshot(fieldDefinition.getFieldType());
        dynamicValue.setValueText(draft.valueText());
        dynamicValue.setValueNumber(draft.valueNumber());
        dynamicValue.setDisplayValue(draft.displayValue());
        return dynamicValue;
    }

    private String normalizeSearchQuery(String query) {
        if (query == null) {
            return null;
        }

        String trimmedQuery = query.trim();
        if (trimmedQuery.length() < 2 || trimmedQuery.length() > 120) {
            return null;
        }

        return trimmedQuery;
    }

    private TicketCategoryConfig resolveAssignableCategory(Long categoryId, TicketCategory legacyCategory) {
        TicketCategoryConfig category = resolveCategory(categoryId, legacyCategory);
        if (!category.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inactive category cannot be assigned");
        }
        return category;
    }

    private TicketCategoryConfig resolveCategory(Long categoryId, TicketCategory legacyCategory) {
        if (categoryId != null) {
            return ticketCategoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
        }
        if (legacyCategory != null) {
            return ticketCategoryRepository.findByCategoryKey(legacyCategory.name())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found"));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category is required");
    }

    private void assignCategory(Ticket ticket, TicketCategoryConfig category) {
        ticket.setCategoryRecord(category);
        ticket.setCategory(toBuiltInCategory(category.getCategoryKey()));
    }

    private TicketCategory toBuiltInCategory(String categoryKey) {
        try {
            return TicketCategory.valueOf(categoryKey);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String escapeLikeWildcards(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String parameterName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Enum.valueOf(enumType, value.trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + parameterName);
        }
    }

    private LocalDate parseDate(String value, String parameterName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + parameterName);
        }
    }

    private boolean parseBoolean(String value, String parameterName) {
        if (value == null || value.isBlank() || "false".equalsIgnoreCase(value.trim())) {
            return false;
        }
        if ("true".equalsIgnoreCase(value.trim())) {
            return true;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + parameterName);
    }

    private Instant atStartOfBusinessDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay(BUSINESS_ZONE).toInstant();
    }

    private LocalDate nextDay(LocalDate date) {
        try {
            return date.plusDays(1);
        } catch (DateTimeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid createdTo");
        }
    }

    private String formatTicketNumber(Long sequenceValue) {
        return String.format("KE-%03d", sequenceValue);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private String trimToEmpty(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.trim();
    }

    private record DynamicValueDraft(
            CategoryFieldConfig config,
            String valueText,
            BigDecimal valueNumber,
            String displayValue
    ) {
    }
}
