package com.ke.ticketsystemke.dto;
import com.ke.ticketsystemke.entity.WarrantyClaimState;
import jakarta.validation.constraints.*; import java.time.LocalDate; import java.util.*;
public class UpdateWarrantyClaimRequest extends CreateWarrantyClaimRequest {
 @NotNull private Long version;
 private WarrantyClaimState state;
 private final Set<String> supplied=new HashSet<>();
 public Long getVersion(){return version;} public void setVersion(Long v){version=v; supplied.add("version");}
 public WarrantyClaimState getState(){return state;} public void setState(WarrantyClaimState v){state=v; supplied.add("state");}
 @Override public void setBillingDate(LocalDate v){super.setBillingDate(v);supplied.add("billingDate");} @Override public void setWarrantyStartDate(LocalDate v){super.setWarrantyStartDate(v);supplied.add("warrantyStartDate");} @Override public void setWarrantyEndDate(LocalDate v){super.setWarrantyEndDate(v);supplied.add("warrantyEndDate");}
 @Override public void setManufacturerName(String v){super.setManufacturerName(v);supplied.add("manufacturerName");} @Override public void setProductSerialNumber(String v){super.setProductSerialNumber(v);supplied.add("productSerialNumber");} @Override public void setModelNumber(String v){super.setModelNumber(v);supplied.add("modelNumber");}
 @Override public void setManufacturerComplaintNumber(String v){super.setManufacturerComplaintNumber(v);supplied.add("manufacturerComplaintNumber");} @Override public void setComplaintRegisteredDate(LocalDate v){super.setComplaintRegisteredDate(v);supplied.add("complaintRegisteredDate");} @Override public void setExpectedVisitDate(LocalDate v){super.setExpectedVisitDate(v);supplied.add("expectedVisitDate");}
 @Override public void setManufacturerEngineerName(String v){super.setManufacturerEngineerName(v);supplied.add("manufacturerEngineerName");} @Override public void setManufacturerEngineerMobile(String v){super.setManufacturerEngineerMobile(v);supplied.add("manufacturerEngineerMobile");} @Override public void setManufacturerServiceCenterName(String v){super.setManufacturerServiceCenterName(v);supplied.add("manufacturerServiceCenterName");}
 @Override public void setWarrantyNotes(String v){super.setWarrantyNotes(v);supplied.add("warrantyNotes");} @Override public void setNextFollowUpDate(LocalDate v){super.setNextFollowUpDate(v);supplied.add("nextFollowUpDate");} @Override public void setWarrantyOwnerEmployeeId(String v){super.setWarrantyOwnerEmployeeId(v);supplied.add("warrantyOwnerEmployeeId");}
 public boolean supplied(String field){return supplied.contains(field);} public Set<String> suppliedFields(){return Set.copyOf(supplied);}
}
