package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.CreateEmployeeRequest;
import com.ke.ticketsystemke.dto.EmployeeResponse;
import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.EmployeeRole;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EmployeeService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> listEmployees() {
        return employeeRepository.findAll()
                .stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeRequest request, String actorEmployeeId) {
        String employeeId = request.getEmployeeId().trim();
        if (employeeRepository.existsByEmployeeId(employeeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee ID already exists");
        }

        Employee employee = new Employee();
        employee.setName(request.getName().trim());
        employee.setEmployeeId(employeeId);
        employee.setRole(request.getRole());
        employee.setPassword(passwordEncoder.encode(request.getPassword()));
        employee.setActive(request.getActive() == null || request.getActive());

        try {
            Employee created = employeeRepository.saveAndFlush(employee);
            log.info("event=employee_created actorEmployeeId={} targetEmployeeId={} targetRole={} activeStatus={}",
                    actorEmployeeId, created.getEmployeeId(), created.getRole(), created.isActive());
            return EmployeeResponse.from(created);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee ID already exists");
        }
    }

    @Transactional
    public EmployeeResponse updateStatus(Long id, boolean active, String actorEmployeeId) {
        Employee employee = fetchEmployee(id);

        if (!active) {
            if (employee.getEmployeeId().equals(actorEmployeeId)) {
                deny(actorEmployeeId, employee, "self_disable");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot disable your own account");
            }
            requireAnotherActiveSuperAdmin(employee, actorEmployeeId, "last_super_admin_disable");
        }

        employee.setActive(active);
        Employee saved = employeeRepository.save(employee);
        log.info("event={} actorEmployeeId={} targetEmployeeId={} activeStatus={}",
                active ? "employee_enabled" : "employee_disabled",
                actorEmployeeId, saved.getEmployeeId(), saved.isActive());
        return EmployeeResponse.from(saved);
    }

    @Transactional
    public EmployeeResponse updateRole(Long id, EmployeeRole role, String actorEmployeeId) {
        Employee employee = fetchEmployee(id);

        if (employee.getEmployeeId().equals(actorEmployeeId) && role != EmployeeRole.SUPER_ADMIN) {
            deny(actorEmployeeId, employee, "self_demotion");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change your own SUPER_ADMIN role");
        }
        if (employee.getRole() == EmployeeRole.SUPER_ADMIN && role != EmployeeRole.SUPER_ADMIN) {
            requireAnotherActiveSuperAdmin(employee, actorEmployeeId, "last_super_admin_demotion");
        }

        EmployeeRole oldRole = employee.getRole();
        employee.setRole(role);
        Employee saved = employeeRepository.save(employee);
        log.info("event=employee_role_changed actorEmployeeId={} targetEmployeeId={} oldRole={} newRole={}",
                actorEmployeeId, saved.getEmployeeId(), oldRole, saved.getRole());
        return EmployeeResponse.from(saved);
    }

    @Transactional
    public EmployeeResponse resetPassword(Long id, String password, String actorEmployeeId) {
        Employee employee = fetchEmployee(id);
        employee.setPassword(passwordEncoder.encode(password));
        Employee saved = employeeRepository.save(employee);
        log.info("event=employee_password_reset actorEmployeeId={} targetEmployeeId={}",
                actorEmployeeId, saved.getEmployeeId());
        return EmployeeResponse.from(saved);
    }

    private Employee fetchEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    private void requireAnotherActiveSuperAdmin(Employee employee, String actorEmployeeId, String reason) {
        if (employee.getRole() == EmployeeRole.SUPER_ADMIN
                && employee.isActive()
                && employeeRepository.findAllByRole(EmployeeRole.SUPER_ADMIN)
                        .stream()
                        .filter(Employee::isActive)
                        .count() <= 1) {
            deny(actorEmployeeId, employee, reason);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove the last active SUPER_ADMIN");
        }
    }

    private void deny(String actorEmployeeId, Employee employee, String reason) {
        log.warn("event=employee_management_denied actorEmployeeId={} targetEmployeeId={} reason={} httpStatus={}",
                actorEmployeeId, employee.getEmployeeId(), reason, HttpStatus.BAD_REQUEST.value());
    }
}
