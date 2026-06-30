package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.EmployeeRole;
import com.ke.ticketsystemke.entity.Role;

public record AssignableEmployeeResponse(
        String employeeId,
        String name,
        String role,
        String roleKey,
        String roleDisplayName
) {
    public static AssignableEmployeeResponse from(Employee employee) {
        Role roleRecord = employee.getRoleRecord();
        String roleKey = roleRecord != null ? roleRecord.getRoleKey() : fallbackRoleKey(employee.getRole());
        return new AssignableEmployeeResponse(
                employee.getEmployeeId(),
                employee.getName(),
                roleKey,
                roleKey,
                roleRecord != null ? roleRecord.getDisplayName() : null
        );
    }

    private static String fallbackRoleKey(EmployeeRole role) {
        return role == null ? null : role.name();
    }
}
