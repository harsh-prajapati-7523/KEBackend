package com.ke.ticketsystemke.config;

import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.EmployeeRole;
import com.ke.ticketsystemke.entity.AuthenticationMode;
import com.ke.ticketsystemke.entity.Role;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
public class BootstrapSuperAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapSuperAdminInitializer.class);
    private static final String BOOTSTRAP_EMPLOYEE_ID = "SUPER_ADMIN_001";
    private static final String BOOTSTRAP_NAME = "Bootstrap SUPER_ADMIN";
    private static final String DEVELOPMENT_FALLBACK_PASSWORD = "admin123";

    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;
    private final String bootstrapPassword;
    private final boolean resetBootstrapPassword;

    public BootstrapSuperAdminInitializer(
            EmployeeRepository employeeRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            Environment environment,
            @Value("${BOOTSTRAP_SUPER_ADMIN_PASSWORD:}") String bootstrapPassword,
            @Value("${BOOTSTRAP_SUPER_ADMIN_RESET_PASSWORD:false}") boolean resetBootstrapPassword
    ) {
        this.employeeRepository = employeeRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
        this.bootstrapPassword = bootstrapPassword;
        this.resetBootstrapPassword = resetBootstrapPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Role superAdminRole = ensureSuperAdminRole();
        Optional<Employee> existingBootstrapEmployee = employeeRepository.findByEmployeeIdIgnoreCase(BOOTSTRAP_EMPLOYEE_ID);
        if (existingBootstrapEmployee.isPresent()) {
            Employee employee = existingBootstrapEmployee.get();
            employee.setName(BOOTSTRAP_NAME);
            employee.setRole(EmployeeRole.SUPER_ADMIN);
            employee.setRoleRecord(superAdminRole);
            employee.setActive(true);
            if (resetBootstrapPassword || employee.getPassword() == null || employee.getPassword().isBlank()) {
                employee.setPassword(passwordEncoder.encode(resolveBootstrapPassword()));
            }
            employeeRepository.save(employee);
            log.info("event=bootstrap_super_admin_repaired employeeId={} role={}",
                    BOOTSTRAP_EMPLOYEE_ID, EmployeeRole.SUPER_ADMIN);
            return;
        }

        if (employeeRepository.existsByRole(EmployeeRole.SUPER_ADMIN)
                || !employeeRepository.findAllByRoleRecord_RoleKey(EmployeeRole.SUPER_ADMIN.name()).isEmpty()) {
            log.info("event=bootstrap_super_admin_skipped employeeId={} role={} reason=existing_super_admin",
                    BOOTSTRAP_EMPLOYEE_ID, EmployeeRole.SUPER_ADMIN);
            return;
        }

        String password = resolveBootstrapPassword();

        Employee employee = new Employee();
        employee.setEmployeeId(BOOTSTRAP_EMPLOYEE_ID);
        employee.setName(BOOTSTRAP_NAME);
        employee.setRole(EmployeeRole.SUPER_ADMIN);
        employee.setRoleRecord(superAdminRole);
        employee.setActive(true);
        employee.setPassword(passwordEncoder.encode(password));

        employeeRepository.save(employee);
        log.info("event=bootstrap_super_admin_created employeeId={} role={}",
                BOOTSTRAP_EMPLOYEE_ID, EmployeeRole.SUPER_ADMIN);
    }

    private Role ensureSuperAdminRole() {
        Role role = roleRepository.findByRoleKey(EmployeeRole.SUPER_ADMIN.name())
                .orElseGet(() -> {
                    Role created = new Role();
                    created.setRoleKey(EmployeeRole.SUPER_ADMIN.name());
                    created.setDisplayName("Super Admin");
                    created.setSystemRole(true);
                    return created;
                });
        role.setDisplayName("Super Admin");
        role.setActive(true);
        role.setSystemRole(true);
        role.setAuthenticationMode(AuthenticationMode.PASSWORD_PIN);
        return roleRepository.save(role);
    }

    private String resolveBootstrapPassword() {
        if (bootstrapPassword != null && !bootstrapPassword.isBlank()) {
            return bootstrapPassword;
        }

        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            log.warn("event=bootstrap_super_admin_config_missing employeeId={} role={} reason=missing_bootstrap_password",
                    BOOTSTRAP_EMPLOYEE_ID, EmployeeRole.SUPER_ADMIN);
            throw new IllegalStateException("BOOTSTRAP_SUPER_ADMIN_PASSWORD is required when creating the first SUPER_ADMIN in prod");
        }

        log.warn("event=bootstrap_super_admin_config_missing employeeId={} role={} reason=development_fallback_password",
                BOOTSTRAP_EMPLOYEE_ID, EmployeeRole.SUPER_ADMIN);
        return DEVELOPMENT_FALLBACK_PASSWORD;
    }
}
