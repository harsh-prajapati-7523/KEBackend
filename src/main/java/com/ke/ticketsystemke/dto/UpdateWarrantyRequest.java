package com.ke.ticketsystemke.dto;

import com.ke.ticketsystemke.entity.ManufacturerStatus;
import com.ke.ticketsystemke.entity.WarrantyStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UpdateWarrantyRequest {

    @NotNull
    private WarrantyStatus warrantyStatus;

    @NotNull
    private ManufacturerStatus manufacturerStatus;

    @Size(max = 80)
    private String manufacturerComplaintNumber;

    @Size(max = 80)
    private String manufacturerOrBrandName;

    @Size(max = 80)
    private String productSerialNumber;

    public WarrantyStatus getWarrantyStatus() {
        return warrantyStatus;
    }

    public void setWarrantyStatus(WarrantyStatus warrantyStatus) {
        this.warrantyStatus = warrantyStatus;
    }

    public ManufacturerStatus getManufacturerStatus() {
        return manufacturerStatus;
    }

    public void setManufacturerStatus(ManufacturerStatus manufacturerStatus) {
        this.manufacturerStatus = manufacturerStatus;
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
