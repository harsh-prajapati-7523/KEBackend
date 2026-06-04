package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.CreateWorkflowStatusRequest;
import com.ke.ticketsystemke.dto.UpdateWorkflowStatusRequest;
import com.ke.ticketsystemke.dto.UpdateWorkflowStatusStateRequest;
import com.ke.ticketsystemke.dto.WorkflowStatusResponse;
import com.ke.ticketsystemke.service.WorkflowStatusService;
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
public class WorkflowStatusController {

    private final WorkflowStatusService workflowStatusService;

    public WorkflowStatusController(WorkflowStatusService workflowStatusService) {
        this.workflowStatusService = workflowStatusService;
    }

    @GetMapping("/statuses")
    public List<WorkflowStatusResponse> listStatuses(Authentication authentication) {
        return workflowStatusService.listStatuses(authentication.getName());
    }

    @PostMapping("/statuses")
    public ResponseEntity<WorkflowStatusResponse> createStatus(
            @Valid @RequestBody CreateWorkflowStatusRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workflowStatusService.createStatus(request, authentication.getName()));
    }

    @PatchMapping("/statuses/{id}")
    public WorkflowStatusResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkflowStatusRequest request,
            Authentication authentication
    ) {
        return workflowStatusService.updateStatus(id, request, authentication.getName());
    }

    @PatchMapping("/statuses/{id}/status")
    public WorkflowStatusResponse updateStatusState(
            @PathVariable Long id,
            @Valid @RequestBody UpdateWorkflowStatusStateRequest request,
            Authentication authentication
    ) {
        return workflowStatusService.updateStatusState(id, request, authentication.getName());
    }
}
