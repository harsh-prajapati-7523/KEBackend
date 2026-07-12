package com.ke.ticketsystemke.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ke.ticketsystemke.dto.DynamicRoleAccessResponse;
import com.ke.ticketsystemke.dto.UpdateDynamicRoleAccessRequest;
import com.ke.ticketsystemke.entity.AuthenticationMode;
import com.ke.ticketsystemke.entity.Role;
import com.ke.ticketsystemke.repository.AccessKeyMetadataRepository;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.repository.RoleAccessRuleRepository;
import com.ke.ticketsystemke.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class RoleAccessServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private RoleAccessRuleRepository roleAccessRuleRepository;

    @Mock
    private AccessKeyMetadataRepository accessKeyMetadataRepository;

    @Mock
    private AccessService accessService;

    @Test
    void updateDynamicRoleAccessPersistsAuthenticationModeWhenRulesAreOmitted() {
        Role role = new Role();
        role.setRoleKey("TECHNICIAN");
        role.setDisplayName("Technician");
        role.setAuthenticationMode(AuthenticationMode.PASSWORD_PIN);
        role.setActive(true);

        when(roleRepository.findById(42L)).thenReturn(Optional.of(role));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(employeeRepository.findByEmployeeIdIgnoreCase("")) .thenReturn(Optional.empty());

        RoleAccessService service = new RoleAccessService(
                roleRepository,
                employeeRepository,
                roleAccessRuleRepository,
                accessKeyMetadataRepository,
                accessService
        );

        UpdateDynamicRoleAccessRequest request = new UpdateDynamicRoleAccessRequest();
        request.setAuthenticationMode(AuthenticationMode.DEVICE_PAIRING_PIN);
        request.setRules(null);

        DynamicRoleAccessResponse response = assertDoesNotThrow(() -> service.updateDynamicRoleAccess(42L, request, null));

        assertThat(response.authenticationMode()).isEqualTo(AuthenticationMode.DEVICE_PAIRING_PIN);
        assertThat(role.getAuthenticationMode()).isEqualTo(AuthenticationMode.DEVICE_PAIRING_PIN);
    }
}
