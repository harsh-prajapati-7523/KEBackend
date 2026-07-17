package com.ke.ticketsystemke.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="warranty_claim_events")
public class WarrantyClaimEvent {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="warranty_claim_id", nullable=false) private WarrantyClaim warrantyClaim;
    @Column(nullable=false) private Long ticketId;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=50) private WarrantyClaimEventType eventType;
    @Column(nullable=false,length=80) private String actorEmployeeId;
    @Column(length=120) private String actorNameSnapshot;
    @Column(nullable=false,updatable=false) private Instant eventTimestamp;
    @Column(columnDefinition="TEXT") private String changedFieldsJson;
    @Column(nullable=false,length=500) private String summary;
    @PrePersist void timestamp(){if(eventTimestamp==null)eventTimestamp=Instant.now();}
    public Long getId(){return id;} public WarrantyClaim getWarrantyClaim(){return warrantyClaim;} public void setWarrantyClaim(WarrantyClaim v){warrantyClaim=v;}
    public Long getTicketId(){return ticketId;} public void setTicketId(Long v){ticketId=v;}
    public WarrantyClaimEventType getEventType(){return eventType;} public void setEventType(WarrantyClaimEventType v){eventType=v;}
    public String getActorEmployeeId(){return actorEmployeeId;} public void setActorEmployeeId(String v){actorEmployeeId=v;}
    public String getActorNameSnapshot(){return actorNameSnapshot;} public void setActorNameSnapshot(String v){actorNameSnapshot=v;}
    public Instant getEventTimestamp(){return eventTimestamp;} public String getChangedFieldsJson(){return changedFieldsJson;} public void setChangedFieldsJson(String v){changedFieldsJson=v;}
    public String getSummary(){return summary;} public void setSummary(String v){summary=v;}
}
