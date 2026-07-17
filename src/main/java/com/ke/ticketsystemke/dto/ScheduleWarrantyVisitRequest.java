package com.ke.ticketsystemke.dto;
import jakarta.validation.constraints.*; import java.time.LocalDate;
public record ScheduleWarrantyVisitRequest(@NotNull LocalDate expectedVisitDate,@Size(max=120) String engineerName,@Pattern(regexp="^$|^[0-9+ -]{7,20}$") String engineerMobile,@Size(max=160) String serviceCenterName,@Size(max=1500) String visitNotes,LocalDate nextFollowUpDate,@NotNull Long version) {}
