package com.ke.ticketsystemke.dto;
import com.ke.ticketsystemke.entity.WarrantyClaimEvent; import com.ke.ticketsystemke.entity.WarrantyClaimEventType; import java.time.Instant;
public record WarrantyClaimEventResponse(Long id,WarrantyClaimEventType eventType,String actorEmployeeId,String actorNameSnapshot,Instant eventTimestamp,String summary){
 public static WarrantyClaimEventResponse from(WarrantyClaimEvent e){return new WarrantyClaimEventResponse(e.getId(),e.getEventType(),e.getActorEmployeeId(),e.getActorNameSnapshot(),e.getEventTimestamp(),e.getSummary());}
}
