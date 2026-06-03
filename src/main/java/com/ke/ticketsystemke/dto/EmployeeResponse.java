package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.EmployeeRole;
import com.ke.ticketsystemke.entity.Role;

import java.time.Instant;

public record EmployeeResponse(
        Long id,
        String name,
        String employeeId,
        String role,
        Long roleId,
        String roleKey,
        String roleDisplayName,
        boolean active,
        Instant createdAt
) {
    public static EmployeeResponse from(Employee employee) {
        Role roleRecord = employee.getRoleRecord();
        String roleKey = roleRecord != null ? roleRecord.getRoleKey() : fallbackRoleKey(employee.getRole());
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getEmployeeId(),
                roleKey,
                roleRecord != null ? roleRecord.getId() : null,
                roleKey,
                roleRecord != null ? roleRecord.getDisplayName() : null,
                employee.isActive(),
                employee.getCreatedAt()
        );
    }

    private static String fallbackRoleKey(EmployeeRole role) {
        return role == null ? null : role.name();
    }
}
