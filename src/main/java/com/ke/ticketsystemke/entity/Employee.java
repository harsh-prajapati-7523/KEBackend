package com.ke.ticketsystemke.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String employeeId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    @Column
    private String pinHash;

    @Column
    private Instant pinSetAt;

    @Column
    private Instant pinUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column
    private EmployeeRole role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role roleRecord;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int failedLoginAttempts = 0;

    @Column(nullable = false, columnDefinition = "integer default 0")
    private int pinFailedAttempts = 0;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean accountLocked = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Employee() {
    }

    public Long getId() {
        return id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPinHash() {
        return pinHash;
    }

    public void setPinHash(String pinHash) {
        this.pinHash = pinHash;
    }

    public Instant getPinSetAt() {
        return pinSetAt;
    }

    public void setPinSetAt(Instant pinSetAt) {
        this.pinSetAt = pinSetAt;
    }

    public Instant getPinUpdatedAt() {
        return pinUpdatedAt;
    }

    public void setPinUpdatedAt(Instant pinUpdatedAt) {
        this.pinUpdatedAt = pinUpdatedAt;
    }

    public EmployeeRole getRole() {
        return role;
    }

    public void setRole(EmployeeRole role) {
        this.role = role;
    }

    public Role getRoleRecord() {
        return roleRecord;
    }

    public void setRoleRecord(Role roleRecord) {
        this.roleRecord = roleRecord;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public int getPinFailedAttempts() {
        return pinFailedAttempts;
    }

    public void setPinFailedAttempts(int pinFailedAttempts) {
        this.pinFailedAttempts = pinFailedAttempts;
    }

    public boolean isAccountLocked() {
        return accountLocked;
    }

    public void setAccountLocked(boolean accountLocked) {
        this.accountLocked = accountLocked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @PrePersist
    void setCreationTimestamp() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
