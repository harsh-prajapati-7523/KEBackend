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

    List<WorkflowTransition> findByFromStatusRecord_IdAndActiveTrueOrderBySortOrderAscIdAsc(Long fromStatusId);

    List<WorkflowTransition> findByActionKeyIn(List<String> actionKeys);

    Optional<WorkflowTransition> findByActionKeyAndFromStatusAndToStatus(
            String actionKey,
            TicketStatus fromStatus,
            TicketStatus toStatus
    );

    Optional<WorkflowTransition> findByActionKeyAndFromStatusRecord_IdAndToStatusRecord_Id(
            String actionKey,
            Long fromStatusId,
            Long toStatusId
    );

    List<WorkflowTransition> findAllByActionKeyAndFromStatusRecord_IdAndToStatusRecord_IdOrderByIdAsc(
            String actionKey,
            Long fromStatusId,
            Long toStatusId
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
