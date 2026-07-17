package com.ke.ticketsystemke.dto;
import com.ke.ticketsystemke.entity.WarrantyRejectionDecision; import jakarta.validation.constraints.*; import java.time.LocalDate;
public record RecordWarrantyCustomerDecisionRequest(@NotNull WarrantyRejectionDecision decision,@NotNull LocalDate decisionDate,@NotBlank @Size(max=1500) String decisionNotes,@NotNull Long version) {}
