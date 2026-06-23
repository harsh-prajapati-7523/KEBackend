package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.DynamicRoleAccessResponse;
import com.ke.ticketsystemke.dto.DynamicRoleAccessRuleResponse;
import com.ke.ticketsystemke.dto.RoleAccessResponse;
import com.ke.ticketsystemke.dto.RoleAccessRuleResponse;
import com.ke.ticketsystemke.dto.UpdateDynamicRoleAccessRequest;
import com.ke.ticketsystemke.dto.UpdateDynamicRoleAccessRuleRequest;
import com.ke.ticketsystemke.dto.UpdateRoleAccessRequest;
import com.ke.ticketsystemke.dto.UpdateRoleAccessRuleRequest;
import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.entity.AccessKeyMetadata;
import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.Role;
import com.ke.ticketsystemke.entity.RoleAccessRule;
import com.ke.ticketsystemke.repository.AccessKeyMetadataRepository;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class RoleAccessService {

    private static final Logger log = LoggerFactory.getLogger(RoleAccessService.class);
    private static final String SUPER_ADMIN_ROLE_KEY = "SUPER_ADMIN";
    private static final Pattern ACCESS_KEY_PATTERN = Pattern.compile("^[A-Z0-9_]{2,60}$");

    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleAccessRuleRepository roleAccessRuleRepository;
    private final AccessKeyMetadataRepository accessKeyMetadataRepository;
    private final AccessService accessService;

    public RoleAccessService(
            RoleRepository roleRepository,
            EmployeeRepository employeeRepository,
            RoleAccessRuleRepository roleAccessRuleRepository,
            AccessKeyMetadataRepository accessKeyMetadataRepository,
            AccessService accessService
    ) {
        this.roleRepository = roleRepository;
        this.employeeRepository = employeeRepository;
        this.roleAccessRuleRepository = roleAccessRuleRepository;
        this.accessKeyMetadataRepository = accessKeyMetadataRepository;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public RoleAccessResponse getRoleAccess(Long roleId) {
        Role role = findRole(roleId);
        return toResponse(role);
    }

    @Transactional(readOnly = true)
    public DynamicRoleAccessResponse getDynamicRoleAccess(Long roleId) {
        Role role = findRole(roleId);
        List<RoleAccessRule> roleRules = roleAccessRuleRepository.findAllByRoleId(role.getId());
        Map<String, Boolean> allowedByAccessKey = roleRules.stream()
                .collect(java.util.stream.Collectors.toMap(
                        RoleAccessRule::getAccessKey,
                        RoleAccessRule::isAllowed,
                        (first, second) -> second
                ));

        List<DynamicRoleAccessRuleResponse> rules = accessKeyMetadataRepository
                .findAllBySystemKeyFalseAndProtectedKeyFalseOrderByCategoryAscSortOrderAscDisplayNameAscAccessKeyAsc()
                .stream()
                .filter(metadata -> !isSystemAccessKey(metadata.getAccessKey()))
                .map(metadata -> toDynamicRuleResponse(
                        metadata,
                        allowedByAccessKey.getOrDefault(metadata.getAccessKey(), false)
                ))
                .toList();

        log.info("event=dynamic_role_access_returned roleId={} ruleCount={} result=returned", role.getId(), rules.size());
        return new DynamicRoleAccessResponse(
                role.getId(),
                role.getRoleKey(),
                role.getDisplayName(),
                isSuperAdmin(role),
                rules
        );
    }

    @Transactional
    public DynamicRoleAccessResponse updateDynamicRoleAccess(
            Long roleId,
            UpdateDynamicRoleAccessRequest request,
            String actorEmployeeId
    ) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Access rules are required");
        }

        Role role = findRole(roleId);
        if (isSuperAdmin(role)) {
            log.warn("event=dynamic_role_access_update_denied employeeId={} roleId={} roleKey={} decision=super_admin_protected",
                    actorEmployeeId, role.getId(), role.getRoleKey());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SUPER_ADMIN access cannot be changed");
        }
        if (request.getRules() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Access rules are required");
        }

        String lookupActorEmployeeId = actorEmployeeId == null ? "" : actorEmployeeId.trim();
        Employee actor = employeeRepository.findByEmployeeIdIgnoreCase(lookupActorEmployeeId).orElse(null);
        Set<String> seen = new HashSet<>();
        List<DynamicRoleAccessRuleResponse> responses = new ArrayList<>();

        for (UpdateDynamicRoleAccessRuleRequest ruleRequest : request.getRules()) {
            if (ruleRequest == null || ruleRequest.getAllowed() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid access rule");
            }

            String accessKey = normalizeDynamicAccessKey(ruleRequest.getAccessKey());
            if (!seen.add(accessKey)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate access rule");
            }

            AccessKeyMetadata metadata = accessKeyMetadataRepository.findByAccessKey(accessKey)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown access key"));
            validateDynamicMetadata(metadata);

            RoleAccessRule rule = roleAccessRuleRepository.findByRoleIdAndAccessKey(role.getId(), accessKey)
                    .orElseGet(() -> createDynamicRule(role, accessKey));
            rule.setAllowed(ruleRequest.getAllowed());
            rule.setUpdatedByEmployee(actor);
            roleAccessRuleRepository.save(rule);

            responses.add(toDynamicRuleResponse(metadata, rule.isAllowed()));
        }

        log.info("event=dynamic_role_access_updated employeeId={} roleId={} roleKey={} resultCount={}",
                actorEmployeeId, role.getId(), role.getRoleKey(), request.getRules().size());
        return new DynamicRoleAccessResponse(
                role.getId(),
                role.getRoleKey(),
                role.getDisplayName(),
                isSuperAdmin(role),
                responses
        );
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

        String lookupActorEmployeeId = actorEmployeeId == null ? "" : actorEmployeeId.trim();
        Employee actor = employeeRepository.findByEmployeeIdIgnoreCase(lookupActorEmployeeId).orElse(null);
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

    private RoleAccessRule createDynamicRule(Role role, String accessKey) {
        RoleAccessRule rule = new RoleAccessRule();
        rule.setRole(role);
        rule.setAccessKey(accessKey);
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

    private String normalizeDynamicAccessKey(String value) {
        String accessKey = value == null ? "" : value.trim();
        if (!ACCESS_KEY_PATTERN.matcher(accessKey).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Access key must contain 2 to 60 uppercase letters, digits, or underscores");
        }
        return accessKey;
    }

    private void validateDynamicMetadata(AccessKeyMetadata metadata) {
        if (!metadata.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inactive access key cannot be assigned");
        }
        if (metadata.isSystemKey() || metadata.isProtectedKey() || isSystemAccessKey(metadata.getAccessKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "System or protected access key cannot be assigned through dynamic endpoint");
        }
    }

    private boolean isSystemAccessKey(String accessKey) {
        try {
            AccessKey.valueOf(accessKey);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private DynamicRoleAccessRuleResponse toDynamicRuleResponse(AccessKeyMetadata metadata, boolean allowed) {
        return new DynamicRoleAccessRuleResponse(
                metadata.getAccessKey(),
                metadata.getDisplayName(),
                metadata.getDescription(),
                metadata.getCategory(),
                metadata.isActive(),
                allowed,
                metadata.isSystemKey(),
                metadata.isProtectedKey(),
                metadata.getSortOrder()
        );
    }
}
