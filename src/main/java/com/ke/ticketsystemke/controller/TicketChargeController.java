package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.ChargeItemRequest;
import com.ke.ticketsystemke.dto.ChargeListResponse;
import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.service.AccessService;
import com.ke.ticketsystemke.service.TicketChargeService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/volt/tickets")
public class TicketChargeController {

    private static final Logger log = LoggerFactory.getLogger(TicketChargeController.class);

    private final TicketChargeService ticketChargeService;
    private final AccessService accessService;

    public TicketChargeController(TicketChargeService ticketChargeService, AccessService accessService) {
        this.ticketChargeService = ticketChargeService;
        this.accessService = accessService;
    }

    @GetMapping("/{ticketId}/charges")
    public ChargeListResponse listCharges(
            @PathVariable Long ticketId,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        accessService.requireAllowed(employeeId, AccessKey.VIEW_CHARGES);
        log.info("event=charge_list_requested endpoint=GET /volt/tickets/{}/charges employeeId={}", ticketId, employeeId);
        return ticketChargeService.listCharges(ticketId, employeeId);
    }

    @PostMapping("/{ticketId}/charges")
    public ResponseEntity<ChargeListResponse> addCharge(
            @PathVariable Long ticketId,
            @Valid @RequestBody ChargeItemRequest request,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        accessService.requireAllowed(employeeId, AccessKey.ADD_CHARGE);
        String role = extractRole(authentication);
        log.info("event=charge_add_requested endpoint=POST /volt/tickets/{}/charges employeeId={} role={}", ticketId, employeeId, role);
        ChargeListResponse response = ticketChargeService.addCharge(ticketId, request, employeeId, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{ticketId}/charges/{chargeItemId}")
    public ResponseEntity<ChargeListResponse> deleteCharge(
            @PathVariable Long ticketId,
            @PathVariable Long chargeItemId,
            Authentication authentication
    ) {
        String employeeId = authentication.getName();
        accessService.requireAllowed(employeeId, AccessKey.DELETE_CHARGE);
        String role = extractRole(authentication);
        log.info("event=charge_delete_requested endpoint=DELETE /volt/tickets/{}/charges/{} employeeId={} role={}", ticketId, chargeItemId, employeeId, role);
        ChargeListResponse response = ticketChargeService.deleteCharge(ticketId, chargeItemId, employeeId, role);
        return ResponseEntity.ok(response);
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
