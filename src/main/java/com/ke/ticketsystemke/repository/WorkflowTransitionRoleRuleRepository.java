package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.WorkflowTransitionRoleRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowTransitionRoleRuleRepository extends JpaRepository<WorkflowTransitionRoleRule, Long> {

    List<WorkflowTransitionRoleRule> findAllByWorkflowTransition_IdOrderByIdAsc(Long workflowTransitionId);

    Optional<WorkflowTransitionRoleRule> findByIdAndWorkflowTransition_Id(Long id, Long workflowTransitionId);

    Optional<WorkflowTransitionRoleRule> findByWorkflowTransition_IdAndRole_Id(Long workflowTransitionId, Long roleId);

    boolean existsByWorkflowTransition_Id(Long workflowTransitionId);

    boolean existsByWorkflowTransition_IdAndRole_IdAndActiveTrue(Long workflowTransitionId, Long roleId);
}
