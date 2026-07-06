package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.LoginRequest;
import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.Role;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.repository.RoleRepository;
import com.ke.ticketsystemke.security.JwtService;
import com.ke.ticketsystemke.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerLoginLockTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmployeeService employeeService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController();
        ReflectionTestUtils.setField(authController, "employeeRepository", employeeRepository);
        ReflectionTestUtils.setField(authController, "roleRepository", roleRepository);
        ReflectionTestUtils.setField(authController, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(authController, "jwtService", jwtService);
        ReflectionTestUtils.setField(authController, "employeeService", employeeService);
    }

    @Test
    void loginLocksAccountAfterThirdBadPassword() {
        Employee employee = employee();
        employee.setFailedLoginAttempts(2);
        LoginRequest request = request("wrong-password");

        when(employeeRepository.findByEmployeeIdIgnoreCase("EMP001")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("wrong-password", "hash")).thenReturn(false);

        ResponseEntity<?> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(employee.getFailedLoginAttempts()).isEqualTo(3);
        assertThat(employee.isAccountLocked()).isTrue();
        verify(employeeRepository).save(employee);
        verify(jwtService, never()).generateToken("EMP001");
    }

    @Test
    void loginRejectsLockedAccountWithoutCheckingPassword() {
        Employee employee = employee();
        employee.setAccountLocked(true);
        LoginRequest request = request("correct-password");

        when(employeeRepository.findByEmployeeIdIgnoreCase("EMP001")).thenReturn(Optional.of(employee));

        ResponseEntity<?> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.LOCKED);
        verify(passwordEncoder, never()).matches("correct-password", "hash");
        verify(employeeRepository, never()).save(employee);
    }

    @Test
    void successfulLoginResetsFailedAttempts() {
        Employee employee = employee();
        employee.setFailedLoginAttempts(2);
        LoginRequest request = request("correct-password");

        when(employeeRepository.findByEmployeeIdIgnoreCase("EMP001")).thenReturn(Optional.of(employee));
        when(passwordEncoder.matches("correct-password", "hash")).thenReturn(true);
        when(jwtService.generateToken("EMP001")).thenReturn("jwt-token");

        ResponseEntity<?> response = authController.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(employee.getFailedLoginAttempts()).isZero();
        assertThat(employee.isAccountLocked()).isFalse();
        verify(employeeRepository).save(employee);
    }

    private Employee employee() {
        Role role = new Role();
        role.setRoleKey("TECHNICIAN");
        role.setDisplayName("Technician");
        role.setActive(true);

        Employee employee = new Employee();
        employee.setEmployeeId("EMP001");
        employee.setName("Employee");
        employee.setPassword("hash");
        employee.setRoleRecord(role);
        employee.setActive(true);
        return employee;
    }

    private LoginRequest request(String password) {
        LoginRequest request = new LoginRequest();
        request.setEmployeeId("EMP001");
        request.setPassword(password);
        return request;
    }
}
