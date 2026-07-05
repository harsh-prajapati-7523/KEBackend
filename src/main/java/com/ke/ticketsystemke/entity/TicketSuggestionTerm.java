package com.ke.ticketsystemke.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "ticket_suggestion_terms",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ticket_suggestion_terms_type_normalized",
                columnNames = {"suggestion_type", "normalized_value"}
        ),
        indexes = {
                @Index(name = "idx_ticket_suggestion_terms_type_normalized", columnList = "suggestion_type, normalized_value"),
                @Index(name = "idx_ticket_suggestion_terms_type_count", columnList = "suggestion_type, usage_count")
        }
)
public class TicketSuggestionTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "suggestion_type", nullable = false, length = 30)
    private TicketSuggestionType suggestionType;

    @Column(name = "normalized_value", nullable = false, length = 255)
    private String normalizedValue;

    @Column(name = "display_value", nullable = false, length = 255)
    private String displayValue;

    @Column(name = "usage_count", nullable = false)
    private long usageCount;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public TicketSuggestionType getSuggestionType() {
        return suggestionType;
    }

    public void setSuggestionType(TicketSuggestionType suggestionType) {
        this.suggestionType = suggestionType;
    }

    public String getNormalizedValue() {
        return normalizedValue;
    }

    public void setNormalizedValue(String normalizedValue) {
        this.normalizedValue = normalizedValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public void setDisplayValue(String displayValue) {
        this.displayValue = displayValue;
    }

    public long getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(long usageCount) {
        this.usageCount = usageCount;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @PrePersist
    void setCreationTimestamps() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (lastUsedAt == null) {
            lastUsedAt = now;
        }
    }

    @PreUpdate
    void setUpdatedTimestamp() {
        updatedAt = Instant.now();
    }
}
