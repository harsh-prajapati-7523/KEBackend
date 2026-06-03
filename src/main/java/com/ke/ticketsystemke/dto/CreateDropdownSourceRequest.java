package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.DropdownSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateDropdownSourceRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Z0-9_]{3,50}$", message = "sourceKey must contain 3 to 50 uppercase letters, digits, or underscores")
    private String sourceKey;

    @NotBlank
    @Size(max = 80, message = "displayName must contain at most 80 characters")
    private String displayName;

    private DropdownSourceType sourceType;

    private Boolean active;

    public String getSourceKey() {
        return sourceKey;
    }

    public void setSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public DropdownSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(DropdownSourceType sourceType) {
        this.sourceType = sourceType;
    }
}
