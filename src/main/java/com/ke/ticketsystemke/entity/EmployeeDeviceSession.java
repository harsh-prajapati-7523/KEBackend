package com.ke.ticketsystemke.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
        name = "employee_device_sessions",
        indexes = {
                @Index(name = "idx_employee_device_sessions_employee", columnList = "employee_id"),
                @Index(name = "idx_employee_device_sessions_refresh_token_hash", columnList = "refresh_token_hash")
        }
)
public class EmployeeDeviceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "device_fingerprint", length = 256)
    private String deviceFingerprint;

    @Column(name = "device_label", length = 120)
    private String deviceLabel;

    @Column(name = "refresh_token_hash", nullable = false, length = 64)
    private String refreshTokenHash;

    @Column(name = "trusted_at", nullable = false)
    private Instant trustedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_employee_id")
    private Employee approvedByEmployee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pairing_request_id")
    private DevicePairingRequest pairingRequest;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public String getDeviceFingerprint() {
        return deviceFingerprint;
    }

    public void setDeviceFingerprint(String deviceFingerprint) {
        this.deviceFingerprint = deviceFingerprint;
    }

    public String getDeviceLabel() {
        return deviceLabel;
    }

    public void setDeviceLabel(String deviceLabel) {
        this.deviceLabel = deviceLabel;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public void setRefreshTokenHash(String refreshTokenHash) {
        this.refreshTokenHash = refreshTokenHash;
    }

    public Instant getTrustedAt() {
        return trustedAt;
    }

    public void setTrustedAt(Instant trustedAt) {
        this.trustedAt = trustedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public Employee getApprovedByEmployee() {
        return approvedByEmployee;
    }

    public void setApprovedByEmployee(Employee approvedByEmployee) {
        this.approvedByEmployee = approvedByEmployee;
    }

    public DevicePairingRequest getPairingRequest() {
        return pairingRequest;
    }

    public void setPairingRequest(DevicePairingRequest pairingRequest) {
        this.pairingRequest = pairingRequest;
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
        if (trustedAt == null) {
            trustedAt = now;
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
        updatedAt = Instant.now();
    }
}
