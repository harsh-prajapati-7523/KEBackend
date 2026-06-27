package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.CreateWorkflowActionRequest;
import com.ke.ticketsystemke.dto.UpdateWorkflowActionRequest;
import com.ke.ticketsystemke.dto.UpdateWorkflowActionStateRequest;
import com.ke.ticketsystemke.dto.WorkflowActionResponse;
import com.ke.ticketsystemke.service.WorkflowActionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @PatchMapping("/actions/{id}")
    public WorkflowActionResponse updateAction(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkflowActionRequest request,
            Authentication authentication
    ) {
        return workflowActionService.updateAction(id, request, authentication.getName());
    }

    @PatchMapping("/actions/{id}/status")
    public WorkflowActionResponse updateActionState(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkflowActionStateRequest request,
            Authentication authentication
    ) {
        return workflowActionService.updateActionState(id, request.getActive(), authentication.getName());
    }
}
