package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.EmployeeDeviceSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeDeviceSessionRepository extends JpaRepository<EmployeeDeviceSession, Long> {

    Optional<EmployeeDeviceSession> findByRefreshTokenHashAndRevokedAtIsNull(String refreshTokenHash);

    List<EmployeeDeviceSession> findAllByRevokedAtIsNullOrderByTrustedAtDesc();
}
