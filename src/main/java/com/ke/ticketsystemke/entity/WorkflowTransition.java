package com.ke.ticketsystemke.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "workflow_transitions")
public class WorkflowTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action_key", nullable = false, length = 60)
    private String actionKey;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 30)
    private TicketStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private TicketStatus toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_status_id", nullable = false)
    private WorkflowStatus fromStatusRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_status_id", nullable = false)
    private WorkflowStatus toStatusRecord;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "system_transition", nullable = false)
    private boolean systemTransition = true;

    @Column(name = "protected_transition", nullable = false)
    private boolean protectedTransition = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_employee_id")
    private Employee updatedByEmployee;

    public Long getId() {
        return id;
    }

    public String getActionKey() {
        return actionKey;
    }

    public void setActionKey(String actionKey) {
        this.actionKey = normalizeActionKey(actionKey);
    }

    public void setActionKey(AccessKey actionKey) {
        this.actionKey = actionKey == null ? null : actionKey.name();
    }

    public AccessKey getSystemActionKey() {
        try {
            return actionKey == null ? null : AccessKey.valueOf(actionKey);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public TicketStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(TicketStatus fromStatus) {
        this.fromStatus = fromStatus;
    }

    public TicketStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(TicketStatus toStatus) {
        this.toStatus = toStatus;
    }

    public WorkflowStatus getFromStatusRecord() {
        return fromStatusRecord;
    }

    public void setFromStatusRecord(WorkflowStatus fromStatusRecord) {
        this.fromStatusRecord = fromStatusRecord;
    }

    public WorkflowStatus getToStatusRecord() {
        return toStatusRecord;
    }

    public void setToStatusRecord(WorkflowStatus toStatusRecord) {
        this.toStatusRecord = toStatusRecord;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isSystemTransition() {
        return systemTransition;
    }

    public void setSystemTransition(boolean systemTransition) {
        this.systemTransition = systemTransition;
    }

    public boolean isProtectedTransition() {
        return protectedTransition;
    }

    public void setProtectedTransition(boolean protectedTransition) {
        this.protectedTransition = protectedTransition;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Employee getUpdatedByEmployee() {
        return updatedByEmployee;
    }

    public void setUpdatedByEmployee(Employee updatedByEmployee) {
        this.updatedByEmployee = updatedByEmployee;
    }

    @PrePersist
    void setCreationTimestamp() {
        actionKey = normalizeActionKey(actionKey);
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void setUpdateTimestamp() {
        actionKey = normalizeActionKey(actionKey);
        updatedAt = Instant.now();
    }

    private String normalizeActionKey(String actionKey) {
        if (actionKey == null || actionKey.isBlank()) {
            return null;
        }
        return actionKey.trim().toUpperCase(Locale.ROOT);
    }
}
