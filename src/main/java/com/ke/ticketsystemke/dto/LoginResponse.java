package com.ke.ticketsystemke.dto;

public class LoginResponse {

    private String token;
    private String accessToken;
    private String employeeName;
    private String role;
    private String employeeId;
    private boolean pinRequired;

    public LoginResponse(
            String token,
            String employeeName,
            String role,
            String employeeId
    ) {
        this(token, employeeName, role, employeeId, false);
    }

    public LoginResponse(
            String accessToken,
            String employeeName,
            String role,
            String employeeId,
            boolean pinRequired
    ) {
        this.token = accessToken;
        this.accessToken = accessToken;
        this.employeeName = employeeName;
        this.role = role;
        this.employeeId = employeeId;
        this.pinRequired = pinRequired;
    }

    public String getToken() {
        return token;
    }

    public String getAccessToken() {
        return accessToken;
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

    public boolean isPinRequired() {
        return pinRequired;
    }
}
