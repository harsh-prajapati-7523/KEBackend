package com.ke.ticketsystemke.dto;

import jakarta.validation.constraints.NotNull;

public class UpdateCategoryFieldConfigStatusRequest {

    @NotNull
    private Boolean visible;

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}
