package com.ke.ticketsystemke.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Entity
@Table(name = "tickets")
@Data
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ticketNumber;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false, length = 10)
    private String mobileNumber;

    private String villageOrArea;

    @Column(nullable = false)
    private String productType;

    @Enumerated(EnumType.STRING)
    @Column
    private TicketCategory category;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private TicketCategoryConfig categoryRecord;

    @Column(nullable = false, length = 1000)
    private String complaintDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(nullable = false, updatable = false)
    private String createdByEmployeeId;

    @Column(name = "picked_by_employee_id")
    private String pickedByEmployeeId;

    private Instant completedAt;

    @Column(name = "completed_by_employee_id")
    private String completedByEmployeeId;

    @Column(length = 1000)
    private String completionRemark;

    private Instant cancelledAt;

    @Column(name = "cancelled_by_employee_id")
    private String cancelledByEmployeeId;

    @Column(length = 1000)
    private String cancellationReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WarrantyStatus warrantyStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ManufacturerStatus manufacturerStatus;

    @Column(length = 80)
    private String manufacturerComplaintNumber;

    @Column(length = 80)
    private String manufacturerOrBrandName;

    @Column(length = 80)
    private String productSerialNumber;

    private Instant warrantyUpdatedAt;

    private String warrantyUpdatedByEmployeeId;

    @PrePersist
    void setCreationTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void setUpdatedTimestamp() {
        updatedAt = Instant.now();
    }
}
