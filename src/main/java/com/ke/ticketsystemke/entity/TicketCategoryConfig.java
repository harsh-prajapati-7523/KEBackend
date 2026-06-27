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

@Entity
@Table(name = "ticket_categories")
public class TicketCategoryConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_key", nullable = false, unique = true, updatable = false, length = 40)
    private String categoryKey;

    @Column(name = "display_name", nullable = false, length = 80)
    private String displayName;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "system_category", nullable = false)
    private boolean systemCategory = false;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "workflow_mode", nullable = false, length = 30)
    private WorkflowMode workflowMode = WorkflowMode.LEGACY_FIXED;

    @Column(name = "fixed_actions_enabled", nullable = false)
    private boolean fixedActionsEnabled = true;

    @Column(name = "db_workflow_enabled", nullable = false)
    private boolean dbWorkflowEnabled = false;

    @Column(name = "workflow_mode_updated_at")
    private Instant workflowModeUpdatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_mode_updated_by_employee_id")
    private Employee workflowModeUpdatedByEmployee;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public TicketCategoryConfig() {
    }

    public Long getId() {
        return id;
    }

    public String getCategoryKey() {
        return categoryKey;
    }

    public void setCategoryKey(String categoryKey) {
        this.categoryKey = categoryKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isSystemCategory() {
        return systemCategory;
    }

    public void setSystemCategory(boolean systemCategory) {
        this.systemCategory = systemCategory;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public WorkflowMode getWorkflowMode() {
        return workflowMode;
    }

    public void setWorkflowMode(WorkflowMode workflowMode) {
        this.workflowMode = workflowMode == null ? WorkflowMode.LEGACY_FIXED : workflowMode;
    }

    public boolean isFixedActionsEnabled() {
        return fixedActionsEnabled;
    }

    public void setFixedActionsEnabled(boolean fixedActionsEnabled) {
        this.fixedActionsEnabled = fixedActionsEnabled;
    }

    public boolean isDbWorkflowEnabled() {
        return dbWorkflowEnabled;
    }

    public void setDbWorkflowEnabled(boolean dbWorkflowEnabled) {
        this.dbWorkflowEnabled = dbWorkflowEnabled;
    }

    public Instant getWorkflowModeUpdatedAt() {
        return workflowModeUpdatedAt;
    }

    public void setWorkflowModeUpdatedAt(Instant workflowModeUpdatedAt) {
        this.workflowModeUpdatedAt = workflowModeUpdatedAt;
    }

    public Employee getWorkflowModeUpdatedByEmployee() {
        return workflowModeUpdatedByEmployee;
    }

    public void setWorkflowModeUpdatedByEmployee(Employee workflowModeUpdatedByEmployee) {
        this.workflowModeUpdatedByEmployee = workflowModeUpdatedByEmployee;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    void setCreationTimestamp() {
        Instant now = Instant.now();
        if (workflowMode == null) {
            workflowMode = WorkflowMode.LEGACY_FIXED;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void setUpdateTimestamp() {
        if (workflowMode == null) {
            workflowMode = WorkflowMode.LEGACY_FIXED;
        }
        updatedAt = Instant.now();
    }
}
