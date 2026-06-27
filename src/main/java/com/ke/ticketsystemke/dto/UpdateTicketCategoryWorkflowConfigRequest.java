package com.ke.ticketsystemke.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ke.ticketsystemke.entity.WorkflowMode;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = false)
public class UpdateTicketCategoryWorkflowConfigRequest {

    @NotNull
    private WorkflowMode workflowMode;

    @NotNull
    private Boolean dbWorkflowEnabled;

    @NotNull
    private Boolean fixedActionsEnabled;

    public WorkflowMode getWorkflowMode() {
        return workflowMode;
    }

    public void setWorkflowMode(WorkflowMode workflowMode) {
        this.workflowMode = workflowMode;
    }

    public Boolean getDbWorkflowEnabled() {
        return dbWorkflowEnabled;
    }

    public void setDbWorkflowEnabled(Boolean dbWorkflowEnabled) {
        this.dbWorkflowEnabled = dbWorkflowEnabled;
    }

    public Boolean getFixedActionsEnabled() {
        return fixedActionsEnabled;
    }

    public void setFixedActionsEnabled(Boolean fixedActionsEnabled) {
        this.fixedActionsEnabled = fixedActionsEnabled;
    }
}
