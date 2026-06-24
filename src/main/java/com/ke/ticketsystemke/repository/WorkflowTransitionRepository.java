package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.AccessKey;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowTransition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {

    List<WorkflowTransition> findAllByOrderBySortOrderAscIdAsc();

    List<WorkflowTransition> findByFromStatusAndActiveTrueOrderBySortOrderAscIdAsc(TicketStatus fromStatus);

    Optional<WorkflowTransition> findByActionKeyAndFromStatusAndToStatus(
            String actionKey,
            TicketStatus fromStatus,
            TicketStatus toStatus
    );

    default Optional<WorkflowTransition> findByActionKeyAndFromStatusAndToStatus(
            AccessKey actionKey,
            TicketStatus fromStatus,
            TicketStatus toStatus
    ) {
        return actionKey == null
                ? Optional.empty()
                : findByActionKeyAndFromStatusAndToStatus(actionKey.name(), fromStatus, toStatus);
    }
}
