package com.ke.ticketsystemke.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "warranty_claims", uniqueConstraints = @UniqueConstraint(name = "uk_warranty_claims_ticket_sequence", columnNames = {"ticket_id", "claim_sequence"}))
public class WarrantyClaim {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ticket_id", nullable = false) private Ticket ticket;
    @Column(name = "claim_sequence", nullable = false) private int claimSequence;
    @Column(nullable = false) private boolean active = true;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private WarrantyClaimState state;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private WarrantyClaimResult result;
    private LocalDate billingDate;
    private LocalDate warrantyStartDate;
    private LocalDate warrantyEndDate;
    @Column(length = 120) private String manufacturerName;
    @Column(length = 120) private String productSerialNumber;
    @Column(length = 120) private String modelNumber;
    @Column(length = 120) private String manufacturerComplaintNumber;
    private LocalDate complaintRegisteredDate;
    private LocalDate complaintRegistrationAttemptDate;
    @Enumerated(EnumType.STRING) @Column(length = 40) private WarrantyContactMethod complaintContactMethod;
    @Column(length = 1000) private String complaintAttemptNotes;
    private LocalDate expectedVisitDate;
    private LocalDate actualVisitDate;
    @Enumerated(EnumType.STRING) @Column(length = 50) private WarrantyVisitOutcome visitOutcome;
    @Column(length = 1500) private String visitNotes;
    @Column(length = 120) private String manufacturerEngineerName;
    @Column(length = 20) private String manufacturerEngineerMobile;
    @Column(length = 160) private String manufacturerServiceCenterName;
    @Column(length = 2000) private String warrantyNotes;
    private LocalDate nextFollowUpDate;
    @Column(length = 1000) private String followUpNotes;
    @Enumerated(EnumType.STRING) @Column(length = 40) private WarrantyFollowUpSource followUpSource;
    private Instant lastFollowUpScheduledAt;
    @Column(length = 80) private String lastFollowUpUpdatedByEmployeeId;
    private Instant complaintRegisteredAt;
    private Instant visitScheduledAt;
    private Instant visitRecordedAt;
    private Instant manufacturerRepairCompletedAt;
    private LocalDate manufacturerRepairCompletionDate;
    @Column(length = 1500) private String manufacturerRepairCompletionNotes;
    @Column(length = 120) private String manufacturerRepairReference;
    private Instant warrantyRejectedAt;
    private LocalDate warrantyRejectionDate;
    @Column(length = 1500) private String warrantyRejectionReason;
    @Enumerated(EnumType.STRING) @Column(length = 40) private WarrantyRejectionDecision rejectionDecision;
    private Instant customerDecisionAt;
    private LocalDate customerDecisionDate;
    @Column(length = 1500) private String customerDecisionNotes;
    private Instant customerDidNotProceedAt;
    private LocalDate customerDidNotProceedDate;
    @Column(length = 1500) private String customerDidNotProceedReason;
    @Enumerated(EnumType.STRING) @Column(length = 40) private WarrantyCustomerJourneyDecision customerJourneyDecision;
    private Instant replacementApprovedAt;
    private LocalDate replacementApprovalDate;
    @Column(length = 120) private String replacementApprovalReference;
    private LocalDate expectedReplacementDate;
    @Column(length = 1500) private String replacementApprovalNotes;
    private Instant claimClosedAt;
    @Column(length=1000) private String billDocumentOverrideReason; private Instant billDocumentOverriddenAt; @Column(length=80) private String billDocumentOverriddenByEmployeeId;
    @Column(length = 80) private String claimClosedByEmployeeId;
    @Column(length = 80) private String warrantyOwnerEmployeeId;
    @Column(nullable = false, updatable = false) private Instant markedWarrantyAt;
    private Instant resolvedAt;
    @Column(nullable = false, updatable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;
    @Column(nullable = false, updatable = false, length = 80) private String createdByEmployeeId;
    @Column(nullable = false, length = 80) private String updatedByEmployeeId;
    @Version @Column(nullable = false) private Long version;

    @PrePersist void createTimestamps() { Instant now=Instant.now(); if(markedWarrantyAt==null) markedWarrantyAt=now; if(createdAt==null) createdAt=now; updatedAt=now; }
    @PreUpdate void updateTimestamp() { updatedAt=Instant.now(); }
    public Long getId(){return id;} public Ticket getTicket(){return ticket;} public void setTicket(Ticket v){ticket=v;}
    public int getClaimSequence(){return claimSequence;} public void setClaimSequence(int v){claimSequence=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public WarrantyClaimState getState(){return state;} public void setState(WarrantyClaimState v){state=v;}
    public WarrantyClaimResult getResult(){return result;} public void setResult(WarrantyClaimResult v){result=v;}
    public LocalDate getBillingDate(){return billingDate;} public void setBillingDate(LocalDate v){billingDate=v;}
    public LocalDate getWarrantyStartDate(){return warrantyStartDate;} public void setWarrantyStartDate(LocalDate v){warrantyStartDate=v;}
    public LocalDate getWarrantyEndDate(){return warrantyEndDate;} public void setWarrantyEndDate(LocalDate v){warrantyEndDate=v;}
    public String getManufacturerName(){return manufacturerName;} public void setManufacturerName(String v){manufacturerName=v;}
    public String getProductSerialNumber(){return productSerialNumber;} public void setProductSerialNumber(String v){productSerialNumber=v;}
    public String getModelNumber(){return modelNumber;} public void setModelNumber(String v){modelNumber=v;}
    public String getManufacturerComplaintNumber(){return manufacturerComplaintNumber;} public void setManufacturerComplaintNumber(String v){manufacturerComplaintNumber=v;}
    public LocalDate getComplaintRegisteredDate(){return complaintRegisteredDate;} public void setComplaintRegisteredDate(LocalDate v){complaintRegisteredDate=v;}
    public LocalDate getComplaintRegistrationAttemptDate(){return complaintRegistrationAttemptDate;} public void setComplaintRegistrationAttemptDate(LocalDate v){complaintRegistrationAttemptDate=v;} public WarrantyContactMethod getComplaintContactMethod(){return complaintContactMethod;} public void setComplaintContactMethod(WarrantyContactMethod v){complaintContactMethod=v;} public String getComplaintAttemptNotes(){return complaintAttemptNotes;} public void setComplaintAttemptNotes(String v){complaintAttemptNotes=v;}
    public LocalDate getExpectedVisitDate(){return expectedVisitDate;} public void setExpectedVisitDate(LocalDate v){expectedVisitDate=v;}
    public LocalDate getActualVisitDate(){return actualVisitDate;} public void setActualVisitDate(LocalDate v){actualVisitDate=v;} public WarrantyVisitOutcome getVisitOutcome(){return visitOutcome;} public void setVisitOutcome(WarrantyVisitOutcome v){visitOutcome=v;} public String getVisitNotes(){return visitNotes;} public void setVisitNotes(String v){visitNotes=v;}
    public String getManufacturerEngineerName(){return manufacturerEngineerName;} public void setManufacturerEngineerName(String v){manufacturerEngineerName=v;}
    public String getManufacturerEngineerMobile(){return manufacturerEngineerMobile;} public void setManufacturerEngineerMobile(String v){manufacturerEngineerMobile=v;}
    public String getManufacturerServiceCenterName(){return manufacturerServiceCenterName;} public void setManufacturerServiceCenterName(String v){manufacturerServiceCenterName=v;}
    public String getWarrantyNotes(){return warrantyNotes;} public void setWarrantyNotes(String v){warrantyNotes=v;}
    public LocalDate getNextFollowUpDate(){return nextFollowUpDate;} public void setNextFollowUpDate(LocalDate v){nextFollowUpDate=v;}
    public String getFollowUpNotes(){return followUpNotes;} public void setFollowUpNotes(String v){followUpNotes=v;} public WarrantyFollowUpSource getFollowUpSource(){return followUpSource;} public void setFollowUpSource(WarrantyFollowUpSource v){followUpSource=v;} public Instant getLastFollowUpScheduledAt(){return lastFollowUpScheduledAt;} public void setLastFollowUpScheduledAt(Instant v){lastFollowUpScheduledAt=v;} public String getLastFollowUpUpdatedByEmployeeId(){return lastFollowUpUpdatedByEmployeeId;} public void setLastFollowUpUpdatedByEmployeeId(String v){lastFollowUpUpdatedByEmployeeId=v;}
    public Instant getComplaintRegisteredAt(){return complaintRegisteredAt;} public void setComplaintRegisteredAt(Instant v){complaintRegisteredAt=v;} public Instant getVisitScheduledAt(){return visitScheduledAt;} public void setVisitScheduledAt(Instant v){visitScheduledAt=v;} public Instant getVisitRecordedAt(){return visitRecordedAt;} public void setVisitRecordedAt(Instant v){visitRecordedAt=v;}
    public Instant getManufacturerRepairCompletedAt(){return manufacturerRepairCompletedAt;} public void setManufacturerRepairCompletedAt(Instant v){manufacturerRepairCompletedAt=v;} public LocalDate getManufacturerRepairCompletionDate(){return manufacturerRepairCompletionDate;} public void setManufacturerRepairCompletionDate(LocalDate v){manufacturerRepairCompletionDate=v;} public String getManufacturerRepairCompletionNotes(){return manufacturerRepairCompletionNotes;} public void setManufacturerRepairCompletionNotes(String v){manufacturerRepairCompletionNotes=v;} public String getManufacturerRepairReference(){return manufacturerRepairReference;} public void setManufacturerRepairReference(String v){manufacturerRepairReference=v;}
    public Instant getWarrantyRejectedAt(){return warrantyRejectedAt;} public void setWarrantyRejectedAt(Instant v){warrantyRejectedAt=v;} public LocalDate getWarrantyRejectionDate(){return warrantyRejectionDate;} public void setWarrantyRejectionDate(LocalDate v){warrantyRejectionDate=v;} public String getWarrantyRejectionReason(){return warrantyRejectionReason;} public void setWarrantyRejectionReason(String v){warrantyRejectionReason=v;} public WarrantyRejectionDecision getRejectionDecision(){return rejectionDecision;} public void setRejectionDecision(WarrantyRejectionDecision v){rejectionDecision=v;}
    public Instant getCustomerDecisionAt(){return customerDecisionAt;} public void setCustomerDecisionAt(Instant v){customerDecisionAt=v;} public LocalDate getCustomerDecisionDate(){return customerDecisionDate;} public void setCustomerDecisionDate(LocalDate v){customerDecisionDate=v;} public String getCustomerDecisionNotes(){return customerDecisionNotes;} public void setCustomerDecisionNotes(String v){customerDecisionNotes=v;}
    public Instant getCustomerDidNotProceedAt(){return customerDidNotProceedAt;} public void setCustomerDidNotProceedAt(Instant v){customerDidNotProceedAt=v;} public LocalDate getCustomerDidNotProceedDate(){return customerDidNotProceedDate;} public void setCustomerDidNotProceedDate(LocalDate v){customerDidNotProceedDate=v;} public String getCustomerDidNotProceedReason(){return customerDidNotProceedReason;} public void setCustomerDidNotProceedReason(String v){customerDidNotProceedReason=v;} public WarrantyCustomerJourneyDecision getCustomerJourneyDecision(){return customerJourneyDecision;} public void setCustomerJourneyDecision(WarrantyCustomerJourneyDecision v){customerJourneyDecision=v;}
    public Instant getReplacementApprovedAt(){return replacementApprovedAt;} public void setReplacementApprovedAt(Instant v){replacementApprovedAt=v;} public LocalDate getReplacementApprovalDate(){return replacementApprovalDate;} public void setReplacementApprovalDate(LocalDate v){replacementApprovalDate=v;} public String getReplacementApprovalReference(){return replacementApprovalReference;} public void setReplacementApprovalReference(String v){replacementApprovalReference=v;} public LocalDate getExpectedReplacementDate(){return expectedReplacementDate;} public void setExpectedReplacementDate(LocalDate v){expectedReplacementDate=v;} public String getReplacementApprovalNotes(){return replacementApprovalNotes;} public void setReplacementApprovalNotes(String v){replacementApprovalNotes=v;}
    public Instant getClaimClosedAt(){return claimClosedAt;} public void setClaimClosedAt(Instant v){claimClosedAt=v;} public String getClaimClosedByEmployeeId(){return claimClosedByEmployeeId;} public void setClaimClosedByEmployeeId(String v){claimClosedByEmployeeId=v;} public String getBillDocumentOverrideReason(){return billDocumentOverrideReason;} public void setBillDocumentOverrideReason(String v){billDocumentOverrideReason=v;} public Instant getBillDocumentOverriddenAt(){return billDocumentOverriddenAt;} public void setBillDocumentOverriddenAt(Instant v){billDocumentOverriddenAt=v;} public String getBillDocumentOverriddenByEmployeeId(){return billDocumentOverriddenByEmployeeId;} public void setBillDocumentOverriddenByEmployeeId(String v){billDocumentOverriddenByEmployeeId=v;}
    public String getWarrantyOwnerEmployeeId(){return warrantyOwnerEmployeeId;} public void setWarrantyOwnerEmployeeId(String v){warrantyOwnerEmployeeId=v;}
    public Instant getMarkedWarrantyAt(){return markedWarrantyAt;} public Instant getResolvedAt(){return resolvedAt;} public void setResolvedAt(Instant v){resolvedAt=v;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
    public String getCreatedByEmployeeId(){return createdByEmployeeId;} public void setCreatedByEmployeeId(String v){createdByEmployeeId=v;}
    public String getUpdatedByEmployeeId(){return updatedByEmployeeId;} public void setUpdatedByEmployeeId(String v){updatedByEmployeeId=v;}
    public Long getVersion(){return version;}
}
