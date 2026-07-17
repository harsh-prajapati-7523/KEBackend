package com.ke.ticketsystemke.dto;
import com.ke.ticketsystemke.entity.*;
import java.time.Instant; import java.time.LocalDate;
public record WarrantyClaimResponse(
 Long id,Long ticketId,String ticketNumber,int claimSequence,boolean active,WarrantyClaimState state,WarrantyClaimResult result,
 LocalDate billingDate,LocalDate warrantyStartDate,LocalDate warrantyEndDate,String manufacturerName,String productSerialNumber,String modelNumber,
 String manufacturerComplaintNumber,LocalDate complaintRegisteredDate,LocalDate expectedVisitDate,String manufacturerEngineerName,
 LocalDate complaintRegistrationAttemptDate,WarrantyContactMethod complaintContactMethod,String complaintAttemptNotes,
 LocalDate actualVisitDate,WarrantyVisitOutcome visitOutcome,String visitNotes,
 String manufacturerEngineerMobile,String manufacturerServiceCenterName,String warrantyNotes,LocalDate nextFollowUpDate,String followUpNotes,WarrantyFollowUpSource followUpSource,
 WarrantyOwnerResponse warrantyOwner,WarrantyPendingActionResponse pendingAction,Long version,Instant markedWarrantyAt,Instant resolvedAt,
 Instant createdAt,Instant updatedAt,String createdByEmployeeId,String updatedByEmployeeId) {}
