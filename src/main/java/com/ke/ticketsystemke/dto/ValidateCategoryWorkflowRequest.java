package com.ke.ticketsystemke.dto;

import jakarta.validation.constraints.NotNull;

public class ValidateCategoryWorkflowRequest {

    @NotNull
    private Long categoryId;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }
}
