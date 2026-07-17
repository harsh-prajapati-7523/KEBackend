package com.ke.ticketsystemke.dto;
import jakarta.validation.Valid; import jakarta.validation.constraints.*;
public record SupersedeWarrantyReplacementRequest(@NotBlank @Size(max=1000) String supersedeReason,@NotNull Long claimVersion,@NotNull Long currentReplacementVersion,@NotNull @Valid ReplacementDetailsRequest replacement){}
