package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.CurrentAccessResponse;
import com.ke.ticketsystemke.dto.DynamicRoleAccessResponse;
import com.ke.ticketsystemke.dto.RoleAccessResponse;
import com.ke.ticketsystemke.dto.UpdateDynamicRoleAccessRequest;
import com.ke.ticketsystemke.dto.UpdateRoleAccessRequest;
import com.ke.ticketsystemke.service.AccessService;
import com.ke.ticketsystemke.service.RoleAccessService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoleAccessController {

    private final AccessService accessService;
    private final RoleAccessService roleAccessService;

    public RoleAccessController(AccessService accessService, RoleAccessService roleAccessService) {
        this.accessService = accessService;
        this.roleAccessService = roleAccessService;
    }

    @GetMapping("/volt/access/me")
    public CurrentAccessResponse getCurrentAccess(Authentication authentication) {
        return accessService.getCurrentAccess(authentication.getName());
    }

    @GetMapping("/volt/role-access/{roleId}")
    public RoleAccessResponse getRoleAccess(@PathVariable Long roleId) {
        return roleAccessService.getRoleAccess(roleId);
    }

    @PatchMapping("/volt/role-access/{roleId}")
    public RoleAccessResponse updateRoleAccess(
            @PathVariable Long roleId,
            @Valid @RequestBody UpdateRoleAccessRequest request,
            Authentication authentication
    ) {
        return roleAccessService.updateRoleAccess(roleId, request, authentication.getName());
    }

    @PatchMapping("/volt/role-access/{roleId}/dynamic")
    public DynamicRoleAccessResponse updateDynamicRoleAccess(
            @PathVariable Long roleId,
            @Valid @RequestBody UpdateDynamicRoleAccessRequest request,
            Authentication authentication
    ) {
        return roleAccessService.updateDynamicRoleAccess(roleId, request, authentication.getName());
    }
}
