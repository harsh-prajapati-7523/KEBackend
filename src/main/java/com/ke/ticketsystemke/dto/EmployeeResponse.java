package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.EmployeeRole;

import java.time.Instant;

public record EmployeeResponse(
        Long id,
        String name,
        String employeeId,
        EmployeeRole role,
        boolean active,
        Instant createdAt
) {
    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getEmployeeId(),
                employee.getRole(),
                employee.isActive(),
                employee.getCreatedAt()
        );
    }
}
