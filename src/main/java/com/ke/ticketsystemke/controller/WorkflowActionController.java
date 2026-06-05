package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.WorkflowActionResponse;
import com.ke.ticketsystemke.service.WorkflowActionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/volt/workflow")
public class WorkflowActionController {

    private final WorkflowActionService workflowActionService;

    public WorkflowActionController(WorkflowActionService workflowActionService) {
        this.workflowActionService = workflowActionService;
    }

    @GetMapping("/actions")
    public List<WorkflowActionResponse> listActions(Authentication authentication) {
        return workflowActionService.listActions(authentication.getName());
    }
}
