package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.TicketStatus;
import jakarta.validation.constraints.Size;

public class UpdateWorkflowStatusRequest {

    @Size(max = 80)
    private String displayName;

    private Boolean terminal;

    private TicketStatus behaviorBucket;

    private Integer sortOrder;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Boolean getTerminal() {
        return terminal;
    }

    public void setTerminal(Boolean terminal) {
        this.terminal = terminal;
    }

    public TicketStatus getBehaviorBucket() {
        return behaviorBucket;
    }

    public void setBehaviorBucket(TicketStatus behaviorBucket) {
        this.behaviorBucket = behaviorBucket;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
