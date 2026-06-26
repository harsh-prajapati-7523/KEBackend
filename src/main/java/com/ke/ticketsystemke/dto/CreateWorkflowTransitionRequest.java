package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateWorkflowTransitionRequest {

    private String actionKey;

    @NotBlank
    @Size(max = 80)
    private String displayName;

    private TicketStatus fromStatus;

    private TicketStatus toStatus;

    private Long fromStatusId;

    private Long toStatusId;

    private Boolean active;

    private Integer sortOrder;

    public String getActionKey() {
        return actionKey;
    }

    public void setActionKey(String actionKey) {
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

    public Long getFromStatusId() {
        return fromStatusId;
    }

    public void setFromStatusId(Long fromStatusId) {
        this.fromStatusId = fromStatusId;
    }

    public Long getToStatusId() {
        return toStatusId;
    }

    public void setToStatusId(Long toStatusId) {
        this.toStatusId = toStatusId;
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
