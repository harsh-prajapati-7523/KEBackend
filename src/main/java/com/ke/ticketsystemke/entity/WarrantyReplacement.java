package com.ke.ticketsystemke.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name="warranty_replacements", uniqueConstraints=@UniqueConstraint(name="uk_warranty_replacement_sequence",columnNames={"warranty_claim_id","replacement_sequence"}))
public class WarrantyReplacement {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="warranty_claim_id",nullable=false) private WarrantyClaim warrantyClaim;
 @Column(name="replacement_sequence",nullable=false) private int replacementSequence;
 @Column(nullable=false) private boolean active=true;
 private LocalDate replacementReceivedDate; private LocalDate replacementGivenToCustomerDate;
 @Column(length=160) private String newProductName; @Column(length=120) private String newProductType; @Column(length=120) private String newModelNumber;
 @Column(length=160) private String newSerialNumber; @Column(length=160) private String normalizedNewSerialNumber;
 private LocalDate newWarrantyStartDate; private LocalDate newWarrantyEndDate;
 @Column(length=160) private String replacementReferenceNumber; @Column(length=160) private String normalizedReferenceNumber;
 @Column(length=160) private String replacementProvidedBy; @Column(length=2000) private String replacementNotes;
 @Column(length=1000) private String serialNumberOverrideReason; @Column(length=1000) private String referenceOverrideReason;
 @Column(length=1000) private String supersedeReason;
 @Column(nullable=false,updatable=false) private Instant createdAt; @Column(nullable=false) private Instant updatedAt;
 @Column(nullable=false,updatable=false,length=80) private String createdByEmployeeId; @Column(nullable=false,length=80) private String updatedByEmployeeId;
 @Version @Column(nullable=false) private Long version;
 @PrePersist void created(){Instant n=Instant.now();if(createdAt==null)createdAt=n;updatedAt=n;} @PreUpdate void updated(){updatedAt=Instant.now();}
 public Long getId(){return id;} public WarrantyClaim getWarrantyClaim(){return warrantyClaim;} public void setWarrantyClaim(WarrantyClaim v){warrantyClaim=v;} public int getReplacementSequence(){return replacementSequence;} public void setReplacementSequence(int v){replacementSequence=v;} public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
 public LocalDate getReplacementReceivedDate(){return replacementReceivedDate;} public void setReplacementReceivedDate(LocalDate v){replacementReceivedDate=v;} public LocalDate getReplacementGivenToCustomerDate(){return replacementGivenToCustomerDate;} public void setReplacementGivenToCustomerDate(LocalDate v){replacementGivenToCustomerDate=v;}
 public String getNewProductName(){return newProductName;} public void setNewProductName(String v){newProductName=v;} public String getNewProductType(){return newProductType;} public void setNewProductType(String v){newProductType=v;} public String getNewModelNumber(){return newModelNumber;} public void setNewModelNumber(String v){newModelNumber=v;} public String getNewSerialNumber(){return newSerialNumber;} public void setNewSerialNumber(String v){newSerialNumber=v;} public String getNormalizedNewSerialNumber(){return normalizedNewSerialNumber;} public void setNormalizedNewSerialNumber(String v){normalizedNewSerialNumber=v;}
 public LocalDate getNewWarrantyStartDate(){return newWarrantyStartDate;} public void setNewWarrantyStartDate(LocalDate v){newWarrantyStartDate=v;} public LocalDate getNewWarrantyEndDate(){return newWarrantyEndDate;} public void setNewWarrantyEndDate(LocalDate v){newWarrantyEndDate=v;} public String getReplacementReferenceNumber(){return replacementReferenceNumber;} public void setReplacementReferenceNumber(String v){replacementReferenceNumber=v;} public String getNormalizedReferenceNumber(){return normalizedReferenceNumber;} public void setNormalizedReferenceNumber(String v){normalizedReferenceNumber=v;} public String getReplacementProvidedBy(){return replacementProvidedBy;} public void setReplacementProvidedBy(String v){replacementProvidedBy=v;} public String getReplacementNotes(){return replacementNotes;} public void setReplacementNotes(String v){replacementNotes=v;}
 public String getSerialNumberOverrideReason(){return serialNumberOverrideReason;} public void setSerialNumberOverrideReason(String v){serialNumberOverrideReason=v;} public String getReferenceOverrideReason(){return referenceOverrideReason;} public void setReferenceOverrideReason(String v){referenceOverrideReason=v;} public String getSupersedeReason(){return supersedeReason;} public void setSupersedeReason(String v){supersedeReason=v;}
 public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;} public String getCreatedByEmployeeId(){return createdByEmployeeId;} public void setCreatedByEmployeeId(String v){createdByEmployeeId=v;} public String getUpdatedByEmployeeId(){return updatedByEmployeeId;} public void setUpdatedByEmployeeId(String v){updatedByEmployeeId=v;} public Long getVersion(){return version;}
}
