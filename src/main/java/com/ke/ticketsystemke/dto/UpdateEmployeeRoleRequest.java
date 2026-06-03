package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.EmployeeRole;

public class UpdateEmployeeRoleRequest {

    private EmployeeRole role;

    private Long roleId;

    public EmployeeRole getRole() {
        return role;
    }

    public void setRole(EmployeeRole role) {
        this.role = role;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
