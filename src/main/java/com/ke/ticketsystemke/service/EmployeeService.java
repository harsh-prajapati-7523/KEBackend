package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.CreateEmployeeRequest;
import com.ke.ticketsystemke.dto.EmployeeResponse;
import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.EmployeeRole;
import com.ke.ticketsystemke.entity.Role;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.repository.RoleRepository;
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
    private static final String SUPER_ADMIN_ROLE_KEY = "SUPER_ADMIN";

    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeService(EmployeeRepository employeeRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.roleRepository = roleRepository;
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
        assignRole(employee, resolveAssignableRole(request.getRoleId(), request.getRole()));
        employee.setPassword(passwordEncoder.encode(request.getPassword()));
        employee.setActive(request.getActive() == null || request.getActive());

        try {
            Employee created = employeeRepository.saveAndFlush(employee);
            log.info("event=employee_created actorEmployeeId={} targetEmployeeId={} targetRole={} activeStatus={}",
                    actorEmployeeId, created.getEmployeeId(), effectiveRoleKey(created), created.isActive());
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
    public EmployeeResponse updateRole(Long id, Long roleId, EmployeeRole role, String actorEmployeeId) {
        Employee employee = fetchEmployee(id);
        Role newRole = resolveAssignableRole(roleId, role);
        String newRoleKey = newRole.getRoleKey();
        String oldRoleKey = effectiveRoleKey(employee);

        if (employee.getEmployeeId().equals(actorEmployeeId) && !SUPER_ADMIN_ROLE_KEY.equals(newRoleKey)) {
            deny(actorEmployeeId, employee, "self_demotion");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change your own SUPER_ADMIN role");
        }
        if (isSuperAdmin(employee) && !SUPER_ADMIN_ROLE_KEY.equals(newRoleKey)) {
            requireAnotherActiveSuperAdmin(employee, actorEmployeeId, "last_super_admin_demotion");
        }

        assignRole(employee, newRole);
        Employee saved = employeeRepository.save(employee);
        log.info("event=employee_role_changed actorEmployeeId={} targetEmployeeId={} oldRole={} newRole={}",
                actorEmployeeId, saved.getEmployeeId(), oldRoleKey, effectiveRoleKey(saved));
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
        if (isSuperAdmin(employee)
                && employee.isActive()
                && employeeRepository.findAllByRoleRecord_RoleKey(SUPER_ADMIN_ROLE_KEY)
                        .stream()
                        .filter(Employee::isActive)
                        .count() <= 1
                && employeeRepository.findAllByRole(EmployeeRole.SUPER_ADMIN)
                        .stream()
                        .filter(Employee::isActive)
                        .count() <= 1) {
            deny(actorEmployeeId, employee, reason);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove the last active SUPER_ADMIN");
        }
    }

    private Role resolveAssignableRole(Long roleId, EmployeeRole legacyRole) {
        Role role = resolveRole(roleId, legacyRole);
        if (!role.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inactive role cannot be assigned");
        }
        return role;
    }

    private Role resolveRole(Long roleId, EmployeeRole legacyRole) {
        if (roleId != null) {
            return roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found"));
        }
        if (legacyRole != null) {
            return roleRepository.findByRoleKey(legacyRole.name())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role not found"));
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
    }

    private void assignRole(Employee employee, Role role) {
        employee.setRoleRecord(role);
        employee.setRole(toBuiltInRole(role.getRoleKey()));
    }

    private EmployeeRole toBuiltInRole(String roleKey) {
        try {
            return EmployeeRole.valueOf(roleKey);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean isSuperAdmin(Employee employee) {
        return SUPER_ADMIN_ROLE_KEY.equals(effectiveRoleKey(employee));
    }

    private String effectiveRoleKey(Employee employee) {
        if (employee.getRoleRecord() != null) {
            return employee.getRoleRecord().getRoleKey();
        }
        return employee.getRole() == null ? null : employee.getRole().name();
    }

    private void deny(String actorEmployeeId, Employee employee, String reason) {
        log.warn("event=employee_management_denied actorEmployeeId={} targetEmployeeId={} reason={} httpStatus={}",
                actorEmployeeId, employee.getEmployeeId(), reason, HttpStatus.BAD_REQUEST.value());
    }
}
