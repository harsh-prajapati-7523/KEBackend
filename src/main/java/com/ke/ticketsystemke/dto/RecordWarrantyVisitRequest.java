package com.ke.ticketsystemke.dto;
import com.ke.ticketsystemke.entity.WarrantyVisitOutcome; import jakarta.validation.constraints.*; import java.time.LocalDate;
public record RecordWarrantyVisitRequest(@NotNull LocalDate actualVisitDate,@NotNull WarrantyVisitOutcome visitOutcome,@NotBlank @Size(max=1500) String visitNotes,@Size(max=120) String engineerName,@Pattern(regexp="^$|^[0-9+ -]{7,20}$") String engineerMobile,@Size(max=160) String serviceCenterName,LocalDate nextFollowUpDate,@NotNull Long version) {}
