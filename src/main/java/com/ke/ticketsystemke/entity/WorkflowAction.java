package com.ke.ticketsystemke.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "workflow_actions")
public class WorkflowAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action_key", nullable = false, unique = true, updatable = false, length = 60)
    private String actionKey;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(name = "button_label", nullable = false, length = 80)
    private String buttonLabel;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "system_action", nullable = false)
    private boolean systemAction = false;

    @Column(name = "protected_action", nullable = false)
    private boolean protectedAction = false;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "requires_comment", nullable = false)
    private boolean requiresComment = false;

    @Column(name = "confirmation_required", nullable = false)
    private boolean confirmationRequired = false;

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
        this.actionKey = actionKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getButtonLabel() {
        return buttonLabel;
    }

    public void setButtonLabel(String buttonLabel) {
        this.buttonLabel = buttonLabel;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isSystemAction() {
        return systemAction;
    }

    public void setSystemAction(boolean systemAction) {
        this.systemAction = systemAction;
    }

    public boolean isProtectedAction() {
        return protectedAction;
    }

    public void setProtectedAction(boolean protectedAction) {
        this.protectedAction = protectedAction;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isRequiresComment() {
        return requiresComment;
    }

    public void setRequiresComment(boolean requiresComment) {
        this.requiresComment = requiresComment;
    }

    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
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
        updatedAt = Instant.now();
    }
}
