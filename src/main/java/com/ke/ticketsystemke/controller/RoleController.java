package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.CreateRoleRequest;
import com.ke.ticketsystemke.dto.RoleResponse;
import com.ke.ticketsystemke.dto.UpdateRoleStatusRequest;
import com.ke.ticketsystemke.service.RoleService;
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
@RequestMapping("/volt/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public List<RoleResponse> listRoles() {
        return roleService.listRoles();
    }

    @PostMapping
    public ResponseEntity<RoleResponse> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(roleService.createRole(request, authentication.getName()));
    }

    @PatchMapping("/{id}/status")
    public RoleResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleStatusRequest request,
            Authentication authentication
    ) {
        return roleService.updateStatus(id, request.getActive(), authentication.getName());
    }
}
