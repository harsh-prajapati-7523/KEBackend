package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.TicketFieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateTicketFieldDefinitionRequest {

    @NotBlank
    @Pattern(regexp = "^[A-Z0-9_]{3,50}$", message = "fieldKey must contain 3 to 50 uppercase letters, digits, or underscores")
    private String fieldKey;

    @NotBlank
    @Size(max = 80, message = "displayName must contain at most 80 characters")
    private String displayName;

    @NotNull
    private TicketFieldType fieldType;

    private Boolean active;

    @Size(max = 255, message = "helpText must contain at most 255 characters")
    private String helpText;

    private Boolean defaultRequired;

    private Integer sortOrder;

    public String getFieldKey() {
        return fieldKey;
    }

    public void setFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public TicketFieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(TicketFieldType fieldType) {
        this.fieldType = fieldType;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getHelpText() {
        return helpText;
    }

    public void setHelpText(String helpText) {
        this.helpText = helpText;
    }

    public Boolean getDefaultRequired() {
        return defaultRequired;
    }

    public void setDefaultRequired(Boolean defaultRequired) {
        this.defaultRequired = defaultRequired;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
