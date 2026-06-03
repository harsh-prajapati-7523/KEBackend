package com.ke.ticketsystemke.dto;

import jakarta.validation.constraints.NotNull;

public class AddCategoryFieldConfigRequest {

    @NotNull
    private Long fieldDefinitionId;

    private Boolean required;

    private Boolean visible;

    private Integer sortOrder;

    public Long getFieldDefinitionId() {
        return fieldDefinitionId;
    }

    public void setFieldDefinitionId(Long fieldDefinitionId) {
        this.fieldDefinitionId = fieldDefinitionId;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
