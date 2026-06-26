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

        Employee employee = resolveActiveEmployee(employeeId);
        Role role = resolveActiveRole(employee);
        if (role == null) {
            log.warn(
                    "event=workflow_transition_role_scope_denied employeeId={} transitionId={} reason=role_unavailable decision=deny",
                    employeeId,
                    transitionId
            );
            return false;
        }
        return workflowTransitionRoleRuleRepository
                .existsByWorkflowTransition_IdAndRole_IdAndActiveTrue(transitionId, role.getId());
    }

    @Transactional(readOnly = true, noRollbackFor = ResponseStatusException.class)
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
        Employee employee = employeeRepository.findByEmployeeIdIgnoreCase(lookupEmployeeId).orElse(null);
        if (employee == null) {
            log.warn("event=workflow_transition_role_scope_denied employeeId={} reason=employee_missing decision=deny", employeeId);
            return null;
        }
        if (!employee.isActive()) {
            log.warn("event=workflow_transition_role_scope_denied employeeId={} reason=employee_inactive decision=deny", employeeId);
            return null;
        }
        return employee;
    }

    private Role resolveActiveRole(Employee employee) {
        if (employee == null) {
            return null;
        }
        Role role = employee.getRoleRecord();
        if (role == null && employee.getRole() != null) {
            role = roleRepository.findByRoleKey(employee.getRole().name()).orElse(null);
        }
        if (role == null || !role.isActive()) {
            return null;
        }
        return role;
    }
}
