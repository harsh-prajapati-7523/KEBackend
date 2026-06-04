package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.CreateWorkflowTransitionRequest;
import com.ke.ticketsystemke.dto.UpdateWorkflowTransitionRequest;
import com.ke.ticketsystemke.dto.WorkflowTransitionOptionsResponse;
import com.ke.ticketsystemke.dto.WorkflowTransitionResponse;
import com.ke.ticketsystemke.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/volt/workflow")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/transitions")
    public List<WorkflowTransitionResponse> listTransitions() {
        return workflowService.listTransitions();
    }

    @GetMapping("/transition-options")
    public WorkflowTransitionOptionsResponse getTransitionOptions(Authentication authentication) {
        return workflowService.getTransitionOptions(authentication.getName());
    }

    @PostMapping("/transitions")
    public ResponseEntity<WorkflowTransitionResponse> createTransition(
            @Valid @RequestBody CreateWorkflowTransitionRequest request,
            Authentication authentication
    ) {
        WorkflowTransitionResponse response = workflowService.createTransition(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/transitions/{id}")
    public WorkflowTransitionResponse updateTransition(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkflowTransitionRequest request,
            Authentication authentication
    ) {
        return workflowService.updateTransition(id, request, authentication.getName());
    }
}
