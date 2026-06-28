package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.TicketWorkflowHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketWorkflowHistoryRepository extends JpaRepository<TicketWorkflowHistory, Long> {

    List<TicketWorkflowHistory> findByTicketIdOrderByCreatedAtDesc(Long ticketId);

    @EntityGraph(attributePaths = {
            "workflowAction",
            "workflowTransition",
            "workflowTransition.fromStatusRecord",
            "workflowTransition.toStatusRecord",
            "fromStatusRecord",
            "toStatusRecord"
    })
    Page<TicketWorkflowHistory> findByTicketId(Long ticketId, Pageable pageable);
}
