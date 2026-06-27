package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.CreateTicketCategoryRequest;
import com.ke.ticketsystemke.dto.TicketCategoryResponse;
import com.ke.ticketsystemke.dto.TicketCategoryWorkflowConfigResponse;
import com.ke.ticketsystemke.dto.TicketFormFieldsResponse;
import com.ke.ticketsystemke.dto.UpdateTicketCategoryStatusRequest;
import com.ke.ticketsystemke.dto.UpdateTicketCategoryWorkflowConfigRequest;
import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.service.AccessService;
import com.ke.ticketsystemke.service.CategoryFieldConfigService;
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
    private final CategoryFieldConfigService categoryFieldConfigService;
    private final AccessService accessService;

    public TicketCategoryController(
            TicketCategoryService ticketCategoryService,
            CategoryFieldConfigService categoryFieldConfigService,
            AccessService accessService
    ) {
        this.ticketCategoryService = ticketCategoryService;
        this.categoryFieldConfigService = categoryFieldConfigService;
        this.accessService = accessService;
    }

    @GetMapping
    public List<TicketCategoryResponse> listCategories(Authentication authentication) {
        accessService.requireAnyAllowed(
                authentication.getName(),
                AccessKey.CREATE_TICKET,
                AccessKey.VIEW_TICKET_CATEGORY_MANAGEMENT,
                AccessKey.MANAGE_TICKET_CATEGORIES
        );
        return ticketCategoryService.listCategories();
    }

    @GetMapping("/{id}/form-fields")
    public TicketFormFieldsResponse getFormFields(@PathVariable Long id, Authentication authentication) {
        accessService.requireAnyAllowed(
                authentication.getName(),
                AccessKey.CREATE_TICKET,
                AccessKey.VIEW_CATEGORY_FIELD_CONFIGURATION,
                AccessKey.MANAGE_CATEGORY_FIELD_CONFIGS
        );
        return categoryFieldConfigService.getFormFields(id);
    }

    @PostMapping
    public ResponseEntity<TicketCategoryResponse> createCategory(
            @Valid @RequestBody CreateTicketCategoryRequest request,
            Authentication authentication
    ) {
        accessService.requireAllowed(authentication.getName(), AccessKey.MANAGE_TICKET_CATEGORIES);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketCategoryService.createCategory(request, authentication.getName()));
    }

    @PatchMapping("/{id}/status")
    public TicketCategoryResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketCategoryStatusRequest request,
            Authentication authentication
    ) {
        accessService.requireAllowed(authentication.getName(), AccessKey.MANAGE_TICKET_CATEGORIES);
        return ticketCategoryService.updateStatus(id, request.getActive(), authentication.getName());
    }

    @GetMapping("/{id}/workflow-config")
    public TicketCategoryWorkflowConfigResponse getWorkflowConfig(
            @PathVariable Long id,
            Authentication authentication
    ) {
        accessService.requireAllowed(authentication.getName(), AccessKey.MANAGE_TICKET_CATEGORIES);
        return ticketCategoryService.getWorkflowConfig(id);
    }

    @PatchMapping("/{id}/workflow-config")
    public TicketCategoryWorkflowConfigResponse updateWorkflowConfig(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketCategoryWorkflowConfigRequest request,
            Authentication authentication
    ) {
        accessService.requireAllowed(authentication.getName(), AccessKey.MANAGE_TICKET_CATEGORIES);
        return ticketCategoryService.updateWorkflowConfig(id, request, authentication.getName());
    }
}
