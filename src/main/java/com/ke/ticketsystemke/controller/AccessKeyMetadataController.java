package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.AccessKeyMetadataResponse;
import com.ke.ticketsystemke.dto.CreateAccessKeyMetadataRequest;
import com.ke.ticketsystemke.dto.UpdateAccessKeyMetadataRequest;
import com.ke.ticketsystemke.dto.UpdateAccessKeyMetadataStateRequest;
import com.ke.ticketsystemke.service.AccessKeyMetadataService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AccessKeyMetadataController {

    private final AccessKeyMetadataService accessKeyMetadataService;

    public AccessKeyMetadataController(AccessKeyMetadataService accessKeyMetadataService) {
        this.accessKeyMetadataService = accessKeyMetadataService;
    }

    @GetMapping("/volt/access/keys")
    public List<AccessKeyMetadataResponse> listAccessKeys(Authentication authentication) {
        return accessKeyMetadataService.listAccessKeys(authentication.getName());
    }

    @PostMapping("/volt/access/keys")
    public AccessKeyMetadataResponse createAccessKey(
            @RequestBody CreateAccessKeyMetadataRequest request,
            Authentication authentication
    ) {
        return accessKeyMetadataService.createAccessKey(request, authentication.getName());
    }

    @PatchMapping("/volt/access/keys/{id}")
    public AccessKeyMetadataResponse updateAccessKey(
            @PathVariable Long id,
            @RequestBody UpdateAccessKeyMetadataRequest request,
            Authentication authentication
    ) {
        return accessKeyMetadataService.updateAccessKey(id, request, authentication.getName());
    }

    @PatchMapping("/volt/access/keys/{id}/status")
    public AccessKeyMetadataResponse updateAccessKeyState(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAccessKeyMetadataStateRequest request,
            Authentication authentication
    ) {
        return accessKeyMetadataService.updateAccessKeyState(
                id,
                request == null ? null : request.getActive(),
                authentication.getName()
        );
    }
}
