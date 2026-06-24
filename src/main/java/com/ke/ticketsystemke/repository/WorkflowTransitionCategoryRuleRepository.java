package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.WorkflowTransitionCategoryRule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowTransitionCategoryRuleRepository extends JpaRepository<WorkflowTransitionCategoryRule, Long> {

    boolean existsByWorkflowTransition_Id(Long workflowTransitionId);

    boolean existsByWorkflowTransition_IdAndCategory_IdAndActiveTrue(Long workflowTransitionId, Long categoryId);
}
