package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.RoleAccessResponse;
import com.ke.ticketsystemke.dto.RoleAccessRuleResponse;
import com.ke.ticketsystemke.dto.UpdateRoleAccessRequest;
import com.ke.ticketsystemke.dto.UpdateRoleAccessRuleRequest;
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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Service
public class RoleAccessService {

    private static final Logger log = LoggerFactory.getLogger(RoleAccessService.class);
    private static final String SUPER_ADMIN_ROLE_KEY = "SUPER_ADMIN";

    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleAccessRuleRepository roleAccessRuleRepository;
    private final AccessService accessService;

    public RoleAccessService(
            RoleRepository roleRepository,
            EmployeeRepository employeeRepository,
            RoleAccessRuleRepository roleAccessRuleRepository,
            AccessService accessService
    ) {
        this.roleRepository = roleRepository;
        this.employeeRepository = employeeRepository;
        this.roleAccessRuleRepository = roleAccessRuleRepository;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public RoleAccessResponse getRoleAccess(Long roleId) {
        Role role = findRole(roleId);
        return toResponse(role);
    }

    @Transactional
    public RoleAccessResponse updateRoleAccess(Long roleId, UpdateRoleAccessRequest request, String actorEmployeeId) {
        Role role = findRole(roleId);
        if (isSuperAdmin(role)) {
            log.warn("event=role_access_update_denied employeeId={} roleId={} roleKey={} decision=super_admin_protected",
                    actorEmployeeId, role.getId(), role.getRoleKey());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SUPER_ADMIN access cannot be changed");
        }
        if (request.getRules() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Access rules are required");
        }

        Employee actor = employeeRepository.findByEmployeeId(actorEmployeeId).orElse(null);
        EnumSet<AccessKey> seen = EnumSet.noneOf(AccessKey.class);
        for (UpdateRoleAccessRuleRequest ruleRequest : request.getRules()) {
            if (ruleRequest == null || ruleRequest.getAccessKey() == null || ruleRequest.getAllowed() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid access rule");
            }
            AccessKey accessKey = ruleRequest.getAccessKey();
            if (!seen.add(accessKey)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate access rule");
            }

            RoleAccessRule rule = roleAccessRuleRepository.findByRoleIdAndAccessKey(role.getId(), accessKey)
                    .orElseGet(() -> createRule(role, accessKey));
            rule.setAllowed(accessKey == AccessKey.VIEW_DASHBOARD || ruleRequest.getAllowed());
            rule.setUpdatedByEmployee(actor);
            roleAccessRuleRepository.save(rule);
        }

        log.info("event=role_access_updated employeeId={} roleId={} roleKey={} resultCount={}",
                actorEmployeeId, role.getId(), role.getRoleKey(), request.getRules().size());
        return toResponse(role);
    }

    private RoleAccessRule createRule(Role role, AccessKey accessKey) {
        RoleAccessRule rule = new RoleAccessRule();
        rule.setRole(role);
        rule.setSystemAccessKey(accessKey);
        return rule;
    }

    private RoleAccessResponse toResponse(Role role) {
        boolean protectedRole = isSuperAdmin(role);
        Map<AccessKey, Boolean> accessMap = accessService.buildAccessMap(role, protectedRole);
        List<RoleAccessRuleResponse> rules = new ArrayList<>();
        for (AccessKey accessKey : AccessKey.values()) {
            rules.add(new RoleAccessRuleResponse(accessKey, accessMap.getOrDefault(accessKey, false)));
        }
        return new RoleAccessResponse(
                role.getId(),
                role.getRoleKey(),
                role.getDisplayName(),
                protectedRole,
                rules
        );
    }

    private Role findRole(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
    }

    private boolean isSuperAdmin(Role role) {
        return SUPER_ADMIN_ROLE_KEY.equals(role.getRoleKey());
    }
}
