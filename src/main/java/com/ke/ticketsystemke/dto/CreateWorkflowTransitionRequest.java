package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.entity.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateWorkflowTransitionRequest {

    @NotNull
    private AccessKey actionKey;

    @NotBlank
    @Size(max = 80)
    private String displayName;

    @NotNull
    private TicketStatus fromStatus;

    @NotNull
    private TicketStatus toStatus;

    private Boolean active;

    private Integer sortOrder;

    public AccessKey getActionKey() {
        return actionKey;
    }

    public void setActionKey(AccessKey actionKey) {
        this.actionKey = actionKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public TicketStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(TicketStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public TicketStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(TicketStatus toStatus) {
        this.toStatus = toStatus;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
