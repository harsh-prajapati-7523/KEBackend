package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.WorkflowTransitionRoleRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface WorkflowTransitionRoleRuleRepository extends JpaRepository<WorkflowTransitionRoleRule, Long> {

    @Query("select rule from WorkflowTransitionRoleRule rule order by rule.workflowTransition.id asc, rule.id asc")
    List<WorkflowTransitionRoleRule> findAllOrderedByTransitionIdAndId();

    List<WorkflowTransitionRoleRule> findAllByWorkflowTransition_IdOrderByIdAsc(Long workflowTransitionId);

    Optional<WorkflowTransitionRoleRule> findByIdAndWorkflowTransition_Id(Long id, Long workflowTransitionId);

    Optional<WorkflowTransitionRoleRule> findByWorkflowTransition_IdAndRole_Id(Long workflowTransitionId, Long roleId);

    boolean existsByWorkflowTransition_Id(Long workflowTransitionId);

    boolean existsByWorkflowTransition_IdAndRole_IdAndActiveTrue(Long workflowTransitionId, Long roleId);
}
