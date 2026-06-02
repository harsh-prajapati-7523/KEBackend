package com.ke.ticketsystemke.dto;

public class LoginResponse {

    private String token;
    private String employeeName;
    private String role;
    private String employeeId;

    public LoginResponse(
            String token,
            String employeeName,
            String role,
            String employeeId
    ) {
        this.token = token;
        this.employeeName = employeeName;
        this.role = role;
        this.employeeId = employeeId;
    }

    public String getToken() {
        return token;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public String getRole() {
        return role;
    }

    public String getEmployeeId() {
        return employeeId;
    }
}
