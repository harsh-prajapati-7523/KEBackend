package com.ke.ticketsystemke.dto;

import jakarta.validation.constraints.Size;

public class GenericTransitionPreviewRequest {

    @Size(max = 1000)
    private String comment;

    @Size(max = 500)
    private String reason;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
