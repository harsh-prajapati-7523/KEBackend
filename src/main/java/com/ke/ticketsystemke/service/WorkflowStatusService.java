package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.WorkflowStatusResponse;
import com.ke.ticketsystemke.repository.WorkflowStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkflowStatusService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowStatusService.class);

    private final WorkflowStatusRepository workflowStatusRepository;

    public WorkflowStatusService(WorkflowStatusRepository workflowStatusRepository) {
        this.workflowStatusRepository = workflowStatusRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkflowStatusResponse> listStatuses(String employeeId) {
        List<WorkflowStatusResponse> statuses = workflowStatusRepository.findAllByOrderBySortOrderAscIdAsc()
                .stream()
                .map(WorkflowStatusResponse::from)
                .toList();
        log.info("event=workflow_statuses_returned employeeId={} statusCount={}", employeeId, statuses.size());
        return statuses;
    }
}
