package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.AuthenticationConfigRequest;
import com.ke.ticketsystemke.dto.AuthenticationConfigResponse;
import com.ke.ticketsystemke.dto.ChangePasswordRequest;
import com.ke.ticketsystemke.dto.LoginRequest;
import com.ke.ticketsystemke.dto.LoginResponse;
import com.ke.ticketsystemke.dto.MessageResponse;
import com.ke.ticketsystemke.dto.PinLoginRequest;
import com.ke.ticketsystemke.dto.PinLoginStatusResponse;
import com.ke.ticketsystemke.dto.PinSetupRequest;
import com.ke.ticketsystemke.dto.PinSetupResponse;
import com.ke.ticketsystemke.entity.AuthenticationMode;
import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.EmployeeRefreshToken;
import com.ke.ticketsystemke.entity.RefreshTokenAuthLevel;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.repository.RoleRepository;
import com.ke.ticketsystemke.security.JwtService;
import com.ke.ticketsystemke.service.EmployeeService;
import com.ke.ticketsystemke.service.RefreshTokenService;
import com.ke.ticketsystemke.service.RefreshTokenService.IssuedRefreshToken;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

@RestController
@RequestMapping("/volt/auth")
public class AuthController {

        private static final Logger log = LoggerFactory.getLogger(AuthController.class);
        private static final int MAX_FAILED_LOGIN_ATTEMPTS = 3;
        private static final int MAX_FAILED_PIN_ATTEMPTS = 5;
        private static final String SIX_DIGIT_PIN_PATTERN = "\\d{6}";
        private static final String REFRESH_TOKEN_COOKIE = "ke_refresh_token";
        private static final String REFRESH_TOKEN_COOKIE_PATH = "/volt/auth";

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @PostMapping("/config")
    @Transactional(readOnly = true)
    public ResponseEntity<?> authenticationConfig(@RequestBody AuthenticationConfigRequest request) {
        String employeeId = request == null || request.getEmployeeId() == null ? "" : request.getEmployeeId().trim();
        Employee employee = employeeId.isBlank()
                ? null
                : employeeRepository.findByEmployeeIdIgnoreCase(employeeId).orElse(null);
        if (employee == null || !employee.isActive() || hasInactiveRole(employee)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Authentication configuration unavailable");
        }
        return ResponseEntity.ok(new AuthenticationConfigResponse(resolveAuthenticationMode(employee)));
    }

    @PostMapping("/employeelogin")
    @Transactional
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {

        String employeeId = request == null || request.getEmployeeId() == null ? "" : request.getEmployeeId().trim();
        log.info("event=login_attempt employeeId={}", employeeId);

        Employee employee = employeeId.isBlank()
                ? null
                : employeeRepository.findByEmployeeIdIgnoreCase(employeeId).orElse(null);

        if (employee == null ||
                !employee.isActive() ||
                hasInactiveRole(employee)) {
            log.warn("event=login_failure employeeId={}", employeeId);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid employee ID or password");
        }

        if (employee.isAccountLocked()) {
            log.warn("event=login_failure employeeId={} reason=account_locked", employeeId);
            return ResponseEntity
                    .status(HttpStatus.LOCKED)
                    .body("Account is locked. Please contact an administrator to reset your password.");
        }

        if (resolveAuthenticationMode(employee) != AuthenticationMode.PASSWORD_PIN) {
            log.warn("event=login_failure employeeId={} reason=password_flow_not_allowed", employeeId);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Password login is not enabled for this role");
        }

        String credential = request == null ? null : request.getPassword();
        if (credential == null ||
                !passwordEncoder.matches(
                        credential,
                        employee.getPassword()
                )) {
            recordFailedLogin(employee, employeeId);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid employee ID or password");
        }

        if (employee.getFailedLoginAttempts() != 0) {
            employee.setFailedLoginAttempts(0);
            employeeRepository.save(employee);
        }

        String role = resolveRoleKey(employee);
        boolean needsPinSetup = employee.getPinHash() == null;
        IssuedRefreshToken refreshToken = refreshTokenService.issue(employee, userAgent(servletRequest), ipAddress(servletRequest), RefreshTokenAuthLevel.PRE_PIN);
        String token = needsPinSetup
                ? jwtService.generatePinSetupToken(employee.getEmployeeId(), refreshToken.tokenHash())
                : null;

        log.info("event=login_success employeeId={} role={}", employeeId, role);

        return withRefreshCookie(
                ResponseEntity.ok(),
                refreshToken
        ).body(loginResponse(employee, token, role));
    }

