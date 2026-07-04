package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.AssignTicketRequest;
import com.ke.ticketsystemke.dto.AssignableEmployeeResponse;
import com.ke.ticketsystemke.dto.CreateTicketRequest;
import com.ke.ticketsystemke.dto.CustomerHistoryResponse;
import com.ke.ticketsystemke.dto.GenericTransitionExecutionRequest;
import com.ke.ticketsystemke.dto.GenericTransitionPreviewRequest;
import com.ke.ticketsystemke.dto.GenericTransitionPreviewResponse;
import com.ke.ticketsystemke.dto.TicketAvailableActionsResponse;
import com.ke.ticketsystemke.dto.TicketDynamicValuesResponse;
import com.ke.ticketsystemke.dto.TicketResponse;
import com.ke.ticketsystemke.dto.TicketStatusFilterOptionResponse;
import com.ke.ticketsystemke.dto.TicketWorkflowHistoryPageResponse;
import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.service.AccessService;
import com.ke.ticketsystemke.service.EmployeeService;
import com.ke.ticketsystemke.service.GenericTransitionExecutorService;
import com.ke.ticketsystemke.service.TicketService;
import com.ke.ticketsystemke.service.TicketWorkflowHistoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/volt/tickets")
public class TicketController {

    private static final Logger log = LoggerFactory.getLogger(TicketController.class);

    private final TicketService service;
    private final AccessService accessService;
    private final EmployeeService employeeService;
    private final TicketWorkflowHistoryService ticketWorkflowHistoryService;
    private final GenericTransitionExecutorService genericTransitionExecutorService;

