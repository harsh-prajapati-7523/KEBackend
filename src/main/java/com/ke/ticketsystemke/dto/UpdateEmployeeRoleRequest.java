package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.EmployeeRole;
import jakarta.validation.constraints.NotNull;

public class UpdateEmployeeRoleRequest {

    @NotNull
    private EmployeeRole role;

    public EmployeeRole getRole() {
        return role;
    }

    public void setRole(EmployeeRole role) {
        this.role = role;
    }
}
