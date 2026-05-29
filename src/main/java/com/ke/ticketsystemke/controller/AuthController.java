package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.LoginRequest;
import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.security.JwtUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request
    ) {

        Employee employee = employeeRepository
                .findByEmployeeId(request.getEmployeeId())
                .orElse(null);

        if (employee == null) {
            return ResponseEntity
                    .badRequest()
                    .body("Invalid Employee ID");
        }

        boolean passwordMatched =
                passwordEncoder.matches(
                        request.getPassword(),
                        employee.getPassword()
                );

        if (!passwordMatched) {
            return ResponseEntity
                    .badRequest()
                    .body("Invalid Password");
        }

        String token =
                JwtUtil.generateToken(employee.getEmployeeId());

        Map<String, Object> response = new HashMap<>();

        response.put("token", token);
        response.put("employeeName", employee.getName());
        response.put("role", employee.getRole());

        return ResponseEntity.ok(response);
    }
}