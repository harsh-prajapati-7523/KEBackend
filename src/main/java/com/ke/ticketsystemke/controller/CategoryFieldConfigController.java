package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.AddCategoryFieldConfigRequest;
import com.ke.ticketsystemke.dto.CategoryFieldConfigResponse;
import com.ke.ticketsystemke.dto.UpdateCategoryFieldConfigRequest;
import com.ke.ticketsystemke.dto.UpdateCategoryFieldConfigStatusRequest;
import com.ke.ticketsystemke.service.CategoryFieldConfigService;
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
@RequestMapping("/volt/ticket-categories/{categoryId}/field-configs")
public class CategoryFieldConfigController {

    private final CategoryFieldConfigService categoryFieldConfigService;

    public CategoryFieldConfigController(CategoryFieldConfigService categoryFieldConfigService) {
        this.categoryFieldConfigService = categoryFieldConfigService;
    }

    @GetMapping
    public List<CategoryFieldConfigResponse> listConfigs(@PathVariable Long categoryId) {
        return categoryFieldConfigService.listConfigs(categoryId);
    }

    @PostMapping
    public ResponseEntity<CategoryFieldConfigResponse> addConfig(
            @PathVariable Long categoryId,
            @Valid @RequestBody AddCategoryFieldConfigRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryFieldConfigService.addConfig(categoryId, request, authentication.getName()));
    }

    @PatchMapping("/{configId}")
    public CategoryFieldConfigResponse updateConfig(
            @PathVariable Long categoryId,
            @PathVariable Long configId,
            @RequestBody UpdateCategoryFieldConfigRequest request,
            Authentication authentication
    ) {
        return categoryFieldConfigService.updateConfig(categoryId, configId, request, authentication.getName());
    }

    @PatchMapping("/{configId}/status")
    public CategoryFieldConfigResponse updateStatus(
            @PathVariable Long categoryId,
            @PathVariable Long configId,
            @Valid @RequestBody UpdateCategoryFieldConfigStatusRequest request,
            Authentication authentication
    ) {
        return categoryFieldConfigService.updateStatus(categoryId, configId, request.getVisible(), authentication.getName());
    }
}
