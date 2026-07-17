package com.ke.ticketsystemke.dto;
import com.ke.ticketsystemke.entity.WarrantyCustomerJourneyDecision; import jakarta.validation.constraints.*; import java.time.LocalDate;
public record CustomerDidNotProceedRequest(@NotNull LocalDate decisionDate,@NotBlank @Size(max=1500) String reason,@NotNull WarrantyCustomerJourneyDecision ticketJourneyDecision,@NotNull Long version) {}
