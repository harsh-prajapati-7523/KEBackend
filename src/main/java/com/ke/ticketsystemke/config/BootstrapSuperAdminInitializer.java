package com.ke.ticketsystemke.config;

import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.EmployeeRole;
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

    public BootstrapSuperAdminInitializer(
            EmployeeRepository employeeRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            Environment environment,
            @Value("${BOOTSTRAP_SUPER_ADMIN_PASSWORD:}") String bootstrapPassword
    ) {
        this.employeeRepository = employeeRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (employeeRepository.existsByRole(EmployeeRole.SUPER_ADMIN)) {
            log.info("event=bootstrap_super_admin_skipped employeeId={} role={} reason=existing_super_admin",
                    BOOTSTRAP_EMPLOYEE_ID, EmployeeRole.SUPER_ADMIN);
            return;
        }

        if (employeeRepository.existsByEmployeeIdIgnoreCase(BOOTSTRAP_EMPLOYEE_ID)) {
            log.warn("event=bootstrap_super_admin_failed employeeId={} role={} reason=employee_id_already_exists",
                    BOOTSTRAP_EMPLOYEE_ID, EmployeeRole.SUPER_ADMIN);
            throw new IllegalStateException("Cannot create bootstrap SUPER_ADMIN because employeeId already exists");
        }

        String password = resolveBootstrapPassword();

        Employee employee = new Employee();
        employee.setEmployeeId(BOOTSTRAP_EMPLOYEE_ID);
        employee.setName(BOOTSTRAP_NAME);
        employee.setRole(EmployeeRole.SUPER_ADMIN);
        roleRepository.findByRoleKey(EmployeeRole.SUPER_ADMIN.name())
                .ifPresent(employee::setRoleRecord);
        employee.setActive(true);
        employee.setPassword(passwordEncoder.encode(password));

        employeeRepository.save(employee);
        log.info("event=bootstrap_super_admin_created employeeId={} role={}",
                BOOTSTRAP_EMPLOYEE_ID, EmployeeRole.SUPER_ADMIN);
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
