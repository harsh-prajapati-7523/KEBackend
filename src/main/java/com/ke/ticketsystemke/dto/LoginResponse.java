package com.ke.ticketsystemke.dto;

public class LoginResponse {

    private String token;
    private String employeeName;
    private String role;

    public LoginResponse(
            String token,
            String employeeName,
            String role
    ) {
        this.token = token;
        this.employeeName = employeeName;
        this.role = role;
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
}
