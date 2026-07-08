package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.EmployeeRefreshToken;
import com.ke.ticketsystemke.entity.RefreshTokenAuthLevel;
import com.ke.ticketsystemke.repository.EmployeeDeviceSessionRepository;
import com.ke.ticketsystemke.repository.EmployeeRefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 32;

    private final EmployeeRefreshTokenRepository refreshTokenRepository;
    private final EmployeeDeviceSessionRepository deviceSessionRepository;
    private final Duration refreshTokenTtl;

    public RefreshTokenService(
            EmployeeRefreshTokenRepository refreshTokenRepository,
            EmployeeDeviceSessionRepository deviceSessionRepository,
            @Value("${auth.refresh-token.expiration-days:${AUTH_REFRESH_TOKEN_EXPIRATION_DAYS:30}}") long refreshTokenExpirationDays
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.deviceSessionRepository = deviceSessionRepository;
        this.refreshTokenTtl = Duration.ofDays(refreshTokenExpirationDays);
    }

    @Transactional
    public IssuedRefreshToken issue(Employee employee, String userAgent, String ipAddress) {
        return issue(employee, userAgent, ipAddress, RefreshTokenAuthLevel.FULL);
    }

    @Transactional
    public IssuedRefreshToken issue(Employee employee, String userAgent, String ipAddress, RefreshTokenAuthLevel authLevel) {
        String rawToken = generateToken();
        String tokenHash = hash(rawToken);
        Instant now = Instant.now();

        EmployeeRefreshToken refreshToken = new EmployeeRefreshToken();
        refreshToken.setEmployee(employee);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setIssuedAt(now);
        refreshToken.setExpiresAt(now.plus(refreshTokenTtl));
        refreshToken.setUserAgent(limit(userAgent, 512));
        refreshToken.setIpAddress(limit(ipAddress, 64));
        refreshToken.setAuthLevel(authLevel);
        refreshTokenRepository.save(refreshToken);

        return new IssuedRefreshToken(rawToken, tokenHash, refreshToken.getExpiresAt(), refreshTokenTtl);
    }

    @Transactional(readOnly = true)
    public EmployeeRefreshToken validate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidRefreshToken();
        }

        EmployeeRefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(this::invalidRefreshToken);

        if (refreshToken.getRevokedAt() != null || !refreshToken.getExpiresAt().isAfter(Instant.now())) {
            throw invalidRefreshToken();
        }

        Employee employee = refreshToken.getEmployee();
        if (employee == null || !employee.isActive()) {
            throw invalidRefreshToken();
        }

        return refreshToken;
    }

    @Transactional
    public IssuedRefreshToken rotate(String rawToken, String userAgent, String ipAddress) {
        EmployeeRefreshToken currentToken = validateWithLock(rawToken);
        IssuedRefreshToken nextToken = issue(currentToken.getEmployee(), userAgent, ipAddress, currentToken.getAuthLevel());
        currentToken.setRevokedAt(Instant.now());
        currentToken.setReplacedByTokenHash(nextToken.tokenHash());
        refreshTokenRepository.save(currentToken);
        deviceSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(currentToken.getTokenHash())
                .ifPresent(deviceSession -> {
                    deviceSession.setRefreshTokenHash(nextToken.tokenHash());
                    deviceSessionRepository.save(deviceSession);
                });
        return nextToken;
    }

    @Transactional
    public IssuedRefreshToken rotateToFull(String rawToken, String userAgent, String ipAddress) {
        EmployeeRefreshToken currentToken = validateWithLock(rawToken);
        IssuedRefreshToken nextToken = issue(currentToken.getEmployee(), userAgent, ipAddress, RefreshTokenAuthLevel.FULL);
        currentToken.setRevokedAt(Instant.now());
        currentToken.setReplacedByTokenHash(nextToken.tokenHash());
        refreshTokenRepository.save(currentToken);
        deviceSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(currentToken.getTokenHash())
                .ifPresent(deviceSession -> {
                    deviceSession.setRefreshTokenHash(nextToken.tokenHash());
                    deviceSessionRepository.save(deviceSession);
                });
        return nextToken;
    }

    @Transactional(readOnly = true)
    public EmployeeRefreshToken validateFull(String rawToken) {
        EmployeeRefreshToken refreshToken = validate(rawToken);
        if (refreshToken.getAuthLevel() != RefreshTokenAuthLevel.FULL) {
            throw invalidRefreshToken();
        }
        return refreshToken;
    }

    @Transactional(readOnly = true)
    public EmployeeRefreshToken validateFullByTokenHash(String tokenHash) {
        EmployeeRefreshToken refreshToken = validateByTokenHash(tokenHash);
        if (refreshToken.getAuthLevel() != RefreshTokenAuthLevel.FULL) {
            throw invalidRefreshToken();
        }
        return refreshToken;
    }

    @Transactional(readOnly = true)
    public EmployeeRefreshToken validatePrePinByTokenHash(String tokenHash) {
        EmployeeRefreshToken refreshToken = validateByTokenHash(tokenHash);
        if (refreshToken.getAuthLevel() != RefreshTokenAuthLevel.PRE_PIN) {
            throw invalidRefreshToken();
        }
        return refreshToken;
    }

    private EmployeeRefreshToken validateByTokenHash(String tokenHash) {
        if (tokenHash == null || tokenHash.isBlank()) {
            throw invalidRefreshToken();
        }
        EmployeeRefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(this::invalidRefreshToken);
        if (refreshToken.getRevokedAt() != null || !refreshToken.getExpiresAt().isAfter(Instant.now())) {
            throw invalidRefreshToken();
        }
        Employee employee = refreshToken.getEmployee();
        if (employee == null || !employee.isActive()) {
            throw invalidRefreshToken();
        }
        return refreshToken;
    }

    private EmployeeRefreshToken validateWithLock(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw invalidRefreshToken();
        }

        EmployeeRefreshToken refreshToken = refreshTokenRepository.findWithLockByTokenHash(hash(rawToken))
                .orElseThrow(this::invalidRefreshToken);

        if (refreshToken.getRevokedAt() != null || !refreshToken.getExpiresAt().isAfter(Instant.now())) {
            throw invalidRefreshToken();
        }

        Employee employee = refreshToken.getEmployee();
        if (employee == null || !employee.isActive()) {
            throw invalidRefreshToken();
        }

        return refreshToken;
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(refreshToken -> {
            if (refreshToken.getRevokedAt() == null) {
                refreshToken.setRevokedAt(Instant.now());
                refreshTokenRepository.save(refreshToken);
            }
        });
    }

    @Transactional
    public void revokeByTokenHash(String tokenHash) {
        if (tokenHash == null || tokenHash.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(refreshToken -> {
            if (refreshToken.getRevokedAt() == null) {
                refreshToken.setRevokedAt(Instant.now());
                refreshTokenRepository.save(refreshToken);
            }
        });
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private ResponseStatusException invalidRefreshToken() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid session");
    }

    public record IssuedRefreshToken(String rawToken, String tokenHash, Instant expiresAt, Duration maxAge) {
    }
}
