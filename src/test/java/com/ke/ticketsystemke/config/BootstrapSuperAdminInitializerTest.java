package com.ke.ticketsystemke.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.ke.ticketsystemke.entity.AuthenticationMode;
import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.EmployeeRole;
import com.ke.ticketsystemke.entity.Role;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class BootstrapSuperAdminInitializerTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void existingBootstrapSuperAdminKeepsStoredPasswordByDefault() {
        Role superAdminRole = superAdminRole();
        Employee existingEmployee = new Employee();
        existingEmployee.setEmployeeId("SUPER_ADMIN_001");
        existingEmployee.setName("Changed Name");
        existingEmployee.setRole(EmployeeRole.SUPER_ADMIN);
        existingEmployee.setRoleRecord(superAdminRole);
        existingEmployee.setActive(false);
        existingEmployee.setPassword("existing-password-hash");

        when(roleRepository.findByRoleKey("SUPER_ADMIN")).thenReturn(Optional.of(superAdminRole));
        when(roleRepository.save(superAdminRole)).thenReturn(superAdminRole);
        when(employeeRepository.findByEmployeeIdIgnoreCase("SUPER_ADMIN_001")).thenReturn(Optional.of(existingEmployee));

        BootstrapSuperAdminInitializer initializer = new BootstrapSuperAdminInitializer(
                employeeRepository,
                roleRepository,
                passwordEncoder,
                new MockEnvironment(),
                "default-password",
                false
        );

        initializer.run(new DefaultApplicationArguments());

        assertThat(existingEmployee.getPassword()).isEqualTo("existing-password-hash");
        assertThat(existingEmployee.isActive()).isTrue();
        assertThat(existingEmployee.getRoleRecord()).isSameAs(superAdminRole);
        verify(passwordEncoder, never()).encode("default-password");
        verify(employeeRepository).save(existingEmployee);
    }

    @Test
    void existingBootstrapSuperAdminPasswordResetIsExplicit() {
        Role superAdminRole = superAdminRole();
        Employee existingEmployee = new Employee();
        existingEmployee.setEmployeeId("SUPER_ADMIN_001");
        existingEmployee.setName("Changed Name");
        existingEmployee.setRole(EmployeeRole.SUPER_ADMIN);
        existingEmployee.setRoleRecord(superAdminRole);
        existingEmployee.setActive(true);
        existingEmployee.setPassword("existing-password-hash");

        when(roleRepository.findByRoleKey("SUPER_ADMIN")).thenReturn(Optional.of(superAdminRole));
        when(roleRepository.save(superAdminRole)).thenReturn(superAdminRole);
        when(employeeRepository.findByEmployeeIdIgnoreCase("SUPER_ADMIN_001")).thenReturn(Optional.of(existingEmployee));
        when(passwordEncoder.encode("default-password")).thenReturn("new-password-hash");

        BootstrapSuperAdminInitializer initializer = new BootstrapSuperAdminInitializer(
                employeeRepository,
                roleRepository,
                passwordEncoder,
                new MockEnvironment(),
                "default-password",
                true
        );

        initializer.run(new DefaultApplicationArguments());

        assertThat(existingEmployee.getPassword()).isEqualTo("new-password-hash");
        verify(employeeRepository).save(existingEmployee);
    }

    @Test
    void createsMissingBootstrapSuperAdminWithConfiguredPassword() {
        Role superAdminRole = superAdminRole();

        when(roleRepository.findByRoleKey("SUPER_ADMIN")).thenReturn(Optional.of(superAdminRole));
        when(roleRepository.save(superAdminRole)).thenReturn(superAdminRole);
        when(employeeRepository.findByEmployeeIdIgnoreCase("SUPER_ADMIN_001")).thenReturn(Optional.empty());
        when(employeeRepository.existsByRole(EmployeeRole.SUPER_ADMIN)).thenReturn(false);
        when(employeeRepository.findAllByRoleRecord_RoleKey("SUPER_ADMIN")).thenReturn(java.util.List.of());
        when(passwordEncoder.encode("default-password")).thenReturn("new-password-hash");

        BootstrapSuperAdminInitializer initializer = new BootstrapSuperAdminInitializer(
                employeeRepository,
                roleRepository,
                passwordEncoder,
                new MockEnvironment(),
                "default-password",
                false
        );

        initializer.run(new DefaultApplicationArguments());

        ArgumentCaptor<Employee> employeeCaptor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(employeeCaptor.capture());
        Employee createdEmployee = employeeCaptor.getValue();
        assertThat(createdEmployee.getEmployeeId()).isEqualTo("SUPER_ADMIN_001");
        assertThat(createdEmployee.getPassword()).isEqualTo("new-password-hash");
        assertThat(createdEmployee.getRole()).isEqualTo(EmployeeRole.SUPER_ADMIN);
        assertThat(createdEmployee.getRoleRecord()).isSameAs(superAdminRole);
        assertThat(createdEmployee.isActive()).isTrue();
    }

    private Role superAdminRole() {
        Role role = new Role();
        role.setRoleKey("SUPER_ADMIN");
        role.setDisplayName("Super Admin");
        role.setActive(true);
        role.setSystemRole(true);
        role.setAuthenticationMode(AuthenticationMode.PASSWORD_PIN);
        return role;
    }
}
