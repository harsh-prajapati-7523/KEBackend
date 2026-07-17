package com.ke.ticketsystemke.dto;
import com.ke.ticketsystemke.entity.WarrantyRejectionDecision; import jakarta.validation.constraints.*; import java.time.LocalDate;
public record RejectWarrantyRequest(@NotNull LocalDate rejectionDate,@NotBlank @Size(max=1500) String rejectionReason,@NotNull WarrantyRejectionDecision decision,@Size(max=500) String managerOverrideReason,@NotNull Long version) {}
