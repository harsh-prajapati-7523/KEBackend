package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.DevicePairingApprovalRequest;
import com.ke.ticketsystemke.dto.DevicePairingRequestResponse;
import com.ke.ticketsystemke.dto.DevicePairingStartRequest;
import com.ke.ticketsystemke.dto.DevicePairingStartResponse;
import com.ke.ticketsystemke.dto.LoginResponse;
import com.ke.ticketsystemke.dto.TrustedDeviceResponse;
import com.ke.ticketsystemke.entity.AuthenticationMode;
import com.ke.ticketsystemke.entity.DevicePairingRequest;
import com.ke.ticketsystemke.entity.DevicePairingStatus;
import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.EmployeeDeviceSession;
import com.ke.ticketsystemke.repository.DevicePairingRequestRepository;
import com.ke.ticketsystemke.repository.EmployeeDeviceSessionRepository;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.security.JwtService;
import com.ke.ticketsystemke.service.RefreshTokenService.IssuedRefreshToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class DevicePairingService {

    private static final Logger log = LoggerFactory.getLogger(DevicePairingService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration PAIRING_TTL = Duration.ofMinutes(2);
    private static final int TOKEN_BYTE_LENGTH = 24;
    private static final String SUPER_ADMIN_ROLE_KEY = "SUPER_ADMIN";

    private final DevicePairingRequestRepository pairingRequestRepository;
    private final EmployeeDeviceSessionRepository deviceSessionRepository;
    private final EmployeeRepository employeeRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final String signingSecret;

    public DevicePairingService(
            DevicePairingRequestRepository pairingRequestRepository,
            EmployeeDeviceSessionRepository deviceSessionRepository,
            EmployeeRepository employeeRepository,
            RefreshTokenService refreshTokenService,
            JwtService jwtService,
            @Value("${device-pairing.signature-secret:${jwt.secret:${JWT_SECRET}}}") String signingSecret
    ) {
        this.pairingRequestRepository = pairingRequestRepository;
        this.deviceSessionRepository = deviceSessionRepository;
        this.employeeRepository = employeeRepository;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.signingSecret = signingSecret;
    }

    @Transactional
    public DevicePairingStartResponse start(DevicePairingStartRequest request) {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Instant expiresAt = now.plus(PAIRING_TTL);
        String requestId = randomToken();
        String nonce = randomToken();
        String signature = sign(requestId, nonce, expiresAt.toString());
        String deviceFingerprint = requireValue(request == null ? null : request.getDeviceFingerprint(), "Device fingerprint is required");
        Employee technician = employeeRepository.findByEmployeeIdIgnoreCase(requireValue(request == null ? null : request.getEmployeeId(), "Employee is required"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee not found"));
        if (!technician.isActive() || effectiveAuthenticationMode(technician) != AuthenticationMode.DEVICE_PAIRING_PIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee is not eligible for device pairing");
        }

        DevicePairingRequest pairingRequest = new DevicePairingRequest();
        pairingRequest.setRequestId(requestId);
        pairingRequest.setNonceHash(sha256(nonce));
        pairingRequest.setStatus(DevicePairingStatus.PENDING);
        pairingRequest.setDeviceFingerprint(limit(deviceFingerprint, 256));
        pairingRequest.setDeviceLabel(limit(request == null ? null : request.getDeviceLabel(), 120));
        pairingRequest.setEmployee(technician);
        pairingRequest.setExpiresAt(expiresAt);
        pairingRequestRepository.save(pairingRequest);
        log.info("event=device_pairing_requested requestId={} employeeId={} deviceLabel={} expiresAt={}",
                requestId, technician.getEmployeeId(), pairingRequest.getDeviceLabel(), expiresAt);

        String pairingToken = "{\"requestId\":\"" + requestId
                + "\",\"nonce\":\"" + nonce
                + "\",\"expiresAt\":\"" + expiresAt
                + "\",\"signature\":\"" + signature + "\"}";
        return new DevicePairingStartResponse(
                requestId,
                nonce,
                expiresAt,
                signature,
                pairingToken,
                DevicePairingStatus.PENDING.name(),
                3
        );
    }

    @Transactional(readOnly = true)
    public List<DevicePairingRequestResponse> pendingRequests() {
        return pairingRequestRepository.findTop25ByStatusAndExpiresAtAfterOrderByCreatedAtDesc(DevicePairingStatus.PENDING, Instant.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TrustedDeviceResponse> trustedDevices() {
        return deviceSessionRepository.findAllByRevokedAtIsNullOrderByTrustedAtDesc()
                .stream()
                .map(this::toTrustedDeviceResponse)
                .toList();
    }

    @Transactional
    public DevicePairingRequestResponse approve(DevicePairingApprovalRequest request, String approverEmployeeId) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pairing approval request is required");
        }

        String requestId = requireValue(request.getRequestId(), "Request ID is required");
        String nonce = requireValue(request.getNonce(), "Nonce is required");
        String signature = requireValue(request.getSignature(), "Signature is required");

        DevicePairingRequest pairingRequest = pairingRequestRepository.findWithLockByRequestId(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pairing request not found"));
        Instant now = Instant.now();
        if (!pairingRequest.getNonceHash().equals(sha256(nonce))) {
            log.warn("event=device_pairing_approval_denied requestId={} reason=nonce_mismatch", requestId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pairing token");
        }
        String signedExpiry = pairingRequest.getExpiresAt().toString();
        if (!constantTimeEquals(signature, sign(requestId, nonce, signedExpiry))) {
            log.warn("event=device_pairing_approval_denied requestId={} reason=signature_mismatch", requestId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pairing token");
        }
        if (pairingRequest.getStatus() != DevicePairingStatus.PENDING) {
            log.warn("event=device_pairing_approval_denied requestId={} reason=inactive_status status={}",
                    requestId, pairingRequest.getStatus());
            throw new ResponseStatusException(HttpStatus.GONE, "Pairing request is no longer active");
        }
        if (pairingRequest.getExpiresAt().isBefore(now)) {
            pairingRequest.setStatus(DevicePairingStatus.EXPIRED);
            pairingRequestRepository.save(pairingRequest);
            log.info("event=device_pairing_expired requestId={} deviceLabel={}", requestId, pairingRequest.getDeviceLabel());
            throw new ResponseStatusException(HttpStatus.GONE, "Pairing request is no longer active");
        }

        Employee technician = pairingRequest.getEmployee();
        if (technician == null) {
            technician = employeeRepository.findByEmployeeIdIgnoreCase(requireValue(request.getEmployeeId(), "Employee is required"))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee not found"));
            pairingRequest.setEmployee(technician);
        }
        if (!technician.isActive() || effectiveAuthenticationMode(technician) != AuthenticationMode.DEVICE_PAIRING_PIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee is not eligible for device pairing");
        }

        Employee approver = employeeRepository.findByEmployeeIdIgnoreCase(approverEmployeeId == null ? "" : approverEmployeeId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid approver"));

        pairingRequest.setApprovedByEmployee(approver);
        pairingRequest.setApprovedAt(now);
        pairingRequest.setStatus(DevicePairingStatus.APPROVED);
        log.info("event=device_pairing_approved requestId={} approverEmployeeId={} assignedEmployeeId={} deviceLabel={}",
                requestId, approver.getEmployeeId(), technician.getEmployeeId(), pairingRequest.getDeviceLabel());
        return toResponse(pairingRequestRepository.save(pairingRequest));
    }

    @Transactional
    public StatusClaim claimStatus(String requestId, String nonce, String deviceFingerprint, String userAgent, String ipAddress) {
        DevicePairingRequest pairingRequest = pairingRequestRepository.findWithLockByRequestId(requireValue(requestId, "Request ID is required"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pairing request not found"));

        if (!pairingRequest.getNonceHash().equals(sha256(requireValue(nonce, "Nonce is required")))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pairing token");
        }
        String originalFingerprint = pairingRequest.getDeviceFingerprint();
        String presentedFingerprint = requireValue(deviceFingerprint, "Device fingerprint is required");
        if (originalFingerprint == null || !originalFingerprint.equals(limit(presentedFingerprint, 256))) {
            log.warn("event=device_pairing_claim_denied requestId={} reason=device_fingerprint_mismatch",
                    pairingRequest.getRequestId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pairing device");
        }

        Instant now = Instant.now();
        if (pairingRequest.getStatus() == DevicePairingStatus.PENDING && !pairingRequest.getExpiresAt().isAfter(now)) {
            pairingRequest.setStatus(DevicePairingStatus.EXPIRED);
            pairingRequestRepository.save(pairingRequest);
            log.info("event=device_pairing_expired requestId={} deviceLabel={}",
                    pairingRequest.getRequestId(), pairingRequest.getDeviceLabel());
        }

        if (pairingRequest.getStatus() != DevicePairingStatus.APPROVED || pairingRequest.getEmployee() == null || pairingRequest.getUsedAt() != null) {
            return new StatusClaim(pairingRequest.getStatus().name(), pairingRequest.getExpiresAt(), null, null);
        }

        Employee employee = pairingRequest.getEmployee();
        if (!employee.isActive() || effectiveAuthenticationMode(employee) != AuthenticationMode.DEVICE_PAIRING_PIN) {
            pairingRequest.setStatus(DevicePairingStatus.CANCELLED);
            pairingRequestRepository.save(pairingRequest);
            log.info("event=device_pairing_cancelled requestId={} employeeId={} reason=ineligible_employee",
                    pairingRequest.getRequestId(), employee.getEmployeeId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee is not eligible for device pairing");
        }

        IssuedRefreshToken refreshToken = refreshTokenService.issue(employee, userAgent, ipAddress);
        EmployeeDeviceSession session = new EmployeeDeviceSession();
        session.setEmployee(employee);
        session.setDeviceFingerprint(limit(deviceFingerprint == null ? pairingRequest.getDeviceFingerprint() : deviceFingerprint, 256));
        session.setDeviceLabel(pairingRequest.getDeviceLabel());
        session.setRefreshTokenHash(refreshToken.tokenHash());
        session.setApprovedByEmployee(pairingRequest.getApprovedByEmployee());
        session.setPairingRequest(pairingRequest);
        session.setTrustedAt(now);
        deviceSessionRepository.save(session);

        pairingRequest.setUsedAt(now);
        pairingRequest.setStatus(DevicePairingStatus.USED);
        pairingRequestRepository.save(pairingRequest);
        log.info("event=device_session_created requestId={} employeeId={} deviceSessionId={} deviceLabel={}",
                pairingRequest.getRequestId(), employee.getEmployeeId(), session.getId(), pairingRequest.getDeviceLabel());

        String accessToken = jwtService.generateToken(employee.getEmployeeId());
        String role = effectiveRoleKey(employee);
        LoginResponse loginResponse = new LoginResponse(
                accessToken,
                employee.getName(),
                role,
                employee.getEmployeeId(),
                employee.getPinHash() == null
        );
        return new StatusClaim(DevicePairingStatus.APPROVED.name(), pairingRequest.getExpiresAt(), loginResponse, refreshToken);
    }

    @Transactional
    public DevicePairingRequestResponse cancel(String requestId, String nonce) {
        DevicePairingRequest pairingRequest = pairingRequestRepository.findWithLockByRequestId(requireValue(requestId, "Request ID is required"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pairing request not found"));
        if (!pairingRequest.getNonceHash().equals(sha256(requireValue(nonce, "Nonce is required")))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pairing token");
        }
        if (pairingRequest.getStatus() == DevicePairingStatus.PENDING) {
            pairingRequest.setStatus(DevicePairingStatus.CANCELLED);
            pairingRequestRepository.save(pairingRequest);
            log.info("event=device_pairing_cancelled requestId={} deviceLabel={}",
                    pairingRequest.getRequestId(), pairingRequest.getDeviceLabel());
        }
        return toResponse(pairingRequest);
    }

    @Transactional
    public TrustedDeviceResponse revokeTrustedDevice(Long deviceSessionId, String actorEmployeeId) {
        EmployeeDeviceSession session = deviceSessionRepository.findById(deviceSessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trusted device not found"));
        if (session.getRevokedAt() == null) {
            Instant now = Instant.now();
            session.setRevokedAt(now);
            refreshTokenService.revokeByTokenHash(session.getRefreshTokenHash());
            deviceSessionRepository.save(session);
            log.info("event=device_session_revoked deviceSessionId={} actorEmployeeId={} employeeId={} deviceLabel={}",
                    session.getId(),
                    actorEmployeeId,
                    session.getEmployee() == null ? null : session.getEmployee().getEmployeeId(),
                    session.getDeviceLabel());
        }
        return toTrustedDeviceResponse(session);
    }

    private DevicePairingRequestResponse toResponse(DevicePairingRequest request) {
        Employee employee = request.getEmployee();
        return new DevicePairingRequestResponse(
                request.getRequestId(),
                request.getStatus().name(),
                request.getDeviceLabel(),
                request.getDeviceFingerprint(),
                request.getCreatedAt(),
                request.getExpiresAt(),
                request.getApprovedAt(),
                employee == null ? null : employee.getEmployeeId(),
                employee == null ? null : employee.getName()
        );
    }

    private TrustedDeviceResponse toTrustedDeviceResponse(EmployeeDeviceSession session) {
        Employee employee = session.getEmployee();
        Employee approver = session.getApprovedByEmployee();
        return new TrustedDeviceResponse(
                session.getId(),
                employee == null ? null : employee.getEmployeeId(),
                employee == null ? null : employee.getName(),
                session.getDeviceLabel(),
                session.getDeviceFingerprint(),
                session.getTrustedAt(),
                session.getRevokedAt(),
                approver == null ? null : approver.getEmployeeId()
        );
    }

    private AuthenticationMode effectiveAuthenticationMode(Employee employee) {
        if (SUPER_ADMIN_ROLE_KEY.equals(effectiveRoleKey(employee))) {
            return AuthenticationMode.PASSWORD_PIN;
        }
        if (employee.getRoleRecord() != null) {
            return employee.getRoleRecord().getAuthenticationMode();
        }
        return AuthenticationMode.PASSWORD_PIN;
    }

    private String effectiveRoleKey(Employee employee) {
        if (employee.getRoleRecord() != null) {
            return employee.getRoleRecord().getRoleKey();
        }
        return employee.getRole() == null ? null : employee.getRole().name();
    }

    private String sign(String requestId, String nonce, String expiresAt) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((requestId + "." + nonce + "." + expiresAt).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Device pairing signing failed", ex);
        }
    }

    private boolean constantTimeEquals(String first, String second) {
        return MessageDigest.isEqual(
                first.getBytes(StandardCharsets.UTF_8),
                second.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String requireValue(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String limit(String value, int maxLength) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    public record StatusClaim(
            String status,
            Instant expiresAt,
            LoginResponse loginResponse,
            IssuedRefreshToken refreshToken
    ) {
    }
}