    @PostMapping("/refresh")
    @Transactional
    public ResponseEntity<?> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String rawRefreshToken,
            HttpServletRequest servletRequest
    ) {
        try {
            EmployeeRefreshToken refreshToken = refreshTokenService.validateFull(rawRefreshToken);
            Employee employee = refreshToken.getEmployee();
            if (hasInactiveRole(employee)) {
                throw new IllegalStateException("Inactive role");
            }

            IssuedRefreshToken rotatedRefreshToken = refreshTokenService.rotate(rawRefreshToken, userAgent(servletRequest), ipAddress(servletRequest));
            String accessToken = jwtService.generateToken(employee.getEmployeeId(), rotatedRefreshToken.tokenHash());
            log.info("event=refresh_success employeeId={}", employee.getEmployeeId());
            return withRefreshCookie(ResponseEntity.ok(), rotatedRefreshToken)
                    .body(loginResponse(employee, accessToken, resolveRoleKey(employee)));
        } catch (Exception ex) {
            log.warn("event=refresh_failure reason={}", ex.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid session");
        }
    }

    @PostMapping("/pin/setup")
    @Transactional
    public ResponseEntity<?> setupPin(
            @RequestBody PinSetupRequest request,
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String rawRefreshToken,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid session");
        }

        EmployeeRefreshToken refreshToken;
        try {
            refreshToken = refreshTokenService.validate(rawRefreshToken);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid session");
        }

        Employee employee = refreshToken.getEmployee();
        if (!employee.getEmployeeId().equalsIgnoreCase(authentication.getName())) {
            log.warn("event=pin_setup_denied employeeId={} reason=session_mismatch", authentication.getName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid session");
        }

        String pin = request == null ? null : request.getPin();
        String confirmPin = request == null ? null : request.getConfirmPin();
        if (pin == null || !pin.matches(SIX_DIGIT_PIN_PATTERN)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("PIN must be exactly 6 digits");
        }
        if (!pin.equals(confirmPin)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("PIN and confirm PIN must match");
        }

        Instant now = Instant.now();
        employee.setPinHash(passwordEncoder.encode(pin));
        if (employee.getPinSetAt() == null) {
            employee.setPinSetAt(now);
        }
        employee.setPinUpdatedAt(now);
        employee.setPinFailedAttempts(0);
        employeeRepository.save(employee);
        log.info("event=pin_setup_success employeeId={}", employee.getEmployeeId());
        return ResponseEntity.ok(new PinSetupResponse(true, true));
    }

    @PostMapping("/pin/login")
    @Transactional
    public ResponseEntity<?> pinLogin(
            @RequestBody PinLoginRequest request,
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String rawRefreshToken,
            HttpServletRequest servletRequest
    ) {
        try {
            EmployeeRefreshToken refreshToken = refreshTokenService.validate(rawRefreshToken);
            Employee employee = refreshToken.getEmployee();
            if (hasInactiveRole(employee) || employee.getPinHash() == null) {
                throw new IllegalStateException("PIN unavailable");
            }

            String pin = request == null ? null : request.getPin();
            if (pin == null || !pin.matches(SIX_DIGIT_PIN_PATTERN) || !passwordEncoder.matches(pin, employee.getPinHash())) {
                return recordFailedPinLogin(employee, rawRefreshToken);
            }

            if (employee.getPinFailedAttempts() != 0) {
                employee.setPinFailedAttempts(0);
                employeeRepository.save(employee);
            }
            IssuedRefreshToken rotatedRefreshToken = refreshTokenService.rotateToFull(rawRefreshToken, userAgent(servletRequest), ipAddress(servletRequest));
            String accessToken = jwtService.generateToken(employee.getEmployeeId(), rotatedRefreshToken.tokenHash());
            log.info("event=pin_login_success employeeId={}", employee.getEmployeeId());
            return withRefreshCookie(ResponseEntity.ok(), rotatedRefreshToken)
                    .body(loginResponse(employee, accessToken, resolveRoleKey(employee)));
        } catch (Exception ex) {
            log.warn("event=pin_login_failure reason={}", ex.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid session or PIN");
        }
    }

    @PostMapping("/pin/status")
    @Transactional(readOnly = true)
    public PinLoginStatusResponse pinLoginStatus(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String rawRefreshToken
    ) {
        try {
            EmployeeRefreshToken refreshToken = refreshTokenService.validate(rawRefreshToken);
            Employee employee = refreshToken.getEmployee();
            boolean pinAvailable = employee.getPinHash() != null && !hasInactiveRole(employee);
            return new PinLoginStatusResponse(pinAvailable, pinAvailable ? employee.getName() : null, true);
        } catch (Exception ex) {
            return new PinLoginStatusResponse(false, null, false);
        }
    }

    @PostMapping("/pin/reset")
    @Transactional
    public ResponseEntity<MessageResponse> resetPin(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String rawRefreshToken
    ) {
        try {
            EmployeeRefreshToken refreshToken = refreshTokenService.validate(rawRefreshToken);
            Employee employee = refreshToken.getEmployee();
            clearEmployeePin(employee);
            employeeRepository.save(employee);
            log.info("event=pin_reset_success employeeId={}", employee.getEmployeeId());
        } catch (Exception ex) {
            log.warn("event=pin_reset_without_valid_session reason={}", ex.getClass().getSimpleName());
        }

        refreshTokenService.revoke(rawRefreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .body(new MessageResponse("PIN reset successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String rawRefreshToken
    ) {
        refreshTokenService.revoke(rawRefreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .body(new MessageResponse("Logged out successfully"));
    }

    private void recordFailedLogin(Employee employee, String employeeId) {
        int failedAttempts = employee.getFailedLoginAttempts() + 1;
        employee.setFailedLoginAttempts(failedAttempts);
        if (failedAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
            employee.setAccountLocked(true);
            log.warn("event=account_locked employeeId={} failedAttempts={}", employeeId, failedAttempts);
        }
        employeeRepository.save(employee);
        log.warn("event=login_failure employeeId={} failedAttempts={}", employeeId, failedAttempts);
    }

    private ResponseEntity<MessageResponse> recordFailedPinLogin(Employee employee, String rawRefreshToken) {
        int failedAttempts = employee.getPinFailedAttempts() + 1;
        if (failedAttempts >= MAX_FAILED_PIN_ATTEMPTS) {
            clearEmployeePin(employee);
            employeeRepository.save(employee);
            refreshTokenService.revoke(rawRefreshToken);
            log.warn("event=pin_reset_after_failed_attempts employeeId={} failedAttempts={}", employee.getEmployeeId(), failedAttempts);
            return ResponseEntity
                    .status(HttpStatus.LOCKED)
                    .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                    .body(new MessageResponse("PIN reset after too many invalid attempts"));
        }

        employee.setPinFailedAttempts(failedAttempts);
        employeeRepository.save(employee);
        log.warn("event=pin_login_failure employeeId={} failedAttempts={}", employee.getEmployeeId(), failedAttempts);
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Invalid session or PIN"));
    }

    private void clearEmployeePin(Employee employee) {
        employee.setPinHash(null);
        employee.setPinSetAt(null);
        employee.setPinUpdatedAt(null);
        employee.setPinFailedAttempts(0);
    }

    @PatchMapping("/change-password")
    public MessageResponse changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        employeeService.changeOwnPassword(authentication.getName(), request);
        return new MessageResponse("Password changed successfully");
    }

    private LoginResponse loginResponse(Employee employee, String accessToken, String role) {
        return new LoginResponse(
                accessToken,
                employee.getName(),
                role,
                employee.getEmployeeId(),
                employee.getPinHash() == null
        );
    }

    private ResponseEntity.BodyBuilder withRefreshCookie(ResponseEntity.BodyBuilder builder, IssuedRefreshToken refreshToken) {
        return builder.header(HttpHeaders.SET_COOKIE, refreshCookie(refreshToken).toString());
    }

    private ResponseCookie refreshCookie(IssuedRefreshToken refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken.rawToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .maxAge(refreshToken.maxAge())
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .maxAge(0)
                .build();
    }

    private String userAgent(HttpServletRequest request) {
        return request == null ? null : request.getHeader("User-Agent");
    }

    private String ipAddress(HttpServletRequest request) {
        if (request == null) return null;
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveRoleKey(Employee employee) {
        if (employee.getRoleRecord() != null) {
            return employee.getRoleRecord().getRoleKey();
        }
        return employee.getRole() == null ? null : employee.getRole().name();
    }

    private AuthenticationMode resolveAuthenticationMode(Employee employee) {
        String role = resolveRoleKey(employee);
        if ("SUPER_ADMIN".equals(role)) {
            return AuthenticationMode.PASSWORD_PIN;
        }
        if (employee.getRoleRecord() != null) {
            return employee.getRoleRecord().getAuthenticationMode();
        }
        return AuthenticationMode.PASSWORD_PIN;
    }

    private boolean hasInactiveRole(Employee employee) {
        if (employee.getRoleRecord() != null) {
            return !employee.getRoleRecord().isActive();
        }
        if (employee.getRole() != null) {
            return roleRepository.findByRoleKey(employee.getRole().name())
                    .map(role -> !role.isActive())
                    .orElse(true);
        }
        return true;
    }
}
