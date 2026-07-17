package com.ke.ticketsystemke.dto;
import jakarta.validation.constraints.*; import java.time.LocalDate;
public record ManufacturerRepairDoneRequest(@NotNull LocalDate completionDate,@NotBlank @Size(max=1500) String completionNotes,@Size(max=120) String engineerName,@Size(max=160) String serviceCenterName,@Size(max=120) String manufacturerReference,@Size(max=500) String managerOverrideReason,@NotNull Long version) {}
