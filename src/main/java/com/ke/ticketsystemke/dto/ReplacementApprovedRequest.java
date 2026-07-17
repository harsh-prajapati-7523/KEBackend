package com.ke.ticketsystemke.dto;
import jakarta.validation.constraints.*; import java.time.LocalDate;
public record ReplacementApprovedRequest(@NotNull LocalDate approvalDate,@Size(max=120) String approvalReference,@Size(max=1500) String approvalNotes,LocalDate expectedReplacementDate,@NotNull Long version) {}
