package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.DevicePairingApprovalRequest;
import com.ke.ticketsystemke.dto.DevicePairingRequestResponse;
import com.ke.ticketsystemke.dto.DevicePairingStartRequest;
import com.ke.ticketsystemke.dto.DevicePairingStartResponse;
import com.ke.ticketsystemke.dto.DevicePairingStatusRequest;
import com.ke.ticketsystemke.dto.DevicePairingStatusResponse;
import com.ke.ticketsystemke.dto.TrustedDeviceResponse;
import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.service.AccessService;
import com.ke.ticketsystemke.service.DevicePairingService;
import com.ke.ticketsystemke.service.DevicePairingService.StatusClaim;
import com.ke.ticketsystemke.service.RefreshTokenService.IssuedRefreshToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/volt/auth/device-pairing")
public class DevicePairingController {

    private static final String REFRESH_TOKEN_COOKIE = "ke_refresh_token";
    private static final String REFRESH_TOKEN_COOKIE_PATH = "/volt/auth";

    private final DevicePairingService devicePairingService;
    private final AccessService accessService;

    public DevicePairingController(DevicePairingService devicePairingService, AccessService accessService) {
        this.devicePairingService = devicePairingService;
        this.accessService = accessService;
    }

    @PostMapping("/requests")
    public DevicePairingStartResponse start(@RequestBody(required = false) DevicePairingStartRequest request) {
        return devicePairingService.start(request);
    }

    @PostMapping("/status")
    public ResponseEntity<DevicePairingStatusResponse> status(
            @RequestBody DevicePairingStatusRequest request,
            HttpServletRequest servletRequest
    ) {
        StatusClaim claim = devicePairingService.claimStatus(
                request == null ? null : request.getRequestId(),
                request == null ? null : request.getNonce(),
                request == null ? null : request.getDeviceFingerprint(),
                userAgent(servletRequest),
                ipAddress(servletRequest)
        );

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        if (claim.refreshToken() != null) {
            builder.header(HttpHeaders.SET_COOKIE, refreshCookie(claim.refreshToken()).toString());
        }
        return builder.body(new DevicePairingStatusResponse(claim.status(), claim.expiresAt(), claim.loginResponse()));
    }

    @PostMapping("/cancel")
    public DevicePairingRequestResponse cancel(@RequestBody DevicePairingStatusRequest request) {
        return devicePairingService.cancel(
                request == null ? null : request.getRequestId(),
                request == null ? null : request.getNonce()
        );
    }

    @GetMapping("/pending")
    public List<DevicePairingRequestResponse> pending(Authentication authentication) {
        requirePairingManager(authentication);
        return devicePairingService.pendingRequests();
    }

    @GetMapping("/trusted-devices")
    public List<TrustedDeviceResponse> trustedDevices(Authentication authentication) {
        requirePairingManager(authentication);
        return devicePairingService.trustedDevices();
    }

    @PatchMapping("/approve")
    public DevicePairingRequestResponse approve(
            @RequestBody DevicePairingApprovalRequest request,
            Authentication authentication
    ) {
        requirePairingManager(authentication);
        return devicePairingService.approve(request, authentication.getName());
    }

    @PatchMapping("/trusted-devices/{deviceSessionId}/revoke")
    public TrustedDeviceResponse revokeTrustedDevice(
            @PathVariable Long deviceSessionId,
            Authentication authentication
    ) {
        requirePairingManager(authentication);
        return devicePairingService.revokeTrustedDevice(deviceSessionId, authentication.getName());
    }

    private void requirePairingManager(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        accessService.requireAnyAllowed(
                authentication.getName(),
                AccessKey.MANAGE_EMPLOYEES,
                AccessKey.MANAGE_ROLES
        );
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
}
