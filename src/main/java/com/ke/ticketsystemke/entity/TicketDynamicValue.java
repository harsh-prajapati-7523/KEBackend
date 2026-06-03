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
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "ticket_dynamic_values",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ticket_dynamic_values_ticket_field",
                columnNames = {"ticket_id", "field_definition_id"}
        )
)
public class TicketDynamicValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "field_definition_id", nullable = false)
    private TicketFieldDefinition fieldDefinition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_field_config_id", nullable = false)
    private CategoryFieldConfig categoryFieldConfig;

    @Column(name = "field_key_snapshot", nullable = false, length = 50)
    private String fieldKeySnapshot;

    @Column(name = "field_label_snapshot", nullable = false, length = 80)
    private String fieldLabelSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type_snapshot", nullable = false, length = 20)
    private TicketFieldType fieldTypeSnapshot;

    @Column(name = "value_text", length = 1000)
    private String valueText;

    @Column(name = "value_number", precision = 12, scale = 2)
    private BigDecimal valueNumber;

    @Column(name = "display_value", nullable = false, length = 1000)
    private String displayValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public TicketFieldDefinition getFieldDefinition() {
        return fieldDefinition;
    }

    public void setFieldDefinition(TicketFieldDefinition fieldDefinition) {
        this.fieldDefinition = fieldDefinition;
    }

    public CategoryFieldConfig getCategoryFieldConfig() {
        return categoryFieldConfig;
    }

    public void setCategoryFieldConfig(CategoryFieldConfig categoryFieldConfig) {
        this.categoryFieldConfig = categoryFieldConfig;
    }

    public String getFieldKeySnapshot() {
        return fieldKeySnapshot;
    }

    public void setFieldKeySnapshot(String fieldKeySnapshot) {
        this.fieldKeySnapshot = fieldKeySnapshot;
    }

    public String getFieldLabelSnapshot() {
        return fieldLabelSnapshot;
    }

    public void setFieldLabelSnapshot(String fieldLabelSnapshot) {
        this.fieldLabelSnapshot = fieldLabelSnapshot;
    }

    public TicketFieldType getFieldTypeSnapshot() {
        return fieldTypeSnapshot;
    }

    public void setFieldTypeSnapshot(TicketFieldType fieldTypeSnapshot) {
        this.fieldTypeSnapshot = fieldTypeSnapshot;
    }

    public String getValueText() {
        return valueText;
    }

    public void setValueText(String valueText) {
        this.valueText = valueText;
    }

    public BigDecimal getValueNumber() {
        return valueNumber;
    }

    public void setValueNumber(BigDecimal valueNumber) {
        this.valueNumber = valueNumber;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
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
