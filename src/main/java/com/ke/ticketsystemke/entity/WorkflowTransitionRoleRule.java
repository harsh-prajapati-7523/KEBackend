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
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "workflow_transition_role_rules",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_workflow_transition_role_rules_transition_role",
                columnNames = {"workflow_transition_id", "role_id"}
        )
)
public class WorkflowTransitionRoleRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_transition_id", nullable = false)
    private WorkflowTransition workflowTransition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_employee_id")
    private Employee createdByEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_employee_id")
    private Employee updatedByEmployee;

    public Long getId() {
        return id;
    }

    public WorkflowTransition getWorkflowTransition() {
        return workflowTransition;
    }

    public void setWorkflowTransition(WorkflowTransition workflowTransition) {
        this.workflowTransition = workflowTransition;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Employee getCreatedByEmployee() {
        return createdByEmployee;
    }

    public void setCreatedByEmployee(Employee createdByEmployee) {
        this.createdByEmployee = createdByEmployee;
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
