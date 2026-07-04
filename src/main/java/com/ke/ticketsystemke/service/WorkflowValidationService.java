package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.dto.WorkflowValidationIssueResponse;
import com.ke.ticketsystemke.dto.WorkflowValidationResponse;
import com.ke.ticketsystemke.entity.AccessKeyMetadata;
import com.ke.ticketsystemke.entity.Role;
import com.ke.ticketsystemke.entity.RoleAccessRule;
import com.ke.ticketsystemke.entity.TicketCategoryConfig;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowAction;
import com.ke.ticketsystemke.entity.WorkflowMode;
import com.ke.ticketsystemke.entity.WorkflowTransition;
import com.ke.ticketsystemke.entity.WorkflowTransitionRoleRule;
import com.ke.ticketsystemke.repository.AccessKeyMetadataRepository;
import com.ke.ticketsystemke.repository.RoleAccessRuleRepository;
import com.ke.ticketsystemke.repository.RoleRepository;
import com.ke.ticketsystemke.repository.TicketCategoryRepository;
import com.ke.ticketsystemke.repository.WorkflowActionRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionCategoryRuleRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionRoleRuleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class WorkflowValidationService {

    private final TicketCategoryRepository ticketCategoryRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final WorkflowActionRepository workflowActionRepository;
    private final WorkflowTransitionCategoryRuleRepository workflowTransitionCategoryRuleRepository;
    private final WorkflowTransitionRoleRuleRepository workflowTransitionRoleRuleRepository;
    private final RoleRepository roleRepository;
    private final RoleAccessRuleRepository roleAccessRuleRepository;
    private final AccessKeyMetadataRepository accessKeyMetadataRepository;

    public WorkflowValidationService(
            TicketCategoryRepository ticketCategoryRepository,
            WorkflowTransitionRepository workflowTransitionRepository,
            WorkflowActionRepository workflowActionRepository,
            WorkflowTransitionCategoryRuleRepository workflowTransitionCategoryRuleRepository,
            WorkflowTransitionRoleRuleRepository workflowTransitionRoleRuleRepository,
            RoleRepository roleRepository,
            RoleAccessRuleRepository roleAccessRuleRepository,
            AccessKeyMetadataRepository accessKeyMetadataRepository
    ) {
        this.ticketCategoryRepository = ticketCategoryRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
        this.workflowActionRepository = workflowActionRepository;
        this.workflowTransitionCategoryRuleRepository = workflowTransitionCategoryRuleRepository;
        this.workflowTransitionRoleRuleRepository = workflowTransitionRoleRuleRepository;
        this.roleRepository = roleRepository;
        this.roleAccessRuleRepository = roleAccessRuleRepository;
        this.accessKeyMetadataRepository = accessKeyMetadataRepository;
    }

    @Transactional(readOnly = true)
    public WorkflowValidationResponse validateCategoryWorkflow(Long categoryId) {
        return validateCategoryWorkflow(categoryId, false);
    }

    @Transactional(readOnly = true)
    public WorkflowValidationResponse validateCategoryWorkflow(Long categoryId, boolean requireDbConfiguredReady) {
        TicketCategoryConfig category = ticketCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));

        List<WorkflowValidationIssueResponse> blockingIssues = new ArrayList<>();
        List<WorkflowValidationIssueResponse> warnings = new ArrayList<>();
        Map<String, Integer> duplicateCounts = new HashMap<>();
        Map<String, List<WorkflowTransition>> ambiguousTransitionsByKey = new HashMap<>();
        List<WorkflowTransition> executableCategoryTransitions = new ArrayList<>();
        int validTransitionCount = 0;

        for (WorkflowTransition transition : workflowTransitionRepository.findAll()) {
            validateTransitionReferences(transition, blockingIssues);
            if (!transition.isActive()) {
                continue;
            }

            duplicateCounts.merge(duplicateKey(transition), 1, Integer::sum);
            Optional<WorkflowAction> action = workflowActionRepository.findByActionKey(transition.getActionKey());
            if (action.isEmpty() || !action.get().isActive()) {
                addIssue(blockingIssues, "INACTIVE_OR_MISSING_ACTION", "Active transition uses an inactive or missing action", transition.getId());
                reportInactiveActionAccessMetadata(transition, warnings);
                continue;
            }
            if ((action.get().isProtectedAction() || transition.isProtectedTransition())
                    && !isCategoryExplicitlyAllowed(transition, category.getId())) {
                continue;
            }
            if (transition.getToStatusRecord() != null && !transition.getToStatusRecord().isActive()) {
                addIssue(blockingIssues, "INACTIVE_TARGET_STATUS", "Active transition targets an inactive workflow status", transition.getId());
                continue;
            }
            if (transition.getFromStatusRecord() != null && !transition.getFromStatusRecord().isActive()) {
                addIssue(blockingIssues, "INACTIVE_SOURCE_STATUS", "Active transition uses an inactive source workflow status", transition.getId());
                continue;
            }
            if (isTerminalSource(transition)) {
                addIssue(blockingIssues, "TERMINAL_SOURCE_STATUS", "Active transition has a terminal source status", transition.getId());
                continue;
            }
            if (!isCategoryAllowed(transition, category.getId())) {
                continue;
            }
            if (hasRuntimeUnsupportedGenericExecution(transition, action.get(), blockingIssues)) {
                continue;
            }

            validTransitionCount++;
            executableCategoryTransitions.add(transition);
            ambiguousTransitionsByKey
                    .computeIfAbsent(ambiguityKey(transition), ignored -> new ArrayList<>())
                    .add(transition);

            validateRoleCoverage(transition, action.get(), warnings, blockingIssues);
        }

        duplicateCounts.forEach((key, count) -> {
            if (count > 1) {
                addIssue(blockingIssues, "DUPLICATE_ACTIVE_TRANSITION", "Duplicate active transition found for " + key, null);
            }
        });

        ambiguousTransitionsByKey.forEach((key, transitions) -> {
            if (transitions.size() > 1) {
                addIssue(blockingIssues, "AMBIGUOUS_TRANSITION", "Multiple active transitions match " + key, null);
            }
        });

        if ((requireDbConfiguredReady
                || category.getWorkflowMode() == WorkflowMode.DB_CONFIGURED && category.isDbWorkflowEnabled())
                && validTransitionCount == 0) {
            addIssue(blockingIssues, "NO_VALID_TRANSITIONS", "DB_CONFIGURED category has no valid active transitions", null);
        }

        validateReachability(executableCategoryTransitions, blockingIssues);

        return new WorkflowValidationResponse(
                category.getId(),
                category.getCategoryKey(),
                blockingIssues.isEmpty(),
                blockingIssues,
                blockingIssues.isEmpty(),
                blockingIssues,
                warnings
        );
    }

    @Transactional(readOnly = true)
    public void requireValidCategoryWorkflow(Long categoryId) {
        WorkflowValidationResponse response = validateCategoryWorkflow(categoryId, true);
        if (!response.valid()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, buildInvalidWorkflowMessage(response));
        }
    }

    private String buildInvalidWorkflowMessage(WorkflowValidationResponse response) {
        WorkflowValidationIssueResponse primaryIssue = response.blockingIssues().stream()
                .findFirst()
                .orElse(null);
        if (primaryIssue == null) {
            return "Category workflow configuration is invalid";
        }

        String transitionContext = primaryIssue.transitionId() == null
                ? ""
                : " on transition #" + primaryIssue.transitionId();
        String message = primaryIssue.message() == null || primaryIssue.message().isBlank()
                ? primaryIssue.code()
                : primaryIssue.message();
        return limitMessage("Workflow invalid: " + primaryIssue.code() + transitionContext + " - " + message);
    }

    private String limitMessage(String message) {
        int maxLength = 160;
        if (message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength - 3) + "...";
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
            return;
        }
        if (transition.getFromStatusRecord().getBehaviorBucket() != null
                && transition.getFromStatus() != transition.getFromStatusRecord().getBehaviorBucket()) {
            addIssue(issues, "STALE_SOURCE_STATUS_BEHAVIOR", "Transition source behavior is out of sync with source workflow status", transition.getId());
        }
        if (transition.getToStatusRecord().getBehaviorBucket() != null
                && transition.getToStatus() != transition.getToStatusRecord().getBehaviorBucket()) {
            addIssue(issues, "STALE_TARGET_STATUS_BEHAVIOR", "Transition target behavior is out of sync with target workflow status", transition.getId());
        }
    }

    private boolean isCategoryAllowed(WorkflowTransition transition, Long categoryId) {
        if (!workflowTransitionCategoryRuleRepository.existsByWorkflowTransition_Id(transition.getId())) {
            return true;
        }
        return workflowTransitionCategoryRuleRepository
                .existsByWorkflowTransition_IdAndCategory_IdAndActiveTrue(transition.getId(), categoryId);
    }

    private boolean isCategoryExplicitlyAllowed(WorkflowTransition transition, Long categoryId) {
        return workflowTransitionCategoryRuleRepository
                .existsByWorkflowTransition_IdAndCategory_IdAndActiveTrue(transition.getId(), categoryId);
    }

    private void validateRoleCoverage(
            WorkflowTransition transition,
            WorkflowAction action,
            List<WorkflowValidationIssueResponse> warnings,
            List<WorkflowValidationIssueResponse> blockingIssues
    ) {
        List<Role> activeRoles = roleRepository.findAll()
                .stream()
                .filter(Role::isActive)
                .toList();
        List<WorkflowTransitionRoleRule> activeRoleRules = workflowTransitionRoleRuleRepository
                .findAllByWorkflowTransition_IdOrderByIdAsc(transition.getId())
                .stream()
                .filter(WorkflowTransitionRoleRule::isActive)
                .toList();

        if (activeRoleRules.isEmpty()) {
            addIssue(warnings, "MISSING_ROLE_TRANSITION_SCOPE_COVERAGE", "No transition-specific role scope is configured. This transition is available to all roles that have permission for this action.", transition.getId());
        }

        List<Role> candidateRoles = activeRoleRules.isEmpty()
                ? activeRoles
                : activeRoleRules.stream()
                        .map(WorkflowTransitionRoleRule::getRole)
                        .filter(role -> role != null && role.isActive())
                        .toList();

        if (candidateRoles.isEmpty()) {
            addIssue(blockingIssues, "NO_ACTIVE_ROLE_SCOPE", "Active transition is not executable by any active role", transition.getId());
            return;
        }

        boolean executableByAnyRole = false;
        for (Role role : candidateRoles) {
            boolean allowed = "SUPER_ADMIN".equals(role.getRoleKey())
                    || roleAccessRuleRepository.findByRoleIdAndAccessKey(role.getId(), action.getActionKey())
                            .map(RoleAccessRule::isAllowed)
                            .orElse(false);
            if (allowed) {
                executableByAnyRole = true;
            } else if (!activeRoleRules.isEmpty()) {
                addIssue(
                        blockingIssues,
                        "MISSING_ROLE_ACCESS_GRANT",
                        "Role " + role.getRoleKey() + " is scoped to transition but lacks action access " + action.getActionKey(),
                        transition.getId()
                );
            }
        }

        if (!executableByAnyRole) {
            addIssue(blockingIssues, "NOT_EXECUTABLE_BY_ACTIVE_ROLE", "Active transition cannot be executed by any active role", transition.getId());
        }
    }

    private void reportInactiveActionAccessMetadata(
            WorkflowTransition transition,
            List<WorkflowValidationIssueResponse> warnings
    ) {
        if (transition.getActionKey() == null) {
            return;
        }
        Optional<AccessKeyMetadata> metadata = accessKeyMetadataRepository.findByAccessKey(transition.getActionKey());
        if (metadata.isPresent() && metadata.get().isActive()) {
            addIssue(
                    warnings,
                    "INACTIVE_ACTION_ACCESS_METADATA_ACTIVE",
                    "Access metadata is active for an inactive workflow action",
                    transition.getId()
            );
        }
    }

    private boolean hasRuntimeUnsupportedGenericExecution(
            WorkflowTransition transition,
            WorkflowAction action,
            List<WorkflowValidationIssueResponse> blockingIssues
    ) {
        boolean unsupported = false;
        WorkflowStatusValidationHelper.CustomStatusExecutability sourceExecutability =
                WorkflowStatusValidationHelper.evaluateCustomStatusExecutability(transition.getFromStatusRecord(), false);
        WorkflowStatusValidationHelper.CustomStatusExecutability targetExecutability =
                WorkflowStatusValidationHelper.evaluateCustomStatusExecutability(
                        transition.getToStatusRecord(),
                        WorkflowStatusValidationHelper.isAllowedCustomTerminalTarget(transition.getToStatusRecord())
                );
        TicketStatus targetBehavior = transition.getToStatusRecord() == null
                ? transition.getToStatus()
                : transition.getToStatusRecord().getBehaviorBucket();
        WorkflowStatusValidationHelper.GenericTransitionEligibility eligibility =
                WorkflowStatusValidationHelper.evaluateGenericTransitionEligibility(
                        transition,
                        action,
                        transition.getFromStatusRecord(),
                        transition.getToStatusRecord(),
                        targetBehavior
                );

        if (eligibility.contains(WorkflowStatusValidationHelper.GenericTransitionEligibilityIssue.PROTECTED_TRANSITION)) {
            addIssue(
                    blockingIssues,
                    "RUNTIME_UNSUPPORTED_GENERIC_TRANSITION",
                    "Protected transitions require dedicated workflow handling and cannot run as generic custom transitions.",
                    transition.getId()
            );
            unsupported = true;
        }
        if (eligibility.contains(WorkflowStatusValidationHelper.GenericTransitionEligibilityIssue.PROTECTED_ACTION)
                || eligibility.contains(WorkflowStatusValidationHelper.GenericTransitionEligibilityIssue.PROTECTED_FIXED_ACTION_KEY)) {
            addIssue(
                    blockingIssues,
                    "RUNTIME_UNSUPPORTED_FIXED_ACTION",
                    "Use a custom non-protected action for generic workflow transitions. Pick, start, complete, and cancel use fixed workflow buttons because they require dedicated side effects.",
                    transition.getId()
            );
            unsupported = true;
        }
        if (!sourceExecutability.executable()) {
            addIssue(
                    blockingIssues,
                    "RUNTIME_UNSUPPORTED_GENERIC_TRANSITION",
                    "Use an active custom source status with a supported behavior bucket for generic execution.",
                    transition.getId()
            );
            unsupported = true;
        }
        if (eligibility.contains(WorkflowStatusValidationHelper.GenericTransitionEligibilityIssue.TERMINAL_SOURCE_STATUS)
                && sourceExecutability.executable()) {
            addIssue(
                    blockingIssues,
                    "RUNTIME_UNSUPPORTED_GENERIC_TRANSITION",
                    "Terminal source statuses cannot be changed by generic workflow transitions.",
                    transition.getId()
            );
            unsupported = true;
        }
        if (!targetExecutability.executable()) {
            addIssue(
                    blockingIssues,
                    "RUNTIME_UNSUPPORTED_TARGET_STATUS",
                    "Use an active custom target status with a supported behavior bucket. Protected fixed statuses require dedicated workflow handling.",
                    transition.getId()
            );
            unsupported = true;
        }

        if (targetBehavior == TicketStatus.PICKED || targetBehavior == TicketStatus.CANCELLED) {
            addIssue(
                    blockingIssues,
                    "RUNTIME_UNSUPPORTED_TARGET_STATUS",
                    "Generic transitions cannot target PICKED or CANCELLED because those statuses require fixed workflow side effects.",
                    transition.getId()
            );
            unsupported = true;
        }
        if (targetBehavior == TicketStatus.COMPLETED
                && eligibility.contains(WorkflowStatusValidationHelper.GenericTransitionEligibilityIssue.PROTECTED_TERMINAL_TARGET_STATUS)) {
            addIssue(
                    blockingIssues,
                    "RUNTIME_UNSUPPORTED_TARGET_STATUS",
                    "Generic completion must target an active custom terminal COMPLETED status. Protected/system completion uses fixed workflow handling.",
                    transition.getId()
            );
            unsupported = true;
        }
        if (WorkflowStatusValidationHelper.isTerminalBehavior(targetBehavior)
                && transition.getToStatusRecord() != null
                && !transition.getToStatusRecord().isTerminal()) {
            addIssue(
                    blockingIssues,
                    "RUNTIME_UNSUPPORTED_TARGET_STATUS",
                    "Non-terminal custom target statuses cannot use terminal behavior buckets.",
                    transition.getId()
            );
            unsupported = true;
        }
        return unsupported;
    }

    private void validateReachability(
            List<WorkflowTransition> transitions,
            List<WorkflowValidationIssueResponse> blockingIssues
    ) {
        if (transitions.isEmpty()) {
            return;
        }

        Map<Long, List<WorkflowTransition>> transitionsBySource = new HashMap<>();
        for (WorkflowTransition transition : transitions) {
            if (transition.getFromStatusRecord() == null || transition.getFromStatusRecord().getId() == null) {
                continue;
            }
            transitionsBySource
                    .computeIfAbsent(transition.getFromStatusRecord().getId(), ignored -> new ArrayList<>())
                    .add(transition);
        }

        Set<Long> reachable = new HashSet<>();
        List<Long> frontier = new ArrayList<>();
        for (WorkflowTransition transition : transitions) {
            if (transition.getFromStatusRecord() != null
                    && isNewStartStatus(transition)) {
                Long newStatusId = transition.getFromStatusRecord().getId();
                if (reachable.add(newStatusId)) {
                    frontier.add(newStatusId);
                }
            }
        }

        if (frontier.isEmpty()) {
            addIssue(blockingIssues, "UNREACHABLE_WORKFLOW_FROM_NEW", "Workflow has no active transition path from NEW", null);
            return;
        }

        boolean reachableTerminal = false;
        while (!frontier.isEmpty()) {
            Long current = frontier.remove(0);
            for (WorkflowTransition transition : transitionsBySource.getOrDefault(current, List.of())) {
                if (transition.getToStatusRecord() == null || transition.getToStatusRecord().getId() == null) {
                    continue;
                }
                Long target = transition.getToStatusRecord().getId();
                if (transition.getToStatusRecord().isTerminal()) {
                    reachableTerminal = true;
                }
                if (reachable.add(target)) {
                    frontier.add(target);
                }
            }
        }

        boolean anyUnreachable = transitions.stream()
                .anyMatch(transition -> transition.getFromStatusRecord() != null
                        && transition.getFromStatusRecord().getId() != null
                        && !reachable.contains(transition.getFromStatusRecord().getId()));
        if (anyUnreachable) {
            addIssue(blockingIssues, "UNREACHABLE_WORKFLOW_PATH", "Workflow contains active transitions unreachable from NEW", null);
        }

        if (!reachableTerminal) {
            addIssue(blockingIssues, "MISSING_TERMINAL_COMPLETION_PATH", "Workflow has no reachable terminal completion path", null);
        }
    }

    private boolean isNewStartStatus(WorkflowTransition transition) {
        if (transition.getFromStatusRecord() == null) {
            return transition.getFromStatus() == TicketStatus.NEW;
        }
        return transition.getFromStatusRecord().getBehaviorBucket() == TicketStatus.NEW
                || "NEW".equals(transition.getFromStatusRecord().getStatusKey());
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
