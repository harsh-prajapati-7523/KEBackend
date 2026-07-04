package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.config.CacheNames;
import com.ke.ticketsystemke.entity.AccessKeyMetadata;
import com.ke.ticketsystemke.entity.Employee;
import com.ke.ticketsystemke.entity.Role;
import com.ke.ticketsystemke.entity.RoleAccessRule;
import com.ke.ticketsystemke.entity.TicketCategoryConfig;
import com.ke.ticketsystemke.entity.TicketStatus;
import com.ke.ticketsystemke.entity.WorkflowAction;
import com.ke.ticketsystemke.entity.WorkflowMode;
import com.ke.ticketsystemke.entity.WorkflowStatus;
import com.ke.ticketsystemke.entity.WorkflowTransition;
import com.ke.ticketsystemke.entity.WorkflowTransitionCategoryRule;
import com.ke.ticketsystemke.entity.WorkflowTransitionRoleRule;
import com.ke.ticketsystemke.repository.AccessKeyMetadataRepository;
import com.ke.ticketsystemke.repository.RoleAccessRuleRepository;
import com.ke.ticketsystemke.repository.RoleRepository;
import com.ke.ticketsystemke.repository.WorkflowActionRepository;
import com.ke.ticketsystemke.repository.WorkflowStatusRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionCategoryRuleRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionRepository;
import com.ke.ticketsystemke.repository.WorkflowTransitionRoleRuleRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DefaultWorkflowProvisioningService {

    private static final String ACCESS_CATEGORY = "Workflow Actions";
    private static final List<DefaultStatus> DEFAULT_STATUSES = List.of(
            new DefaultStatus("DEFAULT_NEW", "Default New", TicketStatus.NEW, false, 10),
            new DefaultStatus("DEFAULT_IN_PROGRESS", "Default In Progress", TicketStatus.IN_PROGRESS, false, 20),
            new DefaultStatus("DEFAULT_DONE", "Default Done", TicketStatus.COMPLETED, true, 30)
    );
    private static final List<DefaultAction> DEFAULT_ACTIONS = List.of(
            new DefaultAction("DEFAULT_START", "Start", 10),
            new DefaultAction("DEFAULT_FINISH", "Finish", 20)
    );
    private static final List<DefaultTransition> DEFAULT_TRANSITIONS = List.of(
            new DefaultTransition("DEFAULT_START", "Start", "DEFAULT_NEW", "DEFAULT_IN_PROGRESS", 10),
            new DefaultTransition("DEFAULT_FINISH", "Finish", "DEFAULT_IN_PROGRESS", "DEFAULT_DONE", 20)
    );

    private final WorkflowStatusRepository workflowStatusRepository;
    private final WorkflowActionRepository workflowActionRepository;
    private final WorkflowTransitionRepository workflowTransitionRepository;
    private final WorkflowTransitionCategoryRuleRepository categoryRuleRepository;
    private final WorkflowTransitionRoleRuleRepository roleRuleRepository;
    private final RoleRepository roleRepository;
    private final RoleAccessRuleRepository roleAccessRuleRepository;
    private final AccessKeyMetadataRepository accessKeyMetadataRepository;
    private final WorkflowValidationService workflowValidationService;

    public DefaultWorkflowProvisioningService(
            WorkflowStatusRepository workflowStatusRepository,
            WorkflowActionRepository workflowActionRepository,
            WorkflowTransitionRepository workflowTransitionRepository,
            WorkflowTransitionCategoryRuleRepository categoryRuleRepository,
            WorkflowTransitionRoleRuleRepository roleRuleRepository,
            RoleRepository roleRepository,
            RoleAccessRuleRepository roleAccessRuleRepository,
            AccessKeyMetadataRepository accessKeyMetadataRepository,
            WorkflowValidationService workflowValidationService
    ) {
        this.workflowStatusRepository = workflowStatusRepository;
        this.workflowActionRepository = workflowActionRepository;
        this.workflowTransitionRepository = workflowTransitionRepository;
        this.categoryRuleRepository = categoryRuleRepository;
        this.roleRuleRepository = roleRuleRepository;
        this.roleRepository = roleRepository;
        this.roleAccessRuleRepository = roleAccessRuleRepository;
        this.accessKeyMetadataRepository = accessKeyMetadataRepository;
        this.workflowValidationService = workflowValidationService;
    }

    @Transactional
    @CacheEvict(cacheNames = {
            CacheNames.WORKFLOW_STATUSES,
            CacheNames.WORKFLOW_ACTIONS,
            CacheNames.WORKFLOW_TRANSITIONS,
            CacheNames.WORKFLOW_TRANSITION_OPTIONS,
            CacheNames.ACCESS_KEY_METADATA,
            CacheNames.TICKET_CATEGORIES
    }, allEntries = true)
    public void provisionAndActivate(TicketCategoryConfig category, Employee actor) {
        Map<String, WorkflowStatus> statusesByKey = ensureStatuses(actor);
        ensureActions(actor);
        List<WorkflowTransition> transitions = ensureTransitions(statusesByKey, actor);
        List<Role> activeRoles = roleRepository.findAllByOrderByRoleKeyAsc()
                .stream()
                .filter(Role::isActive)
                .toList();

        for (WorkflowTransition transition : transitions) {
            ensureCategoryRule(transition, category, actor);
            ensureRoleRules(transition, activeRoles, actor);
        }
        ensureRoleAccess(activeRoles, actor);

        workflowValidationService.requireValidCategoryWorkflow(category.getId());
        category.setWorkflowMode(WorkflowMode.DB_CONFIGURED);
        category.setDbWorkflowEnabled(true);
        category.setFixedActionsEnabled(false);
        category.setWorkflowModeUpdatedAt(Instant.now());
        category.setWorkflowModeUpdatedByEmployee(actor);
    }

    private Map<String, WorkflowStatus> ensureStatuses(Employee actor) {
        Map<String, WorkflowStatus> statusesByKey = new LinkedHashMap<>();
        for (DefaultStatus defaultStatus : DEFAULT_STATUSES) {
            WorkflowStatus status = workflowStatusRepository.findByStatusKey(defaultStatus.key())
                    .orElseGet(() -> {
                        WorkflowStatus created = new WorkflowStatus();
                        created.setStatusKey(defaultStatus.key());
                        created.setDisplayName(defaultStatus.displayName());
                        return created;
                    });
            status.setActive(true);
            status.setSystemStatus(false);
            status.setProtectedStatus(false);
            status.setTerminal(defaultStatus.terminal());
            status.setBehaviorBucket(defaultStatus.behaviorBucket());
            status.setSortOrder(defaultStatus.sortOrder());
            status.setUpdatedByEmployee(actor);
            statusesByKey.put(defaultStatus.key(), workflowStatusRepository.save(status));
        }
        return statusesByKey;
    }

    private void ensureActions(Employee actor) {
        for (DefaultAction defaultAction : DEFAULT_ACTIONS) {
            WorkflowAction action = workflowActionRepository.findByActionKey(defaultAction.key())
                    .orElseGet(() -> {
                        WorkflowAction created = new WorkflowAction();
                        created.setActionKey(defaultAction.key());
                        created.setDisplayName(defaultAction.displayName());
                        created.setButtonLabel(defaultAction.displayName());
                        created.setDescription("Default generic workflow action.");
                        return created;
                    });
            action.setActive(true);
            action.setSystemAction(false);
            action.setProtectedAction(false);
            action.setSortOrder(defaultAction.sortOrder());
            action.setRequiresComment(false);
            action.setConfirmationRequired(false);
            action.setUpdatedByEmployee(actor);
            workflowActionRepository.save(action);
            ensureAccessMetadata(defaultAction, actor);
        }
    }

    private void ensureAccessMetadata(DefaultAction defaultAction, Employee actor) {
        AccessKeyMetadata metadata = accessKeyMetadataRepository.findByAccessKey(defaultAction.key())
                .orElseGet(() -> {
                    AccessKeyMetadata created = new AccessKeyMetadata();
                    created.setAccessKey(defaultAction.key());
                    return created;
                });
        metadata.setDisplayName(defaultAction.displayName());
        metadata.setDescription("Default generic workflow action.");
        metadata.setCategory(ACCESS_CATEGORY);
        metadata.setActive(true);
        metadata.setSystemKey(false);
        metadata.setProtectedKey(false);
        metadata.setSortOrder(defaultAction.sortOrder());
        metadata.setUpdatedByEmployee(actor);
        accessKeyMetadataRepository.save(metadata);
    }

    private List<WorkflowTransition> ensureTransitions(
            Map<String, WorkflowStatus> statusesByKey,
            Employee actor
    ) {
        return DEFAULT_TRANSITIONS.stream()
                .map(defaultTransition -> ensureTransition(defaultTransition, statusesByKey, actor))
                .toList();
    }

    private WorkflowTransition ensureTransition(
            DefaultTransition defaultTransition,
            Map<String, WorkflowStatus> statusesByKey,
            Employee actor
    ) {
        WorkflowStatus fromStatus = statusesByKey.get(defaultTransition.fromStatusKey());
        WorkflowStatus toStatus = statusesByKey.get(defaultTransition.toStatusKey());
        WorkflowTransition transition = workflowTransitionRepository
                .findByActionKeyAndFromStatusRecord_IdAndToStatusRecord_Id(
                        defaultTransition.actionKey(),
                        fromStatus.getId(),
                        toStatus.getId()
                )
                .orElseGet(WorkflowTransition::new);
        transition.setActionKey(defaultTransition.actionKey());
        transition.setDisplayName(defaultTransition.displayName());
        transition.setFromStatus(fromStatus.getBehaviorBucket());
        transition.setToStatus(toStatus.getBehaviorBucket());
        transition.setFromStatusRecord(fromStatus);
        transition.setToStatusRecord(toStatus);
        transition.setActive(true);
        transition.setSortOrder(defaultTransition.sortOrder());
        transition.setSystemTransition(false);
        transition.setProtectedTransition(false);
        transition.setUpdatedByEmployee(actor);
        return workflowTransitionRepository.save(transition);
    }

    private void ensureCategoryRule(
            WorkflowTransition transition,
            TicketCategoryConfig category,
            Employee actor
    ) {
        WorkflowTransitionCategoryRule rule = categoryRuleRepository
                .findByWorkflowTransition_IdAndCategory_Id(transition.getId(), category.getId())
                .orElseGet(() -> {
                    WorkflowTransitionCategoryRule created = new WorkflowTransitionCategoryRule();
                    created.setWorkflowTransition(transition);
                    created.setCategory(category);
                    created.setCreatedByEmployee(actor);
                    return created;
                });
        rule.setActive(true);
        rule.setUpdatedByEmployee(actor);
        categoryRuleRepository.save(rule);
    }

    private void ensureRoleRules(
            WorkflowTransition transition,
            List<Role> activeRoles,
            Employee actor
    ) {
        for (Role role : activeRoles) {
            WorkflowTransitionRoleRule rule = roleRuleRepository
                    .findByWorkflowTransition_IdAndRole_Id(transition.getId(), role.getId())
                    .orElseGet(() -> {
                        WorkflowTransitionRoleRule created = new WorkflowTransitionRoleRule();
                        created.setWorkflowTransition(transition);
                        created.setRole(role);
                        created.setCreatedByEmployee(actor);
                        return created;
                    });
            rule.setActive(true);
            rule.setUpdatedByEmployee(actor);
            roleRuleRepository.save(rule);
        }
    }

    private void ensureRoleAccess(List<Role> activeRoles, Employee actor) {
        for (Role role : activeRoles) {
            for (DefaultAction defaultAction : DEFAULT_ACTIONS) {
                RoleAccessRule rule = roleAccessRuleRepository
                        .findByRoleIdAndAccessKey(role.getId(), defaultAction.key())
                        .orElseGet(() -> {
                            RoleAccessRule created = new RoleAccessRule();
                            created.setRole(role);
                            created.setAccessKey(defaultAction.key());
                            return created;
                        });
                rule.setAllowed(true);
                rule.setUpdatedByEmployee(actor);
                roleAccessRuleRepository.save(rule);
            }
        }
    }

    private record DefaultStatus(
            String key,
            String displayName,
            TicketStatus behaviorBucket,
            boolean terminal,
            Integer sortOrder
    ) {
    }

    private record DefaultAction(
            String key,
            String displayName,
            Integer sortOrder
    ) {
    }

    private record DefaultTransition(
            String actionKey,
            String displayName,
            String fromStatusKey,
            String toStatusKey,
            Integer sortOrder
    ) {
    }
}
