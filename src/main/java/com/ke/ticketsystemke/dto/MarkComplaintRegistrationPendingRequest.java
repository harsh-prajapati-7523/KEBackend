package com.ke.ticketsystemke.dto;
import com.ke.ticketsystemke.entity.WarrantyContactMethod; import jakarta.validation.constraints.*; import java.time.LocalDate;
public record MarkComplaintRegistrationPendingRequest(@NotNull LocalDate attemptDate,@NotNull WarrantyContactMethod contactMethod,@NotBlank @Size(max=1000) String notes,@NotNull LocalDate nextFollowUpDate,@NotNull Long version) {}
