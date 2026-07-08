package com.ke.ticketsystemke.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import com.ke.ticketsystemke.dto.DevicePairingApprovalRequest;
import com.ke.ticketsystemke.dto.DevicePairingRequestResponse;
import com.ke.ticketsystemke.dto.TrustedDeviceResponse;
import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.service.AccessService;
import com.ke.ticketsystemke.service.DevicePairingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class DevicePairingControllerTest {

    @Mock
    private DevicePairingService devicePairingService;

    @Mock
    private AccessService accessService;

    private DevicePairingController controller;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        controller = new DevicePairingController(devicePairingService, accessService);
        authentication = new UsernamePasswordAuthenticationToken("EMP001", null);
    }

    @Test
    void pendingRequestsRequireDevicePairingApprovalAccess() {
        when(devicePairingService.pendingRequests()).thenReturn(List.of());

        assertThat(controller.pending(authentication)).isEmpty();

        verify(accessService).requireAnyAllowed("EMP001", AccessKey.APPROVE_DEVICE_PAIRING);
    }

    @Test
    void trustedDevicesRequireDevicePairingApprovalAccess() {
        when(devicePairingService.trustedDevices()).thenReturn(List.of());

        assertThat(controller.trustedDevices(authentication)).isEmpty();

        verify(accessService).requireAnyAllowed("EMP001", AccessKey.APPROVE_DEVICE_PAIRING);
    }

    @Test
    void approveRequiresDevicePairingApprovalAccess() {
        DevicePairingApprovalRequest request = new DevicePairingApprovalRequest();
        DevicePairingRequestResponse response = new DevicePairingRequestResponse(
                "request-1",
                "APPROVED",
                "Android browser",
                "fingerprint",
                Instant.now(),
                Instant.now().plusSeconds(60),
                Instant.now(),
                "EMP002",
                "Employee Two"
        );
        when(devicePairingService.approve(request, "EMP001")).thenReturn(response);

        assertThat(controller.approve(request, authentication)).isSameAs(response);

        verify(accessService).requireAnyAllowed("EMP001", AccessKey.APPROVE_DEVICE_PAIRING);
    }

    @Test
    void revokeTrustedDeviceRequiresDevicePairingApprovalAccess() {
        TrustedDeviceResponse response = new TrustedDeviceResponse(
                10L,
                "EMP002",
                "Employee Two",
                "Android browser",
                "fingerprint",
                Instant.now(),
                Instant.now(),
                "EMP001"
        );
        when(devicePairingService.revokeTrustedDevice(10L, "EMP001")).thenReturn(response);

        assertThat(controller.revokeTrustedDevice(10L, authentication)).isSameAs(response);

        verify(accessService).requireAnyAllowed("EMP001", AccessKey.APPROVE_DEVICE_PAIRING);
    }
}
