package com.ke.ticketsystemke.dto;
import jakarta.validation.constraints.*; import java.time.LocalDate;
public record UpdateWarrantyFollowUpRequest(LocalDate nextFollowUpDate,@Size(max=1000) String followUpNotes,@NotNull Long version) {}
