package com.ke.ticketsystemke.dto;
import jakarta.validation.constraints.NotNull; import java.time.LocalDate;
public record ReplacementDeliveredRequest(@NotNull LocalDate deliveredDate,@NotNull Long claimVersion,@NotNull Long replacementVersion){}
