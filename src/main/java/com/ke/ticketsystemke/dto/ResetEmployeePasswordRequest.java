package com.ke.ticketsystemke.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetEmployeePasswordRequest {

    @NotBlank
    @Size(min = 8, message = "password must contain at least 8 characters")
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
