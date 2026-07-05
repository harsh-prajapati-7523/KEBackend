package com.ke.ticketsystemke.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "speech_usage_monthly",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_speech_usage_monthly_provider_month",
                columnNames = {"provider_key", "year_month"}
        )
)
public class SpeechUsageMonthly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    @Column(name = "provider_key", nullable = false, length = 40)
    private String providerKey;

    @Column(name = "used_seconds", nullable = false)
    private long usedSeconds;

    @Column(name = "monthly_limit_seconds", nullable = false)
    private long monthlyLimitSeconds;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public String getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(String yearMonth) {
        this.yearMonth = yearMonth;
    }

    public String getProviderKey() {
        return providerKey;
    }

    public void setProviderKey(String providerKey) {
        this.providerKey = providerKey;
    }

    public long getUsedSeconds() {
        return usedSeconds;
    }

    public void setUsedSeconds(long usedSeconds) {
        this.usedSeconds = usedSeconds;
    }

    public long getMonthlyLimitSeconds() {
        return monthlyLimitSeconds;
    }

    public void setMonthlyLimitSeconds(long monthlyLimitSeconds) {
        this.monthlyLimitSeconds = monthlyLimitSeconds;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
    }

    @PreUpdate
    void setUpdateTimestamp() {
        updatedAt = Instant.now();
    }
}
