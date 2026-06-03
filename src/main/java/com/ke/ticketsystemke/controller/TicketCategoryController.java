package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.CreateTicketCategoryRequest;
import com.ke.ticketsystemke.dto.TicketCategoryResponse;
import com.ke.ticketsystemke.dto.UpdateTicketCategoryStatusRequest;
import com.ke.ticketsystemke.service.TicketCategoryService;
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
@RequestMapping("/volt/ticket-categories")
public class TicketCategoryController {

    private final TicketCategoryService ticketCategoryService;

    public TicketCategoryController(TicketCategoryService ticketCategoryService) {
        this.ticketCategoryService = ticketCategoryService;
    }

    @GetMapping
    public List<TicketCategoryResponse> listCategories() {
        return ticketCategoryService.listCategories();
    }

    @PostMapping
    public ResponseEntity<TicketCategoryResponse> createCategory(
            @Valid @RequestBody CreateTicketCategoryRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketCategoryService.createCategory(request, authentication.getName()));
    }

    @PatchMapping("/{id}/status")
    public TicketCategoryResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketCategoryStatusRequest request,
            Authentication authentication
    ) {
        return ticketCategoryService.updateStatus(id, request.getActive(), authentication.getName());
    }
}
