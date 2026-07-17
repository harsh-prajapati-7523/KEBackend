package com.ke.ticketsystemke.dto;
import jakarta.validation.constraints.Pattern; import jakarta.validation.constraints.Size;
import java.time.LocalDate;
public class CreateWarrantyClaimRequest {
 private LocalDate billingDate,warrantyStartDate,warrantyEndDate,complaintRegisteredDate,expectedVisitDate,nextFollowUpDate;
 @Size(max=120) private String manufacturerName,productSerialNumber,modelNumber,manufacturerComplaintNumber,manufacturerEngineerName;
 @Pattern(regexp="^$|^[0-9+ -]{7,20}$",message="manufacturerEngineerMobile is invalid") private String manufacturerEngineerMobile;
 @Size(max=160) private String manufacturerServiceCenterName;
 @Size(max=2000) private String warrantyNotes;
 @Size(max=80) private String warrantyOwnerEmployeeId;
 public LocalDate getBillingDate(){return billingDate;} public void setBillingDate(LocalDate v){billingDate=v;} public LocalDate getWarrantyStartDate(){return warrantyStartDate;} public void setWarrantyStartDate(LocalDate v){warrantyStartDate=v;} public LocalDate getWarrantyEndDate(){return warrantyEndDate;} public void setWarrantyEndDate(LocalDate v){warrantyEndDate=v;}
 public String getManufacturerName(){return manufacturerName;} public void setManufacturerName(String v){manufacturerName=v;} public String getProductSerialNumber(){return productSerialNumber;} public void setProductSerialNumber(String v){productSerialNumber=v;} public String getModelNumber(){return modelNumber;} public void setModelNumber(String v){modelNumber=v;}
 public String getManufacturerComplaintNumber(){return manufacturerComplaintNumber;} public void setManufacturerComplaintNumber(String v){manufacturerComplaintNumber=v;} public LocalDate getComplaintRegisteredDate(){return complaintRegisteredDate;} public void setComplaintRegisteredDate(LocalDate v){complaintRegisteredDate=v;} public LocalDate getExpectedVisitDate(){return expectedVisitDate;} public void setExpectedVisitDate(LocalDate v){expectedVisitDate=v;}
 public String getManufacturerEngineerName(){return manufacturerEngineerName;} public void setManufacturerEngineerName(String v){manufacturerEngineerName=v;} public String getManufacturerEngineerMobile(){return manufacturerEngineerMobile;} public void setManufacturerEngineerMobile(String v){manufacturerEngineerMobile=v;} public String getManufacturerServiceCenterName(){return manufacturerServiceCenterName;} public void setManufacturerServiceCenterName(String v){manufacturerServiceCenterName=v;}
 public String getWarrantyNotes(){return warrantyNotes;} public void setWarrantyNotes(String v){warrantyNotes=v;} public LocalDate getNextFollowUpDate(){return nextFollowUpDate;} public void setNextFollowUpDate(LocalDate v){nextFollowUpDate=v;} public String getWarrantyOwnerEmployeeId(){return warrantyOwnerEmployeeId;} public void setWarrantyOwnerEmployeeId(String v){warrantyOwnerEmployeeId=v;}
}
