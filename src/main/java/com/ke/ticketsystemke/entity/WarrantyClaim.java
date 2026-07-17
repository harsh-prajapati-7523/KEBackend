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
    private LocalDate expectedVisitDate;
    @Column(length = 120) private String manufacturerEngineerName;
    @Column(length = 20) private String manufacturerEngineerMobile;
    @Column(length = 160) private String manufacturerServiceCenterName;
    @Column(length = 2000) private String warrantyNotes;
    private LocalDate nextFollowUpDate;
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
    public LocalDate getExpectedVisitDate(){return expectedVisitDate;} public void setExpectedVisitDate(LocalDate v){expectedVisitDate=v;}
    public String getManufacturerEngineerName(){return manufacturerEngineerName;} public void setManufacturerEngineerName(String v){manufacturerEngineerName=v;}
    public String getManufacturerEngineerMobile(){return manufacturerEngineerMobile;} public void setManufacturerEngineerMobile(String v){manufacturerEngineerMobile=v;}
    public String getManufacturerServiceCenterName(){return manufacturerServiceCenterName;} public void setManufacturerServiceCenterName(String v){manufacturerServiceCenterName=v;}
    public String getWarrantyNotes(){return warrantyNotes;} public void setWarrantyNotes(String v){warrantyNotes=v;}
    public LocalDate getNextFollowUpDate(){return nextFollowUpDate;} public void setNextFollowUpDate(LocalDate v){nextFollowUpDate=v;}
    public String getWarrantyOwnerEmployeeId(){return warrantyOwnerEmployeeId;} public void setWarrantyOwnerEmployeeId(String v){warrantyOwnerEmployeeId=v;}
    public Instant getMarkedWarrantyAt(){return markedWarrantyAt;} public Instant getResolvedAt(){return resolvedAt;} public void setResolvedAt(Instant v){resolvedAt=v;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
    public String getCreatedByEmployeeId(){return createdByEmployeeId;} public void setCreatedByEmployeeId(String v){createdByEmployeeId=v;}
    public String getUpdatedByEmployeeId(){return updatedByEmployeeId;} public void setUpdatedByEmployeeId(String v){updatedByEmployeeId=v;}
    public Long getVersion(){return version;}
}
