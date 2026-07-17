package com.ke.ticketsystemke.service;
import com.ke.ticketsystemke.dto.WarrantyPendingActionResponse; import com.ke.ticketsystemke.entity.WarrantyClaim; import org.springframework.stereotype.Service; import java.util.*;
@Service
public class WarrantyPendingActionCalculator {
 public WarrantyPendingActionResponse calculate(WarrantyClaim claim){
  if(!claim.isActive() || claim.getState()==com.ke.ticketsystemke.entity.WarrantyClaimState.CLOSED || claim.getState()==com.ke.ticketsystemke.entity.WarrantyClaimState.RESOLVED) return action("NO_IMMEDIATE_ACTION","No immediate action",100,false);
  if(claim.getWarrantyOwnerEmployeeId()==null) return action("ASSIGN_WARRANTY_OWNER","Assign warranty owner",10,true,"WARRANTY_OWNER_MISSING");
  List<String> missing=new ArrayList<>();
  if(claim.getManufacturerName()==null) missing.add("MANUFACTURER_MISSING");
  if(claim.getProductSerialNumber()==null && claim.getModelNumber()==null) missing.add("PRODUCT_IDENTIFICATION_MISSING");
  if(!missing.isEmpty()) return new WarrantyPendingActionResponse("COMPLETE_WARRANTY_DETAILS","Complete warranty details",20,true,missing);
  if(claim.getManufacturerComplaintNumber()==null || claim.getComplaintRegisteredDate()==null) return action("REGISTER_MANUFACTURER_COMPLAINT","Register manufacturer complaint",30,false,"COMPLAINT_NOT_REGISTERED");
  if(claim.getNextFollowUpDate()==null) return action("ADD_FOLLOW_UP_DATE","Add next follow-up date",40,false,"FOLLOW_UP_DATE_MISSING");
  return action("NO_IMMEDIATE_ACTION","No immediate action",100,false);
 }
 private WarrantyPendingActionResponse action(String code,String label,int priority,boolean blocking,String... reasons){return new WarrantyPendingActionResponse(code,label,priority,blocking,List.of(reasons));}
}
