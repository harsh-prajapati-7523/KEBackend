package com.ke.ticketsystemke.dto;

import jakarta.validation.constraints.Size;

public class UpdateWorkflowActionRequest {

    @Size(max = 80)
    private String displayName;

    @Size(max = 80)
    private String buttonLabel;

    @Size(max = 255)
    private String description;

    private Integer sortOrder;

    private Boolean requiresComment;

    private Boolean confirmationRequired;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getButtonLabel() {
        return buttonLabel;
    }

    public void setButtonLabel(String buttonLabel) {
        this.buttonLabel = buttonLabel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getRequiresComment() {
        return requiresComment;
    }

    public void setRequiresComment(Boolean requiresComment) {
        this.requiresComment = requiresComment;
    }

    public Boolean getConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(Boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }
}
