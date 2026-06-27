package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.WorkflowTransitionCategoryRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowTransitionCategoryRuleRepository extends JpaRepository<WorkflowTransitionCategoryRule, Long> {

    List<WorkflowTransitionCategoryRule> findAllByWorkflowTransition_IdOrderByIdAsc(Long workflowTransitionId);

    Optional<WorkflowTransitionCategoryRule> findByIdAndWorkflowTransition_Id(Long id, Long workflowTransitionId);

    Optional<WorkflowTransitionCategoryRule> findByWorkflowTransition_IdAndCategory_Id(Long workflowTransitionId, Long categoryId);

    boolean existsByWorkflowTransition_Id(Long workflowTransitionId);

    boolean existsByWorkflowTransition_IdAndCategory_IdAndActiveTrue(Long workflowTransitionId, Long categoryId);
}
