package com.ke.ticketsystemke.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateDropdownOptionRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Z0-9_]{2,50}$", message = "optionKey must contain 2 to 50 uppercase letters, digits, or underscores")
    private String optionKey;

    @NotBlank
    @Size(max = 120, message = "displayValue must contain at most 120 characters")
    private String displayValue;

    private Boolean active;

    private Integer sortOrder;

    public String getOptionKey() {
        return optionKey;
    }

    public void setOptionKey(String optionKey) {
        this.optionKey = optionKey;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
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
