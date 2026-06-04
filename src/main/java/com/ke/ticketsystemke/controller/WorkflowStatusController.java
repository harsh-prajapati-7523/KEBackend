package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.WorkflowStatusResponse;
import com.ke.ticketsystemke.service.WorkflowStatusService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/volt/workflow")
public class WorkflowStatusController {

    private final WorkflowStatusService workflowStatusService;

    public WorkflowStatusController(WorkflowStatusService workflowStatusService) {
        this.workflowStatusService = workflowStatusService;
    }

    @GetMapping("/statuses")
    public List<WorkflowStatusResponse> listStatuses(Authentication authentication) {
        return workflowStatusService.listStatuses(authentication.getName());
    }
}
