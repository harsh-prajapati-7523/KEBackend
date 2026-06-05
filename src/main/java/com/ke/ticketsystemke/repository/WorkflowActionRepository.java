package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.WorkflowAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowActionRepository extends JpaRepository<WorkflowAction, Long> {

    List<WorkflowAction> findAllByOrderBySortOrderAscActionKeyAsc();

    Optional<WorkflowAction> findByActionKey(String actionKey);

    boolean existsByActionKey(String actionKey);
}
