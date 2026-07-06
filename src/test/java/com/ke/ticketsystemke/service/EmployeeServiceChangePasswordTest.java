package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.ChangePasswordRequest;
import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.Role;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceChangePasswordTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private EmployeeService employeeService;

    @BeforeEach
    void setUp() {
        employeeService = new EmployeeService(employeeRepository, roleRepository, passwordEncoder);
    }

    @Test
    void changeOwnPasswordValidatesCurrentPasswordAndSavesEncodedNewPassword() {
        Employee employee = employee("EMP001", "existing-hash", "SUPER_ADMIN");
        ChangePasswordRequest request = request("oldPassword", "newPassword123", "newPassword123");

        when(employeeRepository.findByEmployeeIdIgnoreCase("EMP001")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("oldPassword", "existing-hash")).thenReturn(true);
        when(passwordEncoder.matches("newPassword123", "existing-hash")).thenReturn(false);
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-hash");
        when(employeeRepository.save(employee)).thenReturn(employee);

        employeeService.changeOwnPassword("EMP001", request);

        assertThat(employee.getPassword()).isEqualTo("new-hash");
        verify(employeeRepository).save(employee);
    }

    @Test
    void changeOwnPasswordRejectsIncorrectCurrentPassword() {
        Employee employee = employee("EMP001", "existing-hash", "SUPER_ADMIN");
        ChangePasswordRequest request = request("wrongPassword", "newPassword123", "newPassword123");

        when(employeeRepository.findByEmployeeIdIgnoreCase("EMP001")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("wrongPassword", "existing-hash")).thenReturn(false);

        assertThatThrownBy(() -> employeeService.changeOwnPassword("EMP001", request))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).isEqualTo("Current password is incorrect");
                });

        verify(employeeRepository, never()).save(employee);
    }

    @Test
    void changeOwnPasswordRejectsMismatchedConfirmation() {
        ChangePasswordRequest request = request("oldPassword", "newPassword123", "differentPassword123");

        assertThatThrownBy(() -> employeeService.changeOwnPassword("EMP001", request))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).isEqualTo("New password and confirm password must match");
                });

        verify(employeeRepository, never()).findByEmployeeIdIgnoreCase("EMP001");
    }

    @Test
    void changeOwnPasswordRejectsShortNewPassword() {
        ChangePasswordRequest request = request("oldPassword", "short", "short");

        assertThatThrownBy(() -> employeeService.changeOwnPassword("EMP001", request))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).isEqualTo("New password must contain at least 8 characters");
                });

        verify(employeeRepository, never()).findByEmployeeIdIgnoreCase("EMP001");
    }

    @Test
    void changeOwnPasswordRejectsSamePassword() {
        Employee employee = employee("EMP001", "existing-hash", "SUPER_ADMIN");
        ChangePasswordRequest request = request("oldPassword", "oldPassword", "oldPassword");

        when(employeeRepository.findByEmployeeIdIgnoreCase("EMP001")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("oldPassword", "existing-hash")).thenReturn(true, true);

        assertThatThrownBy(() -> employeeService.changeOwnPassword("EMP001", request))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getReason()).isEqualTo("New password must be different from current password");
                });

        verify(employeeRepository, never()).save(employee);
    }

    @Test
    void resetPasswordUnlocksAccountAndClearsFailedLoginAttempts() {
        Employee employee = employee("EMP001", "existing-hash", "SUPER_ADMIN");
        employee.setAccountLocked(true);
        employee.setFailedLoginAttempts(3);

        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-hash");
        when(employeeRepository.save(employee)).thenReturn(employee);

        employeeService.resetPassword(10L, "newPassword123", "ADMIN001");

        assertThat(employee.getPassword()).isEqualTo("new-hash");
        assertThat(employee.isAccountLocked()).isFalse();
        assertThat(employee.getFailedLoginAttempts()).isZero();
        verify(employeeRepository).save(employee);
    }

    private Employee employee(String employeeId, String password, String roleKey) {
        Role role = new Role();
        role.setRoleKey(roleKey);
        role.setDisplayName(roleKey);
        role.setActive(true);

        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setName("Employee");
        employee.setPassword(password);
        employee.setRoleRecord(role);
        return employee;
    }

    private ChangePasswordRequest request(String currentPassword, String newPassword, String confirmPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(currentPassword);
        request.setNewPassword(newPassword);
        request.setConfirmPassword(confirmPassword);
        return request;
    }
}