    public TicketController(
            TicketService service,
            AccessService accessService,
            EmployeeService employeeService,
            TicketWorkflowHistoryService ticketWorkflowHistoryService,
            GenericTransitionExecutorService genericTransitionExecutorService
    ) {
        this.service = service;
        this.accessService = accessService;
        this.employeeService = employeeService;
        this.ticketWorkflowHistoryService = ticketWorkflowHistoryService;
        this.genericTransitionExecutorService = genericTransitionExecutorService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        accessService.requireAllowed(employeeId, AccessKey.CREATE_TICKET);
        log.info("event=ticket_create_requested employeeId={}", employeeId);

        TicketResponse response = service.createTicket(
            request,
            employeeId
        );

        log.info("event=ticket_created ticketNumber={} employeeId={}", response.ticketNumber(), employeeId);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @GetMapping
    public List<TicketResponse> listTickets(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        accessService.requireAllowed(employeeId, AccessKey.VIEW_TICKETS);
        log.info("event=ticket_list_requested employeeId={} page={} size={}", employeeId, page, size);
        List<TicketResponse> list = service.listTickets(page, size);
        log.info("event=ticket_list_returned count={}", list.size());
        return list;
    }

    @GetMapping("/my")
    public List<TicketResponse> listMyTickets(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        accessService.requireAllowed(employeeId, AccessKey.VIEW_TICKETS);
        log.info("event=my_ticket_list_requested employeeId={} page={} size={}", employeeId, page, size);
        List<TicketResponse> list = service.listMyTickets(employeeId, page, size);
        log.info("event=my_ticket_list_returned employeeId={} count={}", employeeId, list.size());
        return list;
    }

    @GetMapping("/by-number/{ticketNumber}")
    public TicketResponse getTicketByNumber(
            @PathVariable String ticketNumber,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        accessService.requireAllowed(employeeId, AccessKey.VIEW_TICKETS);
        log.info("event=ticket_by_number_requested employeeId={}", employeeId);
        return service.getTicketByNumber(ticketNumber);
    }

    @GetMapping("/assignable-employees")
    public List<AssignableEmployeeResponse> listAssignableEmployees(Authentication authentication) {
        String employeeId = authentication.getName();
        accessService.requireAllowed(employeeId, AccessKey.ASSIGN_TICKET);
        log.info("event=ticket_assignable_employees_requested employeeId={}", employeeId);
        return employeeService.listAssignableEmployees();
    }

    @GetMapping("/{id}")
    public TicketResponse getTicket(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        accessService.requireAllowed(employeeId, AccessKey.VIEW_TICKETS);
        log.info("event=ticket_detail_requested employeeId={} ticketId={}", employeeId, id);
        return service.getTicket(id);
    }

    @PatchMapping("/{id}/assign")
    public TicketResponse assignTicket(
            @PathVariable Long id,
            @Valid @RequestBody AssignTicketRequest request,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        accessService.requireAllowed(employeeId, AccessKey.ASSIGN_TICKET);
        log.info("event=ticket_assignment_requested employeeId={} ticketId={}", employeeId, id);
        TicketResponse response = service.assignTicket(id, request, employeeId);
        log.info("event=ticket_assigned employeeId={} ticketId={} currentOwner={}",
                employeeId, response.id(), response.currentOwnerEmployeeId());
        return response;
    }

    @GetMapping("/status-filter-options")
    public List<TicketStatusFilterOptionResponse> getStatusFilterOptions(Authentication authentication) {
        String employeeId = authentication.getName();
        accessService.requireAllowed(employeeId, AccessKey.USE_TICKET_FILTERS);
        log.info("event=ticket_status_filter_options_requested employeeId={}", employeeId);
        return service.getTicketStatusFilterOptions(employeeId);
    }

    @GetMapping("/{id}/customer-history")
    public CustomerHistoryResponse getCustomerHistory(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        accessService.requireAllowed(employeeId, AccessKey.VIEW_CUSTOMER_HISTORY);
        log.info("event=customer_history_requested employeeId={} ticketId={}", employeeId, id);
        return service.getCustomerHistory(id, employeeId);
    }

    @GetMapping("/{id}/dynamic-values")
    public TicketDynamicValuesResponse getDynamicValues(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        accessService.requireAllowed(employeeId, AccessKey.VIEW_TICKETS);
        log.info("event=ticket_dynamic_values_requested employeeId={} ticketId={}", employeeId, id);
        return service.getDynamicValues(id, employeeId);
    }

    @GetMapping("/{id}/workflow-history")
    public TicketWorkflowHistoryPageResponse getWorkflowHistory(
            @PathVariable Long id,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        accessService.requireAllowed(employeeId, AccessKey.VIEW_TICKETS);
        log.info("event=ticket_workflow_history_requested employeeId={} ticketId={} page={} size={}",
                employeeId, id, page, size);
        return ticketWorkflowHistoryService.getTicketWorkflowHistory(id, page, size);
    }

    @GetMapping("/{id}/available-actions")
    public TicketAvailableActionsResponse getAvailableActions(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        String role = extractRole(authentication);
        log.info("event=ticket_available_actions_requested employeeId={} ticketId={}", employeeId, id);
        return service.getAvailableActions(id, employeeId, role);
    }

    @PostMapping("/{id}/workflow-transitions/{transitionId}/preview")
    public GenericTransitionPreviewResponse previewWorkflowTransition(
            @PathVariable Long id,
            @PathVariable Long transitionId,
            @Valid @RequestBody(required = false) GenericTransitionPreviewRequest request,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        accessService.requireAllowed(employeeId, AccessKey.VIEW_TICKETS);
        log.info("event=ticket_workflow_transition_preview_requested employeeId={} ticketId={} transitionId={}",
                employeeId, id, transitionId);
        GenericTransitionPreviewRequest safeRequest = request == null
                ? new GenericTransitionPreviewRequest()
                : request;
        return genericTransitionExecutorService.previewTransition(id, transitionId, employeeId, safeRequest);
    }

    @PostMapping("/{id}/workflow-transitions/{transitionId}/execute")
    public TicketResponse executeWorkflowTransition(
            @PathVariable Long id,
            @PathVariable Long transitionId,
            @Valid @RequestBody(required = false) GenericTransitionExecutionRequest request,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        accessService.requireAllowed(employeeId, AccessKey.VIEW_TICKETS);
        log.info("event=ticket_workflow_transition_execute_requested employeeId={} ticketId={} transitionId={}",
                employeeId, id, transitionId);
        GenericTransitionExecutionRequest safeRequest = request == null
                ? new GenericTransitionExecutionRequest()
                : request;
        TicketResponse response = genericTransitionExecutorService.executeTransition(id, transitionId, employeeId, safeRequest);
        log.info("event=ticket_workflow_transition_executed employeeId={} ticketId={} transitionId={} ticketNumber={}",
                employeeId, response.id(), transitionId, response.ticketNumber());
        return response;
    }

    @PostMapping("/{id}/workflow-actions/{actionKey}/execute")
    public TicketResponse executeWorkflowAction(
            @PathVariable Long id,
            @PathVariable String actionKey,
            @Valid @RequestBody(required = false) GenericTransitionExecutionRequest request,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        accessService.requireAllowed(employeeId, AccessKey.VIEW_TICKETS);
        log.info("event=ticket_workflow_action_execute_requested employeeId={} ticketId={} actionKey={}",
                employeeId, id, actionKey);
        GenericTransitionExecutionRequest safeRequest = request == null
                ? new GenericTransitionExecutionRequest()
                : request;
        TicketResponse response = genericTransitionExecutorService.executeActionTransition(id, actionKey, employeeId, safeRequest);
        log.info("event=ticket_workflow_action_executed employeeId={} ticketId={} actionKey={} ticketNumber={}",
                employeeId, response.id(), actionKey, response.ticketNumber());
        return response;
    }

    @GetMapping("/search")
    public List<TicketResponse> searchTickets(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        accessService.requireAllowed(employeeId, AccessKey.VIEW_TICKETS);
        accessService.requireAllowed(employeeId, AccessKey.USE_TICKET_SEARCH);
        int queryLength = query == null ? 0 : query.length();
        log.info("event=ticket_search_requested employeeId={} queryLength={} page={} size={}",
                employeeId, queryLength, page, size);

        List<TicketResponse> list = service.searchTickets(query, page, size);

        log.info("event=ticket_search_returned employeeId={} queryLength={} resultCount={}",
                employeeId, queryLength, list.size());
        return list;
    }

    @GetMapping("/query")
    public List<TicketResponse> queryTickets(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String createdFrom,
            @RequestParam(required = false) String createdTo,
            @RequestParam(required = false) String mine,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        accessService.requireAllowed(employeeId, AccessKey.VIEW_TICKETS);
        accessService.requireAllowed(employeeId, AccessKey.USE_TICKET_FILTERS);
        boolean hasSearch = search != null && search.trim().length() >= 2 && search.trim().length() <= 120;
        boolean mineRequested = "true".equalsIgnoreCase(mine == null ? "" : mine.trim());
        int activeFilterCount = countActiveFilters(
                hasSearch,
                mineRequested,
                status,
                category,
                createdFrom,
                createdTo
        );
        log.info("event=ticket_query_requested employeeId={} activeFilterCount={} hasSearch={} mine={} page={} size={}",
                employeeId, activeFilterCount, hasSearch, mineRequested, page, size);

        List<TicketResponse> list = service.queryTickets(
                search,
                status,
                category,
                createdFrom,
                createdTo,
                mine,
                employeeId,
                page,
                size
        );

        log.info("event=ticket_query_returned employeeId={} activeFilterCount={} hasSearch={} mine={} resultCount={}",
                employeeId, activeFilterCount, hasSearch, mineRequested, list.size());
        return list;
    }

    private int countActiveFilters(boolean hasSearch, boolean mine, String... filters) {
        int count = (hasSearch ? 1 : 0) + (mine ? 1 : 0);
        for (String filter : filters) {
            if (filter != null && !filter.isBlank()) {
                count++;
            }
        }
        return count;
    }

    private String extractRole(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse("");
    }
}
