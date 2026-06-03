package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.CreateTicketFieldDefinitionRequest;
import com.ke.ticketsystemke.dto.TicketFieldDefinitionResponse;
import com.ke.ticketsystemke.dto.UpdateTicketFieldDefinitionStatusRequest;
import com.ke.ticketsystemke.dto.UpdateTicketFieldDropdownSourceRequest;
import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.service.AccessService;
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
    private final AccessService accessService;

    public TicketFieldDefinitionController(TicketFieldDefinitionService ticketFieldDefinitionService, AccessService accessService) {
        this.ticketFieldDefinitionService = ticketFieldDefinitionService;
        this.accessService = accessService;
    }

    @GetMapping
    public List<TicketFieldDefinitionResponse> listFieldDefinitions(Authentication authentication) {
        accessService.requireAnyAllowed(
                authentication.getName(),
                AccessKey.VIEW_TICKET_FIELD_MANAGEMENT,
                AccessKey.MANAGE_TICKET_FIELDS
        );
        return ticketFieldDefinitionService.listFieldDefinitions();
    }

    @PostMapping
    public ResponseEntity<TicketFieldDefinitionResponse> createFieldDefinition(
            @Valid @RequestBody CreateTicketFieldDefinitionRequest request,
            Authentication authentication
    ) {
        accessService.requireAllowed(authentication.getName(), AccessKey.MANAGE_TICKET_FIELDS);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketFieldDefinitionService.createFieldDefinition(request, authentication.getName()));
    }

    @PatchMapping("/{id}/status")
    public TicketFieldDefinitionResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketFieldDefinitionStatusRequest request,
            Authentication authentication
    ) {
        accessService.requireAllowed(authentication.getName(), AccessKey.MANAGE_TICKET_FIELDS);
        return ticketFieldDefinitionService.updateStatus(id, request.getActive(), authentication.getName());
    }

    @PatchMapping("/{id}/dropdown-source")
    public TicketFieldDefinitionResponse updateDropdownSource(
            @PathVariable Long id,
            @RequestBody UpdateTicketFieldDropdownSourceRequest request,
            Authentication authentication
    ) {
        accessService.requireAllowed(authentication.getName(), AccessKey.MANAGE_TICKET_FIELDS);
        return ticketFieldDefinitionService.updateDropdownSource(
                id,
                request.getDropdownSourceId(),
                authentication.getName()
        );
    }
}
