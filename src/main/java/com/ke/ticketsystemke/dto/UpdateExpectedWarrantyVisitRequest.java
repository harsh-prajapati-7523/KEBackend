package com.ke.ticketsystemke.dto;
import jakarta.validation.constraints.*; import java.time.LocalDate;
public record UpdateExpectedWarrantyVisitRequest(@NotNull LocalDate expectedVisitDate,@NotBlank @Size(max=500) String changeReason,@NotNull Long version) {}
