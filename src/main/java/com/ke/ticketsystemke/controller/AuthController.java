package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.LoginRequest;
import com.ke.ticketsystemke.dto.LoginResponse;
import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.security.JwtService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/volt/auth")
public class AuthController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/employeelogin")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request
    ) {

        Employee employee = employeeRepository
                .findByEmployeeId(request.getEmployeeId())
                .orElse(null);

        if (employee == null ||
                request.getPassword() == null ||
                !passwordEncoder.matches(
                        request.getPassword(),
                        employee.getPassword()
                )) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid employee ID or password");
        }

        String token =
                jwtService.generateToken(employee.getEmployeeId());

        return ResponseEntity.ok(
                new LoginResponse(
                        token,
                        employee.getName(),
                        employee.getRole()
                )
        );
    }
}
