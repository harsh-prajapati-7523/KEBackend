package com.ke.ticketsystemke.dto;
import jakarta.validation.constraints.*; import java.time.LocalDate;
public record RegisterWarrantyComplaintRequest(@Size(max=120) String manufacturerComplaintNumber,LocalDate complaintRegisteredDate,LocalDate expectedVisitDate,LocalDate nextFollowUpDate,@Size(max=1000) String billDocumentOverrideReason,@NotNull Long version) {public RegisterWarrantyComplaintRequest(String number,LocalDate registered,LocalDate visit,LocalDate follow,Long version){this(number,registered,visit,follow,null,version);}}
