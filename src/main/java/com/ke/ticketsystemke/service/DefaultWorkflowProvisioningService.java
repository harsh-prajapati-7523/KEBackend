package com.ke.ticketsystemke.service;

import com.ke.ticketsystemke.config.CacheNames;
import com.ke.ticketsystemke.entity.AccessKey;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class DefaultWorkflowProvisioningService {

    private static final String ACCESS_CATEGORY = "Workflow Actions";
    private static final List<DefaultStatus> DEFAULT_STATUSES = List.of(
            new DefaultStatus(TicketStatus.NEW, "New", TicketStatus.NEW, false, 10),
            new DefaultStatus(TicketStatus.PICKED, "Picked", TicketStatus.PICKED, false, 20),
            new DefaultStatus(TicketStatus.IN_PROGRESS, "In Progress", TicketStatus.IN_PROGRESS, false, 30),
            new DefaultStatus(TicketStatus.COMPLETED, "Completed", TicketStatus.COMPLETED, true, 40),
            new DefaultStatus(TicketStatus.CANCELLED, "Cancelled", TicketStatus.CANCELLED, true, 50)
    );
    private static final List<DefaultAction> DEFAULT_ACTIONS = List.of(
            new DefaultAction(AccessKey.PICK_TICKET, "Pick Ticket", 10),
            new DefaultAction(AccessKey.START_WORK, "Start Work", 20),
            new DefaultAction(AccessKey.COMPLETE_TICKET, "Complete Ticket", 30),
            new DefaultAction(AccessKey.CANCEL_TICKET, "Cancel Ticket", 40)
    );
    private static final List<DefaultTransition> DEFAULT_TRANSITIONS = List.of(
            new DefaultTransition(AccessKey.PICK_TICKET, "Pick Ticket", TicketStatus.NEW, TicketStatus.PICKED, 10),
            new DefaultTransition(AccessKey.PICK_TICKET, "Pick Ticket", TicketStatus.PICKED, TicketStatus.PICKED, 20),
            new DefaultTransition(AccessKey.START_WORK, "Start Work", TicketStatus.PICKED, TicketStatus.IN_PROGRESS, 30),
            new DefaultTransition(AccessKey.COMPLETE_TICKET, "Complete Ticket", TicketStatus.IN_PROGRESS, TicketStatus.COMPLETED, 40),
            new DefaultTransition(AccessKey.CANCEL_TICKET, "Cancel Ticket", TicketStatus.NEW, TicketStatus.CANCELLED, 50),
            new DefaultTransition(AccessKey.CANCEL_TICKET, "Cancel Ticket", TicketStatus.PICKED, TicketStatus.CANCELLED, 60),
            new DefaultTransition(AccessKey.CANCEL_TICKET, "Cancel Ticket", TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED, 70)
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
        Map<TicketStatus, WorkflowStatus> statusesByBucket = ensureStatuses(actor);
        ensureActions(actor);
        List<WorkflowTransition> transitions = ensureTransitions(statusesByBucket, actor);
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
        category.setFixedActionsEnabled(true);
        category.setWorkflowModeUpdatedAt(Instant.now());
        category.setWorkflowModeUpdatedByEmployee(actor);
    }

    private Map<TicketStatus, WorkflowStatus> ensureStatuses(Employee actor) {
        Map<TicketStatus, WorkflowStatus> statusesByBucket = new EnumMap<>(TicketStatus.class);
        for (DefaultStatus defaultStatus : DEFAULT_STATUSES) {
            WorkflowStatus status = workflowStatusRepository.findByStatusKey(defaultStatus.key().name())
                    .orElseGet(() -> {
                        WorkflowStatus created = new WorkflowStatus();
                        created.setStatusKey(defaultStatus.key().name());
                        created.setDisplayName(defaultStatus.displayName());
                        created.setSystemStatus(true);
                        created.setProtectedStatus(true);
                        return created;
                    });
            status.setActive(true);
            status.setTerminal(defaultStatus.terminal());
            status.setBehaviorBucket(defaultStatus.behaviorBucket());
            status.setSortOrder(defaultStatus.sortOrder());
            status.setUpdatedByEmployee(actor);
            statusesByBucket.put(defaultStatus.key(), workflowStatusRepository.save(status));
        }
        return statusesByBucket;
    }

    private void ensureActions(Employee actor) {
        for (DefaultAction defaultAction : DEFAULT_ACTIONS) {
            WorkflowAction action = workflowActionRepository.findByActionKey(defaultAction.key().name())
                    .orElseGet(() -> {
                        WorkflowAction created = new WorkflowAction();
                        created.setActionKey(defaultAction.key().name());
                        created.setDisplayName(defaultAction.displayName());
                        created.setButtonLabel(defaultAction.displayName());
                        created.setDescription("Default workflow action.");
                        created.setSystemAction(true);
                        created.setProtectedAction(true);
                        return created;
                    });
            action.setActive(true);
            action.setSortOrder(defaultAction.sortOrder());
            action.setRequiresComment(false);
            action.setConfirmationRequired(false);
            action.setUpdatedByEmployee(actor);
            workflowActionRepository.save(action);
            ensureAccessMetadata(defaultAction, actor);
        }
    }

    private void ensureAccessMetadata(DefaultAction defaultAction, Employee actor) {
        AccessKeyMetadata metadata = accessKeyMetadataRepository.findByAccessKey(defaultAction.key().name())
                .orElseGet(() -> {
                    AccessKeyMetadata created = new AccessKeyMetadata();
                    created.setAccessKey(defaultAction.key().name());
                    return created;
                });
        metadata.setDisplayName(defaultAction.displayName());
        metadata.setDescription("Default workflow action.");
        metadata.setCategory(ACCESS_CATEGORY);
        metadata.setActive(true);
        metadata.setSystemKey(true);
        metadata.setProtectedKey(true);
        metadata.setSortOrder(defaultAction.sortOrder());
        metadata.setUpdatedByEmployee(actor);
        accessKeyMetadataRepository.save(metadata);
    }

    private List<WorkflowTransition> ensureTransitions(
            Map<TicketStatus, WorkflowStatus> statusesByBucket,
            Employee actor
    ) {
        return DEFAULT_TRANSITIONS.stream()
                .map(defaultTransition -> ensureTransition(defaultTransition, statusesByBucket, actor))
                .toList();
    }

    private WorkflowTransition ensureTransition(
            DefaultTransition defaultTransition,
            Map<TicketStatus, WorkflowStatus> statusesByBucket,
            Employee actor
    ) {
        WorkflowStatus fromStatus = statusesByBucket.get(defaultTransition.fromStatus());
        WorkflowStatus toStatus = statusesByBucket.get(defaultTransition.toStatus());
        WorkflowTransition transition = workflowTransitionRepository
                .findByActionKeyAndFromStatusRecord_IdAndToStatusRecord_Id(
                        defaultTransition.action().name(),
                        fromStatus.getId(),
                        toStatus.getId()
                )
                .orElseGet(WorkflowTransition::new);
        transition.setActionKey(defaultTransition.action());
        transition.setDisplayName(defaultTransition.displayName());
        transition.setFromStatus(defaultTransition.fromStatus());
        transition.setToStatus(defaultTransition.toStatus());
        transition.setFromStatusRecord(fromStatus);
        transition.setToStatusRecord(toStatus);
        transition.setActive(true);
        transition.setSortOrder(defaultTransition.sortOrder());
        transition.setSystemTransition(true);
        transition.setProtectedTransition(true);
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
                        .findByRoleIdAndAccessKey(role.getId(), defaultAction.key().name())
                        .orElseGet(() -> {
                            RoleAccessRule created = new RoleAccessRule();
                            created.setRole(role);
                            created.setAccessKey(defaultAction.key().name());
                            return created;
                        });
                rule.setAllowed(true);
                rule.setUpdatedByEmployee(actor);
                roleAccessRuleRepository.save(rule);
            }
        }
    }

    private record DefaultStatus(
            TicketStatus key,
            String displayName,
            TicketStatus behaviorBucket,
            boolean terminal,
            Integer sortOrder
    ) {
    }

    private record DefaultAction(
            AccessKey key,
            String displayName,
            Integer sortOrder
    ) {
    }

    private record DefaultTransition(
            AccessKey action,
            String displayName,
            TicketStatus fromStatus,
            TicketStatus toStatus,
            Integer sortOrder
    ) {
    }
}
