package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.WorkflowTransitionRoleRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowTransitionRoleRuleRepository extends JpaRepository<WorkflowTransitionRoleRule, Long> {

    boolean existsByWorkflowTransition_Id(Long workflowTransitionId);

    boolean existsByWorkflowTransition_IdAndRole_IdAndActiveTrue(Long workflowTransitionId, Long roleId);
}
