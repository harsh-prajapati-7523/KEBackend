package com.ke.ticketsystemke.dto;
import jakarta.validation.constraints.*; import java.time.LocalDate;
public record RegisterWarrantyComplaintRequest(@NotBlank @Size(max=120) String manufacturerComplaintNumber,@NotNull LocalDate complaintRegisteredDate,LocalDate expectedVisitDate,LocalDate nextFollowUpDate,@NotNull Long version) {}
