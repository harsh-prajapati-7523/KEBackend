package com.ke.ticketsystemke.dto;
import java.util.List;
public record WarrantyClaimEventPageResponse(Long ticketId,Long claimId,int page,int size,long totalElements,int totalPages,boolean first,boolean last,List<WarrantyClaimEventResponse> events) {}
