package com.ke.ticketsystemke.dto;

import jakarta.validation.constraints.Size;

public class UpdateWorkflowStatusRequest {

    @Size(max = 80)
    private String displayName;

    private Integer sortOrder;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
