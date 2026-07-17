package com.ke.ticketsystemke.config;

import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.entity.AccessKeyMetadata;
import com.ke.ticketsystemke.entity.AuthenticationMode;
import com.ke.ticketsystemke.entity.EmployeeRole;
import com.ke.ticketsystemke.entity.Role;
import com.ke.ticketsystemke.entity.RoleAccessRule;
import com.ke.ticketsystemke.repository.AccessKeyMetadataRepository;
import com.ke.ticketsystemke.repository.RoleAccessRuleRepository;
import com.ke.ticketsystemke.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class AccessBootstrapInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AccessBootstrapInitializer.class);

    private static final List<RoleSeed> SYSTEM_ROLES = List.of(
            new RoleSeed(EmployeeRole.SUPER_ADMIN.name(), "Super Admin"),
            new RoleSeed(EmployeeRole.ADMIN.name(), "Admin"),
            new RoleSeed(EmployeeRole.TECHNICIAN.name(), "Technician")
    );

    private static final Map<AccessKey, AccessMetadataSeed> ACCESS_METADATA = buildAccessMetadata();

    private static final Set<AccessKey> ADMIN_DEFAULT_ACCESS = Set.of(
            AccessKey.VIEW_TICKETS,
            AccessKey.CREATE_TICKET,
            AccessKey.PICK_TICKET,
            AccessKey.ASSIGN_TICKET,
            AccessKey.START_WORK,
            AccessKey.COMPLETE_TICKET,
            AccessKey.CANCEL_TICKET,
            AccessKey.UPDATE_WARRANTY,
            AccessKey.VIEW_WARRANTY,
            AccessKey.MANAGE_WARRANTY,
            AccessKey.RESOLVE_WARRANTY,
            AccessKey.MANAGE_WARRANTY_DOCUMENTS,
            AccessKey.VIEW_CUSTOMER_HISTORY,
            AccessKey.VIEW_CHARGES,
            AccessKey.ADD_CHARGE,
            AccessKey.DELETE_CHARGE,
            AccessKey.USE_TICKET_SEARCH,
            AccessKey.USE_TICKET_FILTERS,
            AccessKey.USE_SMART_SUGGESTIONS
    );

    private static final Set<AccessKey> TECHNICIAN_DEFAULT_ACCESS = Set.of(
            AccessKey.VIEW_TICKETS,
            AccessKey.PICK_TICKET,
            AccessKey.START_WORK,
            AccessKey.COMPLETE_TICKET,
            AccessKey.UPDATE_WARRANTY,
            AccessKey.VIEW_WARRANTY,
            AccessKey.VIEW_CUSTOMER_HISTORY,
            AccessKey.VIEW_CHARGES,
            AccessKey.ADD_CHARGE,
            AccessKey.USE_TICKET_SEARCH,
            AccessKey.USE_TICKET_FILTERS,
            AccessKey.USE_SMART_SUGGESTIONS
    );

    private final RoleRepository roleRepository;
    private final AccessKeyMetadataRepository accessKeyMetadataRepository;
    private final RoleAccessRuleRepository roleAccessRuleRepository;

    public AccessBootstrapInitializer(
            RoleRepository roleRepository,
            AccessKeyMetadataRepository accessKeyMetadataRepository,
            RoleAccessRuleRepository roleAccessRuleRepository
    ) {
        this.roleRepository = roleRepository;
        this.accessKeyMetadataRepository = accessKeyMetadataRepository;
        this.roleAccessRuleRepository = roleAccessRuleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, Role> rolesByKey = ensureSystemRoles();
        ensureSystemAccessMetadata();
        ensureDefaultRoleAccess(rolesByKey.get(EmployeeRole.ADMIN.name()), ADMIN_DEFAULT_ACCESS);
        ensureDefaultRoleAccess(rolesByKey.get(EmployeeRole.TECHNICIAN.name()), TECHNICIAN_DEFAULT_ACCESS);
        log.info("event=access_bootstrap_completed roleCount={} accessKeyCount={}", rolesByKey.size(), ACCESS_METADATA.size());
    }

    private Map<String, Role> ensureSystemRoles() {
        Map<String, Role> rolesByKey = new java.util.HashMap<>();
        for (RoleSeed seed : SYSTEM_ROLES) {
            Role role = roleRepository.findByRoleKey(seed.roleKey())
                    .orElseGet(() -> {
                        Role created = new Role();
                        created.setRoleKey(seed.roleKey());
                        created.setActive(true);
                        return created;
                    });
            role.setDisplayName(seed.displayName());
            role.setSystemRole(true);
            role.setAuthenticationMode(AuthenticationMode.PASSWORD_PIN);
            if (EmployeeRole.SUPER_ADMIN.name().equals(seed.roleKey())) {
                role.setActive(true);
            }
            rolesByKey.put(seed.roleKey(), roleRepository.save(role));
        }
        return rolesByKey;
    }

    private void ensureSystemAccessMetadata() {
        for (Map.Entry<AccessKey, AccessMetadataSeed> entry : ACCESS_METADATA.entrySet()) {
            AccessKey accessKey = entry.getKey();
            AccessMetadataSeed seed = entry.getValue();
            AccessKeyMetadata metadata = accessKeyMetadataRepository.findByAccessKey(accessKey.name())
                    .orElseGet(() -> {
                        AccessKeyMetadata created = new AccessKeyMetadata();
                        created.setAccessKey(accessKey.name());
                        return created;
                    });
            metadata.setDisplayName(seed.displayName());
            metadata.setDescription(seed.description());
            metadata.setCategory(seed.category());
            metadata.setActive(true);
            metadata.setSystemKey(true);
            metadata.setProtectedKey(true);
            metadata.setSortOrder(seed.sortOrder());
            accessKeyMetadataRepository.save(metadata);
        }
    }

    private void ensureDefaultRoleAccess(Role role, Set<AccessKey> allowedAccessKeys) {
        if (role == null) {
            return;
        }
        for (AccessKey accessKey : allowedAccessKeys) {
            RoleAccessRule rule = roleAccessRuleRepository.findByRoleIdAndAccessKey(role.getId(), accessKey)
                    .orElseGet(() -> {
                        RoleAccessRule created = new RoleAccessRule();
                        created.setRole(role);
                        created.setSystemAccessKey(accessKey);
                        return created;
                    });
            rule.setAllowed(true);
            roleAccessRuleRepository.save(rule);
        }
    }

    private static Map<AccessKey, AccessMetadataSeed> buildAccessMetadata() {
        Map<AccessKey, AccessMetadataSeed> metadata = new EnumMap<>(AccessKey.class);
        metadata.put(AccessKey.VIEW_DASHBOARD, new AccessMetadataSeed("View Dashboard", "View the employee dashboard.", "Dashboard", 10));
        metadata.put(AccessKey.VIEW_TICKETS, new AccessMetadataSeed("View Tickets", "View ticket lists and ticket details.", "Tickets", 20));
        metadata.put(AccessKey.CREATE_TICKET, new AccessMetadataSeed("Create Ticket", "Create new tickets.", "Tickets", 30));
        metadata.put(AccessKey.PICK_TICKET, new AccessMetadataSeed("Pick Ticket", "Pick or take ownership of an eligible ticket.", "Ticket Actions", 40));
        metadata.put(AccessKey.ASSIGN_TICKET, new AccessMetadataSeed("Assign Ticket", "Assign a ticket current owner to an active employee.", "Ticket Actions", 50));
        metadata.put(AccessKey.START_WORK, new AccessMetadataSeed("Start Work", "Move an eligible ticket into work in progress.", "Ticket Actions", 60));
        metadata.put(AccessKey.COMPLETE_TICKET, new AccessMetadataSeed("Complete Ticket", "Complete an eligible in-progress ticket.", "Ticket Actions", 70));
        metadata.put(AccessKey.CANCEL_TICKET, new AccessMetadataSeed("Cancel Ticket", "Cancel an eligible ticket.", "Ticket Actions", 80));
        metadata.put(AccessKey.UPDATE_WARRANTY, new AccessMetadataSeed("Update Warranty", "Update ticket warranty and manufacturer details.", "Ticket Actions", 90));
        metadata.put(AccessKey.VIEW_WARRANTY, new AccessMetadataSeed("View Warranty", "View warranty claims and warranty activity.", "Warranty", 91));
        metadata.put(AccessKey.MANAGE_WARRANTY, new AccessMetadataSeed("Manage Warranty", "Create and update warranty claims.", "Warranty", 92));
        metadata.put(AccessKey.RESOLVE_WARRANTY, new AccessMetadataSeed("Resolve Warranty", "Record warranty outcomes and approved warranty workflow decisions.", "Warranty", 93));
        metadata.put(AccessKey.MANAGE_WARRANTY_DOCUMENTS, new AccessMetadataSeed("Manage Warranty Documents", "Upload, supersede, and remove private warranty evidence.", "Warranty", 94));
        metadata.put(AccessKey.VIEW_CUSTOMER_HISTORY, new AccessMetadataSeed("View Customer History", "View customer ticket history.", "Tickets", 100));
        metadata.put(AccessKey.VIEW_CHARGES, new AccessMetadataSeed("View Charges", "View ticket charges.", "Ticket Charges", 110));
        metadata.put(AccessKey.ADD_CHARGE, new AccessMetadataSeed("Add Charge", "Add charges to eligible tickets.", "Ticket Charges", 120));
        metadata.put(AccessKey.DELETE_CHARGE, new AccessMetadataSeed("Delete Charge", "Delete ticket charges.", "Ticket Charges", 130));
        metadata.put(AccessKey.USE_TICKET_SEARCH, new AccessMetadataSeed("Use Ticket Search", "Use ticket search.", "Ticket Filters", 140));
        metadata.put(AccessKey.USE_TICKET_FILTERS, new AccessMetadataSeed("Use Ticket Filters", "Use ticket filters.", "Ticket Filters", 150));
        metadata.put(AccessKey.USE_SMART_SUGGESTIONS, new AccessMetadataSeed("Use Smart Suggestions", "Use smart suggestions in supported fields.", "Smart Suggestions", 160));
        metadata.put(AccessKey.VIEW_EMPLOYEE_MANAGEMENT, new AccessMetadataSeed("View Employee Management", "View employee management.", "Admin Configuration", 170));
        metadata.put(AccessKey.MANAGE_EMPLOYEES, new AccessMetadataSeed("Manage Employees", "Create, update, and administer employees.", "Admin Configuration", 180));
        metadata.put(AccessKey.VIEW_ROLE_MANAGEMENT, new AccessMetadataSeed("View Role Management", "View role management.", "Role Access", 190));
        metadata.put(AccessKey.MANAGE_ROLES, new AccessMetadataSeed("Manage Roles", "Create, update, and administer roles.", "Role Access", 200));
        metadata.put(AccessKey.APPROVE_DEVICE_PAIRING, new AccessMetadataSeed("Approve Device Pairing", "Approve or revoke employee device pairing requests.", "Role Access", 210));
        metadata.put(AccessKey.VIEW_TICKET_CATEGORY_MANAGEMENT, new AccessMetadataSeed("View Ticket Category Management", "View ticket category management.", "Admin Configuration", 220));
        metadata.put(AccessKey.MANAGE_TICKET_CATEGORIES, new AccessMetadataSeed("Manage Ticket Categories", "Create and update ticket categories.", "Admin Configuration", 230));
        metadata.put(AccessKey.VIEW_TICKET_FIELD_MANAGEMENT, new AccessMetadataSeed("View Ticket Field Management", "View ticket field management.", "Admin Configuration", 240));
        metadata.put(AccessKey.MANAGE_TICKET_FIELDS, new AccessMetadataSeed("Manage Ticket Fields", "Create and update ticket fields.", "Admin Configuration", 250));
        metadata.put(AccessKey.VIEW_CATEGORY_FIELD_CONFIGURATION, new AccessMetadataSeed("View Category Field Configuration", "View category field configuration.", "Admin Configuration", 260));
        metadata.put(AccessKey.MANAGE_CATEGORY_FIELD_CONFIGS, new AccessMetadataSeed("Manage Category Field Configs", "Create and update category field configuration.", "Admin Configuration", 270));
        metadata.put(AccessKey.VIEW_DROPDOWN_SOURCE_MANAGEMENT, new AccessMetadataSeed("View Dropdown Source Management", "View dropdown source management.", "Admin Configuration", 280));
        metadata.put(AccessKey.MANAGE_DROPDOWN_SOURCES, new AccessMetadataSeed("Manage Dropdown Sources", "Create and update dropdown sources.", "Admin Configuration", 290));
        return metadata;
    }

    private record RoleSeed(String roleKey, String displayName) {
    }

    private record AccessMetadataSeed(String displayName, String description, String category, Integer sortOrder) {
    }
}
