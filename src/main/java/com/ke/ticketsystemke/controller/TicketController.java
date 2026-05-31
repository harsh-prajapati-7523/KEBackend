package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.CreateTicketRequest;
import com.ke.ticketsystemke.dto.TicketResponse;
import com.ke.ticketsystemke.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/volt/tickets")
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            Authentication authentication
    ) {
        TicketResponse response = service.createTicket(
                request,
                authentication.getName()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public List<TicketResponse> listTickets() {
        return service.listTickets();
    }

    @PostMapping("/{id}/pick")
    public TicketResponse pickTicket(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return service.pickTicket(id, authentication.getName());
    }

    @PostMapping("/{id}/start-work")
    public TicketResponse startWork(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return service.startWork(id, authentication.getName());
    }
}
