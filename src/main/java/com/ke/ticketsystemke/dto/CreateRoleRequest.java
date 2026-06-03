package com.ke.ticketsystemke.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateRoleRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Z0-9_]{3,30}$", message = "roleKey must contain 3 to 30 uppercase letters, digits, or underscores")
    private String roleKey;

    @NotBlank
    @Size(max = 80, message = "displayName must contain at most 80 characters")
    private String displayName;

    private Boolean active;

    public String getRoleKey() {
        return roleKey;
    }

    public void setRoleKey(String roleKey) {
        this.roleKey = roleKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
