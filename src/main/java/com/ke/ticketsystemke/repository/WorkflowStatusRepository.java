package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowStatusRepository extends JpaRepository<WorkflowStatus, Long> {

    List<WorkflowStatus> findAllByOrderBySortOrderAscIdAsc();

    Optional<WorkflowStatus> findByStatusKey(String statusKey);

    boolean existsByStatusKey(String statusKey);
}
