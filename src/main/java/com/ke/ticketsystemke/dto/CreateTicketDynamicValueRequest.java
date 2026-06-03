package com.ke.ticketsystemke.dto;

public class CreateTicketDynamicValueRequest {

    private Long categoryFieldConfigId;

    private Long fieldDefinitionId;

    private String value;

    public Long getCategoryFieldConfigId() {
        return categoryFieldConfigId;
    }

    public void setCategoryFieldConfigId(Long categoryFieldConfigId) {
        this.categoryFieldConfigId = categoryFieldConfigId;
    }

    public Long getFieldDefinitionId() {
        return fieldDefinitionId;
    }

    public void setFieldDefinitionId(Long fieldDefinitionId) {
        this.fieldDefinitionId = fieldDefinitionId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
