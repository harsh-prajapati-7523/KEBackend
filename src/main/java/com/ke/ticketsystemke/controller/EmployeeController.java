package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.CreateEmployeeRequest;
import com.ke.ticketsystemke.dto.EmployeeResponse;
import com.ke.ticketsystemke.dto.ResetEmployeePasswordRequest;
import com.ke.ticketsystemke.dto.UpdateEmployeeRoleRequest;
import com.ke.ticketsystemke.dto.UpdateEmployeeStatusRequest;
import com.ke.ticketsystemke.service.EmployeeService;
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
@RequestMapping("/volt/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public List<EmployeeResponse> listEmployees() {
        return employeeService.listEmployees();
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeService.createEmployee(request, authentication.getName()));
    }

    @PatchMapping("/{id}/status")
    public EmployeeResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeStatusRequest request,
            Authentication authentication
    ) {
        return employeeService.updateStatus(id, request.getActive(), authentication.getName());
    }

    @PatchMapping("/{id}/role")
    public EmployeeResponse updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRoleRequest request,
            Authentication authentication
    ) {
        return employeeService.updateRole(id, request.getRoleId(), request.getRole(), authentication.getName());
    }

    @PatchMapping("/{id}/password")
    public EmployeeResponse resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetEmployeePasswordRequest request,
            Authentication authentication
    ) {
        return employeeService.resetPassword(id, request.getPassword(), authentication.getName());
    }
}
