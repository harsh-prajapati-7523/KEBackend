package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.CreateDropdownOptionRequest;
import com.ke.ticketsystemke.dto.CreateDropdownSourceRequest;
import com.ke.ticketsystemke.dto.DropdownOptionResponse;
import com.ke.ticketsystemke.dto.DropdownSourceResponse;
import com.ke.ticketsystemke.dto.UpdateDropdownOptionStatusRequest;
import com.ke.ticketsystemke.dto.UpdateDropdownSourceStatusRequest;
import com.ke.ticketsystemke.service.DropdownSourceService;
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
@RequestMapping("/volt/dropdown-sources")
public class DropdownSourceController {

    private final DropdownSourceService dropdownSourceService;

    public DropdownSourceController(DropdownSourceService dropdownSourceService) {
        this.dropdownSourceService = dropdownSourceService;
    }

    @GetMapping
    public List<DropdownSourceResponse> listSources() {
        return dropdownSourceService.listSources();
    }

    @PostMapping
    public ResponseEntity<DropdownSourceResponse> createSource(
            @Valid @RequestBody CreateDropdownSourceRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dropdownSourceService.createSource(request, authentication.getName()));
    }

    @PatchMapping("/{id}/status")
    public DropdownSourceResponse updateSourceStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDropdownSourceStatusRequest request,
            Authentication authentication
    ) {
        return dropdownSourceService.updateSourceStatus(id, request.getActive(), authentication.getName());
    }

    @GetMapping("/{sourceId}/options")
    public List<DropdownOptionResponse> listOptions(@PathVariable Long sourceId) {
        return dropdownSourceService.listOptions(sourceId);
    }

    @PostMapping("/{sourceId}/options")
    public ResponseEntity<DropdownOptionResponse> createOption(
            @PathVariable Long sourceId,
            @Valid @RequestBody CreateDropdownOptionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dropdownSourceService.createOption(sourceId, request, authentication.getName()));
    }

    @PatchMapping("/{sourceId}/options/{optionId}/status")
    public DropdownOptionResponse updateOptionStatus(
            @PathVariable Long sourceId,
            @PathVariable Long optionId,
            @Valid @RequestBody UpdateDropdownOptionStatusRequest request,
            Authentication authentication
    ) {
        return dropdownSourceService.updateOptionStatus(sourceId, optionId, request.getActive(), authentication.getName());
    }
}
