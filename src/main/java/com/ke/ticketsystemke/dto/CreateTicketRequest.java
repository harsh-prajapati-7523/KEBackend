package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.TicketCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.ArrayList;
import java.util.List;

public class CreateTicketRequest {

    @NotBlank
    private String customerName;

    @NotBlank
    @Pattern(regexp = "\\d{10}", message = "mobileNumber must contain exactly 10 digits")
    private String mobileNumber;

    @NotBlank
    private String villageOrArea;

    @NotBlank
    private String productType;

    private TicketCategory category;

    private Long categoryId;

    private String complaintDescription;

    private List<CreateTicketDynamicValueRequest> dynamicValues = new ArrayList<>();

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getVillageOrArea() {
        return villageOrArea;
    }

    public void setVillageOrArea(String villageOrArea) {
        this.villageOrArea = villageOrArea;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public TicketCategory getCategory() {
        return category;
    }

    public void setCategory(TicketCategory category) {
        this.category = category;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getComplaintDescription() {
        return complaintDescription;
    }

    public void setComplaintDescription(String complaintDescription) {
        this.complaintDescription = complaintDescription;
    }

    public List<CreateTicketDynamicValueRequest> getDynamicValues() {
        return dynamicValues;
    }

    public void setDynamicValues(List<CreateTicketDynamicValueRequest> dynamicValues) {
        this.dynamicValues = dynamicValues == null ? new ArrayList<>() : dynamicValues;
    }
}
