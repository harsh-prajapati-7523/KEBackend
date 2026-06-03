package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.CreateRoleRequest;
import com.ke.ticketsystemke.dto.RoleResponse;
import com.ke.ticketsystemke.entity.Role;
import com.ke.ticketsystemke.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);
    private static final String SUPER_ADMIN_ROLE_KEY = "SUPER_ADMIN";

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        return roleRepository.findAllByOrderByRoleKeyAsc()
                .stream()
                .map(RoleResponse::from)
                .toList();
    }

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request, String actorEmployeeId) {
        String roleKey = request.getRoleKey().trim();
        if (roleRepository.existsByRoleKey(roleKey)) {
            log.warn("event=role_create_denied actorEmployeeId={} roleKey={} decision=duplicate_role_key correlationId={}",
                    actorEmployeeId, roleKey, MDC.get("correlationId"));
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role key already exists");
        }

        Role role = new Role();
        role.setRoleKey(roleKey);
        role.setDisplayName(request.getDisplayName().trim());
        role.setActive(request.getActive() == null || request.getActive());
        role.setSystemRole(false);

        try {
            Role created = roleRepository.saveAndFlush(role);
            log.info("event=role_created actorEmployeeId={} roleId={} roleKey={} targetActive={} correlationId={}",
                    actorEmployeeId, created.getId(), created.getRoleKey(), created.isActive(), MDC.get("correlationId"));
            return RoleResponse.from(created);
        } catch (DataIntegrityViolationException ex) {
            log.warn("event=role_create_denied actorEmployeeId={} roleKey={} decision=duplicate_role_key correlationId={}",
                    actorEmployeeId, roleKey, MDC.get("correlationId"));
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role key already exists");
        }
    }

    @Transactional
    public RoleResponse updateStatus(Long id, boolean active, String actorEmployeeId) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("event=role_update_denied actorEmployeeId={} roleId={} targetActive={} decision=role_not_found correlationId={}",
                            actorEmployeeId, id, active, MDC.get("correlationId"));
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found");
                });

        if (!active && SUPER_ADMIN_ROLE_KEY.equals(role.getRoleKey())) {
            log.warn("event=role_update_denied actorEmployeeId={} roleId={} roleKey={} targetActive={} decision=super_admin_protected correlationId={}",
                    actorEmployeeId, role.getId(), role.getRoleKey(), active, MDC.get("correlationId"));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SUPER_ADMIN role cannot be disabled");
        }

        role.setActive(active);
        Role saved = roleRepository.save(role);
        log.info("event={} actorEmployeeId={} roleId={} roleKey={} targetActive={} correlationId={}",
                active ? "role_enabled" : "role_disabled",
                actorEmployeeId, saved.getId(), saved.getRoleKey(), saved.isActive(), MDC.get("correlationId"));
        return RoleResponse.from(saved);
    }
}
