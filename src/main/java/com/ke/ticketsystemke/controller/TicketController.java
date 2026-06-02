package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.CancelTicketRequest;
import com.ke.ticketsystemke.dto.CompleteTicketRequest;
import com.ke.ticketsystemke.dto.CreateTicketRequest;
import com.ke.ticketsystemke.dto.TicketResponse;
import com.ke.ticketsystemke.dto.UpdateWarrantyRequest;
import com.ke.ticketsystemke.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
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

    public TicketController(TicketService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
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
    public List<TicketResponse> listTickets() {
        log.info("event=ticket_list_requested");
        List<TicketResponse> list = service.listTickets();
        log.info("event=ticket_list_returned count={}", list.size());
        return list;
    }

    @GetMapping("/search")
    public List<TicketResponse> searchTickets(
            @RequestParam(required = false) String query,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        int queryLength = query == null ? 0 : query.length();
        log.info("event=ticket_search_requested employeeId={} queryLength={}", employeeId, queryLength);

        List<TicketResponse> list = service.searchTickets(query);

        log.info("event=ticket_search_returned employeeId={} queryLength={} resultCount={}",
                employeeId, queryLength, list.size());
        return list;
    }

    @PostMapping("/{id}/pick")
    public TicketResponse pickTicket(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        log.info("event=ticket_pick_requested ticketId={} employeeId={}", id, employeeId);
        TicketResponse resp = service.pickTicket(id, employeeId);
        log.info("event=ticket_picked ticketId={} ticketNumber={} pickedBy={}", resp.id(), resp.ticketNumber(), employeeId);
        return resp;
    }

    @PostMapping("/{id}/start-work")
    public TicketResponse startWork(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        log.info("event=ticket_start_requested ticketId={} employeeId={}", id, employeeId);
        TicketResponse resp = service.startWork(id, employeeId);
        log.info("event=ticket_started ticketId={} ticketNumber={} employeeId={}", resp.id(), resp.ticketNumber(), employeeId);
        return resp;
    }

    @PostMapping("/{id}/complete")
    public TicketResponse completeTicket(
            @PathVariable Long id,
            @Valid @RequestBody CompleteTicketRequest request,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        String role = extractRole(authentication);
        log.info("event=ticket_complete_requested ticketId={} employeeId={} role={}", id, employeeId, role);
        TicketResponse resp = service.completeTicket(id, request, employeeId, role);
        log.info("event=ticket_completed ticketId={} ticketNumber={} employeeId={}", resp.id(), resp.ticketNumber(), employeeId);
        return resp;
    }

    @PostMapping("/{id}/cancel")
    public TicketResponse cancelTicket(
            @PathVariable Long id,
            @Valid @RequestBody CancelTicketRequest request,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        String role = extractRole(authentication);
        log.info("event=ticket_cancel_requested ticketId={} employeeId={} role={}", id, employeeId, role);
        TicketResponse resp = service.cancelTicket(id, request, employeeId, role);
        log.info("event=ticket_cancelled ticketId={} ticketNumber={} employeeId={}", resp.id(), resp.ticketNumber(), employeeId);
        return resp;
    }

    @PatchMapping("/{id}/warranty")
    public TicketResponse updateWarranty(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWarrantyRequest request,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        String role = extractRole(authentication);
        return service.updateWarranty(id, request, employeeId, role);
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
