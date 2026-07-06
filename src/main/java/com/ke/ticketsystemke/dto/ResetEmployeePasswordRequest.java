package com.ke.ticketsystemke.dto;

import jakarta.validation.constraints.NotBlank;

public class ResetEmployeePasswordRequest {

    @NotBlank
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
