package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.CurrentAccessResponse;
import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.Role;
import com.ke.ticketsystemke.entity.RoleAccessRule;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.repository.RoleAccessRuleRepository;
import com.ke.ticketsystemke.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class AccessService {

    private static final Logger log = LoggerFactory.getLogger(AccessService.class);
    private static final String SUPER_ADMIN_ROLE_KEY = "SUPER_ADMIN";

    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final RoleAccessRuleRepository roleAccessRuleRepository;

    public AccessService(
            EmployeeRepository employeeRepository,
            RoleRepository roleRepository,
            RoleAccessRuleRepository roleAccessRuleRepository
    ) {
        this.employeeRepository = employeeRepository;
        this.roleRepository = roleRepository;
        this.roleAccessRuleRepository = roleAccessRuleRepository;
    }

    @Transactional(readOnly = true)
    public boolean isAllowed(String employeeId, AccessKey accessKey) {
        try {
            Employee employee = resolveActiveEmployee(employeeId);
            Role role = resolveActiveRole(employee);
            if (isSuperAdmin(role)) {
                return true;
            }
            if (accessKey == AccessKey.VIEW_DASHBOARD) {
                return true;
            }
            return isAllowedForRole(role, accessKey.name());
        } catch (RuntimeException ex) {
            log.warn("event=access_check_failed employeeId={} accessKey={} decision=deny", employeeId, accessKey);
            return false;
        }
    }

    @Transactional(readOnly = true)
    public boolean isAllowed(String employeeId, String accessKey) {
        String normalizedAccessKey = normalizeAccessKey(accessKey);
        if (normalizedAccessKey == null) {
            return false;
        }

        AccessKey systemAccessKey = toSystemAccessKey(normalizedAccessKey, false);
        if (systemAccessKey != null) {
            return isAllowed(employeeId, systemAccessKey);
        }

        try {
            Employee employee = resolveActiveEmployee(employeeId);
            Role role = resolveActiveRole(employee);
            if (isSuperAdmin(role)) {
                return true;
            }
            return isAllowedForRole(role, normalizedAccessKey);
        } catch (RuntimeException ex) {
            log.warn("event=access_check_failed employeeId={} accessKey={} decision=deny", employeeId, accessKey);
            return false;
        }
    }

    private boolean isAllowedForRole(Role role, String accessKey) {
        return roleAccessRuleRepository.findByRoleIdAndAccessKey(role.getId(), accessKey)
                .map(RoleAccessRule::isAllowed)
                .orElse(false);
    }

    @Transactional(readOnly = true, noRollbackFor = ResponseStatusException.class)
    public void requireAllowed(String employeeId, AccessKey accessKey) {
        if (!isAllowed(employeeId, accessKey)) {
            log.warn("event=access_denied employeeId={} accessKey={} decision=deny", employeeId, accessKey);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    @Transactional(readOnly = true, noRollbackFor = ResponseStatusException.class)
    public void requireAllowed(String employeeId, String accessKey) {
        if (!isAllowed(employeeId, accessKey)) {
            log.warn("event=access_denied employeeId={} accessKey={} decision=deny", employeeId, accessKey);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    @Transactional(readOnly = true, noRollbackFor = ResponseStatusException.class)
    public void requireAnyAllowed(String employeeId, AccessKey... accessKeys) {
        for (AccessKey accessKey : accessKeys) {
            if (isAllowed(employeeId, accessKey)) {
                return;
            }
        }
        log.warn("event=access_denied employeeId={} accessKeys={} decision=deny", employeeId, Arrays.toString(accessKeys));
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    @Transactional(readOnly = true)
    public CurrentAccessResponse getCurrentAccess(String employeeId) {
        Employee employee = resolveActiveEmployee(employeeId);
        Role role = resolveActiveRole(employee);
        boolean superAdmin = isSuperAdmin(role);
        Map<AccessKey, Boolean> access = buildAccessMap(role, superAdmin);
        return new CurrentAccessResponse(
                role.getId(),
                role.getRoleKey(),
                role.getDisplayName(),
                superAdmin,
                access
        );
    }

    Map<AccessKey, Boolean> buildAccessMap(Role role, boolean superAdmin) {
        Map<AccessKey, Boolean> access = new EnumMap<>(AccessKey.class);
        for (AccessKey accessKey : AccessKey.values()) {
            access.put(accessKey, superAdmin || accessKey == AccessKey.VIEW_DASHBOARD);
        }

        if (!superAdmin) {
            List<RoleAccessRule> rules = roleAccessRuleRepository.findAllByRoleId(role.getId());
            for (RoleAccessRule rule : rules) {
                AccessKey accessKey = toSystemAccessKey(rule.getAccessKey(), true);
                if (accessKey != null && accessKey != AccessKey.VIEW_DASHBOARD) {
                    access.put(accessKey, rule.isAllowed());
                }
            }
        }
        return access;
    }

    private AccessKey toSystemAccessKey(String accessKey, boolean logUnknown) {
        try {
            String normalizedAccessKey = normalizeAccessKey(accessKey);
            return normalizedAccessKey == null ? null : AccessKey.valueOf(normalizedAccessKey);
        } catch (IllegalArgumentException ex) {
            if (logUnknown) {
                log.warn("event=unknown_role_access_key accessKey={} decision=ignore", accessKey);
            }
            return null;
        }
    }

    private String normalizeAccessKey(String accessKey) {
        if (accessKey == null || accessKey.isBlank()) {
            return null;
        }
        return accessKey.trim();
    }

    Role resolveActiveRole(Employee employee) {
        Role role = employee.getRoleRecord();
        if (role == null && employee.getRole() != null) {
            role = roleRepository.findByRoleKey(employee.getRole().name()).orElse(null);
        }
        if (role == null || !role.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inactive role");
        }
        return role;
    }

    private Employee resolveActiveEmployee(String employeeId) {
        String lookupEmployeeId = employeeId == null ? "" : employeeId.trim();
        Employee employee = employeeRepository.findByEmployeeIdIgnoreCase(lookupEmployeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid employee"));
        if (!employee.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inactive employee");
        }
        return employee;
    }

    private boolean isSuperAdmin(Role role) {
        return role != null && SUPER_ADMIN_ROLE_KEY.equals(role.getRoleKey());
    }
}
