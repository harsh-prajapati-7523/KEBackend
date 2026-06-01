package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.TicketCategory;
import com.ke.ticketsystemke.entity.WarrantyStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateTicketRequest {

    @NotBlank
    private String customerName;

    @NotBlank
    @Pattern(regexp = "\\d{10}", message = "mobileNumber must contain exactly 10 digits")
    private String mobileNumber;

    private String villageOrArea;

    @NotBlank
    private String productType;

    @NotNull
    private TicketCategory category;

    @NotBlank
    private String complaintDescription;

    @NotNull
    private WarrantyStatus warrantyStatus;

    @Size(max = 80)
    private String manufacturerComplaintNumber;

    @Size(max = 80)
    private String manufacturerOrBrandName;

    @Size(max = 80)
    private String productSerialNumber;

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

    public String getComplaintDescription() {
        return complaintDescription;
    }

    public void setComplaintDescription(String complaintDescription) {
        this.complaintDescription = complaintDescription;
    }

    public WarrantyStatus getWarrantyStatus() {
        return warrantyStatus;
    }

    public void setWarrantyStatus(WarrantyStatus warrantyStatus) {
        this.warrantyStatus = warrantyStatus;
    }

    public String getManufacturerComplaintNumber() {
        return manufacturerComplaintNumber;
    }

    public void setManufacturerComplaintNumber(String manufacturerComplaintNumber) {
        this.manufacturerComplaintNumber = manufacturerComplaintNumber;
    }

    public String getManufacturerOrBrandName() {
        return manufacturerOrBrandName;
    }

    public void setManufacturerOrBrandName(String manufacturerOrBrandName) {
        this.manufacturerOrBrandName = manufacturerOrBrandName;
    }

    public String getProductSerialNumber() {
        return productSerialNumber;
    }

    public void setProductSerialNumber(String productSerialNumber) {
        this.productSerialNumber = productSerialNumber;
    }
}
