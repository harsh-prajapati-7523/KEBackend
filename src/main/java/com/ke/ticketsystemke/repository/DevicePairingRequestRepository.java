package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.DevicePairingRequest;
import com.ke.ticketsystemke.entity.DevicePairingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DevicePairingRequestRepository extends JpaRepository<DevicePairingRequest, Long> {

    Optional<DevicePairingRequest> findByRequestId(String requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select request from DevicePairingRequest request where request.requestId = :requestId")
    Optional<DevicePairingRequest> findWithLockByRequestId(@Param("requestId") String requestId);

    List<DevicePairingRequest> findTop25ByStatusAndExpiresAtAfterOrderByCreatedAtDesc(DevicePairingStatus status, Instant now);

    List<DevicePairingRequest> findAllByStatusAndExpiresAtBefore(DevicePairingStatus status, Instant now);

    List<DevicePairingRequest> findAllByEmployeeEmployeeIdIgnoreCaseAndDeviceFingerprintAndStatus(
            String employeeId,
            String deviceFingerprint,
            DevicePairingStatus status
    );
}
