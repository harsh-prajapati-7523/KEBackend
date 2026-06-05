package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.WorkflowActionResponse;
import com.ke.ticketsystemke.repository.WorkflowActionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkflowActionService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowActionService.class);

    private final WorkflowActionRepository workflowActionRepository;

    public WorkflowActionService(WorkflowActionRepository workflowActionRepository) {
        this.workflowActionRepository = workflowActionRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkflowActionResponse> listActions(String employeeId) {
        List<WorkflowActionResponse> actions = workflowActionRepository.findAllByOrderBySortOrderAscActionKeyAsc()
                .stream()
                .map(WorkflowActionResponse::from)
                .toList();
        log.info("event=workflow_actions_returned employeeId={} actionCount={}", employeeId, actions.size());
        return actions;
    }
}
