package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.CancelTicketRequest;
import com.ke.ticketsystemke.dto.CompleteTicketRequest;
import com.ke.ticketsystemke.dto.CreateTicketDynamicValueRequest;
import com.ke.ticketsystemke.dto.CreateTicketRequest;
import com.ke.ticketsystemke.dto.CustomerHistoryResponse;
import com.ke.ticketsystemke.dto.CustomerHistoryTicketResponse;
import com.ke.ticketsystemke.dto.TicketActionAvailabilityResponse;
import com.ke.ticketsystemke.dto.TicketAvailableActionsResponse;
import com.ke.ticketsystemke.dto.TicketDynamicValueResponse;
import com.ke.ticketsystemke.dto.TicketDynamicValuesResponse;
import com.ke.ticketsystemke.dto.TicketResponse;
import com.ke.ticketsystemke.dto.UpdateWarrantyRequest;
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
import com.ke.ticketsystemke.repository.CategoryFieldConfigRepository;
import com.ke.ticketsystemke.repository.DropdownOptionRepository;
import com.ke.ticketsystemke.repository.TicketCategoryRepository;
import com.ke.ticketsystemke.repository.TicketDynamicValueRepository;
import com.ke.ticketsystemke.repository.TicketRepository;
import com.ke.ticketsystemke.repository.TicketSpecifications;
import com.ke.ticketsystemke.repository.WorkflowStatusRepository;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");
    private static final int DYNAMIC_TEXT_MAX_LENGTH = 255;
    private static final int DYNAMIC_TEXTAREA_MAX_LENGTH = 1000;
    private static final String ROLE_ACCESS_DENIED = "ROLE_ACCESS_DENIED";
    private static final String WORKFLOW_TRANSITION_INACTIVE = "WORKFLOW_TRANSITION_INACTIVE";
    private static final String STATUS_NOT_ALLOWED = "STATUS_NOT_ALLOWED";
    private static final String OWNER_REQUIRED = "OWNER_REQUIRED";
    private static final String ADMIN_REQUIRED = "ADMIN_REQUIRED";
    private static final String WARRANTY_NOT_CHECKED = "WARRANTY_NOT_CHECKED";
    private static final String TERMINAL_STATUS = "TERMINAL_STATUS";

    private final TicketRepository repository;
    private final TicketCategoryRepository ticketCategoryRepository;
    private final CategoryFieldConfigRepository categoryFieldConfigRepository;
    private final DropdownOptionRepository dropdownOptionRepository;
    private final TicketDynamicValueRepository ticketDynamicValueRepository;
    private final TicketChargeService ticketChargeService;
    private final WorkflowService workflowService;
    private final AccessService accessService;
    private final WorkflowStatusRepository workflowStatusRepository;

    public TicketService(
            TicketRepository repository,
            TicketCategoryRepository ticketCategoryRepository,
            CategoryFieldConfigRepository categoryFieldConfigRepository,
            DropdownOptionRepository dropdownOptionRepository,
            TicketDynamicValueRepository ticketDynamicValueRepository,
            TicketChargeService ticketChargeService,
            WorkflowService workflowService,
            AccessService accessService,
            WorkflowStatusRepository workflowStatusRepository
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
        ticket.setComplaintDescription(request.getComplaintDescription().trim());
        ticket.setStatus(TicketStatus.NEW);
        ticket.setStatusRecord(resolveWorkflowStatusForTicketStatus(TicketStatus.NEW));
        ticket.setCreatedByEmployeeId(createdByEmployeeId);
        validateWarrantyDetails(
                request.getWarrantyStatus(),
                request.getManufacturerOrBrandName(),
                request.getProductSerialNumber()
        );
        ticket.setWarrantyStatus(request.getWarrantyStatus());
        ticket.setManufacturerStatus(ManufacturerStatus.NOT_REQUIRED);
        ticket.setManufacturerComplaintNumber(trimToNull(request.getManufacturerComplaintNumber()));
        ticket.setManufacturerOrBrandName(trimToNull(request.getManufacturerOrBrandName()));
        ticket.setProductSerialNumber(trimToNull(request.getProductSerialNumber()));
        ticket.setWarrantyUpdatedAt(Instant.now());
        ticket.setWarrantyUpdatedByEmployeeId(createdByEmployeeId);

        Ticket saved = repository.save(ticket);
        saveDynamicValues(saved, dynamicValueDrafts);
        return TicketResponse.from(saved, BigDecimal.ZERO.setScale(2));
    }

    @Transactional(readOnly = true)
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

        log.info("event=ticket_available_actions_returned employeeId={} ticketId={}", employeeId, ticketId);
        return new TicketAvailableActionsResponse(ticket.getId(), actions);
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
            ticket.setStatus(TicketStatus.PICKED);
            ticket.setStatusRecord(resolveWorkflowStatusForTicketStatus(TicketStatus.PICKED));
            ticket.setPickedByEmployeeId(employeeId);
            Ticket saved = repository.save(ticket);
            TicketResponse resp = TicketResponse.from(saved, ticketChargeService.calculateTotalCharge(saved.getId()));
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
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticket.setStatusRecord(resolveWorkflowStatusForTicketStatus(TicketStatus.IN_PROGRESS));
            String previousOwner = ticket.getPickedByEmployeeId();
            ticket.setPickedByEmployeeId(employeeId);
            Ticket saved = repository.save(ticket);
            TicketResponse resp = TicketResponse.from(saved, ticketChargeService.calculateTotalCharge(saved.getId()));
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

        if (ticket.getWarrantyStatus() == WarrantyStatus.NOT_CHECKED) {
            log.warn("event=warranty_completion_blocked ticketId={} ticketNumber={} employeeId={} warrantyStatus={} manufacturerStatus={}",
                    ticketId, ticket.getTicketNumber(), employeeId, ticket.getWarrantyStatus(), ticket.getManufacturerStatus());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ticket warranty must be confirmed before completion"
            );
        }

        ticket.setStatus(TicketStatus.COMPLETED);
        ticket.setStatusRecord(resolveWorkflowStatusForTicketStatus(TicketStatus.COMPLETED));
        ticket.setCompletedAt(Instant.now());
        ticket.setCompletedByEmployeeId(employeeId);
        ticket.setCompletionRemark(trimToNull(request.getCompletionRemark()));

        Ticket saved = repository.save(ticket);
        TicketResponse resp = TicketResponse.from(saved, ticketChargeService.calculateTotalCharge(saved.getId()));
        log.info("event=ticket_completed ticketId={} ticketNumber={} employeeId={} statusTransition=IN_PROGRESS->COMPLETED", ticketId, ticket.getTicketNumber(), employeeId);
        return resp;
    }

    @Transactional
    public TicketResponse updateWarranty(
            Long ticketId,
            UpdateWarrantyRequest request,
            String employeeId,
            String role
    ) {
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        if (!isOperationalRole(role)) {
            log.warn("event=warranty_update_denied ticketId={} ticketNumber={} employeeId={} warrantyStatus={} manufacturerStatus={}",
                    ticketId, ticket.getTicketNumber(), employeeId, ticket.getWarrantyStatus(), ticket.getManufacturerStatus());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not authorized to update ticket warranty");
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            log.warn("event=warranty_update_denied ticketId={} ticketNumber={} employeeId={} warrantyStatus={} manufacturerStatus={}",
                    ticketId, ticket.getTicketNumber(), employeeId, ticket.getWarrantyStatus(), ticket.getManufacturerStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancelled ticket warranty cannot be updated");
        }

        WarrantyStatus previousWarrantyStatus = ticket.getWarrantyStatus();
        if (!isSuperAdminRole(role)
                && previousWarrantyStatus != WarrantyStatus.NOT_CHECKED
                && previousWarrantyStatus != request.getWarrantyStatus()) {
            log.warn("event=invalid_warranty_status_change ticketId={} ticketNumber={} employeeId={} warrantyStatus={} manufacturerStatus={}",
                    ticketId, ticket.getTicketNumber(), employeeId, previousWarrantyStatus, ticket.getManufacturerStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Confirmed warranty status cannot be changed");
        }

        validateWarrantyDetails(
                request.getWarrantyStatus(),
                request.getManufacturerOrBrandName(),
                request.getProductSerialNumber()
        );

        ticket.setWarrantyStatus(request.getWarrantyStatus());
        ticket.setManufacturerStatus(request.getManufacturerStatus());
        ticket.setManufacturerComplaintNumber(trimToNull(request.getManufacturerComplaintNumber()));
        ticket.setManufacturerOrBrandName(trimToNull(request.getManufacturerOrBrandName()));
        ticket.setProductSerialNumber(trimToNull(request.getProductSerialNumber()));
        ticket.setWarrantyUpdatedAt(Instant.now());
        ticket.setWarrantyUpdatedByEmployeeId(employeeId);

        Ticket saved = repository.save(ticket);
        TicketResponse response = TicketResponse.from(saved, ticketChargeService.calculateTotalCharge(saved.getId()));
        log.info("event=warranty_updated ticketId={} ticketNumber={} employeeId={} warrantyStatus={} manufacturerStatus={}",
                ticketId, saved.getTicketNumber(), employeeId, saved.getWarrantyStatus(), saved.getManufacturerStatus());
        return response;
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
        workflowService.requireTransitionAllowed(AccessKey.CANCEL_TICKET, previousStatus, TicketStatus.CANCELLED);
        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setStatusRecord(resolveWorkflowStatusForTicketStatus(TicketStatus.CANCELLED));
        ticket.setCancelledAt(Instant.now());
        ticket.setCancelledByEmployeeId(employeeId);
        ticket.setCancellationReason(request.getCancellationReason().trim());

        Ticket saved = repository.save(ticket);
        TicketResponse resp = TicketResponse.from(saved, ticketChargeService.calculateTotalCharge(saved.getId()));
        log.info("event=ticket_cancelled ticketId={} ticketNumber={} employeeId={} statusTransition={}->CANCELLED", ticketId, ticket.getTicketNumber(), employeeId, previousStatus);
        return resp;
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

        if (ticket.getWarrantyStatus() == WarrantyStatus.NOT_CHECKED) {
            return unavailable(
                    WARRANTY_NOT_CHECKED,
                    "Warranty must be checked before completing the ticket."
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

    private TicketActionAvailabilityResponse available() {
        return new TicketActionAvailabilityResponse(true, null, null);
    }

    private TicketActionAvailabilityResponse unavailable(String reasonCode, String message) {
        return new TicketActionAvailabilityResponse(false, reasonCode, message);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> listTickets() {
        List<TicketResponse> list = repository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(ticket -> TicketResponse.from(ticket, ticketChargeService.calculateTotalCharge(ticket.getId())))
            .toList();
        log.info("event=ticket_list_returned count={}", list.size());
        return list;
    }

    @Transactional(readOnly = true)
    public CustomerHistoryResponse getCustomerHistory(Long ticketId, String employeeId) {
        Ticket ticket = repository.findById(ticketId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Ticket not found"
                ));

        List<CustomerHistoryTicketResponse> tickets = repository
                .findTop10ByMobileNumberAndIdNotOrderByCreatedAtDesc(ticket.getMobileNumber(), ticketId)
                .stream()
                .map(historyTicket -> CustomerHistoryTicketResponse.from(
                        historyTicket,
                        ticketChargeService.calculateTotalCharge(historyTicket.getId())
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
        String normalizedQuery = normalizeSearchQuery(query);
        if (normalizedQuery == null) {
            return Collections.emptyList();
        }

        return repository.searchTickets(escapeLikeWildcards(normalizedQuery))
                .stream()
                .map(ticket -> TicketResponse.from(ticket, ticketChargeService.calculateTotalCharge(ticket.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> queryTickets(
            String search,
            String status,
            String category,
            String warrantyStatus,
            String manufacturerStatus,
            String createdFrom,
            String createdTo,
            String mine,
            String employeeId
    ) {
        String normalizedSearch = normalizeSearchQuery(search);
        TicketStatus parsedStatus = parseEnum(TicketStatus.class, status, "status");
        TicketCategory parsedCategory = parseEnum(TicketCategory.class, category, "category");
        WarrantyStatus parsedWarrantyStatus = parseEnum(WarrantyStatus.class, warrantyStatus, "warrantyStatus");
        ManufacturerStatus parsedManufacturerStatus = parseEnum(ManufacturerStatus.class, manufacturerStatus, "manufacturerStatus");
        LocalDate parsedCreatedFrom = parseDate(createdFrom, "createdFrom");
        LocalDate parsedCreatedTo = parseDate(createdTo, "createdTo");
        boolean parsedMine = parseBoolean(mine, "mine");

        if (parsedCreatedFrom != null && parsedCreatedTo != null && parsedCreatedFrom.isAfter(parsedCreatedTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "createdFrom must not be after createdTo");
        }

        Instant createdFromInclusive = atStartOfBusinessDay(parsedCreatedFrom);
        Instant createdToExclusive = parsedCreatedTo == null ? null : atStartOfBusinessDay(nextDay(parsedCreatedTo));

        return repository.findAll(
                        TicketSpecifications.queryTickets(
                                normalizedSearch == null ? null : escapeLikeWildcards(normalizedSearch),
                                parsedStatus,
                                parsedCategory,
                                parsedWarrantyStatus,
                                parsedManufacturerStatus,
                                createdFromInclusive,
                                createdToExclusive,
                                parsedMine ? employeeId : null
                        ),
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
                .stream()
                .map(ticket -> TicketResponse.from(ticket, ticketChargeService.calculateTotalCharge(ticket.getId())))
                .toList();
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

    private void validateWarrantyDetails(
            WarrantyStatus warrantyStatus,
            String manufacturerOrBrandName,
            String productSerialNumber
    ) {
        if (warrantyStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Warranty status is required");
        }

        if (warrantyStatus == WarrantyStatus.IN_WARRANTY) {
            if (trimToNull(manufacturerOrBrandName) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Manufacturer or brand name is required for in-warranty tickets");
            }
            if (trimToNull(productSerialNumber) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product serial number is required for in-warranty tickets");
            }
        }
    }

    private record DynamicValueDraft(
            CategoryFieldConfig config,
            String valueText,
            BigDecimal valueNumber,
            String displayValue
    ) {
    }
}
