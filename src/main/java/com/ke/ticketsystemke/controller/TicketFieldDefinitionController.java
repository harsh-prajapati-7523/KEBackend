package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.CreateTicketFieldDefinitionRequest;
import com.ke.ticketsystemke.dto.TicketFieldDefinitionResponse;
import com.ke.ticketsystemke.dto.UpdateTicketFieldDefinitionStatusRequest;
import com.ke.ticketsystemke.service.TicketFieldDefinitionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/volt/ticket-fields")
public class TicketFieldDefinitionController {

    private final TicketFieldDefinitionService ticketFieldDefinitionService;

    public TicketFieldDefinitionController(TicketFieldDefinitionService ticketFieldDefinitionService) {
        this.ticketFieldDefinitionService = ticketFieldDefinitionService;
    }

    @GetMapping
    public List<TicketFieldDefinitionResponse> listFieldDefinitions() {
        return ticketFieldDefinitionService.listFieldDefinitions();
    }

    @PostMapping
    public ResponseEntity<TicketFieldDefinitionResponse> createFieldDefinition(
            @Valid @RequestBody CreateTicketFieldDefinitionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketFieldDefinitionService.createFieldDefinition(request, authentication.getName()));
    }

    @PatchMapping("/{id}/status")
    public TicketFieldDefinitionResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketFieldDefinitionStatusRequest request,
            Authentication authentication
    ) {
        return ticketFieldDefinitionService.updateStatus(id, request.getActive(), authentication.getName());
    }
}
