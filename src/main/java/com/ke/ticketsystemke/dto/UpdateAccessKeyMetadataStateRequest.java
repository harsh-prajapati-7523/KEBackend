package com.ke.ticketsystemke.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateAccessKeyMetadataStateRequest {

    @NotNull
    private Boolean active;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
