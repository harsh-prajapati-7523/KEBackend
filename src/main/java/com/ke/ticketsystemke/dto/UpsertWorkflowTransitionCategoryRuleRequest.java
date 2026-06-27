package com.ke.ticketsystemke.dto;

import jakarta.validation.constraints.NotNull;

public class UpsertWorkflowTransitionCategoryRuleRequest {

    @NotNull
    private Long categoryId;

    private Boolean active;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
