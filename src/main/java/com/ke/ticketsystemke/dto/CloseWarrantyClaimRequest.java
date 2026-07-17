package com.ke.ticketsystemke.dto;
import jakarta.validation.constraints.*;
public record CloseWarrantyClaimRequest(@NotNull Long version,@NotBlank @Size(max=500) String notes) {}
