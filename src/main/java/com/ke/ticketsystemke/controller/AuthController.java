package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.ChangePasswordRequest;
import com.ke.ticketsystemke.dto.LoginRequest;
import com.ke.ticketsystemke.dto.LoginResponse;
import com.ke.ticketsystemke.dto.MessageResponse;
import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.repository.RoleRepository;
import com.ke.ticketsystemke.security.JwtService;
import com.ke.ticketsystemke.service.EmployeeService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@RestController
@RequestMapping("/volt/auth")
public class AuthController {

        private static final Logger log = LoggerFactory.getLogger(AuthController.class);
        private static final int MAX_FAILED_LOGIN_ATTEMPTS = 3;

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

    @PostMapping("/employeelogin")
    @Transactional
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request
    ) {

        String employeeId = request.getEmployeeId() == null ? "" : request.getEmployeeId().trim();
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

        if (request.getPassword() == null ||
                !passwordEncoder.matches(
                        request.getPassword(),
                        employee.getPassword()
                )) {
            int failedAttempts = employee.getFailedLoginAttempts() + 1;
            employee.setFailedLoginAttempts(failedAttempts);
            if (failedAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
                employee.setAccountLocked(true);
                log.warn("event=account_locked employeeId={} failedAttempts={}", employeeId, failedAttempts);
            }
            employeeRepository.save(employee);
            log.warn("event=login_failure employeeId={} failedAttempts={}", employeeId, failedAttempts);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid employee ID or password");
        }

        if (employee.getFailedLoginAttempts() != 0) {
            employee.setFailedLoginAttempts(0);
            employeeRepository.save(employee);
        }

        String token = jwtService.generateToken(employee.getEmployeeId());

        String role = resolveRoleKey(employee);

        log.info("event=login_success employeeId={} role={}", employeeId, role);

        return ResponseEntity.ok(
                new LoginResponse(
                        token,
                        employee.getName(),
                        role,
                        employee.getEmployeeId()
                )
        );
    }

    @PatchMapping("/change-password")
    public MessageResponse changePassword(
            @RequestBody ChangePasswordRequest request,
            Authentication authentication
    ) {
        employeeService.changeOwnPassword(authentication.getName(), request);
        return new MessageResponse("Password changed successfully");
    }

    private String resolveRoleKey(Employee employee) {
        if (employee.getRoleRecord() != null) {
            return employee.getRoleRecord().getRoleKey();
        }
        return employee.getRole() == null ? null : employee.getRole().name();
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
