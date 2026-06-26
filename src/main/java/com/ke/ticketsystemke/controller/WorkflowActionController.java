package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.CreateWorkflowActionRequest;
import com.ke.ticketsystemke.dto.WorkflowActionResponse;
import com.ke.ticketsystemke.service.WorkflowActionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping("/actions")
    public ResponseEntity<WorkflowActionResponse> createAction(
            @Valid @RequestBody CreateWorkflowActionRequest request,
            Authentication authentication
    ) {
        WorkflowActionResponse response = workflowActionService.createAction(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
