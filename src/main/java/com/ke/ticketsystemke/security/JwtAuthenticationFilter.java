package com.ke.ticketsystemke.security;

import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.repository.RoleRepository;
import com.ke.ticketsystemke.service.RefreshTokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

        private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader =
                request.getHeader("Authorization");

        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {

            // No bearer token present; do not leak info for public auth endpoints
            log.debug("event=missing_bearer_token endpoint={} method={}", request.getRequestURI(), request.getMethod());
            filterChain.doFilter(request, response);
            return;
        }

        String token =
                authHeader.substring(7);

        try {

            Claims claims = jwtService.extractClaims(token);
            String employeeId = jwtService.extractEmployeeId(claims);
            String tokenUse = jwtService.extractTokenUse(claims);
            String sessionHash = jwtService.extractSessionHash(claims);

            String lookupEmployeeId = employeeId == null ? "" : employeeId.trim();

            Employee employee = employeeRepository
                    .findByEmployeeIdIgnoreCase(lookupEmployeeId)
                    .orElseThrow();

            if (!employee.isActive()) {
                throw new IllegalStateException("Inactive employee");
            }
            if (hasInactiveRole(employee)) {
                throw new IllegalStateException("Inactive role");
            }
            if (JwtService.TOKEN_USE_PIN_SETUP.equals(tokenUse)) {
                if (!isPinSetupRequest(request)) {
                    throw new IllegalStateException("Pin setup token cannot access this endpoint");
                }
                refreshTokenService.validatePrePinByTokenHash(sessionHash);
            } else {
                refreshTokenService.validateFullByTokenHash(sessionHash);
            }

            String role = resolveRoleKey(employee);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            employeeId,
                            null,
                            List.of(new SimpleGrantedAuthority(
                                    "ROLE_" + role
                            ))
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            // set employeeId in MDC for downstream logs
            MDC.put("employeeId", employeeId);
            log.info("event=authentication_success employeeId={}", employeeId);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception e) {

            log.warn("event=jwt_validation_failed endpoint={} method={} reason={}", request.getRequestURI(), request.getMethod(), e.getClass().getSimpleName());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("employeeId");
        }
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

    private boolean isPinSetupRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && "/volt/auth/pin/setup".equals(request.getRequestURI());
    }
}
