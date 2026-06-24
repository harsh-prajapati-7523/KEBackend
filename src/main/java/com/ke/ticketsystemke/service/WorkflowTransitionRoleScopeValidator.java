package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.Role;
import com.ke.ticketsystemke.entity.WorkflowTransition;
import com.ke.ticketsystemke.repository.EmployeeRepository;
import com.ke.ticketsystemke.repository.RoleRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionRoleRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkflowTransitionRoleScopeValidator {

    private static final Logger log = LoggerFactory.getLogger(WorkflowTransitionRoleScopeValidator.class);

    private final WorkflowTransitionRoleRuleRepository workflowTransitionRoleRuleRepository;
    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;

    public WorkflowTransitionRoleScopeValidator(
            WorkflowTransitionRoleRuleRepository workflowTransitionRoleRuleRepository,
            EmployeeRepository employeeRepository,
            RoleRepository roleRepository
    ) {
        this.workflowTransitionRoleRuleRepository = workflowTransitionRoleRuleRepository;
        this.employeeRepository = employeeRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public boolean isAllowed(WorkflowTransition transition, String employeeId) {
        if (transition == null || transition.getId() == null) {
            return false;
        }

        Long transitionId = transition.getId();
        if (!workflowTransitionRoleRuleRepository.existsByWorkflowTransition_Id(transitionId)) {
            return true;
        }

        try {
            Role role = resolveActiveRole(resolveActiveEmployee(employeeId));
            return workflowTransitionRoleRuleRepository
                    .existsByWorkflowTransition_IdAndRole_IdAndActiveTrue(transitionId, role.getId());
        } catch (RuntimeException ex) {
            log.warn(
                    "event=workflow_transition_role_scope_check_failed employeeId={} transitionId={} decision=deny",
                    employeeId,
                    transitionId
            );
            return false;
        }
    }

    @Transactional(readOnly = true)
    public void requireAllowed(WorkflowTransition transition, String employeeId) {
        if (!isAllowed(transition, employeeId)) {
            log.warn(
                    "event=workflow_transition_role_scope_denied employeeId={} transitionId={} decision=deny",
                    employeeId,
                    transition == null ? null : transition.getId()
            );
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Workflow transition is not available");
        }
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

    private Role resolveActiveRole(Employee employee) {
        Role role = employee.getRoleRecord();
        if (role == null && employee.getRole() != null) {
            role = roleRepository.findByRoleKey(employee.getRole().name()).orElse(null);
        }
        if (role == null || !role.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inactive role");
        }
        return role;
    }
}
