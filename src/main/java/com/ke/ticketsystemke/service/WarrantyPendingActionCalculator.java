package com.ke.ticketsystemke.service;
import com.ke.ticketsystemke.dto.WarrantyPendingActionResponse; import com.ke.ticketsystemke.entity.*; import org.springframework.stereotype.Service;
import java.time.*; import java.time.temporal.ChronoUnit; import java.util.*;
@Service
public class WarrantyPendingActionCalculator {
 private static final ZoneId BUSINESS_ZONE=ZoneId.of("Asia/Kolkata"); private final Clock clock;
 public WarrantyPendingActionCalculator(){this(Clock.system(BUSINESS_ZONE));} WarrantyPendingActionCalculator(Clock clock){this.clock=clock;}
 public WarrantyPendingActionResponse calculate(WarrantyClaim c){
  LocalDate today=LocalDate.now(clock);
  if(!c.isActive()||c.getState()==WarrantyClaimState.CLOSED||c.getState()==WarrantyClaimState.RESOLVED) return action("NO_IMMEDIATE_ACTION","No immediate action",100,false,null,today,List.of(),null);
  if(c.getWarrantyOwnerEmployeeId()==null) return action("ASSIGN_WARRANTY_OWNER","Assign warranty owner",10,true,null,today,List.of("WARRANTY_OWNER_MISSING"),null);
  List<String> missing=new ArrayList<>(); if(c.getManufacturerName()==null)missing.add("MANUFACTURER_MISSING");if(c.getProductSerialNumber()==null&&c.getModelNumber()==null)missing.add("PRODUCT_IDENTIFICATION_MISSING");
  if(!missing.isEmpty())return action("COMPLETE_WARRANTY_DETAILS","Complete warranty details",20,true,null,today,missing,null);
  if(c.getState()==WarrantyClaimState.COMPLAINT_REGISTRATION_PENDING){if(due(c.getNextFollowUpDate(),today))return action("FOLLOW_UP_COMPLAINT_REGISTRATION","Follow up on complaint registration",30,false,c.getNextFollowUpDate(),today,List.of(overdue(c.getNextFollowUpDate(),today)?"FOLLOW_UP_OVERDUE":"FOLLOW_UP_DUE_TODAY"),"complaint-registration-pending");return action("NO_IMMEDIATE_ACTION","Complaint registration follow-up scheduled",100,false,c.getNextFollowUpDate(),today,List.of(),null);}
  if(c.getManufacturerComplaintNumber()==null||c.getComplaintRegisteredDate()==null)return action("REGISTER_MANUFACTURER_COMPLAINT","Register manufacturer complaint",30,false,null,today,List.of("COMPLAINT_NOT_REGISTERED"),"register-complaint");
  if(c.getActualVisitDate()!=null&&c.getVisitOutcome()!=null&&c.getVisitOutcome()!=WarrantyVisitOutcome.ENGINEER_DID_NOT_VISIT)return action("RECORD_WARRANTY_RESULT","Manufacturer visit completed — result action will be available in the next warranty phase",90,false,null,today,List.of("VISIT_COMPLETED_RESULT_PENDING"),null);
  if(c.getExpectedVisitDate()==null)return action("ADD_EXPECTED_VISIT_DATE","Schedule manufacturer visit",40,false,null,today,List.of("EXPECTED_VISIT_MISSING"),"schedule-visit");
  boolean visitOverdue=overdue(c.getExpectedVisitDate(),today),followDue=due(c.getNextFollowUpDate(),today); List<String> reasons=new ArrayList<>();if(visitOverdue)reasons.add("EXPECTED_VISIT_OVERDUE");if(followDue)reasons.add(overdue(c.getNextFollowUpDate(),today)?"FOLLOW_UP_OVERDUE":"FOLLOW_UP_DUE_TODAY");
  if(followDue)return action("FOLLOW_UP_MANUFACTURER","Follow up with manufacturer",50,false,earlier(c.getNextFollowUpDate(),c.getExpectedVisitDate()),today,reasons,"follow-up");
  if(visitOverdue||c.getVisitOutcome()==WarrantyVisitOutcome.ENGINEER_DID_NOT_VISIT)return action("RESCHEDULE_MANUFACTURER_VISIT","Reschedule manufacturer visit",60,false,c.getExpectedVisitDate(),today,reasons.isEmpty()?List.of("ENGINEER_DID_NOT_VISIT"):reasons,"update-visit");
  if(!c.getExpectedVisitDate().isAfter(today))return action("RECORD_MANUFACTURER_VISIT","Record manufacturer visit",70,false,c.getExpectedVisitDate(),today,List.of("EXPECTED_VISIT_REACHED"),"record-visit");
  return action("NO_IMMEDIATE_ACTION","No immediate action",100,false,earlier(c.getNextFollowUpDate(),c.getExpectedVisitDate()),today,List.of(),null);
 }
 private WarrantyPendingActionResponse action(String code,String label,int priority,boolean blocking,LocalDate due,LocalDate today,List<String> reasons,String form){boolean late=overdue(due,today);long days=late?ChronoUnit.DAYS.between(due,today):0;return new WarrantyPendingActionResponse(code,label,priority,blocking,due,late,days,List.copyOf(reasons),form);}
 private boolean due(LocalDate date,LocalDate today){return date!=null&&!date.isAfter(today);} private boolean overdue(LocalDate date,LocalDate today){return date!=null&&date.isBefore(today);} private LocalDate earlier(LocalDate a,LocalDate b){if(a==null)return b;if(b==null)return a;return a.isBefore(b)?a:b;}
}
