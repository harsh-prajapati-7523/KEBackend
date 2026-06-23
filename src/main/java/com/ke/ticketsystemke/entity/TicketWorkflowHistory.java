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
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "ticket_workflow_history")
public class TicketWorkflowHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id")
    private Long ticketId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", insertable = false, updatable = false)
    private Ticket ticket;

    @Column(name = "ticket_number", length = 40)
    private String ticketNumber;

    @Column(name = "action_key", length = 60)
    private String actionKey;

    @Column(name = "workflow_action_id")
    private Long workflowActionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_action_id", insertable = false, updatable = false)
    private WorkflowAction workflowAction;

    @Column(name = "workflow_transition_id")
    private Long workflowTransitionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_transition_id", insertable = false, updatable = false)
    private WorkflowTransition workflowTransition;

    @Column(name = "from_status", length = 60)
    private String fromStatus;

    @Column(name = "to_status", length = 60)
    private String toStatus;

    @Column(name = "from_status_id")
    private Long fromStatusId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_status_id", insertable = false, updatable = false)
    private WorkflowStatus fromStatusRecord;

    @Column(name = "to_status_id")
    private Long toStatusId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_status_id", insertable = false, updatable = false)
    private WorkflowStatus toStatusRecord;

    @Column(name = "executed_by_employee_id", length = 80)
    private String executedByEmployeeId;

    @Column(name = "executed_by_employee_name_snapshot", length = 120)
    private String executedByEmployeeNameSnapshot;

    @Column(name = "previous_owner_employee_id", length = 80)
    private String previousOwnerEmployeeId;

    @Column(name = "new_owner_employee_id", length = 80)
    private String newOwnerEmployeeId;

    @Column(length = 1000)
    private String comment;

    @Column(length = 500)
    private String reason;

    @Column(length = 30)
    private String result;

    @Column(name = "failure_reason_code", length = 80)
    private String failureReasonCode;

    @Column(name = "failure_message", length = 255)
    private String failureMessage;

    @Column(name = "system_transition", nullable = false)
    private boolean systemTransition = false;

    @Column(name = "custom_transition", nullable = false)
    private boolean customTransition = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    public Long getId() {
        return id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public String getActionKey() {
        return actionKey;
    }

    public void setActionKey(String actionKey) {
        this.actionKey = actionKey;
    }

    public Long getWorkflowActionId() {
        return workflowActionId;
    }

    public void setWorkflowActionId(Long workflowActionId) {
        this.workflowActionId = workflowActionId;
    }

    public WorkflowAction getWorkflowAction() {
        return workflowAction;
    }

    public Long getWorkflowTransitionId() {
        return workflowTransitionId;
    }

    public void setWorkflowTransitionId(Long workflowTransitionId) {
        this.workflowTransitionId = workflowTransitionId;
    }

    public WorkflowTransition getWorkflowTransition() {
        return workflowTransition;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public Long getFromStatusId() {
        return fromStatusId;
    }

    public void setFromStatusId(Long fromStatusId) {
        this.fromStatusId = fromStatusId;
    }

    public WorkflowStatus getFromStatusRecord() {
        return fromStatusRecord;
    }

    public Long getToStatusId() {
        return toStatusId;
    }

    public void setToStatusId(Long toStatusId) {
        this.toStatusId = toStatusId;
    }

    public WorkflowStatus getToStatusRecord() {
        return toStatusRecord;
    }

    public String getExecutedByEmployeeId() {
        return executedByEmployeeId;
    }

    public void setExecutedByEmployeeId(String executedByEmployeeId) {
        this.executedByEmployeeId = executedByEmployeeId;
    }

    public String getExecutedByEmployeeNameSnapshot() {
        return executedByEmployeeNameSnapshot;
    }

    public void setExecutedByEmployeeNameSnapshot(String executedByEmployeeNameSnapshot) {
        this.executedByEmployeeNameSnapshot = executedByEmployeeNameSnapshot;
    }

    public String getPreviousOwnerEmployeeId() {
        return previousOwnerEmployeeId;
    }

    public void setPreviousOwnerEmployeeId(String previousOwnerEmployeeId) {
        this.previousOwnerEmployeeId = previousOwnerEmployeeId;
    }

    public String getNewOwnerEmployeeId() {
        return newOwnerEmployeeId;
    }

    public void setNewOwnerEmployeeId(String newOwnerEmployeeId) {
        this.newOwnerEmployeeId = newOwnerEmployeeId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getFailureReasonCode() {
        return failureReasonCode;
    }

    public void setFailureReasonCode(String failureReasonCode) {
        this.failureReasonCode = failureReasonCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public boolean isSystemTransition() {
        return systemTransition;
    }

    public void setSystemTransition(boolean systemTransition) {
        this.systemTransition = systemTransition;
    }

    public boolean isCustomTransition() {
        return customTransition;
    }

    public void setCustomTransition(boolean customTransition) {
        this.customTransition = customTransition;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    @PrePersist
    void setCreationTimestamp() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
