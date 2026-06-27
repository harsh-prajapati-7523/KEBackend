package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.WorkflowValidationIssueResponse;
import com.ke.ticketsystemke.dto.WorkflowValidationResponse;
import com.ke.ticketsystemke.entity.TicketCategoryConfig;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowAction;
import com.ke.ticketsystemke.entity.WorkflowMode;
import com.ke.ticketsystemke.entity.WorkflowTransition;
import com.ke.ticketsystemke.repository.TicketCategoryRepository;
import com.ke.ticketsystemke.repository.WorkflowActionRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionCategoryRuleRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class WorkflowValidationService {

    private final TicketCategoryRepository ticketCategoryRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final WorkflowActionRepository workflowActionRepository;
    private final WorkflowTransitionCategoryRuleRepository workflowTransitionCategoryRuleRepository;

    public WorkflowValidationService(
            TicketCategoryRepository ticketCategoryRepository,
            WorkflowTransitionRepository workflowTransitionRepository,
            WorkflowActionRepository workflowActionRepository,
            WorkflowTransitionCategoryRuleRepository workflowTransitionCategoryRuleRepository
    ) {
        this.ticketCategoryRepository = ticketCategoryRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
        this.workflowActionRepository = workflowActionRepository;
        this.workflowTransitionCategoryRuleRepository = workflowTransitionCategoryRuleRepository;
    }

    @Transactional(readOnly = true)
    public WorkflowValidationResponse validateCategoryWorkflow(Long categoryId) {
        return validateCategoryWorkflow(categoryId, false);
    }

    @Transactional(readOnly = true)
    public WorkflowValidationResponse validateCategoryWorkflow(Long categoryId, boolean requireDbConfiguredReady) {
        TicketCategoryConfig category = ticketCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        List<WorkflowValidationIssueResponse> issues = new ArrayList<>();
        Map<String, Integer> duplicateCounts = new HashMap<>();
        Map<String, List<WorkflowTransition>> ambiguousTransitionsByKey = new HashMap<>();
        int validTransitionCount = 0;

        for (WorkflowTransition transition : workflowTransitionRepository.findAll()) {
            validateTransitionReferences(transition, issues);
            if (!transition.isActive()) {
                continue;
            }

            duplicateCounts.merge(duplicateKey(transition), 1, Integer::sum);
            Optional<WorkflowAction> action = workflowActionRepository.findByActionKey(transition.getActionKey());
            if (action.isEmpty() || !action.get().isActive()) {
                addIssue(issues, "INACTIVE_OR_MISSING_ACTION", "Active transition uses an inactive or missing action", transition.getId());
                continue;
            }
            if (action.get().isProtectedAction() || transition.isProtectedTransition()) {
                continue;
            }
            if (transition.getToStatusRecord() != null && !transition.getToStatusRecord().isActive()) {
                addIssue(issues, "INACTIVE_TARGET_STATUS", "Active transition targets an inactive workflow status", transition.getId());
                continue;
            }
            if (isTerminalSource(transition)) {
                addIssue(issues, "TERMINAL_SOURCE_STATUS", "Active transition has a terminal source status", transition.getId());
                continue;
            }
            if (!isCategoryAllowed(transition, category.getId())) {
                continue;
            }

            validTransitionCount++;
            ambiguousTransitionsByKey
                    .computeIfAbsent(ambiguityKey(transition), ignored -> new ArrayList<>())
                    .add(transition);
        }

        duplicateCounts.forEach((key, count) -> {
            if (count > 1) {
                addIssue(issues, "DUPLICATE_ACTIVE_TRANSITION", "Duplicate active transition found for " + key, null);
            }
        });

        ambiguousTransitionsByKey.forEach((key, transitions) -> {
            if (transitions.size() > 1) {
                addIssue(issues, "AMBIGUOUS_TRANSITION", "Multiple active transitions match " + key, null);
            }
        });

        if ((requireDbConfiguredReady
                || category.getWorkflowMode() == WorkflowMode.DB_CONFIGURED && category.isDbWorkflowEnabled())
                && validTransitionCount == 0) {
            addIssue(issues, "NO_VALID_TRANSITIONS", "DB_CONFIGURED category has no valid active transitions", null);
        }

        return new WorkflowValidationResponse(
                category.getId(),
                category.getCategoryKey(),
                issues.isEmpty(),
                issues
        );
    }

    @Transactional(readOnly = true)
    public void requireValidCategoryWorkflow(Long categoryId) {
        WorkflowValidationResponse response = validateCategoryWorkflow(categoryId, true);
        if (!response.valid()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category workflow configuration is invalid");
        }
    }

    private void validateTransitionReferences(
            WorkflowTransition transition,
            List<WorkflowValidationIssueResponse> issues
    ) {
        if (transition.getActionKey() == null || workflowActionRepository.findByActionKey(transition.getActionKey()).isEmpty()) {
            addIssue(issues, "MISSING_ACTION_REFERENCE", "Transition references a missing workflow action", transition.getId());
        }
        if (transition.getFromStatusRecord() == null || transition.getToStatusRecord() == null) {
            addIssue(issues, "MISSING_STATUS_REFERENCE", "Transition references a missing workflow status", transition.getId());
        }
    }

    private boolean isCategoryAllowed(WorkflowTransition transition, Long categoryId) {
        if (!workflowTransitionCategoryRuleRepository.existsByWorkflowTransition_Id(transition.getId())) {
            return true;
        }
        return workflowTransitionCategoryRuleRepository
                .existsByWorkflowTransition_IdAndCategory_IdAndActiveTrue(transition.getId(), categoryId);
    }

    private boolean isTerminalSource(WorkflowTransition transition) {
        if (transition.getFromStatusRecord() != null && transition.getFromStatusRecord().isTerminal()) {
            return true;
        }
        TicketStatus fromStatus = transition.getFromStatus();
        return fromStatus == TicketStatus.COMPLETED || fromStatus == TicketStatus.CANCELLED;
    }

    private String duplicateKey(WorkflowTransition transition) {
        return String.join(
                "|",
                safe(transition.getActionKey()),
                String.valueOf(transition.getFromStatusRecord() == null ? null : transition.getFromStatusRecord().getId()),
                String.valueOf(transition.getToStatusRecord() == null ? null : transition.getToStatusRecord().getId())
        );
    }

    private String ambiguityKey(WorkflowTransition transition) {
        return String.join(
                "|",
                safe(transition.getActionKey()),
                String.valueOf(transition.getFromStatusRecord() == null ? null : transition.getFromStatusRecord().getId())
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void addIssue(
            List<WorkflowValidationIssueResponse> issues,
            String code,
            String message,
            Long transitionId
    ) {
        issues.add(new WorkflowValidationIssueResponse(code, message, transitionId));
    }
}
