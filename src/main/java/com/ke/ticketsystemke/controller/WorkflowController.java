package com.ke.ticketsystemke.controller;

import com.ke.ticketsystemke.dto.CreateWorkflowTransitionRequest;
import com.ke.ticketsystemke.dto.UpdateWorkflowTransitionCategoryRuleRequest;
import com.ke.ticketsystemke.dto.UpdateWorkflowTransitionRoleRuleRequest;
import com.ke.ticketsystemke.dto.UpdateWorkflowTransitionRequest;
import com.ke.ticketsystemke.dto.UpsertWorkflowTransitionCategoryRuleRequest;
import com.ke.ticketsystemke.dto.UpsertWorkflowTransitionRoleRuleRequest;
import com.ke.ticketsystemke.dto.ValidateCategoryWorkflowRequest;
import com.ke.ticketsystemke.dto.WorkflowValidationResponse;
import com.ke.ticketsystemke.dto.WorkflowTransitionCategoryRuleResponse;
import com.ke.ticketsystemke.dto.WorkflowTransitionCategoryRulesResponse;
import com.ke.ticketsystemke.dto.WorkflowTransitionOptionsResponse;
import com.ke.ticketsystemke.dto.WorkflowTransitionRoleRuleResponse;
import com.ke.ticketsystemke.dto.WorkflowTransitionRoleRulesResponse;
import com.ke.ticketsystemke.dto.WorkflowTransitionResponse;
import com.ke.ticketsystemke.service.WorkflowService;
import com.ke.ticketsystemke.service.WorkflowValidationService;
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
    private final WorkflowValidationService workflowValidationService;

    public WorkflowController(
            WorkflowService workflowService,
            WorkflowValidationService workflowValidationService
    ) {
        this.workflowService = workflowService;
        this.workflowValidationService = workflowValidationService;
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

    @GetMapping("/transitions/{transitionId}/category-rules")
    public List<WorkflowTransitionCategoryRuleResponse> listCategoryRules(@PathVariable Long transitionId) {
        return workflowService.listCategoryRules(transitionId);
    }

    @GetMapping("/transition-category-rules")
    public WorkflowTransitionCategoryRulesResponse listAllCategoryRules() {
        return workflowService.listAllCategoryRules();
    }

    @PostMapping("/transitions/{transitionId}/category-rules")
    public ResponseEntity<WorkflowTransitionCategoryRuleResponse> createCategoryRule(
            @PathVariable Long transitionId,
            @Valid @RequestBody UpsertWorkflowTransitionCategoryRuleRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workflowService.createCategoryRule(transitionId, request, authentication.getName()));
    }

    @PatchMapping("/transitions/{transitionId}/category-rules/{ruleId}")
    public WorkflowTransitionCategoryRuleResponse updateCategoryRule(
            @PathVariable Long transitionId,
            @PathVariable Long ruleId,
            @Valid @RequestBody UpdateWorkflowTransitionCategoryRuleRequest request,
            Authentication authentication
    ) {
        return workflowService.updateCategoryRule(transitionId, ruleId, request, authentication.getName());
    }

    @GetMapping("/transitions/{transitionId}/role-rules")
    public List<WorkflowTransitionRoleRuleResponse> listRoleRules(@PathVariable Long transitionId) {
        return workflowService.listRoleRules(transitionId);
    }

    @GetMapping("/transition-role-rules")
    public WorkflowTransitionRoleRulesResponse listAllRoleRules() {
        return workflowService.listAllRoleRules();
    }

    @PostMapping("/transitions/{transitionId}/role-rules")
    public ResponseEntity<WorkflowTransitionRoleRuleResponse> createRoleRule(
            @PathVariable Long transitionId,
            @Valid @RequestBody UpsertWorkflowTransitionRoleRuleRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workflowService.createRoleRule(transitionId, request, authentication.getName()));
    }

    @PatchMapping("/transitions/{transitionId}/role-rules/{ruleId}")
    public WorkflowTransitionRoleRuleResponse updateRoleRule(
            @PathVariable Long transitionId,
            @PathVariable Long ruleId,
            @Valid @RequestBody UpdateWorkflowTransitionRoleRuleRequest request,
            Authentication authentication
    ) {
        return workflowService.updateRoleRule(transitionId, ruleId, request, authentication.getName());
    }

    @PostMapping("/validate-category-workflow")
    public WorkflowValidationResponse validateCategoryWorkflow(
            @Valid @RequestBody ValidateCategoryWorkflowRequest request
    ) {
        return workflowValidationService.validateCategoryWorkflow(request.getCategoryId());
    }
}
